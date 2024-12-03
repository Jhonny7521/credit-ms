package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.client.CustomerClient;
import com.bm_nttdata.credit_ms.dto.CustomerDto;
import com.bm_nttdata.credit_ms.dto.OperationResponseDto;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.enums.CreditStatusEnum;
import com.bm_nttdata.credit_ms.exception.ApiInvalidRequestException;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.mapper.CreditMapper;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDto;
import com.bm_nttdata.credit_ms.model.CreditRequestDto;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDto;
import com.bm_nttdata.credit_ms.repository.CreditRepository;
import com.bm_nttdata.credit_ms.service.CreditPaymentScheduleService;
import com.bm_nttdata.credit_ms.service.CreditService;
import com.bm_nttdata.credit_ms.util.MonthlyInstallmentCalculator;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de créditos.
 * Gestiona las operaciones CRUD de créditos, validaciones de negocio,
 * y procesamiento de pagos y actualizaciones de saldo.
 */
@Slf4j
@Transactional
@Service
public class CreditServiceImpl implements CreditService {

    @Autowired
    private CreditRepository creditRepository;
    @Autowired
    private CreditMapper creditMapper;
    @Autowired
    private CreditPaymentScheduleService paymentScheduleService;
    @Autowired
    private CustomerClient customerClient;

    @Autowired
    private MonthlyInstallmentCalculator installmentCalculator;

    /**
     * Obtiene todos los créditos de un cliente.
     *
     * @param customerId ID del cliente
     * @return Lista de créditos del cliente
     * @throws ApiInvalidRequestException si no se envia un Id de cliente
     */
    @Override
    public List<Credit> getAllCredits(String customerId) {

        if (customerId == null) {
            throw new ApiInvalidRequestException("Customer id is required");
        }

        List<Credit> creditList = creditRepository.findByCustomerId(customerId);

        return creditList;
    }

    /**
     * Obtiene un crédito por su ID.
     *
     * @param id ID del crédito
     * @return Crédito encontrado
     * @throws CreditNotFoundException si no se encuentra un crédito con el id enviado.
     */
    @Override
    public Credit getCreditById(String id) {
        return creditRepository.findById(id)
                .orElseThrow(() -> new CreditNotFoundException("Credit not found with id: " + id));
    }

    /**
     * Crea un nuevo crédito.
     *
     * @param creditRequest DTO con la información del nuevo crédito
     * @return Crédito creado
     * @throws ServiceException Si ocurre un error durante la creación
     */
    @Override
    public Credit createCredit(CreditRequestDto creditRequest) {

        CustomerDto customer;

        try {
            customer = customerClient.getCustomerById(creditRequest.getCustomerId());
        } catch (FeignException e) {
            log.error("Error calling customer service: {}", e.getMessage());
            throw new ServiceException("Error retrieving customer information: " + e.getMessage());
        }

        validateCreditCreation(customer, creditRequest);
        BigDecimal monthlyPayment =
                installmentCalculator.calculateMonthlyPayment(
                        creditRequest.getAmount(),
                        creditRequest.getInterestRate(),
                        creditRequest.getTerm());
        Credit credit = initializeCredit(
                creditMapper.creditRequestDtoToCreditEntity(creditRequest), monthlyPayment);

        try {
            credit = creditRepository.save(credit);
            paymentScheduleService.createPaymentSchedule(credit);
            return credit;
        } catch (Exception e) {
            log.error("Unexpected error while saving credit: {}", e.getMessage());
            throw new ServiceException("Unexpected error creating credit" + e.getMessage());
        }
    }

