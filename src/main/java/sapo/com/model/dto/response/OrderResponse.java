package sapo.com.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long storeId;
    private Long customerId;
    private String customerName;    // Nếu cần lấy từ Customer entity
    private Long creatorId;
    private String creatorName;     // Nếu cần lấy từ User entity
    private String code;
    private LocalDateTime createdOn;
    private int totalQuantity;
    private String note;
    private BigDecimal cashReceive;
    private BigDecimal cashRepay;
    private BigDecimal totalPayment;
    private String paymentType;
    private List<OrderDetailResponseV2> orderDetails;  // Danh sách chi tiết đơn hàng
}