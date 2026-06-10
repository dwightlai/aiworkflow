package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryGenerationOutputStore implements GenerationOutputStore {
    private final List<GenerationOutput> outputs = new CopyOnWriteArrayList<>();

    @Override
    public GenerationOutput save(GenerationOutput output) {
        outputs.removeIf(current -> current.id().equals(output.id()));
        outputs.add(output);
        return output;
    }

    @Override
    public Optional<GenerationOutput> findById(String id) {
        return outputs.stream().filter(output -> output.id().equals(id)).findFirst();
    }

    @Override
    public List<GenerationOutput> listByJobId(String jobId) {
        return outputs.stream().filter(output -> output.jobId().equals(jobId)).toList();
    }
}
