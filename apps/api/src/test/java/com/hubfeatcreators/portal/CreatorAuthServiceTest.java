package com.hubfeatcreators.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.influenciador.Influenciador;
import com.hubfeatcreators.domain.influenciador.InfluenciadorRepository;
import com.hubfeatcreators.domain.portal.*;
import com.hubfeatcreators.infra.mail.SystemMailService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.JwtService;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CreatorAuthServiceTest {

    @Mock CreatorUserRepository creatorUserRepo;
    @Mock CreatorInviteRepository inviteRepo;
    @Mock InfluenciadorRepository influenciadorRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock SystemMailService mailService;

    AppProperties props = new AppProperties();
    CreatorAuthService service;

    UUID influenciadorId = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        props.getFeatures().setPortalEnabled(true);
        props.getWeb().setBaseUrl("http://localhost:3000");
        service =
                new CreatorAuthService(
                        creatorUserRepo,
                        inviteRepo,
                        influenciadorRepo,
                        passwordEncoder,
                        jwtService,
                        mailService,
                        props);
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        CreatorUser user = new CreatorUser(influenciadorId, "c@test.com", "hash");
        when(creatorUserRepo.findByEmail("c@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(jwtService.generateCreatorToken(any(), any())).thenReturn("tok");

        var resp = service.login("c@test.com", "senha123");
        assertThat(resp.token()).isEqualTo("tok");
        assertThat(resp.email()).isEqualTo("c@test.com");
    }

    @Test
    void login_wrongPassword_throws401() {
        CreatorUser user = new CreatorUser(influenciadorId, "c@test.com", "hash");
        when(creatorUserRepo.findByEmail("c@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("c@test.com", "wrong"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Credenciais");
    }

    @Test
    void login_portalDisabled_throws403() {
        props.getFeatures().setPortalEnabled(false);
        assertThatThrownBy(() -> service.login("x@x.com", "pass"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void login_inactiveUser_throws401() {
        CreatorUser user = new CreatorUser(influenciadorId, "c@test.com", "hash");
        user.setStatus("INATIVO");
        when(creatorUserRepo.findByEmail("c@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login("c@test.com", "senha123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inativa");
    }

    // ─── Convidar ────────────────────────────────────────────────────────────

    @Test
    void convidar_success_sendsEmail() {
        AuthPrincipal principal = new AuthPrincipal(usuarioId, "OWNER", Set.of());
        when(influenciadorRepo.existsById(influenciadorId)).thenReturn(true);
        when(inviteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.convidar(principal, influenciadorId, "creator@test.com");

        ArgumentCaptor<CreatorInvite> captor = ArgumentCaptor.forClass(CreatorInvite.class);
        verify(inviteRepo).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("creator@test.com");
        verify(mailService).sendInvite(eq("creator@test.com"), anyString(), anyString());
    }

    @Test
    void convidar_noEmail_throws400() {
        AuthPrincipal principal = new AuthPrincipal(usuarioId, "OWNER", Set.of());
        when(influenciadorRepo.existsById(influenciadorId)).thenReturn(true);

        assertThatThrownBy(() -> service.convidar(principal, influenciadorId, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void convidar_influenciadorNotFound_throws404() {
        AuthPrincipal principal = new AuthPrincipal(usuarioId, "OWNER", Set.of());
        when(influenciadorRepo.existsById(influenciadorId)).thenReturn(false);

        assertThatThrownBy(() -> service.convidar(principal, influenciadorId, "x@x.com"))
                .isInstanceOf(BusinessException.class);
    }

    // ─── Aceitar convite ──────────────────────────────────────────────────────

    @Test
    void aceitarConvite_success() {
        CreatorInvite invite =
                new CreatorInvite(
                        influenciadorId,
                        "c@test.com",
                        "hash",
                        Instant.now().plus(7, ChronoUnit.DAYS),
                        usuarioId);

        when(inviteRepo.findByTokenHash(anyString())).thenReturn(Optional.of(invite));
        when(creatorUserRepo.existsByInfluenciadorId(influenciadorId)).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("encodedHash");
        when(creatorUserRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inviteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateCreatorToken(any(), any())).thenReturn("tok");

        var resp = service.aceitarConvite("rawtoken", "senha123");
        assertThat(resp.token()).isEqualTo("tok");
        assertThat(invite.getAceitoEm()).isNotNull();
    }

    @Test
    void aceitarConvite_expired_throws410() {
        CreatorInvite invite =
                new CreatorInvite(
                        influenciadorId,
                        "c@test.com",
                        "hash",
                        Instant.now().minus(1, ChronoUnit.HOURS),
                        usuarioId);

        when(inviteRepo.findByTokenHash(anyString())).thenReturn(Optional.of(invite));

        assertThatThrownBy(() -> service.aceitarConvite("rawtoken", "senha"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void aceitarConvite_creatorAlreadyExists_throws409() {
        CreatorInvite invite =
                new CreatorInvite(
                        influenciadorId,
                        "c@test.com",
                        "hash",
                        Instant.now().plus(1, ChronoUnit.HOURS),
                        usuarioId);

        when(inviteRepo.findByTokenHash(anyString())).thenReturn(Optional.of(invite));
        when(creatorUserRepo.existsByInfluenciadorId(influenciadorId)).thenReturn(true);

        assertThatThrownBy(() -> service.aceitarConvite("rawtoken", "senha"))
                .isInstanceOf(BusinessException.class);
    }

    // ─── Info convite ─────────────────────────────────────────────────────────

    @Test
    void infoConvite_returnsEmailAndNome() {
        CreatorInvite invite =
                new CreatorInvite(
                        influenciadorId,
                        "c@test.com",
                        "hash",
                        Instant.now().plus(1, ChronoUnit.HOURS),
                        usuarioId);

        when(inviteRepo.findByTokenHash(anyString())).thenReturn(Optional.of(invite));
        Influenciador inf = new Influenciador("João Silva", usuarioId);
        when(influenciadorRepo.findById(influenciadorId)).thenReturn(Optional.of(inf));

        var info = service.infoConvite("rawtoken");
        assertThat(info.email()).isEqualTo("c@test.com");
        assertThat(info.nomeInfluenciador()).isEqualTo("João Silva");
    }
}
