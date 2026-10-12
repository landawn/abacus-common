package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Numbers;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class TypeStructurePerformanceFixTest extends TestBase {
    @Test
    void fullWidthCharArraysPreserveSignsOffsetsLeadingZerosAndSuffixes() {
        for (int value : new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1000000000, 0, 1000000000, Integer.MAX_VALUE - 1, Integer.MAX_VALUE }) {
            final String digits = Integer.toString(value);
            final String padded = (value < 0 ? "-" : "+") + "000000000000" + (value < 0 ? digits.substring(1) : digits);

            for (String suffix : List.of("", "l", "L", "f", "F", "d", "D")) {
                final String token = padded + suffix;
                final char[] region = ("prefix" + token + "suffix").toCharArray();
                assertEquals(value, AbstractType.parseInt(region, 6, token.length()), token);
                assertEquals(value, Type.of(Integer.class).valueOf(region, 6, token.length()), token);
            }
        }

        for (long value : new long[] { Long.MIN_VALUE, Long.MIN_VALUE + 1, -1000000000000000000L, 0, 1000000000000000000L, Long.MAX_VALUE - 1,
                Long.MAX_VALUE }) {
            final String digits = Long.toString(value);
            final String padded = (value < 0 ? "-" : "+") + "000000000000000000000" + (value < 0 ? digits.substring(1) : digits);

            for (String suffix : List.of("", "l", "L", "f", "F", "d", "D")) {
                final String token = padded + suffix;
                final char[] region = ("prefix" + token + "suffix").toCharArray();
                assertEquals(value, AbstractType.parseLong(region, 6, token.length()), token);
                assertEquals(value, Type.of(Long.class).valueOf(region, 6, token.length()), token);
            }
        }
    }

    @Test
    void fullWidthRandomCharRegionsMatchTheirOriginalValues() {
        final Random random = new Random(20261008L);

        for (int i = 0; i < 512; i++) {
            final int intValue = random.nextInt();
            final String intText = Integer.toString(intValue);
            final String intToken = (intValue < 0 ? "-0000000000" + intText.substring(1) : "+0000000000" + intText) + "F";
            assertEquals(intValue, AbstractType.parseInt(("[" + intToken + "]").toCharArray(), 1, intToken.length()));

            final long longValue = random.nextLong();
            final String longText = Long.toString(longValue);
            final String longToken = (longValue < 0 ? "-0000000000000000000" + longText.substring(1) : "+0000000000000000000" + longText) + "L";
            assertEquals(longValue, AbstractType.parseLong(("[" + longToken + "]").toCharArray(), 1, longToken.length()));
        }
    }

    @Test
    void malformedCharactersAfterOverflowKeepFormatErrorsAndSuffixCauses() {
        for (String token : List.of("999999999999999999999x1", "-99999999999999999999.1", "+99999999999999999999 1")) {
            final NumberFormatException expectedInt = assertThrows(NumberFormatException.class, () -> Numbers.toInt(token));
            final NumberFormatException expectedLong = assertThrows(NumberFormatException.class, () -> Numbers.toLong(token));
            final String suffixed = token + "F";
            final char[] region = ("[" + suffixed + "]").toCharArray();
            final NumberFormatException actualInt = assertThrows(NumberFormatException.class, () -> AbstractType.parseInt(region, 1, suffixed.length()));
            final NumberFormatException actualLong = assertThrows(NumberFormatException.class, () -> AbstractType.parseLong(region, 1, suffixed.length()));
            assertEquals("Invalid numeric String: \"" + suffixed + "\"", actualInt.getMessage());
            assertEquals(actualInt.getMessage(), actualLong.getMessage());
            assertNotNull(actualInt.getCause());
            assertNotNull(actualLong.getCause());
            assertEquals(expectedInt.getMessage(), actualInt.getCause().getMessage());
            assertEquals(expectedLong.getMessage(), actualLong.getCause().getMessage());
        }
    }

    @Test
    void overflowKeepsStrippedTokenDiagnosticsAndRadixFallback() {
        for (String token : List.of("00000000002147483648", "-00000000002147483649")) {
            final ArithmeticException expected = assertThrows(ArithmeticException.class, () -> Numbers.toInt(token));
            final String input = token + "D";
            final ArithmeticException actual = assertThrows(ArithmeticException.class, () -> AbstractType.parseInt(input.toCharArray(), 0, input.length()));
            assertEquals(expected.getMessage(), actual.getMessage());
        }

        for (String token : List.of("00000000009223372036854775808", "-00000000009223372036854775809")) {
            final ArithmeticException expected = assertThrows(ArithmeticException.class, () -> Numbers.toLong(token));
            final String input = token + "L";
            final ArithmeticException actual = assertThrows(ArithmeticException.class, () -> AbstractType.parseLong(input.toCharArray(), 0, input.length()));
            assertEquals(expected.getMessage(), actual.getMessage());
        }

        final String intRadix = "0x0000000012345678";
        final String longRadix = "0x000000001234567890123456";
        assertEquals(Numbers.toInt(intRadix), AbstractType.parseInt(intRadix.toCharArray(), 0, intRadix.length()));
        assertEquals(Numbers.toLong(longRadix), AbstractType.parseLong(longRadix.toCharArray(), 0, longRadix.length()));
    }

    @Test
    void treeMultisetSerializationDoesNotSearchAgainForEveryCount() {
        final AtomicInteger comparisons = new AtomicInteger();
        final TreeMultiset<Integer> values = TreeMultiset.create((a, b) -> {
            comparisons.incrementAndGet();
            return Integer.compare(a, b);
        });
        for (int i = 0; i < 4096; i++) {
            values.add(i, 1 + i % 5);
        }

        final Type<TreeMultiset<Integer>> type = TypeFactory.getType("com.google.common.collect.TreeMultiset<Integer>");
        comparisons.set(0);
        final String json = type.stringOf(values);
        assertEquals(0, comparisons.get(), "serializing known entry counts must not repeat comparator searches");
        assertEquals(values, type.valueOf(json));
    }

    @Test
    void multisetEntryCountsPreserveDescendingViewsNullsAndLargeCounts() {
        final TreeMultiset<String> sorted = TreeMultiset.create();
        sorted.add("a", 1);
        sorted.add("b", 2);
        sorted.add("c", 3);
        sorted.add("d", 4);
        final SortedMultiset<String> view = sorted.subMultiset("b", BoundType.CLOSED, "d", BoundType.CLOSED).descendingMultiset();
        final Type<SortedMultiset<String>> sortedType = TypeFactory.getType("com.google.common.collect.SortedMultiset<String>");
        assertEquals("{\"d\": 4, \"c\": 3, \"b\": 2}", sortedType.stringOf(view));

        final LinkedHashMultiset<String> linked = LinkedHashMultiset.create();
        linked.add(null, 2);
        linked.add("FB", Integer.MAX_VALUE);
        linked.add("Ea", 3);
        final Type<Multiset<String>> linkedType = TypeFactory.getType("com.google.common.collect.LinkedHashMultiset<String>");
        assertEquals("{\"null\": 2, \"FB\": 2147483647, \"Ea\": 3}", linkedType.stringOf(linked));

        final ImmutableMultiset<String> immutable = ImmutableMultiset.<String> builder().addCopies("FB", Integer.MAX_VALUE).addCopies("Ea", 3).build();
        final Type<ImmutableMultiset<String>> immutableType = TypeFactory.getType("com.google.common.collect.ImmutableMultiset<String>");
        assertEquals("{\"FB\": 2147483647, \"Ea\": 3}", immutableType.stringOf(immutable));
    }
}
