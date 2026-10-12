package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CharsetQuotedPairReview20261003Test extends TestBase {

    @Test
    public void quotedPairInCharsetResolvesToLiteralCharacter() {
        assertEquals(StandardCharsets.ISO_8859_1, HttpUtil.getCharset("text/plain; charset=\"ISO-8859\\-1\""));
        assertEquals(StandardCharsets.UTF_8, HttpUtil.getCharset("text/plain; charset=\"\\U\\T\\F\\-\\8\"; x=y", StandardCharsets.ISO_8859_1));
        assertEquals(StandardCharsets.ISO_8859_1, HttpUtil.getCharset("text/plain; note=\"escaped\\\"; charset=UTF-16\"; CHARSET = \"ISO-8859\\-1\"", null));
    }

    @Test
    public void responseCharsetQuotedPairPreservesNonAsciiText() {
        // An application interceptor supplies the response without opening a socket.
        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> new Response.Builder().request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "text/plain; charset=\"ISO-8859\\-1\"")
                .body(ResponseBody.create("caf\u00e9".getBytes(StandardCharsets.ISO_8859_1), null))
                .build()).build();

        assertEquals("caf\u00e9", OkHttpRequest.create("http://example.test/", client).get(String.class));
    }

    @Test
    public void invalidOrUnterminatedQuotedCharsetsRetainFallback() {
        for (final String value : new String[] { "", "\"\"", "\"", "\"UTF-8\\", "\"UTF-8\\\"", "\"UTF-8\\\\\"", "\"UTF-8\\\"\"", "\"unknown\\-charset\"",
                "'UTF\\-8'" }) {
            final String contentType = "text/plain; charset=" + value;
            assertEquals(StandardCharsets.UTF_16, HttpUtil.getCharset(contentType, StandardCharsets.UTF_16), contentType);
            assertNull(HttpUtil.getCharset(contentType, null), contentType);
        }
    }

    @Test
    public void ordinaryCharsetAndSingleQuoteLeniencyAreUnchanged() {
        for (final String value : new String[] { "ISO-8859-1", "\"ISO-8859-1\"", "'ISO-8859-1'" }) {
            assertEquals(StandardCharsets.ISO_8859_1, HttpUtil.getCharset("text/plain; charset=" + value));
        }

        assertEquals(StandardCharsets.UTF_16, HttpUtil.getCharset("text/plain; note=\"charset=ISO-8859\\-1\"", StandardCharsets.UTF_16));
    }
}
