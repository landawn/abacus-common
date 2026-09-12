package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;

/**
 * Regression tests for the behaviour corrected in the 2026-08-30 review pass.
 *
 * <ul>
 *   <li>{@code Maps.getAs{Byte,Short,Int,Long}} range-check a {@code Number} the same way they already
 *       range-checked a {@code String},</li>
 *   <li>{@code Maps.getAsBoolean} and {@code Maps.getAs(map, key, Boolean.class)} agree,</li>
 *   <li>every {@code Maps} method that compares values uses {@code deepEquals},</li>
 *   <li>{@code Maps.replaceAll} delegates to {@link Map#replaceAll},</li>
 *   <li>{@code N.equals(float[]/double[], delta)} treats {@code null} the way the exact overload does,</li>
 *   <li>the {@code Linked}/{@code Sorted} {@code SetMultimap} factories order their value sets too,</li>
 *   <li>{@code Beans} introspection does not construct the class being introspected,</li>
 *   <li>{@code Beans.deepBeanToMap}/{@code beanToFlatMap} reject a reference cycle,</li>
 *   <li>{@code Iterables.SetView} is an unmodifiable view rather than a (falsely) immutable set.</li>
 * </ul>
 */
public class MultiClassRegressionATest extends TestBase {

    // ================================================================ Maps: numeric getAs* range checking

