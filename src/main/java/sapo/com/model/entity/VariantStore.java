package sapo.com.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;


//@Entity
//@NoArgsConstructor
//@AllArgsConstructor
//@Getter
//@Setter
//@Builder
//@Table(name = "variant_store")
//public class VariantStore {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    private Long variantId;
//    private Long storeId;
//    private Long quantity;
//}


@Entity
@Table(name = "variant_store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(name = "variant_id", insertable = false, updatable = false)
//    private Long variantId;

    private Long storeId;
    private Long quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    @JsonBackReference(value = "variant-variantStore")
    private Variant variant;

}
