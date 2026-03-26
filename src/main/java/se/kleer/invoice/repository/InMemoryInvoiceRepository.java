package se.kleer.invoice.repository;

import org.springframework.stereotype.Repository;
import se.kleer.invoice.domain.Invoice;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    public Optional<Invoice> findById(String id) {
        return Optional.ofNullable(invoices.get(id));
    }

    @Override
    public String generateInvoiceNumber() {
        return INVOICE_NUMBER_PREFIX + invoiceCounter.incrementAndGet();
    }
}
