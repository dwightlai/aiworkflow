package com.aiworkflow.integration.auth;

import org.springframework.stereotype.Service;

@Service
public class RemoteAuthenticationService {
    private final AuthenticationClient authenticationClient;

    public RemoteAuthenticationService(AuthenticationClient authenticationClient) {
        this.authenticationClient = authenticationClient;
    }

    public TokenIntrospectionResponse introspect(String token) {
        return authenticationClient.introspect(new TokenIntrospectionRequest(token));
    }

    public AuthUserResponse getUser(String userId) {
        return authenticationClient.getUser(userId);
    }
}
