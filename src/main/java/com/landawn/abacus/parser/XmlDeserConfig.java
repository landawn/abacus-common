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

/**
 * Configuration class for XML deserialization settings.
 *
 * <p>This class extends {@link DeserializationConfig} to provide XML-specific
 * deserialization options. It inherits all configuration options from its parent
 * class without adding XML-specific settings.</p>
 *
 * <p>At a root collection, a configured element type overrides the target collection's generic element type,
 * for both child-element and compact JSON-text representations. Nested property and map-value declarations
 * retain their explicit element constraints; configuration fills their open {@code Object} components.
 * A declared array component is reified, so it beats global element configuration for JSON text and for
 * child elements, at the root and on map values: a {@code Long[]} target reads its elements as {@code Long}
 * whatever {@code setElementType} says, and so does a primitive array that an open {@code Object} slot adopts from
 * metadata: {@code [1,1]} under {@code type="char[]"} is {@code {'1', '1'}}. Only an open {@code Object} component
 * yields to configuration, and
 * then the array keeps its declared runtime class: an {@code Object[]} of {@code Long}, never a {@code Long[]}.
 * The open component may be nested, as in {@code Object[][]}, {@code List<Object[]>} or
 * {@code Map<String, Object[]>}: with element configuration present, metadata such as {@code Long[][]} supplies
 * the container implementation but does not narrow the declared arrays, and the configured type reaches the
 * innermost elements of compact text as the text is parsed: a parameterized component such as {@code List<Long>}
 * keeps its own arguments at every level, and a String component keeps each token's text ({@code 1.2300} stays
 * {@code "1.2300"}). A configured container component ({@code List<Object>}, {@code Object[]}) types the slot's open
 * elements once: its own elements are never typed again ({@code [[1,2],[3]]} read as {@code Object[]} with a
 * {@code List<Object>} component holds the two lists, not lists of lists), while a declared container element
 * ({@code Object[]} in {@code Object[][]}) is read as that element with the configuration applied inside it, for
 * compact item text and child elements alike. Without element configuration, array metadata on an open slot is the only
 * component information and decides both the runtime class and the elements. A declared reference array that is not
 * open ({@code Number[]}, {@code Map<String, Object>[]}) keeps its array class on every backend, at the root as in a
 * property; metadata refines its elements only: a {@code Number[]} slot with {@code Long[]} metadata holds {@code Long}
 * elements, read from compact text and from child elements alike, and so does a {@code List<Number>} with
 * {@code ArrayList<Long>} metadata. Configured elements may themselves be parameterized containers or
 * beans.</p>
 *
 * <p>Map implementation metadata must preserve the declared value shape. A list-valued implementation such as
 * Spring's {@code MultiValueMap} cannot hold a map's declared scalar, set or concrete-list values; such a
 * declaration, or such a global {@code setMapValueType}, keeps ordinary map construction on every backend, for
 * compact text as for child elements. In a declared map slot a declared key or value type wins over the global map
 * key and value types, which win over the arguments the slot's metadata supplies. At the
 * root, an explicit global map key or value type takes precedence over the requested type's own scalar, bean or open
 * arguments, while a requested container argument (an array, a collection or a map) keeps its shape and the global
 * type applies inside it, as it does below the root; a declared argument of a nested map keeps its precedence over the
 * global configuration. A root list's requested container element ({@code List<Object[]>}) is kept the same way, with
 * the configured element type reaching its open components. The global map key and value types
 * fill a map's open slots at any nesting level, inside compact text as inside child elements.
 * Compatible {@code List<V>} and open {@code Object} values keep both the specialized implementation and their
 * declared element types, for compact text and structured XML alike. Per-key {@code setValueType} configuration
 * is applied literally to any key by its text, inside compact text at any nesting level as well as to child
 * elements, and inside the compact text of a root array or list: an override of a present key that is not a
 * {@code List} (a scalar, or an explicit {@code Object}, which reads the value as open scalar text, or adopts the
 * value's own map metadata when it carries some) moves the map to ordinary construction and preserves the entries read
 * before it, even when the key's value is null; a {@code List} override keeps the implementation. Configuration for an
 * absent key does not change the implementation. A scalar per-key type on a
 * declared list, set or array value names its element type and keeps the declared container shape, for compact text and
 * child elements alike: {@code setValueType("k", String.class)} reads a {@code Map<String, List<Integer>>} entry written as
 * {@code <k>[1,2]</k>} or as {@code <k><e>1</e><e>2</e></k>} as {@code ["1", "2"]}, an {@code Integer[]} value as a
 * {@code String[]} and a {@code Set<Integer>} value as a {@code Set<String>}; a per-key container type ({@code String[]})
 * names the value's own type on every backend. A declared scalar or open
 * value keeps the configured type and reads its text literally. A list-valued implementation gets its {@code List}
 * values from the implementation rather than from the declaration, so there a scalar per-key type names the whole
 * value: scalar text moves the map to ordinary construction, while a JSON array or child elements read as a list of
 * the configured type and keep the implementation. List and array metadata that names a specialized map item
 * applies to child elements as it does to compact text. In standard XML a nested map value is written inside a
 * {@code <map>} element; a value without that wrapper is rejected rather than misread. The children of an open
 * {@code Object} slot written without type information are read as the writer named them: a single child is the
 * value (a {@code <map>} the map, an {@code <e>} its text), while several children, or a nested container child,
 * form a list whose {@code <map>}, {@code <list>}, {@code <set>}, {@code <array>} and {@code <e>} items are a map,
 * a list, a set, an {@code Object[]} and a String. A single child with a type attribute is the value the writer
 * wrapped for a property declared {@code Object}, read with that type. An empty element is the empty string for a
 * String or open slot. A structured item of an open list or array ({@code List<Object>},
 * a raw list) is typed by its own type attribute when that names a bean, a map or a container, otherwise by its
 * element name in the same way. In Abacus XML, container metadata on the {@code <list>} or {@code <array>} element
 * alone types the items as wrapper metadata does, and a bean-shaped element in an open slot whose name resolves to
 * no bean class is read as a map of its properties, as the standard readers read it.</p>
 *
 * <p>Compact JSON text accepts JSON string escapes and surrounding Unicode whitespace, for arrays, collections
 * and maps, at the root and inside properties, on every backend. A map written as compact text inside a list
 * element ({@code <e>{"a":[1,2]}</e>}) is read by every backend, and a map written as compact text at the root
 * ({@code <map>{"a":1}</map>}) is read with the root's key and value types, an explicit global type ahead of the
 * requested arguments, exactly as its child elements are. Primitive array elements use their scalar text
 * conversion after unescaping quoted tokens once; an unquoted {@code null} element becomes the primitive
 * default. Unicode whitespace between primitive tokens is also ignored. For example, {@code ["1", null, "65"]}
 * read as {@code char[]} yields {@code {'1', '\0', 'A'}}. Whitespace inside quoted string elements is preserved.</p>
 *
 * <p>A collection root with no items or only blank text ({@code <list/>}, {@code <list> </list>}) is an empty collection of
 * the requested type on every backend. A root of any type - bean, map, array, collection or scalar - marked {@code isNull="true"}
 * is {@code null} on every backend, whatever payload it carries, after its {@code type} attribute has been checked against
 * the type allowlist. A blank map, bean or {@code MapEntity} item of an array or collection ({@code <list><map></map></list>}, the standard
 * writer's empty structured item) is an empty value of that type on both standard backends; declared {@code MapEntity} elements also retain
 * their entity names and type in collection/array properties. A blank nested list or array item
 * written by hand ({@code <list><list/></list>}) is backend-specific on the standard parser (an empty container on its
 * StAX reader, {@code null} on its DOM reader); the writers emit {@code []} or {@code isNull="true"} for those.</p>
 *
 * <p><b>Note — intentional omission of the JSON-only deser knobs:</b> the
 * {@code setMapInstanceType} / {@code setReadNullToEmpty} / {@code setIgnoreNullOrEmpty}
 * options live on {@link JsonDeserConfig} on purpose: they are read only by the JSON
 * deserializer ({@code JsonParserImpl}). The XML deserializers ({@code XmlParserImpl} /
 * {@code AbacusXmlParserImpl}) do not consume them, so exposing them here would create
 * dead configuration. They are therefore deliberately NOT provided on {@code XmlDeserConfig}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * XmlDeserConfig config = new XmlDeserConfig()
 *     .setIgnoreUnmatchedProperty(true)
 *     .setElementType(String.class)
 *     .setMapKeyType(String.class)
 *     .setMapValueType(Integer.class);
 * }</pre>
 *
 * @see DeserializationConfig
 * @see XmlParser
 */
public class XmlDeserConfig extends DeserializationConfig<XmlDeserConfig> {

    /**
     * Creates a new XmlDeserConfig with default settings.
     *
     * <p>Inherits all default settings from {@link DeserializationConfig}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlDeserConfig config = new XmlDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setElementType(String.class);
     * }</pre>
     *
     */
    public XmlDeserConfig() {
        super();
    }

    /**
     * Creates a new XmlDeserConfig instance with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlDeserConfig config = XmlDeserConfig.create()
     *     .setMapKeyType(String.class)
     *     .setMapValueType(Integer.class);
     * }</pre>
     *
     * @return a new XmlDeserConfig instance
     */
    public static XmlDeserConfig create() {
        return new XmlDeserConfig();
    }

}
