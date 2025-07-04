package sapo.com.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import sapo.com.exception.UserException;
import sapo.com.model.dto.request.*;
import sapo.com.model.dto.response.UserResponse;
import sapo.com.model.dto.response.user.UserResponseDTO;
import sapo.com.model.entity.Role;
import sapo.com.model.entity.User;



public interface UserService {
    User register(User user, Long storeId) throws Exception;
    User registerV2(User user, Long storeId) throws Exception;
    UserResponse login (UserRequest userRequest) throws Exception;

    UserResponse loginForEmployee(UserRequestForEmployee userRequest) throws Exception;
    User resetPasswordByEmail (String email) throws Exception;
    User resetPasswordByEmailAndPhone(String email, String phoneNumber) throws Exception;
    User resetPassword (Long id) throws Exception;
    Page<User> findAll(Pageable pageable);
    Page<User> findUsersByFilter(Pageable pageable, String search, String role, Long storeId);
    Page<User> findAllByRolesName(Pageable pageable , String role) ;

    Page<User> findAllBySearch(Pageable pageable , String search);

    User update (Long id , User user) throws Exception;

    User updateRole (Long id , Role role) throws Exception;

    User findById(Long id) throws UserException;
    UserResponseDTO getUserById(Long id) throws UserException;

    User findByName(String name) throws Exception;

    User findByPhoneNumber(String phoneNumber) throws Exception;

    User findByEmail(String email) throws Exception;

    void existPhoneNumber (String phoneNumber) throws Exception;

    void existEmail(String email) throws Exception ;

    User changPassword (Long id , PasswordRequest passwordRequest) throws Exception;

    void deleteById (Long id) throws Exception;

    User findUserProfileByJwt(String jwt) throws UserException;
}
