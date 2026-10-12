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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;

import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.ParserUtil.XmlEmbeddedJsonConfig;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.StringBufferType;
import com.landawn.abacus.type.StringBuilderType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.TypeAttrParser;
import com.landawn.abacus.util.XmlUtil;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u;

/**
 * Abstract base class providing common functionality for XML parser implementations.
 * This class extends {@link AbstractParser} and implements the {@link XmlParser} interface,
 * serving as the foundation for concrete XML parsing implementations.
 *
 * <p>This class provides:</p>
 * <ul>
 *   <li>Integration with JSON parser for hybrid JSON/XML processing</li>
 *   <li>Pre-configured JSON serialization configs used for the values that are embedded as JSON text</li>
 *   <li>Support for circular reference detection in XML serialization</li>
 *   <li>Default type definitions for XML key-value processing</li>
 *   <li>Default XML serialization and deserialization configurations</li>
 * </ul>
 *
 * <p>The class maintains four {@link JsonSerConfig} instances for the values it embeds as JSON text
 * (collections, arrays and other non-scalar values that have no element form): the default one and the
 * variants that tolerate empty beans, circular references, or both. They are ordinary JSON configurations -
 * quotation is <i>not</i> disabled, so the embedded text is valid JSON and is XML-escaped on the way out.</p>
 *
 * <p>Subclasses should implement the specific XML parsing and serialization logic while
 * leveraging these common utilities for consistent XML processing behavior.</p>
 *
 * @see XmlParser
 * @see AbstractParser
 * @see XmlSerConfig
 * @see XmlDeserConfig
 */
abstract class AbstractXmlParser extends AbstractParser<XmlSerConfig, XmlDeserConfig> implements XmlParser {

    private static final Logger logger = LoggerFactory.getLogger(AbstractXmlParser.class);

    /**
     * Former process-wide switch, retained as an internal name for compatibility tests. It no longer
     * grants type permissions; unknown classes cannot be loaded just because XML names them.
     */
    static final String XML_TYPE_CLASS_FOR_NAME_PROPERTY = "abacus.xml.allowTypeAttrClassForName";

    /**
     * Exact built-in scalar/container names, including runtime classes emitted by JDK collection factories.
     * Explicit application type permissions are held by each parser, independently of global registration.
     */
    private static final Set<String> SAFE_XML_TYPE_ATTRIBUTE_NAMES = builtInXmlTypeNames(Set.of("boolean", "byte", "char", "short", "int", "long", "float",
            "double", "Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float", "Double", "java.lang.Boolean", "java.lang.Byte",
            "java.lang.Character", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double", "String", "StringBuilder",
            "StringBuffer", "CharSequence", "Object", "Number", "java.lang.String", "java.lang.StringBuilder", "java.lang.StringBuffer",
            "java.lang.CharSequence", "java.lang.Object", "java.lang.Number", "BigInteger", "BigDecimal", "java.math.BigInteger", "java.math.BigDecimal",
            "Date", "Time", "Timestamp", "JUDate", "java.sql.Date", "java.sql.Time", "java.sql.Timestamp", "java.util.Date", "Calendar", "GregorianCalendar",
            "java.util.Calendar", "java.util.GregorianCalendar", "Duration", "Instant", "LocalDate", "LocalDateTime", "LocalTime", "MonthDay", "OffsetDateTime",
            "OffsetTime", "Period", "Year", "YearMonth", "ZonedDateTime", "ZoneId", "ZoneOffset", "java.time.Duration", "java.time.Instant",
            "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime", "java.time.MonthDay", "java.time.OffsetDateTime", "java.time.OffsetTime",
            "java.time.Period", "java.time.Year", "java.time.YearMonth", "java.time.ZonedDateTime", "java.time.ZoneId", "java.time.ZoneOffset", "UUID", "URI",
            "URL", "File", "Locale", "Currency", "java.util.UUID", "java.net.URI", "java.net.URL", "java.io.File", "java.util.Locale", "java.util.Currency",
            "Optional", "OptionalInt", "OptionalLong", "OptionalDouble", "java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong",
            "java.util.OptionalDouble", "AtomicBoolean", "AtomicInteger", "AtomicLong", "AtomicReference", "java.util.concurrent.atomic.AtomicBoolean",
            "java.util.concurrent.atomic.AtomicInteger", "java.util.concurrent.atomic.AtomicLong", "java.util.concurrent.atomic.AtomicReference", "Collection",
            "List", "ArrayList", "LinkedList", "Vector", "Stack", "Set", "HashSet", "LinkedHashSet", "SortedSet", "NavigableSet", "TreeSet", "Queue", "Deque",
            "ArrayDeque", "PriorityQueue", "java.util.Collection", "java.util.List", "java.util.ArrayList", "java.util.LinkedList", "java.util.Vector",
            "java.util.Stack", "java.util.Set", "java.util.HashSet", "java.util.LinkedHashSet", "java.util.SortedSet", "java.util.NavigableSet",
            "java.util.TreeSet", "java.util.Queue", "java.util.Deque", "java.util.ArrayDeque", "java.util.PriorityQueue", "CopyOnWriteArrayList",
            "CopyOnWriteArraySet", "ConcurrentLinkedQueue", "ConcurrentLinkedDeque", "LinkedBlockingQueue", "LinkedBlockingDeque", "PriorityBlockingQueue",
            "ConcurrentSkipListSet", "java.util.concurrent.CopyOnWriteArrayList", "java.util.concurrent.CopyOnWriteArraySet",
            "java.util.concurrent.ConcurrentLinkedQueue", "java.util.concurrent.ConcurrentLinkedDeque", "java.util.concurrent.LinkedBlockingQueue",
            "java.util.concurrent.LinkedBlockingDeque", "java.util.concurrent.PriorityBlockingQueue", "java.util.concurrent.ConcurrentSkipListSet", "Map",
            "HashMap", "LinkedHashMap", "SortedMap", "NavigableMap", "TreeMap", "Hashtable", "IdentityHashMap", "WeakHashMap", "Properties", "java.util.Map",
            "java.util.HashMap", "java.util.LinkedHashMap", "java.util.SortedMap", "java.util.NavigableMap", "java.util.TreeMap", "java.util.Hashtable",
            "java.util.IdentityHashMap", "java.util.WeakHashMap", "java.util.Properties", "ConcurrentMap", "ConcurrentHashMap", "ConcurrentNavigableMap",
            "ConcurrentSkipListMap", "java.util.concurrent.ConcurrentMap", "java.util.concurrent.ConcurrentHashMap",
            "java.util.concurrent.ConcurrentNavigableMap", "java.util.concurrent.ConcurrentSkipListMap", "ImmutableList", "ImmutableSet", "ImmutableMap",
            "com.landawn.abacus.util.ImmutableList", "com.landawn.abacus.util.ImmutableSet", "com.landawn.abacus.util.ImmutableMap", "MapEntity",
            "com.landawn.abacus.util.MapEntity", "Dataset", "com.landawn.abacus.util.Dataset", "RowDataset", "com.landawn.abacus.util.RowDataset",
            // The writer's names of the java.util optional types and of java.time.Duration (JdkOptionalType, JdkDurationType etc.:
            // type="Map<String, JdkOptional<String>>", type="JdkDuration"). Their classes are approved above, so these spellings grant
            // nothing new; rejecting them made the parser's own output unreadable.
            "JdkOptional", "JdkOptionalInt", "JdkOptionalLong", "JdkOptionalDouble", "JdkDuration",
            // These simple aliases resolve to Abacus types, not to the similarly named JDK types.
            "com.landawn.abacus.util.Duration", "com.landawn.abacus.util.u$Optional", "com.landawn.abacus.util.u.Optional",
            "com.landawn.abacus.util.u$OptionalInt", "com.landawn.abacus.util.u.OptionalInt", "com.landawn.abacus.util.u$OptionalLong",
            "com.landawn.abacus.util.u.OptionalLong", "com.landawn.abacus.util.u$OptionalDouble", "com.landawn.abacus.util.u.OptionalDouble"));

    private static Set<String> builtInXmlTypeNames(final Set<String> names) {
        final Set<String> result = new HashSet<>(names);
        // These library values have intrinsic short names in the XML writer. Both spellings also remain
        // available to explicitly restricted parsers, independently of application type registrations.
        for (final String name : List.of("BooleanList", "CharList", "ByteList", "ShortList", "IntList", "LongList", "FloatList", "DoubleList", "MutableBoolean",
                "MutableChar", "MutableByte", "MutableShort", "MutableInt", "MutableLong", "MutableFloat", "MutableDouble")) {
            result.add(name);
            result.add("com.landawn.abacus.util." + name);
        }

        // Collection writers preserve runtime type names. Discover the exact JDK classes from trusted
        // factories rather than allowing a package prefix or loading a class named by the document.
        // Uninstantiable wrappers still use the reader's existing fallback to the declared container type.
        for (final Object container : new Object[] { List.of(), List.of(0), List.of(0, 1, 2), Set.of(), Set.of(0), Set.of(0, 1, 2), Map.of(), Map.of(0, 0),
                Map.of(0, 0, 1, 1), Arrays.asList(0), Collections.nCopies(1, 0), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(),
                Collections.emptySortedSet(), Collections.emptyNavigableSet(), Collections.emptySortedMap(), Collections.emptyNavigableMap(),
                Collections.singletonList(0), Collections.singleton(0), Collections.singletonMap(0, 0), Collections.unmodifiableCollection(new ArrayList<>()),
                Collections.unmodifiableList(new ArrayList<>()), Collections.unmodifiableList(new LinkedList<>()), Collections.unmodifiableSet(new HashSet<>()),
                Collections.unmodifiableSortedSet(new TreeSet<>()), Collections.unmodifiableNavigableSet(new TreeSet<>()),
                Collections.unmodifiableMap(new HashMap<>()), Collections.unmodifiableSortedMap(new TreeMap<>()),
                Collections.unmodifiableNavigableMap(new TreeMap<>()), Collections.synchronizedCollection(new ArrayList<>()),
                Collections.synchronizedList(new ArrayList<>()), Collections.synchronizedList(new LinkedList<>()), Collections.synchronizedSet(new HashSet<>()),
                Collections.synchronizedSortedSet(new TreeSet<>()), Collections.synchronizedNavigableSet(new TreeSet<>()),
                Collections.synchronizedMap(new HashMap<>()), Collections.synchronizedSortedMap(new TreeMap<>()),
                Collections.synchronizedNavigableMap(new TreeMap<>()), Collections.checkedCollection(new ArrayList<>(), Object.class),
                Collections.checkedList(new ArrayList<>(), Object.class), Collections.checkedList(new LinkedList<>(), Object.class),
                Collections.checkedSet(new HashSet<>(), Object.class), Collections.checkedSortedSet(new TreeSet<>(), Object.class),
                Collections.checkedNavigableSet(new TreeSet<>(), Object.class), Collections.checkedMap(new HashMap<>(), Object.class, Object.class),
                Collections.checkedSortedMap(new TreeMap<>(), Object.class, Object.class),
                Collections.checkedNavigableMap(new TreeMap<>(), Object.class, Object.class) }) {
            result.add(container.getClass().getName());
            final String canonicalName = container.getClass().getCanonicalName();
            // Factory implementations may be local or anonymous classes, which have no canonical name.
            if (canonicalName != null) {
                result.add(canonicalName);
            }
        }
        return Set.copyOf(result);
    }

    // protected static final int TEXT_SIZE_TO_READ_MORE = 256;

    /** Shared JSON parser used to serialize values that are emitted as a JSON payload inside an XML element. */
    protected static final JsonParser jsonParser = new CompactJsonParser();

    /**
     * Baseline JSON serialization configuration used for JSON payloads embedded in XML. Character
     * values stay quoted so array/collection delimiters are unambiguous. Selected by {@link #getJSC(XmlSerConfig)} when neither
     * circular references nor empty beans need to be tolerated.
     */
    protected static final JsonSerConfig jsc = new XmlEmbeddedJsonConfig();

    /** Variant of {@link #jsc} that also tolerates beans with no serializable property. */
    protected static final JsonSerConfig jscWithEmptyBeanSupported = new XmlEmbeddedJsonConfig().setFailOnEmptyBean(false);

    /** Variant of {@link #jsc} that also tolerates circular references. */
    protected static final JsonSerConfig jscWithCircularRefSupported = new XmlEmbeddedJsonConfig().setCircularReferenceSupported(true);

    /** Variant of {@link #jsc} that tolerates both circular references and empty beans. */
    protected static final JsonSerConfig jscWithCircularRefAndEmptyBeanSupported = new XmlEmbeddedJsonConfig().setFailOnEmptyBean(false)
            .setCircularReferenceSupported(true);

    /** Key type assumed for map entries when the configuration specifies none ({@code Object}). */
    protected static final Type<?> defaultKeyType = objType;

    /** Value type assumed for map entries and elements when the configuration specifies none ({@code Object}). */
    protected static final Type<?> defaultValueType = objType;

    /** The fallback serialization configuration used when a per-call {@code config} argument is {@code null}. */
    protected final XmlSerConfig defaultXmlSerConfig;

    /** The fallback deserialization configuration used when a per-call {@code config} argument is {@code null}. */
    protected final XmlDeserConfig defaultXmlDeserConfig;

    private final Map<String, Type<?>> approvedXmlTypes;
    private final Set<Class<?>> approvedXmlClasses;
    private final boolean acceptRegisteredTypes;

    /**
     * Constructs an {@code AbstractXmlParser} with default serialization and deserialization configurations.
     */
    protected AbstractXmlParser() {
        this(null, null);
    }

    /**
     * Constructs an {@code AbstractXmlParser} with the given serialization and deserialization configurations.
     * When either argument is {@code null}, a new default configuration is used in its place.
     *
     * @param xsc the XML serialization configuration, or {@code null} to use a new default configuration
     * @param xdc the XML deserialization configuration, or {@code null} to use a new default configuration
     */
    protected AbstractXmlParser(final XmlSerConfig xsc, final XmlDeserConfig xdc) {
        this(xsc, xdc, Set.of(), true);
    }

    /**
     * Constructs a parser with the supplied configurations and explicitly approved XML types.
     *
     * @param xsc the serialization configuration, or {@code null} to use a new default configuration
     * @param xdc the deserialization configuration, or {@code null} to use a new default configuration
     * @param allowedTypeClasses the approved classes; may be empty but must not be {@code null} or contain {@code null}
     * @throws IllegalArgumentException if {@code allowedTypeClasses} is {@code null} or an approved type name is ambiguous
     * @throws NullPointerException if {@code allowedTypeClasses} contains a {@code null} element
     */
    protected AbstractXmlParser(final XmlSerConfig xsc, final XmlDeserConfig xdc, final Set<Class<?>> allowedTypeClasses)
            throws IllegalArgumentException, NullPointerException {
        this(xsc, xdc, allowedTypeClasses, false);
    }

    /**
     * Initializes the configuration defaults and a snapshot of the explicitly approved XML types.
     *
     * @param xsc the serialization configuration, or {@code null} to use a new default configuration
     * @param xdc the deserialization configuration, or {@code null} to use a new default configuration
     * @param allowedTypeClasses the approved classes; may be empty but must not be {@code null} or contain {@code null}
     * @param acceptRegisteredTypes whether registered types may also be resolved
     * @throws IllegalArgumentException if {@code allowedTypeClasses} is {@code null} or an approved type name is ambiguous
     * @throws NullPointerException if {@code allowedTypeClasses} contains a {@code null} element
     */
    private AbstractXmlParser(final XmlSerConfig xsc, final XmlDeserConfig xdc, final Set<Class<?>> allowedTypeClasses, final boolean acceptRegisteredTypes)
            throws IllegalArgumentException, NullPointerException {
        N.checkArgNotNull(allowedTypeClasses, cs.allowedTypeClasses);
        this.acceptRegisteredTypes = acceptRegisteredTypes;
        final Map<String, Type<?>> approved = new HashMap<>();
        final Set<Class<?>> resolvedClasses = new HashSet<>();
        for (final Class<?> cls : Set.copyOf(allowedTypeClasses)) {
            final Type<?> type = Type.of(cls);
            // An enum constant's class resolves to its declaring enum. Approve that resolved class,
            // while retaining only the supplied class's names as permitted document spellings below.
            resolvedClasses.add(type.javaType());
            approved.put(cls.getName(), type);
            if (cls.getCanonicalName() != null) {
                approved.put(cls.getCanonicalName(), type);
            }
            if (cls.getSimpleName().equals(type.name())) {
                final Type<?> previous = approved.putIfAbsent(type.name(), type);
                N.checkArgument(previous == null || previous.javaType() == cls, "Ambiguous approved XML type name: " + type.name());
            }
        }
        approvedXmlTypes = Map.copyOf(approved);
        approvedXmlClasses = Set.copyOf(resolvedClasses);
        defaultXmlSerConfig = xsc != null ? xsc : new XmlSerConfig();
        defaultXmlDeserConfig = xdc != null ? xdc : new XmlDeserConfig();
    }

    /**
     * Deserializes an XML DOM node into an object of the specified target type using default deserialization configuration.
     * This method provides a convenient way to convert XML node structures into Java objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = XmlUtil.createDOMParser().parse(new InputSource(new StringReader(xmlString)));
     * Node node = doc.getDocumentElement();
     * User user = parser.deserialize(node, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the XML DOM node to deserialize
     * @param targetType the type of the target object to deserialize into
     * @return an instance of the target type populated with data from the XML node
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure does not match the target type
     * @throws UnsupportedOperationException if the concrete parser does not support deserialization from a DOM {@code Node}
     */
    @Override
    public <T> T deserialize(final Node source, final Type<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UnsupportedOperationException {
        return deserialize(source, null, targetType);
    }

    /**
     * Deserializes an XML DOM node into an object of the specified target class using default deserialization configuration.
     * This method provides a convenient way to convert XML node structures into Java objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = XmlUtil.createDOMParser().parse(new InputSource(new StringReader(xmlString)));
     * Node node = doc.getDocumentElement();
     * User user = parser.deserialize(node, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the XML DOM node to deserialize
     * @param targetType the class of the target object to deserialize into
     * @return an instance of the target class populated with data from the XML node
     * @throws IllegalArgumentException if {@code source} or {@code targetType} is {@code null}
     * @throws ParsingException if the XML structure does not match the target type
     * @throws UnsupportedOperationException if the concrete parser does not support deserialization from a DOM {@code Node}
     */
    @Override
    public <T> T deserialize(final Node source, final Class<? extends T> targetType)
            throws IllegalArgumentException, ParsingException, UnsupportedOperationException {
        return deserialize(source, null, targetType);
    }

    /**
     * Deserializes XML content from a file into an object, using a map of XML element names to types
     * for dynamic type resolution during parsing.
     *
     * <p>This default implementation throws {@link UnsupportedOperationException}. Concrete subclasses
     * that support node-typed deserialization must override this method.</p>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML content to deserialize
     * @param config the XML deserialization configuration; may be {@code null} for default settings
     * @param nodeTypes a map of XML element names to their corresponding {@link Type} descriptors,
     *        used to resolve the concrete type of the root element
     * @return an instance of the resolved target type populated with data from the XML content
     * @throws UnsupportedOperationException always thrown by this base-class implementation
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML content from an input stream into an object, using a map of XML element names
     * to types for dynamic type resolution during parsing.
     *
     * <p>This default implementation throws {@link UnsupportedOperationException}. Concrete subclasses
     * that support node-typed deserialization must override this method.</p>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML content to deserialize
     * @param config the XML deserialization configuration; may be {@code null} for default settings
     * @param nodeTypes a map of XML element names to their corresponding {@link Type} descriptors,
     *        used to resolve the concrete type of the root element
     * @return an instance of the resolved target type populated with data from the XML content
     * @throws UnsupportedOperationException always thrown by this base-class implementation
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML content from a reader into an object, using a map of XML element names to types
     * for dynamic type resolution during parsing.
     *
     * <p>This default implementation throws {@link UnsupportedOperationException}. Concrete subclasses
     * that support node-typed deserialization must override this method.</p>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML content to deserialize
     * @param config the XML deserialization configuration; may be {@code null} for default settings
     * @param nodeTypes a map of XML element names to their corresponding {@link Type} descriptors,
     *        used to resolve the concrete type of the root element
     * @return an instance of the resolved target type populated with data from the XML content
     * @throws UnsupportedOperationException always thrown by this base-class implementation
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes an XML DOM node into an object, using a map of XML element names to types for
     * dynamic type resolution during parsing.
     *
     * <p>This default implementation throws {@link UnsupportedOperationException}. Concrete subclasses
     * that support node-typed deserialization must override this method.</p>
     *
     * @param <T> the type of the target object
     * @param source the XML DOM node to deserialize
     * @param config the XML deserialization configuration; may be {@code null} for default settings
     * @param nodeTypes a map of XML element names to their corresponding {@link Type} descriptors,
     *        used to resolve the concrete type of the root element
     * @return an instance of the resolved target type populated with data from the XML node
     * @throws UnsupportedOperationException always thrown by this base-class implementation
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Event filter hiding comments and processing instructions. Neither is character data, so
     * {@code <text>a<?pi x?>b</text>} must read back as {@code "ab"}: without the filter the text-coalescing
     * loop every StAX backend runs stops at the intervening event and everything before it is dropped.
     */
    private static final StreamFilter NO_COMMENT_OR_PI = reader -> reader.getEventType() != XMLStreamConstants.COMMENT
            && reader.getEventType() != XMLStreamConstants.PROCESSING_INSTRUCTION;

    /**
     * Creates an XML stream reader that filters out comments and processing instructions while preserving all
     * character data. Structural readers handle indentation separately so scalar whitespace is not lost.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new StringReader("<root><item>value</item></root>");
     * XMLStreamReader streamReader = createXMLStreamReader(reader);
     * }</pre>
     *
     * @param br the reader containing XML content to parse
     * @return an XMLStreamReader configured to skip comments and processing instructions, and to retain whitespace
     * @throws IllegalArgumentException if {@code br} is {@code null}
     * @throws RuntimeException if the StAX provider cannot create the reader over {@code br} or its filtering wrapper
     */
    protected XMLStreamReader createXMLStreamReader(final Reader br) throws IllegalArgumentException, RuntimeException {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(br), NO_COMMENT_OR_PI);
    }

