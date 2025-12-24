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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.XmlUtil;

/**
 * Implementation of the XMLParser interface providing XML serialization and deserialization capabilities.
 * This parser supports both StAX (Streaming API for XML) and DOM (Document Object Model) parsing modes.
 * 
 * <p>The parser handles various Java types including:
 * <ul>
 *   <li>Primitive types and their wrappers</li>
 *   <li>Arrays and Collections</li>
 *   <li>Maps and MapEntity objects</li>
 *   <li>JavaBeans with getter/setter methods</li>
 * </ul>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Circular reference detection and handling</li>
 *   <li>Type information preservation</li>
 *   <li>Pretty-printing support</li>
 *   <li>Flexible property naming policies</li>
 *   <li>Ignoring specific properties during serialization/deserialization</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
 * 
 * // Serialize object to XML
 * MyBean bean = new MyBean();
 * String xml = parser.serialize(bean);
 * 
 * // Deserialize XML to object
 * MyBean restored = parser.deserialize(xml, MyBean.class);
 * 
 * // With configuration
 * XMLSerializationConfig config = new XMLSerializationConfig()
 *     .prettyFormat(true)
 *     .writeTypeInfo(false);
 * String xmlWithConfig = parser.serialize(bean, config);
 * }</pre>
 * 
 * @see XMLParser
 * @see XMLSerializationConfig
 * @see XMLDeserializationConfig
 */
final class XMLParserImpl extends AbstractXMLParser {

    private final XMLParserType parserType;

    /**
     * Constructs a new XMLParserImpl with the specified parser type.
     * 
     * @param parserType the type of XML parser to use (StAX or DOM)
     */
    XMLParserImpl(final XMLParserType parserType) {
        this.parserType = parserType;
    }

