package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

public class XmlSerConfigTest extends TestBase {

    private XmlSerConfig config;

    @BeforeEach
    public void setUp() {
        config = new XmlSerConfig();
    }

    // isPrettyFormat / setPrettyFormat
    @Test
    public void test_prettyFormat() {
        XmlSerConfig config = new XmlSerConfig();
        assertFalse(config.isPrettyFormat());

        config.setPrettyFormat(true);
        assertTrue(config.isPrettyFormat());
    }

    // getDateTimeFormat / setDateTimeFormat
    @Test
    public void test_setDateTimeFormat() {
        XmlSerConfig config = new XmlSerConfig();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    // isSkipTransientField / setSkipTransientField
    @Test
    public void test_skipTransientField() {
        XmlSerConfig config = new XmlSerConfig();
        config.setSkipTransientField(true);
        assertTrue(config.isSkipTransientField());

        config.setSkipTransientField(false);
        assertFalse(config.isSkipTransientField());
    }

    // constructor
    @Test
    public void test_constructor() {
        XmlSerConfig config = new XmlSerConfig();
        assertNotNull(config);
        assertEquals(SK.CHAR_ZERO, config.getCharQuotation());
        assertEquals(SK.CHAR_ZERO, config.getStringQuotation());
        assertTrue(config.isTagByPropertyName());
        assertFalse(config.isWriteTypeInfo());
    }

    // getExclusion / setExclusion
    @Test
    public void test_setExclusion() {
        XmlSerConfig config = new XmlSerConfig();
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    // setStringQuotation
    @Test
    public void test_setStringQuotation() {
        XmlSerConfig config = new XmlSerConfig();
        XmlSerConfig result = config.setStringQuotation('"');
        assertSame(config, result);
        assertEquals('"', config.getStringQuotation());
    }

    @Test
    public void testSetStringQuotation() {
        XmlSerConfig result = config.setStringQuotation('"');
        assertSame(config, result);
        assertEquals('"', config.getStringQuotation());
    }

    // setCharQuotation
    @Test
    public void test_setCharQuotation() {
        XmlSerConfig config = new XmlSerConfig();
        XmlSerConfig result = config.setCharQuotation('"');
        assertSame(config, result);
        assertEquals('"', config.getCharQuotation());
    }

    @Test
    public void testSetCharQuotation() {
        XmlSerConfig result = config.setCharQuotation('\'');
        assertSame(config, result);
        assertEquals('\'', config.getCharQuotation());
    }

    @Test
    public void testNoCharQuotation() {
        config.setCharQuotation('"');
        config.noCharQuotation();
        assertEquals((char) 0, config.getCharQuotation());
    }

    // noCharQuotation
    @Test
    public void test_noCharQuotation() {
        XmlSerConfig config = new XmlSerConfig();
        XmlSerConfig result = config.noCharQuotation();
        assertSame(config, result);
        assertEquals(SK.CHAR_ZERO, config.getCharQuotation());
    }

    @Test
    public void testNoStringQuotation() {
        config.setStringQuotation('"');
        config.noStringQuotation();
        assertEquals((char) 0, config.getStringQuotation());
    }

    // noStringQuotation
    @Test
    public void test_noStringQuotation() {
        XmlSerConfig config = new XmlSerConfig();
        XmlSerConfig result = config.noStringQuotation();
        assertSame(config, result);
        assertEquals(SK.CHAR_ZERO, config.getStringQuotation());
    }

    @Test
    public void testNoQuotation() {
        config.setCharQuotation('"');
        config.setStringQuotation('"');
        config.noQuotation();
        assertEquals((char) 0, config.getCharQuotation());
        assertEquals((char) 0, config.getStringQuotation());
    }

    // noQuotation
    @Test
    public void test_noQuotation() {
        XmlSerConfig config = new XmlSerConfig();
        XmlSerConfig result = config.noQuotation();
        assertSame(config, result);
        assertEquals(SK.CHAR_ZERO, config.getCharQuotation());
        assertEquals(SK.CHAR_ZERO, config.getStringQuotation());
    }

    @Test
    public void testIsTagByPropertyName() {
        assertTrue(config.isTagByPropertyName());
        config.setTagByPropertyName(false);
        assertFalse(config.isTagByPropertyName());
    }

    // isTagByPropertyName
    @Test
    public void test_tagByPropertyName() {
        XmlSerConfig config = new XmlSerConfig();
        assertTrue(config.isTagByPropertyName());

        XmlSerConfig result = config.setTagByPropertyName(false);
        assertSame(config, result);
        assertFalse(config.isTagByPropertyName());

        config.setTagByPropertyName(true);
        assertTrue(config.isTagByPropertyName());
    }

    // setTagByPropertyName
    @Test
    public void testSetTagByPropertyName() {
        XmlSerConfig result = config.setTagByPropertyName(false);
        assertSame(config, result);
        assertFalse(config.isTagByPropertyName());
    }

    // copy
    @Test
    public void testCopy() {
        config.setTagByPropertyName(false);
        config.setWriteTypeInfo(true);
        config.setPrettyFormat(true);

        XmlSerConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
        assertFalse(copy.isTagByPropertyName());
        assertTrue(copy.isWriteTypeInfo());
        assertTrue(copy.isPrettyFormat());
    }

    @Test
    public void testIsWriteTypeInfo() {
        assertFalse(config.isWriteTypeInfo());
        config.setWriteTypeInfo(true);
        assertTrue(config.isWriteTypeInfo());
    }

    // isWriteTypeInfo
    @Test
    public void test_writeTypeInfo() {
        XmlSerConfig config = new XmlSerConfig();
        assertFalse(config.isWriteTypeInfo());

        XmlSerConfig result = config.setWriteTypeInfo(true);
        assertSame(config, result);
        assertTrue(config.isWriteTypeInfo());

        config.setWriteTypeInfo(false);
        assertFalse(config.isWriteTypeInfo());
    }

    // setWriteTypeInfo
    @Test
    public void testSetWriteTypeInfo() {
        XmlSerConfig result = config.setWriteTypeInfo(true);
        assertSame(config, result);
        assertTrue(config.isWriteTypeInfo());
    }

    // hashCode
    @Test
    public void test_hashCode() {
        XmlSerConfig config1 = new XmlSerConfig();
        XmlSerConfig config2 = new XmlSerConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config2.setTagByPropertyName(false);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode() {
        XmlSerConfig config1 = new XmlSerConfig();
        XmlSerConfig config2 = new XmlSerConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config1.setWriteTypeInfo(true);
        assertNotEquals(config1.hashCode(), config2.hashCode());

        config2.setWriteTypeInfo(true);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    // equals
    @Test
    public void test_equals() {
        XmlSerConfig config1 = new XmlSerConfig();
        XmlSerConfig config2 = new XmlSerConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));

        config2.setTagByPropertyName(false);
        assertFalse(config1.equals(config2));

        config2.setTagByPropertyName(true);
        assertTrue(config1.equals(config2));

        config2.setWriteTypeInfo(true);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void testEquals() {
        XmlSerConfig config1 = new XmlSerConfig();
        XmlSerConfig config2 = new XmlSerConfig();

        assertEquals(config1, config1);
        assertEquals(config1, config2);
        assertNotEquals(config1, null);
        assertNotEquals(config1, "string");

        config1.setTagByPropertyName(false);
        assertNotEquals(config1, config2);

        config2.setTagByPropertyName(false);
        assertEquals(config1, config2);
    }

    // toString
    @Test
    public void test_toString() {
        XmlSerConfig config = new XmlSerConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("tagByPropertyName"));
        assertTrue(str.contains("writeTypeInfo"));
    }

    @Test
    public void testToString() {
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("tagByPropertyName=true"));
        assertTrue(str.contains("writeTypeInfo=false"));
        assertTrue(str.contains("prettyFormat="));
        assertTrue(str.contains("exclusion="));
    }

    // create
    @Test
    public void test_XSC_create() {
        XmlSerConfig config = XmlSerConfig.create();
        assertNotNull(config);
    }

    @Test
    public void testCreate() {
        XmlSerConfig newConfig = XmlSerConfig.create();
        assertNotNull(newConfig);
        assertNotSame(config, newConfig);
        assertTrue(newConfig.isTagByPropertyName());
        assertFalse(newConfig.isWriteTypeInfo());
    }

    // combined config tests
    @Test
    public void test_XSC_of_tagByPropertyName_writeTypeInfo() {
        XmlSerConfig config = XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(true);
        assertNotNull(config);
        assertTrue(config.isTagByPropertyName());
        assertTrue(config.isWriteTypeInfo());
    }

    @Test
    public void test_XSC_of_dateTimeFormat() {
        XmlSerConfig config = XmlSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertNotNull(config);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void test_XSC_of_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        XmlSerConfig config = XmlSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredPropNames);
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

        XmlSerConfig config = XmlSerConfig.create()
                .setTagByPropertyName(true)
                .setWriteTypeInfo(true)
                .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)
                .setExclusion(Exclusion.NULL)
                .setIgnoredPropNames(ignoredPropNames);
        assertNotNull(config);
        assertTrue(config.isTagByPropertyName());
        assertTrue(config.isWriteTypeInfo());
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void testSetPrettyFormat() {
        XmlSerConfig result = config.setPrettyFormat(true);
        assertSame(config, result);
        assertTrue(config.isPrettyFormat());

        config.setPrettyFormat(false);
        assertFalse(config.isPrettyFormat());
    }

