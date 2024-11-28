package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.DTO.OperationResponseDTO;
import com.bm_nttdata.credit_ms.DTO.CustomerDTO;
import com.bm_nttdata.credit_ms.client.CustomerClient;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.enums.CardStatusEnum;
import com.bm_nttdata.credit_ms.exception.ApiInvalidRequestException;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.mapper.CreditCardMapper;
import com.bm_nttdata.credit_ms.model.*;
import com.bm_nttdata.credit_ms.repository.CreditCardRepository;
import com.bm_nttdata.credit_ms.service.ICreditCardInstallmentService;
import com.bm_nttdata.credit_ms.service.ICreditCardService;
import com.bm_nttdata.credit_ms.util.CardNumberGenerator;
import com.bm_nttdata.credit_ms.util.MonthlyInstallmentCalculator;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Transactional
@Service
public class CreditCardServiceImpl implements ICreditCardService {

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private CreditCardMapper creditCardMapper;

    @Autowired
    private CustomerClient customerClient;

    @Autowired
    private MonthlyInstallmentCalculator installmentCalculator;

    @Autowired
    private CardNumberGenerator cardNumberGenerator;

    @Autowired
    private ICreditCardInstallmentService cardInstallmentService;

    @Override
    public List<CreditCard> getAllCreditCards(String customerId) {

        if (customerId == null){
            throw new ApiInvalidRequestException("Customer id is required");
        }

        return creditCardRepository.findByCustomerId(customerId);
    }

    @Override
    public CreditCard getCreditCardById(String id) {

        log.info("Querying credit card data: {}", id);
        return creditCardRepository.findById(id)
                .orElseThrow(() -> new CreditNotFoundException("Credit Card not found with id: " + id));
    }

    @Override
    public CreditCard createCreditCard(CreditCardRequestDTO creditCardRequestDTO) {

        CustomerDTO customerDTO ;

        try{
            customerDTO = customerClient.getCustomerById(creditCardRequestDTO.getCustomerId());
        } catch (FeignException e){
            log.error("Error calling customer service: {}", e.getMessage());
            throw new ServiceException("Error retrieving customer information: " + e.getMessage());
        }

        validateCreditCreation(customerDTO, creditCardRequestDTO);
        CreditCard creditCard = initializeCreditCard(creditCardMapper.creditCardRequestDtoToCreditCardEntity(creditCardRequestDTO));

        try{
            return creditCardRepository.save(creditCard);
        } catch (Exception e) {
            log.error("Unexpected error while saving credit card: {}", e.getMessage());
            throw new ServiceException("Unexpected error creating credit card" + e.getMessage());
        }
    }

    @Override
    public OperationResponseDTO chargeCreditCard(ChargueCreditCardRequestDTO chargueCreditCardRequestDTO) {

        log.info("Starting credit card charge process: {}", chargueCreditCardRequestDTO.getCreditCardId());

        try{
            CreditCard creditCard = getCreditCardById(chargueCreditCardRequestDTO.getCreditCardId());
            BigDecimal chargeAmount = chargueCreditCardRequestDTO.getChargeAmount();

            if (creditCard.getAvailableCredit().compareTo(chargeAmount) < 0) {

                return OperationResponseDTO.builder()
                        .status("FAILED")
                        .message("Unprocessed charge")
                        .error("Insufficient available credit")
                        .build();
            }

            if (chargueCreditCardRequestDTO.getTotalInstallment() > 1){
                chargeAmount = installmentCalculator.calculateMonthlyPayment(
                        chargueCreditCardRequestDTO.getChargeAmount(),
                        BigDecimal.valueOf(creditCard.getInterestRate()),
                        chargueCreditCardRequestDTO.getTotalInstallment());
            }

            cardInstallmentService.createCharges(
                    chargeAmount, chargueCreditCardRequestDTO.getTotalInstallment(),
                    creditCard.getId(), creditCard.getPaymentDate());

            BalanceUpdateRequestDTO balanceUpdateRequestDTO = new BalanceUpdateRequestDTO();
            balanceUpdateRequestDTO.setTransactionAmount(chargeAmount);
            balanceUpdateRequestDTO.setTransactionType(BalanceUpdateRequestDTO.TransactionTypeEnum.CREDIT_CHARGE);

            OperationResponseDTO operationResponseDTO = updateCreditCardBalance(creditCard.getId(), balanceUpdateRequestDTO);

            if (!operationResponseDTO.getStatus().equals("SUCCESS")){

                return operationResponseDTO;
            }

            log.info("Credit card charge processed successfully: {}", creditCard.getId());
            operationResponseDTO.setMessage("Charge successfully processed");

            return operationResponseDTO;

        } catch (Exception e) {
            log.error("Unexpected error during credit card charge process: {}", e.getMessage());
            //throw new ServiceException("Error processing credit card charge: " + e.getMessage());
            return OperationResponseDTO.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error processing credit card charge" + e.getMessage())
                    .build();
        }
    }

