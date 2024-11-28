package com.bm_nttdata.credit_ms.entity;

import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_card_installment")
public class CreditCardInstallment {

    @Id
    private String id;
    private String purchaseId;
    private String creditCardId;
    private int installmentNumber;
    private int totalInstallments;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
    private LocalDate dueDate;
    private InstallmentStatusEnum status;
    private long daysOverdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
