package sapo.com.model.dto.response.user;

import lombok.*;
import sapo.com.model.entity.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private Boolean status;
    private LocalDate birthDay;
    private Set<Role> roles;
    private LocalDateTime createdOn;
    private LocalDateTime updateOn;

}
