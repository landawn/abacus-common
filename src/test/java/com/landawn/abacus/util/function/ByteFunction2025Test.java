package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        ByteFunction<String> toString = b -> String.valueOf(b);

        assertEquals("5", toString.apply((byte) 5));
        assertEquals("-10", toString.apply((byte) -10));
        assertEquals("0", toString.apply((byte) 0));
    }

    @Test
    public void testApplyWithLambda() {
        ByteFunction<Integer> toInt = b -> (int) b;

        assertEquals(5, toInt.apply((byte) 5));
        assertEquals(-127, toInt.apply((byte) -127));
    }

    @Test
    public void testApplyWithAnonymousClass() {
        ByteFunction<Double> toDouble = new ByteFunction<Double>() {
            @Override
            public Double apply(byte value) {
                return (double) value * 2;
            }
        };

        assertEquals(10.0, toDouble.apply((byte) 5));
        assertEquals(-20.0, toDouble.apply((byte) -10));
    }

    @Test
    public void testAndThen() {
        ByteFunction<Integer> toInt = b -> (int) b;
        java.util.function.Function<Integer, String> intToString = i -> "Number: " + i;

        ByteFunction<String> combined = toInt.andThen(intToString);

        assertEquals("Number: 5", combined.apply((byte) 5));
        assertEquals("Number: -10", combined.apply((byte) -10));
    }

    @Test
    public void testAndThenChaining() {
        ByteFunction<Integer> toInt = b -> (int) b;
        java.util.function.Function<Integer, Integer> multiply = i -> i * 2;
        java.util.function.Function<Integer, String> toString = i -> "Result: " + i;

        ByteFunction<String> combined = toInt.andThen(multiply).andThen(toString);

        assertEquals("Result: 10", combined.apply((byte) 5));
    }

    @Test
    public void testBox() {
        assertEquals(Byte.valueOf((byte) 5), ByteFunction.BOX.apply((byte) 5));
        assertEquals(Byte.valueOf((byte) -127), ByteFunction.BOX.apply((byte) -127));
        assertEquals(Byte.valueOf((byte) 0), ByteFunction.BOX.apply((byte) 0));
    }

    @Test
    public void testIdentity() {
        ByteFunction<Byte> identity = ByteFunction.identity();

        assertEquals(Byte.valueOf((byte) 5), identity.apply((byte) 5));
        assertEquals(Byte.valueOf((byte) -10), identity.apply((byte) -10));
        assertEquals(Byte.valueOf((byte) 0), identity.apply((byte) 0));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteFunction<String> toString = b -> String.valueOf(b);

        assertEquals(String.valueOf(Byte.MAX_VALUE), toString.apply(Byte.MAX_VALUE));
        assertEquals(String.valueOf(Byte.MIN_VALUE), toString.apply(Byte.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        ByteFunction<String> toString = String::valueOf;

        assertEquals("42", toString.apply((byte) 42));
    }

    @Test
    public void testReturningComplexObject() {
        ByteFunction<TestObject> createObject = b -> new TestObject(b);

        TestObject obj = createObject.apply((byte) 15);
        assertEquals(15, obj.value);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final byte value;

        TestObject(byte value) {
            this.value = value;
        }
    }
}
