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
 * writer.write(XmlConstants.ARRAY_ELE_START);
 * // ... write array elements
 * writer.write(XmlConstants.ARRAY_ELE_END);
 * }</pre>
 * 
 */
public class XmlConstants {

    /**
     * Protected constructor to prevent direct instantiation of this utility class.
     */
    protected XmlConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * Used as a special marker for {@code null} end elements in XML processing.
     * Value: {@code -128}.
     */
    public static final int NULL_END_ELEMENT = -128;

    /**
     * XML attribute name used to indicate {@code null} values.
     * Value: {@code "isNull"}.
     */
    public static final String IS_NULL = "isNull";

    /**
     * String representation of boolean {@code true} value in XML.
     * Value: {@code "true"}.
     */
    public static final String TRUE = "true";

    /**
     * String representation of boolean {@code false} value in XML.
     * Value: {@code "false"}.
     */
    public static final String FALSE = "false";

    /**
     * Pre-formatted XML attribute string for {@code null} values.
     * Value: {@code " isNull=\"true\""}.
     */
    public static final String IS_NULL_ATTR = " isNull=\"true\"";

    /**
     * XML attribute name for element names.
     * Value: {@code "name"}.
     */
    public static final String NAME = "name";

    /**
     * XML attribute name for type information.
     * Value: {@code "type"}.
     */
    public static final String TYPE = "type";

    /**
     * XML attribute name for version information.
     * Value: {@code "version"}.
     */
    public static final String VERSION = "version";

    /**
     * XML attribute name for size information.
     * Value: {@code "size"}.
     */
    public static final String SIZE = "size";

    /**
     * Self-closing XML element representing a {@code null} value.
     * Value: {@code "<null isNull=\"true\" />"}.
     */
    public static final String NULL_NULL_ELE = "<null isNull=\"true\" />";

    /**
     * XML element name for array structures.
     * Value: {@code "array"}.
     */
    public static final String ARRAY = "array";

    /**
     * Opening tag for array XML elements.
     * Value: {@code "<array>"}.
     */
    public static final String ARRAY_ELE_START = "<array>";

    /**
     * Closing tag for array XML elements.
     * Value: {@code "</array>"}.
     */
    public static final String ARRAY_ELE_END = "</array>";

    /**
     * Self-closing XML element representing a {@code null} array.
     * Value: {@code "<array isNull=\"true\" />"}.
     */
    public static final String ARRAY_NULL_ELE = "<array isNull=\"true\" />";

    /**
     * XML element name for list structures.
     * Value: {@code "list"}.
     */
    public static final String LIST = "list";

    /**
     * Opening tag for list XML elements.
     * Value: {@code "<list>"}.
     */
    public static final String LIST_ELE_START = "<list>";

    /**
     * Closing tag for list XML elements.
     * Value: {@code "</list>"}.
     */
    public static final String LIST_ELE_END = "</list>";

    /**
     * Self-closing XML element representing a {@code null} list.
     * Value: {@code "<list isNull=\"true\" />"}.
     */
    public static final String LIST_NULL_ELE = "<list isNull=\"true\" />";

    /**
     * XML element name for set structures.
     * Value: {@code "set"}.
     */
    public static final String SET = "set";

    /**
     * Opening tag for set XML elements.
     * Value: {@code "<set>"}.
     */
    public static final String SET_ELE_START = "<set>";

    /**
     * Closing tag for set XML elements.
     * Value: {@code "</set>"}.
     */
    public static final String SET_ELE_END = "</set>";

    /**
     * Self-closing XML element representing a {@code null} set.
     * Value: {@code "<set isNull=\"true\" />"}.
     */
    public static final String SET_NULL_ELE = "<set isNull=\"true\" />";

    /**
     * XML element name for generic collection structures.
     * Value: {@code "collection"}.
     */
    public static final String COLLECTION = "collection";

    /**
     * Opening tag for collection XML elements.
     * Value: {@code "<collection>"}.
     */
    public static final String COLLECTION_ELE_START = "<collection>";

    /**
     * Closing tag for collection XML elements.
     * Value: {@code "</collection>"}.
     */
    public static final String COLLECTION_ELE_END = "</collection>";

    /**
     * Self-closing XML element representing a {@code null} collection.
     * Value: {@code "<collection isNull=\"true\" />"}.
     */
    public static final String COLLECTION_NULL_ELE = "<collection isNull=\"true\" />";

    /**
     * XML element name for generic elements within collections.
     * Value: {@code "e"}.
     */
    public static final String E = "e";

