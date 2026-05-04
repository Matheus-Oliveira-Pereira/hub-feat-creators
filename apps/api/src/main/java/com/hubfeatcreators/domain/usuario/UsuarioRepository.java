package com.hubfeatcreators.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.assessoriaId = :assessoriaId AND u.email = :email AND u.deletedAt IS NULL")
    Optional<Usuario> findActiveByAssessoriaIdAndEmail(UUID assessoriaId, String email);

    @Query("SELECT u FROM Usuario u WHERE u.assessoriaId = :assessoriaId AND u.deletedAt IS NULL ORDER BY u.createdAt")
    List<Usuario> findAllByAssessoriaId(UUID assessoriaId);

    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.assessoriaId = :assessoriaId AND u.deletedAt IS NULL")
    Optional<Usuario> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);
}
