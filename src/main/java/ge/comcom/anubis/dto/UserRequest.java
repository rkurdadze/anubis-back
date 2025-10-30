package ge.comcom.anubis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequest {

    @NotBlank
    @Size(max = 255)
    private String username;

    @Size(max = 255)
    private String fullName;

    @Size(max = 255)
    private String passwordHash;

    private Set<Long> groupIds;
}
