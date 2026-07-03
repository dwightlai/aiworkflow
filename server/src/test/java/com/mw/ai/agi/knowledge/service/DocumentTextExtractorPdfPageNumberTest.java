package com.mw.ai.agi.knowledge.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextExtractorPdfPageNumberTest {

    @Test
    void removesChangingNumericPageFootersButPreservesBodyNumbers() throws Exception {
        byte[] pdf = pdfWithNumericFooters();

        String text = new DocumentTextExtractor().extract(
                "numbered.pdf",
                "application/pdf",
                new ByteArrayInputStream(pdf)
        );

        assertThat(text)
                .contains("Retention period is 4 years.")
                .contains("Section 6.4 body content on page two.")
                .contains("Section 7.1 body content on page three.")
                .doesNotContainPattern("(?m)^\\s*(16|17|18)\\s*$");
    }

    private byte[] pdfWithNumericFooters() throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (int index = 0; index < 3; index++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 12);
                    content.newLineAtOffset(72, 700);
                    content.showText(switch (index) {
                        case 0 -> "Retention period is 4 years.";
                        case 1 -> "Section 6.4 body content on page two.";
                        default -> "Section 7.1 body content on page three.";
                    });
                    content.endText();

                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 10);
                    content.newLineAtOffset(300, 30);
                    content.showText(String.valueOf(16 + index));
                    content.endText();
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
