package sapo.com.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import sapo.com.exception.DataConflictException;
import sapo.com.exception.ResourceNotFoundException;
import sapo.com.model.dto.request.ProductRequest;
import sapo.com.model.dto.request.VariantRequest;
import sapo.com.model.dto.request.VariantStoreRequest;
import sapo.com.model.dto.response.ProductResponse;
import sapo.com.model.dto.response.StoreQuantityDto;
import sapo.com.model.dto.response.VariantResponse;
import sapo.com.model.entity.*;
import sapo.com.repository.*;
import sapo.com.service.ProductService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ImagePathRepository imagePathRepository;

    @Autowired
    private VariantStoreRepository variantStoreRepository;

    public List<ProductResponse> getListOfProducts(Long page, Long limit, String queryString, Long storeId) {
        Set<Product> products = productRepository.getListOfProducts(page + 1, limit, queryString);
        List<ProductResponse> productsResponse = new ArrayList<>();
        for (Product product : products) {
            ProductResponse productResponse = product.transferToResponse();

            // Lặp qua các variant trong sản phẩm
            List<VariantResponse> variantResponses = new ArrayList<>();
            for (Variant variant : product.getVariants()) {

                    System.out.println("Variant ID: " + variant.getId());
                    System.out.println("Name: " + variant.getName());
                    System.out.println("SKU: " + variant.getSku());
                    System.out.println("Product ID: " + (variant.getProduct() != null ? variant.getProduct().getId() : "null"));
                    System.out.println("Product Name: " + (variant.getProduct() != null ? variant.getProduct().getName() : "null"));

                VariantResponse variantResponse = variant.transferToResponse();

                List<VariantStore> variantStores;
                if (storeId != null) {
                    variantStores = variantStoreRepository.findByVariantIdAndStoreId(variant.getId(), storeId);
                } else {
                    variantStores = variantStoreRepository.findByVariantId(variant.getId());
                }

                List<StoreQuantityDto> storeDtos = variantStores.stream()
                        .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
                        .collect(Collectors.toList());

                variantResponse.setVariantStores(storeDtos);
                variantResponses.add(variantResponse);
            }

            productResponse.setVariants(variantResponses);
            productsResponse.add(productResponse);
        }
        if (!productsResponse.isEmpty())
            return productsResponse;
        else throw new ResourceNotFoundException("Sản phẩm không tồn tại");
    }

