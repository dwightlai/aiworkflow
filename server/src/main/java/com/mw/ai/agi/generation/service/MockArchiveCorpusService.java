package com.mw.ai.agi.generation.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MockArchiveCorpusService implements ArchiveCorpusService {
    public List<Map<String, Object>> listThemeLibraries() {
        return List.of(
                Map.of(
                        "id", "theme_001",
                        "name", "电子文件归档管理主题库",
                        "description", "收录电子文件归档相关政策、通知与制度资料",
                        "itemCount", 2
                ),
                Map.of(
                        "id", "theme_002",
                        "name", "档案数字化主题库",
                        "description", "收录档案数字化建设相关档案条目",
                        "itemCount", 1
                )
        );
    }

    public List<Map<String, Object>> listThemeLibraryItems(String themeLibraryId) {
        if ("theme_002".equals(themeLibraryId)) {
            return List.of(archiveItem("archive_002", "档案数字化建设方案", "A002-2023-0001", "2023-06-15"));
        }
        return List.of(
                archiveItem("archive_001", "关于加强电子文件归档管理的通知", "A001-2024-0001", "2024-01-12"),
                archiveItem("archive_003", "电子文件归档管理暂行办法", "A001-2022-0008", "2022-11-03")
        );
    }

    public Map<String, Object> getArchiveItem(String archiveItemId) {
        return switch (archiveItemId) {
            case "archive_002" -> fullArchiveItem(
                    "archive_002",
                    "档案数字化建设方案",
                    "A002-2023-0001",
                    "2023-06-15",
                    "某单位",
                    "提出档案数字化建设目标、实施步骤和保障措施。"
            );
            case "archive_003" -> fullArchiveItem(
                    "archive_003",
                    "电子文件归档管理暂行办法",
                    "A001-2022-0008",
                    "2022-11-03",
                    "档案管理部门",
                    "明确电子文件归档范围、归档流程和质量要求。"
            );
            default -> fullArchiveItem(
                    "archive_001",
                    "关于加强电子文件归档管理的通知",
                    "A001-2024-0001",
                    "2024-01-12",
                    "办公室",
                    "2024年1月12日，办公室发布关于加强电子文件归档管理的通知，要求各单位规范电子文件收集、整理和归档。"
            );
        };
    }

    public List<Map<String, Object>> loadCorpusItems(String themeLibraryId) {
        return listThemeLibraryItems(themeLibraryId).stream()
                .map(item -> getArchiveItem(String.valueOf(item.get("id"))))
                .toList();
    }

    public Map<String, Object> loadCorpusBundle(String themeLibraryId) {
        List<Map<String, Object>> items = loadCorpusItems(themeLibraryId);
        return Map.of(
                "themeLibraryId", themeLibraryId,
                "items", items,
                "summary", buildCorpusSummary(items),
                "itemCount", items.size()
        );
    }

    private String buildCorpusSummary(List<Map<String, Object>> corpusItems) {
        if (corpusItems.isEmpty()) {
            return "暂无档案馆资料。";
        }
        StringBuilder builder = new StringBuilder();
        for (Map<String, Object> item : corpusItems) {
            builder.append("- ").append(String.valueOf(item.get("title")))
                    .append("：")
                    .append(String.valueOf(item.get("summary")))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private Map<String, Object> archiveItem(String id, String title, String archiveNo, String formedAt) {
        return Map.of(
                "id", id,
                "title", title,
                "archiveNo", archiveNo,
                "formedAt", formedAt
        );
    }

    private Map<String, Object> fullArchiveItem(
            String id,
            String title,
            String archiveNo,
            String formedAt,
            String responsibleParty,
            String summary
    ) {
        return Map.of(
                "sourceType", "ARCHIVE_ITEM",
                "sourceId", id,
                "title", title,
                "summary", summary,
                "contentText", summary,
                "metadata", Map.of(
                        "档号", archiveNo,
                        "责任者", responsibleParty,
                        "形成时间", formedAt
                ),
                "citation", Map.of(
                        "title", title,
                        "locator", "档号 " + archiveNo
                )
        );
    }
}