    /**
     * Constructs a new XMLParserImpl with the specified parser type and configurations.
     * 
     * @param parserType the type of XML parser to use (StAX or DOM)
     * @param xsc the XML serialization configuration
     * @param xdc the XML deserialization configuration
     */
    XMLParserImpl(final XMLParserType parserType, final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        super(xsc, xdc);
        this.parserType = parserType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses StAX (Streaming API for XML) for serialization, providing efficient
     * memory usage and good performance for objects of any size. The serialization process converts
     * Java objects to XML string format following the configured serialization settings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * // Simple serialization
     * User user = new User("John", "Doe");
     * String xml = parser.serialize(user, null);
     *
     * // With configuration
     * XMLSerializationConfig config = new XMLSerializationConfig()
     *     .prettyFormat(true)
     *     .writeTypeInfo(false);
     * String prettyXml = parser.serialize(user, config);
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @return the XML string representation; returns empty string if {@code obj} is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during serialization
     * @throws ParseException if the object type is not supported for serialization
     */
    @Override
    public String serialize(final Object obj, final XMLSerializationConfig config) {
        if (obj == null) {
            return Strings.EMPTY;
        }

        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();
        final IdentityHashSet<Object> serializedObjects = config == null || !config.supportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, config, null, serializedObjects, bw, false);

            return bw.toString();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation writes the XML output to a file using StAX serialization. The file is created
     * if it doesn't exist, and the contents are flushed and properly closed after serialization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     * User user = new User("Jane", "Smith");
     * File outputFile = new File("user.xml");
     *
     * XMLSerializationConfig config = new XMLSerializationConfig()
     *     .prettyFormat(true);
     * parser.serialize(user, config, outputFile);
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @param output the file to write the XML content to; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during file operations
     * @throws ParseException if the object type is not supported for serialization
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
     * {@inheritDoc}
     *
     * <p>This implementation writes the XML output to an output stream using StAX serialization. The stream
     * is flushed after serialization but is not closed, allowing the caller to manage the stream lifecycle.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     * User user = new User("Bob", "Johnson");
     *
     * try (OutputStream out = new FileOutputStream("user.xml")) {
     *     XMLSerializationConfig config = new XMLSerializationConfig()
     *         .prettyFormat(true);
     *     parser.serialize(user, config, out);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @param output the output stream to write the XML content to; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during stream operations
     * @throws ParseException if the object type is not supported for serialization
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final OutputStream output) {
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(output);
        final IdentityHashSet<Object> serializedObjects = config == null || !config.supportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, config, null, serializedObjects, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation writes the XML output to a writer using StAX serialization. The writer
     * is flushed after serialization but is not closed, allowing the caller to manage the writer lifecycle.
     * If the provided writer is already a {@link BufferedXMLWriter}, it is used directly for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     * User user = new User("Alice", "Williams");
     *
     * StringWriter sw = new StringWriter();
     * XMLSerializationConfig config = new XMLSerializationConfig()
     *     .prettyFormat(true)
     *     .writeTypeInfo(true);
     * parser.serialize(user, config, sw);
     * String xml = sw.toString();
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @param output the writer to write the XML content to; must not be {@code null}
     * @throws UncheckedIOException if an I/O error occurs during write operations
     * @throws ParseException if the object type is not supported for serialization
     */
    @Override
    public void serialize(final Object obj, final XMLSerializationConfig config, final Writer output) {
        final boolean isBufferedWriter = output instanceof BufferedXMLWriter;
        final BufferedXMLWriter bw = isBufferedWriter ? (BufferedXMLWriter) output : Objectory.createBufferedXMLWriter(output);
        final IdentityHashSet<Object> serializedObjects = config == null || !config.supportCircularReference() ? null : new IdentityHashSet<>();

        try {
            write(obj, config, null, serializedObjects, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     * Writes an object to XML using the specified configuration and writer.
     * This is the main internal method that handles the serialization logic.
     * 
     * @param obj the object to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing
     * @param serializedObjects set of already serialized objects for circular reference detection
     * @param bw the buffered XML writer
     * @param flush whether to flush the writer after writing
     * @throws IOException if an I/O error occurs
     */
    protected void write(final Object obj, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final BufferedXMLWriter bw, final boolean flush) throws IOException {
        final XMLSerializationConfig configToUse = check(config);

        if (hasCircularReference(obj, serializedObjects, configToUse, bw)) {
            return;
        }

        if (obj == null) {
            IOUtil.write(Strings.EMPTY, bw);
            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);

        switch (type.getSerializationType()) {
            case SERIALIZABLE:
                if (type.isObjectArray()) {
                    writeArray(obj, configToUse, indentation, serializedObjects, type, bw);
                } else if (type.isCollection()) {
                    writeCollection((Collection<?>) obj, configToUse, indentation, serializedObjects, type, bw);
                } else {
                    type.writeCharacter(bw, obj, configToUse);
                }

                break;

            case ENTITY:
                writeBean(obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            case MAP:
                writeMap((Map<?, ?>) obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            case MAP_ENTITY:
                writeMapEntity((MapEntity) obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            case ARRAY:
                writeArray(obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            case COLLECTION:
                writeCollection((Collection<?>) obj, configToUse, indentation, serializedObjects, type, bw);

                break;

            default:
                if (config == null || config.failOnEmptyBean()) {
                    throw new ParseException("Unsupported class: " + ClassUtil.getCanonicalClassName(cls)
                            + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
                } else {
                    // ignore bw.write("");
                }
        }

        if (flush) {
            bw.flush();
        }
    }

    protected void writeBean(final Object obj, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(type.javaType());

        if (N.isEmpty(beanInfo.jsonXmlSerializablePropInfos)) {
            throw new ParseException("No serializable property is found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final boolean tagByPropertyName = config.tagByPropertyName();
        final boolean writeTypeInfo = config.writeTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (tagByPropertyName) {
            if (writeTypeInfo) {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].namedStartWithType);
            } else {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].namedStart);
            }
        } else {
            if (writeTypeInfo) {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].epStartWithType);
            } else {
                bw.write(beanInfo.xmlNameTags[nameTagIdx].epStart);
            }
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;

        writeProperties(obj, config, propIndentation, serializedObjects, type, bw);

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

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

    protected void writeProperties(final Object obj, final XMLSerializationConfig config, final String propIndentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.clazz();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(type.javaType());

        final Exclusion exclusion = getExclusion(config, beanInfo);

        final boolean ignoreNullProperty = (exclusion == Exclusion.NULL) || (exclusion == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = (exclusion == Exclusion.DEFAULT);

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean tagByPropertyName = config.tagByPropertyName();
        final boolean writeTypeInfo = config.writeTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        final String nextIndentation = isPrettyFormat ? ((propIndentation == null ? Strings.EMPTY : propIndentation) + config.getIndentation()) : null;
        final PropInfo[] propInfoList = config.skipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
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
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(propIndentation);
            }

            if (propValue == null) {
                if (tagByPropertyName) {
                    if (writeTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNullWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNull);
                    }
                } else {
                    if (writeTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNullWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNull);
                    }
                }
            } else {
                if (tagByPropertyName) {
                    if (writeTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStartWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStart);
                    }
                } else {
                    if (writeTypeInfo) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStartWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStart);
                    }
                }

                if (propInfo.isJsonRawValue) {
                    strType.writeCharacter(bw, jsonParser.serialize(propValue, getJSC(config)), config);
                } else if (propInfo.hasFormat) {
                    propInfo.writePropValue(bw, propValue, config);
                } else {
                    writeValue(propValue, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects, propInfo, propInfo.jsonXmlType, bw);
                }

                if (tagByPropertyName) {
                    bw.write(propInfo.xmlNameTags[nameTagIdx].namedEnd);
                } else {
                    bw.write(propInfo.xmlNameTags[nameTagIdx].epEnd);
                }
            }
        }
    }

    protected void writeMap(final Map<?, ?> m, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        //    if (hasCircularReference(m, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.clazz();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean writeTypeInfo = config.writeTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (writeTypeInfo) {
            bw.write(XMLConstants.START_MAP_ELE_WITH_TYPE);
            bw.write(Type.of(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        } else {
            bw.write(XMLConstants.MAP_ELE_START);
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final String nextIndentation = propIndentation + config.getIndentation();

        String strKey = null;
        Type<Object> valueType = null;
        Object key = null;
        Object value = null;

        for (final Map.Entry<Object, Object> entry : ((Map<Object, Object>) m).entrySet()) {
            key = entry.getKey();

            //noinspection SuspiciousMethodCalls
            if ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(key)) {
                continue;
            }

            strKey = key == null ? NULL_STRING : key.toString();
            value = entry.getValue();

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(propIndentation);
            }

            if (value == null) {
                bw.write(WD._LESS_THAN);
                bw.write(strKey);
                bw.write(XMLConstants.IS_NULL_ATTR);
                bw.write(XMLConstants.END_ELEMENT);
            } else {
                valueType = Type.of(value.getClass());

                bw.write(WD._LESS_THAN);
                bw.write(strKey);

                if (writeTypeInfo) {
                    bw.write(XMLConstants.START_TYPE_ATTR);
                    bw.write(valueType.xmlName());
                    bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                } else {
                    bw.write(WD._GREATER_THAN);
                }

                writeValue(value, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects, null, valueType, bw);

                bw.write(WD._LESS_THAN);
                bw.write(WD._SLASH);
                bw.write(strKey);
                bw.write(WD._GREATER_THAN);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XMLConstants.MAP_ELE_END);
    }

    protected void writeMapEntity(final MapEntity mapEntity, final XMLSerializationConfig config, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        //    if (hasCircularReference(mapEntity, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.clazz();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean writeTypeInfo = config.writeTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        bw.write(WD._LESS_THAN);
        bw.write(mapEntity.entityName());

        if (writeTypeInfo) {
            bw.write(XMLConstants.START_TYPE_ATTR);
            bw.write(Type.of(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        } else {
            bw.write(WD._GREATER_THAN);
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final String nextIndentation = propIndentation + config.getIndentation();

        Object value = null;
        Type<Object> valueType = null;

        for (String key : mapEntity.keySet()) {
            if ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(key)) {
                continue;
            }

            value = mapEntity.get(key);

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(propIndentation);
            }

            key = jsonXmlNamingPolicy == null ? key : jsonXmlNamingPolicy.convert(key);

            if (value == null) {
                bw.write(WD._LESS_THAN);
                bw.write(key);
                bw.write(XMLConstants.IS_NULL_ATTR);
                bw.write(XMLConstants.END_ELEMENT);
            } else {
                valueType = Type.of(value.getClass());

                bw.write(WD._LESS_THAN);
                bw.write(key);

                if (writeTypeInfo) {
                    bw.write(XMLConstants.START_TYPE_ATTR);
                    bw.write(valueType.xmlName());
                    bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
                } else {
                    bw.write(WD._GREATER_THAN);
                }

                writeValue(value, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects, null, valueType, bw);

                bw.write(WD._LESS_THAN);
                bw.write(WD._SLASH);
                bw.write(key);
                bw.write(WD._GREATER_THAN);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(WD._LESS_THAN);
        bw.write(WD._SLASH);
        bw.write(mapEntity.entityName());
        bw.write(WD._GREATER_THAN);
    }

    protected void writeArray(final Object obj, final XMLSerializationConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.clazz();
        final boolean writeTypeInfo = config.writeTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (writeTypeInfo) {
            bw.write(XMLConstants.START_ARRAY_ELE_WITH_TYPE);
            bw.write(Type.of(cls).xmlName());
            bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
        } else {
            bw.write(XMLConstants.ARRAY_ELE_START);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final Object[] a = (Object[]) obj;
        final boolean isSerializableByJSON = isSerializableByJSON(a);

        if (isSerializableByJSON) {
            // jsonParser.serialize(bw, a);

            strType.writeCharacter(bw, jsonParser.serialize(a, getJSC(config)), config);
        } else {
            for (final Object e : a) {
                if (e == null) {
                    bw.write(XMLConstants.NULL_NULL_ELE);
                } else {
                    write(e, config, nextIndentation, serializedObjects, bw, false);
                }
            }
        }

        if (isPrettyFormat && !isSerializableByJSON) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XMLConstants.ARRAY_ELE_END);
    }

    protected void writeCollection(final Collection<?> c, final XMLSerializationConfig config, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedXMLWriter bw) throws IOException {
        //    if (hasCircularReference(c, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.clazz();
        final boolean writeTypeInfo = config.writeTypeInfo();
        final boolean isPrettyFormat = config.prettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (type.isList()) {
            if (writeTypeInfo) {
                bw.write(XMLConstants.START_LIST_ELE_WITH_TYPE);
                bw.write(Type.of(cls).xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            } else {
                bw.write(XMLConstants.LIST_ELE_START);
            }
        } else if (type.isSet()) {
            if (writeTypeInfo) {
                bw.write(XMLConstants.START_SET_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            } else {
                bw.write(XMLConstants.SET_ELE_START);
            }
        } else {
            if (writeTypeInfo) {
                bw.write(XMLConstants.START_COLLECTION_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XMLConstants.CLOSE_ATTR_AND_ELE);
            } else {
                bw.write(XMLConstants.COLLECTION_ELE_START);
            }
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final boolean isSerializableByJSON = isSerializableByJSON(c);

        if (isSerializableByJSON) {
            // jsonParser.serialize(bw, c);

            strType.writeCharacter(bw, jsonParser.serialize(c, getJSC(config)), config);
        } else {
            for (final Object e : c) {
                if (e == null) {
                    bw.write(XMLConstants.NULL_NULL_ELE);
                } else {
                    write(e, config, nextIndentation, serializedObjects, bw, false);
                }
            }
        }

        if (isPrettyFormat && !isSerializableByJSON) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

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
     * Writes a value to XML, handling different types appropriately.
     * 
     * @param value the value to write
     * @param config the serialization configuration
     * @param isPrettyFormat whether pretty formatting is enabled
     * @param propIndentation the property indentation string
     * @param nextIndentation the next level indentation string
     * @param serializedObjects set of already serialized objects
     * @param propInfo the property information
     * @param valueType the value type information
     * @param bw the XML writer
     * @throws IOException if an I/O error occurs
     */
    protected void writeValue(final Object value, final XMLSerializationConfig config, final boolean isPrettyFormat, final String propIndentation,
            final String nextIndentation, final IdentityHashSet<Object> serializedObjects, final PropInfo propInfo, final Type<Object> valueType,
            final BufferedXMLWriter bw) throws IOException {
        //    if (hasCircularReference(value, serializedObjects)) {
        //        return;
        //    }

        if (propInfo != null && propInfo.isJsonRawValue) {
            strType.writeCharacter(bw, jsonParser.serialize(value, getJSC(config)), config);
        } else if (valueType.isSerializable()) {
            if (valueType.isObjectArray() || valueType.isCollection()) {
                // jsonParser.serialize(bw, value);

                strType.writeCharacter(bw, jsonParser.serialize(value, getJSC(config)), config);
            } else {
                if (propInfo != null && propInfo.hasFormat) {
                    propInfo.writePropValue(bw, value, config);
                } else {
                    valueType.writeCharacter(bw, value, config);
                }
            }
        } else if (valueType.isObjectArray()) {
            final Object[] a = (Object[]) value;
            final boolean isSerializableByJSON = isSerializableByJSON(a);

            if (isSerializableByJSON) {
                // jsonParser.serialize(bw, a);

                strType.writeCharacter(bw, jsonParser.serialize(a, getJSC(config)), config);
            } else {
                for (final Object e : a) {
                    if (e == null) {
                        if (isPrettyFormat) {
                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                            bw.write(nextIndentation);
                        }

                        bw.write(XMLConstants.NULL_NULL_ELE);
                    } else {
                        write(e, config, nextIndentation, serializedObjects, bw, false);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    bw.write(propIndentation);
                }
            }

        } else if (valueType.isCollection()) {
            final Collection<?> c = (Collection<?>) value;
            final boolean isSerializableByJSON = isSerializableByJSON(c);

            if (isSerializableByJSON) {
                // jsonParser.serialize(bw, c);

                strType.writeCharacter(bw, jsonParser.serialize(c, getJSC(config)), config);
            } else {
                for (final Object e : c) {
                    if (e == null) {
                        if (isPrettyFormat) {
                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                            bw.write(nextIndentation);
                        }

                        bw.write(XMLConstants.NULL_NULL_ELE);
                    } else {
                        write(e, config, nextIndentation, serializedObjects, bw, false);
                    }
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    bw.write(propIndentation);
                }
            }

        } else {
            write(value, config, nextIndentation, serializedObjects, bw, false);

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(propIndentation);
            }
        }
    }

    /**
     * Checks if an array can be serialized as JSON.
     * Arrays containing only serializable types can be serialized as JSON for efficiency.
     * 
     * @param a the array to check
     * @return {@code true} if the array can be serialized as JSON
     */
    protected boolean isSerializableByJSON(final Object[] a) {
        if (Type.of(a.getClass().getComponentType()).isSerializable()) {
            return true;
        } else {
            for (final Object e : a) {
                if (e != null && Type.of(e.getClass()).isSerializable()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if a collection can be serialized as JSON.
     * Collections containing only serializable types can be serialized as JSON for efficiency.
     * 
     * @param c the collection to check
     * @return {@code true} if the collection can be serialized as JSON
     */
    protected boolean isSerializableByJSON(final Collection<?> c) {
        for (final Object e : c) {
            if (e != null && Type.of(e.getClass()).isSerializable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses StAX (Streaming API for XML) or DOM parsing depending on the parser type
     * configured during construction. StAX provides efficient streaming parsing with minimal memory overhead,
     * while DOM loads the entire document into memory for tree-based processing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * String xml = "<user><name>John</name><age>30</age></user>";
     * Type<User> userType = Type.of(User.class);
     * User user = parser.deserialize(xml, null, userType);
     *
     * // With configuration
     * XMLDeserializationConfig config = new XMLDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * User user2 = parser.deserialize(xml, config, userType);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}; returns default value if source is empty
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final String source, final XMLDeserializationConfig config, final Type<? extends T> targetType) {
        if (Strings.isEmpty(source)) {
            return targetType.defaultValue();
        }

        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, null, targetType);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(String, XMLDeserializationConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * String xml = "<user><name>Jane</name><email>jane@example.com</email></user>";
     * User user = parser.deserialize(xml, null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}; returns default value if source is empty
     * @throws UncheckedIOException if an I/O error occurs during parsing
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final String source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a file and parses it using StAX or DOM parsing. The file
     * is automatically opened, read, and closed. The parser type determines the parsing strategy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * File xmlFile = new File("user.xml");
     * Type<User> userType = Type.of(User.class);
     * User user = parser.deserialize(xmlFile, null, userType);
     *
     * // With configuration
     * XMLDeserializationConfig config = new XMLDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * User user2 = parser.deserialize(xmlFile, config, userType);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final File source, final XMLDeserializationConfig config, final Type<? extends T> targetType) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return deserialize(reader, config, targetType);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(File, XMLDeserializationConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * File xmlFile = new File("data.xml");
     * User user = parser.deserialize(xmlFile, null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final File source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from an input stream and parses it using StAX or DOM parsing.
     * The stream is read but not closed, allowing the caller to manage the stream lifecycle.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * try (InputStream in = new FileInputStream("user.xml")) {
     *     Type<User> userType = Type.of(User.class);
     *     User user = parser.deserialize(in, null, userType);
     * }
     *
     * // With configuration
     * try (InputStream in = new URL("http://api.example.com/user.xml").openStream()) {
     *     XMLDeserializationConfig config = new XMLDeserializationConfig()
     *         .ignoreUnmatchedProperty(true);
     *     User user = parser.deserialize(in, config, Type.of(User.class));
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading the stream
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Type<? extends T> targetType) {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, null, targetType);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(InputStream, XMLDeserializationConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * try (InputStream in = new FileInputStream("data.xml")) {
     *     User user = parser.deserialize(in, null, User.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading the stream
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a Reader using StAX or DOM parsing. The reader is used directly
     * for parsing but is not closed, allowing the caller to manage the reader lifecycle.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * try (Reader reader = new FileReader("user.xml")) {
     *     Type<User> userType = Type.of(User.class);
     *     User user = parser.deserialize(reader, null, userType);
     * }
     *
     * // From StringReader
     * String xml = "<user><name>Tom</name></user>";
     * Reader stringReader = new StringReader(xml);
     * User user = parser.deserialize(stringReader, null, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Type<? extends T> targetType) {
        // BufferedReader? will the target parser create the BufferedReader internally?
        return read(source, config, null, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(Reader, XMLDeserializationConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * try (Reader reader = new FileReader("data.xml")) {
     *     User user = parser.deserialize(reader, null, User.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @throws ParseException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses DOM parsing to deserialize XML from a pre-parsed DOM node. This is useful
     * when working with XML that has already been loaded into a DOM tree, or when you need to deserialize
     * a specific portion of an XML document.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.DOM);
     *
     * // Parse XML to DOM
     * Document doc = DocumentBuilderFactory.newInstance()
     *     .newDocumentBuilder()
     *     .parse(new File("user.xml"));
     *
     * // Deserialize from root node
     * Type<User> userType = Type.of(User.class);
     * User user = parser.deserialize(doc.getDocumentElement(), null, userType);
     *
     * // Deserialize from specific child node
     * Node childNode = doc.getElementsByTagName("address").item(0);
     * Address address = parser.deserialize(childNode, null, Type.of(Address.class));
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws ParseException if the XML structure doesn't match the target type
     */
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Type<? extends T> targetType) {
        return readByDOMParser(source, config, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(Node, XMLDeserializationConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.DOM);
     *
     * Document doc = DocumentBuilderFactory.newInstance()
     *     .newDocumentBuilder()
     *     .parse(new File("user.xml"));
     *
     * User user = parser.deserialize(doc.getDocumentElement(), null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws ParseException if the XML structure doesn't match the target type
     */
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Class<? extends T> targetClass) {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a file and uses node class mappings for dynamic type resolution.
     * This enables polymorphic deserialization where different XML elements map to different Java types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * // Define node type mappings
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("customer", Type.of(Customer.class));
     * nodeTypes.put("supplier", Type.of(Supplier.class));
     * nodeTypes.put("product", Type.of(Product.class));
     *
     * File xmlFile = new File("entities.xml");
     * Object entity = parser.deserialize(xmlFile, null, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading the file
     * @throws ParseException if no matching type is found in nodeTypes or XML is malformed
     */
    @Override
    public <T> T deserialize(final File source, final XMLDeserializationConfig config, final Map<String, Type<?>> nodeTypes) {
        Reader reader = null;

        try {
            reader = IOUtil.newFileReader(source);

            return read(reader, config, nodeTypes, null);
        } finally {
            IOUtil.closeQuietly(reader);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from an input stream and uses node class mappings for dynamic type resolution.
     * The stream is read but not closed, allowing the caller to manage the stream lifecycle.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * // Define node type mappings
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("order", Type.of(Order.class));
     * nodeTypes.put("invoice", Type.of(Invoice.class));
     *
     * try (InputStream in = new FileInputStream("document.xml")) {
     *     Object document = parser.deserialize(in, null, nodeTypes);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading the stream
     * @throws ParseException if no matching type is found in nodeTypes or XML is malformed
     */
    @Override
    public <T> T deserialize(final InputStream source, final XMLDeserializationConfig config, final Map<String, Type<?>> nodeTypes) {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, nodeTypes, null);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a Reader and uses node class mappings for dynamic type resolution.
     * The reader is used directly for parsing but is not closed, allowing the caller to manage the reader lifecycle.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
     *
     * // Define node type mappings
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("employee", Type.of(Employee.class));
     * nodeTypes.put("contractor", Type.of(Contractor.class));
     *
     * try (Reader reader = new FileReader("workers.xml")) {
     *     Object worker = parser.deserialize(reader, null, nodeTypes);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws UncheckedIOException if an I/O error occurs while reading
     * @throws ParseException if no matching type is found in nodeTypes or XML is malformed
     */
    @Override
    public <T> T deserialize(final Reader source, final XMLDeserializationConfig config, final Map<String, Type<?>> nodeTypes) {
        return read(source, config, nodeTypes, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses DOM parsing to deserialize from a pre-parsed node using node class mappings
     * for dynamic type resolution. The target type is determined by matching the node's name or its "name"
     * attribute against the provided nodeTypes map.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = new XMLParserImpl(XMLParserType.DOM);
     *
     * // Define node type mappings
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("product", Type.of(Product.class));
     * nodeTypes.put("service", Type.of(Service.class));
     *
     * Document doc = DocumentBuilderFactory.newInstance()
     *     .newDocumentBuilder()
     *     .parse(new File("items.xml"));
     *
     * Object item = parser.deserialize(doc.getDocumentElement(), null, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws ParseException if no matching type is found in nodeTypes or XML structure is invalid
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(final Node source, final XMLDeserializationConfig config, final Map<String, Type<?>> nodeTypes) {
        Type<? extends T> targetType = null;

        if (N.notEmpty(nodeTypes)) {
            String nodeName = XmlUtil.getAttribute(source, XMLConstants.NAME);

            if (Strings.isEmpty(nodeName)) {
                nodeName = source.getNodeName();
            }

            targetType = (Type<T>) nodeTypes.get(nodeName);
        }

        if (targetType == null) {
            throw new ParseException("No target type is specified"); //NOSONAR
        }

        return readByDOMParser(source, config, targetType);
    }

    @SuppressWarnings("unchecked")
    protected <T> T read(final Reader source, final XMLDeserializationConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType) {
        final XMLDeserializationConfig configToUse = check(config);

        switch (parserType) {
            case StAX:
                try {
                    final XMLStreamReader xmlReader = createXMLStreamReader(source);

                    for (int event = xmlReader.next(); event != XMLStreamConstants.START_ELEMENT && xmlReader.hasNext(); event = xmlReader.next()) {
                        // do nothing.
                    }

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        String nodeName = null;

                        if (xmlReader.getAttributeCount() > 0) {
                            nodeName = xmlReader.getAttributeValue(null, XMLConstants.NAME);
                        }

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = xmlReader.getLocalName();
                        }

                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParseException("No target type is specified");
                    }

                    return readByStreamParser(xmlReader, configToUse, targetType);
                } catch (final XMLStreamException e) {
                    throw new ParseException(e);
                }

            case DOM: //NOSONAR
                final DocumentBuilder docBuilder = XmlUtil.createContentParser();

                try {
                    final Document doc = docBuilder.parse(new InputSource(source));
                    final Node node = doc.getFirstChild();

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        String nodeName = XmlUtil.getAttribute(node, XMLConstants.NAME);

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = node.getNodeName();
                        }

                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParseException("No target type is specified");
                    }

                    return readByDOMParser(node, configToUse, targetType);
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

    protected <T> T readByStreamParser(final XMLStreamReader xmlReader, final XMLDeserializationConfig config, final Type<? extends T> targetType)
            throws XMLStreamException {
        return readByStreamParser(xmlReader, config, null, null, targetType);
    }

    @SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
    @SuppressWarnings({ "null", "fallthrough", "deprecation" })
    protected <T> T readByStreamParser(final XMLStreamReader xmlReader, final XMLDeserializationConfig config, PropInfo propInfo, Type<?> propType,
            Type<?> targetType) throws XMLStreamException {
        if (targetType.clazz().equals(Object.class)) {
            targetType = mapEntityType;
        }

        final XMLDeserializationConfig configToUse = check(config);
        final boolean hasPropTypes = configToUse.hasValueTypes();

        if (hasPropTypes && xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            targetType = configToUse.getValueType(xmlReader.getLocalName(), targetType);
        }

        final Class<?> targetClass = targetType.clazz();
        final SerializationType serializationType = getDeserializationType(targetType);

        String propName = null;
        Object propValue = null;
        String text = null;
        StringBuilder sb = null;

        switch (serializationType) {
            case ENTITY: {
                final boolean ignoreUnmatchedProperty = configToUse.ignoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.javaType());
                final Object result = beanInfo.createBeanResult();
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propName = xmlReader.getLocalName();
                                propInfo = beanInfo.getPropInfo(propName);

                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    continue;
                                }

                                if (propInfo == null) {
                                    if (ignoreUnmatchedProperty) {
                                        continue;
                                    } else {
                                        throw new ParseException("Unknown property element: " + propName + " for class: " + targetClass);
                                    }
                                }

                                propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                                if (propType == null) {
                                    if (propInfo.jsonXmlType.isSerializable()) {
                                        propType = propInfo.jsonXmlType;
                                    } else {
                                        attrCount = xmlReader.getAttributeCount();

                                        if (attrCount == 1) {
                                            if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                                propType = Type.of(xmlReader.getAttributeValue(0));
                                            }
                                        } else if (attrCount > 1) {
                                            for (int i = 0; i < attrCount; i++) {
                                                if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                    propType = Type.of(xmlReader.getAttributeValue(i));

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
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObjectType()) {
                                        propValue = readByStreamParser(xmlReader, configToUse, propInfo, propType,
                                                propType.isObjectType() ? mapType : propType);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        final Collection<Object> c = Collection.class.isAssignableFrom(propType.clazz())
                                                ? N.newCollection((Class<Collection>) propType.clazz())
                                                : new ArrayList<>();

                                        final Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                                c.add(null);

                                                xmlReader.next();
                                            } else {
                                                c.add(readByStreamParser(xmlReader, configToUse, null, propType, propEleType));
                                            }
                                        } while (xmlReader.hasNext() && xmlReader.next() == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.isArray() ? collection2Array(c, propType) : c;
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propName)) {
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
                                    throw new ParseException("Unknown parser error at element: " + xmlReader.getLocalName()); //NOSONAR
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            if (propInfo != null) {
                                text = xmlReader.getText();

                                if (text != null && (event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                    do {
                                        if (sb == null) {
                                            sb = new StringBuilder(text.length() * 2);
                                            sb.append(text);
                                        } else if (sb.isEmpty()) {
                                            sb.append(text);
                                        }

                                        sb.append(xmlReader.getText());
                                    } while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS);

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                propValue = propInfo.hasFormat ? propInfo.readPropValue(text) : propType.valueOf(text);

                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                            || (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                        // ignore;
                                    } else {
                                        propInfo.setPropValue(result, propValue == null ? propType.defaultValue() : propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                    propInfo = null;
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propName == null) {
                                return beanInfo.finishBeanResult(result);
                            } else {
                                if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Expose.SERIALIZE_ONLY
                                        || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                    // ignore;
                                } else {
                                    propInfo.setPropValue(result, propValue == null ? propType.defaultValue() : propValue);
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
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[0].isObjectType()) {
                    keyType = propInfo.jsonXmlType.getParameterTypes()[0];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[0].isObjectType()) {
                    keyType = propType.getParameterTypes()[0];
                } else {
                    if (configToUse.getMapKeyType() != null && !configToUse.getMapKeyType().isObjectType()) {
                        keyType = configToUse.getMapKeyType();
                    }
                }

                final boolean isStringKey = keyType.clazz() == String.class;
                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[1].isObjectType()) {
                    valueType = propInfo.jsonXmlType.getParameterTypes()[1];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    }
                }

                @SuppressWarnings("rawtypes")
                final Map<Object, Object> map = N.newMap((Class<Map>) targetClass);
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propName = xmlReader.getLocalName();
                                propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                                if (propType == null) {
                                    attrCount = xmlReader.getAttributeCount();

                                    if (attrCount == 1) {
                                        if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                            propType = Type.of(xmlReader.getAttributeValue(0));
                                        }
                                    } else if (attrCount > 1) {
                                        for (int i = 0; i < attrCount; i++) {
                                            if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                propType = Type.of(xmlReader.getAttributeValue(i));

                                                break;
                                            }
                                        }
                                    }

                                    if (propType == null) {
                                        propType = valueType;
                                    }
                                }
                            } else {
                                if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    for (int startCount = 1, e = xmlReader.next();; e = xmlReader.next()) {
                                        startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                        if (startCount < 0 || !xmlReader.hasNext()) {
                                            break;
                                        }
                                    }
                                } else {
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObjectType()) {
                                        propValue = readByStreamParser(xmlReader, configToUse, null, propType, propType.isObjectType() ? mapType : propType);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        final Collection<Object> c = Collection.class.isAssignableFrom(propType.clazz())
                                                ? N.newCollection((Class<Collection>) propType.clazz())
                                                : new ArrayList<>();

                                        final Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                                c.add(null);

                                                xmlReader.next();
                                            } else {
                                                c.add(readByStreamParser(xmlReader, configToUse, null, propType, propEleType));
                                            }
                                        } while (xmlReader.hasNext() && xmlReader.next() == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.isArray() ? collection2Array(c, propType) : c;
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propName)) {
                                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore.
                                    } else {
                                        map.put(isStringKey ? propName : keyType.valueOf(propName), propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                } else {
                                    throw new ParseException("Unknown parser error at element: " + xmlReader.getLocalName());
                                }

                            }

                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && (event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                do {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                } while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS);

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            propValue = propType.valueOf(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    // ignore;
                                } else {
                                    map.put(isStringKey ? propName : keyType.valueOf(propName), propValue == null ? propType.defaultValue() : propValue);
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propName == null) {
                                return (T) map;
                            } else {
                                if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    // ignore;
                                } else {
                                    map.put(isStringKey ? propName : keyType.valueOf(propName), propValue == null ? propType.defaultValue() : propValue);
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error");
            }

            case MAP_ENTITY: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.getParameterTypes().length == 2 && !propInfo.jsonXmlType.getParameterTypes()[1].isObjectType()) {
                    valueType = propInfo.jsonXmlType.getParameterTypes()[1];
                } else if (propType != null && propType.getParameterTypes().length == 2 && propType.isMap()
                        && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    }
                }

                final MapEntity mapEntity = new MapEntity(xmlReader.getLocalName());
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propName = xmlReader.getLocalName();
                                propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                                if (propType == null) {
                                    attrCount = xmlReader.getAttributeCount();

                                    if (attrCount == 1) {
                                        if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                            propType = Type.of(xmlReader.getAttributeValue(0));
                                        }
                                    } else if (attrCount > 1) {
                                        for (int i = 0; i < attrCount; i++) {
                                            if (XMLConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                propType = Type.of(xmlReader.getAttributeValue(i));

                                                break;
                                            }
                                        }
                                    }

                                    if (propType == null) {
                                        propType = valueType;
                                    }
                                }
                            } else {
                                if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    for (int startCount = 1, e = xmlReader.next();; e = xmlReader.next()) {
                                        startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                        if (startCount < 0 || !xmlReader.hasNext()) {
                                            break;
                                        }
                                    }
                                } else {
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObjectType()) {
                                        propValue = readByStreamParser(xmlReader, configToUse, null, propType,
                                                propType.isObjectType() ? mapEntityType : propType);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        final Collection<Object> c = Collection.class.isAssignableFrom(propType.clazz())
                                                ? N.newCollection((Class<Collection>) propType.clazz())
                                                : new ArrayList<>();

                                        final Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                                c.add(null);

                                                xmlReader.next();
                                            } else {
                                                c.add(readByStreamParser(xmlReader, defaultXMLDeserializationConfig, null, propType, propEleType));
                                            }
                                        } while (xmlReader.hasNext() && xmlReader.next() == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.isArray() ? collection2Array(c, propType) : c;
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propName)) {
                                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore.
                                    } else {
                                        mapEntity.set(propName, propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                } else {
                                    throw new ParseException("Unknown parser error at element: " + xmlReader.getLocalName());
                                }

                            }

                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && (event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                do {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                } while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS);

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            propValue = propType.valueOf(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propName != null) {
                                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore;
                                    } else {
                                        mapEntity.set(propName, propValue == null ? propType.defaultValue() : propValue);
                                    }
                                } else {
                                    // should never happen.
                                    throw new RuntimeException("Unexpected error");
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (propName == null) {
                                return (T) mapEntity;
                            } else {
                                if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    // ignore;
                                } else {
                                    mapEntity.set(propName, propValue == null ? propType.defaultValue() : propValue);
                                }

                                propName = null;
                                propValue = null;
                            }

                            break;
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParseException("Unknown parser error");
            }

            case ARRAY: {
                Type<?> eleType = null;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = Type.of(propInfo.clazz.getComponentType());
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = targetType.isArray() ? targetType.getElementType() : strType;
                    }
                }

                final List<Object> list = Objectory.createList();

                try {
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        switch (event) {
                            case XMLStreamConstants.START_ELEMENT: {
                                if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                    list.add(null);
                                } else if (String.class == eleType.clazz() || Object.class == eleType.clazz()) {
                                    list.add(readByStreamParser(xmlReader, configToUse, null, eleType, mapType));
                                } else {
                                    list.add(readByStreamParser(xmlReader, configToUse, null, eleType, eleType));
                                }

                                break;
                            }

                            // simple array with sample format <array>[1, 2,
                            // 3...]</array>
                            case XMLStreamConstants.CHARACTERS: {
                                text = xmlReader.getText();

                                if (text != null && (event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                    do {
                                        if (sb == null) {
                                            sb = new StringBuilder(text.length() * 2);
                                            sb.append(text);
                                        } else if (sb.isEmpty()) {
                                            sb.append(text);
                                        }

                                        sb.append(xmlReader.getText());
                                    } while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS);

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                                    propValue = targetType.valueOf(text);
                                } else {
                                    propValue = jsonParser.deserialize(text, JDC.create().setElementType(eleType.clazz()), targetType);
                                }

                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (propValue != null) {
                                        return (T) propValue;
                                    } else {
                                        return collection2Array(list, targetType);
                                    }
                                }
                            }

                            case XMLStreamConstants.END_ELEMENT: {
                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return collection2Array(list, targetType);
                                }
                            }

                            default:
                                // continue;
                        }
                    }

                } finally {
                    Objectory.recycle(list);
                }

                throw new ParseException("Unknown parser error");
            }

            case COLLECTION: {
                Type<?> eleType = defaultValueType;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = Type.of(propInfo.clazz.getComponentType());
                } else if (propType != null && propType.getParameterTypes().length == 1 && Collection.class.isAssignableFrom(propType.clazz())
                        && !propType.getParameterTypes()[0].isObjectType()) {
                    eleType = propType.getParameterTypes()[0];
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    }
                }

                @SuppressWarnings("rawtypes")
                final Collection<Object> result = N.newCollection((Class<Collection>) targetClass);

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XMLConstants.IS_NULL))) {
                                result.add(null);
                            } else if (String.class == eleType.clazz() || Object.class == eleType.clazz()) {
                                result.add(readByStreamParser(xmlReader, configToUse, null, eleType, mapType));
                            } else {
                                result.add(readByStreamParser(xmlReader, configToUse, null, eleType, eleType));
                            }

                            break;
                        }

                        // simple list with sample format <list>[1, 2, 3...]</list>
                        case XMLStreamConstants.CHARACTERS: {
                            text = xmlReader.getText();

                            if (text != null && (event = xmlReader.next()) == XMLStreamConstants.CHARACTERS) {
                                do {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                } while ((event = xmlReader.next()) == XMLStreamConstants.CHARACTERS);

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                                propValue = targetType.valueOf(text);
                            } else {
                                propValue = jsonParser.deserialize(text, JDC.create().setElementType(eleType.clazz()), targetType);
                            }

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return (T) result;
                                }
                            }
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
                        + ". Only object array, collection, map and bean types are supported");
        }
    }

    protected <T> T readByDOMParser(final Node node, final XMLDeserializationConfig config, final Type<? extends T> targetType) {
        final XMLDeserializationConfig configToUse = check(config);

        return readByDOMParser(node, configToUse, configToUse.getElementType(), false, false, false, true, targetType);
    }

    @SuppressWarnings({ "unchecked", "null", "deprecation" })
    protected <T> T readByDOMParser(final Node node, final XMLDeserializationConfig config, Type<?> propType, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final boolean isFirstCall, Type<? extends T> inputType) {
        if (node.getNodeType() == Document.TEXT_NODE) {
            return null;
        }

        if (inputType.clazz().equals(Object.class)) {
            inputType = (Type<T>) mapEntityType;
        }

        final XMLDeserializationConfig configToUse = check(config);

        final boolean hasPropTypes = configToUse.hasValueTypes();
        Type<?> targetType = null;
        Class<?> targetClass = null;

        if (isFirstCall) {
            targetType = inputType;
            targetClass = targetType.clazz();
        } else {
            if (propType == null || propType.isString() || propType.isObjectType()) {
                String nodeName = null;
                if (checkedAttr) {
                    nodeName = isTagByPropertyName ? node.getNodeName() : XmlUtil.getAttribute(node, XMLConstants.NAME);
                } else {
                    final String nameAttr = XmlUtil.getAttribute(node, XMLConstants.NAME);
                    nodeName = Strings.isNotEmpty(nameAttr) ? nameAttr : node.getNodeName();
                }

                targetType = hasPropTypes ? configToUse.getValueType(nodeName, propType) : null;
            } else {
                targetType = propType;
            }

            if (targetType == null || targetType.isString() || targetType.isObjectType()) {
                // if (isOneNode(node)) {
                // targetClass = Map.class;
                // } else {
                // targetClass = List.class;
                // }
                //
                targetType = listType;
            }

            targetClass = targetType == null ? null : targetType.clazz();
        }

        targetClass = checkedAttr ? (ignoreTypeInfo ? targetClass : getConcreteClass(node, targetClass)) : getConcreteClass(node, targetClass);

        if (!targetType.clazz().equals(targetClass)) {
            targetType = Type.of(targetClass);
        }

        final SerializationType deserializationType = getDeserializationType(targetType);

        PropInfo propInfo = null;
        String propName = null;
        Node propNode = null;
        Object propValue = null;
        NodeList propNodes = node.getChildNodes();
        int propNodeLength = getNodeLength(propNodes);

        switch (deserializationType) {
            case ENTITY: {
                final boolean ignoreUnmatchedProperty = configToUse.ignoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.javaType());
                final Object result = beanInfo.createBeanResult();

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(propNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(propNode, XMLConstants.TYPE));
                        checkedAttr = true;
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
                            throw new ParseException("Unknown property element: " + propName + " for class: " + targetClass.getCanonicalName());
                        }
                    }

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        if (propInfo.jsonXmlType.isSerializable()) {
                            propType = propInfo.jsonXmlType;
                        } else {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType : Type.of(getConcreteClass(propNode, propInfo.jsonXmlType.clazz()));
                        }
                    }

                    //noinspection ConstantValue
                    propValue = getPropValue(propNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, true,
                            inputType);

                    if (propInfo.jsonXmlExpose != JsonXmlField.Expose.SERIALIZE_ONLY) {
                        propInfo.setPropValue(result, propValue);
                    }
                }

                return beanInfo.finishBeanResult(result);
            }

            case MAP: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[0].isObjectType()) {
                    keyType = propType.getParameterTypes()[0];
                } else {
                    if (configToUse.getMapKeyType() != null && !configToUse.getMapKeyType().isObjectType()) {
                        keyType = configToUse.getMapKeyType();
                    }
                }

                final boolean isStringKey = keyType.clazz() == String.class;

                Type<?> valueType = defaultValueType;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    }
                }

                final Map<Object, Object> mResult = newPropInstance(targetClass, node);

                propNodes = node.getChildNodes();
                propNodeLength = getNodeLength(propNodes);
                //noinspection DataFlowIssue
                propNode = null;
                propType = null;
                //noinspection DataFlowIssue
                propValue = null;

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(propNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(propNode, XMLConstants.TYPE));
                        checkedAttr = true;
                    }

                    propName = isTagByPropertyName ? propNode.getNodeName() : XmlUtil.getAttribute(propNode, XMLConstants.NAME); //NOSONAR

                    if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? valueType : Type.of(getConcreteClass(propNode, valueType.clazz()));
                    }

