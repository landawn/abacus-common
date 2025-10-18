package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

@Tag("new-test")
public class HttpResponse100Test extends TestBase {

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
        String url = "https://api.example.com/users";
        HttpResponse response = new HttpResponse(url, 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON, StandardCharsets.UTF_8);
        assertEquals(url, response.requestUrl());
    }

    @Test
    public void testRequestSentAtMillis() {
        long sentAt = 1234567890L;
        HttpResponse response = new HttpResponse("http://example.com", sentAt, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertEquals(sentAt, response.requestSentAtMillis());
    }

    @Test
    public void testResponseReceivedAtMillis() {
        long receivedAt = 9876543210L;
        HttpResponse response = new HttpResponse("http://example.com", 1000L, receivedAt, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertEquals(receivedAt, response.responseReceivedAtMillis());
    }

    @Test
    public void testStatusCode() {
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 404, "Not Found", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertEquals(404, response.statusCode());
    }

    @Test
    public void testMessage() {
        String message = "Created";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 201, message, new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertEquals(message, response.message());
    }

    @Test
    public void testHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Accept", Arrays.asList("text/plain", "application/json"));

        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", headers, "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);

        assertEquals(headers, response.headers());
    }

    @Test
    public void testBody() {
        byte[] body = "test body content".getBytes();
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), body, ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertArrayEquals(body, response.body());
    }

    @Test
    public void testBodyAsString() {
        String bodyContent = "test body content";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), bodyContent.getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);
        assertEquals(bodyContent, response.body(String.class));
    }

    @Test
    public void testBodyAsByteArray() {
        byte[] body = "test body content".getBytes();
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), body, ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertArrayEquals(body, response.body(byte[].class));
    }

    @Test
    public void testBodyWithNullResultClass() {
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> response.body((Class<?>) null));
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
    public void testBodyWithType() {
        String json = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), json.getBytes(StandardCharsets.UTF_8),
                ContentFormat.JSON, StandardCharsets.UTF_8);

        Type<List<Map<String, String>>> type = TypeFactory.getType("List<Map<String, String>>");
        List<Map<String, String>> result = response.body(type);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).get("name"));
        assertEquals("Jane", result.get(1).get("name"));
    }

    @Test
    public void testBodyWithTypeString() {
        String content = "test content";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), content.getBytes(StandardCharsets.UTF_8),
                ContentFormat.NONE, StandardCharsets.UTF_8);

        Type<String> type = TypeFactory.getType("String");
        String result = response.body(type);
        assertEquals(content, result);
    }

    @Test
    public void testBodyWithTypeByteArray() {
        byte[] content = "test content".getBytes();
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), content, ContentFormat.NONE,
                StandardCharsets.UTF_8);

        Type<byte[]> type = TypeFactory.getType("byte[]");
        byte[] result = response.body(type);
        assertArrayEquals(content, result);
    }

    @Test
    public void testBodyWithNullType() {
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> response.body((Type<?>) null));
    }

    @Test
    public void testHashCode() {
        HttpResponse response1 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);
        HttpResponse response2 = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);

        assertEquals(response1.hashCode(), response2.hashCode());
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
    public void testToString() {
        HttpResponse response = new HttpResponse("http://example.com/api", 1000L, 2000L, 200, "OK", new HashMap<>(), "test".getBytes(), ContentFormat.JSON,
                StandardCharsets.UTF_8);

        String str = response.toString();
        assertNotNull(str);
        assertTrue(str.contains("statusCode=200"));
        assertTrue(str.contains("message=OK"));
        assertTrue(str.contains("url=http://example.com/api"));
        assertTrue(str.contains("elapsedTime=1000"));
    }

    @Test
    public void testBodyWithFormUrlEncoded() {
        String formData = "name=John&age=30";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), formData.getBytes(StandardCharsets.UTF_8),
                ContentFormat.FormUrlEncoded, StandardCharsets.UTF_8);

        Map<String, String> result = response.body(Map.class);
        assertEquals("John", result.get("name"));
        assertEquals("30", result.get("age"));
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
    public void testBodyWithNullContentFormat() {
        String content = "test content";
        HttpResponse response = new HttpResponse("http://example.com", 1000L, 2000L, 200, "OK", new HashMap<>(), content.getBytes(StandardCharsets.UTF_8), null,
                StandardCharsets.UTF_8);

        assertEquals(content, response.body(String.class));
    }
}
