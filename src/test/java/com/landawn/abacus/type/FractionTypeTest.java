package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Fraction;
import com.landawn.abacus.util.Objectory;

public class FractionTypeTest extends TestBase {

    private FractionType fractionType;

    @BeforeEach
    public void setUp() {
        fractionType = (FractionType) createType(Fraction.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Fraction.class, fractionType.javaType());
    }

    @Test
    public void testIsNumber() {
        assertTrue(fractionType.isNumber());
    }

    @Test
    public void testIsImmutable() {
        assertTrue(fractionType.isImmutable());
    }

    @Test
    public void testIsComparable() {
        assertTrue(fractionType.isComparable());
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(fractionType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOf() {
        assertNull(fractionType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(fractionType.valueOf(null));
        assertNull(fractionType.valueOf(""));

    }

    @Test
    public void testSerializeTo_nullAsZero() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.isWriteNullNumberAsZero()).thenReturn(true);

        fractionType.serializeTo(writer, null, config);

        // The substituted zero now travels the ordinary (quoted) fraction path instead of being hand-written as a
        // bare '0': Fraction.ZERO renders as "0/1", exactly like any other fraction.
        verify(writer).writeCharacter("0/1");
    }

    // ---- review fixes 2026-09-06: T3-06 documented exceptions of valueOf(String) ----

    @Test
    public void reviewFixes20260906_valueOfInvalidFractionThrowsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> fractionType.valueOf("1/0"));
        assertThrows(ArithmeticException.class, () -> fractionType.valueOf("1 1/0"));
        assertThrows(ArithmeticException.class, () -> fractionType.valueOf("1/-2147483648"));
    }

    @Test
    public void reviewFixes20260906_valueOfDoesNotTrimWhitespace() {
        assertEquals(Fraction.of(1, 2), fractionType.valueOf("1/2"));
        assertEquals(Fraction.of(3, 2), fractionType.valueOf("1 1/2"));
        assertThrows(NumberFormatException.class, () -> fractionType.valueOf(" 1/2"));
        assertThrows(NumberFormatException.class, () -> fractionType.valueOf("1/2 "));
        assertThrows(NumberFormatException.class, () -> fractionType.valueOf("abc"));
    }

    // ---- review fixes 2026-09-08 (finding 119): writeNullNumberAsZero wrote a bare 0 for null while a non-null
    // fraction was written as a quoted string, so the JSON field flipped between number and string on nullness. ----

    @Test
    public void reviewFixes20260908_nullAsZeroKeepsTheFieldAString() throws IOException {
        final JsonSerConfig config = JsonSerConfig.create().setWriteNullNumberAsZero(true);
        final BufferedJsonWriter nullWriter = Objectory.createBufferedJsonWriter();
        final BufferedJsonWriter valueWriter = Objectory.createBufferedJsonWriter();

        try {
            fractionType.serializeTo(nullWriter, null, config);
            fractionType.serializeTo(valueWriter, Fraction.of(3, 4), config);

            // A fraction has no bare JSON-number form, so the substituted zero must be quoted like any other value.
            assertEquals("\"0/1\"", nullWriter.toString());
            assertEquals("\"3/4\"", valueWriter.toString());
        } finally {
            Objectory.recycle(nullWriter);
            Objectory.recycle(valueWriter);
        }
    }

    @Test
    public void reviewFixes20260908_nullAsZeroIsIndistinguishableFromARealZeroAndRoundTrips() throws IOException {
        final JsonSerConfig config = JsonSerConfig.create().setWriteNullNumberAsZero(true);
        final BufferedJsonWriter nullWriter = Objectory.createBufferedJsonWriter();
        final BufferedJsonWriter zeroWriter = Objectory.createBufferedJsonWriter();

        try {
            fractionType.serializeTo(nullWriter, null, config);
            fractionType.serializeTo(zeroWriter, Fraction.ZERO, config);

            assertEquals(zeroWriter.toString(), nullWriter.toString());
            assertEquals(Fraction.ZERO, fractionType.valueOf("0/1"));
        } finally {
            Objectory.recycle(nullWriter);
            Objectory.recycle(zeroWriter);
        }
    }

    @Test
    public void reviewFixes20260908_nullWithoutTheFlagIsStillNull() throws IOException {
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            fractionType.serializeTo(writer, null, JsonSerConfig.create());
            assertEquals("null", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }
}
