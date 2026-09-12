package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.u.Nullable;

public class TuplePrecisionParsingTest extends TestBase {
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void everyTupleArityPreservesExactDecimals(int arity) {
        Type<Tuple> type = TypeFactory.getType("Tuple" + arity + "<" + String.join(",", Collections.nCopies(arity, "BigDecimal")) + ">");
        for (String token : new String[] { "0.123456789012345678901", "1.2300", "12345678901234567890.123456789", "1E-1000", "-0.0000" }) {
            BigDecimal expected = new BigDecimal(token);
            Tuple tuple = type.valueOf("[" + String.join(", ", Collections.nCopies(arity, token)) + "]");
            for (Object value : tuple.toArray()) {
                assertEquals(expected, value);
            }
            assertEquals(tuple, type.valueOf(type.stringOf(tuple)));
        }
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[]"));
    }

    @Test
    void pairTripleIndexedAndTimedPreserveTypedValues() {
        String token = "0.123456789012345678901";
        BigDecimal expected = new BigDecimal(token);
        Type<Pair<BigDecimal, BigInteger>> pairType = TypeFactory.getType("Pair<BigDecimal,BigInteger>");
        Pair<BigDecimal, BigInteger> pair = pairType.valueOf("[" + token + ",9223372036854775808]");
        assertEquals(expected, pair.left());
        assertEquals(new BigInteger("9223372036854775808"), pair.right());
        Type<Triple<BigDecimal, String, BigDecimal>> tripleType = TypeFactory.getType("Triple<BigDecimal,String,BigDecimal>");
        Triple<BigDecimal, String, BigDecimal> triple = tripleType.valueOf("[" + token + ",\"null\",1.2300]");
        assertEquals(expected, triple.left());
        assertEquals("null", triple.middle());
        assertEquals(new BigDecimal("1.2300"), triple.right());
        Type<Indexed<BigDecimal>> indexedType = TypeFactory.getType("Indexed<BigDecimal>");
        assertEquals(expected, indexedType.valueOf("[1," + token + "]").value());
        Type<Timed<BigDecimal>> timedType = TypeFactory.getType("Timed<BigDecimal>");
        assertEquals(expected, timedType.valueOf("[1," + token + "]").value());
    }

    @Test
    void nestedGenericsAndEscapedStringsUseOriginalTokens() {
        Type<Tuple1<List<Map<String, BigDecimal>>>> type = TypeFactory.getType("Tuple1<List<Map<String,BigDecimal>>>");
        Tuple1<List<Map<String, BigDecimal>>> result = type.valueOf("[[{\"x,]\\\"\":0.123456789012345678901,\"scale\":1.2300}]]");
        assertEquals(new BigDecimal("0.123456789012345678901"), result._1.get(0).get("x,]\""));
        assertEquals(new BigDecimal("1.2300"), result._1.get(0).get("scale"));
        Type<Pair<String, List<BigDecimal>>> pairType = TypeFactory.getType("Pair<String,List<BigDecimal>>");
        Pair<String, List<BigDecimal>> pair = pairType.valueOf(" [ \"a,]\\\"\\\\\u6C49\uD83D\uDE00\", [1.2300,null] ] ");
        assertEquals("a,]\"\\\u6C49\uD83D\uDE00", pair.left());
        assertEquals(new BigDecimal("1.2300"), pair.right().get(0));
        assertNull(pair.right().get(1));
        assertEquals(pair, pairType.valueOf(pairType.stringOf(pair)));
    }

