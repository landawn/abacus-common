package com.landawn.abacus.util;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Covers the non-{@code Retry} fixes from the 2026-09-01 review of {@code Index}, {@code Median}, {@code Wrapper},
 * {@code Keyed}, {@code TypeReference}, {@code Clazz}, {@code Try} and {@code Throwables}:
 *
 * <ul>
 *   <li><b>B2</b> - {@code TypeReference} preserves generic array component metadata now that {@code TypeFactory}
 *       supports it, while malformed or unresolved captures retain their diagnostic exceptions.</li>
 *   <li><b>D1</b> - custom {@code Wrapper.of(value, hash, equals)} wrappers compare only when they share the same
 *       function instances; {@code Fn.wrap} captures one pair so every wrapper it produces is comparable.</li>
 *   <li><b>D10</b> - {@code TypeReference.reflectType()} aliases {@code javaType()}.</li>
 *   <li><b>D11</b> - {@code Clazz}'s implementation factories are typed to the implementation, so {@code cast} and
 *       {@code isInstance} no longer promise more than they accept.</li>
 *   <li><b>Javadoc</b> - claims verified here so they cannot silently rot: {@code Median}'s null-parity ambiguity and
 *       three-element tie behaviour, {@code Index}'s empty-pattern conventions, {@code Try} vs {@code Throwables}
 *       {@code Error} handling, and {@code Keyed}'s class-exact equality.</li>
 * </ul>
 */
public class MultiClassRegressionETest extends TestBase {

    // ------------------------------------------------------------------------------------------------------
    // B2: TypeReference and generic array types
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testTypeReference_GenericArrayType_PreservesComponentMetadata() {
        final TypeReference<List<String>[]> ref = new TypeReference<>() {
        };
        final java.lang.reflect.GenericArrayType arrayType = Assertions.assertInstanceOf(java.lang.reflect.GenericArrayType.class, ref.javaType());
        final java.lang.reflect.ParameterizedType componentType = Assertions.assertInstanceOf(java.lang.reflect.ParameterizedType.class,
                arrayType.getGenericComponentType());

        Assertions.assertEquals(List.class, componentType.getRawType());
        Assertions.assertArrayEquals(new java.lang.reflect.Type[] { String.class }, componentType.getActualTypeArguments());
        Assertions.assertEquals(List[].class, ref.type().javaType());
        Assertions.assertEquals(String.class, ref.type().elementType().elementType().javaType());
        Assertions.assertEquals(List.of("1"), ref.type().valueOf("[[1]]")[0]);
    }

    /** A named intermediate subclass whose substitution produces a generic array - the other route to the same case. */
    abstract static class GenericArrayMid<X> extends TypeReference<List<X>[]> {
    }

    public static class GenericArrayOwner<T> {
        public class Member {
            public T value;
        }
    }

    abstract static class GenericOwnerArrayMid<T> extends TypeReference<GenericArrayOwner<T>.Member[]> {
    }

    @Test
    public void testTypeReference_GenericArrayOwnerWithoutMemberArgumentsRendersCorrectly() {
        final TypeReference<GenericArrayOwner<String>.Member[]> substituted = new GenericOwnerArrayMid<String>() {
        };
        final TypeReference<GenericArrayOwner<String>.Member[]> direct = new TypeReference<>() {
        };

        Assertions.assertEquals(direct.javaType(), substituted.javaType());
        Assertions.assertEquals(direct.javaType().hashCode(), substituted.javaType().hashCode());
        Assertions.assertEquals(direct.javaType().getTypeName(), substituted.javaType().getTypeName());
        Assertions.assertEquals(direct.javaType().getTypeName(), substituted.javaType().toString());
    }

    @Test
    public void testTypeReference_GenericArrayViaIntermediateSubclass_PreservesSubstitution() {
        final TypeReference<List<String>[]> ref = new GenericArrayMid<>() {
        };
        final TypeReference<List<String>[]> direct = new TypeReference<>() {
        };

        Assertions.assertInstanceOf(java.lang.reflect.GenericArrayType.class, ref.javaType());
        Assertions.assertEquals(direct.javaType(), ref.javaType());
        Assertions.assertEquals(direct, ref);
        Assertions.assertEquals(direct.hashCode(), ref.hashCode());
        Assertions.assertEquals(List[].class, ref.type().javaType());
        Assertions.assertEquals(String.class, ref.type().elementType().elementType().javaType());
        Assertions.assertEquals(List.of("2"), ref.type().valueOf("[[2]]")[0]);
    }

