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

package com.landawn.abacus.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.ObjectPool;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

final class AbacusXMLParserImpl extends AbstractXMLParser {

    // ...
    private static final Map<Class<?>, Map<String, Class<?>>> nodeNameClassMapPool = new ConcurrentHashMap<>(POOL_SIZE);

    private static final Map<String, NodeType> nodeTypePool = new ObjectPool<>(64);

    static {
        nodeTypePool.put(XMLConstants.ARRAY, NodeType.ARRAY);
        nodeTypePool.put(XMLConstants.LIST, NodeType.COLLECTION);
        nodeTypePool.put(XMLConstants.SET, NodeType.COLLECTION);
        nodeTypePool.put(XMLConstants.COLLECTION, NodeType.COLLECTION);
        nodeTypePool.put(XMLConstants.MAP, NodeType.MAP);
        nodeTypePool.put(XMLConstants.E, NodeType.ELEMENT);
        nodeTypePool.put(XMLConstants.ENTRY, NodeType.ENTRY);
        nodeTypePool.put(XMLConstants.KEY, NodeType.KEY);
        nodeTypePool.put(XMLConstants.VALUE, NodeType.VALUE);
    }

    // ...
    private static final Queue<XmlSAXHandler<?>> xmlSAXHandlerPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private final XMLParserType parserType;

    AbacusXMLParserImpl(final XMLParserType parserType) {
        this.parserType = parserType;
    }

