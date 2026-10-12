package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.Maps;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.function.CharToBooleanFunction;

/**
 * Code review 2026-10-05: one simple rule for converting a value to a boolean, everywhere.
 * <ul>
 *   <li>A number: only an {@code Integer}, {@code Short} or {@code Byte} is read - {@code 1} is {@code true}, {@code 0}
 *       and {@code -1} are {@code false}, and any other value is rejected with an {@link IllegalArgumentException}. A
 *       {@code Number} of any other class ({@code 1L}, {@code 1.0}, {@code BigDecimal}, {@code AtomicInteger}) is
 *       rejected whatever its value.</li>
 *   <li>Text or a character: {@code "1"}, {@code "y"}, {@code "Y"} and {@code "true"} (any case) are {@code true};
 *       {@code "0"}, {@code "-1"}, {@code "n"}, {@code "N"} and {@code "false"} (any case) are {@code false}; any other
 *       text is rejected. Text is never parsed as a number, so {@code "2"}, {@code "1.0"} and {@code "01"} are rejected.
 *       Surrounding padding is removed first, and blank text keeps meaning "absent" (the default value).</li>
 * </ul>
 * Before, unrecognized text ({@code "yes"}, {@code "abc"}) silently read as {@code false}, numeric text was read by value,
 * and numbers followed three different rules.
 */
@Tag("unit")
class BooleanConversionRule20261005Test extends TestBase {
    private static final JsonParser jsonParser = ParserFactory.createJsonParser();

    private static final String NUMBER_RULE = "only 1 (true), 0 (false) and -1 (false) are accepted";

    private static final String NUMBER_TYPE_RULE = "only Integer, Short and Byte values 1 (true), 0 (false) and -1 (false) are accepted";

    private static final char BACKSLASH = (char) 92;

    private static final String TEXT_RULE = "only 1, y, Y and true (any case) are true, and only 0, -1, n, N and false (any case) are false";

    private static final String[] TRUE_TEXT = { "1", "y", "Y", "true", "TRUE", "True", "tRuE", " true ", "\u3000Y", "\u00011\u0001", "\ttrue\n" };

    private static final String[] FALSE_TEXT = { "0", "-1", "n", "N", "false", "FALSE", "False", "fAlSe", " -1 ", "\u3000n", "\u0001false" };

    // Numeric text is not parsed; only the exact texts above are accepted. The word false spelled with the long s
    // (U+017F), which String.equalsIgnoreCase would match to "false", is rejected: "any case" means ASCII case.
    private static final String[] REJECTED_TEXT = { "2", "9", "-2", "+1", "01", "00", "-0", "1.0", "0.0", "1e0", "1L", "0x1", "10", "--1", "-", "+", ".", "yes",
            "no", "Yes", "NO", "T", "F", "t", "f", "on", "off", "abc", "tru", "truee", "fals", "falsee", "NaN", "Infinity", "null", "y n", "tr ue", "1 1",
            "\u0001", "fal\u017Fe", "\uFF54rue", "\u30002" };

    @Test
    void textIsReadByTheExactTokens() {
        for (final Type<Boolean> type : List.of(Type.of(boolean.class), Type.of(Boolean.class))) {
            for (final String text : TRUE_TEXT) {
                assertEquals(Boolean.TRUE, type.valueOf(text), type.name() + " " + escape(text));
                assertEquals(Boolean.TRUE, type.valueOf(new StringBuilder(text)), type.name() + " CharSequence " + escape(text));
                assertEquals(Boolean.TRUE, N.convert(text, type.javaType()), "convert " + escape(text));
            }

            for (final String text : FALSE_TEXT) {
                assertEquals(Boolean.FALSE, type.valueOf(text), type.name() + " " + escape(text));
                assertEquals(Boolean.FALSE, type.valueOf(new StringBuilder(text)), type.name() + " CharSequence " + escape(text));
                assertEquals(Boolean.FALSE, N.convert(text, type.javaType()), "convert " + escape(text));
            }

            for (final String text : REJECTED_TEXT) {
                assertRejectedText(() -> type.valueOf(text), text);
                assertRejectedText(() -> type.valueOf(new StringBuilder(text)), text);
                assertRejectedText(() -> N.convert(text, type.javaType()), text);
            }
        }

        // Blank text is absent: the type default from valueOf(String), FALSE from valueOf(Object).
        assertEquals(Boolean.FALSE, Type.of(boolean.class).valueOf(" "));
        assertNull(Type.of(Boolean.class).valueOf(" "));
        assertNull(Type.of(Boolean.class).valueOf(""));
        assertEquals(Boolean.FALSE, Type.of(Boolean.class).valueOf((Object) " "));
        assertEquals(Boolean.FALSE, N.convert("", Boolean.class));
    }

