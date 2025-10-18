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
public class ByteConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Byte> result = new ArrayList<>();
        ByteConsumer consumer = b -> result.add(b);

        consumer.accept((byte) 5);
        consumer.accept((byte) 10);

        assertEquals(2, result.size());
        assertEquals((byte) 5, result.get(0));
        assertEquals((byte) 10, result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final byte[] result = new byte[1];
        ByteConsumer consumer = b -> result[0] = (byte) (b * 2);

        consumer.accept((byte) 7);
        assertEquals((byte) 14, result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<Byte> result = new ArrayList<>();
        ByteConsumer consumer = new ByteConsumer() {
            @Override
            public void accept(byte t) {
                result.add((byte) (t + 1));
            }
        };

        consumer.accept((byte) 3);
        assertEquals(1, result.size());
        assertEquals((byte) 4, result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<Byte> result = new ArrayList<>();
        ByteConsumer first = b -> result.add(b);
        ByteConsumer second = b -> result.add((byte) (b * 2));

        ByteConsumer combined = first.andThen(second);
        combined.accept((byte) 5);

        assertEquals(2, result.size());
        assertEquals((byte) 5, result.get(0));
        assertEquals((byte) 10, result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<Byte> result = new ArrayList<>();
        ByteConsumer first = b -> result.add(b);
        ByteConsumer second = b -> result.add((byte) (b + 1));
        ByteConsumer third = b -> result.add((byte) (b * 2));

        ByteConsumer combined = first.andThen(second).andThen(third);
        combined.accept((byte) 5);

        assertEquals(3, result.size());
        assertEquals((byte) 5, result.get(0));
        assertEquals((byte) 6, result.get(1));
        assertEquals((byte) 10, result.get(2));
    }

    @Test
    public void testWithNegativeValues() {
        final List<Byte> result = new ArrayList<>();
        ByteConsumer consumer = b -> result.add(b);

        consumer.accept((byte) -5);
        consumer.accept((byte) -127);

        assertEquals(2, result.size());
        assertEquals((byte) -5, result.get(0));
        assertEquals((byte) -127, result.get(1));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<Byte> result = new ArrayList<>();
        ByteConsumer consumer = b -> result.add(b);

        consumer.accept(Byte.MIN_VALUE);
        consumer.accept(Byte.MAX_VALUE);
        consumer.accept((byte) 0);

        assertEquals(3, result.size());
        assertEquals(Byte.MIN_VALUE, result.get(0));
        assertEquals(Byte.MAX_VALUE, result.get(1));
        assertEquals((byte) 0, result.get(2));
    }

    @Test
    public void testMethodReference() {
        final List<Byte> result = new ArrayList<>();
        ByteConsumer consumer = result::add;

        consumer.accept((byte) 42);
        assertEquals(1, result.size());
        assertEquals((byte) 42, result.get(0));
    }

    @Test
    public void testSideEffects() {
        final byte[] counter = {0};
        ByteConsumer consumer = b -> counter[0]++;

        consumer.accept((byte) 1);
        consumer.accept((byte) 2);
        consumer.accept((byte) 3);

        assertEquals(3, counter[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
