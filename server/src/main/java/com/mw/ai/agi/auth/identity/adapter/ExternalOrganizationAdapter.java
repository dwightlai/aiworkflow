package com.mw.ai.agi.auth.identity.adapter;

import com.mw.ai.agi.auth.identity.adapter.model.ExternalOrganizationProfile;

public interface ExternalOrganizationAdapter {
    ExternalOrganizationProfile loadOrganizationContext(String userId);
}
