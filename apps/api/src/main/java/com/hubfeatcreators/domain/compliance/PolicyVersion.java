package com.hubfeatcreators.domain.compliance;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "policy_versions")
public class PolicyVersion {

    @Id
    private String versao;

    @Column(nullable = false, columnDefinition = "text")
    private String texto;

    @Column(nullable = false)
    private String hash;

    @Column(nullable = false)
    private boolean material = false;

    @Column(name = "vigente_de", nullable = false)
    private Instant vigenteDe = Instant.now();

    public PolicyVersion() {}

    public PolicyVersion(String versao, String texto, String hash, boolean material) {
        this.versao = versao;
        this.texto = texto;
        this.hash = hash;
        this.material = material;
    }

    public String getVersao() { return versao; }
    public String getTexto() { return texto; }
    public String getHash() { return hash; }
    public boolean isMaterial() { return material; }
    public Instant getVigenteDe() { return vigenteDe; }
}
