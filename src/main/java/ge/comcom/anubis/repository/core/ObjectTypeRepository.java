package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObjectTypeRepository extends JpaRepository<ObjectType, Long> {

    boolean existsByVault_Id(Long vaultId);

    @Override
    @EntityGraph(attributePaths = "vault")
    List<ObjectType> findAll();

    @Override
    @EntityGraph(attributePaths = "vault")
    Optional<ObjectType> findById(Long id);

    Optional<ObjectType> findByNameIgnoreCase(String name);
}
