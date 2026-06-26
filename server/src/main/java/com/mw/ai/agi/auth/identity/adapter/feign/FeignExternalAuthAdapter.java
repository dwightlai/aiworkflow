package com.mw.ai.agi.auth.identity.adapter.feign;

import com.mw.ai.agi.auth.identity.adapter.ExternalAuthAdapter;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalTokenIntrospection;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalUserProfile;
import com.mw.ai.agi.integration.auth.AuthUserResponse;
import com.mw.ai.agi.integration.auth.AuthenticationClient;
import com.mw.ai.agi.integration.auth.TokenIntrospectionRequest;
import com.mw.ai.agi.integration.auth.TokenIntrospectionResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@ConditionalOnProperty(prefix = "agi.identity.adapter", name = "auth", havingValue = "feign")
public class FeignExternalAuthAdapter implements ExternalAuthAdapter {
    private final AuthenticationClient authenticationClient;

    public FeignExternalAuthAdapter(AuthenticationClient authenticationClient) {
        this.authenticationClient = authenticationClient;
    }

    @Override
    public ExternalTokenIntrospection introspect(String accessToken) {
        TokenIntrospectionResponse response = authenticationClient.introspect(new TokenIntrospectionRequest(accessToken));
        return new ExternalTokenIntrospection(
                response.active(),
                response.userId(),
                response.username(),
                response.roles(),
                response.permissions(),
                null
        );
    }

    @Override
    public Optional<ExternalUserProfile> getUser(String userId) {
        AuthUserResponse response = authenticationClient.getUser(userId);
        if (response == null) {
            return Optional.empty();
        }
        return Optional.of(new ExternalUserProfile(
                response.id(),
                response.username(),
                response.displayName(),
                response.enabled(),
                response.roles()
        ));
    }
}
