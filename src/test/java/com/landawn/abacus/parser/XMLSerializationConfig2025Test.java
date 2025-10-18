package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.WD;

@Tag("2025")
public class XMLSerializationConfig2025Test extends TestBase {

    @Test
    public void test_constructor() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        assertNotNull(config);
        assertEquals(WD.CHAR_ZERO, config.getCharQuotation());
        assertEquals(WD.CHAR_ZERO, config.getStringQuotation());
        assertTrue(config.tagByPropertyName());
        assertFalse(config.writeTypeInfo());
    }

    @Test
    public void test_XSC_create() {
        XMLSerializationConfig config = XMLSerializationConfig.XSC.create();
        assertNotNull(config);
    }

    @Test
    public void test_tagByPropertyName() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        assertTrue(config.tagByPropertyName());

        XMLSerializationConfig result = config.tagByPropertyName(false);
        assertSame(config, result);
        assertFalse(config.tagByPropertyName());

        config.tagByPropertyName(true);
        assertTrue(config.tagByPropertyName());
    }

    @Test
    public void test_writeTypeInfo() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        assertFalse(config.writeTypeInfo());

        XMLSerializationConfig result = config.writeTypeInfo(true);
        assertSame(config, result);
        assertTrue(config.writeTypeInfo());

        config.writeTypeInfo(false);
        assertFalse(config.writeTypeInfo());
    }

    @Test
    public void test_setCharQuotation() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        XMLSerializationConfig result = config.setCharQuotation('"');
        assertSame(config, result);
        assertEquals('"', config.getCharQuotation());
    }

    @Test
    public void test_setStringQuotation() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        XMLSerializationConfig result = config.setStringQuotation('"');
        assertSame(config, result);
        assertEquals('"', config.getStringQuotation());
    }

    @Test
    public void test_noCharQuotation() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        XMLSerializationConfig result = config.noCharQuotation();
        assertSame(config, result);
        assertEquals(WD.CHAR_ZERO, config.getCharQuotation());
    }

    @Test
    public void test_noStringQuotation() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        XMLSerializationConfig result = config.noStringQuotation();
        assertSame(config, result);
        assertEquals(WD.CHAR_ZERO, config.getStringQuotation());
    }

    @Test
    public void test_noQuotation() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        XMLSerializationConfig result = config.noQuotation();
        assertSame(config, result);
        assertEquals(WD.CHAR_ZERO, config.getCharQuotation());
        assertEquals(WD.CHAR_ZERO, config.getStringQuotation());
    }

    @Test
    public void test_XSC_of_tagByPropertyName_writeTypeInfo() {
        XMLSerializationConfig config = XMLSerializationConfig.XSC.of(true, true);
        assertNotNull(config);
        assertTrue(config.tagByPropertyName());
        assertTrue(config.writeTypeInfo());
    }

    @Test
    public void test_XSC_of_dateTimeFormat() {
        XMLSerializationConfig config = XMLSerializationConfig.XSC.of(DateTimeFormat.ISO_8601_DATE_TIME);
        assertNotNull(config);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void test_XSC_of_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        XMLSerializationConfig config = XMLSerializationConfig.XSC.of(Exclusion.NULL, ignoredPropNames);
        assertNotNull(config);
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_XSC_of_all_parameters() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        XMLSerializationConfig config = XMLSerializationConfig.XSC.of(true, true, DateTimeFormat.ISO_8601_DATE_TIME, Exclusion.NULL, ignoredPropNames);
        assertNotNull(config);
        assertTrue(config.tagByPropertyName());
        assertTrue(config.writeTypeInfo());
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_equals() {
        XMLSerializationConfig config1 = new XMLSerializationConfig();
        XMLSerializationConfig config2 = new XMLSerializationConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));

        config2.tagByPropertyName(false);
        assertFalse(config1.equals(config2));

        config2.tagByPropertyName(true);
        assertTrue(config1.equals(config2));

        config2.writeTypeInfo(true);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void test_hashCode() {
        XMLSerializationConfig config1 = new XMLSerializationConfig();
        XMLSerializationConfig config2 = new XMLSerializationConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config2.tagByPropertyName(false);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("tagByPropertyName"));
        assertTrue(str.contains("writeTypeInfo"));
    }

    @Test
    public void test_prettyFormat() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        assertFalse(config.prettyFormat());

        config.prettyFormat(true);
        assertTrue(config.prettyFormat());
    }

    @Test
    public void test_setDateTimeFormat() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void test_setExclusion() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    @Test
    public void test_skipTransientField() {
        XMLSerializationConfig config = new XMLSerializationConfig();
        config.skipTransientField(true);
        assertTrue(config.skipTransientField());

        config.skipTransientField(false);
        assertFalse(config.skipTransientField());
    }
}