    AbacusXMLParserImpl(final XMLParserType parserType, final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        super(xsc, xdc);
        this.parserType = parserType;
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    @Override
    public String serialize(final Object obj, final XMLSerializationConfig config) {
        if (obj == null) {
            return Strings.EMPTY_STRING;
        }

        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();
        final IdentityHashSet<Object> serializedObjects = config == null || !config.supportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, null, config, null, serializedObjects, bw, false);

            return bw.toString();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final File output) {
        Writer writer = null;

        try {
            createNewFileIfNotExists(output);

            writer = IOUtil.newFileWriter(output);

            serialize(obj, config, writer);

            writer.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final OutputStream output) {
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(output);
        final IdentityHashSet<Object> serializedObjects = config == null || !config.supportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, null, config, null, serializedObjects, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param output
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final Writer output) {
        final boolean isBufferedWriter = output instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) output : Objectory.createBufferedXMLWriter(output);
        final IdentityHashSet<Object> serializedObjects = config == null || !config.supportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, null, config, null, serializedObjects, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     *
     * @param obj
     * @param propInfo
     * @param config
     * @param indentation
     * @param serializedObjects
     * @param bw
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void write(final Object obj, final PropInfo propInfo, final XMLSerializationConfig config, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final BufferedXMLWriter bw, final boolean flush) throws IOException {
        final XMLSerializationConfig configToUse = check(config);

        if (obj == null) {
            IOUtil.write(Strings.EMPTY_STRING, bw);
            return;
        }

        if (propInfo != null && propInfo.isJsonRawValue) {
            strType.writeCharacter(bw, jsonParser.serialize(obj, getJSC(config)), config);
            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = N.typeOf(cls);

        switch (type.getSerializationType()) {
            case SERIALIZABLE:
                if (type.isObjectArray()) {
                    writeArray(obj, configToUse, indentation, serializedObjects, type, bw);
                } else if (type.isCollection()) {
                    writeCollection((Collection<?>) obj, configToUse, indentation, serializedObjects, type, bw);
                } else {
                    if (propInfo != null && propInfo.hasFormat) {
                        propInfo.writePropValue(bw, obj, configToUse);
                    } else {
                        type.writeCharacter(bw, obj, configToUse);
                    }
                }

                break;

            case ENTITY:
                writeBean(obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            case MAP:
                writeMap((Map<?, ?>) obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            case ARRAY:
                writeArray(obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            case COLLECTION:
                writeCollection((Collection<?>) obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            default:
                throw new ParseException("Unsupported class: " + ClassUtil.getCanonicalClassName(cls)
                        + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
        }

        if (flush) {
            bw.flush();
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param indentation
     * @param serializedObjects
     * @param type TODO
     * @param bw
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void writeBean(final Object obj, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        if (hasCircularReference(obj, serializedObjects, bw)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

        if (N.isEmpty(beanInfo.jsonXmlSerializablePropInfos)) {
            throw new ParseException("No serializable property is found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final boolean tagByPropertyName = config.tagByPropertyName();
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (tagByPropertyName) {
            if (ignoreTypeInfo) {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].namedStart);
            } else {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].namedStartWithType);
            }
        } else {
            if (ignoreTypeInfo) {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].epStart);
            } else {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].epStartWithType);
            }
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;

        writeProperties(obj, config, propIndentation, serializedObjects, type, bw);

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        if (tagByPropertyName) {
            bw.write(beanInfo.xmlNameTags[nameTagIdx].namedEnd);
        } else {
            bw.write(beanInfo.xmlNameTags[nameTagIdx].epEnd);
        }
    }

    /**
     *
     * @param obj
     * @param config
     * @param propIndentation
     * @param serializedObjects
     * @param type TODO
     * @param bw
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void writeProperties(final Object obj, final XMLSerializationConfig config, final String propIndentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        if (hasCircularReference(obj, serializedObjects, bw)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

        final Exclusion exclusion = getExclusion(config, beanInfo);

        final boolean ignoreNullProperty = (exclusion == Exclusion.NULL) || (exclusion == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = (exclusion == Exclusion.DEFAULT);

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean tagByPropertyName = config.tagByPropertyName();
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        final String nextIndentation = isPrettyFormat ? ((propIndentation == null ? Strings.EMPTY_STRING : propIndentation) + config.getIndentation()) : null;
        final PropInfo[] propInfoList = config.skipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();
        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;

        for (final PropInfo element : propInfoList) {
            propInfo = element;
            propName = propInfo.name;

            if (propInfo.jsonXmlExpose == JsonXmlField.Expose.DESERIALIZE_ONLY
                    || ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(propName))) {
                continue;
            }

            propValue = propInfo.getPropValue(obj);

            if ((ignoreNullProperty && propValue == null) || (ignoreDefaultProperty && propValue != null && (propInfo.jsonXmlType != null)
                    && propInfo.jsonXmlType.isPrimitiveType() && propValue.equals(propInfo.jsonXmlType.defaultValue()))) {
                continue;
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(propIndentation);
            }

            if (propValue == null) {
                if (tagByPropertyName) {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNull);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNullWithType);
                    }
                } else {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNull);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNullWithType);
                    }
                }
            } else {
                if (tagByPropertyName) {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStart);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStartWithType);
                    }
                } else {
                    if (ignoreTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStart);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStartWithType);
                    }
                }

                if (propInfo.isJsonRawValue) {
                    strType.writeCharacter(bw, jsonParser.serialize(obj, getJSC(config)), config);
                } else if (propInfo.jsonXmlType.isSerializable()) {
                    if (propInfo.jsonXmlType.isObjectArray() || propInfo.jsonXmlType.isCollection()) {
                        // jsonParser.serialize(bw, propValue);

                        strType.writeCharacter(bw, jsonParser.serialize(propValue, getJSC(config)), config);
                    } else {
                        if (propInfo.hasFormat) {
                            propInfo.writePropValue(bw, propValue, config);
                        } else {
                            propInfo.jsonXmlType.writeCharacter(bw, propValue, config);
                        }
                    }
                } else {
                    write(propValue, propInfo, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR);
                        bw.write(propIndentation);
                    }
                }

                if (tagByPropertyName) {
                    bw.write(propInfo.xmlNameTags[nameTagIdx].namedEnd);
                } else {
                    bw.write(propInfo.xmlNameTags[nameTagIdx].epEnd);
                }
            }
        }
    }

    /**
     *
     * @param m
     * @param config
     * @param indentation
     * @param serializedObjects
     * @param type TODO
     * @param bw
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void writeMap(final Map<?, ?> m, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        if (hasCircularReference(m, serializedObjects, bw)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (ignoreTypeInfo) {
            bw.write(XMLConstants.MAP_ELE_START);
        } else {
            bw.write(XMLConstants.START_MAP_ELE_WITH_TYPE);
            bw.write(N.typeOf(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        }

        final String entryIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        final String keyValueIndentation = entryIndentation + config.getIndentation();
        final String nextIndentation = keyValueIndentation + config.getIndentation();

        final Type<Object> stringType = N.typeOf(String.class);
        Type<Object> keyType = null;
        Type<Object> valueType = null;
        Object key = null;
        Object value = null;

        for (final Map.Entry<Object, Object> entry : ((Map<Object, Object>) m).entrySet()) {
            key = entry.getKey();

            //noinspection SuspiciousMethodCalls
            if ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(key)) {
                continue;
            }

            value = entry.getValue();

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(entryIndentation);
            }

            bw.write(XMLConstants.ENTRY_ELE_START);

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(keyValueIndentation);
            }

            if (key == null) {
                bw.write(XMLConstants.KEY_NULL_ELE);
            } else {
                if (key.getClass() == String.class) {
                    if (ignoreTypeInfo) {
                        bw.write(XMLConstants.KEY_ELE_START);
                    } else {
                        bw.write(XMLConstants.START_KEY_ELE_WITH_STRING_TYPE);
                    }

                    stringType.writeCharacter(bw, key, config);
                } else {
                    keyType = N.typeOf(key.getClass());

                    if (ignoreTypeInfo) {
                        bw.write(XMLConstants.KEY_ELE_START);
                    } else {
                        bw.write(XMLConstants.START_KEY_ELE_WITH_TYPE);
                        bw.write(keyType.xmlName());
                        bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                    }

                    if (keyType.isSerializable()) {
                        if (keyType.isObjectArray() || keyType.isCollection()) {
                            // jsonParser.serialize(bw, key);

                            strType.writeCharacter(bw, jsonParser.serialize(key, getJSC(config)), config);
                        } else {
                            keyType.writeCharacter(bw, key, config);
                        }
                    } else {
                        write(key, null, config, nextIndentation, serializedObjects, bw, false);

                        if (isPrettyFormat) {
                            bw.write(IOUtil.LINE_SEPARATOR);
                            bw.write(keyValueIndentation);
                        }
                    }
                }

                bw.write(XMLConstants.KEY_ELE_END);
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(keyValueIndentation);
            }

            if (value == null) {
                bw.write(XMLConstants.VALUE_NULL_ELE);
            } else {
                valueType = N.typeOf(value.getClass());

                if (ignoreTypeInfo) {
                    bw.write(XMLConstants.VALUE_ELE_START);
                } else {
                    bw.write(XMLConstants.START_VALUE_ELE_WITH_TYPE);
                    bw.write(valueType.xmlName());
                    bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                }

                if (valueType.isSerializable()) {
                    if (valueType.isObjectArray() || valueType.isCollection()) {
                        // jsonParser.serialize(bw, value);

                        strType.writeCharacter(bw, jsonParser.serialize(value, getJSC(config)), config);
                    } else {
                        valueType.writeCharacter(bw, value, config);
                    }
                } else {
                    write(value, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR);
                        bw.write(keyValueIndentation);
                    }
                }

                bw.write(XMLConstants.VALUE_ELE_END);
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(entryIndentation);
            }

            bw.write(XMLConstants.ENTRY_ELE_END);
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XMLConstants.MAP_ELE_END);
    }

    /**
     *
     * @param obj
     * @param config
     * @param indentation
     * @param serializedObjects
     * @param type TODO
     * @param bw
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void writeArray(final Object obj, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        if (hasCircularReference(obj, serializedObjects, bw)) {
            return;
        }

        final Class<?> cls = type.clazz();
        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (ignoreTypeInfo) {
            bw.write(XMLConstants.ARRAY_ELE_START);
        } else {
            bw.write(XMLConstants.START_ARRAY_ELE_WITH_TYPE);
            bw.write(N.typeOf(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        }

        final String eleIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        final String nextIndentation = eleIndentation + config.getIndentation();
        final Object[] a = (Object[]) obj;
        Type<Object> eleType = null;

        for (final Object e : a) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(eleIndentation);
            }

            if (e == null) {
                bw.write(XMLConstants.E_NULL_ELE);
            } else {
                eleType = N.typeOf(e.getClass());

                if (ignoreTypeInfo) {
                    bw.write(XMLConstants.E_ELE_START);
                } else {
                    bw.write(XMLConstants.START_E_ELE_WITH_TYPE);
                    bw.write(eleType.xmlName());
                    bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                }

                if (eleType.isSerializable()) {
                    if (eleType.isObjectArray() || eleType.isCollection()) {
                        // jsonParser.serialize(bw, e);

                        strType.writeCharacter(bw, jsonParser.serialize(e, getJSC(config)), config);
                    } else {
                        eleType.writeCharacter(bw, e, config);
                    }
                } else {
                    write(e, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR);
                        bw.write(eleIndentation);
                    }
                }

                bw.write(XMLConstants.E_ELE_END);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XMLConstants.ARRAY_ELE_END);
    }

    /**
     *
     * @param c
     * @param config
     * @param indentation
     * @param serializedObjects
     * @param type TODO
     * @param bw
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void writeCollection(final Collection<?> c, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        if (hasCircularReference(c, serializedObjects, bw)) {
            return;
        }

        final boolean ignoreTypeInfo = config.ignoreTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR);
            bw.write(indentation);
        }

        if (type.isList()) {
            if (ignoreTypeInfo) {
                bw.write(XMLConstants.LIST_ELE_START);
            } else {
                bw.write(XMLConstants.START_LIST_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            }
        } else if (type.isSet()) {
            if (ignoreTypeInfo) {
                bw.write(XMLConstants.SET_ELE_START);
            } else {
                bw.write(XMLConstants.START_SET_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            }
        } else {
            if (ignoreTypeInfo) {
                bw.write(XMLConstants.COLLECTION_ELE_START);
            } else {
                bw.write(XMLConstants.START_COLLECTION_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            }
        }

        final String eleIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY_STRING : indentation) + config.getIndentation()) : null;
        final String nextIndentation = eleIndentation + config.getIndentation();

        Type<Object> eleType = null;

        for (final Object e : c) {
            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write(eleIndentation);
            }

            if (e == null) {
                bw.write(XMLConstants.E_NULL_ELE);
            } else {
                eleType = N.typeOf(e.getClass());

                if (ignoreTypeInfo) {
                    bw.write(XMLConstants.E_ELE_START);
                } else {
                    bw.write(XMLConstants.START_E_ELE_WITH_TYPE);
                    bw.write(eleType.xmlName());
                    bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                }

                if (eleType.isSerializable()) {
                    if (eleType.isObjectArray() || eleType.isCollection()) {
                        // jsonParser.serialize(bw, e);

                        strType.writeCharacter(bw, jsonParser.serialize(e, getJSC(config)), config);
                    } else {
                        eleType.writeCharacter(bw, e, config);
                    }
                } else {
                    write(e, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR);
                        bw.write(eleIndentation);
                    }
                }

                bw.write(XMLConstants.E_ELE_END);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        if (type.isList()) {
            bw.write(XMLConstants.LIST_ELE_END);
        } else if (type.isSet()) {
            bw.write(XMLConstants.SET_ELE_END);
        } else {
            bw.write(XMLConstants.COLLECTION_ELE_END);
        }
    }

    /**
     * Checks for circular reference.
     * @param obj
     * @param serializedObjects
     * @param bw
     *
     * @return {@code true}, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private boolean hasCircularReference(final Object obj, final IdentityHashSet<Object> serializedObjects, final BufferedXMLWriter bw) throws IOException {
        if (obj != null && serializedObjects != null) {
            if (serializedObjects.contains(obj)) {
                bw.write("null");
                return true;
            } else {
                serializedObjects.add(obj);
            }
        }

        return false;
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final String source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        if (Strings.isEmpty(source)) {
            return N.defaultValueOf(targetClass);
        }

        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, null, targetClass);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final File source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return deserialize(reader, config, targetClass);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, null, targetClass);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        // BufferedReader? will the target parser create the BufferedReader
        // internally.
        return read(source, config, null, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param targetClass
     * @return
     */
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        return readByDOMParser(source, config, targetClass);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses) {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, nodeClasses, null);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses) {
        return read(source, config, nodeClasses, null);
    }

    /**
     *
     * @param <T>
     * @param source
     * @param config
     * @param nodeClasses
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses) {
        return (T) readByDOMParser(source, config, nodeClasses.get(source.getNodeName()));
    }

    /**
     *
     * @param source
     * @param config
     * @param nodeClasses
     * @param targetClass
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    <T> T read(final Reader source, final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses, Class<? extends T> targetClass) {
        final XMLDeserializationConfig configToUse = check(config);

        switch (parserType) {
            case SAX:

                final SAXParser saxParser = XmlUtil.createSAXParser();
                final XmlSAXHandler<T> dh = getXmlSAXHandler(configToUse, nodeClasses, targetClass);
                T result = null;

                try {
                    saxParser.parse(new InputSource(source), dh);
                    result = dh.resultHolder.value();
                } catch (final SAXException e) {
                    throw new ParseException(e);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    recycle(dh);
                    XmlUtil.recycleSAXParser(saxParser);
                }

                return result;

            case StAX:
                try {
                    final XMLStreamReader xmlReader = createXMLStreamReader(source);

                    //noinspection StatementWithEmptyBody
                    for (int event = xmlReader.next(); event != XMLStreamConstants.START_ELEMENT && xmlReader.hasNext(); event = xmlReader.next()) {
                        // do nothing.
                    }

                    if (targetClass == null && N.notEmpty(nodeClasses)) {
                        String nodeName = null;

                        if (xmlReader.getAttributeCount() > 0) {
                            nodeName = xmlReader.getAttributeValue(null, XMLConstants.NAME);
                        }

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = xmlReader.getLocalName();
                        }

                        targetClass = (Class<T>) nodeClasses.get(nodeName);
                    }

                    if (targetClass == null) {
                        throw new ParseException("No target class is specified");
                    }

                    return readByStreamParser(xmlReader, configToUse, targetClass);
                } catch (final XMLStreamException e) {
                    throw new ParseException(e);
                }

            case DOM: //NOSONAR
                final DocumentBuilder docBuilder = XmlUtil.createContentParser();

                try {
                    final Document doc = docBuilder.parse(new InputSource(source));
                    final Node node = doc.getFirstChild();

                    if (targetClass == null && N.notEmpty(nodeClasses)) {
                        String nodeName = XmlUtil.getAttribute(node, XMLConstants.NAME);

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = node.getNodeName();
                        }

                        targetClass = (Class<T>) nodeClasses.get(nodeName);
                    }

                    if (targetClass == null) {
                        throw new ParseException("No target class is specified");
                    }

                    return readByDOMParser(node, configToUse, targetClass);
                } catch (final SAXException e) {
                    throw new ParseException(e);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    XmlUtil.recycleContentParser(docBuilder);
                }

            default:
                throw new ParseException("Unsupported parser: " + parserType);
        }
    }

    /**
     * Read by stream parser.
     * @param xmlReader
     * @param config
     * @param inputClass
     *
     * @param <T>
     * @return
     * @throws XMLStreamException the XML stream exception
     */
    <T> T readByStreamParser(final XMLStreamReader xmlReader, final XMLDeserializationConfig config, final Class<T> inputClass) throws XMLStreamException {
        return readByStreamParser(xmlReader, config, null, null, false, false, false, true, inputClass, inputClass);
    }

    /**
     * Read by stream parser.
     * @param xmlReader
     * @param config
     * @param propType
     * @param propInfo
     * @param checkedAttr
     * @param isTagByPropertyName
     * @param ignoreTypeInfo
     * @param isFirstCall
     * @param inputClass
     * @param targetClass
     * @param <T>
     * @return
     * @throws XMLStreamException the XML stream exception
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    @SuppressWarnings({ "null", "fallthrough", "unused", "deprecation", "DataFlowIssue" })
    <T> T readByStreamParser(final XMLStreamReader xmlReader, final XMLDeserializationConfig config, Type<?> propType, PropInfo propInfo, boolean checkedAttr,
            boolean isTagByPropertyName, boolean ignoreTypeInfo, final boolean isFirstCall, final Class<?> inputClass, Class<?> targetClass)
            throws XMLStreamException {

        final boolean hasPropTypes = config.hasValueTypes();
        String nodeName = null;

        if (checkedAttr) {
            nodeName = isTagByPropertyName || xmlReader.getAttributeCount() == 0 ? xmlReader.getLocalName() : getAttribute(xmlReader, XMLConstants.NAME);
        } else {
            final String nameAttr = getAttribute(xmlReader, XMLConstants.NAME);
            nodeName = Strings.isNotEmpty(nameAttr) ? nameAttr : xmlReader.getLocalName();
        }

        targetClass = hasPropTypes ? config.getValueTypeClass(nodeName, targetClass) : targetClass;

        targetClass = checkedAttr ? (ignoreTypeInfo ? targetClass : getConcreteClass(targetClass, xmlReader)) : getConcreteClass(targetClass, xmlReader);
        NodeType nodeType = null;

        if (nodeName == null) {
            final Type<?> targetType = N.typeOf(targetClass);
            if (targetType.isMap()) {
                nodeType = NodeType.MAP;
            } else if (targetType.isArray()) {
                nodeType = NodeType.ARRAY;
            } else if (targetType.isCollection()) {
                nodeType = NodeType.COLLECTION;
            } else if (targetType.isBean()) {
                nodeType = NodeType.ENTITY;
            } else {
                nodeType = NodeType.PROPERTY;
            }
        } else {
            nodeType = getNodeType(nodeName, null);
        }

        String propName = null;
        Object propValue = null;
        boolean isNullValue = false;
        String text = null;
        StringBuilder sb = null;

        switch (nodeType) {
            case ENTITY: {
                if (!ClassUtil.isBeanClass(targetClass)) {
                    if ((propType != null) && propType.isBean()) {
                        targetClass = propType.clazz();
                    } else {
                        if (ClassUtil.getSimpleClassName(inputClass).equalsIgnoreCase(nodeName)) {
                            targetClass = inputClass;
                        } else {
                            if (Collection.class.isAssignableFrom(inputClass) || Map.class.isAssignableFrom(inputClass) || inputClass.isArray()) {
                                if (propType != null) {
                                    targetClass = getClassByNodeName(nodeName, propType.clazz());
                                }
                            } else {
                                targetClass = getClassByNodeName(nodeName, inputClass);
                            }

                            checkBeanType(nodeName, inputClass, targetClass);
                        }
                    }
                }

                if (!checkedAttr) {
                    isTagByPropertyName = Strings.isEmpty(getAttribute(xmlReader, XMLConstants.NAME));
                    ignoreTypeInfo = Strings.isEmpty(getAttribute(xmlReader, XMLConstants.TYPE));
                    checkedAttr = true;
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));

                final boolean ignoreUnmatchedProperty = config.ignoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetClass);
                final Object result = isNullValue ? null : beanInfo.createBeanResult();
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));

                                propName = isTagByPropertyName ? xmlReader.getLocalName() : getAttribute(xmlReader, XMLConstants.NAME);
                                propInfo = beanInfo.getPropInfo(propName);

                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    continue;
                                }

                                if (propInfo == null) {
                                    if (ignoreUnmatchedProperty) {
                                        continue;
                                    } else {
                                        throw new ParseException("Unknown property element: " + propName + " for class: " + targetClass.getCanonicalName()); //NOSONAR
                                    }
                                }

                                propType = hasPropTypes ? config.getValueType(propName) : null;

                                if (propType == null) {
                                    if (propInfo.jsonXmlType.isSerializable()) {
                                        propType = propInfo.jsonXmlType;
                                    } else {
                                        attrCount = xmlReader.getAttributeCount();

                                        if (attrCount == 1) {
                                            if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                                propType = N.typeOf(xmlReader.getAttributeValue(0));
                                            }
                                        } else if (attrCount > 1) {
                                            for (int i = 0; i < attrCount; i++) {
                                                if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                    propType = N.typeOf(xmlReader.getAttributeValue(i));

                                                    break;
                                                }
                                            }
                                        }

                                        if (propType == null) {
                                            propType = propInfo.jsonXmlType;
                                        }
                                    }
                                }

                            } else {
                                if (propInfo == null || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                    for (int startCount = 1, e = xmlReader.next();; e = xmlReader.next()) {
                                        startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                        if (startCount < 0 || !xmlReader.hasNext()) {
                                            break;
                                        }
                                    }
                                } else {
                                    propValue = readByStreamParser(xmlReader, config, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo,
                                            false, inputClass, propType.clazz());

                                    for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                        startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                        if (startCount < 0 || !xmlReader.hasNext()) {
                                            break;
                                        }
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT
                                        && (isTagByPropertyName ? xmlReader.getLocalName().equals(propName)
                                                : xmlReader.getLocalName().equals(XMLConstants.PROPERTY))) {
                                    //noinspection StatementWithEmptyBody
                                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                            || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                        // ignore.
                                    } else {
                                        propInfo.setPropValue(result, propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                    propInfo = null;
                                } else {
                                    throw new ParseException("Unknown parser error at element: " + xmlReader.getLocalName());
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                }

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            propValue = (isNullValue || propInfo == null) ? null : propInfo.readPropValue(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                //noinspection StatementWithEmptyBody
                                if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                        || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                    // ignore;
                                } else {
                                    propInfo.setPropValue(result,
                                            isNullValue ? null : (propValue == null ? propType.valueOf(Strings.EMPTY_STRING) : propValue));
                                }

                                propName = null;
                                propValue = null;
                                propInfo = null;
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propName == null) {
                                return beanInfo.finishBeanResult(result);
                            } else {
                                //noinspection StatementWithEmptyBody
                                if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                        || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                    // ignore;
                                } else {
                                    propInfo.setPropValue(result,
                                            isNullValue ? null : (propValue == null ? propType.valueOf(Strings.EMPTY_STRING) : propValue));
                                }

                                propName = null;
                                propValue = null;
                                propInfo = null;
                            }

                            break;
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error"); //NOSONAR
            }

            case MAP: {
                if ((targetClass == null) || !Map.class.isAssignableFrom(targetClass)) {
                    if ((propType != null) && propType.isMap()) {
                        targetClass = propType.clazz();
                    } else {
                        targetClass = LinkedHashMap.class;
                    }
                }

                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[0].isObjectType()) {
                    keyType = propInfo.jsonXmlType.getParameterTypes()[0];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[0].isObjectType()) {
                    keyType = propType.getParameterTypes()[0];
                } else {
                    if (config.getMapKeyType() != null && !config.getMapKeyType().isObjectType()) {
                        keyType = config.getMapKeyType();
                    }
                }

                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[1].isObjectType()) {
                    valueType = propInfo.jsonXmlType.getParameterTypes()[1];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (config.getMapValueType() != null && !config.getMapValueType().isObjectType()) {
                        valueType = config.getMapValueType();
                    }
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));
                @SuppressWarnings("rawtypes")
                final Map<Object, Object> mResult = isNullValue ? null : N.newMap((Class<Map>) targetClass);
                Object key = null;
                Type<?> entryKeyType = null;
                Type<?> entryValueType = null;
                String typeAttr = null;
                boolean isStringKey = false;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // move to key element;
                            xmlReader.next();

                            isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));
                            typeAttr = getAttribute(xmlReader, XMLConstants.TYPE);
                            entryKeyType = Strings.isEmpty(typeAttr) ? keyType : N.typeOf(typeAttr);
                            isStringKey = entryKeyType.clazz().equals(String.class);

                            switch (event = xmlReader.next()) {
                                case XMLStreamConstants.START_ELEMENT:
                                    key = readByStreamParser(xmlReader, config, entryKeyType, null, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                            inputClass, entryKeyType.clazz());

                                    // end of key.
                                    xmlReader.next();

                                    break;

                                case XMLStreamConstants.CHARACTERS:
                                    text = xmlReader.getText();

                                    if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                        while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                            if (sb == null) {
                                                sb = new StringBuilder(text.length() * 2);
                                                sb.append(text);
                                            } else if (sb.isEmpty()) {
                                                sb.append(text);
                                            }

                                            sb.append(xmlReader.getText());
                                        }

                                        if (sb != null && sb.length() > text.length()) {
                                            text = sb.toString();
                                            sb.setLength(0);
                                        }
                                    }

                                    key = isNullValue ? null : entryKeyType.valueOf(text);

                                    if (event == XMLStreamConstants.CHARACTERS) {
                                        // end of key.
                                        xmlReader.next();
                                    }

                                    break;

                                case XMLStreamConstants.END_ELEMENT: {
                                    key = isNullValue ? null : (key == null ? entryKeyType.valueOf(Strings.EMPTY_STRING) : key);

                                    break;
                                }

                                default:
                                    // continue;
                            }

                            // move to value element;
                            xmlReader.next();

                            isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));
                            typeAttr = getAttribute(xmlReader, XMLConstants.TYPE);
                            entryValueType = Strings.isEmpty(typeAttr) ? valueType : N.typeOf(typeAttr);

                            if (hasPropTypes && isStringKey) {
                                final Type<?> tmpType = config.getValueType(N.toString(key));
                                if (tmpType != null) {
                                    entryValueType = tmpType;
                                }
                            }

                            switch (event = xmlReader.next()) {
                                case XMLStreamConstants.START_ELEMENT:
                                    propValue = readByStreamParser(xmlReader, config, entryValueType, null, checkedAttr, isTagByPropertyName, ignoreTypeInfo,
                                            false, inputClass, entryValueType.clazz());

                                    // end of value.
                                    xmlReader.next();

                                    break;

                                case XMLStreamConstants.CHARACTERS:
                                    text = xmlReader.getText();

                                    if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                        while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                            if (sb == null) {
                                                sb = new StringBuilder(text.length() * 2);
                                                sb.append(text);
                                            } else if (sb.isEmpty()) {
                                                sb.append(text);
                                            }

                                            sb.append(xmlReader.getText());
                                        }

                                        if (sb != null && sb.length() > text.length()) {
                                            text = sb.toString();
                                            sb.setLength(0);
                                        }
                                    }

                                    propValue = isNullValue ? null : entryValueType.valueOf(text);

                                    if (event == XMLStreamConstants.CHARACTERS) {
                                        // end of value.
                                        xmlReader.next();
                                    }

                                    break;

                                case XMLStreamConstants.END_ELEMENT: {
                                    propValue = isNullValue ? null : (propValue == null ? entryValueType.valueOf(Strings.EMPTY_STRING) : propValue);

                                    break;
                                }

                                default:
                                    // continue;
                            }

                            // end of entry.
                            xmlReader.next();

                            //noinspection StatementWithEmptyBody
                            if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                                // ignore.
                            } else {
                                mResult.put(key, propValue);
                            }

                            key = null;
                            propValue = null;

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            return (T) mResult;
                        }

                        default:
                            // continue;
                    }
                }
            }

            case ARRAY: {
                if ((targetClass == null) || !targetClass.isArray()) {
                    if ((propType != null) && propType.clazz().isArray()) {
                        targetClass = propType.clazz();
                    } else {
                        targetClass = String[].class;
                    }
                }

                Type<?> eleType = null;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = N.typeOf(propInfo.clazz.getComponentType());
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                        eleType = config.getElementType();
                    } else {
                        eleType = N.typeOf(targetClass.isArray() ? targetClass.getComponentType() : String.class);
                    }
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));
                final List<Object> list = isNullValue ? null : Objectory.createList();

                try {
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        switch (event) {
                            case XMLStreamConstants.START_ELEMENT: {
                                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));

                                switch (event = xmlReader.next()) {
                                    case XMLStreamConstants.START_ELEMENT: {
                                        list.add(readByStreamParser(xmlReader, config, eleType, null, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                                inputClass, eleType.clazz()));

                                        // end of element.
                                        xmlReader.next();

                                        break;
                                    }

                                    case XMLStreamConstants.CHARACTERS: {
                                        text = xmlReader.getText();

                                        if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                            while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                                if (sb == null) {
                                                    sb = new StringBuilder(text.length() * 2);
                                                    sb.append(text);
                                                } else if (sb.isEmpty()) {
                                                    sb.append(text);
                                                }

                                                sb.append(xmlReader.getText());
                                            }

                                            if (sb != null && sb.length() > text.length()) {
                                                text = sb.toString();
                                                sb.setLength(0);
                                            }
                                        }

                                        list.add(isNullValue ? null : eleType.valueOf(text));

                                        if (event == XMLStreamConstants.CHARACTERS) {
                                            // end of element.
                                            xmlReader.next();
                                        }

                                        break;
                                    }

                                    case XMLStreamConstants.END_ELEMENT: {
                                        list.add(isNullValue ? null : eleType.valueOf(Strings.EMPTY_STRING));

                                        break;
                                    }

                                    default:
                                        // continue;
                                }

                                break;
                            }

                            // simple array with sample format <array>[1, 2,
                            // 3...]</array>
                            case XMLStreamConstants.CHARACTERS: {
                                text = xmlReader.getText();

                                if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                    while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                        if (sb == null) {
                                            sb = new StringBuilder(text.length() * 2);
                                            sb.append(text);
                                        } else if (sb.isEmpty()) {
                                            sb.append(text);
                                        }

                                        sb.append(xmlReader.getText());
                                    }

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                                    propValue = isNullValue ? null : N.typeOf(targetClass).valueOf(text);
                                } else {
                                    propValue = isNullValue ? null : jsonParser.deserialize(text, JDC.create().setElementType(eleType.clazz()), targetClass);
                                }

                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (propValue != null) {
                                        return (T) propValue;
                                    } else {
                                        return collection2Array(list, targetClass);
                                    }
                                }

                                break;
                            }

                            case XMLStreamConstants.END_ELEMENT: {
                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return collection2Array(list, targetClass);
                                }
                            }

                            default:
                                // continue;
                        }
                    }

                } finally {
                    if (list != null) {
                        Objectory.recycle(list);
                    }
                }

                throw new ParseException("Unknown parser error");
            }

            case COLLECTION: {
                if ((targetClass == null) || !Collection.class.isAssignableFrom(targetClass)) {
                    if ((propType != null) && Collection.class.isAssignableFrom(propType.clazz())) {
                        targetClass = propType.clazz();
                    } else {
                        targetClass = List.class;
                    }
                }

                Type<?> eleType = defaultValueType;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = N.typeOf(propInfo.clazz.getComponentType());
                } else if (propType != null && propType.getParameterTypes().length == 1 && Collection.class.isAssignableFrom(propType.clazz())
                        && !propType.getParameterTypes()[0].isObjectType()) {
                    eleType = propType.getParameterTypes()[0];
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                        eleType = config.getElementType();
                    }
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));
                @SuppressWarnings("rawtypes")
                final Collection<Object> result = isNullValue ? null : N.newCollection((Class<Collection>) targetClass);

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XMLConstants.IS_NULL));

                            switch (event = xmlReader.next()) {
                                case XMLStreamConstants.START_ELEMENT: {
                                    result.add(readByStreamParser(xmlReader, config, eleType, null, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                            inputClass, eleType.clazz()));
                                    // N.println(xmlReader.getLocalName());

                                    // end of element.
                                    xmlReader.next();

                                    break;
                                }

                                case XMLStreamConstants.CHARACTERS: {
                                    text = xmlReader.getText();

                                    if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                        while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                            if (sb == null) {
                                                sb = new StringBuilder(text.length() * 2);
                                                sb.append(text);
                                            } else if (sb.isEmpty()) {
                                                sb.append(text);
                                            }

                                            sb.append(xmlReader.getText());
                                        }

                                        if (sb != null && sb.length() > text.length()) {
                                            text = sb.toString();
                                            sb.setLength(0);
                                        }
                                    }

                                    result.add(isNullValue ? null : eleType.valueOf(text));

                                    if (event == XMLStreamConstants.CHARACTERS) {
                                        // end of element.
                                        xmlReader.next();
                                    }

                                    break;
                                }

                                case XMLStreamConstants.END_ELEMENT: {
                                    result.add(isNullValue ? null : eleType.valueOf(Strings.EMPTY_STRING));

                                    break;
                                }

                                default:
                                    // continue;
                            }

                            break;
                        }

                        // simple list with sample format <list>[1, 2, 3...]</list>
                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && text.length() > TEXT_SIZE_TO_READ_MORE) {
                                while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                }

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                                propValue = isNullValue ? null : N.typeOf(targetClass).valueOf(text);
                            } else {
                                propValue = isNullValue ? null : jsonParser.deserialize(text, JDC.create().setElementType(eleType.clazz()), targetClass);
                            }

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return (T) result;
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propValue != null) {
                                return (T) propValue;
                            } else {
                                return (T) result;
                            }
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error");
            }

            default:
                throw new ParseException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only array, collection, map and bean types are supported");
        }
    }

    /**
     * Read by DOM parser.
     * @param node
     * @param config
     * @param targetClass
     *
     * @param <T>
     * @return
     */
    <T> T readByDOMParser(final Node node, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        final XMLDeserializationConfig configToUse = check(config);

        return readByDOMParser(node, configToUse, configToUse.getElementType(), false, false, false, true, targetClass);
    }

    /**
     * Read by DOM parser.
     * @param node
     * @param config
     * @param propType
     * @param checkedAttr
     * @param isTagByPropertyName
     * @param ignoreTypeInfo
     * @param isFirstCall
     * @param inputClass
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "deprecation" })
    <T> T readByDOMParser(final Node node, final XMLDeserializationConfig config, Type<?> propType, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final boolean isFirstCall, final Class<T> inputClass) {
        if (node.getNodeType() == Document.TEXT_NODE) {
            return null;
        }

        final boolean hasPropTypes = config.hasValueTypes();

        String nodeName = checkedAttr ? (isTagByPropertyName ? node.getNodeName() : XmlUtil.getAttribute(node, XMLConstants.NAME))
                : XmlUtil.getAttribute(node, XMLConstants.NAME);
        nodeName = (nodeName == null) ? node.getNodeName() : nodeName;

        Class<?> targetClass = null;

        if (isFirstCall) {
            targetClass = inputClass;
        } else {
            if (propType == null || String.class.equals(propType.clazz()) || propType.isObjectType()) {
                targetClass = hasPropTypes ? config.getValueTypeClass(nodeName, null) : null;
            } else {
                targetClass = propType.clazz();
            }

            if (targetClass == null || String.class.equals(targetClass) || Object.class.equals(targetClass)) {
                // if (isOneNode(node)) {
                // targetClass = Map.class;
                // } else {
                // targetClass = List.class;
                // }
                //
                targetClass = List.class;
            }
        }

        Class<?> typeClass = checkedAttr ? (ignoreTypeInfo ? targetClass : getConcreteClass(targetClass, node)) : getConcreteClass(targetClass, node);
        final NodeType nodeType = getNodeType(nodeName, null);

        final NodeList propNodes = node.getChildNodes();
        final int propNodeLength = getNodeLength(propNodes);
        PropInfo propInfo = null;
        Node propNode = null;
        String propName = null;
        Object propValue = null;

        switch (nodeType) {
            case ENTITY: {
                if (!ClassUtil.isBeanClass(typeClass)) {
                    if ((propType != null) && propType.isBean()) {
                        typeClass = propType.clazz();
                    } else {
                        if (ClassUtil.getSimpleClassName(inputClass).equalsIgnoreCase(nodeName)) {
                            typeClass = inputClass;
                        } else {
                            if (Collection.class.isAssignableFrom(inputClass) || Map.class.isAssignableFrom(inputClass) || inputClass.isArray()) {
                                if (propType != null) {
                                    typeClass = getClassByNodeName(nodeName, propType.clazz());
                                }
                            } else {
                                typeClass = getClassByNodeName(nodeName, inputClass);
                            }

                            checkBeanType(nodeName, inputClass, typeClass);
                        }
                    }
                }

                if (!checkedAttr) {
                    isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(node, XMLConstants.NAME));
                    ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(node, XMLConstants.TYPE));
                    checkedAttr = true;
                }

                final boolean ignoreUnmatchedProperty = config.ignoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(typeClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(typeClass);
                final Object result = beanInfo.createBeanResult();

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    propName = isTagByPropertyName ? propNode.getNodeName() : XmlUtil.getAttribute(propNode, XMLConstants.NAME); //NOSONAR
                    propInfo = beanInfo.getPropInfo(propName);

                    if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        } else {
                            throw new ParseException("Unknown property element: " + propName + " for class: " + typeClass.getCanonicalName());
                        }
                    }

                    propType = hasPropTypes ? config.getValueType(propName) : null;

                    if (propType == null) {
                        if (propInfo.jsonXmlType.isSerializable()) {
                            propType = propInfo.jsonXmlType;
                        } else {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType : N.typeOf(getConcreteClass(propInfo.jsonXmlType.clazz(), propNode));
                        }
                    }

                    if (XmlUtil.isTextElement(propNode)) {
                        propValue = getPropValue(propName, propType, propInfo, propNode);
                    } else {
                        //noinspection ConstantValue
                        propValue = readByDOMParser(checkOneNode(propNode), config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputClass);
                    }

                    if (propInfo.jsonXmlExpose != JsonXmlField.Expose.SERIALIZE_ONLY) {
                        propInfo.setPropValue(result, propValue);
                    }
                }

                return beanInfo.finishBeanResult(result);
            }

            case MAP: {
                if ((typeClass == null) || !Map.class.isAssignableFrom(typeClass)) {
                    if ((propType != null) && propType.isMap()) {
                        typeClass = propType.clazz();
                    } else {
                        typeClass = LinkedHashMap.class;
                    }
                }

                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[0].isObjectType()) {
                    keyType = propType.getParameterTypes()[0];
                } else {
                    if (config.getMapKeyType() != null && !config.getMapKeyType().isObjectType()) {
                        keyType = config.getMapKeyType();
                    }
                }

                final boolean isStringKey = keyType.clazz() == String.class;

                Type<?> valueType = defaultValueType;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (config.getMapValueType() != null && !config.getMapValueType().isObjectType()) {
                        valueType = config.getMapValueType();
                    }
                }

                final Map<Object, Object> mResult = newPropInstance(typeClass, node);

                final NodeList entryNodes = node.getChildNodes();
                Node entryNode = null;
                NodeList subEntryNodes = null;
                Node propKeyNode = null;
                Node propValueNode = null;
                Class<?> propKeyClass = null;
                Class<?> propValueClass = null;
                Type<?> propKeyType = null;
                Type<?> propValueType = null;
                Object propKey = null;

                for (int k = 0; k < entryNodes.getLength(); k++) {
                    entryNode = entryNodes.item(k);

                    if (entryNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    subEntryNodes = entryNode.getChildNodes();

                    int index = 0;

                    for (; index < subEntryNodes.getLength(); index++) {
                        propKeyNode = subEntryNodes.item(index);

                        if (propKeyNode.getNodeType() == Document.ELEMENT_NODE) {
                            index++;

                            break;
                        }
                    }

                    for (; index < subEntryNodes.getLength(); index++) {
                        propValueNode = subEntryNodes.item(index);

                        if (propValueNode.getNodeType() == Document.ELEMENT_NODE) {
                            break;
                        }
                    }

                    propKeyClass = checkedAttr ? (ignoreTypeInfo ? keyType.clazz() : getConcreteClass(keyType.clazz(), propKeyNode))
                            : getConcreteClass(keyType.clazz(), propKeyNode);

                    if (propKeyClass == Object.class) {
                        propKeyClass = String.class;
                    }

                    propKeyType = propKeyClass == keyType.clazz() ? keyType : N.typeOf(propKeyClass);

                    //noinspection DataFlowIssue
                    if (XmlUtil.isTextElement(propKeyNode)) {
                        //noinspection ConstantValue
                        propKey = getPropValue(XMLConstants.KEY, propKeyType, propInfo, propKeyNode);
                    } else {
                        propKey = readByDOMParser(checkOneNode(propKeyNode), config, keyType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputClass);
                    }

                    //noinspection SuspiciousMethodCalls
                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propKey)) {
                        continue;
                    }

                    propValueType = hasPropTypes && isStringKey ? config.getValueType(N.toString(propKey)) : null;

                    if (propValueType == null) {
                        propValueClass = checkedAttr ? (ignoreTypeInfo ? valueType.clazz() : getConcreteClass(valueType.clazz(), propValueNode))
                                : getConcreteClass(valueType.clazz(), propValueNode);
                    } else {
                        propValueClass = propValueType.clazz();
                    }

                    if (propValueClass == Object.class) {
                        propValueClass = String.class;
                    }

                    propValueType = propValueClass == valueType.clazz() ? valueType : N.typeOf(propValueClass);

                    //noinspection DataFlowIssue
                    if (XmlUtil.isTextElement(propValueNode)) {
                        //noinspection ConstantValue
                        propValue = getPropValue(XMLConstants.VALUE, propValueType, propInfo, propValueNode);
                    } else {
                        propValue = readByDOMParser(checkOneNode(propValueNode), config, propValueType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputClass);
                    }

                    mResult.put(propKey, propValue);
                }

                return (T) mResult;
            }

            case ARRAY: { //NOSONAR
                if ((typeClass == null) || !typeClass.isArray()) {
                    if ((propType != null) && propType.clazz().isArray()) {
                        typeClass = propType.clazz();
                    } else {
                        typeClass = String[].class;
                    }
                }

                Type<?> eleType = null;
                Class<?> propClass = null;

                if (propType != null && (propType.isArray() || propType.isCollection()) && propType.getElementType() != null
                        && !propType.getElementType().isObjectType()) {
                    eleType = propType.getElementType();
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                        eleType = config.getElementType();
                    } else {
                        eleType = N.typeOf(typeClass.isArray() ? typeClass.getComponentType() : Object.class);
                    }
                }

                propName = XMLConstants.E; //NOSONAR

                if (XmlUtil.isTextElement(node)) {
                    final String st = XmlUtil.getTextContent(node);

                    if (Strings.isEmpty(st)) {
                        return N.newArray(eleType.clazz(), 0);
                    } else {
                        return (T) N.valueOf(st, typeClass);
                    }
                } else {
                    final List<Object> c = Objectory.createList();

                    try {
                        final NodeList eleNodes = node.getChildNodes();
                        Node eleNode = null;

                        for (int k = 0; k < eleNodes.getLength(); k++) {
                            eleNode = eleNodes.item(k);

                            if (eleNode.getNodeType() == Document.TEXT_NODE) {
                                continue;
                            }

                            propClass = checkedAttr ? (ignoreTypeInfo ? eleType.clazz() : getConcreteClass(eleType.clazz(), eleNode))
                                    : getConcreteClass(eleType.clazz(), eleNode);

                            if (propClass == Object.class) {
                                propClass = String.class;
                            }

                            propType = propClass == eleType.clazz() ? eleType : N.typeOf(propClass);

                            if (XmlUtil.isTextElement(eleNode)) {
                                //noinspection ConstantValue
                                c.add(getPropValue(propName, propType, propInfo, eleNode));
                            } else {
                                c.add(readByDOMParser(checkOneNode(eleNode), config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                        inputClass));
                            }
                        }

                        return collection2Array(c, typeClass);
                    } finally {
                        Objectory.recycle(c);
                    }
                }
            }

            case COLLECTION: {
                if ((typeClass == null) || !Collection.class.isAssignableFrom(typeClass)) {
                    if ((propType != null) && Collection.class.isAssignableFrom(propType.clazz())) {
                        typeClass = propType.clazz();
                    } else {
                        typeClass = List.class;
                    }
                }

                Type<?> eleType = null;
                Class<?> propClass = null;

                if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.getElementType().isObjectType()) {
                    eleType = propType.getElementType();
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                        eleType = config.getElementType();
                    } else {
                        eleType = objType;
                    }
                }

                propName = XMLConstants.E; //NOSONAR

                final Collection<Object> result = newPropInstance(typeClass, node);

                final NodeList eleNodes = node.getChildNodes();
                Node eleNode = null;

                for (int k = 0; k < eleNodes.getLength(); k++) {
                    eleNode = eleNodes.item(k);

                    if (eleNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    propClass = checkedAttr ? (ignoreTypeInfo ? eleType.clazz() : getConcreteClass(eleType.clazz(), eleNode))
                            : getConcreteClass(eleType.clazz(), eleNode);

                    if (propClass == Object.class) {
                        propClass = String.class;
                    }

                    propType = propClass == eleType.clazz() ? eleType : N.typeOf(propClass);

                    if (XmlUtil.isTextElement(eleNode)) {
                        //noinspection ConstantValue
                        result.add(getPropValue(propName, propType, propInfo, eleNode));
                    } else {
                        result.add(
                                readByDOMParser(checkOneNode(eleNode), config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, inputClass));
                    }
                }

                return (T) result;
            }

            default:
                throw new ParseException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only array, collection, map and bean types are supported");
        }
    }

    private static void checkBeanType(final String nodeName, final Class<?> inputClass, final Class<?> targetClass) {
        if (!ClassUtil.isBeanClass(targetClass)) {
            throw new ParseException("No bean class found by node name : " + nodeName + " in package of class: " + inputClass.getCanonicalName());
        }
    }

    /**
     * Gets the node type.
     *
     * @param nodeName
     * @param previousNodeType
     * @return
     */
    private static NodeType getNodeType(final String nodeName, final NodeType previousNodeType) {
        if (previousNodeType == NodeType.ENTITY) {
            return NodeType.PROPERTY;
        }

        final NodeType nodeType = nodeTypePool.get(nodeName);

        if (nodeType == null) {
            return NodeType.ENTITY;
        }

        return nodeType;
    }

    /**
     * Gets the class by node name.
     * @param nodeName
     * @param cls
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "deprecation", "null" })
    private static <T> Class<T> getClassByNodeName(final String nodeName, final Class<?> cls) {
        if (cls == null) {
            return null;
        }

        Class<?> nodeClass = null;
        Map<String, Class<?>> nodeNameClassMap = nodeNameClassMapPool.get(cls);

        if (nodeNameClassMap == null) {
            nodeNameClassMap = new ConcurrentHashMap<>();
            nodeNameClassMapPool.put(cls, nodeNameClassMap);
        } else {
            nodeClass = nodeNameClassMap.get(nodeName);
        }

        if (nodeClass == null) {
            String packName = null;

            if (cls.getPackage() == null || cls.getPackage().getName().startsWith("java.lang") || cls.getPackage().getName().startsWith("java.util")) {
                final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                final String xmlUtilPackageName = AbacusXMLParserImpl.class.getPackage().getName();
                String className = null;

                for (int i = stackTrace.length - 1; i >= 0; i--) {
                    className = stackTrace[i].getClassName();

                    if (!(className.startsWith("java.lang") || className.startsWith("java.util") || className.startsWith(xmlUtilPackageName))) {
                        packName = ClassUtil.forClass(className).getPackage().getName();

                        break;
                    }
                }
            } else {
                packName = cls.getPackage().getName();
            }

            if (Strings.isEmpty(packName)) {
                return null;
            }

            final String[] tokens = packName.split("\\.");

            // search the bean class under package:
            // com.companyName.componentName
            if (tokens.length > 3) {
                packName = Strings.join(tokens, 0, 3, ".");
            }

            final List<Class<?>> classList = ClassUtil.getClassesByPackage(packName, true, true);

            for (final Class<?> e : classList) {
                if (ClassUtil.getSimpleClassName(e).equalsIgnoreCase(nodeName)) {
                    nodeClass = e;

                    break;
                }
            }

            if ((nodeClass == null) && !nodeName.equalsIgnoreCase(ClassUtil.formalizePropName(nodeName))) {
                nodeClass = getClassByNodeName(ClassUtil.formalizePropName(nodeName), cls);
            }

            if (nodeClass == null) {
                nodeClass = ClassUtil.CLASS_MASK;
            }

            nodeNameClassMap.put(nodeName, nodeClass);
        }

        return (Class<T>) ((nodeClass == ClassUtil.CLASS_MASK) ? null : nodeClass);
    }

    /**
     * Gets the xml SAX handler.
     * @param config
     * @param nodeClasses
     * @param targetClass
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> XmlSAXHandler<T> getXmlSAXHandler(final XMLDeserializationConfig config, final Map<String, Class<?>> nodeClasses,
            final Class<? extends T> targetClass) {
        XmlSAXHandler<T> xmlSAXHandler = (XmlSAXHandler<T>) xmlSAXHandlerPool.poll();

        if (xmlSAXHandler == null) {
            xmlSAXHandler = new XmlSAXHandler<>();
        }

        xmlSAXHandler.nodeClasses = nodeClasses;
        xmlSAXHandler.inputClass = (Class<T>) targetClass;
        xmlSAXHandler.setConfig(config);

        return xmlSAXHandler;
    }

    /**
     *
     * @param xmlSAXHandler
     */
    private static void recycle(final XmlSAXHandler<?> xmlSAXHandler) {
        if (xmlSAXHandler == null) {
            return;
        }

        synchronized (xmlSAXHandlerPool) {
            if (xmlSAXHandlerPool.size() < POOL_SIZE) {
                xmlSAXHandler.reset();
                xmlSAXHandlerPool.add(xmlSAXHandler);
            }
        }
    }

    /**
     * The Class XmlSAXHandler.
     *
     * @param <T>
     */
    static final class XmlSAXHandler<T> extends DefaultHandler { // NOSONAR

        private final Holder<T> resultHolder = new Holder<>();

        private StringBuilder sb = null;

        private Map<String, Class<?>> nodeClasses;

        private Class<T> inputClass;

        private XMLDeserializationConfig config;

        private boolean hasPropTypes = false;

        private boolean ignoreUnmatchedProperty;

        private Collection<String> mapIgnoredPropNames;

        private BeanInfo beanInfo;

        private PropInfo propInfo;

        private final List<String> beanOrPropNameQueue = new ArrayList<>();

        private final List<NodeType> nodeTypeQueue = new ArrayList<>();

        private final List<Object> nodeValueQueue = new ArrayList<>();

        private final List<Object> keyQueue = new ArrayList<>();

        // ...
        private String nodeName;

        private String beanOrPropName;

        private Collection<String> ignoredClassPropNames;

        private Object bean;

        private Class<?> beanClass;

        private Object array;

        private Collection<Object> coll;

        private Map<Object, Object> map;

        private Object eleValue;

        private Class<?> targetClass;

        private Class<?> typeClass;

        private Type<?> propType;

        private Type<?> eleType;

        private Type<?> keyType;

        private Type<?> valueType;

        private final List<Type<?>> eleTypeQueue = new ArrayList<>();

        private final List<Type<?>> keyTypeQueue = new ArrayList<>();

        private final List<Type<?>> valueTypeQueue = new ArrayList<>();

        private final IdentityHashMap<Object, BeanInfo> beanInfoQueue = new IdentityHashMap<>(1);

        private boolean isNull = false;

        private boolean checkedPropNameTag = false;

        private boolean checkedTypeInfo = false;

        private boolean isTagByPropertyName = false;

        private boolean ignoreTypeInfo = false;

        private boolean isFirstCall = true;

        private int inIgnorePropRefCount = 0;

        @Override
        @SuppressWarnings("unchecked")
        public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes attrs) {
            if (inIgnorePropRefCount > 0) {
                inIgnorePropRefCount++;

                return;
            }

            if (sb == null) {
                sb = Objectory.createStringBuilder();
            } else {
                sb.setLength(0);
            }

            nodeName = qName;
            if (checkedPropNameTag) {
                beanOrPropName = isTagByPropertyName || attrs == null || attrs.getLength() == 0 ? nodeName : attrs.getValue(XMLConstants.NAME);
            } else {
                beanOrPropName = attrs.getValue(XMLConstants.NAME);
                if (beanOrPropName == null) {
                    beanOrPropName = nodeName;
                }
            }

            if (isFirstCall) {
                if ((nodeClasses != null) && (inputClass == null)) {
                    inputClass = (Class<T>) nodeClasses.get(beanOrPropName);
                }

                if (inputClass == null) {
                    throw new ParseException("No input class found for node: " + nodeName);
                }

                targetClass = inputClass;
            } else {
                if (propType == null || String.class.equals(propType.clazz()) || propType.isObjectType()) {
                    targetClass = hasPropTypes ? config.getValueTypeClass(nodeName, null) : null;
                } else {
                    targetClass = propType.clazz();
                }
            }

            if (targetClass == null) {
                typeClass = null;
            } else {
                typeClass = checkedTypeInfo ? ((ignoreTypeInfo || attrs == null || attrs.getLength() == 0) ? targetClass : getConcreteClass(targetClass, attrs))
                        : ((attrs == null || attrs.getLength() == 0) ? targetClass : getConcreteClass(targetClass, attrs));
            }

            final NodeType previousNodeType = (nodeTypeQueue.isEmpty()) ? null : nodeTypeQueue.get(nodeTypeQueue.size() - 1);
            final NodeType nodeType = getNodeType(nodeName, previousNodeType);

            isNull = attrs != null && attrs.getLength() != 0 && Boolean.parseBoolean(attrs.getValue(XMLConstants.IS_NULL));

            switch (nodeType) {
                case ENTITY: {

                    if (!checkedPropNameTag) {
                        isTagByPropertyName = (attrs == null) || (Strings.isEmpty(attrs.getValue(XMLConstants.NAME)));
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XMLConstants.TYPE)));
                        checkedPropNameTag = true;
                        checkedTypeInfo = true;
                    }

                    if (!isTagByPropertyName) {
                        //noinspection DataFlowIssue
                        beanOrPropName = attrs.getValue(XMLConstants.NAME);
                        beanOrPropNameQueue.add(beanOrPropName);
                    }

                    typeClass = hasPropTypes ? config.getValueTypeClass(beanOrPropName, typeClass) : typeClass;

                    if (typeClass == null || !N.typeOf(typeClass).isBean()) {
                        if ((eleType != null) && ClassUtil.isBeanClass(eleType.clazz())) {
                            typeClass = eleType.clazz();
                        } else {
                            if (ClassUtil.getSimpleClassName(inputClass).equalsIgnoreCase(beanOrPropName)) {
                                typeClass = inputClass;
                            } else {
                                if (Collection.class.isAssignableFrom(inputClass) || Map.class.isAssignableFrom(inputClass) || inputClass.isArray()) {
                                    if (config.getElementType() != null) {
                                        typeClass = getClassByNodeName(beanOrPropName, config.getElementType().clazz());
                                    }
                                } else {
                                    typeClass = getClassByNodeName(beanOrPropName, inputClass);
                                }

                                checkBeanType(nodeName, inputClass, typeClass);
                            }
                        }
                    }

                    beanClass = typeClass;
                    beanInfo = ParserUtil.getBeanInfo(beanClass);

                    bean = beanInfo.createBeanResult();
                    nodeValueQueue.add(bean);

                    beanInfoQueue.put(bean, beanInfo);

                    if (isFirstCall) {
                        resultHolder.setValue((T) bean);
                        isFirstCall = false;
                    }

                    propInfo = null;
                    propType = null;

                    break;
                }

                case MAP: {

                    if (!checkedTypeInfo) {
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XMLConstants.TYPE)));
                        checkedTypeInfo = true;
                    }

