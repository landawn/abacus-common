package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;

public class JsonHttpMessageConverterContractTest extends TestBase {
    @Test
    public void nonemptyMediaTypesReplaceDefaultsAndAreCopied() {
        final MediaType custom = new MediaType("application", "vnd.review+json");
        final MediaType[] array = { custom };
        final List<MediaType> list = new ArrayList<>(List.of(custom));
        final List<JsonHttpMessageConverter> converters = List.of(new JsonHttpMessageConverter(array),
                new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), array),
                new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), list));
        array[0] = MediaType.TEXT_PLAIN;
        list.clear();
        for (final JsonHttpMessageConverter converter : converters) {
            assertEquals(List.of(custom), converter.getSupportedMediaTypes());
            assertTrue(converter.canRead(String.class, custom));
            assertTrue(converter.canWrite(String.class, custom));
            assertFalse(converter.canRead(String.class, MediaType.APPLICATION_JSON));
            assertFalse(converter.canWrite(String.class, MediaType.APPLICATION_JSON));
        }
    }

    @Test
    public void nullAndEmptyMediaTypeContainersRetainDefaults() {
        final List<MediaType> defaults = new JsonHttpMessageConverter().getSupportedMediaTypes();
        final List<JsonHttpMessageConverter> converters = List.of(new JsonHttpMessageConverter((MediaType[]) null),
                new JsonHttpMessageConverter(new MediaType[0]), new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), (MediaType[]) null),
                new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), new MediaType[0]),
                new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), (List<MediaType>) null),
                new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), List.of()));
        for (final JsonHttpMessageConverter converter : converters) {
            assertEquals(defaults, converter.getSupportedMediaTypes());
        }
    }

    @Test
    public void publicReadLeavesUnicodeInputOpen() throws IOException {
        final boolean[] closed = { false };
        final InputStream input = new ByteArrayInputStream("{\"value\":\"\u540D\uD83D\uDE80\"}".getBytes(StandardCharsets.UTF_16LE)) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_16LE));
        assertEquals(Map.of("value", "\u540D\uD83D\uDE80"), new JsonHttpMessageConverter().read(Map.class, message(input, headers)));
        assertFalse(closed[0]);
        input.close();
        assertTrue(closed[0]);
    }

    @Test
    public void failedPublicReadAlsoLeavesInputOpen() {
        final boolean[] closed = { false };
        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };
        assertThrows(HttpMessageNotReadableException.class, () -> new JsonHttpMessageConverter().read(String.class, message(input, new HttpHeaders())));
        assertFalse(closed[0]);
    }

    @Test
    public void directReadHookLeavesEmptyAndNullInputReadersOpen() {
        for (final String json : List.of("", "null")) {
            final boolean[] closed = { false };
            final StringReader reader = new StringReader(json) {
                @Override
                public void close() {
                    closed[0] = true;
                }
            };
            new JsonHttpMessageConverter().readInternal(Map.class, reader);
            assertFalse(closed[0]);
        }
    }

    @Test
    public void directWriteHookLeavesWriterOpen() {
        final boolean[] closed = { false };
        final StringWriter writer = new StringWriter() {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        final JsonHttpMessageConverter converter = new JsonHttpMessageConverter();
        converter.writeInternal(List.of("\u540D\uD83D\uDE80"), List.class, writer);
        assertEquals("[\"\u540D\uD83D\uDE80\"]", writer.toString());
        assertFalse(closed[0]);
    }

    // ---- F112 review fix 2026-09-08: the "emit valid JSON" rule now covers structured roots too ----

    public static class Widget {
        public int id = 123;
        public String name = "w";
    }

    private static List<JsonSerConfig> jsonIncapableSerConfigs() {
        // R04 review 2026-09-08: bracketRootValue=false was the one "cannot produce JSON" switch the guard
        // missed - it drops the enclosing [ ] / { } of a structured root, so a bean body left the converter as
        // `"id": 123, "name": "w"`.
        return List.of(new JsonSerConfig().setStringQuotation('\''), new JsonSerConfig().setStringQuotation((char) 0),
                new JsonSerConfig().setCharQuotation('\''), new JsonSerConfig().setQuotePropName(false), new JsonSerConfig().setQuoteMapKey(false),
                new JsonSerConfig().setBracketRootValue(false));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void serConfigThatCannotProduceJsonIsRejectedByEveryConstructor() {
        for (final JsonSerConfig bad : jsonIncapableSerConfigs()) {
            assertThrows(IllegalArgumentException.class, () -> new JsonHttpMessageConverter(bad, new JsonDeserConfig()));
            assertThrows(IllegalArgumentException.class, () -> new JsonHttpMessageConverter(bad, new JsonDeserConfig(), MediaType.APPLICATION_JSON));
            assertThrows(IllegalArgumentException.class, () -> new JsonHttpMessageConverter(bad, new JsonDeserConfig(), List.of(MediaType.APPLICATION_JSON)));
        }

        // the defaults, and settings that do not affect JSON validity, are still accepted
        assertEquals("{\"id\": 123, \"name\": \"w\"}", write(new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig()), new Widget()));
        assertEquals("{\"id\": 123, \"name\": \"w\"}",
                write(new JsonHttpMessageConverter(new JsonSerConfig().setPrettyFormat(false), new JsonDeserConfig()), new Widget()));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void serConfigMutatedAfterConstructionIsRejectedForBothRootShapes() {
        // A bean/map/collection root used to take the untouched N.toJson(..) fast path, so a converter that
        // rejected a String body with ParsingException happily wrote {id: 123} - not JSON - for a bean body.
        // The configuration is retained by reference, so the check has to run per write, not only at wiring.
        for (final JsonSerConfig bad : jsonIncapableSerConfigs()) {
            final JsonSerConfig good = new JsonSerConfig();
            final JsonHttpMessageConverter converter = new JsonHttpMessageConverter(good, new JsonDeserConfig());
            assertEquals("{\"id\": 123, \"name\": \"w\"}", write(converter, new Widget()));

            good.setStringQuotation(bad.getStringQuotation())
                    .setCharQuotation(bad.getCharQuotation())
                    .setQuotePropName(bad.isQuotePropName())
                    .setQuoteMapKey(bad.isQuoteMapKey())
                    .setBracketRootValue(bad.isBracketRootValue());

            assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(new Widget(), Widget.class, new StringWriter()));
            assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(Map.of("k", "v"), Map.class, new StringWriter()));
            assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(List.of("a"), List.class, new StringWriter()));
            assertThrows(IllegalArgumentException.class, () -> converter.writeInternal("scalar", String.class, new StringWriter()));
            assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(null, String.class, new StringWriter()));
        }
    }

    // R04 review 2026-09-08: bracketRootValue=false drops the enclosing brackets of a bean/map/collection root,
    // so the converter handed Spring `"id": 123, "name": "w"` as an application/json body - the very failure the
    // quotation checks above were added to prevent. It belongs to the same family and must be refused the same way.
    @SuppressWarnings("deprecation")
    @Test
    public void reviewFixes20260908_bracketRootValueFalseIsRejectedLikeTheQuotationSwitches() {
        assertThrows(IllegalArgumentException.class, () -> new JsonHttpMessageConverter(new JsonSerConfig().setBracketRootValue(false), new JsonDeserConfig()));

        final JsonSerConfig good = new JsonSerConfig();
        final JsonHttpMessageConverter converter = new JsonHttpMessageConverter(good, new JsonDeserConfig());
        assertEquals("{\"id\": 123, \"name\": \"w\"}", write(converter, new Widget()));
        assertEquals("[\"a\", \"b\"]", write(converter, new ArrayList<>(List.of("a", "b"))));

        // retained by reference, so the per-write check is what catches a later mutation
        good.setBracketRootValue(false);

        assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(new Widget(), Widget.class, new StringWriter()));
        assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(List.of("a", "b"), List.class, new StringWriter()));
        assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(Map.of("k", "v"), Map.class, new StringWriter()));
        // a scalar root has no brackets to lose, but the configuration is still refused rather than half-honoured
        assertThrows(IllegalArgumentException.class, () -> converter.writeInternal("scalar", String.class, new StringWriter()));

        // and restoring it makes the same converter usable again
        good.setBracketRootValue(true);
        assertEquals("{\"id\": 123, \"name\": \"w\"}", write(converter, new Widget()));
    }

    private static String write(final JsonHttpMessageConverter converter, final Object value) {
        final StringWriter writer = new StringWriter();
        converter.writeInternal(value, value == null ? Object.class : value.getClass(), writer);
        return writer.toString();
    }

    private static HttpInputMessage message(final InputStream input, final HttpHeaders headers) {
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return input;
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
    }
}
