package com.mw.ai.agi.connector.api;

import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.connector.domain.Connector;
import com.mw.ai.agi.connector.domain.ConnectorOperation;
import com.mw.ai.agi.connector.service.ConnectorService;
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
@RequestMapping("/api/connectors")
public class ConnectorController {
    private final ConnectorService connectorService;

    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
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
