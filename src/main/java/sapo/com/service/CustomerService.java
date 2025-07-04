package sapo.com.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sapo.com.exception.CustomerNotFoundException;
import sapo.com.model.dto.response.customer.CustomerResponse;
import sapo.com.model.entity.Customer;
import sapo.com.model.entity.Order;
import sapo.com.repository.CustomerRepository;
import sapo.com.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerService {
    @Autowired private CustomerRepository customerRepository;
    @Autowired private OrderRepository orderRepository;

    public Page<CustomerResponse> findByKeyword(String keyword, Pageable pageable) throws CustomerNotFoundException {
        Page<Customer> customersByKeyword;

        if (keyword != null) {
            customersByKeyword = customerRepository.findByKeyword(keyword, pageable);
        } else {
            // Chuyển sang trả về Page với sắp xếp từ Pageable
            customersByKeyword = customerRepository.findAll(pageable);
        }

        // Kiểm tra nếu không tìm thấy khách hàng nào
        if (customersByKeyword.isEmpty()) {
            throw new CustomerNotFoundException("Không tìm thấy khách hàng nào.");
        }

        // Chuyển đổi từ Page<Customer> sang Page<CustomerResponse>
        List<CustomerResponse> customerResponses = customersByKeyword.getContent().stream()
                .map(CustomerResponse::new) // Chuyển đổi mỗi Customer thành CustomerResponse
                .collect(Collectors.toList());

        return new PageImpl<>(customerResponses, pageable, customersByKeyword.getTotalElements());
    }



    public Customer findByPhoneNumber(String phoneNumber) {

        Customer existingCustomer = customerRepository.findByPhoneNumber(phoneNumber);
        return existingCustomer;


    }

    public Customer findById(Long id) throws CustomerNotFoundException{
        Optional<Customer> customer = customerRepository.findById(id);

        if (customer.isPresent()) {
            Customer customerById = customer.get();
            return customerById;
        } else {
            throw new CustomerNotFoundException("Customer not found with ID: " + id);
        }
    }

    public Customer register(Customer customer) {
        Customer savedCustomer = customerRepository.save(customer);

        // Tạo mã code
        String customerCode = String.format("CUS%05d", savedCustomer.getId());
        savedCustomer.setCode(customerCode);

        // Lưu lại và return
        return customerRepository.save(savedCustomer);
    }

//    public Customer update(Customer customerInForm) throws CustomerNotFoundException {
//        Optional<Customer> customerInDB = customerRepository.findById(customerInForm.getId());
//        if(customerInDB.isPresent()){
//            Customer customer = customerInDB.get();
//            customer.setName(customerInForm.getName());
//            customer.setAddress(customerInForm.getAddress());
//            customer.setPhoneNumber(customerInForm.getPhoneNumber());
//            customer.setGender(customerInForm.isGender());
//            customer.setEmail(customerInForm.getEmail());
//            customer.setBirthday(customerInForm.getBirthday());
//            customer.setNote(customerInForm.getNote());
//            return customerRepository.save(customer);
//        }else{
//            throw new CustomerNotFoundException("Customer not found with ID: " + customerInForm.getId());
//        }
//    }

    public Customer update(Customer customerInDb, Customer customerInForm) {
        customerInDb.setName(customerInForm.getName());
        customerInDb.setAddress(customerInForm.getAddress());
        customerInDb.setPhoneNumber(customerInForm.getPhoneNumber());
        customerInDb.setGender(customerInForm.isGender());
        customerInDb.setEmail(customerInForm.getEmail());
        customerInDb.setBirthday(customerInForm.getBirthday());
        customerInDb.setNote(customerInForm.getNote());

        return customerRepository.save(customerInDb); // chỉ 1 câu SQL update
    }

    public void delete(Long id) throws CustomerNotFoundException{

        Optional<Customer> customer = customerRepository.findById(id);
        if(customer.isPresent()){
            customerRepository.deleteById(id);

        }else{
            throw new CustomerNotFoundException("Không tìm thấy thông tin khách hàng ID:"+id);
        }

    }

}
