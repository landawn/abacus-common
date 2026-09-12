package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SetMultimap;

public class MultimapTypeTest extends TestBase {

    private MultimapType<String, Integer, List<Integer>, ListMultimap<String, Integer>> listMultimapType;
    private MultimapType<String, Integer, Set<Integer>, SetMultimap<String, Integer>> setMultimapType;

    @BeforeEach
    public void setUp() {
        listMultimapType = (MultimapType<String, Integer, List<Integer>, ListMultimap<String, Integer>>) createType("ListMultimap<String, Integer>");
        setMultimapType = (MultimapType<String, Integer, Set<Integer>, SetMultimap<String, Integer>>) createType("SetMultimap<String, Integer>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = listMultimapType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Multimap"));
    }

    @Test
    public void testClazz() {
        Class<?> clazz = listMultimapType.javaType();
        Assertions.assertNotNull(clazz);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = listMultimapType.parameterTypes();
        Assertions.assertNotNull(paramTypes);
        Assertions.assertTrue(paramTypes.size() >= 2);
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = listMultimapType.isParameterizedType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testIsSerializable() {
        boolean isSerializable = listMultimapType.isSerializable();
        Assertions.assertTrue(isSerializable);
    }

    @Test
    public void testStringOfNull() {
        String result = listMultimapType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key1", 2);
        multimap.put("key2", 3);

        String result = listMultimapType.stringOf(multimap);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key1"));
        Assertions.assertTrue(result.contains("key2"));
    }

    @Test
    public void testValueOfNull() {
        ListMultimap<String, Integer> result = listMultimapType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        ListMultimap<String, Integer> result = listMultimapType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidJsonForListMultimap() {
        ListMultimap<String, Integer> result = listMultimapType.valueOf("{\"key1\":[1,2],\"key2\":[3]}");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.containsKey("key1"));
        Assertions.assertTrue(result.containsKey("key2"));
        assertEquals(2, result.get("key1").size());
        assertEquals(1, result.get("key2").size());
    }

    @Test
    public void testValueOfValidJsonForSetMultimap() {
        SetMultimap<String, Integer> result = setMultimapType.valueOf("{\"key1\":[1,2],\"key2\":[3]}");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.containsKey("key1"));
        Assertions.assertTrue(result.containsKey("key2"));
        assertEquals(2, result.get("key1").size());
        assertEquals(1, result.get("key2").size());
    }

    /**
     * Regression: when only the key + element types are specified (the typical
     * "ListMultimap<K,V>"/"SetMultimap<K,V>" form), parameterTypes.get(1) is
     * the element type, NOT a collection type. The old code unconditionally
     * checked Set.class.isAssignableFrom against the element, so the resulting
     * concrete multimap implementation was always ListMultimap-backed —
     * dropping the Set semantics that SetMultimap callers depend on
     * (e.g. duplicate values would have been kept). The fix bases the decision
     * on the declared multimap class first.
     */
    @Test
    public void testValueOf_setMultimapDeducedFromDeclaredClass_dropsDuplicates() {
        // The SetMultimap declaration must produce a Set-backed multimap, so
        // duplicate values in the JSON array collapse. If the bug regressed,
        // the values collection size would be 3 (List behaviour) instead of 2.
        SetMultimap<String, Integer> result = setMultimapType.valueOf("{\"key1\":[1,2,1]}");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.get("key1") instanceof Set);
        assertEquals(2, result.get("key1").size(), "SetMultimap must collapse duplicates; got " + result.get("key1"));
    }

    @Test
    public void testValueOf_listMultimapDeducedFromDeclaredClass_keepsDuplicates() {
        // Mirror test for ListMultimap — duplicates must be preserved.
        ListMultimap<String, Integer> result = listMultimapType.valueOf("{\"key1\":[1,2,1]}");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.get("key1") instanceof List);
        assertEquals(3, result.get("key1").size(), "ListMultimap must keep duplicates; got " + result.get("key1"));
    }

    // T8-01: valueOf lost the JSON document order (HashMap intermediate) although the javadoc promises it.
    @Test
    public void reviewFixes20260906_valueOfKeepsDocumentKeyOrderForEveryDeclaredForm() {
        final String json = "{\"z\": [1], \"a\": [2], \"m\": [3], \"b\": [4], \"q\": [5]}";
        final List<String> expected = java.util.Arrays.asList("z", "a", "m", "b", "q");

        for (final String typeName : new String[] { "ListMultimap<String, Integer>", "SetMultimap<String, Integer>", "Multimap<String, List<Integer>>",
                "Multimap<String, Set<Integer>>", "Multimap<String, Integer, List<Integer>>", "Multimap<String, Integer, Set<Integer>>",
                "Multimap<String, Integer, LinkedHashSet<Integer>>" }) {
            final Type<com.landawn.abacus.util.Multimap<String, Integer, ?>> t = TypeFactory.getType(typeName);
            final com.landawn.abacus.util.Multimap<String, Integer, ?> mm = t.valueOf(json);

            assertEquals(expected, new java.util.ArrayList<>(mm.keySet()), typeName);
            assertEquals(json, t.stringOf(mm), typeName);
            assertEquals(json, t.stringOf(t.valueOf(t.stringOf(mm))), typeName);
        }

        assertEquals(expected, new java.util.ArrayList<>(listMultimapType.valueOf(json).keySet()));
        assertEquals(expected, new java.util.ArrayList<>(setMultimapType.valueOf(json).keySet()));
    }

    @Test
    public void reviewFixes20260906_valueOfValueOrderDependsOnDeclaredValueCollection() {
        final String json = "{\"k\": [\"z\", \"a\", \"m\", \"b\", \"q\", \"z\"]}";
        final List<String> ordered = java.util.Arrays.asList("z", "a", "m", "b", "q");

        // implied or ordered value collections keep the array order
        final Type<com.landawn.abacus.util.Multimap<String, String, java.util.LinkedHashSet<String>>> linked = TypeFactory
                .getType("Multimap<String, String, LinkedHashSet<String>>");
        assertEquals(ordered, new java.util.ArrayList<>(linked.valueOf(json).get("k")));

        final Type<com.landawn.abacus.util.Multimap<String, String, List<String>>> listForm = TypeFactory.getType("Multimap<String, List<String>>");
        assertEquals(java.util.Arrays.asList("z", "a", "m", "b", "q", "z"), listForm.valueOf(json).get("k"));

        // an explicitly declared Set<E> keeps its (unordered) semantics: same elements, order unspecified
        final Type<com.landawn.abacus.util.Multimap<String, String, Set<String>>> setForm = TypeFactory.getType("Multimap<String, String, Set<String>>");
        assertEquals(new java.util.HashSet<>(ordered), new java.util.HashSet<>(setForm.valueOf(json).get("k")));
        Assertions.assertTrue(setForm.valueOf(json) instanceof SetMultimap);
    }

    @Test
    public void reviewFixes20260906_valueOfEdgeCases() {
        assertEquals(java.util.Collections.emptyList(), new java.util.ArrayList<>(listMultimapType.valueOf("{}").keySet()));
        assertEquals(java.util.Arrays.asList("only"), new java.util.ArrayList<>(listMultimapType.valueOf("{\"only\": [1]}").keySet()));

        // duplicate key: position of the first occurrence, values of the last one
        final ListMultimap<String, Integer> dup = listMultimapType.valueOf("{\"a\": [1], \"b\": [2], \"a\": [3]}");
        assertEquals(java.util.Arrays.asList("a", "b"), new java.util.ArrayList<>(dup.keySet()));
        assertEquals(java.util.Arrays.asList(3), dup.get("a"));

        // T8-05: a null value or an empty array drops the key (both flavours)
        assertEquals(java.util.Arrays.asList("b"), new java.util.ArrayList<>(listMultimapType.valueOf("{\"a\": null, \"b\": [1], \"c\": []}").keySet()));
        assertEquals(java.util.Arrays.asList("b"), new java.util.ArrayList<>(setMultimapType.valueOf("{\"a\": null, \"b\": [1], \"c\": []}").keySet()));

        // T8-12: malformed text
        Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> listMultimapType.valueOf("{\"a\": [1]"));
    }

    public static class MultimapBean {
        private ListMultimap<String, Integer> lm;

        public ListMultimap<String, Integer> getLm() {
            return lm;
        }

        public void setLm(final ListMultimap<String, Integer> lm) {
            this.lm = lm;
        }
    }

    // T8-04 (documented contract, not a behaviour change): a nested multimap is written as a quoted JSON string.
    @Test
    public void reviewFixes20260906_nestedMultimapIsWrittenAsQuotedJsonString() {
        final MultimapBean bean = new MultimapBean();
        bean.setLm(N.newLinkedListMultimap());
        bean.getLm().put("z", 1);
        bean.getLm().put("a", 2);

        final String json = N.toJson(bean);

        assertEquals("{\"lm\": \"{\\\"z\\\": [1], \\\"a\\\": [2]}\"}", json);

        final MultimapBean back = N.fromJson(json, MultimapBean.class);
        assertEquals(bean.getLm(), back.getLm());
        assertEquals(java.util.Arrays.asList("z", "a"), new java.util.ArrayList<>(back.getLm().keySet()));

        final String xml = N.toXml(bean);
        Assertions.assertTrue(xml.contains("<lm>{&quot;z&quot;: [1], &quot;a&quot;: [2]}</lm>"), xml);
        assertEquals(bean.getLm(), N.fromXml(xml, MultimapBean.class).getLm());
    }
}
