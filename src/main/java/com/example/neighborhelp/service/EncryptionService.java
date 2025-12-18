package com.example.neighborhelp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private final SecretKey secretKey;

    // Constructor que recibe las claves desde application.properties
    public EncryptionService(
            @Value("${APP_ENCRYPTION_SECRET}") String secret,
            @Value("${APP_ENCRYPTION_SALT}") String salt) {

        this.secretKey = deriveKey(secret, salt);
    }

    private String maskString(String value) {
        if (value == null || value.length() <= 4) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }

    private SecretKey deriveKey(String secret, String salt) {
        try {
            byte[] saltBytes = salt.getBytes("UTF-8");

            // Usar PBKDF2 para derivar una clave segura
            PBEKeySpec spec = new PBEKeySpec(
                    secret.toCharArray(),
                    saltBytes,
                    65536,    // Iteraciones
                    256       // Longitud de clave en bits
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();

            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Error creando clave de encriptación", e);
        }
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encriptando texto", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (javax.crypto.BadPaddingException e) {
            throw new IllegalArgumentException("Error: Datos incorrectamente cifrados, clave inválida o datos corruptos.", e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error al decodificar los datos cifrados (Base64 inválido).", e);
        } catch (Exception e) {
            throw new RuntimeException("Error inesperado al desencriptar el texto", e);
        }
    }


    /**
     * Método para verificar que la encriptación funciona
     */
    public String testEncryption(String testText) {
        try {
            String encrypted = encrypt(testText);
            String decrypted = decrypt(encrypted);

            return String.format(
                    "Texto original: %s%nEncriptado: %s%nDesencriptado: %s%n¿Coinciden? %s",
                    testText,
                    encrypted,
                    decrypted,
                    testText.equals(decrypted) ? "SÍ ✓" : "NO ✗"
            );
        } catch (Exception e) {
            return "Error en test: " + e.getMessage();
        }
    }
}