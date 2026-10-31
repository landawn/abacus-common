package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Queue;
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
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

@Tag("unit")
public class XmlUtilCachedSAXParserTest {

    @TempDir
    Path directory;

    // Every SAX1/SAX2 overload must retire the builder without masking the original failure or calling reset.
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    @SuppressWarnings("deprecation")
    public void failedParsersAreNotRecycled(final int overload) throws Exception {
        final var field = XmlUtil.class.getDeclaredField("saxParserPool");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Queue<WeakReference<SAXParser>> pool = (Queue<WeakReference<SAXParser>>) field.get(null);
        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                for (final Throwable failure : new Throwable[] { new SAXException("parse"), new IOException("read"), new IllegalStateException("provider"),
                        new AssertionError("provider") }) {
                    final FailingParser delegate = new FailingParser(failure);
                    final SAXParser parser = wrap(delegate);
                    final HandlerBase legacy = new HandlerBase();
                    final DefaultHandler modern = new DefaultHandler();
                    assertSame(failure, assertThrows(Throwable.class, () -> {
                        switch (overload) {
                            case 0 -> parser.parse(InputStream.nullInputStream(), legacy);
                            case 1 -> parser.parse(InputStream.nullInputStream(), legacy, "urn:test");
                            case 2 -> parser.parse(InputStream.nullInputStream(), modern);
                            case 3 -> parser.parse(InputStream.nullInputStream(), modern, "urn:test");
                            case 4 -> parser.parse("urn:test", legacy);
                            case 5 -> parser.parse("urn:test", modern);
                            case 6 -> parser.parse(directory.resolve("document.xml").toFile(), legacy);
                            case 7 -> parser.parse(directory.resolve("document.xml").toFile(), modern);
                            case 8 -> parser.parse(new InputSource(), legacy);
                            default -> parser.parse(new InputSource(), modern);
                        }
                    }));
                    XmlUtil.recycleSAXParser(parser);
                    XmlUtil.recycleSAXParser(parser);
                    assertEquals(overload, delegate.calledOverload);
                    assertEquals(0, delegate.resets);
                    assertTrue(pool.isEmpty());
                }
            } finally {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }

    @Test
    public void successfulParseAfterFailureDoesNotRestorePoolEligibility() throws Exception {
        final SAXParser parser = XmlUtil.createSAXParser();
        assertThrows(SAXException.class, () -> parser.parse(new InputSource(new StringReader("<root>")), new DefaultHandler()));
        parser.reset();
        parser.parse(new InputSource(new StringReader("<root/>")), new DefaultHandler());
        XmlUtil.recycleSAXParser(parser);
        final var field = XmlUtil.class.getDeclaredField("saxParserPool");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Queue<WeakReference<SAXParser>> pool = (Queue<WeakReference<SAXParser>>) field.get(null);
        synchronized (pool) {
            assertTrue(pool.stream().noneMatch(reference -> reference.get() == parser));
        }
    }

    @SuppressWarnings("unchecked")
    private static Queue<WeakReference<SAXParser>> saxParserPool() throws ReflectiveOperationException {
        final var field = XmlUtil.class.getDeclaredField("saxParserPool");
        field.setAccessible(true);
        return (Queue<WeakReference<SAXParser>>) field.get(null);
    }

    private static void throwUnchecked(final Throwable failure) throws SAXException {
        if (failure instanceof SAXException e) {
            throw e;
        } else if (failure instanceof RuntimeException e) {
            throw e;
        }
        throw (Error) failure;
    }

