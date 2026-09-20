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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
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
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ConcurrentCacheMap;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IdentityHashSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;
import com.landawn.abacus.util.cs;

/**
 * Abacus-specific implementation of XML parser supporting SAX, DOM, and StAX parsing modes.
 *
 * <p>This implementation extends {@link AbstractXmlParser} and provides optimized XML serialization
 * and deserialization capabilities tailored for the abacus-common framework. It supports multiple XML
 * parsing strategies including SAX (Simple API for XML), DOM (Document Object Model), and StAX
 * (Streaming API for XML).</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Multiple parser types (SAX, DOM, StAX) selected via the constructor and used for deserialization</li>
 *   <li>Efficient handling of large XML documents through streaming</li>
 *   <li>Support for complex object graphs and collections</li>
 *   <li>Circular reference handling when {@code XmlSerConfig.setCircularReferenceSupported(true)} is set;
 *       with the default configuration no object identity is tracked and a cyclic graph is rejected with a
 *       {@link ParsingException} once {@link #MAX_SERIALIZATION_DEPTH} nested values have been written</li>
 *   <li>Dynamic type resolution through node class mappings</li>
 *   <li>Configurable property naming policies and exclusions</li>
 *   <li>Pretty-printing and indentation support</li>
 *   <li>Type information preservation in XML attributes</li>
 * </ul>
 * <p>Array and collection item type attributes preserve compatible runtime types. Explicit
 * declared/configured item types retain precedence over incompatible attributes, and declared
 * generic arguments are retained when metadata selects a concrete container implementation.</p>
 * <p>Item attributes remain effective when an ancestor omits its own type attribute.</p>
 * <p>Every consumed value's type attribute requires approval, even when ancestor metadata is absent or
 * the value is marked null. Explicitly ignored properties and their subtrees remain opaque.</p>
 * <p>An empty enum element is passed to its type codec. Name-based enums read it as null; annotated value/creator
 * codecs may accept the empty token or reject it with an exception. An explicit {@code isNull="true"} marker
 * bypasses token conversion and remains null.</p>
 *
 * <p>{@code Dataset}, {@code Sheet} and {@code MapEntity} values are not supported by this parser: a
 * {@link ParsingException} is thrown for them at the root and inside bean properties, regardless of
 * {@code failOnEmptyBean}. That setting controls empty beans and generic property-less objects only.
 * Use {@code Dataset.toXml()} or the JSON parser for those structured types.</p>
 *
 * <p>Parser type characteristics:</p>
 * <ul>
 *   <li><b>SAX</b>: Event-driven, memory efficient, suitable for large documents, read-only</li>
 *   <li><b>DOM</b>: Tree-based, allows random access and modification, memory intensive</li>
 *   <li><b>StAX</b>: Pull-based streaming, balanced performance, recommended for most use cases</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create parser with StAX for balanced performance
 * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
 *
 * // Serialize object to XML
 * MyBean bean = new MyBean();
 * String xml = parser.serialize(bean);
 *
 * // Deserialize with configuration
 * XmlDeserConfig config = new XmlDeserConfig()
 *     .setIgnoreUnmatchedProperty(true);
 * MyBean restored = parser.deserialize(xml, config, MyBean.class);
 *
 * // Use SAX for memory-efficient parsing of large documents
 * XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX);
 * LargeBean large = saxParser.deserialize(largeXmlFile, LargeBean.class);
 * }</pre>
 *
 * <p>This class is package-private and should be accessed through {@link ParserFactory} or
 * {@link XmlParser} interface.</p>
 *
 * @see AbstractXmlParser
 * @see XmlParser
 * @see XmlParserType
 * @see XmlSerConfig
 * @see XmlDeserConfig
 */
final class AbacusXmlParserImpl extends AbstractXmlParser {

    /**
     * Maximum number of structured values (beans, maps, collections, arrays, ...) that may be nested on one
     * serialization path. The bound is a cycle heuristic for the default {@code circularReferenceSupported=false}
     * mode, which tracks no object identities: a graph deeper than this is rejected with a
     * {@link ParsingException} instead of unwinding in {@link StackOverflowError}. Kept equal to
     * {@code XmlParserImpl.MAX_SERIALIZATION_DEPTH} so the two XML parsers reject the same graphs.
     */
    static final int MAX_SERIALIZATION_DEPTH = 256;

    /** Per-thread serialization nesting depth counter (used as a single-element mutable int). */
    private static final ThreadLocal<int[]> SERIALIZATION_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    // Cached node-name and node-type metadata used during deserialization.
    private static final Map<Class<?>, Map<String, Class<?>>> nodeNameClassMapPool = new ConcurrentHashMap<>(POOL_SIZE);

    private static final Map<String, NodeType> nodeTypePool = new ConcurrentCacheMap<>(64);

    static {
        nodeTypePool.put(XmlConstants.ARRAY, NodeType.ARRAY);
        nodeTypePool.put(XmlConstants.LIST, NodeType.COLLECTION);
        nodeTypePool.put(XmlConstants.SET, NodeType.COLLECTION);
        nodeTypePool.put(XmlConstants.COLLECTION, NodeType.COLLECTION);
        nodeTypePool.put(XmlConstants.MAP, NodeType.MAP);
        nodeTypePool.put(XmlConstants.E, NodeType.ELEMENT);
        nodeTypePool.put(XmlConstants.ENTRY, NodeType.ENTRY);
        nodeTypePool.put(XmlConstants.KEY, NodeType.KEY);
        nodeTypePool.put(XmlConstants.VALUE, NodeType.VALUE);
    }

    // Reusable SAX handlers; each handler is reset before it returns to this pool.
    private static final Queue<XmlSAXHandler<?>> xmlSAXHandlerPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private final XmlParserType parserType;

    /**
     * Constructs a new AbacusXmlParserImpl with the specified parser type and default configurations.
     *
     * @param parserType the XML parser type to use (SAX, DOM, or StAX)
     */
    AbacusXmlParserImpl(final XmlParserType parserType) {
        this.parserType = parserType;
    }

    /**
     * Constructs a new AbacusXmlParserImpl with the specified parser type and custom configurations.
     *
     * @param parserType the XML parser type to use (SAX, DOM, or StAX)
     * @param xsc the XML serialization configuration; may be {@code null} for defaults
     * @param xdc the XML deserialization configuration; may be {@code null} for defaults
     */
    AbacusXmlParserImpl(final XmlParserType parserType, final XmlSerConfig xsc, final XmlDeserConfig xdc) {
        super(xsc, xdc);
        this.parserType = parserType;
    }

