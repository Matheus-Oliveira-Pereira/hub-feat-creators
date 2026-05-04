package com.hubfeatcreators.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    record SignupRequest(
            @NotBlank String assessoriaNome,
            @NotBlank @Size(min = 3, max = 50) String slug,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String senha) {}

    record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}

    record RefreshRequest(@NotBlank String refreshToken) {}

    record LogoutRequest(@NotBlank String refreshToken) {}

    record TokenResponse(String accessToken, String refreshToken) {}

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody SignupRequest req, HttpServletRequest http) {
        var pair =
                authService.signup(
                        req.assessoriaNome(), req.slug(), req.email(), req.senha(), http);
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        var pair = authService.login(req.email(), req.senha(), http);
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        var pair = authService.refresh(req.refreshToken(), http);
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req.refreshToken());
    }
}
