package com.mw.ai.agi.integration.organization;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "${third-party.organization.service-name:organization-service}",
        url = "${third-party.organization.base-url:}"
)
public interface OrganizationClient {
    @GetMapping("/api/organization/users/{userId}")
    OrganizationUserResponse getUser(@PathVariable("userId") String userId);

    @GetMapping("/api/organization/departments/{departmentId}")
    OrganizationDepartmentResponse getDepartment(@PathVariable("departmentId") String departmentId);

    @GetMapping("/api/organization/users/{userId}/departments")
    List<OrganizationDepartmentResponse> listUserDepartments(@PathVariable("userId") String userId);
}
