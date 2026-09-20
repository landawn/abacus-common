package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class HARUtilTest extends TestBase {

    @Test
    public void testReplaySynthesizedFormReplacesCapturedContentType() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            for (final String headerName : new String[] { null, "Content-Type", "cOnTeNt-TyPe" }) {
                for (final String originalText : new String[] { null, "" }) {
                    for (final String capturedType : new String[] { "multipart/form-data; boundary=abc", "application/json",
                            "application/x-www-form-urlencoded; charset=ISO-8859-1" }) {
                        final Map<String, Object> entry = createRequestEntry(server.url("/form").toString());
                        entry.put("method", "POST");
                        entry.put("headers", headerName == null ? List.of() : List.of(Map.of("name", headerName, "value", capturedType)));
                        final Map<String, Object> postData = new HashMap<>();
                        postData.put("text", originalText);
                        postData.put("mimeType", "multipart/form-data");
                        postData.put("params", java.util.Arrays.asList(Map.of("name", "a", "value", "two words"),
                                Map.of("name", "a", "value", "caf\u00e9\u4e2d\ud83d\ude00"), Map.of("name", "empty")));
                        entry.put("postData", postData);
                        server.enqueue(new MockResponse().setBody("ok"));
                        assertEquals("ok", HARUtil.sendRequestByRequestEntry(entry, String.class));
                        final RecordedRequest sent = server.takeRequest();
                        assertEquals(List.of("application/x-www-form-urlencoded; charset=UTF-8"), sent.getHeaders().values("Content-Type"));
                        assertEquals("a=two+words&a=caf%C3%A9%E4%B8%AD%F0%9F%98%80&empty=", sent.getBody().readUtf8());
                    }
                }
            }
        }
    }

    @Test
    public void testReplayPreservesRawOrAbsentBodyHeaders() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            for (final String text : new String[] { null, "", "raw caf\u00e9\u4e2d\ud83d\ude00" }) {
                for (final List<?> params : List.of(List.of())) {
                    final Map<String, Object> entry = createRequestEntry(server.url("/raw").toString());
                    entry.put("method", "POST");
                    entry.put("headers", List.of(Map.of("name", "cOnTeNt-TyPe", "value", "text/plain; charset=UTF-8")));
                    final Map<String, Object> postData = new HashMap<>();
                    postData.put("text", text);
                    postData.put("mimeType", "application/json");
                    postData.put("params", params);
                    entry.put("postData", postData);
                    server.enqueue(new MockResponse().setBody("ok"));
                    assertEquals("ok", HARUtil.sendRequestByRequestEntry(entry, String.class));
                    final RecordedRequest sent = server.takeRequest();
                    assertEquals("text/plain; charset=UTF-8", sent.getHeader("Content-Type"));
                    assertEquals(text == null ? "" : text, sent.getBody().readUtf8());
                }
            }
        }
    }

    @Test
    public void testReplayOriginalTextTakesPrecedenceOverFormParams() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            final Map<String, Object> entry = createRequestEntry(server.url("/original").toString());
            entry.put("method", "POST");
            entry.put("headers", List.of(Map.of("name", "Content-Type", "value", "application/json")));
            entry.put("postData", Map.of("text", "{\"a\":1}", "mimeType", "multipart/form-data", "params", List.of(Map.of("name", "a", "value", "2"))));
            server.enqueue(new MockResponse().setBody("ok"));
            assertEquals("ok", HARUtil.sendRequestByRequestEntry(entry, String.class));
            final RecordedRequest sent = server.takeRequest();
            assertEquals("application/json", sent.getHeader("Content-Type"));
            assertEquals("{\"a\":1}", sent.getBody().readUtf8());
        }
    }

    private String sampleHAR;
    private File tempHARFile;

    @BeforeEach
    public void setUp() throws IOException {
        sampleHAR = "{" + "\"log\": {" + "\"entries\": [" + "{" + "\"request\": {" + "\"method\": \"GET\"," + "\"url\": \"https://api.example.com/users\","
                + "\"headers\": [" + "{" + "\"name\": \"Accept\"," + "\"value\": \"application/json\"" + "}" + "]" + "}" + "}," + "{" + "\"request\": {"
                + "\"method\": \"POST\"," + "\"url\": \"https://api.example.com/orders\"," + "\"headers\": [" + "{" + "\"name\": \"Content-Type\","
                + "\"value\": \"application/json\"" + "}" + "]," + "\"postData\": {" + "\"text\": \"{\\\"item\\\":\\\"test\\\"}\","
                + "\"mimeType\": \"application/json\"" + "}" + "}" + "}" + "]" + "}" + "}";

        tempHARFile = File.createTempFile("test-har", ".har");
        tempHARFile.deleteOnExit();
        Files.write(tempHARFile.toPath(), sampleHAR.getBytes());
    }

    @AfterEach
    public void tearDown() {
        // Reset to defaults after each test
        HARUtil.resetThreadLocalHeaderFilter();
        HARUtil.resetCurlLoggingForCurrentThread();
    }

    // Helper methods to create test HAR data

    private String createTestHarString(String url, String method) {
        return "{" + "\"log\": {" + "\"entries\": [" + "{" + "\"request\": {" + "\"method\": \"" + method + "\"," + "\"url\": \"" + url + "\","
                + "\"headers\": [" + "{" + "\"name\": \"Content-Type\"," + "\"value\": \"application/json\"" + "}" + "]," + "\"postData\": {"
                + "\"text\": \"{}\"," + "\"mimeType\": \"application/json\"" + "}" + "}" + "}" + "]" + "}" + "}";
    }

    private String createTestHarStringWithMultipleEntries() {
        return "{" + "\"log\": {" + "\"entries\": [" + "{" + "\"request\": {" + "\"method\": \"GET\"," + "\"url\": \"https://api.example.com/users\","
                + "\"headers\": []" + "}" + "}," + "{" + "\"request\": {" + "\"method\": \"GET\"," + "\"url\": \"https://api.example.com/products\","
                + "\"headers\": []" + "}" + "}," + "{" + "\"request\": {" + "\"method\": \"POST\"," + "\"url\": \"https://api.example.com/orders\","
                + "\"headers\": []" + "}" + "}" + "]" + "}" + "}";
    }

    private Map<String, Object> createRequestEntry(final String url) {
        final Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "GET");
        requestEntry.put("url", url);
        requestEntry.put("headers", List.of());
        return requestEntry;
    }

    @Test
    public void testResetHttpHeaderFilterAfterCustomFilter() {
        // Set a custom filter
        HARUtil.setThreadLocalHeaderFilter((name, value) -> false);
        // Reset to default
        HARUtil.resetThreadLocalHeaderFilter();

        Map<String, Object> requestEntry = new HashMap<>();
        List<Map<String, String>> headersList = new ArrayList<>();

        Map<String, String> header = new HashMap<>();
        header.put("name", "Content-Type");
        header.put("value", "application/json");
        headersList.add(header);

        requestEntry.put("headers", headersList);

        HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);
        assertEquals("application/json", headers.get("Content-Type"));
    }

    // --- setThreadLocalHeaderFilter ---    @Test
    public void testSetThreadLocalHeaderFilter_Null() {
        assertThrows(IllegalArgumentException.class, () -> HARUtil.setThreadLocalHeaderFilter(null));
    }

    @Test
    public void testConfigureCurlLoggingOverloads() {
        assertDoesNotThrow(() -> {
            HARUtil.configureCurlLoggingForCurrentThread(true);
            HARUtil.configureCurlLoggingForCurrentThread(false);

            HARUtil.configureCurlLoggingForCurrentThread(true, '"');
            HARUtil.configureCurlLoggingForCurrentThread(false, '\'');
        });
    }

    @Test
    public void testConfigureCurlLoggingWithHandler() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ok"));
            final String url = server.url("/curl").toString();
            final AtomicReference<String> capturedCurl = new AtomicReference<>();
            HARUtil.configureCurlLoggingForCurrentThread(true, '"', capturedCurl::set);

            assertEquals("ok", HARUtil.sendRequestByRequestEntry(createRequestEntry(url), String.class));
            assertNotNull(capturedCurl.get());
            assertTrue(capturedCurl.get().contains(url));
        }
    }

    @Test
    public void testConfigureCurlLoggingDisabled() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ok"));
            final AtomicReference<String> capturedCurl = new AtomicReference<>();
            HARUtil.configureCurlLoggingForCurrentThread(false, '\'', capturedCurl::set);

            assertEquals("ok", HARUtil.sendRequestByRequestEntry(createRequestEntry(server.url("/disabled").toString()), String.class));
            assertNull(capturedCurl.get());
        }
    }

    @Test
    public void testResetCurlLoggingForCurrentThread() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ok"));
            final AtomicReference<String> capturedCurl = new AtomicReference<>();
            HARUtil.configureCurlLoggingForCurrentThread(true, '\'', capturedCurl::set);
            HARUtil.resetCurlLoggingForCurrentThread();

            assertEquals("ok", HARUtil.sendRequestByRequestEntry(createRequestEntry(server.url("/reset").toString()), String.class));
            assertNull(capturedCurl.get());
        }
    }

    @Test
    public void testConfigureCurlLoggingRejectsNullHandler() {
        assertThrows(IllegalArgumentException.class, () -> HARUtil.configureCurlLoggingForCurrentThread(true, '\'', null));
    }

    @Test
    public void testSendRequest_MissingUrlOverloads() {
        Predicate<String> missing = url -> url.contains("/nonexistent");
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(tempHARFile, "https://api.example.com/nonexistent"));
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(tempHARFile, missing));
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(sampleHAR, "https://api.example.com/nonexistent"));
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(sampleHAR, missing));
    }

    @Test
    public void testSendRequestByHARWithFileAndFilter() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(200).setBody("users-response"));

            final String url = server.url("/users").toString();
            Files.write(tempHARFile.toPath(), createTestHarString(url, "POST").getBytes());

            assertEquals("users-response", HARUtil.sendRequest(tempHARFile, candidate -> candidate.endsWith("/users")));

            final RecordedRequest recordedRequest = server.takeRequest();
            assertEquals("POST", recordedRequest.getMethod());
            assertEquals("/users", recordedRequest.getPath());
            assertEquals("application/json", recordedRequest.getHeader("Content-Type"));
            assertEquals("{}", recordedRequest.getBody().readUtf8());
        }
    }

    @Test
    public void testSendAndStreamRequestsOverloads() {
        Predicate<String> missing = url -> url.contains("/nonexistent");
        Predicate<String> users = url -> url.contains("/users");
        assertTrue(HARUtil.sendRequests(tempHARFile, missing).isEmpty());
        assertTrue(HARUtil.sendRequests(sampleHAR, missing).isEmpty());
        assertNotNull(HARUtil.streamRequests(tempHARFile, users));
        assertNotNull(HARUtil.streamRequests(sampleHAR, users));
    }

    @Test
    public void testSendRequestByRequestEntry() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(200).setBody("direct-response"));

            Map<String, Object> requestEntry = new HashMap<>();
            requestEntry.put("url", server.url("/test?source=har").toString());
            requestEntry.put("method", "GET");

            List<Map<String, String>> headers = new ArrayList<>();
            Map<String, String> header = new HashMap<>();
            header.put("name", "Accept");
            header.put("value", "application/json");
            headers.add(header);
            requestEntry.put("headers", headers);

            assertEquals("direct-response", HARUtil.sendRequestByRequestEntry(requestEntry, String.class));

            final RecordedRequest recordedRequest = server.takeRequest();
            assertEquals("GET", recordedRequest.getMethod());
            assertEquals("/test?source=har", recordedRequest.getPath());
            assertEquals("application/json", recordedRequest.getHeader("Accept"));
        }
    }

    @Test
    public void testSendRequestByRequestEntryPreservesDeleteBody() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(200).setBody("deleted"));

            final Map<String, Object> requestEntry = new HashMap<>();
            requestEntry.put("url", server.url("/resource?source=har").toString());
            requestEntry.put("method", "DELETE");
            requestEntry.put("headers", List.of(Map.of("name", "Accept", "value", "application/json")));
            requestEntry.put("postData", Map.of("text", "{\"reason\":\"duplicate\"}", "mimeType", "application/json"));

            assertEquals("deleted", HARUtil.sendRequestByRequestEntry(requestEntry, String.class));

            final RecordedRequest recordedRequest = server.takeRequest();
            assertEquals("DELETE", recordedRequest.getMethod());
            assertEquals("/resource?source=har", recordedRequest.getPath());
            assertEquals("application/json", recordedRequest.getHeader("Content-Type"));
            assertEquals("{\"reason\":\"duplicate\"}", recordedRequest.getBody().readUtf8());
        }
    }

    @Test
    public void testSendRequestByRequestEntryPreservesOptionsBody() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(200).setBody("options"));

            final Map<String, Object> requestEntry = new HashMap<>();
            requestEntry.put("url", server.url("/capabilities").toString());
            requestEntry.put("method", "OPTIONS");
            requestEntry.put("headers", List.of());
            requestEntry.put("postData", Map.of("text", "feature=upload", "mimeType", "text/plain"));

            assertEquals("options", HARUtil.sendRequestByRequestEntry(requestEntry, String.class));

            final RecordedRequest recordedRequest = server.takeRequest();
            assertEquals("OPTIONS", recordedRequest.getMethod());
            assertEquals("text/plain", recordedRequest.getHeader("Content-Type"));
            assertEquals("feature=upload", recordedRequest.getBody().readUtf8());
        }
    }

    @Test
    public void testFindRequestEntry_FileAndStringOverloads() {
        Predicate<String> users = url -> url.contains("/users");
        Predicate<String> missing = url -> url.contains("/nonexistent");
        Optional<Map<String, Object>> fromFile = HARUtil.findRequestEntry(tempHARFile, users);
        Optional<Map<String, Object>> fromString = HARUtil.findRequestEntry(sampleHAR, users);
        assertTrue(fromFile.isPresent());
        assertTrue(fromString.isPresent());
        assertEquals("https://api.example.com/users", fromFile.get().get("url"));
        assertEquals("https://api.example.com/users", fromString.get().get("url"));
        assertFalse(HARUtil.findRequestEntry(tempHARFile, missing).isPresent());
        assertFalse(HARUtil.findRequestEntry(sampleHAR, missing).isPresent());
    }

    @Test
    public void testGetRequestEntryByUrlFromHARWithExactMatch() {
        String har = createTestHarString("https://api.example.com/users", "GET");

        Optional<Map<String, Object>> result = HARUtil.findRequestEntry(har, url -> url.equals("https://api.example.com/users"));
        assertTrue(result.isPresent());
        assertEquals("https://api.example.com/users", result.get().get("url"));
    }

    @Test
    public void testGetRequestEntryByUrlFromHARWithPartialMatch() {
        String har = createTestHarString("https://api.example.com/users/123", "GET");

        Optional<Map<String, Object>> result = HARUtil.findRequestEntry(har, url -> url.contains("/users"));
        assertTrue(result.isPresent());
        assertEquals("https://api.example.com/users/123", result.get().get("url"));
    }

    @Test
    public void testGetRequestEntryByUrlFromHARNoMatch() {
        String har = createTestHarString("https://api.example.com/users", "GET");

        Optional<Map<String, Object>> result = HARUtil.findRequestEntry(har, url -> url.contains("/products"));
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetRequestEntryByUrlFromHARMultipleEntries() {
        String har = createTestHarStringWithMultipleEntries();

        Optional<Map<String, Object>> result = HARUtil.findRequestEntry(har, url -> url.contains("/products"));
        assertTrue(result.isPresent());
        assertEquals("https://api.example.com/products", result.get().get("url"));
    }

    // --- getRequestUrl ---

    @Test
    public void testGetRequestUrl() {
        assertEquals("https://api.example.com/users", HARUtil.getRequestUrl(Map.of("url", "https://api.example.com/users")));
        assertEquals("https://api.example.com/search?q=test&limit=10", HARUtil.getRequestUrl(Map.of("url", "https://api.example.com/search?q=test&limit=10")));
    }

    @Test
    public void testGetHttpMethodByRequestEntry() {
        assertEquals(HttpMethod.POST, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "POST")));
        assertEquals(HttpMethod.GET, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "GET")));
        assertEquals(HttpMethod.DELETE, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "DELETE")));
        assertEquals(HttpMethod.HEAD, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "HEAD")));
        assertEquals(HttpMethod.OPTIONS, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "OPTIONS")));
        assertEquals(HttpMethod.PATCH, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "PATCH")));
        assertEquals(HttpMethod.GET, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "get")));
        assertEquals(HttpMethod.PUT, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "PuT")));
    }

    @Test
    public void testGetHttpMethodByRequestEntry_missingMethodThrowsIllegalArgument() {
        Map<String, Object> requestEntry = new HashMap<>();
        // No "method" key - prior implementation NPE'd; should now match the JavaDoc contract.
        assertThrows(IllegalArgumentException.class, () -> HARUtil.getHttpMethodByRequestEntry(requestEntry));
    }

    // --- getHeadersByRequestEntry ---

    @Test
    public void testGetHeadersByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        List<Map<String, String>> headersList = new ArrayList<>();

        Map<String, String> header1 = new HashMap<>();
        header1.put("name", "Content-Type");
        header1.put("value", "application/json");
        headersList.add(header1);

        Map<String, String> header2 = new HashMap<>();
        header2.put("name", "Authorization");
        header2.put("value", "Bearer token123");
        headersList.add(header2);

        requestEntry.put("headers", headersList);

        HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("Bearer token123", headers.get("Authorization"));
    }

    @Test
    public void testGetHeadersByRequestEntryEmpty() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("headers", new ArrayList<>());

        HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);
        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testGetHeadersByRequestEntryWithFilter() {
        HARUtil.setThreadLocalHeaderFilter((name, value) -> !"Authorization".equalsIgnoreCase(name));

        Map<String, Object> requestEntry = new HashMap<>();
        List<Map<String, String>> headersList = new ArrayList<>();

        Map<String, String> header1 = new HashMap<>();
        header1.put("name", "Content-Type");
        header1.put("value", "application/json");
        headersList.add(header1);

        Map<String, String> header2 = new HashMap<>();
        header2.put("name", "Authorization");
        header2.put("value", "Bearer token123");
        headersList.add(header2);

        requestEntry.put("headers", headersList);

        HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals(null, headers.get("Authorization")); // Should be filtered out
    }

    @Test
    public void testGetHeadersByRequestEntryMultipleHeaders() {
        Map<String, Object> requestEntry = new HashMap<>();
        List<Map<String, String>> headersList = new ArrayList<>();

        Map<String, String> header1 = new HashMap<>();
        header1.put("name", "Content-Type");
        header1.put("value", "application/json");
        headersList.add(header1);

        Map<String, String> header2 = new HashMap<>();
        header2.put("name", "Accept");
        header2.put("value", "application/json");
        headersList.add(header2);

        Map<String, String> header3 = new HashMap<>();
        header3.put("name", "User-Agent");
        header3.put("value", "Mozilla/5.0");
        headersList.add(header3);

        requestEntry.put("headers", headersList);

        HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);
        assertNotNull(headers);
        assertEquals(3, headers.headerNames().size());
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("application/json", headers.get("Accept"));
        assertEquals("Mozilla/5.0", headers.get("User-Agent"));
    }

    @Test
    public void testGetHeadersByRequestEntryRejectsNullEntry() {
        final Map<String, Object> requestEntry = new HashMap<>();
        final List<Map<String, String>> headersList = new ArrayList<>();
        headersList.add(null);
        headersList.add(Map.of("value", "missing-name"));
        headersList.add(Map.of("name", "Accept", "value", "application/json"));
        requestEntry.put("headers", headersList);

        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> HARUtil.getHeadersByRequestEntry(requestEntry)).getMessage().contains("request.headers[0]"));
    }

    // --- getBodyAndMimeTypeByRequestEntry ---

    @Test
    public void testGetBodyAndMimeTypeByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        Map<String, String> postData = new HashMap<>();
        postData.put("text", "{\"name\":\"John\"}");
        postData.put("mimeType", "application/json");

        requestEntry.put("postData", postData);

        Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertNotNull(result);
        assertEquals("{\"name\":\"John\"}", result._1);
        assertEquals("application/json", result._2);
    }

    @Test
    public void testGetBodyAndMimeTypeByRequestEntryNoPostData() {
        Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(new HashMap<>());
        assertNull(result._1);
        assertNull(result._2);
    }

    @Test
    public void testGetBodyAndMimeTypeByRequestEntryEmptyText() {
        Map<String, Object> requestEntry = new HashMap<>();
        Map<String, String> postData = new HashMap<>();
        postData.put("text", "");
        postData.put("mimeType", "text/plain");

        requestEntry.put("postData", postData);

        Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertNotNull(result);
        assertEquals("", result._1);
        assertEquals("text/plain", result._2);
    }

    @Test
    public void testReplayPreservesMimeTypeForEmptyBody() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ok"));

            final Map<String, Object> requestEntry = new HashMap<>();
            requestEntry.put("method", "POST");
            requestEntry.put("url", server.url("/empty").toString());
            requestEntry.put("headers", List.of());
            requestEntry.put("postData", Map.of("text", "", "mimeType", "application/json"));

            assertEquals("ok", HARUtil.sendRequestByRequestEntry(requestEntry, String.class));
            assertEquals("application/json", server.takeRequest().getHeader("Content-Type"));
        }
    }

    @Test
    public void testGetBodyAndMimeTypeByRequestEntryWithParams() {
        Map<String, Object> requestEntry = new HashMap<>();
        Map<String, Object> postData = new HashMap<>();
        List<Map<String, String>> params = new ArrayList<>();
        params.add(Map.of("name", "a", "value", "1"));
        params.add(Map.of("name", "b", "value", "two words"));
        postData.put("params", params);
        postData.put("mimeType", "application/x-www-form-urlencoded");

        requestEntry.put("postData", postData);

        Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertNotNull(result);
        assertEquals("a=1&b=two+words", result._1);
        assertEquals("application/x-www-form-urlencoded", result._2);
    }

    @Test
    public void testSynthesizedParamBodyDoesNotRetainIncompatibleMultipartMimeType() {
        Map<String, Object> requestEntry = new HashMap<>();
        Map<String, Object> postData = new HashMap<>();
        postData.put("params", List.of(Map.of("name", "a", "value", "two words")));
        postData.put("mimeType", "multipart/form-data; boundary=missing-from-har");
        requestEntry.put("postData", postData);

        Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);

        assertEquals("a=two+words", result._1);
        assertEquals(HttpHeaders.Values.APPLICATION_URL_ENCODED, result._2);
    }

    @Test
    public void testGetBodyAndMimeTypeByRequestEntryWithXml() {
        Map<String, Object> requestEntry = new HashMap<>();
        Map<String, String> postData = new HashMap<>();
        postData.put("text", "<root><item>data</item></root>");
        postData.put("mimeType", "application/xml");

        requestEntry.put("postData", postData);

        Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertNotNull(result);
        assertEquals("<root><item>data</item></root>", result._1);
        assertEquals("application/xml", result._2);
    }

    @Test
    public void testEmptyHarOverloads() {
        String noEntries = "{\"log\": {}}";
        String emptyEntries = "{\"log\": {\"entries\": []}}";
        for (String har : new String[] { noEntries, emptyEntries }) {
            assertThrows(IllegalArgumentException.class, () -> HARUtil.sendRequest(har, url -> true));
            assertTrue(HARUtil.sendRequests(har, url -> true).isEmpty());
            assertTrue(HARUtil.streamRequests(har, url -> true).toList().isEmpty());
            assertFalse(HARUtil.findRequestEntry(har, url -> true).isPresent());
        }
    }

    @Test
    public void testFindRequestEntryRejectsMalformedEntries() {
        String har = "{\"log\":{\"entries\":[null, {}, {\"request\":\"not-an-object\"}, {\"request\":{}},"
                + "{\"request\":{\"method\":\"GET\",\"url\":\"https://api.example.com/valid\",\"headers\":[]}}]}}";

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> HARUtil.findRequestEntry(har, url -> url.endsWith("/valid")));
        assertTrue(error.getMessage().contains("log.entries[0]"));
    }

    // ==================== a08 F-2: bodies on body-less methods ====================

    @Test
    public void testReplayDropsEmptyBodyOnBodylessMethods() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            for (final String method : new String[] { "GET", "HEAD", "TRACE" }) {
                for (final String mimeType : new String[] { null, "", "application/json" }) {
                    final Map<String, Object> entry = new HashMap<>();
                    entry.put("url", server.url("/items?q=1").toString());
                    entry.put("method", method);
                    entry.put("headers", List.of(Map.of("name", "Accept", "value", "application/json")));
                    final Map<String, Object> postData = new HashMap<>();
                    postData.put("text", "");
                    if (mimeType != null) {
                        postData.put("mimeType", mimeType);
                    }
                    entry.put("postData", postData);
                    server.enqueue(new MockResponse().setResponseCode(200).setBody("HEAD".equals(method) ? "" : "ok"));

                    final HttpResponse response = HARUtil.sendRequestByRequestEntry(entry, HttpResponse.class);
                    assertEquals(200, response.statusCode(), method + " / " + mimeType);

                    final RecordedRequest sent = server.takeRequest();
                    assertEquals(method, sent.getMethod());
                    assertEquals("/items?q=1", sent.getPath());
                    assertEquals("application/json", sent.getHeader("Accept"));
                    assertEquals(0L, sent.getBody().size(), method + " / " + mimeType);
                    assertNull(sent.getHeader("Content-Type"), method + " / " + mimeType);
                }
            }
        }
    }

    @Test
    public void testReplayRejectsNonEmptyBodyOnBodylessMethods() {
        for (final String method : new String[] { "GET", "HEAD", "TRACE", "get" }) {
            for (final String text : new String[] { "{\"q\":1}", " ", "café中😀" }) {
                final Map<String, Object> entry = new HashMap<>();
                entry.put("url", "http://127.0.0.1:9/items");
                entry.put("method", method);
                entry.put("headers", List.of());
                entry.put("postData", Map.of("text", text, "mimeType", "application/json"));

                final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> HARUtil.sendRequestByRequestEntry(entry, String.class),
                        method + " / " + text);
                assertTrue(ex.getMessage().contains(method.toUpperCase(java.util.Locale.ROOT)), ex.getMessage());
                assertTrue(ex.getMessage().contains("cannot send one"), ex.getMessage());
            }

            // A body synthesized from postData.params counts as a body too.
            final Map<String, Object> withParams = new HashMap<>();
            withParams.put("url", "http://127.0.0.1:9/items");
            withParams.put("method", method);
            withParams.put("headers", List.of());
            withParams.put("postData", Map.of("params", List.of(Map.of("name", "a", "value", "1"))));
            assertThrows(IllegalArgumentException.class, () -> HARUtil.sendRequestByRequestEntry(withParams, String.class));
        }
    }

    @Test
    public void testReplayStillAttachesBodiesForBodyMethods() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            for (final String method : new String[] { "POST", "PUT", "DELETE", "OPTIONS" }) {
                for (final String text : new String[] { "", "{\"a\":1}", "café中" }) {
                    final Map<String, Object> entry = new HashMap<>();
                    entry.put("url", server.url("/items").toString());
                    entry.put("method", method);
                    entry.put("headers", List.of());
                    entry.put("postData", Map.of("text", text, "mimeType", "application/json"));
                    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

                    assertEquals("ok", HARUtil.sendRequestByRequestEntry(entry, String.class));

                    final RecordedRequest sent = server.takeRequest();
                    assertEquals(method, sent.getMethod());
                    assertEquals(text, sent.getBody().readUtf8(), method + " / " + text);
                    assertEquals("application/json", sent.getHeader("Content-Type"), method + " / " + text);
                }
            }

            // No postData at all on a body method: nothing is sent and no Content-Type is invented.
            final Map<String, Object> entry = new HashMap<>();
            entry.put("url", server.url("/items").toString());
            entry.put("method", "POST");
            entry.put("headers", List.of());
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            assertEquals("ok", HARUtil.sendRequestByRequestEntry(entry, String.class));
            final RecordedRequest sent = server.takeRequest();
            assertEquals("POST", sent.getMethod());
            assertEquals(0L, sent.getBody().size());
        }
    }

    @Test
    public void testReplayOfPatchEntryIsRejectedByHttpRequest() {
        final Map<String, Object> entry = new HashMap<>();
        entry.put("url", "http://127.0.0.1:9/items/1");
        entry.put("method", "PATCH");
        entry.put("headers", List.of());
        entry.put("postData", Map.of("text", "{\"a\":1}", "mimeType", "application/json"));

        assertThrows(UnsupportedOperationException.class, () -> HARUtil.sendRequestByRequestEntry(entry, String.class));

        final String har = "{\"log\":{\"entries\":[{\"request\":{\"method\":\"PATCH\",\"url\":\"http://127.0.0.1:9/items/1\",\"headers\":[]}}]}}";
        assertThrows(UnsupportedOperationException.class, () -> HARUtil.sendRequest(har, "http://127.0.0.1:9/items/1"));
        assertThrows(UnsupportedOperationException.class, () -> HARUtil.sendRequests(har, url -> true));
        assertThrows(UnsupportedOperationException.class, () -> HARUtil.streamRequests(har, url -> true).toList());
    }

    @Test
    public void testSendRequestsStopsAtTheFirstFailingEntry() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            final String base = server.url("/").toString();
            final String har = "{\"log\":{\"entries\":[" //
                    + "{\"request\":{\"method\":\"GET\",\"url\":\"" + base + "a\",\"headers\":[]}}," //
                    + "{\"request\":{\"method\":\"GET\",\"url\":\"" + base
                    + "b\",\"headers\":[],\"postData\":{\"text\":\"{}\",\"mimeType\":\"application/json\"}}}," //
                    + "{\"request\":{\"method\":\"GET\",\"url\":\"" + base + "c\",\"headers\":[]}}]}}";
            server.enqueue(new MockResponse().setBody("a"));
            server.enqueue(new MockResponse().setBody("c"));

            assertThrows(IllegalArgumentException.class, () -> HARUtil.sendRequests(har, url -> true));
            assertEquals(1, server.getRequestCount());
            assertEquals("/a", server.takeRequest().getPath());

            assertThrows(IllegalArgumentException.class, () -> HARUtil.streamRequests(har, url -> true).toList());
            assertEquals(2, server.getRequestCount());
            assertEquals("/a", server.takeRequest().getPath());
        }
    }

    // ==================== a08 F-6: non-string HAR values ====================

    @Test
    public void testGetHeadersByRequestEntryCoercesNonStringNamesAndValues() {
        final List<Map<String, Object>> headers = new ArrayList<>();
        headers.add(Map.of("name", "X-Num", "value", 5));
        headers.add(Map.of("name", "X-Bool", "value", true));
        headers.add(Map.of("name", "X-Dec", "value", 1.5));
        headers.add(Map.of("name", 7, "value", "seven"));
        headers.add(Map.of("name", "X-Long", "value", 9_000_000_000L));
        final Map<String, Object> entry = new HashMap<>();
        entry.put("headers", headers);

        final HttpHeaders result = HARUtil.getHeadersByRequestEntry(entry);
        assertEquals("5", result.getAsString("X-Num"));
        assertEquals("true", result.getAsString("X-Bool"));
        assertEquals("1.5", result.getAsString("X-Dec"));
        assertEquals("seven", result.getAsString("7"));
        assertEquals("9000000000", result.getAsString("X-Long"));

        // The same shape arriving through JSON parsing.
        final String har = "{\"log\":{\"entries\":[{\"request\":{\"method\":\"GET\",\"url\":\"http://h/\",\"headers\":[{\"name\":\"X-Num\",\"value\":5},"
                + "{\"name\":\"X-Bool\",\"value\":false},{\"name\":\"Accept\",\"value\":\"text/plain\"}]}}]}}";
        final Map<String, Object> parsed = HARUtil.findRequestEntry(har, url -> true).get();
        final HttpHeaders fromJson = HARUtil.getHeadersByRequestEntry(parsed);
        assertEquals("5", fromJson.getAsString("X-Num"));
        assertEquals("false", fromJson.getAsString("X-Bool"));
        assertEquals("text/plain", fromJson.getAsString("Accept"));

        // Null / no-name entries are rejected; a named header may retain a null value.
        final Map<String, Object> nullValue = new HashMap<>();
        nullValue.put("name", "X-Null");
        nullValue.put("value", null);
        assertThrows(IllegalArgumentException.class,
                () -> HARUtil.getHeadersByRequestEntry(Map.of("headers", java.util.Arrays.asList(null, Map.of("value", 3), nullValue))));
        assertEquals(List.of("X-Null"), new ArrayList<>(HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(nullValue))).headerNames()));
    }

    @Test
    public void testGetBodyAndMimeTypeByRequestEntryCoercesNonStringValues() {
        final Object[][] cases = { { 42, "text/plain", "42", "text/plain" }, { true, "text/plain", "true", "text/plain" }, { 1.5, 7, "1.5", "7" },
                { "{\"a\":1}", "application/json", "{\"a\":1}", "application/json" }, { 0L, null, "0", null } };

        for (final Object[] entry : cases) {
            final Map<String, Object> postData = new HashMap<>();
            postData.put("text", entry[0]);
            postData.put("mimeType", entry[1]);
            final Map<String, Object> requestEntry = new HashMap<>();
            requestEntry.put("postData", postData);

            final Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
            assertEquals(entry[2], result._1, String.valueOf(entry[0]));
            assertEquals(entry[3], result._2, String.valueOf(entry[1]));
        }

        // Absent postData is still (null, null).
        final Tuple2<String, String> absent = HARUtil.getBodyAndMimeTypeByRequestEntry(new HashMap<>());
        assertNull(absent._1);
        assertNull(absent._2);
    }

    @Test
    public void testReplaySendsCoercedNumericBody() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setBody("ok"));

            final Map<String, Object> entry = new HashMap<>();
            entry.put("url", server.url("/n").toString());
            entry.put("method", "POST");
            entry.put("headers", List.of(Map.of("name", "X-Num", "value", 5)));
            entry.put("postData", Map.of("text", 42, "mimeType", "text/plain"));

            assertEquals("ok", HARUtil.sendRequestByRequestEntry(entry, String.class));

            final RecordedRequest sent = server.takeRequest();
            assertEquals("42", sent.getBody().readUtf8());
            assertEquals("5", sent.getHeader("X-Num"));
            assertEquals("text/plain", sent.getHeader("Content-Type"));
        }
    }

    @Test
    public void reviewFixes20260908_malformedHarContainersAreRejectedWithoutClassCastException() {
        for (Object headers : List.of(Map.of("X-Foo", "1"), "X-Foo: 1", 7, List.of("junk"))) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> HARUtil.getHeadersByRequestEntry(Map.of("headers", headers))).getMessage()
                    .contains("request.headers"));
        }
        for (String har : List.of("{\"log\":{\"entries\":{}}}", "{\"log\":{\"entries\":[\"junk\"]}}")) {
            assertThrows(IllegalArgumentException.class, () -> HARUtil.sendRequest(har, url -> true));
            assertThrows(IllegalArgumentException.class, () -> HARUtil.sendRequests(har, url -> true));
            assertThrows(IllegalArgumentException.class, () -> HARUtil.streamRequests(har, url -> true).count());
            assertThrows(IllegalArgumentException.class, () -> HARUtil.findRequestEntry(har, url -> true));
        }
        for (Object params : List.of("a=1", List.of("junk"))) {
            assertTrue(
                    assertThrows(IllegalArgumentException.class, () -> HARUtil.getBodyAndMimeTypeByRequestEntry(Map.of("postData", Map.of("params", params))))
                            .getMessage()
                            .contains("request.postData.params"));
        }
    }

}
