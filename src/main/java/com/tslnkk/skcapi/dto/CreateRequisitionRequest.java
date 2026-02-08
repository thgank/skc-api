package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на создание новой заявки на закупку")
public record CreateRequisitionRequest(

        @Schema(description = "Идентификатор организатора", example = "user-123")
        @NotBlank(message = "Идентификатор организатора обязателен")
        String organizerId
) {
}
