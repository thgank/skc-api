package com.tslnkk.skcapi.domain;

/**
 * Статусы заявки на закупку.
 * <p>
 * Основной набор: DRAFT, APPROVED, CANCELLED, CLOSED.
 * Расширенный набор включает промежуточные статусы: SUBMITTED, IN_PROCUREMENT, REJECTED.
 * <p>
 * Бизнес-правила:
 * - DRAFT — разрешено редактирование позиций
 * - Все остальные статусы — read-only (изменение позиций запрещено)
 */
public enum RequisitionStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    IN_PROCUREMENT,
    CLOSED,
    REJECTED,
    CANCELLED
}
