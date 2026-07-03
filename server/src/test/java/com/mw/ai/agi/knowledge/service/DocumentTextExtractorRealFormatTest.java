package com.mw.ai.agi.knowledge.service;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextExtractorRealFormatTest {
    private static final Path SAMPLES = Path.of(
            "..", "sampledata", "chunkSample", "rag_chunking_real_format_samples"
    );
    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    @Test
    void preservesDocxHeadingsTablesAndFootnoteEvidence() throws Exception {
        String text = extract("docx/02_软件服务采购合同_脚注例外与付款节点.docx");

        assertThat(text)
                .contains("# ")
                .contains("|")
                .contains("40%")
                .contains("试运行完成并通过初验")
                .contains("不适用前述删除期限");
    }

    @Test
    void preservesXlsxSheetHeadersAndCompleteRows() throws Exception {
        String text = extract("xlsx/01_设备采购台账_多工作表同字段.xlsx");

        assertThat(text)
                .contains("# 工作表：")
                .contains("|")
                .contains("OCR 识别授权")
                .contains("待审批");
    }

    @Test
    void extractsPdfBodyWhileRemovingRepeatedPageNoise() throws Exception {
        String text = extract("pdf/01_扫描PDF_OCR页眉页脚噪声.pdf");

        assertThat(text)
                .contains("0.85")
                .doesNotContainPattern("(?s)(企业档案知识库测试材料.*){2,}");
    }

    private String extract(String relativePath) throws Exception {
        Path path = SAMPLES.resolve(relativePath);
        try (InputStream input = Files.newInputStream(path)) {
            return extractor.extract(path.getFileName().toString(), null, input);
        }
    }
}
