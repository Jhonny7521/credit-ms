package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.dto.OperationResponseDto;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDto;
import com.bm_nttdata.credit_ms.model.ChargueCreditCardRequestDto;
import com.bm_nttdata.credit_ms.model.CreditCardRequestDto;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDto;
import java.util.List;

/**
 * Servicio que gestiona las operaciones principales de tarjetas de crédito.
 * Maneja la creación, consulta, actualización y eliminación de tarjetas de crédito,
 * así como las operaciones de cargo, pago y transferencia.
 */
public interface CreditCardService {

    /**
     * Obtiene todas las tarjetas de crédito de un cliente.
     *
     * @param customerId ID del cliente
     * @return Lista de tarjetas de crédito del cliente
     */
    List<CreditCard> getAllCreditCards(String customerId);

    /**
     * Obtiene una tarjeta de crédito por su ID.
     *
     * @param id ID de la tarjeta de crédito
     * @return Tarjeta de crédito encontrada
     */
    CreditCard getCreditCardById(String id);

    /**
     * Crea una nueva tarjeta de crédito.
     *
     * @param creditCardRequest DTO con la información de la nueva tarjeta
     * @return Tarjeta de crédito creada
     */
    CreditCard createCreditCard(CreditCardRequestDto creditCardRequest);

    /**
     * Realiza un cargo a una tarjeta de crédito.
     *
     * @param chargueCreditCardRequest DTO con la información del cargo
     * @return Respuesta de la operación
     */
    OperationResponseDto chargeCreditCard(ChargueCreditCardRequestDto chargueCreditCardRequest);

    /**
     * Procesa un pago a una tarjeta de crédito.
     *
     * @param paymentCreditProductRequest DTO con la información del pago
     * @return Respuesta de la operación
     */
    OperationResponseDto paymentCreditCard(
            PaymentCreditProductRequestDto paymentCreditProductRequest);

    /**
     * Actualiza el saldo de una tarjeta de crédito.
     *
     * @param id ID de la tarjeta de crédito
     * @param balanceUpdateRequest DTO con la información de actualización del saldo
     * @return Respuesta de la operación
     */
    OperationResponseDto updateCreditCardBalance(
            String id, BalanceUpdateRequestDto balanceUpdateRequest);

    /**
     * Elimina una tarjeta de crédito.
     *
     * @param id ID de la tarjeta de crédito a eliminar
     */
    void deleteCredit(String id);
}
