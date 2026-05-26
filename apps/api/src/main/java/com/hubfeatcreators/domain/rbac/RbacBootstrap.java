package com.hubfeatcreators.domain.rbac;

import com.hubfeatcreators.domain.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures seed perfis (Owner, Assessor, Leitor) exist at startup. In single-tenant mode there is
 * one global set of perfis — no assessoria scoping. Migration V20 seeds them via SQL; this class
 * provides a programmatic fallback and a helper to look up the right seed perfil for a user role.
 */
@Service
public class RbacBootstrap {

    private final PerfilRepository perfilRepo;

    public RbacBootstrap(PerfilRepository perfilRepo) {
        this.perfilRepo = perfilRepo;
    }

    /** Ensures system perfis exist. Returns the Owner perfil. */
    @Transactional
    public Perfil ensureSystemPerfis() {
        Perfil owner =
                perfilRepo
                        .findByNome("Owner")
                        .orElseGet(
                                () ->
                                        perfilRepo.save(
                                                new Perfil(
                                                        "Owner",
                                                        "Acesso total",
                                                        PermissionCodes.OWNER_DEFAULT,
                                                        true)));
        perfilRepo
                .findByNome("Assessor")
                .orElseGet(
                        () ->
                                perfilRepo.save(
                                        new Perfil(
                                                "Assessor",
                                                "Operacional padrão de prospecção e cadastros",
                                                PermissionCodes.ASSESSOR_DEFAULT,
                                                true)));
        perfilRepo
                .findByNome("Leitor")
                .orElseGet(
                        () ->
                                perfilRepo.save(
                                        new Perfil(
                                                "Leitor",
                                                "Somente leitura",
                                                PermissionCodes.LEITOR_DEFAULT,
                                                true)));
        return owner;
    }

    /** Resolves the seed perfil matching the user's coarse role. */
    @Transactional(readOnly = true)
    public Perfil seedFor(Usuario.Role role) {
        String nome = role == Usuario.Role.OWNER ? "Owner" : "Assessor";
        return perfilRepo
                .findByNome(nome)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Perfil seed '" + nome + "' não encontrado"));
    }
}
