package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class HttpResponseTest extends TestBase {

    private HttpResponse createTestResponse(int statusCode, String message, String body) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        return new HttpResponse("https://api.example.com/test", System.currentTimeMillis() - 1000, System.currentTimeMillis(), statusCode, message, headers,
                body.getBytes(StandardCharsets.UTF_8), ContentFormat.JSON, StandardCharsets.UTF_8);
    }

    @Test
    public void testResponseWithNullBodyFormat() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("text/plain"));

        HttpResponse response = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers, "test".getBytes(StandardCharsets.UTF_8),
                null, // null body format
                StandardCharsets.UTF_8);

        String body = response.body(String.class);
        assertEquals("test", body);
    }

    @Test
    public void testResponseWithNullCharsetDefaultsToUtf8() {
        final byte[] utf8Body = "caf\u00e9".getBytes(StandardCharsets.UTF_8);
        final HttpResponse response = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", Collections.emptyMap(), utf8Body,
                ContentFormat.NONE, null);

        assertEquals("caf\u00e9", response.body(String.class));
        assertEquals("caf\u00e9", response.body(Type.of(String.class)));
    }

    @Test
    public void testResponseWithXMLFormat() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/xml"));

        HttpResponse response = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers,
                "<root>test</root>".getBytes(StandardCharsets.UTF_8), ContentFormat.XML, StandardCharsets.UTF_8);

        String body = response.body(String.class);
        assertEquals("<root>test</root>", body);
    }

    @Test
    public void testResponseWithFORM_URL_ENCODEDFormat() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        HttpResponse response = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers, "key=value".getBytes(StandardCharsets.UTF_8),
                ContentFormat.FORM_URL_ENCODED, StandardCharsets.UTF_8);

        String body = response.body(String.class);
        assertEquals("key=value", body);
    }

    @Test
    public void testMultipleHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));
        headers.put("Cache-Control", Collections.singletonList("no-cache"));
        headers.put("X-Custom-Header", Collections.singletonList("custom-value"));

        HttpResponse response = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers, "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        assertEquals(3, response.headers().size());
        assertEquals("no-cache", response.headers().get("Cache-Control").get(0));
        assertEquals("custom-value", response.headers().get("X-Custom-Header").get(0));
    }

    @Test
    public void testIsSuccessful() {
        HttpResponse response200 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertTrue(response200.isSuccessful());

        HttpResponse response201 = new HttpResponse("http://example.com", 1000L, 2000L, 201, "Created", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertTrue(response201.isSuccessful());

        HttpResponse response299 = new HttpResponse("http://example.com", 1000L, 2000L, 299, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertTrue(response299.isSuccessful());

        HttpResponse response300 = new HttpResponse("http://example.com", 1000L, 2000L, 300, "Multiple Choices", new HashMap<>(), "test".getBytes(),
                ContentFormat.JSON, StandardCharsets.UTF_8);
        assertFalse(response300.isSuccessful());

        HttpResponse response400 = new HttpResponse("http://example.com", 1000L, 2000L, 400, "Bad Request", new HashMap<>(), "test".getBytes(),
                ContentFormat.JSON, StandardCharsets.UTF_8);
        assertFalse(response400.isSuccessful());

        HttpResponse response500 = new HttpResponse("http://example.com", 1000L, 2000L, 500, "Internal Server Error", new HashMap<>(), "test".getBytes(),
                ContentFormat.JSON, StandardCharsets.UTF_8);
        assertFalse(response500.isSuccessful());
    }

    @Test
    public void testRequestUrl() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertEquals("https://api.example.com/test", response.requestUrl());
    }

    @Test
    public void testRequestSentAtMillis() {
        long before = System.currentTimeMillis() - 2000;
        HttpResponse response = createTestResponse(200, "OK", "test");
        long after = System.currentTimeMillis();

        assertTrue(response.requestSentAtMillis() >= before);
        assertTrue(response.requestSentAtMillis() <= after);
    }

    @Test
    public void testResponseReceivedAtMillis() {
        long before = System.currentTimeMillis();
        HttpResponse response = createTestResponse(200, "OK", "test");
        long after = System.currentTimeMillis() + 1000;

        assertTrue(response.responseReceivedAtMillis() >= before);
        assertTrue(response.responseReceivedAtMillis() <= after);
    }

    @Test
    public void testTimestampRelationship() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertTrue(response.responseReceivedAtMillis() >= response.requestSentAtMillis());
    }

    @Test
    public void testStatusCode() {
        assertEquals(200, createTestResponse(200, "OK", "test").statusCode());
        assertEquals(404, createTestResponse(404, "Not Found", "error").statusCode());
    }

    @Test
    public void testMessage() {
        assertEquals("OK", createTestResponse(200, "OK", "test").message());
        assertEquals("Not Found", createTestResponse(404, "Not Found", "error").message());
    }

    @Test
    public void testHeaders() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        Map<String, List<String>> headers = response.headers();

        assertNotNull(headers);
        assertTrue(headers.containsKey("Content-Type"));
        assertEquals("application/json", headers.get("Content-Type").get(0));
    }

    @Test
    public void testBodyWithType() {
        String testBody = "test response body";
        HttpResponse response = createTestResponse(200, "OK", testBody);

        Type<String> stringType = Type.of(String.class);
        String body = response.body(stringType);
        assertEquals(testBody, body);
    }

    @Test
    public void testBody() {
        String testBody = "test body content";
        byte[] body = testBody.getBytes(StandardCharsets.UTF_8);
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), body, ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertArrayEquals(body, response.body());
        assertEquals(testBody, response.body(String.class));
        assertArrayEquals(body, response.body(byte[].class));
        assertArrayEquals(body, response.body(Type.of(byte[].class)));
    }

    @Test
    public void testBodyAsJsonObject() {
        String json = "{\"name\":\"John\",\"age\":30}";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), json.getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        Map<String, Object> result = response.body(Map.class);
        assertEquals("John", result.get("name"));
        assertEquals(30, ((Number) result.get("age")).intValue());
    }

    @Test
    public void testBodyWithFORM_URL_ENCODED() {
        String formData = "name=John&age=30";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), formData.getBytes(StandardCharsets.UTF_8),
                ContentFormat.FORM_URL_ENCODED, StandardCharsets.UTF_8);

        Map<String, String> result = response.body(Map.class);
        assertEquals("John", result.get("name"));
        assertEquals("30", result.get("age"));
    }

    @Test
    public void testFormUrlEncodedBodyUsesResponseCharsetForPercentEscapes() {
        String formData = "name=caf%E9";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), formData.getBytes(StandardCharsets.ISO_8859_1),
                ContentFormat.FORM_URL_ENCODED, StandardCharsets.ISO_8859_1);

        Map<String, String> byClass = response.body(Map.class);
        Map<String, String> byType = response.body(Type.of("Map<String, String>"));

        assertEquals("café", byClass.get("name"));
        assertEquals("café", byType.get("name"));
    }

    @Test
    public void testBodyReturnsDefensiveCopies() {
        byte[] rawBody = "test response body".getBytes(StandardCharsets.UTF_8);
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), rawBody, ContentFormat.NONE,
                StandardCharsets.UTF_8);

        rawBody[0] = 'X';
        assertEquals("test response body", response.body(String.class));

        byte[] body = response.body();
        body[0] = 'Y';
        assertEquals("test response body", response.body(String.class));

        byte[] bodyByClass = response.body(byte[].class);
        byte[] bodyByType = response.body(Type.of(byte[].class));

        assertNotSame(bodyByClass, bodyByType);
        assertArrayEquals("test response body".getBytes(StandardCharsets.UTF_8), bodyByClass);
        assertArrayEquals("test response body".getBytes(StandardCharsets.UTF_8), bodyByType);
    }

    @Test
    public void testHeadersReturnDefensiveCopy() {
        Map<String, List<String>> headers = new HashMap<>();
        List<String> contentTypes = new ArrayList<>();
        contentTypes.add("application/json");
        headers.put("Content-Type", contentTypes);

        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        contentTypes.set(0, "text/plain");
        headers.put("X-Added", Collections.singletonList("new"));

        assertEquals(1, response.headers().size());
        assertEquals("application/json", response.headers().get("Content-Type").get(0));
        assertThrows(UnsupportedOperationException.class, () -> response.headers().put("X-Test", Collections.singletonList("value")));
        assertThrows(UnsupportedOperationException.class, () -> response.headers().get("Content-Type").add("text/xml"));
    }

    @Test
    public void testResponseWithEmptyBody() {
        HttpResponse response = createTestResponse(204, "No Content", "");
        byte[] body = response.body();
        assertNotNull(body);
        assertEquals(0, body.length);
    }

    // M12: headers()/body() return empty (never null) when absent.

    @Test
    public void testHeadersNeverNull() {
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", null, "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.NONE, StandardCharsets.UTF_8);

        assertNotNull(response.headers());
        assertTrue(response.headers().isEmpty());
        // Still unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> response.headers().put("X", Collections.singletonList("v")));
    }

    @Test
    public void testBodyNeverNull() {
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), null, ContentFormat.NONE,
                StandardCharsets.UTF_8);

        assertNotNull(response.body());
        assertEquals(0, response.body().length);
        // The typed body(...) accessors still return null when there was no body.
        assertNull(response.body(String.class));
        assertNull(response.body(byte[].class));
    }

    @Test
    public void testBodyWithXml() {
        String xml = "<user><name>John</name><age>30</age></user>";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), xml.getBytes(StandardCharsets.UTF_8),
                ContentFormat.XML, StandardCharsets.UTF_8);

        Map<String, Object> result = response.body(Map.class);
        assertNotNull(result);
    }

    @Test
    public void testBody_NullResultType() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertThrows(IllegalArgumentException.class, () -> response.body((Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> response.body((Type<?>) null));
    }

    @Test
    public void testHashCode() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertEquals(response.hashCode(), response.hashCode());
    }

    @Test
    public void testEquals() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        HttpResponse response1 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        HttpResponse response2 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);

        assertEquals(response1, response2);
        assertEquals(response1, response1);

        HttpResponse response3 = new HttpResponse("http://different.com", 1000L, 2000L, 200, "OK", headers, "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertNotEquals(response1, response3);

        HttpResponse response4 = new HttpResponse("http://example.com", 1000L, 2000L, 404, "OK", headers, "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertNotEquals(response1, response4);

        HttpResponse response5 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "Not OK", headers, "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertNotEquals(response1, response5);

        HttpResponse response6 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertNotEquals(response1, response6);

        HttpResponse response7 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, "different".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertNotEquals(response1, response7);

        HttpResponse response8 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, "test".getBytes(), ContentFormat.XML,
                StandardCharsets.UTF_8);
        assertNotEquals(response1, response8);

        assertNotEquals(response1, "not a response");
        assertNotEquals(response1, null);
    }

    @Test
    public void testEqualsAndHashCodeIncludeResponseCharset() {
        final Map<String, List<String>> headers = Collections.singletonMap("Content-Type", Collections.singletonList("text/plain"));
        final byte[] body = "test".getBytes(StandardCharsets.UTF_8);
        final HttpResponse utf8 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, body, ContentFormat.NONE, StandardCharsets.UTF_8);
        final HttpResponse sameUtf8 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, body, ContentFormat.NONE,
                StandardCharsets.UTF_8);
        final HttpResponse latin1 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, body, ContentFormat.NONE,
                StandardCharsets.ISO_8859_1);

        assertEquals(utf8, sameUtf8);
        assertEquals(utf8.hashCode(), sameUtf8.hashCode());
        assertNotEquals(utf8, latin1);
        assertNotEquals(utf8.hashCode(), latin1.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeNullVsEmptyNormalized() {
        // headers() normalizes null -> empty map and body() normalizes null -> empty array,
        // so equals()/hashCode() must treat null and empty as equivalent.
        HttpResponse nullHeaders = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", null, "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);
        HttpResponse emptyHeaders = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        assertEquals(nullHeaders, emptyHeaders);
        assertEquals(emptyHeaders, nullHeaders);
        assertEquals(nullHeaders.hashCode(), emptyHeaders.hashCode());

        // Same for the body: null body vs empty byte array.
        HttpResponse nullBody = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), null, ContentFormat.JSON,
                StandardCharsets.UTF_8);
        HttpResponse emptyBody = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), new byte[0], ContentFormat.JSON,
                StandardCharsets.UTF_8);

        assertEquals(nullBody, emptyBody);
        assertEquals(emptyBody, nullBody);
        assertEquals(nullBody.hashCode(), emptyBody.hashCode());
    }

    @Test
    public void testToString() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        String str = response.toString();

        assertNotNull(str);
        assertTrue(str.contains("HttpResponse"));
        assertTrue(str.contains("statusCode=200"));
        assertTrue(str.contains("message=OK"));
        assertTrue(str.contains("url=https://api.example.com/test"));
        assertTrue(str.contains("elapsedTime"));
    }
}
