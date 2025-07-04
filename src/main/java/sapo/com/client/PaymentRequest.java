package sapo.com.client;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PaymentRequest {
    private String orderCode;
    private int amount;
}