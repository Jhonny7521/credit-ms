package com.bm_nttdata.credit_ms.mapper;

import com.bm_nttdata.credit_ms.dto.OperationResponseDto;
import com.bm_nttdata.credit_ms.model.ApiResponseDto;
import org.mapstruct.Mapper;

/**
 * Interfaz de mapeo para la conversión de respuestas de operaciones a DTOs de respuesta API.
 * Utiliza MapStruct para la implementación automática de las conversiones.
 */
@Mapper(componentModel = "spring")
public interface OperationResponseMapper {

    /**
     * Convierte una respuesta de operación a un DTO de respuesta API.
     *
     * @param operationResponse DTO con la respuesta de la operación a convertir
     * @return DTO con el formato estándar de respuesta API
     */
    ApiResponseDto entityOperationResponseToApiResponseDto(OperationResponseDto operationResponse);
}
