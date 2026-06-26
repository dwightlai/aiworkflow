package com.mw.ai.agi.auth.identity.adapter;

import com.mw.ai.agi.auth.identity.adapter.model.ExternalTokenIntrospection;
import com.mw.ai.agi.auth.identity.adapter.model.ExternalUserProfile;

import java.util.Optional;

public interface ExternalAuthAdapter {
    ExternalTokenIntrospection introspect(String accessToken);

    Optional<ExternalUserProfile> getUser(String userId);
}
