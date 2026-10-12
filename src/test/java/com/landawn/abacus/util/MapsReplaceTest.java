package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class MapsReplaceTest extends MapsTestSupport {

    private static BiMap<String, Integer> caseInsensitiveMap() {
        final BiMap<String, Integer> map = new BiMap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), LinkedHashMap::new);
        map.put("x", 1);
        map.put("y", 2);
        return map;
    }

    // C-372: replaceKeys on a map whose key equivalence a scratch copy cannot reproduce
    private static Map<String, Integer> syncCaseInsensitive(final Object... kvs) {
        return Collections.synchronizedMap(caseInsensitive(kvs));
    }

    // replace

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
    public void testReplace_EmptyMap_ReturnsNull() {
        assertNull(Maps.replace(new HashMap<String, String>(), "key", "newValue"));
    }

    @Test
    public void testReplace_keyNotPresent() {
        Map<String, String> map = new HashMap<>(testMap);
        assertNull(Maps.replace(map, "missing", "newVal"));
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

    // replaceKeys

    @Test
    public void testC372_emptyAndNullMapsAreNoOps() {
        Maps.replaceKeys((Map<String, Integer>) null, k -> "x");
        final Map<String, Integer> empty = Collections.synchronizedMap(new HashMap<>());
        Maps.replaceKeys(empty, k -> "x");
        Maps.replaceKeys(empty, k -> "x", Integer::sum);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testU1604_faithfulScratchStillReportsDuplicatesBeforeTouchingTheMap() {
        // an unmodifiable SortedMap gets a faithful TreeMap scratch: the duplicate is decided there, before
        // clear() is ever attempted, so there is no UOE cause and equals-only instances collide as the
        // comparator says they do
        final TreeMap<String, Integer> tree = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tree.put("a", 1);
        tree.put("b", 2);
        final SortedMap<String, Integer> map = Collections.unmodifiableSortedMap(tree);

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> "b".equals(k) ? "A" : k));
        assertEquals("Duplicate new key: A - produced by both 'a' and 'b'", ex.getMessage());
        assertEquals(null, ex.getCause());
        assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(map, k -> k + "!"));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(map.keySet()));
    }

    // U16-05: the in-place path (no scratch fill) still decides duplicates on the real map
    @Test
    public void testU1605_inPlacePathDecidesDuplicatesOnTheRealMap() {
        final Map<String, Integer> map = Collections.synchronizedMap(ordered("a", "b", "c"));

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Maps.replaceKeys(map, k -> "c".equals(k) ? "A" : k.toUpperCase(Locale.ROOT)));
        assertEquals("Duplicate new key: A - produced by both 'a' and 'c'", ex.getMessage());
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));
        assertEquals(Arrays.asList(0, 1, 2), new ArrayList<>(map.values()));

        // a synchronized case-insensitive TreeMap decides under its own rule: "a" and "A" collide there
        final TreeMap<String, Integer> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ci.put("a", 1);
        ci.put("b", 2);
        final Map<String, Integer> sync = Collections.synchronizedMap(ci);
        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(sync, k -> "b".equals(k) ? "A" : k));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(sync.keySet()));

        // and a converter failure leaves the map untouched (every key is converted before clear())
        assertThrows(ArithmeticException.class, () -> Maps.replaceKeys(sync, k -> {
            if ("b".equals(k)) {
                throw new ArithmeticException("boom");
            }
            return k + "!";
        }));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(sync.keySet()));

        Maps.replaceKeys(sync, k -> k + "!");
        assertEquals(Arrays.asList("a!", "b!"), new ArrayList<>(sync.keySet()));
        assertEquals(Integer.valueOf(1), sync.get("A!"));
    }

    @Test
    public void testC372_plainCaseInsensitiveTreeMapStillThrowsBeforeModifying() {
        final TreeMap<String, Integer> map = caseInsensitive("a", 1, "b", 2);

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> k.equals("a") ? "X" : "x"));
        assertEquals(caseInsensitive("a", 1, "b", 2), map);
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
    public void testReplaceKeys_DuplicateConvertedKeyThrowsIllegalStateException() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("left", 1);
        map.put("right", 2);

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> "dup"));
    }

    @Test
    public void testReplaceKeys_emptyMap() {
        Map<String, Integer> map = new HashMap<>();
        Maps.replaceKeys(map, String::toUpperCase);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeys_nullMap() {
        assertDoesNotThrow(() -> {
            Maps.replaceKeys((Map<String, Integer>) null, String::toUpperCase);
            // no exception should be thrown
        });
    }

    @Test
    public void testReplaceKeys_NullValue_NoCollision_PutsNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("oldKey", null); // null value, no collision
        Maps.replaceKeys(map, k -> "newKey", (existing, incoming) -> existing);
        assertTrue(map.containsKey("newKey"));
        assertNull(map.get("newKey"));
        assertFalse(map.containsKey("oldKey"));
    }

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

    // G053-01: replaceKeys used the pre-filled scratch map as "faithful" and wrote its stray entries into the caller's map.
    @Test
    public void testReplaceKeys_prefilledAnonymousSourceClass() {
        final Map<String, Integer> m = g053DoubleBraceMapWithXRemoved();
        Maps.replaceKeys(m, k -> k.toUpperCase());
        assertEquals(CommonUtil.asMap("Y", 2, "Z", 3), m);

        final Map<String, Integer> m2 = g053DoubleBraceMapWithXRemoved();
        Maps.replaceKeys(m2, k -> k.toUpperCase(), Integer::sum);
        assertEquals(CommonUtil.asMap("Y", 2, "Z", 3), m2);
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
            if (k.equals("keyB") || k.equals("oldC")) {
                return "newKey";
            }
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
    public void testReplaceKeys_withMerger_emptyMap() {
        Map<String, Integer> map = new HashMap<>();
        Maps.replaceKeys(map, String::toUpperCase, Integer::sum);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeys_withMerger_nullMap() {
        assertDoesNotThrow(() -> {
            Maps.replaceKeys((Map<String, Integer>) null, String::toUpperCase, Integer::sum);
            // no exception should be thrown
        });
    }

    @Test
    public void testReplaceKeysDuplicateDoesNotRepeatConverter() {
        for (final Map<String, Integer> map : Arrays.<Map<String, Integer>> asList(new LinkedHashMap<>(), new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                new IdentityHashMap<>(), new CaseInsensitiveIntegerMap())) {
            map.put("first", 1);
            map.put("second", 2);
            final List<String> originalKeys = new ArrayList<>(map.keySet());
            final List<String> converted = new ArrayList<>();

            final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> {
                if (converted.contains(key)) {
                    throw new AssertionError("Converter called twice for " + key);
                }
                converted.add(key);
                return (map instanceof TreeMap || map instanceof CaseInsensitiveIntegerMap) && key.equals("second") ? "DUPLICATE" : "duplicate";
            }));

            assertEquals(originalKeys, converted);
            assertTrue(exception.getMessage().contains("'first'"), exception.getMessage());
            assertTrue(exception.getMessage().contains("'second'"), exception.getMessage());
            assertEquals(originalKeys, new ArrayList<>(map.keySet()));
            assertEquals(2, map.size());
            assertEquals(Integer.valueOf(1), map.get("first"));
            assertEquals(Integer.valueOf(2), map.get("second"));
        }

        final Map<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put("first", 1);
        identityMap.put("second", 2);
        final String firstTarget = new String("equal");
        final String secondTarget = new String("equal");
        final List<String> converted = new ArrayList<>();
        Maps.replaceKeys(identityMap, key -> {
            assertFalse(converted.contains(key), "Converter must be called once per source key");
            converted.add(key);
            return key.equals("first") ? firstTarget : secondTarget;
        });
        assertEquals(2, converted.size());
        assertEquals(2, identityMap.size());
        assertEquals(Integer.valueOf(1), identityMap.get(firstTarget));
        assertEquals(Integer.valueOf(2), identityMap.get(secondTarget));
    }

    @Test
    void replaceKeysMergesComparatorEquivalentKeys() {
        final BiMap<String, Integer> map = caseInsensitiveMap();
        final AtomicInteger merges = new AtomicInteger();

        Maps.replaceKeys(map, key -> key.equals("x") ? "a" : "A", (left, right) -> {
            merges.incrementAndGet();
            return left + right;
        });

        assertEquals(1, merges.get());
        assertEquals(1, map.size());
        assertEquals(3, map.get("A"));
        assertEquals("a", map.getByValue(3));
        assertFalse(map.inverse().containsKey(1));
        assertFalse(map.inverse().containsKey(2));
    }

    @Test
    void replaceKeysPreservesDistinctIdentityKeysWithEqualContents() {
        for (final boolean merge : new boolean[] { false, true }) {
            final BiMap<String, Integer> map = new BiMap<>(IdentityHashMap::new, LinkedHashMap::new);
            final String firstKey = new String("same");
            final String secondKey = new String("same");
            map.put("x", 1);
            map.put("y", 2);

            if (merge) {
                Maps.replaceKeys(map, key -> key.equals("x") ? firstKey : secondKey, Integer::sum);
            } else {
                Maps.replaceKeys(map, key -> key.equals("x") ? firstKey : secondKey);
            }

            assertEquals(2, map.size());
            assertEquals(1, map.get(firstKey));
            assertEquals(2, map.get(secondKey));
            assertSame(firstKey, map.getByValue(1));
            assertSame(secondKey, map.getByValue(2));
        }
    }

    @Test
    void replaceKeysPreservesDistinctIdentityValuesWithEqualContents() {
        for (final boolean merge : new boolean[] { false, true }) {
            final BiMap<String, String> map = new BiMap<>(LinkedHashMap::new, IdentityHashMap::new);
            final String firstValue = new String("same");
            final String secondValue = new String("same");
            map.put("x", firstValue);
            map.put("y", secondValue);

            if (merge) {
                Maps.replaceKeys(map, key -> "new-" + key, (left, right) -> left);
            } else {
                Maps.replaceKeys(map, key -> "new-" + key);
            }

            assertEquals(2, map.size());
            assertSame(firstValue, map.get("new-x"));
            assertSame(secondValue, map.get("new-y"));
            assertEquals("new-x", map.getByValue(firstValue));
            assertEquals("new-y", map.getByValue(secondValue));
        }
    }

    @Test
    public void testReplaceKeysRejectedKeyDoesNotMutateMap() {
        final Map<String, Integer> map = new ConcurrentHashMap<>();
        map.put("a", 1);

        assertThrows(NullPointerException.class, () -> Maps.replaceKeys(map, key -> null));
        assertEquals(Map.of("a", 1), map);
    }

    @Test
    void replaceKeysRejectsComparatorEquivalentKeysAndRestoresOriginalEntries() {
        final BiMap<String, Integer> map = caseInsensitiveMap();

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> key.equals("x") ? "a" : "A"));

        assertEquals(Map.of("x", 1, "y", 2), map);
        assertEquals(Map.of(1, "x", 2, "y"), map.inverse());
    }

    @Test
    void replaceKeysRestoresOriginalEntriesWhenComparatorMergeFails() {
        final BiMap<String, Integer> map = caseInsensitiveMap();
        final IllegalStateException failure = new IllegalStateException("merge failed");

        assertSame(failure, assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> key.equals("x") ? "a" : "A", (left, right) -> {
            throw failure;
        })));

        assertEquals(Map.of("x", 1, "y", 2), map);
        assertEquals(Map.of(1, "x", 2, "y"), map.inverse());
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
    public void testReplaceKeysWithMergerHandlesChainedRenames() {
        // regression: iterating a key snapshot while renaming let later iterations see earlier
        // renames - chained renames merged spuriously and results were iteration-order dependent
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Maps.replaceKeys(map, k -> k.equals("a") ? "b" : "c", Integer::sum);
        assertEquals(CommonUtil.asMap("b", 1, "c", 2), map); // distinct targets: no merge at all

        final Map<String, Integer> swapped = new LinkedHashMap<>();
        swapped.put("a", 1);
        swapped.put("b", 2);
        Maps.replaceKeys(swapped, k -> k.equals("a") ? "b" : "a", Integer::sum);
        assertEquals(CommonUtil.asMap("b", 1, "a", 2), swapped); // clean swap

        final Map<String, Integer> dup = new LinkedHashMap<>();
        dup.put("a", 1);
        dup.put("b", 2);
        Maps.replaceKeys(dup, k -> "x", Integer::sum);
        assertEquals(CommonUtil.asMap("x", 3), dup); // true duplicate targets still merge
    }

    @Test
    public void reviewFixes20260906_replaceKeysWithMergerCanLeaveTheMapEmpty() {
        // Doc-only: the two-argument overload claimed "the map is left untouched" for an
        // UnsupportedOperationException, but the body is clear() + putAll(), so a putAll that fails leaves the
        // map EMPTY. The single-argument sibling already said so; this pins both.
        final Map<String, Integer> withMerger = new NoPutAllLinkedHashMap<>();
        withMerger.put("a1", 1);
        withMerger.put("b1", 2);
        assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(withMerger, k -> k.substring(0, 1), Integer::sum));
        assertTrue(withMerger.isEmpty());

        final Map<String, Integer> noMerger = new NoPutAllLinkedHashMap<>();
        noMerger.put("a", 1);
        noMerger.put("b", 2);
        assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(noMerger, String::toUpperCase));
        assertTrue(noMerger.isEmpty());

        // Control: a failure BEFORE the clear() - during conversion/merging - really does leave the map untouched,
        // which is the half of the sentence that was right.
        final Map<String, Integer> untouched = new LinkedHashMap<>();
        untouched.put("a", 1);
        untouched.put("b", 2);
        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(untouched, k -> {
            throw new IllegalStateException("boom");
        }, Integer::sum));
        assertEquals(CommonUtil.asMap("a", 1, "b", 2), untouched);

        // Control: an ordinary map is rekeyed and merged as before.
        final Map<String, Integer> ok = new LinkedHashMap<>();
        ok.put("a1", 1);
        ok.put("a2", 2);
        Maps.replaceKeys(ok, k -> k.substring(0, 1), Integer::sum);
        assertEquals(CommonUtil.asMap("a", 3), ok);
    }

    @Test
    public void testU1604_sameReferenceCollisionOnAnUnclearableMapIsStillIllegalState() {
        final Map<String, Integer> map = Collections.unmodifiableMap(ordered("a", "b", "c"));

        // the same literal for every key: a duplicate under every key equivalence -> ISE, cause UOE
        final IllegalStateException same = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> "same"));
        assertEquals("Duplicate new key: same - produced by both 'a' and 'b'", same.getMessage());
        assertTrue(same.getCause() instanceof UnsupportedOperationException);

        // the first collision decides, and it names the earliest producer
        final IllegalStateException first = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> "b".equals(k) ? "y" : "x"));
        assertEquals("Duplicate new key: x - produced by both 'a' and 'c'", first.getMessage());

        // multiple nulls are a certain duplicate too
        final IllegalStateException nulls = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> null));
        assertEquals("Duplicate new key: null - produced by both 'a' and 'b'", nulls.getMessage());
        assertTrue(nulls.getCause() instanceof UnsupportedOperationException);

        // equals-only collision (distinct instances): UOE with the probable duplicate suppressed
        final UnsupportedOperationException equalsOnly = assertThrows(UnsupportedOperationException.class,
                () -> Maps.replaceKeys(map, k -> new String("same")));
        assertEquals(1, equalsOnly.getSuppressed().length);
        assertTrue(equalsOnly.getSuppressed()[0].getMessage().startsWith("Duplicate new key: same - produced by both 'a' and 'b'"),
                equalsOnly.getSuppressed()[0].getMessage());

        // no collision at all: the plain UOE, nothing suppressed
        final UnsupportedOperationException plain = assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(map, k -> k + "!"));
        assertEquals(0, plain.getSuppressed().length);

        // an ImmutableMap is unclearable in the same way
        final Map<String, Integer> immutable = ImmutableMap.copyOf(ordered("a", "b"));
        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(immutable, k -> "same"));
        assertEquals(0, assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(immutable, k -> k + "!")).getSuppressed().length);

        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));
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
    public void testC372_synchronizedCaseInsensitiveMergeIsApplied() {
        final Map<String, Integer> map = syncCaseInsensitive("a", 1, "b", 2);

        Maps.replaceKeys(map, k -> k.equals("a") ? "X" : "x", Integer::sum);

        assertEquals(1, map.size());
        assertEquals(3, map.get("X"));
        assertEquals(3, map.get("x"));
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
    public void testC372_synchronizedMergeNullResultRule() {
        final Map<String, Integer> map = Collections.synchronizedMap(new LinkedHashMap<>());
        map.put("a1", 1);
        map.put("a2", 2);
        map.put("a3", 3);

        Maps.replaceKeys(map, k -> "a", (existing, incoming) -> existing == 1 ? null : existing + incoming);

        assertEquals(Collections.singletonMap("a", 3), map);
    }

    // U16-04: an unclearable map reports the UOE unless the duplicate is certain under every equivalence
    @Test
    public void testU1604_unmodifiableIdentityMapReportsUnsupportedWithTheProbableDuplicateSuppressed() {
        final IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        identity.put("p", 1);
        identity.put("q", 2);
        final Map<String, Integer> map = Collections.unmodifiableMap(identity);

        // two distinct String instances: a duplicate under equals, not under the map's identity rule
        final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(map, k -> new String("K")));
        assertEquals(1, ex.getSuppressed().length);
        assertTrue(ex.getSuppressed()[0] instanceof IllegalStateException, ex.getSuppressed()[0].toString());
        final String probable = ex.getSuppressed()[0].getMessage();
        assertTrue(probable.startsWith("Duplicate new key: K - produced by both '"), probable);
        assertTrue(probable.contains("equals-keyed copy"), probable);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("p"));
        assertEquals(Integer.valueOf(2), map.get("q"));

        // control: the same converter on a modifiable identity map keeps both keys apart
        final Map<String, Integer> modifiable = Collections.synchronizedMap(new IdentityHashMap<>(identity));
        Maps.replaceKeys(modifiable, k -> new String("K"));
        assertEquals(2, modifiable.size());
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
}
