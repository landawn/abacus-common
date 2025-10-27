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
 * Constants used for XML parsing and serialization operations.
 * This class contains predefined XML element names, attributes, and tags used throughout
 * the XML processing framework.
 * 
 * <p>The constants are organized into categories:
 * <ul>
 *   <li>Basic XML attributes and values (IS_NULL, TRUE, FALSE, etc.)</li>
 *   <li>Collection-related elements (ARRAY, LIST, SET, etc.)</li>
 *   <li>Map-related elements (MAP, ENTRY, KEY, VALUE, etc.)</li>
 *   <li>Entity and property elements (ENTITY, PROPERTY, etc.)</li>
 *   <li>SOAP envelope elements</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Using constants for XML serialization
 * writer.write(XMLConstants.ARRAY_ELE_START);
 * // ... write array elements
 * writer.write(XMLConstants.ARRAY_ELE_END);
 * }</pre>
 * 
 * @since 1.0
 */
public class XMLConstants {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    protected XMLConstants() {
        // singleton.
    }

    /**
     * Field NULL_END_ELEMENT (value is {@code -128})
     * Used as a special marker for {@code null} end elements in XML processing.
     */
    public static final int NULL_END_ELEMENT = -128;

    /**
     * Field IS_NULL (value is {@code "isNull"})
     * XML attribute name used to indicate {@code null} values.
     */
    public static final String IS_NULL = "isNull";

    /**
     * Field TRUE (value is {@code "true"})
     * String representation of boolean {@code true} value in XML.
     */
    public static final String TRUE = "true";

    /**
     * Field FALSE (value is {@code "false"})
     * String representation of boolean {@code false} value in XML.
     */
    public static final String FALSE = "false";

    /**
     * Field IS_NULL_ATTR (value is {@code " isNull=\"true\""})
     * Pre-formatted XML attribute string for {@code null} values.
     */
    public static final String IS_NULL_ATTR = " isNull=\"true\"";

    /**
     * Field NAME (value is {@code "name"})
     * XML attribute name for element names.
     */
    public static final String NAME = "name";

    /**
     * Field TYPE (value is {@code "type"})
     * XML attribute name for type information.
     */
    public static final String TYPE = "type";

    /**
     * Field VERSION (value is {@code "version"})
     * XML attribute name for version information.
     */
    public static final String VERSION = "version";

    /**
     * Field SIZE (value is {@code "size"})
     * XML attribute name for size information.
     */
    public static final String SIZE = "size";

    /**
     * Field NULL_NULL_ELE (value is {@code "<null isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} value.
     */
    public static final String NULL_NULL_ELE = "<null isNull=\"true\" />";

    /**
     * Field ARRAY (value is {@code "array"})
     * XML element name for array structures.
     */
    public static final String ARRAY = "array";

    /**
     * Field ARRAY_ELE_START (value is {@code "<array>"})
     * Opening tag for array XML elements.
     */
    public static final String ARRAY_ELE_START = "<array>";

    /**
     * Field ARRAY_ELE_END (value is {@code "</array>"})
     * Closing tag for array XML elements.
     */
    public static final String ARRAY_ELE_END = "</array>";

    /**
     * Field ARRAY_NULL_ELE (value is {@code "<array isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} array.
     */
    public static final String ARRAY_NULL_ELE = "<array isNull=\"true\" />";

    /**
     * Field LIST (value is {@code "list"})
     * XML element name for list structures.
     */
    public static final String LIST = "list";

    /**
     * Field LIST_ELE_START (value is {@code "<list>"})
     * Opening tag for list XML elements.
     */
    public static final String LIST_ELE_START = "<list>";

    /**
     * Field LIST_ELE_END (value is {@code "</list>"})
     * Closing tag for list XML elements.
     */
    public static final String LIST_ELE_END = "</list>";

    /**
     * Field LIST_NULL_ELE (value is {@code "<list isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} list.
     */
    public static final String LIST_NULL_ELE = "<list isNull=\"true\" />";

    /**
     * Field SET (value is {@code "set"})
     * XML element name for set structures.
     */
    public static final String SET = "set";

    /**
     * Field SET_ELE_START (value is {@code "<set>"})
     * Opening tag for set XML elements.
     */
    public static final String SET_ELE_START = "<set>";

