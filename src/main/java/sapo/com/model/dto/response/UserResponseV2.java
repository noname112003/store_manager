package sapo.com.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseV2 {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private String address;
    private Boolean status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDay;

    private Set<String> roles;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdOn;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateOn;

    private List<Long> storeIds;
}