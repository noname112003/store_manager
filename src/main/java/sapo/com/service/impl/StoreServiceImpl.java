package sapo.com.service.impl;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sapo.com.model.entity.Store;
import sapo.com.model.entity.UserStore;
import sapo.com.repository.StoreRepository;
import sapo.com.repository.UserStoreRepository;
import sapo.com.service.StoreService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreServiceImpl implements StoreService {
    @Autowired
    private UserStoreRepository userStoreRepository;

    @Autowired
    private StoreRepository storeRepository;

    public List<Store> getStoresByUserId(Long userId) {
        // Lấy tất cả các mapping của userId từ bảng user_store_mapping
        List<UserStore> userStores = userStoreRepository.findByUserId(userId);

        // Lấy tất cả storeId từ kết quả
        List<Long> storeIds = userStores.stream()
                .map(UserStore::getStoreId)
                .collect(Collectors.toList());

        // Truy vấn các store từ bảng stores theo storeIds
        return storeRepository.findAllById(storeIds);
    }

    @Override
    public Store getStoreById(Long id) {
        return storeRepository.findById(id).orElse(null);
    }

    @Override
    public Store saveStore(Store store) {
        return storeRepository.save(store);
    }
}
