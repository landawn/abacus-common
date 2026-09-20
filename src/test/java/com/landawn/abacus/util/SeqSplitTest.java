package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors;

public class SeqSplitTest extends SeqTestSupport {

    @Test
    public void testSplit_CharSequence() throws Exception {
        assertEquals(Arrays.asList("a", "b", "c"), Seq.split("a,b,c", ',').toList());
        assertEquals(Collections.singletonList(""), Seq.split("", ',').toList());
        assertEquals(Arrays.asList("a", "b", "c"), Seq.split("a::b::c", "::").toList());
        assertEquals(Arrays.asList("a", "b", "c"), Seq.split("a1b2c", Pattern.compile("\\d")).toList());
        assertEquals(Arrays.asList("a", "b", "c", "d"), Seq.split("a123b456c789d", Pattern.compile("\\d+")).toList());
        assertEquals(Arrays.asList("a", "b", "c", "d"), Seq.split("a.b.c.d", Pattern.compile("\\.")).toList());
    }

    @Test
    public void testSplitToLines() throws Exception {
        assertEquals(Arrays.asList("line1", "line2", "line3"), Seq.splitToLines("line1\nline2\r\nline3").toList());
        assertEquals(Arrays.asList("line1", "line2"), Seq.splitToLines("  line1  \n\n  line2  \n", true, true).toList());
        assertEquals(Arrays.asList("line1", "", "line2", "", "line3"), Seq.splitToLines("  line1  \n\n  line2  \n  \n  line3  ", true, false).toList());
        assertEquals(Arrays.asList("  line1  ", "  line2  ", "  ", "  line3  "),
                Seq.splitToLines("  line1  \n\n  line2  \n  \n  line3  ", false, true).toList());
        assertEquals(Arrays.asList("line1", "", "line2", ""), Seq.splitToLines("line1\n\nline2\n", false, false).toList());
    }