    @Test
    public void testTypeReference_NonGenericArraysStillWork() {
        Assertions.assertEquals(String[].class, new TypeReference<String[]>() {
        }.javaType());
        Assertions.assertEquals(int[].class, new TypeReference<int[]>() {
        }.javaType());
        // An array nested inside a type argument is fine too.
        Assertions.assertNotNull(new TypeReference<List<String[]>>() {
        }.javaType());
    }

    /** A named intermediate subclass whose substitution produces a plain array class. */
    abstract static class ArrayMid<X> extends TypeReference<X[]> {
    }

    @Test
    public void testTypeReference_ResolvedArrayOfClassComponentBecomesAnArrayClass() {
        Assertions.assertEquals(String[].class, new ArrayMid<String>() {
        }.javaType());
    }

    @Test
    public void testTypeReference_OtherRejectionsKeepTheirOwnMessages() {
        // A raw TypeReference and an unresolved type variable are rejected before TypeFactory is ever consulted,
        // so they must not pick up the generic-array wording.
        @SuppressWarnings("rawtypes")
        final IllegalArgumentException raw = Assertions.assertThrows(IllegalArgumentException.class, () -> new TypeReference() {
        });
        Assertions.assertTrue(raw.getMessage().contains("without actual type information"), raw.getMessage());
        Assertions.assertFalse(raw.getMessage().contains("generic array"), raw.getMessage());

        final IllegalArgumentException typeVar = Assertions.assertThrows(IllegalArgumentException.class, MultiClassRegressionETest::captureTypeVariable);
        Assertions.assertTrue(typeVar.getMessage().contains("without concrete type information"), typeVar.getMessage());
    }

    private static <T> java.lang.reflect.Type captureTypeVariable() {
        return new TypeReference<List<T>>() {
        }.javaType();
    }

    // ------------------------------------------------------------------------------------------------------
    // D10: TypeReference.reflectType()
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testTypeReference_ReflectTypeIsAnAliasOfJavaType() {
        final TypeReference<Map<String, Integer>> ref = new TypeReference<>() {
        };

        Assertions.assertSame(ref.javaType(), ref.reflectType());
        Assertions.assertEquals("java.util.Map<java.lang.String, java.lang.Integer>", ref.reflectType().getTypeName());

        // ... and it is deliberately different from Type#javaType(), which is the raw Class.
        final Type<Map<String, Integer>> type = ref.type();
        Assertions.assertEquals(Map.class, type.javaType());
        Assertions.assertNotEquals(ref.reflectType(), type.javaType());
    }

    @Test
    public void testTypeReference_ReflectTypeOnTypeToken() {
        final TypeReference.TypeToken<List<String>> token = new TypeReference.TypeToken<>() {
        };

        Assertions.assertSame(token.javaType(), token.reflectType());
        Assertions.assertEquals(token, new TypeReference<List<String>>() {
        });
    }

    // ------------------------------------------------------------------------------------------------------
    // D1: custom Wrapper.of(value, hash, equals) compares by function-instance identity
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testWrapper_PerValueFactoryComparesOnlyWithSharedFunctionInstances() {
        // Separate call sites supply their own lambdas, so a Set lookup misses.
        final Set<Wrapper<String>> set = new HashSet<>();
        set.add(Wrapper.of("alice", s -> s.length(), (a, b) -> a.equals(b)));
        Assertions.assertFalse(set.contains(Wrapper.of("alice", s -> s.length(), (a, b) -> a.equals(b))));

        final ToIntFunction<String> hash = String::hashCode;
        final BiPredicate<String, String> eq = String::equals;
        Assertions.assertEquals(Wrapper.of("bob", hash, eq), Wrapper.of("bob", hash, eq));

        final Function<String, String> render = s -> "S[" + s + "]";
        Assertions.assertEquals("Wrapper[S[bob]]", Wrapper.of("bob", hash, eq, render).toString());
    }

