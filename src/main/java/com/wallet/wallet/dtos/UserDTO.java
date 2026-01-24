package com.wallet.wallet.dtos;

import com.wallet.wallet.domain.UserType;
import java.math.BigDecimal;

// Record: O Java cria getters, equals, hashcode e toString automaticamente.
public record UserDTO(
    String firstName, 
    String lastName, 
    String document, 
    BigDecimal balance, 
    String email, 
    String password, 
    UserType userType
) {
}