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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.Exclusion;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S1192")
public final class PropertiesUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    private static final String TYPE = "type";

    private static final XMLSerializationConfig xsc = XSC.create()
            .tagByPropertyName(true)
            .ignoreTypeInfo(true)
            .setDateTimeFormat(DateTimeFormat.ISO_8601_DATETIME)
            .setExclusion(Exclusion.NONE)
            .setIgnoredPropNames((Map<Class<?>, Set<String>>) null);

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

                    for (Map.Entry<Resource, Properties<?, ?>> entry : registeredAutoRefreshProperties.entrySet()) {
                        resource = entry.getKey();
                        properties = entry.getValue();

                        file = resource.getFile();

                        if ((file != null) && (file.lastModified() > resource.getLastLoadTime())) {
                            long lastLoadTime = file.lastModified();
                            InputStream is = null;

                            if (logger.isWarnEnabled()) {
                                logger.warn("Start to refresh properties with the updated file: " + file.getAbsolutePath());
                                logger.warn("[PROPERTIES]" + properties);
                            }

                            try {
                                is = IOUtil.newFileInputStream(resource.getFile());

                                if (resource.getType() == ResourceType.PROPERTIES) {
                                    load((Properties<String, String>) properties, is);
                                } else {
                                    loadFromXml(properties, is, properties.getClass());
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
                    }
                }
            }
        };

        scheduledExecutor.scheduleWithFixedDelay(refreshTask, 1000, 1000, TimeUnit.MICROSECONDS);
    }

    private PropertiesUtil() {
        // singleton.
    }

    /**
     *
     * @param configFileName
     * @return
     */
    public static File findFile(String configFileName) {
        return Configuration.findFile(configFileName);
    }

    /**
     *
     * @param configDir
     * @return
     */
    public static File findDir(String configDir) {
        return Configuration.findDir(configDir);
    }

    /**
     *
     * @param source
     * @return
     */
    public static Properties<String, String> load(File source) {
        return load(source, false);
    }

    /**
     *
     * @param source
     * @param autoRefresh
     * @return
     */
    public static Properties<String, String> load(File source, boolean autoRefresh) {
        Properties<String, String> properties = null;

        InputStream is = null;
        try {
            is = IOUtil.newFileInputStream(source);

            if (autoRefresh) {
                Resource resource = new Resource(Properties.class, source, ResourceType.PROPERTIES);
                resource.setLastLoadTime(source.lastModified());

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
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     *
     * @param source
     * @return
     */
    public static Properties<String, String> load(InputStream source) {
        return load(null, source);
    }

    /**
     *
     * @param targetProperties
     * @param source
     * @return
     */
    private static Properties<String, String> load(Properties<String, String> targetProperties, InputStream source) {
        java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return create(targetProperties, tmp);
    }

    /**
     *
     * @param source
     * @return
     */
    public static Properties<String, String> load(Reader source) {
        java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(source);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return create(null, tmp);
    }

    /**
     *
     * @param targetProperties
     * @param newProperties
     * @return
     */
    private static Properties<String, String> create(Properties<String, String> targetProperties, java.util.Properties newProperties) {
        Properties<String, String> properties = null;
        if (targetProperties == null) {
            properties = new Properties<>();
        } else {
            properties = targetProperties;
        }

        Set<String> newKeySet = N.newHashSet();
        Enumeration<?> it = newProperties.propertyNames();
        String propName = null;

        while (it.hasMoreElements()) {
            propName = it.nextElement().toString();
            properties.set(propName, newProperties.getProperty(propName));
            newKeySet.add(propName);
        }

        if (targetProperties != null) {
            Set<String> oldKeySet = N.newHashSet(properties.keySet());

            for (String key : oldKeySet) {
                if (!newKeySet.contains(key)) {
                    properties.remove(key);
                }
            }
        }

        return properties;
    }

    /**
     * Load from XML.
     *
     * @param source
     * @return
     */
    public static Properties<String, Object> loadFromXml(File source) {
        return loadFromXml(source, false);
    }

    /**
     * Load from XML.
     *
     * @param source
     * @param autoRefresh
     * @return
     */
    public static Properties<String, Object> loadFromXml(File source, boolean autoRefresh) {
        return loadFromXml(source, autoRefresh, Properties.class);
    }

    /**
     * Load from XML.
     *
     * @param source
     * @return
     */
    public static Properties<String, Object> loadFromXml(InputStream source) {
        return loadFromXml(source, Properties.class);
    }

    /**
     * Load from XML.
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    public static <T extends Properties<String, Object>> T loadFromXml(File source, Class<? extends T> targetClass) {
        return loadFromXml(source, false, targetClass);
    }

    /**
     * Load from XML.
     *
     * @param <T>
     * @param source
     * @param autoRefresh
     * @param targetClass
     * @return
     */
    public static <T extends Properties<String, Object>> T loadFromXml(File source, boolean autoRefresh, Class<? extends T> targetClass) {
        T properties = null;
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            if (autoRefresh) {
                Resource resource = new Resource(targetClass, source, ResourceType.XML);
                resource.setLastLoadTime(source.lastModified());

                synchronized (registeredAutoRefreshProperties) {
                    properties = (T) registeredAutoRefreshProperties.get(resource);

                    if (properties == null) {
                        properties = loadFromXml(is, targetClass);

                        registeredAutoRefreshProperties.put(resource, properties);
                    }
                }
            } else {
                properties = loadFromXml(is, targetClass);
            }

            return properties;
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Load from XML.
     *
     * @param <T>
     * @param source
     * @param targetClass
     * @return
     */
    public static <T extends Properties<String, Object>> T loadFromXml(InputStream source, Class<? extends T> targetClass) {
        return loadFromXml(null, source, targetClass);
    }

    /**
     * Load from XML.
     *
     * @param <T>
     * @param targetProperties
     * @param source
     * @param targetClass
     * @return
     */
    private static <T extends Properties<String, Object>> T loadFromXml(Object targetProperties, InputStream source, Class<? extends T> targetClass) {
        DocumentBuilder docBuilder = XMLUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(source);
        } catch (SAXException e) {
            throw new ParseException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Node node = doc.getFirstChild();
        return loadFromXml(targetProperties, targetClass, node, null, true);
    }

    /**
     * Load from XML.
     *
     * @param <T>
     * @param targetProperties
     * @param inputClass
     * @param source
     * @param propSetMethod
     * @param isFirstCall
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Properties<String, Object>> T loadFromXml(Object targetProperties, Class<T> inputClass, Node source, Method propSetMethod,
            boolean isFirstCall) {

        // TODO it's difficult to support duplicated property and may be misused.
        if (hasDuplicatedPropName(source)) {
            throw new RuntimeException("The source xml document contains duplicated properties which has same node tag name in the same root.");
        }

        Class<?> targetClass = null;

        if (isFirstCall) {
            targetClass = targetProperties == null ? (inputClass == null ? Properties.class : inputClass) : targetProperties.getClass();
        } else {
            targetClass = (propSetMethod == null) ? Properties.class : propSetMethod.getParameterTypes()[0];
        }

        T properties = (T) (targetProperties == null ? N.newInstance(targetClass) : targetProperties);

        NodeList propNodes = source.getChildNodes();
        int propNodeLength = (propNodes == null) ? 0 : propNodes.getLength();
        Set<String> newKeySet = N.newHashSet();
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
                if (Strings.isEmpty(typeAttr)) {
                    propValue = Configuration.getTextContent(propNode);
                } else {
                    propValue = N.typeOf(typeAttr).valueOf(Configuration.getTextContent(propNode));
                }
            } else {
                // TODO it's difficult to support duplicated property and may be misused.
                // How to get target property value for auto-refresh if it's list of Properties or entities.
                Object targetPropValue = properties.get(propName);
                Class<T> propClass = (Class<T>) (propSetMethod == null ? Properties.class : propSetMethod.getParameterTypes()[0]);
                propValue = loadFromXml(targetPropValue, propClass, propNode, propSetMethod, false);
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

                    if (Strings.isEmpty(propValue.toString()) && Properties.class.isAssignableFrom(parameterType)) {
                        propValue = N.newInstance(parameterType);
                    }

                    ClassUtil.setPropValue(properties, propSetMethod, propValue);
                }
            }
        }

        if (targetProperties != null) {
            Set<String> oldKeySet = N.newHashSet(properties.keySet());
            Method removeMethod = null;
            for (String key : oldKeySet) {
                if (!newKeySet.contains(key)) {
                    removeMethod = ClassUtil.getDeclaredMethod(properties.getClass(), "remove" + Strings.capitalize(key));

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

    /**
     *
     * @param properties
     * @param comments
     * @param output
     */
    public static void store(Properties<?, ?> properties, String comments, File output) {
        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            store(properties, comments, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     *
     * @param properties
     * @param comments
     * @param output
     */
    public static void store(Properties<?, ?> properties, String comments, OutputStream output) {
        BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            store(properties, comments, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param properties
     * @param comments
     * @param output
     */
    public static void store(Properties<?, ?> properties, String comments, Writer output) {
        final java.util.Properties tmp = new java.util.Properties();

        tmp.putAll(properties);

        try {
            tmp.store(output, comments);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Store to XML.
     *
     * @param properties
     * @param rootElementName
     * @param ignoreTypeInfo
     * @param output
     */
    public static void storeToXml(Properties<?, ?> properties, String rootElementName, boolean ignoreTypeInfo, File output) {
        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            storeToXml(properties, rootElementName, ignoreTypeInfo, os);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     * Store to XML.
     *
     * @param properties
     * @param rootElementName
     * @param ignoreTypeInfo
     * @param output
     */
    public static void storeToXml(Properties<?, ?> properties, String rootElementName, boolean ignoreTypeInfo, OutputStream output) {
        try {
            storeToXml(properties, rootElementName, ignoreTypeInfo, true, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Store to XML.
     *
     * @param properties
     * @param rootElementName
     * @param ignoreTypeInfo
     * @param isFirstCall
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void storeToXml(Properties<?, ?> properties, String rootElementName, boolean ignoreTypeInfo, boolean isFirstCall, OutputStream output)
            throws IOException {
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(output);

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
            for (Object propName : properties.keySet()) { //NOSONAR
                listPropName = propName + "List";
                elementPropName = propName.toString();

                if (elementPropName.endsWith("List")) {
                    elementPropName = elementPropName.substring(0, elementPropName.length() - 4);
                }

                propValue = properties.get(propName);
                listPropvalue = properties.get(listPropName);

                if ((propValue == null) || (listPropvalue instanceof List && ((List<?>) listPropvalue).size() > 0)) {
                    continue;
                }

                if (propValue instanceof List && properties.containsKey(elementPropName)) {
                    for (Object e : ((List<?>) propValue)) {
                        if (e == null) {
                            // continue;
                        } else if (e instanceof Properties) {
                            bw.flush();

                            storeToXml((Properties<?, ?>) e, elementPropName, ignoreTypeInfo, false, output);
                        } else {
                            type = N.typeOf(e.getClass());

                            if (ignoreTypeInfo) {
                                bw.write("<" + elementPropName + ">");
                            } else {
                                if (ClassUtil.isPrimitiveWrapper(type.clazz())) {
                                    bw.write("<" + elementPropName + " type=\"" + ClassUtil.getSimpleClassName(ClassUtil.unwrap(type.clazz())) + "\">");
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

                    storeToXml((Properties<?, ?>) propValue, propName.toString(), ignoreTypeInfo, false, output);
                } else {
                    type = N.typeOf(propValue.getClass());

                    if (ignoreTypeInfo) {
                        bw.write("<" + propName + ">");
                    } else {
                        if (ClassUtil.isPrimitiveWrapper(type.clazz())) {
                            bw.write("<" + propName + " type=\"" + ClassUtil.getSimpleClassName(ClassUtil.unwrap(type.clazz())) + "\">");
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
     * @param source
     * @param srcPath
     * @param packageName
     * @param className
     * @param isPublicField
     */
    public static void xml2Java(File source, String srcPath, String packageName, String className, boolean isPublicField) {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            xml2Java(is, srcPath, packageName, className, isPublicField);
        } finally {
            IOUtil.close(is);
        }
    }

    /**
     * Generate java code by the specified xml.
     *
     * @param source
     * @param srcPath
     * @param packageName
     * @param className
     * @param isPublicField
     */
    public static void xml2Java(InputStream source, String srcPath, String packageName, String className, boolean isPublicField) {
        DocumentBuilder docBuilder = XMLUtil.createDOMParser(true, true);
        Writer writer = null;

        try { //NOSONAR
            Document doc = docBuilder.parse(source);
            Node root = doc.getFirstChild();

            // TODO it's difficult to support duplicated property and may be misused.
            if (hasDuplicatedPropName(root)) {
                throw new RuntimeException("The source xml document contains duplicated properties which has same node tag name in the same root.");
            }

            if (className == null) {
                className = Strings.capitalize(root.getNodeName());
            }

            String classFilePath = ClassUtil.makePackageFolder(srcPath, packageName);
            File classFile = new File(classFilePath + className + ".java");

            IOUtil.deleteIfExists(classFile);

            IOUtil.createNewFileIfNotExists(classFile);

            writer = IOUtil.newOutputStreamWriter(IOUtil.newFileOutputStream(classFile), IOUtil.DEFAULT_CHARSET);
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

            xmlProperties2Java(root, className, isPublicField, "", true, writer);

            writer.flush();
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Xml properties 2 java.
     *
     * @param source
     * @param className
     * @param isPublicField
     * @param spaces
     * @param isRoot
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void xmlProperties2Java(Node source, String className, boolean isPublicField, String spaces, boolean isRoot, Writer output)
            throws IOException {
        if (className == null) {
            className = Strings.capitalize(source.getNodeName());
        }

        output.write(IOUtil.LINE_SEPARATOR);

        if (isRoot) {
            output.write(spaces + "/**" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + " * Auto-generated by Abacus." + IOUtil.LINE_SEPARATOR);
            output.write(spaces + " */" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "public class " + className + " extends " + Properties.class.getSimpleName() + "<String, Object> {" + IOUtil.LINE_SEPARATOR);
        } else {
            output.write(spaces + "public static class " + className + " extends " + Properties.class.getSimpleName() + "<String, Object> {"
                    + IOUtil.LINE_SEPARATOR);
        }

        NodeList childNodes = source.getChildNodes();

        if ((childNodes != null) && (childNodes.getLength() > 0)) {
            Set<String> duplicatedPropNameSet = getDuplicatedPropNameSet(source);
            Set<String> propNameSet = N.newHashSet();

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

                output.write(spaces + "    " + (isPublicField ? "public " : "private ") + typeName + " " + propName + ";" + IOUtil.LINE_SEPARATOR);

                if (duplicatedPropNameSet.contains(propName)) {
                    String listPropName = propName + "List";
                    String elementTypeName = N.typeOf(typeName).isPrimitiveType() ? ClassUtil.getSimpleClassName(ClassUtil.wrap(N.typeOf(typeName).clazz()))
                            : typeName;

                    output.write(spaces + "    " + (isPublicField ? "public " : "private ") + "List<" + elementTypeName + "> " + listPropName
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

                output.write(IOUtil.LINE_SEPARATOR);

                typeName = getTypeName(childNode, propName);

                writeMethod(methodSpace, propName, typeName, duplicatedPropNameSet, output);
            }

            // disable put/put/all/set/remove method
            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public " + className + " set(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public Object put(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public void putAll(Map<? extends String, ? extends Object> m) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public Object remove(Object propName) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public void clear() {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "throw new UnsupportedOperationException();" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            propNameSet.clear();

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = ClassUtil.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName) || Strings.isNotEmpty(XMLUtil.getAttribute(childNode, TYPE))) {
                    continue;
                }

                propNameSet.add(propName);

                if (childNode.getChildNodes().getLength() > 1) {
                    xmlProperties2Java(childNode, null, isPublicField, spaces + "    ", false, output);
                }
            }
        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);
    }

    /**
     * Gets the import type.
     *
     * @param node
     * @return
     */
    private static Set<String> getImportType(Node node) {
        Set<String> result = N.newLinkedHashSet();
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

            if (Strings.isNotEmpty(attr)) {
                type = N.typeOf(attr);
                if (type != null) {
                    Class<?> typeClass = type.clazz();
                    if (typeClass.getCanonicalName().startsWith("java.lang") || ClassUtil.isPrimitiveType(typeClass)
                            || (typeClass.isArray() && ClassUtil.isPrimitiveType(typeClass.getComponentType()))) {
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

    /**
     *
     * @param spaces
     * @param propName
     * @param typeName
     * @param duplicatedPropNameSet
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writeMethod(String spaces, String propName, String typeName, Set<String> duplicatedPropNameSet, Writer output) throws IOException {
        String listPropName = propName + "List";
        String elementTypeName = N.typeOf(typeName).isPrimitiveType() ? ClassUtil.getSimpleClassName(ClassUtil.wrap(N.typeOf(typeName).clazz())) : typeName;

        output.write(spaces + "public " + typeName + " get" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "return " + propName + ";" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        output.write(IOUtil.LINE_SEPARATOR);

        output.write(spaces + "public void set" + Strings.capitalize(propName) + "(" + typeName + " " + propName + ") {" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "super.put(\"" + propName + "\", " + propName + ");" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "this." + propName + " = " + propName + ";" + IOUtil.LINE_SEPARATOR);

        if (duplicatedPropNameSet.contains(propName)) {
            output.write(spaces + "    " + "put(\"" + listPropName + "\", " + listPropName + ");" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "    " + "this." + listPropName + ".add(" + propName + ");" + IOUtil.LINE_SEPARATOR);
        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        output.write(IOUtil.LINE_SEPARATOR);

        output.write(spaces + "public void remove" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "super.remove(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "this." + propName + " = " + N.typeOf(typeName).defaultValue() + ";" + IOUtil.LINE_SEPARATOR);

        // TODO it's difficult to support duplicated property and may be misused.
        //        if (duplicatedPropNameSet.contains(propName)) {
        //            writer.write(spaces + "    " + "remove(\"" + listPropName + "\", " + listPropName + ");" + N.LINE_SEPARATOR);
        //            writer.write(spaces + "    " + "this." + listPropName + ".remove(" + propName + ");" + N.LINE_SEPARATOR);
        //        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        if (duplicatedPropNameSet.contains(propName)) {
            output.write(IOUtil.LINE_SEPARATOR);

            output.write(spaces + "public List<" + elementTypeName + "> get" + Strings.capitalize(listPropName) + "() {" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "    " + "return " + listPropName + ";" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);

            output.write(spaces + "public void set" + Strings.capitalize(listPropName) + "(List<" + elementTypeName + "> " + listPropName + ") {"
                    + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "    " + "super.put(\"" + listPropName + "\", " + listPropName + ");" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "    " + "this." + listPropName + " = " + listPropName + ";" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);
        }
    }

    /**
     * Gets the type name.
     *
     * @param node
     * @param propName
     * @return
     */
    private static String getTypeName(Node node, String propName) {
        String typeName = node.getChildNodes().getLength() > 1 ? Strings.capitalize(propName) : "String";
        String typeAttr = XMLUtil.getAttribute(node, TYPE);

        if (Strings.isNotEmpty(typeAttr)) {
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

    /**
     * Checks for duplicated prop name.
     *
     * @param node
     * @return
     */
    private static boolean hasDuplicatedPropName(Node node) {
        NodeList childNodes = node.getChildNodes();

        if ((childNodes == null) || (childNodes.getLength() == 0)) {
            return false;
        }

        String propName = null;
        Node childNode = null;

        Set<String> propNameSet = N.newHashSet();

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

    /**
     * Gets the duplicated prop name set.
     *
     * @param node
     * @return
     */
    private static Set<String> getDuplicatedPropNameSet(Node node) {
        NodeList childNodes = node.getChildNodes();
        if (childNodes == null || childNodes.getLength() == 0) {
            return N.newHashSet();
        }

        Set<String> propNameSet = N.newHashSet();
        Set<String> duplicatedPropNameSet = N.newHashSet();

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

    /**
     * The Class ConfigBean.
     */
    static final class ConfigBean { // NOSONAR

        /** The id. */
        private long id;

        /** The name. */
        private String name;

        /** The content. */
        private String content;

        /** The included servers. */
        private List<String> includedServers;

        /** The excluded servers. */
        private List<String> excludedServers;

        /** The status. */
        private Status status;

        /** The description. */
        private String description;

        /** The last update time. */
        private Timestamp lastUpdateTime;

        /** The create time. */
        private Timestamp createTime;

        /**
         * Gets the id.
         *
         * @return
         */
        public long getId() {
            return id;
        }

        /**
         * Sets the id.
         *
         * @param id the new id
         */
        public void setId(long id) {
            this.id = id;
        }

        /**
         * Gets the name.
         *
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         *
         * @param name the new name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the content.
         *
         * @return
         */
        public String getContent() {
            return content;
        }

        /**
         * Sets the content.
         *
         * @param content the new content
         */
        public void setContent(String content) {
            this.content = content;
        }

        /**
         * Gets the included servers.
         *
         * @return
         */
        public List<String> getIncludedServers() {
            return includedServers;
        }

        /**
         * Sets the included servers.
         *
         * @param includedServers the new included servers
         */
        public void setIncludedServers(List<String> includedServers) {
            this.includedServers = includedServers;
        }

        /**
         * Gets the excluded servers.
         *
         * @return
         */
        public List<String> getExcludedServers() {
            return excludedServers;
        }

        /**
         * Sets the excluded servers.
         *
         * @param excludedServers the new excluded servers
         */
        public void setExcludedServers(List<String> excludedServers) {
            this.excludedServers = excludedServers;
        }

        /**
         * Gets the status.
         *
         * @return
         */
        public Status getStatus() {
            return status;
        }

        /**
         * Sets the status.
         *
         * @param status the new status
         */
        public void setStatus(Status status) {
            this.status = status;
        }

        /**
         * Gets the description.
         *
         * @return
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the description.
         *
         * @param description the new description
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * Gets the last update time.
         *
         * @return
         */
        public Timestamp getLastUpdateTime() {
            return lastUpdateTime;
        }

        /**
         * Sets the last update time.
         *
         * @param lastUpdateTime the new last update time
         */
        public void setLastUpdateTime(Timestamp lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        /**
         * Gets the creates the time.
         *
         * @return
         */
        public Timestamp getCreateTime() {
            return createTime;
        }

        /**
         * Sets the creates the time.
         *
         * @param createTime the new creates the time
         */
        public void setCreateTime(Timestamp createTime) {
            this.createTime = createTime;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, name, content, includedServers, excludedServers, status, description, lastUpdateTime, createTime);
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ConfigBean other) {
                return N.equals(other.id, id) && N.equals(other.name, name) && N.equals(other.content, content)
                        && N.equals(other.includedServers, includedServers) && N.equals(other.excludedServers, excludedServers)
                        && N.equals(other.status, status) && N.equals(other.description, description) && N.equals(other.lastUpdateTime, lastUpdateTime)
                        && N.equals(other.createTime, createTime);

            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "{id=" + id + ", name=" + name + ", content=" + content + ", includedServers=" + includedServers + ", excludedServers=" + excludedServers
                    + ", status=" + status + ", description=" + description + ", lastUpdateTime=" + lastUpdateTime + ", createTime=" + createTime + "}";
        }

    }

    /**
     * The Enum ResourceType.
     */
    enum ResourceType {

        /** The properties. */
        PROPERTIES,
        /** The xml. */
        XML
    }

    /**
     * The Class Resource.
     */
    static class Resource {

        /** The target class. */
        private final Class<?> targetClass;

        /** The file. */
        private final File file;

        /** The file path. */
        private final String filePath;

        /** The last load time. */
        private long lastLoadTime;

        /** The resource type. */
        private final ResourceType resourceType;

        /**
         * Instantiates a new resource.
         *
         * @param cls
         * @param file
         * @param resourceType
         */
        public Resource(Class<?> cls, File file, ResourceType resourceType) {
            this.targetClass = cls;
            this.file = file;
            this.filePath = file.getPath();
            this.resourceType = resourceType;
        }

        /**
         * Gets the last load time.
         *
         * @return
         */
        public long getLastLoadTime() {
            return lastLoadTime;
        }

        /**
         * Sets the last load time.
         *
         * @param lastLoadTime the new last load time
         */
        public void setLastLoadTime(long lastLoadTime) {
            this.lastLoadTime = lastLoadTime;
        }

        /**
         * Gets the file.
         *
         * @return
         */
        public File getFile() {
            return file;
        }

        /**
         * Gets the type.
         *
         * @return
         */
        public ResourceType getType() {
            return resourceType;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + N.hashCode(targetClass);
            return prime * result + N.hashCode(filePath);
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Resource other) {
                return N.equals(other.targetClass, targetClass) && N.equals(other.filePath, filePath);

            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "{file=" + file + "}";
        }
    }
}
