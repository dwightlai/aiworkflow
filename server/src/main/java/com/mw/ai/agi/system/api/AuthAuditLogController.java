package com.mw.ai.agi.system.api;

import com.mw.ai.agi.auth.persistence.AuthAuditLogEntity;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.system.service.AuthAuditLogQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/system/audit-logs")
public class AuthAuditLogController {
    private final AuthAuditLogQueryService auditLogQueryService;

    public AuthAuditLogController(AuthAuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuthAuditLogEntity>> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        AuthAuditLogQueryService.PageResult<AuthAuditLogEntity> pageResult = auditLogQueryService.search(
                eventType,
                userId,
                result,
                from,
                to,
                page,
                pageSize
        );
        return ApiResponse.success(new PageResponse<>(pageResult.items(), pageResult.total()));
    }

    @GetMapping("/event-types")
    public ApiResponse<List<String>> eventTypes() {
        return ApiResponse.success(auditLogQueryService.listEventTypes());
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
