package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteNConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<byte[]> result = new ArrayList<>();
        ByteNConsumer consumer = args -> result.add(args);

        consumer.accept((byte) 1, (byte) 2, (byte) 3);
        assertEquals(1, result.size());
        assertArrayEquals(new byte[]{1, 2, 3}, result.get(0));
    }

    @Test
    public void testAcceptWithLambda() {
        final byte[] result = new byte[1];
        ByteNConsumer consumer = args -> {
            byte sum = 0;
            for (byte b : args) {
                sum += b;
            }
            result[0] = sum;
        };

        consumer.accept((byte) 10, (byte) 20, (byte) 30);
        assertEquals((byte) 60, result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<Byte> result = new ArrayList<>();
        ByteNConsumer consumer = new ByteNConsumer() {
            @Override
            public void accept(byte... args) {
                byte product = 1;
                for (byte b : args) {
                    product *= b;
                }
                result.add(product);
            }
        };

        consumer.accept((byte) 2, (byte) 3, (byte) 4);
        assertEquals(1, result.size());
        assertEquals((byte) 24, result.get(0));
    }

    @Test
    public void testAcceptEmptyArray() {
        final List<byte[]> result = new ArrayList<>();
        ByteNConsumer consumer = args -> result.add(args);

        consumer.accept();
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).length);
    }

    @Test
    public void testAcceptSingleElement() {
        final List<byte[]> result = new ArrayList<>();
        ByteNConsumer consumer = args -> result.add(args);

        consumer.accept((byte) 42);
        assertEquals(1, result.size());
        assertArrayEquals(new byte[]{42}, result.get(0));
    }

    @Test
    public void testAcceptManyElements() {
        final byte[] result = new byte[1];
        ByteNConsumer consumer = args -> result[0] = (byte) args.length;

        consumer.accept((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7);
        assertEquals((byte) 7, result[0]);
    }

    @Test
    public void testAndThen() {
        final List<Byte> result = new ArrayList<>();
        ByteNConsumer first = args -> {
            byte sum = 0;
            for (byte b : args) {
                sum += b;
            }
            result.add(sum);
        };
        ByteNConsumer second = args -> result.add((byte) args.length);

        ByteNConsumer combined = first.andThen(second);
        combined.accept((byte) 5, (byte) 10, (byte) 15);

        assertEquals(2, result.size());
        assertEquals((byte) 30, result.get(0));
        assertEquals((byte) 3, result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<Byte> result = new ArrayList<>();
        ByteNConsumer first = args -> result.add((byte) args.length);
        ByteNConsumer second = args -> result.add(args.length > 0 ? args[0] : 0);
        ByteNConsumer third = args -> result.add(args.length > 1 ? args[1] : 0);

        ByteNConsumer combined = first.andThen(second).andThen(third);
        combined.accept((byte) 5, (byte) 10, (byte) 15);

        assertEquals(3, result.size());
        assertEquals((byte) 3, result.get(0));
        assertEquals((byte) 5, result.get(1));
        assertEquals((byte) 10, result.get(2));
    }

    @Test
    public void testWithNegativeValues() {
        final byte[] result = new byte[1];
        ByteNConsumer consumer = args -> {
            byte sum = 0;
            for (byte b : args) {
                sum += b;
            }
            result[0] = sum;
        };

        consumer.accept((byte) -5, (byte) 3, (byte) -2);
        assertEquals((byte) -4, result[0]);
    }

    @Test
    public void testWithBoundaryValues() {
        final List<byte[]> result = new ArrayList<>();
        ByteNConsumer consumer = args -> result.add(args);

        consumer.accept(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE);
        assertEquals(1, result.size());
        assertArrayEquals(new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE}, result.get(0));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteNConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
