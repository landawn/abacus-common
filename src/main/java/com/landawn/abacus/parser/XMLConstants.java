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

public class XMLConstants {

    protected XMLConstants() {
        // singleton.
    }

    /**
     * Field NULL_END_ELEMENT (value is {@code -128})
     */
    public static final int NULL_END_ELEMENT = -128;
    /**
     * Field IS_NULL (value is {@code "isNull"})
     */
    public static final String IS_NULL = "isNull";
    /**
     * Field TRUE (value is {@code "true"})
     */
    public static final String TRUE = "true";
    /**
     * Field FALSE (value is {@code "false"})
     */
    public static final String FALSE = "false";
    /**
     * Field IS_NULL_ATTR (value is {@code " isNull=\"true\"})
     */
    public static final String IS_NULL_ATTR = " isNull=\"true\"";
    /**
     * Field NAME (value is {@code "name"})
     */
    public static final String NAME = "name";
    /**
     * Field TYPE (value is {@code "type"})
     */
    public static final String TYPE = "type";
    /**
     * Field VERSION (value is {@code "version"})
     */
    public static final String VERSION = "version";
    /**
     * Field SIZE (value is {@code "size"})
     */
    public static final String SIZE = "size";
    /**
     * Field NULL_NULL_ELE (value is {@code "<null isNull=\"true\" />"})
     */
    public static final String NULL_NULL_ELE = "<null isNull=\"true\" />";
    /**
     * Field ARRAY (value is {@code "array"})
     */
    public static final String ARRAY = "array";
    /**
     * Field ARRAY_ELE_START (value is {@code "<array>"})
     */
    public static final String ARRAY_ELE_START = "<array>";
    /**
     * Field ARRAY_ELE_END (value is {@code "</array>"})
     */
    public static final String ARRAY_ELE_END = "</array>";
    /**
     * Field ARRAY_NULL_ELE (value is {@code "<array isNull=\"true\" />"})
     */
    public static final String ARRAY_NULL_ELE = "<array isNull=\"true\" />";
    /**
     * Field LIST (value is {@code "list"})
     */
    public static final String LIST = "list";
    /**
     * Field LIST_ELE_START (value is {@code "<list>"})
     */
    public static final String LIST_ELE_START = "<list>";
    /**
     * Field LIST_ELE_END (value is {@code "</list>"})
     */
    public static final String LIST_ELE_END = "</list>";
    /**
     * Field LIST_NULL_ELE (value is {@code "<list isNull=\"true\" />"})
     */
    public static final String LIST_NULL_ELE = "<list isNull=\"true\" />";

    /**
     * Field SET (value is {@code "set"})
     */
    public static final String SET = "set";

    /**
     * Field SET_ELE_START (value is {@code "<set>"})
     */
    public static final String SET_ELE_START = "<set>";

    /**
     * Field SET_ELE_END (value is {@code "</set>"})
     */
    public static final String SET_ELE_END = "</set>";

    /**
     * Field SET_NULL_ELE (value is {@code "<set isNull=\"true\" />"})
     */
    public static final String SET_NULL_ELE = "<set isNull=\"true\" />";

    /**
     * Field COLLECTION (value is {@code "collection"})
     */
    public static final String COLLECTION = "collection";

    /**
     * Field COLLECTION_ELE_START (value is {@code "<collection>"})
     */
    public static final String COLLECTION_ELE_START = "<collection>";

    /**
     * Field COLLECTION_ELE_END (value is {@code "</collection>"})
     */
    public static final String COLLECTION_ELE_END = "</collection>";