                    if (propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    //noinspection ConstantValue
                    propValue = getPropValue(propNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, true,
                            inputType);

                    mResult.put(isStringKey ? propName : keyType.valueOf(propName), propValue);
                }

                return (T) mResult;
            }

            case MAP_ENTITY: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> valueType = null;

                if (propType != null && propType.isMap() && !propType.getParameterTypes()[1].isObjectType()) {
                    valueType = propType.getParameterTypes()[1];
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObjectType()) {
                        valueType = configToUse.getMapValueType();
                    } else {
                        valueType = objType;
                    }
                }

                final MapEntity mResult = new MapEntity(node.getNodeName());

                propNodes = node.getChildNodes();
                propNodeLength = getNodeLength(propNodes);
                //noinspection DataFlowIssue
                propNode = null;
                propType = null;
                //noinspection DataFlowIssue
                propValue = null;

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(propNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(propNode, XMLConstants.TYPE));
                        checkedAttr = true;
                    }

                    propName = isTagByPropertyName ? propNode.getNodeName() : XmlUtil.getAttribute(propNode, XMLConstants.NAME); //NOSONAR

                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? valueType : Type.of(getConcreteClass(propNode, valueType.clazz()));
                    }

                    if (propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    //noinspection ConstantValue
                    propValue = getPropValue(propNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, true,
                            inputType);

                    mResult.set(propName, propValue);
                }

                return (T) mResult;
            }

            case ARRAY: { //NOSONAR
                Type<?> eleType = null;

                if (propType != null && (propType.isArray() || propType.isCollection()) && propType.getElementType() != null
                        && !propType.getElementType().isObjectType()) {
                    eleType = propType.getElementType();
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = targetType.isCollection() || targetType.isArray() ? targetType.getElementType() : objType;
                    }
                }

                if (XmlUtil.isTextElement(node)) {
                    if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                        return (T) targetType.valueOf(XmlUtil.getTextContent(node));
                    } else {
                        return (T) jsonParser.deserialize(XmlUtil.getTextContent(node), JDC.create().setElementType(eleType.clazz()), targetType);
                    }
                }

                final List<Object> c = Objectory.createList();

                try {
                    final NodeList eleNodes = node.getChildNodes();
                    Node eleNode = null;

                    for (int i = 0; i < eleNodes.getLength(); i++) {
                        eleNode = eleNodes.item(i);

                        if (eleNode.getNodeType() == Document.TEXT_NODE) {
                            continue;
                        }

                        if (!checkedAttr) {
                            isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XMLConstants.NAME));
                            ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XMLConstants.TYPE));
                            checkedAttr = true;
                        }

                        propName = isTagByPropertyName ? eleNode.getNodeName() : XmlUtil.getAttribute(eleNode, XMLConstants.NAME); //NOSONAR

                        propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                        if (propType == null) {
                            propType = ignoreTypeInfo ? eleType : Type.of(getConcreteClass(eleNode, eleType.clazz()));
                        }

                        if (propType.clazz() == Object.class) {
                            propType = defaultValueType;
                        }

                        //noinspection ConstantValue
                        propValue = getPropValue(eleNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputType);

                        c.add(propValue);
                    }

                    return collection2Array(c, targetType);
                } finally {
                    Objectory.recycle(c);
                }
            }

            case COLLECTION: {
                Type<?> eleType = null;

                if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.getElementType().isObjectType()) {
                    eleType = propType.getElementType();
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObjectType()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = objType;
                    }
                }

                if (XmlUtil.isTextElement(node)) {
                    if (eleType.clazz() == String.class || eleType.clazz() == Object.class) {
                        return (T) targetType.valueOf(XmlUtil.getTextContent(node));
                    } else {
                        return (T) jsonParser.deserialize(XmlUtil.getTextContent(node), JDC.create().setElementType(eleType.clazz()), targetType);
                    }
                }

                final Collection<Object> result = newPropInstance(targetClass, node);

                final NodeList eleNodes = node.getChildNodes();
                Node eleNode = null;

                for (int i = 0; i < eleNodes.getLength(); i++) {
                    eleNode = eleNodes.item(i);

                    if (eleNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XMLConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XMLConstants.TYPE));
                        checkedAttr = true;
                    }

                    propName = isTagByPropertyName ? eleNode.getNodeName() : XmlUtil.getAttribute(eleNode, XMLConstants.NAME); //NOSONAR

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? eleType : Type.of(getConcreteClass(eleNode, eleType.clazz()));
                    }

                    if (propType.clazz() == Object.class) {
                        propType = defaultValueType;
                    }

                    //noinspection ConstantValue
                    propValue = getPropValue(eleNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                            inputType);

                    result.add(propValue);
                }

                return (T) result;
            }

            default:
                throw new ParseException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only object array, collection, map and bean types are supported");
        }
    }

    private <T> Object getPropValue(Node propNode, final XMLDeserializationConfig config, final String propName, Type<?> propType, final PropInfo propInfo,
            final boolean checkedAttr, final boolean isTagByPropertyName, final boolean ignoreTypeInfo, final boolean isProp, final Type<T> inputType) {
        Object propValue = null;

        if (XmlUtil.isTextElement(propNode)) {
            propValue = getPropValue(propName, propType, propInfo, propNode);
        } else {
            if (propType.isMap() || propType.isBean() || propType.isMapEntity()) {
                if (isProp) {
                    propNode = checkOneNode(propNode);
                }

                propType = propType.isObjectType() ? (inputType.isMapEntity() ? inputType : mapType) : propType;

                propValue = readByDOMParser(propNode, config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, inputType);
            } else {
                @SuppressWarnings("rawtypes")
                final Collection<Object> coll = Collection.class.isAssignableFrom(propType.clazz()) ? N.newCollection((Class<Collection>) propType.clazz())
                        : new ArrayList<>();

                final Type<?> propEleType = getPropEleType(propType);

                final NodeList subPropNodes = propNode.getChildNodes();
                final int subPropNodeLength = getNodeLength(subPropNodes);
                Node subPropNode = null;
                for (int k = 0; k < subPropNodeLength; k++) {
                    subPropNode = subPropNodes.item(k);
                    if (subPropNode.getNodeType() == Document.TEXT_NODE) {
                        continue;
                    }

                    coll.add(readByDOMParser(subPropNode, config, propEleType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, inputType));
                }

                propValue = propType.isArray() ? collection2Array(coll, propType) : coll;
            }
        }

        return propValue;
    }

    protected SerializationType getDeserializationType(final Type<?> type) {
        SerializationType serializationType = type.getSerializationType();

        if (type.isSerializable()) {
            if (type.isObjectArray()) {
                serializationType = SerializationType.ARRAY;
            } else if (type.isCollection()) {
                serializationType = SerializationType.COLLECTION;
            }
        }

        return serializationType;
    }

    private Type<?> getPropEleType(final Type<?> propType) {
        Type<?> propEleType = null;

        if (propType.clazz().isArray() && (propType.getElementType().isBean() || propType.getElementType().isMap())) {
            propEleType = propType.getElementType();
        } else if (propType.getParameterTypes().length == 1 && (propType.getParameterTypes()[0].isBean() || propType.getParameterTypes()[0].isMap())) {
            propEleType = propType.getParameterTypes()[0];
        } else {
            propEleType = mapType;
        }

        return propEleType;
    }
}
