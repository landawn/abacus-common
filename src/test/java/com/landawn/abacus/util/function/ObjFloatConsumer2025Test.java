package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjFloatConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjFloatConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("test", 5.5f);
        consumer.accept("value", 10.25f);

        assertEquals(2, result.size());
        assertEquals("test:5.5", result.get(0));
        assertEquals("value:10.25", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final float[] floatResult = new float[1];
        final String[] stringResult = new String[1];
        ObjFloatConsumer<String> consumer = (t, value) -> {
            floatResult[0] = value * 2;
            stringResult[0] = t.toUpperCase();
        };

        consumer.accept("hello", 7.5f);

        assertEquals(15.0f, floatResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        ObjFloatConsumer<String> consumer = new ObjFloatConsumer<String>() {
            @Override
            public void accept(String t, float value) {
                result.add(String.format("%s[%.2f]", t, value));
            }
        };

        consumer.accept("test", 3.14159f);

        assertEquals(1, result.size());
        assertEquals("test[3.14]", result.get(0));
    }

    @Test
    public void testWithNegativeValues() {
        final List<String> result = new ArrayList<>();
        ObjFloatConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("negative", -5.5f);
        consumer.accept("value", -100.75f);

        assertEquals(2, result.size());
        assertEquals("negative:-5.5", result.get(0));
        assertEquals("value:-100.75", result.get(1));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<String> result = new ArrayList<>();
        ObjFloatConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("max", Float.MAX_VALUE);
        consumer.accept("min", Float.MIN_VALUE);
        consumer.accept("zero", 0.0f);

        assertEquals(3, result.size());
        assertEquals("max:" + Float.MAX_VALUE, result.get(0));
        assertEquals("min:" + Float.MIN_VALUE, result.get(1));
        assertEquals("zero:0.0", result.get(2));
    }

    @Test
    public void testWithSpecialValues() {
        final List<String> result = new ArrayList<>();
        ObjFloatConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("nan", Float.NaN);
        consumer.accept("posinf", Float.POSITIVE_INFINITY);
        consumer.accept("neginf", Float.NEGATIVE_INFINITY);

        assertEquals(3, result.size());
        assertEquals("nan:NaN", result.get(0));
        assertEquals("posinf:Infinity", result.get(1));
        assertEquals("neginf:-Infinity", result.get(2));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        ObjFloatConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept(null, 5.5f);

        assertEquals(1, result.size());
        assertEquals("null:5.5", result.get(0));
    }

    @Test
    public void testSideEffects() {
        final float[] sum = {0.0f};
        ObjFloatConsumer<String> consumer = (t, value) -> sum[0] += value;

        consumer.accept("a", 1.5f);
        consumer.accept("b", 2.5f);
        consumer.accept("c", 3.0f);

        assertEquals(7.0f, sum[0], 0.001f);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjFloatConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
