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

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableBooleanArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableByteArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableCharArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDeque;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDoubleArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableFloatArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableIntArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableLongArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposablePair;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableShortArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableTriple;
import com.landawn.abacus.util.NoCachingNoUpdating.Timed;

public class NoCachingNoUpdatingTest extends NoCachingNoUpdatingTestSupport {

    @Test
    public void testDisposableObjArray() {
        DisposableObjArray created = DisposableObjArray.create(5);
        assertEquals(5, created.length());
        assertEquals(0, DisposableObjArray.create(0).length());
        assertThrows(IllegalArgumentException.class, () -> DisposableObjArray.create(-1));
        assertThrows(UnsupportedOperationException.class, () -> DisposableObjArray.create(String.class, 5));

        Object[] data = { 1, "hello", true };
        DisposableObjArray wrapped = DisposableObjArray.wrap(data);
        assertEquals(3, wrapped.length());
        assertEquals(1, wrapped.get(0));
        assertEquals("hello", wrapped.get(1));
        assertEquals(true, wrapped.get(2));
        assertEquals(0, DisposableObjArray.wrap(new Object[0]).length());
    }

    @Test
    public void testDisposablePair() throws Exception {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        assertEquals("left", disposable.left());
        assertEquals(123, disposable.right());
        Pair<String, Integer> copy = disposable.copy();
        assertEquals("left", copy.left());
        assertEquals(123, copy.right());
        assertEquals("left=123", disposable.apply((l, r) -> l + "=" + r));
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept((l, r) -> ref.set(l + "=" + r));
        assertEquals("left=123", ref.get());
        assertEquals("[left, 123]", disposable.toString());

        assertThrows(IllegalArgumentException.class, () -> DisposablePair.wrap(null));

        DisposablePair<String, Integer> nullLeft = DisposablePair.wrap(Pair.of(null, 100));
        assertNull(nullLeft.left());
        assertEquals(100, nullLeft.right());
        DisposablePair<String, Integer> nullRight = DisposablePair.wrap(Pair.of("left", null));
        assertEquals("left", nullRight.left());
        assertNull(nullRight.right());
        DisposablePair<String, Integer> bothNull = DisposablePair.wrap(Pair.of(null, null));
        assertNull(bothNull.left());
        assertNull(bothNull.right());
    }

    @Test
    public void testDisposableTriple() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        assertEquals("left", disposable.left());
        assertEquals(123, disposable.middle());
        assertEquals(true, disposable.right());
        Triple<String, Integer, Boolean> copy = disposable.copy();
        assertEquals("left", copy.left());
        assertEquals(123, copy.middle());
        assertEquals(true, copy.right());
        assertEquals("left=123=true", disposable.apply((l, m, r) -> l + "=" + m + "=" + r));
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept((l, m, r) -> ref.set(l + "=" + m + "=" + r));
        assertEquals("left=123=true", ref.get());
        assertEquals("[left, 123, true]", disposable.toString());

        assertThrows(IllegalArgumentException.class, () -> DisposableTriple.wrap(null));