    /**
     * Field COLLECTION_NULL_ELE (value is {@code "<collection isNull=\"true\" />"})
     */
    public static final String COLLECTION_NULL_ELE = "<collection isNull=\"true\" />";
    /**
     * Field E (value is {@code "e"})
     */
    public static final String E = "e";
    /**
     * Field E_ELE_END (value is {@code "<e>"})
     */
    public static final String E_ELE_START = "<e>";
    /**
     * Field E_ELE_END (value is {@code "</e>"})
     */
    public static final String E_ELE_END = "</e>";
    /**
     * Field E_NULL_ELE (value is {@code "<e isNull=\"true\" />"})
     */
    public static final String E_NULL_ELE = "<e isNull=\"true\" />";
    /**
     * Field MAP (value is {@code "map"})
     */
    public static final String MAP = "map";
    /**
     * Field MAP_ELE_START (value is {@code "<map>"})
     */
    public static final String MAP_ELE_START = "<map>";
    /**
     * Field MAP_ELE_END (value is {@code "</map>"})
     */
    public static final String MAP_ELE_END = "</map>";
    /**
     * Field MAP_NULL_ELE (value is {@code "<map isNull=\"true\" />"})
     */
    public static final String MAP_NULL_ELE = "<map isNull=\"true\" />";
    /**
     * Field ENTRY (value is {@code "entry"})
     */
    public static final String ENTRY = "entry";
    /**
     * Field MAP_ELE_START (value is {@code "<entry>"})
     */
    public static final String ENTRY_ELE_START = "<entry>";
    /**
     * Field MAP_ELE_END (value is {@code "</entry>"})
     */
    public static final String ENTRY_ELE_END = "</entry>";
    /**
     * Field KEY (value is {@code "key"})
     */
    public static final String KEY = "key";
    /**
     * Field KEY_ELE_START (value is {@code "<key>"})
     */
    public static final String KEY_ELE_START = "<key>";
    /**
     * Field KEY_ELE_END (value is {@code "</key>"})
     */
    public static final String KEY_ELE_END = "</key>";
    /**
     * Field KEY_NULL_ELE (value is {@code "<key isNull=\"true\" />"})
     */
    public static final String KEY_NULL_ELE = "<key isNull=\"true\" />";
    /**
     * Field VALUE (value is {@code "value"})
     */
    public static final String VALUE = "value";
    /**
     * Field VALUE_ELE_START (value is {@code "<value>"})
     */
    public static final String VALUE_ELE_START = "<value>";
    /**
     * Field VALUE_ELE_END (value is {@code "</value>"})
     */
    public static final String VALUE_ELE_END = "</value>";
    /**
     * Field VALUE_NULL_ELE (value is {@code "<value isNull=\"true\" />"})
     */
    public static final String VALUE_NULL_ELE = "<value isNull=\"true\" />";
    /**
     * Field ENTITY_ID (value is {@code "entityId"})
     */
    public static final String ENTITY_ID = "entityId";
    /**
     * Field ENTITY_ID_ELE_START (value is {@code "<entityId>"})
     */
    public static final String ENTITY_ID_ELE_START = "<entityId>";
    /**
     * Field ENTITY_ID_ELE_END (value is {@code "</entityId>"})
     */
    public static final String ENTITY_ID_ELE_END = "</entityId>";
    /**
     * Field ENTITY_ID_NULL_ELE (value is {@code "<entityId isNull=\"true\" />"})
     */
    public static final String ENTITY_ID_NULL_ELE = "<entityId isNull=\"true\" />";
    /**
     * Field ENTITY_IDS (value is {@code "entityIds"})
     */
    public static final String ENTITY_IDS = "entityIds";
    /**
     * Field ENTITY_IDS_ELE_START (value is {@code "<entityIds>"})
     */
    public static final String ENTITY_IDS_ELE_START = "<entityIds>";
    /**
     * Field ENTITY_IDS_ELE_END (value is {@code "</entityIds>"})
     */
    public static final String ENTITY_IDS_ELE_END = "</entityIds>";
    /**
     * Field ENTITY_IDS_NULL_ELE (value is {@code "<entityIds isNull=\"true\" />"})
     */
    public static final String ENTITY_IDS_NULL_ELE = "<entityIds isNull=\"true\" />";
    /**
     * Field ENTITY (value is {@code "bean"})
     */
    public static final String ENTITY = "bean";
    /**
     * Field ENTITY_ELE_START (value is {@code "<bean>"})
     */
    public static final String ENTITY_ELE_START = "<bean>";
    /**
     * Field ENTITY_ELE_END (value is {@code "</bean>"})
     */
    public static final String ENTITY_ELE_END = "</bean>";
    /**
     * Field ENTITY_NULL_ELE (value is {@code "<bean isNull=\"true\" />"})
     */
    public static final String ENTITY_NULL_ELE = "<bean isNull=\"true\" />";
    /**
     * Field ENTITY_NAME (value is {@code "beanName"})
     */
    public static final String ENTITY_NAME = "beanName";
    /**
     * Field ENTITY_NAME_ELE_START (value is {@code "<beanName>"})
     */
    public static final String ENTITY_NAME_ELE_START = "<beanName>";
    /**
     * Field ENTITY_NAME_ELE_END (value is {@code "</beanName>"})
     */
    public static final String ENTITY_NAME_ELE_END = "</beanName>";
    /**
     * Field ENTITY_NAME_NULL_ELE (value is {@code "<beanName isNull=\"true\" />"})
     */
    public static final String ENTITY_NAME_NULL_ELE = "<beanName isNull=\"true\" />";
    /**
     * Field ENTITY_CLASS (value is {@code "beanClass"})
     */
    public static final String ENTITY_CLASS = "beanClass";
    /**
     * Field ENTITY_CLASS_ELE_START (value is {@code "<beanClass>"})
     */
    public static final String ENTITY_CLASS_ELE_START = "<beanClass>";
    /**
     * Field ENTITY_CLASS_ELE_END (value is {@code "</beanClass>"})
     */
    public static final String ENTITY_CLASS_ELE_END = "</beanClass>";
    /**
     * Field ENTITY_CLASS_NULL_ELE (value is {@code "<beanClass isNull=\"true\" />"})
     */
    public static final String ENTITY_CLASS_NULL_ELE = "<beanClass isNull=\"true\" />";
    /**
     * Field PROPERTY (value is {@code "property"})
     */
    public static final String PROPERTY = "property";
    /**
     * Field PROPERTY_ELE_START (value is {@code "<property>"})
     */
    public static final String PROPERTY_ELE_START = "<property>";
    /**
     * Field PROPERTY_ELE_END (value is {@code "</property>"})
     */
    public static final String PROPERTY_ELE_END = "</property>";
    /**
     * Field PROPERTY_NULL_ELE (value is {@code "<property isNull=\"true\" />"})
     */
    public static final String PROPERTY_NULL_ELE = "<property isNull=\"true\" />";
    /**
     * Field PROPERTIES (value is {@code "properties"})
     */
    public static final String PROPERTIES = "properties";
    /**
     * Field PROPERTIES_ELE_START (value is {@code "<properties>"})
     */
    public static final String PROPERTIES_ELE_START = "<properties>";
    /**
     * Field PROPERTIES_ELE_END (value is {@code "</properties>"})
     */
    public static final String PROPERTIES_ELE_END = "</properties>";
    /**
     * Field PROPERTIES_NULL_ELE (value is {@code "<properties isNull=\"true\" />"})
     */
    public static final String PROPERTIES_NULL_ELE = "<properties isNull=\"true\" />";
    /**
     * Field PROP (value is {@code "propName"})
     */
    public static final String PROP = "prop";
    /**
     * Field PROP_ELE_START (value is {@code "<prop>"})
     */
    public static final String PROP_ELE_START = "<prop>";
    /**
     * Field PROP_ELE_END (value is {@code "</prop>"})
     */
    public static final String PROP_ELE_END = "</prop>";
    /**
     * Field PROP_NULL_ELE (value is {@code "<prop isNull=\"true\" />"})
     */
    public static final String PROP_NULL_ELE = "<prop isNull=\"true\" />";
    /**
     * Field PROP_NAME (value is {@code "propName"})
     */
    public static final String PROP_NAME = "propName";
    /**
     * Field PROP_NAME_ELE_START (value is {@code "<propName>"})
     */
    public static final String PROP_NAME_ELE_START = "<propName>";
    /**
     * Field PROP_NAME_ELE_END (value is {@code "</propName>"})
     */
    public static final String PROP_NAME_ELE_END = "</propName>";
    /**
     * Field PROP_NAME_NULL_ELE (value is {@code "<propName isNull=\"true\" />"})
     */
    public static final String PROP_NAME_NULL_ELE = "<propName isNull=\"true\" />";
    /**
     * Field PROP_VALUE (value is {@code "propValue"})
     */
    public static final String PROP_VALUE = "propValue";
    /**
     * Field PROP_VALUE_ELE_START (value is {@code "<propValue>"})
     */
    public static final String PROP_VALUE_ELE_START = "<propValue>";
    /**
     * Field PROP_VALUE_ELE_END (value is {@code "</propValue>"})
     */
    public static final String PROP_VALUE_ELE_END = "</propValue>";
    /**
     * Field PROP_VALUE_NULL_ELE (value is {@code "<propValue isNull=\"true\" />"})
     */
    public static final String PROP_VALUE_NULL_ELE = "<propValue isNull=\"true\" />";
    /**
     * Field PROPS (value is {@code "props"})
     */
    public static final String PROPS = "props";
    /**
     * Field PROPS_ELE_START (value is {@code "<props>"})
     */
    public static final String PROPS_ELE_START = "<props>";
    /**
     * Field PROPS_ELE_END (value is {@code "</props>"})
     */
    public static final String PROPS_ELE_END = "</props>";
    /**
     * Field PROPS_NULL_ELE (value is {@code "<props isNull=\"true\" />"})
     */
    public static final String PROPS_NULL_ELE = "<props isNull=\"true\" />";
    /**
     * Field PROPS_LIST (value is {@code "propsList"})
     */
    public static final String PROPS_LIST = "propsList";
    /**
     * Field PROPS_LIST_ELE_START (value is {@code "<propsList>"})
     */
    public static final String PROPS_LIST_ELE_START = "<propsList>";
    /**
     * Field PROPS_LIST_ELE_END (value is {@code "</propsList>"})
     */
    public static final String PROPS_LIST_ELE_END = "</propsList>";
    /**
     * Field PROPS_LIST_NULL_ELE (value is {@code "<propsList isNull=\"true\" />"})
     */
    public static final String PROPS_LIST_NULL_ELE = "<propsList isNull=\"true\" />";
    /**
     * Field ENTITIES (value is {@code "entities"})
     *
     */
    public static final String ENTITIES = "entities";
    /**
     * Field ENTITIES_ELE_START (value is {@code "<entities>"})
     */
    public static final String ENTITIES_ELE_START = "<entities>";
    /**
     * Field ENTITIES_ELE_END (value is {@code "</entities>"})
     */
    public static final String ENTITIES_ELE_END = "</entities>";
    /**
     * Field ENTITIES_NULL_ELE (value is {@code "<entities isNull=\"true\" />"})
     */
    public static final String ENTITIES_NULL_ELE = "<entities isNull=\"true\" />";
    /**
     * Field RESULT (value is {@code "result"})
     */
    public static final String RESULT = "result";
    /**
     * Field RESULT_ELE_START (value is {@code "<result>"})
     */
    public static final String RESULT_ELE_START = "<result>";
    /**
     * Field RESULT_ELE_END (value is {@code "</result>"})
     */
    public static final String RESULT_ELE_END = "</result>";
    /**
     * Field RESULT_NULL_ELE (value is {@code "<result isNull=\"true\" />"})
     */
    public static final String RESULT_NULL_ELE = "<result isNull=\"true\" />";
    /**
     * Field DATA_SET (value is {@code "dataSet"})
     */
    public static final String DATA_SET = "dataSet";
    /**
     * Field DATA_SET_ELE_START (value is {@code "<dataSet>"})
     */
    public static final String DATA_SET_ELE_START = "<dataSet>";
    /**
     * Field DATA_SET_ELE_END (value is {@code "</dataSet>"})
     */
    public static final String DATA_SET_ELE_END = "</dataSet>";
    /**
     * Field DATA_SET_NULL_ELE (value is {@code "<dataSet isNull=\"true\" />"})
     */
    public static final String DATA_SET_NULL_ELE = "<dataSet isNull=\"true\" />";
    /**
     * Field ENVELOPE (value is {@code "Envelope"})
     */
    public static final String ENVELOPE = "Envelope";
    /**
     * Field SOAP_ENVELOPE_ELE_START (value is {@code "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"})
     */
    public static final String SOAP_ENVELOPE_ELE_START = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">";
    /**
     * Field SOAP_ENVELOPE_ELE_END (value is {@code "</soap:Envelope>"})
     */
    public static final String SOAP_ENVELOPE_ELE_END = "</soap:Envelope>";
    /**
     * Field HEADER (value is {@code "Header"})
     */
    public static final String HEADER = "Header";
    /**
     * Field SOAP_HEADER_ELE_START (value is {@code "<soap:Header>"})
     */
    public static final String SOAP_HEADER_ELE_START = "<soap:Header>";
    /**
     * Field SOAP_HEADER_ELE_END (value is {@code "</soap:Header>"})
     */
    public static final String SOAP_HEADER_ELE_END = "</soap:Header>";
    /**
     * Field BODY (value is {@code "BODY"})
     */
    public static final String BODY = "BODY";
    /**
     * Field SOAP_BODY_ELE_START (value is {@code "<soap:Body>"})
     */
    public static final String SOAP_BODY_ELE_START = "<soap:Body>";
    /**
     * Field SOAP_BODY_ELE_END (value is {@code "</soap:Body>"})
     */
    public static final String SOAP_BODY_ELE_END = "</soap:Body>";
    /**
     * Field FAULT (value is {@code "Fault"})
     */
    public static final String FAULT = "Fault";
    /**
     * Field SOAP_FAULT_ELE_START (value is {@code "<soap:Fault>"})
     */
    public static final String SOAP_FAULT_ELE_START = "<soap:Fault>";
    /**
     * Field SOAP_FAULT_ELE_END (value is {@code "</soap:Fault>"})
     */
    public static final String SOAP_FAULT_ELE_END = "</soap:Fault>";
    /**
     * Field START_ARRAY_ELE_WITH_TYPE (value is {@code "<array type=\""})
     */
    static final char[] START_ARRAY_ELE_WITH_TYPE = "<array type=\"".toCharArray();
    /**
     * Field START_LIST_ELE_WITH_TYPE (value is {@code "<list type=\""})
     */
    static final char[] START_LIST_ELE_WITH_TYPE = "<list type=\"".toCharArray();
    /**
     * Field START_SET_ELE_WITH_TYPE (value is {@code "<set type=\""})
     */
    static final char[] START_SET_ELE_WITH_TYPE = "<set type=\"".toCharArray();
    /**
     * Field START_COLLECTION_ELE_WITH_TYPE (value is {@code "<collection type=\""})
     */
    static final char[] START_COLLECTION_ELE_WITH_TYPE = "<collection type=\"".toCharArray();
    /**
     * Field START_E_ELE_WITH_TYPE (value is {@code "<e type=\""})
     */
    static final char[] START_E_ELE_WITH_TYPE = "<e type=\"".toCharArray();
    /**
     * Field START_MAP_ELE_WITH_TYPE (value is {@code "<map type=\""})
     */
    static final char[] START_MAP_ELE_WITH_TYPE = "<map type=\"".toCharArray();
    /**
     * Field START_KEY_ELE_WITH_TYPE (value is {@code "<key type=\""})
     */
    static final char[] START_KEY_ELE_WITH_TYPE = "<key type=\"".toCharArray();
    /**
     * Field START_KEY_ELE_WITH_STRING_TYPE (value is {@code "<key type=\"String\">"})
     */
    static final char[] START_KEY_ELE_WITH_STRING_TYPE = "<key type=\"String\">".toCharArray();
    /**
     * Field START_VALUE_ELE_WITH_TYPE (value is {@code "<value type=\""})
     */
    static final char[] START_VALUE_ELE_WITH_TYPE = "<value type=\"".toCharArray();
    /**
     * Field START_TYPE_ATTR (value is {@code " type=\""})
     */
    static final char[] START_TYPE_ATTR = " type=\"".toCharArray();
    /**
     * Field CLOSE_ATTR_AND_ELE (value is {@code "\">"})
     */
    static final char[] CLOSE_ATTR_AND_ELE = "\">".toCharArray();
    /**
     * Field END_ELEMENT (value is {@code " />"})
     */
    static final char[] END_ELEMENT = " />".toCharArray();
}
