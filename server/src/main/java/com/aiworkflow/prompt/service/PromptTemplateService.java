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

    public List<PromptTemplate> list() {
        return new ArrayList<>(templates);
    }
}
