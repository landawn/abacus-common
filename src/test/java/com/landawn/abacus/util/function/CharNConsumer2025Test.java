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
public class CharNConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<char[]> result = new ArrayList<>();
        CharNConsumer consumer = args -> result.add(args);

        consumer.accept('a', 'b', 'c');
        assertEquals(1, result.size());
        assertArrayEquals(new char[]{'a', 'b', 'c'}, result.get(0));
    }

    @Test
    public void testAcceptWithLambda() {
        final String[] result = new String[1];
        CharNConsumer consumer = args -> {
            StringBuilder sb = new StringBuilder();
            for (char c : args) {
                sb.append(c);
            }
            result[0] = sb.toString();
        };

        consumer.accept('H', 'e', 'l', 'l', 'o');
        assertEquals("Hello", result[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        CharNConsumer consumer = new CharNConsumer() {
            @Override
            public void accept(char... args) {
                StringBuilder sb = new StringBuilder();
                for (char c : args) {
                    sb.append(Character.toUpperCase(c));
                }
                result.add(sb.toString());
            }
        };

        consumer.accept('h', 'i');
        assertEquals(1, result.size());
        assertEquals("HI", result.get(0));
    }

    @Test
    public void testAcceptEmptyArray() {
        final List<char[]> result = new ArrayList<>();
        CharNConsumer consumer = args -> result.add(args);

        consumer.accept();
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).length);
    }

    @Test
    public void testAcceptSingleElement() {
        final List<char[]> result = new ArrayList<>();
        CharNConsumer consumer = args -> result.add(args);

        consumer.accept('X');
        assertEquals(1, result.size());
        assertArrayEquals(new char[]{'X'}, result.get(0));
    }

    @Test
    public void testAcceptManyElements() {
        final int[] result = new int[1];
        CharNConsumer consumer = args -> result[0] = args.length;

        consumer.accept('a', 'b', 'c', 'd', 'e', 'f', 'g');
        assertEquals(7, result[0]);
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        CharNConsumer first = args -> {
            StringBuilder sb = new StringBuilder();
            for (char c : args) {
                sb.append(c);
            }
            result.add(sb.toString());
        };
        CharNConsumer second = args -> result.add(String.valueOf(args.length));

        CharNConsumer combined = first.andThen(second);
        combined.accept('x', 'y', 'z');

        assertEquals(2, result.size());
        assertEquals("xyz", result.get(0));
        assertEquals("3", result.get(1));
    }

    @Test
    public void testAndThenChaining() {
        final List<String> result = new ArrayList<>();
        CharNConsumer first = args -> result.add(String.valueOf(args.length));
        CharNConsumer second = args -> result.add(args.length > 0 ? String.valueOf(args[0]) : "");
        CharNConsumer third = args -> result.add(args.length > 1 ? String.valueOf(args[1]) : "");

        CharNConsumer combined = first.andThen(second).andThen(third);
        combined.accept('a', 'b', 'c');

        assertEquals(3, result.size());
        assertEquals("3", result.get(0));
        assertEquals("a", result.get(1));
        assertEquals("b", result.get(2));
    }

    @Test
    public void testWithSpecialCharacters() {
        final List<char[]> result = new ArrayList<>();
        CharNConsumer consumer = args -> result.add(args);

        consumer.accept('\n', '\t', ' ');
        assertEquals(1, result.size());
        assertArrayEquals(new char[]{'\n', '\t', ' '}, result.get(0));
    }

    @Test
    public void testWithBoundaryValues() {
        final List<char[]> result = new ArrayList<>();
        CharNConsumer consumer = args -> result.add(args);

        consumer.accept(Character.MIN_VALUE, 'M', Character.MAX_VALUE);
        assertEquals(1, result.size());
        assertArrayEquals(new char[]{Character.MIN_VALUE, 'M', Character.MAX_VALUE}, result.get(0));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharNConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
