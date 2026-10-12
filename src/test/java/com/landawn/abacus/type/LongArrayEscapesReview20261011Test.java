package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class LongArrayEscapesReview20261011Test extends TestBase {

    private final LongArrayType type = new LongArrayType();

    @Test
    void decodesEscapedDigitsInQuotedLongValues() {
        assertArrayEquals(new Long[] { 1L, -2L, null, 3L }, type.valueOf("[\"\\u0031\", '-\\u0032', null, 3]"));
    }

    @Test
    void retainsTheFullLongRangeAfterDecoding() {
        assertArrayEquals(new Long[] { Long.MIN_VALUE, Long.MAX_VALUE }, type.valueOf("[\"\\u002d9223372036854775808\", \"\\u0039223372036854775807\"]"));
        assertThrows(ArithmeticException.class, () -> type.valueOf("[\"\\u0039223372036854775808\"]"));
    }

    @Test
    void emptyAndWhitespaceOnlyQuotedTokensRemainInvalid() {
        assertThrows(NumberFormatException.class, () -> type.valueOf("[\"\"]"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("['']"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("[\" \" ]"));
    }

    @Test
    void malformedEscapesCannotTurnIntoNumericValues() {
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[\"\\uXXXX\"]"));
        assertThrows(NumberFormatException.class, () -> type.valueOf("[\"\\u0031x\"]"));
    }
}
