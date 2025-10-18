package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("2025")
public class HttpResponse2025Test extends TestBase {

    private HttpResponse createTestResponse(int statusCode, String message, String body) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        return new HttpResponse("https://api.example.com/test", System.currentTimeMillis() - 1000, System.currentTimeMillis(), statusCode, message, headers,
                body.getBytes(StandardCharsets.UTF_8), ContentFormat.JSON, StandardCharsets.UTF_8);
    }

    @Test
    public void testIsSuccessfulWith200() {
        HttpResponse response = createTestResponse(200, "OK", "success");
        assertTrue(response.isSuccessful());
    }

    @Test
    public void testIsSuccessfulWith201() {
        HttpResponse response = createTestResponse(201, "Created", "success");
        assertTrue(response.isSuccessful());
    }

    @Test
    public void testIsSuccessfulWith299() {
        HttpResponse response = createTestResponse(299, "Custom Success", "success");
        assertTrue(response.isSuccessful());
    }

    @Test
    public void testIsSuccessfulWith404() {
        HttpResponse response = createTestResponse(404, "Not Found", "error");
        assertFalse(response.isSuccessful());
    }

    @Test
    public void testIsSuccessfulWith500() {
        HttpResponse response = createTestResponse(500, "Internal Server Error", "error");
        assertFalse(response.isSuccessful());
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
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testStatusCode404() {
        HttpResponse response = createTestResponse(404, "Not Found", "error");
        assertEquals(404, response.statusCode());
    }

    @Test
    public void testMessage() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertEquals("OK", response.message());
    }

    @Test
    public void testMessageNotFound() {
        HttpResponse response = createTestResponse(404, "Not Found", "error");
        assertEquals("Not Found", response.message());
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
    public void testBodyRaw() {
        String testBody = "test response body";
        HttpResponse response = createTestResponse(200, "OK", testBody);

        byte[] body = response.body();
        assertNotNull(body);
        assertArrayEquals(testBody.getBytes(StandardCharsets.UTF_8), body);
    }

    @Test
    public void testBodyAsString() {
        String testBody = "test response body";
        HttpResponse response = createTestResponse(200, "OK", testBody);

        String body = response.body(String.class);
        assertEquals(testBody, body);
    }

    @Test
    public void testBodyAsByteArray() {
        String testBody = "test response body";
        HttpResponse response = createTestResponse(200, "OK", testBody);

        byte[] body = response.body(byte[].class);
        assertNotNull(body);
        assertArrayEquals(testBody.getBytes(StandardCharsets.UTF_8), body);
    }

    @Test
    public void testBodyWithNullClass() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertThrows(IllegalArgumentException.class, () -> response.body((Class<?>) null));
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
    public void testBodyWithTypeByteArray() {
        String testBody = "test response body";
        HttpResponse response = createTestResponse(200, "OK", testBody);

        Type<byte[]> byteArrayType = Type.of(byte[].class);
        byte[] body = response.body(byteArrayType);
        assertNotNull(body);
        assertArrayEquals(testBody.getBytes(StandardCharsets.UTF_8), body);
    }

    @Test
    public void testBodyWithNullType() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertThrows(IllegalArgumentException.class, () -> response.body((Type<?>) null));
    }

    @Test
    public void testHashCode() {
        HttpResponse response1 = createTestResponse(200, "OK", "test");
        HttpResponse response2 = createTestResponse(200, "OK", "test");

        assertNotNull(response1.hashCode());
        assertNotNull(response2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        int hashCode1 = response.hashCode();
        int hashCode2 = response.hashCode();
        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testEqualsSameInstance() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertTrue(response.equals(response));
    }

    @Test
    public void testEqualsWithNull() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertFalse(response.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        assertFalse(response.equals("string"));
    }

    @Test
    public void testEqualsDifferentStatusCode() {
        HttpResponse response1 = createTestResponse(200, "OK", "test");
        HttpResponse response2 = createTestResponse(404, "Not Found", "test");
        assertFalse(response1.equals(response2));
    }

    @Test
    public void testEqualsDifferentMessage() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        HttpResponse response1 = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers, "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        HttpResponse response2 = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "Success", headers,
                "test".getBytes(StandardCharsets.UTF_8), ContentFormat.JSON, StandardCharsets.UTF_8);

        assertFalse(response1.equals(response2));
    }

    @Test
    public void testEqualsDifferentUrl() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        HttpResponse response1 = new HttpResponse("https://api.example.com/test1", 1000L, 2000L, 200, "OK", headers, "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        HttpResponse response2 = new HttpResponse("https://api.example.com/test2", 1000L, 2000L, 200, "OK", headers, "test".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        assertFalse(response1.equals(response2));
    }

    @Test
    public void testEqualsDifferentBody() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        HttpResponse response1 = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers, "body1".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        HttpResponse response2 = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers, "body2".getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        assertFalse(response1.equals(response2));
    }

    @Test
    public void testToString() {
        HttpResponse response = createTestResponse(200, "OK", "test");
        String str = response.toString();

        assertNotNull(str);
        assertTrue(str.contains("HttpResponse"));
        assertTrue(str.contains("200"));
        assertTrue(str.contains("OK"));
        assertTrue(str.contains("https://api.example.com/test"));
        assertTrue(str.contains("elapsedTime"));
    }

    @Test
    public void testToStringFormat() {
        HttpResponse response = createTestResponse(404, "Not Found", "error");
        String str = response.toString();

        assertTrue(str.contains("statusCode=404"));
        assertTrue(str.contains("message=Not Found"));
        assertTrue(str.contains("url=https://api.example.com/test"));
    }

    @Test
    public void testResponseWithEmptyBody() {
        HttpResponse response = createTestResponse(204, "No Content", "");
        byte[] body = response.body();
        assertNotNull(body);
        assertEquals(0, body.length);
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
    public void testResponseWithXMLFormat() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/xml"));

        HttpResponse response = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers,
                "<root>test</root>".getBytes(StandardCharsets.UTF_8), ContentFormat.XML, StandardCharsets.UTF_8);

        String body = response.body(String.class);
        assertEquals("<root>test</root>", body);
    }

    @Test
    public void testResponseWithFormUrlEncodedFormat() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        HttpResponse response = new HttpResponse("https://api.example.com/test", 1000L, 2000L, 200, "OK", headers, "key=value".getBytes(StandardCharsets.UTF_8),
                ContentFormat.FormUrlEncoded, StandardCharsets.UTF_8);

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
}
