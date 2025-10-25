package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjByteConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjByteConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("test", (byte) 5);
        consumer.accept("value", (byte) 10);

        assertEquals(2, result.size());
        assertEquals("test:5", result.get(0));
        assertEquals("value:10", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final byte[] byteResult = new byte[1];
        final String[] stringResult = new String[1];
        ObjByteConsumer<String> consumer = (t, value) -> {
            byteResult[0] = (byte) (value * 2);
            stringResult[0] = t.toUpperCase();
        };

        consumer.accept("hello", (byte) 7);

        assertEquals((byte) 14, byteResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        ObjByteConsumer<String> consumer = new ObjByteConsumer<String>() {
            @Override
            public void accept(String t, byte value) {
                result.add(String.format("%s[%d]", t, value));
            }
        };

        consumer.accept("test", (byte) 42);

        assertEquals(1, result.size());
        assertEquals("test[42]", result.get(0));
    }

    @Test
    public void testWithNegativeValues() {
        final List<String> result = new ArrayList<>();
        ObjByteConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("negative", (byte) -5);
        consumer.accept("value", (byte) -127);

        assertEquals(2, result.size());
        assertEquals("negative:-5", result.get(0));
        assertEquals("value:-127", result.get(1));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<String> result = new ArrayList<>();
        ObjByteConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("max", Byte.MAX_VALUE);
        consumer.accept("min", Byte.MIN_VALUE);
        consumer.accept("zero", (byte) 0);

        assertEquals(3, result.size());
        assertEquals("max:127", result.get(0));
        assertEquals("min:-128", result.get(1));
        assertEquals("zero:0", result.get(2));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        ObjByteConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept(null, (byte) 5);

        assertEquals(1, result.size());
        assertEquals("null:5", result.get(0));
    }

    @Test
    public void testSideEffects() {
        final int[] sum = { 0 };
        ObjByteConsumer<String> consumer = (t, value) -> sum[0] += value;

        consumer.accept("a", (byte) 10);
        consumer.accept("b", (byte) 20);
        consumer.accept("c", (byte) 30);

        assertEquals(60, sum[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjByteConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
