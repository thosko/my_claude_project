package se.ts.invoice.domain.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void admin_hasAllPermissions() {
        assertThat(Role.ADMIN.getPermissions())
                .containsExactlyInAnyOrder(
                        Permission.INVOICE_READ,
                        Permission.INVOICE_READ_ALL,
                        Permission.INVOICE_CREATE,
                        Permission.INVOICE_UPDATE,
                        Permission.INVOICE_DELETE
                );
    }

    @Test
    void manager_hasReadAllCreateUpdate_butNotDeleteOrRestrictedRead() {
        assertThat(Role.MANAGER.getPermissions())
                .containsExactlyInAnyOrder(
                        Permission.INVOICE_READ_ALL,
                        Permission.INVOICE_CREATE,
                        Permission.INVOICE_UPDATE
                )
                .doesNotContain(Permission.INVOICE_DELETE, Permission.INVOICE_READ);
    }

    @Test
    void employee_hasRestrictedReadOnly() {
        assertThat(Role.EMPLOYEE.getPermissions())
                .containsExactly(Permission.INVOICE_READ)
                .doesNotContain(
                        Permission.INVOICE_READ_ALL,
                        Permission.INVOICE_CREATE,
                        Permission.INVOICE_UPDATE,
                        Permission.INVOICE_DELETE
                );
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
