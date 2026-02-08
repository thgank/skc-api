package com.tslnkk.skcapi.service;

import com.tslnkk.skcapi.domain.PurchaseRequisition;
import com.tslnkk.skcapi.domain.RequisitionItem;
import com.tslnkk.skcapi.domain.RequisitionStatus;
import com.tslnkk.skcapi.dto.CreateItemRequest;
import com.tslnkk.skcapi.dto.ItemResponse;
import com.tslnkk.skcapi.dto.PatchItemRequest;
import com.tslnkk.skcapi.exception.BusinessException;
import com.tslnkk.skcapi.exception.ErrorCode;
import com.tslnkk.skcapi.reference.ReferenceDataService;
import com.tslnkk.skcapi.reference.ReferenceDataService.NomenclatureRef;
import com.tslnkk.skcapi.repository.PurchaseRequisitionRepository;
import com.tslnkk.skcapi.repository.RequisitionItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequisitionItemServiceTest {

    @Mock
    private PurchaseRequisitionRepository requisitionRepository;

    @Mock
    private RequisitionItemRepository itemRepository;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private RequisitionItemService service;

    private PurchaseRequisition draftRequisition;
    private PurchaseRequisition approvedRequisition;
    private NomenclatureRef nomenclatureRef;

    @BeforeEach
    void setUp() {
        draftRequisition = PurchaseRequisition.builder()
                .id(1L)
                .number("ЗК-2025-00001")
                .status(RequisitionStatus.DRAFT)
                .organizerId("user-123")
                .totalLotSumNoNds(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        approvedRequisition = PurchaseRequisition.builder()
                .id(2L)
                .number("ЗК-2025-00002")
                .status(RequisitionStatus.APPROVED)
                .organizerId("user-123")
                .totalLotSumNoNds(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        nomenclatureRef = new NomenclatureRef(
                "TRU-001", "Бумага офисная A4", Set.of("PIECE", "PACK", "BOX"));
    }

    private CreateItemRequest validCreateRequest() {
        return new CreateItemRequest(
                "TRU-001",
                "Бумага офисная A4",
                BigDecimal.TEN,
                "PACK",
                new BigDecimal("350.00"),
                LocalDate.now().plusDays(10),
                null
        );
    }

    // ─── Test 1: Успешное создание позиции ──────────────────────────

    @Test
    @DisplayName("createItem: успешное создание позиции в DRAFT заявке")
    void createItem_shouldCreateSuccessfully() {
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));
        when(referenceDataService.findNomenclature("TRU-001")).thenReturn(Optional.of(nomenclatureRef));
        when(itemRepository.saveAndFlush(any(RequisitionItem.class)))
                .thenAnswer(invocation -> {
                    RequisitionItem item = invocation.getArgument(0);
                    item.setId(1L);
                    item.setVersion(0L);
                    return item;
                });
        when(requisitionRepository.saveAndFlush(any(PurchaseRequisition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ItemResponse response = service.createItem(1L, validCreateRequest());

        assertNotNull(response);
        assertEquals("TRU-001", response.nomenclatureCode());
        assertEquals("Бумага офисная A4", response.nomenclatureName());
        assertEquals(1, response.rowNumber());
        assertEquals(1L, response.id());
        verify(itemRepository).saveAndFlush(any(RequisitionItem.class));
        verify(requisitionRepository).saveAndFlush(any(PurchaseRequisition.class));
    }

    // ─── Test 2: Заявка не в статусе DRAFT ─────────────────────────

    @Test
    @DisplayName("createItem: ошибка если заявка не в DRAFT")
    void createItem_shouldThrowWhenNotDraft() {
        when(requisitionRepository.findById(2L)).thenReturn(Optional.of(approvedRequisition));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createItem(2L, validCreateRequest()));

        assertEquals(ErrorCode.REQUISITION_NOT_IN_DRAFT, ex.getErrorCode());
        verify(requisitionRepository, never()).saveAndFlush(any());
    }

    // ─── Test 3: Номенклатура не найдена ────────────────────────────

    @Test
    @DisplayName("createItem: ошибка если номенклатура не найдена в справочнике")
    void createItem_shouldThrowWhenNomenclatureNotFound() {
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));
        when(referenceDataService.findNomenclature("TRU-999")).thenReturn(Optional.empty());

        CreateItemRequest request = new CreateItemRequest(
                "TRU-999", "Неизвестный товар", BigDecimal.TEN,
                "PIECE", BigDecimal.valueOf(100), LocalDate.now().plusDays(10), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createItem(1L, request));

        assertEquals(ErrorCode.NOMENCLATURE_NOT_FOUND, ex.getErrorCode());
        assertEquals("nomenclatureCode", ex.getField());
    }

    // ─── Test 4: Наименование номенклатуры не совпадает ─────────────

    @Test
    @DisplayName("createItem: ошибка если наименование номенклатуры не совпадает")
    void createItem_shouldThrowWhenNomenclatureNameMismatch() {
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));
        when(referenceDataService.findNomenclature("TRU-001")).thenReturn(Optional.of(nomenclatureRef));

        CreateItemRequest request = new CreateItemRequest(
                "TRU-001", "Неправильное название", BigDecimal.TEN,
                "PACK", BigDecimal.valueOf(350), LocalDate.now().plusDays(10), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createItem(1L, request));

        assertEquals(ErrorCode.NOMENCLATURE_NAME_MISMATCH, ex.getErrorCode());
        assertEquals("nomenclatureName", ex.getField());
    }

    // ─── Test 5: Единица измерения не разрешена ─────────────────────

    @Test
    @DisplayName("createItem: ошибка если единица измерения не разрешена для номенклатуры")
    void createItem_shouldThrowWhenUnitNotAllowed() {
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));
        when(referenceDataService.findNomenclature("TRU-001")).thenReturn(Optional.of(nomenclatureRef));

        CreateItemRequest request = new CreateItemRequest(
                "TRU-001", "Бумага офисная A4", BigDecimal.TEN,
                "KG", BigDecimal.valueOf(350), LocalDate.now().plusDays(10), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createItem(1L, request));

        assertEquals(ErrorCode.UNIT_NOT_ALLOWED_FOR_NOMENCLATURE, ex.getErrorCode());
        assertEquals("unitCode", ex.getField());
    }

    // ─── Test 6: Дубликат номенклатуры в заявке ─────────────────────

    @Test
    @DisplayName("createItem: ошибка при дубликате номенклатуры в заявке")
    void createItem_shouldThrowWhenDuplicateNomenclature() {
        // Добавим существующую позицию с TRU-001
        RequisitionItem existingItem = RequisitionItem.builder()
                .id(1L).rowNumber(1).truCode("TRU-001").truName("Бумага офисная A4")
                .count(BigDecimal.TEN).mkei("PACK").price(new BigDecimal("350.00"))
                .requisition(draftRequisition).build();
        draftRequisition.getItems().add(existingItem);

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));
        when(referenceDataService.findNomenclature("TRU-001")).thenReturn(Optional.of(nomenclatureRef));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createItem(1L, validCreateRequest()));

        assertEquals(ErrorCode.DUPLICATE_NOMENCLATURE_IN_REQUISITION, ex.getErrorCode());
    }

    // ─── Test 7: Невалидная дата поставки ───────────────────────────

    @Test
    @DisplayName("createItem: ошибка если дата поставки раньше сегодня + 3 дня")
    void createItem_shouldThrowWhenDeliveryDateTooEarly() {
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));
        when(referenceDataService.findNomenclature("TRU-001")).thenReturn(Optional.of(nomenclatureRef));

        CreateItemRequest request = new CreateItemRequest(
                "TRU-001", "Бумага офисная A4", BigDecimal.TEN,
                "PACK", BigDecimal.valueOf(350),
                LocalDate.now().plusDays(1), // слишком рано
                null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createItem(1L, request));

        assertEquals(ErrorCode.INVALID_DELIVERY_DATE, ex.getErrorCode());
        assertEquals("desiredDeliveryDate", ex.getField());
    }

    // ─── Test 8: Запрет удаления последней позиции ──────────────────

    @Test
    @DisplayName("deleteItem: ошибка при удалении последней позиции в заявке")
    void deleteItem_shouldThrowWhenLastItem() {
        RequisitionItem singleItem = RequisitionItem.builder()
                .id(1L).rowNumber(1).truCode("TRU-001").truName("Бумага офисная A4")
                .count(BigDecimal.TEN).mkei("PACK").price(new BigDecimal("350.00"))
                .version(0L).requisition(draftRequisition).build();
        draftRequisition.getItems().add(singleItem);

        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteItem(1L, 1L));

        assertEquals(ErrorCode.LAST_ITEM_DELETE_FORBIDDEN, ex.getErrorCode());
        verify(requisitionRepository, never()).saveAndFlush(any());
    }

    // ─── Test 9: Успешная реактивация CANCELLED → DRAFT ─────────────

    @Test
    @DisplayName("reactivateRequisition: успешная реактивация CANCELLED заявки")
    void reactivateRequisition_shouldSucceed() {
        PurchaseRequisition cancelledRequisition = PurchaseRequisition.builder()
                .id(3L)
                .number("ЗК-2025-00003")
                .status(RequisitionStatus.CANCELLED)
                .organizerId("user-456")
                .totalLotSumNoNds(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        when(requisitionRepository.findById(3L)).thenReturn(Optional.of(cancelledRequisition));
        when(requisitionRepository.saveAndFlush(any(PurchaseRequisition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reactivateRequisition(3L);

        assertEquals(RequisitionStatus.DRAFT, cancelledRequisition.getStatus());
        verify(requisitionRepository).saveAndFlush(cancelledRequisition);
    }

    // ─── Test 10: Реактивация не-CANCELLED заявки → ошибка ──────────

    @Test
    @DisplayName("reactivateRequisition: ошибка при реактивации не-CANCELLED заявки")
    void reactivateRequisition_shouldThrowWhenNotCancelled() {
        when(requisitionRepository.findById(2L)).thenReturn(Optional.of(approvedRequisition));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reactivateRequisition(2L));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        assertEquals("status", ex.getField());
        verify(requisitionRepository, never()).saveAndFlush(any());
    }

    // ─── Test 11: Реактивация DRAFT заявки → ошибка ─────────────────

    @Test
    @DisplayName("reactivateRequisition: ошибка при реактивации DRAFT заявки")
    void reactivateRequisition_shouldThrowWhenAlreadyDraft() {
        when(requisitionRepository.findById(1L)).thenReturn(Optional.of(draftRequisition));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reactivateRequisition(1L));

        assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        verify(requisitionRepository, never()).saveAndFlush(any());
    }
}
