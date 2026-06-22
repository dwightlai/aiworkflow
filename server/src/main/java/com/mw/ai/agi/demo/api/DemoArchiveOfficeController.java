package com.mw.ai.agi.demo.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo/archive")
public class DemoArchiveOfficeController {

    @GetMapping("/borrow")
    public Map<String, Object> borrow(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String archiveNo
    ) {
        String query = keyword == null || keyword.isBlank() ? "档案借阅" : keyword.trim();
        String no = archiveNo == null || archiveNo.isBlank() ? "DA-2024-0186" : archiveNo.trim();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene", "档案借阅");
        result.put("archiveNo", no);
        result.put("keyword", query);
        result.put("answer", """
                【档案借阅】已受理您的咨询

                档号：%s
                用户诉求：%s

                办理指引：
                1. 持工作证/介绍信至档案室前台提交《档案借阅申请表》
                2. 原件借阅限馆内查阅，一般不得外借；复印件需登记用途并加盖档案章
                3. 涉密及未开放档案须按审批权限逐级报批

                当前模拟状态：待审批（预计 1 个工作日）
                可借形态：复印件 / 馆内查阅
                责任部门：综合档案室
                """.formatted(no, query).trim());
        result.put("status", "PENDING_APPROVAL");
        result.put("allowedForms", List.of("馆内查阅", "复印件"));
        result.put("source", "archive_office.borrow_apply");
        return result;
    }

    @GetMapping("/utilize")
    public Map<String, Object> utilize(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String purpose
    ) {
        String query = keyword == null || keyword.isBlank() ? "档案利用" : keyword.trim();
        String usePurpose = purpose == null || purpose.isBlank() ? "编史修志" : purpose.trim();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene", "档案利用");
        result.put("keyword", query);
        result.put("purpose", usePurpose);
        result.put("answer", """
                【档案利用】已匹配利用服务

                利用诉求：%s
                利用目的：%s

                服务说明：
                1. 开放档案可通过查档机自助检索，或向接待窗口申请调阅
                2. 支持摘录、复制（按页计费）、缩微胶片阅览；数字化副本需单独申请
                3. 利用活动须填写《档案利用登记表》，遵守著作权与隐私保护规定

                阅览室时间：工作日 9:00-17:00
                今日接待量（模拟）：23 人次
                推荐服务：专题汇编查阅 / 口述史料采集辅助
                """.formatted(query, usePurpose).trim());
        result.put("readingRoomHours", "工作日 9:00-17:00");
        result.put("todayVisitors", 23);
        result.put("source", "archive_office.utilize_query");
        return result;
    }

    @GetMapping("/files")
    public Map<String, Object> queryFiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String archiveNo,
            @RequestParam(required = false) String title) {
        String query = firstNonBlank(keyword, title, archiveNo, "档案查询");
        List<Map<String, Object>> allFiles = List.of(
                file("DA-2024-0186", "2024年度信息化建设合同", "文书档案", "综合部", "2019-03", "开放"),
                file("DA-2023-0452", "党组会议纪要汇编（第3册）", "会议档案", "办公室", "2023-06", "开放"),
                file("DA-2022-1098", "人事任免及考核材料", "人事档案", "人事处", "2022-11", "控制"),
                file("DA-2021-0773", "重大工程竣工图纸", "基建档案", "基建科", "2021-08", "开放")
        );
        String searchKeyword = keyword != null ? keyword.trim() : "";
        String searchArchiveNo = archiveNo != null ? archiveNo.trim() : "";
        String searchTitle = title != null ? title.trim() : "";

        // 尝试从 keyword 中提取档号格式
        if (searchArchiveNo.isEmpty() && !searchKeyword.isEmpty()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("DA-\\d{4}-\\d{4}").matcher(searchKeyword);
            if (m.find()) {
                searchArchiveNo = m.group();
            }
        }

        final String filterArchiveNo = searchArchiveNo;
        final String filterTitle = searchTitle;
        final String filterKeyword = searchKeyword;
        List<Map<String, Object>> files = allFiles.stream()
                .filter(f -> {
                    if (!filterArchiveNo.isEmpty() && !filterArchiveNo.equals(f.get("archiveNo"))) {
                        return false;
                    }
                    if (!filterTitle.isEmpty() && !String.valueOf(f.get("title")).contains(filterTitle)) {
                        return false;
                    }
                    if (filterArchiveNo.isEmpty() && filterTitle.isEmpty() && !filterKeyword.isEmpty()) {
                        String fArchiveNo = String.valueOf(f.get("archiveNo"));
                        String fTitle = String.valueOf(f.get("title"));
                        return fArchiveNo.contains(filterKeyword) || fTitle.contains(filterKeyword);
                    }
                    return true;
                })
                .toList();
        StringBuilder answerBody = new StringBuilder();
        if (files.isEmpty()) {
            answerBody.append("未检索到匹配的档案记录。\n\n检索条件：").append(query);
        } else {
            answerBody.append("已检索到 ").append(files.size()).append(" 条相关记录\n\n检索条件：").append(query).append("\n\n");
            int idx = 1;
            for (Map<String, Object> f : files) {
                answerBody.append(idx++).append(". ")
                        .append(f.get("archiveNo")).append("｜")
                        .append(f.get("title")).append("｜")
                        .append(f.get("category")).append("｜")
                        .append(f.get("department")).append("｜")
                        .append(f.get("formedAt")).append("｜")
                        .append(f.get("accessLevel")).append("\n");
            }
            answerBody.append("\n说明：开放档案可自助查档；控制档案需履行审批手续后调阅。");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene", "档案文件查询");
        result.put("keyword", query);
        result.put("total", files.size());
        result.put("files", files);
        result.put("answer", answerBody.toString().trim());
        result.put("source", "archive_office.file_query");
        return result;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "档案查询";
    }

    private static Map<String, Object> file(
            String archiveNo,
            String title,
            String category,
            String department,
            String formedAt,
            String accessLevel
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("archiveNo", archiveNo);
        item.put("title", title);
        item.put("category", category);
        item.put("department", department);
        item.put("formedAt", formedAt);
        item.put("accessLevel", accessLevel);
        return item;
    }
}
