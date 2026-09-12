package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.type.Type;

import testfixtures.UtilJsonFactorySources.FilledMap;
import testfixtures.UtilJsonFactorySources.ReusedList;
import testfixtures.UtilJsonFactorySources.SourceList;

@Tag("unit")
public class JsonNestedContainersTest {
    public static class Bean {
        private List<Integer> values;

        public List<Integer> getValues() {
            return values;
        }

        public void setValues(List<Integer> values) {
            this.values = values;
        }
    }

    public static class ScalarBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    void directScalarConversionsRejectContainerCyclesBeforeFormatting() {
        List<Object> first = new ArrayList<>();
        List<Object> second = new ArrayList<>();
        first.add(second);
        second.add(first);
        JSONArray array = new JSONArray().put((Object) first);
        JSONObject object = new JSONObject().put("value", (Object) first);
        List<org.junit.jupiter.api.function.Executable> conversions = List.of(() -> JsonUtil.toList(array, String.class),
                () -> JsonUtil.unwrap(array, Type.of("List<String>")), () -> JsonUtil.unwrap(array, String[].class), () -> JsonUtil.unwrap(array, int[].class),
                () -> JsonUtil.unwrap(object, Type.of("Map<String,String>")), () -> JsonUtil.unwrap(object, ScalarBean.class));
        for (var conversion : conversions) {
            assertEquals("Cyclic JSON container conversion", assertThrows(IllegalArgumentException.class, conversion).getMessage());
        }
        assertSame(first, JsonUtil.toList(array, Object.class).get(0));
        assertEquals("[12]", JsonUtil.toList(new JSONArray().put((Object) List.of(12)), String.class).get(0));
    }

    @Test
    void allFivePathsApplyNestedTargetTypes() {
        List<String> source = new ArrayList<>(List.of("12"));
        JSONArray array = new JSONArray().put((Object) source);
        Type<List<Integer>> integers = Type.of("List<Integer>");
        assertSame(source, array.get(0));
        assertEquals(12, JsonUtil.toList(array, integers).get(0).get(0));
        List<List<Integer>> lists = JsonUtil.unwrap(array, Type.of("List<List<Integer>>"));
        assertEquals(12, lists.get(0).get(0));
        List<Integer>[] genericArray = JsonUtil.unwrap(array, Type.of("List<Integer>[]"));
        assertEquals(12, genericArray[0].get(0));
        JSONObject object = new JSONObject().put("values", (Object) source);
        Map<String, List<Integer>> map = JsonUtil.unwrap(object, Type.of("Map<String,List<Integer>>"));
        assertEquals(12, map.get("values").get(0));
        assertEquals(12, JsonUtil.unwrap(object, Bean.class).getValues().get(0));
        List<Map<String, List<Integer>>> nested = JsonUtil.toList(new JSONArray().put((Object) Map.of("values", source)), Type.of("Map<String,List<Integer>>"));
        assertEquals(12, nested.get(0).get("values").get(0));
        assertEquals(List.of("12"), source);
        assertSame(source, JsonUtil.toList(array, Object.class).get(0));
        assertNotSame(source, JsonUtil.toList(array, Type.of("List<Object>")).get(0));
        assertEquals(lists, JsonUtil.unwrap(new JSONArray("[[\"12\"]]"), Type.of("List<List<Integer>>")));
    }

    @Test
    void nullsArraysMixedLeavesCollisionsAndCycles() {
        JSONArray source = new JSONArray().put((Object) Arrays.asList(1, "12", JSONObject.NULL));
        List<List<Integer>> list = JsonUtil.toList(source, Type.of("List<Integer>"));
        assertEquals(Arrays.asList(1, 12, null), list.get(0));
        List<int[]> arrays = JsonUtil.toList(new JSONArray().put(new Object[] { JSONObject.NULL, "12" }), Type.of(int[].class));
        assertArrayEquals(new int[] { 0, 12 }, arrays.get(0));
        List<List<Integer>> fromArray = JsonUtil.toList(new JSONArray().put(new String[] { "12" }), Type.of("List<Integer>"));
        assertEquals(List.of(12), fromArray.get(0));
        List<List<Object>> objects = JsonUtil.toList(new JSONArray().put((Object) Arrays.asList(JSONObject.NULL, new JSONArray("[1]"))),
                Type.of("List<Object>"));
        assertNull(objects.get(0).get(0));
        assertEquals(List.of(1), objects.get(0).get(1));
        assertThrows(IllegalArgumentException.class,
                () -> JsonUtil.toList(new JSONArray().put((Object) Map.of("1", "a", "01", "b")), Type.of("Map<Integer,String>")));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(new JSONObject().put("1", "a").put("01", "b"), Type.of("Map<Integer,String>")));
        List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        JSONArray root = new JSONArray().put((Object) cycle);
        assertSame(cycle, JsonUtil.toList(root, Object.class).get(0));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.toList(root, Type.of("List<Integer>")));
        JSONArray nativeCycle = new JSONArray();
        nativeCycle.put(nativeCycle);
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(nativeCycle, Object.class));
        JSONObject objectCycle = new JSONObject();
        objectCycle.put("self", objectCycle);
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(objectCycle, Object.class));
        List<String> shared = List.of("12");
        List<List<List<Integer>>> repeated = JsonUtil.toList(new JSONArray().put((Object) List.of(shared, shared)), Type.of("List<List<Integer>>"));
        assertEquals(List.of(List.of(12), List.of(12)), repeated.get(0));
    }

    @Test
    void factoriesCannotReuseSourceSiblingsOutputsOrPopulatedContainers() {
        SourceList<Object> later = new SourceList<>();
        IntFunctions.registerForCollection(SourceList.class, size -> later);
        JSONArray source = new JSONArray().put((Object) List.of("12")).put((Object) later);
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.toList(source, new TypeReference<SourceList<Integer>>() {
        }.type()));
        assertTrue(later.isEmpty(), "a later source sibling must be indexed before the first factory call");
        ReusedList<Object> reused = new ReusedList<>();
        IntFunctions.registerForCollection(ReusedList.class, size -> reused);
        assertThrows(IllegalArgumentException.class,
                () -> JsonUtil.toList(new JSONArray().put((Object) List.of()).put((Object) List.of()), new TypeReference<ReusedList<Integer>>() {
                }.type()));
        FilledMap<Object, Object> filled = new FilledMap<>();
        filled.put("x", 7);
        IntFunctions.registerForMap(FilledMap.class, size -> filled);
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap(new JSONObject(), new TypeReference<FilledMap<String, Integer>>() {
        }.type()));
        assertEquals(Map.of("x", 7), filled);
    }
}
