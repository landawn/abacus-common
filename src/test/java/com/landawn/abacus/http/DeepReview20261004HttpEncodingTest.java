package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserFactory;

@Tag("unit")
class DeepReview20261004HttpEncodingTest extends TestBase {
    @Test
    void rejectsUnknownEncodingBeforeOpeningAConnection() throws Exception {
        assertRejected(ContentFormat.JSON, "deflate");
    }

    @Test
    void rejectsStackedEncodingsBeforeOpeningAConnection() throws Exception {
        assertRejected(ContentFormat.JSON_GZIP, "gzip, br");
    }

    @Test
    void rejectsRepeatedEncodingBeforeOpeningAConnection() throws Exception {
        assertRejected(ContentFormat.JSON_GZIP, "gzip, gzip");
    }

    @Test
    void rejectsCompressionForAnUncompressedFormFormat() throws Exception {
        assertRejected(ContentFormat.FORM_URL_ENCODED, "gzip");
    }

    private static void assertRejected(final ContentFormat format, final String declaration) throws Exception {
        final Fixture fixture = new Fixture();
        final HttpSettings settings = HttpSettings.create().setContentFormat(format).setContentEncoding(declaration);
        final HttpClient client = HttpClient.create(fixture.url, 1, 1_000, 1_000, settings);
        assertThrows(IllegalArgumentException.class, () -> client.post(Map.of("value", "ok")), declaration);
        assertEquals(0, fixture.opens.get(), declaration);
    }

    @Test
    void gzipAliasAndIdentityStillDescribeTheWrittenBytes() throws Exception {
        final Fixture compressed = new Fixture();
        final HttpSettings gzip = HttpSettings.create().setContentFormat(ContentFormat.JSON_GZIP).setContentEncoding(" X-GZIP ");
        assertEquals("{}", HttpClient.create(compressed.url, 1, 1_000, 1_000, gzip).post(Map.of("value", "ok")));
        try (InputStream decoded = new GZIPInputStream(new ByteArrayInputStream(compressed.body.toByteArray()))) {
            assertEquals(Map.of("value", "ok"),
                    ParserFactory.createJsonParser().deserialize(new String(decoded.readAllBytes(), StandardCharsets.UTF_8), Map.class));
        }

        final Fixture plain = new Fixture();
        final HttpSettings identity = HttpSettings.create().setContentFormat(ContentFormat.JSON).setContentEncoding("identity");
        assertEquals("{}", HttpClient.create(plain.url, 1, 1_000, 1_000, identity).post(Map.of("value", "ok")));
        assertEquals(Map.of("value", "ok"), ParserFactory.createJsonParser().deserialize(plain.body.toString(StandardCharsets.UTF_8), Map.class));
    }

    private static final class Fixture {
        final AtomicInteger opens = new AtomicInteger();
        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        final URL url;

        Fixture() throws Exception {
            url = new URL(null, "http://example.test/form", new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(final URL target) {
                    opens.incrementAndGet();
                    return new HttpURLConnection(target) {
                        @Override
                        public void connect() {
                        }

                        @Override
                        public void disconnect() {
                        }

                        @Override
                        public boolean usingProxy() {
                            return false;
                        }

                        @Override
                        public OutputStream getOutputStream() {
                            return body;
                        }

                        @Override
                        public int getResponseCode() {
                            return 200;
                        }

                        @Override
                        public String getResponseMessage() {
                            return "OK";
                        }

                        @Override
                        public Map<String, List<String>> getHeaderFields() {
                            return Map.of("Content-Type", List.of("application/json"));
                        }

                        @Override
                        public InputStream getInputStream() {
                            return new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
                        }
                    };
                }
            });
        }
    }
}