    @Test
    public void testSplitByChunkCount() throws Exception {
        assertEquals(Arrays.asList(Pair.of(0, 3), Pair.of(3, 5), Pair.of(5, 7)), Seq.splitByChunkCount(7, 3, Pair::of).toList());
        assertEquals(Arrays.asList(Pair.of(0, 2), Pair.of(2, 4), Pair.of(4, 7)), Seq.splitByChunkCount(7, 3, true, Pair::of).toList());
        assertEquals(Arrays.asList(Pair.of(0, 1), Pair.of(1, 2)), Seq.splitByChunkCount(2, 5, Pair::of).toList());
        assertTrue(Seq.splitByChunkCount(0, 3, Pair::of).toList().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(-1, 3, Pair::of));
        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(5, 0, Pair::of));
        assertEquals(Arrays.asList("0-2", "2-4", "4-6"), Seq.splitByChunkCount(6, 3, true, (from, to) -> from + "-" + to).toList());
        assertEquals("0-3", Seq.splitByChunkCount(7, 3, false, (from, to) -> from + "-" + to).toList().get(0));
    }

    @Test
    public void testSplit() throws Exception {
        List<List<Integer>> chunks = Seq.of(1, 2, 3, 4, 5, 6, 7).split(3).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6), Arrays.asList(7)), chunks);
        assertTrue(Seq.<Integer, Exception> empty().split(3).toList().isEmpty());
        assertEquals(Arrays.asList(Arrays.asList(1, 2)), Seq.of(1, 2).split(5).toList());
        assertEquals(Arrays.asList("1-2-3", "4-5-6", "7"),
                Seq.of(1, 2, 3, 4, 5, 6, 7).split(3, Collectors.mapping(String::valueOf, Collectors.joining("-"))).toList());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), Seq.of("a", "b", "c", "d", "e").split(2, IntFunctions.ofSet()).toList().get(0));
        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), Seq.of(1, 2, 3, 4, 5, 6).split(2, Collectors.joining(",")).toList());
        assertEquals(Arrays.asList(Arrays.asList("d", "e")), Seq.of("a", "b", "c", "d", "e").split(3).skip(1).toList());
    }

    @Test
    public void testSplit_Predicate() throws Exception {
        List<List<Integer>> chunks = Seq.of(1, 2, 3, 0, 4, 5, 0, 6).split(x -> x == 0).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(0), Arrays.asList(4, 5), Arrays.asList(0), Arrays.asList(6)), chunks);
        assertEquals(Arrays.asList("3", "21", "11", "20"),
                Seq.of(1, 2, 10, 11, 5, 6, 20).split(x -> x < 10, Collectors.summingInt(x -> x)).map(String::valueOf).toList());
        assertEquals(new HashSet<>(Arrays.asList("a")), Seq.of("a", "ab", "abc", "d", "de").split(s -> s.length() > 1, Suppliers.ofSet()).toList().get(0));
        assertEquals(Arrays.asList("1,3,5", "2,4,6", "7,9"), Seq.of(1, 3, 5, 2, 4, 6, 7, 9).split(n -> n % 2 == 0, Collectors.joining(",")).toList());
        List<Optional<Integer>> maxValues = Seq.of(1, 3, 2, 8, 6, 4, 5, 7).split(n -> n > 4, Collectors.max()).toList();
        assertEquals(3, maxValues.get(0).get().intValue());
        assertEquals(8, maxValues.get(1).get().intValue());
    }

    @Test
    public void testSplitAt() throws Exception {
        List<Seq<Integer, Exception>> split = Seq.of(1, 2, 3, 4, 5).splitAt(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertEquals(Arrays.asList(4, 5), split.get(1).toList());
        assertTrue(Seq.of(1, 2, 3).splitAt(0).toList().get(0).toList().isEmpty());
        assertTrue(Seq.of(1, 2).splitAt(5).toList().get(1).toList().isEmpty());
        assertTrue(Seq.<Integer, Exception> empty().splitAt(2).toList().get(0).toList().isEmpty());

        List<Seq<Integer, Exception>> byPred = Seq.of(1, 2, 3, 4, 5).splitAt(n -> n > 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), byPred.get(0).toList());
        assertEquals(Arrays.asList(4, 5), byPred.get(1).toList());
        assertTrue(Seq.of(5, 1, 2, 3).splitAt(n -> n > 3).toList().get(0).toList().isEmpty());

        Seq<String, RuntimeException>[] parts = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).toArray(Seq[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, parts[0].toArray(String[]::new));
        assertArrayEquals(new String[] { "d", "e" }, parts[1].toArray(String[]::new));
    }

    @Test
    public void testSplitAt_LiveViewAndExhaustedOuter() throws Exception {
        final File f = tempDir.resolve("splitAt-live-view.txt").toFile();
        IOUtil.writeLines(CommonUtil.asList("l1", "l2", "l3", "l4", "l5"), f);
        final List<Seq<String, IOException>> parts = Seq.ofLines(f).splitAt(2).toList();
        assertEquals(CommonUtil.asList("l1", "l2"), parts.get(0).toList());
        assertTrue(parts.get(1).toList().isEmpty());
        final List<List<String>> collected = new ArrayList<>();
        Seq.ofLines(f).splitAt(2).forEach(part -> collected.add(part.toList()));
        assertEquals(CommonUtil.asList(CommonUtil.asList("l1", "l2"), CommonUtil.asList("l3", "l4", "l5")), collected);

        for (final List<Integer> input : Arrays.asList(Collections.<Integer> emptyList(), Arrays.asList(1, 2, 3, 4))) {
            for (final int position : new int[] { 0, 2, 6 }) {
                try (Seq<Integer, RuntimeException> source = Seq.of(input);
                     Seq<Seq<Integer, RuntimeException>, RuntimeException> split = source.splitAt(position)) {
                    final Throwables.Iterator<Seq<Integer, RuntimeException>, RuntimeException> outer = split.iteratorEx();
                    try (Seq<Integer, RuntimeException> first = outer.next();
                         Seq<Integer, RuntimeException> second = outer.next()) {
                        assertEquals(0, outer.count());
                        assertThrows(NoSuchElementException.class, outer::next);
                        final int splitIndex = Math.min(position, input.size());
                        assertEquals(input.subList(0, splitIndex), first.toList());
                        assertEquals(input.subList(splitIndex, input.size()), second.toList());
                    }
                }
            }
        }
    }

    @Test
    public void testSplitAt_FileLifetimeAndBufferedPredicateBoundary() throws Exception {
        final File file = tempDir.resolve("splitAt-lifetime.txt").toFile();
        for (final List<String> input : Arrays.asList(Collections.<String> emptyList(), Arrays.asList("", "\u00e9", "\ud83d\ude42", "last"))) {
            IOUtil.writeLines(input, file);
            for (final int position : new int[] { 0, 2, 6 }) {
                final List<Seq<String, IOException>> parts = Seq.ofLines(file).splitAt(position).toList();
                assertEquals(input.subList(0, Math.min(position, input.size())), parts.get(0).toList());
                assertTrue(parts.get(1).toList().isEmpty());

                final List<String> inPlace = new ArrayList<>();
                Seq.ofLines(file).splitAt(position).forEach(part -> inPlace.addAll(part.toList()));
                assertEquals(input, inPlace);
            }

            final List<Seq<String, IOException>> parts = Seq.ofLines(file).splitAt("\u00e9"::equals).toList();
            assertEquals(input.isEmpty() ? Collections.emptyList() : Collections.singletonList(""), parts.get(0).toList());
            // The predicate has already read its matching boundary, but closure exhausts the remaining file iterator.
            assertEquals(input.isEmpty() ? Collections.emptyList() : Collections.singletonList("\u00e9"), parts.get(1).toList());
            final List<String> inPlace = new ArrayList<>();
            Seq.ofLines(file).splitAt("\u00e9"::equals).forEach(part -> inPlace.addAll(part.toList()));
            assertEquals(input, inPlace);
        }
    }
}
