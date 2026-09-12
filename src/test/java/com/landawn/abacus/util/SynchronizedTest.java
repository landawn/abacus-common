package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SynchronizedTest extends TestBase {

    @Test
    public void testOn() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        assertNotNull(sync);
        assertThrows(IllegalArgumentException.class, () -> Synchronized.on(null));
    }

    @Test
    public void testInstanceRunCallTest() throws Exception {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        sync.run(() -> list.add("test"));
        assertEquals(List.of("test"), list);
        sync.run(() -> list.add("second"));
        sync.run(() -> list.add("third"));
        assertEquals(3, list.size());
        assertThrows(IllegalArgumentException.class, () -> sync.run(null));
        RuntimeException runEx = assertThrows(RuntimeException.class, () -> sync.run(() -> {
            throw new RuntimeException("Instance run exception");
        }));
        assertEquals("Instance run exception", runEx.getMessage());

        Map<String, Integer> map = new HashMap<>();
        map.put("count", 42);
        Synchronized<Map<String, Integer>> syncMap = Synchronized.on(map);
        assertEquals(42, syncMap.call(() -> map.get("count")).intValue());
        AtomicInteger counter = new AtomicInteger(0);
        Synchronized<AtomicInteger> syncCounter = Synchronized.on(counter);
        assertEquals(1, syncCounter.call(counter::incrementAndGet).intValue());
        assertEquals(2, syncCounter.call(counter::incrementAndGet).intValue());
        assertNull(sync.call(() -> null));
        assertThrows(IllegalArgumentException.class, () -> sync.call(null));
        RuntimeException callEx = assertThrows(RuntimeException.class, () -> sync.call(() -> {
            throw new RuntimeException("Instance call exception");
        }));
        assertEquals("Instance call exception", callEx.getMessage());

        assertTrue(sync.test(l -> !l.isEmpty()));
        assertFalse(Synchronized.on(new ArrayList<String>()).test(l -> !l.isEmpty()));
        assertFalse(syncMap.test(m -> m.containsKey("key")));
        map.put("key", 10);
        assertTrue(syncMap.test(m -> m.containsKey("key")));
        assertTrue(syncMap.test(m -> m.get("key") == 10));
        assertThrows(IllegalArgumentException.class, () -> sync.test(null));
        RuntimeException testEx = assertThrows(RuntimeException.class, () -> sync.test(l -> {
            throw new RuntimeException("Instance test exception");
        }));
        assertEquals("Instance test exception", testEx.getMessage());
    }

    @Test
    public void testInstanceAcceptApply() {
        StringBuilder sb = new StringBuilder();
        Synchronized<StringBuilder> syncSb = Synchronized.on(sb);
        syncSb.accept(s -> s.append("Hello"));
        assertEquals("Hello", sb.toString());
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        sync.accept(l -> l.add("first"));
        sync.accept(l -> l.add("second"));
        sync.accept(l -> {
            l.add("third");
            l.add("fourth");
        });
        assertEquals(4, list.size());
        assertThrows(IllegalArgumentException.class, () -> syncSb.accept(null));
        RuntimeException acceptEx = assertThrows(RuntimeException.class, () -> syncSb.accept(s -> {
            throw new RuntimeException("Instance accept exception");
        }));
        assertEquals("Instance accept exception", acceptEx.getMessage());

        list.clear();
        list.add("a");
        list.add("b");
        list.add("c");
        assertEquals(3, sync.apply(List::size).intValue());
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 42);
        Synchronized<Map<String, Integer>> syncMap = Synchronized.on(map);
        assertEquals("Count: 42", syncMap.apply(m -> "Count: " + m.get("count")));
        assertEquals(20, Synchronized.on(new HashMap<>(Map.of("count", 10))).apply(m -> m.get("count") * 2).intValue());
        assertNull(sync.apply(l -> null));
        assertThrows(IllegalArgumentException.class, () -> sync.apply(null));
        RuntimeException applyEx = assertThrows(RuntimeException.class, () -> sync.apply(l -> {
            throw new RuntimeException("Instance apply exception");
        }));
        assertEquals("Instance apply exception", applyEx.getMessage());

        AtomicReference<String> ref = new AtomicReference<>("initial");
        Synchronized.on(ref).accept(r -> r.set("updated"));
        assertEquals("updated", ref.get());
        AtomicBoolean flag = new AtomicBoolean(false);
        Synchronized.on(flag).run(() -> flag.set(true));
        assertTrue(flag.get());

        Map<String, List<Integer>> complexMap = new HashMap<>();
        complexMap.put("numbers", new ArrayList<>());
        Synchronized<Map<String, List<Integer>>> syncComplex = Synchronized.on(complexMap);
        syncComplex.accept(m -> {
            m.get("numbers").add(1);
            m.get("numbers").add(2);
            m.get("numbers").add(3);
        });
        assertEquals(6, syncComplex.apply(m -> m.get("numbers").stream().mapToInt(Integer::intValue).sum()).intValue());
    }

    @Test
    public void testInstanceMixedAndNested() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        Synchronized.run(list, () -> list.add("static1"));
        Synchronized.accept(list, l -> l.add("static2"));
        sync.run(() -> list.add("instance1"));
        sync.accept(l -> l.add("instance2"));
        assertEquals(4, list.size());
        assertTrue(list.containsAll(List.of("static1", "static2", "instance1", "instance2")));

        sync.run(() -> {
            list.add("outer");
            sync.run(() -> list.add("inner"));
        });
        assertEquals("outer", list.get(4));
        assertEquals("inner", list.get(5));
    }

    @Test
    public void testStaticRunCall() {
        List<String> list = new ArrayList<>();
        Synchronized.run(list, () -> list.add("test"));
        assertEquals(List.of("test"), list);
        Synchronized.run(list, () -> {
            list.add("item1");
            list.add("item2");
        });
        assertTrue(list.contains("item1"));
        assertTrue(list.contains("item2"));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.run(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.run(list, null));
        RuntimeException runEx = assertThrows(RuntimeException.class, () -> Synchronized.run(list, () -> {
            throw new RuntimeException("Test exception");
        }));
        assertEquals("Test exception", runEx.getMessage());

        List<String> items = new ArrayList<>();
        items.add("item");
        assertEquals("item", Synchronized.call(items, () -> items.get(0)));
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 42);
        assertEquals(42, Synchronized.call(map, () -> {
            int value = map.get("count");
            map.put("count", value + 1);
            return value;
        }).intValue());
        assertEquals(43, map.get("count").intValue());
        assertThrows(IllegalArgumentException.class, () -> Synchronized.call(null, () -> "result"));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.call(list, null));
        RuntimeException callEx = assertThrows(RuntimeException.class, () -> Synchronized.call(list, () -> {
            throw new RuntimeException("Call exception");
        }));
        assertEquals("Call exception", callEx.getMessage());
    }

    @Test
    public void testStaticTest() {
        List<String> list = new ArrayList<>();
        list.add("item");
        assertTrue(Synchronized.test(list, l -> !l.isEmpty()));
        assertFalse(Synchronized.test(new ArrayList<String>(), l -> !l.isEmpty()));
        assertFalse(Synchronized.test(list, l -> l.size() > 5));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.test(null, x -> true));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.test(list, null));
        RuntimeException testEx = assertThrows(RuntimeException.class, () -> Synchronized.test(list, l -> {
            throw new RuntimeException("Predicate exception");
        }));
        assertEquals("Predicate exception", testEx.getMessage());

        Map<String, Integer> map = new HashMap<>();
        map.put("count", 10);
        assertTrue(Synchronized.test(map, 5, (m, threshold) -> m.get("count") > threshold));
        map.put("count", 3);
        assertFalse(Synchronized.test(map, 5, (m, threshold) -> m.get("count") > threshold));
        assertTrue(Synchronized.test(map, null, (m, threshold) -> threshold == null));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.test(null, "value", (x, y) -> true));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.test(map, 5, null));
        RuntimeException biEx = assertThrows(RuntimeException.class, () -> Synchronized.test(map, 5, (m, threshold) -> {
            throw new RuntimeException("BiPredicate exception");
        }));
        assertEquals("BiPredicate exception", biEx.getMessage());
    }

    @Test
    public void testStaticAccept() {
        StringBuilder sb = new StringBuilder();
        Synchronized.accept(sb, s -> s.append("Hello"));
        assertEquals("Hello", sb.toString());
        List<String> list = new ArrayList<>();
        Synchronized.accept(list, l -> {
            l.add("first");
            l.add("second");
        });
        assertEquals(List.of("first", "second"), list);
        assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(null, x -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(sb, null));
        RuntimeException acceptEx = assertThrows(RuntimeException.class, () -> Synchronized.accept(sb, s -> {
            throw new RuntimeException("Consumer exception");
        }));
        assertEquals("Consumer exception", acceptEx.getMessage());

        Map<String, String> map = new HashMap<>();
        Synchronized.accept(map, "key", (m, k) -> m.put(k, "value"));
        assertEquals("value", map.get("key"));
        Synchronized.accept(map, null, (m, k) -> m.put("nullKey", k == null ? "null" : (String) k));
        assertEquals("null", map.get("nullKey"));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(null, "value", (x, y) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(map, "key", null));
        RuntimeException biEx = assertThrows(RuntimeException.class, () -> Synchronized.accept(map, "key", (m, k) -> {
            throw new RuntimeException("BiConsumer exception");
        }));
        assertEquals("BiConsumer exception", biEx.getMessage());
    }

    @Test
    public void testStaticApply() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        assertEquals(2, Synchronized.apply(list, List::size).intValue());
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 10);
        assertEquals("Count is 10", Synchronized.apply(map, m -> "Count is " + m.get("count")));
        assertEquals("TEST", Synchronized.apply("test", String::toUpperCase));
        assertTrue(Synchronized.test(42, n -> n > 40));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(null, x -> "result"));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(list, null));
        RuntimeException applyEx = assertThrows(RuntimeException.class, () -> Synchronized.apply(list, l -> {
            throw new RuntimeException("Function exception");
        }));
        assertEquals("Function exception", applyEx.getMessage());

        assertEquals(15, Synchronized.apply(map, 5, (m, increment) -> m.merge("count", increment, Integer::sum)).intValue());
        assertEquals(15, map.get("count").intValue());
        assertEquals("null", Synchronized.apply(new HashMap<String, String>(), null, (m, value) -> value == null ? "null" : value));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(null, "value", (x, y) -> "result"));
        assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(map, 5, null));
        RuntimeException biEx = assertThrows(RuntimeException.class, () -> Synchronized.apply(map, 5, (m, increment) -> {
            throw new RuntimeException("BiFunction exception");
        }));
        assertEquals("BiFunction exception", biEx.getMessage());
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        List<Integer> list = new ArrayList<>();
        Synchronized<List<Integer>> sync = Synchronized.on(list);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            new Thread(() -> {
                sync.run(() -> list.add(value));
                latch.countDown();
            }).start();
        }
        latch.await();
        assertEquals(threadCount, list.size());

        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch callLatch = new CountDownLatch(threadCount);
        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                Integer result = Synchronized.on(counter).call(counter::incrementAndGet);
                synchronized (results) {
                    results.add(result);
                }
                callLatch.countDown();
            }).start();
        }
        callLatch.await();
        assertEquals(threadCount, results.size());
        assertEquals(threadCount, counter.get());

        StringBuilder sb = new StringBuilder();
        Synchronized<StringBuilder> syncSb = Synchronized.on(sb);
        CountDownLatch acceptLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            new Thread(() -> {
                syncSb.accept(s -> s.append(value).append(","));
                acceptLatch.countDown();
            }).start();
        }
        acceptLatch.await();
        assertEquals(threadCount * 2, sb.toString().length());

        Map<String, Integer> map = new HashMap<>();
        map.put("counter", 0);
        CountDownLatch mapLatch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
                Synchronized.on(map).accept(m -> m.put("counter", m.get("counter") + 1));
                mapLatch.countDown();
            }).start();
        }
        mapLatch.await();
        assertEquals(20, map.get("counter").intValue());

        AtomicInteger mixed = new AtomicInteger(0);
        Synchronized<AtomicInteger> syncMixed = Synchronized.on(mixed);
        CountDownLatch mixedLatch = new CountDownLatch(30);
        AtomicBoolean allPassed = new AtomicBoolean(true);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    syncMixed.run(mixed::incrementAndGet);
                } catch (Exception e) {
                    allPassed.set(false);
                }
                mixedLatch.countDown();
            }).start();
        }
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    syncMixed.call(mixed::incrementAndGet);
                } catch (Exception e) {
                    allPassed.set(false);
                }
                mixedLatch.countDown();
            }).start();
        }
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    syncMixed.accept(AtomicInteger::incrementAndGet);
                } catch (Exception e) {
                    allPassed.set(false);
                }
                mixedLatch.countDown();
            }).start();
        }
        mixedLatch.await();
        assertTrue(allPassed.get());
        assertEquals(30, mixed.get());

        List<Integer> staticList = new ArrayList<>();
        CountDownLatch staticLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            new Thread(() -> {
                Synchronized.run(staticList, () -> staticList.add(value));
                staticLatch.countDown();
            }).start();
        }
        staticLatch.await();
        assertEquals(threadCount, staticList.size());

        AtomicInteger staticCounter = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> Synchronized.run(staticCounter, () -> {
                int current = staticCounter.get();
                Thread.yield();
                staticCounter.set(current + 1);
            }));
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(10, staticCounter.get());

        Map<String, Integer> staticMap = new HashMap<>();
        staticMap.put("counter", 0);
        CountDownLatch staticAcceptLatch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
                Synchronized.accept(staticMap, m -> m.put("counter", m.get("counter") + 1));
                staticAcceptLatch.countDown();
            }).start();
        }
        staticAcceptLatch.await();
        assertEquals(20, staticMap.get("counter").intValue());
    }
}
