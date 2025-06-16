package sapo.com.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sapo.com.model.dto.response.StoreResponse;

@Getter
@Setter
@Entity
@Table(name = "stores")
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String status;
    private Long createdAt;
    private Long modifiedOn;
    private String city;
    private String district;
    private String ward;

    public StoreResponse transferToResponse() {
        StoreResponse response = new StoreResponse();
        response.setId(this.getId());
        response.setName(this.getName());
        response.setAddress(this.getAddress());
        response.setPhone(this.getPhone());
        response.setStatus(this.getStatus());
        response.setCity(this.getCity());
        response.setDistrict(this.getDistrict());
        response.setWard(this.getWard());
        response.setCreatedAt(this.getCreatedAt());
        response.setModifiedOn(this.getModifiedOn());
        return response;
    }
}
