package se.ts.invoice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import se.ts.invoice.domain.security.Permission;
import se.ts.invoice.domain.security.Role;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionResolverTest {

    private PermissionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PermissionResolver();
    }

    @Test
    void resolveAuthorities_adminRole_grantsAllPermissions() {
        // when
        Collection<GrantedAuthority> authorities = resolver.resolveAuthorities(Set.of(Role.ADMIN));

        // then
        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        Permission.INVOICE_READ.name(),
                        Permission.INVOICE_READ_ALL.name(),
                        Permission.INVOICE_CREATE.name(),
                        Permission.INVOICE_UPDATE.name(),
                        Permission.INVOICE_DELETE.name()
                );
    }

    @Test
    void resolveAuthorities_managerRole_grantsReadAllCreateUpdate_notDeleteOrRestrictedRead() {
        // when
        Collection<GrantedAuthority> authorities = resolver.resolveAuthorities(Set.of(Role.MANAGER));

        // then
        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        Permission.INVOICE_READ_ALL.name(),
                        Permission.INVOICE_CREATE.name(),
                        Permission.INVOICE_UPDATE.name()
                )
                .doesNotContain(Permission.INVOICE_DELETE.name(), Permission.INVOICE_READ.name());
    }

    @Test
    void resolveAuthorities_userRole_grantsNoPermissions() {
        // when
        Collection<GrantedAuthority> authorities = resolver.resolveAuthorities(Set.of(Role.USER));

        // then
        assertThat(authorities).isEmpty();
    }

    @Test
    void resolveAuthorities_emptyRoles_returnsEmptyCollection() {
        // when
        Collection<GrantedAuthority> authorities = resolver.resolveAuthorities(Set.of());

        // then
        assertThat(authorities).isEmpty();
    }

    @Test
    void resolveAuthorities_multipleRoles_returnsUnionOfPermissions() {
        // given — USER has none, MANAGER adds READ_ALL/CREATE/UPDATE
        // when
        Collection<GrantedAuthority> authorities = resolver.resolveAuthorities(Set.of(Role.USER, Role.MANAGER));

        // then
        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        Permission.INVOICE_READ_ALL.name(),
                        Permission.INVOICE_CREATE.name(),
                        Permission.INVOICE_UPDATE.name()
                );
    }

    @Test
    void resolveAuthorities_overlappingRoles_deduplicatesPermissions() {
        // given — ADMIN and MANAGER both grant INVOICE_READ_ALL; should appear only once
        // when
        Collection<GrantedAuthority> authorities = resolver.resolveAuthorities(Set.of(Role.ADMIN, Role.MANAGER));

        // then
        long readAllCount = authorities.stream()
                .filter(a -> a.getAuthority().equals(Permission.INVOICE_READ_ALL.name()))
                .count();
        assertThat(readAllCount).isEqualTo(1);
    }

    private Set<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
