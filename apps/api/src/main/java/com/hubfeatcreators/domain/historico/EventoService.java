package com.hubfeatcreators.domain.historico;

import com.hubfeatcreators.domain.historico.Evento.EntidadeRef;
import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {

    private static final Logger log = LoggerFactory.getLogger(EventoService.class);
    private static final int MAX_SIZE = 200;

    private final EventoRepository repo;
    private final MeterRegistry meterRegistry;

    public EventoService(EventoRepository repo, MeterRegistry meterRegistry) {
        this.repo = repo;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Evento registrar(
            UUID autorId, EventoTipo tipo, Map<String, Object> payload, EntidadeRef... entidades) {
        Evento evento = Evento.of(tipo, autorId, payload, entidades);
        repo.save(evento);
        meterRegistry.counter("evento_publicado_total", "tipo", tipo.name()).increment();
        log.debug("evento.registrado id={} tipo={}", evento.getId(), tipo);
        return evento;
    }

    @Transactional(readOnly = true)
    public List<Evento> listar(
            AuthPrincipal principal,
            String entidadeTipo,
            UUID entidadeId,
            List<String> tipos,
            String cursor,
            int size) {
        int lim = Math.min(Math.max(1, size), MAX_SIZE);
        String entId = entidadeId != null ? entidadeId.toString() : null;
        String tipoFiltro = (tipos != null && !tipos.isEmpty()) ? tipos.get(0) : null;

        DecodedCursor dc = decodeCursor(cursor);

        if (canSeeAll(principal)) {
            return dc == null
                    ? repo.findOwnerFirst(entidadeTipo, entId, tipoFiltro, lim)
                    : repo.findOwner(entidadeTipo, entId, tipoFiltro, dc.ts(), dc.id(), lim);
        }
        return dc == null
                ? repo.findAssessorFirst(
                        principal.usuarioId(), entidadeTipo, entId, tipoFiltro, lim)
                : repo.findAssessor(
                        principal.usuarioId(),
                        entidadeTipo,
                        entId,
                        tipoFiltro,
                        dc.ts(),
                        dc.id(),
                        lim);
    }

    public String encodeCursor(Instant ts, UUID id) {
        String raw = ts.toEpochMilli() + ":" + id;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private DecodedCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String raw = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(":", 2);
            Instant ts = Instant.ofEpochMilli(Long.parseLong(parts[0]));
            UUID id = UUID.fromString(parts[1]);
            return new DecodedCursor(ts, id);
        } catch (Exception e) {
            log.warn("historico.cursor.invalido cursor={}", cursor);
            return null;
        }
    }

    public boolean canSeeAll(AuthPrincipal p) {
        return "OWNER".equals(p.role()) || p.permissions().contains(PermissionCodes.OWNR);
    }

    record DecodedCursor(Instant ts, UUID id) {}
}
