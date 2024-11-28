package com.bm_nttdata.credit_ms.service;

import java.math.BigDecimal;

public interface ICreditCardInstallmentService {

    BigDecimal calculateCurrentMonthPayment(String creditCardId, int paymentDay);

    void createCharges(BigDecimal installmentAmount, int totalInstallments, String creditCardId, int paymentDate);

    BigDecimal payBillMonth(BigDecimal installmentAmount, String creditCardId, int paymentDate);

}
