package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class SplitterUtilReview20261011Test extends TestBase {
    @Test
    public void finalRetainedTokenDoesNotSearchOrCopyDiscardedPrefixes() {
        final String value = "a".repeat(100_000) + ",tail";
        for (final Splitter splitter : List.of(Splitter.with(','), Splitter.with("::"), Splitter.with(Pattern.compile(",")), Splitter.forLines())) {
            final CountingSequence source = new CountingSequence(value);
            assertEquals(List.of(value), splitter.limit(1).split(source));
            assertEquals(0, source.reads, "The final retained token does not need delimiter scanning");
            assertEquals(1, source.slices, "The remainder is the only token that needs materializing");
        }
    }

    @Test
    public void limitStopsScanningAfterTheRequiredSeparators() {
        final CountingSequence source = new CountingSequence("head," + "x".repeat(100_000) + ",tail");
        final ObjIterator<String> tokens = Splitter.with(',').limit(2).iterate(source);
        assertEquals("head", tokens.next());
        final int readsAfterFirstToken = source.reads;
        assertEquals("x".repeat(100_000) + ",tail", tokens.next());
        assertEquals(readsAfterFirstToken, source.reads);
        assertEquals(2, source.slices);
        assertFalse(tokens.hasNext());
    }

    @Test
    public void limitPreservesEmptyTokensAndTrimsTheWholeRemainder() {
        assertEquals(List.of(""), Splitter.with(',').limit(1).split(""));
        assertEquals(List.of(",a,"), Splitter.with(',').limit(1).split(",a,"));
        assertEquals(List.of("a", ""), Splitter.with(',').limit(2).split("a,"));
        assertEquals(List.of(", a ,"), Splitter.with(',').trimResults().limit(1).split("  , a ,  "));
        assertEquals(List.of("a", "b, c"), Splitter.with(',').stripResults().limit(2).split("\ta,\tb, c\t"));
        assertEquals(List.of("", "a,b"), Splitter.with(Pattern.compile("(?=a)")).limit(2).split("a,b"));
    }

    @Test
    public void omittedEmptyTokensAreStillSkippedBeforeTakingTheRemainder() {
        assertEquals(List.of("a,,b,"), Splitter.with(',').omitEmptyStrings().limit(1).split(",,a,,b,"));
        assertEquals(List.of("a", "b,,c,"), Splitter.with(',').omitEmptyStrings().limit(2).split(",a,,b,,c,"));
        assertEquals(List.of("a , b"), Splitter.with(',').trimResults().omitEmptyStrings().limit(1).split(" ,  , a , b  "));
        assertEquals(List.of(), Splitter.with(',').stripResults().omitEmptyStrings().limit(1).split("\t,\n,"));
    }

    private static final class CountingSequence implements CharSequence {
        private final String value;
        private int reads;
        private int slices;

        private CountingSequence(final String value) {
            this.value = value;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(final int index) {
            reads++;
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            slices++;
            return value.substring(start, end);
        }
    }
}
