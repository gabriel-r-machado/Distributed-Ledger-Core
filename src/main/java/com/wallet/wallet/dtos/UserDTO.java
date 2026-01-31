package com.wallet.wallet.dtos;

import com.wallet.wallet.domain.UserType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * DTO para criação de usuário com validações de entrada.
 */
public record UserDTO(
    @NotBlank(message = "Nome não pode ser vazio")
    @Size(min = 2, max = 50, message = "Nome deve ter entre 2 e 50 caracteres")
    String firstName,
    
    @NotBlank(message = "Sobrenome não pode ser vazio")
    @Size(min = 2, max = 50, message = "Sobrenome deve ter entre 2 e 50 caracteres")
    String lastName,
    
    @NotBlank(message = "Documento não pode ser vazio")
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "Documento deve ter 11 (CPF) ou 14 (CNPJ) dígitos")
    String document,
    
    @NotNull(message = "Saldo inicial não pode ser nulo")
    @DecimalMin(value = "0.0", message = "Saldo não pode ser negativo")
    BigDecimal balance,
    
    @NotBlank(message = "Email não pode ser vazio")
    @Email(message = "Email inválido")
    String email,
    
    @NotBlank(message = "Senha não pode ser vazia")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    String password,
    
    @NotNull(message = "Tipo de usuário não pode ser nulo")
    UserType userType
) {
}