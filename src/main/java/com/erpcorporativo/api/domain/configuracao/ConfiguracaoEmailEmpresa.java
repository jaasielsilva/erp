package com.erpcorporativo.api.domain.configuracao;

import com.erpcorporativo.api.domain.empresa.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "configuracao_email_empresa")
@Getter
@Setter
public class ConfiguracaoEmailEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @Column(nullable = false)
    private boolean ativo = false;

    @Column(nullable = false, length = 40)
    private String provedor = "SMTP_GMAIL";

    @Column(name = "endereco_remetente", length = 160)
    private String enderecoRemetente;

    @Column(name = "smtp_host", length = 160)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth = true;

    @Column(name = "smtp_starttls", nullable = false)
    private boolean smtpStarttls = true;

    @Column(name = "smtp_username", length = 160)
    private String smtpUsername;

    @Column(name = "smtp_password_ciphertext", columnDefinition = "TEXT")
    private String smtpPasswordCiphertext;

    @Column(name = "oauth_client_id", columnDefinition = "TEXT")
    private String oauthClientId;

    @Column(name = "oauth_client_secret_ciphertext", columnDefinition = "TEXT")
    private String oauthClientSecretCiphertext;

    @Column(name = "oauth_refresh_token_ciphertext", columnDefinition = "TEXT")
    private String oauthRefreshTokenCiphertext;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
