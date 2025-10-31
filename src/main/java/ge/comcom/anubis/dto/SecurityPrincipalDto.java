package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.GranteeType;
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
public class SecurityPrincipalDto {

    private Long id;
    private GranteeType type;
    private String displayName;
    private String login;
    private String description;
    private UserStatus status;
    private Set<Long> groupIds;
    private Set<Long> memberIds;
    private Set<Long> directRoleIds;
    private Set<Long> effectiveRoleIds;
    private Set<RoleSummaryDto> directRoles;
    private Set<RoleSummaryDto> effectiveRoles;
}
