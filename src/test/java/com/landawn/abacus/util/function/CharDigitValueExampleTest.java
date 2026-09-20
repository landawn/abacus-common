package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class CharDigitValueExampleTest extends TestBase {
    @ParameterizedTest
    @CsvSource({ "48, 0", "55, 7", "57, 9", "1632, 0", "1639, 7", "1641, 9", "65303, 7", "120, -1", "65, -1", "0, -1", "32, -1", "178, -1", "55296, -1",
            "56320, -1", "65535, -1" })
    void decimalDigitConversionUsesUnicodeValues(int codeUnit, int expected) {
        CharToIntFunction digitValue = c -> Character.digit(c, 10);
        assertEquals(expected, digitValue.applyAsInt((char) codeUnit));
        assertEquals(codeUnit, CharToIntFunction.DEFAULT.applyAsInt((char) codeUnit));
    }
}
