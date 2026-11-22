package com.ecardfolio.card;

import org.springframework.http.HttpStatus;
import com.ecardfolio.account.AccountConflictException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthenticationException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> authenticationFailed(AuthenticationException ex) { return Map.of("error", "Email 或密碼不正確。"); }
    @ExceptionHandler(AccountConflictException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> accountConflict(AccountConflictException ex) { return Map.of("error", ex.getMessage()); }
    @ExceptionHandler(CardNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(CardNotFoundException ex) { return Map.of("error", ex.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().findFirst().map(e -> e.getField() + " 欄位格式不正確").orElse("請求資料不正確");
        return Map.of("error", message);
    }
}
