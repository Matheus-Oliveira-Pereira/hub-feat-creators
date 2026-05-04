package com.hubfeatcreators.onboarding;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.onboarding.*;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.mail.SystemMailService;
import com.hubfeatcreators.infra.web.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingServiceTest {

    @Mock EmailVerifyTokenRepository verifyRepo;
    @Mock PasswordResetTokenRepository resetRepo;
    @Mock UsuarioRepository usuarioRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SystemMailService mailService;
    @Mock AppProperties props;
    @InjectMocks OnboardingService service;

    @BeforeEach
    void setup() {
        AppProperties.Web web = new AppProperties.Web();
        when(props.getWeb()).thenReturn(web);
    }

    @Test
    void sendVerification_savesTokenAndSendsEmail() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "a@b.com", "hash", Usuario.Role.ASSESSOR);
        when(verifyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(verifyRepo).invalidateExisting(any());

        service.sendVerification(usuario);

        verify(verifyRepo).save(any());
        verify(mailService).sendVerifyEmail(eq("a@b.com"), anyString());
    }

    @Test
    void verifyEmail_validToken_setsVerifiedAt() {
        String raw = "token-raw";
        String hash = OnboardingService.sha256(raw);
        EmailVerifyToken token = new EmailVerifyToken(UUID.randomUUID(), hash, Instant.now().plus(1, ChronoUnit.HOURS));

        Usuario usuario = new Usuario(UUID.randomUUID(), "a@b.com", "hash", Usuario.Role.ASSESSOR);

        when(verifyRepo.findByTokenHash(hash)).thenReturn(Optional.of(token));
        when(usuarioRepo.findById(token.getUsuarioId())).thenReturn(Optional.of(usuario));
        when(verifyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.verifyEmail(raw);

        verify(usuarioRepo).save(argThat(u -> u.getEmailVerificadoEm() != null));
    }

    @Test
    void verifyEmail_expiredToken_throws410() {
        String raw = "expired-token";
        String hash = OnboardingService.sha256(raw);
        EmailVerifyToken token = new EmailVerifyToken(UUID.randomUUID(), hash, Instant.now().minus(1, ChronoUnit.HOURS));

        when(verifyRepo.findByTokenHash(hash)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail(raw))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.GONE);
    }

    @Test
    void requestPasswordReset_unknownEmail_noError() {
        when(usuarioRepo.findByEmail("x@y.com")).thenReturn(Optional.empty());
        service.requestPasswordReset("x@y.com"); // silent
        verify(mailService, never()).sendPasswordReset(anyString(), anyString());
    }

    @Test
    void resetPassword_validToken_changesHash() {
        String raw = "reset-raw";
        String hash = OnboardingService.sha256(raw);
        UUID uid = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetToken(uid, hash, Instant.now().plus(1, ChronoUnit.HOURS));

        Usuario usuario = new Usuario(uid, "a@b.com", "old", Usuario.Role.ASSESSOR);
        when(resetRepo.findByTokenHash(hash)).thenReturn(Optional.of(token));
        when(usuarioRepo.findById(uid)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nova123!")).thenReturn("argon2hash");
        when(resetRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.resetPassword(raw, "nova123!");

        verify(usuarioRepo).save(argThat(u -> "argon2hash".equals(u.getSenhaHash())));
    }
}
