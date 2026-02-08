package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Данные заявки на закупку (список)")
public record RequisitionResponse(

        @Schema(description = "ID заявки")
        Long id,

        @Schema(description = "Номер заявки")
        String number,

        @Schema(description = "Текущий статус")
        String status,

        @Schema(description = "Идентификатор организатора")
        String organizerId,

        @Schema(description = "Итоговая сумма без НДС")
        BigDecimal totalLotSumNoNds,

        @Schema(description = "Дата создания")
        LocalDateTime createdFrom,

        @Schema(description = "Дата обновления")
        LocalDateTime updatedFrom
) {
}
