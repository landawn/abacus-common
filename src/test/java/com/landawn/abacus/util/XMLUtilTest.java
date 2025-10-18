/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.helpers.DefaultHandler;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.PersonType;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

public class XMLUtilTest extends AbstractParserTest {

    @Test
    public void test_writerCharacter() throws Exception {
        final String str = "<> \" \\ /";
        final String xml = "&lt;&gt; &quot; \\ /";

        StringBuilder sb = new StringBuilder();
        XmlUtil.writeCharacters(str, sb);
        N.println(sb.toString());
        assertEquals(xml, sb.toString());

        sb = new StringBuilder();
        XmlUtil.writeCharacters(str.toCharArray(), sb);
        N.println(sb.toString());
        assertEquals(xml, sb.toString());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmlUtil.writeCharacters(str, os);
        N.println(sb.toString());
        assertEquals(xml, os.toString());

        os = new ByteArrayOutputStream();
        XmlUtil.writeCharacters(str.toCharArray(), os);
        N.println(os.toString());
        assertEquals(xml, os.toString());

        Writer writer = new StringWriter();
        XmlUtil.writeCharacters(str, writer);
        N.println(writer.toString());
        assertEquals(xml, writer.toString());

        writer = new StringWriter();
        XmlUtil.writeCharacters(str.toCharArray(), writer);
        N.println(writer.toString());
        assertEquals(xml, writer.toString());
    }

    @Test
    public void test_node() throws Exception {
        final Account account = createAccount(Account.class);
        account.setLastName("   aaa \n \r \\ bbb \t ccc    ");

        final AccountContact contact = new AccountContact();
        contact.setCity("sunnyvale");
        account.setContact(contact);

        final String xml = abacusXMLParser.serialize(account);
        N.println(xml);

        final Document doc = XmlUtil.createDOMParser().parse(new ByteArrayInputStream(xml.getBytes()));

        final Element element = doc.getDocumentElement();
        final List<Element> elements = XmlUtil.getElementsByTagName(element, "firstName");
        assertEquals("firstName", elements.get(0).getNodeName());

        final List<Node> nodes = XmlUtil.getNodesByName(element, "firstName");
        assertEquals("firstName", nodes.get(0).getNodeName());

        final Node node = XmlUtil.getNextNodeByName(element, "firstName");
        assertEquals("firstName", node.getNodeName());

        final Node node2 = XmlUtil.getNextNodeByName(node, "firstName");
        assertTrue(node == node2);

        final Node node3 = XmlUtil.getNextNodeByName(element, "city");
        assertEquals("sunnyvale", node3.getTextContent());

        assertNull(XmlUtil.getAttribute(node, "non-exist"));
        assertTrue(XmlUtil.isTextElement(node));

        assertEquals(account.getFirstName(), XmlUtil.getTextContent(node));
        assertEquals(account.getFirstName(), XmlUtil.getTextContent(node, true));
        assertEquals(account.getFirstName(), XmlUtil.getTextContent(node, false));

        final Node lastNameNode = XmlUtil.getNextNodeByName(element, "lastName");
        N.println(XmlUtil.getTextContent(lastNameNode, true));
        N.println(XmlUtil.getTextContent(lastNameNode, false));

        assertEquals("aaa   \\ bbb  ccc", XmlUtil.getTextContent(lastNameNode, true));
        assertEquals(account.getLastName(), XmlUtil.getTextContent(lastNameNode, false));
    }

    @Test
    public void test_xmlEncode() throws Exception {
        final Account account = createAccount(Account.class);
        final String xml = XmlUtil.xmlEncode(account);
        N.println(xml);

        final Account account2 = XmlUtil.xmlDecode(xml);
        N.println(account2);
        // assertEquals(account, account2); // TODO can't recognize the fluent setter method in bean class
    }

    @Test
    public void test_stransfermer() throws Exception {
        final Transformer transfermer = XmlUtil.createXMLTransformer();
        transfermer.getOutputProperties();

        final Account account = createAccount(Account.class);
        final String xml = abacusXMLParser.serialize(account);

        final DocumentBuilder docBuilder = XmlUtil.createDOMParser();
        final Document doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
        final File file = new File("./src/test/resources/xml.xml");
        XmlUtil.transform(doc, file);

        String str = IOUtil.readAllToString(file);
        N.println(str);
        assertEquals(xml, str.substring(54));

        // IOUtil.delete(file);
        final OutputStream os = new FileOutputStream(file);
        XmlUtil.transform(doc, os);
        os.flush();
        IOUtil.close(os);
        str = IOUtil.readAllToString(file);
        N.println(str);
        assertEquals(xml, str.substring(54));
        IOUtil.deleteAllIfExists(file);
    }

