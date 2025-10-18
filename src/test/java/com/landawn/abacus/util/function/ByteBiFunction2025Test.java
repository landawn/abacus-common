package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteBiFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        ByteBiFunction<String> toString = (t, u) -> t + "," + u;

        assertEquals("5,10", toString.apply((byte) 5, (byte) 10));
        assertEquals("-3,7", toString.apply((byte) -3, (byte) 7));
    }

    @Test
    public void testApplyWithLambda() {
        ByteBiFunction<Integer> sum = (t, u) -> (int) (t + u);

        assertEquals(15, sum.apply((byte) 5, (byte) 10));
        assertEquals(4, sum.apply((byte) -3, (byte) 7));
    }

    @Test
    public void testApplyWithAnonymousClass() {
        ByteBiFunction<Double> product = new ByteBiFunction<Double>() {
            @Override
            public Double apply(byte t, byte u) {
                return (double) t * u;
            }
        };

        assertEquals(50.0, product.apply((byte) 5, (byte) 10));
        assertEquals(-21.0, product.apply((byte) -3, (byte) 7));
    }

    @Test
    public void testAndThen() {
        ByteBiFunction<Integer> sum = (t, u) -> (int) (t + u);
        java.util.function.Function<Integer, String> intToString = i -> "Sum: " + i;

        ByteBiFunction<String> combined = sum.andThen(intToString);

        assertEquals("Sum: 15", combined.apply((byte) 5, (byte) 10));
        assertEquals("Sum: 4", combined.apply((byte) -3, (byte) 7));
    }

    @Test
    public void testAndThenChaining() {
        ByteBiFunction<Integer> sum = (t, u) -> (int) (t + u);
        java.util.function.Function<Integer, Integer> multiply = i -> i * 2;
        java.util.function.Function<Integer, String> toString = i -> "Result: " + i;

        ByteBiFunction<String> combined = sum.andThen(multiply).andThen(toString);

        assertEquals("Result: 30", combined.apply((byte) 5, (byte) 10));
    }

    @Test
    public void testReturningComplexObject() {
        ByteBiFunction<TestObject> createObject = (t, u) -> new TestObject(t, u);

        TestObject obj = createObject.apply((byte) 5, (byte) 10);
        assertEquals(5, obj.first);
        assertEquals(10, obj.second);
    }

    @Test
    public void testWithBoundaryValues() {
        ByteBiFunction<String> toString = (t, u) -> t + "," + u;

        String result = toString.apply(Byte.MIN_VALUE, Byte.MAX_VALUE);
        assertEquals(Byte.MIN_VALUE + "," + Byte.MAX_VALUE, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteBiFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final byte first;
        final byte second;

        TestObject(byte first, byte second) {
            this.first = first;
            this.second = second;
        }
    }
}