    @Test
    void nestedTuplesKeepTheirTypesInsideCollectionsAndMaps() {
        Type<Tuple1<List<Pair<BigDecimal, String>>>> type = TypeFactory.getType("Tuple1<List<Pair<BigDecimal,String>>>");
        Tuple1<List<Pair<BigDecimal, String>>> result = type.valueOf("[[[1.2300,\"x\"]]]");
        assertEquals(new BigDecimal("1.2300"), result._1.get(0).left());
        assertEquals("x", result._1.get(0).right());
        assertEquals(result, type.valueOf(type.stringOf(result)));

        Type<Pair<Timed<BigDecimal>, Map<String, Indexed<BigDecimal>>>> metadata = TypeFactory
                .getType("Pair<Timed<BigDecimal>,Map<String,Indexed<BigDecimal>>>");
        Pair<Timed<BigDecimal>, Map<String, Indexed<BigDecimal>>> parsed = metadata
                .valueOf("[[-9223372036854775808,1.2300],{\"x\":[9223372036854775807,0.123456789012345678901]}]");
        assertEquals(Long.MIN_VALUE, parsed.left().timestamp());
        assertEquals(new BigDecimal("1.2300"), parsed.left().value());
        assertEquals(Long.MAX_VALUE, parsed.right().get("x").longIndex());
        assertEquals(new BigDecimal("0.123456789012345678901"), parsed.right().get("x").value());
        for (String invalid : new String[] { "[[[1.2]]]", "[[[1.2,\"x\",3]]]", "[[[1.2,]]]", "[[[,\"x\"]]]" }) {
            assertThrows(RuntimeException.class, () -> type.valueOf(invalid));
        }
    }

    @Test
    void nestedWrapperSlotsPreserveTheirRuntimeTypesAndPrecision() {
        for (String wrapper : new String[] { "java.util.Optional", "Optional", "Nullable", "Holder" }) {
            Type<Tuple1<Object>> scalar = TypeFactory.getType("Tuple1<" + wrapper + "<BigDecimal>>");
            Tuple1<Object> parsedScalar = scalar.valueOf("[1.2300]");
            assertEquals(new BigDecimal("1.2300"), unwrap(parsedScalar._1));
            assertEquals(parsedScalar, scalar.valueOf(scalar.stringOf(parsedScalar)));
            assertNull(scalar.valueOf("[null]")._1);
            Type<Tuple1<Object>> text = TypeFactory.getType("Tuple1<" + wrapper + "<String>>");
            assertEquals("null", unwrap(text.valueOf("[\"null\"]")._1));
        }
        Type<Triple<java.util.Optional<List<BigDecimal>>, Holder<Pair<BigDecimal, String>>, Nullable<Map<String, BigDecimal>>>> type = TypeFactory
                .getType("Triple<java.util.Optional<List<BigDecimal>>,Holder<Pair<BigDecimal,String>>,Nullable<Map<String,BigDecimal>>>");
        Triple<java.util.Optional<List<BigDecimal>>, Holder<Pair<BigDecimal, String>>, Nullable<Map<String, BigDecimal>>> result = type
                .valueOf("[[1.2300],[0.123456789012345678901,\"\\u6c49\"],{\"x\":1.2300}]");
        assertEquals(new BigDecimal("1.2300"), result.left().get().get(0));
        assertEquals(new BigDecimal("0.123456789012345678901"), result.middle().value().left());
        assertEquals("\u6c49", result.middle().value().right());
        assertEquals(new BigDecimal("1.2300"), result.right().get().get("x"));
        assertEquals(result, type.valueOf(type.stringOf(result)));
    }

    @Test
    void scalarSlotsDecodeJsonWithoutConfusingQuotedNullOrInheritedTypes() {
        Type<Pair<String, Object>> type = TypeFactory.getType("Pair<String,Object>");
        assertEquals(Pair.of("null", "null"), type.valueOf("[\"null\",\"null\"]"));
        assertEquals(Pair.of(null, null), type.valueOf("[null,null]"));
        assertEquals(Pair.of("", 42), type.valueOf("[\"\",42]"));
        Type<Tuple1<Object>> primitive = TypeFactory.getType("Tuple1<int>");
        assertNull(primitive.valueOf("[null]")._1);
        Type<Tuple1<List<Pair<String, Object>>>> nested = TypeFactory.getType("Tuple1<List<Pair<String,Object>>>");
        assertEquals(List.of(1, "x"), nested.valueOf("[[[\"key\",[1,\"x\"]]]]")._1.get(0).right());
        Type<Tuple1<Map<String, Pair<String, Object>>>> mapped = TypeFactory.getType("Tuple1<Map<String,Pair<String,Object>>>");
        assertEquals(Map.of("inner", List.of(1, "x")), mapped.valueOf("[{\"outer\":[\"key\",{\"inner\":[1,\"x\"]}]}]")._1.get("outer").right());
        Type<Tuple1<BigDecimal>> decimal = TypeFactory.getType("Tuple1<BigDecimal>");
        assertEquals(new BigDecimal("1.2300"), decimal.valueOf("[\"1.2300\"]")._1);
    }

