package sapo.com.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserRequestForEmployee {
    private String adminPhoneNumber;
    private String employeePhoneNumber;
    private String employeePassword;
}
