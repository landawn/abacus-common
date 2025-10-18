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
public class CharBiConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Pair<Character, Character>> result = new ArrayList<>();
        CharBiConsumer consumer = (t, u) -> result.add(Pair.of(t, u));

        consumer.accept('a', 'b');
        consumer.accept('x', 'y');

        assertEquals(2, result.size());
        assertEquals('a', result.get(0).left());
        assertEquals('b', result.get(0).right());
        assertEquals('x', result.get(1).left());
        assertEquals('y', result.get(1).right());
    }

    @Test
    public void testAcceptWithLambda() {
        final String[] result = new String[1];
        CharBiConsumer consumer = (t, u) -> result[0] = "" + t + u;

        consumer.accept('H', 'i');
        assertEquals("Hi", result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        CharBiConsumer consumer = new CharBiConsumer() {
            @Override
            public void accept(char t, char u) {
                result.add("" + Character.toUpperCase(t) + Character.toUpperCase(u));
            }
        };

        consumer.accept('a', 'b');
        assertEquals(1, result.size());
        assertEquals("AB", result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        CharBiConsumer first = (t, u) -> result.add("" + t + u);
        CharBiConsumer second = (t, u) -> result.add("" + u + t);

        CharBiConsumer combined = first.andThen(second);
        combined.accept('a', 'b');

        assertEquals(2, result.size());
        assertEquals("ab", result.get(0));
        assertEquals("ba", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<String> result = new ArrayList<>();
        CharBiConsumer first = (t, u) -> result.add("" + t);
        CharBiConsumer second = (t, u) -> result.add("" + u);
        CharBiConsumer third = (t, u) -> result.add("" + t + u);

        CharBiConsumer combined = first.andThen(second).andThen(third);
        combined.accept('x', 'y');

        assertEquals(3, result.size());
        assertEquals("x", result.get(0));
        assertEquals("y", result.get(1));
        assertEquals("xy", result.get(2));
    }

    @Test
    public void testWithSpecialCharacters() {
        final List<String> result = new ArrayList<>();
        CharBiConsumer consumer = (t, u) -> result.add("" + t + u);

        consumer.accept('\n', '\t');
        assertEquals(1, result.size());
        assertEquals("\n\t", result.get(0));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<Pair<Character, Character>> result = new ArrayList<>();
        CharBiConsumer consumer = (t, u) -> result.add(Pair.of(t, u));

        consumer.accept(Character.MIN_VALUE, Character.MAX_VALUE);
        assertEquals(1, result.size());
        assertEquals(Character.MIN_VALUE, result.get(0).left());
        assertEquals(Character.MAX_VALUE, result.get(0).right());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharBiConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
