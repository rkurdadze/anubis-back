package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.GranteeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclEntryDto {

    private Long id;
    private Long aclId;
    private GranteeType granteeType;
    private Long granteeId;
    private Boolean canRead;
    private Boolean canWrite;
    private Boolean canDelete;
    private Boolean canChangeAcl;
    private SecurityPrincipalDto principal;
}
