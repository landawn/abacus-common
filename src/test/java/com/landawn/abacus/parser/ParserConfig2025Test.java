package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ParserConfig2025Test extends TestBase {

    private TestParserConfig config;

    private static class TestParserConfig extends ParserConfig<TestParserConfig> {
        // Concrete implementation for testing
    }

    @BeforeEach
    public void setUp() {
        config = new TestParserConfig();
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
    public void test_getIgnoredPropNames_forClass_returnsGlobalWhenClassNotFound() {
        Set<String> globalProps = new HashSet<>();
        globalProps.add("field1");

        config.setIgnoredPropNames(globalProps);

        Collection<String> result = config.getIgnoredPropNames(String.class);
        assertNotNull(result);
        assertEquals(globalProps, result);
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

        // Shallow copy - they share the same map and sets
        assertEquals(config.getIgnoredPropNames(), copy.getIgnoredPropNames());
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
}
