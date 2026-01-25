package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class XmlConstants2025Test extends TestBase {

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
}
