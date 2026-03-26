package se.ts.invoice.domain.security;

/**
 * Defines the fine-grained permissions available in the system.
 *
 * <p>Authorization checks always use permissions, never roles directly.
 * This means adding a new role never requires touching service method guards —
 * only the role's permission set in {@link Role} needs updating.</p>
 */
public enum Permission {

    /** Allows reading own tenant's invoices (EMPLOYEE level — restricted scope). */
    INVOICE_READ,

    /** Allows reading all invoices within the tenant (MANAGER level — full scope). */
    INVOICE_READ_ALL,

    /** Allows creating a new invoice. */
    INVOICE_CREATE,

    /** Allows updating an existing invoice. */
    INVOICE_UPDATE,

    /** Allows deleting an invoice. */
    INVOICE_DELETE
}
