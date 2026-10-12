package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class ProfilerXmlLabelReview20261003Test extends TestBase {
    @ParameterizedTest
    @MethodSource("labels")
    void xmlPreservesMethodLabels(final boolean bytes, final String label) throws Exception {
        final Profiler.SingleLoopStatistics loop = new Profiler.SingleLoopStatistics();
        loop.addMethodStatistics(new Profiler.MethodStatistics(label, 0, 1, 0, 1_000_000));
        final Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 1, 0, 1_000_000, 1, List.of(loop));

        final Document xml = render(stats, bytes);
        assertEquals(1, xml.getElementsByTagName("method").getLength());
        assertEquals(String.valueOf(label), xml.getElementsByTagName("method").item(0).getAttributes().getNamedItem("name").getNodeValue());
        assertEquals("1.000", xml.getElementsByTagName("avgTime").item(0).getTextContent());
        assertEquals(List.of(String.valueOf(label)), stats.getMethodNameList());
    }

    static Stream<Arguments> labels() {
        return Stream.of(false, true)
                .flatMap(
                        bytes -> Arrays
                                .asList("alpha\tbeta", "alpha\nbeta", "alpha\rbeta", "alpha\r\nbeta", "\t<&\"'>&#9;\r\n", "alpha beta", "a<&\"'b",
                                        "literal &#9; &#10; &#13;", "\u6f22\u5b57\ud83d\ude00", "", null)
                                .stream()
                                .map(label -> Arguments.of(bytes, label)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void distinctWhitespaceLabelsRemainDistinctInTheReport(final boolean bytes) throws Exception {
        final List<String> labels = List.of("alpha\tbeta", "alpha\nbeta", "alpha\rbeta", "alpha beta");
        final Profiler.SingleLoopStatistics loop = new Profiler.SingleLoopStatistics();
        for (int i = 0; i < labels.size(); i++) {
            loop.addMethodStatistics(new Profiler.MethodStatistics(labels.get(i), 0, 1, 0, (i + 1) * 1_000_000L));
        }
        final Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 1, 0, 1_000_000, 1, List.of(loop));
        final Document xml = render(stats, bytes);
        final List<String> rendered = new ArrayList<>();
        for (int i = 0; i < xml.getElementsByTagName("method").getLength(); i++) {
            rendered.add(xml.getElementsByTagName("method").item(i).getAttributes().getNamedItem("name").getNodeValue());
        }
        assertEquals(labels, rendered);
    }

    private static Document render(final Profiler.MultiLoopsStatistics stats, final boolean bytes) throws Exception {
        final String xml;
        if (bytes) {
            final TrackingOutputStream output = new TrackingOutputStream();
            stats.writeXmlResult(output);
            assertTrue(output.flushed);
            assertFalse(output.closed);
            xml = output.toString(StandardCharsets.UTF_8);
        } else {
            final TrackingWriter output = new TrackingWriter();
            stats.writeXmlResult(output);
            assertTrue(output.flushed);
            assertFalse(output.closed);
            xml = output.toString();
        }
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static class TrackingOutputStream extends ByteArrayOutputStream {
        boolean flushed;
        boolean closed;

        @Override
        public void flush() {
            flushed = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class TrackingWriter extends java.io.StringWriter {
        boolean flushed;
        boolean closed;

        @Override
        public void flush() {
            flushed = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
