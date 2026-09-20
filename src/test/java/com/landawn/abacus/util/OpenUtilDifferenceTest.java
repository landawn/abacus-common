package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;

@Tag("unit")
public class OpenUtilDifferenceTest extends TestBase {
    @Test
    void ignoredAliasesAreSymmetricAndNeverReadOrCompared() {
        for (final String value : Arrays.asList(null, "", "甲🙂")) {
            for (final String other : Arrays.asList(value, "different")) {
                final LowerName left = new LowerName(other);
                final IgnoredCamelName right = new IgnoredCamelName(value);
                left.failOnRead = true;
                right.failOnRead = true;
                for (boolean reverse : new boolean[] { false, true }) {
                    final Object a = reverse ? right : left;
                    final Object b = reverse ? left : right;
                    final AtomicInteger comparisons = new AtomicInteger();
                    final var diff = Difference.BeanDifference.ofByProps(a, b, null, (name, x, y) -> {
                        comparisons.incrementAndGet();
                        return N.equals(x, y);
                    });
                    assertTrue(diff.areEqual());
                    assertTrue(diff.common().isEmpty());
                    assertTrue(diff.onlyOnLeft().isEmpty());
                    assertTrue(diff.onlyOnRight().isEmpty());
                    assertEquals(0, comparisons.get());
                    assertTrue(Difference.BeanDifference.of(a, b).areEqual());
                    assertTrue(Difference.BeanDifference.of(a, b, List.of()).areEqual());
                    assertTrue(Difference.BeanDifference.of(List.of(a), List.of(b), List.of(), ignored -> 1, ignored -> 1).areEqual());
                }
                assertTrue(Difference.BeanDifference.of(null, right).onlyOnRight().isEmpty());
                left.failOnRead = false;
                right.failOnRead = false;
                final var selected = Difference.BeanDifference.of(left, right, List.of("username"));
                assertEquals(N.equals(other, value), selected.areEqual());
                assertEquals(1, selected.common().size() + selected.differentValues().size());
                final var reverseSelected = Difference.BeanDifference.of(right, left, List.of("userName"));
                assertEquals(N.equals(other, value), reverseSelected.areEqual());
                assertEquals(1, reverseSelected.common().size() + reverseSelected.differentValues().size());
            }
        }
    }

    public static class LowerName {
        private String username;
        boolean failOnRead;

        public LowerName() {
        }

        LowerName(String value) {
            username = value;
        }

        public String getUsername() {
            if (failOnRead)
                throw new AssertionError("excluded getter");
            return username;
        }

        public void setUsername(String value) {
            username = value;
        }
    }

    public static class IgnoredCamelName {
        @DiffIgnore
        private String userName;
        boolean failOnRead;

        public IgnoredCamelName() {
        }

        IgnoredCamelName(String value) {
            userName = value;
        }

        public String getUserName() {
            if (failOnRead)
                throw new AssertionError("excluded getter");
            return userName;
        }

        public void setUserName(String value) {
            userName = value;
        }
    }
}
