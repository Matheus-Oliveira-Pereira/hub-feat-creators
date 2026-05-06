package com.hubfeatcreators.historico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.historico.Evento;
import com.hubfeatcreators.domain.historico.Evento.EntidadeRef;
import com.hubfeatcreators.domain.historico.EventoRepository;
import com.hubfeatcreators.domain.historico.EventoService;
import com.hubfeatcreators.domain.historico.EventoTipo;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventoServiceTest {

    @Mock EventoRepository repo;

    EventoService service;

    UUID assessoriaId = UUID.randomUUID();
    UUID autorId = UUID.randomUUID();
    UUID entidadeId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new EventoService(repo, new SimpleMeterRegistry());
    }

    @Test
    void registrar_grava_evento_com_entidades_corretas() {
        EntidadeRef ref1 = new EntidadeRef("PROSPECCAO", entidadeId);
        EntidadeRef ref2 = new EntidadeRef("MARCA", UUID.randomUUID());

        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Evento saved =
                service.registrar(
                        assessoriaId,
                        autorId,
                        EventoTipo.PROSPECCAO_CRIADA,
                        Map.of("titulo", "Test"),
                        ref1,
                        ref2);

        ArgumentCaptor<Evento> cap = ArgumentCaptor.forClass(Evento.class);
        verify(repo).save(cap.capture());

        Evento e = cap.getValue();
        assertThat(e.getAssessoriaId()).isEqualTo(assessoriaId);
        assertThat(e.getAutorId()).isEqualTo(autorId);
        assertThat(e.getTipo()).isEqualTo(EventoTipo.PROSPECCAO_CRIADA.name());
        assertThat(e.getEntidadesRelacionadas()).hasSize(2);
        assertThat(e.getEntidadesRelacionadas().get(0).get("tipo")).isEqualTo("PROSPECCAO");
        assertThat(e.getEntidadesRelacionadas().get(1).get("tipo")).isEqualTo("MARCA");
        assertThat(saved).isNotNull();
    }

    @Test
    void listar_owner_usa_query_owner() {
        AuthPrincipal owner = principal("OWNER", Set.of("OWNR"));

        when(repo.findOwnerFirst(any(), any(), any(), any(), anyInt())).thenReturn(List.of());

        service.listar(owner, "PROSPECCAO", entidadeId, null, null, 50);

        verify(repo).findOwnerFirst(assessoriaId, "PROSPECCAO", entidadeId.toString(), null, 50);
        verify(repo, never()).findAssessorFirst(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void listar_assessor_usa_query_assessor_com_userId() {
        UUID userId = UUID.randomUUID();
        AuthPrincipal assessor = principalAssessor(userId);

        when(repo.findAssessorFirst(any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        service.listar(assessor, "PROSPECCAO", entidadeId, null, null, 50);

        verify(repo)
                .findAssessorFirst(
                        assessoriaId, userId, "PROSPECCAO", entidadeId.toString(), null, 50);
        verify(repo, never()).findOwnerFirst(any(), any(), any(), any(), anyInt());
    }

    @Test
    void cursor_encode_decode_roundtrip() {
        // Use millis-precision instant to avoid sub-millisecond truncation difference
        Instant ts = Instant.ofEpochMilli(Instant.now().toEpochMilli());
        UUID id = UUID.randomUUID();

        String cursor = service.encodeCursor(ts, id);
        assertThat(cursor).isNotBlank();

        String raw = new String(java.util.Base64.getDecoder().decode(cursor));
        String[] parts = raw.split(":", 2);
        assertThat(Instant.ofEpochMilli(Long.parseLong(parts[0]))).isEqualTo(ts);
        assertThat(UUID.fromString(parts[1])).isEqualTo(id);
    }

    private AuthPrincipal principal(String role, Set<String> perms) {
        AuthPrincipal p = mock(AuthPrincipal.class);
        when(p.assessoriaId()).thenReturn(assessoriaId);
        when(p.usuarioId()).thenReturn(autorId);
        when(p.role()).thenReturn(role);
        when(p.permissions()).thenReturn(perms);
        return p;
    }

    private AuthPrincipal principalAssessor(UUID userId) {
        AuthPrincipal p = mock(AuthPrincipal.class);
        when(p.assessoriaId()).thenReturn(assessoriaId);
        when(p.usuarioId()).thenReturn(userId);
        when(p.role()).thenReturn("ASSESSOR");
        when(p.permissions()).thenReturn(Set.of());
        return p;
    }
}
