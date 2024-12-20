package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.dto.PaymentDetailsDto;
import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import java.math.BigDecimal;

/**
 * Servicio que gestiona las operaciones relacionadas con las cuotas de tarjetas de crédito.
 * Proporciona funcionalidades para el cálculo y gestión de pagos mensuales y cargos.
 */
public interface CreditCardInstallmentService {

    /**
     * Calcula el pago del mes actual para una tarjeta de crédito específica.
     *
     * @param creditCardId ID de la tarjeta de crédito
     * @param paymentDay Día de pago establecido
     * @return Dto con el detalle a pagar en el mes actual
     */
    PaymentDetailsDto calculateCurrentMonthPayment(String creditCardId, int paymentDay);

    /**
     * Crea los cargos para un plan de cuotas de tarjeta de crédito.
     *
     * @param installmentAmount Monto de cada cuota
     * @param totalInstallments Número total de cuotas
     * @param creditCardId ID de la tarjeta de crédito
     * @param paymentDate Día de pago mensual
     */
    void createCharges(
            BigDecimal installmentAmount, int totalInstallments,
            String creditCardId, int paymentDate);

    /**
     * Procesa el pago mensual de una tarjeta de crédito.
     *
     * @param installmentAmount Monto de la cuota a pagar
     * @param creditCardId ID de la tarjeta de crédito
     * @param paymentDate Fecha de pago
     * @return Dto con el detalle de lo pagado pagado
     */
    PaymentDetailsDto payBillMonth(
            BigDecimal installmentAmount, String creditCardId, int paymentDate);

    /**
     * Verifica si existen cuotas vencidas para una tarjeta de crédito.
     *
     * @param creditId identificador de tarjeta de crédito
     * @param status estatus de la cuota
     * @return resultado si la tarjeta de credito cuenta con deudas vencidas
     */
    boolean getCustomerCreditCardDebts(String creditId, InstallmentStatusEnum status);

}
