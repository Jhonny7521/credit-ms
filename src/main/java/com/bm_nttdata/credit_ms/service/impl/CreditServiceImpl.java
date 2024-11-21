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
import com.bm_nttdata.credit_ms.service.ICreditService;
import com.bm_nttdata.credit_ms.util.CardNumberGenerator;
import com.netflix.discovery.converters.Auto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private CardNumberGenerator cardNumberGenerator;
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

        CustomerDTO customerDTO = new CustomerDTO();

        try{
            customerDTO = customerClient.getCustomerById(creditRequestDTO.getCustomerId());

        } catch (FeignException e){
            log.error("Error calling customer service: {}", e.getMessage());
            throw new ServiceException("Error retrieving customer information: " + e.getMessage());
        }

        validateCreditCreation(customerDTO);

        Credit credit = initializeCredit(creditMapper.creditRequestDtoToCreditEntity(creditRequestDTO));

        try{
            return creditRepository.save(credit);
        }catch (Exception e) {
            log.error("Unexpected error while saving credit: {}", e.getMessage());
            throw new ServiceException("Unexpected error creating credit");
        }
    }

    @Override
    public Credit updateCredit(String id, BalanceUpdateRequestDTO balanceUpdateRequestDTO) {

        Credit credit = getCreditById(id);
        try{
            credit = updateBalanceByTransactionType(credit, balanceUpdateRequestDTO);

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

    private void validateCreditCreation(CustomerDTO customerDTO) {

        if (customerDTO.getCustomerType().equals("PERSONAL")){
            long count = creditRepository.countByCustomerIdAndAmountGreaterThan(customerDTO.getId(), 0);
            if (count > 0 ) {
                throw new BusinessRuleException("Customer already has a credit with an outstanding balance");
            }
        }
    }

    private void validateAccountDeletion(Credit credit) {
        if (credit.getBalance() > 0){
            throw new BusinessRuleException("An credit with an outstanding balance can't be deleted.");
        }
    }

    private Credit updateBalanceByTransactionType(Credit credit, BalanceUpdateRequestDTO balanceUpdateRequestDTO){

        Double newBalance = credit.getBalance() - Double.valueOf(balanceUpdateRequestDTO.getTransactionAmount().doubleValue());

        credit.setBalance(newBalance);
        credit.setNextPaymentDate(credit.getNextPaymentDate().plusMonths(1));
        credit.setUpdatedAt(LocalDateTime.now());

        return credit;
    }

    private Credit initializeCredit(Credit credit) {
        if (credit.getInterestRate() == null || credit.getInterestRate() == 0) credit.setInterestRate(15.0);
        credit.setBalance(credit.getAmount());
        credit.setStatus(CreditStatusEnum.valueOf("ACTIVE"));
        credit.setNextPaymentDate(LocalDate.now().plusMonths(1));
        credit.setCreatedAt(LocalDateTime.now());
        credit.setUpdatedAt(LocalDateTime.now());

        return credit;
    }
}
