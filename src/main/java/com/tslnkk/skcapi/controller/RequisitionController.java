package com.tslnkk.skcapi.controller;

import com.tslnkk.skcapi.dto.*;
import com.tslnkk.skcapi.service.RequisitionService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/requisitions")
@RequiredArgsConstructor
@Tag(name = "Заявки", description = "Управление заявками на закупку")
public class RequisitionController {

    private final RequisitionService requisitionService;

    @Operation(summary = "Список всех заявок")
    @ApiResponse(responseCode = "200", description = "Список заявок")
    @GetMapping
    public ResponseEntity<List<RequisitionResponse>> listAll() {
        return ResponseEntity.ok(requisitionService.listAll());
    }

    @Operation(summary = "Получить заявку с позициями")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заявка найдена"),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RequisitionDetailResponse> getById(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.getById(id));
    }

    @Operation(summary = "Создать новую заявку",
            description = "Создаёт заявку в статусе DRAFT. Номер генерируется автоматически.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заявка создана",
                    content = @Content(schema = @Schema(implementation = RequisitionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RequisitionResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для создания заявки",
                    required = true)
            @Valid @RequestBody CreateRequisitionRequest request) {
        RequisitionResponse response = requisitionService.create(request);
        URI location = URI.create("/api/v1/requisitions/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Обновить заявку",
            description = "Частичное обновление. Доступно только для заявок в статусе DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заявка обновлена",
                    content = @Content(schema = @Schema(implementation = RequisitionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или заявка не в DRAFT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<RequisitionResponse> update(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Поля для частичного обновления заявки",
                    required = true)
            @Valid @RequestBody PatchRequisitionRequest request) {
        return ResponseEntity.ok(requisitionService.update(id, request));
    }

    @Operation(summary = "Удалить заявку",
            description = "Удаление заявки со всеми позициями. Доступно только для DRAFT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Заявка удалена"),
            @ApiResponse(responseCode = "400", description = "Заявка не в статусе DRAFT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long id) {
        requisitionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Сменить статус заявки",
            description = """
                    Переводит заявку в указанный целевой статус.
                    Допустимые переходы:
                    DRAFT → SUBMITTED, CANCELLED;
                    SUBMITTED → APPROVED, REJECTED, CANCELLED;
                    APPROVED → IN_PROCUREMENT, CANCELLED;
                    IN_PROCUREMENT → CLOSED, CANCELLED;
                    REJECTED → DRAFT;
                    CANCELLED → DRAFT;
                    CLOSED — терминальный статус.
                    Для перехода DRAFT → SUBMITTED заявка должна содержать хотя бы одну позицию.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус изменён",
                    content = @Content(schema = @Schema(implementation = RequisitionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Недопустимый переход или заявка без позиций",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/transition")
    public ResponseEntity<RequisitionResponse> transition(
            @Parameter(description = "ID заявки", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Целевой статус для перехода заявки",
                    required = true)
            @Valid @RequestBody TransitionRequest request) {
        return ResponseEntity.ok(requisitionService.transition(id, request.targetStatus()));
    }
}
