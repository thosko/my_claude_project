package se.ts.invoice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import se.ts.invoice.domain.security.Role;
import se.ts.invoice.domain.security.TokenClaims;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mock implementation of {@link TokenValidator} for development and testing.
 *
 * <p>Expects tokens in the format: {@code tenantId|userId|ROLE1,ROLE2}</p>
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code tenant-abc|user-123|ADMIN}</li>
 *   <li>{@code tenant-abc|user-456|MANAGER}</li>
 *   <li>{@code tenant-abc|user-789|USER}</li>
 * </ul>
 *
 * <p>Activated by the Spring profile {@code mock-auth}.
 * Replace with {@code KeycloakTokenValidator} (profile {@code prod}) when Keycloak is ready.</p>
 */
@Component
@Profile("mock-auth")
public class MockTokenValidator implements TokenValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MockTokenValidator.class);
    private static final String TOKEN_DELIMITER = "\\|";
    private static final String ROLE_DELIMITER = ",";
    private static final int EXPECTED_PART_COUNT = 3;
    private static final int TENANT_ID_INDEX = 0;
    private static final int USER_ID_INDEX = 1;
    private static final int ROLES_INDEX = 2;

    @Override
    public TokenClaims validate(String token) {
        LOG.debug("Validating mock token");
        String[] parts = token.split(TOKEN_DELIMITER);
        if (parts.length != EXPECTED_PART_COUNT) {
            throw new BadCredentialsException(
                    "Invalid mock token format. Expected: tenantId|userId|ROLE1,ROLE2");
        }

        String tenantId = parts[TENANT_ID_INDEX].trim();
        String userId = parts[USER_ID_INDEX].trim();
        Set<Role> roles = parseRoles(parts[ROLES_INDEX].trim());

        if (tenantId.isEmpty() || userId.isEmpty()) {
            throw new BadCredentialsException("tenantId and userId must not be empty");
        }

        LOG.debug("Mock token validated: user={}, tenant={}, roles={}", userId, tenantId, roles);
        return new TokenClaims(userId, tenantId, roles);
    }

    private Set<Role> parseRoles(String rolesString) {
        return Arrays.stream(rolesString.split(ROLE_DELIMITER))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> {
                    try {
                        return Role.valueOf(r.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new BadCredentialsException("Unknown role: " + r);
                    }
                })
                .collect(Collectors.toSet());
    }
}
