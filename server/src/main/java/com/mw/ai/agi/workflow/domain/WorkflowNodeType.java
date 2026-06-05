package com.mw.ai.agi.workflow.domain;

public enum WorkflowNodeType {
    START,
    END,
    LLM,
    PROMPT,
    KNOWLEDGE_RETRIEVAL,
    HTTP_TOOL,
    CONDITION,
    TEXT_TRANSFORM,
    CONTENT_TEMPLATE,
    LOOP
}
