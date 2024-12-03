package com.bm_nttdata.credit_ms.service.impl;

import com.bm_nttdata.credit_ms.client.CustomerClient;
import com.bm_nttdata.credit_ms.dto.CustomerDto;
import com.bm_nttdata.credit_ms.dto.OperationResponseDto;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.enums.CardStatusEnum;
import com.bm_nttdata.credit_ms.exception.ApiInvalidRequestException;
import com.bm_nttdata.credit_ms.exception.BusinessRuleException;
import com.bm_nttdata.credit_ms.exception.CreditNotFoundException;
import com.bm_nttdata.credit_ms.exception.ServiceException;
import com.bm_nttdata.credit_ms.mapper.CreditCardMapper;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDto;
import com.bm_nttdata.credit_ms.model.ChargueCreditCardRequestDto;
import com.bm_nttdata.credit_ms.model.CreditCardRequestDto;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDto;
import com.bm_nttdata.credit_ms.repository.CreditCardRepository;
import com.bm_nttdata.credit_ms.service.CreditCardInstallmentService;
import com.bm_nttdata.credit_ms.service.CreditCardService;
import com.bm_nttdata.credit_ms.util.CardNumberGenerator;
import com.bm_nttdata.credit_ms.util.MonthlyInstallmentCalculator;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de gestión de tarjetas de crédito.
 * Maneja la lógica de negocio para las operaciones CRUD de tarjetas de crédito,
 * así como el procesamiento de cargos, pagos y actualizaciones de saldo.
 */
@Slf4j
@Transactional
@Service
public class CreditCardServiceImpl implements CreditCardService {

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
    private CreditCardInstallmentService cardInstallmentService;

    /**
     * Obtiene todas las tarjetas de crédito de un cliente.
     *
     * @param customerId ID del cliente
     * @return Lista de tarjetas de crédito del cliente
     * @throws ApiInvalidRequestException si no se envia un Id de cliente
     */
    @Override
    public List<CreditCard> getAllCreditCards(String customerId) {

        if (customerId == null) {
            throw new ApiInvalidRequestException("Customer id is required");
        }

        return creditCardRepository.findByCustomerId(customerId);
    }

    /**
     * Obtiene una tarjeta de crédito por su ID.
     *
     * @param id ID de la tarjeta de crédito
     * @return Tarjeta de crédito encontrada
     * @throws CreditNotFoundException si no se encuentra una tarjeta de crédito con el id enviado.
     */
    @Override
    public CreditCard getCreditCardById(String id) {

        log.info("Querying credit card data: {}", id);
        return creditCardRepository.findById(id)
                .orElseThrow(() ->
                        new CreditNotFoundException("Credit Card not found with id: " + id));
    }

    /**
     * Crea una nueva tarjeta de crédito.
     *
     * @param creditCardRequest DTO con la información de la nueva tarjeta
     * @return Tarjeta de crédito creada
     * @throws ServiceException Si ocurre un error durante la creación
     */
    @Override
    public CreditCard createCreditCard(CreditCardRequestDto creditCardRequest) {

        CustomerDto customer;

        try {
            customer = customerClient.getCustomerById(creditCardRequest.getCustomerId());
        } catch (FeignException e) {
            log.error("Error calling customer service: {}", e.getMessage());
            throw new ServiceException("Error retrieving customer information: " + e.getMessage());
        }

        validateCreditCreation(customer, creditCardRequest);
        CreditCard creditCard = initializeCreditCard(
                creditCardMapper.creditCardRequestDtoToCreditCardEntity(creditCardRequest));

        try {
            return creditCardRepository.save(creditCard);
        } catch (Exception e) {
            log.error("Unexpected error while saving credit card: {}", e.getMessage());
            throw new ServiceException("Unexpected error creating credit card" + e.getMessage());
        }
    }

