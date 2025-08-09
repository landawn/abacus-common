package com.landawn.abacus.util;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AndroidUtil100Test extends TestBase {

    @Test
    public void testGetSerialExecutor() {
        Executor executor = AndroidUtil.getSerialExecutor();
        Assertions.assertNotNull(executor);
    }

    @Test
    public void testGetThreadPoolExecutor() {
        Executor executor = AndroidUtil.getThreadPoolExecutor();
        Assertions.assertNotNull(executor);
    }

    @Test
    public void testExecutorsAreDifferent() {
        Executor serial = AndroidUtil.getSerialExecutor();
        Executor threadPool = AndroidUtil.getThreadPoolExecutor();
        // In non-Android environment, these should be different executors
        Assertions.assertNotSame(serial, threadPool);
    }
}