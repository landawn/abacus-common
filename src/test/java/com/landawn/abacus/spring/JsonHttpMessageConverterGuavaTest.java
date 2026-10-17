package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multimap;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u;

@Tag("unit")
class JsonHttpMessageConverterGuavaTest {
    private final JsonHttpMessageConverter converter = new JsonHttpMessageConverter();

    private ArrayListMultimap<String, Integer> multimap;
    private LinkedHashMultiset<Integer> multiset;
    private Optional<ArrayListMultimap<String, Integer>> optionalMultimap;
    private Optional<? extends ArrayListMultimap<String, Integer>> wildcardMultimap;
    private Optional<? extends LinkedHashMultiset<Integer>> wildcardMultiset;
    private Holder<? extends ArrayListMultimap<Long, List<Integer>>> wildcardHolder;
    private u.Optional<? extends ImmutableListMultimap<Long, List<Integer>>> wildcardAbacusMultimap;
    private u.Nullable<? extends ImmutableMultiset<Long>> wildcardNullableMultiset;
    private Optional<? extends Multimap<Long, List<Integer>>> wildcardInterfaceMultimap;

    @Test
    void readGuavaMultimapPreservesDeclaredValueType() throws Exception {
        final Object result = read("multimap", "{\"a\":[1,2,1]}");
        final ArrayListMultimap<?, ?> values = assertInstanceOf(ArrayListMultimap.class, result);
        assertEquals(List.of(1, 2, 1), values.asMap().get("a"));
    }

    @Test
    void readGuavaMultisetPreservesDeclaredElementTypeAndCounts() throws Exception {
        final Object result = read("multiset", "{\"2\":3,\"1\":1}");
        final LinkedHashMultiset<?> values = assertInstanceOf(LinkedHashMultiset.class, result);
        assertEquals(List.of(2, 2, 2, 1), List.copyOf(values));
    }

    @Test
    void readWrappedGuavaMultimapPreservesDeclaredValueType() throws Exception {
        final Optional<?> value = assertInstanceOf(Optional.class, read("optionalMultimap", "{\"a\":[1,2]}"));
        final ArrayListMultimap<?, ?> values = assertInstanceOf(ArrayListMultimap.class, value.orElseThrow());
        assertEquals(List.of(1, 2), values.asMap().get("a"));
    }

    @Test
    void readUpperBoundedMultimapPreservesDeclaredValueType() throws Exception {
        final Optional<?> value = assertInstanceOf(Optional.class, read("wildcardMultimap", "{\"a\":[1,2]}"));
        final ArrayListMultimap<?, ?> values = assertInstanceOf(ArrayListMultimap.class, value.orElseThrow());
        assertEquals(List.of(1, 2), values.asMap().get("a"));
    }

    @Test
    void readUpperBoundedMultisetPreservesCounts() throws Exception {
        final Optional<?> value = assertInstanceOf(Optional.class, read("wildcardMultiset", "{\"2\":3,\"1\":1}"));
        final LinkedHashMultiset<?> values = assertInstanceOf(LinkedHashMultiset.class, value.orElseThrow());
        assertEquals(List.of(2, 2, 2, 1), List.copyOf(values));
    }

    @ParameterizedTest
    @ValueSource(strings = { "wildcardHolder", "wildcardAbacusMultimap", "wildcardInterfaceMultimap" })
    void readUpperBoundedMultimapsRetainNestedArgumentsAcrossWrapperKinds(final String field) throws Exception {
        final Multimap<?, ?> values = assertInstanceOf(Multimap.class, unwrap(read(field, "{\"1\":[[2,3],[4]]}")));
        assertEquals(List.of(List.of(2, 3), List.of(4)), List.copyOf(values.asMap().get(1L)));
        assertEquals(1, values.keySet().size());
        if (field.equals("wildcardAbacusMultimap")) {
            assertInstanceOf(ImmutableListMultimap.class, values);
        }
    }

    @Test
    void readUpperBoundedNullableMultisetRetainsImmutableTypeAndCounts() throws Exception {
        final ImmutableMultiset<?> values = assertInstanceOf(ImmutableMultiset.class, unwrap(read("wildcardNullableMultiset", "{\"2\":3,\"1\":1}")));
        assertEquals(List.of(2L, 2L, 2L, 1L), List.copyOf(values));
    }

