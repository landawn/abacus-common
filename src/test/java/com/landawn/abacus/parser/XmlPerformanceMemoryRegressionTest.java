package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.u.Nullable;

import testfixtures.XmlPerformanceMemoryFixtures.FactoryCow;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
public class XmlPerformanceMemoryRegressionTest extends TestBase {
    private static final XmlParserType[] BACKENDS = { XmlParserType.StAX, XmlParserType.DOM };

    @Test
    public void copyOnWriteListsKeepExactResultClassOrderNullsAndPropertyTypes() {
        final Type<CopyOnWriteArrayList<String>> type = Type.of("java.util.concurrent.CopyOnWriteArrayList<String>");
        final List<String> expected = Arrays.asList("a", null, "a", "b");
        for (final XmlParserType backend : BACKENDS) {
            final XmlParserImpl parser = new XmlParserImpl(backend);
            for (final String xml : new String[] { "<list><e>a</e><null isNull=\"true\"/><e>a</e><e>b</e></list>", "<list>[\"a\",null,\"a\",\"b\"]</list>" }) {
                final CopyOnWriteArrayList<String> result = parser.deserialize(xml, type);
                assertSame(CopyOnWriteArrayList.class, result.getClass());
                assertEquals(expected, result);
                result.add("after");
                assertEquals(5, result.size());
            }
            final CopyOnWriteArrayList<String> emptyElement = parser.deserialize("<list/>", type);
            assertSame(CopyOnWriteArrayList.class, emptyElement.getClass());
            assertTrue(emptyElement.isEmpty());
            assertTrue(parser.deserialize("<list>[]</list>", type).isEmpty());

            final CowBean result = parser.deserialize(
                    "<cowBean><items><item><value>3</value></item><null isNull=\"true\"/><item><value>5</value></item></items></cowBean>", CowBean.class);
            assertSame(CopyOnWriteArrayList.class, result.items.getClass());
            assertEquals(3, result.items.size());
            assertEquals(3, result.items.get(0).value);
            assertEquals(null, result.items.get(1));
            assertEquals(5, result.items.get(2).value);
        }
    }

    @Test
    public void copyOnWriteSubclassesKeepInsertionAndFactoryBehavior() {
        for (final XmlParserType backend : BACKENDS) {
            CountingCow.added.set(0);
            final CountingCow result = new XmlParserImpl(backend).deserialize("<list><e>a</e><e>b</e></list>", CountingCow.class);
            assertEquals(List.of("a", "b"), result);
            assertEquals(2, CountingCow.added.get());
        }
        final AtomicInteger factories = new AtomicInteger();
        assertTrue(Suppliers.registerForCollection(FactoryCow.class, () -> {
            factories.incrementAndGet();
            return new FactoryCow("factory");
        }));
        final FactoryCow result = new XmlParserImpl(XmlParserType.StAX).deserialize("<list><e>a</e></list>", FactoryCow.class);
        assertEquals("factory", result.marker);
        assertEquals(List.of("a"), result);
        assertEquals(1, factories.get());
        assertNull(AbstractParser.collectionCreatorAndConverter(CopyOnWriteArrayList.class, false));
    }

    @Test
    public void domCompactContainersReadFragmentedTextOnce() throws Exception {
        final XmlParser parser = new XmlParserImpl(XmlParserType.DOM);
        for (final String xml : new String[] { "<list>[1,<![CDATA[2,]]>3]</list>", "<map>{\"a\":<![CDATA[1]]>}</map>" }) {
            final Node node = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
            final AtomicInteger reads = new AtomicInteger();
            final Node proxy = (Node) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Node.class }, (p, method, args) -> {
                if (method.getName().equals("getTextContent")) {
                    reads.incrementAndGet();
                }
                try {
                    return method.invoke(node, args);
                } catch (final InvocationTargetException e) {
                    throw e.getCause();
                }
            });
            final boolean list = xml.startsWith("<list>");
            final Object result = parser.deserialize(proxy, Type.of(list ? "List<Object>" : "Map<String,Object>"));
            assertEquals(list ? List.of(1, 2, 3) : Map.of("a", 1), result);
            assertEquals(1, reads.get());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void streamedEmbeddedJsonMatchesBufferedEscapingAndWrapperPolicy() throws Exception {
        final XmlParserImpl parser = new XmlParserImpl(XmlParserType.StAX);
        final String special = "<&>\"'\t\r\n\u0000\uD83D\uDE00\uD800x\uDC00\uFFFE\uFFFF";
        final RawPayload raw = new RawPayload();
        // Raw JSON text includes valid and isolated surrogates before/after many JSON buffer boundaries.
        raw.payload = "\"" + ("a".repeat(257) + "\uD83D\uDE00\uD800x\uDC00\uFFFE\uFFFF<&>").repeat(140) + "\"";
        final List<Object> values = Arrays.asList(null, "<&>\"'", new int[] { 1, 2, 3 }, Arrays.asList(special.repeat(2000), null, "tail"),
                Map.of("payload", List.of(special)), raw, java.util.Optional.of(List.of(special)), Tuple.of(special, 3), Holder.of(List.of(special)));

        for (final char quotation : new char[] { 0, '\'', '"' }) {
            final XmlSerConfig config = new XmlSerConfig().setStringQuotation(quotation);
            for (final Object value : values) {
                for (final boolean rawValue : new boolean[] { false, true }) {
                    assertEquals(bufferedEmbedding(parser, value, config, rawValue), streamedEmbedding(parser, value, config, rawValue));
                }
            }
        }
        assertThrows(ParsingException.class, () -> streamedEmbedding(parser, Nullable.of(null), new XmlSerConfig(), false));
        assertThrows(ParsingException.class, () -> streamedEmbedding(parser, "\u0000", new XmlSerConfig(), true));
        final RawPayload invalidRaw = new RawPayload();
        invalidRaw.payload = "\u0000";
        assertThrows(ParsingException.class, () -> streamedEmbedding(parser, invalidRaw, new XmlSerConfig(), true));
        final List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        final XmlSerConfig cycles = new XmlSerConfig().setCircularReferenceSupported(true);
        assertEquals(bufferedEmbedding(parser, cycle, cycles, false), streamedEmbedding(parser, cycle, cycles, false));
    }