    /**
     * Field SET_ELE_END (value is {@code "</set>"})
     * Closing tag for set XML elements.
     */
    public static final String SET_ELE_END = "</set>";

    /**
     * Field SET_NULL_ELE (value is {@code "<set isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} set.
     */
    public static final String SET_NULL_ELE = "<set isNull=\"true\" />";

    /**
     * Field COLLECTION (value is {@code "collection"})
     * XML element name for generic collection structures.
     */
    public static final String COLLECTION = "collection";

    /**
     * Field COLLECTION_ELE_START (value is {@code "<collection>"})
     * Opening tag for collection XML elements.
     */
    public static final String COLLECTION_ELE_START = "<collection>";

    /**
     * Field COLLECTION_ELE_END (value is {@code "</collection>"})
     * Closing tag for collection XML elements.
     */
    public static final String COLLECTION_ELE_END = "</collection>";

    /**
     * Field COLLECTION_NULL_ELE (value is {@code "<collection isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} collection.
     */
    public static final String COLLECTION_NULL_ELE = "<collection isNull=\"true\" />";

    /**
     * Field E (value is {@code "e"})
     * XML element name for generic elements within collections.
     */
    public static final String E = "e";

    /**
     * Field E_ELE_START (value is {@code "<e>"})
     * Opening tag for generic element XML tags.
     */
    public static final String E_ELE_START = "<e>";

    /**
     * Field E_ELE_END (value is {@code "</e>"})
     * Closing tag for generic element XML tags.
     */
    public static final String E_ELE_END = "</e>";

    /**
     * Field E_NULL_ELE (value is {@code "<e isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} element.
     */
    public static final String E_NULL_ELE = "<e isNull=\"true\" />";

    /**
     * Field MAP (value is {@code "map"})
     * XML element name for map structures.
     */
    public static final String MAP = "map";

    /**
     * Field MAP_ELE_START (value is {@code "<map>"})
     * Opening tag for map XML elements.
     */
    public static final String MAP_ELE_START = "<map>";

    /**
     * Field MAP_ELE_END (value is {@code "</map>"})
     * Closing tag for map XML elements.
     */
    public static final String MAP_ELE_END = "</map>";

    /**
     * Field MAP_NULL_ELE (value is {@code "<map isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} map.
     */
    public static final String MAP_NULL_ELE = "<map isNull=\"true\" />";

    /**
     * Field ENTRY (value is {@code "entry"})
     * XML element name for map entry structures.
     */
    public static final String ENTRY = "entry";

    /**
     * Field ENTRY_ELE_START (value is {@code "<entry>"})
     * Opening tag for map entry XML elements.
     */
    public static final String ENTRY_ELE_START = "<entry>";

    /**
     * Field ENTRY_ELE_END (value is {@code "</entry>"})
     * Closing tag for map entry XML elements.
     */
    public static final String ENTRY_ELE_END = "</entry>";

    /**
     * Field KEY (value is {@code "key"})
     * XML element name for map key elements.
     */
    public static final String KEY = "key";

    /**
     * Field KEY_ELE_START (value is {@code "<key>"})
     * Opening tag for map key XML elements.
     */
    public static final String KEY_ELE_START = "<key>";

    /**
     * Field KEY_ELE_END (value is {@code "</key>"})
     * Closing tag for map key XML elements.
     */
    public static final String KEY_ELE_END = "</key>";

    /**
     * Field KEY_NULL_ELE (value is {@code "<key isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} key.
     */
    public static final String KEY_NULL_ELE = "<key isNull=\"true\" />";

    /**
     * Field VALUE (value is {@code "value"})
     * XML element name for value elements.
     */
    public static final String VALUE = "value";

    /**
     * Field VALUE_ELE_START (value is {@code "<value>"})
     * Opening tag for value XML elements.
     */
    public static final String VALUE_ELE_START = "<value>";

    /**
     * Field VALUE_ELE_END (value is {@code "</value>"})
     * Closing tag for value XML elements.
     */
    public static final String VALUE_ELE_END = "</value>";

    /**
     * Field VALUE_NULL_ELE (value is {@code "<value isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} value.
     */
    public static final String VALUE_NULL_ELE = "<value isNull=\"true\" />";

    /**
     * Field ENTITY_ID (value is {@code "entityId"})
     * XML element name for entity identifiers.
     */
    public static final String ENTITY_ID = "entityId";

