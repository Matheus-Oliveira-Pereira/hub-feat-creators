package com.hubfeatcreators.domain.rbac;

import com.hubfeatcreators.domain.usuario.Usuario;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria os perfis seed (Owner, Assessor, Leitor) ao criar uma assessoria nova, e atribui o perfil
 * correto ao usuário recém-criado conforme {@link Usuario#getRole()}.
 *
 * <p>Os mesmos perfis e roles também são criados pelo backfill da migration V3 para tenants
 * pré-existentes — esta classe é o caminho "novo signup".
 */
@Service
public class RbacBootstrap {

    private final PerfilRepository perfilRepo;

    public RbacBootstrap(PerfilRepository perfilRepo) {
        this.perfilRepo = perfilRepo;
    }

    /** Cria os 3 perfis seed para uma assessoria nova. Retorna o perfil "Owner". */
    @Transactional
    public Perfil seedAssessoria(UUID assessoriaId) {
        Perfil owner =
                perfilRepo.save(
                        new Perfil(
                                assessoriaId,
                                "Owner",
                                "Acesso total à assessoria",
                                PermissionCodes.OWNER_DEFAULT,
                                true));
        perfilRepo.save(
                new Perfil(
                        assessoriaId,
                        "Assessor",
                        "Operacional padrão de prospecção e cadastros",
                        PermissionCodes.ASSESSOR_DEFAULT,
                        true));
        perfilRepo.save(
                new Perfil(
                        assessoriaId,
                        "Leitor",
                        "Somente leitura",
                        PermissionCodes.LEITOR_DEFAULT,
                        true));
        return owner;
    }

    /** Resolve o perfil seed correspondente ao role coarse — usado em convite. */
    @Transactional(readOnly = true)
    public Perfil seedFor(UUID assessoriaId, Usuario.Role role) {
        String nome = role == Usuario.Role.OWNER ? "Owner" : "Assessor";
        return perfilRepo
                .findByAssessoriaIdAndNome(assessoriaId, nome)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Perfil seed '"
                                                + nome
                                                + "' não encontrado para assessoria "
                                                + assessoriaId));
    }
}
