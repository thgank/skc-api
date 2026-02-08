package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Сводка по заявке на закупку")
public record RequisitionSummaryResponse(

        @Schema(description = "Общая сумма без НДС", example = "36275.00")
        BigDecimal totalAmountWithoutVat,

        @Schema(description = "Общее количество по всем позициям", example = "150")
        BigDecimal totalQuantity,

        @Schema(description = "Минимальная дата поставки", example = "2025-03-15")
        LocalDate minDesiredDeliveryDate,

        @Schema(description = "Максимальная дата поставки", example = "2025-04-01")
        LocalDate maxDesiredDeliveryDate,

        @Schema(description = "Количество позиций", example = "3")
        int itemCount,

        @Schema(description = "Валюта (всегда KZT)", example = "KZT")
        String currency
) {
}
