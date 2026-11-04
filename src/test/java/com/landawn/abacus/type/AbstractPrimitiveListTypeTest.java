package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntList;

public class AbstractPrimitiveListTypeTest extends TestBase {

    private Type<IntList> intListType;

    @BeforeEach
    public void setUp() {
        intListType = createType("IntList");
    }

    @Test
    public void testIsPrimitiveList() {
        assertTrue(intListType.isPrimitiveList());
    }

    // ---- perf review 2026-09-26 G012 begin ----
    // G012-01: the char-list stringOf fast path must match EscapeUtil.escapeEcmaScript for every char value
    @Test
    public void testStringOfCharArray_everyCharMatchesEscapeEcmaScript() {
        final char[] all = new char[0x10000];

        for (int c = 0; c < all.length; c++) {
            all[c] = (char) c;
        }

        final StringBuilder expected = new StringBuilder("[");

        for (int c = 0; c < all.length; c++) {
            if (c > 0) {
                expected.append(AbstractType.ELEMENT_SEPARATOR);
            }

            expected.append('\'').append(com.landawn.abacus.util.EscapeUtil.escapeEcmaScript(String.valueOf(all[c]))).append('\'');
        }

        expected.append(']');

        org.junit.jupiter.api.Assertions.assertEquals(expected.toString(), AbstractPrimitiveListType.stringOf(all, all.length));
    }

    // G012-01: edge cases of the char-list stringOf path (null, empty, logical length shorter than the array, specials)
    @Test
    public void testStringOfCharArray_edgeCases() {
        org.junit.jupiter.api.Assertions.assertNull(AbstractPrimitiveListType.stringOf((char[]) null, 0));
        org.junit.jupiter.api.Assertions.assertEquals("[]", AbstractPrimitiveListType.stringOf(new char[] { 'a' }, 0));
        org.junit.jupiter.api.Assertions.assertEquals("['a']", AbstractPrimitiveListType.stringOf(new char[] { 'a', 'b' }, 1));
        org.junit.jupiter.api.Assertions.assertEquals("['\\'', '\\\"', '\\\\', '\\/', ' ', '~', '\u007f', '\\u0080', '\\n', '\\u0000']",
                AbstractPrimitiveListType.stringOf(new char[] { '\'', '"', '\\', '/', ' ', '~', '\u007f', '\u0080', '\n', '\u0000' }, 10));

        final Type<com.landawn.abacus.util.CharList> charListType = createType("CharList");
        final com.landawn.abacus.util.CharList list = com.landawn.abacus.util.CharList.of('a', '\'', 'Z', '\u00e9', '/');
        org.junit.jupiter.api.Assertions.assertEquals("['a', '\\'', 'Z', '\\u00E9', '\\/']", charListType.stringOf(list));
        org.junit.jupiter.api.Assertions.assertEquals(list, charListType.valueOf(charListType.stringOf(list)));
    }
    // ---- perf review 2026-09-26 G012 end ----

    // ---- deep review 2026-11-02b G015 begin ----
    // G015-01: pin for the direct escape of chars above U+007F in the CharList stringOf path: hex-digit boundaries, surrogate
    // halves on their own, U+007F kept verbatim, and a backing array longer than the list's logical size.
    @Test
    public void testStringOfCharList_charsAboveAsciiUseUpperCaseUnicodeEscape_r116() {
        final char[] chars = { (char) 0x80, (char) 0xFF, (char) 0x100, (char) 0x7FF, (char) 0x800, (char) 0x9999, (char) 0xABCD, (char) 0xD83D, (char) 0xDE00,
                (char) 0xDBFF, (char) 0xDC00, (char) 0xFFFE, (char) 0xFFFF, (char) 0x7F, 'a', '/' };
        final String bs = "\\";
        final String expected = "['" + bs + "u0080', '" + bs + "u00FF', '" + bs + "u0100', '" + bs + "u07FF', '" + bs + "u0800', '" + bs + "u9999', '" + bs
                + "uABCD', '" + bs + "uD83D', '" + bs + "uDE00', '" + bs + "uDBFF', '" + bs + "uDC00', '" + bs + "uFFFE', '" + bs + "uFFFF', '" + (char) 0x7F
                + "', 'a', '" + bs + "/']";

        org.junit.jupiter.api.Assertions.assertEquals(expected, AbstractPrimitiveListType.stringOf(chars, chars.length));
        org.junit.jupiter.api.Assertions.assertEquals("['" + bs + "u0080']", AbstractPrimitiveListType.stringOf(new char[] { (char) 0x80, (char) 0xFFFF }, 1));

        for (final char ch : chars) {
            if (ch > 0x7F) {
                org.junit.jupiter.api.Assertions.assertEquals(com.landawn.abacus.util.EscapeUtil.escapeEcmaScript(String.valueOf(ch)),
                        bs + "u" + String.format("%04X", (int) ch));
            }
        }

        final Type<com.landawn.abacus.util.CharList> charListType = createType("CharList");
        final com.landawn.abacus.util.CharList list = new com.landawn.abacus.util.CharList(64);
        list.addAll(chars);

        org.junit.jupiter.api.Assertions.assertEquals(expected, charListType.stringOf(list));
        org.junit.jupiter.api.Assertions.assertEquals(list, charListType.valueOf(charListType.stringOf(list)));
    }
    // ---- deep review 2026-11-02b G015 end ----
}
