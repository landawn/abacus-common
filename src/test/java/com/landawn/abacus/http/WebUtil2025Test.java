package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class WebUtil2025Test extends TestBase {

    @Test
    public void testCurl2HttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequestWithPost() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".post(requestBody);"));
        assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2HttpRequestWithHeaders() {
        String curl = "curl -H \"Content-Type: application/json\" -H \"Authorization: Bearer token\" https://api.example.com/users";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token\")"));
    }

    @Test
    public void testCurl2HttpRequestWithPut() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d '{\"name\":\"Jane\"}'";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".put(requestBody);"));
    }

    @Test
    public void testCurl2HttpRequestWithDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2HttpRequestWithHead() {
        String curl = "curl -I https://api.example.com/users";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.HEAD)"));
    }

    @Test
    public void testCurl2HttpRequestWithDataInfersPost() {
        String curl = "curl https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".post(requestBody);"));
    }

    @Test
    public void testCurl2HttpRequestWithDataRaw() {
        String curl = "curl --data-raw '{\"key\":\"value\"}' https://api.example.com/data";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".post(requestBody);"));
    }

    @Test
    public void testCurl2HttpRequestWithLongForm() {
        String curl = "curl --request POST --header \"Content-Type: application/json\" https://api.example.com/users";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".post("));
    }

    @Test
    public void testCurl2HttpRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequest(null));
    }

    @Test
    public void testCurl2HttpRequestWithEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequest(""));
    }

    @Test
    public void testCurl2HttpRequestWithInvalidStartThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequest("wget https://example.com"));
    }

    @Test
    public void testCurl2OkHttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.curlToOkHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPost() {
        String curl = "curl -X POST -H \"Content-Type: application/json\" https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToOkHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".post();"));
        assertTrue(result.contains("RequestBody requestBody"));
        assertTrue(result.contains("MediaType.parse(\"application/json\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithHeaders() {
        String curl = "curl -H \"Accept: application/json\" https://api.example.com/data";
        String result = WebUtil.curlToOkHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPut() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d 'data'";
        String result = WebUtil.curlToOkHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".put();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.curlToOkHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithBody() {
        String curl = "curl -H \"Content-Type: text/plain\" -d 'Hello World' https://api.example.com/echo";
        String result = WebUtil.curlToOkHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains("MediaType.parse(\"text/plain\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequest(null));
    }

    @Test
    public void testCurl2OkHttpRequestWithEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequest(""));
    }

    @Test
    public void testCreateOkHttpRequestForCurlWithLogHandler() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", capturedCurl::set);

        assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurlWithQuoteChar() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", '"', capturedCurl::set);

        assertNotNull(request);
    }

    @Test
    public void testBuildCurlWithGetMethod() {
        String result = WebUtil.buildCurl("GET", "https://api.example.com/users", null, null, null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X GET"));
        assertTrue(result.contains("'https://api.example.com/users'"));
    }

    @Test
    public void testBuildCurlWithPostMethod() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        String result = WebUtil.buildCurl("POST", "https://api.example.com/users", headers, "{\"name\":\"John\"}", "application/json", '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X POST"));
        assertTrue(result.contains("-H 'Content-Type: application/json'"));
        assertTrue(result.contains("-d '{\"name\":\"John\"}'"));
    }

    @Test
    public void testBuildCurlWithMultipleHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");
        headers.put("Accept", "application/json");

        String result = WebUtil.buildCurl("POST", "https://api.example.com/data", headers, "{}", "application/json", '\'');

        assertNotNull(result);
        assertTrue(result.contains("-H 'Content-Type: application/json'"));
        assertTrue(result.contains("-H 'Authorization: Bearer token123'"));
        assertTrue(result.contains("-H 'Accept: application/json'"));
    }

    @Test
    public void testBuildCurlWithDoubleQuotes() {
        String result = WebUtil.buildCurl("GET", "https://api.example.com/users", null, null, null, '"');

        assertNotNull(result);
        assertTrue(result.contains("\"https://api.example.com/users\""));
    }

    @Test
    public void testBuildCurlWithBodyNoHeaders() {
        String result = WebUtil.buildCurl("POST", "https://api.example.com/data", null, "test data", "text/plain", '\'');

        assertNotNull(result);
        assertTrue(result.contains("-H 'Content-Type: text/plain'"));
        assertTrue(result.contains("-d 'test data'"));
    }

    @Test
    public void testBuildCurlWithBodyAndContentTypeHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml");

        String result = WebUtil.buildCurl("POST", "https://api.example.com/data", headers, "<root/>", "application/json", '\'');

        assertNotNull(result);
        // Should use the header value, not the bodyType
        assertTrue(result.contains("-H 'Content-Type: application/xml'"));
        // Should not add another Content-Type header
        int firstIndex = result.indexOf("Content-Type");
        int lastIndex = result.lastIndexOf("Content-Type");
        assertEquals(firstIndex, lastIndex);   // Only one occurrence
    }

    @Test
    public void testBuildCurlWithEmptyBody() {
        String result = WebUtil.buildCurl("GET", "https://api.example.com/users", null, "", null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X GET"));
        assertTrue(result.contains("'https://api.example.com/users'"));
        assertTrue(!result.contains("-d"));
    }

    @Test
    public void testBuildCurlWithQuoteEscaping() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Custom-Header", "value with 'quotes'");

        String result = WebUtil.buildCurl("GET", "https://api.example.com/test", headers, null, null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("Custom-Header"));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWhenNotSet() {
        HttpHeaders headers = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("application/json", headers);

        assertEquals("application/json", headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWhenAlreadySet() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setContentType("text/xml");

        WebUtil.setContentTypeByRequestBodyType("application/json", headers);

        // Should not change existing Content-Type
        assertEquals("text/xml", headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWithEmptyBodyType() {
        HttpHeaders headers = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("", headers);

        // Should not set Content-Type
        assertEquals(null, headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWithNullBodyType() {
        HttpHeaders headers = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType(null, headers);

        // Should not set Content-Type
        assertEquals(null, headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testCurl2HttpRequestWithSpecialCharactersInBody() {
        String curl = "curl -d '{\"message\":\"Hello\\nWorld\"}' https://api.example.com/data";
        String result = WebUtil.curlToHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPatch() {
        String curl = "curl -X PATCH https://api.example.com/users/1 -d '{}'";
        String result = WebUtil.curlToOkHttpRequest(curl);

        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.PATCH)"));
    }

    @Test
    public void testBuildCurlWithPutMethod() {
        String result = WebUtil.buildCurl("PUT", "https://api.example.com/users/1", null, "{\"name\":\"Updated\"}", "application/json", '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X PUT"));
        assertTrue(result.contains("-d"));
    }

    @Test
    public void testBuildCurlWithDeleteMethod() {
        String result = WebUtil.buildCurl("DELETE", "https://api.example.com/users/1", null, null, null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X DELETE"));
    }
}
