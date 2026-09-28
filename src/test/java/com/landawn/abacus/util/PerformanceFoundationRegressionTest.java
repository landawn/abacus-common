package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("unit")
class PerformanceFoundationRegressionTest {
    @Test
    void closedThrowingWrappersReleaseCapturedReferences() throws Exception {
        final AtomicInteger closes = new AtomicInteger();
        final Throwables.Iterator<Integer, RuntimeException> source = new Throwables.Iterator<>() {
            @Override public boolean hasNext() { return true; }
            @Override public Integer next() { return 1; }
            @Override protected void closeResourceInternal() { closes.incrementAndGet(); }
        };
        final Object payload = new Object();
        final Throwables.Iterator<Integer, RuntimeException> mapped = source.map(value -> { payload.hashCode(); return value + 1; });
        assertEquals(2, mapped.next());
        mapped.closeResource();
        mapped.closeResource();
        assertEquals(1, closes.get());
        assertFalse(mapped.hasNext());
        assertThrows(NoSuchElementException.class, mapped::next);
        assertReferenceFieldsCleared(mapped);

        final Throwables.Iterator<Integer, RuntimeException> filtered = Throwables.Iterator.<Integer, RuntimeException>of(1, 2, 3)
                .filter(value -> { payload.hashCode(); return value == 2; });
        assertTrue(filtered.hasNext()); // A cached element must be released as well.
        filtered.closeResource();
        assertFalse(filtered.hasNext());
        assertReferenceFieldsCleared(filtered);

        final AtomicInteger supplies = new AtomicInteger();
        final Throwables.Iterator<Integer, RuntimeException> deferred = Throwables.Iterator.defer(() -> {
            supplies.incrementAndGet();
            payload.hashCode();
            return Throwables.Iterator.just(7);
        });
        deferred.closeResource();
        assertFalse(deferred.hasNext());
        assertEquals(0, supplies.get());
        assertReferenceFieldsCleared(deferred);

        final Throwables.Iterator<Integer, RuntimeException> initialized = Throwables.Iterator.defer(() -> Throwables.Iterator.just(7));
        assertEquals(7, initialized.next());
        initialized.closeResource();
        assertReferenceFieldsCleared(initialized);
    }

    @Test
    void throwingWrapperReentrantCloseAndFailedCloseStillReleaseReferences() throws Exception {
        final List<Throwables.Iterator<Integer, RuntimeException>> holder = new ArrayList<>();
        final Throwables.Iterator<Integer, RuntimeException> filtered = Throwables.Iterator.<Integer, RuntimeException>of(1)
                .filter(value -> { holder.get(0).closeResource(); return true; });
        holder.add(filtered);
        assertFalse(filtered.hasNext());
        assertReferenceFieldsCleared(filtered);

        final RuntimeException failure = new RuntimeException("close");
        final Throwables.Iterator<Integer, RuntimeException> mapped = new Throwables.Iterator<Integer, RuntimeException>() {
            @Override public boolean hasNext() { return true; }
            @Override public Integer next() { return 1; }
            @Override protected void closeResourceInternal() { throw failure; }
        }.map(value -> value);
        assertSame(failure, assertThrows(RuntimeException.class, mapped::closeResource));
        assertDoesNotThrow(mapped::closeResource);
        assertReferenceFieldsCleared(mapped);
    }

