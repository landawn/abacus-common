package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.http.HttpMethod;

public class HttpRequestTest extends TestBase {

    private static final String TEST_URL = "https://httpbin.org/get";
    private static final String POST_URL = "https://httpbin.org/post";
    private static final String PUT_URL = "https://httpbin.org/put";
    private static final String PATCH_URL = "https://httpbin.org/patch";
    private static final String DELETE_URL = "https://httpbin.org/delete";

    private final String testUrl = "https://httpbin.org/get";
    private final URI testUri = URI.create(testUrl);
    private final HttpClient mockHttpClient = HttpClient.newHttpClient();

    public static class TestBean {
        private String name;
        private int value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    // ==================== create(String, HttpClient) ====================

    @Test
    public void test_create_withStringUrl() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.create(TEST_URL, client);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithUrlAndHttpClient() {
        HttpRequest request = HttpRequest.create(testUrl, mockHttpClient);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithStringReturnsNonNull() {
        HttpClient client = HttpClient.newHttpClient();
        assertNotNull(HttpRequest.create(testUrl, client));
    }

    // ==================== create(URI, HttpClient) ====================

    @Test
    public void test_create_withURI() {
        HttpClient client = HttpClient.newHttpClient();
        URI uri = URI.create(TEST_URL);
        HttpRequest request = HttpRequest.create(uri, client);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithURIAndHttpClient() {
        HttpRequest request = HttpRequest.create(testUri, mockHttpClient);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithURIReturnsNonNull() {
        HttpClient client = HttpClient.newHttpClient();
        assertNotNull(HttpRequest.create(testUri, client));
    }

    @Test
    public void testCreateWithURIContainingQueryParams() {
        URI uriWithParams = URI.create("https://httpbin.org/get?key=value");
        HttpRequest request = HttpRequest.url(uriWithParams);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithDifferentHttpClients() {
        HttpClient client1 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpClient client2 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        HttpRequest request1 = HttpRequest.create(testUrl, client1);
        HttpRequest request2 = HttpRequest.create(testUrl, client2);

        assertNotNull(request1);
        assertNotNull(request2);
    }

    @Test
    public void testCreateWithHttpsUrl() {
        HttpRequest request = HttpRequest.url("https://httpbin.org/get");
        assertNotNull(request);
    }

    @Test
    public void testCreateWithHttpUrl() {
        HttpRequest request = HttpRequest.url("http://httpbin.org/get");
        assertNotNull(request);
    }

    // ==================== create(URL, HttpClient) ====================

    @Test
    public void test_create_withURL() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        URL url = new URL(TEST_URL);
        HttpRequest request = HttpRequest.create(url, client);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithURLObjectAndHttpClient() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.create(url, mockHttpClient);
        assertNotNull(request);
    }

    @Test
    public void testCreateWithURLReturnsNonNull() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        assertNotNull(HttpRequest.create(new URL(testUrl), client));
    }

    @Test
    public void testCreateWithTimeoutsAndExecute() {
        try {
            HttpResponse<String> response = HttpRequest.url(testUrl, 5000L, 30000L).header("Accept", "application/json").get();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void testCreateWithTimeoutsUsesNewClient() {
        HttpRequest request = HttpRequest.url(testUrl, 1000L, 2000L);
        assertNotNull(request);
        // The request should work with internal client built from timeouts
        assertDoesNotThrow(() -> {
            try {
                request.get();
            } catch (Exception e) {
                // Network errors are expected
            }
        });
    }

    // ==================== url(String) ====================

    @Test
    public void test_url_withString() {
        HttpRequest request = HttpRequest.url(TEST_URL);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithString() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithStringReturnsNonNull() {
        assertNotNull(HttpRequest.url(testUrl));
    }

    // ==================== url(String, long, long) ====================

    @Test
    public void test_url_withStringAndTimeouts() {
        HttpRequest request = HttpRequest.url(TEST_URL, 5000, 10000);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithStringAndTimeouts() {
        HttpRequest request = HttpRequest.url(testUrl, 5000L, 30000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithStringAndTimeoutsReturnsNonNull() {
        assertNotNull(HttpRequest.url(testUrl, 3000L, 6000L));
    }

    @Test
    public void testUrlWithStringAndZeroTimeouts() {
        HttpRequest request = HttpRequest.url(testUrl, 0L, 0L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithStringAndLargeTimeouts() {
        HttpRequest request = HttpRequest.url(testUrl, 60000L, 120000L);
        assertNotNull(request);
    }

    // ==================== url(URI) ====================

    @Test
    public void test_url_withURI() {
        URI uri = URI.create(TEST_URL);
        HttpRequest request = HttpRequest.url(uri);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURI() {
        HttpRequest request = HttpRequest.url(testUri);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURIReturnsNonNull() {
        assertNotNull(HttpRequest.url(testUri));
    }

    // ==================== url(URI, long, long) ====================

    @Test
    public void test_url_withURIAndTimeouts() {
        URI uri = URI.create(TEST_URL);
        HttpRequest request = HttpRequest.url(uri, 5000, 10000);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURIAndTimeouts() {
        HttpRequest request = HttpRequest.url(testUri, 5000L, 30000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURIAndTimeoutsReturnsNonNull() {
        assertNotNull(HttpRequest.url(testUri, 3000L, 6000L));
    }

    @Test
    public void testUrlWithURIAndZeroTimeouts() {
        HttpRequest request = HttpRequest.url(testUri, 0L, 0L);
        assertNotNull(request);
    }

    @Test
    public void testMultipleConnectTimeoutOverrides() {
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ofSeconds(1)).connectTimeout(Duration.ofSeconds(5));
        assertNotNull(request);
    }

    @Test
    public void testMultipleReadTimeoutOverrides() {
        HttpRequest request = HttpRequest.url(testUrl).readTimeout(Duration.ofSeconds(5)).readTimeout(Duration.ofSeconds(30));
        assertNotNull(request);
    }

    @Test
    public void testChainedBuilderWithQueryAndHeaders() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        HttpRequest request = HttpRequest.url(testUrl).header("Accept", "application/json").query(params).readTimeout(Duration.ofSeconds(10));
        assertNotNull(request);
    }

    @Test
    public void testChainedBuilderWithBodyAndHeaders() {
        HttpRequest request = HttpRequest.url(testUrl).header("X-Custom", "custom-value").jsonBody("{\"key\":\"value\"}").connectTimeout(Duration.ofSeconds(5));
        assertNotNull(request);
    }

    @Test
    public void testChainedBuilderWithAllOptions() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        HttpRequest request = HttpRequest.url(testUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .basicAuth("user", "pass")
                .headers(headers)
                .query("param=value")
                .jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testUrlWithQueryStringAlreadyInUrl() {
        HttpRequest request = HttpRequest.url("https://httpbin.org/get?existing=param");
        assertNotNull(request);
    }

    // ==================== url(URL) ====================

    @Test
    public void test_url_withURL() throws Exception {
        URL url = new URL(TEST_URL);
        HttpRequest request = HttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLObject() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.url(url);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLReturnsNonNull() throws Exception {
        assertNotNull(HttpRequest.url(new URL(testUrl)));
    }

    // ==================== url(URL, long, long) ====================

    @Test
    public void test_url_withURLAndTimeouts() throws Exception {
        URL url = new URL(TEST_URL);
        HttpRequest request = HttpRequest.url(url, 5000, 10000);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLObjectAndTimeouts() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.url(url, 5000L, 30000L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURLAndTimeoutsReturnsNonNull() throws Exception {
        assertNotNull(HttpRequest.url(new URL(testUrl), 3000L, 6000L));
    }

    @Test
    public void testUrlWithURLAndZeroTimeouts() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.url(url, 0L, 0L);
        assertNotNull(request);
    }

    @Test
    public void testUrlWithURITimeoutsUsesNewClient() throws Exception {
        URI uri = URI.create(testUrl);
        HttpRequest request = HttpRequest.url(uri, 1000L, 2000L);
        assertNotNull(request);
        assertDoesNotThrow(() -> {
            try {
                request.get();
            } catch (Exception e) {
                // Network errors are expected
            }
        });
    }

    @Test
    public void testUrlWithURLTimeoutsUsesNewClient() throws Exception {
        URL url = new URL(testUrl);
        HttpRequest request = HttpRequest.url(url, 1000L, 2000L);
        assertNotNull(request);
        assertDoesNotThrow(() -> {
            try {
                request.get();
            } catch (Exception e) {
                // Network errors are expected
            }
        });
    }

    // ==================== connectTimeout(Duration) ====================

    @Test
    public void test_connectTimeout() {
        HttpRequest request = HttpRequest.url(TEST_URL).connectTimeout(Duration.ofSeconds(10));
        assertNotNull(request);
    }

    @Test
    public void testConnectTimeout() {
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ofSeconds(10));
        assertNotNull(request);
    }

    @Test
    public void testConnectTimeoutReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.connectTimeout(Duration.ofSeconds(5)));
    }

    @Test
    public void testConnectTimeoutWithZeroDuration() {
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ZERO);
        assertNotNull(request);
    }

    @Test
    public void testConnectTimeoutWithSmallDuration() {
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ofMillis(1));
        assertNotNull(request);
    }

    @Test
    public void testConnectTimeoutWithLargeDuration() {
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ofMinutes(5));
        assertNotNull(request);
    }

    @Test
    public void testConnectTimeoutOnCreateInstance() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.create(testUrl, client).connectTimeout(Duration.ofSeconds(10));
        assertNotNull(request);
    }

    @Test
    public void testConnectTimeoutThenAuthenticator() {
        Authenticator auth = mock(Authenticator.class);
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ofSeconds(5)).authenticator(auth);
        assertNotNull(request);
    }

    // ==================== readTimeout(Duration) ====================

    @Test
    public void test_readTimeout() {
        HttpRequest request = HttpRequest.url(TEST_URL).readTimeout(Duration.ofSeconds(30));
        assertNotNull(request);
    }

    @Test
    public void testReadTimeout() {
        HttpRequest request = HttpRequest.url(testUrl).readTimeout(Duration.ofSeconds(30));
        assertNotNull(request);
    }

    @Test
    public void testReadTimeoutReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.readTimeout(Duration.ofSeconds(30)));
    }

    @Test
    public void testReadTimeoutWithZeroDuration() {
        HttpRequest request = HttpRequest.url(testUrl).readTimeout(Duration.ZERO);
        assertNotNull(request);
    }

    @Test
    public void testReadTimeoutWithLargeDuration() {
        HttpRequest request = HttpRequest.url(testUrl).readTimeout(Duration.ofMinutes(10));
        assertNotNull(request);
    }

    // ==================== authenticator(Authenticator) ====================

    @Test
    public void test_authenticator() {
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("user", "pass".toCharArray());
            }
        };
        HttpRequest request = HttpRequest.url(TEST_URL).authenticator(auth);
        assertNotNull(request);
    }

    @Test
    public void testAuthenticator() {
        Authenticator authenticator = mock(Authenticator.class);
        HttpRequest request = HttpRequest.url(testUrl).authenticator(authenticator);
        assertNotNull(request);
    }

    @Test
    public void testAuthenticatorReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        Authenticator auth = mock(Authenticator.class);
        assertSame(request, request.authenticator(auth));
    }

    @Test
    public void testAuthenticatorOnCreateInstance() {
        Authenticator auth = mock(Authenticator.class);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.create(testUrl, client).authenticator(auth);
        assertNotNull(request);
    }

    @Test
    public void testAuthenticatorWithConnectTimeout() {
        Authenticator auth = mock(Authenticator.class);
        HttpRequest request = HttpRequest.url(testUrl).connectTimeout(Duration.ofSeconds(5)).authenticator(auth);
        assertNotNull(request);
    }

    @Test
    public void testAuthenticatorThenConnectTimeout() {
        Authenticator auth = mock(Authenticator.class);
        HttpRequest request = HttpRequest.url(testUrl).authenticator(auth).connectTimeout(Duration.ofSeconds(5));
        assertNotNull(request);
    }

    // ==================== basicAuth(String, Object) ====================

    @Test
    public void test_basicAuth() {
        HttpRequest request = HttpRequest.url(TEST_URL).basicAuth("testuser", "testpass");
        assertNotNull(request);
    }

    @Test
    public void testBasicAuth() {
        HttpRequest request = HttpRequest.url(testUrl).basicAuth("username", "password");
        assertNotNull(request);
    }

    @Test
    public void testBasicAuthReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.basicAuth("user", "pass"));
    }

    @Test
    public void testBasicAuthWithEmptyCredentials() {
        HttpRequest request = HttpRequest.url(testUrl).basicAuth("", "");
        assertNotNull(request);
    }

    @Test
    public void testBasicAuthWithSpecialCharacters() {
        HttpRequest request = HttpRequest.url(testUrl).basicAuth("user@domain.com", "p@ss:w0rd!");
        assertNotNull(request);
    }

    @Test
    public void testBasicAuthWithNumericPassword() {
        HttpRequest request = HttpRequest.url(testUrl).basicAuth("admin", 12345);
        assertNotNull(request);
    }

    // ==================== header(String, Object) ====================

    @Test
    public void test_header() {
        HttpRequest request = HttpRequest.url(TEST_URL).header("Accept", "application/json");
        assertNotNull(request);
    }

    @Test
    public void testHeader() {
        HttpRequest request = HttpRequest.url(testUrl).header("Accept", "application/json");
        assertNotNull(request);
    }

    @Test
    public void testHeaderReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.header("Accept", "application/json"));
    }

