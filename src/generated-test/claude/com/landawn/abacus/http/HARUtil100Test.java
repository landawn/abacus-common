package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

import com.landawn.abacus.TestBase;


public class HARUtil100Test extends TestBase {

    private String sampleHAR;
    private File tempHARFile;

    @BeforeEach
    public void setUp() throws IOException {
        sampleHAR = """
            {
                "log": {
                    "entries": [
                        {
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/users",
                                "headers": [
                                    {"name": "Accept", "value": "application/json"},
                                    {"name": "Authorization", "value": "Bearer token123"}
                                ]
                            }
                        },
                        {
                            "request": {
                                "method": "POST",
                                "url": "https://api.example.com/users",
                                "headers": [
                                    {"name": "Content-Type", "value": "application/json"}
                                ],
                                "postData": {
                                    "mimeType": "application/json",
                                    "text": "{\\"name\\":\\"John\\"}"
                                }
                            }
                        }
                    ]
                }
            }
            """;
        
        Path tempPath = Files.createTempFile("test", ".har");
        tempHARFile = tempPath.toFile();
        Files.write(tempPath, sampleHAR.getBytes());
    }

    @AfterEach
    public void tearDown() {
        if (tempHARFile != null && tempHARFile.exists()) {
            tempHARFile.delete();
        }
        HARUtil.resetHttpHeaderFilterForHARRequest();
    }

    @Test
    public void testSetHttpHeaderFilterForHARRequest() {
        BiPredicate<String, String> filter = (name, value) -> !name.equalsIgnoreCase("Authorization");
        HARUtil.setHttpHeaderFilterForHARRequest(filter);
        
        // Test with null filter
        assertThrows(IllegalArgumentException.class, () -> HARUtil.setHttpHeaderFilterForHARRequest(null));
    }

    @Test
    public void testResetHttpHeaderFilterForHARRequest() {
        BiPredicate<String, String> filter = (name, value) -> false;
        HARUtil.setHttpHeaderFilterForHARRequest(filter);
        HARUtil.resetHttpHeaderFilterForHARRequest();
        // No exception should be thrown
    }

    @Test
    public void testLogRequestCurlForHARRequest() {
        HARUtil.logRequestCurlForHARRequest(true);
        HARUtil.logRequestCurlForHARRequest(false);
        
        HARUtil.logRequestCurlForHARRequest(true, '"');
        HARUtil.logRequestCurlForHARRequest(false, '\'');
    }

    @Test
    public void testLogRequestCurlForHARRequestWithHandler() {
        Consumer<String> handler = curl -> System.out.println(curl);
        HARUtil.logRequestCurlForHARRequest(true, '\'', handler);
        HARUtil.logRequestCurlForHARRequest(false, '"', handler);
    }

    @Test
    public void testSendRequestByHARWithFileAndExactUrl() {
        // This would require mocking HTTP requests, so we test the parsing logic
        assertThrows(RuntimeException.class, () -> 
            HARUtil.sendRequestByHAR(tempHARFile, "https://api.example.com/nonexistent"));
    }

    @Test
    public void testSendRequestByHARWithFileAndFilter() {
        Predicate<String> filter = url -> url.contains("/users");
        assertThrows(RuntimeException.class, () -> 
            HARUtil.sendRequestByHAR(tempHARFile, filter));
    }

    @Test
    public void testSendRequestByHARWithStringAndExactUrl() {
        assertThrows(RuntimeException.class, () -> 
            HARUtil.sendRequestByHAR(sampleHAR, "https://api.example.com/nonexistent"));
    }

