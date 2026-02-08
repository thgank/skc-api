package com.tslnkk.skcapi.controller;

import com.tslnkk.skcapi.dto.*;
import com.tslnkk.skcapi.service.RequisitionItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/requisitions/{requisitionId}")
@RequiredArgsConstructor
@Tag(name = "Позиции заявки", description = "Управление позициями в заявке на закупку")
public class RequisitionItemController {

    private final RequisitionItemService itemService;

    @Operation(summary = "Создать позицию в заявке",
            description = "Доступно только для заявок в статусе DRAFT")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Позиция создана",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации/бизнес-правил",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/items")
    public ResponseEntity<ItemResponse> createItem(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long requisitionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные новой позиции",
                    required = true)
            @Valid @RequestBody CreateItemRequest request) {

        ItemResponse response = itemService.createItem(requisitionId, request);
        URI location = URI.create("/api/v1/requisitions/" + requisitionId + "/items/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Частичное обновление позиции",
            description = "Разрешено менять: quantity, desiredDeliveryDate, comment. "
                    + "Обязательно передать version для оптимистичной блокировки.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Позиция обновлена",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации/бизнес-правил",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка или позиция не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Конфликт оптимистичной блокировки",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ItemResponse> patchItem(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long requisitionId,
            @Parameter(description = "ID позиции", example = "10") @PathVariable Long itemId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Частичное обновление позиции (с обязательной версией)",
                    required = true)
            @Valid @RequestBody PatchItemRequest request) {

        ItemResponse response = itemService.patchItem(requisitionId, itemId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Удалить позицию из заявки",
            description = "Доступно только для DRAFT. Нельзя удалить последнюю позицию.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Позиция удалена"),
            @ApiResponse(responseCode = "400", description = "Ошибка бизнес-правил",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка или позиция не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long requisitionId,
            @Parameter(description = "ID позиции", example = "10") @PathVariable Long itemId) {

        itemService.deleteItem(requisitionId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Реактивировать отменённую заявку",
            description = "Переводит заявку из статуса CANCELLED обратно в DRAFT. "
                    + "Доступно только для заявок в статусе CANCELLED.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Заявка успешно реактивирована"),
            @ApiResponse(responseCode = "400", description = "Заявка не в статусе CANCELLED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/reactivate")
    public ResponseEntity<Void> reactivateRequisition(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long requisitionId) {

        itemService.reactivateRequisition(requisitionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить сводку по заявке",
            description = "Возвращает агрегированные данные: сумму, количество, мин/макс даты")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сводка",
                    content = @Content(schema = @Schema(implementation = RequisitionSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/summary")
    public ResponseEntity<RequisitionSummaryResponse> getSummary(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long requisitionId) {

        RequisitionSummaryResponse response = itemService.getSummary(requisitionId);
        return ResponseEntity.ok(response);
    }
}
