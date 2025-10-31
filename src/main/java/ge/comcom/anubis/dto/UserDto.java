package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String username;
    private String fullName;
    private Set<Long> groupIds;
    private Set<Long> roleIds;
    private UserStatus status;
}
