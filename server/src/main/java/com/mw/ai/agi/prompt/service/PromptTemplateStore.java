package com.mw.ai.agi.prompt.service;

import com.mw.ai.agi.prompt.domain.PromptTemplate;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateStore {
    PromptTemplate save(PromptTemplate template);

    Optional<PromptTemplate> findById(String id);

    List<PromptTemplate> list(String tenantId);

    void delete(String id);
}
