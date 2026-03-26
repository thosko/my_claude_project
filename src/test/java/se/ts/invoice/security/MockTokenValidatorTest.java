package se.ts.invoice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import se.ts.invoice.domain.security.Role;
import se.ts.invoice.domain.security.TokenClaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockTokenValidatorTest {

    private MockTokenValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MockTokenValidator();
    }

    @Test
    void validate_validAdminToken_returnsCorrectClaims() {
        // when
        TokenClaims claims = validator.validate("tenant-abc|user-123|ADMIN");

        // then
        assertThat(claims.tenantId()).isEqualTo("tenant-abc");
        assertThat(claims.subject()).isEqualTo("user-123");
        assertThat(claims.roles()).containsExactly(Role.ADMIN);
    }

    @Test
    void validate_multipleRoles_parsesAllRoles() {
        // when
        TokenClaims claims = validator.validate("tenant-abc|user-456|ADMIN,MANAGER");

        // then
        assertThat(claims.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.MANAGER);
    }

    @Test
    void validate_lowercaseRole_parsedCaseInsensitively() {
        // when
        TokenClaims claims = validator.validate("tenant-abc|user-1|admin");

        // then
        assertThat(claims.roles()).containsExactly(Role.ADMIN);
    }

    @Test
    void validate_mixedCaseRole_parsedCaseInsensitively() {
        // when
        TokenClaims claims = validator.validate("tenant-abc|user-1|Manager");

        // then
        assertThat(claims.roles()).containsExactly(Role.MANAGER);
    }

    @Test
    void validate_tokenWithWhitespace_trimsFields() {
        // when
        TokenClaims claims = validator.validate(" tenant-abc | user-1 | ADMIN ");

        // then
        assertThat(claims.tenantId()).isEqualTo("tenant-abc");
        assertThat(claims.subject()).isEqualTo("user-1");
    }

    @Test
    void validate_missingParts_throwsBadCredentialsException() {
        assertThatThrownBy(() -> validator.validate("tenant-abc|user-123"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid mock token format");
    }

    @Test
    void validate_tooManyParts_throwsBadCredentialsException() {
        assertThatThrownBy(() -> validator.validate("a|b|ADMIN|extra"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validate_emptyTenantId_throwsBadCredentialsException() {
        assertThatThrownBy(() -> validator.validate("|user-1|ADMIN"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("tenantId and userId must not be empty");
    }

    @Test
    void validate_emptyUserId_throwsBadCredentialsException() {
        assertThatThrownBy(() -> validator.validate("tenant-abc||ADMIN"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("tenantId and userId must not be empty");
    }

    @Test
    void validate_unknownRole_throwsBadCredentialsException() {
        assertThatThrownBy(() -> validator.validate("tenant-abc|user-1|SUPERUSER"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Unknown role: SUPERUSER");
    }
}
