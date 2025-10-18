package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ParserConfig100Test extends TestBase {

    private TestParserConfig config;

    private static class TestParserConfig extends ParserConfig<TestParserConfig> {
    }

    @BeforeEach
    public void setUp() {
        config = new TestParserConfig();
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
    public void testCopy() {
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("prop1");
        config.setIgnoredPropNames(ignoredProps);

        TestParserConfig copy = config.copy();
        assertNotSame(config, copy);
        assertEquals(config.getIgnoredPropNames(), copy.getIgnoredPropNames());
    }

    @Test
    public void testGetIgnoredPropNamesFallbackToGlobal() {
        Set<String> globalProps = new HashSet<>();
        globalProps.add("globalProp");
        config.setIgnoredPropNames(globalProps);

        assertEquals(globalProps, config.getIgnoredPropNames(String.class));
    }
}
