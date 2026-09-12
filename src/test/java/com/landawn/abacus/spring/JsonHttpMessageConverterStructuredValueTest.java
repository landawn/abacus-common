package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple;

public class JsonHttpMessageConverterStructuredValueTest extends TestBase {
    private Map.Entry<String, List<Integer>> entry;
    private AbstractMap.SimpleImmutableEntry<String, List<Integer>> immutableEntry;
    private Timed<List<String>> timed;
    private Indexed<List<Integer>> indexed;
    private Pair<Long, List<String>> pair;
    private Triple<String, Integer, List<String>> triple;
    private Tuple.Tuple2<Integer, List<String>> tuple;
    private IntList primitiveList;
    private Holder<List<String>> holder;
    private Holder<Optional<String>> nestedHolder;

    private final JsonHttpMessageConverter converter = new JsonHttpMessageConverter();

    static Stream<Arguments> structuredValues() {
        return Stream.of(Arguments.of("entry", Map.entry("\u540D", List.of(1, 2))),
                Arguments.of("immutableEntry", new AbstractMap.SimpleImmutableEntry<>("\u540D", List.of(1, 2))),
                Arguments.of("timed", Timed.of(List.of("\uD83D\uDE80", ""), Long.MAX_VALUE)),
                Arguments.of("indexed", Indexed.of(List.of(1, 2), Long.MAX_VALUE)), Arguments.of("pair", Pair.of(Long.MIN_VALUE, List.of("\u540D"))),
                Arguments.of("triple", Triple.of("", 1, List.of("\uD83D\uDE80"))), Arguments.of("tuple", Tuple.of(1, List.of("\u540D"))),
                Arguments.of("primitiveList", IntList.of(Integer.MIN_VALUE, Integer.MAX_VALUE)));
    }

    private Type fieldType(final String name) throws Exception {
        return getClass().getDeclaredField(name).getGenericType();
    }

    private Object read(final String json, final Type type) {
        return converter.readInternal(type, new StringReader(json));
    }

    private String write(final Object value) {
        final var writer = new StringWriter();
        converter.writeInternal(value, null, writer);
        return writer.toString();
    }

    @ParameterizedTest
    @MethodSource("structuredValues")
    public void builtInValueHandlersPreserveNestedShapes(final String name, final Object value) throws Exception {
        final String expected = N.toJson(value);
        assertEquals(expected, write(value));
        assertEquals(value, read(expected, fieldType(name)));
        assertThrows(ParsingException.class, () -> read(expected + " true", fieldType(name)));
    }

    @Test
    public void customArrayValuesAndEnumsRoundTrip() {
        for (final int[] values : List.of(new int[0], new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE })) {
            final ArrayValue value = ArrayValue.of(values);
            assertEquals(N.toJson(value), write(value));
            assertArrayEquals(values, ((ArrayValue) read(write(value), ArrayValue.class)).values());
        }
        assertEquals(N.toJson(ArrayEnum.VALUE), write(ArrayEnum.VALUE));
        assertEquals(ArrayEnum.VALUE, read("[1,2]", ArrayEnum.class));
        assertNull(read("null", ArrayValue.class));
    }

    @Test
    public void customNestedValueRetainsRuntimeElementTypes() {
        final PairValue value = PairValue.of(Pair.of(1, List.of("\u540D\uD83D\uDE80")));
        assertEquals(N.toJson(value), write(value));
        final List<?> json = (List<?>) N.fromJson(write(value), Object.class);
        assertEquals(1, json.get(0));
        assertEquals(List.of("\u540D\uD83D\uDE80"), json.get(1));
    }

    @Test
    public void customScalarValuesStayQuotedAndRejectMultipleRoots() {
        for (final String value : List.of("", "[1,2]", "\u540D\uD83D\uDE80\"\\\n")) {
            final String json = write(StringValue.of(value));
            assertEquals(value, ((StringValue) read(json, StringValue.class)).value());
            assertEquals(value, N.fromJson("[" + json + "]", new JsonDeserConfig().setElementType(String.class), Object[].class)[0]);
        }
        assertThrows(ParsingException.class, () -> read("[1,2] true", StringValue.class));
        assertThrows(ParsingException.class, () -> read("[1,2] [3]", ArrayValue.class));
        for (final Class<?> scalar : List.of(String.class, Boolean.class, Pattern.class)) {
            assertThrows(ParsingException.class, () -> read("[1,2]", scalar));
        }
    }

    @Test
    public void holdersPreserveContainedShapesAndNull() throws Exception {
        final List<String> values = List.of("\u540D", "");
        assertEquals(write(values), write(Holder.of(values)));
        assertEquals(values, ((Holder<?>) read(write(values), fieldType("holder"))).value());
        assertEquals("\"[1]\"", write(Holder.of("[1]")));
        assertEquals("null", write(Holder.of(null)));
        assertNull(((Holder<?>) read("null", fieldType("holder"))).value());
        assertNull(((Holder<?>) read("null", fieldType("nestedHolder"))).value());
        assertEquals("null", write(Holder.of(Optional.empty())));
        assertEquals("\"x\"", write(Optional.of(Holder.of("x"))));
    }

    @Test
    public void cyclicRootHoldersFailBeforeWriting() {
        final Holder<Object> holder = Holder.of(null);
        holder.setValue(Optional.of(holder));
        final var writer = new StringWriter();
        assertThrows(ParsingException.class, () -> converter.writeInternal(holder, null, writer));
        assertEquals("", writer.toString());
    }

    @Test
    public void structuredDirectHooksPreserveConfigurationAndOwnership() throws Exception {
        final var config = new JsonDeserConfig().setIgnoreNullOrEmpty(true).setElementType(String.class);
        final var custom = new JsonHttpMessageConverter(new JsonSerConfig(), config);
        final boolean[] closed = { false, false };
        final var reader = new StringReader("[1,2]") {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        final var writer = new StringWriter() {
            @Override
            public void close() {
                closed[1] = true;
            }
        };
        final Object value = custom.readInternal(IntList.class, reader);
        custom.writeInternal(value, null, writer);
        assertEquals(N.toJson(IntList.of(1, 2)), writer.toString());
        assertEquals(String.class, config.getElementType().javaType());
        assertEquals(true, config.isIgnoreNullOrEmpty());
        assertFalse(closed[0]);
        assertFalse(closed[1]);
    }

    public static final class ArrayValue {
        private final int[] values;

        private ArrayValue(final int[] values) {
            this.values = values;
        }

        @JsonXmlCreator
        public static ArrayValue of(final int[] values) {
            return new ArrayValue(values);
        }

        @JsonXmlValue
        public int[] values() {
            return values;
        }
    }

    public enum ArrayEnum {
        VALUE;

        @JsonXmlValue
        public int[] payload() {
            return new int[] { 1, 2 };
        }

        @JsonXmlCreator
        public static ArrayEnum of(final int[] values) {
            if (!Arrays.equals(new int[] { 1, 2 }, values)) {
                throw new IllegalArgumentException();
            }
            return VALUE;
        }
    }

    public static final class PairValue {
        private final Pair<Integer, List<String>> value;

        private PairValue(final Pair<Integer, List<String>> value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static PairValue of(final Pair<Integer, List<String>> value) {
            return new PairValue(value);
        }

        @JsonXmlValue
        public Pair<Integer, List<String>> value() {
            return value;
        }
    }

    public static final class StringValue {
        private final String value;

        private StringValue(final String value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static StringValue of(final String value) {
            return new StringValue(value);
        }

        @JsonXmlValue
        public String value() {
            return value;
        }
    }
}
