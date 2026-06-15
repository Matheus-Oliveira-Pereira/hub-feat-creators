package com.hubfeatcreators.domain.rbac;

import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepo;

    public PerfilService(PerfilRepository perfilRepo) {
        this.perfilRepo = perfilRepo;
    }

    @Transactional(readOnly = true)
    public List<Perfil> listar() {
        return perfilRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Perfil buscar(UUID id) {
        return perfilRepo.findById(id).orElseThrow(() -> BusinessException.notFound("PERFIL"));
    }

    @Transactional
    public Perfil criar(String nome, String descricao, Set<String> roles) {
        validateRoles(roles);
        perfilRepo
                .findByNome(nome)
                .ifPresent(
                        p -> {
                            throw BusinessException.conflict(
                                    "PERFIL_EM_USO", "Já existe perfil com esse nome.");
                        });
        Perfil p = new Perfil(nome, descricao, roles, false);
        return perfilRepo.save(p);
    }

    @Transactional
    public Perfil atualizar(UUID id, String nome, String descricao, Set<String> roles) {
        Perfil p = buscar(id);
        if (p.isSystem() && !p.getNome().equals(nome)) {
            throw BusinessException.badRequest(
                    "PERFIL_SISTEMA", "Perfis do sistema não podem ter nome alterado.");
        }
        if (p.isSystem() && !p.rolesAsSet().equals(roles)) {
            throw BusinessException.badRequest(
                    "PERFIL_SISTEMA", "Roles de perfis do sistema não podem ser alteradas.");
        }
        validateRoles(roles);
        p.setNome(nome);
        p.setDescricao(descricao);
        p.setRoles(roles);
        p.setUpdatedAt(Instant.now());
        return perfilRepo.save(p);
    }

    @Transactional
    public void deletar(UUID id) {
        Perfil p = buscar(id);
        if (p.isSystem()) {
            throw BusinessException.badRequest(
                    "PERFIL_SISTEMA", "Perfis do sistema não podem ser removidos.");
        }
        long usuarios = perfilRepo.countUsuariosUsando(id);
        if (usuarios > 0) {
            throw BusinessException.conflict(
                    "PERFIL_EM_USO",
                    "Perfil em uso por " + usuarios + " usuário(s); reatribua antes de remover.");
        }
        p.setDeletedAt(Instant.now());
        perfilRepo.save(p);
    }

    private void validateRoles(Set<String> roles) {
        for (String r : roles) {
            if (!PermissionCodes.ALL.contains(r)) {
                throw BusinessException.badRequest("ROLE_INVALIDA", "Role desconhecida: " + r);
            }
        }
    }
}
