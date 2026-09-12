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
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;
import com.landawn.abacus.util.cs;

/**
 * Implementation of the XmlParser interface providing XML serialization and deserialization capabilities.
 * This parser supports both StAX (Streaming API for XML) and DOM (Document Object Model) parsing modes.
 *
 * <p>The parser handles various Java types including:</p>
 * <ul>
 *   <li>Primitive types and their wrappers</li>
 *   <li>Arrays and Collections</li>
 *   <li>Maps and MapEntity objects</li>
 *   <li>JavaBeans with getter/setter methods</li>
 * </ul>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Circular reference handling when {@code XmlSerConfig.setCircularReferenceSupported(true)} is set;
 *       with the default configuration no object identity is tracked and a cyclic graph is rejected with a
 *       {@link ParsingException} once {@link #MAX_SERIALIZATION_DEPTH} nested values have been written</li>
 *   <li>Type information preservation</li>
 *   <li>Pretty-printing support</li>
 *   <li>Flexible property naming policies</li>
 *   <li>Ignoring specific properties during serialization/deserialization</li>
 * </ul>
 *
 * <p>Format notes:</p>
 * <ul>
 *   <li>A map entry is written as an element named after its key, so every key must be a valid XML element
 *       name (an NCName). A {@code Map<Integer, ?>} key, a key holding a space or markup, and a key holding
 *       {@code ':'} are rejected with a {@link ParsingException} instead of producing a document that no XML
 *       reader accepts. A {@code null} key is written as {@code <null>} and read back as the String "null".
 *       A {@code MapEntity}'s own entity name and its property names are checked the same way, as is a bean
 *       property whose custom name ({@code @JsonXmlField}, {@code @JSONField}, {@code @JsonProperty}) is not a
 *       valid element name - but only under the default {@code tagByPropertyName=true}, since
 *       {@link XmlSerConfig#setTagByPropertyName(boolean) tagByPropertyName=false} puts that name in attribute
 *       position, where markup is escaped instead. Under that setting the name is still rejected when it holds
 *       a character XML 1.0 cannot represent, because escaping turns such a character into a reference
 *       ({@code name="a&#x1;b"}) that no XML reader accepts. {@code AbacusXmlParser} applies both checks to a
 *       bean property name too.</li>
 *   <li>Text that XML 1.0 cannot carry (control characters other than tab, LF and CR, isolated surrogates,
 *       {@code U+FFFE} and {@code U+FFFF}) is rejected with a {@link ParsingException} when it is written as
 *       element text. The same characters inside an array or collection that is written as an embedded JSON
 *       payload survive instead, because the JSON escape represents them losslessly. A {@code char} value of
 *       {@code '\0'} is written as an empty element, which reads back as {@code '\0'}.</li>
 *   <li>An array or collection whose elements are not all JSON-serializable is written element by element;
 *       a scalar element is wrapped in an {@code <e>} element so that consecutive scalars stay separate
 *       values instead of being concatenated into one text node. {@code e} is therefore reserved: a bean whose
 *       own element name is {@code e} is read back as text when it appears in an {@code Object}-typed mixed
 *       array or collection (a declared element type that is a bean, map or map entity is honoured). An
 *       {@code <e>} wrapper is also the one place where a {@code type} attribute naming a class the allowlist
 *       does not carry is ignored - the declared element type is used instead - rather than rejected with a
 *       {@link ParsingException}, on both backends.</li>
 *   <li>With {@link XmlParserType#StAX} an element with no text ({@code <s/>}, {@code <s></s>}) yields the
 *       property's default value ({@code null} for a String); with {@link XmlParserType#DOM} it yields an
 *       empty String.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
 *
 * // Serialize object to XML
 * MyBean bean = new MyBean();
 * String xml = parser.serialize(bean);
 *
 * // Deserialize XML to object
 * MyBean restored = parser.deserialize(xml, MyBean.class);
 *
 * // With configuration
 * XmlSerConfig config = new XmlSerConfig()
 *     .setPrettyFormat(true)
 *     .setWriteTypeInfo(false);
 * String xmlWithConfig = parser.serialize(bean, config);
 * }</pre>
 *
 * @see XmlParser
 * @see XmlSerConfig
 * @see XmlDeserConfig
 */
final class XmlParserImpl extends AbstractXmlParser {

    private final XmlParserType parserType;

    /**
     * Constructs a new XmlParserImpl with the specified parser type.
     *
     * @param parserType the type of XML parser to use (StAX or DOM)
     */
    XmlParserImpl(final XmlParserType parserType) {
        this.parserType = parserType;
    }

