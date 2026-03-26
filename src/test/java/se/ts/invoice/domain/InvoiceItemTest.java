package se.ts.invoice.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceItemTest {

    @Test
    void constructor_calculatesTotalCents() {
        // given
        int quantity = 3;
        long unitPriceCents = 5000L;

        // when
        InvoiceItem item = new InvoiceItem("item-1", "Widget", quantity, unitPriceCents);

        // then
        assertThat(item.getTotalCents()).isEqualTo(15000L);
    }

    @Test
    void setQuantity_recalculatesTotalCents() {
        // given
        InvoiceItem item = new InvoiceItem("item-1", "Widget", 2, 5000L);

        // when
        item.setQuantity(4);

        // then
        assertThat(item.getTotalCents()).isEqualTo(20000L);
    }

    @Test
    void setUnitPriceCents_recalculatesTotalCents() {
        // given
        InvoiceItem item = new InvoiceItem("item-1", "Widget", 3, 1000L);

        // when
        item.setUnitPriceCents(2000L);

        // then
        assertThat(item.getTotalCents()).isEqualTo(6000L);
    }

    @Test
    void constructor_zeroQuantity_producesZeroTotal() {
        // given / when
        InvoiceItem item = new InvoiceItem("item-1", "Widget", 0, 5000L);

        // then
        assertThat(item.getTotalCents()).isZero();
    }
}
