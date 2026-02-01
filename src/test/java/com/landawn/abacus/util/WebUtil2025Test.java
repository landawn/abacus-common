package com.landawn.abacus.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.http.HttpUtil;
import com.landawn.abacus.http.OkHttpRequest;
import com.landawn.abacus.http.WebUtil;

@Tag("2025")
public class WebUtil2025Test extends TestBase {

    @Test
    public void testCurl2HttpRequest_SimpleGetRequest() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        Assertions.assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequest_PostRequestWithBody() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        Assertions.assertTrue(result.contains(".post("));
        Assertions.assertTrue(result.contains("requestBody"));
        Assertions.assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2HttpRequest_PostWithHeaders() {
        String curl = "curl -X POST https://api.example.com/users " + "-H \"Content-Type: application/json\" " + "-H \"Authorization: Bearer token123\" "
                + "-d '{\"name\":\"John\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Content-Type"));
        Assertions.assertTrue(result.contains("application/json"));
        Assertions.assertTrue(result.contains("Authorization"));
        Assertions.assertTrue(result.contains("Bearer token123"));
    }

    @Test
    public void testCurl2HttpRequest_PutRequest() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d '{\"name\":\"Jane\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".put("));
        Assertions.assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2HttpRequest_DeleteRequest() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2HttpRequest_WithDataOption() {
        String curl = "curl https://api.example.com/users --data '{\"test\":\"value\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".post("));
        Assertions.assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2HttpRequest_WithDataRawOption() {
        String curl = "curl https://api.example.com/users --data-raw '{\"test\":\"value\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2HttpRequest_WithHeaderOption() {
        String curl = "curl https://api.example.com/users --header \"Accept: application/json\"";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Accept"));
        Assertions.assertTrue(result.contains("application/json"));
    }

    @Test
    public void testCurl2HttpRequest_WithRequestOption() {
        String curl = "curl --request GET https://api.example.com/users";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequest_HeadRequest() {
        String curl = "curl -I https://api.example.com/users";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("HttpMethod.HEAD") || result.contains("execute"));
    }

    @Test
    public void testCurl2HttpRequest_CustomHttpMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/1 -d '{\"name\":\"Updated\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("HttpMethod.PATCH"));
        Assertions.assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2HttpRequest_HttpsUrl() {
        String curl = "curl https://secure.example.com/api";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("https://secure.example.com/api"));
    }

    @Test
    public void testCurl2HttpRequest_HttpUrl() {
        String curl = "curl http://example.com/api";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("http://example.com/api"));
    }

    @Test
    public void testCurl2HttpRequest_WithSpecialCharactersInBody() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"message\":\"Hello\\nWorld\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2HttpRequest_WithQuotesInHeader() {
        String curl = "curl https://api.example.com -H 'User-Agent: Mozilla/5.0'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("User-Agent"));
    }

    @Test
    public void testCurl2HttpRequest_MultilineCommand() {
        String curl = "curl -X POST https://api.example.com/users \\\n" + "-H \"Content-Type: application/json\" \\\n" + "-d '{\"name\":\"John\"}'";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Content-Type"));
    }

    @Test
    public void testCurl2HttpRequest_NullInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WebUtil.convertCurlToHttpRequest(null);
        });
    }

    @Test
    public void testCurl2HttpRequest_EmptyInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WebUtil.convertCurlToHttpRequest("");
        });
    }

    @Test
    public void testCurl2HttpRequest_InvalidInputNotStartingWithCurl() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WebUtil.convertCurlToHttpRequest("wget https://example.com");
        });
    }

    @Test
    public void testCurl2HttpRequest_WithMultipleHeaders() {
        String curl = "curl https://api.example.com " + "-H \"Header1: value1\" " + "-H \"Header2: value2\" " + "-H \"Header3: value3\"";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Header1"));
        Assertions.assertTrue(result.contains("Header2"));
        Assertions.assertTrue(result.contains("Header3"));
    }

    @Test
    public void testCurl2HttpRequest_PostWithoutBody() {
        String curl = "curl -X POST https://api.example.com/users";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".post("));
    }

    @Test
    public void testCurl2HttpRequest_PutWithoutBody() {
        String curl = "curl -X PUT https://api.example.com/users/1";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".put("));
    }

    @Test
    public void testCurl2OkHttpRequest_SimpleGetRequest() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        Assertions.assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2OkHttpRequest_PostRequestWithBody() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        Assertions.assertTrue(result.contains(".post();"));
        Assertions.assertTrue(result.contains("RequestBody requestBody"));
    }

    @Test
    public void testCurl2OkHttpRequest_WithContentTypeHeader() {
        String curl = "curl -X POST https://api.example.com/users " + "-H \"Content-Type: application/json\" " + "-d '{\"name\":\"John\"}'";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("MediaType.parse(\"application/json\")"));
        Assertions.assertTrue(result.contains("RequestBody requestBody"));
    }

    @Test
    public void testCurl2OkHttpRequest_PutRequest() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d '{\"name\":\"Jane\"}'";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".put();"));
        Assertions.assertTrue(result.contains(".body(requestBody)"));
    }

    @Test
    public void testCurl2OkHttpRequest_DeleteRequest() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2OkHttpRequest_WithMultipleHeaders() {
        String curl = "curl https://api.example.com " + "-H \"Accept: application/json\" " + "-H \"Authorization: Bearer token\"";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Accept"));
        Assertions.assertTrue(result.contains("Authorization"));
    }

    @Test
    public void testCurl2OkHttpRequest_CustomHttpMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/1";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("HttpMethod.PATCH"));
    }

    @Test
    public void testCurl2OkHttpRequest_HeadRequest() {
        String curl = "curl -I https://api.example.com/users";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("HttpMethod.HEAD") || result.contains("execute"));
    }

    @Test
    public void testCurl2OkHttpRequest_NullInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WebUtil.convertCurlToOkHttpRequest(null);
        });
    }

    @Test
    public void testCurl2OkHttpRequest_EmptyInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WebUtil.convertCurlToOkHttpRequest("");
        });
    }

    @Test
    public void testCurl2OkHttpRequest_InvalidInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WebUtil.convertCurlToOkHttpRequest("not a curl command");
        });
    }

    @Test
    public void testCurl2OkHttpRequest_WithDataOption() {
        String curl = "curl https://api.example.com/users --data '{\"test\":\"value\"}'";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2OkHttpRequest_WithDataRawOption() {
        String curl = "curl https://api.example.com/users --data-raw '{\"test\":\"value\"}'";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("RequestBody"));
    }

    @Test
    public void testCreateOkHttpRequestForCurl_TwoParams_BasicUsage() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();

        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", capturedCurl::set);

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_TwoParams_UrlNotNull() {
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://test.example.com", curl -> {
        });

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_TwoParams_LogHandlerCalled() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();

        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", capturedCurl::set);

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_TwoParams_HttpsUrl() {
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://secure.example.com/api", curl -> {
        });

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_TwoParams_HttpUrl() {
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("http://example.com/api", curl -> {
        });

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_ThreeParams_WithSingleQuote() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();

        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", '\'', capturedCurl::set);

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_ThreeParams_WithDoubleQuote() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();

        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com", '"', capturedCurl::set);

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_ThreeParams_BasicUsage() {
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://test.example.com", '\'', curl -> {
        });

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_ThreeParams_UrlNotNull() {
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com/v1/users", '"', curl -> {
        });

        Assertions.assertNotNull(request);
    }

    @Test
    public void testCreateOkHttpRequestForCurl_ThreeParams_ComplexUrl() {
        OkHttpRequest request = WebUtil.createOkHttpRequestForCurl("https://api.example.com/v1/users?page=1&limit=10", '\'', curl -> {
        });

        Assertions.assertNotNull(request);
    }

    @Test
    public void testBuildCurl_SimpleGetRequest() {
        String result = WebUtil.buildCurl("GET", "https://api.example.com/users", null, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X GET"));
        Assertions.assertTrue(result.contains("https://api.example.com/users"));
    }

    @Test
    public void testBuildCurl_PostWithBody() {
        String result = WebUtil.buildCurl("POST", "https://api.example.com/users", null, "{\"name\":\"John\"}", "application/json", '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X POST"));
        Assertions.assertTrue(result.contains("-d"));
        Assertions.assertTrue(result.contains("name"));
    }

    @Test
    public void testBuildCurl_WithHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");

        String result = WebUtil.buildCurl("POST", "https://api.example.com/users", headers, "{\"name\":\"John\"}", null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("-H"));
        Assertions.assertTrue(result.contains("Content-Type"));
        Assertions.assertTrue(result.contains("Authorization"));
    }

    @Test
    public void testBuildCurl_WithSingleQuote() {
        String result = WebUtil.buildCurl("GET", "https://api.example.com", null, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("'https://api.example.com'"));
    }

    @Test
    public void testBuildCurl_WithDoubleQuote() {
        String result = WebUtil.buildCurl("GET", "https://api.example.com", null, null, null, '"');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("\"https://api.example.com\""));
    }

    @Test
    public void testBuildCurl_PutRequest() {
        String result = WebUtil.buildCurl("PUT", "https://api.example.com/users/1", null, "{\"name\":\"Updated\"}", "application/json", '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X PUT"));
    }

    @Test
    public void testBuildCurl_DeleteRequest() {
        String result = WebUtil.buildCurl("DELETE", "https://api.example.com/users/1", null, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X DELETE"));
    }

    @Test
    public void testBuildCurl_WithBodyTypeButNoContentTypeHeader() {
        String result = WebUtil.buildCurl("POST", "https://api.example.com/users", null, "{\"data\":\"test\"}", "application/json", '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Content-Type"));
        Assertions.assertTrue(result.contains("application/json"));
    }

    @Test
    public void testBuildCurl_WithExistingContentTypeHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/xml");

        String result = WebUtil.buildCurl("POST", "https://api.example.com/users", headers, "<data>test</data>", "application/json", '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("text/xml"));
    }

    @Test
    public void testBuildCurl_EmptyHeaders() {
        Map<String, String> headers = new HashMap<>();

        String result = WebUtil.buildCurl("GET", "https://api.example.com", headers, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X GET"));
    }

    @Test
    public void testBuildCurl_EmptyBody() {
        String result = WebUtil.buildCurl("POST", "https://api.example.com", null, "", null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.contains("-d"));
    }

    @Test
    public void testBuildCurl_NullBody() {
        String result = WebUtil.buildCurl("POST", "https://api.example.com", null, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.contains("-d"));
    }

    @Test
    public void testBuildCurl_WithSpecialCharactersInUrl() {
        String result = WebUtil.buildCurl("GET", "https://api.example.com/search?q=test&page=1", null, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("search?q=test&page=1"));
    }

    @Test
    public void testBuildCurl_WithSpecialCharactersInBody() {
        String result = WebUtil.buildCurl("POST", "https://api.example.com", null, "{\"message\":\"Hello\\nWorld\"}", null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("-d"));
    }

    @Test
    public void testBuildCurl_WithMultipleHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Header1", "value1");
        headers.put("Header2", "value2");
        headers.put("Header3", "value3");

        String result = WebUtil.buildCurl("GET", "https://api.example.com", headers, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Header1"));
        Assertions.assertTrue(result.contains("Header2"));
        Assertions.assertTrue(result.contains("Header3"));
    }

    @Test
    public void testBuildCurl_CustomHttpMethod() {
        String result = WebUtil.buildCurl("PATCH", "https://api.example.com/users/1", null, "{\"status\":\"active\"}", "application/json", '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X PATCH"));
    }

    @Test
    public void testBuildCurl_ComplexRequest() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer abc123");
        headers.put("Accept", "application/json");

        String result = WebUtil.buildCurl("POST", "https://api.example.com/v1/users", headers, "{\"name\":\"John\",\"email\":\"john@example.com\"}", null,
                '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X POST"));
        Assertions.assertTrue(result.contains("Content-Type"));
        Assertions.assertTrue(result.contains("Authorization"));
        Assertions.assertTrue(result.contains("Accept"));
        Assertions.assertTrue(result.contains("-d"));
    }

    @Test
    public void testSetContentTypeByRequestBodyType_SetWhenNotPresent() {
        HttpHeaders headers = HttpHeaders.create();

        WebUtil.setContentTypeByRequestBodyType("application/json", headers);

        Assertions.assertNotNull(headers.get(HttpHeaders.Names.CONTENT_TYPE));
        Assertions.assertEquals("application/json", HttpUtil.getContentType(headers));
    }

    @Test
    public void testSetContentTypeByRequestBodyType_DoNotOverwriteExisting() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setContentType("text/xml");

        WebUtil.setContentTypeByRequestBodyType("application/json", headers);

        Assertions.assertEquals("text/xml", HttpUtil.getContentType(headers));
    }

    @Test
    public void testSetContentTypeByRequestBodyType_NullBodyType() {
        HttpHeaders headers = HttpHeaders.create();

        WebUtil.setContentTypeByRequestBodyType(null, headers);

        Assertions.assertNull(headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyType_EmptyBodyType() {
        HttpHeaders headers = HttpHeaders.create();

        WebUtil.setContentTypeByRequestBodyType("", headers);

        Assertions.assertNull(headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyType_VariousContentTypes() {
        HttpHeaders headers1 = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("application/json", headers1);
        Assertions.assertEquals("application/json", HttpUtil.getContentType(headers1));

        HttpHeaders headers2 = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("text/xml", headers2);
        Assertions.assertEquals("text/xml", HttpUtil.getContentType(headers2));

        HttpHeaders headers3 = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("application/x-www-form-urlencoded", headers3);
        Assertions.assertEquals("application/x-www-form-urlencoded", HttpUtil.getContentType(headers3));
    }

    @Test
    public void testSetContentTypeByRequestBodyType_WithCharset() {
        HttpHeaders headers = HttpHeaders.create();

        WebUtil.setContentTypeByRequestBodyType("application/json; charset=UTF-8", headers);

        String contentType = HttpUtil.getContentType(headers);
        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(contentType.contains("application/json"));
    }

    @Test
    public void testSetContentTypeByRequestBodyType_CalledMultipleTimes() {
        HttpHeaders headers = HttpHeaders.create();

        WebUtil.setContentTypeByRequestBodyType("application/json", headers);
        Assertions.assertEquals("application/json", HttpUtil.getContentType(headers));

        WebUtil.setContentTypeByRequestBodyType("text/xml", headers);
        Assertions.assertEquals("application/json", HttpUtil.getContentType(headers));
    }

    @Test
    public void testCurl2HttpRequest_CaseInsensitiveCurlKeyword() {
        String curl = "CURL https://api.example.com";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
    }

    @Test
    public void testCurl2HttpRequest_MixedCaseCurlKeyword() {
        String curl = "CuRl https://api.example.com";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
    }

    @Test
    public void testCurl2HttpRequest_WithLeadingSpaces() {
        String curl = "   curl https://api.example.com";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
    }

    @Test
    public void testCurl2HttpRequest_WithTrailingSpaces() {
        String curl = "curl https://api.example.com   ";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
    }

    @Test
    public void testCurl2OkHttpRequest_CaseInsensitiveCurlKeyword() {
        String curl = "CURL https://api.example.com";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
    }

    @Test
    public void testBuildCurl_HeadRequest() {
        String result = WebUtil.buildCurl("HEAD", "https://api.example.com/users", null, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X HEAD"));
    }

    @Test
    public void testBuildCurl_OptionsRequest() {
        String result = WebUtil.buildCurl("OPTIONS", "https://api.example.com", null, null, null, '\'');

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("curl -X OPTIONS"));
    }

    @Test
    public void testCurl2HttpRequest_WithComments() {
        String curl = "curl https://api.example.com // this is a comment";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("https://api.example.com"));
    }

    @Test
    public void testCurl2HttpRequest_LongHeaderValue() {
        String curl = "curl https://api.example.com -H \"Authorization: Bearer " + "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.abc123\"";
        String result = WebUtil.convertCurlToHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("Authorization"));
    }

    @Test
    public void testCurl2OkHttpRequest_PostWithoutBodyButWithHeaders() {
        String curl = "curl -X POST https://api.example.com -H \"Content-Type: application/json\"";
        String result = WebUtil.convertCurlToOkHttpRequest(curl);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains(".post();"));
    }
}
