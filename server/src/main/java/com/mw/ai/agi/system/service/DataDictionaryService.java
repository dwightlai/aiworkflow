package com.mw.ai.agi.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.auth.service.TenantAdminGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.system.persistence.DataDictionaryEntity;
import com.mw.ai.agi.system.persistence.DataDictionaryItemEntity;
import com.mw.ai.agi.system.persistence.DataDictionaryItemMapper;
import com.mw.ai.agi.system.persistence.DataDictionaryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DataDictionaryService {
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private static final Pattern STATUS_PATTERN = Pattern.compile("^(ENABLED|DISABLED)$");

    private final DataDictionaryMapper dictionaryMapper;
    private final DataDictionaryItemMapper itemMapper;
    private final TenantAdminGuard tenantAdminGuard;
    private final SystemAuditService systemAuditService;

    public DataDictionaryService(
            DataDictionaryMapper dictionaryMapper,
            DataDictionaryItemMapper itemMapper,
            TenantAdminGuard tenantAdminGuard,
            SystemAuditService systemAuditService
    ) {
        this.dictionaryMapper = dictionaryMapper;
        this.itemMapper = itemMapper;
        this.tenantAdminGuard = tenantAdminGuard;
        this.systemAuditService = systemAuditService;
    }

    public List<DataDictionaryEntity> listDictionaries() {
        return dictionaryMapper.selectList(new LambdaQueryWrapper<DataDictionaryEntity>()
                .eq(DataDictionaryEntity::getTenantId, tenantId())
                .orderByAsc(DataDictionaryEntity::getCode));
    }

    public DataDictionaryEntity getDictionary(String id) {
        return requireDictionary(id);
    }

    public List<DataDictionaryItemEntity> listItems(String dictionaryId) {
        requireDictionary(dictionaryId);
        return itemMapper.selectList(new LambdaQueryWrapper<DataDictionaryItemEntity>()
                .eq(DataDictionaryItemEntity::getDictionaryId, dictionaryId)
                .orderByAsc(DataDictionaryItemEntity::getSortOrder)
                .orderByAsc(DataDictionaryItemEntity::getLabel));
    }

    public List<DataDictionaryItemEntity> listItemsByCode(String code) {
        DataDictionaryEntity dictionary = dictionaryMapper.selectOne(new LambdaQueryWrapper<DataDictionaryEntity>()
                .eq(DataDictionaryEntity::getTenantId, tenantId())
                .eq(DataDictionaryEntity::getCode, code.trim())
                .eq(DataDictionaryEntity::getStatus, "ENABLED"));
        if (dictionary == null) {
            return List.of();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<DataDictionaryItemEntity>()
                .eq(DataDictionaryItemEntity::getDictionaryId, dictionary.getId())
                .eq(DataDictionaryItemEntity::getStatus, "ENABLED")
                .orderByAsc(DataDictionaryItemEntity::getSortOrder));
    }

    public DataDictionaryEntity createDictionary(String code, String name, String description, String status) {
        tenantAdminGuard.assertSystemConfigurator();
        validateDictionaryInput(code, name, status);
        ensureCodeUnique(code, null);
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        DataDictionaryEntity entity = new DataDictionaryEntity();
        entity.setId("dict_" + UUID.randomUUID().toString().replace("-", ""));
        entity.setTenantId(tenantId());
        entity.setCode(code.trim());
        entity.setName(name.trim());
        entity.setDescription(description);
        entity.setStatus(normalizeStatus(status));
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        dictionaryMapper.insert(entity);
        systemAuditService.recordSuccess("SYSTEM_DICT_CREATED", entity.getId());
        return entity;
    }

    public DataDictionaryEntity updateDictionary(String id, String name, String description, String status) {
        tenantAdminGuard.assertSystemConfigurator();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dictionary name is required.");
        }
        DataDictionaryEntity current = requireDictionary(id);
        current.setName(name.trim());
        current.setDescription(description);
        current.setStatus(normalizeStatus(status));
        current.setUpdatedBy(OperatorContext.currentUserId());
        current.setUpdatedAt(Instant.now());
        dictionaryMapper.updateById(current);
        systemAuditService.recordSuccess("SYSTEM_DICT_UPDATED", current.getId());
        return current;
    }

    @Transactional
    public void deleteDictionary(String id) {
        tenantAdminGuard.assertSystemConfigurator();
        DataDictionaryEntity dictionary = requireDictionary(id);
        itemMapper.delete(new LambdaQueryWrapper<DataDictionaryItemEntity>()
                .eq(DataDictionaryItemEntity::getDictionaryId, dictionary.getId()));
        dictionaryMapper.deleteById(dictionary.getId());
        systemAuditService.recordSuccess("SYSTEM_DICT_DELETED", dictionary.getId());
    }

    public DataDictionaryItemEntity createItem(
            String dictionaryId,
            String label,
            String value,
            String description,
            Integer sortOrder,
            String status
    ) {
        tenantAdminGuard.assertSystemConfigurator();
        requireDictionary(dictionaryId);
        validateItemInput(label, value, status);
        ensureItemValueUnique(dictionaryId, value, null);
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        DataDictionaryItemEntity entity = new DataDictionaryItemEntity();
        entity.setId("dict_item_" + UUID.randomUUID().toString().replace("-", ""));
        entity.setDictionaryId(dictionaryId);
        entity.setLabel(label.trim());
        entity.setItemValue(value.trim());
        entity.setDescription(description);
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setStatus(normalizeStatus(status));
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        itemMapper.insert(entity);
        systemAuditService.recordSuccess("SYSTEM_DICT_ITEM_CREATED", entity.getId());
        return entity;
    }

    public DataDictionaryItemEntity updateItem(
            String itemId,
            String label,
            String value,
            String description,
            Integer sortOrder,
            String status
    ) {
        tenantAdminGuard.assertSystemConfigurator();
        validateItemInput(label, value, status);
        DataDictionaryItemEntity current = requireItem(itemId);
        ensureItemValueUnique(current.getDictionaryId(), value, itemId);
        current.setLabel(label.trim());
        current.setItemValue(value.trim());
        current.setDescription(description);
        current.setSortOrder(sortOrder == null ? 0 : sortOrder);
        current.setStatus(normalizeStatus(status));
        current.setUpdatedBy(OperatorContext.currentUserId());
        current.setUpdatedAt(Instant.now());
        itemMapper.updateById(current);
        systemAuditService.recordSuccess("SYSTEM_DICT_ITEM_UPDATED", current.getId());
        return current;
    }

    public void deleteItem(String itemId) {
        tenantAdminGuard.assertSystemConfigurator();
        DataDictionaryItemEntity item = requireItem(itemId);
        itemMapper.deleteById(item.getId());
        systemAuditService.recordSuccess("SYSTEM_DICT_ITEM_DELETED", item.getId());
    }

    private DataDictionaryEntity requireDictionary(String id) {
        DataDictionaryEntity entity = dictionaryMapper.selectById(id);
        if (entity == null || !tenantId().equals(entity.getTenantId())) {
            throw new IllegalArgumentException("Dictionary not found.");
        }
        return entity;
    }

    private DataDictionaryItemEntity requireItem(String itemId) {
        DataDictionaryItemEntity entity = itemMapper.selectById(itemId);
        if (entity == null) {
            throw new IllegalArgumentException("Dictionary item not found.");
        }
        requireDictionary(entity.getDictionaryId());
        return entity;
    }

    private void ensureCodeUnique(String code, String excludeId) {
        LambdaQueryWrapper<DataDictionaryEntity> wrapper = new LambdaQueryWrapper<DataDictionaryEntity>()
                .eq(DataDictionaryEntity::getTenantId, tenantId())
                .eq(DataDictionaryEntity::getCode, code.trim());
        if (excludeId != null) {
            wrapper.ne(DataDictionaryEntity::getId, excludeId);
        }
        if (dictionaryMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("Dictionary code already exists.");
        }
    }

    private void ensureItemValueUnique(String dictionaryId, String value, String excludeItemId) {
        LambdaQueryWrapper<DataDictionaryItemEntity> wrapper = new LambdaQueryWrapper<DataDictionaryItemEntity>()
                .eq(DataDictionaryItemEntity::getDictionaryId, dictionaryId)
                .eq(DataDictionaryItemEntity::getItemValue, value.trim());
        if (excludeItemId != null) {
            wrapper.ne(DataDictionaryItemEntity::getId, excludeItemId);
        }
        if (itemMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("Dictionary item value already exists.");
        }
    }

    private void validateDictionaryInput(String code, String name, String status) {
        if (code == null || code.isBlank() || !CODE_PATTERN.matcher(code.trim()).matches()) {
            throw new IllegalArgumentException("Dictionary code must use lowercase letters, numbers, or underscores.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dictionary name is required.");
        }
        normalizeStatus(status);
    }

    private void validateItemInput(String label, String value, String status) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Dictionary item label is required.");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Dictionary item value is required.");
        }
        normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank() ? "ENABLED" : status.trim();
        if (!STATUS_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Status must be ENABLED or DISABLED.");
        }
        return normalized;
    }

    private String tenantId() {
        return TenantContext.requireTenantId();
    }
}
