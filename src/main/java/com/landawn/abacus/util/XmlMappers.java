/*
 * Copyright (C) 2024 HaiYang Li
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
package com.landawn.abacus.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.api.WstxOutputProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * A high-performance utility class for XML serialization and deserialization based on Jackson's {@code XmlMapper}.
 * This class provides convenient static methods for converting between Java objects and XML representations,
 * with support for various input/output formats and configuration options.
 *
 * <p>The class maintains bounded caches of configuration-bound {@code XmlMapper} instances, keyed by the supplied
 * {@link SerializationConfig} / {@link DeserializationConfig}. A cached mapper is never reconfigured after its
 * first use, preventing Jackson's serializer and root-deserializer caches from leaking one call's configuration
 * into another.</p>
 *
 * <p><b>Reuse your config objects.</b> Jackson's config classes do not override {@code equals}/{@code hashCode},
 * so these caches match on object identity. The config factory methods return fresh instances that carry their own
 * {@code ConfigOverrides}, and {@code with(...)} returns a new instance when it changes a setting. Recreating
 * configs inside a loop can construct a new mapper per call, retained until its cache entry is evicted. Hold a
 * single config instance and pass it repeatedly. The
 * {@code SerializationFeature}/{@code DeserializationFeature} overloads do not use these caches at all - they
 * derive a lightweight {@link ObjectWriter}/{@link ObjectReader} instead - and are the cheaper choice when you
 * only need to toggle features.</p>
 *
 * <p><b>Input conventions:</b> a {@code null} {@code SerializationConfig}/{@code DeserializationConfig} means
 * "use the default configuration". Reading from a {@link java.io.DataInput} is not supported by Jackson's XML
 * backend: those {@code fromXml} overloads always throw {@link UnsupportedOperationException}. An empty or blank
 * XML source is not treated as {@code null}; it fails with a wrapped {@code JsonParseException}
 * ("Unexpected EOF in prolog"). Unknown elements are rejected: Jackson's {@code FAIL_ON_UNKNOWN_PROPERTIES} is on
 * by default. File output targets must already have an existing parent directory.</p>
 *
 * <p><b>XML names are validated.</b> Mappers owned by this class reject invalid root, element and
 * attribute local names, including map keys and annotation-provided names. Empty names, leading digits,
 * whitespace and colons in local names fail serialization instead of producing malformed or lossy XML.
 * Valid Unicode XML names are preserved. Specify namespaces through Jackson's namespace annotations,
 * rather than embedding a prefix in a local name. Output streams and writers may already contain partial
 * output when validation fails.</p>
 *
 * <p>The selected {@link XMLOutputFactory} provider must support Woodstox's
 * {@code com.ctc.wstx.outputValidateNames} property; initialization fails if it cannot enable validation.
 * This requirement applies to default, pretty-printing and configuration-bound mappers. A mapper supplied
 * to {@link #wrap(XmlMapper)} retains its own name handling. To serialize arbitrary string keys, use
 * {@link JsonMappers}, or configure a caller-owned XML mapper with a reversible name encoding and valid
 * root names.</p>
 *
 * <p><b>The root element name is the runtime class's simple name.</b> For a bean that is stable and
 * predictable ({@code Plain} to {@code <Plain>}), and {@code @JacksonXmlRootElement} overrides it. For a
 * {@code Map} or {@code Collection} it exposes the <i>implementation</i> class, so the same logical content
 * produces different documents: {@code new HashMap<>()} gives {@code <HashMap>},
 * {@code new LinkedHashMap<>()} gives {@code <LinkedHashMap>}, {@code new TreeMap<>()} gives
 * {@code <TreeMap>}, {@code Map.of(..)} gives {@code <Map1>}, {@code Collections.singletonMap(..)} gives
 * {@code <SingletonMap>}, {@code List.of(..)} gives {@code <List12>} and {@code Collections.emptyMap()}
 * gives {@code <EmptyMap/>}. Swapping one {@code Map} implementation for another therefore changes the wire
 * format, and JDK-internal names such as {@code Map1} and {@code List12} leak into it. Reading back into
 * {@code Map.class} accepts any root name, so this only bites consumers that care about it - a schema, an
 * XPath expression, or a parser in another language. Pin the name when it matters, by wrapping a mapper and
 * writing through {@code mapper.writer().withRootName("root")}, or by annotating the type. {@link JsonMappers}
 * has no equivalent leak: a JSON object has no root name.</p>
 *
 * <p><b>Namespace prefixes are discarded on read.</b> Reading into an untyped {@code Map} strips the prefix
 * from every element name, so {@code <p:x>1</p:x><q:x>2</q:x>} in distinct namespaces and a plain
 * {@code <x>1</x><x>2</x>} both yield {@code {x=[1, 2]}} - the two documents become indistinguishable.</p>
 *
 * <p><b>Non-finite values are written as bare text.</b> {@code NaN}, {@code Infinity} and
 * {@code -Infinity} appear unquoted in the element body, and a round trip through this class restores them
 * exactly for a typed target. Be aware that {@code Infinity}/{@code -Infinity} are <i>not</i> valid
 * {@code xs:double} lexical forms - XML Schema spells them {@code INF}/{@code -INF} - so the output may be
 * rejected by a strictly schema-validating consumer ({@code NaN} is valid). Reading back into an untyped
 * {@code Map} leaves them as {@link String}. For comparison: {@link JsonMappers} writes quoted strings,
 * {@link FastJson} writes {@code null} and loses them, and {@link JsonUtil} rejects them outright.</p>
 *
 * <p><b>A failed write does not leave the target file intact.</b> The {@code File} overloads open (and
 * therefore truncate) the target before serialization runs. If serialization throws part-way, the previous
 * content is gone and what remains can be <i>well-formed but wrong</i>: a failure while writing the first
 * property typically leaves just the closed root element (for example {@code <Bean></Bean>}), which parses
 * cleanly back into an empty object. Never treat a file written by a call that threw as usable. Serialize to
 * a {@code String} first, or write to a temporary file and rename, when the destination must survive a
 * failure. The same applies to a caller-supplied {@code OutputStream} or {@code Writer}.</p>
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Configuration-bound XmlMapper caching to improve performance</li>
 *   <li>Support for pretty-printing XML output</li>
 *   <li>Configuration support for serialization and deserialization</li>
 *   <li>Multiple input/output formats (String, File, Stream, Reader/Writer, etc.)</li>
 *   <li>TypeReference support for complex generic types</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple serialization
 * Person person = new Person("John", 30);
 * String xml = XmlMappers.toXml(person);
 *
 * // Deserialization with TypeReference
 * String listXml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
 * List<String> list = XmlMappers.fromXml(listXml, new TypeReference<List<String>>() {});
 * }</pre>
 *
 * <p><b>XML security:</b> Every mapper created and owned by this class disables DTD processing and external-entity
 * resolution. A DOCTYPE declaration may still be accepted when it does not require entity expansion.
 * The hardened {@link XMLInputFactory} is configured once, for the single default mapper; cached
 * configuration-bound mappers are copies of it and therefore share that same factory instance rather than
 * re-deriving one, and pretty printing uses an {@link ObjectWriter} derived from it rather than a second mapper.
 * A mapper supplied to {@link #wrap(XmlMapper)} is not modified; the caller remains responsible for configuring
 * its underlying {@link XMLInputFactory} for the trust level of the XML it will read.</p>
 *
 * <p>This class is not instantiable.</p>
 *
 * @see XmlMapper
 * @see TypeReference
 * @see SerializationConfig
 * @see DeserializationConfig
 */