    @Test
    void readRootUpperBoundRetainsGenericKeyAndNestedValueTypes() throws Exception {
        final ParameterizedType holder = (ParameterizedType) getClass().getDeclaredField("wildcardHolder").getGenericType();
        final Type bound = holder.getActualTypeArguments()[0];
        final ArrayListMultimap<?, ?> values = assertInstanceOf(ArrayListMultimap.class, converter.readInternal(bound, new StringReader("{\"1\":[[2,3]]}")));
        assertEquals(List.of(List.of(2, 3)), values.asMap().get(1L));
    }

    @ParameterizedTest
    @ValueSource(strings = { "wildcardMultimap", "wildcardMultiset", "wildcardHolder", "wildcardAbacusMultimap", "wildcardNullableMultiset",
            "wildcardInterfaceMultimap" })
    void upperBoundedWrappersDistinguishNullAndEmptyContainers(final String field) throws Exception {
        assertNull(unwrap(read(field, " \t null\r\n")));
        assertEquals("null", write(read(field, "null")));
        final Object empty = unwrap(read(field, "{}"));
        assertEquals(Map.of(), N.fromJson(write(empty), Map.class));
        assertEquals(Map.of(), N.fromJson(write(read(field, "{}")), Map.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "multimap", "multiset", "wildcardMultimap", "wildcardMultiset", "wildcardHolder", "wildcardAbacusMultimap",
            "wildcardNullableMultiset", "wildcardInterfaceMultimap" })
    void structuredGuavaTargetsRejectMalformedJsonAndMultipleRoots(final String field) {
        for (final String json : List.of("", " \t", "{", "{} {}", "null []", "{} trailing", "{\"a\":[1],}")) {
            assertThrows(ParsingException.class, () -> read(field, json), field + ": " + json);
        }
    }

    @Test
    void structuredGuavaReadFailureKeepsCauseAndCallerReaderOpen() throws Exception {
        final IOException cause = new IOException("structured read failure");
        final boolean[] closed = { false };
        final Reader reader = new Reader() {
            private boolean first = true;

            @Override
            public int read(final char[] buffer, final int offset, final int length) throws IOException {
                if (length == 0) {
                    return 0;
                }
                if (first) {
                    first = false;
                    buffer[offset] = '{';
                    return 1;
                }
                throw cause;
            }

            @Override
            public void close() {
                closed[0] = true;
            }
        };
        final Type type = getClass().getDeclaredField("wildcardMultimap").getGenericType();
        final UncheckedIOException error = assertThrows(UncheckedIOException.class, () -> converter.readInternal(type, reader));
        assertSame(cause, error.getCause());
        assertFalse(closed[0]);
    }

    @Test
    void readGuavaContainersRejectsTrailingRoot() {
        assertThrows(ParsingException.class, () -> read("multimap", "{\"a\":[1]} {}"));
        assertThrows(ParsingException.class, () -> read("multiset", "{\"1\":2} []"));
    }

    @Test
    void writeGuavaMultimapEmitsJsonObject() {
        final ArrayListMultimap<String, Integer> values = ArrayListMultimap.create();
        values.putAll("a", List.of(1, 2, 1));
        assertEquals(Map.of("a", List.of(1, 2, 1)), N.fromJson(write(values), Map.class));
    }

    @Test
    void writeGuavaMultisetEmitsCountsAsJsonObject() {
        final LinkedHashMultiset<String> values = LinkedHashMultiset.create();
        values.add("a", 3);
        values.add("b", 1);
        assertEquals(Map.of("a", 3, "b", 1), N.fromJson(write(values), Map.class));
    }

    @Test
    void writeWrappedGuavaMultimapEmitsJsonObject() {
        final ArrayListMultimap<String, Integer> values = ArrayListMultimap.create();
        values.put("a", 1);
        assertEquals(Map.of("a", List.of(1)), N.fromJson(write(Optional.of(values)), Map.class));
    }

    private static Object unwrap(final Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        } else if (value instanceof u.Optional<?> optional) {
            return optional.orElse(null);
        } else if (value instanceof u.Nullable<?> nullable) {
            return nullable.orElse(null);
        }
        return assertInstanceOf(Holder.class, value).value();
    }

    private Object read(final String field, final String json) throws Exception {
        final Type type = getClass().getDeclaredField(field).getGenericType();
        return converter.readInternal(type, new StringReader(json));
    }

    private String write(final Object value) {
        final StringWriter output = new StringWriter();
        converter.writeInternal(value, value.getClass(), output);
        return output.toString();
    }
}
