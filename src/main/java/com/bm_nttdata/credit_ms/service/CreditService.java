package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.dto.OperationResponseDto;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDto;
import com.bm_nttdata.credit_ms.model.CreditRequestDto;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDto;
import java.util.List;

/**
 * Servicio que gestiona las operaciones principales de créditos.
 * Maneja la creación, consulta, actualización y eliminación de créditos,
 * así como las operaciones de pago y actualización de saldo.
 */
public interface CreditService {

    /**
     * Obtiene todos los créditos de un cliente.
     *
     * @param customerId ID del cliente
     * @return Lista de créditos del cliente
     */
    List<Credit> getAllCredits(String customerId);

    /**
     * Obtiene un crédito por su ID.
     *
     * @param id ID del crédito
     * @return Crédito encontrado
     */
    Credit getCreditById(String id);

    /**
     * Crea un nuevo crédito.
     *
     * @param creditRequest DTO con la información del nuevo crédito
     * @return Crédito creado
     */
    Credit createCredit(CreditRequestDto creditRequest);

    /**
     * Procesa un pago a un crédito.
     *
     * @param paymentCreditProductRequest DTO con la información del pago
     * @return Respuesta de la operación
     */
    OperationResponseDto paymentCredit(PaymentCreditProductRequestDto paymentCreditProductRequest);

    /**
     * Actualiza el saldo de un crédito.
     *
     * @param id ID del crédito
     * @param balanceUpdateRequest DTO con la información de actualización del saldo
     * @return Respuesta de la operación
     */
    OperationResponseDto updateCreditBalance(
            String id, BalanceUpdateRequestDto balanceUpdateRequest);

    /**
     * Elimina un crédito.
     *
     * @param id ID del crédito a eliminar
     */
    void deleteCredit(String id);

}
