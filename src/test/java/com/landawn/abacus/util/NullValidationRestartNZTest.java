package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.DataInput;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.landawn.abacus.TestBase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class NullValidationRestartNZTest extends TestBase {
    public static class Bean {
        public String value = "initial";

        public String value() {
            return value;
        }
    }

    private static void rejects(final String parameter, final Executable action) {
        assertTrue(assertThrows(IllegalArgumentException.class, action).getMessage().contains(parameter));
    }

    @Test
    public void reflectionRejectsNullNamesBeforeResolvingMembers() {
        final Bean bean = new Bean();
        final Reflection<Bean> reflection = Reflection.on(bean);
        rejects("fieldName", () -> reflection.get(null));
        rejects("fieldName", () -> reflection.set(null, "x"));
        rejects("methodName", () -> reflection.invoke(null));
        rejects("methodName", () -> reflection.call(null));
        assertEquals("initial", bean.value);
        reflection.set("value", null);
        assertNull(reflection.get("value"));
        assertNull(reflection.invoke("value"));

        final ReflectASM<Bean> asm = ReflectASM.on(bean);
        rejects("fieldName", () -> asm.get((String) null));
        rejects("fieldName", () -> asm.set((String) null, "x"));
        rejects("methodName", () -> asm.invoke(null));
        rejects("methodName", () -> asm.call(null));
        asm.set("value", "changed");
        assertEquals("changed", asm.invoke("value"));
        asm.set("value", null);
        assertNull(asm.get("value"));
    }

    @Test
    public void domHelpersRejectMissingNodesAndAttributeNames() throws Exception {
        rejects("node", () -> XmlUtil.getElementsByTagName(null, "child"));
        rejects("node", () -> XmlUtil.getNodesByName(null, "child"));
        rejects("node", () -> XmlUtil.getNextNodeByName(null, "child"));
        rejects("node", () -> XmlUtil.getAttribute(null, "id"));
        rejects("node", () -> XmlUtil.readAttributes(null));
        rejects("element", () -> XmlUtil.readElement(null));

        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element root = document.createElement("root");
        document.appendChild(root);
        rejects("attrName", () -> XmlUtil.getAttribute(root, null));
        root.setAttribute("id", "42");
        final Element child = document.createElement("child");
        root.appendChild(child);
        assertEquals("42", XmlUtil.getAttribute(root, "id"));
        assertEquals(1, XmlUtil.getElementsByTagName(root, "child").size());
        assertTrue(XmlUtil.getElementsByTagName(root, null).isEmpty());
        assertTrue(XmlUtil.getNodesByName(root, null).isEmpty());
        assertNull(XmlUtil.getNextNodeByName(root, null));
    }

    @Test
    public void staxFactoriesValidateSourcesAndOutputs() throws Exception {
        rejects("source", () -> XmlUtil.createXMLStreamReader((Reader) null));
        rejects("source", () -> XmlUtil.createXMLStreamReader((InputStream) null));
        rejects("source", () -> XmlUtil.createXMLStreamReader((InputStream) null, "UTF-8"));
        rejects("output", () -> XmlUtil.createXMLStreamWriter((Writer) null));
        rejects("output", () -> XmlUtil.createXMLStreamWriter((OutputStream) null));
        rejects("output", () -> XmlUtil.createXMLStreamWriter((OutputStream) null, "UTF-8"));
        rejects("source", () -> XmlUtil.createFilteredStreamReader(null, reader -> true));

        final XMLStreamReader reader = XmlUtil.createXMLStreamReader(new StringReader("<root/>"));
        try {
            rejects("filter", () -> XmlUtil.createFilteredStreamReader(reader, null));
            reader.nextTag();
            assertEquals("root", reader.getLocalName());
        } finally {
            reader.close();
        }
        final StringWriter output = new StringWriter();
        final var writer = XmlUtil.createXMLStreamWriter(output);
        writer.writeEmptyElement("root");
        writer.close();
        assertTrue(output.toString().contains("root"));
    }

    @Test
    public void propertiesInputsFailAtThePublicBoundary() {
        rejects("source", () -> PropertiesUtil.load((File) null));
        rejects("source", () -> PropertiesUtil.load((File) null, true));
        rejects("source", () -> PropertiesUtil.load((InputStream) null));
        rejects("source", () -> PropertiesUtil.load((Reader) null));
        rejects("source", () -> PropertiesUtil.loadFromXml((File) null));
        rejects("source", () -> PropertiesUtil.loadFromXml((File) null, true));
        rejects("source", () -> PropertiesUtil.loadFromXml((File) null, Properties.class));
        rejects("source", () -> PropertiesUtil.loadFromXml((InputStream) null));
        rejects("source", () -> PropertiesUtil.loadFromXml((Reader) null));
        rejects("xml", () -> PropertiesUtil.xmlToJava((String) null, "unused", null, null, false));
        rejects("xml", () -> PropertiesUtil.xmlToJava((File) null, "unused", null, null, false));
        rejects("xml", () -> PropertiesUtil.xmlToJava((InputStream) null, "unused", null, null, false));
        rejects("xml", () -> PropertiesUtil.xmlToJava((Reader) null, "unused", null, null, false));
        assertEquals("value", PropertiesUtil.load(new StringReader("key=value")).get("key"));
        assertNotNull(PropertiesUtil.loadFromXml(new StringReader("<properties/>"), null));
    }

    @Test
    public void xmlMappersValidateGenericTargetBeforeReadingSources() {
        final TypeReference<Bean> missingType = null;
        final String xml = "<Bean><value>ok</value></Bean>";
        final byte[] bytes = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        rejects("targetType", () -> XmlMappers.fromXml(xml, missingType));
        rejects("targetType", () -> XmlMappers.fromXml(xml, missingType, (DeserializationConfig) null));
        rejects("targetType", () -> XmlMappers.fromXml(bytes, missingType));
        rejects("targetType", () -> XmlMappers.fromXml(bytes, 0, bytes.length, missingType));
        rejects("targetType", () -> XmlMappers.fromXml(new File("missing-null-validation-test.xml"), missingType));
        rejects("targetType", () -> XmlMappers.fromXml(new ByteArrayInputStream(bytes), missingType));
        rejects("targetType", () -> XmlMappers.fromXml(new StringReader(xml), missingType));
        rejects("targetType", () -> XmlMappers.fromXml((DataInput) null, missingType));
        final var mapper = XmlMappers.wrap(new XmlMapper());
        rejects("targetType", () -> mapper.fromXml(xml, missingType));
        rejects("targetType", () -> mapper.fromXml(bytes, 0, bytes.length, missingType));
        rejects("targetType", () -> mapper.fromXml(new File("missing-null-validation-test.xml"), missingType));
        rejects("targetType", () -> mapper.fromXml(new StringReader(xml), missingType));
        rejects("targetType", () -> mapper.fromXml((DataInput) null, missingType));
        assertEquals("ok", XmlMappers.fromXml(xml, new TypeReference<Bean>() {
        }, (DeserializationConfig) null).value);
        assertEquals("ok", mapper.fromXml(xml, new TypeReference<Bean>() {
        }).value);
    }

    @Test
    public void dispatcherConstructionRequiresBothCallbacks() {
        rejects("onError", () -> new Observer.DispatcherBase<Object>(null, () -> {
        }) {
        });
        rejects("onComplete", () -> new Observer.DispatcherBase<Object>(error -> {
        }, null) {
        });
        final AtomicInteger errors = new AtomicInteger();
        final AtomicInteger completions = new AtomicInteger();
        final Observer.DispatcherBase<Object> dispatcher = new Observer.DispatcherBase<>(error -> errors.incrementAndGet(), completions::incrementAndGet) {
        };
        dispatcher.onNext(null);
        dispatcher.onError(new Exception("test"));
        dispatcher.onComplete();
        assertEquals(1, errors.get());
        assertEquals(1, completions.get());
    }
}