    @Override
    public OperationResponseDTO paymentCreditCard(PaymentCreditProductRequestDTO paymentCreditProductRequestDTO) {

        log.info("Initiating payment processing on credit card: {}", paymentCreditProductRequestDTO.getCreditProductId());

        try {
            CreditCard creditCard = getCreditCardById(paymentCreditProductRequestDTO.getCreditProductId());
            BigDecimal amountPaid = cardInstallmentService.payBillMonth(
                    paymentCreditProductRequestDTO.getPaymentAmount(),
                    creditCard.getId(),
                    creditCard.getPaymentDate());

            BalanceUpdateRequestDTO balanceUpdateRequestDTO = new BalanceUpdateRequestDTO();
            balanceUpdateRequestDTO.setTransactionAmount(amountPaid);
            balanceUpdateRequestDTO.setTransactionType(BalanceUpdateRequestDTO.TransactionTypeEnum.PAYMENT);

            OperationResponseDTO operationResponseDTO = updateCreditCardBalance(creditCard.getId(), balanceUpdateRequestDTO);

            if (!operationResponseDTO.getStatus().equals("SUCCESS")){

                return operationResponseDTO;
            }

            log.info("Payment processed successfully: {}", creditCard.getId());
            operationResponseDTO.setMessage("Payment successfully processed");

            return operationResponseDTO;

        }catch (Exception e){
            log.error("Unexpected error paying monthly credit card bill: {}", e.getMessage());
            return OperationResponseDTO.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error when paying monthly credit card bill: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public OperationResponseDTO updateCreditCardBalance(String id, BalanceUpdateRequestDTO balanceUpdateRequestDTO) {

        log.info("Initiating credit card balance update: {}", id);

        try {
            CreditCard creditCard = getCreditCardById(id);
            String transactionType = balanceUpdateRequestDTO.getTransactionType().getValue();
            BigDecimal transactionAmount = balanceUpdateRequestDTO.getTransactionAmount();

            switch (transactionType){
                case "PAYMENT":
                    creditCard.setAvailableCredit(creditCard.getAvailableCredit().add(transactionAmount));
                    break;

                case "CREDIT_CHARGE":
                    if (creditCard.getAvailableCredit().compareTo(transactionAmount) < 0) {
                        return OperationResponseDTO.builder()
                                .status("FAILED")
                                .message("Unprocessed charge")
                                .error("Insufficient available credit")
                                .build();
                    }

                    creditCard.setAvailableCredit(creditCard.getAvailableCredit().subtract(transactionAmount));
                    break;

                default:
                    return OperationResponseDTO.builder()
                            .status("FAILED")
                            .message("Unprocessed charge")
                            .error("Incorrect transaction type")
                            .build();
            }

            creditCard.setUpdatedAt(LocalDateTime.now());

            creditCardRepository.save(creditCard);

            log.info(" *** Balance update successful *** ");

            return OperationResponseDTO.builder()
                    .status("SUCCESS")
                    .message("Balance update successful")
                    .build();

        }catch (Exception e){
            log.error("Unexpected error while updating credit card balance: {}", e.getMessage());
            return OperationResponseDTO.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error while updating monthly credit card balance: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public void deleteCredit(String id) {
        log.info("Initiating credit deletion: {}", id);

        try {
            CreditCard creditCard = getCreditCardById(id);
            validateCreditCardDeletion(creditCard);

            creditCardRepository.delete(creditCard);
        } catch (Exception e) {
            log.error("Error deleting credit {}: {}", id, e.getMessage());
            throw new ServiceException("Error deleting credit: " + e.getMessage());
        }
    }

    private void validateCreditCardDeletion(CreditCard creditCard) {
        if (creditCard.getCreditLimit().compareTo(creditCard.getAvailableCredit()) != 0){
            throw new BusinessRuleException("A credit card with an outstanding balance can't be deleted.");
        }
    }

    private void validateCreditCreation(CustomerDTO customerDTO, CreditCardRequestDTO creditCardRequestDTO) {

        if (customerDTO.getCustomerType().equals("PERSONAL")){

            if (creditCardRequestDTO.getCardType().getValue().equals("BUSINESS")){
                throw new BusinessRuleException("Personal client can't apply for Business credit card");
            }
        }
        else if (customerDTO.getCustomerType().equals("BUSINESS")){
            if (creditCardRequestDTO.getCardType().getValue().equals("PERSONAL")){
                throw new BusinessRuleException("Business client can't apply for Personal credit card");
            }
        }
    }
    private CreditCard initializeCreditCard(CreditCard creditCard) {

        creditCard.setCardNumber(cardNumberGenerator.generateCardNumber());
        creditCard.setAvailableCredit(creditCard.getCreditLimit());
        creditCard.setStatus(CardStatusEnum.valueOf("ACTIVE"));
        creditCard.setCreatedAt(LocalDateTime.now());
        creditCard.setUpdatedAt(LocalDateTime.now());

        return creditCard;
    }
}
