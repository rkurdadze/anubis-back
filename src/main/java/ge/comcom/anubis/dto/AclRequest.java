package ge.comcom.anubis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AclRequest {

    @NotBlank
    @Size(max = 255)
    private String name;
}
