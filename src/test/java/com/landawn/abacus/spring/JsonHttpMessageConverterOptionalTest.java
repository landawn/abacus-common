package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.u.Nullable;

public class JsonHttpMessageConverterOptionalTest extends TestBase {
    enum Wrapper {
        JDK(Optional.class), ABACUS(com.landawn.abacus.util.u.Optional.class), NULLABLE(Nullable.class);

        final Class<?> raw;

        Wrapper(final Class<?> raw) {
            this.raw = raw;
        }

        Type type(final Type element) {
            return new GenericType(raw, element);
        }

        Object of(final Object value) {
            return switch (this) {
                case JDK -> Optional.ofNullable(value);
                case ABACUS -> com.landawn.abacus.util.u.Optional.ofNullable(value);
                case NULLABLE -> value == null ? Nullable.empty() : Nullable.of(value);
            };
        }

        Object get(final Object value) {
            return switch (this) {
                case JDK -> ((Optional<?>) value).orElse(null);
                case ABACUS -> ((com.landawn.abacus.util.u.Optional<?>) value).orElse(null);
                case NULLABLE -> ((Nullable<?>) value).orElse(null);
            };
        }
    }

    private record GenericType(Class<?> raw, Type element) implements ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { element };
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return raw.getDeclaringClass();
        }
    }

    private final JsonHttpMessageConverter converter = new JsonHttpMessageConverter();

    private Object read(final String json, final Type type) {
        return converter.readInternal(type, new StringReader(json));
    }

    private String write(final Object value) {
        final var writer = new StringWriter();
        converter.writeInternal(value, null, writer);
        return writer.toString();
    }

    @ParameterizedTest
    @EnumSource(Wrapper.class)
    public void scalarRuntimeTypesAndUnicodeRoundTrip(final Wrapper wrapper) {
        for (final Object value : List.of("", "null", "123", "\u540D\uD83D\uDE80\"\\\n", true, Long.MAX_VALUE, -0.0d)) {
            final String json = write(wrapper.of(value));
            assertEquals(write(value), json);
            assertEquals(wrapper.of(value), read(json, wrapper.type(value.getClass())));
        }
        assertEquals(wrapper.of("123"), read("\"123\"", wrapper.type(Object.class)));
        assertInstanceOf(Number.class, wrapper.get(read("123", wrapper.type(Object.class))));
        assertEquals(wrapper.of(false), read("false", wrapper.raw));
    }

    @ParameterizedTest
    @EnumSource(Wrapper.class)
    public void structuredRootsPreserveShapeAndGenericElements(final Wrapper wrapper) throws Exception {
        final List<String> values = List.of("\u540D\uD83D\uDE80", "");
        final Type listType = new GenericType(List.class, String.class);
        assertEquals(write(values), write(wrapper.of(values)));
        assertEquals(wrapper.of(values), read(write(values), wrapper.type(listType)));
        final Map<String, List<Integer>> map = Map.of("key", List.of(1, 2));
        final Type mapType = getClass().getDeclaredField("genericMap").getGenericType();
        assertEquals(write(map), write(wrapper.of(map)));
        assertEquals(wrapper.of(map), read(write(map), wrapper.type(mapType)));
        assertEquals(wrapper.of(Map.of()), read("{}", wrapper.type(Object.class)));
        assertEquals(wrapper.of(List.of()), read("[]", wrapper.type(listType)));
        assertEquals(write(new int[] { 1, 2 }), write(wrapper.of(new int[] { 1, 2 })));
        assertArrayEquals(new int[] { 1, 2 }, (int[]) wrapper.get(read("[1,2]", wrapper.type(int[].class))));
    }

    private Map<String, List<Integer>> genericMap;

    @ParameterizedTest
    @EnumSource(Wrapper.class)
    public void nullKeepsTheOuterWrapperEmpty(final Wrapper wrapper) {
        assertEquals("null", write(wrapper.of(null)));
        assertEquals("null", write(Nullable.of(null)));
        for (final Type element : List.of(String.class, Object.class, new GenericType(List.class, String.class), wrapper.type(String.class))) {
            assertEquals(wrapper.of(null), read(" \t null\r\n", wrapper.type(element)));
        }
        assertEquals(Nullable.empty(), read("null", new GenericType(Nullable.class, String.class)));
    }

    @ParameterizedTest
    @EnumSource(Wrapper.class)
    public void nestedWrappersPreservePayloads(final Wrapper wrapper) {
        final var value = wrapper.of(Optional.of(List.of("\u540D")));
        final Type type = wrapper.type(new GenericType(Optional.class, new GenericType(List.class, String.class)));
        assertEquals("[\"\u540D\"]", write(value));
        assertEquals(value, read(write(value), type));
        assertEquals("null", write(wrapper.of(Optional.empty())));
        assertEquals(wrapper.of(null), read("null", type));
    }

    @ParameterizedTest
    @EnumSource(Wrapper.class)
    public void callerConfigurationAndNullSemanticsArePreserved(final Wrapper wrapper) {
        final var config = new JsonDeserConfig().setIgnoreNullOrEmpty(true).setReadNullToEmpty(true).setElementType(String.class);
        final var custom = new JsonHttpMessageConverter(new JsonSerConfig(), config);
        assertEquals(wrapper.of(null), custom.readInternal(wrapper.type(String.class), new StringReader("null")));
        assertEquals(wrapper.of(""), custom.readInternal(wrapper.type(String.class), new StringReader("\"\"")));
        assertEquals(wrapper.of(List.of("1")), custom.readInternal(wrapper.type(Object.class), new StringReader("[null,1]")));
        assertTrue(config.isIgnoreNullOrEmpty());
        assertTrue(config.isReadNullToEmpty());
        assertEquals(String.class, config.getElementType().javaType());
    }

    @ParameterizedTest
    @EnumSource(Wrapper.class)
    public void malformedAndMismatchedRootsAreRejected(final Wrapper wrapper) {
        for (final String json : List.of("", " \t", "nil", "null true", "\"bad\\q\"", "1 2", "NaN", "[]", "{}")) {
            assertThrows(ParsingException.class, () -> read(json, wrapper.type(String.class)), json);
        }
        assertThrows(ParsingException.class, () -> read("\"hello\"", wrapper.type(new GenericType(List.class, String.class))));
        final var writer = new StringWriter();
        assertThrows(ParsingException.class, () -> converter.writeInternal(wrapper.of(Double.NaN), null, writer));
        assertEquals("", writer.toString());
    }

    @ParameterizedTest
    @EnumSource(Wrapper.class)
    public void wrapperHooksLeaveResourcesOpen(final Wrapper wrapper) {
        final boolean[] closed = { false, false };
        final var reader = new StringReader("[\"x\"]") {
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
        final Object value = converter.readInternal(wrapper.type(new GenericType(List.class, String.class)), reader);
        converter.writeInternal(value, null, writer);
        assertEquals("[\"x\"]", writer.toString());
        assertFalse(closed[0]);
        assertFalse(closed[1]);
    }

    @Test
    public void primitiveOptionalsRetainTheirScalarHandlers() {
        assertEquals("2", write(java.util.OptionalInt.of(2)));
        assertEquals("null", write(java.util.OptionalInt.empty()));
        assertEquals(java.util.OptionalLong.of(Long.MAX_VALUE), read(Long.toString(Long.MAX_VALUE), java.util.OptionalLong.class));
        assertEquals(com.landawn.abacus.util.u.OptionalInt.of(2), read("2", com.landawn.abacus.util.u.OptionalInt.class));
        assertEquals("2", write(com.landawn.abacus.util.u.OptionalInt.of(2)));
    }
}
