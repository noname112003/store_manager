package sapo.com.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreRequest {
    private String name;
    private String address;
    private String phone;
    private boolean status;
    private String city;
    private String district;
    private String ward;
    private Long userId;
}