package sapo.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sapo.com.model.entity.Variant;
import sapo.com.model.entity.VariantStore;

import java.util.List;

public interface VariantStoreRepositry extends JpaRepository<VariantStore, Long> {
    List<VariantStore> findByVariantId(Long variantId);
    List<VariantStore> findByVariantIdAndStoreId(Long variantId, Long storeId);
}
