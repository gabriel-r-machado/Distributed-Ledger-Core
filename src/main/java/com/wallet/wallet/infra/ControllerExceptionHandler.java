package com.wallet.wallet.infra;

import com.wallet.wallet.dtos.ExceptionDTO;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler para tratamento centralizado de exceções.
 * 
 * Cobre:
 * - Validações de entrada (Bean Validation)
 * - Erros de integridade de dados (duplicatas)
 * - Recursos não encontrados
 * - Argumentos ilegais
 * - Erros gerais
 */
@RestControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    /**
     * Trata erros de validação do Bean Validation (@Valid).
     * Retorna mapa com campos inválidos e mensagens.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        
        exception.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        logger.warn("Erro de validação de entrada: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Trata violações de integridade (ex: email ou documento duplicado).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionDTO> handleDuplicateEntry(DataIntegrityViolationException exception) {
        logger.warn("Violação de integridade de dados: {}", exception.getMessage());
        
        String message = "Usuário já cadastrado";
        if (exception.getMessage() != null && exception.getMessage().contains("document")) {
            message = "Documento já cadastrado no sistema";
        } else if (exception.getMessage() != null && exception.getMessage().contains("email")) {
            message = "Email já cadastrado no sistema";
        }
        
        ExceptionDTO exceptionDTO = new ExceptionDTO(message, "400");
        return ResponseEntity.badRequest().body(exceptionDTO);
    }

    /**
     * Trata recursos não encontrados (404).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ExceptionDTO> handleNotFound(EntityNotFoundException exception) {
        logger.debug("Recurso não encontrado: {}", exception.getMessage());
        ExceptionDTO exceptionDTO = new ExceptionDTO("Recurso não encontrado", "404");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionDTO);
    }

    /**
     * Trata argumentos ilegais (ex: transação de lojista, saldo insuficiente).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionDTO> handleIllegalArgument(IllegalArgumentException exception) {
        logger.warn("Argumento inválido: {}", exception.getMessage());
        ExceptionDTO exceptionDTO = new ExceptionDTO(exception.getMessage(), "400");
        return ResponseEntity.badRequest().body(exceptionDTO);
    }

    /**
     * Fallback para exceções gerais não tratadas.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDTO> handleGeneralException(Exception exception) {
        logger.error("Erro interno do servidor", exception);
        
        // Em desenvolvimento, retorna mensagem detalhada; em produção, genérica
        String message = exception.getMessage() != null && !exception.getMessage().isEmpty()
            ? exception.getMessage()
            : "Erro interno do servidor";
        
        ExceptionDTO exceptionDTO = new ExceptionDTO(message, "500");
        return ResponseEntity.internalServerError().body(exceptionDTO);
    }
}
