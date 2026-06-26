package com.mw.ai.agi.auth.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "agi.identity")
public class IdentityProperties {
    private String mode = "local";
    private String fixedTenantId = "tenant_default";
    private boolean autoProvisionShadowUser = false;
    private Map<String, String> roleMapping = new LinkedHashMap<>();
    private Remote remote = new Remote();
    private Adapter adapter = new Adapter();

    public IdentityMode resolvedMode() {
        return IdentityMode.from(mode);
    }

    public boolean isRemote() {
        return resolvedMode() == IdentityMode.REMOTE;
    }

    public List<String> mapRoleIds(List<String> externalRoleIds) {
        if (externalRoleIds == null || externalRoleIds.isEmpty()) {
            return List.of();
        }
        return externalRoleIds.stream()
                .map(role -> roleMapping.getOrDefault(role, role))
                .distinct()
                .toList();
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFixedTenantId() {
        return fixedTenantId;
    }

    public void setFixedTenantId(String fixedTenantId) {
        this.fixedTenantId = fixedTenantId;
    }

    public boolean isAutoProvisionShadowUser() {
        return autoProvisionShadowUser;
    }

    public void setAutoProvisionShadowUser(boolean autoProvisionShadowUser) {
        this.autoProvisionShadowUser = autoProvisionShadowUser;
    }

    public Map<String, String> getRoleMapping() {
        return roleMapping;
    }

    public void setRoleMapping(Map<String, String> roleMapping) {
        this.roleMapping = roleMapping == null ? new LinkedHashMap<>() : new LinkedHashMap<>(roleMapping);
    }

    public Remote getRemote() {
        return remote;
    }

    public void setRemote(Remote remote) {
        this.remote = remote == null ? new Remote() : remote;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter == null ? new Adapter() : adapter;
    }

    public static class Remote {
        private boolean allowBreakGlassLogin = true;
        private boolean disableLocalOrgAdmin = true;

        public boolean isAllowBreakGlassLogin() {
            return allowBreakGlassLogin;
        }

        public void setAllowBreakGlassLogin(boolean allowBreakGlassLogin) {
            this.allowBreakGlassLogin = allowBreakGlassLogin;
        }

        public boolean isDisableLocalOrgAdmin() {
            return disableLocalOrgAdmin;
        }

        public void setDisableLocalOrgAdmin(boolean disableLocalOrgAdmin) {
            this.disableLocalOrgAdmin = disableLocalOrgAdmin;
        }
    }

    public static class Adapter {
        private String auth = "unconfigured";
        private String organization = "unconfigured";

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }
    }
}
