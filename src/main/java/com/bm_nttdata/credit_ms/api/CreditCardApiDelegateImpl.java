package com.bm_nttdata.credit_ms.api;

import com.bm_nttdata.credit_ms.dto.OperationResponseDto;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.mapper.CreditCardMapper;
import com.bm_nttdata.credit_ms.mapper.DailyCreditBalanceMapper;
import com.bm_nttdata.credit_ms.mapper.OperationResponseMapper;
import com.bm_nttdata.credit_ms.model.ApiResponseDto;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDto;
import com.bm_nttdata.credit_ms.model.ChargueCreditCardRequestDto;
import com.bm_nttdata.credit_ms.model.CreditCardBalanceResponseDto;
import com.bm_nttdata.credit_ms.model.CreditCardRequestDto;
import com.bm_nttdata.credit_ms.model.CreditCardResponseDto;
import com.bm_nttdata.credit_ms.model.DailyBalanceDto;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDto;
import com.bm_nttdata.credit_ms.service.CreditCardService;
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
 * Implementación del delegado de la API de tarjetas de credito.
 * Maneja las peticiones HTTP recibidas por los endpoints de la API,
 * delegando la lógica de negocio al servicio correspondiente y
 * transformando las respuestas al formato requerido por la API.
 */
@Slf4j
@Component
public class CreditCardApiDelegateImpl implements CreditCardApiDelegate {

    @Autowired
    private CreditCardService creditCardService;

    @Autowired
    private CreditCardMapper creditCardMapper;

    @Autowired
    private OperationResponseMapper responseMapper;

    @Autowired
    private DailyCreditBalanceMapper creditBalanceMapper;

    @Override
    public ResponseEntity<List<CreditCardResponseDto>> getAllCreditCars(String customerId) {

        log.info("Getting credit cards for customer: {}", customerId);
        List<CreditCardResponseDto> creditCardList = creditCardService.getAllCreditCards(customerId)
                .stream()
                .map(creditCardMapper::creditCardEntityToCreditCardResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(creditCardList);
    }

    @Override
    public ResponseEntity<CreditCardResponseDto> getCreditCardById(String id) {

        log.info("Getting credit card: {}", id);
        CreditCard creditCard = creditCardService.getCreditCardById(id);
        return ResponseEntity.ok(
                creditCardMapper.creditCardEntityToCreditCardResponseDto(creditCard));
    }

    @Override
    public ResponseEntity<Boolean> getCustomerCreditCardDebts(String customerId) {
        log.info("Getting customer credit card debts: {}", customerId);
        boolean hasDebts = creditCardService.getCustomerCreditCardDebts(customerId);
        return ResponseEntity.ok(hasDebts);
    }

    @Override
    @CircuitBreaker(name = "createCreditCard", fallbackMethod = "createCreditCardFallback")
    public ResponseEntity<CreditCardResponseDto> createCreditCard(
            CreditCardRequestDto creditCardRequest) {

        log.info("Creating credit card for customer: {}", creditCardRequest.getCustomerId());
        CreditCard creditCard = creditCardService.createCreditCard(creditCardRequest);
        return ResponseEntity.ok(
                creditCardMapper.creditCardEntityToCreditCardResponseDto(creditCard));
    }

    @Override
    public ResponseEntity<Void> deleteCreditCard(String id) {
        log.info("Deleting credit card: {}", id);
        creditCardService.deleteCredit(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<List<DailyBalanceDto>> getAllCreditCardDailyBalances(
            String id, LocalDate searchMonth) {
        log.info("Getting daily balances for credit card: {}", id);
        List<DailyBalanceDto> dailyCardBalances =
                creditCardService.getAllCreditCardDailyBalances(id, searchMonth)
                        .stream()
                        .map(creditBalanceMapper::dailyBalanceToDto)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(dailyCardBalances);
    }

    @Override
    public ResponseEntity<ApiResponseDto> chargeCreditCard(
            String id, ChargueCreditCardRequestDto chargueCreditCardRequest) {

        log.info("Generating credit card charges: {}", chargueCreditCardRequest.getCreditCardId());
        OperationResponseDto operationResponse =
                creditCardService.chargeCreditCard(chargueCreditCardRequest);

        return ResponseEntity.ok(
                responseMapper.entityOperationResponseToApiResponseDto(operationResponse));
    }

    @Override
    public ResponseEntity<ApiResponseDto> paymentCreditCard(
            PaymentCreditProductRequestDto paymentCreditProductRequest) {

        log.info("Processing credit card payment: {}", paymentCreditProductRequest.getCreditId());
        OperationResponseDto operationResponse =
                creditCardService.paymentCreditCard(paymentCreditProductRequest);

        return ResponseEntity.ok(
                responseMapper.entityOperationResponseToApiResponseDto(operationResponse));
    }

    @Override
    public ResponseEntity<ApiResponseDto> updateCreditCardBalance(
            String id, BalanceUpdateRequestDto balanceUpdateRequest) {

        log.info("Updating credit card balance: {}", id);
        OperationResponseDto operationResponse =
                creditCardService.updateCreditCardBalance(id, balanceUpdateRequest);

        return ResponseEntity.ok(
                responseMapper.entityOperationResponseToApiResponseDto(operationResponse));
    }

    @Override
    public ResponseEntity<CreditCardBalanceResponseDto> getCreditCardBalance(String id) {

        log.info("Obtaining credit card balance: {}", id);
        CreditCard creditCard = creditCardService.getCreditCardById(id);

        return ResponseEntity.ok(
                creditCardMapper.creditCardEntityToCreditCardBalanceResponseDto(creditCard));
    }

    private ResponseEntity<CreditCardResponseDto> createCreditCardFallback(
            CreditCardRequestDto creditCardRequest, Exception e) {
        log.error("Fallback for create credit card: {}", e.getMessage());
        return new ResponseEntity(
                "We are experiencing some errors. Please try again later", HttpStatus.OK);
    }
}
