package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import okhttp3.Request;

public class CurlMethodReview20261003Test extends TestBase {

    // Each method is a legal HTTP token accepted by OkHttp; none of these generated commands is executed.
    private static final String[][] METHODS = { { "M`id`", "'M`id`'", "\"M\\`id\\`\"" }, { "M$HOME", "'M$HOME'", "\"M\\$HOME\"" },
            { "M'Q", "'M'\\''Q'", "\"M'Q\"" }, { "M|Q", "'M|Q'", "\"M|Q\"" }, { "M*Q", "'M*Q'", "\"M*Q\"" }, { "M&Q", "'M&Q'", "\"M&Q\"" } };

    @Test
    public void extensionMethodsRemainLiteralWithSingleQuotes() {
        assertLiteralMethods('\'', 1);
    }

    @Test
    public void extensionMethodsRemainLiteralWithDoubleQuotes() {
        assertLiteralMethods('"', 2);
    }

    private static void assertLiteralMethods(final char quoteChar, final int expectedColumn) {
        for (final String[] fixture : METHODS) {
            final Request request = new Request.Builder().url("http://localhost/example").method(fixture[0], null).build();
            assertEquals(fixture[0], request.method());
            final String curl = WebUtil.buildCurlByMethodName(request.method(), request.url().toString(), Map.of(), null, null, quoteChar);
            assertTrue(curl.stripLeading().startsWith("curl -X " + fixture[expectedColumn] + " "), curl);
        }

        assertTrue(WebUtil.buildCurlByMethodName("PROPFIND", "http://localhost/example", Map.of(), null, null, quoteChar)
                .stripLeading()
                .startsWith("curl -X PROPFIND "));
    }
}
