package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class CharsetNameTest {
    @Test
    void unicodeCannotCollideWithWarmAsciiNames() {
        Charsets.get("ISO-8859-1");
        Charsets.get("US-ASCII");
        for (final String name : new String[] { "\u0131SO-8859-1", "U\u017f-ASCII", "\uD800", "\uDC00", "\uD83D\uDE00", "UTF-\uFF18" }) {
            assertThrows(IllegalCharsetNameException.class, () -> Charset.forName(name));
            final var exception = assertThrows(IllegalCharsetNameException.class, () -> Charsets.get(name));
            assertEquals(name, exception.getCharsetName());
        }
    }

    @Test
    void legalNamesShareTheCacheAndInvalidAsciiKeepsJdkSemantics() {
        for (final String name : new String[] { "UTF-8", "utf-8", "UtF-8", "utf8", "ISO-8859-1", "US-ASCII" }) {
            assertEquals(Charset.forName(name), Charsets.get(name));
            assertSame(Charsets.get(name), Charsets.get(name.toLowerCase(java.util.Locale.ROOT)));
        }
        for (final String name : new String[] { "", " ", "UTF 8", "-UTF8", "UTF/8" }) {
            assertThrows(IllegalCharsetNameException.class, () -> Charsets.get(name));
        }
        assertThrows(UnsupportedCharsetException.class, () -> Charsets.get("x-no-such-charset-123456789"));
        assertThrows(IllegalArgumentException.class, () -> Charsets.get(null));
    }
}
