package se.ts.invoice.domain;

public class InvoiceItem {

    private String id;
    private String description;
    private int quantity;
    private long unitPriceCents;
    private long totalCents;

    public InvoiceItem() {
    }

    public InvoiceItem(String id, String description, int quantity, long unitPriceCents) {
        this.id = id;
        this.description = description;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.totalCents = quantity * unitPriceCents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.totalCents = quantity * unitPriceCents;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
        this.totalCents = quantity * unitPriceCents;
    }

    public long getTotalCents() {
        return totalCents;
    }
}