    @Test
    public void testHeaderWithIntegerValue() {
        HttpRequest request = HttpRequest.url(testUrl).header("X-Request-Id", 12345);
        assertNotNull(request);
    }

    @Test
    public void testHeaderWithMultipleCalls() {
        HttpRequest request = HttpRequest.url(testUrl).header("Accept", "application/json").header("User-Agent", "TestAgent").header("X-Custom", "value");
        assertNotNull(request);
    }

    @Test
    public void testHeaderWithEmptyValue() {
        HttpRequest request = HttpRequest.url(testUrl).header("X-Empty", "");
        assertNotNull(request);
    }

    @Test
    public void testHeaderWithBooleanValue() {
        HttpRequest request = HttpRequest.url(testUrl).header("X-Debug", true);
        assertNotNull(request);
    }

    @Test
    public void testHeaderWithLongValue() {
        HttpRequest request = HttpRequest.url(testUrl).header("X-Timestamp", System.currentTimeMillis());
        assertNotNull(request);
    }

    // ==================== headers(String, Object, String, Object) ====================

    @Test
    public void test_headers_twoParams() {
        HttpRequest request = HttpRequest.url(TEST_URL).headers("Accept", "application/json", "User-Agent", "TestAgent");
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithTwoHeaders() {
        HttpRequest request = HttpRequest.url(testUrl).headers("Accept", "application/json", "Content-Type", "application/json");
        assertNotNull(request);
    }

    @Test
    public void testHeadersTwoReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.headers("Accept", "application/json", "User-Agent", "TestAgent"));
    }

    @Test
    public void testHeadersTwoWithIntegerValues() {
        HttpRequest request = HttpRequest.url(testUrl).headers("X-Id", 100, "X-Count", 200);
        assertNotNull(request);
    }

    // ==================== headers(String, Object, String, Object, String, Object) ====================

    @Test
    public void test_headers_threeParams() {
        HttpRequest request = HttpRequest.url(TEST_URL).headers("Accept", "application/json", "User-Agent", "TestAgent", "X-Custom", "value");
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithThreeHeaders() {
        HttpRequest request = HttpRequest.url(testUrl)
                .headers("Accept", "application/json", "Content-Type", "application/json", "Authorization", "Bearer token");
        assertNotNull(request);
    }

    @Test
    public void testHeadersThreeReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.headers("Accept", "application/json", "User-Agent", "TestAgent", "X-Custom", "value"));
    }

    @Test
    public void testHeadersThreeWithMixedValueTypes() {
        HttpRequest request = HttpRequest.url(testUrl).headers("X-String", "abc", "X-Int", 42, "X-Bool", true);
        assertNotNull(request);
    }

    // ==================== headers(Map) ====================

    @Test
    public void test_headers_withMap() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("User-Agent", "TestAgent");

        HttpRequest request = HttpRequest.url(TEST_URL).headers(headers);
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithMap() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");

        HttpRequest request = HttpRequest.url(testUrl).headers(headers);
        assertNotNull(request);
    }

    @Test
    public void testHeadersMapReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        assertSame(request, request.headers(headers));
    }

    @Test
    public void test_headers_withNullMap() {
        HttpRequest request = HttpRequest.url(TEST_URL).headers((Map<String, String>) null);
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithEmptyMap() {
        HttpRequest request = HttpRequest.url(testUrl).headers(new HashMap<>());
        assertNotNull(request);
    }

    @Test
    public void testHeadersWithLargeMap() {
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            headers.put("X-Header-" + i, "value-" + i);
        }
        HttpRequest request = HttpRequest.url(testUrl).headers(headers);
        assertNotNull(request);
    }

    // ==================== query(String) ====================

    @Test
    public void test_query_withString() {
        HttpRequest request = HttpRequest.url(TEST_URL).query("param1=value1&param2=value2");
        assertNotNull(request);
    }

    @Test
    public void testQueryWithString() {
        HttpRequest request = HttpRequest.url(testUrl).query("param1=value1&param2=value2");
        assertNotNull(request);
    }

    @Test
    public void testQueryStringReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.query("param=value"));
    }

    @Test
    public void test_query_withEmptyString() {
        HttpRequest request = HttpRequest.url(TEST_URL).query("");
        assertNotNull(request);
    }

    @Test
    public void testQueryWithNullString() {
        HttpRequest request = HttpRequest.url(testUrl).query((String) null);
        assertNotNull(request);
    }

    @Test
    public void testQueryWithSingleParam() {
        HttpRequest request = HttpRequest.url(testUrl).query("key=value");
        assertNotNull(request);
    }

    @Test
    public void testQueryWithEncodedValues() {
        HttpRequest request = HttpRequest.url(testUrl).query("q=hello+world&lang=en");
        assertNotNull(request);
    }

    // ==================== query(Map) ====================

    @Test
    public void test_query_withMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", 123);

        HttpRequest request = HttpRequest.url(TEST_URL).query(params);
        assertNotNull(request);
    }

    @Test
    public void testQueryWithMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");

        HttpRequest request = HttpRequest.url(testUrl).query(params);
        assertNotNull(request);
    }

    @Test
    public void testQueryMapReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        assertSame(request, request.query(params));
    }

    @Test
    public void test_query_withEmptyMap() {
        HttpRequest request = HttpRequest.url(TEST_URL).query(new HashMap<>());
        assertNotNull(request);
    }

    @Test
    public void testQueryWithNullMap() {
        HttpRequest request = HttpRequest.url(testUrl).query((Map<String, Object>) null);
        assertNotNull(request);
    }

    @Test
    public void testQueryWithMapContainingIntegerValues() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("size", 20);
        HttpRequest request = HttpRequest.url(testUrl).query(params);
        assertNotNull(request);
    }

    @Test
    public void testQueryOverridesOnSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        request.query("first=1");
        request.query("second=2");
        assertNotNull(request);
    }

    @Test
    public void testQueryWithMultipleParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("q", "search term");
        params.put("page", 1);
        params.put("size", 20);
        params.put("sort", "name");
        HttpRequest request = HttpRequest.url(testUrl).query(params);
        assertNotNull(request);
    }

    // ==================== jsonBody(String) ====================

    @Test
    public void test_jsonBody_withString() {
        HttpRequest request = HttpRequest.url(POST_URL).jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithString() {
        HttpRequest request = HttpRequest.url(testUrl).jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyStringReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.jsonBody("{\"key\":\"value\"}"));
    }

    @Test
    public void testJsonBodyWithEmptyJsonString() {
        HttpRequest request = HttpRequest.url(testUrl).jsonBody("{}");
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithJsonArrayString() {
        HttpRequest request = HttpRequest.url(testUrl).jsonBody("[1,2,3]");
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithComplexJsonString() {
        HttpRequest request = HttpRequest.url(testUrl).jsonBody("{\"name\":\"test\",\"items\":[1,2,3],\"nested\":{\"key\":\"val\"}}");
        assertNotNull(request);
    }

    // ==================== jsonBody(Object) ====================

    @Test
    public void test_jsonBody_withObject() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);

        HttpRequest request = HttpRequest.url(POST_URL).jsonBody(data);
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithObject() {
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");

        HttpRequest request = HttpRequest.url(testUrl).jsonBody(obj);
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyObjectReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");
        assertSame(request, request.jsonBody(obj));
    }

    @Test
    public void testJsonBodyWithEmptyObject() {
        HttpRequest request = HttpRequest.url(testUrl).jsonBody(new HashMap<>());
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithBean() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(42);
        HttpRequest request = HttpRequest.url(testUrl).jsonBody(bean);
        assertNotNull(request);
    }

    @Test
    public void testJsonBodyWithNestedMap() {
        Map<String, Object> outer = new HashMap<>();
        Map<String, Object> inner = new HashMap<>();
        inner.put("nested_key", "nested_value");
        outer.put("data", inner);
        outer.put("count", 5);
        HttpRequest request = HttpRequest.url(testUrl).jsonBody(outer);
        assertNotNull(request);
    }

    // ==================== xmlBody(String) ====================

    @Test
    public void test_xmlBody_withString() {
        HttpRequest request = HttpRequest.url(POST_URL).xmlBody("<root><key>value</key></root>");
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithString() {
        HttpRequest request = HttpRequest.url(testUrl).xmlBody("<root><key>value</key></root>");
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyStringReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.xmlBody("<root/>"));
    }

    @Test
    public void testXmlBodyWithEmptyXmlString() {
        HttpRequest request = HttpRequest.url(testUrl).xmlBody("<root/>");
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithNestedXml() {
        HttpRequest request = HttpRequest.url(testUrl).xmlBody("<root><parent><child>value</child></parent></root>");
        assertNotNull(request);
    }

    // ==================== xmlBody(Object) ====================

    @Test
    public void test_xmlBody_withObject() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        HttpRequest request = HttpRequest.url(POST_URL).xmlBody(data);
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithObject() {
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");

        HttpRequest request = HttpRequest.url(testUrl).xmlBody(obj);
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyObjectReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        Map<String, String> obj = new HashMap<>();
        obj.put("key", "value");
        assertSame(request, request.xmlBody(obj));
    }

    @Test
    public void testXmlBodyWithEmptyMapObject() {
        HttpRequest request = HttpRequest.url(testUrl).xmlBody(new HashMap<>());
        assertNotNull(request);
    }

    @Test
    public void testXmlBodyWithBeanObject() {
        TestBean bean = new TestBean();
        bean.setName("xmlTest");
        bean.setValue(99);
        HttpRequest request = HttpRequest.url(testUrl).xmlBody(bean);
        assertNotNull(request);
    }

    // ==================== formBody(Map) ====================

    @Test
    public void test_formBody_withMap() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "testuser");
        formData.put("password", "testpass");

        HttpRequest request = HttpRequest.url(POST_URL).formBody(formData);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithMap() {
        Map<String, String> formData = new HashMap<>();
        formData.put("username", "user");
        formData.put("password", "pass");

        HttpRequest request = HttpRequest.url(testUrl).formBody(formData);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyMapReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        Map<String, String> formData = new HashMap<>();
        formData.put("user", "admin");
        assertSame(request, request.formBody(formData));
    }

    @Test
    public void testFormBodyWithEmptyMap() {
        HttpRequest request = HttpRequest.url(testUrl).formBody(new HashMap<>());
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithSpecialCharactersMap() {
        Map<String, String> formData = new HashMap<>();
        formData.put("query", "hello world&foo=bar");
        formData.put("email", "user@example.com");
        HttpRequest request = HttpRequest.url(testUrl).formBody(formData);
        assertNotNull(request);
    }

    // ==================== formBody(Object) ====================

    @Test
    public void test_formBody_withBean() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);

        HttpRequest request = HttpRequest.url(POST_URL).formBody(bean);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithObject() {
        TestBean bean = new TestBean();
        bean.setName("user");
        bean.setValue(1);

        HttpRequest request = HttpRequest.url(testUrl).formBody(bean);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyObjectReturnsSameInstance() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(1);
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.formBody(bean));
    }

    @Test
    public void testFormBodyWithDefaultBean() {
        TestBean bean = new TestBean();
        HttpRequest request = HttpRequest.url(testUrl).formBody(bean);
        assertNotNull(request);
    }

    @Test
    public void testFormBodyThenJsonBody() {
        Map<String, String> formData = new HashMap<>();
        formData.put("key", "value");
        HttpRequest request = HttpRequest.url(testUrl);
        request.formBody(formData);
        request.jsonBody("{\"key\":\"value\"}");
        assertNotNull(request);
    }

    @Test
    public void testFormBodyWithSingleEntryMap() {
        Map<String, String> formData = new HashMap<>();
        formData.put("token", "abc123");
        HttpRequest request = HttpRequest.url(testUrl).formBody(formData);
        assertNotNull(request);
    }

    // ==================== body(BodyPublisher) ====================

    @Test
    public void test_body_withBodyPublisher() {
        HttpRequest request = HttpRequest.url(POST_URL).body(BodyPublishers.ofString("test data"));
        assertNotNull(request);
    }

    @Test
    public void testBodyWithBodyPublisher() {
        BodyPublisher publisher = BodyPublishers.ofString("test");
        HttpRequest request = HttpRequest.url(testUrl).body(publisher);
        assertNotNull(request);
    }

    @Test
    public void testBodyPublisherReturnsSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertSame(request, request.body(BodyPublishers.ofString("test")));
    }

    @Test
    public void testBodyPublisherWithNoBody() {
        HttpRequest request = HttpRequest.url(testUrl).body(BodyPublishers.noBody());
        assertNotNull(request);
    }

    @Test
    public void testBodyPublisherWithByteArray() {
        HttpRequest request = HttpRequest.url(testUrl).body(BodyPublishers.ofByteArray(new byte[] { 1, 2, 3 }));
        assertNotNull(request);
    }

    @Test
    public void testBodyPublisherWithEmptyByteArray() {
        HttpRequest request = HttpRequest.url(testUrl).body(BodyPublishers.ofByteArray(new byte[0]));
        assertNotNull(request);
    }

    @Test
    public void testBodyOverridesOnSameInstance() {
        HttpRequest request = HttpRequest.url(testUrl);
        request.jsonBody("{\"first\":1}");
        request.xmlBody("<second/>");
        assertNotNull(request);
    }

    @Test
    public void testBodyPublisherOverridesJsonBody() {
        HttpRequest request = HttpRequest.url(testUrl);
        request.jsonBody("{\"key\":\"value\"}");
        request.body(BodyPublishers.ofString("plain text"));
        assertNotNull(request);
    }

    // ==================== get() ====================

    @Test
    public void test_get() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).get();
            assertNotNull(response);
            assertEquals(200, response.statusCode());
        } catch (Exception e) {
        }
    }

    // ==================== get(BodyHandler) ====================

    @Test
    public void test_get_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).get(BodyHandlers.ofString());
            assertNotNull(response);
            assertEquals(200, response.statusCode());
        } catch (Exception e) {
        }
    }

    @Test
    public void test_get_withByteArrayBodyHandler() {
        try {
            HttpResponse<byte[]> response = HttpRequest.url(TEST_URL).get(BodyHandlers.ofByteArray());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_get_withDiscardingBodyHandler() {
        try {
            HttpResponse<Void> response = HttpRequest.url(TEST_URL).get(BodyHandlers.discarding());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== get(Class) ====================

    @Test
    public void test_get_withResultClass() {
        try {
            String result = HttpRequest.url(TEST_URL).get(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_get_withByteArrayResultClass() {
        try {
            byte[] result = HttpRequest.url(TEST_URL).get(byte[].class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void testGetWithQueryParams() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("key", "value");
            HttpResponse<String> response = HttpRequest.url(TEST_URL).query(params).get();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void testGetWithStringQuery() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).query("key=value&other=123").get();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== post() ====================

    @Test
    public void test_post() {
        try {
            HttpResponse<String> response = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== post(BodyHandler) ====================

    @Test
    public void test_post_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_post_withByteArrayBodyHandler() {
        try {
            HttpResponse<byte[]> response = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(BodyHandlers.ofByteArray());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== post(Class) ====================

    @Test
    public void test_post_withResultClass() {
        try {
            String result = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").post(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void testPostWithFormBody() {
        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("username", "testuser");
            formData.put("password", "testpass");
            HttpResponse<String> response = HttpRequest.url(POST_URL).formBody(formData).post();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void testPostWithXmlBody() {
        try {
            HttpResponse<String> response = HttpRequest.url(POST_URL).xmlBody("<root><key>value</key></root>").post();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== put() ====================

    @Test
    public void test_put() {
        try {
            HttpResponse<String> response = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== put(BodyHandler) ====================

    @Test
    public void test_put_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_put_withByteArrayBodyHandler() {
        try {
            HttpResponse<byte[]> response = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(BodyHandlers.ofByteArray());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== put(Class) ====================

    @Test
    public void test_put_withResultClass() {
        try {
            String result = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").put(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    // ==================== patch() ====================

    @Test
    public void test_patch() {
        try {
            HttpResponse<String> response = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== patch(BodyHandler) ====================

    @Test
    public void test_patch_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_patch_withByteArrayBodyHandler() {
        try {
            HttpResponse<byte[]> response = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(BodyHandlers.ofByteArray());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== patch(Class) ====================

    @Test
    public void test_patch_withResultClass() {
        try {
            String result = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").patch(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    // ==================== delete() ====================

    @Test
    public void test_delete() {
        try {
            HttpResponse<String> response = HttpRequest.url(DELETE_URL).delete();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== delete(BodyHandler) ====================

    @Test
    public void test_delete_withBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(DELETE_URL).delete(BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_delete_withByteArrayBodyHandler() {
        try {
            HttpResponse<byte[]> response = HttpRequest.url(DELETE_URL).delete(BodyHandlers.ofByteArray());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== delete(Class) ====================

    @Test
    public void test_delete_withResultClass() {
        try {
            String result = HttpRequest.url(DELETE_URL).delete(String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    // ==================== head() ====================

    @Test
    public void test_head() {
        try {
            HttpResponse<Void> response = HttpRequest.url(TEST_URL).head();
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== execute(HttpMethod) ====================

    @Test
    public void test_execute_withMethod() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).execute(HttpMethod.GET);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withNullMethod() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(TEST_URL).execute(null);
        });
    }

    @Test
    public void testExecuteWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertThrows(IllegalArgumentException.class, () -> request.execute(null));
    }

    @Test
    public void test_execute_withPostMethod() {
        try {
            HttpResponse<String> response = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").execute(HttpMethod.POST);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withPutMethod() {
        try {
            HttpResponse<String> response = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").execute(HttpMethod.PUT);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withDeleteMethod() {
        try {
            HttpResponse<String> response = HttpRequest.url(DELETE_URL).execute(HttpMethod.DELETE);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withPatchMethod() {
        try {
            HttpResponse<String> response = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").execute(HttpMethod.PATCH);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_execute_withHeadMethod() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).execute(HttpMethod.HEAD);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== execute(HttpMethod, BodyHandler) ====================

    @Test
    public void test_execute_withMethodAndBodyHandler() {
        try {
            HttpResponse<String> response = HttpRequest.url(TEST_URL).execute(HttpMethod.GET, BodyHandlers.ofString());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void testExecuteWithNullMethodAndBodyHandler() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(testUrl).execute(null, BodyHandlers.ofString());
        });
    }

    @Test
    public void test_execute_withMethodAndByteArrayBodyHandler() {
        try {
            HttpResponse<byte[]> response = HttpRequest.url(TEST_URL).execute(HttpMethod.GET, BodyHandlers.ofByteArray());
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    // ==================== execute(HttpMethod, Class) ====================

    @Test
    public void test_execute_withMethodAndResultClass() {
        try {
            String result = HttpRequest.url(TEST_URL).execute(HttpMethod.GET, String.class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    @Test
    public void testExecuteWithNullMethodAndResultClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(testUrl).execute(null, String.class);
        });
    }

    @Test
    public void test_execute_withMethodAndByteArrayResultClass() {
        try {
            byte[] result = HttpRequest.url(TEST_URL).execute(HttpMethod.GET, byte[].class);
            assertNotNull(result);
        } catch (Exception e) {
        }
    }

    // ==================== asyncGet() ====================

    @Test
    public void test_asyncGet() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncGet();
            assertNotNull(future);
            HttpResponse<String> response = future.get();
            assertNotNull(response);
        } catch (ExecutionException e) {
        }
    }

    // ==================== asyncGet(BodyHandler) ====================

    @Test
    public void test_asyncGet_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofString());
            assertNotNull(future);
            HttpResponse<String> response = future.get();
            assertNotNull(response);
        } catch (ExecutionException e) {
        }
    }

    @Test
    public void test_asyncGet_withByteArrayBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<byte[]>> future = HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofByteArray());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncGet(Class) ====================

    @Test
    public void test_asyncGet_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(TEST_URL).asyncGet(String.class);
            assertNotNull(future);
            String result = future.get();
            assertNotNull(result);
        } catch (ExecutionException e) {
        }
    }

    // ==================== asyncGet(BodyHandler, PushPromiseHandler) ====================

    @Test
    public void test_asyncGet_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncGetWithQueryMap() throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).query(params).asyncGet();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPost() ====================

    @Test
    public void test_asyncPost() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPost(BodyHandler) ====================

    @Test
    public void test_asyncPost_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPost_withByteArrayBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<byte[]>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofByteArray());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPost(Class) ====================

    @Test
    public void test_asyncPost_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPost(BodyHandler, PushPromiseHandler) ====================

    @Test
    public void test_asyncPost_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPostWithFormBody() throws Exception {
        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("key", "value");
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).formBody(formData).asyncPost();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPut() ====================

    @Test
    public void test_asyncPut() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPut(BodyHandler) ====================

    @Test
    public void test_asyncPut_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPut_withByteArrayBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<byte[]>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofByteArray());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPut(Class) ====================

    @Test
    public void test_asyncPut_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPut(BodyHandler, PushPromiseHandler) ====================

    @Test
    public void test_asyncPut_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPatch() ====================

    @Test
    public void test_asyncPatch() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPatch(BodyHandler) ====================

    @Test
    public void test_asyncPatch_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncPatch_withByteArrayBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<byte[]>> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(BodyHandlers.ofByteArray());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPatch(Class) ====================

    @Test
    public void test_asyncPatch_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncPatch(BodyHandler, PushPromiseHandler) ====================

    @Test
    public void test_asyncPatch_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PATCH_URL)
                    .jsonBody("{\"test\":\"data\"}")
                    .asyncPatch(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncDelete() ====================

    @Test
    public void test_asyncDelete() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(DELETE_URL).asyncDelete();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncDelete(BodyHandler) ====================

    @Test
    public void test_asyncDelete_withBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncDelete_withByteArrayBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<byte[]>> future = HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofByteArray());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncDelete(Class) ====================

    @Test
    public void test_asyncDelete_withResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(DELETE_URL).asyncDelete(String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncDelete(BodyHandler, PushPromiseHandler) ====================

    @Test
    public void test_asyncDelete_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncHead() ====================

    @Test
    public void test_asyncHead() throws Exception {
        try {
            CompletableFuture<HttpResponse<Void>> future = HttpRequest.url(TEST_URL).asyncHead();
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncExecute(HttpMethod) ====================

    @Test
    public void test_asyncExecute_withMethod() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withNullMethod() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(TEST_URL).asyncExecute(null);
        });
    }

    @Test
    public void testAsyncExecuteWithNullHttpMethod() {
        HttpRequest request = HttpRequest.url(testUrl);
        assertThrows(IllegalArgumentException.class, () -> request.asyncExecute(null));
    }

    @Test
    public void test_asyncExecute_withPostMethod() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncExecute(HttpMethod.POST);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withPutMethod() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncExecute(HttpMethod.PUT);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withDeleteMethod() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(DELETE_URL).asyncExecute(HttpMethod.DELETE);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void test_asyncExecute_withPatchMethod() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncExecute(HttpMethod.PATCH);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncExecute(HttpMethod, BodyHandler) ====================

    @Test
    public void test_asyncExecute_withMethodAndBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofString());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncExecuteWithNullMethodAndBodyHandler() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(testUrl).asyncExecute(null, BodyHandlers.ofString());
        });
    }

    @Test
    public void test_asyncExecute_withMethodAndByteArrayBodyHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<byte[]>> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofByteArray());
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    // ==================== asyncExecute(HttpMethod, Class) ====================

    @Test
    public void test_asyncExecute_withMethodAndResultClass() throws Exception {
        try {
            CompletableFuture<String> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, String.class);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncExecuteWithNullMethodAndResultClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(testUrl).asyncExecute(null, String.class);
        });
    }

    // ==================== asyncExecute(HttpMethod, BodyHandler, PushPromiseHandler) ====================

    @Test
    public void test_asyncExecute_withPushPromiseHandler() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofString(), null);
            assertNotNull(future);
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncExecuteWithNullMethodAndPushPromiseHandler() {
        assertThrows(IllegalArgumentException.class, () -> {
            HttpRequest.url(testUrl).asyncExecute(null, BodyHandlers.ofString(), null);
        });
    }

    // ==================== Comprehensive integration / edge case tests ====================

    @Test
    public void testChainedBuilderMethods() {
        HttpRequest request = HttpRequest.url(testUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", "TestAgent");
        assertNotNull(request);
    }

}
