package com.hubfeatcreators.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NULL AND LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findActiveByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NULL ORDER BY u.createdAt")
    List<Usuario> findAllActive();

    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    Page<Usuario> findAllActivePaged(Pageable pageable);

    boolean existsByRoleAndDeletedAtIsNull(Usuario.Role role);
}
