package com.tslnkk.skcapi.dto;

import com.tslnkk.skcapi.domain.RequisitionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на смену статуса заявки")
public record TransitionRequest(

        @Schema(description = "Целевой статус", example = "SUBMITTED")
        @NotNull(message = "Целевой статус обязателен")
        RequisitionStatus targetStatus
) {
}