                    if (typeClass == null || !N.typeOf(typeClass).isMap()) {
                        if ((eleType != null) && Map.class.isAssignableFrom(eleType.clazz())) {
                            typeClass = eleType.clazz();
                        } else {
                            typeClass = LinkedHashMap.class;
                        }
                    }

                    if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2
                            && !propInfo.jsonXmlType.getParameterTypes()[0].isObjectType()) {
                        keyType = propInfo.jsonXmlType.getParameterTypes()[0];
                    } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                            && !propType.getParameterTypes()[0].isObjectType()) {
                        keyType = propType.getParameterTypes()[0];
                    } else {
                        if (config.getMapKeyType() != null && !config.getMapKeyType().isObjectType()) {
                            keyType = config.getMapKeyType();
                        } else {
                            keyType = defaultKeyType;
                        }
                    }

                    if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2
                            && !propInfo.jsonXmlType.getParameterTypes()[1].isObjectType()) {
                        valueType = propInfo.jsonXmlType.getParameterTypes()[1];
                    } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                            && !propType.getParameterTypes()[1].isObjectType()) {
                        valueType = propType.getParameterTypes()[1];
                    } else {
                        if (config.getMapValueType() != null && !config.getMapValueType().isObjectType()) {
                            valueType = config.getMapValueType();
                        } else {
                            valueType = defaultValueType;
                        }
                    }

                    keyTypeQueue.add(keyType);
                    valueTypeQueue.add(valueType);

                    map = newPropInstance(typeClass, attrs);
                    nodeValueQueue.add(map);

                    if (isFirstCall) {
                        resultHolder.setValue((T) map);
                        isFirstCall = false;
                    }

                    propInfo = null;
                    propType = null;

                    break;
                }

                case ARRAY: {

                    if (!checkedTypeInfo) {
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XMLConstants.TYPE)));
                        checkedTypeInfo = true;
                    }

                    if (typeClass == null || !N.typeOf(typeClass).isArray()) {
                        if ((eleType != null) && eleType.clazz().isArray()) {
                            typeClass = eleType.clazz();
                        } else {
                            typeClass = String[].class;
                        }
                    }

                    if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                        eleType = N.typeOf(propInfo.clazz.getComponentType());
                    } else {
                        if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                            eleType = config.getElementType();
                        } else {
                            eleType = N.typeOf(
                                    typeClass.isArray() && !Object.class.equals(typeClass.getComponentType()) ? typeClass.getComponentType() : String.class);
                        }
                    }

                    eleTypeQueue.add(eleType);

                    array = N.newArray(eleType.clazz(), 0);
                    nodeValueQueue.add(array);

                    coll = new ArrayList<>();
                    nodeValueQueue.add(coll);

                    if (isFirstCall) {
                        // resultHolder.setObject((T) array);
                        isFirstCall = false;
                    }

                    propInfo = null;
                    propType = null;

                    break;
                }

                case COLLECTION: {

                    if (!checkedTypeInfo) {
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XMLConstants.TYPE)));
                        checkedTypeInfo = true;
                    }

                    if (typeClass == null || !N.typeOf(typeClass).isCollection()) {
                        if ((eleType != null) && Collection.class.isAssignableFrom(eleType.clazz())) {
                            typeClass = eleType.clazz();
                        } else {
                            typeClass = List.class;
                        }
                    }

                    if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 1
                            && !propInfo.jsonXmlType.getParameterTypes()[0].isObjectType()) {
                        eleType = propInfo.jsonXmlType.getParameterTypes()[0];
                    } else if (propType != null && propType.getParameterTypes().length == 1 && Collection.class.isAssignableFrom(propType.clazz())
                            && !propType.getParameterTypes()[0].isObjectType()) {
                        eleType = propType.getParameterTypes()[0];
                    } else {
                        if (config.getElementType() != null && !config.getElementType().isObjectType()) {
                            eleType = config.getElementType();
                        } else {
                            eleType = defaultValueType;
                        }
                    }

                    eleTypeQueue.add(eleType);

                    coll = newPropInstance(typeClass, attrs);
                    nodeValueQueue.add(coll);

                    if (isFirstCall) {
                        resultHolder.setValue((T) coll);
                        isFirstCall = false;
                    }

                    propInfo = null;
                    propType = null;

                    break;
                }

                case PROPERTY: {
                    if (!isTagByPropertyName) {
                        //noinspection DataFlowIssue
                        beanOrPropName = attrs.getValue(XMLConstants.NAME);
                        beanOrPropNameQueue.add(beanOrPropName);
                    }

                    propInfo = beanInfo.getPropInfo(beanOrPropName);
                    ignoredClassPropNames = config.getIgnoredPropNames(beanClass);

                    if (N.notEmpty(ignoredClassPropNames) && ignoredClassPropNames.contains(beanOrPropName)) {
                        inIgnorePropRefCount = 1;

                        break;
                    }

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            break;
                        } else {
                            throw new ParseException("Unknown property element: " + beanOrPropName + " for class: " + beanClass.getCanonicalName());
                        }
                    }

                    if (hasPropTypes) {
                        propType = config.getValueType(beanOrPropName);

                        if (propType == null) {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType : N.typeOf(getConcreteClass(propInfo.clazz, attrs));
                        }
                    } else {
                        propType = ignoreTypeInfo ? propInfo.jsonXmlType : N.typeOf(getConcreteClass(propInfo.clazz, attrs));
                    }

                    if ((propType == null) || propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    break;
                }

                case ELEMENT: {

                    propType = ignoreTypeInfo ? eleType : N.typeOf(getConcreteClass(eleType.clazz(), attrs));

                    if ((propType == null) || propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    break;
                }

                case KEY: {

                    propType = ignoreTypeInfo ? keyType : N.typeOf(getConcreteClass(keyType.clazz(), attrs));

                    if ((propType == null) || propType.clazz() == Object.class) {
                        propType = defaultKeyType;
                    }

                    break;
                }

                case VALUE: {
                    if (hasPropTypes) {
                        final Object key = keyQueue.get(keyQueue.size() - 1);
                        if (key != null && key.getClass() == String.class) {
                            propType = config.getValueType((String) key);

                            if (propType == null) {
                                propType = ignoreTypeInfo ? valueType : N.typeOf(getConcreteClass(valueType.clazz(), attrs));
                            }
                        } else {
                            propType = ignoreTypeInfo ? valueType : N.typeOf(getConcreteClass(valueType.clazz(), attrs));
                        }
                    } else {
                        propType = ignoreTypeInfo ? valueType : N.typeOf(getConcreteClass(valueType.clazz(), attrs));
                    }

                    if ((propType == null) || propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    break;
                }

                case ENTRY: {

                    break;
                }

                default:
                    throw new ParseException("only array, collection, map and bean nodes are supported"); //NOSONAR
            }

            if (isFirstCall) {
                throw new ParseException("only array, collection, map and bean nodes are supported");
            }

            nodeTypeQueue.add(nodeType);
        }

        @Override
        @SuppressWarnings({ "unchecked" })
        public void endElement(final String namespaceURI, final String localName, final String qName) {
            if (inIgnorePropRefCount > 1) {
                inIgnorePropRefCount--;

                return;
            }

            nodeName = qName;
            beanOrPropName = nodeName;

            final NodeType nodeType = nodeTypeQueue.remove(nodeTypeQueue.size() - 1);

            switch (nodeType) {
                case ENTITY: {

                    if (!isTagByPropertyName) {
                        beanOrPropName = beanOrPropNameQueue.remove(beanOrPropNameQueue.size() - 1);
                    }

                    popupNodeValue();

                    break;
                }

                case ARRAY: {

                    if (!coll.isEmpty()) {
                        array = collection2Array(coll, nodeValueQueue.get(nodeValueQueue.size() - 2).getClass());
                    } else if (!sb.isEmpty()) {
                        array = N.valueOf(sb.toString(), typeClass);
                    }

                    if (nodeTypeQueue.isEmpty()) {
                        resultHolder.setValue((T) array);
                    }

                    array = null;

                    nodeValueQueue.remove(nodeValueQueue.size() - 1);

                    popupNodeValue();

                    break;
                }

                case COLLECTION, MAP: {

                    popupNodeValue();

                    break;
                }

                case PROPERTY: {
                    if (inIgnorePropRefCount == 1) {
                        inIgnorePropRefCount--;

                        eleValue = null;
                        propInfo = null;
                        propType = null;

                        break;
                    }

                    if (!isTagByPropertyName) {
                        beanOrPropName = beanOrPropNameQueue.remove(beanOrPropNameQueue.size() - 1);
                    }

                    propInfo = beanInfo.getPropInfo(beanOrPropName);

                    // for propInfo is null if it's unknown property
                    if (propInfo != null && propInfo.jsonXmlExpose != JsonXmlField.Expose.SERIALIZE_ONLY) {
                        if (eleValue == null) {
                            if (isNull) {
                                propInfo.setPropValue(bean, null);
                            } else {
                                propInfo.setPropValue(bean, propInfo.readPropValue(sb.toString()));
                            }
                        } else {
                            propInfo.setPropValue(bean, eleValue);

                            eleValue = null;
                        }
                    }

                    propInfo = null;
                    propType = null;

                    break;
                }

                case ELEMENT: {

                    if (eleValue == null) {
                        if (isNull) {
                            coll.add(null);
                        } else {
                            coll.add(propType.valueOf(sb.toString()));
                        }
                    } else {
                        coll.add(eleValue);
                        eleValue = null;
                    }

                    propType = null;

                    break;
                }

                case KEY: {

                    if (eleValue == null) {
                        if (isNull) {
                            keyQueue.add(null);
                        } else {
                            keyQueue.add(propType.valueOf(sb.toString()));
                        }
                    } else {
                        keyQueue.add(eleValue);
                        eleValue = null;
                    }

                    propType = null;

                    if (mapIgnoredPropNames != null) {
                        final Object latestKey = keyQueue.get(keyQueue.size() - 1);
                        //noinspection SuspiciousMethodCalls
                        if (latestKey != null && mapIgnoredPropNames.contains(latestKey)) {
                            inIgnorePropRefCount = 1;
                        }
                    }

                    break;
                }

                case VALUE: {
                    if (inIgnorePropRefCount == 1) {
                        inIgnorePropRefCount--;

                        eleValue = null;
                        propType = null;

                        break;
                    }

                    if (eleValue == null) {
                        if (isNull) {
                            map.put(keyQueue.remove(keyQueue.size() - 1), null);
                        } else {
                            map.put(keyQueue.remove(keyQueue.size() - 1), propType.valueOf(sb.toString()));
                        }
                    } else {
                        map.put(keyQueue.remove(keyQueue.size() - 1), eleValue);
                        eleValue = null;
                    }

                    propType = null;

                    break;
                }

                case ENTRY:

                    break;

                default:
                    throw new ParseException("only array, collection, map and bean nodes are supported");
            }
        }

        @Override
        public void characters(final char[] buffer, final int offset, final int count) {
            //noinspection StatementWithEmptyBody
            if (inIgnorePropRefCount > 0) {
                // ignore.
            } else {
                sb.append(buffer, offset, count);
            }
        }

        @SuppressWarnings("unchecked")
        private void popupNodeValue() {
            eleValue = nodeValueQueue.remove(nodeValueQueue.size() - 1);
            beanInfo = beanInfoQueue.remove(eleValue);

            if (beanInfo != null) {
                beanClass = beanInfo.clazz;

                if (resultHolder.value() == bean) {
                    bean = beanInfo.finishBeanResult(bean);
                    resultHolder.setValue((T) bean);
                } else {
                    bean = beanInfo.finishBeanResult(bean);
                }
            } else if (eleValue instanceof Map) {
                keyTypeQueue.remove(keyTypeQueue.size() - 1);
                valueTypeQueue.remove(valueTypeQueue.size() - 1);
            } else if (eleValue.getClass().isArray() || eleValue instanceof Collection) {
                eleTypeQueue.remove(eleTypeQueue.size() - 1);
            }

            if (!nodeValueQueue.isEmpty()) {
                final Object next = nodeValueQueue.get(nodeValueQueue.size() - 1);
                beanInfo = beanInfoQueue.get(next);

                if (beanInfo != null) {
                    bean = next;
                    beanClass = beanInfo.clazz;
                } else {
                    typeClass = next.getClass();

                    if (next instanceof Collection) {
                        coll = ((Collection<Object>) next);

                        eleType = eleTypeQueue.get(eleTypeQueue.size() - 1);
                        // Should not happen
                        // } else if (next.getClass().isArray()) {
                        //
                        // eleType = eleTypeQueue.get(eleTypeQueue.size() - 1);
                    } else if (next instanceof Map) {
                        map = ((Map<Object, Object>) next);

                        keyType = keyTypeQueue.get(keyTypeQueue.size() - 1);
                        valueType = valueTypeQueue.get(valueTypeQueue.size() - 1);
                    }
                }
            }
        }

        private void setConfig(final XMLDeserializationConfig config) {
            this.config = config;
            hasPropTypes = config.hasValueTypes();
            ignoreUnmatchedProperty = config.ignoreUnmatchedProperty();
            mapIgnoredPropNames = config.getIgnoredPropNames(Map.class);
        }

        private void reset() {
            resultHolder.setValue(null);
            Objectory.recycle(sb);
            sb = null;

            // ...
            nodeClasses = null;
            inputClass = null;
            config = null;

            // ...
            hasPropTypes = false;
            ignoreUnmatchedProperty = false;
            mapIgnoredPropNames = null;

            // ...
            beanInfo = null;
            propInfo = null;

            beanOrPropNameQueue.clear();
            nodeTypeQueue.clear();
            nodeValueQueue.clear();
            keyQueue.clear();

            nodeName = null;
            beanOrPropName = null;
            ignoredClassPropNames = null;
            bean = null;
            beanClass = null;
            array = null;
            coll = null;
            map = null;
            eleValue = null;
            targetClass = null;
            typeClass = null;
            eleType = null;
            propType = null;
            keyType = null;
            valueType = null;
            eleTypeQueue.clear();
            keyTypeQueue.clear();
            valueTypeQueue.clear();
            beanInfoQueue.clear();

            // ...
            isNull = false;
            checkedPropNameTag = false;
            checkedTypeInfo = false;
            isTagByPropertyName = false;
            ignoreTypeInfo = false;
            isFirstCall = true;
            inIgnorePropRefCount = 0;
        }
    }
}
