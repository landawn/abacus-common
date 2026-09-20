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

package com.landawn.abacus.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.XmlConstants;
import com.landawn.abacus.type.Type;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * A comprehensive utility class providing various XML processing capabilities including JAXB marshalling/unmarshalling,
 * DOM manipulation, SAX parsing, StAX processing, and XML transformation operations.
 *
 * <p>This class offers a wide range of static methods for:</p>
 * <ul>
 *   <li>JAXB operations (marshal/unmarshal with caching)</li>
 *   <li>DOM parsing and manipulation</li>
 *   <li>SAX parser creation with pooling</li>
 *   <li>StAX reader/writer creation</li>
 *   <li>XML transformation</li>
 *   <li>XML encoding/decoding using Java beans</li>
 *   <li>Character encoding for XML content</li>
 *   <li>Node and attribute manipulation</li>
 * </ul>
 *
 * <p>The class pools parsers and opportunistically reuses JAXB contexts. JAXB contexts are scoped by
 * binding class or context path and by the identity of the current thread context class loader.
 * Context-path construction uses that captured loader explicitly; class-based construction retains
 * JAXB's normal provider discovery. Weak context and loader references allow these caches to release
 * unused binding and provider loaders. A context may be rebuilt after garbage collection.</p>
 *
 * <p>Keep JAXB provider/discovery configuration stable for a given context loader during reuse.
 * Changing service configuration or system properties under the same loader does not guarantee an
 * immediate rebuild. This cache policy does not control references retained by providers or other
 * libraries. Each request creates a separate marshaller or unmarshaller; those objects remain subject
 * to their provider's thread-safety rules.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // JAXB marshalling
 * Person person = new Person("John", 30);
 * String xml = XmlUtil.marshal(person);
 *
 * // JAXB unmarshalling
 * Person p = XmlUtil.unmarshal(Person.class, xml);
 *
 * // DOM parsing
 * DocumentBuilder parser = XmlUtil.createDOMParser();
 * Document doc = parser.parse(new File("data.xml"));
 *
 * // Get element attributes
 * Element elem = doc.getDocumentElement();
 * String attr = XmlUtil.getAttribute(elem, "id");
 * }</pre>
 *
 * <p>This class is not instantiable.</p>
 *
 * <p><b>System properties:</b></p>
 * <ul>
 *   <li>{@code abacus.xml.allowXmlEncoderDecoder} - enables the deprecated {@link #xmlEncode(Object)} /
 *       {@link #xmlDecode(String)} pair. Read once, when this class is initialized.</li>
 *   <li>{@code abacus.xml.allowTypeAttrClassForName} - lets a {@code type} attribute in deserialized XML
 *       name any class, instead of only those on the built-in allowlist. Attacker-controlled type names
 *       feed reflective construction, so enable this only for trusted XML. Unlike the property above it
 *       is read on <i>every</i> resolution, so it can be toggled at runtime.</li>
 * </ul>
 *
 * @see XmlMappers
 * @see javax.xml.parsers.DocumentBuilder
 * @see jakarta.xml.bind.JAXBContext
 */
public final class XmlUtil {

    /** Logger for this class, used to report security configuration warnings. */
    protected static final Logger logger = LoggerFactory.getLogger(XmlUtil.class);

    static final String NAME = "name";

    static final String TYPE = "type";

    private static final int POOL_SIZE = 1000;

    private static final Set<String> CRITICAL_SECURITY_NAMES = Set.of(XMLConstants.FEATURE_SECURE_PROCESSING,
            "http://apache.org/xml/features/disallow-doctype-decl", "http://xml.org/sax/features/external-general-entities",
            "http://xml.org/sax/features/external-parameter-entities", "http://apache.org/xml/features/nonvalidating/load-external-dtd",
            XMLInputFactory.SUPPORT_DTD, XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, XMLConstants.ACCESS_EXTERNAL_DTD, XMLConstants.ACCESS_EXTERNAL_SCHEMA,
            XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "XIncludeAware", "expandEntityReferences", "XMLResolver");

    // Hardened SAX parser configuration and reusable parser pool.
    private static final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    static {
        setSaxParserFactoryFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setSaxParserFactoryFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        setSaxParserFactoryFeature("http://xml.org/sax/features/external-general-entities", false);
        setSaxParserFactoryFeature("http://xml.org/sax/features/external-parameter-entities", false);
        setSaxParserFactoryFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        setSaxParserFactoryXIncludeAware(false);
        // Namespace-awareness must be on for any code that walks the DOM by namespace URI (e.g.
        // XML signature verification, XPath with namespaces). The factory default is false.
        saxParserFactory.setNamespaceAware(true);
    }

    private static final Queue<SAXParser> saxParserPool = new ArrayBlockingQueue<>(POOL_SIZE);
    private static final WeakIdentitySet<SAXParser> ownedSaxParsers = new WeakIdentitySet<>();

    /**
     * Which parsers are currently sitting in {@link #saxParserPool}. Guarded by that queue's monitor.
     *
     * <p>Recycling used to scan the whole queue for an identity match, which is O(POOL_SIZE) (1000) per
     * call on what is meant to be the fast path. This membership set answers the same question in
     * constant time.</p>
     */
    private static final Map<SAXParser, Boolean> pooledSaxParsers = new IdentityHashMap<>();

    // Hardened DOM builder configuration and reusable builder pool.
    private static final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

    static {
        setDocumentBuilderFactoryFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setDocumentBuilderFactoryFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        setDocumentBuilderFactoryFeature("http://xml.org/sax/features/external-general-entities", false);
        setDocumentBuilderFactoryFeature("http://xml.org/sax/features/external-parameter-entities", false);
        setDocumentBuilderFactoryFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        setDocumentBuilderFactoryXIncludeAware(false);
        setDocumentBuilderFactoryExpandEntityReferences(false);
        setDocumentBuilderFactoryAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setDocumentBuilderFactoryAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        // Namespace-awareness must be on so DOMs preserve xmlns:* declarations and so XPath /
        // attribute lookups by namespace URI work. Factory default is false.
        docBuilderFactory.setNamespaceAware(true);
    }

    private static final Queue<DocumentBuilder> contentDocBuilderPool = new ArrayBlockingQueue<>(POOL_SIZE);
    private static final WeakIdentitySet<DocumentBuilder> ownedContentParsers = new WeakIdentitySet<>();

    /** Which builders are currently in {@link #contentDocBuilderPool}; see {@link #pooledSaxParsers}. */
    private static final Map<DocumentBuilder, Boolean> pooledContentParsers = new IdentityHashMap<>();

    // private static final int BUFFER_SIZE = 1024 * 16; // 16KB
    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        setXmlInputFactoryProperty(XMLInputFactory.SUPPORT_DTD, false);
        setXmlInputFactoryProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        // IS_REPLACING_ENTITY_REFERENCES must remain true (the factory default). When false, the
        // JDK StAX impl emits ENTITY_REFERENCE events for predefined entities (&amp;, &lt;, &gt;, etc.)
        // which the deserializers then drop, silently corrupting any string containing them. With
        // SUPPORT_DTD and IS_SUPPORTING_EXTERNAL_ENTITIES already false, replacement is safe.
        setXmlInputFactoryProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        setXmlInputFactoryResolver();

        try {
            if (Class.forName("com.ctc.wstx.stax.WstxInputFactory").isAssignableFrom(xmlInputFactory.getClass())) {
                // xmlInputFactory.setProperty("com.ctc.wstx.inputBufferLength", BUFFER_SIZE);
            } else {
                if (logger.isWarnEnabled()) {
                    logger.warn("It's recommended to use woodstox: https://github.com/FasterXML/woodstox");
                }

                // xmlInputFactory.setProperty("javax.xml.stream.bufferSize", BUFFER_SIZE);   // 8KB buffer
            }
        } catch (final Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn("It's recommended to use woodstox: https://github.com/FasterXML/woodstox");
            }

            // xmlInputFactory.setProperty("javax.xml.stream.bufferSize", BUFFER_SIZE);   // 8KB buffer
        }
    }

    // private static final Queue<DocumentBuilder> xmlInputPool = new ArrayBlockingQueue<>(POOL_SIZE);
    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    // private static final Queue<DocumentBuilder> xmlOutputPool = new ArrayBlockingQueue<>(POOL_SIZE);

    // Hardened transformer configuration.
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    // private static final Queue<DocumentBuilder> xmlTransferPool = new ArrayBlockingQueue<>(POOL_SIZE);

    static {
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            if (!transformerFactory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING)) {
                throw new IllegalStateException("TransformerFactory ignored FEATURE_SECURE_PROCESSING");
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("TransformerFactory feature", XMLConstants.FEATURE_SECURE_PROCESSING, e);
        }

        setTransformerFactoryAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        setTransformerFactoryAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

    // JAXB contexts and XML node-name metadata.
    private static final LoaderJaxbCache<String> pathJaxbContextPool = new LoaderJaxbCache<>();

    private static final ClassValue<LoaderJaxbCache<Boolean>> classJaxbContextPool = new ClassValue<>() {
        @Override
        protected LoaderJaxbCache<Boolean> computeValue(final Class<?> type) {
            return new LoaderJaxbCache<>();
        }
    };

    private static final Map<String, NodeType> nodeTypePool = new HashMap<>();

    static {
        nodeTypePool.put(XmlConstants.ARRAY, NodeType.ARRAY);
        nodeTypePool.put(XmlConstants.LIST, NodeType.COLLECTION);
        nodeTypePool.put(XmlConstants.SET, NodeType.COLLECTION);
        nodeTypePool.put(XmlConstants.COLLECTION, NodeType.COLLECTION);
        nodeTypePool.put(XmlConstants.E, NodeType.ELEMENT);
        nodeTypePool.put(XmlConstants.MAP, NodeType.MAP);
        nodeTypePool.put(XmlConstants.ENTRY, NodeType.ENTRY);
        nodeTypePool.put(XmlConstants.KEY, NodeType.KEY);
        nodeTypePool.put(XmlConstants.VALUE, NodeType.VALUE);
    }

    private XmlUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Reports a failure to apply an XML factory feature/property/attribute.
     *
     * <p>If {@code name} is one of {@link #CRITICAL_SECURITY_NAMES}, failing open would leave XML
     * processing running with a silently weakened security policy, so class initialization is
     * aborted instead. Non-critical settings are only logged.</p>
     *
     * @param factory the factory whose setting could not be applied
     * @param name the feature/property/attribute name
     * @param e the failure
     *
     * @throws ExceptionInInitializerError if {@code name} identifies a required XML security setting
     */
    private static void logFactoryFailure(final String factory, final String name, final Exception e) {
        // For critical XXE flags, fail-open is dangerous: abort class initialization instead of
        // continuing with unrestricted DTD/external-entity processing.
        if (CRITICAL_SECURITY_NAMES.contains(name)) {
            logger.warn(e, "Failed to apply required XML security setting '{}' on {}", name, factory);
            throw new ExceptionInInitializerError(e);
        } else if (logger.isDebugEnabled()) {
            logger.debug(e, "Failed to set {} '{}'", factory, name);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if {@code featureName} is a required security setting and the provider rejects or ignores it
     */
    private static void setSaxParserFactoryFeature(final String featureName, final boolean value) {
        try {
            saxParserFactory.setFeature(featureName, value);

            if (saxParserFactory.getFeature(featureName) != value) {
                throw new IllegalStateException("SAXParserFactory ignored feature: " + featureName);
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("SAXParserFactory feature", featureName, e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if the XInclude setting is a required security setting and the provider rejects or ignores it
     */
    private static void setSaxParserFactoryXIncludeAware(final boolean value) {
        try {
            saxParserFactory.setXIncludeAware(value);

            if (saxParserFactory.isXIncludeAware() != value) {
                throw new IllegalStateException("SAXParserFactory ignored XIncludeAware");
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("SAXParserFactory", "XIncludeAware", e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if {@code featureName} is a required security setting and the provider rejects or ignores it
     */
    private static void setDocumentBuilderFactoryFeature(final String featureName, final boolean value) {
        try {
            docBuilderFactory.setFeature(featureName, value);

            if (docBuilderFactory.getFeature(featureName) != value) {
                throw new IllegalStateException("DocumentBuilderFactory ignored feature: " + featureName);
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("DocumentBuilderFactory feature", featureName, e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if the XInclude setting is a required security setting and the provider rejects or ignores it
     */
    private static void setDocumentBuilderFactoryXIncludeAware(final boolean value) {
        try {
            docBuilderFactory.setXIncludeAware(value);

            if (docBuilderFactory.isXIncludeAware() != value) {
                throw new IllegalStateException("DocumentBuilderFactory ignored XIncludeAware");
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("DocumentBuilderFactory", "XIncludeAware", e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if the entity-expansion setting is a required security setting and the provider rejects or ignores it
     */
    private static void setDocumentBuilderFactoryExpandEntityReferences(final boolean value) {
        try {
            docBuilderFactory.setExpandEntityReferences(value);

            if (docBuilderFactory.isExpandEntityReferences() != value) {
                throw new IllegalStateException("DocumentBuilderFactory ignored expandEntityReferences");
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("DocumentBuilderFactory", "expandEntityReferences", e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if {@code attributeName} is a required security setting and the provider rejects or ignores it
     */
    private static void setDocumentBuilderFactoryAttribute(final String attributeName, final String value) {
        try {
            docBuilderFactory.setAttribute(attributeName, value);

            if (!value.equals(docBuilderFactory.getAttribute(attributeName))) {
                throw new IllegalStateException("DocumentBuilderFactory ignored attribute: " + attributeName);
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("DocumentBuilderFactory attribute", attributeName, e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if {@code propertyName} is a required security setting and the provider rejects or ignores it
     */
    private static void setXmlInputFactoryProperty(final String propertyName, final Object value) {
        try {
            xmlInputFactory.setProperty(propertyName, value);

            if (!value.equals(xmlInputFactory.getProperty(propertyName))) {
                throw new IllegalStateException("XMLInputFactory ignored property: " + propertyName);
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("XMLInputFactory property", propertyName, e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if the external-entity resolver is a required security setting and the provider rejects or ignores it
     */
    private static void setXmlInputFactoryResolver() {
        try {
            xmlInputFactory.setXMLResolver((publicID, systemID, baseURI, namespace) -> {
                throw new XMLStreamException("External entity resolution is disabled");
            });

            if (xmlInputFactory.getXMLResolver() == null) {
                throw new IllegalStateException("XMLInputFactory ignored XMLResolver");
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("XMLInputFactory", "XMLResolver", e);
        }
    }

    /**
     * Applies an XML factory setting and verifies that the provider honors it.
     *
     * @throws ExceptionInInitializerError if {@code attributeName} is a required security setting and the provider rejects or ignores it
     */
    private static void setTransformerFactoryAttribute(final String attributeName, final String value) {
        try {
            transformerFactory.setAttribute(attributeName, value);

            if (!value.equals(transformerFactory.getAttribute(attributeName))) {
                throw new IllegalStateException("TransformerFactory ignored attribute: " + attributeName);
            }
        } catch (Exception e) { // NOSONAR
            logFactoryFailure("TransformerFactory attribute", attributeName, e);
        }
    }

    /**
     * Marshals the given JAXB bean into an XML string.
     * The JAXBContext is weakly cached for the bean's class and current context loader; see the class-level cache policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @XmlRootElement
     * public class Person {
     *     private String name;
     *     private int age;
     *
     *     public Person() {}
     *     public Person(String name, int age) { this.name = name; this.age = age; }
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public int getAge() { return age; }
     *     public void setAge(int age) { this.age = age; }
     * }
     *
     * Person person = new Person("John", 30);
     * String xml = XmlUtil.marshal(person);
     * // xml contains a <person> element with <name>John</name> and <age>30</age>.
     * }</pre>
     *
     * @param jaxbBean The JAXB-annotated bean to be marshalled (must not be {@code null})
     * @return The XML string representation of the JAXB bean, decoded as UTF-8
     * @throws IllegalArgumentException if {@code jaxbBean} is {@code null}
     * @throws RuntimeException if marshalling fails (e.g. a {@code JAXBException} is raised)
     * @throws UncheckedIOException if flushing the marshalling buffer fails
     * @see JAXBContext#newInstance(Class...)
     * @see Marshaller#marshal(Object, java.io.OutputStream)
     */
    public static String marshal(final Object jaxbBean) throws IllegalArgumentException, RuntimeException, UncheckedIOException {
        N.checkArgNotNull(jaxbBean, cs.jaxbBean);

        final Class<?> cls = jaxbBean.getClass();
        final ByteArrayOutputStream writer = Objectory.createByteArrayOutputStream();

        try {
            final JAXBContext jc = jaxbContext(cls);

            final Marshaller marshaller = jc.createMarshaller();
            marshaller.marshal(jaxbBean, writer);
            writer.flush();

            // JAXB's default output encoding for an OutputStream is UTF-8 (and the emitted XML
            // declaration says so); decode the bytes as UTF-8 rather than the platform default
            // charset, which would corrupt non-ASCII content on non-UTF-8 JVMs.
            return writer.toString(Charsets.UTF_8);
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(writer);
        }
    }

    /**
     * Unmarshals the given XML string into an object of the specified class.
     * The JAXBContext is weakly cached for the target class and current context loader; see the class-level cache policy.
     * Parsing uses the security-hardened StAX factory, and the internal stream reader is closed
     * before this method returns or propagates a parsing failure.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><age>30</age><name>John</name></person>";
     * Person person = XmlUtil.unmarshal(Person.class, xml);
     * System.out.println(person.getName());   // prints John
     * }</pre>
     *
     * @param <T> The type of the object to be returned
     * @param cls The class of the object to be returned (must be JAXB-annotated)
     * @param xml The XML string to be unmarshalled (must not be {@code null})
     * @return The unmarshalled object of the specified class
     * @throws IllegalArgumentException if {@code cls} or {@code xml} is {@code null}
     * @throws RuntimeException if secure XML parsing or JAXB unmarshalling fails
     * @throws ClassCastException if the XML root resolves to a JAXB object that is not an instance of {@code cls}
     * @see JAXBContext#newInstance(Class...)
     * @see Unmarshaller#unmarshal(XMLStreamReader)
     */
    public static <T> T unmarshal(final Class<? extends T> cls, final String xml) throws IllegalArgumentException, RuntimeException, ClassCastException {
        N.checkArgNotNull(cls, cs.cls);
        N.checkArgNotNull(xml, cs.xml);

        // Parse through the hardened StAX factory (DTD and external entities disabled) instead of
        // handing a raw Reader to JAXB, whose default unmarshaller would otherwise create its own
        // XXE-vulnerable parser and bypass the hardening every other parse path in this class uses.
        return unmarshalAndClose(cls, createXMLStreamReader(new StringReader(xml)));
    }

    /** Unmarshals from and always closes the supplied reader. Package-private for lifecycle testing. */
    static <T> T unmarshalAndClose(final Class<? extends T> cls, final XMLStreamReader xmlStreamReader) {
        try {
            final JAXBContext jc = jaxbContext(cls);

            final Unmarshaller unmarshaller = jc.createUnmarshaller();

            // A JAXB context also knows related roots; creating it for cls does not enforce the result type.
            return cls.cast(unmarshaller.unmarshal(xmlStreamReader));
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            try {
                xmlStreamReader.close();
            } catch (final XMLStreamException | RuntimeException e) {
                // The public entry point uses an in-memory StringReader and unmarshalling is already complete.
                // Do not replace a successful result (or the primary JAXB failure) with a
                // provider-specific cleanup failure.
                if (logger.isDebugEnabled()) {
                    logger.debug(e, "Failed to close XMLStreamReader after JAXB unmarshalling");
                }
            }
        }
    }

    /**
     * Creates a JAXB Marshaller for the given context path.
     * The JAXBContext is weakly cached for this binding and current context loader; see the class-level cache policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Marshaller marshaller = XmlUtil.createMarshaller("com.example.model");
     * marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
     * marshaller.marshal(jaxbObject, System.out);
     * }</pre>
     *
     * @param contextPath The context path for which to create the Marshaller (package names separated by ':')
     * @return The created Marshaller
     * @throws IllegalArgumentException if {@code contextPath} is {@code null}
     * @throws RuntimeException if the Marshaller cannot be created
     * @see JAXBContext#newInstance(String)
     * @see JAXBContext#createMarshaller()
     */
    public static Marshaller createMarshaller(final String contextPath) throws IllegalArgumentException, RuntimeException {
        try {
            final JAXBContext jc = jaxbContext(contextPath);

            return jc.createMarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a JAXB Marshaller for the given class.
     * The JAXBContext is weakly cached for this binding and current context loader; see the class-level cache policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Marshaller marshaller = XmlUtil.createMarshaller(Person.class);
     * marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
     * marshaller.marshal(person, new File("person.xml"));
     * }</pre>
     *
     * @param cls The class for which to create the Marshaller (must be JAXB-annotated)
     * @return The created Marshaller
     * @throws IllegalArgumentException if {@code cls} is {@code null}
     * @throws RuntimeException if the Marshaller cannot be created
     * @see JAXBContext#newInstance(Class...)
     * @see JAXBContext#createMarshaller()
     */
    public static Marshaller createMarshaller(final Class<?> cls) throws IllegalArgumentException, RuntimeException {
        try {
            final JAXBContext jc = jaxbContext(cls);

            return jc.createMarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a JAXB Unmarshaller for the given context path.
     * The JAXBContext is weakly cached for this binding and current context loader; see the class-level cache policy.
     *
     * <p><b>Security:</b> An {@code Unmarshaller} does not itself define the security policy of a
     * parser it creates for raw {@code File}, {@code Reader}, or {@code InputStream} inputs. Do not
     * use those overloads for untrusted XML. Supply a security-hardened {@link XMLStreamReader}, or
     * prefer {@link #unmarshal(Class, String)} when the target class is known.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Unmarshaller unmarshaller = XmlUtil.createUnmarshaller("com.example.model");
     * XMLStreamReader reader = XmlUtil.createXMLStreamReader(new StringReader(xml));
     * try {
     *     Object result = unmarshaller.unmarshal(reader);
     * } finally {
     *     reader.close();
     * }
     * }</pre>
     *
     * @param contextPath The context path for which to create the Unmarshaller (package names separated by ':')
     * @return The created Unmarshaller
     * @throws IllegalArgumentException if {@code contextPath} is {@code null}
     * @throws RuntimeException if the Unmarshaller cannot be created
     * @see JAXBContext#newInstance(String)
     * @see JAXBContext#createUnmarshaller()
     */
    public static Unmarshaller createUnmarshaller(final String contextPath) throws IllegalArgumentException, RuntimeException {
        try {
            final JAXBContext jc = jaxbContext(contextPath);

            return jc.createUnmarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a JAXB Unmarshaller for the given class.
     * The JAXBContext is weakly cached for this binding and current context loader; see the class-level cache policy.
     *
     * <p><b>Security:</b> An {@code Unmarshaller} does not itself define the security policy of a
     * parser it creates for raw {@code File}, {@code Reader}, or {@code InputStream} inputs. Do not
     * use those overloads for untrusted XML. Supply a security-hardened {@link XMLStreamReader}, or
     * prefer {@link #unmarshal(Class, String)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(Person.class);
     * XMLStreamReader reader = XmlUtil.createXMLStreamReader(new StringReader(xmlString));
     * try {
     *     Person person = (Person) unmarshaller.unmarshal(reader);
     * } finally {
     *     reader.close();
     * }
     * }</pre>
     *
     * @param cls The class for which to create the Unmarshaller (must be JAXB-annotated)
     * @return The created Unmarshaller
     * @throws IllegalArgumentException if {@code cls} is {@code null}
     * @throws RuntimeException if the Unmarshaller cannot be created
     * @see JAXBContext#newInstance(Class...)
     * @see JAXBContext#createUnmarshaller()
     */
    public static Unmarshaller createUnmarshaller(final Class<?> cls) throws IllegalArgumentException, RuntimeException {
        try {
            final JAXBContext jc = jaxbContext(cls);

            return jc.createUnmarshaller();
        } catch (final JAXBException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} with default configuration.
     * The parser is created from the shared, security-hardened {@code DocumentBuilderFactory}
     * (namespace-aware, with DTD and external-entity processing disabled to mitigate XXE).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DocumentBuilder parser = XmlUtil.createDOMParser();
     * Document doc = parser.parse(new File("data.xml"));
     * Element root = doc.getDocumentElement();
     * }</pre>
     *
     * @return A new instance of {@code DocumentBuilder}
     * @throws RuntimeException if the parser cannot be created
     * @see DocumentBuilderFactory#newDocumentBuilder()
     */
    public static DocumentBuilder createDOMParser() throws RuntimeException {
        synchronized (docBuilderFactory) {
            try {
                return docBuilderFactory.newDocumentBuilder();
            } catch (final ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} with the specified configuration.
     * This method allows control over comment and whitespace handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Ignore comments and request removal of ignorable element-content whitespace.
     * DocumentBuilder parser = XmlUtil.createDOMParser(true, true);
     * Document doc = parser.parse(xmlFile);
     * }</pre>
     *
     * <p>Whitespace removal applies only to whitespace identified as ignorable by an element-only
     * content model. This non-validating, DTD-disabled factory does not generally remove indentation
     * or other whitespace-only text nodes.</p>
     *
     * @param ignoreComments Whether to ignore comments in the XML
     * @param ignoringElementContentWhitespace Whether to request removal of ignorable element-content whitespace
     * @return A new instance of {@code DocumentBuilder} with the specified configuration
     * @throws RuntimeException if the parser cannot be created
     * @see DocumentBuilderFactory#newDocumentBuilder()
     */
    public static DocumentBuilder createDOMParser(final boolean ignoreComments, final boolean ignoringElementContentWhitespace) throws RuntimeException {
        DocumentBuilder documentBuilder = null;

        synchronized (docBuilderFactory) {
            final boolean orgIgnoreComments = docBuilderFactory.isIgnoringComments();
            final boolean orgIgnoringElementContentWhitespace = docBuilderFactory.isIgnoringElementContentWhitespace();

            try {
                docBuilderFactory.setIgnoringComments(ignoreComments);
                docBuilderFactory.setIgnoringElementContentWhitespace(ignoringElementContentWhitespace);

                documentBuilder = docBuilderFactory.newDocumentBuilder();
            } catch (final ParserConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            } finally {
                docBuilderFactory.setIgnoringComments(orgIgnoreComments);
                docBuilderFactory.setIgnoringElementContentWhitespace(orgIgnoringElementContentWhitespace);
            }
        }

        return documentBuilder;
    }

    /**
     * Creates a new instance of {@code DocumentBuilder} optimized for parsing content.
     * The parser ignores comments and requests removal of ignorable element-content whitespace.
     * As with {@link #createDOMParser(boolean, boolean)}, ordinary indentation is generally retained
     * because the factory is non-validating and disables DTDs.
     * This method uses object pooling for better performance.
     *
     * <p>Important: Call {@link #recycleContentParser(DocumentBuilder)} when done to return the parser to the pool.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DocumentBuilder parser = XmlUtil.createContentParser();
     * try {
     *     Document doc = parser.parse(xmlInputStream);
     *     // Process document
     * } finally {
     *     XmlUtil.recycleContentParser(parser);
     * }
     * }</pre>
     *
     * @return A {@code DocumentBuilder} instance from the pool or newly created
     * @throws RuntimeException if the parser cannot be created
     */
    public static DocumentBuilder createContentParser() throws RuntimeException {
        DocumentBuilder documentBuilder;

        synchronized (contentDocBuilderPool) {
            documentBuilder = contentDocBuilderPool.poll();

            if (documentBuilder != null) {
                pooledContentParsers.remove(documentBuilder);
            }
        }

        if (documentBuilder == null) {
            synchronized (docBuilderFactory) {
                final boolean orgIgnoreComments = docBuilderFactory.isIgnoringComments();
                final boolean orgIgnoringElementContentWhitespace = docBuilderFactory.isIgnoringElementContentWhitespace();

                try {
                    if (!orgIgnoreComments) {
                        docBuilderFactory.setIgnoringComments(true);
                    }

                    if (!orgIgnoringElementContentWhitespace) {
                        docBuilderFactory.setIgnoringElementContentWhitespace(true);
                    }

                    documentBuilder = docBuilderFactory.newDocumentBuilder();
                    ownedContentParsers.add(documentBuilder);
                } catch (final ParserConfigurationException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                } finally {
                    docBuilderFactory.setIgnoringComments(orgIgnoreComments);
                    docBuilderFactory.setIgnoringElementContentWhitespace(orgIgnoringElementContentWhitespace);
                }
            }
        }

        return documentBuilder;
    }

    /**
     * Recycles the given DocumentBuilder instance by resetting it and adding it back to the pool.
     * Only instances obtained from {@link #createContentParser()} are accepted; foreign and duplicate
     * instances are ignored so they cannot weaken or corrupt the security-hardened shared pool.
     * A parser is also discarded rather than pooled when the pool is already at capacity, or when the
     * provider's {@code reset()} throws; this method still returns normally in those cases. A caller
     * cannot tell whether the instance was pooled, so it must not use the parser again after calling
     * this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DocumentBuilder parser = XmlUtil.createContentParser();
     * try {
     *     // Use parser
     * } finally {
     *     XmlUtil.recycleContentParser(parser);
     * }
     * }</pre>
     *
     * @param docBuilder The DocumentBuilder instance to be recycled (can be null)
     */
    public static void recycleContentParser(final DocumentBuilder docBuilder) {
        if (docBuilder == null) {
            return;
        }

        synchronized (contentDocBuilderPool) {
            if (contentDocBuilderPool.size() >= POOL_SIZE || pooledContentParsers.containsKey(docBuilder) || !ownedContentParsers.contains(docBuilder)) {
                return;
            }

            try {
                docBuilder.reset();
                contentDocBuilderPool.add(docBuilder);
                pooledContentParsers.put(docBuilder, Boolean.TRUE);
            } catch (final RuntimeException e) {
                // A provider is permitted not to support reset. Discard that parser rather than
                // masking an earlier parsing failure from a caller's finally block.
                if (logger.isDebugEnabled()) {
                    logger.debug(e, "Discarding DocumentBuilder that could not be reset");
                }
            }
        }
    }

    /**
     * Creates a new instance of {@code SAXParser} from a pool or creates a new one if the pool is empty.
     * This method uses object pooling for better performance.
     *
     * <p>Important: Call {@link #recycleSAXParser(SAXParser)} when done to return the parser to the pool.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SAXParser parser = XmlUtil.createSAXParser();
     * try {
     *     parser.parse(xmlFile, myHandler);
     * } finally {
     *     XmlUtil.recycleSAXParser(parser);
     * }
     * }</pre>
     *
     * @return A {@code SAXParser} instance from the pool, or a newly created one if the pool is empty
     * @throws RuntimeException if the SAX parser configuration is invalid
     * @throws ParsingException if the underlying SAX implementation fails to create the parser
     * @see SAXParserFactory#newSAXParser()
     */
    public static SAXParser createSAXParser() throws RuntimeException, ParsingException {
        synchronized (saxParserPool) {
            SAXParser saxParser = saxParserPool.poll();

            if (saxParser != null) {
                pooledSaxParsers.remove(saxParser);
            }

            if (saxParser == null) {
                try {
                    saxParser = saxParserFactory.newSAXParser();
                    ownedSaxParsers.add(saxParser);
                } catch (final ParserConfigurationException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                } catch (final SAXException e) {
                    throw new ParsingException(e);
                }
            }

            return saxParser;
        }
    }

    /**
     * Recycles the given SAXParser instance by resetting it and adding it back to the pool.
     * Only instances obtained from {@link #createSAXParser()} are accepted; foreign and duplicate
     * instances are ignored so they cannot weaken or corrupt the security-hardened shared pool.
     * A parser is also discarded rather than pooled when the pool is already at capacity, or when the
     * provider's {@code reset()} throws; this method still returns normally in those cases. A caller
     * cannot tell whether the instance was pooled, so it must not use the parser again after calling
     * this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SAXParser parser = XmlUtil.createSAXParser();
     * try {
     *     // Use parser
     * } finally {
     *     XmlUtil.recycleSAXParser(parser);
     * }
     * }</pre>
     *
     * @param saxParser The SAXParser instance to be recycled (can be null)
     */
    public static void recycleSAXParser(final SAXParser saxParser) {
        if (saxParser == null) {
            return;
        }

        synchronized (saxParserPool) {
            if (saxParserPool.size() >= POOL_SIZE || pooledSaxParsers.containsKey(saxParser) || !ownedSaxParsers.contains(saxParser)) {
                return;
            }

            try {
                saxParser.reset();
                saxParserPool.add(saxParser);
                pooledSaxParsers.put(saxParser, Boolean.TRUE);
            } catch (final RuntimeException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e, "Discarding SAXParser that could not be reset");
                }
            }
        }
    }

    /**
     * Creates an XMLStreamReader from the given Reader source.
     * This is used for StAX (Streaming API for XML) parsing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringReader reader = new StringReader(xmlString);
     * XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(reader);
     * try {
     *     while (xmlReader.hasNext()) {
     *         int event = xmlReader.next();
     *         // Process events
     *     }
     * } finally {
     *     xmlReader.close();
     * }
     * }</pre>
     *
     * @param source The Reader source from which to create the XMLStreamReader
     * @return The created XMLStreamReader
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws RuntimeException if the StAX provider cannot initialize an XML reader over {@code source}
     * @see XMLInputFactory#createXMLStreamReader(Reader)
     */
    public static XMLStreamReader createXMLStreamReader(final Reader source) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(source, cs.source);

        synchronized (xmlInputFactory) {
            try {
                return xmlInputFactory.createXMLStreamReader(source);
            } catch (final XMLStreamException e) {
                throw toRuntimeException(e);
            }
        }
    }

    /**
     * Creates an XMLStreamReader from the given InputStream source.
     * This is used for StAX (Streaming API for XML) parsing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream source = new FileInputStream("data.xml")) {
     *     XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(source);
     *     try {
     *         while (xmlReader.hasNext()) {
     *             xmlReader.next();
     *         }
     *     } finally {
     *         xmlReader.close();
     *     }
     * }
     * }</pre>
     *
     * @param source The InputStream source from which to create the XMLStreamReader
     * @return The created XMLStreamReader
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws RuntimeException if the StAX provider cannot initialize an XML reader over {@code source}
     * @see XMLInputFactory#createXMLStreamReader(InputStream)
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(source, cs.source);

        synchronized (xmlInputFactory) {
            try {
                return xmlInputFactory.createXMLStreamReader(source);
            } catch (final XMLStreamException e) {
                throw toRuntimeException(e);
            }
        }
    }

    /**
     * Creates an XMLStreamReader from the given InputStream source with the specified encoding.
     * This is used for StAX (Streaming API for XML) parsing with explicit character encoding.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream source = new FileInputStream("data.xml")) {
     *     XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(source, "UTF-8");
     *     try {
     *         while (xmlReader.hasNext()) {
     *             xmlReader.next();
     *         }
     *     } finally {
     *         xmlReader.close();
     *     }
     * }
     * }</pre>
     *
     * @param source The InputStream source from which to create the XMLStreamReader
     * @param encoding The character encoding to be used (e.g., "UTF-8", "ISO-8859-1")
     * @return The created XMLStreamReader
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @throws RuntimeException if the StAX provider cannot initialize an XML reader over {@code source}
     * @see XMLInputFactory#createXMLStreamReader(InputStream, String)
     */
    public static XMLStreamReader createXMLStreamReader(final InputStream source, final String encoding) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(source, cs.source);

        synchronized (xmlInputFactory) {
            try {
                return xmlInputFactory.createXMLStreamReader(source, encoding);
            } catch (final XMLStreamException e) {
                throw toRuntimeException(e);
            }
        }
    }

    private static RuntimeException toRuntimeException(final XMLStreamException e) {
        // Identity-based cycle detection, matching ExceptionUtil's cause walks: a provider is free to return a
        // cause chain that loops (a custom getCause(), or initCause wiring done reflectively), and this walk runs
        // inside the factory monitor (xmlInputFactory or xmlOutputFactory), so a loop here froze every other
        // thread creating a reader or writer too.
        final Set<Throwable> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(e);

        for (Throwable cause = e.getCause(); cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return new UncheckedIOException((IOException) cause);
            }
        }

        final Throwable nested = e.getNestedException();

        if (nested instanceof IOException) {
            return new UncheckedIOException((IOException) nested);
        }

        return ExceptionUtil.toRuntimeException(e, true);
    }

    /**
     * Creates a filtered XMLStreamReader from the given source XMLStreamReader and StreamFilter.
     * The filter allows selective processing of XML events.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLStreamReader reader = XmlUtil.createXMLStreamReader(inputStream);
     * StreamFilter filter = new StreamFilter() {
     *     public boolean accept(XMLStreamReader reader) {
     *         return reader.isStartElement() || reader.isEndElement();
     *     }
     * };
     * XMLStreamReader filteredReader = XmlUtil.createFilteredStreamReader(reader, filter);
     * try {
     *     while (filteredReader.hasNext()) {
     *         filteredReader.next();
     *     }
     * } finally {
     *     filteredReader.close();
     * }
     * }</pre>
     *
     * @param source The source XMLStreamReader to be filtered
     * @param filter The StreamFilter to apply to the source. Must not be {@code null}.
     * @return The filtered XMLStreamReader
     * @throws IllegalArgumentException if {@code source} or {@code filter} is {@code null}
     * @throws RuntimeException if the StAX provider cannot create a filtered reader over {@code source} using {@code filter}
     * @see XMLInputFactory#createFilteredReader(XMLStreamReader, StreamFilter)
     */
    public static XMLStreamReader createFilteredStreamReader(final XMLStreamReader source, final StreamFilter filter)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(filter, cs.filter);

        synchronized (xmlInputFactory) {
            try {
                return xmlInputFactory.createFilteredReader(source, filter);
            } catch (final XMLStreamException e) {
                throw toRuntimeException(e);
            }
        }
    }

    /**
     * Creates an XMLStreamWriter from the given Writer output.
     * This is used for StAX (Streaming API for XML) writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(writer);
     * try {
     *     xmlWriter.writeStartDocument();
     *     xmlWriter.writeStartElement("root");
     *     xmlWriter.writeCharacters("Hello XML");
     *     xmlWriter.writeEndElement();
     *     xmlWriter.writeEndDocument();
     *     xmlWriter.flush();
     * } finally {
     *     xmlWriter.close();
     * }
     * }</pre>
     *
     * @param output The Writer output to which the XMLStreamWriter will write
     * @return The created XMLStreamWriter
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws RuntimeException if the StAX provider cannot initialize an XML writer for {@code output}
     * @see XMLOutputFactory#createXMLStreamWriter(Writer)
     */
    public static XMLStreamWriter createXMLStreamWriter(final Writer output) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(output, cs.output);

        synchronized (xmlOutputFactory) {
            try {
                return xmlOutputFactory.createXMLStreamWriter(output);
            } catch (final XMLStreamException e) {
                throw toRuntimeException(e);
            }
        }
    }

    /**
     * Creates an XMLStreamWriter from the given OutputStream.
     * This is used for StAX (Streaming API for XML) writing with default encoding.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream output = new FileOutputStream("output.xml")) {
     *     XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(output);
     *     try {
     *         xmlWriter.writeStartElement("root");
     *         xmlWriter.writeEndElement();
     *         xmlWriter.flush();
     *     } finally {
     *         xmlWriter.close();
     *     }
     * }
     * }</pre>
     *
     * @param output The OutputStream to which the XMLStreamWriter will write
     * @return The created XMLStreamWriter
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws RuntimeException if the StAX provider cannot initialize an XML writer for {@code output}
     * @see XMLOutputFactory#createXMLStreamWriter(OutputStream)
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(output, cs.output);

        synchronized (xmlOutputFactory) {
            try {
                return xmlOutputFactory.createXMLStreamWriter(output);
            } catch (final XMLStreamException e) {
                throw toRuntimeException(e);
            }
        }
    }

    /**
     * Creates an XMLStreamWriter from the given OutputStream with the specified encoding.
     * This is used for StAX (Streaming API for XML) writing with explicit character encoding.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream output = new FileOutputStream("output.xml")) {
     *     XMLStreamWriter xmlWriter = XmlUtil.createXMLStreamWriter(output, "UTF-8");
     *     try {
     *         xmlWriter.writeStartDocument("UTF-8", "1.0");
     *         xmlWriter.writeStartElement("root");
     *         xmlWriter.writeCharacters("value");
     *         xmlWriter.writeEndElement();
     *         xmlWriter.writeEndDocument();
     *         xmlWriter.flush();
     *     } finally {
     *         xmlWriter.close();
     *     }
     * }
     * }</pre>
     *
     * @param output The OutputStream to which the XMLStreamWriter will write
     * @param encoding The character encoding to be used (e.g., "UTF-8", "ISO-8859-1")
     * @return The created XMLStreamWriter
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws RuntimeException if the StAX provider cannot initialize an XML writer for {@code output}
     * @see XMLOutputFactory#createXMLStreamWriter(OutputStream, String)
     */
    public static XMLStreamWriter createXMLStreamWriter(final OutputStream output, final String encoding) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(output, cs.output);

        synchronized (xmlOutputFactory) {
            try {
                return xmlOutputFactory.createXMLStreamWriter(output, encoding);
            } catch (final XMLStreamException e) {
                throw toRuntimeException(e);
            }
        }
    }

    /**
     * Creates a new instance of Transformer for XML transformation operations.
     * The returned transformer performs an identity transformation and can serialize a DOM document.
     * This method does not compile or apply an XSLT stylesheet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Transformer transformer = XmlUtil.createXMLTransformer();
     * transformer.setOutputProperty(OutputKeys.INDENT, "yes");
     * transformer.transform(new DOMSource(document), new StreamResult(outputFile));
     * }</pre>
     *
     * @return A new instance of Transformer
     * @throws RuntimeException if a TransformerConfigurationException occurs
     * @see TransformerFactory#newTransformer()
     */
    public static Transformer createXMLTransformer() throws RuntimeException {
        // TransformerFactory instances are not guaranteed thread-safe for concurrent factory-method
        // calls (mirrors the synchronized(docBuilderFactory) / synchronized(saxParserPool) guards
        // used above for DocumentBuilderFactory/SAXParserFactory, for the same reason).
        synchronized (transformerFactory) {
            try {
                return transformerFactory.newTransformer();
            } catch (final TransformerConfigurationException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Transforms the given XML Document to the specified output file.
     * The file will be created if it doesn't exist, or overwritten if it does.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = parser.parse(inputFile);
     * // Modify document
     * XmlUtil.transform(doc, new File("output.xml"));
     * }</pre>
     *
     * <p>The file content is encoded by the transformer itself (UTF-8 by default), so the bytes
     * on disk match the encoding declared in the XML header.</p>
     *
     * <p>The output path is used exactly as supplied. In particular, a {@code %20} in the path is
     * <i>not</i> decoded to a space: this method always writes to {@code output} itself and never
     * redirects to a different, already-existing file.</p>
     *
     * @param source The XML Document to be transformed (must not be {@code null})
     * @param output The output file where the transformed XML will be written; used verbatim
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}; checked before opening the file
     * @throws UncheckedIOException if creating the output file or writing the transformed XML to it fails
     * @throws RuntimeException if a {@code TransformerException} occurs during transformation
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, final File output) throws IllegalArgumentException, UncheckedIOException, RuntimeException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

        // Do NOT run the output path through PropertiesUtil.formatPath: that helper is a *read*-side
        // heuristic which returns the "%20"-decoded sibling when that sibling happens to exist. On a
        // write path it silently retargets the transform at an unrelated pre-existing file (asking for
        // "a%20b.xml" would overwrite "a b.xml" and never create "a%20b.xml" at all).

        // Write through an OutputStream so the Transformer performs the character encoding
        // (UTF-8 by default, matching the encoding="UTF-8" it writes in the XML declaration).
        // A platform-default-charset FileWriter would produce bytes that contradict the
        // declaration for non-ASCII content on non-UTF-8 JVMs.
        OutputStream os = null;

        try {
            IOUtil.createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            transform(source, os);

            os.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            IOUtil.close(os);
        }
    }

    /**
     * Transforms the given XML Document to the specified OutputStream.
     * The stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = XmlUtil.createDOMParser().newDocument();
     * doc.appendChild(doc.createElement("root"));
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * XmlUtil.transform(doc, baos);
     * String xmlString = baos.toString("UTF-8");
     * }</pre>
     *
     * @param source The XML Document to be transformed (must not be {@code null})
     * @param output The OutputStream where the transformed XML will be written (not closed by this method)
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}
     * @throws RuntimeException if a {@code TransformerException} occurs
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, final OutputStream output) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

        // Prepare the DOM document for writing
        final Source domSource = new DOMSource(source);

        final Result result = new StreamResult(output);

        try {
            createXMLTransformer().transform(domSource, result);
        } catch (final TransformerException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Transforms the given XML Document to the specified Writer.
     * The writer is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = XmlUtil.createDOMParser().newDocument();
     * doc.appendChild(doc.createElement("root"));
     * StringWriter writer = new StringWriter();
     * XmlUtil.transform(doc, writer);
     * String xmlString = writer.toString();
     * }</pre>
     *
     * @param source The XML Document to be transformed (must not be {@code null})
     * @param output The Writer where the transformed XML will be written (not closed by this method)
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}
     * @throws RuntimeException if a {@code TransformerException} occurs
     * @see Transformer#transform(Source, Result)
     */
    public static void transform(final Document source, final Writer output) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

        // Prepare the DOM document for writing
        final Source domSource = new DOMSource(source);

        final Result result = new StreamResult(output);

        try {
            createXMLTransformer().transform(domSource, result);
        } catch (final TransformerException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Opt-in escape hatch for {@link #xmlEncode}/{@link #xmlDecode}. Off by default because
     * {@link java.beans.XMLDecoder} is a known deserialization gadget primitive (CVE-2017-3506,
     * CVE-2017-10271, etc.) — a malicious payload can encode arbitrary reflective method
     * invocations, including {@code Runtime.exec}. Set
     * {@code -Dabacus.xml.allowXmlEncoderDecoder=true} only if you trust both the encode-side
     * and decode-side inputs (e.g. a same-process round-trip).
     */
    private static final boolean ALLOW_XML_ENCODER_DECODER = Boolean.parseBoolean(System.getProperty("abacus.xml.allowXmlEncoderDecoder", "false"));

    /**
     * Encodes the given bean object into an XML string using Java's {@link XMLEncoder}.
     * This method is suitable for serializing JavaBeans with standard getter/setter patterns.
     *
     * <p>Note: This uses Java's built-in {@link XMLEncoder}, not JAXB. The output format is
     * specific to Java serialization and may not be suitable for interoperability with non-Java
     * systems.</p>
     *
     * <p><b>Disabled by default:</b> {@code xmlEncode}/{@link #xmlDecode(String)} are guarded by
     * the system property {@code abacus.xml.allowXmlEncoderDecoder} (defaults to {@code false}).
     * When the guard is off this method always throws {@link UnsupportedOperationException}, since
     * {@link java.beans.XMLDecoder} is a well-known unsafe-deserialization primitive
     * (CVE-2017-3506, CVE-2017-10271, ...). Migrate to JAXB or the abacus XML parser instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Start the JVM with -Dabacus.xml.allowXmlEncoderDecoder=true.
     * // Use this legacy format only for trusted, same-application data.
     * Person person = new Person("John", 30);
     * String xml = XmlUtil.xmlEncode(person);
     * // Result contains Java-specific XML encoding
     * }</pre>
     *
     * @param bean The object to be encoded into XML
     * @return The XML string representation of the given object
     * @throws UnsupportedOperationException if {@code abacus.xml.allowXmlEncoderDecoder} is not set to {@code true}
     * @see XMLEncoder#writeObject(Object)
     * @deprecated unsafe deserialization primitive; disabled by default. Use JAXB or {@code XmlMappers}.
     */
    @Deprecated
    public static String xmlEncode(final Object bean) throws UnsupportedOperationException {
        if (!ALLOW_XML_ENCODER_DECODER) {
            throw new UnsupportedOperationException("xmlEncode/xmlDecode are disabled by default because "
                    + "java.beans.XMLDecoder is an unsafe-deserialization primitive (CVE-2017-3506 etc). "
                    + "Set -Dabacus.xml.allowXmlEncoderDecoder=true to opt in for trusted round-trips, or migrate to JAXB / abacus XmlParser.");
        }
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();

        try {
            try (XMLEncoder xmlEncoder = new XMLEncoder(os)) {
                xmlEncoder.writeObject(bean);
                xmlEncoder.flush();
            }

            // XMLEncoder always writes UTF-8 (its declaration claims UTF-8), and xmlDecode reads the
            // string back via getBytes(UTF_8) — decode with UTF-8 so non-ASCII content round-trips
            // on JVMs whose default charset is not UTF-8.
            return os.toString(Charsets.UTF_8);
        } finally {
            Objectory.recycle(os);
        }
    }

    /**
     * Decodes the given XML string into an object using Java's {@link XMLDecoder}.
     * This method is the counterpart to {@link #xmlEncode(Object)} and should be used
     * to decode XML created by {@link XMLEncoder}.
     *
     * <p>Note: This uses Java's built-in {@link XMLDecoder}, not JAXB. The XML format must be
     * compatible with Java's {@link XMLEncoder} output.</p>
     *
     * <p><b>Disabled by default:</b> {@link #xmlEncode(Object)}/{@code xmlDecode} are guarded by
     * the system property {@code abacus.xml.allowXmlEncoderDecoder} (defaults to {@code false}).
     * When the guard is off this method always throws {@link UnsupportedOperationException}, since
     * {@link java.beans.XMLDecoder} is a well-known unsafe-deserialization primitive
     * (CVE-2017-3506, CVE-2017-10271, ...). Migrate to JAXB or the abacus XML parser instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Start the JVM with -Dabacus.xml.allowXmlEncoderDecoder=true, and decode only trusted XML.
     * String xml = XmlUtil.xmlEncode(originalPerson);
     * Person decodedPerson = XmlUtil.xmlDecode(xml);
     * }</pre>
     *
     * @param <T> The type of the object to be returned
     * @param xml The XML string to be decoded
     * @return The decoded object
     * @throws UnsupportedOperationException if {@code abacus.xml.allowXmlEncoderDecoder} is not set to {@code true}
     * @throws IllegalArgumentException if XML decoding is enabled and {@code xml} is {@code null}
     * @see XMLDecoder#readObject()
     * @deprecated unsafe deserialization primitive; disabled by default. Use JAXB or {@code XmlMappers}.
     */
    @Deprecated
    public static <T> T xmlDecode(final String xml) throws UnsupportedOperationException, IllegalArgumentException {
        if (!ALLOW_XML_ENCODER_DECODER) {
            throw new UnsupportedOperationException("xmlEncode/xmlDecode are disabled by default because "
                    + "java.beans.XMLDecoder is an unsafe-deserialization primitive (CVE-2017-3506 etc). "
                    + "Set -Dabacus.xml.allowXmlEncoderDecoder=true to opt in for trusted round-trips, or migrate to JAXB / abacus XmlParser.");
        }
        N.checkArgNotNull(xml, cs.xml);

        // The InputStream wraps a byte array, so closing it has no effect; we still close
        // the XMLDecoder which drains references.
        try (XMLDecoder xmlDecoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8)))) {
            return (T) xmlDecoder.readObject();
        }
    }

    /**
     * Gets all direct child elements with the specified tag name from the given parent element.
     * This method only returns elements that are immediate children of the parent node,
     * not descendants at deeper levels.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element parent = doc.getDocumentElement();
     * List<Element> childElements = XmlUtil.getElementsByTagName(parent, "child");
     * for (Element child : childElements) {
     *     System.out.println(child.getTextContent());
     * }
     * }</pre>
     *
     * <p>Matching is performed on the qualified tag name (the same semantics as
     * {@link Element#getElementsByTagName(String)}); namespace URIs are not considered. As in the
     * DOM API, the special value {@code "*"} matches every direct child element.</p>
     *
     * @param node The parent element to search within
     * @param tagName The tag name of the elements to find, or {@code "*"} to match every direct child element
     * @return A list of elements with the specified tag name that are direct children of the given
     *         node; an empty list if there is no match (never {@code null})
     * @throws IllegalArgumentException if {@code node} is {@code null}
     * @see Element#getElementsByTagName(String)
     * @see #getNodesByName(Node, String)
     */
    public static List<Element> getElementsByTagName(final Element node, final String tagName) throws IllegalArgumentException {
        N.checkArgNotNull(node, cs.node);

        final List<Element> result = new ArrayList<>();
        final NodeList nodeList = node.getChildNodes();
        final boolean matchAll = "*".equals(tagName);

        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node child = nodeList.item(i);

            if (child instanceof Element element && (matchAll || element.getTagName().equals(tagName))) {
                result.add(element);
            }
        }

        return result;
    }

    /**
     * Gets all nodes with the specified name from the given node and its descendants.
     * This method searches the entire node tree in depth-first preorder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = parser.parse(xmlFile);
     * List<Node> allPriceNodes = XmlUtil.getNodesByName(doc, "price");
     * }</pre>
     *
     * <p>The search includes {@code node} itself: if its node name equals {@code nodeName} it is
     * included in the result.</p>
     *
     * <p>Traversal uses an explicit work stack and does not impose a recursive call-depth limit.</p>
     *
     * @param node The parent node to search within
     * @param nodeName The name of the nodes to find
     * @return A list of all nodes with the specified name; an empty list if there is no match
     *         (never {@code null})
     * @throws IllegalArgumentException if {@code node} is {@code null}
     * @see #getNextNodeByName(Node, String)
     * @see #getElementsByTagName(Element, String)
     */
    public static List<Node> getNodesByName(final Node node, final String nodeName) throws IllegalArgumentException {
        N.checkArgNotNull(node, cs.node);

        final List<Node> nodes = new ArrayList<>();

        getNodesByName(node, nodeName, nodes);

        return nodes;
    }

    private static void getNodesByName(final Node node, final String nodeName, final List<Node> output) {
        final var pending = new java.util.ArrayDeque<Node>();
        pending.push(node);
        // Reverse push order preserves the original preorder without using one call frame per DOM level.
        while (!pending.isEmpty()) {
            final Node current = pending.pop();
            if (current.getNodeName().equals(nodeName)) {
                output.add(current);
            }
            final NodeList children = current.getChildNodes();
            for (int i = children.getLength() - 1; i >= 0; i--) {
                pending.push(children.item(i));
            }
        }
    }

    /**
     * Gets the first node with the specified name from the given node or its descendants.
     * The given node itself is checked first, then its direct children, and finally its
     * deeper descendants; the first matching node found is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = parser.parse(xmlFile);
     * Node firstPriceNode = XmlUtil.getNextNodeByName(doc, "price");
     * if (firstPriceNode != null) {
     *     System.out.println("Price: " + firstPriceNode.getTextContent());
     * }
     * }</pre>
     *
     * <p>Traversal uses an explicit work stack and does not impose a recursive call-depth limit.</p>
     *
     * @param node The parent node to search within
     * @param nodeName The name of the node to find
     * @return The first node with the specified name, or {@code null} if no such node is found
     * @throws IllegalArgumentException if {@code node} is {@code null}
     */
    @MayReturnNull
    public static Node getNextNodeByName(final Node node, final String nodeName) throws IllegalArgumentException {
        N.checkArgNotNull(node, cs.node);

        final var pending = new java.util.ArrayDeque<Node>();
        pending.push(node);
        while (!pending.isEmpty()) {
            final Node current = pending.pop();
            if (current.getNodeName().equals(nodeName)) {
                return current;
            }
            final NodeList children = current.getChildNodes();
            // Retain this method's special order: direct children precede descendants of any child.
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                if (child.getNodeName().equals(nodeName)) {
                    return child;
                }
            }
            for (int i = children.getLength() - 1; i >= 0; i--) {
                pending.push(children.item(i));
            }
        }

        return null;
    }

    /**
     * Gets the attribute value of the specified attribute name from the given XML node.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element element = doc.getElementById("myElement");
     * String id = XmlUtil.getAttribute(element, "id");
     * String className = XmlUtil.getAttribute(element, "class");
     * }</pre>
     *
     * @param node The XML node from which to get the attribute
     * @param attrName The name of the attribute to retrieve
     * @return The value of the specified attribute, or {@code null} if the attribute does not exist
     * @throws IllegalArgumentException if {@code node} or {@code attrName} is {@code null}
     */
    @MayReturnNull
    public static String getAttribute(final Node node, final String attrName) throws IllegalArgumentException {
        N.checkArgNotNull(node, cs.node);
        N.checkArgNotNull(attrName, cs.attrName);

        final NamedNodeMap attrsNode = node.getAttributes();

        if (attrsNode == null) {
            return null;
        }

        final Node attrNode = attrsNode.getNamedItem(attrName);

        return (attrNode == null) ? null : attrNode.getNodeValue();
    }

    /**
     * Reads all attributes of the given XML node and returns them as a map.
     * The map keys are attribute names and values are attribute values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element element = doc.getElementById("myElement");
     * Map<String, String> attrs = XmlUtil.readAttributes(element);
     * for (Map.Entry<String, String> entry : attrs.entrySet()) {
     *     System.out.println(entry.getKey() + "=" + entry.getValue());
     * }
     * }</pre>
     *
     * @param node The XML node from which to read the attributes
     * @return A map containing the attributes of the given node, where keys are attribute names and values are attribute values
     * @throws IllegalArgumentException if {@code node} is {@code null}
     */
    public static Map<String, String> readAttributes(final Node node) throws IllegalArgumentException {
        N.checkArgNotNull(node, cs.node);

        return readAttributes(Strings.EMPTY, node, new LinkedHashMap<>());
    }

    private static Map<String, String> readAttributes(final String parentNodeName, final Node node, final Map<String, String> output) {
        final NamedNodeMap attrNodes = node.getAttributes();

        if (attrNodes == null || attrNodes.getLength() == 0) {
            return output;
        }

        final boolean isEmptyParentNodeName = Strings.isEmpty(parentNodeName);

        for (int i = 0; i < attrNodes.getLength(); i++) {
            final String attrName = attrNodes.item(i).getNodeName();
            final String attrValue = attrNodes.item(i).getNodeValue();

            if (isEmptyParentNodeName) {
                output.put(attrName, attrValue);
            } else {
                output.put(parentNodeName + "." + attrName, attrValue);
            }
        }

        return output;
    }

    /**
     * Reads the given XML element and returns its attributes and text content as a map.
     * This method processes the element and all its descendant elements,
     * creating a flattened map with dot-notation keys for nested elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element root = doc.getDocumentElement();
     * Map<String, String> data = XmlUtil.readElement(root);
     * // For XML: <person age="30"><name>John</name></person>
     * // Result: {"age"="30", "person.name"="John"}
     * }</pre>
     *
     * <p>The flattened representation can hold only one value per element path. If siblings use
     * the same name, later values overwrite earlier values only for keys they share; keys emitted
     * only by an earlier sibling remain in the map. Text is recorded only for elements without
     * child elements, and leading and trailing whitespace is stripped. Traverse the DOM directly
     * when repeated-element multiplicity or mixed content must be preserved. Root
     * attributes retain unqualified keys for compatibility; nested attributes use the complete
     * element path.</p>
     *
     * <p>Traversal uses an explicit work stack and does not impose a recursive call-depth limit.</p>
     *
     * @param element The XML element to be read
     * @return A map containing the attributes and text content of the given element and its descendants
     * @throws IllegalArgumentException if {@code element} is {@code null}
     */
    public static Map<String, String> readElement(final Element element) throws IllegalArgumentException {
        N.checkArgNotNull(element, cs.element);

        return readElement(Strings.EMPTY, element, new LinkedHashMap<>());
    }

    private static Map<String, String> readElement(final String parentNodeName, final Element element, final Map<String, String> output) {
        final var pending = new java.util.ArrayDeque<Map.Entry<Element, String>>();
        pending.push(Map.entry(element, parentNodeName));
        while (!pending.isEmpty()) {
            final var frame = pending.pop();
            final Element current = frame.getKey();
            final boolean root = Strings.isEmpty(frame.getValue());
            final String elementPath = root ? current.getNodeName() : frame.getValue() + "." + current.getNodeName();
            // Preserve unqualified root attributes and fully qualified descendant attributes.
            readAttributes(root ? Strings.EMPTY : elementPath, current, output);
            if (isTextElement(current)) {
                output.put(elementPath, Strings.strip(getTextContent(current)));
            }
            final NodeList children = current.getChildNodes();
            // Preorder also preserves insertion order and the last-value-wins policy for repeated paths.
            for (int i = children.getLength() - 1; i >= 0; i--) {
                if (children.item(i) instanceof Element child) {
                    pending.push(Map.entry(child, elementPath));
                }
            }
        }

        return output;
    }

    /**
     * Checks whether the given node is non-null and has no direct child elements.
     * Despite the method name, the node itself need not be an {@link Element}: text, comment,
     * and other non-element nodes also qualify when they have no element children.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element elem = doc.createElement("name");
     * elem.setTextContent("John");
     * boolean isText = XmlUtil.isTextElement(elem);   // returns true
     *
     * Element parent = doc.createElement("person");
     * parent.appendChild(elem);
     * boolean isParentText = XmlUtil.isTextElement(parent);   // returns false
     * }</pre>
     *
     * @param node The node to be checked; may be {@code null}
     * @return {@code true} if the node is non-null and has no direct child elements, {@code false} otherwise
     */
    public static boolean isTextElement(final Node node) {
        if (node == null) {
            return false;
        }

        final NodeList childNodeList = node.getChildNodes();

        for (int i = 0; i < childNodeList.getLength(); i++) {
            if (childNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the DOM text content of the specified node.
     *
     * <p>This is a null-safe wrapper around {@link Node#getTextContent()}. If the node is not
     * {@code null}, the returned value contains the concatenated text of the node and its descendants
     * exactly as reported by the DOM implementation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element elem = doc.createElement("message");
     * elem.setTextContent("  Hello World  ");
     *
     * String content = XmlUtil.getTextContent(elem);   // returns "  Hello World  "
     * String missing = XmlUtil.getTextContent(null);   // returns null
     * }</pre>
     *
     * @param node the XML node to read text from
     * @return the value of {@code node.getTextContent()}; {@code null} if {@code node} is {@code null}, and also
     *         {@code null} for node types whose DOM text content is undefined (document, document-type and
     *         notation nodes)
     */
    @MayReturnNull
    public static String getTextContent(final Node node) {
        return node == null ? null : node.getTextContent();
    }

    /**
     * Returns the text content of the specified node, optionally normalizing selected whitespace
     * control characters.
     *
     * <p>If {@code ignoreWhiteChar} is {@code false}, this method behaves the same as
     * {@link #getTextContent(Node)}. If it is {@code true}, the method first reads
     * {@link Node#getTextContent()} and then replaces tab, backspace, newline, carriage return,
     * and form-feed characters with single spaces, suppresses repeated inserted spaces, and trims
     * leading and trailing spaces from the final result. Regular space characters that are already
     * present in the text are otherwise preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Element elem = doc.createElement("message");
     * elem.setTextContent("  Hello\n\tWorld  ");
     *
     * String normalized = XmlUtil.getTextContent(elem, true);   // returns "Hello World"
     * String raw = XmlUtil.getTextContent(elem, false);         // returns "  Hello\n\tWorld  "
     * String missing = XmlUtil.getTextContent(null, true);      // returns null
     * }</pre>
     *
     * @param node the XML node to read text from
     * @param ignoreWhiteChar {@code true} to normalize tab/backspace/newline/carriage-return/form-feed
     *        characters and trim outer spaces; {@code false} to return the raw DOM text content
     * @return the processed text content; {@code null} if {@code node} is {@code null}, and also {@code null}
     *         when the DOM reports no text content for the node type (document, document-type and notation nodes)
     */
    @MayReturnNull
    public static String getTextContent(final Node node, final boolean ignoreWhiteChar) {
        if (node == null) {
            return null;
        }

        String textContent = node.getTextContent();

        if (ignoreWhiteChar && Strings.isNotEmpty(textContent)) {
            final StringBuilder sb = Objectory.createStringBuilder();

            try {
                for (final char c : textContent.toCharArray()) {
                    switch (c) {
                        case '\t':
                        case '\b':
                        case '\n':
                        case '\r':
                        case '\f':

                            if ((!sb.isEmpty()) && (sb.charAt(sb.length() - 1) != ' ')) {
                                sb.append(' ');
                            }

                            break;

                        default:
                            sb.append(c);
                    }
                }

                final int length = sb.length();

                if ((length > 0) && ((sb.charAt(0) == ' ') || (sb.charAt(length - 1) == ' '))) {
                    int from = 0;

                    do {
                        if (sb.charAt(from) != ' ') {
                            break;
                        }

                        from++;
                    } while (from < length);

                    int to = length - 1;

                    do {
                        if (sb.charAt(to) != ' ') {
                            break;
                        }

                        to--;
                    } while (to >= 0);

                    if (from <= to) {
                        textContent = sb.substring(from, to + 1);
                    } else {
                        textContent = "";
                    }
                } else {
                    textContent = sb.toString();
                }
            } finally {
                // Matches the pooled-handle shape of marshal/xmlEncode in this file: the builder returns to the
                // pool even if the body ever gains a throwing call. The result String is already materialised.
                Objectory.recycle(sb);
            }
        }

        return textContent;
    }

    /**
     * Writes XML-escaped characters from the specified character array to the given StringBuilder.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Hello <world> & \"friends\"".toCharArray();
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters(chars, sb);
     * // Result: "Hello &lt;world&gt; &amp; &quot;friends&quot;"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws UncheckedIOException if writing the escaped characters fails
     */
    public static void writeCharacters(final char[] cbuf, final StringBuilder output)
            throws NullPointerException, IllegalArgumentException, UncheckedIOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes XML-escaped characters from a portion of a character array to the given StringBuilder.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Data: <value> & 'text'".toCharArray();
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters(chars, 6, 14, sb);
     * // Result: "&lt;value&gt; &amp; &apos;tex"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param off The start offset in the character array
     * @param len The number of characters to write
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or the requested range exceeds {@code cbuf.length}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws UncheckedIOException if writing the escaped characters fails
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final StringBuilder output)
            throws NullPointerException, IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        Objects.checkFromIndexSize(off, len, cbuf.length);

        // A StringBuilder-backed writer cannot fail; the checked exception is unreachable here.
        try {
            writeCharacters(cbuf, off, len, IOUtil.newStringWriter(output));
        } catch (final IOException e) {
            throw new UncheckedIOException(e); //NOSONAR
        }
    }

    /**
     * Writes XML-escaped characters from the specified string to the given StringBuilder.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * If the string is {@code null}, the text "null" is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters("<tag attr='value'>text & more</tag>", sb);
     * // Result: "&lt;tag attr=&apos;value&apos;&gt;text &amp; more&lt;/tag&gt;"
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws UncheckedIOException if writing the escaped characters fails
     */
    public static void writeCharacters(String str, final StringBuilder output) throws IllegalArgumentException, UncheckedIOException {
        str = (str == null) ? Strings.NULL : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes XML-escaped characters from a portion of a string to the given StringBuilder.
     * If the string is {@code null}, the offset and length select a slice of the literal {@code "null"}.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Hello <world> & friends";
     * StringBuilder sb = new StringBuilder();
     * XmlUtil.writeCharacters(text, 6, 8, sb);
     * // Result: "&lt;world&gt; "
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param off The start offset in the string
     * @param len The number of characters to write
     * @param output The StringBuilder to which the escaped characters will be written
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or the requested range exceeds the length of
     *         {@code str} (or the literal {@code "null"} when {@code str} is {@code null})
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws UncheckedIOException if writing the escaped characters fails
     */
    public static void writeCharacters(final String str, final int off, final int len, final StringBuilder output)
            throws IndexOutOfBoundsException, IllegalArgumentException, UncheckedIOException {
        Objects.checkFromIndexSize(off, len, str == null ? Strings.NULL.length() : str.length());

        // A StringBuilder-backed writer cannot fail; the checked exception is unreachable here.
        try {
            writeCharacters(str, off, len, IOUtil.newStringWriter(output));
        } catch (final IOException e) {
            throw new UncheckedIOException(e); //NOSONAR
        }
    }

    /**
     * Writes XML-escaped characters from the specified character array to the given OutputStream.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "<data>value & more</data>".toCharArray();
     * ByteArrayOutputStream baos = new ByteArrayOutputStream();
     * XmlUtil.writeCharacters(chars, baos);
     * // Result: "&lt;data&gt;value &amp; more&lt;/data&gt;"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param output The OutputStream to receive UTF-8 encoded escaped characters; flushed but not closed
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws IOException if writing the escaped characters to {@code output} or flushing {@code output} fails
     */
    public static void writeCharacters(final char[] cbuf, final OutputStream output) throws NullPointerException, IllegalArgumentException, IOException {
        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes XML-escaped characters from a portion of a character array to the given OutputStream.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * Control characters U+0000 through U+001F and U+007F are written as numeric character
     * references such as <code>&amp;#x1f;</code>. References do not make characters prohibited by
     * the selected XML version legal; callers must provide text valid for that version.
     * Uses a BufferedXmlWriter internally for efficient writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Data: <value> & 'text'".toCharArray();
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XmlUtil.writeCharacters(chars, 7, 14, fos);
     * // Writes: "value&gt; &amp; &apos;text"
     * fos.close();
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param off The start offset in the character array
     * @param len The number of characters to write
     * @param output The OutputStream to receive UTF-8 encoded escaped characters; flushed but not closed
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or the requested range exceeds {@code cbuf.length}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws IOException if writing the escaped characters to {@code output} or flushing {@code output} fails
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final OutputStream output)
            throws NullPointerException, IndexOutOfBoundsException, IllegalArgumentException, IOException {
        Objects.checkFromIndexSize(off, len, cbuf.length);

        final BufferedXmlWriter bufWriter = Objectory.createBufferedXmlWriter(output); //NOSONAR

        bufWriter.writeCharacter(cbuf, off, len);
        bufWriter.flush();
        // Recycling flushes pending output. After a failure, discard this wrapper instead of
        // retrying a possibly partial write and replacing the original exception.
        Objectory.recycle(bufWriter);
    }

    /**
     * Writes XML-escaped characters from the specified string to the given OutputStream.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * If the string is {@code null}, the text "null" is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String data = "<message>Hello & goodbye</message>";
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XmlUtil.writeCharacters(data, fos);
     * // Writes: "&lt;message&gt;Hello &amp; goodbye&lt;/message&gt;"
     * fos.close();
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param output The OutputStream to receive UTF-8 encoded escaped characters; flushed but not closed
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws IOException if writing the escaped characters to {@code output} or flushing {@code output} fails
     */
    public static void writeCharacters(String str, final OutputStream output) throws IllegalArgumentException, IOException {
        str = (str == null) ? Strings.NULL : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes XML-escaped characters from a portion of a string to the given OutputStream.
     * If the string is {@code null}, the offset and length select a slice of the literal {@code "null"}.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * Control characters U+0000 through U+001F and U+007F are written as numeric character
     * references such as <code>&amp;#x1f;</code>. References do not make characters prohibited by
     * the selected XML version legal; callers must provide text valid for that version.
     * Uses a BufferedXmlWriter internally for efficient writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Prefix <tag>content</tag> suffix";
     * FileOutputStream fos = new FileOutputStream("output.xml");
     * XmlUtil.writeCharacters(text, 7, 17, fos);
     * // Writes: "&lt;tag&gt;content&lt;/tag"
     * fos.close();
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param off The start offset in the string
     * @param len The number of characters to write
     * @param output The OutputStream to receive UTF-8 encoded escaped characters; flushed but not closed
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or the requested range exceeds the length of
     *         {@code str} (or the literal {@code "null"} when {@code str} is {@code null})
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws IOException if writing the escaped characters to {@code output} or flushing {@code output} fails
     */
    public static void writeCharacters(final String str, final int off, final int len, final OutputStream output)
            throws IndexOutOfBoundsException, IllegalArgumentException, IOException {
        Objects.checkFromIndexSize(off, len, str == null ? Strings.NULL.length() : str.length());

        final BufferedXmlWriter bufWriter = Objectory.createBufferedXmlWriter(output); //NOSONAR

        bufWriter.writeCharacter(str, off, len);
        bufWriter.flush();
        // Recycling can flush again, so only return a wrapper whose write and flush succeeded.
        Objectory.recycle(bufWriter);
    }

    /**
     * Writes XML-escaped characters from the specified character array to the given Writer.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Test: <value> & 'more'".toCharArray();
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters(chars, writer);
     * String result = writer.toString();
     * // Result: "Test: &lt;value&gt; &amp; &apos;more&apos;"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param output The Writer to receive escaped characters; flushed but not closed
     * @throws IOException if {@code output} is a closed {@code BufferedXmlWriter}, or writing or flushing {@code output} fails
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void writeCharacters(final char[] cbuf, final Writer output) throws IOException, NullPointerException, IllegalArgumentException {
        if (output instanceof BufferedXmlWriter) {
            ((BufferedXmlWriter) output).ensureOpen();
        }

        writeCharacters(cbuf, 0, cbuf.length, output);
    }

    /**
     * Writes XML-escaped characters from a portion of a character array to the given Writer.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * Control characters U+0000 through U+001F and U+007F are written as numeric character
     * references such as <code>&amp;#x1f;</code>. References do not make characters prohibited by
     * the selected XML version legal; callers must provide text valid for that version.
     * Uses a BufferedXmlWriter for efficient writing if the output is not already a BufferedXmlWriter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Data: <tag>value</tag> end".toCharArray();
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters(chars, 6, 16, writer);
     * // Result: "&lt;tag&gt;value&lt;/tag&gt;"
     * }</pre>
     *
     * @param cbuf The character array containing the characters to be written
     * @param off The start offset in the character array
     * @param len The number of characters to write
     * @param output The Writer to receive escaped characters; flushed but not closed
     * @throws IOException if {@code output} is a closed {@code BufferedXmlWriter}, or writing or flushing {@code output} fails
     * @throws NullPointerException if {@code cbuf} is {@code null}
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or the requested range exceeds {@code cbuf.length}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void writeCharacters(final char[] cbuf, final int off, final int len, final Writer output)
            throws IOException, NullPointerException, IndexOutOfBoundsException, IllegalArgumentException {
        if (output instanceof BufferedXmlWriter) {
            ((BufferedXmlWriter) output).ensureOpen();
        }

        Objects.checkFromIndexSize(off, len, cbuf.length);

        final boolean isBufferedWriter = output instanceof BufferedXmlWriter;
        final BufferedXmlWriter bw = isBufferedWriter ? (BufferedXmlWriter) output : Objectory.createBufferedXmlWriter(output); //NOSONAR

        bw.writeCharacter(cbuf, off, len);
        bw.flush();
        // Do not retry failed output through the recycling flush, or recycle a caller-owned writer.
        if (!isBufferedWriter) {
            Objectory.recycle(bw);
        }
    }

    /**
     * Writes XML-escaped characters from the specified string to the given Writer.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * If the string is {@code null}, the text "null" is written.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<book title='Java & XML'>Content</book>";
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters(xml, writer);
     * String result = writer.toString();
     * // Result: "&lt;book title=&apos;Java &amp; XML&apos;&gt;Content&lt;/book&gt;"
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param output The Writer to receive escaped characters; flushed but not closed
     * @throws IOException if {@code output} is a closed {@code BufferedXmlWriter}, or writing or flushing {@code output} fails
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void writeCharacters(String str, final Writer output) throws IOException, IllegalArgumentException {
        if (output instanceof BufferedXmlWriter) {
            ((BufferedXmlWriter) output).ensureOpen();
        }

        str = (str == null) ? Strings.NULL : str;
        writeCharacters(str, 0, str.length(), output);
    }

    /**
     * Writes XML-escaped characters from a portion of a string to the given Writer.
     * If the string is {@code null}, the offset and length select a slice of the literal {@code "null"}.
     * Special XML characters (&lt;, &gt;, &amp;, ', ") are escaped to their XML entity representations.
     * Control characters U+0000 through U+001F and U+007F are written as numeric character
     * references such as <code>&amp;#x1f;</code>. References do not make characters prohibited by
     * the selected XML version legal; callers must provide text valid for that version.
     * Uses a BufferedXmlWriter for efficient writing if the output is not already a BufferedXmlWriter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringWriter writer = new StringWriter();
     * XmlUtil.writeCharacters("Hello <world>", 0, 13, writer);
     * String result = writer.toString();   // returns "Hello &lt;world&gt;"
     * }</pre>
     *
     * @param str The string containing the characters to be written
     * @param off The start offset in the string
     * @param len The number of characters to write
     * @param output The Writer to receive escaped characters; flushed but not closed
     * @throws IOException if {@code output} is a closed {@code BufferedXmlWriter}, or writing or flushing {@code output} fails
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative, or the requested range exceeds the length of
     *         {@code str} (or the literal {@code "null"} when {@code str} is {@code null})
     * @throws IllegalArgumentException if {@code output} is {@code null}
     */
    public static void writeCharacters(final String str, final int off, final int len, final Writer output)
            throws IOException, IndexOutOfBoundsException, IllegalArgumentException {
        if (output instanceof BufferedXmlWriter) {
            ((BufferedXmlWriter) output).ensureOpen();
        }

        Objects.checkFromIndexSize(off, len, str == null ? Strings.NULL.length() : str.length());

        final boolean isBufferedWriter = output instanceof BufferedXmlWriter;
        final BufferedXmlWriter bw = isBufferedWriter ? (BufferedXmlWriter) output : Objectory.createBufferedXmlWriter(output); //NOSONAR

        bw.writeCharacter(str, off, len);
        bw.flush();
        // Do not retry failed output through the recycling flush, or recycle a caller-owned writer.
        if (!isBufferedWriter) {
            Objectory.recycle(bw);
        }
    }

    /**
     * Opt-in escape hatch for the legacy "fall back to Class.forName for unknown type attributes"
     * behavior. Off by default since attacker-controlled type= attributes feed directly into
     * reflective construction (gadget-chain primitive — see CVE histories for similar Jackson /
     * XStream issues). Enable with {@code -Dabacus.xml.allowTypeAttrClassForName=true} ONLY when
     * deserializing trusted XML.
     */
    private static final String XML_TYPE_CLASS_FOR_NAME_PROPERTY = "abacus.xml.allowTypeAttrClassForName";

    /**
     * Type names that may be resolved from untrusted XML without unrestricted name-driven class loading.
     * Arrays and generic type expressions are accepted only when their component and nested
     * type names also satisfy this allowlist.
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

    /**
     * Returns whether {@code typeName} would be accepted by {@link #getAttributeType(Node)} under the
     * currently effective policy, i.e. either it is on the built-in allowlist or the legacy
     * {@code abacus.xml.allowTypeAttrClassForName} opt-in is enabled.
     *
     * <p>Writers use this to make sure they never emit a {@code type} attribute that the reader would
     * later refuse, which would otherwise produce XML this library cannot load back. Surrounding
     * whitespace is ignored, because the reader also trims the attribute value before resolving it.</p>
     *
     * @param typeName the candidate {@code type} attribute value
     * @return {@code true} if this name passes the current type policy; actual resolution may still fail
     */
    static boolean isResolvableXmlTypeAttributeName(final String typeName) {
        if (Strings.isEmpty(typeName)) {
            return false;
        }

        // Normalise exactly as getAttributeType does before deciding: untrimmed whitespace otherwise defeats
        // isSafeXmlTypeAttributeName's "[]" suffix stripping, so the two sides answer differently.
        final String normalized = typeName.trim();

        return !normalized.isEmpty() && (Boolean.getBoolean(XML_TYPE_CLASS_FOR_NAME_PROPERTY) || isSafeXmlTypeAttributeName(normalized));
    }

    /**
     * Returns whether the scalar/container name and every nested generic component, after removing
     * array suffixes, are safe to pass to the type registry without the legacy opt-in.
     */
    private static boolean isSafeXmlTypeAttributeName(String typeName) {
        while (typeName.endsWith("[]")) {
            typeName = typeName.substring(0, typeName.length() - 2);
        }

        try {
            final TypeAttrParser typeAttr = TypeAttrParser.parse(typeName);
            final String className = typeAttr.getClassName();

            if (!SAFE_XML_TYPE_ATTRIBUTE_NAMES.contains(className)) {
                return false;
            }

            for (final String typeParameter : typeAttr.getTypeParameters()) {
                if (!isSafeXmlTypeAttributeName(typeParameter)) {
                    return false;
                }
            }

            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Resolves the Java class indicated by the {@code type} attribute without permitting arbitrary
     * class loading by default. Only exact names in the built-in scalar/container allowlist (and
     * arrays and generic expressions composed of those types) are accepted. Trusted legacy XML may
     * restore unrestricted resolution by setting {@value #XML_TYPE_CLASS_FOR_NAME_PROPERTY} to {@code true}.
     *
     * @param node the XML node whose {@code type} attribute is to be resolved; must not be {@code null}
     * @return the resolved {@link Class}, or {@code null} if the attribute is missing, blank, or rejected by the type policy
     * @throws IllegalArgumentException if {@code node} is {@code null}
     * @throws RuntimeException if an accepted type expression cannot be resolved by the type registry
     */
    static Class<?> getAttributeTypeClass(final Node node) throws IllegalArgumentException, RuntimeException {
        final Type<?> type = getAttributeType(node);

        return type == null ? null : type.javaType();
    }

    /**
     * Resolves the complete type expression declared by a node after applying the XML type allowlist.
     * Unlike {@link #getAttributeTypeClass(Node)}, this retains generic component information.
     *
     * @throws IllegalArgumentException if {@code node} is {@code null}
     */
    static Type<?> getAttributeType(final Node node) throws IllegalArgumentException {
        final String typeAttr = XmlUtil.getAttribute(node, TYPE);

        if (Strings.isEmpty(typeAttr)) {
            return null;
        }

        final String typeName = typeAttr.trim();

        if (typeName.isEmpty() || (!Boolean.getBoolean(XML_TYPE_CLASS_FOR_NAME_PROPERTY) && !isSafeXmlTypeAttributeName(typeName))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Refusing to resolve XML type attribute '{}'. Set -D{}=true only for trusted legacy XML.", typeName,
                        XML_TYPE_CLASS_FOR_NAME_PROPERTY);
            }

            return null;
        }

        return Type.of(typeName);
    }

    /*
     * static Class&lt;?&gt; getAttributeTypeClass(Attributes attrs) { if (attrs == null) { return null; }
     *
     * String typeAttr = attrs.getValue(TYPE);
     *
     * if (typeAttr == null) { return null; }
     *
     * Type&lt;?&gt; type = N.getType(typeAttr);
     *
     * if (type != null) { return type.getTypeClass(); }
     *
     * try { return N.forName(typeAttr); } catch (RuntimeException e) { return null; } }
     */
    /**
     * Returns the most concrete class to use when deserializing a value.
     * If {@code targetClass} is {@code null}, returns {@code typeClass}. Otherwise,
     * if {@code typeClass} is {@code null} or is not assignable to {@code targetClass},
     * {@code targetClass} is returned unchanged. Otherwise {@code typeClass} (the more
     * specific, XML-declared type) is returned.
     *
     * @param targetClass the declared or expected class from the caller; may be {@code null}
     * @param typeClass the class resolved from an XML {@code type} attribute; may be {@code null}
     * @return {@code typeClass} when {@code targetClass} is {@code null} or accepts that type, otherwise {@code targetClass}
     */
    static Class<?> getConcreteClass(final Class<?> targetClass, final Class<?> typeClass) {
        if ((typeClass == null) || ((targetClass != null) && !targetClass.isAssignableFrom(typeClass))) {
            return targetClass;
        } else {
            return typeClass;
        }
    }

    /**
     * Returns the most concrete class to use when deserializing a value, resolving the
     * {@code type} attribute of {@code node} via {@link #getAttributeTypeClass(Node)} and
     * delegating to {@link #getConcreteClass(Class, Class)}.
     *
     * @param targetClass the declared or expected class; may be {@code null}
     * @param node the XML node that may carry a {@code type} attribute; may be {@code null}
     * @return the resolved concrete class, or {@code targetClass} if {@code node} is {@code null}
     *         or carries no usable type information
     */
    static Class<?> getConcreteClass(final Class<?> targetClass, final Node node) {
        if (node == null) {
            return targetClass;
        }

        final Class<?> typeClass = getAttributeTypeClass(node);

        return getConcreteClass(targetClass, typeClass);
    }

    /*
     * static Class&lt;?&gt; getConcreteClass(Class&lt;?&gt; targetClass, Attributes attrs) { if (attrs == null) { return targetClass;
     * }
     *
     * Class&lt;?&gt; typeClass = getAttributeTypeClass(attrs);
     *
     * return getConcreteClass(targetClass, typeClass); }
     */
    /**
     * Determines the {@link NodeType} for an XML element based on its name and the type of
     * its parent node. If the previous node was an {@link NodeType#ENTITY ENTITY}, the current
     * node is treated as a {@link NodeType#PROPERTY PROPERTY}. Otherwise the node name is
     * looked up in the internal type pool; if no match is found, {@link NodeType#ENTITY ENTITY}
     * is returned.
     *
     * @param nodeName the local name of the XML element being classified
     * @param previousNodeType the {@link NodeType} of the immediately enclosing parent node
     * @return the resolved {@link NodeType} for the element
     */
    static NodeType getNodeType(final String nodeName, final NodeType previousNodeType) {
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
     * @throws IllegalArgumentException if {@code path} is {@code null}
     * @throws JAXBException if a JAXB provider cannot create a context for the packages in {@code path}
     */
    private static JAXBContext jaxbContext(final String path) throws IllegalArgumentException, JAXBException {
        N.checkArgNotNull(path, cs.contextPath);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return pathJaxbContextPool.get(loader, path, () -> JAXBContext.newInstance(path, loader));
    }

    /**
     * @throws IllegalArgumentException if {@code type} is {@code null}
     * @throws JAXBException if a JAXB provider cannot create a context for {@code type}
     */
    private static JAXBContext jaxbContext(final Class<?> type) throws IllegalArgumentException, JAXBException {
        N.checkArgNotNull(type, cs.cls);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return classJaxbContextPool.get(type).get(loader, Boolean.TRUE, () -> JAXBContext.newInstance(type));
    }

    /** Context values must also be weak: a context can retain its binding and provider loaders. */
    private static final class LoaderJaxbCache<K> {
        private final ReferenceQueue<ClassLoader> staleLoaders = new ReferenceQueue<>();
        private final Map<IdentityWeakReference<ClassLoader>, Map<K, JaxbContextSlot>> byLoader = new HashMap<>();

        JAXBContext get(final ClassLoader loader, final K key, final Throwables.Supplier<JAXBContext, JAXBException> factory) throws JAXBException {
            final JaxbContextSlot slot;
            synchronized (this) {
                java.lang.ref.Reference<? extends ClassLoader> stale;
                while ((stale = staleLoaders.poll()) != null) {
                    byLoader.remove(stale);
                }
                // Null represents the actual null TCCL; ordinary loader keys compare by identity.
                final IdentityWeakReference<ClassLoader> lookup = loader == null ? null : new IdentityWeakReference<>(loader);
                Map<K, JaxbContextSlot> contexts = byLoader.get(lookup);
                if (contexts == null) {
                    contexts = new HashMap<>();
                    byLoader.put(loader == null ? null : new IdentityWeakReference<>(loader, staleLoaders), contexts);
                }
                slot = contexts.computeIfAbsent(key, ignored -> new JaxbContextSlot());
            }
            // Expensive creation is serialized per key, without blocking unrelated context misses.
            return slot.get(factory);
        }
    }

    private static final class JaxbContextSlot {
        private WeakReference<JAXBContext> cached = new WeakReference<>(null);

        synchronized JAXBContext get(final Throwables.Supplier<JAXBContext, JAXBException> factory) throws JAXBException {
            JAXBContext context = cached.get();
            if (context == null) {
                context = factory.get();
                cached = new WeakReference<>(context);
            }
            // Do not retain the factory: its captured arguments can strongly retain class loaders.
            return context;
        }
    }

    /** Weak, identity-based ownership registry used to keep caller-supplied parsers out of shared pools. */
    private static final class WeakIdentitySet<T> {
        private final ReferenceQueue<T> staleReferences = new ReferenceQueue<>();
        private final Set<IdentityWeakReference<T>> references = new HashSet<>();

        synchronized void add(final T value) {
            removeStaleReferences();
            references.add(new IdentityWeakReference<>(value, staleReferences));
        }

        synchronized boolean contains(final T value) {
            removeStaleReferences();
            return references.contains(new IdentityWeakReference<>(value));
        }

        @SuppressWarnings("unchecked")
        private void removeStaleReferences() {
            IdentityWeakReference<T> reference;

            while ((reference = (IdentityWeakReference<T>) staleReferences.poll()) != null) {
                references.remove(reference);
            }
        }
    }

    private static final class IdentityWeakReference<T> extends WeakReference<T> {
        private final int identityHashCode;

        IdentityWeakReference(final T referent) {
            super(referent);
            identityHashCode = System.identityHashCode(referent);
        }

        IdentityWeakReference(final T referent, final ReferenceQueue<T> referenceQueue) {
            super(referent, referenceQueue);
            identityHashCode = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return identityHashCode;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof IdentityWeakReference<?>)) {
                return false;
            }

            final Object referent = get();
            return referent != null && referent == ((IdentityWeakReference<?>) obj).get();
        }
    }

    /**
     * Classifies each XML node encountered during abacus-XML deserialization so that the
     * parser knows how to reconstruct the corresponding Java object graph.
     */
    enum NodeType {

        /** A JavaBean (entity) element whose child elements are property values. */
        ENTITY,
        /** A single property element that belongs to an enclosing {@link #ENTITY}. */
        PROPERTY,
        /** An array container element. */
        ARRAY,
        /** An individual element within an array or collection. */
        ELEMENT,
        /** A {@code Collection} (list/set) container element. */
        COLLECTION,
        /** A {@code Map} container element. */
        MAP,
        /** A single map-entry element within a {@link #MAP}. */
        ENTRY,
        /** The key child element of a map {@link #ENTRY}. */
        KEY,
        /** The value child element of a map {@link #ENTRY}. */
        VALUE
    }

}
