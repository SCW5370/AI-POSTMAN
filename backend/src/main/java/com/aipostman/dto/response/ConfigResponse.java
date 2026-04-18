package com.aipostman.dto.response;

public class ConfigResponse {
    // 大模型配置
    private String llmApiKey;
    private String llmApiBase;
    private String llmModel;
    
    // SMTP配置
    private String smtpUsername;
    private String smtpPassword;

    // Getters and Setters
    public String getLlmApiKey() {
        return llmApiKey;
    }

    public void setLlmApiKey(String llmApiKey) {
        this.llmApiKey = llmApiKey;
    }

    public String getLlmApiBase() {
        return llmApiBase;
    }

    public void setLlmApiBase(String llmApiBase) {
        this.llmApiBase = llmApiBase;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }
}
