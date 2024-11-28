package com.bm_nttdata.credit_ms.mapper;

import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    CreditCard creditCardRequestDtoToCreditCardEntity(CreditCardRequestDTO creditCardRequestDTO);

    CreditCardResponseDTO creditCardEntityToCreditCardResponseDto(CreditCard creditCard);

    @Mapping(target = "creditCardId", source = "id")
    CreditCardBalanceResponseDTO creditCardEntityToCreditCardBalanceResponseDto(CreditCard creditCard);

    default OffsetDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.toLocalDateTime();
    }

}
