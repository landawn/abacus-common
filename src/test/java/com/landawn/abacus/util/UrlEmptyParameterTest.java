package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class UrlEmptyParameterTest extends TestBase {
    @Test
    void ambiguousMapAndPairEntriesAreRejectedBeforeAppendingTheirSeparator() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("", null);
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(map));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(new Object[] { "", null }));
        map.clear();
        map.put("a", 1);
        map.put("", null);
        final StringBuilder output = new StringBuilder();
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(map, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE, output));
        assertEquals("a=1", output.toString());
    }

    @Test
    void representableEmptyNamesNullValuesAndUnicodeRoundTrip() {
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("", "");
        values.put("flag", null);
        values.put("\u03B1", "\uD83D\uDE00");
        assertEquals(values, URLEncodedUtil.decode(URLEncodedUtil.encode(values)));
        assertEquals("=", URLEncodedUtil.encode(new Object[] { "", "" }));
        assertEquals("flag", URLEncodedUtil.encode(new Object[] { "flag", null }));
        assertEquals("", URLEncodedUtil.encode(Map.of()));
        assertEquals("", URLEncodedUtil.encode(new Object[0]));
    }
}
