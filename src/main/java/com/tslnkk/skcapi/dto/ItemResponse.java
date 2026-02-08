package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Ответ с данными позиции заявки")
public record ItemResponse(

        @Schema(description = "ID позиции")
        Long id,

        @Schema(description = "Порядковый номер строки в заявке")
        Integer rowNumber,

        @Schema(description = "Код номенклатуры")
        String nomenclatureCode,

        @Schema(description = "Наименование номенклатуры")
        String nomenclatureName,

        @Schema(description = "Количество")
        BigDecimal quantity,

        @Schema(description = "Код единицы измерения")
        String unitCode,

        @Schema(description = "Цена без НДС")
        BigDecimal priceWithoutVat,

        @Schema(description = "Желаемая дата поставки")
        LocalDate desiredDeliveryDate,

        @Schema(description = "Комментарий")
        String comment,

        @Schema(description = "Версия (для оптимистичной блокировки)")
        Long version
) {
}
