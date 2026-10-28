package com.smartdesk.controller;

import com.smartdesk.client.AnaliseException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AnaliseException.class)
    public ResponseEntity<Map<String, String>> analise(AnaliseException e) {
        return ResponseEntity.status(e.status()).body(Map.of("erro", e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("erro", message == null ? "Chamado inválido." : message));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformed() {
        return ResponseEntity.badRequest().body(Map.of("erro", "Envie um JSON válido com o campo descricao."));
    }
}
