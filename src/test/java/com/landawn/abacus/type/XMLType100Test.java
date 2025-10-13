package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class XMLType100Test extends TestBase {

    private XMLType<Map<String, Object>> xmlMapType;
    private XMLType<TestBean> xmlBeanType;

    @BeforeEach
    public void setUp() {
        xmlMapType = (XMLType<Map<String, Object>>) createType("XML<Map>");
        xmlBeanType = (XMLType<TestBean>) createType("XML<com.landawn.abacus.type.XMLType100Test$TestBean>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = xmlMapType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("XML"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = xmlMapType.clazz();
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
    public void testValueOf() {
        String xml = "<root><key>value</key></root>";
        Map<String, Object> result = xmlMapType.valueOf(xml);
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
    public void testBeanTypeStringOf() {
        TestBean bean = new TestBean();
        bean.name = "test";
        bean.value = 123;

        String result = xmlBeanType.stringOf(bean);
        assertNotNull(result);
    }

    @Test
    public void testBeanTypeValueOf() {
        String xml = "<TestBean><name>test</name><value>123</value></TestBean>";
        TestBean result = xmlBeanType.valueOf(xml);
    }

    public static class TestBean {
        public String name;
        public int value;
    }
}
