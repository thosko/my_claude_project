package se.kleer.invoice.service;

import org.springframework.stereotype.Service;
import se.kleer.invoice.domain.Invoice;
import se.kleer.invoice.domain.InvoiceItem;
import se.kleer.invoice.domain.InvoiceStatus;
import se.kleer.invoice.repository.InvoiceRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Creates a new invoice.
     *
     * @param customerId   the customer ID
     * @param customerName the customer name
     * @param issueDate    the issue date
     * @param dueDate      the due date
     * @param items        the invoice line items
     * @param currency     the currency code
     * @return the created invoice
     */
    public Invoice createInvoice(String customerId,
                                  String customerName,
                                  Instant issueDate,
                                  Instant dueDate,
                                  List<InvoiceItem> items,
                                  String currency) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID().toString());
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
