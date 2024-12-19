package com.bm_nttdata.credit_ms.mapper;

import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.model.CreditCardBalanceResponseDto;
import com.bm_nttdata.credit_ms.model.CreditCardRequestDto;
import com.bm_nttdata.credit_ms.model.CreditCardResponseDto;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


/**
 * Interfaz de mapeo para la conversión entre entidades y DTOs relacionados con tarjetas de crédito.
 * Utiliza MapStruct para la implementación automática de las conversiones.
 */
@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    /**
     * Convierte un objeto DTO de solicitud de tarjeta de crédito a una entidad de tarjeta
     * de crédito.
     *
     * @param creditCardRequest DTO con la información de solicitud de tarjeta de crédito
     * @return Entidad CreditCard con los datos mapeados
     */
    CreditCard creditCardRequestDtoToCreditCardEntity(CreditCardRequestDto creditCardRequest);

    /**
     * Convierte una entidad de tarjeta de crédito a un DTO de respuesta.
     *
     * @param creditCard Entidad de tarjeta de crédito a convertir
     * @return DTO con la información de respuesta de la tarjeta de crédito
     */
    CreditCardResponseDto creditCardEntityToCreditCardResponseDto(CreditCard creditCard);

    /**
     * Convierte una entidad de tarjeta de crédito a un DTO de respuesta de balance.
     * El campo 'id' de la entidad se mapea al campo 'creditCardId' del DTO.
     *
     * @param creditCard Entidad de tarjeta de crédito a convertir
     * @return DTO con la información de balance de la tarjeta de crédito
     */
    @Mapping(target = "creditCardId", source = "id")
    CreditCardBalanceResponseDto creditCardEntityToCreditCardBalanceResponseDto(
            CreditCard creditCard);

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
