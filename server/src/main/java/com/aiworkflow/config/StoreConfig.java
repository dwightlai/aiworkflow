package com.aiworkflow.config;

import com.aiworkflow.workflow.engine.InMemoryWorkflowExecutionStore;
import com.aiworkflow.workflow.engine.JdbcWorkflowExecutionStore;
import com.aiworkflow.workflow.engine.WorkflowExecutionStore;
import com.aiworkflow.workflow.service.InMemoryWorkflowStore;
import com.aiworkflow.workflow.service.JdbcWorkflowStore;
import com.aiworkflow.workflow.service.WorkflowStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class StoreConfig {
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public WorkflowStore jdbcWorkflowStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcWorkflowStore(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowStore.class)
    public WorkflowStore inMemoryWorkflowStore() {
        return new InMemoryWorkflowStore();
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    public WorkflowExecutionStore jdbcWorkflowExecutionStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new JdbcWorkflowExecutionStore(jdbcTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowExecutionStore.class)
    public WorkflowExecutionStore inMemoryWorkflowExecutionStore() {
        return new InMemoryWorkflowExecutionStore();
    }
}
