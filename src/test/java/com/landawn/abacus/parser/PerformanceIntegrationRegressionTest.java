package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.exception.ParsingException;
import performanceintegration.PerformanceIntegrationXmlModel;

@Tag("unit")
class PerformanceIntegrationRegressionTest {
    public static class Bean {
        private int keep;
        public int getKeep() { return keep; }
        public void setKeep(int value) { keep = value; }
    }

    public static class CountingMap extends HashMap<Object, Object> {
        static int insertions;
        @Override public Object put(Object key, Object value) {
            insertions++;
            return super.put(key, value);
        }
    }

    public static class CheckedList extends ArrayList<Object> {
        static int sizeChecks;
        @Override public int size() {
            sizeChecks++;
            return super.size();
        }
    }

    @Test
    void discardedMapsReadAnOverriddenFactorySettingOnlyOnce() {
        AtomicInteger calls = new AtomicInteger();
        JsonDeserConfig config = new JsonDeserConfig() {
            @Override public Class<? extends Map> getMapInstanceType() {
                return calls.incrementAndGet() == 1 ? CountingMap.class : HashMap.class;
            }
        };
        CountingMap.insertions = 0;
        JsonParser parser = new JsonParserImpl(null, config);
        assertEquals(7, parser.deserialize("{\"discard\":{\"a\":1,\"b\":2},\"keep\":7}", Bean.class).getKeep());
        assertEquals(1, calls.get(), "An overridden map factory getter must not be evaluated again for the discard optimization");
        assertEquals(2, CountingMap.insertions, "The factory selected by the callback must still receive the entries");
    }

    @Test
    void discardedTypedValuesStillRunConfiguredEmptinessChecks() {
        JsonDeserConfig config = JsonDeserConfig.create().setIgnoreNullOrEmpty(true).setValueType("values", CheckedList.class);
        JsonParser parser = new JsonParserImpl(null, config);
        CheckedList.sizeChecks = 0;
        assertEquals(7, parser.deserialize("{\"discard\":{\"values\":[1,2]},\"keep\":7}", Bean.class).getKeep());
        assertTrue(CheckedList.sizeChecks > 0, "A discarded typed collection must retain its configured emptiness check");

        config = JsonDeserConfig.create().setIgnoreNullOrEmpty(true).setElementType(CheckedList.class);
        parser = new JsonParserImpl(null, config);
        CheckedList.sizeChecks = 0;
        assertEquals(7, parser.deserialize(new StringReader("{\"discard\":[[1,2]],\"keep\":7}"), Bean.class).getKeep());
        assertTrue(CheckedList.sizeChecks > 0, "Typed discarded array elements must retain their configured emptiness check");
    }

    @Test
    void discardedSubtreesKeepSyntaxAndScalarConversionRules() {
        JsonParser parser = new JsonParserImpl();
        for (String subtree : List.of("[1,{\"a\":[true,null,\"escaped\\ntext\"]},[],{}]", "{\"a\":{\"b\":[1,2,3]}}", "[,,1,]")) {
            String json = "{\"discard\":" + subtree + ",\"keep\":7}";
            assertEquals(7, parser.deserialize(json, Bean.class).getKeep());
            assertEquals(7, parser.deserialize(new StringReader(json), Bean.class).getKeep());
        }
        for (String subtree : List.of("[1 2]", "{\"a\":1 \"b\":2}", "[{}{}]", "{\"a\":}", "[1", "{\"a\":[1}")) {
            assertThrows(RuntimeException.class, () -> parser.deserialize("{\"discard\":" + subtree + ",\"keep\":7}", Bean.class), subtree);
        }
        JsonParser typed = new JsonParserImpl(null, JsonDeserConfig.create().setElementType(Integer.class));
        assertThrows(RuntimeException.class, () -> typed.deserialize("{\"discard\":[\"not-an-int\"]}", Bean.class));
        JsonParser typedValues = new JsonParserImpl(null, JsonDeserConfig.create().setValueType("number", Integer.class));
        assertThrows(RuntimeException.class, () -> typedValues.deserialize("{\"discard\":{\"number\":\"bad\"}}", Bean.class));
        // Type-specific braced conversion must still reject a structured value for this scalar.
        assertThrows(RuntimeException.class, () -> typedValues.deserialize("{\"discard\":{\"number\":{\"value\":1}}}", Bean.class));
        assertThrows(ParsingException.class,
                () -> parser.deserialize("{\"discard\":" + "[".repeat(300) + "0" + "]".repeat(300) + "}", Bean.class));
        assertEquals(2, parser.deserialize("{\"keep\":2}", Bean.class).getKeep());
    }

    @Test
    void discardedSubtreesPreserveHandlersAndCustomMaps() {
        AtomicReference<Collection<Object>> captured = new AtomicReference<>();
        JsonDeserConfig config = JsonDeserConfig.create().setPropHandler("discard", (Collection<Object> collection, Object value) -> {
            collection.add(value);
            captured.set(collection);
        });
        new JsonParserImpl().deserialize("{\"discard\":[{\"a\":[1,2]},3],\"keep\":1}", config, Bean.class);
        assertEquals(List.of(Map.of("a", List.of(1, 2)), 3), new ArrayList<>(captured.get()));

        JsonDeserConfig nested = JsonDeserConfig.create().setPropHandler("values", (Collection<Object> collection, Object value) -> {
            collection.add(value);
            captured.set(collection);
        });
        new JsonParserImpl(null, nested).deserialize("{\"discard\":{\"values\":[{\"a\":1},2]}}", Bean.class);
        assertEquals(List.of(Map.of("a", 1), 2), new ArrayList<>(captured.get()));

        CountingMap.insertions = 0;
        JsonParser customMaps = new JsonParserImpl(null, JsonDeserConfig.create().setMapInstanceType(CountingMap.class));
        customMaps.deserialize("{\"discard\":{\"a\":1,\"b\":{\"c\":2}}}", Bean.class);
        assertEquals(3, CountingMap.insertions);
    }

