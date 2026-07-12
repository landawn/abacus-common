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
import java.util.Map;

import org.w3c.dom.Node;

import com.landawn.abacus.type.Type;

/**
 * Interface for XML parsing operations, extending the base {@link Parser} interface.
 * This interface provides XML-specific serialization and deserialization methods,
 * including support for DOM nodes and node class mappings.
 *
 * <p>The XmlParser supports:</p>
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
 * XmlParser parser = ParserFactory.createXmlParser();
 *
 * // Serialize object to XML
 * MyBean bean = new MyBean();
 * String xml = parser.serialize(bean);
 *
 * // Deserialize XML to object
 * MyBean restored = parser.deserialize(xml, MyBean.class);
 *
 * // Deserialize from DOM Node
 * try {
 *     Document doc = XmlUtil.createDOMParser().parse(xmlFile);
 *     MyBean fromNode = parser.deserialize(doc.getFirstChild(), MyBean.class);
 * } catch (Exception e) {
 *     // Handle parsing exception
 * }
 *
 * // Deserialize with node type mappings
 * Map<String, Type<?>> nodeTypes = new HashMap<>();
 * nodeTypes.put("person", Type.of(Person.class));
 * nodeTypes.put("company", Type.of(Company.class));
 * Object result = parser.deserialize(xmlStream, config, nodeTypes);
 * }</pre>
 *
 * @see Parser
 * @see XmlSerConfig
 * @see XmlDeserConfig
 */
public interface XmlParser extends Parser<XmlSerConfig, XmlDeserConfig> {

    /**
     * Deserializes an XML DOM node to an object of the specified type using a Type descriptor.
     *
     * <p>This method parses the provided DOM node and creates an instance of the target type,
     * populating its fields with values from the XML structure. The deserialization process
     * uses default configuration settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     Document doc = XmlUtil.createDOMParser().parse(xmlFile);
     *     Type<MyBean> type = Type.of(MyBean.class);
     *     MyBean bean = parser.deserialize(doc.getFirstChild(), type);
     * } catch (Exception e) {
     *     // Handle parsing exception
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing the XML data to deserialize (must not be {@code null})
     * @param targetType the Type descriptor of the object to create (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.ParsingException if the XML structure does not match the target type
     */
    <T> T deserialize(Node source, Type<? extends T> targetType);

    /**
     * Deserializes an XML DOM node to an object of the specified type using default deserialization configuration.
     *
     * <p>This method parses the provided DOM node and creates an instance of the target class,
     * populating its fields with values from the XML structure. The deserialization process
     * uses default configuration settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     Document doc = XmlUtil.createDOMParser().parse(xmlFile);
     *     MyBean bean = parser.deserialize(doc.getFirstChild(), MyBean.class);
     * } catch (Exception e) {
     *     // Handle parsing exception
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing the XML data to deserialize (must not be {@code null})
     * @param targetType the class of the object to create (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.ParsingException if the XML structure does not match the target type
     */
    <T> T deserialize(Node source, Class<? extends T> targetType);

    /**
     * Deserializes an XML DOM node to an object of the specified type with custom deserialization configuration.
     *
     * <p>This method provides fine-grained control over the deserialization process through the configuration
     * parameter. The configuration can specify options such as ignoring unknown properties, handling {@code null} values,
     * and other deserialization behaviors.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     *
     * try {
     *     Document doc = XmlUtil.createDOMParser().parse(xmlFile);
     *     Type<MyBean> type = Type.of(MyBean.class);
     *     MyBean bean = parser.deserialize(doc.getFirstChild(), config, type);
     * } catch (Exception e) {
     *     // Handle parsing exception
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing the XML data to deserialize (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the Type descriptor of the object to create (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.ParsingException if the XML structure does not match the target type
     */
    <T> T deserialize(Node source, XmlDeserConfig config, Type<? extends T> targetType);

    /**
     * Deserializes an XML DOM node to an object of the specified type with custom deserialization configuration.
     *
     * <p>This method provides fine-grained control over the deserialization process through the configuration
     * parameter. The configuration can specify options such as ignoring unknown properties, handling {@code null} values,
     * and other deserialization behaviors.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     *
     * try {
     *     Document doc = XmlUtil.createDOMParser().parse(xmlFile);
     *     MyBean bean = parser.deserialize(doc.getFirstChild(), config, MyBean.class);
     * } catch (Exception e) {
     *     // Handle parsing exception
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing the XML data to deserialize (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the class of the object to create (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.ParsingException if the XML structure does not match the target type
     */
    <T> T deserialize(Node source, XmlDeserConfig config, Class<? extends T> targetType);

    /**
     * Deserializes XML from a file using node class mappings for dynamic type resolution.
     *
     * <p>This method enables polymorphic deserialization by mapping XML element names to types.
     * The root element's name is looked up in the nodeTypes map to determine which class to
     * instantiate (nested elements are not resolved through the map). This is useful when the
     * target type cannot be determined statically.
     *
     * <p><b>Note on the return type:</b> although the signature declares {@code <T> T}, the concrete runtime
     * type is selected from {@code nodeTypes} at parse time (there is no {@code Class<T>}/{@code Type<T>}
     * argument to drive the inference). The {@code <T>} merely saves the caller an explicit cast; callers
     * should treat the result as {@code Object} and only narrow it once the actual element type is known.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("customer", Type.of(Customer.class));
     * nodeTypes.put("invoice", Type.of(Invoice.class));
     *
     * File xmlFile = new File("data.xml");
     * Object result = parser.deserialize(xmlFile, config, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the file containing XML data to deserialize (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.UncheckedIOException if an I/O error occurs while reading the file
     * @throws com.landawn.abacus.exception.ParsingException if no matching type is found in {@code nodeTypes} or the XML is malformed
     */
    <T> T deserialize(File source, XmlDeserConfig config, Map<String, Type<?>> nodeTypes);

