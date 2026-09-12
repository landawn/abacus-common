package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.AbstractTest;

public abstract class MapsTestSupport extends AbstractTest {

    protected Map<String, String> testMap;
    protected Map<String, Integer> intMap;
    protected Map<String, Object> objectMap;
    protected Map<String, Map<String, String>> nestedMap;
    protected Map<String, List<String>> listMap;
    protected Map<String, Set<String>> setMap;

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

    protected static final class SimpleBean {
        protected int id;
        protected String value;

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

    protected static final class Address {
        protected String city;
        protected String zip;

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

    protected static final class Person {
        protected String name;
        protected int age;
        protected Address address;

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

    protected static final class NestedBean {
        protected String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    protected static final class TestBean {
        protected String name;
        protected int age;
        protected boolean active;
        protected String nullableField;
        protected NestedBean nestedBean;

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

    protected static final class ComplexBean {
        protected Date date;
        protected BigDecimal bigDecimal;
        protected List<String> stringList;
        protected int[] intArray;

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

    protected static final class CircularBean {
        protected String name;
        protected CircularBean reference;

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

    protected static final class NonInstantiableHashMap<K, V> extends HashMap<K, V> {
        protected static final long serialVersionUID = 1L;

        protected NonInstantiableHashMap(String ignored) {
        }
    }

    // zip with empty keys/values and mapSupplier returns empty map

    // ---- Additional tests for previously untested methods/overloads ----

    // getAsCharOrDefaultIfAbsent: value is not Character -> converted from its string form

    // getAsByteOrDefaultIfAbsent: value is not Number -> calls Numbers.toByte(N.toString(val))

    // getAsShortOrDefaultIfAbsent: value is not Number -> calls Numbers.toShort

    // getAsLongOrDefaultIfAbsent: value is not Number -> calls Numbers.toLong

    // getAsStringOrDefaultIfAbsent: value is not String -> calls N.stringOf(val)

    // getAs(Map, key, Type) with empty map

    // symmetricDifference: map1 non-empty, map2 empty -> all entries have Nullable.empty() right side (L2933)

    // removeIf(BiPredicate) with empty map returns false (L3433)

    // removeIfKey with empty map returns false (L3487)

    // removeIfValue with empty map returns false (L3542)

    // replaceKeys with merger: value being moved is null, destination key exists -> merger called with (existing, null)

    // replaceKeys with merger: value is null, destination key exists, merger returns null -> removes key

    // replaceKeys with merger: value is null, destination key doesn't exist -> puts null

    // replace(Map, key, newValue) with empty map returns null (L3594)

    // filter(null map, predicate) returns empty HashMap (L3757)

    // flatToMap with entry having empty collection - skips that entry (L4043)

    public static class CaseInsensitiveIntegerMap extends HashMap<String, Integer> {
        @Override
        public Integer put(final String key, final Integer value) {
            return super.put(key.toLowerCase(java.util.Locale.ROOT), value);
        }

        @Override
        public boolean containsKey(final Object key) {
            return super.containsKey(key instanceof String ? ((String) key).toLowerCase(java.util.Locale.ROOT) : key);
        }
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    // ===================== API review 2026-06-15 follow-up =====================

    // 3.2 removeEntry(Map, Map.Entry) tolerates a null entry (matches containsEntry)

    // 3.4 keySet/values/entrySet always return an unmodifiable, live read-through view

    // 3.6 difference/symmetricDifference accept a covariantly-typed second map

    // 3.8 flatten/unflatten accept a size-aware IntFunction map supplier

    // 5.4 filter(map, BiPredicate, IntFunction mapSupplier) — caller controls the result map type

    // 5.1 Maps.merge(Map, K, V, BiFunction) — null-safe static mirror of Map.merge

    // ------------------------------------------------------------------------------------------------
    // getAs(Map, key, Class/Type) and getAsOrDefaultIfAbsent: a null conversion result is "absent".
    // N.convert answers null for "" to every numeric wrapper type; Optional.of(null) used to raise a
    // message-less NPE, and getAsOrDefaultIfAbsent used to let that null escape as its return value.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // getAsChar: a Number is a UTF-16 code unit, not the text of one.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // Empty text is absent for the numeric accessors. Numbers.toXxx(Object) falls back to its
    // defaultValue (zero, as these call it) for empty input, which used to surface "" as a parsed 0
    // while " " threw NumberFormatException.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // getByPath: an index segment must carry a number. Numbers.toInt("") answers 0 (a missing-input
    // fallback, not a parse), which silently turned "a[]" into "a[0]".
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // The shared path resolver now reports "not found" with a protected sentinel rather than with the
    // caller's own default value, so a default that the map genuinely contains is no longer ambiguous.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // symmetricDifference must take its key semantics from the map that supplies the keys.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // The mutating methods reject a null map with IllegalArgumentException, matching merge(...) and
    // the rest of this class's argument validation. The no-op mutators still tolerate one.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // Argument-validation messages
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // Signature widening
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // Result-map construction: a template class that cannot be instantiated reflectively falls back to
    // a correctly-sized HashMap / LinkedHashMap.
    // ------------------------------------------------------------------------------------------------

    //
    // ============================ review fixes 2026-09-06 ============================
    //

    /** Accepts clear() but rejects putAll() - the shape the two replaceKeys javadocs disagreed about. */
    protected static final class NoPutAllLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        protected static final long serialVersionUID = 1L;

        @Override
        public void putAll(final Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException("putAll is not supported");
        }
    }
}
