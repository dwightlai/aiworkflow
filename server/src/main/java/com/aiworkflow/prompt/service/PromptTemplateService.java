package com.aiworkflow.prompt.service;

import com.aiworkflow.prompt.domain.PromptTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PromptTemplateService {
    private final List<PromptTemplate> templates = new CopyOnWriteArrayList<>();

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
        templates.add(promptTemplate);
        return promptTemplate;
    }

    public PromptTemplate update(String id, String name, String template, String description) {
        for (int index = 0; index < templates.size(); index += 1) {
            PromptTemplate current = templates.get(index);
            if (current.id().equals(id)) {
                PromptTemplate updated = new PromptTemplate(
                        current.id(),
                        name,
                        template,
                        description,
                        current.createdAt(),
                        Instant.now()
                );
                templates.set(index, updated);
                return updated;
            }
        }
        throw new IllegalArgumentException("Prompt template not found: " + id);
    }

    public List<PromptTemplate> list() {
        return new ArrayList<>(templates);
    }

    public void delete(String id) {
        templates.removeIf(template -> template.id().equals(id));
    }
}
