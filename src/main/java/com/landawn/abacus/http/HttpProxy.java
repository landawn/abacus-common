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
import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.core.MapEntity;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.DeserializationConfig;
import com.landawn.abacus.parser.Parser;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.AndroidUtil;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Result;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.URLEncodedUtil;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.function.Predicate;

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

    private static final Logger logger = LoggerFactory.getLogger(HttpProxy.class);

    private static final int DEFAULT_MAX_CONNECTION = HttpClient.DEFAULT_MAX_CONNECTION;

    private static final int DEFAULT_CONNECTION_TIMEOUT = HttpClient.DEFAULT_CONNECTION_TIMEOUT;

    private static final int DEFAULT_READ_TIMEOUT = HttpClient.DEFAULT_READ_TIMEOUT;

    // Upper and lower characters, digits, underscores, and hyphens, starting with a character.
    private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";

    private static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);

    private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass 
     * @param baseUrl
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final String baseUrl) {
        return createClientProxy(interfaceClass, baseUrl, null);
    }

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass 
     * @param baseUrl
     * @param config
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final String baseUrl, final Config config) {
        final WebService wsAnno = interfaceClass.getAnnotation(WebService.class);
        final int maxConnection = wsAnno != null && wsAnno.maxConnection() > 0 ? wsAnno.maxConnection() : DEFAULT_MAX_CONNECTION;
        final long connectionTimeout = wsAnno != null && wsAnno.connectionTimeout() > 0 ? wsAnno.connectionTimeout() : DEFAULT_CONNECTION_TIMEOUT;
        final long readTimeout = wsAnno != null && wsAnno.readTimeout() > 0 ? wsAnno.readTimeout() : DEFAULT_READ_TIMEOUT;

        return createClientProxy(interfaceClass, baseUrl, maxConnection, connectionTimeout, readTimeout, config);
    }

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass 
     * @param baseUrl
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final String baseUrl, final int maxConnection, final long connectionTimeout,
            final long readTimeout) {
        return createClientProxy(interfaceClass, baseUrl, maxConnection, connectionTimeout, readTimeout, null);
    }

    /**
     * 
     * @param <T>
     * @param interfaceClass
     * @param baseUrl
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @param config
     * @return
     */
    public static <T> T createClientProxy(final Class<T> interfaceClass, final String baseUrl, final int maxConnection, final long connectionTimeout,
            final long readTimeout, final Config config) {
        return createClientProxy(interfaceClass, null, baseUrl, maxConnection, connectionTimeout, readTimeout, config);
    }

    /**
     * Creates the client proxy.
     *
     * @param <T>
     * @param interfaceClass 
     * @param contentformat add for test only
     * @param baseUrl
     * @param maxConnection
     * @param connectionTimeout
     * @param readTimeout
     * @param config
     * @return
     */
    static <T> T createClientProxy(final Class<T> interfaceClass, final ContentFormat contentformat, final String baseUrl, final int maxConnection,
            final long connectionTimeout, final long readTimeout, final Config config) {

        N.checkArgNotNull(interfaceClass, "interfaceClass");
        N.checkArgPositive(maxConnection == 0 ? DEFAULT_MAX_CONNECTION : maxConnection, "maxConnection");
        N.checkArgPositive(connectionTimeout == 0 ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeout, "connectionTimeout");
        N.checkArgPositive(readTimeout == 0 ? DEFAULT_READ_TIMEOUT : readTimeout, "readTimeout");

        final Annotation[] interfaceAnnos = interfaceClass.getAnnotations();
        final WebService wsAnno = interfaceClass.getAnnotation(WebService.class);
        final String finalBaseUrl = N.isNullOrEmpty(baseUrl) && wsAnno != null ? wsAnno.baseUrl() : baseUrl;

        N.checkArgNotNull(finalBaseUrl, "baseUrl");

        @SuppressWarnings("deprecation")
        InvocationHandler h = new InvocationHandler() {
            private final Logger _logger = LoggerFactory.getLogger(interfaceClass);

            private final String _baseUrl = composeUrl(finalBaseUrl, HttpUtil.getHttpPath(interfaceAnnos));
            private final int _maxConnection = maxConnection == 0 ? DEFAULT_MAX_CONNECTION : maxConnection;
            private final long _connectionTimeout = connectionTimeout == 0 ? DEFAULT_CONNECTION_TIMEOUT : connectionTimeout;
            private final long _readTimeout = readTimeout == 0 ? DEFAULT_READ_TIMEOUT : readTimeout;

            private final ContentFormat _contentFormat = contentformat == null || contentformat == ContentFormat.NONE
                    ? HttpUtil.getContentFormat(interfaceAnnos, ContentFormat.JSON)
                    : contentformat;
            private final ContentFormat _acceptFormat = HttpUtil.getAcceptFormat(interfaceAnnos, ContentFormat.JSON);

            private final String _contentCharset = wsAnno == null ? null : wsAnno.contentCharset();
            private final String _acceptCharset = wsAnno == null ? null : wsAnno.accepCharset();

            private final int _maxRetryTimes = wsAnno == null ? 0 : wsAnno.maxRetryTimes();
            private final long _retryInterval = wsAnno == null ? 0 : wsAnno.retryInterval();

            private final Map<String, String> _httpHeaders = HttpUtil.getHttpHeaders(interfaceAnnos);

            private final Config _config = config == null ? new Config() : N.copy(config);

            private boolean _hasFutureReturnType = false;

            {
                final Set<Method> declaredMethods = N.asLinkedHashSet(interfaceClass.getDeclaredMethods());

                for (Class<?> superClass : interfaceClass.getInterfaces()) {
                    declaredMethods.addAll(Arrays.asList(superClass.getDeclaredMethods()));
                }

                final Set<String> declaredMethodNames = N.newHashSet(declaredMethods.size());

                for (Method method : declaredMethods) {
                    declaredMethodNames.add(method.getName());
                }

                if (_config.parser == null) {
                    _config.setParser(HttpUtil.getParser(_contentFormat));
                }

                if (config != null && config.getRequestSettings() != null) {
                    _config.setRequestSettings(config.getRequestSettings().copy());
                } else {
                    _config.setRequestSettings(HttpSettings.create());
                }

                // set operation configuration.
                final Map<String, OperationConfig> newOperationConfigs = N.newHashMap(declaredMethods.size());

                if (config != null && N.notNullOrEmpty(config.operationConfigs)) {
                    for (Map.Entry<String, OperationConfig> entry : config.operationConfigs.entrySet()) {
                        if (!declaredMethodNames.contains(entry.getKey())) {
                            throw new IllegalArgumentException("No method found by name: " + entry.getKey() + " for OperationConfig");
                        }

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
                    }

                    if (operationConfig.getRequestSettings() == null) {
                        if (_config.getRequestSettings() != null) {
                            operationConfig.setRequestSettings(_config.getRequestSettings().copy());
                        } else {
                            operationConfig.setRequestSettings(HttpSettings.create());
                        }
                    }

                    _config.methodConfigs.put(method, operationConfig);

                    operationConfig.requestEntityName = StringUtil.capitalize(methodName) + "Request";
                    operationConfig.responseEntityName = StringUtil.capitalize(methodName) + "Response";

                    final Annotation[] methdAnnos = method.getAnnotations();
                    final WebMethod wmAnnno = method.getAnnotation(WebMethod.class);

                    operationConfig.httpMethod = HttpUtil.getHttpMethod(method);
                    operationConfig.path = HttpUtil.getHttpPath(method);
                    operationConfig.requestUrl = composeUrl(_baseUrl, operationConfig.path);
                    operationConfig.connectionTimeout = wmAnnno == null || wmAnnno.connectionTimeout() <= 0 ? _connectionTimeout : wmAnnno.connectionTimeout();
                    operationConfig.readTimeout = wmAnnno == null || wmAnnno.readTimeout() <= 0 ? _readTimeout : wmAnnno.readTimeout();
                    operationConfig.maxRetryTimes = wmAnnno == null || wmAnnno.maxRetryTimes() < 0 ? _maxRetryTimes : wmAnnno.maxRetryTimes();
                    operationConfig.retryInterval = wmAnnno == null || wmAnnno.retryInterval() < 0 ? _retryInterval : wmAnnno.retryInterval();

                    operationConfig.contentFormat = HttpUtil.getContentFormat(methdAnnos, _contentFormat);
                    operationConfig.acceptFormat = HttpUtil.getAcceptFormat(methdAnnos, _acceptFormat);

                    operationConfig.contentCharset = wmAnnno == null || N.isNullOrEmpty(wmAnnno.contentCharset()) ? _contentCharset : wmAnnno.contentCharset();
                    operationConfig.acceptCharset = wmAnnno == null || N.isNullOrEmpty(wmAnnno.acceptCharset()) ? _acceptCharset : wmAnnno.acceptCharset();

                    if (N.notNullOrEmpty(_httpHeaders)) {
                        operationConfig.getRequestSettings().headers(_httpHeaders);
                    }

                    if (wmAnnno != null && N.notNullOrEmpty(wmAnnno.headers())) {
                        operationConfig.getRequestSettings().headers(N.asMap((Object[]) wmAnnno.headers()));
                    }

                    operationConfig.getRequestSettings().setContentFormat(operationConfig.contentFormat);

                    String contentType = HttpUtil.getContentType(operationConfig.requestSettings);

                    if (N.isNullOrEmpty(contentType)) {
                        if (operationConfig.contentFormat != null && operationConfig.contentFormat != ContentFormat.NONE) {
                            contentType = operationConfig.contentFormat.contentType();
                            operationConfig.requestSettings.header(HttpHeaders.Names.CONTENT_TYPE, contentType);
                        }
                    }

                    if (N.notNullOrEmpty(operationConfig.contentCharset)) {
                        if (N.isNullOrEmpty(contentType)) {
                            operationConfig.requestSettings.header(HttpHeaders.Names.CONTENT_TYPE, "charset=" + operationConfig.contentCharset);
                        } else if (StringUtil.indexOfIgnoreCase(contentType, "charset=") < 0) {
                            operationConfig.requestSettings.header(HttpHeaders.Names.CONTENT_TYPE, contentType + "; charset=" + operationConfig.contentCharset);
                        }
                    }

                    if (N.isNullOrEmpty(HttpUtil.getContentEncoding(operationConfig.requestSettings))) {
                        if (operationConfig.contentFormat != null && operationConfig.contentFormat != ContentFormat.NONE) {
                            operationConfig.requestSettings.header(HttpHeaders.Names.CONTENT_ENCODING, operationConfig.contentFormat.contentEncoding());
                        }
                    }

                    if (N.isNullOrEmpty(HttpUtil.getAccept(operationConfig.requestSettings))) {
                        if (operationConfig.acceptFormat != null && operationConfig.acceptFormat != ContentFormat.NONE) {
                            operationConfig.requestSettings.header(HttpHeaders.Names.ACCEPT, operationConfig.acceptFormat.contentType());
                        }
                    }

                    if (N.isNullOrEmpty(HttpUtil.getAcceptEncoding(operationConfig.requestSettings))) {
                        if (operationConfig.acceptFormat != null && operationConfig.acceptFormat != ContentFormat.NONE) {
                            operationConfig.requestSettings.header(HttpHeaders.Names.ACCEPT_ENCODING, operationConfig.acceptFormat.contentEncoding());
                        }
                    }

                    if (N.isNullOrEmpty(HttpUtil.getAcceptCharset(operationConfig.requestSettings))) {
                        if (N.notNullOrEmpty(operationConfig.acceptCharset)) {
                            operationConfig.requestSettings.header(HttpHeaders.Names.ACCEPT_CHARSET, operationConfig.acceptCharset);
                        }
                    }

                    operationConfig.parser = operationConfig.contentFormat == _contentFormat ? _config.parser
                            : HttpUtil.getParser(operationConfig.contentFormat);

                    if (operationConfig.parser.getClass().equals(_config.parser.getClass())) {
                        operationConfig.sc = _config.sc;
                        operationConfig.dc = _config.dc;
                    } else {
                        operationConfig.sc = null;
                        operationConfig.dc = null;
                    }

                    {
                        operationConfig.paramTypes = new Type[parameterCount];
                        operationConfig.fieldParams = new Field[parameterCount];
                        operationConfig.fieldNameSet = N.newHashSet(parameterCount);
                        operationConfig.pathParams = new Tuple2[parameterCount];
                        operationConfig.pathParamNameSet = N.newHashSet(parameterCount);
                        operationConfig.queryParams = new String[parameterCount];
                        operationConfig.queryParamNameSet = N.newHashSet(parameterCount);
                        operationConfig.pathAndQueryParamNameSet = N.newHashSet(parameterCount);
                        operationConfig.paramNameTypeMap = new HashMap<>();

                        final Annotation[][] parameterAnnotationArrays = method.getParameterAnnotations();
                        for (int i = 0; i < parameterCount; i++) {
                            operationConfig.paramTypes[i] = N.typeOf(parameterTypes[i]);

                            for (Annotation parameterAnnotation : parameterAnnotationArrays[i]) {
                                if (parameterAnnotation.annotationType() == Field.class) {
                                    operationConfig.fieldParams[i] = (Field) parameterAnnotation;

                                    if (operationConfig.paramNameTypeMap.put(operationConfig.fieldParams[i].value(),
                                            Tuple.of(i, operationConfig.paramTypes[i])) != null) {
                                        throw new IllegalArgumentException("Duplicated field parameter names: " + operationConfig.fieldParams[i].value()
                                                + " in method: " + method.getName());
                                    }

                                    operationConfig.fieldNameSet.add(operationConfig.fieldParams[i].value());
                                } else if (parameterAnnotation.annotationType() == PathParam.class) {
                                    final PathParam pathParam = (PathParam) parameterAnnotation;

                                    operationConfig.validatePathName(pathParam.value());
                                    operationConfig.pathParams[i] = Tuple.of(pathParam.value(), pathParam.encode());

                                    if (operationConfig.paramNameTypeMap.put(operationConfig.pathParams[i]._1,
                                            Tuple.of(i, operationConfig.paramTypes[i])) != null) {
                                        throw new IllegalArgumentException(
                                                "Duplicated path parameter names: " + operationConfig.pathParams[i]._1 + " in method: " + method.getName());
                                    }

                                    operationConfig.pathParamNameSet.add(operationConfig.pathParams[i]._1);
                                    operationConfig.pathAndQueryParamNameSet.add(operationConfig.pathParams[i]._1);
                                } else if (parameterAnnotation.annotationType() == QueryParam.class) {
                                    final QueryParam queryParam = (QueryParam) parameterAnnotation;

                                    operationConfig.queryParams[i] = queryParam.value();

                                    if (operationConfig.paramNameTypeMap.put(operationConfig.queryParams[i],
                                            Tuple.of(i, operationConfig.paramTypes[i])) != null) {
                                        throw new IllegalArgumentException(
                                                "Duplicated query parameter names: " + operationConfig.queryParams[i] + " in method: " + method.getName());
                                    }

                                    operationConfig.queryParamNameSet.add(operationConfig.queryParams[i]);
                                    operationConfig.pathAndQueryParamNameSet.add(operationConfig.queryParams[i]);
                                } else {
                                    try {
                                        if (parameterAnnotation.annotationType() == javax.ws.rs.PathParam.class) {
                                            final javax.ws.rs.PathParam pathParam = (javax.ws.rs.PathParam) parameterAnnotation;

                                            operationConfig.validatePathName(pathParam.value());
                                            operationConfig.pathParams[i] = Tuple.of(pathParam.value(), true);

                                            if (operationConfig.paramNameTypeMap.put(operationConfig.pathParams[i]._1,
                                                    Tuple.of(i, operationConfig.paramTypes[i])) != null) {
                                                throw new IllegalArgumentException("Duplicated path parameter names: " + operationConfig.pathParams[i]._1
                                                        + " in method: " + method.getName());
                                            }

                                            operationConfig.pathParamNameSet.add(operationConfig.pathParams[i]._1);
                                            operationConfig.pathAndQueryParamNameSet.add(operationConfig.pathParams[i]._1);
                                        }
                                    } catch (Throwable e) {
                                        // ignore
                                    }

                                    try {
                                        if (parameterAnnotation.annotationType() == javax.ws.rs.QueryParam.class) {
                                            final javax.ws.rs.QueryParam queryParam = (javax.ws.rs.QueryParam) parameterAnnotation;

                                            operationConfig.queryParams[i] = queryParam.value();

                                            if (operationConfig.paramNameTypeMap.put(operationConfig.queryParams[i],
                                                    Tuple.of(i, operationConfig.paramTypes[i])) != null) {
                                                throw new IllegalArgumentException("Duplicated query parameter names: " + operationConfig.queryParams[i]
                                                        + " in method: " + method.getName());
                                            }

                                            operationConfig.queryParamNameSet.add(operationConfig.queryParams[i]);
                                            operationConfig.pathAndQueryParamNameSet.add(operationConfig.queryParams[i]);
                                        }
                                    } catch (Throwable e) {
                                        // ignore
                                    }
                                }

                                if (operationConfig.fieldParams[i] == null && operationConfig.pathParams[i] == null && operationConfig.queryParams[i] == null) {
                                    throw new IllegalArgumentException("Parameter is not named by annoation @Field/@FieldParam/@QueryParam at position: " + i
                                            + " in method: " + method.getName());
                                }
                            }
                        }

                        operationConfig.urlPartsSplittedByParaNames = operationConfig.requestUrl.split(PARAM_URL_REGEX.pattern());

                        if (operationConfig.urlPartsSplittedByParaNames.length > 1) {
                            final List<String> patterns = new ArrayList<>(operationConfig.urlPartsSplittedByParaNames.length);
                            final Matcher m = PARAM_URL_REGEX.matcher(operationConfig.requestUrl);
                            while (m.find()) {
                                patterns.add(m.group(1));
                            }
                            operationConfig.urlParamNames = patterns.toArray(new String[patterns.size()]);
                        } else {
                            operationConfig.urlParamNames = N.EMPTY_STRING_ARRAY;
                        }

                        operationConfig.urlParamNameSet = N.asSet(operationConfig.urlParamNames);

                        if (N.notNullOrEmpty(operationConfig.paramNameTypeMap)) {
                            final List<String> diff = N.symmetricDifference(operationConfig.urlParamNameSet, operationConfig.pathParamNameSet);

                            if (N.notNullOrEmpty(diff)) {
                                throw new IllegalArgumentException("Path parameters: " + diff + " are not configured in path: " + operationConfig.path
                                        + " in method: " + method.getName());
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

                    operationConfig.returnType = N.typeOf(ClassUtil.formatParameterizedTypeName(method.getGenericReturnType().toString()));

                    operationConfig.concreteReturnType = Future.class.isAssignableFrom(method.getReturnType())
                            ? (Type<Object>) operationConfig.returnType.getParameterTypes()[0]
                            : operationConfig.returnType;

                    if (Future.class.isAssignableFrom(method.getReturnType())) {
                        operationConfig.isFutureReturnType = true;

                        if (ContinuableFuture.class.isAssignableFrom(method.getReturnType())) {
                            operationConfig.isContinuableFutureReturnType = true;
                        }

                        _hasFutureReturnType = true;
                    }

                    if (!Modifier.isAbstract(method.getModifiers())) {
                        final OperationConfig finalOperationConfig = operationConfig;
                        final int paramLen = method.getParameterCount();

                        final WebMethod[] lastParam = paramLen > 0 && method.getParameterTypes()[paramLen - 1].isAssignableFrom(WebMethod[].class)
                                && wmAnnno != null ? Array.oF(wmAnnno) : new WebMethod[0];

                        final MethodHandle methodHandle = ClassUtil.createMethodHandle(method);

                        final Throwables.BiFunction<Object, Object[], Object, Exception> call = new Throwables.BiFunction<Object, Object[], Object, Exception>() {
                            @Override
                            public Object apply(final Object proxy, final Object[] args) throws Exception {
                                if (lastParam != null) {
                                    args[paramLen - 1] = lastParam;
                                }

                                if (_logger.isInfoEnabled()) {
                                    _logger.info(finalOperationConfig.parser.serialize(args, finalOperationConfig.sc));
                                }

                                Object result = null;

                                try {
                                    result = methodHandle.bindTo(proxy).invokeWithArguments(args);
                                } catch (Throwable e) {
                                    if (e instanceof Exception) {
                                        throw (Exception) e;
                                    }

                                    throw N.toRuntimeException(e);
                                }

                                if (_logger.isInfoEnabled()) {
                                    if (!finalOperationConfig.concreteReturnType.clazz().equals(void.class)
                                            && finalOperationConfig.concreteReturnType.isSerializable()) {
                                        _logger.info(finalOperationConfig.concreteReturnType.stringOf(result));
                                    } else {
                                        _logger.info(finalOperationConfig.parser.serialize(result, finalOperationConfig.sc));
                                    }
                                }

                                return result;
                            }
                        };

                        _config.methodCalls.put(method, call);
                    }
                }
            }

            private final AtomicInteger sharedActiveConnectionCounter = new AtomicInteger(0);
            private final Map<Method, HttpClient> _httpClientPool = N.newHashMap(_config.operationConfigs.size());
            private final Executor _asyncExecutor;

            {
                Executor executor = null;

                if (_config.executedByThreadPool || _hasFutureReturnType) {
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
                    return method.invoke(proxy, args);
                }

                final OperationConfig operationConfig = _config.methodConfigs.get(method);

                if (operationConfig.maxRetryTimes > 0) {
                    final Predicate<Result<Object, Throwable>> ifRetry = _config.getIfRetry();
                    int retriedTimes = 0;
                    Result<Object, Throwable> result = null;
                    Throwable throwable = null;

                    do {
                        if (result != null && operationConfig.retryInterval > 0) {
                            N.sleepUninterruptibly(operationConfig.retryInterval);
                        }

                        try {
                            result = Result.of(invoke2(proxy, method, args, operationConfig), null);
                        } catch (Throwable e) {
                            if (throwable == null) {
                                throwable = e;
                            }

                            result = Result.of(null, e);

                            _logger.error("Failed to call: " + method.getName(), e);
                        }
                    } while (retriedTimes++ < operationConfig.maxRetryTimes
                            && ((ifRetry == null && result.isFailure()) || (ifRetry != null && ifRetry.test(result))));

                    return result.orElseThrow(throwable);
                } else {
                    return invoke2(proxy, method, args, operationConfig);
                }
            }

            private Object invoke2(final Object proxy, final Method method, final Object[] args, final OperationConfig operationConfig) throws Throwable {
                if (_config.executedByThreadPool || operationConfig.isFutureReturnType) {
                    final Callable<Object> cmd = new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            if (_config.handler == null) {
                                return invoke3(proxy, method, args, operationConfig);
                            } else {
                                _config.handler.preInvoke(method, args);

                                Object result = null;
                                Throwable exception = null;

                                try {
                                    result = invoke3(proxy, method, args, operationConfig);
                                } catch (Throwable e) {
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

                    if (operationConfig.isContinuableFutureReturnType) {
                        return ContinuableFuture.wrap(futureTask);
                    } else if (operationConfig.isFutureReturnType) {
                        return futureTask;
                    } else {
                        return futureTask.get();
                    }
                } else {
                    if (_config.handler == null) {
                        return invoke3(proxy, method, args, operationConfig);
                    } else {
                        _config.handler.preInvoke(method, args);

                        Object result = null;
                        Throwable exception = null;

                        try {
                            result = invoke3(proxy, method, args, operationConfig);
                        } catch (Throwable e) {
                            exception = e;
                        } finally {
                            result = _config.handler.postInvoke(exception, result, method, args);
                        }

                        return result;
                    }
                }
            }

            private Object invoke3(final Object proxy, final Method method, final Object[] args, final OperationConfig operationConfig) throws Exception {
                if (_logger.isInfoEnabled()) {
                    _logger.info(operationConfig.parser.serialize(args, operationConfig.sc));
                }

                if (!Modifier.isAbstract(method.getModifiers())) {
                    final Object result = _config.methodCalls.get(method).apply(proxy, args);

                    if (_logger.isInfoEnabled()) {
                        _logger.info(operationConfig.parser.serialize(result, operationConfig.sc));
                    }

                    return result;
                }

                final Charset requestCharset = N.isNullOrEmpty(operationConfig.contentCharset) ? Charsets.UTF_8 : Charsets.get(operationConfig.contentCharset);

                String newRequestUrl = operationConfig.requestUrl;

                if (N.notNullOrEmpty(operationConfig.urlParamNames)) {
                    final StringBuilder sb = Objectory.createStringBuilder();

                    try {
                        if (N.isNullOrEmpty(operationConfig.paramNameTypeMap)) {
                            final Type<Object> type = N.typeOf(N.checkArgNotNull(args[0]).getClass());

                            if (type.isMap()) {
                                final Map<String, Object> map = (Map<String, Object>) args[0];
                                Object value = null;

                                for (int i = 0, len = operationConfig.urlParamNames.length; i < len; i++) {
                                    value = map.get(operationConfig.urlParamNames[i]);

                                    if (value == null && !map.containsKey(operationConfig.urlParamNames[i])) {
                                        throw new IllegalArgumentException(
                                                "No value is set for path parameter: " + operationConfig.urlParamNames[i] + " in method: " + method.getName());
                                    }

                                    sb.append(operationConfig.urlPartsSplittedByParaNames[i]);

                                    sb.append(N.urlEncode(N.stringOf(value)));
                                }
                            } else if (type.isEntity()) {
                                final Object entity = args[0];
                                final EntityInfo entityInfo = ParserUtil.getEntityInfo(entity.getClass());
                                Object value = null;

                                for (int i = 0, len = operationConfig.urlParamNames.length; i < len; i++) {
                                    value = entityInfo.getPropValue(entity, operationConfig.urlParamNames[i]);

                                    sb.append(operationConfig.urlPartsSplittedByParaNames[i]);

                                    sb.append(N.urlEncode(N.stringOf(value)));
                                }
                            } else {
                                throw new IllegalArgumentException("Unsupported path parameter type: " + type.name() + " in method: " + method.getName());
                            }
                        } else {
                            Tuple2<Integer, Type<Object>> tp = null;

                            for (int i = 0, len = operationConfig.urlParamNames.length; i < len; i++) {
                                sb.append(operationConfig.urlPartsSplittedByParaNames[i]);

                                tp = operationConfig.paramNameTypeMap.get(operationConfig.urlParamNames[i]);

                                if (operationConfig.pathParams[tp._1]._2) {
                                    sb.append(N.urlEncode(tp._2.stringOf(args[tp._1])));
                                } else {
                                    sb.append(tp._2.stringOf(args[tp._1]));
                                }
                            }
                        }

                        if (operationConfig.urlPartsSplittedByParaNames.length > operationConfig.urlParamNames.length) {
                            sb.append(operationConfig.urlPartsSplittedByParaNames[operationConfig.urlParamNames.length]);
                        }

                        if (N.notNullOrEmpty(operationConfig.queryParamNameSet)) {
                            Map<String, Object> queryParams = N.newHashMap(operationConfig.queryParamNameSet.size());
                            Tuple2<Integer, Type<Object>> tp = null;

                            for (String qureyParamName : operationConfig.queryParamNameSet) {
                                tp = operationConfig.paramNameTypeMap.get(qureyParamName);
                                queryParams.put(qureyParamName, tp._2.stringOf(args[tp._1]));
                            }

                            sb.append(WD._QUESTION_MARK);

                            URLEncodedUtil.encode(sb, queryParams, requestCharset);
                        }

                        newRequestUrl = sb.toString();
                    } finally {
                        Objectory.recycle(sb);
                    }
                } else if (N.notNullOrEmpty(operationConfig.queryParamNameSet)) {
                    final Map<String, Object> queryParams = N.newHashMap(operationConfig.queryParamNameSet.size());
                    Tuple2<Integer, Type<Object>> tp = null;

                    for (String qureyParamName : operationConfig.queryParamNameSet) {
                        tp = operationConfig.paramNameTypeMap.get(qureyParamName);
                        queryParams.put(qureyParamName, tp._2.stringOf(args[tp._1]));
                    }

                    newRequestUrl = URLEncodedUtil.encode(newRequestUrl, queryParams, requestCharset);
                }

                Object requestParameter = null;

                if (N.notNullOrEmpty(operationConfig.fieldNameSet)) {
                    final Map<String, Object> queryParams = N.newHashMap(operationConfig.fieldNameSet.size());
                    Tuple2<Integer, Type<Object>> tp = null;

                    for (String fieldName : operationConfig.fieldNameSet) {
                        tp = operationConfig.paramNameTypeMap.get(fieldName);
                        queryParams.put(fieldName, tp._2.stringOf(args[tp._1]));
                    }

                    if (operationConfig.httpMethod == HttpMethod.GET || operationConfig.httpMethod == HttpMethod.DELETE) {
                        newRequestUrl = URLEncodedUtil.encode(newRequestUrl, queryParams, requestCharset);
                    } else {
                        requestParameter = queryParams;
                    }
                } else if (operationConfig.httpMethod == HttpMethod.POST || operationConfig.httpMethod == HttpMethod.PUT) {
                    //    if (N.isNullOrEmpty(operationConfig.pathAndQueryParamNameSet)) {
                    //        requestParameter = args[0];
                    //    } else if (args[0] != null) {
                    //        requestParameter = args[0];
                    //        final Type<?> type = N.typeOf(args[0].getClass());
                    //
                    //        if (type.isMap()) {
                    //            final Map<String, Object> map = (Map<String, Object>) args[0];
                    //
                    //            if (N.containsAny(map.keySet(), operationConfig.pathAndQueryParamNameSet)) {
                    //                final Map<String, Object> tmp = N.newInstance(map.getClass());
                    //
                    //                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    //                    if (!operationConfig.pathAndQueryParamNameSet.contains(entry.getKey())) {
                    //                        tmp.put(entry.getKey(), entry.getValue());
                    //                    }
                    //                }
                    //
                    //                requestParameter = tmp;
                    //            }
                    //        } else if (type.isEntity()) {
                    //            if (N.notNullOrEmpty(operationConfig.getEncryptionUserName()) && N.notNullOrEmpty(operationConfig.getEncryptionPassword())) {
                    //
                    //            } else {
                    //                requestParameter = Maps.entity2Map(args[0], operationConfig.pathAndQueryParamNameSet);
                    //            }
                    //        }
                    //    }

                    requestParameter = args != null && args.length == 1 ? args[0] : args;
                } else if (operationConfig.httpMethod == HttpMethod.GET || operationConfig.httpMethod == HttpMethod.DELETE) {
                    if (args != null && args.length == 1) {
                        newRequestUrl = URLEncodedUtil.encode(newRequestUrl, args[0], requestCharset, _config.queryParamNamingPolicy);
                    }
                }

                if (N.notNullOrEmpty(operationConfig.getEncryptionUserName()) && N.notNullOrEmpty(operationConfig.getEncryptionPassword())) {
                    ((SecurityDTO) requestParameter).encrypt(operationConfig.getEncryptionUserName(), operationConfig.getEncryptionPassword(),
                            operationConfig.getEncryptionMessage());
                }

                HttpClient httpClient = null;

                if (newRequestUrl.equals(operationConfig.requestUrl)) {
                    synchronized (_httpClientPool) {
                        httpClient = _httpClientPool.get(method);

                        if (httpClient == null) {
                            httpClient = HttpClient.create(operationConfig.requestUrl, _maxConnection, operationConfig.connectionTimeout,
                                    operationConfig.readTimeout, operationConfig.getRequestSettings(), sharedActiveConnectionCounter);

                            _httpClientPool.put(method, httpClient);
                        }
                    }
                } else {
                    httpClient = HttpClient.create(newRequestUrl, _maxConnection, operationConfig.connectionTimeout, operationConfig.readTimeout,
                            operationConfig.getRequestSettings(), sharedActiveConnectionCounter);
                }

                InputStream is = null;
                OutputStream os = null;

                final HttpURLConnection connection = httpClient.openConnection(operationConfig.httpMethod, operationConfig.requestSettings);

                try {
                    if (requestParameter != null && (operationConfig.httpMethod == HttpMethod.POST || operationConfig.httpMethod == HttpMethod.PUT)) {
                        os = HttpUtil.getOutputStream(connection, operationConfig.contentFormat, operationConfig.requestSettings.getContentType(),
                                operationConfig.requestSettings.getContentEncoding());

                        switch (operationConfig.contentFormat) {
                            case JSON:
                            case JSON_LZ4:
                            case JSON_SNAPPY:
                            case JSON_GZIP:
                                Type<Object> type = N.typeOf(requestParameter.getClass());

                                if (type.isSerializable()) {
                                    os.write(type.stringOf(requestParameter).getBytes(requestCharset));
                                } else {
                                    operationConfig.parser.serialize(os, requestParameter, operationConfig.sc);
                                }

                                break;

                            case XML:
                            case XML_LZ4:
                            case XML_SNAPPY:
                            case XML_GZIP:
                                if (requestParameter instanceof Map) {
                                    operationConfig.parser.serialize(os,
                                            MapEntity.valueOf(operationConfig.requestEntityName, (Map<String, Object>) requestParameter), operationConfig.sc);
                                } else {
                                    operationConfig.parser.serialize(os, requestParameter, operationConfig.sc);
                                }

                                break;

                            case FormUrlEncoded:
                                os.write(URLEncodedUtil.encode(requestParameter, requestCharset).getBytes(requestCharset));

                                break;

                            case KRYO:
                                operationConfig.parser.serialize(os, requestParameter, operationConfig.sc);

                                break;

                            default:
                                throw new IllegalArgumentException("Unsupported content type: " + operationConfig.contentFormat.toString());
                        }

                        HttpUtil.flush(os);
                    } else {
                        String contentType = HttpUtil.getContentType(operationConfig.contentFormat);

                        if (N.notNullOrEmpty(contentType)) {
                            connection.setRequestProperty(HttpHeaders.Names.CONTENT_TYPE, contentType);
                        }

                        String contentEncoding = HttpUtil.getContentEncoding(operationConfig.contentFormat);

                        if (N.notNullOrEmpty(contentEncoding)) {
                            connection.setRequestProperty(HttpHeaders.Names.CONTENT_ENCODING, contentEncoding);
                        }
                    }

                    final int code = connection.getResponseCode();
                    final Map<String, List<String>> respHeaders = connection.getHeaderFields();
                    final Charset respCharset = HttpUtil.getResponseCharset(respHeaders, requestCharset);
                    final ContentFormat responseContentFormat = HttpUtil.getResponseContentFormat(respHeaders, operationConfig.contentFormat);
                    final Parser<SerializationConfig<?>, DeserializationConfig<?>> responseParser = responseContentFormat == operationConfig.contentFormat
                            ? operationConfig.parser
                            : HttpUtil.getParser(responseContentFormat);
                    final SerializationConfig<?> responseSC = responseContentFormat == operationConfig.contentFormat ? operationConfig.sc : null;
                    final DeserializationConfig<?> responseDC = responseContentFormat == operationConfig.contentFormat ? operationConfig.dc : null;

                    if (code < 200 || code >= 300) {
                        throw new UncheckedIOException(new IOException(
                                code + ": " + connection.getResponseMessage() + ". " + IOUtil.readString(connection.getInputStream(), respCharset)));
                    }

                    is = HttpUtil.getInputStream(connection, responseContentFormat);

                    if (void.class.equals(operationConfig.concreteReturnType.clazz())) {
                        return null;
                    } else {
                        Object result = null;

                        switch (responseContentFormat) {
                            case JSON:
                            case JSON_LZ4:
                            case JSON_SNAPPY:
                            case JSON_GZIP:
                                if (operationConfig.concreteReturnType.isSerializable()) {
                                    result = operationConfig.concreteReturnType.valueOf(IOUtil.readString(is, respCharset));
                                } else {
                                    result = responseParser.deserialize(operationConfig.concreteReturnType.clazz(), IOUtil.newBufferedReader(is, respCharset),
                                            responseDC);
                                }

                                break;

                            case XML:
                            case XML_LZ4:
                            case XML_SNAPPY:
                            case XML_GZIP:
                                result = responseParser.deserialize(operationConfig.concreteReturnType.clazz(), IOUtil.newBufferedReader(is, respCharset),
                                        responseDC);
                                break;

                            case FormUrlEncoded:
                                result = URLEncodedUtil.decode(operationConfig.concreteReturnType.clazz(), IOUtil.readString(is, respCharset));

                                break;

                            case KRYO:
                                result = responseParser.deserialize(operationConfig.concreteReturnType.clazz(), is, responseDC);
                                break;

                            default:
                                throw new IllegalArgumentException("Unsupported content type: " + responseContentFormat.toString());
                        }

                        if (_logger.isInfoEnabled()) {
                            if (!operationConfig.concreteReturnType.clazz().equals(void.class) && operationConfig.concreteReturnType.isSerializable()) {
                                _logger.info(operationConfig.concreteReturnType.stringOf(result));
                            } else {
                                _logger.info(responseParser.serialize(result, responseSC));
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
        };

        return (T) N.newProxyInstance(N.asArray(interfaceClass), h);
    }

    static String composeUrl(final String baseUrl, final String path) {
        String result = baseUrl;

        if (N.notNullOrEmpty(path)) {
            if (baseUrl.endsWith("/") || baseUrl.endsWith("\\")) {
                if (path.startsWith("/") || path.startsWith("\\")) {
                    result = baseUrl + path.substring(1);
                } else {
                    result = baseUrl + path;
                }
            } else {
                if (path.startsWith("/") || path.startsWith("\\")) {
                    result = baseUrl + path;
                } else {
                    result = baseUrl + "/" + path;
                }
            }
        }

        if (result.endsWith("/") || result.endsWith("\\")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * The Class Config.
     */
    public static final class Config {
        private Parser<SerializationConfig<?>, DeserializationConfig<?>> parser;
        private SerializationConfig<?> sc;
        private DeserializationConfig<?> dc;
        private Handler handler;
        private boolean executedByThreadPool;
        private Executor asyncExecutor;
        private Predicate<Result<Object, Throwable>> ifRetry;
        private HttpSettings requestSettings;
        private Map<String, OperationConfig> operationConfigs;
        private NamingPolicy queryParamNamingPolicy;
        private String encryptionUserName;
        private byte[] encryptionPassword;
        private MessageEncryption encryptionMessage;

        final Map<Method, OperationConfig> methodConfigs = new HashMap<>();
        final Map<Method, Throwables.BiFunction<Object, Object[], Object, Exception>> methodCalls = new HashMap<>();

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
         * Gets the if retry.
         *
         * @return
         */
        public Predicate<Result<Object, Throwable>> getIfRetry() {
            return ifRetry;
        }

        /**
         * Sets the if retry.
         *
         * @param ifRetry
         * @return
         */
        public Config setIfRetry(final Predicate<Result<Object, Throwable>> ifRetry) {
            this.ifRetry = ifRetry;

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
         * Gets the query param naming policy.
         *
         * @return
         */
        public NamingPolicy getQueryParamNamingPolicy() {
            return queryParamNamingPolicy;
        }

        /**
         * Sets the query param naming policy.
         *
         * @param queryParamNamingPolicy
         * @return
         */
        public Config setQueryParamNamingPolicy(final NamingPolicy queryParamNamingPolicy) {
            this.queryParamNamingPolicy = queryParamNamingPolicy;

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
            return "{parser=" + parser + ", sc=" + sc + ", dc=" + dc + ", handler=" + handler + ", executedByThreadPool=" + executedByThreadPool + ", ifRetry="
                    + ifRetry + ", asyncExecutor=" + asyncExecutor + ", requestSettings=" + requestSettings + ", queryParamNamingPolicy="
                    + queryParamNamingPolicy + ", operationConfigs=" + operationConfigs + "}";
        }
    }

    /**
     * The Class OperationConfig.
     */
    public static class OperationConfig {
        private HttpSettings requestSettings;
        private String encryptionUserName;
        private byte[] encryptionPassword;
        private MessageEncryption encryptionMessage;

        String requestUrl;
        String path;
        HttpMethod httpMethod;
        long connectionTimeout;
        long readTimeout;
        int maxRetryTimes;
        long retryInterval;

        String requestEntityName;
        String responseEntityName;

        String[] urlPartsSplittedByParaNames;
        String[] urlParamNames;
        Set<String> urlParamNameSet;
        Type<Object>[] paramTypes;
        Field[] fieldParams;
        Set<String> fieldNameSet;
        Tuple2<String, Boolean>[] pathParams;
        Set<String> pathParamNameSet;
        String[] queryParams;
        Set<String> queryParamNameSet;
        Set<String> pathAndQueryParamNameSet;
        Map<String, Tuple2<Integer, Type<Object>>> paramNameTypeMap;
        ContentFormat contentFormat = null;
        ContentFormat acceptFormat = null;

        String contentCharset = null;
        String acceptCharset = null;

        Parser<SerializationConfig<?>, DeserializationConfig<?>> parser;
        SerializationConfig<?> sc;
        DeserializationConfig<?> dc;

        Type<Object> returnType = null;
        Type<Object> concreteReturnType = null;

        boolean isFutureReturnType = false;
        boolean isContinuableFutureReturnType = false;

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
                throw new IllegalArgumentException(String.format("@Path parameter name must match %s. Found: %s", PARAM_URL_REGEX.pattern(), name));
            }

            // Verify URL replacement name is actually present in the URL path.
            if (!urlParamNameSet.contains(name)) {
                throw new IllegalArgumentException(String.format("URL \"%s\" does not contain \"{%s}\".", path, name));
            }
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "{httpMethod=" + httpMethod + ", requestUrl=" + requestUrl + ", requestSettings=" + requestSettings + ", maxRetryTimes=" + maxRetryTimes
                    + ", retryInterval=" + retryInterval + "}";
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
