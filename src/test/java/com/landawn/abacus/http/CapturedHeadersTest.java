package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class CapturedHeadersTest extends TestBase {
    @AfterEach
    public void resetFilter() {
        HARUtil.resetThreadLocalHeaderFilter();
    }

    @Test
    public void testHarReplayPreservesRepeatedAcceptAndCookie() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ok"));
            final Map<String, Object> entry = new HashMap<>();
            entry.put("url", server.url("/har").toString());
            entry.put("method", "GET");
            entry.put("headers", List.of(header("Accept", "text/plain; note=\"a,b\", text/html"), header("accept", "application/json"), header("Cookie", "a=1"),
                    header("COOKIE", "b=2")));
            assertEquals("ok", HARUtil.sendRequestByRequestEntry(entry, String.class));
            assertHeaders(server.takeRequest());
        }
    }

    @Test
    public void testBothGeneratedProgramsPreserveEffectiveRepeatedHeaders() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            final String curl = "curl " + server.url("/generated")
                    + " -H 'Accept: text/plain; note=\"a,b\", text/html' -H 'accept: application/json' -H 'Cookie: a=1' -H 'COOKIE: b=2'"
                    + " -H 'cOnTeNt-TyPe: text/plain; charset=UTF-8' --data-raw 'caf\u00e9\u4e2d'";
            int sequence = 0;
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(curl), WebUtil.curlToOkHttpRequestCode(curl) }) {
                server.enqueue(new MockResponse().setBody("ok"));
                executeGenerated("CapturedHeaderProgram" + sequence++, code);
                final RecordedRequest sent = server.takeRequest();
                assertHeaders(sent);
                assertEquals("text/plain; charset=UTF-8", sent.getHeader("Content-Type"));
                assertEquals("caf\u00e9\u4e2d", sent.getBody().readUtf8());
            }
        }
    }

    @Test
    public void testCombinationAllowlistAndConditionalWildcards() {
        for (final String name : new String[] { "Accept", "Accept-Charset", "Accept-Encoding", "Accept-Language",
                "Access-Control-Request-Headers", "Cache-Control", "Content-Language", "Expect", "Forwarded", "If-Match", "If-None-Match", "Pragma", "TE",
                "Via", "X-Forwarded-For" }) {
            final String first = name.startsWith("If-") ? "\"one,two\"" : "one,two";
            final String second = name.startsWith("If-") ? "\"three\"" : "three";
            final HttpHeaders headers = HARUtil
                    .getHeadersByRequestEntry(Map.of("headers", List.of(header(name, first), header(name.toLowerCase(java.util.Locale.ROOT), second))));
            assertEquals(first + ", " + second, headers.getAsString(name));
            assertEquals(List.of(name), new ArrayList<>(headers.headerNames()));
        }
        for (final String name : new String[] { "Content-Type", "Host", "Authorization", "X-Custom", "Range", "Content-Encoding", "Transfer-Encoding" }) {
            assertRejected(name, "one", "two");
        }
        for (final String name : new String[] { "If-Match", "If-None-Match" }) {
            assertRejected(name, "*", "\"tag\"");
            assertRejected(name, "\"tag\"", "*");
        }
    }

    @Test
    public void testFilteringPrecedesCombinationAndNullValuesAreExplicit() {
        assertTrue(HARUtil.getHeadersByRequestEntry(Map.of()).isEmpty());
        assertTrue(HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of())).isEmpty());
        HARUtil.setThreadLocalHeaderFilter((name, value) -> !"excluded".equals(value));
        final Map<String, Object> entry = Map.of("headers",
                Arrays.asList(null, Map.of("value", "missing-name"), header("Content-Type", "text/plain"), header("content-type", "excluded")));
        assertEquals("text/plain", HARUtil.getHeadersByRequestEntry(entry).getAsString("Content-Type"));
        final Map<String, String> nullHeader = new HashMap<>();
        nullHeader.put("name", "Accept");
        assertNull(HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(nullHeader))).get("Accept"));
        assertThrows(IllegalArgumentException.class,
                () -> HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(nullHeader, header("Accept", "text/plain")))));
        assertThrows(IllegalArgumentException.class,
                () -> HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(header("Accept", "text/plain"), nullHeader))));
    }

    @Test
    public void testEmptyRepeatedValuesDoNotManufactureSeparators() {
        for (final String name : new String[] { "Accept", "Cookie" }) {
            for (final String[] values : new String[][] { { "", "value" }, { "value", "" }, { " ", "value" }, { "", "" } }) {
                final String expected = values[0].isBlank() ? values[1] : values[0];
                final HttpHeaders headers = HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(header(name, values[0]), header(name, values[1]))));
                assertEquals(expected, headers.getAsString(name));
                final String curl = "curl https://example.test/ -H '" + name + ": " + values[0] + "' -H '" + name + ": " + values[1] + "'";
                for (final String code : new String[] { WebUtil.curlToHttpRequestCode(curl), WebUtil.curlToOkHttpRequestCode(curl) }) {
                    if (expected.isEmpty()) {
                        // "-H 'Name: '" is curl's spelling for suppressing a header, not for sending it
                        // empty ("-H 'Name;'" does that), so two of them add no header at all.
                        assertFalse(code.contains(".header("), code);
                        assertTrue(code.contains("suppress the header(s) " + name), code);
                    } else {
                        assertTrue(code.contains(".header(\"" + name + "\", \"" + expected + "\")"), code);
                    }

                    assertFalse(code.contains("null"), code);
                }
            }
        }
    }

    private static Map<String, String> header(final String name, final String value) {
        return Map.of("name", name, "value", value);
    }

    private static void assertRejected(final String name, final String first, final String second) {
        assertThrows(IllegalArgumentException.class,
                () -> HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(header(name, first), header(name, second)))));
        final String curl = "curl https://example.test/ -H '" + name + ": " + first + "' -H '" + name + ": " + second + "'";
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(curl));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(curl));
    }

    private static void assertHeaders(final RecordedRequest request) {
        assertEquals(List.of("text/plain; note=\"a,b\", text/html, application/json"), request.getHeaders().values("Accept"));
        assertEquals(List.of("a=1; b=2"), request.getHeaders().values("Cookie"));
    }

    private static void executeGenerated(final String name, final String code) throws Exception {
        final Path directory = Files.createTempDirectory("captured-header-program-");
        final Path source = directory.resolve(name + ".java");
        try {
            Files.writeString(source, "import com.landawn.abacus.http.*; import okhttp3.Response; import okhttp3.RequestBody; import okhttp3.MediaType; "
                    + "public class " + name + " { public static void run() throws Exception { " + code + " } }", StandardCharsets.UTF_8);
            final javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler);
            try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
                final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
                assertTrue(compiler.getTask(null, files, null, List.of("-classpath", classpath, "-d", directory.toString()), null,
                        files.getJavaFileObjects(source.toFile())).call(), code);
            }
            try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] { directory.toUri().toURL() }, CapturedHeadersTest.class.getClassLoader())) {
                loader.loadClass(name).getMethod("run").invoke(null);
            }
        } finally {
            Files.deleteIfExists(directory.resolve(name + ".class"));
            Files.deleteIfExists(source);
            Files.deleteIfExists(directory);
        }
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-08 spillover S3 ITEM 2 (finding 98): the combinable set covered nine names, so a HAR or
    // cURL command captured through a proxy - which repeats Via / Forwarded / X-Forwarded-For - was
    // rejected wholesale even though those fields are #lists whose "," combination is semantics-preserving
    // (RFC 9110 5.3 / 5.6.1). TE is in the set too: being hop-by-hop does not stop it combining.
    // ------------------------------------------------------------------------------------------

    @Test
    public void reviewFixes20260908_proxyCapturedListFieldsAreCombinedInsteadOfRejected() {
        final HttpHeaders headers = HARUtil.getHeadersByRequestEntry(Map.of("headers",
                List.of(header("Via", "1.1 edge"), header("via", "1.1 origin"), header("X-Forwarded-For", "203.0.113.7"),
                        header("x-forwarded-for", "198.51.100.9"), header("Forwarded", "for=203.0.113.7;proto=https"), header("forwarded", "for=198.51.100.9"),
                        header("TE", "trailers"), header("te", "gzip"), header("Expect", "100-continue"), header("expect", "100-continue"),
                        header("Access-Control-Request-Headers", "x-a"), header("access-control-request-headers", "x-b"))));

        assertEquals("1.1 edge, 1.1 origin", headers.getAsString("Via"));
        assertEquals("203.0.113.7, 198.51.100.9", headers.getAsString("X-Forwarded-For"));
        assertEquals("for=203.0.113.7;proto=https, for=198.51.100.9", headers.getAsString("Forwarded"));
        assertEquals("trailers, gzip", headers.getAsString("TE"));
        assertEquals("100-continue, 100-continue", headers.getAsString("Expect"));
        assertEquals("x-a, x-b", headers.getAsString("Access-Control-Request-Headers"));
        // one entry per field, under the spelling first encountered
        assertEquals(List.of("Via", "X-Forwarded-For", "Forwarded", "TE", "Expect", "Access-Control-Request-Headers"),
                new ArrayList<>(headers.headerNames()));

        // both cURL generators combine them the same way instead of throwing
        final String curl = "curl https://example.test/ -H 'Via: 1.1 edge' -H 'via: 1.1 origin'"
                + " -H 'X-Forwarded-For: 203.0.113.7' -H 'x-forwarded-for: 198.51.100.9'";
        for (final String code : new String[] { WebUtil.curlToHttpRequestCode(curl), WebUtil.curlToOkHttpRequestCode(curl) }) {
            assertTrue(code.contains(".header(\"Via\", \"1.1 edge, 1.1 origin\")"), code);
            assertTrue(code.contains(".header(\"X-Forwarded-For\", \"203.0.113.7, 198.51.100.9\")"), code);
        }

        // fields whose repetition still cannot be reproduced keep rejecting
        assertRejected("Content-Encoding", "gzip", "br");
        assertRejected("Transfer-Encoding", "chunked", "gzip");
        assertRejected("Authorization", "Bearer a", "Bearer b");
        assertRejected("Host", "a.example", "b.example");
    }

}
