package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn.UnaryOperators;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiObjConsumer;
import com.landawn.abacus.util.function.IntBiObjFunction;
import com.landawn.abacus.util.function.IntBiObjPredicate;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.IntObjPredicate;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.function.UnaryOperator;

@Tag("new-test")
public class Fn103Test extends TestBase {

    @Nested
    @DisplayName("LongSuppliers Tests")
    public class LongSuppliersTest {

        @Test
        public void testOfCurrentTimeMillis() {
            LongSupplier supplier = Fn.LongSuppliers.ofCurrentTimeMillis();
            assertNotNull(supplier);

            long time1 = supplier.getAsLong();
            assertTrue(time1 > 0);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long time2 = supplier.getAsLong();
            assertTrue(time2 >= time1);
        }
    }

    @Nested
    @DisplayName("Suppliers Tests")
    public class SuppliersTest {

        @Test
        public void testOf_Supplier() {
            Supplier<String> original = () -> "test";
            Supplier<String> result = Suppliers.of(original);
            assertSame(original, result);
            assertEquals("test", result.get());
        }

        @Test
        public void testOf_WithFunction() {
            String input = "input";
            Function<String, Integer> func = String::length;
            Supplier<Integer> supplier = Suppliers.of(input, func);

            assertNotNull(supplier);
            assertEquals(5, supplier.get());
            assertEquals(5, supplier.get());
        }

        @Test
        public void testOfInstance() {
            String instance = "constant";
            Supplier<String> supplier = Suppliers.ofInstance(instance);

            assertSame(instance, supplier.get());
            assertSame(instance, supplier.get());
        }

        @Test
        public void testOfUUID() {
            Supplier<String> uuidSupplier = Suppliers.ofUUID();
            assertNotNull(uuidSupplier);

            String uuid1 = uuidSupplier.get();
            String uuid2 = uuidSupplier.get();

            assertNotNull(uuid1);
            assertNotNull(uuid2);
            assertNotEquals(uuid1, uuid2);
        }

        @Test
        public void testOfGUID() {
            Supplier<String> guidSupplier = Suppliers.ofGUID();
            assertNotNull(guidSupplier);

            String guid1 = guidSupplier.get();
            String guid2 = guidSupplier.get();

            assertNotNull(guid1);
            assertNotNull(guid2);
            assertNotEquals(guid1, guid2);
        }

        @Test
        public void testOfEmptyArraySuppliers() {
            assertArrayEquals(new boolean[0], Suppliers.ofEmptyBooleanArray().get());
            assertArrayEquals(new char[0], Suppliers.ofEmptyCharArray().get());
            assertArrayEquals(new byte[0], Suppliers.ofEmptyByteArray().get());
            assertArrayEquals(new short[0], Suppliers.ofEmptyShortArray().get());
            assertArrayEquals(new int[0], Suppliers.ofEmptyIntArray().get());
            assertArrayEquals(new long[0], Suppliers.ofEmptyLongArray().get());
            assertArrayEquals(new float[0], Suppliers.ofEmptyFloatArray().get(), 0.0f);
            assertArrayEquals(new double[0], Suppliers.ofEmptyDoubleArray().get(), 0.0);
            assertArrayEquals(new String[0], Suppliers.ofEmptyStringArray().get());
            assertArrayEquals(new Object[0], Suppliers.ofEmptyObjectArray().get());
        }

        @Test
        public void testOfEmptyString() {
            Supplier<String> supplier = Suppliers.ofEmptyString();
            assertEquals("", supplier.get());
            assertSame(supplier.get(), supplier.get());
        }

        @Test
        public void testOfPrimitiveListSuppliers() {
            assertTrue(Suppliers.ofBooleanList().get() instanceof BooleanList);
            assertTrue(Suppliers.ofCharList().get() instanceof CharList);
            assertTrue(Suppliers.ofByteList().get() instanceof ByteList);
            assertTrue(Suppliers.ofShortList().get() instanceof ShortList);
            assertTrue(Suppliers.ofIntList().get() instanceof IntList);
            assertTrue(Suppliers.ofLongList().get() instanceof LongList);
            assertTrue(Suppliers.ofFloatList().get() instanceof FloatList);
            assertTrue(Suppliers.ofDoubleList().get() instanceof DoubleList);
        }

        @Test
        public void testOfCollectionSuppliers() {
            assertTrue(Suppliers.ofList().get() instanceof ArrayList);
            assertTrue(Suppliers.ofLinkedList().get() instanceof LinkedList);
            assertTrue(Suppliers.ofSet().get() instanceof HashSet);
            assertTrue(Suppliers.ofLinkedHashSet().get() instanceof LinkedHashSet);
            assertTrue(Suppliers.ofSortedSet().get() instanceof TreeSet);
            assertTrue(Suppliers.ofNavigableSet().get() instanceof TreeSet);
            assertTrue(Suppliers.ofTreeSet().get() instanceof TreeSet);
            assertTrue(Suppliers.ofQueue().get() instanceof Queue);
            assertTrue(Suppliers.ofDeque().get() instanceof Deque);
            assertTrue(Suppliers.ofArrayDeque().get() instanceof ArrayDeque);
        }

        @Test
        public void testOfConcurrentCollectionSuppliers() {
            assertTrue(Suppliers.ofLinkedBlockingQueue().get() instanceof LinkedBlockingQueue);
            assertTrue(Suppliers.ofLinkedBlockingDeque().get() instanceof LinkedBlockingDeque);
            assertTrue(Suppliers.ofConcurrentLinkedQueue().get() instanceof ConcurrentLinkedQueue);
            assertTrue(Suppliers.ofPriorityQueue().get() instanceof PriorityQueue);
            assertTrue(Suppliers.ofConcurrentHashSet().get() instanceof Set);
        }

        @Test
        public void testOfMapSuppliers() {
            assertTrue(Suppliers.ofMap().get() instanceof HashMap);
            assertTrue(Suppliers.ofLinkedHashMap().get() instanceof LinkedHashMap);
            assertTrue(Suppliers.ofIdentityHashMap().get() instanceof IdentityHashMap);
            assertTrue(Suppliers.ofSortedMap().get() instanceof TreeMap);
            assertTrue(Suppliers.ofNavigableMap().get() instanceof TreeMap);
            assertTrue(Suppliers.ofTreeMap().get() instanceof TreeMap);
            assertTrue(Suppliers.ofConcurrentMap().get() instanceof ConcurrentHashMap);
            assertTrue(Suppliers.ofConcurrentHashMap().get() instanceof ConcurrentHashMap);
        }

        @Test
        public void testOfMultimapSuppliers() {
            assertTrue(Suppliers.ofBiMap().get() instanceof BiMap);
            assertTrue(Suppliers.ofMultiset().get() instanceof Multiset);
            assertTrue(Suppliers.ofListMultimap().get() instanceof ListMultimap);
            assertTrue(Suppliers.ofSetMultimap().get() instanceof SetMultimap);
        }

        @Test
        public void testOfMultisetWithMapType() {
            Supplier<Multiset<String>> supplier = Suppliers.ofMultiset(LinkedHashMap.class);
            Multiset<String> multiset = supplier.get();
            assertNotNull(multiset);
        }

        @Test
        public void testOfMultisetWithMapSupplier() {
            Supplier<Multiset<String>> supplier = Suppliers.ofMultiset(HashMap::new);
            Multiset<String> multiset = supplier.get();
            assertNotNull(multiset);
        }

        @Test
        public void testOfListMultimapVariants() {
            Supplier<ListMultimap<String, Integer>> supplier1 = Suppliers.ofListMultimap(LinkedHashMap.class);
            assertNotNull(supplier1.get());

            Supplier<ListMultimap<String, Integer>> supplier2 = Suppliers.ofListMultimap(HashMap.class, ArrayList.class);
            assertNotNull(supplier2.get());

            Supplier<ListMultimap<String, Integer>> supplier3 = Suppliers.ofListMultimap(HashMap::new, ArrayList::new);
            assertNotNull(supplier3.get());
        }

        @Test
        public void testOfSetMultimapVariants() {
            Supplier<SetMultimap<String, Integer>> supplier1 = Suppliers.ofSetMultimap(LinkedHashMap.class);
            assertNotNull(supplier1.get());

            Supplier<SetMultimap<String, Integer>> supplier2 = Suppliers.ofSetMultimap(HashMap.class, HashSet.class);
            assertNotNull(supplier2.get());

            Supplier<SetMultimap<String, Integer>> supplier3 = Suppliers.ofSetMultimap(HashMap::new, HashSet::new);
            assertNotNull(supplier3.get());
        }

        @Test
        public void testOfMultimap() {
            Supplier<Multimap<String, Integer, List<Integer>>> supplier = Suppliers.ofMultimap(HashMap::new, ArrayList::new);
            Multimap<String, Integer, List<Integer>> multimap = supplier.get();
            assertNotNull(multimap);
        }

        @Test
        public void testOfStringBuilder() {
            Supplier<StringBuilder> supplier = Suppliers.ofStringBuilder();
            StringBuilder sb1 = supplier.get();
            StringBuilder sb2 = supplier.get();

            assertNotNull(sb1);
            assertNotNull(sb2);
            assertNotSame(sb1, sb2);
        }

        @Test
        public void testOfCollection() {
            assertEquals(ArrayList.class, Suppliers.ofCollection(List.class).get().getClass());
            assertEquals(ArrayList.class, Suppliers.ofCollection(ArrayList.class).get().getClass());
            assertEquals(LinkedList.class, Suppliers.ofCollection(LinkedList.class).get().getClass());
            assertEquals(HashSet.class, Suppliers.ofCollection(Set.class).get().getClass());
            assertEquals(LinkedHashSet.class, Suppliers.ofCollection(LinkedHashSet.class).get().getClass());
            assertTrue(Suppliers.ofCollection(SortedSet.class).get() instanceof TreeSet);

            assertThrows(IllegalArgumentException.class, () -> Suppliers.ofCollection(ImmutableList.class));
        }

        @Test
        public void testOfMap() {
            assertEquals(HashMap.class, Suppliers.ofMap(Map.class).get().getClass());
            assertEquals(HashMap.class, Suppliers.ofMap(HashMap.class).get().getClass());
            assertEquals(LinkedHashMap.class, Suppliers.ofMap(LinkedHashMap.class).get().getClass());
            assertTrue(Suppliers.ofMap(SortedMap.class).get() instanceof TreeMap);

            assertThrows(IllegalArgumentException.class, () -> Suppliers.ofMap((Class) String.class));
        }

        @Test
        public void testDeprecatedImmutableMethods() {
            assertThrows(UnsupportedOperationException.class, Suppliers::ofImmutableList);
            assertThrows(UnsupportedOperationException.class, Suppliers::ofImmutableSet);
            assertThrows(UnsupportedOperationException.class, Suppliers::ofImmutableMap);
        }

        @Test
        public void testNewException() {
            Supplier<Exception> supplier = Suppliers.newException();
            Exception e1 = supplier.get();
            Exception e2 = supplier.get();

            assertNotNull(e1);
            assertNotNull(e2);
            assertNotSame(e1, e2);
        }

        @Test
        public void testNewRuntimeException() {
            Supplier<RuntimeException> supplier = Suppliers.newRuntimeException();
            RuntimeException e1 = supplier.get();
            RuntimeException e2 = supplier.get();

            assertNotNull(e1);
            assertNotNull(e2);
            assertNotSame(e1, e2);
        }

        @Test
        public void testNewNoSuchElementException() {
            Supplier<NoSuchElementException> supplier = Suppliers.newNoSuchElementException();
            NoSuchElementException e1 = supplier.get();
            NoSuchElementException e2 = supplier.get();

            assertNotNull(e1);
            assertNotNull(e2);
            assertNotSame(e1, e2);
        }
    }

    @Nested
    @DisplayName("IntFunctions Tests")
    public class IntFunctionsTest {

        @Test
        public void testOf() {
            IntFunction<String> original = i -> "Number: " + i;
            IntFunction<String> result = IntFunctions.of(original);
            assertSame(original, result);
            assertEquals("Number: 5", result.apply(5));
        }

        @Test
        public void testOfArrayFunctions() {
            assertArrayEquals(new boolean[5], IntFunctions.ofBooleanArray().apply(5));
            assertArrayEquals(new char[5], IntFunctions.ofCharArray().apply(5));
            assertArrayEquals(new byte[5], IntFunctions.ofByteArray().apply(5));
            assertArrayEquals(new short[5], IntFunctions.ofShortArray().apply(5));
            assertArrayEquals(new int[5], IntFunctions.ofIntArray().apply(5));
            assertArrayEquals(new long[5], IntFunctions.ofLongArray().apply(5));
            assertArrayEquals(new float[5], IntFunctions.ofFloatArray().apply(5), 0.0f);
            assertArrayEquals(new double[5], IntFunctions.ofDoubleArray().apply(5), 0.0);
            assertArrayEquals(new String[5], IntFunctions.ofStringArray().apply(5));
            assertArrayEquals(new Object[5], IntFunctions.ofObjectArray().apply(5));
        }

