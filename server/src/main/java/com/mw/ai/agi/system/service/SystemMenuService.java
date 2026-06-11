package com.mw.ai.agi.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.auth.service.TenantAdminGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.system.persistence.SysMenuEntity;
import com.mw.ai.agi.system.persistence.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SystemMenuService {
    private static final Pattern MENU_KEY_PATTERN = Pattern.compile("^[a-z0-9-]+$");
    private static final Pattern STATUS_PATTERN = Pattern.compile("^(ENABLED|DISABLED)$");

    private final SysMenuMapper menuMapper;
    private final TenantAdminGuard tenantAdminGuard;
    private final SystemAuditService systemAuditService;

    public SystemMenuService(
            SysMenuMapper menuMapper,
            TenantAdminGuard tenantAdminGuard,
            SystemAuditService systemAuditService
    ) {
        this.menuMapper = menuMapper;
        this.tenantAdminGuard = tenantAdminGuard;
        this.systemAuditService = systemAuditService;
    }

    public List<SysMenuEntity> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getTenantId, tenantId())
                .orderByAsc(SysMenuEntity::getGroupTitle)
                .orderByAsc(SysMenuEntity::getSortOrder));
    }

    public List<MenuGroupView> listVisibleGroups(boolean platformOperator) {
        List<SysMenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getTenantId, tenantId())
                .eq(SysMenuEntity::getStatus, "ENABLED")
                .eq(SysMenuEntity::getVisible, true)
                .orderByAsc(SysMenuEntity::getGroupTitle)
                .orderByAsc(SysMenuEntity::getSortOrder));
        Map<String, MenuGroupView> groups = new LinkedHashMap<>();
        for (SysMenuEntity menu : menus) {
            if (Boolean.TRUE.equals(menu.getPlatformOnly()) && !platformOperator) {
                continue;
            }
            groups.computeIfAbsent(menu.getGroupTitle(), MenuGroupView::new).items().add(menu);
        }
        return new ArrayList<>(groups.values());
    }

    public SysMenuEntity create(
            String groupTitle,
            String menuKey,
            String title,
            String path,
            Integer sortOrder,
            Boolean visible,
            Boolean platformOnly,
            String status
    ) {
        tenantAdminGuard.assertSystemConfigurator();
        validateMenuInput(groupTitle, menuKey, title, path, status);
        ensureMenuKeyUnique(menuKey, null);
        Instant now = Instant.now();
        String operator = OperatorContext.currentUserId();
        SysMenuEntity entity = new SysMenuEntity();
        entity.setId("menu_" + UUID.randomUUID().toString().replace("-", ""));
        entity.setTenantId(tenantId());
        entity.setGroupTitle(groupTitle.trim());
        entity.setMenuKey(menuKey.trim());
        entity.setTitle(title.trim());
        entity.setPath(normalizePath(path));
        entity.setSortOrder(sortOrder == null ? 0 : sortOrder);
        entity.setVisible(visible == null || visible);
        entity.setPlatformOnly(platformOnly != null && platformOnly);
        entity.setStatus(normalizeStatus(status));
        entity.setCreatedBy(operator);
        entity.setUpdatedBy(operator);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        menuMapper.insert(entity);
        systemAuditService.recordSuccess("SYSTEM_MENU_CREATED", entity.getId());
        return entity;
    }

    public SysMenuEntity update(
            String id,
            String groupTitle,
            String menuKey,
            String title,
            String path,
            Integer sortOrder,
            Boolean visible,
            Boolean platformOnly,
            String status
    ) {
        tenantAdminGuard.assertSystemConfigurator();
        validateMenuInput(groupTitle, menuKey, title, path, status);
        SysMenuEntity current = requireMenu(id);
        ensureMenuKeyUnique(menuKey, id);
        current.setGroupTitle(groupTitle.trim());
        current.setMenuKey(menuKey.trim());
        current.setTitle(title.trim());
        current.setPath(normalizePath(path));
        current.setSortOrder(sortOrder == null ? 0 : sortOrder);
        current.setVisible(visible == null || visible);
        current.setPlatformOnly(platformOnly != null && platformOnly);
        current.setStatus(normalizeStatus(status));
        current.setUpdatedBy(OperatorContext.currentUserId());
        current.setUpdatedAt(Instant.now());
        menuMapper.updateById(current);
        systemAuditService.recordSuccess("SYSTEM_MENU_UPDATED", current.getId());
        return current;
    }

    public void delete(String id) {
        tenantAdminGuard.assertSystemConfigurator();
        SysMenuEntity menu = requireMenu(id);
        menuMapper.deleteById(menu.getId());
        systemAuditService.recordSuccess("SYSTEM_MENU_DELETED", menu.getId());
    }

    private SysMenuEntity requireMenu(String id) {
        SysMenuEntity entity = menuMapper.selectById(id);
        if (entity == null || !tenantId().equals(entity.getTenantId())) {
            throw new IllegalArgumentException("Menu not found.");
        }
        return entity;
    }

    private void ensureMenuKeyUnique(String menuKey, String excludeId) {
        LambdaQueryWrapper<SysMenuEntity> wrapper = new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getTenantId, tenantId())
                .eq(SysMenuEntity::getMenuKey, menuKey.trim());
        if (excludeId != null) {
            wrapper.ne(SysMenuEntity::getId, excludeId);
        }
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("Menu key already exists.");
        }
    }

    private void validateMenuInput(String groupTitle, String menuKey, String title, String path, String status) {
        if (groupTitle == null || groupTitle.isBlank()) {
            throw new IllegalArgumentException("Menu group title is required.");
        }
        if (menuKey == null || menuKey.isBlank() || !MENU_KEY_PATTERN.matcher(menuKey.trim()).matches()) {
            throw new IllegalArgumentException("Menu key must use lowercase letters, numbers, or hyphens.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Menu title is required.");
        }
        if (path == null || path.isBlank() || !path.trim().startsWith("/")) {
            throw new IllegalArgumentException("Menu path must start with '/'.");
        }
        normalizeStatus(status);
    }

    private String normalizePath(String path) {
        return path.trim();
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank() ? "ENABLED" : status.trim();
        if (!STATUS_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Menu status must be ENABLED or DISABLED.");
        }
        return normalized;
    }

    private String tenantId() {
        return TenantContext.requireTenantId();
    }

    public record MenuGroupView(String title, List<SysMenuEntity> items) {
        public MenuGroupView(String title) {
            this(title, new ArrayList<>());
        }
    }
}
