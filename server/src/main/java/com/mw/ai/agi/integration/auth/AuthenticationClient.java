package com.mw.ai.agi.integration.auth;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "${third-party.auth.service-name:auth-service}",
        url = "${third-party.auth.base-url:}"
)
public interface AuthenticationClient {
    @PostMapping("/api/auth/tokens/introspect")
    TokenIntrospectionResponse introspect(@RequestBody TokenIntrospectionRequest request);

    @GetMapping("/api/auth/users/{userId}")
    AuthUserResponse getUser(@PathVariable("userId") String userId);
}
