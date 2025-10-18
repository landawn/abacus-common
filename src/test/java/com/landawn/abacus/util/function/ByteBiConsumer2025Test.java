package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Pair;

@Tag("2025")
public class ByteBiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Pair<Byte, Byte>> result = new ArrayList<>();
        ByteBiConsumer consumer = (t, u) -> result.add(Pair.of(t, u));

        consumer.accept((byte) 3, (byte) 5);
        consumer.accept((byte) 7, (byte) 9);

        assertEquals(2, result.size());
        assertEquals((byte) 3, result.get(0).left());
        assertEquals((byte) 5, result.get(0).right());
        assertEquals((byte) 7, result.get(1).left());
        assertEquals((byte) 9, result.get(1).right());
    }

    @Test
    public void testAcceptWithLambda() {
        final byte[] result = new byte[1];
        ByteBiConsumer consumer = (t, u) -> result[0] = (byte) (t + u);

        consumer.accept((byte) 10, (byte) 20);
        assertEquals((byte) 30, result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<Byte> result = new ArrayList<>();
        ByteBiConsumer consumer = new ByteBiConsumer() {
            @Override
            public void accept(byte t, byte u) {
                result.add((byte) (t * u));
            }
        };

        consumer.accept((byte) 3, (byte) 4);
        assertEquals(1, result.size());
        assertEquals((byte) 12, result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<Byte> result = new ArrayList<>();
        ByteBiConsumer first = (t, u) -> result.add((byte) (t + u));
        ByteBiConsumer second = (t, u) -> result.add((byte) (t - u));

        ByteBiConsumer combined = first.andThen(second);
        combined.accept((byte) 10, (byte) 5);

        assertEquals(2, result.size());
        assertEquals((byte) 15, result.get(0));
        assertEquals((byte) 5, result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<Byte> result = new ArrayList<>();
        ByteBiConsumer first = (t, u) -> result.add(t);
        ByteBiConsumer second = (t, u) -> result.add(u);
        ByteBiConsumer third = (t, u) -> result.add((byte) (t + u));

        ByteBiConsumer combined = first.andThen(second).andThen(third);
        combined.accept((byte) 5, (byte) 7);

        assertEquals(3, result.size());
        assertEquals((byte) 5, result.get(0));
        assertEquals((byte) 7, result.get(1));
        assertEquals((byte) 12, result.get(2));
    }

    @Test
    public void testWithNegativeValues() {
        final List<Byte> result = new ArrayList<>();
        ByteBiConsumer consumer = (t, u) -> result.add((byte) (t + u));

        consumer.accept((byte) -5, (byte) 3);
        assertEquals(1, result.size());
        assertEquals((byte) -2, result.get(0));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<Pair<Byte, Byte>> result = new ArrayList<>();
        ByteBiConsumer consumer = (t, u) -> result.add(Pair.of(t, u));

        consumer.accept(Byte.MIN_VALUE, Byte.MAX_VALUE);
        assertEquals(1, result.size());
        assertEquals(Byte.MIN_VALUE, result.get(0).left());
        assertEquals(Byte.MAX_VALUE, result.get(0).right());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteBiConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
