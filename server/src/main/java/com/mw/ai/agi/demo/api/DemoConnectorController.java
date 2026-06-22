package com.mw.ai.agi.demo.api;

import com.mw.ai.agi.generation.service.MockArchiveCorpusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo/connector")
public class DemoConnectorController {
    private final MockArchiveCorpusService mockArchiveCorpusService;

    public DemoConnectorController(MockArchiveCorpusService mockArchiveCorpusService) {
        this.mockArchiveCorpusService = mockArchiveCorpusService;
    }

    @GetMapping("/greeting")
    public Map<String, Object> greeting(@RequestParam(required = false) String keyword) {
        String text = keyword == null || keyword.isBlank() ? "连接器" : keyword.trim();
        return Map.of(
                "answer", "【连接器演示】接口 greeting 已调用，收到输入：" + text,
                "keyword", text,
                "source", "demo_platform.greeting"
        );
    }

    @GetMapping("/run")
    public Map<String, Object> run(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "theme_001") String themeLibraryId
    ) {
        String text = keyword == null || keyword.isBlank() ? "档案主题库" : keyword.trim();
        Map<String, Object> corpus = mockArchiveCorpusService.loadCorpusBundle(themeLibraryId);
        String summary = String.valueOf(corpus.getOrDefault("summary", ""));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", "【连接器演示】已调用 demo_platform.run_demo\n用户输入：" + text + "\n\n主题库 " + themeLibraryId + " 资料摘要：\n" + summary);
        result.put("keyword", text);
        result.put("themeLibraryId", themeLibraryId);
        result.put("itemCount", corpus.get("itemCount"));
        result.put("corpusSummary", summary);
        result.put("source", "demo_platform.run_demo");
        return result;
    }
}
