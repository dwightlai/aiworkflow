package com.mw.ai.agi.prompt.service;

import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.prompt.domain.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PromptTemplateService {
    private final PromptTemplateStore store;
    private final TenantBusinessGuard tenantGuard;

    public PromptTemplateService() {
        this(new InMemoryPromptTemplateStore(), null);
    }

    @Autowired
    public PromptTemplateService(PromptTemplateStore store, TenantBusinessGuard tenantGuard) {
        this.store = store;
        this.tenantGuard = tenantGuard;
    }

    public PromptTemplate create(String name, String template, String description) {
        Instant now = Instant.now();
        PromptTemplate promptTemplate = new PromptTemplate(
                "prompt_" + UUID.randomUUID(),
                currentTenantId(),
                name,
                template,
                description,
                now,
                now
        );
        return store.save(promptTemplate);
    }

    public PromptTemplate update(String id, String name, String template, String description) {
        PromptTemplate current = get(id);
        return store.save(new PromptTemplate(
                current.id(),
                current.tenantId(),
                name,
                template,
                description,
                current.createdAt(),
                Instant.now()
        ));
    }

    public List<PromptTemplate> list() {
        return store.list(currentTenantId());
    }

    public void delete(String id) {
        get(id);
        store.delete(id);
    }

    private PromptTemplate get(String id) {
        PromptTemplate template = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt template not found: " + id));
        assertTenantAccessible(template.tenantId());
        return template;
    }

    private String currentTenantId() {
        return tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
    }

    private void assertTenantAccessible(String resourceTenantId) {
        if (tenantGuard != null) {
            tenantGuard.assertAccessible(resourceTenantId);
        }
    }
}
