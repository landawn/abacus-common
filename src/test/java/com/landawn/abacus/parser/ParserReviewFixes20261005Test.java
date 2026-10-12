package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.esotericsoftware.kryo.KryoException;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.AbstractPrimitiveListType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.PrimitiveList;
import com.landawn.abacus.util.ShortList;

/**
 * Code review 2026-10-05 of the parser memory work.
 *
 * <ul>
 * <li>{@code KryoParser.read(InputStream, ...)}: when decoding failed and the drain of the remaining input then failed
 * too, the drain's {@code UncheckedIOException} thrown from {@code finally} replaced the decoding failure without a
 * trace. The thrown type is unchanged (a source read failure stays an {@code UncheckedIOException}, as when the whole
 * stream was read before decoding), but the decoding failure is now attached as a suppressed exception.</li>
 * <li>{@code JsonParserImpl.readArray}: whether a primitive array was collected in primitive storage depended on how much
 * input remained after the array ({@code >= 256} characters), so every small array of a larger document paid for a
 * primitive buffer. The switch now happens when the array itself reaches 256 elements; the tests pin that the
 * result never depends on the array length, its position, the trailing input, the source kind or the configuration.</li>
 * <li>{@code readPrimitiveListDirect} re-implements the delimiter grammar of {@code readPrimitiveList}, which still serves
 * custom primitive-list types. A differential test keeps both grammars producing the same results and errors.</li>
 * </ul>
 */
@Tag("unit")
class ParserReviewFixes20261005Test extends TestBase {
    private static final JsonParser jsonParser = ParserFactory.createJsonParser();

    // ---------------------------------------------------------------- Kryo: drain failure must not hide the decoding failure

    /** Serves {@code payload}, then either ends or fails every further read, like a connection reset after the payload. */
    private static InputStream payloadThen(final byte[] payload, final boolean failAfterPayload) {
        return new InputStream() {
            private int position;

            @Override
            public int read() throws IOException {
                final byte[] one = new byte[1];
                final int n = read(one, 0, 1);
                return n < 0 ? -1 : one[0] & 0xFF;
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                if (len == 0) {
                    return 0;
                }

                if (position < payload.length) {
                    final int n = Math.min(len, payload.length - position);
                    System.arraycopy(payload, position, b, off, n);
                    position += n;
                    return n;
                }

                if (failAfterPayload) {
                    throw new IOException("connection reset after payload");
                }

                return -1;
            }
        };
    }

    // Varint class ID 1_000_000_000, never registered: Kryo rejects it from these five bytes without requesting more input.
    // (Avoid ID -1: a fresh class resolver's memoized-ID sentinel is -1, so -1 decodes as null on the first call.)
    private static final byte[] UNREGISTERED_CLASS_ID = { (byte) 0x80, (byte) 0x94, (byte) 0xEB, (byte) 0xDC, 0x03 };

    @Test
    void kryoDrainFailureKeepsTheDecodingFailureAsSuppressed() {
        final KryoParser parser = ParserFactory.createKryoParser();

        final UncheckedIOException e = assertThrows(UncheckedIOException.class,
                () -> parser.deserialize(payloadThen(UNREGISTERED_CLASS_ID, true), null, (Class<Object>) null));

        assertInstanceOf(IOException.class, e.getCause());
        assertEquals("connection reset after payload", e.getCause().getMessage());
        assertEquals(1, e.getSuppressed().length, "the decoding failure must travel with the drain failure");
        assertInstanceOf(KryoException.class, e.getSuppressed()[0]);
        assertTrue(e.getSuppressed()[0].getMessage().contains("unregistered class ID"), e.getSuppressed()[0].getMessage());
    }

    @Test
    void kryoDecodingFailureWithoutDrainFailureIsUnchanged() {
        final KryoParser parser = ParserFactory.createKryoParser();

        final KryoException e = assertThrows(KryoException.class,
                () -> parser.deserialize(payloadThen(UNREGISTERED_CLASS_ID, false), null, (Class<Object>) null));

        assertTrue(e.getMessage().contains("unregistered class ID"), e.getMessage());
        assertEquals(0, e.getSuppressed().length);
    }