    /**
     * Field ENTITY_ID_ELE_START (value is {@code "<entityId>"})
     * Opening tag for entity ID XML elements.
     */
    public static final String ENTITY_ID_ELE_START = "<entityId>";

    /**
     * Field ENTITY_ID_ELE_END (value is {@code "</entityId>"})
     * Closing tag for entity ID XML elements.
     */
    public static final String ENTITY_ID_ELE_END = "</entityId>";

    /**
     * Field ENTITY_ID_NULL_ELE (value is {@code "<entityId isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} entity ID.
     */
    public static final String ENTITY_ID_NULL_ELE = "<entityId isNull=\"true\" />";

    /**
     * Field ENTITY_IDS (value is {@code "entityIds"})
     * XML element name for collections of entity identifiers.
     */
    public static final String ENTITY_IDS = "entityIds";

    /**
     * Field ENTITY_IDS_ELE_START (value is {@code "<entityIds>"})
     * Opening tag for entity IDs collection XML elements.
     */
    public static final String ENTITY_IDS_ELE_START = "<entityIds>";

    /**
     * Field ENTITY_IDS_ELE_END (value is {@code "</entityIds>"})
     * Closing tag for entity IDs collection XML elements.
     */
    public static final String ENTITY_IDS_ELE_END = "</entityIds>";

    /**
     * Field ENTITY_IDS_NULL_ELE (value is {@code "<entityIds isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} entity IDs collection.
     */
    public static final String ENTITY_IDS_NULL_ELE = "<entityIds isNull=\"true\" />";

    /**
     * Field ENTITY (value is {@code "bean"})
     * XML element name for entity/bean structures.
     */
    public static final String ENTITY = "bean";

    /**
     * Field ENTITY_ELE_START (value is {@code "<bean>"})
     * Opening tag for entity/bean XML elements.
     */
    public static final String ENTITY_ELE_START = "<bean>";

    /**
     * Field ENTITY_ELE_END (value is {@code "</bean>"})
     * Closing tag for entity/bean XML elements.
     */
    public static final String ENTITY_ELE_END = "</bean>";

    /**
     * Field ENTITY_NULL_ELE (value is {@code "<bean isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} entity/bean.
     */
    public static final String ENTITY_NULL_ELE = "<bean isNull=\"true\" />";

    /**
     * Field ENTITY_NAME (value is {@code "beanName"})
     * XML element name for entity/bean names.
     */
    public static final String ENTITY_NAME = "beanName";

    /**
     * Field ENTITY_NAME_ELE_START (value is {@code "<beanName>"})
     * Opening tag for entity/bean name XML elements.
     */
    public static final String ENTITY_NAME_ELE_START = "<beanName>";

    /**
     * Field ENTITY_NAME_ELE_END (value is {@code "</beanName>"})
     * Closing tag for entity/bean name XML elements.
     */
    public static final String ENTITY_NAME_ELE_END = "</beanName>";

    /**
     * Field ENTITY_NAME_NULL_ELE (value is {@code "<beanName isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} entity/bean name.
     */
    public static final String ENTITY_NAME_NULL_ELE = "<beanName isNull=\"true\" />";

    /**
     * Field ENTITY_CLASS (value is {@code "beanClass"})
     * XML element name for entity/bean class information.
     */
    public static final String ENTITY_CLASS = "beanClass";

    /**
     * Field ENTITY_CLASS_ELE_START (value is {@code "<beanClass>"})
     * Opening tag for entity/bean class XML elements.
     */
    public static final String ENTITY_CLASS_ELE_START = "<beanClass>";

    /**
     * Field ENTITY_CLASS_ELE_END (value is {@code "</beanClass>"})
     * Closing tag for entity/bean class XML elements.
     */
    public static final String ENTITY_CLASS_ELE_END = "</beanClass>";

    /**
     * Field ENTITY_CLASS_NULL_ELE (value is {@code "<beanClass isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} entity/bean class.
     */
    public static final String ENTITY_CLASS_NULL_ELE = "<beanClass isNull=\"true\" />";

    /**
     * Field PROPERTY (value is {@code "property"})
     * XML element name for property elements.
     */
    public static final String PROPERTY = "property";

