package com.bm_nttdata.credit_ms.util;

import com.bm_nttdata.credit_ms.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class MonthlyInstallmentCalculator {

    public BigDecimal calculateMonthlyPayment(BigDecimal creditAmount, BigDecimal interestRate, int months){

        log.info("Calculating monthly payment");

        try {
            BigDecimal monthlyRate = BigDecimal.valueOf(Math.pow(1 + (interestRate.doubleValue() / 100), 1.0 / 12) - 1); // r = ((1 + TEA)^(1/12)) - 1
            BigDecimal ratePowerMonths = monthlyRate.add(BigDecimal.ONE).pow(months); // (1 + r)^n
            BigDecimal monthlyPayment = creditAmount
                    .multiply(monthlyRate)
                    .multiply(ratePowerMonths)
                    .divide(ratePowerMonths.subtract(BigDecimal.ONE), 2 , RoundingMode.HALF_UP);

            return monthlyPayment;
        } catch (Exception e){
            log.error("Unexpected error while calculating the monthly payment: {}", e.getMessage());
            throw new ServiceException("Unexpected error while calculating the monthly payment");
        }
    }
}
