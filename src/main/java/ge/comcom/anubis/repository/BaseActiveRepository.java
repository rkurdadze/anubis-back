package ge.comcom.anubis.repository;

import ge.comcom.anubis.entity.ActivatableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface BaseActiveRepository<T extends ActivatableEntity, ID> extends JpaRepository<T, ID> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.isActive = true")
    List<T> findAllActive();

    default void deactivate(T entity) {
        entity.setIsActive(false);
        save(entity);
    }
}

