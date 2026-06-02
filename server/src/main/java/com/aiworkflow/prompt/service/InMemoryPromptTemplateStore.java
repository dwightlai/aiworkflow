package com.aiworkflow.prompt.service;

import com.aiworkflow.prompt.domain.PromptTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryPromptTemplateStore implements PromptTemplateStore {
    private final List<PromptTemplate> templates = new CopyOnWriteArrayList<>();

    @Override
    public PromptTemplate save(PromptTemplate template) {
        templates.removeIf(current -> current.id().equals(template.id()));
        templates.add(template);
        return template;
    }

    @Override
    public Optional<PromptTemplate> findById(String id) {
        return templates.stream()
                .filter(template -> template.id().equals(id))
                .findFirst();
    }

    @Override
    public List<PromptTemplate> list() {
        return new ArrayList<>(templates);
    }

    @Override
    public void delete(String id) {
        templates.removeIf(template -> template.id().equals(id));
    }
}
