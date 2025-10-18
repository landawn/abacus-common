package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Triple;

@Tag("2025")
public class ByteTriConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Triple<Byte, Byte, Byte>> result = new ArrayList<>();
        ByteTriConsumer consumer = (a, b, c) -> result.add(Triple.of(a, b, c));

        consumer.accept((byte) 1, (byte) 2, (byte) 3);
        consumer.accept((byte) 4, (byte) 5, (byte) 6);

        assertEquals(2, result.size());
        assertEquals((byte) 1, result.get(0).left());
        assertEquals((byte) 2, result.get(0).middle());
        assertEquals((byte) 3, result.get(0).right());
    }

    @Test
    public void testAcceptWithLambda() {
        final byte[] result = new byte[1];
        ByteTriConsumer consumer = (a, b, c) -> result[0] = (byte) (a + b + c);

        consumer.accept((byte) 10, (byte) 20, (byte) 30);
        assertEquals((byte) 60, result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<Byte> result = new ArrayList<>();
        ByteTriConsumer consumer = new ByteTriConsumer() {
            @Override
            public void accept(byte a, byte b, byte c) {
                result.add((byte) (a * b * c));
            }
        };

        consumer.accept((byte) 2, (byte) 3, (byte) 4);
        assertEquals(1, result.size());
        assertEquals((byte) 24, result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<Byte> result = new ArrayList<>();
        ByteTriConsumer first = (a, b, c) -> result.add((byte) (a + b + c));
        ByteTriConsumer second = (a, b, c) -> result.add((byte) (a * b * c));

        ByteTriConsumer combined = first.andThen(second);
        combined.accept((byte) 2, (byte) 3, (byte) 4);

        assertEquals(2, result.size());
        assertEquals((byte) 9, result.get(0));
        assertEquals((byte) 24, result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<Byte> result = new ArrayList<>();
        ByteTriConsumer first = (a, b, c) -> result.add(a);
        ByteTriConsumer second = (a, b, c) -> result.add(b);
        ByteTriConsumer third = (a, b, c) -> result.add(c);

        ByteTriConsumer combined = first.andThen(second).andThen(third);
        combined.accept((byte) 5, (byte) 10, (byte) 15);

        assertEquals(3, result.size());
        assertEquals((byte) 5, result.get(0));
        assertEquals((byte) 10, result.get(1));
        assertEquals((byte) 15, result.get(2));
    }

    @Test
    public void testWithNegativeValues() {
        final List<Byte> result = new ArrayList<>();
        ByteTriConsumer consumer = (a, b, c) -> result.add((byte) (a + b + c));

        consumer.accept((byte) -5, (byte) 3, (byte) -2);
        assertEquals(1, result.size());
        assertEquals((byte) -4, result.get(0));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<Triple<Byte, Byte, Byte>> result = new ArrayList<>();
        ByteTriConsumer consumer = (a, b, c) -> result.add(Triple.of(a, b, c));

        consumer.accept(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE);
        assertEquals(1, result.size());
        assertEquals(Byte.MIN_VALUE, result.get(0).left());
        assertEquals((byte) 0, result.get(0).middle());
        assertEquals(Byte.MAX_VALUE, result.get(0).right());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteTriConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
