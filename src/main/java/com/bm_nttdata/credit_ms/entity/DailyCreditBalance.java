package com.bm_nttdata.credit_ms.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Clase que representa una entidad de balance diario en el sistema bancario.
 * Esta clase maneja el almacenamiento y gestión de los balances diarios
 * de créditos y tarjetas de créditos.
 */
@Data
@Builder
@Document(collection = "daily_credit_balances")
public class DailyCreditBalance {

    @Id
    private String id;
    private String creditProductId;
    private LocalDate date;
    private BigDecimal balance;

}