    /**
     * Creates an XML stream reader that filters out comments and processing instructions while preserving character
     * data from a byte stream. XML encoding is detected from the stream content (for example, BOM/XML declaration).
     *
     * @param is the input stream containing XML content to parse
     * @return an XMLStreamReader configured to skip comments and processing instructions, and to retain whitespace
     * @throws IllegalArgumentException if {@code is} is {@code null}
     * @throws RuntimeException if the StAX provider cannot create the reader over {@code is} or its filtering wrapper
     */
    protected XMLStreamReader createXMLStreamReader(final InputStream is) throws IllegalArgumentException, RuntimeException {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(is), NO_COMMENT_OR_PI);
    }

    // Use only between structural wrappers; scalar readers must retain whitespace events.
    /**
     * @throws XMLStreamException if advancing the XML reader fails
     */
    static int nextStructuralEvent(final XMLStreamReader reader) throws XMLStreamException {
        int event;
        do {
            event = reader.next();
        } while (reader.isWhiteSpace() && reader.hasNext());
        return event;
    }

    /**
     * Advances a newly created stream reader to the document element.
     *
     * @param xmlReader the stream reader to advance
     * @throws XMLStreamException if the underlying stream cannot be read
     * @throws ParsingException if the document ends without a root element
     */
    protected static void moveToRootElement(final XMLStreamReader xmlReader) throws XMLStreamException, ParsingException {
        if (xmlReader.getEventType() == XMLStreamConstants.START_ELEMENT) {
            return;
        }

        while (xmlReader.hasNext()) {
            if (xmlReader.next() == XMLStreamConstants.START_ELEMENT) {
                return;
            }
        }

        throw new ParsingException("No root element found in XML document");
    }

    /**
     * Advances the reader to the next element boundary, skipping character data and whitespace (comments and
     * processing instructions are already filtered by {@link #createXMLStreamReader(Reader)}).
     *
     * @param reader the stream reader to advance
     * @return {@code START_ELEMENT}, {@code END_ELEMENT} or {@code END_DOCUMENT}; another event type only
     *         when the reader has no further events
     * @throws XMLStreamException if the underlying stream cannot be read
     */
    static int nextElementEvent(final XMLStreamReader reader) throws XMLStreamException {
        int event;
        do {
            event = reader.next();
        } while (event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT && event != XMLStreamConstants.END_DOCUMENT
                && reader.hasNext());
        return event;
    }

    /**
     * Consumes the epilog of a document after the root element has been read, so that a second root
     * element, character data or a CDATA section after the root is reported instead of being silently
     * ignored. Whitespace, comments and processing instructions are legal there and pass. The StAX
     * implementation enforces the epilog grammar itself, so this method only has to keep pulling events
     * until {@code END_DOCUMENT}.
     *
     * <p>Only call this for a bounded source (a String, a byte array or a file): on an open pipe or
     * socket it would block waiting for the next event.</p>
     *
     * @param xmlReader the stream reader positioned on (or after) the root element's end element
     * @throws XMLStreamException if the epilog contains anything other than whitespace, comments or
     *         processing instructions, or if the underlying stream cannot be read
     */
    protected static void drainEpilog(final XMLStreamReader xmlReader) throws XMLStreamException {
        while (xmlReader.hasNext()) {
            xmlReader.next();
        }
    }

    /**
     * Returns whether {@code cp} is a code point that XML 1.0 can carry as character data. Keep in sync
     * with the private {@code RowDataset.isXmlCharacter}, so the two XML writers in this library agree.
     */
    private static boolean isXmlCharacter(final int cp) {
        return cp == 9 || cp == 10 || cp == 13 || cp >= 0x20 && cp <= 0xD7FF || cp >= 0xE000 && cp <= 0xFFFD || cp >= 0x10000 && cp <= 0x10FFFF;
    }

    /**
     * Verifies that {@code text} can be written as XML 1.0 character data. {@code BufferedXmlWriter}
     * turns control characters into character references (for example {@code &#x1;}) and passes
     * surrogates and {@code U+FFFE}/{@code U+FFFF} through unchanged; neither form is accepted by any
     * XML parser, so such a value would be serialized into a document that cannot be read back.
     * Tab, LF and CR are legal and pass; a valid surrogate pair passes; a lone surrogate does not.
     *
     * @param text the text to check; {@code null} passes
     * @param what a short description of the value used in the error message, for example
     *        {@code "Property 'name'"} or {@code "Map value"}
     * @throws ParsingException if {@code text} contains a code unit that cannot be represented in XML 1.0
     */
    protected static void checkXmlText(final CharSequence text, final String what) throws ParsingException {
        checkXmlText(text, what, null);
    }

    private static void checkXmlText(final CharSequence text, final String what, final String propName) throws ParsingException {
        if (text == null) {
            return;
        }

        for (int i = 0, len = text.length(); i < len; i++) {
            final char ch = text.charAt(i);

            if (ch >= 0x20 && ch <= 0xD7FF) {
                continue;
            }

            if (Character.isHighSurrogate(ch) && i + 1 < len && Character.isLowSurrogate(text.charAt(i + 1))) {
                i++;
                continue;
            }

            if (!isXmlCharacter(ch)) {
                throw notXmlCharacter(propName == null ? what : "Property '" + propName + "'", ch);
            }
        }
    }

    /**
     * Creates the error for a code unit that XML 1.0 character data cannot represent. Shared by
     * {@link #checkXmlText(CharSequence, String)} and the streaming raw-JSON writer so both report it the same way.
     *
     * @param what a short description of the value, for example {@code "JSON raw value"}
     * @param ch the offending code unit
     * @return the exception to throw
     */
    private static ParsingException notXmlCharacter(final String what, final char ch) {
        return new ParsingException(what + " contains U+" + String.format("%04X", (int) ch) + ", which cannot be represented in XML 1.0");
    }

    /**
     * Returns the entity that replaces an XML markup character ({@code &}, {@code <} or {@code >}) in an
     * embedded raw JSON payload, or {@code null} if {@code ch} is written as is. Quotes are left alone.
     * Shared by {@link #writeRawJson(BufferedXmlWriter, String)} and the streaming raw-JSON writer.
     *
     * @param ch the code unit to check
     * @return the replacement entity, or {@code null}
     */
    private static String rawJsonEntity(final char ch) {
        return ch == '&' ? "&amp;" : ch == '<' ? "&lt;" : ch == '>' ? "&gt;" : null;
    }

    /**
     * Returns whether a code unit that is not part of a valid surrogate pair must be written as a JSON
     * {@code \\uXXXX} escape in embedded JSON: an isolated surrogate, {@code U+FFFE} or {@code U+FFFF}.
     * XML character references cannot represent these values. Shared by {@link #escapeEmbeddedJson(String)}
     * and the streaming embedded-JSON writer.
     *
     * @param ch the code unit to check, never the high half of a valid surrogate pair
     * @return {@code true} if {@code ch} must be escaped
     */
    private static boolean isJsonEscapedInXml(final char ch) {
        return Character.isSurrogate(ch) || ch == '\uFFFE' || ch == '\uFFFF';
    }

    /**
     * Returns the index of the first code unit of {@code text} that XML 1.0 cannot carry, applying the same
     * rule as {@link #checkXmlText(CharSequence, String)}: a valid surrogate pair passes, a lone surrogate
     * does not.
     *
     * @param text the text to scan
     * @return the index of the offending code unit, or {@code -1} when every code unit is representable
     */
    private static int indexOfNonXmlCharacter(final char[] text) {
        for (int i = 0, len = text.length; i < len; i++) {
            final char ch = text[i];

            if (ch >= 0x20 && ch <= 0xD7FF) {
                continue;
            }

            if (Character.isHighSurrogate(ch) && i + 1 < len && Character.isLowSurrogate(text[i + 1])) {
                i++;
                continue;
            }

            if (!isXmlCharacter(ch)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Verifies that {@code name} can be written inside a {@code name="..."} attribute. Attribute position has
     * an escaping mechanism, so markup characters are fine there, but the characters XML 1.0 cannot represent
     * at all are not: {@link ParserUtil.XmlNameTag} turns them into a character reference
     * ({@code name="a&#x1;b"}) or passes an isolated surrogate through unchanged, and no XML reader accepts
     * either. The same code unit in a property <i>value</i> is rejected by
     * {@link #checkXmlText(CharSequence, String)}, so the two positions agree.
     *
     * @param name the precomputed name to check
     * @param what a short description of the value used in the error message, for example
     *        {@code "Property name"}
     * @throws ParsingException if {@code name} contains a code unit that cannot be represented in XML 1.0
     */
    protected static void checkXmlAttributeName(final char[] name, final String what) throws ParsingException {
        final int idx = indexOfNonXmlCharacter(name);

        if (idx >= 0) {
            throw new ParsingException(
                    what + " '" + String.valueOf(name) + "' contains U+" + String.format("%04X", (int) name[idx]) + ", which cannot be represented in XML 1.0");
        }
    }

    /**
     * Non-ASCII inclusive start/end pairs from XML 1.0 fourth-edition Appendix B, in sorted order.
     * The fixed grammar must not grow with Java's Unicode categories: a newly assigned letter can
     * otherwise be emitted as a tag that the supported XML readers reject. Keeping the ranges here
     * avoids a provider dependency in this shared writer.
     *
     * @see <a href="https://www.w3.org/TR/2006/REC-xml-20060816/#CharClasses">XML 1.0 fourth-edition character classes</a>
     */
    private static final int[] XML_NAME_START_RANGES = { 0x00C0, 0x00D6, 0x00D8, 0x00F6, 0x00F8, 0x0131, 0x0134, 0x013E, 0x0141, 0x0148, 0x014A, 0x017E, 0x0180,
            0x01C3, 0x01CD, 0x01F0, 0x01F4, 0x01F5, 0x01FA, 0x0217, 0x0250, 0x02A8, 0x02BB, 0x02C1, 0x0386, 0x0386, 0x0388, 0x038A, 0x038C, 0x038C, 0x038E,
            0x03A1, 0x03A3, 0x03CE, 0x03D0, 0x03D6, 0x03DA, 0x03DA, 0x03DC, 0x03DC, 0x03DE, 0x03DE, 0x03E0, 0x03E0, 0x03E2, 0x03F3, 0x0401, 0x040C, 0x040E,
            0x044F, 0x0451, 0x045C, 0x045E, 0x0481, 0x0490, 0x04C4, 0x04C7, 0x04C8, 0x04CB, 0x04CC, 0x04D0, 0x04EB, 0x04EE, 0x04F5, 0x04F8, 0x04F9, 0x0531,
            0x0556, 0x0559, 0x0559, 0x0561, 0x0586, 0x05D0, 0x05EA, 0x05F0, 0x05F2, 0x0621, 0x063A, 0x0641, 0x064A, 0x0671, 0x06B7, 0x06BA, 0x06BE, 0x06C0,
            0x06CE, 0x06D0, 0x06D3, 0x06D5, 0x06D5, 0x06E5, 0x06E6, 0x0905, 0x0939, 0x093D, 0x093D, 0x0958, 0x0961, 0x0985, 0x098C, 0x098F, 0x0990, 0x0993,
            0x09A8, 0x09AA, 0x09B0, 0x09B2, 0x09B2, 0x09B6, 0x09B9, 0x09DC, 0x09DD, 0x09DF, 0x09E1, 0x09F0, 0x09F1, 0x0A05, 0x0A0A, 0x0A0F, 0x0A10, 0x0A13,
            0x0A28, 0x0A2A, 0x0A30, 0x0A32, 0x0A33, 0x0A35, 0x0A36, 0x0A38, 0x0A39, 0x0A59, 0x0A5C, 0x0A5E, 0x0A5E, 0x0A72, 0x0A74, 0x0A85, 0x0A8B, 0x0A8D,
            0x0A8D, 0x0A8F, 0x0A91, 0x0A93, 0x0AA8, 0x0AAA, 0x0AB0, 0x0AB2, 0x0AB3, 0x0AB5, 0x0AB9, 0x0ABD, 0x0ABD, 0x0AE0, 0x0AE0, 0x0B05, 0x0B0C, 0x0B0F,
            0x0B10, 0x0B13, 0x0B28, 0x0B2A, 0x0B30, 0x0B32, 0x0B33, 0x0B36, 0x0B39, 0x0B3D, 0x0B3D, 0x0B5C, 0x0B5D, 0x0B5F, 0x0B61, 0x0B85, 0x0B8A, 0x0B8E,
            0x0B90, 0x0B92, 0x0B95, 0x0B99, 0x0B9A, 0x0B9C, 0x0B9C, 0x0B9E, 0x0B9F, 0x0BA3, 0x0BA4, 0x0BA8, 0x0BAA, 0x0BAE, 0x0BB5, 0x0BB7, 0x0BB9, 0x0C05,
            0x0C0C, 0x0C0E, 0x0C10, 0x0C12, 0x0C28, 0x0C2A, 0x0C33, 0x0C35, 0x0C39, 0x0C60, 0x0C61, 0x0C85, 0x0C8C, 0x0C8E, 0x0C90, 0x0C92, 0x0CA8, 0x0CAA,
            0x0CB3, 0x0CB5, 0x0CB9, 0x0CDE, 0x0CDE, 0x0CE0, 0x0CE1, 0x0D05, 0x0D0C, 0x0D0E, 0x0D10, 0x0D12, 0x0D28, 0x0D2A, 0x0D39, 0x0D60, 0x0D61, 0x0E01,
            0x0E2E, 0x0E30, 0x0E30, 0x0E32, 0x0E33, 0x0E40, 0x0E45, 0x0E81, 0x0E82, 0x0E84, 0x0E84, 0x0E87, 0x0E88, 0x0E8A, 0x0E8A, 0x0E8D, 0x0E8D, 0x0E94,
            0x0E97, 0x0E99, 0x0E9F, 0x0EA1, 0x0EA3, 0x0EA5, 0x0EA5, 0x0EA7, 0x0EA7, 0x0EAA, 0x0EAB, 0x0EAD, 0x0EAE, 0x0EB0, 0x0EB0, 0x0EB2, 0x0EB3, 0x0EBD,
            0x0EBD, 0x0EC0, 0x0EC4, 0x0F40, 0x0F47, 0x0F49, 0x0F69, 0x10A0, 0x10C5, 0x10D0, 0x10F6, 0x1100, 0x1100, 0x1102, 0x1103, 0x1105, 0x1107, 0x1109,
            0x1109, 0x110B, 0x110C, 0x110E, 0x1112, 0x113C, 0x113C, 0x113E, 0x113E, 0x1140, 0x1140, 0x114C, 0x114C, 0x114E, 0x114E, 0x1150, 0x1150, 0x1154,
            0x1155, 0x1159, 0x1159, 0x115F, 0x1161, 0x1163, 0x1163, 0x1165, 0x1165, 0x1167, 0x1167, 0x1169, 0x1169, 0x116D, 0x116E, 0x1172, 0x1173, 0x1175,
            0x1175, 0x119E, 0x119E, 0x11A8, 0x11A8, 0x11AB, 0x11AB, 0x11AE, 0x11AF, 0x11B7, 0x11B8, 0x11BA, 0x11BA, 0x11BC, 0x11C2, 0x11EB, 0x11EB, 0x11F0,
            0x11F0, 0x11F9, 0x11F9, 0x1E00, 0x1E9B, 0x1EA0, 0x1EF9, 0x1F00, 0x1F15, 0x1F18, 0x1F1D, 0x1F20, 0x1F45, 0x1F48, 0x1F4D, 0x1F50, 0x1F57, 0x1F59,
            0x1F59, 0x1F5B, 0x1F5B, 0x1F5D, 0x1F5D, 0x1F5F, 0x1F7D, 0x1F80, 0x1FB4, 0x1FB6, 0x1FBC, 0x1FBE, 0x1FBE, 0x1FC2, 0x1FC4, 0x1FC6, 0x1FCC, 0x1FD0,
            0x1FD3, 0x1FD6, 0x1FDB, 0x1FE0, 0x1FEC, 0x1FF2, 0x1FF4, 0x1FF6, 0x1FFC, 0x2126, 0x2126, 0x212A, 0x212B, 0x212E, 0x212E, 0x2180, 0x2182, 0x3007,
            0x3007, 0x3021, 0x3029, 0x3041, 0x3094, 0x30A1, 0x30FA, 0x3105, 0x312C, 0x4E00, 0x9FA5, 0xAC00, 0xD7A3 };

    /**
     * Sorted inclusive start/end pairs for the additional non-ASCII CombiningChar, Digit and Extender
     * characters allowed after the first character by XML 1.0 fourth-edition Appendix B.
     */
    private static final int[] XML_NAME_EXTRA_RANGES = { 0x00B7, 0x00B7, 0x02D0, 0x02D1, 0x0300, 0x0345, 0x0360, 0x0361, 0x0387, 0x0387, 0x0483, 0x0486, 0x0591,
            0x05A1, 0x05A3, 0x05B9, 0x05BB, 0x05BD, 0x05BF, 0x05BF, 0x05C1, 0x05C2, 0x05C4, 0x05C4, 0x0640, 0x0640, 0x064B, 0x0652, 0x0660, 0x0669, 0x0670,
            0x0670, 0x06D6, 0x06E4, 0x06E7, 0x06E8, 0x06EA, 0x06ED, 0x06F0, 0x06F9, 0x0901, 0x0903, 0x093C, 0x093C, 0x093E, 0x094D, 0x0951, 0x0954, 0x0962,
            0x0963, 0x0966, 0x096F, 0x0981, 0x0983, 0x09BC, 0x09BC, 0x09BE, 0x09C4, 0x09C7, 0x09C8, 0x09CB, 0x09CD, 0x09D7, 0x09D7, 0x09E2, 0x09E3, 0x09E6,
            0x09EF, 0x0A02, 0x0A02, 0x0A3C, 0x0A3C, 0x0A3E, 0x0A42, 0x0A47, 0x0A48, 0x0A4B, 0x0A4D, 0x0A66, 0x0A71, 0x0A81, 0x0A83, 0x0ABC, 0x0ABC, 0x0ABE,
            0x0AC5, 0x0AC7, 0x0AC9, 0x0ACB, 0x0ACD, 0x0AE6, 0x0AEF, 0x0B01, 0x0B03, 0x0B3C, 0x0B3C, 0x0B3E, 0x0B43, 0x0B47, 0x0B48, 0x0B4B, 0x0B4D, 0x0B56,
            0x0B57, 0x0B66, 0x0B6F, 0x0B82, 0x0B83, 0x0BBE, 0x0BC2, 0x0BC6, 0x0BC8, 0x0BCA, 0x0BCD, 0x0BD7, 0x0BD7, 0x0BE7, 0x0BEF, 0x0C01, 0x0C03, 0x0C3E,
            0x0C44, 0x0C46, 0x0C48, 0x0C4A, 0x0C4D, 0x0C55, 0x0C56, 0x0C66, 0x0C6F, 0x0C82, 0x0C83, 0x0CBE, 0x0CC4, 0x0CC6, 0x0CC8, 0x0CCA, 0x0CCD, 0x0CD5,
            0x0CD6, 0x0CE6, 0x0CEF, 0x0D02, 0x0D03, 0x0D3E, 0x0D43, 0x0D46, 0x0D48, 0x0D4A, 0x0D4D, 0x0D57, 0x0D57, 0x0D66, 0x0D6F, 0x0E31, 0x0E31, 0x0E34,
            0x0E3A, 0x0E46, 0x0E4E, 0x0E50, 0x0E59, 0x0EB1, 0x0EB1, 0x0EB4, 0x0EB9, 0x0EBB, 0x0EBC, 0x0EC6, 0x0EC6, 0x0EC8, 0x0ECD, 0x0ED0, 0x0ED9, 0x0F18,
            0x0F19, 0x0F20, 0x0F29, 0x0F35, 0x0F35, 0x0F37, 0x0F37, 0x0F39, 0x0F39, 0x0F3E, 0x0F3F, 0x0F71, 0x0F84, 0x0F86, 0x0F8B, 0x0F90, 0x0F95, 0x0F97,
            0x0F97, 0x0F99, 0x0FAD, 0x0FB1, 0x0FB7, 0x0FB9, 0x0FB9, 0x20D0, 0x20DC, 0x20E1, 0x20E1, 0x3005, 0x3005, 0x302A, 0x302F, 0x3031, 0x3035, 0x3099,
            0x309A, 0x309D, 0x309E, 0x30FC, 0x30FE };

    /**
     * Returns whether {@code name} is an XML NCName under the XML 1.0 fourth-edition character tables.
     * A {@code ':'} is deliberately rejected: the readers are namespace-aware and would report an
     * unbound prefix for it. Base letters and ideographics may start a name; digits, combining characters,
     * extenders, {@code '-'} and {@code '.'} may additionally follow it, and {@code '_'} is allowed anywhere.
     *
     * <p>The XML backends supported by this parser include fourth-edition implementations. Matching their
     * fixed tables, including rejection of supplementary-plane names and newer BMP letters, prevents the
     * writer from producing names its readers cannot consume. Java's evolving Unicode categories are not
     * the XML grammar.</p>
     *
     * @param name the candidate element name; {@code null} and the empty String are not names
     * @return {@code true} if {@code name} can be used as an XML element name
     */
    private static boolean isXmlName(final String name) {
        if (Strings.isEmpty(name)) {
            return false;
        }

        if (!isXmlNameStartChar(name.charAt(0))) {
            return false;
        }

        for (int i = 1, len = name.length(); i < len; i++) {
            if (!isXmlNameChar(name.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * The {@code char[]} form of {@link #isXmlName(String)}, for the precomputed
     * {@link ParserUtil.XmlNameTag#name} of a bean property: checking that array directly keeps the
     * per-property check on the serialization path allocation-free.
     *
     * @param name the candidate element name; {@code null} and the empty array are not names
     * @return {@code true} if {@code name} can be used as an XML element name
     */
    private static boolean isXmlName(final char[] name) {
        if (N.isEmpty(name)) {
            return false;
        }

        if (!isXmlNameStartChar(name[0])) {
            return false;
        }

        for (int i = 1, len = name.length; i < len; i++) {
            if (!isXmlNameChar(name[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether {@code ch} may start an XML name (an NCName, so {@code ':'} is excluded).
     *
     * @param ch the candidate character
     * @return whether the character may start a name
     */
    private static boolean isXmlNameStartChar(final char ch) {
        return ch < 128 ? ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z' || ch == '_' : inXmlNameRanges(ch, XML_NAME_START_RANGES);
    }

    /**
     * Returns whether {@code ch} may appear after the first character of an XML name.
     *
     * @param ch the candidate character
     * @return whether the character may appear after a name's first character
     */
    private static boolean isXmlNameChar(final char ch) {
        return isXmlNameStartChar(ch) || (ch < 128 ? ch >= '0' && ch <= '9' || ch == '-' || ch == '.' : inXmlNameRanges(ch, XML_NAME_EXTRA_RANGES));
    }

    /**
     * Returns whether {@code ch} belongs to sorted, disjoint inclusive start/end pairs.
     *
     * @param ch the candidate character
     * @param ranges the sorted, disjoint inclusive range pairs
     * @return whether any pair contains the character
     */
    private static boolean inXmlNameRanges(final char ch, final int[] ranges) {
        int low = 0;
        int high = ranges.length / 2 - 1;

        while (low <= high) {
            final int middle = (low + high) >>> 1;
            final int index = middle * 2;
            if (ch < ranges[index]) {
                high = middle - 1;
            } else if (ch > ranges[index + 1]) {
                low = middle + 1;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifies that {@code name} can be written as an XML element name, which is how these formats carry a map
     * key ({@code XmlParser}), a {@code MapEntity} property name and, under
     * {@link XmlSerConfig#setTagByPropertyName(boolean) tagByPropertyName}, a bean property name. XML has no
     * escaping mechanism in name position, so an invalid name can only be reported: writing it produces a
     * document that no XML reader accepts ({@code <map><1>a</1></map>}). Use {@code AbacusXmlParser}, whose
     * format writes map keys as element text, for maps with arbitrary keys.
     *
     * @param name the element name to check
     * @param what a short description of the value used in the error message, for example {@code "Map key"}
     * @throws ParsingException if {@code name} is not a valid XML element name
     */
    protected static void checkXmlElementName(final String name, final String what) throws ParsingException {
        if (!isXmlName(name)) {
            throw new ParsingException(what + " '" + name + "' is not a valid XML element name");
        }
    }

    /**
     * The {@code char[]} form of {@link #checkXmlElementName(String, String)}, used for the precomputed
     * element name of a bean property.
     *
     * @param name the element name to check
     * @param what a short description of the value used in the error message
     * @throws ParsingException if {@code name} is not a valid XML element name
     */
    protected static void checkXmlElementName(final char[] name, final String what) throws ParsingException {
        if (!isXmlName(name)) {
            throw new ParsingException(what + " '" + String.valueOf(name) + "' is not a valid XML element name");
        }
    }

    /**
     * The XML name tags of a property-less class, built once per class for {@link #writeEmptyObject}: the generic
     * object handler has no {@code BeanInfo} to hold them, and building the tags of every naming policy again for
     * each written instance cost about forty times the allocation of a one-property bean. The entry records the type
     * the tags were built for; should another type describe the class later, the tags are built for it as before.
     */
    private static final ClassValue<EmptyObjectTags> EMPTY_OBJECT_TAGS = new ClassValue<>() {
        @Override
        protected EmptyObjectTags computeValue(final Class<?> cls) {
            return buildEmptyObjectTags(Type.of(cls));
        }
    };

    /** The tags of a property-less class, indexed by {@link NamingPolicy#ordinal()}, with the type they were built for. */
    private static final class EmptyObjectTags {
        private final Type<?> type;
        private final ParserUtil.XmlNameTag[] tags;

        private EmptyObjectTags(final Type<?> type, final ParserUtil.XmlNameTag[] tags) {
            this.type = type;
            this.tags = tags;
        }
    }

    private static EmptyObjectTags buildEmptyObjectTags(final Type<?> type) {
        final String name = Beans.normalizePropName(ClassUtil.getSimpleClassName(type.javaType()));
        return new EmptyObjectTags(type, ParserUtil.getXmlNameTags(name, type.name(), true));
    }

    /**
     * Writes a property-less object as an empty bean element when requested. This applies only to the
     * generic object handler: unsupported structured types such as Dataset must still fail, not lose data.
     * This serialization option does not provide a deserialization codec for the otherwise unsupported class.
     *
     * @throws IOException if writing the element to {@code writer} fails
     * @throws ParsingException if {@code tagByPropertyName} is enabled and the class's simple name, converted by the naming policy, is not
     *         a valid XML element name
     */
    protected static boolean writeEmptyObject(final Type<?> type, final XmlSerConfig config, final String indentation, final BufferedXmlWriter writer)
            throws IOException, ParsingException {
        if (config.isFailOnEmptyBean() || !(type instanceof ObjectType) || !type.isObject()) {
            return false;
        }
        final JsonXmlConfig annotation = type.javaType().getAnnotation(JsonXmlConfig.class);
        final NamingPolicy policy = config.getPropNamingPolicy() != null ? config.getPropNamingPolicy()
                : annotation == null ? NamingPolicy.CAMEL_CASE : annotation.namingPolicy();
        EmptyObjectTags tags = EMPTY_OBJECT_TAGS.get(type.javaType());
        if (tags.type != type) {
            tags = buildEmptyObjectTags(type);
        }
        final ParserUtil.XmlNameTag tag = tags.tags[policy.ordinal()];
        if (config.isPrettyFormat() && indentation != null) {
            writer.write(IOUtil.LINE_SEPARATOR_UNIX);
            writer.write(indentation);
        }
        if (config.isTagByPropertyName()) {
            tag.checkXmlName(true, "Class name");
            writer.write(config.isWriteTypeInfo() ? tag.namedStartWithType : tag.namedStart);
            writer.write(tag.namedEnd);
        } else {
            writer.write(config.isWriteTypeInfo() ? tag.epStartWithType : tag.epStart);
            writer.write(tag.epEnd);
        }
        return true;
    }

    /**
     * Writes a directly serializable scalar as element text, guarding the values XML 1.0 cannot carry.
     * String, {@code CharSequence} and {@code char} values, including NUL, are checked with
     * {@link #checkXmlText(CharSequence, String)}
     * first; every other type is written unchanged.
     *
     * @param bw the writer to write to
     * @param type the type handler for {@code value}
     * @param value the value to write; must not be {@code null}
     * @param config the serialization configuration
     * @param what a short description of the value used in the error message
     * @throws ParsingException if the value contains a code unit that cannot be represented in XML 1.0, or wraps a present
     *         {@code Nullable} holding {@code null}
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected static void writeXmlScalar(final BufferedXmlWriter bw, final Type<?> type, final Object value, final XmlSerConfig config, final String what)
            throws ParsingException, IOException {
        writeXmlScalar(bw, type, value, config, what, null);
    }

    /**
     * The property name is formatted only if validation fails, avoiding a diagnostic String on each successful write.
     *
     * @throws ParsingException if the value contains a code unit that cannot be represented in XML 1.0, or wraps a present
     *         {@code Nullable} holding {@code null}
     * @throws IOException if writing the serialized content to {@code bw} fails
     */
    @SuppressWarnings("unchecked")
    static void writeXmlScalar(final BufferedXmlWriter bw, final Type<?> type, final Object value, final XmlSerConfig config, final String what,
            final String propName) throws ParsingException, IOException {
        if (value instanceof StringBuilder && type.getClass() == StringBuilderType.class
                || value instanceof StringBuffer && type.getClass() == StringBufferType.class) {
            // The built-in handlers write this same snapshot as text. Share it with validation;
            // custom handlers must still receive the original value through serializeTo below.
            final String text = value.toString();
            checkXmlText(text, what, propName);
            final char quotation = config == null ? 0 : config.getStringQuotation();
            if (quotation != 0) {
                bw.write(quotation);
            }
            bw.writeCharacter(text);
            if (quotation != 0) {
                bw.write(quotation);
            }
            return;
        }

        Object scalar = value;
        while (scalar != null) {
            final Object unwrapped = unwrapOptional(scalar);
            if (unwrapped == scalar) {
                break;
            }
            scalar = unwrapped;
        }
        if (scalar instanceof CharSequence) {
            checkXmlText(scalar.toString(), what, propName);
        } else if (scalar instanceof Character) {
            final char ch = (Character) scalar;
            if (!isXmlCharacter(ch)) {
                checkXmlText(scalar.toString(), what, propName);
            }
        }
        ((Type<Object>) type).serializeTo(bw, value, config);
    }

    /**
     * Writes a JSON payload of an {@code isJsonRawValue} property. The payload is inserted without JSON
     * escaping, but the three XML markup characters {@code &}, {@code <} and {@code >} are entity-escaped
     * so the document stays well-formed; the reader decodes them back to the verbatim payload. Quotes are
     * left alone.
     *
     * @param bw the writer to write to
     * @param json the JSON text to embed; {@code null} writes nothing
     * @throws ParsingException if the payload contains a code unit that cannot be represented in XML 1.0
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected static void writeRawJson(final BufferedXmlWriter bw, final String json) throws ParsingException, IOException {
        if (json == null) {
            return;
        }

        checkXmlText(json, "JSON raw value");

        int start = 0;

        for (int i = 0, len = json.length(); i < len; i++) {
            final char ch = json.charAt(i);
            final String replacement = rawJsonEntity(ch);

            if (replacement != null) {
                if (i > start) {
                    bw.write(json, start, i - start);
                }

                bw.write(replacement);
                start = i + 1;
            }
        }

        if (start == 0) {
            bw.write(json);
        } else if (start < json.length()) {
            bw.write(json, start, json.length() - start);
        }
    }

    /**
     * Unwraps an optional/nullable wrapper ({@code u.Optional}, the primitive {@code u.OptionalXxx}
     * wrappers, {@code u.Nullable}, {@code java.util.Optional}, {@code java.util.OptionalInt/Long/Double}).
     *
     * @param value the wrapper instance, or any other object
     * @return the wrapped element when present; {@code null} for an empty wrapper;
     *         {@code value} itself when it is not a wrapper
     * @throws ParsingException if a present-null Nullable cannot be represented without losing its presence state
     */
    protected static Object unwrapOptional(final Object value) throws ParsingException {
        if (value instanceof u.Optional) {
            return ((u.Optional<?>) value).orElseNull();
        } else if (value instanceof u.Nullable) {
            final u.Nullable<?> nullable = (u.Nullable<?>) value;
            if (nullable.isPresent() && nullable.get() == null) {
                throw new ParsingException("XML cannot represent Nullable.of(null) distinctly from Nullable.empty()");
            }
            return nullable.orElseNull();
        } else if (value instanceof java.util.Optional) {
            return ((java.util.Optional<?>) value).orElse(null);
        } else if (value instanceof u.OptionalInt) {
            return ((u.OptionalInt) value).isPresent() ? ((u.OptionalInt) value).get() : null;
        } else if (value instanceof u.OptionalLong) {
            return ((u.OptionalLong) value).isPresent() ? ((u.OptionalLong) value).get() : null;
        } else if (value instanceof u.OptionalDouble) {
            return ((u.OptionalDouble) value).isPresent() ? ((u.OptionalDouble) value).get() : null;
        } else if (value instanceof u.OptionalBoolean) {
            return ((u.OptionalBoolean) value).isPresent() ? ((u.OptionalBoolean) value).get() : null;
        } else if (value instanceof u.OptionalChar) {
            return ((u.OptionalChar) value).isPresent() ? ((u.OptionalChar) value).get() : null;
        } else if (value instanceof u.OptionalByte) {
            return ((u.OptionalByte) value).isPresent() ? ((u.OptionalByte) value).get() : null;
        } else if (value instanceof u.OptionalShort) {
            return ((u.OptionalShort) value).isPresent() ? ((u.OptionalShort) value).get() : null;
        } else if (value instanceof u.OptionalFloat) {
            return ((u.OptionalFloat) value).isPresent() ? ((u.OptionalFloat) value).get() : null;
        } else if (value instanceof java.util.OptionalInt) {
            return ((java.util.OptionalInt) value).isPresent() ? ((java.util.OptionalInt) value).getAsInt() : null;
        } else if (value instanceof java.util.OptionalLong) {
            return ((java.util.OptionalLong) value).isPresent() ? ((java.util.OptionalLong) value).getAsLong() : null;
        } else if (value instanceof java.util.OptionalDouble) {
            return ((java.util.OptionalDouble) value).isPresent() ? ((java.util.OptionalDouble) value).getAsDouble() : null;
        }

        return value;
    }

    /**
     * Returns whether {@code type} is a tuple-like handler ({@code Tuple1..9}, {@code Pair}, {@code Triple},
     * {@code Indexed}, {@code Timed}, {@code Map.Entry}) whose {@code serializeTo} writes its String slots unquoted under an
     * XML configuration (string quotation {@code 0}), so that a comma inside a slot corrupts the text.
     * Such values must use JSON quoting while retaining XML's embedded-value policy; calling
     * {@code stringOf} directly would bypass rejection of nested present-null Nullable values.
     *
     * @param type the type handler to inspect
     * @return {@code true} for the tuple-like handlers
     */
    protected static boolean isTupleLike(final Type<?> type) {
        final Class<?> cls = type.javaType();

        // Map.Entry (MapEntryType/ImmutableMapEntryType) writes {"k":v} with unquoted String slots under the XML
        // configuration just like the tuples: ("a,b", "c,d") was written as {"a,b":c,d}, which no reader parses back.
        return cls != null && (Tuple.class.isAssignableFrom(cls) || Pair.class.isAssignableFrom(cls) || Triple.class.isAssignableFrom(cls)
                || Indexed.class.isAssignableFrom(cls) || Timed.class.isAssignableFrom(cls) || Map.Entry.class.isAssignableFrom(cls));
    }

    /**
     * Writes the element of a present optional wrapper, or a tuple-like value, as element text that the
     * corresponding {@code Type.valueOf} reads back. Scalars go through
     * {@link #writeXmlScalar(BufferedXmlWriter, Type, Object, XmlSerConfig, String)}; collections, object
     * arrays, tuple-likes, beans and maps are written as their JSON text (entity-escaped), which is what the
     * wrapper's and the tuple's {@code valueOf} parse.
     *
     * @param bw the writer to write to
     * @param declaredElementType the declared element type, or {@code null}/{@code Object} to use the runtime type
     * @param value the unwrapped, non-{@code null} value
     * @param config the serialization configuration
     * @param what a short description of the value used in error messages
     * @throws ParsingException if the value, or a value nested in it, is a present {@code Nullable} holding {@code null}, or holds text
     *         that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    protected void writeUnwrappedValue(final BufferedXmlWriter bw, final Type<?> declaredElementType, final Object value, final XmlSerConfig config,
            final String what) throws ParsingException, IOException {
        writeUnwrappedValue(bw, declaredElementType, value, config, what, null);
    }

    /**
     * Same as {@link #writeUnwrappedValue(BufferedXmlWriter, Type, Object, XmlSerConfig, String)}, but a non-{@code null}
     * {@code propName} names the value as {@code Property '<propName>'} in error messages instead of {@code what}, so a
     * per-property caller does not have to build that description for every value it writes.
     *
     * @param bw the writer to write to
     * @param declaredElementType the declared element type, or {@code null}/{@code Object} to use the runtime type
     * @param value the unwrapped, non-{@code null} value
     * @param config the serialization configuration
     * @param what a short description of the value used in error messages when {@code propName} is {@code null}
     * @param propName the name of the property holding the value, or {@code null}
     * @throws ParsingException if the value, or a value nested in it, is a present {@code Nullable} holding {@code null}, or holds text
     *         that XML 1.0 cannot carry
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    @SuppressWarnings("unchecked")
    void writeUnwrappedValue(final BufferedXmlWriter bw, final Type<?> declaredElementType, final Object value, final XmlSerConfig config, final String what,
            final String propName) throws ParsingException, IOException {
        final Type<?> type = declaredElementType == null || declaredElementType.isObject() || !declaredElementType.javaType().isInstance(value)
                ? Type.of(value.getClass())
                : declaredElementType;

        if (type.isOptionalOrNullable()) {
            final Object nested = unwrapOptional(value);

            if (nested != null) {
                writeUnwrappedValue(bw, type.elementType(), nested, config, what, propName);
            }
        } else if (isTupleLike(type)) {
            // Preserve declared slot types and JSON quoting while propagating XML's policy into nested values.
            // stringOf uses the ordinary JSON defaults and would silently collapse present-null Nullable slots.
            writeEmbeddedJson(bw, type, value, config, false);
        } else if (type.isSerializable() && !type.isObjectArray() && !type.isCollection()) {
            writeXmlScalar(bw, type, value, config, what, propName);
        } else {
            writeEmbeddedJson(bw, value, config);
        }
    }

    /**
     * Extracts and converts a property value from an XML node to the appropriate Java type.
     * This method handles {@code null} values, type conversions, and formatted property values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Node propNode = element.getChildNodes().item(0);
     * Object value = getPropValue("age", Type.of(Integer.class), propInfo, propNode);
     * }</pre>
     *
     * @param propName the name of the property being extracted
     * @param propType the target type for the property value
     * @param propInfo property metadata including format information, or {@code null}
     * @param propNode the XML node containing the property value
     * @return the converted property value, or {@code null} if the node indicates a {@code null} value
     *         ({@code isNull="true"}, whatever text the node contains)
     * @throws ParsingException if a nonblank type attribute is not approved, or {@code propType} is {@code null}
     *         and the node does not indicate a {@code null} value
     */
    protected Object getPropValue(final String propName, final Type<?> propType, final PropInfo propInfo, final Node propNode) throws ParsingException {
        // Declared/configured scalar types and null markers do not waive type-attribute approval.
        return getPropValue(propName, propType, propInfo, propNode, resolvePresentTypeAttribute(XmlUtil.getAttribute(propNode, XmlConstants.TYPE)));
    }

    /**
     * The body of {@link #getPropValue(String, Type, PropInfo, Node)} for a node whose type attribute the caller has
     * already read and approved, so that a reader which resolved the attribute to select the node's type does not
     * resolve it again for the text conversion (the Abacus DOM reader passes its single resolution down here).
     *
     * @param propName the name of the property being extracted
     * @param propType the target type for the property value
     * @param propInfo property metadata including format information, or {@code null}
     * @param propNode the XML node containing the property value
     * @param attributeType the node's approved type attribute, or {@code null} when it is absent or blank
     * @return the converted property value, or {@code null} if the node indicates a {@code null} value
     * @throws ParsingException if {@code propType} is {@code null} and the node does not indicate a {@code null} value
     */
    static Object getPropValue(final String propName, final Type<?> propType, final PropInfo propInfo, final Node propNode, final Type<?> attributeType)
            throws ParsingException {
        // The null marker wins over any text, as it does in the SAX and StAX readers.
        final NamedNodeMap attributes = propNode.getAttributes();

        if (attributes != null) {
            final Node attributeNode = attributes.getNamedItem(XmlConstants.IS_NULL);

            if ((attributeNode != null) && Boolean.parseBoolean(attributeNode.getNodeValue())) { //NOSONAR
                return null;
            }
        }

        final String txtValue = XmlUtil.getTextContent(propNode);

        if (propType == null) {
            throw new ParsingException("Can't parse property " + propName + " with value: " + txtValue);
        }

        if (propInfo != null && propInfo.hasFormat) {
            return propInfo.readPropValue(txtValue);
        } else {
            return valueOfAdoptedText(refineObjectWrapper(propType, attributeType), attributeType, txtValue);
        }
    }

    /**
     * Converts element text with a type that may have been selected by the element's type attribute. A codec that cannot
     * construct a value from text at all (an abstract class such as {@code Number}, whose {@code valueOf} throws
     * {@code UnsupportedOperationException}) is a document error when the attribute selected it: the attribute names a
     * class that can never hold the element's text, which is reported as a {@link ParsingException} naming the attribute,
     * the same on every backend. Value errors of a working codec ({@code NumberFormatException} for
     * {@code <open type="Long">x</open>}, the {@code IllegalArgumentException} of an unparsable date) propagate as the codec
     * reports them, as they do for a declared type and in the JSON reader.
     *
     * @param type the type to convert with
     * @param attributeType the element's approved type attribute, or {@code null}
     * @param text the element text
     * @return the converted value
     * @throws ParsingException if {@code type} was selected by the type attribute and cannot read text at all
     */
    protected static Object valueOfAdoptedText(final Type<?> type, final Type<?> attributeType, final String text) throws ParsingException {
        try {
            return type.valueOf(text);
        } catch (final UnsupportedOperationException e) {
            if (attributeType == null || attributeType.javaType() != type.javaType()) {
                throw e;
            }

            throw new ParsingException("XML type attribute '" + attributeType.name() + "' names " + ClassUtil.getCanonicalClassName(type.javaType())
                    + ", whose values cannot be read from element text", e);
        }
    }

    /**
     * Refines a wrapper's unconstrained payload slots with approved metadata while keeping the wrapper itself.
     * Specific declared/configured arguments retain their existing precedence and generic constraints.
     * Uninstantiable container metadata on an Object payload supplies its arguments through an open mutable schema.
     *
     * @param declaredType the declared or configured type, possibly {@code null}
     * @param attributeType the approved payload type, possibly {@code null}
     * @return the refined wrapper type, or the unchanged declaration when no refinement applies
     */
    protected static Type<?> refineObjectWrapper(final Type<?> declaredType, final Type<?> attributeType) {
        if (declaredType != null && declaredType.isOptionalOrNullable() && declaredType.elementType() != null) {
            return refineDeclaredSchema(declaredType, attributeType);
        }
        return declaredType;
    }

    /**
     * Selects the element schema for JSON-form collection text without mistaking metadata for a declaration.
     * Explicit element constraints retain precedence over the global element configuration; an originally
     * open Object slot uses the reader's effective configured/metadata type. Keep refinements inside an
     * explicit nested schema, including its construction hints.
     *
     * @param declaredType the collection schema before metadata refinement, possibly {@code null}
     * @param refinedType the resolved collection schema
     * @param effectiveElementType the element type selected by the XML reader
     * @return the element schema for the text reader
     */
    protected static Type<?> collectionTextElementType(final Type<?> declaredType, final Type<?> refinedType, final Type<?> effectiveElementType) {
        return declaredType != null && declaredType.isCollection() && declaredType.elementType() != null && !declaredType.elementType().isObject()
                ? refinedType.elementType()
                : effectiveElementType;
    }

    /**
     * Returns the payload type of a declared {@code java.util.Optional} / {@code u.Optional} / {@code u.Nullable}
     * type, or the type itself when it is not such a wrapper.
     *
     * @param type the declared or refined schema, possibly {@code null}
     * @return the wrapper's element type, or {@code type} itself
     */
    protected static Type<?> optionalPayload(final Type<?> type) {
        return type != null && type.isOptionalOrNullable() && type.elementType() != null ? type.elementType() : type;
    }

    /**
     * Returns an open mutable schema carrying a container's arguments for a container class the readers cannot
     * instantiate (the {@code List.of(..)}/{@code Set.of(..)}/{@code Map.of(..)} runtime classes). An open Object
     * slot adopts this schema instead of dropping the metadata: reading fills an {@code ArrayList},
     * {@code LinkedHashSet} or {@code LinkedHashMap}, so the adopted value stays mutable.
     *
     * @param metadata the approved type information
     * @return the mutable schema with the metadata's arguments, or {@code null} when the metadata is not a container
     */
    protected static Type<?> mutableContainerSchema(final Type<?> metadata) {
        if (metadata.isCollection() && metadata.elementType() != null) {
            return Type.of((Set.class.isAssignableFrom(metadata.javaType()) ? "LinkedHashSet<" : "ArrayList<") + metadata.elementType().name() + ">");
        }
        if (metadata.isMap() && metadata.parameterTypes().size() == 2) {
            final List<Type<?>> parameters = metadata.parameterTypes();
            return Type.of("LinkedHashMap<" + parameters.get(0).name() + ", " + parameters.get(1).name() + ">");
        }
        return null;
    }

    /**
     * Returns the component global element configuration imposes on a container slot, or {@code null} when the slot
     * keeps its own component. A slot is open to configuration only when its declaration imposed no constraint
     * (a declared {@code Object} or an array/collection with an {@code Object} component, including wrapped
     * forms); an explicitly declared component always wins over configuration.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the configured component for an open slot, otherwise {@code null}
     */
    protected static Type<?> slotComponentType(final Type<?> declaredSlot, final Type<?> configuredElementType) {
        final Type<?> declaredPayload = optionalPayload(declaredSlot);
        // Metadata can refine List<Object> before its text or child elements are read. Configuration still
        // owns that originally open component, just as it owns a container adopted by an Object slot.
        // The open component may be nested: Object[][], List<Object[]> and Map<String, Object[]> are open at their
        // innermost array level. A collection nested in a collection keeps its own declaration and metadata refinement.
        final boolean openComponent = declaredPayload == null || declaredPayload.javaType() == Object.class || hasConfigurableComponent(declaredPayload, false);
        return openComponent && configuredElementType != null && !configuredElementType.isObject() ? configuredElementType : null;
    }

    /**
     * Returns whether the global element configuration types the elements of a container slot whose own element is
     * open. It does not when the slot's declared element is a container ({@code List<Object[]>} at the root): that
     * element keeps its shape and the configuration applies inside it, as below the root. (An item the configuration
     * typed is read with a configuration without element type, see {@link #itemConfig}, so its own elements are never
     * typed again.)
     *
     * @param config the deserialization configuration
     * @param declaredSlot the slot's declared schema, possibly a wrapper, possibly {@code null}
     * @return {@code true} when the configured element type applies to the slot's elements
     */
    protected static boolean elementConfigurationApplies(final XmlDeserConfig config, final Type<?> declaredSlot) {
        final Type<?> configured = config.getElementType();
        if (configured == null || configured.isObject()) {
            return false;
        }
        final Type<?> declared = optionalPayload(declaredSlot);
        return declared == null || !(declared.isCollection() || declared.isArray()) || declared.elementType() == null
                || !isContainerArgument(declared.elementType());
    }

    /**
     * Returns whether a container item is a configured container component: the slot's declared element is open, so the
     * configuration supplied the item type, and that type is a collection or an array, whose elements are the
     * component's own. A declared container element ({@code Object[]} in {@code Object[][]}) is never a configured
     * item, even when it equals the configured type: the configuration applies inside it. A configured bean component
     * keeps typing the open collections nested in the bean.
     *
     * @param config the deserialization configuration
     * @param declaredSlot the slot's declared schema, possibly a wrapper, possibly {@code null}
     * @param itemType the item type the reader resolved, possibly {@code null}
     * @return {@code true} for an item that is a configured container component
     */
    protected static boolean isConfiguredItem(final XmlDeserConfig config, final Type<?> declaredSlot, final Type<?> itemType) {
        final Type<?> configured = config.getElementType();
        if (configured == null || !(configured.isCollection() || configured.isArray()) || itemType == null
                || !sameType(configured, optionalPayload(itemType))) {
            return false;
        }
        final Type<?> declared = optionalPayload(declaredSlot);
        final Type<?> declaredElement = declared != null && (declared.isArray() || declared.isCollection()) ? declared.elementType() : null;
        return declaredElement == null || declaredElement.isObject();
    }

    /**
     * Returns the configuration a container item is read with: an item that is a configured container component is
     * read with a copy of the configuration without that element type, so the configured component types the slot's
     * open elements once and the component's own elements are never typed again ({@code [[1,2],[3]]} under a
     * {@code List<Object>} component holds two lists, not lists of lists), as the compact reader applies the
     * component at the slot's open level only; any other item keeps the configuration.
     *
     * @param config the deserialization configuration
     * @param declaredSlot the slot's declared schema, possibly a wrapper, possibly {@code null}
     * @param itemType the item type the reader resolved, possibly {@code null}
     * @return the configuration for the item
     */
    protected static XmlDeserConfig itemConfig(final XmlDeserConfig config, final Type<?> declaredSlot, final Type<?> itemType) {
        return isConfiguredItem(config, declaredSlot, itemType) ? config.copy().setElementType(objType) : config;
    }

    /**
     * Returns the type the structured items of a container slot are read with, or {@code null} when the slot's own
     * element applies: for an array, {@link #arrayItemType}; for a collection whose declared element is itself a
     * container ({@code List<Object[]>}), that element with its open form kept under configuration
     * ({@link #refinedItemType}); otherwise the configured component of an open element or the metadata-refined
     * component ({@link #arrayComponentOverride}). The configured component of a nested open level applies inside the
     * item, never as the item type.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param schema the container schema after metadata refinement, possibly a wrapper
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the item type, or {@code null}
     */
    protected static Type<?> containerItemType(final Type<?> declaredSlot, final Type<?> schema, final Type<?> configuredElementType) {
        final Type<?> payload = optionalPayload(schema);
        if (payload != null && payload.isArray()) {
            return arrayItemType(declaredSlot, payload, configuredElementType);
        }
        final Type<?> declared = optionalPayload(declaredSlot);
        final Type<?> declaredElement = declared != null && declared.isCollection() ? declared.elementType() : null;
        if (declaredElement != null && (declaredElement.isArray() || declaredElement.isCollection())) {
            return refinedItemType(declaredElement, payload, configuredElementType);
        }
        return arrayComponentOverride(declaredSlot, payload, configuredElementType);
    }

    /**
     * Returns whether a requested map argument is a container that the global map key or value type must not replace:
     * at the root an explicit global type takes a requested scalar, bean or open argument's place, but a requested
     * array, collection or map keeps its shape, and the global type applies inside it as it does below the root.
     *
     * @param declaredType the requested map schema, possibly a wrapper, possibly {@code null}
     * @param argument 0 for the key argument, 1 for the value argument
     * @return {@code true} when the requested argument is a container
     */
    protected static boolean keepsRequestedContainer(final Type<?> declaredType, final int argument) {
        final Type<?> declared = optionalPayload(declaredType);
        return declared != null && declared.isMap() && declared.parameterTypes().size() == 2 && isContainerArgument(declared.parameterTypes().get(argument));
    }

    /** Whether two types are the same type, by identity or by name (the readers construct equal types from names). */
    private static boolean sameType(final Type<?> a, final Type<?> b) {
        return a == b || (a != null && b != null && a.name().equals(b.name()));
    }

    private static boolean isContainerArgument(final Type<?> type) {
        return type.isArray() || type.isCollection() || type.isMap();
    }

    /**
     * Reads the JSON text of a collection with the element type the reader resolved, through
     * {@link #readContainerText}, so that the open components nested in that element ({@code List<Object[]>} with a
     * configured {@code Long}) are typed as they are for child elements.
     *
     * @param text the collection's JSON text
     * @param containerType the collection schema
     * @param targetClass the construction class, or {@code null} for the schema's own class
     * @param elementType the element type the reader resolved
     * @param config the XML configuration, possibly {@code null}
     * @return the decoded collection
     */
    protected static Object readCollectionText(final String text, final Type<?> containerType, final Class<?> targetClass, final Type<?> elementType,
            final XmlDeserConfig config) {
        final Class<?> construction = targetClass != null ? targetClass : containerType.javaType();
        if (Strings.isBlank(text)) {
            // Blank text is the empty collection the JSON reader builds.
            return CompactJsonParser.withConfiguredComponent(null, containerType,
                    () -> jsonParser.deserialize(text, compactJsonConfig(config).setElementType(elementType), construction));
        }
        // The resolved element is the JSON reader's element configuration (an open element stays open: a configured
        // item's own elements are never typed again); the configured component reaches the open elements nested in
        // the element at parse time (see CompactJsonParser.nestedElementType), as it does for child elements.
        final Type<?> schema = Type.of(construction.getCanonicalName() + "<" + elementType.name() + ">");
        final Type<?> configuredComponent = config != null && config.getElementType() != null && !config.getElementType().isObject()
                && hasNestedOpenComponent(schema) ? config.getElementType() : null;
        return CompactJsonParser.withConfiguredComponent(configuredComponent, schema,
                () -> typeNestedOpenComponents(jsonParser.deserialize(text.strip(), compactJsonConfig(config).setElementType(elementType), construction),
                        schema, configuredComponent));
    }

    /**
     * Returns the nesting depth, counted in bracketed values below the top container, of the container whose open
     * elements the global element configuration types: {@code 1} for {@code Object[][]}, {@code List<Object[]>} and
     * {@code Map<String, Object[]>}, {@code 0} when the top container's own elements are open ({@code List<Object>}),
     * {@code -1} when the configuration owns no element of the schema. The ownership rules are those of
     * {@link #hasConfigurableComponent}: arrays at any depth, a collection's direct element, a map's array or
     * collection value; a collection nested in a collection keeps its own element. A map between two levels is
     * entered without a bracket and does not count.
     *
     * @param schema the schema the text is read with, possibly a wrapper
     * @return the depth, or {@code -1}
     */
    static int openComponentDepth(final Type<?> schema) {
        Type<?> type = optionalPayload(schema);
        int depth = 0;
        boolean insideCollection = false;
        while (type != null) {
            if (type.isArray() || type.isCollection()) {
                final Type<?> element = type.elementType();
                if (element == null || (type.isCollection() && insideCollection)) {
                    return -1;
                }
                if (element.javaType() == Object.class) {
                    return depth;
                }
                if (element.isArray() || element.isCollection()) {
                    insideCollection = type.isCollection();
                    type = element;
                    depth++;
                } else if (element.isMap()) {
                    insideCollection = false;
                    type = element;
                } else {
                    return -1;
                }
            } else if (type.isMap() && type.parameterTypes().size() == 2) {
                final Type<?> value = type.parameterTypes().get(1);
                if (!(value.isArray() || value.isCollection())) {
                    return -1;
                }
                insideCollection = false;
                type = value;
                depth++;
            } else {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Returns whether global element configuration owns a component inside {@code type}: the {@code Object} element of
     * an array at any array depth, of a collection, or of a map's array or collection value. The element of a collection
     * nested in a collection is not configuration's: that declared container keeps its own element and any metadata
     * refinement of it.
     *
     * @param type the declared payload or one of its arguments
     * @param insideCollection whether {@code type} is the element of an enclosing collection
     * @return {@code true} when configuration types an element inside {@code type}
     */
    private static boolean hasConfigurableComponent(final Type<?> type, final boolean insideCollection) {
        if (type.isArray()) {
            final Type<?> element = type.elementType();
            return element != null && (element.javaType() == Object.class || hasConfigurableComponent(element, false));
        }
        if (type.isCollection()) {
            final Type<?> element = type.elementType();
            return !insideCollection && element != null && (element.javaType() == Object.class || hasConfigurableComponent(element, true));
        }
        if (type.isMap() && type.parameterTypes().size() == 2) {
            final Type<?> value = type.parameterTypes().get(1);
            return (value.isArray() || value.isCollection()) && hasConfigurableComponent(value, false);
        }
        return false;
    }

    /**
     * Returns the innermost element of nested arrays and collections: {@code Object} for {@code Object[][]},
     * {@code List<Object[]>} and {@code Object} itself, {@code Long} for {@code List<Long[]>}, and a map or scalar as is.
     *
     * @param type the declared slot payload
     * @return the innermost component type
     */
    private static Type<?> innermostComponent(final Type<?> type) {
        Type<?> component = type;
        while ((component.isArray() || component.isCollection()) && component.elementType() != null) {
            component = component.elementType();
        }
        return component;
    }

    /**
     * Returns whether a slot has an open component below its top level: the elements of {@code Object[][]},
     * {@code List<Object[]>} or {@code Map<String, Object[]>}, which the JSON reader's root element configuration
     * cannot reach.
     *
     * @param schema the slot schema, possibly a wrapper
     * @return {@code true} when a nested array or collection has an {@code Object} innermost component
     */
    private static boolean hasNestedOpenComponent(final Type<?> schema) {
        final Type<?> payload = optionalPayload(schema);
        if (payload == null) {
            return false;
        }
        final Type<?> nested = payload.isArray() || payload.isCollection() ? payload.elementType()
                : payload.isMap() && payload.parameterTypes().size() == 2 ? payload.parameterTypes().get(1) : null;
        return nested != null && (nested.isArray() || nested.isCollection()) && hasConfigurableComponent(nested, payload.isCollection());
    }

    /**
     * Types the innermost open components of a container read from compact text. The compact reader already types
     * the open elements of nested containers at parse time (see {@code CompactJsonParser.nestedElementType}); this
     * pass remains for a configured component below the root (the elements of {@code Object[][]},
     * {@code List<Object[]>} or {@code Map<String, Object[]>}) in a container the reader built without it, converting
     * as the child-element readers convert each scalar child: a JSON-native token becomes the configured type's value
     * of its text. Elements already of the configured type are left alone. Arrays and lists are updated in place; the
     * structure and its runtime classes are unchanged.
     *
     * @param value the decoded container, possibly wrapped, possibly {@code null}
     * @param schema the slot schema the text was decoded with
     * @param component the configured component type
     * @return {@code value}
     */
    static Object typeNestedOpenComponents(final Object value, final Type<?> schema, final Type<?> component) {
        if (value == null || schema == null || component == null) {
            return value;
        }
        if (schema.isOptionalOrNullable() && schema.elementType() != null) {
            typeNestedOpenComponents(unwrapOptional(value), schema.elementType(), component);
            return value;
        }
        if (schema.isArray()) {
            final Type<?> element = schema.elementType();
            if (element != null && value instanceof Object[] array) {
                if (element.javaType() == Object.class) {
                    for (int i = 0; i < array.length; i++) {
                        array[i] = typedComponent(array[i], component);
                    }
                } else if (isNestingType(element)) {
                    for (final Object e : array) {
                        typeNestedOpenComponents(e, element, component);
                    }
                }
            }
        } else if (schema.isCollection()) {
            final Type<?> element = schema.elementType();
            if (element != null && value instanceof Collection<?> collection) {
                if (element.javaType() == Object.class) {
                    if (value instanceof List<?> list) {
                        @SuppressWarnings("unchecked")
                        final ListIterator<Object> iterator = ((List<Object>) list).listIterator();
                        while (iterator.hasNext()) {
                            final Object elementValue = iterator.next();
                            final Object typedValue = typedComponent(elementValue, component);
                            // The compact reader normally converted this element already. Avoid an observable
                            // replacement, especially for immutable lists and copy-on-write snapshot iterators.
                            if (typedValue != elementValue) {
                                iterator.set(typedValue);
                            }
                        }
                    }
                } else if (element.isArray() || element.isOptionalOrNullable()) {
                    // A collection nested in a collection keeps its own element declaration and metadata refinement.
                    for (final Object e : collection) {
                        typeNestedOpenComponents(e, element, component);
                    }
                }
            }
        } else if (schema.isMap() && schema.parameterTypes().size() == 2 && value instanceof Map<?, ?> map) {
            // A map's own Object value is schema, never an element slot; only its array/collection values nest.
            final Type<?> valueType = schema.parameterTypes().get(1);
            if (isNestingType(valueType) && !valueType.isMap()) {
                for (final Object v : map.values()) {
                    typeNestedOpenComponents(v, valueType, component);
                }
            }
        }
        return value;
    }

    private static boolean isNestingType(final Type<?> type) {
        return type.isArray() || type.isCollection() || type.isMap() || type.isOptionalOrNullable();
    }

    private static Object typedComponent(final Object element, final Type<?> component) {
        if (element == null) {
            return null;
        }
        final Class<?> componentClass = component.javaType();
        // Primitive codecs store their exact boxed values in open containers. Re-parsing those values would
        // allocate replacement boxes and attempt to mutate an already typed immutable/copy-on-write list.
        if ((componentClass.isPrimitive() ? ClassUtil.wrap(componentClass) : componentClass).isInstance(element)) {
            return element;
        }
        // The child-element readers convert each scalar's text; a JSON-native token is converted from its text too.
        return component.valueOf(element instanceof CharSequence ? element.toString() : N.stringOf(element));
    }

    /**
     * Applies {@link #slotComponentType} to a container schema adopted or refined for a slot: when global element
     * configuration overrides the component of an originally open slot, a collection schema is rebuilt with that
     * component (wrapper and construction class preserved). Array and map schemas are returned unchanged: an array
     * keeps its declared runtime class and has only its elements typed at read time (see {@link #readArrayText}),
     * and a map's key/value types are schema, never element configuration.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param schema the container schema after metadata refinement, possibly a wrapper, possibly {@code null}
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the schema with the configured component, or {@code schema} unchanged
     */
    protected static Type<?> withSlotComponent(final Type<?> declaredSlot, final Type<?> schema, final Type<?> configuredElementType) {
        final Type<?> component = slotComponentType(declaredSlot, configuredElementType);
        if (component == null || schema == null) {
            return schema;
        }
        final Type<?> declaredPayload = optionalPayload(declaredSlot);
        if (schema.isOptionalOrNullable() && schema.elementType() != null) {
            final Type<?> payload = schema.elementType();
            final Type<?> replaced = replaceSlotComponent(declaredPayload, payload, component, false);
            return replaced == payload ? schema : Type.of(schema.javaType().getCanonicalName() + "<" + replaced.name() + ">");
        }
        return replaceSlotComponent(declaredPayload, schema, component, false);
    }

    /**
     * Applies the global map key and value types to a map slot whose declared key or value is open, ahead of the
     * arguments the slot's metadata supplied: a declared argument wins over configuration, which wins over metadata,
     * for child elements and compact text alike (the JSON reader fills a typed map slot's open arguments the same
     * way). The metadata's implementation still constructs the map where it can hold the configured values; a
     * list-valued implementation that cannot falls back to ordinary construction (see
     * {@link #mapCreatorAndConverter(Class, Type)} and {@link #readContainerText}). A declared list-valued map, a
     * slot without open arguments and a configuration without global map types leave the schema unchanged.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param schema the schema after metadata refinement, possibly a wrapper, possibly {@code null}
     * @param config the deserialization configuration, possibly {@code null}
     * @return the schema with the configured arguments, or {@code schema} unchanged
     */
    protected static Type<?> withSlotMapTypes(final Type<?> declaredSlot, final Type<?> schema, final XmlDeserConfig config) {
        if (config == null || schema == null || (config.getMapKeyType() == null && config.getMapValueType() == null)) {
            return schema;
        }
        final Type<?> declared = optionalPayload(declaredSlot);
        if (declared == null || !declared.isMap() || declared.parameterTypes().size() != 2 || isListValuedMap(declared.javaType())) {
            return schema;
        }
        final Type<?> declaredKey = declared.parameterTypes().get(0);
        final Type<?> declaredValue = declared.parameterTypes().get(1);
        final Type<?> key = declaredKey.isObject() && config.getMapKeyType() != null && !config.getMapKeyType().isObject() ? config.getMapKeyType()
                : declaredKey;
        final Type<?> value = declaredValue.isObject() && config.getMapValueType() != null && !config.getMapValueType().isObject() ? config.getMapValueType()
                : declaredValue;
        if (key == declaredKey && value == declaredValue) {
            return schema;
        }
        if (schema.isOptionalOrNullable() && schema.elementType() != null) {
            final Type<?> payload = schema.elementType();
            final Type<?> filled = withSlotMapTypes(declared, payload, config);
            return filled == payload ? schema : Type.of(schema.javaType().getCanonicalName() + "<" + filled.name() + ">");
        }
        if (!schema.isMap()) {
            return schema;
        }
        // The filled declaration is refined again by the schema, which then supplies only what is still open.
        return refineDeclaredSchema(Type.of(declared.javaType().getCanonicalName() + "<" + key.name() + ", " + value.name() + ">"), schema);
    }

    /**
     * Rebuilds a refined container schema so that configuration keeps the components it owns. A collection whose
     * declared element is {@code Object} takes the configured component. A collection or map whose declared element or
     * value is itself an array or collection with an open innermost component keeps that declared argument: metadata
     * may supply the container implementation, but may not narrow a nested {@code Object[]} to a {@code Long[]} while
     * configuration types the elements. The slot's own array keeps its runtime class here (see
     * {@link #openArrayDestination}); a nested array returns to its declared, open form. A map's key type and a
     * list-valued map are never rewritten.
     *
     * @param declared the declared schema of the same slot, possibly {@code null}
     * @param schema the refined schema
     * @param component the configured scalar component
     * @param nested whether {@code schema} is an argument of an enclosing container rather than the slot itself
     * @return the rebuilt schema, or {@code schema} unchanged
     */
    private static Type<?> replaceSlotComponent(final Type<?> declared, final Type<?> schema, final Type<?> component, final boolean nested) {
        if (schema.isCollection() && schema.elementType() != null) {
            final Type<?> declaredElement = declared != null && declared.isCollection() ? declared.elementType() : null;
            final Type<?> element = declaredElement == null || declaredElement.javaType() == Object.class ? component
                    : declaredElement.isArray() ? replaceSlotComponent(declaredElement, schema.elementType(), component, true) : schema.elementType();
            return schema.elementType().equals(element) ? schema : Type.of(schema.javaType().getCanonicalName() + "<" + element.name() + ">");
        }
        if (schema.isArray()) {
            // An array keeps its declared runtime class: configuration types its elements at read time (an Object[] of
            // Long, never a Long[], which would ArrayStoreException on a non-Long store and would not even be an Object[]
            // for a primitive component). The slot's own array is restored by openArrayDestination.
            return nested && declared != null && declared.isArray() && !schema.equals(declared) ? declared : schema;
        }
        if (schema.isMap() && schema.parameterTypes().size() == 2 && declared != null && declared.isMap() && declared.parameterTypes().size() == 2
                && !isListValuedMap(schema.javaType())) {
            // A map's key/value types are schema supplied by metadata or declaration, never global element
            // configuration; only a declared value slot that is an open nested array or collection is restored.
            final Type<?> declaredValue = declared.parameterTypes().get(1);
            if ((declaredValue.isArray() || declaredValue.isCollection()) && hasConfigurableComponent(declaredValue, false)) {
                final Type<?> value = replaceSlotComponent(declaredValue, schema.parameterTypes().get(1), component, true);
                if (!schema.parameterTypes().get(1).equals(value)) {
                    return Type.of(schema.javaType().getCanonicalName() + "<" + schema.parameterTypes().get(0).name() + ", " + value.name() + ">");
                }
            }
        }
        return schema;
    }

    /**
     * Reads one compact JSON document without an override for a nested component. The per-key and global map
     * configuration still applies. Even an open array or collection needs its own component scope: a custom codec
     * can start this independent read while another compact document's configured component is active.
     *
     * @param json the compact JSON text
     * @param config the XML configuration, possibly {@code null}
     * @param targetType the complete type to read
     * @param <T> the result type
     * @return the decoded value
     */
    protected static <T> T readCompactJson(final String json, final XmlDeserConfig config, final Type<T> targetType) {
        return (T) CompactJsonParser.withConfiguredComponent(null, targetType, () -> jsonParser.deserialize(json, compactJsonConfig(config), targetType));
    }

    /**
     * Reads JSON-form array text for a slot, always constructing the declared array class. An originally open
     * array slot lets global element configuration type the elements (an {@code Object[]} of {@code Long}, or
     * stringified tokens for a String configuration), while a declared reference component (a {@code Long[]})
     * keeps its own type; configuration never reifies the array. A primitive component (char, boolean, int, ...),
     * declared or adopted from metadata by an open slot, always keeps its established {@code Type.valueOf} text codec
     * rather than the JSON element conversion.
     *
     * Unicode whitespace around the JSON text is ignored on every backend, as it is around a primitive or String array's
     * text, which the array type's own {@code valueOf} strips. An open component is read through the compact JSON
     * reader, so the configuration's per-key value types and global map types reach the maps inside.
     *
     * @param text the array's JSON text
     * @param arrayType the declared array schema after metadata refinement
     * @param componentOverride the component selected by {@link #slotComponentType}, or {@code null}
     * @param config the XML configuration whose value types apply to element maps, possibly {@code null}
     * @return the decoded array, an instance of {@code arrayType}'s class
     */
    protected static Object readArrayText(final String text, final Type<?> arrayType, final Type<?> componentOverride, final XmlDeserConfig config) {
        final Type<?> element = componentOverride != null ? componentOverride : arrayType.elementType();
        final Type<?> component = arrayType.elementType();
        // A primitive component, declared or adopted from metadata, keeps its established Type.valueOf text codec
        // whatever the configuration says: char[] reads a single character as itself and a multi-character region
        // as a code point ('1' vs '65'->'A'), where the JSON element decoder would instead read a number as a
        // UTF-16 code unit (boolean[] reads 1 as true and 0 or -1 as false either way, quoted or not). A declared
        // String[] without an override keeps valueOf as well: it strips Unicode boundary padding and stringifies
        // bare tokens. Everything else — a configured or open component (Long, String, ...) — goes through the
        // JSON element conversion, which also stringifies bare numeric/boolean tokens for a String component.
        if (Strings.isBlank(text) || element == null || (component != null && component.javaType().isPrimitive())
                || (componentOverride == null && element.isString())) {
            return arrayType.valueOf(text);
        }
        final String json = text.strip();
        if (element.isObject()) {
            // An open component: the compact JSON reader, so the per-key value types and the global map types reach the
            // maps inside; unquoted tokens keep their JSON types either way.
            return readCompactJson(json, config, arrayType);
        }
        final Type<?> declaredComponent = arrayType.elementType();
        if (componentOverride != null && declaredComponent != null && declaredComponent.javaType() != Object.class
                && !declaredComponent.javaType().isPrimitive() && !declaredComponent.equals(componentOverride)
                && declaredComponent.javaType().isAssignableFrom(element.javaType())) {
            // A declared reference component refined by metadata (Long for Number[], a specialized map for Map[]): the
            // JSON reader's element configuration types only an Object component, so the elements are read as a list of
            // the refined component and stored in the declared array class.
            final List<?> elements = readCompactJson(json, config, Type.of("List<" + element.name() + ">"));
            return collectionToArray(elements, arrayType);
        }
        // The configured component also types the open elements of nested containers at parse time (see
        // CompactJsonParser.nestedElementType), so a parameterized component keeps its arguments and a String component
        // keeps each token's text at every level; typeNestedOpenComponents remains for containers built without it.
        final Object array = CompactJsonParser.withConfiguredComponent(componentOverride, arrayType,
                () -> jsonParser.deserialize(json, compactJsonConfig(config).setElementType(element), arrayType));
        return componentOverride != null && hasNestedOpenComponent(arrayType) ? typeNestedOpenComponents(array, arrayType, componentOverride) : array;
    }

    /**
     * Returns the destination array type for an array read. Runtime metadata never narrows a declared array class:
     * a declared reference component ({@code Number[]}, {@code Map<String, Object>[]}) keeps its array, and only its
     * elements take the metadata's component (see {@link #arrayComponentOverride}). An originally open slot
     * ({@code Object[]}, {@code Object[][]} or a wrapper of them) also keeps its declared array while global element
     * configuration types the elements, because the metadata's concrete array ({@code Integer[]}) could not hold the
     * configured elements. Without configuration, metadata is the only component information an open slot has, so it
     * decides the runtime class as well.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param schema the array schema after metadata refinement, possibly {@code null}
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the declared array type when it must be constructed, otherwise {@code schema}
     */
    protected static Type<?> openArrayDestination(final Type<?> declaredSlot, final Type<?> schema, final Type<?> configuredElementType) {
        if (schema != null && schema.isArray()) {
            final Type<?> declaredPayload = optionalPayload(declaredSlot);

            if (declaredPayload != null && declaredPayload.isArray() && schema.javaType() != declaredPayload.javaType()
                    && (innermostComponent(declaredPayload).javaType() != Object.class || slotComponentType(declaredSlot, configuredElementType) != null)) {
                return declaredPayload;
            }
        }

        return schema;
    }

    /**
     * A slot's schema after metadata refinement and configuration, with the type to construct for it. The refined
     * schema types the elements ({@link #arrayComponentOverride}) and reads the text ({@code readWrappedText}); the
     * destination is the type to construct, the declared array where {@link #openArrayDestination} keeps it.
     *
     * @param refined the schema with the configuration's component and map types applied
     * @param destination the type to construct: {@code refined} itself unless the slot's declared array must be kept
     */
    protected record SlotSchema(Type<?> refined, Type<?> destination) {
    }

    /**
     * Refines a slot's schema in one step, so that no reader can apply the configuration's component to a slot without
     * also restoring the slot's declared array (the two were separate calls at every site, and a site that forgot the
     * second silently narrowed an open {@code Object[]} to the metadata's class): global element configuration fills an
     * originally open component ({@link #withSlotComponent}), the global map key and value types fill a declared map
     * slot's open arguments ({@link #withSlotMapTypes}), and {@link #openArrayDestination} decides the array to
     * construct from the refined schema.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param schema the schema after metadata refinement, possibly a wrapper, possibly {@code null}
     * @param config the deserialization configuration, possibly {@code null}
     * @return the refined schema and the type to construct
     */
    protected static SlotSchema refineSlot(final Type<?> declaredSlot, final Type<?> schema, final XmlDeserConfig config) {
        final Type<?> configuredElementType = config == null ? null : config.getElementType();
        final Type<?> refined = withSlotMapTypes(declaredSlot, withSlotComponent(declaredSlot, schema, configuredElementType), config);
        return new SlotSchema(refined, openArrayDestination(declaredSlot, refined, configuredElementType));
    }

    /**
     * Same as {@link #refineSlot(Type, Type, XmlDeserConfig)} with the global element configuration alone, for the
     * readers that apply the global map types where they resolve a node's type attribute (the DOM property readers):
     * the configured component fills an originally open component and the slot's declared array is restored, while a
     * declared map slot's arguments are left as the caller refined them.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param schema the schema after metadata refinement, possibly a wrapper, possibly {@code null}
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the refined schema and the type to construct
     */
    protected static SlotSchema refineSlot(final Type<?> declaredSlot, final Type<?> schema, final Type<?> configuredElementType) {
        final Type<?> refined = withSlotComponent(declaredSlot, schema, configuredElementType);
        return new SlotSchema(refined, openArrayDestination(declaredSlot, refined, configuredElementType));
    }

    /**
     * Whether {@link #refineSlot(Type, Type, XmlDeserConfig)} (or its element-configuration form) can change a slot's
     * schema at all. Global element configuration ({@link #withSlotComponent}), the global map types
     * ({@link #withSlotMapTypes}) and the declared-array restoration ({@link #openArrayDestination}) only ever rewrite
     * a map, collection, array or Optional/Nullable schema; for any other schema (a scalar, a bean, an open
     * {@code Object}) all three hand back the schema itself, so {@code refineSlot} returns it as both the refined type
     * and the destination. The property and entry readers skip {@code refineSlot} for those, which spares a
     * {@code SlotSchema} allocation per scalar property or map entry. Keep this in step with {@code refineSlot} and
     * its three helpers.
     *
     * @param schema the declared or configured slot schema, possibly {@code null}
     * @return {@code true} if the schema has to go through {@code refineSlot}
     */
    protected static boolean isRefinableSlot(final Type<?> schema) {
        return schema == null || schema.isMap() || schema.isCollection() || schema.isArray() || schema.isOptionalOrNullable();
    }

    /**
     * Returns the element type an array read converts its elements with when it differs from the destination's
     * own component: the configured component of an open slot, or the metadata-refined component of a declared
     * reference array ({@code Long} for a {@code Number[]} slot with {@code Long[]} metadata, a specialized map for
     * a {@code Map<String, Object>[]} slot), whose array class {@link #openArrayDestination} keeps. A primitive
     * component is never overridden, whether the slot declared the array or adopted it from metadata.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper, possibly {@code null}
     * @param schema the array schema after metadata refinement, possibly {@code null}
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the element type to convert with, or {@code null} when the destination's component applies
     */
    protected static Type<?> arrayComponentOverride(final Type<?> declaredSlot, final Type<?> schema, final Type<?> configuredElementType) {
        if (schema != null && schema.isArray() && schema.elementType() != null && schema.elementType().javaType().isPrimitive()) {
            // A primitive component is reified whether the slot declared the array or adopted it from metadata: a
            // char[] cannot hold the configured Integers, and its own codec reads "1" as the digit, never as code 1.
            return null;
        }
        final Type<?> configured = slotComponentType(declaredSlot, configuredElementType);
        if (configured != null) {
            return configured;
        }
        final Type<?> declaredPayload = optionalPayload(declaredSlot);
        if (schema != null && schema.isArray() && declaredPayload != null && declaredPayload.isArray() && schema.javaType() != declaredPayload.javaType()
                && schema.elementType() != null && !schema.elementType().equals(declaredPayload.elementType())) {
            return schema.elementType();
        }
        return null;
    }

    /**
     * Returns whether a nested declared component adopts the component its metadata names: the declaration is an
     * abstract class or interface that has a type of its own ({@code Number}, {@code Calendar}; an open {@code Object}
     * slot adopts metadata on its own), the metadata names a scalar that has a type of its own (never the reflective
     * fallback type, whose text conversion would store text where the declaration expects a value), and that scalar
     * fits the declaration and can be constructed for it.
     *
     * @param declaredType the declared component
     * @param metadata the approved component metadata, assignable to the declaration
     * @return {@code true} when the component is read as {@code metadata}
     */
    private static boolean adoptsScalarMetadata(final Type<?> declaredType, final Type<?> metadata) {
        return isAbstractScalar(declaredType) && !metadata.isObject() && !metadata.isArray() && !metadata.isCollection() && !metadata.isMap()
                && !metadata.isOptionalOrNullable() && getConcreteClass(metadata.javaType(), declaredType.javaType()) == metadata.javaType();
    }

    /**
     * Returns whether a declared type is a scalar that cannot be read on its own: an abstract class or interface with
     * a type of its own ({@code Number}, {@code Calendar}), as opposed to an open {@code Object} slot, a container, a
     * wrapper or a concrete scalar.
     *
     * @param type the declared type
     * @return {@code true} for an abstract scalar declaration
     */
    private static boolean isAbstractScalar(final Type<?> type) {
        final Class<?> clazz = type.javaType();
        return Modifier.isAbstract(clazz.getModifiers()) && !clazz.isPrimitive() && !type.isObject() && !type.isArray() && !type.isCollection() && !type.isMap()
                && !type.isOptionalOrNullable();
    }

    /**
     * Returns the type a container's declared argument is read with when the container's refined schema narrows it: an
     * element or map value declared as an abstract scalar ({@code Number} in {@code List<Number>} or
     * {@code Map<String, Number>}) adopts the concrete scalar the container's metadata named for it ({@code Long} from
     * {@code ArrayList<Long>} or {@code HashMap<String, Long>}, see {@link #refineDeclaredSchema}), since it cannot
     * read the children's text itself; any other declared argument is kept, as before.
     *
     * @param declaredArgument the argument of the declared container schema
     * @param refinedArgument the same argument of the refined container schema, possibly {@code null}
     * @return the type to read the children with
     */
    protected static Type<?> narrowedArgument(final Type<?> declaredArgument, final Type<?> refinedArgument) {
        return refinedArgument != null && refinedArgument != declaredArgument && adoptsScalarMetadata(declaredArgument, refinedArgument) ? refinedArgument
                : declaredArgument;
    }

    /**
     * Returns whether a declared slot that the readers treat as scalar text, a "serializable" scalar or an array or
     * collection of scalars written as compact text, ignores its {@code type} attribute as before: a declared
     * {@code Long} is read as {@code Long} whatever the attribute says. An array or collection whose component is an
     * abstract scalar ({@code Number[]}, {@code List<Number>}, possibly wrapped or nested) is the exception: it needs
     * the metadata's component to read its elements at all, so its attribute refines the declaration
     * ({@link #refineDeclaredSchema}) while the slot's own array or collection class is kept.
     *
     * @param declaredType the declared property type
     * @return {@code true} when the readers keep the declared type without consulting the attribute
     */
    protected static boolean ignoresTypeAttribute(final Type<?> declaredType) {
        return declaredType.isSerializable() && !hasAbstractComponent(declaredType);
    }

    private static boolean hasAbstractComponent(final Type<?> type) {
        final Type<?> payload = optionalPayload(type);
        if (payload == null || !(payload.isArray() || payload.isCollection())) {
            return false;
        }
        final Type<?> element = payload.elementType();
        return element != null && (isAbstractScalar(element) || hasAbstractComponent(element));
    }

    /**
     * Fills Object slots from approved metadata without replacing specific declared arguments or wrappers. A nested
     * component declared as an abstract class or interface with a type of its own ({@code Number}) adopts the concrete
     * scalar its metadata names ({@code Long} for a {@code Number[]} slot with {@code Long[]} metadata, or for a
     * {@code List<Number>} with {@code ArrayList<Long>}), since the declaration cannot read the elements' text itself;
     * the slot's own array or collection class is kept by its reader (see {@link #arrayComponentOverride}).
     * Container arguments are the handler's semantic key/value/element schema, not the implementation class's
     * Java type variables. Callers carry the outer construction class separately; nested container types must
     * retain compatible implementation hints themselves so that identity, ordering and membership survive.
     * Compatible metadata can refine a declared container's Object arguments even when its implementation cannot
     * be instantiated; construction then retains the declared container's mutable fallback.
     * Unchanged or matching types are reused. Rebuilding an ordinary container name uses its resolved arguments
     * directly, avoiding another recursive parse and without retaining additional types in a cache.
     *
     * @param declaredType the declared parsing schema
     * @param metadata the approved type information, or {@code null}
     * @return the declaration with Object slots refined and nested implementations retained, or the original type if unchanged
     */
    protected static Type<?> refineDeclaredSchema(final Type<?> declaredType, final Type<?> metadata) {
        return refineDeclaredSchema(declaredType, metadata, false);
    }

    /**
     * Refines a schema and carries construction inside nested type arguments, where readers have no separate hint.
     *
     * @param declaredType the declared parsing schema
     * @param metadata the approved type information, or {@code null}
     * @param nested whether the type is an argument or array component rather than the outer parsing schema
     * @return the refined type, retaining specific declared arguments and usable nested implementations
     */
    private static Type<?> refineDeclaredSchema(final Type<?> declaredType, final Type<?> metadata, final boolean nested) {
        if (metadata == null || declaredType == metadata || metadata.javaType() == Object.class) {
            return declaredType;
        }
        if (declaredType.isOptionalOrNullable() && declaredType.elementType() != null) {
            final Type<?> payload = refineDeclaredSchema(declaredType.elementType(),
                    metadata.isOptionalOrNullable() && metadata.elementType() != null ? metadata.elementType() : metadata, nested);
            return payload == declaredType.elementType() ? declaredType : Type.of(declaredType.javaType().getCanonicalName() + "<" + payload.name() + ">");
        }
        if (declaredType.isObject()) {
            // Object-like handlers can also describe interfaces. Keep compatibility and mutable-container fallbacks.
            final Class<?> metadataClass = ClassUtil.wrap(metadata.javaType());
            if (getConcreteClass(metadataClass, declaredType.javaType()) == metadataClass) {
                return metadata;
            }
            // An uninstantiable container (the List.of(..)/Set.of(..)/Map.of(..) runtime classes) cannot be
            // constructed, but its arguments still describe the value: an open Object slot adopts them as an
            // open mutable schema instead of dropping the metadata.
            final Type<?> mutableSchema = mutableContainerSchema(metadata);
            // Object-like interfaces are still declared constraints: a mutable container fallback must fit
            // that interface, just as the original metadata had to fit it above.
            return mutableSchema != null && declaredType.javaType().isAssignableFrom(mutableSchema.javaType()) ? mutableSchema : declaredType;
        }
        if (!declaredType.javaType().isAssignableFrom(metadata.javaType())) {
            return declaredType;
        }
        if (declaredType.isArray() && metadata.isArray() && !declaredType.javaType().getComponentType().isPrimitive()) {
            final Type<?> element = refineDeclaredSchema(declaredType.elementType(), metadata.elementType(), true);
            return element == declaredType.elementType() ? declaredType : Type.of(element.name() + "[]");
        }
        if (!(declaredType.isMap() && metadata.isMap() || declaredType.isCollection() && metadata.isCollection())) {
            // A nested component declared as an abstract class or interface that has a type of its own (Number, Calendar)
            // adopts the scalar the metadata names (Long for a Number[] slot with Long[] metadata, for a List<Number> with
            // ArrayList<Long>) the way an Object slot adopts metadata: the declared type cannot read the elements' text
            // itself, and the readers keep the slot's own array or collection class (see openArrayDestination). A concrete
            // declared component (Date in a Date[] with Timestamp[] metadata) reads its own text and is left alone, as it
            // was before (code review 2026-10-06: the narrowing the javadoc promised was never produced, so a Number[]
            // read its JSON tokens as Integer and could not read child elements at all).
            return nested && adoptsScalarMetadata(declaredType, metadata) ? metadata : declaredType;
        }

        final List<Type<?>> declaredParameters = declaredType.parameterTypes();
        final List<Type<?>> metadataParameters = metadata.parameterTypes();
        if (declaredParameters.size() != metadataParameters.size()) {
            return declaredType;
        }
        // Construct nested containers directly: copying an ordinary map/set afterwards may already have lost entries.
        Type<?> constructionType = nested && metadata.javaType() != declaredType.javaType()
                && getConcreteClass(metadata.javaType(), declaredType.javaType()) == metadata.javaType() ? metadata : declaredType;
        Type<?>[] refined = null;
        for (int i = 0, size = declaredParameters.size(); i < size; i++) {
            final Type<?> parameter = refineDeclaredSchema(declaredParameters.get(i), metadataParameters.get(i), true);
            if (parameter != declaredParameters.get(i)) {
                if (refined == null) {
                    refined = declaredParameters.toArray(new Type<?>[size]);
                }
                refined[i] = parameter;
            }
        }
        if (refined == null && constructionType == declaredType) {
            return declaredType;
        }

        // Reuse approved metadata when its semantic slots already match, without rebuilding its generic name.
        boolean matchesConstruction = true;
        for (int i = 0, size = declaredParameters.size(); i < size; i++) {
            if ((refined == null ? declaredParameters.get(i) : refined[i]) != constructionType.parameterTypes().get(i)) {
                matchesConstruction = false;
                break;
            }
        }
        if (matchesConstruction) {
            return constructionType;
        }

        String[] namedParameters = null;
        if (constructionType.isMap() && constructionType.parameterTypes().get(1).isCollection()) {
            // Only collection-valued maps can have MultiValueMap's List<V> semantic slot named as V.
            // Ordinary containers already expose their named arguments; avoid parsing nested type names again.
            namedParameters = TypeAttrParser.parse(constructionType.name()).getTypeParameters();
            if (constructionType != declaredType) {
                for (int i = 0, size = declaredParameters.size(); i < size; i++) {
                    final Type<?> named = Type.of(namedParameters[i]);
                    final Type<?> original = constructionType.parameterTypes().get(i);
                    final Type<?> parameter = refined == null ? declaredParameters.get(i) : refined[i];
                    if (original != named && original.isCollection() && original.elementType().equals(named) && parameter.javaType() != original.javaType()) {
                        // A MultiValueMap can represent List<V> values, but must not change a declared scalar or concrete list slot.
                        constructionType = declaredType;
                        namedParameters = TypeAttrParser.parse(declaredType.name()).getTypeParameters();
                        break;
                    }
                }
                if (refined == null && constructionType == declaredType) {
                    return declaredType;
                }
            }
        }

        final String constructionName = constructionType.name();
        final int argumentsStart = constructionName.indexOf('<');
        int nameLength = argumentsStart + 2 + (declaredParameters.size() - 1) * 2;
        for (int i = 0, size = declaredParameters.size(); i < size; i++) {
            nameLength += (refined == null ? declaredParameters.get(i) : refined[i]).name().length();
        }
        final StringBuilder result = new StringBuilder(nameLength).append(constructionName, 0, argumentsStart).append('<');
        for (int i = 0, size = declaredParameters.size(); i < size; i++) {
            if (i > 0) {
                result.append(", ");
            }
            final Type<?> parameter = refined == null ? declaredParameters.get(i) : refined[i];
            if (namedParameters != null) {
                // MultiValueMap<K,V> exposes Map<K,List<V>> slots. Its name still takes V, not List<V>.
                final Type<?> named = Type.of(namedParameters[i]);
                final Type<?> original = constructionType.parameterTypes().get(i);
                if (original != named && original.isCollection() && original.elementType().equals(named)) {
                    result.append(parameter.elementType().name());
                    continue;
                }
            }
            result.append(parameter.name());
        }
        return Type.of(result.append('>').toString());
    }

    /**
     * Returns the effective serialization configuration, falling back to the default configuration
     * supplied at construction time when {@code config} is {@code null}.
     *
     * @param config the requested serialization configuration, possibly {@code null}
     * @return {@code config} if non-{@code null}, otherwise the parser's default XML serialization configuration
     */
    protected XmlSerConfig check(XmlSerConfig config) {
        return config == null ? defaultXmlSerConfig : config;
    }

    /**
     * Returns the effective deserialization configuration, falling back to the default configuration
     * supplied at construction time when {@code config} is {@code null}.
     *
     * @param config the requested deserialization configuration, possibly {@code null}
     * @return {@code config} if non-{@code null}, otherwise the parser's default XML deserialization configuration
     */
    protected XmlDeserConfig check(XmlDeserConfig config) {
        return config == null ? defaultXmlDeserConfig : config;
    }

    /**
     * Creates a new instance of a property class, using type information from an XML node's attributes if needed.
     * This method attempts to instantiate the property class directly. If instantiation fails or
     * the property class is abstract, it falls back to the type specified in the node's {@code type} attribute.
     *
     * @param <T> the type of the property instance to create
     * @param propClass the class to instantiate, or {@code null} to use type from the node attribute
     * @param node the XML node that may contain a {@code type} attribute specifying the concrete class
     * @return a new instance of the property class; if instantiation fails an exception is thrown (never {@code null})
     * @throws ParsingException if a nonblank type attribute is not allowed or no usable property class is available
     */
    @SuppressWarnings("unchecked")
    protected <T> T newPropInstance(final Class<?> propClass, final Node node) throws ParsingException {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                // One line, no stack trace: this method then falls back to the type attribute or retries the class,
                // which rethrows this failure when nothing else works.
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName() + " - " + e);
                }
            }
        }

        final Class<?> attributeTypeClass = getAttributeTypeClass(node);

        return newPropInstance(propClass, attributeTypeClass);
    }

    /**
     * Creates a new instance of a property class, using type information from XML attributes if needed.
     * This method attempts to instantiate the property class directly, falling back to type attribute information.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Attributes attrs = getAttributes(element);
     * List<String> list = newPropInstance(List.class, attrs);
     * }</pre>
     *
     * @param <T> the type of the property instance to create
     * @param propClass the class to instantiate, or {@code null} to use type from attributes
     * @param attrs the XML attributes that may contain type information
     * @return a new instance of the property class; if instantiation fails an exception is thrown (never {@code null})
     * @throws ParsingException if a nonblank type attribute is not allowed or no usable property class is available
     */
    @SuppressWarnings("unchecked")
    protected <T> T newPropInstance(final Class<?> propClass, final Attributes attrs) throws ParsingException {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                // One line, no stack trace: this method then falls back to the type attribute or retries the class,
                // which rethrows this failure when nothing else works.
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName() + " - " + e);
                }
            }
        }

        final Class<?> attributeTypeClass = getAttributeTypeClass(attrs);

        return newPropInstance(propClass, attributeTypeClass);
    }

    /**
     * Retrieves the value of a named, unqualified attribute from an XML stream reader.
     * This method efficiently searches through the attributes of the current element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLStreamReader reader = createXMLStreamReader(inputReader);
     * moveToRootElement(reader);
     * String typeValue = getAttribute(reader, "type");
     * }</pre>
     *
     * @param xmlReader the XML stream reader positioned at an element
     * @param attributeName the name of the attribute to retrieve
     * @return the attribute value, or {@code null} if the attribute is not found
     */
    protected static String getAttribute(final XMLStreamReader xmlReader, final String attributeName) {
        // Parser metadata is unqualified. A foreign x:type or x:isNull must not change the value;
        // getAttributeValue(null, name) also matches foreign namespaces with some StAX providers.
        for (int i = 0, count = xmlReader.getAttributeCount(); i < count; i++) {
            if (attributeName.equals(xmlReader.getAttributeLocalName(i)) && Strings.isEmpty(xmlReader.getAttributeNamespace(i))) {
                return xmlReader.getAttributeValue(i);
            }
        }

        return null;
    }

    /**
     * Extracts the Java class specified in the "type" attribute of an XML node.
     * This method is used to determine the runtime type for deserialization when explicit type information is provided.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Node node = doc.getElementsByTagName("item").item(0);
     * Class<?> typeClass = getAttributeTypeClass(node);
     * }</pre>
     *
     * @param node the XML node to examine for type attribute
     * @return the class corresponding to an allowed type attribute, or {@code null} if the attribute is absent or blank
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected Class<?> getAttributeTypeClass(final Node node) throws ParsingException {
        final String typeAttr = XmlUtil.getAttribute(node, XmlConstants.TYPE);
        final Type<?> type = resolvePresentTypeAttribute(typeAttr);

        return type == null ? null : type.javaType();
    }

    /**
     * Extracts the Java class specified in the "type" attribute from XML attributes.
     * This method is used to determine the runtime type for deserialization from SAX attributes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Attributes attrs = getAttributes();
     * Class<?> typeClass = getAttributeTypeClass(attrs);
     * }</pre>
     *
     * @param attrs the XML attributes to examine for type information
     * @return the class corresponding to an allowed type attribute, or {@code null} if the attribute is absent or blank
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected Class<?> getAttributeTypeClass(final Attributes attrs) throws ParsingException {
        if (attrs == null) {
            return null;
        }

        final String typeAttr = attrs.getValue(XmlConstants.TYPE);
        final Type<?> type = resolvePresentTypeAttribute(typeAttr);

        return type == null ? null : type.javaType();
    }

    /**
     * Extracts the Java class specified in the "type" attribute from an XML stream reader.
     * This method is used to determine the runtime type for deserialization during streaming.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLStreamReader reader = createXMLStreamReader(inputReader);
     * moveToRootElement(reader);
     * Class<?> typeClass = getAttributeTypeClass(reader);
     * }</pre>
     *
     * @param xmlReader the XML stream reader positioned at an element with attributes
     * @return the class corresponding to an allowed type attribute, or {@code null} if the attribute is absent or blank
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected Class<?> getAttributeTypeClass(final XMLStreamReader xmlReader) throws ParsingException {
        if (xmlReader.getAttributeCount() == 0) {
            return null;
        }

        final String typeAttr = getAttribute(xmlReader, XmlConstants.TYPE);
        final Type<?> type = resolvePresentTypeAttribute(typeAttr);

        return type == null ? null : type.javaType();
    }

    /**
     * Resolves an XML type attribute using built-ins and the application classes explicitly approved
     * for this parser. Ordinary factories also accept intrinsic names of already-registered classes,
     * preserving target-class and serializer round-trips without loading classes named only by XML.
     * Every array/generic component must qualify. Explicit approval sets exclude global registrations;
     * registration aliases and the former system-property switch never grant permission.
     *
     * @param typeAttr the decoded attribute value, or {@code null}
     * @return the resolved type, or {@code null} when the value is empty or not allowed
     */
    protected Type<?> resolveTypeAttribute(final String typeAttr) {
        if (Strings.isEmpty(typeAttr)) {
            return null;
        }

        final String typeName = typeAttr.trim();

        if (typeName.isEmpty()) {
            return null;
        }

        if (isAllowedXmlTypeAttributeName(typeName)) {
            final Type<?> approved = approvedXmlTypes.get(typeName);
            final Type<?> resolved = approved != null ? approved : Type.of(typeName);
            return isApprovedResolvedType(resolved) ? resolved : null;
        }

        return null;
    }

    /**
     * Resolves a type attribute that was read from XML, distinguishing an absent or blank attribute from an explicitly rejected one.
     * Rejected values fail closed so that deserialization cannot silently fall back to node-name-based class discovery.
     *
     * @param typeAttr the decoded attribute value, or {@code null}
     * @return the resolved type, or {@code null} when the attribute is absent or blank
     * @throws ParsingException if a nonblank attribute is not allowed
     */
    protected final Type<?> resolvePresentTypeAttribute(final String typeAttr) throws ParsingException {
        final Type<?> type = resolveTypeAttribute(typeAttr);

        if (type == null && Strings.isNotBlank(typeAttr)) {
            throw new ParsingException("XML type attribute is not allowed: " + typeAttr.trim());
        }

        return type;
    }

    // A global alias must not turn an allowed built-in name into permission for an application class.
    private boolean isApprovedResolvedType(final Type<?> type) {
        Class<?> cls = type.javaType();
        while (cls.isArray()) {
            cls = cls.getComponentType();
        }
        // Check class membership once rather than scanning each approved class's binary/canonical aliases.
        final boolean allowed = approvedXmlClasses.contains(cls) || SAFE_XML_TYPE_ATTRIBUTE_NAMES.contains(cls.getName())
                || acceptRegisteredTypes && isRegisteredXmlTypeName(cls.getCanonicalName());
        if (!allowed) {
            return false;
        }
        for (final Type<?> parameter : type.parameterTypes()) {
            if (!isApprovedResolvedType(parameter)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllowedXmlTypeAttributeName(String typeName) {
        int end = typeName.length();
        while (end >= 2 && typeName.charAt(end - 2) == '[' && typeName.charAt(end - 1) == ']') {
            end -= 2;
        }
        if (end != typeName.length()) {
            typeName = typeName.substring(0, end);
        }

        try {
            final TypeAttrParser typeAttr = TypeAttrParser.parse(typeName);

            final String className = typeAttr.getClassName();

            if (acceptRegisteredTypes && SAFE_XML_TYPE_ATTRIBUTE_NAMES.contains(className)) {
                // Compatibility with registered classes must not let an application alias hijack a built-in name.
                final Type<?> registered = TypeFactory.getTypeIfPresent(className);
                if (registered != null && !SAFE_XML_TYPE_ATTRIBUTE_NAMES.contains(registered.javaType().getName())) {
                    return false;
                }
            }

            if (!SAFE_XML_TYPE_ATTRIBUTE_NAMES.contains(className) && !approvedXmlTypes.containsKey(className)
                    && !(acceptRegisteredTypes && isRegisteredXmlTypeName(className))) {
                return false;
            }

            for (final String typeParameter : typeAttr.getTypeParameters()) {
                if (!isAllowedXmlTypeAttributeName(typeParameter)) {
                    return false;
                }
            }

            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isRegisteredXmlTypeName(final String name) {
        if (name == null) {
            return false;
        }
        // Non-creating lookup: a document cannot load a new class. A custom registration alias is not
        // an intrinsic name, even when its handler reports that alias as Type.name().
        final Type<?> registered = TypeFactory.getTypeIfPresent(name);
        if (registered == null) {
            return false;
        }
        final Class<?> cls = registered.javaType();
        return name.equals(cls.getCanonicalName()) || name.equals(cls.getName()) || name.equals(cls.getSimpleName()) && name.equals(registered.name());
    }

    /**
     * Determines the concrete class to use for deserialization by examining XML node attributes.
     * This method resolves the actual class to instantiate, preferring type attribute information over the target class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Node node = doc.getElementsByTagName("item").item(0);
     * Class<?> concreteClass = getConcreteClass(node, Collection.class);
     * }</pre>
     *
     * @param node the XML node that may contain type attribute information
     * @param targetType the expected target class for deserialization
     * @return the concrete class to instantiate, either from the type attribute or the target class
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected Class<?> getConcreteClass(final Node node, final Class<?> targetType) throws ParsingException {
        if (node == null) {
            return targetType;
        }

        final Class<?> typeClass = getAttributeTypeClass(node);

        return getConcreteClass(typeClass, targetType);
    }

    /**
     * Keeps a declared container's parsing schema when validated runtime metadata selects its implementation class.
     * The caller still constructs {@code concreteClass}; the returned type supplies keys, values and elements. Keeping
     * these separate avoids erasing generics or attaching a base type's arguments to a subclass with different type variables.
     *
     * @param declaredType the declared parsing type
     * @param concreteClass the already validated, compatible class to construct
     * @return the declared container schema when it has specific arguments, otherwise the concrete type
     */
    protected static Type<?> containerReadType(final Type<?> declaredType, final Class<?> concreteClass) {
        if ((declaredType.isMap() || declaredType.isCollection()) && declaredType.javaType().isAssignableFrom(concreteClass)) {
            for (final Type<?> parameter : declaredType.parameterTypes()) {
                if (!parameter.isObject()) {
                    return declaredType;
                }
            }
        }

        return Type.of(concreteClass);
    }

    /**
     * Decodes container text using the declared arguments and the resolved implementation independently.
     * Parsing through the declaration's default container first could discard entries (for example equal
     * elements destined for an identity set), so a different implementation is supplied to the JSON reader directly.
     * When the schema already includes the implementation, its full type retains nested constraints during conversion.
     * Unicode whitespace around the JSON text is ignored, as it is around the text of a primitive or String array.
     *
     * <p>The XML configuration's per-key value types and its global map key and value types reach the JSON reader, so a
     * compact map applies the same {@code setValueType} and {@code setMapValueType} rules as its child-element form, at
     * every nesting level. A list-valued implementation (Spring's {@code MultiValueMap}) keeps the declared value shape:
     * a declared scalar, set or concrete-list value type, or such a global map-value configuration, retains ordinary map
     * construction, and a present key whose override is not a {@code List} moves the entries read so far into an
     * ordinary map (see {@link ListValuedMapGuard}).</p>
     *
     * @param text the container's JSON text
     * @param schema the declared parsing schema
     * @param concreteClass the validated, compatible implementation class
     * @param config the XML configuration whose value types apply to the text, possibly {@code null}
     * @return the decoded container, or the schema's usual null/empty-text result
     */
    protected static Object readContainerText(final String text, final Type<?> schema, final Class<?> concreteClass, final XmlDeserConfig config) {
        if (Strings.isBlank(text)) {
            return schema.valueOf(text);
        }
        // The configured component also types the open elements of nested containers at parse time (see
        // CompactJsonParser.nestedElementType); typeNestedOpenComponents in the body remains for containers built
        // without it.
        final Type<?> configuredComponent = config != null && config.getElementType() != null && !config.getElementType().isObject()
                && hasNestedOpenComponent(schema) ? config.getElementType() : null;
        return CompactJsonParser.withConfiguredComponent(configuredComponent, schema, () -> readContainerJson(text.strip(), schema, concreteClass, config));
    }

    /**
     * The body of {@link #readContainerText}, for stripped JSON text.
     *
     * @param json the stripped JSON text
     * @param schema the container schema the text is read with
     * @param concreteClass the construction class
     * @param config the XML configuration, possibly {@code null}
     * @return the decoded container
     */
    private static Object readContainerJson(final String json, final Type<?> schema, final Class<?> concreteClass, final XmlDeserConfig config) {
        final JsonDeserConfig jdc = compactJsonConfig(config);
        Class<?> construction = concreteClass;
        if (schema.isMap() && isListValuedMap(construction) && !isListValuedMap(schema.javaType())) {
            // The declared value shape survives metadata: a scalar, set or concrete-list value type cannot be stored in a
            // list-valued map, so the declaration keeps its ordinary construction.
            final Type<?> valueType = schema.parameterTypes().size() == 2 ? schema.parameterTypes().get(1) : objType;
            final Type<?> effectiveValueType = valueType.isObject() && jdc.getMapValueType() != null ? jdc.getMapValueType() : valueType;
            if (!acceptsListValues(effectiveValueType)) {
                construction = schema.javaType();
            } else {
                // Name the implementation with the declared element: a raw list-valued class would read List<Object>
                // values and hide a declared Map<K, List<V>>'s element type. MultiValueMap<K, V> reads Map<K, List<V>>.
                final Type<?> key = schema.parameterTypes().size() == 2 ? schema.parameterTypes().get(0) : objType;
                final Type<?> element = effectiveValueType.isCollection() && effectiveValueType.elementType() != null ? effectiveValueType.elementType()
                        : objType;
                return jsonParser.deserialize(json, jdc, Type.of(construction.getCanonicalName() + "<" + key.name() + ", " + element.name() + ">"));
            }
        }
        final Type<?> configuredComponent = jdc.getElementType() != null && hasNestedOpenComponent(schema) ? jdc.getElementType() : null;
        if (schema.javaType() == construction) {
            // Pass the full type: a raw MultiValueMap class supplies List<Object> values, which can otherwise
            // hide the refined List<V> schema carried by its codec's JSON configuration.
            if (!(schema.isMap() || schema.isCollection())) {
                return schema.valueOf(json);
            }
            return typeNestedOpenComponents(jsonParser.deserialize(json, jdc, schema), schema, configuredComponent);
        }

        if (schema.isCollection()) {
            final Type<?> element = schema.elementType();
            if (configuredComponent != null) {
                // A nested open component keeps its declared, open element while the configured type reaches its
                // innermost elements; the JSON reader's element configuration would type every open level instead.
                return typeNestedOpenComponents(jsonParser.deserialize(json, jdc, Type.of(construction.getCanonicalName() + "<" + element.name() + ">")),
                        schema, configuredComponent);
            }
            return jsonParser.deserialize(json, jdc.setElementType(element), construction);
        } else if (schema.isMap()) {
            final List<Type<?>> parameters = schema.parameterTypes();
            if (parameters.size() == 2 && !java.util.EnumMap.class.isAssignableFrom(construction)) {
                // The schema's arguments type this map's keys and values; the global map types stay in the configuration
                // for the open arguments of the maps nested inside (CompactJsonParser.nestedMapType), as for child elements.
                final Type<?> typed = Type.of(construction.getCanonicalName() + "<" + parameters.get(0).name() + ", " + parameters.get(1).name() + ">");
                return typeNestedOpenComponents(jsonParser.deserialize(json, jdc, typed), schema, configuredComponent);
            }
            if (parameters.size() == 2) {
                if (!parameters.get(0).isObject()) {
                    jdc.setMapKeyType(parameters.get(0));
                }
                if (!parameters.get(1).isObject()) {
                    jdc.setMapValueType(parameters.get(1));
                }
            }
            return typeNestedOpenComponents(jsonParser.deserialize(json, jdc, construction), schema, configuredComponent);
        }

        return schema.valueOf(json);
    }

    /**
     * Whether an element's own text is the compact JSON form of a map: non-blank text whose first character after
     * Unicode whitespace is an opening brace. Blank text is an empty map; any other text is stray.
     *
     * @param text the element's text, possibly {@code null}
     * @return {@code true} for compact map text
     */
    protected static boolean isCompactMapText(final String text) {
        if (text == null) {
            return false;
        }
        // The first code unit String.strip() would expose, read in place instead of copying the whole text for it:
        // strip() and isBlank() skip the Character.isWhitespace code points, none of which is supplementary.
        for (int i = 0, length = text.length(); i < length; i++) {
            final char ch = text.charAt(i);
            if (!Character.isWhitespace(ch)) {
                return ch == '{';
            }
        }
        return false;
    }

    /**
     * Returns the schema compact map text is read with: below the root a declared map that carries both arguments,
     * otherwise a map of the key and value types the reader resolved for the element, which at the root puts an
     * explicit global type ahead of the requested arguments, as for the element's children.
     *
     * @param isRoot whether the map is the document root
     * @param propType the declared or configured map type, possibly {@code null}
     * @param keyType the resolved key type
     * @param valueType the resolved value type
     * @return the schema to read the text with
     */
    protected static Type<?> compactMapSchema(final boolean isRoot, final Type<?> propType, final Type<?> keyType, final Type<?> valueType) {
        if (!isRoot && propType != null && propType.isMap() && propType.parameterTypes().size() == 2) {
            return propType;
        }
        return Type.of("Map<" + keyType.name() + ", " + valueType.name() + ">");
    }

    /**
     * Builds the JSON configuration for compact text from the XML configuration: the global element, map key and map
     * value types fill open slots exactly as they do for child elements, and the per-key value types apply to every
     * JSON map at any nesting level, as they do to every XML map.
     *
     * @param config the XML configuration, possibly {@code null}
     * @return a new JSON configuration, sharing the XML configuration's validated per-key value types
     */
    protected static JsonDeserConfig compactJsonConfig(final XmlDeserConfig config) {
        final JsonDeserConfig jdc = JsonDeserConfig.create();
        if (config == null) {
            return jdc;
        }
        if (config.getElementType() != null && !config.getElementType().isObject()) {
            jdc.setElementType(config.getElementType());
        }
        if (config.getMapKeyType() != null && !config.getMapKeyType().isObject()) {
            jdc.setMapKeyType(config.getMapKeyType());
        }
        if (config.getMapValueType() != null && !config.getMapValueType().isObject()) {
            jdc.setMapValueType(config.getMapValueType());
        }
        if (config.valueTypeMap != null && !config.valueTypeMap.isEmpty()) {
            // The XML configuration validated its entries when they were set, so the map is shared as it is, like the
            // bean info below: re-running the validating setter here made every compact value of a document pay for
            // every configured key again. Neither parser adds or removes entries while reading (code review 2026-10-06).
            jdc.valueTypeMap = config.valueTypeMap;
        }
        if (config.beanInfoForValueTypes != null) {
            jdc.beanInfoForValueTypes = config.beanInfoForValueTypes;
        }
        return jdc;
    }

    /**
     * Applies a per-key value type to a declared map value. A scalar type configured for a key whose declared value
     * is a list or array names the element type, so the value reads as a list of that type from compact text and
     * from child elements alike: {@code setValueType("k", String.class)} reads the {@code Map<String, List<Integer>>}
     * entries {@code <k>[1,2]</k>} and {@code <k><e>1</e><e>2</e></k>} both as {@code ["1", "2"]}. A container, bean,
     * {@code Object} or wrapper type still names the whole value, and a declared scalar or open value keeps the
     * configured type as it is, so its text is read literally. The {@code List} values of a list-valued
     * implementation (Spring's {@code MultiValueMap}) come from the implementation, not from the declaration, so
     * there the configured type still names the whole value: scalar text moves the map to ordinary construction,
     * while a value that is itself a list (a JSON array in compact text, or child elements) reads as a list of the
     * configured type and keeps the implementation.
     *
     * @param mapClass the class of the map being read, possibly {@code null}
     * @param declaredValueType the map's declared value type, possibly {@code null}
     * @param configured the configured per-key type, possibly {@code null}
     * @return the type the value is read with: {@code configured}, or the declared container with that element
     */
    protected static Type<?> elementOverride(final Class<?> mapClass, final Type<?> declaredValueType, final Type<?> configured) {
        if (configured == null || declaredValueType == null || configured == declaredValueType || isListValuedMap(mapClass)
                || !(declaredValueType.isCollection() || declaredValueType.isArray()) || !isScalarType(configured)) {
            return configured;
        }
        return declaredValueType.isArray() ? Type.of(configured.name() + "[]")
                : Type.of(declaredValueType.javaType().getCanonicalName() + "<" + configured.name() + ">");
    }

    private static boolean isScalarType(final Type<?> type) {
        return !(type.isCollection() || type.isArray() || type.isMap() || type.isBean() || type.isObject() || type.isOptionalOrNullable() || type.isMapEntity()
                || type.isPrimitiveList() || type.isDataset());
    }

    /**
     * Checks that the element wrapping a map-typed value is the {@code <map>} element the standard writer emits for
     * a nested map. Any other element in that position is a document without the wrapper, whose entries would
     * otherwise be misread as the wrapper and its children: the first entry's name as the map, its children as the
     * entries.
     *
     * @param elementName the name of the element found where the wrapper is expected
     * @param valueName the property or key whose value is read, for the message
     * @throws ParsingException if the element is not {@code <map>}
     */
    protected static void checkMapWrapper(final String elementName, final String valueName) throws ParsingException {
        if (!XmlConstants.MAP.equals(elementName)) {
            throw new ParsingException("The value of " + valueName + " must be a <map> element, found <" + elementName + ">");
        }
    }

    /**
     * Returns the type a collection or array item is read with: the declared element type, refined by the item
     * type the container's approved metadata names, so that {@code ArrayList<LinkedMultiValueMap<String, Integer>>}
     * metadata on a declared {@code List<Map<String, Object>>} constructs its items as that map, for child elements
     * as for compact text. A declared constraint the metadata cannot refine is kept, as everywhere else.
     *
     * @param declaredElement the declared element type
     * @param containerType the container schema after metadata refinement, possibly {@code null}
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the item type
     */
    protected static Type<?> refinedItemType(final Type<?> declaredElement, final Type<?> containerType, final Type<?> configuredElementType) {
        if (declaredElement == null || containerType == null || !(containerType.isCollection() || containerType.isArray())
                || containerType.elementType() == null || containerType.elementType() == declaredElement) {
            return declaredElement;
        }
        if (configuredElementType != null && !configuredElementType.isObject() && (declaredElement.isArray() || declaredElement.isCollection())
                && hasConfigurableComponent(declaredElement, false)) {
            // Configuration owns the item's open component: the item keeps its declared, open form (an Object[] of the
            // configured elements), which the nested reader types; metadata may not narrow it to Long[].
            return declaredElement;
        }
        return refineDeclaredSchema(declaredElement, containerType.elementType(), true);
    }

    /**
     * Returns the type the child elements of a declared array slot are read with, or {@code null} when the
     * destination's own component applies. A nested container item ({@code Object[]} in {@code Object[][]}) keeps the
     * component of the destination {@link #openArrayDestination} selects: the declared, open form when configuration
     * owns its innermost component, otherwise the metadata-refined one. Any other item takes
     * {@link #arrayComponentOverride}.
     *
     * @param declaredSlot the slot schema before metadata refinement, possibly a wrapper
     * @param schema the array schema after metadata refinement
     * @param configuredElementType the global element configuration, possibly {@code null}
     * @return the item type, or {@code null}
     */
    protected static Type<?> arrayItemType(final Type<?> declaredSlot, final Type<?> schema, final Type<?> configuredElementType) {
        final Type<?> declaredPayload = optionalPayload(declaredSlot);
        final Type<?> declaredComponent = declaredPayload != null && declaredPayload.isArray() ? declaredPayload.elementType() : null;
        if (declaredComponent != null && (declaredComponent.isArray() || declaredComponent.isCollection())) {
            final Type<?> destination = openArrayDestination(declaredSlot, schema, configuredElementType);
            return destination != null && destination.isArray() ? destination.elementType() : null;
        }
        return arrayComponentOverride(declaredSlot, schema, configuredElementType);
    }

    /**
     * Spring's {@code MultiValueMap} interface when it is on the class path, otherwise {@code null}.
     */
    private static final Class<?> MULTI_VALUE_MAP_CLASS = loadOptionalClass("org.springframework.util.MultiValueMap");

    private static Class<?> loadOptionalClass(final String name) {
        try {
            return ClassUtil.forName(name);
        } catch (final Throwable e) {
            return null;
        }
    }

    /**
     * Returns whether a map class stores every value as a {@code List}, as Spring's {@code MultiValueMap} does.
     *
     * @param mapClass the map implementation class, possibly {@code null}
     * @return {@code true} for a list-valued implementation
     */
    protected static boolean isListValuedMap(final Class<?> mapClass) {
        return mapClass != null && MULTI_VALUE_MAP_CLASS != null && MULTI_VALUE_MAP_CLASS.isAssignableFrom(mapClass);
    }

    /**
     * Returns whether a declared or configured map value type can hold the {@code List} values a list-valued map stores:
     * an open {@code Object} slot or a {@code List}, {@code Collection} or {@code Iterable} interface can, while a
     * scalar, a set or a concrete list class cannot.
     *
     * @param valueType the effective value type, possibly {@code null}
     * @return {@code true} when a {@code List} value satisfies the type
     */
    protected static boolean acceptsListValues(final Type<?> valueType) {
        return valueType == null || valueType.isObject() || valueType.javaType().isAssignableFrom(List.class);
    }

    private static final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> ORDINARY_MAP = Tuple.of(cls -> new LinkedHashMap<>(), map -> map);

    private static final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> GUARDED_LIST_VALUED_MAP = Tuple
            .of(cls -> new ListValuedMapGuard(newReaderMap(cls)), map -> ((ListValuedMapGuard) map).unwrap());

    /**
     * Supplies the creator and converter for a map the readers construct from metadata. An {@code Immutable*} map keeps
     * its registered pair. A list-valued implementation (Spring's {@code MultiValueMap}) is constructed only when the
     * effective value type accepts {@code List} values; a declared scalar, set or concrete-list value type keeps an
     * ordinary {@link LinkedHashMap} instead. Even a compatible list-valued map is guarded: a value that is not a
     * {@code List} (a present key's scalar override, for example) moves the entries read so far into an ordinary map,
     * which the converter unwraps. Configuration for an absent key changes nothing.
     *
     * @param targetClass the map class to construct
     * @param valueType the effective value type the reader converts values with, possibly {@code null}
     * @return the pair, or {@code null} when the reader constructs {@code targetClass} itself
     */
    protected static Tuple2<Function<Class<?>, Object>, Function<Object, Object>> mapCreatorAndConverter(final Class<?> targetClass, final Type<?> valueType) {
        return mapCreatorAndConverter(targetClass, valueType, null);
    }

    /**
     * Same as {@link #mapCreatorAndConverter(Class, Type)}, with the per-key value configuration the guard consults: a
     * present key whose override cannot hold a {@code List} (a scalar, an explicit {@code Object}, an array, a set) moves
     * the map to ordinary construction even when its value is null.
     *
     * @param targetClass the map class to construct
     * @param valueType the effective value type the reader converts values with, possibly {@code null}
     * @param overrides the per-key configured type by key, or {@code null} without per-key configuration
     * @return the pair, or {@code null} when the reader constructs {@code targetClass} itself
     */
    protected static Tuple2<Function<Class<?>, Object>, Function<Object, Object>> mapCreatorAndConverter(final Class<?> targetClass, final Type<?> valueType,
            final Function<Object, Type<?>> overrides) {
        final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> registered = mapOfCreatorAndConverterForTargetType.get(targetClass);
        if (registered != null || !isListValuedMap(targetClass)) {
            return registered;
        }
        if (!acceptsListValues(valueType)) {
            return ORDINARY_MAP;
        }
        return overrides == null ? GUARDED_LIST_VALUED_MAP
                : Tuple.of(cls -> new ListValuedMapGuard(newReaderMap(cls), overrides), map -> ((ListValuedMapGuard) map).unwrap());
    }

    /**
     * Returns the per-key configured types of a configuration as a lookup by key, for {@link ListValuedMapGuard}, or
     * {@code null} when the configuration has none.
     *
     * @param config the deserialization configuration, possibly {@code null}
     * @return the lookup, or {@code null}
     */
    protected static Function<Object, Type<?>> scalarOverrides(final DeserializationConfig<?> config) {
        return config == null || !config.hasValueTypes() ? null : key -> config.getValueType(N.toString(key));
    }

    /**
     * A list-valued map (Spring's {@code MultiValueMap}) selected by XML metadata, guarded while the readers fill it:
     * the first value that is not a {@code List} moves the entries read so far into an ordinary {@link LinkedHashMap},
     * so a present key's scalar override keeps the declared value shape instead of failing with a
     * {@link ClassCastException}. {@link #unwrap()} returns whichever map holds the entries.
     */
    static final class ListValuedMapGuard implements Map<Object, Object> {
        private Map<Object, Object> delegate;
        private final Function<Object, Type<?>> overrides;
        private boolean listValued = true;

        ListValuedMapGuard(final Map<Object, Object> delegate) {
            this(delegate, null);
        }

        ListValuedMapGuard(final Map<Object, Object> delegate, final Function<Object, Type<?>> overrides) {
            this.delegate = delegate;
            this.overrides = overrides;
        }

        Map<Object, Object> unwrap() {
            return delegate;
        }

        private void accept(final Object key, final Object value) {
            if (!listValued) {
                return;
            }
            // A null value says nothing about the shape; a present key whose override cannot hold a List does: a scalar,
            // an explicit Object (open scalar text), an array or a set.
            final boolean scalar = value != null ? !(value instanceof List) : overrides != null && isNonListOverride(overrides.apply(key));
            if (scalar) {
                delegate = new LinkedHashMap<>(delegate);
                listValued = false;
            }
        }

        private static boolean isNonListOverride(final Type<?> override) {
            return override != null && (override.isObject() || !acceptsListValues(override));
        }

        @Override
        public Object put(final Object key, final Object value) {
            accept(key, value);
            return delegate.put(key, value);
        }

        @Override
        public void putAll(final Map<?, ?> m) {
            for (final Map.Entry<?, ?> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(final Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(final Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public Object get(final Object key) {
            return delegate.get(key);
        }

        @Override
        public Object remove(final Object key) {
            return delegate.remove(key);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<Object> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return delegate.values();
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public boolean equals(final Object other) {
            return other == this || delegate.equals(other);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    /**
     * The JSON parser for compact values: the ordinary JSON parser, except that a map class selected by XML metadata is
     * constructed through {@link AbstractXmlParser#mapCreatorAndConverter(Class, Type)}, so a list-valued
     * implementation is guarded against values that are not lists (see {@link ListValuedMapGuard}) at every nesting
     * level of the text, that a scalar per-key value type on a declared list or array value names the element type
     * when it meets a JSON array, as it does for child elements (see {@link AbstractXmlParser#elementOverride}), and that the global map key and value
     * types fill a map's open slots at any nesting level, as they do for child elements. Ordinary JSON callers keep
     * {@link JsonParserImpl} unchanged.
     */
    static final class CompactJsonParser extends JsonParserImpl {
        /** The configured component of a compact read, the depth of the container whose open elements it types, and the current depth. */
        private static final class ConfiguredComponent {
            private final Type<?> component;
            private final int openDepth;
            private int depth;

            ConfiguredComponent(final Type<?> component, final int openDepth) {
                this.component = component;
                this.openDepth = openDepth;
            }
        }

        private static final ThreadLocal<ConfiguredComponent> CONFIGURED_COMPONENT = new ThreadLocal<>();

        /**
         * Runs a compact read with the configured component in force for the open elements of the nested container
         * that owns them (see {@link #nestedElementType}); without a component, or when the configuration owns no
         * nested element of the schema, the read runs without a nested component. An independent reentrant read
         * temporarily replaces the enclosing read's component state and restores it even when the nested read fails.
         *
         * @param component the configured component, possibly {@code null}
         * @param schema the schema the text is read with
         * @param read the compact read
         * @return the read's result
         */
        static Object withConfiguredComponent(final Type<?> component, final Type<?> schema, final Supplier<Object> read) {
            final int openDepth = component == null ? -1 : openComponentDepth(schema);
            final ConfiguredComponent previous = CONFIGURED_COMPONENT.get();
            if (openDepth < 1 && previous == null) {
                // The top container's own elements are typed by the configuration the reader passes; nothing nested is open.
                return read.get();
            }
            if (openDepth < 1) {
                // A custom codec may start an independent compact XML read on this thread. Its unconfigured
                // elements must not inherit the enclosing document's component; restore that document afterwards.
                CONFIGURED_COMPONENT.remove();
            } else {
                CONFIGURED_COMPONENT.set(new ConfiguredComponent(component, openDepth));
            }
            try {
                return read.get();
            } finally {
                if (previous == null) {
                    CONFIGURED_COMPONENT.remove();
                } else {
                    CONFIGURED_COMPONENT.set(previous);
                }
            }
        }

        @Override
        protected Object readBracketedValue(final JsonReader jr, final JsonDeserConfig config, final BiConsumer<? super Collection<Object>, ?> propHandler,
                final Type<?> type) throws IOException {
            final ConfiguredComponent state = CONFIGURED_COMPONENT.get();
            if (state == null) {
                return super.readBracketedValue(jr, config, propHandler, type);
            }
            state.depth++;
            try {
                return super.readBracketedValue(jr, config, propHandler, type);
            } finally {
                state.depth--;
            }
        }

        @Override
        protected Type<?> nestedElementType(final Type<?> declaredElement, final JsonDeserConfig config) {
            // The open elements of the container at the slot's open level take the configured component, as nested child
            // elements do; a declared element keeps its precedence, and the component's own elements (one level deeper)
            // are never typed again.
            final ConfiguredComponent state = CONFIGURED_COMPONENT.get();
            return state != null && state.depth == state.openDepth && declaredElement.isObject() ? state.component : declaredElement;
        }

        @Override
        protected Tuple2<Function<Class<?>, Object>, Function<Object, Object>> mapCreatorAndConverter(final Class<?> targetClass,
                final JsonDeserConfig config) {
            final Tuple2<Function<Class<?>, Object>, Function<Object, Object>> guarded = AbstractXmlParser.mapCreatorAndConverter(targetClass, objType,
                    scalarOverrides(config));
            return guarded != null ? guarded : super.mapCreatorAndConverter(targetClass, config);
        }

        @Override
        protected Type<?> bracketedValueType(final Type<?> configured, final Type<?> declaredValueType, final JsonDeserConfig config, final String propName) {
            // A scalar per-key type on a declared list, set or array value names the element type and keeps the declared
            // container shape (a String[] for an Integer[] value, a Set<String> for a Set<Integer> value), as it does for
            // child elements; on a declared scalar or open value it keeps the configured type, which reads the text literally.
            if (config.getValueType(propName) == null || !isScalarType(configured)) {
                return configured;
            }
            if (declaredValueType.isArray()) {
                return Type.of(configured.name() + "[]");
            }
            return declaredValueType.isCollection() ? Type.of(declaredValueType.javaType().getCanonicalName() + "<" + configured.name() + ">") : configured;
        }

        @Override
        protected Type<?> nestedMapType(final Type<?> declared, final JsonDeserConfig config, final boolean key) {
            // The global map key and value types keep filling the open arguments of nested maps, as for child elements.
            final Type<?> enclosing = key ? config.getMapKeyType() : config.getMapValueType();
            return declared.isObject() && enclosing != null && !enclosing.isObject() ? enclosing : declared;
        }

        @Override
        protected boolean globalMapTypesApply(final Type<?> propType) {
            // The global map key and value types fill a map's open slots at any nesting level, as they do for child elements.
            return true;
        }
    }

    /**
     * Determines the concrete class to use for deserialization by examining XML attributes.
     * This method resolves the actual class to instantiate from SAX attributes during parsing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Attributes attrs = getAttributes();
     * Class<?> concreteClass = getConcreteClass(attrs, List.class);
     * }</pre>
     *
     * @param attrs the XML attributes that may contain type information
     * @param targetType the expected target class for deserialization
     * @return the concrete class to instantiate, either from the type attribute or the target class
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected Class<?> getConcreteClass(final Attributes attrs, final Class<?> targetType) throws ParsingException {
        if (attrs == null) {
            return targetType;
        }

        final Class<?> typeClass = getAttributeTypeClass(attrs);

        return getConcreteClass(typeClass, targetType);
    }

    /**
     * Determines the concrete class to use for deserialization by examining XML stream reader attributes.
     * This method resolves the actual class to instantiate during streaming deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLStreamReader reader = createXMLStreamReader(inputReader);
     * moveToRootElement(reader);
     * Class<?> concreteClass = getConcreteClass(reader, Map.class);
     * }</pre>
     *
     * @param xmlReader the XML stream reader positioned at an element with attributes
     * @param targetType the expected target class for deserialization
     * @return the concrete class to instantiate, either from the type attribute or the target class
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected Class<?> getConcreteClass(final XMLStreamReader xmlReader, final Class<?> targetType) throws ParsingException {
        if (xmlReader.getAttributeCount() == 0) {
            return targetType;
        }

        final Class<?> typeClass = getAttributeTypeClass(xmlReader);

        return getConcreteClass(typeClass, targetType);
    }

    /**
     * Validates and extracts a single child element node from an XML element.
     * This method ensures that an element contains at most one child element, ignoring text,
     * comments, processing instructions, and other non-element DOM nodes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Node element = doc.getElementsByTagName("wrapper").item(0);
     * Node singleChild = checkOneNode(element);
     * }</pre>
     *
     * @param eleNode the XML element node to examine
     * @return the single child element node, or {@code null} if there is none
     * @throws ParsingException if the element contains more than one child element node
     */
    protected static Node checkOneNode(final Node eleNode) throws ParsingException {
        final NodeList subEleNodes = eleNode.getChildNodes();
        Node subEleNode = null;

        for (int j = 0; j < subEleNodes.getLength(); j++) {
            final Node child = subEleNodes.item(j);

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (subEleNode != null) {
                throw new ParsingException("Only one child element is supported");
            }

            subEleNode = child;
        }

        return subEleNode;
    }

    /**
     * Returns the number of nodes in the given {@link NodeList}, or {@code 0} if the list is {@code null}.
     *
     * @param nodeList the node list to measure; may be {@code null}
     * @return the number of nodes in {@code nodeList}, or {@code 0} if {@code nodeList} is {@code null}
     */
    protected static int getNodeLength(final NodeList nodeList) {
        return (nodeList == null) ? 0 : nodeList.getLength();
    }

    /**
     * Writes {@code value} as JSON text content of the current XML element (the compact form of
     * collections, object arrays and map keys/values). Containers are streamed through the XML escaper
     * instead of retaining the complete JSON text; scalar values keep the String writer's null and
     * quotation behavior. Isolated surrogates, {@code U+FFFE} and {@code U+FFFF} are written as JSON
     * {@code \\uXXXX} escapes, as {@link #serializeEmbeddedJson(Object, XmlSerConfig)} does.
     *
     * <p>Because the JSON is streamed, a failure part-way through may leave a partial value in {@code bw}.</p>
     *
     * @param bw the XML writer to write to
     * @param value the value to serialize; may be {@code null}
     * @param config the XML serialization settings, or {@code null} for the defaults
     * @throws ParsingException if {@code value} cannot be serialized as JSON under the XML embedding policy, for example
     *         because it holds a present {@code Nullable} whose value is {@code null}
     * @throws IOException if writing to {@code bw} fails
     */
    protected void writeEmbeddedJson(final BufferedXmlWriter bw, final Object value, final XmlSerConfig config) throws IOException {
        writeEmbeddedJson(bw, value, config, false);
    }

    /**
     * Writes {@code value} as the payload of an {@code isJsonRawValue} property: unquoted JSON whose quotes and
     * whitespace are left unchanged, with only {@code &}, {@code <} and {@code >} entity-escaped (as
     * {@link #writeRawJson(BufferedXmlWriter, String)} does) so the document stays well-formed. Containers are
     * streamed; a failure part-way through may leave a partial value in {@code bw}.
     *
     * @param bw the XML writer to write to
     * @param value the value to serialize; may be {@code null}
     * @param config the XML serialization settings, or {@code null} for the defaults
     * @throws ParsingException if the payload contains a code unit that cannot be represented in XML 1.0, or if
     *         {@code value} cannot be serialized as JSON under the XML embedding policy
     * @throws IOException if writing to {@code bw} fails
     */
    protected void writeEmbeddedRawJson(final BufferedXmlWriter bw, final Object value, final XmlSerConfig config) throws IOException {
        writeEmbeddedJson(bw, value, config, true);
    }

    private void writeEmbeddedJson(final BufferedXmlWriter bw, final Object value, final XmlSerConfig config, final boolean raw) throws IOException {
        final Type<?> type = value == null ? null : Type.of(value.getClass());

        if (type != null && (type.isOptionalOrNullable() || isTupleLike(type) || value instanceof Holder)) {
            writeEmbeddedJson(bw, type, value, config, raw);
        } else if (type == null || type.isSerializable() && !(type.isCollection() || type.isArray() || type.isPrimitiveList() || type.javaType().isEnum())) {
            // Scalar JSON roots use stringOf, which a custom handler may return as null. Keep its existing
            // null/quotation behavior; these handlers already supply text rather than a buffered JSON document.
            final String json = serializeEmbeddedJson(value, config);
            if (raw) {
                writeRawJson(bw, json);
            } else {
                strType.serializeTo(bw, json, config);
            }
        } else {
            writeEmbeddedJson(bw, null, value, config, raw);
        }
    }

    @SuppressWarnings("unchecked")
    private void writeEmbeddedJson(final BufferedXmlWriter bw, final Type<?> valueType, final Object value, final XmlSerConfig config, final boolean raw)
            throws IOException {
        final char quotation = raw || config == null ? 0 : config.getStringQuotation();
        final EmbeddedJsonWriter output = new EmbeddedJsonWriter(bw, raw);
        if (quotation != 0) {
            bw.write(quotation);
        }

        if (valueType == null) {
            jsonParser.serialize(value, getJSC(config), output);
        } else {
            // The wrapper/tuple value writer preserves declared slot types and the XML embedding policy.
            final BufferedJsonWriter jsonWriter = Objectory.createBufferedJsonWriter(output);
            try {
                ((Type<Object>) valueType).serializeTo(jsonWriter, value, getJSC(config));
            } finally {
                Objectory.recycle(jsonWriter);
            }
        }

        output.finish();
        if (quotation != 0) {
            bw.write(quotation);
        }
    }

    /**
     * A bounded-memory XML sink for JSON output. One pending high surrogate allows a valid pair to span
     * JSON buffer boundaries; isolated surrogates and noncharacters use the same JSON escapes as
     * {@link #escapeEmbeddedJson(String)}, and raw payloads the same entity and XML 1.0 checks as
     * {@link #writeRawJson(BufferedXmlWriter, String)} (the rules are shared: {@code isJsonEscapedInXml},
     * {@code rawJsonEntity}, {@code notXmlCharacter}). Flushing JSON must not flush or close the enclosing XML writer.
     */
    private static final class EmbeddedJsonWriter extends Writer {
        private static final String HEX = "0123456789abcdef";
        private final BufferedXmlWriter output;
        private final boolean raw;
        private final char[] oneCharacter = new char[1];
        private char highSurrogate;

        EmbeddedJsonWriter(final BufferedXmlWriter output, final boolean raw) {
            this.output = output;
            this.raw = raw;
        }

        @Override
        public void write(final int ch) throws IOException {
            oneCharacter[0] = (char) ch;
            write(oneCharacter, 0, 1);
        }

        @Override
        public void write(final char[] chars, final int offset, final int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, chars.length);
            write(chars, null, offset, length);
        }

        @Override
        public void write(final String text, final int offset, final int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, text.length());
            write(null, text, offset, length);
        }

        private void write(final char[] chars, final String text, final int offset, final int length) throws IOException {
            if (length == 0) {
                return;
            }

            int start = offset;
            final int end = offset + length;
            if (highSurrogate != 0) {
                final char first = chars == null ? text.charAt(start) : chars[start];
                if (Character.isLowSurrogate(first)) {
                    output.write(highSurrogate);
                    output.write(first);
                    start++;
                } else {
                    writeUnicodeEscape(highSurrogate);
                }
                highSurrogate = 0;
            }

            for (int i = start; i < end; i++) {
                final char ch = chars == null ? text.charAt(i) : chars[i];
                if (Character.isHighSurrogate(ch)) {
                    if (i + 1 == end) {
                        writeRun(chars, text, start, i - start);
                        highSurrogate = ch;
                        return;
                    }
                    if (Character.isLowSurrogate(chars == null ? text.charAt(i + 1) : chars[i + 1])) {
                        i++;
                        continue;
                    }
                } else if (!isJsonEscapedInXml(ch)) {
                    if (!raw) {
                        continue;
                    }
                    if (!isXmlCharacter(ch)) {
                        throw notXmlCharacter("JSON raw value", ch);
                    }
                    final String replacement = rawJsonEntity(ch);
                    if (replacement == null) {
                        continue;
                    }
                    writeRun(chars, text, start, i - start);
                    output.write(replacement);
                    start = i + 1;
                    continue;
                }

                writeRun(chars, text, start, i - start);
                writeUnicodeEscape(ch);
                start = i + 1;
            }

            writeRun(chars, text, start, end - start);
        }

        private void writeRun(final char[] chars, final String text, final int offset, final int length) throws IOException {
            if (length == 0) {
                return;
            }
            if (chars == null) {
                if (raw) {
                    output.write(text, offset, length);
                } else {
                    output.writeCharacter(text, offset, length);
                }
            } else if (raw) {
                output.write(chars, offset, length);
            } else {
                output.writeCharacter(chars, offset, length);
            }
        }

        private void writeUnicodeEscape(final char ch) throws IOException {
            output.write("\\u");
            output.write(HEX.charAt(ch >>> 12));
            output.write(HEX.charAt(ch >>> 8 & 15));
            output.write(HEX.charAt(ch >>> 4 & 15));
            output.write(HEX.charAt(ch & 15));
        }

        void finish() throws IOException {
            if (highSurrogate != 0) {
                writeUnicodeEscape(highSurrogate);
                highSurrogate = 0;
            }
        }

        @Override
        public void flush() {
            // A JSON serializer may flush between the two code units of a surrogate pair.
        }

        @Override
        public void close() {
            // The enclosing XML serializer owns the destination and the end of this JSON value.
        }
    }

    /**
     * Serializes a JSON payload for embedding in XML, escaping code units forbidden in XML text.
     * Valid surrogate pairs remain intact; isolated surrogates round-trip through JSON Unicode escapes.
     * XML entity escaping, where required, is still performed by the caller.
     *
     * @param value the value to serialize
     * @param config the XML serialization settings
     * @return the JSON text with XML-forbidden code units escaped, or null if the type returns null text
     * @throws ParsingException if {@code value} cannot be serialized as JSON under the XML embedding policy, for example because it
     *         holds a present {@code Nullable} whose value is {@code null}
     */
    protected String serializeEmbeddedJson(final Object value, final XmlSerConfig config) throws ParsingException {
        if (value != null) {
            final Type<?> type = Type.of(value.getClass());
            if (type.isOptionalOrNullable() || isTupleLike(type) || value instanceof Holder) {
                try {
                    return serializeEmbeddedJson(type, value, config);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return escapeEmbeddedJson(jsonParser.serialize(value, getJSC(config)));
    }

    @SuppressWarnings("unchecked")
    private String serializeEmbeddedJson(final Type<?> type, final Object value, final XmlSerConfig config) throws IOException {
        // The JSON String overload uses stringOf for scalar roots. Use the value writer so wrappers/raw-JSON
        // properties retain the XML policy, including declared tuple slot types and properly quoted String slots.
        final BufferedJsonWriter jsonWriter = Objectory.createBufferedJsonWriter();
        try {
            ((Type<Object>) type).serializeTo(jsonWriter, value, getJSC(config));
            return escapeEmbeddedJson(jsonWriter.toString());
        } finally {
            Objectory.recycle(jsonWriter);
        }
    }

    /**
     * Escapes, as JSON {@code \\uXXXX} sequences, the code units of a JSON text that XML character data
     * cannot carry (isolated surrogates, {@code U+FFFE}, {@code U+FFFF}). Valid surrogate pairs are kept.
     *
     * @param json the JSON text, or {@code null}
     * @return the escaped text, or {@code null} if {@code json} is {@code null}
     */
    protected static String escapeEmbeddedJson(final String json) {
        if (json == null) {
            return null;
        }

        StringBuilder escaped = null;
        for (int i = 0; i < json.length(); i++) {
            final char ch = json.charAt(i);
            if (Character.isHighSurrogate(ch) && i + 1 < json.length() && Character.isLowSurrogate(json.charAt(i + 1))) {
                if (escaped != null) {
                    escaped.append(ch).append(json.charAt(i + 1));
                }
                i++;
            } else if (isJsonEscapedInXml(ch)) {
                if (escaped == null) {
                    escaped = new StringBuilder(json.length()).append(json, 0, i);
                }
                // These code units always have four hexadecimal digits. Escaping belongs to the
                // embedded JSON layer: XML character references cannot represent these values.
                escaped.append("\\u").append(Integer.toHexString(ch));
            } else if (escaped != null) {
                escaped.append(ch);
            }
        }
        return escaped == null ? json : escaped.toString();
    }

    /**
     * Retrieves the appropriate JSON serialization configuration based on XML serialization settings.
     * This method maps XML serialization options to JSON serialization configurations for internal processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerConfig xmlConfig = new XmlSerConfig().setCircularReferenceSupported(true);
     * JsonSerConfig jsonConfig = getJSC(xmlConfig);
     * }</pre>
     *
     * @param config the XML serialization configuration to map, or {@code null} for default
     * @return a JSON serialization configuration with corresponding settings
     */
    protected JsonSerConfig getJSC(final XmlSerConfig config) {
        if (config == null) {
            return jsc;
        }

        final JsonSerConfig baseConfig;

        if (config.isCircularReferenceSupported()) {
            if (!config.isFailOnEmptyBean()) {
                baseConfig = jscWithCircularRefAndEmptyBeanSupported;
            } else {
                baseConfig = jscWithCircularRefSupported;
            }
        } else if (!config.isFailOnEmptyBean()) {
            baseConfig = jscWithEmptyBeanSupported;
        } else {
            baseConfig = jsc;
        }

        // The common XML settings already match the shared embedding configuration. Avoid
        // cloning it for every scalar collection/property; neither writer mutates it.
        if (config.getIgnoredPropNames() == null && config.getExclusion() == baseConfig.getExclusion()
                && config.isSkipTransientField() == baseConfig.isSkipTransientField() && config.getDateTimeFormat() == baseConfig.getDateTimeFormat()
                && config.getPropNamingPolicy() == baseConfig.getPropNamingPolicy() && config.isWriteLongAsString() == baseConfig.isWriteLongAsString()
                && config.isWriteNullStringAsEmpty() == baseConfig.isWriteNullStringAsEmpty()
                && config.isWriteNullNumberAsZero() == baseConfig.isWriteNullNumberAsZero()
                && config.isWriteNullBooleanAsFalse() == baseConfig.isWriteNullBooleanAsFalse()
                && config.isWriteBigDecimalAsPlain() == baseConfig.isWriteBigDecimalAsPlain()) {
            return baseConfig;
        }

        // XML uses the JSON serializer for raw-JSON properties and for compact scalar
        // arrays/collections. Preserve the JSON syntax choices needed by that embedding
        // (notably quoted strings and compact layout), but forward every shared setting
        // that changes the serialized values. Otherwise a scalar Date/BigDecimal/bean and
        // the same value inside a JSON-backed collection are serialized differently.
        return baseConfig.copy()
                .setIgnoredPropNames(config.getIgnoredPropNames())
                .setExclusion(config.getExclusion())
                .setSkipTransientField(config.isSkipTransientField())
                .setDateTimeFormat(config.getDateTimeFormat())
                .setPropNamingPolicy(config.getPropNamingPolicy())
                .setWriteLongAsString(config.isWriteLongAsString())
                .setWriteNullStringAsEmpty(config.isWriteNullStringAsEmpty())
                .setWriteNullNumberAsZero(config.isWriteNullNumberAsZero())
                .setWriteNullBooleanAsFalse(config.isWriteNullBooleanAsFalse())
                .setWriteBigDecimalAsPlain(config.isWriteBigDecimalAsPlain());
    }

    /**
     * Internal enumeration of the structural roles an XML node can play during deserialization,
     * used to track the parsing context (bean/entity, property, array, element, collection, map,
     * map entry, key, or value).
     */
    enum NodeType {
        ENTITY, PROPERTY, ARRAY, ELEMENT, COLLECTION, MAP, ENTRY, KEY, VALUE
    }
}
