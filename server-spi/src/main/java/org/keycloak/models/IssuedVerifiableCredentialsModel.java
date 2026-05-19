package org.keycloak.models;

public class IssuedVerifiableCredentialsModel {

    private String id;
    private String userId;
    private String credentialType;
    private Long issuedAt;
    private Long expiresAt;
    private String walletId;
    private String revision;

    public IssuedVerifiableCredentialsModel() {
    }

    public IssuedVerifiableCredentialsModel(String userId, String credentialType, String walletId) {
        this.userId = userId;
        this.credentialType = credentialType;
        this.walletId = walletId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
