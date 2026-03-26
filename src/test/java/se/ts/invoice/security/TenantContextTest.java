package se.ts.invoice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void getTenantId_beforeSet_returnsNull() {
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void setTenantId_andGet_returnsSameValue() {
        // when
        TenantContext.setTenantId("tenant-abc");

        // then
        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-abc");
    }

    @Test
    void clear_removesValue() {
        // given
        TenantContext.setTenantId("tenant-abc");

        // when
        TenantContext.clear();

        // then
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void setTenantId_overwritesPreviousValue() {
        // given
        TenantContext.setTenantId("tenant-first");

        // when
        TenantContext.setTenantId("tenant-second");

        // then
        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-second");
    }

    @Test
    void tenantContext_isIsolatedPerThread() throws InterruptedException {
        // given
        TenantContext.setTenantId("main-thread-tenant");
        String[] otherThreadValue = {null};

        // when — a second thread should see null, not the main thread's value
        Thread thread = new Thread(() -> otherThreadValue[0] = TenantContext.getTenantId());
        thread.start();
        thread.join();

        // then
        assertThat(otherThreadValue[0]).isNull();
        assertThat(TenantContext.getTenantId()).isEqualTo("main-thread-tenant");
    }
}
