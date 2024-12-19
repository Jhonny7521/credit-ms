package com.bm_nttdata.credit_ms.mapper;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.model.CreditBalanceResponseDto;
import com.bm_nttdata.credit_ms.model.CreditRequestDto;
import com.bm_nttdata.credit_ms.model.CreditResponseDto;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Interfaz de mapeo para la conversión entre entidades y DTOs relacionados con créditos.
 * Utiliza MapStruct para la implementación automática de las conversiones.
 */
@Mapper(componentModel = "spring")
public interface CreditMapper {

    /**
     * Convierte un objeto DTO de solicitud de crédito a una entidad de crédito.
     *
     * @param creditRequest DTO con la información de solicitud de crédito
     * @return Entidad Credit con los datos mapeados
     */
    Credit creditRequestDtoToCreditEntity(CreditRequestDto creditRequest);

    /**
     * Convierte una entidad de crédito a un DTO de respuesta.
     *
     * @param credit Entidad de crédito a convertir
     * @return DTO con la información de respuesta del crédito
     */
    CreditResponseDto creditEntityToCreditResponseDto(Credit credit);

    /**
     * Convierte una entidad de crédito a un DTO de respuesta de saldo.
     * Realiza los siguientes mapeos específicos:
     * - El campo 'amount' se mapea a 'originalAmount'
     * - El campo 'balance' se mapea a 'currentBalance'
     * - El campo 'nextPaymentDate' se mantiene con el mismo nombre
     * - Los campos 'nextPaymentAmount' y 'daysOverdue' se ignoran en el mapeo
     *
     * @param credit Entidad de crédito a convertir
     * @return DTO con la información de balance del crédito
     */
    @Mapping(target = "originalAmount", source = "amount")
    @Mapping(target = "currentBalance", source = "balance")
    @Mapping(target = "nextPaymentAmount", ignore = true)
    @Mapping(target = "nextPaymentDate", source = "nextPaymentDate")
    @Mapping(target = "daysOverdue", ignore = true)
    CreditBalanceResponseDto creditEntityToCreditBalanceResponseDto(Credit credit);

    /**
     * Convierte un LocalDateTime a OffsetDateTime en UTC.
     *
     * @param localDateTime Fecha y hora local
     * @return Fecha y hora con zona horaria UTC
     */
    default OffsetDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    /**
     * Convierte un OffsetDateTime a LocalDateTime.
     *
     * @param offsetDateTime Fecha y hora con zona horaria
     * @return Fecha y hora local
     */
    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.toLocalDateTime();
    }
}
