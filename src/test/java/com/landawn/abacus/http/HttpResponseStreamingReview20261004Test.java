package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

public class HttpResponseStreamingReview20261004Test extends TestBase {
    @Test
    void retainedStructuredBytesDecodeDirectlyForClassAndParameterizedType() {
        final Map<String, List<String>> value = Map.of("values", List.of("caf\u00e9".repeat(40_000), "second"));
        final Type<Map<String, List<String>>> type = Type.of("Map<String, List<String>>");
        for (final boolean xml : new boolean[] { false, true }) {
            final Charset charset = xml ? StandardCharsets.UTF_16LE : StandardCharsets.ISO_8859_1;
            final String text = xml ? N.toXml(value) : N.toJson(value);
            final HttpResponse response = new HttpResponse("http://example.test", 0, 1, 200, "OK", Map.of(), text.getBytes(charset),
                    xml ? ContentFormat.XML : ContentFormat.JSON, charset);
            assertEquals(xml ? N.fromXml(text, Map.class) : value, response.body(Map.class));
            assertEquals(xml ? N.fromXml(text, type) : value, response.body(type));
            assertEquals(text, response.body(String.class));
        }
    }

    @Test
    void noFormatScalarAndFormConversionsKeepTheirExistingBehavior() {
        final HttpResponse scalar = new HttpResponse("http://example.test", 0, 1, 200, "OK", Map.of(), "123".getBytes(StandardCharsets.UTF_8),
                ContentFormat.NONE, StandardCharsets.UTF_8);
        assertEquals(123, scalar.body(Integer.class));
        assertEquals(123, scalar.body(Type.of(Integer.class)));
        final HttpResponse form = new HttpResponse("http://example.test", 0, 1, 200, "OK", Map.of(), "name=caf%E9".getBytes(StandardCharsets.ISO_8859_1),
                ContentFormat.FORM_URL_ENCODED, StandardCharsets.ISO_8859_1);
        assertEquals(Map.of("name", "caf\u00e9"), form.body(Map.class));
    }

    @Test
    void emptyStructuredBodiesRetainStringParserDefaultsForClassAndType() {
        for (final ContentFormat format : new ContentFormat[] { ContentFormat.XML, ContentFormat.XML_GZIP, ContentFormat.JSON, ContentFormat.JSON_GZIP }) {
            final HttpResponse response = new HttpResponse("http://example.test", 0, 1, 200, "OK", Map.of(), new byte[0], format, StandardCharsets.UTF_8);
            for (final Class<?> cls : new Class<?>[] { Integer.class, int.class, Map.class, List.class, Object[].class }) {
                assertEquals(HttpUtil.getParser(format).deserialize("", cls), response.body(cls), format + " " + cls);
                final Type<?> type = Type.of(cls);
                assertEquals(HttpUtil.getParser(format).deserialize("", type), response.body(type), format + " " + type);
            }
            final Type<Map<String, List<Integer>>> nestedType = Type.of("Map<String,List<Integer>>");
            assertEquals(HttpUtil.getParser(format).deserialize("", nestedType), response.body(nestedType));
            assertEquals("", response.body(String.class));
            assertArrayEquals(new byte[0], response.body(byte[].class));
        }
        final HttpResponse xml = new HttpResponse("http://example.test", 0, 1, 200, "OK", Map.of(), new byte[0], ContentFormat.XML, StandardCharsets.UTF_8);
        assertNull(xml.body(Map.class));
        assertEquals(0, xml.body(int.class));
    }

    @Test
    void whitespaceXmlIsStillParsedAndRejectedRatherThanTreatedAsAnEmptyBody() {
        final HttpResponse response = new HttpResponse("http://example.test", 0, 1, 200, "OK", Map.of(), " \t\r\n".getBytes(StandardCharsets.UTF_8),
                ContentFormat.XML, StandardCharsets.UTF_8);
        assertThrows(RuntimeException.class, () -> response.body(Map.class));
        assertThrows(RuntimeException.class, () -> response.body(Type.of("Map<String,Integer>")));
    }
}
