package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjBooleanConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjBooleanConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("test", true);
        consumer.accept("value", false);

        assertEquals(2, result.size());
        assertEquals("test:true", result.get(0));
        assertEquals("value:false", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final boolean[] boolResult = new boolean[1];
        final String[] stringResult = new String[1];
        ObjBooleanConsumer<String> consumer = (t, value) -> {
            boolResult[0] = value;
            stringResult[0] = t.toUpperCase();
        };

        consumer.accept("hello", true);

        assertEquals(true, boolResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        ObjBooleanConsumer<String> consumer = new ObjBooleanConsumer<String>() {
            @Override
            public void accept(String t, boolean value) {
                result.add(value ? t : "");
            }
        };

        consumer.accept("test", true);
        consumer.accept("hidden", false);

        assertEquals(2, result.size());
        assertEquals("test", result.get(0));
        assertEquals("", result.get(1));
    }

    @Test
    public void testWithTrueValue() {
        final List<String> result = new ArrayList<>();
        ObjBooleanConsumer<String> consumer = (t, value) -> {
            if (value) {
                result.add(t);
            }
        };

        consumer.accept("included", true);
        consumer.accept("excluded", false);

        assertEquals(1, result.size());
        assertEquals("included", result.get(0));
    }

    @Test
    public void testWithFalseValue() {
        final List<String> result = new ArrayList<>();
        ObjBooleanConsumer<String> consumer = (t, value) -> {
            if (!value) {
                result.add(t);
            }
        };

        consumer.accept("excluded", true);
        consumer.accept("included", false);

        assertEquals(1, result.size());
        assertEquals("included", result.get(0));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        ObjBooleanConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept(null, true);

        assertEquals(1, result.size());
        assertEquals("null:true", result.get(0));
    }

    @Test
    public void testSideEffects() {
        final int[] counter = { 0 };
        ObjBooleanConsumer<String> consumer = (t, value) -> {
            if (value)
                counter[0]++;
        };

        consumer.accept("a", true);
        consumer.accept("b", false);
        consumer.accept("c", true);

        assertEquals(2, counter[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjBooleanConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
