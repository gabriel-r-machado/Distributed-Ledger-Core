# Resiliência na Autorização Externa - Resilience4j

## 📋 Visão Geral

O sistema implementa **Circuit Breaker** com **Retry** e **Fallback** para proteger chamadas ao microsserviço autorizador externo contra latências, timeouts e indisponibilidades.

---

## 🏗️ Arquitetura de Resiliência

```
┌─────────────────────────────────────────────────────────────┐
│ TransactionService.createTransaction()                       │
│                                                               │
│  1️⃣ Busca usuários e valida regras de negócio               │
│                                                               │
│  2️⃣ Chama AuthorizationService.authorizeTransaction() ───── │
│     com Resilience4j envolvendo a chamada HTTP              │
│                                                               │
│  3️⃣ Se falhar:                                               │
│     ├─ Retry: Tenta novamente (até 3 vezes)                │
│     ├─ Circuit Breaker: Abre se > 50% de falhas             │
│     └─ Fallback: Nega a transação (seguro)                 │
│                                                               │
│  4️⃣ Se autorizado: Continua com Pessimistic Locking         │
│     (adquire locks nas wallets)                             │
│                                                               │
│  5️⃣ Debita/Credita e faz commit (libera locks)             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Componentes Implementados

### 1. **AuthorizationService** (`AuthorizationService.java`)

Encapsula a lógica de chamada ao autorizador externo com resiliência.

```java
@CircuitBreaker(
    name = "authorizerCircuitBreaker",
    fallbackMethod = "authorizationFallback"
)
@Retry(name = "authorizerRetry")
public boolean authorizeTransaction(String senderId, BigDecimal value) {
    // Chamada HTTP ao Mocky
}

// Executado quando Circuit Breaker ou Retry falham
public boolean authorizationFallback(String senderId, BigDecimal value, Throwable ex) {
    return false; // Nega a transação com segurança
}
```

### 2. **Configuração Resilience4j** (`application.properties`)

Define os limites e thresholds de falha:

```properties
# Circuit Breaker: Abre após 50% de falhas em 10 chamadas
resilience4j.circuitbreaker.instances.authorizerCircuitBreaker.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.authorizerCircuitBreaker.sliding-window-size=10
resilience4j.circuitbreaker.instances.authorizerCircuitBreaker.wait-duration-in-open-state=30000

# Retry: Tenta 3 vezes com backoff exponencial (100ms → 200ms → 400ms)
resilience4j.retry.instances.authorizerRetry.max-attempts=3
resilience4j.retry.instances.authorizerRetry.wait-duration=100
resilience4j.retry.instances.authorizerRetry.interval-function=exponential
resilience4j.retry.instances.authorizerRetry.exponential-backoff-multiplier=2.0
```

### 3. **TransactionService** (Refatorado)

Agora injeta `AuthorizationService` em vez de chamar HTTP diretamente:

```java
@Autowired
private AuthorizationService authorizationService;

@Transactional
public Transaction createTransaction(TransactionDTO transaction) throws Exception {
    // ... validações ...
    
    // Chamada protegida por resiliência
    boolean isAuthorized = this.authorizationService.authorizeTransaction(
        sender.getId(),
        transaction.value()
    );
    
    if (!isAuthorized) {
        throw new Exception("Transação não autorizada (...)");
    }
    
    // ... resto da lógica com locks ...
}
```

---

## 🔄 Padrões Implementados

### **Circuit Breaker (Estado de Máquina)**

O circuit breaker tem 3 estados:

| Estado | Descrição | Comportamento |
|--------|-----------|---------------|
| **CLOSED** | Tudo normal | Requisições passam direto, são monitoradas |
| **OPEN** | Muitas falhas | Requisições são rejeitadas imediatamente (fail-fast) |
| **HALF_OPEN** | Tentando se recuperar | Permite algumas requisições para testar se o serviço voltou |

**Transição:**
```
CLOSED → (50% falhas em 10 chamadas) → OPEN → (espera 30s) → HALF_OPEN → (testa) → CLOSED ou OPEN
```

### **Retry com Backoff Exponencial**

Quando uma requisição falha (ex: timeout), o sistema retenta com espera crescente:

```
Tentativa 1: Chamada falha
  ↓ Espera 100ms
Tentativa 2: Chamada falha
  ↓ Espera 200ms (100 × 2)
Tentativa 3: Chamada falha
  ↓ Espera 400ms (200 × 2)
