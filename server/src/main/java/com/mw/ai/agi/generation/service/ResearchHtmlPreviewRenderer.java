package com.mw.ai.agi.generation.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ResearchHtmlPreviewRenderer {
    @SuppressWarnings("unchecked")
    public String render(Map<String, Object> contentJson) {
        String title = stringValue(contentJson.get("title"), "编研成果");
        String category = stringValue(contentJson.get("templateCategory"), "report");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"/>");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>");
        html.append("<title>").append(escape(title)).append("</title>");
        html.append("<style>");
        html.append("body{font-family:'Microsoft YaHei',sans-serif;margin:0;background:#f5f6f8;color:#1f2937;}");
        html.append(".page{max-width:960px;margin:0 auto;padding:32px 20px 48px;}");
        html.append(".cover{background:#fff;border-radius:12px;padding:48px 32px;text-align:center;box-shadow:0 8px 24px rgba(15,23,42,.08);margin-bottom:24px;}");
        html.append(".cover h1{margin:0 0 12px;font-size:32px;}");
        html.append(".section{background:#fff;border-radius:12px;padding:24px;margin-bottom:16px;box-shadow:0 4px 16px rgba(15,23,42,.06);}");
        html.append(".section h2{margin:0 0 16px;font-size:22px;border-left:4px solid #2563eb;padding-left:12px;}");
        html.append(".timeline{border-left:3px solid #cbd5e1;padding-left:18px;}");
        html.append(".timeline-item{margin-bottom:16px;}");
        html.append(".timeline-date{font-weight:700;color:#2563eb;}");
        html.append(".gallery-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px;}");
        html.append(".gallery-card{border:1px solid #e5e7eb;border-radius:10px;padding:16px;background:#fafafa;}");
        html.append(".refs{font-size:14px;color:#4b5563;}");
        html.append("</style></head><body><div class=\"page\">");
        html.append("<section class=\"cover\"><h1>").append(escape(title)).append("</h1>");
        html.append("<p>").append(categoryLabel(category)).append("</p></section>");

        Object sections = contentJson.get("sections");
        if (sections instanceof List<?> sectionItems) {
            for (Object item : sectionItems) {
                if (!(item instanceof Map<?, ?> section)) {
                    continue;
                }
                renderSection(html, (Map<String, Object>) section);
            }
        }

        Object references = contentJson.get("references");
        if (references instanceof List<?> refItems && !refItems.isEmpty()) {
            html.append("<section class=\"section\"><h2>参考文献 / 资料来源</h2><div class=\"refs\">");
            for (Object item : refItems) {
                if (item instanceof Map<?, ?> ref) {
                    html.append("<p>[")
                            .append(escape(String.valueOf(ref.get("refNo"))))
                            .append("] ")
                            .append(escape(stringValue(ref.get("sourceTitle"), "")))
                            .append("</p>");
                }
            }
            html.append("</div></section>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private void renderSection(StringBuilder html, Map<String, Object> section) {
        String type = stringValue(section.get("type"), "text");
        html.append("<section class=\"section\"><h2>").append(escape(stringValue(section.get("title"), ""))).append("</h2>");
        if ("timeline".equals(type)) {
            html.append("<div class=\"timeline\">");
            Object events = section.get("events");
            if (events instanceof List<?> eventItems) {
                for (Object item : eventItems) {
                    if (item instanceof Map<?, ?> event) {
                        html.append("<div class=\"timeline-item\">");
                        String date = stringValue(event.get("date"), "");
                        if (!date.isBlank()) {
                            html.append("<div class=\"timeline-date\">").append(escape(date)).append("</div>");
                        }
                        html.append("<div>").append(escape(stringValue(event.get("description"), stringValue(event.get("title"), "")))).append("</div>");
                        html.append("</div>");
                    }
                }
            }
            html.append("</div>");
        } else if ("gallery".equals(type)) {
            html.append("<div class=\"gallery-grid\">");
            Object items = section.get("items");
            if (items instanceof List<?> galleryItems) {
                for (Object item : galleryItems) {
                    if (item instanceof Map<?, ?> galleryItem) {
                        html.append("<article class=\"gallery-card\">");
                        html.append("<h3>").append(escape(stringValue(galleryItem.get("title"), "展品"))).append("</h3>");
                        String archiveCode = stringValue(galleryItem.get("archiveCode"), "");
                        if (!archiveCode.isBlank()) {
                            html.append("<p>档号：").append(escape(archiveCode)).append("</p>");
                        }
                        html.append("<p>").append(escape(stringValue(galleryItem.get("description"), ""))).append("</p>");
                        html.append("</article>");
                    }
                }
            }
            html.append("</div>");
        } else {
            html.append("<p>").append(escape(stringValue(section.get("content"), "")).replace("\n", "<br/>")).append("</p>");
        }
        html.append("</section>");
    }

    private String categoryLabel(String category) {
        return switch (category) {
            case "gallery" -> "图文展陈成果";
            case "timeline" -> "时间轴专题成果";
            default -> "普通编研报告";
        };
    }

    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
