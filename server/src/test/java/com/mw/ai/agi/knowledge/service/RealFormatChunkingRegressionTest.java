package com.mw.ai.agi.knowledge.service;

import com.mw.ai.agi.knowledge.domain.KnowledgeChunkPreview;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RealFormatChunkingRegressionTest {
    private static final Path SAMPLES = Path.of(
            "..", "sampledata", "chunkSample", "rag_chunking_real_format_samples"
    );
    private final DocumentTextExtractor extractor = new DocumentTextExtractor();
    private final KnowledgeDocumentSplitter splitter = new KnowledgeDocumentSplitter(new com.mw.ai.agi.knowledge.chunking.HeuristicTokenCounter(), new LocalEmbeddingClient());

    @Test
    void extractsAndChunksEveryPrimaryRealFormatSample() throws Exception {
        Map<String, String> evidenceByFile = Map.ofEntries(
                Map.entry("docx/01_员工差旅报销制度_条款引用与条件列表.docx", "一线城市 | 600 元"),
                Map.entry("docx/02_软件服务采购合同_脚注例外与付款节点.docx", "试运行完成并通过初验"),
                Map.entry("docx/03_项目会议纪要_多议题多负责人.docx", "2026年7月22日"),
                Map.entry("docx/04_知识库操作手册_流程步骤与截图说明.docx", "DOCX"),
                Map.entry("docx/05_档案材料汇编_卷件附件层级.docx", "知识库分块优化设计文档"),
                Map.entry("xlsx/01_设备采购台账_多工作表同字段.xlsx", "待审批"),
                Map.entry("xlsx/02_档案移交清单_案卷文件层级.xlsx", "AJ-001"),
                Map.entry("xlsx/03_OCR质量日报_置信度与处理意见.xlsx", "人工复核关键字段"),
                Map.entry("pdf/01_扫描PDF_OCR页眉页脚噪声.pdf", "0.85"),
                Map.entry("pdf/02_双栏PDF_阅读顺序混排.pdf", "混合检索"),
                Map.entry("pdf/03_PDF图表图注_正文引用图1.pdf", "文本抽取")
        );

        for (Map.Entry<String, String> sample : evidenceByFile.entrySet()) {
            Path path = SAMPLES.resolve(sample.getKey());
            String extracted;
            try (InputStream input = Files.newInputStream(path)) {
                extracted = extractor.extract(path.getFileName().toString(), null, input);
            }
            List<KnowledgeChunkPreview> chunks = splitter.splitDocument(
                    path.getFileName().toString(),
                    extracted,
                    new KnowledgeSplitRequest("STRUCTURE_AWARE", 500, 50, null)
            );

            assertThat(chunks).as(sample.getKey()).isNotEmpty();
            assertThat(chunks).allSatisfy(chunk -> {
                assertThat(chunk.parentChunkId()).isNotBlank();
                assertThat(chunk.embeddingContent()).contains(chunk.content());
            });
            String[] evidenceParts = java.util.Arrays.stream(sample.getValue().split("\\|"))
                    .map(this::compact)
                    .filter(value -> !value.isBlank())
                    .toArray(String[]::new);
            assertThat(chunks.stream().map(KnowledgeChunkPreview::content).toList())
                    .as(sample.getKey())
                    .anySatisfy(content -> assertThat(compact(content)).contains(evidenceParts));
        }
    }

    private String compact(String value) {
        return value == null ? "" : value.replaceAll("[\\s|]+", "");
    }
}
