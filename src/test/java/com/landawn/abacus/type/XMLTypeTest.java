package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class XMLTypeTest extends TestBase {

    private XMLType<Map<String, Object>> xmlMapType;
    private XMLType<TestBean> xmlBeanType;

    @BeforeEach
    public void setUp() {
        xmlMapType = (XMLType<Map<String, Object>>) createType("XML<Map>");
        xmlBeanType = (XMLType<TestBean>) createType("XML<com.landawn.abacus.type.XMLTypeTest$TestBean>");
    }

    public static class TestBean {
        public String name;
        public int value;
    }

    @Test
    public void testDeclaringName() {
        String declaringName = xmlMapType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("XML"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = xmlMapType.javaType();
        assertNotNull(clazz);
        assertEquals(Map.class, clazz);
    }

    @Test
    public void testStringOf() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        String result = xmlMapType.stringOf(map);
        assertNotNull(result);
    }

    @Test
    public void testStringOfNull() {
        String result = xmlMapType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testBeanTypeStringOf() {
        TestBean bean = new TestBean();
        bean.name = "test";
        bean.value = 123;

        String result = xmlBeanType.stringOf(bean);
        assertNotNull(result);
    }

    @Test
    public void testValueOf() {
        String xml = "<root><key>value</key></root>";
        Map<String, Object> result = xmlMapType.valueOf(xml);
        assertNotNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        Map<String, Object> result = xmlMapType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Map<String, Object> result = xmlMapType.valueOf(null);
        assertNull(result);
    }

    @Test
    public void testBeanTypeValueOf() {
        String xml = "<TestBean><name>test</name><value>123</value></TestBean>";
        TestBean result = xmlBeanType.valueOf(xml);
        assertNotNull(result);
    }

    @Test
    public void testNestedGenericTargetIsPreserved() {
        XMLType<List<Long>> type = (XMLType<List<Long>>) createType("XML<List<Long>>");
        List<Long> source = List.of(1L, 2L);

        List<Long> parsed = type.valueOf(type.stringOf(source));

        assertTrue(type.isParameterizedType());
        assertEquals("List<Long>", type.parameterTypes().get(0).declaringName());
        assertEquals(Long.class, parsed.get(0).getClass());
        assertEquals(source, parsed);
    }

    // ---- review fixes 2026-09-06, T2-07: blank input is null instead of a StAX "Unexpected EOF in prolog" ----

    @Test
    public void reviewFixes20260906_blankStringIsNull() {
        assertNull(xmlMapType.valueOf("  "));
        assertNull(xmlMapType.valueOf("\t\n"));
        assertNull(xmlBeanType.valueOf("   "));
        // Consistent with JSONType for the same target.
        assertNull(createType("JSON<Map>").valueOf("  "));
    }

    // ---- T2-12: the declaring name expands a raw Map argument ----

    @Test
    public void reviewFixes20260906_declaringNameExpandsRawMap() {
        assertEquals("XML<Map<Object, Object>>", xmlMapType.declaringName());
        assertEquals("XML<com.landawn.abacus.type.XMLTypeTest.TestBean>", xmlBeanType.declaringName());
    }

    // ---- T2-14: the parser is present in this build; the null-parser guard is only reachable without StAX ----

    @Test
    public void reviewFixes20260906_parserPresentSoNoUnsupportedOperation() {
        assertTrue(com.landawn.abacus.parser.ParserFactory.isXmlParserAvailable());
        assertNotNull(xmlMapType.stringOf(new HashMap<>(Map.of("a", 1))));
    }

    // ---- bug review 2026-10-02 G124 begin ----
    // G124-02: an instance of the target class is converted through this type's XML form; the inherited default parsed its
    // JSON text as XML ("Unexpected character '{' in prolog").
    @Test
    public void testValueOfObject_beanInstanceIsConvertedThroughXml() {
        final TestBean bean = new TestBean();
        bean.name = "test";
        bean.value = 123;

        final TestBean result = xmlBeanType.valueOf(bean);

        assertEquals("test", result.name);
        assertEquals(123, result.value);
    }

    // G124-02: map and list instances; the contents are converted to the target type's parameters, as by valueOf(stringOf(obj)).
    @Test
    public void testValueOfObject_mapAndListInstancesAreConvertedThroughXml() {
        final XMLType<Map<String, Integer>> mapType = (XMLType<Map<String, Integer>>) createType("XML<Map<String, Integer>>");
        final Map<String, Object> map = new HashMap<>();
        map.put("x", 1L);
        map.put("y", 2);

        final Map<String, Integer> convertedMap = mapType.valueOf(map);
        assertEquals(Map.of("x", 1, "y", 2), convertedMap);
        assertEquals(Integer.class, convertedMap.get("x").getClass());

        final XMLType<List<String>> listType = (XMLType<List<String>>) createType("XML<List<String>>");
        assertEquals(List.of("p", "q"), listType.valueOf((Object) new java.util.ArrayList<>(List.of("p", "q"))));

        final XMLType<List<Long>> longListType = (XMLType<List<Long>>) createType("XML<List<Long>>");
        final List<Long> longs = longListType.valueOf((Object) new java.util.ArrayList<>(List.of(1, 2)));
        assertEquals(List.of(1L, 2L), longs);
        assertEquals(Long.class, longs.get(0).getClass());

        assertEquals(new HashMap<>(Map.of("key", "value")), xmlMapType.valueOf((Object) new HashMap<>(Map.of("key", "value"))));
    }

    // G124-02 neighbours (unchanged): XML text given as a String or Reader is parsed, also for an Object target; null is null.
    @Test
    public void testValueOfObject_textSourcesAreStillParsed() {
        final String xml = "<root><key>value</key></root>";
        assertEquals("value", xmlMapType.valueOf((Object) xml).get("key"));
        assertEquals("value", xmlMapType.valueOf(new java.io.StringReader(xml)).get("key"));

        final Object fromObjectTarget = createType("XML<Object>").valueOf((Object) xml);
        assertTrue(fromObjectTarget instanceof com.landawn.abacus.util.MapEntity, String.valueOf(fromObjectTarget));

        final TestBean fromText = xmlBeanType.valueOf((Object) "<TestBean><name>test</name><value>123</value></TestBean>");
        assertEquals("test", fromText.name);
        assertNull(xmlBeanType.valueOf((Object) null));
    }
    // ---- bug review 2026-10-02 G124 end ----

    // ---- bug review 2026-10-02 verify G125 begin ----
    // G124-02: array and set instances of an array/collection target are converted through the XML form as well
    @Test
    public void testValueOfObject_arrayAndSetInstancesAreConvertedThroughXml() {
        final String[] strings = { "a", "b" };
        final String[] convertedStrings = ((XMLType<String[]>) createType("XML<String[]>")).valueOf(strings);
        assertTrue(java.util.Arrays.equals(strings, convertedStrings));

        final Long[] longs = ((XMLType<Long[]>) createType("XML<Long[]>")).valueOf(new Long[] { 1L, 2L });
        assertTrue(java.util.Arrays.equals(new Long[] { 1L, 2L }, longs));

        assertEquals(java.util.Set.of("s"), createType("XML<Set<String>>").valueOf((Object) new java.util.LinkedHashSet<>(List.of("s"))));
    }

    // G124-02 neighbour (unchanged): for an XML<Object> target every object is an instance, but it keeps the inherited default
    // and fails as before - re-parsing its XML form as Object would give a MapEntity that silently drops a collection's elements
    @Test
    public void testValueOfObject_objectTargetKeepsInheritedDefault() {
        final XMLType<Object> objectType = (XMLType<Object>) createType("XML<Object>");
        final TestBean bean = new TestBean();
        bean.name = "test";

        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class,
                () -> objectType.valueOf((Object) new java.util.ArrayList<>(List.of("p", "q"))));
        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> objectType.valueOf(new String[] { "a", "b" }));
        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class,
                () -> objectType.valueOf((Object) new HashMap<>(Map.of("a", 1))));
        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> objectType.valueOf(bean));
    }
    // ---- bug review 2026-10-02 verify G125 end ----
}
