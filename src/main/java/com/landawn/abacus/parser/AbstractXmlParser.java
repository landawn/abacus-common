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
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.StreamFilter;
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
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.TypeAttrParser;
import com.landawn.abacus.util.XmlUtil;
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
     * Legacy compatibility switch for XML documents whose {@code type} attributes name arbitrary
     * application classes. Enabling it is unsafe for untrusted XML because resolving such a name can
     * initialize and later instantiate the named class.
     */
    static final String XML_TYPE_CLASS_FOR_NAME_PROPERTY = "abacus.xml.allowTypeAttrClassForName";

    /**
     * Exact built-in type names and framework-emitted aliases that may be resolved from untrusted XML without arbitrary name-driven class loading.
     * A generic expression is accepted only when its raw type and every nested type argument are either safe
     * built-ins or a name an already-registered type answers to as its own and the class itself supplies (its
     * canonical class name, or its simple name when that is the type's {@link Type#name()} - the name the writers
     * emit). Registration is checked without performing a creating lookup; names of unregistered classes and custom
     * registration aliases are deliberately excluded.
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
     * Baseline JSON serialization configuration used for JSON payloads embedded in XML. Character
     * values stay quoted so array/collection delimiters are unambiguous. Selected by {@link #getJSC(XmlSerConfig)} when neither
     * circular references nor empty beans need to be tolerated.
     */
    protected static final JsonSerConfig jsc = JsonSerConfig.create();

    /** Variant of {@link #jsc} that also tolerates beans with no serializable property. */
    protected static final JsonSerConfig jscWithEmptyBeanSupported = JsonSerConfig.create().setFailOnEmptyBean(false);

    /** Variant of {@link #jsc} that also tolerates circular references. */
    protected static final JsonSerConfig jscWithCircularRefSupported = JsonSerConfig.create().setCircularReferenceSupported(true);

    /** Variant of {@link #jsc} that tolerates both circular references and empty beans. */
    protected static final JsonSerConfig jscWithCircularRefAndEmptyBeanSupported = JsonSerConfig.create()
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
     */
    protected XMLStreamReader createXMLStreamReader(final Reader br) {
        return XmlUtil.createFilteredStreamReader(XmlUtil.createXMLStreamReader(br), NO_COMMENT_OR_PI);
    }

    /**
     * Creates an XML stream reader that filters out comments and processing instructions while preserving character
     * data from a byte stream. XML encoding is detected from the stream content (for example, BOM/XML declaration).
     *
     * @param is the input stream containing XML content to parse
     * @return an XMLStreamReader configured to skip comments and processing instructions, and to retain whitespace
     */
    protected XMLStreamReader createXMLStreamReader(final InputStream is) {
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
                throw new ParsingException(what + " contains U+" + String.format("%04X", (int) ch) + ", which cannot be represented in XML 1.0");
            }
        }
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
     * Returns whether {@code name} is an XML NCName: a letter or {@code '_'} followed by letters, digits,
     * {@code '-'}, {@code '.'}, {@code '_'}, combining marks and the extender {@code U+00B7}. A {@code ':'} is
     * deliberately rejected: both readers are namespace-aware and would report an unbound prefix for it.
     *
     * <p>The scan is by {@code char}, which also rejects any name containing a supplementary-plane character
     * (a surrogate pair is neither a letter nor a combining mark). That is deliberate, not an oversight of the
     * XML 1.0 fifth-edition {@code NameStartChar} range {@code [#x10000-#xEFFFF]}: the JDK readers this parser
     * runs on implement the fourth-edition name tables and reject such an element name, so accepting it here
     * would emit exactly the unreadable document this check exists to prevent.</p>
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

    /** Returns whether {@code ch} may start an XML name (an NCName, so {@code ':'} is excluded). */
    private static boolean isXmlNameStartChar(final char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    /** Returns whether {@code ch} may appear after the first character of an XML name. */
    private static boolean isXmlNameChar(final char ch) {
        if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '.' || ch == '_' || ch == 0x00B7) {
            return true;
        }

        final int charType = Character.getType(ch);

        return charType == Character.NON_SPACING_MARK || charType == Character.COMBINING_SPACING_MARK;
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
     * Writes a directly serializable scalar as element text, guarding the values XML 1.0 cannot carry.
     * A {@code char}/{@code Character} value of {@code '\0'} is written as an empty element (nothing is
     * written), which {@code Type.of(char.class).valueOf("")} reads back as {@code '\0'} while
     * {@code Type.of(Character.class).valueOf("")} reads back as {@code null}. Any other String,
     * {@code CharSequence} or {@code char} value is checked with {@link #checkXmlText(CharSequence, String)}
     * first; every other type is written unchanged.
     *
     * @param bw the writer to write to
     * @param type the type handler for {@code value}
     * @param value the value to write; must not be {@code null}
     * @param config the serialization configuration
     * @param what a short description of the value used in the error message
     * @throws ParsingException if the value contains a code unit that cannot be represented in XML 1.0
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    @SuppressWarnings("unchecked")
    protected static void writeXmlScalar(final BufferedXmlWriter bw, final Type<?> type, final Object value, final XmlSerConfig config, final String what)
            throws ParsingException, IOException {
        if (type.isCharacter() && value instanceof Character) {
            final char ch = (Character) value;

            if (ch == 0) {
                // NUL has no XML representation at all; the empty element is the one lossless spelling for a char.
                return;
            }

            checkXmlText(String.valueOf(ch), what);
        } else if (value instanceof CharSequence) {
            checkXmlText((CharSequence) value, what);
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
            final String replacement = ch == '&' ? "&amp;" : ch == '<' ? "&lt;" : ch == '>' ? "&gt;" : null;

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
     * @return the wrapped element when present; {@code null} for an empty wrapper and for
     *         {@code Nullable.of(null)}; {@code value} itself when it is not a wrapper
     */
    protected static Object unwrapOptional(final Object value) {
        if (value instanceof u.Optional) {
            return ((u.Optional<?>) value).orElseNull();
        } else if (value instanceof u.Nullable) {
            return ((u.Nullable<?>) value).orElseNull();
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
     * {@code Indexed}, {@code Timed}) whose {@code serializeTo} writes its String slots unquoted under an
     * XML configuration (string quotation {@code 0}), so that a comma inside a slot corrupts the text.
     * Such values must be written as their {@code stringOf} (JSON) form instead.
     *
     * @param type the type handler to inspect
     * @return {@code true} for the tuple-like handlers
     */
    protected static boolean isTupleLike(final Type<?> type) {
        final Class<?> cls = type.javaType();

        return cls != null && (Tuple.class.isAssignableFrom(cls) || Pair.class.isAssignableFrom(cls) || Triple.class.isAssignableFrom(cls)
                || Indexed.class.isAssignableFrom(cls) || Timed.class.isAssignableFrom(cls));
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
     * @throws IOException if writing or flushing the serialized content to {@code bw} fails
     */
    @SuppressWarnings("unchecked")
    protected void writeUnwrappedValue(final BufferedXmlWriter bw, final Type<?> declaredElementType, final Object value, final XmlSerConfig config,
            final String what) throws IOException {
        final Type<?> type = declaredElementType == null || declaredElementType.isObject() || !declaredElementType.javaType().isInstance(value)
                ? Type.of(value.getClass())
                : declaredElementType;

        if (type.isOptionalOrNullable()) {
            final Object nested = unwrapOptional(value);

            if (nested != null) {
                writeUnwrappedValue(bw, type.elementType(), nested, config, what);
            }
        } else if (isTupleLike(type)) {
            // stringOf is the JSON form the tuple's valueOf parses; serializeTo would write String slots unquoted.
            strType.serializeTo(bw, escapeEmbeddedJson(((Type<Object>) type).stringOf(value)), config);
        } else if (type.isSerializable() && !type.isObjectArray() && !type.isCollection()) {
            writeXmlScalar(bw, type, value, config, what);
        } else {
            strType.serializeTo(bw, serializeEmbeddedJson(value, config), config);
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
     * @throws ParsingException if {@code propType} is {@code null} (and the node does not indicate a {@code null} value)
     */
    protected Object getPropValue(final String propName, final Type<?> propType, final PropInfo propInfo, final Node propNode) throws ParsingException {
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
    protected static <T> T newPropInstance(final Class<?> propClass, final Node node) throws ParsingException {
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
    protected static <T> T newPropInstance(final Class<?> propClass, final Attributes attrs) throws ParsingException {
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
    protected static Class<?> getAttributeTypeClass(final Node node) throws ParsingException {
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
    protected static Class<?> getAttributeTypeClass(final Attributes attrs) throws ParsingException {
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
    protected static Class<?> getAttributeTypeClass(final XMLStreamReader xmlReader) throws ParsingException {
        if (xmlReader.getAttributeCount() == 0) {
            return null;
        }

        final String typeAttr = getAttribute(xmlReader, XmlConstants.TYPE);
        final Type<?> type = resolvePresentTypeAttribute(typeAttr);

        return type == null ? null : type.javaType();
    }

    /**
     * Resolves an XML {@code type} attribute without permitting arbitrary class loading by default.
     * Exact names in the built-in scalar/container allowlist, explicitly approved framework aliases, and the names an
     * already-registered type answers to as its own and the class itself supplies - its canonical class name, or its
     * simple name when that is the type's {@link Type#name()}, the name the writers emit - are accepted. The name of an
     * unregistered class or a custom registration alias is not sufficient.
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
    private static Type<?> resolvePresentTypeAttribute(final String typeAttr) throws ParsingException {
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

            if (!SAFE_XML_TYPE_ATTRIBUTE_NAMES.contains(className) && !isRegisteredXmlTypeName(className)) {
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
     * Returns whether {@code typeName} is a name an already-registered type answers to <i>as its own</i>, and that the
     * named class itself supplies: its canonical class name, or its simple name when that is also the type's intrinsic
     * {@link Type#name()}. The simple-name form matters because it is what the writers emit - {@link Type#xmlName()}
     * is {@code name()} with the angle brackets escaped - and many built-ins register under the simple name of their
     * class ({@code MapEntity}, {@code MutableInt}, {@code Dataset}), so accepting only canonical names rejected this
     * library's own output.
     *
     * <p>The registry lookup is non-creating, so no name in a document can trigger class loading. Requiring the name to
     * be one the class itself supplies is what keeps an application-defined registration <i>alias</i> rejected:
     * {@code TypeFactory.registerType("my.app.Alias", Foo.class, ..)} pools a handler whose own {@code name()} is the
     * alias, so the {@code name()} test alone would readmit it.</p>
     */
    private static boolean isRegisteredXmlTypeName(final String typeName) {
        final Type<?> registeredType = TypeFactory.getTypeIfPresent(typeName);

        if (registeredType == null || registeredType.javaType() == null) {
            return false;
        }

        final Class<?> javaType = registeredType.javaType();

        return typeName.equals(javaType.getCanonicalName()) || typeName.equals(javaType.getSimpleName()) && typeName.equals(registeredType.name());
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
    protected static Class<?> getConcreteClass(final Node node, final Class<?> targetType) throws ParsingException {
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
    protected static Class<?> getConcreteClass(final Attributes attrs, final Class<?> targetType) throws ParsingException {
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
    protected static Class<?> getConcreteClass(final XMLStreamReader xmlReader, final Class<?> targetType) throws ParsingException {
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
     * Serializes a JSON payload for embedding in XML, escaping code units forbidden in XML text.
     * Valid surrogate pairs remain intact; isolated surrogates round-trip through JSON Unicode escapes.
     * XML entity escaping, where required, is still performed by the caller.
     *
     * @param value the value to serialize
     * @param config the XML serialization settings
     * @return the JSON text with XML-forbidden code units escaped, or null if the type returns null text
     */
    protected String serializeEmbeddedJson(final Object value, final XmlSerConfig config) {
        return escapeEmbeddedJson(jsonParser.serialize(value, getJSC(config)));
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
            } else if (Character.isSurrogate(ch) || ch == '\uFFFE' || ch == '\uFFFF') {
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
