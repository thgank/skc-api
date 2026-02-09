package com.tslnkk.skcapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Авторизация", description = "Проверка текущей аутентификации пользователя")
public class AuthController {

    @Schema(description = "Информация о текущем аутентифицированном пользователе")
    public record AuthMeResponse(
            @Schema(description = "Имя пользователя", example = "admin")
            String username,
            @Schema(description = "Роли пользователя", example = "[\"ROLE_ADMIN\"]")
            List<String> roles
    ) {
    }

    @Operation(summary = "Текущий пользователь", description = "Возвращает пользователя из Basic Auth контекста.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь аутентифицирован",
                    content = @Content(schema = @Schema(implementation = AuthMeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();
        return ResponseEntity.ok(new AuthMeResponse(authentication.getName(), roles));
    }
}
