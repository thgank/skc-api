package com.tslnkk.skcapi.service;

import com.tslnkk.skcapi.domain.PurchaseRequisition;
import com.tslnkk.skcapi.domain.RequisitionItem;
import com.tslnkk.skcapi.domain.RequisitionStatus;
import com.tslnkk.skcapi.dto.*;
import com.tslnkk.skcapi.exception.BusinessException;
import com.tslnkk.skcapi.exception.ErrorCode;
import com.tslnkk.skcapi.reference.ReferenceDataService;
import com.tslnkk.skcapi.repository.PurchaseRequisitionRepository;
import com.tslnkk.skcapi.repository.RequisitionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

/**
 * Основная бизнес-логика управления позициями заявки.
 * Все мутации допустимы только для заявок в статусе DRAFT.
 *
 * <p>Логирование обеспечивается через AOP — {@link com.tslnkk.skcapi.aspect.LoggingAspect}.</p>
 */
@Service
@RequiredArgsConstructor
public class RequisitionItemService {

    private static final String CURRENCY = "KZT";

    private final PurchaseRequisitionRepository requisitionRepository;
    private final RequisitionItemRepository itemRepository;
    private final ReferenceDataService referenceDataService;

    /**
     * Создаёт новую позицию в заявке со статусом DRAFT.
     * Валидирует номенклатуру, единицу измерения, уникальность и дату поставки.
     * Автоматически назначает rowNumber и пересчитывает итог заявки.
     *
     * @param requisitionId ID заявки
     * @param request       данные для создания позиции
     * @return ответ с созданной позицией, включая ID и version
     */
    @Transactional
    public ItemResponse createItem(Long requisitionId, CreateItemRequest request) {
        PurchaseRequisition requisition = findRequisitionOrThrow(requisitionId);
        ensureDraft(requisition);

        var nomenclature = referenceDataService.findNomenclature(request.nomenclatureCode())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOMENCLATURE_NOT_FOUND,
                        "nomenclatureCode",
                        request.nomenclatureCode()));

        if (!nomenclature.name().equals(request.nomenclatureName())) {
            throw new BusinessException(
                    ErrorCode.NOMENCLATURE_NAME_MISMATCH,
                    "nomenclatureName",
                    request.nomenclatureName());
        }

        if (!nomenclature.allowedUnits().contains(request.unitCode())) {
            throw new BusinessException(
                    ErrorCode.UNIT_NOT_ALLOWED_FOR_NOMENCLATURE,
                    "unitCode",
                    request.unitCode());
        }

        boolean duplicate = requisition.getItems().stream()
                .anyMatch(item -> item.getTruCode().equals(request.nomenclatureCode()));
        if (duplicate) {
            throw new BusinessException(
                    ErrorCode.DUPLICATE_NOMENCLATURE_IN_REQUISITION,
                    "nomenclatureCode",
                    request.nomenclatureCode());
        }

        validateDeliveryDate(request.desiredDeliveryDate());

        int maxRowNumber = requisition.getItems().stream()
                .mapToInt(RequisitionItem::getRowNumber)
                .max()
                .orElse(0);

        RequisitionItem item = RequisitionItem.builder()
                .rowNumber(maxRowNumber + 1)
                .truCode(request.nomenclatureCode())
                .truName(request.nomenclatureName())
                .count(request.quantity())
                .mkei(request.unitCode())
                .price(request.priceWithoutVat())
                .durationMonth(request.desiredDeliveryDate())
                .comment(request.comment())
                .requisition(requisition)
                .build();

        item = itemRepository.saveAndFlush(item);
        requisition.getItems().add(item);

        requisition.recalculateTotal();
        requisitionRepository.saveAndFlush(requisition);

        return toResponse(item);
    }

    /**
     * Частичное обновление позиции (PATCH-семантика).
     * Допустимые поля: quantity, desiredDeliveryDate, comment.
     * Обязателен version для оптимистичной блокировки.
     *
     * @param requisitionId ID заявки
     * @param itemId        ID позиции внутри заявки
     * @param request       данные патча с version
     * @return обновлённая позиция
     */
    @Transactional
    public ItemResponse patchItem(Long requisitionId, Long itemId, PatchItemRequest request) {
        PurchaseRequisition requisition = findRequisitionOrThrow(requisitionId);
        ensureDraft(requisition);

        RequisitionItem item = requisition.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.getVersion().equals(request.version())) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }

        if (request.quantity() != null) {
            if (request.quantity().compareTo(BigDecimal.ONE) < 0) {
                throw new BusinessException(
                        ErrorCode.INVALID_QUANTITY,
                        "Quantity must be >= 1",
                        "quantity",
                        request.quantity());
            }
            item.setCount(request.quantity());
        }

        if (request.desiredDeliveryDate() != null) {
            validateDeliveryDate(request.desiredDeliveryDate());
            item.setDurationMonth(request.desiredDeliveryDate());
        }

        if (request.comment() != null) {
            item.setComment(request.comment());
        }

        requisition.recalculateTotal();
        requisitionRepository.saveAndFlush(requisition);

        return toResponse(item);
    }

    /**
     * Удаляет позицию из заявки в статусе DRAFT.
     * Нельзя удалить последнюю оставшуюся позицию.
     * После удаления пересчитывает итог заявки.
     *
     * @param requisitionId ID заявки
     * @param itemId        ID удаляемой позиции
     */
    @Transactional
    public void deleteItem(Long requisitionId, Long itemId) {
        PurchaseRequisition requisition = findRequisitionOrThrow(requisitionId);
        ensureDraft(requisition);

        if (requisition.getItems().size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_ITEM_DELETE_FORBIDDEN);
        }

        boolean removed = requisition.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }

        requisition.recalculateTotal();
        requisitionRepository.saveAndFlush(requisition);
    }

    /**
     * Возвращает агрегированную сводку по заявке:
     * общая сумма, общее количество, мин/макс даты поставки, число позиций, валюта.
     *
     * @param requisitionId ID заявки
     * @return сводка по заявке
     */
    @Transactional(readOnly = true)
    public RequisitionSummaryResponse getSummary(Long requisitionId) {
        PurchaseRequisition requisition = findRequisitionOrThrow(requisitionId);
        var items = requisition.getItems();

        BigDecimal totalAmount = items.stream()
                .map(i -> i.getPrice().multiply(i.getCount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalQuantity = items.stream()
                .map(RequisitionItem::getCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate minDate = items.stream()
                .map(RequisitionItem::getDurationMonth)
                .filter(d -> d != null)
                .min(Comparator.naturalOrder())
                .orElse(null);

        LocalDate maxDate = items.stream()
                .map(RequisitionItem::getDurationMonth)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new RequisitionSummaryResponse(
                totalAmount, totalQuantity, minDate, maxDate, items.size(), CURRENCY);
    }

    /**
     * Реактивирует отменённую заявку — переводит из CANCELLED в DRAFT.
     * Допустим только переход CANCELLED → DRAFT.
     *
     * @param requisitionId ID заявки
     */
    @Transactional
    public void reactivateRequisition(Long requisitionId) {
        PurchaseRequisition requisition = findRequisitionOrThrow(requisitionId);

        if (requisition.getStatus() != RequisitionStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "Reactivation is only allowed for CANCELLED requisitions. Current status: " + requisition.getStatus(),
                    "status",
                    requisition.getStatus().name());
        }

        requisition.setStatus(RequisitionStatus.DRAFT);
        requisitionRepository.saveAndFlush(requisition);
    }

    private PurchaseRequisition findRequisitionOrThrow(Long requisitionId) {
        return requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUISITION_NOT_FOUND));
    }

    private void ensureDraft(PurchaseRequisition requisition) {
        if (!requisition.isDraft()) {
            throw new BusinessException(
                    ErrorCode.REQUISITION_NOT_IN_DRAFT,
                    "Operation forbidden: requisition is in " + requisition.getStatus() + " status");
        }
    }

    private void validateDeliveryDate(LocalDate date) {
        LocalDate minDate = LocalDate.now().plusDays(3);
        if (date.isBefore(minDate)) {
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_DATE,
                    "Delivery date must be no earlier than " + minDate,
                    "desiredDeliveryDate",
                    date);
        }
    }

    private ItemResponse toResponse(RequisitionItem item) {
        return new ItemResponse(
                item.getId(),
                item.getRowNumber(),
                item.getTruCode(),
                item.getTruName(),
                item.getCount(),
                item.getMkei(),
                item.getPrice(),
                item.getDurationMonth(),
                item.getComment(),
                item.getVersion()
        );
    }
}
