package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Queue;

import javax.xml.parsers.SAXParser;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.XmlUtil;

import jakarta.xml.bind.annotation.XmlRootElement;

public class JaxbParserPoolFailureTest extends TestBase {
    @XmlRootElement(name = "root")
    public static class JaxbPoolRoot {
        public String value;
    }

    private static Reader failingReader(final Throwable failure) {
        return new Reader() {
            @Override
            public int read(final char[] chars, final int offset, final int length) throws IOException {
                if (failure instanceof IOException e) {
                    throw e;
                } else if (failure instanceof RuntimeException e) {
                    throw e;
                }
                throw (Error) failure;
            }

            @Override
            public void close() {
            }
        };
    }

    // JAXB parses through the pooled parser's XMLReader. That reader records a failed parse, so JaxbParser can recycle
    // from its finally block: a failed parser is discarded, a successful one is reused.
    @Test
    public void failedUnmarshallingDiscardsTheParserAndSuccessfulUnmarshallingReusesIt() throws Exception {
        final var field = XmlUtil.class.getDeclaredField("saxParserPool");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Queue<WeakReference<SAXParser>> pool = (Queue<WeakReference<SAXParser>>) field.get(null);
        synchronized (pool) {
            final var saved = new ArrayList<>(pool);
            pool.clear();
            try {
                final XmlParser parser = ParserFactory.createJaxbParser();
                final SAXParser failed = XmlUtil.createSAXParser();
                XmlUtil.recycleSAXParser(failed);
                assertThrows(ParsingException.class, () -> parser.deserialize("<root>", JaxbPoolRoot.class));
                assertTrue(pool.isEmpty(), "a malformed document is a failed parse through the exposed XMLReader");

                final SAXParser ioFailed = XmlUtil.createSAXParser();
                XmlUtil.recycleSAXParser(ioFailed);
                assertThrows(UncheckedIOException.class, () -> parser.deserialize(failingReader(new IOException("read failed")), JaxbPoolRoot.class));
                assertTrue(pool.isEmpty());

                // Unchecked failures escape JAXB without an UnmarshalException; they are recorded by the reader all the same.
                for (final Throwable failure : new Throwable[] { new IllegalStateException("read failed"), new AssertionError("read failed") }) {
                    final SAXParser uncheckedFailed = XmlUtil.createSAXParser();
                    XmlUtil.recycleSAXParser(uncheckedFailed);
                    assertThrows(Throwable.class, () -> parser.deserialize(failingReader(failure), JaxbPoolRoot.class));
                    assertTrue(pool.isEmpty(), failure.toString());
                }

                final SAXParser healthy = XmlUtil.createSAXParser();
                XmlUtil.recycleSAXParser(healthy);
                parser.deserialize(new StringReader("<root><value>ok</value></root>"), JaxbPoolRoot.class);
                assertSame(healthy, XmlUtil.createSAXParser(), "successful JAXB reads still reuse the parser");
            } finally {
                pool.clear();
                pool.addAll(saved);
            }
        }
    }
}
