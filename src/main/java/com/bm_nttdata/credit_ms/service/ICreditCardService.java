package com.bm_nttdata.credit_ms.service;

import com.bm_nttdata.credit_ms.DTO.OperationResponseDTO;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.model.BalanceUpdateRequestDTO;
import com.bm_nttdata.credit_ms.model.ChargueCreditCardRequestDTO;
import com.bm_nttdata.credit_ms.model.CreditCardRequestDTO;
import com.bm_nttdata.credit_ms.model.PaymentCreditProductRequestDTO;

import java.util.List;

public interface ICreditCardService {

    List<CreditCard> getAllCreditCards(String customerId);

    CreditCard getCreditCardById(String id);

    CreditCard createCreditCard(CreditCardRequestDTO creditCardRequestDTO);

    OperationResponseDTO chargeCreditCard(ChargueCreditCardRequestDTO chargueCreditCardRequestDTO);

    OperationResponseDTO paymentCreditCard(PaymentCreditProductRequestDTO paymentCreditProductRequestDTO);

    OperationResponseDTO updateCreditCardBalance(String id, BalanceUpdateRequestDTO balanceUpdateRequestDTO);
}