    @Test
    void characterArrayRegionsAgreeWithTheStringPath() {
        for (final Type<Boolean> type : List.of(Type.of(boolean.class), Type.of(Boolean.class), Type.<Boolean> of("BooleanInt"),
                Type.<Boolean> of("BooleanChar"))) {
            for (final String text : concat(TRUE_TEXT, FALSE_TEXT, REJECTED_TEXT, new String[] { "", " ", "\u3000", "x" })) {
                final char[] padded = ("[" + text + "]").toCharArray();
                assertEquals(outcome(() -> type.valueOf(text)), outcome(() -> type.valueOf(padded, 1, text.length())), type.name() + " char[] " + escape(text));
            }
        }
    }

    @Test
    void onlyIntegerShortAndByteNumbersAreRead() {
        final Object[] one = { 1, (short) 1, (byte) 1 };
        final Object[] zeroOrMinusOne = { 0, -1, (short) 0, (short) -1, (byte) 0, (byte) -1 };
        // An Integer, Short or Byte outside -1..1 is a number that is not a boolean.
        final Object[] outOfRange = { 2, -2, Integer.MAX_VALUE, Integer.MIN_VALUE, (short) 3, Short.MIN_VALUE, (byte) 4, Byte.MIN_VALUE };
        // Any other Number class is rejected whatever its value, 1, 0 and -1 included.
        final Number[] otherClasses = { 1L, 0L, -1L, Long.MAX_VALUE, 1.0, 0.0, -1.0, 0.5, Double.NaN, 1.0f, -0.0f, BigInteger.ONE, BigInteger.ZERO,
                new BigDecimal("1.00"), BigDecimal.ZERO, new AtomicInteger(1), new AtomicInteger(0), new AtomicLong(-1) };

        for (final Object number : one) {
            assertEquals(Boolean.TRUE, Type.of(boolean.class).valueOf(number), "Type " + number);
            assertEquals(Boolean.TRUE, Type.of(Boolean.class).valueOf(number), "Type " + number);
            assertEquals(Boolean.TRUE, N.convert(number, Boolean.class), "convert " + number);
            assertEquals(Boolean.TRUE, N.convert(number, boolean.class), "convert " + number);
            assertTrue(Maps.getAsBoolean(Map.of("k", number), "k").orElseThrow(), "Maps " + number);
            assertTrue(Maps.getAsBooleanOrDefaultIfAbsent(Map.of("k", number), "k", false), "Maps " + number);
            assertEquals(Boolean.TRUE, Type.<Boolean> of("BooleanInt").valueOf(number), "BooleanInt " + number);
            assertEquals(Boolean.TRUE, Type.<Boolean> of("BooleanChar").valueOf(number), "BooleanChar " + number);
        }

        for (final Object number : zeroOrMinusOne) {
            assertEquals(Boolean.FALSE, Type.of(boolean.class).valueOf(number), "Type " + number);
            assertEquals(Boolean.FALSE, Type.of(Boolean.class).valueOf(number), "Type " + number);
            assertEquals(Boolean.FALSE, N.convert(number, Boolean.class), "convert " + number);
            assertEquals(Boolean.FALSE, N.convert(number, boolean.class), "convert " + number);
            assertFalse(Maps.getAsBoolean(Map.of("k", number), "k").orElseThrow(), "Maps " + number);
            assertFalse(Maps.getAsBooleanOrDefaultIfAbsent(Map.of("k", number), "k", true), "Maps " + number);
            assertEquals(Boolean.FALSE, Type.<Boolean> of("BooleanInt").valueOf(number), "BooleanInt " + number);
            assertEquals(Boolean.FALSE, Type.<Boolean> of("BooleanChar").valueOf(number), "BooleanChar " + number);
        }

        for (final Object number : outOfRange) {
            final String message = "Cannot convert " + number.getClass().getSimpleName() + " value " + number + " to boolean: " + NUMBER_RULE;
            assertMessage(message, () -> Type.of(boolean.class).valueOf(number));
            assertMessage(message, () -> Type.of(Boolean.class).valueOf(number));
            assertMessage(message, () -> N.convert(number, Boolean.class));
            assertMessage(message, () -> N.convert(number, boolean.class));
            assertMessage(message, () -> Maps.getAsBoolean(Map.of("k", number), "k"));
            assertMessage(message, () -> Maps.getAsBooleanOrDefaultIfAbsent(Map.of("k", number), "k", false));
            assertMessage(message, () -> Type.<Boolean> of("BooleanInt").valueOf(number));
            assertMessage(message, () -> Type.<Boolean> of("BooleanChar").valueOf(number));
        }

        for (final Number number : otherClasses) {
            assertRejectedNumberType(() -> Type.of(boolean.class).valueOf(number), number);
            assertRejectedNumberType(() -> Type.of(Boolean.class).valueOf(number), number);
            assertRejectedNumberType(() -> N.convert(number, Boolean.class), number);
            assertRejectedNumberType(() -> N.convert(number, boolean.class), number);
            assertRejectedNumberType(() -> Maps.getAsBoolean(Map.of("k", number), "k"), number);
            assertRejectedNumberType(() -> Maps.getAsBooleanOrDefaultIfAbsent(Map.of("k", number), "k", false), number);
            assertRejectedNumberType(() -> Type.<Boolean> of("BooleanInt").valueOf(number), number);
            assertRejectedNumberType(() -> Type.<Boolean> of("BooleanChar").valueOf(number), number);
        }

        // The N.convert Javadoc examples.
        assertEquals(Boolean.TRUE, N.convert(1, Boolean.class));
        assertEquals(Boolean.FALSE, N.convert(-1, Boolean.class));
        assertEquals(Boolean.FALSE, N.convert(0, Boolean.class));
        assertEquals(Boolean.TRUE, N.convert("Y", Boolean.class));
        assertRejectedNumber(() -> N.convert(2, Boolean.class), "convert 2");
        assertRejectedNumberType(() -> N.convert(1L, Boolean.class), 1L);
        assertRejectedText(() -> N.convert("yes", Boolean.class), "yes");
    }

