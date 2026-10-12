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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Behaviour-pinning and memory-retention tests written by the line-by-line performance and memory review of
 * {@code com.landawn.abacus.util} on 2026-10-08. Each review group appends one {@code @Nested} class named after its group id.
 * <p>
 * Extends {@code TestBase} and is tagged {@code slow-test}: performance-review tests stay out of the
 * regular unit suite ({@code AbacusCoreTestSuite} excludes {@code slow-test}); run this class by name.
 */
@Tag("slow-test")
class UtilPerformanceMemoryReview20261008Test extends TestBase {

    // ---- perf review 2026-10-08 G003 begin ----
    @Nested
    class G003 {
        /**
         * Builds a LinkedHashMap whose insertion order is neither sorted nor hash order, so an order mismatch shows.
         */
        private LinkedHashMap<String, Integer> scrambledLinkedHashMap(final int size, final long seed) {
            final List<Integer> ids = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                ids.add(i);
            }

            Collections.shuffle(ids, new Random(seed));

            final LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

            for (final Integer id : ids) {
                result.put("k" + id, id * 3 + 1);
            }

            return result;
        }

        /**
         * Asserts that {@code copy} is an independent BiMap with the same content and the same forward order as
         * {@code source}, and also the same reverse-map order when {@code checkInverseOrder} is set (a copy fills its
         * reverse map in forward order, so that holds only when the source's two maps list the pairs alike).
         */
        private <K, V> void assertFaithfulCopy(final BiMap<K, V> source, final BiMap<K, V> copy, final boolean checkInverseOrder) {
            assertNotSame(source, copy);
            assertNotSame(source.keyMap, copy.keyMap);
            assertNotSame(source.keyMap, copy.valueMap);
            assertNotSame(source.valueMap, copy.keyMap);
            assertNotSame(source.valueMap, copy.valueMap);
            assertSame(source.keyMapSupplier, copy.keyMapSupplier);
            assertSame(source.valueMapSupplier, copy.valueMapSupplier);
            assertEquals(source, copy);
            assertEquals(source.inverse(), copy.inverse());
            assertEquals(source.size(), copy.inverse().size());
            assertEquals(new ArrayList<>(source.keySet()), new ArrayList<>(copy.keySet()));
            assertEquals(new ArrayList<>(source.values()), new ArrayList<>(copy.values()));
            assertEquals(new ArrayList<>(source.entrySet()), new ArrayList<>(copy.entrySet()));

            if (checkInverseOrder) {
                assertEquals(new ArrayList<>(source.inverse().keySet()), new ArrayList<>(copy.inverse().keySet()));
                assertEquals(new ArrayList<>(source.inverse().values()), new ArrayList<>(copy.inverse().values()));
            }
        }

        // G003-01: copy() of a BiMap backed by the library's LinkedHashMap suppliers, on both sides of the 12-entry presizing threshold
        @Test
        void testCopy_linkedHashMapBackedSizesAroundPresizeThreshold() {
            for (final int size : new int[] { 0, 1, 2, 11, 12, 13, 23, 24, 25, 47, 48, 49, 96, 97, 1000, 3000 }) {
                final LinkedHashMap<String, Integer> template = scrambledLinkedHashMap(size, size);
                final BiMap<String, Integer> source = BiMap.copyOf(template);
                assertSame(Suppliers.ofLinkedHashMap(), source.keyMapSupplier);
                assertSame(Suppliers.ofLinkedHashMap(), source.valueMapSupplier);

                final BiMap<String, Integer> copy = source.copy();

                assertFaithfulCopy(source, copy, true);
                assertEquals(LinkedHashMap.class, copy.keyMap.getClass());
                assertEquals(LinkedHashMap.class, copy.valueMap.getClass());
                assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(copy.keySet()));
                assertEquals(new ArrayList<>(template.values()), new ArrayList<>(copy.values()));
                assertEquals(new ArrayList<>(template.values()), new ArrayList<>(copy.inverse().keySet()));

                // A copy of the copy, and a copy of the inverse view, keep the same orders.
                assertFaithfulCopy(copy, copy.copy(), true);
                final BiMap<Integer, String> inverseCopy = source.inverse().copy();
                assertEquals(new ArrayList<>(template.values()), new ArrayList<>(inverseCopy.keySet()));
                assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(inverseCopy.values()));
                assertEquals(LinkedHashMap.class, inverseCopy.keyMap.getClass());
                assertEquals(LinkedHashMap.class, inverseCopy.valueMap.getClass());

                // The copy is independent and keeps behaving like an insertion-ordered, bijective BiMap.
                copy.put("new", -1);
                assertFalse(source.containsKey("new"));
                assertEquals("new", new ArrayList<>(copy.keySet()).get(copy.size() - 1));
                assertThrows(IllegalArgumentException.class, () -> copy.put("other", -1));

                if (size > 0) {
                    final String firstKey = template.keySet().iterator().next();
                    copy.put(firstKey, -2);
                    assertEquals(firstKey, copy.keySet().iterator().next());
                    assertEquals(firstKey, copy.getByValue(-2));
                    assertEquals(template.get(firstKey), source.get(firstKey));

                    source.remove(firstKey);
                    assertTrue(copy.containsKey(firstKey));
                    assertEquals(size + 1, copy.size());
                    assertEquals(size - 1, source.size());
                }
            }
        }

        // G003-01: class-based and explicitly passed library LinkedHashMap suppliers, after removals, re-insertions and forcePut
        @Test
        void testCopy_linkedHashMapSuppliersAfterRemovalsAndForcePut() {
            final List<BiMap<String, Integer>> sources = new ArrayList<>();
            sources.add(new BiMap<>(LinkedHashMap.class, LinkedHashMap.class));
            sources.add(new BiMap<>(Suppliers.ofLinkedHashMap(), Suppliers.ofLinkedHashMap()));

            for (final BiMap<String, Integer> source : sources) {
                for (int i = 0; i < 60; i++) {
                    source.put("k" + (i * 37 % 61), i);
                }

                for (int i = 0; i < 60; i += 4) {
                    source.remove("k" + (i * 37 % 61));
                }

                source.put("k" + (8 * 37 % 61), 1000);
                source.forcePut("displacer", 5);
                source.putIfAbsent("late", 2000);
                source.forcePut("k" + (5 * 37 % 61), 3000);

                final BiMap<String, Integer> copy = source.copy();
                assertFaithfulCopy(source, copy, true);
                assertFaithfulCopy(source.inverse(), source.inverse().copy(), true);
                assertFaithfulCopy(copy, copy.copy(), true);
            }
        }

        // G003-01: maps that are not the library's LinkedHashMap supplier pair still come from their suppliers, once per side
        @Test
        void testCopy_otherSuppliersStillUsed() {
            final AtomicInteger keyMapCalls = new AtomicInteger();
            final AtomicInteger valueMapCalls = new AtomicInteger();
            final BiMap<String, Integer> counted = new BiMap<>(() -> {
                keyMapCalls.incrementAndGet();
                return new LinkedHashMap<>();
            }, () -> {
                valueMapCalls.incrementAndGet();
                return new LinkedHashMap<>();
            });
            counted.putAll(scrambledLinkedHashMap(40, 7));
            keyMapCalls.set(0);
            valueMapCalls.set(0);
            final BiMap<String, Integer> countedCopy = counted.copy();
            assertEquals(1, keyMapCalls.get());
            assertEquals(1, valueMapCalls.get());
            assertFaithfulCopy(counted, countedCopy, true);

            // HashMap-backed (default): copied through its suppliers, same content and order.
            final BiMap<String, Integer> hashed = new BiMap<>();
            hashed.putAll(scrambledLinkedHashMap(40, 8));
            final BiMap<String, Integer> hashedCopy = hashed.copy();
            assertFaithfulCopy(hashed, hashedCopy, false);
            assertEquals(HashMap.class, hashedCopy.keyMap.getClass());
            assertEquals(HashMap.class, hashedCopy.valueMap.getClass());

            // Comparator-keyed forward map with a LinkedHashMap reverse map: mixed suppliers.
            final TreeMap<String, Integer> sorted = new TreeMap<>(Collections.reverseOrder());
            sorted.putAll(scrambledLinkedHashMap(40, 9));
            final BiMap<String, Integer> sortedBiMap = BiMap.copyOf(sorted);
            final BiMap<String, Integer> sortedCopy = sortedBiMap.copy();
            assertFaithfulCopy(sortedBiMap, sortedCopy, true);
            assertEquals(new ArrayList<>(sorted.keySet()), new ArrayList<>(sortedCopy.keySet()));
            assertEquals(LinkedHashMap.class, sortedCopy.valueMap.getClass());

            // A supplier that hands out one shared instance is still rejected.
            final Map<String, Integer> shared = new LinkedHashMap<>();
            final BiMap<String, Integer> aliasing = new BiMap<>(() -> shared, LinkedHashMap::new);
            assertThrows(IllegalArgumentException.class, aliasing::copy);
            aliasing.put("a", 1);
            assertThrows(IllegalArgumentException.class, aliasing::copy);
        }

        /**
         * A value whose hashCode can be made to fail after it has been stored.
         */
        private final class FlakyValue {
            private final int id;
            private boolean armed;

            FlakyValue(final int id) {
                this.id = id;
            }

            @Override
            public int hashCode() {
                if (armed) {
                    throw new IllegalStateException("hashCode failed for " + id);
                }

                return id;
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof FlakyValue && ((FlakyValue) obj).id == id;
            }
        }

        // G003-01: a failing element callback during a presized copy propagates unchanged and leaves the source intact
        @Test
        void testCopy_linkedHashMapBackedFailingValueHashCode() {
            final LinkedHashMap<String, FlakyValue> template = new LinkedHashMap<>();
            final List<FlakyValue> values = new ArrayList<>();

            for (int i = 0; i < 30; i++) {
                final FlakyValue value = new FlakyValue(i);
                values.add(value);
                template.put("k" + i, value);
            }

            final BiMap<String, FlakyValue> source = BiMap.copyOf(template);
            values.get(0).armed = true;

            final IllegalStateException e = assertThrows(IllegalStateException.class, source::copy);
            assertEquals("hashCode failed for 0", e.getMessage());

            values.get(0).armed = false;
            values.get(17).armed = true;
            final IllegalStateException e17 = assertThrows(IllegalStateException.class, source::copy);
            assertEquals("hashCode failed for 17", e17.getMessage());

            values.get(17).armed = false;
            assertEquals(30, source.size());
            assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(source.keySet()));
            final BiMap<String, FlakyValue> copy = source.copy();
            assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(copy.keySet()));
            assertEquals(new ArrayList<>(source.inverse().keySet()), new ArrayList<>(copy.inverse().keySet()));
        }
    }

    // ---- perf review 2026-10-08 G003 end ----
    // ---- perf review 2026-10-08 G004 begin ----
    @Nested
    class G004 {
        /**
         * An entity, so the computed {@code getFullName()} is a read-only property.
         */
        @com.landawn.abacus.annotation.Entity
        public static class G004Person {
            private String firstName;
            private String lastName = "initial";
            private int age;
            private G004Address address;

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(final String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(final String lastName) {
                this.lastName = lastName;
            }

            public int getAge() {
                return age;
            }

            public void setAge(final int age) {
                this.age = age;
            }

            public G004Address getAddress() {
                return address;
            }

            public void setAddress(final G004Address address) {
                this.address = address;
            }

            public String getFullName() {
                return firstName + " " + lastName;
            }
        }

        /**
         * An entity with a computed, read-only {@code label}.
         */
        @com.landawn.abacus.annotation.Entity
        public static class G004Address {
            private String city;
            private String zip;

            public String getCity() {
                return city;
            }

            public void setCity(final String city) {
                this.city = city;
            }

            public String getZip() {
                return zip;
            }

            public void setZip(final String zip) {
                this.zip = zip;
            }

            public String getLabel() {
                return city + "-" + zip;
            }
        }

        /**
         * Records its construction and every setter call, so the tests can see where validation interleaves with writes.
         */
        // Accessor-based on purpose: this fixture observes getter/setter calls, and properties are read and written
        // through their fields unless @AccessFieldByMethod says otherwise (in both the reflective and the ASM mode).
        @com.landawn.abacus.annotation.AccessFieldByMethod
        public static class G004Recorder {
            static final List<String> LOG = new ArrayList<>();

            private String a;
            private String b;

            public G004Recorder() {
                LOG.add("new");
            }

            public String getA() {
                return a;
            }

            public void setA(final String a) {
                LOG.add("a=" + a);
                this.a = a;
            }

            public String getB() {
                return b;
            }

            public void setB(final String b) {
                LOG.add("b=" + b);
                this.b = b;
            }
        }

        private String notABeanMessage(final Class<?> cls) {
            return assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(cls)).getMessage();
        }

        private void assertNotABean(final Class<?> cls, final org.junit.jupiter.api.function.Executable executable) {
            assertEquals(notABeanMessage(cls), assertThrows(IllegalArgumentException.class, executable).getMessage(), cls.getName());
        }

        private String readOnlyMessage(final String propName, final Class<?> cls) {
            return "No setter method found with property name: " + propName + " in class: " + ClassUtil.getCanonicalClassName(cls)
                    + " (the property is read-only: no permitted field write, setter, or supported getter-based write)";
        }

        private String noSetterMessage(final String propName, final Class<?> cls) {
            return "No setter method found with property name: " + propName + " in class: " + cls.getCanonicalName();
        }

        private G004Person person() {
            final G004Person p = new G004Person();
            p.setFirstName("Ann");
            p.setAge(7);
            final G004Address a = new G004Address();
            a.setCity("NYC");
            p.setAddress(a);
            return p;
        }

        // G004-01: getPropValue/getPropValueIfPresent/getPropNames(bean, ..) validate the class once (inside ParserUtil.getBeanInfo)
        // and still throw the reason-bearing exception for every kind of non-bean, before any callback runs.
        @Test
        public void testGetPropValue_nonBeanClassesKeepTheReasonBearingException() {
            assertEquals("Not a bean class: java.lang.String - CharSequence implementations are never treated as beans", notABeanMessage(String.class));
            assertEquals("Not a bean class: java.lang.IllegalStateException - Throwable subclasses are never treated as beans",
                    notABeanMessage(IllegalStateException.class));

            final AtomicInteger callbacks = new AtomicInteger();

            for (final Object nonBean : Arrays.asList("abc", 5, new ArrayList<>(Arrays.asList(1)), new HashMap<>(), new Object(), new java.util.Date(0),
                    new IllegalStateException("x"))) {
                final Class<?> cls = nonBean.getClass();

                assertFalse(Beans.isBeanClass(cls), cls.getName());
                assertNotABean(cls, () -> Beans.getPropValue(nonBean, "length"));
                assertNotABean(cls, () -> Beans.getPropValue(nonBean, "length", true));
                assertNotABean(cls, () -> Beans.getPropValue(nonBean, "message", false));
                assertNotABean(cls, () -> Beans.getPropValueIfPresent(nonBean, "x"));
                assertNotABean(cls, () -> Beans.getPropNames(nonBean, true));
                assertNotABean(cls, () -> Beans.getPropNames(nonBean, false));
                assertNotABean(cls, () -> Beans.getPropNames(nonBean, (java.util.function.Predicate<String>) n -> callbacks.incrementAndGet() > 0));
                assertNotABean(cls,
                        () -> Beans.getPropNames(nonBean, (java.util.function.BiPredicate<String, Object>) (n, v) -> callbacks.incrementAndGet() > 0));
            }

            assertEquals(0, callbacks.get());

            // argument validation still comes first, with the same messages as for a bean
            final G004Person bean = person();
            final String nullFilter = assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames(bean, (java.util.function.Predicate<String>) null))
                    .getMessage();
            assertEquals(nullFilter,
                    assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames("abc", (java.util.function.Predicate<String>) null)).getMessage());
            final String nullBiFilter = assertThrows(IllegalArgumentException.class,
                    () -> Beans.getPropNames(bean, (java.util.function.BiPredicate<String, Object>) null)).getMessage();
            assertEquals(nullBiFilter,
                    assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames("abc", (java.util.function.BiPredicate<String, Object>) null))
                            .getMessage());
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(null, "firstName"));
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValueIfPresent(null, "firstName"));
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames((Object) null, true));
        }

        // G004-01: the bean paths of the same methods are unchanged (values, aliases, nested paths, misses, callback order).
        @Test
        public void testGetPropValue_beanPathsAndCallbacksUnchanged() {
            final G004Person p = person();

            assertEquals("Ann", Beans.<String> getPropValue(p, "firstName"));
            assertEquals("Ann", Beans.<String> getPropValue(p, "FIRST_NAME"));
            assertEquals("Ann", Beans.<String> getPropValue(p, "first_name", false));
            assertEquals(Integer.valueOf(7), Beans.<Integer> getPropValue(p, "age"));
            assertEquals("Ann initial", Beans.<String> getPropValue(p, "fullName"));
            assertEquals("NYC", Beans.<String> getPropValue(p, "address.city"));
            assertEquals("NYC-null", Beans.<String> getPropValue(p, "address.label"));
            assertNull(Beans.getPropValue(p, "address.zip"));
            assertNull(Beans.getPropValue(p, "nope", true));
            assertEquals("No property method found with property name: nope in class " + ClassUtil.getCanonicalClassName(G004Person.class),
                    assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(p, "nope")).getMessage());
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(p, "address.nope", false));
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(p, (String) null));

            assertEquals(u.Nullable.of("Ann"), Beans.getPropValueIfPresent(p, "firstName"));
            assertEquals(u.Nullable.of("NYC"), Beans.getPropValueIfPresent(p, "address.city"));
            assertEquals(u.Nullable.of((Object) null), Beans.getPropValueIfPresent(p, "address.zip"));
            assertFalse(Beans.getPropValueIfPresent(p, "nope").isPresent());

            final G004Person noAddress = new G004Person();
            assertNull(Beans.getPropValue(noAddress, "address.city"));
            assertFalse(Beans.getPropValueIfPresent(noAddress, "address.city").isPresent());

            assertEquals(Arrays.asList("firstName", "lastName", "age", "address", "fullName"), Beans.getPropNames(p, false));
            assertEquals(Arrays.asList("lastName", "age", "fullName"), Beans.getPropNames(noAddress, true));

            final List<String> seen = new ArrayList<>();
            assertEquals(Arrays.asList("firstName", "lastName", "fullName"), Beans.getPropNames(p, (java.util.function.Predicate<String>) n -> {
                seen.add(n);
                return n.endsWith("Name");
            }));
            assertEquals(Arrays.asList("firstName", "lastName", "age", "address", "fullName"), seen);

            seen.clear();
            assertEquals(Arrays.asList("age"), Beans.getPropNames(p, (java.util.function.BiPredicate<String, Object>) (n, v) -> {
                seen.add(n + "=" + (v instanceof G004Address ? "address" : v));
                return v instanceof Integer;
            }));
            assertEquals(Arrays.asList("firstName=Ann", "lastName=initial", "age=7", "address=address", "fullName=Ann initial"), seen);
        }

        // G004-02: mapToBean/mapsToBeans check the bean class once per call (ParserUtil.getBeanInfo does it for a non-null map);
        // a non-bean class is still rejected with the same exception for null/non-null maps and null/empty/bogus selections.
        @Test
        public void testMapToBean_beanClassCheckedOnceWithSameException() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("firstName", "Bob");

            for (final Class<?> cls : Arrays.asList(String.class, Integer.class, ArrayList.class, HashMap.class, Object.class, java.util.Date.class,
                    IllegalStateException.class)) {
                @SuppressWarnings("unchecked")
                final Class<Object> target = (Class<Object>) cls;

                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, target));
                assertNotABean(cls, () -> Beans.mapToBean(map, target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, false, target));
                assertNotABean(cls, () -> Beans.mapToBean(map, false, target));
                assertNotABean(cls, () -> Beans.mapToBean(new HashMap<>(), true, target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, (Collection<String>) null, target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, Collections.<String> emptyList(), target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList("bogus"), target));
                assertNotABean(cls, () -> Beans.mapToBean(map, (Collection<String>) null, target));
                assertNotABean(cls, () -> Beans.mapToBean(map, Collections.<String> emptyList(), target));
                assertNotABean(cls, () -> Beans.mapToBean(map, Arrays.asList("bogus"), target));
                assertNotABean(cls, () -> Beans.mapsToBeans(new ArrayList<>(), target));
                assertNotABean(cls, () -> Beans.mapsToBeans(Arrays.asList(map, null), true, target));
                assertNotABean(cls, () -> Beans.mapsToBeans(Arrays.asList(null, map), Arrays.asList("bogus"), target));
            }

            final String nullClass = assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean((Map<String, Object>) null, (Class<Object>) null))
                    .getMessage();
            assertEquals(nullClass, assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, (Class<Object>) null)).getMessage());
            assertEquals(nullClass, assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, false, (Class<Object>) null)).getMessage());
            assertEquals(nullClass,
                    assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, (Collection<String>) null, (Class<Object>) null)).getMessage());
            assertEquals(nullClass,
                    assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("bogus"), (Class<Object>) null)).getMessage());

            // the bean class itself: null map -> null, otherwise a populated new bean
            assertNull(Beans.mapToBean((Map<String, Object>) null, G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, false, G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, (Collection<String>) null, G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, Collections.<String> emptyList(), G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, Arrays.asList("firstName", "address.city"), G004Person.class));
            assertEquals(noSetterMessage("bogus", G004Person.class), assertThrows(IllegalArgumentException.class,
                    () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList("firstName", "bogus"), G004Person.class)).getMessage());

            final Map<String, Object> full = new LinkedHashMap<>();
            full.put("FIRST_NAME", "Bob");
            full.put("age", "41");
            full.put("address", CommonUtil.<String, Object> asMap("city", "LA", "bogus", 1));
            full.put("unknown", 1);
            full.put("fullName", "ignored");

            for (final G004Person p : Arrays.asList(Beans.mapToBean(full, G004Person.class), Beans.mapToBean(full, true, G004Person.class),
                    Beans.mapToBean(full, (Collection<String>) null, G004Person.class), Beans.mapsToBeans(Arrays.asList(full), G004Person.class).get(0))) {
                assertEquals("Bob", p.getFirstName());
                assertEquals("initial", p.getLastName());
                assertEquals(41, p.getAge());
                assertEquals("LA", p.getAddress().getCity());
            }

            assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(full, false, G004Person.class));
            assertEquals(Arrays.asList(null, null), Beans.mapsToBeans(Arrays.asList(null, null), G004Person.class));
            assertEquals(0, Beans.mapsToBeans(null, G004Person.class).size());
        }

        // G004-03: mapToBean(Map, Collection, Class) resolves each selected name once; the checks, their messages and their
        // interleaving with the constructor and setter calls are unchanged.
        @Test
        public void testMapToBeanSelected_validationAndWritesInterleaveAsBefore() {
            Beans.mapToBean(CommonUtil.<String, Object> asMap("a", "warm"), G004Recorder.class);

            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("a", "1");
            map.put("b", "2");

            G004Recorder.LOG.clear();
            G004Recorder r = Beans.mapToBean(map, Arrays.asList("b", "A", "a", new String("b")), G004Recorder.class);
            assertEquals("1", r.getA());
            assertEquals("2", r.getB());
            // "A" resolves to property a, but the map has no "A" key, so it is skipped; the second "b" is written again
            assertEquals(Arrays.asList("new", "b=2", "a=1", "b=2"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            assertEquals(noSetterMessage("bogus", G004Recorder.class),
                    assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("a", "bogus", "b"), G004Recorder.class))
                            .getMessage());
            assertEquals(Arrays.asList("new", "a=1"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("b", null, "a"), G004Recorder.class));
            assertEquals(Arrays.asList("new", "b=2"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            r = Beans.mapToBean(map, Collections.<String> emptyList(), G004Recorder.class);
            assertNull(r.getA());
            assertEquals(Arrays.asList("new"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            final List<G004Recorder> list = Beans.mapsToBeans(Arrays.asList(map, null, CommonUtil.<String, Object> asMap("a", "x")), Arrays.asList("a"),
                    G004Recorder.class);
            assertEquals(3, list.size());
            assertEquals("1", list.get(0).getA());
            assertNull(list.get(1));
            assertEquals("x", list.get(2).getA());
            assertEquals(Arrays.asList("new", "a=1", "new", "a=x"), G004Recorder.LOG);
        }

        // G004-03: read-only, unknown, aliased and dot-separated selected names keep their outcomes and messages.
        @Test
        public void testMapToBeanSelected_readOnlyUnknownAndDottedNames() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("firstName", "Cy");
            map.put("FIRST_NAME", "Dee");
            map.put("lastName", null);
            map.put("age", 3);
            map.put("address.city", "SF");
            map.put("fullName", "x");

            G004Person p = Beans.mapToBean(map, Arrays.asList("firstName", "age"), G004Person.class);
            assertEquals("Cy", p.getFirstName());
            assertEquals("initial", p.getLastName());
            assertEquals(3, p.getAge());
            assertNull(p.getAddress());

            p = Beans.mapToBean(map, Arrays.asList("FIRST_NAME", "lastName", "address.city"), G004Person.class);
            assertEquals("Dee", p.getFirstName());
            assertNull(p.getLastName());
            assertEquals("SF", p.getAddress().getCity());

            p = Beans.mapToBean(map, Arrays.asList("address.zip", "first_name"), G004Person.class);
            assertNull(p.getFirstName());
            assertNull(p.getAddress());

            for (final String readOnly : Arrays.asList("fullName", "FULL_NAME", "full_name", "address.label")) {
                assertEquals(readOnlyMessage(readOnly, G004Person.class),
                        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("firstName", readOnly), G004Person.class))
                                .getMessage(),
                        readOnly);
                assertEquals(readOnlyMessage(readOnly, G004Person.class),
                        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(Arrays.asList(map), Arrays.asList(readOnly), G004Person.class))
                                .getMessage(),
                        readOnly);
                assertEquals(readOnlyMessage(readOnly, G004Person.class), assertThrows(IllegalArgumentException.class,
                        () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList(readOnly), G004Person.class)).getMessage(), readOnly);
            }

            for (final String unknown : Arrays.asList("nope", "address.nope", "nope.city")) {
                assertEquals(noSetterMessage(unknown, G004Person.class),
                        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList(unknown), G004Person.class)).getMessage(),
                        unknown);
                assertEquals(noSetterMessage(unknown, G004Person.class), assertThrows(IllegalArgumentException.class,
                        () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList(unknown), G004Person.class)).getMessage(), unknown);
            }
        }
    }

    // ---- perf review 2026-10-08 G004 end ----
    // ---- perf review 2026-10-08 G005 begin ----
    @Nested
    class G005 {
        private static final java.util.regex.Pattern UUID_FRAGMENT = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]");
        private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{3}@email\\.com");
        private static final String NOT_A_BEAN_STRING = "Not a bean class: java.lang.String - CharSequence implementations are never treated as beans";
        private static final String NOT_A_BEAN_INTEGER = "Not a bean class: java.lang.Integer - Number implementations are never treated as beans";
        private static final String LONG_PROP = "longPropertyName" + "Abcdefghij".repeat(12); // 136 characters: past the 128-character cache key limit

        public static class G005Address {
            private String city;
            private String zip;
            private int number;

            public String getCity() {
                return city;
            }

            public void setCity(final String city) {
                this.city = city;
            }

            public String getZip() {
                return zip;
            }

            public void setZip(final String zip) {
                this.zip = zip;
            }

            public int getNumber() {
                return number;
            }

            public void setNumber(final int number) {
                this.number = number;
            }
        }

        public static class G005Person {
            private String name;
            private String email;
            private String mail;
            private String workEMail;
            private int age;
            private G005Address address;

            public String getName() {
                return name;
            }

            public void setName(final String name) {
                this.name = name;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(final String email) {
                this.email = email;
            }

            public String getMail() {
                return mail;
            }

            public void setMail(final String mail) {
                this.mail = mail;
            }

            public String getWorkEMail() {
                return workEMail;
            }

            public void setWorkEMail(final String workEMail) {
                this.workEMail = workEMail;
            }

            public int getAge() {
                return age;
            }

            public void setAge(final int age) {
                this.age = age;
            }

            public G005Address getAddress() {
                return address;
            }

            public void setAddress(final G005Address address) {
                this.address = address;
            }
        }

        public static class G005Node {
            private String label;
            private G005Node parent;

            public String getLabel() {
                return label;
            }

            public void setLabel(final String label) {
                this.label = label;
            }

            public G005Node getParent() {
                return parent;
            }

            public void setParent(final G005Node parent) {
                this.parent = parent;
            }
        }

        public static class G005Names {
            private String userID;
            private String htmlURLValue;
            private int value2Go;
            private String x;
            private G005Address homeAddress;
            private String longPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij;

            public String getUserID() {
                return userID;
            }

            public void setUserID(final String userID) {
                this.userID = userID;
            }

            public String getHtmlURLValue() {
                return htmlURLValue;
            }

            public void setHtmlURLValue(final String htmlURLValue) {
                this.htmlURLValue = htmlURLValue;
            }

            public int getValue2Go() {
                return value2Go;
            }

            public void setValue2Go(final int value2Go) {
                this.value2Go = value2Go;
            }

            public String getX() {
                return x;
            }

            public void setX(final String x) {
                this.x = x;
            }

            public G005Address getHomeAddress() {
                return homeAddress;
            }

            public void setHomeAddress(final G005Address homeAddress) {
                this.homeAddress = homeAddress;
            }

            public String getLongPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij() {
                return longPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij;
            }

            public void setLongPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij(
                    final String value) {
                this.longPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij = value;
            }
        }

        private G005Person person(final String name, final String email, final String mail, final int age, final G005Address address) {
            final G005Person person = new G005Person();
            person.setName(name);
            person.setEmail(email);
            person.setMail(mail);
            person.setAge(age);
            person.setAddress(address);
            return person;
        }

        private G005Address address(final String city, final String zip, final int number) {
            final G005Address address = new G005Address();
            address.setCity(city);
            address.setZip(zip);
            address.setNumber(number);
            return address;
        }

        private void assertUuidFragment(final String value) {
            assertNotNull(value);
            assertTrue(UUID_FRAGMENT.matcher(value).matches(), value);
        }

        private void assertEmail(final String value) {
            assertNotNull(value);
            assertTrue(EMAIL.matcher(value).matches(), value);
        }

        // G005-01: a random String is still the 16-character prefix of a version-4 UUID (12 characters + "@email.com" for a
        // property whose name contains "email", ignoring case), at the top level and in a nested bean, and values differ.
        @Test
        public void testNewRandomBean_stringPropertiesAreUuidFragments() {
            final Set<String> names = new HashSet<>();

            for (int i = 0; i < 300; i++) {
                final G005Person person = Beans.newRandomBean(G005Person.class);

                assertUuidFragment(person.getName());
                assertUuidFragment(person.getMail()); // "mail" does not contain "email"
                assertEmail(person.getEmail());
                assertEmail(person.getWorkEMail()); // "workEMail" contains "email" ignoring case
                assertNotNull(person.getAddress());
                assertUuidFragment(person.getAddress().getCity());
                assertUuidFragment(person.getAddress().getZip());
                assertTrue(names.add(person.getName()), person.getName());
            }

            final G005Node node = Beans.newRandomBean(G005Node.class);
            assertUuidFragment(node.getLabel());
            assertNull(node.getParent()); // the reference cycle stops the recursion
        }

        // G005-01: the same shapes through randomize(..) and newRandomBeanList(..); unselected properties are untouched.
        @Test
        public void testRandomize_andNewRandomBeanList_stringShapes() {
            final G005Person person = person(null, null, "keep", 5, null);
            Beans.randomize(person, Arrays.asList("name", "email"));
            assertUuidFragment(person.getName());
            assertEmail(person.getEmail());
            assertEquals("keep", person.getMail());
            assertNull(person.getWorkEMail());
            assertEquals(5, person.getAge());
            assertNull(person.getAddress());

            Beans.randomize(person);
            assertUuidFragment(person.getName());
            assertUuidFragment(person.getMail());
            assertEmail(person.getEmail());
            assertEmail(person.getWorkEMail());
            assertUuidFragment(person.getAddress().getCity());

            final List<G005Person> list = Beans.newRandomBeanList(G005Person.class, 20);
            final Set<String> names = new HashSet<>();
            assertEquals(20, list.size());

            for (final G005Person e : list) {
                assertUuidFragment(e.getName());
                assertEmail(e.getEmail());
                assertUuidFragment(e.getAddress().getZip());
                assertTrue(names.add(e.getName()), e.getName());
            }

            final List<G005Person> partial = Beans.newRandomBeanList(G005Person.class, Arrays.asList("workEMail"), 3);
            assertEquals(3, partial.size());

            for (final G005Person e : partial) {
                assertEmail(e.getWorkEMail());
                assertNull(e.getName());
                assertNull(e.getAddress());
            }

            assertTrue(Beans.newRandomBeanList(G005Person.class, 0).isEmpty());
        }

        // G005-01: callers on several threads each get well-formed values (every value comes from the calling thread's own
        // ThreadLocalRandom).
        @Test
        public void testNewRandomBean_concurrentCallers() throws Exception {
            final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);

            try {
                final List<java.util.concurrent.Future<Set<String>>> futures = new ArrayList<>();

                for (int t = 0; t < 4; t++) {
                    futures.add(executor.submit(() -> {
                        final Set<String> names = new HashSet<>();

                        for (int i = 0; i < 200; i++) {
                            final G005Person person = Beans.newRandomBean(G005Person.class);
                            assertUuidFragment(person.getName());
                            assertEmail(person.getEmail());
                            names.add(person.getName());
                        }

                        return names;
                    }));
                }

                final Set<String> all = new HashSet<>();

                for (final java.util.concurrent.Future<Set<String>> future : futures) {
                    all.addAll(future.get(60, java.util.concurrent.TimeUnit.SECONDS));
                }

                assertEquals(800, all.size());
            } finally {
                executor.shutdownNow();
            }
        }

        // G005-02: the null-selection (default) paths of beanToMap / deepBeanToMap / beanToFlatMap and N.convert(bean, Map)
        // produce the same maps, keys and order; a null bean leaves the output untouched.
        @Test
        public void testBeanToMap_nullSelectionPathsKeepResults() {
            final G005Address address = address("c", null, 4);
            final G005Person person = person("n", null, "m", 3, address);

            final Map<String, Object> shallow = Beans.beanToMap(person);
            assertEquals(Arrays.asList("name", "mail", "age", "address"), new ArrayList<>(shallow.keySet()));
            assertEquals("n", shallow.get("name"));
            assertEquals("m", shallow.get("mail"));
            assertEquals(3, shallow.get("age"));
            assertSame(address, shallow.get("address"));
            assertEquals(shallow, CommonUtil.convert(person, Map.class));

            final Map<String, Object> output = new LinkedHashMap<>();
            output.put("k", 1);
            Beans.beanToMap(person, output);
            assertEquals(Arrays.asList("k", "name", "mail", "age", "address"), new ArrayList<>(output.keySet()));

            final Map<String, Object> snake = new LinkedHashMap<>();
            Beans.beanToMap(person, (Collection<String>) null, NamingPolicy.SCREAMING_SNAKE_CASE, snake);
            assertEquals(Arrays.asList("NAME", "MAIL", "AGE", "ADDRESS"), new ArrayList<>(snake.keySet()));

            final Map<String, Object> deep = Beans.deepBeanToMap(person);
            assertEquals(Arrays.asList("name", "mail", "age", "address"), new ArrayList<>(deep.keySet()));
            final Map<String, Object> expectedAddress = new LinkedHashMap<>();
            expectedAddress.put("city", "c");
            expectedAddress.put("number", 4);
            assertEquals(expectedAddress, deep.get("address"));
            assertEquals(Arrays.asList("city", "number"), new ArrayList<>(((Map<?, ?>) deep.get("address")).keySet()));

            final TreeMap<String, Object> deepTree = Beans.deepBeanToMap(person, IntFunctions.<String, Object> ofTreeMap());
            assertTrue(deepTree.get("address") instanceof TreeMap, String.valueOf(deepTree.get("address")));

            final Map<String, Object> flat = Beans.beanToFlatMap(person);
            assertEquals(Arrays.asList("name", "mail", "age", "address.city", "address.number"), new ArrayList<>(flat.keySet()));
            assertEquals("c", flat.get("address.city"));

            final Map<String, Object> flatSnake = Beans.beanToFlatMap(person, (Collection<String>) null, NamingPolicy.SCREAMING_SNAKE_CASE,
                    IntFunctions.<String, Object> ofLinkedHashMap());
            assertEquals(Arrays.asList("NAME", "MAIL", "AGE", "ADDRESS.CITY", "ADDRESS.NUMBER"), new ArrayList<>(flatSnake.keySet()));

            final Map<String, Object> deepOutput = new LinkedHashMap<>();
            deepOutput.put("k", 1);
            Beans.deepBeanToMap(person, deepOutput);
            Beans.beanToFlatMap(person, deepOutput);
            assertEquals(Arrays.asList("k", "name", "mail", "age", "address", "address.city", "address.number"), new ArrayList<>(deepOutput.keySet()));

            // the selection paths are unchanged: a selected null property is kept, two spellings of one property give one entry
            assertEquals(Arrays.asList("name", "email"), new ArrayList<>(Beans.beanToMap(person, Arrays.asList("name", "email", "NAME")).keySet()));

            final Map<String, Object> untouched = new LinkedHashMap<>();
            untouched.put("k", 1);
            Beans.beanToMap(null, untouched);
            Beans.deepBeanToMap(null, untouched);
            Beans.beanToFlatMap(null, untouched);
            assertEquals(Collections.singletonMap("k", 1), untouched);
            assertTrue(Beans.beanToMap((Object) null).isEmpty());
            assertTrue(Beans.deepBeanToMap((Object) null).isEmpty());
            assertTrue(Beans.beanToFlatMap((Object) null).isEmpty());
        }

        // G005-02: a non-bean is rejected on the default paths with the same IllegalArgumentException as before, before
        // anything is written, and the deep/flat cycle guard is clean afterwards.
        @Test
        public void testBeanToMap_nullSelectionPathsRejectNonBeanBeforeWriting() {
            final Map<String, Object> output = new LinkedHashMap<>();
            output.put("k", 1);

            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap("abc")).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap("abc", output)).getMessage());
            assertEquals(NOT_A_BEAN_STRING,
                    assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap("abc", (Collection<String>) null, NamingPolicy.SNAKE_CASE, output))
                            .getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap("abc")).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap("abc", output)).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap("abc")).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap("abc", output)).getMessage());
            assertEquals(NOT_A_BEAN_INTEGER, assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap(5, output)).getMessage());
            assertEquals(NOT_A_BEAN_INTEGER, assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(5, output)).getMessage());
            assertEquals(NOT_A_BEAN_INTEGER, assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(5, output)).getMessage());
            // the selection paths reject it the same way
            assertEquals(NOT_A_BEAN_STRING,
                    assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap("abc", Arrays.asList("name"), output)).getMessage());
            assertEquals(NOT_A_BEAN_STRING,
                    assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap("abc", Arrays.asList("name"), output)).getMessage());
            assertEquals(Collections.singletonMap("k", 1), output);

            // the cycle guard was left clean by those failures
            final G005Node child = new G005Node();
            final G005Node parent = new G005Node();
            child.setLabel("c");
            child.setParent(parent);
            parent.setLabel("p");
            assertEquals(Arrays.asList("label", "parent.label"), new ArrayList<>(Beans.beanToFlatMap(child).keySet()));
            assertEquals("p", ((Map<?, ?>) Beans.deepBeanToMap(child).get("parent")).get("label"));

            parent.setParent(child);
            final String deepMessage = assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(child)).getMessage();
            final String flatMessage = assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(child)).getMessage();
            assertTrue(deepMessage.startsWith("Cyclic bean reference cannot be converted to a map: "), deepMessage);
            assertTrue(flatMessage.startsWith("Cyclic bean reference cannot be flattened: "), flatMessage);

            parent.setParent(null);
            assertEquals(2, Beans.deepBeanToMap(child).size());
        }

        private String key(final NamingPolicy policy, final String propName) {
            return policy == NamingPolicy.CAMEL_CASE || policy == NamingPolicy.NO_CHANGE ? propName : policy.convert(propName);
        }

        private List<String> keys(final Map<String, Object> map) {
            return new ArrayList<>(map.keySet());
        }

        // G005-04: under every NamingPolicy each key is exactly policy.convert(propertyName) (the name itself for
        // CAMEL_CASE / NO_CHANGE) on every conversion path - top level, selected, nested, flattened, builder - both
        // before and after the converted names are cached, and for a property name too long to be cached.
        @Test
        public void testBeanToMap_keyNamingPoliciesConvertEveryKeyLikeThePolicy() {
            final G005Names bean = new G005Names();
            bean.setUserID("u");
            bean.setHtmlURLValue("h");
            bean.setValue2Go(2);
            bean.setX("x");
            bean.setHomeAddress(address("c", "z", 3));
            bean.setLongPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij(
                    "l");

            final List<String> propNames = Beans.getPropNameList(G005Names.class);
            assertEquals(Arrays.asList("userID", "htmlURLValue", "value2Go", "x", "homeAddress", LONG_PROP), propNames);
            final List<String> addressPropNames = Arrays.asList("city", "zip", "number");

            for (int round = 0; round < 2; round++) {
                for (final NamingPolicy policy : NamingPolicy.values()) {
                    final List<String> top = new ArrayList<>();
                    final List<String> flat = new ArrayList<>();

                    for (final String propName : propNames) {
                        top.add(key(policy, propName));

                        if ("homeAddress".equals(propName)) {
                            for (final String nested : addressPropNames) {
                                flat.add(key(policy, propName) + "." + key(policy, nested));
                            }
                        } else {
                            flat.add(key(policy, propName));
                        }
                    }

                    final List<String> nestedKeys = new ArrayList<>();

                    for (final String nested : addressPropNames) {
                        nestedKeys.add(key(policy, nested));
                    }

                    final String message = policy + " round " + round;
                    final String addressKey = key(policy, "homeAddress");

                    assertEquals(top, keys(Beans.beanToMap(bean, false, null, policy)), message);
                    assertEquals(top, keys(Beans.beanToMap(bean, propNames, policy, IntFunctions.<String, Object> ofLinkedHashMap())), message);

                    final Map<String, Object> deep = Beans.deepBeanToMap(bean, false, null, policy);
                    assertEquals(top, keys(deep), message);
                    assertEquals(nestedKeys, new ArrayList<>(((Map<?, ?>) deep.get(addressKey)).keySet()), message);

                    final Map<String, Object> deepSelected = Beans.deepBeanToMap(bean, propNames, policy, IntFunctions.<String, Object> ofLinkedHashMap());
                    assertEquals(top, keys(deepSelected), message);
                    assertEquals(nestedKeys, new ArrayList<>(((Map<?, ?>) deepSelected.get(addressKey)).keySet()), message);

                    assertEquals(flat, keys(Beans.beanToFlatMap(bean, false, null, policy)), message);
                    assertEquals(flat, keys(Beans.beanToFlatMap(bean, propNames, policy, IntFunctions.<String, Object> ofLinkedHashMap())), message);

                    assertEquals(top, keys(Beans.mapBuilder(bean).naming(policy).toMap()), message);
                    assertEquals(top, keys(Beans.mapBuilder(bean).naming(policy).skipNulls().toMap()), message);
                    final Map<String, Object> builderDeep = Beans.mapBuilder(bean).naming(policy).skipNulls().deep().toMap();
                    assertEquals(top, keys(builderDeep), message);
                    assertEquals(nestedKeys, new ArrayList<>(((Map<?, ?>) builderDeep.get(addressKey)).keySet()), message);
                    assertEquals(flat, keys(Beans.mapBuilder(bean).naming(policy).flat().toMap()), message);
                    assertEquals(flat, keys(Beans.mapBuilder(bean).naming(policy).skipNulls().flat().toMap()), message);

                    assertEquals("u", Beans.beanToMap(bean, false, null, policy).get(key(policy, "userID")), message);
                    assertEquals("l", Beans.beanToFlatMap(bean, false, null, policy).get(key(policy, LONG_PROP)), message);
                }
            }

            // spot checks of the converted forms themselves
            assertEquals(Arrays.asList("user_id", "html_url_value", "value2_go", "x", "home_address", Strings.toSnakeCase(LONG_PROP)),
                    keys(Beans.beanToMap(bean, false, null, NamingPolicy.SNAKE_CASE)));
            assertEquals("HOME_ADDRESS.CITY", keys(Beans.beanToFlatMap(bean, false, null, NamingPolicy.SCREAMING_SNAKE_CASE)).get(4));
        }
    }

    // ---- perf review 2026-10-08 G005 end ----
    // ---- perf review 2026-10-08 G006 begin ----
    @Nested
    class G006 {
        // A real Brotli stream whose decompressed payload is "hello": hex 0b 02 80 68 65 6c 6c 6f 03.
        private final byte[] helloBr = { 0x0b, 0x02, (byte) 0x80, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x03 };

        // G006-01: array reads and skip never touch the single-byte read buffer, so they must not allocate it
        @Test
        void testBrotliInputStreamRead_arrayReadsAndSkipLeaveTheSingleByteBufferUnallocated() throws Exception {
            try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr))) {
                assertEquals("unallocated", readBufferState(in));

                final byte[] out = new byte[8];
                assertEquals(2, in.read(out, 0, 2));
                assertEquals(1, in.skip(1));
                assertEquals(2, in.read(out));
                assertEquals("lo", new String(out, 0, 2, java.nio.charset.StandardCharsets.UTF_8));
                assertEquals(-1, in.read(out));
                assertEquals(0, in.skip(5));
                assertEquals("unallocated", readBufferState(in));

                // The first single-byte read creates it with the configured size.
                assertEquals(-1, in.read());
                assertEquals("byte[" + org.brotli.dec.BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE + "]", readBufferState(in));
            }

            try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr), 3)) {
                assertEquals("unallocated", readBufferState(in));
                assertEquals('h', in.read());
                assertEquals("byte[3]", readBufferState(in));
            }
        }

        private String readBufferState(final BrotliInputStream in) throws ReflectiveOperationException {
            final java.lang.reflect.Field field = BrotliInputStream.class.getDeclaredField("buf");
            field.setAccessible(true);
            final byte[] buffer = (byte[]) field.get(in);

            return buffer == null ? "unallocated" : "byte[" + buffer.length + "]";
        }

        // G006-01: single-byte, array and skip reads interleave exactly as before, for every buffer size and refill pattern
        @Test
        void testBrotliInputStreamRead_interleavedReadsReturnThePayloadForEveryBufferSize() throws Exception {
            final Random random = new Random(20261008);
            final byte[] payload = new byte[70_000]; // two uncompressed meta-blocks (65,536 + 4,464 bytes)
            random.nextBytes(payload);
            final byte[] stream = uncompressedBrotli(payload);

            for (final int size : new int[] { 1, 2, 3, 7, 64, 4096, 65_536, 70_001, org.brotli.dec.BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE }) {
                // single-byte reads only
                try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(stream), size)) {
                    final byte[] read = new byte[payload.length];
                    int n = 0;
                    int b;
                    while ((b = in.read()) != -1) {
                        read[n++] = (byte) b;
                    }
                    assertEquals(payload.length, n, "size=" + size);
                    assertArrayEquals(payload, read, "size=" + size);
                    assertEquals(-1, in.read());
                    assertEquals(-1, in.read(new byte[4]));
                }

                // a seeded mix of read(), read(byte[], int, int), read(byte[]) and skip(long)
                for (int round = 0; round < 3; round++) {
                    try (BrotliInputStream in = new BrotliInputStream(new java.io.ByteArrayInputStream(stream), size)) {
                        final java.io.ByteArrayOutputStream read = new java.io.ByteArrayOutputStream();
                        final java.io.ByteArrayOutputStream expected = new java.io.ByteArrayOutputStream();
                        int position = 0;

                        while (true) {
                            final int op = random.nextInt(4);

                            if (op == 0) {
                                final int b = in.read();
                                if (b == -1) {
                                    assertEquals(payload.length, position, "size=" + size);
                                    break;
                                }
                                read.write(b);
                                expected.write(payload[position++]);
                            } else if (op == 1 || op == 2) {
                                final byte[] chunk = new byte[1 + random.nextInt(300)];
                                final int n = op == 1 ? in.read(chunk, 0, chunk.length) : in.read(chunk);
                                if (n == -1) {
                                    assertEquals(payload.length, position, "size=" + size);
                                    break;
                                }
                                assertTrue(n > 0 && n <= chunk.length);
                                read.write(chunk, 0, n);
                                expected.write(payload, position, n);
                                position += n;
                            } else {
                                final long skipped = in.skip(random.nextInt(200));
                                assertTrue(skipped >= 0 && position + skipped <= payload.length);
                                position += (int) skipped;
                            }
                        }

                        assertArrayEquals(expected.toByteArray(), read.toByteArray(), "size=" + size);
                        assertEquals(-1, in.read());
                    }
                }
            }
        }

        // G006-01: failures of the first single-byte read are unchanged (closed stream, truncated and invalid input)
        @Test
        void testBrotliInputStreamRead_firstSingleByteReadFailuresAreUnchanged() throws Exception {
            final BrotliInputStream closed = new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr));
            closed.close();
            assertThrows(IllegalStateException.class, () -> closed.read());
            assertThrows(IllegalStateException.class, () -> closed.read(new byte[4], 0, 4));
            assertThrows(IllegalStateException.class, () -> closed.skip(1));
            assertEquals(0, closed.read(new byte[0]));

            try (BrotliInputStream empty = new BrotliInputStream(new java.io.ByteArrayInputStream(new byte[0]))) {
                assertThrows(java.io.IOException.class, () -> empty.read());
            }

            try (BrotliInputStream invalid = new BrotliInputStream(new java.io.ByteArrayInputStream(new byte[] { 1, 2, 3, 4, 5 }))) {
                assertThrows(java.io.IOException.class, () -> invalid.read());
            }

            assertThrows(IllegalArgumentException.class, () -> new BrotliInputStream(new java.io.ByteArrayInputStream(helloBr), 0));
            assertThrows(IllegalArgumentException.class, () -> new BrotliInputStream(null, 16));
        }

        /**
         * Builds a valid Brotli stream (WBITS 16) that stores {@code payload} in uncompressed meta-blocks of at most
         * 65,536 bytes each, followed by an empty last meta-block.
         */
        private byte[] uncompressedBrotli(final byte[] payload) {
            final BrotliBits bits = new BrotliBits();
            bits.write(0, 1); // WBITS = 16
            int offset = 0;

            while (offset < payload.length) {
                final int length = Math.min(65_536, payload.length - offset);
                bits.write(0, 1); // ISLAST = 0
                bits.write(0, 2); // MNIBBLES = 4
                bits.write(length - 1, 16); // MLEN - 1
                bits.write(1, 1); // ISUNCOMPRESSED = 1
                bits.alignToByte();
                bits.bytes.write(payload, offset, length);
                offset += length;
            }

            bits.write(1, 1); // ISLAST = 1
            bits.write(1, 1); // ISLASTEMPTY = 1
            bits.alignToByte();

            return bits.bytes.toByteArray();
        }

        /**
         * Least-significant-bit-first bit writer for the Brotli stream format.
         */
        private final class BrotliBits {
            private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            private int current;
            private int used;

            void write(final int value, final int count) {
                for (int i = 0; i < count; i++) {
                    current |= ((value >>> i) & 1) << used;

                    if (++used == 8) {
                        bytes.write(current);
                        current = 0;
                        used = 0;
                    }
                }
            }

            void alignToByte() {
                if (used > 0) {
                    bytes.write(current);
                    current = 0;
                    used = 0;
                }
            }
        }
    }

    // ---- perf review 2026-10-08 G006 end ----
    // ---- perf review 2026-10-08 G008 begin ----
    @Nested
    class G008 {

        private int referenceHash(final byte[] a, final int size) {
            int h = 1;

            for (int i = 0; i < size; i++) {
                h = 31 * h + a[i];
            }

            return h;
        }

        @SuppressWarnings("deprecation")
        private int capacity(final ByteList list) {
            return list.internalArray().length;
        }

        private void assertHashMatchesContent(final ByteList list) {
            final byte[] content = list.toArray();

            assertEquals(referenceHash(content, content.length), list.hashCode());
            assertEquals(Arrays.hashCode(content), list.hashCode());
        }

        // G008-01: an exactly sized list (Arrays.hashCode path) and a list with spare capacity (range path) hash the same content identically
        @Test
        public void testHashCode_exactAndSpareCapacityAgree() {
            final Random rnd = new Random(20261008L);
            final List<Integer> sizes = new ArrayList<>();

            for (int n = 0; n <= 70; n++) {
                sizes.add(n);
            }

            sizes.addAll(Arrays.asList(127, 128, 255, 256, 257, 1000, 1024, 4096, 65537));

            for (final int n : sizes) {
                for (int round = 0; round < 4; round++) {
                    final byte[] a = new byte[n];

                    if (round == 0 || round == 1) {
                        rnd.nextBytes(a);
                    } else if (round == 2) {
                        Arrays.fill(a, Byte.MIN_VALUE);
                    } else {
                        Arrays.fill(a, (byte) -1);
                    }

                    final int expected = referenceHash(a, n);
                    assertEquals(Arrays.hashCode(a), expected);

                    // exactly sized backing arrays
                    final ByteList exact = ByteList.of(a.clone());
                    assertEquals(n, capacity(exact));
                    assertEquals(expected, exact.hashCode());
                    assertEquals(expected, ByteList.copyOf(a).hashCode());
                    assertEquals(expected, exact.copy().hashCode());
                    assertEquals(expected, ByteIterator.of(a).toList().hashCode());

                    // spare capacity: grown by add, presized, and a window over a larger array whose tail is not zero
                    final ByteList grown = new ByteList();

                    for (final byte b : a) {
                        grown.add(b);
                    }

                    assertEquals(expected, grown.hashCode());

                    final ByteList presized = new ByteList(n + 7);
                    presized.addAll(a);
                    assertTrue(capacity(presized) > presized.size());
                    assertEquals(expected, presized.hashCode());

                    final byte[] wider = Arrays.copyOf(a, n + 5);
                    Arrays.fill(wider, n, n + 5, (byte) 0x5A);
                    final ByteList window = ByteList.of(wider, n);
                    assertTrue(capacity(window) > window.size());
                    assertEquals(expected, window.hashCode());

                    // equal lists hash equal across both paths; trimming moves a list onto the exact path
                    assertEquals(exact, presized);
                    assertEquals(exact.hashCode(), presized.hashCode());
                    assertEquals(exact.hashCode(), window.hashCode());
                    assertEquals(expected, presized.trimToSize().hashCode());
                    assertEquals(n, capacity(presized));
                }
            }
        }

        // G008-01: the hash follows mutations while a list moves between the exact-size and spare-capacity paths
        @Test
        public void testHashCode_tracksMutationsAcrossPaths() {
            final ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertEquals(30817, list.hashCode());
            assertHashMatchesContent(list);

            list.add(Byte.MIN_VALUE);
            assertTrue(capacity(list) > list.size());
            assertHashMatchesContent(list);

            list.trimToSize();
            assertEquals(list.size(), capacity(list));
            assertHashMatchesContent(list);

            list.set(0, Byte.MAX_VALUE);
            assertHashMatchesContent(list);

            list.removeAt(1);
            assertHashMatchesContent(list);

            list.addFirst((byte) -7);
            assertHashMatchesContent(list);

            list.clear();
            assertEquals(1, list.hashCode());
            list.trimToSize();
            assertEquals(0, capacity(list));
            assertEquals(1, list.hashCode());

            assertEquals(1, new ByteList().hashCode());
            assertEquals(1, ByteList.of().hashCode());
            assertEquals(1, new ByteList(16).hashCode());
            assertEquals(31 - 7, ByteList.of((byte) -7).hashCode());
            assertEquals(ByteList.of((byte) 5, (byte) -5).hashCode(), Arrays.asList((byte) 5, (byte) -5).hashCode());
        }
    }

    // ---- perf review 2026-10-08 G008 end ----
    // ---- perf review 2026-10-08 G009 begin ----
    @Nested
    class G009 {

        private static final String[] RANGES = { "ascii", "cjk", "full" };

        private char[] randomChars(final Random random, final int length, final String range) {
            final char[] a = new char[length];

            for (int i = 0; i < length; i++) {
                if ("ascii".equals(range)) {
                    a[i] = (char) (0x20 + random.nextInt(95));
                } else if ("cjk".equals(range)) {
                    a[i] = (char) (0x4E00 + random.nextInt(2000));
                } else {
                    a[i] = (char) random.nextInt(65536);
                }
            }

            if (length > 4 && "full".equals(range)) {
                a[0] = Character.MIN_VALUE;
                a[1] = Character.MAX_VALUE;
                a[2] = (char) 63;
                a[3] = (char) 64;
                a[4] = (char) 0xD800;
            }

            return a;
        }

        private Map<Character, Integer> counts(final char[] a) {
            final Map<Character, Integer> counts = new HashMap<>();

            for (final char e : a) {
                counts.merge(e, 1, Integer::sum);
            }

            return counts;
        }

        private boolean take(final Map<Character, Integer> counts, final char e) {
            final Integer count = counts.get(e);

            if (count == null || count == 0) {
                return false;
            }

            counts.put(e, count - 1);
            return true;
        }

        /**
         * Boxed reference for the occurrence-based set operations: {intersection, difference, symmetricDifference}, where the
         * symmetric difference appends b's unmatched occurrences in index order.
         */
        private char[][] reference(final char[] a, final char[] b) {
            final char[] intersection = new char[a.length];
            final char[] difference = new char[a.length];
            final char[] symmetricDifference = new char[a.length + b.length];
            int ni = 0;
            int nd = 0;
            int ns = 0;

            Map<Character, Integer> counts = counts(b);

            for (final char e : a) {
                if (take(counts, e)) {
                    intersection[ni++] = e;
                } else {
                    difference[nd++] = e;
                }
            }

            counts = counts(b);

            for (final char e : a) {
                if (!take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            for (final char e : b) {
                if (take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            return new char[][] { Arrays.copyOf(intersection, ni), Arrays.copyOf(difference, nd), Arrays.copyOf(symmetricDifference, ns) };
        }

        private void assertMultisetOps(final char[] a, final char[] b, final String msg) {
            final char[][] expected = reference(a, b);
            final CharList list = CharList.of(a.clone());
            final CharList other = CharList.of(b.clone());

            final CharList intersection = list.intersection(other);
            final CharList difference = list.difference(other);
            final CharList symmetricDifference = list.symmetricDifference(other);

            assertArrayEquals(expected[0], intersection.toArray(), msg);
            assertArrayEquals(expected[1], difference.toArray(), msg);
            assertArrayEquals(expected[2], symmetricDifference.toArray(), msg);
            assertNotSame(list, intersection, msg);
            assertNotSame(list, difference, msg);
            assertNotSame(list, symmetricDifference, msg);
            assertNotSame(other, symmetricDifference, msg);

            assertArrayEquals(expected[0], list.intersection(b.clone()).toArray(), msg);
            assertArrayEquals(expected[1], list.difference(b.clone()).toArray(), msg);
            assertArrayEquals(expected[2], list.symmetricDifference(b.clone()).toArray(), msg);

            // neither operand is modified
            assertArrayEquals(a, list.toArray(), msg);
            assertArrayEquals(b, other.toArray(), msg);
        }

        // G009-01/G009-02: intersection/difference/symmetricDifference take the count-table path when this list has more than
        // 65,536 elements even if the argument is small (<= 1024); results must match a boxed counting reference on both sides
        // of both thresholds (receiver 65,536/65,537, argument 1,024/1,025), for both overloads and three value ranges.
        @Test
        public void testMultisetOps_largeReceiverMatchesBoxedReference() {
            final Random random = new Random(20261008L);
            final int[] receiverLengths = { 200, 65535, 65536, 65537, 66000 };
            final int[] argumentLengths = { 1, 2, 16, 500, 1024, 1025 };

            for (final int lengthA : receiverLengths) {
                for (final int lengthB : argumentLengths) {
                    for (final String range : RANGES) {
                        final char[] a = randomChars(random, lengthA, range);
                        final char[] b = randomChars(random, lengthB, range);

                        assertMultisetOps(a, b, "a=" + lengthA + ", b=" + lengthB + ", range=" + range);
                    }
                }
            }
        }

        // G009-01: edge cases of the large-receiver count-table path - one repeated value, argument values absent from this list,
        // partially cancelled duplicates in the argument, a self argument, and a receiver holding every char value.
        @Test
        public void testMultisetOps_largeReceiverEdgeCases() {
            final char[] sameValue = new char[70000];
            Arrays.fill(sameValue, 'x');
            assertMultisetOps(sameValue, new char[] { 'x', 'x', 'y' }, "same value");
            assertMultisetOps(sameValue, new char[] { 'y', 'z', (char) 0xFFFF, (char) 0 }, "argument absent");
            assertMultisetOps(sameValue, new char[] { 'x' }, "single argument");

            final CharList xs = CharList.of(sameValue.clone());
            assertEquals(2, xs.intersection(CharList.of('x', 'x', 'y')).size());
            assertEquals(69998, xs.difference(CharList.of('x', 'x', 'y')).size());
            final CharList sym = xs.symmetricDifference(CharList.of('x', 'x', 'y'));
            assertEquals(69999, sym.size());
            assertEquals('y', sym.getLast());

            // one 'b' cancels; b's surviving occurrences are its earliest ones in index order: [.., b, a]
            final char[] oneB = new char[70000];
            Arrays.fill(oneB, 'z');
            oneB[12345] = 'b';
            final CharList oneBList = CharList.of(oneB.clone());
            final CharList oneBSym = oneBList.symmetricDifference(CharList.of('b', 'a', 'b'));
            assertEquals(70001, oneBSym.size());
            assertEquals('b', oneBSym.get(69999));
            assertEquals('a', oneBSym.get(70000));
            assertMultisetOps(oneB, new char[] { 'b', 'a', 'b' }, "partial cancellation");

            final char[] all = new char[65536 + 7];
            for (int i = 0; i < all.length; i++) {
                all[i] = (char) (i * 7919);
            }
            assertMultisetOps(all, new char[] { (char) 0, (char) 0, (char) 0xFFFF, (char) 0xD800, 'a', 'a' }, "every value");

            final Random random = new Random(7L);
            final char[] big = randomChars(random, 70000, "ascii");
            final CharList self = CharList.of(big.clone());
            assertArrayEquals(big, self.intersection(self).toArray());
            assertEquals(0, self.difference(self).size());
            assertEquals(0, self.symmetricDifference(self).size());
            assertArrayEquals(big, self.toArray());
        }
    }

    // ---- perf review 2026-10-08 G009 end ----
    // ---- perf review 2026-10-08 G011 begin ----
    @Nested
    class G011 {

        private int referenceHash(final char[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        private int referenceHash(final byte[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        private int referenceHash(final short[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        private int referenceHash(final int[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        // G011-01: the range hashCode(char[]/byte[]/short[]/int[], from, to) overloads take Arrays.hashCode for the whole-array
        // range; pin both the whole-array and every partial range against the 31 * h + e reference fold.
        @Test
        public void testHashCodeRange_wholeAndPartialRangesMatchReference() {
            final Random random = new Random(20261008L);
            final int[] lengths = { 0, 1, 2, 3, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 1000, 4099 };

            for (final int len : lengths) {
                final char[] ca = new char[len];
                final byte[] ba = new byte[len];
                final short[] sa = new short[len];
                final int[] ia = new int[len];

                for (int i = 0; i < len; i++) {
                    final int r = random.nextInt();
                    // include the extreme values of each type (char is widened unsigned, byte/short signed)
                    ca[i] = i % 11 == 0 ? Character.MAX_VALUE : (char) r;
                    ba[i] = i % 13 == 0 ? Byte.MIN_VALUE : (byte) r;
                    sa[i] = i % 17 == 0 ? Short.MIN_VALUE : (short) r;
                    ia[i] = i % 19 == 0 ? Integer.MIN_VALUE : r;
                }

                // whole array: the new branch
                assertEquals(referenceHash(ca, 0, len), CommonUtil.hashCode(ca, 0, len));
                assertEquals(referenceHash(ba, 0, len), CommonUtil.hashCode(ba, 0, len));
                assertEquals(referenceHash(sa, 0, len), CommonUtil.hashCode(sa, 0, len));
                assertEquals(referenceHash(ia, 0, len), CommonUtil.hashCode(ia, 0, len));
                assertEquals(CommonUtil.hashCode(ia), CommonUtil.hashCode(ia, 0, len));

                // partial ranges: the loop
                final int[][] ranges = { { 0, len - 1 }, { 1, len }, { len / 3, len - len / 3 }, { len / 2, len / 2 }, { len, len } };

                for (final int[] range : ranges) {
                    final int from = range[0];
                    final int to = range[1];

                    if (from < 0 || to < from) {
                        continue;
                    }

                    assertEquals(referenceHash(ca, from, to), CommonUtil.hashCode(ca, from, to));
                    assertEquals(referenceHash(ba, from, to), CommonUtil.hashCode(ba, from, to));
                    assertEquals(referenceHash(sa, from, to), CommonUtil.hashCode(sa, from, to));
                    assertEquals(referenceHash(ia, from, to), CommonUtil.hashCode(ia, from, to));
                }
            }
        }

        // G011-01: null/empty arrays and the range validation (type, message and precedence over the null check) are unchanged.
        @Test
        public void testHashCodeRange_nullEmptyAndValidationUnchanged() {
            assertEquals(0, CommonUtil.hashCode((char[]) null, 0, 0));
            assertEquals(0, CommonUtil.hashCode((byte[]) null, 0, 0));
            assertEquals(0, CommonUtil.hashCode((short[]) null, 0, 0));
            assertEquals(0, CommonUtil.hashCode((int[]) null, 0, 0));
            assertEquals(1, CommonUtil.hashCode(new char[0], 0, 0));
            assertEquals(1, CommonUtil.hashCode(new byte[0], 0, 0));
            assertEquals(1, CommonUtil.hashCode(new short[0], 0, 0));
            assertEquals(1, CommonUtil.hashCode(new int[0], 0, 0));
            assertEquals(31 + 'x', CommonUtil.hashCode(new char[] { 'x' }, 0, 1));
            assertEquals(31 - 7, CommonUtil.hashCode(new byte[] { -7 }, 0, 1));
            assertEquals(31 - 7, CommonUtil.hashCode(new short[] { -7 }, 0, 1));
            assertEquals(31 - 7, CommonUtil.hashCode(new int[] { -7 }, 0, 1));

            final int[] ia = { 1, 2, 3, 4, 5 };
            assertEquals("Index range [0, 1) is out-of-bounds for length 0",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode((int[]) null, 0, 1)).getMessage());
            assertEquals("Index range [0, 6) is out-of-bounds for length 5",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(ia, 0, 6)).getMessage());
            assertEquals("Index range [-1, 5) is out-of-bounds for length 5",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(ia, -1, 5)).getMessage());
            assertEquals("Index range [3, 2) is out-of-bounds for length 5",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(ia, 3, 2)).getMessage());
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(new char[2], 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(new byte[2], 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(new short[2], 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode((char[]) null, 0, 1));
        }

        // G011-01: PrimitiveList.hashCode() passes (0, size): a full list (size == capacity) now takes the whole-array branch,
        // a list with spare capacity the loop; equal lists keep equal hash codes either way.
        @Test
        public void testPrimitiveListHashCode_fullAndSpareCapacityListsAgree() {
            final Random random = new Random(8L);

            for (final int len : new int[] { 0, 1, 5, 16, 33, 1000 }) {
                final int[] ia = new int[len];
                final char[] ca = new char[len];
                final byte[] ba = new byte[len];
                final short[] sa = new short[len];

                for (int i = 0; i < len; i++) {
                    ia[i] = random.nextInt();
                    ca[i] = (char) random.nextInt();
                    ba[i] = (byte) random.nextInt();
                    sa[i] = (short) random.nextInt();
                }

                final IntList fullInts = IntList.of(ia.clone());
                final IntList spareInts = new IntList(Arrays.copyOf(ia, len + 3), len);
                assertEquals(referenceHash(ia, 0, len), fullInts.hashCode());
                assertEquals(referenceHash(ia, 0, len), spareInts.hashCode());
                assertEquals(fullInts, spareInts);

                final CharList fullChars = CharList.of(ca.clone());
                final CharList spareChars = new CharList(Arrays.copyOf(ca, len + 3), len);
                assertEquals(referenceHash(ca, 0, len), fullChars.hashCode());
                assertEquals(referenceHash(ca, 0, len), spareChars.hashCode());
                assertEquals(fullChars, spareChars);

                final ByteList fullBytes = ByteList.of(ba.clone());
                final ByteList spareBytes = new ByteList(Arrays.copyOf(ba, len + 3), len);
                assertEquals(referenceHash(ba, 0, len), fullBytes.hashCode());
                assertEquals(referenceHash(ba, 0, len), spareBytes.hashCode());
                assertEquals(fullBytes, spareBytes);

                final ShortList fullShorts = ShortList.of(sa.clone());
                final ShortList spareShorts = new ShortList(Arrays.copyOf(sa, len + 3), len);
                assertEquals(referenceHash(sa, 0, len), fullShorts.hashCode());
                assertEquals(referenceHash(sa, 0, len), spareShorts.hashCode());
                assertEquals(fullShorts, spareShorts);

                // a mutation through the list is seen by the next hashCode() on both paths
                if (len > 0) {
                    fullInts.set(len - 1, ia[len - 1] + 1);
                    spareInts.set(len - 1, ia[len - 1] + 1);
                    assertEquals(fullInts.hashCode(), spareInts.hashCode());
                    assertEquals(referenceHash(fullInts.toArray(), 0, len), fullInts.hashCode());
                }
            }
        }
    }

    // ---- perf review 2026-10-08 G011 end ----
    // ---- perf review 2026-10-08 G012 begin ----
    @Nested
    class G012 {

        static class StaticBean {
            int value = 7;

            class InnerOfStatic {
                StaticBean outer() {
                    return StaticBean.this;
                }
            }
        }

        class InnerBean {
            int value = 11;

            G012 outer() {
                return G012.this;
            }

            class Deeper {
                InnerBean outer() {
                    return InnerBean.this;
                }
            }
        }

        class InnerWithoutUsableConstructor {
            InnerWithoutUsableConstructor(final int ignored) {
            }
        }

        private final Object anonymousBean = new Object() {
            @Override
            public String toString() {
                return "anonymous bean";
            }
        };

        @SuppressWarnings("serial")
        private final List<String> anonymousList = new ArrayList<>() {
        };

        private <T> void assertFreshInstances(final Class<T> cls, final Class<?> expectedResultClass) {
            final Object first = CommonUtil.newInstance(cls);
            final Object second = CommonUtil.newInstance(cls);

            assertEquals(expectedResultClass, first.getClass());
            assertEquals(expectedResultClass, second.getClass());
            assertNotSame(first, second);
        }

        // G012-01: newInstance caches the anonymous-or-member and enclosing-class lookups; results for every class shape, on
        // first and repeated (cached) calls, interleaved, must be what the uncached lookups produced.
        @Test
        public void testNewInstance_topLevelStaticNestedAndInnerClassesRepeatedly() {
            for (int round = 0; round < 3; round++) {
                assertFreshInstances(Object.class, Object.class);
                assertFreshInstances(StringBuilder.class, StringBuilder.class);
                assertFreshInstances(ArrayList.class, ArrayList.class);
                assertFreshInstances(HashMap.class, HashMap.class);
                assertFreshInstances(LinkedHashSet.class, LinkedHashSet.class);
                assertFreshInstances(List.class, ArrayList.class);
                assertFreshInstances(StaticBean.class, StaticBean.class);
                assertFreshInstances(StaticBean.InnerOfStatic.class, StaticBean.InnerOfStatic.class);
                assertFreshInstances(InnerBean.class, InnerBean.class);
                assertFreshInstances(InnerBean.Deeper.class, InnerBean.Deeper.class);
                assertFreshInstances(anonymousBean.getClass(), anonymousBean.getClass());

                assertTrue(((Collection<?>) CommonUtil.newInstance(ArrayList.class)).isEmpty());
                assertEquals(7, CommonUtil.newInstance(StaticBean.class).value);
                assertEquals(11, CommonUtil.newInstance(InnerBean.class).value);

                // a new enclosing chain is built every time: the caller's enclosing object is never reused
                final InnerBean inner = CommonUtil.newInstance(InnerBean.class);
                assertNotSame(this, inner.outer());
                assertNotSame(inner.outer(), CommonUtil.newInstance(InnerBean.class).outer());

                final InnerBean.Deeper deeper = CommonUtil.newInstance(InnerBean.Deeper.class);
                assertNotNull(deeper.outer());
                assertNotSame(this, deeper.outer().outer());
                assertNotNull(CommonUtil.newInstance(StaticBean.InnerOfStatic.class).outer());
                assertEquals("anonymous bean", CommonUtil.newInstance(anonymousBean.getClass()).toString());
            }
        }

        // G012-01: the rejections that follow the cached lookups keep their exception type and message on every call.
        @Test
        public void testNewInstance_rejectedShapesThrowTheSameExceptionOnEveryCall() {
            class LocalBean {
            }

            for (int round = 0; round < 3; round++) {
                final IllegalArgumentException anonymousContainer = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(anonymousList.getClass()));
                assertEquals("Cannot reflectively instantiate anonymous collection/map class: " + anonymousList.getClass().getName()
                        + ". Use a named container class.", anonymousContainer.getMessage());

                final IllegalArgumentException local = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(LocalBean.class));
                assertTrue(local.getMessage().startsWith("No default constructor found in class: "), local.getMessage());

                final IllegalArgumentException member = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(InnerWithoutUsableConstructor.class));
                assertTrue(member.getMessage().startsWith("No constructor accepting an enclosing instance found in class: "), member.getMessage());

                final IllegalArgumentException noDefault = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Integer.class));
                assertEquals("No default constructor found in class: java.lang.Integer", noDefault.getMessage());

                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Runnable.class));
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(int.class));
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(String[].class));
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance((Class<?>) null));
            }
        }

        // G012-02: an Object[] source is read directly instead of through reflect.Array.get - same elements, same order,
        // same element identity, same destinations.
        @Test
        public void testConvert_objectArraySourceIntoCollections() {
            final com.landawn.abacus.type.Type<List<String>> listOfString = new TypeReference<List<String>>() {
            }.type();
            final String b = new String("b");
            final Object[] mixed = { b, null, "a", 3, "a" };

            final List<String> strings = CommonUtil.convert(mixed, listOfString);
            assertEquals(Arrays.asList("b", null, "a", "3", "a"), strings);
            assertEquals(ArrayList.class, strings.getClass());
            assertSame(b, strings.get(0));

            final String[] typed = { "x", null, "y" };
            final List<String> fromTyped = CommonUtil.convert(typed, listOfString);
            assertEquals(Arrays.asList("x", null, "y"), fromTyped);
            assertSame(typed[0], fromTyped.get(0));
            fromTyped.add("mutable");

            final Set<String> set = CommonUtil.convert(mixed, new TypeReference<Set<String>>() {
            }.type());
            assertEquals(Arrays.asList("b", null, "a", "3"), new ArrayList<>(set));
            assertEquals(LinkedHashSet.class, set.getClass());

            final ImmutableList<Integer> immutable = CommonUtil.convert(new Object[] { "1", 2, null }, new TypeReference<ImmutableList<Integer>>() {
            }.type());
            assertEquals(Arrays.asList(1, 2, null), immutable);

            final Collection<Long> longs = CommonUtil.convert(new Object[] { 1, "2", null }, new TypeReference<Collection<Long>>() {
            }.type());
            assertEquals(Arrays.asList(1L, 2L, null), new ArrayList<>(longs));

            final List<List<Integer>> nested = CommonUtil.convert(new int[][] { { 1, 2 }, null, {} }, new TypeReference<List<List<Integer>>>() {
            }.type());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), null, Collections.emptyList()), nested);

            final List<String> empty = CommonUtil.convert(new Object[0], listOfString);
            assertTrue(empty.isEmpty());
            empty.add("mutable");

            assertSame(mixed, CommonUtil.convert(mixed, Object[].class));
        }

        // G012-02: an array of references is written directly when the converted element is an instance of the component
        // type; element identity, nulls, length and the exact array class are unchanged.
        @Test
        public void testConvert_intoReferenceArrays() {
            final Integer[] integers = CommonUtil.convert(Arrays.asList(1, "2", null, 4L), Integer[].class);
            assertArrayEquals(new Integer[] { 1, 2, null, 4 }, integers);
            assertEquals(Integer[].class, integers.getClass());

            final String first = new String("first");
            final String[] fromObjects = CommonUtil.convert(new Object[] { first, null, 5 }, String[].class);
            assertArrayEquals(new String[] { "first", null, "5" }, fromObjects);
            assertEquals(String[].class, fromObjects.getClass());
            assertSame(first, fromObjects[0]);

            final Number[] numbers = CommonUtil.convert(Arrays.asList(1, 2L, 3.5d), Number[].class);
            assertEquals(Number[].class, numbers.getClass());
            assertEquals(Integer.class, numbers[0].getClass());
            assertEquals(Long.class, numbers[1].getClass());
            assertEquals(Double.class, numbers[2].getClass());

            final Object element = new Object();
            final Object[] objects = CommonUtil.convert(new LinkedList<>(Arrays.asList(element, null)), Object[].class);
            assertEquals(Object[].class, objects.getClass());
            assertSame(element, objects[0]);
            assertNull(objects[1]);

            final String[][] grid = CommonUtil.convert(new Object[] { new Object[] { 1, "a" }, null, Arrays.asList("b") }, String[][].class);
            assertEquals(String[][].class, grid.getClass());
            assertArrayEquals(new String[] { "1", "a" }, grid[0]);
            assertNull(grid[1]);
            assertArrayEquals(new String[] { "b" }, grid[2]);

            final List<String>[] lists = CommonUtil.convert(new Object[] { new Object[] { 1, 2 }, Arrays.asList(3) }, new TypeReference<List<String>[]>() {
            }.type());
            assertEquals(Arrays.asList("1", "2"), lists[0]);
            assertEquals(Arrays.asList("3"), lists[1]);

            assertEquals(0, CommonUtil.convert(new ArrayList<>(), String[].class).length);
            assertEquals(String[].class, CommonUtil.convert(new Object[0], String[].class).getClass());
        }

        // G012-02: primitive arrays keep going through reflect.Array (boxing on read, unboxing and widening on write).
        @Test
        public void testConvert_primitiveArraysUnchanged() {
            assertEquals(Arrays.asList(1, 2, 3), CommonUtil.convert(new int[] { 1, 2, 3 }, new TypeReference<List<Integer>>() {
            }.type()));
            assertArrayEquals(new int[] { 1, 0, 3 }, CommonUtil.convert(Arrays.asList(1L, null, "3"), int[].class));
            assertArrayEquals(new Long[] { 1L, 2L }, CommonUtil.convert(new long[] { 1L, 2L }, Long[].class));
            assertArrayEquals(new double[] { 1.0, 2.5 }, CommonUtil.convert(new Object[] { 1, "2.5" }, double[].class), 0.0);
            assertArrayEquals(new long[] { 4L, 5L }, CommonUtil.convert(new Integer[] { 4, 5 }, long[].class));

            final int[] same = { 9 };
            assertSame(same, CommonUtil.convert(same, int[].class));
        }

        // G012-02: element failures and cycles surface exactly as before, whatever the source or destination kind.
        @Test
        public void testConvert_arrayElementFailuresAndCycles() {
            final com.landawn.abacus.type.Type<List<Integer>> listOfInteger = new TypeReference<List<Integer>>() {
            }.type();

            assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new Object[] { "1", "bad" }, listOfInteger));
            assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new Object[] { "1", "bad" }, Integer[].class));
            assertThrows(NumberFormatException.class, () -> CommonUtil.convert(Arrays.asList("1", "bad"), Integer[].class));

            final Object[] cyclic = new Object[2];
            cyclic[0] = "a";
            cyclic[1] = cyclic;
            final IllegalArgumentException cycle = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(cyclic, String[].class));
            assertEquals("Cyclic container conversion is not supported", cycle.getMessage());
            assertEquals("Cyclic container conversion is not supported",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(cyclic, listOfInteger)).getMessage());

            final IllegalArgumentException notBoolean = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(new Object[] { 1, 2 }, Boolean[].class));
            assertTrue(notBoolean.getMessage().startsWith("Cannot convert"), notBoolean.getMessage());
        }
    }

    // ---- perf review 2026-10-08 G012 end ----
    // ---- perf review 2026-10-08 G014 begin ----
    @Nested
    class G014 {

        private static final int[] COUNTS_AROUND_OLD_CAP = { 1, 2, 1023, 1024, 1025, 1536, 2000, 2999, 3000, 3001, 5000 };

        private List<Integer> distinctValues(final int size) {
            final List<Integer> values = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                values.add((int) ((i * 7919L) % 3001)); // distinct for size <= 3001, not in ascending order
            }

            return values;
        }

        private Collection<Integer> newCollection(final String kind, final List<Integer> values) {
            switch (kind) {
                case "HashSet":
                    return new HashSet<>(values);
                case "LinkedHashSet":
                    return new LinkedHashSet<>(values);
                case "TreeSet":
                    return new TreeSet<>(values);
                case "ArrayDeque":
                    return new java.util.ArrayDeque<>(values);
                case "PriorityQueue":
                    return new java.util.PriorityQueue<>(values);
                case "ConcurrentLinkedQueue":
                    return new java.util.concurrent.ConcurrentLinkedQueue<>(values);
                case "ArrayBlockingQueue":
                    return new java.util.concurrent.ArrayBlockingQueue<>(Math.max(1, values.size()), false, values);
                default:
                    throw new IllegalArgumentException(kind);
            }
        }

        private <T> List<T> iterationOrder(final Iterable<T> c) {
            final List<T> order = new ArrayList<>();

            for (final T e : c) {
                order.add(e);
            }

            return order;
        }

        /**
         * A Collection that counts the hasNext()/next() calls of its iterators and has no descending iterator.
         */
        private final class CountingCollection<T> extends java.util.AbstractCollection<T> {
            private final List<T> backing;
            final AtomicInteger hasNextCalls = new AtomicInteger();
            final AtomicInteger nextCalls = new AtomicInteger();

            CountingCollection(final List<T> backing) {
                this.backing = backing;
            }

            @Override
            public Iterator<T> iterator() {
                final Iterator<T> iterator = backing.iterator();

                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        hasNextCalls.incrementAndGet();
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        nextCalls.incrementAndGet();
                        return iterator.next();
                    }
                };
            }

            @Override
            public int size() {
                return backing.size();
            }

            void resetCounts() {
                hasNextCalls.set(0);
                nextCalls.set(0);
            }
        }

        // G014-01: firstElements(Iterable, n) on a non-List Collection larger than n sizes its result for exactly n elements;
        // pins the elements, their encounter order and the fresh mutable ArrayList result on both sides of the old 1024 cap.
        @Test
        void testFirstElements_nonListCollectionsAroundOldCapacityCap() {
            final List<Integer> values = distinctValues(3000);

            for (final String kind : new String[] { "HashSet", "LinkedHashSet", "TreeSet", "ArrayDeque", "PriorityQueue", "ConcurrentLinkedQueue",
                    "ArrayBlockingQueue" }) {
                final Collection<Integer> c = newCollection(kind, values);
                final List<Integer> order = iterationOrder(c);
                assertEquals(3000, order.size());

                for (final int n : COUNTS_AROUND_OLD_CAP) {
                    final List<Integer> result = CommonUtil.firstElements(c, n);

                    assertEquals(order.subList(0, Math.min(n, order.size())), result, kind + " n=" + n);
                    assertEquals(ArrayList.class, result.getClass(), kind + " n=" + n);
                    result.add(-1); // a fresh, modifiable list
                    assertEquals(3000, c.size(), kind + " n=" + n);
                }

                assertTrue(CommonUtil.firstElements(c, 0).isEmpty());
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(c, -1));
            }

            // a non-Collection Iterable (size unknown) keeps its capped initial capacity and the same result
            final Iterable<Integer> iterable = () -> values.iterator();

            for (final int n : COUNTS_AROUND_OLD_CAP) {
                assertEquals(values.subList(0, Math.min(n, values.size())), CommonUtil.firstElements(iterable, n), "Iterable n=" + n);
            }

            // a Collection that holds nulls (no descending iterator, not a List)
            final List<Integer> withNulls = new ArrayList<>(values);

            for (int i = 0; i < withNulls.size(); i += 5) {
                withNulls.set(i, null);
            }

            final CountingCollection<Integer> counting = new CountingCollection<>(withNulls);

            for (final int n : COUNTS_AROUND_OLD_CAP) {
                assertEquals(withNulls.subList(0, Math.min(n, withNulls.size())), CommonUtil.firstElements(counting, n), "nulls n=" + n);
            }
        }

        // G014-02: lastElements(Iterable, n) on a Collection without a descending iterator (the ArrayDeque window path) sizes the
        // window for exactly n elements; pins elements, order, null elements and the fresh mutable ArrayList result.
        @Test
        void testLastElements_windowPathAroundOldCapacityCap() {
            final List<Integer> values = distinctValues(3000);

            final List<Integer> hashSetValues = new ArrayList<>(values);
            hashSetValues.set(1234, null); // HashSet allows one null element

            final List<Collection<Integer>> sources = new ArrayList<>();
            sources.add(new HashSet<>(hashSetValues));
            sources.add(newCollection("PriorityQueue", values));
            sources.add(newCollection("ConcurrentLinkedQueue", values));
            sources.add(newCollection("ArrayBlockingQueue", values));

            final List<Integer> withNulls = new ArrayList<>(values);

            for (int i = 0; i < withNulls.size(); i += 7) {
                withNulls.set(i, null);
            }

            sources.add(new CountingCollection<>(withNulls));

            for (final Collection<Integer> c : sources) {
                final String kind = c.getClass().getSimpleName();
                final List<Integer> order = iterationOrder(c);
                assertEquals(3000, order.size());

                for (final int n : COUNTS_AROUND_OLD_CAP) {
                    final List<Integer> result = CommonUtil.lastElements(c, n);

                    assertEquals(order.subList(Math.max(0, order.size() - n), order.size()), result, kind + " n=" + n);
                    assertEquals(ArrayList.class, result.getClass(), kind + " n=" + n);
                    result.add(-1); // a fresh, modifiable list
                    assertEquals(3000, c.size(), kind + " n=" + n);
                }

                assertTrue(CommonUtil.lastElements(c, 0).isEmpty());
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(c, -1));
            }

            // a non-Collection Iterable (size unknown) keeps its capped initial capacity and the same result
            final Iterable<Integer> iterable = () -> withNulls.iterator();

            for (final int n : COUNTS_AROUND_OLD_CAP) {
                assertEquals(withNulls.subList(Math.max(0, withNulls.size() - n), withNulls.size()), CommonUtil.lastElements(iterable, n), "Iterable n=" + n);
            }
        }

        // G014-01/02: the source iterator is driven exactly as before: firstElements stops right after the n-th next() (no
        // extra hasNext()), lastElements walks the whole collection once; the backing collection is not modified.
        @Test
        void testFirstLastElements_iteratorCallSequenceOnNonListCollection() {
            final List<Integer> values = distinctValues(2500);
            final CountingCollection<Integer> counting = new CountingCollection<>(values);

            for (final int n : new int[] { 1, 1024, 1025, 2000, 2499 }) {
                counting.resetCounts();
                assertEquals(values.subList(0, n), CommonUtil.firstElements(counting, n), "first n=" + n);
                assertEquals(n, counting.hasNextCalls.get(), "first hasNext n=" + n);
                assertEquals(n, counting.nextCalls.get(), "first next n=" + n);

                counting.resetCounts();
                assertEquals(values.subList(values.size() - n, values.size()), CommonUtil.lastElements(counting, n), "last n=" + n);
                assertEquals(values.size() + 1, counting.hasNextCalls.get(), "last hasNext n=" + n);
                assertEquals(values.size(), counting.nextCalls.get(), "last next n=" + n);
            }

            assertEquals(distinctValues(2500), values);
        }
    }

    // ---- perf review 2026-10-08 G014 end ----
    // ---- perf review 2026-10-08 G015 begin ----
    @Nested
    class G015 {

        private final Class<?>[] componentTypes = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class,
                String.class };

        private Object valueAt(final Class<?> componentType, final int i) {
            if (componentType == boolean.class) {
                return (i * 37 % 5) < 2;
            } else if (componentType == char.class) {
                return (char) (i * 7 + 1);
            } else if (componentType == byte.class) {
                return (byte) (i * 13 + 1);
            } else if (componentType == short.class) {
                return (short) (i * 13 + 1);
            } else if (componentType == int.class) {
                return i * 13 + 1;
            } else if (componentType == long.class) {
                return i * 13L + (1L << 40);
            } else if (componentType == float.class) {
                return i % 7 == 0 ? Float.NaN : (i % 7 == 1 ? -0.0f : i * 0.5f);
            } else if (componentType == double.class) {
                return i % 7 == 0 ? Double.NaN : (i % 7 == 1 ? -0.0d : i * 0.25d);
            } else {
                return i % 4 == 0 ? null : "s" + i;
            }
        }

        private Object newArray(final Class<?> componentType, final int length) {
            final Object a = java.lang.reflect.Array.newInstance(componentType, length);

            for (int i = 0; i < length; i++) {
                java.lang.reflect.Array.set(a, i, valueAt(componentType, i));
            }

            return a;
        }

        private Object copyOf(final Object a) {
            final int length = java.lang.reflect.Array.getLength(a);
            final Object copy = java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), length);
            System.arraycopy(a, 0, copy, 0, length);
            return copy;
        }

        // reference: element i of [fromIndex, toIndex) moves to fromIndex + (i + floorMod(distance, length)) % length
        private Object expectedRotation(final Object original, final int fromIndex, final int toIndex, final int distance) {
            final Object expected = copyOf(original);
            final int length = toIndex - fromIndex;

            if (length > 0) {
                final int shift = Math.floorMod(distance, length);

                for (int i = 0; i < length; i++) {
                    java.lang.reflect.Array.set(expected, fromIndex + (i + shift) % length, java.lang.reflect.Array.get(original, fromIndex + i));
                }
            }

            return expected;
        }

        private void rotate(final Object a, final int fromIndex, final int toIndex, final int distance) {
            if (a instanceof boolean[]) {
                CommonUtil.rotate((boolean[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof char[]) {
                CommonUtil.rotate((char[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof byte[]) {
                CommonUtil.rotate((byte[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof short[]) {
                CommonUtil.rotate((short[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof int[]) {
                CommonUtil.rotate((int[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof long[]) {
                CommonUtil.rotate((long[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof float[]) {
                CommonUtil.rotate((float[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof double[]) {
                CommonUtil.rotate((double[]) a, fromIndex, toIndex, distance);
            } else {
                CommonUtil.rotate((Object[]) a, fromIndex, toIndex, distance);
            }
        }

        private void rotateWhole(final Object a, final int distance) {
            if (a instanceof boolean[]) {
                CommonUtil.rotate((boolean[]) a, distance);
            } else if (a instanceof char[]) {
                CommonUtil.rotate((char[]) a, distance);
            } else if (a instanceof byte[]) {
                CommonUtil.rotate((byte[]) a, distance);
            } else if (a instanceof short[]) {
                CommonUtil.rotate((short[]) a, distance);
            } else if (a instanceof int[]) {
                CommonUtil.rotate((int[]) a, distance);
            } else if (a instanceof long[]) {
                CommonUtil.rotate((long[]) a, distance);
            } else if (a instanceof float[]) {
                CommonUtil.rotate((float[]) a, distance);
            } else if (a instanceof double[]) {
                CommonUtil.rotate((double[]) a, distance);
            } else {
                CommonUtil.rotate((Object[]) a, distance);
            }
        }

        private void assertSameElements(final Object expected, final Object actual, final String message) {
            final int length = java.lang.reflect.Array.getLength(expected);
            assertEquals(length, java.lang.reflect.Array.getLength(actual), message);

            for (int i = 0; i < length; i++) {
                // Float/Double.equals: NaN equals NaN, -0.0 differs from 0.0
                assertEquals(java.lang.reflect.Array.get(expected, i), java.lang.reflect.Array.get(actual, i), message + " at index " + i);
            }
        }

        private void assertRotation(final Object original, final int fromIndex, final int toIndex, final int distance) {
            final Object actual = copyOf(original);
            rotate(actual, fromIndex, toIndex, distance);
            assertSameElements(expectedRotation(original, fromIndex, toIndex, distance), actual, original.getClass().getSimpleName() + " range [" + fromIndex
                    + ", " + toIndex + ") of " + java.lang.reflect.Array.getLength(original) + ", distance " + distance);
        }

        private int[] distancesFor(final int length) {
            final List<Integer> result = new ArrayList<>();

            if (length <= 21) {
                for (int d = -2 * length - 1; d <= 2 * length + 1; d++) {
                    result.add(d);
                }
            } else {
                for (final int d : new int[] { 0, 1, -1, 2, -2, 7, -7, length - 1, length, length + 1, -(length - 1), -length, -(length + 1), length / 2,
                        length / 3 + 1, ((int) (length * 0.618)) | 1, -(((int) (length * 0.618)) | 1), 3 * length + 5 }) {
                    result.add(d);
                }
            }

            result.addAll(Arrays.asList(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE, Integer.MAX_VALUE - 1));

            return result.stream().mapToInt(Integer::intValue).toArray();
        }

        // G015-01: rotate(X[], from, to, distance) uses a shift for distance +/-1 and three reversals otherwise (primitive overloads);
        // results must equal the plain cyclic-shift definition for every length (both sides of the bulk-copy threshold), offset and distance.
        @Test
        void testRotate_allArrayTypesMatchReference() {
            final List<Integer> lengths = new ArrayList<>();

            for (int length = 0; length <= 21; length++) {
                lengths.add(length);
            }

            lengths.addAll(Arrays.asList(31, 64, 65, 257));

            for (final Class<?> componentType : componentTypes) {
                for (final int length : lengths) {
                    for (final int prefix : new int[] { 0, 2 }) {
                        for (final int suffix : new int[] { 0, 3 }) {
                            final Object original = newArray(componentType, prefix + length + suffix);

                            for (final int distance : distancesFor(length)) {
                                assertRotation(original, prefix, prefix + length, distance);
                            }
                        }
                    }
                }
            }
        }

        // G015-01: large ranges (where the former cycle-following loop was cache-unfriendly) give the same result.
        @Test
        void testRotate_largeRangesMatchReference() {
            for (final Class<?> componentType : componentTypes) {
                final int length = componentType == int.class ? 10_007 : 4_096;
                final Object original = newArray(componentType, length);

                for (final int distance : new int[] { 1, -1, 2, -3, ((int) (length * 0.618)) | 1, length / 2, length / 3 + 1, length - 1, Integer.MIN_VALUE,
                        Integer.MAX_VALUE }) {
                    assertRotation(original, 0, length, distance);
                    assertRotation(original, 3, length - 2, distance);
                }
            }
        }

        // G015-01: the whole-array overloads, null/empty/singleton arrays and the unchanged range validation.
        @Test
        void testRotate_wholeArrayNullAndInvalidRanges() {
            for (final Class<?> componentType : componentTypes) {
                for (final int length : new int[] { 0, 1, 2, 9, 10, 11, 40 }) {
                    for (final int distance : new int[] { -11, -1, 0, 1, 3, 10, 41 }) {
                        final Object original = newArray(componentType, length);
                        final Object actual = copyOf(original);
                        rotateWhole(actual, distance);
                        assertSameElements(expectedRotation(original, 0, length, distance), actual,
                                componentType + " whole length " + length + ", distance " + distance);
                    }
                }

                final Object a = newArray(componentType, 5);
                assertThrows(IndexOutOfBoundsException.class, () -> rotate(a, -1, 2, 1));
                assertThrows(IndexOutOfBoundsException.class, () -> rotate(a, 3, 2, 1));
                assertThrows(IndexOutOfBoundsException.class, () -> rotate(a, 0, 6, 1));
                assertSameElements(newArray(componentType, 5), a, "unchanged after failed validation");
            }

            CommonUtil.rotate((boolean[]) null, 1);
            CommonUtil.rotate((char[]) null, 1);
            CommonUtil.rotate((byte[]) null, 1);
            CommonUtil.rotate((short[]) null, 1);
            CommonUtil.rotate((int[]) null, 1);
            CommonUtil.rotate((long[]) null, 1);
            CommonUtil.rotate((float[]) null, 1);
            CommonUtil.rotate((double[]) null, 1);
            CommonUtil.rotate((int[]) null, 0, 0, 7);
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate((int[]) null, 0, 1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate((byte[]) null, 0, 1, 1));
        }

        // G015-01: the primitive lists rotate their used range [0, size) of a larger backing array through N.rotate.
        @Test
        void testRotate_primitiveListsWithSpareCapacity() {
            for (final int size : new int[] { 2, 9, 10, 37, 1000 }) {
                for (final int distance : new int[] { 1, -1, 4, -(size / 3 + 1), size + 2 }) {
                    final IntList intList = new IntList(size * 2 + 5);
                    final ByteList byteList = new ByteList(size * 2 + 5);
                    final int[] expected = new int[size];

                    for (int i = 0; i < size; i++) {
                        intList.add(i * 3 + 1);
                        byteList.add((byte) (i * 3 + 1));
                        expected[Math.floorMod(i + distance, size)] = i * 3 + 1;
                    }

                    intList.rotate(distance);
                    byteList.rotate(distance);

                    assertArrayEquals(expected, intList.toArray(), "IntList size " + size + ", distance " + distance);

                    final byte[] expectedBytes = new byte[size];

                    for (int i = 0; i < size; i++) {
                        expectedBytes[i] = (byte) expected[i];
                    }

                    assertArrayEquals(expectedBytes, byteList.toArray(), "ByteList size " + size + ", distance " + distance);
                }
            }
        }
    }

    // ---- perf review 2026-10-08 G015 end ----
    // ---- perf review 2026-10-08 G017 begin ----
    @Nested
    class G017 {

        // G017-01: unquoted fields with surrounding whitespace are stripped (now from the builder, one String per field)
        @Test
        void testParseLine_paddedUnquotedFields() {
            final CsvParser parser = new CsvParser();

            assertEquals(Arrays.asList("a", "b", "c", "d"), parser.parseLine("a , b ,c  ,  d  "));
            assertEquals(Arrays.asList("John", "Smith", "42"), parser.parseLine("John      ,Smith     ,42        "));
            assertEquals(Arrays.asList("New York", "x"), parser.parseLine("New York  , x"));
            assertEquals(Arrays.asList("", "", ""), parser.parseLine(" , ,"));
            assertEquals(Arrays.asList(""), parser.parseLine("   "));
            assertEquals(Arrays.asList(""), parser.parseLine(""));
            assertEquals(Arrays.asList("a", "b"), parser.parseLine("a,b   "));
            assertEquals(Arrays.asList("x", "y"), parser.parseLine("x \u001F,y"));
            // quoted fields keep their inner whitespace; whitespace around the quotes is dropped
            assertEquals(Arrays.asList(" a ", "b "), parser.parseLine("\" a \",\"b \""));
            assertEquals(Arrays.asList("a", "b"), parser.parseLine("\"a\"  ,  \"b\"  "));
        }

        // G017-01: Character.isWhitespace decides what is stripped (incl. non-Latin-1 whitespace); N\P is data
        @Test
        void testParseLine_unicodeWhitespaceAndNonLatin1Fields() {
            final CsvParser parser = new CsvParser();

            assertEquals(Arrays.asList("a", "b"), parser.parseLine("\u2003a\u2003,\u3000b\u3000"));
            assertEquals(Arrays.asList("\u00A0a\u00A0", "c"), parser.parseLine("\u00A0a\u00A0,c"));
            assertEquals(Arrays.asList("\u4E2D\u6587", "\u00E9"), parser.parseLine("\u4E2D\u6587 , \u00E9 "));
        }

        // G017-01: leading whitespace inside the field buffer (quote characters consumed by ignoreQuotations) is stripped too
        @Test
        void testParseLine_ignoreQuotationsLeadingWhitespaceInField() {
            final CsvParser ignoreQuotes = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, true, true);

            assertEquals(Arrays.asList("a", "b", "c"), ignoreQuotes.parseLine("a,\" b \",c"));
            assertEquals(Arrays.asList("a", "b"), ignoreQuotes.parseLine("\" a \",\"b \""));
        }

        // G017-01: nothing is stripped when ignoreLeadingWhitespace is off
        @Test
        void testParseLine_noTrimKeepsWhitespace() {
            final CsvParser noTrim = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, false);

            assertEquals(Arrays.asList("a ", " b ", "c  ", "  d  "), noTrim.parseLine("a , b ,c  ,  d  "));
            assertEquals(Arrays.asList("   "), noTrim.parseLine("   "));
            assertEquals(Arrays.asList("\u2003a\u2003", "\u3000b\u3000"), noTrim.parseLine("\u2003a\u2003,\u3000b\u3000"));
        }

        // G017-01: the array/collection overloads strip the same way; an overflow still fails after the leading slots are written
        @Test
        void testParseLineInto_paddedFields() {
            final CsvParser parser = new CsvParser();

            final String[] output = new String[4];
            Arrays.fill(output, "#");
            parser.parseLineInto("x  , y  ,z", output);
            assertArrayEquals(new String[] { "x", "y", "z", "#" }, output);

            assertArrayEquals(new String[] { "p", "q", "r" }, parser.parseLineToArray("  p  ,q  ,  r"));

            final List<String> collected = new ArrayList<>(Arrays.asList("keep"));
            parser.parseLineInto(" m ,n  ", collected);
            assertEquals(Arrays.asList("keep", "m", "n"), collected);

            final String[] small = new String[2];
            Arrays.fill(small, "#");
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> parser.parseLineInto("x  , y  , z  ", small));
            assertArrayEquals(new String[] { "x", "y" }, small);
        }

        // G017-01: stripped values are ordinary equal Strings, including empty and non-Latin-1 results
        @Test
        void testParseLine_strippedValuesAreOrdinaryStrings() {
            final List<String> fields = new CsvParser().parseLine("  ab  ,   ,\u4E2D  ");

            assertEquals(3, fields.size());
            assertEquals("ab", fields.get(0));
            assertEquals("ab".hashCode(), fields.get(0).hashCode());
            assertTrue(fields.get(1).isEmpty());
            assertEquals("\u4E2D", fields.get(2));
            assertEquals(1, fields.get(2).length());
        }

        // G017-02: runs of ordinary characters are copied in bulk - fields longer than the pooled builder, non-Latin-1 inside a run
        @Test
        void testParseLine_longRunsAndNonLatin1() {
            final CsvParser parser = new CsvParser();
            final String big = Strings.repeat('x', 20000) + "\u4E2D" + Strings.repeat('y', 20000);

            final List<String> fields = parser.parseLine(big + ",tail," + big);
            assertEquals(3, fields.size());
            assertEquals(big, fields.get(0));
            assertEquals("tail", fields.get(1));
            assertEquals(big, fields.get(2));

            final String quotedBody = Strings.repeat("a,b ", 5000);
            assertEquals(Arrays.asList(quotedBody, "z"), parser.parseLine("\"" + quotedBody + "\",z"));

            // a UTF-16 field followed by Latin-1 fields that reuse the same builder
            assertEquals(Arrays.asList("\u00E9t\u00E9 \u4E2D\u6587 ok", "plain", "x"), parser.parseLine("\u00E9t\u00E9 \u4E2D\u6587 ok,plain,x"));

            final String[] output = new String[2];
            parser.parseLineInto(big + "," + big, output);
            assertArrayEquals(new String[] { big, big }, output);
        }

        // G017-02: a run stops at every escape, quote and unquoted separator; separators inside quotes stay data
        @Test
        void testParseLine_runBoundaries() {
            final CsvParser escapeParser = new CsvParser(',', '"', '\\');

            assertEquals(Arrays.asList("ab\"cd\\ef", "g"), escapeParser.parseLine("\"ab\\\"cd\\\\ef\",g"));
            assertEquals(Arrays.asList("a\\b", "c"), escapeParser.parseLine("a\\b,c"));
            assertEquals(Arrays.asList("a,b,c", "d"), new CsvParser().parseLine("\"a,b,c\",d"));
            assertEquals(Arrays.asList("a\u0000b", "c"), new CsvParser().parseLine("a\u0000b,c"));
            assertEquals(Arrays.asList("bc\"d\"ef", "g"), new CsvParser().parseLine("bc\"d\"ef,g"));

            // strict quotes: characters outside the quotes are still dropped
            final CsvParser strict = new CsvParser(',', '"', '\\', true);
            assertEquals(Arrays.asList("in side", "q", ""), strict.parseLine("xx\"in side\"yy,\"q\",zz"));

            // ignoreQuotations: quote characters end a run, embedded ones are kept as data
            final CsvParser ignoreQuotes = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, true, true);
            assertEquals(Arrays.asList("ab\"cd", "e"), ignoreQuotes.parseLine("ab\"cd,e"));
        }
    }

    // ---- perf review 2026-10-08 G017 end ----
    // ---- perf review 2026-10-08 G021 begin ----
    @Nested
    class G021 {
        private final java.util.TimeZone newYork = java.util.TimeZone.getTimeZone("America/New_York");

        // A SimpleTimeZone with daylight-saving rules no ZoneId can express: the Calendar fallback of resolveLocalMillis.
        private final java.util.TimeZone customDst = new java.util.SimpleTimeZone(-5 * 3600_000, "custom-dst", 2, 8, -1, 7200000, 10, 1, -1, 7200000, 3600000);

        private void assertTimestamp(final java.time.Instant expected, final java.sql.Timestamp actual) {
            assertEquals(expected.toEpochMilli(), actual.getTime());
            assertEquals(expected.getNano(), actual.getNanos());
        }

        private void assertParseFailure(final String expectedMessage, final Class<? extends Throwable> expectedCause,
                final org.junit.jupiter.api.function.Executable executable) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
            assertEquals(expectedMessage, e.getMessage());
            assertNotNull(e.getCause());
            assertEquals(expectedCause, e.getCause().getClass());
        }

        // G021-01: auto-detected ISO-8601 'Z' timestamps (1-9 fraction digits) are read directly - same instant as before
        @Test
        void testParseToTimestamp_autoUtcFractionValues() {
            final String[] texts = { "2025-01-15T10:30:45.123Z", "2025-01-15T10:30:45.5Z", "2025-01-15T10:30:45.12Z", "2025-01-15T10:30:45.1234Z",
                    "2025-01-15T10:30:45.123456789Z", "2025-01-15T10:30:45.000000001Z", "2025-01-15T10:30:45.0Z", "2024-02-29T23:59:59.999999999Z",
                    "2000-02-29T00:00:00.1Z", "0001-01-01T00:00:00.000Z", "9999-12-31T23:59:59.999999999Z", "1582-10-10T12:00:00.5Z",
                    "1969-12-31T23:59:59.999Z" };

            for (final String text : texts) {
                final java.time.Instant expected = java.time.LocalDateTime.parse(text.substring(0, text.length() - 1)).toInstant(java.time.ZoneOffset.UTC);

                assertTimestamp(expected, Dates.parseToTimestamp(text));
                // the designator the text carries wins over a supplied zone, with or without an empty format
                assertTimestamp(expected, Dates.parseToTimestamp(text, null, newYork));
                assertTimestamp(expected, Dates.parseToTimestamp(text, "", customDst));
            }

            assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z").getTime());
            assertEquals(500000000, Dates.parseToTimestamp("2025-01-15T10:30:45.5Z").getNanos());
            assertEquals(123456789, Dates.parseToTimestamp("2025-01-15T10:30:45.123456789Z").getNanos());
        }

        // G021-01: out-of-range fields still get the java.time grammar's diagnostic
        @Test
        void testParseToTimestamp_autoUtcFractionInvalidFields() {
            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;

            assertParseFailure("Cannot parse \"2025-02-29T00:00:00.0Z\" with the auto-detected format: Text '2025-02-29T00:00:00.0Z' could not be parsed: "
                    + "Invalid date 'February 29' as '2025' is not a leap year", dtpe, () -> Dates.parseToTimestamp("2025-02-29T00:00:00.0Z"));
            assertParseFailure("Cannot parse \"2025-02-30T10:30:45.123Z\" with the auto-detected format: Text '2025-02-30T10:30:45.123Z' could not be parsed: "
                    + "Invalid date 'FEBRUARY 30'", dtpe, () -> Dates.parseToTimestamp("2025-02-30T10:30:45.123Z"));
            assertParseFailure("Cannot parse \"2025-13-01T00:00:00.1Z\" with the auto-detected format: Text '2025-13-01T00:00:00.1Z' could not be parsed: "
                    + "Invalid value for MonthOfYear (valid values 1 - 12): 13", dtpe, () -> Dates.parseToTimestamp("2025-13-01T00:00:00.1Z"));
            assertParseFailure("Cannot parse \"2025-01-15T24:00:00.0Z\" with the auto-detected format: Text '2025-01-15T24:00:00.0Z' could not be parsed: "
                    + "Invalid value for HourOfDay (valid values 0 - 23): 24", dtpe, () -> Dates.parseToTimestamp("2025-01-15T24:00:00.0Z"));
            assertParseFailure("Cannot parse \"2025-01-15T23:60:00.0Z\" with the auto-detected format: Text '2025-01-15T23:60:00.0Z' could not be parsed: "
                    + "Invalid value for MinuteOfHour (valid values 0 - 59): 60", dtpe, () -> Dates.parseToTimestamp("2025-01-15T23:60:00.0Z"));
            assertParseFailure("Cannot parse \"2025-01-15T23:59:60.0Z\" with the auto-detected format: Text '2025-01-15T23:59:60.0Z' could not be parsed: "
                    + "Invalid value for SecondOfMinute (valid values 0 - 59): 60", dtpe, () -> Dates.parseToTimestamp("2025-01-15T23:59:60.0Z"));

            final IllegalArgumentException yearZero = assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("0000-01-01T00:00:00.0Z"));
            assertEquals("Format 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'' requires a Common Era year from 0001 through 9999 written as exactly four digits: "
                    + "\"0000-01-01T00:00:00.0Z\"", yearZero.getMessage());
        }

        // G021-01: auto-detected ISO local timestamps (1-9 fraction digits) are read directly; zone resolution unchanged
        @Test
        void testParseToTimestamp_autoLocalFraction() {
            final String[] texts = { "2025-01-15T10:30:45.123", "2025-01-15T10:30:45.5", "2025-01-15T10:30:45.123456", "2025-07-15T10:30:45.123456789",
                    "2024-02-29T23:59:59.999999999", "0001-01-01T00:00:00.1", "9999-12-31T23:59:59.9" };

            for (final String text : texts) {
                final java.time.LocalDateTime local = java.time.LocalDateTime.parse(text);

                assertTimestamp(local.atZone(java.time.ZoneId.of("America/New_York")).toInstant(), Dates.parseToTimestamp(text, null, newYork));
                assertTimestamp(local.toInstant(java.time.ZoneOffset.UTC), Dates.parseToTimestamp(text, "", java.util.TimeZone.getTimeZone("UTC")));
            }

            // the Calendar fallback for rules no ZoneId can express: winter offset -05:00, fraction re-added
            assertTimestamp(java.time.Instant.parse("2025-01-15T15:30:45.123456Z"), Dates.parseToTimestamp("2025-01-15T10:30:45.123456", null, customDst));
            assertTimestamp(java.time.Instant.parse("2025-11-02T06:30:00.500Z"), Dates.parseToTimestamp("2025-11-02T01:30:00.5", null, customDst));

            final Class<? extends Throwable> dte = java.time.DateTimeException.class;
            assertParseFailure(
                    "Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)",
                    dte, () -> Dates.parseToTimestamp("2025-03-09T02:30:00.5", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-11-02T01:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Ambiguous local date-time 2025-11-02T01:30 in zone America/New_York (DST overlap); valid offsets are [-04:00, -05:00]",
                    dte, () -> Dates.parseToTimestamp("2025-11-02T01:30:00.5", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-04-31T00:00:00.1\" with the auto-detected format in time zone America/New_York: "
                            + "Text '2025-04-31T00:00:00.1' could not be parsed: Invalid date 'APRIL 31'",
                    java.time.format.DateTimeParseException.class, () -> Dates.parseToTimestamp("2025-04-31T00:00:00.1", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T24:00:00.1\" with the auto-detected format in time zone custom-dst: "
                            + "Text '2025-01-15T24:00:00.1' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24",
                    java.time.format.DateTimeParseException.class, () -> Dates.parseToTimestamp("2025-01-15T24:00:00.1", null, customDst));

            final IllegalArgumentException gap = assertThrows(IllegalArgumentException.class,
                    () -> Dates.parseToTimestamp("2025-03-09T02:30:00.5", null, customDst));
            assertTrue(gap.getMessage().startsWith("Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone custom-dst: "),
                    gap.getMessage());
            assertEquals(IllegalArgumentException.class, gap.getCause().getClass());
        }

        // G021-01: auto-detected fractional ISO offset timestamps (+HH:mm, compact +HHmm) are read directly
        @Test
        void testParseToTimestamp_autoOffsetFraction() {
            final String[][] cases = { { "2025-01-15T10:30:45.123+05:30", "2025-01-15T05:00:45.123Z" },
                    { "2025-01-15T10:30:45.123-0500", "2025-01-15T15:30:45.123Z" }, { "2025-01-15T10:30:45.5+0545", "2025-01-15T04:45:45.500Z" },
                    { "2025-01-15T10:30:45.123456789-09:30", "2025-01-15T20:00:45.123456789Z" },
                    { "2025-01-15T10:30:45.123+18:00", "2025-01-14T16:30:45.123Z" }, { "2025-01-15T10:30:45.123-18:00", "2025-01-16T04:30:45.123Z" },
                    { "2025-01-15T10:30:45.123-00:00", "2025-01-15T10:30:45.123Z" }, { "2025-01-15T10:30:45.123+0000", "2025-01-15T10:30:45.123Z" },
                    { "2024-02-29T00:00:00.1+01:00", "2024-02-28T23:00:00.100Z" },
                    // a seconds-precision offset keeps the java.time route
                    { "1880-01-15T10:30:45.123-04:56:02", "1880-01-15T15:26:47.123Z" } };

            for (final String[] c : cases) {
                final java.time.Instant expected = java.time.Instant.parse(c[1]);

                assertTimestamp(expected, Dates.parseToTimestamp(c[0]));
                assertTimestamp(expected, Dates.parseToTimestamp(c[0], null, newYork));
            }

            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;
            assertParseFailure(
                    "Cannot parse \"2025-01-15T10:30:45.123+18:01\" with the auto-detected format: Text '2025-01-15T10:30:45.123+18:01' "
                            + "could not be parsed: Zone offset not in valid range: -18:00 to +18:00",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+18:01"));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T10:30:45.123+19:00\" with the auto-detected format: Text '2025-01-15T10:30:45.123+19:00' "
                            + "could not be parsed: Zone offset not in valid range: -18:00 to +18:00",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+19:00"));
            assertParseFailure("Cannot parse \"2025-01-15T10:30:45.123+05:60\" with the auto-detected format: Text '2025-01-15T10:30:45.123+05:60' "
                    + "could not be parsed, unparsed text found at index 26", dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+05:60"));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T10:30:45.123+2400\" with the auto-detected format: Text '2025-01-15T10:30:45.123+24:00' "
                            + "could not be parsed: Value out of range: Hour[0-23], Minute[0-59], Second[0-59]",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+2400"));
            assertParseFailure("Cannot parse \"2025-02-30T10:30:45.123+05:30\" with the auto-detected format: Text '2025-02-30T10:30:45.123+05:30' "
                    + "could not be parsed: Invalid date 'FEBRUARY 30'", dtpe, () -> Dates.parseToTimestamp("2025-02-30T10:30:45.123+05:30"));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T24:00:00.1+05:30\" with the auto-detected format: Text '2025-01-15T24:00:00.1+05:30' "
                            + "could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T24:00:00.1+05:30"));
            assertParseFailure("Cannot parse \"2025-0a-15T10:30:45.123+05:30\" with the auto-detected format: Text '2025-0a-15T10:30:45.123+05:30' "
                    + "could not be parsed at index 5", dtpe, () -> Dates.parseToTimestamp("2025-0a-15T10:30:45.123+05:30"));
        }

        // G021-01: parseToXMLGregorianCalendar shares parseAutoTimestampToInstant - same XML values and fractions
        @Test
        void testParseToXMLGregorianCalendar_autoFraction() {
            assertEquals("2025-01-15T10:30:45.123Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123Z").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.123456789Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789Z").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.500+05:30", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.5+05:30").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.123456789-05:00", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789-0500").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.120-05:00", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.120", null, newYork).toXMLFormat());

            final IllegalArgumentException wide = assertThrows(IllegalArgumentException.class,
                    () -> Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.1+18:00"));
            assertEquals("XMLGregorianCalendar timezone must be a whole-minute offset in the range -14:00 through +14:00; zone 'GMT+18:00' has offset "
                    + "64800000 milliseconds at epoch millisecond 1736872245100", wide.getMessage());
        }

        // G021-01: the directly read ISO local timestamp resolves its wall clock exactly as the JDBC and LOCAL_DATE_TIME routes do
        @Test
        void testParseToTimestamp_autoLocalFractionMatchesOtherRoutes() {
            final String[] zones = { "America/New_York", "Asia/Kolkata", "Australia/Lord_Howe", "America/Sao_Paulo", "UTC" };
            final String[] wallClocks = { "2025-01-15 10:30:45", "2025-03-09 01:59:59", "2025-03-09 03:00:00", "2025-11-02 00:59:59", "2025-11-02 02:00:00",
                    "2025-10-05 02:30:00", "2025-04-06 02:00:00", "1883-11-18 11:59:59", "1899-12-31 23:59:59", "0001-01-01 00:00:00", "9999-12-31 23:59:59" };

            for (final String zoneId : zones) {
                final java.util.TimeZone zone = java.util.TimeZone.getTimeZone(zoneId);
                final java.time.zone.ZoneRules rules = java.time.ZoneId.of(zoneId).getRules();

                for (final String wallClock : wallClocks) {
                    final java.time.LocalDateTime local = java.time.LocalDateTime.parse(wallClock.replace(' ', 'T'));
                    final java.util.List<java.time.ZoneOffset> offsets = rules.getValidOffsets(local);

                    if (offsets.size() != 1) {
                        continue;
                    }

                    final long expected = local.toInstant(offsets.get(0)).toEpochMilli();
                    final String isoLocal = wallClock.replace(' ', 'T');

                    // the auto-detected ISO local timestamp shape, read directly
                    assertEquals(expected + 500, Dates.parseToTimestamp(isoLocal + ".5", null, zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(expected + 123, Dates.parseToTimestamp(isoLocal + ".123456789", "", zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(123456789, Dates.parseToTimestamp(isoLocal + ".123456789", "", zone).getNanos());
                    // the JDBC escape text and the LOCAL_DATE_TIME constant name the same instant
                    assertEquals(expected + 123, Dates.parseToTimestamp(wallClock + ".123456789", null, zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(expected, Dates.parseToTimestamp(wallClock, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(expected, Dates.parseToJUDate(wallClock, null, zone).getTime(), wallClock + " " + zoneId);
                }
            }
        }

        // G021-02: the explicitly supplied 'Z' constants read canonical text directly - same instant on every legacy target
        @Test
        void testParseExplicitUtcConstants_values() {
            final String dt = Dates.ISO_8601_DATE_TIME_FORMAT;
            final String ts = Dates.ISO_8601_TIMESTAMP_FORMAT;
            final String[] dateTimes = { "2025-01-15T10:30:45", "2024-02-29T23:59:59", "2000-02-29T00:00:00", "0001-01-01T00:00:00", "9999-12-31T23:59:59",
                    "1582-10-10T12:00:00", "1969-12-31T23:59:59", "0099-06-30T06:07:08" };
            final java.util.TimeZone[] utcEquivalent = { null, java.util.TimeZone.getTimeZone("UTC"), java.util.TimeZone.getTimeZone("GMT"),
                    java.util.TimeZone.getTimeZone("Etc/UTC"), new java.util.SimpleTimeZone(0, "custom-zero") };

            for (final String local : dateTimes) {
                final long seconds = java.time.LocalDateTime.parse(local).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();

                for (final java.util.TimeZone zone : utcEquivalent) {
                    assertEquals(seconds, Dates.parseToJUDate(local + "Z", dt, zone).getTime(), local);
                    assertEquals(seconds + 123, Dates.parseToTimestamp(local + ".123Z", ts, zone).getTime(), local);
                    assertEquals(123000000, Dates.parseToTimestamp(local + ".123Z", ts, zone).getNanos(), local);
                    assertEquals(seconds + 999, Dates.parseToDate(local + ".999Z", ts, zone).getTime(), local);
                    assertEquals(seconds, Dates.parseToTime(local + "Z", dt, zone).getTime(), local);
                }

                final java.util.Calendar calendar = Dates.parseToCalendar(local + ".005Z", ts);
                assertEquals(seconds + 5, calendar.getTimeInMillis(), local);
                assertEquals("UTC", calendar.getTimeZone().getID());
            }

            assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", ts).getTime());
            assertEquals(-12219681599500L, Dates.parseToTimestamp("1582-10-10T12:00:00.500Z", ts).getTime());
            assertEquals("2025-01-15T10:30:45.123Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123Z", ts).toXMLFormat());
            assertEquals("2025-01-15T10:30:45.000Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45Z", dt).toXMLFormat());
        }

        // G021-02: out-of-range fields still reach the pooled SimpleDateFormat and keep its diagnostic; checks before it are unchanged
        @Test
        void testParseExplicitUtcConstants_diagnostics() {
            final String dt = Dates.ISO_8601_DATE_TIME_FORMAT;
            final String ts = Dates.ISO_8601_TIMESTAMP_FORMAT;
            final String[] badDateTimes = { "2025-02-29T00:00:00Z", "2025-02-30T10:30:45Z", "2025-13-15T10:30:45Z", "2025-00-15T10:30:45Z",
                    "2025-01-00T10:30:45Z", "2025-01-15T24:00:00Z", "2025-01-15T23:60:00Z", "2025-01-15T23:59:60Z" };

            for (final String text : badDateTimes) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, dt));
                assertEquals("Cannot parse \"" + text + "\" with format 'yyyy-MM-dd'T'HH:mm:ss'Z'': Unparseable date: \"" + text + "\"", e.getMessage());
                assertEquals(java.text.ParseException.class, e.getCause().getClass());

                final String withMillis = text.substring(0, 19) + ".000Z";
                final IllegalArgumentException m = assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(withMillis, ts));
                assertEquals("Cannot parse \"" + withMillis + "\" with format 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'': Unparseable date: \"" + withMillis + "\"",
                        m.getMessage());
                assertEquals(java.text.ParseException.class, m.getCause().getClass());
            }

            assertEquals(
                    "Format 'yyyy-MM-dd'T'HH:mm:ss'Z'' requires its exact canonical shape, every field written as digits at its full width: "
                            + "\"2025-01-15T10:30:45z\"; use a variable-width pattern such as \"yyyy-M-d'T'H:m:sXXX\" to accept shorter fields",
                    assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45z", dt)).getMessage());
            assertEquals("Format 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'' requires its exact canonical shape: 24 characters, every field written as digits at its"
                    + " full width, exactly three fractional-second digits and a trailing 'Z': \"2025-01-15T10:30:45.12Z\"; use a variable-width pattern"
                    + " such as \"yyyy-M-d'T'H:m:s.SSSXXX\" to accept shorter fields, or auto-detection (no format), which accepts the JDBC escape grammar"
                    + " with 1-9 fraction digits",
                    assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.12Z", ts)).getMessage());
            assertEquals(
                    "The UTC designator 'Z' requires a UTC-equivalent time zone; input: 2025-01-15T10:30:45.123Z, format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z',"
                            + " time zone: America/New_York (raw offset -05:00)",
                    assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", ts, newYork)).getMessage());
        }
    }

    // ---- perf review 2026-10-08 G021 end ----
    // ---- perf review 2026-10-08 G022 begin ----
    @Nested
    class G022 {
        /** 0001-01-01T00:00:00Z, the first instant the fixed-'Z' constants can render. */
        private static final long YEAR_1_START = -62135596800000L;

        /** 9999-12-31T23:59:59.999Z, the last instant the fixed-'Z' constants can render. */
        private static final long YEAR_9999_END = 253402300799999L;

        private String utcReference(final long millis, final boolean withMillis) {
            return java.time.format.DateTimeFormatter.ofPattern(withMillis ? "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'" : "uuuu-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ROOT)
                    .withZone(java.time.ZoneOffset.UTC)
                    .format(java.time.Instant.ofEpochMilli(millis));
        }

        // G022-01: the two fixed-'Z' constants render exactly the java.time UTC text and the default format's text
        @Test
        public void testFormat_iso8601ConstantsMatchUtcReferenceAndDefaultFormat() {
            final List<Long> instants = new ArrayList<>(
                    Arrays.asList(YEAR_1_START, YEAR_1_START + 1, YEAR_1_START + 999, YEAR_9999_END, YEAR_9999_END - 999, 0L, -1L, 1L, 999L, 1000L, -999L,
                            -1000L, -1001L, -12219292800001L, -12219292800000L, -2208988800001L, 4102358400000L, 1736937045123L, 951782400000L, 946684799999L));
            final Random random = new Random(20261008L);

            for (int i = 0; i < 3000; i++) {
                instants.add(YEAR_1_START + (long) (random.nextDouble() * (YEAR_9999_END - YEAR_1_START)));
            }

            final java.util.TimeZone utc = java.util.TimeZone.getTimeZone("UTC");

            for (final long millis : instants) {
                final java.util.Date date = new java.util.Date(millis);
                final java.sql.Timestamp timestamp = new java.sql.Timestamp(millis);
                timestamp.setNanos(timestamp.getNanos() + random.nextInt(1_000_000)); // a sub-millisecond fraction the 'Z' constants do not print

                assertEquals(utcReference(millis, false), Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT), "millis=" + millis);
                assertEquals(utcReference(millis, true), Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT), "millis=" + millis);
                assertEquals(utcReference(millis, false), Dates.format(timestamp, Dates.ISO_8601_DATE_TIME_FORMAT, utc), "millis=" + millis);
                assertEquals(utcReference(millis, true), Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT, null), "millis=" + millis);

                // the explicit constants and the default format agree for every value they can both render
                assertEquals(Dates.format(date), Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, null), "millis=" + millis);
                assertEquals(Dates.format(timestamp), Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT, utc), "millis=" + millis);

                final StringBuilder sb = new StringBuilder("prefix:");
                Dates.formatTo(date, Dates.ISO_8601_TIMESTAMP_FORMAT, sb);
                Dates.formatTo(timestamp, Dates.ISO_8601_DATE_TIME_FORMAT, null, sb);
                assertEquals("prefix:" + utcReference(millis, true) + utcReference(millis, false), sb.toString(), "millis=" + millis);
            }
        }

        // G022-01: out-of-range years are still rejected by the predefined format's own year check, before any output
        @Test
        public void testFormat_iso8601ConstantsYearRangeMessageAndNoOutput() {
            for (final String format : new String[] { Dates.ISO_8601_DATE_TIME_FORMAT, Dates.ISO_8601_TIMESTAMP_FORMAT }) {
                final IllegalArgumentException before = assertThrows(IllegalArgumentException.class,
                        () -> Dates.format(new java.util.Date(YEAR_1_START - 1), format));
                assertEquals("Format '" + format + "' supports Common Era years from 0001 through 9999; got instant 0000-12-31T23:59:59.999Z",
                        before.getMessage());

                final IllegalArgumentException after = assertThrows(IllegalArgumentException.class,
                        () -> Dates.format(new java.sql.Timestamp(YEAR_9999_END + 1), format, java.util.TimeZone.getTimeZone("GMT")));
                assertEquals("Format '" + format + "' supports Common Era years from 0001 through 9999; got instant +10000-01-01T00:00:00Z",
                        after.getMessage());

                assertThrows(IllegalArgumentException.class, () -> Dates.format(new java.util.Date(Long.MIN_VALUE), format));
                assertThrows(IllegalArgumentException.class, () -> Dates.format(new java.util.Date(Long.MAX_VALUE), format));

                final StringBuilder sb = new StringBuilder("unchanged");
                assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(new java.util.Date(YEAR_9999_END + 1), format, null, sb));
                assertEquals("unchanged", sb.toString());
            }
        }

        // G022-01: the Appendable still receives the text as one append(String) call; a Writer is not handed a char[] directly
        @Test
        public void testFormatTo_iso8601ConstantsAppendableAndWriterCalls() throws Exception {
            final List<String> calls = new ArrayList<>();
            final Appendable recorder = new Appendable() {
                @Override
                public Appendable append(final CharSequence csq) {
                    calls.add("append:" + csq.getClass().getName() + ":" + csq);
                    return this;
                }

                @Override
                public Appendable append(final CharSequence csq, final int start, final int end) {
                    calls.add("appendRange");
                    return this;
                }

                @Override
                public Appendable append(final char c) {
                    calls.add("appendChar");
                    return this;
                }
            };

            Dates.formatTo(new java.util.Date(1736937045123L), Dates.ISO_8601_DATE_TIME_FORMAT, null, recorder);
            Dates.formatTo(new java.sql.Timestamp(1736937045123L), Dates.ISO_8601_TIMESTAMP_FORMAT, java.util.TimeZone.getTimeZone("GMT"),
                    java.util.Locale.FRENCH, recorder);
            assertEquals(Arrays.asList("append:java.lang.String:2025-01-15T10:30:45Z", "append:java.lang.String:2025-01-15T10:30:45.123Z"), calls);

            final List<String> writerCalls = new ArrayList<>();
            final java.io.Writer writer = new java.io.Writer() {
                @Override
                public void write(final char[] cbuf, final int off, final int len) {
                    writerCalls.add("writeChars:" + new String(cbuf, off, len));
                }

                @Override
                public void write(final String str) throws java.io.IOException {
                    writerCalls.add("writeString:" + str);
                    super.write(str);
                }

                @Override
                public java.io.Writer append(final CharSequence csq) throws java.io.IOException {
                    writerCalls.add("append:" + csq);
                    return super.append(csq);
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };

            Dates.formatTo(new java.util.Date(0L), Dates.ISO_8601_DATE_TIME_FORMAT, writer);
            Dates.formatTo(new java.util.Date(-1L), Dates.ISO_8601_TIMESTAMP_FORMAT, null, writer);
            assertEquals(Arrays.asList("append:1970-01-01T00:00:00Z", "writeString:1970-01-01T00:00:00Z", "writeChars:1970-01-01T00:00:00Z",
                    "append:1969-12-31T23:59:59.999Z", "writeString:1969-12-31T23:59:59.999Z", "writeChars:1969-12-31T23:59:59.999Z"), writerCalls);

            final java.io.IOException failure = new java.io.IOException("append failed");
            final Appendable failing = new Appendable() {
                @Override
                public Appendable append(final CharSequence csq) throws java.io.IOException {
                    throw failure;
                }

                @Override
                public Appendable append(final CharSequence csq, final int start, final int end) throws java.io.IOException {
                    throw failure;
                }

                @Override
                public Appendable append(final char c) throws java.io.IOException {
                    throw failure;
                }
            };

            for (final String format : new String[] { Dates.ISO_8601_DATE_TIME_FORMAT, Dates.ISO_8601_TIMESTAMP_FORMAT }) {
                final RuntimeException e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> Dates.formatTo(new java.util.Date(0L), format, null, failing));
                assertSame(failure, e.getCause());
            }
        }

        // G022-01: zone resolution, conflict check, locale independence and every Date/Calendar entry point stay unchanged
        @Test
        public void testFormat_iso8601ConstantsZonesLocalesAndEntryPoints() throws Exception {
            final long millis = 1736937045123L;
            final java.util.Date date = new java.util.Date(millis);

            for (final java.util.TimeZone zone : new java.util.TimeZone[] { null, java.util.TimeZone.getTimeZone("UTC"), java.util.TimeZone.getTimeZone("GMT"),
                    java.util.TimeZone.getTimeZone("Etc/UTC"), java.util.TimeZone.getTimeZone("Etc/GMT"), new java.util.SimpleTimeZone(0, "Fixed0") }) {
                assertEquals("2025-01-15T10:30:45Z", Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, zone));
                assertEquals("2025-01-15T10:30:45.123Z", Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT, zone));
            }

            for (final java.util.Locale locale : new java.util.Locale[] { java.util.Locale.US, java.util.Locale.FRENCH,
                    java.util.Locale.forLanguageTag("ar-SA"), java.util.Locale.forLanguageTag("th-TH-u-nu-thai"),
                    java.util.Locale.forLanguageTag("hi-IN-u-nu-deva") }) {
                assertEquals("2025-01-15T10:30:45.123Z", Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT, null, locale), locale.toLanguageTag());
                assertEquals("2025-01-15T10:30:45Z", Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, null, locale), locale.toLanguageTag());
            }

            // an equal pattern that is not the constant instance
            assertEquals("2025-01-15T10:30:45Z", Dates.format(date, new String(Dates.ISO_8601_DATE_TIME_FORMAT.toCharArray())));
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(date, new String(Dates.ISO_8601_TIMESTAMP_FORMAT.toCharArray())));

            // a conflicting zone is still rejected first, and nothing reaches the Appendable
            final StringBuilder sb = new StringBuilder();
            final IllegalArgumentException conflict = assertThrows(IllegalArgumentException.class,
                    () -> Dates.formatTo(date, Dates.ISO_8601_TIMESTAMP_FORMAT, java.util.TimeZone.getTimeZone("Asia/Kolkata"), sb));
            assertEquals(
                    "The UTC designator 'Z' requires a UTC-equivalent time zone; format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z', time zone: Asia/Kolkata (raw offset +05:30)",
                    conflict.getMessage());
            assertEquals("", sb.toString());
            assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, null, null));
            assertNull(Dates.format((java.util.Date) null, Dates.ISO_8601_DATE_TIME_FORMAT));

            final java.sql.Timestamp timestamp = new java.sql.Timestamp(millis);
            timestamp.setNanos(123_456_789);
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT));
            assertEquals("2025-01-15T10:30:45Z", Dates.format(timestamp, Dates.ISO_8601_DATE_TIME_FORMAT));
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(new java.sql.Date(millis), Dates.ISO_8601_TIMESTAMP_FORMAT));
            assertEquals("2025-01-15T10:30:45Z", Dates.format(new java.sql.Time(millis), Dates.ISO_8601_DATE_TIME_FORMAT));

            final java.util.GregorianCalendar calendar = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTimeInMillis(millis);
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(calendar, Dates.ISO_8601_TIMESTAMP_FORMAT));
            final StringBuilder calendarText = new StringBuilder();
            Dates.formatTo(calendar, Dates.ISO_8601_DATE_TIME_FORMAT, calendarText);
            assertEquals("2025-01-15T10:30:45Z", calendarText.toString());

            final javax.xml.datatype.XMLGregorianCalendar xml = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(xml, Dates.ISO_8601_TIMESTAMP_FORMAT));
            assertEquals("2025-01-15T10:30:45Z", Dates.format(xml, Dates.ISO_8601_DATE_TIME_FORMAT, java.util.TimeZone.getTimeZone("UTC")));
        }
    }

    // ---- perf review 2026-10-08 G022 end ----
    // ---- perf review 2026-10-08 G023 begin ----
    @Nested
    class G023 {

        private static final long T0 = 1736937045123L; // 2025-01-15T10:30:45.123Z
        private static final long DAY = 86_400_000L;

        /** A Date subclass without a (long) constructor: Dates must build its results by clone(). */
        public static final class NoArgOnlyDate extends java.util.Date {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger constructed = new AtomicInteger();
            String marker;

            public NoArgOnlyDate() {
                super(0L);
                constructed.incrementAndGet();
            }
        }

        /** A Date subclass with a public (long) constructor: Dates must construct every result with it. */
        public static final class LongCtorDate extends java.util.Date {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger constructed = new AtomicInteger();
            String marker;

            public LongCtorDate(final long millis) {
                super(millis);
                constructed.incrementAndGet();
            }
        }

        /** A calendar with only a no-arg constructor: the second strategy of constructOrCloneCalendar. */
        public static final class NoArgOnlyCalendar extends java.util.GregorianCalendar {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger constructed = new AtomicInteger();
            String marker;

            public NoArgOnlyCalendar() {
                super();
                constructed.incrementAndGet();
            }
        }

        /** A calendar with a (long) and a no-arg constructor: the (long) one wins. */
        public static final class LongCtorCalendar extends java.util.GregorianCalendar {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger longConstructed = new AtomicInteger();
            static final AtomicInteger noArgConstructed = new AtomicInteger();
            String marker;

            public LongCtorCalendar() {
                super();
                noArgConstructed.incrementAndGet();
            }

            public LongCtorCalendar(final long millis) {
                super();
                setTimeInMillis(millis);
                longConstructed.incrementAndGet();
            }
        }

        /** A calendar with neither constructor shape: only clone() can build its results. */
        public static final class ZoneCtorOnlyCalendar extends java.util.GregorianCalendar {
            private static final long serialVersionUID = 1L;
            String marker;

            public ZoneCtorOnlyCalendar(final java.util.TimeZone zone) {
                super(zone);
            }
        }

        /** No usable constructor and a clone() that returns the source: must fail the same way on every call. */
        public static final class SelfCloningNoCtorDate extends java.util.Date {
            private static final long serialVersionUID = 1L;

            public SelfCloningNoCtorDate(final String unusedMarker) {
                super(T0);
            }

            @Override
            public Object clone() {
                return this;
            }
        }

        private java.util.TimeZone utc() {
            return java.util.TimeZone.getTimeZone("UTC");
        }

        // G023-01: a Date subclass without a (long) constructor is cloned on every call (the cached
        // "no such constructor" answer must not change the strategy, the type, the time or the source).
        @Test
        public void testCreateDate_noLongConstructorClonesOnEveryCall() {
            final NoArgOnlyDate source = new NoArgOnlyDate();
            source.setTime(T0);
            source.marker = "source-state";
            final int constructedBefore = NoArgOnlyDate.constructed.get();

            for (int i = 1; i <= 3; i++) {
                final NoArgOnlyDate result = Dates.addDays(source, i);
                assertEquals(NoArgOnlyDate.class, result.getClass());
                assertNotSame(source, result);
                assertEquals(T0 + i * DAY, result.getTime());
                assertEquals("source-state", result.marker, "built by clone(), which carries the instance state");
                assertEquals(T0, source.getTime());
            }

            final NoArgOnlyDate rounded = Dates.truncate(source, CalendarField.SECOND);
            assertEquals(T0 - 123L, rounded.getTime());
            assertEquals("source-state", rounded.marker);
            assertEquals(constructedBefore, NoArgOnlyDate.constructed.get(), "no constructor is ever invoked");
        }

        // G023-01: a declared (long) constructor is still found and invoked on every call.
        @Test
        public void testCreateDate_longConstructorInvokedOnEveryCall() {
            final LongCtorDate source = new LongCtorDate(T0);
            source.marker = "source-state";
            final int constructedBefore = LongCtorDate.constructed.get();

            for (int i = 1; i <= 3; i++) {
                final LongCtorDate result = Dates.addHours(source, i);
                assertEquals(LongCtorDate.class, result.getClass());
                assertEquals(T0 + i * 3_600_000L, result.getTime());
                assertNull(result.marker, "built by the (long) constructor, not by clone()");
            }

            assertEquals(constructedBefore + 3, LongCtorDate.constructed.get());
        }

        // G023-01: no usable constructor and a hostile clone() fails with the same exception every time.
        @Test
        public void testCreateDate_hostileCloneFailsIdenticallyOnEveryCall() {
            final SelfCloningNoCtorDate source = new SelfCloningNoCtorDate("x");
            String firstMessage = null;

            for (int i = 0; i < 3; i++) {
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Dates.addDays(source, 1));

                if (firstMessage == null) {
                    firstMessage = e.getMessage();
                } else {
                    assertEquals(firstMessage, e.getMessage());
                }
            }

            assertTrue(firstMessage.contains("must return a distinct Date"), firstMessage);
            assertEquals(T0, source.getTime());
        }

        // G023-01: a calendar with only a no-arg constructor gets it on every call, plus the source settings.
        @Test
        public void testCreateCalendar_noArgConstructorInvokedOnEveryCall() {
            final java.util.TimeZone tokyo = java.util.TimeZone.getTimeZone("Asia/Tokyo");
            final NoArgOnlyCalendar source = new NoArgOnlyCalendar();
            source.setTimeZone(tokyo);
            source.setLenient(false);
            source.setFirstDayOfWeek(java.util.Calendar.WEDNESDAY);
            source.setMinimalDaysInFirstWeek(4);
            source.setTimeInMillis(T0);
            source.marker = "source-state";
            final int constructedBefore = NoArgOnlyCalendar.constructed.get();

            for (int i = 1; i <= 3; i++) {
                final NoArgOnlyCalendar result = Dates.addDays(source, i);
                assertEquals(NoArgOnlyCalendar.class, result.getClass());
                assertNotSame(source, result);
                assertEquals(T0 + i * DAY, result.getTimeInMillis());
                assertNull(result.marker, "built by the no-arg constructor, not by clone()");
                assertEquals("Asia/Tokyo", result.getTimeZone().getID());
                assertFalse(result.isLenient());
                assertEquals(java.util.Calendar.WEDNESDAY, result.getFirstDayOfWeek());
                assertEquals(4, result.getMinimalDaysInFirstWeek());
            }

            assertEquals(constructedBefore + 3, NoArgOnlyCalendar.constructed.get());
            assertEquals(T0, source.getTimeInMillis());
        }

        // G023-01: a declared (long) constructor keeps precedence over the no-arg one on every call.
        @Test
        public void testCreateCalendar_longConstructorPreferredOnEveryCall() {
            final LongCtorCalendar source = new LongCtorCalendar(T0);
            source.setTimeZone(utc());
            source.marker = "source-state";
            final int longBefore = LongCtorCalendar.longConstructed.get();
            final int noArgBefore = LongCtorCalendar.noArgConstructed.get();

            for (int i = 1; i <= 3; i++) {
                final LongCtorCalendar result = Dates.addMinutes(source, i);
                assertEquals(LongCtorCalendar.class, result.getClass());
                assertEquals(T0 + i * 60_000L, result.getTimeInMillis());
                assertNull(result.marker);
            }

            assertEquals(longBefore + 3, LongCtorCalendar.longConstructed.get());
            assertEquals(noArgBefore, LongCtorCalendar.noArgConstructed.get());
        }

        // G023-01: a calendar with neither constructor shape is cloned on every call.
        @Test
        public void testCreateCalendar_noUsableConstructorClonesOnEveryCall() {
            final ZoneCtorOnlyCalendar source = new ZoneCtorOnlyCalendar(utc());
            source.setTimeInMillis(T0);
            source.marker = "source-state";

            for (int i = 1; i <= 3; i++) {
                final ZoneCtorOnlyCalendar result = Dates.addDays(source, i);
                assertEquals(ZoneCtorOnlyCalendar.class, result.getClass());
                assertNotSame(source, result);
                assertEquals(T0 + i * DAY, result.getTimeInMillis());
                assertEquals("source-state", result.marker, "built by clone(), which carries the instance state");
            }

            final ZoneCtorOnlyCalendar truncated = Dates.truncate(source, java.util.Calendar.DATE);
            assertEquals(T0 - (T0 % DAY), truncated.getTimeInMillis());
            assertEquals(T0, source.getTimeInMillis());
        }

        // G023-01: the JDK's own alternate calendars (no (long) constructor; Japanese has no no-arg one, the
        // Buddhist one is not exported) keep their runtime class, settings and results on repeated calls.
        @Test
        public void testCreateCalendar_jdkAlternateCalendarsOnRepeatedCalls() {
            for (final String tag : new String[] { "ja-JP-u-ca-japanese", "th-TH" }) {
                final java.util.Calendar source = java.util.Calendar.getInstance(utc(), java.util.Locale.forLanguageTag(tag));
                source.setFirstDayOfWeek(java.util.Calendar.FRIDAY);
                source.setTimeInMillis(T0);
                final Class<?> type = source.getClass();
                assertNotSame(java.util.GregorianCalendar.class, type, tag);

                for (int i = 1; i <= 3; i++) {
                    final java.util.Calendar result = Dates.addDays(source, i);
                    assertEquals(type, result.getClass(), tag);
                    assertNotSame(source, result, tag);
                    assertEquals(T0 + i * DAY, result.getTimeInMillis(), tag);
                    assertEquals("UTC", result.getTimeZone().getID(), tag);
                    assertEquals(java.util.Calendar.FRIDAY, result.getFirstDayOfWeek(), tag);
                    assertEquals(source.get(java.util.Calendar.ERA), result.get(java.util.Calendar.ERA), tag);
                }

                assertEquals(T0 - (T0 % DAY), Dates.truncate(source, java.util.Calendar.DATE).getTimeInMillis(), tag);
                assertEquals(T0, source.getTimeInMillis(), tag);
            }
        }

        private String failureOf(final org.junit.jupiter.api.function.Executable call) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertNotNull(e.getCause());
            return e.getMessage() + " | " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
        }

        /** Every DTF parser, instant-producing or not, with an optional fallback zone. */
        private List<org.junit.jupiter.api.function.Executable> allParsers(final Dates.DTF dtf, final String text, final java.util.TimeZone tz) {
            final List<org.junit.jupiter.api.function.Executable> calls = new ArrayList<>();
            calls.add(() -> dtf.parseToOffsetDateTime(text, tz));
            calls.add(() -> dtf.parseToZonedDateTime(text, tz));
            calls.add(() -> dtf.parseToInstant(text, tz));
            calls.add(() -> dtf.parseToJUDate(text, tz));
            calls.add(() -> dtf.parseToDate(text, tz));
            calls.add(() -> dtf.parseToTime(text, tz));
            calls.add(() -> dtf.parseToTimestamp(text, tz));
            calls.add(() -> dtf.parseToCalendar(text, tz));

            if (tz == null) {
                calls.add(() -> dtf.parseToOffsetDateTime(text));
                calls.add(() -> dtf.parseToZonedDateTime(text));
                calls.add(() -> dtf.parseToInstant(text));
                calls.add(() -> dtf.parseToJUDate(text));
                calls.add(() -> dtf.parseToDate(text));
                calls.add(() -> dtf.parseToTime(text));
                calls.add(() -> dtf.parseToTimestamp(text));
                calls.add(() -> dtf.parseToCalendar(text));
                calls.add(() -> dtf.parseToLocalDate(text));
                calls.add(() -> dtf.parseToLocalTime(text));
                calls.add(() -> dtf.parseToLocalDateTime(text));
                calls.add(() -> dtf.parseToTemporalAccessor(text));
            }

            return calls;
        }

        private java.util.TimeZone[] fallbackZones() {
            // the last one has rules no ZoneId can express: a zone written in the text must not consult it
            return new java.util.TimeZone[] { null, utc(), java.util.TimeZone.getTimeZone("Asia/Tokyo"),
                    new java.util.SimpleTimeZone(3_600_000, "CustomDst", java.util.Calendar.MARCH, 8, -java.util.Calendar.SUNDAY, 7_200_000,
                            java.util.Calendar.NOVEMBER, 1, -java.util.Calendar.SUNDAY, 7_200_000, 3_600_000) };
        }

        // G023-02: an offset the written region rejects fails every parser with the same message and cause,
        // whether parseRaw or the zoned resolution of an instant-producing parser reports it.
        @Test
        public void testDtfParse_datedOffsetInvalidForRegionSameFailureEverywhere() {
            final String text = "2025-07-15T10:30:45+01:00[Europe/Paris]";
            final String expected = "Cannot parse \"" + text + "\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'': "
                    + "Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45 in zone Europe/Paris; valid offsets are [+02:00]"
                    + " | java.time.DateTimeException: Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45 in zone Europe/Paris;"
                    + " valid offsets are [+02:00]";

            for (final java.util.TimeZone tz : fallbackZones()) {
                for (final org.junit.jupiter.api.function.Executable call : allParsers(Dates.DTF.ISO_ZONED_DATE_TIME, text, tz)) {
                    assertEquals(expected, failureOf(call));
                }
            }

            final String gapText = "2025-03-09T02:30:00-05:00[America/New_York]";
            final String gap = "Cannot parse \"" + gapText + "\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'': "
                    + "Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)"
                    + " | java.time.DateTimeException: Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)";

            for (final org.junit.jupiter.api.function.Executable call : allParsers(Dates.DTF.ISO_ZONED_DATE_TIME, gapText, null)) {
                assertEquals(gap, failureOf(call));
            }

            final String fixedText = "2025-01-15T10:30:45+05:00[+03:00]";
            final String fixed = "Cannot parse \"" + fixedText + "\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'': "
                    + "Offset +05:00 is not valid for local date-time 2025-01-15T10:30:45 in zone +03:00; valid offsets are [+03:00]"
                    + " | java.time.DateTimeException: Offset +05:00 is not valid for local date-time 2025-01-15T10:30:45 in zone +03:00;"
                    + " valid offsets are [+03:00]";

            for (final org.junit.jupiter.api.function.Executable call : allParsers(Dates.DTF.ISO_ZONED_DATE_TIME, fixedText, null)) {
                assertEquals(fixed, failureOf(call));
            }

            // the static entry points that delegate to the DTF zoned parsers report the same diagnostic
            assertTrue(failureOf(() -> Dates.parseToInstant(text)).contains("Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45"));
            assertTrue(failureOf(() -> Dates.parseToZonedDateTime(text, Dates.ISO_ZONED_DATE_TIME_FORMAT))
                    .contains("Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45"));
        }

        // G023-02: time-only text keeps its offset validation (on 1970-01-01) ahead of the missing-date
        // rejection of the instant-producing parsers; parseToTime anchors a valid one.
        @Test
        public void testDtfParse_timeOnlyOffsetValidatedFirst() {
            final Dates.DTF dtf = Dates.DTF.of("HH:mm:ssXXX'['VV']'");
            final String invalid = "10:30:45-04:00[America/New_York]";
            final String expected = "Cannot parse \"" + invalid + "\" with pattern 'HH:mm:ssXXX'['VV']'': "
                    + "Offset -04:00 is not valid for local date-time 1970-01-01T10:30:45 in zone America/New_York; valid offsets are [-05:00]"
                    + " | java.time.DateTimeException: Offset -04:00 is not valid for local date-time 1970-01-01T10:30:45 in zone America/New_York;"
                    + " valid offsets are [-05:00]";

            for (final java.util.TimeZone tz : fallbackZones()) {
                for (final org.junit.jupiter.api.function.Executable call : allParsers(dtf, invalid, tz)) {
                    assertEquals(expected, failureOf(call));
                }
            }

            final String valid = "10:30:45-05:00[America/New_York]";
            assertEquals(15L * 3_600_000L + 30L * 60_000L + 45_000L, dtf.parseToTime(valid).getTime());
            assertEquals(15L * 3_600_000L + 30L * 60_000L + 45_000L, dtf.parseToTime(valid, utc()).getTime());
            assertTrue(failureOf(() -> dtf.parseToInstant(valid)).contains("No date fields in \"" + valid + "\""));
            assertTrue(failureOf(() -> dtf.parseToCalendar(valid, utc())).contains("No date fields in \"" + valid + "\""));
        }

        // G023-02: valid zoned and offset text resolves to the same instants, zones and calendars as before.
        @Test
        public void testDtfParse_validZonedAndOffsetTextResolves() {
            final String overlapStandard = "2025-11-02T01:30:00-05:00[America/New_York]";
            final String overlapDaylight = "2025-11-02T01:30:00-04:00[America/New_York]";
            final String summer = "2025-07-15T10:30:45+02:00[Europe/Paris]";

            for (final java.util.TimeZone tz : fallbackZones()) {
                final Dates.DTF dtf = Dates.DTF.ISO_ZONED_DATE_TIME;
                assertEquals(1762065000000L, dtf.parseToInstant(overlapStandard, tz).toEpochMilli());
                assertEquals(1762061400000L, dtf.parseToInstant(overlapDaylight, tz).toEpochMilli());
                assertEquals(java.time.ZonedDateTime.parse(summer), dtf.parseToZonedDateTime(summer, tz));
                assertEquals(java.time.OffsetDateTime.parse("2025-11-02T01:30-05:00"), dtf.parseToOffsetDateTime(overlapStandard, tz));
                assertEquals(1762065000000L, dtf.parseToJUDate(overlapStandard, tz).getTime());
                assertEquals(1762065000000L, dtf.parseToDate(overlapStandard, tz).getTime());
                assertEquals(1762065000000L, dtf.parseToTime(overlapStandard, tz).getTime());
                assertEquals(1762065000000L, dtf.parseToTimestamp(overlapStandard, tz).getTime());

                final java.util.Calendar calendar = dtf.parseToCalendar(summer, tz);
                assertEquals(java.time.Instant.parse("2025-07-15T08:30:45Z").toEpochMilli(), calendar.getTimeInMillis());
                assertEquals("Europe/Paris", calendar.getTimeZone().getID());

                final Dates.DTF offset = Dates.DTF.ISO_OFFSET_DATE_TIME;
                assertEquals(java.time.Instant.parse("2025-01-15T05:00:45Z"), offset.parseToInstant("2025-01-15T10:30:45+05:30", tz));
                assertEquals(java.time.ZonedDateTime.parse("2025-01-15T10:30:45+05:30"), offset.parseToZonedDateTime("2025-01-15T10:30:45+05:30", tz));
                assertEquals(java.time.Instant.parse("2025-01-15T10:13:15Z"), offset.parseToInstant("2025-01-15T10:30:45+00:17:30", tz));
            }

            assertEquals(java.time.ZonedDateTime.parse(summer), Dates.parseToZonedDateTime(summer));
            assertEquals(java.time.Instant.parse("2025-07-15T08:30:45Z"), Dates.parseToInstant(summer));
        }
    }

    // ---- perf review 2026-10-08 G023 end ----
    // ---- perf review 2026-10-08 G024 begin ----
    @Nested
    class G024 {

        public static class Person {
            private long id;
            private String name;
            private Integer age;
            private String email;
            private String note;

            public Person() {
            }

            Person(final long id, final String name, final Integer age, final String email, final String note) {
                this.id = id;
                this.name = name;
                this.age = age;
                this.email = email;
                this.note = note;
            }

            public long getId() {
                return id;
            }

            public void setId(final long id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(final String name) {
                this.name = name;
            }

            public Integer getAge() {
                return age;
            }

            public void setAge(final Integer age) {
                this.age = age;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(final String email) {
                this.email = email;
            }

            public String getNote() {
                return note;
            }

            public void setNote(final String note) {
                this.note = note;
            }
        }

        public static class ExactAfterFuzzy {
            public String id = "1";
            public String userName = "fuzzy";
            public String username = "exact";
        }

        public static class IdAndLowerName {
            public String id = "1";
            public String username = "exact";
            public String extra = "right-only";
            @com.landawn.abacus.annotation.DiffIgnore
            public String secret = "ignored";
        }

        public static class LowerName {
            public String username;

            public LowerName() {
            }

            LowerName(final String username) {
                this.username = username;
            }
        }

        public static class ActiveIgnoredActive {
            public String userName = "same";
            @com.landawn.abacus.annotation.DiffIgnore
            public String username = "ignored";
            public String user_name = "same";
        }

        public static class IgnoredThenActive {
            @com.landawn.abacus.annotation.DiffIgnore
            public String userName = "ignored";
            public String user_name = "same";
        }

        public static class ActiveThenIgnored {
            public String user_name = "same";
            @com.landawn.abacus.annotation.DiffIgnore
            public String userName = "ignored";
        }

        // G024-01/G024-02: same-class beans (no fuzzy match): results, null/null handling and predicate calls unchanged.
        @Test
        public void testBeanDifferenceOf_sameClassBeans() {
            final Person left = new Person(1, "John", 30, null, "n1");
            final Person right = new Person(1, "John", 31, null, "n2");

            final var diff = Difference.BeanDifference.of(left, right);

            assertEquals(Arrays.asList("id", "name"), new ArrayList<>(diff.common().keySet()));
            assertEquals(1L, diff.common().get("id"));
            assertEquals("John", diff.common().get("name"));
            assertEquals(Arrays.asList("age", "note"), new ArrayList<>(diff.differentValues().keySet()));
            assertEquals(Pair.of(30, 31), diff.differentValues().get("age"));
            assertEquals(Pair.of("n1", "n2"), diff.differentValues().get("note"));
            assertTrue(diff.onlyOnLeft().isEmpty());
            assertTrue(diff.onlyOnRight().isEmpty());
            assertFalse(diff.areEqual());
            assertEquals(diff, Difference.BeanDifference.of(left, right));

            final List<String> calls = new ArrayList<>();
            final var byProps = Difference.BeanDifference.ofByProps(left, right, (name, v1, v2) -> {
                calls.add(name + "=" + v1 + "/" + v2);
                return !"note".equals(name);
            });

            assertEquals(Arrays.asList("id=1/1", "name=John/John", "age=30/31", "email=null/null", "note=n1/n2"), calls);
            assertEquals(Arrays.asList("id", "name", "age"), new ArrayList<>(byProps.common().keySet()));
            assertEquals(Collections.singletonList("note"), new ArrayList<>(byProps.differentValues().keySet()));
            assertTrue(byProps.onlyOnLeft().isEmpty());
            assertTrue(byProps.onlyOnRight().isEmpty());

            final Person same = new Person(7, null, null, null, null);
            final var equal = Difference.BeanDifference.of(same, new Person(7, null, null, null, null));
            assertTrue(equal.areEqual());
            assertEquals(Collections.singletonMap("id", 7L), equal.common());
        }

        // G024-01: the lazily built exact-name set still reserves an exact name declared after the first fuzzy match.
        @Test
        public void testBeanDifferenceOf_exactNameDeclaredAfterFuzzyMatch() {
            final var diff = Difference.BeanDifference.of(new ExactAfterFuzzy(), new IdAndLowerName());

            assertEquals(Arrays.asList("id", "username"), new ArrayList<>(diff.common().keySet()));
            assertEquals("exact", diff.common().get("username"));
            assertEquals(Collections.singletonMap("userName", "fuzzy"), diff.onlyOnLeft());
            assertEquals(Collections.singletonMap("extra", "right-only"), diff.onlyOnRight());
            assertTrue(diff.differentValues().isEmpty());

            final var reverse = Difference.BeanDifference.of(new IdAndLowerName(), new ExactAfterFuzzy());
            assertEquals(Arrays.asList("id", "username"), new ArrayList<>(reverse.common().keySet()));
            assertEquals(Collections.singletonMap("extra", "right-only"), reverse.onlyOnLeft());
            assertEquals(Collections.singletonMap("userName", "fuzzy"), reverse.onlyOnRight());
        }

        // G024-02: an ignored alias between two active fuzzy matches of one right property still triggers the IAE.
        @Test
        public void testBeanDifferenceOf_activeIgnoredActiveAliasesRejected() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> Difference.BeanDifference.of(new ActiveIgnoredActive(), new LowerName("same")));
            assertEquals("Multiple properties of " + ActiveIgnoredActive.class.getName() + " resolve to property 'username' of " + LowerName.class.getName(),
                    e.getMessage());

            assertThrows(IllegalArgumentException.class,
                    () -> Difference.BeanDifference.ofByProps(new ActiveIgnoredActive(), new LowerName(null), (name, v1, v2) -> true));
        }

        // G024-02: an ignored alias before or after the single active match leaves that match intact.
        @Test
        public void testBeanDifferenceOf_ignoredAliasAroundActiveMatch() {
            final var ignoredFirst = Difference.BeanDifference.of(new IgnoredThenActive(), new LowerName("same"));
            assertTrue(ignoredFirst.areEqual());
            assertEquals(Collections.singletonMap("user_name", "same"), ignoredFirst.common());

            final var ignoredLast = Difference.BeanDifference.of(new ActiveThenIgnored(), new LowerName("other"));
            assertFalse(ignoredLast.areEqual());
            assertTrue(ignoredLast.common().isEmpty());
            assertEquals(Collections.singletonMap("user_name", Pair.of("same", "other")), ignoredLast.differentValues());
            assertTrue(ignoredLast.onlyOnLeft().isEmpty());
            assertTrue(ignoredLast.onlyOnRight().isEmpty());
        }

        // G024-01/G024-02: the collection overload runs the all-properties path once per matched pair.
        @Test
        public void testBeanDifferenceOfCollections_allProperties() {
            final List<Person> left = Arrays.asList(new Person(1, "a", 1, null, "x"), new Person(2, "b", 2, "e", "x"), new Person(3, "c", 3, null, "x"));
            final List<Person> right = Arrays.asList(new Person(4, "d", 4, null, "x"), new Person(2, "b", 2, "e", "x"), new Person(1, "a", 9, null, "x"));

            final var diff = Difference.BeanDifference.of(left, right, Person::getId);

            assertEquals(1, diff.common().size());
            assertSame(left.get(1), diff.common().get(0));
            assertEquals(1, diff.onlyOnLeft().size());
            assertSame(left.get(2), diff.onlyOnLeft().get(0));
            assertEquals(1, diff.onlyOnRight().size());
            assertSame(right.get(0), diff.onlyOnRight().get(0));
            assertEquals(Collections.singletonList(1L), new ArrayList<>(diff.differentValues().keySet()));
            assertEquals(Difference.BeanDifference.of(left.get(0), right.get(2)), diff.differentValues().get(1L));
            assertEquals(Collections.singletonMap("age", Pair.of(1, 9)), diff.differentValues().get(1L).differentValues());
        }

        private List<Map<String, Object>> rows(final Object... idAndValues) {
            final List<Map<String, Object>> rows = new ArrayList<>();

            for (int i = 0; i < idAndValues.length; i += 4) {
                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", idAndValues[i]);
                row.put("a", idAndValues[i + 1]);
                row.put("b", idAndValues[i + 2]);
                row.put("c", idAndValues[i + 3]);
                rows.add(row);
            }

            return rows;
        }

        // G024-03: a List selection (copied once) compares every matched pair exactly like MapDifference.of(Map, Map, Collection).
        @Test
        public void testMapDifferenceOfCollections_listSelection() {
            final List<Map<String, Object>> left = rows(1, "a1", "b1", "c1", 2, "a2", "b2", "c2", 3, "a3", "b3", "c3", 5, null, "b5", "c5");
            final List<Map<String, Object>> right = rows(3, "a3", "B3", "c3", 1, "a1", "b1", "C1", 4, "a4", "b4", "c4", 2, "A2", "b2", "c2", 5, null, "b5",
                    "C5");
            final List<String> selection = new ArrayList<>(Arrays.asList("a", "b", "a", "zzz", null));
            final List<String> selectionCopy = new ArrayList<>(selection);

            final var diff = Difference.MapDifference.of(left, right, selection, m -> m.get("id"));

            assertEquals(selectionCopy, selection);
            assertEquals(2, diff.common().size());
            assertSame(left.get(0), diff.common().get(0));
            assertSame(left.get(3), diff.common().get(1));
            assertTrue(diff.onlyOnLeft().isEmpty());
            assertEquals(1, diff.onlyOnRight().size());
            assertSame(right.get(2), diff.onlyOnRight().get(0));
            assertEquals(Arrays.asList(2, 3), new ArrayList<>(diff.differentValues().keySet()));
            assertEquals(Difference.MapDifference.of(left.get(1), right.get(3), selection), diff.differentValues().get(2));
            assertEquals(Difference.MapDifference.of(left.get(2), right.get(0), selection), diff.differentValues().get(3));
            assertEquals(Collections.singletonMap("a", Pair.of("a2", "A2")), diff.differentValues().get(2).differentValues());
            assertEquals(Collections.singletonMap("b", Pair.of("b3", "B3")), diff.differentValues().get(3).differentValues());

            final var twoExtractors = Difference.MapDifference.of(left, right, selection, m -> m.get("id"), m -> m.get("id"));
            assertEquals(diff, twoExtractors);

            // null and empty selections still compare every key
            final var all = Difference.MapDifference.of(left, right, m -> m.get("id"));
            assertEquals(all, Difference.MapDifference.of(left, right, (Collection<String>) null, m -> m.get("id")));
            assertEquals(all, Difference.MapDifference.of(left, right, new ArrayList<String>(), m -> m.get("id")));
            assertEquals(Arrays.asList(1, 2, 3, 5), new ArrayList<>(all.differentValues().keySet()));
            assertTrue(all.common().isEmpty());
        }

        // G024-03: a Set selection is used as given for every pair; no selection is read when no pair matches.
        @Test
        public void testMapDifferenceOfCollections_setSelectionAndNoMatches() {
            final List<Map<String, Object>> left = rows(1, "x", "y", "z", 2, "x", "y", "z");
            final List<Map<String, Object>> right = rows(1, "X", "y", "z", 2, "x", "y", "Z");
            final Set<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitive.add("A");

            final var viaSet = Difference.MapDifference.of(left, right, caseInsensitive, m -> m.get("id"));
            assertEquals(Collections.singletonList(1), new ArrayList<>(viaSet.differentValues().keySet()));
            assertEquals(Collections.singletonMap("a", Pair.of("x", "X")), viaSet.differentValues().get(1).differentValues());
            assertEquals(1, viaSet.common().size());
            assertSame(left.get(1), viaSet.common().get(0));

            final var viaList = Difference.MapDifference.of(left, right, Arrays.asList("A"), m -> m.get("id"));
            assertTrue(viaList.differentValues().isEmpty());
            assertEquals(2, viaList.common().size());

            final AtomicInteger reads = new AtomicInteger();
            final List<String> keys = Arrays.asList("a", "b");
            final Collection<String> countingSelection = new java.util.AbstractCollection<>() {
                @Override
                public Iterator<String> iterator() {
                    reads.incrementAndGet();
                    return keys.iterator();
                }

                @Override
                public int size() {
                    reads.incrementAndGet();
                    return keys.size();
                }
            };

            final var noMatches = Difference.MapDifference.of(left, rows(8, "x", "y", "z", 9, "x", "y", "z"), countingSelection, m -> m.get("id"));
            assertEquals(0, reads.get());
            assertEquals(2, noMatches.onlyOnLeft().size());
            assertEquals(2, noMatches.onlyOnRight().size());

            final var matched = Difference.MapDifference.of(left, right, countingSelection, m -> m.get("id"));
            assertTrue(reads.get() > 0);
            assertEquals(Difference.MapDifference.of(left, right, keys, m -> m.get("id")), matched);
        }
    }

    // ---- perf review 2026-10-08 G024 end ----
    // ---- perf review 2026-10-08 G025 begin ----
    @Nested
    class G025 {
        // quiet NaN bit patterns with different signs and payloads; Double.equals treats them all as one value
        private final long[] quietNaNBits = { 0x7ff8000000000000L, 0x7ff8000000000001L, 0xfff8000000000000L, 0x7fffffffffffffffL, 0xfff80000000abcdeL };

        /**
         * Reference for {@code DoubleList.removeDuplicates()}: the raw bits of the first occurrence of each value under
         * {@code Double.equals} (that is, {@code Double.doubleToLongBits}), in encounter order.
         */
        private long[] firstOccurrenceBits(final double[] a, final int size) {
            final Set<Long> seen = new HashSet<>();
            final List<Long> kept = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                if (seen.add(Double.doubleToLongBits(a[i]))) {
                    kept.add(Double.doubleToRawLongBits(a[i]));
                }
            }

            final long[] result = new long[kept.size()];

            for (int i = 0; i < result.length; i++) {
                result[i] = kept.get(i);
            }

            return result;
        }

        @SuppressWarnings("deprecation")
        private void assertRemoveDuplicatesMatchesReference(final double[] a, final int size, final String label) {
            final double[] backing = a.clone();
            final long[] expected = firstOccurrenceBits(backing, size);
            final double[] spare = Arrays.copyOfRange(backing, size, backing.length);
            final DoubleList list = DoubleList.of(backing, size);

            assertEquals(expected.length != size, list.removeDuplicates(), label);
            assertEquals(expected.length, list.size(), label);
            assertSame(backing, list.internalArray(), label);

            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], Double.doubleToRawLongBits(backing[i]), label + " index " + i);
            }

            for (int i = expected.length; i < size; i++) {
                assertEquals(0L, Double.doubleToRawLongBits(backing[i]), label + " cleared slot " + i);
            }

            for (int i = size; i < backing.length; i++) {
                assertEquals(Double.doubleToRawLongBits(spare[i - size]), Double.doubleToRawLongBits(backing[i]), label + " spare slot " + i);
            }
        }

        private double randomValue(final Random rnd, final int domain, final int size, final double[] specials) {
            switch (domain) {
                case 0: // few distinct values: NaN payload variants, signed zeros, infinities, extremes
                    return rnd.nextInt(4) == 0 ? Double.longBitsToDouble(quietNaNBits[rnd.nextInt(quietNaNBits.length)])
                            : specials[rnd.nextInt(specials.length)];
                case 1: // whole numbers with duplicates
                    return rnd.nextInt(Math.max(1, size / 2));
                case 2: // mostly distinct values
                    return rnd.nextDouble();
                default: { // random bit patterns; NaN patterns are replaced by quiet NaN payload variants
                    final double d = Double.longBitsToDouble(rnd.nextLong());
                    return Double.isNaN(d) ? Double.longBitsToDouble(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : d;
                }
            }
        }

        // G025-02: removeDuplicates keeps the first occurrence of each value bit for bit, the backing array, zeroed cleared slots and untouched spare slots
        @Test
        public void testRemoveDuplicates_matchesFirstOccurrenceReference() {
            final Random rnd = new Random(20261008L);
            final double[] specials = { 0.0d, -0.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MAX_VALUE,
                    -Double.MAX_VALUE, 1d, -1d };
            final int[] sizes = { 2, 3, 4, 5, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 127, 128, 129, 257, 1000, 3000 };

            for (final int size : sizes) {
                for (int domain = 0; domain < 4; domain++) {
                    for (int trial = 0; trial < 5; trial++) {
                        final double[] a = new double[size + rnd.nextInt(3)];

                        for (int i = 0; i < a.length; i++) {
                            a[i] = randomValue(rnd, domain, size, specials);
                        }

                        assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " domain=" + domain + " trial=" + trial);
                    }
                }
            }
        }

        // G025-02: keys that all share one home slot of N's primitive key set (an N.distinct-based removeDuplicates would hit its boxed fallback) keep the reference result
        @Test
        public void testRemoveDuplicates_collidingKeysMatchReference() {
            final long hashMultiplier = 0x9E3779B97F4A7C15L; // multiplier of the hash used by the primitive key set behind N.distinct
            long inverse = hashMultiplier;

            for (int i = 0; i < 6; i++) {
                inverse *= 2 - hashMultiplier * inverse;
            }

            final Random rnd = new Random(8L);

            for (final int distinct : new int[] { 70, 200, 5000 }) {
                final double[] a = new double[distinct * 2];

                for (int i = 0; i < a.length; i++) {
                    // the products with the multiplier are consecutive, so all keys start probing at the same slot
                    final double d = Double.longBitsToDouble((0x123456789L + rnd.nextInt(distinct)) * inverse);
                    a[i] = Double.isNaN(d) ? Double.longBitsToDouble(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : d;
                }

                assertRemoveDuplicatesMatchesReference(a, a.length, "colliding distinct=" + distinct);
            }
        }

        // G025-02: in-place semantics on a shared array: kept order, cleared slots, untouched spare slots, unchanged when unique, first NaN payload kept
        @Test
        @SuppressWarnings("deprecation")
        public void testRemoveDuplicates_sharedBackingArrayExamples() {
            final double[] shared = { 3d, 1d, 3d, 2d, 1d, 9d, 9d };
            final DoubleList list = DoubleList.of(shared, 5);

            assertTrue(list.removeDuplicates());
            assertSame(shared, list.internalArray());
            assertEquals(3, list.size());
            assertArrayEquals(new double[] { 3d, 1d, 2d, 0d, 0d, 9d, 9d }, shared);

            final double[] unique = { 3d, 1d, 2d, -0.0d, 0.0d, Double.NaN };
            final DoubleList uniqueList = DoubleList.of(unique);

            assertFalse(uniqueList.removeDuplicates());
            assertSame(unique, uniqueList.internalArray());
            assertEquals(6, uniqueList.size());
            assertArrayEquals(new double[] { 3d, 1d, 2d, -0.0d, 0.0d, Double.NaN }, unique);

            final double firstNaN = Double.longBitsToDouble(0x7ff8000000000001L);
            final double[] nans = { 2d, firstNaN, 1d, Double.NaN, 2d };
            final DoubleList nanList = DoubleList.of(nans);

            assertTrue(nanList.removeDuplicates());
            assertEquals(3, nanList.size());
            assertEquals(0x7ff8000000000001L, Double.doubleToRawLongBits(nans[1]));
            assertArrayEquals(new double[] { 2d, firstNaN, 1d, 0d, 0d }, nans);
        }
    }

    // ---- perf review 2026-10-08 G025 end ----
    // ---- perf review 2026-10-08 G026 begin ----
    @Nested
    class G026 {

        private static final char BACKSLASH = (char) 92;

        private static final String NUL_MESSAGE = "Null byte present in file/path name. There are no "
                + "known legitimate use cases for such data, but several injection attacks may use it";

        /**
         * Builds a String from UTF-16 code units (keeps this source ASCII-only).
         */
        private String chars(final int... codeUnits) {
            final StringBuilder sb = new StringBuilder(codeUnits.length);

            for (final int codeUnit : codeUnits) {
                sb.append((char) codeUnit);
            }

            return sb.toString();
        }

        private String allChars() {
            final StringBuilder sb = new StringBuilder(Character.MAX_VALUE + 1);

            for (int c = 0; c <= Character.MAX_VALUE; c++) {
                sb.append((char) c);
            }

            return sb.toString();
        }

        /**
         * The inline control-char table of EscapeUtil.ESCAPE_XML10.
         */
        private String[][] xml10ControlTable() {
            final List<String[]> rows = new ArrayList<>();

            for (int c = 0; c <= 0x1f; c++) {
                if (c != 0x9 && c != 0xa && c != 0xd) {
                    rows.add(new String[] { chars(c), "" });
                }
            }

            rows.add(new String[] { chars(0xfffe), "" });
            rows.add(new String[] { chars(0xffff), "" });

            return rows.toArray(new String[0][]);
        }

        /**
         * The inline control-char table of EscapeUtil.ESCAPE_XML11.
         */
        private String[][] xml11ControlTable() {
            return new String[][] { { chars(0), "" }, { chars(0xb), "&#11;" }, { chars(0xc), "&#12;" }, { chars(0xfffe), "" }, { chars(0xffff), "" } };
        }

        private String[][] javaQuoteTable() {
            return new String[][] { { "\"", BACKSLASH + "\"" }, { String.valueOf(BACKSLASH), BACKSLASH + "" + BACKSLASH } };
        }

        private String[][] ecmaQuoteTable() {
            return new String[][] { { "'", BACKSLASH + "'" }, { "\"", BACKSLASH + "\"" }, { String.valueOf(BACKSLASH), BACKSLASH + "" + BACKSLASH },
                    { "/", BACKSLASH + "/" } };
        }

        private String[][] jsonQuoteTable() {
            return new String[][] { { "\"", BACKSLASH + "\"" }, { String.valueOf(BACKSLASH), BACKSLASH + "" + BACKSLASH }, { "/", BACKSLASH + "/" } };
        }

        /**
         * The plain greedy longest-match lookup, without the prefix/suffix pre-filters and without the one-char fast path.
         */
        private final class ReferenceLookup extends EscapeUtil.CharSequenceTranslator {
            private final Map<String, String> map = new HashMap<>();
            private int shortest = Integer.MAX_VALUE;
            private int longest = 0;

            ReferenceLookup(final String[][] table) {
                for (final String[] row : table) {
                    map.put(row[0], row[1]);
                    shortest = Math.min(shortest, row[0].length());
                    longest = Math.max(longest, row[0].length());
                }
            }

            @Override
            public int translate(final CharSequence input, final int index, final java.io.Writer out) throws java.io.IOException {
                for (int i = Math.min(longest, input.length() - index); i >= shortest; i--) {
                    final String result = map.get(input.subSequence(index, index + i).toString());

                    if (result != null) {
                        out.write(result);
                        return Character.codePointCount(input, index, index + i);
                    }
                }

                return 0;
            }
        }

        private String randomText(final Random random, final char[] alphabet, final int maxLength) {
            final char[] text = new char[random.nextInt(maxLength + 1)];

            for (int i = 0; i < text.length; i++) {
                text[i] = alphabet[random.nextInt(alphabet.length)];
            }

            return new String(text);
        }

        private void assertSameTranslation(final EscapeUtil.CharSequenceTranslator expected, final EscapeUtil.CharSequenceTranslator actual,
                final char[] alphabet, final long seed) throws java.io.IOException {
            for (int c = 0; c <= Character.MAX_VALUE; c++) {
                final String one = chars(c);
                final java.io.StringWriter expectedOut = new java.io.StringWriter();
                final java.io.StringWriter actualOut = new java.io.StringWriter();

                assertEquals(expected.translate(one, 0, expectedOut), actual.translate(one, 0, actualOut), one);
                assertEquals(expectedOut.toString(), actualOut.toString(), one);
            }

            final String all = allChars();
            assertEquals(expected.translate(all), actual.translate(all));
            assertEquals(expected.translate(new StringBuilder(all)), actual.translate(new StringBuilder(all)));

            final Random random = new Random(seed);

            for (int trial = 0; trial < 2000; trial++) {
                final String text = randomText(random, alphabet, 24);
                assertEquals(expected.translate(text), actual.translate(text), text);
            }
        }

        private char[] alphabetOf(final String[][]... tables) {
            final StringBuilder sb = new StringBuilder("az 9.&;#");
            sb.append(chars(0xe9, 0x4e2d, 0xd83d, 0xde00, 0xd800, 0xdfff, 0x7f, 0x85, 0x0, 0x1f, 0x2022));

            for (final String[][] table : tables) {
                for (final String[] row : table) {
                    sb.append(row[0]);
                }
            }

            return sb.toString().toCharArray();
        }

        // G026-01: one-char-key LookupTranslator tables now find a match by binary search; every char must translate as the plain map lookup does
        @Test
        public void testLookupTranslator_singleCharTablesMatchReference() throws java.io.IOException {
            final String[][][] tables = { EscapeUtil.EntityArrays.basicEscape(), EscapeUtil.EntityArrays.aposEscape(),
                    EscapeUtil.EntityArrays.iso8859_1Escape(), EscapeUtil.EntityArrays.html40ExtendedEscape(), EscapeUtil.EntityArrays.javaCtrlCharsEscape(),
                    javaQuoteTable(), ecmaQuoteTable(), jsonQuoteTable(), xml10ControlTable(), xml11ControlTable(),
                    // duplicate key (the last row wins), lone surrogates as keys, an empty replacement
                    { { "a", "1" }, { "b", "2" }, { "a", "3" } }, { { chars(0xd800), "H" }, { chars(0xdc00), "L" }, { "x", "" } } };

            long seed = 20261008L;

            for (final String[][] table : tables) {
                assertSameTranslation(new ReferenceLookup(table), new EscapeUtil.LookupTranslator(table), alphabetOf(table), seed++);
            }

            // tables that are not one-char-only keep the general path; spot-check them against the reference too
            final String[][] mixed = { { "a", "1" }, { "ab", "2" }, { chars(0xd83d, 0xde00), "S" } };
            assertSameTranslation(new ReferenceLookup(mixed), new EscapeUtil.LookupTranslator(mixed), alphabetOf(mixed), seed);
            assertEquals("abc", new EscapeUtil.LookupTranslator().translate("abc"));
            assertEquals("abc", new EscapeUtil.LookupTranslator((CharSequence[][]) null).translate("abc"));
        }

        // G026-01: the public escape translators (all built from one-char tables) against reference compositions of the plain lookup
        @Test
        public void testEscapeTranslators_matchReferenceCompositions() throws java.io.IOException {
            final ReferenceLookup basic = new ReferenceLookup(EscapeUtil.EntityArrays.basicEscape());
            final ReferenceLookup apos = new ReferenceLookup(EscapeUtil.EntityArrays.aposEscape());
            final ReferenceLookup iso = new ReferenceLookup(EscapeUtil.EntityArrays.iso8859_1Escape());
            final ReferenceLookup html40 = new ReferenceLookup(EscapeUtil.EntityArrays.html40ExtendedEscape());
            final ReferenceLookup ctrl = new ReferenceLookup(EscapeUtil.EntityArrays.javaCtrlCharsEscape());
            final char[] alphabet = alphabetOf(EscapeUtil.EntityArrays.basicEscape(), EscapeUtil.EntityArrays.aposEscape(),
                    EscapeUtil.EntityArrays.javaCtrlCharsEscape(), ecmaQuoteTable(), xml10ControlTable(),
                    new String[][] { { chars(0xc0, 0xff, 0x3b1, 0x20ac), "" } });

            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(new ReferenceLookup(javaQuoteTable()), ctrl, EscapeUtil.JavaUnicodeEscaper.outsideOf(32, 0x7f)),
                    EscapeUtil.ESCAPE_JAVA, alphabet, 1L);
            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(new ReferenceLookup(ecmaQuoteTable()), ctrl, EscapeUtil.JavaUnicodeEscaper.outsideOf(32, 0x7f)),
                    EscapeUtil.ESCAPE_ECMASCRIPT, alphabet, 2L);
            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(new ReferenceLookup(jsonQuoteTable()), ctrl, EscapeUtil.JavaUnicodeEscaper.outsideOf(32, 0x7f)),
                    EscapeUtil.ESCAPE_JSON, alphabet, 3L);
            assertSameTranslation(new EscapeUtil.AggregateTranslator(basic, apos, new ReferenceLookup(xml10ControlTable()),
                    EscapeUtil.NumericEntityEscaper.between(0x7f, 0x84), EscapeUtil.NumericEntityEscaper.between(0x86, 0x9f),
                    new EscapeUtil.UnicodeUnpairedSurrogateRemover()), EscapeUtil.ESCAPE_XML10, alphabet, 4L);
            assertSameTranslation(
                    new EscapeUtil.AggregateTranslator(basic, apos, new ReferenceLookup(xml11ControlTable()), EscapeUtil.NumericEntityEscaper.between(0x1, 0x8),
                            EscapeUtil.NumericEntityEscaper.between(0xe, 0x1f), EscapeUtil.NumericEntityEscaper.between(0x7f, 0x84),
                            EscapeUtil.NumericEntityEscaper.between(0x86, 0x9f), new EscapeUtil.UnicodeUnpairedSurrogateRemover()),
                    EscapeUtil.ESCAPE_XML11, alphabet, 5L);
            assertSameTranslation(new EscapeUtil.AggregateTranslator(basic, iso), EscapeUtil.ESCAPE_HTML3, alphabet, 6L);
            assertSameTranslation(new EscapeUtil.AggregateTranslator(basic, iso, html40), EscapeUtil.ESCAPE_HTML4, alphabet, 7L);

            // a few literal results, and the static helpers that delegate to these translators
            assertEquals("say " + BACKSLASH + "\"hi" + BACKSLASH + "\"" + BACKSLASH + "n", EscapeUtil.escapeJava("say \"hi\"\n"));
            assertEquals("D&eacute;j&agrave; &ldquo;x&rdquo; &amp; &lt;",
                    EscapeUtil.escapeHtml4("D" + chars(0xe9) + "j" + chars(0xe0) + " " + chars(0x201c) + "x" + chars(0x201d) + " & <"));
            assertEquals("It&apos;s &quot;A&quot;", EscapeUtil.escapeXml10("It's \"A\"" + chars(0x0, 0xffff)));
            assertEquals("&#11;&#1;&#127;", EscapeUtil.escapeXml11(chars(0xb, 0x1, 0x7f, 0x0)));
            assertEquals(BACKSLASH + "'" + BACKSLASH + "/" + BACKSLASH + "t", EscapeUtil.escapeEcmaScript("'/\t"));
            assertEquals(BACKSLASH + "/" + BACKSLASH + "u00E9", EscapeUtil.escapeJson("/" + chars(0xe9)));
            assertEquals("&eacute;" + chars(0x201c), EscapeUtil.escapeHtml3(chars(0xe9, 0x201c)));
            assertNull(EscapeUtil.escapeJava(null));
            assertEquals("", EscapeUtil.escapeHtml4(""));
        }

        // G026-01: a null Writer still fails at the write of a matched one-char key, and nothing is consumed for an unmatched char
        @Test
        public void testLookupTranslator_singleCharTable_nullWriterAndIndexErrors() throws java.io.IOException {
            final EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(EscapeUtil.EntityArrays.basicEscape());

            assertThrows(NullPointerException.class, () -> translator.translate("<", 0, null));
            assertEquals(0, translator.translate("a", 0, null));
            assertThrows(IndexOutOfBoundsException.class, () -> translator.translate("<", 1, new java.io.StringWriter()));
            assertThrows(IndexOutOfBoundsException.class, () -> translator.translate("<", -1, new java.io.StringWriter()));

            final java.io.StringWriter out = new java.io.StringWriter();
            assertEquals(1, translator.translate("x<y", 1, out));
            assertEquals("&lt;", out.toString());
        }

        // G026-02: backslash-u escapes are now decoded in place; results, consumed counts and error messages are unchanged
        @Test
        public void testUnicodeUnescaper_decodesInPlaceWithSameErrors() throws java.io.IOException {
            final String u = BACKSLASH + "u";

            assertEquals("A", EscapeUtil.unescapeJava(u + "0041"));
            assertEquals("x" + chars(0xe9) + "y", EscapeUtil.unescapeJava("x" + BACKSLASH + "uu+00e9y"));
            assertEquals(chars(0xabcd, 0xabcd), EscapeUtil.unescapeJava(u + "ABCD" + u + "abcd"));
            assertEquals(chars(0xd83d, 0xde00), EscapeUtil.unescapeJson(u + "D83D" + u + "de00"));
            assertEquals("qQ", EscapeUtil.UNESCAPE_JAVA.translate(new StringBuilder("q" + u + "0051")));
            assertEquals(chars(0x4e2d, 0x6587) + " " + BACKSLASH + "\n",
                    EscapeUtil.unescapeJava(u + "4e2d" + u + "6587 " + BACKSLASH + BACKSLASH + BACKSLASH + "n"));

            final java.io.StringWriter out = new java.io.StringWriter();
            assertEquals(9, EscapeUtil.UNESCAPE_JAVA.translate("ab" + BACKSLASH + "uuu+0042cd", 2, out));
            assertEquals("B", out.toString());

            final String[][] invalid = { { "ab" + u + "12G4cd", "12G4" }, { u + "+00x1", "00x1" }, { BACKSLASH + "uuu+ 123", " 123" }, { u + "-123", "-123" },
                    { u + chars(0x660, 0x660, 0x664, 0x661), chars(0x660, 0x660, 0x664, 0x661) }, { u + "123" + chars(0xff10), "123" + chars(0xff10) } };

            for (final String[] row : invalid) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava(row[0]));
                assertEquals("Unable to parse unicode value: " + row[1], e.getMessage(), row[0]);
            }

            final IllegalArgumentException shortEscape = assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("x" + u + "12"));
            assertEquals("Less than 4 hex digits in unicode value: '" + u + "12' due to end of CharSequence", shortEscape.getMessage());

            // the text before a malformed escape is already written; nothing of the escape itself is
            final java.io.StringWriter partial = new java.io.StringWriter();
            assertThrows(IllegalArgumentException.class, () -> EscapeUtil.UNESCAPE_JAVA.translate("ok" + u + "00Z0", partial));
            assertEquals("ok", partial.toString());

            // a CharSequence whose subSequence is not a String: the message text still comes from that subsequence
            final String text = "z" + u + "12x4";
            final CharSequence custom = new CharSequence() {
                @Override
                public int length() {
                    return text.length();
                }

                @Override
                public char charAt(final int index) {
                    return text.charAt(index);
                }

                @Override
                public CharSequence subSequence(final int start, final int end) {
                    return new StringBuilder(text.substring(start, end));
                }

                @Override
                public String toString() {
                    return text;
                }
            };

            final IllegalArgumentException customError = assertThrows(IllegalArgumentException.class, () -> EscapeUtil.UNESCAPE_JAVA.translate(custom));
            assertEquals("Unable to parse unicode value: 12x4", customError.getMessage());
        }

        // G026-03: the NUL scan now uses String.indexOf; every entry point keeps its result and its IAE (type, message, which part is checked)
        @Test
        public void testFilenameUtil_nullByteScanSameOutcomes() {
            final String nul = chars(0);
            final String cjk = chars(0x4e2d, 0x6587);
            final String[] poisoned = { nul, nul + "a.txt", "a" + nul + ".txt", "dir/a.txt" + nul, "C:/dir/" + nul + "/f.txt", cjk + nul + ".txt",
                    "/" + chars(0xe9) + "/x" + nul, "~" + nul, "dir" + nul + "/a.txt" };

            for (final String path : poisoned) {
                // these scan the whole filename first
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalize(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalize(path, true)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalizeNoEndSeparator(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getName(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getBaseName(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getExtension(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.removeExtension(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.indexOfExtension(path)).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.changeExtension(path, "log")).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(path, "txt")).getMessage(), path);
                assertEquals(NUL_MESSAGE,
                        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(path, new String[] { "txt" })).getMessage(), path);
                assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.isExtension(path, Arrays.asList("txt"))).getMessage(),
                        path);
            }

            // these check only the part they return
            assertEquals("", FilenameUtil.getPrefix("a" + nul + ".txt"));
            assertEquals("/", FilenameUtil.getPrefix("/x" + nul));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPrefix("~" + nul)).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPrefix("~ab" + nul + "/x")).getMessage());
            assertEquals("dir/", FilenameUtil.getPath("dir/a" + nul + ".txt"));
            assertEquals("dir", FilenameUtil.getPathNoEndSeparator("dir/a" + nul + ".txt"));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPath("dir" + nul + "/a.txt")).getMessage());
            assertEquals(NUL_MESSAGE,
                    assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getPathNoEndSeparator("dir" + nul + "/a.txt")).getMessage());
            assertEquals("C:/dir/", FilenameUtil.getFullPath("C:/dir/" + nul + "f.txt"));
            assertEquals("C:/dir", FilenameUtil.getFullPathNoEndSeparator("C:/dir/" + nul + "f.txt"));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPath("C:/d" + nul + "/f.txt")).getMessage());
            assertEquals(NUL_MESSAGE,
                    assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPathNoEndSeparator("C:/d" + nul + "/f.txt")).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getFullPathNoEndSeparator("~" + nul)).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.changeExtension("a.txt", "lo" + nul + "g")).getMessage());

            // no NUL: unchanged results, Latin-1 and UTF-16 strings, empty and null
            assertEquals("txt", FilenameUtil.getExtension(cjk + "/" + cjk + ".txt"));
            assertEquals(cjk + ".txt", FilenameUtil.getName("a/" + cjk + ".txt"));
            assertEquals(cjk, FilenameUtil.getBaseName("a" + BACKSLASH + cjk + ".txt"));
            assertEquals(cjk + ".log", FilenameUtil.changeExtension(cjk + ".txt", ".log"));
            assertEquals("a/" + cjk + "/", FilenameUtil.normalize("a/./b/../" + cjk + "//", true));
            assertTrue(FilenameUtil.isExtension(cjk + ".java", "java"));
            assertTrue(FilenameUtil.isExtension(cjk, new String[0]));
            assertFalse(FilenameUtil.isExtension("x." + cjk, Arrays.asList("txt", null)));
            assertEquals(4, FilenameUtil.indexOfExtension("file.txt"));
            assertEquals(-1, FilenameUtil.indexOfExtension("a.txt/b"));
            assertEquals("", FilenameUtil.getExtension(""));
            assertEquals("", FilenameUtil.normalize(""));
            assertNull(FilenameUtil.getExtension(null));
            assertNull(FilenameUtil.normalize(null));
            assertFalse(FilenameUtil.isExtension(null, "txt"));
            assertEquals("~user/", FilenameUtil.getPrefix("~user"));

            final StringBuilder longPath = new StringBuilder();

            while (longPath.length() < 5000) {
                longPath.append("/segment").append(longPath.length() % 7);
            }

            assertEquals("txt", FilenameUtil.getExtension(longPath + "/f.txt"));
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getExtension(longPath + "/f.txt" + nul)).getMessage());
            assertEquals(NUL_MESSAGE, assertThrows(IllegalArgumentException.class, () -> FilenameUtil.getName(cjk + longPath + nul + "/f.txt")).getMessage());
        }
    }

    // ---- perf review 2026-10-08 G026 end ----
    // ---- perf review 2026-10-08 G027 begin ----
    @Nested
    class G027 {
        // quiet NaN bit patterns with different signs and payloads; Float.equals treats them all as one value
        private final int[] quietNaNBits = { 0x7fc00000, 0x7fc00001, 0xffc00000, 0x7fffffff, 0xfff00abc };

        /**
         * Reference for {@code FloatList.removeDuplicates()}: the raw bits of the first occurrence of each value under
         * {@code Float.equals} (that is, {@code Float.floatToIntBits}), in encounter order.
         */
        private int[] firstOccurrenceBits(final float[] a, final int size) {
            final Set<Integer> seen = new HashSet<>();
            final List<Integer> kept = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                if (seen.add(Float.floatToIntBits(a[i]))) {
                    kept.add(Float.floatToRawIntBits(a[i]));
                }
            }

            final int[] result = new int[kept.size()];

            for (int i = 0; i < result.length; i++) {
                result[i] = kept.get(i);
            }

            return result;
        }

        @SuppressWarnings("deprecation")
        private void assertRemoveDuplicatesMatchesReference(final float[] a, final int size, final String label) {
            final float[] backing = a.clone();
            final int[] expected = firstOccurrenceBits(backing, size);
            final float[] spare = Arrays.copyOfRange(backing, size, backing.length);
            final FloatList list = FloatList.of(backing, size);

            assertEquals(expected.length != size, list.removeDuplicates(), label);
            assertEquals(expected.length, list.size(), label);
            assertSame(backing, list.internalArray(), label);

            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], Float.floatToRawIntBits(backing[i]), label + " index " + i);
            }

            for (int i = expected.length; i < size; i++) {
                assertEquals(0, Float.floatToRawIntBits(backing[i]), label + " cleared slot " + i);
            }

            for (int i = size; i < backing.length; i++) {
                assertEquals(Float.floatToRawIntBits(spare[i - size]), Float.floatToRawIntBits(backing[i]), label + " spare slot " + i);
            }
        }

        private float randomValue(final Random rnd, final int domain, final int size, final float[] specials) {
            switch (domain) {
                case 0: // few distinct values: NaN payload variants, signed zeros, infinities, extremes
                    return rnd.nextInt(4) == 0 ? Float.intBitsToFloat(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : specials[rnd.nextInt(specials.length)];
                case 1: // whole numbers with duplicates
                    return rnd.nextInt(Math.max(1, size / 2));
                case 2: // mostly distinct values
                    return rnd.nextFloat();
                default: { // random bit patterns; NaN patterns are replaced by quiet NaN payload variants
                    final float f = Float.intBitsToFloat(rnd.nextInt());
                    return Float.isNaN(f) ? Float.intBitsToFloat(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : f;
                }
            }
        }

        // G027-01 (OPEN): removeDuplicates keeps first occurrences bit for bit, the backing array, cleared slots and spare capacity; pins any primitive-set port of the unsorted path
        @Test
        public void testRemoveDuplicates_matchesFirstOccurrenceReference() {
            final Random rnd = new Random(20261008L);
            final float[] specials = { 0.0f, -0.0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.MIN_VALUE, -Float.MIN_VALUE, Float.MAX_VALUE,
                    -Float.MAX_VALUE, 1f, -1f };
            final int[] sizes = { 2, 3, 4, 5, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 127, 128, 129, 257, 1000, 3000 };

            for (final int size : sizes) {
                for (int domain = 0; domain < 4; domain++) {
                    for (int trial = 0; trial < 5; trial++) {
                        final float[] a = new float[size + rnd.nextInt(3)];

                        for (int i = 0; i < a.length; i++) {
                            a[i] = randomValue(rnd, domain, size, specials);
                        }

                        assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " domain=" + domain + " trial=" + trial);
                    }
                }
            }
        }

        // G027-01 (OPEN): keys that all share one home slot of the primitive key set in N (the structure a port would use; they force its boxed fallback) keep the same result
        @Test
        public void testRemoveDuplicates_collidingKeysMatchReference() {
            final int hashMultiplier = 0x9E3779B9; // multiplier of the hash used by the primitive key set in N (N.distinct)
            int inverse = hashMultiplier;

            for (int i = 0; i < 5; i++) {
                inverse *= 2 - hashMultiplier * inverse;
            }

            final Random rnd = new Random(8L);

            for (final int distinct : new int[] { 70, 200, 5000 }) {
                final float[] a = new float[distinct * 2];

                for (int i = 0; i < a.length; i++) {
                    // the products with the multiplier are consecutive, so all keys start probing at the same slot
                    final float f = Float.intBitsToFloat((0x91A00000 + rnd.nextInt(distinct)) * inverse);
                    a[i] = Float.isNaN(f) ? Float.intBitsToFloat(quietNaNBits[rnd.nextInt(quietNaNBits.length)]) : f;
                }

                assertRemoveDuplicatesMatchesReference(a, a.length, "colliding distinct=" + distinct);
            }
        }

        // G027-01 (OPEN): in-place semantics on a shared array: kept order, cleared slots, untouched spare slots, unchanged when unique, first NaN payload kept
        @Test
        @SuppressWarnings("deprecation")
        public void testRemoveDuplicates_sharedBackingArrayExamples() {
            final float[] shared = { 3f, 1f, 3f, 2f, 1f, 9f, 9f };
            final FloatList list = FloatList.of(shared, 5);

            assertTrue(list.removeDuplicates());
            assertSame(shared, list.internalArray());
            assertEquals(3, list.size());
            assertArrayEquals(new float[] { 3f, 1f, 2f, 0f, 0f, 9f, 9f }, shared);

            final float[] unique = { 3f, 1f, 2f, -0.0f, 0.0f, Float.NaN };
            final FloatList uniqueList = FloatList.of(unique);

            assertFalse(uniqueList.removeDuplicates());
            assertSame(unique, uniqueList.internalArray());
            assertEquals(6, uniqueList.size());
            assertArrayEquals(new float[] { 3f, 1f, 2f, -0.0f, 0.0f, Float.NaN }, unique);

            final float firstNaN = Float.intBitsToFloat(0x7fc00001);
            final float[] nans = { 2f, firstNaN, 1f, Float.NaN, 2f };
            final FloatList nanList = FloatList.of(nans);

            assertTrue(nanList.removeDuplicates());
            assertEquals(3, nanList.size());
            assertEquals(0x7fc00001, Float.floatToRawIntBits(nans[1]));
            assertArrayEquals(new float[] { 2f, firstNaN, 1f, 0f, 0f }, nans);
        }
    }

    // ---- perf review 2026-10-08 G027 end ----
    // ---- perf review 2026-10-08 G030 begin ----
    @Nested
    class G030 {

        private String parsed(final String text) {
            try {
                final Fraction fraction = Fraction.of(text);
                return fraction.numerator() + "/" + fraction.denominator();
            } catch (final RuntimeException e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        private String terms(final java.util.function.Supplier<Fraction> operation) {
            try {
                final Fraction fraction = operation.get();
                return fraction.numerator() + "/" + fraction.denominator();
            } catch (final ArithmeticException e) {
                return "AE: " + e.getMessage();
            }
        }

        // Reference for every reducing factory/operation: exact reduction, sign moved onto the numerator, int range check.
        private String reducedReference(java.math.BigInteger numerator, java.math.BigInteger denominator) {
            if (denominator.signum() == 0) {
                return "AE: The denominator must not be zero";
            }
            if (numerator.signum() == 0) {
                return "0/1";
            }
            final java.math.BigInteger gcd = numerator.gcd(denominator);
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
            if (denominator.signum() < 0) {
                numerator = numerator.negate();
                denominator = denominator.negate();
            }
            if (numerator.compareTo(java.math.BigInteger.valueOf(Integer.MIN_VALUE)) < 0
                    || numerator.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                    || denominator.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                return "AE: Fraction terms exceed the int range: " + numerator + "/" + denominator;
            }
            return numerator + "/" + denominator;
        }

        private java.math.BigInteger big(final long value) {
            return java.math.BigInteger.valueOf(value);
        }

        private int randomTerm(final Random random) {
            switch (random.nextInt(6)) {
                case 0:
                    return random.nextInt(41) - 20;
                case 1:
                    return random.nextInt(20001) - 10000;
                case 2:
                    return random.nextInt();
                case 3:
                    return (random.nextBoolean() ? 1 : -1) << random.nextInt(32);
                case 4:
                    return (random.nextInt(9) - 4) * (1 << random.nextInt(28));
                default:
                    return random.nextBoolean() ? Integer.MIN_VALUE + random.nextInt(3) : Integer.MAX_VALUE - random.nextInt(3);
            }
        }

        // G030-01: the "Y/Z" and "X Y/Z" forms parse their integer components in place (whole-number and decimal forms unchanged).
        @Test
        void testOfString_fractionFormsParsedInPlace() {
            assertEquals("3/4", parsed("3/4"));
            assertEquals("-7/8", parsed("-7/8"));
            assertEquals("2/4", parsed("2/4"));
            assertEquals("3/4", parsed("+3/+4"));
            assertEquals("-3/4", parsed("3/-4"));
            assertEquals("3/4", parsed("-3/-4"));
            assertEquals("12/4", parsed("0012/0004"));
            assertEquals("-2147483648/2147483647", parsed("-2147483648/2147483647"));
            assertEquals("2147483647/1", parsed("2147483647/1"));
            assertEquals("3/4", parsed("\u0663/\u0664"));
            assertEquals("7/4", parsed("1 3/4"));
            assertEquals("-7/3", parsed("-2 1/3"));
            assertEquals("6/4", parsed("1 2/4"));
            assertEquals("-6/4", parsed("-1 2/4"));
            assertEquals("15/2", parsed("7 1/2"));
            assertEquals("15/2", parsed("+7 1/2"));
            assertEquals("-1/2", parsed("-0 1/2"));
            assertEquals("-2/4", parsed("-0 2/4"));
            assertEquals("1/2", parsed("+0 1/2"));
            assertEquals("1/2", parsed("0 1/2"));
            assertEquals("0/2", parsed("-0 0/2"));
            assertEquals("0/1", parsed("0 0/1"));
            assertEquals("3/2", parsed("\u0661 \u0661/\u0662"));
            assertEquals("-2147483648/1", parsed("-2147483648 0/1"));
            assertEquals("5/1", parsed("5"));
            assertEquals("-12/1", parsed("-12"));
            assertEquals("7/1", parsed("+7"));
            assertEquals("0/1", parsed("-0"));
            assertEquals("-2147483648/1", parsed("-2147483648"));
            assertEquals("1/2", parsed("0.5"));
            assertEquals("1/4", parsed("0.25"));
            // the decimal form keeps Double.parseDouble's leniency
            assertEquals("1/2", parsed(" 0.5"));
            assertEquals("1/2", parsed("0.5d"));

            final Fraction unreduced = Fraction.of("-0 2/4");
            assertEquals(-2, unreduced.numerator());
            assertEquals(4, unreduced.denominator());
            assertNotSame(Fraction.ZERO, Fraction.of("0/1"));
            assertEquals(Fraction.ZERO, Fraction.of("0/1"));
        }

        // G030-01: a malformed component is still named exactly in the message, and the failure order is unchanged.
        @Test
        void testOfString_malformedComponentsKeepTheirMessages() {
            assertEquals("NumberFormatException: The fraction \"\" could not be parsed: \"\" is not an integer", parsed(""));
            assertEquals("NumberFormatException: The fraction \" 3\" could not be parsed: \" 3\" is not an integer", parsed(" 3"));
            assertEquals("NumberFormatException: The fraction \" \" could not be parsed: \" \" is not an integer", parsed(" "));
            assertEquals("NumberFormatException: The fraction \"x\" could not be parsed: \"x\" is not an integer", parsed("x"));
            assertEquals("NumberFormatException: The fraction \"-\" could not be parsed: \"-\" is not an integer", parsed("-"));
            assertEquals("NumberFormatException: The fraction \"1/\" could not be parsed: \"\" is not an integer", parsed("1/"));
            assertEquals("NumberFormatException: The fraction \"/1\" could not be parsed: \"\" is not an integer", parsed("/1"));
            assertEquals("NumberFormatException: The fraction \"/\" could not be parsed: \"\" is not an integer", parsed("/"));
            assertEquals("NumberFormatException: The fraction \"-/2\" could not be parsed: \"-\" is not an integer", parsed("-/2"));
            assertEquals("NumberFormatException: The fraction \"1/+\" could not be parsed: \"+\" is not an integer", parsed("1/+"));
            assertEquals("NumberFormatException: The fraction \"1/2/3\" could not be parsed: \"2/3\" is not an integer", parsed("1/2/3"));
            assertEquals("NumberFormatException: The fraction \"2147483648/1\" could not be parsed: \"2147483648\" is not an integer", parsed("2147483648/1"));
            assertEquals("NumberFormatException: The fraction \"1/-2147483649\" could not be parsed: \"-2147483649\" is not an integer",
                    parsed("1/-2147483649"));
            assertEquals("NumberFormatException: The fraction \"1.0/2.0\" could not be parsed: \"1.0\" is not an integer", parsed("1.0/2.0"));
            assertEquals("NumberFormatException: The fraction \"3/4.\" could not be parsed: \"4.\" is not an integer", parsed("3/4."));
            assertEquals("NumberFormatException: The fraction \"1\t1/2\" could not be parsed: \"1\t1\" is not an integer", parsed("1\t1/2"));
            assertEquals("NumberFormatException: The fraction \"1/2x\" could not be parsed: \"2x\" is not an integer", parsed("1/2x"));
            // "X Y/Z": whole part first, then the '/' check, then numerator, then denominator
            assertEquals("NumberFormatException: The fraction \"1/2 3/4\" could not be parsed: \"1/2\" is not an integer", parsed("1/2 3/4"));
            assertEquals("NumberFormatException: The fraction \"b 3/x\" could not be parsed: \"b\" is not an integer", parsed("b 3/x"));
            assertEquals("NumberFormatException: The fraction \"2 3\" could not be parsed as the format X Y/Z", parsed("2 3"));
            assertEquals("NumberFormatException: The fraction \"2 \" could not be parsed as the format X Y/Z", parsed("2 "));
            assertEquals("NumberFormatException: The fraction \"1 /2\" could not be parsed: \"\" is not an integer", parsed("1 /2"));
            assertEquals("NumberFormatException: The fraction \"1 1/\" could not be parsed: \"\" is not an integer", parsed("1 1/"));
            assertEquals("NumberFormatException: The fraction \"1 2 3/4\" could not be parsed: \"2 3\" is not an integer", parsed("1 2 3/4"));
            assertEquals("NumberFormatException: The fraction \"1  1/2\" could not be parsed: \" 1\" is not an integer", parsed("1  1/2"));
            assertEquals("NumberFormatException: The fraction \"1 1/2/3\" could not be parsed: \"2/3\" is not an integer", parsed("1 1/2/3"));
            assertEquals("NumberFormatException: The fraction \"1 x/0\" could not be parsed: \"x\" is not an integer", parsed("1 x/0"));
            assertEquals("NumberFormatException: The fraction \"1 1.5/2\" could not be parsed: \"1.5\" is not an integer", parsed("1 1.5/2"));
            assertEquals("NumberFormatException: The fraction \"1 1/2 \" could not be parsed: \"2 \" is not an integer", parsed("1 1/2 "));
            assertEquals("NumberFormatException: The fraction \"1 1/99999999999\" could not be parsed: \"99999999999\" is not an integer",
                    parsed("1 1/99999999999"));
            // component values that parse but are rejected by the factories
            assertEquals("ArithmeticException: The denominator must not be zero", parsed("1/0"));
            assertEquals("ArithmeticException: The denominator must not be zero", parsed("1 1/0"));
            assertEquals("ArithmeticException: The numerator must not be negative", parsed("1 -1/2"));
            assertEquals("ArithmeticException: The denominator must not be negative", parsed("1 1/-2"));
            assertEquals("ArithmeticException: Fraction terms exceed the int range: 2147483648/1", parsed("2147483647 1/1"));
            assertEquals("ArithmeticException: Fraction terms exceed the int range: 4294967294/2147483647", parsed("1 2147483647/2147483647"));
            assertEquals("ArithmeticException: Fraction terms exceed the int range: 2147483648/1", parsed("-2147483648/-1"));
            assertEquals("IllegalArgumentException: The string must not be null", parsed(null));
        }

        // G030-01: random tokens (digits, signs, spaces, '/', a letter, a non-ASCII digit) give the same result or the same
        // exception as an independent reading of the documented grammar.
        @Test
        void testOfString_randomTokensMatchComponentwiseReading() {
            final char[] alphabet = { '0', '1', '2', '3', '7', '9', '-', '+', ' ', '/', '/', 'x', '\u0663' };
            final Random random = new Random(20261008L);
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 20000; i++) {
                sb.setLength(0);
                final int length = random.nextInt(10);
                for (int j = 0; j < length; j++) {
                    sb.append(alphabet[random.nextInt(alphabet.length)]);
                }
                final String text = sb.toString();
                assertEquals(expectedParse(text), parsed(text), text);
            }
        }

        // Independent model of Fraction.of(String) for tokens without '.': component parsing via substrings.
        private String expectedParse(final String text) {
            final int slash = text.indexOf('/');
            final int space = text.indexOf(' ');
            try {
                if (space > 0) {
                    final int whole = component(text, text.substring(0, space));
                    final String rest = text.substring(space + 1);
                    final int restSlash = rest.indexOf('/');
                    if (restSlash < 0) {
                        return "NumberFormatException: The fraction \"" + text + "\" could not be parsed as the format X Y/Z";
                    }
                    final int numerator = component(text, rest.substring(0, restSlash));
                    final int denominator = component(text, rest.substring(restSlash + 1));
                    final Fraction mixed = Fraction.ofMixed(whole, numerator, denominator);
                    if (whole == 0 && text.charAt(0) == '-') {
                        return Fraction.of(-mixed.numerator(), mixed.denominator()).numerator() + "/" + mixed.denominator();
                    }
                    return mixed.numerator() + "/" + mixed.denominator();
                }
                if (slash < 0) {
                    return component(text, text) + "/1";
                }
                final Fraction simple = Fraction.of(component(text, text.substring(0, slash)), component(text, text.substring(slash + 1)));
                return simple.numerator() + "/" + simple.denominator();
            } catch (final RuntimeException e) {
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        private int component(final String text, final String part) {
            try {
                return Integer.parseInt(part);
            } catch (final NumberFormatException e) {
                throw new NumberFormatException("The fraction \"" + text + "\" could not be parsed: \"" + part + "\" is not an integer");
            }
        }

        // G030-02: reducing factories and operations skip the division by a gcd of 1; pin coprime and non-coprime terms,
        // sign normalisation, the ZERO constant and the int-range failure message.
        @Test
        void testOfReduce_coprimeAndCommonFactorTerms() {
            assertEquals("3/4", terms(() -> Fraction.of(3, 4, true)));
            assertEquals("3/4", terms(() -> Fraction.of(6, 8, true)));
            assertEquals("-3/4", terms(() -> Fraction.of(3, -4, true)));
            assertEquals("-3/4", terms(() -> Fraction.of(-6, 8, true)));
            assertEquals("3/4", terms(() -> Fraction.of(-6, -8, true)));
            assertEquals("1/1", terms(() -> Fraction.of(7, 7, true)));
            assertEquals("-1/1", terms(() -> Fraction.of(7, -7, true)));
            assertEquals("1/1", terms(() -> Fraction.of(Integer.MIN_VALUE, Integer.MIN_VALUE, true)));
            assertEquals("-1/1073741824", terms(() -> Fraction.of(2, Integer.MIN_VALUE, true)));
            assertEquals("AE: Fraction terms exceed the int range: 1/2147483648", terms(() -> Fraction.of(-1, Integer.MIN_VALUE, true)));
            assertEquals("AE: Fraction terms exceed the int range: -3/2147483648", terms(() -> Fraction.of(3, Integer.MIN_VALUE, true)));
            assertEquals("AE: Fraction terms exceed the int range: 2147483648/1", terms(() -> Fraction.of(Integer.MIN_VALUE, -1, true)));
            assertEquals("-2147483647/2147483646", terms(() -> Fraction.of(Integer.MAX_VALUE, -(Integer.MAX_VALUE - 1), true)));
            assertEquals("AE: The denominator must not be zero", terms(() -> Fraction.of(1, 0, true)));
            assertSame(Fraction.ZERO, Fraction.of(0, 7, true));
            assertSame(Fraction.ZERO, Fraction.of(0, -7, true));
            assertEquals("7/4", terms(() -> Fraction.ofMixed(1, 3, 4, true)));
            assertEquals("3/2", terms(() -> Fraction.ofMixed(1, 2, 4, true)));
            assertEquals("-3/2", terms(() -> Fraction.ofMixed(-1, 2, 4, true)));

            final Fraction half = Fraction.of(1, 2);
            final Fraction third = Fraction.of(1, 3);
            assertEquals("5/6", terms(() -> half.add(third)));
            assertEquals("1/1", terms(() -> half.add(half)));
            assertEquals("1/6", terms(() -> half.subtract(third)));
            assertEquals("0/1", terms(() -> half.subtract(half)));
            assertSame(Fraction.ZERO, half.subtract(half));
            assertEquals("1/6", terms(() -> half.multipliedBy(third)));
            assertEquals("1/2", terms(() -> Fraction.of(2, 3).multipliedBy(Fraction.of(3, 4))));
            assertEquals("3/2", terms(() -> half.dividedBy(third)));
            assertEquals("1/1", terms(() -> half.dividedBy(Fraction.of(2, 4))));
            assertEquals("-1/2", terms(() -> half.negate()));
            assertEquals("-1/2", terms(() -> Fraction.of(2, 4).negate()));
            assertEquals("2/1", terms(() -> half.invert()));
            assertEquals("-2/1", terms(() -> Fraction.of(-2, 4).invert()));
            assertEquals("AE: Fraction terms exceed the int range: 2147483648/1", terms(() -> Fraction.of(Integer.MIN_VALUE, 1).negate()));
            assertEquals("AE: Fraction terms exceed the int range: 2147483648/1", terms(() -> Fraction.of(Integer.MIN_VALUE, 1).abs()));
            // a coprime (gcd 1) sum whose terms do not fit in int
            assertEquals("AE: Fraction terms exceed the int range: 4294967293/4611686011984936962",
                    terms(() -> Fraction.of(1, Integer.MAX_VALUE).add(Fraction.of(1, Integer.MAX_VALUE - 1))));
            assertEquals("32/243", terms(() -> Fraction.of(2, 3).pow(5)));
            assertEquals("-2147483648/1", terms(() -> Fraction.of(-2, 1).pow(31)));
            assertEquals("243/32", terms(() -> Fraction.of(4, 6).pow(-5)));
        }

        // G030-02: every reducing operation agrees with an exact BigInteger reference on random terms (incl. the
        // int extremes, powers of two and small values, so both gcd == 1 and gcd > 1 occur often).
        @Test
        void testArithmetic_randomTermsMatchExactReference() {
            final Random random = new Random(8L);
            int coprime = 0;
            int common = 0;
            for (int i = 0; i < 20000; i++) {
                final int n1 = randomTerm(random);
                final int rawD1 = randomTerm(random);
                final int n2 = randomTerm(random);
                final int rawD2 = randomTerm(random);
                final int d1 = rawD1 == 0 ? 1 : rawD1;
                final int d2 = rawD2 == 0 ? 3 : rawD2;

                final String reduced = reducedReference(big(n1), big(d1));
                assertEquals(reduced, terms(() -> Fraction.of(n1, d1, true)), n1 + "/" + d1);
                if (big(n1).gcd(big(d1)).equals(java.math.BigInteger.ONE)) {
                    coprime++;
                } else {
                    common++;
                }

                final Fraction a;
                final Fraction b;
                try {
                    // the non-reducing factory rejects a negative denominator whose sign cannot move (MIN_VALUE terms)
                    a = Fraction.of(n1, d1);
                    b = Fraction.of(n2, d2);
                } catch (final ArithmeticException e) {
                    continue;
                }
                final long an = a.numerator(), ad = a.denominator(), bn = b.numerator(), bd = b.denominator();
                assertEquals(reducedReference(big(an).multiply(big(bd)).add(big(bn).multiply(big(ad))), big(ad).multiply(big(bd))), terms(() -> a.add(b)),
                        a + " + " + b);
                assertEquals(reducedReference(big(an).multiply(big(bd)).subtract(big(bn).multiply(big(ad))), big(ad).multiply(big(bd))),
                        terms(() -> a.subtract(b)), a + " - " + b);
                assertEquals(reducedReference(big(an).multiply(big(bn)), big(ad).multiply(big(bd))), terms(() -> a.multipliedBy(b)), a + " * " + b);
                if (bn != 0) {
                    assertEquals(reducedReference(big(an).multiply(big(bd)), big(ad).multiply(big(bn))), terms(() -> a.dividedBy(b)), a + " / " + b);
                    assertEquals(reducedReference(big(bd), big(bn)), terms(b::invert), "invert " + b);
                }
                assertEquals(reducedReference(big(-an), big(ad)), terms(a::negate), "negate " + a);
            }
            assertTrue(coprime > 1000, "coprime=" + coprime);
            assertTrue(common > 1000, "common=" + common);
        }
    }

    // ---- perf review 2026-10-08 G030 end ----
    // ---- perf review 2026-10-08 G031 begin ----
    @Nested
    class G031 {

        /**
         * An input whose {@code isDone()} answers {@code false} on its first call only (or throws {@code isDoneFailure}
         * from the second call on); its timed {@code get} yields {@code value} or fails with {@code failure}.
         */
        private final class FlipFuture implements java.util.concurrent.Future<String> {
            private final AtomicInteger isDoneCalls = new AtomicInteger();
            private final AtomicInteger getCalls = new AtomicInteger();
            private final String value;
            private final Exception failure;
            private final RuntimeException isDoneFailure;

            FlipFuture(final String value, final Exception failure, final RuntimeException isDoneFailure) {
                this.value = value;
                this.failure = failure;
                this.isDoneFailure = isDoneFailure;
            }

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                if (isDoneCalls.incrementAndGet() == 1) {
                    return false;
                }
                if (isDoneFailure != null) {
                    throw isDoneFailure;
                }
                return true;
            }

            @Override
            public String get() throws java.util.concurrent.ExecutionException {
                return get(0, java.util.concurrent.TimeUnit.NANOSECONDS);
            }

            @Override
            public String get(final long timeout, final java.util.concurrent.TimeUnit unit) throws java.util.concurrent.ExecutionException {
                getCalls.incrementAndGet();
                if (failure != null) {
                    throw new java.util.concurrent.ExecutionException(failure);
                }
                return value;
            }
        }

        // G031-01: a pending anyOf aggregate answers isDone() == false on every poll without registering an observer,
        // and an input's success is published by the next poll.
        @Test
        void testAnyOfIsDone_pendingThenSuccess() throws Exception {
            final java.util.concurrent.CompletableFuture<String> first = new java.util.concurrent.CompletableFuture<>();
            final java.util.concurrent.CompletableFuture<String> second = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(first, second);

            for (int i = 0; i < 100; i++) {
                assertFalse(any.isDone());
            }

            assertFalse(any.isCancelled());
            assertFalse(Thread.currentThread().isInterrupted());
            assertEquals(0, first.getNumberOfDependents(), "isDone() polls; it never registers a completion listener");
            assertEquals(0, second.getNumberOfDependents());
            assertThrows(java.util.concurrent.TimeoutException.class, () -> any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));

            second.complete("second");
            assertTrue(any.isDone());
            first.complete("first");
            assertTrue(any.isDone());
            assertEquals("second", any.get());
            assertEquals("second", any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));
        }

        // G031-01: isDone() polls of a failed + pending aggregate attach nothing to anything; the non-positive-timeout
        // get still throws a TimeoutException carrying the failure exactly once, before and after a blocking getter
        // recorded it.
        @Test
        void testAnyOfIsDone_failedAndPendingKeepsTimeoutContract() throws Exception {
            final IllegalStateException failure = new IllegalStateException("first");
            final java.util.concurrent.CompletableFuture<String> pending = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(java.util.concurrent.CompletableFuture.<String> failedFuture(failure), pending);

            for (int i = 0; i < 50; i++) {
                assertFalse(any.isDone());
            }

            assertEquals(0, failure.getSuppressed().length);

            for (final long timeout : new long[] { 0, -1 }) {
                final java.util.concurrent.TimeoutException expired = assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> any.get(timeout, java.util.concurrent.TimeUnit.NANOSECONDS));
                assertArrayEquals(new Throwable[] { failure }, expired.getSuppressed());
            }

            final java.util.concurrent.TimeoutException blockingExpired = assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> any.get(20, java.util.concurrent.TimeUnit.MILLISECONDS));
            assertArrayEquals(new Throwable[] { failure }, blockingExpired.getSuppressed());

            for (int i = 0; i < 10; i++) {
                assertFalse(any.isDone());
            }

            final java.util.concurrent.TimeoutException polled = assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));
            assertArrayEquals(new Throwable[] { failure }, polled.getSuppressed());
            assertEquals(0, failure.getSuppressed().length);
            assertFalse(any.isCancelled());

            pending.complete("late");
            assertTrue(any.isDone());
            assertEquals("late", any.get());
        }

        // G031-01: when every input turns out failed during the isDone() poll, the aggregate is done and get() reports
        // the first failure (input order) with the others suppressed; later polls answer without reading the inputs.
        @Test
        void testAnyOfIsDone_allFailedDuringPollPublishesFailure() {
            final IllegalArgumentException first = new IllegalArgumentException("first");
            final IllegalStateException second = new IllegalStateException("second");
            final FlipFuture flipping = new FlipFuture(null, first, null);
            final ContinuableFuture<String> any = Futures.anyOf(flipping, java.util.concurrent.CompletableFuture.<String> failedFuture(second));

            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get());
            assertEquals(1, flipping.getCalls.get());

            final java.util.concurrent.ExecutionException e = assertThrows(java.util.concurrent.ExecutionException.class, any::get);
            assertSame(first, e.getCause());
            assertArrayEquals(new Throwable[] { second }, first.getSuppressed());
            assertFalse(any.isCancelled());
            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get(), "a published outcome answers without polling again");
            assertEquals(1, flipping.getCalls.get());
        }

        // G031-01: a success read by the isDone() poll is published once and retained; nothing is registered on the
        // still-pending sibling.
        @Test
        void testAnyOfIsDone_successReadByPollIsPublished() throws Exception {
            final FlipFuture flipping = new FlipFuture("polled", null, null);
            final java.util.concurrent.CompletableFuture<String> pending = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(flipping, pending);

            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get());
            assertEquals(1, flipping.getCalls.get());
            assertEquals("polled", any.get());
            assertEquals("polled", any.get(0, java.util.concurrent.TimeUnit.NANOSECONDS));
            assertTrue(any.isDone());
            assertEquals(2, flipping.isDoneCalls.get());
            assertEquals(1, flipping.getCalls.get());
            assertEquals(0, pending.getNumberOfDependents());
        }

        // G031-01: an InterruptedException met by the isDone() poll answers false, restores the interrupt flag, carries
        // the failures observed by that poll, and leaves the aggregate retryable.
        @Test
        void testAnyOfIsDone_interruptedPoll() throws Exception {
            final IllegalStateException failure = new IllegalStateException("first");
            final InterruptedException interruption = new InterruptedException("poll");
            final AtomicInteger zipCalls = new AtomicInteger();
            final java.util.concurrent.Future<String> interruptOnce = Futures.compose(java.util.concurrent.CompletableFuture.completedFuture("a"),
                    java.util.concurrent.CompletableFuture.completedFuture("b"), (a, b) -> {
                        if (zipCalls.incrementAndGet() == 1) {
                            throw interruption;
                        }
                        return "retry";
                    });
            final java.util.concurrent.CompletableFuture<String> pending = new java.util.concurrent.CompletableFuture<>();
            final ContinuableFuture<String> any = Futures.anyOf(java.util.concurrent.CompletableFuture.<String> failedFuture(failure), interruptOnce, pending);

            try {
                assertFalse(any.isDone());
                assertTrue(Thread.currentThread().isInterrupted());
                assertArrayEquals(new Throwable[] { failure }, interruption.getSuppressed());
                assertEquals(0, failure.getSuppressed().length);
                assertEquals(1, zipCalls.get());
            } finally {
                Thread.interrupted();
            }

            assertTrue(any.isDone());
            assertEquals(2, zipCalls.get());
            assertEquals("retry", any.get());
            assertEquals(0, pending.getNumberOfDependents());
        }

        // G031-01: an unchecked exception thrown by an input's isDone() during the poll keeps its old outcome:
        // a CancellationException reads as done, anything else propagates unchanged.
        @Test
        void testAnyOfIsDone_inputIsDoneThrowsDuringPoll() {
            final FlipFuture cancelling = new FlipFuture(null, null, new java.util.concurrent.CancellationException("from isDone"));
            assertTrue(Futures.anyOf(cancelling, new java.util.concurrent.CompletableFuture<String>()).isDone());
            assertEquals(2, cancelling.isDoneCalls.get());

            final IllegalStateException boom = new IllegalStateException("boom");
            final FlipFuture failing = new FlipFuture(null, null, boom);
            final ContinuableFuture<String> any = Futures.anyOf(failing, new java.util.concurrent.CompletableFuture<String>());
            assertSame(boom, assertThrows(IllegalStateException.class, any::isDone));
            assertEquals(0, failing.getCalls.get());
        }
    }

    // ---- perf review 2026-10-08 G031 end ----
    // ---- perf review 2026-10-08 G032 begin ----
    @Nested
    class G032 {

        /** The pooled read buffer IOUtil's stream reads start in (16 KB on heaps up to 16 GB). */
        private final int bufferSize = Objectory.BUFFER_SIZE;

        /**
         * Delivers {@code data} in seeded pseudo-random chunks, may answer 0, may fail or break the read contract at a
         * chosen call, and folds every read call (destination length, offset, requested length, answer) into a digest.
         */
        private final class ScriptedInput extends java.io.InputStream {
            private final byte[] data;
            private final Random random;
            private final int maxChunk;
            private final int zeroPercent;
            private final int failAtCall;
            private final int anomalyAtCall;
            private final int anomalyDelta;
            final java.io.IOException failure = new java.io.IOException("scripted failure");
            int position;
            int calls;
            long digest = 17;

            ScriptedInput(final byte[] data, final long seed, final int maxChunk, final int zeroPercent, final int failAtCall, final int anomalyAtCall,
                    final int anomalyDelta) {
                this.data = data;
                this.random = new Random(seed);
                this.maxChunk = maxChunk;
                this.zeroPercent = zeroPercent;
                this.failAtCall = failAtCall;
                this.anomalyAtCall = anomalyAtCall;
                this.anomalyDelta = anomalyDelta;
            }

            @Override
            public int read() {
                throw new UnsupportedOperationException("single-byte read is not expected");
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws java.io.IOException {
                calls++;
                digest = ((digest * 31 + b.length) * 31 + off) * 31 + len;

                if (calls == failAtCall) {
                    throw failure;
                }

                final int answer;

                if (calls == anomalyAtCall) {
                    // Contract-breaking count: an over-report (nothing written) or a negative value other than -1.
                    answer = anomalyDelta >= 0 ? len + anomalyDelta : anomalyDelta;
                } else if (len == 0 || random.nextInt(100) < zeroPercent) {
                    answer = 0;
                } else if (position >= data.length) {
                    answer = -1;
                } else {
                    answer = Math.min(len, Math.min(data.length - position, 1 + random.nextInt(maxChunk)));
                    System.arraycopy(data, position, b, off, answer);
                    position += answer;
                }

                digest = digest * 31 + answer;

                return answer;
            }
        }

        private List<Object> outcome(final ScriptedInput input, final java.util.function.Function<ScriptedInput, String> operation) {
            final List<Object> outcome = new ArrayList<>();

            try {
                outcome.add(operation.apply(input));
            } catch (final RuntimeException e) {
                outcome.add(e.getClass().getName());
                outcome.add(e.getMessage());
                outcome.add(e.getCause() == null ? null : e.getCause().getClass().getName());
                outcome.add(e.getCause() == input.failure);
            }

            outcome.add(input.calls);
            outcome.add(input.digest);
            outcome.add(input.position);

            return outcome;
        }

        /** The pre-change implementation of readAllToString(InputStream, Charset): read all bytes, then decode them. */
        private String reference(final java.io.InputStream input, final java.nio.charset.Charset charset) {
            final byte[] bytes = IOUtil.readAllBytes(input);

            return new String(bytes, charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset);
        }

        private void assertSameAsReference(final byte[] data, final java.nio.charset.Charset charset, final long seed, final int maxChunk,
                final int zeroPercent, final int failAtCall, final int anomalyAtCall, final int anomalyDelta) {
            final List<Object> expected = outcome(new ScriptedInput(data, seed, maxChunk, zeroPercent, failAtCall, anomalyAtCall, anomalyDelta),
                    in -> reference(in, charset));
            final List<Object> actual = outcome(new ScriptedInput(data, seed, maxChunk, zeroPercent, failAtCall, anomalyAtCall, anomalyDelta),
                    in -> IOUtil.readAllToString(in, charset));

            assertEquals(expected, actual, () -> "length=" + data.length + " charset=" + charset + " seed=" + seed + " maxChunk=" + maxChunk + " zero%="
                    + zeroPercent + " failAt=" + failAtCall + " anomalyAt=" + anomalyAtCall + "/" + anomalyDelta);
        }

        private byte[] content(final int kind, final int length, final long seed) {
            final byte[] result = new byte[length];

            if (kind == 0) {
                for (int i = 0; i < length; i++) {
                    result[i] = (byte) (i % 61 == 60 ? '\n' : 'a' + i % 26);
                }
            } else if (kind == 1) {
                // 'a', e-acute (2 bytes), euro sign (3 bytes), U+1F600 (4 bytes), cut at length: sequences straddle every boundary.
                final byte[] pattern = { 'a', (byte) 0xC3, (byte) 0xA9, (byte) 0xE2, (byte) 0x82, (byte) 0xAC, (byte) 0xF0, (byte) 0x9F, (byte) 0x98,
                        (byte) 0x80 };

                for (int i = 0; i < length; i++) {
                    result[i] = pattern[i % pattern.length];
                }
            } else {
                new Random(seed).nextBytes(result);
            }

            return result;
        }

        private List<java.nio.charset.Charset> charsets() {
            final List<java.nio.charset.Charset> charsets = new ArrayList<>(Arrays.asList(java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.charset.StandardCharsets.ISO_8859_1, java.nio.charset.StandardCharsets.UTF_16, null));

            if (java.nio.charset.Charset.isSupported("windows-1252")) {
                charsets.add(java.nio.charset.Charset.forName("windows-1252"));
            }

            return charsets;
        }

        // G032-01: readAllToString(InputStream, Charset) decodes in place from the pooled buffer; pins it to readAllBytes + decode.
        @Test
        void testReadAllToString_matchesReadAllBytesDecodeAroundBufferAndGrowthBoundaries() {
            final int firstGrowth = (int) (bufferSize * 1.75);
            final int secondGrowth = (int) (firstGrowth * 1.75);
            final int[] lengths = { 0, 1, 2, 3, 100, bufferSize / 2, bufferSize - 2, bufferSize - 1, bufferSize, bufferSize + 1, bufferSize + 2,
                    firstGrowth - 1, firstGrowth, firstGrowth + 1, secondGrowth - 1, secondGrowth, secondGrowth + 1 };
            final int[][] deliveries = { { Integer.MAX_VALUE, 0 }, { 1000, 0 }, { 4099, 10 }, { 1, 0 } };
            long seed = 1;

            for (final int length : lengths) {
                for (int kind = 0; kind < 3; kind++) {
                    final byte[] data = content(kind, length, length * 7L + kind);

                    for (final java.nio.charset.Charset charset : charsets()) {
                        for (final int[] delivery : deliveries) {
                            if (delivery[0] == 1 && length > 300) {
                                continue; // one byte per read only for the small sizes
                            }

                            assertSameAsReference(data, charset, seed++, delivery[0], delivery[1], -1, -1, 0);
                        }
                    }
                }
            }
        }

        // G032-01: zero reads (one retried, two in a row end the read), read failures and contract-breaking counts behave as before.
        @Test
        void testReadAllToString_zeroReadsFailuresAndBrokenCountsMatchReadAllBytes() {
            final Random random = new Random(20261008);
            final List<java.nio.charset.Charset> charsets = charsets();
            final int[] anomalyDeltas = { 1, 3, bufferSize, -2, -5, -100_000 };

            for (int i = 0; i < 1500; i++) {
                final int length = random.nextInt(4) == 0 ? random.nextInt(64) : random.nextInt(3 * bufferSize);
                final byte[] data = content(random.nextInt(3), length, random.nextLong());
                final java.nio.charset.Charset charset = charsets.get(random.nextInt(charsets.size()));
                final int maxChunk = random.nextInt(3) == 0 ? Integer.MAX_VALUE : 1 + random.nextInt(9000);
                final int zeroPercent = random.nextInt(3) == 0 ? 0 : random.nextInt(40);
                final int failAtCall = random.nextInt(5) == 0 ? 1 + random.nextInt(6) : -1;
                final int anomalyAtCall = random.nextInt(5) == 0 ? 1 + random.nextInt(6) : -1;
                final int anomalyDelta = anomalyDeltas[random.nextInt(anomalyDeltas.length)];

                assertSameAsReference(data, charset, random.nextLong(), maxChunk, zeroPercent, failAtCall, anomalyAtCall, anomalyDelta);
            }
        }

        /** A caller-supplied single-byte charset whose decoder records the length of the backing array it is handed. */
        private final class RecordingCharset extends java.nio.charset.Charset {
            final List<Integer> seenArrayLengths = new ArrayList<>();

            RecordingCharset() {
                super("x-g032-recording", null);
            }

            @Override
            public boolean contains(final java.nio.charset.Charset other) {
                return other == this;
            }

            @Override
            public java.nio.charset.CharsetDecoder newDecoder() {
                return new java.nio.charset.CharsetDecoder(this, 1f, 1f) {
                    @Override
                    protected java.nio.charset.CoderResult decodeLoop(final java.nio.ByteBuffer in, final java.nio.CharBuffer out) {
                        if (in.hasArray()) {
                            seenArrayLengths.add(in.array().length);
                        }

                        while (in.hasRemaining()) {
                            if (!out.hasRemaining()) {
                                return java.nio.charset.CoderResult.OVERFLOW;
                            }

                            out.put((char) (in.get() & 0xFF));
                        }

                        return java.nio.charset.CoderResult.UNDERFLOW;
                    }
                };
            }

            @Override
            public java.nio.charset.CharsetEncoder newEncoder() {
                throw new UnsupportedOperationException();
            }
        }

        // G032-01: a caller-supplied Charset is still handed a private exact-size array, never the shared pooled buffer.
        @Test
        void testReadAllToString_callerSuppliedCharsetGetsPrivateExactCopy() {
            for (final int length : new int[] { 1, 1000, bufferSize - 1, bufferSize, bufferSize + 1 }) {
                final byte[] data = content(2, length, length);
                final RecordingCharset expectedCharset = new RecordingCharset();
                final RecordingCharset actualCharset = new RecordingCharset();

                final String expected = reference(new java.io.ByteArrayInputStream(data), expectedCharset);
                final String actual = IOUtil.readAllToString(new java.io.ByteArrayInputStream(data), actualCharset);

                assertEquals(expected, actual);
                assertEquals(expectedCharset.seenArrayLengths, actualCharset.seenArrayLengths);
                assertEquals(Collections.singletonList(length), actualCharset.seenArrayLengths);
            }
        }

        // G032-01: argument validation and the empty source are unchanged.
        @Test
        void testReadAllToString_nullSourceAndEmptySource() {
            final IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes((java.io.InputStream) null));
            final IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                    () -> IOUtil.readAllToString((java.io.InputStream) null, java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(expected.getMessage(), actual.getMessage());
            assertEquals(expected.getMessage(),
                    assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((java.io.InputStream) null)).getMessage());

            final String empty = IOUtil.readAllToString(new java.io.ByteArrayInputStream(new byte[0]), null);
            assertEquals("", empty);
            assertNotSame("", empty); // a fresh String, as new String(byte[0], charset) always was

            final byte[] data = content(1, bufferSize - 1, 7);
            final String text = IOUtil.readAllToString(new java.io.ByteArrayInputStream(data));
            // The pooled buffer it was decoded from is wiped and reused by the next read; the result must not change.
            IOUtil.readAllBytes(new java.io.ByteArrayInputStream(content(2, bufferSize - 1, 8)));
            assertEquals(new String(data, java.nio.charset.StandardCharsets.UTF_8), text);
        }
    }

    // ---- perf review 2026-10-08 G032 end ----
    // ---- perf review 2026-10-08 G033 begin ----
    @Nested
    class G033 {
        private java.io.File newFile(final java.io.File dir, final String name, final String content) throws java.io.IOException {
            final java.io.File file = new java.io.File(dir, name);
            java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return file;
        }

        private String read(final java.io.File file) throws java.io.IOException {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        }

        /**
         * The 8.3 short name Windows generated for {@code file}, or {@code null} when the volume generates none (or it
         * cannot be asked).
         */
        private String shortNameOf(final java.io.File file) {
            try {
                final Process process = new ProcessBuilder("cmd", "/c", "for %I in (\"" + file.getAbsolutePath() + "\") do @echo %~sI")
                        .redirectErrorStream(true)
                        .start();
                final String out = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();

                if (process.waitFor() != 0 || out.isEmpty() || out.contains("\n")) {
                    return null;
                }

                final String name = out.substring(out.lastIndexOf('\\') + 1);

                return name.isEmpty() || name.equalsIgnoreCase(file.getName()) ? null : name;
            } catch (final Exception e) {
                return null;
            }
        }

        // G033-01: a file copied into another directory keeps its on-disk name and content, for every source spelling.
        @Test
        void testCopyToDirectory_fileIntoOtherDirectory(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir) throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File sourceDir = new java.io.File(root, "Source Dir");
            final java.io.File destinationDir = new java.io.File(root, "dest");
            assertTrue(sourceDir.mkdirs());
            final java.io.File source = newFile(sourceDir, "Payload.txt", "hello");

            final java.io.File copy = IOUtil.copyToDirectory(source, destinationDir);

            assertEquals(new java.io.File(destinationDir.getCanonicalFile(), "Payload.txt"), copy);
            assertEquals("Payload.txt", copy.getName());
            assertEquals("hello", read(copy));
            assertEquals(source.lastModified(), copy.lastModified());
            assertEquals("hello", read(source));

            // the same file spelled through "." and ".." elements (and, on Windows, in another case) is copied under its
            // on-disk name, never as "Copy of": the destination is another directory
            final java.io.File otherDir = new java.io.File(root, "other");
            final List<java.io.File> spellings = new ArrayList<>();
            spellings.add(new java.io.File(sourceDir, "./Payload.txt"));
            spellings.add(new java.io.File(new java.io.File(sourceDir, ".."), "Source Dir/Payload.txt"));

            if (IOUtil.IS_OS_WINDOWS) {
                spellings.add(new java.io.File(sourceDir.getPath().toUpperCase(java.util.Locale.ROOT), "PAYLOAD.TXT"));
            }

            for (final java.io.File spelling : spellings) {
                final java.io.File again = IOUtil.copyToDirectory(spelling, otherDir, false);

                assertEquals(new java.io.File(otherDir.getCanonicalFile(), "Payload.txt"), again, spelling.getPath());
                assertEquals("Payload.txt", again.getName(), spelling.getPath());
                assertEquals("hello", read(again), spelling.getPath());
                assertTrue(again.delete());
            }
        }

        // G033-01: a file copied into its own directory is "Copy of <name>" for every spelling of source and destination.
        @Test
        void testCopyToDirectory_fileIntoOwnDirectoryIsCopyOfForEverySpelling(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir)
                throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File sourceDir = new java.io.File(root, "averyveryverylongdirectorynamewithashortname");
            final java.io.File sub = new java.io.File(sourceDir, "sub");
            assertTrue(sub.mkdirs());
            final java.io.File source = newFile(sourceDir, "Payload.txt", "hello");

            final List<java.io.File[]> pairs = new ArrayList<>();
            pairs.add(new java.io.File[] { source, sourceDir });
            pairs.add(new java.io.File[] { new java.io.File(sourceDir, "./Payload.txt"), sourceDir });
            pairs.add(new java.io.File[] { new java.io.File(sub, "../Payload.txt"), new java.io.File(sub, "..") });
            pairs.add(new java.io.File[] { source, new java.io.File(sourceDir, ".") });
            pairs.add(new java.io.File[] { new java.io.File(new java.io.File(root, "."), sourceDir.getName() + "/Payload.txt"), sub.getParentFile() });

            if (IOUtil.IS_OS_WINDOWS) {
                pairs.add(new java.io.File[] { new java.io.File(sourceDir.getPath().toUpperCase(java.util.Locale.ROOT), "PAYLOAD.TXT"), sourceDir });
                pairs.add(new java.io.File[] { source, new java.io.File(sourceDir.getPath().toLowerCase(java.util.Locale.ROOT)) });

                final String shortName = shortNameOf(sourceDir);

                if (shortName != null) {
                    pairs.add(new java.io.File[] { new java.io.File(new java.io.File(root, shortName), "Payload.txt"), sourceDir });
                    pairs.add(new java.io.File[] { source, new java.io.File(root, shortName) });
                }
            }

            for (final java.io.File[] pair : pairs) {
                final String label = pair[0].getPath() + " -> " + pair[1].getPath();
                final java.io.File copy = IOUtil.copyToDirectory(pair[0], pair[1], false);

                assertEquals("Copy of Payload.txt", copy.getName(), label);
                assertEquals(sourceDir.getCanonicalFile(), copy.getParentFile().getCanonicalFile(), label);
                assertEquals("hello", read(copy), label);
                assertTrue(copy.delete(), label);
            }

            assertEquals("hello", read(source));
            assertEquals(java.util.Arrays.asList("Payload.txt", "sub"), sortedNames(sourceDir));
        }

        // G033-01: a second copy into its own directory is refused ("already exists"); source and first copy untouched.
        @Test
        void testCopyToDirectory_secondCopyIntoOwnDirectoryIsRefused(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir) throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File source = newFile(root, "doc.txt", "v1");

            final java.io.File first = IOUtil.copyToDirectory(source, root);
            assertEquals("Copy of doc.txt", first.getName());
            java.nio.file.Files.write(first.toPath(), "edited".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            final java.io.IOException failure = assertThrows(java.io.IOException.class, () -> IOUtil.copyToDirectory(source, root));
            assertTrue(failure.getMessage().contains("already exists"), failure.getMessage());
            assertEquals("edited", read(first));
            assertEquals("v1", read(source));
        }

        // G033-01: a directory source is recreated by name with its contents; copying it into itself is refused.
        @Test
        void testCopyToDirectory_directorySource(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir) throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File tree = new java.io.File(root, "tree");
            final java.io.File inner = new java.io.File(tree, "inner");
            assertTrue(inner.mkdirs());
            newFile(tree, "leaf.txt", "leaf");
            newFile(inner, "deep.txt", "deep");
            final java.io.File destinationDir = new java.io.File(root, "dest");

            final java.io.File copy = IOUtil.copyToDirectory(new java.io.File(tree, "."), destinationDir, false);

            assertEquals(new java.io.File(destinationDir.getCanonicalFile(), "tree"), copy);
            assertEquals("leaf", read(new java.io.File(copy, "leaf.txt")));
            assertEquals("deep", read(new java.io.File(copy, "inner/deep.txt")));
            assertEquals(java.util.Arrays.asList("inner", "leaf.txt"), sortedNames(copy));

            assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(tree, inner));
            assertEquals(java.util.Arrays.asList("deep.txt"), sortedNames(inner));
        }

        private List<String> sortedNames(final java.io.File dir) {
            final String[] names = dir.list();
            assertNotNull(names);
            Arrays.sort(names);
            return Arrays.asList(names);
        }
    }

    // ---- perf review 2026-10-08 G033 end ----
    // ---- perf review 2026-10-08 G034 begin ----
    @Nested
    class G034 {

        @org.junit.jupiter.api.io.TempDir
        java.nio.file.Path tempDir;

        private java.io.File dir(final java.io.File parent, final String relative) {
            final java.io.File d = new java.io.File(parent, relative);
            assertTrue(d.isDirectory() || d.mkdirs(), "cannot create " + d);
            return d;
        }

        private java.io.File file(final java.io.File parent, final String relative) throws java.io.IOException {
            final java.io.File f = new java.io.File(parent, relative);
            java.nio.file.Files.write(f.toPath(), relative.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return f;
        }

        /** Files at every depth, empty directories, a chain of directories holding only directories, odd names. */
        private java.io.File mixedTree() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-mixed");
            file(root, "f0.txt");
            file(root, "a b.txt");
            file(root, "noext");
            dir(root, "empty");
            dir(root, "d1/d2");
            dir(root, "d1/d3");
            file(root, "d1/x.txt");
            file(root, "d1/d2/p.txt");
            file(root, "d1/d2/q");
            dir(root, "chain/c1/c2/c3");
            dir(root, "deep/k1");
            file(root, "deep/only.txt");
            file(root, "deep/k1/leaf.txt");
            return root;
        }

        /** Depth-first, pre-order reference of the documented traversal for a tree without links. */
        private void reference(final java.io.File directory, final boolean recursively, final boolean directoriesOnly, final List<java.io.File> out) {
            final java.io.File[] subFiles = directory.listFiles();

            if (subFiles == null) {
                return;
            }

            for (final java.io.File f : subFiles) {
                if (f.isDirectory() == directoriesOnly) {
                    out.add(f);
                }

                if (recursively && f.isDirectory()) {
                    reference(f, recursively, directoriesOnly, out);
                }
            }
        }

        private List<java.io.File> reference(final java.io.File root, final boolean recursively, final boolean directoriesOnly) {
            final List<java.io.File> out = new ArrayList<>();
            reference(root, recursively, directoriesOnly, out);
            return out;
        }

        // G034-01: listFiles(.., true, true) asks isDirectory() once per entry; order, content and traversal unchanged
        @Test
        public void testListFiles_excludeDirectoryRecursiveKeepsPreOrderAndContent() throws java.io.IOException {
            final java.io.File root = mixedTree();
            final List<java.io.File> expected = reference(root, true, false);

            assertEquals(8, expected.size());
            assertEquals(expected, IOUtil.listFiles(root, true, true));
            // the same selection through a caller filter (the general path) and through the lazy twin
            assertEquals(expected, IOUtil.listFiles(root, true, (parent, f) -> !f.isDirectory()));
            assertEquals(expected, IOUtil.walk(root, true, true).toList());

            final List<java.io.File> deepOnly = IOUtil.listFiles(new java.io.File(root, "deep"), true, true);
            assertEquals(2, deepOnly.size());
            assertEquals(reference(new java.io.File(root, "deep"), true, false), deepOnly);
            // a tree holding directories only yields nothing, yet every level is descended into
            assertEquals(new ArrayList<>(), IOUtil.listFiles(new java.io.File(root, "chain"), true, true));
        }

        // G034-01: listDirectories(.., true) asks isDirectory() once per entry; order, content and traversal unchanged
        @Test
        public void testListDirectories_recursiveKeepsPreOrderAndContent() throws java.io.IOException {
            final java.io.File root = mixedTree();
            final List<java.io.File> expected = reference(root, true, true);

            assertEquals(10, expected.size());
            assertEquals(expected, IOUtil.listDirectories(root, true));
            assertEquals(expected, IOUtil.listFiles(root, true, (parent, f) -> f.isDirectory()));
            // every directory of the eager all-entries listing, in the same relative order
            final List<java.io.File> all = IOUtil.listFiles(root, true, false);
            final List<java.io.File> dirsOfAll = new ArrayList<>();

            for (final java.io.File f : all) {
                if (f.isDirectory()) {
                    dirsOfAll.add(f);
                }
            }

            assertEquals(expected, dirsOfAll);
            assertEquals(Arrays.asList(new java.io.File(root, "chain/c1"), new java.io.File(root, "chain/c1/c2"), new java.io.File(root, "chain/c1/c2/c3")),
                    IOUtil.listDirectories(new java.io.File(root, "chain"), true));
        }

        // G034-01: the non-recursive selections by kind and the root edge cases answer exactly as before
        @Test
        public void testListFiles_byKindNonRecursiveAndEdgeRoots() throws java.io.IOException {
            final java.io.File root = mixedTree();

            assertEquals(reference(root, false, false), IOUtil.listFiles(root, false, true));
            assertEquals(3, IOUtil.listFiles(root, false, true).size());
            assertEquals(reference(root, false, true), IOUtil.listDirectories(root));
            assertEquals(reference(root, false, true), IOUtil.listDirectories(root, false));
            assertEquals(4, IOUtil.listDirectories(root).size());

            // fresh, modifiable ArrayList results, also when empty
            final java.io.File empty = new java.io.File(root, "empty");
            final List<java.io.File> r1 = IOUtil.listFiles(empty, true, true);
            final List<java.io.File> r2 = IOUtil.listFiles(empty, true, true);
            assertEquals(ArrayList.class, r1.getClass());
            assertTrue(r1.isEmpty());
            assertNotSame(r1, r2);
            r1.add(empty);
            assertEquals(1, r1.size());
            assertEquals(ArrayList.class, IOUtil.listDirectories(empty, true).getClass());
            assertTrue(IOUtil.listDirectories(empty, true).isEmpty());

            // null and missing roots: empty results; a file root: the wrong-kind IllegalArgumentException
            assertTrue(IOUtil.listFiles(null, true, true).isEmpty());
            assertTrue(IOUtil.listDirectories(null, true).isEmpty());
            final java.io.File missing = new java.io.File(root, "missing");
            assertTrue(IOUtil.listFiles(missing, true, true).isEmpty());
            assertTrue(IOUtil.listDirectories(missing, true).isEmpty());
            final java.io.File aFile = new java.io.File(root, "f0.txt");
            final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(aFile, true, true));
            assertEquals("'" + aFile.getAbsolutePath() + "' is not a directory", e1.getMessage());
            final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.listDirectories(aFile, true));
            assertEquals("'" + aFile.getAbsolutePath() + "' is not a directory", e2.getMessage());
            assertThrows(IllegalArgumentException.class,
                    () -> IOUtil.listFiles(root, true, (Throwables.BiPredicate<java.io.File, java.io.File, RuntimeException>) null));
        }

        // G034-01: a linked directory (junction on Windows, symbolic link elsewhere) is selected by kind but never descended into
        @Test
        public void testListFiles_byKindNeverDescendsIntoALinkedDirectory() throws Exception {
            final java.io.File root = dir(tempDir.toFile(), "g034-links");
            final java.io.File target = dir(tempDir.toFile(), "g034-link-target");
            file(target, "inside.txt");
            dir(target, "innerDir");
            file(root, "own.txt");
            dir(root, "sub");
            file(root, "sub/a.txt");

            final java.io.File link = new java.io.File(new java.io.File(root, "sub"), "link");

            if (IOUtil.IS_OS_WINDOWS) {
                final Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath()).redirectErrorStream(true)
                        .start();
                p.getInputStream().readAllBytes();
                org.junit.jupiter.api.Assumptions.assumeTrue(
                        p.waitFor() == 0 && java.nio.file.Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS),
                        "mklink /J is not available here");
            } else {
                try {
                    java.nio.file.Files.createSymbolicLink(link.toPath(), target.toPath());
                } catch (final java.io.IOException | UnsupportedOperationException e) {
                    org.junit.jupiter.api.Assumptions.abort("cannot create a symbolic link here: " + e);
                }
            }

            try {
                assertTrue(link.isDirectory(), "the link resolves to a directory");
                // the order of root's own two entries is the file system's listing order: compared as a set here
                assertEquals(new HashSet<>(Arrays.asList(new java.io.File(root, "own.txt"), new java.io.File(root, "sub/a.txt"))),
                        new HashSet<>(IOUtil.listFiles(root, true, true)));
                assertEquals(2, IOUtil.listFiles(root, true, true).size());
                assertEquals(Arrays.asList(new java.io.File(root, "sub"), link), IOUtil.listDirectories(root, true));
                assertEquals(IOUtil.listFiles(root, true, (parent, f) -> !f.isDirectory()), IOUtil.listFiles(root, true, true));
                assertEquals(IOUtil.listFiles(root, true, (parent, f) -> f.isDirectory()), IOUtil.listDirectories(root, true));
                assertEquals(IOUtil.walk(root, true, true).toList(), IOUtil.listFiles(root, true, true));
                // a linked directory handed in as the root itself is followed and listed
                assertEquals(Arrays.asList(new java.io.File(link, "inside.txt")), IOUtil.listFiles(link, true, true));
                assertEquals(Arrays.asList(new java.io.File(link, "innerDir")), IOUtil.listDirectories(link, true));
            } finally {
                // removes the link entry only (a junction is unlinked by Files.delete, its target is untouched)
                java.nio.file.Files.deleteIfExists(link.toPath());
            }

            assertTrue(new java.io.File(target, "inside.txt").isFile(), "the link target is untouched");
        }

        /** A {@code File} that counts its {@code length()} lookups (a file-system call each). */
        private static final class LengthCountingFile extends java.io.File {
            private static final long serialVersionUID = 1L;

            private int lengthCalls;

            LengthCountingFile(final java.io.File file) {
                super(file.getPath());
            }

            @Override
            public long length() {
                lengthCalls++;
                return super.length();
            }

            int takeLengthCalls() {
                final int result = lengthCalls;
                lengthCalls = 0;
                return result;
            }
        }

        private static final String G034_TEXT = "alpha\nbeta\r\ngamma\n\ndelta";

        private java.io.File gzipOf(final java.io.File target, final String content) throws java.io.IOException {
            try (java.io.OutputStream os = new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(target))) {
                os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            return target;
        }

        // G034-02: openFile looks the file length up only for a line-count sample; the read family returns the same content
        @Test
        public void testReadFamily_plainGzipAndZipSourcesReadTheSameContent() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-read");
            final java.io.File plain = new java.io.File(root, "plain.txt");
            java.nio.file.Files.write(plain.toPath(), G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final java.io.File gz = gzipOf(new java.io.File(root, "plain.txt.gz"), G034_TEXT);
            final java.io.File zip = new java.io.File(root, "one.zip");

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zip))) {
                zos.putNextEntry(new java.util.zip.ZipEntry("d/"));
                zos.closeEntry();
                zos.putNextEntry(new java.util.zip.ZipEntry("d/a.txt"));
                zos.write(G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            final List<String> expectedLines = Arrays.asList("alpha", "beta", "gamma", "", "delta");

            for (final java.io.File source : new java.io.File[] { plain, gz, zip }) {
                final String name = source.getName();
                assertArrayEquals(G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8), IOUtil.readAllBytes(source), name);
                assertEquals(G034_TEXT, IOUtil.readAllToString(source), name);
                assertEquals(G034_TEXT, new String(IOUtil.readAllChars(source)), name);
                assertEquals(expectedLines, IOUtil.readAllLines(source), name);
                assertEquals(Arrays.asList("beta", "gamma"), IOUtil.readLines(source, 1, 2), name);
                assertEquals("alpha", IOUtil.readFirstLine(source), name);
                assertEquals("delta", IOUtil.readLastLine(source), name);
                assertEquals("gamma", IOUtil.readLine(source, 2), name);
                assertArrayEquals(Arrays.copyOfRange(G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8), 1, 8), IOUtil.readBytes(source, 1, 7), name);

                final List<String> lines = new ArrayList<>();
                IOUtil.forEachLine(source, lines::add);
                assertEquals(expectedLines, lines, name);

                final byte[] buf = new byte[64];
                assertEquals(G034_TEXT.length(), IOUtil.read(source, buf), name);
            }

            // an empty plain file: empty results, not end-of-stream errors
            final java.io.File empty = new java.io.File(root, "empty.txt");
            java.nio.file.Files.write(empty.toPath(), new byte[0]);
            assertEquals(0, IOUtil.readAllBytes(empty).length);
            assertTrue(IOUtil.readAllLines(empty).isEmpty());
            assertNull(IOUtil.readFirstLine(empty));
            assertEquals(-1, IOUtil.read(empty, new byte[4]));
        }

        // G034-02: missing, directory, empty-gzip and not-gzip sources still fail with the same exception, cause and message
        @Test
        public void testReadFamily_failedOpensKeepTheirExceptions() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-read-fail");
            final java.io.File missing = new java.io.File(root, "missing.txt");
            final java.io.File missingGz = new java.io.File(root, "missing.gz");
            final java.io.File directory = dir(root, "adir");
            final java.io.File directoryGz = dir(root, "adir.gz");
            final java.io.File emptyGz = new java.io.File(root, "empty.gz");
            java.nio.file.Files.write(emptyGz.toPath(), new byte[0]);
            final java.io.File notGzip = new java.io.File(root, "notgzip.gz");
            java.nio.file.Files.write(notGzip.toPath(), "plain".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            for (final java.io.File absent : new java.io.File[] { missing, missingGz }) {
                final com.landawn.abacus.exception.UncheckedIOException e1 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> IOUtil.readAllBytes(absent));
                assertEquals(java.io.FileNotFoundException.class, e1.getCause().getClass());
                final com.landawn.abacus.exception.UncheckedIOException e2 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> IOUtil.readAllLines(absent));
                assertEquals(java.io.FileNotFoundException.class, e2.getCause().getClass());
                assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.read(absent, new byte[4]));
                final com.landawn.abacus.exception.UncheckedIOException e3 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> IOUtil.forEachLine(absent, line -> {
                        }));
                assertEquals("'" + absent.getAbsolutePath() + "' does not exist", e3.getCause().getMessage());
            }

            for (final java.io.File wrongKind : new java.io.File[] { directory, directoryGz }) {
                final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes(wrongKind));
                assertEquals("'" + wrongKind.getAbsolutePath() + "' is a directory, not a file", e1.getMessage());
                assertEquals(java.io.FileNotFoundException.class, e1.getCause().getClass());
                final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines(wrongKind));
                assertEquals("'" + wrongKind.getAbsolutePath() + "' is a directory, not a file", e2.getMessage());
                assertThrows(IllegalArgumentException.class, () -> IOUtil.read(wrongKind, new byte[4]));
            }

            final com.landawn.abacus.exception.UncheckedIOException e4 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                    () -> IOUtil.readAllLines(emptyGz));
            assertEquals(java.io.EOFException.class, e4.getCause().getClass());
            assertThrows(java.io.EOFException.class, () -> IOUtil.read(emptyGz, new byte[4]));

            final com.landawn.abacus.exception.UncheckedIOException e5 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                    () -> IOUtil.readAllBytes(notGzip));
            assertEquals(java.util.zip.ZipException.class, e5.getCause().getClass());
            assertEquals("Not in GZIP format", e5.getCause().getMessage());
            assertThrows(java.util.zip.ZipException.class, () -> IOUtil.read(notGzip, new byte[4]));
        }

        // G034-02: the sampled line-count estimate of splitByLine still scales the sample by the file length
        @Test
        public void testSplitByLine_sampledEstimateStillScalesByTheFileLength() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-split");
            final java.io.File source = new java.io.File(root, "big.txt");
            final int lineCount = 120_000;
            final StringBuilder sb = new StringBuilder(lineCount * 42);

            for (int i = 0; i < lineCount; i++) {
                sb.append("line ").append(i).append(" of the sampled line-count estimate\n");
            }

            java.nio.file.Files.write(source.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // larger than the 4 MB minimum sample and the 10,000-line sample, so the estimate is scaled, not counted
            assertTrue(source.length() > 4L * 1024 * 1024, "the source must exceed the minimum sample: " + source.length());

            final java.io.File destination = dir(root, "parts");
            IOUtil.splitByLine(source, 4, destination);

            final String[] names = destination.list();
            assertNotNull(names);
            Arrays.sort(names);
            assertEquals(Arrays.asList("big_0001.txt", "big_0002.txt", "big_0003.txt", "big_0004.txt"), Arrays.asList(names));

            long total = 0;
            long biggest = 0;

            for (final String name : names) {
                final long n = IOUtil.readAllLines(new java.io.File(destination, name)).size();
                total += n;
                biggest = Math.max(biggest, n);
            }

            assertEquals(lineCount, total, "no line may be lost");
            // Unscaled (the sample count alone, ~100,000 lines here) would leave ~45,000 lines in the last part.
            assertTrue(biggest <= lineCount / 4 + lineCount / 50, "parts are unbalanced; biggest = " + biggest);
        }

        // G034-02: perf pin - only the presize hint (readAllBytes/readAllToString of a plain file) and the line-count sample
        // look the length up. RED on base by design: there every read looked it up once more, for nothing.
        @Test
        public void testReadFamily_looksTheFileLengthUpOnlyWhereItIsUsed() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-length");
            final java.io.File plainFile = new java.io.File(root, "plain.txt");
            java.nio.file.Files.write(plainFile.toPath(), G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final LengthCountingFile plain = new LengthCountingFile(plainFile);
            final LengthCountingFile gz = new LengthCountingFile(gzipOf(new java.io.File(root, "plain.txt.gz"), G034_TEXT));

            for (final LengthCountingFile source : Arrays.asList(plain, gz)) {
                final String name = source.getName();
                source.takeLengthCalls();
                IOUtil.readAllLines(source);
                assertEquals(0, source.takeLengthCalls(), name + " readAllLines");
                IOUtil.readFirstLine(source);
                assertEquals(0, source.takeLengthCalls(), name + " readFirstLine");
                IOUtil.readAllChars(source);
                assertEquals(0, source.takeLengthCalls(), name + " readAllChars");
                IOUtil.read(source, new byte[8]);
                assertEquals(0, source.takeLengthCalls(), name + " read(File, byte[])");
                IOUtil.forEachLine(source, line -> {
                });
                assertEquals(0, source.takeLengthCalls(), name + " forEachLine");
            }

            // the presize hint of the whole-file byte readers is a real use of the length (plain files only)
            IOUtil.readAllBytes(plain);
            assertEquals(1, plain.takeLengthCalls(), "readAllBytes presize hint");
            IOUtil.readAllToString(plain);
            assertEquals(1, plain.takeLengthCalls(), "readAllToString presize hint");
            IOUtil.readAllBytes(gz);
            assertEquals(0, gz.takeLengthCalls(), "a decompressed stream has no length to presize from");
        }
    }

    // ---- perf review 2026-10-08 G034 end ----
    // ---- perf review 2026-10-08 G035 begin ----
    @Nested
    class G035 {

        private List<Integer> scrambledIds(final int count, final long seed) {
            final List<Integer> ids = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                ids.add(i);
            }

            Collections.shuffle(ids, new Random(seed));

            return ids;
        }

        /**
         * A reference BiMap whose backing maps are far larger than either builder sizing (the current 32-bucket tables or
         * the default 16-bucket ones recommended by G035-02), so a matching result cannot depend on the table capacity.
         */
        private BiMap<String, Integer> referenceBiMap() {
            return new BiMap<>(() -> new LinkedHashMap<>(1024), () -> new LinkedHashMap<>(1024));
        }

        /**
         * @param expected the expected forward mappings, in the expected iteration order
         * @param expectedInverse the expected value-to-key mappings in the inverse view's iteration order (the reverse
         *        map's own order, which differs from the forward value order once a key's value has been replaced)
         * @param actual the map under test
         */
        private void assertSameContentAndOrder(final Map<String, Integer> expected, final Map<Integer, String> expectedInverse,
                final ImmutableBiMap<String, Integer> actual) {
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
            assertEquals(new ArrayList<>(expected.values()), new ArrayList<>(actual.values()));
            assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.isEmpty(), actual.isEmpty());
            assertEquals(expected, actual);
            assertTrue(actual.equals(expected));
            assertEquals(expected.hashCode(), actual.hashCode());
            assertEquals(expected.toString(), actual.toString());

            for (final Map.Entry<String, Integer> entry : expected.entrySet()) {
                assertEquals(entry.getValue(), actual.get(entry.getKey()));
                assertTrue(actual.containsKey(entry.getKey()));
                assertTrue(actual.containsValue(entry.getValue()));
                assertEquals(entry.getKey(), actual.getByValue(entry.getValue()));
            }

            assertNull(actual.get("absent"));
            assertNull(actual.getByValue(-1));
            assertNull(actual.getByValue(null));

            final ImmutableBiMap<Integer, String> inverse = actual.inverse();
            assertEquals(new ArrayList<>(expectedInverse.keySet()), new ArrayList<>(inverse.keySet()));
            assertEquals(new ArrayList<>(expectedInverse.values()), new ArrayList<>(inverse.values()));
            assertEquals(new ArrayList<>(expectedInverse.entrySet()), new ArrayList<>(inverse.entrySet()));
            assertEquals(expectedInverse, inverse);
            assertSame(actual, inverse.inverse());
            assertSame(inverse, actual.inverse());
            // A consumed no-argument builder owns its storage, so copyOf hands the same instance back.
            assertSame(actual, ImmutableBiMap.copyOf(actual));
        }

        // G035-02 (OPEN): pins builder() order, lookups, views and inverse across the 12/13, 24/25 and 48/49 resize thresholds
        @Test
        void testBuilder_encounterOrderAcrossResizeThresholds() {
            for (final int count : new int[] { 0, 1, 2, 3, 6, 11, 12, 13, 16, 23, 24, 25, 48, 49, 100, 1000 }) {
                final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
                final Map<String, Integer> expected = new LinkedHashMap<>();
                final Map<Integer, String> expectedInverse = new LinkedHashMap<>();

                for (final Integer id : scrambledIds(count, 31L * count + 7)) {
                    assertSame(builder, builder.put("k" + id, id * 7 + 1));
                    expected.put("k" + id, id * 7 + 1);
                    expectedInverse.put(id * 7 + 1, "k" + id);
                }

                assertSameContentAndOrder(expected, expectedInverse, builder.build());
            }
        }

        // G035-02 (OPEN): pins builder() put/putAll semantics (replace, duplicate value, null, partial failure) vs a reference
        @Test
        void testBuilder_putSemanticsMatchLargeCapacityReference() {
            final Random random = new Random(20261008L);

            for (int round = 0; round < 300; round++) {
                final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
                final BiMap<String, Integer> reference = referenceBiMap();
                final int operations = random.nextInt(70);

                for (int i = 0; i < operations; i++) {
                    final String key = random.nextInt(12) == 0 ? null : "k" + random.nextInt(30);
                    final Integer value = random.nextInt(12) == 0 ? null : random.nextInt(40);
                    String expectedFailure = null;

                    try {
                        reference.put(key, value);
                    } catch (final IllegalArgumentException e) {
                        expectedFailure = e.getMessage();
                    }

                    if (expectedFailure == null) {
                        assertSame(builder, builder.put(key, value));
                    } else {
                        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> builder.put(key, value));
                        assertEquals(expectedFailure, e.getMessage());
                    }
                }

                final Map<String, Integer> batch = new LinkedHashMap<>();

                for (int i = 0, batchSize = random.nextInt(20); i < batchSize; i++) {
                    batch.put("b" + random.nextInt(30), random.nextInt(60));
                }

                String expectedFailure = null;

                try {
                    reference.putAll(batch);
                } catch (final IllegalArgumentException e) {
                    expectedFailure = e.getMessage();
                }

                if (expectedFailure == null) {
                    assertSame(builder, builder.putAll(batch));
                } else {
                    final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> builder.putAll(batch));
                    assertEquals(expectedFailure, e.getMessage());
                }

                final ImmutableBiMap<String, Integer> built = builder.build();
                assertSameContentAndOrder(new LinkedHashMap<>(reference), new LinkedHashMap<>(reference.inverse()), built);
                assertThrows(IllegalStateException.class, () -> builder.put("late", -2));
                assertEquals(reference.size(), built.size());
            }
        }

        // G035-01 (OPEN): pins isEmpty() of every view shape (incl. N.slice's Slice and a stale subList's CME) for the candidate
        @Test
        void testIsEmpty_viewsAgreeWithSizeAcrossBackings() {
            final List<Collection<?>> nonEmpty = new ArrayList<>();
            final List<Collection<?>> empty = new ArrayList<>();

            final ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4);
            nonEmpty.add(list);
            nonEmpty.add(list.subList(1, 2));
            nonEmpty.add(list.reversed());
            nonEmpty.add(list.reversed().subList(0, 1));
            nonEmpty.add(ImmutableList.copyOf(Arrays.asList(5, 6)));
            nonEmpty.add(ImmutableList.wrap(new LinkedList<>(Arrays.asList(1))));
            empty.add(list.subList(2, 2));
            empty.add(list.reversed().subList(3, 3));
            empty.add(ImmutableList.empty());
            empty.add(ImmutableList.copyOf(new ArrayList<Integer>()));

            final ImmutableNavigableSet<Integer> navigableSet = ImmutableNavigableSet.copyOf(new TreeSet<>(Arrays.asList(10, 20, 30, 40)));
            nonEmpty.add(navigableSet);
            nonEmpty.add(navigableSet.headSet(20, true));
            nonEmpty.add(navigableSet.tailSet(40, true));
            nonEmpty.add(navigableSet.subSet(15, true, 25, false));
            nonEmpty.add(navigableSet.descendingSet());
            empty.add(navigableSet.headSet(10, false));
            empty.add(navigableSet.tailSet(40, false));
            empty.add(navigableSet.subSet(21, true, 29, true));
            empty.add(navigableSet.descendingSet().headSet(40, false));

            final ImmutableNavigableMap<Integer, String> navigableMap = ImmutableNavigableMap.copyOf(new TreeMap<>(Map.of(1, "a", 3, "c", 5, "e")));
            nonEmpty.add(navigableMap.navigableKeySet());
            nonEmpty.add(navigableMap.headMap(3, true).navigableKeySet());
            nonEmpty.add(navigableMap.tailMap(5, true).descendingKeySet());
            empty.add(navigableMap.headMap(1, false).navigableKeySet());
            empty.add(navigableMap.subMap(2, true, 2, true).descendingKeySet());

            nonEmpty.add(ImmutableSet.of("x"));
            nonEmpty.add(ImmutableSet.wrap(new HashSet<>(Arrays.asList("y"))));
            empty.add(ImmutableSet.empty());
            empty.add(ImmutableCollection.wrap(null));

            final LinkedHashSet<String> linked = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
            nonEmpty.add(CommonUtil.slice(linked, 0, 2));
            nonEmpty.add(CommonUtil.slice(linked, 2, 3));
            empty.add(CommonUtil.slice(linked, 1, 1));

            final java.util.concurrent.ConcurrentLinkedQueue<Integer> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();
            final ImmutableCollection<Integer> queueView = ImmutableCollection.wrap(queue);
            empty.add(queueView);

            for (final Collection<?> c : nonEmpty) {
                assertFalse(c.isEmpty(), () -> c.getClass() + " " + c);
                assertTrue(c.size() > 0, () -> c.getClass() + " " + c);
            }

            for (final Collection<?> c : empty) {
                assertTrue(c.isEmpty(), () -> c.getClass() + " " + c);
                assertEquals(0, c.size(), () -> c.getClass() + " " + c);
            }

            // Live views follow their backing collection.
            queue.add(7);
            assertFalse(queueView.isEmpty());
            queue.clear();
            assertTrue(queueView.isEmpty());

            final List<Integer> backing = new ArrayList<>();
            final ImmutableList<Integer> live = ImmutableList.wrap(backing);
            assertTrue(live.isEmpty());
            assertTrue(live.reversed().isEmpty());
            backing.add(1);
            assertFalse(live.isEmpty());
            assertFalse(live.reversed().isEmpty());

            // A stale sublist of a wrap()-backed ArrayList reports the structural change from isEmpty() as well.
            final List<Integer> source = new ArrayList<>(Arrays.asList(1, 2, 3));
            final ImmutableList<Integer> staleSubList = ImmutableList.wrap(source).subList(0, 2);
            source.add(4);
            assertThrows(java.util.ConcurrentModificationException.class, staleSubList::isEmpty);
        }
    }

    // ---- perf review 2026-10-08 G035 end ----
    // ---- perf review 2026-10-08 G037 begin ----
    @Nested
    class G037 {
        private final Class<?>[] primitiveTypes = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class };

        // Codes 0..7 select a distinct value per type; float/double include -0, NaN, a NaN with payload and infinities.
        private Object primitiveArray(final Class<?> type, final int... codes) {
            final float[] floats = { 0f, -0f, Float.NaN, Float.intBitsToFloat(0x7fa12345), Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1f, -2f };
            final double[] doubles = { 0d, -0d, Double.NaN, Double.longBitsToDouble(0x7ff0123412341234L), Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    1d, -2d };
            final Object result = java.lang.reflect.Array.newInstance(type, codes.length);

            for (int i = 0; i < codes.length; i++) {
                final int code = codes[i];

                if (type == boolean.class) {
                    java.lang.reflect.Array.setBoolean(result, i, (code & 1) != 0);
                } else if (type == char.class) {
                    java.lang.reflect.Array.setChar(result, i, (char) ('a' + code));
                } else if (type == byte.class) {
                    java.lang.reflect.Array.setByte(result, i, (byte) (code - 3));
                } else if (type == short.class) {
                    java.lang.reflect.Array.setShort(result, i, (short) (code * 1000));
                } else if (type == int.class) {
                    java.lang.reflect.Array.setInt(result, i, code * 100_000);
                } else if (type == long.class) {
                    java.lang.reflect.Array.setLong(result, i, code * 0x100000001L);
                } else if (type == float.class) {
                    java.lang.reflect.Array.setFloat(result, i, floats[code & 7]);
                } else {
                    java.lang.reflect.Array.setDouble(result, i, doubles[code & 7]);
                }
            }

            return result;
        }

        private int search(final Class<?> type, final Object source, final int bound, final Object pattern, final int offset, final int size,
                final boolean reverse) throws Exception {
            final Class<?> arrayType = java.lang.reflect.Array.newInstance(type, 0).getClass();
            final java.lang.reflect.Method method = Index.class.getMethod(reverse ? "lastOfSubArray" : "ofSubArray", arrayType, int.class, arrayType, int.class,
                    int.class);

            try {
                return ((u.OptionalInt) method.invoke(null, source, bound, pattern, offset, size)).orElse(-1);
            } catch (final java.lang.reflect.InvocationTargetException e) {
                throw e.getCause() instanceof Exception cause ? cause : e;
            }
        }

        // Plain candidate-by-candidate scan; boxed equals has the same canonical-NaN and signed-zero rules as N.equals.
        private int naive(final Object source, final int bound, final Object pattern, final int offset, final int size, final boolean reverse) {
            final int length = java.lang.reflect.Array.getLength(source);
            final int first = reverse ? Math.min(bound, length - size) : Math.max(bound, 0);

            for (int i = first; i >= 0 && i <= length - size; i += reverse ? -1 : 1) {
                boolean matches = true;

                for (int j = 0; j < size && matches; j++) {
                    matches = java.util.Objects.equals(java.lang.reflect.Array.get(source, i + j), java.lang.reflect.Array.get(pattern, offset + j));
                }

                if (matches) {
                    return i;
                }
            }

            return -1;
        }

        private void assertAllBounds(final Class<?> type, final int[] sourceCodes, final int[] patternCodes, final int offset, final int size)
                throws Exception {
            final Object source = primitiveArray(type, sourceCodes);
            final Object pattern = primitiveArray(type, patternCodes);

            for (int bound = -2; bound <= sourceCodes.length + 1; bound++) {
                for (final boolean reverse : new boolean[] { false, true }) {
                    if (reverse && bound < 0) {
                        assertEquals(-1, search(type, source, bound, pattern, offset, size, true));
                    } else {
                        assertEquals(naive(source, bound, pattern, offset, size, reverse), search(type, source, bound, pattern, offset, size, reverse),
                                () -> type + " " + Arrays.toString(sourceCodes) + " / " + Arrays.toString(patternCodes) + " [" + offset + ", +" + size
                                        + ") reverse=" + reverse);
                    }
                }
            }
        }

        // G037-01: the scan for the pattern's first element must decide every edge exactly like a candidate-by-candidate scan
        @Test
        void testOfSubArray_firstElementScanEdges() throws Exception {
            final int[][] sources = { { 1, 2, 3, 4, 5, 6 }, // first element absent
                    { 1, 2, 3, 4, 5, 0 }, // first element only at the last index, past the last possible start
                    { 1, 2, 3, 4, 0, 6 }, // first element at the last possible start, tail differs
                    { 1, 2, 3, 4, 0, 7 }, // match at the last possible start
                    { 0, 7, 0, 7, 0, 7 }, // matches at the first position and repeatedly
                    { 0, 0, 0, 0, 0, 7 }, // runs of the first element before the match
                    { 0, 0, 7, 0, 7, 7 }, { 7 }, {} };
            final int[][] patterns = { { 0, 7 }, { 0 }, { 7 }, { 3, 0, 7 }, { 0, 0, 7 }, { 5, 0, 7, 4 } };

            for (final Class<?> type : primitiveTypes) {
                for (final int[] sourceCodes : sources) {
                    for (final int[] patternCodes : patterns) {
                        for (int offset = 0; offset < patternCodes.length; offset++) {
                            for (int size = 1; offset + size <= patternCodes.length; size++) {
                                assertAllBounds(type, sourceCodes, patternCodes, offset, size);
                            }
                        }
                    }
                }
            }

            assertEquals(4, Index.ofSubArray(new int[] { 1, 2, 3, 4, 0, 7 }, new int[] { 0, 7 }).orElse(-1));
            assertEquals(-1, Index.ofSubArray(new long[] { 1, 2, 3, 4, 5, 0 }, new long[] { 0, 7 }).orElse(-1));
            assertEquals(2, Index.ofSubArray("xyabcab".toCharArray(), 1, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(5, Index.ofSubArray("xyabcab".toCharArray(), 3, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(-1, Index.ofSubArray("xyabcab".toCharArray(), 6, "zab".toCharArray(), 1, 2).orElse(-1));
        }

        // G037-01: same edges searching backwards
        @Test
        void testLastOfSubArray_firstElementScanEdges() throws Exception {
            for (final Class<?> type : primitiveTypes) {
                assertAllBounds(type, new int[] { 7, 0, 7, 0, 7, 0 }, new int[] { 0, 7 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 4, 5 }, new int[] { 0, 1 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 4, 5 }, new int[] { 9, 0, 2 }, 1, 2);
                assertAllBounds(type, new int[] { 6, 6, 6, 6, 6, 6 }, new int[] { 6, 6, 6 }, 0, 3);
                assertAllBounds(type, new int[] { 1, 2, 3 }, new int[] { 4 }, 0, 1);
            }

            assertEquals(5, Index.lastOfSubArray("xyabcab".toCharArray(), "ab".toCharArray()).orElse(-1));
            assertEquals(2, Index.lastOfSubArray("xyabcab".toCharArray(), 3, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(-1, Index.lastOfSubArray("xyabcab".toCharArray(), 1, "zab".toCharArray(), 1, 2).orElse(-1));
            assertEquals(0, Index.lastOfSubArray(new double[] { -0.0, 1, 0.0, 1 }, 1, new double[] { -0.0, 1 }, 0, 2).orElse(-1));
        }

        // G037-01: low-entropy data (the first element matches often), both sides of the 16-element fallback threshold
        @Test
        void testOfSubArray_lowEntropyAgreesWithNaiveScan() throws Exception {
            final Random random = new Random(20261008);

            for (final Class<?> type : primitiveTypes) {
                for (int trial = 0; trial < 120; trial++) {
                    final int alphabet = 1 + trial % 4;
                    final int[] sourceCodes = new int[random.nextInt(90)];
                    final int[] patternCodes = new int[1 + random.nextInt(trial % 3 == 0 ? 24 : 6)];

                    for (int i = 0; i < sourceCodes.length; i++) {
                        sourceCodes[i] = random.nextInt(alphabet);
                    }

                    for (int i = 0; i < patternCodes.length; i++) {
                        patternCodes[i] = random.nextInt(alphabet);
                    }

                    final int offset = random.nextInt(patternCodes.length);
                    final int size = 1 + random.nextInt(patternCodes.length - offset);

                    if (size <= sourceCodes.length && trial % 2 == 0) {
                        System.arraycopy(patternCodes, offset, sourceCodes, random.nextInt(sourceCodes.length - size + 1), size);
                    }

                    assertAllBounds(type, sourceCodes, patternCodes, offset, size);
                }
            }
        }

        // G037-01: the linear-work fallback is still reached when every candidate passes the first-element check
        @Test
        void testLastOfSubArray_repeatedPrefixFallbackAfterFirstElementHits() throws Exception {
            for (final Class<?> type : primitiveTypes) {
                final int[] sourceCodes = new int[3000];
                final int[] patternCodes = new int[260];
                patternCodes[0] = 7; // outside the slice [1, 258)
                patternCodes[257] = 1;
                sourceCodes[1200] = 1;
                sourceCodes[2900] = 1;
                final Object source = primitiveArray(type, sourceCodes);
                final Object pattern = primitiveArray(type, patternCodes);

                for (final int bound : new int[] { -1, 0, 900, 944, 945, 946, 2643, 2644, 2645, 3000, Integer.MAX_VALUE, Integer.MIN_VALUE }) {
                    for (final boolean reverse : new boolean[] { false, true }) {
                        final int expected = reverse && bound < 0 ? -1 : naive(source, bound, pattern, 1, 257, reverse);
                        assertEquals(expected, search(type, source, bound, pattern, 1, 257, reverse), type + " bound=" + bound + " reverse=" + reverse);
                    }
                }
            }

            final int[] zeros = new int[5000];
            zeros[4000] = 1;
            final int[] run = new int[100];
            run[99] = 1;
            assertEquals(3901, Index.ofSubArray(zeros, run).orElse(-1));
            assertEquals(3901, Index.lastOfSubArray(zeros, run).orElse(-1));
            assertEquals(-1, Index.ofSubArray(zeros, 3902, run).orElse(-1));
            assertEquals(-1, Index.lastOfSubArray(zeros, 3900, run).orElse(-1));
        }

        // G037-01: NaN (any payload), signed zeros and infinities as the pattern's first element
        @Test
        void testOfSubArray_floatingFirstElementKeepsCompareSemantics() throws Exception {
            for (final Class<?> type : new Class<?>[] { float.class, double.class }) {
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 2, 6 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 1, 6 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 0, 1 }, 0, 2);
                assertAllBounds(type, new int[] { 0, 1, 2, 3, 2, 6, 3, 6, 4, 5, 1, 6 }, new int[] { 4, 5 }, 0, 2);
                assertAllBounds(type, new int[] { 3, 3, 3, 3, 3, 3 }, new int[] { 2, 2, 3 }, 0, 3);
            }

            assertEquals(3,
                    Index.ofSubArray(new float[] { 0f, 1f, Float.NaN, Float.intBitsToFloat(0x7fa12345), 2f }, new float[] { Float.NaN, 2f }).orElse(-1));
            assertEquals(2, Index.ofSubArray(new double[] { 0.0, 1, -0.0, 1 }, new double[] { -0.0, 1 }).orElse(-1));
            assertEquals(-1, Index.ofSubArray(new double[] { 0.0, 1, 0.0, 1 }, new double[] { -0.0, 1 }).orElse(-1));
        }

        // The candidate-by-candidate scan of the Object[] overloads: every element comparison in its documented order.
        private int candidateScan(final Object[] source, final int bound, final Object[] pattern, final int offset, final int size, final boolean reverse) {
            final int len = source.length;
            final int end = offset + size;

            if (reverse) {
                if (bound < 0 || len < size) {
                    return -1;
                }

                for (int i = Math.min(bound, len - size); i >= 0; i--) {
                    for (int k = i, j = offset; j < end; k++, j++) {
                        if (!CommonUtil.equals(source[k], pattern[j])) {
                            break;
                        } else if (j == end - 1) {
                            return i;
                        }
                    }
                }
            } else {
                if (bound >= len || len - Math.max(bound, 0) < size) {
                    return -1;
                }

                for (int i = Math.max(bound, 0); i <= len - size; i++) {
                    for (int k = i, j = offset; j < end; k++, j++) {
                        if (!CommonUtil.equals(source[k], pattern[j])) {
                            break;
                        } else if (j == end - 1) {
                            return i;
                        }
                    }
                }
            }

            return -1;
        }

        private void assertSameEqualsCalls(final boolean reverse) {
            final List<String> calls = new ArrayList<>();

            final class Element {
                final String name;

                Element(final String name) {
                    this.name = name;
                }

                @Override
                public boolean equals(final Object obj) {
                    calls.add(name + "=" + (obj instanceof Element e ? e.name : String.valueOf(obj)));
                    return obj instanceof Element e && name.charAt(0) == e.name.charAt(0);
                }

                @Override
                public int hashCode() {
                    return name.charAt(0);
                }
            }

            final Random random = new Random(reverse ? 37 : 73);

            for (int trial = 0; trial < 300; trial++) {
                final Object[] source = new Object[random.nextInt(40)];
                final Object[] pattern = new Object[1 + random.nextInt(trial % 3 == 0 ? 20 : 5)];
                final int alphabet = 1 + trial % 4;

                for (int i = 0; i < source.length; i++) {
                    source[i] = random.nextInt(9) == 0 ? null : new Element((char) ('a' + random.nextInt(alphabet)) + "s" + i);
                }

                for (int i = 0; i < pattern.length; i++) {
                    pattern[i] = random.nextInt(9) == 0 ? null : new Element((char) ('a' + random.nextInt(alphabet)) + "p" + i);
                }

                final int offset = random.nextInt(pattern.length);
                final int size = 1 + random.nextInt(pattern.length - offset);

                if (size <= source.length && trial % 2 == 0) {
                    System.arraycopy(pattern, offset, source, random.nextInt(source.length - size + 1), size);
                }

                for (int bound = -2; bound <= source.length + 1; bound++) {
                    calls.clear();
                    final int expected = candidateScan(source, bound, pattern, offset, size, reverse);
                    final List<String> expectedCalls = new ArrayList<>(calls);
                    calls.clear();
                    final int actual = (reverse ? Index.lastOfSubArray(source, bound, pattern, offset, size)
                            : Index.ofSubArray(source, bound, pattern, offset, size)).orElse(-1);
                    final int b = bound;
                    final int t = trial;
                    assertEquals(expected, actual, () -> "trial=" + t + " bound=" + b);
                    assertEquals(expectedCalls, calls, () -> "trial=" + t + " bound=" + b);
                }
            }
        }

        // G037-02: the Object[] search makes the same equals calls (receiver, argument, order) as a candidate-by-candidate scan
        @Test
        void testOfSubArray_objectEqualsCallSequenceUnchanged() {
            assertSameEqualsCalls(false);
        }

        // G037-02: same for the backward search
        @Test
        void testLastOfSubArray_objectEqualsCallSequenceUnchanged() {
            assertSameEqualsCalls(true);
        }
    }

    // ---- perf review 2026-10-08 G037 end ----
    // ---- perf review 2026-10-08 G038 begin ----
    @Nested
    class G038 {
        /**
         * Reference for {@code IntList.removeDuplicates()}: the first occurrence of each value in encounter order.
         */
        private int[] firstOccurrences(final int[] a, final int size) {
            final Set<Integer> kept = new LinkedHashSet<>();

            for (int i = 0; i < size; i++) {
                kept.add(a[i]);
            }

            final int[] result = new int[kept.size()];
            int k = 0;

            for (final Integer x : kept) {
                result[k++] = x;
            }

            return result;
        }

        @SuppressWarnings("deprecation")
        private void assertRemoveDuplicatesMatchesReference(final int[] a, final int size, final String label) {
            final int[] backing = a.clone();
            final int[] expected = firstOccurrences(backing, size);
            final int[] spare = Arrays.copyOfRange(backing, size, backing.length);
            final IntList list = IntList.of(backing, size);

            assertEquals(expected.length != size, list.removeDuplicates(), label);
            assertEquals(expected.length, list.size(), label);
            assertSame(backing, list.internalArray(), label);
            assertArrayEquals(expected, Arrays.copyOfRange(backing, 0, expected.length), label);

            for (int i = expected.length; i < size; i++) {
                assertEquals(0, backing[i], label + " cleared slot " + i);
            }

            assertArrayEquals(spare, Arrays.copyOfRange(backing, size, backing.length), label + " spare slots");

            // the list keeps its capacity and stays usable after the shrink
            list.add(Integer.MIN_VALUE);
            assertEquals(expected.length + 1, list.size(), label);
            assertEquals(Integer.MIN_VALUE, list.get(expected.length), label);
        }

        private int randomValue(final Random rnd, final int domain, final int size) {
            switch (domain) {
                case 0: { // extremes and small values around the Integer cache bounds
                    final int[] specials = { 0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE, 127, 128, -128, -129 };
                    return specials[rnd.nextInt(specials.length)];
                }
                case 1: // small (cached) values with duplicates: a narrow span
                    return rnd.nextInt(Math.max(1, size / 2));
                case 2: // large values with duplicates: a wide span
                    return (rnd.nextInt(Math.max(1, size / 2)) - size / 4) * 1_000_003;
                case 3: // dense values just below Integer.MAX_VALUE
                    return Integer.MAX_VALUE - rnd.nextInt(Math.max(1, size));
                case 4: // dense values just above Integer.MIN_VALUE
                    return Integer.MIN_VALUE + rnd.nextInt(Math.max(1, 2 * size));
                case 5: // dense negative and positive values around zero, mostly distinct
                    return rnd.nextInt(Math.max(1, 8 * size)) - 4 * size;
                default: // full range, mostly distinct
                    return rnd.nextInt();
            }
        }

        // G038-01: removeDuplicates' unsorted path (a bit set when the value range fits in 8 bits per element, else the unchanged hash
        // set) keeps the first occurrences in order and the backing array, zeroes the cleared slots and leaves spare capacity untouched
        @Test
        public void testRemoveDuplicates_matchesFirstOccurrenceReference() {
            final Random rnd = new Random(20261008L);
            final int[] sizes = { 2, 3, 4, 5, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 127, 128, 129, 257, 1000, 3000 };

            for (final int size : sizes) {
                for (int domain = 0; domain < 7; domain++) {
                    for (int trial = 0; trial < 5; trial++) {
                        final int[] a = new int[size + rnd.nextInt(3)];

                        for (int i = 0; i < a.length; i++) {
                            a[i] = randomValue(rnd, domain, size);
                        }

                        assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " domain=" + domain + " trial=" + trial);
                    }
                }
            }
        }

        // G038-01: wide-range keys crafted to share one probe run of a multiplicative hash (the one of N's primitive key set) take the
        // hash-set path and give the same result
        @Test
        public void testRemoveDuplicates_collidingKeysMatchReference() {
            final int hashMultiplier = 0x9E3779B9; // multiplier of the hash used by N's primitive key set (N.distinct)
            int inverse = hashMultiplier;

            for (int i = 0; i < 5; i++) {
                inverse *= 2 - hashMultiplier * inverse;
            }

            final Random rnd = new Random(8L);

            for (final int distinct : new int[] { 70, 200, 5000 }) {
                final int[] a = new int[distinct * 2];

                for (int i = 0; i < a.length; i++) {
                    // the products with the multiplier are consecutive, so all keys start probing at the same slot
                    a[i] = (0x12340000 + rnd.nextInt(distinct)) * inverse;
                }

                assertRemoveDuplicatesMatchesReference(a, a.length, "colliding distinct=" + distinct);
            }
        }

        // G038-01: value spans just inside (max - min == 8 * size - 1, bit set) and just outside (max - min == 8 * size, hash set) of the
        // dense-span rule, at both ends of the int range, give the same result
        @Test
        public void testRemoveDuplicates_spanThresholdBoundary() {
            final Random rnd = new Random(38L);

            for (final int size : new int[] { 2, 3, 8, 63, 64, 65, 1000 }) {
                for (final long spanMinusOne : new long[] { 8L * size - 1, 8L * size }) {
                    for (final long min : new long[] { 0, -4L * size, Integer.MAX_VALUE - spanMinusOne, Integer.MIN_VALUE }) {
                        for (int trial = 0; trial < 3; trial++) {
                            final int[] a = new int[size];
                            a[0] = (int) min;
                            a[1] = (int) (min + spanMinusOne);

                            for (int i = 2; i < size; i++) {
                                // values inside [min, min + spanMinusOne], some repeated
                                a[i] = rnd.nextInt(3) == 0 ? a[rnd.nextInt(i)] : (int) (min + (long) (rnd.nextDouble() * (spanMinusOne + 1)));
                            }

                            for (int i = size - 1; i > 0; i--) {
                                final int j = rnd.nextInt(i + 1);
                                final int t = a[i];
                                a[i] = a[j];
                                a[j] = t;
                            }

                            assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " span-1=" + spanMinusOne + " min=" + min + " trial=" + trial);
                        }
                    }
                }
            }
        }

        // G038-01: the range check stops early: a range that only becomes too wide at the last element (or at the first ones) still gives
        // the same result, and so does one that stays narrow up to the last element
        @Test
        public void testRemoveDuplicates_rangeWidensLate() {
            for (final int size : new int[] { 3, 9, 64, 1000 }) {
                final long limit = 8L * size;

                for (final long lastOffset : new long[] { limit - 1, limit, limit + 1, -limit + 1, -limit, 1L << 31 }) {
                    final int[] a = new int[size];

                    for (int i = 0; i < size - 1; i++) {
                        a[i] = (i * 7) % Math.max(1, size / 2) + 1; // narrow, unsorted, with duplicates
                    }

                    final long last = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, 1 + lastOffset));
                    a[size - 1] = (int) last;

                    assertRemoveDuplicatesMatchesReference(a, size, "size=" + size + " last=" + last);

                    final int[] wideFirst = a.clone();
                    wideFirst[0] = a[size - 1];
                    wideFirst[size - 1] = a[0];

                    assertRemoveDuplicatesMatchesReference(wideFirst, size, "size=" + size + " first=" + last);
                }
            }
        }

        // G038-01: in-place semantics: shared backing array, unchanged when all values are distinct, sorted path, capacity kept
        @Test
        @SuppressWarnings("deprecation")
        public void testRemoveDuplicates_sharedBackingArrayExamples() {
            final int[] shared = { 300, 100, 300, 200, 100, 900, 900 };
            final IntList list = IntList.of(shared, 5);
            final IntList other = IntList.of(shared, 5);

            assertTrue(list.removeDuplicates());
            assertSame(shared, list.internalArray());
            assertEquals(3, list.size());
            assertArrayEquals(new int[] { 300, 100, 200, 0, 0, 900, 900 }, shared);
            assertEquals(5, other.size());
            assertArrayEquals(new int[] { 300, 100, 200, 0, 0 }, other.toArray());

            final int[] unique = { 3, 1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0 };
            final IntList uniqueList = IntList.of(unique);

            assertFalse(uniqueList.removeDuplicates());
            assertSame(unique, uniqueList.internalArray());
            assertEquals(7, uniqueList.size());
            assertArrayEquals(new int[] { 3, 1, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0 }, unique);

            final int[] sorted = { 1, 1, 2, 3, 3, 3, 7 };
            final IntList sortedList = IntList.of(sorted);

            assertTrue(sortedList.removeDuplicates());
            assertArrayEquals(new int[] { 1, 2, 3, 7, 0, 0, 0 }, sorted);

            final IntList grown = new IntList(64);

            for (int i = 0; i < 40; i++) {
                grown.add((i * 7) % 13);
            }

            final int[] before = grown.internalArray();

            assertTrue(grown.removeDuplicates());
            assertSame(before, grown.internalArray());
            assertEquals(64, grown.internalArray().length);
            assertArrayEquals(new int[] { 0, 7, 1, 8, 2, 9, 3, 10, 4, 11, 5, 12, 6 }, grown.toArray());

            final IntList single = IntList.of(5);
            assertFalse(single.removeDuplicates());
            assertFalse(new IntList().removeDuplicates());
        }
    }

    // ---- perf review 2026-10-08 G038 end ----
    // ---- perf review 2026-10-08 G041 begin ----
    @Nested
    class G041 {

        /**
         * Compares a JsonUtil.wrap(array) result with org.json's reflective JSONArray(Object) constructor, which is what
         * every JsonUtil.wrap(array) overload delegated to before: same length, element classes, element text and JSON.
         */
        private void assertSameAsReflectiveConstructor(final Object array, final org.json.JSONArray actual) {
            final org.json.JSONArray expected = new org.json.JSONArray(array);

            assertEquals(expected.length(), actual.length());

            for (int i = 0; i < expected.length(); i++) {
                final Object expectedElement = expected.get(i);
                final Object actualElement = actual.get(i);

                assertEquals(expectedElement.getClass(), actualElement.getClass(), "element class at " + i);
                assertEquals(String.valueOf(expectedElement), String.valueOf(actualElement), "element at " + i);

                if (!(expectedElement instanceof org.json.JSONObject) && !(expectedElement instanceof org.json.JSONArray)) {
                    assertEquals(expectedElement, actualElement, "element at " + i);
                }
            }

            assertTrue(expected.similar(actual));
            assertEquals(expected.toString(), actual.toString());
        }

        /** Runs both conversions and requires the same result or the same exception class and message. */
        private void assertSameOutcomeAsReflectiveConstructor(final Object array, final java.util.function.Supplier<org.json.JSONArray> wrap) {
            org.json.JSONArray expected = null;
            RuntimeException expectedFailure = null;

            try {
                expected = new org.json.JSONArray(array);
            } catch (final RuntimeException e) {
                expectedFailure = e;
            }

            if (expectedFailure == null) {
                final org.json.JSONArray actual = wrap.get();
                assertEquals(expected.toString(), actual.toString());
                assertSameAsReflectiveConstructor(array, actual);
            } else {
                final RuntimeException actualFailure = assertThrows(RuntimeException.class, wrap::get);
                assertEquals(expectedFailure.getClass(), actualFailure.getClass());
                assertEquals(expectedFailure.getMessage(), actualFailure.getMessage());
            }
        }

        // G041-01: wrap(primitive[]) appends through typed put(..) instead of the reflective JSONArray(Object) constructor.
        @Test
        public void testWrapPrimitiveArrays_sameElementsAsReflectiveConstructor() {
            final boolean[] booleans = { true, false, false, true };
            final char[] chars = { 'a', (char) 0, (char) 0x7F, (char) 0x80, (char) 0xE9, (char) 0xFFFF, 'Z' };
            final byte[] bytes = { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE };
            final short[] shorts = { Short.MIN_VALUE, -129, -128, 0, 127, 128, Short.MAX_VALUE };
            final int[] ints = { Integer.MIN_VALUE, -129, -128, -1, 0, 1, 127, 128, Integer.MAX_VALUE };
            final long[] longs = { Long.MIN_VALUE, -129L, -1L, 0L, 127L, 128L, Long.MAX_VALUE };
            final float[] floats = { -Float.MAX_VALUE, -1.5f, -0.0f, 0.0f, Float.MIN_VALUE, 0.1f, 3.4028235E38f };
            final double[] doubles = { -Double.MAX_VALUE, -1.5, -0.0, 0.0, Double.MIN_VALUE, 0.1, Double.MAX_VALUE };

            assertSameAsReflectiveConstructor(booleans, JsonUtil.wrap(booleans));
            assertSameAsReflectiveConstructor(chars, JsonUtil.wrap(chars));
            assertSameAsReflectiveConstructor(bytes, JsonUtil.wrap(bytes));
            assertSameAsReflectiveConstructor(shorts, JsonUtil.wrap(shorts));
            assertSameAsReflectiveConstructor(ints, JsonUtil.wrap(ints));
            assertSameAsReflectiveConstructor(longs, JsonUtil.wrap(longs));
            assertSameAsReflectiveConstructor(floats, JsonUtil.wrap(floats));
            assertSameAsReflectiveConstructor(doubles, JsonUtil.wrap(doubles));

            // char/byte/short stay Character/Byte/Short (there is no put(char/byte/short); put(int) would give Integer).
            assertEquals(Character.class, JsonUtil.wrap(chars).get(0).getClass());
            assertEquals(Byte.class, JsonUtil.wrap(bytes).get(0).getClass());
            assertEquals(Short.class, JsonUtil.wrap(shorts).get(0).getClass());
            assertEquals(Boolean.class, JsonUtil.wrap(booleans).get(0).getClass());

            // Signed zero survives.
            assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits((Float) JsonUtil.wrap(floats).get(2)));
            assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits((Double) JsonUtil.wrap(doubles).get(2)));

            // Empty and singleton arrays.
            assertEquals(0, JsonUtil.wrap(new boolean[0]).length());
            assertEquals(0, JsonUtil.wrap(new char[0]).length());
            assertEquals(0, JsonUtil.wrap(new byte[0]).length());
            assertEquals(0, JsonUtil.wrap(new short[0]).length());
            assertEquals(0, JsonUtil.wrap(new int[0]).length());
            assertEquals(0, JsonUtil.wrap(new long[0]).length());
            assertEquals(0, JsonUtil.wrap(new float[0]).length());
            assertEquals(0, JsonUtil.wrap(new double[0]).length());
            assertEquals("[]", JsonUtil.wrap(new int[0]).toString());
            assertSameAsReflectiveConstructor(new int[] { 42 }, JsonUtil.wrap(new int[] { 42 }));
            assertSameAsReflectiveConstructor(new char[] { 'x' }, JsonUtil.wrap(new char[] { 'x' }));

            // The result is a fresh, independent, mutable JSONArray.
            final org.json.JSONArray first = JsonUtil.wrap(ints);
            assertNotSame(first, JsonUtil.wrap(ints));
            first.put(5);
            assertEquals(ints.length + 1, first.length());
            assertEquals(ints.length, JsonUtil.wrap(ints).length());
        }

        // G041-01: larger random inputs, every primitive overload.
        @Test
        public void testWrapPrimitiveArrays_randomLargeInputs() {
            final Random random = new Random(41);
            final int size = 3000;
            final boolean[] booleans = new boolean[size];
            final char[] chars = new char[size];
            final byte[] bytes = new byte[size];
            final short[] shorts = new short[size];
            final int[] ints = new int[size];
            final long[] longs = new long[size];
            final float[] floats = new float[size];
            final double[] doubles = new double[size];

            for (int i = 0; i < size; i++) {
                booleans[i] = random.nextBoolean();
                chars[i] = (char) random.nextInt(0x10000);
                bytes[i] = (byte) random.nextInt();
                shorts[i] = (short) random.nextInt();
                ints[i] = i % 3 == 0 ? random.nextInt(256) - 128 : random.nextInt();
                longs[i] = i % 3 == 0 ? random.nextInt(256) - 128 : random.nextLong();
                floats[i] = (random.nextFloat() - 0.5f) * (float) Math.pow(10, random.nextInt(30) - 15);
                doubles[i] = (random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(600) - 300);
            }

            assertSameAsReflectiveConstructor(booleans, JsonUtil.wrap(booleans));
            assertSameAsReflectiveConstructor(chars, JsonUtil.wrap(chars));
            assertSameAsReflectiveConstructor(bytes, JsonUtil.wrap(bytes));
            assertSameAsReflectiveConstructor(shorts, JsonUtil.wrap(shorts));
            assertSameAsReflectiveConstructor(ints, JsonUtil.wrap(ints));
            assertSameAsReflectiveConstructor(longs, JsonUtil.wrap(longs));
            assertSameAsReflectiveConstructor(floats, JsonUtil.wrap(floats));
            assertSameAsReflectiveConstructor(doubles, JsonUtil.wrap(doubles));
        }

        // G041-01: non-finite values fail with the same JSONException; null arrays are still rejected first.
        @Test
        public void testWrapPrimitiveArrays_failuresUnchanged() {
            for (final float bad : new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY }) {
                final float[] array = { 1.5f, bad, 2.5f };
                assertSameOutcomeAsReflectiveConstructor(array, () -> JsonUtil.wrap(array));
                assertEquals("JSON does not allow non-finite numbers.", assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(array)).getMessage());
            }

            for (final double bad : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
                final double[] array = { bad };
                assertSameOutcomeAsReflectiveConstructor(array, () -> JsonUtil.wrap(array));
                assertEquals("JSON does not allow non-finite numbers.", assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(array)).getMessage());
            }

            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((boolean[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((char[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((byte[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((short[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((int[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((long[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((float[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((double[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((Object[]) null));
        }

        // G041-02: wrap(Object[]) goes through JSONArray(Collection) instead of the reflective array constructor.
        @Test
        public void testWrapObjectArray_sameElementsAsReflectiveConstructor() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("k", 1);
            map.put("n", null);
            map.put("list", Arrays.asList(1, null, "x"));

            final org.json.JSONObject jsonObject = new org.json.JSONObject().put("a", 1);
            final org.json.JSONArray jsonArray = new org.json.JSONArray().put(1).put("b");
            final Object[] array = { null, "text", 7, 7L, 1.25d, 2.5f, (short) 3, (byte) 4, 'c', true, new java.math.BigDecimal("1.10"),
                    new java.math.BigInteger("123456789012345678901234567890"), map, Arrays.asList(1, null, "x"), new LinkedHashSet<>(Arrays.asList(3, 1, 2)),
                    new int[] { 1, 2 }, new char[] { 'h', 'i' }, new Object[] { "nested", null, new double[] { 0.5 } }, jsonObject, jsonArray,
                    java.util.concurrent.TimeUnit.SECONDS, new StringBuilder("sb"), org.json.JSONObject.NULL };

            final org.json.JSONArray wrapped = JsonUtil.wrap(array);
            assertSameAsReflectiveConstructor(array, wrapped);
            assertSame(org.json.JSONObject.NULL, wrapped.get(0));
            assertSame(jsonObject, wrapped.get(18));
            assertSame(jsonArray, wrapped.get(19));
            assertSame(org.json.JSONObject.NULL, wrapped.get(22));

            assertSameAsReflectiveConstructor(new Object[0], JsonUtil.wrap(new Object[0]));
            assertSameAsReflectiveConstructor(new Object[] { null }, JsonUtil.wrap(new Object[] { null }));

            // A covariant String[] argument.
            final String[] strings = { "a", null, "c" };
            assertSameAsReflectiveConstructor(strings, JsonUtil.wrap(strings));

            final Random random = new Random(4102);
            final Object[] large = new Object[2000];

            for (int i = 0; i < large.length; i++) {
                final int kind = random.nextInt(5);
                large[i] = kind == 0 ? null
                        : kind == 1 ? "s" + random.nextInt() : kind == 2 ? random.nextInt() : kind == 3 ? random.nextDouble() : Arrays.asList(i, "e");
            }

            assertSameAsReflectiveConstructor(large, JsonUtil.wrap(large));
        }

        // G041-02: the failures (non-finite numbers, nesting-depth limit) are reported exactly as before, at the same depth.
        @Test
        public void testWrapObjectArray_failuresAndNestingLimitUnchanged() {
            final Object[] withNaN = { 1, Double.NaN };
            assertSameOutcomeAsReflectiveConstructor(withNaN, () -> JsonUtil.wrap(withNaN));
            assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(withNaN));

            final Object[] withNestedInfinity = { Arrays.asList(1.0f, Float.POSITIVE_INFINITY) };
            assertSameOutcomeAsReflectiveConstructor(withNestedInfinity, () -> JsonUtil.wrap(withNestedInfinity));

            // Nested lists around org.json's nesting limit: both routes must start the elements at the same depth.
            for (final int depth : new int[] { 510, 511, 512, 513, 514 }) {
                final List<Object> deep = new ArrayList<>();
                List<Object> current = deep;

                for (int i = 1; i < depth; i++) {
                    final List<Object> next = new ArrayList<>();
                    current.add(next);
                    current = next;
                }

                current.add("leaf");

                final Object[] array = { "head", deep };
                assertSameOutcomeAsReflectiveConstructor(array, () -> JsonUtil.wrap(array));
            }

            // A self-containing map element hits the same recursion guard.
            final Map<String, Object> self = new HashMap<>();
            self.put("self", self);
            final Object[] withCycle = { self };
            assertSameOutcomeAsReflectiveConstructor(withCycle, () -> JsonUtil.wrap(withCycle));
        }

        // G041-03: unwrap(JSONArray, primitive[]) stores exact-wrapper elements directly instead of through Array.set.
        @Test
        public void testUnwrapToPrimitiveArrays_valuesDefaultsAndConversions() {
            final org.json.JSONArray ints = new org.json.JSONArray().put(1)
                    .put(org.json.JSONObject.NULL)
                    .put("12")
                    .put(Integer.MIN_VALUE)
                    .put(Integer.MAX_VALUE)
                    .put(-128)
                    .put(128);
            assertArrayEquals(new int[] { 1, 0, 12, Integer.MIN_VALUE, Integer.MAX_VALUE, -128, 128 }, JsonUtil.unwrap(ints, int[].class));
            assertArrayEquals(new int[] { 1, 0, 12, Integer.MIN_VALUE, Integer.MAX_VALUE, -128, 128 },
                    (int[]) JsonUtil.unwrap(ints, CommonUtil.typeOf(int[].class)));

            final org.json.JSONArray longs = new org.json.JSONArray().put(Long.MIN_VALUE).put(org.json.JSONObject.NULL).put(Long.MAX_VALUE).put(7).put("-9");
            assertArrayEquals(new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE, 7L, -9L }, JsonUtil.unwrap(longs, long[].class));

            final org.json.JSONArray doubles = new org.json.JSONArray().put(1.5).put(org.json.JSONObject.NULL).put(-0.0).put(Double.MAX_VALUE).put("2.25");
            final double[] doubleResult = JsonUtil.unwrap(doubles, double[].class);
            assertEquals(5, doubleResult.length);
            assertEquals(1.5, doubleResult[0], 0.0);
            assertEquals(0.0, doubleResult[1], 0.0);
            assertEquals(Double.MAX_VALUE, doubleResult[3], 0.0);
            assertEquals(2.25, doubleResult[4], 0.0);

            final org.json.JSONArray floats = new org.json.JSONArray().put(1.5f).put(org.json.JSONObject.NULL).put("2.5").put(Float.MAX_VALUE);
            final float[] floatResult = JsonUtil.unwrap(floats, float[].class);
            assertEquals(4, floatResult.length);
            assertEquals(1.5f, floatResult[0], 0.0f);
            assertEquals(0.0f, floatResult[1], 0.0f);
            assertEquals(2.5f, floatResult[2], 0.0f);
            assertEquals(Float.MAX_VALUE, floatResult[3], 0.0f);

            final org.json.JSONArray booleans = new org.json.JSONArray().put(true).put(org.json.JSONObject.NULL).put(false).put("true");
            assertArrayEquals(new boolean[] { true, false, false, true }, JsonUtil.unwrap(booleans, boolean[].class));

            final org.json.JSONArray chars = new org.json.JSONArray().put("A").put(org.json.JSONObject.NULL).put("z");
            assertArrayEquals(new char[] { 'A', (char) 0, 'z' }, JsonUtil.unwrap(chars, char[].class));

            final org.json.JSONArray bytes = new org.json.JSONArray().put(Byte.MIN_VALUE).put(org.json.JSONObject.NULL).put(Byte.MAX_VALUE).put("5");
            assertArrayEquals(new byte[] { Byte.MIN_VALUE, 0, Byte.MAX_VALUE, 5 }, JsonUtil.unwrap(bytes, byte[].class));

            final org.json.JSONArray shorts = new org.json.JSONArray().put(Short.MIN_VALUE).put(org.json.JSONObject.NULL).put(Short.MAX_VALUE).put("-6");
            assertArrayEquals(new short[] { Short.MIN_VALUE, 0, Short.MAX_VALUE, -6 }, JsonUtil.unwrap(shorts, short[].class));

            // Empty input, a fresh result per call, and the round trip through wrap.
            assertEquals(0, JsonUtil.unwrap(new org.json.JSONArray(), int[].class).length);
            assertNotSame(JsonUtil.unwrap(ints, int[].class), JsonUtil.unwrap(ints, int[].class));

            final Random random = new Random(1041);
            final int[] source = new int[4000];
            final double[] doubleSource = new double[4000];

            for (int i = 0; i < source.length; i++) {
                source[i] = random.nextInt();
                doubleSource[i] = random.nextDouble() * 1e6 - 5e5;
            }

            assertArrayEquals(source, JsonUtil.unwrap(JsonUtil.wrap(source), int[].class));
            assertArrayEquals(doubleSource, JsonUtil.unwrap(JsonUtil.wrap(doubleSource), double[].class), 0.0);
        }

        // G041-03: a conversion failure is still thrown by the element conversion, before any store, with the same type.
        @Test
        public void testUnwrapToPrimitiveArrays_conversionFailuresUnchanged() {
            assertThrows(NumberFormatException.class, () -> JsonUtil.unwrap(new org.json.JSONArray().put(1).put("abc"), int[].class));
            assertThrows(RuntimeException.class, () -> JsonUtil.unwrap(new org.json.JSONArray().put(1).put(new org.json.JSONObject()), int[].class));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap((org.json.JSONArray) null, int[].class));
        }
    }

    // ---- perf review 2026-10-08 G041 end ----
    // ---- perf review 2026-10-08 G042 begin ----
    @Nested
    class G042 {
        private java.lang.reflect.Field lazyField(final String name) throws Exception {
            final java.lang.reflect.Field field = LazyInitializer.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }

        // G042-02: an attempt rejected because the supplier swallowed the recursive-access failure must not stay referenced
        @Test
        public void testGet_rejectedRecursiveAttemptDoesNotRetainValue() throws Exception {
            final StringBuilder rejected = new StringBuilder("rejected value");
            final AtomicInteger attempts = new AtomicInteger();
            @SuppressWarnings("unchecked")
            final com.landawn.abacus.util.function.Supplier<Object>[] self = new com.landawn.abacus.util.function.Supplier[1];

            self[0] = N.lazyInit(() -> {
                if (attempts.incrementAndGet() == 1) {
                    try {
                        self[0].get();
                    } catch (final IllegalStateException swallowed) {
                        // the supplier swallows the recursive-access failure and returns normally
                    }

                    return rejected;
                }

                return "ok";
            });

            assertTrue(self[0] instanceof LazyInitializer);

            final IllegalStateException failure = assertThrows(IllegalStateException.class, self[0]::get);
            assertEquals("Recursive initialization of deferred value", failure.getMessage());
            assertEquals(1, attempts.get());

            assertNull(lazyField("value").get(self[0]), "the rejected value must not stay referenced by the initializer");
            assertNotNull(lazyField("supplier").get(self[0]), "a failed attempt keeps the supplier for the retry");

            assertEquals("ok", self[0].get());
            assertEquals("ok", self[0].get());
            assertEquals(2, attempts.get());
            assertEquals("ok", lazyField("value").get(self[0]));
            assertNull(lazyField("supplier").get(self[0]));
        }

        // G042-02: every attempt rejected (supplier always swallows the recursion): fresh ISE per get(), one supplier call each, nothing kept
        @Test
        public void testGet_repeatedRejectedAttemptsKeepNothingAndStayRetryable() throws Exception {
            final AtomicInteger attempts = new AtomicInteger();
            final List<Object> produced = new ArrayList<>();
            @SuppressWarnings("unchecked")
            final LazyInitializer<Object>[] self = new LazyInitializer[1];

            self[0] = LazyInitializer.of(() -> {
                attempts.incrementAndGet();

                try {
                    self[0].get();
                } catch (final IllegalStateException swallowed) {
                    // swallowed every time
                }

                final Object result = new Object();
                produced.add(result);
                return result;
            });

            IllegalStateException previous = null;

            for (int i = 1; i <= 3; i++) {
                final IllegalStateException failure = assertThrows(IllegalStateException.class, self[0]::get);
                assertEquals("Recursive initialization of deferred value", failure.getMessage());
                assertNotSame(previous, failure);
                previous = failure;

                assertEquals(i, attempts.get());
                assertNull(lazyField("value").get(self[0]), "attempt " + i);
                assertFalse((Boolean) lazyField("initialized").get(self[0]), "attempt " + i);
            }

            assertEquals(3, produced.size());
        }

        // G042-02: unchanged ordinary paths: a successful (also null) value is cached and the supplier released; a throwing supplier is retried
        @Test
        public void testGet_successAndThrowingSupplierPathsUnchanged() throws Exception {
            final AtomicInteger calls = new AtomicInteger();
            final LazyInitializer<String> nullValue = LazyInitializer.of(() -> {
                calls.incrementAndGet();
                return null;
            });

            assertNull(nullValue.get());
            assertNull(nullValue.get());
            assertEquals(1, calls.get());
            assertNull(lazyField("supplier").get(nullValue));
            assertTrue((Boolean) lazyField("initialized").get(nullValue));

            final AtomicInteger attempts = new AtomicInteger();
            final LazyInitializer<String> flaky = LazyInitializer.of(() -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new UnsupportedOperationException("attempt " + attempts.get());
                }

                return "third";
            });

            assertEquals("attempt 1", assertThrows(UnsupportedOperationException.class, flaky::get).getMessage());
            assertNull(lazyField("value").get(flaky));
            assertEquals("attempt 2", assertThrows(UnsupportedOperationException.class, flaky::get).getMessage());
            assertEquals("third", flaky.get());
            assertEquals("third", flaky.get());
            assertEquals(3, attempts.get());
        }
    }

    // ---- perf review 2026-10-08 G042 end ----
    // ---- perf review 2026-10-08 G043 begin ----
    @Nested
    class G043 {

        /**
         * The query-first algorithm {@code Maps.invert(Map, BiFunction)} used before the review, applied to a result map
         * created exactly as {@code Maps.newOrderingMap} creates it. Records every merge call.
         */
        private <K, V> Map<V, K> referenceInvert(final Map<K, V> map, final java.util.function.BiFunction<? super K, ? super K, ? extends K> mergeFunction) {
            final Map<V, K> result = map.getClass() == HashMap.class ? CommonUtil.newHashMap(map.size()) : CommonUtil.newLinkedHashMap(map.size());

            for (final Map.Entry<K, V> entry : map.entrySet()) {
                final K oldVal = result.get(entry.getValue());

                if (oldVal != null || result.containsKey(entry.getValue())) {
                    final K merged = mergeFunction.apply(oldVal, entry.getKey());

                    if (merged == null) {
                        result.remove(entry.getValue());
                    } else {
                        result.put(entry.getValue(), merged);
                    }
                } else {
                    result.put(entry.getValue(), entry.getKey());
                }
            }

            return result;
        }

        private java.util.function.BiFunction<String, String, String> recordingMerger(final List<String> calls, final int mode) {
            return (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);

                switch (mode) {
                    case 0:
                        return oldKey;
                    case 1:
                        return newKey;
                    case 2:
                        return null;
                    default:
                        // null for every third call, a concatenation otherwise
                        return calls.size() % 3 == 0 ? null : oldKey + "+" + newKey;
                }
            };
        }

        private void assertSameInvert(final Map<String, String> input, final int mode) {
            final List<String> expectedCalls = new ArrayList<>();
            final List<String> actualCalls = new ArrayList<>();
            final Map<String, String> expected = referenceInvert(input, recordingMerger(expectedCalls, mode));
            final Map<String, String> actual = Maps.invert(input, recordingMerger(actualCalls, mode));

            assertEquals(expected.getClass(), actual.getClass(), input.getClass().getSimpleName());
            assertEquals(expected, actual);
            // Same iteration order: identical put/remove sequence on a map of the same class and capacity.
            assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
            assertEquals(expectedCalls, actualCalls);
        }

        // G043-01: invert(Map, BiFunction) - random inputs with duplicate values, null keys and null values, every merge mode
        @Test
        public void testInvertWithMerge_randomInputsMatchQueryFirstReference() {
            final Random random = new Random(20261008L);

            for (int round = 0; round < 300; round++) {
                final int size = random.nextInt(round < 50 ? 6 : 60);
                final int distinctValues = 1 + random.nextInt(Math.max(1, size));
                final Map<String, String> hashMap = new HashMap<>();
                final Map<String, String> linkedHashMap = new LinkedHashMap<>();
                final Map<String, String> treeMap = new TreeMap<>();

                for (int i = 0; i < size; i++) {
                    final String key = random.nextInt(10) == 0 ? null : "k" + random.nextInt(size * 2 + 1);
                    final String value = random.nextInt(8) == 0 ? null : "v" + random.nextInt(distinctValues);
                    hashMap.put(key, value);
                    linkedHashMap.put(key, value);

                    if (key != null) {
                        treeMap.put(key, value);
                    }
                }

                for (int mode = 0; mode < 4; mode++) {
                    assertSameInvert(hashMap, mode);
                    assertSameInvert(linkedHashMap, mode);
                    assertSameInvert(treeMap, mode);
                }
            }
        }

        // G043-01: a null merge result removes the value; a later equal value is re-inserted at the end without a merge call
        @Test
        public void testInvertWithMerge_nullMergeRemovesThenReinsertsAtEnd() {
            final Map<String, String> map = new LinkedHashMap<>();
            map.put("a", "x");
            map.put("b", "y");
            map.put("c", "x");
            map.put("d", "z");
            map.put("e", "x");
            final List<String> calls = new ArrayList<>();

            final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);
                return null;
            });

            assertTrue(inverted instanceof LinkedHashMap);
            assertEquals(Arrays.asList("y", "z", "x"), new ArrayList<>(inverted.keySet()));
            assertEquals(Arrays.asList("b", "d", "e"), new ArrayList<>(inverted.values()));
            assertEquals(Arrays.asList("a|c"), calls);
        }

        // G043-01: a value first bound to the null input key is present (merge sees null), also in a HashMap result
        @Test
        public void testInvertWithMerge_valueBoundToNullKeyIsPresent() {
            final Map<String, String> map = new HashMap<>();
            map.put(null, "v");
            map.put("k", "v");
            map.put("other", "w");
            final List<String> calls = new ArrayList<>();

            final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);
                return "merged";
            });

            assertTrue(inverted.getClass() == HashMap.class);
            assertEquals(2, inverted.size());
            assertEquals("merged", inverted.get("v"));
            assertEquals("other", inverted.get("w"));
            assertEquals(1, calls.size());
            assertTrue(calls.get(0).equals("null|k") || calls.get(0).equals("k|null"), calls.toString());

            // a null key that collides with nothing is stored as a null value
            final Map<String, String> single = new HashMap<>();
            single.put(null, "v");
            final Map<String, String> invertedSingle = Maps.invert(single, (oldKey, newKey) -> "never");
            assertTrue(invertedSingle.containsKey("v"));
            assertNull(invertedSingle.get("v"));
        }

        // G043-01: exceptions from the merge function and from a value's hashCode still propagate unchanged
        @Test
        public void testInvertWithMerge_failuresPropagate() {
            final Map<String, String> map = new LinkedHashMap<>();
            map.put("a", "x");
            map.put("b", "x");

            final IllegalStateException mergeFailure = assertThrows(IllegalStateException.class, () -> Maps.invert(map, (oldKey, newKey) -> {
                throw new IllegalStateException("merge " + oldKey + " " + newKey);
            }));
            assertEquals("merge a b", mergeFailure.getMessage());

            final Object badValue = new Object() {
                @Override
                public int hashCode() {
                    throw new UnsupportedOperationException("no hash");
                }
            };
            final Map<String, Object> withBadValue = new LinkedHashMap<>();
            withBadValue.put("a", "fine");
            withBadValue.put("b", badValue);
            final UnsupportedOperationException hashFailure = assertThrows(UnsupportedOperationException.class,
                    () -> Maps.invert(withBadValue, (oldKey, newKey) -> oldKey));
            assertEquals("no hash", hashFailure.getMessage());

            assertThrows(IllegalArgumentException.class, () -> Maps.invert(map, null));
            assertTrue(Maps.invert((Map<String, String>) null, (oldKey, newKey) -> oldKey).isEmpty());
        }

        private final class Item {
            final String name;
            final int seq;

            Item(final String name, final int seq) {
                this.name = name;
                this.seq = seq;
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof Item && ((Item) obj).name.equals(name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public String toString() {
                return name + "#" + seq;
            }
        }

        // G043-01: a BiMap input yields a BiMap result, which keeps the query-first sequence. With seq-ordered backing maps the
        // input holds equals()-equal keys and values; binding k2 before merging would hit the value-uniqueness check (k2 equals kX).
        @Test
        public void testInvertWithMerge_biMapResultKeepsQueryFirstSequence() {
            final java.util.Comparator<Item> bySeq = java.util.Comparator.comparingInt(item -> item.seq);
            final BiMap<Item, Item> input = new BiMap<>(() -> new TreeMap<>(bySeq), () -> new TreeMap<>(bySeq));
            final Item kX = new Item("kx", 0);
            final Item k1 = new Item("k1", 1);
            final Item k2 = new Item("kx", 2);
            final Item vX = new Item("vx", 10);
            final Item v1 = new Item("v", 11);
            final Item v2 = new Item("v", 12);
            input.put(kX, vX);
            input.put(k1, v1);
            input.put(k2, v2);
            final List<String> calls = new ArrayList<>();

            final Map<Item, Item> inverted = Maps.invert(input, (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);
                return oldKey;
            });

            assertTrue(inverted instanceof BiMap, inverted.getClass().getName());
            assertEquals(Arrays.asList("k1#1|kx#2"), calls);
            assertEquals(2, inverted.size());
            assertSame(kX, inverted.get(vX));
            assertSame(k1, inverted.get(v1));

            // an ordinary BiMap: no collisions, merge never called
            final BiMap<String, Integer> plain = new BiMap<>();
            plain.put("a", 1);
            plain.put("b", 2);
            final Map<Integer, String> invertedPlain = Maps.invert(plain, (oldKey, newKey) -> {
                throw new AssertionError("no merge expected");
            });
            assertTrue(invertedPlain instanceof BiMap);
            assertEquals(CommonUtil.asMap(1, "a", 2, "b"), invertedPlain);
        }

        @SuppressWarnings("serial")
        static final class NamedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
            public NamedLinkedHashMap() {
            }
        }

        // G043-02: newTargetMap - exact HashMap/LinkedHashMap templates yield an empty map of that exact class; other classes are unaffected
        @Test
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void testNewTargetMap_hashMapAndLinkedHashMapTemplates() {
            final Map<String, Integer> hashMap = new HashMap<>(CommonUtil.asMap("a", 1, "b", 2, "c", 3));
            final Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
            linkedHashMap.put("c", 3);
            linkedHashMap.put("a", 1);
            linkedHashMap.put("b", 2);
            final Map<String, Integer> accessOrdered = new LinkedHashMap<>(16, 0.75f, true);
            accessOrdered.putAll(linkedHashMap);

            for (final int size : new int[] { 0, 1, 2, 3, 16, 1000 }) {
                for (final Map template : Arrays.asList(hashMap, linkedHashMap, accessOrdered)) {
                    final Map created = Maps.newTargetMap(template, size);
                    assertEquals(template.getClass(), created.getClass());
                    assertTrue(created.isEmpty());
                    assertNotSame(template, created);

                    final Map created3 = Maps.newTargetMap(template, size, size * 2);
                    assertEquals(template.getClass(), created3.getClass());
                    assertTrue(created3.isEmpty());
                }
            }

            // a LinkedHashMap result built from an access-ordered template is insertion-ordered (no access order carried over)
            final Map<String, Integer> fromAccessOrdered = Maps.newTargetMap(accessOrdered, 3);
            fromAccessOrdered.put("x", 1);
            fromAccessOrdered.put("y", 2);
            fromAccessOrdered.get("x");
            assertEquals(Arrays.asList("x", "y"), new ArrayList<>(fromAccessOrdered.keySet()));

            // negative sizes keep failing with the same IllegalArgumentException message
            final String expectedMessage = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newHashMap(-1)).getMessage();
            assertEquals(expectedMessage, assertThrows(IllegalArgumentException.class, () -> Maps.newTargetMap(hashMap, -1)).getMessage());
            assertEquals(expectedMessage, assertThrows(IllegalArgumentException.class, () -> Maps.newTargetMap(linkedHashMap, -1)).getMessage());

            // subclasses, sorted maps and the null template keep their own paths
            final NamedLinkedHashMap<String, Integer> named = new NamedLinkedHashMap<>();
            named.put("a", 1);
            assertEquals(NamedLinkedHashMap.class, Maps.newTargetMap(named, 1).getClass());
            assertEquals(TreeMap.class, Maps.newTargetMap(new TreeMap<>(linkedHashMap), 3).getClass());
            assertEquals(HashMap.class, Maps.newTargetMap(null, 3).getClass());

            // the public operations built on it
            final Map<String, Integer> filtered = Maps.filter(linkedHashMap, (k, v) -> v > 1);
            assertEquals(LinkedHashMap.class, filtered.getClass());
            assertEquals(Arrays.asList("c", "b"), new ArrayList<>(filtered.keySet()));
            assertEquals(HashMap.class, Maps.filterByKey(hashMap, k -> !k.equals("a")).getClass());
            assertEquals(CommonUtil.asMap("b", 2, "c", 3), Maps.filterByValue(hashMap, v -> v > 1));
            assertEquals(LinkedHashMap.class, Maps.intersection(linkedHashMap, hashMap).getClass());
            assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(Maps.intersection(linkedHashMap, hashMap).keySet()));
            assertEquals(HashMap.class, Maps.difference(hashMap, CommonUtil.asMap("a", 1)).getClass());
            assertEquals(LinkedHashMap.class, Maps.symmetricDifference(linkedHashMap, hashMap).getClass());
            assertEquals(LinkedHashMap.class, Maps.intersection(linkedHashMap, null).getClass());
        }
    }

    // ---- perf review 2026-10-08 G043 end ----
    // ---- perf review 2026-10-08 G045 begin ----
    @Nested
    class G045 {

        // G045-01: the no-arg constructor builds a fresh, empty HashMap backing map of its own
        @Test
        void testNoArgConstructor_freshEmptyHashMapBacking() {
            final Multiset<String> first = new Multiset<>();
            final Multiset<String> second = new Multiset<>();

            assertTrue(first.isEmpty());
            assertEquals(0, first.size());
            assertEquals(0, first.countOfDistinctElements());
            assertEquals(0L, first.sumOfOccurrences());
            assertEquals("{}", first.toString());
            assertSame(new HashMap<String, Object>().keySet().getClass(), first.elementSet().getClass());
            assertSame(HashMap.class, first.toMap().getClass());
            assertNotSame(first.elementSet(), second.elementSet());

            assertEquals(0, first.add("a", 2));
            first.add("b");
            assertEquals(2, first.getCount("a"));
            assertEquals(3, first.size());
            assertEquals(0, second.getCount("a"));
            assertTrue(second.isEmpty());
            assertSame(HashMap.class, first.toMap().getClass());
            assertEquals(Map.of("a", 2, "b", 1), first.toMap());
        }

        // G045-01: HashMap semantics of the default backing map (null element, equals-based keys, foreign-typed lookup keys)
        @Test
        void testNoArgConstructor_hashMapSemantics() {
            final Multiset<String> multiset = new Multiset<>();

            assertTrue(multiset.add(null));
            assertEquals(1, multiset.add(null, 2));
            multiset.add(new String("x"));
            multiset.add(new String("x"));

            assertEquals(3, multiset.getCount(null));
            assertEquals(2, multiset.getCount("x"));
            assertEquals(2, multiset.countOfDistinctElements());
            assertEquals(5, multiset.size());
            assertEquals(0, multiset.getCount(Integer.valueOf(42)));
            assertFalse(multiset.contains(Integer.valueOf(42)));
            assertEquals(0, multiset.removeAllOccurrencesOf(Integer.valueOf(42)));

            final Multiset<String> viaClass = new Multiset<>(HashMap.class);
            viaClass.add("x", 2);
            viaClass.add(null, 3);
            assertEquals(viaClass, multiset);
            assertEquals(multiset, viaClass);
            assertEquals(viaClass.hashCode(), multiset.hashCode());

            assertEquals(3, multiset.removeAllOccurrencesOf((Object) null));
            assertEquals(1, multiset.removeAndGetCount("x", 1));
            assertEquals(0, multiset.removeAndGetCount("x", 5));
            assertTrue(multiset.isEmpty());
            assertThrows(IllegalArgumentException.class, () -> multiset.add("y", -1));
            assertTrue(multiset.isEmpty());
        }

        // G045-01: factories that delegate to the no-arg constructor still return independent HashMap-backed multisets
        @Test
        void testNoArgConstructor_viaFactories() {
            final Multiset<String> fromNullIterator = Multiset.create((Iterator<String>) null);
            assertTrue(fromNullIterator.isEmpty());
            assertSame(HashMap.class, fromNullIterator.toMap().getClass());

            final Multiset<String> fromIterator = Multiset.create(Arrays.asList("b", "a", "b").iterator());
            assertEquals(2, fromIterator.getCount("b"));
            assertEquals(1, fromIterator.getCount("a"));
            assertSame(HashMap.class, fromIterator.toMap().getClass());

            final Multiset<String> emptyOf = Multiset.of();
            final Multiset<String> otherEmptyOf = Multiset.of();
            assertTrue(emptyOf.isEmpty());
            assertSame(HashMap.class, emptyOf.toMap().getClass());
            emptyOf.add("z");
            assertTrue(otherEmptyOf.isEmpty());

            final Multiset<String> fromN = CommonUtil.newMultiset();
            assertTrue(fromN.isEmpty());
            assertSame(HashMap.class, fromN.toMap().getClass());

            final Multiset<String> collected = com.landawn.abacus.util.stream.Stream.of("b", "a", "b", "c")
                    .collect(com.landawn.abacus.util.stream.Collectors.toMultiset());
            assertEquals(Map.of("a", 1, "b", 2, "c", 1), collected.toMap());
            assertSame(HashMap.class, collected.toMap().getClass());

            final Multiset<String> streamed = com.landawn.abacus.util.stream.Stream.of("q", "q").toMultiset();
            assertEquals(2, streamed.getCount("q"));
            assertSame(HashMap.class, streamed.toMap().getClass());
        }
    }

    // ---- perf review 2026-10-08 G045 end ----
    // ---- perf review 2026-10-08 G046 begin ----
    @Nested
    class G046 {

        private List<List<Object>> rowsOf(final Dataset ds) {
            final List<List<Object>> rows = new ArrayList<>();

            for (int rowIndex = 0; rowIndex < ds.size(); rowIndex++) {
                final List<Object> row = new ArrayList<>();

                for (int columnIndex = 0; columnIndex < ds.columnCount(); columnIndex++) {
                    final Object value = ds.get(rowIndex, columnIndex);
                    row.add(value);
                }

                rows.add(row);
            }

            return rows;
        }

        private List<String> commonColumnsInFirstOrder(final Dataset a, final Dataset b) {
            final Set<String> namesOfB = new HashSet<>(b.columnNames());
            final List<String> common = new ArrayList<>();

            for (final String name : a.columnNames()) {
                if (namesOfB.contains(name)) {
                    common.add(name);
                }
            }

            return common;
        }

        // G046-01: no common column -> same IllegalArgumentException and message from every key-less overload; null checks first
        @Test
        void testIntersection_datasetNoCommonColumns_messageAndNullChecksPinned() {
            final Dataset a = Dataset.rows(Arrays.asList("a1", "a2"), new Object[][] { { 1, 2 } });
            final Dataset b = Dataset.rows(Arrays.asList("b1", "b2", "b3"), new Object[][] { { 1, 2, 3 } });
            final String expected = "These two Datasets do not have any common column names: [a1, a2], [b1, b2, b3]";

            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.intersection(a, b)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.intersection(a, b, true)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.difference(a, b)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.difference(a, b, false)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, b)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, b, true)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [b1, b2, b3], [a1, a2]",
                    assertThrows(IllegalArgumentException.class, () -> N.intersection(b, a)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [], [b1, b2, b3]",
                    assertThrows(IllegalArgumentException.class, () -> N.difference(Dataset.empty(), b)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [a1, a2], []",
                    assertThrows(IllegalArgumentException.class, () -> N.intersection(a, Dataset.empty())).getMessage());

            assertEquals("The first specified Dataset is null", assertThrows(IllegalArgumentException.class, () -> N.intersection(null, b)).getMessage());
            assertEquals("The second specified Dataset is null", assertThrows(IllegalArgumentException.class, () -> N.difference(a, null)).getMessage());
            assertEquals("The second specified Dataset is null",
                    assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, null)).getMessage());

            // nothing was changed on either side
            assertEquals(Arrays.asList("a1", "a2"), a.columnNames());
            assertEquals(Arrays.asList("b1", "b2", "b3"), b.columnNames());
        }

        // G046-01: key columns = a's columns that b also has (b reordered, extra columns on both sides), occurrence semantics kept
        @Test
        void testIntersection_datasetPartialOverlapReorderedColumns() {
            final Dataset a = Dataset.rows(Arrays.asList("id", "name", "deptA", "k"),
                    new Object[][] { { 1, "x", "d1", null }, { 2, "y", "d2", 5 }, { 1, "x", "d3", null }, { 3, "z", "d4", 7 }, { 2, "y", "d5", 5 } });
            final Dataset b = Dataset.rows(Arrays.asList("extraB", "k", "name", "id"),
                    new Object[][] { { "e1", null, "x", 1 }, { "e2", 5, "y", 2 }, { "e3", 9, "q", 4 } });
            final List<String> keys = Arrays.asList("id", "name", "k");

            final Dataset intersection = N.intersection(a, b);
            assertEquals(Arrays.asList("id", "name", "deptA", "k"), intersection.columnNames());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "d1", null), Arrays.asList(2, "y", "d2", 5)), rowsOf(intersection));
            assertEquals(rowsOf(N.intersection(a, b, keys)), rowsOf(intersection));
            assertEquals(rowsOf(intersection), rowsOf(N.intersection(a, b, false)));

            final Dataset difference = N.difference(a, b);
            assertEquals(Arrays.asList("id", "name", "deptA", "k"), difference.columnNames());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "d3", null), Arrays.asList(3, "z", "d4", 7), Arrays.asList(2, "y", "d5", 5)), rowsOf(difference));
            assertEquals(rowsOf(N.difference(a, b, keys)), rowsOf(difference));

            final Dataset reversedIntersection = N.intersection(b, a);
            assertEquals(Arrays.asList("extraB", "k", "name", "id"), reversedIntersection.columnNames());
            assertEquals(Arrays.asList(Arrays.asList("e1", null, "x", 1), Arrays.asList("e2", 5, "y", 2)), rowsOf(reversedIntersection));
            assertEquals(Arrays.asList(Arrays.asList("e3", 9, "q", 4)), rowsOf(N.difference(b, a)));

            final Dataset symmetric = N.symmetricDifference(a, b);
            final Dataset symmetricByKeys = N.symmetricDifference(a, b, keys);
            assertEquals(symmetricByKeys.columnNames(), symmetric.columnNames());
            assertEquals(rowsOf(symmetricByKeys), rowsOf(symmetric));
            assertEquals(4, symmetric.size());

            final Dataset noRows = Dataset.rows(Arrays.asList("deptA", "k", "id", "name"), new Object[0][]);
            assertEquals(0, N.intersection(noRows, b).size());
            assertEquals(Arrays.asList("deptA", "k", "id", "name"), N.difference(noRows, b).columnNames());
            assertEquals(0, N.intersection(a, noRows).size());
            assertEquals(rowsOf(a), rowsOf(N.difference(a, noRows)));
        }

        // G046-01: wide Datasets (hundreds of columns, b reversed with gaps and extras) match the explicit-key overloads
        @Test
        void testIntersection_datasetWideReorderedColumnsMatchExplicitKeys() {
            final int width = 300;
            final List<String> namesA = new ArrayList<>();

            for (int i = 0; i < width; i++) {
                namesA.add("c" + i);
            }

            final List<String> namesB = new ArrayList<>();

            for (int i = width - 1; i >= 0; i--) {
                if (i % 7 != 3) {
                    namesB.add("c" + i);
                }
            }

            namesB.add("onlyB1");
            namesB.add("onlyB2");

            final Object[][] rowsA = new Object[6][width];

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < width; c++) {
                    rowsA[r][c] = (r % 3) * 1000 + c; // rows r and r + 3 are equal
                }
            }

            final int[] patternsB = { 0, 0, 1, 9 };
            final Object[][] rowsB = new Object[patternsB.length][namesB.size()];

            for (int r = 0; r < patternsB.length; r++) {
                for (int j = 0; j < namesB.size(); j++) {
                    final String name = namesB.get(j);
                    rowsB[r][j] = name.startsWith("only") ? name + r : (Object) (patternsB[r] * 1000 + Integer.parseInt(name.substring(1)));
                }
            }

            final Dataset a = Dataset.rows(namesA, rowsA);
            final Dataset b = Dataset.rows(namesB, rowsB);
            final List<String> keys = commonColumnsInFirstOrder(a, b);
            assertEquals(width - 43, keys.size());

            final Dataset intersection = N.intersection(a, b);
            assertEquals(namesA, intersection.columnNames());
            assertEquals(Arrays.asList(rowsOf(a).get(0), rowsOf(a).get(1), rowsOf(a).get(3)), rowsOf(intersection));
            assertEquals(rowsOf(N.intersection(a, b, keys)), rowsOf(intersection));

            final Dataset difference = N.difference(a, b);
            assertEquals(Arrays.asList(rowsOf(a).get(2), rowsOf(a).get(4), rowsOf(a).get(5)), rowsOf(difference));
            assertEquals(rowsOf(N.difference(a, b, keys)), rowsOf(difference));

            final Dataset symmetric = N.symmetricDifference(a, b);
            assertEquals(4, symmetric.size());
            assertEquals(width + 2, symmetric.columnCount());
            assertEquals(rowsOf(N.symmetricDifference(a, b, keys)), rowsOf(symmetric));

            final Dataset reversed = N.intersection(b, a);
            assertEquals(namesB, reversed.columnNames());
            assertEquals(Arrays.asList(rowsOf(b).get(0), rowsOf(b).get(1), rowsOf(b).get(2)), rowsOf(reversed));
            assertEquals(rowsOf(N.intersection(b, a, commonColumnsInFirstOrder(b, a))), rowsOf(reversed));
        }

        // G046-01: requiresSameColumns accepts the same column set in another order and still rejects a different set
        @Test
        void testIntersection_datasetRequiresSameColumnsReordered() {
            final Dataset a = Dataset.rows(Arrays.asList("id", "name", "v"), new Object[][] { { 1, "x", "p" }, { 2, "y", "q" }, { 2, "y", "q" } });
            final Dataset b = Dataset.rows(Arrays.asList("v", "id", "name"), new Object[][] { { "p", 1, "x" }, { "q", 2, "y" } });

            assertEquals(Arrays.asList(Arrays.asList(1, "x", "p"), Arrays.asList(2, "y", "q")), rowsOf(N.intersection(a, b, true)));
            assertEquals(Arrays.asList(Arrays.asList(2, "y", "q")), rowsOf(N.difference(a, b, true)));
            assertEquals(Arrays.asList("id", "name", "v"), N.symmetricDifference(a, b, true).columnNames());
            assertEquals(Arrays.asList(Arrays.asList(2, "y", "q")), rowsOf(N.symmetricDifference(a, b, true)));

            final Dataset extra = Dataset.rows(Arrays.asList("v", "id", "name", "extra"), new Object[][] { { "p", 1, "x", "e" }, { "q", 2, "y", "f" } });

            assertEquals("These two Datasets do not have the same column names: [id, name, v], [v, id, name, extra]",
                    assertThrows(IllegalArgumentException.class, () -> N.intersection(a, extra, true)).getMessage());
            assertEquals("These two Datasets do not have the same column names: [id, name, v], [v, id, name, extra]",
                    assertThrows(IllegalArgumentException.class, () -> N.symmetricDifference(a, extra, true)).getMessage());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "p"), Arrays.asList(2, "y", "q")), rowsOf(N.intersection(a, extra, false)));
        }

        // G046-01: the column lookup follows b's current columns after rename/remove/add (index built before) and for slices
        @Test
        void testIntersection_datasetColumnIndexFollowsRenameRemoveAddAndSlice() {
            final Dataset a = Dataset.rows(Arrays.asList("id", "name", "v"), new Object[][] { { 1, "x", "a" }, { 1, "y", "b" }, { 2, "x", "c" } });
            final Dataset b = Dataset.rows(Arrays.asList("id", "tmp"), new Object[][] { { 1, "y" }, { 2, "z" } });

            assertEquals(1, b.getColumnIndex("tmp")); // the column index of b exists before the changes below
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.intersection(a, b)));

            b.renameColumn("tmp", "name");
            assertEquals(Arrays.asList(Arrays.asList(1, "y", "b")), rowsOf(N.intersection(a, b)));
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.difference(a, b)));

            b.removeColumn("name");
            assertEquals(Arrays.asList("id"), b.columnNames());
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.intersection(a, b)));

            b.addColumn("name", Arrays.asList("x", "q"));
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a")), rowsOf(N.intersection(a, b)));

            final Dataset firstRow = b.slice(0, 1);
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a")), rowsOf(N.intersection(a, firstRow)));
            assertEquals(Arrays.asList(Arrays.asList(1, "y", "b"), Arrays.asList(2, "x", "c")), rowsOf(N.difference(a, firstRow)));

            final Dataset idOnly = b.slice(Arrays.asList("id"));
            assertEquals(Arrays.asList(Arrays.asList(1, "x", "a"), Arrays.asList(2, "x", "c")), rowsOf(N.intersection(a, idOnly)));
            assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2)), rowsOf(N.intersection(idOnly, a)));
        }
    }

    // ---- perf review 2026-10-08 G046 end ----
    // ---- perf review 2026-10-08 G047 begin ----
    @Nested
    class G047 {

        // G047-01: unsorted char ranges of <= 32 elements are de-duplicated by a scan instead of an 8 KB bitmap; pins the result on both sides of that limit
        @Test
        void testRemoveDuplicates_charSmallRangeScanMatchesFirstOccurrenceModel() {
            final Random rnd = new Random(20261008L);
            final char[] pool = { (char) 0, (char) 0xFFFF, 'a', 'b', 'A', (char) 0xD83D, (char) 0xDE80, (char) 0x4E00, (char) 0xE9, '0' };

            for (int trial = 0; trial < 6000; trial++) {
                final int len = rnd.nextInt(70);
                final char[] a = new char[len];
                final int mode = trial % 3;

                for (int i = 0; i < len; i++) {
                    a[i] = mode == 0 ? pool[rnd.nextInt(pool.length)] : mode == 1 ? (char) rnd.nextInt(65536) : (char) ('a' + rnd.nextInt(1 + trial % 40));
                }

                final char[] copy = a.clone();
                final int from = rnd.nextInt(len + 1);
                final int to = from + rnd.nextInt(len - from + 1);

                assertDistinct(charModel(a, from, to), N.removeDuplicates(a, from, to, false), a);
                assertDistinct(charModel(a, 0, len), N.removeDuplicates(a, 0, len, false), a);
                assertDistinct(charModel(a, 0, len), N.distinct(a), a);
                assertDistinct(charModel(a, from, to), N.distinct(a, from, to), a);
                assertDistinct(charModel(a, 0, len), N.removeDuplicates(a, false), a);
                assertDistinct(charModel(a, from, to), CharList.of(a.clone()).distinct(from, to).toArray(), a);
                assertArrayEquals(copy, a);
            }
        }

        // G047-01: the same for short (negative values, both extremes, -1 next to 0xFFFF-looking values)
        @Test
        void testRemoveDuplicates_shortSmallRangeScanMatchesFirstOccurrenceModel() {
            final Random rnd = new Random(8102026L);
            final short[] pool = { Short.MIN_VALUE, Short.MAX_VALUE, -1, 0, 1, -32767, 255, -256, 128, -129 };

            for (int trial = 0; trial < 6000; trial++) {
                final int len = rnd.nextInt(70);
                final short[] a = new short[len];
                final int mode = trial % 3;

                for (int i = 0; i < len; i++) {
                    a[i] = mode == 0 ? pool[rnd.nextInt(pool.length)] : mode == 1 ? (short) rnd.nextInt(65536) : (short) (rnd.nextInt(1 + trial % 40) - 20);
                }

                final short[] copy = a.clone();
                final int from = rnd.nextInt(len + 1);
                final int to = from + rnd.nextInt(len - from + 1);

                assertDistinct(shortModel(a, from, to), N.removeDuplicates(a, from, to, false), a);
                assertDistinct(shortModel(a, 0, len), N.removeDuplicates(a, 0, len, false), a);
                assertDistinct(shortModel(a, 0, len), N.distinct(a), a);
                assertDistinct(shortModel(a, from, to), N.distinct(a, from, to), a);
                assertDistinct(shortModel(a, 0, len), N.removeDuplicates(a, false), a);
                assertDistinct(shortModel(a, from, to), ShortList.of(a.clone()).distinct(from, to).toArray(), a);
                assertArrayEquals(copy, a);
            }
        }

        // G047-01: exact boundary of the scan limit (31/32/33 elements), all distinct and with a single repeat at either end
        @Test
        void testRemoveDuplicates_charShortScanLimitBoundary() {
            for (final int len : new int[] { 2, 3, 31, 32, 33, 34 }) {
                final char[] distinctChars = new char[len];
                final short[] distinctShorts = new short[len];

                for (int i = 0; i < len; i++) {
                    distinctChars[i] = (char) (0xFFFF - i * 997);
                    distinctShorts[i] = (short) (Short.MIN_VALUE + i * 2039);
                }

                assertDistinct(distinctChars.clone(), N.distinct(distinctChars), distinctChars);
                assertDistinct(distinctShorts.clone(), N.distinct(distinctShorts), distinctShorts);

                final char[] repeatLastChar = distinctChars.clone();
                repeatLastChar[len - 1] = repeatLastChar[0];
                assertDistinct(charModel(repeatLastChar, 0, len), N.distinct(repeatLastChar), repeatLastChar);
                assertEquals(len - 1, N.distinct(repeatLastChar).length);

                final short[] repeatFirstShort = distinctShorts.clone();
                repeatFirstShort[0] = repeatFirstShort[len - 1];
                assertDistinct(shortModel(repeatFirstShort, 0, len), N.distinct(repeatFirstShort), repeatFirstShort);
                assertEquals(len - 1, N.distinct(repeatFirstShort).length);

                final char[] allSameChar = new char[len];
                Arrays.fill(allSameChar, (char) 0xFFFF);
                assertArrayEquals(new char[] { (char) 0xFFFF }, N.distinct(allSameChar));

                final short[] allSameShort = new short[len];
                Arrays.fill(allSameShort, (short) -1);
                assertArrayEquals(new short[] { -1 }, N.distinct(allSameShort));
            }
        }

        // G047-02: an all-distinct unsorted range is returned as a fresh copy of the range (byte/char/short), also past the bitmap capacity
        @Test
        void testRemoveDuplicates_allDistinctRangeReturnsFreshCopyOfRange() {
            for (final int len : new int[] { 2, 32, 33, 200, 255, 256 }) {
                final byte[] bytes = new byte[len + 2];

                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) (i * 7 + 3);
                }

                if (len + 2 <= 256) {
                    assertDistinct(bytes.clone(), N.distinct(bytes), bytes);
                    assertDistinct(bytes.clone(), N.removeDuplicates(bytes, false), bytes);
                }

                assertDistinct(Arrays.copyOfRange(bytes, 1, len + 1), N.removeDuplicates(bytes, 1, len + 1, false), bytes);
            }

            final byte[] allBytes = new byte[300];
            for (int i = 0; i < allBytes.length; i++) {
                allBytes[i] = (byte) i;
            }
            assertDistinct(Arrays.copyOf(allBytes, 256), N.distinct(allBytes), allBytes);
            assertDistinct(Arrays.copyOfRange(allBytes, 44, 300), N.removeDuplicates(allBytes, 44, 300, false), allBytes);

            for (final int len : new int[] { 2, 32, 33, 1000, 65535, 65536 }) {
                final char[] chars = new char[len + 2];
                final short[] shorts = new short[len + 2];

                for (int i = 0; i < chars.length; i++) {
                    chars[i] = (char) (i * 3 + 1); // 3 is odd, so i * 3 + 1 is distinct for 65536 consecutive i
                    shorts[i] = (short) (i * 5 - 7);
                }

                if (len + 2 <= 65536) {
                    assertDistinct(chars.clone(), N.distinct(chars), chars);
                    assertDistinct(shorts.clone(), N.distinct(shorts), shorts);
                    assertDistinct(chars.clone(), CharList.of(chars.clone()).distinct(0, chars.length).toArray(), chars);
                }

                assertDistinct(Arrays.copyOfRange(chars, 1, len + 1), N.removeDuplicates(chars, 1, len + 1, false), chars);
                assertDistinct(Arrays.copyOfRange(shorts, 2, len + 2), N.removeDuplicates(shorts, 2, len + 2, false), shorts);
            }

            // the returned array is independent of the input
            final char[] src = { 'x', 'y', 'z' };
            final char[] res = N.distinct(src);
            res[0] = 'q';
            assertArrayEquals(new char[] { 'x', 'y', 'z' }, src);
            final byte[] srcBytes = { 1, 2, 3 };
            final byte[] resBytes = N.distinct(srcBytes);
            resBytes[0] = 9;
            assertArrayEquals(new byte[] { 1, 2, 3 }, srcBytes);
        }

        // G047-01: guard against a return of the per-call 8 KB bitmap for tiny ranges (base: ~8,300 bytes per call)
        @Test
        void testDistinct_charShortTinyRangeAllocatesNoBitmap() {
            final com.sun.management.ThreadMXBean threadMx = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
            final char[] chars = "abcabcdefghijklm".toCharArray();
            final short[] shorts = { 5, -5, 5, 7, 300, -300, 7, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            long sink = 0;

            for (int i = 0; i < 2000; i++) {
                sink += N.distinct(chars).length + N.distinct(shorts).length;
            }

            final long before = threadMx.getCurrentThreadAllocatedBytes();

            for (int i = 0; i < 2000; i++) {
                sink += N.distinct(chars).length + N.distinct(shorts).length;
            }

            final long perCall = (threadMx.getCurrentThreadAllocatedBytes() - before) / 4000;
            assertEquals(4000L * (13 + 12), sink);
            assertTrue(perCall < 2048, "N.distinct(char[16]/short[16]) allocated " + perCall + " bytes per call");
        }

        private char[] charModel(final char[] a, final int from, final int to) {
            final Set<Character> set = new LinkedHashSet<>();

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            final char[] result = new char[set.size()];
            int k = 0;

            for (final Character c : set) {
                result[k++] = c;
            }

            return result;
        }

        private short[] shortModel(final short[] a, final int from, final int to) {
            final Set<Short> set = new LinkedHashSet<>();

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            final short[] result = new short[set.size()];
            int k = 0;

            for (final Short s : set) {
                result[k++] = s;
            }

            return result;
        }

        private void assertDistinct(final char[] expected, final char[] actual, final char[] input) {
            assertArrayEquals(expected, actual);
            assertNotSame(input, actual);
        }

        private void assertDistinct(final short[] expected, final short[] actual, final short[] input) {
            assertArrayEquals(expected, actual);
            assertNotSame(input, actual);
        }

        private void assertDistinct(final byte[] expected, final byte[] actual, final byte[] input) {
            assertArrayEquals(expected, actual);
            assertNotSame(input, actual);
        }
    }

    // ---- perf review 2026-10-08 G047 end ----
    // ---- perf review 2026-10-08 G048 begin ----
    @Nested
    class G048 {

        private boolean referenceContainsDuplicates(final char[] a, final int fromIndex, final int toIndex) {
            final Set<Character> seen = new HashSet<>();

            for (int i = fromIndex; i < toIndex; i++) {
                if (!seen.add(a[i])) {
                    return true;
                }
            }

            return false;
        }

        private boolean referenceContainsDuplicates(final short[] a, final int fromIndex, final int toIndex) {
            final Set<Short> seen = new HashSet<>();

            for (int i = fromIndex; i < toIndex; i++) {
                if (!seen.add(a[i])) {
                    return true;
                }
            }

            return false;
        }

        private char[] distinctChars(final int n) {
            final char[] a = new char[n];

            // odd multiplier: n distinct values for n <= 65536, wrapping through 0xFFFF and 0x0000
            for (int i = 0; i < n; i++) {
                a[i] = (char) (i * 2053 + 0xFFF0);
            }

            return a;
        }

        private short[] distinctShorts(final int n) {
            final short[] a = new short[n];

            // odd multiplier: n distinct values for n <= 65536, crossing -1/0 and Short.MAX_VALUE/Short.MIN_VALUE
            for (int i = 0; i < n; i++) {
                a[i] = (short) (i * 2053 + 0x7FF0);
            }

            return a;
        }

        // G048-01: unsorted char ranges of at most 32 elements are compared pairwise instead of through the 8 KB bitmap;
        // every single duplicate pair, ranges that include/exclude one of its ends, both sides of the threshold.
        @Test
        void testContainsDuplicates_charEveryDuplicatePairAroundPairwiseThreshold() {
            for (int n = 0; n <= 70; n++) {
                final char[] a = distinctChars(n);

                assertFalse(N.containsDuplicates(a, false), "n=" + n);
                assertFalse(N.containsDuplicates(a), "n=" + n);
                assertFalse(CharList.of(a.clone()).containsDuplicates(), "n=" + n);

                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        final char[] b = a.clone();
                        b[j] = b[i];
                        final String msg = "n=" + n + ", pair=(" + i + ", " + j + ")";

                        assertTrue(N.containsDuplicates(b, false), msg);
                        assertTrue(N.containsDuplicates(b, i, j + 1, false), msg);
                        assertFalse(N.containsDuplicates(b, i + 1, n, false), msg);
                        assertFalse(N.containsDuplicates(b, 0, j, false), msg);
                        assertTrue(CharList.of(b).containsDuplicates(), msg);
                    }
                }
            }
        }

        // G048-01: same for short ranges.
        @Test
        void testContainsDuplicates_shortEveryDuplicatePairAroundPairwiseThreshold() {
            for (int n = 0; n <= 70; n++) {
                final short[] a = distinctShorts(n);

                assertFalse(N.containsDuplicates(a, false), "n=" + n);
                assertFalse(N.containsDuplicates(a), "n=" + n);
                assertFalse(ShortList.of(a.clone()).containsDuplicates(), "n=" + n);

                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        final short[] b = a.clone();
                        b[j] = b[i];
                        final String msg = "n=" + n + ", pair=(" + i + ", " + j + ")";

                        assertTrue(N.containsDuplicates(b, false), msg);
                        assertTrue(N.containsDuplicates(b, i, j + 1, false), msg);
                        assertFalse(N.containsDuplicates(b, i + 1, n, false), msg);
                        assertFalse(N.containsDuplicates(b, 0, j, false), msg);
                        assertTrue(ShortList.of(b).containsDuplicates(), msg);
                    }
                }
            }
        }

        // G048-01: random ranges over small and large alphabets (dense duplicates, few duplicates, none) match a HashSet scan.
        @Test
        void testContainsDuplicates_charAndShortRandomRangesMatchReference() {
            final Random rnd = new Random(20261008L);
            final int[] alphabets = { 1, 2, 5, 26, 40, 100, 1000, 65536 };

            for (int round = 0; round < 20000; round++) {
                final int length = rnd.nextInt(80);
                final int alphabet = alphabets[rnd.nextInt(alphabets.length)];
                final int base = rnd.nextInt(65536);
                final char[] c = new char[length];
                final short[] s = new short[length];

                for (int i = 0; i < length; i++) {
                    final int v = base + rnd.nextInt(alphabet);
                    c[i] = (char) v;
                    s[i] = (short) v;
                }

                final int fromIndex = length == 0 ? 0 : rnd.nextInt(length + 1);
                final int toIndex = fromIndex + rnd.nextInt(length - fromIndex + 1);
                final String msg = "round=" + round + ", c=" + Arrays.toString(c) + ", range=[" + fromIndex + ", " + toIndex + ")";

                assertEquals(referenceContainsDuplicates(c, fromIndex, toIndex), N.containsDuplicates(c, fromIndex, toIndex, false), msg);
                assertEquals(referenceContainsDuplicates(s, fromIndex, toIndex), N.containsDuplicates(s, fromIndex, toIndex, false), msg);
                assertEquals(referenceContainsDuplicates(c, 0, length), N.containsDuplicates(c, false), msg);
                assertEquals(referenceContainsDuplicates(s, 0, length), N.containsDuplicates(s, false), msg);
            }
        }

        // G048-01: a list's spare capacity (zeros after size) is outside the scanned range, also on the pairwise path.
        @Test
        void testContainsDuplicates_charAndShortListSpareCapacityIsNotScanned() {
            for (int n = 1; n <= 40; n++) {
                final CharList chars = new CharList(80);
                final ShortList shorts = new ShortList(80);

                chars.add('\u0000');
                shorts.add((short) 0);

                for (int i = 1; i < n; i++) {
                    chars.add((char) ('a' + i));
                    shorts.add((short) (-i));
                }

                assertFalse(chars.containsDuplicates(), "n=" + n);
                assertFalse(shorts.containsDuplicates(), "n=" + n);

                chars.add('\u0000');
                shorts.add((short) 0);

                assertTrue(chars.containsDuplicates(), "n=" + n);
                assertTrue(shorts.containsDuplicates(), "n=" + n);
            }
        }

        // G048-01: isSorted == true keeps comparing adjacent elements only (a non-adjacent duplicate is missed), and the
        // equal-value identity of short keys: (short) 0xFFFF is -1.
        @Test
        void testContainsDuplicates_charAndShortSortedFlagAndShortIdentity() {
            for (int n = 4; n <= 40; n++) {
                final char[] c = distinctChars(n);
                final short[] s = distinctShorts(n);
                c[n - 1] = c[0];
                s[n - 1] = s[0];

                assertFalse(N.containsDuplicates(c, true), "n=" + n);
                assertFalse(N.containsDuplicates(s, true), "n=" + n);
                assertTrue(N.containsDuplicates(c, false), "n=" + n);
                assertTrue(N.containsDuplicates(s, false), "n=" + n);
            }

            assertTrue(N.containsDuplicates(new short[] { 7, (short) 0xFFFF, 3, -1 }, false));
            assertFalse(N.containsDuplicates(new short[] { 7, Short.MIN_VALUE, 3, Short.MAX_VALUE, -1, 0 }, false));
            assertTrue(N.containsDuplicates(new char[] { 'x', '\uFFFF', 'y', (char) -1 }, false));
            assertFalse(N.containsDuplicates(new char[] { 'x', '\uFFFF', 'y', '\u0000', '\uD83D', '\uDE80' }, false));
        }

        // G048-01: range validation is unchanged (before any scan) and a null array is length zero.
        @Test
        void testContainsDuplicates_charAndShortRangeValidation() {
            final char[] c = { 'a', 'b', 'a', 'c' };
            final short[] s = { 1, 2, 1, 3 };

            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(c, -1, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(c, 3, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(c, 0, 5, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(s, -1, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(s, 3, 2, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates(s, 0, 5, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates((char[]) null, 0, 1, false));
            assertThrows(IndexOutOfBoundsException.class, () -> N.containsDuplicates((short[]) null, 0, 1, false));
            assertFalse(N.containsDuplicates((char[]) null, 0, 0, false));
            assertFalse(N.containsDuplicates((short[]) null, 0, 0, false));
            assertFalse(N.containsDuplicates((char[]) null, false));
            assertFalse(N.containsDuplicates((short[]) null, false));
            assertTrue(N.containsDuplicates(c, 0, 4, false));
            assertFalse(N.containsDuplicates(c, 1, 4, false));
            assertTrue(N.containsDuplicates(s, 0, 3, false));
            assertFalse(N.containsDuplicates(s, 1, 4, false));
        }
    }

    // ---- perf review 2026-10-08 G048 end ----
    // ---- perf review 2026-10-08 G050 begin ----
    @Nested
    class G050 {

        private Integer[] sample(final int size) {
            final Integer[] a = new Integer[size];

            for (int i = 0; i < size; i++) {
                a[i] = i % 7 == 3 ? null : Integer.valueOf(i % 5); // nulls and duplicates
            }

            return a;
        }

        private List<Integer> suffix(final Integer[] a, final int fromIndex) {
            final List<Integer> expected = new ArrayList<>();

            for (int i = fromIndex; i < a.length; i++) {
                expected.add(a[i]);
            }

            return expected;
        }

        private void assertFreshMutableArrayList(final List<Integer> result, final List<Integer> expected) {
            assertEquals(ArrayList.class, result.getClass());
            assertEquals(expected, result);

            result.add(-1);
            result.add(0, -2);
            assertEquals(expected.size() + 2, result.size());
            assertEquals(Integer.valueOf(-2), result.get(0));
            assertEquals(Integer.valueOf(-1), result.get(result.size() - 1));
        }

        // G050-01: dropWhile(T[]) allocates its result once the dropped prefix is known; contents, list type and predicate calls are unchanged.
        @Test
        public void testDropWhile_arrayKeepsSuffixForEveryPrefixLength() {
            for (final int size : new int[] { 1, 2, 3, 8, 9, 10, 11, 17, 1000 }) {
                final Integer[] a = sample(size);
                final Integer[] copy = a.clone();

                for (int dropCount = 0; dropCount <= size; dropCount++) {
                    final int limit = dropCount;
                    final List<Integer> calls = new ArrayList<>();
                    final AtomicInteger counter = new AtomicInteger();

                    final List<Integer> result = N.dropWhile(a, e -> {
                        calls.add(e);
                        return counter.getAndIncrement() < limit;
                    });

                    assertFreshMutableArrayList(result, suffix(a, dropCount));
                    assertEquals(Arrays.asList(a).subList(0, Math.min(dropCount + 1, size)), calls);
                    assertArrayEquals(copy, a);
                }
            }

            final Integer[] big = sample(100_000);

            for (final int dropCount : new int[] { 0, 1, 50_000, 99_999, 100_000 }) {
                final AtomicInteger counter = new AtomicInteger();
                final List<Integer> result = N.dropWhile(big, e -> counter.getAndIncrement() < dropCount);

                assertEquals(suffix(big, dropCount), result);
                assertEquals(Math.min(dropCount + 1, big.length), counter.get());
            }
        }

        // G050-01: skipUntil(T[]) allocates its result once the skipped prefix is known; contents, list type and predicate calls are unchanged.
        @Test
        public void testSkipUntil_arrayKeepsSuffixForEveryPrefixLength() {
            for (final int size : new int[] { 1, 2, 3, 8, 9, 10, 11, 17, 1000 }) {
                final Integer[] a = sample(size);
                final Integer[] copy = a.clone();

                for (int skipCount = 0; skipCount <= size; skipCount++) {
                    final int limit = skipCount;
                    final List<Integer> calls = new ArrayList<>();
                    final AtomicInteger counter = new AtomicInteger();

                    final List<Integer> result = N.skipUntil(a, e -> {
                        calls.add(e);
                        return counter.getAndIncrement() >= limit;
                    });

                    assertFreshMutableArrayList(result, suffix(a, skipCount));
                    assertEquals(Arrays.asList(a).subList(0, Math.min(skipCount + 1, size)), calls);
                    assertArrayEquals(copy, a);
                }
            }

            final Integer[] big = sample(100_000);

            for (final int skipCount : new int[] { 0, 1, 50_000, 99_999, 100_000 }) {
                final AtomicInteger counter = new AtomicInteger();
                final List<Integer> result = N.skipUntil(big, e -> counter.getAndIncrement() >= skipCount);

                assertEquals(suffix(big, skipCount), result);
                assertEquals(Math.min(skipCount + 1, big.length), counter.get());
            }
        }

        // G050-01: empty / null arrays: a fresh empty mutable ArrayList and no predicate call.
        @Test
        public void testDropWhileAndSkipUntil_emptyAndNullArrays() {
            final AtomicInteger calls = new AtomicInteger();

            for (final Integer[] a : new Integer[][] { null, {} }) {
                final List<Integer> dropped = N.dropWhile(a, e -> calls.incrementAndGet() > 0);
                final List<Integer> skipped = N.skipUntil(a, e -> calls.incrementAndGet() > 0);

                assertFreshMutableArrayList(dropped, Collections.emptyList());
                assertFreshMutableArrayList(skipped, Collections.emptyList());
                assertNotSame(dropped, N.dropWhile(a, e -> true));
            }

            assertEquals(0, calls.get());
            assertThrows(IllegalArgumentException.class, () -> N.dropWhile(new Integer[] { 1 }, null));
            assertThrows(IllegalArgumentException.class, () -> N.skipUntil(new Integer[] { 1 }, null));
        }

        // G050-01: a throwing predicate propagates its own exception after the same calls; nothing is returned.
        @Test
        public void testDropWhileAndSkipUntil_arrayThrowingPredicate() {
            final Integer[] a = sample(20);

            for (int failAt = 0; failAt < a.length; failAt++) {
                final int limit = failAt;
                final RuntimeException failure = new IllegalStateException("fail at " + failAt);
                final AtomicInteger dropCalls = new AtomicInteger();
                final AtomicInteger skipCalls = new AtomicInteger();

                assertSame(failure, assertThrows(IllegalStateException.class, () -> N.dropWhile(a, e -> {
                    if (dropCalls.getAndIncrement() == limit) {
                        throw failure;
                    }

                    return true;
                })));

                assertSame(failure, assertThrows(IllegalStateException.class, () -> N.skipUntil(a, e -> {
                    if (skipCalls.getAndIncrement() == limit) {
                        throw failure;
                    }

                    return false;
                })));

                assertEquals(failAt + 1, dropCalls.get());
                assertEquals(failAt + 1, skipCalls.get());
            }
        }

        // G050-01: the kept elements are read from the array after the predicate loop, so writes made by the predicate are seen.
        @Test
        public void testDropWhileAndSkipUntil_arrayPredicateWritingToTheArray() {
            final Integer[] a = { 1, 2, 3, 4, 5 };

            final List<Integer> dropped = N.dropWhile(a, e -> {
                a[4] = e * 100; // overwrites a kept element while dropping
                a[2] = e == 3 ? 30 : a[2]; // overwrites the first kept element while it is tested
                return e < 3;
            });

            assertEquals(Arrays.asList(30, 4, 300), dropped);

            final Integer[] b = { 1, 2, 3, 4, 5 };

            final List<Integer> skipped = N.skipUntil(b, e -> {
                b[3] = e * 10;
                b[2] = e == 3 ? -3 : b[2];
                return e >= 3;
            });

            assertEquals(Arrays.asList(-3, 30, 5), skipped);
        }
    }

    // ---- perf review 2026-10-08 G050 end ----
    // ---- perf review 2026-10-08 G051 begin ----
    @Nested
    class G051 {
        private String recordList(final int records) {
            final StringBuilder sb = new StringBuilder("<list>");

            for (int i = 0; i < records; i++) {
                sb.append("\n  <item id=\"")
                        .append(i)
                        .append("\" note=\"a&amp;b &lt;i&gt; &quot;q&quot;\"><name>n&amp;")
                        .append(i)
                        .append("</name><value>")
                        .append(i)
                        .append("</value></item>");
            }

            return sb.append("\n</list>").toString();
        }

        private String expectedRecordList(final int records, final String indentation) {
            final StringBuilder sb = new StringBuilder("<list>");

            for (int i = 0; i < records; i++) {
                sb.append('\n').append(indentation).append("<item id=\"").append(i).append("\" note=\"a&amp;b &lt;i&gt; &quot;q&quot;\">");
                sb.append('\n').append(indentation).append(indentation).append("<name>n&amp;").append(i).append("</name>");
                sb.append('\n').append(indentation).append(indentation).append("<value>").append(i).append("</value>");
                sb.append('\n').append(indentation).append("</item>");
            }

            return sb.append("\n</list>").toString();
        }

        // G051-01: wide element in the one-child-per-line layout (children are now walked by sibling links)
        @Test
        void testFormatXml_wideRecordList_layoutUnchanged() {
            for (final int records : new int[] { 0, 1, 2, 3, 2500 }) {
                final String xml = recordList(records);

                assertEquals(expectedRecordList(records, "    "), N.formatXml(xml), "records=" + records);
                assertEquals(expectedRecordList(records, "    "), N.formatXml(xml, (com.landawn.abacus.parser.XmlSerConfig) null), "records=" + records);
                assertEquals(expectedRecordList(records, "\t"), N.formatXml(xml, com.landawn.abacus.parser.XmlSerConfig.create().setIndentation("\t")),
                        "records=" + records);
            }
        }

        // G051-01: wide mixed-content and xml:space="preserve" elements are still written verbatim
        @Test
        void testFormatXml_wideVerbatimContent_unchanged() {
            final StringBuilder mixed = new StringBuilder();

            for (int i = 0; i < 2500; i++) {
                mixed.append("t&amp;").append(i).append("<b>x<i>y</i></b>");
            }

            assertEquals("<root>\n    <p>" + mixed + "</p>\n</root>", N.formatXml("<root><p>" + mixed + "</p></root>"));

            final StringBuilder preserved = new StringBuilder();

            for (int i = 0; i < 2500; i++) {
                preserved.append("\n  <k a=\"").append(i).append("\"><v>").append(i).append("</v><v/></k>");
            }

            assertEquals("<root>\n    <s xml:space=\"preserve\">" + preserved + "\n</s>\n</root>",
                    N.formatXml("<root><s xml:space=\"preserve\">" + preserved + "\n</s></root>"));
        }

        // G051-01: deep nesting, every level holding whitespace text plus one element child
        @Test
        void testFormatXml_deepChain_unchanged() {
            final int depth = 60;
            final StringBuilder xml = new StringBuilder();
            final StringBuilder expected = new StringBuilder();

            for (int i = 0; i < depth; i++) {
                xml.append("<e i=\"").append(i).append("\">\n ");

                if (i > 0) {
                    expected.append('\n').append("  ".repeat(i));
                }

                expected.append("<e i=\"").append(i).append("\">");
            }

            xml.append("<leaf>x</leaf><leaf/>");
            expected.append('\n').append("  ".repeat(depth)).append("<leaf>x</leaf>");
            expected.append('\n').append("  ".repeat(depth)).append("<leaf/>");

            for (int i = depth - 1; i >= 0; i--) {
                xml.append(" </e>");
                expected.append('\n').append("  ".repeat(i)).append("</e>");
            }

            assertEquals(expected.toString(), N.formatXml(xml.toString(), com.landawn.abacus.parser.XmlSerConfig.create().setIndentation("  ")));
        }

        // G051-01: every child kind, empty elements, attribute escapes and top-level misc nodes (output recorded on the pristine build)
        @Test
        void testFormatXml_childKinds_unchanged() {
            final String ideographicSpace = String.valueOf((char) 0x3000);
            final String kinds = "<!-- top --><root a=\"x&#9;y&#10;z&#13;\" b=\"&lt;&amp;&gt;&quot;'\">\n"
                    + "  <!-- c1 -->\n  <?pi data 1?>\n  <?nodata?>\n  <e/>\n  <w>  </w>\n  <c><![CDATA[<x>&]]></c>\n"
                    + "  <s xml:space=\"preserve\">\n    <k>1</k>\n  </s>\n  <n><m><o>1</o><o>2</o></m><m/></n>\n"
                    + "  <t>a&#13;b&gt;c&#12288;</t>\n  <mix>t<b>x</b>u</mix>\n</root><?tail end?>";

            assertEquals("<!-- top -->\n<root a=\"x&#9;y&#10;z&#13;\" b=\"&lt;&amp;&gt;&quot;'\">\n    <!-- c1 -->\n    <?pi data 1?>\n    <?nodata?>\n"
                    + "    <e/>\n    <w>  </w>\n    <c><![CDATA[<x>&]]></c>\n    <s xml:space=\"preserve\">\n    <k>1</k>\n  </s>\n    <n>\n        <m>\n"
                    + "            <o>1</o>\n            <o>2</o>\n        </m>\n        <m/>\n    </n>\n    <t>a&#13;b&gt;c" + ideographicSpace + "</t>\n"
                    + "    <mix>t<b>x</b>u</mix>\n</root>\n<?tail end?>", N.formatXml(kinds));

            assertEquals(
                    "<!-- top -->\n<root a=\"x&#9;y&#10;z&#13;\" b=\"&lt;&amp;&gt;&quot;'\">\n\t<!-- c1 -->\n\t<?pi data 1?>\n\t<?nodata?>\n\t<e/>\n"
                            + "\t<w>  </w>\n\t<c><![CDATA[<x>&]]></c>\n\t<s xml:space=\"preserve\">\n    <k>1</k>\n  </s>\n\t<n>\n\t\t<m>\n\t\t\t<o>1</o>\n"
                            + "\t\t\t<o>2</o>\n\t\t</m>\n\t\t<m/>\n\t</n>\n\t<t>a&#13;b&gt;c" + ideographicSpace
                            + "</t>\n\t<mix>t<b>x</b>u</mix>\n</root>\n<?tail end?>",
                    N.formatXml(kinds, com.landawn.abacus.parser.XmlSerConfig.create().setIndentation("\t")));
        }

        // G051-01: XML 1.1 declaration and character references in a structured document (output recorded on the pristine build)
        @Test
        void testFormatXml_xml11Structure_unchanged() {
            final String xml = "<?xml version=\"1.1\"?><r>\n<c k=\"&#1;\">&#133;x&#8232;</c>\n<c k=\"&#127;\"><d>&#2;</d><d/></c>\n</r>";

            assertEquals("<?xml version=\"1.1\"?>\n<r>\n    <c k=\"&#1;\">&#133;x&#8232;</c>\n    <c k=\"&#127;\">\n        <d>&#2;</d>\n        <d/>\n"
                    + "    </c>\n</r>", N.formatXml(xml));
        }
    }

    // ---- perf review 2026-10-08 G051 end ----
    // ---- perf review 2026-10-08 G054 begin ----
    @Nested
    class G054 {

        /**
         * The exact binary value truncated toward zero, built the slow way (the reference the fast path must match).
         */
        private java.math.BigInteger exactTruncation(final double d) {
            return new java.math.BigDecimal(d).toBigInteger();
        }

        /**
         * A {@code Number} subtype with an unusable {@code toString()}, so an integral or {@code BigInteger} target falls back to its
         * {@code doubleValue()}.
         */
        private Number opaque(final double d) {
            return new Number() {
                private static final long serialVersionUID = 1L;

                @Override
                public int intValue() {
                    return (int) d;
                }

                @Override
                public long longValue() {
                    return (long) d;
                }

                @Override
                public float floatValue() {
                    return (float) d;
                }

                @Override
                public double doubleValue() {
                    return d;
                }

                @Override
                public String toString() {
                    return "n/a";
                }
            };
        }

        private void assertDoubleToBigInteger(final double d) {
            final String why = d + " / bits " + Long.toHexString(Double.doubleToRawLongBits(d));
            final com.landawn.abacus.type.Type<java.math.BigInteger> bigIntegerType = com.landawn.abacus.type.Type.of(java.math.BigInteger.class);

            if (Double.isNaN(d) || Double.isInfinite(d)) {
                final ArithmeticException e1 = assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.valueOf(d), java.math.BigInteger.class),
                        why);
                assertEquals("BigInteger overflow: " + Double.valueOf(d), e1.getMessage(), why);
                final ArithmeticException e2 = assertThrows(ArithmeticException.class, () -> Numbers.convert(Double.valueOf(d), bigIntegerType), why);
                assertEquals("BigInteger overflow: " + Double.valueOf(d), e2.getMessage(), why);
                final ArithmeticException e3 = assertThrows(ArithmeticException.class, () -> Numbers.convert(opaque(d), java.math.BigInteger.class), why);
                assertEquals("BigInteger overflow: n/a", e3.getMessage(), why);
                return;
            }

            final java.math.BigInteger expected = exactTruncation(d);

            assertEquals(expected, Numbers.convert(Double.valueOf(d), java.math.BigInteger.class), why);
            assertEquals(expected, Numbers.convert(Double.valueOf(d), bigIntegerType), why);
            assertEquals(expected, Numbers.convert(Double.valueOf(d), java.math.BigInteger.class, java.math.BigInteger.TEN), why);
            assertEquals(expected, Numbers.convert(opaque(d), java.math.BigInteger.class), why);
            assertEquals(expected, CommonUtil.convert(Double.valueOf(d), java.math.BigInteger.class), why);
        }

        private void assertFloatToBigInteger(final float f) {
            final String why = f + "f / bits " + Integer.toHexString(Float.floatToRawIntBits(f));

            if (Float.isNaN(f) || Float.isInfinite(f)) {
                final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Numbers.convert(Float.valueOf(f), java.math.BigInteger.class), why);
                assertEquals("BigInteger overflow: " + Float.valueOf(f), e.getMessage(), why);
                return;
            }

            // A float widens to double exactly, so the reference is the exact expansion of that double.
            final java.math.BigInteger expected = exactTruncation(f);

            assertEquals(expected, Numbers.convert(Float.valueOf(f), java.math.BigInteger.class), why);
            assertEquals(expected, Numbers.convert(Float.valueOf(f), com.landawn.abacus.type.Type.of(java.math.BigInteger.class)), why);
        }

        // G054-01: doubleToBigInteger fast path inside the long range must equal the exact BigDecimal expansion.
        @Test
        public void testConvertDoubleToBigInteger_boundariesMatchExactTruncation() {
            final double twoTo63 = 0x1p63;
            final double[] values = { 0.0d, -0.0d, Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_NORMAL, 0.5d, -0.5d, Math.nextDown(1.0d),
                    -Math.nextDown(1.0d), 1.0d, -1.0d, Math.nextUp(1.0d), -Math.nextUp(1.0d), 1.5d, -1.5d, 1.9999999999999998d, 2.5d, -2.5d, 12.9d, -12.9d,
                    123456.75d, -123456.75d, 2147483647.9d, 2147483648.5d, -2147483648.9d, 0x1p52 + 0.5d, -(0x1p52 + 0.5d), 0x1p53, 0x1p53 + 2, -0x1p53,
                    9007199254740994.0d, 1000000000000000128.0d, -1000000000000000128.0d, 4611686018427388928.0d, -4611686018427388928.0d,
                    Math.nextDown(twoTo63), -Math.nextDown(twoTo63), twoTo63, -twoTo63, Math.nextUp(twoTo63), Math.nextDown(-twoTo63), 1e19d, -1e19d, 1e30d,
                    -1e30d, 0x1p64, -0x1p64, Double.MAX_VALUE, -Double.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
                    Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.longBitsToDouble(0x7ff8000000000001L) };

            for (final double d : values) {
                assertDoubleToBigInteger(d);
            }

            // Named answers, independent of the reference expansion.
            assertEquals(java.math.BigInteger.valueOf(Long.MIN_VALUE), Numbers.convert(-twoTo63, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(9223372036854774784L), Numbers.convert(Math.nextDown(twoTo63), java.math.BigInteger.class));
            assertEquals(new java.math.BigInteger("9223372036854775808"), Numbers.convert(twoTo63, java.math.BigInteger.class));
            assertEquals(new java.math.BigInteger("-9223372036854777856"), Numbers.convert(Math.nextDown(-twoTo63), java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(12), Numbers.convert(12.9d, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(-12), Numbers.convert(-12.9d, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(2147483648L), Numbers.convert(2147483648.5d, java.math.BigInteger.class));
            assertSame(java.math.BigInteger.ZERO, Numbers.convert(-0.0d, java.math.BigInteger.class));
            assertSame(java.math.BigInteger.ZERO, Numbers.convert(-0.999d, java.math.BigInteger.class));
            assertEquals(0, Numbers.convert(-0.5d, java.math.BigInteger.class).signum());
        }

        // G054-01: random doubles (raw bit patterns and magnitudes on both sides of 2^63) match the exact expansion.
        @Test
        public void testConvertDoubleToBigInteger_randomMatchesExactTruncation() {
            final Random random = new Random(20261008L);

            for (int i = 0; i < 20_000; i++) {
                assertDoubleToBigInteger(Double.longBitsToDouble(random.nextLong()));
            }

            for (int i = 0; i < 40_000; i++) {
                // Exponents from 2^-3 to 2^70: below one, the whole long range, the 2^63 edge and beyond it.
                double d = Math.scalb(1.0d + random.nextDouble(), random.nextInt(74) - 3);

                if (random.nextBoolean()) {
                    d = -d;
                }

                assertDoubleToBigInteger(d);
            }

            for (int i = 0; i < 2_000; i++) {
                // The doubles adjacent to +-2^63.
                final double edge = random.nextBoolean() ? 0x1p63 : -0x1p63;
                double d = edge;
                final int steps = random.nextInt(8);

                for (int s = 0; s < steps; s++) {
                    d = random.nextBoolean() ? Math.nextUp(d) : Math.nextDown(d);
                }

                assertDoubleToBigInteger(d);
            }
        }

        // G054-01: Float sources take the same path (widened exactly to double).
        @Test
        public void testConvertFloatToBigInteger_matchesExactTruncation() {
            final float[] values = { 0.0f, -0.0f, Float.MIN_VALUE, -Float.MIN_VALUE, 0.5f, -0.5f, Math.nextDown(1.0f), 1.0f, -1.0f, 1.5f, -1.5f, 12.9f, -12.9f,
                    1.21f, 16777217f, 33554448f, -33554448f, Integer.MAX_VALUE, Integer.MIN_VALUE, 0x1p63f, -0x1p63f, Math.nextDown(0x1p63f),
                    -Math.nextDown(0x1p63f), Math.nextUp(0x1p63f), Math.nextDown(-0x1p63f), 1e19f, -1e19f, 1e30f, Float.MAX_VALUE, -Float.MAX_VALUE, Float.NaN,
                    Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY };

            for (final float f : values) {
                assertFloatToBigInteger(f);
            }

            final Random random = new Random(8100L);

            for (int i = 0; i < 20_000; i++) {
                assertFloatToBigInteger(Float.intBitsToFloat(random.nextInt()));
            }

            for (int i = 0; i < 20_000; i++) {
                float f = Math.scalb(1.0f + random.nextFloat(), random.nextInt(70) - 3);

                if (random.nextBoolean()) {
                    f = -f;
                }

                assertFloatToBigInteger(f);
            }

            assertEquals(java.math.BigInteger.valueOf(2147483648L), Numbers.convert((float) Integer.MAX_VALUE, java.math.BigInteger.class));
            assertEquals(java.math.BigInteger.valueOf(Long.MIN_VALUE), Numbers.convert(-0x1p63f, java.math.BigInteger.class));
            assertEquals(new java.math.BigInteger("9223372036854775808"), Numbers.convert(0x1p63f, java.math.BigInteger.class));
        }
    }

    // ---- perf review 2026-10-08 G054 end ----
    // ---- perf review 2026-10-08 G055 begin ----
    @Nested
    class G055 {

        /**
         * Independent exact reference for C(n, k): after step {@code i} the running value is C(n, i + 1), so every
         * division is exact.
         */
        private java.math.BigInteger referenceBinomial(final int n, final int k) {
            java.math.BigInteger result = java.math.BigInteger.ONE;

            for (int i = 0; i < k; i++) {
                result = result.multiply(java.math.BigInteger.valueOf((long) n - i)).divide(java.math.BigInteger.valueOf(i + 1L));
            }

            return result;
        }

        // G055-01: around the lower edge of the product window (k = 64), both orientations, small to maximal n
        @Test
        void testBinomialToBigInteger_lowerWindowEdgeMatchesReference() {
            final int[] ns = { 116, 126, 127, 128, 129, 130, 131, 140, 200, 1000, 12345, 65536, 1 << 20, Integer.MAX_VALUE - 1, Integer.MAX_VALUE };

            for (final int n : ns) {
                for (int k = 56; k <= 72 && k <= n; k++) {
                    final int kk = k;
                    final java.math.BigInteger expected = referenceBinomial(n, Math.min(k, n - k));

                    assertEquals(expected, Numbers.binomialToBigInteger(n, k), () -> "C(" + n + ", " + kk + ")");
                    assertEquals(expected, Numbers.binomialToBigInteger(n, n - k), () -> "C(" + n + ", " + (n - kk) + ")");
                }
            }
        }

        // G055-01: fixed-seed random shapes inside the window, both orientations
        @Test
        void testBinomialToBigInteger_randomWindowShapesMatchReference() {
            final Random random = new Random(55L);

            for (int i = 0; i < 90; i++) {
                final int k = 64 + random.nextInt(i % 10 == 0 ? 2500 : 600);
                final int n = i % 3 == 0 ? 2 * k + random.nextInt(3 * k)
                        : i % 3 == 1 ? 2 * k + random.nextInt(1 << 22) : Integer.MAX_VALUE - random.nextInt(1 << 16);
                final java.math.BigInteger expected = referenceBinomial(n, k);

                assertEquals(expected, Numbers.binomialToBigInteger(n, k), () -> "C(" + n + ", " + k + ")");
                assertEquals(expected, Numbers.binomialToBigInteger(n, n - k), () -> "C(" + n + ", " + (n - k) + ")");
            }

            assertEquals(referenceBinomial(5000, 2500), Numbers.binomialToBigInteger(5000, 2500));
            assertEquals(referenceBinomial(20001, 10000), Numbers.binomialToBigInteger(20001, 10001));
            assertEquals(referenceBinomial(1_000_000, 3000), Numbers.binomialToBigInteger(1_000_000, 3000));
        }

        // G055-01: both window edges cross-checked against the batched path through exact identities
        @Test
        void testBinomialToBigInteger_windowEdgesAgreeWithBatchedPath() {
            // C(n, 64) * 64 == C(n, 63) * (n - 63): k = 63 stays on the batched loop, k = 64 takes the products.
            for (final int n : new int[] { 128, 129, 130, 1000, 99991, Integer.MAX_VALUE }) {
                assertEquals(Numbers.binomialToBigInteger(n, 63).multiply(java.math.BigInteger.valueOf(n - 63L)),
                        Numbers.binomialToBigInteger(n, 64).multiply(java.math.BigInteger.valueOf(64)), "n = " + n);
            }

            // Pascal's rule across the upper edge: C(131074, 65537) keeps k = 65537 (batched loop), while
            // C(131073, 65536) and C(131073, 65537) both reduce to k = 65536, the last k of the product window.
            assertEquals(Numbers.binomialToBigInteger(131074, 65537),
                    Numbers.binomialToBigInteger(131073, 65536).add(Numbers.binomialToBigInteger(131073, 65537)));

            // Symmetry and argument checks are unchanged.
            assertEquals(Numbers.binomialToBigInteger(131073, 65536), Numbers.binomialToBigInteger(131073, 65537));
            assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(1000, 1001));
            assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(1000, -64));
            assertThrows(IllegalArgumentException.class, () -> Numbers.binomialToBigInteger(-1, 64));
        }
    }

    // ---- perf review 2026-10-08 G055 end ----
    // ---- perf review 2026-10-08 G058 begin ----
    @Nested
    class G058 {

        /** Fast-path candidates and every reason ReflectASM declines a call; each method records what ran. */
        public static class Target {
            public int calls;
            public String selected;

            public String first() {
                mark("first");
                return "first";
            }

            public String one(final String s) {
                mark("one");
                return "one:" + s;
            }

            public String same(final String s) {
                mark("sameS");
                return "sameS:" + s;
            }

            public String same(final Integer i) {
                mark("sameI");
                return "sameI:" + i;
            }

            public String arity(final int a) {
                mark("arity1");
                return "arity1:" + a;
            }

            public String arity(final int a, final int b) {
                mark("arity2");
                return "arity2:" + (a + b);
            }

            public static String stat(final String s) {
                return "stat:" + s;
            }

            public String vararg(final String... values) {
                mark("vararg");
                return "vararg:" + values.length;
            }

            public String hidden(final Object o) {
                mark("hiddenO");
                return "hiddenO:" + o;
            }

            @SuppressWarnings("unused")
            private String hidden(final String s) {
                mark("hiddenS");
                return "hiddenS:" + s;
            }

            public long widen(final long v) {
                mark("widen");
                return v + 1;
            }

            public String boxed(final Integer v) {
                mark("boxed");
                return "boxed:" + v;
            }

            public int prim(final int v) {
                mark("prim");
                return v * 2;
            }

            public String fail(final String s) throws java.io.IOException {
                mark("fail");
                throw new java.io.IOException("io:" + s);
            }

            public void setter(final String s) {
                mark("setter:" + s);
            }

            public String last() {
                mark("last");
                return "last";
            }

            @Override
            public String toString() {
                return "Target";
            }

            void mark(final String s) {
                calls++;
                selected = s;
            }
        }

        /** Overrides first() (two table entries for the name) and inherits the rest. */
        public static class Child extends Target {
            @Override
            public String first() {
                mark("childFirst");
                return "childFirst";
            }

            public String childOnly(final String s) {
                mark("childOnly");
                return "childOnly:" + s;
            }
        }

        /** Many uniquely named methods, so the selected method's position in the table varies widely. */
        public static class Wide {
            public int calls;

            public String w00() {
                calls++;
                return "w00";
            }

            public String w01() {
                calls++;
                return "w01";
            }

            public String w02() {
                calls++;
                return "w02";
            }

            public String w03() {
                calls++;
                return "w03";
            }

            public String w04() {
                calls++;
                return "w04";
            }

            public String w05() {
                calls++;
                return "w05";
            }

            public String w06() {
                calls++;
                return "w06";
            }

            public String w07() {
                calls++;
                return "w07";
            }

            public String w08() {
                calls++;
                return "w08";
            }

            public String w09() {
                calls++;
                return "w09";
            }

            public String w10() {
                calls++;
                return "w10";
            }

            public String w11() {
                calls++;
                return "w11";
            }

            public String w12() {
                calls++;
                return "w12";
            }

            public String w13() {
                calls++;
                return "w13";
            }

            public String w14() {
                calls++;
                return "w14";
            }

            public String w15() {
                calls++;
                return "w15";
            }

            public String w16() {
                calls++;
                return "w16";
            }

            public String w17() {
                calls++;
                return "w17";
            }

            public String w18() {
                calls++;
                return "w18";
            }

            public String w19() {
                calls++;
                return "w19";
            }

            public String w20(final String s) {
                calls++;
                return "w20:" + s;
            }

            public String w21(final String s, final Integer i) {
                calls++;
                return "w21:" + s + i;
            }

            public String w22(final int i) {
                calls++;
                return "w22:" + i;
            }

            public String w23() {
                calls++;
                return "w23";
            }
        }

        private static void assertCanInvoke(final boolean expected, final Object target, final String name, final Object... arguments) {
            assertEquals(expected, ReflectASM.on(target).canInvoke(name, arguments), () -> target.getClass().getSimpleName() + "." + name);
        }

        // G058-01: canInvoke answers from the per-class name index exactly as the full table scan did
        @Test
        void testCanInvoke_nameIndexMatchesTableScanRules() {
            final Target t = new Target();
            assertCanInvoke(true, t, "first");
            assertCanInvoke(true, t, "last");
            assertCanInvoke(true, t, "one", "x");
            assertCanInvoke(true, t, "one", (Object) null);
            assertCanInvoke(false, t, "one");
            assertCanInvoke(false, t, "one", 5);
            assertCanInvoke(false, t, "one", "x", "y");
            assertCanInvoke(false, t, "same", "x");
            assertCanInvoke(false, t, "same", 1);
            assertCanInvoke(true, t, "arity", 1);
            assertCanInvoke(true, t, "arity", 1, 2);
            assertCanInvoke(false, t, "arity", (Object) null);
            assertCanInvoke(false, t, "arity", "1");
            assertCanInvoke(true, t, "stat", "x");
            assertCanInvoke(true, t, "vararg", (Object) new String[] { "a" });
            assertCanInvoke(false, t, "vararg", "a");
            assertCanInvoke(false, t, "hidden", "x");
            assertCanInvoke(false, t, "hidden", new Object());
            assertCanInvoke(false, t, "widen", 5);
            assertCanInvoke(true, t, "widen", 5L);
            assertCanInvoke(true, t, "boxed", 5);
            assertCanInvoke(true, t, "prim", 3);
            assertCanInvoke(false, t, "prim", (Object) null);
            assertCanInvoke(true, t, "toString");
            assertCanInvoke(false, t, "hashCode");
            assertCanInvoke(false, t, "getClass");
            assertCanInvoke(false, t, "noSuchMethod");
            assertCanInvoke(false, t, "");
            assertTrue(ReflectASM.on(t).canInvoke("first", (Object[]) null));
            assertFalse(ReflectASM.on(t).canInvoke("one", (Object[]) null));

            final Child c = new Child();
            assertCanInvoke(false, c, "first");
            assertCanInvoke(true, c, "childOnly", "x");
            assertCanInvoke(true, c, "one", "x");
            assertCanInvoke(true, c, "last");

            assertCanInvoke(true, "hello", "isEmpty");
            assertCanInvoke(true, "hello", "length");
            assertCanInvoke(false, new ArrayList<>(), "size");
            assertFalse(ReflectASM.on(java.util.Comparator.class).canInvoke("naturalOrder"));

            // repeated lookups (served from the cached index) give the same answers
            for (int i = 0; i < 3; i++) {
                assertCanInvoke(true, t, "arity", 1, 2);
                assertCanInvoke(false, t, "same", "x");
                assertCanInvoke(false, t, "noSuchMethod" + i);
            }
        }

        // G058-01: the fast path runs exactly the method the name lookup selected, once, wherever it sits in the table
        @Test
        void testInvoke_fastPathSelectsSameMethodAtEveryTablePosition() {
            final Wide wide = new Wide();
            int expectedCalls = 0;

            for (int round = 0; round < 2; round++) {
                for (int i = 0; i < 24; i++) {
                    final String name = (i < 10 ? "w0" : "w") + i;
                    final Object[] arguments = i == 20 ? new Object[] { "s" }
                            : i == 21 ? new Object[] { "s", 7 } : i == 22 ? new Object[] { 9 } : new Object[0];
                    final String expected = i == 20 ? "w20:s" : i == 21 ? "w21:s7" : i == 22 ? "w22:9" : name;

                    assertTrue(ReflectASM.on(wide).canInvoke(name, arguments), name);
                    assertEquals(expected, Reflection.on(wide).invoke(name, arguments));
                    assertEquals(++expectedCalls, wide.calls, name);

                    final Reflection<Wide> reflection = Reflection.on(wide);
                    assertSame(reflection, reflection.call(name, arguments));
                    assertEquals(++expectedCalls, wide.calls, name);
                }
            }

            for (final String name : new String[] { "isEmpty", "length", "isBlank", "toUpperCase", "trim", "strip", "hashCode", "intern" }) {
                final Object viaReflection = Reflection.on((Object) "Hello").invoke(name);
                assertEquals(directStringCall("Hello", name), viaReflection, name);
            }
        }

        private Object directStringCall(final String s, final String name) {
            switch (name) {
                case "isEmpty":
                    return s.isEmpty();
                case "length":
                    return s.length();
                case "isBlank":
                    return s.isBlank();
                case "toUpperCase":
                    return s.toUpperCase();
                case "trim":
                    return s.trim();
                case "strip":
                    return s.strip();
                case "hashCode":
                    return s.hashCode();
                case "intern":
                    return s.intern();
                default:
                    throw new IllegalArgumentException(name);
            }
        }

        // G058-01: results, selected overload and call counts of invoke/call on both paths (fast path and fallback)
        @Test
        void testInvokeAndCall_resultsAndCallbacksOnBothPaths() {
            final String[][] cases = {
                    // name, expected result, expected selected
                    { "first", "first", "first" }, { "last", "last", "last" } };

            for (final String[] row : cases) {
                final Target t = new Target();
                assertEquals(row[1], Reflection.on(t).invoke(row[0]));
                assertEquals(row[2], t.selected);
                assertEquals(1, t.calls);
                final Reflection<Target> r = Reflection.on(t);
                assertSame(r, r.call(row[0]));
                assertEquals(2, t.calls);
            }

            Target t = new Target();
            assertEquals("one:x", Reflection.on(t).invoke("one", "x"));
            assertEquals("one:null", Reflection.on(t).invoke("one", (Object) null));
            assertEquals("sameS:x", Reflection.on(t).invoke("same", "x"));
            assertEquals("sameI:4", Reflection.on(t).invoke("same", 4));
            assertEquals("arity1:3", Reflection.on(t).invoke("arity", 3));
            assertEquals("arity2:7", Reflection.on(t).invoke("arity", 3, 4));
            assertEquals("stat:x", Reflection.on(t).invoke("stat", "x"));
            assertEquals("vararg:2", Reflection.on(t).invoke("vararg", (Object) new String[] { "a", "b" }));
            assertEquals("hiddenS:x", Reflection.on(t).invoke("hidden", "x"));
            assertEquals("hiddenO:5", Reflection.on(t).invoke("hidden", 5));
            assertEquals(Long.valueOf(6L), Reflection.on(t).invoke("widen", 5));
            assertEquals(Long.valueOf(6L), Reflection.on(t).invoke("widen", 5L));
            assertEquals("boxed:5", Reflection.on(t).invoke("boxed", 5));
            assertEquals(Integer.valueOf(6), Reflection.on(t).invoke("prim", 3));
            assertEquals(Integer.valueOf(130), Reflection.on(t).invoke("prim", 'A'));
            assertEquals("Target", Reflection.on(t).invoke("toString"));
            assertEquals(14, t.calls);
            assertSame(Target.class, Reflection.on(t).invoke("getClass"));

            final Reflection<Target> setter = Reflection.on(t);
            assertSame(setter, setter.call("setter", "v"));
            assertEquals("setter:v", t.selected);
            assertEquals(15, t.calls);

            final Child c = new Child();
            assertEquals("childFirst", Reflection.on(c).invoke("first"));
            assertEquals("childOnly:y", Reflection.on(c).invoke("childOnly", "y"));
            assertEquals("one:z", Reflection.on(c).invoke("one", "z"));
            assertEquals(3, c.calls);

            assertEquals(Integer.valueOf(3), Reflection.on(new ArrayList<>(Arrays.asList(1, 2, 3))).invoke("size"));
            assertEquals(Integer.valueOf(5), Reflection.on((Object) "hello").invoke("length"));
            assertEquals("stat:q", Reflection.on(Target.class).invoke("stat", "q"));
        }

        // G058-01: failures on the fast path are unchanged - target exceptions, missing instance, unresolvable calls
        @Test
        void testInvoke_failuresUnchanged() {
            final Target t = new Target();
            final RuntimeException wrapped = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("fail", "x"));
            assertTrue(wrapped.getCause() instanceof java.io.IOException);
            assertEquals("io:x", wrapped.getCause().getMessage());
            assertEquals(1, t.calls);

            final RuntimeException wrappedByCall = assertThrows(RuntimeException.class, () -> Reflection.on(t).call("fail", "y"));
            assertEquals("io:y", wrappedByCall.getCause().getMessage());
            assertEquals(2, t.calls);

            assertThrows(NullPointerException.class, () -> Reflection.on(Target.class).invoke("first"));
            assertThrows(NullPointerException.class, () -> Reflection.on(Target.class).call("one", "x"));

            final RuntimeException missing = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("noSuchMethod"));
            assertEquals("No method found by name: noSuchMethod with parameter types: []", missing.getMessage());

            final RuntimeException wrongArity = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("one"));
            assertEquals("No method found by name: one with parameter types: []", wrongArity.getMessage());

            final RuntimeException nullPrimitive = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("prim", (Object) null));
            assertEquals("No method found by name: prim with parameter types: [null]", nullPrimitive.getMessage());

            final RuntimeException spread = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("vararg", "a", "b"));
            assertTrue(spread.getMessage().startsWith("No method found by name: vararg"), spread.getMessage());

            assertThrows(IllegalArgumentException.class, () -> Reflection.on(t).invoke(null));
            assertThrows(IllegalArgumentException.class, () -> Reflection.on(t).call(null));
            assertEquals(2, t.calls);
        }

        // G058-01: the name index is built from the class's own method table and does not grow with looked-up names
        @Test
        @SuppressWarnings("unchecked")
        void testInvoke_nameIndexBoundedByMethodTable() throws Exception {
            final Target t = new Target();

            for (int i = 0; i < 200; i++) {
                final String name = "noSuchMethod" + i;
                assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke(name));
            }

            java.lang.reflect.Field indexField = null;

            try {
                indexField = ReflectASM.class.getDeclaredField("METHOD_INDEXES_BY_NAME");
            } catch (final NoSuchFieldException e) {
                // pristine build: no name index, nothing to check
            }

            if (indexField != null) {
                indexField.setAccessible(true);
                final Map<String, int[]> index = ((ClassValue<Map<String, int[]>>) indexField.get(null)).get(Target.class);
                final java.lang.reflect.Field accessField = ReflectASM.class.getDeclaredField("METHOD_ACCESS_CACHE");
                accessField.setAccessible(true);
                final String[] table = ((ClassValue<com.esotericsoftware.reflectasm.MethodAccess>) accessField.get(null)).get(Target.class).getMethodNames();
                assertEquals(new HashSet<>(Arrays.asList(table)), index.keySet());

                int entries = 0;

                for (final Map.Entry<String, int[]> entry : index.entrySet()) {
                    int previous = -1;

                    for (final int position : entry.getValue()) {
                        assertTrue(position > previous);
                        assertEquals(entry.getKey(), table[position]);
                        previous = position;
                        entries++;
                    }
                }

                assertEquals(table.length, entries);
            }
        }
    }

    // ---- perf review 2026-10-08 G058 end ----
    // ---- perf review 2026-10-08 G059 begin ----
    @Nested
    class G059 {

        private final String eAcute = String.valueOf((char) 0xE9);
        private final String cjk = new String(new char[] { (char) 0x4E16, (char) 0x754C });
        private final String smiley = new String(Character.toChars(0x1F600));

        private final String[] regexes = { "\\w+", "\\d", "a*", "", "\\b", "(?=a)", "x", "^", "$", "a|b", "\\s+", "(a)(b)?", "\\R", "\\p{L}+", "[^a-z]",
                "(?<=a)", "\\$", "\\\\", "." };

        private String[] sources() {
            return new String[] { "", "a", "aaa", "baaac", "hello world", "  lead and trail  ", "a1b2c3", "price $5 and C:\\dir\\", "abab",
                    "caf" + eAcute + " " + cjk + " end", smiley + "x" + smiley + "a", "line1\r\nline2\nline3\r", "$$\\\\$1", "xxxx" };
        }

        private List<java.util.function.Function<String, String>> functions() {
            final List<java.util.function.Function<String, String>> result = new ArrayList<>();
            result.add(String::toUpperCase);
            result.add(match -> null);
            result.add(match -> "");
            result.add(match -> "$" + match + "\\");
            result.add(match -> "[" + match + "]");
            result.add(match -> "$1");
            result.add(match -> cjk + match);
            result.add(match -> match + match);
            return result;
        }

        private List<com.landawn.abacus.util.function.IntBiFunction<String>> indexFunctions() {
            final List<com.landawn.abacus.util.function.IntBiFunction<String>> result = new ArrayList<>();
            result.add((start, end) -> "[" + start + "-" + end + "]");
            result.add((start, end) -> null);
            result.add((start, end) -> "");
            result.add((start, end) -> "$" + start + "\\");
            result.add((start, end) -> start == end ? smiley : eAcute);
            return result;
        }

        /**
         * The pre-change implementation of replaceAll(String, Pattern, Function): Matcher.replaceAll with the function
         * result escaped by Matcher.quoteReplacement.
         */
        private String jdkReplaceAll(final String source, final java.util.regex.Pattern pattern, final java.util.function.Function<String, String> replacer) {
            return pattern.matcher(source)
                    .replaceAll(matcher -> java.util.regex.Matcher
                            .quoteReplacement(Strings.nullToEmpty(replacer.apply(source.substring(matcher.start(), matcher.end())))));
        }

        /**
         * The pre-change implementation of replaceAll(String, Pattern, IntBiFunction).
         */
        private String jdkReplaceAll(final String source, final java.util.regex.Pattern pattern,
                final com.landawn.abacus.util.function.IntBiFunction<String> replacer) {
            return pattern.matcher(source)
                    .replaceAll(matcher -> java.util.regex.Matcher.quoteReplacement(Strings.nullToEmpty(replacer.apply(matcher.start(), matcher.end()))));
        }

        private void assertSameAsJdk(final String source, final java.util.regex.Pattern pattern, final java.util.function.Function<String, String> function) {
            final List<String> expectedCalls = new ArrayList<>();
            final String expected = jdkReplaceAll(source, pattern, match -> {
                expectedCalls.add(match);
                return function.apply(match);
            });

            final List<String> actualCalls = new ArrayList<>();
            final String actual = RegExUtil.replaceAll(source, pattern, match -> {
                actualCalls.add(match);
                return function.apply(match);
            });

            final String context = "source=" + source + ", pattern=" + pattern.pattern();
            assertEquals(expected, actual, context);
            assertEquals(expectedCalls, actualCalls, context);

            if (expectedCalls.isEmpty()) {
                assertSame(source, actual, context);
            }
        }

        private void assertSameAsJdk(final String source, final java.util.regex.Pattern pattern,
                final com.landawn.abacus.util.function.IntBiFunction<String> function) {
            final List<String> expectedCalls = new ArrayList<>();
            final String expected = jdkReplaceAll(source, pattern, (start, end) -> {
                expectedCalls.add(start + ":" + end);
                return function.apply(start, end);
            });

            final List<String> actualCalls = new ArrayList<>();
            final String actual = RegExUtil.replaceAll(source, pattern, (start, end) -> {
                actualCalls.add(start + ":" + end);
                return function.apply(start, end);
            });

            final String context = "source=" + source + ", pattern=" + pattern.pattern();
            assertEquals(expected, actual, context);
            assertEquals(expectedCalls, actualCalls, context);

            if (expectedCalls.isEmpty()) {
                assertSame(source, actual, context);
            }
        }

        // G059-01: replaceAll(String, Pattern, Function) appends each result literally instead of quoting and re-parsing it
        @Test
        public void testReplaceAllFunction_sameResultAndCallbacksAsQuotedMatcherReplaceAll() {
            for (final String regex : regexes) {
                final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);

                for (final String source : sources()) {
                    for (final java.util.function.Function<String, String> function : functions()) {
                        assertSameAsJdk(source, pattern, function);
                    }
                }
            }

            assertEquals("Hello World", RegExUtil.replaceAll("hello world", java.util.regex.Pattern.compile("\\b\\w"), (String match) -> match.toUpperCase()));
            assertEquals("X", RegExUtil.replaceAll("", java.util.regex.Pattern.compile("a*"), match -> "X"));
            assertEquals("bXc", RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a+"), match -> "X"));
            assertEquals("bc", RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a+"), match -> ""));
            assertEquals("baaac".replaceAll("a*", "X"), RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a*"), match -> "X"));
            assertEquals("a$1\\b", RegExUtil.replaceAll("a.b", java.util.regex.Pattern.compile("\\."), match -> "$1\\"));
            assertEquals("ab", RegExUtil.replaceAll("a1b2", java.util.regex.Pattern.compile("\\d"), match -> null));
            assertEquals("", RegExUtil.replaceAll(null, java.util.regex.Pattern.compile("a*"), match -> "X"));
        }

        // G059-01: replaceAll(String, Pattern, IntBiFunction) appends each result literally instead of quoting and re-parsing it
        @Test
        public void testReplaceAllIntBiFunction_sameResultAndCallbacksAsQuotedMatcherReplaceAll() {
            for (final String regex : regexes) {
                final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);

                for (final String source : sources()) {
                    for (final com.landawn.abacus.util.function.IntBiFunction<String> function : indexFunctions()) {
                        assertSameAsJdk(source, pattern, function);
                    }
                }
            }

            assertEquals("abc[3-6]def",
                    RegExUtil.replaceAll("abc123def", java.util.regex.Pattern.compile("\\d+"), (start, end) -> "[" + start + "-" + end + "]"));
            assertEquals("<0>", RegExUtil.replaceAll("", java.util.regex.Pattern.compile("a*"), (start, end) -> "<" + start + ">"));
            assertEquals("a$c", RegExUtil.replaceAll("abc", java.util.regex.Pattern.compile("b"), (start, end) -> "$"));
            assertEquals("baaac".replaceAll("a*", "X"), RegExUtil.replaceAll("baaac", java.util.regex.Pattern.compile("a*"), (start, end) -> "X"));
            assertEquals("", RegExUtil.replaceAll(null, java.util.regex.Pattern.compile("a*"), (start, end) -> "X"));
        }

        // G059-01: random sources/patterns/results (fixed seed) agree with the quoted Matcher.replaceAll, incl. the callback sequence
        @Test
        public void testReplaceAll_randomInputsSameAsQuotedMatcherReplaceAll() {
            final Random random = new Random(20261008L);
            final String[] pieces = { "a", "b", "1", " ", "$", "\\", "\n", eAcute, cjk, smiley, "ab", "  " };
            final String[] results = { "", "X", "$", "\\", "$0", "\\1", eAcute, smiley, null };

            for (int i = 0; i < 3000; i++) {
                final StringBuilder sb = new StringBuilder();
                final int length = random.nextInt(12);

                for (int j = 0; j < length; j++) {
                    sb.append(pieces[random.nextInt(pieces.length)]);
                }

                final String source = sb.toString();
                final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regexes[random.nextInt(regexes.length)]);
                final String fixed = results[random.nextInt(results.length)];
                final boolean echo = random.nextBoolean();

                assertSameAsJdk(source, pattern, match -> echo ? match + fixed : fixed);
                assertSameAsJdk(source, pattern, (start, end) -> echo ? start + "" + fixed : fixed);
            }
        }

        // G059-01: a throwing replacer propagates its own exception after the same number of calls, and argument checks are unchanged
        @Test
        public void testReplaceAll_throwingReplacerAndArgumentChecks() {
            final java.util.regex.Pattern digit = java.util.regex.Pattern.compile("\\d");
            final AtomicInteger calls = new AtomicInteger();

            final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> RegExUtil.replaceAll("a1b2c3", digit, match -> {
                if (calls.incrementAndGet() == 2) {
                    throw new IllegalStateException("boom " + match);
                }

                return "x";
            }));
            assertEquals("boom 2", ex.getMessage());
            assertEquals(2, calls.get());

            final List<String> seen = new ArrayList<>();
            final IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1b2c3", digit, (start, end) -> {
                seen.add(start + ":" + end);

                if (seen.size() == 3) {
                    throw new IllegalArgumentException("stop " + start);
                }

                return "$";
            }));
            assertEquals("stop 5", ex2.getMessage());
            assertEquals(Arrays.asList("1:2", "3:4", "5:6"), seen);

            final AtomicInteger notCalled = new AtomicInteger();
            assertEquals("", RegExUtil.replaceAll(null, digit, match -> String.valueOf(notCalled.incrementAndGet())));
            assertEquals("", RegExUtil.replaceAll(null, digit, (start, end) -> String.valueOf(notCalled.incrementAndGet())));
            assertEquals(0, notCalled.get());

            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", (java.util.regex.Pattern) null, match -> "x"));
            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", (java.util.regex.Pattern) null, (start, end) -> "x"));
            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll("a1", digit, (java.util.function.Function<String, String>) null));
            assertThrows(IllegalArgumentException.class,
                    () -> RegExUtil.replaceAll("a1", digit, (com.landawn.abacus.util.function.IntBiFunction<String>) null));
            assertThrows(IllegalArgumentException.class, () -> RegExUtil.replaceAll(null, (java.util.regex.Pattern) null, match -> "x"));
            assertThrows(java.util.regex.PatternSyntaxException.class, () -> RegExUtil.replaceAll("a1", "(", match -> "x"));
            assertThrows(java.util.regex.PatternSyntaxException.class, () -> RegExUtil.replaceAll(null, "(", (start, end) -> "x"));
        }

        // G059-01: no match returns the source instance itself (as Matcher.replaceAll did); a match yields a new string
        @Test
        public void testReplaceAll_noMatchReturnsSourceInstance() {
            final String source = new String("no digits here");
            assertSame(source, RegExUtil.replaceAll(source, java.util.regex.Pattern.compile("\\d"), match -> "x"));
            assertSame(source, RegExUtil.replaceAll(source, java.util.regex.Pattern.compile("\\d"), (start, end) -> "x"));
            assertSame(source, RegExUtil.replaceAll(source, "\\d", match -> "x"));
            assertSame(source, RegExUtil.replaceAll(source, "\\d", (start, end) -> "x"));

            final String result = RegExUtil.replaceAll(source, java.util.regex.Pattern.compile("o"), match -> "o");
            assertEquals(source, result);
            assertNotSame(source, result);
        }
    }

    // ---- perf review 2026-10-08 G059 end ----
    // ---- perf review 2026-10-08 G060 begin ----
    @Nested
    class G060 {

        private static final String INVALID_NAME_PREFIX = "Invalid XML element name for ";

        private Dataset oneCell() {
            return Dataset.rows(Arrays.asList("c"), new Object[][] { { Arrays.asList("a", "b") } });
        }

        private String read(final java.io.File file) throws java.io.IOException {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        }

        // G060-01: plain ASCII element names are accepted without building a DOM document; output unchanged.
        @Test
        void testToXml_plainElementNamesWrittenAsBefore(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path dir) throws Exception {
            final Dataset ds = Dataset.rows(Arrays.asList("id", "a.b-c_1", "_x", "Z9"), new Object[][] { { 1, "v", 2.5, true } });
            final String expected = "<dataset><row_1><id>1</id><a.b-c_1>v</a.b-c_1><_x>2.5</_x><Z9>true</Z9></row_1></dataset>";

            assertEquals(expected, ds.toXml("row_1"));
            assertEquals(expected, ds.toXml(0, 1, Arrays.asList("id", "a.b-c_1", "_x", "Z9"), "row_1"));

            final java.io.StringWriter writer = new java.io.StringWriter();
            ds.toXml("row_1", writer);
            assertEquals(expected, writer.toString());

            final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            ds.toXml("row_1", bytes);
            assertEquals(expected, bytes.toString(java.nio.charset.StandardCharsets.UTF_8));

            final java.io.File file = dir.resolve("plain.xml").toAbsolutePath().toFile();
            ds.toXml("row_1", file);
            assertEquals(expected, read(file));

            assertEquals("<dataset><row><id>1</id><Z9>true</Z9></row></dataset>", ds.toXml(0, 1, Arrays.asList("id", "Z9")));
        }

        // G060-01: names outside the plain ASCII subset (incl. the reserved "xml" prefix) are still decided by the DOM.
        @Test
        void testToXml_nonPlainElementNamesStillValidatedByDom() {
            for (final String name : new String[] { "xml", "XmlData", "xmlnsx", "\u540d\u524d", "a\u00b7b" }) {
                assertEquals("<dataset><row><" + name + ">1</" + name + "><v>2</v></row></dataset>",
                        Dataset.rows(Arrays.asList(name, "v"), new Object[][] { { 1, 2 } }).toXml(), name);
                assertEquals("<dataset><" + name + "><c><list>[&quot;a&quot;, &quot;b&quot;]</list></c></" + name + "></dataset>", oneCell().toXml(name), name);
            }

            final String[] rejected = { "xmlns", "a:b", ":a", "a:", "1a", "-a", ".a", "a b", "\uD800", "a\u0000" };
            final short[] codes = { 14, 14, 14, 14, 5, 5, 5, 5, 5, 5 };

            for (int i = 0; i < rejected.length; i++) {
                final String name = rejected[i];
                final Dataset ds = Dataset.rows(Arrays.asList("ok", name), new Object[][] { { 1, 2 } });

                IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ds::toXml);
                assertEquals(INVALID_NAME_PREFIX + "columnName: " + name, e.getMessage());
                assertTrue(e.getCause() instanceof org.w3c.dom.DOMException, name);
                assertEquals(codes[i], ((org.w3c.dom.DOMException) e.getCause()).code, name);

                e = assertThrows(IllegalArgumentException.class, () -> oneCell().toXml(name));
                assertEquals(INVALID_NAME_PREFIX + "rowElementName: " + name, e.getMessage());
                assertEquals(codes[i], ((org.w3c.dom.DOMException) e.getCause()).code, name);

                // An unselected column is not validated.
                assertEquals("<dataset><row><ok>1</ok></row></dataset>", ds.toXml(0, 1, Arrays.asList("ok")));
            }
        }

        // G060-01: validation still happens before anything is written, in the same order (row element name first).
        @Test
        void testToXml_invalidNameFailsBeforeAnyOutput(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path dir) throws Exception {
            final Dataset ds = Dataset.rows(Arrays.asList("ok", "bad name", "1st"), new Object[][] { { 1, 2, 3 } });
            final java.io.StringWriter writer = new java.io.StringWriter();

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(writer));
            assertEquals(INVALID_NAME_PREFIX + "columnName: bad name", e.getMessage());
            assertEquals("", writer.toString());

            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml("bad row", writer));
            assertEquals(INVALID_NAME_PREFIX + "rowElementName: bad row", e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(0, 1, Collections.emptyList(), "bad row", writer));
            assertEquals(INVALID_NAME_PREFIX + "rowElementName: bad row", e.getMessage());
            assertEquals("", writer.toString());

            ds.toXml(0, 1, Collections.emptyList(), "row", writer);
            assertEquals("<dataset></dataset>", writer.toString());

            final java.io.File file = dir.resolve("out.xml").toAbsolutePath().toFile();
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(file));
            assertEquals(INVALID_NAME_PREFIX + "columnName: bad name", e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(0, 1, Arrays.asList("ok", "1st"), "row", file));
            assertEquals(INVALID_NAME_PREFIX + "columnName: 1st", e.getMessage());
            e = assertThrows(IllegalArgumentException.class, () -> ds.toXml("x y", file));
            assertEquals(INVALID_NAME_PREFIX + "rowElementName: x y", e.getMessage());
            assertFalse(file.exists());

            ds.toXml(0, 1, Arrays.asList("ok"), "row", file);
            assertEquals("<dataset><row><ok>1</ok></row></dataset>", read(file));
        }

        public enum Color {
            RED, GREEN
        }

        /** A bean whose serialization fails, so the export falls back to its toString(). */
        // Accessor-based on purpose: this fixture observes getter/setter calls, and properties are read and written
        // through their fields unless @AccessFieldByMethod says otherwise (in both the reflective and the ASM mode).
        @com.landawn.abacus.annotation.AccessFieldByMethod
        public static class ThrowingBean {
            private int x;

            public int getX() {
                throw new IllegalStateException("boom");
            }

            public void setX(final int x) {
                this.x = x;
            }

            @Override
            public String toString() {
                return "TB";
            }
        }

        private Dataset mixedCells() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("k", 1);
            map.put("q\"", "v<&>");

            return Dataset.rows(Arrays.asList("d", "l", "e", "m"), new Object[][] {
                    { new java.util.Date(1700000000000L), Arrays.asList("a", "b"), Color.RED, map },
                    { null, Arrays.asList(1, 2, 3), Color.GREEN, java.time.LocalDate.of(2026, 10, 8) },
                    { new ThrowingBean(), java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), new ThrowingBean(), Arrays.asList("x") } });
        }

        // G060-02: one scratch writer per export call; non-simple cells (incl. parser fallbacks) serialize exactly as before.
        @Test
        void testToJson_nonSimpleCellsWithFallbacks() {
            final String expected = "[{\"d\":\"2023-11-14T22:13:20.000Z\", \"l\":[\"a\", \"b\"], \"e\":\"RED\", \"m\":{\"k\": 1, \"q\\\"\": \"v<&>\"}}, "
                    + "{\"d\":null, \"l\":[1, 2, 3], \"e\":\"GREEN\", \"m\":\"2026-10-08\"}, "
                    + "{\"d\":\"TB\", \"l\":\"123e4567-e89b-12d3-a456-426614174000\", \"e\":\"TB\", \"m\":[\"x\"]}]";
            final Dataset ds = mixedCells();

            assertEquals(expected, ds.toJson());

            final java.io.StringWriter writer = new java.io.StringWriter();
            ds.toJson(writer);
            assertEquals(expected, writer.toString());
            assertEquals("[{\"c\":[\"a\", \"b\"]}]", oneCell().toJson());

            // Column names are escaped through the same scratch writer as the cells.
            assertEquals("[{\"q\\\"uote\":1, \"back\\\\slash\":2, \"tab\\tx\":3, \"\u540d\":4, \"plain\":5}]",
                    Dataset.rows(Arrays.asList("q\"uote", "back\\slash", "tab\tx", "\u540d", "plain"), new Object[][] { { 1, 2, 3, 4, 5 } }).toJson());
        }

        // G060-02: XML counterpart, including a map whose keys are not XML names (parser failure -> string fallback mid-row).
        @Test
        void testToXml_nonSimpleCellsWithFallbacks() {
            final String expected = "<dataset><row><d>2023-11-14T22:13:20.000Z</d><l><list>[&quot;a&quot;, &quot;b&quot;]</list></l><e>RED</e>"
                    + "<m>{k=1, q&quot;=v&lt;&amp;&gt;}</m></row><row><d>null</d><l><list>[1, 2, 3]</list></l><e>GREEN</e><m>2026-10-08</m></row>"
                    + "<row><d>TB</d><l>123e4567-e89b-12d3-a456-426614174000</l><e>TB</e><m><list>[&quot;x&quot;]</list></m></row></dataset>";
            final Dataset ds = mixedCells();

            assertEquals(expected, ds.toXml());

            final java.io.StringWriter writer = new java.io.StringWriter();
            ds.toXml(writer);
            assertEquals(expected, writer.toString());
            assertEquals("<dataset><row><c><list>[&quot;a&quot;, &quot;b&quot;]</list></c></row></dataset>", oneCell().toXml());
        }

        // G060-02: a long cell followed by short ones leaves nothing behind; every multi-row export equals the
        // composition of single-cell exports (each of which starts with a new scratch writer).
        @Test
        void testToJsonToXml_reusedScratchMatchesSingleCellExports() {
            final List<Object> longList = new ArrayList<>();

            for (int i = 0; i < 300; i++) {
                longList.add("element-" + i);
            }

            // Serializes to more characters than a pooled buffer holds, so the scratch buffer has to grow.
            final List<Object> hugeList = new ArrayList<>();

            for (int i = 0; i < 3000; i++) {
                hugeList.add("huge-element-" + i);
            }

            final Object[] cells = { hugeList, longList, Arrays.asList("s"), new java.util.Date(1700000000000L), null, "plain", 7, Color.GREEN,
                    java.time.LocalDate.of(2026, 1, 2), Collections.singletonMap("k", Arrays.asList(1, 2)), new ThrowingBean(), Arrays.asList(),
                    java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000") };
            final int columnCount = 3;
            final int rowCount = 40;
            final List<String> columnNames = Arrays.asList("c0", "c1", "c2");
            final Object[][] rows = new Object[rowCount][columnCount];
            final Random random = new Random(60);

            for (final Object[] row : rows) {
                for (int c = 0; c < columnCount; c++) {
                    row[c] = cells[random.nextInt(cells.length)];
                }
            }

            final StringBuilder json = new StringBuilder("[");
            final StringBuilder xml = new StringBuilder("<dataset>");

            for (int r = 0; r < rowCount; r++) {
                json.append(r > 0 ? ", {" : "{");
                xml.append("<row>");

                for (int c = 0; c < columnCount; c++) {
                    final Dataset single = Dataset.rows(Arrays.asList("x"), new Object[][] { { rows[r][c] } });
                    final String singleJson = single.toJson();
                    final String singleXml = single.toXml();
                    final String jsonValue = singleJson.substring("[{\"x\":".length(), singleJson.length() - "}]".length());
                    final String xmlValue = singleXml.substring("<dataset><row><x>".length(), singleXml.length() - "</x></row></dataset>".length());

                    json.append(c > 0 ? ", " : "").append('"').append(columnNames.get(c)).append("\":").append(jsonValue);
                    xml.append('<').append(columnNames.get(c)).append('>').append(xmlValue).append("</").append(columnNames.get(c)).append('>');
                }

                json.append('}');
                xml.append("</row>");
            }

            json.append(']');
            xml.append("</dataset>");

            final Dataset ds = Dataset.rows(columnNames, rows);

            assertEquals(json.toString(), ds.toJson());
            assertEquals(xml.toString(), ds.toXml());
            assertTrue(ds.toJson().length() > 300, "sanity");

            final Dataset shortAfterLong = Dataset.rows(Arrays.asList("x"),
                    new Object[][] { { longList }, { hugeList }, { Arrays.asList("s") }, { Arrays.asList() } });
            assertTrue(shortAfterLong.toJson().endsWith("}, {\"x\":[\"s\"]}, {\"x\":[]}]"), shortAfterLong.toJson());
            assertTrue(shortAfterLong.toXml().endsWith("<row><x><list>[&quot;s&quot;]</list></x></row><row><x><list>[]</list></x></row></dataset>"),
                    shortAfterLong.toXml());
        }

        // G060-02: a cell that fails validation after earlier non-simple cells fails at the same cell with the same
        // partial output (the failing cell's text is never written).
        @Test
        void testToJsonToXml_validationFailureAfterReusedScratch() {
            final Dataset nan = Dataset.rows(Arrays.asList("a", "b"),
                    new Object[][] { { Arrays.asList(1.0), "x" }, { Arrays.asList(2.0), Arrays.asList(Double.NaN) } });
            final java.io.StringWriter jsonWriter = new java.io.StringWriter();
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> nan.toJson(jsonWriter));
            assertEquals("Cannot export JSON: invalid value at row 1, column 'b'", e.getMessage());
            assertEquals("[{\"a\":[1.0], \"b\":\"x\"}, {\"a\":[2.0], \"b\":", jsonWriter.toString());

            final Dataset bad = Dataset.rows(Arrays.asList("a", "b"),
                    new Object[][] { { Arrays.asList("ok"), "x" }, { Arrays.asList("y"), Collections.singletonMap("nested", "\u0000") } });
            final java.io.StringWriter xmlWriter = new java.io.StringWriter();
            e = assertThrows(IllegalArgumentException.class, () -> bad.toXml(xmlWriter));
            assertEquals("Cannot export XML: invalid value at row 1, column 'b'", e.getMessage());
            assertEquals("<dataset><row><a><list>[&quot;ok&quot;]</list></a><b>x</b></row><row><a><list>[&quot;y&quot;]</list></a><b>", xmlWriter.toString());

            // The export after a failed one is unaffected.
            assertEquals("[{\"c\":[\"a\", \"b\"]}]", oneCell().toJson());
            assertEquals("<dataset><row><c><list>[&quot;a&quot;, &quot;b&quot;]</list></c></row></dataset>", oneCell().toXml());
        }

        // G060-03: the serialized-XML scan tests its markers only at '<', ']', '-' and '&'; references, escaped
        // markers and supplementary characters are accepted or rejected exactly as before.
        @Test
        void testToXml_serializedTextScan() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("tab", "a\tb");
            map.put("nl", "c\nd");
            map.put("del", "e\u007ff");
            map.put("marks", "<![CDATA[x]]> <!-- y --> &#0; &amp; ]]> -->");
            map.put("dash", "2026-10-08");

            final Dataset ok = Dataset.rows(Arrays.asList("id", "m", "l"),
                    new Object[][] { { 1, map, Arrays.asList("p]]>q", "r-->s", "&#1;") }, { 2, Collections.singletonMap("k", "\u00e9\uD83D\uDE00"), null } });

            assertEquals("<dataset><row><id>1</id><m><map><tab>a&#x9;b</tab><nl>c&#xa;d</nl><del>e&#x7f;f</del>"
                    + "<marks>&lt;![CDATA[x]]&gt; &lt;!-- y --&gt; &amp;#0; &amp;amp; ]]&gt; --&gt;</marks><dash>2026-10-08</dash></map></m>"
                    + "<l><list>[&quot;p]]&gt;q&quot;, &quot;r--&gt;s&quot;, &quot;&amp;#1;&quot;]</list></l></row>"
                    + "<row><id>2</id><m><map><k>\u00e9\uD83D\uDE00</k></map></m><l>null</l></row></dataset>", ok.toXml());

            for (final String bad : new String[] { "\u0001", "x\u001fy", "\u0000" }) {
                final Dataset ds = Dataset.rows(Arrays.asList("a", "b"),
                        new Object[][] { { Collections.singletonMap("k", "fine-text"), 1 }, { 2, Collections.singletonMap("k", bad) } });
                final java.io.StringWriter writer = new java.io.StringWriter();
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ds.toXml(writer));

                assertEquals("Cannot export XML: invalid value at row 1, column 'b'", e.getMessage());
                assertEquals("<dataset><row><a><map><k>fine-text</k></map></a><b>1</b></row><row><a>2</a><b>", writer.toString());
            }
        }

        private Dataset abc() {
            return Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" }, { 2, "y" }, { 3, "z" } });
        }

        // G060-04: append/prepend snapshot each source column once; aliasing sources (self, own slices) still see the
        // pre-write rows, column order is mapped by name, and the appended rows are independent copies.
        @Test
        void testAppendPrepend_snapshotOfAliasingSources() {
            final Dataset self = abc();
            self.append(self);
            assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), self.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "x", "y", "z"), self.getColumn("b"));

            final Dataset prepended = abc();
            prepended.prepend(prepended.slice(1, 3));
            assertEquals(Arrays.asList(2, 3, 1, 2, 3), prepended.getColumn("a"));
            assertEquals(Arrays.asList("y", "z", "x", "y", "z"), prepended.getColumn("b"));

            final Dataset reordered = abc();
            final Dataset other = Dataset.rows(Arrays.asList("b", "a"), new Object[][] { { "p", 9 }, { "q", 8 } });
            reordered.append(other);
            assertEquals(Arrays.asList(1, 2, 3, 9, 8), reordered.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "p", "q"), reordered.getColumn("b"));
            other.set(0, 0, "changed");
            assertEquals("p", reordered.get(3, 1));

            final Dataset empty = abc();
            empty.append(Dataset.rows(Arrays.asList("a", "b"), new Object[0][]));
            assertEquals(3, empty.size());

            final Dataset stale = abc();
            final Dataset slice = stale.slice(0, 2);
            stale.swapRows(0, 1);
            assertThrows(java.util.ConcurrentModificationException.class, () -> stale.append(slice));
            assertEquals(Arrays.asList(2, 1, 3), stale.getColumn("a"));
            assertEquals(Arrays.asList("y", "x", "z"), stale.getColumn("b"));
        }

        // G060-04: merge snapshots the selected source range once; new columns are added and missing ones null-filled.
        @Test
        void testMerge_snapshotOfSelectedRange() {
            final Dataset target = Dataset.rows(Arrays.asList("a", "b"), new Object[][] { { 1, "x" } });
            target.merge(Dataset.rows(Arrays.asList("b", "c"), new Object[][] { { "q", true }, { "r", false } }));
            assertEquals(Arrays.asList("a", "b", "c"), target.columnNames());
            assertEquals(Arrays.asList(1, null, null), target.getColumn("a"));
            assertEquals(Arrays.asList("x", "q", "r"), target.getColumn("b"));
            assertEquals(Arrays.asList(null, true, false), target.getColumn("c"));

            final Dataset self = abc();
            self.merge(self, 1, 3, Arrays.asList("b"));
            assertEquals(Arrays.asList(1, 2, 3, null, null), self.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "y", "z"), self.getColumn("b"));

            final Dataset viaSlice = abc();
            viaSlice.merge(viaSlice.slice(0, 2));
            assertEquals(Arrays.asList(1, 2, 3, 1, 2), viaSlice.getColumn("a"));
            assertEquals(Arrays.asList("x", "y", "z", "x", "y"), viaSlice.getColumn("b"));
        }

        // G060-04: moveRows copies the moved block once; results for forward, backward, overlapping and no-op moves.
        @Test
        void testMoveRows_blockCopiedOnce() {
            final Object[][] rows = new Object[6][];

            for (int i = 0; i < rows.length; i++) {
                rows[i] = new Object[] { i, "r" + i };
            }

            final Dataset ds = Dataset.rows(Arrays.asList("a", "b"), rows);

            ds.moveRows(0, 2, 4);
            assertEquals(Arrays.asList(2, 3, 4, 5, 0, 1), ds.getColumn("a"));
            assertEquals(Arrays.asList("r2", "r3", "r4", "r5", "r0", "r1"), ds.getColumn("b"));

            ds.moveRows(4, 6, 0);
            assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), ds.getColumn("a"));

            ds.moveRows(1, 4, 2);
            assertEquals(Arrays.asList(0, 4, 1, 2, 3, 5), ds.getColumn("a"));
            assertEquals(Arrays.asList("r0", "r4", "r1", "r2", "r3", "r5"), ds.getColumn("b"));

            final Dataset slice = ds.slice(0, 2);
            ds.moveRows(2, 2, 0);
            ds.moveRows(0, 6, 0);
            assertEquals(Arrays.asList(0, 4), slice.getColumn("a"));
            assertEquals(Arrays.asList(0, 4, 1, 2, 3, 5), ds.getColumn("a"));

            ds.moveRows(5, 6, 0);
            assertEquals(Arrays.asList(5, 0, 4, 1, 2, 3), ds.getColumn("a"));
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.getColumn("a").get(0));
        }
    }

    // ---- perf review 2026-10-08 G060 end ----
    // ---- perf review 2026-10-08 G061 begin ----
    @Nested
    class G061 {
        private final Random random = new Random(20261008L);

        private Object randomKey1() {
            switch (random.nextInt(6)) {
                case 0:
                    return null;
                case 1:
                    return "a";
                case 2:
                    return "b";
                case 3:
                    // A fresh array per cell: rows must still match by content (deep equality).
                    return new int[] { 1 };
                case 4:
                    return new int[] { 2 };
                default:
                    return "c";
            }
        }

        private Object randomKey2() {
            final int value = random.nextInt(4);
            return value == 0 ? null : (Object) Integer.valueOf(value);
        }

        private Dataset joinSide(final String idName, final String valueName, final int rows, final int idBase, final boolean uniqueKeys, final int keyOffset) {
            final Object[][] data = new Object[rows][];

            for (int i = 0; i < rows; i++) {
                final Object key1 = uniqueKeys ? "key" + (i + keyOffset) : randomKey1();
                final Object key2 = uniqueKeys ? (Object) Integer.valueOf((i + keyOffset) % 7) : randomKey2();
                data[i] = new Object[] { idBase + i, key1, key2, valueName + i };
            }

            return Dataset.rows(Arrays.asList(idName, "k1", "k2", valueName), data);
        }

        private Map<String, String> on2() {
            final Map<String, String> on = new LinkedHashMap<>();
            on.put("k1", "k1");
            on.put("k2", "k2");
            return on;
        }

        private boolean sameKey(final Dataset left, final int leftRow, final Dataset right, final int rightRow) {
            // Object locals: an inferred get() type would select N.deepEquals(Object[], Object[]).
            final Object left1 = left.get(leftRow, 1);
            final Object right1 = right.get(rightRow, 1);
            final Object left2 = left.get(leftRow, 2);
            final Object right2 = right.get(rightRow, 2);
            return CommonUtil.deepEquals(left1, right1) && CommonUtil.deepEquals(left2, right2);
        }

        private List<Object> rowValues(final Dataset ds, final int row) {
            final List<Object> values = new ArrayList<>();

            for (int c = 0; c < ds.columnCount(); c++) {
                values.add(ds.get(row, c));
            }

            return values;
        }

        private List<List<Object>> newColumns(final int count) {
            final List<List<Object>> columns = new ArrayList<>();

            for (int c = 0; c < count; c++) {
                columns.add(new ArrayList<>());
            }

            return columns;
        }

        private void addRow(final List<List<Object>> columns, final List<Object> leftValues, final List<Object> rightValues) {
            for (int c = 0; c < leftValues.size(); c++) {
                columns.get(c).add(leftValues.get(c));
            }

            for (int c = 0; c < rightValues.size(); c++) {
                columns.get(leftValues.size() + c).add(rightValues.get(c));
            }
        }

        private boolean leftHasKey(final Dataset left, final Dataset right, final int rightRow) {
            for (int i = 0; i < left.size(); i++) {
                if (sameKey(left, i, right, rightRow)) {
                    return true;
                }
            }

            return false;
        }

        /** mode 0: fullJoin(right, on); 1: fullJoin(right, on, "rr", List.class); 2: ... with ArrayList::new per left row. */
        private List<List<Object>> expectedFullJoin(final Dataset left, final Dataset right, final int mode) {
            final List<Object> nullLeft = Arrays.asList(null, null, null, null);
            final List<Object> nullRight = mode == 0 ? Arrays.asList(null, null, null, null) : Collections.singletonList(null);
            final List<List<Object>> columns = newColumns(mode == 0 ? 8 : 5);

            for (int i = 0; i < left.size(); i++) {
                final List<Object> matches = new ArrayList<>();

                for (int j = 0; j < right.size(); j++) {
                    if (sameKey(left, i, right, j)) {
                        matches.add(rowValues(right, j));
                    }
                }

                if (matches.isEmpty()) {
                    addRow(columns, rowValues(left, i), nullRight);
                } else if (mode == 2) {
                    addRow(columns, rowValues(left, i), Collections.singletonList(matches));
                } else {
                    for (final Object match : matches) {
                        addRow(columns, rowValues(left, i), mode == 0 ? (List<Object>) match : Collections.singletonList(match));
                    }
                }
            }

            final boolean[] done = new boolean[right.size()];

            for (int j = 0; j < right.size(); j++) {
                if (done[j] || leftHasKey(left, right, j)) {
                    continue;
                }

                if (mode == 2) {
                    // One row per unmatched key, in the order the key first occurs on the right.
                    final List<Object> group = new ArrayList<>();

                    for (int k = j; k < right.size(); k++) {
                        if (sameKey(right, j, right, k)) {
                            group.add(rowValues(right, k));
                            done[k] = true;
                        }
                    }

                    addRow(columns, nullLeft, Collections.singletonList(group));
                } else {
                    addRow(columns, nullLeft, mode == 0 ? rowValues(right, j) : Collections.singletonList(rowValues(right, j)));
                }
            }

            return columns;
        }

        private void assertFullJoin(final Dataset left, final Dataset right, final List<String> expectedNames) {
            final Dataset plain = left.fullJoin(right, on2());
            assertEquals(expectedNames, plain.columnNames());
            assertColumns(expectedFullJoin(left, right, 0), plain);

            final Dataset typed = left.fullJoin(right, on2(), "rr", List.class);
            assertEquals(Arrays.asList("id", "k1", "k2", "v", "rr"), typed.columnNames());
            assertColumns(expectedFullJoin(left, right, 1), typed);

            final Dataset collected = left.fullJoin(right, on2(), "rr", List.class, ArrayList::new);
            assertEquals(Arrays.asList("id", "k1", "k2", "v", "rr"), collected.columnNames());
            assertColumns(expectedFullJoin(left, right, 2), collected);
        }

        private void assertColumns(final List<List<Object>> expected, final Dataset actual) {
            assertEquals(expected.size(), actual.columnCount());

            for (int c = 0; c < expected.size(); c++) {
                assertEquals(expected.get(c), new ArrayList<>(actual.getColumn(c)), "column " + actual.columnNames().get(c));
            }
        }

        // G061-01: multi-key fullJoin (plain, row-typed column, one collection per left row) against a naive nested-loop
        // oracle: duplicate, null and content-equal array keys, unmatched rows on both sides, empty sides.
        @Test
        void testFullJoin_multiKeyMatchesNaiveOracle() {
            final List<String> names = Arrays.asList("id", "k1", "k2", "v", "rid", "k1_2", "k2_2", "r");
            final int[] sizes = { 0, 1, 2, 5, 17, 40 };

            for (final int leftSize : sizes) {
                for (final int rightSize : sizes) {
                    final Dataset left = joinSide("id", "v", leftSize, 0, false, 0);
                    final Dataset right = joinSide("rid", "r", rightSize, 1000, false, 0);
                    assertFullJoin(left, right, names);
                }
            }

            // Hundreds of distinct keys (the left key set grows past its initial table), 10% unmatched on each side.
            final Dataset left = joinSide("id", "v", 300, 0, true, 0);
            final Dataset right = joinSide("rid", "r", 300, 1000, true, 30);
            assertFullJoin(left, right, names);
            assertEquals(330, left.fullJoin(right, on2()).size());
        }

        // G061-01: self join and an all-duplicate left side (every left row after the first finds its key already recorded).
        @Test
        void testFullJoin_multiKeySelfJoinAndDuplicateLeftKeys() {
            final Dataset left = joinSide("id", "v", 30, 0, false, 0);
            final Dataset self = left.fullJoin(left, on2());
            assertEquals(Arrays.asList("id", "k1", "k2", "v", "id_2", "k1_2", "k2_2", "v_2"), self.columnNames());
            assertColumns(expectedFullJoin(left, left, 0), self);

            final Object[][] same = new Object[6][];

            for (int i = 0; i < same.length; i++) {
                same[i] = new Object[] { i, new int[] { 7 }, null, "v" + i };
            }

            final Dataset duplicates = Dataset.rows(Arrays.asList("id", "k1", "k2", "v"), same);
            final Dataset right = Dataset.rows(Arrays.asList("rid", "k1", "k2", "r"),
                    new Object[][] { { 100, new int[] { 7 }, null, "r0" }, { 101, "x", 1, "r1" }, { 102, new int[] { 7 }, null, "r2" } });
            assertFullJoin(duplicates, right, Arrays.asList("id", "k1", "k2", "v", "rid", "k1_2", "k2_2", "r"));
            assertEquals(Arrays.asList(0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, null), new ArrayList<>(duplicates.fullJoin(right, on2()).getColumn("id")));
            assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, null),
                    new ArrayList<>(duplicates.fullJoin(right, on2(), "rr", List.class, ArrayList::new).getColumn("id")));
        }

        @SuppressWarnings("unchecked")
        private List<String> keyColumnNames(final Dataset ds, final Dataset other) throws Exception {
            final java.lang.reflect.Method method = RowDataset.class.getDeclaredMethod("getKeyColumnNames", Dataset.class);
            method.setAccessible(true);

            try {
                return (List<String>) method.invoke(ds, other);
            } catch (final java.lang.reflect.InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw e;
            }
        }

        private Dataset oneRow(final List<String> names) {
            final Object[] row = new Object[names.size()];

            for (int i = 0; i < row.length; i++) {
                row[i] = names.get(i).hashCode() % 5;
            }

            return Dataset.rows(names, new Object[][] { row });
        }

        private List<String> retainedNames(final Dataset ds, final Dataset other) {
            // The pre-change algorithm, as an oracle.
            final List<String> expected = new ArrayList<>(ds.columnNames());
            expected.retainAll(new ArrayList<>(other.columnNames()));
            return expected;
        }

        // G061-02: the key columns of the key-less set operations are this Dataset's names that other also has, in this
        // Dataset's order, in a fresh mutable ArrayList; no overlap keeps the same message.
        @Test
        void testGetKeyColumnNames_orderOverlapErrorsAndColumnIndexSync() throws Exception {
            final Dataset abcd = oneRow(Arrays.asList("a", "b", "c", "d"));
            final Dataset dxby = oneRow(Arrays.asList("d", "x", "b", "y"));
            final List<String> common = keyColumnNames(abcd, dxby);
            assertEquals(Arrays.asList("b", "d"), common);
            assertEquals(ArrayList.class, common.getClass());
            common.add("z");
            assertEquals(Arrays.asList("b", "d"), keyColumnNames(abcd, dxby));
            assertEquals(Arrays.asList("d", "b"), keyColumnNames(dxby, abcd));
            assertEquals(Arrays.asList("a", "b", "c", "d"), keyColumnNames(abcd, abcd));

            final Dataset noColumns = new RowDataset(new ArrayList<>(), new ArrayList<>());
            assertEquals(Collections.emptyList(), keyColumnNames(noColumns, new RowDataset(new ArrayList<>(), new ArrayList<>())));
            assertEquals("These two Datasets do not have any common column names: [a, b, c, d], [x]",
                    assertThrows(IllegalArgumentException.class, () -> keyColumnNames(abcd, oneRow(Arrays.asList("x")))).getMessage());
            assertEquals("These two Datasets do not have any common column names: [], [a, b, c, d]",
                    assertThrows(IllegalArgumentException.class, () -> keyColumnNames(noColumns, abcd)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [a, b, c, d], []",
                    assertThrows(IllegalArgumentException.class, () -> keyColumnNames(abcd, noColumns)).getMessage());
            assertEquals("'other' cannot be null", assertThrows(IllegalArgumentException.class, () -> keyColumnNames(abcd, null)).getMessage());
            assertEquals("These two Datasets do not have any common column names: [a, b, c, d], [x]",
                    assertThrows(IllegalArgumentException.class, () -> abcd.semiJoin(oneRow(Arrays.asList("x")))).getMessage());

            // other's column index must follow every kind of column change (patched in place or rebuilt).
            final Dataset other = oneRow(Arrays.asList("d", "x", "b", "y"));
            assertEquals(retainedNames(abcd, other), keyColumnNames(abcd, other));
            other.renameColumn("b", "z");
            assertEquals(Arrays.asList("d"), keyColumnNames(abcd, other));
            other.addColumn("a", Arrays.asList(1));
            assertEquals(Arrays.asList("a", "d"), keyColumnNames(abcd, other));
            other.addColumn(0, "c", Arrays.asList(1));
            assertEquals(Arrays.asList("a", "c", "d"), keyColumnNames(abcd, other));
            other.swapColumns("c", "z");
            other.moveColumn("a", 0);
            assertEquals(Arrays.asList("a", "c", "d"), keyColumnNames(abcd, other));
            other.removeColumn("d");
            assertEquals(Arrays.asList("a", "c"), keyColumnNames(abcd, other));
            other.renameColumn("z", "b");
            assertEquals(Arrays.asList("a", "b", "c"), keyColumnNames(abcd, other));
            assertEquals(retainedNames(abcd, other), keyColumnNames(abcd, other));
            assertEquals(Arrays.asList("b", "c"), keyColumnNames(abcd, other.slice(0, 1, Arrays.asList("x", "c", "b"))));

            // Wide: 300 columns against a reversed subset with extra names.
            final List<String> wide = new ArrayList<>();
            final List<String> partial = new ArrayList<>();

            for (int i = 0; i < 300; i++) {
                wide.add("c" + i);

                if (i % 7 != 3) {
                    partial.add(0, "c" + i);
                }
            }

            partial.add(150, "extra1");
            partial.add("extra2");
            final Dataset wideDs = oneRow(wide);
            final Dataset partialDs = oneRow(partial);
            assertEquals(retainedNames(wideDs, partialDs), keyColumnNames(wideDs, partialDs));
            assertEquals(retainedNames(partialDs, wideDs), keyColumnNames(partialDs, wideDs));
        }

        private Dataset table(final List<String> names, final Object[][] rows) {
            return Dataset.rows(names, rows);
        }

        // G061-02: every key-less set operation equals its explicit-key overload with the common columns (reordered and
        // partially overlapping schemas, duplicates, nulls).
        @Test
        void testKeylessSetOperations_matchExplicitKeyOverloads() {
            final Dataset left = table(Arrays.asList("id", "k", "v", "onlyLeft"), new Object[][] { { 1, "a", 10, "l1" }, { 2, "b", null, "l2" },
                    { 2, "b", null, "l3" }, { 3, null, 30, "l4" }, { 4, "d", 40, "l5" }, { 1, "a", 10, "l6" } });
            final Dataset right = table(Arrays.asList("v", "x", "k", "id"),
                    new Object[][] { { 10, "x1", "a", 1 }, { null, "x2", "b", 2 }, { 30, "x3", null, 3 }, { 99, "x4", "z", 9 }, { 10, "x5", "a", 1 } });
            final List<String> keys = Arrays.asList("id", "k", "v");

            assertEquals(left.semiJoin(right, keys), left.semiJoin(right));
            assertEquals(left.antiJoin(right, keys), left.antiJoin(right));
            assertEquals(left.unionBy(right, keys, false), left.union(right, false));
            assertEquals(left.intersectBy(right, keys, false), left.intersect(right, false));
            assertEquals(left.intersectAllBy(right, keys, false), left.intersectAll(right, false));
            assertEquals(left.exceptBy(right, keys, false), left.except(right, false));
            assertEquals(left.exceptAllBy(right, keys, false), left.exceptAll(right, false));
            assertEquals(Arrays.asList(1, 2, 2, 3, 1), new ArrayList<>(left.semiJoin(right).getColumn("id")));
            assertEquals(Arrays.asList(4), new ArrayList<>(left.antiJoin(right).getColumn("id")));
            assertEquals(Arrays.asList(1, 2, 3, 4, 9), new ArrayList<>(left.union(right, false).getColumn("id")));

            // Same column set in another order: the requiresSameColumns=true overloads accept it.
            final Dataset reordered = table(Arrays.asList("onlyLeft", "v", "id", "k"), new Object[][] { { "l1", 10, 1, "a" }, { "zz", 40, 4, "d" } });
            final List<String> all = Arrays.asList("id", "k", "v", "onlyLeft");
            assertEquals(left.unionBy(reordered, all, true), left.union(reordered));
            assertEquals(left.intersectBy(reordered, all, true), left.intersect(reordered));
            assertEquals(left.intersectAllBy(reordered, all, true), left.intersectAll(reordered));
            assertEquals(left.exceptBy(reordered, all, true), left.except(reordered));
            assertEquals(left.exceptAllBy(reordered, all, true), left.exceptAll(reordered));
            assertEquals(Arrays.asList("l1"), new ArrayList<>(left.intersectAll(reordered).getColumn("onlyLeft")));
            assertEquals(Arrays.asList("l2", "l3", "l4", "l5", "l6"), new ArrayList<>(left.exceptAll(reordered).getColumn("onlyLeft")));
        }

        // G061-03: the same-column-set check of append/prepend/merge(.., true) and of the requiresSameColumns set operations:
        // order-insensitive, count-sensitive, same message, and following every change of this Dataset's columns.
        @Test
        void testRequiresSameColumns_appendPrependMergeAndSetOperations() {
            final Dataset target = table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
            target.append(table(Arrays.asList("c", "a", "b"), new Object[][] { { 30, 10, 20 } }));
            target.prepend(table(Arrays.asList("b", "c", "a"), new Object[][] { { -2, -3, -1 } }));
            target.merge(table(Arrays.asList("b", "a", "c"), new Object[][] { { 200, 100, 300 } }), true);
            assertEquals(Arrays.asList(-1, 1, 10, 100), new ArrayList<>(target.getColumn("a")));
            assertEquals(Arrays.asList(-3, 3, 30, 300), new ArrayList<>(target.getColumn("c")));

            final Dataset abd = table(Arrays.asList("a", "b", "d"), new Object[][] { { 1, 2, 4 } });
            final Dataset ab = table(Arrays.asList("a", "b"), new Object[][] { { 1, 2 } });
            final Dataset abcd = table(Arrays.asList("a", "b", "c", "d"), new Object[][] { { 1, 2, 3, 4 } });
            final String prefix = "These two Datasets do not have the same column names: [a, b, c], ";
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> target.append(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> target.prepend(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> target.merge(abd, true)).getMessage());
            assertEquals(prefix + "[a, b]", assertThrows(IllegalArgumentException.class, () -> target.append(ab)).getMessage());
            assertEquals(prefix + "[a, b, c, d]", assertThrows(IllegalArgumentException.class, () -> target.append(abcd)).getMessage());
            assertEquals(4, target.size());

            final Dataset abc = table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.union(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.intersect(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.except(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.intersectAll(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.exceptAll(abd)).getMessage());
            assertEquals(prefix + "[a, b, d]", assertThrows(IllegalArgumentException.class, () -> abc.unionBy(abd, Arrays.asList("a"), true)).getMessage());
            abc.merge(abd, false);
            assertEquals(Arrays.asList("a", "b", "c", "d"), abc.columnNames());

            // This Dataset's column index must follow renames, swaps, moves, appends, inserts and removals.
            final Dataset ds = table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } });
            assertTrue(ds.containsColumn("a"));
            ds.renameColumn("b", "z");
            ds.append(table(Arrays.asList("z", "a", "c"), new Object[][] { { 21, 11, 31 } }));
            assertThrows(IllegalArgumentException.class, () -> ds.append(table(Arrays.asList("a", "b", "c"), new Object[][] { { 1, 2, 3 } })));
            ds.swapColumns("a", "c");
            ds.moveColumn("z", 0);
            assertTrue(ds.containsColumn("z"));
            ds.addColumn("e", Arrays.asList(51, 52));
            ds.append(table(Arrays.asList("e", "c", "a", "z"), new Object[][] { { 53, 32, 12, 22 } }));
            ds.addColumn(1, "f", Arrays.asList(61, 62, 63));
            ds.prepend(table(Arrays.asList("f", "e", "c", "a", "z"), new Object[][] { { 60, 50, 30, 10, 20 } }));
            ds.removeColumn("c");
            assertThrows(IllegalArgumentException.class, () -> ds.append(table(Arrays.asList("f", "e", "c", "a"), new Object[][] { { 6, 5, 3, 1 } })));
            ds.append(table(Arrays.asList("a", "e", "f", "z"), new Object[][] { { 13, 54, 64, 23 } }));
            assertEquals(Arrays.asList("z", "f", "a", "e"), ds.columnNames());
            assertEquals(Arrays.asList(10, 1, 11, 12, 13), new ArrayList<>(ds.getColumn("a")));
            assertEquals(Arrays.asList(20, 2, 21, 22, 23), new ArrayList<>(ds.getColumn("z")));
            assertEquals(Arrays.asList(50, 51, 52, 53, 54), new ArrayList<>(ds.getColumn("e")));
            assertEquals(Arrays.asList(60, 61, 62, 63, 64), new ArrayList<>(ds.getColumn("f")));

            // Wide schemas, reversed: values land in the right columns; one differing name still fails.
            final List<String> wide = new ArrayList<>();
            final Object[] wideRow = new Object[300];

            for (int i = 0; i < 300; i++) {
                wide.add("c" + i);
                wideRow[i] = i;
            }

            final List<String> reversed = new ArrayList<>(wide);
            Collections.reverse(reversed);
            final Object[] reversedRow = new Object[300];

            for (int i = 0; i < 300; i++) {
                reversedRow[i] = 1000 + 299 - i;
            }

            final Dataset wideDs = table(wide, new Object[][] { wideRow });
            wideDs.append(table(reversed, new Object[][] { reversedRow }));
            assertEquals(Arrays.asList(0, 1000), new ArrayList<>(wideDs.getColumn("c0")));
            assertEquals(Arrays.asList(299, 1299), new ArrayList<>(wideDs.getColumn("c299")));
            final List<String> almost = new ArrayList<>(reversed);
            almost.set(299, "other");
            assertThrows(IllegalArgumentException.class, () -> wideDs.append(table(almost, new Object[][] { reversedRow })));
            assertEquals(2, wideDs.size());
        }

        // G061-02/03: an invalidated slice on either side still fails with ConcurrentModificationException.
        @Test
        void testColumnNameChecks_invalidatedSliceStillFailsFast() {
            final Dataset parent = table(Arrays.asList("a", "b"), new Object[][] { { 1, 2 }, { 3, 4 } });
            final Dataset slice = parent.slice(0, 1);
            final Dataset other = table(Arrays.asList("a", "b"), new Object[][] { { 1, 2 } });
            assertEquals(1, other.semiJoin(slice).size());
            parent.addRow(new Object[] { 5, 6 });

            assertThrows(java.util.ConcurrentModificationException.class, () -> other.semiJoin(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.antiJoin(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.union(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.intersect(slice, false));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.exceptAll(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.semiJoin(other));
            assertThrows(java.util.ConcurrentModificationException.class, () -> slice.union(other));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.append(slice));
            assertThrows(java.util.ConcurrentModificationException.class, () -> other.merge(slice, true));
            final Dataset noColumns = new RowDataset(new ArrayList<>(), new ArrayList<>());
            assertThrows(java.util.ConcurrentModificationException.class, () -> noColumns.semiJoin(slice));
            assertEquals(1, other.size());
        }
    }

    // ---- perf review 2026-10-08 G061 end ----
    // ---- perf review 2026-10-08 G062 begin ----
    @Nested
    class G062 {
        private WeakReference<Object> g062Payload;

        private boolean g062AwaitCleared(final WeakReference<?> ref) {
            for (int i = 0; i < 50 && ref.get() != null; i++) {
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return ref.get() == null;
        }

        // Built in a separate frame so that only the returned sequence references the payload array.
        private Seq<Integer, RuntimeException> g062ConcatOverPrivateArray(final int n) {
            final Integer[] payload = new Integer[n];

            for (int i = 0; i < n; i++) {
                payload[i] = i;
            }

            g062Payload = new WeakReference<>(payload);

            return Seq.concat(Seq.<Integer, RuntimeException> of(payload), null, Seq.<Integer, RuntimeException> of(-1, -2));
        }

        // Built in a separate frame so that only the returned sequence references the elements.
        private Seq<String, RuntimeException> g062SortedThenAppend(final int n) {
            final List<String> values = new ArrayList<>(n);

            for (int i = n - 1; i >= 0; i--) {
                values.add(String.valueOf(1_000_000 + i));
            }

            g062Payload = new WeakReference<>(values.get(n / 2));

            return Seq.<String, RuntimeException> of(values).sorted().append(Seq.<String, RuntimeException> of("tail1", "tail2"));
        }

        // G062-01: an exhausted (and closed) input of concat(..) is no longer reachable from the concatenation while the later inputs run
        @Test
        void testConcat_releasesExhaustedInputWhileLaterInputsRun() {
            final Seq<Integer, RuntimeException> seq = g062ConcatOverPrivateArray(100_000);
            final boolean[] released = { false };
            final long[] sum = { 0 };
            final List<Integer> tail = new ArrayList<>();

            seq.forEach(x -> {
                if (x < 0) {
                    if (x == -1) {
                        released[0] = g062AwaitCleared(g062Payload);
                    }

                    tail.add(x);
                } else {
                    sum[0] += x;
                }
            });

            assertEquals(4_999_950_000L, sum[0]);
            assertEquals(Arrays.asList(-1, -2), tail);
            assertTrue(released[0], "the exhausted first input (and its array) is still reachable from the concatenation");
        }

        // G062-01: append(..) delegates to concat(this, s): the exhausted receiver (here a sorted() buffer) is released too
        @Test
        void testAppend_releasesExhaustedSortedReceiverWhileAppendedSequenceRuns() {
            final Seq<String, RuntimeException> seq = g062SortedThenAppend(20_000);
            final boolean[] released = { false };
            final List<String> seen = new ArrayList<>();

            seq.forEach(x -> {
                if (x.equals("tail1")) {
                    released[0] = g062AwaitCleared(g062Payload);
                }

                if (x.startsWith("tail") || seen.size() < 3) {
                    seen.add(x);
                }
            });

            assertEquals(Arrays.asList("1000000", "1000001", "1000002", "tail1", "tail2"), seen);
            assertTrue(released[0], "the exhausted sorted receiver (and its buffer) is still reachable from the appended sequence");
        }

        // G062-01: order, null inputs, close order (each exhausted input before the next is activated, the rest on close)
        @Test
        void testConcat_orderNullInputsAndCloseOrder() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, RuntimeException> a = Seq.<Integer, RuntimeException> of(1, 2).onClose(() -> log.add("closeA"));
            final Seq<Integer, RuntimeException> b = Seq.<Integer, RuntimeException> of(3).onClose(() -> log.add("closeB"));
            final Seq<Integer, RuntimeException> c = Seq.<Integer, RuntimeException> of(4, 5).onClose(() -> log.add("closeC"));
            final Seq<Integer, RuntimeException> d = Seq.<Integer, RuntimeException> of(6).onClose(() -> log.add("closeD"));
            final List<Seq<Integer, RuntimeException>> input = new ArrayList<>(Arrays.asList(a, null, b, null, c, d));
            final Seq<Integer, RuntimeException> seq = Seq.concat(input);
            final Throwables.Iterator<Integer, RuntimeException> iter = seq.iteratorEx();
            final List<Integer> seen = new ArrayList<>();

            while (seen.size() < 4 && iter.hasNext()) {
                final Integer x = iter.next();
                seen.add(x);
                log.add("next" + x);
            }

            seq.close();

            assertEquals(Arrays.asList(1, 2, 3, 4), seen);
            assertEquals(Arrays.asList("next1", "next2", "closeA", "next3", "closeB", "next4", "closeC", "closeD"), log);
            // only the private snapshot is touched: the caller's collection keeps its elements
            assertEquals(6, input.size());
            assertSame(a, input.get(0));
            assertNull(input.get(1));
            assertSame(b, input.get(2));
            assertSame(c, input.get(4));
            assertSame(d, input.get(5));
            assertThrows(IllegalStateException.class, a::toList);
            assertThrows(IllegalStateException.class, d::toList);

            // varargs: the caller's array is not modified either
            final Seq<Integer, RuntimeException> e = Seq.of(7, 8);
            final Seq<Integer, RuntimeException> f = Seq.of(9);
            @SuppressWarnings("unchecked")
            final Seq<Integer, RuntimeException>[] array = new Seq[] { e, null, f };
            assertEquals(Arrays.asList(7, 8, 9), Seq.concat(array).toList());
            assertSame(e, array[0]);
            assertNull(array[1]);
            assertSame(f, array[2]);

            assertEquals(Collections.emptyList(), Seq.concat(Arrays.<Seq<Integer, RuntimeException>> asList(null, null)).toList());
            assertEquals(Arrays.asList(1, 2, 3, 4), Seq.<Integer, RuntimeException> of(1, 2).append(Seq.<Integer, RuntimeException> of(3, 4)).toList());
            assertEquals(Arrays.asList(3, 4, 1, 2), Seq.<Integer, RuntimeException> of(1, 2).prepend(Seq.<Integer, RuntimeException> of(3, 4)).toList());
            assertEquals(5L,
                    Seq.concat(Seq.<Integer, RuntimeException> of(1, 2), Seq.<Integer, RuntimeException> empty(), Seq.<Integer, RuntimeException> of(3, 4, 5))
                            .count());
        }

        // G062-01: a failing close of an exhausted input and an already-closed input propagate from hasNext(); a retry continues
        @Test
        void testConcat_failingCloseAndClosedInputThenRetry() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, RuntimeException> a = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
                log.add("closeA");
                throw new IllegalStateException("closeA failed");
            });
            final Seq<Integer, RuntimeException> closed = Seq.of(99);
            closed.close();
            final Seq<Integer, RuntimeException> c = Seq.<Integer, RuntimeException> of(3).onClose(() -> log.add("closeC"));
            final Seq<Integer, RuntimeException> seq = Seq.concat(Arrays.asList(a, closed, c));
            final Throwables.Iterator<Integer, RuntimeException> iter = seq.iteratorEx();

            assertEquals(1, iter.next());
            final IllegalStateException closeFailure = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("closeA failed", closeFailure.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry: `a` is closed already, the next input is closed too and is rejected when reached
            final IllegalStateException rejected = assertThrows(IllegalStateException.class, iter::hasNext);
            assertEquals("This sequence is already closed.", rejected.getMessage());
            assertEquals(Arrays.asList("closeA"), log);

            // retry again: traversal continues with the input after the rejected one
            assertTrue(iter.hasNext());
            assertEquals(3, iter.next());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::next);

            seq.close();
            assertEquals(Arrays.asList("closeA", "closeC"), log);

            // the same sequence twice: the second occurrence is rejected, as before
            final Seq<Integer, RuntimeException> twice = Seq.of(1, 2);
            final List<Integer> collected = new ArrayList<>();
            assertThrows(IllegalStateException.class, () -> Seq.concat(twice, twice).forEach(collected::add));
            assertEquals(Arrays.asList(1, 2), collected);

            // a terminal operation over a closed input closes every other input
            final List<String> log2 = new ArrayList<>();
            final Seq<Integer, RuntimeException> x = Seq.<Integer, RuntimeException> of(1).onClose(() -> log2.add("closeX"));
            final Seq<Integer, RuntimeException> closed2 = Seq.of(2);
            closed2.close();
            final Seq<Integer, RuntimeException> z = Seq.<Integer, RuntimeException> of(3).onClose(() -> log2.add("closeZ"));
            assertThrows(IllegalStateException.class, () -> Seq.concat(x, closed2, z).toList());
            assertEquals(Arrays.asList("closeX", "closeZ"), log2);
        }
    }

    // ---- perf review 2026-10-08 G062 end ----
    // ---- perf review 2026-10-08 G064 begin ----
    @Nested
    class G064 {

        /**
         * A source whose resource release is observable: logs "name" when its resource is closed and optionally fails then.
         */
        private Throwables.Iterator<Integer, Exception> source(final String name, final List<String> log, final int size, final boolean failOnClose) {
            return new Throwables.Iterator<>() {
                private int next = 1;

                @Override
                public boolean hasNext() {
                    return next <= size;
                }

                @Override
                public Integer next() {
                    if (next > size) {
                        throw new java.util.NoSuchElementException();
                    }

                    return next++;
                }

                @Override
                protected void closeResourceInternal() {
                    log.add(name);

                    if (failOnClose) {
                        throw new IllegalStateException(name + " failed");
                    }
                }
            };
        }

        // G064-01: the single-handler deque of a created sequence still runs its resource handler once, before onClose handlers
        @Test
        void testCreate_resourceHandlerRunsOnceBeforeOnCloseHandlers() throws Exception {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.of(source("resource", log, 3, false)).onClose(() -> log.add("h1")).onClose(() -> log.add("h2"));

            assertEquals(Arrays.asList(1, 2, 3), seq.toList());
            assertEquals(Arrays.asList("resource", "h1", "h2"), log);

            seq.close();
            assertEquals(Arrays.asList("resource", "h1", "h2"), log);

            final List<String> log2 = new ArrayList<>();
            final Seq<Integer, Exception> plain = Seq.of(source("resource", log2, 0, false));
            assertEquals(0, plain.count());
            assertEquals(Collections.singletonList("resource"), log2);
            plain.close();
            assertEquals(Collections.singletonList("resource"), log2);
        }

        // G064-01: explicit close before traversal releases the resource once and the sequence then rejects reuse
        @Test
        void testCreate_explicitCloseBeforeTraversal() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.of(source("resource", log, 3, false));

            seq.close();
            seq.close();
            assertEquals(Collections.singletonList("resource"), log);
            assertThrows(IllegalStateException.class, seq::toList);
            assertThrows(IllegalStateException.class, () -> seq.onClose(() -> log.add("late")));
            assertEquals(Collections.singletonList("resource"), log);
        }

        // G064-01: a derived pipeline closes the created source exactly once, ahead of the handlers registered downstream
        @Test
        void testCreate_derivedPipelineClosesSourceOnce() throws Exception {
            final List<String> log = new ArrayList<>();
            final List<Integer> result = Seq.of(source("resource", log, 6, false))
                    .onClose(() -> log.add("h0"))
                    .filter(i -> i % 2 == 0)
                    .onClose(() -> log.add("h1"))
                    .map(i -> i * 10)
                    .onClose(() -> log.add("h2"))
                    .toList();

            assertEquals(Arrays.asList(20, 40, 60), result);
            assertEquals(Arrays.asList("resource", "h0", "h1", "h2"), log);
        }

        // G064-01: every inner sequence created per element by flatMap is closed exactly once, in encounter order
        @Test
        void testCreate_flatMapInnerSequencesEachClosedOnce() throws Exception {
            final List<String> log = new ArrayList<>();
            final AtomicInteger onCloseCount = new AtomicInteger();
            final List<Integer> result = Seq.<Integer, Exception> of(1, 2, 3, 4)
                    .flatMap(i -> Seq.of(source("inner" + i, log, i, false)).onClose(onCloseCount::incrementAndGet))
                    .toList();

            assertEquals(Arrays.asList(1, 1, 2, 1, 2, 3, 1, 2, 3, 4), result);
            assertEquals(Arrays.asList("inner1", "inner2", "inner3", "inner4"), log);
            assertEquals(4, onCloseCount.get());

            final List<String> log2 = new ArrayList<>();
            assertEquals(2, Seq.<Integer, Exception> of(1, 2, 3).flatMap(i -> Seq.of(source("x" + i, log2, 5, false))).limit(2).count());
            assertEquals(Collections.singletonList("x1"), log2);
        }

        // G064-01: a failing resource release still runs the later handlers and propagates the same exception
        @Test
        void testCreate_failingResourceCloseStillRunsLaterHandlers() {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.of(source("resource", log, 2, true)).onClose(() -> log.add("h1"));

            final IllegalStateException e = assertThrows(IllegalStateException.class, seq::toList);
            assertEquals("resource failed", e.getMessage());
            assertEquals(Arrays.asList("resource", "h1"), log);

            seq.close();
            assertEquals(Arrays.asList("resource", "h1"), log);
        }

        // G064-01: a sequence rebuilt from a stream (create(Stream) -> create(iterator)) closes that stream once
        @Test
        void testCreate_streamBackedSequenceClosesItsStreamOnce() throws Exception {
            final List<String> log = new ArrayList<>();
            final Seq<Integer, Exception> seq = Seq.<Integer, Exception> of(1, 2, 3)
                    .onClose(() -> log.add("source"))
                    .transformViaStream(st -> st.map(i -> i + 1).onClose(() -> log.add("stream")))
                    .onClose(() -> log.add("h1"));

            assertEquals(Arrays.asList(2, 3, 4), seq.toList());
            assertEquals(Arrays.asList("source", "stream", "h1"), log);

            seq.close();
            assertEquals(Arrays.asList("source", "stream", "h1"), log);
        }
    }

    // ---- perf review 2026-10-08 G064 end ----
    // ---- perf review 2026-10-08 G065 begin ----
    @Nested
    class G065 {
        private Set<String> identityMembershipSet(final String... elements) {
            final Set<String> set = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            Collections.addAll(set, elements);
            return set;
        }

        private TreeSet<String> sortedSet(final java.util.Comparator<String> comparator, final String... elements) {
            final TreeSet<String> set = new TreeSet<>(comparator);
            Collections.addAll(set, elements);
            return set;
        }

        // G065-01: sorted and identity value sets (copied without the identity index) keep their comparator / identity membership,
        // their iteration order and their independence; emptied, null and equals-based values beside them are unchanged, and the
        // value supplier is never called.
        @Test
        void testToImmutableMap_sortedAndIdentityValueSetsWithoutSupplier() {
            final String first = new String("x");
            final String second = new String("x");
            final IdentityHashSet<String> identity = new IdentityHashSet<>();
            identity.add(first);
            identity.add(second);
            final TreeSet<String> caseInsensitive = sortedSet(String.CASE_INSENSITIVE_ORDER, "b", "A", "c");
            final TreeSet<String> reversed = sortedSet(java.util.Comparator.reverseOrder(), "x", "z", "y");
            final java.util.concurrent.ConcurrentSkipListSet<String> skipList = new java.util.concurrent.ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
            skipList.add("Q");
            skipList.add("p");
            final java.util.NavigableSet<String> descending = sortedSet(null, "m", "n").descendingSet();

            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("sorted", caseInsensitive);
            backing.put("identity", identity);
            backing.put("plain", new HashSet<>(Arrays.asList("k")));
            backing.put("reversed", reversed);
            backing.put("skipList", skipList);
            backing.put("descending", descending);

            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return new HashSet<>();
            });
            // reachable after wrapping: a key whose sorted set was emptied in place, and a null value in the wrapped map
            final TreeSet<String> emptied = sortedSet(String.CASE_INSENSITIVE_ORDER, "gone");
            backing.put("emptied", emptied);
            emptied.clear();
            backing.put("null", null);

            final List<ImmutableMap<String, ImmutableSet<String>>> snapshots = List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new),
                    multimap.toImmutableMap(size -> new TreeMap<>()));

            // the sources change afterwards; no snapshot may follow them
            caseInsensitive.add("D");
            identity.clear();
            reversed.clear();
            skipList.add("r");
            descending.add("o");

            assertEquals(0, supplierCalls.get());

            for (final ImmutableMap<String, ImmutableSet<String>> snapshot : snapshots) {
                assertEquals(Set.of("sorted", "identity", "plain", "reversed", "skipList", "descending", "emptied", "null"), new HashSet<>(snapshot.keySet()));

                final ImmutableSet<String> sorted = snapshot.get("sorted");
                assertEquals(Arrays.asList("A", "b", "c"), new ArrayList<>(sorted));
                assertTrue(sorted.contains("a"));
                assertTrue(sorted.contains("B"));
                assertFalse(sorted.contains("d"));

                final ImmutableSet<String> identityCopy = snapshot.get("identity");
                assertEquals(2, identityCopy.size());
                assertTrue(identityCopy.contains(first));
                assertTrue(identityCopy.contains(second));
                assertFalse(identityCopy.contains(new String("x")));

                assertEquals(Arrays.asList("k"), new ArrayList<>(snapshot.get("plain")));
                assertEquals(Arrays.asList("z", "y", "x"), new ArrayList<>(snapshot.get("reversed")));
                assertEquals(Arrays.asList("p", "Q"), new ArrayList<>(snapshot.get("skipList")));
                assertTrue(snapshot.get("skipList").contains("q"));
                assertEquals(Arrays.asList("n", "m"), new ArrayList<>(snapshot.get("descending")));
                assertTrue(snapshot.get("emptied").isEmpty());
                assertTrue(snapshot.get("null").isEmpty());

                assertThrows(UnsupportedOperationException.class, () -> sorted.add("e"));
                assertThrows(UnsupportedOperationException.class, () -> identityCopy.remove(first));
                assertThrows(UnsupportedOperationException.class, () -> snapshot.remove("sorted"));
            }

            // every snapshot owns its own copies, and keeps the key order of the map it was built in
            assertNotSame(snapshots.get(0).get("sorted"), snapshots.get(1).get("sorted"));
            assertNotSame(snapshots.get(0).get("identity"), snapshots.get(1).get("identity"));
            assertEquals(Arrays.asList("sorted", "identity", "plain", "reversed", "skipList", "descending", "emptied", "null"),
                    new ArrayList<>(snapshots.get(0).keySet()));
            assertEquals(new ArrayList<>(snapshots.get(0).keySet()), new ArrayList<>(snapshots.get(1).keySet()));
            assertEquals(Arrays.asList("descending", "emptied", "identity", "null", "plain", "reversed", "skipList", "sorted"),
                    new ArrayList<>(snapshots.get(2).keySet()));
        }

        // G065-01: the common sorted configuration (TreeMap keys, TreeSet values) snapshots in key and value order, independently.
        @Test
        void testToImmutableMap_treeMapTreeSetMultimap() {
            final SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(TreeMap.class, TreeSet.class);
            for (int i = 0; i < 50; i++) {
                multimap.put("k" + (i % 7), 100 - i);
            }

            for (final ImmutableMap<String, ImmutableSet<Integer>> snapshot : List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new))) {
                assertEquals(new ArrayList<>(multimap.keySet()), new ArrayList<>(snapshot.keySet()));
                for (final String key : multimap.keySet()) {
                    assertEquals(new ArrayList<>(multimap.get(key)), new ArrayList<>(snapshot.get(key)));
                }
                multimap.put("k0", -1);
                assertFalse(snapshot.get("k0").contains(-1));
                multimap.removeEntry("k0", -1);
            }
        }

        // G065-01: sorted and identity sets before and after sets that the equals-based copy shrinks. The identity index is now
        // built at the first shrinking set rather than the first sorted set; the value supplier is still called once per
        // shrinking set only, and every set keeps its membership rule.
        @Test
        void testToImmutableMap_sortedSetsMixedWithShrinkingSets() {
            final String first = new String("x");
            final String second = new String("x");
            final String third = new String("y");
            final String fourth = new String("y");
            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("t1", sortedSet(String.CASE_INSENSITIVE_ORDER, "b", "A"));
            backing.put("u1", identityMembershipSet(first, second));
            backing.put("t2", sortedSet(java.util.Comparator.reverseOrder(), "c", "d"));
            backing.put("u2", identityMembershipSet(third, fourth));
            backing.put("i1", new IdentityHashSet<>(Arrays.asList(first, second)));

            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return identityMembershipSet();
            });

            for (final ImmutableMap<String, ImmutableSet<String>> snapshot : List.of(multimap.toImmutableMap(), multimap.toImmutableMap(LinkedHashMap::new))) {
                assertEquals(Arrays.asList("t1", "u1", "t2", "u2", "i1"), new ArrayList<>(snapshot.keySet()));
                assertEquals(Arrays.asList("A", "b"), new ArrayList<>(snapshot.get("t1")));
                assertTrue(snapshot.get("t1").contains("a"));
                assertEquals(Arrays.asList("d", "c"), new ArrayList<>(snapshot.get("t2")));
                for (final String key : Arrays.asList("u1", "i1")) {
                    assertEquals(2, snapshot.get(key).size());
                    assertTrue(snapshot.get(key).contains(first));
                    assertTrue(snapshot.get(key).contains(second));
                    assertFalse(snapshot.get(key).contains(new String("x")));
                }
                assertEquals(2, snapshot.get("u2").size());
                assertTrue(snapshot.get("u2").contains(third));
                assertTrue(snapshot.get("u2").contains(fourth));
                assertNotSame(backing.get("u1"), snapshot.get("u1"));
            }

            // u1 and u2, for each of the two snapshots
            assertEquals(4, supplierCalls.get());
            assertEquals(2, backing.get("u1").size());
            assertEquals(2, backing.get("u2").size());
        }

        // G065-01: with sorted sets ahead of the shrinking sets, a value supplier that hands back a source set (sorted, emptied
        // sorted, or the set being copied), the same set twice, or a non-empty set is still rejected with the same exception
        // and message, and so is a supplied set that merges the distinct elements; no source is modified.
        @Test
        void testToImmutableMap_sortedSetsThenRejectedSupplierResults() {
            final TreeSet<String> sortedSource = sortedSet(String.CASE_INSENSITIVE_ORDER, "a");
            final TreeSet<String> emptiedSortedSource = sortedSet(null, "gone");
            final Set<String> shrinking = identityMembershipSet(new String("x"), new String("x"));
            final Set<String> shrinking2 = identityMembershipSet(new String("y"), new String("y"));

            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("t1", sortedSource);
            backing.put("t2", emptiedSortedSource);
            backing.put("u1", shrinking);
            backing.put("u2", shrinking2);

            final Set<String> reused = identityMembershipSet();
            final SetMultimap<String, String> returnsSortedSource = SetMultimap.wrap(backing, () -> sortedSource);
            final SetMultimap<String, String> returnsEmptiedSortedSource = SetMultimap.wrap(backing, () -> emptiedSortedSource);
            final SetMultimap<String, String> returnsShrinkingSource = SetMultimap.wrap(backing, () -> shrinking);
            final SetMultimap<String, String> returnsSameSetTwice = SetMultimap.wrap(backing, () -> reused);
            final SetMultimap<String, String> returnsNonEmpty = SetMultimap.wrap(backing, () -> identityMembershipSet("z"));
            // the first value is a TreeSet, so new value sets are natural-order TreeSets, which merge the equal elements of u1
            final SetMultimap<String, String> mergingSupplier = SetMultimap.wrap(backing);
            emptiedSortedSource.clear();

            final String inUse = "The value supplier returned a collection already in use; it must return a new empty collection on every call";
            final String nonEmpty = "The value supplier returned a non-empty collection; it must return a new empty collection on every call";

            for (final SetMultimap<String, String> multimap : List.of(returnsSortedSource, returnsEmptiedSortedSource, returnsShrinkingSource,
                    returnsSameSetTwice)) {
                reused.clear();
                assertEquals(inUse, assertThrows(IllegalArgumentException.class, multimap::toImmutableMap).getMessage());
                reused.clear();
                assertEquals(inUse, assertThrows(IllegalArgumentException.class, () -> multimap.toImmutableMap(LinkedHashMap::new)).getMessage());
            }

            assertEquals(nonEmpty, assertThrows(IllegalArgumentException.class, returnsNonEmpty::toImmutableMap).getMessage());
            assertEquals(nonEmpty, assertThrows(IllegalArgumentException.class, () -> returnsNonEmpty.toImmutableMap(LinkedHashMap::new)).getMessage());

            assertEquals("Set copy loses distinct source elements", assertThrows(IllegalArgumentException.class, mergingSupplier::toImmutableMap).getMessage());

            assertEquals(Arrays.asList("a"), new ArrayList<>(sortedSource));
            assertTrue(emptiedSortedSource.isEmpty());
            assertEquals(2, shrinking.size());
            assertEquals(2, shrinking2.size());
        }
    }

    // ---- perf review 2026-10-08 G065 end ----
    // ---- perf review 2026-10-08 G066 begin ----
    @Nested
    class G066 {

        private Sheet<String, String, Integer> sheet(final int rowCount, final int columnCount) {
            final List<String> rowKeys = new ArrayList<>();
            final List<String> columnKeys = new ArrayList<>();
            final Integer[][] rows = new Integer[rowCount][columnCount];

            for (int i = 0; i < rowCount; i++) {
                rowKeys.add("r" + i);

                for (int j = 0; j < columnCount; j++) {
                    rows[i][j] = (i + j) % 4 == 3 ? null : i * 10 + j;
                }
            }

            for (int j = 0; j < columnCount; j++) {
                columnKeys.add("c" + j);
            }

            return Sheet.rows(rowKeys, columnKeys, rows);
        }

        private List<Integer> expectedRowMajor(final Sheet<String, String, Integer> sheet, final int fromRowIndex, final int toRowIndex) {
            final List<Integer> expected = new ArrayList<>();

            for (int i = fromRowIndex; i < toRowIndex; i++) {
                for (int j = 0; j < sheet.columnCount(); j++) {
                    expected.add(sheet.getAt(i, j));
                }
            }

            return expected;
        }

        private List<Integer> expectedColumnMajor(final Sheet<String, String, Integer> sheet, final int fromColumnIndex, final int toColumnIndex) {
            final List<Integer> expected = new ArrayList<>();

            for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                for (int i = 0; i < sheet.rowCount(); i++) {
                    expected.add(sheet.getAt(i, j));
                }
            }

            return expected;
        }

        private List<Integer> drain(final Iterator<Integer> iter) {
            final List<Integer> result = new ArrayList<>();

            while (iter.hasNext()) {
                result.add(iter.next());
            }

            return result;
        }

        private com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iteratorEx(final com.landawn.abacus.util.stream.Stream<Integer> stream) {
            return (com.landawn.abacus.util.stream.ObjIteratorEx<Integer>) stream.iterator();
        }

        private void assertAdvanceAndCount(final java.util.function.Supplier<com.landawn.abacus.util.stream.Stream<Integer>> streamSupplier,
                final List<Integer> expected) {
            final long[] advances = { -1, 0, 1, 2, 3, 4, 5, 7, 11, 100, Long.MAX_VALUE };

            for (int consumed = 0; consumed <= expected.size(); consumed++) {
                for (final long n : advances) {
                    final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(streamSupplier.get());

                    for (int k = 0; k < consumed; k++) {
                        assertEquals(expected.get(k), iter.next());
                    }

                    iter.advance(n);

                    final int skipped = (int) Math.min(expected.size() - consumed, Math.max(0, n));
                    assertEquals(expected.subList(consumed + skipped, expected.size()), drain(iter), "consumed=" + consumed + ", n=" + n);
                    assertFalse(iter.hasNext());
                    assertThrows(java.util.NoSuchElementException.class, iter::next);
                }

                final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(streamSupplier.get());

                for (int k = 0; k < consumed; k++) {
                    iter.next();
                }

                assertEquals(expected.size() - consumed, iter.count());
                assertFalse(iter.hasNext());
                assertThrows(java.util.NoSuchElementException.class, iter::next);
                assertEquals(0, iter.count());
                iter.advance(3);
                assertFalse(iter.hasNext());
            }
        }

        // G066-01: rowMajorStream steps a (row, column) pair along with its cursor instead of dividing the cursor per element.
        @Test
        public void testRowMajorStream_everyRangeOfEveryShape() {
            final int[][] shapes = { { 1, 1 }, { 1, 5 }, { 5, 1 }, { 3, 4 }, { 4, 3 }, { 2, 7 } };

            for (final int[] shape : shapes) {
                final Sheet<String, String, Integer> sheet = sheet(shape[0], shape[1]);

                for (int from = 0; from <= shape[0]; from++) {
                    for (int to = from; to <= shape[0]; to++) {
                        assertEquals(expectedRowMajor(sheet, from, to), sheet.rowMajorStream(from, to).toList(),
                                shape[0] + "x" + shape[1] + " " + from + "-" + to);
                    }
                }

                assertEquals(expectedRowMajor(sheet, 0, shape[0]), sheet.rowMajorStream().toList());
            }
        }

        // G066-01: columnMajorStream steps a (column, row) pair along with its cursor instead of dividing the cursor per element.
        @Test
        public void testColumnMajorStream_everyRangeOfEveryShape() {
            final int[][] shapes = { { 1, 1 }, { 1, 5 }, { 5, 1 }, { 3, 4 }, { 4, 3 }, { 7, 2 } };

            for (final int[] shape : shapes) {
                final Sheet<String, String, Integer> sheet = sheet(shape[0], shape[1]);

                for (int from = 0; from <= shape[1]; from++) {
                    for (int to = from; to <= shape[1]; to++) {
                        assertEquals(expectedColumnMajor(sheet, from, to), sheet.columnMajorStream(from, to).toList(),
                                shape[0] + "x" + shape[1] + " " + from + "-" + to);
                    }
                }

                assertEquals(expectedColumnMajor(sheet, 0, shape[1]), sheet.columnMajorStream().toList());
            }
        }

        // G066-01: advance(n) moves only the cursor (the cell position is re-derived on the next read); count() consumes the rest.
        @Test
        public void testRowMajorStream_advanceAndCountFromEveryPosition() {
            final Sheet<String, String, Integer> sheet = sheet(4, 3);
            final Sheet<String, String, Integer> narrow = sheet(3, 1);

            assertAdvanceAndCount(() -> sheet.rowMajorStream(), expectedRowMajor(sheet, 0, 4));
            assertAdvanceAndCount(() -> sheet.rowMajorStream(1, 3), expectedRowMajor(sheet, 1, 3));
            assertAdvanceAndCount(() -> narrow.rowMajorStream(), expectedRowMajor(narrow, 0, 3));
            assertEquals(expectedRowMajor(sheet, 0, 4).subList(5, 12), sheet.rowMajorStream().skip(5).toList());
            assertEquals(expectedRowMajor(sheet, 0, 4).subList(2, 9), sheet.rowMajorStream().skip(2).limit(7).toList());
        }

        // G066-01: advance(n) moves only the cursor (the cell position is re-derived on the next read); count() consumes the rest.
        @Test
        public void testColumnMajorStream_advanceAndCountFromEveryPosition() {
            final Sheet<String, String, Integer> sheet = sheet(3, 4);
            final Sheet<String, String, Integer> flat = sheet(1, 3);

            assertAdvanceAndCount(() -> sheet.columnMajorStream(), expectedColumnMajor(sheet, 0, 4));
            assertAdvanceAndCount(() -> sheet.columnMajorStream(1, 3), expectedColumnMajor(sheet, 1, 3));
            assertAdvanceAndCount(() -> flat.columnMajorStream(), expectedColumnMajor(flat, 0, 3));
            assertEquals(expectedColumnMajor(sheet, 0, 4).subList(5, 12), sheet.columnMajorStream().skip(5).toList());
            assertEquals(expectedColumnMajor(sheet, 0, 4).subList(2, 9), sheet.columnMajorStream().skip(2).limit(7).toList());
        }

        // G066-01: a stream created before the Sheet had storage returns nulls, and after the first write the live values, also
        // when its position moved on (next/advance) while there was no storage.
        @Test
        public void testRowMajorStream_firstWriteAfterPartialConsumption() {
            final Sheet<String, String, Integer> sheet = new Sheet<>(Arrays.asList("r0", "r1", "r2"), Arrays.asList("c0", "c1"));
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(sheet.rowMajorStream());

            assertNull(iter.next()); // r0,c0
            iter.advance(1); // skips r0,c1
            assertNull(iter.next()); // r1,c0

            sheet.setAt(0, 0, 1);
            sheet.setAt(1, 1, 11);
            sheet.setAt(2, 0, 20);
            sheet.setAt(2, 1, 21);

            assertEquals(Integer.valueOf(11), iter.next()); // r1,c1
            assertEquals(Integer.valueOf(20), iter.next()); // r2,c0
            sheet.setAt(2, 1, 210);
            assertEquals(Integer.valueOf(210), iter.next()); // r2,c1
            assertFalse(iter.hasNext());

            final Sheet<String, String, Integer> other = new Sheet<>(Arrays.asList("r0", "r1"), Arrays.asList("c0", "c1", "c2"));
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter2 = iteratorEx(other.rowMajorStream(1, 2));
            assertNull(iter2.next()); // r1,c0
            other.setAt(1, 2, 12);
            other.setAt(1, 1, 11);
            assertEquals(Arrays.asList(11, 12), drain(iter2));
            assertEquals(Arrays.asList(null, null, null, null, 11, 12), other.rowMajorStream().toList());
        }

        // G066-01: as above, column-major.
        @Test
        public void testColumnMajorStream_firstWriteAfterPartialConsumption() {
            final Sheet<String, String, Integer> sheet = new Sheet<>(Arrays.asList("r0", "r1"), Arrays.asList("c0", "c1", "c2"));
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(sheet.columnMajorStream());

            assertNull(iter.next()); // r0,c0
            iter.advance(1); // skips r1,c0
            assertNull(iter.next()); // r0,c1

            sheet.setAt(1, 1, 11);
            sheet.setAt(0, 2, 2);
            sheet.setAt(1, 2, 12);

            assertEquals(Integer.valueOf(11), iter.next()); // r1,c1
            assertEquals(Integer.valueOf(2), iter.next()); // r0,c2
            sheet.setAt(1, 2, 120);
            assertEquals(Integer.valueOf(120), iter.next()); // r1,c2
            assertFalse(iter.hasNext());
            assertEquals(Arrays.asList(null, null, null, 11, 2, 120), sheet.columnMajorStream().toList());
        }

        // G066-01: (documented as undefined, pinned here as deterministic) a failed column read leaves the position unchanged and
        // a failed cell read has already moved past the cell, exactly as when the position was divided out of the cursor.
        @Test
        public void testRowMajorStream_failedReadsAfterStructuralChange() {
            final Sheet<String, String, Integer> columnRemoved = sheet(2, 3); // r0: 0 1 2, r1: 10 11 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(columnRemoved.rowMajorStream());
            assertEquals(Integer.valueOf(0), iter.next());
            assertEquals(Integer.valueOf(1), iter.next());
            columnRemoved.removeColumn("c0"); // two columns left: c1, c2
            assertThrows(IndexOutOfBoundsException.class, iter::next); // column index 2 is gone; position unchanged
            assertThrows(IndexOutOfBoundsException.class, iter::next);
            assertTrue(iter.hasNext());
            iter.advance(1); // now at row 1, column index 0 (c1 after the removal)
            assertEquals(Integer.valueOf(11), iter.next());
            assertNull(iter.next());
            assertThrows(IndexOutOfBoundsException.class, iter::next);

            final Sheet<String, String, Integer> rowRemoved = sheet(3, 2); // r0: 0 1, r1: 10 11, r2: 20 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter2 = iteratorEx(rowRemoved.rowMajorStream());
            assertEquals(Arrays.asList(0, 1, 10, 11), Arrays.asList(iter2.next(), iter2.next(), iter2.next(), iter2.next()));
            rowRemoved.removeRow("r0"); // rows left: r1, r2 at indexes 0, 1
            assertThrows(IndexOutOfBoundsException.class, iter2::next); // row index 2 is gone; position moved on
            assertThrows(IndexOutOfBoundsException.class, iter2::next);
            assertFalse(iter2.hasNext());
        }

        // G066-01: as above, column-major.
        @Test
        public void testColumnMajorStream_failedReadsAfterStructuralChange() {
            final Sheet<String, String, Integer> columnRemoved = sheet(2, 3); // c0: 0 10, c1: 1 11, c2: 2 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter = iteratorEx(columnRemoved.columnMajorStream());
            assertEquals(Arrays.asList(0, 10, 1, 11), Arrays.asList(iter.next(), iter.next(), iter.next(), iter.next()));
            columnRemoved.removeColumn("c0"); // two columns left: c1, c2
            assertThrows(IndexOutOfBoundsException.class, iter::next); // column index 2 is gone; position unchanged
            assertTrue(iter.hasNext());
            columnRemoved.addColumn("c3", Arrays.asList(3, 13));
            assertEquals(Integer.valueOf(3), iter.next());
            assertEquals(Integer.valueOf(13), iter.next());
            assertFalse(iter.hasNext());

            final Sheet<String, String, Integer> rowRemoved = sheet(3, 2); // c0: 0 10 20, c1: 1 11 null
            final com.landawn.abacus.util.stream.ObjIteratorEx<Integer> iter2 = iteratorEx(rowRemoved.columnMajorStream());
            assertEquals(Arrays.asList(0, 10), Arrays.asList(iter2.next(), iter2.next()));
            rowRemoved.removeRow("r0"); // rows left: r1, r2 at indexes 0, 1
            assertThrows(IndexOutOfBoundsException.class, iter2::next); // row index 2 is gone; position moved on
            assertEquals(Integer.valueOf(11), iter2.next()); // column index 1, row index 0 (r1)
            assertNull(iter2.next());
            assertThrows(IndexOutOfBoundsException.class, iter2::next);
            assertFalse(iter2.hasNext());
        }
    }

    // ---- perf review 2026-10-08 G066 end ----
    // ---- perf review 2026-10-08 G067 begin ----
    @Nested
    class G067 {

        private static final String[] RANGES = { "small", "audio", "full" };

        private short[] randomShorts(final Random random, final int length, final String range) {
            final short[] a = new short[length];

            for (int i = 0; i < length; i++) {
                if ("small".equals(range)) {
                    a[i] = (short) (random.nextInt(256) - 128);
                } else if ("audio".equals(range)) {
                    a[i] = (short) (random.nextInt(6001) - 3000);
                } else {
                    a[i] = (short) random.nextInt(65536);
                }
            }

            if (length > 5 && "full".equals(range)) {
                a[0] = Short.MIN_VALUE;
                a[1] = Short.MAX_VALUE;
                a[2] = (short) -1;
                a[3] = (short) 0;
                a[4] = (short) 128;
                a[5] = (short) -129;
            }

            return a;
        }

        private Map<Short, Integer> counts(final short[] a) {
            final Map<Short, Integer> counts = new HashMap<>();

            for (final short e : a) {
                counts.merge(e, 1, Integer::sum);
            }

            return counts;
        }

        private boolean take(final Map<Short, Integer> counts, final short e) {
            final Integer count = counts.get(e);

            if (count == null || count == 0) {
                return false;
            }

            counts.put(e, count - 1);
            return true;
        }

        /**
         * Boxed reference for the occurrence-based set operations: {intersection, difference, symmetricDifference}, where the
         * symmetric difference appends b's unmatched occurrences in index order.
         */
        private short[][] reference(final short[] a, final short[] b) {
            final short[] intersection = new short[a.length];
            final short[] difference = new short[a.length];
            final short[] symmetricDifference = new short[a.length + b.length];
            int ni = 0;
            int nd = 0;
            int ns = 0;

            Map<Short, Integer> counts = counts(b);

            for (final short e : a) {
                if (take(counts, e)) {
                    intersection[ni++] = e;
                } else {
                    difference[nd++] = e;
                }
            }

            counts = counts(b);

            for (final short e : a) {
                if (!take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            for (final short e : b) {
                if (take(counts, e)) {
                    symmetricDifference[ns++] = e;
                }
            }

            return new short[][] { Arrays.copyOf(intersection, ni), Arrays.copyOf(difference, nd), Arrays.copyOf(symmetricDifference, ns) };
        }

        private void assertFreshGrowable(final ShortList result, final ShortList list, final ShortList other, final String msg) {
            assertNotSame(list, result, msg);
            assertNotSame(other, result, msg);

            final int size = result.size();
            result.add((short) 42);
            assertEquals(size + 1, result.size(), msg);
            assertEquals((short) 42, result.getLast(), msg);
        }

        private void assertMultisetOps(final short[] a, final short[] b, final String msg) {
            final short[][] expected = reference(a, b);
            final ShortList list = ShortList.of(a.clone());
            final ShortList other = ShortList.of(b.clone());

            final ShortList intersection = list.intersection(other);
            final ShortList difference = list.difference(other);
            final ShortList symmetricDifference = list.symmetricDifference(other);

            assertArrayEquals(expected[0], intersection.toArray(), msg);
            assertArrayEquals(expected[1], difference.toArray(), msg);
            assertArrayEquals(expected[2], symmetricDifference.toArray(), msg);
            assertFreshGrowable(intersection, list, other, msg);
            assertFreshGrowable(difference, list, other, msg);
            assertFreshGrowable(symmetricDifference, list, other, msg);

            assertArrayEquals(expected[0], list.intersection(b.clone()).toArray(), msg);
            assertArrayEquals(expected[1], list.difference(b.clone()).toArray(), msg);
            assertArrayEquals(expected[2], list.symmetricDifference(b.clone()).toArray(), msg);

            // neither operand is modified
            assertArrayEquals(a, list.toArray(), msg);
            assertArrayEquals(b, other.toArray(), msg);
        }

        // G067-01/G067-02: ShortList intersection/difference/symmetricDifference take the count-table path when this list has more
        // than 65,536 elements even if the argument is small (<= 1024); results must match a boxed counting reference on both sides of
        // both thresholds (receiver 65,536/65,537, argument 1,024/1,025), for both overloads and three value ranges.
        @Test
        public void testShortListMultisetOps_largeReceiverMatchesBoxedReference() {
            final Random random = new Random(20261008L);
            final int[] receiverLengths = { 200, 65535, 65536, 65537, 66000 };
            final int[] argumentLengths = { 1, 2, 16, 500, 1024, 1025 };

            for (final int lengthA : receiverLengths) {
                for (final int lengthB : argumentLengths) {
                    for (final String range : RANGES) {
                        final short[] a = randomShorts(random, lengthA, range);
                        final short[] b = randomShorts(random, lengthB, range);

                        assertMultisetOps(a, b, "a=" + lengthA + ", b=" + lengthB + ", range=" + range);
                    }
                }
            }
        }

        // G067-01: edge cases of the large-receiver count-table path - one repeated value, argument values absent from this list,
        // partially cancelled duplicates in the argument, extreme values, a self argument, and a receiver holding every short value.
        @Test
        public void testShortListMultisetOps_largeReceiverEdgeCases() {
            final short[] sameValue = new short[70000];
            Arrays.fill(sameValue, (short) 300);
            assertMultisetOps(sameValue, new short[] { 300, 300, 301 }, "same value");
            assertMultisetOps(sameValue, new short[] { 301, -300, Short.MAX_VALUE, Short.MIN_VALUE }, "argument absent");
            assertMultisetOps(sameValue, new short[] { 300 }, "single argument");

            final ShortList repeated = ShortList.of(sameValue.clone());
            assertEquals(2, repeated.intersection(ShortList.of((short) 300, (short) 300, (short) 301)).size());
            assertEquals(69998, repeated.difference(ShortList.of((short) 300, (short) 300, (short) 301)).size());
            final ShortList sym = repeated.symmetricDifference(ShortList.of((short) 300, (short) 300, (short) 301));
            assertEquals(69999, sym.size());
            assertEquals((short) 301, sym.getLast());

            // one -7 cancels; b's surviving occurrences are its earliest ones in index order: [.., -7, 5]
            final short[] oneValue = new short[70000];
            Arrays.fill(oneValue, (short) 9);
            oneValue[12345] = -7;
            final ShortList oneValueList = ShortList.of(oneValue.clone());
            final ShortList oneValueSym = oneValueList.symmetricDifference(ShortList.of((short) -7, (short) 5, (short) -7));
            assertEquals(70001, oneValueSym.size());
            assertEquals((short) -7, oneValueSym.get(69999));
            assertEquals((short) 5, oneValueSym.get(70000));
            assertMultisetOps(oneValue, new short[] { -7, 5, -7 }, "partial cancellation");

            final short[] extremes = new short[65537];
            for (int i = 0; i < extremes.length; i++) {
                extremes[i] = (i & 1) == 0 ? Short.MIN_VALUE : Short.MAX_VALUE;
            }
            assertMultisetOps(extremes, new short[] { Short.MAX_VALUE, Short.MIN_VALUE, Short.MIN_VALUE, 0, -1 }, "extremes");

            final short[] all = new short[65536 + 7];
            for (int i = 0; i < all.length; i++) {
                all[i] = (short) (i * 7919);
            }
            assertMultisetOps(all, new short[] { 0, 0, Short.MIN_VALUE, Short.MAX_VALUE, -1, -1, 127, 128, -128, -129 }, "every value");

            final Random random = new Random(7L);
            final short[] big = randomShorts(random, 70000, "audio");
            final ShortList self = ShortList.of(big.clone());
            assertArrayEquals(big, self.intersection(self).toArray());
            assertEquals(0, self.difference(self).size());
            assertEquals(0, self.symmetricDifference(self).size());
            assertArrayEquals(big, self.toArray());

            // empty / null arguments keep their dedicated branches
            assertEquals(0, self.intersection((ShortList) null).size());
            assertArrayEquals(big, self.difference(new short[0]).toArray());
            assertArrayEquals(big, self.symmetricDifference((ShortList) null).toArray());
            assertArrayEquals(new short[] { 1, 2 }, new ShortList().symmetricDifference(ShortList.of((short) 1, (short) 2)).toArray());
        }
    }

    // ---- perf review 2026-10-08 G067 end ----
    // ---- perf review 2026-10-08 G068 begin ----
    @Nested
    class G068 {

        private final String vt = String.valueOf((char) 0x0B);
        private final String nel = String.valueOf((char) 0x85);
        private final String ls = String.valueOf((char) 0x2028);
        private final String ps = String.valueOf((char) 0x2029);
        private final String high = String.valueOf((char) 0xD83D);
        private final String low = String.valueOf((char) 0xDE00);

        private Splitter configure(final Splitter splitter, final boolean omit, final int mode, final int limit) {
            if (omit) {
                splitter.omitEmptyStrings();
            }

            if (mode == 1) {
                splitter.trimResults();
            } else if (mode == 2) {
                splitter.stripResults();
            }

            if (limit > 0) {
                splitter.limit(limit);
            }

            return splitter;
        }

        private String randomLineSource(final Random rnd, final String[] alphabet, final int maxLength) {
            final int length = rnd.nextInt(maxLength + 1);
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < length; i++) {
                sb.append(alphabet[rnd.nextInt(alphabet.length)]);
            }

            return sb.toString();
        }

        // G068-01: forLines() scans a String source by char; it must split exactly where the pattern engine for \R does.
        @Test
        public void testForLines_stringSourceMatchesPatternEngine() {
            final String[] alphabet = { "a", "b", " ", "\t", "\n", "\r", "\r\n", vt, "\f", nel, ls, ps, high, low, high + low, "\u001C",
                    String.valueOf((char) 0xA0), String.valueOf((char) 0x3000), "\r\r\n", "\n\r", "xyz" };
            final Random rnd = new Random(68);
            final List<String> sources = new ArrayList<>(Arrays.asList("", "\n", "\r", "\r\n", "\n\r", "\r\r", "\r\n\r\n", "a", "a\n", "\na", "a\rb", "a\r\nb",
                    " \n ", vt + "\f" + nel + ls + ps, "x\r", "\r\nx"));

            for (int i = 0; i < 1500; i++) {
                sources.add(randomLineSource(rnd, alphabet, i % 10 == 0 ? 50 : 10));
            }

            final int[] limits = { 0, 1, 2, 3, 5 };

            for (final String source : sources) {
                for (int omit = 0; omit < 2; omit++) {
                    for (int mode = 0; mode < 3; mode++) {
                        for (final int limit : limits) {
                            final Splitter lines = configure(Splitter.forLines(), omit == 1, mode, limit);
                            final Splitter reference = configure(Splitter.with(RegExUtil.LINE_SEPARATOR), omit == 1, mode, limit);
                            final List<String> expected = reference.split(source);
                            final String context = "source=" + Arrays.toString(source.toCharArray()) + " omit=" + omit + " mode=" + mode + " limit=" + limit;

                            assertEquals(expected, lines.split(source), context);
                            assertEquals(expected, lines.splitToStream(source).toList(), context);
                            assertArrayEquals(expected.toArray(new String[0]), lines.splitToArray(source), context);
                        }
                    }
                }
            }
        }

        // G068-01: explicit separator set of \R, incl. "\r\n" as one separator and characters that must not split.
        @Test
        public void testForLines_explicitSeparatorsAndNonSeparators() {
            final Splitter lines = Splitter.forLines();

            assertEquals(Arrays.asList("a", "b"), lines.split("a\r\nb"));
            assertEquals(Arrays.asList("a", "", "b"), lines.split("a\r\rb"));
            assertEquals(Arrays.asList("a", "", "b"), lines.split("a\n\rb"));
            assertEquals(Arrays.asList("a", "", "b"), lines.split("a\r\n\nb"));
            assertEquals(Arrays.asList("", ""), lines.split("\r"));
            assertEquals(Arrays.asList("", ""), lines.split("\r\n"));
            assertEquals(Arrays.asList("a", ""), lines.split("a\r"));
            assertEquals(Arrays.asList(""), lines.split(""));
            assertEquals(Collections.emptyList(), lines.split((CharSequence) null));

            for (final String separator : new String[] { "\n", "\r", "\r\n", vt, "\f", nel, ls, ps }) {
                assertEquals(Arrays.asList("x", "y", ""), lines.split("x" + separator + "y" + separator), Arrays.toString(separator.toCharArray()));
            }

            for (final String other : new String[] { "\t", " ", "\u001C", "\u001D", "\u001E", "\u001F", String.valueOf((char) 0x0E),
                    String.valueOf((char) 0x09), String.valueOf((char) 0x84), String.valueOf((char) 0x86), String.valueOf((char) 0x2027),
                    String.valueOf((char) 0x202A), String.valueOf((char) 0xA0), high, low }) {
                assertEquals(Arrays.asList("x" + other + "y"), lines.split("x" + other + "y"), Arrays.toString(other.toCharArray()));
            }

            assertEquals(Arrays.asList(high + low, "a" + high, low), lines.split(high + low + "\na" + high + "\r\n" + low));
            assertEquals(Arrays.asList("a", "b\r\nc\n"), Splitter.forLines().limit(2).split("a\r\nb\r\nc\n"));
            assertEquals(Arrays.asList("a", "b"), Splitter.forLines().stripResults().omitEmptyStrings().limit(2).split(" a \r\n\r\n\t b \n"));
            assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(Strings.splitToLines("a\nb\r\nc")));
        }

        // G068-01: non-String sources keep the pattern engine; the iterator protocol is unchanged.
        @Test
        public void testForLines_nonStringSourcesAndIteratorProtocol() {
            final String text = " a\r\n\r\nb " + nel + "c" + ls + ps + "\r";
            final List<String> expected = Splitter.with(RegExUtil.LINE_SEPARATOR).split(text);

            assertEquals(Arrays.asList(" a", "", "b ", "c", "", "", ""), expected);
            assertEquals(expected, Splitter.forLines().split(text));
            assertEquals(expected, Splitter.forLines().split(new StringBuilder(text)));
            assertEquals(expected, Splitter.forLines().split(java.nio.CharBuffer.wrap(text)));

            final Iterator<String> iter = Splitter.forLines().omitEmptyStrings().splitToStream("x\n\ny").iterator();

            assertTrue(iter.hasNext());
            assertTrue(iter.hasNext());
            assertEquals("x", iter.next());
            assertEquals("y", iter.next());
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::next);

            final Splitter shared = Splitter.forLines();
            final String[] first = shared.splitToArray("1\n2");
            final String[] second = shared.splitToArray("1\n2");

            assertArrayEquals(new String[] { "1", "2" }, first);
            assertNotSame(first, second);
        }

        // G068-02: splitToArray to a primitive array stores each converted token directly instead of via reflective Array.set.
        @Test
        public void testSplitToArray_primitiveComponentTypesMatchBoxedConversion() {
            final Object[][] cases = { { int[].class, int.class, "1,-2,,2147483647,-2147483648,0,007,+5" },
                    { long[].class, long.class, "1,-2,,9223372036854775807,-9223372036854775808,0" },
                    { double[].class, double.class, "1.5,-0.0,,NaN,Infinity,-Infinity,4.9e-324,1e308,0" },
                    { float[].class, float.class, "1.5,-0.0,,NaN,Infinity,-Infinity,3.4028235e38,1e-45" },
                    { boolean[].class, boolean.class, "true,false,,1,0,-1,y,Y,n,N,TRUE,False" },
                    { char[].class, char.class, "a,,Z,0,9, ," + high + "," + low + "," + String.valueOf((char) 0xE9) },
                    { byte[].class, byte.class, "1,-2,,127,-128,0" }, { short[].class, short.class, "1,-2,,32767,-32768,0" } };

            for (final Object[] testCase : cases) {
                final Class<?> arrayType = (Class<?>) testCase[0];
                final Class<?> componentType = (Class<?>) testCase[1];
                final String source = (String) testCase[2];
                final com.landawn.abacus.type.Type<Object> type = com.landawn.abacus.type.Type.of(componentType);

                for (final Splitter splitter : new Splitter[] { Splitter.with(','), Splitter.with(',').omitEmptyStrings(), Splitter.with(',').trimResults() }) {
                    final List<Object> expected = splitter.split(source, type);
                    final Object actual = splitter.splitToArray(source, arrayType);
                    final Object again = splitter.splitToArray(source, arrayType);

                    assertSame(arrayType, actual.getClass());
                    assertNotSame(actual, again);
                    assertEquals(expected.size(), java.lang.reflect.Array.getLength(actual), source);

                    for (int i = 0; i < expected.size(); i++) {
                        final Object element = java.lang.reflect.Array.get(actual, i);

                        if (element instanceof Double d) {
                            assertEquals(Double.doubleToRawLongBits((Double) expected.get(i)), Double.doubleToRawLongBits(d), source + " @" + i);
                        } else if (element instanceof Float f) {
                            assertEquals(Float.floatToRawIntBits((Float) expected.get(i)), Float.floatToRawIntBits(f), source + " @" + i);
                        } else {
                            assertEquals(expected.get(i), element, source + " @" + i);
                        }
                    }
                }

                assertEquals(0, java.lang.reflect.Array.getLength(Splitter.with(',').splitToArray(null, arrayType)));
                assertEquals(1, java.lang.reflect.Array.getLength(Splitter.with(',').splitToArray("", arrayType)));
            }

            final Random rnd = new Random(1068);
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < 3000; i++) {
                sb.append(i == 0 ? "" : ",").append(rnd.nextInt());
            }

            final int[] ints = Splitter.with(',').splitToArray(sb.toString(), int[].class);
            final String[] tokens = sb.toString().split(",");

            assertEquals(tokens.length, ints.length);

            for (int i = 0; i < tokens.length; i++) {
                assertEquals(Integer.parseInt(tokens[i]), ints[i]);
            }
        }

        // G068-02: conversion failures still surface from the element type's valueOf, with the same type and message.
        @Test
        public void testSplitToArray_primitiveConversionFailuresUnchanged() {
            final Object[][] cases = { { int[].class, int.class, "1,abc,3", "abc" }, { int[].class, int.class, "1,2147483648", "2147483648" },
                    { int[].class, int.class, "1, 2", " 2" }, { long[].class, long.class, "9223372036854775808", "9223372036854775808" },
                    { double[].class, double.class, "1,x", "x" }, { boolean[].class, boolean.class, "true,maybe", "maybe" },
                    { byte[].class, byte.class, "1,128", "128" }, { short[].class, short.class, "-32769", "-32769" },
                    { char[].class, char.class, "a,ab", "ab" } };

            for (final Object[] testCase : cases) {
                final Class<?> arrayType = (Class<?>) testCase[0];
                final com.landawn.abacus.type.Type<Object> type = com.landawn.abacus.type.Type.of((Class<?>) testCase[1]);
                final String badToken = (String) testCase[3];
                RuntimeException expected = null;

                try {
                    type.valueOf(badToken);
                } catch (final RuntimeException e) {
                    expected = e;
                }

                if (expected == null) {
                    // The token converts after all: the array call must succeed too.
                    assertNotNull(Splitter.with(',').splitToArray((String) testCase[2], arrayType));
                    continue;
                }

                final RuntimeException actual = assertThrows(RuntimeException.class, () -> Splitter.with(',').splitToArray((String) testCase[2], arrayType));

                assertSame(expected.getClass(), actual.getClass(), String.valueOf(testCase[2]));
                assertEquals(expected.getMessage(), actual.getMessage(), String.valueOf(testCase[2]));
            }

            assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("1", int.class));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("1", (Class<int[]>) null));
        }
    }

    // ---- perf review 2026-10-08 G068 end ----
    // ---- perf review 2026-10-08 G069 begin ----
    @Nested
    class G069 {

        /**
         * Verbatim copy of the pristine ASCII algorithm behind Strings.toSnakeCase / toScreamingSnakeCase / toKebabCase
         * (Strings.toDelimitedCaseAscii), used as the reference for ASCII input.
         */
        private String referenceDelimitedCaseAscii(final String str, final char separator, final boolean toUpperCase) {
            if (str == null || str.isEmpty()) {
                return str;
            }

            final int len = str.length();
            final StringBuilder result = new StringBuilder(len + 16);

            for (int i = 0; i < len;) {
                final char ch = str.charAt(i);

                if (ch == '_' || ch == '-' || Character.isWhitespace(ch)) {
                    final boolean atStart = result.length() == 0;

                    do {
                        i++;
                    } while (i < len && (str.charAt(i) == '_' || str.charAt(i) == '-' || Character.isWhitespace(str.charAt(i))));

                    if (!atStart && i < len) {
                        result.append(separator);
                    }

                    continue;
                }

                if (isUpper(ch) && i > 0 && (isLower(str.charAt(i - 1)) || (i + 1 < len && isLower(str.charAt(i + 1)))) && result.length() > 0
                        && result.charAt(result.length() - 1) != separator) {
                    result.append(separator);
                }

                result.append(toUpperCase ? isLower(ch) ? (char) (ch - ('a' - 'A')) : ch : isUpper(ch) ? (char) (ch + ('a' - 'A')) : ch);
                i++;
            }

            return result.toString();
        }

        private boolean isLower(final char ch) {
            return ch >= 'a' && ch <= 'z';
        }

        private boolean isUpper(final char ch) {
            return ch >= 'A' && ch <= 'Z';
        }

        private void assertDelimitedCasesMatchReference(final String input) {
            assertEquals(referenceDelimitedCaseAscii(input, '_', false), Strings.toSnakeCase(input), input);
            assertEquals(referenceDelimitedCaseAscii(input, '_', true), Strings.toScreamingSnakeCase(input), input);
            assertEquals(referenceDelimitedCaseAscii(input, '-', false), Strings.toKebabCase(input), input);
        }

        // G069-01: ASCII input that is already in the target form converts to an equal string.
        @Test
        void testToDelimitedCase_alreadyInTargetForm() {
            for (final String snake : new String[] { "id", "first_name", "a_b_c", "x1_y2", "a.b$c", "1", "a1_2b", "v2_api_key", "name" }) {
                assertEquals(snake, Strings.toSnakeCase(snake));
                assertDelimitedCasesMatchReference(snake);
            }

            for (final String screaming : new String[] { "ID", "FIRST_NAME", "HTTP2_URL", "A_B_C", "X.Y$Z", "9", "MAX_VALUE" }) {
                assertEquals(screaming, Strings.toScreamingSnakeCase(screaming));
                assertDelimitedCasesMatchReference(screaming);
            }

            for (final String kebab : new String[] { "id", "first-name", "a-b-c", "x1-y2", "content-type" }) {
                assertEquals(kebab, Strings.toKebabCase(kebab));
                assertDelimitedCasesMatchReference(kebab);
            }

            // already-converted results stay stable (idempotence for ASCII input)
            for (final String camel : new String[] { "firstName", "HTTPStatusCode", "userID", "base64URL", "IOError", "a-b c_d" }) {
                final String snake = Strings.toSnakeCase(camel);
                final String screaming = Strings.toScreamingSnakeCase(camel);
                final String kebab = Strings.toKebabCase(camel);
                assertEquals(snake, Strings.toSnakeCase(snake));
                assertEquals(screaming, Strings.toScreamingSnakeCase(screaming));
                assertEquals(kebab, Strings.toKebabCase(kebab));
            }
        }

        // G069-01: input one step away from the target form is still converted.
        @Test
        void testToDelimitedCase_nearTargetFormStillConverted() {
            assertEquals("a", Strings.toSnakeCase("_a"));
            assertEquals("a", Strings.toSnakeCase("a_"));
            assertEquals("", Strings.toSnakeCase("_"));
            assertEquals("a_b", Strings.toSnakeCase("a__b"));
            assertEquals("a_b", Strings.toSnakeCase("a-b"));
            assertEquals("a_b", Strings.toSnakeCase("a b"));
            assertEquals("a_b", Strings.toSnakeCase("a\tb"));
            assertEquals("a_b", Strings.toSnakeCase("a\u001Fb"));
            assertEquals("a_b", Strings.toSnakeCase("a_-b"));
            assertEquals("a_b", Strings.toSnakeCase("aB"));
            assertEquals("ab", Strings.toSnakeCase("Ab"));
            assertEquals("first_name", Strings.toSnakeCase("first_Name"));
            assertEquals("A", Strings.toScreamingSnakeCase("a"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("aB"));
            assertEquals("A_BC", Strings.toScreamingSnakeCase("ABc"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("A__B"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("A-B"));
            assertEquals("A_B", Strings.toScreamingSnakeCase("_A_B_"));
            assertEquals("first-name", Strings.toKebabCase("first_name"));
            assertEquals("a", Strings.toKebabCase("-a-"));
            assertEquals("a-b", Strings.toKebabCase("a--b"));
            assertEquals("a-b", Strings.toKebabCase("aB"));

            for (final String input : new String[] { "_a", "a_", "_", "-", " ", "a__b", "a-b", "a b", "a\tb", "a\u001Fb", "a_-b", "aB", "Ab", "first_Name", "a",
                    "A", "ABc", "A__B", "A-B", "_A_B_", "first_name", "-a-", "a--b", "a_ b", "a _b", "A1b", "1aB", "a.B" }) {
                assertDelimitedCasesMatchReference(input);
            }
        }

        // G069-01: every ASCII string up to length 6 over a small alphabet converts exactly like the reference.
        @Test
        void testToDelimitedCase_exhaustiveShortAsciiMatchesReference() {
            final char[] alphabet = { 'a', 'Z', '_', '-', ' ', '1', '\u001F' };
            final char[] buffer = new char[6];

            for (int len = 1; len <= 6; len++) {
                final int[] digits = new int[len];

                while (true) {
                    for (int i = 0; i < len; i++) {
                        buffer[i] = alphabet[digits[i]];
                    }

                    assertDelimitedCasesMatchReference(new String(buffer, 0, len));

                    int position = len - 1;

                    while (position >= 0 && ++digits[position] == alphabet.length) {
                        digits[position] = 0;
                        position--;
                    }

                    if (position < 0) {
                        break;
                    }
                }
            }
        }

        // G069-01: random ASCII strings and their already-converted forms (the fast path) match the reference; splitChar overloads too.
        @Test
        void testToDelimitedCase_randomAndReconvertedAsciiMatchesReference() {
            final String pool = "abcxyzABCXYZ0189__--  \t\n\r.$#\u001C";
            final Random random = new Random(20261008L);

            for (int n = 0; n < 20_000; n++) {
                final int len = random.nextInt(31);
                final StringBuilder sb = new StringBuilder(len);

                for (int i = 0; i < len; i++) {
                    final int mode = random.nextInt(10);
                    sb.append(mode < 4 ? (char) ('a' + random.nextInt(26))
                            : mode < 6 ? (char) ('A' + random.nextInt(26)) : pool.charAt(random.nextInt(pool.length())));
                }

                final String input = sb.toString();
                assertDelimitedCasesMatchReference(input);
                assertDelimitedCasesMatchReference(Strings.toSnakeCase(input));
                assertDelimitedCasesMatchReference(Strings.toScreamingSnakeCase(input));
                assertDelimitedCasesMatchReference(Strings.toKebabCase(input));

                final String dotted = input.replace('.', '-');
                assertEquals(referenceDelimitedCaseAscii(dotted, '_', false), Strings.toSnakeCase(input, '.'), input);
                assertEquals(referenceDelimitedCaseAscii(dotted, '_', true), Strings.toScreamingSnakeCase(input, '.'), input);
                assertEquals(referenceDelimitedCaseAscii(dotted, '-', false), Strings.toKebabCase(input, '.'), input);
            }
        }

        // G069-01: null/empty and non-ASCII input (anywhere in the string) keep their paths.
        @Test
        void testToDelimitedCase_nullEmptyAndNonAscii() {
            assertNull(Strings.toSnakeCase(null));
            assertNull(Strings.toScreamingSnakeCase(null));
            assertNull(Strings.toKebabCase(null));
            assertEquals("", Strings.toSnakeCase(""));
            assertEquals("", Strings.toScreamingSnakeCase(""));
            assertEquals("", Strings.toKebabCase(""));

            assertEquals("\u00E9a", Strings.toSnakeCase("\u00C9a"));
            assertEquals("a_\u00E9", Strings.toSnakeCase("a\u00C9"));
            assertEquals("a_\u00E9", Strings.toSnakeCase("a_\u00C9"));
            assertEquals("a_\u00E9", Strings.toSnakeCase("a_\u00E9"));
            assertEquals("A_\u00C9", Strings.toScreamingSnakeCase("a_\u00E9"));
            assertEquals("a-b\u00E9", Strings.toKebabCase("aB\u00E9"));
            assertEquals("a_b", Strings.toSnakeCase("a_\u2003b"));
            assertEquals("first_name", Strings.toSnakeCase("first\u2003name"));
            assertEquals("first_name_\u00E9", Strings.toSnakeCase("first_name_\u00E9"));
        }
    }

    // ---- perf review 2026-10-08 G069 end ----
    // ---- perf review 2026-10-08 G070 begin ----
    @Nested
    class G070 {
        // Alphabet for the split differential tests: delimiters, ASCII space, tab, a char <= ' ' that is not Java whitespace
        // (U+0001), a char <= ' ' that is Java whitespace (U+001F), NBSP (> ' ', not trimmed) and EM SPACE (Java whitespace,
        // > ' ', not trimmed; it also makes the string UTF-16).
        private final char[] splitAlphabet = { 'a', 'b', 'c', ',', ',', ':', ':', ' ', ' ', '\t', (char) 0x0001, (char) 0x001F, (char) 0x00A0, (char) 0x2003 };

        private String randomString(final Random random, final char[] alphabet, final int length) {
            final char[] chars = new char[length];

            for (int i = 0; i < length; i++) {
                chars[i] = alphabet[random.nextInt(alphabet.length)];
            }

            return new String(chars);
        }

        private String[] trimAll(final String[] tokens) {
            final String[] result = new String[tokens.length];

            for (int i = 0; i < tokens.length; i++) {
                result[i] = tokens[i].trim();
            }

            return result;
        }

        private String describe(final String str) {
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < str.length(); i++) {
                sb.append(String.format("%04x ", (int) str.charAt(i)));
            }

            return sb.toString();
        }

        private boolean containsAllReference(final String str, final char[] valuesToFind) {
            for (final char ch : valuesToFind) {
                if (str.indexOf(ch) < 0) {
                    return false;
                }
            }

            return true;
        }

        // G070-02: split(..., trim = true) trims each selected token without the intermediate untrimmed substring; the result
        // must equal the untrimmed split with String.trim() applied to every element (trimming happens after token selection).
        @Test
        public void testSplitTrim_matchesTrimmedUntrimmedSplitForAllWorkers() {
            final Random random = new Random(20261008L);
            final int[] maxValues = { 1, 2, 3, 4, Integer.MAX_VALUE };

            for (int iteration = 0; iteration < 4000; iteration++) {
                final String str = randomString(random, splitAlphabet, random.nextInt(24));
                final int max = maxValues[random.nextInt(maxValues.length)];
                final String context = "str=" + describe(str) + " max=" + max;

                // char delimiter worker
                assertArrayEquals(trimAll(Strings.split(str, ',')), Strings.split(str, ',', true), context);
                assertArrayEquals(trimAll(Strings.split(str, ',', max)), Strings.split(str, ',', max, true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, ',')), Strings.splitPreserveAllTokens(str, ',', true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, ',', max)), Strings.splitPreserveAllTokens(str, ',', max, true), context);
                // a single-char String delimiter routes to the char worker
                assertArrayEquals(trimAll(Strings.split(str, ",")), Strings.split(str, ",", true), context);
                // multi-char String delimiter worker
                assertArrayEquals(trimAll(Strings.split(str, "::")), Strings.split(str, "::", true), context);
                assertArrayEquals(trimAll(Strings.split(str, "::", max)), Strings.split(str, "::", max, true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, "::")), Strings.splitPreserveAllTokens(str, "::", true), context);
                assertArrayEquals(trimAll(Strings.splitPreserveAllTokens(str, "::", max)), Strings.splitPreserveAllTokens(str, "::", max, true), context);
                assertArrayEquals(trimAll(Strings.split(str, ", ", max)), Strings.split(str, ", ", max, true), context);
                // whitespace worker (tokens can still carry U+0001, which trim() removes)
                assertArrayEquals(trimAll(Strings.splitOnWhitespace(str)), Strings.splitOnWhitespace(str, true), context);
                assertArrayEquals(trimAll(Strings.splitOnWhitespace(str, max)), Strings.splitOnWhitespace(str, max, true), context);
                assertArrayEquals(trimAll(Strings.splitOnWhitespacePreserveAllTokens(str)), Strings.splitOnWhitespacePreserveAllTokens(str, true), context);
                assertArrayEquals(trimAll(Strings.splitOnWhitespacePreserveAllTokens(str, max)), Strings.splitOnWhitespacePreserveAllTokens(str, max, true),
                        context);
            }
        }

        // G070-02: fixed edge cases of the trim option: empty-token omission before trimming, whitespace-only tokens, the max
        // remainder, chars <= ' ' that are not Java whitespace, null/empty input, and the whole-string token.
        @Test
        public void testSplitTrim_edgeCases() {
            final String c1 = String.valueOf((char) 0x0001);
            final String c0 = String.valueOf((char) 0x0000);
            final String c2 = String.valueOf((char) 0x0002);
            final String c1f = String.valueOf((char) 0x001F);
            final String nbsp = String.valueOf((char) 0x00A0);
            final String cjk = String.valueOf((char) 0x4E00);

            assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', true));
            assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ',', true));
            assertArrayEquals(new String[] { "a", "b : c" }, Strings.split(" a : b : c ", ':', 2, true));
            assertArrayEquals(new String[] { "a", "b :: c" }, Strings.split(" a :: b :: c ", "::", 2, true));
            assertArrayEquals(new String[] { "a :: b" }, Strings.split(" a :: b ", "::", 1, true));
            assertArrayEquals(new String[] { "", "a", "", "" }, Strings.splitPreserveAllTokens(" , a ,  , ", ',', true));
            assertArrayEquals(new String[] { "", "x", "" }, Strings.splitPreserveAllTokens("::x::", "::", true));
            assertArrayEquals(new String[] { "x", "y" }, Strings.split(c1 + "x" + c1 + "," + c0 + "y" + c1f, ',', true));
            assertArrayEquals(new String[] { nbsp + "x" + nbsp }, Strings.split(" " + nbsp + "x" + nbsp + " ", ',', true));
            assertArrayEquals(new String[] { "x" + c1 + "y" }, Strings.splitOnWhitespace(" " + c1 + "x" + c1 + "y" + c2 + " ", true));
            assertArrayEquals(new String[] { cjk, "b" }, Strings.split(" " + cjk + " , b", ',', true));
            assertArrayEquals(new String[0], Strings.split(null, ',', true));
            assertArrayEquals(new String[0], Strings.split("", ',', true));
            assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ',', true));
            assertArrayEquals(new String[] { "" }, Strings.split("   ", ',', true));

            // change detector, not a contract: a whole-string token with nothing to trim is the input instance itself
            // (substring(0, length) returns this), as before the change
            final String whole = "abc";
            assertSame(whole, Strings.split(whole, ',', true)[0]);
            assertSame(whole, Strings.split(whole, "::", true)[0]);
            assertSame(whole, Strings.splitOnWhitespace(whole, true)[0]);
        }

        // G070-01: containsAll(String, char...) uses String.indexOf per candidate below 16 candidates (was: a membership set of
        // the whole string from 8 candidates on). Differential check against the indexOf reference on both sides of the
        // candidate-count (8, 16) and length (n * k >= 4096) thresholds, Latin-1 and UTF-16 strings, surrogates, duplicates.
        @Test
        public void testContainsAllChars_matchesIndexOfReferenceAroundThresholds() {
            final Random random = new Random(8L);
            final char[] latin = "abcdefghijklmnopqrstuvwxyz0123456789,;: \t".toCharArray();
            final char[] wide = { 'a', 'b', 'c', 'x', 'y', 'z', (char) 0x00E9, (char) 0x4E00, (char) 0x4E01, (char) 0xD83D, (char) 0xDE00, (char) 0xDC00,
                    (char) 0xFFFF };
            final int[] lengths = { 0, 1, 7, 255, 256, 257, 273, 274, 511, 512, 513, 600, 2000 };

            for (int iteration = 0; iteration < 3000; iteration++) {
                final char[] alphabet = random.nextBoolean() ? latin : wide;
                final int length = lengths[random.nextInt(lengths.length)];
                final String str = randomString(random, alphabet, length);
                final int count = random.nextInt(41);
                final char[] candidates = new char[count];

                for (int i = 0; i < count; i++) {
                    final int pick = random.nextInt(10);

                    if (pick < 6 && length > 0) {
                        candidates[i] = str.charAt(random.nextInt(length)); // present
                    } else if (pick < 8) {
                        candidates[i] = alphabet[random.nextInt(alphabet.length)]; // maybe present
                    } else {
                        candidates[i] = (char) random.nextInt(0x10000); // most likely absent
                    }
                }

                final boolean expected = count == 0 || (length > 0 && containsAllReference(str, candidates));
                assertEquals(expected, Strings.containsAll(str, candidates), "length=" + length + " count=" + count);
            }

            assertTrue(Strings.containsAll(null, new char[0]));
            assertTrue(Strings.containsAll("abc", (char[]) null));
            assertFalse(Strings.containsAll(null, 'a'));
            assertFalse(Strings.containsAll("", 'a'));

            final String longStr = Strings.repeat("abcdefghijklmnop", 64); // 1024 chars, n * k >= 4096 for k >= 4
            assertTrue(Strings.containsAll(longStr, "abcdefgh".toCharArray()));
            assertTrue(Strings.containsAll(longStr, "abcdefghijklmno".toCharArray()));
            assertTrue(Strings.containsAll(longStr, "abcdefghijklmnop".toCharArray()));
            assertFalse(Strings.containsAll(longStr, "abcdefgz".toCharArray()));
            assertFalse(Strings.containsAll(longStr, "zabcdefghijklmn".toCharArray()));
            assertFalse(Strings.containsAll(longStr, "abcdefghijklmnoz".toCharArray()));
        }
    }

    // ---- perf review 2026-10-08 G070 end ----
    // ---- perf review 2026-10-08 G071 begin ----
    @Nested
    class G071 {

        /** Scalar reference: the code-unit-by-code-unit definition of containsNone(String, char...). */
        private boolean referenceContainsNone(final String str, final char... valuesToFind) {
            if (str == null || str.isEmpty() || valuesToFind == null || valuesToFind.length == 0) {
                return true;
            }

            for (int i = 0; i < str.length(); i++) {
                for (final char c : valuesToFind) {
                    if (str.charAt(i) == c) {
                        return false;
                    }
                }
            }

            return true;
        }

        private String filler(final int length, final Random random, final char[] alphabet) {
            final char[] chars = new char[length];

            for (int i = 0; i < length; i++) {
                chars[i] = alphabet[random.nextInt(alphabet.length)];
            }

            return new String(chars);
        }

        private String withCharAt(final String str, final int index, final char ch) {
            final char[] chars = str.toCharArray();
            chars[index] = ch;
            return new String(chars);
        }

        private void assertMatchesReference(final String str, final char[] valuesToFind) {
            final boolean expected = referenceContainsNone(str, valuesToFind);
            assertEquals(expected, Strings.containsNone(str, valuesToFind),
                    () -> "containsNone length=" + str.length() + " values=" + Arrays.toString(valuesToFind));
            assertEquals(!expected, Strings.containsAny(str, valuesToFind),
                    () -> "containsAny length=" + str.length() + " values=" + Arrays.toString(valuesToFind));
        }

        // G071-01: windowed containsNone(String, char...) equals the code-unit reference for every length, hit position and candidate order.
        @Test
        public void testContainsNoneChar_windowedSearchMatchesReference() {
            final Random random = new Random(20261008L);
            final char[] latin = "abcdefghijklmnopqrstuvwxyz 0123456789".toCharArray();
            final char[] utf16 = { 'a', 'b', '\u4e2d', '\u6587', '\u00e9', '\uD83D', '\uDE00', ' ' };
            final char[] candidatePool = { ',', '"', '\r', '\n', 'Q', '\u4e8c', '\uD83D', '\uDE00', '\u0100', '\uffff', '\u0000' };
            final int[] lengths = new int[160];

            for (int i = 0; i < 141; i++) {
                lengths[i] = i;
            }

            final int[] longLengths = { 191, 192, 193, 255, 256, 257, 511, 1000, 4097, 70_000 };
            System.arraycopy(longLengths, 0, lengths, 141, longLengths.length);
            final int lengthCount = 141 + longLengths.length;

            for (int li = 0; li < lengthCount; li++) {
                final int length = lengths[li];

                for (int round = 0; round < 6; round++) {
                    final String base = filler(length, random, round % 2 == 0 ? latin : utf16);
                    final int valueCount = 1 + random.nextInt(7);
                    final char[] valuesToFind = new char[valueCount];

                    for (int k = 0; k < valueCount; k++) {
                        valuesToFind[k] = candidatePool[random.nextInt(candidatePool.length)];
                    }

                    // no planted hit (the filler may still contain a surrogate candidate)
                    assertMatchesReference(base, valuesToFind);

                    if (length == 0) {
                        continue;
                    }

                    final int[] positions = { 0, 1, 31, 32, 33, 63, 64, 65, 95, 96, 127, 128, 129, length - 1, random.nextInt(length) };

                    for (final int position : positions) {
                        if (position < length) {
                            // plant the first, a middle and the last candidate
                            assertMatchesReference(withCharAt(base, position, valuesToFind[0]), valuesToFind);
                            assertMatchesReference(withCharAt(base, position, valuesToFind[valueCount / 2]), valuesToFind);
                            assertMatchesReference(withCharAt(base, position, valuesToFind[valueCount - 1]), valuesToFind);
                        }
                    }
                }
            }
        }

        // G071-01: the window doubling stops at 65,536 chars; a 300,000-char string runs through capped windows.
        @Test
        public void testContainsNoneChar_cappedWindowsOnVeryLongString() {
            final String base = Strings.repeat("abcdefgh", 37_500);
            final char[] csv = { ',', '"', '\r', '\n' };

            assertEquals(300_000, base.length());
            assertTrue(Strings.containsNone(base, csv));
            assertFalse(Strings.containsAny(base, csv));

            for (final int position : new int[] { 0, 31, 32, 63, 64, 65_535, 65_536, 131_071, 131_072, 196_607, 196_608, 250_000, 299_999 }) {
                for (final char ch : csv) {
                    final String str = withCharAt(base, position, ch);
                    assertFalse(Strings.containsNone(str, csv), () -> "position " + position + " char " + (int) ch);
                    assertTrue(Strings.containsAny(str, csv), () -> "position " + position + " char " + (int) ch);
                }
            }
        }

        // G071-01: scalar-head boundary (64/65 chars), candidates above U+00FF, surrogate code units, duplicates, >= 8 candidates.
        @Test
        public void testContainsNoneChar_boundariesAndCodeUnits() {
            final String s64 = Strings.repeat('x', 64);
            final String s65 = Strings.repeat('x', 65);

            assertTrue(Strings.containsNone(s64, 'a', 'b'));
            assertTrue(Strings.containsNone(s65, 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s64, 63, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s65, 64, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s65, 32, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(withCharAt(s65, 31, 'b'), 'a', 'b'));
            assertFalse(Strings.containsNone(s65, 'a', 'x'));
            assertFalse(Strings.containsNone(s65, 'x', 'x', 'x'));

            // Latin-1 string, candidates that cannot occur in it
            final String latin = Strings.repeat("caf\u00e9 ", 50);
            assertTrue(Strings.containsNone(latin, '\u0100', '\u4e2d', '\uD83D', '\uffff'));
            assertFalse(Strings.containsNone(latin, '\u0100', '\u00e9'));

            // UTF-16 string: lone high surrogate, then a full pair, far beyond the scalar head
            final String utf16Base = Strings.repeat("\u4e2d\u6587", 100);
            assertTrue(Strings.containsNone(utf16Base, '\uD83D', '\uDE00', ','));
            assertFalse(Strings.containsNone(utf16Base + "\uD83D", ',', '\uD83D'));
            assertFalse(Strings.containsNone(utf16Base + "\uD83D\uDE00", ',', '\uDE00'));
            assertFalse(Strings.containsNone(utf16Base, ',', '\u6587'));

            // >= 8 candidates with length * count < 4096 (scalar region) on strings longer than 64 chars
            final char[] ten = "0123456789".toCharArray();
            final String letters = Strings.repeat("abcdefghij", 40);
            assertTrue(Strings.containsNone(letters, ten));
            assertFalse(Strings.containsNone(withCharAt(letters, 399, '9'), ten));
            assertFalse(Strings.containsNone(withCharAt(letters, 40, '9'), ten));
            assertFalse(Strings.containsNone(withCharAt(letters, 200, '0'), ten));

            // >= 8 candidates with length * count >= 4096 (membership path, unchanged)
            final String longLetters = Strings.repeat("abcdefghij", 60);
            assertTrue(Strings.containsNone(longLetters, ten));
            assertFalse(Strings.containsNone(withCharAt(longLetters, 599, '5'), ten));
        }
    }

    // ---- perf review 2026-10-08 G071 end ----
    // ---- perf review 2026-10-08 G072 begin ----
    @Nested
    class G072 {
        private final Strings.DisplayWidthPolicy[] policies = { Strings.DisplayWidthPolicy.DEFAULT, Strings.DisplayWidthPolicy.CJK,
                new Strings.DisplayWidthPolicy(1, 1), new Strings.DisplayWidthPolicy(2, 1) };

        private java.lang.reflect.Method nextBoundaryMethod;
        private java.lang.reflect.Method clusterWidthMethod;

        private String text(final int... codePoints) {
            final StringBuilder sb = new StringBuilder();

            for (final int codePoint : codePoints) {
                sb.appendCodePoint(codePoint);
            }

            return sb.toString();
        }

        private String hex(final int codePoint) {
            return "U+" + Integer.toHexString(codePoint).toUpperCase();
        }

        private int nextBoundary(final String str, final int clusterStart) throws ReflectiveOperationException {
            if (nextBoundaryMethod == null) {
                nextBoundaryMethod = Strings.class.getDeclaredMethod("nextGraphemeClusterBoundary", String.class, int.class);
                nextBoundaryMethod.setAccessible(true);
            }

            return (Integer) nextBoundaryMethod.invoke(null, str, clusterStart);
        }

        private int clusterWidth(final String str, final int clusterStart, final int clusterEnd, final Strings.DisplayWidthPolicy policy)
                throws ReflectiveOperationException {
            if (clusterWidthMethod == null) {
                clusterWidthMethod = Strings.class.getDeclaredMethod("graphemeClusterDisplayWidth", String.class, int.class, int.class,
                        Strings.DisplayWidthPolicy.class);
                clusterWidthMethod.setAccessible(true);
            }

            return (Integer) clusterWidthMethod.invoke(null, str, clusterStart, clusterEnd, policy);
        }

        /**
         * Width from the general grapheme segmentation alone: every cluster boundary and cluster width comes from the
         * private helpers that displayWidth uses for clusters it cannot measure without the property tables.
         */
        private int fullySegmentedDisplayWidth(final String str, final Strings.DisplayWidthPolicy policy) throws ReflectiveOperationException {
            long width = 0;

            for (int clusterStart = 0; clusterStart < str.length();) {
                final int clusterEnd = nextBoundary(str, clusterStart);
                width += clusterWidth(str, clusterStart, clusterEnd, policy);
                clusterStart = clusterEnd;
            }

            return (int) width;
        }

        private List<Integer> standaloneClusterCodePoints() {
            final List<Integer> result = new ArrayList<>();

            for (int codePoint = 0; codePoint <= 0x7F; codePoint++) {
                if (codePoint != '\r') {
                    result.add(codePoint);
                }
            }

            final int[][] ranges = { { 0x3041, 0x3096 }, { 0x309B, 0x30FF }, { 0x3400, 0x9FFF }, { 0xAC00, 0xD7A3 }, { 0xFF01, 0xFF5E } };

            for (final int[] range : ranges) {
                for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                    result.add(codePoint);
                }
            }

            return result;
        }

        // G072-01: ASCII-but-CR and common wide characters are one-character clusters before each other or the end.
        @Test
        public void testDisplayWidth_standaloneClusterCharsNeverJoinTheirNeighbour() throws ReflectiveOperationException {
            final List<Integer> standalone = standaloneClusterCodePoints();
            final int[] wideFollowers = { 0x3041, 0x3096, 0x309B, 0x30FF, 0x3400, 0x4E2D, 0x9FFF, 0xAC00, 0xAC01, 0xD7A3, 0xFF01, 0xFF5E };

            for (final int codePoint : standalone) {
                final byte graphemeBreak = Unicode17Data.graphemeBreakProperty(codePoint);

                assertTrue(graphemeBreak == Unicode17Data.GCB_OTHER || graphemeBreak == Unicode17Data.GCB_CONTROL || graphemeBreak == Unicode17Data.GCB_LF
                        || graphemeBreak == Unicode17Data.GCB_LV || graphemeBreak == Unicode17Data.GCB_LVT, hex(codePoint));
                assertEquals(Unicode17Data.INCB_NONE, Unicode17Data.indicConjunctBreakProperty(codePoint), hex(codePoint));

                final String single = text(codePoint);
                assertEquals(1, nextBoundary(single, 0), hex(codePoint));

                for (final Strings.DisplayWidthPolicy policy : policies) {
                    assertEquals(Strings.codePointDisplayWidth(codePoint, policy), clusterWidth(single, 0, 1, policy), hex(codePoint) + " " + policy);
                }

                for (final int follower : wideFollowers) {
                    assertEquals(1, nextBoundary(text(codePoint, follower), 0), hex(codePoint) + " " + hex(follower));
                }

                if (codePoint <= 0x7F) {
                    for (int follower = 0; follower <= 0x7F; follower++) {
                        assertEquals(1, nextBoundary(text(codePoint, follower), 0), hex(codePoint) + " " + hex(follower));
                    }
                }
            }

            // CR is excluded on purpose: CR LF is one cluster (UAX #29 GB3).
            assertEquals(2, nextBoundary("\r\n", 0));
        }

        // G072-01: text after a segmentation trigger measures exactly what the general segmentation measures.
        @Test
        public void testDisplayWidth_textAfterSegmentationTriggerMatchesFullSegmentation() throws ReflectiveOperationException {
            final int[] triggers = { 0x1F600, 0x0301, 0x00A9, 0x2122, 0x094D, 0x1100, 0x0600, 0x1F1FA, 0x200D, 0xFE0F, 0xD800, 0x0E33 };
            final int[] standalone = { 'a', 'Z', '0', ' ', '.', '~', '\n', '\t', 0x01, 0x1F, 0x7F, 0x3041, 0x3096, 0x309B, 0x30FF, 0x3400, 0x4E2D, 0x9FFF,
                    0xAC00, 0xAC01, 0xD7A3, 0xFF01, 0xFF5E };
            final int[] others = { '\r', 0x0300, 0x0301, 0x200D, 0x200C, 0xFE0F, 0xFE0E, 0x20E3, 0x1F600, 0x1F469, 0x1F4BB, 0x1F3FB, 0x1100, 0x1161, 0x11A8,
                    0xD7B0, 0xD7CB, 0x0915, 0x094D, 0x0937, 0x093E, 0x0E01, 0x0E31, 0x0E33, 0x0600, 0x0D4E, 0x1F1FA, 0x1F1F8, 0xFF76, 0xFF9E, 0xFF9F, 0x3099,
                    0x3040, 0x3097, 0xD7A4, 0xFF5F, 0x00A1, 0x00E9, 0x00AD, 0x2028, 0x2764, 0x231A, 0x102B, 0x1B35, 0x0BD7, 0x302E, 0xD800, 0xDC00, 0x20000,
                    0xE0100, 0x11A84 };
            final Random random = new Random(20261008L);

            for (int iteration = 0; iteration < 3_000; iteration++) {
                final StringBuilder value = new StringBuilder();
                value.appendCodePoint(triggers[random.nextInt(triggers.length)]);
                final int length = random.nextInt(48);

                for (int i = 0; i < length; i++) {
                    final int[] alphabet = random.nextInt(10) < 7 ? standalone : others;
                    value.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
                }

                final String str = value.toString();

                for (final Strings.DisplayWidthPolicy policy : policies) {
                    assertEquals(fullySegmentedDisplayWidth(str, policy), Strings.displayWidth(str, policy), "iteration=" + iteration + ", policy=" + policy);
                }
            }
        }

        // G072-01: explicit widths after a trigger (computed on the code before the shortcut), incl. joiners and CR LF.
        @Test
        public void testDisplayWidth_textAfterSegmentationTrigger_examples() {
            final Strings.DisplayWidthPolicy narrowEmoji = new Strings.DisplayWidthPolicy(1, 1);

            final String[] values = { text(0xA9) + " 2026 Acme", text(0x1F600) + "abc" + text(0x4E2D, 0x6587), "e" + text(0x301) + "xyz",
                    text(0xA9) + "\r\nab\r\n", text(0x2122) + "a" + text(0x301) + "b", text(0x1F600) + "a" + text(0x200D) + text(0x1F4BB),
                    text(0x1F600) + text(0xAC00, 0x11A8), text(0x1F600) + text(0xFF76, 0xFF9E), text(0x1F600) + "1" + text(0xFE0F, 0x20E3),
                    text(0x0915, 0x094D) + "x", text(0x1F1FA) + "a" + text(0x1F1FA, 0x1F1F8), text(0x0600) + "a", text(0x1F600) + "ab" + text(0x0600) + "cd",
                    text(0x1F600) + text(0xA1) + "a", text(0x1F600) + "a" + text(0x93E) + "b", text(0x1F600) + text(0x3041, 0x3099) + "z",
                    text(0x1F600, '\t', 0x01, 'x'), text(0x301, 0xFF01, 0xFF5E, 0x3400, 0x9FFF, 0xD7A3, '\r') };
            final int[] expectedDefault = { 11, 9, 4, 3, 3, 5, 4, 4, 4, 2, 5, 1, 6, 4, 5, 5, 3, 10 };
            final int[] expectedCjk = { 11, 9, 4, 3, 4, 5, 4, 4, 4, 2, 5, 1, 6, 5, 5, 5, 3, 10 };
            final int[] expectedNarrowEmoji = { 11, 8, 4, 3, 3, 3, 3, 3, 2, 2, 3, 1, 5, 3, 4, 4, 2, 10 };

            for (int i = 0; i < values.length; i++) {
                assertEquals(expectedDefault[i], Strings.displayWidth(values[i]), "case " + i);
                assertEquals(expectedCjk[i], Strings.displayWidth(values[i], Strings.DisplayWidthPolicy.CJK), "case " + i);
                assertEquals(expectedNarrowEmoji[i], Strings.displayWidth(values[i], narrowEmoji), "case " + i);

                final int target = expectedDefault[i] + 3;
                assertEquals(values[i] + "   ", Strings.padEndToDisplayWidth(values[i], target), "case " + i);
                assertTrue(Strings.displayWidth(Strings.padStartToDisplayWidth(values[i], target)) >= target, "case " + i);
            }

            // A long ASCII run after the trigger, and the same run before it.
            final String run = "abcdefghij".repeat(100);
            assertEquals(1002, Strings.displayWidth(text(0x1F600) + run));
            assertEquals(1002, Strings.displayWidth(run + text(0x1F600)));
            assertEquals(1001, Strings.displayWidth(text(0x1F600) + run, narrowEmoji));
        }
    }

    // ---- perf review 2026-10-08 G072 end ----
    // ---- perf review 2026-10-08 G075 begin ----
    @Nested
    class G075 {
        private final java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;

        // Records every call made on it; throws an IOException on call number failAtCall (never if it is negative).
        private final class RecordingAppendable implements Appendable {
            private final List<String> calls = new ArrayList<>();
            private final int failAtCall;

            RecordingAppendable(final int failAtCall) {
                this.failAtCall = failAtCall;
            }

            private void record(final String call) throws java.io.IOException {
                if (calls.size() + 1 == failAtCall) {
                    throw new java.io.IOException("call " + failAtCall);
                }

                calls.add(call);
            }

            @Override
            public Appendable append(final CharSequence csq) throws java.io.IOException {
                record("seq:" + csq);
                return this;
            }

            @Override
            public Appendable append(final CharSequence csq, final int start, final int end) throws java.io.IOException {
                record("range:" + csq.subSequence(start, end));
                return this;
            }

            @Override
            public Appendable append(final char c) throws java.io.IOException {
                record("char:" + c);
                return this;
            }
        }

        private String percentEscaped(final int... bytes) {
            final StringBuilder sb = new StringBuilder();

            for (final int b : bytes) {
                sb.append('%')
                        .append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)))
                        .append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
            }

            return sb.toString();
        }

        private byte[] toBytes(final int... values) {
            final byte[] bytes = new byte[values.length];

            for (int i = 0; i < values.length; i++) {
                bytes[i] = (byte) values[i];
            }

            return bytes;
        }

        // The reporting JDK UTF-8 decoder, the reference for strict decoding of one escape run: its exception, or null if it accepts the bytes.
        private java.nio.charset.CharacterCodingException jdkStrictFailure(final byte[] bytes) {
            try {
                utf8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(bytes));
                return null;
            } catch (final java.nio.charset.CharacterCodingException e) {
                return e;
            }
        }

        // One escape run decoded strictly and leniently must equal what the JDK decoder / new String(bytes, UTF_8) give for it.
        private void assertRunDecodedLikeJdk(final int... values) {
            final byte[] bytes = toBytes(values);
            final String query = "k=" + percentEscaped(values);
            final java.nio.charset.CharacterCodingException expectedFailure = jdkStrictFailure(bytes);

            if (expectedFailure == null) {
                assertEquals(new String(bytes, utf8), URLEncodedUtil.decode(query).get("k"), query);
            } else {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode(query), query);
                assertEquals("Invalid percent-encoded byte sequence at index 0 for charset UTF-8", e.getMessage(), query);
                assertEquals(expectedFailure.getClass(), e.getCause().getClass(), query);
                assertEquals(expectedFailure.getMessage(), e.getCause().getMessage(), query);
            }

            assertEquals(new String(bytes, utf8), URLEncodedUtil.decodeLenient(query).get("k"), query);
        }

        // G075-01: every BMP character (alone, between safe characters, and inside a run with ASCII punctuation) and a spread of supplementary characters encode exactly as java.net.URLEncoder encodes them
        @Test
        void testEncode_utf8RunsMatchUrlEncoder() {
            for (int c = 0; c <= 0xFFFF; c++) {
                if (Character.isSurrogate((char) c)) {
                    continue;
                }

                final String ch = String.valueOf((char) c);

                for (final String value : new String[] { ch, "a" + ch + "b", ch + "/" + ch + ch + ":" }) {
                    assertEquals("k=" + java.net.URLEncoder.encode(value, utf8), URLEncodedUtil.encode(new Object[] { "k", value }), value);
                }
            }

            for (int codePoint = Character.MIN_SUPPLEMENTARY_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint += 0xFF) {
                final String supplementary = new String(Character.toChars(codePoint));

                for (final String value : new String[] { supplementary, "x" + supplementary + "\u00e9 /" + supplementary + supplementary }) {
                    assertEquals("k=" + java.net.URLEncoder.encode(value, utf8), URLEncodedUtil.encode(new Object[] { "k", value }), value);
                }
            }

            assertEquals("k=%F4%8F%BF%BF%F0%90%80%80", URLEncodedUtil.encode(new Object[] { "k", "\udbff\udfff\ud800\udc00" }));
            assertEquals("k=%C2%80%DF%BF%E0%A0%80%EF%BF%BF", URLEncodedUtil.encode(new Object[] { "k", "\u0080\u07ff\u0800\uffff" }));
            assertEquals("a%C3%A9%20b/%E2%82%AC", encodePath("a\u00e9 b/\u20ac"));
        }

        private String encodePath(final String value) {
            final StringBuilder sb = new StringBuilder();

            try {
                URLEncodedUtil.encPath(value, utf8, sb);
            } catch (final java.io.IOException e) {
                throw new AssertionError(e);
            }

            return sb.toString();
        }

        // G075-01: a caller-supplied Appendable still receives one append(char) per output character, and a failure in the middle of a multi-byte run leaves the same prefix
        @Test
        void testEncode_utf8AppendableCallsUnchanged() {
            final String value = "x\u00e9/\u4e2d \ud83d\ude00y";
            final String encodedValue = "x%C3%A9%2F%E4%B8%AD+%F0%9F%98%80y";
            final List<String> expectedCalls = new ArrayList<>();
            expectedCalls.add("char:k");
            expectedCalls.add("seq:=");

            for (final char c : encodedValue.toCharArray()) {
                expectedCalls.add("char:" + c);
            }

            final RecordingAppendable complete = new RecordingAppendable(-1);
            URLEncodedUtil.encode(new Object[] { "k", value }, utf8, complete);
            assertEquals(expectedCalls, complete.calls);

            for (int failAt = 1; failAt <= expectedCalls.size(); failAt++) {
                final RecordingAppendable failing = new RecordingAppendable(failAt);
                final com.landawn.abacus.exception.UncheckedIOException e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> URLEncodedUtil.encode(new Object[] { "k", value }, utf8, failing));
                assertEquals("call " + failAt, e.getCause().getMessage());
                assertEquals(expectedCalls.subList(0, failAt - 1), failing.calls);
            }
        }

        // G075-01: runs holding an unpaired surrogate still go through the reporting encoder - same exception, message, cause, and output written before the run
        @Test
        void testEncode_utf8MalformedRunsRejectedAsBefore() {
            final Object[][] cases = { { "ab\ud800", 2 }, { "a\ud800b", 1 }, { "\udc00", 0 }, { "x\u00e9\ud800", 1 }, { "x\ud800\u00e9", 1 },
                    { "\ud83d\ude00\udc00", 0 }, { "\ude00\ud83d", 0 }, { "ok \ud83d \ude00", 3 }, { "\u4e2d/\ud800:", 0 }, { "a\u00e9b\ud800\ud800", 3 } };

            for (final Object[] testCase : cases) {
                final String value = (String) testCase[0];
                final int runStart = (Integer) testCase[1];
                final StringBuilder sb = new StringBuilder();
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                        () -> URLEncodedUtil.encode(new Object[] { "k", value }, utf8, sb), value);
                assertEquals("Input contains malformed UTF-16 or a character not representable in UTF-8 in the run beginning at index " + runStart,
                        e.getMessage(), value);
                assertEquals(java.nio.charset.MalformedInputException.class, e.getCause().getClass(), value);
                assertEquals("Input length = 1", e.getCause().getMessage(), value);
                assertEquals("k=" + java.net.URLEncoder.encode(value.substring(0, runStart), utf8), sb.toString(), value);
                assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(new Object[] { "k", value }), value);
            }
        }

        // G075-02: every one- and two-byte escape run decodes (strict and lenient) exactly as the JDK UTF-8 decoder / new String do, including the rejection and its cause
        @Test
        void testDecode_utf8AllOneAndTwoByteRunsMatchJdk() {
            for (int b1 = 0; b1 < 256; b1++) {
                assertRunDecodedLikeJdk(b1);

                for (int b2 = 0; b2 < 256; b2++) {
                    assertRunDecodedLikeJdk(b1, b2);
                }
            }
        }

        // G075-02: boundary three- and four-byte runs (overlong forms, encoded surrogates, U+FFFF, U+10000, U+10FFFF and above, invalid leads, truncation, BOM) match the JDK
        @Test
        void testDecode_utf8BoundaryRunsMatchJdk() {
            final int[][] runs = { { 0xE0, 0x80, 0x80 }, { 0xE0, 0x9F, 0xBF }, { 0xE0, 0xA0, 0x80 }, { 0xE1, 0x80, 0x80 }, { 0xEC, 0xBF, 0xBF },
                    { 0xED, 0x80, 0x80 }, { 0xED, 0x9F, 0xBF }, { 0xED, 0xA0, 0x80 }, { 0xED, 0xAF, 0xBF }, { 0xED, 0xB0, 0x80 }, { 0xED, 0xBF, 0xBF },
                    { 0xEE, 0x80, 0x80 }, { 0xEF, 0xBF, 0xBF }, { 0xEF, 0xBB, 0xBF }, { 0xE4, 0xB8 }, { 0xE4, 0xB8, 0x41 }, { 0xE4, 0x41, 0xAD },
                    { 0xE4, 0xB8, 0xC0 }, { 0xF0, 0x80, 0x80, 0x80 }, { 0xF0, 0x8F, 0xBF, 0xBF }, { 0xF0, 0x90, 0x80, 0x80 }, { 0xF0, 0x9F, 0x98, 0x80 },
                    { 0xF3, 0xBF, 0xBF, 0xBF }, { 0xF4, 0x80, 0x80, 0x80 }, { 0xF4, 0x8F, 0xBF, 0xBF }, { 0xF4, 0x90, 0x80, 0x80 }, { 0xF5, 0x80, 0x80, 0x80 },
                    { 0xF7, 0xBF, 0xBF, 0xBF }, { 0xF8, 0x88, 0x80, 0x80, 0x80 }, { 0xFC, 0x84, 0x80, 0x80, 0x80, 0x80 }, { 0xF0, 0x9F, 0x98 }, { 0xF0, 0x9F },
                    { 0xF0, 0x9F, 0x98, 0x41 }, { 0xF0, 0x9F, 0x98, 0x80, 0x80 }, { 0xC3, 0xA9, 0xA9 },
                    { 0x41, 0xC3, 0xA9, 0xE4, 0xB8, 0xAD, 0xF0, 0x9F, 0x98, 0x80, 0x7F, 0x00 }, { 0xC3, 0xA9, 0xE4, 0xB8, 0xAD, 0xC3 } };

            for (final int[] run : runs) {
                assertRunDecodedLikeJdk(run);
            }

            for (int b1 = 0xE0; b1 <= 0xF7; b1++) {
                for (int b2 = 0; b2 < 256; b2 += 0x0F) {
                    assertRunDecodedLikeJdk(b1, b2, 0x80);
                    assertRunDecodedLikeJdk(b1, b2, 0xBF, 0x80);
                }
            }
        }

        // G075-02: several runs in one token, separated by literals and '+': the first malformed run is reported at its own start index with the JDK cause; lenient replaces only that run
        @Test
        void testDecode_utf8MultipleRunsPerToken() {
            assertEquals("\u00e9 \u4e2d+x\ud83d\ude00", URLEncodedUtil.decode("k=%C3%A9+%E4%B8%AD%2Bx%F0%9F%98%80").get("k"));
            assertEquals("\u00e9 \u4e2d+x\ud83d\ude00", URLEncodedUtil.decodeLenient("k=%C3%A9+%E4%B8%AD%2Bx%F0%9F%98%80").get("k"));
            assertEquals("\ufeffbom", URLEncodedUtil.decode("k=%EF%BB%BFbom").get("k"));

            final String query = "k=%C3%A9x%ED%A0%80y%E4%B8%AD";
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode(query));
            assertEquals("Invalid percent-encoded byte sequence at index 7 for charset UTF-8", e.getMessage());
            final java.nio.charset.CharacterCodingException expectedCause = jdkStrictFailure(toBytes(0xED, 0xA0, 0x80));
            assertNotNull(expectedCause);
            assertEquals(expectedCause.getClass(), e.getCause().getClass());
            assertEquals(expectedCause.getMessage(), e.getCause().getMessage());
            assertEquals("\u00e9x" + new String(toBytes(0xED, 0xA0, 0x80), utf8) + "y\u4e2d", URLEncodedUtil.decodeLenient(query).get("k"));

            final Map<String, String> pairs = URLEncodedUtil.decodeLenient("%C3%A9=%C3&a=%E4%B8%AD%E4%B8;%F0%9F%98%80=%C0%80%41");
            final Map<String, String> expected = new LinkedHashMap<>();
            expected.put("\u00e9", new String(toBytes(0xC3), utf8));
            expected.put("a", new String(toBytes(0xE4, 0xB8, 0xAD, 0xE4, 0xB8), utf8));
            expected.put("\ud83d\ude00", new String(toBytes(0xC0, 0x80, 0x41), utf8));
            assertEquals(expected, pairs);
        }

        // G075-02: random escape runs (mostly well-formed UTF-8, some corrupted, some arbitrary bytes) match the JDK in both modes
        @Test
        void testDecode_utf8RandomRunsMatchJdk() {
            final Random random = new Random(75);

            for (int round = 0; round < 20_000; round++) {
                final int length = 1 + random.nextInt(10);
                final byte[] bytes;

                if (random.nextBoolean()) {
                    final StringBuilder sb = new StringBuilder();

                    for (int i = 0; i < length; i++) {
                        final int kind = random.nextInt(4);
                        sb.appendCodePoint(kind == 0 ? random.nextInt(0x80)
                                : kind == 1 ? 0x80 + random.nextInt(0x780) : kind == 2 ? 0xE000 + random.nextInt(0x2000) : 0x10000 + random.nextInt(0x100000));
                    }

                    bytes = sb.toString().getBytes(utf8);

                    if (random.nextInt(3) == 0) {
                        bytes[random.nextInt(bytes.length)] = (byte) random.nextInt(256);
                    }
                } else {
                    bytes = new byte[length];
                    random.nextBytes(bytes);
                }

                final int[] values = new int[bytes.length];

                for (int i = 0; i < bytes.length; i++) {
                    values[i] = bytes[i] & 0xFF;
                }

                assertRunDecodedLikeJdk(values);
            }
        }
    }

    // ---- perf review 2026-10-08 G075 end ----
    // ---- perf review 2026-10-08 G076 begin ----
    @Nested
    class G076 {

        private org.w3c.dom.Element parse(final String xml) throws Exception {
            return XmlUtil.createDOMParser().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml))).getDocumentElement();
        }

        /**
         * Independent recursive model of readElement: preorder, root attributes unqualified, nested attributes and text keyed
         * by the dotted element path, text only for elements without element children, last value wins.
         */
        private Map<String, String> reference(final org.w3c.dom.Element root) {
            final Map<String, String> output = new LinkedHashMap<>();
            visit(root, null, output);
            return output;
        }

        private void visit(final org.w3c.dom.Element element, final String parentPath, final Map<String, String> output) {
            final String path = parentPath == null ? element.getNodeName() : parentPath + "." + element.getNodeName();
            final org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                final org.w3c.dom.Node attribute = attributes.item(i);
                output.put(parentPath == null ? attribute.getNodeName() : path + "." + attribute.getNodeName(), attribute.getNodeValue());
            }
            final org.w3c.dom.NodeList children = element.getChildNodes();
            final List<org.w3c.dom.Element> childElements = new ArrayList<>();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof org.w3c.dom.Element child) {
                    childElements.add(child);
                }
            }
            if (childElements.isEmpty()) {
                output.put(path, Strings.strip(element.getTextContent()));
            }
            for (final org.w3c.dom.Element child : childElements) {
                visit(child, path, output);
            }
        }

        private void assertSameEntries(final Map<String, String> expected, final Map<String, String> actual) {
            assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
            assertEquals(LinkedHashMap.class, actual.getClass());
        }

        private String prettyItems(final int count) {
            final StringBuilder sb = new StringBuilder();
            sb.append("<root a=\"1\">\n");
            for (int i = 0; i < count; i++) {
                sb.append("  <item id=\"").append(i).append("\">\n");
                sb.append("    <name> n").append(i).append(" </name>\n");
                if (i % 3 == 0) {
                    sb.append("    <!-- note ").append(i).append(" -->\n");
                }
                sb.append("    <value>v").append(i).append("<![CDATA[&x]]></value>\n");
                if (i % 5 == 0) {
                    sb.append("    <extra k=\"").append(i).append("\"/>\n");
                }
                sb.append("  </item>\n");
                if (i % 7 == 0) {
                    sb.append("  <?pi data?>\n");
                }
            }
            sb.append("  <tail>end</tail>\n</root>\n");
            return sb.toString();
        }

        private void randomElement(final Random random, final StringBuilder sb, final int depth, final boolean pretty) {
            final String[] names = { "a", "b", "item", "p:n" };
            final String name = names[random.nextInt(names.length)];
            sb.append('<').append(name);
            if (depth == 0) {
                // the root declares the namespace of the "p:" element and attribute names
                sb.append(" xmlns:p=\"urn:p\"");
            }
            final int attributeCount = random.nextInt(3);
            for (int i = 0; i < attributeCount; i++) {
                sb.append(' ').append(i == 0 ? "id" : (random.nextBoolean() ? "x" : "p:y")).append("=\"").append(random.nextInt(5)).append("&amp;\"");
            }
            final int childCount = depth >= 4 ? 0 : random.nextInt(5);
            if (childCount == 0 && random.nextInt(4) == 0) {
                sb.append("/>");
                return;
            }
            sb.append('>');
            for (int i = 0; i < childCount; i++) {
                if (pretty) {
                    sb.append("\n").append("  ".repeat(depth + 1));
                }
                switch (random.nextInt(8)) {
                    case 0:
                        sb.append(" t").append(random.nextInt(9)).append(" &lt; ");
                        break;
                    case 1:
                        sb.append("<!-- c -->");
                        break;
                    case 2:
                        sb.append("<![CDATA[ cd ]]>");
                        break;
                    case 3:
                        sb.append("<?pi x?>");
                        break;
                    default:
                        randomElement(random, sb, depth + 1, pretty);
                }
            }
            if (pretty && childCount > 0) {
                sb.append("\n").append("  ".repeat(depth));
            }
            sb.append("</").append(name).append('>');
        }

        // G076-01: readElement walks children by sibling links; pins its output on pretty-printed documents (whitespace text
        // nodes, comments, CDATA, processing instructions, empty and attribute-only elements, repeated paths).
        @Test
        public void testReadElement_prettyPrintedDocumentMatchesReference() throws Exception {
            final org.w3c.dom.Element root = parse(prettyItems(1500));
            final Map<String, String> expected = reference(root);
            assertSameEntries(expected, XmlUtil.readElement(root));
            // A second call on the same, already traversed DOM must give the same result.
            assertSameEntries(expected, XmlUtil.readElement(root));
            assertEquals("1", expected.get("a"));
            assertEquals("1499", expected.get("root.item.id"));
            assertEquals("n1499", expected.get("root.item.name"));
            assertEquals("v1499&x", expected.get("root.item.value"));
            assertEquals("1495", expected.get("root.item.extra.k"));
            assertEquals("", expected.get("root.item.extra"));
            assertEquals("end", expected.get("root.tail"));
            assertEquals(List.of("a", "root.item.id", "root.item.name", "root.item.value", "root.item.extra.k", "root.item.extra", "root.tail"),
                    new ArrayList<>(expected.keySet()));
        }

        // G076-01: random parsed documents (pretty and compact, namespaces, mixed content) give the reference result, also on
        // a second traversal of the same DOM.
        @Test
        public void testReadElement_randomDocumentsMatchReference() throws Exception {
            final Random random = new Random(20261008L);
            for (int round = 0; round < 400; round++) {
                final boolean pretty = random.nextBoolean();
                final StringBuilder sb = new StringBuilder();
                randomElement(random, sb, 0, pretty);
                final org.w3c.dom.Element root = parse(sb.toString());
                final Map<String, String> expected = reference(root);
                assertSameEntries(expected, XmlUtil.readElement(root));
                assertSameEntries(expected, XmlUtil.readElement(root));
            }
        }

        // G076-01: leaf, attribute-only, text-only, whitespace-only and non-element-only roots, element children followed by
        // trailing non-element nodes, and a programmatically built DOM with an entity reference child.
        @Test
        public void testReadElement_edgeShapesMatchReference() throws Exception {
            final String[] documents = { "<r/>", "<r id=\"1\" x=\"2\"/>", "<r>  text  </r>", "<r>   </r>", "<r><!-- c --></r>", "<r><?pi x?>t<!--c-->u</r>",
                    "<r><a/>tail<!-- c --><?pi?></r>", "<r>\n  <a>1</a>\n</r>", "<r>\n  <a>1</a>\n  <a>2</a>\n  <b><a>3</a></b>\n</r>",
                    "<r><a x=\"1\"><b>t</b></a><a y=\"2\"/></r>" };
            for (final String document : documents) {
                final org.w3c.dom.Element root = parse(document);
                final Map<String, String> expected = reference(root);
                assertSameEntries(expected, XmlUtil.readElement(root));
            }
            assertEquals(Map.of("r", ""), XmlUtil.readElement(parse("<r/>")));
            assertEquals(List.of("id", "x", "r"), new ArrayList<>(XmlUtil.readElement(parse("<r id=\"1\" x=\"2\"/>")).keySet()));
            assertEquals("tu", XmlUtil.readElement(parse("<r><?pi x?>t<!--c-->u</r>")).get("r"));
            assertEquals(Map.of("r.a", "2", "r.b.a", "3"), XmlUtil.readElement(parse("<r>\n  <a>1</a>\n  <a>2</a>\n  <b><a>3</a></b>\n</r>")));

            final org.w3c.dom.Document document = XmlUtil.createDOMParser().newDocument();
            final org.w3c.dom.Element root = document.createElement("root");
            document.appendChild(root);
            final org.w3c.dom.Element first = document.createElement("first");
            first.appendChild(document.createTextNode(" one "));
            root.appendChild(first);
            root.appendChild(document.createEntityReference("ent"));
            root.appendChild(document.createTextNode("  "));
            final org.w3c.dom.Element second = document.createElement("second");
            second.setAttribute("k", "v");
            second.appendChild(document.createEntityReference("ent"));
            root.appendChild(second);
            final Map<String, String> expected = reference(root);
            assertSameEntries(expected, XmlUtil.readElement(root));
            assertEquals("one", expected.get("root.first"));
            assertEquals("v", expected.get("root.second.k"));
            assertTrue(expected.containsKey("root.second"));
        }

        private boolean referenceIsTextElement(final org.w3c.dom.Node node) {
            final org.w3c.dom.NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    return false;
                }
            }
            return true;
        }

        private void collect(final org.w3c.dom.Node node, final List<org.w3c.dom.Node> output) {
            output.add(node);
            final org.w3c.dom.NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    output.add(attributes.item(i));
                }
            }
            for (org.w3c.dom.Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                collect(child, output);
            }
        }

        // G076-02: isTextElement walks sibling links; pins its result for every node type (document, element, text, CDATA,
        // comment, processing instruction, attribute, fragment, entity reference, document type) and on repeated calls.
        @Test
        public void testIsTextElement_allNodeTypesMatchReference() throws Exception {
            assertFalse(XmlUtil.isTextElement(null));
            final Random random = new Random(8_20261008L);
            int checked = 0;
            for (int round = 0; round < 200; round++) {
                final StringBuilder sb = new StringBuilder();
                randomElement(random, sb, 0, random.nextBoolean());
                final org.w3c.dom.Element root = parse(sb.toString());
                final List<org.w3c.dom.Node> nodes = new ArrayList<>();
                collect(root.getOwnerDocument(), nodes);
                for (int pass = 0; pass < 2; pass++) {
                    for (final org.w3c.dom.Node node : nodes) {
                        assertEquals(referenceIsTextElement(node), XmlUtil.isTextElement(node), () -> node.getNodeType() + " " + node.getNodeName());
                        checked++;
                    }
                }
            }
            assertTrue(checked > 1000);

            final org.w3c.dom.Document document = XmlUtil.createDOMParser().newDocument();
            final org.w3c.dom.Element root = document.createElement("root");
            document.appendChild(root);
            final org.w3c.dom.Attr plainAttribute = document.createAttribute("plain");
            plainAttribute.setValue("v");
            root.setAttributeNode(plainAttribute);
            final org.w3c.dom.Attr nodeAttribute = document.createAttribute("nodes");
            nodeAttribute.appendChild(document.createTextNode("a"));
            nodeAttribute.appendChild(document.createEntityReference("ent"));
            root.setAttributeNode(nodeAttribute);
            final org.w3c.dom.DocumentFragment emptyFragment = document.createDocumentFragment();
            final org.w3c.dom.DocumentFragment textFragment = document.createDocumentFragment();
            textFragment.appendChild(document.createTextNode("t"));
            final org.w3c.dom.DocumentFragment elementFragment = document.createDocumentFragment();
            elementFragment.appendChild(document.createTextNode(" "));
            elementFragment.appendChild(document.createElement("e"));
            final org.w3c.dom.DocumentType documentType = document.getImplementation().createDocumentType("root", null, null);
            final org.w3c.dom.Node[] specials = { document, root, plainAttribute, nodeAttribute, emptyFragment, textFragment, elementFragment,
                    document.createEntityReference("ent"), documentType, document.createComment("c"), document.createProcessingInstruction("t", "d"),
                    document.createCDATASection("x"), document.createTextNode("") };
            final boolean[] expected = { false, true, true, true, true, true, false, true, true, true, true, true, true };
            for (int i = 0; i < specials.length; i++) {
                assertEquals(expected[i], referenceIsTextElement(specials[i]), String.valueOf(i));
                assertEquals(expected[i], XmlUtil.isTextElement(specials[i]), String.valueOf(i));
                assertEquals(expected[i], XmlUtil.isTextElement(specials[i]), String.valueOf(i));
            }
        }
    }

    // ---- perf review 2026-10-08 G076 end ----
    // ---- perf review 2026-10-08 G118 begin ----
    @Nested
    class G118 {
        private static final String G118_SAME_COLUMNS_MESSAGE = "These two Datasets do not have the same column names: ";

        private Dataset g118Table(final List<String> columnNames, final Object[]... rows) {
            return CommonUtil.newDataset(columnNames, Arrays.asList(rows));
        }

        private String g118MergeFailure(final Dataset... datasets) {
            return assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(datasets), true)).getMessage();
        }

        /**
         * Asserts that {@code actual} is what {@code java.lang.reflect.Array.get} gives: the same wrapper class and the same value,
         * with floating-point values compared by their raw bits (NaN payloads, -0.0).
         */
        private void g118AssertSameValue(final Object expected, final Object actual) {
            if (expected == null) {
                assertNull(actual);
                return;
            }

            assertNotNull(actual);
            assertEquals(expected.getClass(), actual.getClass());

            if (expected instanceof Double) {
                assertEquals(Double.doubleToRawLongBits((Double) expected), Double.doubleToRawLongBits((Double) actual));
            } else if (expected instanceof Float) {
                assertEquals(Float.floatToRawIntBits((Float) expected), Float.floatToRawIntBits((Float) actual));
            } else {
                assertEquals(expected, actual);
            }
        }

        /**
         * One row of length 7 per primitive array class, with the boundary values of the box caches and of the type.
         */
        private Object[] g118EdgeRows() {
            return new Object[] { new int[] { Integer.MIN_VALUE, -129, -128, 0, 127, 128, Integer.MAX_VALUE },
                    new long[] { Long.MIN_VALUE, -129, -128, 0, 127, 128, Long.MAX_VALUE },
                    new double[] { Double.NaN, Double.longBitsToDouble(0x7ff8000000000123L), -0.0, 0.0, Double.MIN_VALUE, Double.NEGATIVE_INFINITY,
                            Double.MAX_VALUE },
                    new boolean[] { true, false, true, true, false, false, true },
                    new float[] { Float.NaN, Float.intBitsToFloat(0x7fc00123), -0.0f, 0.0f, Float.MIN_VALUE, Float.POSITIVE_INFINITY, Float.MAX_VALUE },
                    new char[] { 0, 'a', 127, 128, (char) 0xD800, (char) 0xDFFF, (char) 0xFFFF }, new byte[] { Byte.MIN_VALUE, -1, 0, 1, 2, 3, Byte.MAX_VALUE },
                    new short[] { Short.MIN_VALUE, -129, -128, 0, 127, 128, Short.MAX_VALUE } };
        }

        private Object g118RandomPrimitiveArray(final Random random, final int kind, final int length) {
            final Object array;

            switch (kind) {
                case 0:
                    array = new int[length];
                    break;
                case 1:
                    array = new long[length];
                    break;
                case 2:
                    array = new double[length];
                    break;
                case 3:
                    array = new boolean[length];
                    break;
                case 4:
                    array = new float[length];
                    break;
                case 5:
                    array = new char[length];
                    break;
                case 6:
                    array = new byte[length];
                    break;
                default:
                    array = new short[length];
                    break;
            }

            for (int i = 0; i < length; i++) {
                final long bits = random.nextInt(4) == 0 ? random.nextInt(300) - 150 : random.nextLong();

                if (array instanceof final int[] a) {
                    a[i] = (int) bits;
                } else if (array instanceof final long[] a) {
                    a[i] = bits;
                } else if (array instanceof final double[] a) {
                    a[i] = random.nextInt(5) == 0 ? Double.longBitsToDouble(bits) : bits / 7.0;
                } else if (array instanceof final boolean[] a) {
                    a[i] = (bits & 1) == 0;
                } else if (array instanceof final float[] a) {
                    a[i] = random.nextInt(5) == 0 ? Float.intBitsToFloat((int) bits) : bits / 7.0f;
                } else if (array instanceof final char[] a) {
                    a[i] = (char) bits;
                } else if (array instanceof final byte[] a) {
                    a[i] = (byte) bits;
                } else {
                    ((short[]) array)[i] = (short) bits;
                }
            }

            return array;
        }

        // G118-01: merge(Collection, true) compares the column-name sets through the datasets' column index maps
        @Test
        void testMerge_requiresSameColumnsAcceptsReorderedSetsAndRejectsOthers() {
            final Dataset ab = g118Table(Arrays.asList("a", "b"), new Object[] { 1, 2 }, new Object[] { 3, 4 });
            final Dataset ba = g118Table(Arrays.asList("b", "a"), new Object[] { 20, 10 });
            final Dataset ab2 = g118Table(Arrays.asList("a", "b"), new Object[] { 5, 6 });

            final Dataset two = CommonUtil.merge(Arrays.asList(ab, ba), true);
            assertEquals(Arrays.asList("a", "b"), two.columnNames());
            assertEquals(Arrays.asList(1, 3, 10), two.getColumn("a"));
            assertEquals(Arrays.asList(2, 4, 20), two.getColumn("b"));

            final Dataset three = CommonUtil.merge(Arrays.asList(ab, ba, ab2), true);
            assertEquals(Arrays.asList("a", "b"), three.columnNames());
            assertEquals(Arrays.asList(1, 3, 10, 5), three.getColumn("a"));
            assertEquals(Arrays.asList(2, 4, 20, 6), three.getColumn("b"));

            final Dataset ac = g118Table(Arrays.asList("a", "c"), new Object[] { 7, 8 });
            final Dataset a = g118Table(Arrays.asList("a"), new Object[] { 9 });
            final Dataset abc = g118Table(Arrays.asList("a", "b", "c"), new Object[] { 1, 2, 3 });

            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b], [a, c]", g118MergeFailure(ab, ac));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b], [a]", g118MergeFailure(ab, ba, a));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b], [a, b, c]", g118MergeFailure(ab, abc, ba));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a], [a, b]", g118MergeFailure(a, ab));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b, c], [a, b]", g118MergeFailure(abc, ab, ab2));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[b, a], [a, c]", g118MergeFailure(ba, ab, ac));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, c], [a, b]", g118MergeFailure(ac, ac, ab));
            // only the first name of each list differs
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[c, a], [b, a]", g118MergeFailure(g118Table(Arrays.asList("c", "a"), new Object[] { 1, 2 }), ba));

            // zero-column datasets
            assertEquals(0, CommonUtil.merge(Arrays.asList(CommonUtil.newEmptyDataset(), CommonUtil.newEmptyDataset()), true).columnCount());
            assertEquals(0, CommonUtil.merge(Arrays.asList(CommonUtil.newEmptyDataset(), CommonUtil.newEmptyDataset(), CommonUtil.newEmptyDataset()), true).columnCount());
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[], [a]", g118MergeFailure(CommonUtil.newEmptyDataset(), a));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a], []", g118MergeFailure(a, CommonUtil.newEmptyDataset(), a));

            // requiresSameColumns == false is not checked; a null element is still rejected first
            final Dataset union = CommonUtil.merge(Arrays.asList(ab, ac, a), false);
            assertEquals(Arrays.asList("a", "b", "c"), union.columnNames());
            assertEquals(Arrays.asList(1, 3, 7, 9), union.getColumn("a"));
            assertEquals(Arrays.asList(2, 4, null, null), union.getColumn("b"));
            assertEquals(Arrays.asList(null, null, 8, null), union.getColumn("c"));
            assertEquals("null element is found in datasets",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(ab, null), true)).getMessage());
            assertEquals("null element is found in datasets",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(ab, ac, null), true)).getMessage());

            // the inputs are not changed
            assertEquals(Arrays.asList("a", "b"), ab.columnNames());
            assertEquals(2, ab.size());
            assertEquals(Arrays.asList("b", "a"), ba.columnNames());
            assertEquals(1, ba.size());
        }

        // G118-01: the check follows every column rename/add/swap/move/remove, and wide schemas in any order
        @Test
        void testMerge_requiresSameColumnsFollowsColumnChangesAndWideSchemas() {
            final Dataset ds = g118Table(Arrays.asList("a", "b", "c"), new Object[] { 1, 2, 3 });
            final Dataset other = g118Table(Arrays.asList("c", "b", "a"), new Object[] { 30, 20, 10 });
            assertEquals(2, CommonUtil.merge(Arrays.asList(ds, other), true).size()); // both column index maps exist from here on

            ds.renameColumn("c", "x");
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b, x], [c, b, a]", g118MergeFailure(ds, other));
            other.renameColumn("c", "x");
            Dataset merged = CommonUtil.merge(Arrays.asList(ds, other, ds), true);
            assertEquals(Arrays.asList("a", "b", "x"), merged.columnNames());
            assertEquals(Arrays.asList(1, 10, 1), merged.getColumn("a"));
            assertEquals(Arrays.asList(3, 30, 3), merged.getColumn("x"));

            ds.addColumn("d", Arrays.asList(4));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b, x, d], [x, b, a]", g118MergeFailure(ds, other));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[x, b, a], [a, b, x, d]", g118MergeFailure(other, ds));
            other.addColumn(0, "d", Arrays.asList(40));
            merged = CommonUtil.merge(Arrays.asList(ds, other), true);
            assertEquals(Arrays.asList(4, 40), merged.getColumn("d"));

            ds.swapColumns("a", "d");
            ds.moveColumn("b", 0);
            assertEquals(Arrays.asList("b", "d", "x", "a"), ds.columnNames());
            merged = CommonUtil.merge(Arrays.asList(other, ds, other), true);
            assertEquals(Arrays.asList("d", "x", "b", "a"), merged.columnNames());
            assertEquals(Arrays.asList(40, 4, 40), merged.getColumn("d"));
            assertEquals(Arrays.asList(10, 1, 10), merged.getColumn("a"));

            ds.removeColumn("x");
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[b, d, a], [d, x, b, a]", g118MergeFailure(ds, other));
            other.removeColumn("x");
            assertEquals(Arrays.asList(20, 2), CommonUtil.merge(Arrays.asList(other, ds), true).getColumn("b"));

            // 300 columns, the other dataset in reverse order
            final List<String> names = new ArrayList<>();
            final Object[] row = new Object[300];

            for (int i = 0; i < 300; i++) {
                names.add("col" + i);
                row[i] = i;
            }

            final List<String> reversedNames = new ArrayList<>(names);
            Collections.reverse(reversedNames);
            final Object[] reversedRow = new Object[300];

            for (int i = 0; i < 300; i++) {
                reversedRow[i] = 1000 + 299 - i;
            }

            final Dataset wide = g118Table(names, row);
            final Dataset wideReversed = g118Table(reversedNames, reversedRow);
            merged = CommonUtil.merge(Arrays.asList(wide, wideReversed, wide), true);
            assertEquals(names, merged.columnNames());

            for (int i = 0; i < 300; i++) {
                assertEquals(Arrays.asList(i, 1000 + i, i), merged.getColumn("col" + i));
            }

            final List<String> renamed = new ArrayList<>(reversedNames);
            renamed.set(150, "other");
            final Dataset wideRenamed = g118Table(renamed, reversedRow);
            assertEquals(G118_SAME_COLUMNS_MESSAGE + names + ", " + renamed, g118MergeFailure(wide, wideReversed, wideRenamed));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + renamed + ", " + names, g118MergeFailure(wideRenamed, wide));

            final List<String> longer = new ArrayList<>(names);
            longer.add("col300");
            final Object[] longerRow = Arrays.copyOf(row, 301);
            assertEquals(G118_SAME_COLUMNS_MESSAGE + names + ", " + longer, g118MergeFailure(wide, g118Table(longer, longerRow)));
        }

        // G118-01: a slice invalidated by its parent still fails with ConcurrentModificationException in any position
        @Test
        void testMerge_requiresSameColumnsInvalidatedSliceStillFailsFast() {
            final Dataset parent = g118Table(Arrays.asList("a", "b"), new Object[] { 1, 2 }, new Object[] { 3, 4 });
            final Dataset slice = parent.slice(0, 1);
            final Dataset other = g118Table(Arrays.asList("b", "a"), new Object[] { 20, 10 });
            assertEquals(3, CommonUtil.merge(Arrays.asList(other, slice, other), true).size());

            parent.addRow(new Object[] { 5, 6 });

            assertThrows(java.util.ConcurrentModificationException.class, () -> CommonUtil.merge(Arrays.asList(other, slice), true));
            assertThrows(java.util.ConcurrentModificationException.class, () -> CommonUtil.merge(Arrays.asList(slice, other), true));
            assertThrows(java.util.ConcurrentModificationException.class, () -> CommonUtil.merge(Arrays.asList(other, other, slice), true));
            assertEquals(1, other.size());
        }

        // G118-02: newDataset reads primitive-array rows with typed loads: same cell values and wrapper classes as Array.get
        @Test
        void testNewDataset_primitiveArrayRowsMatchReflectiveRead() {
            final List<String> columnNames = Arrays.asList("c0", "c1", "c2", "c3", "c4", "c5", "c6");

            for (final Object row : g118EdgeRows()) {
                final Object copy = java.lang.reflect.Array.newInstance(row.getClass().getComponentType(), 7);
                System.arraycopy(row, 0, copy, 0, 7);
                final Dataset ds = CommonUtil.newDataset(columnNames, Arrays.asList(row, null, copy));
                assertEquals(columnNames, ds.columnNames());
                assertEquals(3, ds.size());

                for (int c = 0; c < 7; c++) {
                    final Object expected = java.lang.reflect.Array.get(row, c);
                    g118AssertSameValue(expected, ds.get(0, c));
                    assertNull((Object) ds.get(1, c));
                    g118AssertSameValue(expected, ds.get(2, c));
                }

                // the overload with properties takes the same path
                final Map<String, Object> properties = new HashMap<>();
                properties.put("p", 1);
                final Dataset withProperties = CommonUtil.newDataset(columnNames, Arrays.asList(row), properties);
                assertEquals(properties, withProperties.getProperties());

                for (int c = 0; c < 7; c++) {
                    g118AssertSameValue(java.lang.reflect.Array.get(row, c), withProperties.get(0, c));
                }
            }

            // a single column unpacks a one-element primitive array
            final Dataset single = CommonUtil.newDataset(Arrays.asList("a"),
                    Arrays.asList(new int[] { 5 }, new int[] { 300 }, new char[] { 'z' }, new double[] { -0.0 }));
            g118AssertSameValue(5, single.get(0, 0));
            g118AssertSameValue(300, single.get(1, 0));
            g118AssertSameValue('z', single.get(2, 0));
            g118AssertSameValue(-0.0, single.get(3, 0));

            // random rows of every primitive class, alternating classes from row to row
            final Random random = new Random(118);
            final List<String> six = Arrays.asList("a", "b", "c", "d", "e", "f");
            final List<Object> rows = new ArrayList<>();

            for (int i = 0; i < 400; i++) {
                rows.add(g118RandomPrimitiveArray(random, random.nextInt(3) == 0 ? random.nextInt(8) : i % 8, 6));
            }

            final Dataset randomDataset = CommonUtil.newDataset(six, rows);
            assertEquals(400, randomDataset.size());

            for (int r = 0; r < 400; r++) {
                for (int c = 0; c < 6; c++) {
                    g118AssertSameValue(java.lang.reflect.Array.get(rows.get(r), c), randomDataset.get(r, c));
                }
            }
        }

        // G118-02: mixed row kinds keep their cells; a wrong-length primitive row still throws the same IAE before any Dataset is built
        @Test
        void testNewDataset_mixedRowKindsAndLengthMismatchUnchanged() {
            final List<String> columnNames = Arrays.asList("a", "b");
            final Map<String, Object> mapRow = new LinkedHashMap<>();
            mapRow.put("b", "mb");
            mapRow.put("a", "ma");
            final List<Object> rows = Arrays.asList(new int[] { 1, 1000 }, new Object[] { "x", null }, new long[] { 2L, -2L }, null, Arrays.asList("l1", 7),
                    mapRow, new boolean[] { true, false }, new Integer[] { 3, 4 }, new double[] { 0.5, Double.NaN }, new short[] { 9, -9 });
            final Dataset ds = CommonUtil.newDataset(columnNames, rows);
            assertEquals(10, ds.size());
            assertEquals(Arrays.asList(1, "x", 2L, null, "l1", "ma", true, 3, 0.5, (short) 9), ds.getColumn("a"));
            assertEquals(Arrays.asList(1000, null, -2L, null, 7, "mb", false, 4, Double.NaN, (short) -9), ds.getColumn("b"));
            g118AssertSameValue(1000, ds.get(0, 1));
            g118AssertSameValue(-2L, ds.get(2, 1));
            g118AssertSameValue((short) -9, ds.get(9, 1));

            assertEquals("The length of row array (3) does not match the length of column name list (2)",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(columnNames, Arrays.asList(new int[] { 1, 2 }, new int[] { 1, 2, 3 })))
                            .getMessage());
            assertEquals("The length of row array (1) does not match the length of column name list (2)",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(columnNames, Arrays.asList(new long[] { 1, 2 }, new double[] { 1 })))
                            .getMessage());
            assertEquals("The length of row array (2) does not match the length of column name list (1)",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a"), Arrays.asList(new long[] { 1, 2 }))).getMessage());
            assertEquals("The length of row array (0) does not match the length of column name list (1)",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a"), Arrays.asList(new boolean[0]))).getMessage());
            assertEquals("The length of row array (3) does not match the length of column name list (2)", assertThrows(IllegalArgumentException.class,
                    () -> CommonUtil.newDataset(columnNames, Arrays.asList(new Object[] { 1, 2 }, new char[] { 'a', 'b', 'c' }))).getMessage());
        }

        // G118-03: convertStructure reads primitive-array sources with typed loads: same element values and classes as Array.get
        @Test
        void testConvert_primitiveArraySourcesMatchReflectiveRead() {
            final com.landawn.abacus.type.Type<List<Object>> listOfObject = com.landawn.abacus.type.Type.of("List<Object>");
            final Random random = new Random(1118);
            final List<Object> sources = new ArrayList<>(Arrays.asList(g118EdgeRows()));

            for (int i = 0; i < 200; i++) {
                sources.add(g118RandomPrimitiveArray(random, i % 8, random.nextInt(6)));
            }

            for (final Object source : sources) {
                final int length = java.lang.reflect.Array.getLength(source);
                final List<Object> list = CommonUtil.convert(source, listOfObject);
                final Object[] objects = CommonUtil.convert(source, Object[].class);
                final List<?> rawList = CommonUtil.convert(source, List.class);
                assertEquals(ArrayList.class, list.getClass());
                assertEquals(length, list.size());
                assertEquals(length, objects.length);
                assertEquals(length, rawList.size());

                for (int i = 0; i < length; i++) {
                    final Object expected = java.lang.reflect.Array.get(source, i);
                    g118AssertSameValue(expected, list.get(i));
                    g118AssertSameValue(expected, objects[i]);
                    g118AssertSameValue(expected, rawList.get(i));
                }
            }

            assertEquals(Arrays.asList("1", "-2"), CommonUtil.convert(new int[] { 1, -2 }, com.landawn.abacus.type.Type.of("List<String>")));
            assertEquals(Arrays.asList(true, false), CommonUtil.convert(new int[] { 1, 0 }, com.landawn.abacus.type.Type.of("List<Boolean>")));
            assertEquals(Arrays.asList(1L, 300L), CommonUtil.convert(new int[] { 1, 300 }, com.landawn.abacus.type.Type.of("List<Long>")));
            assertEquals(Arrays.asList(97), CommonUtil.convert(new char[] { 'a' }, com.landawn.abacus.type.Type.of("List<Integer>")));
            assertEquals(new ArrayList<>(Arrays.asList(3, 1)),
                    new ArrayList<>(CommonUtil.convert(new int[] { 3, 1, 3 }, com.landawn.abacus.type.Type.of("Set<Integer>"))));
            assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2, 3)),
                    CommonUtil.convert(new int[][] { { 1 }, { 2, 3 } }, com.landawn.abacus.type.Type.of("List<List<Integer>>")));
            assertArrayEquals(new Object[] { (short) 1, (short) -1 }, CommonUtil.convert(new short[] { 1, -1 }, Object[].class));
            assertArrayEquals(new Number[] { 1.5f }, CommonUtil.convert(new float[] { 1.5f }, Number[].class));
            assertArrayEquals(new String[] { "a", "b" }, CommonUtil.convert(new char[] { 'a', 'b' }, String[].class));
            final Double[] widened = CommonUtil.convert(new float[] { Float.intBitsToFloat(0x7fc00123) }, Double[].class);
            assertEquals(0x7ff8000000000000L, Double.doubleToRawLongBits(widened[0]));
        }

        // G118-03: primitive-array targets store exact-wrapper results with typed stores: same arrays, same exceptions at the same element
        @Test
        void testConvert_primitiveArrayTargetsAndFailuresUnchanged() {
            assertArrayEquals(new int[] { 1, 0, 3 }, CommonUtil.convert(Arrays.asList(1, null, 3), int[].class));
            assertArrayEquals(new int[] { 1, 2, 3, 99 }, CommonUtil.convert(Arrays.asList((short) 1, 2L, "3", 'c'), int[].class));
            assertArrayEquals(new int[] { 7 }, CommonUtil.convert(new long[] { 7 }, int[].class));
            assertArrayEquals(new int[] { 1 }, CommonUtil.convert(new double[] { 1.5 }, int[].class));
            assertArrayEquals(new char[] { 'A' }, CommonUtil.convert(new int[] { 65 }, char[].class));
            assertArrayEquals(new int[] { 97 }, CommonUtil.convert(new char[] { 'a' }, int[].class));
            assertArrayEquals(new boolean[] { true, false }, CommonUtil.convert(new int[] { 1, 0 }, boolean[].class));
            assertArrayEquals(new long[] { 1, 2 }, CommonUtil.convert(Arrays.asList(1, 2), long[].class));
            assertArrayEquals(new double[] { 1.5 }, CommonUtil.convert(Arrays.asList(1.5f), double[].class));
            assertArrayEquals(new byte[] { -1, 5 }, CommonUtil.convert(new Object[] { (byte) -1, "5" }, byte[].class));
            assertArrayEquals(new short[] { -300, 7 }, CommonUtil.convert(new Integer[] { -300, 7 }, short[].class));
            assertArrayEquals(new float[] { 1.25f, Float.POSITIVE_INFINITY }, CommonUtil.convert(new double[] { 1.25, 1e300 }, float[].class));

            final double[] doubles = CommonUtil.convert(Arrays.asList(Double.longBitsToDouble(0x7ff8000000000123L), -0.0), double[].class);
            assertEquals(0x7ff8000000000123L, Double.doubleToRawLongBits(doubles[0]));
            assertEquals(0x8000000000000000L, Double.doubleToRawLongBits(doubles[1]));
            final float[] floats = CommonUtil.convert(Arrays.asList(Float.intBitsToFloat(0x7fc00123), -0.0f), float[].class);
            assertEquals(0x7fc00123, Float.floatToRawIntBits(floats[0]));
            assertEquals(0x80000000, Float.floatToRawIntBits(floats[1]));

            final int[][] nested = CommonUtil.convert(Arrays.asList(new Integer[] { 1, 2 }, null), int[][].class);
            assertArrayEquals(new int[] { 1, 2 }, nested[0]);
            assertNull(nested[1]);

            final int[] same = { 1 };
            assertSame(same, CommonUtil.convert(same, int[].class));

            // random Integer/Long/Double lists into primitive targets: the same values a per-element conversion gives
            final Random random = new Random(2118);

            for (int i = 0; i < 200; i++) {
                final List<Integer> values = new ArrayList<>();

                for (int j = 0, n = random.nextInt(8); j < n; j++) {
                    values.add(random.nextInt(4) == 0 ? null : random.nextInt());
                }

                final int[] ints = CommonUtil.convert(values, int[].class);
                final long[] longs = CommonUtil.convert(values, long[].class);
                assertEquals(values.size(), ints.length);

                for (int j = 0; j < ints.length; j++) {
                    assertEquals(values.get(j) == null ? 0 : values.get(j), ints[j]);
                    assertEquals(values.get(j) == null ? 0L : (long) values.get(j), longs[j]);
                }
            }

            final NumberFormatException listFailure = assertThrows(NumberFormatException.class, () -> CommonUtil.convert(Arrays.asList("1", "abc"), int[].class));
            assertEquals("abc is not a valid Integer.", listFailure.getMessage());
            assertEquals("x is not a valid Integer.",
                    assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new String[] { "1", "x" }, int[].class)).getMessage());
            assertEquals("true is not a valid Integer.",
                    assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new boolean[] { true }, int[].class)).getMessage());
            assertEquals("int overflow: 9223372036854775807",
                    assertThrows(ArithmeticException.class, () -> CommonUtil.convert(new long[] { 1, Long.MAX_VALUE }, int[].class)).getMessage());
            assertEquals("Cannot convert Integer value 2 to boolean: only 1 (true), 0 (false) and -1 (false) are accepted",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(new int[] { 1, 2 }, boolean[].class)).getMessage());
            assertEquals("No public static method or constructor found in java.lang.Number to create a value from String",
                    assertThrows(UnsupportedOperationException.class, () -> CommonUtil.convert(new Object[] { "a" }, Number[].class)).getMessage());
        }
    }

    // ---- perf review 2026-10-08 G118 end ----
    // ---- perf review 2026-10-08 G120 begin ----
    @Nested
    class G120 {
        private final java.util.TimeZone newYork = java.util.TimeZone.getTimeZone("America/New_York");

        // A SimpleTimeZone with daylight-saving rules no ZoneId can express: the Calendar fallback of resolveLocalMillis.
        private final java.util.TimeZone customDst = new java.util.SimpleTimeZone(-5 * 3600_000, "custom-dst", 2, 8, -1, 7200000, 10, 1, -1, 7200000, 3600000);

        // A sub-second raw offset no ZoneId can express.
        private final java.util.TimeZone subSecond = new java.util.SimpleTimeZone(1500, "sub-second");

        private void assertParseFailure(final String expectedMessage, final Class<? extends Throwable> expectedCause,
                final org.junit.jupiter.api.function.Executable executable) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
            assertEquals(expectedMessage, e.getMessage());

            if (expectedCause == null) {
                assertNull(e.getCause());
            } else {
                assertNotNull(e.getCause());
                assertEquals(expectedCause, e.getCause().getClass());
            }
        }

        // G120-01: auto-detected fractional ISO offset text (+HH:mm, compact +HHmm) on the legacy targets - same instant, the fallback zone plays no part
        @Test
        void testParseToJUDate_autoOffsetFraction() {
            final String[][] cases = { { "2025-01-15T10:30:45.123+05:30", "2025-01-15T05:00:45.123Z" },
                    { "2025-01-15T10:30:45.123-0500", "2025-01-15T15:30:45.123Z" }, { "2025-01-15T10:30:45.5+0545", "2025-01-15T04:45:45.500Z" },
                    { "2025-01-15T10:30:45.123456789-09:30", "2025-01-15T20:00:45.123456789Z" }, { "2025-07-15T10:30:45.1-08:00", "2025-07-15T18:30:45.100Z" },
                    { "2025-01-15T10:30:45.123+18:00", "2025-01-14T16:30:45.123Z" }, { "2025-01-15T10:30:45.123-18:00", "2025-01-16T04:30:45.123Z" },
                    { "2025-01-15T10:30:45.123-1800", "2025-01-16T04:30:45.123Z" }, { "2025-01-15T10:30:45.123-00:00", "2025-01-15T10:30:45.123Z" },
                    { "2025-01-15T10:30:45.123+0000", "2025-01-15T10:30:45.123Z" }, { "2024-02-29T00:00:00.1+01:00", "2024-02-28T23:00:00.100Z" },
                    { "2025-03-09T02:30:00.5-05:00", "2025-03-09T07:30:00.500Z" }, { "2025-11-02T01:30:00.25-04:00", "2025-11-02T05:30:00.250Z" },
                    { "0001-01-01T00:00:00.000000001+01:00", "0000-12-31T23:00:00.000000001Z" },
                    { "9999-12-31T23:59:59.999999999-18:00", "+10000-01-01T17:59:59.999999999Z" },
                    { "1969-12-31T23:59:59.9999+00:00", "1969-12-31T23:59:59.9999Z" },
                    // a seconds-precision offset keeps the java.time route
                    { "1880-01-15T10:30:45.123-04:56:02", "1880-01-15T15:26:47.123Z" } };

            for (final String[] c : cases) {
                final String text = c[0];
                final long expected = java.time.Instant.parse(c[1]).toEpochMilli();

                assertEquals(expected, Dates.parseToJUDate(text).getTime(), text);
                assertEquals(expected, Dates.parseToJUDate(text, null, newYork).getTime(), text);
                assertEquals(expected, Dates.parseToJUDate(text, "", customDst).getTime(), text);
                assertEquals(expected, Dates.parseToJUDate(text, null, subSecond, java.util.Locale.FRANCE).getTime(), text);
                assertEquals(expected, Dates.parseToDate(text, null, newYork).getTime(), text);
                assertEquals(expected, Dates.parseToTime(text, "", customDst).getTime(), text);

                final java.util.Calendar calendar = Dates.parseToCalendar(text, null, newYork);
                assertEquals(expected, calendar.getTimeInMillis(), text);
                assertEquals(expected, Dates.parseToGregorianCalendar(text, "", customDst).getTimeInMillis(), text);
                // the routes that read this shape through parseAutoTimestampToInstant agree
                assertEquals(expected, Dates.parseToTimestamp(text, null, newYork).getTime(), text);
            }

            assertEquals(1736917245123L, Dates.parseToJUDate("2025-01-15T10:30:45.123+05:30").getTime());

            final java.util.Calendar kolkata = Dates.parseToCalendar("2025-01-15T10:30:45.123+05:30", null, newYork);
            assertEquals(19800000, kolkata.getTimeZone().getRawOffset());
            assertEquals(10, kolkata.get(java.util.Calendar.HOUR_OF_DAY));
            assertEquals(123, kolkata.get(java.util.Calendar.MILLISECOND));
        }

        // G120-01: offset text the direct read rejects still reaches the java.time grammar and keeps its diagnostic
        @Test
        void testParseToJUDate_autoOffsetFractionInvalidFields() {
            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;
            final String[][] cases = {
                    { "2025-02-29T00:00:00.0+05:30",
                            "Text '2025-02-29T00:00:00.0+05:30' could not be parsed: Invalid date 'February 29' as '2025' is not a leap year" },
                    { "2025-02-30T10:30:45.123+05:30", "Text '2025-02-30T10:30:45.123+05:30' could not be parsed: Invalid date 'FEBRUARY 30'" },
                    { "2025-13-01T00:00:00.1+05:30",
                            "Text '2025-13-01T00:00:00.1+05:30' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13" },
                    { "2025-01-15T24:00:00.1+05:30",
                            "Text '2025-01-15T24:00:00.1+05:30' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24" },
                    { "2025-01-15T23:60:00.1-0500",
                            "Text '2025-01-15T23:60:00.1-05:00' could not be parsed: Invalid value for MinuteOfHour (valid values 0 - 59): 60" },
                    { "2025-01-15T23:59:60.1-05:00",
                            "Text '2025-01-15T23:59:60.1-05:00' could not be parsed: Invalid value for SecondOfMinute (valid values 0 - 59): 60" },
                    { "2025-01-15T10:30:45.123+18:01",
                            "Text '2025-01-15T10:30:45.123+18:01' could not be parsed: Zone offset not in valid range: -18:00 to +18:00" },
                    { "2025-01-15T10:30:45.123+19:00",
                            "Text '2025-01-15T10:30:45.123+19:00' could not be parsed: Zone offset not in valid range: -18:00 to +18:00" },
                    { "2025-01-15T10:30:45.123+05:60", "Text '2025-01-15T10:30:45.123+05:60' could not be parsed, unparsed text found at index 26" },
                    { "2025-01-15T10:30:45.123+2400",
                            "Text '2025-01-15T10:30:45.123+24:00' could not be parsed: Value out of range: Hour[0-23], Minute[0-59], Second[0-59]" },
                    { "2025-0a-15T10:30:45.123+05:30", "Text '2025-0a-15T10:30:45.123+05:30' could not be parsed at index 5" },
                    { "1880-01-15T10:30:45.123-04:56:60", "Text '1880-01-15T10:30:45.123-04:56:60' could not be parsed, unparsed text found at index 29" } };

            for (final String[] c : cases) {
                final String expectedMessage = "Cannot parse \"" + c[0] + "\" with the auto-detected format: " + c[1];

                assertParseFailure(expectedMessage, dtpe, () -> Dates.parseToJUDate(c[0]));
                assertParseFailure(expectedMessage, dtpe, () -> Dates.parseToDate(c[0], null, newYork));
                assertParseFailure(expectedMessage, dtpe, () -> Dates.parseToCalendar(c[0], "", customDst));
            }

            assertParseFailure("Format 'yyyy-MM-dd'T'HH:mm:ssXXX' requires a Common Era year from 0001 through 9999 written as exactly four digits: "
                    + "\"0000-01-01T00:00:00.1+05:30\"", null, () -> Dates.parseToJUDate("0000-01-01T00:00:00.1+05:30"));
        }

        // G120-02: auto-detected ISO local text with a 1-2 or 4-9 digit fraction - same wall-clock resolution, fraction truncated to milliseconds
        @Test
        void testParseToJUDate_autoLocalFraction() {
            final String[] zones = { "America/New_York", "Asia/Kolkata", "Australia/Lord_Howe", "UTC" };
            final String[] texts = { "2025-01-15T10:30:45.5", "2025-01-15T10:30:45.12", "2025-07-15T10:30:45.1234", "2025-01-15T10:30:45.123456",
                    "2025-07-15T10:30:45.123456789", "2025-01-15T10:30:45.000000001", "2024-02-29T23:59:59.999999999", "2025-03-09T01:59:59.99",
                    "2025-03-09T03:00:00.01", "2025-10-05T02:30:00.5", "2025-04-06T02:00:00.5", "1883-11-18T11:59:59.5", "1969-12-31T23:59:59.999999",
                    "0001-01-01T00:00:00.5", "9999-12-31T23:59:59.9" };

            for (final String zoneId : zones) {
                final java.util.TimeZone zone = java.util.TimeZone.getTimeZone(zoneId);
                final java.time.zone.ZoneRules rules = java.time.ZoneId.of(zoneId).getRules();

                for (final String text : texts) {
                    final java.time.LocalDateTime local = java.time.LocalDateTime.parse(text);
                    final java.util.List<java.time.ZoneOffset> offsets = rules.getValidOffsets(local);

                    if (offsets.size() != 1) {
                        continue;
                    }

                    final long expected = local.withNano(0).toInstant(offsets.get(0)).toEpochMilli() + local.getNano() / 1_000_000;

                    assertEquals(expected, Dates.parseToJUDate(text, null, zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToDate(text, "", zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToTime(text, null, zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToCalendar(text, null, zone).getTimeInMillis(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToGregorianCalendar(text, "", zone).getTimeInMillis(), text + " " + zoneId);
                    // the JDBC escape spelling and parseToTimestamp's direct read name the same instant
                    assertEquals(expected, Dates.parseToJUDate(text.replace('T', ' '), null, zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToTimestamp(text, null, zone).getTime(), text + " " + zoneId);
                }
            }

            assertEquals(-1L, Dates.parseToJUDate("1969-12-31T23:59:59.999999", null, java.util.TimeZone.getTimeZone("UTC")).getTime());
            // the Calendar fallback for rules no ZoneId can express: winter offset -05:00; the overlap keeps the standard offset
            assertEquals(java.time.Instant.parse("2025-01-15T15:30:45.123Z").toEpochMilli(),
                    Dates.parseToJUDate("2025-01-15T10:30:45.123456", null, customDst).getTime());
            assertEquals(1762065000500L, Dates.parseToTime("2025-11-02T01:30:00.5", "", customDst).getTime());
            assertEquals(java.time.Instant.parse("2025-07-15T14:30:45.120Z").toEpochMilli(),
                    Dates.parseToCalendar("2025-07-15T10:30:45.12", null, customDst).getTimeInMillis());
        }

        // G120-02: ISO local text the direct read rejects still reaches the java.time grammar; gap/overlap diagnostics unchanged
        @Test
        void testParseToJUDate_autoLocalFractionInvalidFields() {
            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;
            final String[][] cases = { { "2025-04-31T00:00:00.1", "Text '2025-04-31T00:00:00.1' could not be parsed: Invalid date 'APRIL 31'" },
                    { "2025-01-15T24:00:00.1", "Text '2025-01-15T24:00:00.1' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24" },
                    { "2025-02-29T12:00:00.123456",
                            "Text '2025-02-29T12:00:00.123456' could not be parsed: Invalid date 'February 29' as '2025' is not a leap year" },
                    { "2025-01-15T10:60:00.12", "Text '2025-01-15T10:60:00.12' could not be parsed: Invalid value for MinuteOfHour (valid values 0 - 59): 60" },
                    { "2025-01-15T10:30:60.1234",
                            "Text '2025-01-15T10:30:60.1234' could not be parsed: Invalid value for SecondOfMinute (valid values 0 - 59): 60" } };

            for (final String[] c : cases) {
                assertParseFailure("Cannot parse \"" + c[0] + "\" with the auto-detected format in time zone America/New_York: " + c[1], dtpe,
                        () -> Dates.parseToJUDate(c[0], null, newYork));
                assertParseFailure("Cannot parse \"" + c[0] + "\" with the auto-detected format in time zone custom-dst: " + c[1], dtpe,
                        () -> Dates.parseToTime(c[0], "", customDst));
            }

            assertParseFailure(
                    "Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)",
                    java.time.DateTimeException.class, () -> Dates.parseToJUDate("2025-03-09T02:30:00.5", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-11-02T01:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Ambiguous local date-time 2025-11-02T01:30 in zone America/New_York (DST overlap); valid offsets are [-04:00, -05:00]",
                    java.time.DateTimeException.class, () -> Dates.parseToDate("2025-11-02T01:30:00.5", "", newYork));
            assertParseFailure("Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone custom-dst: HOUR_OF_DAY: 2 -> 1",
                    IllegalArgumentException.class, () -> Dates.parseToTime("2025-03-09T02:30:00.5", "", customDst));
            assertParseFailure("Format 'yyyy-MM-dd'T'HH:mm:ss.SSS' requires a Common Era year from 0001 through 9999 written as exactly four digits: "
                    + "\"0000-01-01T00:00:00.5\"", null, () -> Dates.parseToJUDate("0000-01-01T00:00:00.5", null, newYork));
        }
    }

    // ---- perf review 2026-10-08 G120 end ----
    // ---- perf review 2026-10-08 G122 begin ----
    @Nested
    class G122 {
        private final class Item implements Comparable<Item> {
            private final int key;
            private final int id;

            Item(final int key, final int id) {
                this.key = key;
                this.id = id;
            }

            @Override
            public int compareTo(final Item other) {
                return Integer.compare(key, other.key);
            }

            @Override
            public String toString() {
                return key + "#" + id;
            }
        }

        private final class ProbeException extends Exception {
            private static final long serialVersionUID = 1L;

            ProbeException(final String message) {
                super(message);
            }
        }

        private List<Item> items(final int size, final long seed, final int keyRange) {
            final Random random = new Random(seed);
            final List<Item> list = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                list.add(new Item(random.nextInt(keyRange), i));
            }

            return list;
        }

        private Map<Percentage, Item> stableOracle(final List<Item> list, final java.util.Comparator<? super Item> comparator) {
            final Item[] sorted = list.toArray(new Item[0]);
            Arrays.sort(sorted, comparator); // stable, as N.sort is
            return N.percentilesOfSorted(sorted);
        }

        private void assertSameElements(final Map<Percentage, Item> expected, final Map<Percentage, Item> actual) {
            assertEquals(expected.keySet(), actual.keySet());

            for (final Map.Entry<Percentage, Item> e : expected.entrySet()) {
                assertSame(e.getValue(), actual.get(e.getKey()), String.valueOf(e.getKey()));
            }
        }

        // G122-01: percentiles()/percentiles(Comparator) pick the same elements as a stable sort of the remaining elements, for
        // every source shape (array, iterator, pipeline stages, already sorted by the same or by another comparator), with ties.
        @Test
        void testPercentiles_matchesStableSortOracleForEverySourceShape() throws Exception {
            final java.util.Comparator<Item> natural = java.util.Comparator.naturalOrder();
            final java.util.Comparator<Item> reverse = java.util.Comparator.reverseOrder();

            for (final int size : new int[] { 1, 2, 3, 10, 99, 1000 }) {
                for (final int keyRange : new int[] { 1, 3, 1_000_000 }) {
                    final List<Item> list = items(size, size * 31L + keyRange, keyRange);
                    final Item[] array = list.toArray(new Item[0]);

                    assertSameElements(stableOracle(list, natural), Seq.<Item, Exception> of(array).percentiles().get());
                    assertSameElements(stableOracle(list, natural), Seq.<Item, Exception> of(list.iterator()).percentiles().get());
                    assertSameElements(stableOracle(list, reverse), Seq.<Item, Exception> of(array).percentiles(reverse).get());
                    assertSameElements(stableOracle(list, reverse), Seq.<Item, Exception> of(list).map(e -> e).percentiles(reverse).get());
                    // already sorted by the same comparator: the shortcut keeps the sorted order without a second sort
                    assertSameElements(stableOracle(list, natural), Seq.<Item, Exception> of(array).sorted().percentiles().get());
                    assertSameElements(stableOracle(list, reverse), Seq.<Item, Exception> of(array).sorted(reverse).percentiles(reverse).get());
                    // already sorted by another comparator: sorted again, stably, from that order
                    final List<Item> reversedFirst = new ArrayList<>(list);
                    reversedFirst.sort(reverse);
                    assertSameElements(stableOracle(reversedFirst, natural), Seq.<Item, Exception> of(array).sorted(reverse).percentiles().get());

                    final List<Item> filtered = new ArrayList<>();
                    for (final Item e : list) {
                        if (e.id % 3 != 1) {
                            filtered.add(e);
                        }
                    }

                    if (!filtered.isEmpty()) {
                        assertSameElements(stableOracle(filtered, natural), Seq.<Item, Exception> of(array).filter(e -> e.id % 3 != 1).percentiles().get());
                    }
                }
            }

            // nulls: percentiles() orders them first; the caller's array is never reordered
            final Item[] withNulls = { new Item(5, 0), null, new Item(1, 2), null, new Item(3, 4) };
            final Item[] copy = withNulls.clone();
            final Map<Percentage, Item> result = Seq.<Item, Exception> of(withNulls).percentiles().get();
            assertNull(result.get(Percentage._0_0001));
            assertSame(withNulls[0], result.get(Percentage._99_9999));
            assertArrayEquals(copy, withNulls);
            assertTrue(Seq.<Item, Exception> empty().percentiles().isEmpty());
            assertTrue(Seq.<Item, Exception> of(new Item[0]).percentiles(reverse).isEmpty());
        }

        // G122-01: the source is closed exactly once, before the first comparison, and the sequence is unusable afterwards.
        @Test
        void testPercentiles_closesTheSourceOnceBeforeTheFirstComparison() throws Exception {
            final List<String> log = new ArrayList<>();
            final java.util.Comparator<Item> logging = (a, b) -> {
                log.add("compare");
                return Integer.compare(a.key, b.key);
            };
            final Seq<Item, Exception> seq = Seq.<Item, Exception> of(items(50, 7, 10)).onClose(() -> log.add("close1")).onClose(() -> log.add("close2"));

            assertTrue(seq.percentiles(logging).isPresent());
            assertEquals(List.of("close1", "close2"), log.subList(0, 2));
            assertEquals(log.size() - 2, Collections.frequency(log, "compare"));
            assertTrue(log.size() > 2);
            assertThrows(IllegalStateException.class, seq::count);

            log.clear();
            final Seq<Item, Exception> natural = Seq.<Item, Exception> of(items(3, 8, 10)).onClose(() -> log.add("close"));
            assertTrue(natural.percentiles().isPresent());
            assertEquals(List.of("close"), log);
            assertThrows(IllegalStateException.class, natural::percentiles);
        }

        // G122-01: on an already sorted sequence nothing is compared, and the inherited close handlers still run (once) while the
        // sorted sequence itself is not yet marked closed - exactly as sorted(comparator).toArray() does; draining and closing it
        // directly (the stream-side shape) would let such a handler see the sequence closed.
        @Test
        void testPercentiles_alreadySortedShortcutKeepsTheCloseOrder() throws Exception {
            final List<String> log = new ArrayList<>();
            final java.util.Comparator<Item> logging = (a, b) -> {
                log.add("compare");
                return Integer.compare(a.key, b.key);
            };
            final Seq<Item, RuntimeException> sorted = Seq.<Item, RuntimeException> of(items(20, 9, 5)).sorted(logging);
            assertTrue(log.isEmpty()); // sorted(..) is lazy

            final Seq<Item, RuntimeException> withHandler = sorted.onClose(() -> {
                try {
                    log.add("reentrant count=" + sorted.count());
                } catch (final IllegalStateException e) {
                    log.add("reentrant closed");
                }
            });
            final Map<Percentage, Item> result = withHandler.percentiles(logging).get();
            final int compares = Collections.frequency(log, "compare");
            assertTrue(compares > 0); // the sort of sorted(logging) itself
            assertEquals(List.of("reentrant count=0"), log.subList(compares, log.size()));
            assertEquals(Collections.nCopies(compares, "compare"), log.subList(0, compares));
            assertNotNull(result);
            assertThrows(IllegalStateException.class, withHandler::count);

            log.clear();
            final Seq<Item, RuntimeException> unsorted = Seq.<Item, RuntimeException> of(items(20, 9, 5));
            final Seq<Item, RuntimeException> withHandler2 = unsorted.onClose(() -> {
                try {
                    log.add("reentrant count=" + unsorted.count());
                } catch (final IllegalStateException e) {
                    log.add("reentrant closed");
                }
            });
            withHandler2.percentiles(logging);
            assertEquals("reentrant closed", log.get(0)); // closed before the first comparison
            assertEquals(log.size() - 1, Collections.frequency(log, "compare"));
        }

        // G122-01: failures keep their order: a source failure wins (a failing close handler is suppressed into it, once); a
        // comparator failure comes after the close; a failing close handler stops the operation before any comparison.
        @Test
        void testPercentiles_failuresKeepTheirOrderAndCloseOnce() {
            final AtomicInteger closes = new AtomicInteger();
            final AtomicInteger compares = new AtomicInteger();
            final java.util.Comparator<Item> counting = (a, b) -> {
                compares.incrementAndGet();
                return Integer.compare(a.key, b.key);
            };

            final Seq<Item, Exception> failingSource = Seq.<Item, Exception> of(items(10, 1, 10)).map(e -> {
                if (e.id == 4) {
                    throw new ProbeException("source fails at 4");
                }
                return e;
            }).onClose(() -> {
                closes.incrementAndGet();
                throw new IllegalStateException("close fails");
            });
            final ProbeException sourceFailure = assertThrows(ProbeException.class, () -> failingSource.percentiles(counting));
            assertEquals("source fails at 4", sourceFailure.getMessage());
            assertEquals(1, sourceFailure.getSuppressed().length);
            assertEquals("close fails", sourceFailure.getSuppressed()[0].getMessage());
            assertEquals(1, closes.get());
            assertEquals(0, compares.get());

            closes.set(0);
            final java.util.Comparator<Item> failing = (a, b) -> {
                compares.incrementAndGet();
                if (compares.get() == 3) {
                    throw new UnsupportedOperationException("compare fails at 3");
                }
                return Integer.compare(a.key, b.key);
            };
            compares.set(0);
            final Seq<Item, Exception> seq = Seq.<Item, Exception> of(items(10, 2, 10)).onClose(closes::incrementAndGet);
            final UnsupportedOperationException compareFailure = assertThrows(UnsupportedOperationException.class, () -> seq.percentiles(failing));
            assertEquals("compare fails at 3", compareFailure.getMessage());
            assertEquals(0, compareFailure.getSuppressed().length);
            assertEquals(1, closes.get());
            assertEquals(3, compares.get());

            compares.set(0);
            final Seq<Item, Exception> failingClose = Seq.<Item, Exception> of(items(10, 3, 10)).onClose(() -> {
                throw new IllegalStateException("close fails");
            });
            assertEquals("close fails", assertThrows(IllegalStateException.class, () -> failingClose.percentiles(counting)).getMessage());
            assertEquals(0, compares.get());
            assertThrows(IllegalStateException.class, failingClose::count);

            final Seq<Item, Exception> closed = Seq.<Item, Exception> of(items(3, 4, 10));
            closed.close();
            assertThrows(IllegalStateException.class, closed::percentiles);
            assertThrows(IllegalArgumentException.class, () -> Seq.<Item, Exception> of(items(3, 5, 10)).percentiles(null));
        }

        /** A HashSet subclass: SetMultimap copies it through the value supplier. */
        private final class SupplierCopiedSet<T> extends HashSet<T> {
            private static final long serialVersionUID = 1L;
        }

        /** An element whose hashCode can run an action once, at a given call. */
        private final class HookElement {
            private final String name;
            private Runnable hook;

            HookElement(final String name) {
                this.name = name;
            }

            @Override
            public int hashCode() {
                if (hook != null) {
                    final Runnable action = hook;
                    hook = null;
                    action.run();
                }
                return name.hashCode();
            }

            @Override
            public boolean equals(final Object o) {
                return o instanceof HookElement && ((HookElement) o).name.equals(name);
            }

            @Override
            public String toString() {
                return name;
            }
        }

        // G122-02: SetMultimap value sets that are copied with their own constructors (HashSet, LinkedHashSet, sorted sets,
        // IdentityHashSet) never reach the value supplier; every copy() / toMap() / toMap(IntFunction) result is equal to the
        // source, keeps each set's type, order and membership policy, and shares no set with the source or another key.
        @Test
        void testCopy_constructorCopiedValueSetsNeverCallTheValueSupplier() {
            final String first = new String("x");
            final String second = new String("x");
            final IdentityHashSet<String> identity = new IdentityHashSet<>();
            identity.add(first);
            identity.add(second);
            final TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Collections.addAll(caseInsensitive, "b", "A", "c");
            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("hash", new HashSet<>(Arrays.asList("p", "q")));
            backing.put("linked", new LinkedHashSet<>(Arrays.asList("z", "y", "x")));
            backing.put("sorted", caseInsensitive);
            backing.put("identity", identity);
            backing.put("emptied", new LinkedHashSet<>(Arrays.asList("gone")));

            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return new HashSet<>();
            });
            backing.get("emptied").clear();

            final List<Map<String, Set<String>>> results = new ArrayList<>();
            final SetMultimap<String, String> copy = multimap.copy();
            results.add(copy.toMap());
            results.add(multimap.toMap());
            results.add(multimap.toMap(size -> new TreeMap<>()));
            assertEquals(multimap, copy);

            for (final Map<String, Set<String>> result : results) {
                assertEquals(backing.keySet(), result.keySet());

                for (final Map.Entry<String, Set<String>> e : result.entrySet()) {
                    final Set<String> source = backing.get(e.getKey());
                    assertNotSame(source, e.getValue());
                    assertSame(source.getClass(), e.getValue().getClass());
                    if (!"identity".equals(e.getKey())) {
                        assertEquals(new ArrayList<>(source), new ArrayList<>(e.getValue()), e.getKey()); // same order
                    }
                }

                assertEquals(2, result.get("identity").size());
                assertTrue(result.get("sorted").contains("a"));
                assertTrue(result.get("emptied").isEmpty());
            }

            for (final Set<String> value : copy.toMap().values()) {
                for (final Set<String> source : backing.values()) {
                    assertNotSame(source, value);
                }
            }

            assertEquals(0, supplierCalls.get());
        }

        // G122-02: a set copied through the value supplier after constructor copies were made still cannot be handed an
        // earlier copy (captured from the target map) or any source set; the supplier is called once per such set.
        @Test
        void testCopy_valueSupplierCannotReturnAnEarlierCopyOrASource() {
            final Map<String, Set<String>> backing = new LinkedHashMap<>();
            backing.put("a", new HashSet<>(Arrays.asList("1")));
            backing.put("b", new TreeSet<>(Arrays.asList("2", "3")));
            final SupplierCopiedSet<String> odd = new SupplierCopiedSet<>();
            odd.add("4");
            backing.put("c", odd);
            final Set<String> last = new LinkedHashSet<>(Arrays.asList("5"));
            backing.put("d", last);

            final List<Set<String>> captured = new ArrayList<>();
            final int[] mode = { 0 };
            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, String> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                switch (mode[0]) {
                    case 0:
                        return captured.get(captured.size() - 1); // the copy of "b"
                    case 1:
                        return last; // a later source
                    case 2:
                        return backing.get("a"); // an earlier source
                    default:
                        return new LinkedHashSet<>();
                }
            });
            final java.util.function.IntFunction<Map<String, Set<String>>> capturingMap = size -> new LinkedHashMap<>() {
                private static final long serialVersionUID = 1L;

                @Override
                public Set<String> put(final String key, final Set<String> value) {
                    captured.add(value);
                    return super.put(key, value);
                }
            };

            for (int m = 0; m < 3; m++) {
                mode[0] = m;
                captured.clear();
                supplierCalls.set(0);
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> multimap.toMap(capturingMap));
                assertEquals("The value supplier returned a collection already in use; it must return a new empty collection on every call", e.getMessage());
                assertEquals(1, supplierCalls.get());
                assertEquals(2, captured.size());
            }

            mode[0] = 3;
            captured.clear();
            supplierCalls.set(0);
            final Map<String, Set<String>> result = multimap.toMap(capturingMap);
            assertEquals(1, supplierCalls.get());
            assertEquals(new LinkedHashSet<>(Arrays.asList("4")), result.get("c"));
            assertEquals(Arrays.asList("1"), new ArrayList<>(backing.get("a")));
            assertEquals(Arrays.asList("2", "3"), new ArrayList<>(backing.get("b")));
            assertEquals(Arrays.asList("4"), new ArrayList<>(odd));
            assertEquals(Arrays.asList("5"), new ArrayList<>(last));
        }

        // G122-02: the source sets are reserved as they were when the copy started, even when a callback replaces one of them
        // in the backing map before the value supplier is first called: handing back the replaced set is still rejected as a
        // set in use (not as a non-empty set).
        @Test
        void testCopy_sourcesAreReservedAsOfTheStartOfTheCopy() {
            final HookElement hookElement = new HookElement("h");
            final Map<String, Set<Object>> backing = new LinkedHashMap<>();
            backing.put("a", new HashSet<>(Arrays.asList(hookElement)));
            final SupplierCopiedSet<Object> odd = new SupplierCopiedSet<>();
            odd.add("o");
            backing.put("b", odd);
            final Set<Object> original = new HashSet<>(Arrays.asList("c1"));
            backing.put("c", original);
            final AtomicInteger supplierCalls = new AtomicInteger();
            final SetMultimap<String, Object> multimap = SetMultimap.wrap(backing, () -> {
                supplierCalls.incrementAndGet();
                return original;
            });

            // runs while "a" is copied, i.e. after the copy started and before the value supplier is called for "b"
            hookElement.hook = () -> backing.put("c", new LinkedHashSet<>());
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, multimap::toMap);
            assertEquals("The value supplier returned a collection already in use; it must return a new empty collection on every call", e.getMessage());
            assertEquals(1, supplierCalls.get());
            assertEquals(Arrays.asList("c1"), new ArrayList<>(original));
        }

        // G122-02: ListMultimap copies are unchanged: every list comes from the value supplier, which can return neither a
        // source list nor the same list twice.
        @Test
        void testCopy_listMultimapStillChecksEverySuppliedList() {
            final Map<String, List<String>> backing = new LinkedHashMap<>();
            backing.put("a", new ArrayList<>(Arrays.asList("1", "1")));
            backing.put("b", new ArrayList<>(Arrays.asList("2")));
            final ListMultimap<String, String> multimap = ListMultimap.wrap(backing);
            final ListMultimap<String, String> copy = multimap.copy();
            assertEquals(multimap, copy);
            assertNotSame(backing.get("a"), copy.get("a"));
            assertEquals(Arrays.asList("1", "1"), multimap.toMap().get("a"));

            final List<String> shared = new ArrayList<>();
            final ListMultimap<String, String> reusing = CommonUtil.newListMultimap(LinkedHashMap::new, () -> shared);
            reusing.putValues("x", Arrays.asList("1"));
            assertThrows(IllegalArgumentException.class, reusing::copy);
        }
    }
    // ---- perf review 2026-10-08 G122 end ----

    // ---- perf review 2026-10-08 C001 begin ----
    @Nested
    class C001 {
        // C001 (coordinator, from verifier V9): a close handler of an input that re-enters the concatenation's iterator must not
        // make the retention fix clear the slot of the next, not yet closed, input.
        @Test
        void testConcat_closeHandlerReenteringIteratorStillClosesEveryInput() {
            final List<String> log = new ArrayList<>();
            final Iterator<?>[] outer = new Iterator<?>[1];
            final Seq<Integer, RuntimeException> a = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
                log.add("closeA");
                log.add("reenter=" + outer[0].hasNext());
            });
            final Seq<Integer, RuntimeException> b = Seq.<Integer, RuntimeException> of(2).onClose(() -> log.add("closeB"));
            final Seq<Integer, RuntimeException> c = Seq.<Integer, RuntimeException> of(3).onClose(() -> log.add("closeC"));
            final Seq<Integer, RuntimeException> s = Seq.concat(Arrays.asList(a, b, c)).onClose(() -> log.add("closeOuter"));
            final com.landawn.abacus.util.stream.Stream<Integer> st = s.stream();
            final Iterator<Integer> it = st.iterator();
            outer[0] = it;
            final List<Integer> got = new ArrayList<>();

            while (it.hasNext()) {
                got.add(it.next());
            }

            log.add("got=" + got);
            st.close();
            assertTrue(log.contains("closeA") && log.contains("closeB") && log.contains("closeC") && log.contains("closeOuter"), log.toString());
        }
    }
    // ---- perf review 2026-10-08 C001 end ----
}
