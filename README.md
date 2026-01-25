# Distributed Ledger Core (Wallet API)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?logo=java&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker&logoColor=white)](https://www.docker.com/)

> **Engine transacional financeira focada em integridade de dados (ACID), alta concorrência e resiliência.**

Este projeto implementa o *core* de uma carteira digital, resolvendo desafios críticos de sistemas financeiros: prevenção de *double-spending*, condições de corrida (race conditions) e consistência eventual em integrações distribuídas.

---

## 🏛️ Arquitetura & Decisões de Engenharia

O sistema foi desenhado seguindo princípios de **Layered Architecture** com foco em domínio, garantindo que regras de negócio críticas (transferências, validações de saldo) estejam isoladas de detalhes de infraestrutura.

### 1. Concorrência e Integridade (O Diferencial)
O maior desafio em sistemas de pagamentos é garantir que duas transações simultâneas não debitem o mesmo saldo duas vezes.
* **Solução:** Implementação de **Pessimistic Locking** (`SELECT ... FOR UPDATE`) no nível do banco de dados via JPA.
* **Resultado:** Serialização de transações concorrentes na mesma carteira (Wallet), garantindo consistência estrita (Strong Consistency) e eliminando anomalias de escrita.

### 2. Resiliência e Fallbacks
A aprovação da transação depende de um Autorizador Externo (microsserviço simulado).
* **Problema:** Latência ou indisponibilidade do serviço externo não pode travar a thread do banco de dados (Starvation).
* **Solução:** Implementação de padrões de resiliência. Em caso de falha/timeout do autorizador, o sistema aciona uma estratégia de **Fallback** segura, priorizando a disponibilidade sem comprometer a segurança da operação.

### 3. Auditabilidade (Ledger)
* **Imutabilidade:** Nenhuma transação altera o saldo sem deixar um rastro. O modelo de dados trata a entidade `Transaction` como um *Ledger* imutável (Append-Only), permitindo auditoria completa e reconciliação financeira.

---

## 🛠️ Tech Stack

* **Linguagem:** Java 21 (LTS) - Utilizando Records e Pattern Matching.
* **Framework:** Spring Boot 3 (Web, Data JPA, Validation).
* **Database:** PostgreSQL (Isolamento de transação e confiabilidade ACID).
* **Testes:** JUnit 5, Mockito & Integração.
* **Infra:** Docker & Docker Compose.

---

## 🚀 Como Executar

### Pré-requisitos
* Docker & Docker Compose
* Java 21 (Apenas se quiser rodar fora do container)

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/SEU-USUARIO/distributed-ledger-core.git](https://github.com/SEU-USUARIO/distributed-ledger-core.git)
    cd distributed-ledger-core
    ```

2.  **Suba a Infraestrutura (Banco de Dados):**
    ```bash
    docker-compose up -d
    ```

3.  **Execute a Aplicação:**
    ```bash
    ./mvnw spring-boot:run
    ```

A API estará disponível em: `http://localhost:8081`

---

## 📚 Documentação da API

A documentação interativa (OpenAPI/Swagger) é gerada automaticamente e permite testar os endpoints de transferência, criação de usuários e consulta de extrato.

* **Swagger UI:** `http://localhost:8081/swagger-ui/index.html`
* **Spec JSON:** `http://localhost:8081/v3/api-docs`

---

## 🧪 Estratégia de Testes

A qualidade do código é garantida através de uma pirâmide de testes focada nas regras críticas:

* **Service Layer:** Testes unitários com Mockito validando cenários de *Edge Case* (saldo insuficiente, lojista tentando transferir, falha externa).
* **Concurrency Tests:** Cenários de carga validando o funcionamento do *Lock Pessimista* sob estresse.

---

## 🔮 Roadmap & Melhorias

* [ ] Implementação de **Spring Security** com OAuth2 para autenticação de clientes.
* [ ] Pipeline de CI/CD (GitHub Actions) para build automático da imagem Docker.
* [ ] Observabilidade: Integração com **Spring Actuator** e Prometheus para métricas de latência e throughput.

---

Desenvolvido por **Gabriel Machado** — *Software Engineer*
