package com.mw.ai.agi.bot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.bot.domain.BotCapability;
import com.mw.ai.agi.bot.persistence.BotCapabilityEntity;
import com.mw.ai.agi.bot.persistence.BotCapabilityMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class BotCapabilityService {
    private final ObjectProvider<BotCapabilityMapper> mapperProvider;
    private final List<BotCapability> memory = new CopyOnWriteArrayList<>();

    public BotCapabilityService(ObjectProvider<BotCapabilityMapper> mapperProvider) {
        this.mapperProvider = mapperProvider;
    }

    public List<BotCapability> listByBot(String botId) {
        BotCapabilityMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            return memory.stream().filter(item -> botId.equals(item.botId())).toList();
        }
        return mapper.selectList(new LambdaQueryWrapper<BotCapabilityEntity>()
                        .eq(BotCapabilityEntity::getBotId, botId)
                        .orderByDesc(BotCapabilityEntity::getPrimaryCapability)
                        .orderByAsc(BotCapabilityEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    public BotCapability save(
            String botId,
            String capabilityType,
            String capabilityId,
            String capabilityCode,
            String routingKeywords,
            boolean primaryCapability,
            boolean enabled
    ) {
        Instant now = Instant.now();
        BotCapability capability = new BotCapability(
                "bcap_" + UUID.randomUUID(),
                TenantContext.requireTenantId(),
                botId,
                capabilityType,
                capabilityId,
                capabilityCode,
                routingKeywords,
                primaryCapability,
                enabled,
                now,
                now
        );
        if (primaryCapability) {
            clearPrimary(botId);
        }
        persist(capability, true);
        return capability;
    }

    public BotCapability update(
            String botId,
            String id,
            String capabilityType,
            String capabilityId,
            String capabilityCode,
            String routingKeywords,
            Boolean primaryCapability,
            Boolean enabled
    ) {
        BotCapability current = get(botId, id);
        if (Boolean.TRUE.equals(primaryCapability)) {
            clearPrimary(botId);
        }
        BotCapability updated = new BotCapability(
                current.id(),
                current.tenantId(),
                current.botId(),
                capabilityType == null ? current.capabilityType() : capabilityType,
                capabilityId == null ? current.capabilityId() : capabilityId,
                capabilityCode == null ? current.capabilityCode() : capabilityCode,
                routingKeywords == null ? current.routingKeywords() : routingKeywords,
                primaryCapability == null ? current.primaryCapability() : primaryCapability,
                enabled == null ? current.enabled() : enabled,
                current.createdAt(),
                Instant.now()
        );
        persist(updated, false);
        return updated;
    }

    public void delete(String botId, String id) {
        get(botId, id);
        BotCapabilityMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            memory.removeIf(item -> id.equals(item.id()));
            return;
        }
        mapper.delete(new LambdaQueryWrapper<BotCapabilityEntity>()
                .eq(BotCapabilityEntity::getBotId, botId)
                .eq(BotCapabilityEntity::getId, id));
    }

    public BotCapability get(String botId, String id) {
        return listByBot(botId).stream()
                .filter(item -> id.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bot capability not found: " + id));
    }

    private void clearPrimary(String botId) {
        for (BotCapability item : listByBot(botId)) {
            if (!item.primaryCapability()) {
                continue;
            }
            persist(new BotCapability(
                    item.id(),
                    item.tenantId(),
                    item.botId(),
                    item.capabilityType(),
                    item.capabilityId(),
                    item.capabilityCode(),
                    item.routingKeywords(),
                    false,
                    item.enabled(),
                    item.createdAt(),
                    Instant.now()
            ), false);
        }
    }

    private void persist(BotCapability capability, boolean insert) {
        BotCapabilityMapper mapper = mapperProvider.getIfAvailable();
        if (mapper == null) {
            memory.removeIf(item -> capability.id().equals(item.id()));
            memory.add(capability);
            return;
        }
        BotCapabilityEntity entity = toEntity(capability);
        if (insert) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
    }

    private BotCapabilityEntity toEntity(BotCapability capability) {
        BotCapabilityEntity entity = new BotCapabilityEntity();
        entity.setId(capability.id());
        entity.setTenantId(capability.tenantId());
        entity.setBotId(capability.botId());
        entity.setCapabilityType(capability.capabilityType());
        entity.setCapabilityId(capability.capabilityId());
        entity.setCapabilityCode(capability.capabilityCode());
        entity.setRoutingKeywords(capability.routingKeywords());
        entity.setPrimaryCapability(capability.primaryCapability());
        entity.setEnabled(capability.enabled());
        entity.setCreatedAt(capability.createdAt());
        entity.setUpdatedAt(capability.updatedAt());
        return entity;
    }

    private BotCapability toDomain(BotCapabilityEntity entity) {
        return new BotCapability(
                entity.getId(),
                entity.getTenantId(),
                entity.getBotId(),
                entity.getCapabilityType(),
                entity.getCapabilityId(),
                entity.getCapabilityCode(),
                entity.getRoutingKeywords(),
                Boolean.TRUE.equals(entity.getPrimaryCapability()),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
