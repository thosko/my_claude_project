package se.ts.invoice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import se.ts.invoice.domain.Invoice;
import se.ts.invoice.repository.InvoiceRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@code @PreAuthorize} guards on {@link InvoiceService} are enforced.
 * Uses a minimal Spring context so AOP proxies are active.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InvoiceServiceSecurityTest.TestConfig.class)
class InvoiceServiceSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        public InvoiceRepository invoiceRepository() {
            InvoiceRepository repo = mock(InvoiceRepository.class);
            when(repo.generateInvoiceNumber()).thenReturn("INV-1001");
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            return repo;
        }

        @Bean
        public InvoiceService invoiceService(InvoiceRepository repo) {
            return new InvoiceService(repo);
        }
    }

    @Autowired
    private InvoiceService invoiceService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createInvoice_withInvoiceCreateAuthority_succeeds() {
        // given
        authenticateWithAuthority("INVOICE_CREATE");

        // when
        Invoice result = invoiceService.createInvoice(
                "tenant-abc", "cust-1", "Acme Corp",
                Instant.now(), Instant.now().plusSeconds(2592000L),
                List.of(), "SEK");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo("tenant-abc");
    }

    @Test
    void createInvoice_withNoAuthorities_throwsAccessDeniedException() {
        // given — authenticated user with no granted authorities
        authenticateWithAuthority(/* none — empty list */ null);

        // when / then
        assertThatThrownBy(() -> invoiceService.createInvoice(
                "tenant-abc", "cust-1", "Acme Corp",
                Instant.now(), Instant.now().plusSeconds(2592000L),
                List.of(), "SEK"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createInvoice_withWrongAuthority_throwsAccessDeniedException() {
        // given
        authenticateWithAuthority("INVOICE_READ");

        // when / then
        assertThatThrownBy(() -> invoiceService.createInvoice(
                "tenant-abc", "cust-1", "Acme Corp",
                Instant.now(), Instant.now().plusSeconds(2592000L),
                List.of(), "SEK"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void authenticateWithAuthority(String authority) {
        var authorities = authority != null
                ? List.of(new SimpleGrantedAuthority(authority))
                : List.<SimpleGrantedAuthority>of();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, authorities));
    }
}
