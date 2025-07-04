package sapo.com.controller.store;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sapo.com.model.dto.request.StoreRequest;
import sapo.com.model.dto.response.StoreResponse;
import sapo.com.model.entity.Store;
import sapo.com.model.entity.UserStore;
import sapo.com.repository.UserStoreRepository;
import sapo.com.service.StoreService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @Autowired
    private UserStoreRepository userStoreRepository;

    // API lấy danh sách store theo userId
    @GetMapping("/get_list_store")
    public ResponseEntity<Object> getStoresByUserId(@RequestParam Long userId) {
        List<Store> stores = storeService.getStoresByUserId(userId);
        if (stores == null || stores.isEmpty()) {
            // Trả về thông báo chi tiết khi không tìm thấy store nào
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "No stores found for user with ID: " + userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        return ResponseEntity.ok(stores);
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<Object> updateStore(@PathVariable Long storeId, @RequestBody StoreRequest storeRequest) {
        Store existingStore = storeService.getStoreById(storeId);
        if (existingStore == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Store not found with ID " + storeId));
        }

        // Map từ storeRequest sang entity
        existingStore.setName(storeRequest.getName());
        existingStore.setAddress(storeRequest.getAddress());
        existingStore.setPhone(storeRequest.getPhone());
        existingStore.setStatus(storeRequest.isStatus());
        existingStore.setCity(storeRequest.getCity());
        existingStore.setDistrict(storeRequest.getDistrict());
        existingStore.setWard(storeRequest.getWard());
        existingStore.setModifiedOn(System.currentTimeMillis());

        Store savedStore = storeService.saveStore(existingStore);

        // Map entity sang response

        return ResponseEntity.ok(savedStore.transferToResponse());
    }


    @PostMapping
    public ResponseEntity<Object> createStore(@RequestBody StoreRequest storeRequest) {
        Store newStore = new Store();
        newStore.setName(storeRequest.getName());
        newStore.setAddress(storeRequest.getAddress());
        newStore.setPhone(storeRequest.getPhone());
        newStore.setStatus(true);
        newStore.setCity(storeRequest.getCity());
        newStore.setDistrict(storeRequest.getDistrict());
        newStore.setWard(storeRequest.getWard());
        newStore.setCreatedAt(System.currentTimeMillis());
        newStore.setModifiedOn(System.currentTimeMillis());

        Store savedStore = storeService.saveStore(newStore);
        if (storeRequest.getUserId() != null) {
            UserStore mapping = new UserStore();
            mapping.setUserId(storeRequest.getUserId());
            mapping.setStoreId(savedStore.getId());
            userStoreRepository.save(mapping);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStore.transferToResponse());
    }
}
