/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.http.HttpHeaders.Names;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * A comprehensive, thread-safe HTTP client implementation built on Java's {@link HttpURLConnection} foundation,
 * providing high-performance, feature-rich capabilities for modern web service integration and API communication.
 * This class offers an intuitive fluent API for executing HTTP requests with advanced features including automatic
 * content serialization, connection pooling, compression support, SSL/TLS configuration, and asynchronous execution
 * patterns optimized for enterprise-grade applications.
 *
 * <p>The {@code HttpClient} serves as a robust alternative to heavyweight HTTP libraries, focusing on simplicity
 * without sacrificing functionality. It provides seamless integration with Java object models through automatic
 * serialization/deserialization, comprehensive error handling, and efficient resource management suitable for
 * high-throughput scenarios in microservice architectures, REST API clients, and web service integrations.</p>
 *
 * <p><b>⚠️ IMPORTANT - Thread Safety Guarantee:</b>
 * This class is designed to be completely thread-safe and can be safely shared across multiple threads
 * without external synchronization. All internal state is either immutable or properly synchronized,
 * making it suitable for concurrent usage in multi-threaded applications and web servers.</p>
 *
 * <p><b>Key Features and Capabilities:</b>
 * <ul>
 *   <li><b>HTTP Method Support:</b> Full support for GET, POST, PUT, DELETE, HEAD, PATCH, and OPTIONS methods</li>
 *   <li><b>Content Serialization:</b> Automatic JSON, XML, Kryo, and form URL-encoded content handling</li>
 *   <li><b>Compression Support:</b> Built-in GZIP, LZ4, Snappy, and Brotli compression/decompression</li>
 *   <li><b>Asynchronous Execution:</b> Non-blocking operations with {@link ContinuableFuture} integration</li>
 *   <li><b>Connection Management:</b> Intelligent connection pooling and timeout configuration</li>
 *   <li><b>SSL/TLS Support:</b> Custom SSL socket factories and certificate handling</li>
 *   <li><b>Proxy Integration:</b> Comprehensive proxy server support with authentication</li>
 *   <li><b>Error Handling:</b> Robust exception management with detailed error reporting</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Simplicity First:</b> Intuitive API that handles complex HTTP scenarios transparently</li>
 *   <li><b>Performance Optimized:</b> Efficient resource utilization with minimal overhead</li>
 *   <li><b>Type Safety:</b> Strong generic typing for request/response object handling</li>
 *   <li><b>Standards Compliance:</b> Full HTTP/1.1 specification compliance with modern extensions</li>
 *   <li><b>Enterprise Ready:</b> Production-grade reliability with comprehensive configuration options</li>
 * </ul>
 *
 * <p><b>Supported Content Types and Serialization:</b>
 * <table border="1" style="border-collapse: collapse;">
 *   <caption><b>Content Type Support Matrix</b></caption>
 *   <tr style="background-color: #f2f2f2;">
 *     <th>Content Type</th>
 *     <th>Media Type</th>
 *     <th>Serialization Method</th>
 *     <th>Compression Support</th>
 *   </tr>
 *   <tr>
 *     <td>JSON</td>
 *     <td>application/json</td>
 *     <td>Jackson/JSON-B integration</td>
 *     <td>GZIP, Brotli</td>
 *   </tr>
 *   <tr>
 *     <td>XML</td>
 *     <td>application/xml, text/xml</td>
 *     <td>JAXB/DOM parsing</td>
 *     <td>GZIP, Brotli</td>
 *   </tr>
 *   <tr>
 *     <td>Form URL-encoded</td>
 *     <td>application/x-www-form-urlencoded</td>
 *     <td>URLEncodedUtil conversion</td>
 *     <td>GZIP</td>
 *   </tr>
 *   <tr>
 *     <td>Kryo Binary</td>
 *     <td>application/x-kryo</td>
 *     <td>Kryo serialization</td>
 *     <td>LZ4, Snappy</td>
 *   </tr>
 *   <tr>
 *     <td>Plain Text</td>
 *     <td>text/plain</td>
 *     <td>String encoding</td>
 *     <td>GZIP, Brotli</td>
 *   </tr>
 *   <tr>
 *     <td>Binary</td>
 *     <td>application/octet-stream</td>
 *     <td>Raw byte arrays</td>
 *     <td>All supported</td>
 *   </tr>
 * </table>
 *
 * <p><b>HTTP Method Support:</b>
 * <ul>
 *   <li><b>GET:</b> {@code get()}, {@code asyncGet()} - Resource retrieval with optional query parameters</li>
 *   <li><b>POST:</b> {@code post()}, {@code asyncPost()} - Resource creation with request body support</li>
 *   <li><b>PUT:</b> {@code put()}, {@code asyncPut()} - Resource replacement with full entity updates</li>
 *   <li><b>DELETE:</b> {@code delete()}, {@code asyncDelete()} - Resource deletion operations</li>
 *   <li><b>HEAD:</b> {@code head()}, {@code asyncHead()} - Metadata retrieval without response body</li>
 *   <li><b>PATCH:</b> {@code patch()}, {@code asyncPatch()} - Partial resource updates</li>
 *   <li><b>OPTIONS:</b> {@code options()}, {@code asyncOptions()} - Resource capability discovery</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * // Basic client creation with configuration
 * HttpSettings settings = HttpSettings.create()
 *     .setContentType("application/json")
 *     .setContentEncoding("gzip");
 * HttpClient client = HttpClient.create("https://api.example.com",
 *     16, 5000, 30000, settings);
 *
 * // Simple GET request with automatic deserialization
 * User user = client.get("/users/123", User.class);
 *
 * // POST request with automatic serialization
 * CreateUserRequest request = new CreateUserRequest("John", "Doe", "john@example.com");
 * User createdUser = client.post("/users", request, User.class);
 *
 * // Asynchronous operations with error handling
 * ContinuableFuture<List<User>> usersFuture = client.asyncGet("/users",
 *         new Type<List<User>>() {})
 *     .thenApply((users, exception) -> {
 *         if (exception != null) {
 *             logger.error("Failed to fetch users", exception);
 *             return Collections.emptyList();
 *         }
 *         return users;
 *     });
 *
 * // Form data submission with custom settings
 * Map<String, String> formData = Map.of("username", "john", "password", "secret");
 * HttpSettings formSettings = HttpSettings.create()
 *     .setContentType("application/x-www-form-urlencoded");
 * String response = client.post("/login", formData, formSettings, String.class);
 * }</pre>
 *
 * <p><b>Advanced Configuration and Customization:</b>
 * <pre>{@code
 * // SSL/TLS configuration with custom certificates
 * SSLSocketFactory sslFactory = createCustomSSLFactory();
 * HttpSettings secureSettings = HttpSettings.create()
 *     .setSSLSocketFactory(sslFactory);
 * HttpClient secureClient = HttpClient.create("https://secure-api.example.com",
 *     16, 5000, 30000, secureSettings);
 *
 * // Proxy configuration with authentication
 * Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.company.com", 8080));
 * HttpSettings proxySettings = HttpSettings.create()
 *     .setProxy(proxy)
 *     .header("Proxy-Authorization", "Basic " + Strings.base64Encode("user:pass".getBytes()));
 * HttpClient proxyClient = HttpClient.create("https://external-api.com",
 *     16, 5000, 30000, proxySettings);
 *
 * // Custom headers and authentication
 * HttpSettings authSettings = HttpSettings.create()
 *     .header(HttpHeaders.Names.AUTHORIZATION, "Bearer " + accessToken)
 *     .header(HttpHeaders.Names.USER_AGENT, "MyApp/1.0")
 *     .header("X-API-Version", "v2");
 * HttpClient authClient = HttpClient.create("https://api.example.com",
 *     16, 5000, 30000, authSettings);
 *
 * // Connection pooling and timeout tuning
 * HttpClient optimizedClient = HttpClient.create("https://high-throughput-api.com",
 *     50,      // Maximum concurrent connections
 *     2000,    // 2 seconds connection timeout
 *     15000);  // 15 seconds read timeout
 * }</pre>
 *
 * <p><b>Asynchronous Execution and Future Composition:</b>
 * <ul>
 *   <li><b>Non-blocking Operations:</b> All async methods return {@link ContinuableFuture} for non-blocking execution</li>
 *   <li><b>Executor Integration:</b> Custom {@link Executor} support for thread pool management</li>
 *   <li><b>Future Composition:</b> Chainable operations with map, flatMap, and handle methods</li>
 *   <li><b>Error Propagation:</b> Automatic exception propagation through future chains</li>
 *   <li><b>Timeout Support:</b> Configurable timeouts for async operations with cancellation</li>
 * </ul>
 *
 * <p><b>Connection Management and Pooling:</b>
 * <ul>
 *   <li><b>Connection Reuse:</b> Automatic HTTP connection pooling for improved performance</li>
 *   <li><b>Keep-Alive Support:</b> HTTP/1.1 persistent connection management</li>
 *   <li><b>Timeout Configuration:</b> Separate connection and read timeout settings</li>
 *   <li><b>Resource Cleanup:</b> Automatic resource management and connection cleanup</li>
 *   <li><b>Concurrent Limits:</b> Configurable maximum concurrent connection limits</li>
 * </ul>
 *
 * <p><b>Error Handling and Exception Management:</b>
 * <ul>
 *   <li><b>HttpException:</b> Thrown for HTTP error status codes (4xx, 5xx) with response details</li>
 *   <li><b>UncheckedIOException:</b> Wraps IOException for runtime exception handling</li>
 *   <li><b>Timeout Exceptions:</b> Specific exceptions for connection and read timeouts</li>
 *   <li><b>SSL Exceptions:</b> Detailed SSL/TLS handshake and certificate validation errors</li>
 *   <li><b>Serialization Errors:</b> Content serialization/deserialization exception handling</li>
 * </ul>
 *
 * <p><b>Compression and Content Encoding:</b>
 * <ul>
 *   <li><b>GZIP:</b> Standard HTTP compression for text-based content</li>
 *   <li><b>Brotli:</b> Modern compression algorithm with better compression ratios</li>
 *   <li><b>LZ4:</b> Fast compression optimized for binary content (Kryo serialization)</li>
 *   <li><b>Snappy:</b> High-speed compression/decompression for real-time applications</li>
 *   <li><b>Automatic Detection:</b> Content-Type-based compression algorithm selection</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Request Overhead:</b> Minimal per-request overhead comparable to raw HttpURLConnection</li>
 *   <li><b>Memory Usage:</b> Efficient streaming for large request/response bodies</li>
 *   <li><b>Serialization Cost:</b> Optimized object serialization with caching mechanisms</li>
 *   <li><b>Connection Reuse:</b> Significant performance gains through connection pooling</li>
 *   <li><b>Compression Benefits:</b> Reduced bandwidth usage with automatic compression</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrency:</b>
 * <ul>
 *   <li><b>Immutable Configuration:</b> HttpClient instances are immutable after creation</li>
 *   <li><b>Thread-Safe Operations:</b> All methods can be safely called from multiple threads</li>
 *   <li><b>Connection Isolation:</b> Each request uses independent connection resources</li>
 *   <li><b>Async Execution:</b> Non-blocking operations with proper thread isolation</li>
 *   <li><b>Resource Management:</b> Thread-safe resource cleanup and connection management</li>
 * </ul>
 *
 * <p><b>Integration with Frameworks and Libraries:</b>
 * <ul>
 *   <li><b>Spring Integration:</b> Compatible with Spring's RestTemplate patterns and dependency injection</li>
 *   <li><b>JAX-RS Client:</b> Alternative to JAX-RS client implementations</li>
 *   <li><b>Microservice Architecture:</b> Optimized for service-to-service communication</li>
 *   <li><b>Jackson/JSON-B:</b> Seamless integration with popular JSON processing libraries</li>
 *   <li><b>Reactive Streams:</b> Compatible with reactive programming patterns via ContinuableFuture</li>
 * </ul>
 *
 * <p><b>Static Factory Methods:</b>
 * <ul>
 *   <li><b>{@code create(String)}:</b> Create client with base URL</li>
 *   <li><b>{@code create(URL)}:</b> Create client with URL object</li>
 *   <li><b>{@code create(URI)}:</b> Create client with URI object</li>
 *   <li><b>{@code create(HttpSettings)}:</b> Create client with detailed configuration</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Reuse HttpClient instances across multiple requests for connection pooling benefits</li>
 *   <li>Configure appropriate timeouts based on expected response times and network conditions</li>
 *   <li>Use async methods for non-blocking operations in high-concurrency scenarios</li>
 *   <li>Enable compression for APIs that support it to reduce bandwidth usage</li>
 *   <li>Handle exceptions appropriately with retry logic for transient failures</li>
 *   <li>Use strongly-typed objects instead of raw strings for request/response bodies</li>
 *   <li>Configure SSL/TLS properly for production environments with certificate validation</li>
 *   <li>Monitor connection pool usage in high-throughput applications</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Creating new HttpClient instances for each request (defeats connection pooling)</li>
 *   <li>Ignoring HTTP status codes and error responses</li>
 *   <li>Using blocking operations in async contexts (causes thread pool exhaustion)</li>
 *   <li>Not configuring appropriate timeouts (can cause indefinite blocking)</li>
 *   <li>Hardcoding URLs instead of using path() method for URL construction</li>
 *   <li>Not handling SSL certificate validation in production environments</li>
 *   <li>Ignoring content encoding headers when manually processing responses</li>
 * </ul>
 *
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li><b>SSL/TLS Validation:</b> Always validate certificates in production environments</li>
 *   <li><b>Authentication Headers:</b> Secure handling of API keys and authentication tokens</li>
 *   <li><b>Proxy Security:</b> Proper authentication and encryption for proxy connections</li>
 *   <li><b>Input Validation:</b> Validate all input data before serialization</li>
 *   <li><b>Error Information:</b> Avoid exposing sensitive information in error messages</li>
 * </ul>
 *
 * <p><b>Example: Microservice Client Implementation</b>
 * <pre>{@code
 * @Component
 * public class UserServiceClient {
 *     private final HttpClient httpClient;
 *
 *     public UserServiceClient(@Value("${user.service.url}") String baseUrl,
 *                             @Value("${user.service.timeout}") int timeoutMs) {
 *         HttpSettings settings = HttpSettings.create()
 *             .setContentType("application/json")
 *             .setContentEncoding("gzip")
 *             .header(HttpHeaders.Names.USER_AGENT, "UserServiceClient/1.0");
 *         this.httpClient = HttpClient.create(baseUrl, 16,
 *             timeoutMs, timeoutMs * 2, settings);
 *     }
 *
 *     public User findById(Long userId) {
 *         String path = "/users/" + userId;
 *         return httpClient.get(path, User.class);
 *     }
 *
 *     public ContinuableFuture<User> createUserAsync(CreateUserRequest request) {
 *         return httpClient.asyncPost("/users", request, User.class);
 *     }
 *
 *     public List<User> searchUsers(UserSearchCriteria criteria) {
 *         String path = "/users/search?" + URLEncodedUtil.encode(criteria);
 *         return httpClient.get(path, new Type<List<User>>() {});
 *     }
 *
 *     public void updateUserAsync(Long userId, UpdateUserRequest request,
 *                               Consumer<User> onSuccess, Consumer<Exception> onError) {
 *         String path = "/users/" + userId;
 *         httpClient.asyncPut(path, request, User.class)
 *             .thenAccept(user -> onSuccess.accept(user))
 *             .exceptionally(exception -> {
 *                 onError.accept((Exception) exception);
 *                 return null;
 *             });
 *     }
 * }
 * }</pre>
 *
 * <p><b>Comparison with Alternative HTTP Clients:</b>
 * <ul>
 *   <li><b>vs. Apache HttpClient:</b> Simpler API vs. comprehensive but complex feature set</li>
 *   <li><b>vs. OkHttp:</b> Java-native vs. modern design with different dependency requirements</li>
 *   <li><b>vs. java.net.http.HttpClient:</b> Works with older Java versions vs. Java 11+ requirement</li>
 *   <li><b>vs. Spring RestTemplate:</b> Standalone utility vs. Spring Framework integration</li>
 * </ul>
 *
 * <p><b>Monitoring and Debugging:</b>
 * <ul>
 *   <li><b>Request Logging:</b> Built-in logger for debugging HTTP request/response cycles</li>
 *   <li><b>Performance Metrics:</b> Connection pool statistics and timing information</li>
 *   <li><b>Error Diagnostics:</b> Detailed exception messages with request context</li>
 *   <li><b>Network Troubleshooting:</b> Support for network-level debugging and analysis</li>
 * </ul>
 *
 * @see HttpSettings
 * @see HttpRequest
 * @see HttpResponse
 * @see HttpURLConnection
 * @see ContinuableFuture
 * @see URLEncodedUtil
 * @see com.landawn.abacus.util.AsyncExecutor
 * @see <a href="https://tools.ietf.org/html/rfc7231">RFC 7231: HTTP/1.1 Semantics and Content</a>
 * @see <a href="https://tools.ietf.org/html/rfc7540">RFC 7540: HTTP/2</a>
 */
