package com.hubfeatcreators.domain.rbac;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {

    Optional<Perfil> findByNome(String nome);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.profileId = :perfilId AND u.deletedAt IS NULL")
    long countUsuariosUsando(@Param("perfilId") UUID perfilId);

    @Query(
            "SELECT u.profileId, COUNT(u) FROM Usuario u WHERE u.profileId IN :ids AND u.deletedAt IS NULL GROUP BY u.profileId")
    List<Object[]> countsByPerfilIds(@Param("ids") List<UUID> ids);
}