    @Test
    public void testWrapper_SharedFunctionInstancesCompareAcrossCallSites() {
        final ToIntFunction<String> hash = s -> s.toLowerCase(java.util.Locale.ROOT).hashCode();
        final BiPredicate<String, String> eq = String::equalsIgnoreCase;

        final Set<Wrapper<String>> set = new HashSet<>();
        set.add(Wrapper.of("Hello", hash, eq));

        Assertions.assertTrue(set.contains(Wrapper.of("HELLO", hash, eq)));
        Assertions.assertTrue(set.contains(Wrapper.of("hello", hash, eq)));

        // Two different function-instance pairs never compare, even from equivalent functions.
        final ToIntFunction<String> otherHash = s -> s.toLowerCase(java.util.Locale.ROOT).hashCode();
        final BiPredicate<String, String> otherEq = String::equalsIgnoreCase;
        Assertions.assertFalse(set.contains(Wrapper.of("Hello", otherHash, otherEq)));
    }

    @Test
    public void testFnWrap_ProducesWrappersThatCompareWithEachOther() {
        // Fn.wrap captures one hash/equals pair for the whole returned function, so every wrapper it makes is comparable.
        final Function<String, Wrapper<String>> wrap = Fn.wrap(s -> s.length(), (a, b) -> a.length() == b.length());

        final Set<Wrapper<String>> set = new HashSet<>();
        set.add(wrap.apply("abc"));

        Assertions.assertTrue(set.contains(wrap.apply("xyz")));
        Assertions.assertFalse(set.contains(wrap.apply("wxyz")));

        // Two separate Fn.wrap calls capture two function-instance pairs and must not compare.
        final Function<String, Wrapper<String>> other = Fn.wrap(s -> s.length(), (a, b) -> a.length() == b.length());
        Assertions.assertFalse(set.contains(other.apply("abc")));

        Assertions.assertEquals("abc", Fn.<String> unwrap().apply(wrap.apply("abc")));
    }