    @Test
    public void test_xmlStreamReader_writer() throws Exception {
        final Account account = createAccount(Account.class);
        final String xml = abacusXMLParser.serialize(account);
        XMLStreamReader reader = XmlUtil.createXMLStreamReader(new ByteArrayInputStream(xml.getBytes()));
        reader.next();
        N.println(reader.getLocalName());
        assertEquals("account", reader.getLocalName());

        reader = XmlUtil.createXMLStreamReader(new ByteArrayInputStream(xml.getBytes()), Charsets.DEFAULT.toString());
        reader.next();
        N.println(reader.getLocalName());
        assertEquals("account", reader.getLocalName());

        reader = XmlUtil.createXMLStreamReader(new StringReader(xml));
        reader.next();
        N.println(reader.getLocalName());
        assertEquals("account", reader.getLocalName());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = XmlUtil.createXMLStreamWriter(os);
        writer.writeStartElement("a");
        writer.writeCharacters(xml);
        writer.writeEndElement();
        writer.flush();
        N.println(new String(os.toByteArray()));
        IOUtil.close(os);

        os = new ByteArrayOutputStream();
        writer = XmlUtil.createXMLStreamWriter(os, Charsets.UTF_16.toString());
        writer.writeStartElement("a");
        writer.writeCharacters(xml);
        writer.writeEndElement();
        writer.flush();
        N.println(new String(os.toByteArray(), Charsets.UTF_16));
        IOUtil.close(os);

        final StringWriter w = new StringWriter();
        writer = XmlUtil.createXMLStreamWriter(w);
        writer.writeStartElement("a");
        writer.writeCharacters(xml);
        writer.writeEndElement();
        writer.flush();
        N.println(w.toString());
    }

    @Test
    public void test_createSAXParser() throws Exception {
        final PersonType personType = createPerson();
        final Marshaller marshaller = XmlUtil.createMarshaller(PersonType.class);
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        marshaller.marshal(personType, os);

        final String str = os.toString();
        N.println(str);
        Objectory.recycle(os);

        final SAXParser saxParser = XmlUtil.createSAXParser();
        saxParser.parse(new ByteArrayInputStream(str.getBytes()), new DefaultHandler());

        XmlUtil.recycleSAXParser(saxParser);
    }

    @Test
    public void test_createDOMParser() throws Exception {
        final PersonType personType = createPerson();
        final Marshaller marshaller = XmlUtil.createMarshaller(PersonType.class);
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        marshaller.marshal(personType, os);

        final String str = os.toString();
        N.println(str);
        Objectory.recycle(os);

        final DocumentBuilder docBuilder = XmlUtil.createDOMParser();
        final Document doc = docBuilder.parse(new ByteArrayInputStream(str.getBytes()));

        final Element element = doc.getDocumentElement();

        for (int i = 0; i < element.getChildNodes().getLength(); i++) {
            N.println(element.getChildNodes().item(i).getNodeName());
        }

        assertEquals(8, element.getChildNodes().getLength());
    }

    @Test
    public void test_createDOMParser_2() throws Exception {
        final PersonType personType = createPerson();
        final Marshaller marshaller = XmlUtil.createMarshaller(PersonType.class);
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        marshaller.marshal(personType, os);

        final String str = os.toString();
        N.println(str);
        Objectory.recycle(os);

        final DocumentBuilder docBuilder = XmlUtil.createDOMParser(false, false);
        final Document doc = docBuilder.parse(new ByteArrayInputStream(str.getBytes()));

        final Element element = doc.getDocumentElement();

        for (int i = 0; i < element.getChildNodes().getLength(); i++) {
            N.println(element.getChildNodes().item(i).getNodeName());
        }

        assertEquals(8, element.getChildNodes().getLength());
    }

    @Test
    public void test_createContentParser() throws Exception {
        final PersonType personType = createPerson();
        final Marshaller marshaller = XmlUtil.createMarshaller(PersonType.class);
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        marshaller.marshal(personType, os);

        final String str = os.toString();
        N.println(str);
        Objectory.recycle(os);

        final DocumentBuilder docBuilder = XmlUtil.createContentParser();
        final Document doc = docBuilder.parse(new ByteArrayInputStream(str.getBytes()));

        final Element element = doc.getDocumentElement();

        for (int i = 0; i < element.getChildNodes().getLength(); i++) {
            N.println(element.getChildNodes().item(i).getNodeName());
        }

        assertEquals(8, element.getChildNodes().getLength());

        XmlUtil.recycleContentParser(docBuilder);
    }

    @Test
    public void test_createMarshall() throws Exception {
        final PersonType personType = createPerson();
        final Marshaller marshaller = XmlUtil.createMarshaller(PersonType.class);
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        marshaller.marshal(personType, os);

        final String str = os.toString();
        N.println(str);
        Objectory.recycle(os);

        final Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(PersonType.class);

        final PersonType personType2 = (PersonType) unmarshaller.unmarshal(new ByteArrayInputStream(str.getBytes()));

        CommonUtil.equals(personType, personType2);
    }

    @Test
    public void test_createMarshall_2() throws Exception {
        final PersonType personType = createPerson();
        final Marshaller marshaller = XmlUtil.createMarshaller(PersonType.class.getPackage().getName());
        final ByteArrayOutputStream os = Objectory.createByteArrayOutputStream();
        marshaller.marshal(personType, os);

        final String str = os.toString();
        N.println(str);
        Objectory.recycle(os);

        final Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(PersonType.class.getPackage().getName());

        final PersonType personType2 = (PersonType) unmarshaller.unmarshal(new ByteArrayInputStream(str.getBytes()));

        CommonUtil.equals(personType, personType2);
    }

    @Test
    public void test_marshall() {
        final PersonType personType = createPerson();
        final String str = XmlUtil.marshal(personType);
        N.println(str);

        final PersonType personType2 = XmlUtil.unmarshal(PersonType.class, str);

        CommonUtil.equals(personType, personType2);
    }
}
