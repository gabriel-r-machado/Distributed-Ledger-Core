package com.wallet.wallet.repositories;

import com.wallet.wallet.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    // cria o SQL sozinho.
    Optional<User> findUserByDocument(String document);
    
    /**
     * Busca APENAS o ID da carteira do usuário sem carregar as entidades.
     * Isso evita que a Wallet seja carregada no contexto de persistência SEM LOCK.
     * 
     * @param userId ID do usuário
     * @return ID da carteira (String) ou null se usuário não existe
     */
    @Query("SELECT u.wallet.id FROM users u WHERE u.id = :userId")
    String findWalletIdByUserId(@Param("userId") String userId);
}