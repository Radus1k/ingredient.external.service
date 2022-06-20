package request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IngredientRequest {
    @Size(min = 3, message = "must be at least 3 characters long")
    @NotNull(message = "can not be null")
    private String name;
}
