package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на частичное обновление заявки (только DRAFT)")
public record PatchRequisitionRequest(

        @Schema(description = "Новый идентификатор организатора", example = "user-456")
        String organizerId
) {
}
