package com.wallet.wallet.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "users") // Define que é uma tabela no banco
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id") // Comparar Users apenas pelo ID
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Gera IDs únicos automaticamente (UUID é mais seguro que 1, 2, 3...)
    private String id;

    @Column(unique = true) // Não pode ter 2 CPFs iguais
    private String document;

    @Column(unique = true) // Não pode ter 2 emails iguais
    private String email;

    private String firstName;
    private String lastName;

    private String password;

    // criar um Enum para diferenciar Lojista de Usuário Comum
    @Enumerated(EnumType.STRING)
    private UserType userType;

    // Relacionamento: Um Usuário TEM UMA Carteira
    // mappedBy diz que a chave estrangeira está na tabela Wallet
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;
}