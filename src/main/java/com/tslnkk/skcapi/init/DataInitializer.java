package com.tslnkk.skcapi.init;

import com.tslnkk.skcapi.domain.PurchaseRequisition;
import com.tslnkk.skcapi.domain.RequisitionItem;
import com.tslnkk.skcapi.domain.RequisitionStatus;
import com.tslnkk.skcapi.repository.PurchaseRequisitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Инициализатор тестовых данных.
 * Создаёт 4 заявки в разных статусах с позициями при старте приложения.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PurchaseRequisitionRepository requisitionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (requisitionRepository.count() > 0) {
            log.info("Data already initialized, skipping");
            return;
        }

        createDraftRequisition();
        createApprovedRequisition();
        createClosedRequisition();
        createCancelledRequisition();

        log.info("Test data initialized: {} requisitions", requisitionRepository.count());
    }

    private void createDraftRequisition() {
        PurchaseRequisition req = PurchaseRequisition.builder()
                .number("ЗК-2025-00001")
                .status(RequisitionStatus.DRAFT)
                .organizerId("user-123")
                .build();

        addItem(req, 1, "TRU-001", "Бумага офисная A4",
                new BigDecimal("100"), "PACK", new BigDecimal("350.00"),
                LocalDate.now().plusDays(30));

        addItem(req, 2, "TRU-003", "Ручка шариковая",
                new BigDecimal("50"), "PIECE", new BigDecimal("25.50"),
                LocalDate.now().plusDays(30));

        req.recalculateTotal();
        requisitionRepository.save(req);
        log.info("Created requisition {} (DRAFT) with {} items, total={}",
                req.getNumber(), req.getItems().size(), req.getTotalLotSumNoNds());
    }

    private void createApprovedRequisition() {
        PurchaseRequisition req = PurchaseRequisition.builder()
                .number("ЗК-2025-00002")
                .status(RequisitionStatus.APPROVED)
                .organizerId("user-123")
                .build();

        addItem(req, 1, "TRU-002", "Картридж для принтера",
                new BigDecimal("5"), "PIECE", new BigDecimal("4500.00"),
                LocalDate.now().plusDays(14));

        req.recalculateTotal();
        requisitionRepository.save(req);
        log.info("Created requisition {} (APPROVED) with {} items, total={}",
                req.getNumber(), req.getItems().size(), req.getTotalLotSumNoNds());
    }

    private void createClosedRequisition() {
        PurchaseRequisition req = PurchaseRequisition.builder()
                .number("ЗК-2025-00003")
                .status(RequisitionStatus.CLOSED)
                .organizerId("user-456")
                .build();

        addItem(req, 1, "TRU-006", "Степлер",
                new BigDecimal("10"), "PIECE", new BigDecimal("800.00"),
                LocalDate.now().plusDays(7));

        addItem(req, 2, "TRU-008", "Маркер текстовый",
                new BigDecimal("30"), "PACK", new BigDecimal("120.00"),
                LocalDate.now().plusDays(7));

        req.recalculateTotal();
        requisitionRepository.save(req);
        log.info("Created requisition {} (CLOSED) with {} items, total={}",
                req.getNumber(), req.getItems().size(), req.getTotalLotSumNoNds());
    }

    private void createCancelledRequisition() {
        PurchaseRequisition req = PurchaseRequisition.builder()
                .number("ЗК-2025-00004")
                .status(RequisitionStatus.CANCELLED)
                .organizerId("user-456")
                .build();

        addItem(req, 1, "TRU-004", "Папка-регистратор",
                new BigDecimal("20"), "PIECE", new BigDecimal("250.00"),
                LocalDate.now().plusDays(14));

        req.recalculateTotal();
        requisitionRepository.save(req);
        log.info("Created requisition {} (CANCELLED) with {} items, total={}",
                req.getNumber(), req.getItems().size(), req.getTotalLotSumNoNds());
    }

    private void addItem(PurchaseRequisition req, int rowNumber,
                          String truCode, String truName,
                          BigDecimal count, String mkei, BigDecimal price,
                          LocalDate deliveryDate) {
        RequisitionItem item = RequisitionItem.builder()
                .rowNumber(rowNumber)
                .truCode(truCode)
                .truName(truName)
                .count(count)
                .mkei(mkei)
                .price(price)
                .durationMonth(deliveryDate)
                .requisition(req)
                .build();
        req.getItems().add(item);
    }
}
