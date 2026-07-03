package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeChunk;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Business500DatasetRegressionTest {
    private static final Path ROOT = Path.of(
            "..", "sampledata", "chunkSample", "business_chunking_dataset_500"
    );

    @Test
    void runsFullParsingChunkingAndExpandedRecallRegression() throws Exception {
        List<Map<String, String>> manifest = readCsv(ROOT.resolve("metadata/dataset_manifest.csv"));
        List<Map<String, String>> questions = readCsv(ROOT.resolve("metadata/chunking_test_questions.csv"));
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), store);
        KnowledgeBase base = service.create(
                "business-500-regression",
                null,
                null,
                null,
                null,
                "STRUCTURE_AWARE",
                500,
                50,
                "KEYWORD",
                5
        );

        List<String> parseFailures = new ArrayList<>();
        Map<String, Integer> successesByFormat = new LinkedHashMap<>();
        long startedAt = System.nanoTime();
        for (Map<String, String> row : manifest) {
            String relativePath = row.get("file_path");
            Path path = ROOT.resolve(relativePath);
            try (InputStream input = Files.newInputStream(path)) {
                String content = extractor.extract(path.getFileName().toString(), null, input);
                service.addDocument(
                        base.id(),
                        path.getFileName().toString(),
                        content,
                        "STRUCTURE_AWARE",
                        500,
                        50
                );
                successesByFormat.merge(row.get("file_type"), 1, Integer::sum);
            } catch (Exception exception) {
                parseFailures.add(relativePath + ": " + exception.getMessage());
            }
        }

        List<KnowledgeChunk> allChunks = store.listChunks(base.id());
        List<KnowledgeChunk> children = allChunks.stream()
                .filter(chunk -> "CHILD".equalsIgnoreCase(chunk.chunkLevel()))
                .toList();
        long parentLinked = children.stream()
                .filter(chunk -> chunk.parentChunkId() != null && !chunk.parentChunkId().isBlank())
                .count();
        long ordinaryOverMax = children.stream()
                .filter(chunk -> !chunk.metadataJson().contains("\"atomic\":true"))
                .filter(chunk -> chunk.tokenEstimate() > 500)
                .count();

        List<String> recallFailures = new ArrayList<>();
        int recallHits = 0;
        int exactSourceHits = 0;
        for (Map<String, String> question : questions) {
            List<KnowledgeSearchResult> results = service.search(base.id(), question.get("question"), 5);
            String expectedFile = Path.of(question.get("file_path")).getFileName().toString();
            boolean sourceHit = results.stream().anyMatch(result -> expectedFile.equals(result.documentName()));
            if (sourceHit) {
                exactSourceHits++;
            }
            String evidence = results.stream()
                    .map(KnowledgeSearchResult::content)
                    .reduce("", (left, right) -> left + "\n" + right);
            double answerCoverage = coverage(evidence, question.get("expected_evidence"));
            if (answerCoverage >= 0.30) {
                recallHits++;
            } else {
                recallFailures.add(
                        question.get("id") + " " + question.get("question")
                                + " [sourceHit=" + sourceHit
                                + ", coverage=" + String.format(Locale.ROOT, "%.2f", answerCoverage) + "]"
                );
            }
        }

        double parseSuccessRate = ratio(manifest.size() - parseFailures.size(), manifest.size());
        double parentCoverageRate = ratio(parentLinked, children.size());
        double expandedRecallAt5 = ratio(recallHits, questions.size());
        double elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;

        System.out.printf(
                Locale.ROOT,
                "REGRESSION_500_RESULT documents=%d parsed=%d parseSuccess=%.2f%% "
                        + "docx=%d xlsx=%d pdf=%d children=%d parents=%d "
                        + "parentCoverage=%.2f%% ordinaryOverMax=%d questions=%d hits=%d "
                        + "expandedRecallAt5=%.2f%% exactSourceHitAt5=%.2f%% elapsedSeconds=%.2f%n",
                manifest.size(),
                manifest.size() - parseFailures.size(),
                parseSuccessRate * 100,
                successesByFormat.getOrDefault("docx", 0),
                successesByFormat.getOrDefault("xlsx", 0),
                successesByFormat.getOrDefault("pdf", 0),
                children.size(),
                allChunks.size() - children.size(),
                parentCoverageRate * 100,
                ordinaryOverMax,
                questions.size(),
                recallHits,
                expandedRecallAt5 * 100,
                ratio(exactSourceHits, questions.size()) * 100,
                elapsedSeconds
        );
        if (!parseFailures.isEmpty()) {
            System.out.println("REGRESSION_500_PARSE_FAILURES=" + parseFailures);
        }
        if (!recallFailures.isEmpty()) {
            System.out.println("REGRESSION_500_RECALL_FAILURES=" + recallFailures);
        }

        assertThat(parseFailures).as("All 500 business documents must parse").isEmpty();
        assertThat(parseSuccessRate).isEqualTo(1.0);
        assertThat(parentCoverageRate).isGreaterThanOrEqualTo(0.95);
        assertThat(ordinaryOverMax).isZero();
        assertThat(expandedRecallAt5)
                .withFailMessage("Recall misses: %s", recallFailures)
                .isGreaterThanOrEqualTo(0.90);
    }

    private double coverage(String evidence, String expected) {
        String normalizedEvidence = compact(evidence);
        Set<String> units = new LinkedHashSet<>();
        java.util.regex.Matcher values = java.util.regex.Pattern
                .compile("[a-z0-9][a-z0-9_.%≥≤-]*", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(expected == null ? "" : expected);
        while (values.find()) {
            units.add(values.group().toLowerCase(Locale.ROOT));
        }
        String cjk = (expected == null ? "" : expected).replaceAll("[^\\p{IsHan}]", "");
        for (int index = 0; index < cjk.length() - 1; index++) {
            units.add(cjk.substring(index, index + 2));
        }
        if (units.isEmpty()) {
            return normalizedEvidence.contains(compact(expected)) ? 1 : 0;
        }
        long matches = units.stream().filter(normalizedEvidence::contains).count();
        return ratio(matches, units.size());
    }

    private List<Map<String, String>> readCsv(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path);
        List<String> headers = parseCsvLine(lines.get(0)).stream()
                .map(value -> value.replace("\uFEFF", ""))
                .toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            List<String> values = parseCsvLine(lines.get(lineIndex));
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < headers.size(); column++) {
                row.put(headers.get(column), column < values.size() ? values.get(column) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String compact(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }
}
