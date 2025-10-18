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
public class CharTriConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Triple<Character, Character, Character>> result = new ArrayList<>();
        CharTriConsumer consumer = (a, b, c) -> result.add(Triple.of(a, b, c));

        consumer.accept('a', 'b', 'c');
        consumer.accept('x', 'y', 'z');

        assertEquals(2, result.size());
        assertEquals('a', result.get(0).left());
        assertEquals('b', result.get(0).middle());
        assertEquals('c', result.get(0).right());
    }

    @Test
    public void testAcceptWithLambda() {
        final String[] result = new String[1];
        CharTriConsumer consumer = (a, b, c) -> result[0] = "" + a + b + c;

        consumer.accept('H', 'e', 'y');
        assertEquals("Hey", result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        CharTriConsumer consumer = new CharTriConsumer() {
            @Override
            public void accept(char a, char b, char c) {
                result.add("" + Character.toUpperCase(a) + Character.toUpperCase(b) + Character.toUpperCase(c));
            }
        };

        consumer.accept('a', 'b', 'c');
        assertEquals(1, result.size());
        assertEquals("ABC", result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        CharTriConsumer first = (a, b, c) -> result.add("" + a + b + c);
        CharTriConsumer second = (a, b, c) -> result.add("" + c + b + a);

        CharTriConsumer combined = first.andThen(second);
        combined.accept('a', 'b', 'c');

        assertEquals(2, result.size());
        assertEquals("abc", result.get(0));
        assertEquals("cba", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<Character> result = new ArrayList<>();
        CharTriConsumer first = (a, b, c) -> result.add(a);
        CharTriConsumer second = (a, b, c) -> result.add(b);
        CharTriConsumer third = (a, b, c) -> result.add(c);

        CharTriConsumer combined = first.andThen(second).andThen(third);
        combined.accept('x', 'y', 'z');

        assertEquals(3, result.size());
        assertEquals('x', result.get(0));
        assertEquals('y', result.get(1));
        assertEquals('z', result.get(2));
    }

    @Test
    public void testWithSpecialCharacters() {
        final List<String> result = new ArrayList<>();
        CharTriConsumer consumer = (a, b, c) -> result.add("" + a + b + c);

        consumer.accept('\n', '\t', ' ');
        assertEquals(1, result.size());
        assertEquals("\n\t ", result.get(0));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<Triple<Character, Character, Character>> result = new ArrayList<>();
        CharTriConsumer consumer = (a, b, c) -> result.add(Triple.of(a, b, c));

        consumer.accept(Character.MIN_VALUE, 'M', Character.MAX_VALUE);
        assertEquals(1, result.size());
        assertEquals(Character.MIN_VALUE, result.get(0).left());
        assertEquals('M', result.get(0).middle());
        assertEquals(Character.MAX_VALUE, result.get(0).right());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharTriConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