    /**
     * Procesa un pago a un crédito.
     *
     * @param paymentCreditProductRequest DTO con la información del pago
     * @return Respuesta de la operación
     */
    @Override
    public OperationResponseDto paymentCredit(
            PaymentCreditProductRequestDto paymentCreditProductRequest) {

        log.info("Initiating payment processing on credit: {}",
                paymentCreditProductRequest.getCreditId());

        try {
            Credit credit = getCreditById(paymentCreditProductRequest.getCreditId());
            BigDecimal amountPaid = paymentScheduleService.payMonthlyInstallment(
                    paymentCreditProductRequest.getAmount(),
                    credit.getId(),
                    credit.getPaymentDay());

            BalanceUpdateRequestDto balanceUpdateRequest = new BalanceUpdateRequestDto();
            balanceUpdateRequest.setTransactionAmount(amountPaid);
            balanceUpdateRequest.setTransactionType(
                    BalanceUpdateRequestDto.TransactionTypeEnum.PAYMENT);

            OperationResponseDto operationResponse =
                    updateCreditBalance(credit.getId(), balanceUpdateRequest);

            if (!operationResponse.getStatus().equals("SUCCESS")) {

                return operationResponse;
            }

            log.info("Payment processed successfully: {}", credit.getId());
            operationResponse.setMessage("Payment successfully processed");

            return operationResponse;

        } catch (Exception e) {
            log.error("Error when paying monthly payment : {}", e.getMessage());
            return OperationResponseDto.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error when paying monthly payment: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Actualiza el saldo de un crédito.
     *
     * @param id ID del crédito
     * @param balanceUpdateRequest DTO con la información de actualización del saldo
     * @return Respuesta de la operación
     */
    @Override
    public OperationResponseDto updateCreditBalance(
            String id, BalanceUpdateRequestDto balanceUpdateRequest) {

        log.info("Initiating credit balance update: {}", id);

        try {
            Credit credit = getCreditById(id);
            String transactionType = balanceUpdateRequest.getTransactionType().getValue();
            BigDecimal transactionAmount = balanceUpdateRequest.getTransactionAmount();

            if (!transactionType.equals("PAYMENT")) {
                return OperationResponseDto.builder()
                        .status("FAILED")
                        .message("Unprocessed charge")
                        .error("Incorrect transaction type")
                        .build();
            }

            credit.setBalance(credit.getBalance().subtract(transactionAmount));
            credit.setNextPaymentDate(credit.getNextPaymentDate().plusMonths(1));
            credit.setNextPaymentInstallment(credit.getNextPaymentInstallment() + 1);
            credit.setUpdatedAt(LocalDateTime.now());

            creditRepository.save(credit);

            log.info(" *** Balance update successful *** ");

            return OperationResponseDto.builder()
                    .status("SUCCESS")
                    .message("Balance update successful")
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error while updating credit balance: {}", e.getMessage());
            return OperationResponseDto.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error while updating monthly credit balance: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Elimina un crédito.
     *
     * @param id ID del crédito a eliminar
     */
    @Override
    public void deleteCredit(String id) {

        log.info("Initiating credit deletion: {}", id);

        try {
            Credit credit = getCreditById(id);
            validateCreditDeletion(credit);

            creditRepository.delete(credit);
        } catch (Exception e) {
            log.error("Error deleting credit {}: {}", id, e.getMessage());
            throw new ServiceException("Error deleting credit: " + e.getMessage());
        }
    }

    /**
     * Valida la creación de un crédito.
     * Verifica las reglas de negocio para la creación de créditos según el tipo de cliente.
     *
     * @param customer Cliente que solicita el crédito
     * @param creditRequest Solicitud de crédito
     * @throws BusinessRuleException si no se cumplen las reglas de negocio
     */
    private void validateCreditCreation(CustomerDto customer, CreditRequestDto creditRequest) {

        if (customer.getCustomerType().equals("PERSONAL")) {
            long count = creditRepository.countByCustomerIdAndAmountGreaterThan(
                    customer.getId(), BigDecimal.valueOf(0L));
            if (count > 0) {
                throw new BusinessRuleException(
                        "Customer already has a credit with an outstanding balance");
            }

            if (creditRequest.getCreditType().getValue().equals("BUSINESS")) {
                throw new BusinessRuleException(
                        "Personal client can't apply for Business credit");
            }
        } else if (customer.getCustomerType().equals("BUSINESS")) {
            if (creditRequest.getCreditType().getValue().equals("PERSONAL")) {
                throw new BusinessRuleException(
                        "Business client can't apply for Personal credit");
            }
        }

        if (creditRequest.getInterestRate() == null || creditRequest.getInterestRate().equals("")) {
            throw new BusinessRuleException("Attribute InterestRate is required");
        }

        if (creditRequest.getTerm() == null || creditRequest.getTerm().equals("")) {
            throw new BusinessRuleException("Attribute Term is required");
        }
    }

    /**
     * Valida la eliminación de un crédito.
     * Verifica que el crédito no tenga saldo pendiente.
     *
     * @param credit Crédito a validar
     * @throws BusinessRuleException si el crédito tiene saldo pendiente
     */
    private void validateCreditDeletion(Credit credit) {
        if (credit.getBalance().intValue() > 0) {
            throw new BusinessRuleException(
                    "An credit with an outstanding balance can't be deleted.");
        }
    }

    /**
     * Inicializa un nuevo crédito con valores predeterminados.
     * Establece el saldo inicial, estado, fechas de pago y marcas de tiempo.
     *
     * @param credit Crédito a inicializar
     * @param monthlyPayment Monto de la cuota mensual calculada
     * @return Crédito inicializado
     */
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
