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
 * <p>Example usage:
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
     * Deserializes an XML DOM node to an object of the specified type.
     * 
     * <p>Example:
     * <pre>{@code
     * Document doc = DocumentBuilderFactory.newInstance()
     *     .newDocumentBuilder().parse(xmlFile);
     * MyBean bean = parser.deserialize(doc.getFirstChild(), MyBean.class);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param source the DOM node containing the XML data
     * @param targetClass the class of the object to create
     * @return the deserialized object
     */
    <T> T deserialize(Node source, Class<? extends T> targetClass);

    /**
     * Deserializes an XML DOM node to an object of the specified type with custom configuration.
     * 
     * <p>Example:
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
     * @param source the DOM node containing the XML data
     * @param config the deserialization configuration
     * @param targetClass the class of the object to create
     * @return the deserialized object
     */
    <T> T deserialize(Node source, XMLDeserializationConfig config, Class<? extends T> targetClass);

    /**
     * Deserializes XML from an input stream using node class mappings.
     * The target class is determined by matching node names to the provided class mappings.
     * 
     * <p>Example:
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
     * @param source the input stream containing XML data
     * @param config the deserialization configuration
     * @param nodeClasses mapping of node names to their corresponding classes
     * @return the deserialized object
     */
    <T> T deserialize(InputStream source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);

    /**
     * Deserializes XML from a reader using node class mappings.
     * The target class is determined by matching node names to the provided class mappings.
     * 
     * <p>Example:
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
     * @param source the reader containing XML data
     * @param config the deserialization configuration
     * @param nodeClasses mapping of node names to their corresponding classes
     * @return the deserialized object
     */
    <T> T deserialize(Reader source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);

    /**
     * Deserializes an XML DOM node using node class mappings.
     * The target class is determined by matching the node name or its 'name' attribute
     * to the provided class mappings.
     * 
     * <p>Example:
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
     * @param source the DOM node containing XML data
     * @param config the deserialization configuration
     * @param nodeClasses mapping of node names to their corresponding classes
     * @return the deserialized object
     */
    <T> T deserialize(Node source, XMLDeserializationConfig config, Map<String, Class<?>> nodeClasses);
}