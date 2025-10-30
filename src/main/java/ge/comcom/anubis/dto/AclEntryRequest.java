package ge.comcom.anubis.dto;

import ge.comcom.anubis.enums.GranteeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AclEntryRequest {

    @NotNull
    private GranteeType granteeType;

    @NotNull
    private Long granteeId;

    private Boolean canRead;
    private Boolean canWrite;
    private Boolean canDelete;
    private Boolean canChangeAcl;
}
