package com.mw.ai.agi.connector.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.connector.domain.Connector;
import com.mw.ai.agi.connector.domain.ConnectorOperation;
import com.mw.ai.agi.auth.service.RequestIdentity;
import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import com.mw.ai.agi.chat.service.AgentAuditService;
import com.mw.ai.agi.connector.service.ConnectorRuntimeService;
import com.mw.ai.agi.connector.service.ConnectorService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connectors")
public class ConnectorController {
    private final ConnectorService connectorService;
    private final ConnectorRuntimeService connectorRuntimeService;
    private final RequestIdentitySupport identitySupport;
    private final AgentAuditService agentAuditService;

    public ConnectorController(
            ConnectorService connectorService,
            ConnectorRuntimeService connectorRuntimeService,
            RequestIdentitySupport identitySupport,
            AgentAuditService agentAuditService
    ) {
        this.connectorService = connectorService;
        this.connectorRuntimeService = connectorRuntimeService;
        this.identitySupport = identitySupport;
        this.agentAuditService = agentAuditService;
    }

    @GetMapping("/stats")
    public ApiResponse<PageResponse<AgentAuditService.ConnectorCallStat>> stats() {
        List<AgentAuditService.ConnectorCallStat> items = agentAuditService.connectorCallStats(2000);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/{id}/export")
    public ApiResponse<ConnectorService.ConnectorExportBundle> export(@PathVariable String id) {
        return ApiResponse.success(connectorService.exportBundle(id));
    }

    @PostMapping("/import")
    public ApiResponse<Connector> importBundle(@RequestBody ImportConnectorRequest request) {
        return ApiResponse.success(connectorService.importBundle(
                request.bundle(),
                request.overwrite() == null || request.overwrite()
        ));
    }

    @GetMapping
    public ApiResponse<PageResponse<Connector>> list() {
        List<Connector> items = connectorService.listConnectors();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping
    public ApiResponse<Connector> create(@Valid @RequestBody SaveConnectorRequest request) {
        return ApiResponse.success(connectorService.createConnector(
                request.name(), request.code(), request.type(), request.accessType(),
                request.baseUrl(), request.authMode(), request.authConfig(),
                request.enabled() == null || request.enabled(), request.description()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<Connector> update(@PathVariable String id, @Valid @RequestBody SaveConnectorRequest request) {
        return ApiResponse.success(connectorService.updateConnector(
                id, request.name(), request.code(), request.type(), request.accessType(),
                request.baseUrl(), request.authMode(), request.authConfig(),
                request.enabled() == null || request.enabled(), request.description()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        connectorService.deleteConnector(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/operations")
    public ApiResponse<PageResponse<ConnectorOperation>> listOperations(@PathVariable String id) {
        List<ConnectorOperation> items = connectorService.listOperations(id);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping("/{id}/operations")
    public ApiResponse<ConnectorOperation> createOperation(@PathVariable String id, @Valid @RequestBody SaveOperationRequest request) {
        return ApiResponse.success(connectorService.createOperation(
                id, request.name(), request.code(), request.method(), request.path(),
                request.operationType(), request.riskLevel(), request.needConfirm() != null && request.needConfirm(),
                request.confirmSummaryTemplate(), request.requestTemplate(),
                request.enabled() == null || request.enabled(), request.description()
        ));
    }

    @PutMapping("/{id}/operations/{operationId}")
    public ApiResponse<ConnectorOperation> updateOperation(
            @PathVariable String id,
            @PathVariable String operationId,
            @Valid @RequestBody SaveOperationRequest request
    ) {
        return ApiResponse.success(connectorService.updateOperation(
                id, operationId, request.name(), request.code(), request.method(), request.path(),
                request.operationType(), request.riskLevel(), request.needConfirm() != null && request.needConfirm(),
                request.confirmSummaryTemplate(), request.requestTemplate(),
                request.enabled() == null || request.enabled(), request.description()
        ));
    }

    @DeleteMapping("/{id}/operations/{operationId}")
    public ApiResponse<Void> deleteOperation(@PathVariable String id, @PathVariable String operationId) {
        connectorService.deleteOperation(id, operationId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/operations/{operationId}/test")
    public ApiResponse<TestOperationResponse> testOperation(
            @PathVariable String id,
            @PathVariable String operationId,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request
    ) {
        Map<String, Object> context = new LinkedHashMap<>(identitySupport.mergeGrantContext(request, Map.of()));
        identitySupport.resolve(request).ifPresent(identity -> fillIdentityContext(context, identity));
        try {
            Map<String, Object> result = connectorRuntimeService.testOperation(
                    id,
                    operationId,
                    payload == null ? Map.of() : payload,
                    context
            );
            boolean success = Boolean.TRUE.equals(result.get("success"));
            Object statusCode = result.get("statusCode");
            Object raw = result.get("raw");
            return ApiResponse.success(new TestOperationResponse(
                    success,
                    statusCode instanceof Number number ? number.intValue() : null,
                    raw == null ? null : String.valueOf(raw),
                    null,
                    stringValue(result.get("traceId")),
                    stringValue(result.get("requestUrl")),
                    longValue(result.get("durationMs"))
            ));
        } catch (RuntimeException ex) {
            return ApiResponse.success(new TestOperationResponse(false, null, null, ex.getMessage(), null, null, null));
        }
    }

    private void fillIdentityContext(Map<String, Object> context, RequestIdentity identity) {
        context.put("userId", identity.userId());
        context.put("tenantId", identity.tenantId());
        if (identity.activeUnitId() != null) {
            context.put("unitId", identity.activeUnitId());
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public record PageResponse<T>(List<T> items, long total) {
    }

    public record SaveConnectorRequest(
            @NotBlank String name,
            @NotBlank String code,
            @NotBlank String type,
            String accessType,
            String baseUrl,
            String authMode,
            String authConfig,
            Boolean enabled,
            String description
    ) {
    }

    public record TestOperationResponse(
            boolean success,
            Integer statusCode,
            String body,
            String errorMessage,
            String traceId,
            String requestUrl,
            Long durationMs
    ) {
    }

    public record ImportConnectorRequest(
            ConnectorService.ConnectorExportBundle bundle,
            Boolean overwrite
    ) {
    }

    public record SaveOperationRequest(
            @NotBlank String name,
            @NotBlank String code,
            String method,
            String path,
            String operationType,
            String riskLevel,
            Boolean needConfirm,
            String confirmSummaryTemplate,
            String requestTemplate,
            Boolean enabled,
            String description
    ) {
    }
}
