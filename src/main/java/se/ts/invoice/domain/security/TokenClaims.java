package se.ts.invoice.domain.security;

import java.util.Set;

/**
 * Immutable value object representing the validated claims extracted from an
 * authentication token.
 *
 * <p>This is a pure domain record — it has no dependency on any token format
 * (JWT, opaque token, etc.) and is populated by
 * {@link se.ts.invoice.security.TokenValidator}.
 * When Keycloak is introduced, its validator maps Keycloak JWT claims into
 * this record; the rest of the system remains unchanged.</p>
 *
 * @param subject  the authenticated user identifier (e.g. Keycloak subject UUID)
 * @param tenantId the tenant the user belongs to
 * @param roles    the roles assigned to the user within this tenant
 */
public record TokenClaims(String subject, String tenantId, Set<Role> roles) {
}
