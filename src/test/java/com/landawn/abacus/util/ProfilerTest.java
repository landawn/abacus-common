/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class ProfilerTest extends AbstractTest {
    static {
        Profiler.suspend(true);
    }

    @Test
    public void test_normal() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.run(this, method, null, 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).writeHtmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeXmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeResult(System.out);
        assertEquals(String.class, method.getReturnType());

        Profiler.run(this, "normal", 2, 2, 1).printResult();
        Profiler.run(this, "normal", 2, 2, 1).printResult();
    }

    @Test
    public void test_error() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        Profiler.run(this, method, N.asList("a", "b"), 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).writeHtmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeXmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeResult(System.out);
    }

    String normal() throws Exception {
        Thread.sleep(10);

        return null;
    }

    Object error(Object obj) {
        throw new RuntimeException(N.toString(obj));
    }
}
