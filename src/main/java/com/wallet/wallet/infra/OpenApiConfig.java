package com.wallet.wallet.infra;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Distributed Ledger Core - Financial Engine")
                .version("v1.0.0-prod")
                .description("Motor de processamento de transações financeiras de alta performance. " +
                        "Projetado para garantir consistência ACID em cenários de alta concorrência. \n\n" +
                        "**Arquitetura:** Cloud-Native (AWS Elastic Beanstalk + RDS PostgreSQL) \n" +
                        "**Resiliência:** Circuit Breaker & Retry Patterns (Resilience4j) \n" +
                        "**Consistência:** Pessimistic Locking (Row-Level) com SELECT FOR UPDATE \n" +
                        "**Observabilidade:** AWS CloudWatch & Spring Actuator \n" +
                        "**Segurança:** BCrypt Password Hashing + Spring Security")
                .contact(new Contact()
                        .name("Gabriel Machado | Software Engineer")
                        .url("https://www.linkedin.com/in/gabrielmachado-se")
                        .email("gabriel.machado@example.com"))
                .license(new License().name("Enterprise Edition 1.0").url("https://github.com/gabriel-r-machado/Distributed-Ledger-Core")));
    }
}
