package ge.comcom.anubis.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequest {

    @NotBlank
    @Size(max = 255)
    @JsonAlias("email")
    private String username;

    @Size(max = 255)
    @JsonAlias("name")
    private String fullName;

    @Size(max = 255)
    private String passwordHash;

    private Set<Long> groupIds;
}
