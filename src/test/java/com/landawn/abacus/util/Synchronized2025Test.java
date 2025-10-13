package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Synchronized2025Test extends TestBase {

    @Test
    public void test_on_withValidMutex() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        assertNotNull(sync);
    }

    @Test
    public void test_on_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.on(null);
        });
    }

    @Test
    public void test_staticRun_withValidCommand() {
        List<String> list = new ArrayList<>();
        Synchronized.run(list, () -> list.add("test"));
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void test_staticRun_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.run(null, () -> {
            });
        });
    }

    @Test
    public void test_staticRun_withNullCommand() {
        List<String> list = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.run(list, null);
        });
    }

    @Test
    public void test_staticRun_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.run(list, () -> {
                throw new RuntimeException("Test exception");
            });
        });
        assertEquals("Test exception", exception.getMessage());
    }

    @Test
    public void test_staticRun_threadSafety() throws InterruptedException {
        List<Integer> list = new ArrayList<>();
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            new Thread(() -> {
                Synchronized.run(list, () -> list.add(value));
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount, list.size());
    }

    @Test
    public void test_staticCall_withValidCallable() {
        List<String> list = new ArrayList<>();
        list.add("item");
        String result = Synchronized.call(list, () -> list.get(0));
        assertEquals("item", result);
    }

    @Test
    public void test_staticCall_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.call(null, () -> "result");
        });
    }

    @Test
    public void test_staticCall_withNullCallable() {
        List<String> list = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.call(list, null);
        });
    }

    @Test
    public void test_staticCall_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.call(list, () -> {
                throw new RuntimeException("Call exception");
            });
        });
        assertEquals("Call exception", exception.getMessage());
    }

    @Test
    public void test_staticCall_returnsCorrectValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 42);
        Integer result = Synchronized.call(map, () -> {
            int value = map.get("count");
            map.put("count", value + 1);
            return value;
        });
        assertEquals(42, result);
        assertEquals(43, map.get("count").intValue());
    }

    @Test
    public void test_staticCall_threadSafety() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Integer> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                Integer result = Synchronized.call(counter, () -> counter.incrementAndGet());
                synchronized (results) {
                    results.add(result);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount, results.size());
        assertEquals(threadCount, counter.get());
    }

    @Test
    public void test_staticTest_withTruePredicate() {
        List<String> list = new ArrayList<>();
        list.add("item");
        boolean result = Synchronized.test(list, l -> !l.isEmpty());
        assertTrue(result);
    }

    @Test
    public void test_staticTest_withFalsePredicate() {
        List<String> list = new ArrayList<>();
        boolean result = Synchronized.test(list, l -> !l.isEmpty());
        assertFalse(result);
    }

    @Test
    public void test_staticTest_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.test(null, x -> true);
        });
    }

    @Test
    public void test_staticTest_withNullPredicate() {
        List<String> list = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.test(list, null);
        });
    }

    @Test
    public void test_staticTest_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.test(list, l -> {
                throw new RuntimeException("Predicate exception");
            });
        });
        assertEquals("Predicate exception", exception.getMessage());
    }

    @Test
    public void test_staticTestBi_withTruePredicate() {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 10);
        boolean result = Synchronized.test(map, 5, (m, threshold) -> m.get("count") > threshold);
        assertTrue(result);
    }

    @Test
    public void test_staticTestBi_withFalsePredicate() {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 3);
        boolean result = Synchronized.test(map, 5, (m, threshold) -> m.get("count") > threshold);
        assertFalse(result);
    }

    @Test
    public void test_staticTestBi_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.test(null, "value", (x, y) -> true);
        });
    }

    @Test
    public void test_staticTestBi_withNullPredicate() {
        Map<String, Integer> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.test(map, 5, null);
        });
    }

    @Test
    public void test_staticTestBi_withNullSecondArgument() {
        Map<String, Integer> map = new HashMap<>();
        boolean result = Synchronized.test(map, null, (m, threshold) -> threshold == null);
        assertTrue(result);
    }

    @Test
    public void test_staticTestBi_withExceptionThrown() {
        Map<String, Integer> map = new HashMap<>();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.test(map, 5, (m, threshold) -> {
                throw new RuntimeException("BiPredicate exception");
            });
        });
        assertEquals("BiPredicate exception", exception.getMessage());
    }

    @Test
    public void test_staticAccept_withValidConsumer() {
        StringBuilder sb = new StringBuilder();
        Synchronized.accept(sb, s -> s.append("Hello"));
        assertEquals("Hello", sb.toString());
    }

    @Test
    public void test_staticAccept_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.accept(null, x -> {
            });
        });
    }

    @Test
    public void test_staticAccept_withNullConsumer() {
        StringBuilder sb = new StringBuilder();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.accept(sb, null);
        });
    }

    @Test
    public void test_staticAccept_withExceptionThrown() {
        StringBuilder sb = new StringBuilder();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.accept(sb, s -> {
                throw new RuntimeException("Consumer exception");
            });
        });
        assertEquals("Consumer exception", exception.getMessage());
    }

    @Test
    public void test_staticAccept_mutatesObject() {
        List<String> list = new ArrayList<>();
        Synchronized.accept(list, l -> {
            l.add("first");
            l.add("second");
        });
        assertEquals(2, list.size());
        assertEquals("first", list.get(0));
        assertEquals("second", list.get(1));
    }

    @Test
    public void test_staticAcceptBi_withValidConsumer() {
        Map<String, String> map = new HashMap<>();
        Synchronized.accept(map, "key", (m, k) -> m.put(k, "value"));
        assertEquals("value", map.get("key"));
    }

    @Test
    public void test_staticAcceptBi_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.accept(null, "value", (x, y) -> {
            });
        });
    }

    @Test
    public void test_staticAcceptBi_withNullConsumer() {
        Map<String, String> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.accept(map, "key", null);
        });
    }

    @Test
    public void test_staticAcceptBi_withNullSecondArgument() {
        Map<String, String> map = new HashMap<>();
        Synchronized.accept(map, null, (m, k) -> {
            String value = (k == null) ? "null" : (String) k;
            m.put("nullKey", value);
        });
        assertEquals("null", map.get("nullKey"));
    }

    @Test
    public void test_staticAcceptBi_withExceptionThrown() {
        Map<String, String> map = new HashMap<>();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.accept(map, "key", (m, k) -> {
                throw new RuntimeException("BiConsumer exception");
            });
        });
        assertEquals("BiConsumer exception", exception.getMessage());
    }

    @Test
    public void test_staticApply_withValidFunction() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        Integer size = Synchronized.apply(list, l -> l.size());
        assertEquals(2, size.intValue());
    }

    @Test
    public void test_staticApply_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.apply(null, x -> "result");
        });
    }

    @Test
    public void test_staticApply_withNullFunction() {
        List<String> list = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.apply(list, null);
        });
    }

    @Test
    public void test_staticApply_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.apply(list, l -> {
                throw new RuntimeException("Function exception");
            });
        });
        assertEquals("Function exception", exception.getMessage());
    }

    @Test
    public void test_staticApply_transformsCorrectly() {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 10);
        String result = Synchronized.apply(map, m -> "Count is " + m.get("count"));
        assertEquals("Count is 10", result);
    }

    @Test
    public void test_staticApplyBi_withValidFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 10);
        Integer result = Synchronized.apply(map, 5, (m, increment) -> m.merge("count", increment, Integer::sum));
        assertEquals(15, result.intValue());
        assertEquals(15, map.get("count").intValue());
    }

    @Test
    public void test_staticApplyBi_withNullMutex() {
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.apply(null, "value", (x, y) -> "result");
        });
    }

    @Test
    public void test_staticApplyBi_withNullFunction() {
        Map<String, Integer> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            Synchronized.apply(map, 5, null);
        });
    }

    @Test
    public void test_staticApplyBi_withNullSecondArgument() {
        Map<String, String> map = new HashMap<>();
        String result = Synchronized.apply(map, null, (m, value) -> {
            return (value == null) ? "null" : (String) value;
        });
        assertEquals("null", result);
    }

    @Test
    public void test_staticApplyBi_withExceptionThrown() {
        Map<String, Integer> map = new HashMap<>();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            Synchronized.apply(map, 5, (m, increment) -> {
                throw new RuntimeException("BiFunction exception");
            });
        });
        assertEquals("BiFunction exception", exception.getMessage());
    }

    @Test
    public void test_instanceRun_withValidCommand() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        sync.run(() -> list.add("test"));
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void test_instanceRun_withNullCommand() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        assertThrows(IllegalArgumentException.class, () -> {
            sync.run(null);
        });
    }

    @Test
    public void test_instanceRun_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sync.run(() -> {
                throw new RuntimeException("Instance run exception");
            });
        });
        assertEquals("Instance run exception", exception.getMessage());
    }

    @Test
    public void test_instanceRun_multipleOperations() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        sync.run(() -> list.add("first"));
        sync.run(() -> list.add("second"));
        sync.run(() -> list.add("third"));
        assertEquals(3, list.size());
    }

    @Test
    public void test_instanceRun_threadSafety() throws InterruptedException {
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
    }

    @Test
    public void test_instanceCall_withValidCallable() {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 42);
        Synchronized<Map<String, Integer>> sync = Synchronized.on(map);
        Integer result = sync.call(() -> map.get("count"));
        assertEquals(42, result.intValue());
    }

    @Test
    public void test_instanceCall_withNullCallable() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        assertThrows(IllegalArgumentException.class, () -> {
            sync.call(null);
        });
    }

    @Test
    public void test_instanceCall_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sync.call(() -> {
                throw new RuntimeException("Instance call exception");
            });
        });
        assertEquals("Instance call exception", exception.getMessage());
    }

    @Test
    public void test_instanceCall_multipleOperations() {
        AtomicInteger counter = new AtomicInteger(0);
        Synchronized<AtomicInteger> sync = Synchronized.on(counter);
        Integer result1 = sync.call(() -> counter.incrementAndGet());
        Integer result2 = sync.call(() -> counter.incrementAndGet());
        Integer result3 = sync.call(() -> counter.incrementAndGet());
        assertEquals(1, result1.intValue());
        assertEquals(2, result2.intValue());
        assertEquals(3, result3.intValue());
    }

    @Test
    public void test_instanceCall_threadSafety() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Synchronized<AtomicInteger> sync = Synchronized.on(counter);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Integer> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                Integer result = sync.call(() -> counter.incrementAndGet());
                synchronized (results) {
                    results.add(result);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount, results.size());
        assertEquals(threadCount, counter.get());
    }

    @Test
    public void test_instanceTest_withTruePredicate() {
        List<String> list = new ArrayList<>();
        list.add("item");
        Synchronized<List<String>> sync = Synchronized.on(list);
        boolean result = sync.test(l -> !l.isEmpty());
        assertTrue(result);
    }

    @Test
    public void test_instanceTest_withFalsePredicate() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        boolean result = sync.test(l -> !l.isEmpty());
        assertFalse(result);
    }

    @Test
    public void test_instanceTest_withNullPredicate() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        assertThrows(IllegalArgumentException.class, () -> {
            sync.test(null);
        });
    }

    @Test
    public void test_instanceTest_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sync.test(l -> {
                throw new RuntimeException("Instance test exception");
            });
        });
        assertEquals("Instance test exception", exception.getMessage());
    }

    @Test
    public void test_instanceTest_multipleConditions() {
        Map<String, Integer> map = new HashMap<>();
        Synchronized<Map<String, Integer>> sync = Synchronized.on(map);

        assertFalse(sync.test(m -> m.containsKey("key")));
        map.put("key", 10);
        assertTrue(sync.test(m -> m.containsKey("key")));
        assertTrue(sync.test(m -> m.get("key") == 10));
    }

    @Test
    public void test_instanceAccept_withValidConsumer() {
        StringBuilder sb = new StringBuilder();
        Synchronized<StringBuilder> sync = Synchronized.on(sb);
        sync.accept(s -> s.append("Hello"));
        assertEquals("Hello", sb.toString());
    }

    @Test
    public void test_instanceAccept_withNullConsumer() {
        StringBuilder sb = new StringBuilder();
        Synchronized<StringBuilder> sync = Synchronized.on(sb);
        assertThrows(IllegalArgumentException.class, () -> {
            sync.accept(null);
        });
    }

    @Test
    public void test_instanceAccept_withExceptionThrown() {
        StringBuilder sb = new StringBuilder();
        Synchronized<StringBuilder> sync = Synchronized.on(sb);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sync.accept(s -> {
                throw new RuntimeException("Instance accept exception");
            });
        });
        assertEquals("Instance accept exception", exception.getMessage());
    }

    @Test
    public void test_instanceAccept_multipleOperations() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        sync.accept(l -> l.add("first"));
        sync.accept(l -> l.add("second"));
        sync.accept(l -> {
            l.add("third");
            l.add("fourth");
        });
        assertEquals(4, list.size());
    }

    @Test
    public void test_instanceAccept_threadSafety() throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        Synchronized<StringBuilder> sync = Synchronized.on(sb);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int value = i;
            new Thread(() -> {
                sync.accept(s -> s.append(value).append(","));
                latch.countDown();
            }).start();
        }

        latch.await();
        String result = sb.toString();
        assertEquals(threadCount * 2, result.length());
    }

    @Test
    public void test_instanceApply_withValidFunction() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        Synchronized<List<String>> sync = Synchronized.on(list);
        Integer size = sync.apply(l -> l.size());
        assertEquals(3, size.intValue());
    }

    @Test
    public void test_instanceApply_withNullFunction() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        assertThrows(IllegalArgumentException.class, () -> {
            sync.apply(null);
        });
    }

    @Test
    public void test_instanceApply_withExceptionThrown() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sync.apply(l -> {
                throw new RuntimeException("Instance apply exception");
            });
        });
        assertEquals("Instance apply exception", exception.getMessage());
    }

    @Test
    public void test_instanceApply_transformsCorrectly() {
        Map<String, Integer> map = new HashMap<>();
        map.put("count", 42);
        Synchronized<Map<String, Integer>> sync = Synchronized.on(map);
        String result = sync.apply(m -> "Count: " + m.get("count"));
        assertEquals("Count: 42", result);
    }

    @Test
    public void test_instanceApply_multipleOperations() {
        AtomicInteger counter = new AtomicInteger(0);
        Synchronized<AtomicInteger> sync = Synchronized.on(counter);
        Integer result1 = sync.apply(c -> c.incrementAndGet());
        Integer result2 = sync.apply(c -> c.incrementAndGet());
        String result3 = sync.apply(c -> "Value: " + c.get());
        assertEquals(1, result1.intValue());
        assertEquals(2, result2.intValue());
        assertEquals("Value: 2", result3);
    }

    @Test
    public void test_mixedStaticAndInstanceOperations() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);

        Synchronized.run(list, () -> list.add("static1"));
        Synchronized.accept(list, l -> l.add("static2"));

        sync.run(() -> list.add("instance1"));
        sync.accept(l -> l.add("instance2"));

        assertEquals(4, list.size());
        assertTrue(list.contains("static1"));
        assertTrue(list.contains("static2"));
        assertTrue(list.contains("instance1"));
        assertTrue(list.contains("instance2"));
    }

    @Test
    public void test_nestedSynchronizedOperations() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);

        sync.run(() -> {
            list.add("outer");
            sync.run(() -> list.add("inner"));
        });

        assertEquals(2, list.size());
        assertEquals("outer", list.get(0));
        assertEquals("inner", list.get(1));
    }

    @Test
    public void test_concurrentAccessWithStaticMethods() throws InterruptedException {
        Map<String, Integer> map = new HashMap<>();
        map.put("counter", 0);
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                Synchronized.accept(map, m -> {
                    int current = m.get("counter");
                    m.put("counter", current + 1);
                });
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount, map.get("counter").intValue());
    }

    @Test
    public void test_concurrentAccessWithInstanceMethods() throws InterruptedException {
        Map<String, Integer> map = new HashMap<>();
        map.put("counter", 0);
        Synchronized<Map<String, Integer>> sync = Synchronized.on(map);
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                sync.accept(m -> {
                    int current = m.get("counter");
                    m.put("counter", current + 1);
                });
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount, map.get("counter").intValue());
    }

    @Test
    public void test_returnNullFromCallable() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        String result = sync.call(() -> null);
        assertEquals(null, result);
    }

    @Test
    public void test_returnNullFromFunction() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);
        String result = sync.apply(l -> null);
        assertEquals(null, result);
    }

    @Test
    public void test_complexObjectMutation() {
        Map<String, List<Integer>> complexMap = new HashMap<>();
        complexMap.put("numbers", new ArrayList<>());
        Synchronized<Map<String, List<Integer>>> sync = Synchronized.on(complexMap);

        sync.accept(m -> {
            m.get("numbers").add(1);
            m.get("numbers").add(2);
            m.get("numbers").add(3);
        });

        Integer sum = sync.apply(m -> m.get("numbers").stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum.intValue());
    }

    @Test
    public void test_exceptionPreservedAcrossLayers() {
        List<String> list = new ArrayList<>();
        Synchronized<List<String>> sync = Synchronized.on(list);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sync.call(() -> {
                throw new RuntimeException("Deep exception");
            });
        });

        assertEquals("Deep exception", exception.getMessage());
    }

    @Test
    public void test_multipleThreadsWithDifferentOperations() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Synchronized<AtomicInteger> sync = Synchronized.on(counter);
        int threadCount = 30;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean allTestsPassed = new AtomicBoolean(true);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    sync.run(() -> counter.incrementAndGet());
                } catch (Exception e) {
                    allTestsPassed.set(false);
                }
                latch.countDown();
            }).start();
        }

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    sync.call(() -> counter.incrementAndGet());
                } catch (Exception e) {
                    allTestsPassed.set(false);
                }
                latch.countDown();
            }).start();
        }

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    sync.accept(c -> c.incrementAndGet());
                } catch (Exception e) {
                    allTestsPassed.set(false);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertTrue(allTestsPassed.get());
        assertEquals(30, counter.get());
    }

    @Test
    public void test_staticMethodsWithDifferentMutexTypes() {
        String str = "test";
        String result1 = Synchronized.apply(str, s -> s.toUpperCase());
        assertEquals("TEST", result1);

        Integer num = 42;
        boolean result2 = Synchronized.test(num, n -> n > 40);
        assertTrue(result2);

        StringBuilder sb = new StringBuilder("Hello");
        Synchronized.accept(sb, s -> s.append(" World"));
        assertEquals("Hello World", sb.toString());
    }

    @Test
    public void test_instanceMethodsWithDifferentMutexTypes() {
        AtomicReference<String> ref = new AtomicReference<>("initial");
        Synchronized<AtomicReference<String>> sync = Synchronized.on(ref);
        sync.accept(r -> r.set("updated"));
        assertEquals("updated", ref.get());

        AtomicBoolean flag = new AtomicBoolean(false);
        Synchronized<AtomicBoolean> syncFlag = Synchronized.on(flag);
        syncFlag.run(() -> flag.set(true));
        assertTrue(flag.get());
    }
}
