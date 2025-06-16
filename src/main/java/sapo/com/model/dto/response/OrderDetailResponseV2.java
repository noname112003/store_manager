package sapo.com.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sapo.com.model.entity.Order;
import sapo.com.model.entity.Variant;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseV2 {
    private Long id;
    private Variant variant;
    private int quantity;
    private BigDecimal subTotal;
}