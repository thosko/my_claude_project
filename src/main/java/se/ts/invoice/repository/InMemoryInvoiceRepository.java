package se.ts.invoice.repository;

import org.springframework.stereotype.Repository;
import se.ts.invoice.domain.Invoice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link InvoiceRepository}.
 * All reads are tenant-scoped — a lookup for an invoice that belongs to a different
 * tenant returns empty, providing the same isolation guarantee a real database would.
 */
@Repository
public class InMemoryInvoiceRepository implements InvoiceRepository {

    private static final String INVOICE_NUMBER_PREFIX = "INV-";

    private final Map<String, Invoice> invoices = new ConcurrentHashMap<>();
    private final AtomicLong invoiceCounter = new AtomicLong(1000);

    @Override
    public Invoice save(Invoice invoice) {
        invoices.put(invoice.getId(), invoice);
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(String id, String tenantId) {
        return Optional.ofNullable(invoices.get(id))
                .filter(invoice -> tenantId.equals(invoice.getTenantId()));
    }

    @Override
    public List<Invoice> findAllByTenantId(String tenantId) {
        return invoices.values().stream()
                .filter(invoice -> tenantId.equals(invoice.getTenantId()))
                .collect(Collectors.toList());
    }

    @Override
    public String generateInvoiceNumber() {
        return INVOICE_NUMBER_PREFIX + invoiceCounter.incrementAndGet();
    }
}