    private static void assertReferenceFieldsCleared(final Object wrapper) throws Exception {
        for (final Field field : wrapper.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                field.setAccessible(true);
                assertNull(field.get(wrapper), field.getName());
            }
        }
    }

    @Test
    void conversionGraphRetainsCycleDetectionAndAcceptsSharedScalarSubgraphs() {
        final List<String> shared = Arrays.asList("1", "2", null);
        assertEquals(Arrays.asList(Arrays.asList(1, 2, null), Arrays.asList(1, 2, null)),
                N.convert(List.of(shared, shared), new TypeReference<List<List<Integer>>>() { }.type()));
        final List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        assertThrows(IllegalArgumentException.class, () -> N.convert(cycle, new TypeReference<List<String>>() { }.type()));
        final Map<String, Object> mapCycle = new HashMap<>();
        mapCycle.put("self", mapCycle);
        assertThrows(IllegalArgumentException.class, () -> N.convert(mapCycle, new TypeReference<Map<String, String>>() { }.type()));
        final Object[] arrayCycle = new Object[1];
        arrayCycle[0] = arrayCycle;
        assertThrows(IllegalArgumentException.class, () -> N.convert(arrayCycle, new TypeReference<List<String>>() { }.type()));
    }

    private static final class CountedKey {
        private static int comparisons;
        private final int value;
        private CountedKey(final int value) { this.value = value; }
        @Override public int hashCode() { return value; }
        @Override public boolean equals(final Object other) { comparisons++; return other instanceof CountedKey key && value == key.value; }
    }

    @Test
    void disjointUsesLinearHashWorkForOrdinaryListsButKeepsCustomMembership() {
        final List<CountedKey> left = new ArrayList<>(), right = new LinkedList<>();
        for (int i = 0; i < 1000; i++) { left.add(new CountedKey(i)); right.add(new CountedKey(i + 1000)); }
        CountedKey.comparisons = 0;
        assertTrue(N.disjoint(left, right));
        assertTrue(CountedKey.comparisons < 10000, "ordinary well-hashed Lists must not perform a million equality checks");
        right.add(new CountedKey(0));
        assertFalse(N.disjoint(left, right));

        final List<Integer> custom = new ArrayList<>(Collections.nCopies(30, 1)) {
            @Override public boolean contains(final Object ignored) { return true; }
        };
        assertFalse(N.disjoint(custom, new ArrayList<>(Collections.nCopies(20, 2))));
        final Set<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.add("VALUE");
        assertFalse(N.disjoint(sorted, List.of("value")));
        final Set<String> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        identity.add(new String("value"));
        assertTrue(N.disjoint(identity, List.of(new String("value"))));
        final List<Object> nulls = new ArrayList<>(Collections.nCopies(20, null));
        assertFalse(N.disjoint(nulls, new LinkedList<>(nulls)));
    }

    @Test
    void duplicateAndDistinctCapacityChangesKeepKeysOrderAndSupplierHints() {
        assertTrue(N.containsDuplicates(new int[100000]));
        assertTrue(N.containsDuplicates(new long[100000]));
        assertTrue(N.containsDuplicates(new float[] { Float.NaN, 1, 2, Float.intBitsToFloat(0x7fc00001) }));
        assertFalse(N.containsDuplicates(new double[] { -0.0, 0.0, 1, 2 }));
        assertTrue(N.containsDuplicates(new Object[] { new int[] { 1 }, 2, 3, new int[] { 1 } }));
        assertTrue(N.containsDuplicates(Arrays.asList(null, 1, 2, null)));
        final String[] repeated = new String[10000];
        Arrays.fill(repeated, "same");
        assertEquals(List.of("same"), N.distinct(repeated));
        assertEquals(List.of("same"), N.distinct(Arrays.asList(repeated)));
        assertEquals(List.of("a", "bb", "ccc"), N.distinctBy(Arrays.asList("a", "b", "bb", "cc", "ccc"), String::length));
        final AtomicInteger capacity = new AtomicInteger();
        final AtomicInteger mappings = new AtomicInteger();
        assertEquals(List.of("same"), N.distinctBy(repeated, value -> { mappings.incrementAndGet(); return value; }, size -> {
            capacity.set(size);
            return new ArrayList<String>();
        }));
        assertEquals(repeated.length, capacity.get());
        assertEquals(repeated.length, mappings.get());
        N.distinctBy(Arrays.asList(repeated), value -> value, size -> { capacity.set(size); return new ArrayList<String>(); });
        assertEquals(repeated.length, capacity.get());
    }

    @Test
    void subcollectionMatchesMultiplicityReferenceAcrossAsymmetricInputs() {
        final Random random = new Random(606);
        for (int trial = 0; trial < 500; trial++) {
            final List<Integer> subset = randomValues(random, random.nextInt(25));
            final List<Integer> superset = randomValues(random, random.nextInt(80));
            final List<Integer> remaining = new ArrayList<>(superset);
            boolean expected = true;
            for (final Integer value : subset) { if (!remaining.remove(value)) { expected = false; break; } }
            assertEquals(expected, N.isSubCollection(subset, superset));
        }
        assertFalse(N.isSubCollection(List.of(new int[] { 1 }), List.of(new int[] { 1 })));
        assertTrue(N.isSubCollection(List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(null, List.of()));
    }

    private static List<Integer> randomValues(final Random random, final int count) {
        final List<Integer> values = new ArrayList<>();
        for (int i = 0; i < count; i++) { final int value = random.nextInt(12); values.add(value == 0 ? null : value); }
        return values;
    }

    @Test
    void boundedPrimitiveMultisetsPreserveCountsAndSignedEndpoints() {
        assertTrue(N.containsSameElements(new boolean[] { true, false, true }, new boolean[] { true, true, false }));
        assertFalse(N.containsSameElements(new boolean[] { true, false, true }, new boolean[] { false, true, false }));
        assertTrue(N.containsSameElements((boolean[]) null, new boolean[0]));
        assertFalse(N.containsSameElements((boolean[]) null, new boolean[] { false }));
        assertTrue(N.containsSameElements(new byte[] { -128, 127, 0, -128 }, new byte[] { 127, -128, -128, 0 }));
        assertFalse(N.containsSameElements(new byte[] { -128, 127 }, new byte[] { -128, -128 }));
        assertTrue(N.containsSameElements(new short[] { Short.MIN_VALUE, Short.MAX_VALUE, 0 }, new short[] { 0, Short.MAX_VALUE, Short.MIN_VALUE }));
        assertTrue(N.containsSameElements(new char[] { 0, 65535, 1 }, new char[] { 65535, 1, 0 }));
    }

    @Test
    void linkedListSteppedSlicesOnlyTraverseTheWindowAndHandleExtremeSteps() {
        final LinkedList<Integer> source = new LinkedList<>() {
            @Override public Object[] toArray() { throw new AssertionError("whole-list snapshot"); }
            @Override public <T> T[] toArray(final T[] output) { throw new AssertionError("whole-list snapshot"); }
        };
        for (int i = 0; i < 101; i++) { source.add(i); }
        assertEquals(List.of(0, 2, 4, 6, 8), N.copyOfRange(source, 0, 10, 2));
        assertEquals(List.of(99, 96, 93, 90), N.copyOfRange(source, 99, 89, -3));
        assertEquals(List.of(9, 6, 3, 0), N.copyOfRange(source, 9, -1, -3));
        assertEquals(List.of(100), N.copyOfRange(source, 100, -1, Integer.MIN_VALUE));
        assertEquals(List.of(0), N.copyOfRange(source, 0, 101, Integer.MAX_VALUE));
        assertEquals(List.of(), N.copyOfRange(source, 5, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.copyOfRange(source, 101, -1, -2));
        final List<Integer> copy = N.copyOfRange(source, 0, 4, 2);
        copy.set(0, -1);
        assertEquals(0, source.getFirst());
    }

    @Test
    void joinerRetainsSnapshotsOnlyWhileItsBuilderIsRecycled() throws Exception {
        final Field snapshot = Joiner.class.getDeclaredField("latestToStringValue");
        snapshot.setAccessible(true);
        final Joiner ordinary = Joiner.with(",", "[", "]").append("a");
        assertEquals("[a]", ordinary.toString());
        assertNull(snapshot.get(ordinary));
        ordinary.append("b");
        assertEquals("[a,b]", ordinary.toString());
        ordinary.close();
        assertEquals("[a,b]", ordinary.toString());

        final Joiner pooled = Joiner.with(",", "[", "]").reuseBuffer().append("a");
        assertEquals("[a]", pooled.toString());
        assertEquals("[a", snapshot.get(pooled));
        assertEquals("[a]", pooled.toString());
        pooled.append("b");
        assertNull(snapshot.get(pooled));
        assertEquals(5, pooled.length());
        assertEquals("[a,b]", pooled.toString());
        pooled.close();
        assertEquals("[a,b]", pooled.toString());
        assertThrows(IllegalStateException.class, () -> pooled.append("c"));
        final Joiner closedPooled = Joiner.with(",", "[", "]").reuseBuffer().append("a");
        closedPooled.close();
        assertEquals("[a]", closedPooled.toString());
        assertEquals("[a", snapshot.get(closedPooled));
    }

    @Test
    void joinerSelfMergePreservesRecycledSnapshotsAndEmptyElements() {
        for (final String separator : new String[] { ",", "" }) {
            for (final String element : new String[] { "value", "" }) {
                for (int mode = 0; mode < 3; mode++) {
                    try (Joiner joiner = Joiner.with(separator, "[", "]")) {
                        final java.util.StringJoiner reference = new java.util.StringJoiner(separator, "[", "]").add(element);
                        if (mode > 0) {
                            joiner.reuseBuffer();
                        }
                        joiner.append(element);
                        assertEquals(reference.toString(), joiner.toString());
                        // Mode 1 resumes with an ordinary builder; mode 2 recycles the next builder too.
                        if (mode == 2) {
                            joiner.reuseBuffer();
                        }
                        assertSame(joiner, joiner.merge(joiner));
                        reference.merge(reference);
                        assertEquals(reference.toString(), joiner.toString());
                        joiner.append("tail");
                        reference.add("tail");
                        assertEquals(reference.toString(), joiner.toString());
                    }
                }
            }
        }
    }

    @Test
    void hexStringDecodeMatchesArrayValidationAndAllByteValues() {
        final byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) { bytes[i] = (byte) i; }
        final String text = Hex.encodeToString(bytes);
        assertArrayEquals(bytes, Hex.decode(text));
        assertArrayEquals(bytes, Hex.decode(text.toUpperCase(java.util.Locale.ROOT)));
        for (final String invalid : List.of("x", "0x", "g0", "00\uff10a", "00 0")) {
            assertEquals(assertThrows(IllegalArgumentException.class, () -> Hex.decode(invalid.toCharArray())).getMessage(),
                    assertThrows(IllegalArgumentException.class, () -> Hex.decode(invalid)).getMessage());
        }
        assertEquals("Data string cannot be null", assertThrows(IllegalArgumentException.class, () -> Hex.decode((String) null)).getMessage());
    }

    private static final class CountedSequence implements CharSequence {
        private final String value;
        private int reads;
        private CountedSequence(final String value) { this.value = value; }
        @Override public int length() { return value.length(); }
        @Override public char charAt(final int index) { reads++; return value.charAt(index); }
        @Override public CharSequence subSequence(final int start, final int end) { return value.subSequence(start, end); }
        @Override public String toString() { return value; }
    }

    @Test
    void longLiteralSplitterUsesLinearReadsAndKeepsConfigurationSemantics() {
        final String input = "a".repeat(8192);
        final CountedSequence sequence = new CountedSequence(input);
        assertEquals(List.of(input), Splitter.with("a".repeat(255) + "b").split(sequence));
        assertTrue(sequence.reads <= input.length() * 2, "repeated delimiter prefixes must not be rescanned at every character");
        final String delimiter = "::".repeat(8);
        assertEquals(List.of("x", "y"), Splitter.with(delimiter).trim(true).omitEmptyStrings(true).split(" x " + delimiter + " " + delimiter + " y " + delimiter));
        assertEquals(List.of("a", "b" + delimiter + "c"), Splitter.with(delimiter).limit(2).split("a" + delimiter + "b" + delimiter + "c"));
        final StringBuilder mutableDelimiter = new StringBuilder(delimiter);
        final Splitter splitter = Splitter.with(mutableDelimiter);
        mutableDelimiter.setCharAt(0, 'x');
        final StringBuilder mutableSource = new StringBuilder("first" + delimiter + "second" + delimiter + "third");
        try (var stream = splitter.splitToStream(mutableSource)) {
            final Iterator<String> iterator = stream.iterator();
            assertEquals("first", iterator.next());
            mutableSource.replace(5 + delimiter.length(), 11 + delimiter.length(), "SECOND");
            assertEquals("SECOND", iterator.next());
            assertEquals("third", iterator.next());
        }
    }

    @Test
    void longLiteralSplitterSkipsInputsAndTailsShorterThanItsDelimiter() {
        final String delimiter = "a".repeat(16383) + "b";
        final Splitter splitter = Splitter.with(delimiter);
        final String input = "a".repeat(8192);
        final CountedSequence shorterInput = new CountedSequence(input);
        assertEquals(List.of(input), splitter.split(shorterInput));
        assertEquals(0, shorterInput.reads, "an impossible match must not scan the input");

        final CountedSequence shortTail = new CountedSequence("head" + delimiter + "tail");
        assertEquals(List.of("head", "tail"), splitter.split(shortTail));
        assertEquals(4 + delimiter.length(), shortTail.reads, "an impossible match must not scan the remaining tail");
    }

    @Test
    void emptinessPredicatesDoNotRequestCollectionCardinality() throws Exception {
        for (final boolean empty : new boolean[] { false, true }) {
            final Collection<Integer> collection = new AbstractCollection<>() {
                @Override public int size() { throw new AssertionError("size must not be evaluated"); }
                @Override public boolean isEmpty() { return empty; }
                @Override public Iterator<Integer> iterator() { return (empty ? List.<Integer>of() : List.of(1)).iterator(); }
            };
            assertEquals(empty, Fn.isEmptyCollection().test(collection));
            assertEquals(!empty, Fn.notEmptyCollection().test(collection));
            assertEquals(empty, Fnn.isEmptyCollection().test(collection));
            assertEquals(!empty, Fnn.notEmptyCollection().test(collection));
        }
        assertTrue(Fn.isEmptyCollection().test(null));
        assertFalse(Fnn.notEmptyCollection().test(null));
    }

    @Test
    void offsetAbbreviationPreservesSurrogateBoundariesAndTailTransition() {
        assertEquals("...aaaa...", Strings.abbreviate("a".repeat(100000), "...", 50000, 10));
        final String text = "0123456789\ud83d\ude00abcdefghij";
        assertEquals("...abcd...", Strings.abbreviate(text, "...", 11, 10));
        assertEquals("...\ud83d\ude00ab...", Strings.abbreviate(text, "...", 10, 10));
        assertEquals("......", Strings.abbreviate(text, "...", 10, 7));
        assertEquals("...bbbbbbb", Strings.abbreviate("a".repeat(11) + "\ud83d\ude00" + "b".repeat(7), "...", 12, 10));
    }

    @Test
    void tokenIgnoreCaseSearchMatchesBoundaryOracleInBothDirections() {
        final Random random = new Random(1414);
        final String[] tokens = { "", "a", "a".repeat(32), "aA".repeat(16), "\u0130".repeat(16), "\ud801\udc00".repeat(8) };
        final String[] delimiters = { " ", ",", "aa", "aB", "\ud83d\ude00" };
        for (int trial = 0; trial < 250; trial++) {
            final String token = tokens[random.nextInt(tokens.length)];
            final String delimiter = delimiters[random.nextInt(delimiters.length)];
            final String text = "a".repeat(180) + delimiter + token.toLowerCase(java.util.Locale.ROOT) + delimiter + "a".repeat(120) + delimiter;
            for (final int bound : new int[] { -1, 0, 1, 179, 180, 210, text.length(), Integer.MAX_VALUE }) {
                assertEquals(tokenIndex(text, token, delimiter, bound, false), Strings.StrUtil.indexOfTokenIgnoreCase(text, token, delimiter, bound));
                assertEquals(tokenIndex(text, token, delimiter, bound, true), Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, token, delimiter, bound));
            }
        }
        assertEquals(-1, Strings.StrUtil.lastIndexOfToken("A".repeat(32), "a".repeat(32), ",", 100));
    }

    @Test
    void tokenSearchRetainsOverlappingMatchesAfterRejectedUnicodeBoundaries() {
        final String[] characters = { "a", "\u017f", "\u212a", "\u03c2", "\u0130" };
        final String[] equivalents = { "A", "s", "k", "\u03c3", "i" };
        for (int i = 0; i < characters.length; i++) {
            final String text = characters[i].repeat(160);
            final String token = equivalents[i].repeat(32);
            final String delimiter = equivalents[i].repeat(33);
            // Candidates at 1..32 have no full left delimiter; candidate 33 overlaps every rejected match.
            assertEquals(33, Strings.StrUtil.indexOfTokenIgnoreCase(text, token, delimiter, 1));
            // Candidates at 127..96 have no full right delimiter; candidate 95 must remain visible.
            assertEquals(95, Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, token, delimiter, 127));
            assertEquals(0, Strings.StrUtil.indexOfTokenIgnoreCase(text, token, delimiter, 0));
            assertEquals(128, Strings.StrUtil.lastIndexOfTokenIgnoreCase(text, token, delimiter, Integer.MAX_VALUE));
        }
    }

    private static int tokenIndex(final String text, final String token, final String delimiter, final int bound, final boolean reverse) {
        if (reverse && bound < 0) { return -1; }
        for (int i = 0; i <= token.length() - delimiter.length(); i++) {
            if (token.regionMatches(true, i, delimiter, 0, delimiter.length())) { return -1; }
        }
        final int from = reverse ? Math.min(bound, text.length() - token.length())
                : token.isEmpty() ? Math.min(Math.max(bound, 0), text.length()) : Math.max(bound, 0);
        for (int i = from; i >= 0 && i <= text.length() - token.length(); i += reverse ? -1 : 1) {
            if (text.regionMatches(true, i, token, 0, token.length())
                    && (i == 0 || i >= delimiter.length() && text.regionMatches(true, i - delimiter.length(), delimiter, 0, delimiter.length()))
                    && (i + token.length() == text.length() || text.regionMatches(true, i + token.length(), delimiter, 0, delimiter.length()))) {
                return i;
            }
        }
        return -1;
    }
}