    @Test
    public void embeddedJsonDoesNotFlushOrCloseTheEnclosingWriter() throws Exception {
        final TrackingWriter destination = new TrackingWriter();
        final BufferedXmlWriter writer = Objectory.createBufferedXmlWriter(destination);
        try {
            final XmlParserImpl parser = new XmlParserImpl(XmlParserType.StAX);
            parser.writeEmbeddedJson(writer, List.of("x".repeat(100_000)), new XmlSerConfig());
            parser.writeEmbeddedRawJson(writer, Map.of("key", "value"), new XmlSerConfig());
            assertEquals(0, destination.flushes);
            assertEquals(0, destination.closes);
            writer.flush();
            assertEquals(1, destination.flushes);
            assertTrue(destination.toString().startsWith("[&quot;"));
            assertEquals(bufferedEmbedding(parser, List.of("x".repeat(100_000)), new XmlSerConfig(), false)
                    + bufferedEmbedding(parser, Map.of("key", "value"), new XmlSerConfig(), true), destination.toString());
        } finally {
            Objectory.recycle(writer);
        }
        assertEquals(0, destination.closes);
    }

    @Test
    public void classLifetimeConstructorProbeRunsOnceUnderContentionAndCachesFailures() throws Exception {
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Class<?>>> futures = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 32; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return AbstractParser.getConcreteClass(ProbedList.class, List.class);
                }));
            }
            start.countDown();
            for (final Future<Class<?>> future : futures) {
                assertSame(ProbedList.class, future.get());
            }
        }
        assertEquals(1, ProbedList.constructed.get());
        for (int i = 0; i < 8; i++) {
            assertSame(List.class, AbstractParser.getConcreteClass(FailingList.class, List.class));
        }
        assertEquals(1, FailingList.constructed.get());
        assertSame(List.class, AbstractParser.getConcreteClass(RecursiveList.class, List.class));
        assertSame(List.class, AbstractParser.getConcreteClass(RecursiveList.class, List.class));
        assertEquals(1, RecursiveList.constructed.get());
        assertSame(List.class, AbstractParser.getConcreteClass(List.of(1).getClass(), List.class));
        assertSame(Integer.class, AbstractParser.getConcreteClass(Integer.class, Number.class));
    }

    private static String bufferedEmbedding(final XmlParserImpl parser, final Object value, final XmlSerConfig config, final boolean raw) throws IOException {
        final BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();
        try {
            final String json = parser.serializeEmbeddedJson(value, config);
            if (raw) {
                AbstractXmlParser.writeRawJson(writer, json);
            } else {
                Type.<String> of(String.class).serializeTo(writer, json, config);
            }
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    private static String streamedEmbedding(final XmlParserImpl parser, final Object value, final XmlSerConfig config, final boolean raw) throws IOException {
        final BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();
        try {
            if (raw) {
                parser.writeEmbeddedRawJson(writer, value, config);
            } else {
                parser.writeEmbeddedJson(writer, value, config);
            }
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    public static class Item {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }
    }

    public static class CowBean {
        private CopyOnWriteArrayList<Item> items;

        public CopyOnWriteArrayList<Item> getItems() {
            return items;
        }

        public void setItems(final CopyOnWriteArrayList<Item> items) {
            this.items = items;
        }
    }

    public static class CountingCow extends CopyOnWriteArrayList<Object> {
        static final AtomicInteger added = new AtomicInteger();

        @Override
        public boolean add(final Object value) {
            added.incrementAndGet();
            return super.add(value);
        }
    }

    public static class RawPayload {
        @JsonXmlField(isJsonRawValue = true)
        private String payload;

        public String getPayload() {
            return payload;
        }

        public void setPayload(final String payload) {
            this.payload = payload;
        }
    }

    public static class ProbedList extends ArrayList<Object> {
        static final AtomicInteger constructed = new AtomicInteger();

        public ProbedList() {
            constructed.incrementAndGet();
        }
    }

    public static class FailingList extends ArrayList<Object> {
        static final AtomicInteger constructed = new AtomicInteger();

        public FailingList() {
            constructed.incrementAndGet();
            throw new IllegalStateException("expected constructor failure");
        }
    }

    public static class RecursiveList extends ArrayList<Object> {
        static final AtomicInteger constructed = new AtomicInteger();

        public RecursiveList() {
            constructed.incrementAndGet();
            AbstractParser.getConcreteClass(RecursiveList.class, List.class);
        }
    }

    private static class TrackingWriter extends StringWriter {
        int flushes;
        int closes;

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closes++;
        }
    }
}