    /**
     * Field PROPERTY_ELE_START (value is {@code "<property>"})
     * Opening tag for property XML elements.
     */
    public static final String PROPERTY_ELE_START = "<property>";

    /**
     * Field PROPERTY_ELE_END (value is {@code "</property>"})
     * Closing tag for property XML elements.
     */
    public static final String PROPERTY_ELE_END = "</property>";

    /**
     * Field PROPERTY_NULL_ELE (value is {@code "<property isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} property.
     */
    public static final String PROPERTY_NULL_ELE = "<property isNull=\"true\" />";

    /**
     * Field PROPERTIES (value is {@code "properties"})
     * XML element name for collections of properties.
     */
    public static final String PROPERTIES = "properties";

    /**
     * Field PROPERTIES_ELE_START (value is {@code "<properties>"})
     * Opening tag for properties collection XML elements.
     */
    public static final String PROPERTIES_ELE_START = "<properties>";

    /**
     * Field PROPERTIES_ELE_END (value is {@code "</properties>"})
     * Closing tag for properties collection XML elements.
     */
    public static final String PROPERTIES_ELE_END = "</properties>";

    /**
     * Field PROPERTIES_NULL_ELE (value is {@code "<properties isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} properties collection.
     */
    public static final String PROPERTIES_NULL_ELE = "<properties isNull=\"true\" />";

    /**
     * Field PROP (value is {@code "prop"})
     * XML element name for abbreviated property elements.
     */
    public static final String PROP = "prop";

    /**
     * Field PROP_ELE_START (value is {@code "<prop>"})
     * Opening tag for abbreviated property XML elements.
     */
    public static final String PROP_ELE_START = "<prop>";

    /**
     * Field PROP_ELE_END (value is {@code "</prop>"})
     * Closing tag for abbreviated property XML elements.
     */
    public static final String PROP_ELE_END = "</prop>";

    /**
     * Field PROP_NULL_ELE (value is {@code "<prop isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} abbreviated property.
     */
    public static final String PROP_NULL_ELE = "<prop isNull=\"true\" />";

    /**
     * Field PROP_NAME (value is {@code "propName"})
     * XML element name for property names.
     */
    public static final String PROP_NAME = "propName";

    /**
     * Field PROP_NAME_ELE_START (value is {@code "<propName>"})
     * Opening tag for property name XML elements.
     */
    public static final String PROP_NAME_ELE_START = "<propName>";

    /**
     * Field PROP_NAME_ELE_END (value is {@code "</propName>"})
     * Closing tag for property name XML elements.
     */
    public static final String PROP_NAME_ELE_END = "</propName>";

    /**
     * Field PROP_NAME_NULL_ELE (value is {@code "<propName isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} property name.
     */
    public static final String PROP_NAME_NULL_ELE = "<propName isNull=\"true\" />";

    /**
     * Field PROP_VALUE (value is {@code "propValue"})
     * XML element name for property values.
     */
    public static final String PROP_VALUE = "propValue";

    /**
     * Field PROP_VALUE_ELE_START (value is {@code "<propValue>"})
     * Opening tag for property value XML elements.
     */
    public static final String PROP_VALUE_ELE_START = "<propValue>";

    /**
     * Field PROP_VALUE_ELE_END (value is {@code "</propValue>"})
     * Closing tag for property value XML elements.
     */
    public static final String PROP_VALUE_ELE_END = "</propValue>";

    /**
     * Field PROP_VALUE_NULL_ELE (value is {@code "<propValue isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} property value.
     */
    public static final String PROP_VALUE_NULL_ELE = "<propValue isNull=\"true\" />";

    /**
     * Field PROPS (value is {@code "props"})
     * XML element name for properties collections.
     */
    public static final String PROPS = "props";

    /**
     * Field PROPS_ELE_START (value is {@code "<props>"})
     * Opening tag for properties collection XML elements.
     */
    public static final String PROPS_ELE_START = "<props>";

    /**
     * Field PROPS_ELE_END (value is {@code "</props>"})
     * Closing tag for properties collection XML elements.
     */
    public static final String PROPS_ELE_END = "</props>";

    /**
     * Field PROPS_NULL_ELE (value is {@code "<props isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} properties collection.
     */
    public static final String PROPS_NULL_ELE = "<props isNull=\"true\" />";

