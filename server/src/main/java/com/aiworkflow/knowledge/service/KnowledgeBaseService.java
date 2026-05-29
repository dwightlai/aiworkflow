package com.aiworkflow.knowledge.service;

import com.aiworkflow.knowledge.domain.KnowledgeBase;
import com.aiworkflow.knowledge.domain.KnowledgeChunk;
import com.aiworkflow.knowledge.domain.KnowledgeDocument;
import com.aiworkflow.knowledge.domain.KnowledgeSearchResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class KnowledgeBaseService {
    private static final int CHUNK_SIZE = 500;

    private final List<KnowledgeBase> knowledgeBases = new CopyOnWriteArrayList<>();
    private final List<KnowledgeDocument> documents = new CopyOnWriteArrayList<>();
    private final List<KnowledgeChunk> chunks = new CopyOnWriteArrayList<>();

    public KnowledgeBase create(String name, String description) {
        Instant now = Instant.now();
        KnowledgeBase knowledgeBase = new KnowledgeBase(
                "kb_" + UUID.randomUUID(),
                name,
                description,
                0,
                0,
                now,
                now
        );
        knowledgeBases.add(knowledgeBase);
        return knowledgeBase;
    }

    public List<KnowledgeBase> list() {
        return new ArrayList<>(knowledgeBases);
    }

    public KnowledgeDocument addDocument(String knowledgeBaseId, String name, String content) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        List<String> chunkContents = splitContent(content);
        KnowledgeDocument document = new KnowledgeDocument(
                "doc_" + UUID.randomUUID(),
                knowledgeBaseId,
                name,
                chunkContents.size(),
                Instant.now()
        );
        documents.add(document);
        for (int index = 0; index < chunkContents.size(); index += 1) {
            chunks.add(new KnowledgeChunk(
                    "chunk_" + UUID.randomUUID(),
                    knowledgeBaseId,
                    document.id(),
                    document.name(),
                    chunkContents.get(index),
                    index
            ));
        }
        refreshKnowledgeBaseStats(knowledgeBaseId);
        return document;
    }

    public List<KnowledgeDocument> listDocuments(String knowledgeBaseId) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        return documents.stream()
                .filter(document -> document.knowledgeBaseId().equals(knowledgeBaseId))
                .toList();
    }

    public List<KnowledgeSearchResult> search(String knowledgeBaseId, String query, int topK) {
        ensureKnowledgeBaseExists(knowledgeBaseId);
        Set<String> terms = tokenize(query);
        int limit = topK <= 0 ? 3 : topK;
        return chunks.stream()
                .filter(chunk -> chunk.knowledgeBaseId().equals(knowledgeBaseId))
                .map(chunk -> new KnowledgeSearchResult(
                        chunk.id(),
                        chunk.documentName(),
                        chunk.content(),
                        score(chunk.content(), terms)
                ))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(KnowledgeSearchResult::score).reversed())
                .limit(limit)
                .toList();
    }

    private void ensureKnowledgeBaseExists(String knowledgeBaseId) {
        boolean exists = knowledgeBases.stream().anyMatch(knowledgeBase -> knowledgeBase.id().equals(knowledgeBaseId));
        if (!exists) {
            throw new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId);
        }
    }

    private void refreshKnowledgeBaseStats(String knowledgeBaseId) {
        for (int index = 0; index < knowledgeBases.size(); index += 1) {
            KnowledgeBase current = knowledgeBases.get(index);
            if (current.id().equals(knowledgeBaseId)) {
                int documentCount = (int) documents.stream()
                        .filter(document -> document.knowledgeBaseId().equals(knowledgeBaseId))
                        .count();
                int chunkCount = (int) chunks.stream()
                        .filter(chunk -> chunk.knowledgeBaseId().equals(knowledgeBaseId))
                        .count();
                knowledgeBases.set(index, new KnowledgeBase(
                        current.id(),
                        current.name(),
                        current.description(),
                        documentCount,
                        chunkCount,
                        current.createdAt(),
                        Instant.now()
                ));
                return;
            }
        }
    }

    private List<String> splitContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String paragraph : normalized.split("\\R{2,}")) {
            String trimmed = paragraph.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            for (int start = 0; start < trimmed.length(); start += CHUNK_SIZE) {
                result.add(trimmed.substring(start, Math.min(start + CHUNK_SIZE, trimmed.length())));
            }
        }
        return result.isEmpty() ? List.of(normalized) : result;
    }

    private Set<String> tokenize(String query) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        Set<String> terms = new LinkedHashSet<>();
        for (String term : normalized.split("[\\s,，。；;:：]+")) {
            if (!term.isBlank()) {
                terms.add(term);
            }
        }
        normalized.codePoints()
                .filter(Character::isLetterOrDigit)
                .mapToObj(Character::toString)
                .forEach(terms::add);
        return terms;
    }

    private int score(String content, Set<String> terms) {
        String normalized = content.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) {
            if (!term.isBlank() && normalized.contains(term)) {
                score += term.length() > 1 ? 2 : 1;
            }
        }
        return score;
    }
}
