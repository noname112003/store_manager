package sapo.com.model.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import sapo.com.model.dto.request.VariantRequest;
import sapo.com.model.dto.request.VariantStoreRequest;
import sapo.com.model.dto.response.ProductResponse;
import sapo.com.model.dto.response.StoreQuantityDto;
import sapo.com.model.dto.response.VariantResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "variants")
public class Variant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonBackReference(value = "product-variant")
    private Product product;
    //    @ManyToOne
//    @JoinColumn(name = "creator_id")
//    private Long creatorId;
    private String name;
    @Column(nullable = false)

    private String sku;
    private String size;
    private String color;
    private String material;
    private Long quantity;
    private Long stock; // tồn kho tổng
    @Column(name = "initial_price")
    private BigDecimal initialPrice;
    @Column(name = "price_for_sale")
    private BigDecimal priceForSale;
    private Boolean status;
    @Column(name = "image_path")
    private String imagePath;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "created_on")
    private LocalDateTime createdOn;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @OneToMany(mappedBy = "variant", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonManagedReference(value = "variant-variantStore")
    @BatchSize(size = 10) // Hibernate sẽ join theo batch thay vì từng dòng
    private Set<VariantStore> variantStores;

    public VariantResponse transferToResponse() {
        VariantResponse variantResponse = new VariantResponse();
        variantResponse.setName(this.name);
        variantResponse.setId(this.id);
        variantResponse.setProductId(this.product.getId());
        variantResponse.setProductName(this.product.getName());
        variantResponse.setSku(this.sku);
        variantResponse.setSize(this.size);
        variantResponse.setColor(this.color);
        variantResponse.setMaterial(this.material);
        variantResponse.setQuantity(this.quantity);
        variantResponse.setStock(this.stock);
        variantResponse.setInitialPrice(this.initialPrice);
        variantResponse.setPriceForSale(this.priceForSale);
        variantResponse.setStatus(this.status);
        variantResponse.setImagePath(this.imagePath);
        variantResponse.setCreatedOn(this.createdOn);
        variantResponse.setUpdateOn(this.updatedOn);
        if (this.variantStores != null && !this.variantStores.isEmpty()) {
            List<StoreQuantityDto> storeDtos = this.variantStores.stream()
                    .map(vs -> new StoreQuantityDto(vs.getStoreId(), vs.getQuantity()))
                    .collect(Collectors.toList());
            variantResponse.setVariantStores(storeDtos);
        }
        return variantResponse;
    }

    public void updateFromRequest(VariantRequest variantRequest) {
        if (variantRequest.getId() != null) {
            this.id = variantRequest.getId();
        }
        this.name = variantRequest.getName();
        if (variantRequest.getSku().length() != 0)
            this.sku = variantRequest.getSku();
        this.size = variantRequest.getSize();
        this.color = variantRequest.getColor();
        this.material = variantRequest.getMaterial();
        this.initialPrice = variantRequest.getInitialPrice();
        this.priceForSale = variantRequest.getPriceForSale();
        this.quantity = variantRequest.getQuantity();
        this.stock = variantRequest.getStock();
        this.imagePath = variantRequest.getImagePath();
    }

}