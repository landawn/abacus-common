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
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.TypeAttrParser;
import com.landawn.abacus.util.XmlUtil;

/**
 * Abstract base class providing common functionality for XML parser implementations.
 * This class extends {@link AbstractParser} and implements the {@link XmlParser} interface,
 * serving as the foundation for concrete XML parsing implementations.
 *
 * <p>This class provides:</p>
 * <ul>
 *   <li>Integration with JSON parser for hybrid JSON/XML processing</li>
 *   <li>Pre-configured JSON serialization configs for XML formatting (no quotation marks)</li>
 *   <li>Support for circular reference detection in XML serialization</li>
 *   <li>Default type definitions for XML key-value processing</li>
 *   <li>Default XML serialization and deserialization configurations</li>
 * </ul>
 *
 * <p>The class maintains several JSON serialization configurations that are adapted for
 * XML output by removing quotation marks. These configurations support various scenarios
 * including empty beans and circular references.</p>
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
     * Legacy compatibility switch for XML documents whose {@code type} attributes name arbitrary
     * application classes. Enabling it is unsafe for untrusted XML because resolving such a name can
     * initialize and later instantiate the named class.
     */
    static final String XML_TYPE_CLASS_FOR_NAME_PROPERTY = "abacus.xml.allowTypeAttrClassForName";

    /**
     * Exact built-in type names and framework-emitted aliases that may be resolved from untrusted XML without arbitrary name-driven class loading.
     * A generic expression is accepted only when its raw type and every nested type argument are either safe
     * built-ins or the canonical class name of an already-registered type. Registration is checked without
     * performing a creating lookup; simple bean names and custom registration aliases are deliberately excluded.
     */
    private static final Set<String> SAFE_XML_TYPE_ATTRIBUTE_NAMES = Set.of("boolean", "byte", "char", "short", "int", "long", "float", "double", "Boolean",
            "Byte", "Character", "Short", "Integer", "Long", "Float", "Double", "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Short",
            "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double", "String", "StringBuilder", "StringBuffer", "CharSequence", "Object",
            "Number", "java.lang.String", "java.lang.StringBuilder", "java.lang.StringBuffer", "java.lang.CharSequence", "java.lang.Object", "java.lang.Number",
            "BigInteger", "BigDecimal", "java.math.BigInteger", "java.math.BigDecimal", "Date", "Time", "Timestamp", "JUDate", "java.sql.Date", "java.sql.Time",
            "java.sql.Timestamp", "java.util.Date", "Calendar", "GregorianCalendar", "java.util.Calendar", "java.util.GregorianCalendar", "Duration", "Instant",
            "LocalDate", "LocalDateTime", "LocalTime", "MonthDay", "OffsetDateTime", "OffsetTime", "Period", "Year", "YearMonth", "ZonedDateTime", "ZoneId",
            "ZoneOffset", "java.time.Duration", "java.time.Instant", "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime",
            "java.time.MonthDay", "java.time.OffsetDateTime", "java.time.OffsetTime", "java.time.Period", "java.time.Year", "java.time.YearMonth",
            "java.time.ZonedDateTime", "java.time.ZoneId", "java.time.ZoneOffset", "UUID", "URI", "URL", "File", "Locale", "Currency", "java.util.UUID",
            "java.net.URI", "java.net.URL", "java.io.File", "java.util.Locale", "java.util.Currency", "Optional", "OptionalInt", "OptionalLong",
            "OptionalDouble", "java.util.Optional", "java.util.OptionalInt", "java.util.OptionalLong", "java.util.OptionalDouble", "AtomicBoolean",
            "AtomicInteger", "AtomicLong", "AtomicReference", "java.util.concurrent.atomic.AtomicBoolean", "java.util.concurrent.atomic.AtomicInteger",
            "java.util.concurrent.atomic.AtomicLong", "java.util.concurrent.atomic.AtomicReference", "Collection", "List", "ArrayList", "LinkedList", "Vector",
            "Stack", "Set", "HashSet", "LinkedHashSet", "SortedSet", "NavigableSet", "TreeSet", "Queue", "Deque", "ArrayDeque", "PriorityQueue",
            "java.util.Collection", "java.util.List", "java.util.ArrayList", "java.util.LinkedList", "java.util.Vector", "java.util.Stack", "java.util.Set",
            "java.util.HashSet", "java.util.LinkedHashSet", "java.util.SortedSet", "java.util.NavigableSet", "java.util.TreeSet", "java.util.Queue",
            "java.util.Deque", "java.util.ArrayDeque", "java.util.PriorityQueue", "CopyOnWriteArrayList", "CopyOnWriteArraySet", "ConcurrentLinkedQueue",
            "ConcurrentLinkedDeque", "LinkedBlockingQueue", "LinkedBlockingDeque", "PriorityBlockingQueue", "ConcurrentSkipListSet",
            "java.util.concurrent.CopyOnWriteArrayList", "java.util.concurrent.CopyOnWriteArraySet", "java.util.concurrent.ConcurrentLinkedQueue",
            "java.util.concurrent.ConcurrentLinkedDeque", "java.util.concurrent.LinkedBlockingQueue", "java.util.concurrent.LinkedBlockingDeque",
            "java.util.concurrent.PriorityBlockingQueue", "java.util.concurrent.ConcurrentSkipListSet", "Map", "HashMap", "LinkedHashMap", "SortedMap",
            "NavigableMap", "TreeMap", "Hashtable", "IdentityHashMap", "WeakHashMap", "Properties", "java.util.Map", "java.util.HashMap",
            "java.util.LinkedHashMap", "java.util.SortedMap", "java.util.NavigableMap", "java.util.TreeMap", "java.util.Hashtable", "java.util.IdentityHashMap",
            "java.util.WeakHashMap", "java.util.Properties", "ConcurrentMap", "ConcurrentHashMap", "ConcurrentNavigableMap", "ConcurrentSkipListMap",
            "java.util.concurrent.ConcurrentMap", "java.util.concurrent.ConcurrentHashMap", "java.util.concurrent.ConcurrentNavigableMap",
            "java.util.concurrent.ConcurrentSkipListMap", "ImmutableList", "ImmutableSet", "ImmutableMap", "com.landawn.abacus.util.ImmutableList",
            "com.landawn.abacus.util.ImmutableSet", "com.landawn.abacus.util.ImmutableMap");

    // protected static final int TEXT_SIZE_TO_READ_MORE = 256;

    /** Shared JSON parser used to serialize values that are emitted as a JSON payload inside an XML element. */
    protected static final JsonParser jsonParser = ParserFactory.createJsonParser();

    /**
     * Baseline JSON serialization configuration used for XML output: char quotation is disabled so
     * that values become bare element text. Selected by {@link #getJSC(XmlSerConfig)} when neither
     * circular references nor empty beans need to be tolerated.
     */
    @SuppressWarnings("deprecation")
    protected static final JsonSerConfig jsc = JsonSerConfig.create().setCharQuotation(SK.CHAR_ZERO);

    /** Variant of {@link #jsc} that also tolerates beans with no serializable property. */
    @SuppressWarnings("deprecation")
    protected static final JsonSerConfig jscWithEmptyBeanSupported = JsonSerConfig.create().setCharQuotation(SK.CHAR_ZERO).setFailOnEmptyBean(false);

    /** Variant of {@link #jsc} that also tolerates circular references. */
    @SuppressWarnings("deprecation")
    protected static final JsonSerConfig jscWithCircularRefSupported = JsonSerConfig.create()
            .setCharQuotation(SK.CHAR_ZERO)
            .setCircularReferenceSupported(true);

    /** Variant of {@link #jsc} that tolerates both circular references and empty beans. */
    @SuppressWarnings("deprecation")
    protected static final JsonSerConfig jscWithCircularRefAndEmptyBeanSupported = JsonSerConfig.create()
            .setCharQuotation(SK.CHAR_ZERO)
            .setFailOnEmptyBean(false)
            .setCircularReferenceSupported(true);

    /** Key type assumed for map entries when the configuration specifies none ({@code Object}). */
    protected static final Type<?> defaultKeyType = objType;

    /** Value type assumed for map entries and elements when the configuration specifies none ({@code Object}). */
    protected static final Type<?> defaultValueType = objType;

    /** The fallback serialization configuration used when a per-call {@code config} argument is {@code null}. */
    protected final XmlSerConfig defaultXmlSerConfig;

    /** The fallback deserialization configuration used when a per-call {@code config} argument is {@code null}. */
    protected final XmlDeserConfig defaultXmlDeserConfig;

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
     */
    @Override
    public <T> T deserialize(final Node source, final Type<? extends T> targetType) {
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
     */
    @Override
    public <T> T deserialize(final Node source, final Class<? extends T> targetType) {
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
     *        used to resolve the concrete type for each element encountered during parsing
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
     *        used to resolve the concrete type for each element encountered during parsing
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
     *        used to resolve the concrete type for each element encountered during parsing
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
     *        used to resolve the concrete type for each element encountered during parsing
     * @return an instance of the resolved target type populated with data from the XML node
     * @throws UnsupportedOperationException always thrown by this base-class implementation
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserConfig config, final Map<String, Type<?>> nodeTypes) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an XML stream reader that filters out whitespace and comments from the input.
     * This method provides a clean stream reader that only processes meaningful XML content.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new StringReader("<root><item>value</item></root>");
     * XMLStreamReader streamReader = createXMLStreamReader(reader);
     * }</pre>
     *
     * @param br the reader containing XML content to parse
     * @return an XMLStreamReader configured to skip whitespace and comments
     */
    protected XMLStreamReader createXMLStreamReader(final Reader br) {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(br),
                reader -> !(reader.isWhiteSpace() || reader.getEventType() == XMLStreamConstants.COMMENT));
    }

    /**
     * Creates an XML stream reader that filters out whitespace and comments from a byte stream.
     * XML encoding is detected from the stream content (for example, BOM/XML declaration).
     *
     * @param is the input stream containing XML content to parse
     * @return an XMLStreamReader configured to skip whitespace and comments
     */
    protected XMLStreamReader createXMLStreamReader(final InputStream is) {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(is),
                reader -> !(reader.isWhiteSpace() || reader.getEventType() == XMLStreamConstants.COMMENT));
    }

    /**
     * Advances a newly created stream reader to the document element.
     *
     * @param xmlReader the stream reader to advance
     * @throws XMLStreamException if the underlying stream cannot be read
     * @throws ParsingException if the document ends without a root element
     */
    protected static void moveToRootElement(final XMLStreamReader xmlReader) throws XMLStreamException {
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
     * @throws ParsingException if {@code propType} is {@code null} (and the node does not indicate a {@code null} value)
     */
    protected Object getPropValue(final String propName, final Type<?> propType, final PropInfo propInfo, final Node propNode) {
        final String txtValue = XmlUtil.getTextContent(propNode);

        if (Strings.isEmpty(txtValue)) {
            final NamedNodeMap attributes = propNode.getAttributes();

            if (attributes != null) {
                final Node attributeNode = attributes.getNamedItem(XmlConstants.IS_NULL);

                if ((attributeNode != null) && Boolean.parseBoolean(attributeNode.getNodeValue())) { //NOSONAR
                    return null;
                }
            }
        }

        if (propType == null) {
            throw new ParsingException("Can't parse property " + propName + " with value: " + txtValue);
        }

        if (propInfo != null && propInfo.hasFormat) {
            return propInfo.readPropValue(txtValue);
        } else {
            return propType.valueOf(txtValue);
        }
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
    protected static <T> T newPropInstance(final Class<?> propClass, final Node node) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName(), e);
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
    protected static <T> T newPropInstance(final Class<?> propClass, final Attributes attrs) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName(), e);
                }
            }
        }

        final Class<?> attributeTypeClass = getAttributeTypeClass(attrs);

        return newPropInstance(propClass, attributeTypeClass);
    }

    /**
     * Retrieves the value of a named attribute from an XML stream reader.
     * This method efficiently searches through the attributes of the current element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLStreamReader reader = createXMLStreamReader(inputReader);
     * String typeValue = getAttribute(reader, "type");
     * }</pre>
     *
     * @param xmlReader the XML stream reader positioned at an element
     * @param attributeName the name of the attribute to retrieve
     * @return the attribute value, or {@code null} if the attribute is not found
     */
    protected static String getAttribute(final XMLStreamReader xmlReader, final String attributeName) {
        final int attributeCount = xmlReader.getAttributeCount();
        //noinspection StatementWithEmptyBody
        if (attributeCount == 0) {
            // continue;
        } else if (attributeCount == 1) {
            //noinspection StatementWithEmptyBody
            if (attributeName.equals(xmlReader.getAttributeLocalName(0))) {
                return xmlReader.getAttributeValue(0);
            } else {
                // continue
            }
        } else {
            for (int i = 0; i < attributeCount; i++) {
                if (attributeName.equals(xmlReader.getAttributeLocalName(i))) {
                    return xmlReader.getAttributeValue(i);
                }
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
    protected static Class<?> getAttributeTypeClass(final Node node) {
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
    protected static Class<?> getAttributeTypeClass(final Attributes attrs) {
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
     * Class<?> typeClass = getAttributeTypeClass(reader);
     * }</pre>
     *
     * @param xmlReader the XML stream reader positioned at an element with attributes
     * @return the class corresponding to an allowed type attribute, or {@code null} if the attribute is absent or blank
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected static Class<?> getAttributeTypeClass(final XMLStreamReader xmlReader) {
        if (xmlReader.getAttributeCount() == 0) {
            return null;
        }

        final String typeAttr = getAttribute(xmlReader, XmlConstants.TYPE);
        final Type<?> type = resolvePresentTypeAttribute(typeAttr);

        return type == null ? null : type.javaType();
    }

    /**
     * Resolves an XML {@code type} attribute without permitting arbitrary class loading by default.
     * Exact names in the built-in scalar/container allowlist, explicitly approved framework aliases, and canonical class names of
     * already-registered types are accepted. A type's simple name or a custom registration alias is not sufficient.
     * Arrays and generic expressions are accepted only when every component satisfies the same rule. This preserves deterministic
     * round-trips for application beans without loading a class because its name appeared in XML. Applications that deserialize
     * trusted legacy XML may restore unrestricted name-driven lookup by setting
     * {@value #XML_TYPE_CLASS_FOR_NAME_PROPERTY} to {@code true}.
     *
     * @param typeAttr the decoded attribute value, or {@code null}
     * @return the resolved type, or {@code null} when the value is empty or not allowed
     */
    protected static Type<?> resolveTypeAttribute(final String typeAttr) {
        if (Strings.isEmpty(typeAttr)) {
            return null;
        }

        final String typeName = typeAttr.trim();

        if (typeName.isEmpty()) {
            return null;
        }

        if (Boolean.getBoolean(XML_TYPE_CLASS_FOR_NAME_PROPERTY) || isAllowedXmlTypeAttributeName(typeName)) {
            return Type.of(typeName);
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
    private static Type<?> resolvePresentTypeAttribute(final String typeAttr) {
        final Type<?> type = resolveTypeAttribute(typeAttr);

        if (type == null && Strings.isNotBlank(typeAttr)) {
            throw new ParsingException("XML type attribute is not allowed: " + typeAttr.trim());
        }

        return type;
    }

    private static boolean isAllowedXmlTypeAttributeName(String typeName) {
        while (typeName.endsWith("[]")) {
            typeName = typeName.substring(0, typeName.length() - 2);
        }

        try {
            final TypeAttrParser typeAttr = TypeAttrParser.parse(typeName);

            final String className = typeAttr.getClassName();

            if (!SAFE_XML_TYPE_ATTRIBUTE_NAMES.contains(className) && !isRegisteredCanonicalXmlTypeName(className)) {
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

    /**
     * Returns whether {@code typeName} is the canonical class name of an already-registered type.
     * Both checks are necessary: the type pool may also contain ambiguous simple names and arbitrary
     * application-defined aliases, neither of which is safe as an XML discriminator.
     */
    private static boolean isRegisteredCanonicalXmlTypeName(final String typeName) {
        final Type<?> registeredType = TypeFactory.getTypeIfPresent(typeName);

        if (registeredType == null || registeredType.javaType() == null) {
            return false;
        }

        return typeName.equals(registeredType.javaType().getCanonicalName());
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
    protected static Class<?> getConcreteClass(final Node node, final Class<?> targetType) {
        if (node == null) {
            return targetType;
        }

        final Class<?> typeClass = getAttributeTypeClass(node);

        return getConcreteClass(typeClass, targetType);
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
    protected static Class<?> getConcreteClass(final Attributes attrs, final Class<?> targetType) {
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
     * Class<?> concreteClass = getConcreteClass(reader, Map.class);
     * }</pre>
     *
     * @param xmlReader the XML stream reader positioned at an element with attributes
     * @param targetType the expected target class for deserialization
     * @return the concrete class to instantiate, either from the type attribute or the target class
     * @throws ParsingException if a nonblank type attribute is not allowed
     */
    protected static Class<?> getConcreteClass(final XMLStreamReader xmlReader, final Class<?> targetType) {
        if (xmlReader.getAttributeCount() == 0) {
            return targetType;
        }

        final Class<?> typeClass = getAttributeTypeClass(xmlReader);

        return getConcreteClass(typeClass, targetType);
    }

    /**
     * Validates and extracts a single child element node from an XML element.
     * This method ensures that an element contains exactly one child element, ignoring text,
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
    protected static Node checkOneNode(final Node eleNode) {
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
