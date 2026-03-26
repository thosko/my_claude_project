package se.kleer.invoice.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Spring Security configuration.
 *
 * <p>This application exposes gRPC endpoints only — there is no HTTP server.
 * All authentication is handled by {@link AuthTenantInterceptor}, which
 * validates the bearer token and populates the {@link org.springframework.security.core.context.SecurityContext}
 * before each service method is invoked.</p>
 *
 * <p>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} support on
 * Spring-managed beans (e.g. {@link se.kleer.invoice.service.InvoiceService}).</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
}
