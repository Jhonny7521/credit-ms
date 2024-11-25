package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.entity.CreditPaymentSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ICreditPaymentScheduleService {

    void createPaymentSchedule(Credit credit);

    BigDecimal calculateMonthlyPayment(BigDecimal creditAmount, BigDecimal interestRate, int months);

    void updatePaymentSchedule(Credit credit);

}
