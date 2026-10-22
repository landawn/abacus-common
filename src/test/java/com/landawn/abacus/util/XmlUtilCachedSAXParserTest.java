package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

@Tag("unit")
public class XmlUtilCachedSAXParserTest {

    @TempDir
    Path directory;

    private static SAXParser wrap(final SAXParser delegate) throws ReflectiveOperationException {
        final var constructor = Class.forName(XmlUtil.class.getName() + "$CachedSAXParser").getDeclaredConstructor(SAXParser.class);
        constructor.setAccessible(true);
        return (SAXParser) constructor.newInstance(delegate);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void preservesProviderCapabilities(final boolean enabled) throws Exception {
        final SAXParserFactory factory = SAXParserFactory.newDefaultInstance();
        factory.setNamespaceAware(enabled);
        factory.setValidating(enabled);
        factory.setXIncludeAware(enabled);
        final Schema schema = enabled ? SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema() : null;
        factory.setSchema(schema);
        final SAXParser delegate = factory.newSAXParser();
        final SAXParser parser = wrap(delegate);

        assertEquals(enabled, parser.isNamespaceAware());
        assertEquals(enabled, parser.isValidating());
        assertEquals(enabled, parser.isXIncludeAware());
        assertSame(schema, parser.getSchema());
        assertSame(delegate.getXMLReader(), parser.getXMLReader());
        assertThrows(SAXNotRecognizedException.class, () -> parser.getProperty("urn:unknown-property"));
        assertThrows(SAXNotRecognizedException.class, () -> parser.setProperty("urn:unknown-property", new Object()));
    }

    @Test
    public void resetsProviderHandlersAndFeatures() throws Exception {
        final SAXParserFactory factory = SAXParserFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        final SAXParser parser = wrap(factory.newSAXParser());
        final XMLReader reader = parser.getXMLReader();
        final Object originalResolver = reader.getEntityResolver();
        final Object originalErrorHandler = reader.getErrorHandler();
        final DefaultHandler handler = new DefaultHandler();
        parser.parse(new InputSource(new StringReader("<root/>")), handler);
        assertSame(handler, reader.getContentHandler());
        assertSame(handler, reader.getEntityResolver());
        reader.setFeature("http://xml.org/sax/features/namespaces", false);

        // Reset must reach the provider rather than the unsupported default on SAXParser.
        parser.reset();
        assertNull(reader.getContentHandler());
        assertNull(reader.getDTDHandler());
        assertSame(originalResolver, reader.getEntityResolver());
        assertSame(originalErrorHandler, reader.getErrorHandler());
        assertTrue(reader.getFeature("http://xml.org/sax/features/namespaces"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void parsesAllInputFormsWithBothHandlerApis() throws Exception {
        final SAXParser parser = wrap(SAXParserFactory.newDefaultInstance().newSAXParser());
        final String xml = "<root>value</root>";
        final byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        final Path file = Files.writeString(directory.resolve("document.xml"), xml);
        final String systemId = file.toUri().toString();
        final AtomicInteger modernDocuments = new AtomicInteger();
        final AtomicInteger legacyDocuments = new AtomicInteger();
        final DefaultHandler modern = new DefaultHandler() {
            @Override
            public void startDocument() {
                modernDocuments.incrementAndGet();
            }
        };
        final HandlerBase legacy = new HandlerBase() {
            @Override
            public void startDocument() {
                legacyDocuments.incrementAndGet();
            }
        };

        parser.parse(new ByteArrayInputStream(bytes), modern);
        parser.parse(new ByteArrayInputStream(bytes), modern, systemId);
        parser.parse(systemId, modern);
        parser.parse(file.toFile(), modern);
        parser.parse(new InputSource(new StringReader(xml)), modern);
        assertEquals(5, modernDocuments.get());

        parser.parse(new ByteArrayInputStream(bytes), legacy);
        parser.parse(new ByteArrayInputStream(bytes), legacy, systemId);
        parser.parse(systemId, legacy);
        parser.parse(file.toFile(), legacy);
        parser.parse(new InputSource(new StringReader(xml)), legacy);
        assertEquals(5, legacyDocuments.get());
        assertEquals(5, modernDocuments.get(), "switching to SAX1 must not keep invoking the previous SAX2 handler");
    }

    @Test
    public void preservesParsingFailures() throws Exception {
        final SAXParser parser = wrap(SAXParserFactory.newDefaultInstance().newSAXParser());
        final DefaultHandler handler = new DefaultHandler();
        assertThrows(IllegalArgumentException.class, () -> parser.parse((InputStream) null, handler));
        assertThrows(SAXException.class, () -> parser.parse(new InputSource(new StringReader("<root>")), handler));
        final IOException failure = new IOException("Cannot read input");
        final InputStream failingInput = new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IOException.class, () -> parser.parse(failingInput, handler)));
    }
}
