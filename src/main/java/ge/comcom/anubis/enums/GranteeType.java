package ge.comcom.anubis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the type of ACL grantee: user or group.
 */
@Schema(description = "Defines whether ACL entry applies to a USER or a GROUP")
public enum GranteeType {
    USER,
    GROUP
}