    /**
     * Opening tag for generic element XML tags.
     * Value: {@code "<e>"}.
     */
    public static final String E_ELE_START = "<e>";

    /**
     * Closing tag for generic element XML tags.
     * Value: {@code "</e>"}.
     */
    public static final String E_ELE_END = "</e>";

    /**
     * Self-closing XML element representing a {@code null} element.
     * Value: {@code "<e isNull=\"true\" />"}.
     */
    public static final String E_NULL_ELE = "<e isNull=\"true\" />";

    /**
     * XML element name for map structures.
     * Value: {@code "map"}.
     */
    public static final String MAP = "map";

    /**
     * Opening tag for map XML elements.
     * Value: {@code "<map>"}.
     */
    public static final String MAP_ELE_START = "<map>";

    /**
     * Closing tag for map XML elements.
     * Value: {@code "</map>"}.
     */
    public static final String MAP_ELE_END = "</map>";

    /**
     * Self-closing XML element representing a {@code null} map.
     * Value: {@code "<map isNull=\"true\" />"}.
     */
    public static final String MAP_NULL_ELE = "<map isNull=\"true\" />";

    /**
     * XML element name for map entry structures.
     * Value: {@code "entry"}.
     */
    public static final String ENTRY = "entry";

    /**
     * Opening tag for map entry XML elements.
     * Value: {@code "<entry>"}.
     */
    public static final String ENTRY_ELE_START = "<entry>";

    /**
     * Closing tag for map entry XML elements.
     * Value: {@code "</entry>"}.
     */
    public static final String ENTRY_ELE_END = "</entry>";

    /**
     * XML element name for map key elements.
     * Value: {@code "key"}.
     */
    public static final String KEY = "key";

    /**
     * Opening tag for map key XML elements.
     * Value: {@code "<key>"}.
     */
    public static final String KEY_ELE_START = "<key>";

    /**
     * Closing tag for map key XML elements.
     * Value: {@code "</key>"}.
     */
    public static final String KEY_ELE_END = "</key>";

    /**
     * Self-closing XML element representing a {@code null} key.
     * Value: {@code "<key isNull=\"true\" />"}.
     */
    public static final String KEY_NULL_ELE = "<key isNull=\"true\" />";

    /**
     * XML element name for value elements.
     * Value: {@code "value"}.
     */
    public static final String VALUE = "value";

    /**
     * Opening tag for value XML elements.
     * Value: {@code "<value>"}.
     */
    public static final String VALUE_ELE_START = "<value>";

    /**
     * Closing tag for value XML elements.
     * Value: {@code "</value>"}.
     */
    public static final String VALUE_ELE_END = "</value>";

    /**
     * Self-closing XML element representing a {@code null} value.
     * Value: {@code "<value isNull=\"true\" />"}.
     */
    public static final String VALUE_NULL_ELE = "<value isNull=\"true\" />";

    /**
     * XML element name for entity identifiers.
     * Value: {@code "entityId"}.
     */
    public static final String ENTITY_ID = "entityId";

    /**
     * Opening tag for entity ID XML elements.
     * Value: {@code "<entityId>"}.
     */
    public static final String ENTITY_ID_ELE_START = "<entityId>";

    /**
     * Closing tag for entity ID XML elements.
     * Value: {@code "</entityId>"}.
     */
    public static final String ENTITY_ID_ELE_END = "</entityId>";

    /**
     * Self-closing XML element representing a {@code null} entity ID.
     * Value: {@code "<entityId isNull=\"true\" />"}.
     */
    public static final String ENTITY_ID_NULL_ELE = "<entityId isNull=\"true\" />";

    /**
     * XML element name for collections of entity identifiers.
     * Value: {@code "entityIds"}.
     */
    public static final String ENTITY_IDS = "entityIds";

    /**
     * Opening tag for entity IDs collection XML elements.
     * Value: {@code "<entityIds>"}.
     */
    public static final String ENTITY_IDS_ELE_START = "<entityIds>";

    /**
     * Closing tag for entity IDs collection XML elements.
     * Value: {@code "</entityIds>"}.
     */
    public static final String ENTITY_IDS_ELE_END = "</entityIds>";

    /**
     * Self-closing XML element representing a {@code null} entity IDs collection.
     * Value: {@code "<entityIds isNull=\"true\" />"}.
     */
    public static final String ENTITY_IDS_NULL_ELE = "<entityIds isNull=\"true\" />";

    /**
     * XML element name for entity/bean structures.
     * Value: {@code "bean"}.
     */
    public static final String ENTITY = "bean";

    /**
     * Opening tag for entity/bean XML elements.
     * Value: {@code "<bean>"}.
     */
    public static final String ENTITY_ELE_START = "<bean>";

