package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;

public class MultisetTypeTest extends TestBase {

    private MultisetType<String> multisetType;

    @BeforeEach
    public void setUp() {
        multisetType = (MultisetType<String>) createType("Multiset<String>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = multisetType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Multiset"));
    }

    @Test
    public void testClazz() {
        Class<Multiset<String>> clazz = multisetType.javaType();
        Assertions.assertNotNull(clazz);
        assertEquals(Multiset.class, clazz);
    }

    @Test
    public void testGetElementType() {
        Type<String> elementType = multisetType.elementType();
        Assertions.assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = multisetType.parameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = multisetType.isParameterizedType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testIsSerializable() {
        boolean isSerializable = multisetType.isSerializable();
        Assertions.assertTrue(isSerializable);
    }

    @Test
    public void testStringOfNull() {
        String result = multisetType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        Multiset<String> multiset = N.newMultiset();
        multiset.add("apple", 2);
        multiset.add("banana", 3);

        String result = multisetType.stringOf(multiset);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("apple"));
        Assertions.assertTrue(result.contains("banana"));
        Assertions.assertTrue(result.contains("2"));
        Assertions.assertTrue(result.contains("3"));
    }

    @Test
    public void testValueOfNull() {
        Multiset<String> result = multisetType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        Multiset<String> result = multisetType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidJson() {
        Multiset<String> result = multisetType.valueOf("{\"apple\":2,\"banana\":3}");
        Assertions.assertNotNull(result);
        assertEquals(2, result.count("apple"));
        assertEquals(3, result.count("banana"));
    }

    // T8-05: {"a": null} threw an unboxing NullPointerException; now the element is simply not added (like a 0 count).
    @Test
    public void reviewFixes20260906_valueOfNullOrZeroCountAddsNothing() {
        final Multiset<String> ms = multisetType.valueOf("{\"a\": null, \"b\": 1, \"c\": 0}");

        assertEquals(0, ms.count("a"));
        Assertions.assertFalse(ms.contains("a"));
        assertEquals(1, ms.count("b"));
        Assertions.assertFalse(ms.contains("c"));
        assertEquals(1, ms.size());

        Assertions.assertTrue(multisetType.valueOf("{}").isEmpty());
        Assertions.assertTrue(multisetType.valueOf("{\"a\": null}").isEmpty());

        // Unicode elements
        assertEquals(2, multisetType.valueOf("{\"中\": 2, \"😀\": null}").count("中"));

        // documented failure modes
        Assertions.assertThrows(IllegalArgumentException.class, () -> multisetType.valueOf("{\"a\": -1}"));
        Assertions.assertThrows(NumberFormatException.class, () -> multisetType.valueOf("{\"a\": \"x\"}"));
        Assertions.assertThrows(ArithmeticException.class, () -> multisetType.valueOf("{\"a\": 2147483648}"));
        Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> multisetType.valueOf("{\"a\": 1"));
    }

    public static class MultisetBean {
        private Multiset<String> ms;

        public Multiset<String> getMs() {
            return ms;
        }

        public void setMs(final Multiset<String> ms) {
            this.ms = ms;
        }
    }

    // T8-04 (documented contract, not a behaviour change): a nested multiset is written as a quoted JSON string.
    @Test
    public void reviewFixes20260906_nestedMultisetIsWrittenAsQuotedJsonString() {
        final MultisetBean bean = new MultisetBean();
        bean.setMs(N.newMultiset());
        bean.getMs().add("x", 2);

        final String json = N.toJson(bean);

        assertEquals("{\"ms\": \"{\\\"x\\\": 2}\"}", json);
        assertEquals(bean.getMs(), N.fromJson(json, MultisetBean.class).getMs());

        final String xml = N.toXml(bean);
        Assertions.assertTrue(xml.contains("<ms>{&quot;x&quot;: 2}</ms>"), xml);
        assertEquals(bean.getMs(), N.fromXml(xml, MultisetBean.class).getMs());
    }

}
