package com.hubfeatcreators.domain.portal;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/branding")
public class BrandingController {

    private final AssessoriaBrandingRepository brandingRepo;

    public BrandingController(AssessoriaBrandingRepository brandingRepo) {
        this.brandingRepo = brandingRepo;
    }

    /** Public — called by portal frontend before login. Single branding record. */
    @GetMapping
    public AssessoriaBranding get() {
        return brandingRepo.findAll().stream()
                .findFirst()
                .orElse(new AssessoriaBranding(null, null));
    }

    @PutMapping
    @RequirePermission(PermissionCodes.OWNR)
    public AssessoriaBranding upsert(
            @AuthenticationPrincipal Object principal, @Valid @RequestBody BrandingRequest req) {
        AssessoriaBranding branding =
                brandingRepo.findAll().stream()
                        .findFirst()
                        .orElse(new AssessoriaBranding(null, null));
        branding.setLogoUrl(req.logoUrl());
        branding.setCorPrimaria(req.corPrimaria());
        return brandingRepo.save(branding);
    }

    public record BrandingRequest(String logoUrl, String corPrimaria) {}
}
