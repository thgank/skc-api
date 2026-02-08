package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Стандартный формат ошибки API")
public record ErrorResponse(

        @Schema(description = "Код ошибки", example = "REQUISITION_NOT_IN_DRAFT")
        String errorCode,

        @Schema(description = "Описание ошибки", example = "Операция запрещена: заявка не в статусе DRAFT")
        String message,

        @Schema(description = "Поле, вызвавшее ошибку", example = "quantity", nullable = true)
        String field,

        @Schema(description = "Отвергнутое значение", example = "-1", nullable = true)
        Object rejectedValue
) {
}
