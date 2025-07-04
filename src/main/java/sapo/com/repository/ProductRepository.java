package sapo.com.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;

import sapo.com.model.entity.Product;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("update Product p set p.status = false where p.id = :id")
    int deleteProductById(@Param("id") Long id);

    @Query(
            value = "CALL get_list_of_products(:page, :limit, :query)",
            nativeQuery = true
    )
    Set<Product> getListOfProducts(Long page, Long limit, String query );

    @EntityGraph(attributePaths = {
            "brand",
            "category",
            "imagePath",
            "variants",
            "variants.variantStores"
    })
    @Query("""
    SELECT DISTINCT p FROM Product p
    WHERE p.status = true
    AND (:query IS NULL OR :query = '' OR p.name LIKE %:query%)
    ORDER BY p.updatedOn DESC
""")
    List<Product> findProductsWithVariantsAndStores(
            @Param("query") String query,
            Pageable pageable
    );

    Long countByNameContainingAndStatus(String name, boolean status);
    @Query("SELECT p FROM Product p JOIN p.variants v WHERE v.id = :variantId")
    Product findByVariantId(Long variantId);

    @EntityGraph(attributePaths = {
            "brand",
            "category",
            "imagePath",
            "variants"
    })
    @Query("""
    SELECT DISTINCT p FROM Product p
    WHERE p.status = true
    AND (:query IS NULL OR :query = '' OR p.name LIKE %:query%)
    ORDER BY p.updatedOn DESC
""")
    List<Product> findProductsBasicInfo(@Param("query") String query, Pageable pageable);



}