public final class HttpClient {

    static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            // ignore
        } else {
            final int maxConnections = IOUtil.CPU_CORES * 16;

            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.maxConnections", String.valueOf(maxConnections));
        }
    }

    // ...
    /** Default maximum number of concurrent connections per HttpClient instance. */
    public static final int DEFAULT_MAX_CONNECTION = 16;

    /** Default connection timeout in milliseconds (8 seconds). */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 8000;

    /** Default read timeout in milliseconds (16 seconds). */
    public static final int DEFAULT_READ_TIMEOUT = 16000;

    // ...
    private final String _url; //NOSONAR

    private final int _maxConnection; //NOSONAR

    private final long _connectionTimeoutInMillis; //NOSONAR

    private final long _readTimeoutInMillis; //NOSONAR

    private final HttpSettings _settings; //NOSONAR

    final AsyncExecutor _asyncExecutor; //NOSONAR

    private final URL _netURL; //NOSONAR

    private final AtomicInteger _activeConnectionCounter; //NOSONAR

    private HttpClient(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        this(null, url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    private HttpClient(final URL netUrl, final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        N.checkArgument(netUrl != null || Strings.isNotEmpty(url), "url cannot be null or empty");

        if ((maxConnection < 0) || (connectionTimeoutInMillis < 0) || (readTimeoutInMillis < 0)) {
            throw new IllegalArgumentException("maxConnection, connectionTimeoutInMillis or readTimeoutInMillis can't be less than 0: " + maxConnection + ", "
                    + connectionTimeoutInMillis + ", " + readTimeoutInMillis);
        }

        _netURL = netUrl == null ? createNetUrl(url) : netUrl;
        _url = Strings.isEmpty(url) ? _netURL.toString() : url;
        _maxConnection = (maxConnection == 0) ? DEFAULT_MAX_CONNECTION : maxConnection;
        _connectionTimeoutInMillis = (connectionTimeoutInMillis == 0) ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeoutInMillis;
        _readTimeoutInMillis = (readTimeoutInMillis == 0) ? DEFAULT_READ_TIMEOUT : readTimeoutInMillis;
        _settings = settings == null ? HttpSettings.create() : settings;

        _asyncExecutor = executor == null ? HttpUtil.DEFAULT_ASYNC_EXECUTOR : new AsyncExecutor(executor);

        _activeConnectionCounter = sharedActiveConnectionCounter;
    }

    private static URL createNetUrl(final String url) {
        try {
            return URI.create(N.checkArgNotNull(url, "url")).toURL();
        } catch (final MalformedURLException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Gets the base URL configured for this HTTP client.
     * 
     * @return The base URL as a string
     */
    public String url() {
        return _url;
    }

    /**
     * Creates an HttpClient instance with the specified URL and default settings.
     * Uses default values for max connections, connection timeout, and read timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com");
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or empty
     */
    public static HttpClient create(final String url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     * Creates an HttpClient instance with the specified URL and maximum connections.
     * Uses default values for connection timeout and read timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com", 32);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or empty, or maxConnection is negative
     */
    public static HttpClient create(final String url, final int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpClient instance with the specified URL and timeout settings.
     * Uses default value for max connections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com", 5000, 10000);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or empty, or timeouts are negative
     */
    public static HttpClient create(final String url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, DEFAULT_MAX_CONNECTION, connectionTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     * Creates an HttpClient instance with the specified URL, max connections, and timeout settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com", 32, 5000, 10000);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     * Creates an HttpClient instance with the specified URL, max connections, timeout settings, and HTTP settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setContentType("application/json")
     *     .header("Authorization", "Bearer token123");
     * HttpClient client = HttpClient.create("https://api.example.com", 16, 5000, 10000, settings);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings (headers, content type, etc.)
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is {@code null} or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     * Creates an HttpClient instance with a shared active connection counter.
     * This allows multiple HttpClient instances to share a connection limit across all instances.
     * Useful when you need to enforce a global connection limit across multiple HTTP endpoints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger sharedCounter = new AtomicInteger(0);
     * HttpClient client1 = HttpClient.create("https://api1.example.com", 10, 5000, 10000, null, sharedCounter);
     * HttpClient client2 = HttpClient.create("https://api2.example.com", 10, 5000, 10000, null, sharedCounter);
     * // Both clients share a maximum of 10 concurrent connections total
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings
     * @param sharedActiveConnectionCounter Shared counter for active connections across multiple HttpClient instances
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     * Creates an HttpClient instance with a custom executor for async operations.
     * The executor will be used for all asynchronous HTTP requests initiated by this client.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Executor customExecutor = Executors.newFixedThreadPool(4);
     * HttpClient client = HttpClient.create("https://api.example.com", 16, 5000, 10000, customExecutor);
     * client.asyncGet(User.class).thenAccept(user -> System.out.println(user));
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     * Creates an HttpClient instance with the specified URL, settings, and custom executor.
     * Combines HTTP settings configuration with a custom async executor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setContentFormat(ContentFormat.JSON)
     *     .header("Authorization", "Bearer token");
     * Executor executor = Executors.newCachedThreadPool();
     * HttpClient client = HttpClient.create("https://api.example.com", 20, 5000, 15000, settings, executor);
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is {@code null} or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     * Creates an HttpClient instance with all configuration options.
     * This is the most comprehensive factory method allowing full control over all settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger sharedCounter = new AtomicInteger(0);
     * HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON);
     * Executor executor = Executors.newFixedThreadPool(10);
     * HttpClient client = HttpClient.create(
     *     "https://api.example.com", 20, 5000, 10000, settings, sharedCounter, executor
     * );
     * }</pre>
     *
     * @param url The base URL for the HTTP client
     * @param maxConnection Maximum number of concurrent connections per client
     * @param connectionTimeoutInMillis Connection timeout in milliseconds (0 uses default)
     * @param readTimeoutInMillis Read timeout in milliseconds (0 uses default)
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param sharedActiveConnectionCounter Shared counter for managing active connections across multiple clients
     * @param executor Custom executor for asynchronous operations (null uses default)
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or empty, or any numeric parameter is negative
     */
    public static HttpClient create(final String url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     * Creates an HttpClient instance with a URL object and default settings.
     * Uses default values for max connections, connection timeout, and read timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL apiUrl = new URL("https://api.example.com");
     * HttpClient client = HttpClient.create(apiUrl);
     * }</pre>
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is null
     */
    public static HttpClient create(final URL url) {
        return create(url, DEFAULT_MAX_CONNECTION);
    }

    /**
     * Creates an HttpClient instance with a URL object and maximum connections.
     * Uses default values for connection timeout and read timeout.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent connections
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or maxConnection is negative
     */
    public static HttpClient create(final URL url, final int maxConnection) {
        return create(url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates an HttpClient instance with a URL object and timeout settings.
     * Uses default value for max connections.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or timeouts are negative
     */
    public static HttpClient create(final URL url, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, DEFAULT_MAX_CONNECTION, connectionTimeoutInMillis, readTimeoutInMillis);
    }

    /**
     * Creates an HttpClient instance with a URL object, max connections, and timeout settings.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, (HttpSettings) null);
    }

    /**
     * Creates an HttpClient instance with a URL object and all basic configuration options.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is {@code null} or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0));
    }

    /**
     * Creates an HttpClient instance with a URL object and shared connection counter.
     * Allows multiple HttpClient instances to share a connection limit.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param sharedActiveConnectionCounter Shared counter for active connections across multiple clients
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, null);
    }

    /**
     * Creates an HttpClient instance with a URL object and custom executor.
     * The executor will be used for all asynchronous HTTP requests.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final Executor executor) {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, null, executor);
    }

    /**
     * Creates an HttpClient instance with a URL object, settings, and custom executor.
     * Combines HTTP settings configuration with a custom async executor.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds
     * @param readTimeoutInMillis Read timeout in milliseconds
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param executor Custom executor for asynchronous operations
     * @return A new HttpClient instance
     * @throws UncheckedIOException if an I/O error occurs
     * @throws IllegalArgumentException if url is {@code null} or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final Executor executor) throws UncheckedIOException {
        return create(url, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, new AtomicInteger(0), executor);
    }

    /**
     * Creates an HttpClient instance with a URL object and all configuration options.
     * This is the most comprehensive factory method for URL-based clients.
     *
     * @param url The base URL for the HTTP client (as a java.net.URL object)
     * @param maxConnection Maximum number of concurrent connections
     * @param connectionTimeoutInMillis Connection timeout in milliseconds (0 uses default)
     * @param readTimeoutInMillis Read timeout in milliseconds (0 uses default)
     * @param settings Additional HTTP settings (headers, content type, proxy, SSL, etc.)
     * @param sharedActiveConnectionCounter Shared counter for managing active connections across multiple clients
     * @param executor Custom executor for asynchronous operations (null uses default)
     * @return A new HttpClient instance
     * @throws IllegalArgumentException if url is {@code null} or any numeric parameter is negative
     */
    public static HttpClient create(final URL url, final int maxConnection, final long connectionTimeoutInMillis, final long readTimeoutInMillis,
            final HttpSettings settings, final AtomicInteger sharedActiveConnectionCounter, final Executor executor) {
        return new HttpClient(url, null, maxConnection, connectionTimeoutInMillis, readTimeoutInMillis, settings, sharedActiveConnectionCounter, executor);
    }

    /**
     * Performs a GET request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpClient client = HttpClient.create("https://api.example.com/users");
     * String response = client.get();
     * }</pre>
     *
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String get() throws UncheckedIOException {
        return get(String.class);
    }

    /**
     * Performs a GET request with custom settings and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Accept", "application/json");
     * String response = client.get(settings);
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String get(final HttpSettings settings) throws UncheckedIOException {
        return get(settings, String.class);
    }

    /**
     * Performs a GET request with query parameters and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 10);
     * String response = client.get(params);
     * }</pre>
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String get(final Object queryParameters) throws UncheckedIOException {
        return get(queryParameters, String.class);
    }

    /**
     * Performs a GET request with query parameters and custom settings, returning the response as a String.
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String get(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return get(queryParameters, settings, String.class);
    }

    /**
     * Performs a GET request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = client.get(User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final Class<T> resultClass) throws UncheckedIOException {
        return get(null, _settings, resultClass);
    }

    /**
     * Performs a GET request with custom settings and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return get(null, settings, resultClass);
    }

    /**
     * Performs a GET request with query parameters and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final Object queryParameters, final Class<T> resultClass) throws UncheckedIOException {
        return get(queryParameters, _settings, resultClass);
    }

    /**
     * Performs a GET request with all options and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T get(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    /**
     * Performs a DELETE request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = client.delete();
     * }</pre>
     *
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String delete() throws UncheckedIOException {
        return delete(String.class);
    }

    /**
     * Performs a DELETE request with custom settings and returns the response as a String.
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String delete(final HttpSettings settings) throws UncheckedIOException {
        return delete(settings, String.class);
    }

    /**
     * Performs a DELETE request with query parameters and returns the response as a String.
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String delete(final Object queryParameters) throws UncheckedIOException {
        return delete(queryParameters, String.class);
    }

    /**
     * Performs a DELETE request with query parameters and custom settings, returning the response as a String.
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String delete(final Object queryParameters, final HttpSettings settings) throws UncheckedIOException {
        return delete(queryParameters, settings, String.class);
    }

    /**
     * Performs a DELETE request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final Class<T> resultClass) throws UncheckedIOException {
        return delete(null, _settings, resultClass);
    }

    /**
     * Performs a DELETE request with query parameters and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final Object queryParameters, final Class<T> resultClass) throws UncheckedIOException {
        return delete(queryParameters, _settings, resultClass);
    }

    /**
     * Performs a DELETE request with custom settings and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return delete(null, settings, resultClass);
    }

    /**
     * Performs a DELETE request with all options and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T delete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     * Performs a POST request with the specified request body and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", "Doe");
     * String response = client.post(user);
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for JSON/XML serialization)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String post(final Object request) throws UncheckedIOException {
        return post(request, String.class);
    }

    /**
     * Performs a POST request and deserializes the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * User createdUser = client.post(newUser, User.class);
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param request The request body
     * @param resultClass The class of the expected response object
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T post(final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return post(request, _settings, resultClass);
    }

    /**
     * Performs a POST request with custom settings and returns the response as a String.
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String post(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return post(request, settings, String.class);
    }

    /**
     * Performs a POST request with custom settings and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T post(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     * Performs a PUT request with the specified request body and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User updatedUser = new User("John", "Smith");
     * String response = client.put(updatedUser);
     * }</pre>
     *
     * @param request The request body
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String put(final Object request) throws UncheckedIOException {
        return put(request, String.class);
    }

    /**
     * Performs a PUT request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T put(final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return put(request, _settings, resultClass);
    }

    /**
     * Performs a PUT request with custom settings and returns the response as a String.
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String put(final Object request, final HttpSettings settings) throws UncheckedIOException {
        return put(request, settings, String.class);
    }

    /**
     * Performs a PUT request with custom settings and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T put(final Object request, final HttpSettings settings, final Class<T> resultClass) throws UncheckedIOException {
        return execute(HttpMethod.PUT, request, settings, resultClass);
    }

    /**
     * Performs a HEAD request with default settings.
     * HEAD requests are used to retrieve headers without the response body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * client.head(); // Check if resource exists
     * }</pre>
     *
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void head() throws UncheckedIOException {
        head(_settings);
    }

    /**
     * Performs a HEAD request with custom settings.
     * HEAD requests retrieve only headers without the response body.
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void head(final HttpSettings settings) throws UncheckedIOException {
        execute(HttpMethod.HEAD, null, settings, Void.class);
    }

    /**
     * Executes an HTTP request with the specified method and request body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String response = client.execute(HttpMethod.POST, requestBody);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String execute(final HttpMethod httpMethod, final Object request) throws UncheckedIOException {
        return execute(httpMethod, request, String.class);
    }

    /**
     * Executes an HTTP request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass) throws UncheckedIOException {
        return execute(httpMethod, request, _settings, resultClass);
    }

    /**
     * Executes an HTTP request with custom settings and returns the response as a String.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return The response body as a String
     * @throws UncheckedIOException if an I/O error occurs
     */
    public String execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) throws UncheckedIOException {
        return execute(httpMethod, request, settings, String.class);
    }

    /**
     * Executes an HTTP request with all options and deserializes the response to the specified type.
     * This is the core method that all other request methods delegate to.
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return The deserialized response object, or {@code null} if resultClass is Void
     * @throws UncheckedIOException if an I/O error occurs
     */
    public <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass)
            throws UncheckedIOException {
        return execute(httpMethod, request, settings, resultClass, null, null);
    }

    /**
     * Executes an HTTP request and writes the response to a file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File outputFile = new File("response.json");
     * client.execute(HttpMethod.GET, null, settings, outputFile);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request
     * @param output The file to write the response to
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output) throws UncheckedIOException {
        OutputStream os = null;

        try {
            os = IOUtil.newFileOutputStream(output);
            execute(httpMethod, request, settings, os);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     * Executes an HTTP request and writes the response to an output stream.
     * The output stream is not closed by this method.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The output stream to write the response to
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output) throws UncheckedIOException {
        execute(httpMethod, request, settings, null, output, null);
    }

    /**
     * Executes an HTTP request and writes the response to a writer.
     * The writer is not closed by this method.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The writer to write the response to
     * @throws UncheckedIOException if an I/O error occurs
     */
    public void execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output) throws UncheckedIOException {
        execute(httpMethod, request, settings, null, null, output);
    }

    /**
     *
     * @param httpMethod
     * @param request
     * @param settings
     * @param resultClass
     * @param outputStream
     * @param outputWriter
     * @param <T>
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private <T> T execute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass,
            final OutputStream outputStream, final Writer outputWriter) throws UncheckedIOException {
        final Charset requestCharset = HttpUtil.getRequestCharset(settings == null || settings.headers().isEmpty() ? _settings.headers() : settings.headers());
        final ContentFormat requestContentFormat = getContentFormat(settings);
        final boolean doOutput = request != null && !(httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE);

        final HttpURLConnection connection = openConnection(httpMethod, request, settings, doOutput, resultClass);
        final long sentRequestAtMillis = System.currentTimeMillis();
        InputStream is = null;
        OutputStream os = null;

        try { //NOSONAR
            if (request != null && requireBody(httpMethod)) {
                os = HttpUtil.getOutputStream(connection, requestContentFormat, getContentType(settings), getContentEncoding(settings));

                final Type<Object> type = N.typeOf(request.getClass());

                if (request instanceof File fileRequest) {
                    try (InputStream fileInputStream = IOUtil.newFileInputStream(fileRequest)) {
                        IOUtil.write(fileInputStream, os);
                    }
                } else if (type.isInputStream()) {
                    IOUtil.write((InputStream) request, os);
                } else if (type.isReader()) {
                    final BufferedWriter bw = Objectory.createBufferedWriter(IOUtil.newOutputStreamWriter(os, requestCharset));

                    try {
                        IOUtil.write((Reader) request, bw);

                        bw.flush();
                    } finally {
                        Objectory.recycle(bw);
                    }
                } else {
                    if (request instanceof String) {
                        IOUtil.write(((String) request).getBytes(requestCharset), os);
                    } else if (request.getClass().equals(byte[].class)) {
                        IOUtil.write((byte[]) request, os);
                    } else {
                        if (requestContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                            HttpUtil.kryoParser.serialize(request, os);
                        } else if (requestContentFormat == ContentFormat.FormUrlEncoded) {
                            IOUtil.write(URLEncodedUtil.encode(request, requestCharset).getBytes(requestCharset), os);
                        } else {
                            final BufferedWriter bw = Objectory.createBufferedWriter(IOUtil.newOutputStreamWriter(os, requestCharset));

                            try {
                                HttpUtil.getParser(requestContentFormat).serialize(request, bw);

                                bw.flush();
                            } finally {
                                Objectory.recycle(bw);
                            }
                        }
                    }
                }

                HttpUtil.flush(os);
            }

            final int statusCode = connection.getResponseCode();
            final Map<String, List<String>> respHeaders = connection.getHeaderFields();
            final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
            final ContentFormat respContentFormat = HttpUtil.getResponseContentFormat(respHeaders, requestContentFormat);

            is = HttpUtil.getInputStream(connection, respContentFormat);

            if (!HttpUtil.isSuccessfulResponseCode(statusCode) && (resultClass == null || !resultClass.equals(HttpResponse.class))) {
                throw new UncheckedIOException(
                        new IOException(statusCode + ": " + connection.getResponseMessage() + ". " + IOUtil.readAllToString(is, respCharset)));
            }

            if (isOneWayRequest(settings, resultClass, outputStream, outputWriter)) {
                return null;
            } else {
                if (outputStream != null) {
                    IOUtil.write(is, outputStream, true);

                    return null;
                } else if (outputWriter != null) {
                    final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(is, respCharset));

                    try {
                        IOUtil.write(br, outputWriter, true);
                    } finally {
                        Objectory.recycle(br);
                    }

                    return null;
                } else {
                    if (resultClass.equals(HttpResponse.class)) {
                        return (T) new HttpResponse(_url, sentRequestAtMillis, System.currentTimeMillis(), statusCode, connection.getResponseMessage(),
                                respHeaders, IOUtil.readAllBytes(is), respContentFormat, respCharset);
                    } else {
                        if (resultClass.equals(String.class)) {
                            return (T) IOUtil.readAllToString(is, respCharset);
                        } else if (byte[].class.equals(resultClass)) {
                            return (T) IOUtil.readAllBytes(is);
                        } else {
                            if (respContentFormat == ContentFormat.KRYO && HttpUtil.kryoParser != null) {
                                return HttpUtil.kryoParser.deserialize(is, resultClass);
                            } else if (respContentFormat == ContentFormat.FormUrlEncoded) {
                                return URLEncodedUtil.decode(IOUtil.readAllToString(is, respCharset), resultClass);
                            } else {
                                final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(is, respCharset));

                                try {
                                    return HttpUtil.getParser(respContentFormat).deserialize(br, resultClass);
                                } finally {
                                    Objectory.recycle(br);
                                }
                            }
                        }
                    }
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os, is, connection);
        }
    }

    boolean isOneWayRequest(final HttpSettings settings, final Class<?> resultClass, final OutputStream outputStream, final Writer outputWriter) {
        return (resultClass == null || Void.class.equals(resultClass) || (settings == null ? _settings.isOneWayRequest() : settings.isOneWayRequest()))
                && outputStream == null && outputWriter == null;
    }

    ContentFormat getContentFormat(final HttpSettings settings) {
        ContentFormat contentFormat = null;

        if (settings != null) {
            contentFormat = settings.getContentFormat();
        }

        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            contentFormat = _settings.getContentFormat();
        }

        return contentFormat;
    }

    String getContentType(final HttpSettings settings) {
        String contentType = null;

        if (settings != null) {
            contentType = settings.getContentType();
        }

        if (Strings.isEmpty(contentType)) {
            contentType = _settings.getContentType();
        }

        return contentType;
    }

    private String getContentEncoding(final HttpSettings settings) {
        String contentEncoding = null;

        if (settings != null) {
            contentEncoding = settings.getContentEncoding();
        }

        if (Strings.isEmpty(contentEncoding)) {
            contentEncoding = _settings.getContentEncoding();
        }

        return contentEncoding;
    }

    private boolean requireBody(final HttpMethod httpMethod) {
        return httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH;
    }

    /**
     * Opens a new HTTP connection with the specified method and settings.
     * This method is primarily for advanced use cases where direct control over the connection is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpURLConnection conn = client.openConnection(HttpMethod.GET, settings, false, String.class);
     * // Manually configure and use the connection
     * conn.connect();
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param settings Additional HTTP settings for the connection
     * @param doOutput Whether the connection will send a request body
     * @param resultClass The expected result class (used for optimization)
     * @return A configured HttpURLConnection ready for use
     * @throws UncheckedIOException if an I/O error occurs
     */
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final HttpSettings settings, final boolean doOutput, final Class<?> resultClass)
            throws UncheckedIOException {
        return openConnection(httpMethod, null, settings, doOutput, resultClass);
    }

    /**
     * Opens a new HTTP connection with query parameters and the specified settings.
     * This method is primarily for advanced use cases where direct control over the connection is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("id", 123);
     * HttpURLConnection conn = client.openConnection(HttpMethod.GET, params, settings, false, User.class);
     * }</pre>
     *
     * @param httpMethod The HTTP method to use
     * @param queryParameters Query parameters for GET/DELETE requests (can be String, Map, or Bean)
     * @param settings Additional HTTP settings for the connection
     * @param doOutput Whether the connection will send a request body
     * @param resultClass The expected result class (used for optimization)
     * @return A configured HttpURLConnection ready for use
     * @throws UncheckedIOException if an I/O error occurs or connection limit is exceeded
     * @throws RuntimeException if maximum connection limit is exceeded
     */
    @SuppressWarnings("unused")
    public HttpURLConnection openConnection(final HttpMethod httpMethod, final Object queryParameters, final HttpSettings settings, final boolean doOutput,
            final Class<?> resultClass) throws UncheckedIOException {
        HttpURLConnection connection = null;

        if (_activeConnectionCounter.incrementAndGet() > _maxConnection) {
            _activeConnectionCounter.decrementAndGet();
            throw new RuntimeException("Can not get connection, exceeded max connection number: " + _maxConnection);
        }

        try {
            synchronized (_netURL) {
                URL netURL = _netURL;

                if (queryParameters != null && (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE)) {
                    netURL = URI.create(URLEncodedUtil.encode(_url, queryParameters)).toURL();
                }

                final Proxy proxy = (settings == null ? _settings : settings).getProxy();

                if (proxy == null) {
                    connection = (HttpURLConnection) netURL.openConnection();
                } else {
                    connection = (HttpURLConnection) netURL.openConnection(proxy);
                }
            }

            if (connection instanceof HttpsURLConnection) {
                final SSLSocketFactory ssf = (settings == null ? _settings : settings).getSSLSocketFactory();

                if (ssf != null) {
                    ((HttpsURLConnection) connection).setSSLSocketFactory(ssf);
                }
            }

            int connectionTimeoutInMillis = _connectionTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _connectionTimeoutInMillis;

            if (settings != null && settings.getConnectionTimeout() > 0) {
                connectionTimeoutInMillis = Numbers.toIntExact(settings.getConnectionTimeout());
            }

            if (connectionTimeoutInMillis > 0) {
                connection.setConnectTimeout(connectionTimeoutInMillis);
            }

            int readTimeoutInMillis = _readTimeoutInMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) _readTimeoutInMillis;

            if (settings != null && settings.getReadTimeout() > 0) {
                readTimeoutInMillis = Numbers.toIntExact(settings.getReadTimeout());
            }

            if (readTimeoutInMillis > 0) {
                connection.setReadTimeout(readTimeoutInMillis);
            }

            if (settings != null) {
                connection.setDoInput(settings.doInput());
                connection.setDoOutput(settings.doOutput());
            }

            connection.setUseCaches((settings != null && settings.getUseCaches()) || (_settings != null && _settings.getUseCaches()));

            //noinspection DataFlowIssue
            setHttpProperties(connection, settings == null || settings.headers().isEmpty() ? _settings : settings);

            connection.setRequestMethod(httpMethod.name());

            return connection;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sets the http properties.
     *
     * @param connection
     * @param settings
     * @throws UncheckedIOException the unchecked IO exception
     */
    void setHttpProperties(final HttpURLConnection connection, final HttpSettings settings) throws UncheckedIOException {
        final HttpHeaders headers = settings.headers();

        if (headers != null) {
            Object headerValue = null;

            for (final String headerName : headers.headerNameSet()) {
                // lazy set content-encoding
                // because if content-encoding(lz4/snappy/kryo...) is set but no parameter/result write to OutputStream,
                // error may happen when read the input stream in sever side.

                if (Names.CONTENT_ENCODING.equalsIgnoreCase(headerName)) {
                    continue;
                }

                headerValue = headers.get(headerName);

                connection.setRequestProperty(headerName, HttpHeaders.valueOf(headerValue));
            }
        }
    }

    void close(final OutputStream os, final InputStream is, @SuppressWarnings("unused") final HttpURLConnection connection) { //NOSONAR
        try {
            IOUtil.closeQuietly(os);
            IOUtil.closeQuietly(is);
        } finally {
            _activeConnectionCounter.decrementAndGet();
        }

        // connection.disconnect();
    }

    /**
     * Performs an asynchronous GET request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * client.asyncGet()
     *     .thenAccept(response -> System.out.println("Response: " + response))
     *     .exceptionally(e -> { e.printStackTrace(); return null; });
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the response body
     */
    public ContinuableFuture<String> asyncGet() {
        return asyncGet(String.class);
    }

    /**
     * Performs an asynchronous GET request with custom settings.
     * Returns the response as a String asynchronously.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123");
     * client.asyncGet(settings)
     *     .thenAccept(response -> System.out.println("Response: " + response));
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncGet(final HttpSettings settings) {
        return asyncGet(settings, String.class);
    }

    /**
     * Performs an asynchronous GET request with query parameters.
     * Returns the response as a String asynchronously.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("id", 123, "name", "John");
     * client.asyncGet(params)
     *     .thenAccept(response -> System.out.println("Response: " + response));
     * }</pre>
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters) {
        return asyncGet(queryParameters, String.class);
    }

    /**
     * Performs an asynchronous GET request with query parameters and custom settings.
     * Returns the response as a String asynchronously.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 10);
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123");
     * client.asyncGet(params, settings)
     *     .thenAccept(response -> System.out.println("Response: " + response));
     * }</pre>
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncGet(final Object queryParameters, final HttpSettings settings) {
        return asyncGet(queryParameters, settings, String.class);
    }

    /**
     * Performs an asynchronous GET request and deserializes the response to the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * client.asyncGet(User.class)
     *     .thenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncGet(final Class<T> resultClass) {
        return asyncGet(null, _settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with query parameters and deserializes the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("userId", 123);
     * client.asyncGet(params, User.class)
     *     .thenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final Class<T> resultClass) {
        return asyncGet(queryParameters, _settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with custom settings and deserializes the response.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .setContentFormat(ContentFormat.JSON)
     *     .header("Authorization", "Bearer token123");
     * client.asyncGet(settings, User.class)
     *     .thenAccept(user -> System.out.println("User: " + user.getName()));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncGet(final HttpSettings settings, final Class<T> resultClass) {
        return asyncGet(null, settings, resultClass);
    }

    /**
     * Performs an asynchronous GET request with all options and deserializes the response.
     * This is the most comprehensive GET method allowing full control over query parameters,
     * settings, and response deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("page", 1, "size", 20);
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123")
     *     .setConnectionTimeout(10000);
     * client.asyncGet(params, settings, UserList.class)
     *     .thenAccept(users -> users.forEach(u -> System.out.println(u.getName())));
     * }</pre>
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncGet(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.GET, queryParameters, settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request and returns the response as a String.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * client.asyncDelete()
     *     .thenAccept(response -> System.out.println("Deleted: " + response));
     * }</pre>
     *
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete() {
        return asyncDelete(String.class);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> params = Map.of("id", 123);
     * client.asyncDelete(params)
     *     .thenAccept(response -> System.out.println("Deleted: " + response));
     * }</pre>
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters) {
        return asyncDelete(queryParameters, String.class);
    }

    /**
     * Performs an asynchronous DELETE request with custom settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * HttpSettings settings = HttpSettings.create()
     *     .header("Authorization", "Bearer token123");
     * client.asyncDelete(settings)
     *     .thenAccept(response -> System.out.println("Deleted: " + response));
     * }</pre>
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete(final HttpSettings settings) {
        return asyncDelete(settings, String.class);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters and custom settings.
     *
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncDelete(final Object queryParameters, final HttpSettings settings) {
        return asyncDelete(queryParameters, settings, String.class);
    }

    /**
     * Performs an asynchronous DELETE request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final Class<T> resultClass) {
        return asyncDelete(null, _settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with query parameters and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final Class<T> resultClass) {
        return asyncDelete(queryParameters, _settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with custom settings and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final HttpSettings settings, final Class<T> resultClass) {
        return asyncDelete(null, settings, resultClass);
    }

    /**
     * Performs an asynchronous DELETE request with all options and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param queryParameters Query parameters as a String, Map, or Bean object (will be URL-encoded)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncDelete(final Object queryParameters, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.DELETE, queryParameters, settings, resultClass);
    }

    /**
     * Performs an asynchronous POST request with the specified request body.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User newUser = new User("John", "Doe");
     * client.asyncPost(newUser)
     *     .thenAccept(response -> System.out.println("Created: " + response));
     * }</pre>
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPost(final Object request) {
        return asyncPost(request, String.class);
    }

    /**
     * Performs an asynchronous POST request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final Class<T> resultClass) {
        return asyncPost(request, _settings, resultClass);
    }

    /**
     * Performs an asynchronous POST request with custom settings and returns the response as a String.
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPost(final Object request, final HttpSettings settings) {
        return asyncPost(request, settings, String.class);
    }

    /**
     * Performs an asynchronous POST request with custom settings and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPost(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.POST, request, settings, resultClass);
    }

    /**
     * Performs an asynchronous PUT request with the specified request body.
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPut(final Object request) {
        return asyncPut(request, String.class);
    }

    /**
     * Performs an asynchronous PUT request and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final Class<T> resultClass) {
        return asyncPut(request, _settings, resultClass);
    }

    /**
     * Performs an asynchronous PUT request with custom settings and returns the response as a String.
     *
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncPut(final Object request, final HttpSettings settings) {
        return asyncPut(request, settings, String.class);
    }

    /**
     * Performs an asynchronous PUT request with custom settings and deserializes the response to the specified type.
     *
     * @param <T> The type of the response object
     * @param request The request body (can be String, byte[], File, InputStream, Reader, or any object for serialization)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response object
     */
    public <T> ContinuableFuture<T> asyncPut(final Object request, final HttpSettings settings, final Class<T> resultClass) {
        return asyncExecute(HttpMethod.PUT, request, settings, resultClass);
    }

    /**
     * Performs an asynchronous HEAD request with default settings.
     *
     * @return A ContinuableFuture that will complete when the request finishes
     */
    public ContinuableFuture<Void> asyncHead() {
        return asyncHead(_settings);
    }

    /**
     * Performs an asynchronous HEAD request with custom settings.
     *
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete when the request finishes
     */
    public ContinuableFuture<Void> asyncHead(final HttpSettings settings) {
        return asyncExecute(HttpMethod.HEAD, null, settings, Void.class);
    }

    /**
     * Executes an asynchronous HTTP request with the specified method and request body, returning the response as a String.
     * Executes the request asynchronously without blocking the calling thread.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request) {
        return asyncExecute(httpMethod, request, String.class);
    }

    /**
     * Executes an asynchronous HTTP request and deserializes the response to the specified type.
     * Executes the request asynchronously without blocking the calling thread.
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final Class<T> resultClass) {
        return asyncExecute(httpMethod, request, _settings, resultClass);
    }

    /**
     * Executes an asynchronous HTTP request with custom settings and returns the response as a String.
     * Executes the request asynchronously with the specified HTTP settings.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @return A ContinuableFuture that will complete with the response body as a String
     */
    public ContinuableFuture<String> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings) {
        return asyncExecute(httpMethod, request, settings, String.class);
    }

    /**
     * Executes an asynchronous HTTP request with all options and deserializes the response to the specified type.
     * This is the core async method that all other async request methods delegate to.
     * Executes the request asynchronously with full control over method, request body, and settings.
     *
     * @param <T> The type of the response object
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param resultClass The class of the expected response object (for deserialization)
     * @return A ContinuableFuture that will complete with the deserialized response
     */
    public <T> ContinuableFuture<T> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Class<T> resultClass) {
        final Callable<T> cmd = () -> execute(httpMethod, request, settings, resultClass);

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response to a file.
     * Executes the request asynchronously and writes the response body directly to the specified file.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The file to write the response to
     * @return A ContinuableFuture that will complete when the file is written successfully
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final File output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response to an output stream.
     * Executes the request asynchronously and writes the response body to the stream. The stream is not closed by this method.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The output stream to write the response to
     * @return A ContinuableFuture that will complete when the stream is written successfully
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final OutputStream output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Executes an asynchronous HTTP request and writes the response to a writer.
     * Executes the request asynchronously and writes the response body to the writer. The writer is not closed by this method.
     *
     * @param httpMethod The HTTP method to use (GET, POST, PUT, DELETE, HEAD, etc.)
     * @param request The request body (can be {@code null} for GET/DELETE)
     * @param settings Additional HTTP settings for this request (headers, timeouts, etc.)
     * @param output The writer to write the response to
     * @return A ContinuableFuture that will complete when the writer is written successfully
     */
    public ContinuableFuture<Void> asyncExecute(final HttpMethod httpMethod, final Object request, final HttpSettings settings, final Writer output) {
        final Callable<Void> cmd = () -> {
            execute(httpMethod, request, settings, output);

            return null;
        };

        return _asyncExecutor.execute(cmd);
    }

    /**
     * Closes this HTTP client and releases any resources.
     * Note: Currently this method does nothing as connections are managed per-request.
     * The method is provided for API consistency and future enhancements.
     */
    public synchronized void close() {
        // do nothing.
    }
}
