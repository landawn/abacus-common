package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserFactory;

/** Retention, representation and callback regressions from the October performance review. */
@Isolated
@Tag("unit")
class UtilityPerformanceReview20261004Test extends TestBase {
    private static Object field(final Object owner, final String name) throws Exception {
        final Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void assertCleared(final Object owner, final String name) throws Exception {
        final Object tuple = field(owner, name);
        if (tuple instanceof Pair<?, ?> pair) {
            assertNull(pair.left());
            assertNull(pair.right());
        } else {
            final Triple<?, ?, ?> triple = (Triple<?, ?, ?>) tuple;
            assertNull(triple.left());
            assertNull(triple.middle());
            assertNull(triple.right());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void recyclingListBoundsItsBackingCapacity() throws Exception {
        final Field backing = ArrayList.class.getDeclaredField("elementData");
        Assumptions.assumeTrue(backing.trySetAccessible(), "capacity probe requires --add-opens java.base/java.util=ALL-UNNAMED");
        final Field poolField = Objectory.class.getDeclaredField("listPool");
        poolField.setAccessible(true);
        final Queue<List<?>> pool = (Queue<List<?>>) poolField.get(null);
        final List<List<?>> saved = new ArrayList<>(pool);
        pool.clear();
        try {
            // An emptied (or preallocated) list releases its backing array when recycled.
            final ArrayList<Object> list = new ArrayList<>(1_000_000);
            list.add(new Object());
            list.clear();
            Objectory.recycle(list);
            final List<?> reused = Objectory.createList();
            assertSame(list, reused);
            assertTrue(reused.isEmpty());
            assertEquals(0, ((Object[]) backing.get(reused)).length);
            Objectory.recycle(reused);
            assertEquals(0, ((Object[]) backing.get(Objectory.createList())).length);
            // Code review 2026-10-05: a non-empty list is cleared but keeps its capacity, so recycling never copies it.
            final ArrayList<Object> nonEmpty = new ArrayList<>(64);
            nonEmpty.add(new Object());
            Objectory.recycle(nonEmpty);
            final List<?> reusedNonEmpty = Objectory.createList();
            assertSame(nonEmpty, reusedNonEmpty);
            assertTrue(reusedNonEmpty.isEmpty());
            assertEquals(64, ((Object[]) backing.get(reusedNonEmpty)).length);
        } finally {
            pool.clear();
            pool.addAll(saved);
        }
    }

    @Test
    void hashCollectionsAreClearedWithoutRetainingTheirUnobservableCapacity() {
        final HashMap<String, Integer> map = new HashMap<>(1_000_000);
        map.put("value", 1);
        Objectory.recycle(map);
        assertTrue(map.isEmpty());
        assertNotSame(map, Objectory.createMap());
        final LinkedHashMap<String, Integer> linkedMap = new LinkedHashMap<>();
        linkedMap.put("value", 1);
        Objectory.recycle(linkedMap);
        assertTrue(linkedMap.isEmpty());
        assertNotSame(linkedMap, Objectory.createLinkedHashMap());
        final HashSet<Integer> set = new HashSet<>(1_000_000);
        set.add(1);
        Objectory.recycle(set);
        assertTrue(set.isEmpty());
        assertNotSame(set, Objectory.createSet());
        final LinkedHashSet<Integer> linkedSet = new LinkedHashSet<>();
        linkedSet.add(1);
        Objectory.recycle(linkedSet);
        assertTrue(linkedSet.isEmpty());
        assertNotSame(linkedSet, Objectory.createLinkedHashSet());
    }

    @Test
    void trieLeavesShareEmptyChildrenWithoutBreakingSnapshots() throws Exception {
        final var builder = PrefixSearchTable.<Integer, String> builder().add(List.of(1), "one").add(List.of(2), "two");
        final var first = builder.build();
        final Map<?, ?> nodes = (Map<?, ?>) field(first, "nodes");
        assertSame(field(nodes.get(1), "children"), field(nodes.get(2), "children"));
        builder.add(List.of(1, 3), "child");
        assertEquals("one", first.get(List.of(1, 3)).get());
        assertEquals("child", builder.build().get(List.of(1, 3)).get());
    }

    @Test
    void asciiCsvTablesPreserveEveryUtf16CodeUnitInEveryWritePath() throws Exception {
        final char[] input = new char[Character.MAX_VALUE + 1];
        for (int i = 0; i < input.length; i++) {
            input[i] = (char) i;
        }
        final boolean oldBackslashMode = CsvUtil.isBackSlashEscapeCharForWrite();
        try {
            for (final char[][] table : new char[][][] { BufferedCsvWriter.REPLACEMENT_CHARS, BufferedCsvWriter.REPLACEMENT_CHARS_BACK_SLASH }) {
                if (table == BufferedCsvWriter.REPLACEMENT_CHARS_BACK_SLASH) {
                    CsvUtil.setEscapeCharToBackSlashForWrite();
                } else {
                    CsvUtil.resetEscapeCharForWrite();
                }
                assertEquals(128, table.length);
                final StringBuilder expected = new StringBuilder();
                for (final char c : input) {
                    if (c == '"') {
                        expected.append(table == BufferedCsvWriter.REPLACEMENT_CHARS ? "\"\"" : "\\\"");
                    } else if (c == '\\' && table == BufferedCsvWriter.REPLACEMENT_CHARS_BACK_SLASH) {
                        expected.append("\\\\");
                    } else {
                        expected.append(c);
                    }
                }
                final CharacterWriter chars = new BufferedCsvWriter();
                chars.writeCharacter(input);
                assertEquals(expected.toString(), chars.toString());
                final CharacterWriter string = new BufferedCsvWriter();
                string.writeCharacter(new String(input));
                assertEquals(expected.toString(), string.toString());
                final CharacterWriter single = new BufferedCsvWriter();
                for (final char c : input) {
                    single.writeCharacter(c);
                }
                assertEquals(expected.toString(), single.toString());
            }
        } finally {
            if (oldBackslashMode) {
                CsvUtil.setEscapeCharToBackSlashForWrite();
            } else {
                CsvUtil.resetEscapeCharForWrite();
            }
        }
    }

    @Test
    void directoryIteratorReleasesConsumedSlotsWhileKeepingItsNextEntry() throws Exception {
        final File[] entries = { new File("review-file-a"), new File("review-file-b"), new File("review-file-c") };
        final File first = entries[0];
        final File second = entries[1];
        final File root = new File("review-root") {
            @Override
            public File[] listFiles() {
                return entries;
            }
        };
        final var constructor = Class.forName("com.landawn.abacus.util.IOUtil$DepthFirstFileIterator")
                .getDeclaredConstructor(File.class, boolean.class, boolean.class);
        constructor.setAccessible(true);
        final Iterator<?> iterator = (Iterator<?>) constructor.newInstance(root, false, false);
        assertSame(first, iterator.next());
        assertNull(entries[0]);
        assertTrue(iterator.hasNext());
        assertNull(entries[1]);
        assertSame(second, iterator.next());
        assertEquals(1, ((Deque<?>) field(iterator, "stack")).size());
    }

    @Test
    void xmlArraySuffixScanPreservesAllowlistAndGenericChecks() throws Exception {
        final var utilCheck = XmlUtil.class.getDeclaredMethod("isSafeXmlTypeAttributeName", String.class);
        utilCheck.setAccessible(true);
        final var parserCheck = Class.forName("com.landawn.abacus.parser.AbstractXmlParser").getDeclaredMethod("isAllowedXmlTypeAttributeName", String.class);
        parserCheck.setAccessible(true);
        final Object parser = ParserFactory.createXmlParser();
        for (final String valid : List.of("String", "int[][]", "List<String[]>[]", "String" + "[]".repeat(20_000))) {
            assertEquals(true, utilCheck.invoke(null, valid), valid.substring(0, Math.min(valid.length(), 64)));
            assertEquals(true, parserCheck.invoke(parser, valid));
        }
        for (final String invalid : List.of("Unapproved[]", "List<Unapproved[]>[]", "String[ ]", "String[]garbage")) {
            assertEquals(false, utilCheck.invoke(null, invalid));
            assertEquals(false, parserCheck.invoke(parser, invalid));
        }
    }

    @Test
    void cancellationReleasesQueuedAndPrefetchedOutcomesWithoutCancellingInputs() throws Exception {
        final var pending = new CompletableFuture<>();
        final var iterator = Futures.iterate(List.of(CompletableFuture.completedFuture(new byte[1024]), pending), result -> result);
        assertTrue(iterator.hasNext());
        Futures.cancelPendingRelays(iterator);
        assertFalse(iterator.hasNext());
        assertNull(field(iterator, "nextResult"));
        assertFalse(pending.isCancelled());
        pending.complete("late");
        assertFalse(iterator.hasNext());
    }

    @Test
    void liveIteratorKeepsCompletionStateAndClearsConsumedResultBeforeReentrantHandler() throws Exception {
        final var pending = new CompletableFuture<Integer>();
        final var first = CompletableFuture.completedFuture(1);
        final Object[] holder = new Object[1];
        final var iterator = Futures.iterate(List.of(first, pending), result -> {
            if (result.orElseIfFailure(-1) == 1) {
                pending.complete(2);
                assertTrue(((Iterator<?>) holder[0]).hasNext());
            }
            return result.orElseIfFailure(-1);
        });
        holder[0] = iterator;
        assertEquals(1, iterator.next());
        assertEquals(2, iterator.next());
        assertNull(field(iterator, "nextResult"));
        assertFalse(iterator.hasNext());
    }

    @Test
    void timedIteratorStillYieldsOneTimeoutAfterCleanup() {
        final var pending = new CompletableFuture<Integer>();
        final var iterator = Futures.iterate(List.of(pending), 1, TimeUnit.MILLISECONDS, result -> result);
        assertTrue(iterator.next().getException() instanceof java.util.concurrent.TimeoutException);
        assertFalse(iterator.hasNext());
        assertFalse(pending.isCancelled());
    }

    @Test
    void biAndTriAdaptersClearConsumedRejectedAndExceptionalInputs() throws Exception {
        final var bi = BiIterator.of(Map.of(new byte[1024], 1)).filter((a, b) -> true);
        final var biMap = bi.map((a, b) -> a.length);
        assertEquals(1024, biMap.next());
        assertCleared(bi, "next");
        assertCleared(biMap, "tmp");
        final var rejectedBi = BiIterator.of(Map.of(new byte[1024], 1)).filter((a, b) -> false);
        assertFalse(rejectedBi.hasNext());
        assertCleared(rejectedBi, "next");
        final var failedBi = BiIterator.of(Map.of(new byte[1024], 1)).filter((a, b) -> {
            throw new IllegalStateException();
        });
        assertThrows(IllegalStateException.class, failedBi::hasNext);
        assertCleared(failedBi, "next");
        final var tri = TriIterator.zip(List.of(new byte[1024]), List.of(1), List.of(2)).filter((a, b, c) -> true);
        final var triMap = tri.map((a, b, c) -> a.length);
        assertEquals(1024, triMap.next());
        assertCleared(tri, "next");
        assertCleared(triMap, "tmp");
        final var rejectedTri = TriIterator.zip(List.of(new byte[1024]), List.of(1), List.of(2)).filter((a, b, c) -> false);
        assertFalse(rejectedTri.hasNext());
        assertCleared(rejectedTri, "next");
    }

    @Test
    void tupleCallbacksKeepReentrantlyPrefetchedValuesEvenWhenTheyThrow() throws Exception {
        final var bi = BiIterator.zip(List.of(1, 2), List.of("a", "b")).filter((a, b) -> true);
        assertThrows(IllegalStateException.class, () -> bi.next((a, b) -> {
            assertEquals(1, a);
            assertTrue(bi.hasNext());
            throw new IllegalStateException();
        }));
        assertEquals(Pair.of(2, "b"), bi.next());
        final var tri = TriIterator.zip(List.of(1, 2), List.of("a", "b"), List.of(true, false)).filter((a, b, c) -> true);
        assertThrows(IllegalStateException.class, () -> tri.next((a, b, c) -> {
            assertEquals(1, a);
            assertTrue(tri.hasNext());
            throw new IllegalStateException();
        }));
        assertEquals(Triple.of(2, "b", false), tri.next());
    }
}