    @Test
    void booleanOfReadsOnlyIntegerShortAndByte() {
        assertTrue(AbstractType.booleanOf(1));
        assertTrue(AbstractType.booleanOf((short) 1));
        assertTrue(AbstractType.booleanOf((byte) 1));
        assertFalse(AbstractType.booleanOf(0));
        assertFalse(AbstractType.booleanOf((short) -1));
        assertFalse(AbstractType.booleanOf((byte) 0));
        // null is false; the public converters answer null before they get here.
        assertFalse(AbstractType.booleanOf(null));

        // The message names the type with the value, and the values that are accepted.
        assertMessage("Cannot convert Integer value 2 to boolean: " + NUMBER_RULE, () -> AbstractType.booleanOf(2));
        assertMessage("Cannot convert Short value -2 to boolean: " + NUMBER_RULE, () -> AbstractType.booleanOf((short) -2));
        assertMessage("Cannot convert Byte value 127 to boolean: " + NUMBER_RULE, () -> AbstractType.booleanOf((byte) 127));
        assertMessage("Cannot convert java.lang.Long value 1 to boolean: " + NUMBER_TYPE_RULE, () -> AbstractType.booleanOf(1L));
        assertMessage("Cannot convert java.lang.Double value 1.0 to boolean: " + NUMBER_TYPE_RULE, () -> AbstractType.booleanOf(1.0));
        assertRejectedNumberType(() -> AbstractType.booleanOf(1L), 1L);
        assertRejectedNumberType(() -> AbstractType.booleanOf(1.0), 1.0);
        assertRejectedNumberType(() -> AbstractType.booleanOf(BigInteger.ONE), BigInteger.ONE);
        assertRejectedNumberType(() -> AbstractType.booleanOf(new AtomicInteger(1)), new AtomicInteger(1));
    }

