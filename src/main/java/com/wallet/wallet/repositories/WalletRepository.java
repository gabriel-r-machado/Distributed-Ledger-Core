package com.wallet.wallet.repositories;

import com.wallet.wallet.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    
    /**
     * Acquires pessimistic write lock on wallet row.
     * 
     * Native query ensures PostgreSQL FOR UPDATE is applied correctly.
     * Blocks concurrent transactions until lock is released.
     * 
     * @param userId Wallet owner ID
     * @return Locked wallet or empty if not found
     */
    @Query(value = "SELECT w.* FROM wallets w WHERE w.user_id = :userId FOR UPDATE", nativeQuery = true)
    Optional<Wallet> findWalletByUserIdLockedNative(@Param("userId") String userId);
}