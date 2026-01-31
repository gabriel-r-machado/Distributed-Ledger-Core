package com.wallet.wallet.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Wallet API.
 * 
 * MVP setup with disabled authentication.
 * Production requires JWT or OAuth2 implementation.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // LIBERA TUDO DO SWAGGER (Caminho antigo e novo)
                .requestMatchers(
                    "/v3/api-docs/**", 
                    "/api-docs/**", 
                    "/swagger-ui/**", 
                    "/swagger-ui.html"
                ).permitAll()
                
                // LIBERA O HEALTH CHECK DA AWS
                .requestMatchers("/actuator/**").permitAll()
                
                // LIBERA SEUS ENDPOINTS (MVP - TODO: Implementar JWT)
                .requestMatchers("/users/**", "/transactions/**").permitAll()
                
                // O RESTO PRECISA DE LOGIN
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}