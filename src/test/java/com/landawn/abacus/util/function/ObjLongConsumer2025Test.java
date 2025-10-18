package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ObjLongConsumer2025Test extends TestBase {

    @Test
    public void testAccept() {
        final List<String> result = new ArrayList<>();
        ObjLongConsumer<String> consumer = (t, value) -> result.add(t + ":" + value);
        consumer.accept("test", 5L);
        assertEquals(1, result.size());
        assertEquals("test:5", result.get(0));
    }

    @Test
    public void testAndThen() {
        final List<String> result = new ArrayList<>();
        ObjLongConsumer<String> first = (t, value) -> result.add(t + ":" + value);
        ObjLongConsumer<String> second = (t, value) -> result.add(value + ":" + t);
        first.andThen(second).accept("test", 5L);
        assertEquals(2, result.size());
        assertEquals("test:5", result.get(0));
        assertEquals("5:test", result.get(1));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ObjLongConsumer.class.getAnnotation(FunctionalInterface.class));
    }
}