    /**
     * Realiza un cargo a una tarjeta de crédito.
     *
     * @param chargueCreditCardRequest DTO con la información del cargo
     * @return Respuesta de la operación
     */
    @Override
    public OperationResponseDto chargeCreditCard(
            ChargueCreditCardRequestDto chargueCreditCardRequest) {

        log.info("Starting credit card charge process: {}",
                chargueCreditCardRequest.getCreditCardId());

        try {
            CreditCard creditCard =
                    getCreditCardById(chargueCreditCardRequest.getCreditCardId());
            BigDecimal chargeAmount = chargueCreditCardRequest.getChargeAmount();

            if (creditCard.getAvailableCredit().compareTo(chargeAmount) < 0) {

                return OperationResponseDto.builder()
                        .status("FAILED")
                        .message("Unprocessed charge")
                        .error("Insufficient available credit")
                        .build();
            }

            if (chargueCreditCardRequest.getTotalInstallment() > 1) {
                chargeAmount = installmentCalculator.calculateMonthlyPayment(
                        chargueCreditCardRequest.getChargeAmount(),
                        BigDecimal.valueOf(creditCard.getInterestRate()),
                        chargueCreditCardRequest.getTotalInstallment());
            }

            cardInstallmentService.createCharges(
                    chargeAmount, chargueCreditCardRequest.getTotalInstallment(),
                    creditCard.getId(), creditCard.getPaymentDate());

            BalanceUpdateRequestDto balanceUpdateRequest = new BalanceUpdateRequestDto();
            balanceUpdateRequest.setTransactionAmount(chargeAmount);
            balanceUpdateRequest.setTransactionType(
                    BalanceUpdateRequestDto.TransactionTypeEnum.CREDIT_CHARGE);

            OperationResponseDto operationResponse =
                    updateCreditCardBalance(creditCard.getId(), balanceUpdateRequest);

            if (!operationResponse.getStatus().equals("SUCCESS")) {

                return operationResponse;
            }

            log.info("Credit card charge processed successfully: {}", creditCard.getId());
            operationResponse.setMessage("Charge successfully processed");

            return operationResponse;

        } catch (Exception e) {
            log.error("Unexpected error during credit card charge process: {}", e.getMessage());
            return OperationResponseDto.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error processing credit card charge" + e.getMessage())
                    .build();
        }
    }

