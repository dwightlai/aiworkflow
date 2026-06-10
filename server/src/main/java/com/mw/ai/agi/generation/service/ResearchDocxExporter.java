package com.mw.ai.agi.generation.service;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ResearchDocxExporter {
    public byte[] export(String title, String markdown) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (title != null && !title.isBlank()) {
                XWPFParagraph titleParagraph = document.createParagraph();
                titleParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun titleRun = titleParagraph.createRun();
                titleRun.setBold(true);
                titleRun.setFontSize(18);
                titleRun.setText(title);
            }
            for (String line : splitLines(markdown)) {
                if (line.isBlank()) {
                    document.createParagraph();
                    continue;
                }
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setFontSize(12);
                if (line.startsWith("# ")) {
                    run.setBold(true);
                    run.setFontSize(16);
                    run.setText(line.substring(2).trim());
                } else if (line.startsWith("## ")) {
                    run.setBold(true);
                    run.setFontSize(14);
                    run.setText(line.substring(3).trim());
                } else if (line.startsWith("### ")) {
                    run.setBold(true);
                    run.setText(line.substring(4).trim());
                } else if (line.startsWith("- ")) {
                    run.setText("• " + line.substring(2).trim());
                } else {
                    run.setText(line);
                }
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to export DOCX.", exception);
        }
    }

    private List<String> splitLines(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }
        return List.of(markdown.replace("\r\n", "\n").split("\n"));
    }
}
