package sapo.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sapo.com.model.entity.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
