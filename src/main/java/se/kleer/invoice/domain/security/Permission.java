package se.kleer.invoice.domain.security;

/**
 * Defines the fine-grained permissions available in the system.
 *
 * <p>Authorization checks always use permissions, never roles directly.
 * This means adding a new role never requires touching service method guards —
 * only the role's permission set in {@link Role} needs updating.</p>
 */
public enum Permission {

    /** Allows reading/viewing an invoice. */
    INVOICE_READ,

    /** Allows creating a new invoice. */
    INVOICE_CREATE,

    /** Allows updating an existing invoice. */
    INVOICE_UPDATE,

    /** Allows deleting an invoice. */
    INVOICE_DELETE
}
