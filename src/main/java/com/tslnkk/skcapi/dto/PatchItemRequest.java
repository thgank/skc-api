package com.tslnkk.skcapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Запрос на частичное обновление позиции заявки")
public record PatchItemRequest(

        @Schema(description = "Новое количество (>= 1, если указано)", example = "150")
        @DecimalMin(value = "1", message = "Количество не может быть меньше 1")
        BigDecimal quantity,

        @Schema(description = "Новая желаемая дата поставки (не ранее текущей даты + 3 дня)", example = "2025-05-01")
        LocalDate desiredDeliveryDate,

        @Schema(description = "Комментарий к позиции", example = "Обновлённый комментарий")
        String comment,

        @Schema(description = "Версия позиции для оптимистичной блокировки", example = "0")
        @NotNull(message = "Версия обязательна для обновления")
        Long version
) {
}