    /**
     * Deserializes XML from an input stream using node class mappings for dynamic type resolution.
     *
     * <p>This method enables polymorphic deserialization by mapping XML element names to types.
     * The root element's name is looked up in the nodeTypes map to determine which class to
     * instantiate (nested elements are not resolved through the map). This is useful when the
     * target type cannot be determined statically.
     *
     * <p><b>Note on the return type:</b> although the signature declares {@code <T> T}, the concrete runtime
     * type is selected from {@code nodeTypes} at parse time (there is no {@code Class<T>}/{@code Type<T>}
     * argument to drive the inference). The {@code <T>} merely saves the caller an explicit cast; callers
     * should treat the result as {@code Object} and only narrow it once the actual element type is known.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("item", Type.of(Item.class));
     * nodeTypes.put("order", Type.of(Order.class));
     *
     * InputStream inputStream = new FileInputStream("data.xml");
     * Object result = parser.deserialize(inputStream, config, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the input stream containing XML data to deserialize (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.UncheckedIOException if an I/O error occurs while reading the stream
     * @throws com.landawn.abacus.exception.ParsingException if no matching type is found in {@code nodeTypes} or the XML is malformed
     */
    <T> T deserialize(InputStream source, XmlDeserConfig config, Map<String, Type<?>> nodeTypes);

    /**
     * Deserializes XML from a reader using node class mappings for dynamic type resolution.
     *
     * <p>This method enables polymorphic deserialization by mapping XML element names to types.
     * The root element's name is looked up in the nodeTypes map to determine which class to
     * instantiate (nested elements are not resolved through the map). This is useful when the
     * target type cannot be determined statically.
     *
     * <p><b>Note on the return type:</b> although the signature declares {@code <T> T}, the concrete runtime
     * type is selected from {@code nodeTypes} at parse time (there is no {@code Class<T>}/{@code Type<T>}
     * argument to drive the inference). The {@code <T>} merely saves the caller an explicit cast; callers
     * should treat the result as {@code Object} and only narrow it once the actual element type is known.</p>
     *
     * <p>The reader-based approach allows for character encoding control and is suitable for text-based
     * XML sources where character encoding has already been handled by the Reader implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("employee", Type.of(Employee.class));
     * nodeTypes.put("department", Type.of(Department.class));
     *
     * Reader reader = new FileReader("data.xml");
     * Object result = parser.deserialize(reader, config, nodeTypes);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the reader containing XML data to deserialize (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.UncheckedIOException if an I/O error occurs while reading
     * @throws com.landawn.abacus.exception.ParsingException if no matching type is found in {@code nodeTypes} or the XML is malformed
     */
    <T> T deserialize(Reader source, XmlDeserConfig config, Map<String, Type<?>> nodeTypes);

    /**
     * Deserializes an XML DOM node using node class mappings for dynamic type resolution.
     *
     * <p>This method enables polymorphic deserialization from DOM nodes by mapping XML element names
     * to types. The target class is determined by matching the node's <i>name</i> attribute or its tag
     * name to the provided class mappings. This is particularly useful when working with pre-parsed DOM trees
     * containing heterogeneous elements.
     *
     * <p>The parser first attempts to match the node's <i>name</i> attribute against the {@code nodeTypes} map.
     * If the node has no <i>name</i> attribute (or it is empty), it falls back to matching the node's tag name.
     * This provides flexibility in XML structure design.
     *
     * <p><b>Note on the return type:</b> although the signature declares {@code <T> T}, the concrete runtime
     * type is selected from {@code nodeTypes} at parse time (there is no {@code Class<T>}/{@code Type<T>}
     * argument to drive the inference). The {@code <T>} merely saves the caller an explicit cast; callers
     * should treat the result as {@code Object} and only narrow it once the actual element type is known.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Type<?>> nodeTypes = new HashMap<>();
     * nodeTypes.put("product", Type.of(Product.class));
     * nodeTypes.put("category", Type.of(Category.class));
     *
     * try {
     *     Document doc = XmlUtil.createDOMParser().parse(xmlFile);
     *     Object result = parser.deserialize(doc.getFirstChild(), config, nodeTypes);
     * } catch (Exception e) {
     *     // Handle parsing exception
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param source the DOM node containing XML data to deserialize (must not be {@code null})
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param nodeTypes mapping of XML element names to their corresponding types (must not be {@code null})
     * @return the deserialized object of type {@code T}
     * @throws com.landawn.abacus.exception.ParsingException if no matching type is found in {@code nodeTypes} or the XML structure is invalid
     */
    <T> T deserialize(Node source, XmlDeserConfig config, Map<String, Type<?>> nodeTypes);
}
