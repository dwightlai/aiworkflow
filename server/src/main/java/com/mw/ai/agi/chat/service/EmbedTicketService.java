package com.mw.ai.agi.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.auth.service.AuthService;
import com.mw.ai.agi.auth.service.AuthTokenResponse;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.auth.service.TenantContext;
import com.mw.ai.agi.chat.persistence.EmbedTicketEntity;
import com.mw.ai.agi.chat.persistence.EmbedTicketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmbedTicketService {
    private final ObjectProvider<EmbedTicketMapper> mapperProvider;
    private final ObjectMapper objectMapper;
    private final AgentAuditService agentAuditService;
    private final AuthService authService;
    private final Map<String, EmbedTicketEntity> memoryTickets = new ConcurrentHashMap<>();

    public EmbedTicketService(
            ObjectProvider<EmbedTicketMapper> mapperProvider,
            ObjectMapper objectMapper,
            AgentAuditService agentAuditService,
            AuthService authService
    ) {
        this.mapperProvider = mapperProvider;
        this.objectMapper = objectMapper;
        this.agentAuditService = agentAuditService;
        this.authService = authService;
    }

    public EmbedTicketResult issue(
            RuntimeIdentityContext identity,
            String tenantId,
            String userId,
            String botId,
            int expireSeconds,
            Map<String, Object> businessContext
    ) {
        int effectiveExpire = expireSeconds <= 0 ? 300 : expireSeconds;
        Instant now = Instant.now();
        String ticketValue = "tkt_" + UUID.randomUUID().toString().replace("-", "");
        EmbedTicketEntity entity = new EmbedTicketEntity();
        entity.setId("etk_" + UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setBotId(botId);
        entity.setTicket(ticketValue);
        entity.setStatus("PENDING");
        entity.setExpireAt(now.plusSeconds(effectiveExpire));
        entity.setBusinessContext(writeJson(businessContext));
        entity.setSourceAppId(identity.appId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        save(entity);
        agentAuditService.log(userId, botId, null, null, null, null, "EMBED_TICKET_ISSUED",
                "botId=" + botId, null, "SUCCESS", null, ticketValue);
        return new EmbedTicketResult(ticketValue, entity.getExpireAt());
    }

    public AuthTokenResponse exchange(String ticket, String botId) {
        try {
            EmbedTicketEntity entity = findByTicket(ticket);
            if (!"PENDING".equals(entity.getStatus())) {
                throw new IllegalStateException("Ticket is not available");
            }
            if (Instant.now().isAfter(entity.getExpireAt())) {
                entity.setStatus("EXPIRED");
                save(entity);
                throw new IllegalStateException("Ticket expired");
            }
            if (!entity.getBotId().equals(botId)) {
                throw new IllegalArgumentException("Ticket bot mismatch");
            }
            entity.setStatus("USED");
            entity.setUsedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            save(entity);
            TenantContext.set(entity.getTenantId());
            AuthTokenResponse token = authService.issueTokenForUser(entity.getUserId());
            agentAuditService.log(entity.getUserId(), botId, null, null, null, null, "EMBED_TICKET_USED",
                    "botId=" + botId, null, "SUCCESS", null, ticket);
            return token;
        } catch (RuntimeException ex) {
            agentAuditService.log(null, botId, null, null, null, null, "EMBED_TICKET_FAILED",
                    "ticket=" + ticket, null, "FAILED", ex.getMessage(), ticket);
            throw ex;
        }
    }

    private EmbedTicketEntity findByTicket(String ticket) {
        EmbedTicketMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            EmbedTicketEntity entity = mapper.selectOne(new LambdaQueryWrapper<EmbedTicketEntity>()
                    .eq(EmbedTicketEntity::getTicket, ticket));
            if (entity == null) {
                throw new IllegalArgumentException("Ticket not found");
            }
            return entity;
        }
        return memoryTickets.values().stream()
                .filter(item -> ticket.equals(item.getTicket()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    private void save(EmbedTicketEntity entity) {
        EmbedTicketMapper mapper = mapperProvider.getIfAvailable();
        if (mapper != null) {
            if (mapper.selectById(entity.getId()) == null) {
                mapper.insert(entity);
            } else {
                mapper.updateById(entity);
            }
        } else {
            memoryTickets.put(entity.getId(), entity);
        }
    }

    private String writeJson(Map<String, Object> businessContext) {
        try {
            return objectMapper.writeValueAsString(businessContext == null ? Map.of() : businessContext);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public record EmbedTicketResult(String ticket, Instant expireAt) {
    }
}
