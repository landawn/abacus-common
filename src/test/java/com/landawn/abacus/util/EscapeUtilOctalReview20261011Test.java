package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class EscapeUtilOctalReview20261011Test extends TestBase {
    @Test
    void allOctalValuesPreserveTheirDigitBoundaries() throws IOException {
        final EscapeUtil.OctalUnescaper translator = new EscapeUtil.OctalUnescaper();

        for (int digits = 1; digits <= 3; digits++) {
            final int limit = 1 << (3 * digits);

            for (int number = 0; number < limit; number++) {
                final String octal = String.format("%0" + digits + "o", number);
                final int digitCount = digits == 3 && octal.charAt(0) > '3' ? 2 : digits;
                final char expected = (char) Integer.parseInt(octal.substring(0, digitCount), 8);
                final String input = "prefix\\" + octal + "8tail";
                final StringWriter output = new StringWriter();

                assertEquals(digitCount + 1, translator.translate(input, 6, output), input);
                assertEquals(String.valueOf(expected), output.toString(), input);
                assertEquals("prefix" + expected + octal.substring(digitCount) + "8tail", EscapeUtil.unescapeJava(input), input);
            }
        }
    }

    @Test
    void nonOctalInputIsLeftForTheOtherTranslators() throws IOException {
        final EscapeUtil.OctalUnescaper translator = new EscapeUtil.OctalUnescaper();

        for (final String input : new String[] { "text", "\\", "\\8", "\\9", "\\u0041", "\\n", "\\x", "\ud83d\ude00" }) {
            final StringWriter output = new StringWriter();
            assertEquals(0, translator.translate(input, 0, output), input);
            assertEquals("", output.toString(), input);
        }

        assertEquals("8|9|A|\n|x", EscapeUtil.unescapeJava("\\8|\\9|\\u0041|\\n|\\x"));
        assertEquals("\ud83d\ude00A?7", EscapeUtil.unescapeJava("\ud83d\ude00\\101\\777"));
        assertEquals("plain text \ud83d\ude00", EscapeUtil.unescapeJava("plain text \ud83d\ude00"));
    }
}
