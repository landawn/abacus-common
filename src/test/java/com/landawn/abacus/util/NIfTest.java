package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class NIfTest extends NTestSupport {

    @Test
    public void testIfOrEmpty() {
        assertEquals("value", N.ifOrEmpty(true, () -> "value").get());
        assertFalse(N.ifOrEmpty(false, () -> "value").isPresent());
        assertTrue(N.ifOrEmpty(true, () -> null).isPresent());
        assertNull(N.ifOrEmpty(true, () -> null).get());
        assertThrows(RuntimeException.class, () -> N.ifOrEmpty(true, () -> {
            throw new RuntimeException("Supplier failed");
        }));
        assertFalse(N.ifOrEmpty(false, () -> {
            throw new RuntimeException("Should not execute");
        }).isPresent());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIfOrElse() {
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);
        N.ifOrElse(true, trueCount::incrementAndGet, falseCount::incrementAndGet);
        assertEquals(1, trueCount.get());
        assertEquals(0, falseCount.get());
        N.ifOrElse(false, trueCount::incrementAndGet, falseCount::incrementAndGet);
        assertEquals(1, trueCount.get());
        assertEquals(1, falseCount.get());
        assertThrows(IllegalArgumentException.class, () -> N.ifOrElse(true, null, falseCount::incrementAndGet));
        assertThrows(IllegalArgumentException.class, () -> N.ifOrElse(false, trueCount::incrementAndGet, null));
    }

    @Test
    public void testIfNotNull() {
        AtomicInteger counter = new AtomicInteger(0);
        N.ifNotNull("value", v -> counter.incrementAndGet());
        N.ifNotNull(null, v -> counter.incrementAndGet());
        N.ifNotNull(42, counter::addAndGet);
        assertEquals(43, counter.get());

        TestPerson person = new TestPerson("John", 30);
        N.ifNotNull(person, p -> {
            p.setName("Jane");
            p.setAge(31);
        });
        assertEquals("Jane", person.getName());
        assertEquals(31, person.getAge());
    }

    @Test
    public void testIfNotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);
        N.ifNotEmpty("hello", s -> counter.incrementAndGet());
        N.ifNotEmpty("", s -> counter.incrementAndGet());
        N.ifNotEmpty((CharSequence) null, s -> counter.incrementAndGet());
        N.ifNotEmpty(Arrays.asList(1, 2, 3), c -> counter.incrementAndGet());
        N.ifNotEmpty(Collections.emptyList(), c -> counter.incrementAndGet());
        N.ifNotEmpty((Collection<?>) null, c -> counter.incrementAndGet());
        N.ifNotEmpty((Map<?, ?>) null, m -> counter.incrementAndGet());
        assertEquals(2, counter.get());

        StringBuilder sb = new StringBuilder("content");
        N.ifNotEmpty(sb, s -> s.append(" modified"));
        assertEquals("content modified", sb.toString());
        N.ifNotEmpty(new StringBuilder(), s -> s.append("no"));
        assertEquals("content modified", sb.toString());

        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        N.ifNotEmpty(list, l -> {
            l.add(4);
            l.remove(Integer.valueOf(1));
        });
        assertEquals(Arrays.asList(2, 3, 4), list);

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        N.ifNotEmpty(map, m -> {
            m.put("b", 2);
            m.remove("a");
        });
        assertEquals(2, map.get("b"));
        N.ifNotEmpty(new HashMap<String, Integer>(), m -> m.put("x", 1));
        assertEquals(1, map.size());

        N.ifNotEmpty(new HashSet<>(Arrays.asList("x", "y")), set -> assertEquals(2, set.size()));
        Queue<String> queue = new LinkedList<>(Arrays.asList("a", "b", "c"));
        N.ifNotEmpty(queue, q -> assertEquals(3, q.size()));
        Deque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 4));
        N.ifNotEmpty(deque, d -> assertEquals(4, d.size()));
    }
}
