package com.bm_nttdata.credit_ms.api;

import com.bm_nttdata.credit_ms.dto.OperationResponseDto;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.mapper.CreditMapper;
import com.bm_nttdata.credit_ms.mapper.DailyCreditBalanceMapper;
import com.bm_nttdata.credit_ms.mapper.OperationResponseMapper;
import com.bm_nttdata.credit_ms.model.ApiResponseDto;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDto;
import com.bm_nttdata.credit_ms.model.CreditBalanceResponseDto;
import com.bm_nttdata.credit_ms.model.CreditRequestDto;
import com.bm_nttdata.credit_ms.model.CreditResponseDto;
import com.bm_nttdata.credit_ms.model.DailyBalanceDto;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDto;
import com.bm_nttdata.credit_ms.service.CreditService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Implementación del delegado de la API de creditos.
 * Maneja las peticiones HTTP recibidas por los endpoints de la API,
 * delegando la lógica de negocio al servicio correspondiente y
 * transformando las respuestas al formato requerido por la API.
 */
@Slf4j
@Component
public class CreditApiDelegateImpl implements CreditApiDelegate {

    @Autowired
    private CreditService creditService;

    @Autowired
    private CreditMapper creditMapper;

    @Autowired
    private DailyCreditBalanceMapper creditBalanceMapper;

    @Autowired
    private OperationResponseMapper responseMapper;

    @Override
    public ResponseEntity<List<CreditResponseDto>> getAllCredits(String customerId) {

        log.info("Getting credits for customer: {}", customerId);
        List<CreditResponseDto> credits = creditService.getAllCredits(customerId)
                .stream()
                .map(creditMapper::creditEntityToCreditResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(credits);
    }

    @Override
    public ResponseEntity<CreditResponseDto> getCreditById(String id) {

        log.info("Getting credit: {}", id);
        Credit credit = creditService.getCreditById(id);
        return ResponseEntity.ok(creditMapper.creditEntityToCreditResponseDto(credit));
    }

    @Override
    public ResponseEntity<Boolean> getCustomerCreditDebts(String customerId) {
        log.info("Getting customer credit debts: {}", customerId);
        boolean hasDebts = creditService.getCustomerCreditDebts(customerId);
        return ResponseEntity.ok(hasDebts);
    }

    @Override
    @CircuitBreaker(name = "createCredit", fallbackMethod = "createCreditFallback")
    public ResponseEntity<CreditResponseDto> createCredit(CreditRequestDto creditRequest) {

        log.info("Creating credit for customer: {}", creditRequest.getCustomerId());
        Credit credit = creditService.createCredit(creditRequest);
        return ResponseEntity.ok(creditMapper.creditEntityToCreditResponseDto(credit));
    }

    @Override
    public ResponseEntity<Void> deleteCredit(String id) {

        log.info("Deleting credit: {}", id);
        creditService.deleteCredit(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<List<DailyBalanceDto>> getAllCreditDailyBalances(
            String id, LocalDate searchMonth) {
        log.info("Getting daily balances for credit: {}", id);
        List<DailyBalanceDto> dailyBalances =
                creditService.getAllCreditDailyBalances(id, searchMonth)
                .stream()
                .map(creditBalanceMapper::dailyBalanceToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dailyBalances);
    }

    @Override
    public ResponseEntity<ApiResponseDto> paymentCredit(
            PaymentCreditProductRequestDto paymentCreditProductRequest) {

        log.info("Processing credit payment: {}", paymentCreditProductRequest.getCreditId());
        OperationResponseDto operationResponse =
                creditService.paymentCredit(paymentCreditProductRequest);

        return ResponseEntity.ok(
                responseMapper.entityOperationResponseToApiResponseDto(operationResponse));
    }

    @Override
    public ResponseEntity<ApiResponseDto> updateCreditBalance(
            String id, BalanceUpdateRequestDto balanceUpdateRequest) {

        log.info("Updating balance of credit: {}", id);
        OperationResponseDto operationResponse =
                creditService.updateCreditBalance(id, balanceUpdateRequest);
        return ResponseEntity.ok(
                responseMapper.entityOperationResponseToApiResponseDto(operationResponse));
    }

    @Override
    public ResponseEntity<CreditBalanceResponseDto> getCreditBalance(String id) {

        log.info("Getting balance for credit: {}", id);
        Credit account = creditService.getCreditById(id);
        return ResponseEntity.ok(
                creditMapper.creditEntityToCreditBalanceResponseDto(account));
    }

    private ResponseEntity<CreditResponseDto> createCreditFallback(
            CreditRequestDto creditRequest, Exception e) {
        log.error("Fallback for create credit: {}", e.getMessage());
        return new ResponseEntity(
                "We are experiencing some errors. Please try again later", HttpStatus.OK);
    }
}
