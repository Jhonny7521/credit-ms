package com.bm_nttdata.credit_ms.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResponseDTO {

    private String status;
    private String message;
    private String error;

}
