package com.tslnkk.skcapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SKC Purchase Requisition API")
                        .version("1.0.0")
                        .description("""
                                REST API для управления позициями в заявке на закупку.

                                Основные функции:
                                - Создание позиций в заявке (только DRAFT)
                                - Частичное обновление позиций (PATCH, optimistic locking)
                                - Удаление позиций (кроме последней)
                                - Получение сводки по заявке
                                - Реактивация отменённых заявок (CANCELLED → DRAFT)

                                Аутентификация: Basic Auth (admin/admin, user/user)
                                """)
                        .contact(new Contact()
                                .name("Сериков Нурсултан")
                                .email("sultanhuman02@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")));
    }
}
