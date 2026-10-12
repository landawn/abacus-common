package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.ShortList;

@Tag("unit")
class PrimitiveListWhitespaceRegression20261005Test extends TestBase {
    private final JsonParser parser = ParserFactory.createJsonParser();

    static Stream<Class<?>> primitiveLists() {
        return Stream.of(BooleanList.class, CharList.class, ByteList.class, ShortList.class, IntList.class, LongList.class, FloatList.class, DoubleList.class);
    }

    @ParameterizedTest
    @MethodSource("primitiveLists")
    void whollyBlankRootsAndContainersKeepTheirDefaults(final Class<?> listClass) {
        final Type<?> type = Type.of(listClass);
        for (final boolean reader : new boolean[] { false, true }) {
            for (final String blank : new String[] { "", " ", "\u1680", "\u2002", "\u2003", "\u3000", "\u2003\u2003", " \t\u2003\u3000\n" }) {
                assertNull(parse(blank, type, reader), listClass + " root " + blank);
                assertEquals(type.valueOf("[]"), parse("[" + blank + "]", type, reader), listClass + " bracketed " + blank);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("primitiveLists")
    void nestedBlankContainersKeepTheirDefaults(final Class<?> listClass) {
        final Type<ListBean<?>> beanType = Type.of(ListBean.class.getName() + "<" + listClass.getName() + ">");
        for (final boolean reader : new boolean[] { false, true }) {
            for (final String blank : new String[] { "\u2003", "\u3000", "\u2003\u2003" }) {
                final ListBean<?> bean = (ListBean<?>) parse("{\"ids\":[" + blank + "]}", beanType, reader);
                assertEquals(Type.of(listClass).valueOf("[]"), bean.getIds());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("primitiveLists")
    void missingElementsKeepTheirOriginalPositionsAndPrecedence(final Class<?> listClass) {
        final Type<?> type = Type.of(listClass);
        final String[] inputs = { "[1,\u2003]", "[1, ]", "[,1]", "[,,]", "[1,,\u3000]", "[\"1\",\u2003]", "[\"1\",2,\u3000]", "[\"invalid\",\u2003,]",
                "[\"\",\u2003]", "1,\u2003", "\u2003,1" };
        final int[] indices = { 1, 1, 0, 0, 1, 1, 2, 1, 1, 1, 0 };
        for (final boolean reader : new boolean[] { false, true }) {
            for (int i = 0; i < inputs.length; i++) {
                final String input = inputs[i];
                final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse(input, type, reader), listClass + " " + input);
                assertEquals("Missing array element at index " + indices[i], error.getMessage(), listClass + " " + input);
            }
            final Type<ListBean<?>> beanType = Type.of(ListBean.class.getName() + "<" + listClass.getName() + ">");
            final IllegalArgumentException nested = assertThrows(IllegalArgumentException.class, () -> parse("{\"ids\":[\"1\",\u2003]}", beanType, reader));
            assertEquals("Missing array element at index 1", nested.getMessage());
        }
    }

    @ParameterizedTest
    @MethodSource("primitiveLists")
    void grammarErrorsStillPrecedeMissingElementsAndConversionFailures(final Class<?> listClass) {
        final Type<?> type = Type.of(listClass);
        for (final boolean reader : new boolean[] { false, true }) {
            for (final String input : new String[] { "[\"invalid\",,[]]", "[\"invalid\",,{}]", "[\"invalid\",,", "[,\"invalid\" 1]" }) {
                assertThrows(ParsingException.class, () -> parse(input, type, reader), listClass + " " + input);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("primitiveLists")
    void quotedBlanksStillRepresentElements(final Class<?> listClass) {
        final Type<?> type = Type.of(listClass);
        for (final String input : new String[] { "[\"\"]", "[\"\u2003\"]", "['\u3000']", "[\"\u2003\",1]" }) {
            Object expected = null;
            RuntimeException failure = null;
            try {
                expected = type.valueOf(input);
            } catch (final RuntimeException e) {
                failure = e;
            }
            for (final boolean reader : new boolean[] { false, true }) {
                if (failure == null) {
                    assertEquals(expected, parse(input, type, reader), listClass + " " + input);
                } else {
                    final RuntimeException actual = assertThrows(RuntimeException.class, () -> parse(input, type, reader));
                    assertEquals(failure.getClass(), actual.getClass(), listClass + " " + input);
                }
            }
        }
    }

    private Object parse(final String input, final Type<?> type, final boolean reader) {
        return reader ? parser.deserialize(new StringReader(input), type) : parser.deserialize(input, type);
    }

    public static class ListBean<T> {
        private T ids;

        public T getIds() {
            return ids;
        }

        public void setIds(final T ids) {
            this.ids = ids;
        }
    }
}