    /**
     * Closing tag for entity/bean XML elements.
     * Value: {@code "</bean>"}.
     */
    public static final String ENTITY_ELE_END = "</bean>";

    /**
     * Self-closing XML element representing a {@code null} entity/bean.
     * Value: {@code "<bean isNull=\"true\" />"}.
     */
    public static final String ENTITY_NULL_ELE = "<bean isNull=\"true\" />";

    /**
     * XML element name for entity/bean names.
     * Value: {@code "beanName"}.
     */
    public static final String ENTITY_NAME = "beanName";

    /**
     * Opening tag for entity/bean name XML elements.
     * Value: {@code "<beanName>"}.
     */
    public static final String ENTITY_NAME_ELE_START = "<beanName>";

    /**
     * Closing tag for entity/bean name XML elements.
     * Value: {@code "</beanName>"}.
     */
    public static final String ENTITY_NAME_ELE_END = "</beanName>";

    /**
     * Self-closing XML element representing a {@code null} entity/bean name.
     * Value: {@code "<beanName isNull=\"true\" />"}.
     */
    public static final String ENTITY_NAME_NULL_ELE = "<beanName isNull=\"true\" />";

    /**
     * XML element name for entity/bean class information.
     * Value: {@code "beanClass"}.
     */
    public static final String ENTITY_CLASS = "beanClass";

    /**
     * Opening tag for entity/bean class XML elements.
     * Value: {@code "<beanClass>"}.
     */
    public static final String ENTITY_CLASS_ELE_START = "<beanClass>";

    /**
     * Closing tag for entity/bean class XML elements.
     * Value: {@code "</beanClass>"}.
     */
    public static final String ENTITY_CLASS_ELE_END = "</beanClass>";

    /**
     * Self-closing XML element representing a {@code null} entity/bean class.
     * Value: {@code "<beanClass isNull=\"true\" />"}.
     */
    public static final String ENTITY_CLASS_NULL_ELE = "<beanClass isNull=\"true\" />";

    /**
     * XML element name for property elements.
     * Value: {@code "property"}.
     */
    public static final String PROPERTY = "property";

    /**
     * Opening tag for property XML elements.
     * Value: {@code "<property>"}.
     */
    public static final String PROPERTY_ELE_START = "<property>";

    /**
     * Closing tag for property XML elements.
     * Value: {@code "</property>"}.
     */
    public static final String PROPERTY_ELE_END = "</property>";

    /**
     * Self-closing XML element representing a {@code null} property.
     * Value: {@code "<property isNull=\"true\" />"}.
     */
    public static final String PROPERTY_NULL_ELE = "<property isNull=\"true\" />";

    /**
     * XML element name for collections of properties.
     * Value: {@code "properties"}.
     */
    public static final String PROPERTIES = "properties";

    /**
     * Opening tag for properties collection XML elements.
     * Value: {@code "<properties>"}.
     */
    public static final String PROPERTIES_ELE_START = "<properties>";

    /**
     * Closing tag for properties collection XML elements.
     * Value: {@code "</properties>"}.
     */
    public static final String PROPERTIES_ELE_END = "</properties>";

    /**
     * Self-closing XML element representing a {@code null} properties collection.
     * Value: {@code "<properties isNull=\"true\" />"}.
     */
    public static final String PROPERTIES_NULL_ELE = "<properties isNull=\"true\" />";

    /**
     * XML element name for abbreviated property elements.
     * Value: {@code "prop"}.
     */
    public static final String PROP = "prop";

    /**
     * Opening tag for abbreviated property XML elements.
     * Value: {@code "<prop>"}.
     */
    public static final String PROP_ELE_START = "<prop>";

    /**
     * Closing tag for abbreviated property XML elements.
     * Value: {@code "</prop>"}.
     */
    public static final String PROP_ELE_END = "</prop>";

    /**
     * Self-closing XML element representing a {@code null} abbreviated property.
     * Value: {@code "<prop isNull=\"true\" />"}.
     */
    public static final String PROP_NULL_ELE = "<prop isNull=\"true\" />";

    /**
     * XML element name for property names.
     * Value: {@code "propName"}.
     */
    public static final String PROP_NAME = "propName";

    /**
     * Opening tag for property name XML elements.
     * Value: {@code "<propName>"}.
     */
    public static final String PROP_NAME_ELE_START = "<propName>";

    /**
     * Closing tag for property name XML elements.
     * Value: {@code "</propName>"}.
     */
    public static final String PROP_NAME_ELE_END = "</propName>";

