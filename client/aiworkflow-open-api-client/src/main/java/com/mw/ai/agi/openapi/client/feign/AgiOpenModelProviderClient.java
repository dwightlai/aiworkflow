package com.mw.ai.agi.openapi.client.feign;

import com.mw.ai.agi.openapi.client.model.ApiResponse;
import com.mw.ai.agi.openapi.client.model.PageResponse;
import com.mw.ai.agi.openapi.client.model.model.OpenModelProviderView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        contextId = "agiOpenModelProviderClient",
        name = "${agi.openapi.service-name:aiworkflow-server}",
        url = "${agi.openapi.base-url:}"
)
public interface AgiOpenModelProviderClient {

    @GetMapping("/api/open/model-providers")
    ApiResponse<PageResponse<OpenModelProviderView>> listModelProviders();

    @GetMapping("/api/open/model-providers/{id}")
    ApiResponse<OpenModelProviderView> getModelProvider(@PathVariable("id") String id);
}
