package com.mw.ai.agi.connector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.service.TenantBusinessGuard;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.connector.domain.Connector;
import com.mw.ai.agi.connector.domain.ConnectorOperation;
import com.mw.ai.agi.connector.persistence.ConnectorEntity;
import com.mw.ai.agi.connector.persistence.ConnectorMapper;
import com.mw.ai.agi.connector.persistence.ConnectorOperationEntity;
import com.mw.ai.agi.connector.persistence.ConnectorOperationMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConnectorService {
    private final ObjectProvider<ConnectorMapper> connectorMapperProvider;
    private final ObjectProvider<ConnectorOperationMapper> operationMapperProvider;
    private final TenantBusinessGuard tenantGuard;

    public ConnectorService(
            ObjectProvider<ConnectorMapper> connectorMapperProvider,
            ObjectProvider<ConnectorOperationMapper> operationMapperProvider,
            TenantBusinessGuard tenantGuard
    ) {
        this.connectorMapperProvider = connectorMapperProvider;
        this.operationMapperProvider = operationMapperProvider;
        this.tenantGuard = tenantGuard;
    }

    public List<Connector> listConnectors() {
        return connectorMapper().selectList(new LambdaQueryWrapper<ConnectorEntity>()
                        .eq(ConnectorEntity::getTenantId, currentTenantId())
                        .orderByAsc(ConnectorEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public Connector getConnector(String id) {
        ConnectorEntity entity = connectorMapper().selectById(id);
        if (entity == null || !currentTenantId().equals(entity.getTenantId())) {
            throw new ConnectorNotFoundException(id);
        }
        return toDomain(entity);
    }

    public Connector getConnectorByCode(String code) {
        ConnectorEntity entity = connectorMapper().selectOne(new LambdaQueryWrapper<ConnectorEntity>()
                .eq(ConnectorEntity::getTenantId, currentTenantId())
                .eq(ConnectorEntity::getCode, code));
        if (entity == null) {
            throw new ConnectorNotFoundException(code);
        }
        return toDomain(entity);
    }

    public Connector createConnector(
            String name, String code, String type, String accessType, String baseUrl,
            String authMode, String authConfig, boolean enabled, String description
    ) {
        Instant now = Instant.now();
        ConnectorEntity entity = new ConnectorEntity();
        entity.setId("conn_" + UUID.randomUUID());
        entity.setTenantId(currentTenantId());
        entity.setName(name);
        entity.setCode(code);
        entity.setType(type);
        entity.setAccessType(accessType == null || accessType.isBlank() ? "HTTP" : accessType);
        entity.setBaseUrl(baseUrl);
        entity.setAuthMode(authMode);
        entity.setAuthConfig(authConfig);
        entity.setEnabled(enabled);
        entity.setDescription(description);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        connectorMapper().insert(entity);
        return toDomain(entity);
    }

    public Connector updateConnector(
            String id, String name, String code, String type, String accessType, String baseUrl,
            String authMode, String authConfig, boolean enabled, String description
    ) {
        ConnectorEntity entity = requireConnectorEntity(id);
        entity.setName(name);
        entity.setCode(code);
        entity.setType(type);
        entity.setAccessType(accessType == null || accessType.isBlank() ? "HTTP" : accessType);
        entity.setBaseUrl(baseUrl);
        entity.setAuthMode(authMode);
        entity.setAuthConfig(authConfig);
        entity.setEnabled(enabled);
        entity.setDescription(description);
        entity.setUpdatedAt(Instant.now());
        connectorMapper().updateById(entity);
        return toDomain(entity);
    }

    public void deleteConnector(String id) {
        requireConnectorEntity(id);
        operationMapper().delete(new LambdaQueryWrapper<ConnectorOperationEntity>()
                .eq(ConnectorOperationEntity::getConnectorId, id));
        connectorMapper().deleteById(id);
    }

    public List<ConnectorOperation> listOperations(String connectorId) {
        getConnector(connectorId);
        return operationMapper().selectList(new LambdaQueryWrapper<ConnectorOperationEntity>()
                        .eq(ConnectorOperationEntity::getConnectorId, connectorId)
                        .orderByAsc(ConnectorOperationEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public ConnectorOperation getOperation(String connectorId, String operationId) {
        ConnectorOperationEntity entity = requireOperationEntity(connectorId, operationId);
        return toDomain(entity);
    }

    public Optional<ConnectorOperation> findOperationByCode(String connectorCode, String operationCode) {
        Connector connector = getConnectorByCode(connectorCode);
        ConnectorOperationEntity entity = operationMapper().selectOne(new LambdaQueryWrapper<ConnectorOperationEntity>()
                .eq(ConnectorOperationEntity::getTenantId, currentTenantId())
                .eq(ConnectorOperationEntity::getConnectorId, connector.id())
                .eq(ConnectorOperationEntity::getCode, operationCode));
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    public ConnectorOperation createOperation(
            String connectorId, String name, String code, String method, String path,
            String operationType, String riskLevel, boolean needConfirm,
            String confirmSummaryTemplate, String requestTemplate, boolean enabled, String description
    ) {
        getConnector(connectorId);
        Instant now = Instant.now();
        ConnectorOperationEntity entity = new ConnectorOperationEntity();
        entity.setId("cop_" + UUID.randomUUID());
        entity.setTenantId(currentTenantId());
        entity.setConnectorId(connectorId);
        entity.setName(name);
        entity.setCode(code);
        entity.setMethod(method == null || method.isBlank() ? "GET" : method.toUpperCase());
        entity.setPath(path);
        entity.setOperationType(operationType == null || operationType.isBlank() ? "QUERY" : operationType);
        entity.setRiskLevel(riskLevel == null || riskLevel.isBlank() ? "LOW" : riskLevel);
        entity.setNeedConfirm(needConfirm);
        entity.setConfirmSummaryTemplate(confirmSummaryTemplate);
        entity.setRequestTemplate(requestTemplate);
        entity.setEnabled(enabled);
        entity.setDescription(description);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        operationMapper().insert(entity);
        return toDomain(entity);
    }

    public ConnectorOperation updateOperation(
            String connectorId, String operationId, String name, String code, String method, String path,
            String operationType, String riskLevel, boolean needConfirm,
            String confirmSummaryTemplate, String requestTemplate, boolean enabled, String description
    ) {
        ConnectorOperationEntity entity = requireOperationEntity(connectorId, operationId);
        entity.setName(name);
        entity.setCode(code);
        entity.setMethod(method == null || method.isBlank() ? "GET" : method.toUpperCase());
        entity.setPath(path);
        entity.setOperationType(operationType == null || operationType.isBlank() ? "QUERY" : operationType);
        entity.setRiskLevel(riskLevel == null || riskLevel.isBlank() ? "LOW" : riskLevel);
        entity.setNeedConfirm(needConfirm);
        entity.setConfirmSummaryTemplate(confirmSummaryTemplate);
        entity.setRequestTemplate(requestTemplate);
        entity.setEnabled(enabled);
        entity.setDescription(description);
        entity.setUpdatedAt(Instant.now());
        operationMapper().updateById(entity);
        return toDomain(entity);
    }

    public void deleteOperation(String connectorId, String operationId) {
        requireOperationEntity(connectorId, operationId);
        operationMapper().deleteById(operationId);
    }

    public ConnectorExportBundle exportBundle(String connectorId) {
        Connector connector = getConnector(connectorId);
        List<ConnectorOperationExport> operations = listOperations(connectorId).stream()
                .map(op -> new ConnectorOperationExport(
                        op.name(),
                        op.code(),
                        op.method(),
                        op.path(),
                        op.operationType(),
                        op.riskLevel(),
                        op.needConfirm(),
                        op.confirmSummaryTemplate(),
                        op.requestTemplate(),
                        op.enabled(),
                        op.description()
                ))
                .toList();
        return new ConnectorExportBundle(
                new ConnectorExport(
                        connector.name(),
                        connector.code(),
                        connector.type(),
                        connector.accessType(),
                        connector.baseUrl(),
                        connector.authMode(),
                        connector.authConfig(),
                        connector.enabled(),
                        connector.description()
                ),
                operations
        );
    }

    public Connector importBundle(ConnectorExportBundle bundle, boolean overwrite) {
        if (bundle == null || bundle.connector() == null) {
            throw new IllegalArgumentException("Import bundle is empty");
        }
        ConnectorExport source = bundle.connector();
        ConnectorEntity existing = connectorMapper().selectOne(new LambdaQueryWrapper<ConnectorEntity>()
                .eq(ConnectorEntity::getTenantId, currentTenantId())
                .eq(ConnectorEntity::getCode, source.code()));
        Connector connector;
        if (existing == null) {
            connector = createConnector(
                    source.name(),
                    source.code(),
                    source.type(),
                    source.accessType(),
                    source.baseUrl(),
                    source.authMode(),
                    source.authConfig(),
                    source.enabled() == null || source.enabled(),
                    source.description()
            );
        } else if (overwrite) {
            connector = updateConnector(
                    existing.getId(),
                    source.name(),
                    source.code(),
                    source.type(),
                    source.accessType(),
                    source.baseUrl(),
                    source.authMode(),
                    source.authConfig(),
                    source.enabled() == null || source.enabled(),
                    source.description()
            );
        } else {
            throw new IllegalArgumentException("Connector code already exists: " + source.code());
        }
        if (bundle.operations() != null) {
            for (ConnectorOperationExport operation : bundle.operations()) {
                ConnectorOperationEntity opExisting = operationMapper().selectOne(new LambdaQueryWrapper<ConnectorOperationEntity>()
                        .eq(ConnectorOperationEntity::getTenantId, currentTenantId())
                        .eq(ConnectorOperationEntity::getConnectorId, connector.id())
                        .eq(ConnectorOperationEntity::getCode, operation.code()));
                if (opExisting == null) {
                    createOperation(
                            connector.id(),
                            operation.name(),
                            operation.code(),
                            operation.method(),
                            operation.path(),
                            operation.operationType(),
                            operation.riskLevel(),
                            operation.needConfirm() != null && operation.needConfirm(),
                            operation.confirmSummaryTemplate(),
                            operation.requestTemplate(),
                            operation.enabled() == null || operation.enabled(),
                            operation.description()
                    );
                } else if (overwrite) {
                    updateOperation(
                            connector.id(),
                            opExisting.getId(),
                            operation.name(),
                            operation.code(),
                            operation.method(),
                            operation.path(),
                            operation.operationType(),
                            operation.riskLevel(),
                            operation.needConfirm() != null && operation.needConfirm(),
                            operation.confirmSummaryTemplate(),
                            operation.requestTemplate(),
                            operation.enabled() == null || operation.enabled(),
                            operation.description()
                    );
                }
            }
        }
        return connector;
    }

    public record ConnectorExport(
            String name,
            String code,
            String type,
            String accessType,
            String baseUrl,
            String authMode,
            String authConfig,
            Boolean enabled,
            String description
    ) {
    }

    public record ConnectorOperationExport(
            String name,
            String code,
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

    public record ConnectorExportBundle(ConnectorExport connector, List<ConnectorOperationExport> operations) {
    }

    private ConnectorEntity requireConnectorEntity(String id) {
        ConnectorEntity entity = connectorMapper().selectById(id);
        if (entity == null || !currentTenantId().equals(entity.getTenantId())) {
            throw new ConnectorNotFoundException(id);
        }
        return entity;
    }

    private ConnectorOperationEntity requireOperationEntity(String connectorId, String operationId) {
        ConnectorOperationEntity entity = operationMapper().selectById(operationId);
        if (entity == null || !currentTenantId().equals(entity.getTenantId()) || !connectorId.equals(entity.getConnectorId())) {
            throw new ConnectorNotFoundException(operationId);
        }
        return entity;
    }

    private Connector toDomain(ConnectorEntity entity) {
        return new Connector(
                entity.getId(), entity.getTenantId(), entity.getName(), entity.getCode(),
                entity.getType(), entity.getAccessType(), entity.getBaseUrl(), entity.getAuthMode(),
                entity.getAuthConfig(), Boolean.TRUE.equals(entity.getEnabled()), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private ConnectorOperation toDomain(ConnectorOperationEntity entity) {
        return new ConnectorOperation(
                entity.getId(), entity.getTenantId(), entity.getConnectorId(), entity.getName(), entity.getCode(),
                entity.getMethod(), entity.getPath(), entity.getOperationType(), entity.getRiskLevel(),
                Boolean.TRUE.equals(entity.getNeedConfirm()), entity.getConfirmSummaryTemplate(),
                entity.getRequestTemplate(), Boolean.TRUE.equals(entity.getEnabled()), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private String currentTenantId() {
        return tenantGuard == null ? TenantContext.requireTenantId() : tenantGuard.currentTenantId();
    }

    private ConnectorMapper connectorMapper() {
        ConnectorMapper mapper = connectorMapperProvider.getIfAvailable();
        if (mapper == null) {
            throw new IllegalStateException("ConnectorMapper unavailable");
        }
        return mapper;
    }

    private ConnectorOperationMapper operationMapper() {
        ConnectorOperationMapper mapper = operationMapperProvider.getIfAvailable();
        if (mapper == null) {
            throw new IllegalStateException("ConnectorOperationMapper unavailable");
        }
        return mapper;
    }
}
