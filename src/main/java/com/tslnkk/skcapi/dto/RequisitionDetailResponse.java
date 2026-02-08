package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Детальные данные заявки на закупку с позициями")
public record RequisitionDetailResponse(

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
        LocalDateTime updatedFrom,

        @Schema(description = "Позиции заявки")
        List<ItemResponse> items
) {
}
