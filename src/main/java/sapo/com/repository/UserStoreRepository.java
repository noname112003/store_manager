package sapo.com.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sapo.com.model.entity.UserStore;

import java.util.List;

public interface UserStoreRepository extends JpaRepository<UserStore, Long> {
    List<UserStore> findByUserId(Long userId);

    @Query("SELECT us.storeId FROM UserStore us WHERE us.userId = :userId")
    List<Long> findStoreIdsByUserId(@Param("userId") Long userId);
    @Query("SELECT us FROM UserStore us WHERE us.userId IN :userIds")
    List<UserStore> findAllByUserIds(@Param("userIds") List<Long> userIds);

    List<UserStore> findByUserIdAndStoreId(Long userId, Long storeId);


}