public final class XmlMappers {
    private static final int POOL_SIZE = 128;
    private static final Map<SerializationConfig, XmlMapper> serializationMapperPool = new LinkedHashMap<>(POOL_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<SerializationConfig, XmlMapper> eldest) {
            return size() > POOL_SIZE;
        }
    };
    private static final Map<DeserializationConfig, XmlMapper> deserializationMapperPool = new LinkedHashMap<>(POOL_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<DeserializationConfig, XmlMapper> eldest) {
            return size() > POOL_SIZE;
        }
    };

    private static final XmlMapper defaultXmlMapper = newSecureXmlMapper();

    /**
     * Pretty-printing is done through an {@link ObjectWriter} derived from {@link #defaultXmlMapper} rather than
     * through a second, separately hardened mapper. An {@code ObjectWriter} is immutable and thread-safe, shares
     * the mapper's serializer cache, and - because it is bound to the one mapper this class hardens - removes any
     * possibility of a second XML parser configuration drifting out of sync with it. {@code writer(INDENT_OUTPUT)}
     * is used rather than {@code writerWithDefaultPrettyPrinter()} so that the feature itself is enabled on the
     * writer's config: a custom serializer that queries {@code SerializerProvider.isEnabled(INDENT_OUTPUT)} still
     * sees {@code true}, exactly as it did when this was a mapper with the feature enabled.
     */
    private static final ObjectWriter defaultXmlWriterForPretty = defaultXmlMapper.writer(SerializationFeature.INDENT_OUTPUT);

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private XmlMappers() {
        // Utility class - prevent instantiation
    }

    /**
     * @throws IllegalArgumentException if {@code xml} is null or {@code len} is negative
     * @throws IndexOutOfBoundsException if {@code len} is nonnegative and {@code offset} is negative
     *         or the range exceeds {@code xml.length}
     */
    private static void checkByteRange(final byte[] xml, final int offset, final int len) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(xml, cs.xml);
        N.checkFromIndexSize(offset, len, xml.length);
    }

    /**
     * Creates an {@link XmlMapper} backed by an {@link XMLInputFactory} that disables DTD processing and
     * external-entity resolution, and an output factory that validates XML names.
     * Every mapper owned by this class is created this way.
     *
     * @return a new, security-hardened {@code XmlMapper}
     * @throws IllegalStateException if a required input or output factory property cannot be enforced
     */
    private static XmlMapper newSecureXmlMapper() throws IllegalStateException {
        return new XmlMapper(newSecureXmlInputFactory(), newValidatingXmlOutputFactory());
    }

    private static XMLOutputFactory newValidatingXmlOutputFactory() {
        final XMLOutputFactory factory = XMLOutputFactory.newFactory();

        // Validate in the writer because Jackson's name processor does not cover root names.
        // Read back the required property so an unsupported provider cannot silently emit invalid XML.
        try {
            factory.setProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES, true);

            if (!Boolean.TRUE.equals(factory.getProperty(WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES))) {
                throw new IllegalStateException("XMLOutputFactory ignored required XML name validation");
            }
        } catch (final RuntimeException e) {
            throw new IllegalStateException("XMLOutputFactory must support XML name validation: " + WstxOutputProperties.P_OUTPUT_VALIDATE_NAMES, e);
        }

        return factory;
    }

    /**
     * Creates an {@link XMLInputFactory} with DTD support and external-entity support disabled and a
     * resolver installed that refuses every external entity resolution attempt.
     *
     * @return a new, security-hardened {@code XMLInputFactory}
     * @throws IllegalStateException if a required security property cannot be set or is ignored by
     *         the platform's factory, or if the factory rejects the resolver
     */
    private static XMLInputFactory newSecureXmlInputFactory() throws IllegalStateException {
        final XMLInputFactory factory = XMLInputFactory.newFactory();

        setRequiredXmlInputFactoryProperty(factory, XMLInputFactory.SUPPORT_DTD, false);
        setRequiredXmlInputFactoryProperty(factory, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        try {
            factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
                throw new XMLStreamException("External XML entity resolution is disabled");
            });
        } catch (final RuntimeException e) {
            throw new IllegalStateException("The XMLInputFactory does not support a rejecting XMLResolver", e);
        }

        return factory;
    }

    /**
     * Sets a security-relevant boolean property on the given factory and verifies that the factory
     * actually honored it, so that a silently ignoring implementation cannot leave the parser open
     * to XXE attacks.
     *
     * @param factory the factory to configure
     * @param propertyName the name of the property to set
     * @param propertyValue the required value of the property
     * @throws IllegalStateException if the property cannot be set or the factory reports a different value
     */
    private static void setRequiredXmlInputFactoryProperty(final XMLInputFactory factory, final String propertyName, final boolean propertyValue)
            throws IllegalStateException {
        try {
            factory.setProperty(propertyName, propertyValue);

            if (!Boolean.valueOf(propertyValue).equals(factory.getProperty(propertyName))) {
                throw new IllegalStateException("XMLInputFactory ignored required security property: " + propertyName);
            }
        } catch (final RuntimeException e) {
            throw new IllegalStateException("Failed to configure required XMLInputFactory security property: " + propertyName, e);
        }
    }

    /**
     * Serializes the specified object to an XML string using default configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String xml = XmlMappers.toXml(person);
     * // Result: <Person><name>John</name><age>30</age></Person>
     * }</pre>
     *
     * @param obj the object to serialize
     * @return the XML string representation of the object
     * @throws RuntimeException if the configured mapper cannot serialize the object as XML
     */
    public static String toXml(final Object obj) throws RuntimeException {
        try {
            return defaultXmlMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML string with optional pretty formatting.
     * When pretty format is enabled, the output XML will be indented for better readability.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String prettyXml = XmlMappers.toXml(person, true);
     * // Result with indentation:
     * // <Person>
     * //   <name>John</name>
     * //   <age>30</age>
     * // </Person>
     * }</pre>
     *
     * @param obj the object to serialize
     * @param prettyFormat {@code true} to enable pretty printing with indentation, {@code false} for compact output
     * @return the XML string representation of the object
     * @throws RuntimeException if the configured mapper cannot serialize the object as XML
     */
    public static String toXml(final Object obj, final boolean prettyFormat) throws RuntimeException {
        try {
            if (prettyFormat) {
                return defaultXmlWriterForPretty.writeValueAsString(obj);
            } else {
                return defaultXmlMapper.writeValueAsString(obj);
            }
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML string with custom serialization features.
     * This method allows fine-grained control over serialization behavior.
     *
     * <p>Note that each supplied feature is <i>enabled</i>; this method cannot disable a feature.
     * To disable a feature (or otherwise customize the configuration), use
     * {@link #toXml(Object, SerializationConfig)} with a config built via
     * {@link #createSerializationConfig()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", null);
     * String xml = XmlMappers.toXml(person,
     *     SerializationFeature.WRAP_ROOT_VALUE,
     *     SerializationFeature.INDENT_OUTPUT);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param first the first serialization feature to enable
     * @param features additional serialization features to enable
     * @return the XML string representation of the object
     * @throws IllegalArgumentException if {@code first} or the {@code features} array is {@code null}.
     * @throws RuntimeException if the configured mapper cannot serialize the object as XML
     * @see #toXml(Object, SerializationConfig)
     */
    @SafeVarargs
    public static String toXml(final Object obj, final SerializationFeature first, final SerializationFeature... features)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(first, cs.first);
        N.checkArgNotNull(features, cs.features);

        // Uses an ObjectWriter rather than a feature-derived SerializationConfig: SerializationConfig does not
        // override equals/hashCode, so a config built here can never be found again in the mapper cache and every
        // call would construct (and retain) a brand-new XmlMapper - which for XML also means a fresh
        // XMLInputFactory lookup. ObjectWriter is immutable, thread-safe, cheap to derive, and inherits the
        // hardened parser configuration of the default mapper.
        try {
            return defaultXmlMapper.writer(first, features).writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML string using a custom serialization configuration.
     * This method provides maximum flexibility for controlling serialization behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig()
     *     .with(SerializationFeature.WRAP_ROOT_VALUE)
     *     .with(SerializationFeature.INDENT_OUTPUT);
     * String xml = XmlMappers.toXml(person, config);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration to use; if {@code null}, uses default configuration
     * @return the XML string representation of the object
     * @throws RuntimeException if the configured mapper cannot serialize the object as XML
     */
    public static String toXml(final Object obj, final SerializationConfig config) throws RuntimeException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to an XML file using default configuration.
     * The file will be created if it doesn't exist, or overwritten if it does.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * File output = new File("person.xml");
     * XmlMappers.toXml(person, output);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output file to write the XML to
     * @throws RuntimeException if the object cannot be serialized as XML, or opening, writing, flushing, or closing the output file fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final File output) throws RuntimeException, IllegalArgumentException {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML file using a custom serialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT);
     * XmlMappers.toXml(person, new File("person.xml"), config);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output file to write the XML to
     * @param config the serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if the object cannot be serialized as XML, or opening, writing, flushing, or closing the output file fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final File output, final SerializationConfig config) throws RuntimeException, IllegalArgumentException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to an XML output stream using default configuration.
     * Note: the stream is closed after writing, because {@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}
     * is enabled by default on the underlying mapper.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * try (FileOutputStream fos = new FileOutputStream("person.xml")) {
     *     XmlMappers.toXml(person, fos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output stream to write the XML to
     * @throws RuntimeException if the object cannot be serialized as XML, or writing, flushing, or closing {@code output} fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final OutputStream output) throws RuntimeException, IllegalArgumentException {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML output stream using a custom serialization configuration.
     * The stream is closed after writing unless the supplied config disables
     * {@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
     * ByteArrayOutputStream out = new ByteArrayOutputStream();
     * XmlMappers.toXml(N.asMap("name", "Bob"), out, config);   // writes indented <ImmutableMap>...</ImmutableMap>
     * String xml = out.toString();                             // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output stream to write the XML to
     * @param config the serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if the object cannot be serialized as XML, or writing, flushing, or closing {@code output} fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final OutputStream output, final SerializationConfig config) throws RuntimeException, IllegalArgumentException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to an XML writer using default configuration.
     * Note: the writer is closed after writing, because {@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}
     * is enabled by default on the underlying mapper.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * try (StringWriter writer = new StringWriter()) {
     *     XmlMappers.toXml(person, writer);
     *     String xml = writer.toString();
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the writer to write the XML to
     * @throws RuntimeException if the object cannot be serialized as XML, or writing, flushing, or closing {@code output} fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final Writer output) throws RuntimeException, IllegalArgumentException {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML writer using a custom serialization configuration.
     * The writer is closed after writing unless the supplied config disables
     * {@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
     * StringWriter writer = new StringWriter();
     * XmlMappers.toXml(N.asMap("name", "Bob"), writer, config);   // writes indented XML to the writer
     * String xml = writer.toString();                             // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the writer to write the XML to
     * @param config the serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if the object cannot be serialized as XML, or writing, flushing, or closing {@code output} fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final Writer output, final SerializationConfig config) throws RuntimeException, IllegalArgumentException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to a DataOutput using default configuration.
     * This method is useful for writing XML to binary protocols.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
     * DataOutput out = new DataOutputStream(bytes);
     * XmlMappers.toXml(N.asMap("name", "Bob"), out);   // writes <ImmutableMap><name>Bob</name></ImmutableMap>
     * String xml = bytes.toString();                   // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the DataOutput to write the XML to
     * @throws RuntimeException if the object cannot be serialized as XML or writing the XML to {@code output} fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final DataOutput output) throws RuntimeException, IllegalArgumentException {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to a DataOutput using a custom serialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
     * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
     * DataOutput out = new DataOutputStream(bytes);
     * XmlMappers.toXml(N.asMap("name", "Bob"), out, config);   // writes indented XML
     * String xml = bytes.toString();                           // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the DataOutput to write the XML to
     * @param config the serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if the object cannot be serialized as XML or writing the XML to {@code output} fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void toXml(final Object obj, final DataOutput output, final SerializationConfig config) throws RuntimeException, IllegalArgumentException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a byte array into an object of the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] xmlBytes = "<Person><name>John</name><age>30</age></Person>".getBytes();
     * Person person = XmlMappers.fromXml(xmlBytes, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array to deserialize
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a portion of a byte array into an object of the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = "##<LinkedHashMap><name>Bob</name></LinkedHashMap>".getBytes();
     * Map<String, Object> map = XmlMappers.fromXml(bytes, 2, bytes.length - 2, Map.class);
     * // map -> {name=Bob}; the leading "##" is skipped via offset 2
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array containing the data
     * @param offset the start offset in the array
     * @param len the number of bytes to read
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null} or {@code len} is negative.
     * @throws IndexOutOfBoundsException if the requested segment is outside {@code xml}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final int offset, final int len, final Class<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
        checkByteRange(xml, offset, len);

        try {
            return defaultXmlMapper.readValue(xml, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified type.
     * This is one of the most commonly used methods for XML deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<Person><name>John</name><age>30</age></Person>";
     * Person person = XmlMappers.fromXml(xml, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified type with custom deserialization features.
     *
     * <p>Note that each supplied feature is <i>enabled</i>; this method cannot disable a feature.
     * To disable a feature (or otherwise customize the configuration), use
     * {@link #fromXml(String, Class, DeserializationConfig)} with a config built via
     * {@link #createDeserializationConfig()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<Person><name>John</name><age>30</age></Person>";
     * Person person = XmlMappers.fromXml(xml, Person.class,
     *     DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the class of the object to deserialize to
     * @param first the first deserialization feature to enable
     * @param features additional deserialization features to enable
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType}, {@code first} or the {@code features} array is {@code null}.
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see #fromXml(String, Class, DeserializationConfig)
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    @SafeVarargs
    public static <T> T fromXml(final String xml, final Class<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(first, cs.first);
        N.checkArgNotNull(features, cs.features);

        // See toXml(Object, SerializationFeature, SerializationFeature...) for why this uses an ObjectReader
        // instead of a feature-derived DeserializationConfig.
        try {
            return defaultXmlMapper.reader(first, features).forType(targetType).readValue(xml);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
     * Map<String, Object> map = XmlMappers.fromXml(xml, Map.class, config);   // map -> {name=Bob}
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final Class<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("person.xml");
     * Person person = XmlMappers.fromXml(xmlFile, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * File xmlFile = new File("person.xml");
     * Person person = XmlMappers.fromXml(xmlFile, Person.class, config);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final Class<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified type.
     * Note: the stream is closed after reading, because {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}
     * is enabled by default on the underlying mapper.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileInputStream fis = new FileInputStream("person.xml")) {
     *     Person person = XmlMappers.fromXml(fis, Person.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified type using a custom deserialization configuration.
     * The stream is closed after reading unless the supplied config disables
     * {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
     * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
     *     Map<String, Object> map = XmlMappers.fromXml(in, Map.class, config);   // map -> {name=Bob}
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final Class<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified type.
     * Note: the reader is closed after reading, because {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}
     * is enabled by default on the underlying mapper.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (StringReader reader = new StringReader(xmlString)) {
     *     Person person = XmlMappers.fromXml(reader, Person.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified type using a custom deserialization configuration.
     * The reader is closed after reading unless the supplied config disables
     * {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
     * try (Reader reader = new StringReader(xml)) {
     *     Map<String, Object> map = XmlMappers.fromXml(reader, Map.class, config);   // map -> {name=Bob}
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final Class<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified type.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL xmlUrl = new URL("http://example.com/person.xml");
     * Person person = XmlMappers.fromXml(xmlUrl, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}.
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(xml, cs.xml);
        N.checkArgNotNull(targetType, cs.targetType);

        try (InputStream is = xml.openStream()) {
            return defaultXmlMapper.readValue(is, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified type using a custom deserialization configuration.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * URL xmlUrl = new File("person.xml").toURI().toURL();
     * Person person = XmlMappers.fromXml(xmlUrl, Person.class, config);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}.
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL xml, final Class<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(xml, cs.xml);
        N.checkArgNotNull(targetType, cs.targetType);

        final XmlMapper xmlMapper = getXmlMapper(config);

        try (InputStream is = xml.openStream()) {
            return xmlMapper.readValue(is, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified type.
     * Note: Jackson's XML format does not support reading from a {@code DataInput} source;
     * this method always fails at runtime (see example).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * Person person = XmlMappers.fromXml(in, Person.class);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the class of the object to deserialize to
     * @return never returns normally
     * @throws IllegalArgumentException if {@code xml} is {@code null}
     * @throws UnsupportedOperationException if {@code xml} is non-null; Jackson's XML backend cannot read from a {@code DataInput}
     *         source. Use the {@code InputStream}, {@code Reader}, {@code byte[]} or {@code String} overloads instead.
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final Class<? extends T> targetType) throws IllegalArgumentException, UnsupportedOperationException {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified type using a custom deserialization configuration.
     * Note: Jackson's XML format does not support reading from a {@code DataInput} source;
     * this method always fails at runtime (see example).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * Person person = XmlMappers.fromXml(in, Person.class, config);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return never returns normally
     * @throws IllegalArgumentException if {@code xml} is {@code null}
     * @throws UnsupportedOperationException if {@code xml} is non-null; Jackson's XML backend cannot read from a {@code DataInput}
     *         source. Use the {@code InputStream}, {@code Reader}, {@code byte[]} or {@code String} overloads instead.
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final Class<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, UnsupportedOperationException {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a byte array into an object of the specified generic type.
     * Use this method when deserializing generic types like List&lt;String&gt; or Map&lt;String, Object&gt;.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] xmlBytes = "<ArrayList><item>a</item><item>b</item></ArrayList>".getBytes();
     * List<String> list = XmlMappers.fromXml(xmlBytes, new TypeReference<List<String>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array to deserialize
     * @param targetType the type reference describing the target type
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a portion of a byte array into an object of the specified generic type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = "##<List><item>a</item><item>b</item></List>".getBytes();
     * List<String> list = XmlMappers.fromXml(bytes, 2, bytes.length - 2,
     *         new TypeReference<List<String>>() {});
     * // list -> [a, b]; the leading "##" is skipped via offset 2
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array containing the data
     * @param offset the start offset in the array
     * @param len the number of bytes to read
     * @param targetType the type reference describing the target type
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}, or {@code len} is negative.
     * @throws IndexOutOfBoundsException if the requested segment is outside {@code xml}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final int offset, final int len, final TypeReference<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        checkByteRange(xml, offset, len);

        try {
            return defaultXmlMapper.readValue(xml, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified generic type.
     * This is the most commonly used method for deserializing generic types from XML.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<LinkedHashMap><key1>value1</key1><key2>value2</key2></LinkedHashMap>";
     * Map<String, String> map = XmlMappers.fromXml(xml, new TypeReference<Map<String, String>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified generic type with custom deserialization features.
     *
     * <p>Note that each supplied feature is <i>enabled</i>; this method cannot disable a feature.
     * To disable a feature (or otherwise customize the configuration), use
     * {@link #fromXml(String, TypeReference, DeserializationConfig)} with a config built via
     * {@link #createDeserializationConfig()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<List><item>a</item><item>b</item></List>";
     * List<String> list = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {},
     *         DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the type reference describing the target type
     * @param first the first deserialization feature to enable
     * @param features additional deserialization features to enable
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType}, {@code first} or the {@code features} array is {@code null}.
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    @SafeVarargs
    public static <T> T fromXml(final String xml, final TypeReference<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(first, cs.first);
        N.checkArgNotNull(features, cs.features);

        // See toXml(Object, SerializationFeature, SerializationFeature...) for why this uses an ObjectReader
        // instead of a feature-derived DeserializationConfig.
        try {
            return defaultXmlMapper.reader(first, features).forType(targetType).readValue(xml);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified generic type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<List><item>a</item><item>b</item></List>";
     * List<String> list = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified generic type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("items.xml");                                                    // contains <List><item>a</item><item>b</item></List>
     * List<String> list = XmlMappers.fromXml(xmlFile, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified generic type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * File xmlFile = new File("items.xml");                                                            // contains <List><item>a</item><item>b</item></List>
     * List<String> list = XmlMappers.fromXml(xmlFile, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified generic type.
     * The stream is closed after reading (Jackson's default auto-close behavior).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
     *     List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified generic type using a custom deserialization configuration.
     * The stream is closed after reading unless the supplied config disables
     * {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
     *     List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified generic type.
     * The reader is closed after reading (Jackson's default auto-close behavior).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (Reader reader = new StringReader(xml)) {
     *     List<String> list = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified generic type using a custom deserialization configuration.
     * The reader is closed after reading unless the supplied config disables
     * {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (Reader reader = new StringReader(xml)) {
     *     List<String> list = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
     * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
     *         target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified generic type.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL xmlUrl = new File("items.xml").toURI().toURL();                                     // file contains a <List> of items
     * List<String> list = XmlMappers.fromXml(xmlUrl, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the type reference describing the target type
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}.
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(xml, cs.xml);
        N.checkArgNotNull(targetType, cs.targetType);

        try (InputStream is = xml.openStream()) {
            return defaultXmlMapper.readValue(is, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified generic type using a custom deserialization configuration.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * URL xmlUrl = new File("items.xml").toURI().toURL();                                             // file contains a <List> of items
     * List<String> list = XmlMappers.fromXml(xmlUrl, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the type reference describing the target type
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return the deserialized object
     * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}.
     * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
     *         requested target type
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final URL xml, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(xml, cs.xml);
        N.checkArgNotNull(targetType, cs.targetType);

        final XmlMapper xmlMapper = getXmlMapper(config);

        try (InputStream is = xml.openStream()) {
            return xmlMapper.readValue(is, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified generic type.
     * Note: Jackson's XML format does not support reading from a {@code DataInput} source;
     * this method always fails at runtime (see example).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the type reference describing the target type
     * @return never returns normally
     * @throws IllegalArgumentException if {@code targetType} or {@code xml} is {@code null}
     * @throws UnsupportedOperationException if both arguments are non-null; Jackson's XML backend cannot read from a {@code DataInput}
     *         source. Use the {@code InputStream}, {@code Reader}, {@code byte[]} or {@code String} overloads instead.
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final TypeReference<? extends T> targetType)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultXmlMapper.readValue(xml, defaultXmlMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified generic type using a custom deserialization configuration.
     * Note: Jackson's XML format does not support reading from a {@code DataInput} source;
     * this method always fails at runtime (see example).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {}, config);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the type reference describing the target type
     * @param config the deserialization configuration to use; if {@code null}, uses default configuration
     * @return never returns normally
     * @throws IllegalArgumentException if {@code targetType} or {@code xml} is {@code null}
     * @throws UnsupportedOperationException if both arguments are non-null; Jackson's XML backend cannot read from a {@code DataInput}
     *         source. Use the {@code InputStream}, {@code Reader}, {@code byte[]} or {@code String} overloads instead.
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(targetType, cs.targetType);

        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, xmlMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Creates a new SerializationConfig instance with default settings.
     * This config can be customized and used with the toXml methods for fine-grained control over serialization.
     * The returned configuration carries its own {@code ConfigOverrides}, so {@code withPropertyInclusion(..)} -
     * which writes through that object in place and returns the same config - changes only the instance it is
     * invoked on. Jackson state reached <i>through</i> the configuration is <i>not</i> copied:
     * {@code getDefaultPrettyPrinter()} and {@code getDateFormat()} hand back process-wide instances, and
     * {@code getAnnotationIntrospector()} an object shared with this class's other configurations, so mutating one
     * of those in place changes output for mappers this caller never touched.
     *
     * <p>Each call builds and discards a complete copy of the default mapper, which costs a few hundred nanoseconds
     * and several kilobytes of garbage; create the configuration once and pass it repeatedly rather than calling
     * this per operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT)
     *     .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     * String xml = XmlMappers.toXml(object, config);
     * }</pre>
     *
     * @return a new SerializationConfig instance
     */
    public static SerializationConfig createSerializationConfig() {
        // Derived from a throw-away copy of the default mapper rather than from the default mapper itself:
        // with(..)/without(..) carry the mapper's ConfigOverrides by reference, and
        // SerializationConfig.withPropertyInclusion(..) writes through that shared object in place and returns
        // the same config, so a config derived directly from defaultXmlMapper would let one caller change
        // toXml(..) for the whole process. ObjectMapper.copy() does ConfigOverrides.copy(), giving the returned
        // config its own. (The copied mapper itself is discarded here - a SerializationConfig holds no stream
        // factory, so nothing about the XML hardening depends on this call.)
        return defaultXmlMapper.copy().getSerializationConfig();
    }

    /**
     * Creates a new {@link DeserializationConfig} instance with default settings.
     * This config can be customized and used with the fromXml methods for fine-grained control over deserialization.
     * The returned configuration carries its own {@code ConfigOverrides} rather than sharing this class's mappers'.
     * Jackson state reached <i>through</i> the configuration is <i>not</i> copied: {@code getDateFormat()} hands
     * back a process-wide instance and {@code getAnnotationIntrospector()} an object shared with this class's other
     * configurations, so mutating one of those in place changes output for mappers this caller never touched.
     *
     * <p>Each call builds and discards a complete copy of the default mapper, which costs a few hundred nanoseconds
     * and several kilobytes of garbage; create the configuration once and pass it repeatedly rather than calling
     * this per operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * Person person = XmlMappers.fromXml(xml, Person.class, config);
     * }</pre>
     *
     * @return a new {@link DeserializationConfig} instance
     */
    public static DeserializationConfig createDeserializationConfig() {
        // Same reason as createSerializationConfig(): copy() is what gives the returned config a ConfigOverrides
        // of its own instead of defaultXmlMapper's.
        return defaultXmlMapper.copy().getDeserializationConfig();
    }

    /**
     * Retrieves the mapper permanently bound to the specified serialization configuration, creating a
     * security-hardened mapper if this configuration has not been seen before.
     *
     * @param config the serialization configuration to apply, or {@code null} to use the default mapper
     * @return an XmlMapper configured with the specified serialization config
     */
    private static XmlMapper getXmlMapper(final SerializationConfig config) {
        if (config == null) {
            return defaultXmlMapper;
        }

        synchronized (serializationMapperPool) {
            XmlMapper mapper = serializationMapperPool.get(config);
            if (mapper == null) {
                // copy() reuses the default mapper's already-hardened XMLInputFactory instead of running a fresh
                // XMLInputFactory.newFactory() ServiceLoader lookup and re-applying the security properties. That
                // makes the hardening structurally guaranteed here rather than re-derived, and is ~80x cheaper -
                // which matters because this runs while the pool monitor is held.
                mapper = defaultXmlMapper.copy();
                mapper.setConfig(config);
                serializationMapperPool.put(config, mapper);
            }

            return mapper;
        }
    }

    /**
     * Retrieves the mapper permanently bound to the specified deserialization configuration, creating a
     * security-hardened mapper if this configuration has not been seen before.
     *
     * @param config the deserialization configuration to apply, or {@code null} to use the default mapper
     * @return an XmlMapper configured with the specified deserialization config
     */
    private static XmlMapper getXmlMapper(final DeserializationConfig config) {
        if (config == null) {
            return defaultXmlMapper;
        }

        synchronized (deserializationMapperPool) {
            XmlMapper mapper = deserializationMapperPool.get(config);
            if (mapper == null) {
                // copy() reuses the default mapper's already-hardened XMLInputFactory instead of running a fresh
                // XMLInputFactory.newFactory() ServiceLoader lookup and re-applying the security properties. That
                // makes the hardening structurally guaranteed here rather than re-derived, and is ~80x cheaper -
                // which matters because this runs while the pool monitor is held.
                mapper = defaultXmlMapper.copy();
                mapper.setConfig(config);
                deserializationMapperPool.put(config, mapper);
            }

            return mapper;
        }
    }

    /**
     * Configuration-bound mappers remain in the bounded caches above. They must not be reconfigured
     * after use because Jackson retains serializers and root deserializers internally.
     *
     * @param mapper the mapper used by the completed operation; retained by its configuration cache
     */
    private static void recycle(@SuppressWarnings("unused") final XmlMapper mapper) {
        // No-op: see method documentation. The call sites retain their uniform finally blocks.
    }

    /**
     * Wraps an XmlMapper instance to provide convenient serialization and deserialization methods.
     * This allows you to use a pre-configured XmlMapper with the same convenient API as the static methods.
     * The supplied mapper is used as-is and is not security-hardened or otherwise reconfigured by this method.
     * Before reading untrusted XML, configure its {@link XMLInputFactory} to disable DTD and external-entity
     * support and install a rejecting XML resolver.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlMapper customMapper = new XmlMapper();
     * customMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
     * XmlMappers.One wrapper = XmlMappers.wrap(customMapper);
     * String xml = wrapper.toXml(person);
     * }</pre>
     *
     * @param xmlMapper the XmlMapper instance to wrap; must not be {@code null}. Subclasses are supported:
     *        no {@link com.fasterxml.jackson.databind.ObjectMapper#copy()} is performed, so mappers that do not
     *        override {@code copy()} are accepted.
     * @return a {@link One} instance wrapping the provided XmlMapper
     * @throws IllegalArgumentException if {@code xmlMapper} is {@code null}
     * @see One
     */
    public static One wrap(final XmlMapper xmlMapper) throws IllegalArgumentException {
        return new One(N.checkArgNotNull(xmlMapper, cs.xmlMapper));
    }

    /**
     * A wrapper class that provides convenient instance methods for XML serialization and deserialization
     * using a specific XmlMapper instance. This class mirrors the static methods of XmlMappers but uses
     * the wrapped XmlMapper for all operations.
     *
     * <p>This is useful when you need to use a customized XmlMapper repeatedly without having to
     * pass configuration objects to every method call.</p>
     *
     * <p>The wrapped mapper retains the caller's XML parser configuration, and the derived pretty-print
     * {@link ObjectWriter} inherits it. Consequently, the caller is responsible for hardening that
     * configuration before parsing untrusted XML.</p>
     *
     * <p>Configure the supplied mapper before wrapping it and do not mutate it while this wrapper is in use:
     * compact output and every read go straight to the wrapped mapper, while pretty-printed output goes through
     * an {@link ObjectWriter} derived from it at construction time, so mutating the mapper afterwards can make
     * the two paths disagree.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlMapper mapper = new XmlMapper();
     * mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
     * XmlMappers.One xmlMappers = XmlMappers.wrap(mapper);
     *
     * // Use the wrapped mapper for multiple operations
     * String xml1 = xmlMappers.toXml(object1);
     * String xml2 = xmlMappers.toXml(object2, true);   // pretty-printed XML
     * Person person = xmlMappers.fromXml(xmlString, Person.class);
     * }</pre>
     *
     */
    public static final class One {
        private final XmlMapper xmlMapper;
        private final ObjectWriter xmlWriterForPretty;

        /**
         * Creates a {@code One} instance wrapping the specified {@link XmlMapper}.
         * An {@link ObjectWriter} with pretty printing enabled is derived from the mapper
         * to support pretty-print serialization; the mapper itself is neither copied nor modified.
         * The mapper's XML parser factory is not security-hardened by this constructor.
         *
         * @param xmlMapper the XmlMapper to wrap; must not be {@code null}
         * @throws IllegalArgumentException if {@code xmlMapper} is {@code null}
         */
        One(final XmlMapper xmlMapper) throws IllegalArgumentException {
            N.checkArgNotNull(xmlMapper, cs.xmlMapper);

            this.xmlMapper = xmlMapper;
            // Deriving an ObjectWriter instead of copy()-ing the mapper: ObjectMapper.copy() throws
            // IllegalStateException for any subclass that does not override it, which made wrap() reject
            // perfectly usable custom mappers. writer(INDENT_OUTPUT) - rather than
            // writerWithDefaultPrettyPrinter() - keeps the feature enabled on the writer's config, so a
            // custom serializer querying isEnabled(INDENT_OUTPUT) still sees true.
            xmlWriterForPretty = xmlMapper.writer(SerializationFeature.INDENT_OUTPUT);
        }

        /**
         * Serializes the specified object to an XML string using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person person = new Person("John", 30);
         * String xml = xmlMappers.toXml(person);
         * }</pre>
         *
         * @param obj the object to serialize
         * @return the XML string representation of the object
         * @throws RuntimeException if the configured mapper cannot serialize the object as XML
         */
        public String toXml(final Object obj) throws RuntimeException {
            try {
                return xmlMapper.writeValueAsString(obj);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to an XML string with optional pretty formatting
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String compact = xmlMappers.toXml(N.asMap("name", "Bob"), false);   // <ImmutableMap><name>Bob</name></ImmutableMap>
         * String pretty = xmlMappers.toXml(N.asMap("name", "Bob"), true);     // same content, indented across lines
         * }</pre>
         *
         * @param obj the object to serialize
         * @param prettyFormat {@code true} to enable pretty printing with indentation; {@code false} to serialize with
         *                     the wrapped mapper exactly as configured. Note that {@code false} does not force compact
         *                     output: a wrapped mapper that already enables
         *                     {@link SerializationFeature#INDENT_OUTPUT} still produces indented XML. This flag can
         *                     only add pretty printing, never remove it.
         * @return the XML string representation of the object
         * @throws RuntimeException if the configured mapper cannot serialize the object as XML
         */
        public String toXml(final Object obj, final boolean prettyFormat) throws RuntimeException {
            try {
                if (prettyFormat) {
                    return xmlWriterForPretty.writeValueAsString(obj);
                } else {
                    return xmlMapper.writeValueAsString(obj);
                }
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to an XML file using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * File output = new File("person.xml");
         * xmlMappers.toXml(N.asMap("name", "Bob"), output);   // writes <ImmutableMap><name>Bob</name></ImmutableMap>
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the output file to write the XML to
         * @throws RuntimeException if the object cannot be serialized as XML, or opening, writing, flushing, or closing the output file fails
         * @throws IllegalArgumentException if {@code output} is {@code null}
         */
        public void toXml(final Object obj, final File output) throws RuntimeException, IllegalArgumentException {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to an output stream using the wrapped XmlMapper.
         * Note: with Jackson's default settings the wrapped mapper closes the stream after
         * writing ({@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * ByteArrayOutputStream out = new ByteArrayOutputStream();
         * xmlMappers.toXml(N.asMap("name", "Bob"), out);   // writes XML; out contains "<name>Bob</name>"
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the output stream to write the XML to
         * @throws RuntimeException if the object cannot be serialized as XML, or writing, flushing, or closing {@code output} fails
         * @throws IllegalArgumentException if {@code output} is {@code null}
         */
        public void toXml(final Object obj, final OutputStream output) throws RuntimeException, IllegalArgumentException {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to a writer using the wrapped XmlMapper.
         * Note: with Jackson's default settings the wrapped mapper closes the writer after
         * writing ({@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * StringWriter writer = new StringWriter();
         * xmlMappers.toXml(N.asMap("name", "Bob"), writer);   // writer.toString() contains "<name>Bob</name>"
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the writer to write the XML to
         * @throws RuntimeException if the object cannot be serialized as XML, or writing, flushing, or closing {@code output} fails
         * @throws IllegalArgumentException if {@code output} is {@code null}
         */
        public void toXml(final Object obj, final Writer output) throws RuntimeException, IllegalArgumentException {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to a DataOutput using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         * DataOutput out = new DataOutputStream(bytes);
         * xmlMappers.toXml(N.asMap("name", "Bob"), out);   // bytes.toString() contains "<name>Bob</name>"
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the DataOutput to write the XML to
         * @throws RuntimeException if the object cannot be serialized as XML or writing the XML to {@code output} fails
         * @throws IllegalArgumentException if {@code output} is {@code null}
         */
        public void toXml(final Object obj, final DataOutput output) throws RuntimeException, IllegalArgumentException {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a byte array into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "<LinkedHashMap><name>Bob</name></LinkedHashMap>".getBytes();
         * Map<String, Object> map = xmlMappers.fromXml(bytes, Map.class);   // map -> {name=Bob}
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array to deserialize
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
         * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a portion of a byte array into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "##<LinkedHashMap><name>Bob</name></LinkedHashMap>".getBytes();
         * Map<String, Object> map = xmlMappers.fromXml(bytes, 2, bytes.length - 2, Map.class);   // map -> {name=Bob}
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array containing the data
         * @param offset the start offset in the array
         * @param len the number of bytes to read
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} is {@code null} or {@code len} is negative.
         * @throws IndexOutOfBoundsException if the requested segment is outside {@code xml}
         * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final int offset, final int len, final Class<? extends T> targetType)
                throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
            checkByteRange(xml, offset, len);

            try {
                return xmlMapper.readValue(xml, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes an XML string into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String xml = "<Person><name>John</name><age>30</age></Person>";
         * Person person = xmlMappers.fromXml(xml, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML string to deserialize
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
         * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a file into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * File xmlFile = new File("person.xml");
         * Person person = xmlMappers.fromXml(xmlFile, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML file to read from
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
         * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
         *         requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final File xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from an input stream into an object of the specified type
         * using the wrapped XmlMapper. With Jackson's default settings the stream is
         * closed after reading ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
         * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
         *     Map<String, Object> map = xmlMappers.fromXml(in, Map.class);   // map -> {name=Bob}
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the input stream containing XML data
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
         * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
         *         target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final InputStream xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a reader into an object of the specified type
         * using the wrapped XmlMapper. With Jackson's default settings the reader is
         * closed after reading ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
         * try (Reader reader = new StringReader(xml)) {
         *     Map<String, Object> map = xmlMappers.fromXml(reader, Map.class);   // map -> {name=Bob}
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the reader containing XML data
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}
         * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
         *         target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final Reader xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a URL into an object of the specified type
         * using the wrapped XmlMapper.
         * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * URL xmlUrl = new File("person.xml").toURI().toURL();
         * Person person = xmlMappers.fromXml(xmlUrl, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the URL pointing to XML data
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}.
         * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
         *         requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final URL xml, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(xml, cs.xml);
            N.checkArgNotNull(targetType, cs.targetType);

            try (InputStream is = xml.openStream()) {
                return xmlMapper.readValue(is, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a DataInput into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
         * // Note: the underlying Jackson XML format does not support reading from a DataInput
         * // source, so this call throws an UnsupportedOperationException at runtime.
         * Person person = xmlMappers.fromXml(in, Person.class);   // throws UnsupportedOperationException
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the DataInput containing XML data
         * @param targetType the class of the object to deserialize to
         * @return never returns normally
         * @throws IllegalArgumentException if {@code xml} is {@code null}
         * @throws UnsupportedOperationException if {@code xml} is non-null; Jackson's XML backend cannot read from a {@code DataInput}
         *         source. Use the {@code InputStream}, {@code Reader}, {@code byte[]} or {@code String} overloads instead.
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final DataInput xml, final Class<? extends T> targetType) throws IllegalArgumentException, UnsupportedOperationException {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a byte array into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "<List><item>a</item><item>b</item></List>".getBytes();
         * List<String> list = xmlMappers.fromXml(bytes, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array to deserialize
         * @param targetType the type reference describing the target type
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
         * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a portion of a byte array into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "##<List><item>a</item><item>b</item></List>".getBytes();
         * List<String> list = xmlMappers.fromXml(bytes, 2, bytes.length - 2,
         *         new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array containing the data
         * @param offset the start offset in the array
         * @param len the number of bytes to read
         * @param targetType the type reference describing the target type
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}, or {@code len} is negative.
         * @throws IndexOutOfBoundsException if the requested segment is outside {@code xml}
         * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final int offset, final int len, final TypeReference<? extends T> targetType)
                throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            checkByteRange(xml, offset, len);

            try {
                return xmlMapper.readValue(xml, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes an XML string into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
         * List<String> list = xmlMappers.fromXml(xml, new TypeReference<List<String>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML string to deserialize
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
         * @throws RuntimeException if the XML cannot be parsed or its content cannot be converted to the requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a file into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * File xmlFile = new File("items.xml");                                                    // contains <List><item>a</item><item>b</item></List>
         * List<String> list = xmlMappers.fromXml(xmlFile, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML file to read from
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
         * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
         *         requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final File xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from an input stream into an object of the specified generic type
         * using the wrapped XmlMapper. With Jackson's default settings the stream is
         * closed after reading ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<List><item>a</item><item>b</item></List>";
         * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
         *     List<String> list = xmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the input stream containing XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
         * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
         *         target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final InputStream xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a reader into an object of the specified generic type
         * using the wrapped XmlMapper. With Jackson's default settings the reader is
         * closed after reading ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<List><item>a</item><item>b</item></List>";
         * try (Reader reader = new StringReader(xml)) {
         *     List<String> list = xmlMappers.fromXml(reader, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the reader containing XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} is {@code null}, or {@code targetType} is {@code null}
         * @throws RuntimeException if reading or closing {@code xml} fails, the XML cannot be parsed, or its content cannot be converted to the requested
         *         target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final Reader xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a URL into an object of the specified generic type
         * using the wrapped XmlMapper.
         * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * URL xmlUrl = new File("items.xml").toURI().toURL();                                     // file contains a <List> of items
         * List<String> list = xmlMappers.fromXml(xmlUrl, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the URL pointing to XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws IllegalArgumentException if {@code xml} or {@code targetType} is {@code null}.
         * @throws RuntimeException if the XML input cannot be opened, read, or closed, the XML cannot be parsed, or its content cannot be converted to the
         *         requested target type
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final URL xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(xml, cs.xml);
            N.checkArgNotNull(targetType, cs.targetType);

            try (InputStream is = xml.openStream()) {
                return xmlMapper.readValue(is, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a DataInput into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
         * // Note: the underlying Jackson XML format does not support reading from a DataInput
         * // source, so this call throws an UnsupportedOperationException at runtime.
         * List<String> list = xmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // throws UnsupportedOperationException
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the DataInput containing XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return never returns normally
         * @throws IllegalArgumentException if {@code targetType} or {@code xml} is {@code null}
         * @throws UnsupportedOperationException if both arguments are non-null; Jackson's XML backend cannot read from a {@code DataInput}
         *         source. Use the {@code InputStream}, {@code Reader}, {@code byte[]} or {@code String} overloads instead.
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final DataInput xml, final TypeReference<? extends T> targetType) throws IllegalArgumentException, UnsupportedOperationException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return xmlMapper.readValue(xml, xmlMapper.constructType(targetType));
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }
}
