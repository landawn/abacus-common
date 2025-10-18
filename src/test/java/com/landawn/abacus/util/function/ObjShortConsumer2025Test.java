package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjShortConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjShortConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("test", (short) 5);
        consumer.accept("value", (short) 10);

        assertEquals(2, result.size());
        assertEquals("test:5", result.get(0));
        assertEquals("value:10", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final short[] shortResult = new short[1];
        final String[] stringResult = new String[1];
        ObjShortConsumer<String> consumer = (t, value) -> {
            shortResult[0] = (short) (value * 2);
            stringResult[0] = t.toUpperCase();
        };

        consumer.accept("hello", (short) 7);

        assertEquals((short) 14, shortResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        ObjShortConsumer<String> consumer = new ObjShortConsumer<String>() {
            @Override
            public void accept(String t, short value) {
                result.add(String.format("%s[%d]", t, value));
            }
        };

        consumer.accept("test", (short) 42);

        assertEquals(1, result.size());
        assertEquals("test[42]", result.get(0));
    }

    @Test
    public void testWithNegativeValues() {
        final List<String> result = new ArrayList<>();
        ObjShortConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("negative", (short) -5);
        consumer.accept("value", (short) -100);

        assertEquals(2, result.size());
        assertEquals("negative:-5", result.get(0));
        assertEquals("value:-100", result.get(1));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<String> result = new ArrayList<>();
        ObjShortConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("max", Short.MAX_VALUE);
        consumer.accept("min", Short.MIN_VALUE);
        consumer.accept("zero", (short) 0);

        assertEquals(3, result.size());
        assertEquals("max:32767", result.get(0));
        assertEquals("min:-32768", result.get(1));
        assertEquals("zero:0", result.get(2));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        ObjShortConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept(null, (short) 5);

        assertEquals(1, result.size());
        assertEquals("null:5", result.get(0));
    }

    @Test
    public void testSideEffects() {
        final int[] sum = {0};
        ObjShortConsumer<String> consumer = (t, value) -> sum[0] += value;

        consumer.accept("a", (short) 10);
        consumer.accept("b", (short) 20);
        consumer.accept("c", (short) 30);

        assertEquals(60, sum[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjShortConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
