package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

public class MapsTest extends AbstractTest {

    private Map<String, String> testMap;
    private Map<String, Integer> intMap;
    private Map<String, Object> objectMap;
    private Map<String, Map<String, String>> nestedMap;
    private Map<String, List<String>> listMap;
    private Map<String, Set<String>> setMap;

    @BeforeEach
    public void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", "value2");
        testMap.put("key3", "value3");

        intMap = new LinkedHashMap<>();
        intMap.put("one", 1);
        intMap.put("two", 2);
        intMap.put("three", 3);

        objectMap = new LinkedHashMap<>();
        objectMap.put("boolean", true);
        objectMap.put("char", 'A');
        objectMap.put("byte", (byte) 10);
        objectMap.put("short", (short) 100);
        objectMap.put("integer", 123);
        objectMap.put("long", 123456789L);
        objectMap.put("float", 12.34f);
        objectMap.put("double", 45.67d);
        objectMap.put("string", "test");

        nestedMap = new LinkedHashMap<>();
        Map<String, String> innerMap = new LinkedHashMap<>();
        innerMap.put("innerKey1", "innerValue1");
        innerMap.put("innerKey2", "innerValue2");
        nestedMap.put("outer1", innerMap);

        listMap = new LinkedHashMap<>();
        listMap.put("list1", new ArrayList<>(Arrays.asList("a", "b", "c")));

