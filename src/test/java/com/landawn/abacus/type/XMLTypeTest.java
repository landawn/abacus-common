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
}
