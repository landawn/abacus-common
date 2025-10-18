package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteTriFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        ByteTriFunction<String> toString = (a, b, c) -> a + "," + b + "," + c;

        assertEquals("1,2,3", toString.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals("-5,0,10", toString.apply((byte) -5, (byte) 0, (byte) 10));
    }

    @Test
    public void testApplyWithLambda() {
        ByteTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);

        assertEquals(6, sum.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals(15, sum.apply((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testApplyWithAnonymousClass() {
        ByteTriFunction<Double> average = new ByteTriFunction<Double>() {
            @Override
            public Double apply(byte a, byte b, byte c) {
                return (a + b + c) / 3.0;
            }
        };

        assertEquals(2.0, average.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals(5.0, average.apply((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testAndThen() {
        ByteTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);
        java.util.function.Function<Integer, String> intToString = i -> "Sum: " + i;

        ByteTriFunction<String> combined = sum.andThen(intToString);

        assertEquals("Sum: 6", combined.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals("Sum: 15", combined.apply((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testAndThenChaining() {
        ByteTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);
        java.util.function.Function<Integer, Integer> multiply = i -> i * 2;
        java.util.function.Function<Integer, String> toString = i -> "Result: " + i;

        ByteTriFunction<String> combined = sum.andThen(multiply).andThen(toString);

        assertEquals("Result: 12", combined.apply((byte) 1, (byte) 2, (byte) 3));
    }

    @Test
    public void testReturningComplexObject() {
        ByteTriFunction<TestObject> createObject = (a, b, c) -> new TestObject(a, b, c);

        TestObject obj = createObject.apply((byte) 1, (byte) 2, (byte) 3);
        assertEquals(1, obj.a);
        assertEquals(2, obj.b);
        assertEquals(3, obj.c);
    }

    @Test
    public void testWithBoundaryValues() {
        ByteTriFunction<String> toString = (a, b, c) -> a + "," + b + "," + c;

        String result = toString.apply(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE);
        assertEquals(Byte.MIN_VALUE + ",0," + Byte.MAX_VALUE, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteTriFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final byte a;
        final byte b;
        final byte c;

        TestObject(byte a, byte b, byte c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}
