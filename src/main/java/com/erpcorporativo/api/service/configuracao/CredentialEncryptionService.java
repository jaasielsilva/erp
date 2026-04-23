package com.erpcorporativo.api.service.configuracao;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cifra credenciais sensíveis (client secret, refresh token) com AES-256-GCM.
 * A chave mestra vem de variável de ambiente; nunca commitar chaves reais.
 */
@Service
public class CredentialEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String CIPHER = "AES/GCM/NoPadding";

    private final SecretKeySpec secretKey;

    public CredentialEncryptionService(
            @Value("${app.secrets.credential-encryption-key:DEFINA_APP_SECRETS_CREDENTIAL_ENCRYPTION_KEY_32+}") String raw) {
        byte[] keyBytes = sha256(raw.getBytes(StandardCharsets.UTF_8));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar dado sensivel.", e);
        }
    }

    public String decrypt(String cipherTextB64) {
        if (cipherTextB64 == null || cipherTextB64.isEmpty()) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(cipherTextB64);
            if (all.length < GCM_IV_LENGTH + 16) {
                throw new IllegalArgumentException("Texto cifrado invalido.");
            }
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar dado. Verifique a chave de cifra (APP_SECRETS_CREDENTIAL_ENCRYPTION_KEY).", e);
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