    // Bug fix: a caller parsing through the exposed XMLReader or SAX1 Parser (JAXB, a SAXSource) bypassed failure
    // tracking, so recycling from a finally block pooled a failed parser. The exposed objects now record failures too:
    // parse errors, handler exceptions (also one thrown to stop early) and errors, through both parse overloads.
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    @SuppressWarnings("deprecation")
    public void failedParsesThroughExposedReadersAreNotRecycled(final int route) throws Exception {
        final Path document = Files.writeString(directory.resolve("reader.xml"), "<root/>");
        final String systemId = document.toUri().toString();
        final Queue<WeakReference<SAXParser>> pool = saxParserPool();

        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                for (final Throwable failure : new Throwable[] { new SAXException("handler"), new IllegalStateException("handler"),
                        new AssertionError("handler") }) {
                    final SAXParser parser = XmlUtil.createSAXParser();
                    final DefaultHandler modern = new DefaultHandler() {
                        @Override
                        public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes attributes)
                                throws SAXException {
                            throwUnchecked(failure);
                        }
                    };
                    final HandlerBase legacy = new HandlerBase() {
                        @Override
                        public void startElement(final String name, final org.xml.sax.AttributeList attributes) throws SAXException {
                            throwUnchecked(failure);
                        }
                    };

                    assertThrows(Throwable.class, () -> {
                        switch (route) {
                            case 0 -> {
                                final XMLReader reader = parser.getXMLReader();
                                reader.setContentHandler(modern);
                                reader.parse(new InputSource(new StringReader("<root/>")));
                            }
                            case 1 -> {
                                final XMLReader reader = parser.getXMLReader();
                                reader.setContentHandler(modern);
                                reader.parse(systemId);
                            }
                            case 2 -> {
                                final Parser sax1 = parser.getParser();
                                sax1.setDocumentHandler(legacy);
                                sax1.parse(new InputSource(new StringReader("<root/>")));
                            }
                            case 3 -> {
                                final Parser sax1 = parser.getParser();
                                sax1.setDocumentHandler(legacy);
                                sax1.parse(systemId);
                            }
                            default -> parser.getXMLReader().parse(new InputSource(new StringReader("<root>")));
                        }
                    });

                    XmlUtil.recycleSAXParser(parser);
                    assertTrue(pool.isEmpty(), "route " + route + ", " + failure);
                }
            } finally {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void successfulParsesThroughExposedReadersKeepTheParserPoolable() throws Exception {
        final Queue<WeakReference<SAXParser>> pool = saxParserPool();

        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                final SAXParser parser = XmlUtil.createSAXParser();
                final XMLReader reader = parser.getXMLReader();
                final Parser sax1 = parser.getParser();
                assertSame(reader, parser.getXMLReader());
                assertSame(sax1, parser.getParser());

                final AtomicInteger elements = new AtomicInteger();
                reader.setContentHandler(new DefaultHandler() {
                    @Override
                    public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes attributes) {
                        elements.incrementAndGet();
                    }
                });
                reader.parse(new InputSource(new StringReader("<root><a/></root>")));
                assertEquals(2, elements.get());
                sax1.setDocumentHandler(new HandlerBase());
                sax1.parse(new InputSource(new StringReader("<root/>")));

                XmlUtil.recycleSAXParser(parser);
                assertSame(parser, XmlUtil.createSAXParser(), "a parser whose reader parses succeeded is reused");
            } finally {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }

    // Bug fix: JAXP rejects a null source with IllegalArgumentException before any parse state exists; that argument
    // error no longer retires a pooled parser (every bad call used to cost the next caller a new parser).
    @Test
    @SuppressWarnings("deprecation")
    public void nullSourcesAreRejectedWithoutRetiringTheParser() throws Exception {
        final Queue<WeakReference<SAXParser>> pool = saxParserPool();

        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                final SAXParser parser = XmlUtil.createSAXParser();
                final DefaultHandler modern = new DefaultHandler();
                final HandlerBase legacy = new HandlerBase();
                assertThrows(IllegalArgumentException.class, () -> parser.parse((InputStream) null, legacy));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((InputStream) null, legacy, "urn:test"));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((InputStream) null, modern));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((InputStream) null, modern, "urn:test"));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((String) null, legacy));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((String) null, modern));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((File) null, legacy));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((File) null, modern));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((InputSource) null, legacy));
                assertThrows(IllegalArgumentException.class, () -> parser.parse((InputSource) null, modern));

                XmlUtil.recycleSAXParser(parser);
                assertSame(parser, XmlUtil.createSAXParser());

                // A real parse failure after such a call still retires the parser.
                assertThrows(SAXException.class, () -> parser.parse(new InputSource(new StringReader("<root>")), modern));
                XmlUtil.recycleSAXParser(parser);
                assertTrue(pool.isEmpty());
            } finally {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }

    // Unlike SAXParser's own overloads, SAX reader views have no guaranteed pre-parse null validation. Their failures retire the owner.
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3 })
    @SuppressWarnings("deprecation")
    public void nullSourceFailuresThroughExposedReadersRetireTheParser(final int route) throws Exception {
        final Queue<WeakReference<SAXParser>> pool = saxParserPool();
        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                final SAXParser parser = XmlUtil.createSAXParser();
                final XMLReader reader = parser.getXMLReader();
                final Parser sax1 = parser.getParser();
                // The provider chooses the exception type; the wrapper's contract is to discard after any parse failure.
                assertThrows(Exception.class, () -> {
                    switch (route) {
                        case 0 -> reader.parse((InputSource) null);
                        case 1 -> reader.parse((String) null);
                        case 2 -> sax1.parse((InputSource) null);
                        default -> sax1.parse((String) null);
                    }
                });
                XmlUtil.recycleSAXParser(parser);
                XmlUtil.recycleSAXParser(parser);
                assertTrue(pool.isEmpty(), "route " + route);
            } finally {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static final class FailingParser extends SAXParser {
        private final Throwable failure;
        private int calledOverload = -1;
        private int resets;

        FailingParser(final Throwable failure) {
            this.failure = failure;
        }

        private void fail(final int overload) throws SAXException, IOException {
            calledOverload = overload;
            if (failure instanceof SAXException e) {
                throw e;
            } else if (failure instanceof IOException e) {
                throw e;
            } else if (failure instanceof RuntimeException e) {
                throw e;
            } else {
                throw (Error) failure;
            }
        }

        @Override
        public void parse(final InputStream input, final HandlerBase handler) throws SAXException, IOException {
            fail(0);
        }

        @Override
        public void parse(final InputStream input, final HandlerBase handler, final String systemId) throws SAXException, IOException {
            fail(1);
        }

        @Override
        public void parse(final InputStream input, final DefaultHandler handler) throws SAXException, IOException {
            fail(2);
        }

        @Override
        public void parse(final InputStream input, final DefaultHandler handler, final String systemId) throws SAXException, IOException {
            fail(3);
        }

        @Override
        public void parse(final String uri, final HandlerBase handler) throws SAXException, IOException {
            fail(4);
        }

        @Override
        public void parse(final String uri, final DefaultHandler handler) throws SAXException, IOException {
            fail(5);
        }

        @Override
        public void parse(final File file, final HandlerBase handler) throws SAXException, IOException {
            fail(6);
        }

        @Override
        public void parse(final File file, final DefaultHandler handler) throws SAXException, IOException {
            fail(7);
        }

        @Override
        public void parse(final InputSource input, final HandlerBase handler) throws SAXException, IOException {
            fail(8);
        }

        @Override
        public void parse(final InputSource input, final DefaultHandler handler) throws SAXException, IOException {
            fail(9);
        }

        @Override
        public void reset() {
            resets++;
        }

        @Override
        public Parser getParser() {
            throw new AssertionError("Unexpected reader lookup");
        }

        @Override
        public XMLReader getXMLReader() {
            throw new AssertionError("Unexpected reader lookup");
        }

        @Override
        public boolean isNamespaceAware() {
            return false;
        }

        @Override
        public boolean isValidating() {
            return false;
        }

        @Override
        public void setProperty(final String name, final Object value) {
            throw new AssertionError("Unexpected property write");
        }

        @Override
        public Object getProperty(final String name) {
            throw new AssertionError("Unexpected property lookup");
        }
    }

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
        // The exposed reader is a stable view that forwards to the provider's own reader (it records failed parses).
        final XMLReader reader = parser.getXMLReader();
        assertSame(reader, parser.getXMLReader());
        final DefaultHandler handler = new DefaultHandler();
        reader.setContentHandler(handler);
        assertSame(handler, delegate.getXMLReader().getContentHandler());
        assertEquals(delegate.getXMLReader().getFeature("http://xml.org/sax/features/namespaces"), reader.getFeature("http://xml.org/sax/features/namespaces"));
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
