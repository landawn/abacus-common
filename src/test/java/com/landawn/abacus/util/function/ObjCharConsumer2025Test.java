package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjCharConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjCharConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("test", 'a');
        consumer.accept("value", 'b');

        assertEquals(2, result.size());
        assertEquals("test:a", result.get(0));
        assertEquals("value:b", result.get(1));
    }

    @Test
    public void testAcceptWithLambda() {
        final char[] charResult = new char[1];
        final String[] stringResult = new String[1];
        ObjCharConsumer<String> consumer = (t, value) -> {
            charResult[0] = Character.toUpperCase(value);
            stringResult[0] = t.toUpperCase();
        };

        consumer.accept("hello", 'x');

        assertEquals('X', charResult[0]);
        assertEquals("HELLO", stringResult[0]);
    }

    @Test
    public void testAcceptWithAnonymousClass() {
        final List<String> result = new ArrayList<>();
        ObjCharConsumer<String> consumer = new ObjCharConsumer<String>() {
            @Override
            public void accept(String t, char value) {
                result.add(String.format("%s[%c]", t, value));
            }
        };

        consumer.accept("test", 'Z');

        assertEquals(1, result.size());
        assertEquals("test[Z]", result.get(0));
    }

    @Test
    public void testWithDigitChars() {
        final List<String> result = new ArrayList<>();
        ObjCharConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("digit", '5');
        consumer.accept("zero", '0');

        assertEquals(2, result.size());
        assertEquals("digit:5", result.get(0));
        assertEquals("zero:0", result.get(1));
    }

    @Test
    public void testWithSpecialChars() {
        final List<String> result = new ArrayList<>();
        ObjCharConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept("newline", '\n');
        consumer.accept("tab", '\t');
        consumer.accept("space", ' ');

        assertEquals(3, result.size());
        assertEquals("newline:\n", result.get(0));
        assertEquals("tab:\t", result.get(1));
        assertEquals("space: ", result.get(2));
    }

    @Test
    public void testWithNullObject() {
        final List<String> result = new ArrayList<>();
        ObjCharConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);

        consumer.accept(null, 'a');

        assertEquals(1, result.size());
        assertEquals("null:a", result.get(0));
    }

    @Test
    public void testSideEffects() {
        final StringBuilder sb = new StringBuilder();
        ObjCharConsumer<String> consumer = (t, value) -> sb.append(value);

        consumer.accept("a", 'H');
        consumer.accept("b", 'e');
        consumer.accept("c", 'l');
        consumer.accept("d", 'l');
        consumer.accept("e", 'o');

        assertEquals("Hello", sb.toString());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjCharConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
