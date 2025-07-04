package sapo.com.repository;

import org.aspectj.weaver.ast.Var;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sapo.com.model.entity.Product;
import sapo.com.model.entity.Variant;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface VariantRepository extends JpaRepository<Variant, Long> {

    @Query("SELECT DISTINCT v.size FROM Variant v WHERE v.product.id = :productId AND v.status = true AND v.size <> ''")
    Set<String> findDistinctSizesByProductId(@Param("productId") Long productId);

    @Query("SELECT DISTINCT v.color FROM Variant v WHERE v.product.id = :productId AND v.status = true AND v.color <> ''")
    Set<String> findDistinctColorsByProductId(@Param("productId") Long productId);

    @Query("SELECT DISTINCT v.material FROM Variant v WHERE v.product.id = :productId AND v.status = true AND v.material <> ''")

    Set<String> findDistinctMaterialsByProductId(@Param("productId") Long productId);

    @Query(value = """
    SELECT DISTINCT v.size, v.color, v.material
    FROM variants v
    WHERE v.product_id = :productId AND v.status = true
""", nativeQuery = true)
    List<Object[]> findDistinctSizeColorMaterial(@Param("productId") Long productId);

    @Modifying
    @Query("update Variant v set v.status = false where v.id = :id")
    int deleteVariantById(@Param("id") Long id);

    @Modifying
    @Query("update Variant v set v.status = false where v.product.id = :productId")
    int deleteAllVariantOfProduct(Long productId);

    @Modifying
    @Query("update Variant v set v.status = false where v.product.id = :productId AND v.size = :value")
    int deleteVariantBySize(Long productId, String value);

    @Modifying
    @Query("update Variant v set v.status = false where v.product.id = :productId AND v.color = :value")
    int deleteVariantByColor(Long productId, String value);

    @Modifying
    @Query("update Variant v set v.status = false where v.product.id = :productId AND v.material = :value")
    int deleteVariantByMaterial(Long productId, String value);

    @Query(
            value = "CALL get_list_of_variants(:page, :limit, :query)",
            nativeQuery = true
    )
    Set<Variant> getListOfVariants(Long page, Long limit, String query );

    @Query(
            value = """
        SELECT * FROM variants
        WHERE status = true
        AND (:query IS NULL OR :query = '' OR name LIKE %:query% OR sku LIKE %:query%)
        ORDER BY updated_on DESC
        LIMIT :limit OFFSET :offset
    """,
            nativeQuery = true
    )
    List<Variant> findVariantsBySearch(
            @Param("query") String query,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
    @EntityGraph(attributePaths = {"variantStores", "product"})
    @Query("""
    SELECT v FROM Variant v
    WHERE v.status = true
    AND (:query IS NULL OR :query = '' OR v.name LIKE %:query% OR v.sku LIKE %:query%)
    ORDER BY v.updatedOn DESC
""")
    List<Variant> findWithVariantStores(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"variantStores", "product"})
    @Query("""
    SELECT v FROM Variant v
    WHERE v.status = true
    AND (:query IS NULL OR :query = '' OR v.name LIKE %:query% OR v.sku LIKE %:query%)
    ORDER BY v.updatedOn DESC
""")
    List<Variant> findWithVariantStoresV2(@Param("query") String query);
    @EntityGraph(attributePaths = {"variantStores", "product"})
    @Query("""
    SELECT v FROM Variant v
    WHERE v.status = true AND v.product.id IN :productIds
    AND (:query IS NULL OR :query = '' OR v.name LIKE %:query% OR v.sku LIKE %:query%)
""")
    List<Variant> findWithVariantStoresByProductIds(@Param("productIds") List<Long> productIds, @Param("query") String query);
    boolean existsBySku(String sku);

    Optional<Variant> findByIdAndProductId(Long id, Long productId);

    Long countByNameContainingAndStatus(String name, boolean status);
}

