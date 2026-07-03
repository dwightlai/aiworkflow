package com.mw.ai.agi.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("agi_knowledge_failure_sample")
public class KnowledgeFailureSampleEntity {
    @TableId
    private String id;
    private String tenantId;
    private String knowledgeBaseId;
    private String question;
    private String roleSnapshot;
    private String seedChunkIds;
    private String evidenceGroupJson;
    private String expectedEvidence;
    private String expectedAnswerPoints;
    private String failureFlagsJson;
    private String parserVersion;
    private String profileVersion;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getRoleSnapshot() { return roleSnapshot; }
    public void setRoleSnapshot(String roleSnapshot) { this.roleSnapshot = roleSnapshot; }
    public String getSeedChunkIds() { return seedChunkIds; }
    public void setSeedChunkIds(String seedChunkIds) { this.seedChunkIds = seedChunkIds; }
    public String getEvidenceGroupJson() { return evidenceGroupJson; }
    public void setEvidenceGroupJson(String evidenceGroupJson) { this.evidenceGroupJson = evidenceGroupJson; }
    public String getExpectedEvidence() { return expectedEvidence; }
    public void setExpectedEvidence(String expectedEvidence) { this.expectedEvidence = expectedEvidence; }
    public String getExpectedAnswerPoints() { return expectedAnswerPoints; }
    public void setExpectedAnswerPoints(String expectedAnswerPoints) { this.expectedAnswerPoints = expectedAnswerPoints; }
    public String getFailureFlagsJson() { return failureFlagsJson; }
    public void setFailureFlagsJson(String failureFlagsJson) { this.failureFlagsJson = failureFlagsJson; }
    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }
    public String getProfileVersion() { return profileVersion; }
    public void setProfileVersion(String profileVersion) { this.profileVersion = profileVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
