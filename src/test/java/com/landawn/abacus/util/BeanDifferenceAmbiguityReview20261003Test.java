package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.util.Difference.BeanDifference;

@Tag("unit")
public class BeanDifferenceAmbiguityReview20261003Test extends TestBase {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "same", "different" })
    void exactMatchLeavesFuzzyCompetitorOnLeft(final String secondValue) {
        final TwoNames left = new TwoNames(secondValue == null ? null : "same", secondValue);
        final LowerName right = new LowerName(secondValue);

        final var diff = BeanDifference.of(left, right);
        assertFalse(diff.areEqual());
        assertEquals(1, diff.onlyOnLeft().size());
        assertTrue(diff.onlyOnLeft().containsKey("userName"));
        assertEquals(left.userName, diff.onlyOnLeft().get("userName"));
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        if (secondValue != null) {
            assertEquals(Map.of("username", secondValue), diff.common());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void customEquivalenceCannotHideUnmatchedProperty(final boolean equivalent) {
        final TwoNames left = new TwoNames(null, null);
        final LowerName right = new LowerName(null);

        assertFalse(BeanDifference.ofByValues(left, right, (a, b) -> equivalent).areEqual());
        final var diff = BeanDifference.ofByProps(left, right, (name, a, b) -> {
            assertEquals("username", name);
            return equivalent;
        });
        assertTrue(diff.onlyOnLeft().containsKey("userName"));
        assertFalse(diff.areEqual());
    }

    @Test
    void emptyAndNullSelectionsUseAllProperties() {
        final TwoNames left = new TwoNames("same", "same");
        final LowerName right = new LowerName("same");

        assertEquals(Map.of("userName", "same"), BeanDifference.of(left, right, List.of()).onlyOnLeft());
        assertEquals(Map.of("userName", "same"), BeanDifference.of(left, right, (List<String>) null).onlyOnLeft());
        assertEquals(Map.of("userName", "same"), BeanDifference.ofByProps(left, right, List.of(), (name, a, b) -> true).onlyOnLeft());
    }

    @Test
    void collectionOverloadsRetainStructuralDifferences() {
        final List<Object> left = List.of(new TwoNames("same", "same"));
        final List<Object> right = List.of(new LowerName("same"));

        assertFalse(BeanDifference.of(left, right, ignored -> 1).areEqual());
        assertFalse(BeanDifference.of(left, right, List.of(), ignored -> 1).areEqual());
        assertFalse(BeanDifference.of(left, right, ignored -> 1, ignored -> 1).areEqual());
        assertFalse(BeanDifference.of(left, right, List.of(), ignored -> 1, ignored -> 1).areEqual());
    }

    @Test
    void exactMatchingDoesNotDependOnDeclarationOrder() {
        final var diff = BeanDifference.of(new ReverseTwoNames(), new LowerName("exact"));
        assertEquals(Map.of("username", "exact"), diff.common());
        assertEquals(Map.of("userName", "fuzzy"), diff.onlyOnLeft());
    }

    @Test
    void ambiguousFuzzyMatchesWithoutExactNamesStillFail() {
        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(new FuzzyTwoNames(), new LowerName("same")));
    }

    @Test
    void ignoredExactNameDoesNotReserveAnActiveFuzzyMatch() {
        assertTrue(BeanDifference.of(new IgnoredExactPair(), new LowerName("same")).areEqual());
    }

    // follow-up 2026-10-04: the message names local/anonymous bean classes, whose canonical name is null.
    @Test
    void ambiguityMessageNamesAnonymousBeanClasses() {
        final FuzzyTwoNames left = new FuzzyTwoNames() {
        };
        final LowerName right = new LowerName("same") {
        };
        assertNull(left.getClass().getCanonicalName());

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(left, right));
        assertFalse(e.getMessage().contains("null"), e.getMessage());
        assertTrue(e.getMessage().contains(left.getClass().getName()), e.getMessage());
        assertTrue(e.getMessage().contains(right.getClass().getName()), e.getMessage());
    }

    public static class ReverseTwoNames {
        public String username = "exact";
        public String userName = "fuzzy";
    }

    public static class FuzzyTwoNames {
        public String userName = "same";
        public String user_name = "same";
    }

    public static class IgnoredExactPair {
        public String userName = "same";
        @DiffIgnore
        public String username = "ignored";
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "same", "different" })
    void reverseComparisonRetainsTheUnmatchedProperty(final String extraValue) {
        final var diff = BeanDifference.of(new LowerName("same"), new TwoNames(extraValue, "same"));

        assertFalse(diff.areEqual());
        assertEquals(Map.of("username", "same"), diff.common());
        assertEquals(1, diff.onlyOnRight().size());
        assertTrue(diff.onlyOnRight().containsKey("userName"));
        assertEquals(extraValue, diff.onlyOnRight().get("userName"));
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void ordinaryFuzzyMatchingRemainsSymmetric(final boolean reverse) {
        final Object left = reverse ? new LowerName("same") : new CamelName("same");
        final Object right = reverse ? new CamelName("same") : new LowerName("same");
        final var equal = BeanDifference.of(left, right);

        assertTrue(equal.areEqual());
        assertEquals(1, equal.common().size());
        assertFalse(BeanDifference.of(left, reverse ? new CamelName("other") : new LowerName("other")).areEqual());
    }

    @Test
    void explicitAliasesAndRepeatedSelectionsRemainValid() {
        final TwoNames left = new TwoNames("same", "same");
        final LowerName right = new LowerName("same");
        final List<String> selection = Arrays.asList("userName", "username", "userName");
        final var diff = BeanDifference.of(left, right, selection);

        assertTrue(diff.areEqual());
        assertEquals(Map.of("userName", "same", "username", "same"), diff.common());
        assertTrue(BeanDifference.of(List.of(left), List.of(right), selection, ignored -> 1, ignored -> 1).areEqual());
    }

    @Test
    void ignoredLeftAliasDoesNotConsumeTheActiveMatch() {
        final var diff = BeanDifference.of(new IgnoredCamelPair(), new LowerName("same"));

        assertTrue(diff.areEqual());
        assertEquals(Map.of("username", "same"), diff.common());
    }

    @Test
    void ignoredRightPropertyExcludesEveryMatchingLeftProperty() {
        final var diff = BeanDifference.of(new TwoNames("a", "b"), new IgnoredLowerName());

        assertTrue(diff.areEqual());
        assertTrue(diff.common().isEmpty());
    }

    @Test
    void exactDistinctPropertiesAndNullBeanRemainSupported() {
        final TwoNames left = new TwoNames("first", "second");
        final var equal = BeanDifference.of(left, new TwoNames("first", "second"));

        assertTrue(equal.areEqual());
        assertEquals(2, equal.common().size());
        assertEquals(2, BeanDifference.of(left, (Object) null).onlyOnLeft().size());
    }

    public static class TwoNames {
        public String userName;
        public String username;

        public TwoNames() {
        }

        TwoNames(final String camel, final String lower) {
            userName = camel;
            username = lower;
        }
    }

    public static class LowerName {
        public String username;

        public LowerName() {
        }

        LowerName(final String value) {
            username = value;
        }
    }

    public static class CamelName {
        public String userName;

        public CamelName() {
        }

        CamelName(final String value) {
            userName = value;
        }
    }

    public static class IgnoredCamelPair {
        @DiffIgnore
        public String userName = "ignored";
        public String username = "same";
    }

    public static class IgnoredLowerName {
        @DiffIgnore
        public String username = "ignored";
    }
}
