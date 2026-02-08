package com.tslnkk.skcapi.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.LocalDate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RequisitionItemControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTestClient client;

    private static final String BASE_URL = "/api/v1/requisitions";

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeaders(headers -> headers.setBasicAuth("admin", "admin"))
                .build();
    }

    @Test
    @DisplayName("POST /items: создание позиции в DRAFT заявке возвращает 201")
    void createItem_shouldReturnCreated() {
        String body = """
                {
                    "nomenclatureCode": "TRU-005",
                    "nomenclatureName": "Скрепки канцелярские",
                    "quantity": 200,
                    "unitCode": "BOX",
                    "priceWithoutVat": 150.00,
                    "desiredDeliveryDate": "%s",
                    "comment": "Тестовый комментарий"
                }
                """.formatted(LocalDate.now().plusDays(10));

        client.post().uri(BASE_URL + "/1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.nomenclatureCode").isEqualTo("TRU-005")
                .jsonPath("$.nomenclatureName").isEqualTo("Скрепки канцелярские")
                .jsonPath("$.rowNumber").isEqualTo(3)
                .jsonPath("$.quantity").isEqualTo(200);
    }

    @Test
    @DisplayName("POST /items: создание позиции в APPROVED заявке возвращает 400")
    void createItem_onApprovedRequisition_shouldReturn400() {
        String body = """
                {
                    "nomenclatureCode": "TRU-005",
                    "nomenclatureName": "Скрепки канцелярские",
                    "quantity": 200,
                    "unitCode": "BOX",
                    "priceWithoutVat": 150.00,
                    "desiredDeliveryDate": "%s"
                }
                """.formatted(LocalDate.now().plusDays(10));

        client.post().uri(BASE_URL + "/2/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("REQUISITION_NOT_IN_DRAFT");
    }

    @Test
    @DisplayName("PATCH /items/{id}: обновление позиции возвращает 200")
    void patchItem_shouldReturnOk() {
        String patchBody = """
                {
                    "quantity": 200,
                    "comment": "Увеличили количество",
                    "version": 0
                }
                """;

        client.patch().uri(BASE_URL + "/1/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(patchBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.quantity").isEqualTo(200)
                .jsonPath("$.comment").isEqualTo("Увеличили количество");
    }

    @Test
    @DisplayName("DELETE /items/{id}: удаление позиции возвращает 204")
    void deleteItem_shouldReturnNoContent() {
        client.delete().uri(BASE_URL + "/1/items/2")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("GET /summary: сводка по заявке возвращает корректные данные")
    void getSummary_shouldReturnCorrectData() {
        client.get().uri(BASE_URL + "/1/summary")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalAmountWithoutVat").isEqualTo(36275.00)
                .jsonPath("$.itemCount").isEqualTo(2)
                .jsonPath("$.currency").isEqualTo("KZT");
    }

    @Test
    @DisplayName("POST /reactivate: реактивация CANCELLED заявки возвращает 204")
    void reactivateRequisition_shouldReturnNoContent() {
        client.post().uri(BASE_URL + "/4/reactivate")
                .exchange()
                .expectStatus().isNoContent();

        String body = """
                {
                    "nomenclatureCode": "TRU-005",
                    "nomenclatureName": "Скрепки канцелярские",
                    "quantity": 50,
                    "unitCode": "BOX",
                    "priceWithoutVat": 150.00,
                    "desiredDeliveryDate": "%s"
                }
                """.formatted(LocalDate.now().plusDays(10));

        client.post().uri(BASE_URL + "/4/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("POST /reactivate: реактивация APPROVED заявки возвращает 400")
    void reactivateRequisition_onApproved_shouldReturn400() {
        client.post().uri(BASE_URL + "/2/reactivate")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("POST /reactivate: реактивация DRAFT заявки возвращает 400")
    void reactivateRequisition_onDraft_shouldReturn400() {
        client.post().uri(BASE_URL + "/1/reactivate")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("INVALID_STATUS_TRANSITION");
    }
}
