package com.hubfeatcreators.domain.rbac;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {

  List<Perfil> findAllByAssessoriaId(UUID assessoriaId);

  Optional<Perfil> findByAssessoriaIdAndNome(UUID assessoriaId, String nome);

  @Query("SELECT COUNT(u) FROM Usuario u WHERE u.profileId = :perfilId AND u.deletedAt IS NULL")
  long countUsuariosUsando(@Param("perfilId") UUID perfilId);
}
