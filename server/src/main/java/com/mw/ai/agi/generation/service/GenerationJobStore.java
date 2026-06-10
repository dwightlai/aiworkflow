package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationJob;

import java.util.List;
import java.util.Optional;

public interface GenerationJobStore {
    GenerationJob save(GenerationJob job);

    Optional<GenerationJob> findById(String id);

    List<GenerationJob> list(String tenantId);
}
