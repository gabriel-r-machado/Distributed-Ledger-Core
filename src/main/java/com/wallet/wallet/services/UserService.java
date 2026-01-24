package com.wallet.wallet.services;

import com.wallet.wallet.domain.User;
import com.wallet.wallet.domain.UserType;
import com.wallet.wallet.domain.Wallet;
import com.wallet.wallet.dtos.UserDTO;
import com.wallet.wallet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    public void validateTransaction(User sender, BigDecimal amount) throws Exception {
        if(sender.getUserType() == UserType.MERCHANT){
            throw new Exception("Usuário do tipo Lojista não está autorizado a realizar transação");
        }

        // CORREÇÃO 1: Buscamos o saldo dentro da carteira (getWallet().getBalance())
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
        newUser.setPassword(data.password());
        newUser.setUserType(data.userType());

        // CORREÇÃO 2: Criamos a carteira, colocamos o saldo e vinculamos ao usuário
        Wallet newWallet = new Wallet();
        newWallet.setBalance(data.balance()); // Pega o saldo do DTO
        newWallet.setUser(newUser); // Diz que essa carteira é desse usuário
        
        newUser.setWallet(newWallet); // Diz que esse usuário tem essa carteira

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