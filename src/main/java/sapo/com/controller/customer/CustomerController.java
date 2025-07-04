package sapo.com.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sapo.com.exception.CustomerNotFoundException;
import sapo.com.model.dto.response.ResponseObject;
import sapo.com.model.dto.response.customer.CustomerDetailResponse;
import sapo.com.model.dto.response.customer.CustomerResponse;
import sapo.com.model.dto.response.order.AllOrderResponse;
import sapo.com.model.entity.Customer;
import sapo.com.model.entity.Order;
import sapo.com.repository.CustomerRepository;
import sapo.com.repository.OrderRepository;
import sapo.com.service.CustomerService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/v1/customers")
public class CustomerController {

    @Autowired private CustomerService customerService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private OrderRepository orderRepository;

    @GetMapping("")
    public ResponseEntity<Page<CustomerResponse>> findByKeyword(
            @RequestParam(value="pageNum", required = false, defaultValue = "0") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "5") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) throws CustomerNotFoundException {

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdOn"));
        Page<CustomerResponse> customers = customerService.findByKeyword(keyword, pageable);
        return new ResponseEntity<>(customers, HttpStatus.OK); // Trả về trạng thái 200 OK nếu thành công
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<?> findById(@PathVariable Long customerId) throws CustomerNotFoundException {
//        Customer existingCustomer = customerRepository.findById(customerId);
//        Customer existingCustomer = customerRepository.findById(customerId)
//                .orElseThrow(() -> new CustomerNotFoundException("Không tìm thấy khách hàng"));

        Customer existingCustomer = customerRepository.findWithOrders(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Không tìm thấy khách hàng ứng với ID: " + customerId));
        if (existingCustomer == null) {
            return new ResponseEntity<>("Không tìm thấy khách hàng ứng với ID: " + customerId, HttpStatus.NOT_FOUND);
        }
        CustomerDetailResponse customerDetailResponse = new CustomerDetailResponse(existingCustomer);
        return new ResponseEntity<>(customerDetailResponse, HttpStatus.OK);
    }
    @GetMapping("/{customerId}/orders")
    public ResponseEntity<?> getCustomerOrders(@PathVariable Long customerId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "5") int size) {
        Page<Order> ordersPage = orderRepository.findByCustomerId(customerId, PageRequest.of(page, size, Sort.by("createdOn").descending()));
        Page<AllOrderResponse> dtoPage = ordersPage.map(AllOrderResponse::new);
        return ResponseEntity.ok(dtoPage);
    }


    @PostMapping("/create")
    public ResponseEntity<ResponseObject> createCustomer(@RequestBody Customer customer) {

        // Check if the phone number already exists
        Customer existingCustomer = customerService.findByPhoneNumber(customer.getPhoneNumber());
        if (existingCustomer != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ResponseObject.builder()
                            .message("Số điện thoại đã tồn tại.")
                            .status(HttpStatus.CONFLICT)
                            .data(null)
                            .build()
            );
        }

        // Save the new customer
        Customer savedCustomer = customerService.register(customer); // Trả về customer sau khi lưu và set mã code
        CustomerResponse response = new CustomerResponse(savedCustomer);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseObject.builder()
                        .message("Tạo khách hàng mới thành công.")
                        .status(HttpStatus.CREATED)
                        .data(response)
                        .build()
        );

    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable("id") Long id,
                                            @RequestBody Customer customerInForm) throws CustomerNotFoundException {
        Customer customerInDb = customerService.findById(id);  // 1 lần query duy nhất

        // Nếu số điện thoại thay đổi thì mới kiểm tra trùng
        if (!customerInDb.getPhoneNumber().equals(customerInForm.getPhoneNumber())) {
            Customer existingCustomer = customerService.findByPhoneNumber(customerInForm.getPhoneNumber());
            if (existingCustomer != null && !existingCustomer.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ResponseObject.builder()
                                .message("Số điện thoại đã tồn tại.")
                                .status(HttpStatus.CONFLICT)
                                .data(null)
                                .build()
                );
            }
        }

        customerInForm.setId(id);
        Customer updatedCustomer = customerService.update(customerInDb, customerInForm); // Cập nhật xong có thể trả luôn
        CustomerResponse customerResponse = new CustomerResponse(updatedCustomer);

        return ResponseEntity.ok(
                ResponseObject.builder()
                        .message("Customer ID:" + id + " updated successfully")
                        .status(HttpStatus.OK)
                        .data(customerResponse)
                        .build()
        );
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable("id") Long customerId) throws CustomerNotFoundException {

        customerService.delete(customerId);
        return new ResponseEntity<>("Customer with ID " + customerId + " has been successfully deleted.",HttpStatus.OK);


    }


}