    /**
     * Procesa un pago a una tarjeta de crédito.
     *
     * @param paymentCreditProductRequest DTO con la información del pago
     * @return Respuesta de la operación
     */
    @Override
    public OperationResponseDto paymentCreditCard(
            PaymentCreditProductRequestDto paymentCreditProductRequest) {

        log.info("Initiating payment processing on credit card: {}",
                paymentCreditProductRequest.getCreditId());

        try {
            CreditCard creditCard = getCreditCardById(paymentCreditProductRequest.getCreditId());
            BigDecimal amountPaid = cardInstallmentService.payBillMonth(
                    paymentCreditProductRequest.getAmount(),
                    creditCard.getId(),
                    creditCard.getPaymentDate());

            BalanceUpdateRequestDto balanceUpdateRequest = new BalanceUpdateRequestDto();
            balanceUpdateRequest.setTransactionAmount(amountPaid);
            balanceUpdateRequest.setTransactionType(
                    BalanceUpdateRequestDto.TransactionTypeEnum.PAYMENT);

            OperationResponseDto operationResponse =
                    updateCreditCardBalance(creditCard.getId(), balanceUpdateRequest);

            if (!operationResponse.getStatus().equals("SUCCESS")) {

                return operationResponse;
            }

            log.info("Payment processed successfully: {}", creditCard.getId());
            operationResponse.setMessage("Payment successfully processed");

            return operationResponse;

        } catch (Exception e) {
            log.error("Unexpected error paying monthly credit card bill: {}", e.getMessage());
            return OperationResponseDto.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error when paying monthly credit card bill: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Actualiza el saldo de una tarjeta de crédito.
     *
     * @param id ID de la tarjeta de crédito
     * @param balanceUpdateRequest DTO con la información de actualización del saldo
     * @return Respuesta de la operación
     */
    @Override
    public OperationResponseDto updateCreditCardBalance(
            String id, BalanceUpdateRequestDto balanceUpdateRequest) {

        log.info("Initiating credit card balance update: {}", id);

        try {
            CreditCard creditCard = getCreditCardById(id);
            String transactionType = balanceUpdateRequest.getTransactionType().getValue();
            BigDecimal transactionAmount = balanceUpdateRequest.getTransactionAmount();

            switch (transactionType) {
                case "PAYMENT":
                    creditCard.setAvailableCredit(
                            creditCard.getAvailableCredit().add(transactionAmount));
                    break;

                case "CREDIT_CHARGE":
                    if (creditCard.getAvailableCredit().compareTo(transactionAmount) < 0) {
                        return OperationResponseDto.builder()
                                .status("FAILED")
                                .message("Unprocessed charge")
                                .error("Insufficient available credit")
                                .build();
                    }

                    creditCard.setAvailableCredit(
                            creditCard.getAvailableCredit().subtract(transactionAmount));
                    break;

                default:
                    return OperationResponseDto.builder()
                            .status("FAILED")
                            .message("Unprocessed charge")
                            .error("Incorrect transaction type")
                            .build();
            }

            creditCard.setUpdatedAt(LocalDateTime.now());

            creditCardRepository.save(creditCard);

            log.info(" *** Balance update successful *** ");

            return OperationResponseDto.builder()
                    .status("SUCCESS")
                    .message("Balance update successful")
                    .build();

        } catch (Exception e) {
            log.error("Unexpected error while updating credit card balance: {}", e.getMessage());
            return OperationResponseDto.builder()
                    .status("FAILED")
                    .message("Unprocessed charge")
                    .error("Error while updating monthly credit card balance: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Elimina una tarjeta de crédito.
     *
     * @param id ID de la tarjeta de crédito a eliminar
     */
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

    /**
     * Valida la eliminación de una tarjeta de crédito.
     * Verifica que la tarjeta no tenga saldo pendiente.
     *
     * @param creditCard Tarjeta de crédito a validar
     * @throws BusinessRuleException si la tarjeta tiene saldo pendiente
     */
    private void validateCreditCardDeletion(CreditCard creditCard) {
        if (creditCard.getCreditLimit().compareTo(creditCard.getAvailableCredit()) != 0) {
            throw new BusinessRuleException(
                    "A credit card with an outstanding balance can't be deleted.");
        }
    }

    /**
     * Valida la creación de una tarjeta de crédito.
     * Verifica que el tipo de tarjeta coincida con el tipo de cliente.
     *
     * @param customer Cliente que solicita la tarjeta
     * @param creditCardRequest Solicitud de tarjeta de crédito
     * @throws BusinessRuleException si el tipo de tarjeta no es compatible con el tipo de cliente
     */
    private void validateCreditCreation(
            CustomerDto customer, CreditCardRequestDto creditCardRequest) {

        if (customer.getCustomerType().equals("PERSONAL")) {

            if (creditCardRequest.getCardType().getValue().equals("BUSINESS")) {
                throw new BusinessRuleException(
                        "Personal client can't apply for Business credit card");
            }
        } else if (customer.getCustomerType().equals("BUSINESS")) {
            if (creditCardRequest.getCardType().getValue().equals("PERSONAL")) {
                throw new BusinessRuleException(
                        "Business client can't apply for Personal credit card");
            }
        }
    }

    /**
     * Inicializa una nueva tarjeta de crédito con valores predeterminados.
     * Genera el número de tarjeta, establece el crédito disponible igual al límite,
     * activa la tarjeta y establece las marcas de tiempo.
     *
     * @param creditCard Tarjeta de crédito a inicializar
     * @return Tarjeta de crédito inicializada
     */
    private CreditCard initializeCreditCard(CreditCard creditCard) {

        creditCard.setCardNumber(cardNumberGenerator.generateCardNumber());
        creditCard.setAvailableCredit(creditCard.getCreditLimit());
        creditCard.setStatus(CardStatusEnum.valueOf("ACTIVE"));
        creditCard.setCreatedAt(LocalDateTime.now());
        creditCard.setUpdatedAt(LocalDateTime.now());

        return creditCard;
    }
}
