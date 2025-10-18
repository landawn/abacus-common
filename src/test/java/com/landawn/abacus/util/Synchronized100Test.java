package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Synchronized100Test extends TestBase {

    @Test
    public void testOn() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> syncList = Synchronized.on(list);
        Assertions.assertNotNull(syncList);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.on(null));
    }

    @Test
    public void testStaticRun() throws Exception {
        List<String> list = new ArrayList<>();

        Synchronized.run(list, () -> {
            list.add("item1");
            list.add("item2");
        });

        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.contains("item1"));
        Assertions.assertTrue(list.contains("item2"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.run(null, () -> {
        }));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.run(list, null));
    }

    @Test
    public void testStaticCall() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 0);

        Integer result = Synchronized.call(map, () -> {
            int current = map.get("count");
            map.put("count", current + 1);
            return current;
        });

        Assertions.assertEquals(0, result);
        Assertions.assertEquals(1, map.get("count"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.call(null, () -> 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.call(map, null));
    }

    @Test
    public void testStaticTestPredicate() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("item");

        boolean result = Synchronized.test(list, l -> !l.isEmpty());
        Assertions.assertTrue(result);

        result = Synchronized.test(list, l -> l.size() > 5);
        Assertions.assertFalse(result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.test(null, l -> true));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.test(list, null));
    }

    @Test
    public void testStaticTestBiPredicate() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 5);

        boolean result = Synchronized.test(map, 3, (m, threshold) -> m.get("count") > threshold);
        Assertions.assertTrue(result);

        result = Synchronized.test(map, 10, (m, threshold) -> m.get("count") > threshold);
        Assertions.assertFalse(result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.test(null, 3, (m, t) -> true));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.test(map, 3, null));
    }

    @Test
    public void testStaticAcceptConsumer() throws Exception {
        StringBuilder sb = new StringBuilder();

        Synchronized.accept(sb, s -> s.append("Hello").append(" World"));
        Assertions.assertEquals("Hello World", sb.toString());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(null, s -> {
        }));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(sb, null));
    }

    @Test
    public void testStaticAcceptBiConsumer() throws Exception {
        Map<String, String> map = new HashMap<>();

        Synchronized.accept(map, "key", (m, k) -> m.put(k, "value"));
        Assertions.assertEquals("value", map.get("key"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(null, "key", (m, k) -> {
        }));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.accept(map, "key", null));
    }

    @Test
    public void testStaticApplyFunction() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        Integer size = Synchronized.apply(list, l -> l.size());
        Assertions.assertEquals(3, size);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(null, l -> 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(list, null));
    }

    @Test
    public void testStaticApplyBiFunction() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 10);

        Integer result = Synchronized.apply(map, 5, (m, increment) -> m.merge("count", increment, Integer::sum));
        Assertions.assertEquals(15, result);
        Assertions.assertEquals(15, map.get("count"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(null, 5, (m, i) -> 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Synchronized.apply(map, 5, null));
    }

    @Test
    public void testInstanceRun() throws Exception {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> syncList = Synchronized.on(list);

        syncList.run(() -> {
            list.add("item1");
            list.add("item2");
        });

        Assertions.assertEquals(2, list.size());

        Assertions.assertThrows(IllegalArgumentException.class, () -> syncList.run(null));
    }

    @Test
    public void testInstanceCall() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("value", 42);
        Synchronized<Map<String, Integer>> syncMap = Synchronized.on(map);

        Integer result = syncMap.call(() -> map.get("value"));
        Assertions.assertEquals(42, result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> syncMap.call(null));
    }

    @Test
    public void testInstanceTestPredicate() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("test");
        Synchronized<List<String>> syncList = Synchronized.on(list);

        boolean result = syncList.test(l -> l.contains("test"));
        Assertions.assertTrue(result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> syncList.test(null));
    }

    @Test
    public void testInstanceAccept() throws Exception {
        StringBuilder sb = new StringBuilder();
        Synchronized<StringBuilder> syncSb = Synchronized.on(sb);

        syncSb.accept(s -> s.append("test"));
        Assertions.assertEquals("test", sb.toString());

        Assertions.assertThrows(IllegalArgumentException.class, () -> syncSb.accept(null));
    }

    @Test
    public void testInstanceApply() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 10);
        Synchronized<Map<String, Integer>> syncMap = Synchronized.on(map);

        Integer doubled = syncMap.apply(m -> m.get("count") * 2);
        Assertions.assertEquals(20, doubled);

        Assertions.assertThrows(IllegalArgumentException.class, () -> syncMap.apply(null));
    }

    @Test
    public void testConcurrentAccess() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                try {
                    Synchronized.run(counter, () -> {
                        int current = counter.get();
                        Thread.yield();
                        counter.set(current + 1);
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Assertions.assertEquals(10, counter.get());
    }

    @Test
    public void testExceptionPropagation() {
        List<String> list = new ArrayList<>();

        Assertions.assertThrows(RuntimeException.class, () -> {
            Synchronized.run(list, () -> {
                throw new RuntimeException("Test exception");
            });
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            Synchronized.call(list, () -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testComplexOperations() throws Exception {
        Map<String, List<Integer>> complexMap = new HashMap<>();
        Synchronized<Map<String, List<Integer>>> syncMap = Synchronized.on(complexMap);

        syncMap.run(() -> {
            complexMap.computeIfAbsent("numbers", k -> new ArrayList<>());
            complexMap.get("numbers").add(1);
            complexMap.get("numbers").add(2);
            complexMap.get("numbers").add(3);
        });

        Integer sum = syncMap.call(() -> {
            return complexMap.get("numbers").stream().mapToInt(Integer::intValue).sum();
        });

        Assertions.assertEquals(6, sum);
    }
}
