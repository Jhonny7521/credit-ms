package com.bm_nttdata.credit_ms.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clase para representar los detalles de un pago
 * obtenidos en el calculo de un pago mensual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailsDto {

    private BigDecimal paymentAmount;
    private BigDecimal paymentFee;
    private BigDecimal totalPayment;

}
