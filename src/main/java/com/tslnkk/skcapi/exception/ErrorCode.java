package com.tslnkk.skcapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    REQUISITION_NOT_FOUND("Заявка не найдена", 404),
    ITEM_NOT_FOUND("Позиция не найдена", 404),
    REQUISITION_NOT_IN_DRAFT("Операция запрещена: заявка не в статусе DRAFT", 400),
    NOMENCLATURE_NOT_FOUND("Номенклатура не найдена в справочнике", 400),
    NOMENCLATURE_NAME_MISMATCH("Наименование номенклатуры не совпадает со справочником", 400),
    UNIT_NOT_ALLOWED_FOR_NOMENCLATURE("Единица измерения не разрешена для данной номенклатуры", 400),
    DUPLICATE_NOMENCLATURE_IN_REQUISITION("Дубликат номенклатуры в заявке", 400),
    INVALID_DELIVERY_DATE("Дата поставки должна быть не ранее текущей даты + 3 дня", 400),
    INVALID_QUANTITY("Недопустимое значение количества", 400),
    LAST_ITEM_DELETE_FORBIDDEN("Нельзя удалить последнюю позицию в заявке", 400),
    OPTIMISTIC_LOCK_CONFLICT("Конфликт версий: позиция была изменена другим пользователем", 409),
    INVALID_STATUS_TRANSITION("Недопустимый переход статуса заявки", 400),
    REQUISITION_DELETE_FORBIDDEN("Удаление заявки запрещено", 400),
    REQUISITION_EMPTY("Заявка не содержит позиций", 400);

    private final String defaultMessage;
    private final int httpStatus;
}