//    public List<ProductResponse> newGetListOfProducts(Long page, Long limit, String queryString, Long storeId) {
//        if (page == null || page < 1) page = 1L;
//        if (limit == null || limit < 1) limit = 10L;
//        Pageable pageable = PageRequest.of(page.intValue() - 1, limit.intValue());
//
//        List<Product> products = productRepository.findProductsWithVariantsAndStores(queryString, pageable);
//        if (products.isEmpty()) throw new ResourceNotFoundException("Sản phẩm không tồn tại");
//
//        return products.stream().map(product -> {
//            ProductResponse productResponse = product.transferToResponse();
//            List<VariantResponse> variantResponses = product.getVariants().stream().map(variant -> {
//                VariantResponse variantResponse = variant.transferToResponse();
//
//                // Filter theo storeId nếu có
//                List<StoreQuantityDto> storeDtos = variant.getVariantStores().stream()
//                        .filter(vs -> storeId == null || storeId.equals(vs.getStoreId()))
//                        .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
//                        .collect(Collectors.toList());
//
//                variantResponse.setVariantStores(storeDtos);
//                return variantResponse;
//            }).collect(Collectors.toList());
//
//            productResponse.setVariants(variantResponses);
//            return productResponse;
//        }).collect(Collectors.toList());
//    }
public List<ProductResponse> getProductResponses(Long page, Long limit, String queryString, Long storeId) {
    if (page == null || page < 0) page = 0L;
    Pageable pageable = PageRequest.of(page.intValue(), limit.intValue());

    List<Product> products = productRepository.findProductsBasicInfo(queryString, pageable);
    if (products.isEmpty()) throw new ResourceNotFoundException("Sản phẩm không tồn tại");

    List<Long> productIds = products.stream().map(Product::getId).toList();
    List<Variant> variants = variantRepository.findWithVariantStoresV2("");
    for (Variant v : variants) {
        System.out.println("Variant ID: " + v.getId());
        System.out.println("Name: " + v.getName());
        System.out.println("SKU: " + v.getSku());
        System.out.println("Product ID: " + (v.getProduct() != null ? v.getProduct().getId() : "null"));
        System.out.println("Product Name: " + (v.getProduct() != null ? v.getProduct().getName() : "null"));

    }
    // Nhóm variant theo productId
    Map<Long, List<Variant>> variantMap = variants.stream()
            .collect(Collectors.groupingBy(v -> v.getProduct().getId()));

    return products.stream().map(product -> {
        ProductResponse productResponse = product.transferToResponse();

        List<VariantResponse> variantResponses = variantMap.getOrDefault(product.getId(), List.of())
                .stream()
                .map(variant -> {
                    VariantResponse response = variant.transferToResponse();
                    List<StoreQuantityDto> storeDtos = variant.getVariantStores().stream()
                            .filter(vs -> storeId == null || vs.getStoreId().equals(storeId))
                            .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
                            .toList();
                    response.setVariantStores(storeDtos);
                    return response;
                }).toList();

        productResponse.setVariants(variantResponses);
        return productResponse;
    }).toList();
}


    public Long getNumberOfProducts(String queryString) {
        return productRepository.countByNameContainingAndStatus(queryString, true);
    }

    public List<VariantResponse> getListOfVariants(Long page, Long limit, String queryString, Long storeId) {
//        Set<Variant> variants = variantRepository.getListOfVariants(page+1, limit, queryString);
        // Validate & xử lý phân trang
        if (page == null || page < 1) page = 1L;
        if (limit == null || limit < 1) limit = 10L;

        int offset = (int) ((page - 1) * limit);
        List<Variant> variants = variantRepository.findVariantsBySearch(queryString, limit.intValue(), offset);
        List<VariantResponse> variantsResponse = new ArrayList<>();
        for (Variant variant : variants) {
            VariantResponse response = variant.transferToResponse();

            List<VariantStore> variantStores;
            if (storeId != null) {
                // Chỉ lấy theo storeId được truyền
                variantStores = variantStoreRepository.findByVariantIdAndStoreId(variant.getId(), storeId);
            } else {
                // Lấy tất cả các store
                variantStores = variantStoreRepository.findByVariantId(variant.getId());
            }

            List<StoreQuantityDto> storeDtos = variantStores.stream()
                    .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
                    .collect(Collectors.toList());

            response.setVariantStores(storeDtos);
            variantsResponse.add(response);
        }
        if (!variantsResponse.isEmpty())
            return variantsResponse;
        else throw new ResourceNotFoundException("Phiên bản không tồn tại");
    }

    public List<VariantResponse> newGetListOfVariants(Long page, Long limit, String queryString, Long storeId) {
        if (page == null || page < 0) page = 0L;
        Pageable pageable = PageRequest.of(page.intValue(), limit.intValue());


        List<Variant> variants = variantRepository.findWithVariantStores(queryString, pageable);
        for (Variant v : variants) {
            System.out.println("Variant ID: " + v.getId());
            System.out.println("Name: " + v.getName());
            System.out.println("SKU: " + v.getSku());
            System.out.println("Product ID: " + (v.getProduct() != null ? v.getProduct().getId() : "null"));
            System.out.println("Product Name: " + (v.getProduct() != null ? v.getProduct().getName() : "null"));

        }
        if (variants.isEmpty()) {
            throw new ResourceNotFoundException("Phiên bản không tồn tại");
        }

        List<VariantResponse> variantsResponse = new ArrayList<>();
        for (Variant variant : variants) {
            VariantResponse response = variant.transferToResponse();

            List<StoreQuantityDto> storeDtos = variant.getVariantStores().stream()
                    .filter(vs -> storeId == null || vs.getStoreId().equals(storeId))
                    .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
                    .collect(Collectors.toList());

            response.setVariantStores(storeDtos);
            variantsResponse.add(response);
        }

        return variantsResponse;
    }
    public Long getNumberOfVariants(String queryString) {
        return variantRepository.countByNameContainingAndStatus(queryString, true);
    }

    public ProductResponse getProductById(Long id, Long storeId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa"));

        if (!product.getStatus()) {
            throw new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa");
        }

        // Lọc variant có status = true
        product.getVariants().removeIf(variant -> !variant.getStatus());

        // Convert sang response
        ProductResponse productResponse = product.transferToResponse();

        // Duyệt từng variant để lấy variantStore ứng với storeId
        if (productResponse.getVariants() != null) {
            for (VariantResponse variantResponse : productResponse.getVariants()) {
                List<VariantStore> variantStores;
                if (storeId != null) {
                    variantStores = variantStoreRepository.findByVariantIdAndStoreId(variantResponse.getId(), storeId);
                } else {
                    variantStores = variantStoreRepository.findByVariantId(variantResponse.getId());
                }

                List<StoreQuantityDto> storeDtos = variantStores.stream()
                        .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
                        .toList();

                // Gán vào variantResponse
                variantResponse.setVariantStores(storeDtos);

            }
        }

        statisticSizeColorMaterial(productResponse);
        return productResponse;
    }

    public ProductResponse getProductByIdV2(Long id, Long storeId) {
        // 1. Tìm product và validate
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa"));

        if (!product.getStatus()) {
            throw new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa");
        }

        // 2. Lọc variants đã disable
        product.getVariants().removeIf(variant -> !variant.getStatus());

        // 3. Convert sang response DTO
        ProductResponse productResponse = product.transferToResponse();

        // 4. Truy vấn variant_store 1 lần duy nhất — theo productId + storeId hoặc chỉ productId
        List<VariantStore> variantStores = (storeId != null)
                ? variantStoreRepository.findByProductIdAndStoreId(product.getId(), storeId)
                : variantStoreRepository.findByProductId(product.getId());

        // 5. Ánh xạ variantStore thành map để set vào DTO
        Map<Long, List<StoreQuantityDto>> variantStoreDtoMap = mapVariantStoreDtos(variantStores);

        for (VariantResponse variantResponse : productResponse.getVariants()) {
            variantResponse.setVariantStores(
                    variantStoreDtoMap.getOrDefault(variantResponse.getId(), List.of())
            );
        }

        // 6. Lấy size/color/material thống kê
        statisticSizeColorMaterial(productResponse);

        return productResponse;
    }


    private Map<Long, List<StoreQuantityDto>> mapVariantStoreDtos(List<VariantStore> stores) {
        return stores.stream().collect(Collectors.groupingBy(
                vs -> vs.getVariant().getId(),
                Collectors.mapping(
                        vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()),
                        Collectors.toList()
                )
        ));
    }

    public VariantResponse getVariantById(Long productId, Long variantId) {
        Variant variant = variantRepository.findByIdAndProductId(productId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiên bản không tồn tại hoặc đã bị xóa"));
        if (variant.getStatus()) {
            VariantResponse variantResponse = variant.transferToResponse();
            return variantResponse;
        } else
            throw new ResourceNotFoundException("Phiên bản không tồn tại hoặc đã bị xóa");
    }

    @Transactional
    public ProductResponse createNewProduct(ProductRequest productRequest) {
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Loại sản phẩm không tồn tại hoặc đã bị xóa"));
        Brand brand = brandRepository.findById(productRequest.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhãn hiệu không tồn tại hoặc đã bị xóa"));
        //Check sku and variant
        Set<String> variantPropertiesSet = new HashSet<>();
        for (VariantRequest variantRequest : productRequest.getVariants()) {
            String sku = variantRequest.getSku();
            if (sku != "" && sku.startsWith("PVN")) {
                throw new DataConflictException("Mã loại không được có tiền tố " + "PVN");
            }
            if (variantRepository.existsBySku(sku)) {
                throw new DataConflictException("SKU " + sku + " đã tồn tại.");
            }
            String variantKey = variantRequest.getSize() + "-" +
                    variantRequest.getColor() + "-" +
                    variantRequest.getMaterial();
            // Check if combination of size, color, and material is unique
            if (!variantPropertiesSet.add(variantKey)) {
                throw new DataConflictException("Thuộc tính đã bị trùng: " + variantKey);
            }
        }
        // Map ProductRequest to Product entity
        Product product = productRequest.transferToProduct();

        product.setBrand(brand);
        product.setCategory(category);
        Product savedProduct = productRepository.save(product);
        // Lưu VariantStore theo từng variant và từng store
        for (int i = 0; i < productRequest.getVariants().size(); i++) {
            Variant variant = savedProduct.getVariants().get(i);
            VariantRequest variantRequest = productRequest.getVariants().get(i);

            for (VariantStoreRequest vsr : variantRequest.getVariantStores()) {
                VariantStore vs = new VariantStore();
                vs.setVariant(variant);
                vs.setStoreId(vsr.getStoreId());
                vs.setQuantity(vsr.getQuantity());
                variantStoreRepository.save(vs);
            }
        }
        entityManager.refresh(savedProduct);
        ProductResponse productResponse = savedProduct.transferToResponse();
        statisticSizeColorMaterial(productResponse);
        return productResponse;
    }

    @Transactional
    public VariantResponse createNewVariant(Long productId, VariantRequest variantRequest) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa"));
        //Check sku
        String sku = variantRequest.getSku();
        if (sku != "" && sku.startsWith("PVN")) {
            throw new DataConflictException("Mã loại không được có tiền tố " + "PVN");
        }
        if (variantRepository.existsBySku(sku)) {
            throw new DataConflictException("SKU " + sku + " đã tồn tại.");
        }
        //check variant
        String newVariantKey = variantRequest.getSize() + "-" +
                variantRequest.getColor() + "-" +
                variantRequest.getMaterial();
        for (Variant variant : product.getVariants()) {
            if(variant.getSize().isEmpty()&&variant.getColor().isEmpty()&&variant.getMaterial().isEmpty()){
                variant.updateFromRequest(variantRequest);
                Variant savedVariant = variantRepository.saveAndFlush(variant);
                product.setUpdatedOn(LocalDateTime.now());
                productRepository.saveAndFlush(product);
                entityManager.refresh(savedVariant);
                return savedVariant.transferToResponse();
            }
            if(variant.getSize().isEmpty()&&!variantRequest.getSize().isEmpty()){
                variant.setSize(variantRequest.getSize());
            }
            if(variant.getColor().isEmpty()&&!variantRequest.getColor().isEmpty()){
                variant.setColor(variantRequest.getColor());
            }
            if(variant.getMaterial().isEmpty()&&!variantRequest.getMaterial().isEmpty()){
                variant.setMaterial(variantRequest.getMaterial());
            }
            String variantKey = variant.getSize() + "-" +
                    variant.getColor() + "-" +
                    variant.getMaterial();
            if (newVariantKey.equals(variantKey)) {
                throw new DataConflictException("Thuộc tính đã bị trùng: " + variantKey);
            }
        }
        variantRequest.setProductId(productId);
        Variant variant = variantRequest.transferToVariant();
        variant.setProduct(product);
        Variant savedVariant = variantRepository.save(variant);
        product.setUpdatedOn(LocalDateTime.now());
        product.setTotalQuantity(product.getTotalQuantity() + variant.getQuantity());
        productRepository.saveAndFlush(product);
        entityManager.refresh(savedVariant);
        return savedVariant.transferToResponse();
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest, Long storeId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa"));
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Loại sản phẩm không tồn tại hoặc đã bị xóa"));
        Brand brand = brandRepository.findById(productRequest.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhãn hiệu không tồn tại hoặc đã bị xóa"));
        //check properties of variant
        Set<String> variantPropertiesSet = new HashSet<>();
        Long totalQuantity = 0L;
        for (VariantRequest variantRequest : productRequest.getVariants()) {
            Variant variant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(variantRequest.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Loại sản phẩm không tồn tại"));

            // Tìm variantStore có storeId phù hợp
            VariantStoreRequest matchingStoreRequest = variantRequest.getVariantStores().stream()
                    .filter(vs -> vs.getStoreId().equals(storeId))
                    .findFirst()
                    .orElse(null);
            System.out.println("quang "+ matchingStoreRequest);

            if (matchingStoreRequest != null) {
                // Kiểm tra đã tồn tại VariantStore cho variantId + storeId chưa
                List<VariantStore> variantStores = variantStoreRepository.findByVariantIdAndStoreId(variant.getId(), storeId);
                VariantStore variantStore = variantStores.isEmpty()
                        ? VariantStore.builder().variant(variant).storeId(storeId).build()
                        : variantStores.get(0);

                variantStore.setQuantity(matchingStoreRequest.getQuantity());
                System.out.println("quang"+ variantStore);
                variantStoreRepository.save(variantStore);
            }
            //
            String variantKey = variantRequest.getSize() + "-" +
                    variantRequest.getColor() + "-" +
                    variantRequest.getMaterial();
            // Check if combination of size, color, and material is unique
            if (!variantPropertiesSet.add(variantKey)) {
                throw new DataConflictException("Thuộc tính đã bị trùng: " + variantKey);
            }
            totalQuantity += variantRequest.getQuantity();
        }
        product.setTotalQuantity(totalQuantity);

        product.setBrand(brand);
        product.setCategory(category);
        product.setDescription(productRequest.getDescription());
        product.setName(productRequest.getName());
        Map<Long, VariantRequest> existingVariantsMap = productRequest.getVariants().stream()
                .collect(Collectors.toMap(VariantRequest::getId, Function.identity()));
        //check sku of variants

        for (Variant variant : product.getVariants()) {
            String newSku = existingVariantsMap.get(variant.getId()).getSku();
            if (newSku != "" && !newSku.equals(variant.getSku())) {
                if (newSku.startsWith("PVN")) {
                    throw new DataConflictException("Mã loại không được có tiền tố " + "PVN");
                }
                if (variantRepository.existsBySku(newSku)) {
                    throw new DataConflictException("SKU " + newSku + " đã tồn tại.");
                }
            }
            variant.updateFromRequest(existingVariantsMap.get(variant.getId()));
        }
        product.getImagePath().clear();
        for (String imagePath : productRequest.getImagePath()) {
            ImagePath image = new ImagePath();
            image.setPath(imagePath);
            image.setProduct(product);
            product.getImagePath().add(image);
        }
        product.setUpdatedOn(LocalDateTime.now());
        Product savedProduct = productRepository.saveAndFlush(product);
        entityManager.refresh(savedProduct);
        ProductResponse productResponse = savedProduct.transferToResponse();
        if (productResponse.getVariants() != null) {
            for (VariantResponse variantResponse : productResponse.getVariants()) {
                List<VariantStore> variantStores;
                if (storeId != null) {
                    variantStores = variantStoreRepository.findByVariantIdAndStoreId(variantResponse.getId(), storeId);
                } else {
                    variantStores = variantStoreRepository.findByVariantId(variantResponse.getId());
                }

                List<StoreQuantityDto> storeDtos = variantStores.stream()
                        .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
                        .toList();

                // Gán vào variantResponse
                variantResponse.setVariantStores(storeDtos);

            }
        }
        statisticSizeColorMaterial(productResponse);
        return productResponse;
    }

//    @Transactional
//    public ProductResponse updateProductV2(Long id, ProductRequest productRequest, Long storeId) {
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
//
//        Category category = categoryRepository.findById(productRequest.getCategoryId())
//                .orElseThrow(() -> new ResourceNotFoundException("Loại sản phẩm không tồn tại"));
//        Brand brand = brandRepository.findById(productRequest.getBrandId())
//                .orElseThrow(() -> new ResourceNotFoundException("Nhãn hiệu không tồn tại"));
//
//        Map<Long, Variant> variantMap = product.getVariants()
//                .stream()
//                .collect(Collectors.toMap(Variant::getId, Function.identity()));
//
//        Map<Long, VariantRequest> requestVariantMap = productRequest.getVariants()
//                .stream()
//                .collect(Collectors.toMap(VariantRequest::getId, Function.identity()));
//
//        Map<Long, VariantStore> variantStoreMap = variantStoreRepository
//                .findByProductIdAndStoreId(product.getId(), storeId)
//                .stream()
//                .collect(Collectors.toMap(vs -> vs.getVariant().getId(), Function.identity()));
//
//        Set<String> variantKeySet = new HashSet<>();
//        long totalQuantity = 0L;
//
//        for (VariantRequest vr : productRequest.getVariants()) {
//            Variant variant = variantMap.get(vr.getId());
//            if (variant == null) throw new ResourceNotFoundException("Loại sản phẩm không tồn tại");
//
//            // Validate trùng thuộc tính
//            String key = vr.getSize() + "-" + vr.getColor() + "-" + vr.getMaterial();
//            if (!variantKeySet.add(key)) throw new DataConflictException("Trùng thuộc tính: " + key);
//
//            // Validate SKU
//            String newSku = vr.getSku();
//            if (newSku != null && !newSku.equals(variant.getSku())) {
//                if (newSku.startsWith("PVN")) throw new DataConflictException("SKU không hợp lệ");
//                if (variantRepository.existsBySku(newSku)) throw new DataConflictException("SKU đã tồn tại");
//            }
//
//            variant.updateFromRequest(vr); // cập nhật các trường cơ bản
//
//            // Cập nhật variantStore
//            VariantStoreRequest vsr = vr.getVariantStores().stream()
//                    .filter(vs -> vs.getStoreId().equals(storeId)).findFirst().orElse(null);
//            if (vsr != null) {
//                VariantStore vs = variantStoreMap.getOrDefault(variant.getId(),
//                        VariantStore.builder().variant(variant).storeId(storeId).build());
//                vs.setQuantity(vsr.getQuantity());
//                variantStoreRepository.save(vs);
//            }
//
//            totalQuantity += vr.getQuantity();
//        }
//
//        product.setName(productRequest.getName());
//        product.setDescription(productRequest.getDescription());
//        product.setTotalQuantity(totalQuantity);
//        product.setBrand(brand);
//        product.setCategory(category);
//        product.setUpdatedOn(LocalDateTime.now());
//        product.setStock(productRequest.getStock());
//
////        product.getImagePath().clear();
////        for (String imagePath : productRequest.getImagePath()) {
////            product.getImagePath().add(new ImagePath(product, imagePath));
////        }
//        Set<String> newPaths = new HashSet<>(productRequest.getImagePath());
//        Set<String> oldPaths = product.getImagePath().stream()
//                .map(ImagePath::getPath).collect(Collectors.toSet());
//
//        if (!newPaths.equals(oldPaths)) {
//            product.getImagePath().clear();
//            for (String path : newPaths) {
//                product.getImagePath().add(new ImagePath(product, path));
//            }
//        }
//
//        Product savedProduct = productRepository.saveAndFlush(product);
//        entityManager.refresh(savedProduct);
//
//        ProductResponse productResponse = savedProduct.transferToResponse();
//
//        // Mapping lại variantStore để trả về
//        for (VariantResponse variantResponse : productResponse.getVariants()) {
//            List<VariantStore> stores = variantStoreRepository.findByVariantIdAndStoreId(variantResponse.getId(), storeId);
//            variantResponse.setVariantStores(
//                    stores.stream().map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity())).toList()
//            );
//        }
//
//        statisticSizeColorMaterial(productResponse);
//        return productResponse;
//    }

@Transactional
public ProductResponse updateProductV2(Long id, ProductRequest productRequest, Long storeId) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

    Category category = categoryRepository.findById(productRequest.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Loại sản phẩm không tồn tại"));

    Brand brand = brandRepository.findById(productRequest.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException("Nhãn hiệu không tồn tại"));

    // Map các variant hiện có trong DB
    Map<Long, Variant> variantMap = product.getVariants()
            .stream()
            .collect(Collectors.toMap(Variant::getId, Function.identity()));

    // Map dữ liệu variant từ request
    Map<Long, VariantRequest> requestVariantMap = productRequest.getVariants()
            .stream()
            .collect(Collectors.toMap(VariantRequest::getId, Function.identity()));

    // Lấy variantStore hiện có trong DB
    Map<Long, VariantStore> variantStoreMap = variantStoreRepository
            .findByProductIdAndStoreId(product.getId(), storeId)
            .stream()
            .collect(Collectors.toMap(vs -> vs.getVariant().getId(), Function.identity()));

    Set<String> variantKeySet = new HashSet<>();
    long totalQuantity = 0L;

    List<VariantStore> variantStoresToSave = new ArrayList<>();

    for (VariantRequest vr : productRequest.getVariants()) {
        Variant variant = variantMap.get(vr.getId());
        if (variant == null) throw new ResourceNotFoundException("Loại sản phẩm không tồn tại");

        // Validate trùng thuộc tính
        String key = vr.getSize() + "-" + vr.getColor() + "-" + vr.getMaterial();
        if (!variantKeySet.add(key)) {
            throw new DataConflictException("Trùng thuộc tính: " + key);
        }

        // Validate SKU
        String newSku = vr.getSku();
        if (newSku != null && !newSku.equals(variant.getSku())) {
            if (newSku.startsWith("PVN")) throw new DataConflictException("SKU không hợp lệ");
            if (variantRepository.existsBySku(newSku)) throw new DataConflictException("SKU đã tồn tại");
        }

        // Cập nhật thông tin variant
        variant.updateFromRequest(vr);

        // Cập nhật VariantStore
        VariantStoreRequest vsr = vr.getVariantStores().stream()
                .filter(vs -> vs.getStoreId().equals(storeId)).findFirst().orElse(null);

        if (vsr != null) {
            VariantStore vs = variantStoreMap.getOrDefault(variant.getId(),
                    VariantStore.builder().variant(variant).storeId(storeId).build());

            vs.setQuantity(vsr.getQuantity());
            variantStoresToSave.add(vs);
        }

        totalQuantity += vr.getQuantity();
    }

    // Cập nhật thông tin sản phẩm
    product.setName(productRequest.getName());
    product.setDescription(productRequest.getDescription());
    product.setTotalQuantity(totalQuantity);
    product.setBrand(brand);
    product.setCategory(category);
    product.setUpdatedOn(LocalDateTime.now());
    product.setStock(productRequest.getStock());

    // Chỉ cập nhật imagePath nếu có thay đổi
    Set<String> newPaths = new HashSet<>(productRequest.getImagePath());
    Set<String> oldPaths = product.getImagePath().stream()
            .map(ImagePath::getPath).collect(Collectors.toSet());

    if (!newPaths.equals(oldPaths)) {
        product.getImagePath().clear();
        for (String path : newPaths) {
            product.getImagePath().add(new ImagePath(product, path));
        }
    }

    // Lưu thông tin
    Product savedProduct = productRepository.saveAndFlush(product);
    variantStoreRepository.saveAll(variantStoresToSave);
    entityManager.refresh(savedProduct); // đảm bảo đầy đủ dữ liệu liên kết

    // Tạo response
    ProductResponse productResponse = savedProduct.transferToResponse();

    // Truy vấn toàn bộ variantStore một lần để tránh N+1
    List<Long> variantIds = productResponse.getVariants().stream()
            .map(VariantResponse::getId).toList();

    List<VariantStore> storeList = variantStoreRepository.findAllByVariantIdInAndStoreId(variantIds, storeId);
    Map<Long, VariantStore> storeMap = storeList.stream()
            .collect(Collectors.toMap(vs -> vs.getVariant().getId(), Function.identity()));

    for (VariantResponse variantResponse : productResponse.getVariants()) {
        VariantStore vs = storeMap.get(variantResponse.getId());
        if (vs != null) {
            variantResponse.setVariantStores(List.of(
                    new StoreQuantityDto(vs.getStoreId(), vs.getQuantity())
            ));
        }
    }

    statisticSizeColorMaterial(productResponse);
    return productResponse;
}

    public void statisticSizeColorMaterial(ProductResponse productResponse) {
//        productResponse.setSize(variantRepository.findDistinctSizesByProductId(productResponse.getId()));
//        productResponse.setColor(variantRepository.findDistinctColorsByProductId(productResponse.getId()));
//        productResponse.setMaterial(variantRepository.findDistinctMaterialsByProductId(productResponse.getId()));

        List<Object[]> results = variantRepository.findDistinctSizeColorMaterial(productResponse.getId());

        Set<String> sizes = new HashSet<>();
        Set<String> colors = new HashSet<>();
        Set<String> materials = new HashSet<>();

        for (Object[] row : results) {
            if (row[0] != null) sizes.add((String) row[0]);
            if (row[1] != null) colors.add((String) row[1]);
            if (row[2] != null) materials.add((String) row[2]);
        }

        productResponse.setSize(sizes);
        productResponse.setColor(colors);
        productResponse.setMaterial(materials);
    }

    @Transactional
    public Boolean deleteProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa"));
        productRepository.deleteProductById(id);
        variantRepository.deleteAllVariantOfProduct(id);
        return true;

    }

    @Transactional
    public Boolean deleteVariantById(Long productId, Long variantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa"));
        Variant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Phiên bản không tồn tại hoặc đã bị xóa"));
        if (product.getVariants().size() == 1) {
            deleteProductById(productId);
        } else {
            variantRepository.deleteVariantById(variantId);
        }
        return true;

    }

    @Transactional
    public Boolean deleteVariantByProperty(Long productId, String property, String value) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã bị xóa"));
        if (product.getVariants().size() == 1) {
            deleteProductById(productId);
        } else {
            if (property.equals("size"))
                variantRepository.deleteVariantBySize(productId, value);
            else if (property.equals("color"))
                variantRepository.deleteVariantByColor(productId, value);
            else if (property.equals("material"))
                variantRepository.deleteVariantByMaterial(productId, value);
        }
        return true;

    }
}
