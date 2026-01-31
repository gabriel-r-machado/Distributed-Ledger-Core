package com.wallet.wallet.services;

import com.wallet.wallet.domain.Transaction;
import com.wallet.wallet.domain.User;
import com.wallet.wallet.domain.Wallet;
import com.wallet.wallet.dtos.TransactionDTO;
import com.wallet.wallet.infra.LogMasker;
import com.wallet.wallet.repositories.TransactionRepository;
import com.wallet.wallet.repositories.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AuthorizationService authorizationService;

    /**
     * Creates a transaction with pessimistic locking to prevent race conditions.
     * 
     * Locking strategy:
     * - Acquires FOR UPDATE lock on both sender and receiver wallets
     * - Validates balance after lock acquisition
     * - External authorization happens before locking to avoid blocking database
     * 
     * @param transaction Transfer details (amount, sender, receiver)
     * @return Persisted transaction
     * @throws Exception if validation fails or balance insufficient
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        
        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());
        userService.validateTransaction(sender, transaction.value());

        boolean isAuthorized = this.authorizationService.authorizeTransaction(
            sender.getId(),
            transaction.value()
        );
        
        if (!isAuthorized) {
            throw new Exception("Transação não autorizada");
        }

        log.info("🔒 [Thread {}] Tentando adquirir LOCK para userId: {}", 
            Thread.currentThread().threadId(), LogMasker.maskUserId(sender.getId()));
        
        entityManager.flush();
        entityManager.clear();
        
        Wallet senderWallet = this.walletRepository.findWalletByUserIdLockedNative(sender.getId())
                .orElseThrow(() -> new Exception("Carteira do remetente não encontrada"));
        
        log.info("✅ [Thread {}] LOCK ADQUIRIDO! Saldo atual: {}", 
            Thread.currentThread().threadId(), LogMasker.maskBalance(senderWallet.getBalance()));
        
        if (senderWallet.getBalance().compareTo(transaction.value()) < 0) {
            log.warn("❌ [Thread {}] SALDO INSUFICIENTE! Valor tentado: {}", 
                Thread.currentThread().threadId(), LogMasker.maskBalance(transaction.value()));
            throw new Exception("Saldo insuficiente na carteira");
        }
        
        Wallet receiverWallet = this.walletRepository.findWalletByUserIdLockedNative(receiver.getId())
                .orElseThrow(() -> new Exception("Carteira do recebedor não encontrada"));

        senderWallet.setBalance(senderWallet.getBalance().subtract(transaction.value()));
        receiverWallet.setBalance(receiverWallet.getBalance().add(transaction.value()));
        
        log.info("💰 [Thread {}] Transferência executada! Valor: {}", 
            Thread.currentThread().threadId(), LogMasker.maskBalance(transaction.value()));

        this.walletRepository.save(senderWallet);
        this.walletRepository.save(receiverWallet);
        
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());
        this.repository.save(newTransaction);

        log.debug("✅ [Thread {}] Transação persistida. Lock será liberado no commit.", Thread.currentThread().threadId());
        return newTransaction;
    }
}
