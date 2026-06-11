package com.mw.ai.agi.system.api;

import com.mw.ai.agi.auth.service.TenantAdminGuard;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.system.persistence.SysMenuEntity;
import com.mw.ai.agi.system.service.SystemMenuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
public class SystemMenuController {
    private final SystemMenuService menuService;
    private final TenantAdminGuard tenantAdminGuard;

    public SystemMenuController(SystemMenuService menuService, TenantAdminGuard tenantAdminGuard) {
        this.menuService = menuService;
        this.tenantAdminGuard = tenantAdminGuard;
    }

    @GetMapping
    public ApiResponse<PageResponse<SysMenuEntity>> list() {
        List<SysMenuEntity> items = menuService.listAll();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/navigation")
    public ApiResponse<List<SystemMenuService.MenuGroupView>> navigation() {
        return ApiResponse.success(menuService.listVisibleGroups(tenantAdminGuard.isPlatformOperator()));
    }

    @PostMapping
    public ApiResponse<SysMenuEntity> create(@Valid @RequestBody SaveMenuRequest request) {
        return ApiResponse.success(menuService.create(
                request.groupTitle(),
                request.menuKey(),
                request.title(),
                request.path(),
                request.sortOrder(),
                request.visible(),
                request.platformOnly(),
                request.status()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysMenuEntity> update(@PathVariable String id, @Valid @RequestBody SaveMenuRequest request) {
        return ApiResponse.success(menuService.update(
                id,
                request.groupTitle(),
                request.menuKey(),
                request.title(),
                request.path(),
                request.sortOrder(),
                request.visible(),
                request.platformOnly(),
                request.status()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        menuService.delete(id);
        return ApiResponse.success(null);
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record SaveMenuRequest(
            @NotBlank String groupTitle,
            @NotBlank String menuKey,
            @NotBlank String title,
            @NotBlank String path,
            Integer sortOrder,
            Boolean visible,
            Boolean platformOnly,
            String status
    ) {
    }
}
