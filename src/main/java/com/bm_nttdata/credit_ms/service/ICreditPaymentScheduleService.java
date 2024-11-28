package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.entity.Credit;

import java.math.BigDecimal;

public interface ICreditPaymentScheduleService {

    BigDecimal calculateMonthlyPayment(String creditId, int installmentNumber);

    void createPaymentSchedule(Credit credit);

    BigDecimal payMonthlyInstallment(BigDecimal paymentAmount, String id, int paymentDay);
}
