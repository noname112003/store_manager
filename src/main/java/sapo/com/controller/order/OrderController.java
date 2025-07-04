package sapo.com.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sapo.com.exception.OrderNotFoundException;
import sapo.com.model.dto.request.CreatePaymentLinkRequestBody;
import sapo.com.model.dto.request.order.CreateOrderRequest;
import sapo.com.model.dto.request.order.UpdateOrderRequest;
import sapo.com.model.dto.response.OrderResponse;
import sapo.com.model.dto.response.ResponseObject;
import sapo.com.model.dto.response.order.OrderByHourDto;
import sapo.com.model.dto.response.order.OrderDetailResponse;
import sapo.com.model.dto.response.order.OrderRevenueDto;
import sapo.com.model.entity.Order;
import sapo.com.service.OrderService;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/v1/orders")
@CrossOrigin("*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private final PayOS payOS;
    public OrderController(PayOS payOS) {
        super();
        this.payOS = payOS;
    }
    @PostMapping(path = "/create-payment-link")
    public ObjectNode createPaymentLink(@RequestBody CreatePaymentLinkRequestBody RequestBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode response = objectMapper.createObjectNode();
        try {
            final String productName = RequestBody.getProductName();
            final String description = RequestBody.getDescription();
            final String returnUrl = RequestBody.getReturnUrl();
            final String cancelUrl = RequestBody.getCancelUrl();
            final int price = RequestBody.getPrice();
            // Gen order code
            String currentTimeString = String.valueOf(String.valueOf(new Date().getTime()));
            long orderCode = Long.parseLong(currentTimeString.substring(currentTimeString.length() - 6));

            ItemData item = ItemData.builder().name(productName).price(price).quantity(1).build();

            PaymentData paymentData = PaymentData.builder().orderCode(orderCode).description(description).amount(price)
                    .item(item).returnUrl(returnUrl).cancelUrl(cancelUrl).build();

            CheckoutResponseData data = payOS.createPaymentLink(paymentData);

            response.put("error", 0);
            response.put("message", "success");
            response.set("data", objectMapper.valueToTree(data));
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", -1);
            response.put("message", "fail");
            response.set("data", null);
            return response;

        }
    }

    @GetMapping(path = "/get-payos/{orderId}")
    public ObjectNode getOrderById(@PathVariable("orderId") long orderId) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode response = objectMapper.createObjectNode();

        try {
            PaymentLinkData order = payOS.getPaymentLinkInformation(orderId);

            response.set("data", objectMapper.valueToTree(order));
            response.put("error", 0);
            response.put("message", "ok");
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", -1);
            response.put("message", e.getMessage());
            response.set("data", null);
            return response;
        }

    }

    @PutMapping(path = "/{orderId}")
    public ObjectNode cancelOrder(@PathVariable("orderId") int orderId) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode response = objectMapper.createObjectNode();
        try {
            PaymentLinkData order = payOS.cancelPaymentLink(orderId, null);
            response.set("data", objectMapper.valueToTree(order));
            response.put("error", 0);
            response.put("message", "ok");
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", -1);
            response.put("message", e.getMessage());
            response.set("data", null);
            return response;
        }
    }

    @GetMapping("/")
    public ResponseEntity<ResponseObject> getAllOrder(@RequestParam(value = "page", defaultValue = "0") int page,
                                                      @RequestParam(value = "limit", defaultValue = "10") int limit,
                                                      @RequestParam(value = "query", defaultValue = "") String query,
                                                      @RequestParam(value = "startDate") String startDate,
                                                      @RequestParam(value = "endDate") String endDate,
                                                      @RequestParam(value = "storeId", required = false) Long storeId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try {
            return ResponseEntity.ok(ResponseObject.builder().data(orderService.getAllOrder(page, limit, query, LocalDate.parse(startDate, formatter), LocalDate.parse(endDate, formatter).plusDays(1), storeId)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder().message(e.getMessage()).status(null).build());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<ResponseObject> getNumberOfOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                                                      @RequestParam(value = "limit", defaultValue = "10") int limit,
                                                      @RequestParam(value = "query", defaultValue = "") String query,
                                                      @RequestParam(value = "startDate") String startDate,
                                                      @RequestParam(value = "endDate") String endDate,
                                                      @RequestParam(value = "storeId", required = false) Long storeId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try {
            return ResponseEntity.ok(ResponseObject.builder().data(orderService.getNumberOfOrders(query, LocalDate.parse(startDate, formatter), LocalDate.parse(endDate, formatter).plusDays(1), storeId)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder().message(e.getMessage()).status(null).build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseObject> getOrderDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ResponseObject.builder().data(orderService.getOrderDetail(id)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder().message(e.getMessage()).status(null).build());
        }
    }
    @GetMapping("/v2/{orderId}")
    public ResponseEntity<ResponseObject> getOrder(@PathVariable Long orderId) {
        try{
            OrderResponse orderResponse = orderService.getOrderById(orderId);
            return ResponseEntity.ok(ResponseObject.builder().data(orderResponse).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder().message(e.getMessage()).status(null).build());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        try {
            return ResponseEntity.ok(ResponseObject.builder().data(orderService.createOrder(createOrderRequest)).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseObject.builder().message(e.getMessage()).status(null).build());
        }
    }

    @GetMapping("/today")
    public ResponseEntity<OrderRevenueDto> getTodayOrders(@RequestParam(value = "storeId") Long storeId,@RequestParam(value = "pageNum", required = false, defaultValue = "0") int pageNum,
                                                          @RequestParam(value = "pageSize", defaultValue = "5") int pageSize) throws OrderNotFoundException {
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdOn"));
        OrderRevenueDto orderRevenue = orderService.getTodayOrdersAndRevenue(storeId, pageable);
        return new ResponseEntity<>(orderRevenue, HttpStatus.OK);
    }

    @PutMapping("/update/{orderId}")
    public ResponseEntity<ResponseObject> updateOrder(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderRequest updateOrderRequest) {
        try {
            OrderDetailResponse updatedOrder = orderService.updateOrder(orderId, updateOrderRequest);
            return ResponseEntity.ok(
                    ResponseObject.builder().data(updatedOrder).build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ResponseObject.builder()
                            .message(e.getMessage())
                            .status(null)
                            .build());
        }
    }

    // API trả về số lượng đơn hàng theo từng giờ trong ngày
    @GetMapping("/count-by-day")
    public ResponseEntity<List<OrderByHourDto>> getOrderCountByDay(
            @RequestParam("storeId") Long storeId,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        List<OrderByHourDto> result = orderService.getOrderCountByDay(storeId, date);
        return ResponseEntity.ok(result);
    }
}
