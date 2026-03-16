package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;

@Tag("2025")
public class WebUtilTest extends TestBase {

    @Test
    public void testCurl2HttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequestWithPost() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
        assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2HttpRequestWithHeaders() {
        String curl = "curl -H \"Content-Type: application/json\" -H \"Authorization: Bearer token\" https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token\")"));
    }

    @Test
    public void testCurl2HttpRequestWithPut() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d '{\"name\":\"Jane\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".put();"));
    }

    @Test
    public void testCurl2HttpRequestWithDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2HttpRequestWithHead() {
        String curl = "curl -I https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.HEAD)"));
    }

    @Test
    public void testCurl2HttpRequestWithDataInfersPost() {
        String curl = "curl https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestWithDataRaw() {
        String curl = "curl --data-raw '{\"key\":\"value\"}' https://api.example.com/data";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestWithInlineDataOption() {
        String curl = "curl https://api.example.com/users --data='{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestWithLongForm() {
        String curl = "curl --request POST --header \"Content-Type: application/json\" https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".post("));
    }

    @Test
    public void testCurl2HttpRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(null));
    }

    @Test
    public void testCurl2HttpRequestWithEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(""));
    }

    @Test
    public void testCurl2HttpRequestWithInvalidStartThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("wget https://example.com"));
    }

    @Test
    public void testCurl2OkHttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPost() {
        String curl = "curl -X POST -H \"Content-Type: application/json\" https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".post();"));
        assertTrue(result.contains("RequestBody requestBody"));
        assertTrue(result.contains("MediaType.parse(\"application/json\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithHeaders() {
        String curl = "curl -H \"Accept: application/json\" https://api.example.com/data";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPut() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d 'data'";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".put();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithBody() {
        String curl = "curl -H \"Content-Type: text/plain\" -d 'Hello World' https://api.example.com/echo";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains("MediaType.parse(\"text/plain\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithInlineDataOption() {
        String curl = "curl -H \"Content-Type: text/plain\" https://api.example.com/echo --data=Hello";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("RequestBody requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(null));
    }

    @Test
    public void testCurl2OkHttpRequestWithEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(""));
    }

    @Test
    public void testCreateOkHttpRequestForCurlWithLogHandler() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", capturedCurl::set);

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
        // Should use the header value, not the bodyContentType
        assertTrue(result.contains("-H 'Content-Type: application/xml'"));
        // Should not add another Content-Type header
        int firstIndex = result.indexOf("Content-Type");
        int lastIndex = result.lastIndexOf("Content-Type");
        assertEquals(firstIndex, lastIndex); // Only one occurrence
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
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPatch() {
        String curl = "curl -X PATCH https://api.example.com/users/1 -d '{}'";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

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

    @Test
    public void testCurl2HttpRequest() {
        String curl = "curl -X POST https://api.example.com/users -H \"Content-Type: application/json\" -H \"Authorization: Bearer token123\" -d '{\"name\":\"John\",\"age\":30}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
        assertTrue(result.contains("String requestBody = \"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\";"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestGetWithHeaders() {
        String curl = "curl -X GET https://api.example.com/data -H \"Accept: application/json\"";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequestDeleteWithBody() {
        String curl = "curl -X DELETE https://api.example.com/users/123 -d '{\"hard\":true}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2HttpRequestOtherMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        N.println(result);
        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".execute(HttpMethod.PATCH);"));
    }

    @Test
    public void testCurl2HttpRequestWithSingleQuotes() {
        String curl = "curl -X POST 'https://api.example.com/users' -H 'Content-Type: application/json' -d '{\"name\":\"John\"}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
    }

    @Test
    public void testCurl2HttpRequestWithDoubleQuotes() {
        String curl = "curl -X POST \"https://api.example.com/users\" -H \"Content-Type: application/json\"";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
    }

    @Test
    public void testCurl2HttpRequestWithLongHeader() {
        String curl = "curl -X GET https://api.example.com/data --header \"Authorization: Bearer token123\"";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
    }

    @Test
    public void testCurl2HttpRequestInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(""));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(null));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("not a curl command"));
    }

    @Test
    public void testCurl2HttpRequestWithEscapedQuotes() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"message\":\"Hello \\\"World\\\"\"}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2OkHttpRequest() {
        String curl = "curl -X POST https://api.example.com/users -H \"Content-Type: application/json\" -H \"Authorization: Bearer token123\" -d '{\"name\":\"John\",\"age\":30}'";

        String result = WebUtil.curlToOkHttpRequestCode(curl);
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
    public void testCurl2OkHttpRequestOtherMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curlToOkHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.PATCH);"));
    }

    @Test
    public void testCurl2OkHttpRequestWithoutContentType() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";

        String result = WebUtil.curlToOkHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("RequestBody.create(null,"));
    }

    @Test
    public void testCurl2OkHttpRequestInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(""));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(null));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode("not a curl command"));
    }

    @Test
    public void testCreateOkHttpRequestForCurl() {
        Consumer<String> logHandler = curl -> assertNotNull(curl);

        OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", logHandler);
        assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurlWithQuoteChar() {
        Consumer<String> logHandler = curl -> {
            assertNotNull(curl);
            assertTrue(curl.contains("\""));
        };

        OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", '"', logHandler);
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
    public void testParseCurlWithDoubleSlashInPath() {
        String curl = "curl https://api.example.com/a//b";
        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("https://api.example.com/a//b"));
    }

    @Test
    public void testParseCurlWithBackslash() {
        String curl = "curl https://api.example.com/data \\\n-H \"Accept: application/json\"";
        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
    }

    @Test
    public void testParseCurlWithUnmatchedQuote() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John";
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(curl));
    }

    @Test
    public void testCurl2HttpRequestWithHeadLongFormOption() {
        String curl = "curl --head https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.HEAD)"));
    }

    @Test
    public void testCurl2OkHttpRequestWithHeadOption() {
        String curl = "curl -I https://api.example.com/users";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        // HEAD goes through the 'execute(HttpMethod.HEAD)' path in OkHttpRequest code gen
        assertTrue(result.contains(".execute(HttpMethod.HEAD)"));
    }
}
