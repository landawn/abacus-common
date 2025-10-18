package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class XMLDeserializationConfig2025Test extends TestBase {
    @Test
    public void test_ignoreUnmatchedProperty() {
        XMLDeserializationConfig config = new XMLDeserializationConfig();
        XMLDeserializationConfig result = config.ignoreUnmatchedProperty(true);
        assertSame(config, result);
        assertEquals(true, config.ignoreUnmatchedProperty());

        config.ignoreUnmatchedProperty(false);
        assertEquals(false, config.ignoreUnmatchedProperty());
    }

    @Test
    public void test_setIgnoredPropNames() {
        XMLDeserializationConfig config = new XMLDeserializationConfig();
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        props.add("prop2");
        ignoredPropNames.put(String.class, props);

        XMLDeserializationConfig result = config.setIgnoredPropNames(ignoredPropNames);
        assertSame(config, result);
        assertNotNull(config.getIgnoredPropNames());
    }
}
