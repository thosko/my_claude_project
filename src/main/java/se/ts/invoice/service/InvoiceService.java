package se.ts.invoice.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import se.ts.invoice.domain.Invoice;
import se.ts.invoice.domain.InvoiceItem;
import se.ts.invoice.domain.InvoiceStatus;
import se.ts.invoice.repository.InvoiceRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for invoice management.
 * All methods require the caller to hold the appropriate permission,
 * enforced via {@code @PreAuthorize} checks against the Spring Security context
 * populated by the gRPC authentication interceptor.
 */
@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Constructs the service with its required repository dependency.
     *
     * @param invoiceRepository the invoice repository
     */
    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Creates a new invoice stamped with the given tenant ID.
     *
     * @param tenantId     the tenant this invoice belongs to
     * @param customerId   the customer ID
     * @param customerName the customer name
     * @param issueDate    the issue date
     * @param dueDate      the due date
     * @param items        the invoice line items
     * @param currency     the currency code
     * @return the created invoice
     */
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public Invoice createInvoice(String tenantId,
                                  String customerId,
                                  String customerName,
                                  Instant issueDate,
                                  Instant dueDate,
                                  List<InvoiceItem> items,
                                  String currency) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID().toString());
        invoice.setTenantId(tenantId);
        invoice.setInvoiceNumber(invoiceRepository.generateInvoiceNumber());
        invoice.setCustomerId(customerId);
        invoice.setCustomerName(customerName);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(dueDate);
        invoice.setItems(items);
        invoice.setCurrency(currency);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCreatedAt(Instant.now());
        invoice.setUpdatedAt(Instant.now());

        return invoiceRepository.save(invoice);
    }
}