    @Test
    void discardedSubtreeAllocationDoesNotRetainContainerStorage() {
        var bean = ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        var mx = (com.sun.management.ThreadMXBean) bean;
        org.junit.jupiter.api.Assumptions.assumeTrue(mx.isThreadAllocatedMemorySupported());
        mx.setThreadAllocatedMemoryEnabled(true);
        JsonParser parser = new JsonParserImpl();
        String json = "{\"discard\":[" + "null,".repeat(49_999) + "null],\"keep\":9}";
        for (int i = 0; i < 8; i++) parser.deserialize(json, Bean.class);
        long start = mx.getThreadAllocatedBytes(Thread.currentThread().threadId());
        assertEquals(9, parser.deserialize(json, Bean.class).getKeep());
        long allocated = mx.getThreadAllocatedBytes(Thread.currentThread().threadId()) - start;
        // JsonStringReader owns a char[] copy of the input; this bound excludes another growing element array.
        assertTrue(allocated < 2L * json.length() + 100_000, "Discarded null array allocated " + allocated + " bytes");
    }

    @Test
    void xmlClassCacheHitsDoNotWaitForTheInsertionLock() throws Exception {
        final ClassValue<?> cache = xmlClassCache();
        final Class<?> target = PerformanceIntegrationXmlModel.Inner.class;
        cache.remove(target);
        final Method resolve = xmlClassResolver();
        assertSame(target, resolve.invoke(null, "inner", target));
        assertNull(resolve.invoke(null, "unknown", target));
        final Map<?, ?> names = (Map<?, ?>) cache.get(target);
        final CountDownLatch started = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            // Both positive and negative hits must progress while a different thread owns the write lock.
            synchronized (names) {
                final Future<?> hit = executor.submit(() -> {
                    started.countDown();
                    assertSame(target, resolve.invoke(null, "inner", target));
                    assertNull(resolve.invoke(null, "unknown", target));
                    return null;
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                hit.get(5, TimeUnit.SECONDS);
            }
        } finally {
            cache.remove(target);
        }
    }

    @Test
    void xmlClassCacheStaysBoundedDuringConcurrentMissesAndRecoversClearedValues() throws Exception {
        final ClassValue<?> cache = xmlClassCache();
        final Class<?> target = PerformanceIntegrationXmlModel.Inner.class;
        cache.remove(target);
        final Method resolve = xmlClassResolver();
        final Map<?, ?> names = (Map<?, ?>) cache.get(target);
        final CountDownLatch ready = new CountDownLatch(8);
        final CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            // Exercise the capacity boundary immediately after concurrent writers are released.
            for (int i = 0; i < 256; i++) {
                assertNull(resolve.invoke(null, "prefill" + i, target));
            }
            final List<Future<?>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                final int workerId = worker;
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int i = 0; i < 150; i++) {
                        assertNull(resolve.invoke(null, "unknown" + workerId + "x" + i, target));
                        assertSame(target, resolve.invoke(null, "inner", target));
                        assertTrue(names.size() <= 256, "Input-driven names exceeded the cache bound");
                    }
                    return null;
                }));
            }
            try {
                assertTrue(ready.await(5, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            for (Future<?> task : tasks) {
                task.get(30, TimeUnit.SECONDS);
            }
            assertSame(target, resolve.invoke(null, "inner", target));
            assertEquals(256, names.size());
            ((WeakReference<?>) names.get("inner")).clear();
            assertSame(target, resolve.invoke(null, "inner", target));
            assertEquals(256, names.size(), "Refreshing an existing weak entry must not evict another name");
            assertSame(target, ((WeakReference<?>) names.get("inner")).get());
        } finally {
            cache.remove(target);
        }
    }

    private static ClassValue<?> xmlClassCache() throws Exception {
        final Field field = AbacusXmlParserImpl.class.getDeclaredField("nodeNameClassMapPool");
        field.setAccessible(true);
        return (ClassValue<?>) field.get(null);
    }

    private static Method xmlClassResolver() throws Exception {
        final Method method = AbacusXmlParserImpl.class.getDeclaredMethod("getClassByNodeName", String.class, Class.class);
        method.setAccessible(true);
        return method;
    }

    @Test
    void invalidXmlWrapperNamesHaveABoundedPerClassCache() throws Exception {
        XmlParser parser = ParserFactory.createAbacusXmlParser();
        for (int i = 0; i < 600; i++) {
            String xml = "<outer><child><unknown" + i + ">text</unknown" + i + "></child></outer>";
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, PerformanceIntegrationXmlModel.Outer.class));
        }
        Field field = AbacusXmlParserImpl.class.getDeclaredField("nodeNameClassMapPool");
        field.setAccessible(true);
        ClassValue<?> cache = (ClassValue<?>) field.get(null);
        Map<?, ?> names = (Map<?, ?>) cache.get(PerformanceIntegrationXmlModel.Inner.class);
        assertTrue(names.size() <= 256, "Cached " + names.size() + " input names");
        assertEquals("ok", parser.deserialize("<outer><child><inner><name>ok</name></inner></child></outer>", PerformanceIntegrationXmlModel.Outer.class)
                .getChild().getName());
    }
}
