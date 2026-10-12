package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@Tag("unit")
class CurlContentTypeReview20261011Test extends TestBase {
    @Test
    void okHttpConversionPreservesAnExplicitlyEmptyContentType() {
        for (final String option : new String[] { "-H 'Content-Type;'", "--header='content-type;'", "-sH'CONTENT-TYPE;'" }) {
            for (final String body : new String[] { "a=1", "" }) {
                final String command = "curl https://example.test/ " + option + " --data-raw '" + body + "'";
                final String code = WebUtil.curlToOkHttpRequestCode(command);
                assertTrue(code.contains("RequestBody.create(\"" + body + "\", null)"), code);
                assertTrue(code.contains(", \"\")"), code);
                assertFalse(code.contains("application/x-www-form-urlencoded"), code);
                assertFalse(code.contains("suppress the header"), code);
            }
        }
    }

    @Test
    void generatedNullMediaTypeLeavesTheEmptyHeaderOnTheWire() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ok"));
            final String code = WebUtil.curlToOkHttpRequestCode("curl " + server.url("/") + " -H 'Content-Type;' -d 'a=1'");
            assertTrue(code.contains("RequestBody.create(\"a=1\", null)"), code);
            assertTrue(code.contains(".header(\"Content-Type\", \"\")"), code);
            try (Response ignored = OkHttpRequest.url(server.url("/")).header("Content-Type", "").body(RequestBody.create("a=1", null)).post()) {
                final RecordedRequest request = server.takeRequest();
                assertEquals("", request.getHeader("Content-Type"));
                assertEquals("a=1", request.getBody().readUtf8());
            }
        }
    }

    @Test
    void legacyConversionRejectsOnlyAnEmptyContentTypeWithAnAttachedBody() {
        for (final String body : new String[] { "a=1", "" }) {
            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> WebUtil.curlToHttpRequestCode("curl https://example.test/ -H 'Content-Type;' -d '" + body + "'"));
            assertTrue(failure.getMessage().contains("curlToOkHttpRequestCode"));
        }
        assertTrue(WebUtil.curlToHttpRequestCode("curl https://example.test/ -H 'Content-Type;'").contains(".header(\"Content-Type\", \"\")"));
    }

    @Test
    void builtCurlWithAnEmptyContentTypeConvertsOnlyToOkHttp() {
        // buildCurl writes an empty or null Content-Type value as -H 'Content-Type;'. HttpRequest cannot send that
        // header empty with a body, so the round trip is supported by the OkHttp converter only, and rejected loudly
        // by the HttpRequest converter instead of generating code that sends a different Content-Type.
        for (final String value : new String[] { "", null }) {
            final Map<String, Object> headers = new HashMap<>();
            headers.put("Content-Type", value);

            for (final char quoteChar : new char[] { '\'', '"' }) {
                final String curl = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", headers, "a=1", "application/json", quoteChar);
                assertTrue(curl.contains("-H " + quoteChar + "Content-Type;" + quoteChar), curl);
                assertFalse(curl.contains("application/json"), curl);

                final String okHttp = WebUtil.curlToOkHttpRequestCode(curl);
                assertTrue(okHttp.contains("RequestBody.create(\"a=1\", null)"), okHttp);
                assertTrue(okHttp.contains(".header(\"Content-Type\", \"\")"), okHttp);

                final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(curl));
                assertTrue(failure.getMessage().contains("curlToOkHttpRequestCode"), failure.getMessage());
            }
        }

        // Without a body the empty header round-trips through both converters.
        final Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "");
        final String curl = WebUtil.buildCurl(HttpMethod.GET, "https://example.test/", headers, null, null, '\'');
        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains(".header(\"Content-Type\", \"\")"));
        assertTrue(WebUtil.curlToOkHttpRequestCode(curl).contains(".header(\"Content-Type\", \"\")"));
    }

    @Test
    void absentAndSuppressedContentTypesRetainTheirExistingDefaults() {
        for (final String option : new String[] { "", " -H 'Content-Type:'" }) {
            final String command = "curl https://example.test/ -d 'a=1'" + option;
            final String okHttp = WebUtil.curlToOkHttpRequestCode(command);
            final String legacy = WebUtil.curlToHttpRequestCode(command);
            assertTrue(okHttp.contains("MediaType.parse(\"application/x-www-form-urlencoded\")"), okHttp);
            assertTrue(legacy.contains(".header(\"Content-Type\", \"application/x-www-form-urlencoded\")"), legacy);
            assertFalse(okHttp.contains(".header(\"Content-Type\", \"\")"), okHttp);
            if (!option.isEmpty()) {
                assertTrue(okHttp.contains("suppress the header(s) Content-Type"), okHttp);
                assertTrue(legacy.contains("suppress the header(s) Content-Type"), legacy);
            }
        }
    }
}
