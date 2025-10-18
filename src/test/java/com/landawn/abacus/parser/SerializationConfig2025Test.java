package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SerializationConfig2025Test extends TestBase {

    private TestSerializationConfig config;

    private static class TestSerializationConfig extends SerializationConfig<TestSerializationConfig> {
        // Concrete implementation for testing
    }

    @BeforeEach
    public void setUp() {
        config = new TestSerializationConfig();
    }

    @Test
    public void test_getExclusion_defaultValue() {
        assertNull(config.getExclusion());
    }

    @Test
    public void test_setExclusion_withNull() {
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    @Test
    public void test_setExclusion_withDefault() {
        config.setExclusion(Exclusion.DEFAULT);
        assertEquals(Exclusion.DEFAULT, config.getExclusion());
    }

    @Test
    public void test_setExclusion_withNone() {
        config.setExclusion(Exclusion.NONE);
        assertEquals(Exclusion.NONE, config.getExclusion());
    }

    @Test
    public void test_setExclusion_withNullValue() {
        config.setExclusion(Exclusion.NULL);
        config.setExclusion(null);
        assertNull(config.getExclusion());
    }

    @Test
    public void test_setExclusion_methodChaining() {
        TestSerializationConfig result = config.setExclusion(Exclusion.NULL);
        assertEquals(config, result);
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    @Test
    public void test_skipTransientField_defaultValue() {
        assertTrue(config.skipTransientField());
    }

    @Test
    public void test_skipTransientField_setTrue() {
        config.skipTransientField(true);
        assertTrue(config.skipTransientField());
    }

    @Test
    public void test_skipTransientField_setFalse() {
        config.skipTransientField(false);
        assertFalse(config.skipTransientField());
    }

    @Test
    public void test_skipTransientField_methodChaining() {
        TestSerializationConfig result = config.skipTransientField(false);
        assertEquals(config, result);
        assertFalse(config.skipTransientField());
    }

    @Test
    public void test_skipTransientField_toggleValue() {
        config.skipTransientField(false);
        assertFalse(config.skipTransientField());

        config.skipTransientField(true);
        assertTrue(config.skipTransientField());
    }

    @Test
    public void test_hashCode_defaultConfig() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        TestSerializationConfig config2 = new TestSerializationConfig();

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_hashCode_withExclusion() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.setExclusion(Exclusion.NULL);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.setExclusion(Exclusion.NULL);

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_hashCode_withDifferentExclusion() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.setExclusion(Exclusion.NULL);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.setExclusion(Exclusion.DEFAULT);

        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_hashCode_withSkipTransientField() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.skipTransientField(false);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.skipTransientField(false);

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_hashCode_withDifferentSkipTransientField() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.skipTransientField(true);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.skipTransientField(false);

        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_equals_sameInstance() {
        assertTrue(config.equals(config));
    }

    @Test
    public void test_equals_defaultConfigs() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        TestSerializationConfig config2 = new TestSerializationConfig();

        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
    }

    @Test
    public void test_equals_withSameExclusion() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.setExclusion(Exclusion.NULL);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.setExclusion(Exclusion.NULL);

        assertTrue(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentExclusion() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.setExclusion(Exclusion.NULL);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.setExclusion(Exclusion.DEFAULT);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withSameSkipTransientField() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.skipTransientField(false);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.skipTransientField(false);

        assertTrue(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentSkipTransientField() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.skipTransientField(true);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.skipTransientField(false);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withIgnoredPropNames() {
        Set<String> props = new HashSet<>();
        props.add("field1");
        props.add("field2");

        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.setIgnoredPropNames(props);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.setIgnoredPropNames(props);

        assertTrue(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentIgnoredPropNames() {
        Set<String> props1 = new HashSet<>();
        props1.add("field1");

        Set<String> props2 = new HashSet<>();
        props2.add("field2");

        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.setIgnoredPropNames(props1);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.setIgnoredPropNames(props2);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withNull() {
        assertFalse(config.equals(null));
    }

    @Test
    public void test_equals_withDifferentType() {
        assertFalse(config.equals("not a config"));
        assertFalse(config.equals(Integer.valueOf(42)));
    }

    @Test
    public void test_equals_complexScenario() {
        Set<String> props = new HashSet<>();
        props.add("field1");

        TestSerializationConfig config1 = new TestSerializationConfig();
        config1.setExclusion(Exclusion.NULL);
        config1.skipTransientField(false);
        config1.setIgnoredPropNames(props);

        TestSerializationConfig config2 = new TestSerializationConfig();
        config2.setExclusion(Exclusion.NULL);
        config2.skipTransientField(false);
        config2.setIgnoredPropNames(props);

        assertTrue(config1.equals(config2));
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString_defaultConfig() {
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("ignoredPropNames"));
        assertTrue(result.contains("exclusion"));
        assertTrue(result.contains("skipTransientField"));
    }

    @Test
    public void test_toString_withExclusion() {
        config.setExclusion(Exclusion.NULL);
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("NULL"));
    }

    @Test
    public void test_toString_withSkipTransientFieldFalse() {
        config.skipTransientField(false);
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("false"));
    }

    @Test
    public void test_toString_withIgnoredPropNames() {
        Set<String> props = new HashSet<>();
        props.add("field1");
        props.add("field2");
        config.setIgnoredPropNames(props);

        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("field1") || result.contains("field2"));
    }

    @Test
    public void test_copy_createsNewInstance() {
        TestSerializationConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
    }

    @Test
    public void test_copy_copiesExclusion() {
        config.setExclusion(Exclusion.NULL);
        TestSerializationConfig copy = config.copy();
        assertEquals(Exclusion.NULL, copy.getExclusion());
    }

    @Test
    public void test_copy_copiesSkipTransientField() {
        config.skipTransientField(false);
        TestSerializationConfig copy = config.copy();
        assertFalse(copy.skipTransientField());
    }

    @Test
    public void test_copy_copiesIgnoredPropNames() {
        Set<String> props = new HashSet<>();
        props.add("field1");
        config.setIgnoredPropNames(props);

        TestSerializationConfig copy = config.copy();
        assertNotNull(copy.getIgnoredPropNames());
        assertEquals(config.getIgnoredPropNames(), copy.getIgnoredPropNames());
    }

    @Test
    public void test_copy_independentModification() {
        TestSerializationConfig copy = config.copy();

        copy.setExclusion(Exclusion.DEFAULT);
        assertNull(config.getExclusion());
        assertEquals(Exclusion.DEFAULT, copy.getExclusion());
    }

    @Test
    public void test_methodChaining_multipleOperations() {
        Set<String> props = new HashSet<>();
        props.add("field1");

        TestSerializationConfig result = config.setExclusion(Exclusion.NULL).skipTransientField(false).setIgnoredPropNames(props);

        assertEquals(config, result);
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertFalse(config.skipTransientField());
        assertNotNull(config.getIgnoredPropNames());
    }
}