        @Test
        public void testOfCollectionFunctions() {
            assertTrue(IntFunctions.ofList().apply(10) instanceof ArrayList);
            assertTrue(IntFunctions.ofLinkedList().apply(10) instanceof LinkedList);

            assertTrue(IntFunctions.ofSet().apply(10) instanceof HashSet);
            assertTrue(IntFunctions.ofLinkedHashSet().apply(10) instanceof LinkedHashSet);
            assertTrue(IntFunctions.ofSortedSet().apply(10) instanceof TreeSet);
            assertTrue(IntFunctions.ofNavigableSet().apply(10) instanceof TreeSet);
            assertTrue(IntFunctions.ofTreeSet().apply(10) instanceof TreeSet);

            assertTrue(IntFunctions.ofQueue().apply(10) instanceof Queue);
            assertTrue(IntFunctions.ofDeque().apply(10) instanceof Deque);
            assertTrue(IntFunctions.ofArrayDeque().apply(10) instanceof ArrayDeque);
        }

        @Test
        public void testOfConcurrentCollectionFunctions() {
            assertTrue(IntFunctions.ofLinkedBlockingQueue().apply(10) instanceof LinkedBlockingQueue);
            assertEquals(10, ((LinkedBlockingQueue<?>) IntFunctions.ofLinkedBlockingQueue().apply(10)).remainingCapacity());

            assertTrue(IntFunctions.ofArrayBlockingQueue().apply(10) instanceof ArrayBlockingQueue);
            assertEquals(10, ((ArrayBlockingQueue<?>) IntFunctions.ofArrayBlockingQueue().apply(10)).remainingCapacity());

            assertTrue(IntFunctions.ofLinkedBlockingDeque().apply(10) instanceof LinkedBlockingDeque);
            assertTrue(IntFunctions.ofConcurrentLinkedQueue().apply(10) instanceof ConcurrentLinkedQueue);
            assertTrue(IntFunctions.ofPriorityQueue().apply(10) instanceof PriorityQueue);
        }

        @Test
        public void testOfMapFunctions() {
            assertTrue(IntFunctions.ofMap().apply(10) instanceof HashMap);
            assertTrue(IntFunctions.ofLinkedHashMap().apply(10) instanceof LinkedHashMap);
            assertTrue(IntFunctions.ofIdentityHashMap().apply(10) instanceof IdentityHashMap);
            assertTrue(IntFunctions.ofSortedMap().apply(10) instanceof TreeMap);
            assertTrue(IntFunctions.ofNavigableMap().apply(10) instanceof TreeMap);
            assertTrue(IntFunctions.ofTreeMap().apply(10) instanceof TreeMap);
            assertTrue(IntFunctions.ofConcurrentMap().apply(10) instanceof ConcurrentHashMap);
            assertTrue(IntFunctions.ofConcurrentHashMap().apply(10) instanceof ConcurrentHashMap);
        }

        @Test
        public void testOfMultimapFunctions() {
            assertTrue(IntFunctions.ofBiMap().apply(10) instanceof BiMap);
            assertTrue(IntFunctions.ofMultiset().apply(10) instanceof Multiset);
            assertTrue(IntFunctions.ofListMultimap().apply(10) instanceof ListMultimap);
            assertTrue(IntFunctions.ofSetMultimap().apply(10) instanceof SetMultimap);
        }

        @Test
        public void testOfDisposableArray() {
            IntFunction<DisposableObjArray> func = IntFunctions.ofDisposableArray();
            DisposableObjArray array1 = func.apply(5);
            DisposableObjArray array2 = func.apply(10);

            assertNotNull(array1);
            assertNotNull(array2);
            assertSame(array1, array2);
        }

        @Test
        public void testOfDisposableArrayWithType() {
            IntFunction<DisposableArray<String>> func = IntFunctions.ofDisposableArray(String.class);
            DisposableArray<String> array1 = func.apply(5);
            DisposableArray<String> array2 = func.apply(10);

            assertNotNull(array1);
            assertNotNull(array2);
            assertSame(array1, array2);
        }

        @Test
        public void testOfCollection() {
            assertTrue(IntFunctions.ofCollection(List.class).apply(10) instanceof ArrayList);
            assertTrue(IntFunctions.ofCollection(ArrayList.class).apply(10) instanceof ArrayList);
            assertTrue(IntFunctions.ofCollection(LinkedList.class).apply(10) instanceof LinkedList);
            assertTrue(IntFunctions.ofCollection(Set.class).apply(10) instanceof HashSet);
            assertTrue(IntFunctions.ofCollection(LinkedHashSet.class).apply(10) instanceof LinkedHashSet);
            assertTrue(IntFunctions.ofCollection(SortedSet.class).apply(10) instanceof TreeSet);

            assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofCollection(ImmutableList.class));
        }

        @Test
        public void testOfMap() {
            assertTrue(IntFunctions.ofMap(Map.class).apply(10) instanceof HashMap);
            assertTrue(IntFunctions.ofMap(HashMap.class).apply(10) instanceof HashMap);
            assertTrue(IntFunctions.ofMap(LinkedHashMap.class).apply(10) instanceof LinkedHashMap);
            assertTrue(IntFunctions.ofMap(SortedMap.class).apply(10) instanceof TreeMap);

            assertThrows(IllegalArgumentException.class, () -> IntFunctions.ofMap((Class) String.class));
        }

