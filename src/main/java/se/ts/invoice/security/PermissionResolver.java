package se.ts.invoice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import se.ts.invoice.domain.security.Role;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a set of {@link Role}s into Spring Security {@link GrantedAuthority} instances.
 *
 * <p>Each role's permissions are expanded into individual authorities, enabling
 * {@code @PreAuthorize("hasAuthority('INVOICE_READ')")} checks on service methods.
 * Because checks are permission-based (not role-based), adding new roles never
 * requires touching the service layer.</p>
 */
@Component
public class PermissionResolver {

    /**
     * Expands the given roles into a flat collection of permission-based authorities.
     *
     * @param roles the roles to expand
     * @return a collection of {@link GrantedAuthority} instances, one per unique permission
     */
    public Collection<GrantedAuthority> resolveAuthorities(Set<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toSet());
    }
}
