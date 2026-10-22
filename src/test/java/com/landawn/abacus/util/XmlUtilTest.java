package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.Transformer;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;

public class XmlUtilTest extends TestBase {

    @XmlRootElement(name = "person")
    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Person person = (Person) obj;
            return age == person.age && CommonUtil.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    private static Element parse(String xml) throws Exception {
        return XmlUtil.createDOMParser().parse(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))).getDocumentElement();
    }

    private static XMLStreamReader trackingReader(String xml, AtomicBoolean closed) {
        return new StreamReaderDelegate(XmlUtil.createXMLStreamReader(new StringReader(xml))) {
            @Override
            public void close() throws XMLStreamException {
                closed.set(true);
                super.close();
            }
        };
    }

    private static <T> T wrapParser(final Class<T> parserType, final T delegate) throws ReflectiveOperationException {
        final var constructor = Class.forName(XmlUtil.class.getName() + "$Cached" + parserType.getSimpleName()).getDeclaredConstructor(parserType);
        constructor.setAccessible(true);
        return parserType.cast(constructor.newInstance(delegate));
    }

    /** Isolates pool mutations and keeps test parsers alive independently of GC timing. */
    private static final class ParserPoolState<T> implements AutoCloseable {
        final Queue<WeakReference<T>> pool;
        final int capacity;
        private final List<WeakReference<T>> saved;
        private final List<T> pinned = new ArrayList<>();

        @SuppressWarnings("unchecked")
        ParserPoolState(final String fieldName) throws ReflectiveOperationException {
            final var poolField = XmlUtil.class.getDeclaredField(fieldName);
            poolField.setAccessible(true);
            pool = (Queue<WeakReference<T>>) poolField.get(null);
            final var capacityField = XmlUtil.class.getDeclaredField("POOL_SIZE");
            capacityField.setAccessible(true);
            capacity = capacityField.getInt(null);
            synchronized (pool) {
                saved = new ArrayList<>(pool);
                for (final WeakReference<T> reference : saved) {
                    final T parser = reference.get();
                    if (parser != null) {
                        pinned.add(parser);
                    }
                }
                pool.clear();
            }
        }

        T keep(final T parser) {
            pinned.add(parser);
            return parser;
        }

        @Override
        public void close() {
            synchronized (pool) {
                pool.clear();
                pool.addAll(saved);
            }
            Reference.reachabilityFence(pinned);
        }
    }

    private static <T> void assertParserPoolOwnershipAndReuse(final String fieldName, final Supplier<T> create, final Consumer<T> recycle, final T foreign)
            throws Exception {
        try (ParserPoolState<T> state = new ParserPoolState<>(fieldName)) {
            recycle.accept(null);
            recycle.accept(foreign);
            assertTrue(state.pool.isEmpty());

            final T first = state.keep(create.get());
            final T second = state.keep(create.get());
            assertNotSame(first, second);
            recycle.accept(first);
            recycle.accept(second);
            recycle.accept(first);
            assertEquals(2, state.pool.size(), "a duplicate return must not publish the parser twice");
            assertSame(first, create.get());
            assertSame(second, create.get());
            final T third = state.keep(create.get());
            assertNotSame(first, third);
            assertNotSame(second, third);

            recycle.accept(first);
            assertSame(first, create.get(), "a checked-out parser must be recyclable again");
        }
    }

    private static <T> void assertParserPoolCapacityAndResetFailure(final String fieldName, final Supplier<T> create, final Consumer<T> recycle,
            final T resetFails) throws Exception {
        try (ParserPoolState<T> state = new ParserPoolState<>(fieldName)) {
            final T victim = state.keep(create.get());
            final List<T> fillers = new ArrayList<>();
            for (int i = 0; i < state.capacity; i++) {
                fillers.add(state.keep(create.get()));
            }
            fillers.forEach(recycle);
            assertDoesNotThrow(() -> recycle.accept(victim));
            assertEquals(state.capacity, state.pool.size());
            assertFalse(state.pool.stream().anyMatch(reference -> reference.refersTo(victim)));

            state.pool.clear();
            assertDoesNotThrow(() -> recycle.accept(resetFails));
            assertTrue(state.pool.isEmpty(), "a provider that cannot reset must be discarded");
        }
    }

    private static <T> void assertParserPoolSkipsClearedReferences(final String fieldName, final Supplier<T> create) throws Exception {
        try (ParserPoolState<T> state = new ParserPoolState<>(fieldName)) {
            final T first = state.keep(create.get());
            final T second = state.keep(create.get());
            final WeakReference<T> cleared = new WeakReference<>(first);
            // Simulate a cleared entry directly instead of depending on System.gc().
            cleared.clear();
            state.pool.add(cleared);
            state.pool.add(new WeakReference<>(null));
            state.pool.add(new WeakReference<>(first));
            state.pool.add(new WeakReference<>(null));
            state.pool.add(new WeakReference<>(second));

            assertSame(first, create.get());
            assertSame(second, create.get());
            assertTrue(state.pool.isEmpty());

            state.pool.add(new WeakReference<>(null));
            state.pool.add(new WeakReference<>(null));
            final T replacement = state.keep(create.get());
            assertNotNull(replacement);
            assertNotSame(first, replacement);
            assertNotSame(second, replacement);
            assertTrue(state.pool.isEmpty(), "an all-cleared queue must be drained before creating a parser");
        }
    }

    private static <T> void assertParserPoolReclaimsClearedCapacity(final String fieldName, final Supplier<T> create, final Consumer<T> recycle)
            throws Exception {
        try (ParserPoolState<T> state = new ParserPoolState<>(fieldName)) {
            final T returned = state.keep(create.get());
            final List<T> live = new ArrayList<>();
            for (int i = 0; i < state.capacity - 1; i++) {
                live.add(state.keep(create.get()));
            }
            live.forEach(parser -> state.pool.add(new WeakReference<>(parser)));
            state.pool.add(new WeakReference<>(null));
            recycle.accept(returned);
            assertEquals(state.capacity, state.pool.size());
            assertTrue(state.pool.stream().anyMatch(reference -> reference.refersTo(returned)));
            assertTrue(state.pool.stream().allMatch(reference -> reference.get() != null));

            state.pool.clear();
            for (int i = 0; i < state.capacity; i++) {
                state.pool.add(new WeakReference<>(null));
            }
            recycle.accept(returned);
            assertEquals(1, state.pool.size());
            assertSame(returned, create.get());
        }
    }

    private static void assertBlockedOnMonitor(final Thread thread, final Object monitor, final String message) {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        final var threadBean = ManagementFactory.getThreadMXBean();
        boolean blockedOnMonitor = false;
        while (thread.isAlive() && System.nanoTime() < deadline) {
            final var info = threadBean.getThreadInfo(thread.threadId());
            if (info != null && info.getThreadState() == Thread.State.BLOCKED && info.getLockInfo() != null
                    && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(monitor)
                    && info.getLockInfo().getClassName().equals(monitor.getClass().getName()) && info.getLockOwnerId() == Thread.currentThread().threadId()) {
                blockedOnMonitor = true;
                break;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
        assertTrue(blockedOnMonitor, message);
    }

    @Test
    public void testContentParserSkipsClearedReferences() throws Exception {
        assertParserPoolSkipsClearedReferences("contentDocBuilderPool", XmlUtil::createContentParser);
    }

    @Test
    public void testSAXParserSkipsClearedReferences() throws Exception {
        assertParserPoolSkipsClearedReferences("saxParserPool", XmlUtil::createSAXParser);
    }

    @Test
    public void testContentParserReclaimsClearedCapacity() throws Exception {
        assertParserPoolReclaimsClearedCapacity("contentDocBuilderPool", XmlUtil::createContentParser, XmlUtil::recycleContentParser);
    }

    @Test
    public void testSAXParserReclaimsClearedCapacity() throws Exception {
        assertParserPoolReclaimsClearedCapacity("saxParserPool", XmlUtil::createSAXParser, XmlUtil::recycleSAXParser);
    }

    @Test
    public void testContentParserUsesSharedFactoryLock() throws Exception {
        try (ParserPoolState<DocumentBuilder> state = new ParserPoolState<>("contentDocBuilderPool")) {
            final var factoryField = XmlUtil.class.getDeclaredField("docBuilderFactory");
            factoryField.setAccessible(true);
            final DocumentBuilderFactory factory = (DocumentBuilderFactory) factoryField.get(null);
            final CountDownLatch started = new CountDownLatch(1);
            final AtomicReference<DocumentBuilder> created = new AtomicReference<>();
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final DocumentBuilder recyclable = state.keep(XmlUtil.createContentParser());
            final CountDownLatch recyclerStarted = new CountDownLatch(1);
            final CountDownLatch recyclerDone = new CountDownLatch(1);
            final AtomicReference<Throwable> recycleFailure = new AtomicReference<>();
            final Thread creator = new Thread(() -> {
                started.countDown();
                try {
                    created.set(XmlUtil.createContentParser());
                } catch (final Throwable e) {
                    failure.set(e);
                }
            }, "xml-content-factory-test");
            creator.setDaemon(true);
            final Thread recycler = new Thread(() -> {
                recyclerStarted.countDown();
                try {
                    XmlUtil.recycleContentParser(recyclable);
                } catch (final Throwable e) {
                    recycleFailure.set(e);
                } finally {
                    recyclerDone.countDown();
                }
            }, "xml-content-recycle-test");
            recycler.setDaemon(true);
            final boolean originalComments;
            final boolean originalWhitespace;

            try {
                synchronized (factory) {
                    originalComments = factory.isIgnoringComments();
                    originalWhitespace = factory.isIgnoringElementContentWhitespace();
                    creator.start();
                    assertTrue(started.await(5, TimeUnit.SECONDS));
                    assertBlockedOnMonitor(creator, factory, "content creation must wait for the same factory monitor as createDOMParser");

                    // Parser construction must happen without holding the pool monitor, otherwise recycling
                    // would be blocked while a creator waits for the factory.
                    recycler.start();
                    assertTrue(recyclerStarted.await(5, TimeUnit.SECONDS));
                    assertTrue(recyclerDone.await(5, TimeUnit.SECONDS), "recycling must not wait for the factory monitor");
                }
            } finally {
                creator.join(5000);
                if (creator.isAlive()) {
                    creator.interrupt();
                }
                recycler.join(5000);
                if (recycler.isAlive()) {
                    recycler.interrupt();
                }
            }

            assertFalse(creator.isAlive());
            assertFalse(recycler.isAlive());
            assertNull(failure.get());
            assertNull(recycleFailure.get());
            final DocumentBuilder builder = state.keep(created.get());
            assertNotNull(builder);
            final Document document = builder.parse(new org.xml.sax.InputSource(new StringReader("<root><!-- ignored --><child/></root>")));
            assertEquals(Node.ELEMENT_NODE, document.getDocumentElement().getFirstChild().getNodeType());
            synchronized (factory) {
                assertEquals(originalComments, factory.isIgnoringComments());
                assertEquals(originalWhitespace, factory.isIgnoringElementContentWhitespace());
            }
        }
    }

    @Test
    public void testSAXParserPoolReuseDoesNotAcquireFactoryLock() throws Exception {
        try (ParserPoolState<SAXParser> state = new ParserPoolState<>("saxParserPool")) {
            final var factoryField = XmlUtil.class.getDeclaredField("saxParserFactory");
            factoryField.setAccessible(true);
            final SAXParserFactory factory = (SAXParserFactory) factoryField.get(null);
            final SAXParser first = state.keep(XmlUtil.createSAXParser());
            final SAXParser second = state.keep(XmlUtil.createSAXParser());
            XmlUtil.recycleSAXParser(first);
            XmlUtil.recycleSAXParser(second);

            final FutureTask<SAXParser> firstCheckout = new FutureTask<>(XmlUtil::createSAXParser);
            final FutureTask<SAXParser> secondCheckout = new FutureTask<>(XmlUtil::createSAXParser);
            final Thread firstBorrower = new Thread(firstCheckout, "xml-sax-first-checkout-test");
            final Thread secondBorrower = new Thread(secondCheckout, "xml-sax-second-checkout-test");
            firstBorrower.setDaemon(true);
            secondBorrower.setDaemon(true);
            try {
                // Both checkouts must finish while another thread owns the factory monitor. Keeping the
                // parsers strongly reachable prevents GC from turning either checkout into a pool miss.
                synchronized (factory) {
                    firstBorrower.start();
                    secondBorrower.start();
                    final SAXParser firstResult = firstCheckout.get(5, TimeUnit.SECONDS);
                    final SAXParser secondResult = secondCheckout.get(5, TimeUnit.SECONDS);
                    assertTrue(firstResult == first || firstResult == second);
                    assertTrue(secondResult == first || secondResult == second);
                    assertNotSame(firstResult, secondResult, "concurrent borrowers must receive distinct pooled parsers");
                }
            } finally {
                firstBorrower.join(5000);
                secondBorrower.join(5000);
            }
            assertFalse(firstBorrower.isAlive());
            assertFalse(secondBorrower.isAlive());
        }
    }

    @Test
    public void testSAXParserUsesSharedFactoryLock() throws Exception {
        try (ParserPoolState<SAXParser> state = new ParserPoolState<>("saxParserPool")) {
            final var factoryField = XmlUtil.class.getDeclaredField("saxParserFactory");
            factoryField.setAccessible(true);
            final SAXParserFactory factory = (SAXParserFactory) factoryField.get(null);
            final SAXParser recyclable = state.keep(XmlUtil.createSAXParser());
            final FutureTask<SAXParser> creation = new FutureTask<>(XmlUtil::createSAXParser);
            final FutureTask<SAXParser> recycleAndReuse = new FutureTask<>(() -> {
                XmlUtil.recycleSAXParser(recyclable);
                return XmlUtil.createSAXParser();
            });
            final Thread creator = new Thread(creation, "xml-sax-factory-test");
            final Thread recycler = new Thread(recycleAndReuse, "xml-sax-recycle-test");
            creator.setDaemon(true);
            recycler.setDaemon(true);
            try {
                synchronized (factory) {
                    creator.start();
                    assertBlockedOnMonitor(creator, factory, "SAX creation on a pool miss must wait for the shared factory monitor");

                    // A creator waiting for the factory must release the pool monitor so a different
                    // thread can return and borrow a parser without waiting for provider construction.
                    recycler.start();
                    assertSame(recyclable, recycleAndReuse.get(5, TimeUnit.SECONDS));
                }
            } finally {
                creator.join(5000);
                recycler.join(5000);
            }
            assertFalse(creator.isAlive());
            assertFalse(recycler.isAlive());
            final SAXParser created = state.keep(creation.get(5, TimeUnit.SECONDS));
            assertNotNull(created);
            assertNotSame(recyclable, created);
        }
    }

    @Test
    public void testMarshalUnmarshal() {
        Person original = new Person("John", 30);
        String xml = XmlUtil.marshal(original);
        assertNotNull(xml);
        assertTrue(xml.contains("<person>") && xml.contains("<name>John</name>") && xml.contains("<age>30</age>") && xml.contains("</person>"));
        assertEquals(original, XmlUtil.unmarshal(Person.class, xml));

        Person alice = new Person("Alice", 25);
        assertEquals(alice, XmlUtil.unmarshal(Person.class, XmlUtil.marshal(alice)));

        Person nonAscii = new Person("Café 中文", 42);
        String nonAsciiXml = XmlUtil.marshal(nonAscii);
        assertTrue(nonAsciiXml.contains("Café 中文"));
        assertEquals(nonAscii, XmlUtil.unmarshal(Person.class, nonAsciiXml));

        Person fromLiteral = XmlUtil.unmarshal(Person.class,
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><age>30</age><name>John</name></person>");
        assertEquals("John", fromLiteral.getName());
        assertEquals(30, fromLiteral.getAge());
    }

    @Test
    public void testUnmarshal_EdgeCase() throws Exception {
        File externalEntityFile = File.createTempFile("xmlutil-unmarshal-xxe-", ".txt");
        try {
            Files.write(externalEntityFile.toPath(), "XXE_PUBLIC_UNMARSHAL_MARKER_2026".getBytes(Charsets.UTF_8));
            String maliciousXml = "<?xml version=\"1.0\"?><!DOCTYPE person [<!ENTITY xxe SYSTEM \"" + externalEntityFile.toURI()
                    + "\">]><person><name>&xxe;</name><age>25</age></person>";
            assertThrows(RuntimeException.class, () -> XmlUtil.unmarshal(Person.class, maliciousXml));
        } finally {
            externalEntityFile.delete();
        }

        AtomicBoolean closedOnFailure = new AtomicBoolean();
        assertThrows(RuntimeException.class, () -> XmlUtil.unmarshalAndClose(Person.class, trackingReader("<person><name>Alice</person>", closedOnFailure)));
        assertTrue(closedOnFailure.get());

        AtomicBoolean closedOnSuccess = new AtomicBoolean();
        assertEquals(new Person("Alice", 25),
                XmlUtil.unmarshalAndClose(Person.class, trackingReader("<person><name>Alice</name><age>25</age></person>", closedOnSuccess)));
        assertTrue(closedOnSuccess.get());
    }

    @Test
    public void testCreateMarshallerAndUnmarshaller() {
        Marshaller m1 = XmlUtil.createMarshaller(Person.class);
        Marshaller m2 = XmlUtil.createMarshaller(Person.class);
        assertNotNull(m1);
        assertNotNull(m2);
        assertThrows(UncheckedException.class, () -> XmlUtil.createMarshaller("com.landawn.abacus.util"));

        Unmarshaller u1 = XmlUtil.createUnmarshaller(Person.class);
        Unmarshaller u2 = XmlUtil.createUnmarshaller(Person.class);
        assertNotNull(u1);
        assertNotNull(u2);
        assertThrows(UncheckedException.class, () -> XmlUtil.createUnmarshaller("com.landawn.abacus.util"));
    }

    @Test
    public void testCreateDOMParser() {
        assertNotNull(XmlUtil.createDOMParser());
        assertNotNull(XmlUtil.createDOMParser(true, true));
        assertNotNull(XmlUtil.createDOMParser(false, false));
    }

    @Test
    public void testContentParserPool() throws Exception {
        assertParserPoolOwnershipAndReuse("contentDocBuilderPool", XmlUtil::createContentParser, XmlUtil::recycleContentParser,
                DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder());
    }

    @Test
    public void testSAXParserPool() throws Exception {
        assertParserPoolOwnershipAndReuse("saxParserPool", XmlUtil::createSAXParser, XmlUtil::recycleSAXParser,
                SAXParserFactory.newDefaultInstance().newSAXParser());
    }

    @Test
    public void testDomAndSaxParsersRejectDoctype() throws Exception {
        byte[] hostileXml = "<!DOCTYPE root [<!ENTITY xxe SYSTEM \"file:///definitely-not-readable-abacus-xxe\">]><root>&xxe;</root>".getBytes(Charsets.UTF_8);
        DocumentBuilder documentBuilder = XmlUtil.createContentParser();
        try {
            assertThrows(Exception.class, () -> documentBuilder.parse(new ByteArrayInputStream(hostileXml)));
        } finally {
            XmlUtil.recycleContentParser(documentBuilder);
        }
        SAXParser saxParser = XmlUtil.createSAXParser();
        try {
            assertThrows(Exception.class, () -> saxParser.parse(new ByteArrayInputStream(hostileXml), new DefaultHandler()));
        } finally {
            XmlUtil.recycleSAXParser(saxParser);
        }
    }

    @Test
    public void testCreateXMLStreamReader() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><item>test</item></root>";
        assertNotNull(XmlUtil.createXMLStreamReader(new StringReader(xml)));
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))) {
            assertNotNull(XmlUtil.createXMLStreamReader(is));
        }
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))) {
            assertNotNull(XmlUtil.createXMLStreamReader(is, "UTF-8"));
        }

        File externalEntityFile = File.createTempFile("xmlutil-xxe-", ".txt");
        try {
            Files.write(externalEntityFile.toPath(), "XXE_MARKER_2026".getBytes(Charsets.UTF_8));
            String maliciousXml = "<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY xxe SYSTEM \"" + externalEntityFile.toURI() + "\">]><root>&xxe;</root>";
            assertThrows(Exception.class, () -> {
                XMLStreamReader xmlReader = XmlUtil.createXMLStreamReader(new StringReader(maliciousXml));
                try {
                    while (xmlReader.hasNext()) {
                        xmlReader.next();
                    }
                } finally {
                    xmlReader.close();
                }
            });
        } finally {
            externalEntityFile.delete();
        }
    }

    @Test
    public void testCreateFilteredStreamReader() throws Exception {
        XMLStreamReader source = XmlUtil.createXMLStreamReader(new StringReader("<?xml version=\"1.0\"?><root><item>test</item></root>"));
        assertNotNull(XmlUtil.createFilteredStreamReader(source, r -> r.isStartElement() || r.isEndElement()));

        XMLStreamReader nullFilterSource = XmlUtil.createXMLStreamReader(new StringReader("<root/>"));
        try {
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.createFilteredStreamReader(nullFilterSource, null));
        } finally {
            nullFilterSource.close();
        }
    }

    @Test
    public void testCreateXMLStreamWriter() {
        assertNotNull(XmlUtil.createXMLStreamWriter(new StringWriter()));
        assertNotNull(XmlUtil.createXMLStreamWriter(new ByteArrayOutputStream()));
        assertNotNull(XmlUtil.createXMLStreamWriter(new ByteArrayOutputStream(), "UTF-8"));
    }

    @Test
    public void testConcurrentStaxReaderAndWriterCreation() {
        assertDoesNotThrow(() -> java.util.stream.IntStream.range(0, 200).parallel().forEach(i -> {
            XMLStreamReader reader = null;
            XMLStreamWriter writer = null;
            try {
                reader = XmlUtil.createXMLStreamReader(new StringReader("<root><value>" + i + "</value></root>"));
                writer = XmlUtil.createXMLStreamWriter(new StringWriter());
                assertEquals(XMLStreamConstants.START_DOCUMENT, reader.getEventType());
                writer.writeStartDocument();
                writer.writeEmptyElement("root");
                writer.writeEndDocument();
            } catch (Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        }));
    }

    @Test
    public void testCreateXMLTransformer() {
        Transformer transformer = XmlUtil.createXMLTransformer();
        assertNotNull(transformer);
    }

    @Test
    public void testTransform() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);

        File tempFile = File.createTempFile("xmlutil-test", ".xml");
        tempFile.deleteOnExit();
        try {
            XmlUtil.transform(doc, tempFile);
            assertTrue(tempFile.exists() && tempFile.length() > 0);
            assertTrue(new String(Files.readAllBytes(tempFile.toPath())).contains("<root"));
        } finally {
            tempFile.delete();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.transform(doc, baos);
        assertTrue(baos.toString().contains("<root"));

        StringWriter writer = new StringWriter();
        XmlUtil.transform(doc, writer);
        assertTrue(writer.toString().contains("<root"));
    }

    @Test
    public void testTransform_EdgeCase() throws Exception {
        Document doc = XmlUtil.createDOMParser().newDocument();
        Element root = doc.createElement("root");
        root.setTextContent("Café 中文");
        doc.appendChild(root);

        File tempFile = File.createTempFile("xmlutil-utf8", ".xml");
        tempFile.deleteOnExit();
        try {
            XmlUtil.transform(doc, tempFile);
            String content = new String(Files.readAllBytes(tempFile.toPath()), Charsets.UTF_8);
            assertTrue(content.contains("Café 中文"));
            assertEquals("Café 中文", XmlUtil.createDOMParser().parse(tempFile).getDocumentElement().getTextContent());
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testTransformRejectsNullBeforeWriting() throws Exception {
        File existingFile = File.createTempFile("xmlutil-null-source-", ".xml");
        java.nio.file.Path directory = Files.createTempDirectory("xmlutil-null-source-");
        File missingFile = directory.resolve("missing.xml").toFile();
        try {
            Files.writeString(existingFile.toPath(), "original content", Charsets.UTF_8);
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, existingFile));
            assertEquals("original content", Files.readString(existingFile.toPath(), Charsets.UTF_8));

            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, missingFile));
            assertFalse(missingFile.exists());

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write("original content".getBytes(Charsets.UTF_8));
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, output));
            assertEquals("original content", output.toString(Charsets.UTF_8));

            StringWriter writer = new StringWriter();
            writer.write("original content");
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(null, writer));
            assertEquals("original content", writer.toString());

            Document document = parse("<root/>").getOwnerDocument();
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(document, (File) null));
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(document, (java.io.OutputStream) null));
            assertThrows(IllegalArgumentException.class, () -> XmlUtil.transform(document, (java.io.Writer) null));
        } finally {
            Files.deleteIfExists(existingFile.toPath());
            Files.deleteIfExists(missingFile.toPath());
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void testXmlEncodeDecode_disabledByDefault() {
        assertThrows(UnsupportedOperationException.class, () -> XmlUtil.xmlEncode(new Person("Bob", 35)));
        assertThrows(UnsupportedOperationException.class, () -> XmlUtil.xmlDecode("<?xml ?><void/>"));
    }

    @Test
    public void testGetElementsAndNodesByName() throws Exception {
        Element root = parse("<?xml version=\"1.0\"?><root><child>1</child><child>2</child><other><child>3</child></other></root>");
        List<Element> children = XmlUtil.getElementsByTagName(root, "child");
        assertEquals(2, children.size());
        assertEquals("1", children.get(0).getTextContent());
        assertEquals("2", children.get(1).getTextContent());
        List<Element> allDirect = XmlUtil.getElementsByTagName(root, "*");
        assertEquals(3, allDirect.size());
        assertEquals(List.of("child", "child", "other"), allDirect.stream().map(Element::getTagName).toList());

        Document doc = root.getOwnerDocument();
        List<Node> items = XmlUtil
                .getNodesByName(parse("<?xml version=\"1.0\"?><root><item>1</item><container><item>2</item></container></root>").getOwnerDocument(), "item");
        assertEquals(2, items.size());

        Node found = XmlUtil.getNextNodeByName(parse("<?xml version=\"1.0\"?><root><item>1</item><other>2</other><item>3</item></root>").getOwnerDocument(),
                "item");
        assertEquals("item", found.getNodeName());
        assertNull(XmlUtil.getNextNodeByName(doc, "nonexistent"));

        Element itemRoot = parse("<?xml version=\"1.0\"?><item>value</item>");
        assertEquals("item", XmlUtil.getNextNodeByName(itemRoot, "item").getNodeName());
    }

    @Test
    public void testGetAttributeAndReadAttributes() throws Exception {
        Element root = parse("<?xml version=\"1.0\"?><root id=\"123\" name=\"test\" value=\"abc\"/>");
        assertEquals("123", XmlUtil.getAttribute(root, "id"));
        assertEquals("test", XmlUtil.getAttribute(root, "name"));
        assertNull(XmlUtil.getAttribute(root, "missing"));

        Map<String, String> attrs = XmlUtil.readAttributes(root);
        assertEquals("123", attrs.get("id"));
        assertEquals("test", attrs.get("name"));
        assertEquals("abc", attrs.get("value"));

        Element withClass = parse("<?xml version=\"1.0\"?><root id=\"42\" class=\"test\"/>");
        Map<String, String> classAttrs = XmlUtil.readAttributes(withClass);
        assertEquals("42", classAttrs.get("id"));
        assertEquals("test", classAttrs.get("class"));

        assertTrue(XmlUtil.readAttributes(parse("<?xml version=\"1.0\"?><root/>")).isEmpty());
    }

    @Test
    public void testReadElement() throws Exception {
        Map<String, String> textRoot = XmlUtil.readElement(parse("<?xml version=\"1.0\"?><name>John</name>"));
        assertTrue(textRoot.containsKey("name") || textRoot.containsKey("name.name"));

        Map<String, String> person = XmlUtil.readElement(parse("<?xml version=\"1.0\"?><person age=\"30\"><name>John</name><city>NYC</city></person>"));
        assertEquals("30", person.get("age"));
        assertTrue(person.containsKey("person.name"));
        assertTrue(person.containsKey("person.city"));

        Map<String, String> nested = XmlUtil.readElement(parse("<?xml version=\"1.0\"?><person id=\"7\"><home zip=\"10001\"/><work zip=\"94105\"/></person>"));
        assertEquals("7", nested.get("id"));
        assertEquals("10001", nested.get("person.home.zip"));
        assertEquals("94105", nested.get("person.work.zip"));
        assertFalse(nested.containsKey("person.zip"));
    }

    @Test
    public void testIsTextElement() throws Exception {
        Element root = parse("<?xml version=\"1.0\"?><root><text>value</text><parent><child>nested</child></parent><empty/></root>");
        NodeList children = root.getChildNodes();
        Element textElem = null;
        Element parentElem = null;
        Element empty = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("text".equals(node.getNodeName())) {
                    textElem = (Element) node;
                } else if ("parent".equals(node.getNodeName())) {
                    parentElem = (Element) node;
                } else if ("empty".equals(node.getNodeName())) {
                    empty = (Element) node;
                }
            }
        }
        assertTrue(XmlUtil.isTextElement(textElem));
        assertFalse(XmlUtil.isTextElement(parentElem));
        assertTrue(XmlUtil.isTextElement(empty));
    }

    @Test
    public void testGetTextContent() throws Exception {
        assertEquals("  Hello World  ", XmlUtil.getTextContent(parse("<?xml version=\"1.0\"?><root>  Hello World  </root>")));
        assertEquals("", XmlUtil.getTextContent(parse("<?xml version=\"1.0\"?><root></root>")));

        Element ws = parse("<?xml version=\"1.0\"?><root>  Hello\n\tWorld  </root>");
        assertEquals("Hello World", XmlUtil.getTextContent(ws, true));
        assertTrue(XmlUtil.getTextContent(ws, false).contains("\n") || XmlUtil.getTextContent(ws, false).contains("\t"));
        assertEquals("", XmlUtil.getTextContent(parse("<?xml version=\"1.0\"?><root></root>"), true));

        DocumentBuilder db = XmlUtil.createContentParser();
        try {
            Node contentRoot = db.parse(new ByteArrayInputStream("<root>  Hello\n\tWorld  </root>".getBytes("UTF-8"))).getDocumentElement();
            String normalized = XmlUtil.getTextContent(contentRoot, true);
            assertFalse(normalized.startsWith(" "));
            assertFalse(normalized.endsWith(" "));
            assertEquals("  Hello World  ",
                    XmlUtil.getTextContent(db.parse(new ByteArrayInputStream("<root>  Hello World  </root>".getBytes("UTF-8"))).getDocumentElement(), false));
        } finally {
            XmlUtil.recycleContentParser(db);
        }

        String trailing = XmlUtil.getTextContent(parse("<root>hello\t</root>"), true);
        assertTrue(trailing.endsWith("hello") || trailing.equals("hello"));
    }

    @Test
    public void testGetTextContent_preservesWhitespaceRulesAndUnicode() throws Exception {
        final Document document = XmlUtil.createDOMParser().newDocument();
        // Build DOM text directly: an XML parser would normalize line endings and reject some control/surrogate cases.
        final String[][] cases = { { "", "" }, { " \t\b\n\r\f ", "" }, { "  a  b  ", "a  b" }, { "a\t\b\n\r\fb", "a b" }, { "a\n b", "a  b" },
                { "a \n b", "a  b" }, { "a  \n\t  b", "a    b" }, { " \uD83D\uDE03\n\u4E2D\u6587 ", "\uD83D\uDE03 \u4E2D\u6587" },
                { "\t\uD800\uDC00a\uDBFF\uDFFF\r", "\uD800\uDC00a\uDBFF\uDFFF" }, { "\t\uD800\nx\r\uDFFF\f", "\uD800 x \uDFFF" },
                { "\uD83D\n\uDE03", "\uD83D \uDE03" }, { " e\u0301\n\u03A9\uFFFD ", "e\u0301 \u03A9\uFFFD" },
                { "\u00A0a\u000Bb\u00A0", "\u00A0a\u000Bb\u00A0" }, { " \u0085\u00A0\u1680\u2000\u2007\u2028\u2029\u202F\u205F\u3000\uFEFF ",
                        "\u0085\u00A0\u1680\u2000\u2007\u2028\u2029\u202F\u205F\u3000\uFEFF" } };

        for (int i = 0; i < cases.length; i++) {
            final Node text = document.createTextNode(cases[i][0]);
            assertEquals(cases[i][1], XmlUtil.getTextContent(text, true), "Normalized case " + i);
            assertEquals(cases[i][0], XmlUtil.getTextContent(text, false), "Raw case " + i);
            assertEquals(cases[i][0], XmlUtil.getTextContent(text), "Default case " + i);
        }
    }

    @Test
    public void testGetTextContent_preservesUnicodeAcrossDomNodeBoundaries() throws Exception {
        final Document document = XmlUtil.createDOMParser().newDocument();
        final Element root = document.createElement("root");
        document.appendChild(root);
        root.appendChild(document.createTextNode(" \uD83D"));
        root.appendChild(document.createCDATASection("\uDE03\t\u4E2D"));
        final Element child = document.createElement("child");
        child.appendChild(document.createTextNode("\u6587"));
        root.appendChild(child);
        root.appendChild(document.createComment("not text content"));
        root.appendChild(document.createTextNode("\r\uDBFF\uDFFF "));

        final String raw = " \uD83D\uDE03\t\u4E2D\u6587\r\uDBFF\uDFFF ";
        assertEquals("\uD83D\uDE03 \u4E2D\u6587 \uDBFF\uDFFF", XmlUtil.getTextContent(root, true));
        assertEquals(raw, XmlUtil.getTextContent(root, false));
        assertEquals(raw, XmlUtil.getTextContent(root));
    }

    @Test
    public void testWriteCharacters() throws Exception {
        StringBuilder sb = new StringBuilder();
        XmlUtil.writeCharacters("<hello> & \"world\"".toCharArray(), sb);
        assertTrue(sb.toString().contains("&lt;") && sb.toString().contains("&gt;") && sb.toString().contains("&amp;") && sb.toString().contains("&quot;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters("prefix <tag> suffix".toCharArray(), 7, 5, sb);
        assertTrue(sb.toString().contains("&lt;tag&gt;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters("<data> & 'value'", sb);
        assertTrue(sb.toString().contains("&lt;") && sb.toString().contains("&amp;") && sb.toString().contains("&apos;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters("prefix <tag> suffix", 7, 5, sb);
        assertTrue(sb.toString().contains("&lt;tag&gt;"));

        sb = new StringBuilder();
        XmlUtil.writeCharacters((String) null, sb);
        assertEquals("null", sb.toString());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.writeCharacters("<test>".toCharArray(), baos);
        assertTrue(baos.toString().contains("&lt;test&gt;"));
        baos.reset();
        XmlUtil.writeCharacters("prefix <tag> suffix".toCharArray(), 7, 5, baos);
        assertTrue(baos.toString().contains("&lt;tag&gt;"));
        baos.reset();
        XmlUtil.writeCharacters("<data> & value", baos);
        assertTrue(baos.toString().contains("&lt;") && baos.toString().contains("&amp;"));
        baos.reset();
        XmlUtil.writeCharacters("prefix <tag> suffix", 7, 5, baos);
        assertTrue(baos.toString().contains("&lt;tag&gt;"));

        StringWriter writer = new StringWriter();
        XmlUtil.writeCharacters("<element>".toCharArray(), writer);
        assertTrue(writer.toString().contains("&lt;element&gt;"));
        writer = new StringWriter();
        XmlUtil.writeCharacters("prefix <tag> suffix".toCharArray(), 7, 5, writer);
        assertTrue(writer.toString().contains("&lt;tag&gt;"));
        writer = new StringWriter();
        XmlUtil.writeCharacters("<root> & \"data\"", writer);
        assertTrue(writer.toString().contains("&lt;") && writer.toString().contains("&amp;") && writer.toString().contains("&quot;"));
        writer = new StringWriter();
        XmlUtil.writeCharacters("prefix <tag> suffix", 7, 5, writer);
        assertTrue(writer.toString().contains("&lt;tag&gt;"));
    }

    @Test
    public void testWriteCharactersPreservesWriterFailuresWithoutRetrying() {
        for (boolean arrayInput : new boolean[] { false, true }) {
            for (boolean failOnFlush : new boolean[] { false, true }) {
                IOException failure = new IOException("original writer failure");
                int[] calls = new int[3];
                Writer output = new Writer() {
                    @Override
                    public void write(char[] buffer, int offset, int length) throws IOException {
                        calls[0]++;
                        if (!failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        calls[1]++;
                        if (failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void close() {
                        calls[2]++;
                    }
                };
                IOException actual = assertThrows(IOException.class, () -> {
                    if (arrayInput) {
                        XmlUtil.writeCharacters("x<&>y".toCharArray(), 1, 3, output);
                    } else {
                        XmlUtil.writeCharacters("x<&>y", 1, 3, output);
                    }
                });
                assertSame(failure, actual);
                assertEquals(1, calls[0]);
                assertEquals(failOnFlush ? 1 : 0, calls[1]);
                assertEquals(0, calls[2]);
            }
        }
    }

    @Test
    public void testWriteCharactersPreservesOutputStreamFailuresWithoutRetrying() {
        for (boolean arrayInput : new boolean[] { false, true }) {
            for (boolean failOnFlush : new boolean[] { false, true }) {
                IOException failure = new IOException("original stream failure");
                int[] calls = new int[3];
                OutputStream output = new OutputStream() {
                    @Override
                    public void write(int value) throws IOException {
                        write(new byte[] { (byte) value }, 0, 1);
                    }

                    @Override
                    public void write(byte[] buffer, int offset, int length) throws IOException {
                        calls[0]++;
                        if (!failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        calls[1]++;
                        if (failOnFlush) {
                            throw failure;
                        }
                    }

                    @Override
                    public void close() {
                        calls[2]++;
                    }
                };
                IOException actual = assertThrows(IOException.class, () -> {
                    if (arrayInput) {
                        XmlUtil.writeCharacters("x<&>y".toCharArray(), 1, 3, output);
                    } else {
                        XmlUtil.writeCharacters("x<&>y", 1, 3, output);
                    }
                });
                assertSame(failure, actual);
                assertEquals(1, calls[0]);
                assertEquals(failOnFlush ? 1 : 0, calls[1]);
                assertEquals(0, calls[2]);
            }
        }
    }

    @Test
    public void testGetAttributeTypeClass() throws Exception {
        assertEquals(int.class, XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root type=\"int\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root/>")));
        assertEquals(String.class, XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root type=\"String\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<?xml version=\"1.0\"?><root type=\"com.unknown.NonExistentClass12345XYZ\"/>")));
        assertEquals(java.util.LinkedHashMap.class, XmlUtil.getAttributeTypeClass(parse("<root type=\"java.util.LinkedHashMap\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<root type=\"java.lang.Runtime\"/>")));
        assertNull(XmlUtil.getAttributeTypeClass(parse("<root type=\"java.util.List&lt;java.lang.Runtime&gt;\"/>")));
        assertEquals(String[][].class, XmlUtil.getAttributeTypeClass(parse("<root type=\"java.lang.String[][]\"/>")));
    }

    @Test
    public void testGetConcreteClass() throws Exception {
        assertEquals(String.class, XmlUtil.getConcreteClass(Object.class, String.class));
        assertEquals(Integer.class, XmlUtil.getConcreteClass(Integer.class, String.class));
        assertEquals(String.class, XmlUtil.getConcreteClass(String.class, (Class<?>) null));
        assertEquals(String.class, XmlUtil.getConcreteClass(null, String.class));
        assertEquals(String.class, XmlUtil.getConcreteClass(String.class, (Node) null));
        assertNotNull(XmlUtil.getConcreteClass(Object.class, parse("<?xml version=\"1.0\"?><root type=\"int\"/>")));
    }

    @Test
    public void testGetNodeType() {
        assertEquals(XmlUtil.NodeType.PROPERTY, XmlUtil.getNodeType("anything", XmlUtil.NodeType.ENTITY));
        assertEquals(XmlUtil.NodeType.ARRAY, XmlUtil.getNodeType("array", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.ENTITY, XmlUtil.getNodeType("unknownTag", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("list", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.MAP, XmlUtil.getNodeType("map", XmlUtil.NodeType.PROPERTY));
    }

    /**
     * An exception whose cause chain loops back on itself, without touching any JDK internals: {@code getCause()}
     * is overridden to return a target set after construction.
     */
    private static final class LoopingCauseException extends Exception {
        private static final long serialVersionUID = 1L;

        private transient Throwable target;

        LoopingCauseException(final String message) {
            super(message);
        }

        void pointAt(final Throwable t) {
            target = t;
        }

        @Override
        public synchronized Throwable getCause() {
            return target;
        }
    }

    /**
     * A cyclic cause chain must not make the {@code IOException} search loop forever.
     *
     * <p>Guarded by a preemptive timeout on purpose: the defect was an unbounded walk, and it ran inside
     * {@code synchronized (xmlInputFactory)}, so without the timeout a regression would wedge the whole suite.</p>
     */
    @Test
    public void testToRuntimeException_CyclicCauseChainTerminates() throws Exception {
        final LoopingCauseException loop = new LoopingCauseException("loop");
        final XMLStreamException top = new XMLStreamException("top", loop);
        loop.pointAt(top);

        // Reflection, not a widened modifier: the same test then exercises the pre-fix build too, so its
        // RED-on-base result is the unbounded walk itself rather than an access error.
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> assertNotNull(invokeToRuntimeException(top)));
    }

    /** An {@code IOException} anywhere in an acyclic chain is still unwrapped. */
    @Test
    public void testToRuntimeException_UnwrapsIOExceptionFromNestedCause() throws Exception {
        final java.io.IOException io = new java.io.IOException("disk");
        final XMLStreamException top = new XMLStreamException("top", new IllegalStateException("mid", io));

        assertTrue(invokeToRuntimeException(top) instanceof com.landawn.abacus.exception.UncheckedIOException);
    }

    private static RuntimeException invokeToRuntimeException(final XMLStreamException e) throws Exception {
        final java.lang.reflect.Method m = XmlUtil.class.getDeclaredMethod("toRuntimeException", XMLStreamException.class);
        m.setAccessible(true);

        try {
            return (RuntimeException) m.invoke(null, e);
        } catch (final java.lang.reflect.InvocationTargetException ite) {
            final Throwable cause = ite.getCause();

            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }

            throw new AssertionError("unexpected checked exception from toRuntimeException", cause);
        }
    }

    /**
     * The container element names the abacus XML writers really emit must map to {@code COLLECTION}, matching the
     * authoritative table in {@code AbacusXmlParserImpl}.
     */
    @Test
    public void testGetNodeType_SetAndCollectionAreContainerNames() {
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("set", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("collection", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.COLLECTION, XmlUtil.getNodeType("list", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.ARRAY, XmlUtil.getNodeType("array", XmlUtil.NodeType.PROPERTY));
        assertEquals(XmlUtil.NodeType.ENTITY, XmlUtil.getNodeType("unknownTag", XmlUtil.NodeType.PROPERTY));
    }

    /** Contract pin for the documented {@code NullPointerException} of the four JAXB factory methods. */
    @Test
    public void testCreateMarshallerAndUnmarshaller_NullArgument() {
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createMarshaller((String) null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createMarshaller((Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createUnmarshaller((String) null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.createUnmarshaller((Class<?>) null));
    }

    /**
     * Contract pin: a non-null node can still have no DOM text content. {@code Document} and
     * {@code DocumentType} nodes report {@code null}, which both {@code getTextContent} overloads pass through.
     */
    @Test
    public void testGetTextContent_NodeTypesWithoutTextContent() throws Exception {
        DocumentBuilder builder = XmlUtil.createDOMParser();
        Document doc = builder.parse(new ByteArrayInputStream("<root>hello</root>".getBytes(Charsets.UTF_8)));

        assertEquals(Node.DOCUMENT_NODE, doc.getNodeType());
        assertNull(XmlUtil.getTextContent(doc));
        assertNull(XmlUtil.getTextContent(doc, true));
        assertNull(XmlUtil.getTextContent(doc, false));

        // The factory disallows DOCTYPE, so build the document-type node through the DOM implementation.
        org.w3c.dom.DocumentType docType = builder.getDOMImplementation().createDocumentType("qname", "publicId", "systemId");

        assertEquals(Node.DOCUMENT_TYPE_NODE, docType.getNodeType());
        assertNull(XmlUtil.getTextContent(docType));
        assertNull(XmlUtil.getTextContent(docType, true));
        assertNull(XmlUtil.getTextContent(docType, false));

        assertEquals("hello", XmlUtil.getTextContent(doc.getDocumentElement()));
        assertEquals("hello", XmlUtil.getTextContent(doc.getDocumentElement(), true));
    }

    /**
     * The writer-side predicate must answer exactly what the reader does, including for values with surrounding
     * whitespace: without the reader's {@code trim()} a trailing space defeated the {@code "[]"} suffix stripping.
     */
    @Test
    public void testIsResolvableXmlTypeAttributeName_AgreesWithReaderOnPaddedNames() throws Exception {
        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName(" java.util.List[] "));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\" java.util.List[] \"/>")));

        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName(" int[] "));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\" int[] \"/>")));

        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName(" java.util.List "));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\" java.util.List \"/>")));

        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(" "));
        assertNull(XmlUtil.getAttributeType(parse("<root type=\" \"/>")));

        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName("\t"));
        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(""));
        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(null));

        assertFalse(XmlUtil.isResolvableXmlTypeAttributeName(" java.lang.Runtime "));
        assertNull(XmlUtil.getAttributeType(parse("<root type=\" java.lang.Runtime \"/>")));

        assertTrue(XmlUtil.isResolvableXmlTypeAttributeName("java.util.List[]"));
        assertNotNull(XmlUtil.getAttributeType(parse("<root type=\"java.util.List[]\"/>")));
    }

    /**
     * Contract pin for the documented {@code IndexOutOfBoundsException} of the six bounded
     * {@code writeCharacters} overloads. The exception is not an {@code IOException}, so the StringBuilder
     * overloads do not convert it either.
     */
    @Test
    public void testWriteCharacters_OutOfRangeOffsetOrLength() {
        char[] cbuf = "abc".toCharArray();

        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, 2, 5, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, -1, 1, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 2, 5, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 0, -1, new StringBuilder()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters((String) null, 0, 9, new StringBuilder()));

        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, 2, 5, new ByteArrayOutputStream()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 2, 5, new ByteArrayOutputStream()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(cbuf, 2, 5, new StringWriter()));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 2, 5, new StringWriter()));

        // The documented null-String behaviour: off/len select a slice of the literal "null".
        StringBuilder sb = new StringBuilder();
        XmlUtil.writeCharacters((String) null, 1, 3, sb);
        assertEquals("ull", sb.toString());
    }

    /** A {@code DocumentBuilder} whose {@code reset()} is unsupported, which JAXP explicitly permits. */
    private static final class ResetUnsupportedDocumentBuilder extends DocumentBuilder {
        @Override
        public Document parse(final org.xml.sax.InputSource is) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNamespaceAware() {
            return true;
        }

        @Override
        public boolean isValidating() {
            return false;
        }

        @Override
        public void setEntityResolver(final org.xml.sax.EntityResolver er) {
            // no-op
        }

        @Override
        public void setErrorHandler(final org.xml.sax.ErrorHandler eh) {
            // no-op
        }

        @Override
        public Document newDocument() {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.w3c.dom.DOMImplementation getDOMImplementation() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Contract pin for the two documented silent drops: an owned, not-yet-pooled parser is discarded (without
     * an exception) when the pool is at capacity, and likewise when the provider's {@code reset()} throws.
     */
    @Test
    public void testRecycleContentParserDropsWhenPoolIsFullOrResetFails() throws Exception {
        final DocumentBuilder resetFails = wrapParser(DocumentBuilder.class, new ResetUnsupportedDocumentBuilder());
        assertThrows(UnsupportedOperationException.class, resetFails::reset);
        assertParserPoolCapacityAndResetFailure("contentDocBuilderPool", XmlUtil::createContentParser, XmlUtil::recycleContentParser, resetFails);
    }

    /** A {@code SAXParser} whose {@code reset()} is unsupported, which JAXP explicitly permits. */
    private static final class ResetUnsupportedSaxParser extends SAXParser {
        @Override
        @SuppressWarnings("deprecation")
        public org.xml.sax.Parser getParser() {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.xml.sax.XMLReader getXMLReader() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNamespaceAware() {
            return true;
        }

        @Override
        public boolean isValidating() {
            return false;
        }

        @Override
        public void setProperty(final String name, final Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getProperty(final String name) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * The SAX sibling of {@link #testRecycleContentParserDropsWhenPoolIsFullOrResetFails()}: the same two
     * silent drops are documented on {@code recycleSAXParser}, so they are pinned the same way.
     */
    @Test
    public void testRecycleSaxParserDropsWhenPoolIsFullOrResetFails() throws Exception {
        final SAXParser resetFails = wrapParser(SAXParser.class, new ResetUnsupportedSaxParser());
        assertThrows(UnsupportedOperationException.class, resetFails::reset);
        assertParserPoolCapacityAndResetFailure("saxParserPool", XmlUtil::createSAXParser, XmlUtil::recycleSAXParser, resetFails);
    }

    /**
     * The writer factories must unwrap an {@code IOException} cause the same way their
     * {@code createXMLStreamReader} siblings do. An unsupported encoding name is the reachable case: the
     * provider reports it as an {@code XMLStreamException} wrapping an {@code UnsupportedEncodingException}.
     */
    @Test
    public void testCreateXMLStreamWriterUnwrapsIOExceptionCause() {
        com.landawn.abacus.exception.UncheckedIOException thrown = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> XmlUtil.createXMLStreamWriter(new ByteArrayOutputStream(), "no-such-charset-xyz"));

        assertTrue(thrown.getCause() instanceof java.io.UnsupportedEncodingException);
    }

    @Test
    public void testWriteCharactersNullOutputNamesOutputParameter() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> XmlUtil.writeCharacters("a<b", (StringBuilder) null));
        assertTrue(e1.getMessage().contains("'output'"), e1.getMessage());
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> XmlUtil.writeCharacters(new char[] { 'a' }, 0, 1, (StringBuilder) null));
        assertTrue(e2.getMessage().contains("'output'"), e2.getMessage());
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> XmlUtil.writeCharacters("a<b", (OutputStream) null));
        assertTrue(e3.getMessage().contains("'output'"), e3.getMessage());
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class,
                () -> XmlUtil.writeCharacters(new char[] { 'a' }, 0, 1, (OutputStream) null));
        assertTrue(e4.getMessage().contains("'output'"), e4.getMessage());
        final IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () -> XmlUtil.writeCharacters("a<b", (Writer) null));
        assertTrue(e5.getMessage().contains("'output'"), e5.getMessage());
        final IllegalArgumentException e6 = assertThrows(IllegalArgumentException.class,
                () -> XmlUtil.writeCharacters(new char[] { 'a' }, 0, 1, (Writer) null));
        assertTrue(e6.getMessage().contains("'output'"), e6.getMessage());
        // source and range are still validated before the destination
        assertThrows(NullPointerException.class, () -> XmlUtil.writeCharacters((char[]) null, 0, 0, (Writer) null));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", -1, 1, (OutputStream) null));
    }
}
