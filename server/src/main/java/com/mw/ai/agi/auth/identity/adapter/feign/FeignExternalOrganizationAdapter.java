package com.mw.ai.agi.auth.identity.adapter.feign;

import com.mw.ai.agi.auth.identity.adapter.ExternalOrganizationAdapter;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalOrganizationProfile;
import com.mw.ai.agi.integration.organization.OrganizationClient;
import com.mw.ai.agi.integration.organization.OrganizationDepartmentResponse;
import com.mw.ai.agi.integration.organization.OrganizationUserResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Primary
@ConditionalOnProperty(prefix = "agi.identity.adapter", name = "organization", havingValue = "feign")
public class FeignExternalOrganizationAdapter implements ExternalOrganizationAdapter {
    private final OrganizationClient organizationClient;

    public FeignExternalOrganizationAdapter(OrganizationClient organizationClient) {
        this.organizationClient = organizationClient;
    }

    @Override
    public ExternalOrganizationProfile loadOrganizationContext(String userId) {
        OrganizationUserResponse user = organizationClient.getUser(userId);
        List<OrganizationDepartmentResponse> departments = organizationClient.listUserDepartments(userId);
        List<String> departmentIds = departments.stream().map(OrganizationDepartmentResponse::id).toList();
        String activeDepartmentId = user.departmentId();
        if ((activeDepartmentId == null || activeDepartmentId.isBlank()) && !departmentIds.isEmpty()) {
            activeDepartmentId = departmentIds.get(0);
        }
        Set<String> organizationIds = new LinkedHashSet<>();
        if (activeDepartmentId != null && !activeDepartmentId.isBlank()) {
            organizationIds.add(activeDepartmentId);
        }
        organizationIds.addAll(departmentIds);
        return new ExternalOrganizationProfile(
                userId,
                new ArrayList<>(organizationIds),
                organizationIds.isEmpty() ? null : organizationIds.iterator().next(),
                List.of(),
                null,
                departmentIds,
                List.of()
        );
    }
}