    /**
     * Self-closing XML element representing a {@code null} property name.
     * Value: {@code "<propName isNull=\"true\" />"}.
     */
    public static final String PROP_NAME_NULL_ELE = "<propName isNull=\"true\" />";

    /**
     * XML element name for property values.
     * Value: {@code "propValue"}.
     */
    public static final String PROP_VALUE = "propValue";

    /**
     * Opening tag for property value XML elements.
     * Value: {@code "<propValue>"}.
     */
    public static final String PROP_VALUE_ELE_START = "<propValue>";

    /**
     * Closing tag for property value XML elements.
     * Value: {@code "</propValue>"}.
     */
    public static final String PROP_VALUE_ELE_END = "</propValue>";

    /**
     * Self-closing XML element representing a {@code null} property value.
     * Value: {@code "<propValue isNull=\"true\" />"}.
     */
    public static final String PROP_VALUE_NULL_ELE = "<propValue isNull=\"true\" />";

    /**
     * XML element name for properties collections.
     * Value: {@code "props"}.
     */
    public static final String PROPS = "props";

    /**
     * Opening tag for properties collection XML elements.
     * Value: {@code "<props>"}.
     */
    public static final String PROPS_ELE_START = "<props>";

    /**
     * Closing tag for properties collection XML elements.
     * Value: {@code "</props>"}.
     */
    public static final String PROPS_ELE_END = "</props>";

    /**
     * Self-closing XML element representing a {@code null} properties collection.
     * Value: {@code "<props isNull=\"true\" />"}.
     */
    public static final String PROPS_NULL_ELE = "<props isNull=\"true\" />";

    /**
     * XML element name for lists of properties.
     * Value: {@code "propsList"}.
     */
    public static final String PROPS_LIST = "propsList";

    /**
     * Opening tag for properties list XML elements.
     * Value: {@code "<propsList>"}.
     */
    public static final String PROPS_LIST_ELE_START = "<propsList>";

    /**
     * Closing tag for properties list XML elements.
     * Value: {@code "</propsList>"}.
     */
    public static final String PROPS_LIST_ELE_END = "</propsList>";

    /**
     * Self-closing XML element representing a {@code null} properties list.
     * Value: {@code "<propsList isNull=\"true\" />"}.
     */
    public static final String PROPS_LIST_NULL_ELE = "<propsList isNull=\"true\" />";

    /**
     * XML element name for collections of entities.
     * Value: {@code "entities"}.
     */
    public static final String ENTITIES = "entities";

    /**
     * Opening tag for entities collection XML elements.
     * Value: {@code "<entities>"}.
     */
    public static final String ENTITIES_ELE_START = "<entities>";

    /**
     * Closing tag for entities collection XML elements.
     * Value: {@code "</entities>"}.
     */
    public static final String ENTITIES_ELE_END = "</entities>";

    /**
     * Self-closing XML element representing a {@code null} entities collection.
     * Value: {@code "<entities isNull=\"true\" />"}.
     */
    public static final String ENTITIES_NULL_ELE = "<entities isNull=\"true\" />";

    /**
     * XML element name for result elements.
     * Value: {@code "result"}.
     */
    public static final String RESULT = "result";

    /**
     * Opening tag for result XML elements.
     * Value: {@code "<result>"}.
     */
    public static final String RESULT_ELE_START = "<result>";

    /**
     * Closing tag for result XML elements.
     * Value: {@code "</result>"}.
     */
    public static final String RESULT_ELE_END = "</result>";

    /**
     * Self-closing XML element representing a {@code null} result.
     * Value: {@code "<result isNull=\"true\" />"}.
     */
    public static final String RESULT_NULL_ELE = "<result isNull=\"true\" />";

    /**
     * XML element name for data set structures.
     * Value: {@code "dataset"}.
     */
    public static final String DATA_SET = "dataset";

    /**
     * Opening tag for data set XML elements.
     * Value: {@code "<dataset>"}.
     */
    public static final String DATA_SET_ELE_START = "<dataset>";

    /**
     * Closing tag for data set XML elements.
     * Value: {@code "</dataset>"}.
     */
    public static final String DATA_SET_ELE_END = "</dataset>";

    /**
     * Self-closing XML element representing a {@code null} data set.
     * Value: {@code "<dataset isNull=\"true\" />"}.
     */
    public static final String DATA_SET_NULL_ELE = "<dataset isNull=\"true\" />";

    /**
     * XML element name for SOAP envelope elements.
     * Value: {@code "Envelope"}.
     */
    public static final String ENVELOPE = "Envelope";

