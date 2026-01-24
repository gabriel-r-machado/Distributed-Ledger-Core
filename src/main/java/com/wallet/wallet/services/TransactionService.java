package com.wallet.wallet.services;

import com.wallet.wallet.domain.Transaction;
import com.wallet.wallet.domain.User;
import com.wallet.wallet.dtos.TransactionDTO;
import com.wallet.wallet.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TransactionService {
    
    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Transactional // A Mágica do ACID (Atomicidade)
    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        // 1. Busca os usuários no banco
        User sender = this.userService.findUserById(transaction.senderId());
        User receiver = this.userService.findUserById(transaction.receiverId());

        // 2. Valida se quem envia tem saldo e não é lojista
        userService.validateTransaction(sender, transaction.value());

        // 3. Atualiza os saldos (Tira de um, põe no outro)
        sender.getWallet().setBalance(sender.getWallet().getBalance().subtract(transaction.value()));
        receiver.getWallet().setBalance(receiver.getWallet().getBalance().add(transaction.value()));

        // 4. Cria o registro da transação (o extrato)
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        // 5. Salva tudo no banco
        this.repository.save(newTransaction);
        this.userService.saveUser(sender);   // Atualiza saldo do remetente
        this.userService.saveUser(receiver); // Atualiza saldo do recebedor

        return newTransaction;
    }
}