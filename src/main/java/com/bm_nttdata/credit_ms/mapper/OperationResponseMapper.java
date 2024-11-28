package com.bm_nttdata.credit_ms.mapper;

import com.bm_nttdata.credit_ms.DTO.OperationResponseDTO;
import com.bm_nttdata.credit_ms.model.ApiResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OperationResponseMapper {

    ApiResponseDTO entityOperationResponseToApiResponseDTO(OperationResponseDTO operationResponseDTO);
}
