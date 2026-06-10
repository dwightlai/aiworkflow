package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationOutput;

import java.util.List;
import java.util.Optional;

public interface GenerationOutputStore {
    GenerationOutput save(GenerationOutput output);

    Optional<GenerationOutput> findById(String id);

    List<GenerationOutput> listByJobId(String jobId);
}
