package com.mw.ai.agi.auth.identity;

import com.mw.ai.agi.auth.identity.adapter.ExternalAuthAdapter;
import com.mw.ai.agi.auth.identity.adapter.ExternalOrganizationAdapter;
import com.mw.ai.agi.auth.identity.adapter.UnconfiguredExternalAuthAdapter;
import com.mw.ai.agi.auth.identity.adapter.UnconfiguredExternalOrganizationAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExternalAuthAdapter.class)
    ExternalAuthAdapter externalAuthAdapter() {
        return new UnconfiguredExternalAuthAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(ExternalOrganizationAdapter.class)
    ExternalOrganizationAdapter externalOrganizationAdapter() {
        return new UnconfiguredExternalOrganizationAdapter();
    }
}
