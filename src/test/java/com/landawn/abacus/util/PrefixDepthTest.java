package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class PrefixDepthTest {
    @Test
    void deepBuildAndCopyRetainValuesWithoutRecursiveTraversal() {
        final List<String> key = Collections.nCopies(10000, "\uD83D\uDE00");
        final var originalBuilder = PrefixSearchTable.<String, String> builder().add(key, "deep");
        final var table = originalBuilder.build();
        assertEquals("deep", table.get(key).get());
        final var copyBuilder = table.toBuilder();
        final List<String> branch = new ArrayList<>(key);
        branch.set(branch.size() - 1, "branch");
        copyBuilder.add(branch, "new");
        final var copy = copyBuilder.build();
        assertEquals("deep", copy.get(key).get());
        assertEquals("new", copy.get(branch).get());
        assertFalse(table.get(branch).isPresent());
        assertFalse(originalBuilder.build().get(branch).isPresent());
    }

    @Test
    void shallowPrefixesBranchingAndEmptyTablesKeepTheirBehavior() {
        final var builder = PrefixSearchTable.<String, String> builder().add(List.of("a"), "prefix").add(List.of("a", "b"), "child").add(List.of("c"), "other");
        final var copy = builder.build().toBuilder().build();
        assertEquals("prefix", copy.get(List.of("a", "x")).get());
        assertEquals("child", copy.get(List.of("a", "b")).get());
        assertEquals("other", copy.get(List.of("c")).get());
        assertFalse(PrefixSearchTable.<String, String> builder().build().toBuilder().build().get(List.of("a")).isPresent());
    }
}
