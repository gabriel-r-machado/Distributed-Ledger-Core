package com.wallet.wallet.services;

import com.wallet.wallet.domain.User;
import com.wallet.wallet.domain.UserType;
import com.wallet.wallet.domain.Wallet;
import com.wallet.wallet.dtos.TransactionDTO;
import com.wallet.wallet.repositories.UserRepository;
import com.wallet.wallet.repositories.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Disabled("Desabilitado temporariamente devido a problemas com Docker/TestContainers. O PostgresConcurrencyTest já cobre o cenário de concorrência.")
public class ConcurrencyTransactionTest {

    // SOBE UM POSTGRES REAL NO DOCKER SÓ PARA ESSE TESTE
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("test_wallet")
            .withUsername("test")
            .withPassword("test");

    // Configura o Spring para conectar nesse container dinâmico
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @MockBean
    private AuthorizationService authorizationService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setup() {
        walletRepository.deleteAll();
        userRepository.deleteAll();

        when(authorizationService.authorizeTransaction(anyString(), any(BigDecimal.class)))
            .thenReturn(true);

        sender = new User(null, "Sender", "Test", "11111111111", "sender@test.com", "123", UserType.COMMON, null);
        userRepository.save(sender);
        Wallet senderWallet = new Wallet(null, new BigDecimal("100"), sender);
        walletRepository.save(senderWallet);
        sender.setWallet(senderWallet);
        userRepository.save(sender);

        receiver = new User(null, "Receiver", "Test", "22222222222", "receiver@test.com", "123", UserType.COMMON, null);
        userRepository.save(receiver);
        Wallet receiverWallet = new Wallet(null, new BigDecimal("0"), receiver);
        walletRepository.save(receiverWallet);
        receiver.setWallet(receiverWallet);
        userRepository.save(receiver);
    }

    @Test
    @Disabled("Desabilitado temporariamente devido a problemas com Docker/TestContainers. O PostgresConcurrencyTest já cobre o cenário de concorrência.")
    @DisplayName("RACE CONDITION: 5 Threads - PostgreSQL Real via TestContainers")
    void testConcurrentTransfersWithPessimisticLocking() throws InterruptedException {
        int numThreads = 5;
        BigDecimal transferAmount = new BigDecimal("100"); 
        
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    latch.await(); 
                    TransactionDTO request = new TransactionDTO(
                        transferAmount, sender.getId(), receiver.getId()
                    );
                    transactionService.createTransaction(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }, executorService);
            futures.add(future);
        }

        latch.countDown();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        // AGORA ISSO DEVE PASSAR PORQUE O POSTGRES RESPEITA O LOCK
        assertEquals(1, successCount.get(), "Lock FALHOU! Mais de uma transação passou.");
        assertEquals(4, failureCount.get(), "Deveriam ter 4 falhas.");

        Wallet updatedSender = walletRepository.findById(sender.getWallet().getId()).orElseThrow();
        assertEquals(BigDecimal.ZERO.setScale(2), updatedSender.getBalance().setScale(2));
    }
}
