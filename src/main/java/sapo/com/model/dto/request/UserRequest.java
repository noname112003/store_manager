package sapo.com.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sapo.com.validator.phone.TenCharacter;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserRequest {
//    @Email(message = "Email is not valid", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
//    @NotEmpty(message = "Email cannot be empty")
    @TenCharacter
    private String phoneNumber ;
    private String password ;
}
