package sapo.com.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class VariantWithStoresDto {
    private Long id ;
    private String name;
    private Long productId;
    private String productName;
    private String sku;
    private String size;
    private String color;
    private String material;
    private Long quantity;
    private BigDecimal initialPrice;
    private BigDecimal priceForSale;
    private Boolean status ;
    private String imagePath ;
    private LocalDateTime createdOn ;
    private LocalDateTime updateOn ;
    private List<StoreQuantityDto> variantStores;
}
