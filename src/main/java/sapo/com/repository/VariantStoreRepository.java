package sapo.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sapo.com.model.entity.VariantStore;

import java.util.List;
import java.util.Set;

public interface VariantStoreRepository extends JpaRepository<VariantStore, Long> {
    List<VariantStore> findByVariantId(Long variantId);
    List<VariantStore> findByVariantIdAndStoreId(Long variantId, Long storeId);

    @Query("SELECT vs FROM VariantStore vs WHERE vs.variant.product.id = :productId")
    List<VariantStore> findByProductId(@Param("productId") Long productId);
    @Query("SELECT vs FROM VariantStore vs WHERE vs.variant.product.id = :productId AND vs.storeId = :storeId")
    List<VariantStore> findByProductIdAndStoreId(@Param("productId") Long productId, @Param("storeId") Long storeId);
    List<VariantStore> findAllByVariant_IdInAndStoreId(Set<Long> variantIds, Long storeId);
    List<VariantStore> findAllByVariantIdInAndStoreId(List<Long> variantIds, Long storeId);

}
