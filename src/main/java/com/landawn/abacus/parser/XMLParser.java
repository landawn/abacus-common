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

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.w3c.dom.Node;

/**
 * Interface for XML parsing operations, extending the base {@link Parser} interface.
 * This interface provides XML-specific serialization and deserialization methods,
 * including support for DOM nodes and node class mappings.
 * 
 * <p>The XMLParser supports:
 * <ul>
 *   <li>Serialization of Java objects to XML format</li>
 *   <li>Deserialization of XML content from various sources (String, File, InputStream, Reader, DOM Node)</li>
 *   <li>Configuration-based customization of serialization/deserialization behavior</li>
 *   <li>DOM-based parsing with Node objects</li>
 *   <li>Dynamic type resolution using node class mappings</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * XMLParser parser = ParserFactory.createXMLParser();
 * 
 * // Serialize object to XML
 * MyBean bean = new MyBean();
 * String xml = parser.serialize(bean);
 * 
 * // Deserialize XML to object
 * MyBean restored = parser.deserialize(xml, MyBean.class);
 * 
 * // Deserialize from DOM Node
 * Document doc = DocumentBuilderFactory.newInstance()
 *     .newDocumentBuilder().parse(xmlFile);
 * MyBean fromNode = parser.deserialize(doc.getFirstChild(), MyBean.class);
 * 
 * // Deserialize with node class mappings
 * Map<String, Class<?>> nodeClasses = new HashMap<>();
 * nodeClasses.put("person", Person.class);
 * nodeClasses.put("company", Company.class);
 * Object result = parser.deserialize(xmlStream, config, nodeClasses);
 * }</pre>
 * 
 * @see Parser
 * @see XMLSerializationConfig
 * @see XMLDeserializationConfig
 * @since 1.0
 */
public interface XMLParser extends Parser<XMLSerializationConfig, XMLDeserializationConfig> {

    /**
     * Deserializes an XML DOM node to an object of the specified type using default deserialization configuration.
     *
     * <p>This method parses the provided DOM node and creates an instance of the target class,
     * populating its fields with values from the XML structure. The deserialization process
     * uses default configuration settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Document doc = DocumentBuilderFactory.newInstance()
     *     .newDocumentBuilder().parse(xmlFile);
     * MyBean bean = parser.deserialize(doc.getFirstChild(), MyBean.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing the XML data to deserialize; must not be {@code null}
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}, never {@code null}
     * @throws IllegalArgumentException if source or targetClass is {@code null}
     * @throws RuntimeException if deserialization fails due to XML structure mismatch or instantiation errors
     */
    <T> T deserialize(Node source, Class<? extends T> targetClass);

    /**
     * Deserializes an XML DOM node to an object of the specified type with custom deserialization configuration.
     *
     * <p>This method provides fine-grained control over the deserialization process through the configuration
     * parameter. The configuration can specify options such as ignoring unknown properties, handling null values,
     * date/time formats, and other deserialization behaviors.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLDeserializationConfig config = new XMLDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     *
     * Document doc = DocumentBuilderFactory.newInstance()
     *     .newDocumentBuilder().parse(xmlFile);
     * MyBean bean = parser.deserialize(doc.getFirstChild(), config, MyBean.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing the XML data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to control parsing behavior; may be {@code null} to use defaults
     * @param targetClass the class of the object to create; must not be {@code null}
     * @return the deserialized object of type {@code T}, never {@code null}
     * @throws IllegalArgumentException if source or targetClass is {@code null}
     * @throws RuntimeException if deserialization fails due to XML structure mismatch or instantiation errors
     */
    <T> T deserialize(Node source, XMLDeserializationConfig config, Class<? extends T> targetClass);

    /**
     * Deserializes XML from an input stream using node class mappings for dynamic type resolution.
     *
     * <p>This method enables polymorphic deserialization by mapping XML element names to Java classes.
     * When the parser encounters an XML element, it looks up the element name in the nodeClasses map
     * to determine which class to instantiate. This is particularly useful for deserializing heterogeneous
     * collections or when the target type cannot be determined statically.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Class<?>> nodeClasses = new HashMap<>();
     * nodeClasses.put("person", Person.class);
     * nodeClasses.put("company", Company.class);
     *
     * // XML: <person>...</person> will deserialize to Person class
     * Object result = parser.deserialize(inputStream, config, nodeClasses);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to control parsing behavior; may be {@code null} to use defaults
     * @param nodeClasses mapping of XML element names to their corresponding Java classes; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the stream is empty
     * @throws IllegalArgumentException if source or nodeClasses is {@code null}
     * @throws RuntimeException if deserialization fails due to I/O errors, XML parsing errors, or instantiation errors
     */
    <T> T deserialize(InputStream source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);

    /**
     * Deserializes XML from a reader using node class mappings for dynamic type resolution.
     *
     * <p>This method enables polymorphic deserialization by mapping XML element names to Java classes.
     * When the parser encounters an XML element, it looks up the element name in the nodeClasses map
     * to determine which class to instantiate. This is particularly useful for deserializing heterogeneous
     * collections or when the target type cannot be determined statically.
     *
     * <p>The reader-based approach allows for character encoding control and is suitable for text-based
     * XML sources where character encoding has already been handled by the Reader implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Class<?>> nodeClasses = new HashMap<>();
     * nodeClasses.put("employee", Employee.class);
     * nodeClasses.put("department", Department.class);
     *
     * Reader reader = new FileReader("data.xml");
     * Object result = parser.deserialize(reader, config, nodeClasses);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to control parsing behavior; may be {@code null} to use defaults
     * @param nodeClasses mapping of XML element names to their corresponding Java classes; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the reader is empty
     * @throws IllegalArgumentException if source or nodeClasses is {@code null}
     * @throws RuntimeException if deserialization fails due to I/O errors, XML parsing errors, or instantiation errors
     */
    <T> T deserialize(Reader source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);

    /**
     * Deserializes an XML DOM node using node class mappings for dynamic type resolution.
     *
     * <p>This method enables polymorphic deserialization from DOM nodes by mapping XML element names
     * to Java classes. The target class is determined by matching the node name or its 'name' attribute
     * to the provided class mappings. This is particularly useful when working with pre-parsed DOM trees
     * containing heterogeneous elements.
     *
     * <p>The parser first attempts to match the node's tag name against the nodeClasses map. If no match
     * is found and the node has a 'name' attribute, it will attempt to match using that attribute value.
     * This provides flexibility in XML structure design.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Class<?>> nodeClasses = new HashMap<>();
     * nodeClasses.put("product", Product.class);
     * nodeClasses.put("category", Category.class);
     *
     * Document doc = DocumentBuilderFactory.newInstance()
     *     .newDocumentBuilder().parse(xmlFile);
     * Object result = parser.deserialize(doc.getFirstChild(), config, nodeClasses);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data to deserialize; must not be {@code null}
     * @param config the deserialization configuration to control parsing behavior; may be {@code null} to use defaults
     * @param nodeClasses mapping of XML element names to their corresponding Java classes; must not be {@code null}
     * @return the deserialized object of type {@code T}, or {@code null} if the node is empty
     * @throws IllegalArgumentException if source or nodeClasses is {@code null}
     * @throws RuntimeException if deserialization fails due to XML structure mismatch or instantiation errors
     */
    <T> T deserialize(Node source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);
}