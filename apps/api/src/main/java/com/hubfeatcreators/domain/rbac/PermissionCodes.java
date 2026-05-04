package com.hubfeatcreators.domain.rbac;

import java.util.List;
import java.util.Set;

/**
 * Códigos de permissão 4-letter (ADR-015).
 *
 * <p>Formato: [B|C|E|D] + 3 letras de entidade. Roles especiais 4-letras quando justificado (OWNR,
 * INVT, EXPT, BLLG).
 *
 * <p>Lista canônica usada por aspect, frontend (sincronizar manualmente) e seed migration.
 */
public final class PermissionCodes {

    private PermissionCodes() {}

    // Especiais
    public static final String OWNR = "OWNR"; // bypass total
    public static final String INVT = "INVT"; // convidar usuário
    public static final String EXPT = "EXPT"; // exportar CSV/relatórios
    public static final String BLLG = "BLLG"; // browse audit log

    // Prospecção
    public static final String B_PRO = "BPRO";
    public static final String C_PRO = "CPRO";
    public static final String E_PRO = "EPRO";
    public static final String D_PRO = "DPRO";

    // Marca
    public static final String B_MAR = "BMAR";
    public static final String C_MAR = "CMAR";
    public static final String E_MAR = "EMAR";
    public static final String D_MAR = "DMAR";

    // Influenciador
    public static final String B_INF = "BINF";
    public static final String C_INF = "CINF";
    public static final String E_INF = "EINF";
    public static final String D_INF = "DINF";

    // Contato
    public static final String B_CON = "BCON";
    public static final String C_CON = "CCON";
    public static final String E_CON = "ECON";
    public static final String D_CON = "DCON";

    // Usuário
    public static final String B_USU = "BUSU";
    public static final String C_USU = "CUSU";
    public static final String E_USU = "EUSU";
    public static final String D_USU = "DUSU";

    // Perfil
    public static final String B_PRF = "BPRF";
    public static final String C_PRF = "CPRF";
    public static final String E_PRF = "EPRF";
    public static final String D_PRF = "DPRF";

    // Tarefa (PRD-003 futuro)
    public static final String B_TAR = "BTAR";
    public static final String C_TAR = "CTAR";
    public static final String E_TAR = "ETAR";
    public static final String D_TAR = "DTAR";

    // E-mail (PRD-004 futuro)
    public static final String B_EML = "BEML";
    public static final String C_EML = "CEML";
    public static final String E_EML = "EEML";
    public static final String D_EML = "DEML";

    // Relatórios / dashboard
    public static final String B_REL = "BREL";

    /** Conjunto de todas roles válidas — usado pra validar entrada em PerfilController. */
    public static final Set<String> ALL =
            Set.copyOf(
                    List.of(
                            OWNR, INVT, EXPT, BLLG, B_PRO, C_PRO, E_PRO, D_PRO, B_MAR, C_MAR, E_MAR,
                            D_MAR, B_INF, C_INF, E_INF, D_INF, B_CON, C_CON, E_CON, D_CON, B_USU,
                            C_USU, E_USU, D_USU, B_PRF, C_PRF, E_PRF, D_PRF, B_TAR, C_TAR, E_TAR,
                            D_TAR, B_EML, C_EML, E_EML, D_EML, B_REL));

    /** Default roles dos perfis seed (idêntico ao backfill da V3). */
    public static final Set<String> OWNER_DEFAULT = Set.copyOf(ALL);

    public static final Set<String> ASSESSOR_DEFAULT =
            Set.of(
                    B_PRO, C_PRO, E_PRO, B_MAR, B_INF, B_CON, C_CON, E_CON, B_USU, B_REL, B_TAR,
                    C_TAR, E_TAR, B_EML, C_EML, E_EML);

    public static final Set<String> LEITOR_DEFAULT =
            Set.of(B_PRO, B_MAR, B_INF, B_CON, B_USU, B_REL, B_TAR, B_EML);
}
