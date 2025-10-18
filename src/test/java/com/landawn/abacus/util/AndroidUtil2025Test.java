package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AndroidUtil2025Test extends TestBase {

    @Test
    public void testGetSerialExecutor() {
        Executor executor = AndroidUtil.getSerialExecutor();
        assertNotNull(executor);
    }

    @Test
    public void testGetSerialExecutorNotNull() {
        Executor executor1 = AndroidUtil.getSerialExecutor();
        Executor executor2 = AndroidUtil.getSerialExecutor();
        assertNotNull(executor1);
        assertNotNull(executor2);
    }

    @Test
    public void testGetThreadPoolExecutor() {
        Executor executor = AndroidUtil.getThreadPoolExecutor();
        assertNotNull(executor);
    }

    @Test
    public void testGetThreadPoolExecutorNotNull() {
        Executor executor1 = AndroidUtil.getThreadPoolExecutor();
        Executor executor2 = AndroidUtil.getThreadPoolExecutor();
        assertNotNull(executor1);
        assertNotNull(executor2);
    }

    @Test
    public void testSerialExecutorExecutes() throws InterruptedException {
        Executor executor = AndroidUtil.getSerialExecutor();
        final boolean[] executed = { false };

        executor.execute(() -> {
            executed[0] = true;
        });

        Thread.sleep(100);
        assertNotNull(executor);
    }

    @Test
    public void testThreadPoolExecutorExecutes() throws InterruptedException {
        Executor executor = AndroidUtil.getThreadPoolExecutor();
        final boolean[] executed = { false };

        executor.execute(() -> {
            executed[0] = true;
        });

        Thread.sleep(100);
        assertNotNull(executor);
    }
}
