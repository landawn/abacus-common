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
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class HARUtilTest extends TestBase {

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

    // --- sendRequest(File, String) ---

    @Test
    public void testSendRequest_FileAndString() {
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(tempHARFile, "https://api.example.com/nonexistent"));
    } // --- sendRequest(File, Predicate) ---

    @Test
    public void testSendRequest_FileAndPredicate() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(tempHARFile, filter));
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

    // --- sendRequest(String, String) ---

    @Test
    public void testSendRequest_StringAndString() {
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(sampleHAR, "https://api.example.com/nonexistent"));
    } // --- sendRequest(String, Predicate) ---

    @Test
    public void testSendRequest_StringAndPredicate() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        assertThrows(RuntimeException.class, () -> HARUtil.sendRequest(sampleHAR, filter));
    } // --- sendRequests(File, Predicate) ---

    @Test
    public void testSendRequests_FileAndPredicate() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        List<String> results = HARUtil.sendRequests(tempHARFile, filter);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSendMultiRequestsByHARWithFile() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        List<String> results = HARUtil.sendRequests(tempHARFile, filter);
        assertTrue(results.isEmpty());
    }

    // --- sendRequests(String, Predicate) ---

    @Test
    public void testSendRequests_StringAndPredicate() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        List<String> results = HARUtil.sendRequests(sampleHAR, filter);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSendMultiRequestsByHARWithString() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        List<String> results = HARUtil.sendRequests(sampleHAR, filter);
        assertTrue(results.isEmpty());
    }

    // --- streamRequests(File, Predicate) ---

    @Test
    public void testStreamRequests_FileAndPredicate() {
        Predicate<String> filter = url -> url.contains("/users");
        Stream<Tuple.Tuple2<Map<String, Object>, HttpResponse>> stream = HARUtil.streamRequests(tempHARFile, filter);
        assertNotNull(stream);
    } // --- streamRequests(String, Predicate) ---

    @Test
    public void testStreamRequests_StringAndPredicate() {
        Predicate<String> filter = url -> url.contains("/users");
        Stream<Tuple.Tuple2<Map<String, Object>, HttpResponse>> stream = HARUtil.streamRequests(sampleHAR, filter);
        assertNotNull(stream);
    } // --- sendRequestByRequestEntry ---

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

    // --- findRequestEntry(File, Predicate) ---

    @Test
    public void testFindRequestEntry_File() {
        Predicate<String> filter = url -> url.contains("/users");
        Optional<Map<String, Object>> entry = HARUtil.findRequestEntry(tempHARFile, filter);
        assertTrue(entry.isPresent());
        assertEquals("https://api.example.com/users", entry.get().get("url"));
    }

    @Test
    public void testGetRequestEntryByUrlFromHARWithFile() {
        Predicate<String> filter = url -> url.contains("/users");
        Optional<Map<String, Object>> entry = HARUtil.findRequestEntry(tempHARFile, filter);
        assertTrue(entry.isPresent());
    }

    // --- findRequestEntry(String, Predicate) ---

    @Test
    public void testFindRequestEntry_String() {
        Predicate<String> filter = url -> url.contains("/users");
        Optional<Map<String, Object>> entry = HARUtil.findRequestEntry(sampleHAR, filter);
        assertTrue(entry.isPresent());
        assertEquals("https://api.example.com/users", entry.get().get("url"));
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
    public void testFindRequestEntry_File_NoMatch() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        Optional<Map<String, Object>> entry = HARUtil.findRequestEntry(tempHARFile, filter);
        assertFalse(entry.isPresent());
    }

    @Test
    public void testFindRequestEntry_String_NoMatch() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        Optional<Map<String, Object>> entry = HARUtil.findRequestEntry(sampleHAR, filter);
        assertFalse(entry.isPresent());
    }

    @Test
    public void testGetRequestEntryByUrlFromHARWithString() {
        Predicate<String> filter = url -> url.contains("/users");
        Optional<Map<String, Object>> entry = HARUtil.findRequestEntry(sampleHAR, filter);
        assertTrue(entry.isPresent());

        filter = url -> url.contains("/nonexistent");
        entry = HARUtil.findRequestEntry(sampleHAR, filter);
        assertFalse(entry.isPresent());
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
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("url", "https://api.example.com/users");

        String url = HARUtil.getRequestUrl(requestEntry);
        assertEquals("https://api.example.com/users", url);
    }

    @Test
    public void testGetRequestUrl_WithQueryParams() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("url", "https://api.example.com/search?q=test&limit=10");

        String url = HARUtil.getRequestUrl(requestEntry);
        assertEquals("https://api.example.com/search?q=test&limit=10", url);
    } // --- getHttpMethodByRequestEntry ---

    @Test
    public void testGetHttpMethodByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "POST");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.POST, method);
    }

    @Test
    public void testGetHttpMethodByRequestEntry_GET() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "GET");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.GET, method);
    }

    @Test
    public void testGetHttpMethodByRequestEntryDelete() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "DELETE");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.DELETE, method);
    }

    @Test
    public void testGetHttpMethodByRequestEntryHead() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "HEAD");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.HEAD, method);
    }

    @Test
    public void testGetHttpMethodByRequestEntryOptions() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "OPTIONS");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.OPTIONS, method);
    }

    @Test
    public void testGetHttpMethodByRequestEntryPatch() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "PATCH");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.PATCH, method);
    }

    @Test
    public void testGetHttpMethodByRequestEntryLowercase() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "get");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.GET, method);
    }

    @Test
    public void testGetHttpMethodByRequestEntryMixedCase() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "PuT");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.PUT, method);
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
    public void testGetHeadersByRequestEntrySkipsNullEntry() {
        final Map<String, Object> requestEntry = new HashMap<>();
        final List<Map<String, String>> headersList = new ArrayList<>();
        headersList.add(null);
        headersList.add(Map.of("value", "missing-name"));
        headersList.add(Map.of("name", "Accept", "value", "application/json"));
        requestEntry.put("headers", headersList);

        final HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);

        assertEquals(1, headers.headerNames().size());
        assertEquals("application/json", headers.get("Accept"));
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
        Map<String, Object> requestEntry = new HashMap<>();

        Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertNotNull(result);
        assertEquals(null, result._1);
        assertEquals(null, result._2);
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
    public void testGetBodyAndMimeTypeByRequestEntryEmpty() {
        Map<String, Object> requestEntry = new HashMap<>();

        Tuple.Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertNull(result._1);
        assertNull(result._2);
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
    public void testSendRequestWithNoEntries() {
        String harWithoutEntries = "{\"log\": {}}";

        assertThrows(IllegalArgumentException.class, () -> {
            HARUtil.sendRequest(harWithoutEntries, url -> true);
        });
    }

    @Test
    public void testSendRequestWithEmptyEntries() {
        String harWithEmptyEntries = "{\"log\": {\"entries\": []}}";

        assertThrows(IllegalArgumentException.class, () -> {
            HARUtil.sendRequest(harWithEmptyEntries, url -> true);
        });
    }

    @Test
    public void testSendRequestsWithNoEntries() {
        String harWithoutEntries = "{\"log\": {}}";

        List<String> result = HARUtil.sendRequests(harWithoutEntries, url -> true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSendRequestsWithEmptyEntries() {
        String harWithEmptyEntries = "{\"log\": {\"entries\": []}}";

        List<String> result = HARUtil.sendRequests(harWithEmptyEntries, url -> true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStreamRequestsWithNoEntries() {
        String harWithoutEntries = "{\"log\": {}}";

        var result = HARUtil.streamRequests(harWithoutEntries, url -> true);
        assertNotNull(result);
        assertTrue(result.toList().isEmpty());
    }

    @Test
    public void testStreamRequestsWithEmptyEntries() {
        String harWithEmptyEntries = "{\"log\": {\"entries\": []}}";

        var result = HARUtil.streamRequests(harWithEmptyEntries, url -> true);
        assertNotNull(result);
        assertTrue(result.toList().isEmpty());
    }

    @Test
    public void testFindRequestEntryWithNoEntries() {
        String harWithoutEntries = "{\"log\": {}}";

        Optional<Map<String, Object>> result = HARUtil.findRequestEntry(harWithoutEntries, url -> true);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindRequestEntrySkipsMalformedEntries() {
        String har = "{\"log\":{\"entries\":[null, {}, {\"request\":\"not-an-object\"}, {\"request\":{}},"
                + "{\"request\":{\"method\":\"GET\",\"url\":\"https://api.example.com/valid\",\"headers\":[]}}]}}";

        Optional<Map<String, Object>> result = HARUtil.findRequestEntry(har, url -> url.endsWith("/valid"));

        assertTrue(result.isPresent());
        assertEquals("https://api.example.com/valid", result.get().get("url"));
    }

    @Test
    public void testFindRequestEntryWithEmptyEntries() {
        String harWithEmptyEntries = "{\"log\": {\"entries\": []}}";

        Optional<Map<String, Object>> result = HARUtil.findRequestEntry(harWithEmptyEntries, url -> true);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

}
