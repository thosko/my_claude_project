package se.kleer.invoice.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Invoice {

    private String id;
    private String tenantId;
    private String invoiceNumber;
    private String customerId;
    private String customerName;
    private Instant issueDate;
    private Instant dueDate;
    private List<InvoiceItem> items;
    private long subtotalCents;
    private long taxCents;
    private long totalCents;
    private String currency;
    private InvoiceStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Invoice() {
        this.items = new ArrayList<>();
        this.status = InvoiceStatus.DRAFT;
    }

    /**
     * Recalculates the invoice totals based on items.
     */
    public void recalculateTotals() {
        this.subtotalCents = items.stream()
                .mapToLong(InvoiceItem::getTotalCents)
                .sum();
        this.totalCents = subtotalCents + taxCents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the tenant this invoice belongs to.
     *
     * @return the tenant identifier
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant this invoice belongs to.
     *
     * @param tenantId the tenant identifier
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Instant getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Instant issueDate) {
        this.issueDate = issueDate;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
        recalculateTotals();
    }

    public long getSubtotalCents() {
        return subtotalCents;
    }

    public long getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(long taxCents) {
        this.taxCents = taxCents;
        this.totalCents = subtotalCents + taxCents;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