    @Test
    public void testFnWrap_StillValidatesItsArguments() {
        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.wrap(null, (final String a, final String b) -> true))
                .getMessage()
                .contains("hashFunction"));

        Assertions.assertTrue(
                Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.wrap(String::hashCode, null)).getMessage().contains("equalsFunction"));
    }

    @Test
    public void testWrapper_ArrayDeepSemanticsUnchanged() {
        final int[] a = { 1, 2, 3 };
        final int[] b = { 1, 2, 3 };

        Assertions.assertEquals(Wrapper.of(a), Wrapper.of(b));
        Assertions.assertEquals(Wrapper.of(a).hashCode(), Wrapper.of(b).hashCode());
        Assertions.assertEquals(Wrapper.of(null), Wrapper.of(null));
        Assertions.assertSame(Wrapper.of(new int[0]), Wrapper.of(new int[0]));
    }

    @Test
    public void testWrapper_TypeIncompatibleComparisonIsNotEqualAndIsSymmetric() {
        // The documented contract: a ClassCastException from the supplied function counts as "not equal", while any
        // other exception propagates. Shared function instances so both wrappers are actually comparable.
        final ToIntFunction<Object> hash = o -> 1;
        final BiPredicate<Object, Object> eq = (x, y) -> ((String) x).equals(y);

        final Wrapper<Object> text = Wrapper.of("s", hash, eq);
        final Wrapper<Object> number = Wrapper.of(Integer.valueOf(3), hash, eq);

        // number.equals(text) casts an Integer to String -> CCE -> swallowed as "not equal".
        Assertions.assertNotEquals(number, text);
        // text.equals(number) casts a String to String -> no CCE, just an unequal comparison.
        Assertions.assertNotEquals(text, number);
        // Equal values still compare equal through the same function instances.
        Assertions.assertEquals(Wrapper.of("s", hash, eq), Wrapper.of("s", hash, eq));
    }

    @Test
    public void testWrapper_NonClassCastExceptionFromTheEqualsFunctionPropagates() {
        // Only ClassCastException is treated as "not equal"; a broken null policy must still surface.
        final ToIntFunction<String> hash = o -> 1;
        final BiPredicate<String, String> eq = (x, y) -> x.length() == y.length();

        Assertions.assertThrows(NullPointerException.class, () -> Wrapper.of(null, hash, eq).equals(Wrapper.of("x", hash, eq)));
    }

    // ------------------------------------------------------------------------------------------------------
    // D11: Clazz implementation factories are typed to the implementation
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testClazz_ImplementationFactoriesAreTypedToTheImplementation() {
        final Class<LinkedList<String>> linkedList = Clazz.ofLinkedList(String.class);
        final Class<LinkedHashSet<String>> linkedHashSet = Clazz.ofLinkedHashSet(String.class);
        final Class<TreeSet<String>> treeSet = Clazz.ofTreeSet(String.class);
        final Class<ArrayDeque<String>> arrayDeque = Clazz.ofArrayDeque(String.class);
        final Class<ConcurrentLinkedQueue<String>> clq = Clazz.ofConcurrentLinkedQueue(String.class);
        final Class<PriorityQueue<String>> priorityQueue = Clazz.ofPriorityQueue(String.class);
        final Class<LinkedBlockingQueue<String>> lbq = Clazz.ofLinkedBlockingQueue(String.class);
        final Class<LinkedHashMap<String, Object>> linkedHashMap = Clazz.ofLinkedHashMap(String.class, Object.class);
        final Class<TreeMap<String, Object>> treeMap = Clazz.ofTreeMap(String.class, Object.class);
        final Class<ConcurrentHashMap<String, Object>> chm = Clazz.ofConcurrentHashMap(String.class, Object.class);

        Assertions.assertEquals(LinkedList.class, linkedList);
        Assertions.assertEquals(LinkedHashSet.class, linkedHashSet);
        Assertions.assertEquals(TreeSet.class, treeSet);
        Assertions.assertEquals(ArrayDeque.class, arrayDeque);
        Assertions.assertEquals(ConcurrentLinkedQueue.class, clq);
        Assertions.assertEquals(PriorityQueue.class, priorityQueue);
        Assertions.assertEquals(LinkedBlockingQueue.class, lbq);
        Assertions.assertEquals(LinkedHashMap.class, linkedHashMap);
        Assertions.assertEquals(TreeMap.class, treeMap);
        Assertions.assertEquals(ConcurrentHashMap.class, chm);
    }

    @Test
    public void testClazz_ImplementationFactoryCastAcceptsExactlyWhatItsTypeSays() {
        // Before the retype this was declared Class<List<String>> yet held LinkedList.class, so cast() promised to
        // accept any List and then rejected an ArrayList. Now the static type and the runtime class agree.
        final Class<LinkedList<String>> cls = Clazz.ofLinkedList();

        final LinkedList<String> ok = cls.cast(new LinkedList<>(List.of("a")));
        Assertions.assertEquals(List.of("a"), ok);

        Assertions.assertTrue(cls.isInstance(new LinkedList<>()));
        Assertions.assertFalse(cls.isInstance(new ArrayList<>()));
        Assertions.assertThrows(ClassCastException.class, () -> cls.cast(new ArrayList<>()));
    }

    @Test
    public void testClazz_InterfaceFactoriesStillReturnTheInterface() {
        final Class<List<String>> list = Clazz.ofList(String.class);
        final Class<Set<String>> set = Clazz.ofSet(String.class);
        final Class<Map<String, Object>> map = Clazz.ofMap(String.class, Object.class);

        Assertions.assertEquals(List.class, list);
        Assertions.assertEquals(Set.class, set);
        Assertions.assertEquals(Map.class, map);
        Assertions.assertTrue(list.isInterface());
        Assertions.assertTrue(list.isInstance(new ArrayList<>()));
        Assertions.assertTrue(list.isInstance(new LinkedList<>()));
    }

    @Test
    public void testClazz_MapConstantsAreTypedToWhatTheyActuallyHold() {
        // PROPS_MAP and LINKED_HASH_MAP hold LinkedHashMap.class, so they are typed to LinkedHashMap - the same rule
        // the implementation factories follow. MAP holds Map.class and stays typed to the interface.
        final Class<LinkedHashMap<String, Object>> props = Clazz.PROPS_MAP;
        final Class<LinkedHashMap<String, Object>> linkedHashMap = Clazz.LINKED_HASH_MAP;
        final Class<Map<String, Object>> map = Clazz.MAP;

        Assertions.assertEquals(LinkedHashMap.class, props);
        Assertions.assertEquals(LinkedHashMap.class, linkedHashMap);
        Assertions.assertEquals(Map.class, map);
        Assertions.assertSame(props, linkedHashMap);

        // cast/isInstance accept exactly what the declared type promises
        Assertions.assertNotNull(props.cast(new LinkedHashMap<>()));
        Assertions.assertTrue(props.isInstance(new LinkedHashMap<>()));
        Assertions.assertFalse(props.isInstance(new java.util.HashMap<>()));
        Assertions.assertThrows(ClassCastException.class, () -> props.cast(new java.util.HashMap<>()));

        // ... whereas the interface constant accepts any Map
        Assertions.assertTrue(map.isInstance(new java.util.HashMap<>()));
        Assertions.assertTrue(map.isInstance(new LinkedHashMap<>()));
    }

    @Test
    public void testClazz_HintArgumentsAreStillIgnoredAndMayBeNull() {
        Assertions.assertEquals(LinkedList.class, Clazz.ofLinkedList((Class<String>) null));
        Assertions.assertEquals(TreeMap.class, Clazz.ofTreeMap(null, null));
        Assertions.assertSame(Clazz.ofTreeSet(), Clazz.ofTreeSet(Integer.class));
    }

    // ------------------------------------------------------------------------------------------------------
    // Javadoc claims that are now asserted
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testMedian_EmptyRightMeansOddLengthEvenWhenNullsArePresent() {
        final Pair<String, u.Nullable<String>> one = Median.of(new String[] { null });
        final Pair<String, u.Nullable<String>> two = Median.of(new String[] { null, null });

        Assertions.assertNull(one.left());
        Assertions.assertTrue(one.right().isEmpty());
        Assertions.assertNull(two.left());
        Assertions.assertTrue(two.right().isPresent());
        Assertions.assertNull(two.right().get());
        Assertions.assertNotEquals(one, two);
    }

    @Test
    public void testMedian_OnlyTheThreeElementRangeCanDivergeFromAStableSort() {
        // Lengths 1, 2 and >= 4 return exactly the instance a stable sort would place at the median position.
        for (final int len : new int[] { 1, 2, 4, 5, 6, 7 }) {
            final String[] equalButDistinct = new String[len];
            for (int i = 0; i < len; i++) {
                equalButDistinct[i] = new String("x");
            }

            final String[] sorted = equalButDistinct.clone();
            Arrays.sort(sorted, Comparator.naturalOrder());
            final String stableLower = sorted[len % 2 == 0 ? len / 2 - 1 : len / 2];

            Assertions.assertSame(stableLower, Median.of(equalButDistinct, Comparator.<String> naturalOrder()).left(), "len=" + len);
        }
    }

    @Test
    public void testMedian_ValuesAreCorrectAcrossThePathBoundaries() {
        // Guards the len == 1/2/3 short paths against the len >= 4 copy-and-sort path.
        Assertions.assertEquals(1, Median.of(new int[] { 1 }).left());
        Assertions.assertEquals(1, Median.of(new int[] { 2, 1 }).left());
        Assertions.assertEquals(2, Median.of(new int[] { 2, 1 }).right().get());
        Assertions.assertEquals(2, Median.of(new int[] { 3, 1, 2 }).left());
        Assertions.assertTrue(Median.of(new int[] { 3, 1, 2 }).right().isEmpty());
        Assertions.assertEquals(2, Median.of(new int[] { 4, 3, 1, 2 }).left());
        Assertions.assertEquals(3, Median.of(new int[] { 4, 3, 1, 2 }).right().get());
    }

    @Test
    public void testMedian_CollectionGuardMessageDescribesWhatItCaught() {
        // A collection that reports a size it cannot deliver hits the post-copy guard, whose message used to claim
        // the source was "null or empty" even though the pre-check had already passed.
        final List<String> lying = new ArrayList<>() {
            @Override
            public int size() {
                return 3;
            }

            @Override
            public boolean isEmpty() {
                // Disagrees with the (empty) backing storage that toArray() will actually read.
                return false;
            }
        };

        final IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Median.of(lying, Comparator.<String> naturalOrder()));

        Assertions.assertTrue(thrown.getMessage().contains("yielded no elements"), thrown.getMessage());
    }

    @Test
    public void testIndex_ToleranceZeroMatchesSignedZeroButPlainSearchDoesNot() {
        // The (de-duplicated) "Signed zero" paragraph, on both the of and last families.
        final double[] zeros = { 0.0d, -0.0d };

        Assertions.assertEquals(0, Index.of(zeros, 0.0d).get());
        Assertions.assertEquals(1, Index.of(zeros, -0.0d).get());
        Assertions.assertEquals(0, Index.of(zeros, -0.0d, 0, 0.0d).get());
        Assertions.assertEquals(1, Index.last(zeros, 0.0d, 1, 0.0d).get());
        Assertions.assertEquals(0, Index.last(zeros, 0.0d, 1).get());

        final float[] fzeros = { 0.0f, -0.0f };
        Assertions.assertEquals(0, Index.of(fzeros, -0.0f, 0, 0.0f).get());
        Assertions.assertEquals(1, Index.of(fzeros, -0.0f).get());
        Assertions.assertEquals(1, Index.last(fzeros, 0.0f, 1, 0.0f).get());
        Assertions.assertEquals(0, Index.last(fzeros, 0.0f, 1).get());
    }

    @Test
    public void testIndex_EmptyPatternFollowsStringConventions() {
        final int[] source = { 1, 2, 3 };

        Assertions.assertEquals("abc".indexOf(""), Index.ofSubArray(source, new int[0]).get());
        Assertions.assertEquals("abc".indexOf("", 2), Index.ofSubArray(source, 2, new int[0]).get());
        Assertions.assertEquals("abc".indexOf("", 9), Index.ofSubArray(source, 9, new int[0]).get());
        Assertions.assertEquals("abc".lastIndexOf(""), Index.lastOfSubArray(source, new int[0]).get());
        Assertions.assertEquals("abc".lastIndexOf("", 1), Index.lastOfSubArray(source, 1, new int[0]).get());
        Assertions.assertTrue(Index.lastOfSubArray(source, -1, new int[0]).isEmpty());
    }

    @Test
    public void testIndex_NoMatchNeverThrows() {
        Assertions.assertTrue(Index.of((int[]) null, 1).isEmpty());
        Assertions.assertTrue(Index.of(new int[0], 1).isEmpty());
        Assertions.assertTrue(Index.of(new int[] { 1 }, 1, 99).isEmpty());
        Assertions.assertEquals(0, Index.of(new int[] { 1 }, 1, -5).get());
        Assertions.assertTrue(Index.allOf((int[]) null, 1).isEmpty());
    }

    @Test
    public void testTry_ErrorPropagatesWhileThrowablesConvertsIt() {
        // The behavioural difference now documented on both classes.
        Assertions.assertThrows(StackOverflowError.class, () -> Try.run(() -> {
            throw new StackOverflowError("boom");
        }));

        final RuntimeException converted = Assertions.assertThrows(RuntimeException.class, () -> Throwables.run(() -> {
            throw new StackOverflowError("boom");
        }));
        Assertions.assertInstanceOf(StackOverflowError.class, converted.getCause());

        // An Error is not routed to actionOnError, and not replaced by a fallback value either.
        Assertions.assertThrows(StackOverflowError.class, () -> Try.run(() -> {
            throw new StackOverflowError("boom");
        }, ex -> Assertions.fail("actionOnError must not see an Error")));

        Assertions.assertThrows(StackOverflowError.class, () -> Try.call(() -> {
            throw new StackOverflowError("boom");
        }, "fallback"));

        // A checked exception, by contrast, still goes down the handled path.
        Assertions.assertEquals("fallback", Try.call(() -> {
            throw new IOException("io");
        }, "fallback"));
    }

    // ------------------------------------------------------------------------------------------------------
    // Cycle 1: C1-J1/C1-N1 (how far the "deep" of Wrapper reaches), C1-J2 (ofIgnoreCase null needle),
    //          C1-J3 (TypeReference @throws), C1-D1 (ArrayWrapper vs AnyWrapper never compare)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testWrapper_DeepReachesThroughArraysButNotCollections() {
        final int[] a = { 1, 2 };
        final int[] b = { 1, 2 };

        // A bare array, and an array nested inside another array, compare by content.
        Assertions.assertEquals(Wrapper.of(a), Wrapper.of(b));
        Assertions.assertEquals(Wrapper.of(a).hashCode(), Wrapper.of(b).hashCode());
        Assertions.assertEquals(Wrapper.of(new Object[] { a }), Wrapper.of(new Object[] { b }));
        Assertions.assertEquals(Wrapper.of(new int[][] { { 1 }, { 2 } }), Wrapper.of(new int[][] { { 1 }, { 2 } }));

        // An array reached only through a collection does not: List.equals compares the int[] by identity.
        Assertions.assertNotEquals(Wrapper.of(List.of(a)), Wrapper.of(List.of(b)));
        Assertions.assertNotEquals(Wrapper.of(List.of(a)).hashCode(), Wrapper.of(List.of(b)).hashCode());
        Assertions.assertNotEquals(Wrapper.of(Map.of("k", a)), Wrapper.of(Map.of("k", b)));

        // ... and the documented way round it: wrap the arrays themselves.
        Assertions.assertEquals(List.of(Wrapper.of(a)), List.of(Wrapper.of(b)));

        // Non-array values keep their own equality.
        Assertions.assertEquals(Wrapper.of("x"), Wrapper.of("x"));
        Assertions.assertEquals(Wrapper.of(List.of(1, 2)), Wrapper.of(List.of(1, 2)));
        Assertions.assertNotEquals(Wrapper.of(List.of(1, 2)), Wrapper.of(List.of(2, 1)));
    }

    @Test
    public void testWrapper_SelfReferentialArrayIsUnsupportedForHashing() {
        final Object[] cyclic = new Object[1];
        cyclic[0] = cyclic;

        // Arrays.deepHashCode documents this input as undefined; equals survives via its identity shortcut.
        Assertions.assertThrows(StackOverflowError.class, () -> Wrapper.of(cyclic).hashCode());
        Assertions.assertEquals(Wrapper.of(cyclic), Wrapper.of(cyclic));
    }

    @Test
    public void testWrapper_DeepAndCustomWrappersNeverCompareEqual() {
        // ArrayWrapper (of(Object)) and AnyWrapper (of(value, hash, equals)) never compare equal,
        // even when they wrap the same value; that keeps equals symmetric across the two kinds.
        final ToIntFunction<String> hash = String::hashCode;
        final BiPredicate<String, String> eq = String::equals;
        final Wrapper<String> deep = Wrapper.of("x");
        final Wrapper<String> custom = Wrapper.of("x", hash, eq);
        Assertions.assertNotEquals(deep, custom);
        Assertions.assertNotEquals(custom, deep);
        Assertions.assertEquals(Wrapper.ArrayWrapper.class, deep.getClass());
        Assertions.assertEquals(Wrapper.AnyWrapper.class, custom.getClass());
    }

    @Test
    public void testIndex_OfIgnoreCase_NullNeedleMeansOppositeThingsInTheTwoShapes() {
        final String[] arr = { "Hello", null, "WORLD" };

        // String[] shape: a null needle finds a null element.
        Assertions.assertEquals(1, Index.ofIgnoreCase(arr, null).get());
        Assertions.assertEquals(1, Index.ofIgnoreCase(arr, null, 0).get());
        Assertions.assertEquals(1, Index.lastOfIgnoreCase(arr, null).get());
        Assertions.assertEquals(1, Index.lastOfIgnoreCase(arr, null, arr.length - 1).get());

        // String shape: a null needle never matches.
        Assertions.assertTrue(Index.ofIgnoreCase("Hello", null).isEmpty());
        Assertions.assertTrue(Index.ofIgnoreCase("Hello", null, 0).isEmpty());
        Assertions.assertTrue(Index.lastOfIgnoreCase("Hello", null).isEmpty());
        Assertions.assertTrue(Index.lastOfIgnoreCase("Hello", null, 4).isEmpty());

        // A null source is empty for both shapes.
        Assertions.assertTrue(Index.ofIgnoreCase((String) null, "x").isEmpty());
        Assertions.assertTrue(Index.ofIgnoreCase((String[]) null, "x").isEmpty());
        Assertions.assertTrue(Index.lastOfIgnoreCase((String) null, "x").isEmpty());
        Assertions.assertTrue(Index.lastOfIgnoreCase((String[]) null, "x").isEmpty());

        // An empty needle is not the same as a null one.
        Assertions.assertEquals(0, Index.ofIgnoreCase("Hello", "").get());
        Assertions.assertTrue(Index.ofIgnoreCase(arr, "").isEmpty());
    }

    @Test
    public void testIndex_OfIgnoreCase_FollowsStringEqualsIgnoreCaseForUnicode() {
        // Simple case folding, exactly as String.equalsIgnoreCase does it - no locale, no full case folding.
        final String sharpS = "stra" + (char) 0x00DF + "e";
        final String dottedI = (char) 0x0130 + "stanbul";
        final String eAcute = (char) 0x00C9 + "CLAIR";
        final String[] arr = { sharpS, dottedI, eAcute };

        Assertions.assertEquals(sharpS.equalsIgnoreCase("STRASSE"), Index.ofIgnoreCase(arr, "STRASSE").isPresent());
        Assertions.assertFalse(Index.ofIgnoreCase(arr, "STRASSE").isPresent(), "sharp s does not fold to ss");

        Assertions.assertEquals(dottedI.equalsIgnoreCase("istanbul"), Index.ofIgnoreCase(arr, "istanbul").isPresent());
        Assertions.assertEquals(2, Index.ofIgnoreCase(arr, (char) 0x00E9 + "clair").get());

        // Supplementary code points survive the comparison unchanged (one code point, two chars).
        final String emoji = Character.toString(0x1F600);
        Assertions.assertEquals(2, emoji.length());
        Assertions.assertEquals(0, Index.ofIgnoreCase(new String[] { "a" + emoji + "b" }, "A" + emoji + "B").get());
        Assertions.assertEquals(1, Index.ofIgnoreCase("a" + emoji + "b", emoji).get());
    }

    @Test
    public void testTypeReference_IllegalStateGuardIsDocumentedAndStillPresent() throws Exception {
        // C1-J3: the @throws was restored because the guard is still in the constructor. Assert against the source,
        // since javadoc is not visible at runtime and the guard is deliberately unreachable.
        final java.nio.file.Path src = java.nio.file.Path.of("src/main/java/com/landawn/abacus/util/TypeReference.java");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(src), "run from the module root");

        final String body = java.nio.file.Files.readString(src);

        Assertions.assertTrue(body.contains("throw new IllegalStateException("), "the defensive guard must still be present");
        Assertions.assertEquals(2, countOccurrences(body, "@throws IllegalStateException"),
                "both TypeReference() and TypeToken() must document the guard they can still trigger");
    }

    private static int countOccurrences(final String haystack, final String needle) {
        int n = 0;

        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }

        return n;
    }

    @Test
    public void testKeyed_EqualityIsClassExactAndSymmetricWithIndexedKeyed() {
        final Keyed<String, Integer> plain = Keyed.of("k", 1);
        final IndexedKeyed<String, Integer> indexed = IndexedKeyed.of("k", 1, 0);

        Assertions.assertNotEquals(plain, indexed);
        Assertions.assertNotEquals(indexed, plain);

        // Both can live in one hash-based collection without either shadowing the other.
        final Set<Keyed<String, Integer>> set = new HashSet<>();
        set.add(plain);
        set.add(indexed);
        Assertions.assertEquals(2, set.size());

        // Within each type, only the declared components matter.
        Assertions.assertEquals(plain, Keyed.of("k", 999));
        Assertions.assertEquals(indexed, IndexedKeyed.of("k", 999, 0));
        Assertions.assertNotEquals(indexed, IndexedKeyed.of("k", 1, 1));
    }
}
