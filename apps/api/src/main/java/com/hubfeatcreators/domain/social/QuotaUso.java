package com.hubfeatcreators.domain.social;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "quota_uso")
public class QuotaUso {

    @Embeddable
    public static class Pk implements Serializable {
        private String plataforma;
        private LocalDate dia;

        protected Pk() {}

        public Pk(String plataforma, LocalDate dia) {
            this.plataforma = plataforma;
            this.dia = dia;
        }

        public String getPlataforma() {
            return plataforma;
        }

        public LocalDate getDia() {
            return dia;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return plataforma.equals(pk.plataforma) && dia.equals(pk.dia);
        }

        @Override
        public int hashCode() {
            return 31 * plataforma.hashCode() + dia.hashCode();
        }
    }

    @EmbeddedId private Pk id;

    @Column(name = "units_usados", nullable = false)
    private long unitsUsados = 0;

    protected QuotaUso() {}

    public QuotaUso(String plataforma, LocalDate dia) {
        this.id = new Pk(plataforma, dia);
    }

    public Pk getId() {
        return id;
    }

    public String getPlataforma() {
        return id.getPlataforma();
    }

    public LocalDate getDia() {
        return id.getDia();
    }

    public long getUnitsUsados() {
        return unitsUsados;
    }

    public void addUnits(long units) {
        this.unitsUsados += units;
    }
}
