package com.mw.ai.agi.asset.api;

import com.mw.ai.agi.asset.domain.AssetGrant;
import com.mw.ai.agi.asset.service.AssetGrantService;
import com.mw.ai.agi.auth.service.RequestIdentitySupport;
import com.mw.ai.agi.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asset-grants")
public class AssetGrantController {
    private final AssetGrantService grantService;
    private final RequestIdentitySupport identitySupport;

    public AssetGrantController(AssetGrantService grantService, RequestIdentitySupport identitySupport) {
        this.grantService = grantService;
        this.identitySupport = identitySupport;
    }

    @GetMapping
    public ApiResponse<AssetGrantView> list(@RequestParam String assetType, @RequestParam String assetId) {
        return ApiResponse.success(new AssetGrantView(
                grantService.loadOwnerUnitId(assetType, assetId),
                grantService.list(assetType, assetId)
        ));
    }

    @PutMapping
    public ApiResponse<AssetGrantView> replace(
            @Valid @RequestBody ReplaceAssetGrantsRequest request,
            HttpServletRequest servletRequest
    ) {
        String ownerUnitId = grantService.ensureOwnerUnitId(
                request.assetType(),
                request.assetId(),
                request.ownerUnitId(),
                identitySupport.resolveOwnerUnitId(null, servletRequest)
        );
        return ApiResponse.success(new AssetGrantView(
                ownerUnitId,
                grantService.replaceUseGrants(
                        request.assetType(),
                        request.assetId(),
                        ownerUnitId,
                        request.unitIds() == null ? List.of() : request.unitIds(),
                        request.unitScope(),
                        request.departmentIds(),
                        request.departmentScope(),
                        identitySupport.resolve(servletRequest).map(identity -> identity.userId()).orElse(null),
                        identitySupport.grantContext(servletRequest)
                )
        ));
    }

    public record AssetGrantView(
            String ownerUnitId,
            List<AssetGrant> grants
    ) {
    }

    public record ReplaceAssetGrantsRequest(
            @NotBlank String assetType,
            @NotBlank String assetId,
            String ownerUnitId,
            List<String> unitIds,
            String unitScope,
            List<String> departmentIds,
            String departmentScope
    ) {
    }
}
