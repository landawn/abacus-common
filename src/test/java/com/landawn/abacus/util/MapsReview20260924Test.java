package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;

/**
 * Regression tests for the Maps findings of the 2026-09-24 review (cycle 1). See
 * {@code scripts/cross_review/Strings_IOUtil_CommonUtil_N_Array_Iterables_Iterators_Maps_Beans_Files_Multiset_Multimap_ledger_2026-09-24.md}.
 */
public class MapsReview20260924Test extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // C-374: symmetricDifference - an empty first map hands the result template to map2, like a null one
    // ------------------------------------------------------------------------------------------------

    private static TreeMap<String, Integer> caseInsensitive(final Object... kvs) {
        final TreeMap<String, Integer> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < kvs.length; i += 2) {
            m.put((String) kvs[i], (Integer) kvs[i + 1]);
        }

        return m;
    }

    @Test
    public void testC374_emptyFirstMapMirrorsSecondMapLikeNull() {
        final TreeMap<String, Integer> ci = caseInsensitive("A", 1, "b", 2);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> fromEmpty = Maps.symmetricDifference(new HashMap<>(), ci);
        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> fromNull = Maps.symmetricDifference(null, ci);

        assertTrue(fromEmpty instanceof TreeMap, fromEmpty.getClass().getName());
        assertEquals(fromNull.getClass(), fromEmpty.getClass());
        // map2's case-insensitive key semantics are kept, so a lookup in the other case finds the entry
        assertNotNull(fromEmpty.get("a"));
        assertTrue(fromEmpty.get("a").left().isEmpty());
        assertEquals(Nullable.of(1), fromEmpty.get("a").right());
        assertEquals(new ArrayList<>(fromNull.keySet()), new ArrayList<>(fromEmpty.keySet()));
        assertEquals(fromNull.get("B"), fromEmpty.get("B"));
    }

    @Test
    public void testC374_emptyFirstMapUnicodeKeys() {
        final TreeMap<String, Integer> ci = caseInsensitive("Ärger", 1, "été", 2);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(new LinkedHashMap<>(), ci);

        assertEquals(2, result.size());
        assertEquals(Nullable.of(1), result.get("ärger").right());
        assertEquals(Nullable.of(2), result.get("ÉTÉ").right());
    }

    @Test
    public void testC374_emptyOrNullBothSides() {
        // nothing supplies keys: the first map keeps its own type, a null pair still yields an empty map
        assertTrue(Maps.symmetricDifference(new TreeMap<String, Integer>(), null) instanceof TreeMap);
        assertTrue(Maps.symmetricDifference(new TreeMap<String, Integer>(), null).isEmpty());
        assertTrue(Maps.symmetricDifference((Map<String, Integer>) null, (Map<String, Integer>) null).isEmpty());
        assertTrue(Maps.symmetricDifference(new HashMap<String, Integer>(), caseInsensitive()) instanceof TreeMap);
    }

    @Test
    public void testC374_nonEmptyFirstMapStillDictatesTemplate() {
        final Map<String, Integer> first = new LinkedHashMap<>();
        first.put("a", 1);

        final Map<String, Integer> second = new HashMap<>();
        second.put("a", 1);
        second.put("z", 9);

        final Map<String, Pair<Nullable<Integer>, Nullable<Integer>>> result = Maps.symmetricDifference(first, second);

        assertTrue(result instanceof LinkedHashMap);
        assertFalse(result.containsKey("a"));
        assertEquals(Nullable.of(9), result.get("z").right());
        assertTrue(result.get("z").left().isEmpty());
    }

    // ------------------------------------------------------------------------------------------------
    // C-375: filter(Map, BiPredicate, IntFunction) rejects a supplier that returns the input map
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testC375_filterSupplierReturningInputMapIsRejected() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Maps.filter(map, (k, v) -> v > 1, n -> map));

        assertEquals("mapSupplier must create a new map", e.getMessage());
        assertEquals(2, map.size());
    }

    @Test
    public void testC375_filterSupplierReturningEmptyInputMapIsRejected() {
        final Map<String, Integer> empty = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> Maps.filter(empty, (k, v) -> true, n -> empty));
    }

    @Test
    public void testC375_filterSupplierNullAndFreshMaps() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("über", 3);
        map.put("b", 1);

        final NullPointerException e = assertThrows(NullPointerException.class, () -> Maps.filter(map, (k, v) -> true, n -> null));
        assertEquals("mapSupplier returned null", e.getMessage());

        final Map<String, Integer> other = new TreeMap<>();
        final Map<String, Integer> result = Maps.filter(map, (k, v) -> v > 2, n -> other);
        assertSame(other, result);
        assertEquals(Collections.singletonMap("über", 3), result);

        assertTrue(Maps.filter((Map<String, Integer>) null, (k, v) -> true, n -> new HashMap<>()).isEmpty());
    }

    // ------------------------------------------------------------------------------------------------
    // C-376: invert(Map, BiFunction) - a null merge result removes the entry (Map.merge rule)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testC376_invertNullMergeResultRemovesEntry() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("k1", "v");
        map.put("k2", "v");
        map.put("k3", "v");
        final List<String> calls = new ArrayList<>();

        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
            calls.add(oldKey + "|" + newKey);
            return null;
        });

        // k1 stored, k1+k2 merge to null -> removed, k3 then stored directly without a merge call
        assertEquals(Collections.singletonMap("v", "k3"), inverted);
        assertEquals(Arrays.asList("k1|k2"), calls);
    }

    @Test
    public void testC376_invertNullMergeOnConcurrentHashMapDoesNotThrow() {
        final Map<String, String> map = new ConcurrentHashMap<>();
        map.put("a", "x");
        map.put("b", "x");

        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> null);

        assertTrue(inverted.isEmpty(), inverted.toString());
    }

    @Test
    public void testC376_invertNullMergeThenNonNullMerge() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("ä", "v");
        map.put("b", "v");
        map.put("c", "v");
        map.put("d", "v");
        map.put("e", "w");

        // null for the first collision, concatenation afterwards
        final int[] n = { 0 };
        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> n[0]++ == 0 ? null : oldKey + "," + newKey);

        final Map<String, String> expected = new LinkedHashMap<>();
        expected.put("v", "c,d");
        expected.put("w", "e");
        assertEquals(expected, inverted);
    }

    @Test
    public void testC376_invertNullInputKeyIsStillAGenuinePreviousKey() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put(null, "v");
        map.put("k", "v");
        final List<String> calls = new ArrayList<>();

        final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
            calls.add(oldKey + "|" + newKey);
            return oldKey == null ? "was-null" : oldKey;
        });

        assertEquals(Collections.singletonMap("v", "was-null"), inverted);
        assertEquals(Arrays.asList("null|k"), calls);
    }

    @Test
    public void testC376_invertNullAndEmptyInput() {
        assertTrue(Maps.invert((Map<String, String>) null, (a, b) -> null).isEmpty());
        assertTrue(Maps.invert(new HashMap<String, String>(), (a, b) -> null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Maps.invert(new HashMap<String, String>(), null));
    }

    // ------------------------------------------------------------------------------------------------
    // C-384: getByPath* - a key a traversed map cannot hold is "unresolvable", not an exception
    // ------------------------------------------------------------------------------------------------

    private static Map<String, Object> rootWithIntegerKeyedMap() {
        final TreeMap<Integer, String> years = new TreeMap<>();
        years.put(2020, "x");

        final Map<String, Object> root = new HashMap<>();
        root.put("years", years);
        root.put("list", Collections.singletonList(years));

        return root;
    }

    @Test
    public void testC384_integerKeyedNestedMapIsUnresolvable() {
        final Map<String, Object> root = rootWithIntegerKeyedMap();

        assertNull(Maps.getByPath(root, "years.b"));
        assertFalse(Maps.getByPathIfExists(root, "years.2020").isPresent());
        assertNull(Maps.getByPath(root, "years.x.y"));
        assertNull(Maps.getByPath(root, "years.x[0]"));
        assertNull(Maps.getByPath(root, "list[0].x"));
        assertFalse(Maps.getByPathAsInt(root, "years.2020").isPresent());
        assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(root, "years.2020", -1));
        assertFalse(Maps.getByPathAsString(root, "years.é").isPresent());
        assertFalse(Maps.getByPathAs(root, "years.2020", String.class).isPresent());
        assertEquals("d", Maps.getByPathAsOrDefaultIfAbsent(root, "years.2020", "d", String.class));
        // the String-keyed part of the same structure still resolves
        assertTrue(Maps.getByPath(root, "years") instanceof TreeMap);
    }

    @Test
    public void testC384_nullPathOnNullHostileMapIsUnresolvable() {
        final TreeMap<String, Object> tree = new TreeMap<>();
        tree.put("a", 1);
        final ConcurrentHashMap<String, Object> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);

        assertNull(Maps.getByPath(tree, null));
        assertNull(Maps.getByPath(chm, null));
        assertFalse(Maps.getByPathIfExists(tree, null).isPresent());
        assertFalse(Maps.getByPathIfExists(chm, null).isPresent());

        // a map that does allow the null key still answers it
        final Map<String, Object> hash = new HashMap<>();
        hash.put(null, "n");
        assertEquals("n", Maps.getByPath(hash, null));
    }

    @Test
    public void testC384_stringKeyedTreeMapsStillResolve() {
        final TreeMap<String, Object> inner = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        inner.put("Name", "张三");
        final Map<String, Object> root = new HashMap<>();
        root.put("user", inner);

        assertEquals("张三", Maps.getByPath(root, "user.name"));
        assertTrue(Maps.getByPathIfExists(root, "user.NAME").isPresent());
        assertFalse(Maps.getByPathIfExists(root, "user.age").isPresent());
    }

    @Test
    public void testC384_conversionFailuresStillPropagate() {
        final Map<String, Object> inner = new TreeMap<>();
        inner.put("age", "abc");
        final Map<String, Object> root = new HashMap<>();
        root.put("user", inner);

        assertThrows(NumberFormatException.class, () -> Maps.getByPathAsInt(root, "user.age"));
        assertThrows(NumberFormatException.class, () -> Maps.getByPathAs(root, "user.age", Integer.class));
    }

    // ------------------------------------------------------------------------------------------------
    // C-385: isEmptyText - any non-Number value whose string form is empty is absent
    // ------------------------------------------------------------------------------------------------

    private static final Object EMPTY_TO_STRING = new Object() {
        @Override
        public String toString() {
            return "";
        }
    };

    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    @Test
    public void testC385_emptyStringFormIsAbsentForNumericAccessors() {
        final Map<String, Object> map = new HashMap<>();
        map.put("e", EMPTY_TO_STRING);
        map.put("n", NULL_TO_STRING);
        map.put("j", new StringJoiner(","));

        for (final String key : Arrays.asList("e", "n", "j")) {
            assertFalse(Maps.getAsByte(map, key).isPresent(), key);
            assertFalse(Maps.getAsShort(map, key).isPresent(), key);
            assertFalse(Maps.getAsInt(map, key).isPresent(), key);
            assertFalse(Maps.getAsLong(map, key).isPresent(), key);
            assertFalse(Maps.getAsFloat(map, key).isPresent(), key);
            assertFalse(Maps.getAsDouble(map, key).isPresent(), key);
            assertEquals(-7, Maps.getAsIntOrDefaultIfAbsent(map, key, -7), key);
            assertEquals(-7L, Maps.getAsLongOrDefaultIfAbsent(map, key, -7L), key);
            assertEquals(-7.5d, Maps.getAsDoubleOrDefaultIfAbsent(map, key, -7.5d), key);
            // agrees with the typed accessor
            assertFalse(Maps.getAs(map, key, Integer.class).isPresent(), key);
        }

        final Map<String, Object> root = new HashMap<>();
        root.put("user", map);
        assertFalse(Maps.getByPathAsInt(root, "user.e").isPresent());
        assertEquals(5, Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.j", 5));
    }

    @Test
    public void testC385_nonEmptyStringFormsAreStillConverted() {
        final Map<String, Object> map = new HashMap<>();
        map.put("blank", new StringJoiner(",", " ", ""));
        map.put("bool", Boolean.TRUE);
        map.put("num", new StringBuilder("42"));
        map.put("uni", "١"); // Arabic-Indic digit one: not ASCII, rejected
        map.put("empty", "");
        map.put("zero", 0);

        assertThrows(NumberFormatException.class, () -> Maps.getAsInt(map, "blank"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsInt(map, "bool"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsInt(map, "uni"));
        assertEquals(42, Maps.getAsInt(map, "num").getAsInt());
        assertFalse(Maps.getAsInt(map, "empty").isPresent());
        assertEquals(0, Maps.getAsInt(map, "zero").getAsInt());
    }

    // ------------------------------------------------------------------------------------------------
    // C-386: getAsChar - a long overflow is reported in char terms (type unchanged: ArithmeticException)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testC386_charOverflowMessageDoesNotLeakLong() {
        final Map<String, Object> map = new HashMap<>();
        map.put("nan", Double.NaN);
        map.put("inf", Double.NEGATIVE_INFINITY);
        map.put("huge", 1e30);
        map.put("hugeText", "99999999999999999999");

        for (final String key : Arrays.asList("nan", "inf", "huge", "hugeText")) {
            final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Maps.getAsChar(map, key), key);
            assertTrue(e.getMessage().startsWith("Value out of char range: "), e.getMessage());
            assertFalse(e.getMessage().contains("long"), e.getMessage());
            assertTrue(e.getCause() instanceof ArithmeticException, key);

            final ArithmeticException e2 = assertThrows(ArithmeticException.class, () -> Maps.getAsCharOrDefaultIfAbsent(map, key, 'x'), key);
            assertEquals(e.getMessage(), e2.getMessage());
        }

        assertEquals("Value out of char range: NaN", assertThrows(ArithmeticException.class, () -> Maps.getAsChar(map, "nan")).getMessage());
        assertEquals("Value out of char range: 99999999999999999999",
                assertThrows(ArithmeticException.class, () -> Maps.getAsChar(map, "hugeText")).getMessage());
    }

    @Test
    public void testC386_inRangeAndIllegalArgumentPathsUnchanged() {
        final Map<String, Object> map = new HashMap<>();
        map.put("neg", -1);
        map.put("big", "65536");
        map.put("a", 65.9);
        map.put("hex", "0x41");
        map.put("max", 65535L);

        assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "neg"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "big"));
        assertEquals('A', Maps.getAsChar(map, "a").get());
        assertEquals('A', Maps.getAsChar(map, "hex").get());
        assertEquals('￿', Maps.getAsChar(map, "max").get());
    }

    // ------------------------------------------------------------------------------------------------
    // Doc-only rows: pin the behaviour the corrected javadoc now states (C-380, C-390)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testC390_numberNaNReadsAsFalse() {
        final Map<String, Object> map = new HashMap<>();
        map.put("nan", Double.NaN);
        map.put("fnan", Float.NaN);
        map.put("half", 0.5d);

        assertFalse(Maps.getAsBoolean(map, "nan").get());
        assertFalse(Maps.getAsBoolean(map, "fnan").get());
        assertTrue(Maps.getAsBoolean(map, "half").get());
        assertFalse(Maps.getAsBooleanOrDefaultIfAbsent(map, "nan", true));
    }

    @Test
    public void testC380_flatInvertRepeatsKeyPerOccurrence() {
        final Map<String, List<Integer>> map = new LinkedHashMap<>();
        map.put("A", Arrays.asList(1, 1, 2));

        final Map<Integer, List<String>> inverted = Maps.flatInvert(map);

        assertEquals(Arrays.asList("A", "A"), inverted.get(1));
        assertEquals(Arrays.asList("A"), inverted.get(2));
    }

    @Test
    public void testC380_snakeCaseCollapsesUnderscoreRuns() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("user__name", 1);
        Maps.replaceKeysWithSnakeCase(map);
        assertEquals(Collections.singletonMap("user_name", 1), map);

        final Map<String, Object> clash = new LinkedHashMap<>();
        clash.put("user__name", 1);
        clash.put("user_name", 2);
        assertThrows(IllegalStateException.class, () -> Maps.replaceKeysWithSnakeCase(clash));

        final Map<String, Object> screaming = new LinkedHashMap<>();
        screaming.put("USER__NAME", 1);
        Maps.replaceKeysWithScreamingSnakeCase(screaming);
        assertEquals(Collections.singletonMap("USER_NAME", 1), screaming);
    }

    // ================================================================================================
    // V2 items (verifier-confirmed)
    // ================================================================================================

    // ------------------------------------------------------------------------------------------------
    // C-371 (+C-383): a primitive numeric target type treats empty text as absent, like its wrapper
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testC371_primitiveNumericTargetsTreatEmptyTextAsAbsent() {
        final Map<String, Object> map = new HashMap<>();
        map.put("e", "");
        map.put("sb", new StringBuilder());

        for (final String key : Arrays.asList("e", "sb")) {
            for (final Class<?> cls : Arrays.<Class<?>> asList(int.class, long.class, short.class, byte.class, float.class, double.class)) {
                assertFalse(Maps.getAs(map, key, cls).isPresent(), key + " " + cls);
                assertFalse(Maps.getAs(map, key, com.landawn.abacus.type.Type.of(cls)).isPresent(), key + " Type " + cls);
            }

            assertEquals(5, Maps.getAsOrDefaultIfAbsent(map, key, 5, int.class), key);
            assertEquals(5L, Maps.getAsOrDefaultIfAbsent(map, key, 5L, long.class), key);
            assertEquals((short) 5, Maps.getAsOrDefaultIfAbsent(map, key, (short) 5, short.class), key);
            assertEquals((byte) 5, Maps.getAsOrDefaultIfAbsent(map, key, (byte) 5, byte.class), key);
            assertEquals(5.5f, Maps.getAsOrDefaultIfAbsent(map, key, 5.5f, float.class), key);
            assertEquals(5.5d, Maps.getAsOrDefaultIfAbsent(map, key, 5.5d, double.class), key);
        }
    }

    @Test
    public void testC383_getByPathAsPrimitiveTargetTreatsEmptyTextAsAbsent() {
        final Map<String, Object> user = new HashMap<>();
        user.put("score", "");
        user.put("age", "25");
        final Map<String, Object> root = new HashMap<>();
        root.put("u", user);

        assertFalse(Maps.getByPathAs(root, "u.score", int.class).isPresent());
        assertFalse(Maps.getByPathAs(root, "u.score", double.class).isPresent());
        assertEquals(-1, Maps.getByPathAsOrDefaultIfAbsent(root, "u.score", -1, int.class));
        assertEquals(9.5d, Maps.getByPathAsOrDefaultIfAbsent(root, "u.score", 9.5d, double.class));
        assertEquals(25, Maps.getByPathAs(root, "u.age", int.class).get());
        assertEquals(25L, Maps.getByPathAsOrDefaultIfAbsent(root, "u.age", -1L, long.class));
    }

    @Test
    public void testC371_primitiveTargetControls() {
        final Map<String, Object> map = new HashMap<>();
        final Integer seven = 7000;
        final int[] ints = { 1, 2 };
        map.put("text", "7");
        map.put("int", seven);
        map.put("blank", " ");
        map.put("empty", "");
        map.put("ints", ints);
        map.put("uni", "١");

        assertEquals(7, Maps.getAs(map, "text", int.class).get());
        assertEquals(7L, Maps.getAs(map, "text", com.landawn.abacus.type.Type.of(long.class)).get());
        assertSame(seven, Maps.getAs(map, "int", int.class).get());
        assertEquals(7000L, Maps.getAs(map, "int", long.class).get());
        assertThrows(NumberFormatException.class, () -> Maps.getAs(map, "blank", int.class));
        assertThrows(NumberFormatException.class, () -> Maps.getAs(map, "uni", int.class));
        // char/boolean keep their documented "" rules
        assertEquals('\0', Maps.getAs(map, "empty", char.class).get());
        assertFalse(Maps.getAs(map, "empty", boolean.class).get());
        // an array target is not "wrapped" into Integer[]
        assertSame(ints, Maps.getAs(map, "ints", int[].class).get());
    }

    // ------------------------------------------------------------------------------------------------
    // C-372: replaceKeys on a map whose key equivalence a scratch copy cannot reproduce
    // ------------------------------------------------------------------------------------------------

    private static Map<String, Integer> syncCaseInsensitive(final Object... kvs) {
        return Collections.synchronizedMap(caseInsensitive(kvs));
    }

    @Test
    public void testC372_synchronizedCaseInsensitiveDuplicateThrowsAndRestores() {
        final Map<String, Integer> map = syncCaseInsensitive("a", 1, "b", 2, "c", 3);

        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Maps.replaceKeys(map, k -> k.equals("a") ? "X" : k.equals("b") ? "x" : "Z"));

        assertEquals("Duplicate new key: x - produced by both 'a' and 'b'", e.getMessage());
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(map.values()));
    }

    @Test
    public void testC372_plainCaseInsensitiveTreeMapStillThrowsBeforeModifying() {
        final TreeMap<String, Integer> map = caseInsensitive("a", 1, "b", 2);

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> k.equals("a") ? "X" : "x"));
        assertEquals(caseInsensitive("a", 1, "b", 2), map);
    }

    @Test
    public void testC372_synchronizedCaseInsensitiveMergeIsApplied() {
        final Map<String, Integer> map = syncCaseInsensitive("a", 1, "b", 2);

        Maps.replaceKeys(map, k -> k.equals("a") ? "X" : "x", Integer::sum);

        assertEquals(1, map.size());
        assertEquals(3, map.get("X"));
        assertEquals(3, map.get("x"));
    }

    @Test
    public void testC372_synchronizedMergeNullResultRule() {
        final Map<String, Integer> map = Collections.synchronizedMap(new LinkedHashMap<>());
        map.put("a1", 1);
        map.put("a2", 2);
        map.put("a3", 3);

        Maps.replaceKeys(map, k -> "a", (existing, incoming) -> existing == 1 ? null : existing + incoming);

        assertEquals(Collections.singletonMap("a", 3), map);
    }

    @Test
    public void testC372_synchronizedIdentityMapKeepsDistinctKeys() {
        final Map<String, Integer> map = Collections.synchronizedMap(new java.util.IdentityHashMap<>());
        map.put("p", 1);
        map.put("q", 2);

        Maps.replaceKeys(map, k -> new String("K"));

        assertEquals(2, map.size());
    }

    @Test
    public void testC372_synchronizedLinkedHashMapKeepsEncounterOrder() {
        final Map<String, Integer> map = Collections.synchronizedMap(new LinkedHashMap<>());
        final List<String> keys = Arrays.asList("zeta", "alpha", "mu", "beta", "omega", "été", "k10", "k3");

        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), i);
        }

        Maps.replaceKeys(map, k -> k.toUpperCase(java.util.Locale.ROOT));

        assertEquals(Arrays.asList("ZETA", "ALPHA", "MU", "BETA", "OMEGA", "ÉTÉ", "K10", "K3"), new ArrayList<>(map.keySet()));

        final Map<String, Integer> merged = Collections.synchronizedMap(new LinkedHashMap<>());
        merged.put("b1", 1);
        merged.put("a1", 2);
        merged.put("b2", 3);
        Maps.replaceKeys(merged, k -> k.substring(0, 1), Integer::sum);
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(merged.keySet()));
        assertEquals(4, merged.get("b"));
    }

    @Test
    public void testC372_failuresLeaveSynchronizedMapIntact() {
        final Map<String, Integer> map = syncCaseInsensitive("a", 1, "b", 2, "c", 3);

        assertThrows(IllegalArgumentException.class, () -> Maps.replaceKeys(map, k -> {
            if (k.equals("c")) {
                throw new IllegalArgumentException("boom");
            }
            return k + "!";
        }));
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));

        assertThrows(ArithmeticException.class, () -> Maps.replaceKeys(map, k -> "same", (x, y) -> {
            throw new ArithmeticException("merge");
        }));
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(map.values()));

        // a key the real map rejects while refilling: the snapshot is put back
        final Map<String, Integer> chm = Collections.synchronizedMap(new ConcurrentHashMap<>());
        chm.put("a", 1);
        assertThrows(NullPointerException.class, () -> Maps.replaceKeys(chm, k -> null));
        assertEquals(Collections.singletonMap("a", 1), chm);
    }

    @Test
    public void testC372_unmodifiableMapReportsDuplicateBeforeUnsupported() {
        final Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        final Map<String, Integer> map = Collections.unmodifiableMap(source);

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> "same"));
        assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(map, k -> k + "!"));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(source.keySet()));
    }

    @Test
    public void testC372_emptyAndNullMapsAreNoOps() {
        Maps.replaceKeys((Map<String, Integer>) null, k -> "x");
        final Map<String, Integer> empty = Collections.synchronizedMap(new HashMap<>());
        Maps.replaceKeys(empty, k -> "x");
        Maps.replaceKeys(empty, k -> "x", Integer::sum);
        assertTrue(empty.isEmpty());
    }

    // ------------------------------------------------------------------------------------------------
    // C-379: the key-query helpers treat a key the map rejects as absent
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testC379_containsAndRemoveEntryToleratesRejectedKeys() {
        final Map<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);
        final TreeMap<String, Integer> natural = new TreeMap<>();
        natural.put("a", 1);

        assertFalse(Maps.containsEntry(chm, null, 1));
        assertFalse(Maps.containsEntry(chm, new java.util.AbstractMap.SimpleEntry<>(null, 1)));
        assertFalse(Maps.containsEntry(natural, null, 1));
        assertFalse(Maps.containsEntry(natural, 1, 1));
        assertFalse(Maps.removeEntry(chm, null, 1));
        assertFalse(Maps.removeEntry(chm, new java.util.AbstractMap.SimpleEntry<>(null, 1)));
        assertFalse(Maps.removeEntry(natural, 1, 1));
        assertEquals(Collections.singletonMap("a", 1), chm);
        assertEquals(Collections.singletonMap("a", 1), natural);

        // controls: a map that holds the null key still answers it
        final Map<String, Integer> hash = new HashMap<>();
        hash.put(null, 1);
        assertTrue(Maps.containsEntry(hash, null, 1));
        assertFalse(Maps.containsEntry(hash, null, 2));
        assertTrue(Maps.removeEntry(hash, null, 1));
        assertTrue(hash.isEmpty());
        assertTrue(Maps.containsEntry(natural, "a", 1));
    }

    // ------------------------------------------------------------------------------------------------
    // C-381: a template class never receives a guessed size (LRU subclass)
    // ------------------------------------------------------------------------------------------------

    /** An LRU map whose int constructor is an eviction cap, not a capacity. */
    public static class Lru<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int maxEntries;

        public Lru(final int maxEntries) {
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return size() > maxEntries;
        }
    }

    private static Lru<String, Integer> lru10() {
        final Lru<String, Integer> lru = new Lru<>(10);

        for (int i = 0; i < 10; i++) {
            lru.put("k" + i, i);
        }

        return lru;
    }

    @Test
    public void testC381_lruTemplateKeepsEveryResultEntry() {
        final Lru<String, Integer> lru = lru10();

        final Map<String, Integer> inter = Maps.intersection(lru, new HashMap<>(lru));
        assertEquals(10, inter.size());
        assertTrue(inter instanceof Lru);

        final Map<String, Integer> other = new HashMap<>();
        other.put("k0", 99);
        assertEquals(10, Maps.difference(lru, other).size());

        final Map<String, Integer> disjoint = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            disjoint.put("d" + i, i);
        }
        assertEquals(20, Maps.symmetricDifference(lru, disjoint).size());

        assertEquals(10, Maps.filter(lru, (k, v) -> true).size());
    }

    @Test
    public void testC381_emptyIntersectionOfLruTemplateAcceptsLaterPuts() {
        final Map<String, Integer> result = Maps.intersection(lru10(), new HashMap<>());

        assertTrue(result.isEmpty());
        result.put("x", 1);
        assertEquals(Collections.singletonMap("x", 1), result);
    }

    // ------------------------------------------------------------------------------------------------
    // C-382 + C-377: one template policy
    // ------------------------------------------------------------------------------------------------

    /** A case-insensitive map keyed by lower-cased Strings; a non-String key is a ClassCastException. */
    public static class CiMap<V> extends LinkedHashMap<Object, V> {
        private static final long serialVersionUID = 1L;

        public CiMap() {
        }

        public CiMap(final int capacity) {
            super(capacity);
        }

        @Override
        public V put(final Object key, final V value) {
            return super.put(((String) key).toLowerCase(java.util.Locale.ROOT), value);
        }

        @Override
        public V get(final Object key) {
            return super.get(((String) key).toLowerCase(java.util.Locale.ROOT));
        }

        @Override
        public boolean containsKey(final Object key) {
            return super.containsKey(((String) key).toLowerCase(java.util.Locale.ROOT));
        }

        @Override
        public V computeIfAbsent(final Object key, final java.util.function.Function<? super Object, ? extends V> mappingFunction) {
            final V existing = get(key);

            if (existing != null) {
                return existing;
            }

            final V created = mappingFunction.apply(key);
            put(key, created);
            return created;
        }
    }

    @Test
    public void testC382_invertOfCaseInsensitiveMapKeepsBothValues() {
        final CiMap<Object> map = new CiMap<>();
        map.put("a", "X");
        map.put("b", "x");

        final Map<Object, Object> inverted = Maps.invert(map);

        assertEquals(LinkedHashMap.class, inverted.getClass());
        assertEquals(2, inverted.size());
        assertEquals("a", inverted.get("X"));
        assertEquals("b", inverted.get("x"));

        // non-String values: used to be a ClassCastException from the mirrored map
        final CiMap<Object> numbers = new CiMap<>();
        numbers.put("a", 1);
        assertEquals(Collections.singletonMap(1, "a"), Maps.invert(numbers));
        assertEquals(Collections.singletonMap(1, "a"), Maps.invert(numbers, (x, y) -> x));
    }

    @Test
    public void testC382_flatInvertOfCaseInsensitiveMapKeepsBothElements() {
        final CiMap<List<String>> map = new CiMap<>();
        map.put("a", Arrays.asList("X", "x"));

        final Map<String, List<Object>> inverted = Maps.flatInvert(map);

        assertEquals(2, inverted.size());
        assertEquals(Arrays.asList("a"), inverted.get("X"));
        assertEquals(Arrays.asList("a"), inverted.get("x"));
    }

    @Test
    public void testC382_nullHostileTemplatesAcceptNullElements() {
        final Map<String, List<Integer>> chm = new ConcurrentHashMap<>();
        chm.put("a", Arrays.asList(1, null));
        chm.put("b", Arrays.asList(2, 3));
        final Map<String, List<Integer>> table = new java.util.Hashtable<>(chm);

        for (final Map<String, List<Integer>> template : Arrays.asList(chm, table)) {
            final Map<Integer, List<String>> inverted = Maps.flatInvert(template);
            assertEquals(Arrays.asList("a"), inverted.get(null), template.getClass().getName());
            assertEquals(Arrays.asList("a"), inverted.get(1));

            final List<Map<String, Integer>> rows = Maps.transpose(template);
            assertEquals(2, rows.size());
            assertTrue(rows.get(1).containsKey("a"));
            assertNull(rows.get(1).get("a"));
            assertEquals(3, rows.get(1).get("b"));
        }
    }

    @Test
    public void testC382_invertResultTypes() {
        final Map<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);
        assertEquals(LinkedHashMap.class, Maps.invert(chm).getClass());

        final Map<String, Integer> hash = new HashMap<>();
        hash.put("a", 1);
        assertEquals(HashMap.class, Maps.invert(hash).getClass());

        final Map<String, Integer> weak = new java.util.WeakHashMap<>();
        weak.put("a", 1);
        assertEquals(LinkedHashMap.class, Maps.invert(weak).getClass());

        final BiMap<String, Integer> biMap = new BiMap<>();
        biMap.put("a", 1);
        assertTrue(Maps.invert(biMap) instanceof BiMap);
    }

    @Test
    public void testC377_unconstructibleOrderedTemplatesKeepOrder() {
        final Map<String, Integer> source = new LinkedHashMap<>();
        final List<String> keys = Arrays.asList("zeta", "alpha", "mu", "beta", "omega", "über");

        for (int i = 0; i < keys.size(); i++) {
            source.put(keys.get(i), i);
        }

        for (final Map<String, Integer> template : Arrays.asList(Collections.unmodifiableMap(source), Collections.synchronizedMap(source),
                ImmutableMap.copyOf(source))) {
            final Map<String, Integer> filtered = Maps.filter(template, (k, v) -> true);
            assertEquals(LinkedHashMap.class, filtered.getClass(), template.getClass().getName());
            assertEquals(keys, new ArrayList<>(filtered.keySet()));
            assertEquals(keys, new ArrayList<>(Maps.filterByKey(template, k -> true).keySet()));
            assertEquals(keys, new ArrayList<>(Maps.intersection(template, source).keySet()));
        }
    }

    @Test
    public void testC377_enumMapTemplateStaysEnumMap() {
        final java.util.EnumMap<java.util.concurrent.TimeUnit, Integer> map = new java.util.EnumMap<>(java.util.concurrent.TimeUnit.class);
        map.put(java.util.concurrent.TimeUnit.SECONDS, 1);
        map.put(java.util.concurrent.TimeUnit.NANOSECONDS, 2);
        map.put(java.util.concurrent.TimeUnit.DAYS, 3);

        final Map<java.util.concurrent.TimeUnit, Integer> filtered = Maps.filter(map, (k, v) -> v > 1);
        assertTrue(filtered instanceof java.util.EnumMap);
        assertEquals(Arrays.asList(java.util.concurrent.TimeUnit.NANOSECONDS, java.util.concurrent.TimeUnit.DAYS), new ArrayList<>(filtered.keySet()));
        assertTrue(Maps.intersection(map, new HashMap<>(map)) instanceof java.util.EnumMap);

        final java.util.EnumMap<java.util.concurrent.TimeUnit, Integer> empty = new java.util.EnumMap<>(java.util.concurrent.TimeUnit.class);
        assertTrue(Maps.newTargetMap(empty) instanceof java.util.EnumMap);
        assertTrue(Maps.filter(empty, (k, v) -> true).isEmpty());
    }

    @Test
    public void testC377_concurrentSkipListTemplateStaysConcurrent() {
        final java.util.concurrent.ConcurrentSkipListMap<String, List<Integer>> map = new java.util.concurrent.ConcurrentSkipListMap<>(
                java.util.Comparator.reverseOrder());
        map.put("a", Arrays.asList(1, null));
        map.put("b", Arrays.asList(2));

        final Map<String, List<Integer>> filtered = Maps.filter(map, (k, v) -> true);
        assertTrue(filtered instanceof java.util.concurrent.ConcurrentSkipListMap);
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(filtered.keySet()));

        // transpose rows may hold null: a TreeMap with the same comparator
        final List<Map<String, Integer>> rows = Maps.transpose(map);
        assertTrue(rows.get(1) instanceof TreeMap);
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(rows.get(0).keySet()));
        assertNull(rows.get(1).get("a"));
    }

    @Test
    public void testC377_nullTemplateAndCapacityClassesUnchanged() {
        assertEquals(HashMap.class, Maps.newTargetMap(null, 4).getClass());
        assertEquals(HashMap.class, Maps.newTargetMap(new HashMap<>(), 4).getClass());
        assertEquals(LinkedHashMap.class, Maps.newTargetMap(new LinkedHashMap<>(), 4).getClass());
        assertEquals(java.util.IdentityHashMap.class, Maps.newTargetMap(new java.util.IdentityHashMap<>(), 4).getClass());
        assertEquals(ConcurrentHashMap.class, Maps.newTargetMap(new ConcurrentHashMap<>(), 4).getClass());
    }
}
