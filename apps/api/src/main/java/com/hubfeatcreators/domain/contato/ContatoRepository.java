package com.hubfeatcreators.domain.contato;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoRepository extends JpaRepository<Contato, UUID> {
    Optional<Contato> findByIdAndDeletedAtIsNull(UUID id);

    List<Contato> findByMarcaIdAndDeletedAtIsNull(UUID marcaId);
}
