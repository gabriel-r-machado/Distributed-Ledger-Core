package com.wallet.wallet.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity(name = "wallets")
@Table(name = "wallets")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Regra de Ouro: Dinheiro SEMPRE use BigDecimal. Nunca Double (dá erro de arredondamento).
    private BigDecimal balance = BigDecimal.ZERO; // Começa com zero

    @OneToOne
    @JoinColumn(name = "user_id") // Cria a coluna user_id na tabela wallet
    private User user;
}