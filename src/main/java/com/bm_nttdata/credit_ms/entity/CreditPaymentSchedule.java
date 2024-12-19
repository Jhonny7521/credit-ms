package com.bm_nttdata.credit_ms.entity;

import com.bm_nttdata.credit_ms.enums.InstallmentStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Representa un cronograma de pagos de créditos o tarjetas de credito en el sistema bancario.
 * Esta clase gestiona la información del calendario de pagos para créditos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_payment_schedule")
public class CreditPaymentSchedule {

    @Id
    private String id;
    private String creditId;
    private BigDecimal creditAmount;
    private Integer installmentNumber;
    private BigDecimal installmentAmount;
    private LocalDate dueDate;
    private long daysOverdue;
    private BigDecimal interest;
    private InstallmentStatusEnum status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
