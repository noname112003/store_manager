package sapo.com.model.dto.response.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderDetailDto {
    private Long variantId;
    private String variantName;
    private int quantity;
    private BigDecimal subTotal;
    private String sku;
    private String imagePath;

    public OrderDetailDto(Long variantId, String variantName, int quantity, BigDecimal subTotal, String sku, String imagePath) {
        this.variantId = variantId;
        this.variantName = variantName;
        this.quantity = quantity;
        this.subTotal = subTotal;
        this.sku = sku;
        this.imagePath = imagePath;
    }
}