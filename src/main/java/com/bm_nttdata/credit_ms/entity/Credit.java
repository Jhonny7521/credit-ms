package com.bm_nttdata.credit_ms.entity;

import com.bm_nttdata.credit_ms.enums.CreditStatusEnum;
import com.bm_nttdata.credit_ms.enums.CreditTypeEnum;
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
@Document(collection = "credits")
public class Credit {
    @Id
    private String id;
    private String customerId;
    private CreditTypeEnum creditType;
    private BigDecimal amount;
    private BigDecimal balance;
    private Integer term;
    private Double interestRate;
    private CreditStatusEnum status;
    private int paymentDay;
    private LocalDate nextPaymentDate;
    private BigDecimal nextPaymentAmount;
    private int nextPaymentInstallment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
