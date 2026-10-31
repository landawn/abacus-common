package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@Tag("unit")
public class XmlUtilCachedDocumentBuilderTest {

    @TempDir
    Path directory;

    private static DocumentBuilder wrap(final DocumentBuilder delegate) throws ReflectiveOperationException {
        final var constructor = Class.forName(XmlUtil.class.getName() + "$CachedDocumentBuilder")
                .asSubclass(DocumentBuilder.class)
                .getDeclaredConstructor(DocumentBuilder.class);
        constructor.setAccessible(true);
        return constructor.newInstance(delegate);
    }

    private static void assertParsedContent(final Document document) {
        assertEquals("urn:test", document.getDocumentElement().getNamespaceURI());
        assertEquals("root", document.getDocumentElement().getLocalName());
        assertEquals("value", document.getDocumentElement().getTextContent());
    }

    // A provider need not be reusable after a failed parse. Check every forwarding overload and failure kind,
    // including errors, without relying on the JDK provider's recovery or on garbage collection.
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    public void discardsFailedParsersWithoutResettingThem(final int overload) throws Exception {
        final var poolField = XmlUtil.class.getDeclaredField("contentDocBuilderPool");
        poolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Queue<WeakReference<DocumentBuilder>> pool = (Queue<WeakReference<DocumentBuilder>>) poolField.get(null);
        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                for (final Throwable failure : new Throwable[] { new SAXException("parse failed"), new IOException("read failed"),
                        new IllegalStateException("provider failed"), new AssertionError("provider error") }) {
                    final FailingBuilder delegate = new FailingBuilder(failure);
                    final DocumentBuilder builder = wrap(delegate);
                    assertSame(failure, assertThrows(Throwable.class, () -> {
                        switch (overload) {
                            case 0 -> builder.parse(InputStream.nullInputStream());
                            case 1 -> builder.parse(InputStream.nullInputStream(), "urn:test");
                            case 2 -> builder.parse("urn:test");
                            case 3 -> builder.parse(directory.resolve("document.xml").toFile());
                            default -> builder.parse(new InputSource(new StringReader("<root/>")));
                        }
                    }));
                    XmlUtil.recycleContentParser(builder);
                    XmlUtil.recycleContentParser(builder);
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

    private static final class FailingBuilder extends DocumentBuilder {
        private final Throwable failure;
        private int calledOverload = -1;
        private int resets;

        FailingBuilder(final Throwable failure) {
            this.failure = failure;
        }

        private Document fail(final int overload) throws SAXException, IOException {
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
        public Document parse(final InputStream input) throws SAXException, IOException {
            return fail(0);
        }

        @Override
        public Document parse(final InputStream input, final String systemId) throws SAXException, IOException {
            return fail(1);
        }

        @Override
        public Document parse(final String uri) throws SAXException, IOException {
            return fail(2);
        }

        @Override
        public Document parse(final File file) throws SAXException, IOException {
            return fail(3);
        }

        @Override
        public Document parse(final InputSource input) throws SAXException, IOException {
            return fail(4);
        }

        @Override
        public void reset() {
            resets++;
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
        public void setEntityResolver(final EntityResolver resolver) {
        }

        @Override
        public void setErrorHandler(final ErrorHandler handler) {
        }

        @Override
        public Document newDocument() {
            throw new AssertionError("Unexpected document creation");
        }

        @Override
        public DOMImplementation getDOMImplementation() {
            throw new AssertionError("Unexpected DOM lookup");
        }
    }

    @Test
    public void successfulParseAfterFailureDoesNotMakeBuilderPoolableAgain() throws Exception {
        final DocumentBuilder builder = wrap(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder());
        builder.setErrorHandler(new DefaultHandler());
        assertThrows(SAXException.class, () -> builder.parse(new InputSource(new StringReader("<root>"))));
        builder.reset();
        assertEquals("root", builder.parse(new InputSource(new StringReader("<root/>"))).getDocumentElement().getNodeName());
        XmlUtil.recycleContentParser(builder);
        final var poolField = XmlUtil.class.getDeclaredField("contentDocBuilderPool");
        poolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Queue<WeakReference<DocumentBuilder>> pool = (Queue<WeakReference<DocumentBuilder>>) poolField.get(null);
        synchronized (pool) {
            assertTrue(pool.stream().noneMatch(reference -> reference.get() == builder));
        }
    }

    // Bug fix: JAXP rejects a null source with IllegalArgumentException before any parse state exists; that argument
    // error no longer retires a pooled builder. A real failure afterwards still does.
    @Test
    public void nullSourcesAreRejectedWithoutRetiringTheBuilder() throws Exception {
        final var poolField = XmlUtil.class.getDeclaredField("contentDocBuilderPool");
        poolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Queue<WeakReference<DocumentBuilder>> pool = (Queue<WeakReference<DocumentBuilder>>) poolField.get(null);
        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                final DocumentBuilder builder = XmlUtil.createContentParser();
                builder.setErrorHandler(new DefaultHandler());
                assertThrows(IllegalArgumentException.class, () -> builder.parse((InputStream) null));
                assertThrows(IllegalArgumentException.class, () -> builder.parse(null, "urn:test"));
                assertThrows(IllegalArgumentException.class, () -> builder.parse((String) null));
                assertThrows(IllegalArgumentException.class, () -> builder.parse((File) null));
                assertThrows(IllegalArgumentException.class, () -> builder.parse((InputSource) null));

                XmlUtil.recycleContentParser(builder);
                assertSame(builder, XmlUtil.createContentParser());

                assertThrows(SAXException.class, () -> builder.parse(new InputSource(new StringReader("<root>"))));
                XmlUtil.recycleContentParser(builder);
                assertTrue(pool.isEmpty());
            } finally {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void preservesProviderConfigurationAndDocumentCreation(final boolean enabled) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(enabled);
        factory.setValidating(enabled);
        factory.setXIncludeAware(enabled);
        final Schema schema = enabled ? SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema() : null;
        factory.setSchema(schema);
        final DocumentBuilder delegate = factory.newDocumentBuilder();
        final DocumentBuilder builder = wrap(delegate);

        assertEquals(enabled, builder.isNamespaceAware());
        assertEquals(enabled, builder.isValidating());
        assertEquals(enabled, builder.isXIncludeAware());
        assertSame(schema, builder.getSchema());
        assertSame(delegate.getDOMImplementation(), builder.getDOMImplementation());
        final Document document = builder.newDocument();
        assertNull(document.getDocumentElement());
        document.appendChild(document.createElement("root"));
        assertSame(document, document.getDocumentElement().getOwnerDocument());
        assertNotSame(document, builder.newDocument());
    }

    @Test
    public void parsesEveryInputFormAndPreservesSystemIds() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = wrap(factory.newDocumentBuilder());
        final String xml = "<root xmlns='urn:test'>value</root>";
        final byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        final Path file = Files.writeString(directory.resolve("document.xml"), xml);
        final String systemId = file.toUri().toString();

        assertParsedContent(builder.parse(new ByteArrayInputStream(bytes)));
        final Document streamDocument = builder.parse(new ByteArrayInputStream(bytes), systemId);
        assertParsedContent(streamDocument);
        assertEquals(systemId, streamDocument.getDocumentURI());
        assertParsedContent(builder.parse(systemId));
        assertParsedContent(builder.parse(file.toFile()));
        final InputSource source = new InputSource(new StringReader(xml));
        source.setSystemId(systemId);
        final Document sourceDocument = builder.parse(source);
        assertParsedContent(sourceDocument);
        assertEquals(systemId, sourceDocument.getDocumentURI());
    }

    @Test
    public void preservesParseFailuresAndInputValidation() throws Exception {
        final DocumentBuilder builder = wrap(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder());
        builder.setErrorHandler(new DefaultHandler());
        assertThrows(IllegalArgumentException.class, () -> builder.parse((InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> builder.parse(null, "urn:test"));
        assertThrows(IllegalArgumentException.class, () -> builder.parse((String) null));
        assertThrows(IllegalArgumentException.class, () -> builder.parse((File) null));
        assertThrows(IllegalArgumentException.class, () -> builder.parse((InputSource) null));
        assertThrows(IOException.class, () -> builder.parse(directory.resolve("missing.xml").toFile()));
        assertThrows(SAXParseException.class, () -> builder.parse(new InputSource(new StringReader("<root>"))));

        final IOException failure = new IOException("Cannot read input");
        final InputStream failingInput = new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IOException.class, () -> builder.parse(failingInput)));
    }

    @Test
    public void resetsInstalledResolversAndErrorHandlers() throws Exception {
        final DocumentBuilder builder = wrap(DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder());
        final Path dtd = Files.writeString(directory.resolve("document.dtd"), "<!ELEMENT root (#PCDATA)><!ENTITY value 'original'>");
        final String xml = "<!DOCTYPE root SYSTEM '" + dtd.toUri() + "'><root>&value;</root>";
        final AtomicInteger resolutions = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
        final SAXException failure = new SAXException("Custom error handler");
        builder.setEntityResolver((publicId, systemId) -> {
            resolutions.incrementAndGet();
            return new InputSource(new StringReader("<!ELEMENT root (#PCDATA)><!ENTITY value 'custom'>"));
        });
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void fatalError(final SAXParseException exception) throws SAXException {
                failures.incrementAndGet();
                throw failure;
            }
        });

        assertEquals("custom", builder.parse(new InputSource(new StringReader(xml))).getDocumentElement().getTextContent());
        assertSame(failure, assertThrows(SAXException.class, () -> builder.parse(new InputSource(new StringReader("<root>")))));

        // Reset must reach the provider so callbacks from the previous borrower are removed.
        builder.reset();
        assertEquals("original", builder.parse(new InputSource(new StringReader(xml))).getDocumentElement().getTextContent());
        assertThrows(SAXParseException.class, () -> builder.parse(new InputSource(new StringReader("<root>"))));
        assertEquals(1, resolutions.get());
        assertEquals(1, failures.get());
    }
}
