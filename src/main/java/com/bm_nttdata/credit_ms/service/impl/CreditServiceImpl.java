package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.DTO.CustomerDTO;
import com.bm_nttdata.credit_ms.client.CustomerClient;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.enums.CreditStatusEnum;
import com.bm_nttdata.credit_ms.exception.ApiInvalidRequestException;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.mapper.CreditMapper;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDTO;
import com.bm_nttdata.credit_ms.model.CreditRequestDTO;
import com.bm_nttdata.credit_ms.model.CreditResponseDTO;
import com.bm_nttdata.credit_ms.repository.CreditRepository;
import com.bm_nttdata.credit_ms.service.ICreditPaymentScheduleService;
import com.bm_nttdata.credit_ms.service.ICreditService;
import com.bm_nttdata.credit_ms.util.CardNumberGenerator;
import com.netflix.discovery.converters.Auto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Transactional
@Service
public class CreditServiceImpl implements ICreditService {

    @Autowired
    private CreditRepository creditRepository;
    @Autowired
    private CreditMapper creditMapper;
    @Autowired
    private ICreditPaymentScheduleService paymentScheduleService;
    @Autowired
    private CustomerClient customerClient;

    @Override
    public List<Credit> getAllCredits(String customerId) {

        if (customerId == null){
            throw new ApiInvalidRequestException("Customer id is required");
        }

        List<Credit> creditList = creditRepository.findByCustomerId(customerId);

        return creditList;
    }

    @Override
    public Credit getCreditById(String id) {
        return creditRepository.findById(id)
                .orElseThrow(() -> new CreditNotFoundException("Credit not found with id: " + id));
    }

    @Override
    public Credit createCredit(CreditRequestDTO creditRequestDTO) {

        CustomerDTO customerDTO ;

        try{
            customerDTO = customerClient.getCustomerById(creditRequestDTO.getCustomerId());
        } catch (FeignException e){
            log.error("Error calling customer service: {}", e.getMessage());
            throw new ServiceException("Error retrieving customer information: " + e.getMessage());
        }

        validateCreditCreation(customerDTO, creditRequestDTO);
        BigDecimal monthlyPayment = paymentScheduleService.calculateMonthlyPayment(creditRequestDTO.getAmount(), creditRequestDTO.getInterestRate(), creditRequestDTO.getTerm()); //BigDecimal creditAmount, double interestRate, int months
        Credit credit = initializeCredit(creditMapper.creditRequestDtoToCreditEntity(creditRequestDTO), monthlyPayment);

        try{
            credit = creditRepository.save(credit);
            paymentScheduleService.createPaymentSchedule(credit);
            return credit;
        } catch (Exception e) {
            log.error("Unexpected error while saving credit: {}", e.getMessage());
            throw new ServiceException("Unexpected error creating credit");
        }
    }

    @Override
    public Credit updateCredit(String id, BalanceUpdateRequestDTO balanceUpdateRequestDTO) {

        Credit credit = getCreditById(id);
        paymentScheduleService.updatePaymentSchedule(credit);
        credit = updateBalanceByTransactionType(credit, balanceUpdateRequestDTO);

        try{
            return creditRepository.save(credit);
        }catch (Exception e){
            log.error("Unexpected error while updating account {}: {}", id, e.getMessage());
            throw new ServiceException("Unexpected error updating account");
        }
    }

    @Override
    public void deleteCredit(String id) {

        Credit credit = getCreditById(id);
        validateAccountDeletion(credit);

        try {
            creditRepository.delete(credit);
        } catch (Exception e) {
            log.error("Error deleting credit {}: {}", id, e.getMessage());
            throw new ServiceException("Error deleting credit: " + e.getMessage());
        }
    }

    private void validateCreditCreation(CustomerDTO customerDTO, CreditRequestDTO creditRequestDTO) {

        if (customerDTO.getCustomerType().equals("PERSONAL")){
            long count = creditRepository.countByCustomerIdAndAmountGreaterThan(customerDTO.getId(), BigDecimal.valueOf(0L));
            if (count > 0 ) {
                throw new BusinessRuleException("Customer already has a credit with an outstanding balance");
            }

            if (creditRequestDTO.getCreditType().getValue().equals("BUSINESS")){
                throw new BusinessRuleException("Personal client can't apply for Business credit");
            }
        }
        else if (customerDTO.getCustomerType().equals("BUSINESS")){
            if (creditRequestDTO.getCreditType().getValue().equals("PERSONAL")){
                throw new BusinessRuleException("Business client can't apply for Personal credit");
            }
        }

        if (creditRequestDTO.getInterestRate() == null || creditRequestDTO.getInterestRate().equals("")){
            throw new BusinessRuleException("Attribute InterestRate is required");
        }

        if (creditRequestDTO.getTerm() == null || creditRequestDTO.getTerm().equals("")){
            throw new BusinessRuleException("Attribute Term is required");
        }
    }

    private void validateAccountDeletion(Credit credit) {
        if (credit.getBalance().intValue() > 0){
            throw new BusinessRuleException("An credit with an outstanding balance can't be deleted.");
        }
    }

    private Credit updateBalanceByTransactionType(Credit credit, BalanceUpdateRequestDTO balanceUpdateRequestDTO){

        BigDecimal newBalance = credit.getBalance().subtract(balanceUpdateRequestDTO.getTransactionAmount()) ; //credit.getBalance().doubleValue() - Double.valueOf(balanceUpdateRequestDTO.getTransactionAmount().doubleValue());

        credit.setBalance(newBalance);
        credit.setNextPaymentDate(credit.getNextPaymentDate().plusMonths(1));
        credit.setNextPaymentInstallment(credit.getNextPaymentInstallment() + 1);
        credit.setUpdatedAt(LocalDateTime.now());

        return credit;
    }

    private Credit initializeCredit(Credit credit, BigDecimal monthlyPayment) {

        credit.setBalance(credit.getAmount());
        credit.setStatus(CreditStatusEnum.valueOf("ACTIVE"));
        credit.setNextPaymentDate(LocalDate.now().plusMonths(1));
        credit.setNextPaymentAmount(monthlyPayment);
        credit.setNextPaymentInstallment(1);
        credit.setCreatedAt(LocalDateTime.now());
        credit.setUpdatedAt(LocalDateTime.now());

        return credit;
    }
}