    AbacusXmlParserImpl(final XmlParserType parserType, final XmlSerConfig xsc, final XmlDeserConfig xdc, final java.util.Set<Class<?>> allowedTypeClasses) {
        super(xsc, xdc, allowedTypeClasses);
        this.parserType = parserType;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This abacus-specific implementation writes the XML output via a buffered character writer.
     * Serialization always uses the same writer-based output regardless of the configured
     * {@link XmlParserType}; the parser type only affects which parsing strategy is used during
     * deserialization (SAX, DOM, or StAX).</p>
     *
     * <p>A root value whose type is directly serializable (String, Character, Date, primitive arrays and
     * other scalar types) is written as its plain text form ({@code Type.stringOf}) with XML escaping but
     * no enclosing element; that text is not an XML document and cannot be passed back to
     * {@code deserialize}. Wrap such a value in a bean, map, collection or object array to obtain an
     * element form.</p>
     *
     * <p>String and character values are checked for code units XML 1.0 cannot carry (U+0000..U+0008,
     * U+000B, U+000C, U+000E..U+001F, isolated surrogates, U+FFFE, U+FFFF): a {@code char}/{@code Character}
     * property holding {@code '\0'} (the field default) is rejected, as is every other unrepresentable
     * value, with a {@link ParsingException}. Exclusion.DEFAULT may omit default-valued properties.
     * An empty {@code Optional}/{@code OptionalInt}/{@code Nullable}/{@code java.util.Optional} property is
     * written like a {@code null} property ({@code isNull="true"} form, or omitted under
     * {@code Exclusion.NULL}); a present one is written as its element's text, and tuple-like values
     * ({@code Tuple}, {@code Pair}, {@code Triple}, {@code Indexed}, {@code Timed}) as their JSON text, so
     * both read back through the property type's {@code valueOf}. {@code Nullable.of(null)} is rejected
     * because this XML format cannot preserve its distinction from {@code Nullable.empty()}, including inside
     * embedded JSON payloads; an
     * omitted property (the {@code Exclusion.NULL} default) leaves the field at whatever the bean
     * initializes it to. Wrappers and tuple-like values are unwrapped the same way in a map key, a map
     * value, an array element and a collection element; those elements carry no {@code type} attribute
     * under {@code writeTypeInfo}, since the element holds the unwrapped or JSON form and the wrapper's own
     * name is not a type attribute the readers accept.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     * User user = new User("John", "Doe");
     * String xml = parser.serialize(user);
     *
     * // With configuration
     * XmlSerConfig config = new XmlSerConfig()
     *     .setPrettyFormat(true)
     *     .setWriteTypeInfo(true);
     * String prettyXml = parser.serialize(user, config);
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @return the XML string representation; returns empty string if {@code obj} is {@code null}
     * @throws ParsingException if the object type is not supported for serialization (a bean without
     *         serializable properties throws only when {@code failOnEmptyBean} is {@code true}), if a
     *         String/character value contains a code unit that cannot be represented in XML 1.0, if a bean
     *         property's custom name is not usable in the position {@code tagByPropertyName} writes it, or if more
     *         than {@link #MAX_SERIALIZATION_DEPTH} values are nested on one path (a circular reference)
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
            write(obj, null, configToUse, null, serializedObjects, bw, false);

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
     * <p>This implementation writes XML output to a file. Serialization always uses the same writer-based
     * XML output regardless of the configured {@link XmlParserType} (the parser type only affects
     * deserialization). The file is created if it doesn't exist, and the contents are flushed and properly
     * closed after serialization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     * User user = new User("Jane", "Smith");
     * File outputFile = new File("user.xml");
     *
     * XmlSerConfig config = new XmlSerConfig()
     *     .setPrettyFormat(true)
     *     .setWriteTypeInfo(false);
     * parser.serialize(user, config, outputFile);
     * }</pre>
     *
     * @param obj the object to serialize; may be {@code null}
     * @param config the serialization configuration (may be {@code null} for default behavior)
     * @param output the file to write the XML content to; must not be {@code null}
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws ParsingException if the object type is not supported for serialization
     * @throws UncheckedIOException if creating, opening, writing, flushing or closing {@code output}, or reading a resource-backed value
     *         while producing XML, fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final File output)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(output, cs.output);

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
     * <p>This implementation writes XML output to an output stream. Serialization always uses the same
     * writer-based XML output regardless of the configured {@link XmlParserType} (the parser type only
     * affects deserialization). The stream is flushed after serialization but is not closed, allowing
     * the caller to manage the stream lifecycle.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
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
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws ParsingException if the object type is not supported for serialization
     * @throws UncheckedIOException if writing or flushing XML to {@code output}, or reading a resource-backed value during
     *         serialization, fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final OutputStream output)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(output, cs.output);

        final XmlSerConfig configToUse = check(config);
        final BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.isCircularReferenceSupported() ? null : new IdentityHashSet<>();

        try {
            write(obj, null, configToUse, null, serializedObjects, bw, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation writes XML output to a writer. Serialization always uses the same writer-based
     * XML output regardless of the configured {@link XmlParserType} (the parser type only affects
     * deserialization). The writer is flushed after serialization but is not closed, allowing the caller
     * to manage the writer lifecycle. If the provided writer is already a {@link BufferedXmlWriter}, it
     * is used directly for optimal performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
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
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws ParsingException if the object type is not supported for serialization
     * @throws UncheckedIOException if writing or flushing XML to {@code output}, or reading a resource-backed value during
     *         serialization, fails
     */
    @Override
    public void serialize(final Object obj, final XmlSerConfig config, final Writer output)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(output, cs.output);

        final XmlSerConfig configToUse = check(config);
        final boolean isBufferedWriter = output instanceof BufferedXmlWriter;
        final BufferedXmlWriter bw = isBufferedWriter ? (BufferedXmlWriter) output : Objectory.createBufferedXmlWriter(output);
        final IdentityHashSet<Object> serializedObjects = !configToUse.isCircularReferenceSupported() ? null : new IdentityHashSet<>();

        try {
            write(obj, null, configToUse, null, serializedObjects, bw, true);
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
     * This is the main internal method that handles the serialization logic, including
     * circular-reference detection and raw-JSON-value passthrough for properties marked as such.
     *
     * @param obj the object to write
     * @param propInfo the property metadata for {@code obj} when it is being written as a bean
     *        property (used to detect {@code isJsonRawValue}/format handling), or {@code null} at the root
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param bw the buffered XML writer
     * @param flush whether to flush the writer after writing
     * @throws ParsingException if the object type is not supported and {@code failOnEmptyBean} is set, if a String/character value contains a code
     *         unit that cannot be represented in XML 1.0, if a bean property's custom name is not usable in the position {@code tagByPropertyName}
     *         writes it, or if more than {@link #MAX_SERIALIZATION_DEPTH} values are nested on one path (a circular reference)
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void write(final Object obj, final PropInfo propInfo, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final BufferedXmlWriter bw, final boolean flush) throws ParsingException, IOException {
        final XmlSerConfig configToUse = check(config);

        if (hasCircularReference(obj, serializedObjects, configToUse, bw)) {
            return;
        }

        if (obj == null) {
            IOUtil.write(Strings.EMPTY, bw);
            return;
        }

        final Class<?> cls = obj.getClass();
        final Type<Object> type = Type.of(cls);
        final SerializationType serializationType = type.serializationType();

        // Only the structured values recurse (a scalar cannot nest, and the reader side is guarded by
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
            if (propInfo != null && propInfo.isJsonRawValue) {
                writeRawJson(bw, serializeEmbeddedJson(obj, config));
                return;
            }

            switch (serializationType) {
                case SERIALIZABLE:
                    if (type.isObjectArray()) {
                        writeArray(obj, configToUse, indentation, serializedObjects, type, bw);
                    } else if (type.isCollection()) {
                        writeCollection((Collection<?>) obj, configToUse, indentation, serializedObjects, type, bw);
                    } else {
                        if (propInfo != null && propInfo.hasFormat) {
                            propInfo.writePropValue(bw, obj, configToUse);
                        } else if (propInfo == null) {
                            // Root scalar: plain text form, documented on serialize(Object, XmlSerConfig).
                            writeXmlScalar(bw, type, obj, configToUse, "Root value");
                        } else if (type.isOptionalOrNullable() || isTupleLike(type)) {
                            // An Object-typed property holding a wrapper/tuple: an empty wrapper leaves the element empty.
                            final Object unwrapped = unwrapOptional(obj);

                            if (unwrapped != null) {
                                writeUnwrappedValue(bw, null, unwrapped, configToUse, "Property '" + propInfo.name + "'");
                            }
                        } else {
                            writeXmlScalar(bw, type, obj, configToUse, "Property '" + propInfo.name + "'");
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
                    if (writeEmptyObject(type, configToUse, indentation, bw)) {
                        break;
                    }
                    throw new ParsingException("Unsupported class: " + ClassUtil.getCanonicalClassName(cls)
                            + ". Only Array/List/Map and Bean class with getter/setter methods are supported");
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
    void writeBean(final Object obj, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(obj, serializedObjects, bw)) {
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
    void writeProperties(final Object obj, final XmlSerConfig config, final String propIndentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(obj, serializedObjects, bw)) {
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

        final String nextIndentation = isPrettyFormat ? ((propIndentation == null ? Strings.EMPTY : propIndentation) + config.getIndentation()) : null;
        final PropInfo[] propInfoList = config.isSkipTransientField() ? beanInfo.nonTransientSeriPropInfos : beanInfo.jsonXmlSerializablePropInfos;
        final NamingPolicy jsonXmlNamingPolicy = config.getPropNamingPolicy() == null ? beanInfo.jsonXmlNamingPolicy : config.getPropNamingPolicy();
        final int nameTagIdx = jsonXmlNamingPolicy.ordinal();
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

            // An empty Optional/Nullable is written like a null property (its serializeTo would emit the JSON
            // literal "null", which the XML reader cannot map back); a present one is written as its element.
            final boolean unwrapped = propValue != null && propInfo.jsonXmlType.isOptionalOrNullable();

            if (unwrapped) {
                propValue = unwrapOptional(propValue);
            }

            // The element carries the UNWRAPPED value (or the isNull marker), never the wrapper itself, so the
            // wrapper's own name must not be written as the type attribute: it describes the wrong shape and the
            // readers reject it ("XML type attribute is not allowed: JdkOptionalInt"). The declared property type
            // is what re-wraps the value on the way back.
            // The same holds for a Pair/Triple/Tuple/Timed/Indexed property: it is written as its text form, whose
            // shape the declared property type already describes, and its simple name is not an accepted discriminator.
            final boolean writeTypeInfoForProp = writeTypeInfo && !propInfo.jsonXmlType.isOptionalOrNullable() && !isTupleLike(propInfo.jsonXmlType);

            if ((ignoreNullProperty && propValue == null) || (ignoreDefaultProperty && propValue != null && (propInfo.jsonXmlType != null)
                    && propInfo.jsonXmlType.isPrimitive() && propValue.equals(propInfo.jsonXmlType.defaultValue()))) {
                continue;
            }

            // Same check, same gating and same position as XmlParserImpl.writeProperties: a custom name
            // (@JsonXmlField/@JSONField/@JsonProperty) is handed through verbatim, and with tagByPropertyName it
            // lands in element-name position, where XML has no escaping mechanism - @JsonXmlField(name = "a&b")
            // wrote <a&b>v</a&b>, which no XML reader accepts. The ep* style is escaped, but escaping cannot
            // rescue a character XML 1.0 has no representation for: a name holding U+0001 wrote
            // name="a&#x1;b", which this parser's own reader rejects. Checked after the ignore/exclusion
            // filters, so a property that is not written cannot make serialization fail.
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
                    writeRawJson(bw, serializeEmbeddedJson(propValue, config));
                } else if (unwrapped) {
                    writeUnwrappedValue(bw, propInfo.jsonXmlType.elementType(), propValue, config, "Property '" + propName + "'");
                } else if (propInfo.jsonXmlType.isSerializable()) {
                    if (propInfo.jsonXmlType.isObjectArray() || propInfo.jsonXmlType.isCollection()) {
                        // jsonParser.serialize(bw, propValue);

                        strType.serializeTo(bw, serializeEmbeddedJson(propValue, config), config);
                    } else if (isTupleLike(propInfo.jsonXmlType)) {
                        writeUnwrappedValue(bw, propInfo.jsonXmlType, propValue, config, "Property '" + propName + "'");
                    } else {
                        if (propInfo.hasFormat) {
                            propInfo.writePropValue(bw, propValue, config);
                        } else {
                            writeXmlScalar(bw, propInfo.jsonXmlType, propValue, config, "Property '" + propName + "'");
                        }
                    }
                } else {
                    write(propValue, propInfo, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);
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
     * Writes a map to XML, emitting an enclosing map element and, for each entry, an
     * {@code <entry>} element containing a {@code <key>} element followed by a {@code <value>} element.
     *
     * @param m the map to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the map
     * @param bw the buffered XML writer
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void writeMap(final Map<?, ?> m, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws IOException {
        //    if (hasCircularReference(m, serializedObjects, bw)) {
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

        final String entryIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final String keyValueIndentation = isPrettyFormat ? (entryIndentation + config.getIndentation()) : null;
        final String nextIndentation = isPrettyFormat ? (keyValueIndentation + config.getIndentation()) : null;

        final Type<Object> stringType = Type.of(String.class);
        Type<Object> keyType = null;
        Type<Object> valueType = null;
        Object key = null;
        Object value = null;

        for (final Map.Entry<Object, Object> entry : ((Map<Object, Object>) m).entrySet()) {
            key = entry.getKey();
            keyType = key == null ? null : Type.of(key.getClass());

            if (keyType != null && keyType.isOptionalOrNullable()) {
                // Same unwrapping the value gets below: an empty wrapper key is written as the null key form, a
                // present one as its element (runtime type). Without it a wrapper key reaches writeXmlScalar as
                // the wrapper itself, writing the literal "null" and, under writeTypeInfo, a type attribute the
                // readers reject. Unwrapped before the ignored-name test so that test sees the same key.
                key = unwrapOptional(key);
            }

            // Filtered on the name the entry is actually written (and read back) under: a null key becomes
            // the element <null>, so it is ignored by the name "null". Passing the bare null key instead
            // called contains(null), which throws NPE on a Set.of(..) - the very shape
            // ParserConfig.setIgnoredPropNames' own examples use. Matches XmlParserImpl.writeMap.
            if (ignoredClassPropNames != null && ignoredClassPropNames.contains(key == null ? NULL_STRING : key.toString())) {
                continue;
            }

            value = entry.getValue();
            valueType = value == null ? null : Type.of(value.getClass());

            if (valueType != null && valueType.isOptionalOrNullable()) {
                // An empty wrapper is written as the null value form; a present one as its element (runtime type).
                value = unwrapOptional(value);
                valueType = value == null ? null : Type.of(value.getClass());
            }

            //    if (ignoreNullProperty && value == null) {
            //        continue;
            //    }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(entryIndentation);
            }

            bw.write(XmlConstants.ENTRY_ELE_START);

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(keyValueIndentation);
            }

            if (key == null) {
                bw.write(XmlConstants.KEY_NULL_ELE);
            } else {
                if (key.getClass() == String.class) {
                    if (writeTypeInfo) {
                        bw.write(XmlConstants.START_KEY_ELE_WITH_STRING_TYPE);
                    } else {
                        bw.write(XmlConstants.KEY_ELE_START);
                    }

                    writeXmlScalar(bw, stringType, key, config, "Map key");
                } else {
                    keyType = Type.of(key.getClass());

                    // A tuple-like key is written as its JSON text (see writeUnwrappedValue), so its own
                    // xmlName describes the wrong shape and is not an accepted type-attribute name: the readers
                    // reject the parser's own output. Same suppression as the bean-property path above.
                    if (writeTypeInfo && !isTupleLike(keyType)) {
                        bw.write(XmlConstants.START_KEY_ELE_WITH_TYPE);
                        bw.write(keyType.xmlName());
                        bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
                    } else {
                        bw.write(XmlConstants.KEY_ELE_START);
                    }

                    if (keyType.isSerializable()) {
                        if (keyType.isObjectArray() || keyType.isCollection()) {
                            // jsonParser.serialize(bw, key);

                            strType.serializeTo(bw, serializeEmbeddedJson(key, config), config);
                        } else if (isTupleLike(keyType)) {
                            writeUnwrappedValue(bw, keyType, key, config, "Map key");
                        } else {
                            writeXmlScalar(bw, keyType, key, config, "Map key");
                        }
                    } else {
                        write(key, null, config, nextIndentation, serializedObjects, bw, false);

                        if (isPrettyFormat) {
                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                            bw.write(keyValueIndentation);
                        }
                    }
                }

                bw.write(XmlConstants.KEY_ELE_END);
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(keyValueIndentation);
            }

            if (value == null) {
                bw.write(XmlConstants.VALUE_NULL_ELE);
            } else {
                // A tuple-like value is written as its JSON text (see writeUnwrappedValue), so its own
                // xmlName describes the wrong shape and is not an accepted type-attribute name: the readers
                // reject the parser's own output. Same suppression as the bean-property path above.
                if (writeTypeInfo && !isTupleLike(valueType)) {
                    bw.write(XmlConstants.START_VALUE_ELE_WITH_TYPE);
                    bw.write(valueType.xmlName());
                    bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
                } else {
                    bw.write(XmlConstants.VALUE_ELE_START);
                }

                if (valueType.isSerializable()) {
                    if (valueType.isObjectArray() || valueType.isCollection()) {
                        // jsonParser.serialize(bw, value);

                        strType.serializeTo(bw, serializeEmbeddedJson(value, config), config);
                    } else if (isTupleLike(valueType)) {
                        writeUnwrappedValue(bw, valueType, value, config, "Map value");
                    } else {
                        writeXmlScalar(bw, valueType, value, config, "Map value");
                    }
                } else {
                    write(value, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                        bw.write(keyValueIndentation);
                    }
                }

                bw.write(XmlConstants.VALUE_ELE_END);
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(entryIndentation);
            }

            bw.write(XmlConstants.ENTRY_ELE_END);
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
     * Writes an array to XML via reflection, emitting an enclosing array element and an
     * {@code <e>} element for each array element. Using {@link java.lang.reflect.Array} to access
     * elements allows this method to handle both object and primitive arrays.
     *
     * @param obj the array to write (may be a primitive or object array)
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the array
     * @param bw the buffered XML writer
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void writeArray(final Object obj, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws IOException {
        //    if (hasCircularReference(obj, serializedObjects, bw)) {
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

        final String eleIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final String nextIndentation = isPrettyFormat ? (eleIndentation + config.getIndentation()) : null;
        final int len = Array.getLength(obj);
        Type<Object> eleType = null;

        for (int i = 0; i < len; i++) {
            Object e = Array.get(obj, i);
            eleType = e == null ? null : Type.of(e.getClass());

            if (eleType != null && eleType.isOptionalOrNullable()) {
                // An empty wrapper is written as the null element form; a present one as its element (runtime type).
                e = unwrapOptional(e);
                eleType = e == null ? null : Type.of(e.getClass());
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(eleIndentation);
            }

            if (e == null) {
                bw.write(XmlConstants.E_NULL_ELE);
            } else {
                // A tuple-like value is written as its JSON text (see writeUnwrappedValue), so its own
                // xmlName describes the wrong shape and is not an accepted type-attribute name: the readers
                // reject the parser's own output. Same suppression as the bean-property path above.
                if (writeTypeInfo && !isTupleLike(eleType)) {
                    bw.write(XmlConstants.START_E_ELE_WITH_TYPE);
                    bw.write(eleType.xmlName());
                    bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
                } else {
                    bw.write(XmlConstants.E_ELE_START);
                }

                if (eleType.isSerializable()) {
                    if (eleType.isObjectArray() || eleType.isCollection()) {
                        // jsonParser.serialize(bw, e);

                        strType.serializeTo(bw, serializeEmbeddedJson(e, config), config);
                    } else if (isTupleLike(eleType)) {
                        writeUnwrappedValue(bw, eleType, e, config, "Array element");
                    } else {
                        writeXmlScalar(bw, eleType, e, config, "Array element");
                    }
                } else {
                    write(e, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                        bw.write(eleIndentation);
                    }
                }

                bw.write(XmlConstants.E_ELE_END);
            }
        }

        if (isPrettyFormat) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

            if (indentation != null) {
                bw.write(indentation);
            }
        }

        bw.write(XmlConstants.ARRAY_ELE_END);
    }

    /**
     * Writes a collection to XML, emitting an enclosing element appropriate to the collection
     * kind (list, set, or generic collection) and an {@code <e>} element for each collection element.
     *
     * @param c the collection to write
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the collection
     * @param bw the buffered XML writer
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void writeCollection(final Collection<?> c, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws IOException {
        //    if (hasCircularReference(c, serializedObjects, bw)) {
        //        return;
        //    }

        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();

        if (isPrettyFormat && indentation != null) {
            bw.write(IOUtil.LINE_SEPARATOR_UNIX);
            bw.write(indentation);
        }

        if (type.isList()) {
            if (writeTypeInfo) {
                bw.write(XmlConstants.START_LIST_ELE_WITH_TYPE);
                bw.write(type.xmlName());
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

        final String eleIndentation = isPrettyFormat ? ((indentation == null ? Strings.EMPTY : indentation) + config.getIndentation()) : null;
        final String nextIndentation = isPrettyFormat ? (eleIndentation + config.getIndentation()) : null;

        Type<Object> eleType = null;

        for (Object e : c) {
            eleType = e == null ? null : Type.of(e.getClass());

            if (eleType != null && eleType.isOptionalOrNullable()) {
                // An empty wrapper is written as the null element form; a present one as its element (runtime type).
                e = unwrapOptional(e);
                eleType = e == null ? null : Type.of(e.getClass());
            }

            if (isPrettyFormat) {
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write(eleIndentation);
            }

            if (e == null) {
                bw.write(XmlConstants.E_NULL_ELE);
            } else {
                // A tuple-like value is written as its JSON text (see writeUnwrappedValue), so its own
                // xmlName describes the wrong shape and is not an accepted type-attribute name: the readers
                // reject the parser's own output. Same suppression as the bean-property path above.
                if (writeTypeInfo && !isTupleLike(eleType)) {
                    bw.write(XmlConstants.START_E_ELE_WITH_TYPE);
                    bw.write(eleType.xmlName());
                    bw.write(XmlConstants.CLOSE_ATTR_AND_ELE);
                } else {
                    bw.write(XmlConstants.E_ELE_START);
                }

                if (eleType.isSerializable()) {
                    if (eleType.isObjectArray() || eleType.isCollection()) {
                        // jsonParser.serialize(bw, e);

                        strType.serializeTo(bw, serializeEmbeddedJson(e, config), config);
                    } else if (isTupleLike(eleType)) {
                        writeUnwrappedValue(bw, eleType, e, config, "Collection element");
                    } else {
                        writeXmlScalar(bw, eleType, e, config, "Collection element");
                    }
                } else {
                    write(e, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat) {
                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                        bw.write(eleIndentation);
                    }
                }

                bw.write(XmlConstants.E_ELE_END);
            }
        }

        if (isPrettyFormat) {
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
     * {@inheritDoc}
     *
     * <p>This abacus-specific implementation uses the configured parser type (SAX, DOM, or StAX) for XML
     * deserialization. Each parser type offers different performance characteristics:</p>
     * <ul>
     *   <li><b>SAX</b>: Event-driven, extremely memory-efficient for large documents</li>
     *   <li><b>DOM</b>: Tree-based, loads entire document into memory for random access</li>
     *   <li><b>StAX</b>: Pull-based streaming, balanced performance (recommended for most cases)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Using StAX (recommended)
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     * String xml = "<user><name>John</name><age>30</age></user>";
     * Type<User> userType = Type.of(User.class);
     * User user = parser.deserialize(xml, null, userType);
     *
     * // Using SAX for large documents
     * XmlParser saxParser = new AbacusXmlParserImpl(XmlParserType.SAX);
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * User user2 = saxParser.deserialize(largeXml, config, userType);
     * }</pre>
     *
     * <p>The whole string must be one XML document: content after the root element other than
     * whitespace, comments and processing instructions (a second root element, text, a CDATA section)
     * is rejected with a {@link ParsingException} by all three parser types.</p>
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}; returns default value if source is empty
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed,
     *         including content after the root element
     * @throws UncheckedIOException if the DOM or SAX backend or a delegated value reader reports an {@code IOException} while consuming
     *         the XML text
     */
    @Override
    public <T> T deserialize(String source, XmlDeserConfig config, Type<? extends T> targetType)
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
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     * String xml = "<user><name>Jane</name><email>jane@example.com</email></user>";
     * User user = parser.deserialize(xml, null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the XML string to deserialize; may be {@code null} or empty
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}; returns default value if source is empty
     * @throws IllegalArgumentException if {@code targetClass} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM or SAX backend or a delegated value reader reports an {@code IOException} while consuming
     *         the XML text
     */
    @Override
    public <T> T deserialize(final String source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(targetClass, cs.targetClass);

        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a file using the configured parser type (SAX, DOM, or StAX).
     * The file is automatically opened, read, and closed. The whole file must be one XML document:
     * content after the root element other than whitespace, comments and processing instructions is
     * rejected with a {@link ParsingException} by all three parser types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     * File xmlFile = new File("user.xml");
     * User user = parser.deserialize(xmlFile, null, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}, or {@code source} is a directory
     * @throws UncheckedIOException if opening {@code source} fails, or the DOM or SAX backend reports an {@code IOException} while
     *         reading the XML file
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed,
     *         including content after the root element
     */
    @Override
    public <T> T deserialize(File source, XmlDeserConfig config, Type<? extends T> targetType)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgument(!source.isDirectory(), "source must not be a directory: %s", source);
        N.checkArgNotNull(targetType, cs.targetType);

        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return read(is, config, null, targetType, true);
        } finally {
            IOUtil.closeQuietly(is);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(File, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor. Uses the configured parser type (SAX, DOM, or StAX)
     * for XML deserialization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     *
     * File xmlFile = new File("data.xml");
     * User user = parser.deserialize(xmlFile, null, User.class);
     *
     * // With configuration
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * User user2 = parser.deserialize(xmlFile, config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetClass} is {@code null}, or {@code source} is a directory
     * @throws UncheckedIOException if opening {@code source} fails, or the DOM or SAX backend reports an {@code IOException} while
     *         reading the XML file
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgument(!source.isDirectory(), "source must not be a directory: %s", source);
        N.checkArgNotNull(targetClass, cs.targetClass);

        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from an input stream using the configured parser type. With the
     * StAX strategy the stream is left open, and parsing stops at the end of the root element without
     * validating trailing content. The XML reader may buffer bytes beyond that element, so callers must
     * bound each document before parsing a framed stream. The SAX and DOM strategies hand the stream to the
     * JAXP parser, which reads it to the end, rejects content after the root element, and closes the
     * stream when the document ends.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     * try (InputStream in = new FileInputStream("user.xml")) {
     *     User user = parser.deserialize(in, null, Type.of(User.class));
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
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(InputStream source, XmlDeserConfig config, Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetType, cs.targetType);

        return read(source, config, null, targetType, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(InputStream, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor. Uses the configured parser type (SAX, DOM, or StAX)
     * for XML deserialization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     *
     * try (InputStream in = new FileInputStream("data.xml")) {
     *     User user = parser.deserialize(in, null, User.class);
     * }
     *
     * // With configuration
     * try (InputStream in = new URL("http://api.example.com/data.xml").openStream()) {
     *     XmlDeserConfig config = new XmlDeserConfig()
     *         .setIgnoreUnmatchedProperty(true);
     *     User user = parser.deserialize(in, config, User.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetClass} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetClass, cs.targetClass);

        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a Reader using the configured parser type. With the StAX
     * strategy the reader is left open, and parsing stops at the end of the root element without
     * validating trailing content. The XML reader may buffer characters beyond that element, so callers
     * must bound each document before parsing a framed source. The SAX and
     * DOM strategies hand the reader to the JAXP parser, which reads it to the end, rejects content after
     * the root element, and closes the reader when the document ends.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     * try (Reader reader = new FileReader("user.xml")) {
     *     User user = parser.deserialize(reader, null, Type.of(User.class));
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(Reader source, XmlDeserConfig config, Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetType, cs.targetType);

        // BufferedReader? will the target parser create the BufferedReader internally?
        return read(source, config, null, targetType, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(Reader, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor. Uses the configured parser type (SAX, DOM, or StAX)
     * for XML deserialization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     *
     * try (Reader reader = new FileReader("data.xml")) {
     *     User user = parser.deserialize(reader, null, User.class);
     * }
     *
     * // From StringReader
     * String xml = "<user><name>Alice</name><age>30</age></user>";
     * Reader stringReader = new StringReader(xml);
     * User user = parser.deserialize(stringReader, null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data; must not be {@code null}
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetClass} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type or is malformed
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetClass, cs.targetClass);

        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses DOM parsing to deserialize from a pre-parsed node. This is useful when
     * working with XML that has already been loaded into a DOM tree.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.DOM);
     * Document doc = XmlUtil.createDOMParser().parse(new File("user.xml"));
     * User user = parser.deserialize(doc.getDocumentElement(), null, Type.of(User.class));
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
    public <T> T deserialize(Node source, XmlDeserConfig config, Type<? extends T> targetType) throws IllegalArgumentException, ParsingException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetType, cs.targetType);

        return readByDOMParser(source, config, targetType);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is a convenience method that delegates to {@link #deserialize(Node, XmlDeserConfig, Type)}
     * after wrapping the target class in a Type descriptor. This implementation uses DOM parsing for deserialization
     * from a pre-parsed node.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.DOM);
     *
     * Document doc = XmlUtil.createDOMParser().parse(new File("user.xml"));
     *
     * User user = parser.deserialize(doc.getDocumentElement(), null, User.class);
     *
     * // Deserialize from a specific child node
     * Node addressNode = doc.getElementsByTagName("address").item(0);
     * Address address = parser.deserialize(addressNode, null, Address.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data; must not be {@code null}. A {@link org.w3c.dom.Document}
     *        is read as its document element
     * @param config the deserialization configuration (may be {@code null} for default behavior)
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}
     * @throws IllegalArgumentException if {@code source} or {@code targetClass} is {@code null}
     * @throws ParsingException if the XML structure doesn't match the target type
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Class<? extends T> targetClass)
            throws IllegalArgumentException, ParsingException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetClass, cs.targetClass);

        return deserialize(source, config, Type.of(targetClass));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a file using the configured parser type and node class mappings
     * for dynamic type resolution. This enables polymorphic deserialization where different XML elements map
     * to different Java types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     *
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("customer", Type.of(Customer.class));
     * nodeTypes.put("supplier", Type.of(Supplier.class));
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
     * @throws IllegalArgumentException if {@code source} is {@code null} or is a directory
     * @throws UncheckedIOException if opening {@code source} fails, or the DOM or SAX backend reports an {@code IOException} while
     *         reading the XML file
     * @throws ParsingException if no matching type is found in nodeTypes or XML is malformed
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return read(is, config, nodeTypes, null, true);
        } finally {
            IOUtil.closeQuietly(is);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from an input stream using the configured parser type and node class
     * mappings for dynamic type resolution. With the StAX strategy the stream is left open and reading
     * stops at the end of the root element; the SAX and DOM strategies read the stream to the end and
     * close it (see {@link #deserialize(InputStream, XmlDeserConfig, Type)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.SAX);
     *
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
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws ParsingException, UncheckedIOException {
        return read(source, config, nodeTypes, null, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation reads XML from a Reader using the configured parser type and node class mappings
     * for dynamic type resolution. With the StAX strategy the reader is left open and reading stops at the
     * end of the root element; the SAX and DOM strategies read the reader to the end and close it (see
     * {@link #deserialize(Reader, XmlDeserConfig, Type)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
     *
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
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws ParsingException, UncheckedIOException {
        return read(source, config, nodeTypes, null, false);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses DOM parsing to deserialize from a pre-parsed node using node class mappings.
     * The target type is determined by looking up the node's name in the nodeTypes map.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = new AbacusXmlParserImpl(XmlParserType.DOM);
     *
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
     * @throws IllegalArgumentException if {@code source} is {@code null}
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
        String nodeName = XmlUtil.getAttribute(sourceElement, XmlConstants.NAME);

        if (Strings.isEmpty(nodeName)) {
            nodeName = localName(sourceElement);
        }

        final Type<?> targetType = N.notEmpty(nodeTypes) ? nodeTypes.get(nodeName) : null;

        if (targetType == null) {
            throw new ParsingException("No target class is specified for xml node: " + nodeName);
        }

        return (T) readByDOMParser(sourceElement, config, targetType);
    }

    /**
     * Reads and deserializes XML from the given input stream using the parser type this instance was created with
     * (SAX, DOM, or StAX).
     *
     * <p>When {@code targetType} is {@code null}, the type is resolved from {@code nodeTypes} by looking up
     * the root element's {@code name} attribute, falling back to its local name.</p>
     *
     * @param <T> the type of the target object
     * @param source the input stream to read the XML from
     * @param config the deserialization configuration; may be {@code null} for defaults
     * @param nodeTypes a mapping from root node name to target type, used when {@code targetType} is {@code null}
     * @param targetType the type to deserialize into, or {@code null} to resolve it from {@code nodeTypes}
     * @param boundedSource whether the stream is known to end with the document (a file or byte array), in
     *        which case the StAX reader also verifies that nothing but whitespace, comments and processing
     *        instructions follows the root element; {@code false} for a caller-supplied stream that may stay open
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML is malformed, no target type can be resolved for the root node,
     *         or the configured parser type is not supported
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @SuppressWarnings("unchecked")
    <T> T read(final InputStream source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType,
            final boolean boundedSource) throws ParsingException, UncheckedIOException {
        final XmlDeserConfig configToUse = check(config);

        switch (parserType) {
            case SAX:

                final SAXParser saxParser = XmlUtil.createSAXParser();
                final XmlSAXHandler<T> dh = getXmlSAXHandler(configToUse, nodeTypes, targetType);
                T result = null;

                try {
                    saxParser.parse(source, dh);
                    result = dh.resultHolder.value();
                } catch (final SAXException e) {
                    throw new ParsingException(e);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    recycle(dh);
                    XmlUtil.recycleSAXParser(saxParser);
                }

                return result;

            case StAX:
                XMLStreamReader xmlReader = null;
                try {
                    xmlReader = createXMLStreamReader(source);

                    moveToRootElement(xmlReader);

                    // Resolved before the lookup so the failure below can name the key that was looked up, as
                    // the DOM branch does: for <bean name="order"> that is "order", not the element name "bean".
                    String nodeName = xmlReader.getAttributeCount() > 0 ? xmlReader.getAttributeValue(null, XmlConstants.NAME) : null;

                    if (Strings.isEmpty(nodeName)) {
                        nodeName = xmlReader.getLocalName();
                    }

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target type is specified for xml node: " + nodeName
                                + (nodeName.equals(xmlReader.getLocalName()) ? "" : " (element <" + xmlReader.getLocalName() + ">)"));
                    }

                    final T staxResult = readByStreamParser(xmlReader, configToUse, targetType);

                    if (boundedSource) {
                        // A second root or trailing text is an error for a whole document; an open stream is
                        // deliberately not drained (see the deserialize(InputStream, ...) javadoc).
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
                        throw new ParsingException("No document element found in XML source");
                    }

                    // Resolved before the lookup so the failure below can name the key that was looked up: for
                    // <ns:order> that is "order", while getNodeName() reports the qualified "ns:order".
                    String nodeName = XmlUtil.getAttribute(node, XmlConstants.NAME);

                    if (Strings.isEmpty(nodeName)) {
                        nodeName = localName(node);
                    }

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target class is specified for xml node: " + nodeName
                                + (nodeName.equals(node.getNodeName()) ? "" : " (element <" + node.getNodeName() + ">)"));
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
     * Reads and deserializes XML from the given reader using the parser type this instance was created with
     * (SAX, DOM, or StAX).
     *
     * <p>When {@code targetType} is {@code null}, the type is resolved from {@code nodeTypes} by looking up
     * the root element's {@code name} attribute, falling back to its local name.</p>
     *
     * @param <T> the type of the target object
     * @param source the reader to read the XML from
     * @param config the deserialization configuration; may be {@code null} for defaults
     * @param nodeTypes a mapping from root node name to target type, used when {@code targetType} is {@code null}
     * @param targetType the type to deserialize into, or {@code null} to resolve it from {@code nodeTypes}
     * @param boundedSource whether the reader is known to end with the document (a String), in which case
     *        the StAX reader also verifies that nothing but whitespace, comments and processing instructions
     *        follows the root element; {@code false} for a caller-supplied reader that may stay open
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML is malformed, no target type can be resolved for the root node,
     *         or the configured parser type is not supported
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @SuppressWarnings("unchecked")
    <T> T read(final Reader source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType,
            final boolean boundedSource) throws ParsingException, UncheckedIOException {
        final XmlDeserConfig configToUse = check(config);

        switch (parserType) {
            case SAX:

                final SAXParser saxParser = XmlUtil.createSAXParser();
                final XmlSAXHandler<T> dh = getXmlSAXHandler(configToUse, nodeTypes, targetType);
                T result = null;

                try {
                    saxParser.parse(new InputSource(source), dh);
                    result = dh.resultHolder.value();
                } catch (final SAXException e) {
                    throw new ParsingException(e);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    recycle(dh);
                    XmlUtil.recycleSAXParser(saxParser);
                }

                return result;

            case StAX:
                XMLStreamReader xmlReader = null;
                try {
                    xmlReader = createXMLStreamReader(source);

                    moveToRootElement(xmlReader);

                    // Resolved before the lookup so the failure below can name the key that was looked up, as
                    // the DOM branch does: for <bean name="order"> that is "order", not the element name "bean".
                    String nodeName = xmlReader.getAttributeCount() > 0 ? xmlReader.getAttributeValue(null, XmlConstants.NAME) : null;

                    if (Strings.isEmpty(nodeName)) {
                        nodeName = xmlReader.getLocalName();
                    }

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target type is specified for xml node: " + nodeName
                                + (nodeName.equals(xmlReader.getLocalName()) ? "" : " (element <" + xmlReader.getLocalName() + ">)"));
                    }

                    final T staxResult = readByStreamParser(xmlReader, configToUse, targetType);

                    if (boundedSource) {
                        // A second root or trailing text is an error for a whole document; an open reader is
                        // deliberately not drained (see the deserialize(Reader, ...) javadoc).
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
                        throw new ParsingException("No document element found in XML source");
                    }

                    // Resolved before the lookup so the failure below can name the key that was looked up: for
                    // <ns:order> that is "order", while getNodeName() reports the qualified "ns:order".
                    String nodeName = XmlUtil.getAttribute(node, XmlConstants.NAME);

                    if (Strings.isEmpty(nodeName)) {
                        nodeName = localName(node);
                    }

                    if (targetType == null && N.notEmpty(nodeTypes)) {
                        targetType = (Type<T>) nodeTypes.get(nodeName);
                    }

                    if (targetType == null) {
                        throw new ParsingException("No target class is specified for xml node: " + nodeName
                                + (nodeName.equals(node.getNodeName()) ? "" : " (element <" + node.getNodeName() + ">)"));
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
     * Reads a whole document with the StAX pull parser, starting from the root element the reader is
     * positioned on.
     *
     * @param <T> the type of the target object
     * @param xmlReader the stream reader positioned at the root element
     * @param config the deserialization configuration
     * @param inputType the type to deserialize the root element into
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML does not match the target type
     * @throws XMLStreamException if the underlying stream reader fails
     */
    <T> T readByStreamParser(final XMLStreamReader xmlReader, final XmlDeserConfig config, final Type<T> inputType)
            throws ParsingException, XMLStreamException {
        return readByStreamParser(xmlReader, config, null, null, false, false, false, true, inputType, inputType);
    }

    /** Maximum permitted XML element-nesting depth. Defends against StackOverflowError on hostile input. */
    private static final int MAX_XML_NESTING_DEPTH = 1000;

    /** Per-thread XML nesting depth counter. Single-element int[] used as a mutable holder. */
    private static final ThreadLocal<int[]> XML_NESTING_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /**
     * Increments the current thread's XML element-nesting depth.
     *
     * @throws ParsingException if the depth would exceed {@code MAX_XML_NESTING_DEPTH}
     */
    private static void enterXmlNesting() throws ParsingException {
        final int[] depth = XML_NESTING_DEPTH.get();
        if (++depth[0] > MAX_XML_NESTING_DEPTH) {
            depth[0]--;
            throw new ParsingException("XML nesting depth exceeded " + MAX_XML_NESTING_DEPTH + " (defends against stack-overflow DoS)");
        }
    }

    /**
     * Decrements the current thread's XML element-nesting depth, discarding the thread-local counter once
     * the outermost level is left so that no state is retained on pooled threads.
     */
    private static void exitXmlNesting() {
        final int[] depth = XML_NESTING_DEPTH.get();
        if (--depth[0] <= 0) {
            depth[0] = 0;
            XML_NESTING_DEPTH.remove();
        }
    }

    /**
     * Reads the element the StAX reader is positioned on - and, recursively, its children - into a bean,
     * array, collection, map, or scalar value, guarding the recursion with the XML nesting-depth counter.
     * Once a map key identifies an ignored entry, its value is consumed without type resolution or conversion.
     *
     * @param <T> the type of the target object
     * @param xmlReader the stream reader positioned at the element to read
     * @param config the deserialization configuration
     * @param propType the element/value type declared by the enclosing property, or {@code null}
     * @param propInfo the metadata of the property this element maps to, or {@code null}
     * @param checkedAttr whether the caller has already determined how element names are encoded
     * @param isTagByPropertyName whether the element name is the property name itself rather than a
     *        {@code name} attribute
     * @param ignoreTypeInfo whether a {@code type} attribute on the element should be ignored
     * @param isFirstCall whether this is the root element of the document
     * @param targetType the type to read this element into, or {@code null} to resolve it from the element
     * @param inputType the type originally requested for the document, used to resolve bean classes by node name
     * @return the deserialized value of type {@code T}
     * @throws ParsingException if the nesting depth limit is exceeded, or the XML does not match the target type
     * @throws XMLStreamException if the underlying stream reader fails
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    <T> T readByStreamParser(final XMLStreamReader xmlReader, final XmlDeserConfig config, Type<?> propType, PropInfo propInfo, boolean checkedAttr,
            boolean isTagByPropertyName, boolean ignoreTypeInfo, final boolean isFirstCall, Type<?> targetType, final Type<?> inputType)
            throws ParsingException, XMLStreamException {

        enterXmlNesting();
        try {
            return readByStreamParserBody(xmlReader, config, propType, propInfo, checkedAttr, isTagByPropertyName, ignoreTypeInfo, isFirstCall, targetType,
                    inputType);
        } finally {
            exitXmlNesting();
        }
    }

    /**
     * Performs the actual StAX read dispatch for
     * {@link #readByStreamParser(XMLStreamReader, XmlDeserConfig, Type, PropInfo, boolean, boolean, boolean, boolean, Type, Type)},
     * without the surrounding nesting-depth bookkeeping.
     *
     * @param <T> the type of the target object
     * @param xmlReader the stream reader positioned at the element to read
     * @param config the deserialization configuration
     * @param propType the element/value type declared by the enclosing property, or {@code null}
     * @param propInfo the metadata of the property this element maps to, or {@code null}
     * @param checkedAttr whether the caller has already determined how element names are encoded
     * @param isTagByPropertyName whether the element name is the property name itself
     * @param ignoreTypeInfo whether a {@code type} attribute on the element should be ignored
     * @param isFirstCall whether this is the root element of the document
     * @param targetType the type to read this element into, or {@code null} to resolve it from the element
     * @param inputType the type originally requested for the document
     * @return the deserialized value of type {@code T}
     * @throws ParsingException if the XML does not match the target type
     * @throws XMLStreamException if the underlying stream reader fails
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    @SuppressWarnings({ "null", "unused", "deprecation", "DataFlowIssue" })
    private <T> T readByStreamParserBody(final XMLStreamReader xmlReader, final XmlDeserConfig config, Type<?> propType, PropInfo propInfo, boolean checkedAttr,
            boolean isTagByPropertyName, boolean ignoreTypeInfo, final boolean isFirstCall, Type<?> targetType, final Type<?> inputType)
            throws ParsingException, XMLStreamException {

        final boolean hasPropTypes = config.hasValueTypes();
        String nodeName = null;

        if (checkedAttr) {
            nodeName = isTagByPropertyName || xmlReader.getAttributeCount() == 0 ? xmlReader.getLocalName() : getAttribute(xmlReader, XmlConstants.NAME);
        } else {
            final String nameAttr = getAttribute(xmlReader, XmlConstants.NAME);
            nodeName = Strings.isNotEmpty(nameAttr) ? nameAttr : xmlReader.getLocalName();
        }

        targetType = hasPropTypes ? config.getValueType(nodeName, targetType) : targetType;

        Class<?> targetClass = targetType == null ? null : targetType.javaType();

        // Approval is independent of whether ancestor metadata makes this attribute affect type selection.
        final Class<?> attributeClass = getAttributeTypeClass(xmlReader);
        targetClass = checkedAttr && ignoreTypeInfo ? targetClass : getConcreteClass(attributeClass, targetClass);

        if (targetType == null) {
            if (targetClass == null) {
                throw new ParsingException("Unable to determine target type for xml element: " + nodeName);
            }

            targetType = Type.of(targetClass);
        } else if (targetClass == null) {
            targetClass = targetType.javaType();
        } else if (!targetType.javaType().equals(targetClass)) {
            // Root container refinement must retain its element type before item metadata is read.
            final Type<?> concreteType = Type.of(targetClass);
            targetType = targetType.isCollection() ? retainDeclaredParameters(targetType, concreteType) : concreteType;
        }

        NodeType nodeType = null;

        if (nodeName == null) {
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
        boolean advanceEvent = true;

        switch (nodeType) {
            case ENTITY: {
                if (!targetType.isBean()) {
                    if ((propType != null) && propType.isBean()) {
                        targetType = propType;
                    } else {
                        if (inputType.isBean() && ClassUtil.getSimpleClassName(inputType.javaType()).equalsIgnoreCase(nodeName)) {
                            targetType = inputType;
                        } else {
                            final Class<?> classByNodeName;

                            if (inputType.isCollection() || inputType.isArray() || inputType.isMap()) {
                                classByNodeName = propType != null ? getClassByNodeName(nodeName, propType.javaType()) : null;
                            } else {
                                classByNodeName = getClassByNodeName(nodeName, inputType.javaType());
                            }

                            if (classByNodeName != null) {
                                targetType = Type.of(classByNodeName);
                            }

                            // When discovery fails, targetType keeps its non-bean value so checkBeanType
                            // raises the descriptive ParsingException (not Type.of(null)'s IAE).
                            checkBeanType(targetType.javaType(), nodeName, inputType.javaType());
                        }
                    }

                    targetClass = targetType.javaType();
                }

                // A bean-typed slot used to accept ANY child element as the wrapper and drop its text, so an
                // unwrapped nested bean (<friend><name>b</name></friend>) read back as an empty instance.
                final boolean unknownWrapper = !isFirstCall && propType != null && propType.isBean() && !(hasPropTypes && config.getValueType(nodeName) != null)
                        && !isKnownBeanWrapper(nodeName, propType, targetClass);
                final Type<?> slotType = propType;
                final String slotName = propInfo == null ? null : propInfo.name;

                if (!checkedAttr) {
                    isTagByPropertyName = Strings.isEmpty(getAttribute(xmlReader, XmlConstants.NAME));
                    ignoreTypeInfo = Strings.isEmpty(getAttribute(xmlReader, XmlConstants.TYPE));
                    checkedAttr = true;
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL));

                final boolean ignoreUnmatchedProperty = config.isIgnoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
                final Object result = isNullValue ? null : beanInfo.createBeanResult();

                for (int event = xmlReader.next(); xmlReader
                        .hasNext(); event = advanceEvent ? xmlReader.next() : xmlReader.getEventType(), advanceEvent = true) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL));

                                propName = isTagByPropertyName ? xmlReader.getLocalName() : getAttribute(xmlReader, XmlConstants.NAME);

                                if (propName == null) {
                                    throw new ParsingException("Missing '" + XmlConstants.NAME + "' attribute on XML element: " + xmlReader.getLocalName());
                                }

                                propInfo = beanInfo.getPropInfo(propName);

                                if (propName != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                                    propInfo = null;
                                    continue;
                                }

                                if (propInfo == null) {
                                    if (ignoreUnmatchedProperty) {
                                        continue;
                                    } else {
                                        throw new ParsingException("Unknown property element: " + propName + " for class: " + targetClass); //NOSONAR
                                    }
                                }

                                // Declared/configured property types never waive attribute approval.
                                final Type<?> attributeType = resolvePresentTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE));
                                propType = hasPropTypes ? config.getValueType(propName) : null;

                                if (propType == null) {
                                    propType = propInfo.jsonXmlType.isSerializable() || attributeType == null ? propInfo.jsonXmlType : attributeType;
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
                                            false, propType, inputType);

                                    // The nested value must be followed by the property's end element. The former
                                    // skip-to-end loop swallowed every sibling (an unwrapped bean's remaining fields,
                                    // a second wrapper) without a word; trailing text is ignored as SAX/DOM do.
                                    if (nextElementEvent(xmlReader) == XMLStreamConstants.START_ELEMENT) {
                                        throw new ParsingException(
                                                "Unexpected element <" + xmlReader.getLocalName() + "> after the value of property '" + propName + "'");
                                    }
                                }

                                if (xmlReader.getEventType() == XMLStreamConstants.END_ELEMENT
                                        && (isTagByPropertyName ? xmlReader.getLocalName().equals(propName)
                                                : xmlReader.getLocalName().equals(XmlConstants.PROPERTY))) {
                                    //noinspection StatementWithEmptyBody
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
                                    throw new ParsingException("Unknown parser error at element: " + xmlReader.getLocalName());
                                }
                            }

                            break;
                        }

                        case XMLStreamConstants.SPACE:
                        case XMLStreamConstants.CDATA:
                        case XMLStreamConstants.CHARACTERS: {
                            if (propName == null) {
                                if (unknownWrapper && !xmlReader.isWhiteSpace()) {
                                    // Text directly inside an element that is not a wrapper for the bean: the
                                    // unwrapped-bean shape, not mixed content.
                                    throw unexpectedBeanWrapper(nodeName, slotType, slotName);
                                }

                                // Mixed content outside a bean property is ignored, as it is by the DOM path.
                                // Do not call next() here: doing so would consume the following START_ELEMENT,
                                // and the for-loop update would then skip that property entirely.
                                break;
                            }

                            text = xmlReader.getText();

                            if (text != null && isTextEvent(event = xmlReader.next())) {
                                do {
                                    if (sb == null) {
                                        sb = new StringBuilder(text.length() * 2);
                                        sb.append(text);
                                    } else {
                                        // Bug fix: sb may be non-null but also non-empty when reused across
                                        // iterations (sb.setLength(0) clears it, but isEmpty() being false
                                        // would silently drop the initial `text` fragment). Always append
                                        // the first `text` chunk when sb is empty (regardless of how it got
                                        // to empty — fresh allocation or cleared via setLength(0)).
                                        if (sb.isEmpty()) {
                                            sb.append(text);
                                        }
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

                            propValue = (isNullValue || propInfo == null) ? null : propInfo.readPropValue(text);

                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (propInfo != null && propInfo.jsonXmlExpose != JsonXmlField.Direction.SERIALIZE_ONLY
                                        && (ignoredClassPropNames == null || !ignoredClassPropNames.contains(propName))) {
                                    // Text was decoded already: null can be the codec's legitimate result (including
                                    // the literal "null"). Only a genuinely empty element uses the empty-token fallback.
                                    propInfo.setPropValue(result, propValue);
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
                                if (propInfo == null || propInfo.jsonXmlExpose == JsonXmlField.Direction.SERIALIZE_ONLY
                                        || (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName))) {
                                    // ignore;
                                } else {
                                    propInfo.setPropValue(result, isNullValue ? null : (propValue == null ? propType.valueOf(Strings.EMPTY) : propValue));
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

                throw new ParsingException("Unknown parser error"); //NOSONAR
            }

            case MAP: {
                if ((targetType == null) || !targetType.isMap()) {
                    if ((propType != null) && propType.isMap()) {
                        targetType = propType;
                    } else {
                        targetType = linkedHashMapType;
                    }

                    targetClass = targetType.javaType();
                }

                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(0).isObject()) {
                    keyType = propInfo.jsonXmlType.parameterTypes().get(0);
                } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(0).isObject()) {
                    keyType = propType.parameterTypes().get(0);
                } else {
                    if (config.getMapKeyType() != null && !config.getMapKeyType().isObject()) {
                        keyType = config.getMapKeyType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                        keyType = targetType.parameterTypes().get(0);
                    }
                }

                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(1).isObject()) {
                    valueType = propInfo.jsonXmlType.parameterTypes().get(1);
                } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    valueType = propType.parameterTypes().get(1);
                } else {
                    if (config.getMapValueType() != null && !config.getMapValueType().isObject()) {
                        valueType = config.getMapValueType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                        valueType = targetType.parameterTypes().get(1);
                    }
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL));

                if (isNullValue) {
                    // Skip child elements until END_ELEMENT for null MAP
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        if (event == XMLStreamConstants.END_ELEMENT) {
                            return null;
                        }
                    }
                    return null;
                }

                @SuppressWarnings("rawtypes")
                final Map<Object, Object> mResult = N.newMap((Class<Map>) targetClass);
                Object key = null;
                Type<?> entryKeyType = null;
                Type<?> entryValueType = null;
                String typeAttr = null;
                boolean isStringKey = false;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // a map may only contain <entry> wrappers, as the SAX reader already requires
                            if (!XmlConstants.ENTRY.equals(xmlReader.getLocalName())) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

                            // move to a key element; text between the entry parts is skipped as the DOM reader does.
                            // The element must be named <key>: read positionally, <key>a</key><key>b</key> would be
                            // silently mis-read as the entry a -> b, which the SAX reader rejects.
                            if (nextElementEvent(xmlReader) != XMLStreamConstants.START_ELEMENT || !XmlConstants.KEY.equals(xmlReader.getLocalName())) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

                            typeAttr = getAttribute(xmlReader, XmlConstants.TYPE);
                            entryKeyType = resolvePresentTypeAttribute(typeAttr);

                            if (entryKeyType == null) {
                                entryKeyType = keyType;
                            }
                            isStringKey = entryKeyType.javaType().equals(String.class);
                            key = readWrappedValue(xmlReader, config, entryKeyType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType);

                            // NULL_STRING, not a bare null: contains(null) throws NPE on a Set.of(..) - see writeMap.
                            if (ignoredClassPropNames != null && ignoredClassPropNames.contains(key == null ? NULL_STRING : key.toString())) {
                                // The reader is on </key>; consume the remaining entry without resolving or converting its value.
                                for (int depth = 1; depth > 0 && xmlReader.hasNext();) {
                                    final int skippedEvent = xmlReader.next();
                                    if (skippedEvent == XMLStreamConstants.START_ELEMENT) {
                                        depth++;
                                    } else if (skippedEvent == XMLStreamConstants.END_ELEMENT) {
                                        depth--;
                                    }
                                }

                                key = null;
                                propValue = null;
                                break;
                            }

                            // move to a value element; it must be named <value> for the same reason
                            if (nextElementEvent(xmlReader) != XMLStreamConstants.START_ELEMENT || !XmlConstants.VALUE.equals(xmlReader.getLocalName())) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

                            typeAttr = getAttribute(xmlReader, XmlConstants.TYPE);
                            entryValueType = resolvePresentTypeAttribute(typeAttr);

                            if (entryValueType == null) {
                                entryValueType = valueType;
                            }

                            if (hasPropTypes && isStringKey) {
                                final Type<?> tmpType = config.getValueType(N.toString(key));
                                if (tmpType != null) {
                                    entryValueType = tmpType;
                                }
                            }

                            propValue = readWrappedValue(xmlReader, config, entryValueType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType);

                            // end of entry: a second value element (or anything else) is malformed
                            if (nextElementEvent(xmlReader) != XMLStreamConstants.END_ELEMENT) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

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

                throw new ParsingException("Unknown parser error");
            }

            case ARRAY: {
                boolean untypedArray = false;

                if ((targetType == null) || !targetType.isArray()) {
                    if ((propType != null) && propType.javaType().isArray()) {
                        targetType = propType;
                    } else {
                        targetType = strArrayType;
                        untypedArray = true;
                    }

                    targetClass = targetType.javaType();
                }

                Type<?> eleType = null;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = Type.of(propInfo.clazz.getComponentType());
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObject()) {
                        eleType = config.getElementType();
                        untypedArray = false;
                    } else {
                        eleType = targetType.isArray() ? targetType.elementType() : strType;
                    }
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL));

                if (isNullValue) {
                    // Skip child elements until END_ELEMENT for null ARRAY
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        if (event == XMLStreamConstants.END_ELEMENT) {
                            return collectionToArray(null, targetType);
                        }
                    }
                    return collectionToArray(null, targetType);
                }

                final List<Object> list = Objectory.createList();

                try {
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        switch (event) {
                            case XMLStreamConstants.START_ELEMENT: {
                                list.add(readWrappedValue(xmlReader, config, resolveItemType(xmlReader, eleType), checkedAttr, isTagByPropertyName,
                                        ignoreTypeInfo, inputType));

                                break;
                            }

                            // simple array with sample format <array>[1, 2, 3...]</array>
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
                                        } else {
                                            if (sb.isEmpty()) {
                                                sb.append(text);
                                            }
                                        }

                                        sb.append(xmlReader.getText());
                                    } while (isTextEvent(event = xmlReader.next()));

                                    if (sb != null && sb.length() > text.length()) {
                                        text = sb.toString();
                                        sb.setLength(0);
                                    }
                                }

                                // The text is the JSON form only when it is the whole content; text next to <e>
                                // children is mixed content and is ignored, as the SAX and DOM readers do.
                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (list.isEmpty() && !isNullValue) {
                                        propValue = parseJsonCollectionText(text, eleType, targetType);
                                    }

                                    if (propValue != null) {
                                        return (T) propValue;
                                    } else {
                                        return collectionToArray(list, untypedArray ? inferArrayType(list) : targetType);
                                    }
                                } else if (event == XMLStreamConstants.START_ELEMENT) {
                                    // The for-loop update would step past this element; read it here.
                                    list.add(readWrappedValue(xmlReader, config, resolveItemType(xmlReader, eleType), checkedAttr, isTagByPropertyName,
                                            ignoreTypeInfo, inputType));
                                }

                                break;
                            }

                            case XMLStreamConstants.END_ELEMENT: {
                                return collectionToArray(list, untypedArray ? inferArrayType(list) : targetType);
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

                throw new ParsingException("Unknown parser error");
            }

            case COLLECTION: {
                if ((targetType == null) || !targetType.isCollection()) {
                    if ((propType != null) && propType.isCollection()) {
                        targetType = propType;
                    } else {
                        targetType = listType;
                    }

                    targetClass = targetType.javaType();
                }

                Type<?> eleType = defaultValueType;

                if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                    eleType = Type.of(propInfo.clazz.getComponentType());
                } else if (propType != null && propType.parameterTypes().size() == 1 && Collection.class.isAssignableFrom(propType.javaType())
                        && !propType.parameterTypes().get(0).isObject()) {
                    eleType = propType.parameterTypes().get(0);
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObject()) {
                        eleType = config.getElementType();
                    } else if (targetType.elementType() != null && !targetType.elementType().isObject()) {
                        eleType = targetType.elementType();
                    }
                }

                isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL));

                if (isNullValue) {
                    // Skip child elements until END_ELEMENT for null COLLECTION
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        if (event == XMLStreamConstants.END_ELEMENT) {
                            return null;
                        }
                    }
                    return null;
                }

                @SuppressWarnings("rawtypes")
                final Collection<Object> result = N.newCollection((Class<Collection>) targetClass);

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            result.add(readWrappedValue(xmlReader, config, resolveItemType(xmlReader, eleType), checkedAttr, isTagByPropertyName,
                                    ignoreTypeInfo, inputType));

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
                                    } else {
                                        if (sb.isEmpty()) {
                                            sb.append(text);
                                        }
                                    }

                                    sb.append(xmlReader.getText());
                                } while (isTextEvent(event = xmlReader.next()));

                                if (sb != null && sb.length() > text.length()) {
                                    text = sb.toString();
                                    sb.setLength(0);
                                }
                            }

                            // The text is the JSON form only when it is the whole content; text next to <e>
                            // children is mixed content and is ignored, as the SAX and DOM readers do.
                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (result.isEmpty() && !isNullValue) {
                                    propValue = parseJsonCollectionText(text, eleType, targetType);
                                }

                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return (T) result;
                                }
                            } else if (event == XMLStreamConstants.START_ELEMENT) {
                                // The for-loop update would step past this element; read it here.
                                result.add(readWrappedValue(xmlReader, config, resolveItemType(xmlReader, eleType), checkedAttr, isTagByPropertyName,
                                        ignoreTypeInfo, inputType));
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            return (T) result;
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParsingException("Unknown parser error");
            }

            default:
                throw new ParsingException("Unsupported class type: " + targetClass + ". Only object array, collection, map and bean types are supported");
        }
    }

    private static final String MALFORMED_MAP_ENTRY = "An XML map entry must contain exactly one key element and one value element";

    /**
     * Parses the JSON text form of an array or collection element ({@code <array>[1, 2]</array>},
     * {@code <list>[1, 2]</list>}), shared by the three readers.
     *
     * @param text the element text
     * @param eleType the element type
     * @param targetType the array or collection type to produce
     * @return the parsed value, or {@code null} for blank text
     */
    private static Object parseJsonCollectionText(final String text, final Type<?> eleType, final Type<?> targetType) {
        if (Strings.isBlank(text)) {
            return null;
        }

        if (eleType.isString() || eleType.isObject()) {
            return targetType.valueOf(text);
        }

        return jsonParser.deserialize(text, JsonDeserConfig.create().setElementType(eleType.javaType()), targetType);
    }

    /**
     * Returns the namespace-local name of a node, falling back to the qualified node name for nodes
     * created by a builder that is not namespace-aware.
     */
    private static String localName(final Node node) {
        final String name = node.getLocalName();

        return name == null ? node.getNodeName() : name;
    }

    /**
     * Returns whether the element a bean-typed slot is read from is a wrapper the serializer emits for that
     * bean (its class name under any naming policy, or the concrete class named by a {@code type} attribute),
     * or names a sibling class resolvable by node name (polymorphic wrappers). Any other wrapper name is still
     * accepted when the element holds child elements (legacy leniency, pinned by the parser tests); it is
     * rejected by the readers when the element holds only text, which is the unwrapped-bean shape
     * ({@code <friend><name>b</name></friend>}) that used to read back as an empty bean.
     *
     * @param nodeName the element name, or the {@code name} attribute when properties are tagged that way
     * @param declaredType the declared bean type of the slot
     * @param concreteClass the class resolved from the element's {@code type} attribute, or the declared class
     * @return {@code true} if the element is a known wrapper for the bean
     */
    private static boolean isKnownBeanWrapper(final String nodeName, final Type<?> declaredType, final Class<?> concreteClass) {
        if (nodeName == null) {
            return true;
        }

        final Class<?> declaredClass = declaredType.javaType();

        if (isBeanWrapperName(nodeName, declaredClass)
                || (concreteClass != null && concreteClass != declaredClass && isBeanWrapperName(nodeName, concreteClass))) {
            return true;
        }

        // Polymorphic wrapper: the element names a subclass of the declared bean. The package scan matches ANY
        // class of that simple name under the root package, so the assignability check is what makes it a wrapper.
        final Class<?> classByNodeName = getClassByNodeName(nodeName, declaredClass);

        return classByNodeName != null && declaredClass.isAssignableFrom(classByNodeName);
    }

    /**
     * Builds the exception for a text-only element read where a bean wrapper was expected.
     *
     * @param nodeName the offending element name
     * @param declaredType the declared bean type of the slot
     * @param propName the property being read, for the message; may be {@code null}
     * @return the exception to throw
     */
    private static ParsingException unexpectedBeanWrapper(final String nodeName, final Type<?> declaredType, final String propName) {
        final Class<?> declaredClass = declaredType.javaType();

        return new ParsingException(
                "Expected element <" + String.valueOf(ParserUtil.getBeanInfo(declaredClass).xmlNameTags[NamingPolicy.CAMEL_CASE.ordinal()].name) + "> for a "
                        + ClassUtil.getSimpleClassName(declaredClass) + " value" + (propName == null ? "" : " of property '" + propName + "'") + " but found <"
                        + nodeName + "> holding text");
    }

    /**
     * Chooses the array type for an {@code <array>} element without a declared array type. The readers use
     * {@code String[]} as the placeholder; elements that are not Strings (beans, nested containers) get an
     * array of their common runtime class (as the readers produced before {@code collectionToArray} started
     * rejecting elements the declared component type cannot hold), or {@code Object[]} when they differ.
     *
     * @param c the collected elements
     * @return the array type to convert {@code c} to
     */
    private static Type<?> inferArrayType(final Collection<?> c) {
        Class<?> cls = null;

        for (final Object e : c) {
            if (e == null) {
                continue;
            }

            if (cls == null) {
                cls = e.getClass();
            } else if (cls != e.getClass()) {
                cls = Object.class;
                break;
            }
        }

        return cls == null || cls == String.class ? strArrayType : Type.of(N.newArray(cls, 0).getClass());
    }

    private static boolean isBeanWrapperName(final String nodeName, final Class<?> beanClass) {
        if (!Beans.isBeanClass(beanClass)) {
            return true;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (nodeName.equalsIgnoreCase(beanInfo.simpleClassName)) {
            return true;
        }

        for (final ParserUtil.XmlNameTag tag : beanInfo.xmlNameTags) {
            if (nodeName.length() == tag.name.length && nodeName.equalsIgnoreCase(String.valueOf(tag.name))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Resolves collection/array item metadata before advancing past its wrapper. Declared generic
     * arguments survive compatible concrete-container refinement; explicit element types take
     * precedence over conflicting metadata. Map wrappers apply their own key/value precedence.
     */
    private Type<?> resolveItemType(final XMLStreamReader xmlReader, final Type<?> declaredType) {
        return resolveItemType(declaredType, getConcreteClass(xmlReader, declaredType.javaType()), getAttribute(xmlReader, XmlConstants.TYPE));
    }

    private Type<?> resolveItemType(final Node node, final Type<?> declaredType) {
        return resolveItemType(declaredType, getConcreteClass(node, declaredType.javaType()), XmlUtil.getAttribute(node, XmlConstants.TYPE));
    }

    private Type<?> resolveItemType(final Attributes attributes, final Type<?> declaredType) {
        return resolveItemType(declaredType, getConcreteClass(attributes, declaredType.javaType()),
                attributes == null ? null : attributes.getValue(XmlConstants.TYPE));
    }

    private Type<?> resolveItemType(final Type<?> declaredType, final Class<?> concreteClass, final String typeAttribute) {
        // Callers first validate through getConcreteClass, even when the attribute is incompatible.
        // Item metadata is independent of whether an ancestor happened to carry a type attribute.
        final Type<?> attributeType = resolveTypeAttribute(typeAttribute);
        final Type<?> result = attributeType != null && attributeType.javaType() == concreteClass ? attributeType : declaredType;
        return retainDeclaredParameters(declaredType, result);
    }

    private static Type<?> retainDeclaredParameters(final Type<?> declaredType, Type<?> result) {
        final Class<?> concreteClass = result.javaType();
        for (final Type<?> parameter : declaredType.parameterTypes()) {
            if (!parameter.isObject()) {
                if (concreteClass == declaredType.javaType()) {
                    return declaredType;
                }
                if (concreteClass.getTypeParameters().length == declaredType.parameterTypes().size()) {
                    final int parametersStart = declaredType.name().indexOf('<');
                    if (parametersStart >= 0) {
                        result = Type.of(concreteClass.getCanonicalName() + declaredType.name().substring(parametersStart));
                    }
                }
                break;
            }
        }
        return result;
    }

    /**
     * Reads an entry key/value or item wrapper, leaving the reader on its end element.
     * Text is retained until the following event distinguishes a scalar from an indented child.
     * @throws XMLStreamException if advancing the XML reader fails
     * @throws ParsingException if a value wrapper mixes scalar and nested values, has an unexpected closing element, or ends prematurely
     */
    private Object readWrappedValue(final XMLStreamReader xmlReader, final XmlDeserConfig config, final Type<?> valueType, final boolean checkedAttr,
            final boolean isTagByPropertyName, final boolean ignoreTypeInfo, final Type<?> inputType) throws XMLStreamException, ParsingException {
        final boolean isNull = Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL));
        final StringBuilder text = new StringBuilder();
        while (xmlReader.hasNext()) {
            final int event = xmlReader.next();
            if (isTextEvent(event)) {
                text.append(xmlReader.getText());
            } else if (event == XMLStreamConstants.START_ELEMENT) {
                if (!text.toString().isBlank()) {
                    throw new ParsingException("Mixed scalar and nested XML values are not supported");
                }
                final Object value = readByStreamParser(xmlReader, config, valueType, null, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, valueType,
                        inputType);
                if (nextStructuralEvent(xmlReader) != XMLStreamConstants.END_ELEMENT) {
                    throw new ParsingException("Expected the end of an XML value wrapper");
                }
                return isNull ? null : value;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return isNull ? null : valueType.valueOf(text.toString());
            }
        }
        throw new ParsingException("Unexpected end of XML value wrapper");
    }

    /**
     * Checks whether the given StAX event represents a text event. {@code CHARACTERS}, {@code CDATA} and
     * {@code SPACE} carry text content; because the StAX reader is not configured to coalesce,
     * {@code <![CDATA[...]]>} sections arrive as separate {@code CDATA} events and must be treated
     * the same as ordinary character data.
     *
     * @param event the StAX event type
     * @return {@code true} if the event is {@code CHARACTERS}, {@code CDATA} or {@code SPACE}
     */
    private static boolean isTextEvent(final int event) {
        return event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA || event == XMLStreamConstants.SPACE;
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
     * Reads a whole DOM subtree, starting from the given root node.
     *
     * @param <T> the type of the target object
     * @param node the root element node to read; a {@link Document} is read as its document element
     * @param config the deserialization configuration; may be {@code null} for defaults
     * @param targetType the type to deserialize the root node into
     * @return the deserialized object of type {@code T}
     * @throws ParsingException if the XML does not match the target type
     */
    <T> T readByDOMParser(final Node node, final XmlDeserConfig config, Type<? extends T> targetType) throws ParsingException {
        final XmlDeserConfig configToUse = check(config);

        return readByDOMParser(toElementNode(node), configToUse, configToUse.getElementType(), false, false, false, true, targetType);
    }

    /**
     * Reads the given DOM element - and, recursively, its children - into a bean, array, collection, map,
     * or scalar value, guarding the recursion with the XML nesting-depth counter.
     *
     * @param <T> the type of the target object
     * @param node the element node to read; {@code null} is returned for any non-element node
     * @param config the deserialization configuration
     * @param propType the element/value type declared by the enclosing property, or {@code null}
     * @param checkedAttr whether the caller has already determined how element names are encoded
     * @param isTagByPropertyName whether the element name is the property name itself rather than a
     *        {@code name} attribute
     * @param ignoreTypeInfo whether a {@code type} attribute on the element should be ignored
     * @param isFirstCall whether this is the root element of the document
     * @param inputType the type originally requested for the document, used to resolve bean classes by node name
     * @return the deserialized value of type {@code T}
     * @throws ParsingException if the nesting depth limit is exceeded, or the XML does not match the target type
     */
    <T> T readByDOMParser(final Node node, final XmlDeserConfig config, Type<?> propType, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final boolean isFirstCall, final Type<T> inputType) throws ParsingException {
        enterXmlNesting();
        try {
            return readByDOMParserBody(node, config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, isFirstCall, inputType);
        } finally {
            exitXmlNesting();
        }
    }

    /**
     * Performs the actual DOM read dispatch for
     * {@link #readByDOMParser(Node, XmlDeserConfig, Type, boolean, boolean, boolean, boolean, Type)},
     * without the surrounding nesting-depth bookkeeping.
     *
     * @param <T> the type of the target object
     * @param node the element node to read; {@code null} is returned for any non-element node
     * @param config the deserialization configuration
     * @param propType the element/value type declared by the enclosing property, or {@code null}
     * @param checkedAttr whether the caller has already determined how element names are encoded
     * @param isTagByPropertyName whether the element name is the property name itself
     * @param ignoreTypeInfo whether a {@code type} attribute on the element should be ignored
     * @param isFirstCall whether this is the root element of the document
     * @param inputType the type originally requested for the document
     * @return the deserialized value of type {@code T}
     * @throws ParsingException if the XML does not match the target type
     */
    @SuppressWarnings("deprecation")
    private <T> T readByDOMParserBody(final Node node, final XmlDeserConfig config, Type<?> propType, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final boolean isFirstCall, final Type<T> inputType) throws ParsingException {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        // Check before the null-root shortcut and reuse the result when type refinement is enabled.
        final Class<?> attributeClass = getAttributeTypeClass(node);
        final boolean hasPropTypes = config.hasValueTypes();

        String nodeName = checkedAttr ? (isTagByPropertyName ? localName(node) : XmlUtil.getAttribute(node, XmlConstants.NAME))
                : XmlUtil.getAttribute(node, XmlConstants.NAME);
        nodeName = (nodeName == null) ? localName(node) : nodeName;

        Type<?> targetType = null;
        Class<?> targetClass = null;

        if (isFirstCall) {
            if (Boolean.parseBoolean(XmlUtil.getAttribute(node, XmlConstants.IS_NULL))) {
                // The root itself carries the null marker: the StAX reader returns null, so do the same
                // instead of building an empty instance.
                return null;
            }

            targetType = inputType;
            targetClass = targetType.javaType();
        } else {
            if (propType == null || propType.isString() || propType.isObject()) {
                targetType = hasPropTypes ? config.getValueType(nodeName, null) : null;
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

        targetClass = checkedAttr && ignoreTypeInfo ? targetClass : getConcreteClass(attributeClass, targetClass);

        if (targetType == null) {
            if (targetClass == null) {
                throw new ParsingException("Unable to determine target type for xml node: " + nodeName);
            }

            targetType = Type.of(targetClass);
        } else if (targetClass == null) {
            targetClass = targetType.javaType();
        } else if (!targetType.javaType().equals(targetClass)) {
            targetType = Type.of(targetClass);
        }

        final NodeType nodeType = getNodeType(nodeName, null);

        final NodeList propNodes = node.getChildNodes();
        final int propNodeLength = getNodeLength(propNodes);
        PropInfo propInfo = null;
        Node propNode = null;
        String propName = null;
        Object propValue = null;

        switch (nodeType) {
            case ENTITY: {
                if (!targetType.isBean()) {
                    if ((propType != null) && propType.isBean()) {
                        targetType = propType;
                    } else {
                        if (inputType.isBean() && ClassUtil.getSimpleClassName(inputType.javaType()).equalsIgnoreCase(nodeName)) {
                            targetType = inputType;
                        } else {
                            final Class<?> classByNodeName;

                            if (inputType.isCollection() || inputType.isArray() || inputType.isMap()) {
                                classByNodeName = propType != null ? getClassByNodeName(nodeName, propType.javaType()) : null;
                            } else {
                                classByNodeName = getClassByNodeName(nodeName, inputType.javaType());
                            }

                            if (classByNodeName != null) {
                                targetType = Type.of(classByNodeName);
                            }

                            // When discovery fails, targetType keeps its non-bean value so checkBeanType
                            // raises the descriptive ParsingException (not Type.of(null)'s IAE).
                            checkBeanType(targetType.javaType(), nodeName, inputType.javaType());
                        }
                    }

                    targetClass = targetType.javaType();
                }

                if (!isFirstCall && propType != null && propType.isBean() && !(hasPropTypes && config.getValueType(nodeName) != null)
                        && !isKnownBeanWrapper(nodeName, propType, targetClass) && XmlUtil.isTextElement(node)
                        && Strings.isNotBlank(XmlUtil.getTextContent(node))) {
                    // Same guard as the StAX reader: a text-only element that is no wrapper for the bean is the
                    // unwrapped-bean shape, which used to read back as an empty instance.
                    throw unexpectedBeanWrapper(nodeName, propType, null);
                }

                if (!checkedAttr) {
                    isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(node, XmlConstants.NAME));
                    ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(node, XmlConstants.TYPE));
                    checkedAttr = true;
                }

                final boolean ignoreUnmatchedProperty = config.isIgnoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
                final Object result = beanInfo.createBeanResult();

                for (int i = 0; i < propNodeLength; i++) {
                    propNode = propNodes.item(i);

                    if (propNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    propName = isTagByPropertyName ? localName(propNode) : XmlUtil.getAttribute(propNode, XmlConstants.NAME); //NOSONAR

                    if (propName == null) {
                        throw new ParsingException("Missing '" + XmlConstants.NAME + "' attribute on XML element: " + propNode.getNodeName());
                    }

                    propInfo = beanInfo.getPropInfo(propName);

                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propName)) {
                        continue;
                    }

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        } else {
                            throw new ParsingException("Unknown property element: " + propName + " for class: " + ClassUtil.getCanonicalClassName(targetClass));
                        }
                    }

                    // Check the property wrapper before scalar/configured-type shortcuts on the DOM path.
                    resolvePresentTypeAttribute(XmlUtil.getAttribute(propNode, XmlConstants.TYPE));
                    propType = hasPropTypes ? config.getValueType(propName) : null;

                    if (propType == null) {
                        if (propInfo.jsonXmlType.isSerializable()) {
                            propType = propInfo.jsonXmlType;
                        } else {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType : Type.of(getConcreteClass(propNode, propInfo.jsonXmlType.javaType()));
                        }
                    }

                    if (XmlUtil.isTextElement(propNode)) {
                        propValue = getPropValue(propName, propType, propInfo, propNode);
                    } else {
                        //noinspection ConstantValue
                        propValue = readByDOMParser(checkOneNode(propNode), config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputType);
                    }

                    if (propInfo.jsonXmlExpose != JsonXmlField.Direction.SERIALIZE_ONLY) {
                        propInfo.setPropValue(result, propValue);
                    }
                }

                return beanInfo.finishBeanResult(result);
            }

            case MAP: {
                if ((targetType == null) || !targetType.isMap()) {
                    if ((propType != null) && propType.isMap()) {
                        targetType = propType;
                    } else {
                        targetType = linkedHashMapType;
                    }

                    targetClass = targetType.javaType();
                }

                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(Map.class);
                Type<?> keyType = defaultKeyType;

                if (propType != null && propType.isMap() && !propType.parameterTypes().get(0).isObject()) {
                    keyType = propType.parameterTypes().get(0);
                } else {
                    if (config.getMapKeyType() != null && !config.getMapKeyType().isObject()) {
                        keyType = config.getMapKeyType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                        keyType = targetType.parameterTypes().get(0);
                    }
                }

                final boolean isStringKey = keyType.javaType() == String.class;

                Type<?> valueType = defaultValueType;

                if (propType != null && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    valueType = propType.parameterTypes().get(1);
                } else {
                    if (config.getMapValueType() != null && !config.getMapValueType().isObject()) {
                        valueType = config.getMapValueType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                        valueType = targetType.parameterTypes().get(1);
                    }
                }

                final Map<Object, Object> mResult = newPropInstance(targetClass, node);

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

                    if (entryNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    // a map may only contain <entry> wrappers, as the SAX reader already requires
                    if (!XmlConstants.ENTRY.equals(localName(entryNode))) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY);
                    }

                    subEntryNodes = entryNode.getChildNodes();

                    propKeyNode = null;
                    propValueNode = null;

                    for (int index = 0; index < subEntryNodes.getLength(); index++) {
                        final Node childNode = subEntryNodes.item(index);

                        if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (propKeyNode == null) {
                            propKeyNode = childNode;
                        } else if (propValueNode == null) {
                            propValueNode = childNode;
                        } else {
                            throw new ParsingException("An XML map entry must contain exactly one key element and one value element");
                        }
                    }

                    if (propKeyNode == null || propValueNode == null) {
                        throw new ParsingException("An XML map entry must contain exactly one key element and one value element");
                    }

                    // the two child elements must actually be <key> then <value>: taken positionally,
                    // <key>a</key><key>b</key> and <value>1</value><key>k</key> would be silently mis-read
                    if (!XmlConstants.KEY.equals(localName(propKeyNode)) || !XmlConstants.VALUE.equals(localName(propValueNode))) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY);
                    }

                    propKeyClass = checkedAttr ? (ignoreTypeInfo ? keyType.javaType() : getConcreteClass(propKeyNode, keyType.javaType()))
                            : getConcreteClass(propKeyNode, keyType.javaType());

                    if (propKeyClass == Object.class) {
                        propKeyClass = String.class;
                    }

                    propKeyType = propKeyClass == keyType.javaType() ? keyType : Type.of(propKeyClass);

                    //noinspection DataFlowIssue
                    if (XmlUtil.isTextElement(propKeyNode)) {
                        //noinspection ConstantValue
                        propKey = getPropValue(XmlConstants.KEY, propKeyType, propInfo, propKeyNode);
                    } else {
                        propKey = readByDOMParser(checkOneNode(propKeyNode), config, keyType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputType);
                    }

                    // NULL_STRING, not a bare null: contains(null) throws NPE on a Set.of(..) - see writeMap.
                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propKey == null ? NULL_STRING : propKey.toString())) {
                        continue;
                    }

                    propValueType = hasPropTypes && isStringKey ? config.getValueType(N.toString(propKey)) : null;

                    if (propValueType == null) {
                        propValueClass = checkedAttr ? (ignoreTypeInfo ? valueType.javaType() : getConcreteClass(propValueNode, valueType.javaType()))
                                : getConcreteClass(propValueNode, valueType.javaType());
                    } else {
                        propValueClass = propValueType.javaType();
                    }

                    if (propValueClass == Object.class) {
                        propValueClass = String.class;
                    }

                    propValueType = propValueClass == valueType.javaType() ? valueType : Type.of(propValueClass);

                    //noinspection DataFlowIssue
                    if (XmlUtil.isTextElement(propValueNode)) {
                        //noinspection ConstantValue
                        propValue = getPropValue(XmlConstants.VALUE, propValueType, propInfo, propValueNode);
                    } else {
                        propValue = readByDOMParser(checkOneNode(propValueNode), config, propValueType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                inputType);
                    }

                    mResult.put(propKey, propValue);
                }

                return (T) mResult;
            }

            case ARRAY: { //NOSONAR
                boolean untypedArray = false;

                if ((targetType == null) || !targetType.isArray()) {
                    if ((propType != null) && propType.isArray()) {
                        targetType = propType;
                    } else {
                        targetType = strArrayType;
                        untypedArray = true;
                    }

                    targetClass = targetType.javaType();
                }

                Type<?> eleType = null;

                if (propType != null && (propType.isArray() || propType.isCollection()) && propType.elementType() != null
                        && !propType.elementType().isObject()) {
                    eleType = propType.elementType();
                    untypedArray = false;
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObject()) {
                        eleType = config.getElementType();
                        untypedArray = false;
                    } else {
                        eleType = targetType.isArray() ? targetType.elementType() : objType;
                    }
                }

                propName = XmlConstants.E; //NOSONAR

                if (XmlUtil.isTextElement(node)) {
                    final String st = XmlUtil.getTextContent(node);

                    // Whitespace-only content is the pretty-printed empty array; Type.valueOf(blank) would answer null.
                    if (Strings.isBlank(st)) {
                        return N.newArray(eleType.javaType(), 0);
                    } else {
                        final Object parsed = targetType.valueOf(st);

                        if (parsed == null) {
                            throw new ParsingException("Cannot parse array content: " + st);
                        }

                        return (T) parsed;
                    }
                } else {
                    final List<Object> c = Objectory.createList();

                    try {
                        final NodeList eleNodes = node.getChildNodes();
                        Node eleNode = null;

                        for (int k = 0; k < eleNodes.getLength(); k++) {
                            eleNode = eleNodes.item(k);

                            if (eleNode.getNodeType() != Node.ELEMENT_NODE) {
                                continue;
                            }

                            propType = resolveItemType(eleNode, eleType);
                            if (propType.isObject()) {
                                propType = strType;
                            }

                            if (XmlUtil.isTextElement(eleNode)) {
                                //noinspection ConstantValue
                                c.add(getPropValue(propName, propType, propInfo, eleNode));
                            } else {
                                c.add(readByDOMParser(checkOneNode(eleNode), config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false,
                                        inputType));
                            }
                        }

                        return collectionToArray(c, untypedArray ? inferArrayType(c) : targetType);
                    } finally {
                        Objectory.recycle(c);
                    }
                }
            }

            case COLLECTION: {
                if ((targetType == null) || !targetType.isCollection()) {
                    if ((propType != null) && propType.isCollection()) {
                        targetType = propType;
                    } else {
                        targetType = listType;
                    }

                    targetClass = targetType.javaType();
                }

                Type<?> eleType = null;

                if (propType != null && (propType.isCollection() || propType.isArray()) && !propType.elementType().isObject()) {
                    eleType = propType.elementType();
                } else {
                    if (config.getElementType() != null && !config.getElementType().isObject()) {
                        eleType = config.getElementType();
                    } else {
                        eleType = targetType.elementType() == null ? objType : targetType.elementType();
                    }
                }

                propName = XmlConstants.E; //NOSONAR

                final Collection<Object> result = newPropInstance(targetClass, node);

                if (XmlUtil.isTextElement(node)) {
                    // JSON text form <list>[1, 2, 3]</list>, as the StAX reader accepts it; blank text is an empty collection.
                    final Object parsed = parseJsonCollectionText(XmlUtil.getTextContent(node), eleType, targetType);

                    if (parsed != null) {
                        result.addAll((Collection<Object>) parsed);
                    }

                    return (T) result;
                }

                final NodeList eleNodes = node.getChildNodes();
                Node eleNode = null;

                for (int k = 0; k < eleNodes.getLength(); k++) {
                    eleNode = eleNodes.item(k);

                    if (eleNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    propType = resolveItemType(eleNode, eleType);
                    if (propType.isObject()) {
                        propType = strType;
                    }

                    if (XmlUtil.isTextElement(eleNode)) {
                        //noinspection ConstantValue
                        result.add(getPropValue(propName, propType, propInfo, eleNode));
                    } else {
                        result.add(
                                readByDOMParser(checkOneNode(eleNode), config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, inputType));
                    }
                }

                return (T) result;
            }

            default:
                throw new ParsingException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only object array, collection, map and bean types are supported");
        }
    }

    /**
     * Verifies that the class resolved for an XML node is usable as a bean.
     *
     * @param targetClass the class resolved for the node
     * @param nodeName the node name that produced {@code targetClass}, used in the error message
     * @param inputClass the class whose package was searched, used in the error message
     * @throws ParsingException if {@code targetClass} is not a bean class
     */
    private static void checkBeanType(final Class<?> targetClass, final String nodeName, final Class<?> inputClass) throws ParsingException {
        if (!Beans.isBeanClass(targetClass)) {
            throw new ParsingException("No bean class found for node name: " + nodeName + " in package of class: " + inputClass.getCanonicalName());
        }
    }

    /**
     * Classifies an XML element by its name and the kind of its parent element.
     *
     * @param nodeName the element name
     * @param previousNodeType the node type of the enclosing element
     * @return {@code PROPERTY} for any child of a bean element; otherwise the type reserved for the
     *         element name (array, list/set/collection, map, entry, key, value, e), or {@code ENTITY}
     *         when the name is not reserved
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
     * Resolves the bean class whose simple name matches an XML element name, searching the package of
     * {@code cls} (or, for JDK classes, the package of the nearest application class on the call stack).
     * Results - including misses - are cached per {@code cls}.
     *
     * @param <T> the resolved class type
     * @param nodeName the element name to resolve; matched case-insensitively, and retried against the
     *        normalized form of the name
     * @param cls the class whose package anchors the search; {@code null} yields {@code null}
     * @return the matching class, or {@code null} if no class in the searched package matches or that package
     *         cannot be scanned
     */
    @SuppressWarnings({ "unchecked", "deprecation", "null" })
    private static <T> Class<T> getClassByNodeName(final String nodeName, final Class<?> cls) {
        if (cls == null) {
            return null;
        }

        Class<?> nodeClass = null;
        Map<String, Class<?>> nodeNameClassMap = nodeNameClassMapPool.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());
        nodeClass = nodeNameClassMap.get(nodeName);

        if (nodeClass == null) {
            String packName = null;

            if (cls.getPackage() == null || cls.getPackage().getName().startsWith("java.lang") || cls.getPackage().getName().startsWith("java.util")) {
                final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                final String xmlUtilPackageName = AbacusXmlParserImpl.class.getPackage().getName();
                String className = null;

                for (int i = stackTrace.length - 1; i >= 0; i--) {
                    className = stackTrace[i].getClassName();

                    if (!(className.startsWith("java.lang") || className.startsWith("java.util") || className.startsWith(xmlUtilPackageName))) {
                        packName = ClassUtil.forName(className).getPackage().getName();

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

            final List<Class<?>> classList = findClassesInPackageOrEmpty(packName);

            for (final Class<?> e : classList) {
                if (ClassUtil.getSimpleClassName(e).equalsIgnoreCase(nodeName)) {
                    nodeClass = e;

                    break;
                }
            }

            if ((nodeClass == null) && !nodeName.equalsIgnoreCase(Beans.normalizePropName(nodeName))) {
                nodeClass = getClassByNodeName(Beans.normalizePropName(nodeName), cls);
            }

            if (nodeClass == null) {
                nodeClass = ClassUtil.SENTINEL_CLASS;
            }

            nodeNameClassMap.put(nodeName, nodeClass);
        }

        return (Class<T>) ((nodeClass == ClassUtil.SENTINEL_CLASS) ? null : nodeClass);
    }

    /**
     * Runs the package scan for {@link #getClassByNodeName(String, Class)}, answering an empty list when the
     * package cannot be scanned.
     * <p>
     * When the anchor class is a JDK class the scan root is guessed from the call stack, so it can land on a
     * package that is not on the classpath as a scannable resource - under the SAX backend the frames below the
     * parser belong to the XML implementation itself, so a caller inside this package resolves {@code com.sun.org}.
     * {@link ClassUtil#findClassesInPackage(String, boolean, boolean)} reports that with an
     * {@link IllegalArgumentException}, which would otherwise escape a public {@code deserialize} call in place of
     * the descriptive {@link ParsingException} raised for an unresolvable node name.
     *
     * @param packName the package to scan
     * @return the classes found in {@code packName}, or an empty list if it cannot be scanned
     */
    private static List<Class<?>> findClassesInPackageOrEmpty(final String packName) {
        try {
            return ClassUtil.findClassesInPackage(packName, true, true);
        } catch (final IllegalArgumentException e) {
            return N.emptyList();
        }
    }

    /**
     * Borrows a SAX handler from the pool (or creates one) and initialises it for a single parse.
     * The caller must return it via {@code recycle(XmlSAXHandler)} once parsing finishes.
     *
     * @param <T> the target object type produced by the handler
     * @param config the deserialization configuration
     * @param nodeTypes a mapping from root node name to target type, may be {@code null}
     * @param targetType the type to deserialize into, may be {@code null}
     * @return a handler ready to be passed to a {@code SAXParser}
     */
    @SuppressWarnings("unchecked")
    private <T> XmlSAXHandler<T> getXmlSAXHandler(final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType) {
        XmlSAXHandler<T> xmlSAXHandler = (XmlSAXHandler<T>) xmlSAXHandlerPool.poll();

        if (xmlSAXHandler == null) {
            xmlSAXHandler = new XmlSAXHandler<>();
        }

        xmlSAXHandler.xmlParser = this;
        xmlSAXHandler.nodeTypes = nodeTypes;
        xmlSAXHandler.inputType = targetType;
        xmlSAXHandler.setConfig(config);

        return xmlSAXHandler;
    }

    /**
     * Resets a SAX handler and returns it to the pool. A {@code null} handler, or one offered when the
     * pool is already full, is discarded.
     *
     * @param xmlSAXHandler the handler to recycle, may be {@code null}
     */
    private static void recycle(final XmlSAXHandler<?> xmlSAXHandler) {
        if (xmlSAXHandler == null) {
            return;
        }

        synchronized (xmlSAXHandlerPool) {
            if (xmlSAXHandlerPool.size() < POOL_SIZE) {
                xmlSAXHandler.reset();
                xmlSAXHandler.xmlParser = null;
                xmlSAXHandlerPool.add(xmlSAXHandler);
            }
        }
    }

    /**
     * SAX event handler for XML deserialization, supporting beans, arrays, collections, and maps.
     *
     * <p>This handler maintains parse state via stacks of node types, values, and property metadata
     * as SAX events arrive. After parsing completes, the result is accessible through
     * {@code resultHolder}. Instances are pooled and re-used; configuration must be initialised via
     * {@code setConfig(XmlDeserConfig)} (along with the {@code nodeTypes} and {@code inputType}
     * fields) before parsing, and {@code reset()} is invoked when the handler is returned to the
     * pool.</p>
     *
     * @param <T> the target object type produced by this SAX handler
     */
    static final class XmlSAXHandler<T> extends DefaultHandler { // NOSONAR

        private AbacusXmlParserImpl xmlParser;

        private final Holder<T> resultHolder = new Holder<>();

        private StringBuilder sb = null;

        private Map<String, Type<?>> nodeTypes;

        private Type<? extends T> inputType;

        private XmlDeserConfig config;

        private boolean hasPropTypes = false;

        private boolean ignoreUnmatchedProperty;

        private Collection<String> mapIgnoredPropNames;

        private BeanInfo beanInfo;

        private PropInfo propInfo;

        private final List<String> beanOrPropNameQueue = new ArrayList<>();

        private final List<NodeType> nodeTypeQueue = new ArrayList<>();

        // Parallel to nodeTypeQueue: each property/item/key/value wrapper may contain only one child value.
        // Track presence per depth, not through eleValue, which nested parsing consumes and overwrites.
        private final BooleanList valueChildSeenQueue = new BooleanList();

        private final List<Object> nodeValueQueue = new ArrayList<>();

        private final List<Object> keyQueue = new ArrayList<>();

        // Scalar state for the node currently being parsed.
        private String nodeName;

        private String beanOrPropName;

        private Collection<String> ignoredClassPropNames;

        private Object bean;

        private Class<?> beanClass;

        private Object array;

        private Collection<Object> coll;

        private Map<Object, Object> map;

        private Object eleValue;

        private Type<?> targetType;

        private Class<?> targetClass;

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

        // Set when the root element carries isNull="true": the whole document is skipped and the result stays null.
        private boolean rootIsNull = false;

        // keyQueue size at the start of each open <entry>, so a missing/duplicate key or value is detected per entry.
        private final List<Integer> entryKeyMarkQueue = new ArrayList<>();

        // Placeholder pushed for an <array> without a declared type (identity-compared in endElement).
        private static final String[] UNTYPED_ARRAY_PLACEHOLDER = new String[0];

        // The bean whose wrapper element name is unknown, until a property child proves it is a wrapper.
        private Object unknownWrapperBean;

        private Type<?> unknownWrapperType;

        private String unknownWrapperName;

        /**
         * @throws ParsingException if the element has no resolvable input class, required name metadata is missing, an unknown bean property is rejected,
         *         a value wrapper contains multiple child elements, or the element violates the enclosing container structure
         */
        @SuppressWarnings("unchecked")
        @Override
        public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes attrs) throws ParsingException {
            if (inIgnorePropRefCount > 0) {
                inIgnorePropRefCount++;

                return;
            }

            if (sb == null) {
                sb = Objectory.createStringBuilder();
            } else {
                sb.setLength(0);
            }

            // The parser is namespace-aware: match on the local name so a prefixed document (<p:name>) resolves
            // the same properties as the StAX reader does.
            nodeName = Strings.isEmpty(localName) ? qName : localName;

            if (checkedPropNameTag) {
                beanOrPropName = isTagByPropertyName || attrs == null || attrs.getLength() == 0 ? nodeName : attrs.getValue(XmlConstants.NAME);
            } else {
                beanOrPropName = attrs.getValue(XmlConstants.NAME);

                if (Strings.isEmpty(beanOrPropName)) {
                    beanOrPropName = nodeName;
                }
            }

            if (isFirstCall) {
                if ((nodeTypes != null) && (inputType == null)) {
                    inputType = (Type<T>) nodeTypes.get(beanOrPropName);
                }

                if (inputType == null) {
                    throw new ParsingException("No input class found for node: " + nodeName);
                }

                targetType = inputType;
            } else {
                if (propType == null || propType.isString() || propType.isObject()) {
                    targetType = hasPropTypes ? config.getValueType(nodeName, null) : null;
                } else {
                    targetType = propType;
                }
            }

            if (targetType != null) {
                targetClass = targetType.javaType();

                targetClass = checkedTypeInfo
                        ? ((ignoreTypeInfo || attrs == null || attrs.getLength() == 0) ? targetClass : xmlParser.getConcreteClass(attrs, targetClass))
                        : ((attrs == null || attrs.getLength() == 0) ? targetClass : xmlParser.getConcreteClass(attrs, targetClass));

                if (!targetType.javaType().equals(targetClass)) {
                    targetType = Type.of(targetClass);
                }
            }

            final NodeType previousNodeType = (nodeTypeQueue.isEmpty()) ? null : nodeTypeQueue.get(nodeTypeQueue.size() - 1);

            if (previousNodeType == NodeType.PROPERTY || previousNodeType == NodeType.ELEMENT || previousNodeType == NodeType.KEY
                    || previousNodeType == NodeType.VALUE) {
                // Reject a second direct child before it can overwrite the completed value. Children inside
                // the bean/collection/map itself have a different parent frame and remain unrestricted.
                if (valueChildSeenQueue.set(nodeTypeQueue.size() - 1, true)) {
                    throw new ParsingException("Unexpected element <" + nodeName + "> after the value of an XML value wrapper");
                }
            }

            final NodeType nodeType = getNodeType(nodeName, previousNodeType);

            isNull = attrs != null && attrs.getLength() != 0 && Boolean.parseBoolean(attrs.getValue(XmlConstants.IS_NULL));

            if (isFirstCall && isNull
                    && (nodeType == NodeType.ENTITY || nodeType == NodeType.MAP || nodeType == NodeType.ARRAY || nodeType == NodeType.COLLECTION)) {
                // The root itself carries the null marker: the StAX reader returns null, so do the same instead
                // of building an empty instance. Everything inside it is skipped like an ignored property.
                rootIsNull = true;
                isFirstCall = false;
                inIgnorePropRefCount = 1;

                return;
            }

            // A map may only contain <entry> wrappers, the same requirement the StAX and DOM readers carry.
            // Anything else is classified as a bean node here, so <map><pair><key>a</key><value>1</value></pair></map>
            // and <map><foo>a</foo></map> were read as a bean and then discarded: the reader returned an EMPTY
            // map, silently dropping the entry, instead of reporting the document as malformed.
            if (previousNodeType == NodeType.MAP && nodeType != NodeType.ENTRY) {
                throw new ParsingException(MALFORMED_MAP_ENTRY);
            }

            switch (nodeType) {
                case ENTITY: {
                    if (!checkedPropNameTag) {
                        isTagByPropertyName = (attrs == null) || (Strings.isEmpty(attrs.getValue(XmlConstants.NAME)));
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XmlConstants.TYPE)));
                        checkedPropNameTag = true;
                        checkedTypeInfo = true;
                    }

                    if (!isTagByPropertyName) {
                        //noinspection DataFlowIssue
                        beanOrPropName = attrs.getValue(XmlConstants.NAME);
                        beanOrPropNameQueue.add(beanOrPropName);
                    }

                    targetType = hasPropTypes ? config.getValueType(beanOrPropName, targetType) : targetType;

                    if (targetType == null || !targetType.isBean()) {
                        if ((eleType != null) && eleType.isBean()) {
                            targetType = eleType;
                        } else {
                            if (inputType.isBean() && ClassUtil.getSimpleClassName(inputType.javaType()).equalsIgnoreCase(beanOrPropName)) {
                                targetType = inputType;
                            } else {
                                final Class<?> classByNodeName;

                                if (inputType.isCollection() || inputType.isArray() || inputType.isMap()) {
                                    // Must match the StAX/DOM siblings: without a configured element type,
                                    // fall back to node-name discovery instead of leaving targetType null.
                                    classByNodeName = getClassByNodeName(beanOrPropName,
                                            config.getElementType() != null ? config.getElementType().javaType() : inputType.javaType());
                                } else {
                                    classByNodeName = getClassByNodeName(beanOrPropName, inputType.javaType());
                                }

                                // Check before Type.of: a failed discovery must raise the descriptive
                                // ParsingException, not Type.of(null)'s IllegalArgumentException.
                                checkBeanType(classByNodeName, nodeName, inputType.javaType());

                                targetType = Type.of(classByNodeName);
                            }
                        }

                        targetClass = targetType.javaType();
                    }

                    final boolean unknownWrapper = !isFirstCall && propType != null && propType.isBean()
                            && !(hasPropTypes && config.getValueType(beanOrPropName) != null) && !isKnownBeanWrapper(beanOrPropName, propType, targetClass);

                    beanClass = targetClass;
                    beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());

                    bean = beanInfo.createBeanResult();
                    nodeValueQueue.add(bean);

                    if (unknownWrapper) {
                        // Same guard as the StAX reader: remembered until the element ends, when a text-only body
                        // (no property child cleared it) is the unwrapped-bean shape and is rejected.
                        unknownWrapperBean = bean;
                        unknownWrapperType = propType;
                        unknownWrapperName = beanOrPropName;
                    }

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
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XmlConstants.TYPE)));
                        checkedTypeInfo = true;
                    }

                    if (targetType == null || !targetType.isMap()) {
                        if ((eleType != null) && eleType.isMap()) {
                            targetType = eleType;
                        } else {
                            targetType = linkedHashMapType;
                        }

                        targetClass = targetType.javaType();
                    }

                    if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(0).isObject()) {
                        keyType = propInfo.jsonXmlType.parameterTypes().get(0);
                    } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(0).isObject()) {
                        keyType = propType.parameterTypes().get(0);
                    } else {
                        if (config.getMapKeyType() != null && !config.getMapKeyType().isObject()) {
                            keyType = config.getMapKeyType();
                        } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                            keyType = targetType.parameterTypes().get(0);
                        } else {
                            keyType = defaultKeyType;
                        }
                    }

                    if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(1).isObject()) {
                        valueType = propInfo.jsonXmlType.parameterTypes().get(1);
                    } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                        valueType = propType.parameterTypes().get(1);
                    } else {
                        if (config.getMapValueType() != null && !config.getMapValueType().isObject()) {
                            valueType = config.getMapValueType();
                        } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                            valueType = targetType.parameterTypes().get(1);
                        } else {
                            valueType = defaultValueType;
                        }
                    }

                    keyTypeQueue.add(keyType);
                    valueTypeQueue.add(valueType);

                    map = xmlParser.newPropInstance(targetClass, attrs);
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
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XmlConstants.TYPE)));
                        checkedTypeInfo = true;
                    }

                    boolean untypedArray = false;

                    if (targetType == null || !targetType.isArray()) {
                        if ((eleType != null) && eleType.isArray()) {
                            targetType = eleType;
                        } else {
                            targetType = strArrayType;
                            untypedArray = true;
                        }

                        targetClass = targetType.javaType();
                    }

                    if (propInfo != null && propInfo.clazz.isArray() && !Object.class.equals(propInfo.clazz.getComponentType())) {
                        eleType = Type.of(propInfo.clazz.getComponentType());
                    } else {
                        if (config.getElementType() != null && !config.getElementType().isObject()) {
                            eleType = config.getElementType();
                        } else {
                            // must match the StAX/DOM siblings: forcing String for an Object[] target made the
                            // String[] placeholder drive the final conversion and dropped per-element type info.
                            eleType = targetType.isArray() ? targetType.elementType() : strType;
                        }
                    }

                    eleTypeQueue.add(eleType);

                    // The shared placeholder marks an <array> without a declared type, so endElement can infer the
                    // array type from the elements (see inferArrayType).
                    array = untypedArray && eleType == strType ? UNTYPED_ARRAY_PLACEHOLDER : N.newArray(eleType.javaType(), 0);
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
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XmlConstants.TYPE)));
                        checkedTypeInfo = true;
                    }

                    if (targetType == null || !targetType.isCollection()) {
                        if ((eleType != null) && Collection.class.isAssignableFrom(eleType.javaType())) {
                            targetType = eleType;
                        } else {
                            targetType = listType;
                        }

                        targetClass = targetType.javaType();
                    }

                    if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 1 && !propInfo.jsonXmlType.parameterTypes().get(0).isObject()) {
                        eleType = propInfo.jsonXmlType.parameterTypes().get(0);
                    } else if (propType != null && propType.parameterTypes().size() == 1 && Collection.class.isAssignableFrom(propType.javaType())
                            && !propType.parameterTypes().get(0).isObject()) {
                        eleType = propType.parameterTypes().get(0);
                    } else {
                        if (config.getElementType() != null && !config.getElementType().isObject()) {
                            eleType = config.getElementType();
                        } else if (targetType.elementType() != null && !targetType.elementType().isObject()) {
                            eleType = targetType.elementType();
                        } else {
                            eleType = defaultValueType;
                        }
                    }

                    eleTypeQueue.add(eleType);

                    coll = xmlParser.newPropInstance(targetClass, attrs);
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
                    if (bean == unknownWrapperBean) {
                        unknownWrapperBean = null; // a child element: the wrapper holds properties, not text
                    }

                    if (!isTagByPropertyName) {
                        //noinspection DataFlowIssue
                        beanOrPropName = attrs.getValue(XmlConstants.NAME);

                        if (beanOrPropName == null) {
                            throw new ParsingException("Missing '" + XmlConstants.NAME + "' attribute on XML element: " + qName);
                        }

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
                            inIgnorePropRefCount = 1;
                            break;
                        } else {
                            throw new ParsingException("Unknown property element: " + beanOrPropName + " for class: " + beanClass.getCanonicalName());
                        }
                    }

                    if (hasPropTypes) {
                        propType = config.getValueType(beanOrPropName);

                        if (propType == null) {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType : Type.of(xmlParser.getConcreteClass(attrs, propInfo.clazz));
                        }
                    } else {
                        propType = ignoreTypeInfo ? propInfo.jsonXmlType : Type.of(xmlParser.getConcreteClass(attrs, propInfo.clazz));
                    }

                    if ((propType == null) || propType.javaType() == Object.class) {
                        propType = defaultValueType;
                    }

                    break;
                }

                case ELEMENT: {
                    if (eleType == null || coll == null) {
                        throw new ParsingException("Element <" + nodeName + "> is only allowed inside an array or collection element");
                    }

                    propType = xmlParser.resolveItemType(attrs, eleType);

                    if ((propType == null) || propType.javaType() == Object.class) {
                        propType = defaultValueType;
                    }

                    break;
                }

                case KEY: {
                    if (keyType == null || entryKeyMarkQueue.isEmpty()) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY);
                    }

                    propType = ignoreTypeInfo ? keyType : Type.of(xmlParser.getConcreteClass(attrs, keyType.javaType()));

                    if ((propType == null) || propType.javaType() == Object.class) {
                        propType = defaultKeyType;
                    }

                    break;
                }

                case VALUE: {
                    checkEntryHasKey();

                    if (hasPropTypes) {
                        final Object key = keyQueue.get(keyQueue.size() - 1);
                        if (key != null && key.getClass() == String.class) {
                            propType = config.getValueType((String) key);

                            if (propType == null) {
                                propType = ignoreTypeInfo ? valueType : Type.of(xmlParser.getConcreteClass(attrs, valueType.javaType()));
                            }
                        } else {
                            propType = ignoreTypeInfo ? valueType : Type.of(xmlParser.getConcreteClass(attrs, valueType.javaType()));
                        }
                    } else {
                        propType = ignoreTypeInfo ? valueType : Type.of(xmlParser.getConcreteClass(attrs, valueType.javaType()));
                    }

                    if ((propType == null) || propType.javaType() == Object.class) {
                        propType = defaultValueType;
                    }

                    break;
                }

                case ENTRY: {
                    if (map == null) {
                        throw new ParsingException("Element <" + nodeName + "> is only allowed inside a map element");
                    }

                    entryKeyMarkQueue.add(keyQueue.size());

                    break;
                }

                default:
                    throw new ParsingException("only array, collection, map and bean nodes are supported: " + nodeName); //NOSONAR
            }

            if (isFirstCall) {
                throw new ParsingException("only array, collection, map and bean nodes are supported: " + nodeName);
            }

            // Ignored properties remain opaque; every consumed element follows this parser's approval policy.
            if (inIgnorePropRefCount == 0) {
                xmlParser.resolvePresentTypeAttribute(attrs == null ? null : attrs.getValue(XmlConstants.TYPE));
            }
            nodeTypeQueue.add(nodeType);
            valueChildSeenQueue.add(false);
        }

        /**
         * @throws ParsingException if array content cannot be parsed, a map entry has missing or duplicate components, or the closing node has an unsupported type
         */
        @SuppressWarnings("unchecked")
        @Override
        public void endElement(final String namespaceURI, final String localName, final String qName) throws ParsingException {
            if (inIgnorePropRefCount > 1) {
                inIgnorePropRefCount--;

                return;
            }

            if (rootIsNull) {
                // The end of the null root: nothing was pushed for it, the result stays null.
                inIgnorePropRefCount = 0;

                return;
            }

            nodeName = Strings.isEmpty(localName) ? qName : localName;
            beanOrPropName = nodeName;

            final NodeType nodeType = nodeTypeQueue.remove(nodeTypeQueue.size() - 1);
            valueChildSeenQueue.removeAt(nodeTypeQueue.size());

            switch (nodeType) {
                case ENTITY: {

                    if (!isTagByPropertyName) {
                        beanOrPropName = beanOrPropNameQueue.remove(beanOrPropNameQueue.size() - 1);
                    }

                    if (bean == unknownWrapperBean) {
                        unknownWrapperBean = null;

                        if (Strings.isNotBlank(sb)) {
                            throw unexpectedBeanWrapper(unknownWrapperName, unknownWrapperType, null);
                        }
                    }

                    popupNodeValue();

                    break;
                }

                case ARRAY: {

                    if (!coll.isEmpty()) {
                        final Object placeholder = nodeValueQueue.get(nodeValueQueue.size() - 2);
                        array = collectionToArray(coll, placeholder == UNTYPED_ARRAY_PLACEHOLDER ? inferArrayType(coll) : Type.of(placeholder.getClass()));
                    } else if (Strings.isNotBlank(sb)) {
                        array = targetType.valueOf(sb.toString());

                        if (array == null) {
                            throw new ParsingException("Cannot parse array content: " + sb);
                        }
                    } else {
                        // Whitespace-only content (the pretty-printed empty array): keep the empty placeholder
                        // pushed in startElement rather than trusting the `array` field's leftover value.
                        array = nodeValueQueue.get(nodeValueQueue.size() - 2);

                        if (array == UNTYPED_ARRAY_PLACEHOLDER) {
                            array = new String[0];
                        }
                    }

                    if (nodeTypeQueue.isEmpty()) {
                        resultHolder.setValue((T) array);
                    }

                    nodeValueQueue.remove(nodeValueQueue.size() - 1);

                    // The slot at the new top of nodeValueQueue still holds the empty placeholder array
                    // created in startElement. Replace it with the freshly populated `array` so that
                    // popupNodeValue() assigns the actual elements to the enclosing bean/collection/map.
                    // Without this, a nested array would be set as an empty array on its parent.
                    nodeValueQueue.set(nodeValueQueue.size() - 1, array);

                    array = null;

                    popupNodeValue();

                    break;
                }

                case COLLECTION: {
                    if (coll.isEmpty() && Strings.isNotBlank(sb)) {
                        // JSON text form <list>[1, 2, 3]</list>, as the StAX reader accepts it. targetType is still this
                        // collection's own type here: a child element would have made coll non-empty.
                        final Object parsed = parseJsonCollectionText(sb.toString(), eleTypeQueue.get(eleTypeQueue.size() - 1), targetType);

                        if (parsed != null) {
                            coll.addAll((Collection<Object>) parsed);
                        }
                    }

                    popupNodeValue();

                    break;
                }

                case MAP: {

                    popupNodeValue();

                    break;
                }

                case PROPERTY: {
                    if (inIgnorePropRefCount == 1) {
                        inIgnorePropRefCount--;

                        if (!isTagByPropertyName) {
                            beanOrPropNameQueue.remove(beanOrPropNameQueue.size() - 1);
                        }

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
                    if (propInfo != null && propInfo.jsonXmlExpose != JsonXmlField.Direction.SERIALIZE_ONLY) {
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
                    if (keyQueue.size() != entryKeyMarkQueue.get(entryKeyMarkQueue.size() - 1)) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY); // a second key in the same entry
                    }

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
                        if (latestKey != null && mapIgnoredPropNames.contains(latestKey.toString())) {
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

                    checkEntryHasKey();

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
                    // If the key of this entry was marked to be ignored (inIgnorePropRefCount set to 1 at the end of
                    // the <key> element), the matching <value> element was skipped wholesale by the inIgnorePropRefCount
                    // increment/decrement in startElement/endElement, so the VALUE case never got a chance to reset the
                    // flag or to drain the key that was pushed onto keyQueue. Reset both here at the entry boundary;
                    // otherwise the flag stays set and every following entry is silently swallowed.
                    if (inIgnorePropRefCount == 1) {
                        inIgnorePropRefCount--;
                        keyQueue.remove(keyQueue.size() - 1);
                    }

                    if (keyQueue.size() != entryKeyMarkQueue.remove(entryKeyMarkQueue.size() - 1)) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY); // a key without a value
                    }

                    break;

                default:
                    throw new ParsingException("only array, collection, map and bean nodes are supported: " + nodeName);
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

        /**
         * Verifies that the current {@code <value>} follows exactly one {@code <key>} of the same entry.
         *
         * @throws ParsingException if the value has no key, or is not inside an entry at all
         */
        private void checkEntryHasKey() throws ParsingException {
            if (map == null || entryKeyMarkQueue.isEmpty() || keyQueue.size() != entryKeyMarkQueue.get(entryKeyMarkQueue.size() - 1) + 1) {
                throw new ParsingException(MALFORMED_MAP_ENTRY);
            }
        }

        /**
         * Pops the just-closed node off the value stack and makes the enclosing node current again.
         *
         * <p>A popped bean is finished first (which, for records, builders and other immutable beans,
         * produces a new object), and the finished value is what the enclosing bean, collection or map
         * receives. The element/key/value type stacks are unwound in step with the value stack.</p>
         * @throws ParsingException if the closing element has no corresponding value on the node stack
         */
        @SuppressWarnings("unchecked")
        private void popupNodeValue() throws ParsingException {
            eleValue = nodeValueQueue.remove(nodeValueQueue.size() - 1);

            if (eleValue == null) {
                throw new ParsingException("Unexpected empty node value for element: " + nodeName);
            }

            beanInfo = beanInfoQueue.remove(eleValue);

            if (beanInfo != null) {
                beanClass = beanInfo.clazz;

                if (resultHolder.value() == bean) {
                    bean = beanInfo.finishBeanResult(bean);
                    resultHolder.setValue((T) bean);
                } else {
                    bean = beanInfo.finishBeanResult(bean);
                }

                // For immutable beans/records/builders finishBeanResult returns a NEW finished object,
                // so the popped intermediate held in eleValue is stale. Re-point eleValue at the finished
                // bean so the enclosing bean/collection/map consumes the finished value, not the intermediate.
                eleValue = bean;
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
                    targetClass = next.getClass();

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

        /**
         * Installs the deserialization configuration for one parse and caches the flags derived from it.
         *
         * @param config the deserialization configuration, must not be {@code null}
         */
        private void setConfig(final XmlDeserConfig config) {
            this.config = config;
            hasPropTypes = config.hasValueTypes();
            ignoreUnmatchedProperty = config.isIgnoreUnmatchedProperty();
            mapIgnoredPropNames = config.getIgnoredPropNames(Map.class);
        }

        /**
         * Clears every piece of parse state so this handler can be returned to the pool and reused.
         */
        private void reset() {
            resultHolder.setValue(null);
            Objectory.recycle(sb);
            sb = null;

            // Root parse context.
            nodeTypes = null;
            inputType = null;
            config = null;

            // Configuration-derived flags and ignored-name state.
            hasPropTypes = false;
            ignoreUnmatchedProperty = false;
            mapIgnoredPropNames = null;

            // Current bean/property metadata.
            beanInfo = null;
            propInfo = null;

            beanOrPropNameQueue.clear();
            nodeTypeQueue.clear();
            // A rejected duplicate wrapper can leave partial frames; none may survive pooled-handler reuse.
            valueChildSeenQueue.clear();
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
            targetType = null;
            targetClass = null;
            eleType = null;
            propType = null;
            keyType = null;
            valueType = null;
            eleTypeQueue.clear();
            keyTypeQueue.clear();
            valueTypeQueue.clear();
            beanInfoQueue.clear();

            // Per-node parsing flags.
            isNull = false;
            checkedPropNameTag = false;
            checkedTypeInfo = false;
            isTagByPropertyName = false;
            ignoreTypeInfo = false;
            isFirstCall = true;
            inIgnorePropRefCount = 0;
            rootIsNull = false;
            entryKeyMarkQueue.clear();
            unknownWrapperBean = null;
            unknownWrapperType = null;
            unknownWrapperName = null;
        }
    }
}
