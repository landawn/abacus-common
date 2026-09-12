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
        assertTrue(config.toString().contains("{class java.lang.Object=null}") || config.getIgnoredPropNames().toString().equals("{class java.lang.Object=null}"));

        config.setIgnoredPropNames((Map<Class<?>, Set<String>>) null);
        assertNull(config.getIgnoredPropNames());
        assertNull(config.getIgnoredPropNames(Integer.class));
    }

}
