package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class XmlConstantsTest extends TestBase {

    @Test
    public void test_NULL_END_ELEMENT() {
        assertEquals(-128, XmlConstants.NULL_END_ELEMENT);
    }

    @Test
    public void test_IS_NULL() {
        assertEquals("isNull", XmlConstants.IS_NULL);
    }

    @Test
    public void test_TRUE() {
        assertEquals("true", XmlConstants.TRUE);
    }

    @Test
    public void test_FALSE() {
        assertEquals("false", XmlConstants.FALSE);
    }

    @Test
    public void test_IS_NULL_ATTR() {
        assertEquals(" isNull=\"true\"", XmlConstants.IS_NULL_ATTR);
    }

    @Test
    public void test_NAME() {
        assertEquals("name", XmlConstants.NAME);
    }

    @Test
    public void test_TYPE() {
        assertEquals("type", XmlConstants.TYPE);
    }

    @Test
    public void test_VERSION() {
        assertEquals("version", XmlConstants.VERSION);
    }

    @Test
    public void test_SIZE() {
        assertEquals("size", XmlConstants.SIZE);
    }

    @Test
    public void test_ARRAY_constants() {
        assertEquals("array", XmlConstants.ARRAY);
        assertEquals("<array>", XmlConstants.ARRAY_ELE_START);
        assertEquals("</array>", XmlConstants.ARRAY_ELE_END);
        assertEquals("<array isNull=\"true\" />", XmlConstants.ARRAY_NULL_ELE);
    }

    @Test
    public void test_LIST_constants() {
        assertEquals("list", XmlConstants.LIST);
        assertEquals("<list>", XmlConstants.LIST_ELE_START);
        assertEquals("</list>", XmlConstants.LIST_ELE_END);
        assertEquals("<list isNull=\"true\" />", XmlConstants.LIST_NULL_ELE);
    }

    @Test
    public void test_SET_constants() {
        assertEquals("set", XmlConstants.SET);
        assertEquals("<set>", XmlConstants.SET_ELE_START);
        assertEquals("</set>", XmlConstants.SET_ELE_END);
        assertEquals("<set isNull=\"true\" />", XmlConstants.SET_NULL_ELE);
    }

    @Test
    public void test_MAP_constants() {
        assertEquals("map", XmlConstants.MAP);
        assertEquals("<map>", XmlConstants.MAP_ELE_START);
        assertEquals("</map>", XmlConstants.MAP_ELE_END);
        assertEquals("<map isNull=\"true\" />", XmlConstants.MAP_NULL_ELE);
    }

    @Test
    public void test_ENTRY_constants() {
        assertEquals("entry", XmlConstants.ENTRY);
        assertEquals("<entry>", XmlConstants.ENTRY_ELE_START);
        assertEquals("</entry>", XmlConstants.ENTRY_ELE_END);
    }

    @Test
    public void test_KEY_constants() {
        assertEquals("key", XmlConstants.KEY);
        assertEquals("<key>", XmlConstants.KEY_ELE_START);
        assertEquals("</key>", XmlConstants.KEY_ELE_END);
        assertEquals("<key isNull=\"true\" />", XmlConstants.KEY_NULL_ELE);
    }

    @Test
    public void test_VALUE_constants() {
        assertEquals("value", XmlConstants.VALUE);
        assertEquals("<value>", XmlConstants.VALUE_ELE_START);
        assertEquals("</value>", XmlConstants.VALUE_ELE_END);
        assertEquals("<value isNull=\"true\" />", XmlConstants.VALUE_NULL_ELE);
    }

    @Test
    public void test_ENTITY_constants() {
        assertEquals("bean", XmlConstants.ENTITY);
        assertEquals("<bean>", XmlConstants.ENTITY_ELE_START);
        assertEquals("</bean>", XmlConstants.ENTITY_ELE_END);
        assertEquals("<bean isNull=\"true\" />", XmlConstants.ENTITY_NULL_ELE);
    }

    @Test
    public void test_PROPERTY_constants() {
        assertEquals("property", XmlConstants.PROPERTY);
        assertEquals("<property>", XmlConstants.PROPERTY_ELE_START);
        assertEquals("</property>", XmlConstants.PROPERTY_ELE_END);
        assertEquals("<property isNull=\"true\" />", XmlConstants.PROPERTY_NULL_ELE);
    }

    @Test
    public void test_SOAP_ENVELOPE_constants() {
        assertEquals("Envelope", XmlConstants.ENVELOPE);
        assertNotNull(XmlConstants.SOAP_ENVELOPE_ELE_START);
        assertEquals("</soap:Envelope>", XmlConstants.SOAP_ENVELOPE_ELE_END);
    }

    @Test
    public void test_SOAP_HEADER_constants() {
        assertEquals("Header", XmlConstants.HEADER);
        assertEquals("<soap:Header>", XmlConstants.SOAP_HEADER_ELE_START);
        assertEquals("</soap:Header>", XmlConstants.SOAP_HEADER_ELE_END);
    }

    @Test
    public void test_SOAP_BODY_constants() {
        assertEquals("BODY", XmlConstants.BODY);
        assertEquals("<soap:Body>", XmlConstants.SOAP_BODY_ELE_START);
        assertEquals("</soap:Body>", XmlConstants.SOAP_BODY_ELE_END);
    }

    @Test
    public void test_SOAP_FAULT_constants() {
        assertEquals("Fault", XmlConstants.FAULT);
        assertEquals("<soap:Fault>", XmlConstants.SOAP_FAULT_ELE_START);
        assertEquals("</soap:Fault>", XmlConstants.SOAP_FAULT_ELE_END);
    }

    @Test
    public void test_charArrayConstants() {
        assertNotNull(XmlConstants.START_ARRAY_ELE_WITH_TYPE);
        assertNotNull(XmlConstants.START_LIST_ELE_WITH_TYPE);
        assertNotNull(XmlConstants.START_SET_ELE_WITH_TYPE);
        assertNotNull(XmlConstants.START_MAP_ELE_WITH_TYPE);
        assertNotNull(XmlConstants.START_KEY_ELE_WITH_TYPE);
        assertNotNull(XmlConstants.START_VALUE_ELE_WITH_TYPE);
        assertNotNull(XmlConstants.CLOSE_ATTR_AND_ELE);
        assertNotNull(XmlConstants.END_ELEMENT);
    }

    @Test
    public void test_charArrayConstants_content() {
        assertArrayEquals("<array type=\"".toCharArray(), XmlConstants.START_ARRAY_ELE_WITH_TYPE);
        assertArrayEquals("<list type=\"".toCharArray(), XmlConstants.START_LIST_ELE_WITH_TYPE);
        assertArrayEquals("<set type=\"".toCharArray(), XmlConstants.START_SET_ELE_WITH_TYPE);
        assertArrayEquals("<collection type=\"".toCharArray(), XmlConstants.START_COLLECTION_ELE_WITH_TYPE);
        assertArrayEquals("<e type=\"".toCharArray(), XmlConstants.START_E_ELE_WITH_TYPE);
        assertArrayEquals("<map type=\"".toCharArray(), XmlConstants.START_MAP_ELE_WITH_TYPE);
        assertArrayEquals("<key type=\"".toCharArray(), XmlConstants.START_KEY_ELE_WITH_TYPE);
        assertArrayEquals("<key type=\"String\">".toCharArray(), XmlConstants.START_KEY_ELE_WITH_STRING_TYPE);
        assertArrayEquals("<value type=\"".toCharArray(), XmlConstants.START_VALUE_ELE_WITH_TYPE);
        assertArrayEquals(" type=\"".toCharArray(), XmlConstants.START_TYPE_ATTR);
        assertArrayEquals("\">".toCharArray(), XmlConstants.CLOSE_ATTR_AND_ELE);
        assertArrayEquals(" />".toCharArray(), XmlConstants.END_ELEMENT);
    }

    @Test
    public void test_COLLECTION_constants() {
        assertEquals("collection", XmlConstants.COLLECTION);
        assertEquals("<collection>", XmlConstants.COLLECTION_ELE_START);
        assertEquals("</collection>", XmlConstants.COLLECTION_ELE_END);
        assertEquals("<collection isNull=\"true\" />", XmlConstants.COLLECTION_NULL_ELE);
    }

    @Test
    public void test_E_constants() {
        assertEquals("e", XmlConstants.E);
        assertEquals("<e>", XmlConstants.E_ELE_START);
        assertEquals("</e>", XmlConstants.E_ELE_END);
        assertEquals("<e isNull=\"true\" />", XmlConstants.E_NULL_ELE);
    }

    @Test
    public void test_ENTITY_ID_constants() {
        assertEquals("entityId", XmlConstants.ENTITY_ID);
        assertEquals("<entityId>", XmlConstants.ENTITY_ID_ELE_START);
        assertEquals("</entityId>", XmlConstants.ENTITY_ID_ELE_END);
        assertEquals("<entityId isNull=\"true\" />", XmlConstants.ENTITY_ID_NULL_ELE);
    }

    @Test
    public void test_ENTITY_IDS_constants() {
        assertEquals("entityIds", XmlConstants.ENTITY_IDS);
        assertEquals("<entityIds>", XmlConstants.ENTITY_IDS_ELE_START);
        assertEquals("</entityIds>", XmlConstants.ENTITY_IDS_ELE_END);
        assertEquals("<entityIds isNull=\"true\" />", XmlConstants.ENTITY_IDS_NULL_ELE);
    }

    @Test
    public void test_ENTITY_NAME_constants() {
        assertEquals("beanName", XmlConstants.ENTITY_NAME);
        assertEquals("<beanName>", XmlConstants.ENTITY_NAME_ELE_START);
        assertEquals("</beanName>", XmlConstants.ENTITY_NAME_ELE_END);
        assertEquals("<beanName isNull=\"true\" />", XmlConstants.ENTITY_NAME_NULL_ELE);
    }

    @Test
    public void test_ENTITY_CLASS_constants() {
        assertEquals("beanClass", XmlConstants.ENTITY_CLASS);
        assertEquals("<beanClass>", XmlConstants.ENTITY_CLASS_ELE_START);
        assertEquals("</beanClass>", XmlConstants.ENTITY_CLASS_ELE_END);
        assertEquals("<beanClass isNull=\"true\" />", XmlConstants.ENTITY_CLASS_NULL_ELE);
    }

    @Test
    public void test_PROPERTIES_constants() {
        assertEquals("properties", XmlConstants.PROPERTIES);
        assertEquals("<properties>", XmlConstants.PROPERTIES_ELE_START);
        assertEquals("</properties>", XmlConstants.PROPERTIES_ELE_END);
        assertEquals("<properties isNull=\"true\" />", XmlConstants.PROPERTIES_NULL_ELE);
    }

    @Test
    public void test_PROP_constants() {
        assertEquals("prop", XmlConstants.PROP);
        assertEquals("<prop>", XmlConstants.PROP_ELE_START);
        assertEquals("</prop>", XmlConstants.PROP_ELE_END);
        assertEquals("<prop isNull=\"true\" />", XmlConstants.PROP_NULL_ELE);
    }

    @Test
    public void test_PROP_NAME_constants() {
        assertEquals("propName", XmlConstants.PROP_NAME);
        assertEquals("<propName>", XmlConstants.PROP_NAME_ELE_START);
        assertEquals("</propName>", XmlConstants.PROP_NAME_ELE_END);
        assertEquals("<propName isNull=\"true\" />", XmlConstants.PROP_NAME_NULL_ELE);
    }

    @Test
    public void test_PROP_VALUE_constants() {
        assertEquals("propValue", XmlConstants.PROP_VALUE);
        assertEquals("<propValue>", XmlConstants.PROP_VALUE_ELE_START);
        assertEquals("</propValue>", XmlConstants.PROP_VALUE_ELE_END);
        assertEquals("<propValue isNull=\"true\" />", XmlConstants.PROP_VALUE_NULL_ELE);
    }

    @Test
    public void test_PROPS_constants() {
        assertEquals("props", XmlConstants.PROPS);
        assertEquals("<props>", XmlConstants.PROPS_ELE_START);
        assertEquals("</props>", XmlConstants.PROPS_ELE_END);
        assertEquals("<props isNull=\"true\" />", XmlConstants.PROPS_NULL_ELE);
    }

    @Test
    public void test_PROPS_LIST_constants() {
        assertEquals("propsList", XmlConstants.PROPS_LIST);
        assertEquals("<propsList>", XmlConstants.PROPS_LIST_ELE_START);
        assertEquals("</propsList>", XmlConstants.PROPS_LIST_ELE_END);
        assertEquals("<propsList isNull=\"true\" />", XmlConstants.PROPS_LIST_NULL_ELE);
    }

    @Test
    public void test_ENTITIES_constants() {
        assertEquals("entities", XmlConstants.ENTITIES);
        assertEquals("<entities>", XmlConstants.ENTITIES_ELE_START);
        assertEquals("</entities>", XmlConstants.ENTITIES_ELE_END);
        assertEquals("<entities isNull=\"true\" />", XmlConstants.ENTITIES_NULL_ELE);
    }

    @Test
    public void test_RESULT_constants() {
        assertEquals("result", XmlConstants.RESULT);
        assertEquals("<result>", XmlConstants.RESULT_ELE_START);
        assertEquals("</result>", XmlConstants.RESULT_ELE_END);
        assertEquals("<result isNull=\"true\" />", XmlConstants.RESULT_NULL_ELE);
    }

    @Test
    public void test_DATA_SET_constants() {
        assertEquals("dataset", XmlConstants.DATA_SET);
        assertEquals("<dataset>", XmlConstants.DATA_SET_ELE_START);
        assertEquals("</dataset>", XmlConstants.DATA_SET_ELE_END);
        assertEquals("<dataset isNull=\"true\" />", XmlConstants.DATA_SET_NULL_ELE);
    }

    @Test
    public void test_NULL_NULL_ELE() {
        assertEquals("<null isNull=\"true\" />", XmlConstants.NULL_NULL_ELE);
    }

    @Test
    public void test_SOAP_ENVELOPE_ELE_START() {
        assertEquals("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">", XmlConstants.SOAP_ENVELOPE_ELE_START);
    }

    @Test
    public void test_elementPatternConsistency() {
        // Verify that ELE_START / ELE_END pairs follow the pattern <name> / </name>
        assertTrue(XmlConstants.ARRAY_ELE_START.startsWith("<"));
        assertTrue(XmlConstants.ARRAY_ELE_START.endsWith(">"));
        assertTrue(XmlConstants.ARRAY_ELE_END.startsWith("</"));
        assertTrue(XmlConstants.ARRAY_ELE_END.endsWith(">"));

        // Verify NULL_ELE contains isNull attribute
        assertTrue(XmlConstants.ARRAY_NULL_ELE.contains("isNull=\"true\""));
        assertTrue(XmlConstants.LIST_NULL_ELE.contains("isNull=\"true\""));
        assertTrue(XmlConstants.SET_NULL_ELE.contains("isNull=\"true\""));
        assertTrue(XmlConstants.MAP_NULL_ELE.contains("isNull=\"true\""));
        assertTrue(XmlConstants.ENTITY_NULL_ELE.contains("isNull=\"true\""));
    }

    @Test
    public void testConstants() {
        assertEquals(-128, XmlConstants.NULL_END_ELEMENT);

        assertEquals("isNull", XmlConstants.IS_NULL);
        assertEquals("true", XmlConstants.TRUE);
        assertEquals("false", XmlConstants.FALSE);
        assertEquals(" isNull=\"true\"", XmlConstants.IS_NULL_ATTR);
        assertEquals("name", XmlConstants.NAME);
        assertEquals("type", XmlConstants.TYPE);
        assertEquals("version", XmlConstants.VERSION);
        assertEquals("size", XmlConstants.SIZE);
        assertEquals("<null isNull=\"true\" />", XmlConstants.NULL_NULL_ELE);

        assertEquals("array", XmlConstants.ARRAY);
        assertEquals("<array>", XmlConstants.ARRAY_ELE_START);
        assertEquals("</array>", XmlConstants.ARRAY_ELE_END);
        assertEquals("<array isNull=\"true\" />", XmlConstants.ARRAY_NULL_ELE);

        assertEquals("list", XmlConstants.LIST);
        assertEquals("<list>", XmlConstants.LIST_ELE_START);
        assertEquals("</list>", XmlConstants.LIST_ELE_END);
        assertEquals("<list isNull=\"true\" />", XmlConstants.LIST_NULL_ELE);

        assertEquals("set", XmlConstants.SET);
        assertEquals("<set>", XmlConstants.SET_ELE_START);
        assertEquals("</set>", XmlConstants.SET_ELE_END);
        assertEquals("<set isNull=\"true\" />", XmlConstants.SET_NULL_ELE);

        assertEquals("collection", XmlConstants.COLLECTION);
        assertEquals("<collection>", XmlConstants.COLLECTION_ELE_START);
        assertEquals("</collection>", XmlConstants.COLLECTION_ELE_END);
        assertEquals("<collection isNull=\"true\" />", XmlConstants.COLLECTION_NULL_ELE);

        assertEquals("e", XmlConstants.E);
        assertEquals("<e>", XmlConstants.E_ELE_START);
        assertEquals("</e>", XmlConstants.E_ELE_END);
        assertEquals("<e isNull=\"true\" />", XmlConstants.E_NULL_ELE);

        assertEquals("map", XmlConstants.MAP);
        assertEquals("<map>", XmlConstants.MAP_ELE_START);
        assertEquals("</map>", XmlConstants.MAP_ELE_END);
        assertEquals("<map isNull=\"true\" />", XmlConstants.MAP_NULL_ELE);

        assertEquals("entry", XmlConstants.ENTRY);
        assertEquals("<entry>", XmlConstants.ENTRY_ELE_START);
        assertEquals("</entry>", XmlConstants.ENTRY_ELE_END);

        assertEquals("key", XmlConstants.KEY);
        assertEquals("<key>", XmlConstants.KEY_ELE_START);
        assertEquals("</key>", XmlConstants.KEY_ELE_END);
        assertEquals("<key isNull=\"true\" />", XmlConstants.KEY_NULL_ELE);

        assertEquals("value", XmlConstants.VALUE);
        assertEquals("<value>", XmlConstants.VALUE_ELE_START);
        assertEquals("</value>", XmlConstants.VALUE_ELE_END);
        assertEquals("<value isNull=\"true\" />", XmlConstants.VALUE_NULL_ELE);

        assertEquals("entityId", XmlConstants.ENTITY_ID);
        assertEquals("<entityId>", XmlConstants.ENTITY_ID_ELE_START);
        assertEquals("</entityId>", XmlConstants.ENTITY_ID_ELE_END);
        assertEquals("<entityId isNull=\"true\" />", XmlConstants.ENTITY_ID_NULL_ELE);

        assertEquals("entityIds", XmlConstants.ENTITY_IDS);
        assertEquals("<entityIds>", XmlConstants.ENTITY_IDS_ELE_START);
        assertEquals("</entityIds>", XmlConstants.ENTITY_IDS_ELE_END);
        assertEquals("<entityIds isNull=\"true\" />", XmlConstants.ENTITY_IDS_NULL_ELE);

        assertEquals("bean", XmlConstants.ENTITY);
        assertEquals("<bean>", XmlConstants.ENTITY_ELE_START);
        assertEquals("</bean>", XmlConstants.ENTITY_ELE_END);
        assertEquals("<bean isNull=\"true\" />", XmlConstants.ENTITY_NULL_ELE);

        assertEquals("beanName", XmlConstants.ENTITY_NAME);
        assertEquals("<beanName>", XmlConstants.ENTITY_NAME_ELE_START);
        assertEquals("</beanName>", XmlConstants.ENTITY_NAME_ELE_END);
        assertEquals("<beanName isNull=\"true\" />", XmlConstants.ENTITY_NAME_NULL_ELE);

        assertEquals("beanClass", XmlConstants.ENTITY_CLASS);
        assertEquals("<beanClass>", XmlConstants.ENTITY_CLASS_ELE_START);
        assertEquals("</beanClass>", XmlConstants.ENTITY_CLASS_ELE_END);
        assertEquals("<beanClass isNull=\"true\" />", XmlConstants.ENTITY_CLASS_NULL_ELE);

        assertEquals("property", XmlConstants.PROPERTY);
        assertEquals("<property>", XmlConstants.PROPERTY_ELE_START);
        assertEquals("</property>", XmlConstants.PROPERTY_ELE_END);
        assertEquals("<property isNull=\"true\" />", XmlConstants.PROPERTY_NULL_ELE);

        assertEquals("properties", XmlConstants.PROPERTIES);
        assertEquals("<properties>", XmlConstants.PROPERTIES_ELE_START);
        assertEquals("</properties>", XmlConstants.PROPERTIES_ELE_END);
        assertEquals("<properties isNull=\"true\" />", XmlConstants.PROPERTIES_NULL_ELE);

        assertEquals("prop", XmlConstants.PROP);
        assertEquals("<prop>", XmlConstants.PROP_ELE_START);
        assertEquals("</prop>", XmlConstants.PROP_ELE_END);
        assertEquals("<prop isNull=\"true\" />", XmlConstants.PROP_NULL_ELE);

        assertEquals("propName", XmlConstants.PROP_NAME);
        assertEquals("<propName>", XmlConstants.PROP_NAME_ELE_START);
        assertEquals("</propName>", XmlConstants.PROP_NAME_ELE_END);
        assertEquals("<propName isNull=\"true\" />", XmlConstants.PROP_NAME_NULL_ELE);

        assertEquals("propValue", XmlConstants.PROP_VALUE);
        assertEquals("<propValue>", XmlConstants.PROP_VALUE_ELE_START);
        assertEquals("</propValue>", XmlConstants.PROP_VALUE_ELE_END);
        assertEquals("<propValue isNull=\"true\" />", XmlConstants.PROP_VALUE_NULL_ELE);

        assertEquals("props", XmlConstants.PROPS);
        assertEquals("<props>", XmlConstants.PROPS_ELE_START);
        assertEquals("</props>", XmlConstants.PROPS_ELE_END);
        assertEquals("<props isNull=\"true\" />", XmlConstants.PROPS_NULL_ELE);

        assertEquals("propsList", XmlConstants.PROPS_LIST);
        assertEquals("<propsList>", XmlConstants.PROPS_LIST_ELE_START);
        assertEquals("</propsList>", XmlConstants.PROPS_LIST_ELE_END);
        assertEquals("<propsList isNull=\"true\" />", XmlConstants.PROPS_LIST_NULL_ELE);

        assertEquals("entities", XmlConstants.ENTITIES);
        assertEquals("<entities>", XmlConstants.ENTITIES_ELE_START);
        assertEquals("</entities>", XmlConstants.ENTITIES_ELE_END);
        assertEquals("<entities isNull=\"true\" />", XmlConstants.ENTITIES_NULL_ELE);

        assertEquals("result", XmlConstants.RESULT);
        assertEquals("<result>", XmlConstants.RESULT_ELE_START);
        assertEquals("</result>", XmlConstants.RESULT_ELE_END);
        assertEquals("<result isNull=\"true\" />", XmlConstants.RESULT_NULL_ELE);

        assertEquals("dataset", XmlConstants.DATA_SET);
        assertEquals("<dataset>", XmlConstants.DATA_SET_ELE_START);
        assertEquals("</dataset>", XmlConstants.DATA_SET_ELE_END);
        assertEquals("<dataset isNull=\"true\" />", XmlConstants.DATA_SET_NULL_ELE);

        assertEquals("Envelope", XmlConstants.ENVELOPE);
        assertEquals("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">", XmlConstants.SOAP_ENVELOPE_ELE_START);
        assertEquals("</soap:Envelope>", XmlConstants.SOAP_ENVELOPE_ELE_END);

        assertEquals("Header", XmlConstants.HEADER);
        assertEquals("<soap:Header>", XmlConstants.SOAP_HEADER_ELE_START);
        assertEquals("</soap:Header>", XmlConstants.SOAP_HEADER_ELE_END);

        assertEquals("BODY", XmlConstants.BODY);
        assertEquals("<soap:Body>", XmlConstants.SOAP_BODY_ELE_START);
        assertEquals("</soap:Body>", XmlConstants.SOAP_BODY_ELE_END);

        assertEquals("Fault", XmlConstants.FAULT);
        assertEquals("<soap:Fault>", XmlConstants.SOAP_FAULT_ELE_START);
        assertEquals("</soap:Fault>", XmlConstants.SOAP_FAULT_ELE_END);
    }

}
