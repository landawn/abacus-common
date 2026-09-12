package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.Supplier;

/**
 * Targeted review tests covering invariants identified during a code review of
 * {@link Builder}, {@link Suppliers}, {@link IntFunctions}, {@link NoCachingNoUpdating}
 * and {@link Difference}. Focuses on edge cases not redundantly covered elsewhere:
 * fluent-method this-return, length=0 array creation, multiset-semantics of
 * Difference.of for duplicated elements, ordering preservation of right-only
 * elements, Fn.memoize thread-safety and single-call guarantee.
 */
public class BuilderSuppliersDifferenceTest extends TestBase {

    // ===== Builder: fluent methods return this and chain correctly =====

    @Test
    @DisplayName("Builder.of(List).add chain returns same builder and mutates wrapped list")
    public void testBuilderListChainReturnsThis() {
        final List<String> backing = new ArrayList<>();
        final Builder.ListBuilder<String, List<String>> b = Builder.of(backing);
        assertSame(b, b.add("a").add("b").addAll(Arrays.asList("c", "d")));
        assertEquals(Arrays.asList("a", "b", "c", "d"), backing);
        assertSame(backing, b.val());
    }

    @Test
    @DisplayName("Builder.of(Map).put chain returns same builder")
    public void testBuilderMapChainReturnsThis() {
        final Map<String, Integer> m = new HashMap<>();
        final Builder.MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(m);
        assertSame(b, b.put("a", 1).put("b", 2));
        assertEquals(2, m.size());
        assertEquals(1, m.get("a"));
    }

