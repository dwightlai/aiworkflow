package com.aiworkflow.prompt.service;

import com.aiworkflow.prompt.domain.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PromptTemplateService {
    private final PromptTemplateStore store;

    public PromptTemplateService() {
        this(new InMemoryPromptTemplateStore());
    }

    @Autowired
    public PromptTemplateService(PromptTemplateStore store) {
        this.store = store;
    }

    public PromptTemplate create(String name, String template, String description) {
        Instant now = Instant.now();
        PromptTemplate promptTemplate = new PromptTemplate(
                "prompt_" + UUID.randomUUID(),
                name,
                template,
                description,
                now,
                now
        );
        return store.save(promptTemplate);
    }

    public PromptTemplate update(String id, String name, String template, String description) {
        PromptTemplate current = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt template not found: " + id));
        return store.save(new PromptTemplate(
                current.id(),
                name,
                template,
                description,
                current.createdAt(),
                Instant.now()
        ));
    }

    public List<PromptTemplate> list() {
        return store.list();
    }

    public void delete(String id) {
        store.delete(id);
    }
}
