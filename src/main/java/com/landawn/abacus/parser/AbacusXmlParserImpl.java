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
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.XmlUtil;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u;

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
 * <p>The same rule applies to root and nested maps and collections: runtime metadata selects construction while the declared
 * key, value and element types supply the parsing schema, including declared Optional/Nullable wrappers. Compatible
 * metadata may refine individual {@code Object} arguments without replacing specific declared arguments. Compatible
 * implementation hints inside type arguments survive refinement, preserving nested identity-map membership and
 * collection ordering. Metadata may describe the payload on its property/key/value/item wrapper or on the single
 * nested container element. Container
 * implementations that cannot be instantiated retain the mutable fallback for Object slots at any nesting level.</p>
 * <p>For a declared map or collection, compatible metadata arguments still refine Object slots when the runtime
 * implementation requires mutable fallback. At a root collection, a configured element type overrides the target
 * collection's generic element type, for both child-element and compact JSON-text representations. Nested
 * property and map-value declarations retain their explicit element constraints; configuration fills their
 * open {@code Object} components. A declared array component is reified, so it beats global element
 * configuration for JSON text and for child elements, at the root and on map values: a {@code Long[]} target
 * reads its elements as {@code Long} whatever {@code setElementType} says. Only an open {@code Object[]}
 * component yields to configuration, and even then the array keeps its declared {@code Object} component
 * type.</p>
 * <p>Item attributes remain effective when an ancestor omits its own type attribute.</p>
 * <p>Every consumed value's type attribute requires approval, even when ancestor metadata is absent or
 * the value is marked null. Map {@code <entry>} wrapper attributes also require approval, including entries with ignored
 * keys, but do not select the key or value type. Explicitly ignored properties and their subtrees remain opaque.</p>
 * <p>An empty enum element is passed to its type codec. Name-based enums read it as null; annotated value/creator
 * codecs may accept the empty token or reject it with an exception. An explicit {@code isNull="true"} marker
 * bypasses token conversion and remains null.</p>
 * <p>The null marker also applies to structured values and their property/key/value/item wrappers. After
 * approving that element's own type metadata, readers skip its complete subtree and retain following siblings.</p>
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
    private static final ClassValue<Map<String, WeakReference<Class<?>>>> nodeNameClassMapPool = new ClassValue<>() {
        @Override
        protected Map<String, WeakReference<Class<?>>> computeValue(final Class<?> type) {
            // Keep the value JDK-only: a map subclass would retain this loader through long-lived target classes.
            // Reads are unlocked; insertion below bounds input-driven names and weak values allow class unloading.
            return new ConcurrentHashMap<>(64);
        }
    };

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

    AbacusXmlParserImpl(final XmlParserType parserType, final XmlSerConfig xsc, final XmlDeserConfig xdc, final java.util.Set<Class<?>> allowedTypeClasses)
            throws IllegalArgumentException, NullPointerException {
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
     * ({@code Tuple}, {@code Pair}, {@code Triple}, {@code Indexed}, {@code Timed}, {@code Map.Entry}) as their JSON text, so
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
     * @throws IllegalArgumentException if {@code output} is {@code null} or a directory.
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
            if (flush) {
                bw.flush();
            }
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
                writeEmbeddedRawJson(bw, obj, config);
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
                                writeUnwrappedValue(bw, null, unwrapped, configToUse, "Property", propInfo.name);
                            }
                        } else {
                            // The property name is passed separately so the failure message is only built on failure.
                            writeXmlScalar(bw, type, obj, configToUse, "Property", propInfo.name);
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

        if (tagByPropertyName) {
            beanInfo.xmlNameTags[nameTagIdx].checkXmlName(true, "Class name");
        }

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

        writeProperties(obj, config, propIndentation, serializedObjects, type, beanInfo, bw);

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
     * @param beanInfo the metadata of the bean class, already looked up by the caller (one lookup per bean, not two)
     * @param bw the buffered XML writer
     * @throws ParsingException if a property that is written has a custom name that {@code tagByPropertyName} would put in element-name position
     *         and that is not a valid XML element name, or that holds text XML 1.0 cannot carry (which no amount of attribute escaping can rescue),
     *         or if a property value holds such text
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void writeProperties(final Object obj, final XmlSerConfig config, final String propIndentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BeanInfo beanInfo, final BufferedXmlWriter bw) throws ParsingException, IOException {
        //    if (hasCircularReference(obj, serializedObjects, bw)) {
        //        return;
        //    }

        final Class<?> cls = type.javaType();

        final Exclusion exclusion = getExclusion(config, beanInfo);

        final boolean ignoreNullProperty = (exclusion == Exclusion.NULL) || (exclusion == Exclusion.DEFAULT);
        final boolean ignoreDefaultProperty = (exclusion == Exclusion.DEFAULT);

        final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(cls);
        final boolean tagByPropertyName = config.isTagByPropertyName();
        final boolean writeTypeInfo = config.isWriteTypeInfo();
        final boolean isPrettyFormat = config.isPrettyFormat();

        // Built on the first structured (nested) property only: a bean of scalars never needs it.
        String nextIndentation = null;
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
            propInfo.xmlNameTags[nameTagIdx].checkXmlName(tagByPropertyName, "Property name");

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
                    writeEmbeddedRawJson(bw, propValue, config);
                } else if (unwrapped) {
                    writeUnwrappedValue(bw, propInfo.jsonXmlType.elementType(), propValue, config, "Property", propName);
                } else if (propInfo.jsonXmlType.isSerializable()) {
                    if (propInfo.jsonXmlType.isObjectArray() || propInfo.jsonXmlType.isCollection()) {
                        // jsonParser.serialize(bw, propValue);

                        writeEmbeddedJson(bw, propValue, config);
                    } else if (isTupleLike(propInfo.jsonXmlType)) {
                        writeUnwrappedValue(bw, propInfo.jsonXmlType, propValue, config, "Property", propName);
                    } else {
                        if (propInfo.hasFormat) {
                            propInfo.writePropValue(bw, propValue, config);
                        } else {
                            // The property name is passed separately so the failure message is only built on failure.
                            writeXmlScalar(bw, propInfo.jsonXmlType, propValue, config, "Property", propName);
                        }
                    }
                } else {
                    // A repeated object (circularReferenceSupported) is still held by an enclosing level, and write()
                    // then emits nothing at all: tested before write(), which removes a completed value from the set.
                    final boolean isRepeatedObject = isRepeatedObject(propValue, serializedObjects);

                    if (isPrettyFormat && nextIndentation == null) {
                        nextIndentation = (propIndentation == null ? Strings.EMPTY : propIndentation) + config.getIndentation();
                    }

                    write(propValue, propInfo, config, nextIndentation, serializedObjects, bw, false);

                    // A declared type that is not serializable (an Object property) may hold a scalar at runtime, which
                    // write() emits as bare element text: a trailing line break here would become part of that value
                    // (<any>abc\n    </any> read back as "abc\n    "). Only a structured value gets the closing indentation,
                    // and not a repeated object, whose element must stay empty (<self>\n    </self> read back as "\n    ").
                    if (isPrettyFormat && !isRepeatedObject && !isBareScalar(propValue)) {
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

    // Whether write(value, ...) writes nothing because value is a repeated object (see hasCircularReference): its element
    // must then stay empty, without the closing line break and indentation a pretty-printed nested value gets.
    private static boolean isRepeatedObject(final Object value, final IdentityHashSet<Object> serializedObjects) {
        return serializedObjects != null && serializedObjects.contains(value);
    }

    // Whether write(value, ...) writes the value as element text rather than as a nested element.
    private static boolean isBareScalar(final Object value) {
        final Type<Object> type = Type.of(value.getClass());

        return type.serializationType() == SerializationType.SERIALIZABLE && !type.isObjectArray() && !type.isCollection();
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
     * @throws ParsingException if a key or value contains a code unit that cannot be represented in XML 1.0, or a nested value cannot be serialized
     *         (see {@link #write(Object, PropInfo, XmlSerConfig, String, IdentityHashSet, BufferedXmlWriter, boolean)})
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void writeMap(final Map<?, ?> m, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
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

                            writeEmbeddedJson(bw, key, config);
                        } else if (isTupleLike(keyType)) {
                            writeUnwrappedValue(bw, keyType, key, config, "Map key");
                        } else {
                            writeXmlScalar(bw, keyType, key, config, "Map key");
                        }
                    } else {
                        final boolean isRepeatedKey = isRepeatedObject(key, serializedObjects);

                        write(key, null, config, nextIndentation, serializedObjects, bw, false);

                        if (isPrettyFormat && !isRepeatedKey) {
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

                        writeEmbeddedJson(bw, value, config);
                    } else if (isTupleLike(valueType)) {
                        writeUnwrappedValue(bw, valueType, value, config, "Map value");
                    } else {
                        writeXmlScalar(bw, valueType, value, config, "Map value");
                    }
                } else {
                    final boolean isRepeatedValue = isRepeatedObject(value, serializedObjects);

                    write(value, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat && !isRepeatedValue) {
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
     * Writes an array to XML, emitting an enclosing array element and an {@code <e>} element for each
     * array element. An object array is indexed directly; a primitive array is read through
     * {@link java.lang.reflect.Array}, which boxes each element.
     *
     * @param obj the array to write (may be a primitive or object array)
     * @param config the serialization configuration
     * @param indentation the current indentation string for pretty printing, or {@code null}
     * @param serializedObjects set of already serialized objects for circular reference detection, or {@code null}
     * @param type the type information for the array
     * @param bw the buffered XML writer
     * @throws ParsingException if an element contains a code unit that cannot be represented in XML 1.0, or a nested value cannot be serialized
     *         (see {@link #write(Object, PropInfo, XmlSerConfig, String, IdentityHashSet, BufferedXmlWriter, boolean)})
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void writeArray(final Object obj, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
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
        // An object array (the only array kind write() routes here) is indexed directly: the reflective
        // Array.get is a native call per element. A primitive array keeps the reflective, boxing access.
        final Object[] objectArray = obj instanceof Object[] ? (Object[]) obj : null;
        Type<Object> eleType = null;

        for (int i = 0; i < len; i++) {
            Object e = objectArray != null ? objectArray[i] : Array.get(obj, i);
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

                        writeEmbeddedJson(bw, e, config);
                    } else if (isTupleLike(eleType)) {
                        writeUnwrappedValue(bw, eleType, e, config, "Array element");
                    } else {
                        writeXmlScalar(bw, eleType, e, config, "Array element");
                    }
                } else {
                    final boolean isRepeatedElement = isRepeatedObject(e, serializedObjects);

                    write(e, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat && !isRepeatedElement) {
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
     * @throws ParsingException if an element contains a code unit that cannot be represented in XML 1.0, or a nested value cannot be serialized
     *         (see {@link #write(Object, PropInfo, XmlSerConfig, String, IdentityHashSet, BufferedXmlWriter, boolean)})
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    void writeCollection(final Collection<?> c, final XmlSerConfig config, final String indentation, final IdentityHashSet<Object> serializedObjects,
            final Type<Object> type, final BufferedXmlWriter bw) throws ParsingException, IOException {
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

                        writeEmbeddedJson(bw, e, config);
                    } else if (isTupleLike(eleType)) {
                        writeUnwrappedValue(bw, eleType, e, config, "Collection element");
                    } else {
                        writeXmlScalar(bw, eleType, e, config, "Collection element");
                    }
                } else {
                    final boolean isRepeatedElement = isRepeatedObject(e, serializedObjects);

                    write(e, null, config, nextIndentation, serializedObjects, bw, false);

                    if (isPrettyFormat && !isRepeatedElement) {
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
     * @throws ParsingException if no matching type is found in nodeTypes, the XML is malformed, or the root element
     *         cannot be read as the mapped type
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws IllegalArgumentException, UncheckedIOException, ParsingException {
        N.checkArgNotNull(source, cs.source);

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
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws ParsingException if no matching type is found in nodeTypes, the XML is malformed, or the root element
     *         cannot be read as the mapped type
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
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
     * @throws IllegalArgumentException if {@code source} is {@code null} and this parser uses {@link XmlParserType#StAX} (the
     *         {@link XmlParserType#SAX} and {@link XmlParserType#DOM} backends report a {@code null} reader as an {@code UncheckedIOException})
     * @throws ParsingException if no matching type is found in nodeTypes, the XML is malformed, or the root element
     *         cannot be read as the mapped type
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes)
            throws IllegalArgumentException, ParsingException, UncheckedIOException {
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
     * @throws ParsingException if no matching type is found in nodeTypes, the XML structure is invalid, or the root
     *         element cannot be read as the mapped type
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
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws ParsingException if the XML is malformed, no target type can be resolved for the root node, the root element
     *         cannot be read as the target type, or the configured parser type is not supported
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @SuppressWarnings("unchecked")
    <T> T read(final InputStream source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType,
            final boolean boundedSource) throws IllegalArgumentException, ParsingException, UncheckedIOException {
        final XmlDeserConfig configToUse = check(config);

        switch (parserType) {
            case SAX:

                final SAXParser saxParser = XmlUtil.createSAXParser();
                final XmlSAXHandler<T> dh = getXmlSAXHandler(configToUse, nodeTypes, targetType);
                T result = null;

                try {
                    saxParser.parse(source, dh);
                    result = checkRootValue(dh.resultHolder.value(), dh.inputType);
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
                    String nodeName = getAttribute(xmlReader, XmlConstants.NAME);

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
     * @throws IllegalArgumentException if {@code source} is {@code null} and this parser uses {@link XmlParserType#StAX} (the
     *         {@link XmlParserType#SAX} and {@link XmlParserType#DOM} backends report a {@code null} reader as an {@code UncheckedIOException})
     * @throws ParsingException if the XML is malformed, no target type can be resolved for the root node, the root element
     *         cannot be read as the target type, or the configured parser type is not supported
     * @throws UncheckedIOException if the DOM or SAX backend reports an {@code IOException} while reading XML from {@code source}
     */
    @SuppressWarnings("unchecked")
    <T> T read(final Reader source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes, Type<? extends T> targetType,
            final boolean boundedSource) throws IllegalArgumentException, ParsingException, UncheckedIOException {
        final XmlDeserConfig configToUse = check(config);

        switch (parserType) {
            case SAX:

                final SAXParser saxParser = XmlUtil.createSAXParser();
                final XmlSAXHandler<T> dh = getXmlSAXHandler(configToUse, nodeTypes, targetType);
                T result = null;

                try {
                    saxParser.parse(new InputSource(source), dh);
                    result = checkRootValue(dh.resultHolder.value(), dh.inputType);
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
                    String nodeName = xmlReader.getAttributeCount() > 0 ? getAttribute(xmlReader, XmlConstants.NAME) : null;

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
        return checkRootValue(readByStreamParser(xmlReader, config, null, null, false, false, false, true, inputType, inputType), inputType);
    }

    /**
     * Returns the value read for the root element, after checking that it is an instance of the requested type.
     *
     * <p>The kind of the root element decides the shape of the value - an {@code <array>}, {@code <list>} or {@code <map>}
     * root is read as an array, list or map - so a root of another kind than the requested type is rejected here instead of
     * being returned as a value the caller's type cannot hold ({@code deserialize(xml, Alpha.class)} with an
     * {@code <array>} root).</p>
     *
     * @param <T> the requested type
     * @param value the value read for the root element; may be {@code null}
     * @param targetType the requested type; {@code null} skips the check
     * @return {@code value}
     * @throws ParsingException if {@code value} is not an instance of the requested type
     */
    @SuppressWarnings("unchecked")
    private static <T> T checkRootValue(final Object value, final Type<?> targetType) throws ParsingException {
        final Class<?> targetClass = targetType == null ? null : targetType.javaType();

        if (value != null && targetClass != null && !(targetClass.isPrimitive() ? ClassUtil.wrap(targetClass) : targetClass).isInstance(value)) {
            throw new ParsingException("The XML root element was read as " + ClassUtil.getCanonicalClassName(value.getClass())
                    + ", which is not the requested type: " + targetType.name());
        }

        return (T) value;
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
                    inputType, null);
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
     * @param wrapperMetadata the enclosing value wrapper's already approved construction metadata, or {@code null}
     * @return the deserialized value of type {@code T}
     * @throws ParsingException if the XML does not match the target type
     * @throws XMLStreamException if the underlying stream reader fails
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    @SuppressWarnings({ "null", "unused", "deprecation", "DataFlowIssue" })
    private <T> T readByStreamParserBody(final XMLStreamReader xmlReader, final XmlDeserConfig config, Type<?> propType, PropInfo propInfo, boolean checkedAttr,
            boolean isTagByPropertyName, boolean ignoreTypeInfo, final boolean isFirstCall, Type<?> targetType, final Type<?> inputType,
            final Type<?> wrapperMetadata) throws ParsingException, XMLStreamException {

        final boolean hasPropTypes = config.hasValueTypes();
        String nodeName = null;

        if (checkedAttr) {
            nodeName = isTagByPropertyName || xmlReader.getAttributeCount() == 0 ? xmlReader.getLocalName() : getAttribute(xmlReader, XmlConstants.NAME);
        } else {
            final String nameAttr = getAttribute(xmlReader, XmlConstants.NAME);
            nodeName = Strings.isNotEmpty(nameAttr) ? nameAttr : xmlReader.getLocalName();
        }

        targetType = hasPropTypes ? config.getValueType(nodeName, targetType) : targetType;

        final Type<?> declaredTargetType = !isFirstCall && propType != null && propType.isCollection() ? propType : targetType;
        Class<?> targetClass = targetType == null ? null : targetType.javaType();

        // Approval is independent of whether ancestor metadata makes this attribute affect type selection.
        final Type<?> nodeAttributeType = resolvePresentTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE));
        if (!(checkedAttr && ignoreTypeInfo)) {
            if (wrapperMetadata != null && targetType != null) {
                targetClass = getConcreteClass(wrapperMetadata.javaType(), targetClass);
                targetType = resolveItemType(targetType, targetClass, wrapperMetadata);
            }
            targetClass = getConcreteClass(nodeAttributeType == null ? null : nodeAttributeType.javaType(), targetClass);
            if (targetType != null) {
                targetType = resolveItemType(targetType, targetClass, nodeAttributeType);
                if (targetClass == Object.class && (targetType.isMap() || targetType.isCollection() || targetType.isArray())) {
                    // An open Object slot adopted the metadata's container schema (an uninstantiable container as
                    // its open mutable schema); construction follows the adopted class, where getConcreteClass
                    // could only fall back to Object for a class the readers cannot instantiate.
                    targetClass = targetType.javaType();
                }
            }
            // Array metadata must not close an originally open array slot: configuration types the elements
            // while the slot's own declared runtime (Object[]) is constructed. The construction class follows
            // the restored type: containerReadType below would otherwise rebuild the metadata's array class.
            final Type<?> openSlotType = openArrayDestination(propType, targetType, config.getElementType());
            if (openSlotType != targetType) {
                targetType = openSlotType;
                targetClass = targetType.javaType();
            }
        }

        if (targetType == null) {
            if (targetClass == null) {
                throw new ParsingException("Unable to determine target type for xml element: " + nodeName);
            }

            targetType = Type.of(targetClass);
        } else if (targetClass == null) {
            targetClass = targetType.javaType();
        } else if (!targetType.javaType().equals(targetClass)) {
            targetType = containerReadType(targetType, targetClass);
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

        if ((nodeType == NodeType.ENTITY || nodeType == NodeType.MAP || nodeType == NodeType.ARRAY || nodeType == NodeType.COLLECTION)
                && Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL))) {
            skipNullElement(xmlReader);
            return null;
        }

        String propName = null;
        Object propValue = null;
        Type<?> propAttributeType = null;
        // Whether a value type is configured for the current property: configuration, not metadata, then types its text.
        boolean propConfigured = false;
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
                            } else if (isOpenBeanSlot(propType, isFirstCall, inputType)) {
                                // No class for the element: an open slot reads the bean-shaped element as a map of its
                                // properties, as the standard readers do, instead of failing on the writer's own untyped output.
                                return (T) readBeanShapedElementAsMap(xmlReader, config, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType);
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

                final boolean ignoreUnmatchedProperty = config.isIgnoreUnmatchedProperty();
                final Collection<String> ignoredClassPropNames = config.getIgnoredPropNames(targetClass);
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
                final Object result = beanInfo.createBeanResult();

                // The property schema before metadata refinement, remembered per property: an originally open
                // Object slot stays open to global element configuration (metadata must not close it).
                Type<?> declaredSlotType = null;

                for (int event = xmlReader.next(); xmlReader
                        .hasNext(); event = advanceEvent ? xmlReader.next() : xmlReader.getEventType(), advanceEvent = true) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            // N.println(xmlReader.getLocalName());

                            if (propName == null) {
                                final boolean isNullValue = Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL));

                                propName = isTagByPropertyName ? xmlReader.getLocalName() : getAttribute(xmlReader, XmlConstants.NAME);

                                if (propName == null) {
                                    throw new ParsingException("Missing '" + XmlConstants.NAME + "' attribute on XML element: " + xmlReader.getLocalName());
                                }

                                propInfo = beanInfo.getPropInfo(propName);

                                // Test the resolved property name too: getPropInfo also matches case and snake-case variants
                                // (<firstname>, <first_name>), which would otherwise bind an ignored property.
                                if (propName != null && ignoredClassPropNames != null
                                        && (ignoredClassPropNames.contains(propName) || (propInfo != null && ignoredClassPropNames.contains(propInfo.name)))) {
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
                                propAttributeType = attributeType;
                                propType = hasPropTypes ? config.getValueType(propName) : null;
                                propConfigured = propType != null;
                                declaredSlotType = propType != null ? propType : propInfo.jsonXmlType;

                                if (propType == null) {
                                    // A serializable property ignores its type attribute, except an array or collection of an
                                    // abstract scalar (Number[]), whose metadata names the component the elements are read with.
                                    propType = ignoresTypeAttribute(propInfo.jsonXmlType) || attributeType == null ? propInfo.jsonXmlType
                                            : resolveItemType(propInfo.jsonXmlType, getConcreteClass(attributeType.javaType(), propInfo.clazz), attributeType);
                                }

                                // An originally open Object property keeps global element configuration ahead of
                                // the component the metadata supplied (a declared component still wins), the global map
                                // types fill a declared map slot's open arguments ahead of the metadata's, and array
                                // metadata must not close an originally open array slot either: configuration types the
                                // elements while the property's declared runtime (Object[]) is constructed. refineSlot
                                // decides all three together. A scalar, bean or Object property is skipped: refineSlot hands
                                // such a schema back unchanged (isRefinableSlot).
                                if (isRefinableSlot(propType)) {
                                    propType = refineSlot(declaredSlotType, propType, config).destination();
                                }

                                if (isNullValue) {
                                    // A property's marker applies to the whole wrapper, including a structured child. Skipping it
                                    // here is why the text and end-element paths below never see a null-marked known property;
                                    // structured values carrying their own marker return before the node-type switch.
                                    skipNullElement(xmlReader);
                                    if (propInfo.jsonXmlExpose != JsonXmlField.Direction.SERIALIZE_ONLY) {
                                        propInfo.setPropValue(result, null);
                                    }
                                    propName = null;
                                    propValue = null;
                                    propInfo = null;
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
                                    // The refined payload must not close an originally open slot: bake global
                                    // element configuration in here, where the declaration is still remembered,
                                    // so the nested reader sees the effective component.
                                    final Type<?> wrappedType = refineObjectWrapper(propType, propAttributeType);
                                    final Type<?> valueType = isRefinableSlot(wrappedType) ? refineSlot(declaredSlotType, wrappedType, config).refined()
                                            : wrappedType;
                                    // Array metadata must not close an originally open array payload either: the
                                    // nested reader keeps the declared runtime (Object[]) with configured elements.
                                    final Type<?> nestedType = openArrayDestination(declaredSlotType, optionalElementTypeOrSelf(valueType),
                                            config.getElementType());
                                    enterXmlNesting();
                                    try {
                                        propValue = readByStreamParserBody(xmlReader, config, nestedType, propInfo, checkedAttr, isTagByPropertyName,
                                                ignoreTypeInfo, false, nestedType, inputType, propAttributeType);
                                    } finally {
                                        exitXmlNesting();
                                    }
                                    if (nestedType != valueType) {
                                        propValue = wrapOptional(valueType, propValue);
                                    }

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
                            // Only read for an unknown property (propInfo == null): whether ALL of its text fragments are blank.
                            boolean isUnknownPropTextBlank = true;

                            if (propInfo == null) {
                                // An unknown property's text is dropped, so its fragments are not joined: a large value
                                // used to grow sb, whose capacity was then kept for the rest of the bean. The only thing
                                // the text decides is the blank-before-START_ELEMENT check below.
                                if (text != null) {
                                    isUnknownPropTextBlank = text.isBlank();

                                    while (isTextEvent(event = xmlReader.next())) {
                                        if (isUnknownPropTextBlank) {
                                            isUnknownPropTextBlank = xmlReader.getText().isBlank();
                                        }
                                    }
                                }
                            } else if (text != null && isTextEvent(event = xmlReader.next())) {
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

                                // Always drain the buffer: when the later fragments are empty (an empty CDATA section) the
                                // joined text is no longer than the first fragment, and a buffer left holding it was
                                // prepended to the next multi-fragment text read in this element.
                                text = sb.toString();
                                sb.setLength(0);
                            }

                            // Indentation before a nested value is structural. Replay its START_ELEMENT;
                            // whitespace ending at the property end is scalar content and must be kept.
                            if (event == XMLStreamConstants.START_ELEMENT && (propInfo == null ? isUnknownPropTextBlank : text.isBlank())) {
                                advanceEvent = false;
                                break;
                            }

                            propValue = propInfo == null ? null
                                    : readStreamPropertyText(propInfo, propType, propAttributeType, propConfigured, ignoreTypeInfo, text, config);

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
                                    // An element without content is read as empty text, with the same type selection as text: the
                                    // DOM and SAX readers read it so (propType.valueOf ignored the property's format, read a primitive
                                    // or ignored type attribute on an Object property, and skipped the Optional payload refinement).
                                    propInfo.setPropValue(result,
                                            propValue != null ? propValue
                                                    : readStreamPropertyText(propInfo, propType, propAttributeType, propConfigured, ignoreTypeInfo,
                                                            Strings.EMPTY, config));
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
                    if (config.getMapKeyType() != null && !config.getMapKeyType().isObject() && !keepsRequestedContainer(declaredTargetType, 0)) {
                        keyType = config.getMapKeyType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                        keyType = targetType.parameterTypes().get(0);
                    }
                }

                Type<?> valueType = defaultValueType;

                if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(1).isObject()) {
                    // A value declared as an abstract scalar (Map<String, Number>) adopts the value the map's metadata named
                    // (HashMap<String, Long>), as compact text reads it; any other declared value is kept (narrowedArgument).
                    valueType = narrowedArgument(propInfo.jsonXmlType.parameterTypes().get(1),
                            targetType.isMap() && targetType.parameterTypes().size() == 2 ? targetType.parameterTypes().get(1) : null);
                } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    valueType = propType.parameterTypes().get(1);
                } else {
                    if (config.getMapValueType() != null && !config.getMapValueType().isObject() && !keepsRequestedContainer(declaredTargetType, 1)) {
                        valueType = config.getMapValueType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                        valueType = targetType.parameterTypes().get(1);
                    }
                }

                // Immutable* targets are filled through a mutable stand-in and wrapped on return, and an EnumMap needs its
                // key enum, as in JsonParserImpl.readMap: N.newMap would hand back a plain HashMap/TreeMap for all of them.
                final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter = mapCreatorAndConverter(targetClass, valueType,
                        scalarOverrides(config));

                final Map<Object, Object> mResult = EnumMap.class.isAssignableFrom(targetClass) ? newEnumMap(targetClass, keyType, XmlDeserConfig.class)
                        : creatorAndConverter != null ? (Map<Object, Object>) creatorAndConverter._1.apply(targetClass) : newReaderMap(targetClass);
                final Class<?> resultClass = mResult.getClass();
                final boolean discardPopulatedMapText = resultClass == HashMap.class || resultClass == LinkedHashMap.class || resultClass == TreeMap.class
                        || resultClass == EnumMap.class;
                boolean hasMapEntries = false;
                Object key = null;
                Type<?> entryKeyType = null;
                Type<?> entryValueType = null;
                boolean isStringKey = false;
                StringBuilder mapText = null;

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.SPACE:
                        case XMLStreamConstants.CDATA:
                        case XMLStreamConstants.CHARACTERS: {
                            // Text directly inside the map element: the compact JSON form when it is the whole content (see
                            // END_ELEMENT); indentation and text next to entries are skipped, as the DOM and SAX readers do.
                            if (hasMapEntries || (mapText == null && xmlReader.isWhiteSpace())) {
                                break;
                            }

                            if (mapText == null) {
                                mapText = new StringBuilder();
                            }
                            mapText.append(xmlReader.getTextCharacters(), xmlReader.getTextStart(), xmlReader.getTextLength());
                            break;
                        }

                        case XMLStreamConstants.START_ELEMENT: {
                            // a map may only contain <entry> wrappers, as the SAX reader already requires
                            if (!XmlConstants.ENTRY.equals(xmlReader.getLocalName())) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

                            // Structural wrappers do not select a value type, but their attributes must obey the same
                            // approval policy as SAX, including entries whose key will subsequently be ignored.
                            resolvePresentTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE));

                            // move to a key element; text between the entry parts is skipped as the DOM reader does.
                            // The element must be named <key>: read positionally, <key>a</key><key>b</key> would be
                            // silently mis-read as the entry a -> b, which the SAX reader rejects.
                            if (nextElementEvent(xmlReader) != XMLStreamConstants.START_ELEMENT || !XmlConstants.KEY.equals(xmlReader.getLocalName())) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

                            final Type<?> keyAttributeType = resolvePresentTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE));
                            entryKeyType = keyAttributeType;

                            // Keep the declared schema separate from runtime construction, as for collection items.
                            // An attribute class that does not fit the declared key type (a primitive name fits its wrapper) is dropped,
                            // as the DOM and SAX readers drop it: taken as is, <key type="String">1</key> put a String key into a Map<Integer, V>.
                            entryKeyType = resolveItemType(keyType,
                                    getConcreteClass(keyAttributeType == null ? null : ClassUtil.wrap(keyAttributeType.javaType()), keyType.javaType()),
                                    keyAttributeType);
                            isStringKey = entryKeyType.javaType().equals(String.class);
                            key = readWrappedValue(xmlReader, config, entryKeyType, keyType, keyAttributeType, checkedAttr, isTagByPropertyName, ignoreTypeInfo,
                                    inputType);

                            // NULL_STRING, not a bare null: contains(null) throws NPE on a Set.of(..) - see writeMap.
                            if (ignoredClassPropNames != null && ignoredClassPropNames.contains(key == null ? NULL_STRING : key.toString())) {
                                // The reader is on </key>. The value is skipped without being resolved or converted, but the entry
                                // must still be well formed, as the DOM reader requires: exactly one <value> follows the key.
                                if (nextElementEvent(xmlReader) != XMLStreamConstants.START_ELEMENT || !XmlConstants.VALUE.equals(xmlReader.getLocalName())) {
                                    throw new ParsingException(MALFORMED_MAP_ENTRY);
                                }

                                for (int depth = 1; depth > 0 && xmlReader.hasNext();) {
                                    final int skippedEvent = xmlReader.next();
                                    if (skippedEvent == XMLStreamConstants.START_ELEMENT) {
                                        depth++;
                                    } else if (skippedEvent == XMLStreamConstants.END_ELEMENT) {
                                        depth--;
                                    }
                                }

                                // on </value>: the entry must end here
                                if (nextElementEvent(xmlReader) != XMLStreamConstants.END_ELEMENT) {
                                    throw new ParsingException(MALFORMED_MAP_ENTRY);
                                }

                                key = null;
                                propValue = null;
                                break;
                            }

                            // move to a value element; it must be named <value> for the same reason
                            if (nextElementEvent(xmlReader) != XMLStreamConstants.START_ELEMENT || !XmlConstants.VALUE.equals(xmlReader.getLocalName())) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

                            final Type<?> valueAttributeType = resolvePresentTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE));
                            entryValueType = valueAttributeType;

                            // a map value written with type info carries its RAW runtime type
                            // (`type="java.util.EnumMap<Object, Object>"`, `type="ImmutableList<Object>"`). Taking it as-is dropped
                            // the key enum of a declared EnumMap<K, V> value ("EnumMap requires an enum key type") and the element
                            // type of a declared ImmutableList<E> value. Keep the declared type arguments, as the DOM reader does,
                            // and drop an attribute class that does not fit the declared value type (a Long in a Map<String, Integer>).
                            entryValueType = resolveItemType(valueType,
                                    getConcreteClass(valueAttributeType == null ? null : ClassUtil.wrap(valueAttributeType.javaType()), valueType.javaType()),
                                    valueAttributeType);

                            Type<?> declaredEntryValueType = valueType;
                            // Named value configuration applies to any key by its text, as on the standard readers and in JSON.
                            if (hasPropTypes && key != null) {
                                // A scalar per-key type on a declared list value names its element type (elementOverride).
                                final Type<?> tmpType = elementOverride(targetClass, valueType, config.getValueType(N.toString(key)));
                                if (tmpType != null) {
                                    entryValueType = tmpType;
                                    declaredEntryValueType = tmpType;

                                    if (tmpType.javaType() == Object.class && valueAttributeType != null) {
                                        // An explicit Object override is an open slot of its own: container metadata is adopted
                                        // as its schema, as the DOM and SAX readers adopt it, so a nested element is read as
                                        // that container (it was read as an open slot: a bean-shaped element became a map
                                        // where the other readers reject it as no item of the container).
                                        final Type<?> adopted = resolveItemType(tmpType, getConcreteClass(valueAttributeType.javaType(), Object.class),
                                                valueAttributeType);

                                        if (adopted.isMap() || adopted.isCollection() || adopted.isArray()) {
                                            entryValueType = adopted;
                                        }
                                    }
                                }
                            }

                            propValue = readWrappedValue(xmlReader, config, entryValueType, declaredEntryValueType, valueAttributeType, checkedAttr,
                                    isTagByPropertyName, ignoreTypeInfo, inputType);

                            // end of entry: a second value element (or anything else) is malformed
                            if (nextElementEvent(xmlReader) != XMLStreamConstants.END_ELEMENT) {
                                throw new ParsingException(MALFORMED_MAP_ENTRY);
                            }

                            //noinspection StatementWithEmptyBody
                            if (key != null && ignoredClassPropNames != null && ignoredClassPropNames.contains(key.toString())) {
                                // ignore.
                            } else {
                                mResult.put(key, propValue);
                                if (discardPopulatedMapText) {
                                    // Successful insertion into these exact built-in maps prevents compact-text fallback.
                                    // Do not call custom isEmpty() implementations more often or change their insertion hooks.
                                    hasMapEntries = true;
                                    mapText = null;
                                }
                            }

                            key = null;
                            propValue = null;

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            if (mResult.isEmpty() && mapText != null) {
                                final String compactText = mapText.toString();
                                if (isCompactMapText(compactText)) {
                                    // The same resolved key/value schema applies to compact text and structured entries.
                                    return (T) readContainerText(compactText, compactMapSchema(isFirstCall, propType, keyType, valueType), targetClass, config);
                                }
                            }
                            return (T) (creatorAndConverter == null ? mResult : creatorAndConverter._2.apply(mResult));
                        }

                        default:
                            // continue;
                    }
                }

                throw new ParsingException("Unknown parser error");
            }

            case ARRAY: {
                boolean untypedArray = false;
                // An untyped <array> whose items the configured element type types gets an array of the configured class
                // (Integer[] for configured Integer items, as the SAX reader builds it): the String[] placeholder cannot hold them.
                boolean configuredUntypedArray = false;

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
                } else if (!untypedArray && targetType.elementType() != null && !targetType.elementType().isObject()) {
                    // A declared array component is reified: it beats global element configuration for JSON text
                    // and <e> children alike (unlike a root collection's erased generic component).
                    eleType = targetType.elementType();
                } else {
                    // The configured component types the open elements once, never a configured item's own elements.
                    if (elementConfigurationApplies(config, null)) {
                        eleType = config.getElementType();
                        configuredUntypedArray = untypedArray;
                        untypedArray = false;
                    } else {
                        eleType = targetType.isArray() ? targetType.elementType() : strType;
                    }
                }

                // Runtime metadata never narrows a declared array class: a declared Map[] or Number[] slot is constructed
                // as declared (openArrayDestination) while its items take the refined or configured component
                // (arrayItemType), as on the standard readers. The refined schema arrives as targetType, or is rebuilt
                // from the metadata when the head above already restored the declared array: the value wrapper's, or
                // the <array> element's own (the writer puts the type on both; a trimmed document may keep only one).
                // At the root the requested array is the declared slot (propType carries the configured element type there).
                final Type<?> declaredSlot = isFirstCall ? declaredTargetType : propType;
                final boolean declaredArray = declaredSlot != null && declaredSlot.isArray() && !untypedArray;
                final Type<?> arrayMetadata = wrapperMetadata != null ? wrapperMetadata : checkedAttr && ignoreTypeInfo ? null : nodeAttributeType;
                final Type<?> refinedArray = declaredArray && targetType == declaredSlot && arrayMetadata != null
                        ? refineDeclaredSchema(declaredSlot, arrayMetadata)
                        : targetType;
                final Type<?> itemOverride = declaredArray ? arrayItemType(declaredSlot, refinedArray, config.getElementType()) : null;
                if (itemOverride != null) {
                    eleType = itemOverride;
                }
                final Type<?> arrayDestination = declaredArray ? openArrayDestination(declaredSlot, refinedArray, config.getElementType()) : targetType;

                final List<Object> list = Objectory.createList();

                try {
                    for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                        switch (event) {
                            case XMLStreamConstants.START_ELEMENT: {
                                // An untyped array's String placeholder types its text items; a structured item is open, so
                                // a bean-shaped element without a class is read as a map there too (the array's class is
                                // inferred from the items afterwards).
                                list.add(readWrappedItem(xmlReader, itemConfig(config, declaredSlot, eleType), untypedArray ? defaultValueType : eleType,
                                        checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType));

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

                                    // Always drain the buffer (see the bean branch): an empty trailing fragment must not leave it filled.
                                    text = sb.toString();
                                    sb.setLength(0);
                                }

                                // The text is the JSON form only when it is the whole content; text next to <e>
                                // children is mixed content and is ignored, as the SAX and DOM readers do.
                                if (event == XMLStreamConstants.END_ELEMENT) {
                                    if (list.isEmpty()) {
                                        // The shared array reader: the configured component reaches the open elements nested in
                                        // the items (Object[][] with a configured Long), as on the standard readers.
                                        propValue = readArrayText(text, arrayDestination,
                                                declaredArray ? arrayComponentOverride(declaredSlot, refinedArray, config.getElementType())
                                                        : textComponent(eleType, arrayDestination),
                                                config);
                                    }

                                    if (propValue != null) {
                                        return (T) propValue;
                                    } else {
                                        return collectionToArray(list, configuredUntypedArray ? configuredArrayType(list, eleType)
                                                : untypedArray ? inferArrayType(list) : arrayDestination);
                                    }
                                } else if (event == XMLStreamConstants.START_ELEMENT) {
                                    // The for-loop update would step past this element; read it here.
                                    // An untyped array's String placeholder only describes text items. Keep structured
                                    // items open here too, so ignored mixed text cannot change the following item's type.
                                    list.add(readWrappedItem(xmlReader, itemConfig(config, declaredSlot, eleType), untypedArray ? defaultValueType : eleType,
                                            checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType));
                                }

                                break;
                            }

                            case XMLStreamConstants.END_ELEMENT: {
                                return collectionToArray(list,
                                        configuredUntypedArray ? configuredArrayType(list, eleType) : untypedArray ? inferArrayType(list) : arrayDestination);
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
                    // The container's metadata may name a specialized item (a LinkedMultiValueMap in a declared
                    // List<Map<String, Object>>): child elements construct it as compact text does.
                    eleType = refinedItemType(propType.parameterTypes().get(0), targetType, config.getElementType());
                } else {
                    // The configured component types the open elements once (never a configured item's own elements), and a
                    // requested container element (List<Object[]> at the root) keeps its shape with the configuration inside it.
                    if (elementConfigurationApplies(config, declaredTargetType)) {
                        eleType = config.getElementType();
                    } else if (targetType.elementType() != null && !targetType.elementType().isObject()) {
                        eleType = targetType.elementType();
                    }
                }

                // Immutable* targets are filled through a mutable stand-in and wrapped on return, as in
                // JsonParserImpl.readCollection: N.newCollection would hand back a plain ArrayList/HashSet/TreeSet.
                final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter = collectionCreatorAndConverter(targetClass, true);

                final Collection<Object> result = creatorAndConverter != null ? (Collection<Object>) creatorAndConverter._1.apply(targetClass)
                        : newReaderCollection(targetClass);

                for (int event = xmlReader.next(); xmlReader.hasNext(); event = xmlReader.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            result.add(readWrappedItem(xmlReader, itemConfig(config, isFirstCall ? declaredTargetType : propType, eleType), eleType,
                                    checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType));

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

                                // Always drain the buffer: when the later fragments are empty (an empty CDATA section) the
                                // joined text is no longer than the first fragment, and a buffer left holding it was
                                // prepended to the next multi-fragment text read in this element.
                                text = sb.toString();
                                sb.setLength(0);
                            }

                            // The text is the JSON form only when it is the whole content; text next to <e>
                            // children is mixed content and is ignored, as the SAX and DOM readers do.
                            if (event == XMLStreamConstants.END_ELEMENT) {
                                if (result.isEmpty()) {
                                    // Root element configuration also overrides generic arguments for the compact JSON form.
                                    propValue = parseJsonCollectionText(text, eleType, targetType, targetClass, isFirstCall ? null : declaredTargetType,
                                            config);
                                }

                                if (propValue != null) {
                                    return (T) propValue;
                                } else {
                                    return (T) (creatorAndConverter == null ? result : creatorAndConverter._2.apply(result));
                                }
                            } else if (event == XMLStreamConstants.START_ELEMENT) {
                                // The for-loop update would step past this element; read it here.
                                result.add(readWrappedItem(xmlReader, itemConfig(config, isFirstCall ? declaredTargetType : propType, eleType), eleType,
                                        checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType));
                            }

                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            return (T) (creatorAndConverter == null ? result : creatorAndConverter._2.apply(result));
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
     * @param config the effective deserialization configuration, whose per-key value types reach the JSON maps inside
     * @return the parsed value, or {@code null} for blank text
     * @throws ParsingException if {@code text} is not valid JSON for {@code targetType}
     */
    private static Object parseJsonCollectionText(final String text, final Type<?> eleType, final Type<?> targetType, final XmlDeserConfig config)
            throws ParsingException {
        if (Strings.isBlank(text)) {
            return null;
        }

        if (eleType.isObject()) {
            // Open elements: the compact JSON reader, so the per-key value types reach the maps inside.
            return readCompactJson(Strings.strip(text), config, targetType);
        }

        if ((eleType.isString() && targetType.elementType() != null && targetType.elementType().isString())
                || (eleType.javaType().isPrimitive() && targetType.isArray() && eleType.equals(targetType.elementType()))) {
            // A declared primitive array component keeps its Type.valueOf text codec (see readArrayText):
            // char[] reads '1' as a character, boolean[] reads 1 as true.
            return targetType.valueOf(text);
        }

        if (targetType.isArray()) {
            // The shared array reader: a refined reference component is read as a list of that component and stored in
            // the declared array, a configured component reaches the open elements nested in the items (see readArrayText).
            return readArrayText(text, targetType, textComponent(eleType, targetType), config);
        }

        // String configuration over an open Object component must stringify bare JSON tokens ("1", "true"), as
        // readArrayText does; valueOf would keep JSON-native Integer/Boolean values. The shared reader types the open
        // components nested in the element (List<Object[]> with a configured Long) as it does for child elements.
        return readCollectionText(text, targetType, targetType.javaType(), eleType, config);
    }

    /**
     * Returns the component an array's JSON text converts its elements with when the reader resolved an element type
     * other than the destination's own component, otherwise {@code null} (see {@code readArrayText}).
     *
     * @param eleType the element type the reader resolved, possibly {@code null}
     * @param arrayType the array destination
     * @return the component override, or {@code null}
     */
    private static Type<?> textComponent(final Type<?> eleType, final Type<?> arrayType) {
        return eleType != null && arrayType != null && arrayType.isArray() && !eleType.equals(arrayType.elementType()) ? eleType : null;
    }

    /**
     * Parses collection text directly into the resolved implementation without replacing the effective element schema.
     *
     * @param text the element text
     * @param eleType the effective element type, including declared/configured generic arguments
     * @param targetType the declared parsing schema
     * @param targetClass the validated implementation class
     * @param declaredType the collection schema before metadata refinement, possibly {@code null}
     * @return the parsed collection, or {@code null} for blank text
     */
    private static Object parseJsonCollectionText(final String text, final Type<?> eleType, final Type<?> targetType, final Class<?> targetClass,
            final Type<?> declaredType, final XmlDeserConfig config) {
        final Type<?> textElementType = collectionTextElementType(declaredType, targetType, eleType);
        if ((targetClass == targetType.javaType() && textElementType.equals(targetType.elementType())) || Strings.isBlank(text)) {
            return parseJsonCollectionText(text, textElementType, targetType, config);
        }
        // Root configuration can override the target component; nested slots keep explicit constraints. The shared reader
        // types the open components nested in the element (List<Object[]> with a configured Long).
        return readCollectionText(text, targetType, targetClass, textElementType, config);
    }

    // Core JDK classes cannot have registered factories, and the fixed converter registry owns its
    // immutable stand-ins. Custom containers retain their original construction and bulk-insertion hooks.
    private static boolean canReuseCompactContainer(final Class<?> targetClass) {
        return targetClass.getModule() == Object.class.getModule() || mapOfCreatorAndConverterForTargetType.containsKey(targetClass);
    }

    /**
     * Creates the destination of a DOM map element: an {@code EnumMap} keyed by its enum, the mutable stand-in of
     * an {@code Immutable*} target, or else the requested class. The single construction rule for both the eager
     * and the deferred construction in the DOM MAP case.
     *
     * @param creatorAndConverter the stand-in creator and converter for the target class, or {@code null}
     * @param targetClass the requested map class
     * @param keyType the resolved key type, which names the enum of an {@code EnumMap}
     * @param node the map element, whose attributes may name the implementation
     * @return a new, empty map
     * @throws ParsingException if the map class cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> newDomMap(final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter, final Class<?> targetClass,
            final Type<?> keyType, final Node node) throws ParsingException {
        return EnumMap.class.isAssignableFrom(targetClass) ? newEnumMap(targetClass, keyType, XmlDeserConfig.class)
                : creatorAndConverter != null ? (Map<Object, Object>) creatorAndConverter._1.apply(targetClass) : newPropInstance(targetClass, node);
    }

    /**
     * Creates the destination of a DOM collection element: the mutable stand-in of an {@code Immutable*} target,
     * or else the requested class. The single construction rule for the eager and the two deferred constructions
     * in the DOM COLLECTION case.
     *
     * @param creatorAndConverter the stand-in creator and converter for the target class, or {@code null}
     * @param targetClass the requested collection class
     * @param node the collection element, whose attributes may name the implementation
     * @return a new, empty collection
     * @throws ParsingException if the collection class cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> newDomCollection(final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter,
            final Class<?> targetClass, final Node node) throws ParsingException {
        return creatorAndConverter != null ? (Collection<Object>) creatorAndConverter._1.apply(targetClass) : newPropInstance(targetClass, node);
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

    /**
     * Chooses the array type for an {@code <array>} element without a declared array type whose items the configured
     * element type typed: an array of the configured class, as the SAX reader builds it. Items that are all Strings or
     * {@code null} keep the {@code String[]} placeholder (the readers' result for them before the configured class was
     * used), and a configured {@code String} infers the class from the items, both as for an unconfigured untyped array.
     *
     * @param c the collected elements
     * @param configuredType the configured element type that typed the elements
     * @return the array type to convert {@code c} to
     */
    private static Type<?> configuredArrayType(final Collection<?> c, final Type<?> configuredType) {
        final Type<?> inferred = inferArrayType(c);

        return inferred == strArrayType || configuredType == strType ? inferred : Type.of(N.newArray(configuredType.javaType(), 0).getClass());
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
     * Reads a collection/array item after approving its metadata once. The same resolved type supplies both
     * item-type selection and Optional/Nullable refinement; neither step reparses the attribute text.
     *
     * @param xmlReader the reader positioned on the item wrapper
     * @param config the effective deserialization configuration
     * @param declaredType the declared item schema
     * @param checkedAttr whether enclosing attribute handling has been determined
     * @param isTagByPropertyName whether tags represent property names
     * @param ignoreTypeInfo whether ordinary enclosing type metadata is ignored
     * @param inputType the enclosing input type
     * @return the decoded item
     * @throws XMLStreamException if advancing the XML reader fails
     * @throws ParsingException if the element carries a nonblank {@code type} attribute that is not allowed
     */
    private Object readWrappedItem(final XMLStreamReader xmlReader, final XmlDeserConfig config, final Type<?> declaredType, final boolean checkedAttr,
            final boolean isTagByPropertyName, final boolean ignoreTypeInfo, final Type<?> inputType) throws XMLStreamException, ParsingException {
        // Approval is required even for null items and incompatible metadata, before any declared-type fallback.
        final Type<?> attributeType = resolvePresentTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE));
        final Class<?> concreteClass = getConcreteClass(attributeType == null ? null : attributeType.javaType(), declaredType.javaType());
        final Type<?> itemType = resolveItemType(declaredType, concreteClass, attributeType);
        return readWrappedValue(xmlReader, config, itemType, declaredType, attributeType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType);
    }

    /**
     * Selects an item type from a DOM node's already approved metadata: the item, key and value readers read and
     * approve a node's {@code type} attribute once and pass the result here and to the text / nested value readers
     * (each of which used to resolve the same attribute again).
     *
     * @param declaredType the declared item schema
     * @param attributeType the node's approved metadata, or {@code null}
     * @return the effective item type
     */
    private static Type<?> resolveItemType(final Type<?> declaredType, final Type<?> attributeType) {
        return resolveItemType(declaredType, getConcreteClass(attributeType == null ? null : attributeType.javaType(), declaredType.javaType()), attributeType);
    }

    /**
     * Selects an item type from already approved metadata while preserving declared generic constraints.
     * A declared container can use compatible metadata arguments even when the resolved implementation is a mutable fallback.
     *
     * @param declaredType the declared item schema
     * @param concreteClass the compatible, usable implementation class
     * @param attributeType the approved item metadata, or {@code null}
     * @return the effective item type
     */
    private static Type<?> resolveItemType(final Type<?> declaredType, final Class<?> concreteClass, final Type<?> attributeType) {
        if (attributeType != null && (declaredType.isMap() || declaredType.isCollection() || declaredType.isArray() || declaredType.isOptionalOrNullable()
                || declaredType.isObject())) {
            // Construction may reject an immutable class while its compatible key/value/element schema remains useful.
            // An open Object declaration adopts the metadata, an uninstantiable container as an open mutable schema.
            return refineDeclaredSchema(declaredType, attributeType);
        }
        final Type<?> result = attributeType != null && (attributeType.javaType() == concreteClass
                || attributeType.javaType().isPrimitive() && ClassUtil.wrap(attributeType.javaType()) == concreteClass) ? attributeType : declaredType;
        return retainDeclaredParameters(declaredType, result);
    }

    /**
     * Keeps the declared container schema when metadata selects a compatible concrete class.
     *
     * <p>{@code Type.of(Class)} is raw, so building the type from the concrete class alone
     * dropped the key enum of a declared {@code EnumMap<K, V>} ("EnumMap requires an enum key type") and the element
     * type of a declared {@code ImmutableList<E>} at the SAX property/key/value sites; the DOM reader applies this
     * rule already. The construction class is carried separately to the nested reader.</p>
     *
     * @param declaredType the declared property, key or value type
     * @param concreteClass the class resolved through {@code getConcreteClass}, i.e. the declared class when the
     *        attribute is absent, incompatible or names an uninstantiable container
     * @return the declared type for the same class or a constrained container schema, otherwise the concrete type
     */
    private static Type<?> keepDeclaredType(final Type<?> declaredType, final Class<?> concreteClass) {
        return concreteClass == declaredType.javaType() ? declaredType : containerReadType(declaredType, concreteClass);
    }

    /**
     * Reads a bean property's scalar text with the type the DOM reader reads it with, so that the three readers agree. An
     * {@code Object} property (or one declared as an interface the Object handler reads) adopts the approved scalar class its
     * type attribute names while type information is honoured and no value type is configured for the property
     * ({@code <open type="Long">5</open>} reads as {@code Long 5}: the class {@code getConcreteClass} selects for the open slot,
     * as {@link #keepDeclaredType} keeps it in the DOM property loop); a primitive name does not fit an Object slot and keeps the
     * declared type. Every other property keeps its own codec, and a formatted property always reads through its format. A
     * value type configured for the property types its text as on the DOM reader, which reads it through
     * {@code readValueText} with the configured type: the configuration wins over a scalar attribute, an {@code Object}
     * configuration keeps scalar text and adopts container metadata. An attribute naming the declared class itself, whose
     * codec cannot read text at all ({@code type="Number"} on a {@code Number} property, the writer's own output), is the
     * {@link ParsingException} of {@code valueOfAdoptedText}, as on the DOM and standard readers. Container metadata on an
     * unconfigured property never reaches this reader: the callers route container-typed text to {@code readValueText}.
     *
     * @param propInfo the bean property
     * @param configuredType the value type configured for the property, or {@code null}
     * @param attributeType the property element's approved metadata, or {@code null}
     * @param adoptable whether metadata may type the property: type information honoured and no configured value type
     * @param text the property text
     * @param config the deserialization configuration
     * @return the decoded value
     */
    private static Object readPropertyText(final PropInfo propInfo, final Type<?> configuredType, final Type<?> attributeType, final boolean adoptable,
            final String text, final XmlDeserConfig config) {
        if (propInfo.hasFormat) {
            return propInfo.readPropValue(text);
        }

        if (configuredType != null) {
            return readValueText(refineObjectWrapper(configuredType, attributeType), configuredType, attributeType, text, config);
        }

        if (attributeType != null && adoptable && propInfo.jsonXmlType.isObject()) {
            final Type<?> adopted = keepDeclaredType(propInfo.jsonXmlType, getConcreteClass(attributeType.javaType(), propInfo.jsonXmlType.javaType()));

            if (adopted != propInfo.jsonXmlType) {
                return valueOfAdoptedText(adopted, attributeType, text);
            }
        }

        // PropInfo.readPropValue without a format is jsonXmlType.valueOf(text).
        return valueOfAdoptedText(propInfo.jsonXmlType, attributeType, text);
    }

    /**
     * The value of a bean property's text on the StAX reader: the text of an element that holds text, or the empty text of an
     * element without content. The same routing as the DOM reader's {@code readValueText(Node, ...)}: a container, array or
     * Optional/Nullable schema (declared, configured or adopted from metadata) reads through {@code readValueText}, anything
     * else through {@link #readPropertyText}.
     *
     * @param propInfo the bean property
     * @param propType the property's schema after configuration and metadata
     * @param attributeType the property element's approved metadata, or {@code null}
     * @param propConfigured whether a value type is configured for the property ({@code propType} is then that type)
     * @param ignoreTypeInfo whether type information is ignored for the document
     * @param text the property text, empty for an element without content
     * @param config the deserialization configuration
     * @return the decoded value
     */
    private static Object readStreamPropertyText(final PropInfo propInfo, final Type<?> propType, final Type<?> attributeType, final boolean propConfigured,
            final boolean ignoreTypeInfo, final String text, final XmlDeserConfig config) {
        return propInfo.hasFormat || !(propType.isMap() || propType.isCollection() || propType.isArray() || propType.isOptionalOrNullable())
                ? readPropertyText(propInfo, propConfigured ? propType : null, attributeType, !ignoreTypeInfo && !propConfigured, text, config)
                : readValueText(refineObjectWrapper(propType, attributeType), propType, attributeType, text, config);
    }

    /**
     * Retains declared generic arguments without attaching them to a potentially different subclass layout.
     *
     * @param declaredType the declared or configured schema
     * @param result the compatible type selected from approved metadata
     * @return the declared container schema with Object slots refined, otherwise the metadata type
     */
    private static Type<?> retainDeclaredParameters(final Type<?> declaredType, final Type<?> result) {
        if (result == declaredType) {
            return declaredType;
        }
        if (declaredType.isMap() || declaredType.isCollection() || declaredType.isArray() || declaredType.isOptionalOrNullable()) {
            return refineDeclaredSchema(declaredType, result);
        }
        final List<Type<?>> parameters = declaredType.parameterTypes();
        for (int i = 0, size = parameters.size(); i < size; i++) {
            if (!parameters.get(i).isObject()) {
                // A subtype may bind, reorder or fix its base class's variables. Keep the original parsing schema;
                // the readers carry the compatible runtime class separately instead of transplanting argument text.
                return declaredType;
            }
        }
        return result;
    }

    /**
     * Reads an entry key/value or item wrapper, leaving the reader on its end element.
     * Text is retained until the following event distinguishes a scalar from an indented child.
     * Callers must approve the current wrapper's metadata before invoking this method, including for null values
     * and configured types. Reusing that result avoids another attribute parse for every scalar item.
     *
     * @param xmlReader the reader positioned on the value wrapper
     * @param config the effective deserialization configuration
     * @param valueType the selected declared/configured value type
     * @param declaredValueType the original value schema before metadata refinement
     * @param attributeType the current wrapper's approved metadata, or {@code null}
     * @param checkedAttr whether enclosing attribute handling has been determined
     * @param isTagByPropertyName whether tags represent property names
     * @param ignoreTypeInfo whether ordinary enclosing type metadata is ignored
     * @param inputType the enclosing input type
     * @return the decoded value
     * @throws XMLStreamException if advancing the XML reader fails
     * @throws ParsingException if a value wrapper mixes scalar and nested values, has an unexpected closing element, or ends prematurely
     */
    private Object readWrappedValue(final XMLStreamReader xmlReader, final XmlDeserConfig config, Type<?> valueType, final Type<?> declaredValueType,
            final Type<?> attributeType, final boolean checkedAttr, final boolean isTagByPropertyName, final boolean ignoreTypeInfo, final Type<?> inputType)
            throws XMLStreamException, ParsingException {
        valueType = refineObjectWrapper(valueType, attributeType);
        // An originally open Object slot keeps global element configuration ahead of the component the metadata
        // supplied (the declared slot's own component, when any, still wins over configuration).
        valueType = withSlotComponent(declaredValueType, valueType, config.getElementType());
        // The global map types fill a declared map slot's open arguments ahead of the metadata's.
        valueType = withSlotMapTypes(declaredValueType, valueType, config);
        // The array destination is decided where the array is read (readValueText for text, the ARRAY case for child
        // elements), so the refined schema still types the items: a declared Map[] slot with LinkedMultiValueMap[]
        // metadata is constructed as Map[] while its items are that map.
        if (Boolean.parseBoolean(getAttribute(xmlReader, XmlConstants.IS_NULL))) {
            skipNullElement(xmlReader);
            return null;
        }
        // A wrapper usually holds a single text event: keep that fragment as is and only join once a second one arrives.
        String text = null;
        StringBuilder sb = null;
        while (xmlReader.hasNext()) {
            final int event = xmlReader.next();
            if (isTextEvent(event)) {
                if (text == null) {
                    text = String.valueOf(xmlReader.getText());
                } else {
                    if (sb == null) {
                        sb = new StringBuilder(text);
                    }
                    sb.append(xmlReader.getText());
                }
            } else if (event == XMLStreamConstants.START_ELEMENT) {
                if (text != null && !(sb == null ? text : sb.toString()).isBlank()) {
                    throw new ParsingException("Mixed scalar and nested XML values are not supported");
                }
                // See wrapOptional: a nested value of an Optional/Nullable slot is read with the wrapper's element type.
                final Type<?> nestedType = optionalElementTypeOrSelf(valueType);
                // Refinement must not turn an originally open element slot into an explicit constraint that defeats configuration.
                final Type<?> declaredNestedType = optionalElementTypeOrSelf(declaredValueType);
                final Type<?> childType = declaredNestedType.isCollection() || declaredNestedType.isMap() || declaredNestedType.isArray() ? declaredNestedType
                        : nestedType;
                // A String slot cannot hold a nested element: the wrapper's container metadata types it as in an open slot, as the
                // SAX and DOM readers type it (<value type="List<Integer>"><list>.. in a Map<String, String> read String items
                // here). Bean metadata does not open the slot on any reader, and the slot itself (childType) still decides bean
                // discovery.
                final Type<?> nestedTarget = nestedType.isString() && attributeType != null
                        && (attributeType.isMap() || attributeType.isCollection() || attributeType.isArray()) ? objType : nestedType;
                final Object value;
                enterXmlNesting();
                try {
                    value = readByStreamParserBody(xmlReader, config, childType, null, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, nestedTarget,
                            inputType, attributeType);
                } finally {
                    exitXmlNesting();
                }
                if (nextStructuralEvent(xmlReader) != XMLStreamConstants.END_ELEMENT) {
                    throw new ParsingException("Expected the end of an XML value wrapper");
                }
                return nestedType == valueType ? value : wrapOptional(valueType, value);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return readValueText(valueType, declaredValueType, attributeType, sb != null ? sb.toString() : text == null ? Strings.EMPTY : text, config);
            }
        }
        throw new ParsingException("Unexpected end of XML value wrapper");
    }

    /**
     * Consumes a null-marked element through its matching end. Stopping at the first child's end
     * leaves the parent reader one level too deep and can silently discard subsequent siblings.
     * The caller approves the marked element's own metadata before skipping its unused contents.
     *
     * @param reader the reader positioned on the null element's start
     * @throws XMLStreamException if the XML cannot be read
     * @throws ParsingException if the element ends prematurely
     */
    private static void skipNullElement(final XMLStreamReader reader) throws XMLStreamException, ParsingException {
        int depth = 1;
        while (reader.hasNext()) {
            final int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT && --depth == 0) {
                return;
            }
        }
        throw new ParsingException("Unexpected end of null XML element");
    }

    /**
     * Reads container text using its declared schema and runtime construction class, then reapplies any wrapper.
     * An open Object slot adopts container metadata as its schema (an uninstantiable container as an open mutable
     * schema) instead of falling back to a plain String, and keeps global element configuration ahead of the
     * component the metadata supplied.
     *
     * @param valueType the selected declared or configured value type
     * @param declaredSlot the value schema before metadata refinement, possibly a wrapper
     * @param metadata the approved runtime metadata, or {@code null}
     * @param text the scalar or JSON-form container content
     * @param config the deserialization configuration: its element type fills open components, and its value types
     *        reach compact container text
     * @return the decoded value
     */
    private static Object readValueText(final Type<?> valueType, final Type<?> declaredSlot, final Type<?> metadata, final String text,
            final XmlDeserConfig config) {
        final Type<?> configuredElementType = config.getElementType();
        final Type<?> payload = optionalElementTypeOrSelf(valueType);
        // DOM/SAX text slots may reach this reader before nested-element type selection runs.
        // Even without a local attribute, the payload can carry refined metadata from its parent.
        final Type<?> schema = payload.isMap() || payload.isCollection() || payload.isArray() || payload.isObject() ? refineDeclaredSchema(payload, metadata)
                : payload;
        if (schema.isMap() || schema.isCollection() || schema.isArray()) {
            // The global map types fill a declared map slot's open arguments ahead of the metadata's (the construction
            // then falls back from a list-valued implementation that cannot hold the configured values), and JSON text
            // of an array value keeps its declared runtime class: an originally open slot lets global element
            // configuration type the elements (an Object[] of Long, never a Long[]), and array metadata must not close
            // that open slot's destination either. refineSlot decides both.
            final SlotSchema slotSchema = refineSlot(declaredSlot, schema, config);
            final Type<?> effective = slotSchema.refined();
            final Object value = schema.isArray()
                    ? readArrayText(text, slotSchema.destination(), arrayComponentOverride(declaredSlot, effective, configuredElementType), config)
                    : readContainerText(text, effective, getConcreteClass(metadata == null ? null : metadata.javaType(), effective.javaType()), config);
            return payload == valueType ? value : wrapOptional(valueType, value);
        }
        return valueOfAdoptedText(valueType, metadata, text);
    }

    // The element type of a declared java.util.Optional / u.Optional / u.Nullable type, or the type itself.
    private static Type<?> optionalElementTypeOrSelf(final Type<?> type) {
        return type != null && type.isOptionalOrNullable() && type.elementType() != null ? type.elementType() : type;
    }

    // The writers put a present Optional/Nullable map key, map value or item as the value it holds, so a structured value (bean,
    // map, collection, array) arrives as a nested element. Read with the wrapper type itself it became an ArrayList (or a bare
    // bean) in the wrapper-typed slot; it is read with the element type instead and wrapped again here, without the lossy text
    // round trip of the wrapper type's valueOf(Object).
    private static Object wrapOptional(final Type<?> wrapperType, final Object value) {
        final Class<?> wrapperClass = wrapperType.javaType();

        if (wrapperClass == java.util.Optional.class) {
            return java.util.Optional.ofNullable(value);
        } else if (wrapperClass == u.Optional.class) {
            return u.Optional.ofNullable(value);
        } else if (wrapperClass == u.Nullable.class) {
            return value == null ? u.Nullable.empty() : u.Nullable.of(value);
        }

        return wrapperType.valueOf(value);
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

        return checkRootValue(readByDOMParser(toElementNode(node), configToUse, configToUse.getElementType(), false, false, false, true, targetType),
                targetType);
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
            return readByDOMParserBody(node, config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, isFirstCall, inputType, null);
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
     * @param wrapperMetadata the enclosing value wrapper's already approved construction metadata, or {@code null}
     * @return the deserialized value of type {@code T}
     * @throws ParsingException if the XML does not match the target type
     */
    @SuppressWarnings("deprecation")
    private <T> T readByDOMParserBody(final Node node, final XmlDeserConfig config, Type<?> propType, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final boolean isFirstCall, final Type<T> inputType, final Type<?> wrapperMetadata) throws ParsingException {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        // Check before the null-root shortcut and reuse the result when type refinement is enabled.
        final Type<?> nodeAttributeType = resolvePresentTypeAttribute(XmlUtil.getAttribute(node, XmlConstants.TYPE));
        if (isFirstCall && Boolean.parseBoolean(XmlUtil.getAttribute(node, XmlConstants.IS_NULL))) {
            return null;
        }
        final Class<?> attributeClass = nodeAttributeType == null ? null : nodeAttributeType.javaType();
        Type<?> attributeType = null;
        final boolean hasPropTypes = config.hasValueTypes();

        String nodeName = checkedAttr ? (isTagByPropertyName ? localName(node) : XmlUtil.getAttribute(node, XmlConstants.NAME))
                : XmlUtil.getAttribute(node, XmlConstants.NAME);
        nodeName = (nodeName == null) ? localName(node) : nodeName;

        Type<?> targetType = null;
        Class<?> targetClass = null;
        // Set when an open Object slot (root or nested) adopted container metadata as its schema below.
        boolean objectSlotAdopted = false;

        if (isFirstCall) {
            targetType = inputType;
            targetClass = targetType.javaType();
        } else {
            if (propType == null || propType.isString() || propType.isObject()) {
                targetType = hasPropTypes ? config.getValueType(nodeName, null) : null;
            } else {
                targetType = propType;
            }

            if ((targetType == null || targetType.isObject()) && !(checkedAttr && ignoreTypeInfo)) {
                // A String slot is open for a nested element (see above), so wrapper metadata types it as in an Object slot, as
                // the StAX and SAX readers type it: a map under map metadata kept String values here, unlike a list.
                Type<?> payloadType = targetType != null ? targetType : propType == null || propType.isString() ? objType : propType;
                if (wrapperMetadata != null) {
                    payloadType = resolveItemType(payloadType, getConcreteClass(wrapperMetadata.javaType(), payloadType.javaType()), wrapperMetadata);
                }
                payloadType = resolveItemType(payloadType, getConcreteClass(attributeClass, payloadType.javaType()), nodeAttributeType);
                if (payloadType.isMap() || payloadType.isCollection() || payloadType.isArray()) {
                    // An open Object slot adopted the metadata's container schema; the declaration remembered
                    // below stays Object, so global element configuration still overrides the adopted component.
                    objectSlotAdopted = true;
                    targetType = payloadType;
                }
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

        final Type<?> declaredTargetType = objectSlotAdopted ? objType : targetType;
        if (!(checkedAttr && ignoreTypeInfo)) {
            if (wrapperMetadata != null) {
                targetClass = getConcreteClass(wrapperMetadata.javaType(), targetClass);
                targetType = resolveItemType(targetType, targetClass, wrapperMetadata);
            }
            targetClass = getConcreteClass(attributeClass, targetClass);
            targetType = resolveItemType(targetType, targetClass, nodeAttributeType);
            if (targetClass == Object.class && (targetType.isMap() || targetType.isCollection() || targetType.isArray())) {
                // An open Object slot adopted the metadata's container schema at the root (an uninstantiable
                // container as its open mutable schema); construction follows the adopted class.
                targetClass = targetType.javaType();
            }
            // Array metadata must not close an originally open array slot: configuration types the elements
            // while the slot's own declared runtime (Object[]) is constructed. The construction class follows
            // the restored type: containerReadType below would otherwise rebuild the metadata's array class.
            final Type<?> openSlotType = openArrayDestination(declaredTargetType, targetType, config.getElementType());
            if (openSlotType != targetType) {
                targetType = openSlotType;
                targetClass = targetType.javaType();
            }
        }

        if (targetType == null) {
            if (targetClass == null) {
                throw new ParsingException("Unable to determine target type for xml node: " + nodeName);
            }

            targetType = Type.of(targetClass);
        } else if (targetClass == null) {
            targetClass = targetType.javaType();
        } else if (!targetType.javaType().equals(targetClass)) {
            targetType = containerReadType(targetType, targetClass);
        }

        final NodeType nodeType = getNodeType(nodeName, null);

        if ((nodeType == NodeType.ENTITY || nodeType == NodeType.MAP || nodeType == NodeType.ARRAY || nodeType == NodeType.COLLECTION)
                && Boolean.parseBoolean(XmlUtil.getAttribute(node, XmlConstants.IS_NULL))) {
            return null;
        }

        final NodeList propNodes = node.getChildNodes();
        final int propNodeLength = getNodeLength(propNodes);
        PropInfo propInfo = null;
        Node propNode = null;
        String propName = null;
        Object propValue = null;

        switch (nodeType) {
            case ENTITY: {
                if (isFirstCall) {
                    // At the root propType is the configured element type, not this node's declared type (as for
                    // MAP/COLLECTION below): a bean element type made a bean root read as that bean for a List/Object
                    // target, where the StAX/SAX readers reject it.
                    propType = null;
                }

                if (!targetType.isBean()) {
                    if ((propType != null) && propType.isBean()) {
                        targetType = propType;
                    } else if (!isFirstCall && attributeClass != null && !(checkedAttr && ignoreTypeInfo)
                            && (propType == null || propType.javaType().isAssignableFrom(attributeClass))
                            && (attributeType = Type.of(attributeClass)).isBean()) {
                        // A bean held in an Object property names its class in its type attribute, which the StAX reader uses.
                        // The slot's list default cannot hold a bean, so the attribute was dropped above and only node-name
                        // discovery was left, which searches a fixed package and failed for a bean anywhere else. Only a class
                        // that fits the declared slot type is taken (an interface slot is also defaulted to a list); one that does
                        // not fit is left to discovery, as on StAX.
                        targetType = attributeType;
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
                            } else if (isOpenBeanSlot(propType, isFirstCall, inputType)) {
                                // No class for the element: an open slot reads the bean-shaped element as a map of its
                                // properties, as the standard readers do, instead of failing on the writer's own untyped output.
                                return (T) readBeanShapedNodeAsMap(node, config, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType);
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

                    // Test the resolved property name too: getPropInfo also matches case and snake-case variants.
                    if (ignoredClassPropNames != null
                            && (ignoredClassPropNames.contains(propName) || (propInfo != null && ignoredClassPropNames.contains(propInfo.name)))) {
                        continue;
                    }

                    if (propInfo == null) {
                        if (ignoreUnmatchedProperty) {
                            continue;
                        } else {
                            throw new ParsingException("Unknown property element: " + propName + " for class: " + ClassUtil.getCanonicalClassName(targetClass));
                        }
                    }

                    // Check the property wrapper before scalar/configured-type shortcuts on the DOM path. The approved
                    // metadata is reused below (getConcreteClass) and by the text / nested value readers, each of which
                    // used to read and resolve the attribute again.
                    final Type<?> propAttributeType = resolvePresentTypeAttribute(XmlUtil.getAttribute(propNode, XmlConstants.TYPE));
                    propType = hasPropTypes ? config.getValueType(propName) : null;

                    if (propType == null) {
                        // A serializable property ignores its type attribute, except an array or collection of an abstract
                        // scalar (Number[]), whose metadata names the component the elements are read with.
                        if (ignoresTypeAttribute(propInfo.jsonXmlType) || ignoreTypeInfo) {
                            propType = propInfo.jsonXmlType;
                        } else {
                            // Keep the declared type (with its type arguments) when the type attribute names the declared class
                            // itself: Type.of(Class) is raw, and a raw EnumMap<K, V> property would lose the key enum it needs.
                            final Class<?> concreteClass = getConcreteClass(propAttributeType == null ? null : propAttributeType.javaType(),
                                    propInfo.jsonXmlType.javaType());

                            propType = keepDeclaredType(propInfo.jsonXmlType, concreteClass);
                        }

                        // Array metadata must not close an originally open array slot: configuration types the
                        // elements while the property's own declared runtime (Object[]) is constructed.
                        propType = openArrayDestination(propInfo.jsonXmlType, propType, config.getElementType());
                    }

                    if (XmlUtil.isTextElement(propNode)) {
                        propValue = readValueText(propNode, propType, propInfo, propName, config, propAttributeType);
                    } else {
                        //noinspection ConstantValue
                        propValue = readNestedValue(propNode, config, propType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType, propAttributeType);
                    }

                    if (propInfo.jsonXmlExpose != JsonXmlField.Direction.SERIALIZE_ONLY) {
                        propInfo.setPropValue(result, propValue);
                    }
                }

                return beanInfo.finishBeanResult(result);
            }

            case MAP: {
                if (isFirstCall) {
                    // At the root propType is the configured element type, not this map's declared type (see COLLECTION below):
                    // a Map<K, V> element type replaced the key/value types of the root map itself.
                    propType = null;
                }

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
                    if (config.getMapKeyType() != null && !config.getMapKeyType().isObject() && !keepsRequestedContainer(declaredTargetType, 0)) {
                        keyType = config.getMapKeyType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                        keyType = targetType.parameterTypes().get(0);
                    }
                }

                Type<?> valueType = defaultValueType;

                if (propType != null && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                    // A value declared as an abstract scalar (Map<String, Number>) adopts the value the wrapper's metadata named
                    // (HashMap<String, Long>, resolved into targetType above), as compact text reads it (narrowedArgument).
                    valueType = narrowedArgument(propType.parameterTypes().get(1),
                            targetType.isMap() && targetType.parameterTypes().size() == 2 ? targetType.parameterTypes().get(1) : null);
                } else {
                    if (config.getMapValueType() != null && !config.getMapValueType().isObject() && !keepsRequestedContainer(declaredTargetType, 1)) {
                        valueType = config.getMapValueType();
                    } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                        valueType = targetType.parameterTypes().get(1);
                    }
                }

                // Same creation as the StAX reader above: Immutable* via a mutable stand-in, EnumMap keyed by its enum.
                final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter = mapCreatorAndConverter(targetClass, valueType,
                        scalarOverrides(config));

                Map<Object, Object> mResult = canReuseCompactContainer(targetClass) ? null : newDomMap(creatorAndConverter, targetClass, keyType, node);

                if (XmlUtil.isTextElement(node)) {
                    final String compactText = XmlUtil.getTextContent(node);
                    if (isCompactMapText(compactText)) {
                        // The compact reader already constructs the requested implementation and applies its converter.
                        return (T) readContainerText(compactText, compactMapSchema(isFirstCall, propType, keyType, valueType), targetClass, config);
                    }
                }

                if (mResult == null) {
                    mResult = newDomMap(creatorAndConverter, targetClass, keyType, node);
                }

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

                    // Match SAX/StAX: an entry's type does not select its value type, but still requires approval.
                    resolvePresentTypeAttribute(XmlUtil.getAttribute(entryNode, XmlConstants.TYPE));

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

                    // The key wrapper's type attribute is read and approved once per entry and reused by the open-key fallback
                    // and the text / nested readers below (each used to resolve it again). Approval is required even when
                    // type info is ignored: every path below resolved the attribute before reading the key.
                    final Type<?> keyMetadata = resolvePresentTypeAttribute(XmlUtil.getAttribute(propKeyNode, XmlConstants.TYPE));
                    propKeyClass = checkedAttr && ignoreTypeInfo ? keyType.javaType()
                            : getConcreteClass(keyMetadata == null ? null : keyMetadata.javaType(), keyType.javaType());

                    // A text key of an open slot is read as a String; a nested key keeps the Object key type as an open slot, as a
                    // nested value does. A key whose attribute names a concrete class (a bean) is read with that class, as a value
                    // is on every backend (the nested reader used to receive the declared Object type and read the bean as a map).
                    final boolean objectKey = propKeyClass == Object.class;

                    if (objectKey) {
                        propKeyClass = String.class;
                    }

                    propKeyType = keepDeclaredType(keyType, propKeyClass);

                    if (propKeyClass == String.class && keyType.javaType() == Object.class) {
                        // getConcreteClass fell back to Object because the attribute names an uninstantiable
                        // container (List.of(..)/Set.of(..)/Map.of(..)): an open Object KEY still adopts its
                        // arguments as an open mutable schema, as the value side does, before the String fallback.
                        final Type<?> adoptedKey = resolveItemType(keyType, keyMetadata);
                        if (adoptedKey.isMap() || adoptedKey.isCollection() || adoptedKey.isArray()) {
                            propKeyType = adoptedKey;
                            propKeyClass = adoptedKey.javaType();
                        }
                    }

                    //noinspection DataFlowIssue
                    if (XmlUtil.isTextElement(propKeyNode)) {
                        //noinspection ConstantValue
                        propKey = readValueText(propKeyNode, propKeyType, propInfo, XmlConstants.KEY, config, keyMetadata);
                    } else {
                        // An open key that adopted container metadata (an uninstantiable container as its open mutable schema) is
                        // read with that schema, as an open value is and as the StAX and SAX readers read it: a nested element
                        // that is no item of the container is rejected (it was read through the open Object key as a map).
                        final boolean adoptedContainerKey = propKeyType.isMap() || propKeyType.isCollection() || propKeyType.isArray();
                        propKey = readNestedValue(propKeyNode, config, objectKey && !adoptedContainerKey ? keyType : propKeyType, checkedAttr,
                                isTagByPropertyName, ignoreTypeInfo, inputType, keyMetadata);
                    }

                    // NULL_STRING, not a bare null: contains(null) throws NPE on a Set.of(..) - see writeMap.
                    if (ignoredClassPropNames != null && ignoredClassPropNames.contains(propKey == null ? NULL_STRING : propKey.toString())) {
                        continue;
                    }

                    // Named value configuration applies to any key by its text, as on the standard readers and in JSON.
                    // A scalar per-key type on a declared list value names its element type (elementOverride).
                    propValueType = hasPropTypes && propKey != null ? elementOverride(targetClass, valueType, config.getValueType(N.toString(propKey))) : null;
                    // The configured type is the entry's declaration: a configured String[] is constructed, not the map's Integer[].
                    final Type<?> configuredValueType = propValueType;
                    // An explicit Object override reads the value as an open slot of its own: the map's value schema no
                    // longer applies to it, as on the StAX and SAX readers and in compact text.
                    final boolean explicitObjectOverride = propValueType != null && propValueType.javaType() == Object.class;
                    // The value wrapper's type attribute is read and approved once per entry and reused by the open-value
                    // fallback and the text / nested readers below (each used to resolve it again). Approval is required even
                    // for a configured value type or ignored type info: every path below resolved the attribute before reading.
                    final Type<?> valueMetadata = resolvePresentTypeAttribute(XmlUtil.getAttribute(propValueNode, XmlConstants.TYPE));

                    if (propValueType == null) {
                        propValueClass = checkedAttr && ignoreTypeInfo ? valueType.javaType()
                                : getConcreteClass(valueMetadata == null ? null : valueMetadata.javaType(), valueType.javaType());
                    } else {
                        propValueClass = propValueType.javaType();
                    }

                    // A text value is read as a String; a nested element keeps the Object value type, so that a bean in it is read
                    // through its type attribute (the nested reader only accepts an attribute class that fits the slot).
                    boolean objectValue = propValueClass == Object.class;

                    if (objectValue) {
                        propValueClass = String.class;
                    }

                    // A configured value type is kept whole, as the StAX and SAX readers keep it: Type.of(its class) is raw,
                    // so a configured List<Integer> value read back as a list of Strings.
                    propValueType = propValueType != null && propValueType.javaType() == propValueClass ? propValueType
                            : keepDeclaredType(valueType, propValueClass);
                    // Array metadata must not close an originally open array slot: configuration types the elements
                    // while the slot's own declared runtime (Object[]) is constructed.
                    propValueType = openArrayDestination(configuredValueType != null ? configuredValueType : valueType, propValueType, config.getElementType());

                    if (objectValue) {
                        // getConcreteClass fell back to Object because the attribute names an uninstantiable
                        // container (List.of(..)/Set.of(..)/Map.of(..)): the open Object slot still adopts its
                        // arguments as an open mutable schema, with global element configuration ahead of them.
                        final Type<?> openSlot = explicitObjectOverride ? objType : valueType;
                        final Type<?> adopted = withSlotComponent(openSlot, resolveItemType(openSlot, valueMetadata), config.getElementType());
                        if (adopted.isMap() || adopted.isCollection() || adopted.isArray()) {
                            propValueType = adopted;
                            propValueClass = adopted.javaType();
                            objectValue = false;
                        }
                    }

                    //noinspection DataFlowIssue
                    if (XmlUtil.isTextElement(propValueNode)) {
                        //noinspection ConstantValue
                        propValue = readValueText(propValueNode, propValueType, propInfo, XmlConstants.VALUE, config, valueMetadata);
                    } else {
                        Type<?> nestedSlot = objectValue ? objType : propValueType;

                        if (objectValue && explicitObjectOverride && valueMetadata != null && valueMetadata.isBean()) {
                            // An explicit Object override is an open slot of its own: a nested element carrying an approved bean
                            // class is read as that bean, as the StAX and SAX readers read it (an unconfigured open slot adopts
                            // the class through getConcreteClass above; text under the override stays a String either way).
                            nestedSlot = valueMetadata;
                        }

                        propValue = readNestedValue(propValueNode, config, nestedSlot, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType,
                                valueMetadata);
                    }

                    mResult.put(propKey, propValue);
                }

                return (T) (creatorAndConverter == null ? mResult : creatorAndConverter._2.apply(mResult));
            }

            case ARRAY: { //NOSONAR
                if (isFirstCall) {
                    // At the root propType is the configured element type, not this array's declared type (see COLLECTION below).
                    propType = null;
                }

                boolean untypedArray = false;
                // An untyped <array> whose items the configured element type types gets an array of the configured class
                // (Integer[] for configured Integer items, as the SAX reader builds it): the String[] placeholder cannot hold them.
                boolean configuredUntypedArray = false;

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
                    // The array's metadata may name a specialized item: child elements construct it as compact text does.
                    eleType = refinedItemType(propType.elementType(), targetType, config.getElementType());
                    untypedArray = false;
                } else if (!untypedArray && targetType.elementType() != null && !targetType.elementType().isObject()) {
                    // A declared array component is reified: it beats global element configuration, as on the StAX reader.
                    eleType = targetType.elementType();
                } else {
                    // The configured component types the open elements once, never a configured item's own elements.
                    if (elementConfigurationApplies(config, null)) {
                        eleType = config.getElementType();
                        configuredUntypedArray = untypedArray;
                        untypedArray = false;
                    } else {
                        eleType = targetType.isArray() ? targetType.elementType() : objType;
                    }
                }

                propName = XmlConstants.E; //NOSONAR

                // Runtime metadata never narrows a declared array class: a declared Map[] or Number[] slot is constructed
                // as declared (openArrayDestination) while its items take the refined or configured component
                // (arrayItemType), as on the standard readers. The refined schema arrives as targetType, or is rebuilt
                // from the metadata when the head above already restored the declared array: the value wrapper's, or
                // the <array> element's own (the writer puts the type on both; a trimmed document may keep only one).
                // At the root the requested array is the declared slot (propType carries the configured element type there).
                final Type<?> declaredSlot = isFirstCall ? declaredTargetType : propType;
                final boolean declaredArray = declaredSlot != null && declaredSlot.isArray() && !untypedArray;
                final Type<?> arrayMetadata = wrapperMetadata != null ? wrapperMetadata : checkedAttr && ignoreTypeInfo ? null : nodeAttributeType;
                final Type<?> refinedArray = declaredArray && targetType == declaredSlot && arrayMetadata != null
                        ? refineDeclaredSchema(declaredSlot, arrayMetadata)
                        : targetType;
                final Type<?> itemOverride = declaredArray ? arrayItemType(declaredSlot, refinedArray, config.getElementType()) : null;
                if (itemOverride != null) {
                    eleType = itemOverride;
                }
                final Type<?> arrayDestination = declaredArray ? openArrayDestination(declaredSlot, refinedArray, config.getElementType()) : targetType;

                if (XmlUtil.isTextElement(node)) {
                    final String st = XmlUtil.getTextContent(node);

                    // Whitespace-only content is the pretty-printed empty array; Type.valueOf(blank) would answer null.
                    if (Strings.isBlank(st)) {
                        // A typed array keeps its DECLARED component: configuration types the elements, never the
                        // reified array type (an Object[] stays Object[0]), matching the SAX placeholder rule.
                        return N.newArray(untypedArray ? eleType.javaType() : arrayDestination.javaType().getComponentType(), 0);
                    } else {
                        // The JSON form must use the same configured component as individual item elements.
                        // The shared array reader: the configured component reaches the open elements nested in the items
                        // (Object[][] with a configured Long), as on the standard readers.
                        final Object parsed = readArrayText(st, arrayDestination,
                                declaredArray ? arrayComponentOverride(declaredSlot, refinedArray, config.getElementType())
                                        : textComponent(eleType, arrayDestination),
                                config);

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

                            // A text item of an Object or interface item type is read as a String; a nested element keeps the item
                            // type (the array's component type, or the item's own type attribute), as on the StAX and SAX readers -
                            // a bean's type attribute is used when it fits that type.
                            // An untyped array's String placeholder types its text items only: a structured item is open, as on
                            // the StAX reader.
                            // The item's type attribute is read and approved once and reused by the text / nested readers.
                            final Type<?> itemMetadata = resolvePresentTypeAttribute(XmlUtil.getAttribute(eleNode, XmlConstants.TYPE));
                            final Type<?> itemType = resolveItemType(untypedArray ? defaultValueType : eleType, itemMetadata);
                            propType = itemType.isObject() ? strType : itemType;

                            if (XmlUtil.isTextElement(eleNode)) {
                                //noinspection ConstantValue
                                c.add(readValueText(eleNode, propType, propInfo, propName, itemConfig(config, declaredSlot, propType), itemMetadata));
                            } else {
                                c.add(readNestedValue(eleNode, itemConfig(config, declaredSlot, itemType), itemType, checkedAttr, isTagByPropertyName,
                                        ignoreTypeInfo, inputType, itemMetadata));
                            }
                        }

                        return collectionToArray(c,
                                configuredUntypedArray ? configuredArrayType(c, eleType) : untypedArray ? inferArrayType(c) : arrayDestination);
                    } finally {
                        Objectory.recycle(c);
                    }
                }
            }

            case COLLECTION: {
                if (isFirstCall) {
                    // At the root propType is the configured ELEMENT type (see readByDOMParser(Node, XmlDeserConfig, Type)), not
                    // this container's own declared type: using its element type read a List<Integer> element type one level
                    // too deep ([[1, 2]] came back as [[[1], [2]]]). The StAX and SAX readers start the root without it.
                    propType = null;
                }

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
                    // The container's metadata may name a specialized item (a LinkedMultiValueMap in a declared
                    // List<Map<String, Object>>): child elements construct it as compact text does.
                    eleType = refinedItemType(propType.elementType(), targetType, config.getElementType());
                } else {
                    // The configured component types the open elements once (never a configured item's own elements), and a
                    // requested container element (List<Object[]> at the root) keeps its shape with the configuration inside it.
                    if (elementConfigurationApplies(config, declaredTargetType)) {
                        eleType = config.getElementType();
                    } else {
                        // An Object or interface element type (List<SomeInterface>) is an Object slot, as on the StAX and SAX
                        // readers: an item's type attribute names its class.
                        eleType = targetType.elementType() == null || targetType.elementType().isObject() ? objType : targetType.elementType();
                    }
                }

                propName = XmlConstants.E; //NOSONAR

                // The collection's declared slot, for itemConfig below: propType is reused for each item's own type.
                final Type<?> declaredSlot = isFirstCall ? declaredTargetType : propType;

                // Same creation as the StAX reader above: Immutable* targets via a mutable stand-in, wrapped on return.
                final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> creatorAndConverter = collectionCreatorAndConverter(targetClass, true);
                final boolean reuseCompactContainer = canReuseCompactContainer(targetClass);
                Collection<Object> result = reuseCompactContainer ? null : newDomCollection(creatorAndConverter, targetClass, node);

                if (XmlUtil.isTextElement(node)) {
                    // JSON text form <list>[1, 2, 3]</list>, as the StAX reader accepts it; blank text is an empty collection.
                    final Object parsed = parseJsonCollectionText(XmlUtil.getTextContent(node), eleType, targetType, targetClass,
                            isFirstCall ? null : declaredTargetType, config);

                    if (parsed != null) {
                        if (reuseCompactContainer) {
                            return (T) parsed;
                        }
                        result.addAll((Collection<Object>) parsed);
                    }

                    if (result == null) {
                        result = newDomCollection(creatorAndConverter, targetClass, node);
                    }
                    return (T) (creatorAndConverter == null ? result : creatorAndConverter._2.apply(result));
                }

                if (result == null) {
                    result = newDomCollection(creatorAndConverter, targetClass, node);
                }

                final NodeList eleNodes = node.getChildNodes();
                Node eleNode = null;

                for (int k = 0; k < eleNodes.getLength(); k++) {
                    eleNode = eleNodes.item(k);

                    if (eleNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    // A text item of an Object or interface item type is read as a String; a nested element keeps the item type
                    // (the element type, or the item's own type attribute), as on the StAX and SAX readers - a bean's type
                    // attribute is used when it fits that type.
                    // The item's type attribute is read and approved once and reused by the text / nested readers.
                    final Type<?> itemMetadata = resolvePresentTypeAttribute(XmlUtil.getAttribute(eleNode, XmlConstants.TYPE));
                    final Type<?> itemType = resolveItemType(eleType, itemMetadata);
                    propType = itemType.isObject() ? strType : itemType;

                    if (XmlUtil.isTextElement(eleNode)) {
                        //noinspection ConstantValue
                        result.add(readValueText(eleNode, propType, propInfo, propName, itemConfig(config, declaredSlot, propType), itemMetadata));
                    } else {
                        result.add(readNestedValue(eleNode, itemConfig(config, declaredSlot, itemType), itemType, checkedAttr, isTagByPropertyName,
                                ignoreTypeInfo, inputType, itemMetadata));
                    }
                }

                return (T) (creatorAndConverter == null ? result : creatorAndConverter._2.apply(result));
            }

            default:
                throw new ParsingException("Unsupported class type: " + ClassUtil.getCanonicalClassName(targetClass)
                        + ". Only object array, collection, map and bean types are supported");
        }
    }

    /**
     * Reads the single structured child of a DOM value wrapper, retaining its schema and construction metadata.
     *
     * @param wrapperNode the enclosing property, key, value or item node
     * @param config the deserialization configuration
     * @param valueType the declared or configured slot schema
     * @param checkedAttr whether enclosing attribute handling has been determined
     * @param isTagByPropertyName whether tags represent property names
     * @param ignoreTypeInfo whether ordinary enclosing type metadata is ignored
     * @param inputType the type originally requested for the document
     * @return {@code null} for a null-marked wrapper, otherwise the decoded value with any declared Optional/Nullable reapplied
     * @throws ParsingException if metadata is disallowed or a wrapper without a null marker does not contain exactly one child
     */
    private Object readNestedValue(final Node wrapperNode, final XmlDeserConfig config, final Type<?> valueType, final boolean checkedAttr,
            final boolean isTagByPropertyName, final boolean ignoreTypeInfo, final Type<?> inputType) throws ParsingException {
        return readNestedValue(wrapperNode, config, valueType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType,
                resolvePresentTypeAttribute(XmlUtil.getAttribute(wrapperNode, XmlConstants.TYPE)));
    }

    /**
     * Same as {@link #readNestedValue(Node, XmlDeserConfig, Type, boolean, boolean, boolean, Type)} for a wrapper whose
     * type attribute the caller has already read and approved: the property, key, value and item readers resolve it
     * once to select the slot's type and pass it down here, where it used to be resolved again.
     *
     * @param wrapperNode the enclosing property, key, value or item node
     * @param config the deserialization configuration
     * @param valueType the declared or configured slot schema
     * @param checkedAttr whether enclosing attribute handling has been determined
     * @param isTagByPropertyName whether tags represent property names
     * @param ignoreTypeInfo whether ordinary enclosing type metadata is ignored
     * @param inputType the type originally requested for the document
     * @param metadata the wrapper's approved type attribute, or {@code null} when it is absent or blank
     * @return {@code null} for a null-marked wrapper, otherwise the decoded value with any declared Optional/Nullable reapplied
     * @throws ParsingException if a wrapper without a null marker does not contain exactly one child
     */
    private Object readNestedValue(final Node wrapperNode, final XmlDeserConfig config, Type<?> valueType, final boolean checkedAttr,
            final boolean isTagByPropertyName, final boolean ignoreTypeInfo, final Type<?> inputType, final Type<?> metadata) throws ParsingException {
        if (Boolean.parseBoolean(XmlUtil.getAttribute(wrapperNode, XmlConstants.IS_NULL))) {
            return null;
        }
        final Type<?> declaredNestedType = optionalElementTypeOrSelf(valueType);
        // Preserve the open wrapper payload while applying configuration, before metadata looks like a declaration.
        valueType = withSlotMapTypes(valueType, withSlotComponent(valueType, refineObjectWrapper(valueType, metadata), config.getElementType()), config);
        final Type<?> nestedType = optionalElementTypeOrSelf(valueType);
        final Object value;
        enterXmlNesting();
        try {
            // The child resolves the wrapper metadata itself. Retain the original container schema for configuration precedence.
            final Type<?> childType = declaredNestedType.isCollection() || declaredNestedType.isMap() || declaredNestedType.isArray() ? declaredNestedType
                    : nestedType;
            value = readByDOMParserBody(checkOneNode(wrapperNode), config, childType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, false, inputType,
                    metadata);
        } finally {
            exitXmlNesting();
        }

        return nestedType == valueType ? value : wrapOptional(valueType, value);
    }

    /**
     * Reads a DOM scalar/container-text value while retaining format and explicit-null handling.
     *
     * @param node the property, key, value or item node
     * @param valueType the selected parsing schema
     * @param propInfo optional bean property metadata
     * @param propName the property name for conversion diagnostics
     * @param config the effective deserialization configuration
     * @return the decoded value
     * @throws ParsingException if the metadata is not approved
     */
    private Object readValueText(final Node node, final Type<?> valueType, final PropInfo propInfo, final String propName, final XmlDeserConfig config)
            throws ParsingException {
        return readValueText(node, valueType, propInfo, propName, config, resolvePresentTypeAttribute(XmlUtil.getAttribute(node, XmlConstants.TYPE)));
    }

    /**
     * Same as {@link #readValueText(Node, Type, PropInfo, String, XmlDeserConfig)} for a node whose type attribute the
     * caller has already read and approved: the property, key, value and item readers resolve it once to select the
     * slot's type and pass it down here, where it used to be resolved again, and on to {@code getPropValue}, which
     * resolved it once more for a null-marked or formatted node.
     *
     * @param node the property, key, value or item node
     * @param valueType the selected parsing schema
     * @param propInfo optional bean property metadata
     * @param propName the property name for conversion diagnostics
     * @param config the effective deserialization configuration
     * @param metadata the node's approved type attribute, or {@code null} when it is absent or blank
     * @return the decoded value
     * @throws ParsingException if the text cannot be read into the slot
     */
    private Object readValueText(final Node node, Type<?> valueType, final PropInfo propInfo, final String propName, final XmlDeserConfig config,
            final Type<?> metadata) throws ParsingException {
        final Type<?> declaredSlot = valueType;
        valueType = refineObjectWrapper(valueType, metadata);
        final Type<?> payload = optionalElementTypeOrSelf(valueType);
        // Arrays included: an attribute-less Object[] text slot must reach the slot-component rule in readValueText,
        // not fall to valueOf with its raw Object component (which read the JSON numbers as Strings).
        if ((metadata != null || payload.isMap() || payload.isCollection() || payload.isArray())
                && !Boolean.parseBoolean(XmlUtil.getAttribute(node, XmlConstants.IS_NULL)) && (propInfo == null || !propInfo.hasFormat)) {
            return readValueText(valueType, declaredSlot, metadata, XmlUtil.getTextContent(node), config);
        }
        return getPropValue(propName, valueType, propInfo, node, metadata);
    }

    /**
     * The map the SAX handler reads a bean-shaped element into when no bean class exists for it in an open slot. Its
     * own class tells it from a {@code <map>} element while it is open; the enclosing element receives a plain
     * {@link LinkedHashMap}.
     */
    private static final class BeanShapedMap extends LinkedHashMap<String, Object> {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Whether an element whose name resolves to no bean class fills an open slot, where it is read as a map of its
     * properties: the slot is undeclared or {@code Object}, and the element is not the root of a document requested
     * as a bean (whose mismatch is reported).
     *
     * @param slotType the slot's declared type, or {@code null}
     * @param isFirstCall whether the element is the document root
     * @param inputType the requested root type
     * @return whether the element is read as a map
     */
    private static boolean isOpenBeanSlot(final Type<?> slotType, final boolean isFirstCall, final Type<?> inputType) {
        return (slotType == null || slotType.isObject()) && !(isFirstCall && inputType.isBean());
    }

    /**
     * Reads a bean-shaped element that has no bean class ({@code <account><name>n</name><age>3</age></account>} in an
     * open slot) as a map of its properties, the way the standard readers read such an element: each property's text
     * or nested value under its name, typed by configuration or by its own approved type attribute. The reader must be
     * positioned on the element's start and is left on its end.
     *
     * @param xmlReader the reader
     * @param config the effective deserialization configuration
     * @param checkedAttr whether an enclosing element already decided the use of name and type attributes
     * @param isTagByPropertyName whether elements are named after their properties
     * @param ignoreTypeInfo whether type attributes are ignored
     * @param inputType the requested root type
     * @return the map of properties
     * @throws XMLStreamException if advancing the reader fails
     * @throws ParsingException if a property cannot be read
     */
    private Map<String, Object> readBeanShapedElementAsMap(final XMLStreamReader xmlReader, final XmlDeserConfig config, boolean checkedAttr,
            boolean isTagByPropertyName, boolean ignoreTypeInfo, final Type<?> inputType) throws XMLStreamException, ParsingException {
        if (!checkedAttr) {
            isTagByPropertyName = Strings.isEmpty(getAttribute(xmlReader, XmlConstants.NAME));
            ignoreTypeInfo = Strings.isEmpty(getAttribute(xmlReader, XmlConstants.TYPE));
            checkedAttr = true;
        }

        final boolean hasPropTypes = config.hasValueTypes();
        final String elementName = xmlReader.getLocalName();
        final Map<String, Object> map = new LinkedHashMap<>();

        while (xmlReader.hasNext()) {
            final int event = xmlReader.next();

            if (isTextEvent(event) && map.isEmpty() && !xmlReader.isWhiteSpace()) {
                // Text instead of properties: the element is no bean-shaped element, so its unknown name is reported as before.
                throw noBeanClassFound(elementName, inputType);
            } else if (event == XMLStreamConstants.START_ELEMENT) {
                final String name = isTagByPropertyName ? xmlReader.getLocalName() : getAttribute(xmlReader, XmlConstants.NAME);

                if (name == null) {
                    throw new ParsingException("Missing '" + XmlConstants.NAME + "' attribute on XML element: " + xmlReader.getLocalName());
                }

                // Approved even when type information is ignored, as for a bean's property.
                final Type<?> attributeType = resolvePresentTypeAttribute(getAttribute(xmlReader, XmlConstants.TYPE));
                final Type<?> configured = hasPropTypes ? config.getValueType(name) : null;
                final Type<?> valueType = configured != null ? configured : defaultValueType;

                map.put(name, readWrappedValue(xmlReader, config, valueType, valueType, ignoreTypeInfo ? null : attributeType, checkedAttr, isTagByPropertyName,
                        ignoreTypeInfo, inputType));
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return map;
            }
            // text between properties is ignored, as inside a bean
        }

        throw new ParsingException("Unexpected end of XML stream inside element: " + xmlReader.getLocalName());
    }

    /**
     * DOM counterpart of {@link #readBeanShapedElementAsMap}.
     *
     * @param node the bean-shaped element
     * @param config the effective deserialization configuration
     * @param checkedAttr whether an enclosing element already decided the use of name and type attributes
     * @param isTagByPropertyName whether elements are named after their properties
     * @param ignoreTypeInfo whether type attributes are ignored
     * @param inputType the requested root type
     * @return the map of properties
     * @throws ParsingException if a property cannot be read
     */
    private Map<String, Object> readBeanShapedNodeAsMap(final Node node, final XmlDeserConfig config, boolean checkedAttr, boolean isTagByPropertyName,
            boolean ignoreTypeInfo, final Type<?> inputType) throws ParsingException {
        if (!checkedAttr) {
            isTagByPropertyName = Strings.isEmpty(XmlUtil.getAttribute(node, XmlConstants.NAME));
            ignoreTypeInfo = Strings.isEmpty(XmlUtil.getAttribute(node, XmlConstants.TYPE));
            checkedAttr = true;
        }

        if (XmlUtil.isTextElement(node) && Strings.isNotBlank(XmlUtil.getTextContent(node))) {
            // Text instead of properties: the element is no bean-shaped element, so its unknown name is reported as before.
            throw noBeanClassFound(localName(node), inputType);
        }

        final boolean hasPropTypes = config.hasValueTypes();
        final Map<String, Object> map = new LinkedHashMap<>();
        final NodeList propNodes = node.getChildNodes();

        for (int i = 0; i < propNodes.getLength(); i++) {
            final Node propNode = propNodes.item(i);

            if (propNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            final String name = isTagByPropertyName ? localName(propNode) : XmlUtil.getAttribute(propNode, XmlConstants.NAME);

            if (name == null) {
                throw new ParsingException("Missing '" + XmlConstants.NAME + "' attribute on XML element: " + propNode.getNodeName());
            }

            final Type<?> configured = hasPropTypes ? config.getValueType(name) : null;
            final Type<?> valueType = configured != null ? configured : defaultValueType;

            map.put(name, XmlUtil.isTextElement(propNode) ? readValueText(propNode, valueType, null, name, config)
                    : readNestedValue(propNode, config, valueType, checkedAttr, isTagByPropertyName, ignoreTypeInfo, inputType));
        }

        return map;
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
            throw noBeanClassFound(nodeName, inputClass);
        }
    }

    private static ParsingException noBeanClassFound(final String nodeName, final Type<?> inputType) {
        return noBeanClassFound(nodeName, inputType.javaType());
    }

    private static ParsingException noBeanClassFound(final String nodeName, final Class<?> inputClass) {
        return new ParsingException("No bean class found for node name: " + nodeName + " in package of class: " + inputClass.getCanonicalName());
    }

    /**
     * Classifies an XML element by its name and the kind of its parent element.
     *
     * @param nodeName the element name
     * @param previousNodeType the node type of the enclosing element
     * @return {@code PROPERTY} for any child of a bean element, {@code ELEMENT} for any child of an array or
     *         collection element; otherwise the type reserved for the element name (array, list/set/collection,
     *         map, entry, key, value, e), or {@code ENTITY} when the name is not reserved
     */
    private static NodeType getNodeType(final String nodeName, final NodeType previousNodeType) {
        if (previousNodeType == NodeType.ENTITY) {
            return NodeType.PROPERTY;
        }

        // Every child of an array/collection is an item wrapper, whatever its name, exactly as the StAX and DOM readers
        // treat it. Classified by name instead, a non-<e> child such as <list><c isNull="true"/></list> became a bean that
        // no ELEMENT end ever added to the collection: silently dropped, misread, or failing with an IAE/NPE.
        if (previousNodeType == NodeType.COLLECTION || previousNodeType == NodeType.ARRAY) {
            return NodeType.ELEMENT;
        }

        final NodeType nodeType = nodeTypePool.get(nodeName);

        if (nodeType == null) {
            return NodeType.ENTITY;
        }

        return nodeType;
    }

    /**
     * Resolves the bean class whose simple name matches an XML element name, searching the package of
     * {@code targetClass} (or, for {@code java.lang}/{@code java.util} classes and classes without a package, the package
     * of the outermost call-stack class - the one nearest the thread's entry point - outside {@code java.lang},
     * {@code java.util} and this parser's package).
     * Results - including misses - are cached per {@code targetClass}, with at most 256 names retained.
     * Resolved classes are weakly referenced; an evicted name or collected class is resolved again on demand.
     *
     * @param <T> the resolved class type
     * @param nodeName the element name to resolve; matched case-insensitively, and retried against the
     *        normalized form of the name
     * @param targetClass the class whose package anchors the search; {@code null} yields {@code null}
     * @return the matching class, or {@code null} if no class in the searched package matches or that package
     *         cannot be scanned
     */
    @SuppressWarnings({ "unchecked", "deprecation", "null" })
    private static <T> Class<T> getClassByNodeName(final String nodeName, final Class<?> targetClass) {
        if (targetClass == null) {
            return null;
        }

        Class<?> nodeClass = null;
        final Map<String, WeakReference<Class<?>>> nodeNameClassMap = nodeNameClassMapPool.get(targetClass);
        final WeakReference<Class<?>> cachedClass = nodeNameClassMap.get(nodeName);
        nodeClass = cachedClass == null ? null : cachedClass.get();

        if (nodeClass == null) {
            String packName = null;

            if (targetClass.getPackage() == null || targetClass.getPackage().getName().startsWith("java.lang")
                    || targetClass.getPackage().getName().startsWith("java.util")) {
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
                packName = targetClass.getPackage().getName();
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
                nodeClass = getClassByNodeName(Beans.normalizePropName(nodeName), targetClass);
            }

            if (nodeClass == null) {
                nodeClass = ClassUtil.SENTINEL_CLASS;
            }

            // Resolve outside the lock. All writes share this lock so eviction and insertion keep a strict
            // bound under concurrent misses, without making cache hits wait or clearing the entire cache.
            synchronized (nodeNameClassMap) {
                if (!nodeNameClassMap.containsKey(nodeName) && nodeNameClassMap.size() >= 256) {
                    nodeNameClassMap.remove(nodeNameClassMap.keySet().iterator().next());
                }

                nodeNameClassMap.put(nodeName, new WeakReference<>(nodeClass));
            }
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

        // Parallel to nodeTypeQueue: the declared Optional/Nullable type of a key/value/item wrapper whose nested child is read with
        // the wrapper's element type (see startElement), so that its end wraps the child's value again; null otherwise.
        private final List<Type<?>> optionalWrapperQueue = new ArrayList<>();

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

        // The active key/value/item wrapper's construction hint; consumed when its single child starts.
        private Type<?> valueMetadata;

        // The active key/value/item/property schema before metadata refinement: remembered so an originally open
        // Object slot stays open to global element configuration in readValueText.
        private Type<?> declaredSlotType;

        private Type<?> eleType;

        private Type<?> keyType;

        private Type<?> valueType;

        private final List<Type<?>> eleTypeQueue = new ArrayList<>();

        /**
         * Original schema of a text-only collection, which ends before another collection can replace this reference.
         * Collections with parsed children are nonempty and do not take the JSON-text path.
         */
        private Type<?> collectionTextDeclaredType;
        /** The element component each open {@code <array>} converts its JSON text with (see {@code readArrayText}). */
        private final List<Type<?>> arrayTextComponentQueue = new ArrayList<>();
        /** Whether each open item was typed by the element configuration, and the configurations replaced for such items. */
        private final List<Boolean> configuredItemQueue = new ArrayList<>();
        /** Whether the items of each open array or collection are configured container components (pushed with eleTypeQueue). */
        private final List<Boolean> configuredItemsQueue = new ArrayList<>();
        private final List<XmlDeserConfig> configQueue = new ArrayList<>();

        private final List<Type<?>> keyTypeQueue = new ArrayList<>();

        private final List<Type<?>> valueTypeQueue = new ArrayList<>();

        private final IdentityHashMap<Object, BeanInfo> beanInfoQueue = new IdentityHashMap<>(1);

        // Mutable stand-ins created for Immutable* map/collection targets, keyed by identity, with the converter that wraps
        // them once their element closes (see finishContainer).
        private final IdentityHashMap<Object, Function<Object, Object>> containerConverters = new IdentityHashMap<>(1);

        private boolean isNull = false;

        private boolean checkedPropNameTag = false;

        private boolean checkedTypeInfo = false;

        private boolean isTagByPropertyName = false;

        private boolean ignoreTypeInfo = false;

        /**
         * The depth (open-element count) of the bean element that decided {@code checkedPropNameTag}, {@code checkedTypeInfo},
         * {@code isTagByPropertyName} and {@code ignoreTypeInfo}, or {@code -1}.
         */
        private int typeInfoDecisionDepth = -1;

        /** Whether the current element's type attribute has already been checked against the approval policy. */
        private boolean typeAttributeChecked = false;

        /** Whether a value type is configured for the current bean property: configuration, not metadata, then types its text. */
        private boolean propConfigured = false;

        /**
         * The type the text of the current map value is read with when an explicit Object value-type override keeps the bean
         * class its type attribute names for a nested element only (the open Object type), or {@code null}.
         */
        private Type<?> overrideTextType = null;

        /**
         * The current element's resolved type attribute ({@code null} when absent or blank), valid once
         * {@code elementTypeAttributeResolved} is set; see {@link #typeAttributeOf}.
         */
        private Type<?> elementTypeAttribute;

        private boolean elementTypeAttributeResolved = false;

        private boolean isFirstCall = true;

        private int inIgnorePropRefCount = 0;

        // Set when the root element carries isNull="true": the whole document is skipped and the result stays null.
        private boolean rootIsNull = false;

        /** Depth of the null subtree being skipped after the marked element's own metadata is approved. */
        private int inNullValueRefCount = 0;

        /** Whether the skipped structured value has no frame to pop, unlike a null property/item/key/value wrapper. */
        private boolean nullStructuredValue = false;

        /** Whether the elements being skipped are the rest of an entry whose key is ignored, and whether its value was seen. */
        private boolean inIgnoredEntry = false;
        private boolean ignoredEntryValueSeen = false;

        // keyQueue size at the start of each open <entry>, so a missing/duplicate key or value is detected per entry.
        private final List<Integer> entryKeyMarkQueue = new ArrayList<>();

        /** Per open {@code <entry>}: whether its {@code <value>} has been read (exactly one key and one value are allowed). */
        private final List<Boolean> entryValueSeenQueue = new ArrayList<>();

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
            if (inNullValueRefCount > 0) {
                inNullValueRefCount++;
                return;
            }
            if (inIgnorePropRefCount > 0) {
                // The rest of an entry whose key is ignored is skipped, but the entry must still be well formed, as the StAX
                // and DOM readers require: exactly one <value> follows the key.
                if (inIgnoredEntry && inIgnorePropRefCount == 1) {
                    if (ignoredEntryValueSeen || !XmlConstants.VALUE.equals(Strings.isEmpty(localName) ? qName : localName)) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY);
                    }

                    ignoredEntryValueSeen = true;
                }

                inIgnorePropRefCount++;

                return;
            }

            typeAttributeChecked = false;
            elementTypeAttributeResolved = false;
            elementTypeAttribute = null;
            overrideTextType = null;

            if (sb == null) {
                sb = Objectory.createStringBuilder();
            } else {
                sb.setLength(0);
            }

            // The parser is namespace-aware: match on the local name so a prefixed document (<p:name>) resolves
            // the same properties as the StAX reader does.
            nodeName = Strings.isEmpty(localName) ? qName : localName;
            final NodeType previousNodeType = nodeTypeQueue.isEmpty() ? null : nodeTypeQueue.get(nodeTypeQueue.size() - 1);
            final NodeType nodeType = getNodeType(nodeName, previousNodeType);
            final Type<?> wrapperMetadata = valueMetadata;
            valueMetadata = null;

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
                final int parentIndex = nodeTypeQueue.size() - 1;
                final NodeType parentNodeType = parentIndex < 0 ? null : nodeTypeQueue.get(parentIndex);

                // A nested element in an Optional/Nullable key/value/item slot is the value the wrapper holds (the writers unwrap
                // it): read it with the element type, as the StAX/DOM readers do, and wrap it again when the slot ends.
                if ((parentNodeType == NodeType.KEY || parentNodeType == NodeType.VALUE || parentNodeType == NodeType.ELEMENT
                        || parentNodeType == NodeType.PROPERTY) && propType != null && propType.isOptionalOrNullable() && propType.elementType() != null) {
                    optionalWrapperQueue.set(parentIndex, propType);
                    propType = propType.elementType();
                    // The bean property's argument describes the wrapper payload, not the payload's elements.
                    // The held type now supplies that schema; the property is looked up again when its wrapper ends.
                    propInfo = null;
                }

                if (propType == null || propType.isString() || propType.isObject()) {
                    targetType = hasPropTypes ? config.getValueType(nodeName, null) : null;
                } else {
                    targetType = propType;
                }
            }

            final Type<?> declaredTargetType = targetType;
            // The metadata-refined schema, kept beside the declared array that openArrayDestination may restore below.
            Type<?> refinedTargetType = targetType;
            if (nodeType == NodeType.MAP || nodeType == NodeType.COLLECTION || nodeType == NodeType.ARRAY) {
                // Resolve the child's complete metadata before Object falls back to the node-kind default. A wrapper
                // can supply the implementation too, but neither location may overwrite the declared parsing schema.
                final Type<?> metadata = xmlParser.resolvePresentTypeAttribute(attrs == null ? null : attrs.getValue(XmlConstants.TYPE));
                typeAttributeChecked = true;
                targetType = targetType == null ? objType : targetType;
                targetClass = targetType.javaType();
                if (!(checkedTypeInfo && ignoreTypeInfo)) {
                    if (wrapperMetadata != null) {
                        targetClass = getConcreteClass(wrapperMetadata.javaType(), targetClass);
                        targetType = resolveItemType(targetType, targetClass, wrapperMetadata);
                    }
                    targetClass = getConcreteClass(metadata == null ? null : metadata.javaType(), targetClass);
                    targetType = resolveItemType(targetType, targetClass, metadata);
                    if (targetClass == Object.class && (targetType.isMap() || targetType.isCollection() || targetType.isArray())) {
                        // An open Object slot adopted the metadata's container schema (an uninstantiable container
                        // as its open mutable schema); construction follows the adopted class, where
                        // getConcreteClass could only fall back to Object for an uninstantiable attribute class.
                        targetClass = targetType.javaType();
                    }
                }
                // Array metadata must not close an originally open array slot: configuration types the elements
                // while the slot's own declared runtime (Object[]) is constructed, as on the StAX/DOM readers.
                // The construction class follows the restored type, as the containerReadType resync would otherwise
                // rebuild the metadata's array class.
                refinedTargetType = targetType;
                final Type<?> openSlotType = openArrayDestination(declaredTargetType, targetType, config.getElementType());
                if (openSlotType != targetType) {
                    targetType = openSlotType;
                    targetClass = targetType.javaType();
                }
            } else if (targetType != null) {
                targetClass = targetType.javaType();

                targetClass = checkedTypeInfo
                        ? ((ignoreTypeInfo || attrs == null || attrs.getLength() == 0) ? targetClass
                                : getConcreteClass(typeAttributeClassOf(attrs), targetClass))
                        : ((attrs == null || attrs.getLength() == 0) ? targetClass : getConcreteClass(typeAttributeClassOf(attrs), targetClass));

                if (!targetType.javaType().equals(targetClass)) {
                    targetType = containerReadType(targetType, targetClass);
                }
            }

            if (previousNodeType == NodeType.PROPERTY || previousNodeType == NodeType.ELEMENT || previousNodeType == NodeType.KEY
                    || previousNodeType == NodeType.VALUE) {
                // Reject a second direct child before it can overwrite the completed value. Children inside
                // the bean/collection/map itself have a different parent frame and remain unrestricted.
                if (valueChildSeenQueue.set(nodeTypeQueue.size() - 1, true)) {
                    throw new ParsingException("Unexpected element <" + nodeName + "> after the value of an XML value wrapper");
                }
            }

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

            // Likewise an <entry> may only hold its <key> and <value>: a bean element directly inside it was built and then
            // dropped, and the entry silently lost, where the StAX and DOM readers report the document as malformed.
            if (previousNodeType == NodeType.ENTRY && nodeType != NodeType.KEY && nodeType != NodeType.VALUE) {
                throw new ParsingException(MALFORMED_MAP_ENTRY);
            }

            // And <entry> belongs directly in a map, <key>/<value> directly in an <entry>: inside an item or a value they used
            // the state of the enclosing entry (its key was put into the outer map) and failed with a NullPointerException.
            if (nodeType == NodeType.ENTRY && previousNodeType != NodeType.MAP) {
                throw new ParsingException("Element <" + nodeName + "> is only allowed inside a map element");
            }

            if ((nodeType == NodeType.KEY || nodeType == NodeType.VALUE) && previousNodeType != NodeType.ENTRY) {
                throw new ParsingException(MALFORMED_MAP_ENTRY);
            }

            // Likewise an item wrapper belongs directly in an array/collection: an <e> inside an <e> was added to the enclosing
            // collection and its end failed with a NullPointerException.
            if (nodeType == NodeType.ELEMENT && previousNodeType != NodeType.COLLECTION && previousNodeType != NodeType.ARRAY) {
                throw new ParsingException("Element <" + nodeName + "> is only allowed inside an array or collection element");
            }

            if (isNull && (nodeType == NodeType.ENTITY || nodeType == NodeType.MAP || nodeType == NodeType.ARRAY || nodeType == NodeType.COLLECTION)) {
                // The parent wrapper already records that its child was seen. Do not construct a value
                // or push container frames for null; its close supplies a null child to that wrapper.
                if (!typeAttributeChecked) {
                    typeAttributeOf(attrs);
                }
                inNullValueRefCount = 1;
                nullStructuredValue = true;
                return;
            }

            // Whether the element is a map value whose configured value type is an explicit Object override (see case VALUE).
            boolean explicitObjectOverride = false;

            switch (nodeType) {
                case ENTITY: {
                    // Whether the element is bean-shaped without a class and read as a map (see BeanShapedMap).
                    boolean beanShapedElement = false;

                    if (!checkedPropNameTag) {
                        isTagByPropertyName = (attrs == null) || (Strings.isEmpty(attrs.getValue(XmlConstants.NAME)));
                        ignoreTypeInfo = (attrs == null) || (Strings.isEmpty(attrs.getValue(XmlConstants.TYPE)));
                        checkedPropNameTag = true;
                        checkedTypeInfo = true;
                        typeInfoDecisionDepth = nodeTypeQueue.size();
                    }

                    if (!isTagByPropertyName) {
                        //noinspection DataFlowIssue
                        beanOrPropName = attrs.getValue(XmlConstants.NAME);
                        beanOrPropNameQueue.add(beanOrPropName);
                    }

                    targetType = hasPropTypes ? config.getValueType(beanOrPropName, targetType) : targetType;

                    Type<?> attributeBeanType = null;

                    if (targetType == null || !targetType.isBean()) {
                        if ((eleType != null) && eleType.isBean()) {
                            targetType = eleType;
                        } else if ((attributeBeanType = objectSlotBeanType(attrs, propType)) != null) {
                            targetType = attributeBeanType;
                        } else {
                            if (inputType.isBean() && ClassUtil.getSimpleClassName(inputType.javaType()).equalsIgnoreCase(beanOrPropName)) {
                                targetType = inputType;
                            } else {
                                final Class<?> classByNodeName;

                                if (inputType.isCollection() || inputType.isArray() || inputType.isMap()) {
                                    // Must match the StAX/DOM siblings: without a configured element type,
                                    // fall back to node-name discovery instead of leaving targetType null. A bean ROOT is
                                    // rejected, as by those readers: discovery through the configured element type
                                    // returned that bean where a List or Map was requested.
                                    // The search starts at the slot's declared class when it is more specific than Object (an
                                    // interface component type of an array root), as the StAX reader's does.
                                    classByNodeName = isFirstCall ? null
                                            : getClassByNodeName(beanOrPropName, propType != null && propType.javaType() != Object.class ? propType.javaType()
                                                    : config.getElementType() != null ? config.getElementType().javaType() : inputType.javaType());
                                } else {
                                    classByNodeName = getClassByNodeName(beanOrPropName, inputType.javaType());
                                }

                                // propType is the slot's own type (a map value's, an item's): the element type of an enclosing
                                // list says nothing about a bean-shaped element nested in a map value inside its items.
                                if (classByNodeName == null && isOpenBeanSlot(propType, isFirstCall, inputType)) {
                                    // No class for the element: an open slot reads the bean-shaped element as a map of its
                                    // properties, as the StAX and DOM readers and the standard readers do.
                                    beanShapedElement = true;
                                    targetType = defaultValueType;
                                } else {
                                    // Check before Type.of: a failed discovery must raise the descriptive
                                    // ParsingException, not Type.of(null)'s IllegalArgumentException.
                                    checkBeanType(classByNodeName, nodeName, inputType.javaType());

                                    targetType = Type.of(classByNodeName);
                                }
                            }
                        }

                        targetClass = targetType.javaType();
                    }

                    final boolean unknownWrapper = !isFirstCall && propType != null && propType.isBean()
                            && !(hasPropTypes && config.getValueType(beanOrPropName) != null) && !isKnownBeanWrapper(beanOrPropName, propType, targetClass);

                    if (beanShapedElement) {
                        // The element's properties become the entries of the sink; no BeanInfo is queued for it, which is
                        // how the PROPERTY start and end and popupNodeValue tell it from a bean.
                        final BeanShapedMap sink = new BeanShapedMap();
                        beanInfo = null;
                        beanClass = null;
                        bean = sink;
                        eleType = null;
                        nodeValueQueue.add(sink);

                        if (isFirstCall) {
                            resultHolder.setValue((T) sink);
                            isFirstCall = false;
                        }

                        propInfo = null;
                        propType = null;

                        break;
                    }

                    beanClass = targetClass;
                    beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
                    // eleType belongs to the enclosing array/collection and is restored by popupNodeValue when this bean
                    // closes; left set, it typed every nested bean/map/collection reached through an Object property.
                    eleType = null;

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
                    // A container does not decide whether type attributes are used: as on the StAX and DOM readers, the first bean
                    // element outside an already deciding bean does (see the ENTITY end), whatever the container carries.

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
                        if (config.getMapKeyType() != null && !config.getMapKeyType().isObject() && !keepsRequestedContainer(declaredTargetType, 0)) {
                            keyType = config.getMapKeyType();
                        } else if (targetType.isMap() && !targetType.parameterTypes().get(0).isObject()) {
                            keyType = targetType.parameterTypes().get(0);
                        } else {
                            keyType = defaultKeyType;
                        }
                    }

                    if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 2 && !propInfo.jsonXmlType.parameterTypes().get(1).isObject()) {
                        // A value declared as an abstract scalar (Map<String, Number>) adopts the value the map's metadata named
                        // (HashMap<String, Long>), as compact text reads it; any other declared value is kept (narrowedArgument).
                        valueType = narrowedArgument(propInfo.jsonXmlType.parameterTypes().get(1),
                                targetType.isMap() && targetType.parameterTypes().size() == 2 ? targetType.parameterTypes().get(1) : null);
                    } else if (propType != null && propType.parameterTypes().size() == 2 && propType.isMap() && !propType.parameterTypes().get(1).isObject()) {
                        valueType = propType.parameterTypes().get(1);
                    } else {
                        if (config.getMapValueType() != null && !config.getMapValueType().isObject() && !keepsRequestedContainer(declaredTargetType, 1)) {
                            valueType = config.getMapValueType();
                        } else if (targetType.isMap() && !targetType.parameterTypes().get(1).isObject()) {
                            valueType = targetType.parameterTypes().get(1);
                        } else {
                            valueType = defaultValueType;
                        }
                    }

                    keyTypeQueue.add(keyType);
                    valueTypeQueue.add(valueType);
                    // As for a bean: an enclosing collection's element type must not type the values nested in this map.
                    eleType = null;

                    // Same creation as the StAX/DOM readers: Immutable* via a mutable stand-in (wrapped in finishContainer),
                    // EnumMap keyed by its enum.
                    final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> mapCreatorAndConverter = mapCreatorAndConverter(targetClass, valueType,
                            scalarOverrides(config));

                    if (EnumMap.class.isAssignableFrom(targetClass)) {
                        map = newEnumMap(targetClass, keyType, XmlDeserConfig.class);
                    } else if (mapCreatorAndConverter != null) {
                        map = (Map<Object, Object>) mapCreatorAndConverter._1.apply(targetClass);
                        containerConverters.put(map, mapCreatorAndConverter._2);
                    } else {
                        map = xmlParser.newPropInstance(targetClass, attrs);
                    }

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
                    // A container does not decide whether type attributes are used (see MAP above).

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
                    } else if (!untypedArray && targetType.elementType() != null && !targetType.elementType().isObject()) {
                        // A declared array component is reified: it beats global element configuration, as on the
                        // StAX/DOM readers (config here made <e> children Integers that Long[] could not hold).
                        eleType = targetType.elementType();
                    } else {
                        // The configured component types the open elements once, never a configured item's own elements.
                        if (elementConfigurationApplies(config, null)) {
                            eleType = config.getElementType();
                        } else {
                            // must match the StAX/DOM siblings: forcing String for an Object[] target made the
                            // String[] placeholder drive the final conversion and dropped per-element type info.
                            eleType = targetType.isArray() ? targetType.elementType() : strType;
                        }
                    }

                    if (declaredTargetType != null && declaredTargetType.isArray() && !untypedArray) {
                        // Metadata never narrows a declared array class (the placeholder below is the declared array); its
                        // refined component, or the configured component of an open slot, still types the items.
                        final Type<?> itemOverride = arrayItemType(declaredTargetType, refinedTargetType, config.getElementType());
                        if (itemOverride != null) {
                            eleType = itemOverride;
                        }
                    }

                    eleTypeQueue.add(eleType);
                    configuredItemsQueue.add(isConfiguredItem(config, declaredTargetType, eleType));
                    // The component the array's JSON text converts its elements with (see readArrayText), popped at the end.
                    arrayTextComponentQueue.add(declaredTargetType != null && declaredTargetType.isArray() && !untypedArray
                            ? arrayComponentOverride(declaredTargetType, refinedTargetType, config.getElementType())
                            : textComponent(eleType, targetType));

                    // The shared placeholder marks an <array> without a declared type, so endElement can infer the
                    // array type from the elements (see inferArrayType). A typed array keeps its DECLARED component:
                    // configuration types the elements, never the reified array type (a configured Long element type
                    // must not turn a declared Object[] into a Long[], nor a Long[] value into an Integer[]).
                    array = untypedArray && eleType == strType ? UNTYPED_ARRAY_PLACEHOLDER
                            : N.newArray(untypedArray ? eleType.javaType() : targetType.javaType().getComponentType(), 0);
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
                    // A container does not decide whether type attributes are used (see MAP above).
                    collectionTextDeclaredType = isFirstCall ? null : declaredTargetType;

                    if (targetType == null || !targetType.isCollection()) {
                        if ((eleType != null) && Collection.class.isAssignableFrom(eleType.javaType())) {
                            targetType = eleType;
                        } else {
                            targetType = listType;
                        }

                        targetClass = targetType.javaType();
                    }

                    if (propInfo != null && propInfo.jsonXmlType.parameterTypes().size() == 1 && !propInfo.jsonXmlType.parameterTypes().get(0).isObject()) {
                        // An element declared as an abstract scalar (List<Number>) adopts the element the collection's metadata
                        // named (ArrayList<Long>), as the StAX and DOM readers and compact text read it (narrowedArgument).
                        eleType = narrowedArgument(propInfo.jsonXmlType.parameterTypes().get(0), targetType.isCollection() ? targetType.elementType() : null);
                    } else if (propType != null && propType.parameterTypes().size() == 1 && Collection.class.isAssignableFrom(propType.javaType())
                            && !propType.parameterTypes().get(0).isObject()) {
                        // The container's metadata may name a specialized item (a LinkedMultiValueMap in a declared
                        // List<Map<String, Object>>): child elements construct it as compact text does.
                        eleType = refinedItemType(propType.parameterTypes().get(0), targetType, config.getElementType());
                    } else {
                        // The configured component types the open elements once (never a configured item's own elements), and a
                        // requested container element (List<Object[]> at the root) keeps its shape with the configuration inside it.
                        if (elementConfigurationApplies(config, declaredTargetType)) {
                            eleType = config.getElementType();
                        } else if (targetType.elementType() != null && !targetType.elementType().isObject()) {
                            eleType = targetType.elementType();
                        } else {
                            eleType = defaultValueType;
                        }
                    }

                    eleTypeQueue.add(eleType);
                    configuredItemsQueue.add(isConfiguredItem(config, declaredTargetType, eleType));

                    // Same creation as the StAX/DOM readers: Immutable* via a mutable stand-in, wrapped in finishContainer.
                    final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> collCreatorAndConverter = collectionCreatorAndConverter(targetClass,
                            true);

                    if (collCreatorAndConverter != null) {
                        coll = (Collection<Object>) collCreatorAndConverter._1.apply(targetClass);
                        containerConverters.put(coll, collCreatorAndConverter._2);
                    } else {
                        coll = xmlParser.newPropInstance(targetClass, attrs);
                    }

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

                    if (beanInfo == null) {
                        // A property of a bean-shaped element read as a map (see BeanShapedMap): an open entry, typed by
                        // configuration or by its own approved type attribute.
                        propInfo = null;
                        final Type<?> configured = hasPropTypes ? config.getValueType(beanOrPropName) : null;
                        final Type<?> attributeType = checkedTypeInfo && ignoreTypeInfo ? null : typeAttributeOf(attrs);
                        propType = configured != null ? configured : attributeType != null ? attributeType : defaultValueType;
                        declaredSlotType = propType;

                        break;
                    }

                    propInfo = beanInfo.getPropInfo(beanOrPropName);
                    ignoredClassPropNames = config.getIgnoredPropNames(beanClass);

                    // Test the resolved property name too: getPropInfo also matches case and snake-case variants.
                    if (N.notEmpty(ignoredClassPropNames)
                            && (ignoredClassPropNames.contains(beanOrPropName) || (propInfo != null && ignoredClassPropNames.contains(propInfo.name)))) {
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

                    // keep the declared (parameterized) property type when the attribute names its class.
                    if (hasPropTypes) {
                        propType = config.getValueType(beanOrPropName);
                        propConfigured = propType != null;

                        if (propType == null) {
                            propType = ignoreTypeInfo ? propInfo.jsonXmlType
                                    : keepDeclaredType(propInfo.jsonXmlType, getConcreteClass(typeAttributeClassOf(attrs), propInfo.clazz));
                        }
                    } else {
                        propConfigured = false;
                        propType = ignoreTypeInfo ? propInfo.jsonXmlType
                                : keepDeclaredType(propInfo.jsonXmlType, getConcreteClass(typeAttributeClassOf(attrs), propInfo.clazz));
                    }

                    // Array metadata must not close an originally open array slot: configuration types the elements
                    // while the property's own declared runtime (Object[]) is constructed.
                    propType = openArrayDestination(propInfo.jsonXmlType, propType, config.getElementType());

                    if ((propType == null) || propType.javaType() == Object.class) {
                        propType = defaultValueType;
                    }

                    break;
                }

                case ELEMENT: {
                    if (eleType == null || coll == null) {
                        throw new ParsingException("Element <" + nodeName + "> is only allowed inside an array or collection element");
                    }

                    // An item the element configuration typed is read without it (see itemConfig): the component's own
                    // elements are never typed again. The configuration is restored when the item ends.
                    final boolean configuredItem = !configuredItemsQueue.isEmpty() && configuredItemsQueue.get(configuredItemsQueue.size() - 1);
                    configuredItemQueue.add(configuredItem);
                    if (configuredItem) {
                        configQueue.add(config);
                        config = itemConfig(config, null, eleType);
                    }

                    // One resolution of the item's type attribute serves its concrete class, its item type and the approval
                    // check at the end of startElement (resolving the attribute at each use parsed it two or three times).
                    propType = resolveItemType(eleType, getConcreteClass(typeAttributeClassOf(attrs), eleType.javaType()), typeAttributeOf(attrs));

                    if ((propType == null) || propType.javaType() == Object.class) {
                        propType = defaultValueType;
                    } else if (propType.isString() && nodeValueQueue.size() >= 2
                            && nodeValueQueue.get(nodeValueQueue.size() - 2) == UNTYPED_ARRAY_PLACEHOLDER) {
                        // An untyped array's String placeholder types its text items only: a structured item is open, as on
                        // the StAX and DOM readers (its text still reads as a String).
                        propType = defaultValueType;
                    }

                    break;
                }

                case KEY: {
                    // a second key after the entry's value was put
                    if (keyType == null || entryKeyMarkQueue.isEmpty() || entryValueSeenQueue.get(entryValueSeenQueue.size() - 1)) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY);
                    }

                    // keep the declared key type (the enum key of an EnumMap) when the attribute names its class.
                    propType = ignoreTypeInfo ? keyType : keepDeclaredType(keyType, getConcreteClass(typeAttributeClassOf(attrs), keyType.javaType()));

                    if ((propType == null) || propType.javaType() == Object.class) {
                        propType = defaultKeyType;
                    }

                    break;
                }

                case VALUE: {
                    checkEntryHasKey();

                    // a map value written with type info carries its RAW runtime type
                    // (`type="java.util.EnumMap<Object, Object>"`, `type="ImmutableList<Object>"`); Type.of(concrete class) dropped
                    // the key enum of a declared EnumMap<K, V> value and the element type of a declared ImmutableList<E> value.
                    Type<?> configuredValueType = null;
                    if (hasPropTypes) {
                        final Object key = keyQueue.get(keyQueue.size() - 1);
                        if (key != null) {
                            // Named value configuration applies to any key by its text, as on the other readers and in
                            // JSON, and a scalar type on a declared list value names its element type (elementOverride).
                            propType = elementOverride(targetClass, valueType, config.getValueType(N.toString(key)));
                            configuredValueType = propType;
                            explicitObjectOverride = propType != null && propType.javaType() == Object.class;

                            if (propType == null) {
                                propType = ignoreTypeInfo ? valueType
                                        : keepDeclaredType(valueType, getConcreteClass(typeAttributeClassOf(attrs), valueType.javaType()));
                            }
                        } else {
                            propType = ignoreTypeInfo ? valueType
                                    : keepDeclaredType(valueType, getConcreteClass(typeAttributeClassOf(attrs), valueType.javaType()));
                        }
                    } else {
                        propType = ignoreTypeInfo ? valueType
                                : keepDeclaredType(valueType, getConcreteClass(typeAttributeClassOf(attrs), valueType.javaType()));
                    }

                    // Array metadata must not close an originally open array slot: configuration types the elements
                    // while the map's own declared value runtime (Object[]) is constructed. A configured type is the
                    // entry's declaration: a configured String[] is constructed, not the map's Integer[].
                    propType = openArrayDestination(configuredValueType != null ? configuredValueType : valueType, propType, config.getElementType());

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
                    entryValueSeenQueue.add(Boolean.FALSE);

                    break;
                }

                default:
                    throw new ParsingException("only array, collection, map and bean nodes are supported: " + nodeName); //NOSONAR
            }

            if (isFirstCall) {
                throw new ParsingException("only array, collection, map and bean nodes are supported: " + nodeName);
            }

            // The declared slot is remembered before metadata refinement: an originally open Object slot stays
            // open to global element configuration when the value's text is read (readValueText).
            declaredSlotType = propType;

            // Ignored properties remain opaque. The Object-slot bean path already checked this element's type attribute;
            // other paths still need this final approval check (reusing the type they resolved above, if any).
            if (inIgnorePropRefCount == 0 && !typeAttributeChecked) {
                final Type<?> attributeType = typeAttributeOf(attrs);
                final Type<?> payloadType = optionalElementTypeOrSelf(propType);
                // Container children refine metadata themselves. Keep their original open slots until configuration
                // precedence is decided; text-only wrappers refine in readValueText instead.
                if (payloadType == null || !(payloadType.isCollection() || payloadType.isMap() || payloadType.isArray())) {
                    propType = refineObjectWrapper(propType, attributeType);
                    // An open Object slot (including a wrapper's Object payload) adopts container metadata as an
                    // open mutable schema instead of dropping it; global element configuration still overrides the
                    // adopted component, while a declared component keeps its precedence.
                    Type<?> refined = payloadType != null && payloadType.isObject() ? refineDeclaredSchema(propType, attributeType) : propType;

                    if (explicitObjectOverride && refined != propType
                            && !(refined.isMap() || refined.isCollection() || refined.isArray() || refined.isBean())) {
                        // An explicit Object value-type override is open scalar text: a scalar attribute does not type it, as
                        // on the StAX and DOM readers (container and bean metadata still shape a nested element).
                        refined = propType;
                    } else if (explicitObjectOverride && refined != propType && refined.isBean()) {
                        // Bean metadata under the override shapes a nested element only: text stays open scalar text, as on the
                        // StAX and DOM readers (it was converted with the bean class: "Can't parse", or a bean from JSON text).
                        overrideTextType = propType;
                    }

                    propType = withSlotComponent(declaredSlotType, refined, config.getElementType());
                }
                // The global map types fill a declared map slot's open arguments ahead of the metadata's.
                propType = withSlotMapTypes(declaredSlotType, propType, config);
                if (nodeType == NodeType.KEY || nodeType == NodeType.VALUE || nodeType == NodeType.ELEMENT || nodeType == NodeType.PROPERTY) {
                    valueMetadata = attributeType;
                }
            }
            nodeTypeQueue.add(nodeType);
            valueChildSeenQueue.add(false);
            optionalWrapperQueue.add(null);
            if (isNull && inIgnorePropRefCount == 0
                    && (nodeType == NodeType.PROPERTY || nodeType == NodeType.ELEMENT || nodeType == NodeType.KEY || nodeType == NodeType.VALUE)) {
                inNullValueRefCount = 1;
            }
        }

        /**
         * @throws ParsingException if array content cannot be parsed, a map entry has missing or duplicate components, or the closing node has an unsupported type
         */
        @SuppressWarnings("unchecked")
        @Override
        public void endElement(final String namespaceURI, final String localName, final String qName) throws ParsingException {
            if (inNullValueRefCount > 1) {
                inNullValueRefCount--;
                return;
            }
            if (inNullValueRefCount == 1) {
                inNullValueRefCount = 0;
                if (nullStructuredValue) {
                    nullStructuredValue = false;
                    eleValue = null;
                    return;
                }
            }
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
            // A completed child can itself be null. Presence, rather than eleValue != null, distinguishes
            // that result from a scalar wrapper whose text still needs decoding.
            final boolean valueChildSeen = valueChildSeenQueue.removeAt(nodeTypeQueue.size());
            final Type<?> optionalWrapper = optionalWrapperQueue.remove(nodeTypeQueue.size());

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

                    if (nodeValueQueue.get(nodeValueQueue.size() - 1) instanceof BeanShapedMap sink && sink.isEmpty() && Strings.isNotBlank(sb)) {
                        // Text instead of properties: the element is no bean-shaped element, so its unknown name is reported as before.
                        throw noBeanClassFound(nodeName, inputType);
                    }

                    popupNodeValue();

                    // The bean element that decided how property names and type attributes are written has ended: its next
                    // sibling (another item of a root array, collection or map) decides again, as each item does on the StAX
                    // and DOM readers. The decision stuck for the whole document, so an item's type attribute was ignored
                    // whenever an earlier item had none.
                    if (nodeTypeQueue.size() == typeInfoDecisionDepth) {
                        checkedPropNameTag = false;
                        checkedTypeInfo = false;
                        isTagByPropertyName = false;
                        ignoreTypeInfo = false;
                        typeInfoDecisionDepth = -1;
                    }

                    break;
                }

                case ARRAY: {
                    final Type<?> textComponent = arrayTextComponentQueue.remove(arrayTextComponentQueue.size() - 1);

                    if (!coll.isEmpty()) {
                        final Object placeholder = nodeValueQueue.get(nodeValueQueue.size() - 2);
                        array = collectionToArray(coll, placeholder == UNTYPED_ARRAY_PLACEHOLDER ? inferArrayType(coll) : Type.of(placeholder.getClass()));
                    } else if (Strings.isNotBlank(sb)) {
                        // The shared array reader: the configured component reaches the open elements nested in the items
                        // (Object[][] with a configured Long), as on the standard readers.
                        array = readArrayText(sb.toString(), targetType, textComponent, config);

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
                        final Object parsed = parseJsonCollectionText(sb.toString(), eleTypeQueue.get(eleTypeQueue.size() - 1), targetType, targetClass,
                                collectionTextDeclaredType, config);

                        if (parsed != null) {
                            if (canReuseCompactContainer(targetClass)) {
                                replaceContainer(parsed);
                            } else {
                                coll.addAll((Collection<Object>) parsed);
                            }
                        }
                    }

                    finishContainer();
                    popupNodeValue();

                    break;
                }

                case MAP: {
                    final Map<Object, Object> closing = (Map<Object, Object>) nodeValueQueue.get(nodeValueQueue.size() - 1);
                    final String compactText = closing.isEmpty() ? sb.toString() : null;
                    if (compactText != null && isCompactMapText(compactText)) {
                        // The compact JSON form of the map itself, as the StAX and DOM readers read it. The key and value types
                        // resolved at the start apply (at the root an explicit global type ahead of the requested arguments, as
                        // for entries). The text reader already creates and converts the requested implementation.
                        final Object parsed = readContainerText(compactText,
                                compactMapSchema(true, null, keyTypeQueue.get(keyTypeQueue.size() - 1), valueTypeQueue.get(valueTypeQueue.size() - 1)),
                                targetClass, config);
                        if (canReuseCompactContainer(targetClass)) {
                            replaceContainer(parsed);
                        } else {
                            closing.putAll((Map<?, ?>) parsed);
                        }
                    }

                    finishContainer();
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
                        declaredSlotType = null;

                        break;
                    }

                    if (!isTagByPropertyName) {
                        beanOrPropName = beanOrPropNameQueue.remove(beanOrPropNameQueue.size() - 1);
                    }

                    if (beanInfo == null) {
                        // A property of a bean-shaped element read as a map (see BeanShapedMap).
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> sink = (Map<String, Object>) nodeValueQueue.get(nodeValueQueue.size() - 1);

                        if (!valueChildSeen) {
                            sink.put(beanOrPropName, isNull ? null : readValueText(propType, declaredSlotType, null, sb.toString(), config));
                        } else {
                            sink.put(beanOrPropName, optionalWrapper == null ? eleValue : wrapOptional(optionalWrapper, eleValue));

                            eleValue = null;
                        }

                        propInfo = null;
                        propType = null;
                        declaredSlotType = null;

                        break;
                    }

                    propInfo = beanInfo.getPropInfo(beanOrPropName);

                    // for propInfo is null if it's unknown property
                    if (propInfo != null && propInfo.jsonXmlExpose != JsonXmlField.Direction.SERIALIZE_ONLY) {
                        if (!valueChildSeen) {
                            if (isNull) {
                                propInfo.setPropValue(bean, null);
                            } else {
                                // A configured value type is the declared slot remembered before metadata refinement, and it
                                // routes the text as on the StAX and DOM readers, which do not refine a configured type with the
                                // attribute first (an Object configuration under Optional metadata stays scalar text).
                                final Type<?> routingType = propConfigured ? declaredSlotType : propType;
                                propInfo.setPropValue(bean, propInfo.hasFormat
                                        || !(routingType.isMap() || routingType.isCollection() || routingType.isArray() || routingType.isOptionalOrNullable())
                                                ? readPropertyText(propInfo, propConfigured ? declaredSlotType : null, valueMetadata,
                                                        !ignoreTypeInfo && !propConfigured, sb.toString(), config)
                                                : readValueText(propType, declaredSlotType, valueMetadata, sb.toString(), config));
                            }
                        } else {
                            propInfo.setPropValue(bean, optionalWrapper == null ? eleValue : wrapOptional(optionalWrapper, eleValue));

                            eleValue = null;
                        }
                    }

                    propInfo = null;
                    propType = null;
                    declaredSlotType = null;

                    break;
                }

                case ELEMENT: {

                    if (!valueChildSeen) {
                        if (isNull) {
                            coll.add(null);
                        } else {
                            coll.add(readValueText(propType, declaredSlotType, valueMetadata, sb.toString(), config));
                        }
                    } else {
                        coll.add(optionalWrapper == null ? eleValue : wrapOptional(optionalWrapper, eleValue));
                        eleValue = null;
                    }

                    propType = null;
                    declaredSlotType = null;

                    if (configuredItemQueue.remove(configuredItemQueue.size() - 1)) {
                        config = configQueue.remove(configQueue.size() - 1);
                    }

                    break;
                }

                case KEY: {
                    if (keyQueue.size() != entryKeyMarkQueue.get(entryKeyMarkQueue.size() - 1)) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY); // a second key in the same entry
                    }

                    if (!valueChildSeen) {
                        if (isNull) {
                            keyQueue.add(null);
                        } else {
                            keyQueue.add(readValueText(propType, declaredSlotType, valueMetadata, sb.toString(), config));
                        }
                    } else {
                        keyQueue.add(optionalWrapper == null ? eleValue : wrapOptional(optionalWrapper, eleValue));
                        eleValue = null;
                    }

                    propType = null;
                    declaredSlotType = null;

                    if (mapIgnoredPropNames != null) {
                        final Object latestKey = keyQueue.get(keyQueue.size() - 1);
                        // A null key is ignored under the name it is written as ("null"), as in the StAX and DOM readers.
                        if (mapIgnoredPropNames.contains(latestKey == null ? NULL_STRING : latestKey.toString())) {
                            inIgnorePropRefCount = 1;
                            inIgnoredEntry = true;
                            ignoredEntryValueSeen = false;
                        }
                    }

                    break;
                }

                case VALUE: {
                    entryValueSeenQueue.set(entryValueSeenQueue.size() - 1, Boolean.TRUE);

                    if (inIgnorePropRefCount == 1) {
                        inIgnorePropRefCount--;

                        eleValue = null;
                        propType = null;
                        declaredSlotType = null;

                        break;
                    }

                    checkEntryHasKey();

                    if (!valueChildSeen) {
                        if (isNull) {
                            map.put(keyQueue.remove(keyQueue.size() - 1), null);
                        } else {
                            map.put(keyQueue.remove(keyQueue.size() - 1), readValueText(overrideTextType != null ? overrideTextType : propType,
                                    declaredSlotType, valueMetadata, sb.toString(), config));
                        }
                    } else {
                        map.put(keyQueue.remove(keyQueue.size() - 1), optionalWrapper == null ? eleValue : wrapOptional(optionalWrapper, eleValue));
                        eleValue = null;
                    }

                    propType = null;
                    declaredSlotType = null;

                    break;
                }

                case ENTRY:
                    // If the key of this entry was marked to be ignored (inIgnorePropRefCount set to 1 at the end of
                    // the <key> element), the matching <value> element was skipped wholesale by the inIgnorePropRefCount
                    // increment/decrement in startElement/endElement, so the VALUE case never got a chance to reset the
                    // flag or to drain the key that was pushed onto keyQueue. Reset both here at the entry boundary;
                    // otherwise the flag stays set and every following entry is silently swallowed.
                    final boolean valueSeen = entryValueSeenQueue.remove(entryValueSeenQueue.size() - 1);

                    if (inIgnorePropRefCount == 1) {
                        if (inIgnoredEntry && !ignoredEntryValueSeen) {
                            throw new ParsingException(MALFORMED_MAP_ENTRY); // an ignored key without a value
                        }

                        inIgnoredEntry = false;
                        inIgnorePropRefCount--;
                        keyQueue.remove(keyQueue.size() - 1);
                    } else if (!valueSeen) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY); // an empty entry, or a key without a value
                    }

                    if (keyQueue.size() != entryKeyMarkQueue.remove(entryKeyMarkQueue.size() - 1)) {
                        throw new ParsingException(MALFORMED_MAP_ENTRY); // defensive: the checks above cover every known shape
                    }

                    // Entry text belongs to its key/value. If every entry was ignored, stale JSON-looking text
                    // must not be reparsed as the enclosing map's compact representation.
                    sb.setLength(0);
                    break;

                default:
                    throw new ParsingException("only array, collection, map and bean nodes are supported: " + nodeName);
            }
        }

        @Override
        public void characters(final char[] buffer, final int offset, final int count) {
            //noinspection StatementWithEmptyBody
            if (inIgnorePropRefCount > 0 || inNullValueRefCount > 0) {
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
         * Replaces the just-closed map/collection on top of the value stack by its {@code Immutable*} wrapper when it
         * is the mutable stand-in created for an immutable target; the root result is re-pointed as well.
         */
        @SuppressWarnings("unchecked")
        private void finishContainer() {
            if (containerConverters.isEmpty() || nodeValueQueue.isEmpty()) {
                return;
            }

            final int top = nodeValueQueue.size() - 1;
            final Object container = nodeValueQueue.get(top);
            final Function<Object, Object> converter = containerConverters.remove(container);

            if (converter != null) {
                final Object converted = converter.apply(container);

                nodeValueQueue.set(top, converted);

                if (resultHolder.value() == container) {
                    resultHolder.setValue((T) converted);
                }
            }
        }

        /** Installs an already converted compact-text result in place of its unused structured-content destination. */
        @SuppressWarnings("unchecked")
        private void replaceContainer(final Object replacement) {
            final Object previous = nodeValueQueue.set(nodeValueQueue.size() - 1, replacement);
            containerConverters.remove(previous);
            if (resultHolder.value() == previous) {
                resultHolder.setValue((T) replacement);
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
            } else if (eleValue instanceof BeanShapedMap sink) {
                // A bean-shaped element read as a map: the enclosing element receives a plain map.
                eleValue = new LinkedHashMap<>(sink);

                if (resultHolder.value() == sink) {
                    resultHolder.setValue((T) eleValue);
                }
            } else if (eleValue instanceof Map) {
                keyTypeQueue.remove(keyTypeQueue.size() - 1);
                valueTypeQueue.remove(valueTypeQueue.size() - 1);
            } else if (eleValue.getClass().isArray() || eleValue instanceof Collection) {
                eleTypeQueue.remove(eleTypeQueue.size() - 1);
                configuredItemsQueue.remove(configuredItemsQueue.size() - 1);
            }

            if (!nodeValueQueue.isEmpty()) {
                final Object next = nodeValueQueue.get(nodeValueQueue.size() - 1);
                beanInfo = beanInfoQueue.get(next);

                if (beanInfo != null) {
                    bean = next;
                    beanClass = beanInfo.clazz;
                    // Back inside a bean: the element type of the array/collection that just closed no longer applies.
                    eleType = null;
                } else if (next instanceof BeanShapedMap sink) {
                    // Back inside a bean-shaped element read as a map (see BeanShapedMap): its remaining properties are
                    // entries of the sink, which has no BeanInfo and no queued key/value types (treating it as a map
                    // popped the enclosing map's types, or failed on an empty queue inside a list item).
                    bean = sink;
                    beanClass = null;
                    eleType = null;
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
                        eleType = null;
                    }
                }
            }
        }

        /**
         * Resolves the current element's type attribute once. Concrete-class selection, item-type selection and the final
         * approval check at the end of {@code startElement} validate the same attribute text; the first call resolves it (a
         * disallowed name is rejected there, as before) and later calls in the same element reuse the result.
         *
         * @param attrs the element's attributes
         * @return the resolved type, or {@code null} when the attribute is absent or blank
         * @throws ParsingException if the type attribute names a class that is not allowed
         */
        private Type<?> typeAttributeOf(final Attributes attrs) throws ParsingException {
            if (!elementTypeAttributeResolved) {
                elementTypeAttribute = xmlParser.resolvePresentTypeAttribute(attrs == null ? null : attrs.getValue(XmlConstants.TYPE));
                elementTypeAttributeResolved = true;
            }

            return elementTypeAttribute;
        }

        /**
         * The class named by the current element's type attribute, resolved once per element (see {@link #typeAttributeOf}).
         *
         * @param attrs the element's attributes
         * @return the attribute's class, or {@code null} when the attribute is absent or blank
         * @throws ParsingException if the type attribute names a class that is not allowed
         */
        private Class<?> typeAttributeClassOf(final Attributes attrs) throws ParsingException {
            final Type<?> type = typeAttributeOf(attrs);

            return type == null ? null : type.javaType();
        }

        /**
         * The bean type named by the type attribute of a non-root element - a bean held in an Object or interface slot (a
         * property, a list item, a map key or value) - when it fits the slot, or {@code null}. It is used as the StAX reader
         * uses it, before node-name discovery, which searches a fixed package and cannot find a bean anywhere else. A class
         * that does not fit the slot's declared type is left to discovery, as on StAX. Once resolved, the attribute is not
         * checked against the approval policy again at the end of {@code startElement}.
         *
         * @param attrs the element's attributes
         * @param slotType the declared type of the slot the element fills (an interface type is not narrowed to a list here), or {@code null}
         * @return the bean type, or {@code null}
         * @throws ParsingException if the type attribute names a class that is not allowed
         */
        private Type<?> objectSlotBeanType(final Attributes attrs, final Type<?> slotType) throws ParsingException {
            if (isFirstCall || (checkedTypeInfo && ignoreTypeInfo)) {
                return null;
            }

            final Class<?> attributeClass = typeAttributeClassOf(attrs);
            typeAttributeChecked = true;

            if (attributeClass == null || (slotType != null && !slotType.javaType().isAssignableFrom(attributeClass))) {
                return null;
            }

            final Type<?> attributeType = Type.of(attributeClass);

            return attributeType.isBean() ? attributeType : null;
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
            valueMetadata = null;

            beanOrPropNameQueue.clear();
            nodeTypeQueue.clear();
            // A rejected duplicate wrapper can leave partial frames; none may survive pooled-handler reuse.
            valueChildSeenQueue.clear();
            optionalWrapperQueue.clear();
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
            collectionTextDeclaredType = null;
            arrayTextComponentQueue.clear();
            configuredItemQueue.clear();
            configuredItemsQueue.clear();
            configQueue.clear();
            targetClass = null;
            eleType = null;
            propType = null;
            // A failed conversion can leave a user-defined type here; a pooled handler must release its class loader.
            declaredSlotType = null;
            keyType = null;
            valueType = null;
            eleTypeQueue.clear();
            keyTypeQueue.clear();
            valueTypeQueue.clear();
            beanInfoQueue.clear();
            containerConverters.clear();

            // Per-node parsing flags.
            isNull = false;
            checkedPropNameTag = false;
            checkedTypeInfo = false;
            isTagByPropertyName = false;
            ignoreTypeInfo = false;
            typeInfoDecisionDepth = -1;
            typeAttributeChecked = false;
            elementTypeAttribute = null;
            elementTypeAttributeResolved = false;
            propConfigured = false;
            overrideTextType = null;
            isFirstCall = true;
            inIgnorePropRefCount = 0;
            rootIsNull = false;
            inNullValueRefCount = 0;
            nullStructuredValue = false;
            inIgnoredEntry = false;
            ignoredEntryValueSeen = false;
            entryKeyMarkQueue.clear();
            entryValueSeenQueue.clear();
            unknownWrapperBean = null;
            unknownWrapperType = null;
            unknownWrapperName = null;
        }
    }
}
