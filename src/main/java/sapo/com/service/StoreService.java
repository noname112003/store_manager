package sapo.com.service;

import org.springframework.data.jpa.repository.JpaRepository;
import sapo.com.model.entity.Store;

import java.util.List;

public interface StoreService {

    public List<Store> getStoresByUserId(Long userId);

    public Store getStoreById(Long id);
    public Store saveStore(Store store);

}
