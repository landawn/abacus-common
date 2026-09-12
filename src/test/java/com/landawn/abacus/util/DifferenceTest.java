package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;

public class DifferenceTest extends DifferenceTestSupport {

    @Test
    public void testOf() {
        Difference<List<String>, List<String>> dups = Difference.of(Arrays.asList("a", "a", "a", "b"), Arrays.asList("a", "b", "b"));
        assertEquals(Arrays.asList("a", "b"), dups.common());
        assertEquals(Arrays.asList("a", "a"), dups.onlyOnLeft());
        assertEquals(Arrays.asList("b"), dups.onlyOnRight());

        Difference<List<String>, List<String>> allDups = Difference.of(Arrays.asList("a", "a", "a", "a"), Arrays.asList("a", "a", "b", "b"));
        assertEquals(Arrays.asList("a", "a"), allDups.common());
        assertEquals(Arrays.asList("a", "a"), allDups.onlyOnLeft());
        assertEquals(Arrays.asList("b", "b"), allDups.onlyOnRight());

        Difference<List<Integer>, List<Integer>> single = Difference.of(Arrays.asList(1), Arrays.asList(1));
        assertEquals(Arrays.asList(1), single.common());
        assertTrue(single.areEqual());

        Difference<List<Integer>, List<Integer>> mixed = Difference.of(Arrays.asList(1, 2, 3, 2), Arrays.asList(2, 3, 4));
        assertEquals(Arrays.asList(2, 3), mixed.common());
        assertEquals(Arrays.asList(1, 2), mixed.onlyOnLeft());
        assertEquals(Arrays.asList(4), mixed.onlyOnRight());
        assertFalse(mixed.areEqual());
        assertTrue(Difference.of(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3)).areEqual());
        assertTrue(Difference.of(Arrays.asList(1, 2, 3), Arrays.asList(3, 2, 1)).areEqual());
        assertTrue(Difference.of(IntList.of(1, 2, 3), IntList.of(3, 2, 1)).areEqual());
        assertTrue(Difference.of(BooleanList.of(true, false), BooleanList.of(false, true)).areEqual());
    }

    @Test
    public void testOf_LargeAndSpecialValues() {
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            list1.add(i);
        }
        for (int i = 5000; i < 15000; i++) {
            list2.add(i);
        }
        Difference<List<Integer>, List<Integer>> large = Difference.of(list1, list2);
        assertEquals(5000, large.common().size());
        assertEquals(5000, large.onlyOnLeft().size());
        assertEquals(5000, large.onlyOnRight().size());
        assertTrue(large.common().contains(7500));
        assertTrue(large.onlyOnLeft().contains(2500));
        assertTrue(large.onlyOnRight().contains(12500));

        Difference<FloatList, FloatList> nan = Difference.of(FloatList.of(1.0f, Float.NaN, 3.0f), FloatList.of(Float.NaN, 3.0f, 4.0f));
        assertEquals(FloatList.of(Float.NaN, 3.0f), nan.common());
        assertEquals(FloatList.of(1.0f), nan.onlyOnLeft());
        assertEquals(FloatList.of(4.0f), nan.onlyOnRight());

        Difference<DoubleList, DoubleList> inf = Difference.of(DoubleList.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY),
                DoubleList.of(Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY));
        assertEquals(DoubleList.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), inf.common());
        assertEquals(DoubleList.of(0.0), inf.onlyOnLeft());
        assertEquals(DoubleList.of(1.0), inf.onlyOnRight());

        Difference<IntList, IntList> extremes = Difference.of(IntList.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE),
                IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE));
        assertEquals(IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), extremes.common());
        assertEquals(IntList.of(-1, 1), extremes.onlyOnLeft());
        assertTrue(extremes.onlyOnRight().isEmpty());
    }

    @Test
    public void testEqualsHashCodeToString() {
        Difference<List<String>, List<String>> a = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        Difference<List<String>, List<String>> b = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        Difference<List<String>, List<String>> c = Difference.of(Arrays.asList("x", "y"), Arrays.asList("y", "z"));
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "not a difference");
        assertEquals("{areEqual=false, common=[b], onlyOnLeft=[a], onlyOnRight=[c]}", a.toString());

        Difference<List<Integer>, List<Integer>> ints = Difference.of(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4));
        assertTrue(ints.toString().contains("areEqual="));
        assertTrue(ints.toString().contains("common="));
        assertNotEquals(ints, Difference.of(Arrays.asList(1, 2, 5), Arrays.asList(2, 3, 4)));
    }

    @Test
    public void testMapAndBeanDifference() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("string", "value1");
        map1.put("integer", 42);
        map1.put("double", 3.14);
        map1.put("boolean", true);
        map1.put("list", Arrays.asList(1, 2, 3));
        map1.put("null", null);
        map1.put("onlyInFirst", "unique");
        Map<String, Object> map2 = new HashMap<>();
        map2.put("string", "value2");
        map2.put("integer", 42);
        map2.put("double", 3.14159);
        map2.put("boolean", true);
        map2.put("list", Arrays.asList(1, 2, 3));
        map2.put("null", null);
        map2.put("onlyInSecond", "unique");
        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> mapDiff = MapDifference.of(map1, map2);
        assertEquals(4, mapDiff.common().size());
        assertTrue(mapDiff.onlyOnLeft().containsKey("onlyInFirst"));
        assertTrue(mapDiff.onlyOnRight().containsKey("onlyInSecond"));
        assertEquals(Pair.of("value1", "value2"), mapDiff.differentValues().get("string"));
        assertEquals(Pair.of(3.14, 3.14159), mapDiff.differentValues().get("double"));

        Map<String, Integer> eq1 = Map.of("a", 1, "b", 2);
        Map<String, Integer> eq2 = Map.of("a", 1, "b", 2);
        assertTrue(MapDifference.of(eq1, eq2).areEqual());
        assertTrue(MapDifference.of(eq1, eq2).differentValues().isEmpty());
        Map<String, Integer> ne = Map.of("a", 2, "b", 2);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> valueDiff = MapDifference.of(Map.of("a", 1, "b", 2), ne);
        assertFalse(valueDiff.areEqual());
        assertEquals(Pair.of(1, 2), valueDiff.differentValues().get("a"));
        assertEquals(Integer.valueOf(1), valueDiff.differentValues().get("a").left());
        assertEquals(Integer.valueOf(2), valueDiff.differentValues().get("a").right());
        assertTrue(valueDiff.toString().contains("differentValues="));
        assertEquals(valueDiff, MapDifference.of(Map.of("a", 1, "b", 2), ne));
        assertEquals(valueDiff.hashCode(), MapDifference.of(Map.of("a", 1, "b", 2), ne).hashCode());

        Map<String, Integer> allDifferentLeft = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> allDifferentRight = CommonUtil.asMap("a", 10, "b", 20, "c", 30);
        assertEquals(3, MapDifference.of(allDifferentLeft, allDifferentRight).differentValues().size());

        Map<String, String> nulls1 = new HashMap<>();
        nulls1.put("a", "hello");
        nulls1.put("b", null);
        Map<String, String> nulls2 = new HashMap<>();
        nulls2.put("a", null);
        nulls2.put("b", "world");
        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> nullDiff = MapDifference.of(nulls1, nulls2);
        assertEquals(Pair.of("hello", null), nullDiff.differentValues().get("a"));
        assertEquals(Pair.of(null, "world"), nullDiff.differentValues().get("b"));

        List<Map<String, Object>> a = List.of(Map.of("id", 1));
        List<Map<String, Object>> b = List.of(Map.of("id", 1));
        Difference<?, ?> plain = Difference.of(a, b);
        Difference<?, ?> keyed = MapDifference.of(a, b, m -> m.get("id"));
        assertFalse(plain.equals(keyed));
        assertFalse(keyed.equals(plain));
        assertEquals(plain, Difference.of(a, b));

        Account left = new Account();
        left.setLastUpdateTime(Dates.currentTimestampPlus(1, TimeUnit.DAYS));
        left.setCreatedTime(Dates.currentTimestampPlus(1, TimeUnit.DAYS));
        Account right = new Account();
        right.setLastUpdateTime(Dates.currentTimestamp());
        right.setCreatedTime(Dates.currentTimestamp());
        assertFalse(BeanDifference.of(left, right).differentValues().containsKey("lastUpdateTime"));
        assertTrue(BeanDifference.of(left, right, CommonUtil.toList("lastUpdateTime", "createdTime")).differentValues().containsKey("lastUpdateTime"));

        List<Account> listA = Beans.newRandomBeanList(Account.class, 10);
        List<Account> listB = Beans.newRandomBeanList(Account.class, 10);
        listA.get(0).setGUI(listB.get(3).getGUI());
        listA.get(5).setGUI(listB.get(7).getGUI());
        listA.get(7).setGUI(listB.get(1).getGUI());
        listA.set(4, Beans.copy(listB.get(2)));
        listA.set(6, Beans.copy(listB.get(8)));
        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> beanDiff = BeanDifference
                .of(listA, listB, Account::getGUI);
        assertNotNull(beanDiff.differentValues());
        for (int i = 0; i < listA.size(); i++) {
            listA.set(i, Beans.copy(listB.get(i)));
        }
        assertTrue(BeanDifference.of(listA, listB, Account::getGUI).areEqual());
    }

    @Test
    public void testBeanNullProperties() {
        NullPropBean allNull = new NullPropBean(null, null, null);
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> vsNull = BeanDifference.of(allNull, (Object) null);
        assertFalse(vsNull.areEqual());
        assertEquals(3, vsNull.onlyOnLeft().size());
        assertNull(vsNull.onlyOnLeft().get("email"));
        assertTrue(BeanDifference.of(allNull, new NullPropBean(null, null, null)).areEqual());
        assertTrue(BeanDifference.of((Object) null, (Object) null).areEqual());
        assertEquals(Pair.of(null, "e"), BeanDifference.of(new NullPropBean("a", null, 1), new NullPropBean("a", "e", 1)).differentValues().get("email"));

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> rejecting = BeanDifference.ofByProps(allNull, allNull,
                (name, v1, v2) -> false);
        assertEquals(3, rejecting.differentValues().size());
        assertEquals(Pair.of(null, null), rejecting.differentValues().get("email"));
        assertTrue(BeanDifference.ofByProps(allNull, allNull, (name, v1, v2) -> true).areEqual());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> selected = BeanDifference.ofByProps(allNull, allNull,
                Arrays.asList("name", "email", "score"), (name, v1, v2) -> false);
        assertEquals(3, selected.differentValues().size());
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> byDefault = BeanDifference.of(allNull, allNull,
                Arrays.asList("name", "email", "score"));
        assertEquals(3, byDefault.common().size());
        assertNull(byDefault.common().get("email"));

        List<String> seen = new ArrayList<>();
        BeanDifference.ofByProps(new NullPropBean("a", null, 1), new NullPropBean("a", null, 2), (name, v1, v2) -> {
            seen.add(name + "=" + v1 + "/" + v2);
            return CommonUtil.equals(v1, v2);
        });
        assertTrue(seen.contains("name=a/a"));
        assertTrue(seen.contains("email=null/null"));
        assertTrue(seen.contains("score=1/2"));
    }

    @Test
    public void testKeysToCompare() {
        Map<String, Integer> m1 = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> m2 = CommonUtil.asMap("a", 9, "b", 2, "d", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> viaList = MapDifference.of(m1, m2,
                Arrays.asList("a", "b", "d"));
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> viaSet = MapDifference.of(m1, m2,
                new LinkedHashSet<>(Arrays.asList("a", "b", "d")));
        assertEquals(viaSet.common(), viaList.common());
        assertEquals(CommonUtil.asMap("b", 2), viaList.common());
        assertEquals(CommonUtil.asMap("d", 4), viaList.onlyOnRight());
        assertEquals(Pair.of(1, 9), viaList.differentValues().get("a"));

        TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.add("A");
        assertEquals(Pair.of(1, 9),
                MapDifference.of(CommonUtil.asMap("a", 1, "b", 2), CommonUtil.asMap("a", 9, "b", 2), caseInsensitive).differentValues().get("a"));

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> viaNull = MapDifference
                .of(CommonUtil.asMap("a", 1, "b", 2), CommonUtil.asMap("a", 9, "b", 2), (Collection<String>) null);
        assertEquals(viaNull.common(),
                MapDifference.of(CommonUtil.asMap("a", 1, "b", 2), CommonUtil.asMap("a", 9, "b", 2), Collections.<String> emptyList()).common());
        assertEquals(Pair.of(1, 9), MapDifference.of(CommonUtil.asMap("a", 1), CommonUtil.asMap("a", 9), Arrays.asList("a", null)).differentValues().get("a"));
        assertEquals(CommonUtil.asMap("a", 1),
                MapDifference.of(CommonUtil.asMap("a", 1, "b", 2), Collections.<String, Integer> emptyMap(), Arrays.asList("a")).onlyOnLeft());
        assertEquals(CommonUtil.asMap("a", 1),
                MapDifference.of(Collections.<String, Integer> emptyMap(), CommonUtil.asMap("a", 1, "b", 2), Arrays.asList("a")).onlyOnRight());

        Map<String, Integer> nullKey1 = new HashMap<>();
        nullKey1.put(null, 1);
        nullKey1.put("a", 2);
        Map<String, Integer> nullKey2 = new HashMap<>();
        nullKey2.put(null, 9);
        nullKey2.put("a", 2);
        assertTrue(MapDifference.of(nullKey1, nullKey2, List.of("a")).areEqual());
    }

    @Test
    public void testEqualsAcrossDifferenceTypes() {
        Map<String, Object> m1 = CommonUtil.asMap("a", (Object) 1);
        Map<String, Object> m2 = CommonUtil.asMap("a", (Object) 2);
        MapDifference<?, ?, ?> d1 = MapDifference.of(m1, m2);
        MapDifference<?, ?, ?> d2 = MapDifference.of(m1, m2);
        assertEquals(d1, d1);
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> b1 = BeanDifference.of(new NullPropBean("a", null, 1),
                new NullPropBean("a", null, 2));
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> b2 = BeanDifference.of(new NullPropBean("a", null, 1),
                new NullPropBean("a", null, 2));
        assertEquals(b1, b2);
        assertFalse(d1.equals(null));
        assertFalse(d1.equals("not a difference"));
    }

    @Test
    public void testBeanDifference_FuzzyPropertyNames() {
        ReviewFixes20260906LeftBean left = new ReviewFixes20260906LeftBean();
        left.setUserName("jdoe");
        ReviewFixes20260906RightBean right = new ReviewFixes20260906RightBean();
        right.setUsername("jdoe");
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(left, right);
        assertEquals(CommonUtil.asMap("userName", "jdoe"), diff.common());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
        ReviewFixes20260906RightBean other = new ReviewFixes20260906RightBean();
        other.setUsername("someone-else");
        assertFalse(BeanDifference.of(left, other).areEqual());

        ReviewFixes20260906IgnoredLeftBean ignored = new ReviewFixes20260906IgnoredLeftBean();
        ignored.setUserName("jdoe");
        assertTrue(BeanDifference.of(ignored, right).areEqual());
        assertTrue(BeanDifference.of(ignored, other).areEqual());
    }

    @Test
    public void testCollectionBasedDifferences() {
        List<Map<String, Object>> empty = Collections.emptyList();
        List<Map<String, Object>> dups = Arrays.asList(CommonUtil.asMap("id", (Object) 1, "v", "x"), CommonUtil.asMap("id", (Object) 1, "v", "y"));
        List<Map<String, Object>> ok = Arrays.asList(CommonUtil.asMap("id", (Object) 1), CommonUtil.asMap("id", (Object) 2));
        assertThrows(IllegalStateException.class, () -> MapDifference.of(empty, dups, m -> m.get("id")));
        assertThrows(IllegalStateException.class, () -> MapDifference.of(dups, empty, m -> m.get("id")));
        assertThrows(IllegalStateException.class, () -> MapDifference.of(dups, ok, m -> m.get("id")));
        assertEquals(2, MapDifference.of(empty, ok, m -> m.get("id")).onlyOnRight().size());
        assertTrue(MapDifference.of(empty, empty, (Function<Map<String, Object>, Object>) m -> m.get("id")).areEqual());

        List<Map<String, Object>> leftMaps = Arrays.asList(CommonUtil.asMap("id", (Object) 1, "n", "x"));
        List<Map<String, Object>> rightMaps = Arrays.asList(CommonUtil.asMap("id", (Object) 2, "n", "y"));
        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, ?> collectionMapDiff = MapDifference.of(leftMaps, rightMaps, m -> m.get("id"));
        assertTrue(collectionMapDiff.common() instanceof List);
        assertEquals(leftMaps, collectionMapDiff.onlyOnLeft());
        assertTrue(collectionMapDiff.differentValues() instanceof Map);

        List<DiffIncludedBean> leftBeans = Arrays.asList(newDiffIncludedBean("g1"));
        List<DiffIncludedBean> rightBeans = Arrays.asList(newDiffIncludedBean("g2"));
        BeanDifference<List<DiffIncludedBean>, List<DiffIncludedBean>, ?> collectionBeanDiff = BeanDifference.of(leftBeans, rightBeans,
                DiffIncludedBean::getValue);
        assertTrue(collectionBeanDiff.onlyOnLeft() instanceof List);
        assertEquals(1, collectionBeanDiff.onlyOnLeft().size());
        assertTrue(MapDifference.of(CommonUtil.asMap("a", 1, "b", 2), CommonUtil.asMap("b", 2, "c", 4)).common() instanceof Map);
        assertTrue(BeanDifference.of(newDiffIncludedBean("g1"), newDiffIncludedBean("g2")).common() instanceof Map);
    }
}
