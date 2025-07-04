package sapo.com.model.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import sapo.com.model.entity.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProductRequest {
    @Valid
    @NotBlank(message = "Tên sản phẩm không được trống.")
    private String name;
    private Long categoryId;
    private Long brandId;
    private String description;
    private Set<String> imagePath;
    private Long totalQuantity;
    private Long stock;
    private Boolean status;
    private LocalDateTime createdOn ;
    private LocalDateTime updatedOn ;
    @Valid
    @NotEmpty(message = "Phiên bản không được trống.")
    private List<VariantRequest> variants;

    //Do not have brand, category, variant
    public Product transferToProduct(){
        Product product= new Product();
        product.setName(this.name);
        product.setDescription(this.description);
        product.setCreatedOn(LocalDateTime.now());
        product.setUpdatedOn(LocalDateTime.now());
        Set<ImagePath> imagePaths = this.imagePath.stream()
                .map(path -> {
                    ImagePath imagePath = new ImagePath();
                    imagePath.setPath(path);
                    imagePath.setProduct(product);  // Set the product reference in imagePath
                    return imagePath;
                }).collect(Collectors.toSet());
        product.setImagePath(imagePaths);
        List<Variant> variants = this.variants.stream()
                .map(variant -> {
                    Variant variantItem = variant.transferToVariant();
                    variantItem.setProduct(product);
                    return variantItem;
                }).collect(Collectors.toList());
        product.setVariants(variants);
//        long totalQuantity = variants.stream()
//                .mapToLong(Variant::getQuantity) // quantity đã là tổng từ variantStores
//                .sum();
        long totalQuantity = this.totalQuantity;
        long stock = this.stock;
        product.setTotalQuantity(totalQuantity);
        product.setStock(stock);
        product.setStatus(this.status);
        return product;
    }
}