    @Test
    void metadataMustFitAnIntegralLong() {
        Type<Indexed<String>> indexed = TypeFactory.getType("Indexed<String>");
        Type<Timed<String>> timed = TypeFactory.getType("Timed<String>");
        assertEquals(Long.MAX_VALUE, indexed.valueOf("[9223372036854775807,\"x\"]").longIndex());
        assertEquals(Long.MIN_VALUE, timed.valueOf("[-9223372036854775808,\"x\"]").timestamp());
        assertEquals(Long.MAX_VALUE, timed.valueOf("[9223372036854775807,\"x\"]").timestamp());
        assertEquals(0L, indexed.valueOf("[null,null]").index());
        assertEquals(0L, timed.valueOf("[null,null]").timestamp());
        for (String token : new String[] { "1.9", "9223372036854775808", "-9223372036854775809", "1e3", "1e1000" }) {
            assertThrows(RuntimeException.class, () -> indexed.valueOf("[" + token + ",\"x\"]"));
            assertThrows(RuntimeException.class, () -> timed.valueOf("[" + token + ",\"x\"]"));
        }
    }

    @Test
    void metadataRejectsNumericCoercionThroughDirectAndNestedReaders() {
        final JsonParser parser = ParserFactory.createJsonParser();
        for (final String name : List.of("Indexed", "Timed")) {
            final Type<?> type = TypeFactory.getType(name + "<BigDecimal>");
            final Type<?> listType = TypeFactory.getType("List<" + name + "<BigDecimal>>");
            for (final Function<String, Object> parse : List.<Function<String, Object>>of(type::valueOf,
                    json -> ((List<?>) parser.deserialize("[" + json + "]", listType)).get(0),
                    json -> ((List<?>) parser.deserialize(new StringReader("[" + json + "]"), listType)).get(0))) {
                // Metadata uses original integer text in every route; payload decimal scale stays intact.
                for (final String token : List.of("1.0", "1.9", "-1.9", "1e2", "1e1000", "bad")) {
                    assertThrows(NumberFormatException.class, () -> parse.apply("[" + token + ",1.2300]"), name + "/" + token);
                    assertThrows(NumberFormatException.class, () -> parse.apply("[\"" + token + "\",1.2300]"), name + "/quoted " + token);
                }
                for (final String token : List.of("9223372036854775808", "-9223372036854775809")) {
                    assertThrows(ArithmeticException.class, () -> parse.apply("[" + token + ",1.2300]"));
                    assertThrows(ArithmeticException.class, () -> parse.apply("[\"" + token + "\",1.2300]"));
                }
                for (final Map.Entry<String, Long> entry : Map.of("null", 0L, "0", 0L, "\"5\"", 5L, "\"\\u0035\"", 5L,
                        "9223372036854775807", Long.MAX_VALUE).entrySet()) {
                    final Object result = parse.apply("[" + entry.getKey() + ",1.2300]");
                    assertEquals(entry.getValue().longValue(), result instanceof Indexed<?> indexed ? indexed.longIndex() : ((Timed<?>) result).timestamp());
                    assertEquals(new BigDecimal("1.2300"), result instanceof Indexed<?> indexed ? indexed.value() : ((Timed<?>) result).value());
                }
                if (name.equals("Indexed")) {
                    assertThrows(IllegalArgumentException.class, () -> parse.apply("[-1,null]"));
                    assertThrows(IllegalArgumentException.class, () -> parse.apply("[-9223372036854775808,null]"));
                } else {
                    assertEquals(Long.MIN_VALUE, ((Timed<?>) parse.apply("[-9223372036854775808,null]")).timestamp());
                }
                assertThrows(RuntimeException.class, () -> parse.apply("[]"));
            }
            assertNull(type.valueOf((String) null));
            assertNull(type.valueOf(""));
            final Type<?> textType = TypeFactory.getType(name + "<String>");
            final Object text = textType.valueOf("[\"5\",\"\\u6c49\\ud83d\\ude42\"]");
            assertEquals("\u6c49\ud83d\ude42", text instanceof Indexed<?> indexed ? indexed.value() : ((Timed<?>) text).value());
        }
    }

