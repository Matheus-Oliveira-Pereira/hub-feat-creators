package com.hubfeatcreators.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.compliance.*;
import com.hubfeatcreators.domain.contato.Contato;
import com.hubfeatcreators.domain.contato.ContatoRepository;
import com.hubfeatcreators.domain.influenciador.Influenciador;
import com.hubfeatcreators.domain.influenciador.InfluenciadorRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DsrServiceTest {

    @Mock DsrSolicitacaoRepository solicitacaoRepo;
    @Mock DsrTokenRepository tokenRepo;
    @Mock InfluenciadorRepository influenciadorRepo;
    @Mock ContatoRepository contatoRepo;

    @InjectMocks DsrService dsrService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(solicitacaoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criarSolicitacao_returns_result_with_raw_token() {
        UUID assessoriaId = UUID.randomUUID();
        UUID titularId = UUID.randomUUID();

        var result = dsrService.criarSolicitacao(assessoriaId, "INFLUENCIADOR", titularId, DsrSolicitacao.TipoDsr.ACESSO);

        assertThat(result.solicitacao()).isNotNull();
        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.solicitacao().getTipo()).isEqualTo(DsrSolicitacao.TipoDsr.ACESSO);
        assertThat(result.solicitacao().getStatus()).isEqualTo(DsrSolicitacao.StatusDsr.PENDENTE);
        // prazo = 15 dias
        assertThat(result.solicitacao().getPrazoLegalEm())
                .isAfter(Instant.now().plus(java.time.Duration.ofDays(14)));
    }

    @Test
    void executarComToken_valida_e_anonimiza_para_exclusao() {
        UUID solId = UUID.randomUUID();
        UUID titularId = UUID.randomUUID();
        String rawToken = "test-token-abc123";
        String hash = DsrService.hash(rawToken);

        DsrToken dsrToken = new DsrToken(solId, hash, Instant.now().plusSeconds(3600));
        DsrSolicitacao sol = new DsrSolicitacao(UUID.randomUUID(), "INFLUENCIADOR", titularId, DsrSolicitacao.TipoDsr.EXCLUSAO);

        when(tokenRepo.findByTokenHash(hash)).thenReturn(Optional.of(dsrToken));
        when(solicitacaoRepo.findById(solId)).thenReturn(Optional.of(sol));
        when(solicitacaoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Influenciador inf = new Influenciador(UUID.randomUUID(), "João Creator", UUID.randomUUID());
        when(influenciadorRepo.findById(titularId)).thenReturn(Optional.of(inf));
        when(influenciadorRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DsrSolicitacao resultado = dsrService.executarComToken(rawToken);

        assertThat(resultado.getStatus()).isEqualTo(DsrSolicitacao.StatusDsr.CONCLUIDA);
        assertThat(dsrToken.isUsed()).isTrue();

        // Influenciador must be anonymized
        ArgumentCaptor<Influenciador> captor = ArgumentCaptor.forClass(Influenciador.class);
        verify(influenciadorRepo).save(captor.capture());
        assertThat(captor.getValue().getNome()).startsWith("Titular #");
        assertThat(captor.getValue().getHandles()).isEmpty();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void executarComToken_rejeita_token_expirado() {
        String rawToken = "expired-token";
        String hash = DsrService.hash(rawToken);
        DsrToken expiredToken = new DsrToken(UUID.randomUUID(), hash, Instant.now().minusSeconds(1));

        when(tokenRepo.findByTokenHash(hash)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> dsrService.executarComToken(rawToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void executarComToken_rejeita_token_ja_usado() {
        String rawToken = "used-token";
        String hash = DsrService.hash(rawToken);
        DsrToken usedToken = new DsrToken(UUID.randomUUID(), hash, Instant.now().plusSeconds(3600));
        usedToken.setUsedAt(Instant.now());

        when(tokenRepo.findByTokenHash(hash)).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> dsrService.executarComToken(rawToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("utilizado");
    }

    @Test
    void anonimizarTitular_influenciador_limpa_pii() {
        UUID titularId = UUID.randomUUID();
        Influenciador inf = new Influenciador(UUID.randomUUID(), "Maria Creator", UUID.randomUUID());
        when(influenciadorRepo.findById(titularId)).thenReturn(Optional.of(inf));
        when(influenciadorRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dsrService.anonimizarTitular("INFLUENCIADOR", titularId);

        ArgumentCaptor<Influenciador> captor = ArgumentCaptor.forClass(Influenciador.class);
        verify(influenciadorRepo).save(captor.capture());
        Influenciador anon = captor.getValue();
        assertThat(anon.getNome()).startsWith("Titular #");
        assertThat(anon.getHandles()).isEmpty();
        assertThat(anon.getObservacoes()).isNull();
        assertThat(anon.getDeletedAt()).isNotNull();
    }

    @Test
    void anonimizarTitular_contato_limpa_pii() {
        UUID titularId = UUID.randomUUID();
        Contato c = new Contato(UUID.randomUUID(), UUID.randomUUID(), "Pedro Contato");
        c.setEmail("pedro@empresa.com");
        c.setTelefone("11987654321");
        when(contatoRepo.findById(titularId)).thenReturn(Optional.of(c));
        when(contatoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dsrService.anonimizarTitular("CONTATO", titularId);

        ArgumentCaptor<Contato> captor = ArgumentCaptor.forClass(Contato.class);
        verify(contatoRepo).save(captor.capture());
        Contato anon = captor.getValue();
        assertThat(anon.getNome()).startsWith("Titular #");
        assertThat(anon.getEmail()).isNull();
        assertThat(anon.getTelefone()).isNull();
    }

    @Test
    void exportarDados_influenciador_retorna_campos_corretos() {
        UUID titularId = UUID.randomUUID();
        Influenciador inf = new Influenciador(UUID.randomUUID(), "João Creator", UUID.randomUUID());
        when(influenciadorRepo.findById(titularId)).thenReturn(Optional.of(inf));

        var dados = dsrService.exportarDadosTitular("INFLUENCIADOR", titularId);

        assertThat(dados).containsKey("nome");
        assertThat(dados.get("nome")).isEqualTo("João Creator");
        assertThat(dados.get("tipo")).isEqualTo("INFLUENCIADOR");
    }

    @Test
    void hash_is_deterministic() {
        String token = "some-token-value";
        assertThat(DsrService.hash(token)).isEqualTo(DsrService.hash(token));
    }

    @Test
    void hash_does_not_return_raw_input() {
        String token = "my-secret-token";
        assertThat(DsrService.hash(token)).isNotEqualTo(token);
    }
}
