package com.wallet.wallet.repositories;

import com.wallet.wallet.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    // cria o SQL sozinho.
    Optional<User> findUserByDocument(String document);
}