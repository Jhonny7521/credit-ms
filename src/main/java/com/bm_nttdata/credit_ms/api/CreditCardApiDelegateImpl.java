package com.bm_nttdata.credit_ms.api;

import com.bm_nttdata.credit_ms.DTO.OperationResponseDTO;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.mapper.CreditCardMapper;
import com.bm_nttdata.credit_ms.mapper.OperationResponseMapper;
import com.bm_nttdata.credit_ms.model.*;
import com.bm_nttdata.credit_ms.service.ICreditCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CreditCardApiDelegateImpl implements CreditCardApiDelegate{

    @Autowired
    private ICreditCardService creditCardService;

    @Autowired
    private CreditCardMapper creditCardMapper;

    @Autowired
    private OperationResponseMapper responseMapper;

    @Override
    public ResponseEntity<List<CreditCardResponseDTO>> getAllCreditCars(String customerId) {

        log.info("Getting credit cards for customer: {}", customerId);
        List<CreditCardResponseDTO> creditCardList = creditCardService.getAllCreditCards(customerId)
                .stream()
                .map(creditCardMapper::creditCardEntityToCreditCardResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(creditCardList);
    }

    @Override
    public ResponseEntity<CreditCardResponseDTO> getCreditCardById(String id) {

        log.info("Getting credit card: {}", id);
        CreditCard creditCard = creditCardService.getCreditCardById(id);
        return ResponseEntity.ok(creditCardMapper.creditCardEntityToCreditCardResponseDto(creditCard));
    }

    @Override
    public ResponseEntity<CreditCardResponseDTO> createCreditCard(CreditCardRequestDTO creditCardRequestDTO) {

        log.info("Creating credit card for customer: {}", creditCardRequestDTO.getCustomerId());
        CreditCard creditCard = creditCardService.createCreditCard(creditCardRequestDTO);
        return ResponseEntity.ok(creditCardMapper.creditCardEntityToCreditCardResponseDto(creditCard));
    }

    @Override
    public ResponseEntity<Void> deleteCreditCard(String id) {
        log.info("Deleting credit card: {}", id);
        creditCardService.deleteCredit(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<ApiResponseDTO> chargeCreditCard(String id, ChargueCreditCardRequestDTO chargueCreditCardRequestDTO) {

        log.info("Generating credit card charges: {}", chargueCreditCardRequestDTO.getCreditCardId());
        OperationResponseDTO operationResponseDTO = creditCardService.chargeCreditCard(chargueCreditCardRequestDTO);

        return ResponseEntity.ok(responseMapper.entityOperationResponseToApiResponseDTO(operationResponseDTO));
    }

    @Override
    public ResponseEntity<ApiResponseDTO> paymentCreditCard(PaymentCreditProductRequestDTO paymentCreditProductRequestDTO) {

        log.info("Processing credit card payment: {}", paymentCreditProductRequestDTO.getCreditId());
        OperationResponseDTO operationResponseDTO = creditCardService.paymentCreditCard(paymentCreditProductRequestDTO);

        return ResponseEntity.ok(responseMapper.entityOperationResponseToApiResponseDTO(operationResponseDTO));
    }

    @Override
    public ResponseEntity<ApiResponseDTO> updateCreditCardBalance(String id, BalanceUpdateRequestDTO balanceUpdateRequestDTO) {

        log.info("Updating credit card balance: {}", id);
        OperationResponseDTO operationResponseDTO = creditCardService.updateCreditCardBalance(id, balanceUpdateRequestDTO);

        return ResponseEntity.ok(responseMapper.entityOperationResponseToApiResponseDTO(operationResponseDTO));
    }

    @Override
    public ResponseEntity<CreditCardBalanceResponseDTO> getCreditCardBalance(String id) {

        log.info("Obtaining credit card balance: {}", id);
        CreditCard creditCard = creditCardService.getCreditCardById(id);

        return ResponseEntity.ok(creditCardMapper.creditCardEntityToCreditCardBalanceResponseDto(creditCard));
    }
}
