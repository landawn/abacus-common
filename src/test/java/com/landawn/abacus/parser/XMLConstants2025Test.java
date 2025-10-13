package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class XMLConstants2025Test extends TestBase {

    @Test
    public void test_NULL_END_ELEMENT() {
        assertEquals(-128, XMLConstants.NULL_END_ELEMENT);
    }

    @Test
    public void test_IS_NULL() {
        assertEquals("isNull", XMLConstants.IS_NULL);
    }

    @Test
    public void test_TRUE() {
        assertEquals("true", XMLConstants.TRUE);
    }

    @Test
    public void test_FALSE() {
        assertEquals("false", XMLConstants.FALSE);
    }

    @Test
    public void test_IS_NULL_ATTR() {
        assertEquals(" isNull=\"true\"", XMLConstants.IS_NULL_ATTR);
    }

    @Test
    public void test_NAME() {
        assertEquals("name", XMLConstants.NAME);
    }

    @Test
    public void test_TYPE() {
        assertEquals("type", XMLConstants.TYPE);
    }

    @Test
    public void test_VERSION() {
        assertEquals("version", XMLConstants.VERSION);
    }

    @Test
    public void test_SIZE() {
        assertEquals("size", XMLConstants.SIZE);
    }

    @Test
    public void test_ARRAY_constants() {
        assertEquals("array", XMLConstants.ARRAY);
        assertEquals("<array>", XMLConstants.ARRAY_ELE_START);
        assertEquals("</array>", XMLConstants.ARRAY_ELE_END);
        assertEquals("<array isNull=\"true\" />", XMLConstants.ARRAY_NULL_ELE);
    }

    @Test
    public void test_LIST_constants() {
        assertEquals("list", XMLConstants.LIST);
        assertEquals("<list>", XMLConstants.LIST_ELE_START);
        assertEquals("</list>", XMLConstants.LIST_ELE_END);
        assertEquals("<list isNull=\"true\" />", XMLConstants.LIST_NULL_ELE);
    }

    @Test
    public void test_SET_constants() {
        assertEquals("set", XMLConstants.SET);
        assertEquals("<set>", XMLConstants.SET_ELE_START);
        assertEquals("</set>", XMLConstants.SET_ELE_END);
        assertEquals("<set isNull=\"true\" />", XMLConstants.SET_NULL_ELE);
    }

    @Test
    public void test_MAP_constants() {
        assertEquals("map", XMLConstants.MAP);
        assertEquals("<map>", XMLConstants.MAP_ELE_START);
        assertEquals("</map>", XMLConstants.MAP_ELE_END);
        assertEquals("<map isNull=\"true\" />", XMLConstants.MAP_NULL_ELE);
    }

    @Test
    public void test_ENTRY_constants() {
        assertEquals("entry", XMLConstants.ENTRY);
        assertEquals("<entry>", XMLConstants.ENTRY_ELE_START);
        assertEquals("</entry>", XMLConstants.ENTRY_ELE_END);
    }

    @Test
    public void test_KEY_constants() {
        assertEquals("key", XMLConstants.KEY);
        assertEquals("<key>", XMLConstants.KEY_ELE_START);
        assertEquals("</key>", XMLConstants.KEY_ELE_END);
        assertEquals("<key isNull=\"true\" />", XMLConstants.KEY_NULL_ELE);
    }

    @Test
    public void test_VALUE_constants() {
        assertEquals("value", XMLConstants.VALUE);
        assertEquals("<value>", XMLConstants.VALUE_ELE_START);
        assertEquals("</value>", XMLConstants.VALUE_ELE_END);
        assertEquals("<value isNull=\"true\" />", XMLConstants.VALUE_NULL_ELE);
    }

    @Test
    public void test_ENTITY_constants() {
        assertEquals("bean", XMLConstants.ENTITY);
        assertEquals("<bean>", XMLConstants.ENTITY_ELE_START);
        assertEquals("</bean>", XMLConstants.ENTITY_ELE_END);
        assertEquals("<bean isNull=\"true\" />", XMLConstants.ENTITY_NULL_ELE);
    }

    @Test
    public void test_PROPERTY_constants() {
        assertEquals("property", XMLConstants.PROPERTY);
        assertEquals("<property>", XMLConstants.PROPERTY_ELE_START);
        assertEquals("</property>", XMLConstants.PROPERTY_ELE_END);
        assertEquals("<property isNull=\"true\" />", XMLConstants.PROPERTY_NULL_ELE);
    }

    @Test
    public void test_SOAP_ENVELOPE_constants() {
        assertEquals("Envelope", XMLConstants.ENVELOPE);
        assertNotNull(XMLConstants.SOAP_ENVELOPE_ELE_START);
        assertEquals("</soap:Envelope>", XMLConstants.SOAP_ENVELOPE_ELE_END);
    }

    @Test
    public void test_SOAP_HEADER_constants() {
        assertEquals("Header", XMLConstants.HEADER);
        assertEquals("<soap:Header>", XMLConstants.SOAP_HEADER_ELE_START);
        assertEquals("</soap:Header>", XMLConstants.SOAP_HEADER_ELE_END);
    }

    @Test
    public void test_SOAP_BODY_constants() {
        assertEquals("BODY", XMLConstants.BODY);
        assertEquals("<soap:Body>", XMLConstants.SOAP_BODY_ELE_START);
        assertEquals("</soap:Body>", XMLConstants.SOAP_BODY_ELE_END);
    }

    @Test
    public void test_SOAP_FAULT_constants() {
        assertEquals("Fault", XMLConstants.FAULT);
        assertEquals("<soap:Fault>", XMLConstants.SOAP_FAULT_ELE_START);
        assertEquals("</soap:Fault>", XMLConstants.SOAP_FAULT_ELE_END);
    }

    @Test
    public void test_charArrayConstants() {
        assertNotNull(XMLConstants.START_ARRAY_ELE_WITH_TYPE);
        assertNotNull(XMLConstants.START_LIST_ELE_WITH_TYPE);
        assertNotNull(XMLConstants.START_SET_ELE_WITH_TYPE);
        assertNotNull(XMLConstants.START_MAP_ELE_WITH_TYPE);
        assertNotNull(XMLConstants.START_KEY_ELE_WITH_TYPE);
        assertNotNull(XMLConstants.START_VALUE_ELE_WITH_TYPE);
        assertNotNull(XMLConstants.CLOSE_ATTR_AND_ELE);
        assertNotNull(XMLConstants.END_ELEMENT);
    }
}
