package com.bm_nttdata.credit_ms.entity;

import com.bm_nttdata.credit_ms.enums.CardStatusEnum;
import com.bm_nttdata.credit_ms.enums.CreditTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_cards")
public class CreditCard {

    @Id
    private String id;
    private String customerId;
    private String cardNumber;
    private CreditTypeEnum cardType;
    private BigDecimal creditLimit;
    private Double interestRate;
    private BigDecimal availableCredit;
    private int paymentDate;
    private CardStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
