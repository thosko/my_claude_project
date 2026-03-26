package se.ts.invoice.domain.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Defines the available roles and the permissions each role grants.
 *
 * <p>To add a new role: add a new enum constant with its permission set here.
 * To extend what a role can do: update its {@link EnumSet} here.
 * Service methods and {@code @PreAuthorize} guards never need to change.</p>
 *
 * <p>Example: both {@code ADMIN} and {@code MANAGER} grant {@code INVOICE_READ},
 * so any method guarded with {@code hasAuthority('INVOICE_READ')} allows both roles
 * without any code change at the call site.</p>
 */
public enum Role {

    /** Full access to all invoice operations. */
    ADMIN(EnumSet.allOf(Permission.class)),

    /** Can read all invoices, create and update — but cannot delete. */
    MANAGER(EnumSet.of(
            Permission.INVOICE_READ_ALL,
            Permission.INVOICE_CREATE,
            Permission.INVOICE_UPDATE
    )),

    /** Can read own-scope invoices only. */
    EMPLOYEE(EnumSet.of(Permission.INVOICE_READ)),

    /** No invoice permissions granted by default. */
    USER(EnumSet.noneOf(Permission.class));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    /**
     * Returns the set of permissions granted by this role.
     *
     * @return unmodifiable set of permissions
     */
    public Set<Permission> getPermissions() {
        return permissions;
    }
}
