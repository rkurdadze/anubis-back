package ge.comcom.anubis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class GroupRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    private Set<Long> memberIds;
}
