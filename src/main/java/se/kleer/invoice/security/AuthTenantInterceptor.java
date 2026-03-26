package se.kleer.invoice.security;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import se.kleer.invoice.domain.security.TokenClaims;

/**
 * gRPC server interceptor that authenticates each request and establishes the tenant context.
 *
 * <p>Runs before every gRPC service method. Expects an {@code authorization} metadata header
 * with value {@code Bearer <token>}. On success:</p>
 * <ol>
 *   <li>Populates the Spring {@link org.springframework.security.core.context.SecurityContext}
 *       with the authenticated principal and resolved permissions.</li>
 *   <li>Sets {@link TenantContext} with the tenant ID from the token claims.</li>
 * </ol>
 *
 * <p>Both contexts are cleared after each request via the inner {@link TenantAwareListener}
 * to prevent thread-local leaks in the gRPC thread pool.</p>
 */
@GrpcGlobalServerInterceptor
public class AuthTenantInterceptor implements ServerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(AuthTenantInterceptor.class);
    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Context.Key<String> TENANT_ID_CTX_KEY = Context.key("tenantId");

    private final TokenValidator tokenValidator;
    private final PermissionResolver permissionResolver;

    /**
     * Constructs the interceptor with required dependencies.
     *
     * @param tokenValidator     validates the bearer token and extracts claims
     * @param permissionResolver expands role-based permissions to Spring authorities
     */
    public AuthTenantInterceptor(TokenValidator tokenValidator, PermissionResolver permissionResolver) {
        this.tokenValidator = tokenValidator;
        this.permissionResolver = permissionResolver;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String authHeader = headers.get(AUTHORIZATION_KEY);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            call.close(
                    Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"),
                    new Metadata());
            return new ServerCall.Listener<>() { };
        }

        String rawToken = authHeader.substring(BEARER_PREFIX.length());
        TokenClaims claims;
        try {
            claims = tokenValidator.validate(rawToken);
        } catch (AuthenticationException e) {
            LOG.warn("Token validation failed: {}", e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription(e.getMessage()), new Metadata());
            return new ServerCall.Listener<>() { };
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                claims.subject(),
                null,
                permissionResolver.resolveAuthorities(claims.roles())
        );

        Context grpcContext = Context.current().withValue(TENANT_ID_CTX_KEY, claims.tenantId());
        ServerCall.Listener<ReqT> delegate = Contexts.interceptCall(grpcContext, call, headers, next);
        return new TenantAwareListener<>(delegate, claims.tenantId(), authentication);
    }

    /**
     * Wraps a listener to set and clear security and tenant context around each gRPC
     * callback, ensuring thread-local state is always cleaned up even on cancellation.
     */
    private static final class TenantAwareListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final String tenantId;
        private final UsernamePasswordAuthenticationToken authentication;

        private TenantAwareListener(
                ServerCall.Listener<ReqT> delegate,
                String tenantId,
                UsernamePasswordAuthenticationToken authentication) {
            super(delegate);
            this.tenantId = tenantId;
            this.authentication = authentication;
        }

        @Override
        public void onMessage(ReqT message) {
            runWithContext(() -> super.onMessage(message));
        }

        @Override
        public void onHalfClose() {
            runWithContext(super::onHalfClose);
        }

        @Override
        public void onComplete() {
            runWithContext(super::onComplete);
        }

        @Override
        public void onCancel() {
            runWithContext(super::onCancel);
        }

        private void runWithContext(Runnable action) {
            TenantContext.setTenantId(tenantId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                action.run();
            } finally {
                TenantContext.clear();
                SecurityContextHolder.clearContext();
            }
        }
    }
}
