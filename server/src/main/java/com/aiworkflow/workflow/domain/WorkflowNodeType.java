package com.aiworkflow.workflow.domain;

public enum WorkflowNodeType {
    START,
    END,
    LLM,
    PROMPT,
    KNOWLEDGE_RETRIEVAL,
    HTTP_TOOL,
    CONDITION,
    TEXT_TRANSFORM
}
