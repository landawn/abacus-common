package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ParserConfigTest extends TestBase {

    private TestParserConfig config;

    private static class TestParserConfig extends ParserConfig<TestParserConfig> {
        // Concrete implementation for testing
    }

    @BeforeEach
    public void setUp() {
        config = new TestParserConfig();
    }

    @Test
    public void test_getIgnoredPropNames_forClass_returnsClassSpecific() {
        Set<String> globalProps = new HashSet<>();
        globalProps.add("field1");
        config.setIgnoredPropNames(globalProps);

        Set<String> stringProps = new HashSet<>();
        stringProps.add("password");
        config.setIgnoredPropNames(String.class, stringProps);

        Collection<String> result = config.getIgnoredPropNames(String.class);
        assertEquals(stringProps, result);
    }

    @Test
    public void testGetIgnoredPropNamesFallbackToGlobal() {
        Set<String> globalProps = new HashSet<>();
        globalProps.add("globalProp");
        config.setIgnoredPropNames(globalProps);

        assertEquals(globalProps, config.getIgnoredPropNames(String.class));
    }

    @Test
    public void test_getIgnoredPropNames_defaultNull() {
        assertNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_getIgnoredPropNames_forClass_defaultNull() {
        assertNull(config.getIgnoredPropNames(String.class));
    }

    @Test
    public void test_getIgnoredPropNames_forClass_returnsGlobalWhenClassNotFound() {
        Set<String> globalProps = new HashSet<>();
        globalProps.add("field1");

        config.setIgnoredPropNames(globalProps);

        Collection<String> result = config.getIgnoredPropNames(String.class);
        assertNotNull(result);
        assertEquals(globalProps, result);
    }

    @Test
    public void testGetIgnoredPropNames() {
        assertNull(config.getIgnoredPropNames());

        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("prop1");
        config.setIgnoredPropNames(ignoredProps);

        assertNotNull(config.getIgnoredPropNames());
        assertTrue(config.getIgnoredPropNames().containsKey(Object.class));
    }

    @Test
    public void testGetIgnoredPropNamesForClass() {
        assertNull(config.getIgnoredPropNames(String.class));

        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("prop1");
        config.setIgnoredPropNames(String.class, ignoredProps);

        assertEquals(ignoredProps, config.getIgnoredPropNames(String.class));
        assertNull(config.getIgnoredPropNames(Integer.class));
    }

    @Test
    public void test_setIgnoredPropNames_withMap() {
        Map<Class<?>, Set<String>> map = new HashMap<>();

        Set<String> globalProps = new HashSet<>();
        globalProps.add("id");
        map.put(Object.class, globalProps);

        Set<String> stringProps = new HashSet<>();
        stringProps.add("password");
        map.put(String.class, stringProps);

        config.setIgnoredPropNames(map);

        assertEquals(map, config.getIgnoredPropNames());
    }

    @Test
    public void test_setIgnoredPropNames_methodChaining() {
        Set<String> props = new HashSet<>();
        props.add("field1");

        TestParserConfig result = config.setIgnoredPropNames(props);
        assertEquals(config, result);
    }

    @Test
    public void test_setIgnoredPropNames_forClass_methodChaining() {
        Set<String> props = new HashSet<>();
        props.add("field1");

        TestParserConfig result = config.setIgnoredPropNames(String.class, props);
        assertEquals(config, result);
    }

    @Test
    public void test_setIgnoredPropNames_withMap_methodChaining() {
        Map<Class<?>, Set<String>> map = new HashMap<>();
        TestParserConfig result = config.setIgnoredPropNames(map);
        assertEquals(config, result);
    }

    @Test
    public void test_setIgnoredPropNames_global() {
        Set<String> props = new HashSet<>();
        props.add("field1");
        props.add("field2");

        config.setIgnoredPropNames(props);

        assertNotNull(config.getIgnoredPropNames());
        assertTrue(config.getIgnoredPropNames().containsKey(Object.class));
        assertEquals(props, config.getIgnoredPropNames().get(Object.class));
    }

    @Test
    public void test_setIgnoredPropNames_forSpecificClass() {
        Set<String> props = new HashSet<>();
        props.add("password");

        config.setIgnoredPropNames(String.class, props);

        assertNotNull(config.getIgnoredPropNames());
        assertTrue(config.getIgnoredPropNames().containsKey(String.class));
        assertEquals(props, config.getIgnoredPropNames().get(String.class));
    }

    @Test
    public void test_multipleClassesWithIgnoredProps() {
        Set<String> stringProps = new HashSet<>();
        stringProps.add("password");

        Set<String> intProps = new HashSet<>();
        intProps.add("internalId");

        config.setIgnoredPropNames(String.class, stringProps);
        config.setIgnoredPropNames(Integer.class, intProps);

        assertEquals(stringProps, config.getIgnoredPropNames(String.class));
        assertEquals(intProps, config.getIgnoredPropNames(Integer.class));
    }

    @Test
    public void test_emptyIgnoredPropsSet() {
        Set<String> props = new HashSet<>();
        config.setIgnoredPropNames(props);

        assertNotNull(config.getIgnoredPropNames());
        assertTrue(config.getIgnoredPropNames().get(Object.class).isEmpty());
    }

    @Test
    public void testSetIgnoredPropNamesGlobal() {
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("globalProp");

        TestParserConfig result = config.setIgnoredPropNames(ignoredProps);
        assertSame(config, result);

        assertEquals(ignoredProps, config.getIgnoredPropNames(Object.class));
    }

    @Test
    public void testSetIgnoredPropNamesForClass() {
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("classProp");

        TestParserConfig result = config.setIgnoredPropNames(String.class, ignoredProps);
        assertSame(config, result);

        assertEquals(ignoredProps, config.getIgnoredPropNames(String.class));
    }

    @Test
    public void testSetIgnoredPropNamesMap() {
        Map<Class<?>, Set<String>> ignoredMap = new HashMap<>();
        Set<String> set1 = new HashSet<>();
        set1.add("prop1");
        Set<String> set2 = new HashSet<>();
        set2.add("prop2");

        ignoredMap.put(String.class, set1);
        ignoredMap.put(Integer.class, set2);

        TestParserConfig result = config.setIgnoredPropNames(ignoredMap);
        assertSame(config, result);

        assertEquals(set1, config.getIgnoredPropNames(String.class));
        assertEquals(set2, config.getIgnoredPropNames(Integer.class));
    }

    @Test
    public void test_copy_createsNewInstance() {
        TestParserConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
    }

    @Test
    public void test_copy_copiesIgnoredPropNames() {
        Set<String> props = new HashSet<>();
        props.add("field1");
        config.setIgnoredPropNames(props);

        TestParserConfig copy = config.copy();

        assertNotNull(copy.getIgnoredPropNames());
        assertEquals(config.getIgnoredPropNames(), copy.getIgnoredPropNames());
    }

    @Test
    public void test_copy_shallowCopy() {
        Set<String> props = new HashSet<>();
        props.add("field1");
        config.setIgnoredPropNames(props);

        TestParserConfig copy = config.copy();

        // The map is copied; the sets it holds are shared
        assertEquals(config.getIgnoredPropNames(), copy.getIgnoredPropNames());
        assertSame(props, copy.getIgnoredPropNames(Object.class));
    }

    @Test
    public void testCopy() {
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("prop1");
        config.setIgnoredPropNames(ignoredProps);

        TestParserConfig copy = config.copy();
        assertNotSame(config, copy);
        assertEquals(config.getIgnoredPropNames(), copy.getIgnoredPropNames());
    }

    // ---------------------------------------------------------------------------------------------
    // G09 fixes 2026-09-08: copy() owns its ignored-property map, whether or not one was set first
    // ---------------------------------------------------------------------------------------------

    @Test
    public void g09_copy_ignoredPropNameMapIsNotAliased() {
        final Set<String> global = new HashSet<>();
        global.add("version");
        config.setIgnoredPropNames(global);

        final TestParserConfig copy = config.copy();
        assertNotSame(config.getIgnoredPropNames(), copy.getIgnoredPropNames());
        assertEquals(config.getIgnoredPropNames(), copy.getIgnoredPropNames());

        // mutating the copy must not leak into the original ...
        copy.setIgnoredPropNames(String.class, new HashSet<>(Set.of("password")));
        assertNull(config.getIgnoredPropNames().get(String.class));
        assertEquals(global, config.getIgnoredPropNames(String.class));
        assertEquals(1, config.getIgnoredPropNames().size());

        // ... and mutating the original must not leak into the copy
        config.setIgnoredPropNames(Integer.class, new HashSet<>(Set.of("id")));
        assertNull(copy.getIgnoredPropNames().get(Integer.class));
        assertEquals(2, copy.getIgnoredPropNames().size());
    }

    @Test
    public void g09_copy_withoutIgnoredPropNames_lazyMapOnCopyDoesNotLeak() {
        // the leak used to depend on whether a setter had run before the copy
        final TestParserConfig copy = config.copy();
        assertNull(copy.getIgnoredPropNames());

        copy.setIgnoredPropNames(String.class, new HashSet<>(Set.of("password")));
        assertNull(config.getIgnoredPropNames());
        assertNotNull(copy.getIgnoredPropNames());
    }

    // ---------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06 (P8-11 javadoc pins: null / empty class-specific set, null global set)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void reviewFixes20260906_setIgnoredPropNames_classNull_fallsBackToGlobalSet() {
        final Set<String> global = new HashSet<>();
        global.add("version");
        config.setIgnoredPropNames(global);

        config.setIgnoredPropNames(String.class, null);
        // null is not an override: the global set applies again
        assertEquals(global, config.getIgnoredPropNames(String.class));
        assertEquals(global, config.getIgnoredPropNames(Integer.class));
        assertTrue(config.getIgnoredPropNames().containsKey(String.class));
        assertNull(config.getIgnoredPropNames().get(String.class));

        // and it removes the effect of a previous class-specific entry
        final Set<String> forString = new HashSet<>();
        forString.add("password");
        config.setIgnoredPropNames(String.class, forString);
        assertEquals(forString, config.getIgnoredPropNames(String.class));
        config.setIgnoredPropNames(String.class, null);
        assertEquals(global, config.getIgnoredPropNames(String.class));
    }

    @Test
    public void reviewFixes20260906_setIgnoredPropNames_classEmptySet_overridesGlobalSet() {
        final Set<String> global = new HashSet<>();
        global.add("version");
        config.setIgnoredPropNames(global);

        final Set<String> empty = new HashSet<>();
        config.setIgnoredPropNames(String.class, empty);

        final Collection<String> forString = config.getIgnoredPropNames(String.class);
        assertNotNull(forString);
        assertTrue(forString.isEmpty());
        assertEquals(global, config.getIgnoredPropNames(Integer.class));
    }

    @Test
    public void reviewFixes20260906_setIgnoredPropNames_globalNull_keepsEntry_mapNullClears() {
        assertNull(config.getIgnoredPropNames());

        config.setIgnoredPropNames((Set<String>) null);
        final Map<Class<?>, Set<String>> map = config.getIgnoredPropNames();
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(Object.class));
        assertNull(map.get(Object.class));
        assertNull(config.getIgnoredPropNames(Integer.class));
        assertTrue(
                config.toString().contains("{class java.lang.Object=null}") || config.getIgnoredPropNames().toString().equals("{class java.lang.Object=null}"));

        config.setIgnoredPropNames((Map<Class<?>, Set<String>>) null);
        assertNull(config.getIgnoredPropNames());
        assertNull(config.getIgnoredPropNames(Integer.class));
    }

    // ---- perf review 2026-10-07 G17 begin ----

    // G17-01: ConfigMap.snapshot() no longer re-stores the shared flag once it is set; every copy-on-write
    // transition around that flag is pinned: first snapshot, repeated snapshots of an already shared map,
    // snapshot of a snapshot, writes (put/putAll/remove) on the source and on each copy, clear() re-arming
    // the flag, and snapshots of a caller-owned map.
    @Test
    public void testCopyConfigMap_repeatedSnapshotsStayIsolated() {
        final Map<String, Integer> source = ParserConfig.newConfigMap();
        source.put("a", 1);
        source.put("b", 2);

        final Map<String, Integer> first = ParserConfig.copyConfigMap(source);
        final Map<String, Integer> second = ParserConfig.copyConfigMap(source); // the source is already shared here
        final Map<String, Integer> third = ParserConfig.copyConfigMap(first); // snapshot of a snapshot
        assertEquals(source, first);
        assertEquals(source, second);
        assertEquals(source, third);
        assertEquals(2, third.size());

        source.put("a", 10); // the source detaches, the copies keep the old value
        assertEquals(Integer.valueOf(10), source.get("a"));
        assertEquals(Integer.valueOf(1), first.get("a"));
        assertEquals(Integer.valueOf(1), second.get("a"));
        assertEquals(Integer.valueOf(1), third.get("a"));

        first.remove("b"); // the first copy detaches
        assertNull(first.get("b"));
        assertTrue(second.containsKey("b"));
        assertEquals(Integer.valueOf(2), third.get("b"));
        assertEquals(Integer.valueOf(2), source.get("b"));

        final Map<String, Integer> more = new HashMap<>();
        more.put("c", 3);
        second.putAll(more); // the second copy detaches
        assertEquals(Integer.valueOf(3), second.get("c"));
        assertNull(third.get("c"));
        assertNull(source.get("c"));
        assertNull(first.get("c"));

        third.put("d", 4); // the last holder of the original storage detaches too
        assertEquals(Integer.valueOf(4), third.get("d"));
        assertNull(source.get("d"));
        assertNull(first.get("d"));
        assertNull(second.get("d"));

        final Map<String, Integer> fourth = ParserConfig.copyConfigMap(source); // the source shares again
        source.put("e", 5);
        assertNull(fourth.get("e"));
        assertEquals(Integer.valueOf(5), source.get("e"));
        fourth.put("f", 6);
        assertNull(source.get("f"));
        assertEquals(Integer.valueOf(10), fourth.get("a"));

        source.clear(); // clear() re-arms the flag: the cleared map is private again
        assertTrue(source.isEmpty());
        assertEquals(Integer.valueOf(10), fourth.get("a"));
        final Map<String, Integer> fifth = ParserConfig.copyConfigMap(source);
        source.put("g", 7);
        assertTrue(fifth.isEmpty());
        assertEquals(1, source.size());
        fifth.put("h", 8);
        assertNull(source.get("h"));
        assertEquals(Integer.valueOf(8), fifth.get("h"));

        final Map<String, Integer> alias = new HashMap<>(); // a caller-owned map is really copied, both ways
        alias.put("x", 1);
        final Map<String, Integer> ofAlias = ParserConfig.copyConfigMap(alias);
        alias.put("y", 2);
        assertNull(ofAlias.get("y"));
        ofAlias.put("z", 3);
        assertNull(alias.get("z"));
        final Map<String, Integer> ofOfAlias = ParserConfig.copyConfigMap(ofAlias);
        ofAlias.put("x", 100);
        assertEquals(Integer.valueOf(1), ofOfAlias.get("x"));
        assertEquals(Integer.valueOf(100), ofAlias.get("x"));
    }

    // G17-01: the same transitions through the public configuration API (value types and property handlers
    // of a JsonDeserConfig copied repeatedly, as the JSON reader does for every nested generic container).
    @Test
    public void testCopy_repeatedConfigCopiesStayIsolated() {
        final com.landawn.abacus.util.function.BiConsumer<Collection<Object>, Object> firstHandler = Collection::add;
        final com.landawn.abacus.util.function.BiConsumer<Collection<Object>, Object> secondHandler = (c, e) -> c.clear();
        final JsonDeserConfig original = new JsonDeserConfig().setValueType("v", String.class).setPropHandler("h", firstHandler);

        final JsonDeserConfig copy1 = original.copy();
        final JsonDeserConfig copy2 = original.copy();
        final JsonDeserConfig copy3 = copy1.copy();
        assertEquals(original, copy1);
        assertEquals(original, copy2);
        assertEquals(original, copy3);

        original.setValueType("v", Integer.class).setPropHandler("h", secondHandler);
        copy1.setValueType("w", Long.class);
        copy2.setPropHandler("i", secondHandler);

        assertEquals(Integer.class, original.getValueType("v").javaType());
        assertEquals(String.class, copy1.getValueType("v").javaType());
        assertEquals(String.class, copy2.getValueType("v").javaType());
        assertEquals(String.class, copy3.getValueType("v").javaType());
        assertEquals(Long.class, copy1.getValueType("w").javaType());
        assertNull(original.getValueType("w"));
        assertNull(copy2.getValueType("w"));
        assertNull(copy3.getValueType("w"));
        assertSame(secondHandler, original.getPropHandler("h"));
        assertSame(firstHandler, copy1.getPropHandler("h"));
        assertSame(firstHandler, copy2.getPropHandler("h"));
        assertSame(firstHandler, copy3.getPropHandler("h"));
        assertSame(secondHandler, copy2.getPropHandler("i"));
        assertNull(original.getPropHandler("i"));
        assertNull(copy3.getPropHandler("i"));

        final JsonDeserConfig copy4 = original.copy(); // copies taken after the original detached
        final JsonDeserConfig copy5 = original.copy();
        original.setValueType("v", Double.class);
        copy4.setValueType("v", Float.class);
        assertEquals(Double.class, original.getValueType("v").javaType());
        assertEquals(Float.class, copy4.getValueType("v").javaType());
        assertEquals(Integer.class, copy5.getValueType("v").javaType());
        assertSame(secondHandler, copy5.getPropHandler("h"));
    }

    // G17-01: a configuration shared read-only by several parsing threads (each parse snapshots its maps for
    // every nested container) keeps producing the configured results and is left unchanged.
    @Test
    public void testCopy_sharedConfigUnderConcurrentParsesStaysIntact() throws Exception {
        final JsonDeserConfig shared = new JsonDeserConfig().setValueType("tags", "List<Integer>").setValueType("unused", String.class);
        final JsonParser parser = ParserFactory.createJsonParser();
        final String json = "{\"name\":\"n\",\"tags\":[1,2,3],\"nested\":{\"tags\":[4,5]}}";
        final int threads = 4;
        final int iterations = 500;
        final Object[] results = new Object[threads];
        final Thread[] workers = new Thread[threads];

        for (int t = 0; t < threads; t++) {
            final int slot = t;
            workers[t] = new Thread(() -> {
                Object last = null;
                for (int i = 0; i < iterations; i++) {
                    last = parser.deserialize(json, shared, Map.class);
                }
                results[slot] = last;
            });
            workers[t].start();
        }

        for (final Thread worker : workers) {
            worker.join();
        }

        final Map<String, Object> expected = parser.deserialize(json, shared, Map.class);
        assertEquals(java.util.List.of(1, 2, 3), expected.get("tags"));
        assertEquals(java.util.List.of(4, 5), ((Map<?, ?>) expected.get("nested")).get("tags"));
        for (final Object result : results) {
            assertEquals(expected, result);
        }
        assertEquals(Integer.class, shared.getValueType("tags").elementType().javaType());
        assertEquals(String.class, shared.getValueType("unused").javaType());
        assertNull(shared.getValueType("name"));
    }

    // ---- perf review 2026-10-07 G17 end ----

}
