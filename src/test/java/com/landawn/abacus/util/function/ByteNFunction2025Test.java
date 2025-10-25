package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteNFunction2025Test extends TestBase {

    @Test
    public void testApply() {
        ByteNFunction<String> toString = args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(args[i]);
            }
            return sb.toString();
        };

        assertEquals("1,2,3", toString.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals("5", toString.apply((byte) 5));
    }

    @Test
    public void testApplyWithLambda() {
        ByteNFunction<Integer> sum = args -> {
            int total = 0;
            for (byte b : args) {
                total += b;
            }
            return total;
        };

        assertEquals(6, sum.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals(15, sum.apply((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testApplyWithAnonymousClass() {
        ByteNFunction<Double> average = new ByteNFunction<Double>() {
            @Override
            public Double apply(byte... args) {
                if (args.length == 0)
                    return 0.0;
                double sum = 0;
                for (byte b : args) {
                    sum += b;
                }
                return sum / args.length;
            }
        };

        assertEquals(2.0, average.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals(5.0, average.apply((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testApplyEmptyArray() {
        ByteNFunction<Integer> count = args -> args.length;

        assertEquals(0, count.apply());
    }

    @Test
    public void testApplySingleElement() {
        ByteNFunction<Byte> first = args -> args.length > 0 ? args[0] : null;

        assertEquals((byte) 42, first.apply((byte) 42));
    }

    @Test
    public void testApplyManyElements() {
        ByteNFunction<Integer> count = args -> args.length;

        assertEquals(7, count.apply((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7));
    }

    @Test
    public void testAndThen() {
        ByteNFunction<Integer> sum = args -> {
            int total = 0;
            for (byte b : args) {
                total += b;
            }
            return total;
        };
        java.util.function.Function<Integer, String> intToString = i -> "Sum: " + i;

        ByteNFunction<String> combined = sum.andThen(intToString);

        assertEquals("Sum: 6", combined.apply((byte) 1, (byte) 2, (byte) 3));
        assertEquals("Sum: 15", combined.apply((byte) 5, (byte) 5, (byte) 5));
    }

    @Test
    public void testAndThenChaining() {
        ByteNFunction<Integer> sum = args -> {
            int total = 0;
            for (byte b : args) {
                total += b;
            }
            return total;
        };
        java.util.function.Function<Integer, Integer> multiply = i -> i * 2;
        java.util.function.Function<Integer, String> toString = i -> "Result: " + i;

        ByteNFunction<String> combined = sum.andThen(multiply).andThen(toString);

        assertEquals("Result: 12", combined.apply((byte) 1, (byte) 2, (byte) 3));
    }

    @Test
    public void testReturningComplexObject() {
        ByteNFunction<TestObject> createObject = args -> new TestObject(args);

        TestObject obj = createObject.apply((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, obj.values.length);
        assertEquals(1, obj.values[0]);
    }

    @Test
    public void testWithNegativeValues() {
        ByteNFunction<Integer> sum = args -> {
            int total = 0;
            for (byte b : args) {
                total += b;
            }
            return total;
        };

        assertEquals(-4, sum.apply((byte) -5, (byte) 3, (byte) -2));
    }

    @Test
    public void testWithBoundaryValues() {
        ByteNFunction<String> toString = args -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(args[i]);
            }
            return sb.toString();
        };

        String result = toString.apply(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE);
        assertEquals(Byte.MIN_VALUE + ",0," + Byte.MAX_VALUE, result);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteNFunction.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        final byte[] values;

        TestObject(byte... values) {
            this.values = values;
        }
    }
}
