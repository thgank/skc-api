package com.tslnkk.skcapi.controller;

import com.tslnkk.skcapi.reference.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/reference")
@RequiredArgsConstructor
@Tag(name = "Справочники", description = "Справочные данные: номенклатуры и единицы измерения")
public class ReferenceController {

    private final ReferenceDataService referenceDataService;

    @Schema(description = "Номенклатурная позиция из справочника")
    public record NomenclatureDto(
            @Schema(description = "Код номенклатуры", example = "TRU-001")
            String code,
            @Schema(description = "Наименование номенклатуры", example = "Бумага офисная A4")
            String name,
            @Schema(description = "Коды допустимых единиц измерения", example = "[\"PACK\", \"PCS\"]")
            Set<String> allowedUnits
    ) {}

    @Schema(description = "Единица измерения из справочника")
    public record UnitDto(
            @Schema(description = "Код единицы измерения", example = "PACK")
            String code,
            @Schema(description = "Наименование единицы измерения", example = "Упаковка")
            String name
    ) {}

    @Operation(summary = "Список номенклатур",
            description = "Возвращает все доступные номенклатуры с допустимыми единицами измерения.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список номенклатур",
                    content = @Content(schema = @Schema(implementation = NomenclatureDto.class)))
    })
    @GetMapping("/nomenclatures")
    public ResponseEntity<List<NomenclatureDto>> getNomenclatures() {
        List<NomenclatureDto> result = referenceDataService.getAllNomenclatures().stream()
                .map(n -> new NomenclatureDto(n.code(), n.name(), n.allowedUnits()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Список единиц измерения",
            description = "Возвращает все доступные единицы измерения.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список единиц измерения",
                    content = @Content(schema = @Schema(implementation = UnitDto.class)))
    })
    @GetMapping("/units")
    public ResponseEntity<List<UnitDto>> getUnits() {
        List<UnitDto> result = referenceDataService.getAllUnits().stream()
                .map(u -> new UnitDto(u.code(), u.name()))
                .toList();
        return ResponseEntity.ok(result);
    }
}
