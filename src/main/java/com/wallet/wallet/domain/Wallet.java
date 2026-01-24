package com.wallet.wallet.domain;

import com.fasterxml.jackson.annotation.JsonIgnore; // <--- NÃO ESQUEÇA ESSE IMPORT
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity(name="wallets")
@Table(name="wallets")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of="id")
public class Wallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private BigDecimal balance = BigDecimal.ZERO;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore  // <--- O SEGREDO PRA CONSERTAR O SWAGGER
    private User user;
    
    public Wallet(BigDecimal balance, User user){
        this.balance = balance;
        this.user = user;
    }
}