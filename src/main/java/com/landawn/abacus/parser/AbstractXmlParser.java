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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;

import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
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
 * @see XmlSerializationConfig
 * @see XmlDeserializationConfig
 */
abstract class AbstractXmlParser extends AbstractParser<XmlSerializationConfig, XmlDeserializationConfig> implements XmlParser {

    private static final Logger logger = LoggerFactory.getLogger(AbstractXmlParser.class);

    // protected static final int TEXT_SIZE_TO_READ_MORE = 256;

    protected static final JsonParser jsonParser = ParserFactory.createJsonParser();

    @SuppressWarnings("deprecation")
    protected static final JsonSerializationConfig jsc = JSC.create().setCharQuotation(WD.CHAR_ZERO);

    @SuppressWarnings("deprecation")
    protected static final JsonSerializationConfig jscWithEmptyBeanSupported = JSC.create().setCharQuotation(WD.CHAR_ZERO).failOnEmptyBean(false);

    @SuppressWarnings("deprecation")
    protected static final JsonSerializationConfig jscWithCircularRefSupported = JSC.create().setCharQuotation(WD.CHAR_ZERO).supportCircularReference(true);

    @SuppressWarnings("deprecation")
    protected static final JsonSerializationConfig jscWithCircularRefAndEmptyBeanSupported = JSC.create()
            .setCharQuotation(WD.CHAR_ZERO)
            .failOnEmptyBean(false)
            .supportCircularReference(true);

    protected static final Type<?> defaultKeyType = objType;

    protected static final Type<?> defaultValueType = objType;

    protected final XmlSerializationConfig defaultXmlSerializationConfig;

    protected final XmlDeserializationConfig defaultXmlDeserializationConfig;

    protected AbstractXmlParser() {
        this(null, null);
    }

    protected AbstractXmlParser(final XmlSerializationConfig xsc, final XmlDeserializationConfig xdc) {
        defaultXmlSerializationConfig = xsc != null ? xsc : new XmlSerializationConfig();
        defaultXmlDeserializationConfig = xdc != null ? xdc : new XmlDeserializationConfig();
    }

    /**
     * Deserializes an XML DOM node into an object of the specified target type using default deserialization configuration.
     * This method provides a convenient way to convert XML node structures into Java objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = XmlUtil.parse(xmlString);
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
     * Document doc = XmlUtil.parse(xmlString);
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
     * Deserializes XML content from a file into an object using default deserialization configuration.
     * This method is not supported in the abstract base class and should be implemented by subclasses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("data.xml");
     * MyObject obj = parser.deserialize(xmlFile, config, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML content
     * @param config the XML deserialization configuration
     * @param nodeTypes a map of node type definitions for deserialization
     * @return an instance of the target type populated with data from the XML content
     * @throws UnsupportedOperationException always thrown in this abstract implementation
     */
    @Override
    public <T> T deserialize(final File source, final XmlDeserializationConfig config, final Map<String, Type<?>> nodeTypes)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML content from an input stream into an object using default deserialization configuration.
     * This method is not supported in the abstract base class and should be implemented by subclasses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream inputStream = new FileInputStream("data.xml");
     * MyObject obj = parser.deserialize(inputStream, config, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML content
     * @param config the XML deserialization configuration
     * @param nodeTypes a map of node type definitions for deserialization
     * @return an instance of the target type populated with data from the XML content
     * @throws UnsupportedOperationException always thrown in this abstract implementation
     */
    @Override
    public <T> T deserialize(final InputStream source, final XmlDeserializationConfig config, final Map<String, Type<?>> nodeTypes)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML content from a reader into an object using default deserialization configuration.
     * This method is not supported in the abstract base class and should be implemented by subclasses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("data.xml");
     * MyObject obj = parser.deserialize(reader, config, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML content
     * @param config the XML deserialization configuration
     * @param nodeTypes a map of node type definitions for deserialization
     * @return an instance of the target type populated with data from the XML content
     * @throws UnsupportedOperationException always thrown in this abstract implementation
     */
    @Override
    public <T> T deserialize(final Reader source, final XmlDeserializationConfig config, final Map<String, Type<?>> nodeTypes)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes XML content from a DOM node into an object using default deserialization configuration.
     * This method is not supported in the abstract base class and should be implemented by subclasses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = XmlUtil.parse(xmlString);
     * Node node = doc.getDocumentElement();
     * MyObject obj = parser.deserialize(node, config, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the XML DOM node to deserialize
     * @param config the XML deserialization configuration
     * @param nodeTypes a map of node type definitions for deserialization
     * @return an instance of the target type populated with data from the XML node
     * @throws UnsupportedOperationException always thrown in this abstract implementation
     */
    @Override
    public <T> T deserialize(final Node source, final XmlDeserializationConfig config, final Map<String, Type<?>> nodeTypes)
            throws UnsupportedOperationException {
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
     * @param propInfo property metadata including format information, or null
     * @param propNode the XML node containing the property value
     * @return the converted property value, or {@code null} if the node indicates a {@code null} value
     * @throws ParseException if the property cannot be parsed or type is null
     */
    protected Object getPropValue(final String propName, final Type<?> propType, final PropInfo propInfo, final Node propNode) {
        final String txtValue = XmlUtil.getTextContent(propNode);

        if (Strings.isEmpty(txtValue)) {
            final Node attributeNode = propNode.getAttributes().getNamedItem(XmlConstants.IS_NULL);

            if ((attributeNode != null) && Boolean.parseBoolean(attributeNode.getNodeValue())) { //NOSONAR
                return null;
            }
        }

        if (propType == null) {
            throw new ParseException("Can't parse property " + propName + " with value: " + txtValue);
        }

        if (propInfo != null && propInfo.hasFormat) {
            return propInfo.readPropValue(txtValue);
        } else {
            return propType.valueOf(txtValue);
        }
    }

    protected XmlSerializationConfig check(XmlSerializationConfig config) {
        return config == null ? defaultXmlSerializationConfig : config;
    }

    protected XmlDeserializationConfig check(XmlDeserializationConfig config) {
        return config == null ? defaultXmlDeserializationConfig : config;
    }

    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(final Class<?> propClass, final Node node) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName());
                }
            }
        }