        @Test
        public void testDeprecatedImmutableMethods() {
            assertThrows(UnsupportedOperationException.class, IntFunctions::ofImmutableList);
            assertThrows(UnsupportedOperationException.class, IntFunctions::ofImmutableSet);
            assertThrows(UnsupportedOperationException.class, IntFunctions::ofImmutableMap);
        }
    }

    @Nested
    @DisplayName("Factory Tests")
    public class FactoryTest {

        @Test
        public void testFactoryExtendsIntFunctions() {
            assertTrue(IntFunctions.ofList().apply(10) instanceof ArrayList);
            assertTrue(IntFunctions.ofSet().apply(10) instanceof HashSet);
            assertTrue(IntFunctions.ofMap().apply(10) instanceof HashMap);
        }
    }

    @Nested
    @DisplayName("Predicates Tests")
    public class PredicatesTest {

        @Test
        public void testIndexed() {
            List<String> processed = new ArrayList<>();
            IntObjPredicate<String> indexPredicate = (index, value) -> {
                processed.add(index + ":" + value);
                return index % 2 == 0;
            };

            Predicate<String> predicate = Fn.Predicates.indexed(indexPredicate);

            assertTrue(predicate.test("a"));
            assertFalse(predicate.test("b"));
            assertTrue(predicate.test("c"));

            assertEquals(Arrays.asList("0:a", "1:b", "2:c"), processed);

            assertThrows(IllegalArgumentException.class, () -> Fn.Predicates.indexed(null));
        }

        @Test
        public void testDistinct() {
            Predicate<String> predicate = Fn.Predicates.distinct();

            assertTrue(predicate.test("a"));
            assertTrue(predicate.test("b"));
            assertFalse(predicate.test("a"));
            assertTrue(predicate.test("c"));
            assertFalse(predicate.test("b"));

            assertTrue(predicate.test(null));
            assertFalse(predicate.test(null));
        }

        @Test
        public void testDistinctBy() {
            Predicate<String> predicate = Fn.Predicates.distinctBy(String::length);

            assertTrue(predicate.test("a"));
            assertTrue(predicate.test("ab"));
            assertFalse(predicate.test("c"));
            assertFalse(predicate.test("de"));
            assertTrue(predicate.test("abc"));
        }

        @Test
        public void testConcurrentDistinct() {
            Predicate<String> predicate = Fn.Predicates.concurrentDistinct();

            assertTrue(predicate.test("a"));
            assertTrue(predicate.test("b"));
            assertFalse(predicate.test("a"));
            assertTrue(predicate.test("c"));
            assertFalse(predicate.test("b"));
            assertTrue(predicate.test(null));

            // assertThrows(NullPointerException.class, () -> predicate.test(null));
        }

        @Test
        public void testConcurrentDistinctBy() {
            Predicate<String> predicate = Fn.Predicates.concurrentDistinctBy(String::length);

            assertTrue(predicate.test("a"));
            assertTrue(predicate.test("ab"));
            assertFalse(predicate.test("c"));
            assertFalse(predicate.test("de"));
            assertTrue(predicate.test("abc"));
        }

        @Test
        public void testSkipRepeats() {
            Predicate<String> predicate = Fn.Predicates.skipRepeats();

            assertTrue(predicate.test("a"));
            assertFalse(predicate.test("a"));
            assertTrue(predicate.test("b"));
            assertTrue(predicate.test("a"));
            assertFalse(predicate.test("a"));

            assertTrue(predicate.test(null));
            assertFalse(predicate.test(null));
            assertTrue(predicate.test("c"));
        }
    }

    @Nested
    @DisplayName("BiPredicates Tests")
    public class BiPredicatesTest {

        @Test
        public void testAlwaysTrue() {
            BiPredicate<String, Integer> predicate = Fn.BiPredicates.alwaysTrue();
            assertTrue(predicate.test("test", 123));
            assertTrue(predicate.test(null, null));
            assertTrue(predicate.test("", 0));
        }

        @Test
        public void testAlwaysFalse() {
            BiPredicate<String, Integer> predicate = Fn.BiPredicates.alwaysFalse();
            assertFalse(predicate.test("test", 123));
            assertFalse(predicate.test(null, null));
            assertFalse(predicate.test("", 0));
        }

        @Test
        public void testIndexed() {
            List<String> processed = new ArrayList<>();
            IntBiObjPredicate<String, Integer> indexPredicate = (index, str, num) -> {
                processed.add(index + ":" + str + ":" + num);
                return index % 2 == 0;
            };

            BiPredicate<String, Integer> predicate = Fn.BiPredicates.indexed(indexPredicate);

            assertTrue(predicate.test("a", 1));
            assertFalse(predicate.test("b", 2));
            assertTrue(predicate.test("c", 3));

            assertEquals(Arrays.asList("0:a:1", "1:b:2", "2:c:3"), processed);

            assertThrows(IllegalArgumentException.class, () -> Fn.BiPredicates.indexed(null));
        }
    }

    @Nested
    @DisplayName("TriPredicates Tests")
    public class TriPredicatesTest {

        @Test
        public void testAlwaysTrue() {
            TriPredicate<String, Integer, Boolean> predicate = Fn.TriPredicates.alwaysTrue();
            assertTrue(predicate.test("test", 123, true));
            assertTrue(predicate.test(null, null, null));
            assertTrue(predicate.test("", 0, false));
        }

        @Test
        public void testAlwaysFalse() {
            TriPredicate<String, Integer, Boolean> predicate = Fn.TriPredicates.alwaysFalse();
            assertFalse(predicate.test("test", 123, true));
            assertFalse(predicate.test(null, null, null));
            assertFalse(predicate.test("", 0, false));
        }
    }

    @Nested
    @DisplayName("Consumers Tests")
    public class ConsumersTest {

        @Test
        public void testIndexed() {
            List<String> processed = new ArrayList<>();
            IntObjConsumer<String> indexConsumer = (index, value) -> {
                processed.add(index + ":" + value);
            };

            Consumer<String> consumer = Fn.Consumers.indexed(indexConsumer);

            consumer.accept("a");
            consumer.accept("b");
            consumer.accept("c");

            assertEquals(Arrays.asList("0:a", "1:b", "2:c"), processed);

            assertThrows(IllegalArgumentException.class, () -> Fn.Consumers.indexed(null));
        }
    }

    @Nested
    @DisplayName("BiConsumers Tests")
    public class BiConsumersTest {

        @Test
        public void testDoNothing() {
            BiConsumer<String, Integer> consumer = Fn.BiConsumers.doNothing();
            consumer.accept("test", 123);
            consumer.accept(null, null);
        }

        @Test
        public void testOfAdd() {
            List<String> list = new ArrayList<>();
            BiConsumer<List<String>, String> consumer = Fn.BiConsumers.ofAdd();

            consumer.accept(list, "a");
            consumer.accept(list, "b");

            assertEquals(Arrays.asList("a", "b"), list);
        }

        @Test
        public void testOfAddAll() {
            List<String> list1 = new ArrayList<>();
            list1.add("a");

            List<String> list2 = Arrays.asList("b", "c");

            BiConsumer<List<String>, List<String>> consumer = Fn.BiConsumers.ofAddAll();
            consumer.accept(list1, list2);

            assertEquals(Arrays.asList("a", "b", "c"), list1);
        }

        @Test
        public void testOfAddAlll() {
            IntList list1 = new IntList();
            list1.add(1);

            IntList list2 = new IntList();
            list2.add(2);
            list2.add(3);

            BiConsumer<IntList, IntList> consumer = Fn.BiConsumers.ofAddAlll();
            consumer.accept(list1, list2);

            assertEquals(3, list1.size());
            assertEquals(1, list1.get(0));
            assertEquals(2, list1.get(1));
            assertEquals(3, list1.get(2));
        }

        @Test
        public void testOfRemove() {
            List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
            BiConsumer<List<String>, String> consumer = Fn.BiConsumers.ofRemove();

            consumer.accept(list, "b");
            assertEquals(Arrays.asList("a", "c"), list);
        }

        @Test
        public void testOfRemoveAll() {
            List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
            List<String> list2 = Arrays.asList("b", "d");

            BiConsumer<List<String>, List<String>> consumer = Fn.BiConsumers.ofRemoveAll();
            consumer.accept(list1, list2);

            assertEquals(Arrays.asList("a", "c"), list1);
        }

        @Test
        public void testOfRemoveAlll() {
            IntList list1 = new IntList();
            list1.add(1);
            list1.add(2);
            list1.add(3);

            IntList list2 = new IntList();
            list2.add(2);

            BiConsumer<IntList, IntList> consumer = Fn.BiConsumers.ofRemoveAlll();
            consumer.accept(list1, list2);

            assertEquals(2, list1.size());
            assertEquals(1, list1.get(0));
            assertEquals(3, list1.get(1));
        }

        @Test
        public void testOfPut() {
            Map<String, Integer> map = new HashMap<>();
            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);

            BiConsumer<Map<String, Integer>, Map.Entry<String, Integer>> consumer = Fn.BiConsumers.ofPut();
            consumer.accept(map, entry);

            assertEquals(123, map.get("key"));
        }

        @Test
        public void testOfPutAll() {
            Map<String, Integer> map1 = new HashMap<>();
            map1.put("a", 1);

            Map<String, Integer> map2 = new HashMap<>();
            map2.put("b", 2);
            map2.put("c", 3);

            BiConsumer<Map<String, Integer>, Map<String, Integer>> consumer = Fn.BiConsumers.ofPutAll();
            consumer.accept(map1, map2);

            assertEquals(3, map1.size());
            assertEquals(1, map1.get("a"));
            assertEquals(2, map1.get("b"));
            assertEquals(3, map1.get("c"));
        }

        @Test
        public void testOfRemoveByKey() {
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);

            BiConsumer<Map<String, Integer>, String> consumer = Fn.BiConsumers.ofRemoveByKey();
            consumer.accept(map, "a");

            assertEquals(1, map.size());
            assertFalse(map.containsKey("a"));
            assertTrue(map.containsKey("b"));
        }

        @Test
        public void testOfMerge() {
            Joiner joiner1 = Joiner.with(",");
            joiner1.append("a").append("b");

            Joiner joiner2 = Joiner.with(",");
            joiner2.append("c").append("d");

            BiConsumer<Joiner, Joiner> consumer = Fn.BiConsumers.ofMerge();
            consumer.accept(joiner1, joiner2);

            assertEquals("a,b,c,d", joiner1.toString());
        }

        @Test
        public void testOfAppend() {
            StringBuilder sb = new StringBuilder("Hello");
            BiConsumer<StringBuilder, String> consumer = Fn.BiConsumers.ofAppend();

            consumer.accept(sb, " World");
            assertEquals("Hello World", sb.toString());
        }

        @Test
        public void testIndexed() {
            List<String> processed = new ArrayList<>();
            IntBiObjConsumer<String, Integer> indexConsumer = (index, str, num) -> {
                processed.add(index + ":" + str + ":" + num);
            };

            BiConsumer<String, Integer> consumer = Fn.BiConsumers.indexed(indexConsumer);

            consumer.accept("a", 1);
            consumer.accept("b", 2);
            consumer.accept("c", 3);

            assertEquals(Arrays.asList("0:a:1", "1:b:2", "2:c:3"), processed);

            assertThrows(IllegalArgumentException.class, () -> Fn.BiConsumers.indexed(null));
        }
    }

    @Nested
    @DisplayName("TriConsumers Tests")
    public class TriConsumersTest {

        @Test
        public void testClassExists() {
            assertNotNull(Fn.TriConsumers.class);
        }
    }

    @Nested
    @DisplayName("Functions Tests")
    public class FunctionsTest {

        @Test
        public void testIndexed() {
            List<String> processed = new ArrayList<>();
            IntObjFunction<String, String> indexFunction = (index, value) -> {
                String result = index + ":" + value;
                processed.add(result);
                return result;
            };

            Function<String, String> function = Fn.Functions.indexed(indexFunction);

            assertEquals("0:a", function.apply("a"));
            assertEquals("1:b", function.apply("b"));
            assertEquals("2:c", function.apply("c"));

            assertEquals(Arrays.asList("0:a", "1:b", "2:c"), processed);

            assertThrows(IllegalArgumentException.class, () -> Fn.Functions.indexed(null));
        }
    }

    @Nested
    @DisplayName("BiFunctions Tests")
    public class BiFunctionsTest {

        @Test
        public void testSelectFirst() {
            BiFunction<String, Integer, String> function = Fn.BiFunctions.selectFirst();
            assertEquals("first", function.apply("first", 123));
            assertEquals("test", function.apply("test", 456));
            assertNull(function.apply(null, 789));
        }

        @Test
        public void testSelectSecond() {
            BiFunction<String, Integer, Integer> function = Fn.BiFunctions.selectSecond();
            assertEquals(123, function.apply("first", 123));
            assertEquals(456, function.apply("test", 456));
            assertNull(function.apply("null", null));
        }

        @Test
        public void testOfAdd() {
            List<String> list = new ArrayList<>();
            list.add("a");

            BiFunction<List<String>, String, List<String>> function = Fn.BiFunctions.ofAdd();
            List<String> result = function.apply(list, "b");

            assertSame(list, result);
            assertEquals(Arrays.asList("a", "b"), result);
        }

        @Test
        public void testOfAddAll() {
            List<String> list1 = new ArrayList<>();
            list1.add("a");

            List<String> list2 = Arrays.asList("b", "c");

            BiFunction<List<String>, List<String>, List<String>> function = Fn.BiFunctions.ofAddAll();
            List<String> result = function.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(Arrays.asList("a", "b", "c"), result);
        }

        @Test
        public void testOfAddAlll() {
            IntList list1 = new IntList();
            list1.add(1);

            IntList list2 = new IntList();
            list2.add(2);
            list2.add(3);

            BiFunction<IntList, IntList, IntList> function = Fn.BiFunctions.ofAddAlll();
            IntList result = function.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0));
            assertEquals(2, result.get(1));
            assertEquals(3, result.get(2));
        }

        @Test
        public void testOfRemove() {
            List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));

            BiFunction<List<String>, String, List<String>> function = Fn.BiFunctions.ofRemove();
            List<String> result = function.apply(list, "b");

            assertSame(list, result);
            assertEquals(Arrays.asList("a", "c"), result);
        }

        @Test
        public void testOfRemoveAll() {
            List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
            List<String> list2 = Arrays.asList("b", "d");

            BiFunction<List<String>, List<String>, List<String>> function = Fn.BiFunctions.ofRemoveAll();
            List<String> result = function.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(Arrays.asList("a", "c"), result);
        }

        @Test
        public void testOfRemoveAlll() {
            IntList list1 = new IntList();
            list1.add(1);
            list1.add(2);
            list1.add(3);

            IntList list2 = new IntList();
            list2.add(2);

            BiFunction<IntList, IntList, IntList> function = Fn.BiFunctions.ofRemoveAlll();
            IntList result = function.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(2, result.size());
            assertEquals(1, result.get(0));
            assertEquals(3, result.get(1));
        }

        @Test
        public void testOfPut() {
            Map<String, Integer> map = new HashMap<>();
            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);

            BiFunction<Map<String, Integer>, Map.Entry<String, Integer>, Map<String, Integer>> function = Fn.BiFunctions.ofPut();
            Map<String, Integer> result = function.apply(map, entry);

            assertSame(map, result);
            assertEquals(123, result.get("key"));
        }

        @Test
        public void testOfPutAll() {
            Map<String, Integer> map1 = new HashMap<>();
            map1.put("a", 1);

            Map<String, Integer> map2 = new HashMap<>();
            map2.put("b", 2);
            map2.put("c", 3);

            BiFunction<Map<String, Integer>, Map<String, Integer>, Map<String, Integer>> function = Fn.BiFunctions.ofPutAll();
            Map<String, Integer> result = function.apply(map1, map2);

            assertSame(map1, result);
            assertEquals(3, result.size());
            assertEquals(1, result.get("a"));
            assertEquals(2, result.get("b"));
            assertEquals(3, result.get("c"));
        }

        @Test
        public void testOfRemoveByKey() {
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);

            BiFunction<Map<String, Integer>, String, Map<String, Integer>> function = Fn.BiFunctions.ofRemoveByKey();
            Map<String, Integer> result = function.apply(map, "a");

            assertSame(map, result);
            assertEquals(1, result.size());
            assertFalse(result.containsKey("a"));
            assertTrue(result.containsKey("b"));
        }

        @Test
        public void testOfMerge() {
            Joiner joiner1 = Joiner.with(",");
            joiner1.append("a").append("b");

            Joiner joiner2 = Joiner.with(",");
            joiner2.append("c").append("d");

            BiFunction<Joiner, Joiner, Joiner> function = Fn.BiFunctions.ofMerge();
            Joiner result = function.apply(joiner1, joiner2);

            assertSame(joiner1, result);
            assertEquals("a,b,c,d", result.toString());
        }

        @Test
        public void testOfAppend() {
            StringBuilder sb = new StringBuilder("Hello");

            BiFunction<StringBuilder, String, StringBuilder> function = Fn.BiFunctions.ofAppend();
            StringBuilder result = function.apply(sb, " World");

            assertSame(sb, result);
            assertEquals("Hello World", result.toString());
        }

        @Test
        public void testIndexed() {
            List<String> processed = new ArrayList<>();
            IntBiObjFunction<String, Integer, String> indexFunction = (index, str, num) -> {
                String result = index + ":" + str + ":" + num;
                processed.add(result);
                return result;
            };

            BiFunction<String, Integer, String> function = Fn.BiFunctions.indexed(indexFunction);

            assertEquals("0:a:1", function.apply("a", 1));
            assertEquals("1:b:2", function.apply("b", 2));
            assertEquals("2:c:3", function.apply("c", 3));

            assertEquals(Arrays.asList("0:a:1", "1:b:2", "2:c:3"), processed);

            assertThrows(IllegalArgumentException.class, () -> Fn.BiFunctions.indexed(null));
        }
    }

    @Nested
    @DisplayName("TriFunctions Tests")
    public class TriFunctionsTest {

        @Test
        public void testClassExists() {
            assertNotNull(Fn.TriFunctions.class);
        }
    }

    @Nested
    @DisplayName("BinaryOperators Tests")
    public class BinaryOperatorsTest {

        @Test
        public void testOfAddAll_Deprecated() {
            List<String> list1 = new ArrayList<>(Arrays.asList("a", "b"));
            List<String> list2 = Arrays.asList("c", "d");

            BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofAddAll();
            List<String> result = operator.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(Arrays.asList("a", "b", "c", "d"), result);
        }

        @Test
        public void testOfAddAllToFirst() {
            List<String> list1 = new ArrayList<>(Arrays.asList("a", "b"));
            List<String> list2 = Arrays.asList("c", "d");

            BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofAddAllToFirst();
            List<String> result = operator.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(Arrays.asList("a", "b", "c", "d"), result);
        }

        @Test
        public void testOfAddAllToBigger() {
            List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c"));
            List<String> list2 = new ArrayList<>(Arrays.asList("d", "e"));

            BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofAddAllToBigger();

            List<String> result1 = operator.apply(list1, list2);
            assertSame(list1, result1);
            assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result1);

            list1 = new ArrayList<>(Arrays.asList("a"));
            list2 = new ArrayList<>(Arrays.asList("b", "c"));

            List<String> result2 = operator.apply(list1, list2);
            assertSame(list2, result2);
            assertEquals(Arrays.asList("b", "c", "a"), result2);
        }

        @Test
        public void testOfRemoveAll_Deprecated() {
            List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
            List<String> list2 = Arrays.asList("b", "d");

            BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofRemoveAll();
            List<String> result = operator.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(Arrays.asList("a", "c"), result);
        }

        @Test
        public void testOfRemoveAllFromFirst() {
            List<String> list1 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
            List<String> list2 = Arrays.asList("b", "d");

            BinaryOperator<List<String>> operator = Fn.BinaryOperators.ofRemoveAllFromFirst();
            List<String> result = operator.apply(list1, list2);

            assertSame(list1, result);
            assertEquals(Arrays.asList("a", "c"), result);
        }

        @Test
        public void testOfPutAll_Deprecated() {
            Map<String, Integer> map1 = new HashMap<>();
            map1.put("a", 1);

            Map<String, Integer> map2 = new HashMap<>();
            map2.put("b", 2);
            map2.put("c", 3);

            BinaryOperator<Map<String, Integer>> operator = Fn.BinaryOperators.ofPutAll();
            Map<String, Integer> result = operator.apply(map1, map2);

            assertSame(map1, result);
            assertEquals(3, result.size());
            assertEquals(1, result.get("a"));
            assertEquals(2, result.get("b"));
            assertEquals(3, result.get("c"));
        }

        @Test
        public void testOfPutAllToFirst() {
            Map<String, Integer> map1 = new HashMap<>();
            map1.put("a", 1);

            Map<String, Integer> map2 = new HashMap<>();
            map2.put("b", 2);
            map2.put("c", 3);

            BinaryOperator<Map<String, Integer>> operator = Fn.BinaryOperators.ofPutAllToFirst();
            Map<String, Integer> result = operator.apply(map1, map2);

            assertSame(map1, result);
            assertEquals(3, result.size());
        }

        @Test
        public void testOfPutAllToBigger() {
            Map<String, Integer> map1 = new HashMap<>();
            map1.put("a", 1);
            map1.put("b", 2);
            map1.put("c", 3);

            Map<String, Integer> map2 = new HashMap<>();
            map2.put("d", 4);
            map2.put("e", 5);

            BinaryOperator<Map<String, Integer>> operator = Fn.BinaryOperators.ofPutAllToBigger();

            Map<String, Integer> result1 = operator.apply(map1, map2);
            assertSame(map1, result1);
            assertEquals(5, result1.size());

            map1 = new HashMap<>();
            map1.put("a", 1);

            map2 = new HashMap<>();
            map2.put("b", 2);
            map2.put("c", 3);

            Map<String, Integer> result2 = operator.apply(map1, map2);
            assertSame(map2, result2);
            assertEquals(3, result2.size());
        }

        @Test
        public void testOfMerge_Deprecated() {
            Joiner joiner1 = Joiner.with(",");
            joiner1.append("a").append("b");

            Joiner joiner2 = Joiner.with(",");
            joiner2.append("c").append("d");

            BinaryOperator<Joiner> operator = Fn.BinaryOperators.ofMerge();
            Joiner result = operator.apply(joiner1, joiner2);

            assertSame(joiner1, result);
            assertEquals("a,b,c,d", result.toString());
        }

        @Test
        public void testOfMergeToFirst() {
            Joiner joiner1 = Joiner.with(",");
            joiner1.append("a").append("b");

            Joiner joiner2 = Joiner.with(",");
            joiner2.append("c").append("d");

            BinaryOperator<Joiner> operator = Fn.BinaryOperators.ofMergeToFirst();
            Joiner result = operator.apply(joiner1, joiner2);

            assertSame(joiner1, result);
            assertEquals("a,b,c,d", result.toString());
        }

        @Test
        public void testOfMergeToBigger() {
            Joiner joiner1 = Joiner.with(",");
            joiner1.append("a").append("b").append("c");

            Joiner joiner2 = Joiner.with(",");
            joiner2.append("d");

            BinaryOperator<Joiner> operator = Fn.BinaryOperators.ofMergeToBigger();

            Joiner result1 = operator.apply(joiner1, joiner2);
            assertSame(joiner1, result1);
            assertTrue(result1.toString().contains("a,b,c"));

            joiner1 = Joiner.with(",");
            joiner1.append("a");

            joiner2 = Joiner.with(",");
            joiner2.append("b").append("c").append("d");

            Joiner result2 = operator.apply(joiner1, joiner2);
            assertSame(joiner2, result2);
            assertTrue(result2.toString().contains("b,c,d"));
        }

        @Test
        public void testOfAppend_Deprecated() {
            StringBuilder sb1 = new StringBuilder("Hello");
            StringBuilder sb2 = new StringBuilder(" World");

            BinaryOperator<StringBuilder> operator = Fn.BinaryOperators.ofAppend();
            StringBuilder result = operator.apply(sb1, sb2);

            assertSame(sb1, result);
            assertEquals("Hello World", result.toString());
        }

        @Test
        public void testOfAppendToFirst() {
            StringBuilder sb1 = new StringBuilder("Hello");
            StringBuilder sb2 = new StringBuilder(" World");

            BinaryOperator<StringBuilder> operator = Fn.BinaryOperators.ofAppendToFirst();
            StringBuilder result = operator.apply(sb1, sb2);

            assertSame(sb1, result);
            assertEquals("Hello World", result.toString());
        }

        @Test
        public void testOfAppendToBigger() {
            StringBuilder sb1 = new StringBuilder("Hello World");
            StringBuilder sb2 = new StringBuilder("!");

            BinaryOperator<StringBuilder> operator = Fn.BinaryOperators.ofAppendToBigger();

            StringBuilder result1 = operator.apply(sb1, sb2);
            assertSame(sb1, result1);
            assertEquals("Hello World!", result1.toString());

            sb1 = new StringBuilder("Hi");
            sb2 = new StringBuilder("Hello World");

            StringBuilder result2 = operator.apply(sb1, sb2);
            assertSame(sb2, result2);
            assertEquals("Hello WorldHi", result2.toString());
        }

        @Test
        public void testOfConcat() {
            BinaryOperator<String> operator = Fn.BinaryOperators.ofConcat();
            assertEquals("HelloWorld", operator.apply("Hello", "World"));
            assertEquals("", operator.apply("", ""));
            assertEquals("test", operator.apply("test", ""));
        }

        @Test
        public void testOfAddInt() {
            BinaryOperator<Integer> operator = Fn.BinaryOperators.ofAddInt();
            assertEquals(5, operator.apply(2, 3));
            assertEquals(0, operator.apply(0, 0));
            assertEquals(-1, operator.apply(1, -2));
        }

        @Test
        public void testOfAddLong() {
            BinaryOperator<Long> operator = Fn.BinaryOperators.ofAddLong();
            assertEquals(5L, operator.apply(2L, 3L));
            assertEquals(0L, operator.apply(0L, 0L));
            assertEquals(-1L, operator.apply(1L, -2L));
        }

        @Test
        public void testOfAddDouble() {
            BinaryOperator<Double> operator = Fn.BinaryOperators.ofAddDouble();
            assertEquals(5.5, operator.apply(2.2, 3.3), 0.0001);
            assertEquals(0.0, operator.apply(0.0, 0.0), 0.0001);
            assertEquals(-1.0, operator.apply(1.5, -2.5), 0.0001);
        }

        @Test
        public void testOfAddBigInteger() {
            BinaryOperator<BigInteger> operator = Fn.BinaryOperators.ofAddBigInteger();

            BigInteger bi1 = new BigInteger("12345678901234567890");
            BigInteger bi2 = new BigInteger("98765432109876543210");
            BigInteger expected = new BigInteger("111111111011111111100");

            assertEquals(expected, operator.apply(bi1, bi2));
            assertEquals(BigInteger.ZERO, operator.apply(BigInteger.ZERO, BigInteger.ZERO));
        }

        @Test
        public void testOfAddBigDecimal() {
            BinaryOperator<BigDecimal> operator = Fn.BinaryOperators.ofAddBigDecimal();

            BigDecimal bd1 = new BigDecimal("123.456");
            BigDecimal bd2 = new BigDecimal("876.544");
            BigDecimal expected = new BigDecimal("1000.000");

            assertEquals(expected, operator.apply(bd1, bd2));
            assertEquals(BigDecimal.ZERO, operator.apply(BigDecimal.ZERO, BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("UnaryOperators Tests")
    public class UnaryOperatorsTest {

        @Test
        public void testIdentity() {
            UnaryOperator<Object> operator = UnaryOperators.identity();

            String input = "test";
            assertSame(input, operator.apply(input));

            assertNull(operator.apply(null));

            List<Integer> list = Arrays.asList(1, 2, 3);
            assertSame(list, operator.apply(list));
        }
    }

    @Nested
    @DisplayName("Entries Tests")
    public class EntriesTest {

        @Test
        public void testF() {
            BiFunction<String, Integer, String> biFunc = (k, v) -> k + ":" + v;
            Function<Map.Entry<String, Integer>, String> function = Fn.Entries.f(biFunc);

            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
            assertEquals("key:123", function.apply(entry));

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.f(null));
        }

        @Test
        public void testP() {
            BiPredicate<String, Integer> biPred = (k, v) -> v > 100;
            Predicate<Map.Entry<String, Integer>> predicate = Fn.Entries.p(biPred);

            Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 123);
            Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", 50);

            assertTrue(predicate.test(entry1));
            assertFalse(predicate.test(entry2));

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.p(null));
        }

        @Test
        public void testC() {
            List<String> consumed = new ArrayList<>();
            BiConsumer<String, Integer> biCons = (k, v) -> consumed.add(k + ":" + v);
            Consumer<Map.Entry<String, Integer>> consumer = Fn.Entries.c(biCons);

            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
            consumer.accept(entry);

            assertEquals(Arrays.asList("key:123"), consumed);

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.c(null));
        }

        @Test
        public void testEf() {
            Throwables.BiFunction<String, Integer, String, Exception> biFunc = (k, v) -> k + ":" + v;
            Throwables.Function<Map.Entry<String, Integer>, String, Exception> function = Fn.Entries.ef(biFunc);

            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
            try {
                assertEquals("key:123", function.apply(entry));
            } catch (Exception e) {
                fail("Should not throw exception");
            }

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.ef(null));
        }

        @Test
        public void testEp() {
            Throwables.BiPredicate<String, Integer, Exception> biPred = (k, v) -> v > 100;
            Throwables.Predicate<Map.Entry<String, Integer>, Exception> predicate = Fn.Entries.ep(biPred);

            Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 123);
            Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", 50);

            try {
                assertTrue(predicate.test(entry1));
                assertFalse(predicate.test(entry2));
            } catch (Exception e) {
                fail("Should not throw exception");
            }

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.ep(null));
        }

        @Test
        public void testEc() {
            List<String> consumed = new ArrayList<>();
            Throwables.BiConsumer<String, Integer, Exception> biCons = (k, v) -> consumed.add(k + ":" + v);
            Throwables.Consumer<Map.Entry<String, Integer>, Exception> consumer = Fn.Entries.ec(biCons);

            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
            try {
                consumer.accept(entry);
            } catch (Exception e) {
                fail("Should not throw exception");
            }

            assertEquals(Arrays.asList("key:123"), consumed);

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.ec(null));
        }

        @Test
        public void testFf() {
            Throwables.BiFunction<String, Integer, String, Exception> biFunc = (k, v) -> {
                if (v < 0)
                    throw new Exception("Negative value");
                return k + ":" + v;
            };
            Function<Map.Entry<String, Integer>, String> function = Fn.Entries.ff(biFunc);

            Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 123);
            assertEquals("key:123", function.apply(entry1));

            Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", -1);
            assertThrows(RuntimeException.class, () -> function.apply(entry2));

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.ff(null));
        }

        @Test
        public void testPp() {
            Throwables.BiPredicate<String, Integer, Exception> biPred = (k, v) -> {
                if (v < 0)
                    throw new Exception("Negative value");
                return v > 100;
            };
            Predicate<Map.Entry<String, Integer>> predicate = Fn.Entries.pp(biPred);

            Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 123);
            assertTrue(predicate.test(entry1));

            Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", -1);
            assertThrows(RuntimeException.class, () -> predicate.test(entry2));

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.pp(null));
        }

        @Test
        public void testCc() {
            List<String> consumed = new ArrayList<>();
            Throwables.BiConsumer<String, Integer, Exception> biCons = (k, v) -> {
                if (v < 0)
                    throw new Exception("Negative value");
                consumed.add(k + ":" + v);
            };
            Consumer<Map.Entry<String, Integer>> consumer = Fn.Entries.cc(biCons);

            Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 123);
            consumer.accept(entry1);
            assertEquals(Arrays.asList("key:123"), consumed);

            Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", -1);
            assertThrows(RuntimeException.class, () -> consumer.accept(entry2));

            assertThrows(IllegalArgumentException.class, () -> Fn.Entries.cc(null));
        }
    }

    @Nested
    @DisplayName("Pairs Tests")
    public class PairsTest {

        @Test
        public void testToList() {
            Function<Pair<String, String>, List<String>> function = Fn.Pairs.toList();

            Pair<String, String> pair = Pair.of("left", "right");
            List<String> list = function.apply(pair);

            assertEquals(Arrays.asList("left", "right"), list);
        }

        @Test
        public void testToSet() {
            Function<Pair<String, String>, Set<String>> function = Fn.Pairs.toSet();

            Pair<String, String> pair1 = Pair.of("a", "b");
            Set<String> set1 = function.apply(pair1);
            assertEquals(2, set1.size());
            assertTrue(set1.contains("a"));
            assertTrue(set1.contains("b"));

            Pair<String, String> pair2 = Pair.of("x", "x");
            Set<String> set2 = function.apply(pair2);
            assertEquals(1, set2.size());
            assertTrue(set2.contains("x"));
        }
    }

    @Nested
    @DisplayName("Triples Tests")
    public class TriplesTest {

        @Test
        public void testToList() {
            Function<Triple<String, String, String>, List<String>> function = Fn.Triples.toList();

            Triple<String, String, String> triple = Triple.of("left", "middle", "right");
            List<String> list = function.apply(triple);

            assertEquals(Arrays.asList("left", "middle", "right"), list);
        }

        @Test
        public void testToSet() {
            Function<Triple<String, String, String>, Set<String>> function = Fn.Triples.toSet();

            Triple<String, String, String> triple1 = Triple.of("a", "b", "c");
            Set<String> set1 = function.apply(triple1);
            assertEquals(3, set1.size());
            assertTrue(set1.contains("a"));
            assertTrue(set1.contains("b"));
            assertTrue(set1.contains("c"));

            Triple<String, String, String> triple2 = Triple.of("x", "x", "y");
            Set<String> set2 = function.apply(triple2);
            assertEquals(2, set2.size());
            assertTrue(set2.contains("x"));
            assertTrue(set2.contains("y"));
        }
    }

    @Nested
    @DisplayName("Disposables Tests")
    public class DisposablesTest {

        @Test
        public void testCloneArray() {
            Function<DisposableObjArray, Object[]> function = Fn.Disposables.cloneArray();

            Object[] original = new Object[] { "a", "b", "c" };
            DisposableObjArray disposable = DisposableObjArray.wrap(original);

            Object[] cloned = function.apply(disposable);
            assertArrayEquals(original, cloned);
            assertNotSame(original, cloned);
        }

        @Test
        public void testToStr() {
            Function<DisposableObjArray, String> function = Fn.Disposables.toStr();

            Object[] array = new Object[] { "a", "b", "c" };
            DisposableObjArray disposable = DisposableObjArray.wrap(array);

            String result = function.apply(disposable);
            assertNotNull(result);
            assertTrue(result.contains("a"));
            assertTrue(result.contains("b"));
            assertTrue(result.contains("c"));
        }

        @Test
        public void testJoin() {
            Function<DisposableObjArray, String> function = Fn.Disposables.join(", ");

            Object[] array = new Object[] { "a", "b", "c" };
            DisposableObjArray disposable = DisposableObjArray.wrap(array);

            String result = function.apply(disposable);
            assertEquals("a, b, c", result);
        }
    }

    @Nested
    public class FnPrimitivesTest {

        @Nested
        @DisplayName("FC (Char Functions) Tests")
        class FCTest {

            @Test
            public void testIsZero() {
                CharPredicate predicate = Fn.FC.isZero();
                assertTrue(predicate.test((char) 0));
                assertFalse(predicate.test('a'));
                assertFalse(predicate.test(' '));
            }

            @Test
            public void testIsWhitespace() {
                CharPredicate predicate = Fn.FC.isWhitespace();
                assertTrue(predicate.test(' '));
                assertTrue(predicate.test('\t'));
                assertTrue(predicate.test('\n'));
                assertFalse(predicate.test('a'));
                assertFalse(predicate.test('1'));
            }

            @Test
            public void testEqual() {
                CharBiPredicate predicate = Fn.FC.equal();
                assertTrue(predicate.test('a', 'a'));
                assertFalse(predicate.test('a', 'b'));
                assertTrue(predicate.test('\0', '\0'));
            }

            @Test
            public void testNotEqual() {
                CharBiPredicate predicate = Fn.FC.notEqual();
                assertFalse(predicate.test('a', 'a'));
                assertTrue(predicate.test('a', 'b'));
                assertFalse(predicate.test('\0', '\0'));
            }

            @Test
            public void testGreaterThan() {
                CharBiPredicate predicate = Fn.FC.greaterThan();
                assertTrue(predicate.test('b', 'a'));
                assertFalse(predicate.test('a', 'b'));
                assertFalse(predicate.test('a', 'a'));
            }

            @Test
            public void testGreaterEqual() {
                CharBiPredicate predicate = Fn.FC.greaterEqual();
                assertTrue(predicate.test('b', 'a'));
                assertFalse(predicate.test('a', 'b'));
                assertTrue(predicate.test('a', 'a'));
            }

            @Test
            public void testLessThan() {
                CharBiPredicate predicate = Fn.FC.lessThan();
                assertFalse(predicate.test('b', 'a'));
                assertTrue(predicate.test('a', 'b'));
                assertFalse(predicate.test('a', 'a'));
            }

            @Test
            public void testLessEqual() {
                CharBiPredicate predicate = Fn.FC.lessEqual();
                assertFalse(predicate.test('b', 'a'));
                assertTrue(predicate.test('a', 'b'));
                assertTrue(predicate.test('a', 'a'));
            }

            @Test
            public void testUnbox() {
                ToCharFunction<Character> function = Fn.FC.unbox();
                assertEquals('a', function.applyAsChar(Character.valueOf('a')));
                assertEquals('Z', function.applyAsChar(Character.valueOf('Z')));
            }

            @Test
            public void testP() {
                CharPredicate original = c -> c > 'a';
                CharPredicate result = Fn.FC.p(original);
                assertSame(original, result);

                assertThrows(IllegalArgumentException.class, () -> Fn.FC.p(null));
            }

            @Test
            public void testF() {
                CharFunction<String> original = c -> String.valueOf(c);
                CharFunction<String> result = Fn.FC.f(original);
                assertSame(original, result);

                assertThrows(IllegalArgumentException.class, () -> Fn.FC.f(null));
            }

            @Test
            public void testC() {
                CharConsumer original = c -> System.out.print(c);
                CharConsumer result = Fn.FC.c(original);
                assertSame(original, result);

                assertThrows(IllegalArgumentException.class, () -> Fn.FC.c(null));
            }

            @Test
            public void testLen() {
                Function<char[], Integer> function = Fn.FC.len();
                assertEquals(0, function.apply(null));
                assertEquals(0, function.apply(new char[0]));
                assertEquals(3, function.apply(new char[] { 'a', 'b', 'c' }));
            }

            @Test
            public void testAlternate() {
                CharBiFunction<MergeResult> function = Fn.FC.alternate();

                assertEquals(MergeResult.TAKE_FIRST, function.apply('a', 'b'));
                assertEquals(MergeResult.TAKE_SECOND, function.apply('c', 'd'));
                assertEquals(MergeResult.TAKE_FIRST, function.apply('e', 'f'));
            }

            @Test
            public void testCharBinaryOperators() {
                assertEquals('a', Fn.FC.CharBinaryOperators.MIN.applyAsChar('a', 'b'));
                assertEquals('a', Fn.FC.CharBinaryOperators.MIN.applyAsChar('b', 'a'));

                assertEquals('b', Fn.FC.CharBinaryOperators.MAX.applyAsChar('a', 'b'));
                assertEquals('b', Fn.FC.CharBinaryOperators.MAX.applyAsChar('b', 'a'));
            }
        }

        @Nested
        @DisplayName("FB (Byte Functions) Tests")
        class FBTest {

            @Test
            public void testPositive() {
                BytePredicate predicate = Fn.FB.positive();
                assertTrue(predicate.test((byte) 1));
                assertTrue(predicate.test((byte) 127));
                assertFalse(predicate.test((byte) 0));
                assertFalse(predicate.test((byte) -1));
            }

            @Test
            public void testNotNegative() {
                BytePredicate predicate = Fn.FB.notNegative();
                assertTrue(predicate.test((byte) 1));
                assertTrue(predicate.test((byte) 0));
                assertFalse(predicate.test((byte) -1));
            }

            @Test
            public void testBinaryPredicates() {
                assertTrue(Fn.FB.equal().test((byte) 5, (byte) 5));
                assertFalse(Fn.FB.equal().test((byte) 5, (byte) 6));

                assertTrue(Fn.FB.notEqual().test((byte) 5, (byte) 6));
                assertFalse(Fn.FB.notEqual().test((byte) 5, (byte) 5));

                assertTrue(Fn.FB.greaterThan().test((byte) 6, (byte) 5));
                assertFalse(Fn.FB.greaterThan().test((byte) 5, (byte) 5));

                assertTrue(Fn.FB.greaterEqual().test((byte) 5, (byte) 5));
                assertTrue(Fn.FB.greaterEqual().test((byte) 6, (byte) 5));

                assertTrue(Fn.FB.lessThan().test((byte) 5, (byte) 6));
                assertFalse(Fn.FB.lessThan().test((byte) 5, (byte) 5));

                assertTrue(Fn.FB.lessEqual().test((byte) 5, (byte) 5));
                assertTrue(Fn.FB.lessEqual().test((byte) 5, (byte) 6));
            }

            @Test
            public void testUnbox() {
                ToByteFunction<Byte> function = Fn.FB.unbox();
                assertEquals((byte) 5, function.applyAsByte(Byte.valueOf((byte) 5)));
            }

            @Test
            public void testLen() {
                Function<byte[], Integer> function = Fn.FB.len();
                assertEquals(0, function.apply(null));
                assertEquals(0, function.apply(new byte[0]));
                assertEquals(3, function.apply(new byte[] { 1, 2, 3 }));
            }

            @Test
            public void testSum() {
                Function<byte[], Integer> function = Fn.FB.sum();
                byte[] array = new byte[] { 1, 2, 3, 4, 5 };
                assertEquals(15, function.apply(array));
            }

            @Test
            public void testAverage() {
                Function<byte[], Double> function = Fn.FB.average();
                byte[] array = new byte[] { 1, 2, 3, 4, 5 };
                assertEquals(3.0, function.apply(array), 0.0001);
            }

            @Test
            public void testByteBinaryOperators() {
                assertEquals((byte) 3, Fn.FB.ByteBinaryOperators.MIN.applyAsByte((byte) 3, (byte) 5));
                assertEquals((byte) 5, Fn.FB.ByteBinaryOperators.MAX.applyAsByte((byte) 3, (byte) 5));
            }
        }

        @Nested
        @DisplayName("FS (Short Functions) Tests")
        class FSTest {

            @Test
            public void testPositive() {
                ShortPredicate predicate = Fn.FS.positive();
                assertTrue(predicate.test((short) 1));
                assertFalse(predicate.test((short) 0));
                assertFalse(predicate.test((short) -1));
            }

            @Test
            public void testNotNegative() {
                ShortPredicate predicate = Fn.FS.notNegative();
                assertTrue(predicate.test((short) 1));
                assertTrue(predicate.test((short) 0));
                assertFalse(predicate.test((short) -1));
            }

            @Test
            public void testSum() {
                Function<short[], Integer> function = Fn.FS.sum();
                short[] array = new short[] { 1, 2, 3, 4, 5 };
                assertEquals(15, function.apply(array));
            }

            @Test
            public void testAverage() {
                Function<short[], Double> function = Fn.FS.average();
                short[] array = new short[] { 1, 2, 3, 4, 5 };
                assertEquals(3.0, function.apply(array), 0.0001);
            }

            @Test
            public void testShortBinaryOperators() {
                assertEquals((short) 3, Fn.FS.ShortBinaryOperators.MIN.applyAsShort((short) 3, (short) 5));
                assertEquals((short) 5, Fn.FS.ShortBinaryOperators.MAX.applyAsShort((short) 3, (short) 5));
            }
        }

        @Nested
        @DisplayName("FI (Int Functions) Tests")
        class FITest {

            @Test
            public void testPositive() {
                IntPredicate predicate = Fn.FI.positive();
                assertTrue(predicate.test(1));
                assertFalse(predicate.test(0));
                assertFalse(predicate.test(-1));
            }

            @Test
            public void testNotNegative() {
                IntPredicate predicate = Fn.FI.notNegative();
                assertTrue(predicate.test(1));
                assertTrue(predicate.test(0));
                assertFalse(predicate.test(-1));
            }

            @Test
            public void testSum() {
                Function<int[], Integer> function = Fn.FI.sum();
                int[] array = new int[] { 1, 2, 3, 4, 5 };
                assertEquals(15, function.apply(array));
            }

            @Test
            public void testAverage() {
                Function<int[], Double> function = Fn.FI.average();
                int[] array = new int[] { 1, 2, 3, 4, 5 };
                assertEquals(3.0, function.apply(array), 0.0001);
            }

            @Test
            public void testIntBinaryOperators() {
                assertEquals(3, Fn.FI.IntBinaryOperators.MIN.applyAsInt(3, 5));
                assertEquals(5, Fn.FI.IntBinaryOperators.MAX.applyAsInt(3, 5));
            }
        }

        @Nested
        @DisplayName("FL (Long Functions) Tests")
        class FLTest {

            @Test
            public void testPositive() {
                LongPredicate predicate = Fn.FL.positive();
                assertTrue(predicate.test(1L));
                assertFalse(predicate.test(0L));
                assertFalse(predicate.test(-1L));
            }

            @Test
            public void testNotNegative() {
                LongPredicate predicate = Fn.FL.notNegative();
                assertTrue(predicate.test(1L));
                assertTrue(predicate.test(0L));
                assertFalse(predicate.test(-1L));
            }

            @Test
            public void testSum() {
                Function<long[], Long> function = Fn.FL.sum();
                long[] array = new long[] { 1L, 2L, 3L, 4L, 5L };
                assertEquals(15L, function.apply(array));
            }

            @Test
            public void testAverage() {
                Function<long[], Double> function = Fn.FL.average();
                long[] array = new long[] { 1L, 2L, 3L, 4L, 5L };
                assertEquals(3.0, function.apply(array), 0.0001);
            }

            @Test
            public void testLongBinaryOperators() {
                assertEquals(3L, Fn.FL.LongBinaryOperators.MIN.applyAsLong(3L, 5L));
                assertEquals(5L, Fn.FL.LongBinaryOperators.MAX.applyAsLong(3L, 5L));
            }
        }

        @Nested
        @DisplayName("FF (Float Functions) Tests")
        class FFTest {

            @Test
            public void testPositive() {
                FloatPredicate predicate = Fn.FF.positive();
                assertTrue(predicate.test(1.0f));
                assertFalse(predicate.test(0.0f));
                assertFalse(predicate.test(-1.0f));
            }

            @Test
            public void testNotNegative() {
                FloatPredicate predicate = Fn.FF.notNegative();
                assertTrue(predicate.test(1.0f));
                assertTrue(predicate.test(0.0f));
                assertFalse(predicate.test(-1.0f));
            }

            @Test
            public void testEqual() {
                FloatBiPredicate predicate = Fn.FF.equal();
                assertTrue(predicate.test(1.0f, 1.0f));
                assertFalse(predicate.test(1.0f, 1.1f));
                assertTrue(predicate.test(Float.NaN, Float.NaN));
            }

            @Test
            public void testSum() {
                Function<float[], Float> function = Fn.FF.sum();
                float[] array = new float[] { 1.0f, 2.0f, 3.0f };
                assertEquals(6.0f, function.apply(array), 0.0001f);
            }

            @Test
            public void testAverage() {
                Function<float[], Double> function = Fn.FF.average();
                float[] array = new float[] { 1.0f, 2.0f, 3.0f };
                assertEquals(2.0, function.apply(array), 0.0001);
            }

            @Test
            public void testFloatBinaryOperators() {
                assertEquals(3.0f, Fn.FF.FloatBinaryOperators.MIN.applyAsFloat(3.0f, 5.0f));
                assertEquals(5.0f, Fn.FF.FloatBinaryOperators.MAX.applyAsFloat(3.0f, 5.0f));
            }
        }

        @Nested
        @DisplayName("FD (Double Functions) Tests")
        class FDTest {

            @Test
            public void testPositive() {
                DoublePredicate predicate = Fn.FD.positive();
                assertTrue(predicate.test(1.0));
                assertFalse(predicate.test(0.0));
                assertFalse(predicate.test(-1.0));
            }

            @Test
            public void testNotNegative() {
                DoublePredicate predicate = Fn.FD.notNegative();
                assertTrue(predicate.test(1.0));
                assertTrue(predicate.test(0.0));
                assertFalse(predicate.test(-1.0));
            }

            @Test
            public void testEqual() {
                DoubleBiPredicate predicate = Fn.FD.equal();
                assertTrue(predicate.test(1.0, 1.0));
                assertFalse(predicate.test(1.0, 1.1));
                assertTrue(predicate.test(Double.NaN, Double.NaN));
            }

            @Test
            public void testSum() {
                Function<double[], Double> function = Fn.FD.sum();
                double[] array = new double[] { 1.0, 2.0, 3.0 };
                assertEquals(6.0, function.apply(array), 0.0001);
            }

            @Test
            public void testAverage() {
                Function<double[], Double> function = Fn.FD.average();
                double[] array = new double[] { 1.0, 2.0, 3.0 };
                assertEquals(2.0, function.apply(array), 0.0001);
            }

            @Test
            public void testDoubleBinaryOperators() {
                assertEquals(3.0, Fn.FD.DoubleBinaryOperators.MIN.applyAsDouble(3.0, 5.0));
                assertEquals(5.0, Fn.FD.DoubleBinaryOperators.MAX.applyAsDouble(3.0, 5.0));
            }
        }
    }

    @Nested
    public class FnFnnTest {

        @Nested
        @DisplayName("Memoization Tests")
        class MemoizationTest {

            @Test
            public void testMemoize_Supplier() {
                AtomicInteger counter = new AtomicInteger();
                Throwables.Supplier<String, Exception> original = () -> "Value-" + counter.incrementAndGet();

                Throwables.Supplier<String, Exception> memoized = Fnn.memoize(original);

                try {
                    String result1 = memoized.get();
                    String result2 = memoized.get();
                    String result3 = memoized.get();

                    assertEquals("Value-1", result1);
                    assertEquals("Value-1", result2);
                    assertEquals("Value-1", result3);
                    assertEquals(1, counter.get());
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testMemoizeWithExpiration() throws Exception {
                AtomicInteger counter = new AtomicInteger();
                Throwables.Supplier<String, Exception> original = () -> "Value-" + counter.incrementAndGet();

                Throwables.Supplier<String, Exception> memoized = Fnn.memoizeWithExpiration(original, 100, TimeUnit.MILLISECONDS);

                String result1 = memoized.get();
                assertEquals("Value-1", result1);

                Thread.sleep(50);
                String result2 = memoized.get();
                assertEquals("Value-1", result2);

                Thread.sleep(100);
                String result3 = memoized.get();
                assertEquals("Value-2", result3);

                assertEquals(2, counter.get());

                assertThrows(IllegalArgumentException.class, () -> Fnn.memoizeWithExpiration(original, -1, TimeUnit.MILLISECONDS));
                assertThrows(IllegalArgumentException.class, () -> Fnn.memoizeWithExpiration(null, 100, TimeUnit.MILLISECONDS));
            }

            @Test
            public void testMemoize_Function() throws Exception {
                AtomicInteger counter = new AtomicInteger();
                Throwables.Function<String, String, Exception> original = s -> s + "-" + counter.incrementAndGet();

                Throwables.Function<String, String, Exception> memoized = Fnn.memoize(original);

                assertEquals("A-1", memoized.apply("A"));
                assertEquals("A-1", memoized.apply("A"));
                assertEquals("B-2", memoized.apply("B"));
                assertEquals("B-2", memoized.apply("B"));
                assertEquals("A-1", memoized.apply("A"));

                assertEquals(2, counter.get());

                assertEquals("null-3", memoized.apply(null));
                assertEquals("null-3", memoized.apply(null));
            }
        }

        @Nested
        @DisplayName("Identity and Constant Functions Tests")
        class IdentityConstantTest {

            @Test
            public void testIdentity() throws Exception {
                Throwables.Function<Object, Object, Exception> identity = Fnn.identity();

                assertEquals("test", identity.apply("test"));
                assertNull(identity.apply(null));

                List<Integer> list = Arrays.asList(1, 2, 3);
                assertSame(list, identity.apply(list));
            }

            @Test
            public void testAlwaysTrue() throws Exception {
                Throwables.Predicate<String, Exception> predicate = Fnn.alwaysTrue();

                assertTrue(predicate.test("test"));
                assertTrue(predicate.test(null));
                assertTrue(predicate.test(""));
            }

            @Test
            public void testAlwaysFalse() throws Exception {
                Throwables.Predicate<String, Exception> predicate = Fnn.alwaysFalse();

                assertFalse(predicate.test("test"));
                assertFalse(predicate.test(null));
                assertFalse(predicate.test(""));
            }

            @Test
            public void testToStr() throws Exception {
                Throwables.Function<Object, String, Exception> function = Fnn.toStr();

                assertEquals("test", function.apply("test"));
                assertEquals("123", function.apply(123));
                assertEquals("null", function.apply(null));
            }
        }

        @Nested
        @DisplayName("Entry and Pair Functions Tests")
        class EntryPairTest {

            @Test
            public void testKey() throws Exception {
                Throwables.Function<Map.Entry<String, Integer>, String, Exception> function = Fnn.key();

                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
                assertEquals("key", function.apply(entry));
            }

            @Test
            public void testValue() throws Exception {
                Throwables.Function<Map.Entry<String, Integer>, Integer, Exception> function = Fnn.value();

                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
                assertEquals(123, function.apply(entry));
            }

            @Test
            public void testInverse() throws Exception {
                Throwables.Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>, Exception> function = Fnn.inverse();

                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
                Map.Entry<Integer, String> inverted = function.apply(entry);

                assertEquals(123, inverted.getKey());
                assertEquals("key", inverted.getValue());
            }

            @Test
            public void testEntry() throws Exception {
                Throwables.BiFunction<String, Integer, Map.Entry<String, Integer>, Exception> function = Fnn.entry();

                Map.Entry<String, Integer> entry = function.apply("key", 123);
                assertEquals("key", entry.getKey());
                assertEquals(123, entry.getValue());
            }

            @Test
            public void testPair() throws Exception {
                Throwables.BiFunction<String, Integer, Pair<String, Integer>, Exception> function = Fnn.pair();

                Pair<String, Integer> pair = function.apply("left", 123);
                assertEquals("left", pair.left());
                assertEquals(123, pair.right());
            }

            @Test
            public void testTriple() throws Exception {
                Throwables.TriFunction<String, Integer, Boolean, Triple<String, Integer, Boolean>, Exception> function = Fnn.triple();

                Triple<String, Integer, Boolean> triple = function.apply("left", 123, true);
                assertEquals("left", triple.left());
                assertEquals(123, triple.middle());
                assertEquals(true, triple.right());
            }
        }

        @Nested
        @DisplayName("Tuple Functions Tests")
        class TupleTest {

            @Test
            public void testTuple1() throws Exception {
                Throwables.Function<String, Tuple1<String>, Exception> function = Fnn.tuple1();

                Tuple1<String> tuple = function.apply("value");
                assertEquals("value", tuple._1);
            }

            @Test
            public void testTuple2() throws Exception {
                Throwables.BiFunction<String, Integer, Tuple2<String, Integer>, Exception> function = Fnn.tuple2();

                Tuple2<String, Integer> tuple = function.apply("value", 123);
                assertEquals("value", tuple._1);
                assertEquals(123, tuple._2);
            }

            @Test
            public void testTuple3() throws Exception {
                Throwables.TriFunction<String, Integer, Boolean, Tuple3<String, Integer, Boolean>, Exception> function = Fnn.tuple3();

                Tuple3<String, Integer, Boolean> tuple = function.apply("value", 123, true);
                assertEquals("value", tuple._1);
                assertEquals(123, tuple._2);
                assertEquals(true, tuple._3);
            }
        }

        @Nested
        @DisplayName("Action and Consumer Tests")
        class ActionConsumerTest {

            @Test
            public void testEmptyAction() throws Exception {
                Throwables.Runnable<Exception> action = Fnn.emptyAction();
                action.run();
            }

            @Test
            public void testDoNothing() throws Exception {
                Throwables.Consumer<String, Exception> consumer = Fnn.doNothing();
                consumer.accept("test");
                consumer.accept(null);
            }

            @Test
            public void testThrowRuntimeException() {
                Throwables.Consumer<String, RuntimeException> consumer = Fnn.throwRuntimeException("Test error");

                RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.accept("test"));
                assertEquals("Test error", ex.getMessage());
            }

            @Test
            public void testThrowIOException() {
                Throwables.Consumer<String, IOException> consumer = Fnn.throwIOException("IO error");

                IOException ex = assertThrows(IOException.class, () -> consumer.accept("test"));
                assertEquals("IO error", ex.getMessage());
            }

            @Test
            public void testThrowException() {
                Throwables.Consumer<String, Exception> consumer = Fnn.throwException("General error");

                Exception ex = assertThrows(Exception.class, () -> consumer.accept("test"));
                assertEquals("General error", ex.getMessage());
            }

            @Test
            public void testThrowExceptionWithSupplier() {
                Throwables.Consumer<String, IllegalArgumentException> consumer = Fnn.throwException(() -> new IllegalArgumentException("Supplied error"));

                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> consumer.accept("test"));
                assertEquals("Supplied error", ex.getMessage());
            }

            @Test
            public void testSleep() throws Exception {
                Throwables.Consumer<String, Exception> consumer = Fnn.sleep(10);

                long start = System.currentTimeMillis();
                consumer.accept("test");
                long duration = System.currentTimeMillis() - start;

                assertTrue(duration >= 10);
            }

            @Test
            public void testSleepUninterruptibly() throws Exception {
                Throwables.Consumer<String, Exception> consumer = Fnn.sleepUninterruptibly(10);

                long start = System.currentTimeMillis();
                consumer.accept("test");
                long duration = System.currentTimeMillis() - start;

                assertTrue(duration >= 10);
            }

            @Test
            public void testRateLimiter() throws Exception {
                Throwables.Consumer<String, Exception> consumer = Fnn.rateLimiter(2.0);

                long start = System.currentTimeMillis();
                consumer.accept("1");
                consumer.accept("2");
                consumer.accept("3");
                long duration = System.currentTimeMillis() - start;

                assertTrue(duration >= 500);
            }

            @Test
            public void testClose() throws Exception {
                Throwables.Consumer<AutoCloseable, Exception> consumer = Fnn.close();

                AtomicInteger closed = new AtomicInteger();
                AutoCloseable closeable = () -> closed.incrementAndGet();

                consumer.accept(closeable);
                assertEquals(1, closed.get());

                consumer.accept(null);
            }

            @Test
            public void testCloseQuietly() {
                Throwables.Consumer<AutoCloseable, RuntimeException> consumer = Fnn.closeQuietly();

                AtomicInteger closed = new AtomicInteger();
                AutoCloseable closeable = () -> closed.incrementAndGet();

                consumer.accept(closeable);
                assertEquals(1, closed.get());

                AutoCloseable throwingCloseable = () -> {
                    throw new IOException("Close failed");
                };

                assertDoesNotThrow(() -> consumer.accept(throwingCloseable));
            }

            @Test
            public void testPrintln() throws Exception {
                Throwables.Consumer<String, Exception> consumer = Fnn.println();
                consumer.accept("test");
            }

            @Test
            public void testPrintlnWithSeparator() throws Exception {
                Throwables.BiConsumer<String, Integer, Exception> consumer = Fnn.println(" - ");
                consumer.accept("test", 123);
            }
        }

        @Nested
        @DisplayName("Predicate Tests")
        class PredicateTest {

            @Test
            public void testIsNull() throws Exception {
                Throwables.Predicate<String, Exception> predicate = Fnn.isNull();

                assertTrue(predicate.test(null));
                assertFalse(predicate.test("test"));
                assertFalse(predicate.test(""));
            }

            @Test
            public void testIsEmpty() throws Exception {
                Throwables.Predicate<CharSequence, Exception> predicate = Fnn.isEmpty();

                assertTrue(predicate.test(""));
                assertFalse(predicate.test("test"));
                assertFalse(predicate.test(" "));
            }

            @Test
            public void testIsBlank() throws Exception {
                Throwables.Predicate<CharSequence, Exception> predicate = Fnn.isBlank();

                assertTrue(predicate.test(""));
                assertTrue(predicate.test(" "));
                assertTrue(predicate.test("\t\n"));
                assertFalse(predicate.test("test"));
                assertFalse(predicate.test(" test "));
            }

            @Test
            public void testIsEmptyA() throws Exception {
                Throwables.Predicate<String[], Exception> predicate = Fnn.isEmptyA();

                assertTrue(predicate.test(null));
                assertTrue(predicate.test(new String[0]));
                assertFalse(predicate.test(new String[] { "a" }));
            }

            @Test
            public void testIsEmptyC() throws Exception {
                Throwables.Predicate<Collection<String>, Exception> predicate = Fnn.isEmptyC();

                assertTrue(predicate.test(null));
                assertTrue(predicate.test(new ArrayList<>()));
                assertFalse(predicate.test(Arrays.asList("a")));
            }

            @Test
            public void testIsEmptyM() throws Exception {
                Throwables.Predicate<Map<String, String>, Exception> predicate = Fnn.isEmptyM();

                assertTrue(predicate.test(null));
                assertTrue(predicate.test(new HashMap<>()));

                Map<String, String> map = new HashMap<>();
                map.put("key", "value");
                assertFalse(predicate.test(map));
            }

            @Test
            public void testNotNull() throws Exception {
                Throwables.Predicate<String, Exception> predicate = Fnn.notNull();

                assertFalse(predicate.test(null));
                assertTrue(predicate.test("test"));
                assertTrue(predicate.test(""));
            }

            @Test
            public void testNotEmpty() throws Exception {
                Throwables.Predicate<CharSequence, Exception> predicate = Fnn.notEmpty();

                assertFalse(predicate.test(""));
                assertTrue(predicate.test("test"));
                assertTrue(predicate.test(" "));
            }

            @Test
            public void testNotBlank() throws Exception {
                Throwables.Predicate<CharSequence, Exception> predicate = Fnn.notBlank();

                assertFalse(predicate.test(""));
                assertFalse(predicate.test(" "));
                assertFalse(predicate.test("\t\n"));
                assertTrue(predicate.test("test"));
                assertTrue(predicate.test(" test "));
            }

            @Test
            public void testNotEmptyA() throws Exception {
                Throwables.Predicate<String[], Exception> predicate = Fnn.notEmptyA();

                assertFalse(predicate.test(null));
                assertFalse(predicate.test(new String[0]));
                assertTrue(predicate.test(new String[] { "a" }));
            }

            @Test
            public void testNotEmptyC() throws Exception {
                Throwables.Predicate<Collection<String>, Exception> predicate = Fnn.notEmptyC();

                assertFalse(predicate.test(null));
                assertFalse(predicate.test(new ArrayList<>()));
                assertTrue(predicate.test(Arrays.asList("a")));
            }

            @Test
            public void testNotEmptyM() throws Exception {
                Throwables.Predicate<Map<String, String>, Exception> predicate = Fnn.notEmptyM();

                assertFalse(predicate.test(null));
                assertFalse(predicate.test(new HashMap<>()));

                Map<String, String> map = new HashMap<>();
                map.put("key", "value");
                assertTrue(predicate.test(map));
            }
        }

        @Nested
        @DisplayName("Binary Operator Tests")
        class BinaryOperatorTest {

            @Test
            public void testThrowingMerger() {
                Throwables.BinaryOperator<String, Exception> merger = Fnn.throwingMerger();

                assertThrows(IllegalStateException.class, () -> merger.apply("a", "b"));
            }

            @Test
            public void testIgnoringMerger() throws Exception {
                Throwables.BinaryOperator<String, Exception> merger = Fnn.ignoringMerger();

                assertEquals("first", merger.apply("first", "second"));
            }

            @Test
            public void testReplacingMerger() throws Exception {
                Throwables.BinaryOperator<String, Exception> merger = Fnn.replacingMerger();

                assertEquals("second", merger.apply("first", "second"));
            }
        }

        @Nested
        @DisplayName("Map Entry Function Tests")
        class MapEntryFunctionTest {

            @Test
            public void testTestByKey() throws Exception {
                Throwables.Predicate<String, Exception> keyPredicate = s -> s.length() > 3;
                Throwables.Predicate<Map.Entry<String, Integer>, Exception> entryPredicate = Fnn.testByKey(keyPredicate);

                Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("test", 123);
                Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("ab", 456);

                assertTrue(entryPredicate.test(entry1));
                assertFalse(entryPredicate.test(entry2));

                assertThrows(IllegalArgumentException.class, () -> Fnn.testByKey(null));
            }

            @Test
            public void testTestByValue() throws Exception {
                Throwables.Predicate<Integer, Exception> valuePredicate = v -> v > 200;
                Throwables.Predicate<Map.Entry<String, Integer>, Exception> entryPredicate = Fnn.testByValue(valuePredicate);

                Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("a", 300);
                Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("b", 100);

                assertTrue(entryPredicate.test(entry1));
                assertFalse(entryPredicate.test(entry2));

                assertThrows(IllegalArgumentException.class, () -> Fnn.testByValue(null));
            }

            @Test
            public void testAcceptByKey() throws Exception {
                List<String> consumed = new ArrayList<>();
                Throwables.Consumer<String, Exception> keyConsumer = consumed::add;
                Throwables.Consumer<Map.Entry<String, Integer>, Exception> entryConsumer = Fnn.acceptByKey(keyConsumer);

                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
                entryConsumer.accept(entry);

                assertEquals(Arrays.asList("key"), consumed);

                assertThrows(IllegalArgumentException.class, () -> Fnn.acceptByKey(null));
            }

            @Test
            public void testAcceptByValue() throws Exception {
                List<Integer> consumed = new ArrayList<>();
                Throwables.Consumer<Integer, Exception> valueConsumer = consumed::add;
                Throwables.Consumer<Map.Entry<String, Integer>, Exception> entryConsumer = Fnn.acceptByValue(valueConsumer);

                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
                entryConsumer.accept(entry);

                assertEquals(Arrays.asList(123), consumed);

                assertThrows(IllegalArgumentException.class, () -> Fnn.acceptByValue(null));
            }

            @Test
            public void testApplyByKey() throws Exception {
                Throwables.Function<String, Integer, Exception> keyFunction = String::length;
                Throwables.Function<Map.Entry<String, Integer>, Integer, Exception> entryFunction = Fnn.applyByKey(keyFunction);

                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("test", 123);
                assertEquals(4, entryFunction.apply(entry));

                assertThrows(IllegalArgumentException.class, () -> Fnn.applyByKey(null));
            }

            @Test
            public void testApplyByValue() throws Exception {
                Throwables.Function<Integer, String, Exception> valueFunction = Object::toString;
                Throwables.Function<Map.Entry<String, Integer>, String, Exception> entryFunction = Fnn.applyByValue(valueFunction);

                Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
                assertEquals("123", entryFunction.apply(entry));

                assertThrows(IllegalArgumentException.class, () -> Fnn.applyByValue(null));
            }
        }

        @Nested
        @DisplayName("Select Function Tests")
        class SelectFunctionTest {

            @Test
            public void testSelectFirst() throws Exception {
                Throwables.BinaryOperator<String, Exception> operator = Fnn.selectFirst();

                assertEquals("first", operator.apply("first", "second"));
                assertEquals("a", operator.apply("a", "b"));
                assertNull(operator.apply(null, "second"));
            }

            @Test
            public void testSelectSecond() throws Exception {
                Throwables.BinaryOperator<String, Exception> operator = Fnn.selectSecond();

                assertEquals("second", operator.apply("first", "second"));
                assertEquals("b", operator.apply("a", "b"));
                assertNull(operator.apply("first", null));
            }
        }

        @Nested
        @DisplayName("Min/Max Function Tests")
        class MinMaxFunctionTest {

            @Test
            public void testMin() throws Exception {
                Throwables.BinaryOperator<Integer, Exception> operator = Fnn.min();

                assertEquals(3, operator.apply(5, 3));
                assertEquals(3, operator.apply(3, 5));
                assertEquals(-1, operator.apply(-1, 0));
            }

            @Test
            public void testMinWithComparator() throws Exception {
                Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
                Throwables.BinaryOperator<String, Exception> operator = Fnn.min(lengthComparator);

                assertEquals("ab", operator.apply("ab", "abc"));
                assertEquals("ab", operator.apply("abc", "ab"));

                assertThrows(IllegalArgumentException.class, () -> Fnn.min(null));
            }

            @Test
            public void testMinBy() throws Exception {
                Throwables.BinaryOperator<String, Exception> operator = Fnn.minBy(String::length);

                assertEquals("ab", operator.apply("ab", "abc"));
                assertEquals("ab", operator.apply("abc", "ab"));

                assertThrows(IllegalArgumentException.class, () -> Fnn.minBy(null));
            }

            @Test
            public void testMinByKey() throws Exception {
                Throwables.BinaryOperator<Map.Entry<Integer, String>, Exception> operator = Fnn.minByKey();

                Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<>(5, "five");
                Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<>(3, "three");

                assertSame(entry2, operator.apply(entry1, entry2));
                assertSame(entry2, operator.apply(entry2, entry1));
            }

            @Test
            public void testMinByValue() throws Exception {
                Throwables.BinaryOperator<Map.Entry<String, Integer>, Exception> operator = Fnn.minByValue();

                Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("five", 5);
                Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("three", 3);

                assertSame(entry2, operator.apply(entry1, entry2));
                assertSame(entry2, operator.apply(entry2, entry1));
            }

            @Test
            public void testMax() throws Exception {
                Throwables.BinaryOperator<Integer, Exception> operator = Fnn.max();

                assertEquals(5, operator.apply(5, 3));
                assertEquals(5, operator.apply(3, 5));
                assertEquals(0, operator.apply(-1, 0));
            }

            @Test
            public void testMaxWithComparator() throws Exception {
                Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
                Throwables.BinaryOperator<String, Exception> operator = Fnn.max(lengthComparator);

                assertEquals("abc", operator.apply("ab", "abc"));
                assertEquals("abc", operator.apply("abc", "ab"));

                assertThrows(IllegalArgumentException.class, () -> Fnn.max(null));
            }

            @Test
            public void testMaxBy() throws Exception {
                Throwables.BinaryOperator<String, Exception> operator = Fnn.maxBy(String::length);

                assertEquals("abc", operator.apply("ab", "abc"));
                assertEquals("abc", operator.apply("abc", "ab"));

                assertThrows(IllegalArgumentException.class, () -> Fnn.maxBy(null));
            }

            @Test
            public void testMaxByKey() throws Exception {
                Throwables.BinaryOperator<Map.Entry<Integer, String>, Exception> operator = Fnn.maxByKey();

                Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<>(5, "five");
                Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<>(3, "three");

                assertSame(entry1, operator.apply(entry1, entry2));
                assertSame(entry1, operator.apply(entry2, entry1));
            }

            @Test
            public void testMaxByValue() throws Exception {
                Throwables.BinaryOperator<Map.Entry<String, Integer>, Exception> operator = Fnn.maxByValue();

                Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("five", 5);
                Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("three", 3);

                assertSame(entry1, operator.apply(entry1, entry2));
                assertSame(entry1, operator.apply(entry2, entry1));
            }
        }

        @Nested
        @DisplayName("Not Function Tests")
        class NotFunctionTest {

            @Test
            public void testNotPredicate() throws Exception {
                Throwables.Predicate<Integer, Exception> isPositive = i -> i > 0;
                Throwables.Predicate<Integer, Exception> notPositive = Fnn.not(isPositive);

                assertFalse(notPositive.test(5));
                assertTrue(notPositive.test(-5));
                assertTrue(notPositive.test(0));

                assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.Predicate<?, ?>) null));
            }

            @Test
            public void testNotBiPredicate() throws Exception {
                Throwables.BiPredicate<Integer, Integer, Exception> isGreater = (a, b) -> a > b;
                Throwables.BiPredicate<Integer, Integer, Exception> notGreater = Fnn.not(isGreater);

                assertFalse(notGreater.test(5, 3));
                assertTrue(notGreater.test(3, 5));
                assertTrue(notGreater.test(5, 5));

                assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.BiPredicate<?, ?, ?>) null));
            }

            @Test
            public void testNotTriPredicate() throws Exception {
                Throwables.TriPredicate<Integer, Integer, Integer, Exception> allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
                Throwables.TriPredicate<Integer, Integer, Integer, Exception> notAllPositive = Fnn.not(allPositive);

                assertFalse(notAllPositive.test(1, 2, 3));
                assertTrue(notAllPositive.test(-1, 2, 3));
                assertTrue(notAllPositive.test(1, -2, 3));
                assertTrue(notAllPositive.test(1, 2, -3));

                assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.TriPredicate<?, ?, ?, ?>) null));
            }
        }

        @Nested
        @DisplayName("AtMost Test")
        class AtMostTest {

            @Test
            public void testAtMost() throws Exception {
                Throwables.Predicate<String, Exception> predicate = Fnn.atMost(3);

                assertTrue(predicate.test("a"));
                assertTrue(predicate.test("b"));
                assertTrue(predicate.test("c"));
                assertFalse(predicate.test("d"));
                assertFalse(predicate.test("e"));

                assertThrows(IllegalArgumentException.class, () -> Fnn.atMost(-1));
            }
        }

        @Nested
        @DisplayName("From Function Tests")
        class FromFunctionTest {

            @Test
            public void testFromSupplier() {
                Supplier<String> javaSupplier = () -> "test";
                Throwables.Supplier<String, Exception> throwableSupplier = Fnn.from(javaSupplier);

                assertNotNull(throwableSupplier);
                try {
                    assertEquals("test", throwableSupplier.get());
                } catch (Exception e) {
                    fail("Should not throw exception");
                }

            }

            @Test
            public void testFromIntFunction() {
                IntFunction<String> javaFunction = i -> "Number: " + i;
                Throwables.IntFunction<String, Exception> throwableFunction = Fnn.from(javaFunction);

                assertNotNull(throwableFunction);
                try {
                    assertEquals("Number: 5", throwableFunction.apply(5));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromPredicate() {
                Predicate<String> javaPredicate = s -> s.length() > 3;
                Throwables.Predicate<String, Exception> throwablePredicate = Fnn.from(javaPredicate);

                assertNotNull(throwablePredicate);
                try {
                    assertTrue(throwablePredicate.test("test"));
                    assertFalse(throwablePredicate.test("ab"));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromBiPredicate() {
                BiPredicate<String, Integer> javaBiPredicate = (s, i) -> s.length() == i;
                Throwables.BiPredicate<String, Integer, Exception> throwableBiPredicate = Fnn.from(javaBiPredicate);

                assertNotNull(throwableBiPredicate);
                try {
                    assertTrue(throwableBiPredicate.test("test", 4));
                    assertFalse(throwableBiPredicate.test("test", 3));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromConsumer() {
                List<String> consumed = new ArrayList<>();
                Consumer<String> javaConsumer = consumed::add;
                Throwables.Consumer<String, Exception> throwableConsumer = Fnn.from(javaConsumer);

                assertNotNull(throwableConsumer);
                try {
                    throwableConsumer.accept("test");
                    assertEquals(Arrays.asList("test"), consumed);
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromBiConsumer() {
                Map<String, Integer> map = new HashMap<>();
                BiConsumer<String, Integer> javaBiConsumer = map::put;
                Throwables.BiConsumer<String, Integer, Exception> throwableBiConsumer = Fnn.from(javaBiConsumer);

                assertNotNull(throwableBiConsumer);
                try {
                    throwableBiConsumer.accept("key", 123);
                    assertEquals(123, map.get("key"));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromFunction() {
                Function<String, Integer> javaFunction = String::length;
                Throwables.Function<String, Integer, Exception> throwableFunction = Fnn.from(javaFunction);

                assertNotNull(throwableFunction);
                try {
                    assertEquals(4, throwableFunction.apply("test"));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromBiFunction() {
                BiFunction<String, String, String> javaBiFunction = (s1, s2) -> s1 + s2;
                Throwables.BiFunction<String, String, String, Exception> throwableBiFunction = Fnn.from(javaBiFunction);

                assertNotNull(throwableBiFunction);
                try {
                    assertEquals("ab", throwableBiFunction.apply("a", "b"));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromUnaryOperator() {
                UnaryOperator<String> javaOperator = s -> s.toUpperCase();
                Throwables.UnaryOperator<String, Exception> throwableOperator = Fnn.from(javaOperator);

                assertNotNull(throwableOperator);
                try {
                    assertEquals("TEST", throwableOperator.apply("test"));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testFromBinaryOperator() {
                BinaryOperator<Integer> javaOperator = Integer::sum;
                Throwables.BinaryOperator<Integer, Exception> throwableOperator = Fnn.from(javaOperator);

                assertNotNull(throwableOperator);
                try {
                    assertEquals(5, throwableOperator.apply(2, 3));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }
        }

        @Nested
        @DisplayName("Shorthand Method Tests")
        class ShorthandMethodTest {

            @Test
            public void testS_Supplier() {
                Throwables.Supplier<String, Exception> supplier = () -> "test";
                assertSame(supplier, Fnn.s(supplier));
            }

            @Test
            public void testS_WithFunction() {
                String input = "test";
                Throwables.Function<String, Integer, Exception> func = String::length;
                Throwables.Supplier<Integer, Exception> supplier = Fnn.s(input, func);

                try {
                    assertEquals(4, supplier.get());
                } catch (Exception e) {
                    fail("Should not throw exception");
                }
            }

            @Test
            public void testP_Predicate() {
                Throwables.Predicate<String, Exception> predicate = s -> s.length() > 3;
                assertSame(predicate, Fnn.p(predicate));
            }

            @Test
            public void testP_WithBiPredicate() {
                String fixed = "test";
                Throwables.BiPredicate<String, String, Exception> biPredicate = String::equals;
                Throwables.Predicate<String, Exception> predicate = Fnn.p(fixed, biPredicate);

                try {
                    assertTrue(predicate.test("test"));
                    assertFalse(predicate.test("other"));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }

                assertThrows(IllegalArgumentException.class, () -> Fnn.p("a", (Throwables.BiPredicate<String, String, Exception>) null));
            }

            @Test
            public void testP_WithTriPredicate() {
                String fixed1 = "a";
                String fixed2 = "b";
                Throwables.TriPredicate<String, String, String, Exception> triPredicate = (a, b, c) -> a.equals(fixed1) && b.equals(fixed2) && c.equals("c");
                Throwables.Predicate<String, Exception> predicate = Fnn.p(fixed1, fixed2, triPredicate);

                try {
                    assertTrue(predicate.test("c"));
                    assertFalse(predicate.test("d"));
                } catch (Exception e) {
                    fail("Should not throw exception");
                }

                assertThrows(IllegalArgumentException.class, () -> Fnn.p("a", "b", null));
            }

            @Test
            public void testC_Consumer() {
                Throwables.Consumer<String, Exception> consumer = s -> {
                };
                assertSame(consumer, Fnn.c(consumer));
            }

            @Test
            public void testF_Function() {
                Throwables.Function<String, Integer, Exception> function = String::length;
                assertSame(function, Fnn.f(function));
            }

            @Test
            public void testO_UnaryOperator() {
                Throwables.UnaryOperator<String, Exception> operator = s -> s.toUpperCase();
                assertSame(operator, Fnn.o(operator));

                assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.UnaryOperator<?, ?>) null));
            }

            @Test
            public void testO_BinaryOperator() {
                Throwables.BinaryOperator<Integer, Exception> operator = Integer::sum;
                assertSame(operator, Fnn.o(operator));

                assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.BinaryOperator<?, ?>) null));
            }
        }

        @Nested
        @DisplayName("Function Conversion Tests")
        class FunctionConversionTest {

            @Test
            public void testC2f_Consumer() throws Exception {
                List<String> consumed = new ArrayList<>();
                Throwables.Consumer<String, Exception> consumer = consumed::add;

                Throwables.Function<String, Void, Exception> function = Fnn.c2f(consumer);

                assertNull(function.apply("test"));
                assertEquals(Arrays.asList("test"), consumed);

                assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<?, ?>) null));
            }

            @Test
            public void testC2f_ConsumerWithReturn() throws Exception {
                List<String> consumed = new ArrayList<>();
                Throwables.Consumer<String, Exception> consumer = consumed::add;

                Throwables.Function<String, Integer, Exception> function = Fnn.c2f(consumer, 123);

                assertEquals(123, function.apply("test"));
                assertEquals(Arrays.asList("test"), consumed);

                assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<String, Exception>) null, 123));
            }

            @Test
            public void testF2c() throws Exception {
                Throwables.Function<String, Integer, Exception> function = String::length;
                Throwables.Consumer<String, Exception> consumer = Fnn.f2c(function);

                consumer.accept("test");

                assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.Function<String, Integer, Exception>) null));
            }
        }

        @Nested
        @DisplayName("Runnable and Callable Tests")
        class RunnableCallableTest {

            @Test
            public void testR() {
                Throwables.Runnable<Exception> runnable = () -> {
                };
                assertSame(runnable, Fnn.r(runnable));

                assertThrows(IllegalArgumentException.class, () -> Fnn.r(null));
            }

            @Test
            public void testC() {
                Throwables.Callable<String, Exception> callable = () -> "test";
                assertSame(callable, Fnn.c(callable));

                assertThrows(IllegalArgumentException.class, () -> Fnn.c((Throwables.Callable<String, Exception>) null));
            }

            @Test
            public void testR2c() throws Exception {
                AtomicInteger counter = new AtomicInteger();
                Throwables.Runnable<Exception> runnable = counter::incrementAndGet;

                Throwables.Callable<Void, Exception> callable = Fnn.r2c(runnable);

                assertNull(callable.call());
                assertEquals(1, counter.get());

                assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null));
            }

            @Test
            public void testR2cWithReturn() throws Exception {
                AtomicInteger counter = new AtomicInteger();
                Throwables.Runnable<Exception> runnable = counter::incrementAndGet;

                Throwables.Callable<String, Exception> callable = Fnn.r2c(runnable, "done");

                assertEquals("done", callable.call());
                assertEquals(1, counter.get());

                assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null, "done"));
            }

            @Test
            public void testC2r() throws Exception {
                Throwables.Callable<String, Exception> callable = () -> "test";
                Throwables.Runnable<Exception> runnable = Fnn.c2r(callable);

                runnable.run();

                assertThrows(IllegalArgumentException.class, () -> Fnn.c2r(null));
            }

            @Test
            public void testRr() {
                Runnable javaRunnable = () -> {
                };
                Throwables.Runnable<Exception> throwableRunnable = Fnn.rr(javaRunnable);

                assertNotNull(throwableRunnable);
            }

            @Test
            public void testCc_Callable() {
                Callable<String> javaCallable = () -> "test";
                Throwables.Callable<String, Exception> throwableCallable = Fnn.cc(javaCallable);

                assertNotNull(throwableCallable);
            }

            @Test
            public void testJr2r() {
                Runnable javaRunnable = () -> {
                };
                Throwables.Runnable<Exception> throwableRunnable = Fnn.jr2r(javaRunnable);

                assertNotNull(throwableRunnable);

                assertSame(javaRunnable, Fnn.jr2r(javaRunnable));

                assertThrows(IllegalArgumentException.class, () -> Fnn.jr2r(null));
            }

            @Test
            public void testR2jr() {
                AtomicInteger counter = new AtomicInteger();
                Throwables.Runnable<IOException> throwableRunnable = () -> {
                    counter.incrementAndGet();
                    throw new IOException("Test");
                };

                java.lang.Runnable javaRunnable = Fnn.r2jr(throwableRunnable);

                assertThrows(RuntimeException.class, javaRunnable::run);
                assertEquals(1, counter.get());

                Runnable original = () -> {
                };
                assertSame(original, Fnn.r2jr(original));

                assertThrows(IllegalArgumentException.class, () -> Fnn.r2jr(null));
            }

            @Test
            public void testJc2c() throws Exception {
                Callable<String> javaCallable = () -> "test";
                Throwables.Callable<String, Exception> throwableCallable = Fnn.jc2c(javaCallable);

                assertEquals("test", throwableCallable.call());

                assertSame(javaCallable, Fnn.jc2c(javaCallable));

                assertThrows(IllegalArgumentException.class, () -> Fnn.jc2c(null));
            }

            @Test
            public void testC2jc() throws Exception {
                Throwables.Callable<String, Exception> throwableCallable = () -> "test";
                java.util.concurrent.Callable<String> javaCallable = Fnn.c2jc(throwableCallable);

                assertEquals("test", javaCallable.call());

                Callable<String> original = () -> "original";
                assertSame(original, Fnn.c2jc(original));

                assertThrows(IllegalArgumentException.class, () -> Fnn.c2jc(null));
            }
        }
    }
}
