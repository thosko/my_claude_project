package se.kleer.invoice.repository;

import se.kleer.invoice.domain.Invoice;

import java.util.List;
import java.util.Optional;

/**
 * Repository contract for invoice persistence.
 * All read operations are tenant-scoped to enforce data isolation between tenants.
 */
public interface InvoiceRepository {

    /**
     * Saves an invoice to the repository.
     * The invoice must have its {@code tenantId} set before saving.
     *
     * @param invoice the invoice to save
     * @return the saved invoice
     */
    Invoice save(Invoice invoice);

    /**
     * Finds an invoice by its ID within a specific tenant.
     * Returns empty if the invoice exists but belongs to a different tenant.
     *
     * @param id       the invoice ID
     * @param tenantId the tenant the caller belongs to
     * @return an Optional containing the invoice if found within the tenant
     */
    Optional<Invoice> findById(String id, String tenantId);

    /**
     * Returns all invoices belonging to the given tenant.
     *
     * @param tenantId the tenant identifier
     * @return list of invoices for the tenant, never null
     */
    List<Invoice> findAllByTenantId(String tenantId);

    /**
     * Generates the next invoice number.
     *
     * @return the next invoice number
     */
    String generateInvoiceNumber();
}