    @Test
    public void testSendRequestByHARWithStringAndFilter() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        assertThrows(RuntimeException.class, () -> 
            HARUtil.sendRequestByHAR(sampleHAR, filter));
    }

    @Test
    public void testSendMultiRequestsByHARWithFile() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        List<String> results = HARUtil.sendMultiRequestsByHAR(tempHARFile, filter);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSendMultiRequestsByHARWithString() {
        Predicate<String> filter = url -> url.contains("/nonexistent");
        List<String> results = HARUtil.sendMultiRequestsByHAR(sampleHAR, filter);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testStreamMultiRequestsByHARWithFile() {
        Predicate<String> filter = url -> url.contains("/users");
        Stream<Tuple.Tuple2<Map<String, Object>, HttpResponse>> stream = 
            HARUtil.streamMultiRequestsByHAR(tempHARFile, filter);
        assertNotNull(stream);
    }

    @Test
    public void testStreamMultiRequestsByHARWithString() {
        Predicate<String> filter = url -> url.contains("/users");
        Stream<Tuple.Tuple2<Map<String, Object>, HttpResponse>> stream = 
            HARUtil.streamMultiRequestsByHAR(sampleHAR, filter);
        assertNotNull(stream);
    }

    @Test
    public void testSendRequestByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("url", "https://api.example.com/test");
        requestEntry.put("method", "GET");
        
        List<Map<String, String>> headers = new ArrayList<>();
        Map<String, String> header = new HashMap<>();
        header.put("name", "Accept");
        header.put("value", "application/json");
        headers.add(header);
        requestEntry.put("headers", headers);
        
        // This would require mocking HTTP requests
        assertThrows(Exception.class, () -> 
            HARUtil.sendRequestByRequestEntry(requestEntry, String.class));
    }

    @Test
    public void testGetRequestEntryByUrlFromHARWithFile() {
        Predicate<String> filter = url -> url.contains("/users");
        Optional<Map<String, Object>> entry = HARUtil.getRequestEntryByUrlFromHAR(tempHARFile, filter);
        assertTrue(entry.isPresent());
    }

    @Test
    public void testGetRequestEntryByUrlFromHARWithString() {
        Predicate<String> filter = url -> url.contains("/users");
        Optional<Map<String, Object>> entry = HARUtil.getRequestEntryByUrlFromHAR(sampleHAR, filter);
        assertTrue(entry.isPresent());
        
        filter = url -> url.contains("/nonexistent");
        entry = HARUtil.getRequestEntryByUrlFromHAR(sampleHAR, filter);
        assertFalse(entry.isPresent());
    }

    @Test
    public void testGetUrlByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("url", "https://api.example.com/test");
        
        String url = HARUtil.getUrlByRequestEntry(requestEntry);
        assertEquals("https://api.example.com/test", url);
    }

    @Test
    public void testGetHttpMethodByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        requestEntry.put("method", "POST");
        
        HttpMethod method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.POST, method);
        
        requestEntry.put("method", "get");
        method = HARUtil.getHttpMethodByRequestEntry(requestEntry);
        assertEquals(HttpMethod.GET, method);
    }

    @Test
    public void testGetHeadersByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        List<Map<String, String>> headers = new ArrayList<>();
        
        Map<String, String> header1 = new HashMap<>();
        header1.put("name", "Accept");
        header1.put("value", "application/json");
        headers.add(header1);
        
        Map<String, String> header2 = new HashMap<>();
        header2.put("name", "Authorization");
        header2.put("value", "Bearer token");
        headers.add(header2);
        
        requestEntry.put("headers", headers);
        
        HttpHeaders httpHeaders = HARUtil.getHeadersByRequestEntry(requestEntry);
        assertNotNull(httpHeaders);
        assertEquals("application/json", httpHeaders.get("Accept"));
        assertEquals("Bearer token", httpHeaders.get("Authorization"));
    }

    @Test
    public void testGetHeadersByRequestEntryWithFilter() {
        BiPredicate<String, String> filter = (name, value) -> !name.equalsIgnoreCase("Authorization");
        HARUtil.setHttpHeaderFilterForHARRequest(filter);
        
        Map<String, Object> requestEntry = new HashMap<>();
        List<Map<String, String>> headers = new ArrayList<>();
        
        Map<String, String> header1 = new HashMap<>();
        header1.put("name", "Accept");
        header1.put("value", "application/json");
        headers.add(header1);
        
        Map<String, String> header2 = new HashMap<>();
        header2.put("name", "Authorization");
        header2.put("value", "Bearer token");
        headers.add(header2);
        
        requestEntry.put("headers", headers);
        
        HttpHeaders httpHeaders = HARUtil.getHeadersByRequestEntry(requestEntry);
        assertNotNull(httpHeaders);
        assertEquals("application/json", httpHeaders.get("Accept"));
        assertNull(httpHeaders.get("Authorization"));
    }

    @Test
    public void testGetBodyAndMimeTypeByRequestEntry() {
        Map<String, Object> requestEntry = new HashMap<>();
        Map<String, String> postData = new HashMap<>();
        postData.put("text", "{\"name\":\"John\"}");
        postData.put("mimeType", "application/json");
        requestEntry.put("postData", postData);
        
        Tuple.Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertEquals("{\"name\":\"John\"}", result._1);
        assertEquals("application/json", result._2);
    }

    @Test
    public void testGetBodyAndMimeTypeByRequestEntryEmpty() {
        Map<String, Object> requestEntry = new HashMap<>();
        
        Tuple.Tuple2<String, String> result = HARUtil.getBodyAndMimeTypeByRequestEntry(requestEntry);
        assertNull(result._1);
        assertNull(result._2);
    }
}
