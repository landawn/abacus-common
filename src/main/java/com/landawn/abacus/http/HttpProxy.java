/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.core.DirtyMarkerUtil;
import com.landawn.abacus.core.MapEntity;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.DeserializationConfig;
import com.landawn.abacus.parser.Parser;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.AndroidUtil;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.function.Function;

/**
 * The client and server communicate by xml/json(may compressed by lz4/snappy/gzip)
 * through http. There are two ways to send the request: <li>1, Send the request
 * with the url. The target web method is identified by request type.</li> <li>
 * 2, Send the request with the url+'/'+operationName. The target web method is
 * identified by operation name in the url.</li>
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class HttpProxy {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HttpProxy.class);

    /** The Constant DEFAULT_MAX_CONNECTION. */
    private static final int DEFAULT_MAX_CONNECTION = AbstractHttpClient.DEFAULT_MAX_CONNECTION;

    /** The Constant DEFAULT_CONNECTION_TIMEOUT. */
    private static final int DEFAULT_CONNECTION_TIMEOUT = AbstractHttpClient.DEFAULT_CONNECTION_TIMEOUT;

    /** The Constant DEFAULT_READ_TIMEOUT. */
    private static final int DEFAULT_READ_TIMEOUT = AbstractHttpClient.DEFAULT_READ_TIMEOUT;

    /** The Constant PARAM. */
    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";

    /** The Constant PARAM_NAME_REGEX. */
    private static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

    /** The Constant PARAM_URL_REGEX. */
    private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass
     * @param contentFormat
     * @param url
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final ContentFormat contentFormat, final String url) {
        return createClientProxy(interfaceClass, contentFormat, url, DEFAULT_MAX_CONNECTION, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass
     * @param contentFormat
     * @param url
     * @param config
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final ContentFormat contentFormat, final String url, final Config config) {
        return createClientProxy(interfaceClass, contentFormat, url, DEFAULT_MAX_CONNECTION, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT, config);
    }

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass
     * @param contentFormat
     * @param url
     * @param maxConnection
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final ContentFormat contentFormat, final String url, final int maxConnection) {
        return createClientProxy(interfaceClass, contentFormat, url, maxConnection, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass
     * @param contentFormat
     * @param url
     * @param maxConnection
     * @param connTimeout
     * @param readTimeout
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final ContentFormat contentFormat, final String url, final int maxConnection,
            final long connTimeout, final long readTimeout) {
        return createClientProxy(interfaceClass, contentFormat, url, maxConnection, connTimeout, readTimeout, null);
    }

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass
     * @param contentFormat
     * @param url
     * @param maxConnection
     * @param connTimeout
     * @param readTimeout
     * @param config
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final ContentFormat contentFormat, final String url, final int maxConnection,
            final long connTimeout, final long readTimeout, final Config config) {

        if (contentFormat == null || contentFormat == ContentFormat.NONE) {
            throw new IllegalArgumentException("Content format can't be null or NONE");
        }

        InvocationHandler h = new InvocationHandler() {
            private final Logger _logger = LoggerFactory.getLogger(interfaceClass);
            private final ContentFormat _contentFormat = contentFormat;
            private final String _url = url;
            private final int _maxConnection = maxConnection;
            private final long _connTimeout = connTimeout;
            private final long _readTimeout = readTimeout;
            private final Config _config = config == null ? new Config() : N.copy(config);

            {
                final Set<Method> declaredMethods = N.asLinkedHashSet(interfaceClass.getDeclaredMethods());

                for (Class<?> superClass : interfaceClass.getInterfaces()) {
                    declaredMethods.addAll(Arrays.asList(superClass.getDeclaredMethods()));
                }

                if (_config.parser == null) {
                    _config.setParser(HTTP.getParser(_contentFormat));
                }

                // set operation configuration.
                final Map<String, OperationConfig> newOperationConfigs = new HashMap<>(N.initHashCapacity(declaredMethods.size()));
                if (config != null && N.notNullOrEmpty(config.operationConfigs)) {
                    for (Map.Entry<String, OperationConfig> entry : config.operationConfigs.entrySet()) {
                        OperationConfig copy = entry.getValue() == null ? new OperationConfig() : N.copy(entry.getValue());

                        if (entry.getValue() != null && entry.getValue().getRequestSettings() != null) {
                            copy.setRequestSettings(entry.getValue().getRequestSettings().copy());
                        }

                        newOperationConfigs.put(entry.getKey(), copy);
                    }
                }
                _config.setOperationConfigs(newOperationConfigs);

                for (Method method : declaredMethods) {
                    final String methodName = method.getName();
                    final Class<?>[] parameterTypes = method.getParameterTypes();
                    final int parameterCount = parameterTypes.length;

                    OperationConfig operationConfig = _config.operationConfigs.get(methodName);

                    if (operationConfig == null) {
                        operationConfig = new OperationConfig();
                        _config.operationConfigs.put(methodName, operationConfig);
                    }

                    operationConfig.requestEntityName = StringUtil.capitalize(methodName) + "Request";
                    operationConfig.responseEntityName = StringUtil.capitalize(methodName) + "Response";

                    RestMethod methodInfo = null;
                    for (Annotation methodAnnotation : method.getAnnotations()) {
                        Class<? extends Annotation> annotationType = methodAnnotation.annotationType();

                        for (Annotation innerAnnotation : annotationType.getAnnotations()) {
                            if (RestMethod.class == innerAnnotation.annotationType()) {
                                methodInfo = (RestMethod) innerAnnotation;

                                break;
                            }
                        }

                        if (methodInfo != null) {
                            if (N.isNullOrEmpty(operationConfig.getRequestUrl())) {
                                try {
                                    String path = (String) annotationType.getMethod("value").invoke(methodAnnotation);

                                    if (N.notNullOrEmpty(path)) {
                                        operationConfig.setRequestUrl(path);
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to extract String 'value' from @%s annotation:" + annotationType.getSimpleName());
                                }
                            }

                            if (operationConfig.getHttpMethod() == null) {
                                operationConfig.setHttpMethod(HttpMethod.valueOf(methodInfo.value()));
                            }

                            break;
                        }
                    }

                    if (N.isNullOrEmpty(operationConfig.paramNameTypeMap)) {
                        operationConfig.paramTypes = new Type[parameterCount];
                        operationConfig.paramFields = new Field[parameterCount];
                        operationConfig.paramPaths = new Path[parameterCount];
                        operationConfig.paramNameTypeMap = new HashMap<>();

                        final Annotation[][] parameterAnnotationArrays = method.getParameterAnnotations();
                        for (int i = 0; i < parameterCount; i++) {
                            operationConfig.paramTypes[i] = N.typeOf(parameterTypes[i]);

                            for (Annotation parameterAnnotation : parameterAnnotationArrays[i]) {
                                if (parameterAnnotation.annotationType() == Field.class) {
                                    operationConfig.paramFields[i] = (Field) parameterAnnotation;

                                    if (operationConfig.paramNameTypeMap.put(operationConfig.paramFields[i].value(), operationConfig.paramTypes[i]) != null) {
                                        throw new RuntimeException("Duplicated parameter names: " + operationConfig.paramFields[i].value());
                                    }
                                } else if (parameterAnnotation.annotationType() == Path.class) {
                                    operationConfig.validatePathName(((Path) parameterAnnotation).value());

                                    operationConfig.paramPaths[i] = (Path) parameterAnnotation;

                                    if (operationConfig.paramNameTypeMap.put(operationConfig.paramPaths[i].value(), operationConfig.paramTypes[i]) != null) {
                                        throw new RuntimeException("Duplicated parameter names: " + operationConfig.paramPaths[i].value());
                                    }
                                }
                            }
                        }
                    }

                    if (operationConfig.httpMethod == null) {
                        operationConfig.httpMethod = HttpMethod.POST;
                    } else if (!(operationConfig.httpMethod == HttpMethod.GET || operationConfig.httpMethod == HttpMethod.POST
                            || operationConfig.httpMethod == HttpMethod.PUT || operationConfig.httpMethod == HttpMethod.DELETE)) {
                        throw new IllegalArgumentException("Unsupported http method: " + operationConfig.httpMethod);
                    }

                    if (parameterCount > 1 && operationConfig.paramNameTypeMap.isEmpty()) {
                        throw new IllegalArgumentException("Unsupported web service method: " + method.getName()
                                + ". Only one parameter or multi parameters with Field/Path annotaions are supported");
                    }

                    if (N.notNullOrEmpty(operationConfig.requestUrl)) {
                        if (StringUtil.startsWithIgnoreCase(operationConfig.requestUrl, "http:")
                                || StringUtil.startsWithIgnoreCase(operationConfig.requestUrl, "https:")) {
                            // no action took
                        } else {
                            if (_url.endsWith("/") || _url.endsWith("\\")) {
                                if (operationConfig.requestUrl.startsWith("/") || operationConfig.requestUrl.startsWith("\\")) {
                                    operationConfig.requestUrl = _url + operationConfig.requestUrl.substring(1);
                                } else {
                                    operationConfig.requestUrl = _url + operationConfig.requestUrl;
                                }
                            } else {
                                if (operationConfig.requestUrl.startsWith("/") || operationConfig.requestUrl.startsWith("\\")) {
                                    operationConfig.requestUrl = _url + operationConfig.requestUrl;
                                } else {
                                    operationConfig.requestUrl = _url + "/" + operationConfig.requestUrl;
                                }
                            }
                        }
                    } else if (_config.requestByOperatioName) {
                        String operationNameUrl = null;

                        if (_config.requestUrlNamingPolicy == NamingPolicy.LOWER_CASE_WITH_UNDERSCORE) {
                            operationNameUrl = ClassUtil.toLowerCaseWithUnderscore(methodName);
                        } else if (_config.requestUrlNamingPolicy == NamingPolicy.UPPER_CASE_WITH_UNDERSCORE) {
                            operationNameUrl = ClassUtil.toUpperCaseWithUnderscore(methodName);
                        } else {
                            operationNameUrl = methodName;
                        }

                        if (_url.endsWith("/") || _url.endsWith("\\")) {
                            operationConfig.requestUrl = _url + operationNameUrl;
                        } else {
                            operationConfig.requestUrl = _url + "/" + operationNameUrl;
                        }

                    } else {
                        operationConfig.requestUrl = _url;
                    }

                    if ((N.notNullOrEmpty(_config.getEncryptionUserName()) || N.notNullOrEmpty(_config.getEncryptionPassword()))
                            && (N.isNullOrEmpty(operationConfig.getEncryptionUserName()) && N.isNullOrEmpty(operationConfig.getEncryptionPassword()))) {
                        if (N.isNullOrEmpty(operationConfig.getEncryptionUserName())) {
                            operationConfig.setEncryptionUserName(_config.getEncryptionUserName());
                        }

                        if (N.isNullOrEmpty(operationConfig.getEncryptionPassword())) {
                            operationConfig.setEncryptionPassword(_config.getEncryptionPassword());
                        }

                        if (operationConfig.getEncryptionMessage() == null) {
                            operationConfig.setEncryptionMessage(_config.getEncryptionMessage());
                        }

                        if (operationConfig.getEncryptionMessage() == null) {
                            operationConfig.setEncryptionMessage(MessageEncryption.NONE);
                        }
                    }
                }

                if (config != null && config.getRequestSettings() != null) {
                    _config.setRequestSettings(config.getRequestSettings().copy());
                }
            }

            private final AtomicInteger sharedActiveConnectionCounter = new AtomicInteger(0);
            private final Map<String, HttpClient> _httpClientPool = new HashMap<>(N.initHashCapacity(_config.operationConfigs.size()));
            private final HttpClient _httpClient = HttpClient.create(_url, _maxConnection, _connTimeout, _readTimeout, _config.getRequestSettings(),
                    sharedActiveConnectionCounter);

            private final Executor _asyncExecutor;
            {
                Executor executor = null;

                if (_config.executedByThreadPool) {
                    if (_config.getAsyncExecutor() != null) {
                        executor = _config.getAsyncExecutor();
                    } else if (IOUtil.IS_PLATFORM_ANDROID) {
                        executor = AndroidUtil.getThreadPoolExecutor();
                    } else {
                        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Math.max(8, IOUtil.CPU_CORES), Math.max(16, IOUtil.CPU_CORES),
                                180L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
                        threadPoolExecutor.allowCoreThreadTimeOut(true);

                        executor = threadPoolExecutor;
                    }
                }

                _asyncExecutor = executor;

                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        if (_asyncExecutor instanceof ExecutorService) {
                            final ExecutorService executorService = (ExecutorService) _asyncExecutor;

                            logger.warn("Starting to shutdown task in HttpProxy");

                            try {
                                executorService.shutdown();

                                executorService.awaitTermination(60, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                logger.warn("Not all the requests/tasks executed in HttpProxy are completed successfully before shutdown.");
                            } finally {
                                logger.warn("Completed to shutdown task in HttpProxy");
                            }
                        }
                    }
                });
            }

            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                // If the method is a method from Object then defer to normal invocation.
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(this, args);
                }

                final String methodName = method.getName();
                final OperationConfig operationConfig = _config.operationConfigs.get(methodName);

                if (operationConfig.getRetryTimes() > 0) {
                    try {
                        return invoke(method, args);
                    } catch (Exception e) {
                        _logger.error("Failed to call: " + method.getName(), e);

                        final int retryTimes = operationConfig.getRetryTimes();
                        final long retryInterval = operationConfig.getRetryInterval();
                        final Function<Throwable, Boolean> ifRetry = operationConfig.getIfRetry();

                        int retriedTimes = 0;
                        Throwable throwable = e;

                        while (retriedTimes++ < retryTimes && (ifRetry == null || ifRetry.apply(e).booleanValue())) {
                            try {
                                if (retryInterval > 0) {
                                    N.sleep(retryInterval);
                                }

                                return invoke(method, args);
                            } catch (Exception e2) {
                                throwable = e2;
                            }
                        }

                        throw N.toRuntimeException(throwable);
                    }
                } else {
                    return invoke(method, args);
                }
            }

            private Object invoke(final Method method, final Object[] args) throws InterruptedException, ExecutionException {
                if (_config.executedByThreadPool) {
                    final Callable<Object> cmd = new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            if (_config.handler == null) {
                                return execute(method, args);
                            } else {
                                _config.handler.preInvoke(method, args);

                                Object result = null;
                                Throwable exception = null;

                                try {
                                    result = execute(method, args);
                                } catch (Exception e) {
                                    exception = e;
                                } finally {
                                    result = _config.handler.postInvoke(exception, result, method, args);
                                }

                                return result;
                            }
                        }
                    };

                    final FutureTask<Object> futureTask = new FutureTask<>(cmd);
                    _asyncExecutor.execute(futureTask);
                    return futureTask.get();
                } else {
                    if (_config.handler == null) {
                        return execute(method, args);
                    } else {
                        _config.handler.preInvoke(method, args);

                        Object result = null;
                        Throwable exception = null;

                        try {
                            result = execute(method, args);
                        } catch (Exception e) {
                            exception = e;
                        } finally {
                            result = _config.handler.postInvoke(exception, result, method, args);
                        }

                        return result;
                    }
                }
            }

            private Object execute(final Method method, final Object[] args) {
                final String methodName = method.getName();
                final OperationConfig operationConfig = _config.operationConfigs.get(methodName);
                final Object requestParameter = readRequestParameter(args, operationConfig);

                if (N.notNullOrEmpty(operationConfig.getEncryptionUserName()) && N.notNullOrEmpty(operationConfig.getEncryptionPassword())) {
                    ((SecurityDTO) requestParameter).encrypt(operationConfig.getEncryptionUserName(), operationConfig.getEncryptionPassword(),
                            operationConfig.getEncryptionMessage());
                }

                final HttpClient httpClient = getHttpClient(method, requestParameter, operationConfig);
                final Class<T> returnType = (Class<T>) method.getReturnType();

                if (_logger.isInfoEnabled()) {
                    _logger.info(_config.parser.serialize(requestParameter, _config.sc));
                }

                InputStream is = null;
                OutputStream os = null;

                final HttpURLConnection connection = httpClient.openConnection(operationConfig.httpMethod, operationConfig.requestSettings);

                try {
                    if (requestParameter != null && (operationConfig.httpMethod == HttpMethod.POST || operationConfig.httpMethod == HttpMethod.PUT)) {
                        os = HTTP.getOutputStream(connection, _contentFormat);

                        switch (_contentFormat) {
                            case JSON:
                            case JSON_LZ4:
                            case JSON_SNAPPY:
                            case JSON_GZIP:
                                Type<Object> type = N.typeOf(requestParameter.getClass());

                                if (type.isSerializable()) {
                                    os.write(type.stringOf(requestParameter).getBytes());
                                } else {
                                    _config.parser.serialize(os, requestParameter, _config.sc);
                                }

                                break;

                            case XML:
                            case XML_LZ4:
                            case XML_SNAPPY:
                            case XML_GZIP:
                                if (requestParameter instanceof Map) {
                                    _config.parser.serialize(os, MapEntity.valueOf(operationConfig.requestEntityName, (Map<String, Object>) requestParameter),
                                            _config.sc);
                                } else {
                                    _config.parser.serialize(os, requestParameter, _config.sc);
                                }

                                break;

                            case KRYO:
                                _config.parser.serialize(os, requestParameter, _config.sc);

                                break;

                            default:
                                throw new IllegalArgumentException("Unsupported content type: " + _contentFormat.toString());
                        }

                        HTTP.flush(os);
                    } else {
                        String contentType = HTTP.getContentType(_contentFormat);
                        // TODO

                        if (N.notNullOrEmpty(contentType)) {
                            connection.setRequestProperty(HttpHeaders.Names.CONTENT_TYPE, contentType);
                        }

                        String contentEncoding = HTTP.getContentEncoding(_contentFormat);

                        if (N.notNullOrEmpty(contentEncoding)) {
                            connection.setRequestProperty(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
                        }
                    }

                    final ContentFormat responseContentFormat = HTTP.getContentFormat(connection);
                    final Map<String, List<String>> respHeaders = connection.getHeaderFields();
                    final Charset charset = HTTP.getCharset(respHeaders);
                    final Parser<SerializationConfig<?>, DeserializationConfig<?>> responseParser = responseContentFormat == _contentFormat ? _config.parser
                            : HTTP.getParser(responseContentFormat);

                    is = HTTP.getInputStream(connection, responseContentFormat);

                    if (void.class.equals(returnType)) {
                        return null;
                    } else {
                        T result = null;
                        Type<T> type = null;

                        switch (responseContentFormat) {
                            case JSON:
                            case JSON_LZ4:
                            case JSON_SNAPPY:
                            case JSON_GZIP:

                                type = N.typeOf(returnType);

                                if (type.isSerializable()) {
                                    result = type.valueOf(IOUtil.readString(is, charset));
                                } else {
                                    result = responseParser.deserialize(returnType, IOUtil.newBufferedReader(is, charset), _config.dc);
                                }

                                break;

                            case XML:
                            case XML_LZ4:
                            case XML_SNAPPY:
                            case XML_GZIP:
                                result = responseParser.deserialize(returnType, IOUtil.newBufferedReader(is, charset), _config.dc);
                                break;

                            case KRYO:
                                result = responseParser.deserialize(returnType, is, _config.dc);
                                break;

                            default:
                                throw new IllegalArgumentException("Unsupported content type: " + responseContentFormat.toString());
                        }

                        if (_logger.isInfoEnabled()) {
                            if (type != null && type.isSerializable()) {
                                _logger.info(type.stringOf(result));
                            } else {
                                _logger.info(_config.parser.serialize(result, _config.sc));
                            }
                        }

                        return result;
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    httpClient.close(os, is, connection);
                }
            }

            private Object readRequestParameter(final Object[] args, final OperationConfig operationConfig) {
                if (N.isNullOrEmpty(args)) {
                    return null;
                } else if (operationConfig.paramNameTypeMap.isEmpty()) {
                    return args[0];
                } else {
                    final Map<String, Object> parameterMap = new HashMap<>();

                    for (int i = 0, len = args.length; i < len; i++) {
                        if (operationConfig.paramFields[i] != null) {
                            parameterMap.put(operationConfig.paramFields[i].value(), args[i]);
                        } else if (operationConfig.paramPaths[i] != null) {
                            parameterMap.put(operationConfig.paramPaths[i].value(), args[i] == null ? "null"
                                    : (operationConfig.paramPaths[i].encode() ? N.urlEncode(N.stringOf(args[i])) : N.stringOf(args[i])));
                        }
                    }

                    return parameterMap;
                }
            }

            private HttpClient getHttpClient(final Method method, final Object parameter, final OperationConfig operationConfig) {
                final String methodName = method.getName();
                HttpClient httpClient = null;

                if (operationConfig.httpMethod == HttpMethod.POST || operationConfig.httpMethod == HttpMethod.PUT || parameter == null) {
                    synchronized (_httpClientPool) {
                        httpClient = _httpClientPool.get(methodName);

                        if (httpClient == null) {
                            if (operationConfig.requestUrl.equals(_url)) {
                                httpClient = _httpClient;
                            } else {
                                httpClient = HttpClient.create(operationConfig.requestUrl, _maxConnection, _connTimeout, _readTimeout,
                                        _config.getRequestSettings(), sharedActiveConnectionCounter);
                            }

                            _httpClientPool.put(methodName, httpClient);
                        }
                    }
                } else {
                    final Type<?> type = N.typeOf(parameter.getClass());
                    String requestUrl = operationConfig.requestUrl;

                    if (N.notNullOrEmpty(operationConfig.requestUrlParamNames)) {
                        final Map<String, Object> m = (Map<String, Object>) parameter;

                        for (Map.Entry<String, String> entry : operationConfig.requestUrlParamNames.entrySet()) {
                            requestUrl = requestUrl.replace(entry.getValue(), m.remove(entry.getKey()).toString());
                        }
                    }

                    if (type.isMap() && ((Map<String, Object>) parameter).size() == 0) {
                        // ignore.
                    } else {
                        if (_config.requestParamNamingPolicy == null || _config.requestParamNamingPolicy == NamingPolicy.LOWER_CAMEL_CASE) {
                            requestUrl = requestUrl + "?" + N.urlEncode(parameter);
                        } else {
                            Object newParameter = parameter;

                            if (type.isEntity()) {
                                newParameter = Maps.entity2Map(parameter, !DirtyMarkerUtil.isDirtyMarker(parameter.getClass()), null,
                                        _config.requestParamNamingPolicy);
                            } else if (type.isMap()) {
                                final Map<String, Object> m = ((Map<String, Object>) parameter);
                                final Map<String, Object> newMap = new LinkedHashMap<>();

                                switch (_config.requestParamNamingPolicy) {
                                    case LOWER_CASE_WITH_UNDERSCORE:
                                    case UPPER_CASE_WITH_UNDERSCORE: {
                                        for (Map.Entry<String, Object> entry : m.entrySet()) {
                                            newMap.put(_config.requestParamNamingPolicy.convert(entry.getKey()), entry.getValue());
                                        }
                                        break;
                                    }

                                    case LOWER_CAMEL_CASE: {
                                        newMap.putAll(m);
                                        break;
                                    }

                                    default:
                                        throw new IllegalArgumentException("Unsupported Naming policy: " + _config.requestParamNamingPolicy);
                                }

                                newParameter = newMap;
                            }

                            requestUrl = requestUrl + "?" + N.urlEncode(newParameter);
                        }
                    }

                    httpClient = HttpClient.create(requestUrl, _maxConnection, _connTimeout, _readTimeout, _config.getRequestSettings(),
                            sharedActiveConnectionCounter);
                }

                return httpClient;
            }
        };

        return (T) N.newProxyInstance(N.asArray(interfaceClass), h);
    }

    /**
     * Gets the set of unique path parameters used in the given URI. If a parameter is used twice
     * in the URI, it will only show up once in the set.
     *
     * @param path
     * @return
     */
    static Set<String> parsePathParameters(final String path) {
        Matcher m = PARAM_URL_REGEX.matcher(path);
        Set<String> patterns = N.newLinkedHashSet();
        while (m.find()) {
            patterns.add(m.group(1));
        }
        return patterns;
    }

    /**
     * The Class Config.
     */
    public static final class Config {

        /** The parser. */
        private Parser<SerializationConfig<?>, DeserializationConfig<?>> parser;

        /** The sc. */
        private SerializationConfig<?> sc;

        /** The dc. */
        private DeserializationConfig<?> dc;

        /** The handler. */
        private Handler handler;

        /** The executed by thread pool. */
        private boolean executedByThreadPool;

        /** The async executor. */
        private Executor asyncExecutor;

        /** The request settings. */
        private HttpSettings requestSettings;

        /** The operation configs. */
        private Map<String, OperationConfig> operationConfigs;

        /** The request by operatio name. */
        private boolean requestByOperatioName;

        /** The request url naming policy. */
        private NamingPolicy requestUrlNamingPolicy;

        /** The request param naming policy. */
        private NamingPolicy requestParamNamingPolicy;

        /** The encryption user name. */
        private String encryptionUserName;

        /** The encryption password. */
        private byte[] encryptionPassword;

        /** The encryption message. */
        private MessageEncryption encryptionMessage;

        /**
         * Gets the parser.
         *
         * @return
         */
        public Parser<SerializationConfig<?>, DeserializationConfig<?>> getParser() {
            return parser;
        }

        /**
         * Sets the parser.
         *
         * @param parser
         * @return
         */
        public Config setParser(final Parser<SerializationConfig<?>, DeserializationConfig<?>> parser) {
            this.parser = parser;

            return this;
        }

        /**
         * Gets the serialization config.
         *
         * @return
         */
        public SerializationConfig<?> getSerializationConfig() {
            return sc;
        }

        /**
         * Sets the serialization config.
         *
         * @param sc
         * @return
         */
        public Config setSerializationConfig(final SerializationConfig<?> sc) {
            this.sc = sc;

            return this;
        }

        /**
         * Gets the deserialization config.
         *
         * @return
         */
        public DeserializationConfig<?> getDeserializationConfig() {
            return dc;
        }

        /**
         * Sets the deserialization config.
         *
         * @param dc
         * @return
         */
        public Config setDeserializationConfig(final DeserializationConfig<?> dc) {
            this.dc = dc;

            return this;
        }

        /**
         * Checks if is executed by thread pool.
         *
         * @return true, if is executed by thread pool
         */
        public boolean isExecutedByThreadPool() {
            return executedByThreadPool;
        }

        /**
         * Sets the executed by thread pool.
         *
         * @param executedByThreadPool
         * @return
         */
        public Config setExecutedByThreadPool(final boolean executedByThreadPool) {
            this.executedByThreadPool = executedByThreadPool;

            return this;
        }

        /**
         * Gets the async executor.
         *
         * @return
         */
        public Executor getAsyncExecutor() {
            return asyncExecutor;
        }

        /**
         * Sets the async executor.
         *
         * @param asyncExecutor
         * @return
         */
        public Config setAsyncExecutor(final Executor asyncExecutor) {
            this.asyncExecutor = asyncExecutor;

            return this;
        }

        /**
         * Gets the handler.
         *
         * @return
         */
        public Handler getHandler() {
            return handler;
        }

        /**
         * Sets the handler.
         *
         * @param handler
         * @return
         */
        public Config setHandler(final Handler handler) {
            this.handler = handler;

            return this;
        }

        /**
         * Gets the request settings.
         *
         * @return
         */
        public HttpSettings getRequestSettings() {
            return requestSettings;
        }

        /**
         * Sets the request settings.
         *
         * @param requestSettings
         * @return
         */
        public Config setRequestSettings(final HttpSettings requestSettings) {
            this.requestSettings = requestSettings;

            return this;
        }

        /**
         * Checks if is request by operatio name.
         *
         * @return true, if is request by operatio name
         */
        public boolean isRequestByOperatioName() {
            return requestByOperatioName;
        }

        /**
         * Sets the request by operatio name.
         *
         * @param requestByOperatioName
         * @return
         */
        public Config setRequestByOperatioName(final boolean requestByOperatioName) {
            this.requestByOperatioName = requestByOperatioName;

            return this;
        }

        /**
         * Gets the request url naming policy.
         *
         * @return
         */
        public NamingPolicy getRequestUrlNamingPolicy() {
            return requestUrlNamingPolicy;
        }

        /**
         * Sets the request url naming policy.
         *
         * @param requestUrlNamingPolicy
         * @return
         */
        public Config setRequestUrlNamingPolicy(final NamingPolicy requestUrlNamingPolicy) {
            this.requestUrlNamingPolicy = requestUrlNamingPolicy;

            return this;
        }

        /**
         * Gets the request param naming policy.
         *
         * @return
         */
        public NamingPolicy getRequestParamNamingPolicy() {
            return requestParamNamingPolicy;
        }

        /**
         * Sets the request param naming policy.
         *
         * @param requestParamNamingPolicy
         * @return
         */
        public Config setRequestParamNamingPolicy(final NamingPolicy requestParamNamingPolicy) {
            this.requestParamNamingPolicy = requestParamNamingPolicy;

            return this;
        }

        /**
         * Gets the operation configs.
         *
         * @return
         */
        public Map<String, OperationConfig> getOperationConfigs() {
            return operationConfigs;
        }

        /**
         * Sets the operation configs.
         *
         * @param operationConfigs
         * @return
         */
        public Config setOperationConfigs(final Map<String, OperationConfig> operationConfigs) {
            this.operationConfigs = operationConfigs;

            return this;
        }

        /**
         * Gets the encryption user name.
         *
         * @return
         */
        public String getEncryptionUserName() {
            return encryptionUserName;
        }

        /**
         * The target request DTOs must inherit from <code>SecurityDTO</code>.
         *
         * @param encryptionUserName
         * @return
         * @see SecurityDTO#encrypt(String, String)
         */
        public Config setEncryptionUserName(final String encryptionUserName) {
            this.encryptionUserName = encryptionUserName;

            return this;
        }

        /**
         * Gets the encryption password.
         *
         * @return
         */
        public byte[] getEncryptionPassword() {
            return encryptionPassword;
        }

        /**
         * The target request DTOs must inherit from <code>SecurityDTO</code>.
         *
         * @param encryptionPassword
         * @return
         * @see SecurityDTO#encrypt(String, String)
         */
        public Config setEncryptionPassword(final byte[] encryptionPassword) {
            this.encryptionPassword = encryptionPassword;

            return this;
        }

        /**
         * Gets the encryption message.
         *
         * @return
         */
        public MessageEncryption getEncryptionMessage() {
            return encryptionMessage;
        }

        /**
         * The target request DTOs must inherit from <code>SecurityDTO</code>.
         *
         * @param encryptionMessage
         * @return
         * @see SecurityDTO#encrypt(String, String)
         */
        public Config setEncryptionMessage(final MessageEncryption encryptionMessage) {
            this.encryptionMessage = encryptionMessage;

            return this;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "{parser=" + parser + ", sc=" + sc + ", dc=" + dc + ", handler=" + handler + ", executedByThreadPool=" + executedByThreadPool
                    + ", asyncExecutor=" + asyncExecutor + ", requestSettings=" + requestSettings + ", requestByOperatioName=" + requestByOperatioName
                    + ", requestUrlNamingPolicy=" + requestUrlNamingPolicy + ", requestParamNamingPolicy=" + requestParamNamingPolicy + ", operationConfigs="
                    + operationConfigs + "}";
        }
    }

    /**
     * The Class OperationConfig.
     */
    public static class OperationConfig {

        /** The request url. */
        private String requestUrl;

        /** The http method. */
        private HttpMethod httpMethod;

        /** The request settings. */
        private HttpSettings requestSettings;

        /** The retry times. */
        private int retryTimes;

        /** The retry interval. */
        private long retryInterval;

        /** The if retry. */
        private Function<Throwable, Boolean> ifRetry;

        /** The encryption user name. */
        private String encryptionUserName;

        /** The encryption password. */
        private byte[] encryptionPassword;

        /** The encryption message. */
        private MessageEncryption encryptionMessage;

        /** The request entity name. */
        String requestEntityName;

        /** The response entity name. */
        String responseEntityName;

        /** The request url param names. */
        Map<String, String> requestUrlParamNames;

        /** The param types. */
        Type<?>[] paramTypes;

        /** The param fields. */
        Field[] paramFields;

        /** The param paths. */
        Path[] paramPaths;

        /** The param name type map. */
        Map<String, Type<?>> paramNameTypeMap;

        /**
         * Gets the request url.
         *
         * @return
         */
        public String getRequestUrl() {
            return requestUrl;
        }

        /**
         * Sets the request url.
         *
         * @param requestUrl
         * @return
         */
        public OperationConfig setRequestUrl(final String requestUrl) {
            this.requestUrl = requestUrl;
            requestUrlParamNames = new LinkedHashMap<>();

            Matcher m = PARAM_URL_REGEX.matcher(requestUrl);
            while (m.find()) {
                requestUrlParamNames.put(m.group(1), m.group());
            }

            return this;
        }

        /**
         * Gets the http method.
         *
         * @return
         */
        public HttpMethod getHttpMethod() {
            return httpMethod;
        }

        /**
         * Sets the http method.
         *
         * @param httpMethod
         * @return
         */
        public OperationConfig setHttpMethod(final HttpMethod httpMethod) {
            this.httpMethod = httpMethod;

            return this;
        }

        /**
         * Gets the request settings.
         *
         * @return
         */
        public HttpSettings getRequestSettings() {
            return requestSettings;
        }

        /**
         * Sets the request settings.
         *
         * @param requestSettings
         * @return
         */
        public OperationConfig setRequestSettings(final HttpSettings requestSettings) {
            this.requestSettings = requestSettings;

            return this;
        }

        /**
         * Gets the retry times.
         *
         * @return
         */
        public int getRetryTimes() {
            return retryTimes;
        }

        /**
         * Sets the retry times.
         *
         * @param retryTimes
         * @return
         */
        public OperationConfig setRetryTimes(final int retryTimes) {
            this.retryTimes = retryTimes;

            return this;
        }

        /**
         * Gets the retry interval.
         *
         * @return
         */
        public long getRetryInterval() {
            return retryInterval;
        }

        /**
         * Sets the retry interval.
         *
         * @param retryInterval
         * @return
         */
        public OperationConfig setRetryInterval(final long retryInterval) {
            this.retryInterval = retryInterval;

            return this;
        }

        /**
         * Gets the if retry.
         *
         * @return
         */
        public Function<Throwable, Boolean> getIfRetry() {
            return ifRetry;
        }

        /**
         * Sets the if retry.
         *
         * @param ifRetry
         * @return
         */
        public OperationConfig setIfRetry(final Function<Throwable, Boolean> ifRetry) {
            this.ifRetry = ifRetry;

            return this;
        }

        /**
         * Gets the encryption user name.
         *
         * @return
         */
        public String getEncryptionUserName() {
            return encryptionUserName;
        }

        /**
         * The target request DTOs must inherit from <code>SecurityDTO</code>.
         *
         * @param encryptionUserName
         * @return
         * @see SecurityDTO#encrypt(String, String)
         */
        public OperationConfig setEncryptionUserName(final String encryptionUserName) {
            this.encryptionUserName = encryptionUserName;

            return this;
        }

        /**
         * Gets the encryption password.
         *
         * @return
         */
        public byte[] getEncryptionPassword() {
            return encryptionPassword;
        }

        /**
         * The target request DTOs must inherit from <code>SecurityDTO</code>.
         *
         * @param encryptionPassword
         * @return
         * @see SecurityDTO#encrypt(String, String)
         */
        public OperationConfig setEncryptionPassword(final byte[] encryptionPassword) {
            this.encryptionPassword = encryptionPassword;

            return this;
        }

        /**
         * Gets the encryption message.
         *
         * @return
         */
        public MessageEncryption getEncryptionMessage() {
            return encryptionMessage;
        }

        /**
         * The target request DTOs must inherit from <code>SecurityDTO</code>.
         *
         * @param encryptionMessage
         * @return
         * @see SecurityDTO#encrypt(String, String)
         */
        public OperationConfig setEncryptionMessage(final MessageEncryption encryptionMessage) {
            this.encryptionMessage = encryptionMessage;

            return this;
        }

        /**
         * copied from retrofit under the Apache License, Version 2.0 (the "License");
         *
         * @param name
         */
        void validatePathName(final String name) {
            if (!PARAM_NAME_REGEX.matcher(name).matches()) {
                throw new RuntimeException(String.format("@Path parameter name must match %s. Found: %s", PARAM_URL_REGEX.pattern(), name));
            }

            // Verify URL replacement name is actually present in the URL path.
            if (!requestUrlParamNames.containsKey(name)) {
                throw new RuntimeException(String.format("URL \"%s\" does not contain \"{%s}\".", requestUrl, name));
            }
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((httpMethod == null) ? 0 : httpMethod.hashCode());
            result = prime * result + ((requestUrl == null) ? 0 : requestUrl.hashCode());
            result = prime * result + ((requestSettings == null) ? 0 : requestSettings.hashCode());
            result = prime * result + retryTimes;
            result = prime * result + (int) retryInterval;
            result = prime * result + ((ifRetry == null) ? 0 : ifRetry.hashCode());
            result = prime * result + ((encryptionUserName == null) ? 0 : encryptionUserName.hashCode());
            result = prime * result + ((encryptionPassword == null) ? 0 : N.hashCode(encryptionPassword));
            result = prime * result + ((encryptionMessage == null) ? 0 : encryptionMessage.hashCode());
            return result;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof OperationConfig) {
                OperationConfig other = (OperationConfig) obj;

                return N.equals(httpMethod, other.httpMethod) && N.equals(requestUrl, other.requestUrl) && N.equals(requestSettings, other.requestSettings)
                        && N.equals(retryTimes, other.retryTimes) && N.equals(retryInterval, other.retryInterval) && N.equals(ifRetry, other.ifRetry)
                        && N.equals(encryptionUserName, other.encryptionUserName) && N.equals(encryptionPassword, other.encryptionPassword)
                        && N.equals(encryptionMessage, other.encryptionMessage);
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "{httpMethod=" + httpMethod + ", requestUrl=" + requestUrl + ", requestSettings=" + requestSettings + ", retryTimes=" + retryTimes
                    + ", retryInterval=" + retryInterval + ", ifRetry=" + ifRetry + "}";
        }
    }

    /**
     * The Interface Handler.
     */
    public interface Handler {

        /**
         *
         * @param method
         * @param args
         */
        void preInvoke(final Method method, final Object... args);

        /**
         *
         * @param e
         * @param result
         * @param method
         * @param args
         * @return
         */
        Object postInvoke(final Throwable e, final Object result, final Method method, final Object... args);
    }
}
