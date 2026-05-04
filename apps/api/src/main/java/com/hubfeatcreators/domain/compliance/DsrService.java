package com.hubfeatcreators.domain.compliance;

import com.hubfeatcreators.domain.contato.Contato;
import com.hubfeatcreators.domain.contato.ContatoRepository;
import com.hubfeatcreators.domain.influenciador.Influenciador;
import com.hubfeatcreators.domain.influenciador.InfluenciadorRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DsrService {

    private static final Logger log = LoggerFactory.getLogger(DsrService.class);
    private static final int TOKEN_VALIDITY_DAYS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DsrSolicitacaoRepository solicitacaoRepo;
    private final DsrTokenRepository tokenRepo;
    private final InfluenciadorRepository influenciadorRepo;
    private final ContatoRepository contatoRepo;

    public DsrService(
            DsrSolicitacaoRepository solicitacaoRepo,
            DsrTokenRepository tokenRepo,
            InfluenciadorRepository influenciadorRepo,
            ContatoRepository contatoRepo) {
        this.solicitacaoRepo = solicitacaoRepo;
        this.tokenRepo = tokenRepo;
        this.influenciadorRepo = influenciadorRepo;
        this.contatoRepo = contatoRepo;
    }

    /** Creates DSR and returns raw token (caller must send to titular via e-mail). */
    @Transactional
    public DsrResult criarSolicitacao(UUID assessoriaId, String titularTipo, UUID titularId, DsrSolicitacao.TipoDsr tipo) {
        var solicitacao = new DsrSolicitacao(assessoriaId, titularTipo, titularId, tipo);
        solicitacao = solicitacaoRepo.save(solicitacao);

        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        var dsrToken = new DsrToken(
                solicitacao.getId(),
                tokenHash,
                Instant.now().plus(java.time.Duration.ofDays(TOKEN_VALIDITY_DAYS)));
        tokenRepo.save(dsrToken);

        log.info("dsr.criada solicitacaoId={} tipo={} titularId={}", solicitacao.getId(), tipo, titularId);
        return new DsrResult(solicitacao, rawToken);
    }

    /** Validates token and executes DSR action. Single-use. */
    @Transactional
    public DsrSolicitacao executarComToken(String rawToken) {
        String tokenHash = hash(rawToken);
        DsrToken dsrToken = tokenRepo.findByTokenHash(tokenHash)
                .orElseThrow(() -> BusinessException.notFound("DSR_TOKEN"));

        if (dsrToken.isExpired()) {
            throw BusinessException.unprocessable("DSR_TOKEN_EXPIRADO", "Token de DSR expirado.");
        }
        if (dsrToken.isUsed()) {
            throw BusinessException.unprocessable("DSR_TOKEN_USADO", "Token de DSR já utilizado.");
        }

        dsrToken.setUsedAt(Instant.now());
        tokenRepo.save(dsrToken);

        DsrSolicitacao solicitacao = solicitacaoRepo.findById(dsrToken.getSolicitacaoId())
                .orElseThrow(() -> BusinessException.notFound("DSR_SOLICITACAO"));

        solicitacao.setStatus(DsrSolicitacao.StatusDsr.EM_ANDAMENTO);
        solicitacao = solicitacaoRepo.save(solicitacao);

        executarAcao(solicitacao);
        return solicitacao;
    }

    /** Returns all data about titular for ACESSO request. */
    @Transactional(readOnly = true)
    public Map<String, Object> exportarDadosTitular(String titularTipo, UUID titularId) {
        return switch (titularTipo) {
            case "INFLUENCIADOR" -> {
                Influenciador inf = influenciadorRepo.findById(titularId)
                        .orElseThrow(() -> BusinessException.notFound("INFLUENCIADOR"));
                yield Map.of(
                        "tipo", "INFLUENCIADOR",
                        "id", inf.getId(),
                        "nome", inf.getNome(),
                        "handles", inf.getHandles(),
                        "nicho", inf.getNicho() != null ? inf.getNicho() : "",
                        "tags", List.of(inf.getTags()),
                        "createdAt", inf.getCreatedAt()
                );
            }
            case "CONTATO" -> {
                Contato c = contatoRepo.findById(titularId)
                        .orElseThrow(() -> BusinessException.notFound("CONTATO"));
                yield Map.of(
                        "tipo", "CONTATO",
                        "id", c.getId(),
                        "nome", c.getNome(),
                        "email", c.getEmail() != null ? c.getEmail() : "",
                        "telefone", c.getTelefone() != null ? c.getTelefone() : "",
                        "cargo", c.getCargo() != null ? c.getCargo() : "",
                        "createdAt", c.getCreatedAt()
                );
            }
            default -> throw BusinessException.badRequest("TIPO_INVALIDO", "Tipo de titular desconhecido: " + titularTipo);
        };
    }

    /** Anonymizes PII for EXCLUSAO request. */
    @Transactional
    public void anonimizarTitular(String titularTipo, UUID titularId) {
        String anonNome = "Titular #" + titularId.toString().substring(0, 8);

        switch (titularTipo) {
            case "INFLUENCIADOR" -> {
                Influenciador inf = influenciadorRepo.findById(titularId)
                        .orElseThrow(() -> BusinessException.notFound("INFLUENCIADOR"));
                inf.setNome(anonNome);
                inf.setHandles(Map.of());
                inf.setObservacoes(null);
                inf.setUpdatedAt(Instant.now());
                inf.setDeletedAt(Instant.now());
                influenciadorRepo.save(inf);
                log.info("dsr.anonimizado titularTipo=INFLUENCIADOR titularId={}", titularId);
            }
            case "CONTATO" -> {
                Contato c = contatoRepo.findById(titularId)
                        .orElseThrow(() -> BusinessException.notFound("CONTATO"));
                c.setNome(anonNome);
                c.setEmail(null);
                c.setTelefone(null);
                c.setUpdatedAt(Instant.now());
                c.setDeletedAt(Instant.now());
                contatoRepo.save(c);
                log.info("dsr.anonimizado titularTipo=CONTATO titularId={}", titularId);
            }
            default -> throw BusinessException.badRequest("TIPO_INVALIDO", "Tipo de titular desconhecido: " + titularTipo);
        }
    }

    public List<DsrSolicitacao> alertarVencendo(int diasAlerta) {
        Instant limite = Instant.now().plus(java.time.Duration.ofDays(diasAlerta));
        return solicitacaoRepo.findVencendoAntes(limite);
    }

    private void executarAcao(DsrSolicitacao solicitacao) {
        try {
            switch (solicitacao.getTipo()) {
                case EXCLUSAO -> anonimizarTitular(solicitacao.getTitularTipo(), solicitacao.getTitularId());
                case ACESSO, PORTABILIDADE -> {
                    // Dados exportados via endpoint separado; marcar concluída
                }
                case CORRECAO, OPOSICAO -> {
                    // Requer intervenção humana; manter EM_ANDAMENTO
                    return;
                }
            }
            solicitacao.setStatus(DsrSolicitacao.StatusDsr.CONCLUIDA);
            solicitacao.setAtendidoEm(Instant.now());
            solicitacaoRepo.save(solicitacao);
        } catch (Exception e) {
            log.error("dsr.execucao.erro solicitacaoId={}", solicitacao.getId(), e);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record DsrResult(DsrSolicitacao solicitacao, String rawToken) {}
}
