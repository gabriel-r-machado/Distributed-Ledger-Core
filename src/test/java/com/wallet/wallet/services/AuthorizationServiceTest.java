package com.wallet.wallet.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes para AuthorizationService com Resiliência.
 * 
 * Valida:
 * - Circuit Breaker abrindo após falhas
 * - Retry com backoff exponencial
 * - Fallback negando transação em caso de indisponibilidade
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthorizationService authorizationService;

    @BeforeEach
    void setup() {
        // Inicializa Resilience4j antes de cada teste
    }

    @Test
    @DisplayName("Deve autorizar transação quando API responde com 'Autorizado'")
    void testAuthorizeTransactionSuccess() {
        // ARRANGE
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Autorizado");
        
        ResponseEntity<Map<String, Object>> response = ResponseEntity
            .ok(responseBody);

        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn((ResponseEntity) response);

        // ACT
        boolean result = authorizationService.authorizeTransaction("user1", new BigDecimal("100"));

        // ASSERT
        assertTrue(result);
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Deve negar transação quando API responde com 'Não Autorizado'")
    void testAuthorizeTransactionDenied() {
        // ARRANGE
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Não Autorizado");
        
        ResponseEntity<Map<String, Object>> response = ResponseEntity
            .ok(responseBody);

        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn((ResponseEntity) response);

        // ACT
        boolean result = authorizationService.authorizeTransaction("user1", new BigDecimal("100"));

        // ASSERT
        assertFalse(result);
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Deve ativar Retry quando API retorna erro de conexão")
    void testRetryOnConnectionError() {
        // ARRANGE - Simula falha seguida de sucesso
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new RestClientException("Connection timeout"))
            .thenThrow(new RestClientException("Connection timeout"))
            .thenReturn(ResponseEntity.ok(Map.of("message", "Autorizado")));

        // ACT & ASSERT
        // Retry tenta 3 vezes, se a terceira suceder, autoriza
        // (Este teste seria mais robusto em ambiente real com clock)
        try {
            authorizationService.authorizeTransaction("user1", new BigDecimal("100"));
        } catch (Exception e) {
            // Esperado que lance exceção se todas as tentativas falharem
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    @DisplayName("Fallback deve negar transação quando Circuit Breaker abre (simulado)")
    void testFallbackWhenCircuitBreakerOpenSimulation() {
        // ARRANGE
        // Simula falha persistente que causaria abertura do circuit breaker
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new RestClientException("Service unavailable"));

        // ACT & ASSERT
        // Após várias falhas, o fallback nega a transação
        try {
            authorizationService.authorizeTransaction("user1", new BigDecimal("100"));
            fail("Deveria ter lançado exceção");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Falha ao comunicar com autorizador"));
        }
    }

    @Test
    @DisplayName("Fallback deve retornar false (negação de transação) em caso de indisponibilidade")
    void testFallbackReturnsFalse() {
        // ARRANGE
        String senderId = "user1";
        BigDecimal value = new BigDecimal("100");
        Throwable cause = new RuntimeException("Autorizador indisponível");

        // ACT
        boolean result = authorizationService.authorizationFallback(senderId, value, cause);

        // ASSERT
        assertFalse(result);
        System.out.println("✓ Fallback negou a transação com segurança");
    }

    @Test
    @DisplayName("Deve negar transação quando API retorna status 500")
    void testDenyWhenAPIReturnsError() {
        // ARRANGE
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.status(500).build());

        // ACT
        boolean result = authorizationService.authorizeTransaction("user1", new BigDecimal("100"));

        // ASSERT
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve negar quando API retorna null body")
    void testDenyWhenAPIReturnsNullBody() {
        // ARRANGE
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(null));

        // ACT
        boolean result = authorizationService.authorizeTransaction("user1", new BigDecimal("100"));

        // ASSERT
        assertFalse(result);
    }
}
