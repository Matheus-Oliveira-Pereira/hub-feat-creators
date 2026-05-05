package com.hubfeatcreators.domain.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notificacao_preferencias")
@IdClass(NotificacaoPreferencia.PK.class)
public class NotificacaoPreferencia {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificacaoTipo tipo;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificacaoCanal canal;

    @Column(nullable = false)
    private boolean habilitado = false;

    protected NotificacaoPreferencia() {}

    public NotificacaoPreferencia(UUID usuarioId, NotificacaoTipo tipo, NotificacaoCanal canal, boolean habilitado) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.canal = canal;
        this.habilitado = habilitado;
    }

    public UUID getUsuarioId() { return usuarioId; }
    public NotificacaoTipo getTipo() { return tipo; }
    public NotificacaoCanal getCanal() { return canal; }
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }

    public static class PK implements Serializable {
        private UUID usuarioId;
        private NotificacaoTipo tipo;
        private NotificacaoCanal canal;

        public PK() {}
        public PK(UUID usuarioId, NotificacaoTipo tipo, NotificacaoCanal canal) {
            this.usuarioId = usuarioId;
            this.tipo = tipo;
            this.canal = canal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(usuarioId, pk.usuarioId)
                    && tipo == pk.tipo
                    && canal == pk.canal;
        }

        @Override
        public int hashCode() { return Objects.hash(usuarioId, tipo, canal); }
    }
}
