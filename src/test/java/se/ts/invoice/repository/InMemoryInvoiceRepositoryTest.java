package se.ts.invoice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.ts.invoice.domain.Invoice;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryInvoiceRepositoryTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";

    private InMemoryInvoiceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryInvoiceRepository();
    }

    @Test
    void save_andFindById_returnsInvoice() {
        // given
        Invoice invoice = invoiceFor("inv-1", TENANT_A);

        // when
        repository.save(invoice);
        Optional<Invoice> result = repository.findById("inv-1", TENANT_A);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("inv-1");
    }

    @Test
    void findById_wrongTenant_returnsEmpty() {
        // given
        Invoice invoice = invoiceFor("inv-1", TENANT_A);
        repository.save(invoice);

        // when
        Optional<Invoice> result = repository.findById("inv-1", TENANT_B);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        // when
        Optional<Invoice> result = repository.findById("does-not-exist", TENANT_A);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllByTenantId_returnsOnlyInvoicesForTenant() {
        // given
        repository.save(invoiceFor("inv-a1", TENANT_A));
        repository.save(invoiceFor("inv-a2", TENANT_A));
        repository.save(invoiceFor("inv-b1", TENANT_B));

        // when
        List<Invoice> results = repository.findAllByTenantId(TENANT_A);

        // then
        assertThat(results).hasSize(2)
                .extracting(Invoice::getId)
                .containsExactlyInAnyOrder("inv-a1", "inv-a2");
    }

    @Test
    void findAllByTenantId_noInvoicesForTenant_returnsEmptyList() {
        // given
        repository.save(invoiceFor("inv-b1", TENANT_B));

        // when
        List<Invoice> results = repository.findAllByTenantId(TENANT_A);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void save_overwritesExistingInvoiceWithSameId() {
        // given
        Invoice original = invoiceFor("inv-1", TENANT_A);
        original.setCustomerName("Original");
        repository.save(original);

        Invoice updated = invoiceFor("inv-1", TENANT_A);
        updated.setCustomerName("Updated");

        // when
        repository.save(updated);

        // then
        assertThat(repository.findById("inv-1", TENANT_A))
                .isPresent()
                .get()
                .extracting(Invoice::getCustomerName)
                .isEqualTo("Updated");
    }

    @Test
    void generateInvoiceNumber_producesUniqueIncrementingNumbers() {
        // when
        String first = repository.generateInvoiceNumber();
        String second = repository.generateInvoiceNumber();
        String third = repository.generateInvoiceNumber();

        // then
        assertThat(first).startsWith("INV-");
        assertThat(second).startsWith("INV-");
        assertThat(first).isNotEqualTo(second);
        assertThat(second).isNotEqualTo(third);
    }

    private Invoice invoiceFor(String id, String tenantId) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setTenantId(tenantId);
        return invoice;
    }
}
