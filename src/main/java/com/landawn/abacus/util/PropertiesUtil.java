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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashMap;
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
import org.xml.sax.InputSource;
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
 * @see Configuration
 */
@SuppressWarnings("java:S1192")
public final class PropertiesUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    private static final String TYPE = "type";

    private static final XMLSerializationConfig xsc = XSC.create()
            .tagByPropertyName(true)
            .ignoreTypeInfo(true)
            .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)
            .setExclusion(Exclusion.NONE)
            .setIgnoredPropNames((Map<Class<?>, Set<String>>) null);

    private static final ScheduledExecutorService scheduledExecutor;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        scheduledExecutor = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    private static final Map<Resource, Properties<String, ?>> registeredAutoRefreshProperties = new ConcurrentHashMap<>(256);

    static {
        final Runnable refreshTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (registeredAutoRefreshProperties) {
                    Properties<String, ?> properties = null;
                    Resource resource = null;
                    File file = null;

                    for (final Map.Entry<Resource, Properties<String, ?>> entry : registeredAutoRefreshProperties.entrySet()) {
                        resource = entry.getKey();
                        properties = entry.getValue();

                        file = resource.getFile();

                        if ((file != null) && (file.lastModified() > resource.getLastLoadTime())) {
                            final long lastLoadTime = file.lastModified();
                            Reader reader = null;

                            if (logger.isWarnEnabled()) {
                                logger.warn("Start to refresh properties with the updated file: " + file.getAbsolutePath());
                                logger.warn("[PROPERTIES]" + properties);
                            }

                            try {
                                reader = IOUtil.newFileReader(resource.getFile());

                                if (resource.getType() == ResourceType.PROPERTIES) {
                                    merge(load(reader), (Properties<String, String>) properties);
                                } else {
                                    merge(loadFromXml(reader, (Class<Properties<String, Object>>) properties.getClass()),
                                            (Properties<String, Object>) properties);
                                }

                                resource.setLastLoadTime(lastLoadTime);
                            } catch (final Exception e) {
                                logger.error("Failed to refresh properties: " + properties, e);
                            } finally {
                                IOUtil.close(reader);
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
     * Finds the file with the specified configuration file name.
     *
     * @param configFileName the name of the configuration file to find
     * @return the File object representing the found file
     */
    public static File findFile(final String configFileName) {
        return Configuration.findFile(configFileName);
    }

    /**
     * Finds the directory with the specified configuration directory name.
     *
     * @param configDir the name of the configuration directory to find
     * @return the File object representing the found directory
     */
    public static File findDir(final String configDir) {
        return Configuration.findDir(configDir);
    }

    /**
     * Loads properties from the specified file.
     *
     * @param source The file from which to load the properties.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, String> load(final File source) {
        return load(source, false);
    }

    /**
     * Loads properties from the specified file with an option for auto-refresh.
     *
     * @param source The file from which to load the properties.
     * @param autoRefresh If {@code true}, the properties will be automatically refreshed when the file is modified.
     *                    There is a background thread to check the file last modification time every second.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, String> load(final File source, final boolean autoRefresh) {
        Properties<String, String> properties = null;

        Reader reader = null;
        try {
            reader = IOUtil.newFileReader(source);

            if (autoRefresh) {
                final Resource resource = new Resource(Properties.class, source, ResourceType.PROPERTIES);
                resource.setLastLoadTime(source.lastModified());

                synchronized (registeredAutoRefreshProperties) {
                    properties = (Properties<String, String>) registeredAutoRefreshProperties.get(resource);

                    if (properties == null) {
                        properties = load(reader);
                        registeredAutoRefreshProperties.put(resource, properties);
                    }
                }
            } else {
                properties = load(reader);
            }

            return properties;
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Loads properties from the specified InputStream.
     *
     * @param source The InputStream from which to load the properties.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, String> load(final InputStream source) {
        final java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(source);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Properties<String, String> result = new Properties<>();

        merge(tmp, result);

        return result;
    }

    /**
     * Loads properties from the specified Reader.
     *
     * @param source The Reader from which to load the properties.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, String> load(final Reader source) {
        final java.util.Properties tmp = new java.util.Properties();

        try {
            tmp.load(source);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Properties<String, String> result = new Properties<>();

        merge(tmp, result);

        return result;
    }

    /**
     * Merges the source properties into the target properties.
     *
     * @param srcProperties The source properties to merge from.
     * @param targetProperties The target properties to merge into. If {@code null}, a new Properties object will be created.
     * @return A Properties object containing the merged properties.
     */
    @SuppressWarnings("rawtypes")
    private static void merge(final java.util.Properties srcProperties, final Properties<String, String> targetProperties) {
        //    final Map<String, String> valueMap = new LinkedHashMap<>(targetProperties.values);
        //
        //    final Set<String> oldKeySet = N.newHashSet(valueMap.keySet());
        //    final Set<String> srcPropNameSet = srcProperties.stringPropertyNames();
        //    final Set<String> newKeySet = N.newHashSet();
        //
        //    for (final String srcPropName : srcPropNameSet) {
        //        valueMap.put(srcPropName, srcProperties.getProperty(srcPropName));
        //        newKeySet.add(srcPropName);
        //    }
        //
        //    for (final String key : oldKeySet) {
        //        if (!newKeySet.contains(key)) {
        //            valueMap.remove(key);
        //        }
        //    }
        //
        //    targetProperties.reset(valueMap);

        targetProperties.reset(new LinkedHashMap<>((Map) srcProperties));
    }

    private static <K, V> void merge(final Properties<? extends K, ? extends V> srcProperties, final Properties<K, V> targetProperties) {
        //    final Map<K, V> valueMap = new LinkedHashMap<>(targetProperties.values);
        //
        //    final Set<K> oldKeySet = N.newHashSet(valueMap.keySet());
        //    final Set<? extends K> srcPropNameSet = srcProperties.keySet();
        //    final Set<K> newKeySet = N.newHashSet();
        //    V srcPropValue = null;
        //    for (final K srcPropName : srcPropNameSet) {
        //        srcPropValue = srcProperties.get(srcPropName);
        //
        //        //    targetPropValue = valueMap.get(srcPropName);
        //        //
        //        //    if (srcPropValue instanceof Properties && targetPropValue instanceof Properties) {
        //        //        merge((Properties) srcPropValue, (Properties) targetPropValue);
        //        //
        //        //        valueMap.put(srcPropName, targetPropValue);
        //        //    } else {
        //        //        valueMap.put(srcPropName, srcPropValue);
        //        //    }
        //
        //        valueMap.put(srcPropName, srcPropValue);
        //
        //        newKeySet.add(srcPropName);
        //    }
        //
        //    for (final K key : oldKeySet) {
        //        if (!newKeySet.contains(key)) {
        //            valueMap.remove(key);
        //        }
        //    }
        //
        //    targetProperties.reset(valueMap);

        targetProperties.reset(new LinkedHashMap<>(srcProperties.values));
    }

    /**
     * Loads properties from the specified XML file.
     *
     * @param source The XML file from which to load the properties.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, Object> loadFromXml(final File source) {
        return loadFromXml(source, false);
    }

    /**
     * Loads properties from the specified XML file with an option for auto-refresh.
     *
     * @param source The XML file from which to load the properties.
     * @param autoRefresh If {@code true}, the properties will be automatically refreshed when the file is modified.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, Object> loadFromXml(final File source, final boolean autoRefresh) {
        return loadFromXml(source, autoRefresh, Properties.class);
    }

    /**
     * Loads properties from the specified XML InputStream.
     *
     * @param source The InputStream from which to load the properties.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, Object> loadFromXml(final InputStream source) {
        return loadFromXml(source, Properties.class);
    }

    /**
     * Loads properties from the specified XML Reader.
     *
     * @param source The Reader from which to load the properties.
     * @return A Properties object containing the loaded properties.
     */
    public static Properties<String, Object> loadFromXml(final Reader source) {
        return loadFromXml(source, Properties.class);
    }

    /**
     * Loads properties from the specified XML file into the target properties class.
     *
     * @param <T> The type of the target properties class.
     * @param source The XML file from which to load the properties.
     * @param targetClass The class of the target properties.
     * @return An instance of the target properties class containing the loaded properties.
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final File source, final Class<? extends T> targetClass) {
        return loadFromXml(source, false, targetClass);
    }

    /**
     * Loads properties from the specified XML file into the target properties class with an option for auto-refresh.
     *
     * @param <T> The type of the target properties class.
     * @param source The XML file from which to load the properties.
     * @param autoRefresh If {@code true}, the properties will be automatically refreshed when the file is modified.
     *                    There is a background thread to check the file last modification time every second.
     * @param targetClass The class of the target properties.
     * @return An instance of the target properties class containing the loaded properties.
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final File source, final boolean autoRefresh, final Class<? extends T> targetClass) {
        T properties = null;
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            if (autoRefresh) {
                final Resource resource = new Resource(targetClass, source, ResourceType.XML);
                resource.setLastLoadTime(source.lastModified());

                synchronized (registeredAutoRefreshProperties) {
                    properties = (T) registeredAutoRefreshProperties.get(resource);

                    if (properties == null) {
                        properties = loadFromXml(reader, targetClass);

                        registeredAutoRefreshProperties.put(resource, properties);
                    }
                }
            } else {
                properties = loadFromXml(reader, targetClass);
            }

            return properties;
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Loads properties from the specified XML InputStream into the target properties class.
     *
     * @param <T> The type of the target properties class.
     * @param source The InputStream from which to load the properties.
     * @param targetClass The class of the target properties.
     * @return An instance of the target properties class containing the loaded properties.
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final InputStream source, final Class<? extends T> targetClass) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(source);
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Node node = doc.getFirstChild();

        return loadFromXml(node, null, true, null, targetClass);
    }

    /**
     * Loads properties from the specified XML Reader into the target properties class.
     *
     * @param <T> The type of the target properties class.
     * @param source The Reader from which to load the properties.
     * @param targetClass The class of the target properties.
     * @return An instance of the target properties class containing the loaded properties.
     */
    public static <T extends Properties<String, Object>> T loadFromXml(final Reader source, final Class<? extends T> targetClass) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);

        Document doc;
        try {
            doc = docBuilder.parse(new InputSource(source));
        } catch (final SAXException e) {
            throw new ParseException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final Node node = doc.getFirstChild();

        return loadFromXml(node, null, true, null, targetClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Properties<String, Object>> T loadFromXml(final Node source, Method propSetMethod, final boolean isFirstCall, final T output,
            final Class<T> inputClass) {

        // TODO it's difficult to support duplicated property and may be misused.
        if (hasDuplicatedPropName(source)) {
            throw new RuntimeException("The source xml document contains duplicated properties which has same node tag name in the same root.");
        }

        Class<?> targetClass = null;

        if (isFirstCall) {
            targetClass = output == null ? (inputClass == null ? Properties.class : inputClass) : output.getClass();
        } else {
            targetClass = (propSetMethod == null) ? Properties.class : propSetMethod.getParameterTypes()[0];
        }

        final T properties = (T) (output == null ? N.newInstance(targetClass) : output);

        final NodeList propNodes = source.getChildNodes();
        @SuppressWarnings("ConstantValue")
        final int propNodeLength = (propNodes == null) ? 0 : propNodes.getLength();
        final Set<String> newKeySet = N.newHashSet();
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

            typeAttr = XmlUtil.getAttribute(propNode, TYPE);
            propSetMethod = ClassUtil.getPropSetMethod(targetClass, propName);

            if (XmlUtil.isTextElement(propNode)) {
                if (Strings.isEmpty(typeAttr)) {
                    propValue = Strings.strip(XmlUtil.getTextContent(propNode));
                } else {
                    propValue = N.typeOf(typeAttr).valueOf(Strings.strip(XmlUtil.getTextContent(propNode)));
                }
            } else {
                // TODO it's difficult to support duplicated property and may be misused.
                // How to get target property value for auto-refresh if it's list of Properties or entities.
                final T targetPropValue = (T) properties.get(propName);
                final Class<T> propClass = (Class<T>) (propSetMethod == null ? Properties.class : propSetMethod.getParameterTypes()[0]);
                propValue = loadFromXml(propNode, propSetMethod, false, targetPropValue, propClass);
            }

            final Object oldPropValue = properties.get(propName);

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
                    final Class<?> parameterType = propSetMethod.getParameterTypes()[0];

                    if (Strings.isEmpty(propValue.toString()) && Properties.class.isAssignableFrom(parameterType)) {
                        propValue = N.newInstance(parameterType);
                    }

                    ClassUtil.setPropValue(properties, propSetMethod, propValue);
                }
            }
        }

        if (output != null) {
            final Set<String> oldKeySet = N.newHashSet(properties.keySet());
            Method removeMethod = null;
            for (final String key : oldKeySet) {
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
     * Stores the specified properties to the given file with optional comments.
     *
     * @param properties The properties to store.
     * @param comments The comments to include in the stored file.
     * @param output The file to which the properties will be stored.
     */
    public static void store(final Properties<?, ?> properties, final String comments, final File output) {
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            store(properties, comments, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Stores the specified properties to the given OutputStream with optional comments.
     *
     * @param properties The properties to store.
     * @param comments The comments to include in the stored output.
     * @param output The OutputStream to which the properties will be stored.
     */
    public static void store(final Properties<?, ?> properties, final String comments, final OutputStream output) {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            store(properties, comments, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Stores the specified properties to the given Writer with optional comments.
     *
     * @param properties The properties to store.
     * @param comments The comments to include in the stored output.
     * @param output The Writer to which the properties will be stored.
     */
    public static void store(final Properties<?, ?> properties, final String comments, final Writer output) {
        final java.util.Properties tmp = new java.util.Properties();

        tmp.putAll(properties);

        try {
            tmp.store(output, comments);

            output.flush();

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Stores the specified properties to the given XML file.
     *
     * @param properties The properties to store.
     * @param rootElementName The name of the root element in the XML.
     * @param ignoreTypeInfo If {@code true}, type information will be ignored.
     * @param output The file to which the properties will be stored.
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean ignoreTypeInfo, final File output) {
        Writer writer = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            storeToXml(properties, rootElementName, ignoreTypeInfo, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Stores the specified properties to the given XML OutputStream.
     *
     * @param properties The properties to store.
     * @param rootElementName The name of the root element in the XML.
     * @param ignoreTypeInfo If {@code true}, type information will be ignored.
     * @param output The OutputStream to which the properties will be stored.
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean ignoreTypeInfo, final OutputStream output)
            throws UncheckedIOException {
        storeToXml(properties, rootElementName, ignoreTypeInfo, true, IOUtil.newOutputStreamWriter(output));
    }

    /**
     * Stores the specified properties to the given XML OutputStream.
     *
     * @param properties The properties to store.
     * @param rootElementName The name of the root element in the XML.
     * @param ignoreTypeInfo If {@code true}, type information will be ignored.
     * @param output The Writer to which the properties will be stored.
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    public static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean ignoreTypeInfo, final Writer output)
            throws UncheckedIOException {
        storeToXml(properties, rootElementName, ignoreTypeInfo, true, output);
    }

    /**
     * Stores the specified properties to the given XML OutputStream.
     *
     * @param properties The properties to store.
     * @param rootElementName The name of the root element in the XML.
     * @param ignoreTypeInfo If {@code true}, type information will be ignored.
     * @param isFirstCall If {@code true}, this is the first call to the method.
     * @param output The OutputStream to which the properties will be stored.
     * @throws UncheckedIOException Signals that an I/O exception has occurred.
     */
    private static void storeToXml(final Properties<?, ?> properties, final String rootElementName, final boolean ignoreTypeInfo, final boolean isFirstCall,
            final Writer output) throws UncheckedIOException {
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(output);

        try {
            if (isFirstCall) {
                bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            }

            if ((isFirstCall || ignoreTypeInfo) || !properties.getClass().equals(Properties.class)) {
                bw.write("<" + rootElementName + ">");
            } else {
                bw.write("<" + rootElementName + " type=\"Properties\">");
            }

            String propName = null;
            String listPropName = null;
            String elementPropName = null;
            Object propValue = null;
            Object listPropValue;
            Type<Object> type = null;
            for (final Map.Entry<?, ?> entry : properties.entrySet()) { //NOSONAR
                propName = entry.getKey().toString();
                propValue = entry.getValue();

                elementPropName = propName;

                if (elementPropName.endsWith("List")) {
                    elementPropName = elementPropName.substring(0, elementPropName.length() - 4);
                }

                listPropName = elementPropName + "List";

                listPropValue = properties.get(listPropName);

                if ((propValue == null) || (listPropValue instanceof List && ((List<?>) listPropValue).size() > 0)) {
                    continue;
                }

                if (propValue instanceof List && properties.containsKey(elementPropName)) {
                    for (final Object e : ((List<?>) propValue)) {
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

                    storeToXml((Properties<?, ?>) propValue, propName, ignoreTypeInfo, false, output);
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
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Generate Java code from the specified XML.
     *
     * @param xml The XML content as a string.
     * @param srcPath The source path where the generated Java code will be saved.
     * @param packageName The package name for the generated Java classes.
     * @param className The name of the generated Java class.
     * @param isPublicField If {@code true}, the fields in the generated Java class will be public.
     */
    public static void xml2Java(final String xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        xml2Java(IOUtil.string2InputStream(xml), srcPath, packageName, className, isPublicField);
    }

    /**
     * Generate Java code from the specified XML file.
     *
     * @param xml The XML file from which to generate Java code.
     * @param srcPath The source path where the generated Java code will be saved.
     * @param packageName The package name for the generated Java classes.
     * @param className The name of the generated Java class.
     * @param isPublicField If {@code true}, the fields in the generated Java class will be public.
     */
    public static void xml2Java(final File xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(xml);

            xml2Java(reader, srcPath, packageName, className, isPublicField);
        } finally {
            IOUtil.close(reader);
        }
    }

    /**
     * Generate Java code from the specified XML InputStream.
     *
     * @param xml The InputStream from which to generate Java code.
     * @param srcPath The source path where the generated Java code will be saved.
     * @param packageName The package name for the generated Java classes.
     * @param className The name of the generated Java class.
     * @param isPublicField If {@code true}, the fields in the generated Java class will be public.
     */
    public static void xml2Java(final InputStream xml, final String srcPath, final String packageName, final String className, final boolean isPublicField) {
        xml2Java(IOUtil.newInputStreamReader(xml), srcPath, packageName, className, isPublicField);
    }

    /**
     * Generate Java code from the specified XML Reader.
     *
     * @param xml The Reader from which to generate Java code.
     * @param srcPath The source path where the generated Java code will be saved.
     * @param packageName The package name for the generated Java classes.
     * @param className The name of the generated Java class.
     * @param isPublicField If {@code true}, the fields in the generated Java class will be public.
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    public static void xml2Java(final Reader xml, final String srcPath, final String packageName, String className, final boolean isPublicField) {
        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(true, true);
        Writer writer = null;

        try { //NOSONAR
            final Document doc = docBuilder.parse(new InputSource(xml));
            final Node root = doc.getFirstChild();

            // TODO it's difficult to support duplicated property and may be misused.
            if (hasDuplicatedPropName(root)) {
                throw new RuntimeException("The source xml document contains duplicated properties which has same node tag name in the same root.");
            }

            if (className == null) {
                className = Strings.capitalize(root.getNodeName());
            }

            final String classFilePath = ClassUtil.makePackageFolder(srcPath, packageName);
            final File classFile = new File(classFilePath + className + ".java");

            IOUtil.deleteIfExists(classFile);

            IOUtil.createNewFileIfNotExists(classFile);

            writer = IOUtil.newFileWriter(classFile, Charsets.DEFAULT);
            writer.write("package " + packageName + ";" + IOUtil.LINE_SEPARATOR);

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write(IOUtil.LINE_SEPARATOR);

            final Set<String> importType = getImportType(root);

            if (hasDuplicatedPropName(root)) {
                importType.add(List.class.getCanonicalName());
                importType.add(java.util.ArrayList.class.getCanonicalName());
                importType.add(java.util.Collections.class.getCanonicalName());
            }

            importType.add(Map.class.getCanonicalName());

            for (final String clsName : importType) {
                writer.write("import " + clsName + ";" + IOUtil.LINE_SEPARATOR);
            }

            writer.write(IOUtil.LINE_SEPARATOR);
            writer.write("import " + Properties.class.getCanonicalName() + ";" + IOUtil.LINE_SEPARATOR);
            writer.write(IOUtil.LINE_SEPARATOR);

            xmlProperties2Java(root, className, isPublicField, "", true, writer);

            writer.flush();
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            IOUtil.close(writer);
        }
    }

    private static void xmlProperties2Java(final Node xmlNode, String className, final boolean isPublicField, final String spaces, final boolean isRoot,
            final Writer output) throws IOException {
        if (className == null) {
            className = Strings.capitalize(xmlNode.getNodeName());
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

        final NodeList childNodes = xmlNode.getChildNodes();

        //noinspection ConstantValue
        if ((childNodes != null) && (childNodes.getLength() > 0)) {
            final Set<String> duplicatedPropNameSet = getDuplicatedPropNameSet(xmlNode);
            final Set<String> propNameSet = N.newHashSet();

            Node childNode = null;
            String propName = null;

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

                //    typeName = getTypeName(childNode, propName);
                //
                //    output.write(spaces + "    " + (isPublicField ? "public " : "private ") + typeName + " " + propName + ";" + IOUtil.LINE_SEPARATOR);
                //
                //    if (duplicatedPropNameSet.contains(propName)) {
                //        final String listPropName = propName + "List";
                //        final String elementTypeName = N.typeOf(typeName).isPrimitiveType()
                //                ? ClassUtil.getSimpleClassName(ClassUtil.wrap(N.typeOf(typeName).clazz()))
                //                : typeName;
                //
                //        output.write(spaces + "    " + (isPublicField ? "public " : "private ") + "List<" + elementTypeName + "> " + listPropName
                //                + " = Collections.synchronizedList(new ArrayList<" + elementTypeName + ">());" + IOUtil.LINE_SEPARATOR);
                //    }
            }

            propNameSet.clear();

            final String methodSpace = spaces + "    ";
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

                output.write(IOUtil.LINE_SEPARATOR);

                typeName = getTypeName(childNode, propName);

                writeMethod(methodSpace, propName, typeName, duplicatedPropNameSet, output);
            }

            // disable put/put/all/set/remove method
            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public " + className + " set(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "return (" + className + ") super.set(propName, propValue);" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public Object put(String propName, Object propValue) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "return super.put(propName, propValue);" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public void putAll(Map<? extends String, ? extends Object> m) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "super.putAll(m);" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            output.write(IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "public Object remove(Object propName) {" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "    " + "return super.remove(propName);" + IOUtil.LINE_SEPARATOR);
            output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            //    output.write(IOUtil.LINE_SEPARATOR);
            //    output.write(methodSpace + "@Deprecated" + IOUtil.LINE_SEPARATOR);
            //    output.write(methodSpace + "@Override" + IOUtil.LINE_SEPARATOR);
            //    output.write(methodSpace + "public void clear() {" + IOUtil.LINE_SEPARATOR);
            //    output.write(methodSpace + "    " + "super.clear();" + IOUtil.LINE_SEPARATOR);
            //    output.write(methodSpace + "}" + IOUtil.LINE_SEPARATOR);

            propNameSet.clear();

            for (int i = 0; i < childNodes.getLength(); i++) {
                childNode = childNodes.item(i);

                if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                    continue;
                }

                propName = ClassUtil.formalizePropName(childNode.getNodeName());

                if (propNameSet.contains(propName) || Strings.isNotEmpty(XmlUtil.getAttribute(childNode, TYPE))) {
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

    private static Set<String> getImportType(final Node node) {
        final Set<String> result = N.newLinkedHashSet();
        final NodeList childNodes = node.getChildNodes();

        //noinspection ConstantValue
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

            attr = XmlUtil.getAttribute(childNode, TYPE);

            if (Strings.isNotEmpty(attr)) {
                type = N.typeOf(attr);
                if (type != null) {
                    final Class<?> typeClass = type.clazz();
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

    private static void writeMethod(final String spaces, final String propName, final String typeName, final Set<String> duplicatedPropNameSet,
            final Writer output) throws IOException {
        final String listPropName = propName + "List";
        final String elementTypeName = N.typeOf(typeName).isPrimitiveType() ? ClassUtil.getSimpleClassName(ClassUtil.wrap(N.typeOf(typeName).clazz()))
                : typeName;

        output.write(spaces + "public " + typeName + " get" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "return (" + typeName + ") super.get(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        output.write(IOUtil.LINE_SEPARATOR);

        output.write(spaces + "public void set" + Strings.capitalize(propName) + "(" + typeName + " " + propName + ") {" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "super.put(\"" + propName + "\", " + propName + ");" + IOUtil.LINE_SEPARATOR);
        // output.write(spaces + "    " + "this." + propName + " = " + propName + ";" + IOUtil.LINE_SEPARATOR);

        if (duplicatedPropNameSet.contains(propName)) {
            output.write(spaces + "    " + "put(\"" + listPropName + "\", " + listPropName + ");" + IOUtil.LINE_SEPARATOR);
            output.write(spaces + "    " + "this." + listPropName + ".add(" + propName + ");" + IOUtil.LINE_SEPARATOR);
        }

        output.write(spaces + "}" + IOUtil.LINE_SEPARATOR);

        output.write(IOUtil.LINE_SEPARATOR);

        output.write(spaces + "public void remove" + Strings.capitalize(propName) + "() {" + IOUtil.LINE_SEPARATOR);
        output.write(spaces + "    " + "super.remove(\"" + propName + "\");" + IOUtil.LINE_SEPARATOR);
        // output.write(spaces + "    " + "this." + propName + " = " + N.typeOf(typeName).defaultValue() + ";" + IOUtil.LINE_SEPARATOR);

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

    private static String getTypeName(final Node node, final String propName) {
        String typeName = node.getChildNodes().getLength() > 1 ? Strings.capitalize(propName) : "String";
        final String typeAttr = XmlUtil.getAttribute(node, TYPE);

        if (Strings.isNotEmpty(typeAttr)) {
            if (typeAttr.equals("Properties")) {
                typeName = "Properties<String, Object>";
            } else {
                final Type<?> type = N.typeOf(typeAttr);
                if (type != null) {
                    typeName = type.clazz().getSimpleName();
                }
            }
        }

        return typeName;
    }

    private static boolean hasDuplicatedPropName(final Node node) {
        final NodeList childNodes = node.getChildNodes();

        //noinspection ConstantValue
        if ((childNodes == null) || (childNodes.getLength() == 0)) {
            return false;
        }

        String propName = null;
        Node childNode = null;

        final Set<String> propNameSet = N.newHashSet();

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);

            if (childNode.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }

            propName = ClassUtil.formalizePropName(childNode.getNodeName());

            if (propNameSet.contains(propName) || ((childNode.getChildNodes().getLength() > 1) && hasDuplicatedPropName(childNode))) {
                return true;
            } else {
                propNameSet.add(propName);
            }
        }

        return false;
    }

    private static Set<String> getDuplicatedPropNameSet(final Node node) {
        final NodeList childNodes = node.getChildNodes();
        //noinspection ConstantValue
        if (childNodes == null || childNodes.getLength() == 0) {
            return N.newHashSet();
        }

        final Set<String> propNameSet = N.newHashSet();
        final Set<String> duplicatedPropNameSet = N.newHashSet();

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

        /** The created time. */
        private Timestamp createdTime;

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
        public void setId(final long id) {
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
        public void setName(final String name) {
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
        public void setContent(final String content) {
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
        public void setIncludedServers(final List<String> includedServers) {
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
        public void setExcludedServers(final List<String> excludedServers) {
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
        public void setStatus(final Status status) {
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
        public void setDescription(final String description) {
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
        public void setLastUpdateTime(final Timestamp lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        /**
         * Gets the creates the time.
         *
         * @return
         */
        public Timestamp getCreatedTime() {
            return createdTime;
        }

        /**
         * Sets the creates the time.
         *
         * @param createdTime the new creates the time
         */
        public void setCreatedTime(final Timestamp createdTime) {
            this.createdTime = createdTime;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, content, includedServers, excludedServers, status, description, lastUpdateTime, createdTime);
        }

        /**
         *
         * @param obj
         * @return
         */
        @SuppressFBWarnings
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ConfigBean other) {
                return N.equals(other.id, id) && N.equals(other.name, name) && N.equals(other.content, content)
                        && N.equals(other.includedServers, includedServers) && N.equals(other.excludedServers, excludedServers)
                        && N.equals(other.status, status) && N.equals(other.description, description) && N.equals(other.lastUpdateTime, lastUpdateTime)
                        && N.equals(other.createdTime, createdTime);

            }

            return false;
        }

        @Override
        public String toString() {
            return "{id=" + id + ", name=" + name + ", content=" + content + ", includedServers=" + includedServers + ", excludedServers=" + excludedServers
                    + ", status=" + status + ", description=" + description + ", lastUpdateTime=" + lastUpdateTime + ", createdTime=" + createdTime + "}";
        }

    }

    enum ResourceType {
        PROPERTIES, XML
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
        public Resource(final Class<?> cls, final File file, final ResourceType resourceType) {
            targetClass = cls;
            this.file = file;
            filePath = file.getPath();
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
        public void setLastLoadTime(final long lastLoadTime) {
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
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Resource other) {
                return N.equals(other.targetClass, targetClass) && N.equals(other.filePath, filePath);

            }

            return false;
        }

        @Override
        public String toString() {
            return "{file=" + file + "}";
        }
    }
}
