package com.mw.ai.agi.auth.api;

import com.mw.ai.agi.auth.service.ExternalCallerContext;
import com.mw.ai.agi.auth.service.IntegrationAppAuthenticator;
import com.mw.ai.agi.auth.service.RequestAuditContext;
import com.mw.ai.agi.auth.service.RuntimeIdentityContext;
import com.mw.ai.agi.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/open/identity")
@Tag(name = "身份", description = "开放 API 身份解析")
public class OpenIdentityController {
    private static final String APP_CODE_HEADER = "X-AGI-App-Code";
    private static final String API_KEY_HEADER = "X-AGI-Api-Key";

    private final IntegrationAppAuthenticator authenticator;

    public OpenIdentityController(IntegrationAppAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @PostMapping("/resolve")
    public ApiResponse<RuntimeIdentityContext> resolve(
            @RequestHeader(APP_CODE_HEADER) String appCode,
            @RequestHeader(API_KEY_HEADER) String apiKey,
            @Valid @RequestBody ResolveIdentityRequest request,
            HttpServletRequest servletRequest
    ) {
        RuntimeIdentityContext identity = authenticator.authenticateApiKey(
                appCode,
                apiKey,
                new ExternalCallerContext(request.unitId(), request.departmentIds(), request.roleIds(), request.userId()),
                new RequestAuditContext(servletRequest.getRemoteAddr(), servletRequest.getHeader(HttpHeaders.USER_AGENT))
        );
        return ApiResponse.success(identity);
    }

    public record ResolveIdentityRequest(
            String unitId,
            List<String> departmentIds,
            List<String> roleIds,
            String userId
    ) {
    }
}
