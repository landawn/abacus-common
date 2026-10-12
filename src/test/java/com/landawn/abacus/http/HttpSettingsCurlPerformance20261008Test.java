package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class HttpSettingsCurlPerformance20261008Test extends TestBase {
    private static final List<Function<String, String>> CURL_RENDERERS = List.of(WebUtil::curlToHttpRequestCode, WebUtil::curlToOkHttpRequestCode);
    private static final String TARGET = "https://example.test/items";

    @Test
    void settingsCopyPreservesAbsentAndEmptyHeadersAndUnsetFlags() {
        final HttpSettings absent = new HttpSettings();
        final HttpSettings absentCopy = absent.copy();

        for (final HttpSettings settings : List.of(absent, absentCopy)) {
            assertNull(settings.headersOrNull());
            assertNull(settings.contentFormatOrNull());
            assertNull(settings.useCachesOrNull());
            assertNull(settings.doInputOrNull());
            assertNull(settings.doOutputOrNull());
            assertNull(settings.isOneWayRequestOrNull());
            assertNull(settings.streamRequestBodyOrNull());
        }

        final HttpSettings empty = new HttpSettings();
        empty.headers();
        final HttpSettings emptyCopy = empty.copy();
        assertNotSame(empty.headers(), emptyCopy.headers());
        assertTrue(emptyCopy.headers().isEmpty());
        assertNull(emptyCopy.contentFormatOrNull());
        assertEquals(empty.getContentFormat(), emptyCopy.getContentFormat());
        emptyCopy.header("X-Copy", "only in copy");
        assertTrue(empty.headers().isEmpty());
    }

    @Test
    void settingsCopyKeepsIndependentHeaderNamesAndSharedValuesWithoutFreezingFormat() throws Exception {
        final List<String> values = new ArrayList<>(List.of("first"));
        final HttpSettings original = new HttpSettings().setConnectTimeout(1234)
                .setReadTimeout(5678)
                .setProxy(Proxy.NO_PROXY)
                .setSSLSocketFactory(SSLContext.getDefault().getSocketFactory())
                .useCaches(true)
                .doInput(false)
                .doOutput(true)
                .setOneWayRequest(false)
                .streamRequestBody(true)
                .header("Content-Type", "application/json")
                .header("X-Mixed", values)
                .header("x-mixed", values)
                .header("X-Null", null);

        final HttpSettings copy = original.copy();
        assertNotSame(original.headers(), copy.headers());
        assertEquals(original.headers(), copy.headers());
        assertTrue(copy.headers().headerNames().contains("X-Mixed"));
        assertFalse(copy.headers().headerNames().contains("x-mixed"));
        assertSame(values, copy.headers().get("X-MIXED"));
        assertTrue(copy.headers().containsHeader("x-null"));
        assertNull(copy.headers().get("X-Null"));
        assertEquals(1234, copy.getConnectTimeout());
        assertEquals(5678, copy.getReadTimeout());
        assertSame(original.getProxy(), copy.getProxy());
        assertSame(original.getSSLSocketFactory(), copy.getSSLSocketFactory());
        assertEquals(Boolean.TRUE, copy.useCachesOrNull());
        assertEquals(Boolean.FALSE, copy.doInputOrNull());
        assertEquals(Boolean.TRUE, copy.doOutputOrNull());
        assertEquals(Boolean.FALSE, copy.isOneWayRequestOrNull());
        assertEquals(Boolean.TRUE, copy.streamRequestBodyOrNull());
        assertNull(copy.contentFormatOrNull());

        values.add("second");
        assertEquals(List.of("first", "second"), copy.headers().get("x-mixed"));
        copy.header("Content-Type", "application/xml");
        copy.headers().remove("x-mixed");
        assertEquals(ContentFormat.XML, copy.getContentFormat());
        assertEquals(ContentFormat.JSON, original.getContentFormat());
        assertSame(values, original.headers().get("X-Mixed"));

        final HttpSettings explicitFormat = new HttpSettings().setContentFormat(ContentFormat.JSON).header("Content-Type", "application/xml");
        assertEquals(ContentFormat.JSON, explicitFormat.copy().contentFormatOrNull());
    }

    @Test
    void copyingSettingsKeepsItsNormalizedHeaderInvariantAndWrappedMapSemantics() {
        final Map<String, Object> duplicates = new LinkedHashMap<>();
        duplicates.put("Accept", "first");
        duplicates.put("accept", "second");
        final HttpHeaders wrapped = HttpHeaders.wrap(duplicates);
        final HttpSettings original = new HttpSettings().setHeaders(wrapped);
        final HttpSettings copy = original.copy();

        assertEquals(1, original.headers().headerNames().size());
        assertEquals(original.headers(), copy.headers());
        assertEquals("second", copy.headers().get("ACCEPT"));
        assertEquals(2, wrapped.copy().headerNames().size());
        duplicates.put("Accept", "changed externally");
        assertEquals("second", original.headers().get("Accept"));
        assertEquals("second", copy.headers().get("Accept"));
    }

    @Test
    void longCurlFlagClustersProduceTheSameRequests() {
        final String flags = "s".repeat(8192);
        for (final Function<String, String> render : CURL_RENDERERS) {
            assertEquals(render.apply("curl -s " + TARGET), render.apply("curl -" + flags + " " + TARGET));
            assertEquals(render.apply("curl -I " + TARGET), render.apply("curl -" + flags + "I " + TARGET));
        }
    }

    @Test
    void clusteredCurlFlagsPreserveAttachedArgumentsAndLiteralOptionValues() {
        final String[][] equivalent = { { "-sSLXPOST", "-X POST" }, { "-IsL", "-I" }, { "-sSLIX POST", "-I -X POST" },
                { "-ksH'X-Value: -sX'", "-H 'X-Value: -sX'" }, { "-sSda=1", "-d a=1" }, { "-sLo ignored.txt", "-o ignored.txt" },
                { "-sLd '-sXPOST' -H 'X-Value: -Is'", "-d '-sXPOST' -H 'X-Value: -Is'" }, { "-s-head", "--head" } };

        for (final Function<String, String> render : CURL_RENDERERS) {
            for (final String[] pair : equivalent) {
                assertEquals(render.apply("curl " + pair[1] + " " + TARGET), render.apply("curl " + pair[0] + " " + TARGET), pair[0]);
            }
        }
    }

    @Test
    void clusteredCurlFlagsKeepMissingArgumentAndUnsupportedOptionDiagnostics() {
        final String[][] missing = { { "-sSLX", "Missing HTTP method after -X" }, { "-sLH", "Missing header after -H" }, { "-sSd", "Missing data after -d" },
                { "-sLo", "Missing argument after -o" } };

        for (final Function<String, String> render : CURL_RENDERERS) {
            for (final String[] entry : missing) {
                assertEquals(entry[1], assertThrows(IllegalArgumentException.class, () -> render.apply("curl " + TARGET + " " + entry[0])).getMessage());
            }

            for (final String option : List.of("G", "Ffield=value", "Tfile.txt")) {
                final String command = "curl -sSL" + option + " " + TARGET;
                final String message = assertThrows(IllegalArgumentException.class, () -> render.apply(command)).getMessage();
                assertTrue(message.startsWith("Unsupported curl option '-" + option.charAt(0) + "':"), message);
                assertTrue(message.endsWith("Command: " + command), message);
            }
        }
    }
}
