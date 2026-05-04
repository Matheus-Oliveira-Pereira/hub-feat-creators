package com.hubfeatcreators.infra.security;

import com.hubfeatcreators.domain.onboarding.MfaService;
import com.hubfeatcreators.domain.onboarding.OnboardingService;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OnboardingService onboardingService;
    private final MfaService mfaService;

    public AuthController(AuthService authService, OnboardingService onboardingService, MfaService mfaService) {
        this.authService = authService;
        this.onboardingService = onboardingService;
        this.mfaService = mfaService;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    record SignupRequest(
            @NotBlank String assessoriaNome,
            @NotBlank @Size(min = 3, max = 50) String slug,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String senha) {}

    record LoginRequest(@NotBlank @Email String email, @NotBlank String senha, String mfaCode) {}

    record RefreshRequest(@NotBlank String refreshToken) {}

    record LogoutRequest(@NotBlank String refreshToken) {}

    record TokenResponse(String accessToken, String refreshToken) {}

    record SignupResponse(String email, boolean emailVerificado) {}

    record VerifyEmailRequest(@NotBlank String token) {}

    record ForgotPasswordRequest(@NotBlank @Email String email) {}

    record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8) String novaSenha) {}

    record MfaSetupResponse(String secret, String qrCodeUri, List<String> recoveryCodes) {}

    record MfaActivateRequest(@NotBlank String code) {}

    record MfaDisableRequest(@NotBlank String code) {}

    // ── Signup / Login / Tokens ───────────────────────────────────────────

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest req, HttpServletRequest http) {
        var result = authService.signup(req.assessoriaNome(), req.slug(), req.email(), req.senha(), http);
        return new SignupResponse(result.email(), result.emailVerificado());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        var pair = authService.login(req.email(), req.senha(), req.mfaCode(), http);
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        var pair = authService.refresh(req.refreshToken(), http);
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest req, HttpServletRequest http) {
        authService.logout(req.refreshToken(), http);
    }

    // ── Email verification ────────────────────────────────────────────────

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        onboardingService.verifyEmail(req.token());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@AuthenticationPrincipal AuthPrincipal principal) {
        var usuario = authService.getUsuario(principal.usuarioId());
        onboardingService.sendVerification(usuario);
    }

    // ── Password reset ────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        onboardingService.requestPasswordReset(req.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        onboardingService.resetPassword(req.token(), req.novaSenha());
    }

    // ── MFA ───────────────────────────────────────────────────────────────

    @PostMapping("/mfa/setup")
    public MfaSetupResponse mfaSetup(@AuthenticationPrincipal AuthPrincipal principal) {
        var data = mfaService.setupMfa(principal.usuarioId());
        return new MfaSetupResponse(data.secret(), data.qrCodeUri(), data.recoveryCodes());
    }

    @PostMapping("/mfa/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mfaActivate(
            @Valid @RequestBody MfaActivateRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        mfaService.activateMfa(principal.usuarioId(), req.code());
    }

    @PostMapping("/mfa/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mfaDisable(
            @Valid @RequestBody MfaDisableRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        mfaService.disableMfa(principal.usuarioId(), req.code());
    }
}
