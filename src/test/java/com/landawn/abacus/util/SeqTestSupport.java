package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.AbstractTest;

public abstract class SeqTestSupport extends AbstractTest {

    protected Throwables.Iterator<Integer, RuntimeException> failingSlicingIterator() {
        return new Throwables.Iterator<>() {
            protected int next = 1;
            protected boolean failed;

            @Override
            public boolean hasNext() {
                return next <= 4;
            }

            @Override
            public Integer next() {
                if (next == 2 && !failed) {
                    failed = true;
                    throw new IllegalStateException("second source read failed");
                }
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return next++;
            }
        };
    }

    @TempDir
    Path tempDir;

    protected File tempFile;
    protected Path tempPath;
    protected Path tempFolder;

    protected <T, E extends Exception> List<T> drain(Seq<T, E> seq) throws E {
        return seq.toList();
    }

    protected <T, E extends Exception> List<T> drainWithException(Seq<T, E> seq) throws E {
        return seq.toList();
    }

    protected <T, E extends Exception> void assertIteratorCountExhausts(final Seq<T, E> seq, final long expectedCount) throws E {
        final Throwables.Iterator<T, E> iter = seq.iteratorEx();

        assertEquals(expectedCount, iter.count());
        assertEquals(0, iter.count());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        seq.close();
    }

    @BeforeEach
    public void initTempFixtures() throws IOException {
        tempFile = tempDir.resolve("seq-lines-file.txt").toFile();
        Files.write(tempFile.toPath(), Arrays.asList("line1", "line2", "line3"), StandardCharsets.UTF_8);

        tempPath = tempDir.resolve("seq-lines-path.txt");
        Files.write(tempPath, Arrays.asList("line1", "line2", "line3"), StandardCharsets.UTF_8);

        tempFolder = tempDir.resolve("seq-temp-folder");
        Files.createDirectories(tempFolder);
    }

    public static class Department {
        List<Team> teams;

        Department(Team... teams) {
            this.teams = Arrays.asList(teams);
        }

        List<Team> getTeams() {
            return teams;
        }
    }

    public static class Team {
        List<Employee> members;

        Team(Employee... members) {
            this.members = Arrays.asList(members);
        }

        List<Employee> getMembers() {
            return members;
        }
    }

    public static class Employee {
        String name;

        Employee(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // advance(n<=0) returns early in primitive array iterators; skip(0) is a no-op

    // Seq.of(T[]) advance(n<=0) returns early

    // Seq.of(T[]) advance(n > remaining) exhausts

    // skip(n > remaining) exhausts the iterator via advance(n > len - position)

    // skip(n < remaining) partially advances position

    // ===================================================================
    // Review-driven boundary / contract tests (added 2026-05-08)

    // --- regression tests for 2026-06-10 deep-review fixes ---

    // ------------------------------------------------------------------------------------------------------------
    // Regression tests for the 2026-08-30 Seq review.
    // ------------------------------------------------------------------------------------------------------------

    // --- defer(Supplier): the supplier must be invoked at most once ---------------------------------------------

    // --- transformViaStream(fn, true): the deferred overload used to throw for every terminal op -----------------

    // --- ofLines(..): opening failures must surface as the declared checked IOException --------------------------

    // --- listFiles(..) ------------------------------------------------------------------------------------------

    // --- println()/forEachUntil(..) must close this sequence, not only the derived view --------------------------

    // --- containsDuplicates() must use the same key normalization as distinct() ----------------------------------

    // --- a null downstream collector is rejected by the operation, not by the first traversal --------------------

    // --- null-argument consistency --------------------------------------------------------------------------------

    // --- difference(mapper, c) applies the mapper to every element, like intersection(mapper, c) -----------------

    // --- sorted(comparator) recognizes an equivalent natural-order comparator ------------------------------------

    // --- hasMatchCountBetween reports the offending bound --------------------------------------------------------

    // --- onlyOne() must not inline an unbounded element rendering into its message -------------------------------

    // --- ofReversed(List) must not be quadratic on a non-RandomAccess list ---------------------------------------

    // --- the sliding trailing-window rule the javadoc now states --------------------------------------------------

    // --- min/max/minBy/maxBy throw NPE for a null extreme, as the javadoc now states ------------------------------

    // --- splitAt: the second sub-sequence is a live view of the source ---------------------------------------------

    // --- top/takeLast/last(int) really are @TerminalOpTriggered: they materialize the upstream on first access ----
    // (@TerminalOpTriggered has CLASS retention, so the annotation itself is not reflectively observable; assert the
    //  behaviour it documents instead.)

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
