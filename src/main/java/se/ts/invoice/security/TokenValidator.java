package se.ts.invoice.security;

import se.ts.invoice.domain.security.TokenClaims;

/**
 * Contract for validating authentication tokens and extracting claims.
 *
 * <p>Implementations are swapped via Spring profiles:</p>
 * <ul>
 *   <li>{@code mock-auth} profile — {@link MockTokenValidator} for development/testing</li>
 *   <li>{@code prod} profile — {@code KeycloakTokenValidator} validates against Keycloak JWKS</li>
 * </ul>
 *
 * <p>The rest of the system depends only on this interface, so switching
 * from mock to Keycloak requires no changes outside this package.</p>
 */
public interface TokenValidator {

    /**
     * Validates the given token and returns the extracted claims.
     *
     * @param token the raw token string (without the {@code Bearer } prefix)
     * @return the validated {@link TokenClaims} containing subject, tenantId, and roles
     * @throws org.springframework.security.core.AuthenticationException if the token
     *         is invalid, expired, or malformed
     */
    TokenClaims validate(String token);
}