    @Test
    @DisplayName("Builder rejects null val with IllegalArgumentException")
    public void testBuilderNullVal() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Map<String, Integer>) null));
    }

    @Test
    @DisplayName("Builder.accept invokes consumer and returns this; apply returns mapped result")
    public void testBuilderAcceptApply() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        final Builder<List<Integer>> b = new Builder<>(list);
        assertSame(b, b.accept(l -> l.add(4)));
        assertEquals(4, list.size());
        // apply returns the *result* of the function, not the builder
        final Integer size = b.apply(List::size);
        assertEquals(4, size);
    }

    @Test
    @DisplayName("ComparisonBuilder short-circuits on first non-zero")
    public void testComparisonShortCircuit() {
        final int r = Builder.compare("aa", "ab").compare(99, 1).result();
        // first compare yields negative; second is ignored
        assertTrue(r < 0);
    }

    @Test
    @DisplayName("EquivalenceBuilder short-circuits on first false")
    public void testEquivalenceShortCircuit() {
        // "x" != "y" -> false; subsequent equal would set true if not short-circuited
        assertFalse(Builder.equals("x", "y").equals("z", "z").result());
    }

    @Test
    @DisplayName("HashCodeBuilder follows result*31 + N.hashCode pattern")
    public void testHashCodeBuilder() {
        final int expected = (0 * 31 + CommonUtil.hashCode("a")) * 31 + CommonUtil.hashCode(42);
        assertEquals(expected, Builder.hash("a").hash(42).result());
    }

    // ===== IntFunctions: empty array (length 0) and array creation =====

    @Test
    @DisplayName("IntFunctions.ofIntArray() handles length 0 without error")
    public void testIntFunctionsOfIntArrayZeroLength() {
        final IntFunction<int[]> f = IntFunctions.ofIntArray();
        final int[] arr = f.apply(0);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    @DisplayName("IntFunctions.ofObjectArray length 0 returns non-null empty array")
    public void testIntFunctionsObjectArrayZeroLength() {
        assertEquals(0, IntFunctions.ofObjectArray().apply(0).length);
        assertEquals(0, IntFunctions.ofStringArray().apply(0).length);
    }

    @Test
    @DisplayName("IntFunctions.ofIntArray rejects negative length via JVM NegativeArraySizeException")
    public void testIntFunctionsIntArrayNegative() {
        final IntFunction<int[]> f = IntFunctions.ofIntArray();
        assertThrows(NegativeArraySizeException.class, () -> f.apply(-1));
    }

    @Test
    @DisplayName("IntFunctions.ofDisposableArray returns same DisposableArray instance across calls (stateful)")
    public void testIntFunctionsDisposableReuse() {
        final IntFunction<DisposableArray<String>> f = IntFunctions.ofDisposableArray(String.class);
        final DisposableArray<String> first = f.apply(3);
        final DisposableArray<String> second = f.apply(3);
        assertSame(first, second);
    }

    @Test
    @DisplayName("IntFunctions.ofDisposableArray() returns same DisposableObjArray instance across calls")
    public void testIntFunctionsDisposableObjReuse() {
        final IntFunction<DisposableObjArray> f = IntFunctions.ofDisposableArray();
        final DisposableObjArray first = f.apply(2);
        final DisposableObjArray second = f.apply(2);
        assertSame(first, second);
    }

    // ===== Suppliers: empty arrays are constants; UUID is fresh =====

    @Test
    @DisplayName("Suppliers.ofEmptyIntArray returns the same shared empty instance")
    public void testSuppliersEmptyArraySingleton() {
        assertSame(Suppliers.ofEmptyIntArray().get(), Suppliers.ofEmptyIntArray().get());
        assertSame(Suppliers.ofEmptyStringArray().get(), Suppliers.ofEmptyStringArray().get());
    }

    @Test
    @DisplayName("Suppliers.ofUuid returns distinct UUID strings on each call")
    public void testSuppliersUuidFresh() {
        final Supplier<String> uuidSupplier = Suppliers.ofUuid();
        final String a = uuidSupplier.get();
        final String b = uuidSupplier.get();
        assertNotNull(a);
        assertNotNull(b);
        // UUIDs should be unique
        org.junit.jupiter.api.Assertions.assertNotEquals(a, b);
    }

    @Test
    @DisplayName("Suppliers.ofList returns a fresh ArrayList on each call")
    public void testSuppliersListFresh() {
        final Supplier<List<String>> sup = Suppliers.ofList();
        final List<String> a = sup.get();
        final List<String> b = sup.get();
        assertNotSame(a, b);
        a.add("x");
        assertTrue(b.isEmpty());
    }

    @Test
    @DisplayName("Suppliers.ofInstance always returns the same instance")
    public void testSuppliersOfInstance() {
        final Object obj = new Object();
        final Supplier<Object> sup = Suppliers.ofInstance(obj);
        assertSame(obj, sup.get());
        assertSame(obj, sup.get());
    }

    // ===== Fn.memoize: thread-safety and single-call guarantee =====

    @Test
    @DisplayName("Fn.memoize calls supplier exactly once across many concurrent threads")
    public void testFnMemoizeSingleCallUnderConcurrency() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final Supplier<Integer> memoized = Fn.memoize(() -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(20);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return 42;
        });

        final int threads = 16;
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        assertEquals(Integer.valueOf(42), memoized.get());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, calls.get(), "memoize must invoke supplier exactly once");
    }

    @Test
    @DisplayName("Fn.memoizeWithExpiration recomputes after expiration window")
    public void testFnMemoizeWithExpirationRecomputes() throws Exception {
        final AtomicInteger counter = new AtomicInteger();
        final Supplier<Integer> sup = Fn.memoizeWithExpiration(counter::incrementAndGet, 50L, TimeUnit.MILLISECONDS);
        final int v1 = sup.get();
        final int v2 = sup.get();
        assertEquals(v1, v2);
        assertEquals(1, counter.get());
        Thread.sleep(80);
        final int v3 = sup.get();
        assertEquals(2, v3);
        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("Fn.memoizeWithExpiration rejects non-positive duration")
    public void testFnMemoizeWithExpirationInvalidDuration() {
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> "x", 0L, TimeUnit.MILLISECONDS));
        assertThrows(IllegalArgumentException.class, () -> Fn.memoizeWithExpiration(() -> "x", -1L, TimeUnit.MILLISECONDS));
    }

    // ===== Difference: multiset/list semantics, order preservation =====

    @Test
    @DisplayName("Difference.of preserves left-iteration order in common and onlyOnLeft")
    public void testDifferenceLeftOrder() {
        final List<String> a = Arrays.asList("a", "b", "c", "b");
        final List<String> b = Arrays.asList("b", "b", "b", "d");
        final Difference<List<String>, List<String>> diff = Difference.of(a, b);
        assertEquals(Arrays.asList("b", "b"), diff.common());
        assertEquals(Arrays.asList("a", "c"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("b", "d"), diff.onlyOnRight());
    }

    @Test
    @DisplayName("Difference.of(Collection) preserves duplicates per multiset semantics")
    public void testDifferenceMultisetSemantics() {
        final List<String> a = Arrays.asList("a", "a", "a");
        final List<String> b = Arrays.asList("a");
        final Difference<List<String>, List<String>> diff = Difference.of(a, b);
        assertEquals(Arrays.asList("a"), diff.common());
        assertEquals(Arrays.asList("a", "a"), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertFalse(diff.areEqual());
    }

    @Test
    @DisplayName("Difference.of with both null/empty inputs reports areEqual=true with empty diffs")
    public void testDifferenceBothEmpty() {
        final Difference<List<String>, List<String>> diff = Difference.of((java.util.Collection<String>) null, (java.util.Collection<String>) null);
        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    @DisplayName("Difference.of(int[], int[]) handles all-same arrays as areEqual")
    public void testDifferenceIntArrayEqual() {
        final int[] a = { 1, 2, 3 };
        final int[] b = { 3, 2, 1 };
        final Difference<IntList, IntList> diff = Difference.of(a, b);
        assertTrue(diff.areEqual());
        assertEquals(IntList.of(1, 2, 3), diff.common());
    }

    @Test
    @DisplayName("Difference.of with right list having more duplicates of common element")
    public void testDifferenceRightHasMoreDuplicates() {
        final List<Integer> a = Arrays.asList(1, 2);
        final List<Integer> b = Arrays.asList(2, 2, 2);
        final Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);
        assertEquals(Arrays.asList(2), diff.common());
        assertEquals(Arrays.asList(1), diff.onlyOnLeft());
        assertEquals(Arrays.asList(2, 2), diff.onlyOnRight());
    }

    // ===== NoCachingNoUpdating.DisposableArray: pure-helper sanity =====

    @Test
    @DisplayName("DisposableArray.toList returns a fresh independent list")
    public void testDisposableArrayToListIndependent() {
        final DisposableArray<String> da = DisposableArray.wrap(new String[] { "a", "b" });
        final List<String> first = da.toList();
        final List<String> second = da.toList();
        assertNotSame(first, second);
        first.add("mutated");
        assertEquals(2, second.size());
    }

    @Test
    @DisplayName("DisposableArray.copy returns a fresh array (no caching)")
    public void testDisposableArrayCopyFresh() {
        final String[] source = { "x", "y" };
        final DisposableArray<String> da = DisposableArray.wrap(source);
        final String[] copyA = da.copy();
        final String[] copyB = da.copy();
        assertNotSame(copyA, copyB);
        assertNotSame(source, copyA);
        org.junit.jupiter.api.Assertions.assertArrayEquals(source, copyA);
    }
}
