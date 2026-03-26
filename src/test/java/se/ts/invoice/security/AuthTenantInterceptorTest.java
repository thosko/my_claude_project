package se.ts.invoice.security;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import se.ts.invoice.domain.security.Role;
import se.ts.invoice.domain.security.TokenClaims;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTenantInterceptorTest {

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Mock
    private TokenValidator tokenValidator;
    @Mock
    private PermissionResolver permissionResolver;
    @Mock
    private ServerCall<Object, Object> serverCall;
    @Mock
    private ServerCallHandler<Object, Object> serverCallHandler;

    private AuthTenantInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthTenantInterceptor(tokenValidator, permissionResolver);
        when(permissionResolver.resolveAuthorities(any())).thenReturn(Set.of());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void interceptCall_missingAuthorizationHeader_closesCallWithUnauthenticated() {
        // given
        Metadata headers = new Metadata();

        // when
        interceptor.interceptCall(serverCall, headers, serverCallHandler);

        // then
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(serverCallHandler, never()).startCall(any(), any());
    }

    @Test
    void interceptCall_headerWithoutBearerPrefix_closesCallWithUnauthenticated() {
        // given
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION_KEY, "Basic dXNlcjpwYXNz");

        // when
        interceptor.interceptCall(serverCall, headers, serverCallHandler);

        // then
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    void interceptCall_invalidToken_closesCallWithUnauthenticated() {
        // given
        Metadata headers = headersWithToken("bad-token");
        when(tokenValidator.validate("bad-token"))
                .thenThrow(new BadCredentialsException("bad token"));

        // when
        interceptor.interceptCall(serverCall, headers, serverCallHandler);

        // then
        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(serverCall).close(statusCaptor.capture(), any());
        assertThat(statusCaptor.getValue().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(statusCaptor.getValue().getDescription()).isEqualTo("bad token");
    }

    @Test
    void interceptCall_validToken_callsTokenValidatorWithRawToken() {
        // given
        Metadata headers = headersWithToken("tenant-abc|user-1|ADMIN");
        TokenClaims claims = new TokenClaims("user-1", "tenant-abc", Set.of(Role.ADMIN));
        when(tokenValidator.validate("tenant-abc|user-1|ADMIN")).thenReturn(claims);
        when(serverCallHandler.startCall(any(), any())).thenReturn(new ServerCall.Listener<>() { });

        // when
        interceptor.interceptCall(serverCall, headers, serverCallHandler);

        // then
        verify(tokenValidator).validate("tenant-abc|user-1|ADMIN");
        verify(serverCall, never()).close(any(), any());
    }

    @Test
    void interceptCall_validToken_doesNotCloseCall() {
        // given
        Metadata headers = headersWithToken("tenant-abc|user-1|ADMIN");
        TokenClaims claims = new TokenClaims("user-1", "tenant-abc", Set.of(Role.ADMIN));
        when(tokenValidator.validate(any())).thenReturn(claims);
        when(serverCallHandler.startCall(any(), any())).thenReturn(new ServerCall.Listener<>() { });

        // when
        interceptor.interceptCall(serverCall, headers, serverCallHandler);

        // then
        verify(serverCall, never()).close(any(), any());
    }

    @Test
    void interceptCall_validToken_setsSecurityContextOnHalfClose() {
        // given
        Metadata headers = headersWithToken("tenant-abc|user-1|ADMIN");
        TokenClaims claims = new TokenClaims("user-1", "tenant-abc", Set.of(Role.ADMIN));
        when(tokenValidator.validate(any())).thenReturn(claims);

        String[] capturedTenantId = {null};
        when(serverCallHandler.startCall(any(), any())).thenAnswer(inv -> {
            // verify context is set when the downstream handler is invoked
            capturedTenantId[0] = TenantContext.getTenantId();
            return new ServerCall.Listener<>() { };
        });

        // when
        ServerCall.Listener<Object> listener =
                interceptor.interceptCall(serverCall, headers, serverCallHandler);
        listener.onHalfClose();

        // then
        assertThat(capturedTenantId[0]).isEqualTo("tenant-abc");
    }

    @Test
    void tenantAwareListener_clearsTenantContextAfterHalfClose() {
        // given
        Metadata headers = headersWithToken("tenant-abc|user-1|ADMIN");
        TokenClaims claims = new TokenClaims("user-1", "tenant-abc", Set.of(Role.ADMIN));
        when(tokenValidator.validate(any())).thenReturn(claims);
        when(serverCallHandler.startCall(any(), any())).thenReturn(new ServerCall.Listener<>() { });

        // when
        ServerCall.Listener<Object> listener =
                interceptor.interceptCall(serverCall, headers, serverCallHandler);
        listener.onHalfClose();

        // then — context must be cleared after the callback completes
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private Metadata headersWithToken(String token) {
        Metadata headers = new Metadata();
        headers.put(AUTHORIZATION_KEY, "Bearer " + token);
        return headers;
    }
}
