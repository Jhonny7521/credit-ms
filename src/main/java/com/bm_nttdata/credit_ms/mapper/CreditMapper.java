package com.bm_nttdata.credit_ms.mapper;

import com.bm_nttdata.credit_ms.entity.Credit;
import com.bm_nttdata.credit_ms.model.BalanceUpdateResponseDTO;
import com.bm_nttdata.credit_ms.model.CreditBalanceResponseDTO;
import com.bm_nttdata.credit_ms.model.CreditRequestDTO;
import com.bm_nttdata.credit_ms.model.CreditResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface CreditMapper {

    Credit creditRequestDtoToCreditEntity(CreditRequestDTO creditRequestDTO);

    CreditResponseDTO creditEntityToCreditResponseDto(Credit credit);

    BalanceUpdateResponseDTO creditEntityToBalanceUpdateResponseDto(Credit credit);

    @Mapping(target = "originalAmount", source = "amount")
    @Mapping(target = "currentBalance", source = "balance")
    @Mapping(target = "nextPaymentAmount", ignore = true)
    @Mapping(target = "nextPaymentDate", source = "nextPaymentDate")
    @Mapping(target = "daysOverdue", ignore = true)
    CreditBalanceResponseDTO creditEntityToCreditBalanceResponseDto(Credit credit);

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