    @Test
    void malformedArraysAreRejectedWithoutLosingSlotBoundaries() {
        Type<Pair<String, List<Integer>>> type = TypeFactory.getType("Pair<String,List<Integer>>");
        for (String invalid : new String[] { "null", "{}", "[]", "[1]", "[1,2,3]", "[,[]]", "[\"x\",]", "[\"x\",[1}]", "[\"x\",[1]", "[\"unterminated,[]]",
                "[\"x\",[]] trailing" }) {
            assertThrows(RuntimeException.class, () -> type.valueOf(invalid), invalid);
        }
        Pair<String, List<Integer>> empty = type.valueOf("[\"\",[]]");
        assertEquals("", empty.left());
        assertEquals(List.of(), empty.right());
        Pair<String, List<Integer>> nulls = type.valueOf("[null,null]");
        assertNull(nulls.left());
        assertNull(nulls.right());
    }

    @Test
    void malformedInputNamesTheCauseThatActuallyFailed() {
        Type<Pair<String, Integer>> pair = TypeFactory.getType("Pair<String,Integer>");
        assertMessage(pair, " ", "the value is blank");
        assertMessage(pair, "\"a\", 1", "not an array: the value must start with '[' and end with ']'");
        assertMessage(pair, "[\"a\", 1", "not an array: the value must start with '[' and end with ']'");
        assertMessage(pair, "[\"a\", 1] trailing", "not an array: the value must start with '[' and end with ']'");
        assertMessage(pair, "[{\"a\":1], 2]", "unbalanced brackets: ']' at index 7 does not close the enclosing '{'");
        assertMessage(pair, "[[1, 2], 3]]", "unbalanced brackets: ']' at index 10 has no matching '['");
        assertMessage(pair, "[[1, 2, 3]", "unbalanced brackets: unclosed '['");
        assertMessage(pair, "[\"a, 1]", "unterminated quoted value: no closing \"");
        assertMessage(pair, "['a, 1]", "unterminated quoted value: no closing '");
        assertMessage(pair, "[]", "expected exactly 2 elements but found 0");
        assertMessage(pair, "[  ]", "expected exactly 2 elements but found 0");
        assertMessage(pair, "[1]", "expected exactly 2 elements but found 1");
        assertMessage(pair, "[1,2,3]", "expected exactly 2 elements but found 3");
        assertMessage(pair, "[1,2,3,4,5]", "expected exactly 2 elements but found 5");
        assertMessage(pair, "[, 1]", "empty element at index 0");
        assertMessage(pair, "[1, ]", "empty element at index 1");

        // Arity one takes the singular noun, and an empty array is reported as zero elements, not one blank slot.
        Type<Tuple1<String>> single = TypeFactory.getType("Tuple1<String>");
        assertMessage(single, "[]", "expected exactly 1 element but found 0");
        assertMessage(single, "[ ]", "expected exactly 1 element but found 0");
        assertMessage(single, "[1,2]", "expected exactly 1 element but found 2");

        // One decoder serves all five handler families, so each of them reports the real cause too.
        for (String typeName : new String[] { "Pair<String,Integer>", "Triple<String,Integer,Double>", "Indexed<String>", "Timed<String>",
                "Tuple2<String,Integer>" }) {
            Type<?> type = TypeFactory.getType(typeName);
            assertMessage(type, "[\"a, 1]", "unterminated quoted value: no closing \"");
            assertMessage(type, "[[1, 2, 3]", "unbalanced brackets: unclosed '['");
            assertMessage(type, "[}, 1]", "unbalanced brackets: '}' at index 1 has no matching '{'");
            assertMessage(type, "[1,2,3,4]", "expected exactly " + (typeName.startsWith("Triple") ? "3 elements" : "2 elements") + " but found 4");
        }
    }

    private static void assertMessage(Type<?> type, String input, String reason) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> type.valueOf(input), input);
        assertEquals("Invalid " + type.name() + " format: " + reason, e.getMessage(), input);
    }

    private static Object unwrap(Object value) {
        if (value instanceof java.util.Optional<?> optional) {
            return optional.get();
        } else if (value instanceof com.landawn.abacus.util.u.Optional<?> optional) {
            return optional.get();
        } else if (value instanceof Nullable<?> nullable) {
            return nullable.get();
        } else {
            return ((Holder<?>) value).value();
        }
    }
}