    @Test
    public void testGetDateTimeFormat() {
        assertEquals(DateTimeFormat.LONG, config.getDateTimeFormat());
    }

    @Test
    public void testSetDateTimeFormat() {
        XmlSerConfig result = config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertSame(config, result);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void testGetExclusion() {
        assertNull(config.getExclusion());
    }

    @Test
    public void testSetExclusion() {
        XmlSerConfig result = config.setExclusion(Exclusion.NULL);
        assertSame(config, result);
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    @Test
    public void testSetSkipTransientField() {
        XmlSerConfig result = config.setSkipTransientField(false);
        assertSame(config, result);
        assertFalse(config.isSkipTransientField());
    }

    // isWriteLongAsString / setWriteLongAsString
    @Test
    public void testSetWriteLongAsString() {
        assertFalse(config.isWriteLongAsString());

        XmlSerConfig result = config.setWriteLongAsString(true);
        assertSame(config, result);
        assertTrue(config.isWriteLongAsString());
    }

    // isWriteNullStringAsEmpty / setWriteNullStringAsEmpty
    @Test
    public void testSetWriteNullStringAsEmpty() {
        assertFalse(config.isWriteNullStringAsEmpty());

        XmlSerConfig result = config.setWriteNullStringAsEmpty(true);
        assertSame(config, result);
        assertTrue(config.isWriteNullStringAsEmpty());
    }

    // isWriteNullNumberAsZero / setWriteNullNumberAsZero
    @Test
    public void testSetWriteNullNumberAsZero() {
        assertFalse(config.isWriteNullNumberAsZero());

        XmlSerConfig result = config.setWriteNullNumberAsZero(true);
        assertSame(config, result);
        assertTrue(config.isWriteNullNumberAsZero());
    }

    // isWriteNullBooleanAsFalse / setWriteNullBooleanAsFalse
    @Test
    public void testSetWriteNullBooleanAsFalse() {
        assertFalse(config.isWriteNullBooleanAsFalse());

        XmlSerConfig result = config.setWriteNullBooleanAsFalse(true);
        assertSame(config, result);
        assertTrue(config.isWriteNullBooleanAsFalse());
    }

    // isWriteBigDecimalAsPlain / setWriteBigDecimalAsPlain
    @Test
    public void testSetWriteBigDecimalAsPlain() {
        assertFalse(config.isWriteBigDecimalAsPlain());

        XmlSerConfig result = config.setWriteBigDecimalAsPlain(true);
        assertSame(config, result);
        assertTrue(config.isWriteBigDecimalAsPlain());
    }

    // isFailOnEmptyBean / setFailOnEmptyBean
    @Test
    public void testSetFailOnEmptyBean() {
        assertTrue(config.isFailOnEmptyBean());

        XmlSerConfig result = config.setFailOnEmptyBean(false);
        assertSame(config, result);
        assertFalse(config.isFailOnEmptyBean());
    }

    // isCircularReferenceSupported / setCircularReferenceSupported
    @Test
    public void testSetCircularReferenceSupported() {
        assertFalse(config.isCircularReferenceSupported());

        XmlSerConfig result = config.setCircularReferenceSupported(true);
        assertSame(config, result);
        assertTrue(config.isCircularReferenceSupported());
    }

    // getIndentation / setIndentation
    @Test
    public void testGetIndentation() {
        assertEquals("    ", config.getIndentation());
    }

    @Test
    public void testSetIndentation() {
        XmlSerConfig result = config.setIndentation("\t");
        assertSame(config, result);
        assertEquals("\t", config.getIndentation());
    }

    // getPropNamingPolicy / setPropNamingPolicy
    @Test
    public void testGetPropNamingPolicy() {
        assertNull(config.getPropNamingPolicy());
    }

    @Test
    public void testSetPropNamingPolicy() {
        XmlSerConfig result = config.setPropNamingPolicy(NamingPolicy.CAMEL_CASE);
        assertSame(config, result);
        assertEquals(NamingPolicy.CAMEL_CASE, config.getPropNamingPolicy());
    }

    // getCharQuotation
    @Test
    public void testGetCharQuotation() {
        assertEquals(SK.CHAR_ZERO, config.getCharQuotation());
    }

    // getStringQuotation
    @Test
    public void testGetStringQuotation() {
        assertEquals(SK.CHAR_ZERO, config.getStringQuotation());
    }

    // ---------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06 (P8-14 toString without raw NUL; P8-16 javadoc pin)
    // ---------------------------------------------------------------------------------------------

    public static class TagBean {
        private String firstName = "a";
        private int age = 1;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }

    @Test
    public void reviewFixes20260906_toString_containsNoRawNul() {
        final String str = new XmlSerConfig().toString();
        assertTrue(str.indexOf('\0') < 0, "toString must not embed U+0000: " + str);
        assertEquals(-1, str.indexOf(SK.CHAR_ZERO));
        assertTrue(str.contains("charQuotation=\\u0000, stringQuotation=\\u0000, dateTimeFormat=LONG"), str);
        // still starts and ends like a config dump and keeps the other fields
        assertTrue(str.startsWith("{ignoredPropNames=null, charQuotation="), str);
        assertTrue(str.endsWith(", tagByPropertyName=true, writeTypeInfo=false}"), str);
        // a copy renders identically
        assertEquals(str, new XmlSerConfig().copy().toString());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void reviewFixes20260906_toString_nonZeroQuotationRenderedAsTheCharItself() {
        final String str = new XmlSerConfig().setCharQuotation('\'').setStringQuotation('"').toString();
        assertTrue(str.indexOf('\0') < 0, str);
        assertTrue(str.contains("charQuotation=', stringQuotation=\","), str);
    }

    @Test
    public void reviewFixes20260906_tagByPropertyName_falseWritesGenericPropertyElements() {
        final XmlParser xp = ParserFactory.createXmlParser();
        final XmlParser axp = ParserFactory.createAbacusXmlParser();

        assertEquals("<tagBean><firstName>a</firstName><age>1</age></tagBean>", xp.serialize(new TagBean(), new XmlSerConfig().setTagByPropertyName(true)));
        assertEquals("<tagBean><firstName>a</firstName><age>1</age></tagBean>", xp.serialize(new TagBean(), new XmlSerConfig()));
        assertEquals("<bean name=\"tagBean\"><property name=\"firstName\">a</property><property name=\"age\">1</property></bean>",
                xp.serialize(new TagBean(), new XmlSerConfig().setTagByPropertyName(false)));
        assertEquals("<bean name=\"tagBean\"><property name=\"firstName\">a</property><property name=\"age\">1</property></bean>",
                axp.serialize(new TagBean(), new XmlSerConfig().setTagByPropertyName(false)));
    }

    /**
     * A subclass that narrows equality the way {@code AvroSerConfig}/{@code KryoSerConfig} do over
     * {@code SerializationConfig}: {@code instanceof <OwnType> && super.equals(obj)}.
     */
    static class NarrowingXmlSerConfig extends XmlSerConfig {
        @Override
        public boolean equals(final Object obj) {
            return this == obj || (obj instanceof NarrowingXmlSerConfig && super.equals(obj));
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    @Test
    public void reviewFixes20260908_equalsIsSymmetricAgainstASubclassThatNarrowsEquality() {
        final XmlSerConfig base = new XmlSerConfig();
        final NarrowingXmlSerConfig sub = new NarrowingXmlSerConfig();

        // Same settings, different classes. With the old `obj instanceof XmlSerConfig` test base.equals(sub)
        // was true while sub.equals(base) was false, the very asymmetry SerializationConfig#equals documents
        // its exact-class rule to prevent.
        assertEquals(base.equals(sub), sub.equals(base));
        assertFalse(base.equals(sub));
        assertFalse(sub.equals(base));

        // Unchanged for everything else.
        assertTrue(base.equals(new XmlSerConfig()));
        assertTrue(sub.equals(new NarrowingXmlSerConfig()));
        assertEquals(base.hashCode(), new XmlSerConfig().hashCode());
        assertFalse(base.equals(null));
        assertFalse(base.equals("not a config"));
    }

}
