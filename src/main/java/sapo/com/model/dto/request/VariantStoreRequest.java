package sapo.com.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VariantStoreRequest {
    @NotNull
    private Long storeId;

    @NotNull
    @Min(0)
    private Long quantity;
}
