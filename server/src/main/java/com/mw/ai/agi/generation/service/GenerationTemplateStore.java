package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationTemplate;

import java.util.List;
import java.util.Optional;

public interface GenerationTemplateStore {
    GenerationTemplate save(GenerationTemplate template);

    Optional<GenerationTemplate> findById(String id);

    List<GenerationTemplate> list(String tenantId);

    void delete(String id);
}
