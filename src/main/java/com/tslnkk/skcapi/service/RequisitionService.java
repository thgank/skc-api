package com.tslnkk.skcapi.service;

import com.tslnkk.skcapi.domain.PurchaseRequisition;
import com.tslnkk.skcapi.domain.RequisitionItem;
import com.tslnkk.skcapi.domain.RequisitionStatus;
import com.tslnkk.skcapi.dto.*;
import com.tslnkk.skcapi.exception.BusinessException;
import com.tslnkk.skcapi.exception.ErrorCode;
import com.tslnkk.skcapi.repository.PurchaseRequisitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import static com.tslnkk.skcapi.domain.RequisitionStatus.*;

@Service
@RequiredArgsConstructor
public class RequisitionService {

    private static final Map<RequisitionStatus, Set<RequisitionStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<RequisitionStatus, Set<RequisitionStatus>> m = new EnumMap<>(RequisitionStatus.class);
        m.put(DRAFT,          EnumSet.of(SUBMITTED, CANCELLED));
        m.put(SUBMITTED,      EnumSet.of(APPROVED, REJECTED, CANCELLED));
        m.put(APPROVED,       EnumSet.of(IN_PROCUREMENT, CANCELLED));
        m.put(IN_PROCUREMENT, EnumSet.of(CLOSED, CANCELLED));
        m.put(REJECTED,       EnumSet.of(DRAFT));
        m.put(CANCELLED,      EnumSet.of(DRAFT));
        m.put(CLOSED,         EnumSet.noneOf(RequisitionStatus.class));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(m);
    }

    private final PurchaseRequisitionRepository repository;

    @Transactional(readOnly = true)
    public List<RequisitionResponse> listAll() {
        return repository.findAll().stream()
                .map(RequisitionService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequisitionDetailResponse getById(Long id) {
        PurchaseRequisition r = findOrThrow(id);
        return toDetailResponse(r);
    }

    @Transactional
    public RequisitionResponse create(CreateRequisitionRequest request) {
        String number = generateNumber();

        PurchaseRequisition req = PurchaseRequisition.builder()
                .number(number)
                .status(DRAFT)
                .organizerId(request.organizerId())
                .build();

        req = repository.saveAndFlush(req);
        return toResponse(req);
    }

    @Transactional
    public RequisitionResponse update(Long id, PatchRequisitionRequest request) {
        PurchaseRequisition req = findOrThrow(id);
        ensureDraft(req);

        if (request.organizerId() != null && !request.organizerId().isBlank()) {
            req.setOrganizerId(request.organizerId());
        }

        req = repository.saveAndFlush(req);
        return toResponse(req);
    }

    @Transactional
    public void delete(Long id) {
        PurchaseRequisition req = findOrThrow(id);

        if (req.getStatus() != DRAFT) {
            throw new BusinessException(
                    ErrorCode.REQUISITION_DELETE_FORBIDDEN,
                    "Удаление возможно только для заявок в статусе DRAFT. Текущий статус: " + req.getStatus(),
                    "status",
                    req.getStatus().name());
        }

        repository.delete(req);
    }

    @Transactional
    public RequisitionResponse transition(Long id, RequisitionStatus targetStatus) {
        PurchaseRequisition req = findOrThrow(id);
        RequisitionStatus current = req.getStatus();

        Set<RequisitionStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(RequisitionStatus.class));
        if (!allowed.contains(targetStatus)) {
            String allowedStr = allowed.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new BusinessException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    String.format("Переход %s → %s запрещён. Допустимые переходы: %s",
                            current, targetStatus, allowedStr.isEmpty() ? "нет (терминальный статус)" : allowedStr),
                    "status",
                    targetStatus.name());
        }

        if (targetStatus == SUBMITTED && req.getItems().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.REQUISITION_EMPTY,
                    "Невозможно подать заявку без позиций. Добавьте хотя бы одну позицию.",
                    "items",
                    0);
        }

        req.setStatus(targetStatus);
        req = repository.saveAndFlush(req);
        return toResponse(req);
    }

    public static Set<RequisitionStatus> getAllowedTransitions(RequisitionStatus status) {
        return ALLOWED_TRANSITIONS.getOrDefault(status, EnumSet.noneOf(RequisitionStatus.class));
    }

    private PurchaseRequisition findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUISITION_NOT_FOUND));
    }

    private void ensureDraft(PurchaseRequisition req) {
        if (!req.isDraft()) {
            throw new BusinessException(
                    ErrorCode.REQUISITION_NOT_IN_DRAFT,
                    "Операция запрещена: заявка в статусе " + req.getStatus());
        }
    }

    private String generateNumber() {
        int year = Year.now().getValue();
        long count = repository.count();
        return String.format("ЗК-%d-%05d", year, count + 1);
    }

    static RequisitionResponse toResponse(PurchaseRequisition r) {
        return new RequisitionResponse(
                r.getId(),
                r.getNumber(),
                r.getStatus().name(),
                r.getOrganizerId(),
                r.getTotalLotSumNoNds(),
                r.getCreatedFrom(),
                r.getUpdatedFrom());
    }

    static RequisitionDetailResponse toDetailResponse(PurchaseRequisition r) {
        List<ItemResponse> items = r.getItems().stream()
                .sorted(Comparator.comparing(RequisitionItem::getRowNumber))
                .map(RequisitionService::toItemResponse)
                .toList();

        return new RequisitionDetailResponse(
                r.getId(),
                r.getNumber(),
                r.getStatus().name(),
                r.getOrganizerId(),
                r.getTotalLotSumNoNds(),
                r.getCreatedFrom(),
                r.getUpdatedFrom(),
                items);
    }

    static ItemResponse toItemResponse(RequisitionItem item) {
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
                item.getVersion());
    }
}
