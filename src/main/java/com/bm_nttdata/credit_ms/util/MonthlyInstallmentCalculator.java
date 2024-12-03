package com.bm_nttdata.credit_ms.util;

import com.bm_nttdata.credit_ms.exception.ServiceException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Calculadora de cuotas mensuales para créditos.
 * Implementa el cálculo de cuotas mensuales utilizando una fórmula financiera.
 */
@Slf4j
@Component
public class MonthlyInstallmentCalculator {

    /**
     * Calcula la cuota mensual de un crédito basado en el monto, tasa de interés y plazo.
     * Utiliza la fórmula: C = P * (r * (1 + r)^n) / ((1 + r)^n - 1)
     * Donde:
     * C = Cuota mensual
     * P = Monto del crédito
     * r = Tasa de interés mensual (calculada a partir de la TEA)
     * n = Número de meses
     *
     * @param creditAmount Monto total del crédito
     * @param interestRate Tasa de interés efectiva anual (TEA) en porcentaje
     * @param months Plazo del crédito en meses
     * @return Monto de la cuota mensual calculada, redondeada a 2 decimales
     * @throws ServiceException si ocurre un error durante el cálculo
     */
    public BigDecimal calculateMonthlyPayment(
            BigDecimal creditAmount, BigDecimal interestRate, int months) {

        log.info("Calculating monthly payment");

        try {
            BigDecimal monthlyRate = BigDecimal.valueOf(
                    Math.pow(1 + (interestRate.doubleValue() / 100), 1.0 / 12) - 1);
            BigDecimal ratePowerMonths = monthlyRate.add(BigDecimal.ONE).pow(months);
            BigDecimal monthlyPayment = creditAmount
                    .multiply(monthlyRate)
                    .multiply(ratePowerMonths)
                    .divide(ratePowerMonths.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

            return monthlyPayment;
        } catch (Exception e) {
            log.error("Unexpected error while calculating the monthly payment: {}", e.getMessage());
            throw new ServiceException("Unexpected error while calculating the monthly payment");
        }
    }
}
