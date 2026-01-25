package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class XmlConstants100Test extends TestBase {

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
