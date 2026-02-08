package com.tslnkk.skcapi.exception;

import com.tslnkk.skcapi.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Централизованный обработчик исключений для всех REST-эндпоинтов.
 * Логирование на уровне методов обеспечивает {@link com.tslnkk.skcapi.aspect.LoggingAspect}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse response = new ErrorResponse(
                ex.getErrorCode().name(),
                ex.getMessage(),
                ex.getField(),
                ex.getRejectedValue()
        );
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex) {
        ErrorResponse response = new ErrorResponse(
                ErrorCode.OPTIMISTIC_LOCK_CONFLICT.name(),
                ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getDefaultMessage(),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        var fieldError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String errorCode = "VALIDATION_ERROR";
        String message = "Ошибка валидации";
        String field = null;
        Object rejectedValue = null;

        if (fieldError != null) {
            field = fieldError.getField();
            rejectedValue = fieldError.getRejectedValue();
            message = fieldError.getDefaultMessage();

            errorCode = mapFieldToErrorCode(field, errorCode);
        }

        ErrorResponse response = new ErrorResponse(errorCode, message, field, rejectedValue);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse response = new ErrorResponse(
                "INVALID_REQUEST_BODY",
                "Невалидный формат тела запроса",
                null,
                null
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse response = new ErrorResponse(
                "INVALID_PARAMETER",
                "Неверный тип параметра: " + ex.getName(),
                ex.getName(),
                ex.getValue()
        );
        return ResponseEntity.badRequest().body(response);
    }

    private String mapFieldToErrorCode(String field, String defaultCode) {
        return switch (field) {
            case "quantity" -> ErrorCode.INVALID_QUANTITY.name();
            case "desiredDeliveryDate" -> ErrorCode.INVALID_DELIVERY_DATE.name();
            case "version" -> ErrorCode.OPTIMISTIC_LOCK_CONFLICT.name();
            default -> defaultCode;
        };
    }
}