    @Test
    void kryoDrainFailureAfterSuccessfulDecodingStillFailsAlone() {
        final KryoParser parser = ParserFactory.createKryoParser();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        parser.serialize(new ArrayList<>(List.of("a", "b")), null, bytes);
        final byte[] payload = bytes.toByteArray();

        // serialize(Object, config, OutputStream) writes object-only data, so the target class selects the format.
        assertEquals(List.of("a", "b"), parser.deserialize(payloadThen(payload, false), null, ArrayList.class));

        // The complete-consumption contract: an unreadable remainder fails the call even though decoding succeeded.
        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> parser.deserialize(payloadThen(payload, true), null, ArrayList.class));

        assertEquals("connection reset after payload", e.getCause().getMessage());
        assertEquals(0, e.getSuppressed().length, "nothing failed before the drain");

        // Readable trailing bytes are consumed and ignored.
        final byte[] withTrailing = Arrays.copyOf(payload, payload.length + 3);
        assertEquals(List.of("a", "b"), parser.deserialize(payloadThen(withTrailing, false), null, ArrayList.class));
    }

    @Test
    void kryoReadFailureDuringDecodingIsStillTheThrownFailure() {
        final KryoParser parser = ParserFactory.createKryoParser();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        parser.serialize(new ArrayList<>(List.of("a", "b")), null, bytes);
        final byte[] truncated = Arrays.copyOf(bytes.toByteArray(), bytes.size() - 1);

        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> parser.deserialize(payloadThen(truncated, true), null, ArrayList.class));

        assertEquals("connection reset after payload", e.getCause().getMessage());
    }

    // ---------------------------------------------------------------- JSON: primitive storage switch depends on the array only

    private static final int[] ARRAY_SIZES = { 0, 1, 2, 255, 256, 257, 258, 511, 512, 513, 1_000 };

    // Elements that read as the element type's default value; placed on both sides of the switch point.
    private static final int[] DEFAULT_VALUE_POSITIONS = { 0, 254, 255, 256, 257, 700 };

    static Stream<Class<?>> primitiveArrayClasses() {
        return Stream.of(boolean[].class, char[].class, byte[].class, short[].class, int[].class, long[].class, float[].class, double[].class);
    }

    private static boolean isDefaultValuePosition(final int index) {
        return Arrays.stream(DEFAULT_VALUE_POSITIONS).anyMatch(p -> p == index);
    }

    private static String token(final Class<?> arrayClass, final int index) {
        if (isDefaultValuePosition(index)) {
            return index % 2 == 0 ? "null" : "\"\"";
        }

        if (arrayClass == boolean[].class) {
            return index % 3 == 0 ? "true" : "false";
        } else if (arrayClass == char[].class) {
            return "\"" + (char) ('a' + index % 26) + "\"";
        } else if (arrayClass == float[].class || arrayClass == double[].class) {
            return (index % 100) + ".5";
        } else {
            return String.valueOf(index % 100 - 50);
        }
    }

    private static Object expectedArray(final Class<?> arrayClass, final int size) {
        final Object result = Array.newInstance(arrayClass.getComponentType(), size);

        for (int i = 0; i < size; i++) {
            if (isDefaultValuePosition(i)) {
                continue;
            }

            if (arrayClass == boolean[].class) {
                Array.setBoolean(result, i, i % 3 == 0);
            } else if (arrayClass == char[].class) {
                Array.setChar(result, i, (char) ('a' + i % 26));
            } else if (arrayClass == float[].class) {
                Array.setFloat(result, i, i % 100 + 0.5f);
            } else if (arrayClass == double[].class) {
                Array.setDouble(result, i, i % 100 + 0.5d);
            } else if (arrayClass == byte[].class) {
                Array.setByte(result, i, (byte) (i % 100 - 50));
            } else if (arrayClass == short[].class) {
                Array.setShort(result, i, (short) (i % 100 - 50));
            } else if (arrayClass == int[].class) {
                Array.setInt(result, i, i % 100 - 50);
            } else {
                Array.setLong(result, i, i % 100 - 50);
            }
        }

        return result;
    }

    private static String jsonArray(final Class<?> arrayClass, final int size, final int failingIndex, final String failingToken) {
        final StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(i % 7 == 0 ? " , " : ",");
            }

            sb.append(i == failingIndex ? failingToken : token(arrayClass, i));
        }

        return sb.append(']').toString();
    }

    private static void assertSameArray(final Object expected, final Object actual, final String message) {
        assertEquals(expected.getClass(), actual.getClass(), message);
        assertTrue(Objects.deepEquals(expected, actual),
                () -> message + ": expected " + Arrays.deepToString(new Object[] { expected }) + " but was " + Arrays.deepToString(new Object[] { actual }));
    }

    @ParameterizedTest
    @MethodSource("primitiveArrayClasses")
    void primitiveArraysParseIdenticallyBelowAtAndAboveTheSwitchPoint(final Class<?> arrayClass) {
        final JsonDeserConfig ignoreNullOrEmpty = JsonDeserConfig.create().setIgnoreNullOrEmpty(true);
        final JsonDeserConfig readNullToEmpty = JsonDeserConfig.create().setReadNullToEmpty(true);
        final String trailingInput = " ".repeat(600);

        for (final int size : ARRAY_SIZES) {
            final String json = jsonArray(arrayClass, size, -1, null);
            final Object expected = expectedArray(arrayClass, size);
            final String message = arrayClass.getSimpleName() + " size " + size;

            assertSameArray(expected, jsonParser.deserialize(json, arrayClass), message + " from String");
            assertSameArray(expected, jsonParser.deserialize(new StringReader(json), arrayClass), message + " from Reader");
            assertSameArray(expected, jsonParser.deserialize(json + trailingInput, arrayClass), message + " with trailing input");
            // Primitive defaults are values, not null or empty elements: both configurations keep them.
            assertSameArray(expected, jsonParser.deserialize(json, ignoreNullOrEmpty, arrayClass), message + " ignoreNullOrEmpty");
            assertSameArray(expected, jsonParser.deserialize(new StringReader(json), ignoreNullOrEmpty, arrayClass), message + " Reader ignoreNullOrEmpty");
            assertSameArray(expected, jsonParser.deserialize(json, readNullToEmpty, arrayClass), message + " readNullToEmpty");
        }
    }

    @ParameterizedTest
    @MethodSource("primitiveArrayClasses")
    void conversionFailuresAreTheSameOnBothSidesOfTheSwitchPoint(final Class<?> arrayClass) {
        // A boolean rejects every token that is not boolean text, such as 2 (code review 2026-10-05).
        final String invalid = arrayClass == boolean[].class ? "2" : "{\"a\":1}";

        for (final int failingIndex : new int[] { 1, 255, 256, 257, 900 }) {
            final String json = jsonArray(arrayClass, 1_000, failingIndex, invalid);
            final RuntimeException fromString = assertThrows(RuntimeException.class, () -> jsonParser.deserialize(json, arrayClass));
            final RuntimeException fromReader = assertThrows(RuntimeException.class, () -> jsonParser.deserialize(new StringReader(json), arrayClass));
            final RuntimeException small = assertThrows(RuntimeException.class, () -> jsonParser.deserialize(jsonArray(arrayClass, 3, 1, invalid), arrayClass));

            assertEquals(small.getClass(), fromString.getClass(), arrayClass.getSimpleName() + " index " + failingIndex);
            assertEquals(small.getMessage(), fromString.getMessage(), arrayClass.getSimpleName() + " index " + failingIndex);
            assertEquals(small.getClass(), fromReader.getClass(), arrayClass.getSimpleName() + " index " + failingIndex);
            assertEquals(small.getMessage(), fromReader.getMessage(), arrayClass.getSimpleName() + " index " + failingIndex);
        }
    }

    @Test
    void arrayResultDoesNotDependOnItsPositionOrTheFollowingInput() {
        final StringBuilder padding = new StringBuilder();

        for (int i = 0; i < 400; i++) {
            padding.append("\"p").append(i).append("\":").append(i).append(',');
        }

        for (final int size : ARRAY_SIZES) {
            final String ids = jsonArray(int[].class, size, -1, null);
            final Object expected = expectedArray(int[].class, size);

            final ArrayBean last = jsonParser.deserialize("{\"name\":\"n\",\"ids\":" + ids + "}", ArrayBean.class);
            final ArrayBean first = jsonParser.deserialize("{\"ids\":" + ids + ",\"name\":\"n\"}", ArrayBean.class);
            final ArrayBean padded = jsonParser.deserialize("{\"ids\":" + ids + "," + padding + "\"name\":\"n\"}",
                    JsonDeserConfig.create().setIgnoreUnmatchedProperty(true), ArrayBean.class);
            final ArrayBean fromReader = jsonParser.deserialize(new StringReader("{\"ids\":" + ids + "," + padding + "\"name\":\"n\"}"),
                    JsonDeserConfig.create().setIgnoreUnmatchedProperty(true), ArrayBean.class);

            for (final ArrayBean bean : new ArrayBean[] { last, first, padded, fromReader }) {
                assertSameArray(expected, bean.getIds(), "ids of size " + size);
                assertEquals("n", bean.getName());
            }
        }
    }

    @Test
    void referenceArraysKeepTheirElementsAcrossTheSwitchPoint() {
        for (final int size : ARRAY_SIZES) {
            final StringBuilder json = new StringBuilder("[");
            final Integer[] expectedIntegers = new Integer[size];
            final String[] expectedStrings = new String[size];

            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    json.append(',');
                }

                if (i % 50 == 0) {
                    json.append("null");
                } else {
                    json.append(i);
                    expectedIntegers[i] = i;
                    expectedStrings[i] = String.valueOf(i);
                }
            }

            json.append(']');

            assertArrayEquals(expectedIntegers, jsonParser.deserialize(json.toString(), Integer[].class), "Integer[] size " + size);
            assertArrayEquals(expectedStrings, jsonParser.deserialize(json.toString(), String[].class), "String[] size " + size);
            assertArrayEquals(expectedIntegers, jsonParser.deserialize(new StringReader(json.toString()), Integer[].class), "Reader size " + size);
        }
    }

    @Test
    void unbracketedRootValuesStillBecomeOneElementArrays() {
        assertArrayEquals(new int[] { 5 }, jsonParser.deserialize("5", int[].class));
        assertArrayEquals(new int[] { 5 }, jsonParser.deserialize("5" + " ".repeat(600), int[].class));
        assertArrayEquals(new double[] { 1.5 }, jsonParser.deserialize(new StringReader("1.5"), double[].class));
        assertArrayEquals(new int[] { 1, 2, 3 }, jsonParser.deserialize("1,2,3", int[].class));
    }

    public static class ArrayBean {
        private String name;
        private int[] ids;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int[] getIds() {
            return ids;
        }

        public void setIds(final int[] ids) {
            this.ids = ids;
        }
    }

    // ---------------------------------------------------------------- primitive lists: direct grammar == custom-type grammar

    /**
     * A primitive-list type that is not one of the built-in classes, so the parser reads it through the generic
     * {@code readPrimitiveList} grammar and the type's {@code valueOf}, while the built-in type it delegates to is read by
     * {@code readPrimitiveListDirect}. Both must accept and reject exactly the same inputs.
     */
    private static final class DelegatingPrimitiveListType<T extends PrimitiveList<?, ?, ?>> extends AbstractPrimitiveListType<T> {
        private final Type<T> delegate;

        DelegatingPrimitiveListType(final Type<T> delegate) {
            super("ReviewDelegating" + delegate.name());
            this.delegate = delegate;
        }

        @Override
        public Class<T> javaType() {
            return delegate.javaType();
        }

        @Override
        public Type<?> elementType() {
            return delegate.elementType();
        }

        @Override
        public Type<?> arrayType() {
            return ((AbstractPrimitiveListType<?>) delegate).arrayType();
        }

        @Override
        public List<Type<?>> parameterTypes() {
            return delegate.parameterTypes();
        }

        @Override
        public String stringOf(final T x) {
            return delegate.stringOf(x);
        }

        @Override
        public T valueOf(final String str) {
            return delegate.valueOf(str);
        }
    }

    static Stream<Class<?>> primitiveListClasses() {
        return Stream.of(BooleanList.class, CharList.class, ByteList.class, ShortList.class, IntList.class, LongList.class, FloatList.class, DoubleList.class);
    }

    private static final String[] PRIMITIVE_LIST_INPUTS = { "[]", "[1]", "[1,2,3]", " [ 1 , 2 ] ", "1,2,3", "1", "", " ", "\u2003", "[ ]", "[\u3000]", "[,]",
            "[1,]", "[,1]", "[1,,2]", "[1, ]", "[1,\u2003]", "[\"1\",\"2\"]", "['1']", "['1','2']", "[null]", "[null,1]", "[\"\"]", "[1 2]", "[\"1\" 2]",
            "[\"1\",2,\u3000]", "[[1]]", "[{}]", "[1]]", "[[1]", "[1", "1]", "[true,false]", "[\"true\"]", "[1.5]", "[\"a\"]", "[a]", "[abc,def]", "[-0]",
            "[0x10]", "[1e3]", "[ \"x\" ]", "[\"a,b\"]", "[\"[\"]", "[\"invalid\",,[]]", "[\"invalid\",,", "[,\"invalid\" 1]", "[127,128]", "[-129]", "[65535]",
            "[\"\\u0041\"]", "['\\'']", "[\"\\\"\"]", "[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]",
            // 2026-10-05: the array codecs read quoted content decoded once and a bare null as the element default
            "[\" 1 \"]", "[\"\\u0031\"]", "[\"\\t1\"]", "[null,null]", "[\"1\",null]", "[\"-1\"]", "[\"65\"]", "['']", "[\"null\"]", "[\"\\\\u0031\"]" };

    private static String outcome(final Supplier<Object> parse) {
        try {
            final Object result = parse.get();
            return "value " + (result == null ? "null" : result.getClass().getSimpleName() + result);
        } catch (final RuntimeException e) {
            return "error " + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    @ParameterizedTest
    @MethodSource("primitiveListClasses")
    void directPrimitiveListGrammarMatchesTheCustomTypeGrammar(final Class<? extends PrimitiveList<?, ?, ?>> listClass) {
        final Type<PrimitiveList<?, ?, ?>> builtIn = Type.of(listClass);
        final DelegatingPrimitiveListType<PrimitiveList<?, ?, ?>> custom = new DelegatingPrimitiveListType<>(builtIn);

        assertSame(listClass, custom.javaType());

        for (final String input : PRIMITIVE_LIST_INPUTS) {
            final String message = listClass.getSimpleName() + " " + input;

            assertEquals(outcome(() -> jsonParser.deserialize(input, custom)), outcome(() -> jsonParser.deserialize(input, builtIn)), message);
            assertEquals(outcome(() -> jsonParser.deserialize(new StringReader(input), custom)),
                    outcome(() -> jsonParser.deserialize(new StringReader(input), builtIn)), message + " from Reader");
        }
    }

    @Test
    void customPrimitiveListTypeReallyTakesTheGenericPath() {
        // The delegating type must not be mistaken for a built-in type, or the differential test above compares a path with itself.
        assertNull(PrimitiveJsonBuffer.createList(new DelegatingPrimitiveListType<>(Type.of(IntList.class))));
        assertInstanceOf(PrimitiveJsonBuffer.class, PrimitiveJsonBuffer.createList(Type.of(IntList.class)));
    }
}
