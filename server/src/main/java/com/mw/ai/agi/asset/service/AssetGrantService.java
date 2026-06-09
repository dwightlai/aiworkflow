package com.mw.ai.agi.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.asset.domain.AssetGrant;
import com.mw.ai.agi.asset.persistence.AssetGrantEntity;
import com.mw.ai.agi.asset.persistence.AssetGrantMapper;
import com.mw.ai.agi.bot.persistence.AiBotEntity;
import com.mw.ai.agi.bot.persistence.AiBotMapper;
import com.mw.ai.agi.knowledge.persistence.KnowledgeBaseEntity;
import com.mw.ai.agi.knowledge.persistence.KnowledgeBaseMapper;
import com.mw.ai.agi.auth.persistence.OrganizationEntity;
import com.mw.ai.agi.auth.persistence.OrganizationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AssetGrantService {
    public static final String KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    public static final String BOT = "BOT";
    public static final String USE = "USE";
    public static final String MANAGE = "MANAGE";
    public static final String SELF = "SELF";
    public static final String SUBTREE = "SUBTREE";

    private final AssetGrantMapper grantMapper;
    private final OrganizationMapper organizationMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final AiBotMapper aiBotMapper;

    public AssetGrantService(
            AssetGrantMapper grantMapper,
            OrganizationMapper organizationMapper,
            KnowledgeBaseMapper knowledgeBaseMapper,
            AiBotMapper aiBotMapper
    ) {
        this.grantMapper = grantMapper;
        this.organizationMapper = organizationMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.aiBotMapper = aiBotMapper;
    }

    public String ensureOwnerUnitId(String assetType, String assetId, String requestedOwnerUnitId, String operatorUnitId) {
        String stored = loadOwnerUnitId(assetType, assetId);
        String resolved = blankToNull(requestedOwnerUnitId);
        if (resolved == null) {
            resolved = blankToNull(stored);
        }
        if (resolved == null) {
            resolved = blankToNull(operatorUnitId);
        }
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset owner unit is required.");
        }
        if (stored == null || stored.isBlank()) {
            backfillOwnerUnitId(assetType, assetId, resolved);
        }
        return resolved;
    }

    public List<AssetGrant> list(String assetType, String assetId) {
        return grantMapper.selectList(new LambdaQueryWrapper<AssetGrantEntity>()
                        .eq(AssetGrantEntity::getAssetType, assetType)
                        .eq(AssetGrantEntity::getAssetId, assetId)
                        .eq(AssetGrantEntity::getPermission, USE)
                        .orderByAsc(AssetGrantEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public AssetGrant save(
            String assetType,
            String assetId,
            String permission,
            String unitId,
            String unitScope,
            String departmentId,
            String departmentScope,
            Boolean enabled,
            String createdBy
    ) {
        validateAssetType(assetType);
        LambdaQueryWrapper<AssetGrantEntity> duplicateQuery = new LambdaQueryWrapper<AssetGrantEntity>()
                .eq(AssetGrantEntity::getAssetType, assetType)
                .eq(AssetGrantEntity::getAssetId, assetId)
                .eq(AssetGrantEntity::getPermission, USE)
                .eq(AssetGrantEntity::getUnitScope, normalize(unitScope, SELF))
                .eq(AssetGrantEntity::getDepartmentScope, normalize(departmentScope, SELF));
        if (unitId == null || unitId.isBlank()) {
            duplicateQuery.isNull(AssetGrantEntity::getUnitId);
        } else {
            duplicateQuery.eq(AssetGrantEntity::getUnitId, unitId);
        }
        if (departmentId == null || departmentId.isBlank()) {
            duplicateQuery.isNull(AssetGrantEntity::getDepartmentId);
        } else {
            duplicateQuery.eq(AssetGrantEntity::getDepartmentId, departmentId);
        }
        AssetGrantEntity existing = grantMapper.selectOne(duplicateQuery.last("limit 1"));
        if (existing != null) {
            return toDomain(existing);
        }
        Instant now = Instant.now();
        AssetGrantEntity entity = new AssetGrantEntity();
        entity.setId("grant_" + UUID.randomUUID());
        entity.setAssetType(assetType);
        entity.setAssetId(assetId);
        entity.setPermission(USE);
        entity.setUnitId(blankToNull(unitId));
        entity.setUnitScope(normalize(unitScope, SELF));
        entity.setDepartmentId(blankToNull(departmentId));
        entity.setDepartmentScope(normalize(departmentScope, SELF));
        entity.setEnabled(enabled == null || enabled);
        entity.setCreatedBy(blankToNull(createdBy));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        grantMapper.insert(entity);
        return toDomain(entity);
    }

    public List<AssetGrant> replaceUseGrants(
            String assetType,
            String assetId,
            String ownerUnitId,
            List<String> unitIds,
            String unitScope,
            List<String> departmentIds,
            String departmentScope,
            String createdBy,
            Map<String, Object> managerContext
    ) {
        assertCanManage(ownerUnitId, managerContext);
        grantMapper.delete(new LambdaQueryWrapper<AssetGrantEntity>()
                .eq(AssetGrantEntity::getAssetType, assetType)
                .eq(AssetGrantEntity::getAssetId, assetId)
                .eq(AssetGrantEntity::getPermission, USE));
        List<AssetGrant> saved = new ArrayList<>();
        if (ownerUnitId != null && !ownerUnitId.isBlank()) {
            saved.add(save(assetType, assetId, USE, ownerUnitId, SELF, null, SELF, true, createdBy));
        }
        Set<String> effectiveDepartments = new LinkedHashSet<>();
        if (departmentIds != null) {
            departmentIds.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(effectiveDepartments::add);
        }
        Set<String> effectiveUnits = new LinkedHashSet<>();
        if (unitIds != null) {
            unitIds.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .filter(value -> ownerUnitId == null
                            || !value.equals(ownerUnitId)
                            || !effectiveDepartments.isEmpty())
                    .forEach(effectiveUnits::add);
        }
        String normalizedUnitScope = normalize(unitScope, SELF);
        String normalizedDepartmentScope = normalize(departmentScope, SELF);
        if (effectiveUnits.isEmpty()) {
            if (effectiveDepartments.isEmpty()) {
                saved.add(save(assetType, assetId, USE, null, normalizedUnitScope, null, normalizedDepartmentScope, true, createdBy));
            } else {
                for (String departmentId : effectiveDepartments) {
                    saved.add(save(assetType, assetId, USE, null, normalizedUnitScope, departmentId, normalizedDepartmentScope, true, createdBy));
                }
            }
            return saved;
        }
        for (String unitId : effectiveUnits) {
            if (effectiveDepartments.isEmpty()) {
                saved.add(save(assetType, assetId, USE, unitId, normalizedUnitScope, null, normalizedDepartmentScope, true, createdBy));
                continue;
            }
            for (String departmentId : effectiveDepartments) {
                saved.add(save(assetType, assetId, USE, unitId, normalizedUnitScope, departmentId, normalizedDepartmentScope, true, createdBy));
            }
        }
        return saved;
    }

    public void delete(String id) {
        grantMapper.deleteById(id);
    }

    public void assertAllowed(String assetType, String assetId, String ownerUnitId, Map<String, Object> context) {
        if (!isAllowed(assetType, assetId, ownerUnitId, context)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, assetType + " access denied: " + assetId);
        }
    }

    public boolean isAllowed(String assetType, String assetId, String ownerUnitId, Map<String, Object> context) {
        if (!hasContext(context)) {
            return true;
        }
        String unitId = resolveUnitId(context);
        if (unitId.isBlank()) {
            return true;
        }
        if (ownerUnitId != null && !ownerUnitId.isBlank() && ownerUnitMatches(ownerUnitId, unitId)) {
            return true;
        }
        List<AssetGrantEntity> grants = grantMapper.selectList(new LambdaQueryWrapper<AssetGrantEntity>()
                .eq(AssetGrantEntity::getAssetType, assetType)
                .eq(AssetGrantEntity::getAssetId, assetId)
                .eq(AssetGrantEntity::getPermission, USE)
                .eq(AssetGrantEntity::getEnabled, true));
        if (grants.isEmpty()) {
            return ownerUnitId == null || ownerUnitId.isBlank();
        }
        List<String> departmentIds = stringList(context.get("departmentIds"));
        String activeUnitPath = organizationPath(unitId);
        for (AssetGrantEntity grant : grants) {
            if (isOwnerGrant(grant, ownerUnitId)) {
                continue;
            }
            if (!unitMatches(grant, unitId, activeUnitPath)) {
                continue;
            }
            if (!departmentMatches(grant, departmentIds)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public void assertCanManage(String ownerUnitId, Map<String, Object> context) {
        if (!hasContext(context)) {
            return;
        }
        if (ownerUnitId == null || ownerUnitId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset owner unit is required.");
        }
        if (!ownerUnitMatches(ownerUnitId, resolveUnitId(context))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner unit can manage grants.");
        }
    }

    private boolean isOwnerGrant(AssetGrantEntity grant, String ownerUnitId) {
        return ownerUnitId != null
                && ownerUnitId.equals(grant.getUnitId())
                && grant.getDepartmentId() == null;
    }

    private boolean ownerUnitMatches(String ownerUnitId, String activeUnitId) {
        return ownerUnitId != null && ownerUnitId.equals(activeUnitId);
    }

    private void validateAssetType(String assetType) {
        if (assetType == null || assetType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetType is required.");
        }
    }

    private boolean departmentMatches(AssetGrantEntity grant, List<String> activeDepartmentIds) {
        if (grant.getDepartmentId() == null) {
            return true;
        }
        if (activeDepartmentIds.isEmpty()) {
            return false;
        }
        String grantDeptScope = normalize(grant.getDepartmentScope(), SELF);
        for (String activeDepartmentId : activeDepartmentIds) {
            if (Objects.equals(grant.getDepartmentId(), activeDepartmentId)) {
                return true;
            }
            if (SUBTREE.equals(grantDeptScope)) {
                String activePath = organizationPath(activeDepartmentId);
                String grantPath = organizationPath(grant.getDepartmentId());
                if (grantPath != null && activePath != null && activePath.startsWith(grantPath + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean unitMatches(AssetGrantEntity grant, String unitId, String activeUnitPath) {
        if (grant.getUnitId() == null || grant.getUnitId().isBlank()) {
            return true;
        }
        if (Objects.equals(grant.getUnitId(), unitId)) {
            return true;
        }
        if (!SUBTREE.equals(grant.getUnitScope()) || activeUnitPath == null || activeUnitPath.isBlank()) {
            return false;
        }
        String grantPath = organizationPath(grant.getUnitId());
        return grantPath != null && !grantPath.isBlank() && activeUnitPath.startsWith(grantPath + "/");
    }

    private String organizationPath(String organizationId) {
        OrganizationEntity organization = organizationMapper.selectById(organizationId);
        return organization == null ? null : organization.getPath();
    }

    private String resolveUnitId(Map<String, Object> context) {
        String unitId = stringValue(context.get("unitId"));
        if (unitId.isBlank()) {
            unitId = stringValue(context.get("activeUnitId"));
        }
        return unitId;
    }

    private boolean hasContext(Map<String, Object> context) {
        return context != null && !resolveUnitId(context).isBlank();
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return List.of(stringValue);
        }
        return List.of();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public String loadOwnerUnitId(String assetType, String assetId) {
        if (KNOWLEDGE_BASE.equals(assetType)) {
            KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(assetId);
            return entity == null ? null : entity.getOwnerUnitId();
        }
        if (BOT.equals(assetType)) {
            AiBotEntity entity = aiBotMapper.selectById(assetId);
            return entity == null ? null : entity.getOwnerUnitId();
        }
        return null;
    }

    private void backfillOwnerUnitId(String assetType, String assetId, String ownerUnitId) {
        if (KNOWLEDGE_BASE.equals(assetType)) {
            KnowledgeBaseEntity entity = knowledgeBaseMapper.selectById(assetId);
            if (entity != null && (entity.getOwnerUnitId() == null || entity.getOwnerUnitId().isBlank())) {
                entity.setOwnerUnitId(ownerUnitId);
                knowledgeBaseMapper.updateById(entity);
            }
            return;
        }
        if (BOT.equals(assetType)) {
            AiBotEntity entity = aiBotMapper.selectById(assetId);
            if (entity != null && (entity.getOwnerUnitId() == null || entity.getOwnerUnitId().isBlank())) {
                entity.setOwnerUnitId(ownerUnitId);
                aiBotMapper.updateById(entity);
            }
        }
    }

    private AssetGrant toDomain(AssetGrantEntity entity) {
        return new AssetGrant(
                entity.getId(),
                entity.getAssetType(),
                entity.getAssetId(),
                entity.getPermission(),
                entity.getUnitId(),
                entity.getUnitScope(),
                entity.getDepartmentId(),
                entity.getDepartmentScope(),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
