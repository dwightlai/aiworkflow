package com.mw.ai.agi.generation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mw.ai.agi.common.api.ApiResponse;
import com.mw.ai.agi.common.audit.OperatorContext;
import com.mw.ai.agi.generation.domain.GenerationJob;
import com.mw.ai.agi.generation.domain.GenerationOutput;
import com.mw.ai.agi.generation.service.MockArchiveCorpusService;
import com.mw.ai.agi.generation.service.ResearchGenerationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/research")
public class ResearchController {
    private final ResearchGenerationService researchGenerationService;
    private final MockArchiveCorpusService mockArchiveCorpusService;
    private final ObjectMapper objectMapper;

    public ResearchController(
            ResearchGenerationService researchGenerationService,
            MockArchiveCorpusService mockArchiveCorpusService,
            ObjectMapper objectMapper
    ) {
        this.researchGenerationService = researchGenerationService;
        this.mockArchiveCorpusService = mockArchiveCorpusService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/theme-libraries")
    public ApiResponse<PageResponse<Map<String, Object>>> listThemeLibraries() {
        List<Map<String, Object>> items = mockArchiveCorpusService.listThemeLibraries();
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @GetMapping("/mock/theme-libraries")
    public ApiResponse<List<Map<String, Object>>> listThemeLibrariesLegacy() {
        return ApiResponse.success(mockArchiveCorpusService.listThemeLibraries());
    }

    @GetMapping("/mock/theme-libraries/{libraryId}/corpus")
    public ApiResponse<Map<String, Object>> loadThemeLibraryCorpus(@PathVariable String libraryId) {
        return ApiResponse.success(mockArchiveCorpusService.loadCorpusBundle(libraryId));
    }

    @GetMapping("/mock/theme-libraries/{libraryId}/items")
    public ApiResponse<List<Map<String, Object>>> listThemeLibraryItems(@PathVariable String libraryId) {
        return ApiResponse.success(mockArchiveCorpusService.listThemeLibraryItems(libraryId));
    }

    @PostMapping("/compile/from-knowledge-dataset")
    public ApiResponse<ResearchApiMapper.ResearchJobView> compileFromKnowledgeDataset(
            @Valid @RequestBody CompileFromKnowledgeDatasetRequest request,
            HttpServletRequest httpRequest
    ) {
        GenerationJob job = researchGenerationService.runFromKnowledgeDataset(
                request.templateId(),
                request.knowledgeBaseId(),
                request.datasetId(),
                request.unitId(),
                resolveUserId(request.userId()),
                request.variables(),
                bearerToken(httpRequest)
        );
        return ApiResponse.success(ResearchApiMapper.toJobView(job, objectMapper));
    }

    @PostMapping("/jobs")
    public ApiResponse<ResearchApiMapper.ResearchJobView> createJob(
            @Valid @RequestBody CreateResearchJobRequest request,
            HttpServletRequest httpRequest
    ) {
        GenerationJob job = researchGenerationService.run(
                request.botId(),
                request.templateId(),
                request.unitId(),
                resolveUserId(request.userId()),
                request.knowledgeBaseIds(),
                resolveExternalCorpus(request),
                request.variables(),
                bearerToken(httpRequest)
        );
        return ApiResponse.success(ResearchApiMapper.toJobView(job, objectMapper));
    }

    @GetMapping("/output-templates")
    public ApiResponse<PageResponse<Map<String, Object>>> listOutputTemplates(
            @RequestParam(required = false) String outputType,
            @RequestParam(required = false) String templateCategory
    ) {
        List<Map<String, Object>> items = researchGenerationService.listOutputTemplates(outputType, templateCategory);
        return ApiResponse.success(new PageResponse<>(items, items.size()));
    }

    @PostMapping("/outputs/{outputId}/render-docx")
    public ApiResponse<ResearchApiMapper.GenerationOutputView> renderDocx(
            @PathVariable String outputId,
            @RequestBody(required = false) RenderDocxRequest request
    ) {
        String templateId = request == null ? null : request.outputTemplateId();
        GenerationOutput output = researchGenerationService.renderDocx(outputId, templateId);
        return ApiResponse.success(ResearchApiMapper.toOutputView(output, objectMapper));
    }

    @GetMapping(value = "/outputs/{outputId}/html-preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> htmlPreview(@PathVariable String outputId) {
        return ResponseEntity.ok(researchGenerationService.renderHtmlPreview(outputId));
    }

    @GetMapping("/demo/topic-collection/sample-json")
    public ApiResponse<Map<String, Object>> demoTopicCollectionSampleJson() {
        return ApiResponse.success(researchGenerationService.loadDemoTopicCollectionSampleJson());
    }

    @PostMapping("/demo/topic-collection/render-docx")
    public ResponseEntity<Resource> demoTopicCollectionRenderDocx(
            @RequestBody(required = false) Map<String, Object> contentJson
    ) {
        Map<String, Object> payload = contentJson == null || contentJson.isEmpty()
                ? researchGenerationService.loadDemoTopicCollectionSampleJson()
                : contentJson;
        byte[] docxBytes = researchGenerationService.renderDemoTopicCollectionDocx(payload);
        String title = payload.get("title") == null ? "topic_collection" : String.valueOf(payload.get("title"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + title + ".docx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(new org.springframework.core.io.ByteArrayResource(docxBytes));
    }

    @GetMapping("/jobs")
    public ApiResponse<PageResponse<ResearchApiMapper.ResearchJobView>> listJobs() {
        List<ResearchApiMapper.ResearchJobView> jobs = researchGenerationService.listJobs().stream()
                .map(job -> ResearchApiMapper.toJobView(job, objectMapper))
                .toList();
        return ApiResponse.success(new PageResponse<>(jobs, jobs.size()));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<ResearchApiMapper.ResearchJobView> getJob(@PathVariable String jobId) {
        return ApiResponse.success(ResearchApiMapper.toJobView(researchGenerationService.getJob(jobId), objectMapper));
    }

    @GetMapping("/jobs/{jobId}/output")
    public ApiResponse<ResearchApiMapper.GenerationOutputView> getJobOutput(@PathVariable String jobId) {
        GenerationOutput output = researchGenerationService.getFirstOutput(jobId);
        return ApiResponse.success(ResearchApiMapper.toOutputView(output, objectMapper));
    }

    @GetMapping("/jobs/{jobId}/outputs")
    public ApiResponse<PageResponse<ResearchApiMapper.GenerationOutputView>> listOutputs(@PathVariable String jobId) {
        List<ResearchApiMapper.GenerationOutputView> outputs = researchGenerationService.listOutputs(jobId).stream()
                .map(output -> ResearchApiMapper.toOutputView(output, objectMapper))
                .toList();
        return ApiResponse.success(new PageResponse<>(outputs, outputs.size()));
    }

    @GetMapping("/outputs/{outputId}/docx")
    public ResponseEntity<Resource> downloadDocx(@PathVariable String outputId) throws Exception {
        GenerationOutput output = researchGenerationService.getOutput(outputId);
        if (output.contentDocxPath() == null || output.contentDocxPath().isBlank()) {
            throw new IllegalArgumentException("DOCX output not available for: " + outputId);
        }
        Path filePath = Path.of(output.contentDocxPath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("DOCX file not found for: " + outputId);
        }
        String filename = (output.title() == null ? outputId : output.title()) + ".docx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(new FileSystemResource(filePath));
    }

    @GetMapping("/outputs/{outputId}")
    public ApiResponse<ResearchApiMapper.GenerationOutputView> getOutput(@PathVariable String outputId) {
        return ApiResponse.success(
                ResearchApiMapper.toOutputView(researchGenerationService.getOutput(outputId), objectMapper)
        );
    }

    private Map<String, Object> resolveExternalCorpus(CreateResearchJobRequest request) {
        if (request.externalCorpus() != null && !request.externalCorpus().isEmpty()) {
            return request.externalCorpus();
        }
        if (request.themeLibraryId() == null || request.themeLibraryId().isBlank()) {
            return Map.of();
        }
        Map<String, Object> corpus = new LinkedHashMap<>();
        corpus.put("id", request.themeLibraryId());
        corpus.put("type", "ARCHIVE_THEME_LIBRARY");
        return corpus;
    }

    private static String bearerToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        return null;
    }

    private static String resolveUserId(String requestUserId) {
        if (requestUserId != null && !requestUserId.isBlank()) {
            return requestUserId;
        }
        String current = OperatorContext.currentUserId();
        return "system".equals(current) ? null : current;
    }

    public record CompileFromKnowledgeDatasetRequest(
            @NotBlank String templateId,
            @NotBlank String knowledgeBaseId,
            @NotBlank String datasetId,
            String compileType,
            String outputTemplateCode,
            String unitId,
            String userId,
            Map<String, Object> variables
    ) {
    }

    public record CreateResearchJobRequest(
            String botId,
            @NotBlank String templateId,
            String themeLibraryId,
            String unitId,
            String userId,
            List<String> knowledgeBaseIds,
            Map<String, Object> externalCorpus,
            Map<String, Object> variables
    ) {
    }

    public record RenderDocxRequest(String outputTemplateId) {
    }

    public record PageResponse<T>(List<T> items, long total) {
    }
}
