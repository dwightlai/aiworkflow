package com.mw.ai.agi.config;

public record AgiStorageSettings(
        String knowledgeDocumentDir,
        boolean knowledgeDocumentSaveOriginal,
        String researchOutputDir,
        String researchDocxMasterDir,
        boolean customized
) {
}