    /**
     * Opening tag for SOAP envelope XML elements with namespace declaration.
     * Value: {@code "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"}.
     */
    public static final String SOAP_ENVELOPE_ELE_START = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">";

    /**
     * Closing tag for SOAP envelope XML elements.
     * Value: {@code "</soap:Envelope>"}.
     */
    public static final String SOAP_ENVELOPE_ELE_END = "</soap:Envelope>";

    /**
     * XML element name for SOAP header elements.
     * Value: {@code "Header"}.
     */
    public static final String HEADER = "Header";

    /**
     * Opening tag for SOAP header XML elements.
     * Value: {@code "<soap:Header>"}.
     */
    public static final String SOAP_HEADER_ELE_START = "<soap:Header>";

    /**
     * Closing tag for SOAP header XML elements.
     * Value: {@code "</soap:Header>"}.
     */
    public static final String SOAP_HEADER_ELE_END = "</soap:Header>";

    /**
     * Constant representing the body element name in uppercase. Note that the actual SOAP body
     * element tags use {@code "Body"} (see {@link #SOAP_BODY_ELE_START}).
     * Value: {@code "BODY"}.
     */
    public static final String BODY = "BODY";

    /**
     * Opening tag for SOAP body XML elements.
     * Value: {@code "<soap:Body>"}.
     */
    public static final String SOAP_BODY_ELE_START = "<soap:Body>";

    /**
     * Closing tag for SOAP body XML elements.
     * Value: {@code "</soap:Body>"}.
     */
    public static final String SOAP_BODY_ELE_END = "</soap:Body>";

    /**
     * XML element name for SOAP fault elements.
     * Value: {@code "Fault"}.
     */
    public static final String FAULT = "Fault";

    /**
     * Opening tag for SOAP fault XML elements.
     * Value: {@code "<soap:Fault>"}.
     */
    public static final String SOAP_FAULT_ELE_START = "<soap:Fault>";

    /**
     * Closing tag for SOAP fault XML elements.
     * Value: {@code "</soap:Fault>"}.
     */
    public static final String SOAP_FAULT_ELE_END = "</soap:Fault>";

    /**
     * Partial opening tag for typed array XML elements.
     * Value: {@code "<array type=\""}.
     */
    static final char[] START_ARRAY_ELE_WITH_TYPE = "<array type=\"".toCharArray();

    /**
     * Partial opening tag for typed list XML elements.
     * Value: {@code "<list type=\""}.
     */
    static final char[] START_LIST_ELE_WITH_TYPE = "<list type=\"".toCharArray();

    /**
     * Partial opening tag for typed set XML elements.
     * Value: {@code "<set type=\""}.
     */
    static final char[] START_SET_ELE_WITH_TYPE = "<set type=\"".toCharArray();

    /**
     * Partial opening tag for typed collection XML elements.
     * Value: {@code "<collection type=\""}.
     */
    static final char[] START_COLLECTION_ELE_WITH_TYPE = "<collection type=\"".toCharArray();

    /**
     * Partial opening tag for typed element XML tags.
     * Value: {@code "<e type=\""}.
     */
    static final char[] START_E_ELE_WITH_TYPE = "<e type=\"".toCharArray();

    /**
     * Partial opening tag for typed map XML elements.
     * Value: {@code "<map type=\""}.
     */
    static final char[] START_MAP_ELE_WITH_TYPE = "<map type=\"".toCharArray();

    /**
     * Partial opening tag for typed key XML elements.
     * Value: {@code "<key type=\""}.
     */
    static final char[] START_KEY_ELE_WITH_TYPE = "<key type=\"".toCharArray();

    /**
     * Opening tag for string-typed key XML elements.
     * Value: {@code "<key type=\"String\">"}.
     */
    static final char[] START_KEY_ELE_WITH_STRING_TYPE = "<key type=\"String\">".toCharArray();

    /**
     * Partial opening tag for typed value XML elements.
     * Value: {@code "<value type=\""}.
     */
    static final char[] START_VALUE_ELE_WITH_TYPE = "<value type=\"".toCharArray();

    /**
     * XML attribute fragment for type specification.
     * Value: {@code " type=\""}.
     */
    static final char[] START_TYPE_ATTR = " type=\"".toCharArray();

    /**
     * Character sequence to close an attribute and open an element.
     * Value: {@code "\">"}.
     */
    static final char[] CLOSE_ATTR_AND_ELE = "\">".toCharArray();

    /**
     * Character sequence for self-closing XML elements.
     * Value: {@code " />"}.
     */
    static final char[] END_ELEMENT = " />".toCharArray();
}