Fallback: Nega a transação
```

**Vantagens:**
- Não sobrecarrega o serviço com tentativas imediatas
- Aumenta gradualmente as chances de sucesso
- Economiza recursos

### **Fallback (Fail-Safe)**

Quando tudo falha (Circuit Breaker aberto ou todas as tentativas esgotadas):

```java
public boolean authorizationFallback(...) {
    // Estratégia conservadora: NEGA a transação
    return false;
}
```

**Por que negar?**
- Segurança em primeiro lugar (evita fraude)
- Se o autorizador está fora, não devemos confiar
- Cliente recebe mensagem clara: "Serviço indisponível"

---

## 📊 Exemplos de Cenários

### **Cenário 1: Autorização Normal**
```
1. Chama HTTP → Responde "Autorizado" em 200ms
2. Circuit breaker: CLOSED (tudo bem)
3. Transação autorizada → Prossegue
```

### **Cenário 2: Timeout Temporário**
```
1. Chama HTTP → Timeout (3s)
2. Retry 1: Espera 100ms → Tenta novamente → Sucesso!
3. Circuit breaker: CLOSED
4. Transação autorizada
```

### **Cenário 3: Microsserviço Down**
```
1. Chama HTTP → Falha (Connection refused)
2. Retry 1: Espera 100ms → Falha
3. Retry 2: Espera 200ms → Falha
4. Retry 3: Espera 400ms → Falha
5. Circuit breaker entra em OPEN (detecta padrão de falha)
6. Próximas chamadas: Rejeitadas imediatamente (fail-fast)
7. Fallback acionado → **Transação negada com segurança**
8. Cliente recebe: "Transação não autorizada (autorizador indisponível)"
```

### **Cenário 4: Recuperação do Microsserviço**
```
1. Circuit breaker estava OPEN (serviço estava down)
2. Após 30 segundos → Muda para HALF_OPEN
3. Próxima requisição → Testada
4. Se suceder → CLOSED (volta ao normal)
5. Se falhar → Volta para OPEN (dá mais tempo)
```

---

## 🛡️ Proteção contra Cascata de Falhas

**Sem Resilience4j:**
```
Autorizador lento (5s)
↓
Thread do banco aguardando 5s
↓
Mais requisições → Mais threads bloqueadas
↓
Pool de threads esgotado
↓
Banco de dados fica inacessível
↓ TOTAL FAILURE
```

**Com Resilience4j:**
```
Autorizador lento (5s)
↓ Timeout detectado
Retry automaticamente
↓ Se falhar → Fallback (nega em ms)
↓
Thread liberada rapidamente
↓
Sistema continua respondendo
↓ GRACEFUL DEGRADATION
```

---

## 📈 Monitoramento & Observabilidade

Para visualizar o estado do Circuit Breaker (requer `spring-boot-starter-actuator`):

```bash
# Status do Circuit Breaker
curl http://localhost:8081/actuator/health

# Métricas detalhadas
curl http://localhost:8081/actuator/circuitbreakers
```

Resposta exemplo:
```json
{
  "status": "UP",
  "components": {
    "authorizerCircuitBreaker": {
      "status": "UP",
      "details": {
        "state": "CLOSED",
        "failure_rate": "0%",
        "buffered_calls": 5,
        "failed_calls": 0
      }
    }
  }
}
```

---

## 🧪 Testes

### **AuthorizationServiceTest**
Valida:
- ✅ Sucesso em autorização
- ✅ Negação por API
- ✅ Retry em falha de conexão
- ✅ Fallback negando transação (seguro)
- ✅ Comportamento com erros 5xx

### **TransactionServiceTest** (Atualizado)
Agora testa:
- ✅ Autorização bem-sucedida
- ✅ Rejeição quando autorizador nega
- ✅ Lockups de pessimistic locking funcionam
- ✅ Detecção de saldo insuficiente

---

## 🚀 Decisões de Design

| Decisão | Motivo |
|---------|--------|
| **Nega em fallback** | Segurança > Disponibilidade em sistema financeiro |
| **Chamada antes do lock** | Não trava a thread do banco se a API for lenta |
| **Retry com backoff** | Evita thundering herd, economiza CPU |
| **30s de wait em OPEN** | Tempo razoável para microsserviço se recuperar |
| **50% threshold** | Sensível a anomalias, mas não muito agressivo |

---

## 📝 Dependências Adicionadas

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

## ⚡ Melhorias Futuras

1. **Timeout explícito**: Adicionar `@Timeout` do Resilience4j
2. **Bulkhead**: Limitar threads simultâneas ao autorizador
3. **Distribuído**: Usar Redis para compartilhar estado do circuit breaker entre instâncias
4. **Observabilidade**: Integrar com Prometheus/Grafana
5. **Alertas**: Configurar alertas quando circuit breaker abre

---

**Desenvolvido com foco em:** Resiliência | Segurança | High Availability ✨