    @Test
    void charactersAreOneCharacterText() {
        for (char ch = 0; ch < 128; ch++) {
            final Character character = ch;
            final String message = "Character " + escape(String.valueOf(ch));

            if (ch == '1' || ch == 'y' || ch == 'Y') {
                assertEquals(Boolean.TRUE, Type.of(boolean.class).valueOf(character), message);
                assertEquals(Boolean.TRUE, N.convert(character, Boolean.class), message);
            } else if (ch == '0' || ch == 'n' || ch == 'N' || Character.isWhitespace(ch)) {
                // A whitespace character is blank text, which valueOf(Object) reads as FALSE.
                assertEquals(Boolean.FALSE, Type.of(boolean.class).valueOf(character), message);
                assertEquals(Boolean.FALSE, N.convert(character, Boolean.class), message);
            } else {
                assertRejectedText(() -> Type.of(boolean.class).valueOf(character), String.valueOf(ch));
                assertRejectedText(() -> N.convert(character, Boolean.class), String.valueOf(ch));
            }
        }

        // CharToBooleanFunction.DEFAULT is a character test, not a conversion: it keeps its 'Y'/'y'/'1' rule and never throws.
        for (char ch = '0'; ch <= '9'; ch++) {
            assertEquals(ch == '1', CharToBooleanFunction.DEFAULT.applyAsBoolean(ch), "CharToBooleanFunction " + ch);
        }
        assertTrue(CharToBooleanFunction.DEFAULT.applyAsBoolean('Y'));
        assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('x'));
    }

    @Test
    void otherObjectsAreReadByTheirStringForm() {
        assertEquals(Boolean.TRUE, N.convert(new AtomicBoolean(true), Boolean.class));
        assertEquals(Boolean.FALSE, Type.of(boolean.class).valueOf(MutableBoolean.of(false)));
        assertRejectedText(() -> Type.of(Boolean.class).valueOf(OptionalBoolean.of(true)), OptionalBoolean.of(true).toString());
        assertRejectedText(() -> Type.of(Boolean.class).valueOf(List.of()), "[]");
    }

    @Test
    void wrapperTypesReadNumbersAsBooleanDoes() {
        // AtomicBoolean, MutableBoolean and OptionalBoolean read a Number itself, as Boolean does: through its string
        // form a Long 1 would pass as the text "1", though Boolean rejects a Long.
        assertTrue(N.convert(1, AtomicBoolean.class).get());
        assertFalse(N.convert((short) -1, AtomicBoolean.class).get());
        assertTrue(N.convert((byte) 1, MutableBoolean.class).value());
        assertFalse(N.convert(0, MutableBoolean.class).value());
        assertEquals(OptionalBoolean.of(true), N.convert(1, OptionalBoolean.class));
        assertEquals(OptionalBoolean.of(false), N.convert(-1, OptionalBoolean.class));

        assertTrue(Type.<AtomicBoolean> of(AtomicBoolean.class).valueOf(1).get());
        assertFalse(Type.<MutableBoolean> of(MutableBoolean.class).valueOf((short) 0).value());
        assertEquals(OptionalBoolean.of(true), Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf((byte) 1));

        assertRejectedNumber(() -> N.convert(2, AtomicBoolean.class), "AtomicBoolean 2");
        assertRejectedNumberType(() -> N.convert(1L, AtomicBoolean.class), 1L);
        assertRejectedNumberType(() -> N.convert(1.0, MutableBoolean.class), 1.0);
        assertRejectedNumberType(() -> N.convert(BigDecimal.ONE, OptionalBoolean.class), BigDecimal.ONE);
        assertRejectedNumberType(() -> Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf(new AtomicInteger(0)), new AtomicInteger(0));

        // Anything else still goes through the string form, and null keeps each type's absent value.
        assertTrue(Type.<AtomicBoolean> of(AtomicBoolean.class).valueOf((Object) "Y").get());
        assertTrue(Type.<MutableBoolean> of(MutableBoolean.class).valueOf(Boolean.TRUE).value());
        assertRejectedText(() -> Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf((Object) "yes"), "yes");
        assertNull(Type.<AtomicBoolean> of(AtomicBoolean.class).valueOf((Object) null));
        assertNull(Type.<MutableBoolean> of(MutableBoolean.class).valueOf((Object) null));
        assertEquals(OptionalBoolean.empty(), Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf((Object) null));
    }

    @Test
    void wrappedAndEncodedBooleanTypesFollowTheRule() {
        assertTrue(Type.<AtomicBoolean> of(AtomicBoolean.class).valueOf("Y").get());
        assertFalse(Type.<AtomicBoolean> of(AtomicBoolean.class).valueOf("-1").get());
        assertNull(Type.<AtomicBoolean> of(AtomicBoolean.class).valueOf(" "));
        assertRejectedText(() -> Type.<AtomicBoolean> of(AtomicBoolean.class).valueOf("2"), "2");
        assertTrue(Type.<MutableBoolean> of(MutableBoolean.class).valueOf("TRUE").value());
        assertFalse(Type.<MutableBoolean> of(MutableBoolean.class).valueOf("n").value());
        assertRejectedText(() -> Type.<MutableBoolean> of(MutableBoolean.class).valueOf("yes"), "yes");
        assertEquals(OptionalBoolean.of(true), Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf("1"));
        assertEquals(OptionalBoolean.of(false), Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf("False"));
        assertEquals(OptionalBoolean.empty(), Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf(""));
        assertRejectedText(() -> Type.<OptionalBoolean> of(OptionalBoolean.class).valueOf("1.0"), "1.0");

        // BooleanInt (1/0) and BooleanChar (Y/N) write their own codes but read every accepted text; null and blank text
        // are their default, false.
        for (final Type<Boolean> type : List.of(Type.<Boolean> of("BooleanInt"), Type.<Boolean> of("BooleanChar"))) {
            for (final String text : new String[] { "1", "Y", "y", "true", " 1", "TRUE" }) {
                assertEquals(Boolean.TRUE, type.valueOf(text), type.name() + " " + text);
                assertEquals(Boolean.TRUE, type.valueOf(text.toCharArray(), 0, text.length()), type.name() + " char[] " + text);
            }
            for (final String text : new String[] { "0", "-1", "N", "n", "false", "", " " }) {
                assertEquals(Boolean.FALSE, type.valueOf(text), type.name() + " " + text);
                assertEquals(Boolean.FALSE, type.valueOf(text.toCharArray(), 0, text.length()), type.name() + " char[] " + text);
            }
            for (final String text : new String[] { "2", "10", "1.0", "-2", "abc", "T", "yes" }) {
                assertRejectedText(() -> type.valueOf(text), text);
                assertRejectedText(() -> type.valueOf(text.toCharArray(), 0, text.length()), text);
            }
            assertEquals(Boolean.FALSE, type.valueOf((String) null));
            assertEquals(Boolean.FALSE, type.valueOf((char[]) null, 0, 0));
            assertEquals(Boolean.TRUE, type.valueOf((Object) "Y"));
            assertRejectedText(() -> type.valueOf((Object) "x"), "x");
            assertEquals(type.stringOf(true), type.stringOf(type.valueOf(type.stringOf(true))));
            assertEquals(type.stringOf(false), type.stringOf(type.valueOf(type.stringOf(false))));
        }
    }

    @Test
    void quotedAndUnquotedListElementsAgreeOnEveryPath() {
        assertEquals(BooleanList.of(true, false, false), jsonParser.deserialize("[\"1\",\"0\",\"-1\"]", BooleanList.class));
        assertEquals(BooleanList.of(true, false, false), jsonParser.deserialize(new StringReader("[\"Y\",\"n\",\"-1\"]"), BooleanList.class));
        assertEquals(BooleanList.of(true, false, false), Type.of(BooleanList.class).valueOf("[\"1\",\"N\",\"-1\"]"));

        final String[] spellings = { "[1, 0, -1, true, \"Y\", \"n\", false, null]", "[\"1\", \"0\", \"-1\", \"true\", \"Y\", \"n\", \"false\", null]",
                "['1', '0', '-1', 'TRUE', 'Y', 'n', 'False', null]" };
        final boolean[] expected = { true, false, false, true, true, false, false, false };
        final Boolean[] expectedBoxed = { true, false, false, true, true, false, false, null };

        for (final String json : spellings) {
            assertArrayEquals(expected, jsonParser.deserialize(json, boolean[].class), "json boolean[] " + json);
            assertArrayEquals(expected, Type.of(boolean[].class).valueOf(json), "valueOf boolean[] " + json);
            assertEquals(BooleanList.of(expected), jsonParser.deserialize(json, BooleanList.class), "json BooleanList " + json);
            assertEquals(BooleanList.of(expected), Type.of(BooleanList.class).valueOf(json), "valueOf BooleanList " + json);
            assertArrayEquals(expectedBoxed, jsonParser.deserialize(json, Boolean[].class), "json Boolean[] " + json);
            assertArrayEquals(expectedBoxed, Type.of(Boolean[].class).valueOf(json), "valueOf Boolean[] " + json);
        }

        // A quoted "2" is the text 2, an unquoted 2 the number 2: neither is a boolean value.
        for (final String json : new String[] { "[\"1\",\"2\"]", "[1, 2]", "[1, 0.5]", "[1, -2]", "['1', '1.0']", "[\"1\", \"yes\"]", "['T']" }) {
            assertRejected(() -> jsonParser.deserialize(json, BooleanList.class), "json BooleanList " + json);
            assertRejected(() -> jsonParser.deserialize(new StringReader(json), BooleanList.class), "json reader BooleanList " + json);
            assertRejected(() -> Type.of(BooleanList.class).valueOf(json), "valueOf BooleanList " + json);
            assertRejected(() -> jsonParser.deserialize(json, boolean[].class), "json boolean[] " + json);
            assertRejected(() -> Type.of(boolean[].class).valueOf(json), "valueOf boolean[] " + json);
            assertRejected(() -> jsonParser.deserialize(json, Boolean[].class), "json Boolean[] " + json);
            assertRejected(() -> Type.of(Boolean[].class).valueOf(json), "valueOf Boolean[] " + json);
        }
    }

    @Test
    void jsonScalarsFollowTheRule() {
        for (final String json : new String[] { "1", "0", "-1", "true", "false", "\"1\"", "\"0\"", "\"-1\"", "\"Y\"", "\"n\"", "\"TRUE\"", "\"False\"" }) {
            final boolean expected = json.equals("1") || json.equalsIgnoreCase("true") || json.equals("\"1\"") || json.equals("\"Y\"")
                    || json.equalsIgnoreCase("\"true\"");
            assertEquals(expected, jsonParser.deserialize("{\"flag\":" + json + "}", FlagBean.class).isFlag(), "flag " + json);
            assertEquals(expected, jsonParser.deserialize("{\"boxed\":" + json + "}", FlagBean.class).getBoxed(), "boxed " + json);
        }

        // A JSON number token is a number: a decimal or long-sized one is a Number of a class other than Integer,
        // Short or Byte, so it is rejected whatever its value; the same spelling quoted is text, which is never parsed.
        assertRejectedNumberType(() -> jsonParser.deserialize("{\"flag\":1.0}", FlagBean.class), 1.0);
        assertRejectedNumberType(() -> jsonParser.deserialize("{\"boxed\":-1.0}", FlagBean.class), -1.0);
        assertRejectedNumberType(() -> jsonParser.deserialize("{\"flag\":3000000000}", FlagBean.class), 3000000000L);

        for (final String json : new String[] { "2", "-2", "0.5", "1e3", "\"2\"", "\"1.0\"", "\"yes\"", "\"T\"" }) {
            assertRejected(() -> jsonParser.deserialize("{\"flag\":" + json + "}", FlagBean.class), "flag " + json);
            assertRejected(() -> jsonParser.deserialize("{\"boxed\":" + json + "}", FlagBean.class), "boxed " + json);
        }
    }

    @Test
    void xmlScalarsFollowTheSameRule() {
        final FlagBean template = new FlagBean();
        template.setFlag(true);
        template.setBoxed(Boolean.TRUE);

        for (final XmlParser xmlParser : List.of(ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser())) {
            final String xml = xmlParser.serialize(template);
            final String parserName = xmlParser.getClass().getSimpleName();
            assertTrue(xml.contains("<flag>true</flag>") && xml.contains("<boxed>true</boxed>"), xml);

            for (final String text : new String[] { "1", "Y", "true", "0", "-1", "n", "FALSE", "2", "1.0", "yes" }) {
                final String textXml = xml.replace("<flag>true</flag>", "<flag>" + text + "</flag>")
                        .replace("<boxed>true</boxed>", "<boxed>" + text + "</boxed>");

                if (List.of("1", "Y", "true").contains(text)) {
                    final FlagBean bean = xmlParser.deserialize(textXml, FlagBean.class);
                    assertTrue(bean.isFlag(), parserName + " flag " + text);
                    assertEquals(Boolean.TRUE, bean.getBoxed(), parserName + " boxed " + text);
                } else if (List.of("0", "-1", "n", "FALSE").contains(text)) {
                    final FlagBean bean = xmlParser.deserialize(textXml, FlagBean.class);
                    assertFalse(bean.isFlag(), parserName + " flag " + text);
                    assertEquals(Boolean.FALSE, bean.getBoxed(), parserName + " boxed " + text);
                } else {
                    assertRejected(() -> xmlParser.deserialize(textXml, FlagBean.class), parserName + " " + text);
                }
            }
        }
    }

    @Test
    void jdbcValuesReadByTheBooleanHandlersFollowTheRule() throws SQLException {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(1);
        when(rs.getObject(2)).thenReturn((short) -1);
        when(rs.getObject(3)).thenReturn("Y");
        when(rs.getObject(4)).thenReturn(2);
        when(rs.getObject(5)).thenReturn("yes");
        when(rs.getObject("n")).thenReturn("n");
        when(rs.getObject("half")).thenReturn(0.5d);
        // A BIGINT or NUMERIC column is read as a Long or BigDecimal, which is rejected whatever its value.
        when(rs.getObject("bigint")).thenReturn(1L);
        when(rs.getObject("numeric")).thenReturn(BigDecimal.ONE);
        when(rs.getString(1)).thenReturn("Y");
        when(rs.getString(2)).thenReturn("N");
        when(rs.getString(3)).thenReturn(null);
        when(rs.getString(4)).thenReturn("X");

        final Type<Boolean> booleanType = Type.of(Boolean.class);
        final Type<OptionalBoolean> optionalType = Type.of(OptionalBoolean.class);
        final Type<Boolean> booleanChar = Type.of("BooleanChar");

        assertEquals(Boolean.TRUE, booleanType.get(rs, 1));
        assertEquals(Boolean.FALSE, booleanType.get(rs, 2));
        assertEquals(Boolean.TRUE, booleanType.get(rs, 3));
        assertEquals(Boolean.FALSE, booleanType.get(rs, "n"));
        assertRejectedNumber(() -> booleanType.get(rs, 4), "Boolean column 2");
        assertRejectedText(() -> booleanType.get(rs, 5), "yes");
        assertRejectedNumberType(() -> booleanType.get(rs, "half"), 0.5d);
        assertRejectedNumberType(() -> booleanType.get(rs, "bigint"), 1L);
        assertRejectedNumberType(() -> booleanType.get(rs, "numeric"), BigDecimal.ONE);
        assertRejectedNumberType(() -> optionalType.get(rs, "bigint"), 1L);

        assertEquals(OptionalBoolean.of(true), optionalType.get(rs, 1));
        assertEquals(OptionalBoolean.of(true), optionalType.get(rs, 3));
        assertRejectedNumber(() -> optionalType.get(rs, 4), "OptionalBoolean column 2");
        assertRejectedText(() -> optionalType.get(rs, 5), "yes");

        assertEquals(Boolean.TRUE, booleanChar.get(rs, 1));
        assertEquals(Boolean.FALSE, booleanChar.get(rs, 2));
        assertEquals(Boolean.FALSE, booleanChar.get(rs, 3));
        assertRejectedText(() -> booleanChar.get(rs, 4), "X");
    }

    @Test
    void mapsGetAsBooleanFollowsTheRule() {
        final Map<String, Object> map = Map.of("y", "Y", "n", " n ", "blank", "", "yes", "yes", "two", "2", "char", 'N');

        assertTrue(Maps.getAsBoolean(map, "y").orElseThrow());
        assertFalse(Maps.getAsBoolean(map, "n").orElseThrow());
        assertFalse(Maps.getAsBoolean(map, "blank").orElseThrow());
        assertFalse(Maps.getAsBoolean(map, "char").orElseThrow());
        assertFalse(Maps.getAsBoolean(map, "absent").isPresent());
        assertRejectedText(() -> Maps.getAsBoolean(map, "yes"), "yes");
        assertRejectedText(() -> Maps.getAsBooleanOrDefaultIfAbsent(map, "two", true), "2");
        assertRejectedText(() -> Maps.getAs(map, "yes", Boolean.class), "yes");
    }

    @Test
    void rejectionMessagesNameTheValueBrieflyAndOnOneLine() {
        assertEquals("Cannot convert \"abc\" to boolean: " + TEXT_RULE,
                assertThrows(IllegalArgumentException.class, () -> Type.of(boolean.class).valueOf("abc")).getMessage());
        assertEquals("Cannot convert \"5\" to boolean: " + TEXT_RULE,
                assertThrows(IllegalArgumentException.class, () -> Type.of(boolean.class).valueOf(new char[] { '5' }, 0, 1)).getMessage());
        assertEquals("Cannot convert Integer value 2 to boolean: " + NUMBER_RULE,
                assertThrows(IllegalArgumentException.class, () -> N.convert(2, Boolean.class)).getMessage());
        assertEquals("Cannot convert java.lang.Double value 0.5 to boolean: " + NUMBER_TYPE_RULE,
                assertThrows(IllegalArgumentException.class, () -> N.convert(0.5, Boolean.class)).getMessage());

        // A megabyte of text is cut, not quoted whole, and costs no parse; a BigInteger or BigDecimal too wide to print
        // cheaply is summarized by its sign and size, so even a million-digit value costs no toString().
        final String million = "1" + "0".repeat(1_000_000);
        assertEquals("Cannot convert \"" + million.substring(0, 64) + "...[1000001 chars]\" to boolean: " + TEXT_RULE,
                assertThrows(IllegalArgumentException.class, () -> Type.of(boolean.class).valueOf(million)).getMessage());
        assertEquals("Cannot convert java.math.BigInteger value BigInteger[signum=1, bitLength=4000001] to boolean: " + NUMBER_TYPE_RULE,
                assertThrows(IllegalArgumentException.class, () -> N.convert(BigInteger.ONE.shiftLeft(4_000_000), Boolean.class)).getMessage());
        // -2^4000 needs 4000 bits in two's complement.
        assertEquals("Cannot convert java.math.BigDecimal value BigDecimal[signum=-1, unscaledBitLength=4000, scale=5] to boolean: " + NUMBER_TYPE_RULE,
                assertThrows(IllegalArgumentException.class, () -> N.convert(new BigDecimal(BigInteger.ONE.shiftLeft(4000).negate(), 5), Boolean.class))
                        .getMessage());
        // At most 192 bits are printed in full.
        final BigInteger widestPrinted = BigInteger.ONE.shiftLeft(191);
        assertEquals("Cannot convert java.math.BigInteger value " + widestPrinted + " to boolean: " + NUMBER_TYPE_RULE,
                assertThrows(IllegalArgumentException.class, () -> N.convert(widestPrinted, Boolean.class)).getMessage());

        // Control characters and line separators in the value are escaped, so they cannot break the log line.
        assertEquals("Cannot convert \"\\u0001\" to boolean: " + TEXT_RULE,
                assertThrows(IllegalArgumentException.class, () -> Type.of(boolean.class).valueOf("\u0001")).getMessage());

        final Number forged = new Number() {
            @Override
            public int intValue() {
                return 2;
            }

            @Override
            public long longValue() {
                return 2;
            }

            @Override
            public float floatValue() {
                return 2;
            }

            @Override
            public double doubleValue() {
                return 2;
            }

            @Override
            public String toString() {
                return "2" + (char) 10 + "forged" + (char) 0x2028 + "line";
            }
        };

        // A Number subtype prints itself, and its control characters and line separators are escaped, so they cannot
        // break the log line.
        assertEquals("Cannot convert " + forged.getClass().getName() + " value 2" + BACKSLASH + "u000aforged" + BACKSLASH + "u2028line to boolean: "
                + NUMBER_TYPE_RULE, assertThrows(IllegalArgumentException.class, () -> Type.of(boolean.class).valueOf(forged)).getMessage());

        // Unicode format characters (a right-to-left override, a zero-width space, a tag character) and an unpaired
        // surrogate are escaped too; a well-formed pair that is printable (an emoji) is kept.
        final String tricky = "a" + (char) 0x202E + "b" + (char) 0x200B + new String(Character.toChars(0xE0041)) + (char) 0xD800 + "c"
                + new String(Character.toChars(0x1F600));
        final String expectedText = "a" + BACKSLASH + "u202e" + "b" + BACKSLASH + "u200b" + BACKSLASH + "udb40" + BACKSLASH + "udc41" + BACKSLASH + "ud800"
                + "c" + new String(Character.toChars(0x1F600));
        assertEquals("Cannot convert \"" + expectedText + "\" to boolean: " + TEXT_RULE,
                assertThrows(IllegalArgumentException.class, () -> Type.of(boolean.class).valueOf(tricky)).getMessage());

        // A cut never splits a surrogate pair: 63 letters and an emoji keep 63 letters.
        final String longWithPair = "x".repeat(63) + new String(Character.toChars(0x1F600)) + "tail";
        assertEquals("Cannot convert \"" + "x".repeat(63) + "...[69 chars]\" to boolean: " + TEXT_RULE,
                assertThrows(IllegalArgumentException.class, () -> Type.of(boolean.class).valueOf(longWithPair)).getMessage());
    }

    @Test
    void emptyQuotedArrayElementsReadTheSameAsInJson() {
        assertArrayEquals(new boolean[] { false }, Type.of(boolean[].class).valueOf("[\"\"]"));
        assertArrayEquals(new boolean[] { false }, jsonParser.deserialize("[\"\"]", boolean[].class));
        assertEquals(BooleanList.of(false), Type.of(BooleanList.class).valueOf("[\"\"]"));
        assertEquals(BooleanList.of(false), jsonParser.deserialize("[\"\"]", BooleanList.class));
        // A Boolean element reads blank text as its default, null.
        assertArrayEquals(new Boolean[] { null }, Type.of(Boolean[].class).valueOf("[\"\"]"));
        assertArrayEquals(new Boolean[] { null }, jsonParser.deserialize("[\"\"]", Boolean[].class));
    }

    @Test
    void quotedArrayElementsAreUnescapedOnEveryPath() {
        // A quoted element is read by its decoded content, as a JSON string is: an escaped 1 is 1, an escaped tab is
        // padding and an escaped em space is blank text. Before, the boolean[] text form read the escape's letters,
        // which disagreed with the JSON parser once unrecognized text was rejected.
        final String json = "[\"" + BACKSLASH + "u0031\", \"" + BACKSLASH + "tY\", '" + BACKSLASH + "u2003', \"n\"]";
        final boolean[] expected = { true, true, false, false };

        assertArrayEquals(expected, Type.of(boolean[].class).valueOf(json), json);
        assertArrayEquals(expected, jsonParser.deserialize(json, boolean[].class), json);
        assertEquals(BooleanList.of(expected), Type.of(BooleanList.class).valueOf(json), json);
        assertEquals(BooleanList.of(expected), jsonParser.deserialize(json, BooleanList.class), json);
        assertEquals(BooleanList.of(expected), jsonParser.deserialize(new StringReader(json), BooleanList.class), json);
        // A blank Boolean element is its default, null.
        assertArrayEquals(new Boolean[] { true, true, null, false }, Type.of(Boolean[].class).valueOf(json), json);
        assertArrayEquals(new Boolean[] { true, true, null, false }, jsonParser.deserialize(json, Boolean[].class), json);

        // An element whose escapes decode to something that is not boolean text is still rejected, on every path.
        final String doubleEscaped = "[\"" + BACKSLASH + BACKSLASH + "u0031\"]";
        assertRejectedText(() -> Type.of(boolean[].class).valueOf(doubleEscaped), BACKSLASH + "u0031");
        assertRejectedText(() -> jsonParser.deserialize(doubleEscaped, BooleanList.class), BACKSLASH + "u0031");

        // A malformed escape is rejected too.
        assertThrows(IllegalArgumentException.class, () -> Type.of(boolean[].class).valueOf("[\"" + BACKSLASH + "u12\"]"));
        assertThrows(IllegalArgumentException.class, () -> Type.of(Boolean[].class).valueOf("[\"" + BACKSLASH + "u12\"]"));
    }

    private static void assertRejectedText(final Executable call, final String text) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call, escape(text));
        assertTrue(e.getMessage() != null && e.getMessage().endsWith(" to boolean: " + TEXT_RULE), escape(text) + ": " + e.getMessage());
    }

    private static void assertRejectedNumber(final Executable call, final String message) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call, message);
        assertTrue(e.getMessage() != null && e.getMessage().endsWith(" to boolean: " + NUMBER_RULE), message + ": " + e.getMessage());
    }

    private static void assertRejectedNumberType(final Executable call, final Number number) {
        assertMessage("Cannot convert " + number.getClass().getName() + " value " + number + " to boolean: " + NUMBER_TYPE_RULE, call);
    }

    private static void assertMessage(final String expected, final Executable call) {
        assertEquals(expected, assertThrows(IllegalArgumentException.class, call, expected).getMessage());
    }

    private static void assertRejected(final Executable call, final String message) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call, message);
        assertTrue(
                e.getMessage() != null
                        && (e.getMessage().endsWith(TEXT_RULE) || e.getMessage().endsWith(NUMBER_RULE) || e.getMessage().endsWith(NUMBER_TYPE_RULE)),
                message + ": " + e.getMessage());
    }

    private static Object outcome(final Supplier<?> call) {
        try {
            return call.get();
        } catch (final RuntimeException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static String escape(final String text) {
        final StringBuilder sb = new StringBuilder();

        for (final char ch : text.toCharArray()) {
            sb.append(ch < ' ' || ch > '~' ? String.format("\\u%04X", (int) ch) : String.valueOf(ch));
        }

        return sb.toString();
    }

    private static String[] concat(final String[]... arrays) {
        return Arrays.stream(arrays).flatMap(Arrays::stream).toArray(String[]::new);
    }

    public static class FlagBean {
        private boolean flag;
        private Boolean boxed;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(final boolean flag) {
            this.flag = flag;
        }

        public Boolean getBoxed() {
            return boxed;
        }

        public void setBoxed(final Boolean boxed) {
            this.boxed = boxed;
        }
    }
}
