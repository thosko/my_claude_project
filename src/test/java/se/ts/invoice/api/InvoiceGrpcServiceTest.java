package se.ts.invoice.api;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.ts.invoice.domain.Invoice;
import se.ts.invoice.domain.InvoiceItem;
import se.ts.invoice.domain.InvoiceStatus;
import se.ts.invoice.security.TenantContext;
import se.ts.invoice.service.InvoiceService;
import se.ts.invoice.v1.proto.CreateInvoiceRequest;
import se.ts.invoice.v1.proto.CreateInvoiceResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceGrpcServiceTest {

    @Mock
    private InvoiceService invoiceService;
    @Mock
    private StreamObserver<CreateInvoiceResponse> responseObserver;

    private InvoiceGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new InvoiceGrpcService(invoiceService);
        TenantContext.setTenantId("tenant-abc");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createInvoice_passesCorrectFieldsToService() {
        // given
        Instant issueDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant dueDate = Instant.parse("2026-01-31T00:00:00Z");

        CreateInvoiceRequest request = CreateInvoiceRequest.newBuilder()
                .setCustomerId("cust-1")
                .setCustomerName("Acme Corp")
                .setIssueDate(toTimestamp(issueDate))
                .setDueDate(toTimestamp(dueDate))
                .setCurrency("SEK")
                .build();

        when(invoiceService.createInvoice(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(stubInvoice());

        // when
        grpcService.createInvoice(request, responseObserver);

        // then
        ArgumentCaptor<String> tenantCaptor = ArgumentCaptor.forClass(String.class);
        verify(invoiceService).createInvoice(
                tenantCaptor.capture(),
                eq("cust-1"),
                eq("Acme Corp"),
                any(), any(),
                any(),
                eq("SEK"));
        assertThat(tenantCaptor.getValue()).isEqualTo("tenant-abc");
    }

    @Test
    void createInvoice_mapsSingleLineItem() {
        // given
        CreateInvoiceRequest request = CreateInvoiceRequest.newBuilder()
                .setCustomerId("cust-1")
                .setCustomerName("Acme Corp")
                .setIssueDate(toTimestamp(Instant.now()))
                .setDueDate(toTimestamp(Instant.now().plusSeconds(2592000L)))
                .setCurrency("SEK")
                .addItems(se.ts.invoice.v1.proto.InvoiceItem.newBuilder()
                        .setId("item-1")
                        .setDescription("Consulting")
                        .setQuantity(2)
                        .setUnitPriceCents(50000L)
                        .build())
                .build();

        when(invoiceService.createInvoice(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(stubInvoice());

        // when
        grpcService.createInvoice(request, responseObserver);

        // then
        ArgumentCaptor<List<InvoiceItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(invoiceService).createInvoice(
                any(), any(), any(), any(), any(),
                itemsCaptor.capture(),
                any());

        List<InvoiceItem> capturedItems = itemsCaptor.getValue();
        assertThat(capturedItems).hasSize(1);
        assertThat(capturedItems.get(0).getId()).isEqualTo("item-1");
        assertThat(capturedItems.get(0).getDescription()).isEqualTo("Consulting");
        assertThat(capturedItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(capturedItems.get(0).getUnitPriceCents()).isEqualTo(50000L);
    }

    @Test
    void createInvoice_itemWithEmptyId_assignsGeneratedId() {
        // given
        CreateInvoiceRequest request = CreateInvoiceRequest.newBuilder()
                .setCustomerId("cust-1")
                .setCustomerName("Acme Corp")
                .setIssueDate(toTimestamp(Instant.now()))
                .setDueDate(toTimestamp(Instant.now().plusSeconds(2592000L)))
                .setCurrency("SEK")
                .addItems(se.ts.invoice.v1.proto.InvoiceItem.newBuilder()
                        .setDescription("No ID item")
                        .setQuantity(1)
                        .setUnitPriceCents(10000L)
                        .build())
                .build();

        when(invoiceService.createInvoice(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(stubInvoice());

        // when
        grpcService.createInvoice(request, responseObserver);

        // then
        ArgumentCaptor<List<InvoiceItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(invoiceService).createInvoice(
                any(), any(), any(), any(), any(),
                itemsCaptor.capture(),
                any());
        assertThat(itemsCaptor.getValue().get(0).getId()).isNotBlank();
    }

    @Test
    void createInvoice_buildsResponseAndCompletesObserver() {
        // given
        CreateInvoiceRequest request = minimalRequest();
        when(invoiceService.createInvoice(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(stubInvoice());

        // when
        grpcService.createInvoice(request, responseObserver);

        // then
        ArgumentCaptor<CreateInvoiceResponse> responseCaptor =
                ArgumentCaptor.forClass(CreateInvoiceResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        CreateInvoiceResponse response = responseCaptor.getValue();
        assertThat(response.getInvoice().getId()).isEqualTo("invoice-1");
        assertThat(response.getInvoice().getCustomerName()).isEqualTo("Acme Corp");
    }

    @Test
    void createInvoice_mapsAllInvoiceStatusValues() {
        // given
        CreateInvoiceRequest request = minimalRequest();
        Invoice invoice = stubInvoice();
        invoice.setStatus(InvoiceStatus.PAID);
        when(invoiceService.createInvoice(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(invoice);

        // when
        grpcService.createInvoice(request, responseObserver);

        // then
        ArgumentCaptor<CreateInvoiceResponse> captor = ArgumentCaptor.forClass(CreateInvoiceResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertThat(captor.getValue().getInvoice().getStatus())
                .isEqualTo(se.ts.invoice.v1.proto.InvoiceStatus.INVOICE_STATUS_PAID);
    }

    private CreateInvoiceRequest minimalRequest() {
        return CreateInvoiceRequest.newBuilder()
                .setCustomerId("cust-1")
                .setCustomerName("Acme Corp")
                .setIssueDate(toTimestamp(Instant.now()))
                .setDueDate(toTimestamp(Instant.now().plusSeconds(2592000L)))
                .setCurrency("SEK")
                .build();
    }

    private Invoice stubInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId("invoice-1");
        invoice.setTenantId("tenant-abc");
        invoice.setInvoiceNumber("INV-1001");
        invoice.setCustomerId("cust-1");
        invoice.setCustomerName("Acme Corp");
        invoice.setIssueDate(Instant.now());
        invoice.setDueDate(Instant.now().plusSeconds(2592000L));
        invoice.setCurrency("SEK");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCreatedAt(Instant.now());
        invoice.setUpdatedAt(Instant.now());
        return invoice;
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
