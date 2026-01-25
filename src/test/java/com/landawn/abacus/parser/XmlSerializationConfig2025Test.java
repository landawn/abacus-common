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
public class XmlSerializationConfig2025Test extends TestBase {

    @Test
    public void test_constructor() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        assertNotNull(config);
        assertEquals(WD.CHAR_ZERO, config.getCharQuotation());
        assertEquals(WD.CHAR_ZERO, config.getStringQuotation());
        assertTrue(config.tagByPropertyName());
        assertFalse(config.writeTypeInfo());
    }

    @Test
    public void test_XSC_create() {
        XmlSerializationConfig config = XmlSerializationConfig.XSC.create();
        assertNotNull(config);
    }

    @Test
    public void test_tagByPropertyName() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        assertTrue(config.tagByPropertyName());

        XmlSerializationConfig result = config.tagByPropertyName(false);
        assertSame(config, result);
        assertFalse(config.tagByPropertyName());

        config.tagByPropertyName(true);
        assertTrue(config.tagByPropertyName());
    }

    @Test
    public void test_writeTypeInfo() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        assertFalse(config.writeTypeInfo());

        XmlSerializationConfig result = config.writeTypeInfo(true);
        assertSame(config, result);
        assertTrue(config.writeTypeInfo());

        config.writeTypeInfo(false);
        assertFalse(config.writeTypeInfo());
    }

    @Test
    public void test_setCharQuotation() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        XmlSerializationConfig result = config.setCharQuotation('"');
        assertSame(config, result);
        assertEquals('"', config.getCharQuotation());
    }

    @Test
    public void test_setStringQuotation() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        XmlSerializationConfig result = config.setStringQuotation('"');
        assertSame(config, result);
        assertEquals('"', config.getStringQuotation());
    }

    @Test
    public void test_noCharQuotation() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        XmlSerializationConfig result = config.noCharQuotation();
        assertSame(config, result);
        assertEquals(WD.CHAR_ZERO, config.getCharQuotation());
    }

    @Test
    public void test_noStringQuotation() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        XmlSerializationConfig result = config.noStringQuotation();
        assertSame(config, result);
        assertEquals(WD.CHAR_ZERO, config.getStringQuotation());
    }

    @Test
    public void test_noQuotation() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        XmlSerializationConfig result = config.noQuotation();
        assertSame(config, result);
        assertEquals(WD.CHAR_ZERO, config.getCharQuotation());
        assertEquals(WD.CHAR_ZERO, config.getStringQuotation());
    }

    @Test
    public void test_XSC_of_tagByPropertyName_writeTypeInfo() {
        XmlSerializationConfig config = XmlSerializationConfig.XSC.of(true, true);
        assertNotNull(config);
        assertTrue(config.tagByPropertyName());
        assertTrue(config.writeTypeInfo());
    }

    @Test
    public void test_XSC_of_dateTimeFormat() {
        XmlSerializationConfig config = XmlSerializationConfig.XSC.of(DateTimeFormat.ISO_8601_DATE_TIME);
        assertNotNull(config);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void test_XSC_of_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        XmlSerializationConfig config = XmlSerializationConfig.XSC.of(Exclusion.NULL, ignoredPropNames);
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

        XmlSerializationConfig config = XmlSerializationConfig.XSC.of(true, true, DateTimeFormat.ISO_8601_DATE_TIME, Exclusion.NULL, ignoredPropNames);
        assertNotNull(config);
        assertTrue(config.tagByPropertyName());
        assertTrue(config.writeTypeInfo());
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_equals() {
        XmlSerializationConfig config1 = new XmlSerializationConfig();
        XmlSerializationConfig config2 = new XmlSerializationConfig();

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
        XmlSerializationConfig config1 = new XmlSerializationConfig();
        XmlSerializationConfig config2 = new XmlSerializationConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config2.tagByPropertyName(false);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("tagByPropertyName"));
        assertTrue(str.contains("writeTypeInfo"));
    }

    @Test
    public void test_prettyFormat() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        assertFalse(config.prettyFormat());

        config.prettyFormat(true);
        assertTrue(config.prettyFormat());
    }

    @Test
    public void test_setDateTimeFormat() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void test_setExclusion() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    @Test
    public void test_skipTransientField() {
        XmlSerializationConfig config = new XmlSerializationConfig();
        config.skipTransientField(true);
        assertTrue(config.skipTransientField());

        config.skipTransientField(false);
        assertFalse(config.skipTransientField());
    }
}
