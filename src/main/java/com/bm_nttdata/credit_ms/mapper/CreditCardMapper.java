package com.bm_nttdata.credit_ms.mapper;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.entity.CreditCard;
import com.bm_nttdata.credit_ms.model.CreditCardRequestDTO;
import com.bm_nttdata.credit_ms.model.CreditCardResponseDTO;
import com.bm_nttdata.credit_ms.model.CreditRequestDTO;
import com.bm_nttdata.credit_ms.model.CreditResponseDTO;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    CreditCard creditCardRequestDtoToCreditCardEntity(CreditCardRequestDTO creditCardRequestDTO);

    CreditCardResponseDTO creditEntityToCreditResponseDto(CreditCard creditCard);

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