    /**
     * Field PROPS_LIST (value is {@code "propsList"})
     * XML element name for lists of properties.
     */
    public static final String PROPS_LIST = "propsList";

    /**
     * Field PROPS_LIST_ELE_START (value is {@code "<propsList>"})
     * Opening tag for properties list XML elements.
     */
    public static final String PROPS_LIST_ELE_START = "<propsList>";

    /**
     * Field PROPS_LIST_ELE_END (value is {@code "</propsList>"})
     * Closing tag for properties list XML elements.
     */
    public static final String PROPS_LIST_ELE_END = "</propsList>";

    /**
     * Field PROPS_LIST_NULL_ELE (value is {@code "<propsList isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} properties list.
     */
    public static final String PROPS_LIST_NULL_ELE = "<propsList isNull=\"true\" />";

    /**
     * Field ENTITIES (value is {@code "entities"})
     * XML element name for collections of entities.
     */
    public static final String ENTITIES = "entities";

    /**
     * Field ENTITIES_ELE_START (value is {@code "<entities>"})
     * Opening tag for entities collection XML elements.
     */
    public static final String ENTITIES_ELE_START = "<entities>";

    /**
     * Field ENTITIES_ELE_END (value is {@code "</entities>"})
     * Closing tag for entities collection XML elements.
     */
    public static final String ENTITIES_ELE_END = "</entities>";

    /**
     * Field ENTITIES_NULL_ELE (value is {@code "<entities isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} entities collection.
     */
    public static final String ENTITIES_NULL_ELE = "<entities isNull=\"true\" />";

    /**
     * Field RESULT (value is {@code "result"})
     * XML element name for result elements.
     */
    public static final String RESULT = "result";

    /**
     * Field RESULT_ELE_START (value is {@code "<result>"})
     * Opening tag for result XML elements.
     */
    public static final String RESULT_ELE_START = "<result>";

    /**
     * Field RESULT_ELE_END (value is {@code "</result>"})
     * Closing tag for result XML elements.
     */
    public static final String RESULT_ELE_END = "</result>";

    /**
     * Field RESULT_NULL_ELE (value is {@code "<result isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} result.
     */
    public static final String RESULT_NULL_ELE = "<result isNull=\"true\" />";

    /**
     * Field DATA_SET (value is {@code "dataset"})
     * XML element name for data set structures.
     */
    public static final String DATA_SET = "dataset";

    /**
     * Field DATA_SET_ELE_START (value is {@code "<dataset>"})
     * Opening tag for data set XML elements.
     */
    public static final String DATA_SET_ELE_START = "<dataset>";

    /**
     * Field DATA_SET_ELE_END (value is {@code "</dataset>"})
     * Closing tag for data set XML elements.
     */
    public static final String DATA_SET_ELE_END = "</dataset>";

    /**
     * Field DATA_SET_NULL_ELE (value is {@code "<dataset isNull=\"true\" />"})
     * Self-closing XML element representing a {@code null} data set.
     */
    public static final String DATA_SET_NULL_ELE = "<dataset isNull=\"true\" />";

    /**
     * Field ENVELOPE (value is {@code "Envelope"})
     * XML element name for SOAP envelope elements.
     */
    public static final String ENVELOPE = "Envelope";

    /**
     * Field SOAP_ENVELOPE_ELE_START (value is {@code "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"})
     * Opening tag for SOAP envelope XML elements with namespace declaration.
     */
    public static final String SOAP_ENVELOPE_ELE_START = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">";

    /**
     * Field SOAP_ENVELOPE_ELE_END (value is {@code "</soap:Envelope>"})
     * Closing tag for SOAP envelope XML elements.
     */
    public static final String SOAP_ENVELOPE_ELE_END = "</soap:Envelope>";

    /**
     * Field HEADER (value is {@code "Header"})
     * XML element name for SOAP header elements.
     */
    public static final String HEADER = "Header";

    /**
     * Field SOAP_HEADER_ELE_START (value is {@code "<soap:Header>"})
     * Opening tag for SOAP header XML elements.
     */
    public static final String SOAP_HEADER_ELE_START = "<soap:Header>";

    /**
     * Field SOAP_HEADER_ELE_END (value is {@code "</soap:Header>"})
     * Closing tag for SOAP header XML elements.
     */
    public static final String SOAP_HEADER_ELE_END = "</soap:Header>";

