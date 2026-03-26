package se.kleer.invoice.security;

/**
 * Thread-local holder for the current tenant identifier.
 *
 * <p>The tenant ID is set by {@link AuthTenantInterceptor} at the start of each
 * gRPC request and cleared when the request completes. Always use the
 * {@link #clear()} method in a {@code finally} block to prevent thread-local leaks.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Sets the tenant ID for the current thread.
     *
     * @param tenantId the tenant identifier
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    /**
     * Returns the tenant ID for the current thread.
     *
     * @return the tenant identifier, or {@code null} if not set
     */
    public static String getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    /**
     * Clears the tenant ID from the current thread.
     * Must be called at the end of every request to prevent memory leaks in thread pools.
     */
    public static void clear() {
        TENANT_ID_HOLDER.remove();
    }
}
