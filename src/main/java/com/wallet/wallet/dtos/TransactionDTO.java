package com.wallet.wallet.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransactionDTO(
    @NotNull(message = "Valor da transação não pode ser nulo")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal value,
    
    @NotBlank(message = "ID do remetente não pode ser vazio")
    String senderId,
    
    @NotBlank(message = "ID do destinatário não pode ser vazio")
    String receiverId
) {
}