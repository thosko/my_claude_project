package se.kleer.invoice.api;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import se.kleer.invoice.domain.Invoice;
import se.kleer.invoice.domain.InvoiceItem;
import se.kleer.invoice.security.TenantContext;
import se.kleer.invoice.service.InvoiceService;
import se.kleer.invoice.v1.proto.CreateInvoiceRequest;
import se.kleer.invoice.v1.proto.CreateInvoiceResponse;
import se.kleer.invoice.v1.proto.InvoiceServiceGrpc;
import se.kleer.invoice.v1.proto.InvoiceStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@GrpcService
public class InvoiceGrpcService extends InvoiceServiceGrpc.InvoiceServiceImplBase {

    private final InvoiceService invoiceService;

    public InvoiceGrpcService(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    public void createInvoice(CreateInvoiceRequest request,
                              StreamObserver<CreateInvoiceResponse> responseObserver) {
        List<InvoiceItem> items = request.getItemsList().stream()
                .map(this::toDomainItem)
                .toList();

        Invoice invoice = invoiceService.createInvoice(
                TenantContext.getTenantId(),
                request.getCustomerId(),
                request.getCustomerName(),
                toInstant(request.getIssueDate()),
                toInstant(request.getDueDate()),
                items,
                request.getCurrency()
        );

        CreateInvoiceResponse response = CreateInvoiceResponse.newBuilder()
                .setInvoice(toProtoInvoice(invoice))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private InvoiceItem toDomainItem(se.kleer.invoice.v1.proto.InvoiceItem protoItem) {
        return new InvoiceItem(
                protoItem.getId().isEmpty() ? UUID.randomUUID().toString() : protoItem.getId(),
                protoItem.getDescription(),
                protoItem.getQuantity(),
                protoItem.getUnitPriceCents()
        );
    }

    private se.kleer.invoice.v1.proto.Invoice toProtoInvoice(Invoice invoice) {
        se.kleer.invoice.v1.proto.Invoice.Builder builder = se.kleer.invoice.v1.proto.Invoice.newBuilder()
                .setId(invoice.getId())
                .setInvoiceNumber(invoice.getInvoiceNumber())
                .setCustomerId(invoice.getCustomerId())
                .setCustomerName(invoice.getCustomerName())
                .setIssueDate(toTimestamp(invoice.getIssueDate()))
                .setDueDate(toTimestamp(invoice.getDueDate()))
                .setSubtotalCents(invoice.getSubtotalCents())
                .setTaxCents(invoice.getTaxCents())
                .setTotalCents(invoice.getTotalCents())
                .setCurrency(invoice.getCurrency())
                .setStatus(toProtoStatus(invoice.getStatus()))
                .setCreatedAt(toTimestamp(invoice.getCreatedAt()))
                .setUpdatedAt(toTimestamp(invoice.getUpdatedAt()));

        for (InvoiceItem item : invoice.getItems()) {
            builder.addItems(toProtoItem(item));
        }

        return builder.build();
    }

    private se.kleer.invoice.v1.proto.InvoiceItem toProtoItem(InvoiceItem item) {
        return se.kleer.invoice.v1.proto.InvoiceItem.newBuilder()
                .setId(item.getId())
                .setDescription(item.getDescription())
                .setQuantity(item.getQuantity())
                .setUnitPriceCents(item.getUnitPriceCents())
                .setTotalCents(item.getTotalCents())
                .build();
    }

    private InvoiceStatus toProtoStatus(se.kleer.invoice.domain.InvoiceStatus status) {
        return switch (status) {
            case DRAFT -> InvoiceStatus.INVOICE_STATUS_DRAFT;
            case SENT -> InvoiceStatus.INVOICE_STATUS_SENT;
            case PAID -> InvoiceStatus.INVOICE_STATUS_PAID;
            case OVERDUE -> InvoiceStatus.INVOICE_STATUS_OVERDUE;
            case CANCELLED -> InvoiceStatus.INVOICE_STATUS_CANCELLED;
        };
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
