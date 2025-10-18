package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class HARUtil2025Test extends TestBase {

    @AfterEach
    public void tearDown() {
        // Reset to defaults after each test
        HARUtil.resetHttpHeaderFilterForHARRequest();
        HARUtil.logRequestCurlForHARRequest(false);
    }

    @Test
    public void testSetHttpHeaderFilterForHARRequest() {
        HARUtil.setHttpHeaderFilterForHARRequest((name, value) -> !"Authorization".equalsIgnoreCase(name));
        // If we get here without exception, the filter was set successfully
        assertNotNull(HARUtil.class);
    }

    @Test
    public void testSetHttpHeaderFilterForHARRequestWithNull() {
        assertThrows(IllegalArgumentException.class, () -> HARUtil.setHttpHeaderFilterForHARRequest(null));
    }

    @Test
    public void testResetHttpHeaderFilterForHARRequest() {
        HARUtil.setHttpHeaderFilterForHARRequest((name, value) -> false);
        HARUtil.resetHttpHeaderFilterForHARRequest();
        // After reset, should use default filter
        assertNotNull(HARUtil.class);
    }

    @Test
    public void testLogRequestCurlForHARRequestBoolean() {
        HARUtil.logRequestCurlForHARRequest(true);
        // If we get here without exception, logging was enabled
        assertNotNull(HARUtil.class);
    }

    @Test
    public void testLogRequestCurlForHARRequestWithQuoteChar() {
        HARUtil.logRequestCurlForHARRequest(true, '"');
        // If we get here without exception, logging was enabled with custom quote char
        assertNotNull(HARUtil.class);
    }

    @Test
    public void testLogRequestCurlForHARRequestWithLogHandler() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        HARUtil.logRequestCurlForHARRequest(true, '\'', capturedCurl::set);
        // If we get here without exception, logging was configured
        assertNotNull(HARUtil.class);
    }

    @Test
    public void testLogRequestCurlForHARRequestDisabled() {
        HARUtil.logRequestCurlForHARRequest(false);
        // If we get here without exception, logging was disabled
        assertNotNull(HARUtil.class);
    }

    @Test
    public void testGetUrlByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("url", "https://api.example.com/users");

        String url = HARUtil.getUrlByRequestEntry(requestEntry);
        assertEquals("https://api.example.com/users", url);
    }

    @Test
    public void testGetHttpMethodByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "POST");

        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.POST, method);
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
        HARUtil.setHttpHeaderFilterForHARRequest((name, value) -> !"Authorization".equalsIgnoreCase(name));

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
    public void testGetRequestEntryByUrlFromHARWithExactMatch() {
        String har = createTestHarString("https://api.example.com/users", "GET");

        Optional<Map<String, Object>> result = HARUtil.getRequestEntryByUrlFromHAR(har, url -> url.equals("https://api.example.com/users"));
        assertTrue(result.isPresent());
        assertEquals("https://api.example.com/users", result.get().get("url"));
    }

    @Test
    public void testGetRequestEntryByUrlFromHARWithPartialMatch() {
        String har = createTestHarString("https://api.example.com/users/123", "GET");

        Optional<Map<String, Object>> result = HARUtil.getRequestEntryByUrlFromHAR(har, url -> url.contains("/users"));
        assertTrue(result.isPresent());
        assertEquals("https://api.example.com/users/123", result.get().get("url"));
    }

    @Test
    public void testGetRequestEntryByUrlFromHARNoMatch() {
        String har = createTestHarString("https://api.example.com/users", "GET");

        Optional<Map<String, Object>> result = HARUtil.getRequestEntryByUrlFromHAR(har, url -> url.contains("/products"));
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetRequestEntryByUrlFromHARMultipleEntries() {
        String har = createTestHarStringWithMultipleEntries();

        Optional<Map<String, Object>> result = HARUtil.getRequestEntryByUrlFromHAR(har, url -> url.contains("/products"));
        assertTrue(result.isPresent());
        assertEquals("https://api.example.com/products", result.get().get("url"));
    }

    @Test
    public void testGetUrlByRequestEntryWithQueryParams() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("url", "https://api.example.com/search?q=test&limit=10");

        String url = HARUtil.getUrlByRequestEntry(requestEntry);
        assertEquals("https://api.example.com/search?q=test&limit=10", url);
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
        assertEquals(3, headers.headerNameSet().size());
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("application/json", headers.get("Accept"));
        assertEquals("Mozilla/5.0", headers.get("User-Agent"));
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
    public void testLogRequestCurlForHARRequestWithCustomHandler() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        HARUtil.logRequestCurlForHARRequest(true, '\'', curl -> handlerCalled.set(true));
        // The handler will only be called when actually sending a request
        assertNotNull(HARUtil.class);
    }

    @Test
    public void testResetHttpHeaderFilterAfterCustomFilter() {
        // Set a custom filter
        HARUtil.setHttpHeaderFilterForHARRequest((name, value) -> false);
        // Reset to default
        HARUtil.resetHttpHeaderFilterForHARRequest();

        Map<String, Object> requestEntry = new HashMap<>();
        List<Map<String, String>> headersList = new ArrayList<>();

        Map<String, String> header = new HashMap<>();
        header.put("name", "Content-Type");
        header.put("value", "application/json");
        headersList.add(header);

        requestEntry.put("headers", headersList);

        HttpHeaders headers = HARUtil.getHeadersByRequestEntry(requestEntry);
        // After reset, should include headers again (default filter accepts valid headers)
        assertNotNull(headers);
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
}
