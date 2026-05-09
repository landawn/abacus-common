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
}
