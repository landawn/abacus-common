package com.landawn.abacus.http;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

public class HARUtil {

    private static final Logger logger = LoggerFactory.getLogger(HARUtil.class);

    private static final BiPredicate<String, String> defaultHttpHeaderFilterForHARRequest = HttpUtil::isValidHttpHeader;

    private static final ThreadLocal<BiPredicate<String, String>> httpHeaderFilterForHARRequest_TL = ThreadLocal
            .withInitial(() -> defaultHttpHeaderFilterForHARRequest);

    private static final Consumer<String> defaultCurlLogHandler = curl -> {
        if (logger.isInfoEnabled()) {
            logger.info(curl);
        }
    };

    private static final ThreadLocal<Tuple3<Boolean, Character, Consumer<String>>> logRequestCurlForHARRequest_TL = ThreadLocal
            .withInitial(() -> Tuple.of(false, '\'', defaultCurlLogHandler));

    public static void setHttpHeaderFilterForHARRequest(final BiPredicate<String, String> httpHeaderFilterForHARRequest) {
        N.checkArgNotNull(httpHeaderFilterForHARRequest, "httpHeaderFilterForHARRequest");

        httpHeaderFilterForHARRequest_TL.set(httpHeaderFilterForHARRequest);
    }

    public static void resetHttpHeaderFilterForHARRequest() {
        httpHeaderFilterForHARRequest_TL.set(defaultHttpHeaderFilterForHARRequest);
    }

    public static void logRequestCurlForHARRequest(final boolean logRequest) {
        logRequestCurlForHARRequest(logRequest, '\'');
    }

    public static void logRequestCurlForHARRequest(final boolean logRequest, char quoteChar) {
        logRequestCurlForHARRequest_TL.set(Tuple.of(logRequest, quoteChar, defaultCurlLogHandler));
    }