    /**
     * Constructs a new XmlParserImpl with the specified parser type and configurations.
     *
     * @param parserType the type of XML parser to use (StAX or DOM)
     * @param xsc the XML serialization configuration
     * @param xdc the XML deserialization configuration
     */
    XmlParserImpl(final XmlParserType parserType, final XmlSerConfig xsc, final XmlDeserConfig xdc) {
        super(xsc, xdc);
        this.parserType = parserType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation streams the XML output through a buffered writer, providing efficient
     * memory usage and good performance for objects of any size. The serialization process converts
     * Java objects to XML string format following the configured serialization settings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     *
     * // Simple serialization
     * User user = new User("John", "Doe");
     * String xml = parser.serialize(user);
     *
     * // With configuration
     * XmlSerConfig config = new XmlSerConfig()
     *     .setPrettyFormat(true)
     *     .setWriteTypeInfo(false);
     * String prettyXml = parser.serialize(user, config);
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @return the XML string representation; returns empty string if {@code obj} is {@code null}
     * @throws ParsingException if the object type is not supported for serialization, if a map key, a
     *         {@code MapEntity} name or a bean property's custom name is not a valid XML element name, if a
     *         name or a value holds text that XML 1.0 cannot carry, or if more than
     *         {@link #MAX_SERIALIZATION_DEPTH} values are nested on one path (a circular reference)
     * @throws UncheckedIOException if a value serializer cannot read an underlying stream or reader while generating its XML
     *         representation
     */
    @Override
    public String serialize(final Object obj, final XmlSerConfig config) throws ParsingException, UncheckedIOException {
        if (obj == null) {
            return Strings.EMPTY;
        }

        final XmlSerConfig configToUse = check(config);
        final BufferedXmlWriter bw = Objectory.createBufferedXmlWriter();
        final IdentityHashSet<Object> serializedObjects = !configToUse.isCircularReferenceSupported() ? null : new IdentityHashSet<>();

        try {
            write(obj, configToUse, null, serializedObjects, bw, false);

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
     * <p>This implementation writes the XML output to a file. The file is created
     * if it doesn't exist, and the contents are flushed and properly closed after serialization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     * User user = new User("Jane", "Smith");
     * File outputFile = new File("user.xml");
     *
     * XmlSerConfig config = new XmlSerConfig()
     *     .setPrettyFormat(true);
     * parser.serialize(user, config, outputFile);
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @param output the file to write the XML content to; must not be {@code null}
     * @throws ParsingException if the object type is not supported for serialization, if a map key, a
     *         {@code MapEntity} name or a bean property's custom name is not a valid XML element name, if a
     *         name or a value holds text that XML 1.0 cannot carry, or if more than
     *         {@link #MAX_SERIALIZATION_DEPTH} values are nested on one path (a circular reference)
     * @throws UncheckedIOException if creating, opening, writing, flushing or closing {@code output}, or reading a resource-backed value
     *         while producing XML, fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final File output) throws ParsingException, UncheckedIOException {
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
     * <p>This implementation writes the XML output to an output stream. The stream
     * is flushed after serialization but is not closed, allowing the caller to manage the stream lifecycle.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     * User user = new User("Bob", "Johnson");
     *
     * try (OutputStream out = new FileOutputStream("user.xml")) {
     *     XmlSerConfig config = new XmlSerConfig()
     *         .setPrettyFormat(true);
     *     parser.serialize(user, config, out);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @param output the output stream to write the XML content to; must not be {@code null}
     * @throws ParsingException if the object type is not supported for serialization, if a map key, a
     *         {@code MapEntity} name or a bean property's custom name is not a valid XML element name, if a
     *         name or a value holds text that XML 1.0 cannot carry, or if more than
     *         {@link #MAX_SERIALIZATION_DEPTH} values are nested on one path (a circular reference)
     * @throws UncheckedIOException if writing or flushing XML to {@code output}, or reading a resource-backed value during
     *         serialization, fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final OutputStream output) throws ParsingException, UncheckedIOException {
        final XmlSerConfig configToUse = check(config);
        final BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.isCircularReferenceSupported() ? null : new IdentityHashSet<>();

        try {
            write(obj, configToUse, null, serializedObjects, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation writes the XML output to a writer. The writer
     * is flushed after serialization but is not closed, allowing the caller to manage the writer lifecycle.
     * If the provided writer is already a {@link BufferedXmlWriter}, it is used directly for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     * User user = new User("Alice", "Williams");
     *
     * StringWriter sw = new StringWriter();
     * XmlSerConfig config = new XmlSerConfig()
     *     .setPrettyFormat(true)
     *     .setWriteTypeInfo(true);
     * parser.serialize(user, config, sw);
     * String xml = sw.toString();
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @param output the writer to write the XML content to; must not be {@code null}
     * @throws ParsingException if the object type is not supported for serialization, if a map key, a
     *         {@code MapEntity} name or a bean property's custom name is not a valid XML element name, if a
     *         name or a value holds text that XML 1.0 cannot carry, or if more than
     *         {@link #MAX_SERIALIZATION_DEPTH} values are nested on one path (a circular reference)
     * @throws UncheckedIOException if writing or flushing XML to {@code output}, or reading a resource-backed value during
     *         serialization, fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final Writer output) throws ParsingException, UncheckedIOException {
        final XmlSerConfig configToUse = check(config);
        final boolean isBufferedWriter = output instanceof BufferedXmlWriter;
        final BufferedXmlWriter bw = isBufferedWriter ? (BufferedXmlWriter) output : Objectory.createBufferedXmlWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.isCircularReferenceSupported() ? null : new IdentityHashSet<>();

        try {
            write(obj, configToUse, null, serializedObjects, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle(bw);
            }
        }
    }

    /**
     * Maximum number of structured values (beans, maps, collections, arrays, {@code MapEntity}, ...) that may be
     * nested on one serialization path. The bound is a cycle heuristic for the default
     * {@code circularReferenceSupported=false} mode, which tracks no object identities: a graph deeper than this
     * is rejected with a {@link ParsingException} instead of unwinding in {@link StackOverflowError}.
     * 256 levels stay well inside a 512 KB thread stack.
     */
    static final int MAX_SERIALIZATION_DEPTH = 256;

    /** Per-thread serialization nesting depth counter (used as a single-element mutable int). */
    private static final ThreadLocal<int[]> SERIALIZATION_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

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
     * @throws ParsingException if the object type is not supported and the configuration requires failing on it, or if structured values are nested
     *         more than {@link #MAX_SERIALIZATION_DEPTH} levels deep on the current thread (the signature of a cyclic object graph when
     *         circular-reference support is disabled)
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void write(final Object obj, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final BufferedXmlWriter bw, final boolean flush) throws ParsingException, IOException {
        final XmlSerConfig configToUse = check(config);

        if (hasCircularReference(obj, serializedObjects, configToUse, bw)) {
            return;
        }

        if (obj == null) {
            IOUtil.write(Strings.EMPTY, bw);

            if (flush) {
                bw.flush();
            }

            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);
        final SerializationType serializationType = type.serializationType();

        // Bounded-depth guard for structured values (same shape as JsonParserImpl.write and as the read side's
        // XML_NESTING_DEPTH): with circularReferenceSupported=false no identity set is allocated, so a cyclic
        // graph unwound in StackOverflowError instead of a ParsingException. A scalar never recurses from here,
        // but a serializable object array/collection does, so those are counted too.
        final int[] depth = serializationType == SerializationType.SERIALIZABLE && !type.isObjectArray() && !type.isCollection() ? null
                : SERIALIZATION_DEPTH.get();

        if (depth != null && ++depth[0] > MAX_SERIALIZATION_DEPTH) {
            // Undo this level's increment: the finally block below is not reached from here, while the outer
            // levels unwind through theirs and release the thread-local at the root.
            depth[0]--;

            throw new ParsingException("Serialization nesting depth exceeded " + MAX_SERIALIZATION_DEPTH + " while writing " + ClassUtil.getClassName(cls)
                    + ": the object graph is probably circular (self reference). "
                    + "Enable XmlSerConfig.setCircularReferenceSupported(true) to write repeated objects as empty elements");
        }

        try {
            switch (serializationType) {
                case SERIALIZABLE:
                    if (type.isObjectArray()) {
                        writeArray(obj, configToUse, indentation, serializedObjects, type, bw);
                    } else if (type.isCollection()) {
                        writeCollection((Collection<?>) obj, configToUse, indentation, serializedObjects, type, bw);
                    } else {
                        type.serializeTo(bw, obj, configToUse);
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
                    if (configToUse.isFailOnEmptyBean()) {
                        throw new ParsingException("Unsupported class: " + ClassUtil.getCanonicalClassName(cls)
                                + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
                    } else {
                        // ignore bw.write("");
                    }
            }
        } finally {
            // Leaving the outermost level: discard the counter so no state lingers on pooled threads.
            if (depth != null && --depth[0] == 0) {
                SERIALIZATION_DEPTH.remove();
            }

            // Path-based cycle detection (like JsonParserImpl.write): without removing the object
            // after it is fully written, two sibling references to the SAME object (a DAG, not a
            // cycle) would be misidentified as circular and silently emitted as empty elements.
            if (serializedObjects != null) {
                serializedObjects.remove(obj);
            }
        }

        if (flush) {
            bw.flush();
        }
    }

    /**
     * Writes a bean object to XML, emitting the enclosing element and serializing each
     * serializable property.
     *
     * @param obj the bean to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the bean
     * @param bw the buffered XML writer
     * @throws ParsingException if no serializable property is found in the bean class and {@code config.isFailOnEmptyBean()} is {@code true}; with
     *         the flag off such a bean is written as an empty element
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeBean(final Object obj, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.javaType();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(type.reflectType());

        // Gate on the flag only, exactly like JsonParserImpl.writeBean: the code below already emits the
        // start and end tags, so with the flag off an all-ignored bean is written as an empty element.
        if (N.isEmpty(beanInfo.jsonXmlSerializablePropInfos) && config.isFailOnEmptyBean()) {
            throw new ParsingException("No serializable property is found in class: " + ClassUtil.getCanonicalClassName(cls));
        }

        final boolean tagByPropertyName = config.isTagByPropertyName();
        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();
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

    /**
     * Writes the serializable properties of a bean object to XML.
     *
     * <p>An {@code Optional}/{@code Nullable} property is written as the value it holds: an empty wrapper
     * produces the same {@code isNull="true"} element as a {@code null} property (and is dropped by
     * {@link Exclusion#NULL}), a present one produces its unwrapped value. A tuple-like property
     * ({@code Pair}, {@code Triple}, {@code Tuple1..9}, {@code Indexed}, {@code Timed}) is written as its
     * JSON text so that a comma inside a String slot stays inside that slot. Neither shape carries a
     * {@code type} attribute, because the element holds the unwrapped value rather than the wrapper.</p>
     *
     * @param obj the bean whose properties are to be written
     * @param config the serialization configuration
     * @param propIndentation the indentation string applied to each property, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the bean
     * @param bw the buffered XML writer
     * @throws ParsingException if a property that is written has a custom name that {@code tagByPropertyName} would put in element-name position
     *         and that is not a valid XML element name, or that holds text XML 1.0 cannot carry (which no amount of attribute escaping can rescue),
     *         or if a property value holds such text
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeProperties(final Object obj, final XmlSerConfig config, final String propIndentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.javaType();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(type.reflectType());

        final Exclusion exclusion = getExclusion(config, beanInfo);

        final boolean ignoreNullProperty = (exclusion == Exclusion.NULL) || (exclusion == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = (exclusion == Exclusion.DEFAULT);

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean tagByPropertyName = config.isTagByPropertyName();
        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();

        final String nextIndentation = isPrettyFormat ? ((propIndentation == null ? Strings.EMPTY : propIndentation) + config.getIndentation()) : null;
        final PropInfo[] propInfoList = config.isSkipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
        PropInfo propInfo = null;
        String propName = null;
        Object propValue = null;

        for (final PropInfo element : propInfoList) {
            propInfo = element;
            propName = propInfo.name;

            if (propInfo.jsonXmlExpose == JsonXmlField.Direction.DESERIALIZE_ONLY
                    || ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(propName))) {
                continue;
            }

            propValue = propInfo.getPropValue(obj);

            // An empty Optional/Nullable is written like a null property (its own serializeTo would emit the JSON
            // literal "null", which the reader hands to the ELEMENT type: unreadable for anything but a String);
            // a present one is written as its unwrapped element.
            final boolean unwrapped = propValue != null && propInfo.jsonXmlType.isOptionalOrNullable();

            if (unwrapped) {
                propValue = unwrapOptional(propValue);
            }

            // The element carries the UNWRAPPED value (or the isNull marker), never the wrapper itself, so the
            // wrapper's own name must not be written as the type attribute: it describes the wrong shape and the
            // readers reject it ("XML type attribute is not allowed: JdkOptionalInt"). The declared property type
            // is what re-wraps the value on the way back. The same holds for a Pair/Triple/Tuple/Timed/Indexed
            // property, which is written as its text form.
            final boolean writeTypeInfoForProp = writeTypeInfo && !propInfo.jsonXmlType.isOptionalOrNullable() && !isTupleLike(propInfo.jsonXmlType);

            if ((ignoreNullProperty && propValue == null) || (ignoreDefaultProperty && propValue != null && (propInfo.jsonXmlType != null)
                    && propInfo.jsonXmlType.isPrimitive() && propValue.equals(propInfo.jsonXmlType.defaultValue()))) {
                continue;
            }

            // A custom name (@JsonXmlField/@JSONField/@JsonProperty) is handed through verbatim, and with
            // tagByPropertyName it lands in element-name position, where XML has no escaping mechanism:
            // @JsonProperty("a b") wrote <a b>v</a b>, which no XML reader accepts. ParserUtil.XmlNameTag
            // escapes the name for the ep* (attribute) style and leaves this check to the writers, which alone
            // know which style is in use. Checked after the ignore/exclusion filters, exactly as for a map key,
            // so a property that is not written cannot make serialization fail. The ep* style is escaped, but
            // escaping cannot rescue a character XML 1.0 has no representation for: a name holding U+0001
            // wrote name="a&#x1;b", which this parser's own reader rejects ("Illegal character entity"), and
            // the same code unit in a property VALUE is already rejected by writeXmlScalar.
            if (tagByPropertyName) {
                checkXmlElementName(propInfo.xmlNameTags[nameTagIdx].name, "Property name");
            } else {
                checkXmlAttributeName(propInfo.xmlNameTags[nameTagIdx].name, "Property name");
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(propIndentation);
            }

            if (propValue == null) {
                if (tagByPropertyName) {
                    if (writeTypeInfoForProp) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNullWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedNull);
                    }
                } else {
                    if (writeTypeInfoForProp) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNullWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epNull);
                    }
                }
            } else {
                if (tagByPropertyName) {
                    if (writeTypeInfoForProp) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStartWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].namedStart);
                    }
                } else {
                    if (writeTypeInfoForProp) {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStartWithType);
                    } else {
                        bw.write(propInfo.xmlNameTags[nameTagIdx].epStart);
                    }
                }

                if (propInfo.isJsonRawValue) {
                    // Raw JSON: write the serialized payload verbatim apart from the three markup characters,
                    // which writeRawJson entity-escapes so the document stays well-formed. strType.serializeTo
                    // would quote/escape everything and produce <metadata>"{\"k\":\"v\"}"</metadata>.
                    writeRawJson(bw, serializeEmbeddedJson(propValue, config));
                } else if (unwrapped) {
                    writeUnwrappedValue(bw, propInfo.jsonXmlType.elementType(), propValue, config, "Property '" + propName + "'");
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

    /**
     * Writes a map to XML, emitting an enclosing map element and a child element for each entry
     * keyed by the entry's string key.
     *
     * <p>As in {@link #writeProperties}, an {@code Optional}/{@code Nullable} value is written as the value it
     * holds: an empty wrapper produces the same {@code isNull="true"} element as a {@code null} value, a present
     * one produces its unwrapped value and that value's {@code type} attribute. A tuple-like value
     * ({@code Pair}, {@code Triple}, {@code Tuple1..9}, {@code Indexed}, {@code Timed}) is written as its JSON
     * text and carries no {@code type} attribute, since that text is not a value of the tuple handler's own
     * shape; the declared value type is what reads it back.</p>
     *
     * <p>A wrapper <i>key</i> is unwrapped the same way, before the {@code ignoredPropNames} filter and the
     * element-name check see it: {@code Optional.of("k")} is written as {@code <k>}, and an empty wrapper key
     * as the {@code null} key's {@code <null>} element.</p>
     *
     * @param m the map to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the map
     * @param bw the buffered XML writer
     * @throws ParsingException if a key that is not filtered out by {@code ignoredPropNames} is not a valid XML element name, or if a value holds
     *         text that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeMap(final Map<?, ?> m, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(m, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.javaType();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (writeTypeInfo) {
            bw.write(XmlConstants.START_MAP_ELE_WITH_TYPE);
            bw.write(Type.of(cls).xmlName());
            bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
        } else {
            bw.write(XmlConstants.MAP_ELE_START);
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final String nextIndentation = isPrettyFormat ? (propIndentation + config.getIndentation()) : null;

        String strKey = null;
        Type<Object> valueType = null;
        Object key = null;
        Object value = null;

        for (final Map.Entry<Object, Object> entry : ((Map<Object, Object>) m).entrySet()) {
            key = entry.getKey();

            if (key != null && Type.of(key.getClass()).isOptionalOrNullable()) {
                // The same unwrapping the value gets below, and the one JsonParserImpl and
                // AbacusXmlParserImpl.writeMap already apply to a key: without it the wrapper's own toString
                // became the element name, so a present wrapper key failed ("Map key 'Optional[k]' is not a
                // valid XML element name") while an EMPTY one was written as <Optional.empty> and read back as
                // the String key "Optional.empty". Unwrapped before strKey so the ignored-name filter and the
                // element name both see the key the entry is actually written under.
                key = unwrapOptional(key);
            }

            strKey = key == null ? NULL_STRING : key.toString();

            // Filtered on the name the entry is actually written (and read back) under: a null key becomes the
            // element <null>, so it is ignored by the name "null", exactly as the reader filters it. Testing the
            // bare null key also called contains(null), which throws on a Set.of(..) - the very shape
            // ParserConfig.setIgnoredPropNames' own examples use.
            if (ignoredClassPropNames != null && ignoredClassPropNames.contains(strKey)) {
                continue;
            }

            // The key becomes the element name, and XML cannot escape a name: report it here rather than write a
            // document that neither backend can read back (<map><1>a</1></map>). Checked after the ignore filter
            // so an ignored entry with an unusable key does not throw.
            checkXmlElementName(strKey, "Map key");

            value = entry.getValue();

            if (value != null && Type.of(value.getClass()).isOptionalOrNullable()) {
                // As for a bean property (and as AbacusXmlParserImpl.writeMap does): an empty wrapper is written
                // as the isNull form, a present one as its unwrapped value with the unwrapped value's own type.
                // The wrapper's own name is not in the type-attribute allowlist, so writing it made the DOM
                // backend reject this parser's own output ("XML type attribute is not allowed: Nullable<Object>").
                value = unwrapOptional(value);
            }

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(propIndentation);
            }

            if (value == null) {
                bw.write(SK._LESS_THAN);
                bw.write(strKey);
                bw.write(XmlConstants.IS_NULL_ATTR);
                bw.write(XmlConstants.END_ELEMENT);
            } else {
                valueType = Type.of(value.getClass());

                bw.write(SK._LESS_THAN);
                bw.write(strKey);

                // Same suppression as writeProperties/writeElement: a tuple-like value is written as its JSON
                // text, so its own xmlName describes the wrong shape and is not an accepted type-attribute name -
                // writing it made the DOM backend reject this parser's own output.
                if (writeTypeInfo && !isTupleLike(valueType)) {
                    bw.write(XmlConstants.START_TYPE_ATTR);
                    bw.write(valueType.xmlName());
                    bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
                } else {
                    bw.write(SK._GREATER_THAN);
                }

                writeValue(value, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects, null, valueType, bw);

                bw.write(SK._LESS_THAN);
                bw.write(SK._SLASH);
                bw.write(strKey);
                bw.write(SK._GREATER_THAN);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XmlConstants.MAP_ELE_END);
    }

    /**
     * Writes a {@link MapEntity} to XML, emitting an element named after the entity and a child
     * element for each of its properties.
     *
     * <p>As in {@link #writeProperties}, an {@code Optional}/{@code Nullable} value is written as the value it
     * holds: an empty wrapper produces the same {@code isNull="true"} element as a {@code null} value, a present
     * one produces its unwrapped value and that value's {@code type} attribute. A tuple-like value
     * ({@code Pair}, {@code Triple}, {@code Tuple1..9}, {@code Indexed}, {@code Timed}) is written as its JSON
     * text and carries no {@code type} attribute, since that text is not a value of the tuple handler's own
     * shape; the declared value type is what reads it back.</p>
     *
     * @param mapEntity the map entity to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the map entity
     * @param bw the buffered XML writer
     * @throws ParsingException if the entity name is not a valid XML element name, if a property name that is not filtered out by {@code ignoredPropNames}
     *         is not a valid XML element name after the naming policy has been applied, or if a value holds text that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeMapEntity(final MapEntity mapEntity, final XmlSerConfig config, final String indentation,
            final IdentityHashSet<Object> serializedObjects, final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(mapEntity, serializedObjects, config, bw)) {
        //        return;
        //    }

        // The entity name goes into element-name position (both tags below), exactly like the property names
        // checked further down, and MapEntity accepts any String - including "" and "a b" - as its name.
        checkXmlElementName(mapEntity.entityName(), "MapEntity name");

        final Class<?> cls = type.javaType();
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy();
        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
        // final boolean ignoreNullProperty = (config.getExclusion() == Exclusion.NULL) || (config.getExclusion() == Exclusion.DEFAULT);
        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        bw.write(SK._LESS_THAN);
        bw.write(mapEntity.entityName());

        if (writeTypeInfo) {
            bw.write(XmlConstants.START_TYPE_ATTR);
            bw.write(Type.of(cls).xmlName());
            bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
        } else {
            bw.write(SK._GREATER_THAN);
        }

        final String propIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final String nextIndentation = isPrettyFormat ? (propIndentation + config.getIndentation()) : null;

        Object value = null;
        Type<Object> valueType = null;

        for (String key : mapEntity.keySet()) {
            if ((ignoredClassPropNames != null) && ignoredClassPropNames.contains(key)) {
                continue;
            }

            value = mapEntity.get(key);

            if (value != null && Type.of(value.getClass()).isOptionalOrNullable()) {
                // Same as writeMap/writeProperties: an empty wrapper becomes the isNull form and a present one
                // its unwrapped value; the wrapper's own name is not an accepted type attribute.
                value = unwrapOptional(value);
            }

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(propIndentation);
            }

            key = jsonXmlNamingPolicy == null ? key : jsonXmlNamingPolicy.convert(key);

            // Checked AFTER the naming policy: the converted name is what lands in the document.
            checkXmlElementName(key, "MapEntity property name");

            if (value == null) {
                bw.write(SK._LESS_THAN);
                bw.write(key);
                bw.write(XmlConstants.IS_NULL_ATTR);
                bw.write(XmlConstants.END_ELEMENT);
            } else {
                valueType = Type.of(value.getClass());

                bw.write(SK._LESS_THAN);
                bw.write(key);

                // Same suppression as writeProperties/writeElement: a tuple-like value is written as its JSON
                // text, so its own xmlName describes the wrong shape and is not an accepted type-attribute name -
                // writing it made the DOM backend reject this parser's own output.
                if (writeTypeInfo && !isTupleLike(valueType)) {
                    bw.write(XmlConstants.START_TYPE_ATTR);
                    bw.write(valueType.xmlName());
                    bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
                } else {
                    bw.write(SK._GREATER_THAN);
                }

                writeValue(value, config, isPrettyFormat, propIndentation, nextIndentation, serializedObjects, null, valueType, bw);

                bw.write(SK._LESS_THAN);
                bw.write(SK._SLASH);
                bw.write(key);
                bw.write(SK._GREATER_THAN);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(SK._LESS_THAN);
        bw.write(SK._SLASH);
        bw.write(mapEntity.entityName());
        bw.write(SK._GREATER_THAN);
    }

    /**
     * Writes one element of an array or collection that is not written as a JSON payload.
     *
     * <p>A directly serializable scalar is wrapped in an {@code <e>} element (carrying a {@code type} attribute
     * when type information is enabled) so that consecutive scalars stay separate values: written bare, the
     * elements {@code "s"} and {@code 1} fuse into the single text node {@code s1}. Beans, maps, map entities
     * and nested containers keep their own element and are written by
     * {@link #write(Object, XmlSerConfig, String, IdentityHashSet, BufferedXmlWriter, boolean)}; a {@code null}
     * element is written as the {@code <null isNull="true" />} marker. An {@code Optional}/{@code Nullable}
     * element is written as the value it holds, so an empty one produces that same {@code null} marker and a
     * present one the element its unwrapped value would have produced on its own.</p>
     *
     * @param e the element to write; may be {@code null}
     * @param config the serialization configuration
     * @param indentation the indentation string for this element when pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param bw the buffered XML writer
     * @param what a short description of the value used in error messages
     * @throws ParsingException if the element holds text that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    private void writeElement(final Object e, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final BufferedXmlWriter bw, final String what) throws ParsingException, IOException {
        Object value = e;
        Type<Object> eleType = value == null ? null : Type.of(value.getClass());

        if (eleType != null && eleType.isOptionalOrNullable()) {
            // Unwrapped BEFORE the null and structured tests, as AbacusXmlParserImpl.writeArray does: an empty
            // wrapper is then the null element marker (the readers map it back to an empty wrapper) instead of an
            // empty <e>, which StAX reads as null and DOM as "", and a present one carries its own type rather
            // than the wrapper's, which describes the wrong shape.
            value = unwrapOptional(value);
            eleType = value == null ? null : Type.of(value.getClass());
        }

        if (value != null && (!eleType.isSerializable() || eleType.isObjectArray() || eleType.isCollection())) {
            // Structured values bring their own element (and their own pretty-format indentation).
            write(value, config, indentation, serializedObjects, bw, false);

            return;
        }

        if (config.isPrettyFormat()) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        if (value == null) {
            bw.write(XmlConstants.NULL_NULL_ELE);

            return;
        }

        final boolean isTuple = isTupleLike(eleType);

        // A tuple-like element holds its JSON text, not a value of its own handler's shape, and its name is not in
        // the type-attribute allowlist: writing it would make the DOM reader reject this parser's own document.
        if (config.isWriteTypeInfo() && !isTuple) {
            bw.write(XmlConstants.START_E_ELE_WITH_TYPE);
            bw.write(eleType.xmlName());
            bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
        } else {
            bw.write(XmlConstants.E_ELE_START);
        }

        if (isTuple) {
            writeUnwrappedValue(bw, eleType, value, config, what);
        } else {
            writeXmlScalar(bw, eleType, value, config, what);
        }

        bw.write(XmlConstants.E_ELE_END);
    }

    /**
     * Writes an object array to XML, emitting an enclosing array element. If all elements are
     * serializable, the array is written as a JSON payload; otherwise each element is written
     * recursively by {@link #writeElement}, which wraps a scalar element in {@code <e>}.
     *
     * @param obj the array to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the array
     * @param bw the buffered XML writer
     * @throws ParsingException if an element holds text that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeArray(final Object obj, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(obj, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.javaType();
        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (writeTypeInfo) {
            bw.write(XmlConstants.START_ARRAY_ELE_WITH_TYPE);
            bw.write(Type.of(cls).xmlName());
            bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
        } else {
            bw.write(XmlConstants.ARRAY_ELE_START);
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final Object[] a = (Object[]) obj;
        final boolean isSerializableByJson = isSerializableByJson(a);

        if (isSerializableByJson) {
            // jsonParser.serialize(bw, a);

            strType.serializeTo(bw, serializeEmbeddedJson(a, config), config);
        } else {
            for (final Object e : a) {
                writeElement(e, config, nextIndentation, serializedObjects, bw, "Array element");
            }
        }

        if (isPrettyFormat && !isSerializableByJson) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XmlConstants.ARRAY_ELE_END);
    }

    /**
     * Writes a collection to XML, emitting an enclosing element appropriate to the collection
     * kind (list, set, or generic collection). If all elements are serializable, the collection
     * is written as a JSON payload; otherwise each element is written recursively by
     * {@link #writeElement}, which wraps a scalar element in {@code <e>}.
     *
     * @param c the collection to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the collection
     * @param bw the buffered XML writer
     * @throws ParsingException if an element holds text that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeCollection(final Collection<?> c, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(c, serializedObjects, config, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.javaType();
        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (type.isList()) {
            if (writeTypeInfo) {
                bw.write(XmlConstants.START_LIST_ELE_WITH_TYPE);
                bw.write(Type.of(cls).xmlName());
                bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
            } else {
                bw.write(XmlConstants.LIST_ELE_START);
            }
        } else if (type.isSet()) {
            if (writeTypeInfo) {
                bw.write(XmlConstants.START_SET_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
            } else {
                bw.write(XmlConstants.SET_ELE_START);
            }
        } else {
            if (writeTypeInfo) {
                bw.write(XmlConstants.START_COLLECTION_ELE_WITH_TYPE);
                bw.write(type.xmlName());
                bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
            } else {
                bw.write(XmlConstants.COLLECTION_ELE_START);
            }
        }

        final String nextIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final boolean isSerializableByJson = isSerializableByJson(c);

        if (isSerializableByJson) {
            // jsonParser.serialize(bw, c);

            strType.serializeTo(bw, serializeEmbeddedJson(c, config), config);
        } else {
            for (final Object e : c) {
                writeElement(e, config, nextIndentation, serializedObjects, bw, "Collection element");
            }
        }

        if (isPrettyFormat && !isSerializableByJson) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        if (type.isList()) {
            bw.write(XmlConstants.LIST_ELE_END);
        } else if (type.isSet()) {
            bw.write(XmlConstants.SET_ELE_END);
        } else {
            bw.write(XmlConstants.COLLECTION_ELE_END);
        }
    }

    /**
     * Writes a value to XML, handling different types appropriately.
     *
     * <p>A scalar is written as element text; an array or collection whose elements are all serializable is
     * written as a JSON payload, and otherwise element by element with each scalar element wrapped in
     * {@code <e>} (see {@link #writeElement}). A tuple-like value ({@code Pair}, {@code Triple},
     * {@code Tuple1..9}, {@code Indexed}, {@code Timed}) is written as the text its own {@code valueOf} reads
     * back. An optional reaching this method is written the same way -- unwrapped, or nothing at all when it is
     * empty -- but {@link #writeProperties}, {@link #writeMap}, {@link #writeMapEntity} and
     * {@link #writeElement} all unwrap first, so an empty wrapper normally produces the enclosing element's
     * {@code isNull="true"} form rather than an empty element.</p>
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
     * @throws ParsingException if the value holds text that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeValue(final Object value, final XmlSerConfig config, final boolean isPrettyFormat, final String propIndentation,
            final String nextIndentation, final IdentityHashSet<Object> serializedObjects, final PropInfo propInfo, final Type<Object> valueType,
            final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(value, serializedObjects)) {
        //        return;
        //    }

        final String what = propInfo == null ? "Value" : "Property '" + propInfo.name + "'";

        if (propInfo != null && propInfo.isJsonRawValue) {
            writeRawJson(bw, serializeEmbeddedJson(value, config));
        } else if (valueType.isSerializable()) {
            if (valueType.isObjectArray() || valueType.isCollection()) {
                // jsonParser.serialize(bw, value);

                strType.serializeTo(bw, serializeEmbeddedJson(value, config), config);
            } else if (valueType.isOptionalOrNullable() || isTupleLike(valueType)) {
                // The wrapper's own serializeTo would write the JSON literal "null" for an empty optional and
                // unquoted String slots for a tuple (a comma inside a slot then breaks the value); the unwrapped
                // element and the tuple's JSON text are what the matching valueOf reads back.
                final Object unwrapped = unwrapOptional(value);

                if (unwrapped != null) {
                    writeUnwrappedValue(bw, valueType.isOptionalOrNullable() ? valueType.elementType() : valueType, unwrapped, config, what);
                }
            } else {
                if (propInfo != null && propInfo.hasFormat) {
                    propInfo.writePropValue(bw, value, config);
                } else {
                    writeXmlScalar(bw, valueType, value, config, what);
                }
            }
        } else if (valueType.isObjectArray()) {
            final Object[] a = (Object[]) value;
            final boolean isSerializableByJson = isSerializableByJson(a);

            if (isSerializableByJson) {
                // jsonParser.serialize(bw, a);

                strType.serializeTo(bw, serializeEmbeddedJson(a, config), config);
            } else {
                for (final Object e : a) {
                    writeElement(e, config, nextIndentation, serializedObjects, bw, what);
                }

                if (isPrettyFormat) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    bw.write(propIndentation);
                }
            }

        } else if (valueType.isCollection()) {
            final Collection<?> c = (Collection<?>) value;
            final boolean isSerializableByJson = isSerializableByJson(c);

            if (isSerializableByJson) {
                // jsonParser.serialize(bw, c);

                strType.serializeTo(bw, serializeEmbeddedJson(c, config), config);
            } else {
                for (final Object e : c) {
                    writeElement(e, config, nextIndentation, serializedObjects, bw, what);
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
     * An array can be serialized as JSON for efficiency if its component type is serializable,
     * or if every non-{@code null} element is itself serializable.
     *
     * @param a the array to check
     * @return {@code true} if the array can be serialized as JSON; {@code false} otherwise
     */
    protected boolean isSerializableByJson(final Object[] a) {
        if (Type.of(a.getClass().getComponentType()).isSerializable()) {
            return true;
        } else {
            for (final Object e : a) {
                if (e != null && !Type.of(e.getClass()).isSerializable()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if a collection can be serialized as JSON.
     * A collection can be serialized as JSON for efficiency if every non-{@code null} element is serializable.
     *
     * @param c the collection to check
     * @return {@code true} if the collection can be serialized as JSON; {@code false} otherwise
     */
    protected boolean isSerializableByJson(final Collection<?> c) {
        for (final Object e : c) {
            if (e != null && !Type.of(e.getClass()).isSerializable()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses StAX (Streaming API for XML) or DOM parsing depending on the parser type
     * configured during construction. StAX provides efficient streaming parsing with minimal memory overhead,
     * while DOM loads the entire document into memory for tree-based processing.</p>
     *
     * <p>The whole String must be one XML document: content after the root element other than whitespace,
     * comments and processing instructions (a second root element, text, a CDATA section) is rejected with a
     * {@link ParsingException} by both parser types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     *
     * String xml = "<user><name>John</name><age>30</age></user>";
     * Type<User> userType = Type.of(User.class);
     * User user = parser.deserialize(xml, null, userType);
     *
     * // With configuration
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * User user2 = parser.deserialize(xml, config, userType);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}; returns the target type's default value if {@code source} is {@code null} or empty
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM backend or a delegated value reader reports an {@code IOException} while consuming the XML
     *         text
     */
    @Override
    public <T> T deserialize(final String source, final XmlDeserConfig config, final Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (Strings.isEmpty(source)) {
            return targetType.defaultValue();
        }

        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            return read(br, config, null, targetType, true);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(String, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     *
     * String xml = "<user><name>Jane</name><email>jane@example.com</email></user>";
     * User user = parser.deserialize(xml, null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}; returns the target type's default value if {@code source} is {@code null} or empty
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM backend or a delegated value reader reports an {@code IOException} while consuming the XML
     *         text
     */
    @Override
    public <T> T deserialize(final String source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws ParsingException, UncheckedIOException {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a file and parses it using StAX or DOM parsing. The file
     * is automatically opened, read, and closed. The parser type determines the parsing strategy. The file must
     * hold one XML document: content after the root element other than whitespace, comments and processing
     * instructions is rejected with a {@link ParsingException} by both parser types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     *
     * File xmlFile = new File("user.xml");
     * Type<User> userType = Type.of(User.class);
     * User user = parser.deserialize(xmlFile, null, userType);
     *
     * // With configuration
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * User user2 = parser.deserialize(xmlFile, config, userType);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if opening {@code source} fails, or the DOM backend reports an {@code IOException} while reading the
     *         XML file
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(source, cs.source);

        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            // Not deserialize(is, ...): a file is a whole document, so the StAX reader may also check the epilog.
            return read(is, config, null, targetType, true);
        } finally {
            IOUtil.closeQuietly(is);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(File, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
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
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if opening {@code source} fails, or the DOM backend reports an {@code IOException} while reading the
     *         XML file
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws ParsingException, UncheckedIOException {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from an input stream and parses it using StAX or DOM parsing.
     * With {@link XmlParserType#StAX} the stream is not closed and content after the root element is not
     * validated: parsing stops at the end of the root element, because the stream may be an open pipe or
     * socket. The stream is still read in blocks, so an unknown amount of what follows the root element has
     * been buffered when this method returns (often the whole rest of a small stream) and the stream is left
     * at an arbitrary position -- a following document cannot be read from it. {@link XmlParserType#DOM} hands
     * the stream to the JAXP parser, which reads it to the end, rejects content after the root element, and
     * closes the stream when the document ends.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
     *
     * try (InputStream in = new FileInputStream("user.xml")) {
     *     Type<User> userType = Type.of(User.class);
     *     User user = parser.deserialize(in, null, userType);
     * }
     *
     * // With configuration
     * try (InputStream in = java.net.URI.create("http://api.example.com/user.xml").toURL().openStream()) {
     *     XmlDeserConfig config = new XmlDeserConfig()
     *         .setIgnoreUnmatchedProperty(true);
     *     User user = parser.deserialize(in, config, Type.of(User.class));
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(source, cs.source);

        return read(source, config, null, targetType, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(InputStream, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
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
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws ParsingException, UncheckedIOException {
        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a Reader using StAX or DOM parsing. With
     * {@link XmlParserType#StAX} the reader is used directly and not closed, and content after the root
     * element is not validated: parsing stops at the end of the root element, because the reader may be an open
     * pipe. The reader is still read in blocks, so an unknown amount of what follows the root element has been
     * buffered when this method returns (often the whole rest of a small reader) and the reader is left at an
     * arbitrary position -- a following document cannot be read from it. {@link XmlParserType#DOM} hands the
     * reader to the JAXP parser, which reads it to the end, rejects content after the root element, and closes
     * it when the document ends.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
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
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(source, cs.source);

        // BufferedReader? will the target parser create the BufferedReader internally?
        return read(source, config, null, targetType, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(Reader, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
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
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws ParsingException, UncheckedIOException {
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
     * XmlParser parser = new XmlParserImpl(XmlParserType.DOM);
     *
     * // Parse XML to DOM
     * Document doc = XmlUtil.createDOMParser().parse(new File("user.xml"));
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
     * @param source the DOM node containing XML data; must not be {@code null}. A {@link org.w3c.dom.Document}
     *        is read as its document element
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetType, cs.targetType);

        return readByDOMParser(source, config, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(Node, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.DOM);
     *
     * Document doc = XmlUtil.createDOMParser().parse(new File("user.xml"));
     *
     * User user = parser.deserialize(doc.getDocumentElement(), null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data; must not be {@code null}. A {@link org.w3c.dom.Document}
     *        is read as its document element
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, ParsingException {
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
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
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
     * @throws ParsingException if no matching type is found in nodeTypes or XML is malformed
     * @throws UncheckedIOException if opening {@code source} fails, or the DOM backend reports an {@code IOException} while reading the
     *         XML file
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws ParsingException, UncheckedIOException {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            // Not deserialize(is, ...): a file is a whole document, so the StAX reader may also check the epilog.
            return read(is, config, nodeTypes, null, true);
        } finally {
            IOUtil.closeQuietly(is);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from an input stream and uses node class mappings for dynamic type resolution.
     * With {@link XmlParserType#StAX} the stream is not closed and content after the root element is not
     * validated: parsing stops at the end of the root element, because the stream may be an open pipe or
     * socket. The stream is still read in blocks, so an unknown amount of what follows the root element has
     * been buffered when this method returns (often the whole rest of a small stream) and the stream is left
     * at an arbitrary position -- a following document cannot be read from it. {@link XmlParserType#DOM} hands
     * the stream to the JAXP parser, which reads it to the end, rejects content after the root element, and
     * closes the stream when the document ends.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
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
     * @throws ParsingException if no matching type is found in nodeTypes or XML is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws ParsingException, UncheckedIOException {
        return read(source, config, nodeTypes, null, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a Reader and uses node class mappings for dynamic type resolution.
     * With {@link XmlParserType#StAX} the reader is used directly and not closed and content after the root
     * element is not validated, but it may already have been buffered, so the reader is left at an arbitrary
     * position; {@link XmlParserType#DOM} reads the reader to the end, rejects content after the root element,
     * and closes it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
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
     * @throws ParsingException if no matching type is found in nodeTypes or XML is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws ParsingException, UncheckedIOException {
        return read(source, config, nodeTypes, null, false);
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
     * XmlParser parser = new XmlParserImpl(XmlParserType.DOM);
     *
     * // Define node type mappings
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("product", Type.of(Product.class));
     * nodeTypes.put("service", Type.of(Service.class));
     *
     * Document doc = XmlUtil.createDOMParser().parse(new File("items.xml"));
     *
     * Object item = parser.deserialize(doc.getDocumentElement(), null, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data; must not be {@code null}. A {@link org.w3c.dom.Document}
     *        is read as its document element
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if no matching type is found in nodeTypes or XML structure is invalid
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws IllegalArgumentException, ParsingException {
        N.checkArgNotNull(source, cs.source);

        // Resolved on the element itself: a Document (what DocumentBuilder.parse returns) is named #document and
        // carries no attributes, so the lookup could only fail on it.
        final Node sourceElement = toElementNode(source);
        Type<? extends T> targetType = null;

        if (N.notEmpty(nodeTypes)) {
            String nodeName = XmlUtil.getAttribute(sourceElement, XmlConstants.NAME);

            if (Strings.isEmpty(nodeName)) {
                nodeName = sourceElement.getNodeName();
            }

            targetType = (Type<T>) nodeTypes.get(nodeName);
        }

        if (targetType == null) {
            throw new ParsingException("No target type is specified for xml node: " + sourceElement.getNodeName()); //NOSONAR
        }

        return readByDOMParser(sourceElement, config, targetType);
    }

    /**
     * Reads and deserializes XML from an input stream using either StAX or DOM parsing,
     * resolving the target type from {@code targetType} or, when that is {@code null}, by
     * looking up the root element name in {@code nodeTypes}.
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types, used to
     *        resolve the target type when {@code targetType} is {@code null}
     * @param targetType the explicit target type, or {@code null} to resolve it via {@code nodeTypes}
     * @param boundedSource {@code true} when the source is a whole document this method owns (a String or a
     *        file), in which case the StAX parser type also verifies that nothing but whitespace, comments and
     *        processing instructions follows the root element; {@code false} for a caller-supplied stream that
     *        may stay open and may carry further documents
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if no target type can be resolved, the parser type is unsupported,
     *         or the XML is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @SuppressWarnings("unchecked")
    protected <T> T read(final InputStream source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType,
            final boolean boundedSource) throws ParsingException, UncheckedIOException {
        final XmlDeserConfig configToUse = check(config);

        switch (parserType) {
            case StAX:
                XMLStreamReader xmlReader = null;

                try {
                    xmlReader = createXMLStreamReader(source);

                    moveToRootElement(xmlReader);

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        String nodeName = null;

                        if (xmlReader.getAttributeCount() > 0) {
                            nodeName = xmlReader.getAttributeValue(null, XmlConstants.NAME);
                        }

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = xmlReader.getLocalName();
                        }

                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target type is specified for xml node: " + xmlReader.getLocalName());
                    }

                    final T staxResult = readByStreamParser(xmlReader, configToUse, targetType);

                    if (boundedSource) {
                        // A second root element or trailing text is an error for a whole document; a caller's open
                        // stream is deliberately not drained (see the deserialize(InputStream, ...) javadoc).
                        drainEpilog(xmlReader);
                    }

                    return staxResult;
                } catch (final XMLStreamException e) {
                    throw new ParsingException(e);
                } finally {
                    if (xmlReader != null) {
                        try {
                            xmlReader.close();
                        } catch (final XMLStreamException e) {
                            // ignore
                        }
                    }
                }

            case DOM: //NOSONAR
                final DocumentBuilder docBuilder = XmlUtil.createContentParser();

                try {
                    final Document doc = docBuilder.parse(source);
                    final Node node = doc.getDocumentElement();

                    if (node == null) {
                        throw new ParsingException("No root element found in XML document");
                    }

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        String nodeName = XmlUtil.getAttribute(node, XmlConstants.NAME);

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = node.getNodeName();
                        }

                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target type is specified for xml node: " + node.getNodeName());
                    }

                    return readByDOMParser(node, configToUse, targetType);
                } catch (final SAXException e) {
                    throw new ParsingException(e);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    XmlUtil.recycleContentParser(docBuilder);
                }

            default:
                throw new ParsingException("Unsupported parser: " + parserType);
        }
    }

    /**
     * Reads and deserializes XML from a reader using either StAX or DOM parsing,
     * resolving the target type from {@code targetType} or, when that is {@code null}, by
     * looking up the root element name in {@code nodeTypes}.
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types, used to
     *        resolve the target type when {@code targetType} is {@code null}
     * @param targetType the explicit target type, or {@code null} to resolve it via {@code nodeTypes}
     * @param boundedSource {@code true} when the source is a whole document this method owns (a String or a
     *        file), in which case the StAX parser type also verifies that nothing but whitespace, comments and
     *        processing instructions follows the root element; {@code false} for a caller-supplied reader that
     *        may stay open and may carry further documents
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if no target type can be resolved, the parser type is unsupported,
     *         or the XML is malformed
     * @throws UncheckedIOException if the DOM backend reports an {@code IOException} while reading XML from {@code source}
     */
    @SuppressWarnings("unchecked")
    protected <T> T read(final Reader source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType,
            final boolean boundedSource) throws ParsingException, UncheckedIOException {
        final XmlDeserConfig configToUse = check(config);

        switch (parserType) {
            case StAX:
                XMLStreamReader xmlReader = null;

                try {
                    xmlReader = createXMLStreamReader(source);

                    moveToRootElement(xmlReader);

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        String nodeName = null;

                        if (xmlReader.getAttributeCount() > 0) {
                            nodeName = xmlReader.getAttributeValue(null, XmlConstants.NAME);
                        }

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = xmlReader.getLocalName();
                        }

                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target type is specified for xml node: " + xmlReader.getLocalName());
                    }

                    final T staxResult = readByStreamParser(xmlReader, configToUse, targetType);

                    if (boundedSource) {
                        // A second root element or trailing text is an error for a whole document; a caller's open
                        // stream is deliberately not drained (see the deserialize(InputStream, ...) javadoc).
                        drainEpilog(xmlReader);
                    }

                    return staxResult;
                } catch (final XMLStreamException e) {
                    throw new ParsingException(e);
                } finally {
                    if (xmlReader != null) {
                        try {
                            xmlReader.close();
                        } catch (final XMLStreamException e) {
                            // ignore
                        }
                    }
                }

            case DOM: //NOSONAR
                final DocumentBuilder docBuilder = XmlUtil.createContentParser();

                try {
                    final Document doc = docBuilder.parse(new InputSource(source));
                    final Node node = doc.getDocumentElement();

                    if (node == null) {
                        throw new ParsingException("No root element found in XML document");
                    }

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        String nodeName = XmlUtil.getAttribute(node, XmlConstants.NAME);

                        if (Strings.isEmpty(nodeName)) {
                            nodeName = node.getNodeName();
                        }

                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target type is specified for xml node: " + node.getNodeName());
                    }

                    return readByDOMParser(node, configToUse, targetType);
                } catch (final SAXException e) {
                    throw new ParsingException(e);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    XmlUtil.recycleContentParser(docBuilder);
                }

            default:
                throw new ParsingException("Unsupported parser: " + parserType);
        }
    }

    /**
     * Deserializes an object from the current position of an XML stream reader into the given target type.
     *
     * @param <T> the type of the target object
     * @param xmlReader the XML stream reader positioned at the element to deserialize
     * @param config the deserialization configuration
     * @param targetType the type of the object to create
     * @return the deserialized object of type {@code T}
     * @throws XMLStreamException if advancing {@code xmlReader} through element names, attributes or values encounters malformed XML or
     *         cannot read its underlying input
     */
    protected <T> T readByStreamParser(final XMLStreamReader xmlReader, final XmlDeserConfig config, final Type<? extends T> targetType)
            throws XMLStreamException {
        return readByStreamParser(xmlReader, config, null, null, targetType);
    }

    /**
     * Event filter hiding comments and processing instructions. Neither is character data, so
     * {@code <text>a<?pi x?>b</text>} must read back as {@code "ab"}: without the filter the text-coalescing
     * loop stops at the processing instruction and everything before it is dropped.
     */
    private static final StreamFilter NO_COMMENT_OR_PI = reader -> reader.getEventType() != XMLStreamConstants.COMMENT
            && reader.getEventType() != XMLStreamConstants.PROCESSING_INSTRUCTION;

    /**
     * {@inheritDoc}
     *
     * <p>This implementation also filters out processing instructions, so that text separated by one is
     * coalesced instead of truncated.</p>
     */
    @Override
    protected XMLStreamReader createXMLStreamReader(final Reader br) {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(br), NO_COMMENT_OR_PI);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation also filters out processing instructions, so that text separated by one is
     * coalesced instead of truncated.</p>
     */
    @Override
    protected XMLStreamReader createXMLStreamReader(final InputStream is) {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(is), NO_COMMENT_OR_PI);
    }

    /** Maximum permitted XML element-nesting depth. Defends against StackOverflowError on hostile input. */
    private static final int MAX_XML_NESTING_DEPTH = 1000;

    /** Per-thread XML nesting depth counter. */
    private static final ThreadLocal<int[]> XML_NESTING_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /**
     * @throws ParsingException if the XML nesting depth exceeds the configured maximum
     */
    private static void enterXmlNesting() throws ParsingException {
        final int[] depth = XML_NESTING_DEPTH.get();
        if (++depth[0] > MAX_XML_NESTING_DEPTH) {
            depth[0]--;
            throw new ParsingException("XML nesting depth exceeded " + MAX_XML_NESTING_DEPTH + " (defends against stack-overflow DoS)");
        }
    }

    private static void exitXmlNesting() {
        final int[] depth = XML_NESTING_DEPTH.get();
        if (--depth[0] <= 0) {
            depth[0] = 0;
            XML_NESTING_DEPTH.remove();
        }
    }

    /**
     * Deserializes an object from the current position of an XML stream reader, tracking XML
     * nesting depth to guard against stack-overflow on hostile input.
     * Ignored map and map-entity properties are consumed without resolving their type attributes
     * or converting their text to the configured value type.
     *
     * @param <T> the type of the target object
     * @param xmlReader the XML stream reader positioned at the element to deserialize
     * @param config the deserialization configuration
     * @param propInfo the property metadata for the value being read, or {@code null} at the root
     * @param propType the declared property type, or {@code null} if not applicable
     * @param targetType the type of the object to create
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML nesting depth exceeds the allowed maximum
     * @throws XMLStreamException if advancing {@code xmlReader} through element names, attributes or values encounters malformed XML or
     *         cannot read its underlying input
     */
    protected <T> T readByStreamParser(final XMLStreamReader xmlReader, final XmlDeserConfig config, PropInfo propInfo, Type<?> propType, Type<?> targetType)
            throws ParsingException, XMLStreamException {
        enterXmlNesting();
        try {
            return readByStreamParserBody(xmlReader, config, propInfo, propType, targetType);
        } finally {
            exitXmlNesting();
        }
    }

    /**
     * @throws XMLStreamException if advancing the XML stream reader fails
     * @throws ParsingException if an unmatched property is rejected, element structure is invalid, the XML ends prematurely, or the target class cannot be parsed as an array, collection, map, or bean
     */
    @SuppressWarnings({ "null", "deprecation" })
    private <T> T readByStreamParserBody(final XMLStreamReader xmlReader, final XmlDeserConfig config, PropInfo propInfo, Type<?> propType, Type<?> targetType)
            throws XMLStreamException, ParsingException {
        if (targetType.javaType().equals(Object.class)) {
            targetType = mapEntityType;
        }

        final XmlDeserConfig configToUse = check(config);
        final boolean hasPropTypes = configToUse.hasValueTypes();

        if (hasPropTypes && xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            targetType = configToUse.getValueType(xmlReader.getLocalName(), targetType);
        }

        final Class<?> targetClass = targetType.javaType();
        final SerializationType serializationType = getDeserializationType(targetType);

        String propName = null;
        // The element's own name, which differs from the property name for a tagByPropertyName=false document
        // (<property name="contact">). Only read in the iteration that sets it, together with propName.
        String propElementName = null;
        Object propValue = null;
        String text = null;
        StringBuilder sb = null;
        boolean advanceEvent = true;

        switch (serializationType) {
            case ENTITY: {
                // Start from a clean property tracker: the caller passes ITS OWN propInfo/propType down for the
                // MAP/MAP_ENTITY/ARRAY/COLLECTION key/value types, and a nested bean read would otherwise treat
                // this element's text (the indentation of a pretty-printed empty bean) as a value of the OUTER
                // property -- setPropValue then gets the nested instance and throws ClassCastException.
                propInfo = null;
                propType = null;

                final boolean ignoreUnmatchedProperty = configToUse.isIgnoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
                final Object result = beanInfo.createBeanResult();
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader
                        .hasNext(); event = advanceEvent ? xmlReader.next() : xmlReader.getEventType(), advanceEvent = true) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propElementName = xmlReader.getLocalName();
                                propName = resolveElementName(xmlReader);
                                propInfo = beanInfo.getPropInfo(propName);

                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    // Clear propInfo so the ignored property's content is skipped the same way as an
                                    // unmatched property; otherwise the CHARACTERS handler would call valueOf on the
                                    // unresolved (null or stale) propType.
                                    propInfo = null;

                                    continue;
                                }

                                if (propInfo == null) {
                                    if (ignoreUnmatchedProperty) {
                                        continue;
                                    } else {
                                        throw new ParsingException("Unknown property element: " + propName + " for class: " + targetClass);
                                    }
                                }

                                propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                                if (propType == null) {
                                    if (propInfo.jsonXmlType.isSerializable()) {
                                        propType = propInfo.jsonXmlType;
                                    } else {
                                        attrCount = xmlReader.getAttributeCount();

                                        if (attrCount == 1) {
                                            if (XmlConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                                propType = resolveTypeAttribute(xmlReader.getAttributeValue(0));
                                            }
                                        } else if (attrCount > 1) {
                                            for (int i = 0; i < attrCount; i++) {
                                                if (XmlConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                    propType = resolveTypeAttribute(xmlReader.getAttributeValue(i));

                                                    break;
                                                }
                                            }
                                        }

                                        // The attribute wins only when it adds information: a bare
                                        // type="ArrayList" must not erase a declared List<Inner>.
                                        propType = chooseDeclaredType(propType, propInfo.jsonXmlType);
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
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObject()) {
                                        propValue = readByStreamParser(xmlReader, configToUse, propInfo, propType, propType.isObject() ? mapType : propType);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        final Collection<Object> c = Collection.class.isAssignableFrom(propType.javaType())
                                                ? N.newCollection((Class<Collection>) propType.javaType())
                                                : new ArrayList<>();

                                        final Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XmlConstants.IS_NULL))) {
                                                c.add(null);

                                                nextStructuralEvent(xmlReader);
                                            } else if (isScalarEleElement(xmlReader, propType.elementType())) {
                                                // <e> wrapper written by writeElement for a scalar element of a
                                                // mixed array/collection value.
                                                c.add(readScalarElement(xmlReader, propType.elementType()));
                                            } else {
                                                c.add(readByStreamParser(xmlReader, configToUse, null, propType, propEleType));
                                            }
                                        } while (xmlReader.hasNext() && nextStructuralEvent(xmlReader) == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.isArray() ? collectionToArray(c, propType) : c;
                                    }
                                }

                                // Matched on the ELEMENT name: with tagByPropertyName=false the property name comes
                                // from the `name` attribute, so </property> would not equal propName.
                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propElementName)) {
                                    if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
                                            || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                        // ignore.
                                    } else {
                                        propInfo.setPropValue(result, propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                    propInfo = null;
                                } else {
                                    throw new ParsingException("Unknown parser error at element: " + describeCurrentEvent(xmlReader)); //NOSONAR
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.SPACE:
                        case XMLStreamConstants.CDATA:
                        case XMLStreamConstants.CHARACTERS: {
                            if (propInfo != null) {
                                text = xmlReader.getText();

                                if (text != null && isTextEvent(event = xmlReader.next())) {
                                    do {
                                        if (sb == null) {
                                            sb = new StringBuilder(text.length() * 2);
                                            sb.append(text);
                                        } else if (sb.isEmpty()) {
                                            sb.append(text);
                                        }

                                        sb.append(xmlReader.getText());
                                    } while (isTextEvent(event = xmlReader.next()));

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                // Indentation before a nested value is structural. Replay its START_ELEMENT;
                                // whitespace ending at the property end is scalar content and must be kept.
                                if (event == XMLStreamConstants.START_ELEMENT && text.isBlank()) {
                                    advanceEvent = false;
                                    break;
                                }

                                propValue = propInfo.hasFormat ? propInfo.readPropValue(text) : propType.valueOf(text);

                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
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
                                if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
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

                throw new ParsingException("Unknown parser error: unexpected end of XML stream"); //NOSONAR
            }

            case MAP: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(0).isObject()) {
                    keyType = propInfo.jsonXmlType.parameterTypes().get(0);
                } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(0).isObject()) {
                    keyType = propType.parameterTypes().get(0);
                } else {
                    if (configToUse.getMapKeyType() != null && !configToUse.getMapKeyType().isObject()) {
                        keyType = configToUse.getMapKeyType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                        keyType = targetType.parameterTypes().get(0);
                    }
                }

                final boolean isStringKey = keyType.javaType() == String.class;
                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(1).isObject()) {
                    valueType = propInfo.jsonXmlType.parameterTypes().get(1);
                } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    valueType = propType.parameterTypes().get(1);
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObject()) {
                        valueType = configToUse.getMapValueType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                        valueType = targetType.parameterTypes().get(1);
                    }
                }

                @SuppressWarnings("rawtypes")
                final Map<Object, Object> map = N.newMap((Class<Map>) targetClass);
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader
                        .hasNext(); event = advanceEvent ? xmlReader.next() : xmlReader.getEventType(), advanceEvent = true) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propElementName = xmlReader.getLocalName();
                                // Same rule as the ENTITY/MAP_ENTITY branches and as the DOM reader: writeMap
                                // never emits a `name` attribute, so this only changes a bean written with
                                // tagByPropertyName=false that is being read as a map (<property name="a">),
                                // whose keys were otherwise all the literal element name "property".
                                propName = resolveElementName(xmlReader);

                                if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    continue;
                                }

                                propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                                if (propType == null) {
                                    attrCount = xmlReader.getAttributeCount();

                                    if (attrCount == 1) {
                                        if (XmlConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                            propType = resolveTypeAttribute(xmlReader.getAttributeValue(0));
                                        }
                                    } else if (attrCount > 1) {
                                        for (int i = 0; i < attrCount; i++) {
                                            if (XmlConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                propType = resolveTypeAttribute(xmlReader.getAttributeValue(i));

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
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObject()) {
                                        propValue = readByStreamParser(xmlReader, configToUse, null, propType, propType.isObject() ? mapType : propType);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        final Collection<Object> c = Collection.class.isAssignableFrom(propType.javaType())
                                                ? N.newCollection((Class<Collection>) propType.javaType())
                                                : new ArrayList<>();

                                        final Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XmlConstants.IS_NULL))) {
                                                c.add(null);

                                                nextStructuralEvent(xmlReader);
                                            } else if (isScalarEleElement(xmlReader, propType.elementType())) {
                                                // <e> wrapper written by writeElement for a scalar element of a
                                                // mixed array/collection value.
                                                c.add(readScalarElement(xmlReader, propType.elementType()));
                                            } else {
                                                c.add(readByStreamParser(xmlReader, configToUse, null, propType, propEleType));
                                            }
                                        } while (xmlReader.hasNext() && nextStructuralEvent(xmlReader) == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.isArray() ? collectionToArray(c, propType) : c;
                                    }
                                }

                                // Matched on the ELEMENT name: the key may have come from a `name` attribute.
                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propElementName)) {
                                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore.
                                    } else {
                                        map.put(isStringKey ? propName : keyType.valueOf(propName), propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                } else {
                                    throw new ParsingException("Unknown parser error at element: " + describeCurrentEvent(xmlReader));
                                }

                            }

                            break;
                        }

                        case XMLStreamConstants.SPACE:
                        case XMLStreamConstants.CDATA:
                        case XMLStreamConstants.CHARACTERS: {
                            if (propName == null || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // Skip stray or ignored text before value conversion, matching the DOM parser.
                                break;
                            }

                            text = xmlReader.getText();

                            if (text != null && isTextEvent(event = xmlReader.next())) {
                                do {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                } while (isTextEvent(event = xmlReader.next()));

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            // Indentation before a nested value is structural. Replay its START_ELEMENT;
                            // whitespace ending at the property end is scalar content and must be kept.
                            if (event == XMLStreamConstants.START_ELEMENT && text.isBlank()) {
                                advanceEvent = false;
                                break;
                            }

                            propValue = propType.valueOf(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
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

                throw new ParsingException("Unknown parser error: unexpected end of XML stream");
            }

            case MAP_ENTITY: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(1).isObject()) {
                    valueType = propInfo.jsonXmlType.parameterTypes().get(1);
                } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    valueType = propType.parameterTypes().get(1);
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObject()) {
                        valueType = configToUse.getMapValueType();
                    }
                }

                final MapEntity mapEntity = new MapEntity(xmlReader.getLocalName());
                int attrCount = 0;

                for (int event = xmlReader.next(); xmlReader
                        .hasNext(); event = advanceEvent ? xmlReader.next() : xmlReader.getEventType(), advanceEvent = true) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                propElementName = xmlReader.getLocalName();
                                propName = resolveElementName(xmlReader);
                                if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    continue;
                                }

                                propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                                if (propType == null) {
                                    attrCount = xmlReader.getAttributeCount();

                                    if (attrCount == 1) {
                                        if (XmlConstants.TYPE.equals(xmlReader.getAttributeLocalName(0))) {
                                            propType = resolveTypeAttribute(xmlReader.getAttributeValue(0));
                                        }
                                    } else if (attrCount > 1) {
                                        for (int i = 0; i < attrCount; i++) {
                                            if (XmlConstants.TYPE.equals(xmlReader.getAttributeLocalName(i))) {
                                                propType = resolveTypeAttribute(xmlReader.getAttributeValue(i));

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
                                    if (propType.isMap() || propType.isBean() || propType.isMapEntity() || propType.isObject()) {
                                        propValue = readByStreamParser(xmlReader, configToUse, null, propType, propType.isObject() ? mapEntityType : propType);

                                        for (int startCount = 0, e = xmlReader.next();; e = xmlReader.next()) {
                                            startCount += (e == XMLStreamConstants.START_ELEMENT) ? 1 : (e == XMLStreamConstants.END_ELEMENT ? -1 : 0);

                                            if (startCount < 0 || !xmlReader.hasNext()) {
                                                break;
                                            }
                                        }

                                    } else {
                                        @SuppressWarnings("rawtypes")
                                        final Collection<Object> c = Collection.class.isAssignableFrom(propType.javaType())
                                                ? N.newCollection((Class<Collection>) propType.javaType())
                                                : new ArrayList<>();

                                        final Type<?> propEleType = getPropEleType(propType);

                                        do {
                                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XmlConstants.IS_NULL))) {
                                                c.add(null);

                                                nextStructuralEvent(xmlReader);
                                            } else if (isScalarEleElement(xmlReader, propType.elementType())) {
                                                // <e> wrapper written by writeElement for a scalar element of a
                                                // mixed array/collection value.
                                                c.add(readScalarElement(xmlReader, propType.elementType()));
                                            } else {
                                                c.add(readByStreamParser(xmlReader, configToUse, null, propType, propEleType));
                                            }
                                        } while (xmlReader.hasNext() && nextStructuralEvent(xmlReader) == XMLStreamConstants.START_ELEMENT);

                                        propValue = propType.isArray() ? collectionToArray(c, propType) : c;
                                    }
                                }

                                // Matched on the ELEMENT name: with tagByPropertyName=false the property name comes
                                // from the `name` attribute, so </property> would not equal propName.
                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT && xmlReader.getLocalName().equals(propElementName)) {
                                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                        // ignore.
                                    } else {
                                        mapEntity.set(propName, propValue);
                                    }

                                    propName = null;
                                    propValue = null;
                                } else {
                                    throw new ParsingException("Unknown parser error at element: " + describeCurrentEvent(xmlReader));
                                }

                            }

                            break;
                        }

                        case XMLStreamConstants.SPACE:
                        case XMLStreamConstants.CDATA:
                        case XMLStreamConstants.CHARACTERS: {
                            if (propName == null || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                // Skip stray or ignored text before value conversion, matching the DOM parser.
                                break;
                            }

                            text = xmlReader.getText();

                            if (text != null && isTextEvent(event = xmlReader.next())) {
                                do {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                } while (isTextEvent(event = xmlReader.next()));

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            // Indentation before a nested value is structural. Replay its START_ELEMENT;
                            // whitespace ending at the property end is scalar content and must be kept.
                            if (event == XMLStreamConstants.START_ELEMENT && text.isBlank()) {
                                advanceEvent = false;
                                break;
                            }

                            propValue = propType.valueOf(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
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

                throw new ParsingException("Unknown parser error: unexpected end of XML stream");
            }

            case ARRAY: {
                Type<?> eleType = null;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = Type.of(propInfo.clazz.getComponentType());
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObject()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = targetType.isArray() ? targetType.elementType() : strType;
                    }
                }

                final List<Object> list = Objectory.createList();

                try {
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        switch (event) {
                            case XMLStreamConstants.START_ELEMENT: {
                                if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XmlConstants.IS_NULL))) {
                                    list.add(null);

                                    // consume the null element's END_ELEMENT so the loop update doesn't mistake it
                                    // for the enclosing array's end and truncate the remaining elements.
                                    nextStructuralEvent(xmlReader);
                                } else if (isScalarEleElement(xmlReader, eleType)) {
                                    // <e> wrapper written by writeElement for a scalar element of a mixed array.
                                    list.add(readScalarElement(xmlReader, eleType));
                                } else if (String.class == eleType.javaType() || Object.class == eleType.javaType()) {
                                    list.add(readByStreamParser(xmlReader, configToUse, null, eleType, mapType));
                                } else {
                                    list.add(readByStreamParser(xmlReader, configToUse, null, eleType, eleType));
                                }

                                break;
                            }

                            // simple array with sample format <array>[1, 2,
                            // 3...]</array>
                            case XMLStreamConstants.SPACE:
                            case XMLStreamConstants.CDATA:
                            case XMLStreamConstants.CHARACTERS: {
                                if (xmlReader.isWhiteSpace()) {
                                    break;
                                }

                                text = xmlReader.getText();

                                if (text != null && isTextEvent(event = xmlReader.next())) {
                                    do {
                                        if (sb == null) {
                                            sb = new StringBuilder(text.length() * 2);
                                            sb.append(text);
                                        } else if (sb.isEmpty()) {
                                            sb.append(text);
                                        }

                                        sb.append(xmlReader.getText());
                                    } while (isTextEvent(event = xmlReader.next()));

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                if (eleType.javaType() == String.class || eleType.javaType() == Object.class) {
                                    propValue = targetType.valueOf(text);
                                } else {
                                    propValue = jsonParser.deserialize(text, JsonDeserConfig.create().setElementType(eleType.javaType()), targetType);
                                }

                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (propValue != null) {
                                        return (T) propValue;
                                    } else {
                                        return collectionToArray(list, targetType);
                                    }
                                }

                                break;
                            }

                            case XMLStreamConstants.END_ELEMENT: {
                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return collectionToArray(list, targetType);
                                }
                            }

                            default:
                                // continue;
                        }
                    }

                } finally {
                    Objectory.recycle(list);
                }

                throw new ParsingException("Unknown parser error: unexpected end of XML stream");
            }

            case COLLECTION: {
                Type<?> eleType = defaultValueType;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = Type.of(propInfo.clazz.getComponentType());
                } else if (propType != null && propType.parameterTypes().size() == 1 && Collection.class.isAssignableFrom(propType.javaType())
                        && !propType.parameterTypes().get(0).isObject()) {
                    eleType = propType.parameterTypes().get(0);
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObject()) {
                        eleType = configToUse.getElementType();
                    } else if (targetType.elementType() != null && !targetType.elementType().isObject()) {
                        eleType = targetType.elementType();
                    }
                }

                @SuppressWarnings("rawtypes")
                final Collection<Object> result = N.newCollection((Class<Collection>) targetClass);

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            if (xmlReader.getAttributeCount() > 0 && TRUE.equals(xmlReader.getAttributeValue(null, XmlConstants.IS_NULL))) {
                                result.add(null);

                                // consume the null element's END_ELEMENT so the loop update doesn't mistake it
                                // for the enclosing collection's end and truncate the remaining elements.
                                nextStructuralEvent(xmlReader);
                            } else if (isScalarEleElement(xmlReader, eleType)) {
                                // <e> wrapper written by writeElement for a scalar element of a mixed collection.
                                result.add(readScalarElement(xmlReader, eleType));
                            } else if (String.class == eleType.javaType() || Object.class == eleType.javaType()) {
                                result.add(readByStreamParser(xmlReader, configToUse, null, eleType, mapType));
                            } else {
                                result.add(readByStreamParser(xmlReader, configToUse, null, eleType, eleType));
                            }

                            break;
                        }

                        // simple list with sample format <list>[1, 2, 3...]</list>
                        case XMLStreamConstants.SPACE:
                        case XMLStreamConstants.CDATA:
                        case XMLStreamConstants.CHARACTERS: {
                            if (xmlReader.isWhiteSpace()) {
                                break;
                            }

                            text = xmlReader.getText();

                            if (text != null && isTextEvent(event = xmlReader.next())) {
                                do {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else if (sb.isEmpty()) {
                                        sb.append(text);
                                    }

                                    sb.append(xmlReader.getText());
                                } while (isTextEvent(event = xmlReader.next()));

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            if (eleType.javaType() == String.class || eleType.javaType() == Object.class) {
                                propValue = targetType.valueOf(text);
                            } else {
                                propValue = jsonParser.deserialize(text, JsonDeserConfig.create().setElementType(eleType.javaType()), targetType);
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

                throw new ParsingException("Unknown parser error: unexpected end of XML stream");
            }

            default:
                throw new ParsingException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only object array, collection, map and bean types are supported");
        }
    }

    /**
     * Returns the property name of the element the reader is positioned on: the {@code name} attribute when the
     * document was written with {@code tagByPropertyName=false} ({@code <property name="age">}), and the local
     * name otherwise. This is the convention the root-node lookup (see {@code read}) and the DOM reader already
     * use, so the StAX reader can read back what {@code XmlSerConfig.setTagByPropertyName(false)} writes. The
     * decision is per element here, while the DOM reader takes it per bean element - from whether that element
     * is the generic {@code <bean name="...">} form - and falls back to a child's own element name when the
     * child carries no {@code name} attribute: the two agree on every document either writer produces, but not
     * on a hand-written one that mixes the two shapes.
     *
     * @param xmlReader the stream reader positioned on a {@code START_ELEMENT} event
     * @return the resolved property name
     */
    private static String resolveElementName(final XMLStreamReader xmlReader) {
        if (xmlReader.getAttributeCount() > 0) {
            final String nameAttr = xmlReader.getAttributeValue(null, XmlConstants.NAME);

            if (Strings.isNotEmpty(nameAttr)) {
                return nameAttr;
            }
        }

        return xmlReader.getLocalName();
    }

    /**
     * Returns whether the element the reader is positioned on is an {@code <e>} scalar wrapper written by
     * {@link #writeElement} for a mixed array or collection.
     *
     * <p>A declared element type that is a bean, map or map entity wins, so a {@code List<E>} of a bean class
     * named {@code E} is still read as beans. When the declared element type is {@code Object} -- which is the
     * case the {@code <e>} wrapper exists for -- the name alone decides: a bean named {@code E} inside an
     * {@code Object}-typed mixed collection is read as its text. The name is all this can test - a stream reader
     * cannot look ahead to see whether the element has children - so {@link #isScalarEleNode} applies the same
     * rule on the DOM side rather than a stricter one the two backends would disagree on.</p>
     *
     * @param xmlReader the stream reader positioned on a {@code START_ELEMENT} event
     * @param eleType the declared element type of the enclosing array or collection, or {@code null}
     * @return {@code true} if the element is to be read as a scalar
     */
    private static boolean isScalarEleElement(final XMLStreamReader xmlReader, final Type<?> eleType) {
        return XmlConstants.E.equals(xmlReader.getLocalName()) && (eleType == null || !eleType.isBean() && !eleType.isMap() && !eleType.isMapEntity());
    }

    /**
     * Reads an {@code <e>} scalar element and converts its text. The element's own {@code type} attribute wins
     * when it names an allowed type; otherwise {@code eleType} is used, and an {@code Object} element type
     * yields a String -- which is what the DOM backend returns for the same document.
     *
     * @param xmlReader the stream reader positioned on the element's {@code START_ELEMENT} event; it is left on
     *        the matching {@code END_ELEMENT}
     * @param eleType the declared element type, or {@code null}
     * @return the converted value, or {@code null} for an element with no text
     * @throws XMLStreamException if advancing {@code xmlReader} through the scalar element encounters malformed XML or cannot read its
     *         underlying input
     */
    private Object readScalarElement(final XMLStreamReader xmlReader, final Type<?> eleType) throws XMLStreamException {
        Type<?> valueType = xmlReader.getAttributeCount() > 0 ? resolveTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE)) : null;

        if (valueType == null || valueType.isObject()) {
            valueType = eleType == null || eleType.isObject() ? strType : eleType;
        }

        String text = null;
        StringBuilder sb = null;

        for (int depth = 1; xmlReader.hasNext();) {
            final int event = xmlReader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (--depth == 0) {
                    break;
                }
            } else if (isTextEvent(event)) {
                if (text == null) {
                    text = xmlReader.getText();
                } else {
                    if (sb == null) {
                        sb = new StringBuilder(text);
                    }

                    sb.append(xmlReader.getText());
                }
            }
        }

        if (sb != null) {
            text = sb.toString();
        }

        return text == null ? null : valueType.valueOf(text);
    }

    /**
     * Describes the event the reader is positioned on for an error message. {@code getLocalName()} is only legal
     * on an element event, so it cannot be used unconditionally: on a text event it throws
     * {@link IllegalStateException} and hides the parsing error being reported.
     *
     * @param xmlReader the stream reader
     * @return a short description of the current event
     */
    private static String describeCurrentEvent(final XMLStreamReader xmlReader) {
        final int event = xmlReader.getEventType();

        if (event == XMLStreamConstants.START_ELEMENT || event == XMLStreamConstants.END_ELEMENT) {
            return xmlReader.getLocalName();
        }

        return isTextEvent(event) ? "text: " + xmlReader.getText() : "event type " + event;
    }

    /**
     * Checks whether the given StAX event carries element text. Non-coalescing StAX providers
     * (e.g. Woodstox) report CDATA sections as {@code CDATA} events rather than {@code CHARACTERS},
     * so both must be treated as text or CDATA content would be silently dropped.
     *
     * @param event the StAX event type
     * @return {@code true} if the event is {@code CHARACTERS}, {@code CDATA} or {@code SPACE}
     */
    private static boolean isTextEvent(final int event) {
        return event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA || event == XMLStreamConstants.SPACE;
    }

    /**
     * Returns whether {@code type} carries a type argument or element type more specific than {@code Object}.
     *
     * @param type the type to inspect
     * @return {@code true} if the type describes its elements
     */
    private static boolean hasConcreteTypeParameters(final Type<?> type) {
        for (final Type<?> parameterType : type.parameterTypes()) {
            if (!parameterType.isObject()) {
                return true;
            }
        }

        final Type<?> elementType = type.elementType();

        return elementType != null && !elementType.isObject();
    }

    /**
     * Chooses between a type resolved from a {@code type} attribute and the declared type.
     *
     * <p>The attribute wins only when it adds information. {@code getConcreteClass} answers a raw {@code Class},
     * so a declared {@code List<Inner>} whose element carries {@code type="ArrayList"} (or whose resolved
     * attribute has no type arguments) would be reduced to {@code List<Object>} and every element read as a map.
     * The element type is worth more than the implementation class, so the attribute's concrete class is given
     * up in that case: {@code type="LinkedList"} on a declared {@code List<Inner>} yields the declared type's
     * container (an {@code ArrayList}) holding {@code Inner}s rather than a {@code LinkedList} of maps.</p>
     *
     * @param attrType the type resolved from the {@code type} attribute, or {@code null} if there is none
     * @param declaredType the declared property, value or element type, or {@code null}
     * @return the type to read the value with
     */
    private static Type<?> chooseDeclaredType(final Type<?> attrType, final Type<?> declaredType) {
        if (attrType == null || declaredType == null) {
            return attrType == null ? declaredType : attrType;
        }

        // Rejects a type that is not assignable to the declared one, and an uninstantiable container class.
        if (getConcreteClass(attrType.javaType(), declaredType.javaType()) != attrType.javaType()) {
            return declaredType;
        }

        return hasConcreteTypeParameters(attrType) || !hasConcreteTypeParameters(declaredType) ? attrType : declaredType;
    }

    /**
     * Resolves the type to read a DOM value node with, keeping the type parameters the declared type or the
     * node's {@code type} attribute carries. See {@link #chooseDeclaredType}.
     *
     * @param node the value node, which may carry a {@code type} attribute
     * @param declaredType the declared property, value or element type
     * @return the type to read the node with
     * @throws ParsingException if the node carries a type attribute that is not allowed
     */
    private static Type<?> resolveDeclaredType(final Node node, final Type<?> declaredType) throws ParsingException {
        final Type<?> attrType = resolveTypeAttribute(XmlUtil.getAttribute(node, XmlConstants.TYPE));

        if (attrType == null) {
            // Still routed through getConcreteClass: it fails closed on a type attribute that is not allowed.
            final Class<?> concreteClass = getConcreteClass(node, declaredType.javaType());

            return concreteClass == declaredType.javaType() ? declaredType : Type.of(concreteClass);
        }

        return chooseDeclaredType(attrType, declaredType);
    }

    /**
     * Returns the element a caller-supplied node stands for: the document element of a {@link Document} - which
     * is what {@code DocumentBuilder.parse} hands back, and the node most callers pass - and the node itself
     * otherwise. Reading a {@code Document} as such matched no element and returned {@code null}.
     *
     * @param node the node handed to a {@code deserialize(Node, ..)} method
     * @return the element to read, or {@code node} itself when it is not a document with a document element
     */
    private static Node toElementNode(final Node node) {
        if (node instanceof final Document doc) {
            final Node docElement = doc.getDocumentElement();

            return docElement == null ? node : docElement;
        }

        return node;
    }

    /**
     * Deserializes an object from the given DOM node into the specified target type using DOM-based parsing.
     *
     * @param <T> the type of the target object
     * @param node the DOM node to deserialize; a {@link Document} is read as its document element
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the type of the object to create
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML structure doesn't match the target type
     */
    protected <T> T readByDOMParser(final Node node, final XmlDeserConfig config, final Type<? extends T> targetType) throws ParsingException {
        final XmlDeserConfig configToUse = check(config);

        return readByDOMParser(toElementNode(node), configToUse, configToUse.getElementType(), false, false, false, true, targetType);
    }

    /**
     * Deserializes an object from the given DOM node, tracking XML nesting depth to guard
     * against stack-overflow on hostile input.
     *
     * @param <T> the type of the target object
     * @param node the DOM node to deserialize
     * @param config the deserialization configuration
     * @param propType the declared property type for the value being read, or {@code null}
     * @param checkedAttr whether the node's type attribute has already been resolved by the caller
     * @param isTagByPropertyName whether elements are named after bean property names
     * @param ignoreTypeInfo whether to ignore any type attribute present on the node
     * @param isFirstCall whether this is the top-level (root) deserialization call
     * @param inputType the type of the object to create
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML nesting depth exceeds the allowed maximum or the
     *         XML structure doesn't match the target type
     */
    @SuppressWarnings("unchecked")
    protected <T> T readByDOMParser(final Node node, final XmlDeserConfig config, Type<?> propType, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final boolean isFirstCall, Type<? extends T> inputType) throws ParsingException {
        enterXmlNesting();
        try {
            return readByDOMParserBody(node, config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, isFirstCall, inputType);
        } finally {
            exitXmlNesting();
        }
    }

    /**
     * Performs the actual DOM-based deserialization for {@link #readByDOMParser}.
     *
     * <p>This method contains the recursive parsing body and is invoked by {@code readByDOMParser}
     * within its nesting-depth guard. The parameters mirror those of {@code readByDOMParser}.</p>
     *
     * @param <T> the type of the target object
     * @param node the XML DOM node to deserialize
     * @param config the XML deserialization configuration; may be {@code null} for default settings
     * @param propType the expected property type for this node
     * @param checkedAttr whether the node's type attribute has already been resolved by the caller
     * @param isTagByPropertyName whether elements are named after bean property names
     * @param ignoreTypeInfo whether to ignore any type attribute present on the node
     * @param isFirstCall whether this is the top-level (root) deserialization call
     * @param inputType the type of the object to create
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML structure doesn't match the target type
     * @see #readByDOMParser(Node, XmlDeserConfig, Type, boolean, boolean, boolean, boolean, Type)
     */
    @SuppressWarnings({ "deprecation", "null" })
    protected <T> T readByDOMParserBody(final Node node, final XmlDeserConfig config, Type<?> propType, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final boolean isFirstCall, Type<? extends T> inputType) throws ParsingException {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        if (inputType.javaType().equals(Object.class)) {
            inputType = (Type<T>) mapEntityType;
        }

        final XmlDeserConfig configToUse = check(config);

        final boolean hasPropTypes = configToUse.hasValueTypes();
        Type<?> targetType = null;
        Class<?> targetClass = null;

        if (isFirstCall) {
            targetType = inputType;
            targetClass = targetType.javaType();
        } else {
            if (propType == null || propType.isString() || propType.isObject()) {
                String nodeName = null;
                if (checkedAttr) {
                    nodeName = isTagByPropertyName ? node.getNodeName() : XmlUtil.getAttribute(node, XmlConstants.NAME);
                } else {
                    final String nameAttr = XmlUtil.getAttribute(node, XmlConstants.NAME);
                    nodeName = Strings.isNotEmpty(nameAttr) ? nameAttr : node.getNodeName();
                }

                targetType = hasPropTypes ? configToUse.getValueType(nodeName, propType) : null;
            } else {
                targetType = propType;
            }

            if (targetType == null || targetType.isString() || targetType.isObject()) {
                // if (isOneNode(node)) {
                // targetClass = Map.class;
                // } else {
                // targetClass = List.class;
                // }
                //
                targetType = listType;
            }

            targetClass = targetType == null ? null : targetType.javaType();
        }

        targetClass = checkedAttr ? (ignoreTypeInfo ? targetClass : getConcreteClass(node, targetClass)) : getConcreteClass(node, targetClass);

        if (targetType == null) {
            if (targetClass == null) {
                throw new ParsingException("Unable to determine target type for xml node: " + node.getNodeName());
            }

            targetType = Type.of(targetClass);
        } else if (targetClass == null) {
            targetClass = targetType.javaType();
        } else if (!targetType.javaType().equals(targetClass)) {
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
                // Only the shape this parser writes with tagByPropertyName=false - <bean name="..."> holding
                // <property name="..."> elements (ParserUtil.XmlNameTag) - names properties by attribute. Taking
                // ANY `name` attribute for that mode read an ordinary <person name="John"><age>30</age></person>
                // in the generic shape. The mode belongs to the bean element rather than to the document, like
                // the StAX reader's per-element resolveElementName: decided once from an outer element, a bean
                // nested in a map entry (<p1>, which never carries the attribute) lost every property.
                isTagByPropertyName = !isGenericBeanElement(node);

                if (!checkedAttr) {
                    ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(node, XmlConstants.TYPE));
                    checkedAttr = true;
                }

                final boolean ignoreUnmatchedProperty = configToUse.isIgnoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
                final Object result = beanInfo.createBeanResult();

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    // The `name` attribute only exists in documents written with tagByPropertyName=false, and the
                    // mode flag is decided from the OUTERMOST element, so a nested element can lack one. Fall back
                    // to the element's own name, as the MAP/MAP_ENTITY branches below and the StAX reader
                    // (resolveElementName) already do; a name that matches no property is still reported by the
                    // ignoreUnmatchedProperty handling below.
                    final String nameAttr = XmlUtil.getAttribute(propNode, XmlConstants.NAME); //NOSONAR
                    propName = isTagByPropertyName || Strings.isEmpty(nameAttr) ? propNode.getNodeName() : nameAttr;

                    propInfo = beanInfo.getPropInfo(propName);

                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        } else {
                            throw new ParsingException("Unknown property element: " + propName + " for class: " + targetClass.getCanonicalName());
                        }
                    }

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        if (propInfo.jsonXmlType.isSerializable()) {
                            propType = propInfo.jsonXmlType;
                        } else {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType : resolveDeclaredType(propNode, propInfo.jsonXmlType);
                        }
                    }

                    //noinspection ConstantValue
                    propValue = getPropValue(propNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, true,
                            inputType);

                    if (propInfo.jsonXmlExpose != JsonXmlField.Direction.SERIALIZE_ONLY) {
                        propInfo.setPropValue(result, propValue);
                    }
                }

                return beanInfo.finishBeanResult(result);
            }

            case MAP: {
                final Collection<String> ignoredClassPropNames = configToUse.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propType != null && propType.isMap() && !propType.parameterTypes().get(0).isObject()) {
                    keyType = propType.parameterTypes().get(0);
                } else {
                    if (configToUse.getMapKeyType() != null && !configToUse.getMapKeyType().isObject()) {
                        keyType = configToUse.getMapKeyType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                        keyType = targetType.parameterTypes().get(0);
                    }
                }

                final boolean isStringKey = keyType.javaType() == String.class;

                Type<?> valueType = defaultValueType;

                if (propType != null && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    valueType = propType.parameterTypes().get(1);
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObject()) {
                        valueType = configToUse.getMapValueType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                        valueType = targetType.parameterTypes().get(1);
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

                    if (propNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(propNode, XmlConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(propNode, XmlConstants.TYPE));
                        checkedAttr = true;
                    }

                    // A map key is always the element's own name: writeMap/writeMapEntity never emit a `name`
                    // attribute, whatever tagByPropertyName says. Without this fallback an enclosing bean written
                    // with tagByPropertyName=false made every entry demand one ("Missing 'name' attribute: k").
                    final String keyNameAttr = XmlUtil.getAttribute(propNode, XmlConstants.NAME);
                    propName = Strings.isNotEmpty(keyNameAttr) ? keyNameAttr : propNode.getNodeName();

                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? valueType : resolveDeclaredType(propNode, valueType);
                    }

                    if (propType.javaType() == Object.class) {
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

                if (propType != null && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    valueType = propType.parameterTypes().get(1);
                } else {
                    if (configToUse.getMapValueType() != null && !configToUse.getMapValueType().isObject()) {
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

                    if (propNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(propNode, XmlConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(propNode, XmlConstants.TYPE));
                        checkedAttr = true;
                    }

                    // A map key is always the element's own name: writeMap/writeMapEntity never emit a `name`
                    // attribute, whatever tagByPropertyName says. Without this fallback an enclosing bean written
                    // with tagByPropertyName=false made every entry demand one ("Missing 'name' attribute: k").
                    final String keyNameAttr = XmlUtil.getAttribute(propNode, XmlConstants.NAME);
                    propName = Strings.isNotEmpty(keyNameAttr) ? keyNameAttr : propNode.getNodeName();

                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? valueType : resolveDeclaredType(propNode, valueType);
                    }

                    if (propType.javaType() == Object.class) {
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

                if (propType != null && (propType.isArray() || propType.isCollection()) && propType.elementType() != null
                        && !propType.elementType().isObject()) {
                    eleType = propType.elementType();
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObject()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = targetType.isCollection() || targetType.isArray() ? targetType.elementType() : objType;
                    }
                }

                if (XmlUtil.isTextElement(node)) {
                    if (eleType.javaType() == String.class || eleType.javaType() == Object.class) {
                        return (T) targetType.valueOf(XmlUtil.getTextContent(node));
                    } else {
                        return (T) jsonParser.deserialize(XmlUtil.getTextContent(node), JsonDeserConfig.create().setElementType(eleType.javaType()),
                                targetType);
                    }
                }

                final List<Object> c = Objectory.createList();

                try {
                    final NodeList eleNodes = node.getChildNodes();
                    Node eleNode = null;

                    for (int i = 0; i < eleNodes.getLength(); i++) {
                        eleNode = eleNodes.item(i);

                        if (eleNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!checkedAttr) {
                            isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XmlConstants.NAME));
                            ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XmlConstants.TYPE));
                            checkedAttr = true;
                        }

                        if (isScalarEleNode(eleNode, eleType)) {
                            // <e> wrapper written by writeElement for a scalar element of a mixed array: the same
                            // test the StAX reader makes (isScalarEleElement), so both backends read it alike.
                            c.add(readScalarNode(eleNode, eleType));

                            continue;
                        }

                        propName = isTagByPropertyName ? eleNode.getNodeName() : XmlUtil.getAttribute(eleNode, XmlConstants.NAME); //NOSONAR

                        propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                        if (propType == null) {
                            propType = ignoreTypeInfo ? eleType : Type.of(getConcreteClass(eleNode, eleType.javaType()));
                        }

                        if (propType.javaType() == Object.class) {
                            propType = defaultValueType;
                        }

                        //noinspection ConstantValue
                        propValue = getPropValue(eleNode, configToUse, propName, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputType);

                        c.add(propValue);
                    }

                    return collectionToArray(c, targetType);
                } finally {
                    Objectory.recycle(c);
                }
            }

            case COLLECTION: {
                Type<?> eleType = null;

                if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.elementType().isObject()) {
                    eleType = propType.elementType();
                } else {
                    if (configToUse.getElementType() != null && !configToUse.getElementType().isObject()) {
                        eleType = configToUse.getElementType();
                    } else {
                        eleType = targetType.elementType() == null ? objType : targetType.elementType();
                    }
                }

                if (XmlUtil.isTextElement(node)) {
                    if (eleType.javaType() == String.class || eleType.javaType() == Object.class) {
                        return (T) targetType.valueOf(XmlUtil.getTextContent(node));
                    } else {
                        return (T) jsonParser.deserialize(XmlUtil.getTextContent(node), JsonDeserConfig.create().setElementType(eleType.javaType()),
                                targetType);
                    }
                }

                final Collection<Object> result = newPropInstance(targetClass, node);

                final NodeList eleNodes = node.getChildNodes();
                Node eleNode = null;

                for (int i = 0; i < eleNodes.getLength(); i++) {
                    eleNode = eleNodes.item(i);

                    if (eleNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if (!checkedAttr) {
                        isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XmlConstants.NAME));
                        ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(eleNode, XmlConstants.TYPE));
                        checkedAttr = true;
                    }

                    if (isScalarEleNode(eleNode, eleType)) {
                        // <e> wrapper written by writeElement for a scalar element of a mixed collection: the same
                        // test the StAX reader makes (isScalarEleElement), so both backends read it alike.
                        result.add(readScalarNode(eleNode, eleType));

                        continue;
                    }

                    propName = isTagByPropertyName ? eleNode.getNodeName() : XmlUtil.getAttribute(eleNode, XmlConstants.NAME); //NOSONAR

                    propType = hasPropTypes ? configToUse.getValueType(propName) : null;

                    if (propType == null) {
                        propType = ignoreTypeInfo ? eleType : Type.of(getConcreteClass(eleNode, eleType.javaType()));
                    }

                    if (propType.javaType() == Object.class) {
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
                throw new ParsingException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only object array, collection, map and bean types are supported");
        }
    }

    /**
     * Returns whether {@code node} is the generic bean element {@code <bean name="...">} that
     * {@link XmlSerConfig#setTagByPropertyName(boolean) tagByPropertyName=false} writes (see
     * {@code ParserUtil.XmlNameTag}), whose children are {@code <property name="...">} elements rather than
     * elements named after the properties themselves. A bean element with any other name - including one that
     * merely carries a {@code name} attribute of its own, as a hand-written {@code <person name="John">} does -
     * is not that shape.
     *
     * @param node the bean element to inspect
     * @return {@code true} if {@code node} is a generic {@code <bean name="...">} element
     */
    private static boolean isGenericBeanElement(final Node node) {
        return XmlConstants.BEAN.equals(node.getNodeName()) && Strings.isNotEmpty(XmlUtil.getAttribute(node, XmlConstants.NAME));
    }

    /**
     * Returns whether {@code node} carries the {@code isNull="true"} marker written for a {@code null} element.
     *
     * @param node the node to inspect
     * @return {@code true} if the node stands for a {@code null} value
     */
    private static boolean isNullNode(final Node node) {
        return Boolean.parseBoolean(XmlUtil.getAttribute(node, XmlConstants.IS_NULL));
    }

    /**
     * Returns whether {@code node} is an {@code <e>} scalar wrapper written by {@link #writeElement} for a mixed
     * array or collection. A declared element type that is a bean, map or map entity rules it out, so a bean
     * class named {@code E} is still read as a bean when the element type says so.
     *
     * <p>This is the same rule as {@link #isScalarEleElement}, deliberately: whether the element has child
     * elements is something only the DOM reader can test, and one document must not read differently through the
     * two backends. {@link #writeElement} never wraps a structured value in {@code <e>}, so only a hand-written
     * {@code <e>} can have children, and it is then read as its text - which is what the class documentation
     * reserves the name {@code e} for.</p>
     *
     * @param node the child node of an array or collection element
     * @param eleType the declared element type of the enclosing array or collection, or {@code null}
     * @return {@code true} if the node is to be read as a scalar
     */
    private static boolean isScalarEleNode(final Node node, final Type<?> eleType) {
        return XmlConstants.E.equals(node.getNodeName()) && (eleType == null || !eleType.isBean() && !eleType.isMap() && !eleType.isMapEntity());
    }

    /**
     * Reads an {@code <e>} scalar element and converts its text. The element's own {@code type} attribute wins
     * when it names an allowed type; otherwise {@code eleType} is used, and an {@code Object} element type
     * yields a String.
     *
     * @param node the {@code <e>} element
     * @param eleType the declared element type, or {@code null}
     * @return the converted value
     */
    private Object readScalarNode(final Node node, final Type<?> eleType) {
        Type<?> valueType = resolveTypeAttribute(XmlUtil.getAttribute(node, XmlConstants.TYPE));

        if (valueType == null || valueType.isObject()) {
            valueType = eleType == null || eleType.isObject() ? strType : eleType;
        }

        return getPropValue(XmlConstants.E, valueType, null, node);
    }

    private Object getPropValue(Node propNode, final XmlDeserConfig config, final String propName, Type<?> propType, final PropInfo propInfo,
            final boolean checkedAttr, final boolean isTagByPropertyName, final boolean ignoreTypeInfo, final boolean isProp, final Type<?> inputType) {
        Object propValue = null;

        if (XmlUtil.isTextElement(propNode)) {
            propValue = getPropValue(propName, propType, propInfo, propNode);
        } else {
            if (propType.isMap() || propType.isBean() || propType.isMapEntity()) {
                if (isProp) {
                    propNode = checkOneNode(propNode);
                }

                propType = propType.isObject() ? (inputType.isMapEntity() ? inputType : mapType) : propType;

                propValue = readByDOMParser(propNode, config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, inputType);
            } else {
                @SuppressWarnings("rawtypes")
                final Collection<Object> coll = Collection.class.isAssignableFrom(propType.javaType())
                        ? N.newCollection((Class<Collection>) propType.javaType())
                        : new ArrayList<>();

                final Type<?> propEleType = getPropEleType(propType);

                final NodeList subPropNodes = propNode.getChildNodes();
                final int subPropNodeLength = getNodeLength(subPropNodes);
                Node subPropNode = null;
                for (int k = 0; k < subPropNodeLength; k++) {
                    subPropNode = subPropNodes.item(k);
                    if (subPropNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if (isNullNode(subPropNode)) {
                        // <null isNull="true" /> keeps the element's position; reading it as the element type
                        // would fabricate an empty bean/map where the source had null.
                        coll.add(null);
                    } else if (isScalarEleNode(subPropNode, propType.elementType())) {
                        // <e> wrapper written by writeElement for a scalar element of a mixed array/collection;
                        // read through the generic element type it would be given as a map otherwise.
                        coll.add(readScalarNode(subPropNode, propType.elementType()));
                    } else {
                        coll.add(readByDOMParser(subPropNode, config, propEleType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, inputType));
                    }
                }

                propValue = propType.isArray() ? collectionToArray(coll, propType) : coll;
            }
        }

        return propValue;
    }

    /**
     * Determines the {@link SerializationType} to use when deserializing the given type.
     *
     * <p>For serializable object-array types this returns {@link SerializationType#ARRAY}, and
     * for serializable collection types it returns {@link SerializationType#COLLECTION};
     * otherwise the type's own serialization type is returned.</p>
     *
     * @param type the target type being deserialized
     * @return the serialization type to use for deserialization
     */
    protected SerializationType getDeserializationType(final Type<?> type) {
        SerializationType serializationType = type.serializationType();

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

        if (propType.javaType().isArray() && (propType.elementType().isBean() || propType.elementType().isMap())) {
            propEleType = propType.elementType();
        } else if (propType.parameterTypes().size() == 1 && (propType.parameterTypes().get(0).isBean() || propType.parameterTypes().get(0).isMap())) {
            propEleType = propType.parameterTypes().get(0);
        } else {
            propEleType = mapType;
        }

        return propEleType;
    }
}
