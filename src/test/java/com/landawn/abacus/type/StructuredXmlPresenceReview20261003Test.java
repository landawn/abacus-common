package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserUtil.XmlEmbeddedJsonConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

@Tag("unit")
class StructuredXmlPresenceReview20261003Test extends TestBase {

    @ParameterizedTest
    @CsvSource({ "array, nullable", "array, optional", "array, jdkOptional", "array, map", "array, array", "array, list", "collection, nullable",
            "collection, optional", "collection, jdkOptional", "collection, map", "collection, array", "collection, list", "object, nullable",
            "object, optional", "object, jdkOptional", "object, map", "object, array", "object, list" })
    void directXmlWriterRejectsPresentNullAtEveryStructuredDepth(final String container, final String shapeName) throws IOException {
        final Nullable<Integer> value = Nullable.of((Integer) null);
        final Object shape = switch (shapeName) {
            case "nullable" -> value;
            case "optional" -> Optional.of(value);
            case "jdkOptional" -> java.util.Optional.of(value);
            case "map" -> Map.of("value", value);
            case "array" -> new Object[] { value };
            case "list" -> List.of(value);
            default -> throw new AssertionError(shapeName);
        };
        for (final JsonXmlSerConfig<?> config : List.of(XmlSerConfig.create(), new XmlEmbeddedJsonConfig())) {
            assertThrows(ParsingException.class, () -> write(container, new Object[] { shape }, config), shapeName);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "array", "collection", "object" })
    void absentAndPresentValuesKeepCompactEscapedXmlText(final String container) throws IOException {
        final Object[] values = { null, Nullable.empty(), Optional.empty(), java.util.Optional.empty(), Nullable.of(7), "<&\"", Map.of("value", 3) };
        final String expected = "[null, null, null, null, 7, &quot;&lt;&amp;\\&quot;&quot;, {&quot;value&quot;: 3}]";
        assertEquals(expected, write(container, values, XmlSerConfig.create().setPrettyFormat(true).setWriteNullNumberAsZero(true)));
        assertEquals("[]", write(container, new Object[0], XmlSerConfig.create()));
        assertEquals("null", write(container, null, XmlSerConfig.create()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "array", "collection", "object" })
    void ordinaryJsonStillAllowsPresentNull(final String container) throws IOException {
        assertEquals("[null]", write(container, new Object[] { Nullable.of(null) }, JsonSerConfig.create()));
        assertTrue(write(container, new Object[] { Map.of("value", Nullable.of(null)) }, JsonSerConfig.create()).contains("null"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "map", "list", "bean" })
    void genericObjectDispatchPreservesPresenceForEachStructuredRuntimeType(final String shape) throws IOException {
        final Type<Object> type = Type.of(Object.class);
        final Object presentNull = objectShape(shape, Nullable.of((Integer) null));
        assertThrows(ParsingException.class, () -> writeValue(type, presentNull, XmlSerConfig.create()));
        final String emptyText = shape.equals("list") ? "[null]" : "{&quot;value&quot;: null}";
        assertEquals(emptyText, writeValue(type, objectShape(shape, Nullable.empty()), XmlSerConfig.create()));
        assertTrue(writeValue(type, objectShape(shape, Nullable.of(7)), XmlSerConfig.create()).contains("7"));
        assertTrue(writeValue(type, presentNull, JsonSerConfig.create()).contains("null"));
    }

    private static Object objectShape(final String shape, final Nullable<Integer> value) {
        return switch (shape) {
            case "map" -> Map.of("value", value);
            case "list" -> List.of(value);
            case "bean" -> new PresenceBean(value);
            default -> throw new AssertionError(shape);
        };
    }

    private static String write(final String container, final Object[] values, final JsonXmlSerConfig<?> config) throws IOException {
        final Type<Object> type = Type.of(switch (container) {
            case "array" -> "Object[]";
            case "collection" -> "List<Object>";
            case "object" -> "Object";
            default -> throw new AssertionError(container);
        });
        final Object value = values == null ? null : container.equals("collection") ? Arrays.asList(values) : values;
        return writeValue(type, value, config);
    }

    private static String writeValue(final Type<Object> type, final Object value, final JsonXmlSerConfig<?> config) throws IOException {
        final var writer = Objectory.createBufferedXmlWriter();
        try {
            type.serializeTo(writer, value, config);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    public static final class PresenceBean {
        private Nullable<Integer> value;

        public PresenceBean() {
        }

        PresenceBean(final Nullable<Integer> value) {
            this.value = value;
        }

        public Nullable<Integer> getValue() {
            return value;
        }

        public void setValue(final Nullable<Integer> value) {
            this.value = value;
        }
    }
}
