package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

@Tag("unit")
class DeepReview20261004StructuredValueTest extends TestBase {

    @Test
    void annotatedMethodWritesTheWrappedMapAsAnObject() throws IOException {
        final Type<MethodValue> type = Type.of(MethodValue.class);
        final MethodValue value = MethodValue.of(Map.of("number", 7));

        assertTrue(type.isSerializable());
        assertEquals("{\"number\": 7}", serialize(type, value));
        assertEquals("[{\"number\": 7}]", N.toJson(List.of(value)));
        final List<MethodValue> restored = N.fromJson("[{\"number\": 7}]", Type.of("List<" + MethodValue.class.getName() + ">"));
        assertEquals(value.value(), restored.get(0).value());
    }

    @Test
    void annotatedFieldWritesTheWrappedMapAsAnObject() throws IOException {
        final Type<FieldValue> type = Type.of(FieldValue.class);
        final FieldValue value = FieldValue.of(Map.of("number", 7));

        assertTrue(type.isSerializable());
        assertEquals("{\"number\": 7}", serialize(type, value));
        assertEquals("[{\"number\": 7}]", N.toJson(List.of(value)));
        assertEquals(value.value, type.valueOf(type.stringOf(value)).value);
    }

    @Test
    void detectedFactoryWritesTheWrappedMapAsAnObject() throws IOException {
        final Type<DetectedValue> type = Type.of(DetectedValue.class);
        final DetectedValue value = DetectedValue.of(Map.of("number", 7));

        assertTrue(type.isSerializable());
        assertEquals("{\"number\": 7}", serialize(type, value));
        assertEquals("[{\"number\": 7}]", N.toJson(List.of(value)));
        assertEquals(value.value(), type.valueOf(type.stringOf(value)).value());
    }

    @Test
    void structuredWrappedValueStillEscapesForAnXmlWriter() throws IOException {
        final Type<TextValue> type = Type.of(TextValue.class);
        final BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();
        try {
            type.serializeTo(writer, TextValue.of(Map.of("text", "<a&b>")), new JsonSerConfig());
            assertEquals("{&quot;text&quot;: &quot;&lt;a&amp;b&gt;&quot;}", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    void listArrayAndBeanValuesRetainTheirJsonShapes() throws IOException {
        final ListValue list = ListValue.of(List.of(Map.of("number", 7)));
        final ArrayValue array = ArrayValue.of(new Object[] { 7, "text" });
        final BeanValue bean = BeanValue.of(new NumberBean(7));

        assertEquals("[{\"number\": 7}]", serialize(Type.of(ListValue.class), list));
        assertEquals("[7, \"text\"]", serialize(Type.of(ArrayValue.class), array));
        assertEquals("{\"number\": 7}", serialize(Type.of(BeanValue.class), bean));
        assertEquals("[[{\"number\": 7}]]", N.toJson(List.of(list)));
        assertEquals("[[7, \"text\"]]", N.toJson(List.of(array)));
        assertEquals("[{\"number\": 7}]", N.toJson(List.of(bean)));
        assertEquals(list.value, N.fromJson(N.toJson(list), ListValue.class).value);
        assertArrayEquals(array.value, N.fromJson(N.toJson(array), ArrayValue.class).value);
        assertEquals(7, N.fromJson(N.toJson(bean), BeanValue.class).value.number);
    }

    @Test
    void xmlWritesRootContentAndRoundTripsStructuredBeanProperties() {
        final MethodValue value = MethodValue.of(Map.of("number", 7));
        // Scalar XML roots are character content; XML deserialization supports them as bean properties.
        assertEquals("{&quot;number&quot;: 7}", N.toXml(value));

        final Envelope envelope = new Envelope();
        envelope.setValue(value);
        final Envelope restored = N.fromXml(N.toXml(envelope), Envelope.class);
        assertEquals(value.value(), restored.getValue().value());
        assertEquals(value.value(), N.fromJson(N.toJson(envelope), Envelope.class).getValue().value());
    }

    @Test
    void scalarAnnotatedValuesKeepTheirConfiguredValueAndMapKeyQuoting() throws IOException {
        final LongValue value = LongValue.of(7);
        final Type<LongValue> type = Type.of(LongValue.class);
        final JsonSerConfig config = new JsonSerConfig().setWriteLongAsString(true);
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(writer, value, config);
            assertEquals("\"7\"", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
        assertEquals("{\"7\": \"text\"}", N.toJson(Map.of(value, "text"), config));
        assertEquals(7, N.fromJson(N.toJson(value), LongValue.class).value);
    }

    private static <T> String serialize(final Type<T> type, final T value) throws IOException {
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(writer, value, new JsonSerConfig());
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    public static final class MethodValue {
        private final Map<String, Integer> value;

        private MethodValue(final Map<String, Integer> value) {
            this.value = value;
        }

        @JsonXmlValue
        public Map<String, Integer> value() {
            return value;
        }

        @JsonXmlCreator
        public static MethodValue of(final Map<String, Integer> value) {
            return new MethodValue(value);
        }
    }

    public static final class FieldValue {
        @JsonXmlValue
        private final Map<String, Integer> value;

        private FieldValue(final Map<String, Integer> value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static FieldValue of(final Map<String, Integer> value) {
            return new FieldValue(value);
        }
    }

    public static final class DetectedValue {
        private Map<String, Integer> value;

        private DetectedValue(final Map<String, Integer> value) {
            this.value = value;
        }

        public Map<String, Integer> value() {
            return value;
        }

        public static DetectedValue of(final Map<String, Integer> value) {
            return new DetectedValue(value);
        }
    }

    public static final class TextValue {
        @JsonXmlValue
        private final Map<String, String> value;

        private TextValue(final Map<String, String> value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static TextValue of(final Map<String, String> value) {
            return new TextValue(value);
        }
    }

    public static final class ListValue {
        @JsonXmlValue
        private final List<Map<String, Integer>> value;

        private ListValue(final List<Map<String, Integer>> value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static ListValue of(final List<Map<String, Integer>> value) {
            return new ListValue(value);
        }
    }

    public static final class ArrayValue {
        @JsonXmlValue
        private final Object[] value;

        private ArrayValue(final Object[] value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static ArrayValue of(final Object[] value) {
            return new ArrayValue(value);
        }
    }

    public static final class BeanValue {
        @JsonXmlValue
        private final NumberBean value;

        private BeanValue(final NumberBean value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static BeanValue of(final NumberBean value) {
            return new BeanValue(value);
        }
    }

    public static final class NumberBean {
        public int number;

        public NumberBean() {
        }

        NumberBean(final int number) {
            this.number = number;
        }
    }

    public static final class Envelope {
        private MethodValue value;

        public MethodValue getValue() {
            return value;
        }

        public void setValue(final MethodValue value) {
            this.value = value;
        }
    }

    public static final class LongValue {
        @JsonXmlValue
        private final long value;

        private LongValue(final long value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static LongValue of(final long value) {
            return new LongValue(value);
        }
    }
}
