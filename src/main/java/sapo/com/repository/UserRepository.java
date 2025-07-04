package sapo.com.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;
import sapo.com.model.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User , Long> {
    @Query("select u from User u where u.email = :email")
    User findByEmail(String email);

    @Query("select u from User u where u.name = :name")
    User findByName(String name);

    @Query("select u from User u where u.phoneNumber = :phoneNumber")
    User findByPhoneNumber(String phoneNumber);
    User findByNameAndPassword(String name, String password);
    Page<User> findAllByRolesName(String roleName, Pageable pageable);

    // Search by name or phone number
    @Query("SELECT u FROM User u WHERE u.name LIKE %:search% OR u.phoneNumber LIKE %:search%")
    Page<User> findBySearch( String search, Pageable pageable);

    // Filter by role and search by name or phone number
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role AND (u.name LIKE %:search% OR u.phoneNumber LIKE %:search%)")
    Page<User> findByRoleAndSearch( String role,  String search, Pageable pageable);

    // Find by role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role")
    Page<User> findByRole( String role, Pageable pageable);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
    SELECT u FROM User u
    JOIN UserStore us ON u.id = us.userId
    WHERE (:storeId IS NULL OR us.storeId = :storeId)
    AND (
        (:role IS NOT NULL AND :role <> '' AND EXISTS (
            SELECT 1 FROM u.roles r WHERE r.name = :role
        ))
        OR
        ((:role IS NULL OR :role = '') AND EXISTS (
            SELECT 1 FROM u.roles r WHERE r.name <> 'ROLE_ADMIN'
        ))
    )
    AND (:search IS NULL OR u.name LIKE %:search% OR u.email LIKE %:search% OR u.phoneNumber LIKE %:search%)
""")
    Page<User> findUsersByFilter(@Param("storeId") Long storeId,
                                 @Param("role") String role,
                                 @Param("search") String search,
                                 Pageable pageable);
}
