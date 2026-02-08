package com.tslnkk.skcapi.reference;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Захардкоженный справочник номенклатур и единиц измерения.
 * Данные загружаются при старте приложения и хранятся в памяти.
 */
@Service
@Slf4j
public class ReferenceDataService {

    public record NomenclatureRef(String code, String name, Set<String> allowedUnits) {
    }

    public record UnitOfMeasure(String code, String name) {
    }

    private final Map<String, NomenclatureRef> nomenclatures = new LinkedHashMap<>();
    private final Map<String, UnitOfMeasure> units = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        // Единицы измерения (7 шт.)
        addUnit("PIECE", "Штука");
        addUnit("PACK", "Упаковка");
        addUnit("BOX", "Коробка");
        addUnit("KG", "Килограмм");
        addUnit("LITER", "Литр");
        addUnit("METER", "Метр");
        addUnit("SET", "Комплект");

        // Номенклатуры (12 шт.)
        addNomenclature("TRU-001", "Бумага офисная A4", Set.of("PIECE", "PACK", "BOX"));
        addNomenclature("TRU-002", "Картридж для принтера", Set.of("PIECE"));
        addNomenclature("TRU-003", "Ручка шариковая", Set.of("PIECE", "PACK", "BOX"));
        addNomenclature("TRU-004", "Папка-регистратор", Set.of("PIECE"));
        addNomenclature("TRU-005", "Скрепки канцелярские", Set.of("PACK", "BOX"));
        addNomenclature("TRU-006", "Степлер", Set.of("PIECE"));
        addNomenclature("TRU-007", "Клей-карандаш", Set.of("PIECE", "PACK"));
        addNomenclature("TRU-008", "Маркер текстовый", Set.of("PIECE", "PACK", "SET"));
        addNomenclature("TRU-009", "Ножницы офисные", Set.of("PIECE"));
        addNomenclature("TRU-010", "Калькулятор", Set.of("PIECE"));
        addNomenclature("TRU-011", "Блокнот А5", Set.of("PIECE", "PACK"));
        addNomenclature("TRU-012", "Файл-вкладыш", Set.of("PACK", "BOX"));

        log.info("Reference data initialized: {} nomenclatures, {} units of measure",
                nomenclatures.size(), units.size());
    }

    private void addUnit(String code, String name) {
        units.put(code, new UnitOfMeasure(code, name));
    }

    private void addNomenclature(String code, String name, Set<String> allowedUnits) {
        nomenclatures.put(code, new NomenclatureRef(code, name, allowedUnits));
    }

    public Optional<NomenclatureRef> findNomenclature(String code) {
        return Optional.ofNullable(nomenclatures.get(code));
    }

    public Optional<UnitOfMeasure> findUnit(String code) {
        return Optional.ofNullable(units.get(code));
    }

    public boolean isUnitAllowedForNomenclature(String nomenclatureCode, String unitCode) {
        NomenclatureRef ref = nomenclatures.get(nomenclatureCode);
        return ref != null && ref.allowedUnits().contains(unitCode);
    }

    public Collection<NomenclatureRef> getAllNomenclatures() {
        return Collections.unmodifiableCollection(nomenclatures.values());
    }

    public Collection<UnitOfMeasure> getAllUnits() {
        return Collections.unmodifiableCollection(units.values());
    }
}
