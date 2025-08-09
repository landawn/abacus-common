package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class XMLConstants100Test extends TestBase {
    
    @Test
    public void testConstants() {
        // Test numeric constants
        assertEquals(-128, XMLConstants.NULL_END_ELEMENT);
        
        // Test string constants
        assertEquals("isNull", XMLConstants.IS_NULL);
        assertEquals("true", XMLConstants.TRUE);
        assertEquals("false", XMLConstants.FALSE);
        assertEquals(" isNull=\"true\"", XMLConstants.IS_NULL_ATTR);
        assertEquals("name", XMLConstants.NAME);
        assertEquals("type", XMLConstants.TYPE);
        assertEquals("version", XMLConstants.VERSION);
        assertEquals("size", XMLConstants.SIZE);
        assertEquals("<null isNull=\"true\" />", XMLConstants.NULL_NULL_ELE);
        
        // Test array-related constants
        assertEquals("array", XMLConstants.ARRAY);
        assertEquals("<array>", XMLConstants.ARRAY_ELE_START);
        assertEquals("</array>", XMLConstants.ARRAY_ELE_END);
        assertEquals("<array isNull=\"true\" />", XMLConstants.ARRAY_NULL_ELE);
        
        // Test list-related constants
        assertEquals("list", XMLConstants.LIST);
        assertEquals("<list>", XMLConstants.LIST_ELE_START);
        assertEquals("</list>", XMLConstants.LIST_ELE_END);
        assertEquals("<list isNull=\"true\" />", XMLConstants.LIST_NULL_ELE);
        
        // Test set-related constants
        assertEquals("set", XMLConstants.SET);
        assertEquals("<set>", XMLConstants.SET_ELE_START);
        assertEquals("</set>", XMLConstants.SET_ELE_END);
        assertEquals("<set isNull=\"true\" />", XMLConstants.SET_NULL_ELE);
        
        // Test collection-related constants
        assertEquals("collection", XMLConstants.COLLECTION);
        assertEquals("<collection>", XMLConstants.COLLECTION_ELE_START);
        assertEquals("</collection>", XMLConstants.COLLECTION_ELE_END);
        assertEquals("<collection isNull=\"true\" />", XMLConstants.COLLECTION_NULL_ELE);
        
        // Test element constants
        assertEquals("e", XMLConstants.E);
        assertEquals("<e>", XMLConstants.E_ELE_START);
        assertEquals("</e>", XMLConstants.E_ELE_END);
        assertEquals("<e isNull=\"true\" />", XMLConstants.E_NULL_ELE);
        
        // Test map-related constants
        assertEquals("map", XMLConstants.MAP);
        assertEquals("<map>", XMLConstants.MAP_ELE_START);
        assertEquals("</map>", XMLConstants.MAP_ELE_END);
        assertEquals("<map isNull=\"true\" />", XMLConstants.MAP_NULL_ELE);
        
        // Test map entry constants
        assertEquals("entry", XMLConstants.ENTRY);
        assertEquals("<entry>", XMLConstants.ENTRY_ELE_START);
        assertEquals("</entry>", XMLConstants.ENTRY_ELE_END);
        
        // Test key constants
        assertEquals("key", XMLConstants.KEY);
        assertEquals("<key>", XMLConstants.KEY_ELE_START);
        assertEquals("</key>", XMLConstants.KEY_ELE_END);
        assertEquals("<key isNull=\"true\" />", XMLConstants.KEY_NULL_ELE);
        
        // Test value constants
        assertEquals("value", XMLConstants.VALUE);
        assertEquals("<value>", XMLConstants.VALUE_ELE_START);
        assertEquals("</value>", XMLConstants.VALUE_ELE_END);
        assertEquals("<value isNull=\"true\" />", XMLConstants.VALUE_NULL_ELE);
        
        // Test entity-related constants
        assertEquals("entityId", XMLConstants.ENTITY_ID);
        assertEquals("<entityId>", XMLConstants.ENTITY_ID_ELE_START);
        assertEquals("</entityId>", XMLConstants.ENTITY_ID_ELE_END);
        assertEquals("<entityId isNull=\"true\" />", XMLConstants.ENTITY_ID_NULL_ELE);
        
        assertEquals("entityIds", XMLConstants.ENTITY_IDS);
        assertEquals("<entityIds>", XMLConstants.ENTITY_IDS_ELE_START);
        assertEquals("</entityIds>", XMLConstants.ENTITY_IDS_ELE_END);
        assertEquals("<entityIds isNull=\"true\" />", XMLConstants.ENTITY_IDS_NULL_ELE);
        
        assertEquals("bean", XMLConstants.ENTITY);
        assertEquals("<bean>", XMLConstants.ENTITY_ELE_START);
        assertEquals("</bean>", XMLConstants.ENTITY_ELE_END);
        assertEquals("<bean isNull=\"true\" />", XMLConstants.ENTITY_NULL_ELE);
        
        assertEquals("beanName", XMLConstants.ENTITY_NAME);
        assertEquals("<beanName>", XMLConstants.ENTITY_NAME_ELE_START);
        assertEquals("</beanName>", XMLConstants.ENTITY_NAME_ELE_END);
        assertEquals("<beanName isNull=\"true\" />", XMLConstants.ENTITY_NAME_NULL_ELE);
        
        assertEquals("beanClass", XMLConstants.ENTITY_CLASS);
        assertEquals("<beanClass>", XMLConstants.ENTITY_CLASS_ELE_START);
        assertEquals("</beanClass>", XMLConstants.ENTITY_CLASS_ELE_END);
        assertEquals("<beanClass isNull=\"true\" />", XMLConstants.ENTITY_CLASS_NULL_ELE);
        
        // Test property-related constants
        assertEquals("property", XMLConstants.PROPERTY);
        assertEquals("<property>", XMLConstants.PROPERTY_ELE_START);
        assertEquals("</property>", XMLConstants.PROPERTY_ELE_END);
        assertEquals("<property isNull=\"true\" />", XMLConstants.PROPERTY_NULL_ELE);
        
        assertEquals("properties", XMLConstants.PROPERTIES);
        assertEquals("<properties>", XMLConstants.PROPERTIES_ELE_START);
        assertEquals("</properties>", XMLConstants.PROPERTIES_ELE_END);
        assertEquals("<properties isNull=\"true\" />", XMLConstants.PROPERTIES_NULL_ELE);
        
        assertEquals("prop", XMLConstants.PROP);
        assertEquals("<prop>", XMLConstants.PROP_ELE_START);
        assertEquals("</prop>", XMLConstants.PROP_ELE_END);
        assertEquals("<prop isNull=\"true\" />", XMLConstants.PROP_NULL_ELE);
        
        assertEquals("propName", XMLConstants.PROP_NAME);
        assertEquals("<propName>", XMLConstants.PROP_NAME_ELE_START);
        assertEquals("</propName>", XMLConstants.PROP_NAME_ELE_END);
        assertEquals("<propName isNull=\"true\" />", XMLConstants.PROP_NAME_NULL_ELE);
        
        assertEquals("propValue", XMLConstants.PROP_VALUE);
        assertEquals("<propValue>", XMLConstants.PROP_VALUE_ELE_START);
        assertEquals("</propValue>", XMLConstants.PROP_VALUE_ELE_END);
        assertEquals("<propValue isNull=\"true\" />", XMLConstants.PROP_VALUE_NULL_ELE);
        
        assertEquals("props", XMLConstants.PROPS);
        assertEquals("<props>", XMLConstants.PROPS_ELE_START);
        assertEquals("</props>", XMLConstants.PROPS_ELE_END);
        assertEquals("<props isNull=\"true\" />", XMLConstants.PROPS_NULL_ELE);
        
        assertEquals("propsList", XMLConstants.PROPS_LIST);
        assertEquals("<propsList>", XMLConstants.PROPS_LIST_ELE_START);
        assertEquals("</propsList>", XMLConstants.PROPS_LIST_ELE_END);
        assertEquals("<propsList isNull=\"true\" />", XMLConstants.PROPS_LIST_NULL_ELE);
        
        // Test entities constants
        assertEquals("entities", XMLConstants.ENTITIES);
        assertEquals("<entities>", XMLConstants.ENTITIES_ELE_START);
        assertEquals("</entities>", XMLConstants.ENTITIES_ELE_END);
        assertEquals("<entities isNull=\"true\" />", XMLConstants.ENTITIES_NULL_ELE);
        
        // Test result constants
        assertEquals("result", XMLConstants.RESULT);
        assertEquals("<result>", XMLConstants.RESULT_ELE_START);
        assertEquals("</result>", XMLConstants.RESULT_ELE_END);
        assertEquals("<result isNull=\"true\" />", XMLConstants.RESULT_NULL_ELE);
        
        // Test data set constants
        assertEquals("dataSet", XMLConstants.DATA_SET);
        assertEquals("<dataSet>", XMLConstants.DATA_SET_ELE_START);
        assertEquals("</dataSet>", XMLConstants.DATA_SET_ELE_END);
        assertEquals("<dataSet isNull=\"true\" />", XMLConstants.DATA_SET_NULL_ELE);
        
        // Test SOAP envelope constants
        assertEquals("Envelope", XMLConstants.ENVELOPE);
        assertEquals("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">", XMLConstants.SOAP_ENVELOPE_ELE_START);
        assertEquals("</soap:Envelope>", XMLConstants.SOAP_ENVELOPE_ELE_END);
        
        assertEquals("Header", XMLConstants.HEADER);
        assertEquals("<soap:Header>", XMLConstants.SOAP_HEADER_ELE_START);
        assertEquals("</soap:Header>", XMLConstants.SOAP_HEADER_ELE_END);
        
        assertEquals("BODY", XMLConstants.BODY);
        assertEquals("<soap:Body>", XMLConstants.SOAP_BODY_ELE_START);
        assertEquals("</soap:Body>", XMLConstants.SOAP_BODY_ELE_END);
        
        assertEquals("Fault", XMLConstants.FAULT);
        assertEquals("<soap:Fault>", XMLConstants.SOAP_FAULT_ELE_START);
        assertEquals("</soap:Fault>", XMLConstants.SOAP_FAULT_ELE_END);
    }
}

// XMLParserImplTest.java