        DisposableTriple<String, Integer, Boolean> nullLeft = DisposableTriple.wrap(Triple.of(null, 100, true));
        assertNull(nullLeft.left());
        assertEquals(100, nullLeft.middle());
        assertEquals(true, nullLeft.right());
        DisposableTriple<String, Integer, Boolean> nullMiddle = DisposableTriple.wrap(Triple.of("left", null, true));
        assertEquals("left", nullMiddle.left());
        assertNull(nullMiddle.middle());
        DisposableTriple<String, Integer, Boolean> nullRight = DisposableTriple.wrap(Triple.of("left", 100, null));
        assertNull(nullRight.right());
        DisposableTriple<String, Integer, Boolean> allNull = DisposableTriple.wrap(Triple.of(null, null, null));
        assertNull(allNull.left());
        assertNull(allNull.middle());
        assertNull(allNull.right());
    }

    @Test
    public void testDisposableArray() throws Exception {
        DisposableArray<Integer> created = DisposableArray.create(Integer.class, 5);
        assertEquals(5, created.length());

        String[] source = disposableArrayFixtureSource();
        DisposableArray<String> array = DisposableArray.wrap(source);
        assertEquals(source.length, array.length());
        assertEquals("a", array.get(0));
        assertEquals("b", array.get(1));
        assertEquals("c", array.get(2));

        String[] target = new String[3];
        array.toArray(target);
        assertArrayEquals(source, target);

        String[] copy = array.copy();
        assertNotSame(source, copy);
        assertArrayEquals(source, copy);
        assertEquals(Arrays.asList(source), array.toList());
        assertEquals(new HashSet<>(Arrays.asList(source)), array.toSet());
        assertEquals(Arrays.asList(source), array.toCollection(ArrayList::new));

        List<String> foreach = new ArrayList<>();
        array.foreach(foreach::add);
        assertEquals(Arrays.asList(source), foreach);
        Integer length = array.apply(arr -> arr.length);
        assertEquals(source.length, length);
        List<String[]> captured = new ArrayList<>();
        array.accept(captured::add);
        assertSame(source, captured.get(0));

        assertEquals("a,b,c", array.join(","));
        assertEquals("[a,b,c]", array.join(",", "[", "]"));
        List<String> iterated = new ArrayList<>();
        array.iterator().forEachRemaining(iterated::add);
        assertEquals(Arrays.asList(source), iterated);
        assertEquals(Arrays.toString(source), array.toString());
    }

    @Test
    public void testDisposableArray_EdgeCase() {
        String[] arr = { "a\"b", "c,d", "e\nf", "g\th" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals("a\"b,c,d,e\nf,g\th", array.join(","));
        assertEquals("[a\"b|c,d|e\nf|g\th]", array.join("|", "[", "]"));
        assertEquals("a\"bc,de\nfg\th", array.join(""));
        assertEquals("a,null,b", DisposableArray.wrap(new String[] { "a", null, "b" }).join(","));

        Integer[] ints = { 1, 2, 3, 2, 1 };
        DisposableArray<Integer> intArray = DisposableArray.wrap(ints);
        LinkedHashSet<Integer> linkedSet = intArray.toCollection(LinkedHashSet::new);
        Iterator<Integer> iter = linkedSet.iterator();
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        TreeSet<Integer> treeSet = intArray.toCollection(IntFunctions.ofTreeSet());
        assertEquals(1, treeSet.first());
        assertEquals(3, treeSet.last());
        assertEquals(5, intArray.toCollection(size -> new ArrayList<>(size * 2)).size());
    }

    @Test
    public void testPrimitiveArrayStats() {
        assertArrayEquals(new Boolean[] { true, false, true }, DisposableBooleanArray.wrap(new boolean[] { true, false, true }).box());

        DisposableCharArray chars = DisposableCharArray.wrap(new char[] { 'a', 'b', 'c' });
        assertEquals(97 + 98 + 99, chars.sum());
        assertEquals((97 + 98 + 99) / 3.0, chars.average());
        assertEquals('a', chars.min());
        assertEquals('c', chars.max());

        assertEquals(Byte.MIN_VALUE, DisposableByteArray.wrap(new byte[] { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE }).min());
        assertEquals(Byte.MAX_VALUE, DisposableByteArray.wrap(new byte[] { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE }).max());
        assertEquals(Short.MIN_VALUE, DisposableShortArray.wrap(new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE }).min());
        assertEquals(Short.MAX_VALUE, DisposableShortArray.wrap(new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE }).max());
        assertEquals(Integer.MIN_VALUE, DisposableIntArray.wrap(new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE }).min());
        assertEquals(Integer.MAX_VALUE, DisposableIntArray.wrap(new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE }).max());
        assertEquals(Long.MIN_VALUE, DisposableLongArray.wrap(new long[] { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE }).min());
        assertEquals(Long.MAX_VALUE, DisposableLongArray.wrap(new long[] { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE }).max());
    }

    @Test
    public void testDisposableDequeAndEntry() throws Exception {
        DisposableDeque<Integer> deque = disposableDequeFixture();
        assertEquals(1, deque.getFirst());
        assertEquals(3, deque.getLast());
        assertThrows(NoSuchElementException.class, DisposableDeque.wrap(new ArrayDeque<Integer>())::getFirst);

        ArrayDeque<String> backing = new ArrayDeque<>(List.of("a"));
        DisposableDeque<String> live = DisposableDeque.wrap(backing);
        String snapshot = live.toString();
        backing.clear();
        backing.add("b");
        assertEquals("[a]", snapshot);
        assertEquals("[b]", live.toString());

        DisposableEntry<String, Integer> entry = disposableEntryFixture();
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(456));
        assertEquals("key:123", entry.apply((k, v) -> k + ":" + v));
        List<String> accepted = new ArrayList<>();
        entry.accept((k, v) -> accepted.add(k + "=" + v));
        assertEquals(List.of("key=123"), accepted);
    }

    @Test
    public void testTimed() {
        Timed<String> timed = timedFixture();
        assertEquals("test-value", timed.value());
        assertEquals(12345L, timed.timestamp());
        Timed<String> same = Timed.of("test-value", 12345L);
        assertEquals(timed.hashCode(), same.hashCode());
        assertEquals(timed, same);
        assertFalse(timed.equals(Timed.of("test-value", 12346L)));
        assertFalse(timed.equals(Timed.of("other", 12345L)));
        assertFalse(timed.equals(null));
        assertFalse(timed.equals(new Object()));
    }

    @Test
    public void testToCollectionNullSupplierAndNullResultForEveryView() {
        DisposableArray<Object> objects = DisposableArray.wrap(new Object[0]);
        DisposableBooleanArray booleans = DisposableBooleanArray.wrap(new boolean[0]);
        DisposableCharArray chars = DisposableCharArray.wrap(new char[0]);
        DisposableByteArray bytes = DisposableByteArray.wrap(new byte[0]);
        DisposableShortArray shorts = DisposableShortArray.wrap(new short[0]);
        DisposableIntArray ints = DisposableIntArray.wrap(new int[0]);
        DisposableLongArray longs = DisposableLongArray.wrap(new long[0]);
        DisposableFloatArray floats = DisposableFloatArray.wrap(new float[0]);
        DisposableDoubleArray doubles = DisposableDoubleArray.wrap(new double[0]);
        DisposableDeque<Object> deque = DisposableDeque.wrap(new ArrayDeque<>());

        assertThrows(IllegalArgumentException.class, () -> objects.<List<Object>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> booleans.<List<Boolean>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> chars.<List<Character>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> bytes.<List<Byte>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> shorts.<List<Short>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> ints.<List<Integer>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> longs.<List<Long>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> floats.<List<Float>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> doubles.<List<Double>> toCollection(null));
        assertThrows(IllegalArgumentException.class, () -> deque.<List<Object>> toCollection(null));

        assertThrows(IllegalArgumentException.class, () -> objects.<List<Object>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> booleans.<List<Boolean>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> chars.<List<Character>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> bytes.<List<Byte>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> shorts.<List<Short>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> ints.<List<Integer>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> longs.<List<Long>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> floats.<List<Float>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> doubles.<List<Double>> toCollection(size -> null));
        assertThrows(IllegalArgumentException.class, () -> deque.<List<Object>> toCollection(size -> null));
    }

    @Test
    public void testEmptyForeachDoesNotEvaluateNullCallbackForEveryView() {
        assertThrows(IllegalArgumentException.class, () -> DisposableArray.wrap(new Object[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableBooleanArray.wrap(new boolean[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableCharArray.wrap(new char[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableByteArray.wrap(new byte[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableShortArray.wrap(new short[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableIntArray.wrap(new int[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableLongArray.wrap(new long[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableFloatArray.wrap(new float[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableDoubleArray.wrap(new double[0]).foreach(null));
        assertThrows(IllegalArgumentException.class, () -> DisposableDeque.wrap(new ArrayDeque<>()).foreach(null));
    }

    @Test
    public void testApplyAndAcceptNullCallbacksAndNullFunctionResults() throws Exception {
        DisposableArray<Object> objects = DisposableArray.wrap(new Object[0]);
        DisposableBooleanArray booleans = DisposableBooleanArray.wrap(new boolean[0]);
        DisposableCharArray chars = DisposableCharArray.wrap(new char[0]);
        DisposableByteArray bytes = DisposableByteArray.wrap(new byte[0]);
        DisposableShortArray shorts = DisposableShortArray.wrap(new short[0]);
        DisposableIntArray ints = DisposableIntArray.wrap(new int[0]);
        DisposableLongArray longs = DisposableLongArray.wrap(new long[0]);
        DisposableFloatArray floats = DisposableFloatArray.wrap(new float[0]);
        DisposableDoubleArray doubles = DisposableDoubleArray.wrap(new double[0]);
        DisposableDeque<Object> deque = DisposableDeque.wrap(new ArrayDeque<>());

        assertThrows(IllegalArgumentException.class, () -> objects.apply(null));
        assertThrows(IllegalArgumentException.class, () -> booleans.apply(null));
        assertThrows(IllegalArgumentException.class, () -> chars.apply(null));
        assertThrows(IllegalArgumentException.class, () -> bytes.apply(null));
        assertThrows(IllegalArgumentException.class, () -> shorts.apply(null));
        assertThrows(IllegalArgumentException.class, () -> ints.apply(null));
        assertThrows(IllegalArgumentException.class, () -> longs.apply(null));
        assertThrows(IllegalArgumentException.class, () -> floats.apply(null));
        assertThrows(IllegalArgumentException.class, () -> doubles.apply(null));
        assertThrows(IllegalArgumentException.class, () -> deque.apply(null));

        assertThrows(IllegalArgumentException.class, () -> objects.accept(null));
        assertThrows(IllegalArgumentException.class, () -> booleans.accept(null));
        assertThrows(IllegalArgumentException.class, () -> chars.accept(null));
        assertThrows(IllegalArgumentException.class, () -> bytes.accept(null));
        assertThrows(IllegalArgumentException.class, () -> shorts.accept(null));
        assertThrows(IllegalArgumentException.class, () -> ints.accept(null));
        assertThrows(IllegalArgumentException.class, () -> longs.accept(null));
        assertThrows(IllegalArgumentException.class, () -> floats.accept(null));
        assertThrows(IllegalArgumentException.class, () -> doubles.accept(null));
        assertThrows(IllegalArgumentException.class, () -> deque.accept(null));

        assertNull(objects.apply(array -> null));
        assertNull(booleans.apply(array -> null));
        assertNull(deque.apply(value -> null));

        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(new AbstractMap.SimpleEntry<>("key", 1));
        assertThrows(IllegalArgumentException.class, () -> entry.apply((Throwables.Function<DisposableEntry<String, Integer>, Object, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> entry.apply((Throwables.BiFunction<String, Integer, Object, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> entry.accept((Throwables.Consumer<DisposableEntry<String, Integer>, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> entry.accept((Throwables.BiConsumer<String, Integer, RuntimeException>) null));
        assertNull(entry.apply((key, value) -> null));

        DisposablePair<String, Integer> pair = DisposablePair.wrap(Pair.of("left", 1));
        assertThrows(IllegalArgumentException.class, () -> pair.apply(null));
        assertThrows(IllegalArgumentException.class, () -> pair.accept(null));
        assertNull(pair.apply((left, right) -> null));

        DisposableTriple<String, Integer, Boolean> triple = DisposableTriple.wrap(Triple.of("left", 1, true));
        assertThrows(IllegalArgumentException.class, () -> triple.apply(null));
        assertThrows(IllegalArgumentException.class, () -> triple.accept(null));
        assertNull(triple.apply((left, middle, right) -> null));
    }

    @Test
    public void testWrappedValuesAreLiveViewsAndConversionsAreIndependentSnapshots() {
        String[] source = { "a", "b" };
        DisposableArray<String> array = DisposableArray.wrap(source);
        String[] arrayCopy = array.copy();
        List<String> listCopy = array.toList();
        source[0] = "updated";
        assertEquals("updated", array.get(0));
        assertEquals("a", arrayCopy[0]);
        assertEquals("a", listCopy.get(0));
        listCopy.set(1, "list-only");
        assertEquals("b", source[1]);

        Deque<String> sourceDeque = new ArrayDeque<>(Arrays.asList("first", "last"));
        DisposableDeque<String> deque = DisposableDeque.wrap(sourceDeque);
        List<String> dequeCopy = deque.toList();
        sourceDeque.removeFirst();
        assertEquals("last", deque.getFirst());
        assertEquals(Arrays.asList("first", "last"), dequeCopy);

        AbstractMap.SimpleEntry<String, Integer> sourceEntry = new AbstractMap.SimpleEntry<>("key", 1);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(sourceEntry);
        Map.Entry<String, Integer> entryCopy = entry.copy();
        sourceEntry.setValue(2);
        assertEquals(2, entry.getValue());
        assertEquals(1, entryCopy.getValue());

        Pair<String, Integer> sourcePair = Pair.of("left", 1);
        DisposablePair<String, Integer> pair = DisposablePair.wrap(sourcePair);
        Pair<String, Integer> pairCopy = pair.copy();
        sourcePair.setRight(2);
        assertEquals(2, pair.right());
        assertEquals(1, pairCopy.right());

        Triple<String, Integer, Boolean> sourceTriple = Triple.of("left", 1, true);
        DisposableTriple<String, Integer, Boolean> triple = DisposableTriple.wrap(sourceTriple);
        Triple<String, Integer, Boolean> tripleCopy = triple.copy();
        sourceTriple.setMiddle(2);
        assertEquals(2, triple.middle());
        assertEquals(1, tripleCopy.middle());
    }

    @Test
    public void testSmallIntegralSumsDetectOverflow() {
        char[] chars = new char[Integer.MAX_VALUE / Character.MAX_VALUE + 1];
        Arrays.fill(chars, Character.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> DisposableCharArray.wrap(chars).sum());

        short[] shorts = new short[Integer.MAX_VALUE / Short.MAX_VALUE + 1];
        Arrays.fill(shorts, Short.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> DisposableShortArray.wrap(shorts).sum());

        byte[] bytes = new byte[Integer.MAX_VALUE / Byte.MAX_VALUE + 1];
        Arrays.fill(bytes, Byte.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> DisposableByteArray.wrap(bytes).sum());
    }

    @Test
    public void testNumericEmptyNaNInfinitySignedZeroAndLongOverflowContracts() {
        assertThrows(IllegalArgumentException.class, () -> DisposableCharArray.wrap(new char[0]).min());
        assertThrows(IllegalArgumentException.class, () -> DisposableByteArray.wrap(new byte[0]).max());
        assertThrows(IllegalArgumentException.class, () -> DisposableShortArray.wrap(new short[0]).min());
        assertThrows(IllegalArgumentException.class, () -> DisposableIntArray.wrap(new int[0]).max());
        assertThrows(IllegalArgumentException.class, () -> DisposableLongArray.wrap(new long[0]).min());
        assertThrows(IllegalArgumentException.class, () -> DisposableFloatArray.wrap(new float[0]).max());
        assertThrows(IllegalArgumentException.class, () -> DisposableDoubleArray.wrap(new double[0]).min());

        assertEquals(Long.MIN_VALUE, DisposableLongArray.wrap(new long[] { Long.MAX_VALUE, 1 }).sum());
        assertEquals(Long.MAX_VALUE, DisposableLongArray.wrap(new long[] { Long.MAX_VALUE, Long.MAX_VALUE }).average());

        DisposableFloatArray floatNaN = DisposableFloatArray.wrap(new float[] { 1, Float.NaN });
        assertTrue(Float.isNaN(floatNaN.sum()));
        assertTrue(Double.isNaN(floatNaN.average()));
        assertTrue(Float.isNaN(floatNaN.min()));
        assertTrue(Float.isNaN(floatNaN.max()));

        DisposableDoubleArray doubleInfinity = DisposableDoubleArray.wrap(new double[] { Double.POSITIVE_INFINITY, 1 });
        assertEquals(Double.POSITIVE_INFINITY, doubleInfinity.sum());
        assertEquals(Double.POSITIVE_INFINITY, doubleInfinity.average());

        DisposableDoubleArray zeros = DisposableDoubleArray.wrap(new double[] { 0.0d, -0.0d });
        assertEquals(Double.doubleToLongBits(-0.0d), Double.doubleToLongBits(zeros.min()));
        assertEquals(Double.doubleToLongBits(0.0d), Double.doubleToLongBits(zeros.max()));
    }

    @Test
    public void testDisposableArrayToArray() {
        String[] backing = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(backing);
        String[] aliased = array.toArray(backing);
        assertNotSame(backing, aliased);
        assertArrayEquals(new String[] { "a", "b", "c" }, aliased);
        assertArrayEquals(new String[] { "a", "b", "c" }, backing);

        String[] exact = new String[3];
        assertSame(exact, array.toArray(exact));
        assertArrayEquals(new String[] { "a", "b", "c" }, exact);

        String[] scratch = { "p", "q", "r", "s", "t" };
        assertSame(scratch, array.toArray(scratch));
        assertArrayEquals(new String[] { "a", "b", "c", null, "t" }, scratch);

        String[] exactFilled = { "p", "q", "r" };
        assertSame(exactFilled, array.toArray(exactFilled));
        assertArrayEquals(new String[] { "a", "b", "c" }, exactFilled);

        DisposableArray<Object> mixed = DisposableArray.wrap(new Object[] { "a", Integer.valueOf(1) });
        String[] target = { "u", "v", "w", "x" };
        assertThrows(ArrayStoreException.class, () -> mixed.toArray(target));
        assertArrayEquals(new String[] { "a", "v", null, "x" }, target);

        String[] ok = { "u", "v", "w", "x" };
        assertSame(ok, DisposableArray.wrap(new Object[] { "a", "b" }).toArray(ok));
        assertArrayEquals(new String[] { "a", "b", null, "x" }, ok);

        assertThrows(IllegalArgumentException.class, () -> DisposableArray.wrap(new String[] { "a" }).toArray(null));
        assertThrows(NullPointerException.class, () -> DisposableDeque.wrap(new ArrayDeque<String>()).toArray((String[]) null));
    }

    @Test
    public void testTimedCarriesTheDisposableMarkerAnnotations() throws Exception {
        for (String name : new String[] { "NoCachingNoUpdating$Timed", "NoCachingNoUpdating$DisposableArray" }) {
            try (java.io.InputStream in = NoCachingNoUpdating.class.getResourceAsStream(name + ".class")) {
                assertNotNull(in, name);
                String bytes = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
                assertTrue(bytes.contains("Lcom/landawn/abacus/annotation/Beta;"), name);
                assertTrue(bytes.contains("Lcom/landawn/abacus/annotation/SequentialOnly;"), name);
                assertTrue(bytes.contains("Lcom/landawn/abacus/annotation/Stateful;"), name);
            }
        }
        try (java.io.InputStream in = com.landawn.abacus.util.Timed.class.getResourceAsStream("Timed.class")) {
            String bytes = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
            assertTrue(bytes.contains("Lcom/landawn/abacus/annotation/Immutable;"));
            assertFalse(bytes.contains("Lcom/landawn/abacus/annotation/Stateful;"));
        }
    }

    @Test
    public void testTimedIsStrandedInAHashSetWhenTheProducerReusesIt() {
        class ReusableTimed<T> extends Timed<T> {
            ReusableTimed(final T value, final long timestamp) {
                super(value, timestamp);
            }

            void update(final T value, final long timestamp) {
                set(value, timestamp);
            }
        }

        ReusableTimed<String> reusable = new ReusableTimed<>("a", 1L);
        Set<Timed<String>> seen = new HashSet<>();
        seen.add(reusable);
        assertTrue(seen.contains(reusable.copy()));

        int hashBefore = reusable.hashCode();
        reusable.update("b", 2L);
        assertTrue(hashBefore != reusable.hashCode());
        assertFalse(seen.contains(reusable));
        assertFalse(seen.contains(Timed.of("a", 1L)));
        assertEquals(1, seen.size());

        ReusableTimed<String> reusable2 = new ReusableTimed<>("a", 1L);
        Set<Timed<String>> snapshots = new HashSet<>();
        snapshots.add(reusable2.copy());
        reusable2.update("b", 2L);
        assertTrue(snapshots.contains(Timed.of("a", 1L)));
    }

    @Test
    public void testDisposableObjArrayAddsNoBehaviourOfItsOwn() {
        for (java.lang.reflect.Method m : DisposableObjArray.class.getDeclaredMethods()) {
            if (m.isSynthetic()) {
                continue;
            }
            assertTrue(java.lang.reflect.Modifier.isStatic(m.getModifiers()), m.toString());
        }
        String[] scratch = { "p", "q", "r" };
        assertSame(scratch, DisposableObjArray.wrap(new Object[] { "a", "b" }).toArray(scratch));
        assertArrayEquals(new String[] { "a", "b", null }, scratch);
    }

    @Test
    public void testProtectedWrapperConstructorsRejectANullBackingObject() {
        assertEquals("'a' cannot be null", assertThrows(IllegalArgumentException.class, () -> new DisposableLongArray((long[]) null)).getMessage());
        assertEquals("'a' cannot be null", assertThrows(IllegalArgumentException.class, () -> new DisposableFloatArray((float[]) null)).getMessage());
        assertEquals("'a' cannot be null", assertThrows(IllegalArgumentException.class, () -> new DisposableDoubleArray((double[]) null)).getMessage());
        assertEquals("'deque' cannot be null", assertThrows(IllegalArgumentException.class, () -> new DisposableDeque<String>(null)).getMessage());

        assertEquals("'a' cannot be null", assertThrows(IllegalArgumentException.class, () -> new DisposableArray<String>(null)).getMessage());
        assertEquals("'a' cannot be null", assertThrows(IllegalArgumentException.class, () -> new DisposableIntArray((int[]) null)).getMessage());
    }

    @Test
    public void testToCollectionDistinguishesANullSupplierFromASupplierThatReturnsNull() {
        final DisposableArray<Object> objects = DisposableArray.wrap(new Object[0]);
        final DisposableBooleanArray booleans = DisposableBooleanArray.wrap(new boolean[0]);
        final DisposableCharArray chars = DisposableCharArray.wrap(new char[0]);
        final DisposableByteArray bytes = DisposableByteArray.wrap(new byte[0]);
        final DisposableShortArray shorts = DisposableShortArray.wrap(new short[0]);
        final DisposableIntArray ints = DisposableIntArray.wrap(new int[0]);
        final DisposableLongArray longs = DisposableLongArray.wrap(new long[0]);
        final DisposableFloatArray floats = DisposableFloatArray.wrap(new float[0]);
        final DisposableDoubleArray doubles = DisposableDoubleArray.wrap(new double[0]);
        final DisposableDeque<Object> deque = DisposableDeque.wrap(new ArrayDeque<>());

        final List<Runnable> nullSupplierCalls = Arrays.asList(() -> objects.<List<Object>> toCollection(null),
                () -> booleans.<List<Boolean>> toCollection(null), () -> chars.<List<Character>> toCollection(null),
                () -> bytes.<List<Byte>> toCollection(null), () -> shorts.<List<Short>> toCollection(null),
                () -> ints.<List<Integer>> toCollection(null), () -> longs.<List<Long>> toCollection(null),
                () -> floats.<List<Float>> toCollection(null), () -> doubles.<List<Double>> toCollection(null),
                () -> deque.<List<Object>> toCollection(null));

        final List<Runnable> nullResultCalls = Arrays.asList(() -> objects.<List<Object>> toCollection(size -> null),
                () -> booleans.<List<Boolean>> toCollection(size -> null), () -> chars.<List<Character>> toCollection(size -> null),
                () -> bytes.<List<Byte>> toCollection(size -> null), () -> shorts.<List<Short>> toCollection(size -> null),
                () -> ints.<List<Integer>> toCollection(size -> null), () -> longs.<List<Long>> toCollection(size -> null),
                () -> floats.<List<Float>> toCollection(size -> null), () -> doubles.<List<Double>> toCollection(size -> null),
                () -> deque.<List<Object>> toCollection(size -> null));

        assertEquals(10, nullSupplierCalls.size());
        assertEquals(10, nullResultCalls.size());

        for (final Runnable call : nullSupplierCalls) {
            assertEquals("'supplier' cannot be null", assertThrows(IllegalArgumentException.class, call::run).getMessage());
        }

        for (final Runnable call : nullResultCalls) {
            assertEquals("supplier returned null", assertThrows(IllegalArgumentException.class, call::run).getMessage());
        }
    }

    @Test
    public void testDisposableObjArrayRedeclaresBothInheritedStaticFactories() throws Exception {
        // this assignment compiles ONLY because DisposableObjArray redeclares wrap(Object[]), hiding the inherited
        // generic wrap(T[]) whose result type would be DisposableArray<String>
        final DisposableObjArray wrapped = DisposableObjArray.wrap(new String[] { "a" });
        assertEquals("a", wrapped.get(0));
        assertEquals(1, wrapped.length());

        assertEquals(DisposableObjArray.class, DisposableObjArray.class.getDeclaredMethod("wrap", Object[].class).getReturnType());
        assertEquals(DisposableObjArray.class, DisposableObjArray.class.getDeclaredMethod("create", int.class).getReturnType());

        final java.lang.reflect.Method hiddenCreate = DisposableObjArray.class.getDeclaredMethod("create", Class.class, int.class);
        assertEquals(DisposableArray.class, hiddenCreate.getReturnType());
        assertNotNull(hiddenCreate.getAnnotation(Deprecated.class));
        assertThrows(UnsupportedOperationException.class, () -> DisposableObjArray.create(String.class, 5));
    }
}
