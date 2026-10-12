package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class UtilANNullArgumentReview20260913Test extends TestBase {
    @Test
    void everyRequiredArrayRangeSourceIsRejectedBeforeEmptyReturn() throws Exception {
        int tested = 0;
        for (final Method method : CommonUtil.class.getDeclaredMethods()) {
            if (!method.getName().equals("copyOfRange") || !method.getParameterTypes()[0].isArray()) {
                continue;
            }
            final Object[] args = new Object[method.getParameterCount()];
            args[0] = null;
            for (int i = 1; i < args.length; i++) {
                args[i] = method.getParameterTypes()[i] == Class.class ? String[].class : Integer.valueOf(i == 3 ? 2 : 0);
            }
            final InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> method.invoke(null, args));
            assertInstanceOf(IllegalArgumentException.class, exception.getCause(), method.toString());
            assertTrue(exception.getCause().getMessage().contains("original"));
            tested++;
        }
        assertEquals(20, tested);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((List<String>) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((List<String>) null, 0, 0, 2));
    }

    @Test
    void copyTypeValidationPreservesRangesAndNullDefaultCopies() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOf((String[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOf(new Object[0], 0, null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(new Object[0], 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(new Object[0], 0, 0, 2, null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new Object[0], -1, 0, null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(new Object[0], 0, 0, 0, String[].class));
        assertArrayEquals(new int[2], CommonUtil.copyOf((int[]) null, 2));
        assertArrayEquals(new String[2], CommonUtil.copyOf(null, 2, String[].class));
        assertEquals("", CommonUtil.copyOfRange((String) null, 0, 0, 2));
        assertArrayEquals(new String[] { "c", "a" }, CommonUtil.copyOfRange(new String[] { "a", "b", "c" }, 2, -1, -2));
    }

    @Test
    void swapValidatesRequiredContainersButRetainsConditionalNullNoOp() throws Exception {
        int tested = 0;
        for (final Method method : CommonUtil.class.getDeclaredMethods()) {
            if (method.getName().equals("swap") && method.getParameterCount() == 3 && method.getParameterTypes()[0].isArray()) {
                final InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> method.invoke(null, null, 0, 0));
                assertInstanceOf(IllegalArgumentException.class, exception.getCause(), method.toString());
                tested++;
            }
        }
        assertEquals(9, tested);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.swap((List<?>) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.swap((Pair<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.swap((Triple<String, String, String>) null));
        assertFalse(CommonUtil.swapIf((Pair<String, String>) null, pair -> false));
        assertFalse(CommonUtil.swapIf((Triple<String, String, String>) null, triple -> false));
        assertThrows(NullPointerException.class, () -> CommonUtil.swapIf((Pair<String, String>) null, pair -> true));
        final List<Integer> list = Arrays.asList(1, 2);
        CommonUtil.swap(list, 0, 1);
        assertEquals(Arrays.asList(2, 1), list);
    }

    @Test
    void meanRequiresValuesWhileAverageKeepsNullAsZero() {
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean((long[]) null));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean((double[]) null));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(new int[0]));
        assertThrows(IllegalArgumentException.class, () -> Numbers.mean(1d, Double.NaN));
        assertEquals(2d, Numbers.mean(1, 2, 3));
        assertEquals(0d, N.average((int[]) null));
        assertEquals(0d, N.average((long[]) null));
        assertEquals(0d, N.average((double[]) null));
    }

    @Test
    void proxyValidatesInterfaceContainerAndRetainsNullElementContract() {
        final AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newProxyInstance((Class<Runnable>) null, (proxy, method, args) -> null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newProxyInstance((Class<?>[]) null, (proxy, method, args) -> null));
        assertThrows(NullPointerException.class, () -> CommonUtil.newProxyInstance(new Class<?>[] { null }, (proxy, method, args) -> null));
        final Runnable proxy = CommonUtil.newProxyInstance(Runnable.class, (instance, method, args) -> {
            calls.incrementAndGet();
            return null;
        });
        proxy.run();
        assertEquals(1, calls.get());
    }
}
