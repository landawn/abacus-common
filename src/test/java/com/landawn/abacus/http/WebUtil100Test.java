package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class WebUtil100Test extends TestBase {

    @Test
    public void testCurl2HttpRequest() {
        String curl = "curl -X POST https://api.example.com/users -H \"Content-Type: application/json\" -H \"Authorization: Bearer token123\" -d '{\"name\":\"John\",\"age\":30}'";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
        assertTrue(result.contains("String requestBody = \"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\";"));
        assertTrue(result.contains(".post(requestBody);"));
    }

    @Test
    public void testCurl2HttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/data";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/data\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequestGetWithHeaders() {
        String curl = "curl -X GET https://api.example.com/data -H \"Accept: application/json\"";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequestDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/123";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2HttpRequestPut() {
        String curl = "curl -X PUT https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".put(requestBody);"));
    }

    @Test
    public void testCurl2HttpRequestOtherMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.PATCH, requestBody);"));
    }

    @Test
    public void testCurl2HttpRequestWithSingleQuotes() {
        String curl = "curl -X POST 'https://api.example.com/users' -H 'Content-Type: application/json' -d '{\"name\":\"John\"}'";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
    }

    @Test
    public void testCurl2HttpRequestWithDoubleQuotes() {
        String curl = "curl -X POST \"https://api.example.com/users\" -H \"Content-Type: application/json\"";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
    }

    @Test
    public void testCurl2HttpRequestWithDataRaw() {
        String curl = "curl -X POST https://api.example.com/users --data-raw '{\"test\":true}'";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2HttpRequestWithLongHeader() {
        String curl = "curl -X GET https://api.example.com/data --header \"Authorization: Bearer token123\"";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
    }

    @Test
    public void testCurl2HttpRequestInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curl2HttpRequest(""));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curl2HttpRequest(null));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curl2HttpRequest("not a curl command"));
    }

    @Test
    public void testCurl2HttpRequestWithEscapedQuotes() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"message\":\"Hello \\\"World\\\"\"}'";

        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2OkHttpRequest() {
        String curl = "curl -X POST https://api.example.com/users -H \"Content-Type: application/json\" -H \"Authorization: Bearer token123\" -d '{\"name\":\"John\",\"age\":30}'";

        String result = WebUtil.curl2OkHttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
        assertTrue(result.contains(
                "RequestBody requestBody = RequestBody.create(MediaType.parse(\"application/json\"), \"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\");"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2OkHttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/data";

        String result = WebUtil.curl2OkHttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/data\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2OkHttpRequestDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/123";

        String result = WebUtil.curl2OkHttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2OkHttpRequestPut() {
        String curl = "curl -X PUT https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curl2OkHttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".put();"));
    }

    @Test
    public void testCurl2OkHttpRequestOtherMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curl2OkHttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.PATCH);"));
    }

    @Test
    public void testCurl2OkHttpRequestWithoutContentType() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";

        String result = WebUtil.curl2OkHttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("RequestBody.create(null,"));
    }

    @Test
    public void testCurl2OkHttpRequestInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curl2OkHttpRequest(""));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curl2OkHttpRequest(null));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curl2OkHttpRequest("not a curl command"));
    }

    @Test
    public void testCreateOkHttpRequestForCurl() {
        Consumer<String> logHandler = curl -> assertNotNull(curl);

        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", logHandler);
        assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurlWithQuoteChar() {
        Consumer<String> logHandler = curl -> {
            assertNotNull(curl);
            assertTrue(curl.contains("\""));
        };

        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", '"', logHandler);
        assertNotNull(request);
    }

    @Test
    public void testBuildCurl() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");

        String curl = WebUtil.buildCurl("POST", "https://api.example.com/users", headers, "{\"name\":\"John\"}", "application/json", '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("curl -X POST 'https://api.example.com/users'"));
        assertTrue(curl.contains("-H 'Content-Type: application/json'"));
        assertTrue(curl.contains("-H 'Authorization: Bearer token123'"));
        assertTrue(curl.contains("-d '{\"name\":\"John\"}'"));
    }

    @Test
    public void testBuildCurlWithDoubleQuotes() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        String curl = WebUtil.buildCurl("GET", "https://api.example.com/data", headers, null, null, '"');

        assertNotNull(curl);
        assertTrue(curl.contains("curl -X GET \"https://api.example.com/data\""));
        assertTrue(curl.contains("-H \"Accept: application/json\""));
    }

    @Test
    public void testBuildCurlWithoutBody() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", "*/*");

        String curl = WebUtil.buildCurl("GET", "https://api.example.com/data", headers, null, null, '\'');

        assertNotNull(curl);
        assertFalse(curl.contains("-d"));
    }

    @Test
    public void testBuildCurlWithEmptyHeaders() {
        String curl = WebUtil.buildCurl("POST", "https://api.example.com/users", null, "{\"test\":true}", null, '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("curl -X POST"));
        assertTrue(curl.contains("-d '{\"test\":true}'"));
    }

    @Test
    public void testBuildCurlWithBodyTypeButNoContentTypeHeader() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        String curl = WebUtil.buildCurl("POST", "https://api.example.com/users", headers, "{\"test\":true}", "application/json", '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("-H 'Content-Type: application/json'"));
    }

    @Test
    public void testBuildCurlWithSpecialCharacters() {
        Map<String, Object> headers = new HashMap<>();
        String body = "{\"message\":\"Hello 'World'\"}";

        String curl = WebUtil.buildCurl("POST", "https://api.example.com/users", headers, body, null, '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("-d"));
        // The body should be properly escaped
    }

    @Test
    public void testSetContentTypeByRequestBodyType() {
        HttpHeaders headers = HttpHeaders.create();

        WebUtil.setContentTypeByRequestBodyType("application/json", headers);
        assertEquals("application/json", headers.get(HttpHeaders.Names.CONTENT_TYPE));

        // Should not override existing content type
        headers.clear();
        headers.setContentType("text/plain");
        WebUtil.setContentTypeByRequestBodyType("application/json", headers);
        assertEquals("text/plain", headers.get(HttpHeaders.Names.CONTENT_TYPE));

        // Test with null/empty body type
        headers.clear();
        WebUtil.setContentTypeByRequestBodyType(null, headers);
        assertNull(headers.get(HttpHeaders.Names.CONTENT_TYPE));

        WebUtil.setContentTypeByRequestBodyType("", headers);
        assertNull(headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testParseCurlWithComments() {
        String curl = "curl https://api.example.com/data // this is a comment";
        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains("https://api.example.com/data"));
    }

    @Test
    public void testParseCurlWithBackslash() {
        String curl = "curl https://api.example.com/data \\\n-H \"Accept: application/json\"";
        String result = WebUtil.curl2HttpRequest(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
    }

    @Test
    public void testParseCurlWithUnmatchedQuote() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John";
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curl2HttpRequest(curl));
    }
}
