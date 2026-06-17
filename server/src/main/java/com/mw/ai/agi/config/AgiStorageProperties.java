package com.mw.ai.agi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agi.storage")
public class AgiStorageProperties {
    private String knowledgeDocumentDir = "./data/knowledge-documents";
    private boolean knowledgeDocumentSaveOriginal = true;
    private String researchOutputDir = "./data/generation-outputs";
    private String researchDocxMasterDir = "./data/generation-docx-masters";

    public String getKnowledgeDocumentDir() {
        return knowledgeDocumentDir;
    }

    public void setKnowledgeDocumentDir(String knowledgeDocumentDir) {
        this.knowledgeDocumentDir = knowledgeDocumentDir;
    }

    public boolean isKnowledgeDocumentSaveOriginal() {
        return knowledgeDocumentSaveOriginal;
    }

    public void setKnowledgeDocumentSaveOriginal(boolean knowledgeDocumentSaveOriginal) {
        this.knowledgeDocumentSaveOriginal = knowledgeDocumentSaveOriginal;
    }

    public String getResearchOutputDir() {
        return researchOutputDir;
    }

    public void setResearchOutputDir(String researchOutputDir) {
        this.researchOutputDir = researchOutputDir;
    }

    public String getResearchDocxMasterDir() {
        return researchDocxMasterDir;
    }

    public void setResearchDocxMasterDir(String researchDocxMasterDir) {
        this.researchDocxMasterDir = researchDocxMasterDir;
    }
}
