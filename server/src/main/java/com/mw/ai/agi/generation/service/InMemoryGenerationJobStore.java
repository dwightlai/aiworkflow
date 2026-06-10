package com.mw.ai.agi.generation.service;

import com.mw.ai.agi.generation.domain.GenerationJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryGenerationJobStore implements GenerationJobStore {
    private final List<GenerationJob> jobs = new CopyOnWriteArrayList<>();

    @Override
    public GenerationJob save(GenerationJob job) {
        jobs.removeIf(current -> current.id().equals(job.id()));
        jobs.add(job);
        return job;
    }

    @Override
    public Optional<GenerationJob> findById(String id) {
        return jobs.stream().filter(job -> job.id().equals(id)).findFirst();
    }

    @Override
    public List<GenerationJob> list(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return new ArrayList<>(jobs);
        }
        return jobs.stream().filter(job -> tenantId.equals(job.tenantId())).toList();
    }
}
