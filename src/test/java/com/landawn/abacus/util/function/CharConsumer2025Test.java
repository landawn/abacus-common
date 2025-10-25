package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<Character> result = new ArrayList<>();
        CharConsumer consumer = c -> result.add(c);

        consumer.accept('a');
        consumer.accept('b');

        assertEquals(2, result.size());
        assertEquals('a', result.get(0));
        assertEquals('b', result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final char[] result = new char[1];
        CharConsumer consumer = c -> result[0] = Character.toUpperCase(c);

        consumer.accept('x');
        assertEquals('X', result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<Character> result = new ArrayList<>();
        CharConsumer consumer = new CharConsumer() {
            @Override
            public void accept(char t) {
                result.add(Character.toLowerCase(t));
            }
        };

        consumer.accept('A');
        assertEquals(1, result.size());
        assertEquals('a', result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<Character> result = new ArrayList<>();
        CharConsumer first = c -> result.add(c);
        CharConsumer second = c -> result.add(Character.toUpperCase(c));

        CharConsumer combined = first.andThen(second);
        combined.accept('a');

        assertEquals(2, result.size());
        assertEquals('a', result.get(0));
        assertEquals('A', result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<Character> result = new ArrayList<>();
        CharConsumer first = c -> result.add(c);
        CharConsumer second = c -> result.add(Character.toUpperCase(c));
        CharConsumer third = c -> result.add(Character.toLowerCase(c));

        CharConsumer combined = first.andThen(second).andThen(third);
        combined.accept('A');

        assertEquals(3, result.size());
        assertEquals('A', result.get(0));
        assertEquals('A', result.get(1));
        assertEquals('a', result.get(2));
    }

    @Test
    public void testWithSpecialCharacters() {
        final List<Character> result = new ArrayList<>();
        CharConsumer consumer = c -> result.add(c);

        consumer.accept('\0');
        consumer.accept('\n');
        consumer.accept('\t');

        assertEquals(3, result.size());
        assertEquals('\0', result.get(0));
        assertEquals('\n', result.get(1));
        assertEquals('\t', result.get(2));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<Character> result = new ArrayList<>();
        CharConsumer consumer = c -> result.add(c);

        consumer.accept(Character.MIN_VALUE);
        consumer.accept(Character.MAX_VALUE);

        assertEquals(2, result.size());
        assertEquals(Character.MIN_VALUE, result.get(0));
        assertEquals(Character.MAX_VALUE, result.get(1));
    }

    @Test
    public void testMethodReference() {
        final List<Character> result = new ArrayList<>();
        CharConsumer consumer = result::add;

        consumer.accept('Z');
        assertEquals(1, result.size());
        assertEquals('Z', result.get(0));
    }

    @Test
    public void testSideEffects() {
        final int[] counter = { 0 };
        CharConsumer consumer = c -> counter[0]++;

        consumer.accept('a');
        consumer.accept('b');
        consumer.accept('c');

        assertEquals(3, counter[0]);
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
