package sapo.com.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserResponse {
    private Long id ;
    private String token ;
    private String name ;
    private String phoneNumber ;
    private Set<String> roles ;
    private List<Long> storeIds;
}
