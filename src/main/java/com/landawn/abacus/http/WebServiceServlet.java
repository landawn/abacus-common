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

import static com.landawn.abacus.http.HttpUtil.jsonParser;
import static com.landawn.abacus.http.HttpUtil.kryoParser;
import static com.landawn.abacus.http.HttpUtil.xmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.XMLDeserializationConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.URLEncodedUtil;

/**
 * It's a quick way to deploy json/xml web service by
 * <code>WebServiceServlet</code>
 *
 * <pre>
 *  {@code
 *     <servlet>
 *         <description>Hello web service</description>
 *         <display-name>helloWebService</display-name>
 *         <servlet-name>helloWebService</servlet-name>
 *         <servlet-class>com.landawn.abacus.http.WebServiceServlet</servlet-class>
 *         <init-param>
 *             <param-name>serviceImplClass</param-name>
 *             <param-value>com.landawn.ws.cxf.hello.HelloWebServiceImpl</param-value>
 *         </init-param>
 *         <init-param>
 *     </servlet>
 *
 *     <servlet-mapping>
 *         <servlet-name>helloWebService</servlet-name>
 *         <url-pattern>/HelloWebService/*</url-pattern>
 *     </servlet-mapping>
 * }
 * </pre>
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class WebServiceServlet extends AbstractHttpServlet {

    private static final long serialVersionUID = -7919235560292201779L;

    protected static final String SERVICE_IMPL_CLASS = "serviceImplClass";

    protected static final String SERVICE_FACTORY_CLASS = "serviceFactoryClass";

    protected static final String SERVICE_FACTORY_METHOD = "serviceFactoryMethod";

    //    /** The Constant PATH_MAPPER. */
    //    protected static final String PATH_MAPPER = "pathMapper";
    //
    //    /** The Constant HTTP_METHOD_MAPPER. */
    //    protected static final String HTTP_METHOD_MAPPER = "httpMethodMapper";

    protected static final String ENCRYPTION_USER_NAME = "encryptionUserName";

    protected static final String ENCRYPTION_PASSWORD = "encryptionPassword";

    protected static final String ENCRYPTION_MESSAGE = "encryptionMessage";

    private final Map<String, Method> pathMethodMap = new HashMap<>();

    private final Map<String, Class<?>> methodParameterClassMap = new HashMap<>();

    private final Map<String, Method> parameterMethodMap = new HashMap<>();

    private final Map<String, Method> eleNameMethodMap = new HashMap<>();

    private final Map<String, Class<?>> eleNameParameterClassMap = new HashMap<>();

    private final Map<String, Set<HttpMethod>> methodHttpMethodMap = new HashMap<>();

    private final Map<String, String[]> methodParameterNamesMap = new HashMap<>();

    private final Map<String, Map<String, Type<?>>> methodParameterNamingTypesMap = new HashMap<>();

    private final Map<String, JSONDeserializationConfig> methodJSONDeserializationConfigMap = new HashMap<>();

    private final Map<String, XMLDeserializationConfig> methodXMLDeserializationConfigMap = new HashMap<>();

    static {
        LoggerFactory.getLogger(WebServiceServlet.class).warn(IOUtil.JAVA_VERSION);
    }

    private Object serviceImpl;

    private String encryptionUserName;

    private byte[] encryptionPassword;

    private MessageEncryption encryptionMessage;

    private Logger serviceRRLogger;

    private Logger serviceImplLogger;

    /**
     *
     * @throws ServletException the servlet exception
     */
    @Override
    public void init() throws ServletException {
        super.init();
    }

    /**
     *
     * @param config
     * @throws ServletException the servlet exception
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        String serviceImplClassParameter = getInitParameter(config, SERVICE_IMPL_CLASS);
        String serviceFatoryClassParameter = getInitParameter(config, SERVICE_FACTORY_CLASS);
        String serviceFatoryMethodParameter = getInitParameter(config, SERVICE_FACTORY_METHOD);

        encryptionUserName = getInitParameter(config, ENCRYPTION_USER_NAME);
        encryptionPassword = N.isNullOrEmpty(getInitParameter(config, ENCRYPTION_PASSWORD)) ? N.EMPTY_BYTE_ARRAY
                : getInitParameter(config, ENCRYPTION_PASSWORD).getBytes();
        encryptionMessage = N.isNullOrEmpty(getInitParameter(config, ENCRYPTION_MESSAGE)) ? MessageEncryption.NONE
                : MessageEncryption.valueOf(getInitParameter(config, ENCRYPTION_MESSAGE).toUpperCase());

        if (N.isNullOrEmpty(serviceImplClassParameter) && N.isNullOrEmpty(serviceFatoryClassParameter)) {
            throw new RuntimeException("serviceImplClass and serviceFactoryClass can't be null at the same time.");
        }

        Class<?> serviceImplClass = null;
        Class<?> serviceFactoryClass = null;

        if (N.isNullOrEmpty(serviceImplClassParameter)) {
            serviceFactoryClass = ClassUtil.forClass(serviceFatoryClassParameter);
            serviceImplClass = ClassUtil.getDeclaredMethod(serviceFactoryClass, serviceFatoryMethodParameter).getReturnType();
        } else {
            serviceImplClass = ClassUtil.forClass(serviceImplClassParameter);
        }

        if (N.isNullOrEmpty(serviceFatoryMethodParameter)) {
            serviceImpl = N.newInstance(serviceImplClass);
        } else {
            if (N.isNullOrEmpty(serviceFatoryClassParameter)) {
                serviceImpl = ClassUtil.invokeMethod(ClassUtil.getDeclaredMethod(serviceImplClass, serviceFatoryMethodParameter));
            } else {
                if (serviceFactoryClass == null) {
                    serviceFactoryClass = ClassUtil.forClass(serviceFatoryClassParameter);
                }

                serviceImpl = ClassUtil.invokeMethod(ClassUtil.getDeclaredMethod(serviceFactoryClass, serviceFatoryMethodParameter));
            }
        }

        final Set<Class<?>> superClasses = N.newLinkedHashSet();

        if (!(serviceImplClass.getSuperclass() == null || Object.class.equals(serviceImplClass))) {
            superClasses.add(serviceImplClass.getSuperclass());
        }

        if (N.notNullOrEmpty(serviceImplClass.getInterfaces())) {
            superClasses.addAll(Arrays.asList(serviceImplClass.getInterfaces()));
        }

        final Set<Method> declaredMethods = N.asLinkedHashSet(serviceImplClass.getDeclaredMethods());

        for (Class<?> superClass : superClasses) {
            declaredMethods.addAll(Arrays.asList(superClass.getDeclaredMethods()));
        }

        for (Method method : declaredMethods) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            final String methodName = method.getName();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final int parameterCount = parameterTypes.length;

            boolean noFieldAnnotation = true;
            Annotation[][] parameterAnnotationArrays = method.getParameterAnnotations();

            for (int i = 0; i < parameterCount; i++) {
                for (Annotation parameterAnnotation : parameterAnnotationArrays[i]) {
                    if (parameterAnnotation.annotationType() == Field.class) {
                        noFieldAnnotation = false;

                        break;
                    }
                }
            }

            if (noFieldAnnotation) {
                for (Class<?> superClass : superClasses) {
                    if (noFieldAnnotation == false) {
                        break;
                    }

                    for (Method dm : superClass.getDeclaredMethods()) {
                        if (dm.getName().equals(methodName) && N.equals(dm.getParameterTypes(), method.getParameterTypes())) {
                            parameterAnnotationArrays = dm.getParameterAnnotations();

                            for (int i = 0; i < parameterCount; i++) {
                                for (Annotation parameterAnnotation : parameterAnnotationArrays[i]) {
                                    if (parameterAnnotation.annotationType() == Field.class) {
                                        noFieldAnnotation = false;

                                        break;
                                    }
                                }
                            }

                            break;
                        }
                    }
                }
            }

            final Set<HttpMethod> httpMethods = HttpUtil.getHttpMethods(method);

            methodHttpMethodMap.put(methodName, httpMethods);

            final String httpPath = HttpUtil.getHttpPath(method);

            if (N.notNullOrEmpty(httpPath)) {
                pathMethodMap.put(httpPath, method);
            } else {
                pathMethodMap.put(methodName, method);
            }

            if (parameterCount == 0) {
                // ignore.
            } else if (parameterCount == 1 && noFieldAnnotation) {
                final Class<?> parameterClass = method.getParameterTypes()[0];
                methodParameterClassMap.put(methodName, parameterClass);

                parameterMethodMap.put(ClassUtil.getSimpleClassName(parameterClass), method);

                if (ClassUtil.isEntity(parameterClass)) {
                    Object tempInstance = N.newInstance(parameterClass);
                    String st = xmlParser.serialize(tempInstance);
                    int index = st.indexOf('>');
                    if (index > 1) {
                        String eleName = st.substring(1, index);
                        if (N.notNullOrEmpty(eleName)) {
                            eleNameMethodMap.put(eleName, method);
                            eleNameParameterClassMap.put(eleName, parameterClass);
                        }
                    }
                }

                ClassUtil.registerXMLBindingClass(parameterClass);

                if (!void.class.equals(method.getReturnType())) {
                    ClassUtil.registerXMLBindingClass(method.getReturnType());
                }
            } else {
                methodParameterClassMap.put(methodName, Map.class);

                final String[] parameterNames = new String[parameterCount];
                final Map<String, Type<?>> parameterNamingTypes = new HashMap<>();

                for (int i = 0; i < parameterCount; i++) {
                    for (Annotation parameterAnnotation : parameterAnnotationArrays[i]) {
                        if (parameterAnnotation.annotationType() == Field.class) {
                            parameterNames[i] = ((Field) parameterAnnotation).value();
                            parameterNamingTypes.put(parameterNames[i], N.typeOf(parameterTypes[i]));

                            break;
                        }
                    }
                }

                methodParameterNamesMap.put(methodName, parameterNames);
                methodParameterNamingTypesMap.put(methodName, parameterNamingTypes);

                JSONDeserializationConfig jdc = new JSONDeserializationConfig();
                jdc.setPropTypes(parameterNamingTypes);
                methodJSONDeserializationConfigMap.put(methodName, jdc);

                XMLDeserializationConfig xdc = new XMLDeserializationConfig();
                xdc.setPropTypes(parameterNamingTypes);
                methodXMLDeserializationConfigMap.put(methodName, xdc);
            }
        }

        //    String urlMethodMapperParameter = getInitParameter(config, PATH_MAPPER);
        //
        //    if (N.notNullOrEmpty(urlMethodMapperParameter)) {
        //        urlMethodMapperParameter = urlMethodMapperParameter.trim();
        //
        //        String[] sts = urlMethodMapperParameter.split(WD.SEMICOLON);
        //
        //        for (String st : sts) {
        //            String[] tmp = st.split(WD.EQUAL);
        //            pathMethodMap.put(tmp[1].trim(), findDeclaredMethodByName(serviceImplClass, tmp[0].trim()));
        //        }
        //    }
        //
        //    String httpMethodMapperParameter = getInitParameter(config, HTTP_METHOD_MAPPER);
        //
        //    if (N.notNullOrEmpty(httpMethodMapperParameter)) {
        //        httpMethodMapperParameter = httpMethodMapperParameter.trim();
        //
        //        final String[] sts = httpMethodMapperParameter.split(WD.SEMICOLON);
        //
        //        for (String st : sts) {
        //            final String[] tmp = st.split(WD.EQUAL);
        //            final String[] httpMethods = tmp[1].split(WD.COMMA);
        //            final String methodName = tmp[0].trim();
        //
        //            if (N.notNullOrEmpty(httpMethods)) {
        //                if ("ALL".equalsIgnoreCase(httpMethods[0].trim())) {
        //                    methodHttpMethodMap.put(methodName, N.asSet(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
        //                } else {
        //                    final Set<HttpMethod> set = N.newHashSet();
        //
        //                    for (String e : httpMethods) {
        //                        set.add(HttpMethod.valueOf(e.trim().toUpperCase()));
        //                    }
        //
        //                    methodHttpMethodMap.put(methodName, set);
        //                }
        //            }
        //        }
        //    }

        //        for (String key : new ArrayList<>(urlMethodMap.keySet())) {
        //            urlMethodMap.put(key.toLowerCase(), urlMethodMap.get(key));
        //            urlMethodMap.put(key.toUpperCase(), urlMethodMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(methodParameterClassMap.keySet())) {
        //            methodParameterClassMap.put(key.toLowerCase(), methodParameterClassMap.get(key));
        //            methodParameterClassMap.put(key.toUpperCase(), methodParameterClassMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(parameterMethodMap.keySet())) {
        //            parameterMethodMap.put(key.toLowerCase(), parameterMethodMap.get(key));
        //            parameterMethodMap.put(key.toUpperCase(), parameterMethodMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(eleNameMethodMap.keySet())) {
        //            eleNameMethodMap.put(key.toLowerCase(), eleNameMethodMap.get(key));
        //            eleNameMethodMap.put(key.toUpperCase(), eleNameMethodMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(eleNameParameterClassMap.keySet())) {
        //            eleNameParameterClassMap.put(key.toLowerCase(), eleNameParameterClassMap.get(key));
        //            eleNameParameterClassMap.put(key.toUpperCase(), eleNameParameterClassMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(methodHttpMethodMap.keySet())) {
        //            methodHttpMethodMap.put(key.toLowerCase(), methodHttpMethodMap.get(key));
        //            methodHttpMethodMap.put(key.toUpperCase(), methodHttpMethodMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(methodParameterNamesMap.keySet())) {
        //            methodParameterNamesMap.put(key.toLowerCase(), methodParameterNamesMap.get(key));
        //            methodParameterNamesMap.put(key.toUpperCase(), methodParameterNamesMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(methodParameterNamingTypesMap.keySet())) {
        //            methodParameterNamingTypesMap.put(key.toLowerCase(), methodParameterNamingTypesMap.get(key));
        //            methodParameterNamingTypesMap.put(key.toUpperCase(), methodParameterNamingTypesMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(methodJSONDeserializationConfigMap.keySet())) {
        //            methodJSONDeserializationConfigMap.put(key.toLowerCase(), methodJSONDeserializationConfigMap.get(key));
        //            methodJSONDeserializationConfigMap.put(key.toUpperCase(), methodJSONDeserializationConfigMap.get(key));
        //        }
        //
        //        for (String key : new ArrayList<>(methodXMLDeserializationConfigMap.keySet())) {
        //            methodXMLDeserializationConfigMap.put(key.toLowerCase(), methodXMLDeserializationConfigMap.get(key));
        //            methodXMLDeserializationConfigMap.put(key.toUpperCase(), methodXMLDeserializationConfigMap.get(key));
        //        }

        serviceRRLogger = LoggerFactory.getLogger(N.isNullOrEmpty(serviceImplClass.getInterfaces()) ? serviceImplClass : serviceImplClass.getInterfaces()[0]);
        serviceImplLogger = LoggerFactory.getLogger(serviceImplClass);
    }

    /**
     *
     * @param request
     * @param response
     * @throws ServletException the servlet exception
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        execute(request, response);
    }

    /**
     *
     * @param request
     * @param response
     * @throws ServletException the servlet exception
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        execute(request, response);
    }

    /**
     *
     * @param request
     * @param response
     * @throws ServletException the servlet exception
     */
    @Override
    protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        execute(request, response);
    }

    /**
     *
     * @param request
     * @param response
     * @throws ServletException the servlet exception
     */
    @Override
    protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        execute(request, response);
    }

    /**
     *
     * @param request
     * @param response
     */
    protected void execute(final HttpServletRequest request, final HttpServletResponse response) {
        preExecute(request, response);

        final String url = request.getRequestURL().toString();
        final HttpMethod httpMethodName = HttpMethod.valueOf(request.getMethod());
        final boolean isGetOrDelete = httpMethodName.equals(HttpMethod.GET) || httpMethodName.equals(HttpMethod.DELETE);

        InputStream is = null;
        Method method = null;
        Object parameter = null;
        Class<?> parameterClass = null;
        Object result = null;

        try {
            int index = url.lastIndexOf('/');

            if ((index > 0) && (index < url.length())) {
                method = pathMethodMap.get(url.substring(index + 1));
            }

            if (isGetOrDelete) {
                if (method == null) {
                    String msg = "The target web service method can't be identified by url";
                    serviceImplLogger.error(msg);
                    throw new RuntimeException(msg);
                }

                parameterClass = methodParameterClassMap.get(method.getName());

                if (parameterClass != null) {
                    if (Map.class.isAssignableFrom(parameterClass) && methodParameterNamesMap.containsKey(method.getName())) {
                        parameter = jsonParser.deserialize(parameterClass, jsonParser.serialize(URLEncodedUtil.decode(request.getQueryString())),
                                methodJSONDeserializationConfigMap.get(method.getName()));
                    } else {
                        Map<String, String[]> parameterMap = request.getParameterMap();
                        parameter = URLEncodedUtil.parameters2Entity(parameterClass, parameterMap);
                    }
                }
            }

            final ContentFormat requestContentFormat = getRequestContentFormat(request);
            final Charset requestCharset = HttpUtil.getCharset(getContentType(request));
            final ContentFormat responseContentFormat = getResponseContentFormat(request, requestContentFormat);
            final Charset acceptCharset = HttpUtil.getCharset(getAcceptCharset(request), requestCharset);

            if (N.notNullOrEmpty(encryptionUserName) && N.notNullOrEmpty(encryptionPassword)) {
                if (((SecurityDTO) parameter).decrypt(encryptionUserName, encryptionPassword, encryptionMessage) == false) {
                    result = N.newInstance(method.getReturnType());

                    Method respCodeSetMethod = ClassUtil.getPropSetMethod(result.getClass(), "setResponseCode");
                    if (respCodeSetMethod == null) {
                        respCodeSetMethod = ClassUtil.getPropSetMethod(result.getClass(), "setRespCode");
                    }

                    if (respCodeSetMethod != null) {
                        ClassUtil.setPropValue(result, respCodeSetMethod, "1010");
                    }

                    Method respMessageSetMethod = ClassUtil.getPropSetMethod(result.getClass(), "setResponseMessage");
                    if (respMessageSetMethod == null) {
                        respMessageSetMethod = ClassUtil.getPropSetMethod(result.getClass(), "setRespMessage");
                    }

                    if (respMessageSetMethod != null) {
                        ClassUtil.setPropValue(result, respMessageSetMethod, "Security issue: Invalid request.");
                    }
                }
            } else {
                switch (requestContentFormat) {
                    case JSON:
                    case JSON_LZ4:
                    case JSON_SNAPPY:
                    case JSON_GZIP:

                        if (method == null) {
                            String msg = "Unsupported opearation";
                            serviceImplLogger.error(msg);
                            throw new RuntimeException(msg);
                        } else {
                            checkHttpMethod(method, httpMethodName);

                            if (parameter == null) {
                                parameterClass = methodParameterClassMap.get(method.getName());

                                if (parameterClass == null) {
                                    result = ClassUtil.invokeMethod(serviceImpl, method);
                                } else {
                                    is = getInputStream(request, requestContentFormat);
                                    Type<Object> paramType = N.typeOf(parameterClass);
                                    boolean hasFieldAnnotation = parameterClass == Map.class && methodParameterNamesMap.containsKey(method.getName());

                                    if (hasFieldAnnotation) {
                                        parameter = jsonParser.deserialize(parameterClass, is, methodJSONDeserializationConfigMap.get(method.getName()));
                                    } else {
                                        if (paramType.isSerializable()) {
                                            parameter = paramType.valueOf(IOUtil.readString(is, requestCharset));
                                        } else {
                                            parameter = jsonParser.deserialize(parameterClass, is);
                                        }
                                    }

                                    if (serviceRRLogger.isInfoEnabled()) {
                                        if (paramType.isSerializable()) {
                                            serviceRRLogger.info(paramType.stringOf(parameter));
                                        } else {
                                            serviceRRLogger.info(jsonParser.serialize(parameter));
                                        }
                                    }

                                    result = invoke(method, parameter, hasFieldAnnotation);
                                }
                            } else {
                                boolean hasFieldAnnotation = parameterClass == Map.class && methodParameterNamesMap.containsKey(method.getName());
                                final Type<Object> paramType = N.typeOf(parameter.getClass());

                                if (serviceRRLogger.isInfoEnabled()) {
                                    if (paramType.isSerializable()) {
                                        serviceRRLogger.info(paramType.stringOf(parameter));
                                    } else {
                                        serviceRRLogger.info(jsonParser.serialize(parameter));
                                    }
                                }

                                result = invoke(method, parameter, hasFieldAnnotation);
                            }
                        }

                        break;

                    case XML:
                    case XML_LZ4:
                    case XML_SNAPPY:
                    case XML_GZIP:

                        if (method == null) {
                            is = getInputStream(request, requestContentFormat);
                            parameter = xmlParser.deserialize(eleNameParameterClassMap, is, null);
                            method = parameterMethodMap.get(ClassUtil.getSimpleClassName(parameter.getClass()));

                            checkHttpMethod(method, httpMethodName);

                            if (serviceRRLogger.isInfoEnabled()) {
                                serviceRRLogger.info(xmlParser.serialize(parameter));
                            }

                            result = ClassUtil.invokeMethod(serviceImpl, method, parameter);
                        } else {
                            checkHttpMethod(method, httpMethodName);

                            if (parameter == null) {
                                parameterClass = methodParameterClassMap.get(method.getName());

                                if (parameterClass == null) {
                                    result = ClassUtil.invokeMethod(serviceImpl, method);
                                } else {
                                    is = getInputStream(request, requestContentFormat);
                                    boolean hasFieldAnnotation = parameterClass == Map.class && methodParameterNamesMap.containsKey(method.getName());

                                    if (hasFieldAnnotation) {
                                        parameter = xmlParser.deserialize(parameterClass, is, methodXMLDeserializationConfigMap.get(method.getName()));
                                    } else {
                                        parameter = xmlParser.deserialize(parameterClass, is);
                                    }

                                    if (serviceRRLogger.isInfoEnabled()) {
                                        serviceRRLogger.info(xmlParser.serialize(parameter));
                                    }

                                    result = invoke(method, parameter, hasFieldAnnotation);
                                }
                            } else {
                                boolean hasFieldAnnotation = parameterClass == Map.class && methodParameterNamesMap.containsKey(method.getName());

                                if (serviceRRLogger.isInfoEnabled()) {
                                    serviceRRLogger.info(xmlParser.serialize(parameter));
                                }

                                result = invoke(method, parameter, hasFieldAnnotation);
                            }
                        }

                        break;

                    case FormUrlEncoded:
                        if (method == null) {
                            String msg = "Unsupported opearation";
                            serviceImplLogger.error(msg);
                            throw new RuntimeException(msg);
                        } else {
                            checkHttpMethod(method, httpMethodName);

                            if (parameter == null) {
                                parameterClass = methodParameterClassMap.get(method.getName());

                                if (parameterClass == null) {
                                    result = ClassUtil.invokeMethod(serviceImpl, method);
                                } else {
                                    is = getInputStream(request, requestContentFormat);
                                    Type<Object> paramType = N.typeOf(parameterClass);
                                    boolean hasFieldAnnotation = parameterClass == Map.class && methodParameterNamesMap.containsKey(method.getName());

                                    parameter = URLEncodedUtil.decode(parameterClass, IOUtil.readString(is, requestCharset));

                                    if (serviceRRLogger.isInfoEnabled()) {
                                        if (paramType.isSerializable()) {
                                            serviceRRLogger.info(paramType.stringOf(parameter));
                                        } else {
                                            serviceRRLogger.info(jsonParser.serialize(parameter));
                                        }
                                    }

                                    result = invoke(method, parameter, hasFieldAnnotation);
                                }
                            } else {
                                boolean hasFieldAnnotation = parameterClass == Map.class && methodParameterNamesMap.containsKey(method.getName());
                                final Type<Object> paramType = N.typeOf(parameter.getClass());

                                if (serviceRRLogger.isInfoEnabled()) {
                                    if (paramType.isSerializable()) {
                                        serviceRRLogger.info(paramType.stringOf(parameter));
                                    } else {
                                        serviceRRLogger.info(jsonParser.serialize(parameter));
                                    }
                                }

                                result = invoke(method, parameter, hasFieldAnnotation);
                            }
                        }

                        break;

                    case KRYO:

                        if (method == null) {
                            String msg = "Unsupported opearation";
                            serviceImplLogger.error(msg);
                            throw new RuntimeException(msg);
                        } else {
                            checkHttpMethod(method, httpMethodName);

                            if (parameter == null) {
                                parameterClass = methodParameterClassMap.get(method.getName());

                                if (parameterClass == null) {
                                    result = ClassUtil.invokeMethod(serviceImpl, method);
                                } else {
                                    is = getInputStream(request, requestContentFormat);
                                    parameter = kryoParser.deserialize(parameterClass, is);

                                    final Type<Object> paramType = N.typeOf(parameter.getClass());

                                    if (serviceRRLogger.isInfoEnabled()) {
                                        if (paramType.isSerializable()) {
                                            serviceRRLogger.info(paramType.stringOf(parameter));
                                        } else {
                                            serviceRRLogger.info(jsonParser.serialize(parameter));
                                        }
                                    }

                                    result = ClassUtil.invokeMethod(serviceImpl, method, parameter);
                                }
                            } else {
                                final Type<Object> paramType = N.typeOf(parameter.getClass());

                                if (serviceRRLogger.isInfoEnabled()) {
                                    if (paramType.isSerializable()) {
                                        serviceRRLogger.info(paramType.stringOf(parameter));
                                    } else {
                                        serviceRRLogger.info(jsonParser.serialize(parameter));
                                    }
                                }

                                result = ClassUtil.invokeMethod(serviceImpl, method, parameter);
                            }
                        }

                        break;

                    default:
                        String msg = "Unsupported content format: " + requestContentFormat;
                        serviceImplLogger.error(msg);
                        throw new RuntimeException(msg);
                }
            }

            postExecute(response, result, method, parameter, requestContentFormat);

            setResponse(response, result, responseContentFormat, acceptCharset);
        } catch (IOException e) {
            String msg = "Failed to process request";
            serviceImplLogger.error(msg, e);
            throw new UncheckedIOException(msg, e);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     *
     * @param method
     * @param parameter
     * @param hasFieldAnnotation
     * @return
     */
    protected Object invoke(final Method method, final Object parameter, final boolean hasFieldAnnotation) {
        Object result = null;

        if (hasFieldAnnotation) {
            String[] parameterNames = methodParameterNamesMap.get(method.getName());
            Object[] parameters = new Object[parameterNames.length];

            Map<String, Object> parameterMap = (Map<String, Object>) parameter;
            for (int i = 0, len = parameterNames.length; i < len; i++) {
                parameters[i] = parameterMap.get(parameterNames[i]);
            }

            result = ClassUtil.invokeMethod(serviceImpl, method, parameters);
        } else {
            result = ClassUtil.invokeMethod(serviceImpl, method, parameter);
        }

        return result;
    }

    /**
     * Sets the response.
     *
     * @param response
     * @param result
     * @param responseContentFormat
     * @param acceptCharset
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void setResponse(final HttpServletResponse response, final Object result, final ContentFormat responseContentFormat, final Charset acceptCharset)
            throws IOException {
        if (result == null) {
            return;
        }

        OutputStream os = null;

        try {
            os = getOutputStream(response, responseContentFormat, acceptCharset);

            switch (responseContentFormat) {
                case JSON:
                case JSON_LZ4:
                case JSON_SNAPPY:
                case JSON_GZIP: {
                    final Type<Object> type = N.typeOf(result.getClass());

                    if (serviceRRLogger.isInfoEnabled()) {
                        String str = null;
                        if (type.isSerializable()) {
                            str = type.stringOf(response);
                        } else {
                            str = jsonParser.serialize(result);
                        }

                        serviceRRLogger.info(str);

                        os.write(str.getBytes());
                    } else {
                        if (type.isSerializable()) {
                            os.write(type.stringOf(response).getBytes());
                        } else {
                            jsonParser.serialize(os, result);
                        }
                    }

                    break;
                }

                case XML:
                case XML_LZ4:
                case XML_SNAPPY:
                case XML_GZIP: {
                    if (serviceRRLogger.isInfoEnabled()) {
                        String str = xmlParser.serialize(result);
                        serviceRRLogger.info(str);
                        os.write(str.getBytes());
                    } else {
                        xmlParser.serialize(os, result);
                    }

                    break;
                }

                case FormUrlEncoded: {
                    if (serviceRRLogger.isInfoEnabled()) {
                        serviceRRLogger.info(jsonParser.serialize(result));
                    }

                    os.write(URLEncodedUtil.encode(result, acceptCharset).getBytes(acceptCharset));

                    break;
                }

                case KRYO: {
                    if (serviceRRLogger.isInfoEnabled()) {
                        final Type<Object> type = N.typeOf(result.getClass());

                        if (type.isSerializable()) {
                            serviceRRLogger.info(type.stringOf(result));
                        } else {
                            serviceRRLogger.info(jsonParser.serialize(result));
                        }
                    }

                    kryoParser.serialize(os, result);

                    break;
                }

                default:
                    String msg = "Unsupported content type: " + responseContentFormat;
                    serviceImplLogger.error(msg);
                    throw new RuntimeException(msg);
            }

            flush(os);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param request
     * @param response
     */
    protected void preExecute(final HttpServletRequest request, final HttpServletResponse response) {

    }

    /**
     *
     * @param response
     * @param result
     * @param method
     * @param parameter
     * @param contentFormat
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void postExecute(final HttpServletResponse response, final Object result, final Method method, final Object parameter,
            final ContentFormat contentFormat) throws IOException {

    }

    /**
     * Check http method.
     *
     * @param method
     * @param httpMethodName
     */
    protected void checkHttpMethod(final Method method, final HttpMethod httpMethodName) {
        if (!methodHttpMethodMap.get(method.getName()).contains(httpMethodName)) {
            String msg = "HTTP method '" + httpMethodName + "' is not supported by operation: " + method.getName();
            serviceImplLogger.error(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Find declared method by name.
     *
     * @param cls
     * @param methodName
     * @return
     */
    protected static Method findDeclaredMethodByName(final Class<?> cls, final String methodName) {
        Method method = null;

        Method[] methods = cls.getDeclaredMethods();

        for (Method m : methods) {
            if (m.getName().equalsIgnoreCase(methodName)) {
                if ((method == null) || Modifier.isPublic(m.getModifiers())
                        || (Modifier.isProtected(m.getModifiers()) && (!Modifier.isProtected(method.getModifiers())))
                        || (!Modifier.isPrivate(m.getModifiers()) && Modifier.isPrivate(method.getModifiers()))) {

                    method = m;
                }

                if (Modifier.isPublic(method.getModifiers())) {
                    break;
                }
            }
        }

        // SHOULD NOT set it true here.
        // if (method != null) {
        // ClassUtil.setAccessible(method, true);
        // }

        return method;
    }
}
