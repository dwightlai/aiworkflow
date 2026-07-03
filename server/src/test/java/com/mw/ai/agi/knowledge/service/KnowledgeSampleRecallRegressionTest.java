package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeBase;
import com.mw.ai.agi.knowledge.domain.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSampleRecallRegressionTest {
    private static final Path ROOT = Path.of("..", "sampledata", "chunkSample");
    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    @Test
    void baseAlgorithmQuestionsReachExpandedRecallTarget() throws Exception {
        Path samples = ROOT.resolve("rag_chunking_samples");
        List<Question> questions = readQuestions(
                samples.resolve("14_chunking_test_questions.csv"),
                "source_file",
                "question",
                "expected_answer"
        );
        RecallReport report = runRegression(samples, questions);
        assertThat(report.recall()).withFailMessage("Missed questions: %s", report.misses())
                .isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void realFormatQuestionsReachExpandedRecallTarget() throws Exception {
        Path samples = ROOT.resolve("rag_chunking_real_format_samples");
        List<Question> questions = readQuestions(
                samples.resolve("metadata/real_format_test_questions.csv"),
                "file",
                "question",
                "expected_evidence"
        );
        RecallReport report = runRegression(samples, questions);
        assertThat(report.recall()).withFailMessage("Missed questions: %s", report.misses())
                .isGreaterThanOrEqualTo(0.90);
    }

    private RecallReport runRegression(Path samples, List<Question> questions) throws Exception {
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeBaseService service = new KnowledgeBaseService(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), store);
        KnowledgeBase base = service.create(
                "regression",
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
        for (String file : questions.stream().map(Question::file).distinct().toList()) {
            Path path = samples.resolve(file);
            String content;
            try (InputStream input = Files.newInputStream(path)) {
                content = extractor.extract(path.getFileName().toString(), null, input);
            }
            service.addDocument(base.id(), path.getFileName().toString(), content, "STRUCTURE_AWARE", 500, 50);
        }
        int hits = 0;
        List<String> misses = new ArrayList<>();
        for (Question question : questions) {
            List<KnowledgeSearchResult> results = service.search(base.id(), question.question(), 5);
            String evidence = results.stream()
                    .map(KnowledgeSearchResult::content)
                    .reduce("", (left, right) -> left + "\n" + right);
            boolean sourceHit = results.stream().anyMatch(result ->
                    result.documentName().equals(Path.of(question.file()).getFileName().toString()));
            if (sourceHit && coverage(evidence, question.expected()) >= 0.30) {
                hits++;
            } else {
                misses.add(question.question());
            }
        }
        return new RecallReport(questions.isEmpty() ? 0 : (double) hits / questions.size(), misses);
    }

    private double coverage(String evidence, String expected) {
        String normalizedEvidence = compact(evidence);
        java.util.Set<String> units = new java.util.LinkedHashSet<>();
        java.util.regex.Matcher values = java.util.regex.Pattern
                .compile("[a-z0-9][a-z0-9_.%≥≤-]*", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(expected);
        while (values.find()) {
            units.add(values.group().toLowerCase(Locale.ROOT));
        }
        String cjk = expected.replaceAll("[^\\p{IsHan}]", "");
        for (int index = 0; index < cjk.length() - 1; index++) {
            units.add(cjk.substring(index, index + 2));
        }
        if (units.isEmpty()) {
            return normalizedEvidence.contains(compact(expected)) ? 1 : 0;
        }
        long matches = units.stream().filter(normalizedEvidence::contains).count();
        return (double) matches / units.size();
    }

    private List<Question> readQuestions(Path path, String fileColumn, String questionColumn, String expectedColumn)
            throws Exception {
        List<String> lines = Files.readAllLines(path);
        List<String> headers = parseCsvLine(lines.get(0));
        int fileIndex = headers.indexOf(fileColumn);
        int questionIndex = headers.indexOf(questionColumn);
        int expectedIndex = headers.indexOf(expectedColumn);
        List<Question> result = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            List<String> values = parseCsvLine(lines.get(index));
            if (values.size() > Math.max(fileIndex, Math.max(questionIndex, expectedIndex))) {
                result.add(new Question(
                        values.get(fileIndex),
                        values.get(questionIndex),
                        values.get(expectedIndex)
                ));
            }
        }
        return result;
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

    private record Question(String file, String question, String expected) {
    }

    private record RecallReport(double recall, List<String> misses) {
    }
}
