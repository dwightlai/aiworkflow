package com.mw.ai.agi.knowledge.chunking;

public interface StructuredDocumentParser {
    boolean supports(String fileName);

    DocumentStructure parse(String fileName, String content);
}
