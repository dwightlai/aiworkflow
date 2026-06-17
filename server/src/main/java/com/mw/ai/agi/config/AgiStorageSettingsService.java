package com.mw.ai.agi.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.auth.service.TenantAdminGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.system.persistence.SystemSettingEntity;
import com.mw.ai.agi.system.persistence.SystemSettingMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AgiStorageSettingsService {
    private static final String SETTING_KEY = "storage";

    private final AgiStorageProperties defaults;
    private final ObjectProvider<SystemSettingMapper> settingMapperProvider;
    private final TenantAdminGuard tenantAdminGuard;
    private final ObjectMapper objectMapper;

    public AgiStorageSettingsService(
            AgiStorageProperties defaults,
            ObjectProvider<SystemSettingMapper> settingMapperProvider,
            TenantAdminGuard tenantAdminGuard,
            ObjectMapper objectMapper
    ) {
        this.defaults = defaults;
        this.settingMapperProvider = settingMapperProvider;
        this.tenantAdminGuard = tenantAdminGuard;
        this.objectMapper = objectMapper;
    }

    public static AgiStorageSettingsService withDefaults(AgiStorageProperties defaults, ObjectMapper objectMapper) {
        ObjectProvider<SystemSettingMapper> provider = new ObjectProvider<>() {
            @Override
            public SystemSettingMapper getObject() {
                return null;
            }

            @Override
            public SystemSettingMapper getObject(Object... args) {
                return null;
            }

            @Override
            public SystemSettingMapper getIfAvailable() {
                return null;
            }

            @Override
            public SystemSettingMapper getIfUnique() {
                return null;
            }
        };
        return new AgiStorageSettingsService(defaults, provider, null, objectMapper);
    }

    public AgiStorageSettings getEffective() {
        Map<String, Object> override = loadOverride();
        if (override.isEmpty()) {
            return fromDefaults(false);
        }
        return new AgiStorageSettings(
                stringValue(override.get("knowledgeDocumentDir"), defaults.getKnowledgeDocumentDir()),
                boolValue(override.get("knowledgeDocumentSaveOriginal"), defaults.isKnowledgeDocumentSaveOriginal()),
                stringValue(override.get("researchOutputDir"), defaults.getResearchOutputDir()),
                stringValue(override.get("researchDocxMasterDir"), defaults.getResearchDocxMasterDir()),
                true
        );
    }

    public AgiStorageSettings getForAdmin() {
        return getEffective();
    }

    public AgiStorageSettings save(
            String knowledgeDocumentDir,
            boolean knowledgeDocumentSaveOriginal,
            String researchOutputDir,
            String researchDocxMasterDir
    ) {
        assertConfigurator();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("knowledgeDocumentDir", requirePath(knowledgeDocumentDir, "知识库文档路径"));
        value.put("knowledgeDocumentSaveOriginal", knowledgeDocumentSaveOriginal);
        value.put("researchOutputDir", requirePath(researchOutputDir, "编研成果路径"));
        value.put("researchDocxMasterDir", requirePath(researchDocxMasterDir, "编研母版路径"));
        persist(value);
        return getEffective();
    }

    public AgiStorageSettings resetToDefaults() {
        assertConfigurator();
        SystemSettingMapper mapper = settingMapperProvider.getIfAvailable();
        if (mapper != null) {
            mapper.delete(new LambdaQueryWrapper<SystemSettingEntity>()
                    .eq(SystemSettingEntity::getTenantId, tenantId())
                    .eq(SystemSettingEntity::getSettingKey, SETTING_KEY));
        }
        return fromDefaults(false);
    }

    private void persist(Map<String, Object> value) {
        SystemSettingMapper mapper = settingMapperProvider.getIfAvailable();
        if (mapper == null) {
            throw new IllegalStateException("系统设置存储不可用。");
        }
        Instant now = Instant.now();
        String tenantId = tenantId();
        SystemSettingEntity existing = mapper.selectOne(new LambdaQueryWrapper<SystemSettingEntity>()
                .eq(SystemSettingEntity::getTenantId, tenantId)
                .eq(SystemSettingEntity::getSettingKey, SETTING_KEY));
        try {
            String json = objectMapper.writeValueAsString(value);
            if (existing == null) {
                SystemSettingEntity entity = new SystemSettingEntity();
                entity.setId("sys_setting_" + UUID.randomUUID());
                entity.setTenantId(tenantId);
                entity.setSettingKey(SETTING_KEY);
                entity.setSettingValue(json);
                entity.setUpdatedBy(OperatorContext.currentUserId());
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                mapper.insert(entity);
                return;
            }
            existing.setSettingValue(json);
            existing.setUpdatedBy(OperatorContext.currentUserId());
            existing.setUpdatedAt(now);
            mapper.updateById(existing);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to save storage settings.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadOverride() {
        SystemSettingMapper mapper = settingMapperProvider.getIfAvailable();
        if (mapper == null) {
            return Map.of();
        }
        SystemSettingEntity entity = mapper.selectOne(new LambdaQueryWrapper<SystemSettingEntity>()
                .eq(SystemSettingEntity::getTenantId, tenantId())
                .eq(SystemSettingEntity::getSettingKey, SETTING_KEY));
        if (entity == null || entity.getSettingValue() == null || entity.getSettingValue().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(entity.getSettingValue(), Map.class);
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private AgiStorageSettings fromDefaults(boolean customized) {
        return new AgiStorageSettings(
                defaults.getKnowledgeDocumentDir(),
                defaults.isKnowledgeDocumentSaveOriginal(),
                defaults.getResearchOutputDir(),
                defaults.getResearchDocxMasterDir(),
                customized
        );
    }

    private String requirePath(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    private boolean boolValue(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private void assertConfigurator() {
        if (tenantAdminGuard != null) {
            tenantAdminGuard.assertSystemConfigurator();
        }
    }

    private String tenantId() {
        return TenantContext.requireTenantId();
    }
}
