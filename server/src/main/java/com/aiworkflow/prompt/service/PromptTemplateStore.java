package com.aiworkflow.prompt.service;

import com.aiworkflow.prompt.domain.PromptTemplate;

import java.util.List;
import java.util.Optional;

public interface PromptTemplateStore {
    PromptTemplate save(PromptTemplate template);

    Optional<PromptTemplate> findById(String id);

    List<PromptTemplate> list();

    void delete(String id);
}
