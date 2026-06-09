package com.mw.ai.agi.openapi.client.feign;

import com.mw.ai.agi.openapi.client.model.ApiResponse;
import com.mw.ai.agi.openapi.client.model.identity.ResolveIdentityRequest;
import com.mw.ai.agi.openapi.client.model.identity.RuntimeIdentityContext;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        contextId = "agiOpenIdentityClient",
        name = "${agi.openapi.service-name:aiworkflow-server}",
        url = "${agi.openapi.base-url:}"
)
public interface AgiOpenIdentityClient {

    @PostMapping("/api/open/identity/resolve")
    ApiResponse<RuntimeIdentityContext> resolveIdentity(@RequestBody ResolveIdentityRequest request);
}
