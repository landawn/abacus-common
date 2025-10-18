package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ThreadMode2025Test extends TestBase {

    @Test
    public void testValueOf_withStringName() {
        assertEquals(ThreadMode.DEFAULT, ThreadMode.valueOf("DEFAULT"));
        assertEquals(ThreadMode.THREAD_POOL_EXECUTOR, ThreadMode.valueOf("THREAD_POOL_EXECUTOR"));
    }

    @Test
    public void testValues() {
        ThreadMode[] values = ThreadMode.values();
        assertEquals(2, values.length);
        assertEquals(ThreadMode.DEFAULT, values[0]);
        assertEquals(ThreadMode.THREAD_POOL_EXECUTOR, values[1]);
    }

    @Test
    public void testEnumName() {
        assertEquals("DEFAULT", ThreadMode.DEFAULT.name());
        assertEquals("THREAD_POOL_EXECUTOR", ThreadMode.THREAD_POOL_EXECUTOR.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("DEFAULT", ThreadMode.DEFAULT.toString());
        assertEquals("THREAD_POOL_EXECUTOR", ThreadMode.THREAD_POOL_EXECUTOR.toString());
    }

    @Test
    public void testOrdinal() {
        assertEquals(0, ThreadMode.DEFAULT.ordinal());
        assertEquals(1, ThreadMode.THREAD_POOL_EXECUTOR.ordinal());
    }
}
