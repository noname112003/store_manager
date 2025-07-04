package sapo.com.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sapo.com.exception.OrderNotFoundException;
import sapo.com.model.dto.request.order.CreateOrderRequest;
import sapo.com.model.dto.request.order.CreateOrderDetailRequest;
import sapo.com.model.dto.request.order.UpdateOrderRequest;
import sapo.com.model.dto.response.OrderDetailResponseV2;
import sapo.com.model.dto.response.OrderResponse;
import sapo.com.model.dto.response.order.*;
import sapo.com.model.entity.*;
import sapo.com.repository.*;
import sapo.com.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VariantRepository variantRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VariantStoreRepository variantStoreRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    @Transactional
    public OrderDetailResponse createOrder(CreateOrderRequest createOrderRequest) {
        // Kiểm tra thông tin đầu vào
        Customer customer = customerRepository.findById(createOrderRequest.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
        User user = userRepository.findById(createOrderRequest.getCreatorId())
                .orElseThrow(() -> new RuntimeException("Người tạo đơn hàng không tồn tại"));
        if (createOrderRequest.getOrderLineItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng không có sản phẩm");
        }
        if (createOrderRequest.getCashReceive().compareTo(createOrderRequest.getTotalPayment()) < 0) {
            throw new RuntimeException("Số tiền nhận không hợp lệ");
        }

        // Tạo đơn hàng
        Order order = new Order();
        order.setStoreId(createOrderRequest.getStoreId());
        order.setCustomer(customer);
        order.setCreator(user);
        order.setTotalQuantity(createOrderRequest.getTotalQuantity());
        order.setTotalPayment(createOrderRequest.getTotalPayment());
        order.setCashReceive(createOrderRequest.getCashReceive());
        order.setCashRepay(createOrderRequest.getCashRepay());
        order.setPaymentType(createOrderRequest.getPaymentType());
        order.setNote(createOrderRequest.getNote());
        // **Cập nhật status đơn hàng**
        order.setStatus("create");

        // **Cập nhật thời gian tạo và update**
        order.setUpdateTime(LocalDateTime.now());  // Cập nhật thời gian hiện tại khi tạo đơn
        Order newOrder = orderRepository.save(order);

        // Tạo chi tiết đơn hàng
        final Set<CreateOrderDetailRequest> orderDetails = createOrderRequest.getOrderLineItems();
        orderDetails.forEach(createOrderDetailRequest -> {
            Variant variant = variantRepository.findById(createOrderDetailRequest.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            if(createOrderDetailRequest.getQuantity() < 0) {
                throw new RuntimeException("Số lượng sản phẩm không hợp lệ");
            }
            if(createOrderDetailRequest.getQuantity() > variant.getQuantity()) {
                throw new RuntimeException("Số lượng sản phẩm không đủ");
            }

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(newOrder);
            orderDetail.setVariant(variant);
            orderDetail.setQuantity(createOrderDetailRequest.getQuantity());
            orderDetail.setSubTotal(createOrderDetailRequest.getSubTotal());
            orderDetailRepository.save(orderDetail);

            // Cập nhật số lượng sản phẩm
            variant.setQuantity(variant.getQuantity() - createOrderDetailRequest.getQuantity());
            variantRepository.save(variant);

            // Cập nhật tổng số sản phẩm trong product
            Product product = productRepository.findByVariantId(variant.getId());
            product.setTotalQuantity(product.getTotalQuantity() - createOrderDetailRequest.getQuantity());

            // Cập nhật số lượng trong VariantStore
            List<VariantStore> variantStores = variantStoreRepository.findByVariantIdAndStoreId(
                    variant.getId(), createOrderRequest.getStoreId()
            );

            if (variantStores.isEmpty()) {
                throw new RuntimeException("Không tìm thấy VariantStore tương ứng với variantId = "
                        + variant.getId() + " và storeId = " + createOrderRequest.getStoreId());
            }

            VariantStore variantStore = variantStores.get(0); // Giả sử mỗi cặp variantId - storeId là duy nhất

            Long newQuantity = variantStore.getQuantity() - createOrderDetailRequest.getQuantity();
            if (newQuantity < 0) {
                throw new RuntimeException("Số lượng sản phẩm trong kho không đủ");
            }
            variantStore.setQuantity(newQuantity);
            variantStoreRepository.save(variantStore);
        });

        // Cập nhật thông tin khách hàng
        customer.setNumberOfOrder(customer.getNumberOfOrder() + 1);
        if(customer.getTotalExpense() == null) {
            customer.setTotalExpense(newOrder.getTotalPayment());
        } else customer.setTotalExpense(customer.getTotalExpense().add(newOrder.getTotalPayment()));

        // Thêm mã đơn hàng
        newOrder.setCode("SON" + String.format("%05d", newOrder.getId()));
        orderRepository.save(newOrder);

        return getOrderDetail(newOrder.getId());
    }

//    @Override
//    @Transactional
//    public OrderDetailResponse createOrder(CreateOrderRequest request) {
//        // Kiểm tra thông tin đầu vào
//        Customer customer = customerRepository.findById(request.getCustomerId())
//                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
//        User user = userRepository.findById(request.getCreatorId())
//                .orElseThrow(() -> new RuntimeException("Người tạo đơn hàng không tồn tại"));
//        if (request.getOrderLineItems().isEmpty()) {
//            throw new RuntimeException("Đơn hàng không có sản phẩm");
//        }
//        if (request.getCashReceive().compareTo(request.getTotalPayment()) < 0) {
//            throw new RuntimeException("Số tiền nhận không hợp lệ");
//        }
//
//        // Tạo đơn hàng
//        Order order = new Order();
//        order.setStoreId(request.getStoreId());
//        order.setCustomer(customer);
//        order.setCreator(user);
//        order.setTotalQuantity(request.getTotalQuantity());
//        order.setTotalPayment(request.getTotalPayment());
//        order.setCashReceive(request.getCashReceive());
//        order.setCashRepay(request.getCashRepay());
//        order.setPaymentType(request.getPaymentType());
//        order.setNote(request.getNote());
//        order.setStatus("create");
//        order.setUpdateTime(LocalDateTime.now());
//
//        order = orderRepository.save(order); // save để sinh ID
//
//        // Gom tất cả variantId
//        Set<Long> variantIds = request.getOrderLineItems().stream()
//                .map(CreateOrderDetailRequest::getVariantId)
//                .collect(Collectors.toSet());
//
//        // Lấy toàn bộ variant, product và variantStore 1 lần
//        Map<Long, Variant> variantMap = variantRepository.findAllById(variantIds)
//                .stream().collect(Collectors.toMap(Variant::getId, v -> v));
//
//        Map<Long, VariantStore> variantStoreMap = variantStoreRepository
//                .findAllByVariant_IdInAndStoreId(variantIds, request.getStoreId())
//                .stream().collect(Collectors.toMap(vs -> vs.getVariant().getId(), vs -> vs));
//
//        // Lấy product từ variant (tránh query trong vòng lặp)
//        Set<Long> productIds = variantMap.values().stream()
//                .map(v -> v.getProduct().getId())
//                .collect(Collectors.toSet());
//
//        Map<Long, Product> productMap = productRepository.findAllById(productIds)
//                .stream().collect(Collectors.toMap(Product::getId, p -> p));
//
//        // Tạo danh sách để batch xử lý
//        List<OrderDetail> orderDetails = new ArrayList<>();
//        List<Variant> variantsToUpdate = new ArrayList<>();
//        List<Product> productsToUpdate = new ArrayList<>();
//        List<VariantStore> storesToUpdate = new ArrayList<>();
//
//        for (CreateOrderDetailRequest item : request.getOrderLineItems()) {
//            Variant variant = variantMap.get(item.getVariantId());
//            if (item.getQuantity() <= 0 || item.getQuantity() > variant.getQuantity()) {
//                throw new RuntimeException("Số lượng sản phẩm không hợp lệ hoặc không đủ: " + variant.getId());
//            }
//
//            // Tạo chi tiết đơn hàng
//            OrderDetail detail = new OrderDetail();
//            detail.setOrder(order);
//            detail.setVariant(variant);
//            detail.setQuantity(item.getQuantity());
//            detail.setSubTotal(item.getSubTotal());
//            orderDetails.add(detail);
//
//            // Trừ tồn kho variant
//            variant.setQuantity(variant.getQuantity() - item.getQuantity());
//            variantsToUpdate.add(variant);
//
//            // Trừ tổng tồn kho product
//            Product product = productMap.get(variant.getProduct().getId());
//            product.setTotalQuantity(product.getTotalQuantity() - item.getQuantity());
//            productsToUpdate.add(product);
//
//            // Trừ tồn kho ở VariantStore
//            VariantStore store = variantStoreMap.get(variant.getId());
//            if (store == null || store.getQuantity() < item.getQuantity()) {
//                throw new RuntimeException("Sản phẩm trong kho không đủ: variantId = " + variant.getId());
//            }
//            store.setQuantity(store.getQuantity() - item.getQuantity());
//            storesToUpdate.add(store);
//        }
//
//        // Batch save
//        orderDetailRepository.saveAll(orderDetails);
//        variantRepository.saveAll(variantsToUpdate);
//        productRepository.saveAll(productsToUpdate);
//        variantStoreRepository.saveAll(storesToUpdate);
//
//        entityManager.flush();
//
//        // Cập nhật khách hàng
//        customer.setNumberOfOrder(customer.getNumberOfOrder() + 1);
//        if (customer.getTotalExpense() == null) {
//            customer.setTotalExpense(order.getTotalPayment());
//        } else {
//            customer.setTotalExpense(customer.getTotalExpense().add(order.getTotalPayment()));
//        }
//
//        // Gán mã đơn hàng (sau khi có ID)
//        order.setCode("SON" + String.format("%05d", order.getId()));
//////        orderRepository.save(order);
////
////        return getOrderDetail(order.getId());
//////        return new OrderDetailResponse();
//    }


    @Override
    public List<AllOrderResponse> getAllOrder(int page, int limit, String query, LocalDate startDate, LocalDate endDate, Long storeId) {
        List<Order> orders = orderRepository.findOrdersByDateAndCode(startDate, endDate, query, storeId);
        // Chuyển đổi danh sách đơn hàng sang danh sách response
        List<AllOrderResponse> allOrderResponseList = orders.stream().map(AllOrderResponse::new).toList();
        // Phân trang
        return allOrderResponseList.subList(Math.max(page * limit, 0), Math.min((page + 1) * limit, allOrderResponseList.size()));
    }

    @Override
    public int getNumberOfOrders(String query, LocalDate startDate, LocalDate endDate, Long storeId) {
        return orderRepository.findOrdersByDateAndCode(startDate, endDate, query, storeId).size();
    }
    @Override
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Lấy riêng danh sách chi tiết đơn hàng
        List<OrderDetail> orderDetailsEntity = orderDetailRepository.findAllByOrderId(orderId);

        // Map orderDetails sang DTO, trực tiếp dùng entity Variant
        List<OrderDetailResponseV2> orderDetails = orderDetailsEntity.stream()
                .map(detail -> OrderDetailResponseV2.builder()
                        .id(detail.getId())
                        .variant(detail.getVariant())   // trực tiếp dùng entity Variant
                        .quantity(detail.getQuantity())
                        .subTotal(detail.getSubTotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .storeId(order.getStoreId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())  // giả sử có getName()
                .creatorId(order.getCreator().getId())
                .creatorName(order.getCreator().getName())    // giả sử có getName()
                .code(order.getCode())
                .createdOn(order.getCreatedOn())
                .totalQuantity(order.getTotalQuantity())
                .note(order.getNote())
                .cashReceive(order.getCashReceive())
                .cashRepay(order.getCashRepay())
                .totalPayment(order.getTotalPayment())
                .paymentType(order.getPaymentType())
                .orderDetails(orderDetails)
                .build();
    }

    @Override
    public OrderDetailResponse getOrderDetail(Long orderId) {
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        Order order = orderRepository.findWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        OrderDetailResponse orderDetailResponse = new OrderDetailResponse(order);
        // Lấy chi tiết đơn hàng
        orderDetailResponse.setOrderDetails(orderDetailRepository.findAllByOrderId(orderId));
        return orderDetailResponse;
    }

    @Override
    public OrderDetailResponse getOrderDetailV2(Long orderId) {
        Order order = orderRepository.findWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        OrderDetailResponse response = new OrderDetailResponse(order);

        Set<OrderDetail> orderDetails = order.getOrderDetails();
        response.setOrderDetails(orderDetails == null ? new ArrayList<>() : new ArrayList<>(orderDetails));


        return response;
    }


//    @Override
//    public OrderDetailResponse getOrderDetail(Long orderId) {
//        Order order = orderRepository.findWithDetails(orderId)
//                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
//
//        OrderDetailResponse response = new OrderDetailResponse(order);
//
//        List<OrderDetailDto> detailDtos = order.getOrderDetails().stream().map(od -> {
//            return new OrderDetailDto(
//                    od.getVariant().getId(),
//                    od.getVariant().getName(),
//                    od.getQuantity(),
//                    od.getSubTotal(),
//                    od.getVariant().getSku(),
//                    od.getVariant().getImagePath()
//            );
//        }).toList();
//
//        response.setOrderDetails(detailDtos);
//        return response;
//    }


    @Override
    public OrderRevenueDto getTodayOrdersAndRevenue(Long storeId, Pageable pageable) throws OrderNotFoundException {
        LocalDate today = LocalDate.now();

        // Lấy danh sách đơn hàng
        // Lấy đơn hàng theo ngày và store
        Page<Order> ordersToday = orderRepository.findOrdersToday(today, storeId, pageable);

        if (ordersToday.isEmpty()) {
            throw new OrderNotFoundException("Không tìm thấy đơn hàng nào cho ngày hôm nay.");
        }

        // Tính tổng doanh thu
        BigDecimal totalRevenue = ordersToday.getContent().stream()
                .map(Order::getTotalPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderRevenueDto(ordersToday, totalRevenue);
    }

    @Override
    @Transactional
    public OrderDetailResponse updateOrder(Long orderId, UpdateOrderRequest request) {
        // Lấy đơn hàng cũ
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Kiểm tra các trường hợp validate như tạo order
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại"));
        User user = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new RuntimeException("Người tạo đơn hàng không tồn tại"));

        if (request.getOrderLineItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng không có sản phẩm");
        }
        if (request.getCashReceive().compareTo(request.getTotalPayment()) < 0) {
            throw new RuntimeException("Số tiền nhận không hợp lệ");
        }

        // Cập nhật thông tin đơn hàng
        order.setCustomer(customer);
        order.setCreator(user);
        order.setStoreId(request.getStoreId());
        order.setTotalQuantity(request.getTotalQuantity());
        order.setTotalPayment(request.getTotalPayment());
        order.setCashReceive(request.getCashReceive());
        order.setCashRepay(request.getCashRepay());
        order.setPaymentType(request.getPaymentType());
        order.setNote(request.getNote());
        // **Cập nhật status đơn hàng**
        order.setStatus("update");

        orderRepository.save(order);

        // Rollback tồn kho cho chi tiết đơn hàng cũ trước khi xóa
        List<OrderDetail> oldDetails = orderDetailRepository.findAllByOrderId(orderId);
        oldDetails.forEach(od -> {
            Variant variant = od.getVariant();

            // Trả lại số lượng tồn kho và sản phẩm
            variant.setQuantity(variant.getQuantity() + od.getQuantity());
            variantRepository.save(variant);

            Product product = productRepository.findByVariantId(variant.getId());
            product.setTotalQuantity(product.getTotalQuantity() + od.getQuantity());
            productRepository.save(product);

            VariantStore variantStore = variantStoreRepository.findByVariantIdAndStoreId(
                    variant.getId(), order.getStoreId()
            ).stream().findFirst().orElse(null);

            if (variantStore != null) {
                variantStore.setQuantity(variantStore.getQuantity() + od.getQuantity());
                variantStoreRepository.save(variantStore);
            }
        });

        // Xóa chi tiết cũ
        orderDetailRepository.deleteAll(oldDetails);

        // Thêm chi tiết mới và cập nhật tồn kho
        request.getOrderLineItems().forEach(detailReq -> {
            Variant variant = variantRepository.findById(detailReq.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
            if (detailReq.getQuantity() < 0) {
                throw new RuntimeException("Số lượng sản phẩm không hợp lệ");
            }
            if (detailReq.getQuantity() > variant.getQuantity()) {
                throw new RuntimeException("Số lượng sản phẩm không đủ");
            }

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setVariant(variant);
            orderDetail.setQuantity(detailReq.getQuantity());
            orderDetail.setSubTotal(detailReq.getSubTotal());
            orderDetailRepository.save(orderDetail);

            // Trừ tồn kho
            variant.setQuantity(variant.getQuantity() - detailReq.getQuantity());
            variantRepository.save(variant);

            Product product = productRepository.findByVariantId(variant.getId());
            product.setTotalQuantity(product.getTotalQuantity() - detailReq.getQuantity());
            productRepository.save(product);

            VariantStore variantStore = variantStoreRepository.findByVariantIdAndStoreId(
                    variant.getId(), order.getStoreId()
            ).stream().findFirst().orElseThrow(() ->
                    new RuntimeException("Không tìm thấy VariantStore tương ứng"));

            Long newQuantity = variantStore.getQuantity() - detailReq.getQuantity();
            if (newQuantity < 0) {
                throw new RuntimeException("Số lượng sản phẩm trong kho không đủ");
            }
            variantStore.setQuantity(newQuantity);
            variantStoreRepository.save(variantStore);
        });


        BigDecimal totalExpense = orderRepository.sumTotalPaymentByCustomerId(customer.getId());
        customer.setTotalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO);

        customerRepository.save(customer);

        return getOrderDetail(orderId);
    }

    @Override
    public List<OrderByHourDto> getOrderCountByDay(Long storeId, LocalDate date) {
        // Lấy tất cả đơn hàng trong ngày
        List<Order> orders = orderRepository.findOrdersByDate(date, storeId);

        // Khởi tạo mảng 24 giờ
        List<OrderByHourDto> ordersByHour = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            ordersByHour.add(new OrderByHourDto(hour, 0L)); // Đặt số lượng mặc định là 0
        }

        // Tính số đơn hàng cho từng giờ
        for (Order order : orders) {
            int hour = order.getCreatedOn().getHour(); // Lấy giờ từ thời gian tạo đơn
            ordersByHour.get(hour).setOrderCount(ordersByHour.get(hour).getOrderCount() + 1); // Tăng số lượng đơn hàng cho giờ đó
        }

        return ordersByHour;
    }

}
