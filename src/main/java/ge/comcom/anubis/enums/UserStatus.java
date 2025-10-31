package ge.comcom.anubis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents login account status similar to M-Files user management.
 */
@Schema(description = "User account status. ACTIVE users can sign in, INACTIVE/LOCKED are disabled.")
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED
}
