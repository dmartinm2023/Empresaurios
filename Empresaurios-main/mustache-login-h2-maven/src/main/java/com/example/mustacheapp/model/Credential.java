package com.example.mustacheapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "credentials")
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String login;
    @Lob
    private String passwordEncrypted;

    // Owner (Spring Security username)
    private String ownerUsername;

    // Optional: on-chain metadata for aesthetics
    private String onChainCommitment;
    private String onChainTxHash;

    public Credential() {}

    public Credential(String title, String login, String passwordEncrypted, String ownerUsername) {
        this.title = title;
        this.login = login;
        this.passwordEncrypted = passwordEncrypted;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPasswordEncrypted() { return passwordEncrypted; }
    public void setPasswordEncrypted(String passwordEncrypted) { this.passwordEncrypted = passwordEncrypted; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getOnChainCommitment() { return onChainCommitment; }
    public void setOnChainCommitment(String onChainCommitment) { this.onChainCommitment = onChainCommitment; }

    public String getOnChainTxHash() { return onChainTxHash; }
    public void setOnChainTxHash(String onChainTxHash) { this.onChainTxHash = onChainTxHash; }
}
