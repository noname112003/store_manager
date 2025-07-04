package sapo.com.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@Service
public class PayOSService {

    private final RestTemplate restTemplate;
    private final PayOSConfig config;

    public PayOSService(PayOSConfig config) {
        this.restTemplate = new RestTemplate();
        this.config = config;
    }

    public String createOrder(String orderCode, int amount, String returnUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", config.getClientId());
        headers.set("x-api-key", config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", "Thanh toán đơn hàng #" + orderCode);
        body.put("returnUrl", returnUrl);
        body.put("cancelUrl", returnUrl);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                config.getEndpoint() + "/v1/payment-requests", request, Map.class);

        return (String) ((Map) response.getBody().get("data")).get("checkoutUrl");
    }
}
