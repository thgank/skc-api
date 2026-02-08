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
 * Тест оптимистичной блокировки.
 * <p>
 * Сценарий: два последовательных PATCH на одну позицию с одной и той же версией.
 * Первый запрос должен успешно обновить позицию (version 0 → 1).
 * Второй запрос с устаревшей версией (0) должен получить 409 Conflict.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OptimisticLockingTest {

    @LocalServerPort
    private int port;

    private RestTestClient client;

    private static final String ITEM_URL = "/api/v1/requisitions/1/items/1";

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeaders(headers -> headers.setBasicAuth("admin", "admin"))
                .build();
    }

    @Test
    @DisplayName("Optimistic Locking: второй PATCH с устаревшей версией → 409 Conflict")
    void patchWithStaleVersion_shouldReturn409() {
        String firstPatch = """
                {
                    "quantity": 200,
                    "version": 0
                }
                """;

        client.patch().uri(ITEM_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(firstPatch)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.version").isEqualTo(1);

        String secondPatch = """
                {
                    "quantity": 300,
                    "version": 0
                }
                """;

        client.patch().uri(ITEM_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(secondPatch)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("OPTIMISTIC_LOCK_CONFLICT");

        String thirdPatch = """
                {
                    "quantity": 300,
                    "version": 1
                }
                """;

        client.patch().uri(ITEM_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(thirdPatch)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.version").isEqualTo(2);
    }
}
