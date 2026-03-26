package se.ts.invoice.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceTest {

    @Test
    void newInvoice_defaultStatusIsDraft() {
        // when
        Invoice invoice = new Invoice();

        // then
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    void newInvoice_itemsListIsNotNull() {
        // when
        Invoice invoice = new Invoice();

        // then
        assertThat(invoice.getItems()).isNotNull().isEmpty();
    }

    @Test
    void setItems_triggersSubtotalRecalculation() {
        // given
        Invoice invoice = new Invoice();
        InvoiceItem item1 = new InvoiceItem("i1", "Widget", 2, 10000L);
        InvoiceItem item2 = new InvoiceItem("i2", "Gadget", 1, 5000L);

        // when
        invoice.setItems(List.of(item1, item2));

        // then
        assertThat(invoice.getSubtotalCents()).isEqualTo(25000L);
    }

    @Test
    void setItems_withTaxAlreadySet_updatesTotalCorrectly() {
        // given
        Invoice invoice = new Invoice();
        invoice.setTaxCents(2500L);
        InvoiceItem item = new InvoiceItem("i1", "Widget", 1, 10000L);

        // when
        invoice.setItems(List.of(item));

        // then
        assertThat(invoice.getSubtotalCents()).isEqualTo(10000L);
        assertThat(invoice.getTotalCents()).isEqualTo(12500L);
    }

    @Test
    void setTaxCents_updatesTotalWithoutChangingSubtotal() {
        // given
        Invoice invoice = new Invoice();
        invoice.setItems(List.of(new InvoiceItem("i1", "Widget", 1, 10000L)));

        // when
        invoice.setTaxCents(2500L);

        // then
        assertThat(invoice.getSubtotalCents()).isEqualTo(10000L);
        assertThat(invoice.getTaxCents()).isEqualTo(2500L);
        assertThat(invoice.getTotalCents()).isEqualTo(12500L);
    }

    @Test
    void recalculateTotals_emptyItems_producesZeroSubtotal() {
        // given
        Invoice invoice = new Invoice();
        invoice.setTaxCents(1000L);

        // when
        invoice.recalculateTotals();

        // then
        assertThat(invoice.getSubtotalCents()).isZero();
        assertThat(invoice.getTotalCents()).isEqualTo(1000L);
    }

    @Test
    void recalculateTotals_multipleItems_sumsAllTotals() {
        // given
        Invoice invoice = new Invoice();
        invoice.setItems(List.of(
                new InvoiceItem("i1", "A", 2, 5000L),
                new InvoiceItem("i2", "B", 3, 2000L),
                new InvoiceItem("i3", "C", 1, 1000L)
        ));

        // when
        invoice.recalculateTotals();

        // then — (2×5000) + (3×2000) + (1×1000) = 10000 + 6000 + 1000 = 17000
        assertThat(invoice.getSubtotalCents()).isEqualTo(17000L);
    }
}
