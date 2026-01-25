# 💸 Wallet API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Available-blue.svg)](https://www.docker.com/)

API Restful simulando uma Carteira Digital (Wallet) simplificada. O projeto foca na integridade de transações financeiras, tratamento de concorrência e resiliência a falhas de serviços externos.

---

## 🚀 Tecnologias & Ferramentas

* **Linguagem:** Java 21 (LTS)
* **Framework:** Spring Boot 3
* **Banco de Dados:** PostgreSQL (via Docker)
* **Testes:** JUnit 5 & Mockito
* **Resiliência:** Implementação de Fallback Pattern
* **Outros:** Lombok, Spring Data JPA, Docker Compose

---

## ⚙️ Arquitetura e Decisões Técnicas

O projeto segue uma arquitetura em camadas (Clean Architecture simplificada) para garantir desacoplamento e testabilidade.

### Fluxo de Transferência (Highlight do Projeto)
1.  **Validação:** O sistema verifica saldo, existência de usuários e tipo de usuário (Lojistas não transferem).
2.  **Atomicidade:** Uso de `@Transactional` para garantir que, em caso de erro, toda a operação sofra *rollback*, evitando inconsistência financeira.
3.  **Autorização Externa:** Antes de confirmar, consultamos um serviço autorizador externo.
    * *Fallback:* Caso o serviço externo esteja indisponível (timeout/erro 500), implementei uma estratégia de fallback para não travar a operação do cliente, garantindo disponibilidade.

### Modelagem de Dados
* **User:** Utiliza `UUID` para maior segurança na identificação.
* **Wallet:** Separação entre Usuário e Carteira para escalabilidade futura (um usuário poderia ter múltiplas carteiras).
* **Transaction:** Registro imutável de todas as operações (Ledger).

---

## 🧪 Testes Automatizados

A camada de serviço (`TransactionService`), que contém a regra de negócio crítica, está coberta por testes unitários utilizando **Mockito**.

* ✅ **Cenários de Sucesso:** Validação de débito/crédito e persistência.
* ✅ **Cenários de Falha:** Tentativas de transferência sem saldo, usuários não autorizados ou falhas de validação.

---

## 📖 Documentação da API (Swagger)

A API possui documentação interativa e pode ser testada diretamente pelo navegador através do Swagger UI.

* **URL:** `http://localhost:8081/swagger-ui/index.html`

---

## 🛠️ Como Rodar o Projeto

### Pré-requisitos
* Docker & Docker Compose
* Java 21 (Opcional se rodar via Docker)

### Passo a Passo
1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/SEU-USUARIO/wallet.git](https://github.com/SEU-USUARIO/wallet.git)
    cd wallet
    ```

2.  **Suba o Banco de Dados:**
    ```bash
    docker-compose up -d
    ```

3.  **Execute a Aplicação:**
    Abra o projeto na sua IDE favorita e execute a classe `WalletApplication` ou use o Maven:
    ```bash
    ./mvnw spring-boot:run
    ```

A API estará disponível em: `http://localhost:8081`

---

## 🔮 Melhorias Futuras (To-Do)

* [ ] Implementar Spring Security + JWT para autenticação.
* [ ] Containerizar a aplicação completa (Dockerfile).
* [ ] Adicionar Logs estruturados e métricas (Actuator).

---

Desenvolvido por **Machado Dev** 👨‍💻
