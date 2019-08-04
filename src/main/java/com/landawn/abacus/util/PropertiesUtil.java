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

package com.landawn.abacus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.Exclusion;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.SQLExecutor.JdbcSettings;
import com.landawn.abacus.util.SQLExecutor.ResultExtractor;

/**
 *
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    private static final String TYPE = "type";
    private static final XMLSerializationConfig xsc = XSC.of(true, true, DateTimeFormat.ISO_8601_DATETIME, Exclusion.NONE, null);

    private static final ResultExtractor<ConfigEntity> CONFIG_ENTITY_RESULT_SET_EXTRACTOR = new ResultExtractor<ConfigEntity>() {
        @Override
        public ConfigEntity extractData(ResultSet rs, JdbcSettings jdbcSettings) throws SQLException {
            long offset = jdbcSettings.getOffset();
            long count = jdbcSettings.getCount();

            if (JdbcUtil.skip(rs, offset) >= offset) {
                while ((count-- > 0) && rs.next()) {
                    ConfigEntity entity = new ConfigEntity();
                    List<String> columnLabelList = JdbcUtil.getColumnLabelList(rs);
                    int columnCount = columnLabelList.size();
                    Method method = null;
                    Object propValue = null;

                    for (int i = 0; i < columnCount; i++) {
                        method = ClassUtil.getPropSetMethod(ConfigEntity.class, columnLabelList.get(i));
                        propValue = JdbcUtil.getColumnValue(rs, i + 1);

                        if (method != null) {
                            if (method.getName().equals("setIncludedServers") || method.getName().equals("setExcludedServers")) {
                                if (propValue == null || N.isNullOrEmpty(propValue.toString().trim())) {
                                    propValue = new ArrayList<>();
                                } else {
                                    propValue = Splitter.defauLt().trim(true).split(propValue.toString().trim());
                                }
                            } else if (method.getName().equals("setStatus")) {
                                if (propValue == null || N.isNullOrEmpty(propValue.toString().trim())) {
                                    propValue = null;
                                } else {
                                    propValue = Status.valueOf(propValue.toString().trim());
                                }
                            }

                            ClassUtil.setPropValue(entity, method, propValue);
                        }
                    }

                    return entity;
                }
            }

            return null;
        }
    };

    private static final ScheduledExecutorService scheduledExecutor;
    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        scheduledExecutor = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    private static final Map<Resource, Properties<?, ?>> registeredAutoRefreshProperties = new ConcurrentHashMap<>(256);

    static {
        final Runnable refreshTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (registeredAutoRefreshProperties) {
                    Map<Properties<?, ?>, Resource> m = null;

                    Properties<?, ?> properties = null;
                    Resource resource = null;
                    File file = null;
                    SQLExecutor sqlExecutor = null;
                    String sql = null;
                    ResultExtractor<ConfigEntity> resultExtractor = null;

                    for (Map.Entry<Resource, Properties<?, ?>> entry : registeredAutoRefreshProperties.entrySet()) {
                        resource = entry.getKey();
                        properties = entry.getValue();

                        file = resource.getFile();

                        if (file != null) {
                            if (file.lastModified() > resource.getLastLoadTime()) {
                                long lastLoadTime = file.lastModified();
                                InputStream is = null;

                                if (logger.isWarnEnabled()) {
                                    logger.warn("Start to refresh properties with the updated file: " + file.getAbsolutePath());
                                    logger.warn("[PROPERTIES]" + properties);
                                }

                                try {
                                    is = new FileInputStream(resource.getFile());

                                    if (resource.getType() == ResourceType.PROPERTIES) {
                                        load((Properties<String, String>) properties, is);
                                    } else {
                                        loadFromXML(properties, properties.getClass(), is);
                                    }

                                    if (m == null) {
                                        m = new HashMap<>();
                                    }

                                    m.put(properties, resource);

                                    resource.setLastLoadTime(lastLoadTime);
                                } catch (Exception e) {
                                    logger.error("Failed to refresh properties: " + properties, e);
                                } finally {
                                    IOUtil.close(is);
                                }

                                if (logger.isWarnEnabled()) {
                                    logger.warn("End to refresh properties with the updated file: " + file.getAbsolutePath());
                                    logger.warn("[NEW PROPERTIES]" + properties);
                                }
                            }
                        } else {
                            sqlExecutor = resource.getSqlExecutor();
                            sql = resource.getSql();
                            resultExtractor = resource.getResultSetExtractor();
                            try {
                                if (resultExtractor == null) {
                                    resultExtractor = CONFIG_ENTITY_RESULT_SET_EXTRACTOR;
                                }

                                ConfigEntity entity = sqlExecutor.query(sql, resultExtractor);

                                if (entity == null || N.isNullOrEmpty(entity.getContent())) {
                                    throw new AbacusException("No record found or the content of properties is empty");
                                }

                                if (entity.getLastUpdateTime().getTime() > resource.getLastLoadTime()) {
                                    boolean isIncluded = false;

                                    if (N.notNullOrEmpty(entity.getExcludedServers())) {
                                        isIncluded = true;

                                        for (String serverName : entity.getExcludedServers()) {
                                            if (IOUtil.HOST_NAME.matches(serverName)) {
                                                isIncluded = false;

                                                break;
                                            }
                                        }
                                    } else if (N.notNullOrEmpty(entity.getIncludedServers())) {
                                        for (String serverName : entity.getIncludedServers()) {
                                            if (IOUtil.HOST_NAME.matches(serverName)) {
                                                isIncluded = true;

                                                break;
                                            }
                                        }
                                    } else {
                                        isIncluded = true;
                                    }

                                    if (isIncluded) {
                                        if (logger.isWarnEnabled()) {
                                            logger.warn("Start to refresh properties with sql: " + sql);
                                            logger.warn("[PROPERTIES]" + properties);
                                        }

                                        if (resource.getType() == ResourceType.PROPERTIES) {
                                            load((Properties<String, String>) properties, IOUtil.string2InputStream(entity.getContent()));
                                        } else {
                                            loadFromXML(properties, properties.getClass(), IOUtil.string2InputStream(entity.getContent()));
                                        }

                                        if (m == null) {
                                            m = new HashMap<>();
                                        }

                                        m.put(properties, resource);

                                        if (logger.isWarnEnabled()) {
                                            logger.warn("End to refresh properties with sql: " + sql);
                                            logger.warn("[NEW PROPERTIES]" + properties);
                                        }
                                    } else {
                                        if (logger.isWarnEnabled()) {
                                            logger.warn("Properties is not refreshed because it's excluded or not included by: [excludedServers]: "
                                                    + entity.getExcludedServers() + ". [includedServers]: " + entity.getIncludedServers());
                                        }
                                    }

                                    resource.setLastLoadTime(entity.getLastUpdateTime().getTime());
                                }
                            } catch (Exception e) {
                                logger.error("Failed to refresh properties: " + properties, e);
                            }

                        }
                    }
                }
            }
        };

        scheduledExecutor.scheduleWithFixedDelay(refreshTask, 1000, 1000, TimeUnit.MICROSECONDS);
    }

    private PropertiesUtil() {
        // singleton.
    }

    public static File findFile(String configFileName) {
        return Configuration.findFile(configFileName);
    }

    public static File findDir(String configDir) {
        return Configuration.findDir(configDir);
    }

    public static Properties<String, String> load(File file) {
        return load(file, false);
    }

    public static Properties<String, String> load(File file, boolean autoRefresh) {
        Properties<String, String> properties = null;

        InputStream is = null;
        try {
            is = new FileInputStream(file);

            if (autoRefresh) {
                Resource resource = new Resource(Properties.class, file, ResourceType.PROPERTIES);
                resource.setLastLoadTime(file.lastModified());

                synchronized (registeredAutoRefreshProperties) {
                    properties = (Properties<String, String>) registeredAutoRefreshProperties.get(resource);

                    if (properties == null) {
                        properties = load(is);
                        registeredAutoRefreshProperties.put(resource, properties);
                    }
                }
            } else {
                properties = load(is);
            }

            return properties;
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
        }
    }

    public static Properties<String, String> load(InputStream is) {
        return load(null, is);
    }

    private static Properties<String, String> load(Properties<String, String> targetProperties, InputStream is) {
        java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return create(targetProperties, tmp);
    }

    public static Properties<String, String> load(Reader reader) {
        java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return create(null, tmp);
    }

    public static Properties<String, String> load(SQLExecutor sqlExecutor, String sql, boolean autoRefresh) {
        return load(null, sqlExecutor, sql, autoRefresh);
    }

    private static Properties<String, String> load(Properties<String, String> targetProperties, SQLExecutor sqlExecutor, String sql, boolean autoRefresh) {

        ConfigEntity entity = sqlExecutor.query(sql, CONFIG_ENTITY_RESULT_SET_EXTRACTOR);

        if (entity == null || N.isNullOrEmpty(entity.getContent())) {
            throw new AbacusException("No record found or the content of properties is empty");
        }

        Properties<String, String> properties = null;

        if (autoRefresh) {
            Class<?> targetClass = targetProperties == null ? Properties.class : targetProperties.getClass();
            Resource resource = new Resource(targetClass, sqlExecutor, sql, CONFIG_ENTITY_RESULT_SET_EXTRACTOR, ResourceType.PROPERTIES);
            resource.setLastLoadTime(entity.getLastUpdateTime().getTime());

            synchronized (registeredAutoRefreshProperties) {
                properties = (Properties<String, String>) registeredAutoRefreshProperties.get(resource);

                if (properties == null) {
                    properties = load(targetProperties, IOUtil.string2InputStream(entity.getContent()));

                    registeredAutoRefreshProperties.put(resource, properties);
                }
            }
        } else {
            properties = load(targetProperties, IOUtil.string2InputStream(entity.getContent()));
        }

        return properties;
    }

    private static Properties<String, String> create(Properties<String, String> targetProperties, java.util.Properties newProperties) {
        Properties<String, String> properties = null;
        if (targetProperties == null) {
            properties = new Properties<>();
        } else {
            properties = targetProperties;
        }

        Set<String> newKeySet = new HashSet<>();
        Enumeration<?> it = newProperties.propertyNames();
        String propName = null;

        while (it.hasMoreElements()) {
            propName = it.nextElement().toString();
            properties.set(propName, newProperties.getProperty(propName));
            newKeySet.add(propName);
        }

        if (targetProperties != null) {
            Set<String> oldKeySet = new HashSet<>(properties.keySet());

            for (String key : oldKeySet) {
                if (!newKeySet.contains(key)) {
                    properties.remove(key);
                }
            }
        }

        return properties;
    }

    public static Properties<String, Object> loadFromXML(File file) {
        return loadFromXML(file, false);
    }

    public static Properties<String, Object> loadFromXML(File file, boolean autoRefresh) {
        return loadFromXML(Properties.class, file, autoRefresh);
    }

    public static Properties<String, Object> loadFromXML(InputStream is) {
        return loadFromXML(Properties.class, is);
    }

    public static Properties<String, String> loadFromXML(SQLExecutor sqlExecutor, String sql, boolean autoRefresh) {
        return loadFromXML(Properties.class, sqlExecutor, sql, autoRefresh);
    }

    public static <T extends Properties<String, Object>> T loadFromXML(Class<T> targetClass, File file) {
        return loadFromXML(targetClass, file, false);
    }

    /**
     *
     * @param targetClass
     * @param file
     * @param autoRefresh
     * @return
     */
    public static <T extends Properties<String, Object>> T loadFromXML(Class<T> targetClass, File file, boolean autoRefresh) {
        T properties = null;
        InputStream is = null;

        try {
            is = new FileInputStream(file);

            if (autoRefresh) {
                Resource resource = new Resource(targetClass, file, ResourceType.XML);
                resource.setLastLoadTime(file.lastModified());

                synchronized (registeredAutoRefreshProperties) {
                    properties = (T) registeredAutoRefreshProperties.get(resource);

                    if (properties == null) {
                        properties = loadFromXML(targetClass, is);

                        registeredAutoRefreshProperties.put(resource, properties);
                    }
                }
            } else {
                properties = loadFromXML(targetClass, is);
            }

            return properties;
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
        }
    }

    public static <T extends Properties<String, Object>> T loadFromXML(Class<T> targetClass, InputStream is) {
        return loadFromXML(null, targetClass, is);
    }

    public static <T extends Properties<String, Object>> T loadFromXML(Class<T> targetClass, SQLExecutor sqlExecutor, String sql, boolean autoRefresh) {
        ConfigEntity entity = sqlExecutor.query(sql, CONFIG_ENTITY_RESULT_SET_EXTRACTOR);

        if (entity == null || N.isNullOrEmpty(entity.getContent())) {
            throw new AbacusException("No record found or the content of properties is empty");
        }

        T properties = null;

        if (autoRefresh) {
            Resource resource = new Resource(targetClass, sqlExecutor, sql, CONFIG_ENTITY_RESULT_SET_EXTRACTOR, ResourceType.XML);
            resource.setLastLoadTime(entity.getLastUpdateTime().getTime());

            synchronized (registeredAutoRefreshProperties) {
                properties = (T) registeredAutoRefreshProperties.get(resource);

                if (properties == null) {
                    properties = loadFromXML(targetClass, IOUtil.string2InputStream(entity.getContent()));

                    registeredAutoRefreshProperties.put(resource, properties);
                }
            }
        } else {
            properties = loadFromXML(targetClass, IOUtil.string2InputStream(entity.getContent()));
        }

        return properties;
    }

    private static <T extends Properties<String, Object>> T loadFromXML(Object targetProperties, Class<T> targetClass, InputStream is) {
        DocumentBuilder docBuilder = XMLUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(is);
        } catch (SAXException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Node node = doc.getFirstChild();
        return loadFromXML(targetProperties, targetClass, node, null, true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Properties<String, Object>> T loadFromXML(Object targetProperties, Class<T> inputClass, Node node, Method propSetMethod,
            boolean isFirstCall) {

        // TODO it's difficult to support duplicated property and may be misused.
        if (hasDuplicatedPropName(node)) {
            throw new AbacusException("The source xml document contains duplicated properties which has same node tag name in the same root.");
        }

        Class<?> targetClass = null;
        if (isFirstCall) {
            targetClass = targetProperties == null ? (inputClass == null ? Properties.class : inputClass) : targetProperties.getClass();
        } else {
            targetClass = (propSetMethod == null) ? Properties.class : propSetMethod.getParameterTypes()[0];
        }

        T properties = (T) (targetProperties == null ? N.newInstance(targetClass) : targetProperties);

        NodeList propNodes = node.getChildNodes();
        int propNodeLength = (propNodes == null) ? 0 : propNodes.getLength();
        Set<String> newKeySet = new HashSet<>();
        Node propNode = null;
        String typeAttr = null;
        String propName = null;
        Object propValue = null;

        for (int i = 0; i < propNodeLength; i++) {
            propNode = propNodes.item(i);

            if (propNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            propName = ClassUtil.formalizePropName(propNode.getNodeName());
            newKeySet.add(propName);

            typeAttr = XMLUtil.getAttribute(propNode, TYPE);
            propSetMethod = ClassUtil.getPropSetMethod(targetClass, propName);

            if (XMLUtil.isTextElement(propNode)) {
                if (N.isNullOrEmpty(typeAttr)) {
                    propValue = Configuration.getTextContent(propNode);
                } else {
                    propValue = N.typeOf(typeAttr).valueOf(Configuration.getTextContent(propNode));
                }
            } else {
                // TODO it's difficult to support duplicated property and may be misused.
                // How to get target property value for auto-refresh if it's list of Properties or entities.
                Object targetPropValue = properties.get(propName);
                Class<T> propClass = (Class<T>) (propSetMethod == null ? Properties.class : propSetMethod.getParameterTypes()[0]);
                propValue = loadFromXML(targetPropValue, propClass, propNode, propSetMethod, false);
            }

            Object oldPropValue = properties.get(propName);

            if (oldPropValue != null && oldPropValue.getClass().equals(propValue.getClass())
                    && (oldPropValue instanceof Collection || oldPropValue instanceof Map) && !(oldPropValue instanceof Properties)) {
                if (oldPropValue instanceof Collection) {
                    ((Collection) oldPropValue).clear();
                    ((Collection) oldPropValue).addAll((Collection) propValue);
                } else if (oldPropValue instanceof Map) {
                    ((Map) oldPropValue).clear();
                    ((Map) oldPropValue).putAll((Map) propValue);
                }
            } else {
                if (propSetMethod == null) {
                    // TODO it's difficult to support duplicated property and may be misused.
                    //                    if (properties.containsKey(propName)) {
                    //                        String listPropName = propName + "List";
                    //                        List<Object> listProp = (List<Object>) properties.get(listPropName);
                    //
                    //                        if (listProp == null) {
                    //                            listProp = Collections.synchronizedList(new ArrayList<Object>());
                    //                            properties.set(listPropName, listProp);
                    //                        }
                    //
                    //                        if (listProp.size() == 0) {
                    //                            listProp.add(properties.get(propName));
                    //                        }
                    //
                    //                        listProp.add(propValue);
                    //                    }
                    //
                    properties.set(propName, propValue);
                } else {
                    Class<?> parameterType = propSetMethod.getParameterTypes()[0];

                    if (N.isNullOrEmpty(propValue.toString()) && Properties.class.isAssignableFrom(parameterType)) {
                        propValue = N.newInstance(parameterType);
                    }

                    ClassUtil.setPropValue(properties, propSetMethod, propValue);
                }
            }
        }

        if (targetProperties != null) {

            Set<String> oldKeySet = new HashSet<>(properties.keySet());
            Method removeMethod = null;
            for (String key : oldKeySet) {
                if (!newKeySet.contains(key)) {
                    removeMethod = ClassUtil.getDeclaredMethod(properties.getClass(), "remove" + StringUtil.capitalize(key));

                    if (removeMethod == null) {
                        properties.remove(key);
                    } else {
                        ClassUtil.invokeMethod(properties, removeMethod);
                    }
                }
            }
        }

        return properties;

    }

    public static void store(Properties<?, ?> properties, File file, String comments) {
        OutputStream os = null;

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            os = new FileOutputStream(file);

            store(properties, os, comments);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    public static void store(Properties<?, ?> properties, OutputStream os, String comments) {
        BufferedWriter bw = Objectory.createBufferedWriter(os);

        try {
            store(properties, bw, comments);
        } finally {
            Objectory.recycle(bw);
        }
    }

    public static void store(Properties<?, ?> properties, Writer writer, String comments) {
        final java.util.Properties tmp = new java.util.Properties();

        for (Object key : properties.keySet()) {
            tmp.put(key, properties.get(key));
        }

        try {
            tmp.store(writer, comments);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void storeToXML(Properties<?, ?> properties, File file, String rootElementName, boolean ignoreTypeInfo) {
        OutputStream os = null;

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            os = new FileOutputStream(file);

            storeToXML(properties, os, rootElementName, ignoreTypeInfo);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    public static void storeToXML(Properties<?, ?> properties, OutputStream os, String rootElementName, boolean ignoreTypeInfo) {
        try {
            storeToXML(properties, os, rootElementName, ignoreTypeInfo, true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void storeToXML(Properties<?, ?> properties, OutputStream os, String rootElementName, boolean ignoreTypeInfo, boolean isFirstCall)
            throws IOException {
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(os);

        try {
            if (isFirstCall) {
                bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            }

            if (isFirstCall || ignoreTypeInfo) {
                bw.write("<" + rootElementName + ">");
            } else {
                if (properties.getClass().equals(Properties.class)) {
                    bw.write("<" + rootElementName + " type=\"Properties\">");
                } else {
                    bw.write("<" + rootElementName + ">");
                }
            }

            String listPropName = null;
            String elementPropName = null;
            Object propValue = null;
            Object listPropvalue;
            Type<Object> type = null;
            for (Object propName : properties.keySet()) {
                listPropName = propName + "List";
                elementPropName = propName.toString();

                if (elementPropName.endsWith("List")) {
                    elementPropName = elementPropName.substring(0, elementPropName.length() - 4);
                }

                propValue = properties.get(propName);
                listPropvalue = properties.get(listPropName);

                if (propValue == null) {
                    continue;
                }

                if (listPropvalue != null && listPropvalue instanceof List && ((List<?>) listPropvalue).size() > 0) {
                    continue;
                }

                if (propValue instanceof List && properties.containsKey(elementPropName)) {
                    for (Object e : ((List<?>) propValue)) {
                        if (e == null) {
                            continue;
                        } else if (e instanceof Properties) {
                            bw.flush();

                            storeToXML((Properties<?, ?>) e, os, elementPropName, ignoreTypeInfo, false);
                        } else {
                            type = N.typeOf(e.getClass());

                            if (ignoreTypeInfo) {
                                bw.write("<" + elementPropName + ">");
                            } else {
                                if (Primitives.isWrapperType(type.clazz())) {
                                    bw.write("<" + elementPropName + " type=\"" + ClassUtil.getSimpleClassName(Primitives.unwrap(type.clazz())) + "\">");
                                } else {
                                    bw.write("<" + elementPropName + " type=\"" + type.declaringName() + "\">");
                                }
                            }

                            type.writeCharacter(bw, e, xsc);

                            bw.write("</" + elementPropName + ">");
                        }
                    }

                } else if (propValue instanceof Properties) {
                    bw.flush();

                    storeToXML((Properties<?, ?>) propValue, os, propName.toString(), ignoreTypeInfo, false);
                } else {
                    type = N.typeOf(propValue.getClass());

                    if (ignoreTypeInfo) {
                        bw.write("<" + propName + ">");
                    } else {
                        if (Primitives.isWrapperType(type.clazz())) {
                            bw.write("<" + propName + " type=\"" + ClassUtil.getSimpleClassName(Primitives.unwrap(type.clazz())) + "\">");
                        } else {
                            bw.write("<" + propName + " type=\"" + type.declaringName() + "\">");
                        }
                    }

                    type.writeCharacter(bw, propValue, xsc);

                    bw.write("</" + propName + ">");
                }
            }

            bw.write("</" + rootElementName + ">");

            bw.flush();

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Generate java code by the specified xml.
     *
     * @param xml
     * @param srcPath
     * @param packageName
     * @param className
     * @param isPublicField
     */
    public static void xml2Java(String xml, String srcPath, String packageName, String className, boolean isPublicField) {
        xml2Java(IOUtil.string2InputStream(xml), srcPath, packageName, className, isPublicField);
    }

    /**
     * Generate java code by the specified xml.
     *
     * @param file
     * @param srcPath
     * @param packageName
     * @param className
     * @param isPublicField
     */
    public static void xml2Java(File file, String srcPath, String packageName, String className, boolean isPublicField) {
        InputStream is = null;

        try {
            is = new FileInputStream(file);

            xml2Java(is, srcPath, packageName, className, isPublicField);
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Generate java code by the specified xml.
     *
     * @param is
     * @param srcPath
     * @param packageName
     * @param className
     * @param isPublicField
     */
    public static void xml2Java(InputStream is, String srcPath, String packageName, String className, boolean isPublicField) {
        DocumentBuilder docBuilder = XMLUtil.createDOMParser(true, true);
        Writer writer = null;

        try {
            Document doc = docBuilder.parse(is);
            Node root = doc.getFirstChild();

            // TODO it's difficult to support duplicated property and may be misused.
            if (hasDuplicatedPropName(root)) {
                throw new AbacusException("The source xml document contains duplicated properties which has same node tag name in the same root.");
            }

            if (className == null) {
                className = StringUtil.capitalize(root.getNodeName());
            }

            String classFilePath = ClassUtil.makePackageFolder(srcPath, packageName);
            File classFile = new File(classFilePath + className + ".java");

            if (classFile.exists()) {
                classFile.delete();
            }

            classFile.createNewFile();
            writer = new OutputStreamWriter(new FileOutputStream(classFile), Charsets.UTF_8);
            writer.write("package " + packageName + ";" + IOUtil.LINE_SEPARATOR);

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write(IOUtil.LINE_SEPARATOR);

            Set<String> importType = getImportType(root);

            if (hasDuplicatedPropName(root)) {
                importType.add(List.class.getCanonicalName());
                importType.add(java.util.ArrayList.class.getCanonicalName());
                importType.add(java.util.Collections.class.getCanonicalName());
            }

            importType.add(Map.class.getCanonicalName());

            for (String clsName : importType) {
                writer.write("import " + clsName + ";" + IOUtil.LINE_SEPARATOR);
            }

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write("import " + Properties.class.getCanonicalName() + ";" + IOUtil.LINE_SEPARATOR);
            writer.write(IOUtil.LINE_SEPARATOR);

            xmlProperties2Java(root, writer, className, isPublicField, "", true);

            writer.flush();
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    private static void xmlProperties2Java(Node node, Writer writer, String className, boolean isPublicField, String spaces, boolean isRoot)
            throws IOException {
        if (className == null) {
            className = StringUtil.capitalize(node.getNodeName());
        }

        writer.write(IOUtil.LINE_SEPARATOR);

        if (isRoot) {
            writer.write(spaces + "/**" + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + " * Auto-generated by Abacus." + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + " */" + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + "public class " + className + " extends " + Properties.class.getSimpleName() + "<String, Object> {" + IOUtil.LINE_SEPARATOR);
        } else {
            writer.write(spaces + "public static class " + className + " extends " + Properties.class.getSimpleName() + "<String, Object> {"
                    + IOUtil.LINE_SEPARATOR);
        }

        NodeList childNodes = node.getChildNodes();

        if ((childNodes != null) && (childNodes.getLength() > 0)) {
            Set<String> duplicatedPropNameSet = getDuplicatedPropNameSet(node);
            Set<String> propNameSet = new HashSet<>();

            Node childNode = null;
            String propName = null;
            String typeName = null;

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = ClassUtil.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName)) {
                    continue;
                }

                propNameSet.add(propName);

                typeName = getTypeName(childNode, propName);

                writer.write(spaces + "    " + (isPublicField ? "public " : "private ") + typeName + " " + propName + ";" + IOUtil.LINE_SEPARATOR);

                if (duplicatedPropNameSet.contains(propName)) {
                    String listPropName = propName + "List";
                    String elementTypeName = N.typeOf(typeName).isPrimitiveType() ? ClassUtil.getSimpleClassName(Primitives.wrap(N.typeOf(typeName).clazz()))
                            : typeName;

                    writer.write(spaces + "    " + (isPublicField ? "public " : "private ") + "List<" + elementTypeName + "> " + listPropName
                            + " = Collections.synchronizedList(new ArrayList<" + elementTypeName + ">());" + IOUtil.LINE_SEPARATOR);
                }
            }

            propNameSet.clear();

            String methodSpace = spaces + "    ";
            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = ClassUtil.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName)) {
                    continue;
                }

                propNameSet.add(propName);

                writer.write(IOUtil.LINE_SEPARATOR);

                typeName = getTypeName(childNode, propName);

                writeMethod(writer, methodSpace, propName, typeName, duplicatedPropNameSet);
            }

            // disable put/put/all/set/remove method
            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "public " + className + " set(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "public Object put(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "public void putAll(Map<? extends String, ? extends Object> m) {" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "public Object remove(Object propName) {" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "public void clear() {" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            writer.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            propNameSet.clear();

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = ClassUtil.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName)) {
                    continue;
                }

                if (N.notNullOrEmpty(XMLUtil.getAttribute(childNode, TYPE))) {
                    continue;
                }

                propNameSet.add(propName);

                if (childNode.getChildNodes().getLength() > 1) {
                    xmlProperties2Java(childNode, writer, null, isPublicField, spaces + "    ", false);
                }
            }
        }

        writer.write(spaces + "}" + IOUtil.LINE_SEPARATOR);
    }

    private static Set<String> getImportType(Node node) {
        Set<String> result = new LinkedHashSet<>();
        NodeList childNodes = node.getChildNodes();

        if ((childNodes == null) || (childNodes.getLength() == 0)) {
            return result;
        }

        Node childNode = null;
        String attr = null;
        Type<Object> type = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);

            if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            attr = XMLUtil.getAttribute(childNode, TYPE);

            if (N.notNullOrEmpty(attr)) {
                type = N.typeOf(attr);
                if (type != null) {
                    Class<?> typeClass = type.clazz();
                    if (typeClass.getCanonicalName().startsWith("java.lang") || Primitives.isPrimitiveType(typeClass)
                            || (typeClass.isArray() && Primitives.isPrimitiveType(typeClass.getComponentType()))) {
                        // ignore
                    } else {
                        result.add(type.clazz().getCanonicalName());
                    }

                }
            }

            if (childNode.getChildNodes().getLength() > 1) {
                result.addAll(getImportType(childNode));
            }
        }

        return result;
    }

    private static void writeMethod(Writer writer, String spaces, String propName, String typeName, Set<String> duplicatedPropNameSet) throws IOException {
        String listPropName = propName + "List";
        String elementTypeName = N.typeOf(typeName).isPrimitiveType() ? ClassUtil.getSimpleClassName(Primitives.wrap(N.typeOf(typeName).clazz())) : typeName;

        writer.write(spaces + "public " + typeName + " get" + StringUtil.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR);
        writer.write(spaces + "    " + "return " + propName + ";" + IOUtil.LINE_SEPARATOR);
        writer.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        writer.write(IOUtil.LINE_SEPARATOR);

        writer.write(spaces + "public void set" + StringUtil.capitalize(propName) + "(" + typeName + " " + propName + ") {" + IOUtil.LINE_SEPARATOR);
        writer.write(spaces + "    " + "super.put(\"" + propName + "\", " + propName + ");" + IOUtil.LINE_SEPARATOR);
        writer.write(spaces + "    " + "this." + propName + " = " + propName + ";" + IOUtil.LINE_SEPARATOR);

        if (duplicatedPropNameSet.contains(propName)) {
            writer.write(spaces + "    " + "put(\"" + listPropName + "\", " + listPropName + ");" + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + "    " + "this." + listPropName + ".add(" + propName + ");" + IOUtil.LINE_SEPARATOR);
        }

        writer.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        writer.write(IOUtil.LINE_SEPARATOR);

        writer.write(spaces + "public void remove" + StringUtil.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR);
        writer.write(spaces + "    " + "super.remove(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR);
        writer.write(spaces + "    " + "this." + propName + " = " + N.typeOf(typeName).defaultValue() + ";" + IOUtil.LINE_SEPARATOR);

        // TODO it's difficult to support duplicated property and may be misused.
        //        if (duplicatedPropNameSet.contains(propName)) {
        //            writer.write(spaces + "    " + "remove(\"" + listPropName + "\", " + listPropName + ");" + N.LINE_SEPARATOR);
        //            writer.write(spaces + "    " + "this." + listPropName + ".remove(" + propName + ");" + N.LINE_SEPARATOR);
        //        }

        writer.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        if (duplicatedPropNameSet.contains(propName)) {
            writer.write(IOUtil.LINE_SEPARATOR);

            writer.write(spaces + "public List<" + elementTypeName + "> get" + StringUtil.capitalize(listPropName) + "() {" + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + "    " + "return " + listPropName + ";" + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

            writer.write(IOUtil.LINE_SEPARATOR);

            writer.write(spaces + "public void set" + StringUtil.capitalize(listPropName) + "(List<" + elementTypeName + "> " + listPropName + ") {"
                    + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + "    " + "super.put(\"" + listPropName + "\", " + listPropName + ");" + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + "    " + "this." + listPropName + " = " + listPropName + ";" + IOUtil.LINE_SEPARATOR);
            writer.write(spaces + "}" + IOUtil.LINE_SEPARATOR);
        }
    }

    private static String getTypeName(Node node, String propName) {
        String typeName = node.getChildNodes().getLength() > 1 ? StringUtil.capitalize(propName) : "String";
        String typeAttr = XMLUtil.getAttribute(node, TYPE);

        if (N.notNullOrEmpty(typeAttr)) {
            if (typeAttr.equals("Properties")) {
                typeName = "Properties<String, Object>";
            } else {
                Type<?> type = N.typeOf(typeAttr);
                if (type != null) {
                    typeName = type.clazz().getSimpleName();
                }
            }
        }

        return typeName;
    }

    private static boolean hasDuplicatedPropName(Node node) {
        NodeList childNodes = node.getChildNodes();

        if ((childNodes == null) || (childNodes.getLength() == 0)) {
            return false;
        }

        String propName = null;
        Node childNode = null;

        Set<String> propNameSet = new HashSet<>();

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);

            if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            propName = ClassUtil.formalizePropName(childNode.getNodeName());

            if (propNameSet.contains(propName)) {
                return true;
            } else if ((childNode.getChildNodes().getLength() > 1) && hasDuplicatedPropName(childNode)) {
                return true;
            } else {
                propNameSet.add(propName);
            }
        }

        return false;
    }

    private static Set<String> getDuplicatedPropNameSet(Node node) {
        NodeList childNodes = node.getChildNodes();
        if (childNodes == null || childNodes.getLength() == 0) {
            return new HashSet<>();
        }

        Set<String> propNameSet = new HashSet<>();
        Set<String> duplicatedPropNameSet = new HashSet<>();

        Node childNode = null;
        String propName = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);

            if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            propName = ClassUtil.formalizePropName(childNode.getNodeName());

            if (propNameSet.contains(propName)) {
                duplicatedPropNameSet.add(propName);
            } else {
                propNameSet.add(propName);
            }
        }

        return duplicatedPropNameSet;
    }

    static class ConfigEntity {
        private long id;
        private String name;
        private String content;
        private List<String> includedServers;
        private List<String> excludedServers;
        private Status status;
        private String description;
        private Timestamp lastUpdateTime;
        private Timestamp createTime;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<String> getIncludedServers() {
            return includedServers;
        }

        public void setIncludedServers(List<String> includedServers) {
            this.includedServers = includedServers;
        }

        public List<String> getExcludedServers() {
            return excludedServers;
        }

        public void setExcludedServers(List<String> excludedServers) {
            this.excludedServers = excludedServers;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Timestamp getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(Timestamp lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public Timestamp getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Timestamp createTime) {
            this.createTime = createTime;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (id ^ (id >>> 32));
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((content == null) ? 0 : content.hashCode());
            result = prime * result + ((includedServers == null) ? 0 : includedServers.hashCode());
            result = prime * result + ((excludedServers == null) ? 0 : excludedServers.hashCode());
            result = prime * result + ((status == null) ? 0 : status.hashCode());
            result = prime * result + ((description == null) ? 0 : description.hashCode());
            result = prime * result + ((lastUpdateTime == null) ? 0 : lastUpdateTime.hashCode());
            result = prime * result + ((createTime == null) ? 0 : createTime.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ConfigEntity) {
                ConfigEntity other = (ConfigEntity) obj;

                return N.equals(other.id, id) && N.equals(other.name, name) && N.equals(other.content, content)
                        && N.equals(other.includedServers, includedServers) && N.equals(other.excludedServers, excludedServers)
                        && N.equals(other.status, status) && N.equals(other.description, description) && N.equals(other.lastUpdateTime, lastUpdateTime)
                        && N.equals(other.createTime, createTime);

            }

            return false;
        }

        @Override
        public String toString() {
            return "{id=" + id + ", name=" + name + ", content=" + content + ", includedServers=" + includedServers + ", excludedServers=" + excludedServers
                    + ", status=" + status + ", description=" + description + ", lastUpdateTime=" + lastUpdateTime + ", createTime=" + createTime + "}";
        }

    }

    enum ResourceType {
        PROPERTIES, XML
    }

    static class Resource {
        private final Class<?> targetClass;
        private final File file;
        private final String filePath;
        private final SQLExecutor sqlExecutor;
        private final String sql;
        private final ResultExtractor<ConfigEntity> resultExtractor;
        private long lastLoadTime;
        private final ResourceType resourceType;

        public Resource(Class<?> cls, File file, ResourceType resourceType) {
            this.targetClass = cls;
            this.file = file;
            this.filePath = file.getPath();
            this.sqlExecutor = null;
            this.sql = null;
            this.resultExtractor = null;
            this.resourceType = resourceType;
        }

        public Resource(Class<?> cls, SQLExecutor sqlExecutor, String sql, ResultExtractor<ConfigEntity> resultExtractor, ResourceType resourceType) {
            this.targetClass = cls;
            this.file = null;
            this.filePath = null;
            this.sqlExecutor = sqlExecutor;
            this.sql = sql;
            this.resultExtractor = resultExtractor;
            this.resourceType = resourceType;
        }

        public long getLastLoadTime() {
            return lastLoadTime;
        }

        public void setLastLoadTime(long lastLoadTime) {
            this.lastLoadTime = lastLoadTime;
        }

        public File getFile() {
            return file;
        }

        public SQLExecutor getSqlExecutor() {
            return sqlExecutor;
        }

        public String getSql() {
            return sql;
        }

        public ResultExtractor<ConfigEntity> getResultSetExtractor() {
            return resultExtractor;
        }

        public ResourceType getType() {
            return resourceType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(targetClass);
            result = prime * result + N.hashCode(filePath);
            result = prime * result + N.hashCode(sql);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Resource) {
                Resource other = (Resource) obj;

                return N.equals(other.targetClass, targetClass) && N.equals(other.filePath, filePath) && N.equals(other.sql, sql);

            }

            return false;
        }

        @Override
        public String toString() {
            return "{file=" + file + ", sql=" + sql + "}";
        }
    }

}
