package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class ParserNumericTest extends TestBase {
    private final JsonParser parser = ParserFactory.createJsonParser();
    private static final Type<List<BigDecimal>> DECIMAL_LIST = Type.of("List<java.math.BigDecimal>");
    private static final Type<Map<String, BigDecimal>> DECIMAL_MAP = Type.of("Map<String, java.math.BigDecimal>");

    public static class DecimalBean {
        private BigDecimal value;

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }

    @Test
    public void exactDecimalValuesAndScaleAcrossTargetsAndSources() {
        for (String token : List.of("0.8000000067211262", "1.2300", "1.0000", "0.00", "-0.00", "12345678901234567890.00100", "1.23000000000000000000", "1.20e3",
                "0.00000000000000012345", "9007199254740991.0")) {
            BigDecimal expected = new BigDecimal(token);
            String array = "[" + token + "]";
            assertEquals(List.of(expected), parser.deserialize(array, DECIMAL_LIST), token);
            assertEquals(List.of(expected), parser.deserialize(new StringReader(array), DECIMAL_LIST), token);
            assertEquals(List.of(expected), parser.deserialize(new ByteArrayInputStream(array.getBytes(StandardCharsets.UTF_8)), DECIMAL_LIST), token);
            assertArrayEquals(new BigDecimal[] { expected }, parser.deserialize(array, BigDecimal[].class), token);
            assertEquals(Map.of("\u540d\ud83d\ude00", expected), parser.deserialize("{\"\u540d\ud83d\ude00\":" + token + "}", DECIMAL_MAP), token);
            assertEquals(expected, parser.deserialize("{\"value\":" + token + "}", DecimalBean.class).getValue(), token);
            assertEquals(expected, parser.deserialize(new StringReader("{\"value\":" + token + "}"), DecimalBean.class).getValue(), token);
            assertEquals(expected, parser.deserialize(token, BigDecimal.class), token);
        }
    }

    @Test
    public void exactDecimalsAcrossTokenHintsAndBufferRefills() {
        for (String token : List.of("1.2300", "0.8000000067211262", "12345678901234567890.00100", "-0.00")) {
            for (int size : new int[] { 1, 2, 3, 7, 32 }) {
                for (Type<?> hint : List.of(Type.of(String.class), Type.of(Object.class), Type.of(BigDecimal.class))) {
                    for (JsonReader reader : List.of(JsonStringReader.parse(token, new char[0]),
                            JsonStreamReader.parse(new StringReader(token), new char[size], new char[0]))) {
                        reader.nextToken(hint);
                        assertEquals(new BigDecimal(token), reader.readValue(Type.of(BigDecimal.class)), token + "/" + size + "/" + hint);
                    }
                }
            }
        }
    }

    @Test
    public void seededExactDecimalRoundTrips() {
        Random random = new Random(42);
        for (int i = 0; i < 2000; i++) {
            BigDecimal expected = new BigDecimal("0." + (8000000000000000L + random.nextInt(99999999)));
            String json = parser.serialize(List.of(expected));
            assertEquals(List.of(expected), parser.deserialize(json, DECIMAL_LIST), json);
        }
    }

    // G08-1: a decimal token the number fast path cached converts to an integral target by truncating toward
    // zero, the behaviour of every other Number -> integral conversion in this library. Handing the raw text to
    // the integral handlers instead made "1.0" into an int a NumberFormatException while "1.0" into a Double,
    // and a quoted "1.0" into a BigDecimal, both kept working. Only a token the fast path cannot cache (more
    // than 18 significant digits, a negative zero, an exponent) still reaches the target's own parser as text.
    @Test
    public void decimalTokensTruncateTowardZeroForIntegralTargets() {
        for (String token : List.of("1.2300", "1.0")) {
            for (Class<?> target : List.of(Byte.class, Short.class, Integer.class, Long.class, BigInteger.class)) {
                Type<?> listType = Type.of("List<" + target.getName() + ">");
                String json = "[" + token + "]";

                for (Object result : List.of(parser.deserialize(json, listType), parser.deserialize(new StringReader(json), listType))) {
                    List<?> parsed = (List<?>) result;
                    assertEquals(1, parsed.size(), token + "/" + target);
                    assertEquals(target, parsed.get(0).getClass(), token + "/" + target);
                    assertEquals("1", parsed.get(0).toString(), token + "/" + target);
                }
            }
        }

        // truncation never widens the target's range
        for (Class<?> target : List.of(Byte.class, Short.class)) {
            Type<?> listType = Type.of("List<" + target.getName() + ">");
            assertThrows(ArithmeticException.class, () -> parser.deserialize("[2147483647.9]", listType), target.getName());
        }
        for (Class<?> target : List.of(Integer.class, Long.class, BigInteger.class)) {
            Type<?> listType = Type.of("List<" + target.getName() + ">");
            assertEquals("[2147483647]", parser.deserialize("[2147483647.9]", listType).toString(), target.getName());
        }

        // a token the fast path could not cache is still handed to the target's parser as raw text
        for (String token : List.of("1.23000000000000000000", "-0.0000", "1e0")) {
            for (Class<?> target : List.of(Byte.class, Short.class, Integer.class, Long.class, BigInteger.class)) {
                Type<?> listType = Type.of("List<" + target.getName() + ">");
                assertThrows(NumberFormatException.class, () -> parser.deserialize("[" + token + "]", listType), token + "/" + target);
                assertThrows(NumberFormatException.class, () -> parser.deserialize(new StringReader("[" + token + "]"), listType), token + "/" + target);
            }
        }
    }

    @Test
    public void integerBoundariesStillConvertExactlyAndRejectOverflow() {
        assertEquals(List.of(Byte.MIN_VALUE, Byte.MAX_VALUE), parser.deserialize("[-128,127]", Type.of("List<Byte>")));
        assertEquals(List.of(Integer.MIN_VALUE, Integer.MAX_VALUE), parser.deserialize("[-2147483648,2147483647]", Type.of("List<Integer>")));
        assertEquals(List.of(Long.MIN_VALUE, Long.MAX_VALUE), parser.deserialize("[-9223372036854775808,9223372036854775807]", Type.of("List<Long>")));
        assertEquals(List.of(new BigInteger("9223372036854775808")), parser.deserialize("[9223372036854775808]", Type.of("List<BigInteger>")));
        for (String token : List.of("128", "-129")) {
            assertThrows(ArithmeticException.class, () -> parser.deserialize("[" + token + "]", Type.of("List<Byte>")));
        }
        assertThrows(ArithmeticException.class, () -> parser.deserialize("[2147483648]", Type.of("List<Integer>")));
        assertThrows(ArithmeticException.class, () -> parser.deserialize("[9223372036854775808]", Type.of("List<Long>")));
    }

    @Test
    public void floatingConversionsRoundOriginalTokenWhenTypeIsKnownLate() {
        for (String token : List.of("0.5000000298023224", "-0.5000000298023224", "0.5000000298023224f", "-0.5000000298023224F", "-0.0")) {
            for (int size : new int[] { 1, 3, 64 }) {
                for (Type<?> hint : List.of(Type.of(Object.class), Type.of(Float.class), Type.of(Double.class))) {
                    for (JsonReader reader : List.of(JsonStringReader.parse(token, new char[0]),
                            JsonStreamReader.parse(new StringReader(token), new char[size], new char[0]))) {
                        reader.nextToken(hint);
                        assertEquals(Float.floatToIntBits(Float.parseFloat(token)), Float.floatToIntBits(reader.readValue(Type.of(Float.class))), token);
                        assertEquals(Double.doubleToLongBits(Double.parseDouble(token)), Double.doubleToLongBits(reader.readValue(Type.of(Double.class))),
                                token);
                    }
                }
            }
        }
        assertEquals(List.of(Float.parseFloat("0.5000000298023224")), parser.deserialize("[0.5000000298023224]", Type.of("List<Float>")));
    }

    // G08-1: the Java type suffix this tokenizer accepts belongs to no numeric grammar - only the integer/long
    // handlers strip one - so it is removed before the raw token reaches BigDecimal/BigInteger. Rejecting the
    // suffix there while "[123L,1.5f]" deserialized happily into a List<Double> was the inconsistency.
    @Test
    public void suffixedTokensConvertForExactTargetsAndObjectInferenceStaysStable() {
        assertEquals(List.of(new BigDecimal("123")), parser.deserialize("[123L]", DECIMAL_LIST));
        assertEquals(List.of(new BigDecimal("1.5")), parser.deserialize("[1.5f]", DECIMAL_LIST));
        assertEquals(List.of(new BigDecimal("1.5")), parser.deserialize("[1.5d]", DECIMAL_LIST));
        assertEquals(List.of(BigInteger.valueOf(123)), parser.deserialize("[123L]", Type.of("List<BigInteger>")));
        assertEquals(List.of(BigInteger.ONE), parser.deserialize("[1.5f]", Type.of("List<BigInteger>")));
        assertEquals(List.of(BigInteger.ONE), parser.deserialize("[1.5d]", Type.of("List<BigInteger>")));

        List<?> values = parser.deserialize("[1,2147483648,1.25,1f]", List.class);
        assertEquals(List.of(1, 2147483648L, 1.25d, 1f), values);
        assertEquals(List.of(123d, 1.5d), parser.deserialize("[123L,1.5f]", Type.of("List<Double>")));
    }

    @Test
    public void nullEmptyAndQuotedDecimalsRemainSupported() {
        assertNull(parser.deserialize((String) null, BigDecimal.class));
        assertEquals(List.of(), parser.deserialize("[]", DECIMAL_LIST));
        assertEquals(List.of(), parser.deserialize("", DECIMAL_LIST));
        assertEquals(Arrays.asList(null, new BigDecimal("1.2300")), parser.deserialize("[null,\"1.2300\"]", DECIMAL_LIST));
        assertEquals(Arrays.asList(null, new BigDecimal("1.2300")), parser.deserialize(new StringReader("[null,\"1.2300\"]"), DECIMAL_LIST));
        assertNull(parser.deserialize("{\"value\":null}", DecimalBean.class).getValue());
    }
}
