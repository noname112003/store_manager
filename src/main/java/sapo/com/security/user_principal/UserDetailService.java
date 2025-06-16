package sapo.com.security.user_principal;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sapo.com.model.entity.User;
import sapo.com.repository.UserRepository;

@Service
public class UserDetailService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository ;

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {

        User user = userRepository.findByPhoneNumber(phoneNumber);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with phone number: " + phoneNumber);
        }
        return UserPrincipal.build(user);
    }
}