    /**
     * Field BODY (value is {@code "BODY"})
     * XML element name for SOAP body elements.
     */
    public static final String BODY = "BODY";

    /**
     * Field SOAP_BODY_ELE_START (value is {@code "<soap:Body>"})
     * Opening tag for SOAP body XML elements.
     */
    public static final String SOAP_BODY_ELE_START = "<soap:Body>";

    /**
     * Field SOAP_BODY_ELE_END (value is {@code "</soap:Body>"})
     * Closing tag for SOAP body XML elements.
     */
    public static final String SOAP_BODY_ELE_END = "</soap:Body>";

    /**
     * Field FAULT (value is {@code "Fault"})
     * XML element name for SOAP fault elements.
     */
    public static final String FAULT = "Fault";

    /**
     * Field SOAP_FAULT_ELE_START (value is {@code "<soap:Fault>"})
     * Opening tag for SOAP fault XML elements.
     */
    public static final String SOAP_FAULT_ELE_START = "<soap:Fault>";

    /**
     * Field SOAP_FAULT_ELE_END (value is {@code "</soap:Fault>"})
     * Closing tag for SOAP fault XML elements.
     */
    public static final String SOAP_FAULT_ELE_END = "</soap:Fault>";

    /**
     * Field START_ARRAY_ELE_WITH_TYPE (value is {@code "<array type=\""})
     * Partial opening tag for typed array XML elements.
     */
    static final char[] START_ARRAY_ELE_WITH_TYPE = "<array type=\"".toCharArray();

    /**
     * Field START_LIST_ELE_WITH_TYPE (value is {@code "<list type=\""})
     * Partial opening tag for typed list XML elements.
     */
    static final char[] START_LIST_ELE_WITH_TYPE = "<list type=\"".toCharArray();

    /**
     * Field START_SET_ELE_WITH_TYPE (value is {@code "<set type=\""})
     * Partial opening tag for typed set XML elements.
     */
    static final char[] START_SET_ELE_WITH_TYPE = "<set type=\"".toCharArray();

    /**
     * Field START_COLLECTION_ELE_WITH_TYPE (value is {@code "<collection type=\""})
     * Partial opening tag for typed collection XML elements.
     */
    static final char[] START_COLLECTION_ELE_WITH_TYPE = "<collection type=\"".toCharArray();

    /**
     * Field START_E_ELE_WITH_TYPE (value is {@code "<e type=\""})
     * Partial opening tag for typed element XML tags.
     */
    static final char[] START_E_ELE_WITH_TYPE = "<e type=\"".toCharArray();

    /**
     * Field START_MAP_ELE_WITH_TYPE (value is {@code "<map type=\""})
     * Partial opening tag for typed map XML elements.
     */
    static final char[] START_MAP_ELE_WITH_TYPE = "<map type=\"".toCharArray();

    /**
     * Field START_KEY_ELE_WITH_TYPE (value is {@code "<key type=\""})
     * Partial opening tag for typed key XML elements.
     */
    static final char[] START_KEY_ELE_WITH_TYPE = "<key type=\"".toCharArray();

    /**
     * Field START_KEY_ELE_WITH_STRING_TYPE (value is {@code "<key type=\"String\">"})
     * Opening tag for string-typed key XML elements.
     */
    static final char[] START_KEY_ELE_WITH_STRING_TYPE = "<key type=\"String\">".toCharArray();

    /**
     * Field START_VALUE_ELE_WITH_TYPE (value is {@code "<value type=\""})
     * Partial opening tag for typed value XML elements.
     */
    static final char[] START_VALUE_ELE_WITH_TYPE = "<value type=\"".toCharArray();

    /**
     * Field START_TYPE_ATTR (value is {@code " type=\""})
     * XML attribute fragment for type specification.
     */
    static final char[] START_TYPE_ATTR = " type=\"".toCharArray();

    /**
     * Field CLOSE_ATTR_AND_ELE (value is {@code "\">"})
     * Character sequence to close an attribute and open an element.
     */
    static final char[] CLOSE_ATTR_AND_ELE = "\">".toCharArray();

    /**
     * Field END_ELEMENT (value is {@code " />"})
     * Character sequence for self-closing XML elements.
     */
    static final char[] END_ELEMENT = " />".toCharArray();
}
