package com.tslnkk.skcapi.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Интеграционные тесты для {@link com.tslnkk.skcapi.controller.RequisitionController}.
 *
 * <p>Seed data (DataInitializer):
 * <ul>
 *   <li>id=1 — DRAFT, 2 items, organizerId=user-123</li>
 *   <li>id=2 — APPROVED, 1 item, organizerId=user-123</li>
 *   <li>id=3 — CLOSED, 2 items, organizerId=user-456</li>
 *   <li>id=4 — CANCELLED, 1 item, organizerId=user-456</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RequisitionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTestClient client;
    private RestTestClient unauthenticatedClient;

    private static final String BASE_URL = "/api/v1/requisitions";

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        client = RestTestClient.bindToServer()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth("admin", "admin"))
                .build();
        unauthenticatedClient = RestTestClient.bindToServer()
                .baseUrl(baseUrl)
                .build();
    }

    @Test
    @DisplayName("GET /requisitions: без авторизации — 401")
    void listAll_noAuth_shouldReturn401() {
        unauthenticatedClient.get().uri(BASE_URL)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /requisitions/{id}: несуществующая заявка — 404")
    void getById_notFound_shouldReturn404() {
        client.get().uri(BASE_URL + "/999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("REQUISITION_NOT_FOUND");
    }

    @Test
    @DisplayName("POST /requisitions: без organizerId — 400")
    void create_withoutOrganizer_shouldReturn400() {
        String body = """
                {
                    "organizerId": ""
                }
                """;

        client.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("PATCH /requisitions/{id}: обновление APPROVED заявки — 400")
    void update_approved_shouldReturn400() {
        String body = """
                {
                    "organizerId": "updated-user"
                }
                """;

        client.patch().uri(BASE_URL + "/2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("REQUISITION_NOT_IN_DRAFT");
    }

    @Test
    @DisplayName("DELETE /requisitions/{id}: удаление APPROVED заявки — 400")
    void delete_approved_shouldReturn400() {
        client.delete().uri(BASE_URL + "/2")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("REQUISITION_DELETE_FORBIDDEN");
    }
}