        final Class<?> attribeTypeClass = getAttributeTypeClass(node);

        return newPropInstance(propClass, attribeTypeClass);
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
     * @return a new instance of the property class, or {@code null} if instantiation fails
     */
    @SuppressWarnings("unchecked")
    protected static <T> T newPropInstance(final Class<?> propClass, final Attributes attrs) {
        if ((propClass != null) && !Modifier.isAbstract(propClass.getModifiers())) {
            try {
                return (T) N.newInstance(propClass);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to new instance by class: " + propClass.getName());
                }
            }
        }

        final Class<?> attribeTypeClass = getAttributeTypeClass(attrs);

        return newPropInstance(propClass, attribeTypeClass);
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
     * @return the Class corresponding to the type attribute, or {@code null} if no type attribute exists
     */
    protected static Class<?> getAttributeTypeClass(final Node node) {
        final String typeAttr = XmlUtil.getAttribute(node, XmlConstants.TYPE);

        if (typeAttr == null) {
            return null;
        }

        return Type.of(typeAttr).clazz();
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
     * @return the Class corresponding to the type attribute, or {@code null} if no type attribute exists
     */
    protected static Class<?> getAttributeTypeClass(final Attributes attrs) {
        if (attrs == null) {
            return null;
        }

        final String typeAttr = attrs.getValue(XmlConstants.TYPE);

        if (typeAttr == null) {
            return null;
        }

        return Type.of(typeAttr).clazz();
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
     * @return the Class corresponding to the type attribute, or {@code null} if no type attribute exists
     */
    protected static Class<?> getAttributeTypeClass(final XMLStreamReader xmlReader) {
        if (xmlReader.getAttributeCount() == 0) {
            return null;
        }

        final String typeAttr = getAttribute(xmlReader, XmlConstants.TYPE);

        if (typeAttr == null) {
            return null;
        }

        return Type.of(typeAttr).clazz();
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
     * @param node the XML node that may contain type attribute information
     * @param targetType the expected target class for deserialization
     * @return the concrete class to instantiate, either from the type attribute or the target class
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
     * @param attrs the XML attributes that may contain type information
     * @param targetType the expected target class for deserialization 
     * @return the concrete class to instantiate, either from the type attribute or the target class
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
     * @param xmlReader the XML stream reader positioned at an element with attributes
     * @param targetType the expected target class for deserialization
     * @return the concrete class to instantiate, either from the type attribute or the target class
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
     * This method ensures that an element contains exactly one meaningful child node, ignoring text nodes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Node element = doc.getElementsByTagName("wrapper").item(0);
     * Node singleChild = checkOneNode(element);
     * }</pre>
     *
     * @param eleNode the XML element node to examine
     * @return the single child element node
     * @throws ParseException if the element contains more than one child element node
     */
    protected static Node checkOneNode(final Node eleNode) {
        final NodeList subEleNodes = eleNode.getChildNodes();
        Node subEleNode = null;

        if (subEleNodes.getLength() == 1) {
            subEleNode = subEleNodes.item(0);
        } else {
            for (int j = 0; j < subEleNodes.getLength(); j++) {
                //noinspection StatementWithEmptyBody
                if (subEleNodes.item(j).getNodeType() == Document.TEXT_NODE) {
                    //NOSONAR
                } else if (subEleNode == null) {
                    subEleNode = subEleNodes.item(j);
                } else {
                    throw new ParseException("Only one child node is supported");
                }
            }
        }

        return subEleNode;
    }

    protected static int getNodeLength(final NodeList nodeList) {
        return (nodeList == null) ? 0 : nodeList.getLength();
    }

    /**
     * Retrieves the appropriate JSON serialization configuration based on XML serialization settings.
     * This method maps XML serialization options to JSON serialization configurations for internal processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerializationConfig xmlConfig = new XmlSerializationConfig().supportCircularReference(true);
     * JsonSerializationConfig jsonConfig = getJSC(xmlConfig);
     * }</pre>
     *
     * @param config the XML serialization configuration to map, or {@code null} for default
     * @return a JSON serialization configuration with corresponding settings
     */
    protected JsonSerializationConfig getJSC(final XmlSerializationConfig config) {
        if (config == null) {
            return jsc;
        }

        if (config.supportCircularReference()) {
            if (!config.failOnEmptyBean()) {
                return jscWithCircularRefAndEmptyBeanSupported;
            } else {
                return jscWithCircularRefSupported;
            }
        } else if (!config.failOnEmptyBean()) {
            return jscWithEmptyBeanSupported;
        }

        return jsc;
    }

    enum NodeType {
        ENTITY, PROPERTY, ARRAY, ELEMENT, COLLECTION, MAP, ENTRY, KEY, VALUE
    }
}
