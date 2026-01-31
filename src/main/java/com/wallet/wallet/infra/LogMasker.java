package com.wallet.wallet.infra;

import java.math.BigDecimal;

/**
 * Utilitário para mascarar dados sensíveis em logs.
 * Previne a exposição de informações financeiras e pessoais em logs.
 */
public class LogMasker {

    private LogMasker() {
        // Classe utilitária - não deve ser instanciada
    }

    /**
     * Mascara um valor monetário para logs.
     * Em produção, mostra apenas os primeiros e últimos dígitos.
     * 
     * @param value Valor a ser mascarado
     * @return String mascarada (ex: "R$ ***")
     */
    public static String maskBalance(BigDecimal value) {
        if (value == null) {
            return "null";
        }
        
        // Em ambiente de desenvolvimento, mostra valor completo
        if (isDevelopment()) {
            return "R$ " + value.toString();
        }
        
        // Em produção, mascara o valor
        return "R$ ***";
    }

    /**
     * Mascara um ID de usuário para logs.
     * 
     * @param userId ID do usuário
     * @return ID mascarado (ex: "usr_***")
     */
    public static String maskUserId(String userId) {
        if (userId == null || userId.length() <= 4) {
            return "***";
        }
        
        if (isDevelopment()) {
            return userId;
        }
        
        return userId.substring(0, 3) + "***" + userId.substring(userId.length() - 3);
    }

    /**
     * Mascara um documento (CPF/CNPJ) para logs.
     * 
     * @param document Documento a ser mascarado
     * @return Documento mascarado (ex: "***.***.***-**")
     */
    public static String maskDocument(String document) {
        if (document == null) {
            return "***";
        }
        
        if (isDevelopment()) {
            return document;
        }
        
        if (document.length() == 11) { // CPF
            return "***.***." + document.substring(6, 9) + "-**";
        } else if (document.length() == 14) { // CNPJ
            return "**.***." + document.substring(6, 9) + "/****-**";
        }
        
        return "***";
    }

    /**
     * Mascara um email para logs.
     * 
     * @param email Email a ser mascarado
     * @return Email mascarado (ex: "u***@d***.com")
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        
        if (isDevelopment()) {
            return email;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***";
        }
        
        String username = parts[0];
        String domain = parts[1];
        
        String maskedUsername = username.length() > 1 ? 
            username.charAt(0) + "***" : 
            "***";
            
        String[] domainParts = domain.split("\\.");
        if (domainParts.length >= 2) {
            String maskedDomain = domainParts[0].length() > 1 ?
                domainParts[0].charAt(0) + "***" :
                "***";
            return maskedUsername + "@" + maskedDomain + "." + domainParts[1];
        }
        
        return maskedUsername + "@***";
    }

    /**
     * Verifica se está em ambiente de desenvolvimento.
     * 
     * @return true se for ambiente de desenvolvimento
     */
    private static boolean isDevelopment() {
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        return env != null && (env.contains("dev") || env.contains("test"));
    }
}