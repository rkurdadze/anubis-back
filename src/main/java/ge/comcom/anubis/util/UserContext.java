package ge.comcom.anubis.util;

import ge.comcom.anubis.entity.security.User;
import ge.comcom.anubis.enums.UserStatus;
import lombok.experimental.UtilityClass;

/**
 * Temporary placeholder for authenticated user context.
 * In future this will be replaced by integration with Keycloak or Spring Security.
 */
@UtilityClass
public class UserContext {

    private static final Long SYSTEM_USER_ID = 1L;
    private static final String SYSTEM_USERNAME = "system";
    private static final String SYSTEM_FULLNAME = "System User";

    /**
     * Returns a placeholder "system" user.
     * This should be replaced with real authentication context later.
     */
    public User getCurrentUser() {
        User user = new User();
        user.setId(SYSTEM_USER_ID);
        user.setUsername(SYSTEM_USERNAME);
        user.setFullName(SYSTEM_FULLNAME);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
