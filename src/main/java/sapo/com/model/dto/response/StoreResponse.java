package sapo.com.model.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String status;
    private String city;
    private String district;
    private String ward;
    private Long createdAt;
    private Long modifiedOn;
}