package com.wallet.wallet.services;

import com.wallet.wallet.domain.User;
import com.wallet.wallet.domain.UserType;
import com.wallet.wallet.domain.Wallet;
import com.wallet.wallet.dtos.TransactionDTO;
import com.wallet.wallet.repositories.TransactionRepository;
import com.wallet.wallet.repositories.WalletRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock // Mock do UserService
    private UserService userService;

    @Mock // Mock do Repository
    private TransactionRepository repository;

    @Mock // Mock do WalletRepository (para Pessimistic Locking)
    private WalletRepository walletRepository;

    @Mock // Mock do AuthorizationService (com Resilience4j)
    private AuthorizationService authorizationService;

    @Mock // Mock do EntityManager (para flush e clear)
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks // Injeta os Mocks acima dentro do Service real
    private TransactionService transactionService;

    @BeforeEach
    void setup(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve criar transação com sucesso quando tudo estiver OK e autorizador aprovar")
    void createTransactionCase1() throws Exception {
        // 1. Criamos o Sender (Remetente) com setters
        User sender = new User();
        sender.setId("1");
        sender.setFirstName("Matheus");
        sender.setLastName("Silva");
        sender.setDocument("99999999901");
        sender.setEmail("matheus@email.com");
        sender.setUserType(UserType.COMMON);
        Wallet senderWallet = new Wallet("wallet1", new BigDecimal(100), sender);
        sender.setWallet(senderWallet);

        // 2. O Receiver (Recebedor) com setters
        User receiver = new User();
        receiver.setId("2");
        receiver.setFirstName("Joao");
        receiver.setLastName("Santos");
        receiver.setDocument("99999999902");
        receiver.setEmail("joao@email.com");
        receiver.setUserType(UserType.COMMON);
        Wallet receiverWallet = new Wallet("wallet2", new BigDecimal(100), receiver);
        receiver.setWallet(receiverWallet);

        // 3. Mockamos os UserService
        when(userService.findUserById("1")).thenReturn(sender);
        when(userService.findUserById("2")).thenReturn(receiver);
        doNothing().when(userService).validateTransaction(any(), any());

        // 4. Mockamos o WalletRepository com Pessimistic Locking (Native Query)
        when(walletRepository.findWalletByUserIdLockedNative("1")).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findWalletByUserIdLockedNative("2")).thenReturn(Optional.of(receiverWallet));

        // 5. Mockamos a Autorização (AuthorizationService com Resilience4j)
        when(authorizationService.authorizeTransaction("1", new BigDecimal(10))).thenReturn(true);

        // 6. Mockamos EntityManager (flush e clear são chamados no TransactionService) com lenient
        // porque podem não ser chamados se o teste falhar antes
        lenient().doNothing().when(entityManager).flush();
        lenient().doNothing().when(entityManager).clear();

        TransactionDTO request = new TransactionDTO(new BigDecimal(10), "1", "2");
        
        // 7. AÇÃO
        transactionService.createTransaction(request);

        // 7. VERIFICAÇÃO
        // Verifica se o método save foi chamado para a transação
        verify(repository, times(1)).save(any());
        
        // Verifica se as wallets foram salvas com lock
        verify(walletRepository, times(1)).save(senderWallet);
        verify(walletRepository, times(1)).save(receiverWallet);
        
        // Verifica se o saldo foi atualizado corretamente (Sender tinha 100, agora deve ter 90)
        Assertions.assertEquals(new BigDecimal(90), senderWallet.getBalance());
        Assertions.assertEquals(new BigDecimal(110), receiverWallet.getBalance());
        
        // Verifica se o autorizador foi consultado
        verify(authorizationService, times(1)).authorizeTransaction("1", new BigDecimal(10));
    }

    @Test
    @DisplayName("Deve lançar Exception quando autorizador negar a transação")
    void createTransactionCase2_AuthorizerDenies() throws Exception {
        // 1. Criamos Sender e Receiver
        User sender = new User();
        sender.setId("1");
        sender.setFirstName("Matheus");
        sender.setLastName("Silva");
        sender.setDocument("99999999901");
        sender.setEmail("matheus@email.com");
        sender.setUserType(UserType.COMMON);
        Wallet senderWallet = new Wallet("wallet1", new BigDecimal(100), sender);
        sender.setWallet(senderWallet);

        User receiver = new User();
        receiver.setId("2");
        receiver.setFirstName("Joao");
        receiver.setLastName("Santos");
        receiver.setDocument("99999999902");
        receiver.setEmail("joao@email.com");
        receiver.setUserType(UserType.COMMON);
        Wallet receiverWallet = new Wallet("wallet2", new BigDecimal(100), receiver);
        receiver.setWallet(receiverWallet);

        // 2. Mockamos UserService com lenient para stubbings que podem não ser usados
        when(userService.findUserById("1")).thenReturn(sender);
        when(userService.findUserById("2")).thenReturn(receiver);
        lenient().doNothing().when(userService).validateTransaction(any(), any());

        // 3. Mockamos o Autorizador NEGANDO a transação (fallback acionado) com lenient
        lenient().when(authorizationService.authorizeTransaction("1", new BigDecimal(10))).thenReturn(false);

        // 4. Mockamos EntityManager (flush e clear são chamados no TransactionService) com lenient
        lenient().doNothing().when(entityManager).flush();
        lenient().doNothing().when(entityManager).clear();

        TransactionDTO request = new TransactionDTO(new BigDecimal(10), "1", "2");

        // 5. AÇÃO E VERIFICAÇÃO DE ERRO
        Assertions.assertThrows(Exception.class, () -> {
            transactionService.createTransaction(request);
        }, "Deve lançar exceção quando autorizador nega");
        
        // 6. Garante que o sistema protegeu o banco e NÃO tentou adquirir lock
        // (pois a falha ocorre ANTES do findWalletByUserIdLockedNative)
        verify(walletRepository, never()).findWalletByUserIdLockedNative(anyString());
        verify(repository, times(0)).save(any());
    }

    @Test
    @DisplayName("Deve lançar Exception quando a validação de negócio falhar")
    void createTransactionCase3_ValidationFails() throws Exception {
        // 1. Criamos Sender e Receiver
        User sender = new User();
        sender.setId("1");
        sender.setFirstName("Matheus");
        sender.setUserType(UserType.COMMON);
        Wallet senderWallet = new Wallet("wallet1", new BigDecimal(100), sender);
        sender.setWallet(senderWallet);

        User receiver = new User();
        receiver.setId("2");
        receiver.setUserType(UserType.COMMON);
        Wallet receiverWallet = new Wallet("wallet2", new BigDecimal(100), receiver);
        receiver.setWallet(receiverWallet);

        // 2. Mockamos UserService com lenient para stubbings que podem não ser usados
        when(userService.findUserById("1")).thenReturn(sender);
        when(userService.findUserById("2")).thenReturn(receiver);

        // AQUI: Forçamos validação a falhar
        lenient().doThrow(new Exception("Transação não permitida (lojista não pode transferir)"))
            .when(userService).validateTransaction(any(), any());

        // 3. Mockamos EntityManager (flush e clear são chamados no TransactionService) com lenient
        lenient().doNothing().when(entityManager).flush();
        lenient().doNothing().when(entityManager).clear();

        TransactionDTO request = new TransactionDTO(new BigDecimal(10), "1", "2");

        // 4. AÇÃO E VERIFICAÇÃO
        Assertions.assertThrows(Exception.class, () -> {
            transactionService.createTransaction(request);
        });

        // Garante que autorizador não foi nem consultado (falha antes)
        verify(authorizationService, never()).authorizeTransaction(anyString(), any());
        verify(walletRepository, never()).findWalletByUserIdLockedNative(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar Exception quando saldo for insuficiente (race condition protegida)")
    void createTransactionCase4_InsufficientBalance() throws Exception {
        // 1. Sender com saldo baixo
        User sender = new User();
        sender.setId("1");
        Wallet senderWallet = new Wallet("wallet1", new BigDecimal(5), sender); // Saldo: 5
        sender.setWallet(senderWallet);

        User receiver = new User();
        receiver.setId("2");
        Wallet receiverWallet = new Wallet("wallet2", new BigDecimal(100), receiver);
        receiver.setWallet(receiverWallet);

        // 2. Mockamos tudo com lenient para stubbings que podem não ser usados
        when(userService.findUserById("1")).thenReturn(sender);
        when(userService.findUserById("2")).thenReturn(receiver);
        lenient().doNothing().when(userService).validateTransaction(any(), any());
        lenient().when(authorizationService.authorizeTransaction("1", new BigDecimal(10))).thenReturn(true);

        // Wallet retorna com saldo já reduzido (simulando que outro thread mexeu) com lenient
        lenient().when(walletRepository.findWalletByUserIdLockedNative("1")).thenReturn(Optional.of(senderWallet));
        lenient().when(walletRepository.findWalletByUserIdLockedNative("2")).thenReturn(Optional.of(receiverWallet));

        // 3. Mockamos EntityManager (flush e clear são chamados no TransactionService) com lenient
        lenient().doNothing().when(entityManager).flush();
        lenient().doNothing().when(entityManager).clear();

        TransactionDTO request = new TransactionDTO(new BigDecimal(10), "1", "2");

        // 4. Verifica que saldo insuficiente é detectado mesmo com lock
        Assertions.assertThrows(Exception.class, () -> {
            transactionService.createTransaction(request);
        }, "Deve detectar saldo insuficiente e proteger contra race condition");

        verify(repository, never()).save(any());
    }
}
