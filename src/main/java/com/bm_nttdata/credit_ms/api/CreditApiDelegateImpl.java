package com.bm_nttdata.credit_ms.api;

import com.bm_nttdata.credit_ms.DTO.OperationResponseDTO;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.mapper.CreditMapper;
import com.bm_nttdata.credit_ms.mapper.OperationResponseMapper;
import com.bm_nttdata.credit_ms.model.*;
import com.bm_nttdata.credit_ms.service.ICreditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CreditApiDelegateImpl implements CreditApiDelegate{

    @Autowired
    private ICreditService creditService;

    @Autowired
    private CreditMapper creditMapper;

    @Autowired
    private OperationResponseMapper responseMapper;

    @Override
    public ResponseEntity<List<CreditResponseDTO>> getAllCredits(String customerId){
        log.info("Getting credits for customer: {}", customerId);
        List<CreditResponseDTO> credits = creditService.getAllCredits(customerId)
                .stream()
                .map(creditMapper::creditEntityToCreditResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(credits);
    }

    @Override
    public ResponseEntity<CreditResponseDTO> getCreditById(String id){
        log.info("Getting credit: {}", id);
        Credit credit = creditService.getCreditById(id);
        return ResponseEntity.ok(creditMapper.creditEntityToCreditResponseDto(credit));
    }

    @Override
    public ResponseEntity<CreditResponseDTO> createCredit(CreditRequestDTO creditRequestDTO) {
        log.info("Creating credit for customer: {}", creditRequestDTO.getCustomerId());
        Credit credit = creditService.createCredit(creditRequestDTO);
        return ResponseEntity.ok(creditMapper.creditEntityToCreditResponseDto(credit));
    }

    @Override
    public ResponseEntity<Void> deleteCredit(String id) {
        log.info("Deleting credit: {}", id);
        creditService.deleteCredit(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<ApiResponseDTO> paymentCredit(PaymentCreditProductRequestDTO paymentCreditProductRequestDTO) {

        log.info("Processing credit payment: {}", paymentCreditProductRequestDTO.getCreditId());
        OperationResponseDTO operationResponseDTO = creditService.paymentCredit(paymentCreditProductRequestDTO);

        return ResponseEntity.ok(responseMapper.entityOperationResponseToApiResponseDTO(operationResponseDTO));
    }

    @Override
    public ResponseEntity<ApiResponseDTO> updateCreditBalance(String id, BalanceUpdateRequestDTO balanceUpdateRequestDTO) {
        log.info("Updating balance of credit: {}", id);
        OperationResponseDTO operationResponseDTO = creditService.updateCreditBalance(id, balanceUpdateRequestDTO);
        return ResponseEntity.ok(responseMapper.entityOperationResponseToApiResponseDTO(operationResponseDTO));
    }

    @Override
    public ResponseEntity<CreditBalanceResponseDTO> getCreditBalance(String id) {
        log.info("Getting balance for credit: {}", id);
        Credit account = creditService.getCreditById(id);
        return ResponseEntity.ok(creditMapper.creditEntityToCreditBalanceResponseDto(account));

    }
}