        setMap = new LinkedHashMap<>();
        setMap.put("set1", new LinkedHashSet<>(Arrays.asList("x", "y", "z")));
    }

    private static final class SimpleBean {
        private int id;
        private String value;

        SimpleBean() {
        }

        SimpleBean(int id, String value) {
            this.id = id;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static final class Address {
        private String city;
        private String zip;

        Address() {
        }

        Address(String city, String zip) {
            this.city = city;
            this.zip = zip;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }
    }

    private static final class Person {
        private String name;
        private int age;
        private Address address;

        Person() {
        }

        Person(String name, int age, Address address) {
            this.name = name;
            this.age = age;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    private static final class NestedBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static final class TestBean {
        private String name;
        private int age;
        private boolean active;
        private String nullableField;
        private NestedBean nestedBean;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String getNullableField() {
            return nullableField;
        }

        public void setNullableField(String nullableField) {
            this.nullableField = nullableField;
        }

        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }
    }

    private static final class ComplexBean {
        private Date date;
        private BigDecimal bigDecimal;
        private List<String> stringList;
        private int[] intArray;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public BigDecimal getBigDecimal() {
            return bigDecimal;
        }

        public void setBigDecimal(BigDecimal bigDecimal) {
            this.bigDecimal = bigDecimal;
        }

        public List<String> getStringList() {
            return stringList;
        }

        public void setStringList(List<String> stringList) {
            this.stringList = stringList;
        }

        public int[] getIntArray() {
            return intArray;
        }

        public void setIntArray(int[] intArray) {
            this.intArray = intArray;
        }
    }

    private static final class CircularBean {
        private String name;
        private CircularBean reference;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public CircularBean getReference() {
            return reference;
        }

        public void setReference(CircularBean reference) {
            this.reference = reference;
        }
    }

    private static final class NonInstantiableHashMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;

        private NonInstantiableHashMap(String ignored) {
        }
    }

    @Test
    public void testZipWithMapSupplier() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, LinkedHashMap::new);
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testZipWithMergeFunction() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values, Integer::sum, HashMap::new);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void testZip_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("k1", "k2", "k3").iterator();
        Iterable<Integer> values = () -> Arrays.asList(1, 2).iterator();

        Map<String, Integer> result = Maps.zip(keys, values);

        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("k1"));
        assertEquals(Integer.valueOf(2), result.get("k2"));
        assertFalse(result.containsKey("k3"));
    }

    @Test
    public void testZipWithMapSupplier_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("a", "b").iterator();
        Iterable<Integer> values = () -> Arrays.asList(10, 20, 30).iterator();

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, ignored -> new LinkedHashMap<>());

        assertTrue(result instanceof LinkedHashMap);
        assertEquals(Map.of("a", 10, "b", 20), result);
    }

    @Test
    public void testZipWithMergeFunction_NonCollectionIterables() {
        Iterable<String> keys = () -> Arrays.asList("dup", "dup", "tail").iterator();
        Iterable<Integer> values = () -> Arrays.asList(1, 2, 3).iterator();

        LinkedHashMap<String, Integer> result = Maps.zip(keys, values, Integer::sum, ignored -> new LinkedHashMap<>());

        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("dup"));
        assertEquals(Integer.valueOf(3), result.get("tail"));
    }

    @Test
    public void testZipWithDefaults() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values, "default", 99);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("default"));

        List<String> longerKeys = Arrays.asList("a", "b", "c");
        List<Integer> shorterValues = Arrays.asList(1, 2);
        Map<String, Integer> result2 = Maps.zip(longerKeys, shorterValues, "default", 99);
        assertEquals(3, result2.size());
        assertEquals(Integer.valueOf(99), result2.get("c"));
    }

    @Test
    public void testZipWithDefaultsAndMergeFunction() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3, 4);

        BiFunction<Integer, Integer, Integer> merger = Integer::sum;
        IntFunction<HashMap<String, Integer>> supplier = HashMap::new;

        Map<String, Integer> result = Maps.zip(keys, values, "default", 0, merger, supplier);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(7), result.get("default"));
    }

    @Test
    public void testZip_iterables_withSupplier() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);
        IntFunction<LinkedHashMap<String, Integer>> supplier = LinkedHashMap::new;
        Map<String, Integer> result = Maps.zip(keys, values, supplier);
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(Map.of("a", 1, "b", 2), result);
    }

    @Test
    public void testZip_iterables_withMergeAndSupplier() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;
        IntFunction<HashMap<String, Integer>> supplier = HashMap::new;

        Map<String, Integer> result = Maps.zip(keys, values, merger, supplier);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1 + 3);
        expected.put("b", 2);
        assertEquals(expected, result);
    }

    @Test
    public void testZip_iterables_withDefaults() {
        List<String> keys = Arrays.asList("a");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = Maps.zip(keys, values, "defaultKey", 99);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("defaultKey", 2);
        assertEquals(expected, result);

        List<String> keys2 = Arrays.asList("x", "y");
        List<Integer> values2 = Arrays.asList(10);
        Map<String, Integer> result2 = Maps.zip(keys2, values2, "defaultKey", 99);
        Map<String, Integer> expected2 = new HashMap<>();
        expected2.put("x", 10);
        expected2.put("y", 99);
        assertEquals(expected2, result2);
    }

    @Test
    public void testZip_iterables_withDefaultsAndMerge() {
        List<String> keys = Arrays.asList("a", "defaultKey");
        List<Integer> values = Arrays.asList(1, 2, 3);
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;
        Map<String, Integer> result = Maps.zip(keys, values, "defaultKey", 99, merger, HashMap::new);

        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("defaultKey", 5);
        assertEquals(expected, result);

        List<String> keys2 = Arrays.asList("k1", "k2", "k3");
        List<Integer> values2 = Arrays.asList(10, 20);
        Map<String, Integer> result2 = Maps.zip(keys2, values2, "defaultKeyX", 99, merger, HashMap::new);
        Map<String, Integer> expected2 = new HashMap<>();
        expected2.put("k1", 10);
        expected2.put("k2", 20);
        expected2.put("k3", 99);
        assertEquals(expected2, result2);
    }

    @Test
    public void testMapSupplierBehavior() {
        final boolean[] supplierCalled = { false };
        IntFunction<HashMap<String, String>> trackingSupplier = size -> {
            supplierCalled[0] = true;
            return new HashMap<>(size);
        };

        List<String> keys = Arrays.asList("a", "b");
        List<String> values = Arrays.asList("1", "2");

        Maps.zip(keys, values, trackingSupplier);
        assertTrue(supplierCalled[0]);
    }

    @Test
    public void testZip_UnequalSizes() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testZip_WithMergeAndSupplier() {
        List<String> keys = Arrays.asList("a", "b", "a");
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = Maps.zip(keys, values, (v1, v2) -> v1 + v2, HashMap::new);
        assertEquals(Integer.valueOf(4), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    @Test
    public void testZip_WithDefaultsAndMerge() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = Maps.zip(keys, values, "default", 0, (v1, v2) -> v1 + v2, HashMap::new);
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("default"));
    }

    @Test
    public void testZip() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
        assertEquals(Integer.valueOf(3), result.get("c"));

        List<String> longerKeys = Arrays.asList("a", "b", "c", "d");
        Map<String, Integer> result2 = Maps.zip(longerKeys, values);
        assertEquals(3, result2.size());

        assertTrue(Maps.zip(null, values).isEmpty());
        assertTrue(Maps.zip(keys, null).isEmpty());
        assertTrue(Maps.zip(new ArrayList<>(), values).isEmpty());
    }

    @Test
    public void testZipWithEmptyCollections() {
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result1 = Maps.zip(emptyKeys, values);
        assertTrue(result1.isEmpty());

        Map<String, Integer> result2 = Maps.zip(emptyKeys, values, (v1, v2) -> v1 + v2, HashMap::new);
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testZip_iterables() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        assertEquals(expected, Maps.zip(keys, values));

        assertTrue(Maps.zip(null, values).isEmpty());
        assertTrue(Maps.zip(keys, null).isEmpty());
        assertTrue(Maps.zip(Arrays.asList(), values).isEmpty());

        List<String> shortKeys = Arrays.asList("a");
        assertEquals(Map.of("a", 1), Maps.zip(shortKeys, values));
    }

    @Test
    public void testZip_BasicCase() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);

        Map<String, Integer> result = Maps.zip(keys, values);
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testZip_emptyValues_withDefaultAndSupplier() {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        Map<String, Integer> result = Maps.zip(keys, values, "defaultKey", 0, Integer::sum, HashMap::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = Maps.newEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());

        entry.setValue(200);
        assertEquals(Integer.valueOf(200), entry.getValue());
    }

    @Test
    public void testNewEntry_values() {
        Map.Entry<String, Integer> entry = Maps.newEntry("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue().intValue());
    }

    @Test
    public void testNewEntry_nullValues() {
        Map.Entry<String, String> entry = Maps.newEntry(null, null);
        assertNull(entry.getKey());
        assertNull(entry.getValue());
    }

    @Test
    public void testNewImmutableEntry_values() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 123);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue().intValue());
    }

    @Test
    public void testNewImmutableEntry() {
        ImmutableEntry<String, Integer> entry = Maps.newImmutableEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());

        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testNewTargetMap_PreservesSortedComparator() {
        TreeMap<String, Integer> source = new TreeMap<>(Comparator.reverseOrder());
        source.put("b", 2);
        source.put("a", 1);

        Map<?, ?> target = Maps.newTargetMap(source, 4);

        assertTrue(target instanceof TreeMap);
        assertEquals(source.comparator(), ((TreeMap<?, ?>) target).comparator());
    }

    @Test
    public void testNewTargetMapAndNewOrderingMap_FallbackForNonInstantiableMap() {
        Map<String, Integer> source = new NonInstantiableHashMap<>("blocked");
        source.put("a", 1);

        Map<?, ?> target = Maps.newTargetMap(source, 3);
        Map<?, ?> ordering = Maps.newOrderingMap(source);

        assertEquals(HashMap.class, target.getClass());
        assertTrue(target.isEmpty());
        assertEquals(LinkedHashMap.class, ordering.getClass());
        assertTrue(ordering.isEmpty());
    }

    @Test
    public void testNewOrderingMap_SortedAndUnmodifiableMap() {
        TreeMap<String, Integer> sorted = new TreeMap<>(Comparator.reverseOrder());
        sorted.put("b", 2);
        sorted.put("a", 1);

        Map<?, ?> sortedResult = Maps.newOrderingMap(sorted);
        Map<?, ?> fallbackResult = Maps.newOrderingMap(Collections.unmodifiableMap(new LinkedHashMap<>(Map.of("x", 1))));

        assertTrue(sortedResult instanceof LinkedHashMap);
        assertEquals(LinkedHashMap.class, fallbackResult.getClass());
    }

    @Test
    public void testKeys() {
        Set<String> keys = Maps.keySet(testMap);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));

        assertTrue(Maps.keySet(null).isEmpty());
        assertTrue(Maps.keySet(new HashMap<>()).isEmpty());
    }

    @Test
    public void testKeySet_nonEmptyMap() {
        Set<String> keys = Maps.keySet(testMap);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
    }

    @Test
    public void testKeySet_nullMap() {
        assertTrue(Maps.keySet(null).isEmpty());
    }

    @Test
    public void testKeySet_NullMap() {
        Set<String> keys = Maps.keySet(null);
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void testValues() {
        Collection<String> values = Maps.values(testMap);
        assertEquals(3, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));

        assertTrue(Maps.values(null).isEmpty());
        assertTrue(Maps.values(new HashMap<>()).isEmpty());
    }

    @Test
    public void testValues_nonEmptyMap() {
        Collection<String> vals = Maps.values(testMap);
        assertEquals(3, vals.size());
        assertTrue(vals.contains("value1"));
    }

    @Test
    public void testValues_nullMap() {
        assertTrue(Maps.values(null).isEmpty());
    }

    @Test
    public void testValues_NullMap() {
        Collection<String> values = Maps.values(null);
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    // zip with empty keys/values and mapSupplier returns empty map
    @Test
    public void testZip_emptyKeys_withMapSupplier() {
        List<String> emptyKeys = new ArrayList<>();
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = Maps.zip(emptyKeys, values, HashMap::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEntrySet() {
        Set<Map.Entry<String, String>> entries = Maps.entrySet(testMap);
        assertEquals(3, entries.size());

        assertTrue(Maps.entrySet(null).isEmpty());
        assertTrue(Maps.entrySet(new HashMap<>()).isEmpty());
    }

    @Test
    public void testEntrySet_nonEmptyMap() {
        Set<Map.Entry<String, String>> entries = Maps.entrySet(testMap);
        assertEquals(3, entries.size());
    }

    @Test
    public void testEntrySet_nullMap() {
        assertTrue(Maps.entrySet(null).isEmpty());
    }

    @Test
    public void testEntrySet_NullMap() {
        Set<Map.Entry<String, Integer>> entries = Maps.entrySet(null);
        assertNotNull(entries);
        assertTrue(entries.isEmpty());
    }

    @Test
    public void testGet() {
        Nullable<String> result = Maps.getIfExists(testMap, "key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());

        Nullable<String> missing = Maps.getIfExists(testMap, "missing");
        assertFalse(missing.isPresent());

        objectMap.put("nullKey", null);
        Nullable<Object> nullResult = Maps.getIfExists(objectMap, "nullKey");
        assertTrue(nullResult.isPresent());
        assertNull(nullResult.get());

        assertFalse(Maps.getIfExists(null, "key").isPresent());
        assertFalse(Maps.getIfExists(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetNested() {
        Nullable<String> result = Maps.getIfExists(nestedMap, "outer1", "innerKey1");
        assertTrue(result.isPresent());
        assertEquals("innerValue1", result.get());

        assertFalse(Maps.getIfExists(nestedMap, "missing", "innerKey1").isPresent());

        assertFalse(Maps.getIfExists(nestedMap, "outer1", "missing").isPresent());

        assertFalse(Maps.getIfExists(null, "key", "key2").isPresent());
    }

    @Test
    public void testGetWithNullValues() {
        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put("key1", null);
        mapWithNulls.put("key2", "value2");

        Nullable<String> result1 = Maps.getIfExists(mapWithNulls, "key1");
        assertTrue(result1.isPresent());
        assertNull(result1.get());

        Nullable<String> result2 = Maps.getIfExists(mapWithNulls, "key2");
        assertTrue(result2.isPresent());
        assertEquals("value2", result2.get());

        Nullable<String> result3 = Maps.getIfExists(mapWithNulls, "missing");
        assertFalse(result3.isPresent());
    }

    @Test
    public void testNullMapHandling() {
        Map<String, String> nullMap = null;

        assertFalse(Maps.getIfExists(nullMap, "key").isPresent());
        assertTrue(Maps.keySet(nullMap).isEmpty());
        assertTrue(Maps.values(nullMap).isEmpty());
        assertTrue(Maps.entrySet(nullMap).isEmpty());
        assertTrue(Maps.filter(nullMap, e -> true).isEmpty());
        assertTrue(Maps.filterByKey(nullMap, k -> true).isEmpty());
        assertTrue(Maps.filterByValue(nullMap, v -> true).isEmpty());
        assertTrue(Maps.invert(nullMap).isEmpty());
        assertTrue(Maps.flatInvert((Map<String, Collection<String>>) null).isEmpty());
        assertTrue(Maps.intersection(nullMap, new HashMap<>()).isEmpty());
    }

    @Test
    public void testGet_nullable() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        map.put("b", null);

        assertEquals(Nullable.of("apple"), Maps.getIfExists(map, "a"));
        assertEquals(Nullable.of(null), Maps.getIfExists(map, "b"));
        assertEquals(Nullable.empty(), Maps.getIfExists(map, "c"));
        assertEquals(Nullable.empty(), Maps.getIfExists(null, "a"));
    }

    @Test
    public void testGet_nested_nullable() {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put("x", 10);
        innerMap.put("y", null);
        map.put("outer", innerMap);

        assertEquals(Nullable.of(10), Maps.getIfExists(map, "outer", "x"));
        assertEquals(Nullable.of(null), Maps.getIfExists(map, "outer", "y"));
        assertEquals(Nullable.empty(), Maps.getIfExists(map, "outer", "z"));
        assertEquals(Nullable.empty(), Maps.getIfExists(map, "otherOuter", "x"));
        assertEquals(Nullable.empty(), Maps.getIfExists(null, "outer", "x"));
    }

    @Test
    public void testNullHandlingInAllMethods() {
        Map<String, String> nullMap = null;
        Map<String, String> emptyMap = new HashMap<>();

        assertFalse(Maps.getIfExists(nullMap, "key").isPresent());
        assertTrue(Maps.keySet(nullMap).isEmpty());
        assertTrue(Maps.values(nullMap).isEmpty());
        assertTrue(Maps.entrySet(nullMap).isEmpty());
        assertTrue(Maps.filter(nullMap, e -> true).isEmpty());
        assertTrue(Maps.filterByKey(nullMap, k -> true).isEmpty());
        assertTrue(Maps.filterByValue(nullMap, v -> true).isEmpty());
        assertTrue(Maps.invert(nullMap).isEmpty());
        assertTrue(Maps.flatInvert((Map<String, Collection<String>>) null).isEmpty());
        assertTrue(Maps.intersection(nullMap, emptyMap).isEmpty());

        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put("null1", null);
        mapWithNulls.put("null2", null);
        mapWithNulls.put("notNull", "value");

        Map<String, String> inverted = Maps.invert(mapWithNulls);
        assertEquals(2, inverted.size());
        assertTrue(inverted.containsKey(null));
        assertEquals("notNull", inverted.get("value"));
    }

    // ---- Additional tests for previously untested methods/overloads ----

    @Test
    public void testGetIfExists_threeKeys() {
        Map<String, Map<String, Map<String, Integer>>> tripleNested = new HashMap<>();
        Map<String, Map<String, Integer>> middle = new HashMap<>();
        Map<String, Integer> inner = new HashMap<>();
        inner.put("val", 42);
        inner.put("nullVal", null);
        middle.put("mid", inner);
        tripleNested.put("top", middle);

        Nullable<Integer> result = Maps.getIfExists(tripleNested, "top", "mid", "val");
        assertTrue(result.isPresent());
        assertEquals(42, result.get().intValue());

        Nullable<Integer> nullResult = Maps.getIfExists(tripleNested, "top", "mid", "nullVal");
        assertTrue(nullResult.isPresent());
        assertNull(nullResult.get());

        assertFalse(Maps.getIfExists(tripleNested, "top", "mid", "missing").isPresent());
        assertFalse(Maps.getIfExists(tripleNested, "top", "missing", "val").isPresent());
        assertFalse(Maps.getIfExists(tripleNested, "missing", "mid", "val").isPresent());
        assertFalse(Maps.getIfExists((Map<String, Map<String, Map<String, Integer>>>) null, "top", "mid", "val").isPresent());
    }

    @Test
    public void testGetByPath_withTargetType() {
        Map<String, Object> map = Map.of("count", "123");
        assertEquals(Integer.valueOf(123), Maps.getByPathAs(map, "count", Integer.class));
    }

    @Test
    public void testGetByPath_withDefaultValue() {
        Map<String, Object> map = Map.of("name", "Test");
        assertEquals("Test", Maps.getByPathOrDefault(map, "name", "Default"));
        assertEquals("Default", Maps.getByPathOrDefault(map, "address", "Default"));
        assertEquals(Integer.valueOf(123), Maps.getByPathOrDefault(map, "name_no", 123));
    }

    // getByPathAs: value found and is already correct type -> returns directly (L1111)
    @Test
    public void testGetByPathAs_ValueAlreadyCorrectType() {
        Map<String, Object> map = new HashMap<>();
        map.put("score", 42);
        Integer result = Maps.getByPathAs(map, "score", Integer.class);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void test_getByPath() {
        Map map = CommonUtil.asMap("key1", "val1");
        assertEquals("val1", Maps.getByPath(map, "key1"));

        map = CommonUtil.asMap("key1", CommonUtil.toList("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = CommonUtil.asMap("key1", CommonUtil.toSet("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = CommonUtil.asMap("key1", CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", "val2")));
        assertEquals("val2", Maps.getByPath(map, "key1[0][1]"));

        map = CommonUtil.asMap("key1", CommonUtil.toSet(CommonUtil.toList(CommonUtil.toSet("val1"))));
        assertEquals("val1", Maps.getByPath(map, "key1[0][0][0]"));

        map = CommonUtil.asMap("key1", CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", "val22"))));
        assertEquals("val22", Maps.getByPath(map, "key1[0][1].key2"));

        map = CommonUtil.asMap("key1",
                CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.toList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));

        map = CommonUtil.asMap("key1",
                CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.toList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));

        map = CommonUtil.asMap("key1",
                CommonUtil.toList(CommonUtil.toLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.toList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][1].key22[1].key3"));

        map = CommonUtil.asMap("key1", CommonUtil.asMap("key2", null));
        assertNull(Maps.getByPath(map, "key1.key2.key3"));
    }

    @Test
    public void testGetByPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "val1");

        Map<String, Object> nested = new HashMap<>();
        nested.put("key2", "val22");
        map.put("nested", nested);

        assertEquals("val1", Maps.getByPath(map, "key1"));

        assertEquals("val22", Maps.getByPath(map, "nested.key2"));

        assertNull(Maps.getByPath(map, "missing"));
        assertNull(Maps.getByPath(map, "nested.missing"));

        List<String> list = Arrays.asList("a", "b", "c");
        map.put("array", list);
        assertEquals("b", Maps.getByPath(map, "array[1]"));

        List<Map<String, Object>> complexList = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("prop", "value");
        complexList.add(item);
        map.put("complex", complexList);
        assertEquals("value", Maps.getByPath(map, "complex[0].prop"));
    }

    @Test
    public void testGetByPathWithClass() {
        Map<String, Object> map = new HashMap<>();
        map.put("int", "123");

        Integer result = Maps.getByPathAs(map, "int", Integer.class);
        assertEquals(Integer.valueOf(123), result);

        assertNull(Maps.getByPathAs(map, "missing", String.class));
    }

    @Test
    public void testGetByPathWithDefault() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        assertEquals("value", Maps.getByPathOrDefault(map, "key", "default"));
        assertEquals("default", Maps.getByPathOrDefault(map, "missing", "default"));
    }

    @Test
    public void testComplexPathOperations() {
        Map<String, Object> complexMap = new HashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("prop", "value1");
        list.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        List<String> innerList = Arrays.asList("a", "b", "c");
        item2.put("innerList", innerList);
        list.add(item2);

        complexMap.put("list", list);

        assertEquals("value1", Maps.getByPath(complexMap, "list[0].prop"));
        assertEquals("b", Maps.getByPath(complexMap, "list[1].innerList[1]"));

        assertNull(Maps.getByPath(complexMap, "list[5].prop"));
        assertNull(Maps.getByPath(complexMap, "list[1].innerList[10]"));
    }

    @Test
    public void testGetByPath_simple() {
        Map<String, Object> map = Map.of("key1", "val1");
        assertEquals("val1", Maps.getByPath(map, "key1"));
        assertNull(Maps.getByPath(map, "key2"));
    }

    @Test
    public void testGetByPath_listAccess() {
        Map<String, Object> map = Map.of("key1", Arrays.asList("val1.0", "val1.1"));
        assertEquals("val1.0", Maps.getByPath(map, "key1[0]"));
        assertEquals("val1.1", Maps.getByPath(map, "key1[1]"));
        assertNull(Maps.getByPath(map, "key1[2]"));
    }

    @Test
    public void testGetByPath_nestedMapAndList() {
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("key3", "val33");
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("key2", Arrays.asList("val22.0", nestedMap));
        Map<String, Object> map = Map.of("key1", Arrays.asList(new LinkedHashSet<>(Arrays.asList("val1.0.0", innerMap))));

        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));
    }

    @Test
    public void testSpecialCharactersInPaths() {
        Map<String, Object> map = new HashMap<>();
        map.put("key.with.dots", "value1");
        map.put("key[with]brackets", "value2");

        assertEquals("value1", map.get("key.with.dots"));
        assertEquals("value2", map.get("key[with]brackets"));

        Map<String, Object> nested = new HashMap<>();
        nested.put("special.key", "specialValue");
        map.put("normal", nested);

        assertNull(Maps.getByPath(map, "normal.special.key"));
    }

    @Test
    public void testCompleteCodeCoverage() {

        Map<String, Object> pathMap = new HashMap<>();
        pathMap.put("", "emptyKey");
        assertEquals("emptyKey", Maps.getByPath(pathMap, ""));

        Set<String> set = new LinkedHashSet<>();
        set.add("first");
        set.add("second");
        pathMap.put("set", set);
        assertEquals("first", Maps.getByPath(pathMap, "set[0]"));

        TestBean bean = new TestBean();
        bean.setName("Test");

        Map<String, Object> lowerUnderscoreMap = Beans.beanToMap(bean, (Collection<String>) null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(lowerUnderscoreMap.containsKey("name"));

        Map<String, Object> upperUnderscoreMap = Beans.beanToMap(bean, (Collection<String>) null, NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(upperUnderscoreMap.containsKey("NAME"));
    }

    @Test
    public void testGetByPathOrDefault_nullMap() {
        assertEquals("default", Maps.getByPathOrDefault(null, "key", "default"));
    }

    @Test
    public void testGetByPathOrDefault_NestedCollectionIndexDefaultValue() {
        final Map<String, Object> map = new HashMap<>();
        map.put("items", Arrays.asList(Collections.singletonMap("count", "5")));

        assertEquals(Integer.valueOf(5), Maps.getByPathOrDefault(map, "items[0].count", 0));
        assertEquals("fallback", Maps.getByPathOrDefault(map, "items[1].count", "fallback"));
        assertEquals("fallback", Maps.getByPathOrDefault(map, "items[0].missing", "fallback"));
    }

    @Test
    public void testGetByPathAs_nullMap() {
        assertNull(Maps.getByPathAs(null, "key", String.class));
    }

    @Test
    public void testGetByPathAs_emptyMap() {
        assertNull(Maps.getByPathAs(new HashMap<>(), "key", String.class));
    }

    // getByPathAsIfExists: value found and is correct type -> returns Nullable.of(val) (L1216)
    @Test
    public void testGetByPathAsIfExists_ValueAlreadyCorrectType() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Alice");
        Nullable<String> result = Maps.getByPathAsIfExists(map, "name", String.class);
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get());
    }

    @Test
    public void testGetByPathIfExists() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        map.put("key2", CommonUtil.asMap("kk2", "123"));

        Nullable<String> result = Maps.getByPathIfExists(map, "key");
        assertTrue(result.isPresent());
        assertEquals("value", result.get());

        Nullable<Integer> result2 = Maps.getByPathAsIfExists(map, "key2.kk2", int.class);
        assertTrue(result2.isPresent());
        assertEquals(123, result2.get());

        Nullable<String> missing = Maps.getByPathIfExists(map, "missing");
        assertFalse(missing.isPresent());
    }

    @Test
    public void testGetByPathIfExists_nullMap() {
        assertFalse(Maps.getByPathIfExists(null, "key").isPresent());
    }

    @Test
    public void testGetByPathAsIfExists_nullMap() {
        assertFalse(Maps.getByPathAsIfExists(null, "key", String.class).isPresent());
    }

    @Test
    public void testGetByPathAsIfExists_missing() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(Maps.getByPathAsIfExists(map, "missing", String.class).isPresent());
    }

    @Test
    public void testGetByPathAsIfExists_conversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("num", "42");
        Nullable<Integer> result = Maps.getByPathAsIfExists(map, "num", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(42, result.get().intValue());
    }

    @Test
    public void test_getOrDefault() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);

        assertEquals(1, Maps.getOrDefaultIfAbsent(map, "a", 0).intValue());
        assertEquals(0, Maps.getOrDefaultIfAbsent(map, "d", 0).intValue());

        assertEquals(CommonUtil.toList(1, 0), Maps.getValuesOrDefault(map, CommonUtil.toList("a", "d"), 0));

        assertEquals(CommonUtil.toList(1), Maps.getValuesIfPresent(map, CommonUtil.toList("a", "d")));
    }

    @Test
    public void testGetOrDefaultIfAbsentForEach() {
        List<String> keys = Arrays.asList("key1", "missing", "key2");
        List<String> values = Maps.getValuesOrDefault(testMap, keys, "default");
        assertEquals(3, values.size());
        assertEquals(Arrays.asList("value1", "default", "value2"), values);

        List<String> defaultValues = Maps.getValuesOrDefault(new HashMap<>(), keys, "default");
        assertEquals(Arrays.asList("default", "default", "default"), defaultValues);
    }

    @Test
    public void testGetOrDefaultIfAbsent_withSupplier() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        assertEquals("value1", Maps.getOrDefaultIfAbsent(map, "key1", () -> "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(map, "missing", () -> "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent((Map<String, String>) null, "key", () -> "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(new HashMap<>(), "key", () -> "default"));
    }

    @Test
    public void testGetOrDefaultIfAbsent() {
        assertEquals("value1", Maps.getOrDefaultIfAbsent(testMap, "key1", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(testMap, "missing", "default"));

        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(testMap, "key1", (String) null));
    }

    @Test
    public void testGetOrDefaultIfAbsentNested() {
        assertEquals("innerValue1", Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "innerKey1", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "missing", "default"));
        assertEquals("default", Maps.getOrDefaultIfAbsent(nestedMap, "missing", "innerKey1", "default"));

        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(nestedMap, "outer1", "innerKey1", null));
    }

    @Test
    public void testGetOrDefaultIfAbsent_nested() {
        Map<String, Map<String, Integer>> map = new HashMap<>();
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put("x", 10);
        innerMap.put("y", null);
        map.put("outer", innerMap);
        map.put("outerNull", null);
        Integer defaultVal = 99;

        assertEquals(Integer.valueOf(10), Maps.getOrDefaultIfAbsent(map, "outer", "x", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "outer", "y", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "outer", "z", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "otherOuter", "x", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(map, "outerNull", "x", defaultVal));
        assertEquals(defaultVal, Maps.getOrDefaultIfAbsent(null, "outer", "x", defaultVal));
        assertThrows(IllegalArgumentException.class, () -> Maps.getOrDefaultIfAbsent(map, "outer", "x", null));
    }

    @Test
    public void testGetOrEmptyListIfAbsent() {
        List<String> result = Maps.getOrEmptyListIfAbsent(listMap, "list1");
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b", "c"), result);

        List<String> empty = Maps.getOrEmptyListIfAbsent(listMap, "missing");
        assertTrue(empty.isEmpty());

        listMap.put("nullList", null);
        List<String> nullResult = Maps.getOrEmptyListIfAbsent(listMap, "nullList");
        assertTrue(nullResult.isEmpty());
    }

    @Test
    public void testGetOrEmptyListIfAbsent_nullMap() {
        List<String> result = Maps.getOrEmptyListIfAbsent(null, "key");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetOrEmptySetIfAbsent() {
        Set<String> result = Maps.getOrEmptySetIfAbsent(setMap, "set1");
        assertEquals(3, result.size());
        assertTrue(result.contains("x"));

        Set<String> empty = Maps.getOrEmptySetIfAbsent(setMap, "missing");
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetOrEmptySetIfAbsent_nullMap() {
        Set<String> result = Maps.getOrEmptySetIfAbsent(null, "key");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetOrEmptyMapIfAbsent() {
        Map<String, String> result = Maps.getOrEmptyMapIfAbsent(nestedMap, "outer1");
        assertEquals(2, result.size());
        assertEquals("innerValue1", result.get("innerKey1"));

        Map<String, String> empty = Maps.getOrEmptyMapIfAbsent(nestedMap, "missing");
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetOrEmptyMapIfAbsent_nullMap() {
        Map<String, String> result = Maps.getOrEmptyMapIfAbsent(null, "key");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAndPutIfAbsent() {
        Map<String, String> map = new HashMap<>();
        String result1 = Maps.getOrPutIfAbsent(map, "key", () -> "value");
        assertEquals("value", result1);
        assertEquals("value", map.get("key"));

        String result2 = Maps.getOrPutIfAbsent(map, "key", () -> "newValue");
        assertEquals("value", result2);
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testGetOrPutIfAbsent_existingKey() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "existing");
        assertEquals("existing", Maps.getOrPutIfAbsent(map, "key", () -> "new"));
    }

    @Test
    public void testGetAndPutIfAbsent_supplier() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        Supplier<String> supplier = () -> "banana";

        assertEquals("apple", Maps.getOrPutIfAbsent(map, "a", supplier));
        assertEquals("apple", map.get("a"));

        assertNotNull(Maps.getOrPutIfAbsent(map, "b", supplier));
        assertEquals("banana", map.get("b"));

        map.put("c", null);
        assertNotNull(Maps.getOrPutIfAbsent(map, "c", supplier));
        assertEquals("banana", map.get("c"));
    }

    @Test
    public void testGetOrPutListIfAbsent_existingKey() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("key", Arrays.asList("a"));
        assertEquals(Arrays.asList("a"), Maps.getOrPutListIfAbsent(map, "key"));
    }

    @Test
    public void testGetAndPutListIfAbsent() {
        Map<String, List<String>> map = new HashMap<>();
        List<String> list = Maps.getOrPutListIfAbsent(map, "key");
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertEquals(list, map.get("key"));

        list.add("item");
        assertEquals(1, map.get("key").size());
    }

    @Test
    public void testGetOrPutSetIfAbsent_existingKey() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> existing = new HashSet<>(Arrays.asList("a"));
        map.put("key", existing);
        assertEquals(existing, Maps.getOrPutSetIfAbsent(map, "key"));
    }

    @Test
    public void testGetAndPutSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getOrPutSetIfAbsent(map, "key");
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof HashSet);
    }

    @Test
    public void testGetAndPutLinkedHashSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getOrPutLinkedHashSetIfAbsent(map, "key");
        assertNotNull(set);
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testGetOrPutLinkedHashSetIfAbsent_newKey() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> result = Maps.getOrPutLinkedHashSetIfAbsent(map, "newKey");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertTrue(result instanceof LinkedHashSet);
    }

    @Test
    public void testGetOrPutLinkedHashSetIfAbsent() {
        Map<String, Set<String>> map = new HashMap<>();
        Set<String> set = Maps.getOrPutLinkedHashSetIfAbsent(map, "key1");
        assertNotNull(set);
        assertTrue(set instanceof LinkedHashSet);
        set.add("val1");

        Set<String> sameSet = Maps.getOrPutLinkedHashSetIfAbsent(map, "key1");
        assertSame(set, sameSet);
        assertEquals(1, sameSet.size());
    }

    @Test
    public void testGetOrPutMapIfAbsent_existingKey() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> existing = new HashMap<>();
        existing.put("a", "b");
        map.put("key", existing);
        assertEquals(existing, Maps.getOrPutMapIfAbsent(map, "key"));
    }

    @Test
    public void testGetAndPutMapIfAbsent() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> innerMap = Maps.getOrPutMapIfAbsent(map, "key");
        assertNotNull(innerMap);
        assertTrue(innerMap.isEmpty());
        assertTrue(innerMap instanceof HashMap);
    }

    @Test
    public void testGetAndPutLinkedHashMapIfAbsent() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> innerMap = Maps.getOrPutLinkedHashMapIfAbsent(map, "key");
        assertNotNull(innerMap);
        assertTrue(innerMap instanceof LinkedHashMap);
    }

    @Test
    public void testGetOrPutLinkedHashMapIfAbsent_newKey() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> result = Maps.getOrPutLinkedHashMapIfAbsent(map, "newKey");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testGetOrPutLinkedHashMapIfAbsent() {
        Map<String, Map<String, Integer>> outer = new HashMap<>();
        Map<String, Integer> inner = Maps.getOrPutLinkedHashMapIfAbsent(outer, "section1");
        assertNotNull(inner);
        assertTrue(inner instanceof LinkedHashMap);
        inner.put("k", 42);

        Map<String, Integer> sameInner = Maps.getOrPutLinkedHashMapIfAbsent(outer, "section1");
        assertSame(inner, sameInner);
        assertEquals(Integer.valueOf(42), sameInner.get("k"));
    }

    @Test
    public void testGetBoolean() {
        objectMap.put("trueString", "true");
        objectMap.put("falseString", "false");
        objectMap.put("boolTrue", Boolean.TRUE);

        OptionalBoolean result1 = Maps.getAsBoolean(objectMap, "boolean");
        assertTrue(result1.isPresent());
        assertTrue(result1.get());

        OptionalBoolean result2 = Maps.getAsBoolean(objectMap, "trueString");
        assertTrue(result2.isPresent());
        assertTrue(result2.get());

        OptionalBoolean result3 = Maps.getAsBoolean(objectMap, "falseString");
        assertTrue(result3.isPresent());
        assertFalse(result3.get());

        assertFalse(Maps.getAsBoolean(objectMap, "missing").isPresent());
        assertFalse(Maps.getAsBoolean(null, "key").isPresent());
    }

    @Test
    public void testGetAsBoolean_nullMap() {
        assertFalse(Maps.getAsBoolean(null, "key").isPresent());
        assertFalse(Maps.getAsBoolean(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetBooleanWithDefault() {
        assertTrue(Maps.getAsBooleanOrDefault(objectMap, "boolean", false));
        assertTrue(Maps.getAsBooleanOrDefault(objectMap, "missing", true));
        assertFalse(Maps.getAsBooleanOrDefault(objectMap, "missing", false));
        assertFalse(Maps.getAsBooleanOrDefault(null, "key", false));
    }

    @Test
    public void testGetAsBooleanOrDefault_nullMap() {
        assertTrue(Maps.getAsBooleanOrDefault(null, "key", true));
        assertFalse(Maps.getAsBooleanOrDefault(new HashMap<>(), "key", false));
    }

    // getAsCharOrDefault: value is not Character -> calls Strings.parseChar(N.toString(val))
    @Test
    public void testGetAsCharOrDefault_NonCharValue_Converts() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "X"); // String, not Character
        char result = Maps.getAsCharOrDefault(map, "key", 'Z');
        assertEquals('X', result);
    }

    @Test
    public void testGetChar() {
        objectMap.put("charString", "B");

        OptionalChar result1 = Maps.getAsChar(objectMap, "char");
        assertTrue(result1.isPresent());
        assertEquals('A', result1.get());

        OptionalChar result2 = Maps.getAsChar(objectMap, "charString");
        assertTrue(result2.isPresent());
        assertEquals('B', result2.get());

        assertFalse(Maps.getAsChar(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsChar_nullMap() {
        assertFalse(Maps.getAsChar(null, "key").isPresent());
        assertFalse(Maps.getAsChar(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetCharWithDefault() {
        assertEquals('A', Maps.getAsCharOrDefault(objectMap, "char", 'Z'));
        assertEquals('Z', Maps.getAsCharOrDefault(objectMap, "missing", 'Z'));
    }

    @Test
    public void testGetAsCharOrDefault_nullMap() {
        assertEquals('x', Maps.getAsCharOrDefault(null, "key", 'x'));
        assertEquals('x', Maps.getAsCharOrDefault(new HashMap<>(), "key", 'x'));
    }

    // getAsByteOrDefault: value is not Number -> calls Numbers.toByte(N.toString(val))
    @Test
    public void testGetAsByteOrDefault_StringValue_Converts() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "42"); // String, not Number
        byte result = Maps.getAsByteOrDefault(map, "key", (byte) 0);
        assertEquals((byte) 42, result);
    }

    @Test
    public void testGetByte() {
        objectMap.put("byteString", "20");

        OptionalByte result1 = Maps.getAsByte(objectMap, "byte");
        assertTrue(result1.isPresent());
        assertEquals(10, result1.get());

        OptionalByte result2 = Maps.getAsByte(objectMap, "byteString");
        assertTrue(result2.isPresent());
        assertEquals(20, result2.get());

        assertFalse(Maps.getAsByte(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsByte_nullMap() {
        assertFalse(Maps.getAsByte(null, "key").isPresent());
        assertFalse(Maps.getAsByte(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetByteWithDefault() {
        assertEquals(10, Maps.getAsByteOrDefault(objectMap, "byte", (byte) 99));
        assertEquals(99, Maps.getAsByteOrDefault(objectMap, "missing", (byte) 99));
    }

    @Test
    public void testGetAsByteOrDefault_nullMap() {
        assertEquals((byte) 5, Maps.getAsByteOrDefault(null, "key", (byte) 5));
        assertEquals((byte) 5, Maps.getAsByteOrDefault(new HashMap<>(), "key", (byte) 5));
    }

    // getAsShortOrDefault: value is not Number -> calls Numbers.toShort
    @Test
    public void testGetAsShortOrDefault_StringValue_Converts() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "100"); // String
        short result = Maps.getAsShortOrDefault(map, "key", (short) 0);
        assertEquals((short) 100, result);
    }

    @Test
    public void testGetShort() {
        objectMap.put("shortString", "200");

        OptionalShort result1 = Maps.getAsShort(objectMap, "short");
        assertTrue(result1.isPresent());
        assertEquals(100, result1.get());

        OptionalShort result2 = Maps.getAsShort(objectMap, "shortString");
        assertTrue(result2.isPresent());
        assertEquals(200, result2.get());

        assertFalse(Maps.getAsShort(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsShort_nullMap() {
        assertFalse(Maps.getAsShort(null, "key").isPresent());
        assertFalse(Maps.getAsShort(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetShortWithDefault() {
        assertEquals(100, Maps.getAsShortOrDefault(objectMap, "short", (short) 999));
        assertEquals(999, Maps.getAsShortOrDefault(objectMap, "missing", (short) 999));
    }

    @Test
    public void testGetAsShortOrDefault_nullMap() {
        assertEquals((short) 10, Maps.getAsShortOrDefault(null, "key", (short) 10));
        assertEquals((short) 10, Maps.getAsShortOrDefault(new HashMap<>(), "key", (short) 10));
    }

    @Test
    public void testGetInt() {
        OptionalInt result = Maps.getAsInt(objectMap, "integer");
        assertTrue(result.isPresent());
        assertEquals(123, result.getAsInt());

        objectMap.put("intString", "456");
        OptionalInt result2 = Maps.getAsInt(objectMap, "intString");
        assertTrue(result2.isPresent());
        assertEquals(456, result2.getAsInt());

        assertFalse(Maps.getAsInt(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsInt_nullMap() {
        assertFalse(Maps.getAsInt(null, "key").isPresent());
        assertFalse(Maps.getAsInt(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetIntWithDefault() {
        assertEquals(123, Maps.getAsIntOrDefault(objectMap, "integer", 999));
        assertEquals(999, Maps.getAsIntOrDefault(objectMap, "missing", 999));
    }

    @Test
    public void testGetAsIntOrDefault_nullMap() {
        assertEquals(99, Maps.getAsIntOrDefault(null, "key", 99));
        assertEquals(99, Maps.getAsIntOrDefault(new HashMap<>(), "key", 99));
    }

    @Test
    public void testPrimitiveTypeConversions() {
        Map<String, Object> conversionMap = new HashMap<>();
        conversionMap.put("intAsString", "123");
        conversionMap.put("doubleAsString", "45.67");
        conversionMap.put("boolAsString", "true");
        conversionMap.put("boolAsInt", 1);
        conversionMap.put("hexString", "0xFF");

        assertEquals(123, Maps.getAsIntOrDefault(conversionMap, "intAsString", 0));
        assertEquals(45.67, Maps.getAsDoubleOrDefault(conversionMap, "doubleAsString", 0.0), 0.001);
        assertTrue(Maps.getAsBooleanOrDefault(conversionMap, "boolAsString", false));

        conversionMap.put("invalidNumber", "not-a-number");
        try {
            Maps.getAsInt(conversionMap, "invalidNumber");
            fail("Should throw NumberFormatException");
        } catch (Exception e) {
        }
    }

    // getAsLongOrDefault: value is not Number -> calls Numbers.toLong
    @Test
    public void testGetAsLongOrDefault_StringValue_Converts() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "99999"); // String
        long result = Maps.getAsLongOrDefault(map, "key", 0L);
        assertEquals(99999L, result);
    }

    @Test
    public void testGetLong() {
        objectMap.put("longString", "987654321");

        OptionalLong result1 = Maps.getAsLong(objectMap, "long");
        assertTrue(result1.isPresent());
        assertEquals(123456789L, result1.getAsLong());

        OptionalLong result2 = Maps.getAsLong(objectMap, "longString");
        assertTrue(result2.isPresent());
        assertEquals(987654321L, result2.getAsLong());

        assertFalse(Maps.getAsLong(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsLong_nullMap() {
        assertFalse(Maps.getAsLong(null, "key").isPresent());
        assertFalse(Maps.getAsLong(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetLongWithDefault() {
        assertEquals(123456789L, Maps.getAsLongOrDefault(objectMap, "long", 999L));
        assertEquals(999L, Maps.getAsLongOrDefault(objectMap, "missing", 999L));
    }

    @Test
    public void testEdgeCasesForTypeConversions() {
        Map<String, Object> edgeCaseMap = new HashMap<>();

        edgeCaseMap.put("maxLong", Long.MAX_VALUE);
        edgeCaseMap.put("minLong", Long.MIN_VALUE);
        edgeCaseMap.put("infinity", Double.POSITIVE_INFINITY);
        edgeCaseMap.put("nan", Double.NaN);

        assertEquals(Long.MAX_VALUE, Maps.getAsLongOrDefault(edgeCaseMap, "maxLong", 0L));
        assertEquals(Long.MIN_VALUE, Maps.getAsLongOrDefault(edgeCaseMap, "minLong", 0L));
        assertTrue(Double.isInfinite(Maps.getAsDoubleOrDefault(edgeCaseMap, "infinity", 0.0)));
        assertTrue(Double.isNaN(Maps.getAsDoubleOrDefault(edgeCaseMap, "nan", 0.0)));

        edgeCaseMap.put("scientific", "1.23e4");
        assertEquals(12300.0, Maps.getAsDoubleOrDefault(edgeCaseMap, "scientific", 0.0), 0.001);
    }

    @Test
    public void testGetAsLongOrDefault_nullMap() {
        assertEquals(100L, Maps.getAsLongOrDefault(null, "key", 100L));
        assertEquals(100L, Maps.getAsLongOrDefault(new HashMap<>(), "key", 100L));
    }

    @Test
    public void testGetFloat() {
        objectMap.put("floatString", "56.78");

        OptionalFloat result1 = Maps.getAsFloat(objectMap, "float");
        assertTrue(result1.isPresent());
        assertEquals(12.34f, result1.get(), 0.001f);

        OptionalFloat result2 = Maps.getAsFloat(objectMap, "floatString");
        assertTrue(result2.isPresent());
        assertEquals(56.78f, result2.get(), 0.001f);

        assertFalse(Maps.getAsFloat(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsFloat_nullMap() {
        assertFalse(Maps.getAsFloat(null, "key").isPresent());
        assertFalse(Maps.getAsFloat(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetFloatWithDefault() {
        assertEquals(12.34f, Maps.getAsFloatOrDefault(objectMap, "float", 99.99f), 0.001f);
        assertEquals(99.99f, Maps.getAsFloatOrDefault(objectMap, "missing", 99.99f), 0.001f);
    }

    @Test
    public void testGetAsFloatOrDefault_nullMap() {
        assertEquals(1.5f, Maps.getAsFloatOrDefault(null, "key", 1.5f));
        assertEquals(1.5f, Maps.getAsFloatOrDefault(new HashMap<>(), "key", 1.5f));
    }

    @Test
    public void testGetDouble() {
        OptionalDouble result = Maps.getAsDouble(objectMap, "double");
        assertTrue(result.isPresent());
        assertEquals(45.67, result.getAsDouble(), 0.001);

        objectMap.put("doubleString", "89.12");
        OptionalDouble result2 = Maps.getAsDouble(objectMap, "doubleString");
        assertTrue(result2.isPresent());
        assertEquals(89.12, result2.getAsDouble(), 0.001);

        assertFalse(Maps.getAsDouble(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsDouble_nullMap() {
        assertFalse(Maps.getAsDouble(null, "key").isPresent());
        assertFalse(Maps.getAsDouble(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetDoubleWithDefault() {
        assertEquals(45.67, Maps.getAsDoubleOrDefault(objectMap, "double", 99.99), 0.001);
        assertEquals(99.99, Maps.getAsDoubleOrDefault(objectMap, "missing", 99.99), 0.001);
    }

    @Test
    public void testGetAsDoubleOrDefault_nullMap() {
        assertEquals(2.5, Maps.getAsDoubleOrDefault(null, "key", 2.5));
        assertEquals(2.5, Maps.getAsDoubleOrDefault(new HashMap<>(), "key", 2.5));
    }

    // getAsStringOrDefault: value is not String -> calls N.stringOf(val)
    @Test
    public void testGetAsStringOrDefault_NonStringValue_Converts() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", 123); // Integer, not String
        String result = Maps.getAsStringOrDefault(map, "key", "default");
        assertEquals("123", result);
    }

    @Test
    public void testGetString() {
        Optional<String> result = Maps.getAsString(objectMap, "string");
        assertTrue(result.isPresent());
        assertEquals("test", result.get());

        Optional<String> intAsString = Maps.getAsString(objectMap, "integer");
        assertTrue(intAsString.isPresent());
        assertEquals("123", intAsString.get());

        assertFalse(Maps.getAsString(objectMap, "missing").isPresent());
    }

    @Test
    public void testGetAsString_nullMap() {
        assertFalse(Maps.getAsString(null, "key").isPresent());
        assertFalse(Maps.getAsString(new HashMap<>(), "key").isPresent());
    }

    @Test
    public void testGetAsStringOrDefault_nullMap() {
        assertEquals("default", Maps.getAsStringOrDefault(null, "key", "default"));
        assertEquals("default", Maps.getAsStringOrDefault(new HashMap<>(), "key", "default"));
    }

    @Test
    public void testGetStringWithDefault() {
        assertEquals("test", Maps.getAsStringOrDefault(objectMap, "string", "default"));
        assertEquals("default", Maps.getAsStringOrDefault(objectMap, "missing", "default"));

        assertThrows(IllegalArgumentException.class, () -> Maps.getAsStringOrDefault(objectMap, "missing", null));
    }

    @Test
    public void testGetAs_class_conversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("val", "123");
        Optional<Integer> result = Maps.getAs(map, "val", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(123, result.get().intValue());
    }

    @Test
    public void testGetNonNullWithClass() {
        Optional<Integer> result = Maps.getAs(objectMap, "integer", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(123), result.get());

        objectMap.put("stringInt", "456");
        Optional<Integer> converted = Maps.getAs(objectMap, "stringInt", Integer.class);
        assertTrue(converted.isPresent());
        assertEquals(Integer.valueOf(456), converted.get());

        assertFalse(Maps.getAs(objectMap, "missing", Integer.class).isPresent());
    }

    @Test
    public void testGetNonNullWithType() {
        com.landawn.abacus.type.Type<Integer> intType = com.landawn.abacus.type.Type.of(Integer.class);
        Optional<Integer> result = Maps.getAs(objectMap, "integer", intType);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(123), result.get());
    }

    @Test
    public void testGetNonNull_class() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", "text");
        map.put("i", 123);
        map.put("d", 123.45);
        map.put("n", null);

        assertEquals(Optional.of("text"), Maps.getAs(map, "s", String.class));
        assertEquals(Optional.of(123), Maps.getAs(map, "i", Integer.class));
        assertEquals(Optional.of(123.45), Maps.getAs(map, "d", Double.class));
        assertEquals(Optional.of("123"), Maps.getAs(map, "i", String.class));
        assertEquals(Optional.empty(), Maps.getAs(map, "n", String.class));
        assertEquals(Optional.empty(), Maps.getAs(map, "missing", String.class));
    }

    @Test
    public void testGetAs_class_nullMap() {
        assertFalse(Maps.getAs(null, "key", Integer.class).isPresent());
        assertFalse(Maps.getAs(new HashMap<>(), "key", Integer.class).isPresent());
    }

    @Test
    public void testGetAs_class_nullValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("val", null);
        assertFalse(Maps.getAs(map, "val", Integer.class).isPresent());
    }

    // getAs(Map, key, Type) with empty map
    @Test
    public void testGetAs_Type_EmptyMap_ReturnsEmpty() {
        Optional<String> result = Maps.getAs(new HashMap<>(), "key", com.landawn.abacus.type.TypeFactory.getType(String.class));
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetAsOrDefault_conversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("val", "123");
        assertEquals(Integer.valueOf(123), Maps.getAsOrDefault(map, "val", 0));
    }

    @Test
    public void testGetAsOrDefault_nullMap() {
        assertEquals(Integer.valueOf(99), Maps.getAsOrDefault(null, "key", 99));
    }

    @Test
    public void testGetNonNullWithDefault() {
        Integer result = Maps.getAsOrDefault(objectMap, "integer", 999);
        assertEquals(Integer.valueOf(123), result);

        Integer defaultResult = Maps.getAsOrDefault(objectMap, "missing", 999);
        assertEquals(Integer.valueOf(999), defaultResult);

        assertThrows(IllegalArgumentException.class, () -> Maps.getAsOrDefault(objectMap, "missing", (Integer) null));
    }

    @Test
    public void testGetNonNull_default() {
        Map<String, Object> map = new HashMap<>();
        map.put("s", "text");
        map.put("i", 123);
        map.put("n", null);
        String defaultStr = "default";
        Integer defaultInt = 999;

        assertEquals("text", Maps.getAsOrDefault(map, "s", defaultStr));
        assertEquals(Integer.valueOf(123), Maps.getAsOrDefault(map, "i", defaultInt));
        assertEquals("123", Maps.getAsOrDefault(map, "i", defaultStr));
        assertEquals(defaultStr, Maps.getAsOrDefault(map, "n", defaultStr));
        assertEquals(defaultStr, Maps.getAsOrDefault(map, "missing", defaultStr));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsOrDefault(map, "s", (String) null));
    }

    @Test
    public void testGetIfPresentForEach() {
        List<String> keys = Arrays.asList("key1", "missing", "key2", "key3");
        List<String> values = Maps.getValuesIfPresent(testMap, keys);
        assertEquals(3, values.size());
        assertEquals(Arrays.asList("value1", "value2", "value3"), values);

        assertTrue(Maps.getValuesIfPresent(null, keys).isEmpty());
        assertTrue(Maps.getValuesIfPresent(testMap, null).isEmpty());
    }

    @Test
    public void testGetValuesIfPresent_emptyKeys() {
        List<String> result = Maps.getValuesIfPresent(testMap, new ArrayList<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetValuesOrDefault_emptyKeys() {
        List<String> result = Maps.getValuesOrDefault(testMap, new ArrayList<>(), "default");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetValuesOrDefault_nullMap() {
        List<String> keys = Arrays.asList("a", "b");
        List<String> result = Maps.getValuesOrDefault(null, keys, "default");
        assertEquals(2, result.size());
        assertEquals("default", result.get(0));
        assertEquals("default", result.get(1));
    }

    @Test
    public void testIntersection() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "3");
        map2.put("d", "4");

        Map<String, String> result = Maps.intersection(map1, map2);
        assertEquals(2, result.size());
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
        assertNull(result.get("a"));
        assertNull(result.get("d"));

        assertTrue(Maps.intersection(null, map2).isEmpty());
        assertTrue(Maps.intersection(map1, null).isEmpty());
        assertTrue(Maps.intersection(map1, new HashMap<>()).isEmpty());
    }

    @Test
    public void testIntersection_nullMap() {
        Map<String, Integer> result = Maps.intersection(null, new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersection_emptyMap() {
        Map<String, Integer> result = Maps.intersection(new HashMap<>(), intMap);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersection_EmptyMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();

        Map<String, Integer> intersection = Maps.intersection(map1, map2);
        assertTrue(intersection.isEmpty());
    }

    @Test
    public void testIntersection_NoCommon() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        Map<String, Integer> intersection = Maps.intersection(map1, map2);
        assertTrue(intersection.isEmpty());
    }

    @Test
    public void test_difference() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("a", 1, "b", 3);

        N.println(Maps.intersection(map, map2));
        assertEquals(CommonUtil.asMap("a", 1), Maps.intersection(map, map2));

        N.println(Maps.difference(map, map2));
        assertEquals(CommonUtil.asMap("b", Pair.of(2, Nullable.of(3)), "c", Pair.of(3, Nullable.empty())), Maps.difference(map, map2));

        N.println(Maps.symmetricDifference(map, map2));

        assertEquals(CommonUtil.asMap("b", Pair.of(Nullable.of(2), Nullable.of(3)), "c", Pair.of(Nullable.of(3), Nullable.empty())),
                Maps.symmetricDifference(map, map2));

    }

    @Test
    public void testDifference() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "different");
        map2.put("d", "4");

        Map<String, Pair<String, Nullable<String>>> result = Maps.difference(map1, map2);
        assertEquals(2, result.size());

        Pair<String, Nullable<String>> pairA = result.get("a");
        assertEquals("1", pairA.left());
        assertFalse(pairA.right().isPresent());

        Pair<String, Nullable<String>> pairC = result.get("c");
        assertEquals("3", pairC.left());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        assertNull(result.get("b"));

        Map<String, Pair<String, Nullable<String>>> result2 = Maps.difference(map1, null);
        assertEquals("1", result2.get("a").left());
        assertEquals("3", result2.get("c").left());
    }

    @Test
    public void testDifference_nullMap() {
        Map<String, Pair<Integer, Nullable<Integer>>> result = Maps.difference(null, intMap);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifference_BasicCases() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("c", 4);
        map2.put("d", 5);

        Map<String, Pair<Integer, Nullable<Integer>>> diff = Maps.difference(map1, map2);
        // "a" only in map1
        assertTrue(diff.containsKey("a"));
        assertFalse(diff.get("a").right().isPresent());
        // "c" has different values
        assertTrue(diff.containsKey("c"));
        assertEquals(Integer.valueOf(3), diff.get("c").left());
        assertEquals(Integer.valueOf(4), diff.get("c").right().get());
        // "b" same value - not in diff
        assertFalse(diff.containsKey("b"));
    }

    @Test
    public void testDifference_NullMap() {
        Map<String, Pair<Integer, Nullable<Integer>>> diff = Maps.difference(null, null);
        assertEquals(0, diff.size());
    }

    @Test
    public void testSymmetricDifference() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");

        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        map2.put("c", "different");
        map2.put("d", "4");

        Map<String, Pair<Nullable<String>, Nullable<String>>> result = Maps.symmetricDifference(map1, map2);
        assertEquals(3, result.size());

        Pair<Nullable<String>, Nullable<String>> pairA = result.get("a");
        assertTrue(pairA.left().isPresent());
        assertEquals("1", pairA.left().get());
        assertFalse(pairA.right().isPresent());

        Pair<Nullable<String>, Nullable<String>> pairD = result.get("d");
        assertFalse(pairD.left().isPresent());
        assertTrue(pairD.right().isPresent());
        assertEquals("4", pairD.right().get());

        Pair<Nullable<String>, Nullable<String>> pairC = result.get("c");
        assertTrue(pairC.left().isPresent());
        assertEquals("3", pairC.left().get());
        assertTrue(pairC.right().isPresent());
        assertEquals("different", pairC.right().get());

        assertNull(result.get("b"));
    }

    @Test
    public void testMapOperationsWithIdentityHashMap() {
        IdentityHashMap<String, String> identityMap1 = new IdentityHashMap<>();
        String key1 = new String("key");
        String key2 = new String("key");
        identityMap1.put(key1, "value1");

        IdentityHashMap<String, String> identityMap2 = new IdentityHashMap<>();
        identityMap2.put(key2, "value2");

        Map<String, Pair<Nullable<String>, Nullable<String>>> diff = Maps.symmetricDifference(identityMap1, identityMap2);
        assertTrue(diff instanceof IdentityHashMap);
    }

    @Test
    public void testSymmetricDifference_nullMap() {
        Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(null, intMap);
        assertNotNull(result);
    }

    @Test
    public void testSymmetricDifference_New() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("c", 4);
        map2.put("d", 5);

        Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> symDiff = Maps.symmetricDifference(map1, map2);
        // "a" only in map1
        assertTrue(symDiff.containsKey("a"));
        assertTrue(symDiff.get("a").left().isPresent());
        assertFalse(symDiff.get("a").right().isPresent());
        // "d" only in map2
        assertTrue(symDiff.containsKey("d"));
        assertFalse(symDiff.get("d").left().isPresent());
        assertTrue(symDiff.get("d").right().isPresent());
        // "c" has different values
        assertTrue(symDiff.containsKey("c"));
        // "b" same value - not in symDiff
        assertFalse(symDiff.containsKey("b"));
    }

    // symmetricDifference: map1 non-empty, map2 empty -> all entries have Nullable.empty() right side (L2933)
    @Test
    public void testSymmetricDifference_Map2Empty_AllEntriesInResult() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("x", 10);
        map1.put("y", 20);
        Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(map1, new HashMap<>());
        assertEquals(2, result.size());
        assertTrue(result.containsKey("x"));
        assertTrue(result.get("x").left().isPresent());
        assertFalse(result.get("x").right().isPresent());
    }

    @Test
    public void testContainsEntry_WithMapEntry() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);

        Map.Entry<String, Integer> entry = Maps.newEntry("a", 1);
        assertTrue(Maps.containsEntry(map, entry));

        Map.Entry<String, Integer> wrongEntry = Maps.newEntry("a", 2);
        assertFalse(Maps.containsEntry(map, wrongEntry));
    }

    @Test
    public void testContainsEntry() {
        Map.Entry<String, String> entry = CommonUtil.newEntry("key1", "value1");
        assertTrue(Maps.containsEntry(testMap, entry));

        Map.Entry<String, String> wrongValue = CommonUtil.newEntry("key1", "wrongValue");
        assertFalse(Maps.containsEntry(testMap, wrongValue));

        Map.Entry<String, String> missing = CommonUtil.newEntry("missing", "value");
        assertFalse(Maps.containsEntry(testMap, missing));
    }

    @Test
    public void testContainsKeyValue() {
        assertTrue(Maps.containsEntry(testMap, "key1", "value1"));
        assertFalse(Maps.containsEntry(testMap, "key1", "wrongValue"));
        assertFalse(Maps.containsEntry(testMap, "missing", "value1"));

        testMap.put("nullKey", null);
        assertTrue(Maps.containsEntry(testMap, "nullKey", null));

        assertFalse(Maps.containsEntry(new HashMap<>(), "key", "value"));
        assertFalse(Maps.containsEntry(null, "key", "value"));
    }

    @Test
    public void testContains_entry() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        map = new HashMap<>(map);
        map.put("c", null);
        assertTrue(Maps.containsEntry(map, CommonUtil.newEntry("a", 1)));
        assertFalse(Maps.containsEntry(map, CommonUtil.newEntry("a", 2)));
        assertTrue(Maps.containsEntry(map, CommonUtil.newEntry("c", null)));
        assertFalse(Maps.containsEntry(map, CommonUtil.newEntry("d", null)));
    }

    @Test
    public void testContains_keyValue() {
        Map<String, Integer> map = Map.of("a", 1, "b", 2);
        map = new HashMap<>(map);
        map.put("c", null);
        assertTrue(Maps.containsEntry(map, "a", 1));
        assertFalse(Maps.containsEntry(map, "a", 2));
        assertTrue(Maps.containsEntry(map, "c", null));
        assertFalse(Maps.containsEntry(map, "d", null));
    }

    @Test
    public void testContainsEntry_nullMap() {
        assertFalse(Maps.containsEntry((Map<String, String>) null, CommonUtil.newEntry("a", "b")));
    }

    @Test
    public void testContainsEntry_emptyMap() {
        assertFalse(Maps.containsEntry(new HashMap<>(), CommonUtil.newEntry("a", "b")));
    }

    @Test
    public void testPutIfAbsent() {
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");

        assertNull(Maps.putIfAbsent(map, "new", "newValue"));
        assertEquals("newValue", map.get("new"));

        assertEquals("value", Maps.putIfAbsent(map, "existing", "anotherValue"));
        assertEquals("value", map.get("existing"));

        assertNull(Maps.putIfAbsent(map, "nullKey", (String) null));
        assertTrue(map.containsKey("nullKey"));
        assertNull(map.get("nullKey"));
    }

    @Test
    public void testPutIfAbsentWithSupplier() {
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");

        assertNull(Maps.putIfAbsent(map, "new", Fn.s(() -> "newValue")));
        assertEquals("newValue", map.get("new"));

        final boolean[] supplierCalled = { false };
        assertEquals("value", Maps.putIfAbsent(map, "existing", Fn.s(() -> {
            supplierCalled[0] = true;
            return "anotherValue";
        })));
        assertFalse(supplierCalled[0]);
    }

    @Test
    public void testPutIfAbsent_value() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");

        assertEquals("apple", Maps.putIfAbsent(map, "a", "newApple"));
        assertEquals("apple", map.get("a"));

        assertNull(Maps.putIfAbsent(map, "b", "banana"));
        assertEquals("banana", map.get("b"));

        map.put("c", null);
        assertNull(Maps.putIfAbsent(map, "c", "cherry"));
        assertEquals("cherry", map.get("c"));
    }

    @Test
    public void testPutIfAbsent_supplier() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "apple");
        Supplier<String> supplier = () -> "newVal";

        assertEquals("apple", Maps.putIfAbsent(map, "a", supplier));
        assertEquals("apple", map.get("a"));

        assertNull(Maps.putIfAbsent(map, "b", supplier));
        assertEquals("newVal", map.get("b"));
    }

    @Test
    @DisplayName("putIfAbsent returns existing value when key has non-null mapping")
    public void test_putIfAbsent_existingNonNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        String result = Maps.putIfAbsent(map, "key1", "newValue");

        assertEquals("value1", result, "Should return the existing non-null value");
        assertEquals("value1", map.get("key1"), "Map should not be modified");
    }

    @Test
    @DisplayName("putIfAbsent puts value and returns null when key has null mapping")
    public void test_putIfAbsent_existingNullValue() {
        Map<String, String> map = new HashMap<>();
        map.put("key2", null);

        String result = Maps.putIfAbsent(map, "key2", "value2");

        assertNull(result, "Should return null (the previous null value)");
        assertEquals("value2", map.get("key2"), "Map should now have the new value");
    }

    @Test
    @DisplayName("putIfAbsent puts value and returns null when key is absent")
    public void test_putIfAbsent_absentKey() {
        Map<String, String> map = new HashMap<>();

        String result = Maps.putIfAbsent(map, "key3", "value3");

        assertNull(result, "Should return null (no previous mapping)");
        assertEquals("value3", map.get("key3"), "Map should now have the new value");
    }

    @Test
    public void testPutIfAbsent_supplier_nullMap() {
        Map<String, String> map = new HashMap<>();
        map.put("key", null);
        assertNull(Maps.putIfAbsent(map, "key", Fn.s(() -> "value")));
        assertEquals("value", map.get("key"));
        assertEquals("value", Maps.putIfAbsent(map, "key", Fn.s(() -> "other")));
    }

    @Test
    public void testPutIf_keyFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 2, "c", 3, "aa", 4);
        Predicate<String> keyFilter = k -> k.length() == 1;

        assertTrue(Maps.putAllIf(target, source, keyFilter));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), target);

        assertFalse(Maps.putAllIf(target, Map.of("bb", 5), keyFilter));
    }

    @Test
    public void testPutIf_entryFilter() {
        Map<String, Integer> target = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> source = Map.of("b", 20, "c", 3, "d", 40);
        BiPredicate<String, Integer> entryFilter = (k, v) -> v < 10;

        assertTrue(Maps.putAllIf(target, source, entryFilter));
        assertEquals(Map.of("a", 1, "c", 3), target);

        assertFalse(Maps.putAllIf(target, Map.of("e", 50), entryFilter));
    }

    @Test
    public void testPutIfWithPredicate() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        source.put("c", "3");

        Map<String, String> target = new HashMap<>();

        boolean changed = Maps.putAllIf(target, source, key -> !key.equals("b"));
        assertTrue(changed);
        assertEquals(2, target.size());
        assertEquals("1", target.get("a"));
        assertNull(target.get("b"));
        assertEquals("3", target.get("c"));

        assertFalse(Maps.putAllIf(target, new HashMap<>(), key -> true));
    }

    @Test
    public void testPutIfWithBiPredicate() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        Map<String, Integer> target = new HashMap<>();

        boolean changed = Maps.putAllIf(target, source, (key, value) -> value > 1);
        assertTrue(changed);
        assertEquals(2, target.size());
        assertNull(target.get("a"));
        assertEquals(Integer.valueOf(2), target.get("b"));
        assertEquals(Integer.valueOf(3), target.get("c"));
    }

    @Test
    public void testPutAllIf_keyPredicate_emptySource() {
        Map<String, Integer> target = new HashMap<>();
        assertFalse(Maps.putAllIf(target, new HashMap<>(), k -> true));
    }

    @Test
    public void testPutAllIf_biPredicate_emptySource() {
        Map<String, Integer> target = new HashMap<>();
        assertFalse(Maps.putAllIf(target, new HashMap<>(), (k, v) -> true));
    }

    @Test
    public void testRemove_entry() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        assertTrue(Maps.removeEntry(map, CommonUtil.newEntry("a", 1)));
        assertEquals(1, map.size());
        assertFalse(Maps.removeEntry(map, CommonUtil.newEntry("b", 3)));
    }

    @Test
    public void testRemove_keyValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 2));
        assertTrue(Maps.removeEntry(map, "a", 1));
        assertFalse(Maps.removeEntry(map, "b", 3));
        assertFalse(Maps.removeEntry(map, "c", 2));
    }

    @Test
    public void testRemoveEntry() {
        Map<String, String> map = new HashMap<>(testMap);

        Map.Entry<String, String> entry = CommonUtil.newEntry("key1", "value1");
        assertTrue(Maps.removeEntry(map, entry));
        assertFalse(map.containsKey("key1"));

        Map.Entry<String, String> missing = CommonUtil.newEntry("missing", "value");
        assertFalse(Maps.removeEntry(map, missing));

        Map.Entry<String, String> wrongValue = CommonUtil.newEntry("key2", "wrongValue");
        assertFalse(Maps.removeEntry(map, wrongValue));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveKeyValue() {
        Map<String, String> map = new HashMap<>(testMap);

        assertTrue(Maps.removeEntry(map, "key1", "value1"));
        assertFalse(map.containsKey("key1"));

        assertFalse(Maps.removeEntry(map, "key2", "wrongValue"));
        assertTrue(map.containsKey("key2"));

        assertFalse(Maps.removeEntry(map, "missing", "value"));

        assertFalse(Maps.removeEntry(null, "key", "value"));
        assertFalse(Maps.removeEntry(new HashMap<>(), "key", "value"));
    }

    @Test
    public void testRemoveEntries() {
        Map<String, String> map = new HashMap<>(testMap);
        Map<String, String> entriesToRemove = new HashMap<>();
        entriesToRemove.put("key1", "value1");
        entriesToRemove.put("key2", "wrongValue");
        entriesToRemove.put("key3", "value3");

        assertTrue(Maps.removeEntries(map, entriesToRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveEntries_emptyEntries() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeEntries(map, new HashMap<>()));
    }

    @Test
    public void testRemoveEntries_nullMap() {
        assertFalse(Maps.removeEntries(null, testMap));
    }

    @Test
    public void testRemoveKeys() {
        Map<String, String> map = new HashMap<>(testMap);
        List<String> keysToRemove = Arrays.asList("key1", "key3", "missing");

        assertTrue(Maps.removeKeys(map, keysToRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));

        assertFalse(Maps.removeKeys(map, new ArrayList<>()));
        assertFalse(Maps.removeKeys(new HashMap<>(), keysToRemove));
    }

    @Test
    public void testRemoveKeys_emptyKeys() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeKeys(map, new ArrayList<>()));
    }

    @Test
    public void testRemoveKeys_nullMap() {
        assertFalse(Maps.removeKeys(null, Arrays.asList("key1")));
    }

    @Test
    public void testRemoveIfBiPredicate() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIf(map, (key, value) -> key.equals("key1") || value.equals("value3"));
        assertTrue(removed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testRemoveIf_entry_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIf(map, e -> false));
    }

    @Test
    public void testRemoveIf_biPred_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIf(map, (k, v) -> false));
    }

    @Test
    public void testRemoveIf_EntryPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        boolean changed = Maps.removeIf(map, (Map.Entry<String, Integer> e) -> e.getValue() > 1);
        assertTrue(changed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testRemoveIf_NothingRemoved() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);

        boolean changed = Maps.removeIf(map, (Map.Entry<String, Integer> e) -> e.getValue() > 100);
        assertFalse(changed);
        assertEquals(1, map.size());
    }

    @Test
    public void testRemoveIf() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIf(map, entry -> entry.getValue().endsWith("1"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key1"));

        assertFalse(Maps.removeIf(map, entry -> entry.getKey().equals("missing")));

        assertFalse(Maps.removeIf(new HashMap<>(), entry -> true));
    }

    // removeIf(BiPredicate) with empty map returns false (L3433)
    @Test
    public void testRemoveIf_BiPredicate_EmptyMap_ReturnsFalse() {
        assertFalse(Maps.removeIf(new HashMap<String, Integer>(), (k, v) -> true));
    }

    // removeIfKey with empty map returns false (L3487)
    @Test
    public void testRemoveIfKey_EmptyMap_ReturnsFalse() {
        assertFalse(Maps.removeIfKey(new HashMap<String, Integer>(), k -> true));
    }

    // removeIfValue with empty map returns false (L3542)
    @Test
    public void testRemoveIfValue_EmptyMap_ReturnsFalse() {
        assertFalse(Maps.removeIfValue(new HashMap<String, Integer>(), v -> true));
    }

    @Test
    public void testRemoveIf_entryPredicate() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 20, "c", 3));
        assertTrue(Maps.removeIf(map, entry -> entry.getValue() > 10));
        assertEquals(Map.of("a", 1, "c", 3), map);
        assertFalse(Maps.removeIf(map, entry -> entry.getValue() > 100));
        assertThrows(IllegalArgumentException.class, () -> Maps.removeIf(map, (Predicate<Map.Entry<String, Integer>>) null));
    }

    @Test
    public void testRemoveIf_biPredicate() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1, "b", 20, "c", 3));
        assertTrue(Maps.removeIf(map, (k, v) -> k.equals("b")));
        assertEquals(Map.of("a", 1, "c", 3), map);
        assertThrows(IllegalArgumentException.class, () -> Maps.removeIf(map, (BiPredicate<String, Integer>) null));
    }

    @Test
    public void testConcurrentModification() {
        Map<String, String> map = new HashMap<>(testMap);

        try {
            Maps.removeIf(map, entry -> {
                map.put("newKey", "newValue");
                return true;
            });
            fail("Should throw ConcurrentModificationException");
        } catch (Exception e) {
        }
    }

    @Test
    public void testRemoveIfKey() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIfKey(map, key -> key.startsWith("key1"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key1"));
    }

    @Test
    public void testRemoveIfKey_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIfKey(map, k -> false));
    }

    @Test
    public void testRemoveIfValue() {
        Map<String, String> map = new HashMap<>(testMap);

        boolean removed = Maps.removeIfValue(map, value -> value.contains("2"));
        assertTrue(removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("key2"));
    }

    @Test
    public void testRemoveIfValue_noMatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.removeIfValue(map, v -> false));
    }

    @Test
    public void testReplace_withOldValue() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
        assertTrue(Maps.replace(map, "a", 1, 10));
        assertEquals(10, map.get("a"));
        assertFalse(Maps.replace(map, "a", 1, 20));
        assertFalse(Maps.replace(map, "b", 1, 20));
    }

    @Test
    public void testReplace_withOldValue_mismatch() {
        Map<String, String> map = new HashMap<>(testMap);
        assertFalse(Maps.replace(map, "key1", "wrong", "newVal"));
        assertEquals("value1", map.get("key1"));
    }

    @Test
    public void testReplaceWithOldValue() {
        Map<String, String> map = new HashMap<>(testMap);

        assertTrue(Maps.replace(map, "key1", "value1", "newValue1"));
        assertEquals("newValue1", map.get("key1"));

        assertFalse(Maps.replace(map, "key2", "wrongOldValue", "newValue2"));
        assertEquals("value2", map.get("key2"));

        assertFalse(Maps.replace(map, "missing", "oldValue", "newValue"));

        assertFalse(Maps.replace(new HashMap<>(), "key", "old", "new"));
    }

    @Test
    public void testReplace() {
        Map<String, String> map = new HashMap<>(testMap);

        assertEquals("value1", Maps.replace(map, "key1", "newValue1"));
        assertEquals("newValue1", map.get("key1"));

        assertNull(Maps.replace(map, "missing", "newValue"));
        assertFalse(map.containsKey("missing"));

        map.put("nullKey", null);
        assertNull(Maps.replace(map, "nullKey", "newValue"));
        assertEquals("newValue", map.get("nullKey"));
    }

    @Test
    public void testReplace_newValueOnly() {
        Map<String, Integer> map = new HashMap<>(Map.of("a", 1));
        map.put("b", null);
        assertEquals(Integer.valueOf(1), Maps.replace(map, "a", 10));
        assertEquals(10, map.get("a"));
        assertNull(Maps.replace(map, "b", 20));
        assertEquals(20, map.get("b"));
        assertNull(Maps.replace(map, "c", 30));
        assertNull(map.get("c"));
    }

    @Test
    public void testReplace_keyNotPresent() {
        Map<String, String> map = new HashMap<>(testMap);
        assertNull(Maps.replace(map, "missing", "newVal"));
    }

    // replaceKeys with merger: value being moved is null, destination key exists -> merger called with (existing, null)
    @Test
    public void testReplaceKeys_NullValueMerged_NonNullResult() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", null); // null value
        map.put("b", 10);
        // "a" -> "b" conversion means null value collides with existing key "b"
        Maps.replaceKeys(map, k -> k.equals("a") ? "b" : k, (existing, incoming) -> existing == null ? incoming : existing + (incoming == null ? 0 : incoming));
        assertEquals(Integer.valueOf(10), map.get("b"));
        assertFalse(map.containsKey("a"));
    }

    // replaceKeys with merger: value is null, destination key exists, merger returns null -> removes key
    @Test
    public void testReplaceKeys_NullValueMerged_NullResult_RemovesKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", null); // null value
        map.put("b", 5);
        // merger returns null -> should remove "b"
        Maps.replaceKeys(map, k -> k.equals("a") ? "b" : k, (existing, incoming) -> null);
        assertFalse(map.containsKey("b"));
        assertFalse(map.containsKey("a"));
    }

    // replaceKeys with merger: value is null, destination key doesn't exist -> puts null
    @Test
    public void testReplaceKeys_NullValue_NoCollision_PutsNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("oldKey", null); // null value, no collision
        Maps.replaceKeys(map, k -> "newKey", (existing, incoming) -> existing);
        assertTrue(map.containsKey("newKey"));
        assertNull(map.get("newKey"));
        assertFalse(map.containsKey("oldKey"));
    }

    // replace(Map, key, newValue) with empty map returns null (L3594)
    @Test
    public void testReplace_EmptyMap_ReturnsNull() {
        assertNull(Maps.replace(new HashMap<String, String>(), "key", "newValue"));
    }

    @Test
    public void testReplaceAll() {
        Map<String, String> map = new HashMap<>(testMap);

        Maps.replaceAll(map, (key, value) -> key + "-" + value);
        assertEquals("key1-value1", map.get("key1"));
        assertEquals("key2-value2", map.get("key2"));
        assertEquals("key3-value3", map.get("key3"));

        Map<String, String> emptyMap = new HashMap<>();
        Maps.replaceAll(emptyMap, (k, v) -> "new");
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testReplaceAll_function() {
        Map<String, String> map = new HashMap<>(testMap);
        Maps.replaceAll(map, (k, v) -> v.toUpperCase());
        assertEquals("VALUE1", map.get("key1"));
        assertEquals("VALUE2", map.get("key2"));
    }

    @Test
    public void testFilterBiPredicate() {
        Map<String, String> result = Maps.filter(testMap, (key, value) -> key.equals("key1") || value.equals("value3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
    }

    @Test
    public void testLinkedHashMapPreservation() {
        LinkedHashMap<String, String> linkedMap = new LinkedHashMap<>();
        linkedMap.put("first", "1");
        linkedMap.put("second", "2");
        linkedMap.put("third", "3");

        Map<String, String> filtered = Maps.filter(linkedMap, e -> !e.getKey().equals("second"));

        java.util.Iterator<Map.Entry<String, String>> entries = filtered.entrySet().iterator();
        assertEquals("first", entries.next().getKey());
        assertEquals("third", entries.next().getKey());
    }

    @Test
    public void testFilter() {
        Map<String, String> result = Maps.filter(testMap, entry -> entry.getValue().endsWith("1") || entry.getValue().endsWith("3"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value3", result.get("key3"));
        assertNull(result.get("key2"));

        assertTrue(Maps.filter(null, entry -> true).isEmpty());
    }

    @Test
    public void testFilter_entryPredicate() {
        Map<String, Integer> map = Map.of("a", 1, "b", 20, "c", 3);
        Map<String, Integer> filtered = Maps.filter(map, entry -> entry.getValue() < 10);
        assertEquals(Map.of("a", 1, "c", 3), filtered);
        assertNotSame(map, filtered);
        assertEquals(0, Maps.filter(null, e -> true).size());
    }

    @Test
    public void testFilter_emptyMap() {
        Map<String, String> result = Maps.filter(new HashMap<>(), e -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilter_biPredicate_emptyMap() {
        Map<String, String> result = Maps.filter(new HashMap<>(), (k, v) -> true);
        assertTrue(result.isEmpty());
    }

    // filter(null map, predicate) returns empty HashMap (L3757)
    @Test
    public void testFilter_NullMap_ReturnsEmptyMap() {
        Map<String, Integer> result = Maps.filter((Map<String, Integer>) null, (k, v) -> true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterWithNullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> Maps.filter(testMap, (java.util.function.Predicate<Map.Entry<String, String>>) null));
    }

    @Test
    public void testFilterWithPredicateExceptions() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (Predicate<Map.Entry<String, String>>) null));
    }

    @Test
    public void testFilterByKey() {
        Map<String, String> result = Maps.filterByKey(testMap, key -> key.compareTo("key2") <= 0);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key1"));
        assertTrue(result.containsKey("key2"));
        assertFalse(result.containsKey("key3"));
    }

    @Test
    public void testSortedMapPreservation() {
        TreeMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("c", "3");
        sortedMap.put("a", "1");
        sortedMap.put("b", "2");

        Map<String, String> filtered = Maps.filterByKey(sortedMap, k -> !k.equals("b"));
        assertTrue(filtered instanceof TreeMap);

        java.util.Iterator<String> keys = filtered.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("c", keys.next());
    }

    @Test
    public void testThreadSafetyConsiderations() {

        final Map<String, String> concurrentMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            concurrentMap.put("key" + i, "value" + i);
        }

        Map<String, String> filtered = Maps.filterByKey(concurrentMap, k -> k.contains("5"));
        assertTrue(filtered.size() > 0);
    }

    @Test
    public void testFilterByKey_emptyMap() {
        Map<String, String> result = Maps.filterByKey(new HashMap<>(), k -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFilterByValue() {
        Map<String, String> result = Maps.filterByValue(testMap, value -> value.compareTo("value2") >= 0);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key2"));
        assertTrue(result.containsKey("key3"));
        assertFalse(result.containsKey("key1"));
    }

    @Test
    public void testLargeMapOperations() {
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }

        Map<String, Integer> filtered = Maps.filterByValue(largeMap, v -> v % 2 == 0);
        assertEquals(500, filtered.size());

        Map<Integer, String> inverted = Maps.invert(largeMap);
        assertEquals(1000, inverted.size());
        assertEquals("key500", inverted.get(500));
    }

    @Test
    @Disabled("Performance test - run manually")
    public void testPerformanceWithLargeMaps() {
        Map<String, Integer> veryLargeMap = new HashMap<>();
        for (int i = 0; i < 100000; i++) {
            veryLargeMap.put("key" + i, i);
        }

        long start = System.currentTimeMillis();
        Map<String, Integer> filtered = Maps.filterByValue(veryLargeMap, v -> v % 1000 == 0);
        long duration = System.currentTimeMillis() - start;

        assertEquals(100, filtered.size());
        System.out.println("Filter operation on 100k entries took: " + duration + "ms");

        start = System.currentTimeMillis();
        Map<Integer, String> inverted = Maps.invert(veryLargeMap);
        duration = System.currentTimeMillis() - start;

        assertEquals(100000, inverted.size());
        System.out.println("Invert operation on 100k entries took: " + duration + "ms");
    }

    @Test
    public void testFilterByValue_emptyMap() {
        Map<String, String> result = Maps.filterByValue(new HashMap<>(), v -> true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvertWithMergeFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("uno", 1);
        map.put("two", 2);

        Map<Integer, String> inverted = Maps.invert(map, (v1, v2) -> v1 + "," + v2);
        assertEquals(2, inverted.size());
        assertTrue(inverted.get(1).contains("one"));
        assertTrue(inverted.get(1).contains("uno"));
        assertEquals("two", inverted.get(2));
    }

    @Test
    public void testInvert_withMerge() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);
        BiFunction<String, String, String> merger = (oldKey, newKey) -> oldKey + "," + newKey;
        Map<Integer, String> inverted = Maps.invert(map, merger);

        assertEquals("a,c", inverted.get(1));
        assertEquals("b", inverted.get(2));
    }

    @Test
    public void testInvert_WithMergeOp() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 1);

        Map<Integer, String> inverted = Maps.invert(map, (k1, k2) -> k1 + "," + k2);
        assertEquals(2, inverted.size());
        assertTrue(inverted.containsKey(1));
        assertTrue(inverted.containsKey(2));
        assertEquals("y", inverted.get(2));
    }

    @Test
    public void testInvert() {
        Map<Integer, String> inverted = Maps.invert(intMap);
        assertEquals(3, inverted.size());
        assertEquals("one", inverted.get(1));
        assertEquals("two", inverted.get(2));
        assertEquals("three", inverted.get(3));

        assertTrue(Maps.invert(null).isEmpty());
    }

    @Test
    public void testInvert_emptyMap() {
        Map<String, String> result = Maps.invert(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvert_nullMap() {
        Map<String, String> result = Maps.invert(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInvert_withMerge_nullMap() {
        Map<String, String> result = Maps.invert(null, (a, b) -> a);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatInvert() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("even", Arrays.asList(2, 4, 6));
        map.put("odd", Arrays.asList(1, 3, 5));
        map.put("prime", Arrays.asList(2, 3, 5));

        Map<Integer, List<String>> inverted = Maps.flatInvert(map);
        assertEquals(6, inverted.size());

        assertEquals(Arrays.asList("odd"), inverted.get(1));
        assertTrue(inverted.get(2).contains("even"));
        assertTrue(inverted.get(2).contains("prime"));
        assertEquals(2, inverted.get(2).size());
    }

    @Test
    public void testFlatInvert_New() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("Alice", Arrays.asList(1, 2, 3));
        map.put("Bob", Arrays.asList(2, 4));

        Map<Integer, List<String>> result = Maps.flatInvert(map);
        assertEquals(4, result.size());
        assertEquals(1, result.get(1).size());
        assertEquals("Alice", result.get(1).get(0));
        assertEquals(2, result.get(2).size());
    }

    @Test
    public void testFlatInvert_nullMap() {
        Map<String, List<String>> result = Maps.flatInvert(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatInvert_emptyMap() {
        Map<String, List<String>> result = Maps.flatInvert(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatToMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5, 6));
        map.put("c", Arrays.asList(7, 8));

        List<Map<String, Integer>> result = Maps.flatToMap(map);
        assertEquals(3, result.size());

        Map<String, Integer> first = result.get(0);
        assertEquals(Integer.valueOf(1), first.get("a"));
        assertEquals(Integer.valueOf(4), first.get("b"));
        assertEquals(Integer.valueOf(7), first.get("c"));

        Map<String, Integer> second = result.get(1);
        assertEquals(Integer.valueOf(2), second.get("a"));
        assertEquals(Integer.valueOf(5), second.get("b"));
        assertEquals(Integer.valueOf(8), second.get("c"));

        Map<String, Integer> third = result.get(2);
        assertEquals(Integer.valueOf(3), third.get("a"));
        assertEquals(Integer.valueOf(6), third.get("b"));
        assertFalse(third.containsKey("c"));
    }

    @Test
    public void testFlatToMap_nullMap() {
        List<Map<String, String>> result = Maps.flatToMap(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatToMap_emptyMap() {
        List<Map<String, String>> result = Maps.flatToMap(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatToMap_Basic() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(1, 2, 3));
        map.put("b", Arrays.asList(4, 5));

        List<Map<String, Integer>> result = Maps.flatToMap(map);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).get("a"));
        assertEquals(Integer.valueOf(4), result.get(0).get("b"));
        assertEquals(Integer.valueOf(2), result.get(1).get("a"));
        assertEquals(Integer.valueOf(5), result.get(1).get("b"));
        assertEquals(Integer.valueOf(3), result.get(2).get("a"));
        assertNull(result.get(2).get("b"));
    }

    @Test
    public void testFlatToMap_NullMap() {
        List<Map<String, Integer>> result = Maps.flatToMap(null);
        assertEquals(0, result.size());
    }

    // flatToMap with entry having empty collection - skips that entry (L4043)
    @Test
    public void testFlatToMap_EntryWithEmptyCollection_Skipped() {
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", new ArrayList<>()); // empty collection - should be skipped
        map.put("c", Arrays.asList(3));
        List<Map<String, Integer>> result = Maps.flatToMap(map);
        assertEquals(2, result.size());
        // "b" not in results since it had empty collection
        assertFalse(result.get(0).containsKey("b"));
    }

    @Test
    public void test_flatToMap() {
        Map<String, List<Object>> map = new HashMap<>(
                ImmutableMap.of("a", Lists.newArrayList(1, 2, 3), "b", Lists.newArrayList(4, 5, 6), "c", Lists.newArrayList(7, 8)));

        N.println(map);

        int maxValueSize = Stream.ofValues(map).mapToInt(List::size).max().orElseZero();

        List<Map<String, Object>> list = IntStream.range(0, maxValueSize)
                .mapToObj(it -> Stream.of(map).filter(e -> e.getValue().size() > it).toMap(Entry::getKey, e -> e.getValue().get(it)))
                .toList();

        N.println(list);

        list = Maps.flatToMap(map);
        N.println(list);

        list = Stream.just(map).map(Fn.<String, Object> flatmapValue()).first().orElseThrow();
        N.println(list);
        assertNotNull(list);
    }

    @Test
    public void test_flatten() {
        Map<String, Object> map = CommonUtil.asMap("a", CommonUtil.asMap("b", CommonUtil.asMap("c", CommonUtil.asMap("d", 4), "c2", 3), "b2", 2), "a2", 1);
        N.println(map);

        Map<String, Object> result = Maps.flatten(map);
        N.println(result);

        Map<String, Object> map2 = Maps.unflatten(result);
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void testFlatten_withSupplier() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", CommonUtil.asMap("b", 1));
        LinkedHashMap<String, Object> result = Maps.flatten(map, LinkedHashMap::new);
        assertEquals(1, result.get("a.b"));
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testFlatten() {
        Map<String, Object> map = new HashMap<>();
        map.put("simple", "value");

        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "innerValue");
        map.put("nested", nested);

        Map<String, Object> deepNested = new HashMap<>();
        deepNested.put("deep", "deepValue");
        nested.put("level2", deepNested);

        Map<String, Object> flattened = Maps.flatten(map);
        assertEquals(3, flattened.size());
        assertEquals("value", flattened.get("simple"));
        assertEquals("innerValue", flattened.get("nested.inner"));
        assertEquals("deepValue", flattened.get("nested.level2.deep"));
    }

    @Test
    public void testFlattenWithCustomDelimiter() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "value");
        map.put("outer", nested);

        Map<String, Object> flattened = Maps.flatten(map, "_", HashMap::new);
        assertEquals("value", flattened.get("outer_inner"));
    }

    @Test
    public void testFlattenWithSupplier() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> nested = new HashMap<>();
        nested.put("key", "value");
        map.put("outer", nested);

        LinkedHashMap<String, Object> flattened = Maps.flatten(map, () -> new LinkedHashMap<>());
        assertTrue(flattened instanceof LinkedHashMap);
        assertEquals("value", flattened.get("outer.key"));
    }

    @Test
    public void testFlattenAndUnflatten() {
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        inner.put("c", 3);
        nestedMap.put("a", 1);
        nestedMap.put("b", inner);

        Map<String, Object> flat = Maps.flatten(nestedMap);
        assertEquals(1, flat.get("a"));
        assertEquals(3, flat.get("b.c"));
        assertEquals(2, flat.size());

        Map<String, Object> unflattened = Maps.unflatten(flat);
        assertEquals(1, unflattened.get("a"));
        assertTrue(unflattened.get("b") instanceof Map);
        assertEquals(3, ((Map<?, ?>) unflattened.get("b")).get("c"));

        Map<String, Object> flatCustom = Maps.flatten(nestedMap, "_", HashMap::new);
        assertEquals(3, flatCustom.get("b_c"));
        Map<String, Object> unflattenedCustom = Maps.unflatten(flatCustom, "_", HashMap::new);
        assertEquals(3, ((Map<?, ?>) unflattenedCustom.get("b")).get("c"));
    }

    @Test
    public void testNestedMapConversions() {
        Map<String, Object> deepMap = new HashMap<>();
        Map<String, Object> level1 = new HashMap<>();
        Map<String, Object> level2 = new HashMap<>();
        Map<String, Object> level3 = new HashMap<>();

        level3.put("deep", "value");
        level2.put("level3", level3);
        level1.put("level2", level2);
        deepMap.put("level1", level1);

        Map<String, Object> flattened = Maps.flatten(deepMap);
        assertEquals("value", flattened.get("level1.level2.level3.deep"));

        Map<String, Object> unflattened = Maps.unflatten(flattened);
        assertEquals(deepMap, unflattened);
    }

    @Test
    public void testFlatten_nullMap() {
        Map<String, Object> result = Maps.flatten(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatten_emptyMap() {
        Map<String, Object> result = Maps.flatten(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUnflatten() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("simple", "value");
        flat.put("nested.inner", "innerValue");
        flat.put("nested.level2.deep", "deepValue");

        Map<String, Object> unflattened = Maps.unflatten(flat);
        assertEquals(2, unflattened.size());
        assertEquals("value", unflattened.get("simple"));

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) unflattened.get("nested");
        assertNotNull(nested);
        assertEquals("innerValue", nested.get("inner"));

        @SuppressWarnings("unchecked")
        Map<String, Object> level2 = (Map<String, Object>) nested.get("level2");
        assertNotNull(level2);
        assertEquals("deepValue", level2.get("deep"));
    }

    @Test
    public void testUnflattenWithCustomDelimiter() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("outer_inner", "value");

        Map<String, Object> unflattened = Maps.unflatten(flat, "_", HashMap::new);
        @SuppressWarnings("unchecked")
        Map<String, Object> outer = (Map<String, Object>) unflattened.get("outer");
        assertNotNull(outer);
        assertEquals("value", outer.get("inner"));
    }

    @Test
    public void testUnflattenWithSupplier() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("a.b.c", "value");

        Map<String, Object> unflattened = Maps.unflatten(flat, () -> new LinkedHashMap<>());
        assertTrue(unflattened instanceof LinkedHashMap);

        @SuppressWarnings("unchecked")
        Map<String, Object> a = (Map<String, Object>) unflattened.get("a");
        assertNotNull(a);

        @SuppressWarnings("unchecked")
        Map<String, Object> b = (Map<String, Object>) a.get("b");
        assertNotNull(b);
        assertEquals("value", b.get("c"));
    }

    @Test
    public void testUnflatten_nullMap() {
        Map<String, Object> result = Maps.unflatten(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUnflatten_emptyMap() {
        Map<String, Object> result = Maps.unflatten(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUnflatten_withSupplier() {
        Map<String, Object> flat = new HashMap<>();
        flat.put("a.b", 1);
        LinkedHashMap<String, Object> result = Maps.unflatten(flat, LinkedHashMap::new);
        assertNotNull(result.get("a"));
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testReplaceKeys() {
        Map<String, String> map = new HashMap<>();
        map.put("oldKey1", "value1");
        map.put("oldKey2", "value2");

        Maps.replaceKeys(map, key -> key.replace("old", "new"));

        assertEquals(2, map.size());
        assertEquals("value1", map.get("newKey1"));
        assertEquals("value2", map.get("newKey2"));
        assertFalse(map.containsKey("oldKey1"));
        assertFalse(map.containsKey("oldKey2"));
    }

    @Test
    public void testReplaceKeysWithMerger() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a1", 10);
        map.put("a2", 20);
        map.put("b", 30);

        Maps.replaceKeys(map, key -> key.startsWith("a") ? "a" : key, Integer::sum);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(30), map.get("a"));
        assertEquals(Integer.valueOf(30), map.get("b"));
    }

    @Test
    public void testReplaceKeys_simple() {
        Map<String, Integer> map = new HashMap<>(Map.of("keyOne", 1, "keyTwo", 2));
        Maps.replaceKeys(map, k -> k.replace("key", "k"));
        assertEquals(Map.of("kOne", 1, "kTwo", 2), map);
    }

    @Test
    public void testReplaceKeys_withMerge() {
        Map<String, Integer> map = new HashMap<>();
        map.put("keyA", 1);
        map.put("keyB", 2);
        map.put("oldC", 3);

        Function<String, String> keyConverter = k -> {
            if (k.equals("keyB") || k.equals("oldC"))
                return "newKey";
            return k;
        };
        BiFunction<Integer, Integer, Integer> merger = Integer::sum;

        Maps.replaceKeys(map, keyConverter, merger);

        assertEquals(Map.of("keyA", 1, "newKey", 2 + 3), map);
    }

    @Test
    public void testReplaceKeys_WithMerger() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("aaa", 1);
        map.put("bbb", 2);
        map.put("ccc", 3);

        Maps.replaceKeys(map, key -> key.substring(0, 1), Integer::sum);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testReplaceKeys_emptyMap() {
        Map<String, Integer> map = new HashMap<>();
        Maps.replaceKeys(map, String::toUpperCase);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeys_withMerger_emptyMap() {
        Map<String, Integer> map = new HashMap<>();
        Maps.replaceKeys(map, String::toUpperCase, Integer::sum);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeys_nullMap() {
        Maps.replaceKeys((Map<String, Integer>) null, String::toUpperCase);
        // no exception should be thrown
    }

    @Test
    public void testReplaceKeys_withMerger_nullMap() {
        Maps.replaceKeys((Map<String, Integer>) null, String::toUpperCase, Integer::sum);
        // no exception should be thrown
    }

    @Test
    public void testReplaceKeys_DuplicateConvertedKeyThrowsIllegalStateException() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("left", 1);
        map.put("right", 2);

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> "dup"));
    }

    @Test
    public void test_beanToMap() {

        Account account1 = Beans.newRandom(Account.class);
        Map<String, Object> map1 = Beans.beanToMap(account1);
        N.println(map1);

        Beans.beanToMap(account1, new HashMap<String, Object>());
        Map<String, Object> map2 = Beans.beanToMap(account1, IntFunctions.ofMap());
        N.println(map2);
        assertNotNull(map2);
    }

    @Test
    public void test_MapDifference() {
        Account account1 = Beans.newRandom(Account.class);
        Account account2 = Beans.newRandom(Account.class);
        account2.setFirstName(account1.getFirstName());

        var mapDiff = BeanDifference.of(account1, account2);

        N.println(mapDiff);
        N.println(mapDiff.common());
        N.println(mapDiff.onlyOnLeft());
        N.println(mapDiff.onlyOnRight());
        N.println(mapDiff.differentValues());

        mapDiff = BeanDifference.of(account1, account2, CommonUtil.toList("id", "firstName", "lastName2"));

        N.println(mapDiff);
        N.println(mapDiff.common());
        N.println(mapDiff.onlyOnLeft());
        N.println(mapDiff.onlyOnRight());
        N.println(mapDiff.differentValues());
        assertNotNull(mapDiff);
    }

    @Test
    public void testBean2Map_simple() {
        SimpleBean bean = new SimpleBean(1, "test");
        Map<String, Object> map = Beans.beanToMap(bean);
        assertEquals(Map.of("id", 1, "value", "test"), map);
    }

    @Test
    public void testMap2Bean_simple() {
        Map<String, Object> map = Map.of("id", 10, "value", "hello", "extra", "ignored");
        SimpleBean bean = Beans.mapToBean(map, SimpleBean.class);
        assertEquals(10, bean.id);
        assertEquals("hello", bean.value);

        Map<String, Object> mapNoMatch = Map.of("id", 20, "value", "world", "extraField", "data");
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(mapNoMatch, false, false, SimpleBean.class));

    }

    @Test
    public void testDeepBean2Map() {
        Address address = new Address("NY", "10001");
        Person person = new Person("John", 30, address);
        Map<String, Object> map = Beans.deepBeanToMap(person);

        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertTrue(map.get("address") instanceof Map);
        Map<?, ?> addrMap = (Map<?, ?>) map.get("address");
        assertEquals("NY", addrMap.get("city"));
        assertEquals("10001", addrMap.get("zip"));
    }

    @Test
    public void testBean2FlatMap() {
        Address address = new Address("LA", "90001");
        Person person = new Person("Jane", 25, address);
        Map<String, Object> flatMap = Beans.beanToFlatMap(person);

        assertEquals("Jane", flatMap.get("name"));
        assertEquals(25, flatMap.get("age"));
        assertEquals("LA", flatMap.get("address.city"));
        assertEquals("90001", flatMap.get("address.zip"));
    }

    @Test
    public void testMap2Bean() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Beans.mapToBean(map, TestBean.class);
        assertNotNull(bean);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertTrue(bean.isActive());

        assertNull(Beans.mapToBean((Map) null, TestBean.class));
    }

    @Test
    public void testMap2BeanWithFlags() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", null);
        map.put("unknownProperty", "value");

        TestBean bean1 = Beans.mapToBean(map, true, true, TestBean.class);
        assertEquals("John", bean1.getName());
        assertEquals(0, bean1.getAge());

        try {
            Beans.mapToBean(map, false, false, TestBean.class);
            fail("Should throw exception for unmatched property");
        } catch (Exception e) {
        }
    }

    @Test
    public void testMap2BeanWithSelectProps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Beans.mapToBean(map, Arrays.asList("name", "age"), TestBean.class);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertFalse(bean.isActive());
    }

    @Test
    public void testMap2BeanCollection() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", 30);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Jane");
        map2.put("age", 25);

        List<Map<String, Object>> maps = Arrays.asList(map1, map2);
        List<TestBean> beans = Beans.mapToBean(maps, TestBean.class);

        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(30, beans.get(0).getAge());
        assertEquals("Jane", beans.get(1).getName());
        assertEquals(25, beans.get(1).getAge());

        List<TestBean> beans2 = Beans.mapToBean(maps, CommonUtil.toList("name"), TestBean.class);

        assertEquals(2, beans2.size());
        assertEquals("John", beans2.get(0).getName());
        assertEquals(0, beans2.get(0).getAge());
        assertEquals("Jane", beans2.get(1).getName());
        assertEquals(0, beans2.get(1).getAge());
    }

    @Test
    public void testBean2Map() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Map<String, Object> map = Beans.beanToMap(bean);
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertEquals(true, map.get("active"));
    }

    @Test
    public void testBean2MapWithMapSupplier() {
        TestBean bean = new TestBean();
        bean.setName("John");

        Map<String, Object> map = Beans.beanToMap(bean, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("John", map.get("name"));
    }

    @Test
    public void testBean2MapWithSelectProps() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Map<String, Object> map = Beans.beanToMap(bean, Arrays.asList("name", "age"));
        assertEquals(2, map.size());
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithNamingPolicy() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        Map<String, Object> map = Beans.beanToMap(bean, null, NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofMap());
        assertEquals("John", map.get("NAME"));
        assertEquals(30, map.get("AGE"));
    }

    @Test
    public void testBean2MapIgnoreNull() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setNullableField(null);

        Map<String, Object> map1 = Beans.beanToMap(bean, true);
        assertFalse(map1.containsKey("nullableField"));

        Map<String, Object> map2 = Beans.beanToMap(bean, false);
        assertTrue(map2.containsKey("nullableField"));
        assertNull(map2.get("nullableField"));
    }

    @Test
    public void testBean2MapWithIgnoredProps() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Set<String> ignored = new HashSet<>(Arrays.asList("age", "active"));
        Map<String, Object> map = Beans.beanToMap(bean, false, ignored);

        assertEquals(3, map.size());
        assertEquals("John", map.get("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithComplexTypes() {
        ComplexBean bean = new ComplexBean();
        bean.setDate(new Date());
        bean.setBigDecimal(new BigDecimal("123.456"));
        bean.setStringList(Arrays.asList("a", "b", "c"));
        bean.setIntArray(new int[] { 1, 2, 3 });

        Map<String, Object> map = Beans.beanToMap(bean);
        assertNotNull(map.get("date"));
        assertEquals(bean.getBigDecimal(), map.get("bigDecimal"));
        assertEquals(bean.getStringList(), map.get("stringList"));
        assertArrayEquals(bean.getIntArray(), (int[]) map.get("intArray"));
    }

    @Test
    public void testMap2BeanWithNestedMaps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Parent");

        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("value", "NestedValue");
        map.put("nestedBean", nestedMap);

        TestBean bean = Beans.mapToBean(map, TestBean.class);
        assertEquals("Parent", bean.getName());
        assertNotNull(bean.getNestedBean());
        assertEquals("NestedValue", bean.getNestedBean().getValue());
    }

    @Test
    public void testCircularReferenceHandling() {
        CircularBean bean1 = new CircularBean();
        CircularBean bean2 = new CircularBean();
        bean1.setName("Bean1");
        bean2.setName("Bean2");
        bean1.setReference(bean2);
        bean2.setReference(bean1);

        Map<String, Object> map = Beans.beanToMap(bean1);
        assertEquals("Bean1", map.get("name"));
        assertEquals(bean2, map.get("reference"));

        try {
            Beans.deepBeanToMap(bean1);
        } catch (StackOverflowError e) {
        }
    }

}
