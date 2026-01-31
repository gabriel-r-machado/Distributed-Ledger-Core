package com.wallet.wallet.services;

import com.wallet.wallet.domain.User;
import com.wallet.wallet.domain.UserType;
import com.wallet.wallet.domain.Wallet;
import com.wallet.wallet.dtos.UserDTO;
import com.wallet.wallet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void validateTransaction(User sender, BigDecimal amount) throws Exception {
        if (sender == null) {
            throw new Exception("Usuário remetente não pode ser nulo");
        }
        
        if (sender.getUserType() == UserType.MERCHANT){
            throw new Exception("Usuário do tipo Lojista não está autorizado a realizar transação");
        }

        if (sender.getWallet() == null) {
            throw new Exception("Carteira do usuário não encontrada");
        }

        if (sender.getWallet().getBalance() == null) {
            throw new Exception("Saldo da carteira não disponível");
        }

        if(sender.getWallet().getBalance().compareTo(amount) < 0){
            throw new Exception("Saldo insuficiente");
        }
    }

    public User findUserById(String id) throws Exception {
        return this.repository.findById(id).orElseThrow(() -> new Exception("Usuário não encontrado"));
    }

    public User createUser(UserDTO data) {
        User newUser = new User();
        newUser.setFirstName(data.firstName());
        newUser.setLastName(data.lastName());
        newUser.setDocument(data.document());
        newUser.setEmail(data.email());
        
        String hashedPassword = passwordEncoder.encode(data.password());
        newUser.setPassword(hashedPassword);
        
        newUser.setUserType(data.userType());

        Wallet newWallet = new Wallet();
        newWallet.setBalance(data.balance());
        newWallet.setUser(newUser);
        
        newUser.setWallet(newWallet);

        this.saveUser(newUser);
        return newUser;
    }

    public List<User> getAllUsers() {
        return this.repository.findAll();
    }

    public void saveUser(User user){
        this.repository.save(user);
    }
}