package com.hubfeatcreators.domain.portal;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.BusinessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/branding")
public class BrandingController {

    private final AssessoriaBrandingRepository brandingRepo;

    public BrandingController(AssessoriaBrandingRepository brandingRepo) {
        this.brandingRepo = brandingRepo;
    }

    /** Public — called by portal frontend before login. */
    @GetMapping("/{slug}")
    public AssessoriaBranding get(@PathVariable String slug) {
        return brandingRepo.findByAssessoriaSlug(slug).orElseThrow(BusinessException::notFound);
    }

    @PutMapping
    @RequirePermission(PermissionCodes.OWNR)
    public AssessoriaBranding upsert(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestBody BrandingRequest req) {
        AssessoriaBranding branding =
                brandingRepo
                        .findById(principal.assessoriaId())
                        .orElse(new AssessoriaBranding(principal.assessoriaId()));
        branding.setLogoUrl(req.logoUrl());
        branding.setCorPrimaria(req.corPrimaria());
        return brandingRepo.save(branding);
    }

    public record BrandingRequest(String logoUrl, String corPrimaria) {}
}
