package com.wallet.wallet.services;

import com.wallet.wallet.domain.User;
import com.wallet.wallet.domain.UserType;
import com.wallet.wallet.domain.Wallet;
import com.wallet.wallet.dtos.TransactionDTO;
import com.wallet.wallet.repositories.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock // Mock do UserService
    private UserService userService;

    @Mock // Mock do Repository
    private TransactionRepository repository;

    @Mock // Mock do RestTemplate (Importante para evitar NullPointerException na API externa)
    private RestTemplate restTemplate;

    @InjectMocks // Injeta os Mocks acima dentro do Service real
    private TransactionService transactionService;

    @BeforeEach
    void setup(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve criar transação com sucesso quando tudo estiver OK")
    void createTransactionCase1() throws Exception {
        // 1. Criamos o Sender (Remetente) com setters
        User sender = new User();
        sender.setId("1");
        sender.setFirstName("Matheus");
        sender.setLastName("Silva");
        sender.setDocument("99999999901");
        sender.setEmail("matheus@email.com");
        sender.setUserType(UserType.COMMON);
        sender.setWallet(new Wallet("wallet1", new BigDecimal(100), sender));

        // 2. O Receiver (Recebedor) com setters
        User receiver = new User();
        receiver.setId("2");
        receiver.setFirstName("Joao");
        receiver.setLastName("Santos");
        receiver.setDocument("99999999902");
        receiver.setEmail("joao@email.com");
        receiver.setUserType(UserType.COMMON);
        receiver.setWallet(new Wallet("wallet2", new BigDecimal(100), receiver));

        // 3. Ensinei o Mockito
        when(userService.findUserById("1")).thenReturn(sender);
        when(userService.findUserById("2")).thenReturn(receiver);

        // Como mockamos a validação, ele não vai reclamar de nada
        doNothing().when(userService).validateTransaction(any(), any());

        TransactionDTO request = new TransactionDTO(new BigDecimal(10), "1", "2");
        
        // 4. AÇÃO
        transactionService.createTransaction(request);

        // 5. VERIFICAÇÃO
        // Verifica se o método save foi chamado 1 vez
        verify(repository, times(1)).save(any());
        
        // Verifica se o saldo foi atualizado (Sender tinha 100, agora deve ter 90 no objeto da memória)
        Assertions.assertEquals(new BigDecimal(90), sender.getWallet().getBalance());
        
        verify(userService, times(1)).saveUser(sender);
    }

    @Test
    @DisplayName("Deve lançar Exception quando a validação falhar")
    void createTransactionCase2() throws Exception {
        // 1. Criamos Sender com setters (igual ao Case 1)
        User sender = new User();
        sender.setId("1");
        sender.setFirstName("Matheus");
        sender.setLastName("Silva");
        sender.setDocument("99999999901");
        sender.setEmail("matheus@email.com");
        sender.setUserType(UserType.COMMON);
        sender.setWallet(new Wallet("wallet1", new BigDecimal(100), sender));

        // 2. Criamos Receiver com setters
        User receiver = new User();
        receiver.setId("2");
        receiver.setFirstName("Joao");
        receiver.setLastName("Santos");
        receiver.setDocument("99999999902");
        receiver.setEmail("joao@email.com");
        receiver.setUserType(UserType.COMMON);
        receiver.setWallet(new Wallet("wallet2", new BigDecimal(100), receiver));

        // 3. Ensinamos o Mockito
        when(userService.findUserById("1")).thenReturn(sender);
        when(userService.findUserById("2")).thenReturn(receiver);

        // AQUI ESTÁ O TRUQUE: Forçamos o Mock a lançar erro quando validarem a transação
        doThrow(new Exception("Transação não autorizada")).when(userService).validateTransaction(any(), any());

        TransactionDTO request = new TransactionDTO(new BigDecimal(10), "1", "2");

        // 4. AÇÃO E VERIFICAÇÃO DE ERRO
        Assertions.assertThrows(Exception.class, () -> {
            transactionService.createTransaction(request);
        });
        
        // Garante que o sistema protegeu o banco de dados e NÃO salvou nada
        verify(repository, times(0)).save(any());
    }
}