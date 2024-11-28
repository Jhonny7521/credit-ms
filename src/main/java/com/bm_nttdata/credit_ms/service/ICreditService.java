package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.DTO.OperationResponseDTO;
import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDTO;
import com.bm_nttdata.credit_ms.model.CreditRequestDTO;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDTO;

import java.util.List;

public interface ICreditService {

    List<Credit> getAllCredits(String customerId);

    Credit getCreditById(String id);

    Credit createCredit(CreditRequestDTO creditRequestDTO);

    OperationResponseDTO paymentCredit(PaymentCreditProductRequestDTO paymentCreditProductRequestDTO);

    OperationResponseDTO updateCreditBalance(String id, BalanceUpdateRequestDTO balanceUpdateRequestDTO);

    void deleteCredit(String id);

}
