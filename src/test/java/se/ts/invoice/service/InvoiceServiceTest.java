package se.ts.invoice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.ts.invoice.domain.Invoice;
import se.ts.invoice.domain.InvoiceItem;
import se.ts.invoice.domain.InvoiceStatus;
import se.ts.invoice.repository.InvoiceRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(invoiceRepository);
        when(invoiceRepository.generateInvoiceNumber()).thenReturn("INV-1001");
        when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createInvoice_stampsAllProvidedFields() {
        // given
        Instant issueDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant dueDate = Instant.parse("2026-01-31T00:00:00Z");
        List<InvoiceItem> items = List.of(new InvoiceItem("i1", "Consulting", 2, 50000L));

        // when
        Invoice result = invoiceService.createInvoice(
                "tenant-abc", "cust-1", "Acme Corp",
                issueDate, dueDate, items, "SEK");

        // then
        assertThat(result.getTenantId()).isEqualTo("tenant-abc");
        assertThat(result.getCustomerId()).isEqualTo("cust-1");
        assertThat(result.getCustomerName()).isEqualTo("Acme Corp");
        assertThat(result.getIssueDate()).isEqualTo(issueDate);
        assertThat(result.getDueDate()).isEqualTo(dueDate);
        assertThat(result.getCurrency()).isEqualTo("SEK");
        assertThat(result.getItems()).isEqualTo(items);
    }

    @Test
    void createInvoice_assignsGeneratedInvoiceNumber() {
        // when
        Invoice result = createMinimalInvoice();

        // then
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-1001");
    }

    @Test
    void createInvoice_assignsNonNullUuidAsId() {
        // when
        Invoice result = createMinimalInvoice();

        // then
        assertThat(result.getId()).isNotNull().isNotBlank();
    }

    @Test
    void createInvoice_setsDraftStatus() {
        // when
        Invoice result = createMinimalInvoice();

        // then
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void createInvoice_setsCreatedAtAndUpdatedAt() {
        // when
        Invoice result = createMinimalInvoice();

        // then
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void createInvoice_savesInvoiceToRepository() {
        // given
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);

        // when
        createMinimalInvoice();

        // then
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-abc");
    }

    private Invoice createMinimalInvoice() {
        return invoiceService.createInvoice(
                "tenant-abc", "cust-1", "Acme Corp",
                Instant.now(), Instant.now().plusSeconds(2592000L),
                List.of(), "SEK");
    }
}
