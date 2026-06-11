package com.mw.ai.agi.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("standalone")
public class SpaFallbackController {

    @GetMapping({
            "/bots",
            "/workflows",
            "/workflows/**",
            "/workflow-runs",
            "/workflow-runs/**",
            "/knowledge",
            "/knowledge/**",
            "/models",
            "/prompts",
            "/research/**",
            "/system/**",
            "/login",
            "/tools"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
