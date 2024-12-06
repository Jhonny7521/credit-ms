package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.dto.PaymentDetailsDto;
import com.bm_nttdata.credit_ms.entity.Credit;
import java.math.BigDecimal;

/**
 * Servicio que gestiona los cronogramas de pago de créditos.
 * Maneja la creación y gestión de cronogramas de pago, así como el cálculo
 * y procesamiento de pagos mensuales.
 */
public interface CreditPaymentScheduleService {

    /**
     * Calcula el pago mensual considerando intereses por mora si aplica.
     * Si una cuota está vencida, calcula los intereses moratorios basados en los días de retraso.
     *
     * @param creditId ID del crédito
     * @param installmentNumber Número de cuota
     * @return Dto con el detalle a pagar en el mes actual
     */
    PaymentDetailsDto calculateMonthlyPayment(String creditId, int installmentNumber);

    /**
     * Crea un cronograma de pagos para un crédito.
     * Genera las cuotas mensuales para todo el período del crédito.
     *
     * @param credit Crédito para el cual se creará el cronograma
     */
    void createPaymentSchedule(Credit credit);

    /**
     * Procesa el pago de una cuota mensual.
     * Verifica que el monto del pago coincida con la deuda total y actualiza el estado
     * de las cuotas.
     *
     * @param paymentAmount Monto del pago
     * @param id ID del crédito
     * @param paymentDay Día de pago
     * @return Dto con el detalle de lo pagado
     */
    PaymentDetailsDto payMonthlyInstallment(BigDecimal paymentAmount, String id, int paymentDay);
}
