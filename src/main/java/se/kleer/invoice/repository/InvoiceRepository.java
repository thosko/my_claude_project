package se.kleer.invoice.repository;

import se.kleer.invoice.domain.Invoice;

import java.util.Optional;

public interface InvoiceRepository {

    /**
     * Saves an invoice to the repository.
     *
     * @param invoice the invoice to save
     * @return the saved invoice
     */
    Invoice save(Invoice invoice);

    /**
     * Finds an invoice by its ID.
     *
     * @param id the invoice ID
     * @return an Optional containing the invoice if found
     */
    Optional<Invoice> findById(String id);

    /**
     * Generates the next invoice number.
     *
     * @return the next invoice number
     */
    String generateInvoiceNumber();
}
