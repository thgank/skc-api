package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Запрос на создание позиции заявки")
public record CreateItemRequest(

        @Schema(description = "Код номенклатуры из справочника", example = "TRU-001")
        @NotBlank(message = "Код номенклатуры обязателен")
        String nomenclatureCode,

        @Schema(description = "Наименование номенклатуры (должно совпадать со справочником)", example = "Бумага офисная A4")
        @NotBlank(message = "Наименование номенклатуры обязательно")
        String nomenclatureName,

        @Schema(description = "Количество (> 0)", example = "100")
        @NotNull(message = "Количество обязательно")
        @Positive(message = "Количество должно быть больше 0")
        BigDecimal quantity,

        @Schema(description = "Код единицы измерения (должен быть разрешён для номенклатуры)", example = "PACK")
        @NotBlank(message = "Код единицы измерения обязателен")
        String unitCode,

        @Schema(description = "Цена без НДС (>= 0)", example = "350.00")
        @NotNull(message = "Цена без НДС обязательна")
        @PositiveOrZero(message = "Цена не может быть отрицательной")
        BigDecimal priceWithoutVat,

        @Schema(description = "Желаемая дата поставки (не ранее текущей даты + 3 дня)", example = "2025-04-01")
        @NotNull(message = "Желаемая дата поставки обязательна")
        LocalDate desiredDeliveryDate,

        @Schema(description = "Комментарий к позиции", example = "Срочная поставка")
        String comment
) {
}
