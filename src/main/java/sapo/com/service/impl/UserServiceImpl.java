package sapo.com.service.impl;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sapo.com.exception.UserException;
import sapo.com.model.dto.request.*;
import sapo.com.model.dto.response.UserResponse;
import sapo.com.model.dto.response.user.UserResponseDTO;
import sapo.com.model.entity.Role;
import sapo.com.model.entity.User;
import sapo.com.model.entity.UserStore;
import sapo.com.repository.UserRepository;
import sapo.com.repository.UserStoreRepository;
import sapo.com.security.jwt.JwtProvider;
import sapo.com.security.user_principal.UserPrincipal;
import sapo.com.service.RoleService;
import sapo.com.service.UserService;


import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder ;
    @Autowired
    private AuthenticationProvider authenticationProvider;
    @Autowired
    private JwtProvider jwtProvider ;
    @Autowired
    private RoleService roleService;
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserStoreRepository userStoreRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateNewPassword() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int randomIndex = RANDOM.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }

    public void sendNewPasswordEmail(String email, String newPassword)
            throws MessagingException, UnsupportedEncodingException {

        // Tạo nội dung HTML
        String htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset Password</title>
            </head>
            <body>
                <p>Bạn đã yêu cầu đặt lại mật khẩu. Đây là mật khẩu mới của bạn:</p>
                <p style="font-size: 1.5em; color: #007BFF; font-weight: bold;">%s</p>
                <p>Vui lòng sử dụng mật khẩu này để đăng nhập và thay đổi mật khẩu nếu cần.</p>
            </body>
            </html>
            """.formatted( newPassword);

        // Tạo email
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setTo(email);
        helper.setSubject("Mật khẩu mới của bạn");
        helper.setText(htmlContent, true); // true để gửi email HTML
        helper.setFrom("quangteo7112003@gmail.com", "Sapo");

        // Gửi email
        mailSender.send(mimeMessage);
    }
    @Override
    public User resetPasswordByEmail(String email) throws Exception {
        // Lấy User từ email
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("Không tìm thấy người dùng với email: " + email);
        }

        // Tạo mật khẩu mới
        String newPassword = generateNewPassword();

        // Mã hóa mật khẩu mới
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        // Lưu mật khẩu mới vào cơ sở dữ liệu
        userRepository.save(user);

        // Gửi email chứa mật khẩu mới
        try {
            sendNewPasswordEmail(email, newPassword);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new Exception("Không thể gửi email chứa mật khẩu mới: " + e.getMessage());
        }

        return user;
    }

    @Override
    public User resetPasswordByEmailAndPhone(String email, String phoneNumber) throws Exception {
        // Lấy User từ email
        User user = userRepository.findByEmail(email);
        if (user == null || !user.getPhoneNumber().equals(phoneNumber)) {
            throw new Exception("Không tìm thấy người dùng với email và số điện thoại tương ứng.");
        }

        // Tạo mật khẩu mới
        String newPassword = generateNewPassword();

        // Mã hóa mật khẩu mới
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        // Lưu mật khẩu mới vào cơ sở dữ liệu
        userRepository.save(user);

        // Gửi email chứa mật khẩu mới
        try {
            sendNewPasswordEmail(email, newPassword);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new Exception("Không thể gửi email chứa mật khẩu mới: " + e.getMessage());
        }

        return user;
    }


    @Override
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .status(user.getStatus())
                .birthDay(user.getBirthDay())
                .roles(user.getRoles()) // hoặc convert sang RoleDTO nếu cần
                .createdOn(user.getCreatedOn())
                .updateOn(user.getUpdateOn())
                .build();
    }

    @Override
    public Page<User> findUsersByFilter(Pageable pageable, String search, String role, Long storeId) {
        return userRepository.findUsersByFilter(storeId, role, search, pageable);
    }


    @Override
    public User register(User user, Long storeId) throws Exception {
//        ma hoa mat khau
        user.setPassword(passwordEncoder.encode(user.getPassword()));
//        roles
        Set<Role> roles = new HashSet<>();

//        register cua user thi coi no la USER
        if(user.getRoles() == null || user.getRoles().isEmpty()){
            roles.add(roleService.findByName("ROLE_ADMIN") );
        }else {

//        Tao tk va phan quyen thi phai co quyen ADMIN
            user.getRoles().forEach(role -> {
                try {
                    roles.add(roleService.findByName(role.getName()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        }



        User newUser = new User() ;
        newUser.setName(user.getName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(user.getPassword());
        newUser.setStatus(true);
        newUser.setRoles(roles);
        newUser.setAddress(user.getAddress());
        newUser.setPhoneNumber(user.getPhoneNumber());
        newUser.setBirthDay(user.getBirthDay());
        newUser.setCreatedOn(LocalDateTime.now());
        User savedUser = userRepository.save(newUser);
        if (storeId != null) {
            UserStore userStore = new UserStore();
            userStore.setUserId(savedUser.getId());
            userStore.setStoreId(storeId);
            userStoreRepository.save(userStore);
        }
        return savedUser;

    }

    @Override
    public User registerV2(User user, Long storeId) throws Exception {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại.");
        }


        if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại.");
        }


        user.setPassword(passwordEncoder.encode(user.getPassword()));


        Set<Role> roles = new HashSet<>();
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            roles.add(roleService.findByName("ROLE_ADMIN"));
        } else {
            for (Role role : user.getRoles()) {
                roles.add(roleService.findByName(role.getName()));
            }
        }


        User newUser = new User();
        newUser.setName(user.getName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(user.getPassword());
        newUser.setStatus(true);
        newUser.setRoles(roles);
        newUser.setAddress(user.getAddress());
        newUser.setPhoneNumber(user.getPhoneNumber());
        newUser.setBirthDay(user.getBirthDay());
        newUser.setCreatedOn(LocalDateTime.now());
        newUser.setUpdateOn(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);


        if (storeId != null) {
            UserStore userStore = new UserStore();
            userStore.setUserId(savedUser.getId());
            userStore.setStoreId(storeId);
            userStoreRepository.save(userStore);
        }

        return savedUser;
    }
    @Override
    public UserResponse login(UserRequest userRequest) throws Exception {
        try {

            Authentication authentication ;
            authentication = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(userRequest.getPhoneNumber(),userRequest.getPassword()));
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            // In ra các quyền của người dùng để kiểm tra
            System.out.println("Các quyền của người dùng: ");
            userPrincipal.getAuthorities().forEach(grantedAuthority ->
                    System.out.println(grantedAuthority.getAuthority())
            );

            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));

            // Truy vấn các storeId từ bảng user_store_mapping
//            List<Long> storeIds = userStoreRepository.findStoreIdsByUserId(userPrincipal.getId());
            List<Long> storeIds = userStoreRepository.findActiveStoreIdsByUserId(userPrincipal.getId());
            if (!isAdmin) {
                if (storeIds == null || storeIds.isEmpty()) {
                    throw new Exception("Chi nhánh không còn hoạt động.");
                }

                // Nhân viên chỉ được login với 1 chi nhánh
                Long storeId = storeIds.get(0);

                return UserResponse.builder()
                        .token(jwtProvider.generateToken(userPrincipal))
                        .id(userPrincipal.getId())
                        .phoneNumber(userPrincipal.getPhoneNumber())
                        .name(userPrincipal.getName())
                        .roles(userPrincipal.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet()))
                        .storeIds(List.of(storeId))
                        .build();
            }
            return UserResponse.builder()
                    .token(jwtProvider.generateToken(userPrincipal))
                    .id(userPrincipal.getId())
                    .phoneNumber(userPrincipal.getPhoneNumber())
                    .name(userPrincipal.getName())
                    .roles(userPrincipal.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet()))
                    .storeIds(storeIds)
                    .build();

        } catch (BadCredentialsException e) {
            // Handle incorrect email or password scenario
            throw new Exception("Số điện thoại hoặc password không chính xác . Vui lòng thử lại .");
        } catch (DisabledException e) {
            // Handle account disabled scenario
            throw new Exception("Tài khoản của bạn đã bị khóa . Vui lòng liên hệ chủ cửa hàng để được hỗ trợ .");
        }catch (AuthenticationException authenticationException){
            System.err.println(authenticationException);
            throw new Exception("Xác thực không thành công. Vui lòng kiểm tra thông tin đăng nhập của bạn.");

        }
    }

    @Override
    public UserResponse loginForEmployee(UserRequestForEmployee userRequest) throws Exception {
        try {
            // Kiểm tra Admin qua số điện thoại
            User admin = userRepository.findByPhoneNumber(userRequest.getAdminPhoneNumber());
            if (admin == null || !admin.getRoles().contains("ROLE_ADMIN")) {
                throw new Exception("Không tồn tại thông tin admin .");
            }

            // Tìm nhân viên qua số điện thoại và mật khẩu
            User employee = userRepository.findByPhoneNumber(userRequest.getEmployeePhoneNumber());
            if (employee == null || !passwordEncoder.matches(userRequest.getEmployeePassword(), employee.getPassword())) {
                throw new Exception("Tài khoản hoặc mật khẩu không chính xác.");
            }

            // Kiểm tra xem nhân viên có thuộc store mà admin quản lý không
            List<Long> adminStoreIds = userStoreRepository.findStoreIdsByUserId(admin.getId());
            if (adminStoreIds.isEmpty()) {
                throw new Exception("Admin không quản lý store nào.");
            }

            // Kiểm tra xem nhân viên có thuộc store mà admin quản lý không
            boolean isEmployeeOfAdminStore = userStoreRepository.findByUserIdAndStoreId(employee.getId(), adminStoreIds.get(0)).size() > 0;
            if (!isEmployeeOfAdminStore) {
                throw new Exception("Nhân viên không thuộc store mà admin quản lý.");
            }

            // Tạo Authentication cho nhân viên (sử dụng phoneNumber của nhân viên)
            Authentication authentication = authenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(userRequest.getEmployeePhoneNumber(), userRequest.getEmployeePassword()));
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // Truy vấn các storeId mà nhân viên có quyền truy cập
            List<Long> storeIds = userStoreRepository.findStoreIdsByUserId(userPrincipal.getId());

            return UserResponse.builder()
                    .token(jwtProvider.generateToken(userPrincipal))
                    .id(userPrincipal.getId())
                    .phoneNumber(userPrincipal.getPhoneNumber())
                    .name(userPrincipal.getName())
                    .roles(userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
                    .storeIds(storeIds)
                    .build();
        } catch (BadCredentialsException e) {
            throw new Exception("Số điện thoại hoặc mật khẩu không chính xác.");
        } catch (DisabledException e) {
            throw new Exception("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ chủ cửa hàng.");
        } catch (AuthenticationException e) {
            throw new Exception("Xác thực không thành công. Vui lòng kiểm tra lại thông tin đăng nhập.");
        }
    }
    @Override
    public User resetPassword(Long id) throws Exception {
        Optional<User> user = userRepository.findById(id) ;
        if (user.isPresent()){
            User updatePasswordUser = user.get();
            updatePasswordUser.setPassword(passwordEncoder.encode("123456"));
            return userRepository.save(updatePasswordUser);
        }else {
            throw new Exception("Id khong tim thấy");
        }
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> findAllByRolesName(Pageable pageable, String role) {
        return userRepository.findAllByRolesName(role, pageable);
    }

    @Override
    public Page<User> findAllBySearch(Pageable pageable , String search){
        return userRepository.findBySearch(search ,pageable);

    }

//    @Override
//    public User update(Long id, User user) throws Exception {
//
//        Optional<User> findByIdUser = userRepository.findById(id);
//        if (findByIdUser.isPresent()){
//
//            User updateUser = findByIdUser.get();
//            updateUser.setName(user.getName());
//            updateUser.setEmail(user.getEmail());
////            updateUser.setPassword(passwordEncoder.encode(updateUser.getPassword()));
////            updateUser.setPassword(updateUser.getPassword());
//            updateUser.setAddress(user.getAddress());
//            updateUser.setPhoneNumber(user.getPhoneNumber());
//            updateUser.setRoles(user.getRoles());
//            updateUser.setStatus(user.getStatus());
//            updateUser.setBirthDay(user.getBirthDay());
//            updateUser.setUpdateOn(LocalDateTime.now());
//            if (user.getPassword() != null && !user.getPassword().isBlank()) {
//                // Kiểm tra mật khẩu mới sau khi encode có khác mật khẩu cũ không
//                if (!passwordEncoder.matches(user.getPassword(), updateUser.getPassword())) {
//                    updateUser.setPassword(passwordEncoder.encode(user.getPassword()));
//                }
//            }
//
//            return userRepository.save(updateUser);
//        }else {
//            throw new Exception("ID không tìm thấy");
//        }
//    }
@Override
public User update(Long id, User user) throws Exception {
    User updateUser = userRepository.findById(id)
            .orElseThrow(() -> new Exception("ID không tìm thấy"));

    // Chỉ set khi khác
    if (!Objects.equals(updateUser.getName(), user.getName())) {
        updateUser.setName(user.getName());
    }

    if (!Objects.equals(updateUser.getEmail(), user.getEmail())) {
        updateUser.setEmail(user.getEmail());
    }

    if (!Objects.equals(updateUser.getAddress(), user.getAddress())) {
        updateUser.setAddress(user.getAddress());
    }

    if (!Objects.equals(updateUser.getPhoneNumber(), user.getPhoneNumber())) {
        updateUser.setPhoneNumber(user.getPhoneNumber());
    }

    if (!Objects.equals(updateUser.getStatus(), user.getStatus())) {
        updateUser.setStatus(user.getStatus());
    }

    if (!Objects.equals(updateUser.getBirthDay(), user.getBirthDay())) {
        updateUser.setBirthDay(user.getBirthDay());
    }

    // Chỉ set roles nếu thay đổi
    if (user.getRoles() != null && !user.getRoles().equals(updateUser.getRoles())) {
        updateUser.setRoles(user.getRoles());
    }

    // Xử lý mật khẩu mới
    if (user.getPassword() != null && !user.getPassword().isBlank()) {
        if (!passwordEncoder.matches(user.getPassword(), updateUser.getPassword())) {
            updateUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
    }

    updateUser.setUpdateOn(LocalDateTime.now());

    return userRepository.save(updateUser);
}

    @Override
    public User updateRole(Long id, Role role) throws Exception {
        Optional<User> userOptional = userRepository.findById(id);
        Role findRole = roleService.findByName(role.getName());

        if (userOptional.isPresent() && findRole != null) {
            User user = userOptional.get();
            Set<Role> roles = new HashSet<>();
            roles.add(findRole);
            user.setRoles(roles);

            // No need to encode or set the password here since we're only updating roles
            return userRepository.save(user);
        } else {
            throw new Exception("ID không tìm thấy");
        }
    }


    @Override
    public User findById(Long id) throws UserException {
        Optional<User> user = userRepository.findById(id);
        if(user.isPresent()){
            return user.get();
        }throw new UserException("user not found with id :" + id);

//        return null;
    }

    @Override
    public User findByName(String name) throws Exception {
        User user = userRepository.findByName(name);
        if(user != null){
            return user;
        }throw new Exception("Tên không tìm thấy");
    }

    @Override
    public User findByPhoneNumber(String phoneNumber) throws Exception {
        User user = userRepository.findByPhoneNumber(phoneNumber);
        if (user != null){
            return user;
        }else {
            throw new Exception("Số điện thoại không được tìm thấy");
        }
    }



    @Override
    public User findByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user != null){
            return user ;
        }else {
            throw new Exception("Email không được tìm thấy");
        }
    }

    @Override
    public void existPhoneNumber(String phoneNumber) throws Exception {
        User user = userRepository.findByPhoneNumber(phoneNumber);
        if (user != null){
            throw new Exception("Exist phone number");
        }else {
            System.out.println("Not exist phone number");
        }
    }

    @Override
    public void existEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user != null){
            throw new Exception("Exist email");
        }else {
            System.out.println("Not exist email");
        }

    }

    @Override
    public User changPassword(Long id , PasswordRequest passwordRequest) throws Exception {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()){
            User updatePasswordUser = user.get();
            if(!passwordEncoder.matches(passwordRequest.getOldPassword(), updatePasswordUser.getPassword())) {
                throw new Exception("Mật khẩu không chính xác");
            }
            else if (passwordEncoder.matches(passwordRequest.getPassword(), updatePasswordUser.getPassword())){
                throw new Exception("Mật khẩu không thay đổi");
            }
            updatePasswordUser.setPassword(passwordEncoder.encode(passwordRequest.getPassword()));
            return userRepository.save(updatePasswordUser);
        }else {
            throw new Exception("Id not found");
        }

    }

    @Override
    public void deleteById(Long id) throws Exception {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()){
            userRepository.deleteById(id);
        }else {
            throw new Exception("Id not found");
        }
    }

    @Override
    public User findUserProfileByJwt(String jwt) throws UserException{
        String email = jwtProvider.getUserNameFromToken(jwt);
        User user =userRepository.findByEmail(email);
        if(user==null){
            throw new UserException("user not found with email" + email);
        }
        return user;
    }


}