    public static void logRequestCurlForHARRequest(final boolean logRequest, char quoteChar, Consumer<String> logHandler) {
        logRequestCurlForHARRequest_TL.set(Tuple.of(logRequest, quoteChar, logHandler));
    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param targetUrl
     * @return
     */
    public static String sendRequstByHAR(File har, String targetUrl) {
        return sendRequstByHAR(har, Fn.equal(targetUrl));
    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param filterForTargetUrl
     * @return
     */
    public static String sendRequstByHAR(final File har, final Predicate<String> filterForTargetUrl) {
        return sendRequstByHAR(IOUtil.readString(har), filterForTargetUrl);
    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param targetUrl
     * @return
     */
    public static String sendRequstByHAR(String har, String targetUrl) {
        return sendRequstByHAR(har, Fn.equal(targetUrl));
    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param filterForTargetUrl
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static String sendRequstByHAR(final String har, final Predicate<String> filterForTargetUrl) {
        Map map = N.fromJSON(Map.class, har);
        List<Map> entries = Maps.getByPath(map, "log.entries");

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request"))
                // .peek(m -> N.println(m.get("url")))
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .map(requestEntry -> sendRequestByRequestEntry(requestEntry, String.class))
                .first()
                .orElseThrow();

    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param filterForTargetUrl
     * @return
     */
    public static List<String> sendMultiRequstsByHAR(final File har, final Predicate<String> filterForTargetUrl) {
        return sendMultiRequstsByHAR(IOUtil.readString(har), filterForTargetUrl);
    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param filterForTargetUrl
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static List<String> sendMultiRequstsByHAR(final String har, final Predicate<String> filterForTargetUrl) {
        Map map = N.fromJSON(Map.class, har);
        List<Map> entries = Maps.getByPath(map, "log.entries");

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request"))
                // .peek(m -> N.println(m.get("url")))
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .map(requestEntry -> sendRequestByRequestEntry(requestEntry, String.class))
                .toList();

    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param filterForTargetUrl
     * @return first element in the returned {@code Tuple2} is {@code url}. The second element is HttpResponse.
     */
    public static Stream<Tuple2<Map<String, Object>, HttpResponse>> streamMultiRequstsByHAR(final File har, final Predicate<String> filterForTargetUrl) {
        return streamMultiRequstsByHAR(IOUtil.readString(har), filterForTargetUrl);
    }

    /**
     * http://www.softwareishard.com/har/viewer/
     * <br />
     * https://confluence.atlassian.com/kb/generating-har-files-and-analyzing-web-requests-720420612.html
     *
     * @param har
     * @param filterForTargetUrl
     * @return first element in the returned {@code Tuple2} is {@code url}. The second element is HttpResponse.
     */
    @SuppressWarnings("rawtypes")
    public static Stream<Tuple2<Map<String, Object>, HttpResponse>> streamMultiRequstsByHAR(final String har, final Predicate<String> filterForTargetUrl) {
        Map map = N.fromJSON(Map.class, har);
        List<Map> entries = Maps.getByPath(map, "log.entries");

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request"))
                // .peek(m -> N.println(m.get("url")))
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .map(requestEntry -> Tuple.of(requestEntry, sendRequestByRequestEntry(requestEntry, HttpResponse.class)));

    }

    public static <T> T sendRequestByRequestEntry(final Map<String, Object> requestEntry, final Class<T> responseClass) {
        final String url = getUrlByRequestEntry(requestEntry);
        final HttpMethod httpMethod = getHttpMethodByRequestEntry(requestEntry);

        final HttpHeaders httpHeaders = getHeadersByRequestEntry(requestEntry);

        final String requestBody = Maps.getByPath(requestEntry, "postData.text");
        final String bodyType = Maps.getByPath(requestEntry, "postData.mimeType");

        if (N.notNullOrEmpty(requestBody)) {
            WebUtil.setContentTypeByRequestBodyType(bodyType, httpHeaders);
        }

        final Tuple3<Boolean, Character, Consumer<String>> tp = logRequestCurlForHARRequest_TL.get();

        if (tp._1.booleanValue() && (tp._3 != defaultCurlLogHandler || logger.isInfoEnabled())) {
            tp._3.accept(WebUtil.buildCurl(httpMethod.name(), url, httpHeaders.toMap(), requestBody, bodyType, tp._2));
        }

        return HttpRequest.url(url).headers(httpHeaders).execute(responseClass, httpMethod, requestBody);
    }

    public static Optional<Map<String, Object>> getRequestEntryByUrlFromHAR(final File har, final Predicate<String> filterForTargetUrl) {
        return getRequestEntryByUrlFromHAR(IOUtil.readString(har), filterForTargetUrl);
    }

    @SuppressWarnings("rawtypes")
    public static Optional<Map<String, Object>> getRequestEntryByUrlFromHAR(final String har, final Predicate<String> filterForTargetUrl) {
        Map map = N.fromJSON(Map.class, har);
        List<Map> entries = Maps.getByPath(map, "log.entries");

        return Stream.of(entries) //
                .map(m -> (Map<String, Object>) m.get("request"))
                .filter(m -> filterForTargetUrl.test((String) m.get("url")))
                .first();
    }

    public static String getUrlByRequestEntry(final Map<String, Object> requestEntry) {
        return (String) requestEntry.get("url");
    }

    public static HttpMethod getHttpMethodByRequestEntry(final Map<String, Object> requestEntry) {
        return HttpMethod.valueOf(requestEntry.get("method").toString().toUpperCase());
    }

    public static HttpHeaders getHeadersByRequestEntry(final Map<String, Object> requestEntry) {
        final BiPredicate<String, String> httpHeaderValidatorForHARRequest = httpHeaderFilterForHARRequest_TL.get();
        final HttpHeaders httpHeaders = HttpHeaders.create();
        final List<Map<String, String>> headers = (List<Map<String, String>>) requestEntry.get("headers");
        String headerName = null;
        String headerValue = null;

        for (Map<String, String> m : headers) {
            headerName = m.get("name");
            headerValue = m.get("value");

            if (httpHeaderValidatorForHARRequest.test(headerName, headerValue)) {
                httpHeaders.set(headerName, headerValue);
            }
        }

        return httpHeaders;
    }

    public static Tuple2<String, String> getBodyAndMimeTypeByRequestEntry(final Map<String, Object> requestEntry) {
        final String requestBody = Maps.getByPath(requestEntry, "postData.text");
        final String bodyType = Maps.getByPath(requestEntry, "postData.mimeType");

        return Tuple.of(requestBody, bodyType);
    }

    private HARUtil() {
        // Utility class.
    }
}