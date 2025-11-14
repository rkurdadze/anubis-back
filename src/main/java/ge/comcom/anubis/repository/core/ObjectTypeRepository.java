package ge.comcom.anubis.repository.core;

import ge.comcom.anubis.entity.core.ObjectType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ObjectTypeRepository extends JpaRepository<ObjectType, Long> {

    boolean existsByVault_Id(Long vaultId);

    @Override
    @EntityGraph(attributePaths = {"vault", "acl"})
    List<ObjectType> findAll();

    @Override
    @EntityGraph(attributePaths = {"vault", "acl"})
    Optional<ObjectType> findById(Long id);

    Optional<ObjectType> findByNameIgnoreCase(String name);


    @Query("SELECT t.id, t.name FROM ObjectType t")
    List<Object[]> findAllPairs();

    default Map<Long, String> findAllNames() {
        Map<Long, String> map = new HashMap<>();
        for (Object[] row : findAllPairs()) {
            map.put((Long) row[0], (String) row[1]);
        }
        return map;
    }
}
