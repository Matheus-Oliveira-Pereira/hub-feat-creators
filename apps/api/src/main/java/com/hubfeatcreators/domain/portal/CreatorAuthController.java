package com.hubfeatcreators.domain.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/auth")
public class CreatorAuthController {

    private final CreatorAuthService authService;

    public CreatorAuthController(CreatorAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<CreatorAuthService.TokenResponse> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req.email(), req.senha()));
    }

    @PostMapping("/convite/aceitar")
    public ResponseEntity<CreatorAuthService.TokenResponse> aceitarConvite(
            @Valid @RequestBody AceitarConviteRequest req) {
        return ResponseEntity.ok(authService.aceitarConvite(req.token(), req.senha()));
    }

    @GetMapping("/convite/info")
    public ResponseEntity<CreatorAuthService.InviteInfo> infoConvite(@RequestParam String token) {
        return ResponseEntity.ok(authService.infoConvite(token));
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}

    public record AceitarConviteRequest(
            @NotBlank String token, @NotBlank @Size(min = 8) String senha) {}
}
