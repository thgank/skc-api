package com.tslnkk.skcapi.service;

import com.tslnkk.skcapi.domain.PurchaseRequisition;
import com.tslnkk.skcapi.domain.RequisitionItem;
import com.tslnkk.skcapi.domain.RequisitionStatus;
import com.tslnkk.skcapi.dto.*;
import com.tslnkk.skcapi.exception.BusinessException;
import com.tslnkk.skcapi.exception.ErrorCode;
import com.tslnkk.skcapi.repository.PurchaseRequisitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequisitionServiceTest {

    @Mock
    private PurchaseRequisitionRepository repository;

    @InjectMocks
    private RequisitionService service;

    private PurchaseRequisition draftRequisition;
    private PurchaseRequisition approvedRequisition;
    private PurchaseRequisition cancelledRequisition;
    private PurchaseRequisition submittedRequisition;
    private PurchaseRequisition closedRequisition;
    private PurchaseRequisition rejectedRequisition;
    private PurchaseRequisition inProcurementRequisition;

    @BeforeEach
    void setUp() {
        draftRequisition = buildRequisition(1L, "ЗК-2025-00001", RequisitionStatus.DRAFT, "user-123");
        approvedRequisition = buildRequisition(2L, "ЗК-2025-00002", RequisitionStatus.APPROVED, "user-123");
        cancelledRequisition = buildRequisition(3L, "ЗК-2025-00003", RequisitionStatus.CANCELLED, "user-456");
        submittedRequisition = buildRequisition(4L, "ЗК-2025-00004", RequisitionStatus.SUBMITTED, "user-123");
        closedRequisition = buildRequisition(5L, "ЗК-2025-00005", RequisitionStatus.CLOSED, "user-456");
        rejectedRequisition = buildRequisition(6L, "ЗК-2025-00006", RequisitionStatus.REJECTED, "user-456");
        inProcurementRequisition = buildRequisition(7L, "ЗК-2025-00007", RequisitionStatus.IN_PROCUREMENT, "user-456");
    }

    private PurchaseRequisition buildRequisition(Long id, String number, RequisitionStatus status, String organizerId) {
        return PurchaseRequisition.builder()
                .id(id)
                .number(number)
                .status(status)
                .organizerId(organizerId)
                .totalLotSumNoNds(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
    }

    private void addItemToRequisition(PurchaseRequisition req) {
        RequisitionItem item = RequisitionItem.builder()
                .id(100L).rowNumber(1).truCode("TRU-001").truName("Бумага офисная A4")
                .count(BigDecimal.TEN).mkei("PACK").price(new BigDecimal("350.00"))
                .version(0L).requisition(req).build();
        req.getItems().add(item);
    }

    // ═══════════════════════════════════════════════════════════════
    // listAll
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listAll")
    class ListAll {
        @Test
        @DisplayName("возвращает пустой список если заявок нет")
        void shouldReturnEmptyListWhenNoRequisitions() {
            when(repository.findAll()).thenReturn(List.of());

            List<RequisitionResponse> result = service.listAll();

            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // getById
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById")
    class GetById {
        @Test
        @DisplayName("бросает исключение если заявка не найдена")
        void shouldThrowWhenNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.getById(999L));

            assertEquals(ErrorCode.REQUISITION_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // create
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        @DisplayName("создаёт заявку в статусе DRAFT с генерированным номером")
        void shouldCreateDraftRequisition() {
            when(repository.count()).thenReturn(4L);
            when(repository.saveAndFlush(any(PurchaseRequisition.class)))
                    .thenAnswer(invocation -> {
                        PurchaseRequisition req = invocation.getArgument(0);
                        req.setId(5L);
                        return req;
                    });

            RequisitionResponse result = service.create(new CreateRequisitionRequest("user-new"));

            assertEquals("DRAFT", result.status());
            assertEquals("user-new", result.organizerId());
            assertTrue(result.number().startsWith("ЗК-"));
            verify(repository).saveAndFlush(any(PurchaseRequisition.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // update
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update")
    class Update {
        @Test
        @DisplayName("ошибка если заявка не в DRAFT")
        void shouldThrowWhenNotDraft() {
            when(repository.findById(2L)).thenReturn(Optional.of(approvedRequisition));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.update(2L, new PatchRequisitionRequest("org")));

            assertEquals(ErrorCode.REQUISITION_NOT_IN_DRAFT, ex.getErrorCode());
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("не обновляет организатора если значение пустое")
        void shouldNotUpdateIfOrganizerBlank() {
            when(repository.findById(1L)).thenReturn(Optional.of(draftRequisition));
            when(repository.saveAndFlush(any(PurchaseRequisition.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RequisitionResponse result = service.update(1L, new PatchRequisitionRequest(""));

            assertEquals("user-123", result.organizerId());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // delete
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete")
    class Delete {
        @Test
        @DisplayName("ошибка при удалении APPROVED заявки")
        void shouldThrowWhenDeletingApproved() {
            when(repository.findById(2L)).thenReturn(Optional.of(approvedRequisition));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.delete(2L));

            assertEquals(ErrorCode.REQUISITION_DELETE_FORBIDDEN, ex.getErrorCode());
            verify(repository, never()).delete(any());
        }

        @ParameterizedTest(name = "ошибка при удалении заявки в статусе {0}")
        @EnumSource(value = RequisitionStatus.class, names = {"SUBMITTED", "APPROVED", "IN_PROCUREMENT", "CLOSED", "REJECTED", "CANCELLED"})
        @DisplayName("ошибка при удалении не-DRAFT заявок")
        void shouldThrowWhenDeletingNonDraft(RequisitionStatus status) {
            PurchaseRequisition req = buildRequisition(10L, "ЗК-TEST", status, "user");
            when(repository.findById(10L)).thenReturn(Optional.of(req));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.delete(10L));

            assertEquals(ErrorCode.REQUISITION_DELETE_FORBIDDEN, ex.getErrorCode());
        }
    }
}