    @Test
    public void testGetAsNumeric_numberOutOfRangeIsRejectedLikeTheStringForm() {
        // Before the fix the Number branch narrowed with Number.byteValue()/shortValue()/intValue(), so an
        // out-of-range value wrapped silently while the very same value written as text threw.
        final Map<String, Object> asNumber = CommonUtil.asMap("v", (Object) 300);
        final Map<String, Object> asString = CommonUtil.asMap("v", (Object) "300");

        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsByte(asNumber, "v"));
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsByte(asString, "v"));
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsByteOrDefaultIfAbsent(asNumber, "v", (byte) 0));
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsByteOrDefaultIfAbsent(asString, "v", (byte) 0));

        final Map<String, Object> big = CommonUtil.asMap("v", (Object) 100_000);
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsShort(big, "v"));
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsShortOrDefaultIfAbsent(big, "v", (short) 0));

        final Map<String, Object> huge = CommonUtil.asMap("v", (Object) 3_000_000_000L);
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsInt(huge, "v"));
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getAsIntOrDefaultIfAbsent(huge, "v", 0));
    }

    @Test
    public void testGetAsNumeric_inRangeAndFractionalValuesStillWork() {
        final Map<String, Object> map = new HashMap<>();
        map.put("byte", 100);
        map.put("short", 1234);
        map.put("int", 42);
        map.put("long", 42L);
        map.put("fraction", 3.9d);
        map.put("text", "77");

        Assertions.assertEquals((byte) 100, Maps.getAsByte(map, "byte").get());
        Assertions.assertEquals((short) 1234, Maps.getAsShort(map, "short").get());
        Assertions.assertEquals(42, Maps.getAsInt(map, "int").get());
        Assertions.assertEquals(42L, Maps.getAsLong(map, "long").get());

        // a fractional Number is still truncated toward zero, exactly as Numbers.toXxx(Object) documents
        Assertions.assertEquals(3, Maps.getAsInt(map, "fraction").get());
        Assertions.assertEquals(3L, Maps.getAsLong(map, "fraction").get());
        Assertions.assertEquals((byte) 3, Maps.getAsByteOrDefaultIfAbsent(map, "fraction", (byte) 0));

        Assertions.assertEquals(77, Maps.getAsInt(map, "text").get());
        Assertions.assertEquals(77, Maps.getAsIntOrDefaultIfAbsent(map, "text", 0));

        // absent / null keys are untouched by the change
        Assertions.assertFalse(Maps.getAsInt(map, "missing").isPresent());
        Assertions.assertEquals(-1, Maps.getAsIntOrDefaultIfAbsent(map, "missing", -1));
    }

    @Test
    public void testGetByPathAsInt_rangeChecksNumbersToo() {
        final Map<String, Object> map = CommonUtil.asMap("user", (Object) CommonUtil.asMap("age", 3_000_000_000L));

        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getByPathAsInt(map, "user.age"));
        Assertions.assertThrows(ArithmeticException.class, () -> Maps.getByPathAsIntOrDefaultIfAbsent(map, "user.age", 0));

        final Map<String, Object> ok = CommonUtil.asMap("user", (Object) CommonUtil.asMap("age", 25));
        Assertions.assertEquals(25, Maps.getByPathAsInt(ok, "user.age").get());
        Assertions.assertEquals(25, Maps.getByPathAsIntOrDefaultIfAbsent(ok, "user.age", 0));
    }

    // ================================================================ Maps: boolean accessors agree

    @Test
    public void testGetAsBooleanAgreesWithGetAsBooleanClass() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("intOne", 1);
        map.put("intZero", 0);
        map.put("intMinusOne", -1);
        map.put("strOne", "1");
        map.put("strZero", "0");
        map.put("strTrue", "true");
        map.put("strFalse", "false");
        map.put("strJunk", "not-a-boolean");
        map.put("boolTrue", Boolean.TRUE);
        map.put("boolFalse", Boolean.FALSE);

        for (final String key : map.keySet()) {
            final OptionalBoolean viaBoolean = Maps.getAsBoolean(map, key);
            final Boolean viaClass = Maps.getAs(map, key, Boolean.class).orElseNull();

            Assertions.assertTrue(viaBoolean.isPresent(), key);
            Assertions.assertEquals(viaClass, viaBoolean.get(), "getAsBoolean disagrees with getAs(..., Boolean.class) for key " + key);
            Assertions.assertEquals(viaClass.booleanValue(), Maps.getAsBooleanOrDefaultIfAbsent(map, key, !viaClass), key);
        }

        // the specific values that used to disagree
        Assertions.assertTrue(Maps.getAsBoolean(map, "intOne").get());
        Assertions.assertTrue(Maps.getAsBoolean(map, "strOne").get());
        Assertions.assertTrue(Maps.getAsBoolean(map, "intMinusOne").get());
        Assertions.assertFalse(Maps.getAsBoolean(map, "intZero").get());
        Assertions.assertFalse(Maps.getAsBoolean(map, "strJunk").get());

        Assertions.assertFalse(Maps.getAsBoolean(map, "absent").isPresent());
        Assertions.assertTrue(Maps.getAsBooleanOrDefaultIfAbsent(map, "absent", true));
    }

    // ================================================================ Maps: one value equality everywhere

    @Test
    public void testMapsValueEqualityIsDeepEverywhere() {
        final Map<String, Object> m1 = new HashMap<>();
        m1.put("k", new int[] { 1, 2 });

        final Map<String, Object> m2 = new HashMap<>();
        m2.put("k", new int[] { 1, 2 });

        // set algebra already matched by content...
        Assertions.assertEquals(1, Maps.intersection(m1, m2).size());
        Assertions.assertEquals(0, Maps.difference(m1, m2).size());

        // ...and now the entry queries and mutators agree with it
        Assertions.assertTrue(Maps.containsEntry(m1, "k", m2.get("k")));
        Assertions.assertTrue(Maps.containsEntry(m1, CommonUtil.newEntry("k", m2.get("k"))));

        final Map<String, Object> toReplace = new HashMap<>(m1);
        Assertions.assertTrue(Maps.replace(toReplace, "k", m2.get("k"), "replaced"));
        Assertions.assertEquals("replaced", toReplace.get("k"));

        final Map<String, Object> toRemove = new HashMap<>(m1);
        Assertions.assertTrue(Maps.removeEntry(toRemove, "k", m2.get("k")));
        Assertions.assertTrue(toRemove.isEmpty());

        final Map<String, Object> toRemoveAll = new HashMap<>(m1);
        Assertions.assertTrue(Maps.removeEntries(toRemoveAll, m2));
        Assertions.assertTrue(toRemoveAll.isEmpty());

        // nested arrays too, and a genuinely different value is still rejected
        final Map<String, Object> n1 = CommonUtil.asMap("k", (Object) new int[][] { { 1 }, { 2 } });
        final Map<String, Object> n2 = CommonUtil.asMap("k", (Object) new int[][] { { 1 }, { 2 } });
        Assertions.assertTrue(Maps.containsEntry(n1, "k", n2.get("k")));
        Assertions.assertFalse(Maps.containsEntry(m1, "k", new int[] { 1, 3 }));
        Assertions.assertFalse(Maps.removeEntry(new HashMap<>(m1), "k", new int[] { 9 }));
    }

    @Test
    public void testContainsEntryStillHandlesNullAndMissingKeys() {
        final Map<String, Object> map = new HashMap<>();
        map.put("present", 1);
        map.put("nullValued", null);

        Assertions.assertTrue(Maps.containsEntry(map, "present", 1));
        Assertions.assertTrue(Maps.containsEntry(map, "nullValued", null));
        Assertions.assertFalse(Maps.containsEntry(map, "absent", null));
        Assertions.assertFalse(Maps.containsEntry(map, "present", 2));
        Assertions.assertFalse(Maps.containsEntry(null, "present", 1));
        Assertions.assertFalse(Maps.containsEntry(map, (Map.Entry<?, ?>) null));
    }

    // ================================================================ Maps: replaceAll delegates

    @Test
    public void testReplaceAllDelegatesToMapReplaceAll() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Maps.replaceAll(map, (k, v) -> v * 10);
        Assertions.assertEquals(CommonUtil.asMap("a", 10, "b", 20), map);

        // null / empty stay a no-op rather than an NPE
        Maps.replaceAll((Map<String, Integer>) null, (k, v) -> v);
        Maps.replaceAll(new HashMap<String, Integer>(), (k, v) -> v);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Maps.replaceAll(map, null));

        // the supplied map's own contract now shows through
        final Map<String, Integer> unmodifiable = Collections.unmodifiableMap(CommonUtil.asMap("a", 1));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> Maps.replaceAll(unmodifiable, (k, v) -> v));
    }

    @Test
    public void testReplaceAllFailsFastOnConcurrentStructuralModification() {
        // The hand-rolled entrySet/setValue loop could only notice this if Entry.setValue threw
        // IllegalStateException, which HashMap's entries never do. HashMap.replaceAll checks modCount.
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Assertions.assertThrows(java.util.ConcurrentModificationException.class, () -> Maps.replaceAll(map, (k, v) -> {
            map.put("added-" + k, v);
            return v;
        }));
    }

    // ================================================================ Maps: getAs targetType validation

    @Test
    public void testGetAsRejectsNullTargetTypeWithIllegalArgumentException() {
        final Map<String, Object> map = CommonUtil.asMap("k", (Object) "v");

        // was NullPointerException; the rest of the class reports a bad argument as IllegalArgumentException
        Assertions.assertThrows(IllegalArgumentException.class, () -> Maps.getAs(map, "k", (Class<String>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Maps.getAs(map, "k", (com.landawn.abacus.type.Type<String>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Maps.getAsOrDefaultIfAbsent(map, "k", "d", (Class<String>) null));
    }

    // ================================================================ N.equals(float[]/double[], delta)

    @Test
    public void testFuzzyArrayEqualsMatchesTheExactOverloadOnNull() {
        Assertions.assertEquals(CommonUtil.equals((float[]) null, new float[0]), CommonUtil.equals((float[]) null, new float[0], 0.1f));
        Assertions.assertEquals(CommonUtil.equals(new float[0], (float[]) null), CommonUtil.equals(new float[0], (float[]) null, 0.1f));
        Assertions.assertEquals(CommonUtil.equals((double[]) null, new double[0]), CommonUtil.equals((double[]) null, new double[0], 0.1d));
        Assertions.assertEquals(CommonUtil.equals(new double[0], (double[]) null), CommonUtil.equals(new double[0], (double[]) null, 0.1d));

        Assertions.assertFalse(CommonUtil.equals((float[]) null, new float[0], 0.1f));
        Assertions.assertFalse(CommonUtil.equals((double[]) null, new double[0], 0.1d));

        // the cases that were already right stay right
        Assertions.assertTrue(CommonUtil.equals((float[]) null, (float[]) null, 0.1f));
        Assertions.assertTrue(CommonUtil.equals(new float[0], new float[0], 0.1f));
        Assertions.assertTrue(CommonUtil.equals((double[]) null, (double[]) null, 0.1d));
        Assertions.assertTrue(CommonUtil.equals(new double[0], new double[0], 0.1d));
        Assertions.assertTrue(CommonUtil.equals(new float[] { 1.0f }, new float[] { 1.005f }, 0.01f));
        Assertions.assertFalse(CommonUtil.equals(new float[] { 1.0f }, new float[] { 1.5f }, 0.01f));
    }

    // ================================================================ notEmpty uses isEmpty()

    @Test
    public void testNotEmptyUsesIsEmptyForCollections() {
        // ConcurrentLinkedQueue.size() is O(n) while isEmpty() is O(1); the answers must be identical.
        final Collection<String> empty = new ConcurrentLinkedQueue<>();
        final Collection<String> filled = new ConcurrentLinkedQueue<>(Arrays.asList("a", "b"));

        Assertions.assertFalse(CommonUtil.notEmpty(empty));
        Assertions.assertTrue(CommonUtil.notEmpty(filled));
        Assertions.assertFalse(CommonUtil.notEmpty((Collection<String>) null));

        Assertions.assertTrue(CommonUtil.anyEmpty(empty, filled));
        Assertions.assertFalse(CommonUtil.anyEmpty(filled, filled));
        Assertions.assertTrue(CommonUtil.anyEmpty(filled, filled, empty));
        Assertions.assertTrue(CommonUtil.anyEmpty(filled, (Collection<String>) null));

        final Multiset<String> emptyMultiset = CommonUtil.newMultiset();
        Assertions.assertFalse(CommonUtil.notEmpty(emptyMultiset));
        Assertions.assertTrue(CommonUtil.notEmpty(CommonUtil.newMultiset(Arrays.asList("x"))));
    }

    // ================================================================ Linked/Sorted SetMultimap value sets

    @Test
    public void testLinkedSetMultimapKeepsValueInsertionOrder() {
        final SetMultimap<String, Integer> m = CommonUtil.newLinkedSetMultimap();
        m.put("k", 3);
        m.put("k", 1);
        m.put("k", 2);
        m.put("a", 9);

        Assertions.assertEquals(Arrays.asList(3, 1, 2), new ArrayList<>(m.get("k")));
        Assertions.assertEquals(Arrays.asList("k", "a"), new ArrayList<>(m.keySet()));

        final SetMultimap<String, Integer> sized = CommonUtil.newLinkedSetMultimap(16);
        sized.put("k", 3);
        sized.put("k", 1);
        Assertions.assertEquals(Arrays.asList(3, 1), new ArrayList<>(sized.get("k")));

        final SetMultimap<String, Integer> fromMap = CommonUtil.newLinkedSetMultimap(CommonUtil.asMap("k", 3));
        fromMap.put("k", 1);
        Assertions.assertEquals(Arrays.asList(3, 1), new ArrayList<>(fromMap.get("k")));
    }

    @Test
    public void testSortedSetMultimapSortsValuesToo() {
        final SetMultimap<String, Integer> m = CommonUtil.newSortedSetMultimap();
        m.put("b", 3);
        m.put("b", 1);
        m.put("b", 2);
        m.put("a", 5);

        Assertions.assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(m.get("b")));
        Assertions.assertEquals(Arrays.asList("a", "b"), new ArrayList<>(m.keySet()));

        final SetMultimap<String, Integer> fromMap = CommonUtil.newSortedSetMultimap(CommonUtil.asMap("b", 3));
        fromMap.put("b", 1);
        Assertions.assertEquals(Arrays.asList(1, 3), new ArrayList<>(fromMap.get("b")));
    }

    // ================================================================ lastEntry

    @Test
    public void testLastEntryIsCorrectAndWriteThroughForNavigableMaps() {
        final TreeMap<String, Integer> sorted = new TreeMap<>(CommonUtil.asMap("b", 2, "a", 1, "c", 3));
        final Map.Entry<String, Integer> last = CommonUtil.lastEntry(sorted).orElseThrow();

        Assertions.assertEquals("c", last.getKey());
        Assertions.assertEquals(3, last.getValue());

        // still the map's own entry, so setValue writes through
        last.setValue(30);
        Assertions.assertEquals(30, sorted.get("c"));

        final Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("x", 1);
        linked.put("y", 2);
        Assertions.assertEquals("y", CommonUtil.lastEntry(linked).orElseThrow().getKey());

        Assertions.assertFalse(CommonUtil.lastEntry(new TreeMap<String, Integer>()).isPresent());
        Assertions.assertFalse(CommonUtil.lastEntry((Map<String, Integer>) null).isPresent());
        Assertions.assertEquals("a", CommonUtil.lastEntry(CommonUtil.asMap("a", 1)).orElseThrow().getKey());
    }

    // ================================================================ ranged compare/mismatch validation order

    @Test
    public void testRangedCompareAndMismatchReportNegativeLenAsSuch() {
        final Integer[] a = { 1, 2, 3 };
        final Integer[] b = { 1, 2, 3 };
        final List<Integer> la = Arrays.asList(1, 2, 3);
        final List<Integer> lb = Arrays.asList(1, 2, 3);

        final org.junit.jupiter.api.function.Executable[] calls = { //
                () -> CommonUtil.compare(a, 0, b, 0, -1, Comparator.<Integer> naturalOrder()), //
                () -> CommonUtil.mismatch(a, 0, b, 0, -1, Fn.<Integer> identity()), //
                () -> CommonUtil.compare(la, 0, lb, 0, -1, Comparator.<Integer> naturalOrder()), //
                () -> CommonUtil.mismatch(la, 0, lb, 0, -1, Fn.<Integer> identity()) };

        for (final org.junit.jupiter.api.function.Executable call : calls) {
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, call);
            Assertions.assertTrue(ex.getMessage() != null && ex.getMessage().contains("len"), "expected the message to name 'len' but was: " + ex.getMessage());
        }

        // the valid path is unaffected
        Assertions.assertEquals(0, CommonUtil.compare(a, 0, b, 0, 3, Comparator.<Integer> naturalOrder()));
        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 0, b, 0, 3, Fn.<Integer> identity()));
    }

    // ================================================================ ClassUtil primitive classification

    @Test
    public void testPrimitiveClassificationMatrix() {
        Assertions.assertTrue(ClassUtil.isPrimitiveType(int.class));
        Assertions.assertTrue(ClassUtil.isPrimitiveType(boolean.class));
        Assertions.assertTrue(ClassUtil.isPrimitiveType(char.class));
        Assertions.assertFalse(ClassUtil.isPrimitiveType(Integer.class));
        Assertions.assertFalse(ClassUtil.isPrimitiveType(int[].class));
        Assertions.assertFalse(ClassUtil.isPrimitiveType(String.class));
        // void.class.isPrimitive() is true, but this method has never reported it as primitive
        Assertions.assertFalse(ClassUtil.isPrimitiveType(void.class));
        Assertions.assertFalse(ClassUtil.isPrimitiveType(Void.class));

        Assertions.assertTrue(ClassUtil.isPrimitiveWrapper(Integer.class));
        Assertions.assertTrue(ClassUtil.isPrimitiveWrapper(Character.class));
        Assertions.assertTrue(ClassUtil.isPrimitiveWrapper(Double.class));
        Assertions.assertFalse(ClassUtil.isPrimitiveWrapper(int.class));
        Assertions.assertFalse(ClassUtil.isPrimitiveWrapper(Integer[].class));
        Assertions.assertFalse(ClassUtil.isPrimitiveWrapper(String.class));
        Assertions.assertFalse(ClassUtil.isPrimitiveWrapper(Void.class));

        Assertions.assertTrue(ClassUtil.isPrimitiveArrayType(int[].class));
        Assertions.assertTrue(ClassUtil.isPrimitiveArrayType(boolean[].class));
        Assertions.assertFalse(ClassUtil.isPrimitiveArrayType(Integer[].class));
        Assertions.assertFalse(ClassUtil.isPrimitiveArrayType(int[][].class));
        Assertions.assertFalse(ClassUtil.isPrimitiveArrayType(int.class));

        Assertions.assertThrows(IllegalArgumentException.class, () -> ClassUtil.isPrimitiveType(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClassUtil.isPrimitiveWrapper(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ClassUtil.isPrimitiveArrayType(null));
    }

    @Test
    public void testFormatParameterizedTypeNameRequiresADotAsTheDuplicateSeparator() {
        // the documented, test-locked collapses still happen
        Assertions.assertEquals("Foo$Inner", ClassUtil.formatParameterizedTypeName("FooFoo$Inner"));
        Assertions.assertEquals("Foo$Inner", ClassUtil.formatParameterizedTypeName("Foo.Foo$Inner"));
        Assertions.assertEquals("com.x.Outer$Inner", ClassUtil.formatParameterizedTypeName("com.x.Outer.com.x.Outer$Inner"));
        Assertions.assertEquals("Outer$Inner", ClassUtil.formatParameterizedTypeName("Outer$Inner"));

        // ...but an arbitrary separator no longer counts as one
        Assertions.assertEquals("FooXFoo$Inner", ClassUtil.formatParameterizedTypeName("FooXFoo$Inner"));

        // multiple '$' levels are still handled, and unrelated names pass through untouched
        Assertions.assertEquals("Foo$A$B", ClassUtil.formatParameterizedTypeName("Foo.Foo$A$B"));
        Assertions.assertEquals("String", ClassUtil.formatParameterizedTypeName("class java.lang.String"));
        Assertions.assertEquals("java.util.ArrayList", ClassUtil.formatParameterizedTypeName("class java.util.ArrayList"));
    }

    // ================================================================ Beans: introspection has no side effects

    /** Counts how many times a bean is constructed, so introspection can be shown not to do it. */
    public static final class ConstructorCountingBean {
        static int constructorCalls = 0;

        private String name;

        public ConstructorCountingBean() {
            constructorCalls++;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public void testIntrospectionDoesNotConstructTheBean() {
        // Reading metadata used to run the target's no-arg constructor - which in real beans opens files,
        // registers listeners or starts threads. The instance is only needed for the JAXB getter probe.
        ConstructorCountingBean.constructorCalls = 0;

        Assertions.assertTrue(Beans.isBeanClass(ConstructorCountingBean.class));
        Assertions.assertEquals(Arrays.asList("name"), new ArrayList<>(Beans.getPropNameList(ConstructorCountingBean.class)));
        Assertions.assertNotNull(Beans.getPropGetters(ConstructorCountingBean.class));
        Assertions.assertNotNull(Beans.getPropSetters(ConstructorCountingBean.class));

        Assertions.assertEquals(0, ConstructorCountingBean.constructorCalls, "bean introspection constructed the class");

        // and the metadata it produced is still correct
        final ConstructorCountingBean bean = new ConstructorCountingBean();
        Beans.setPropValue(bean, "name", "abc");
        Assertions.assertEquals("abc", Beans.getPropValue(bean, "name"));
    }

    // ================================================================ Beans: cyclic graphs are rejected

    public static final class CyclicNode {
        private String name;

        private CyclicNode peer;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public CyclicNode getPeer() {
            return peer;
        }

        public void setPeer(final CyclicNode peer) {
            this.peer = peer;
        }
    }

    @Test
    public void testDeepBeanToMapAndBeanToFlatMapRejectCycles() {
        final CyclicNode a = new CyclicNode();
        final CyclicNode b = new CyclicNode();
        a.setName("a");
        b.setName("b");
        a.setPeer(b);
        b.setPeer(a);

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(a));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(a));
        // self-reference is the same case
        final CyclicNode self = new CyclicNode();
        self.setName("self");
        self.setPeer(self);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(self));

        // a shared-but-acyclic child is NOT a cycle and must still convert
        final CyclicNode shared = new CyclicNode();
        shared.setName("shared");
        final CyclicNode root = new CyclicNode();
        root.setName("root");
        root.setPeer(shared);

        final Map<String, Object> deep = Beans.deepBeanToMap(root);
        Assertions.assertEquals("root", deep.get("name"));
        Assertions.assertEquals("shared", ((Map<?, ?>) deep.get("peer")).get("name"));

        final Map<String, Object> flat = Beans.beanToFlatMap(root);
        Assertions.assertEquals("root", flat.get("name"));
        Assertions.assertEquals("shared", flat.get("peer.name"));

        // the thread-local cycle tracker must be clean again after a rejected conversion
        Assertions.assertEquals("root", Beans.deepBeanToMap(root).get("name"));
    }

    // ================================================================ Iterators

    @Test
    public void testZipWithDefaultsThrowsNoSuchElementWhenExhausted() {
        final ObjIterator<String> two = Iterators.zip(Arrays.asList("a").iterator(), Arrays.asList(1, 2).iterator(), "x", 0, (s, i) -> s + i);

        Assertions.assertEquals("a1", two.next());
        Assertions.assertEquals("x2", two.next());
        Assertions.assertFalse(two.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, two::next);

        final ObjIterator<String> three = Iterators.zip(Arrays.asList("a").iterator(), Arrays.asList(1).iterator(), Arrays.asList(true).iterator(), "x", 0,
                false, (s, i, b) -> s + i + b);

        Assertions.assertEquals("a1true", three.next());
        Assertions.assertFalse(three.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, three::next);
    }

    @Test
    public void testTakeWhileInclusiveStillEmitsTheFirstNonMatchingElement() {
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), Iterators.takeWhileInclusive(Arrays.asList(1, 2, 3, 4, 5, 2).iterator(), n -> n < 4).toList());
        // an immediately-failing predicate still yields exactly one element
        Assertions.assertEquals(Arrays.asList(9), Iterators.takeWhileInclusive(Arrays.asList(9, 1).iterator(), n -> n < 4).toList());
        Assertions.assertEquals(new ArrayList<>(), Iterators.takeWhileInclusive(new ArrayList<Integer>().iterator(), n -> true).toList());
    }

    // ================================================================ Iterables.SetView

    @Test
    public void testSetViewIsAnUnmodifiableViewNotAnImmutableValue() {
        final Set<String> s1 = new LinkedHashSet<>(Arrays.asList("a"));
        final Set<String> s2 = new LinkedHashSet<>(Arrays.asList("b"));
        final Iterables.SetView<String> view = Iterables.union(s1, s2);

        // it reads through, so it is not an immutable value and must not claim to be one
        Assertions.assertFalse(ImmutableSet.class.isInstance(view), "SetView must not be an ImmutableSet");
        Assertions.assertFalse(view instanceof Immutable, "SetView must not carry the Immutable marker");

        Assertions.assertEquals(Arrays.asList("a", "b"), new ArrayList<>(view));
        s1.add("z");
        Assertions.assertEquals(Arrays.asList("a", "z", "b"), new ArrayList<>(view));
        Assertions.assertEquals(3, view.size());
        Assertions.assertTrue(view.contains("z"));

        // ...and because it does not carry the marker, N.unmodifiableSet wraps it instead of passing it through
        final Set<String> wrapped = CommonUtil.unmodifiableSet(view);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> wrapped.add("q"));

        // every mutator is still rejected, including the ones AbstractSet would otherwise implement via the iterator
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.add("q"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.remove("not-present"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.addAll(Arrays.asList("q")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.removeAll(Arrays.asList("a")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.retainAll(Arrays.asList("a")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.removeIf(x -> true));
        Assertions.assertThrows(UnsupportedOperationException.class, view::clear);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.iterator().remove());

        // copyInto still takes a real, independent snapshot
        final Set<String> snapshot = view.copyInto(new LinkedHashSet<>());
        s1.add("later");
        Assertions.assertEquals(Arrays.asList("a", "z", "b"), new ArrayList<>(snapshot));
    }

    @Test
    public void testAllSetViewFactoriesStillBehave() {
        final Set<Integer> s1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> s2 = new LinkedHashSet<>(Arrays.asList(3, 4));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), new ArrayList<>(Iterables.union(s1, s2)));
        Assertions.assertEquals(Arrays.asList(3), new ArrayList<>(Iterables.intersection(s1, s2)));
        Assertions.assertEquals(Arrays.asList(1, 2), new ArrayList<>(Iterables.difference(s1, s2)));
        Assertions.assertEquals(Arrays.asList(1, 2, 4), new ArrayList<>(Iterables.symmetricDifference(s1, s2)));

        Assertions.assertTrue(Iterables.union(null, null).isEmpty());
        Assertions.assertTrue(Iterables.intersection(s1, null).isEmpty());
        Assertions.assertEquals(3, Iterables.difference(s1, null).size());
        Assertions.assertEquals(3, Iterables.symmetricDifference(s1, null).size());

        // equals/hashCode still work through AbstractSet
        Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4)), Iterables.union(s1, s2));
        Assertions.assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4)).hashCode(), Iterables.union(s1, s2).hashCode());
    }

    // ================================================================ untouched behaviour that the fixes brushed against

    @Test
    public void testStringEqualsAndContainsSameElementsUnchanged() {
        Assertions.assertTrue(CommonUtil.equals("test", "test"));
        Assertions.assertFalse(CommonUtil.equals("test", "Test"));
        Assertions.assertFalse(CommonUtil.equals("test", "testing"));
        Assertions.assertTrue(CommonUtil.equals((String) null, (String) null));
        Assertions.assertFalse(CommonUtil.equals("test", (String) null));
        Assertions.assertFalse(CommonUtil.equals((String) null, "test"));

        Assertions.assertTrue(CommonUtil.containsSameElements(new Object[] { "a", "b", "a" }, new Object[] { "a", "a", "b" }));
        Assertions.assertFalse(CommonUtil.containsSameElements(new Object[] { "a", "b" }, new Object[] { "a", "a" }));
        Assertions.assertTrue(CommonUtil.containsSameElements((Object[]) null, new Object[0]));
    }

    @Test
    public void testToMapVarargsUnchanged() {
        final Map<String, Integer> map = CommonUtil.toMap("a", 1, "b", 2, "c", 3);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(3, map.get("c"));

        final Map<String, Integer> linked = CommonUtil.toLinkedHashMap("a", 1, "b", 2, "c", 3);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(linked.keySet()));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.toMap("a", 1, (Object) "b"));
    }

    @Test
    public void testIteratorForEachStillReportsWorkerFailures() {
        // the error slot moved from Holder to AtomicReference; the propagation contract is unchanged
        final Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4).iterator();

        Assertions.assertThrows(RuntimeException.class, () -> Iterators.forEach(iter, Iterators.IterateOptions.builder().processThreads(2).build(), e -> {
            throw new IllegalStateException("boom");
        }));

        final List<Integer> collected = Collections.synchronizedList(new ArrayList<>());
        Iterators.forEach(Arrays.asList(1, 2, 3, 4).iterator(), Iterators.IterateOptions.builder().processThreads(2).build(), collected::add);
        Assertions.assertEquals(4, collected.size());
    }
}
