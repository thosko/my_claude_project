package se.ts.invoice.domain.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void admin_hasAllPermissions() {
        assertThat(Role.ADMIN.getPermissions())
                .containsExactlyInAnyOrder(
                        Permission.INVOICE_READ,
                        Permission.INVOICE_CREATE,
                        Permission.INVOICE_UPDATE,
                        Permission.INVOICE_DELETE
                );
    }

    @Test
    void manager_hasReadCreateUpdate_butNotDelete() {
        assertThat(Role.MANAGER.getPermissions())
                .containsExactlyInAnyOrder(
                        Permission.INVOICE_READ,
                        Permission.INVOICE_CREATE,
                        Permission.INVOICE_UPDATE
                )
                .doesNotContain(Permission.INVOICE_DELETE);
    }

    @Test
    void user_hasNoPermissions() {
        assertThat(Role.USER.getPermissions()).isEmpty();
    }

    @Test
    void permissionSet_isUnmodifiable() {
        assertThat(Role.MANAGER.getPermissions())
                .satisfies(permissions ->
                        assertThat(permissions.getClass().getName())
                                .contains("Unmodifiable"));
    }
}
