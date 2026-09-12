package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.util.Range;

public class RangeGenericEndpointTest extends TestBase {
    @Test
    public void genericDecimalsRetainTypeScaleAndNumericOrdering() {
        Type<Range<Box<BigDecimal>>> type = rangeType("BigDecimal");
        for (String[] bounds : new String[][] { { "[", "]" }, { "(", "]" }, { "[", ")" }, { "(", ")" } }) {
            String text = bounds[0] + "2.000, 10.000" + bounds[1];
            Range<Box<BigDecimal>> range = type.valueOf(text);
            assertEquals(new BigDecimal("2.000"), range.lowerEndpoint().value);
            assertEquals(new BigDecimal("10.000"), range.upperEndpoint().value);
            assertEquals(text, type.stringOf(range));
            Range<Box<BigDecimal>> again = type.valueOf(type.stringOf(range));
            assertEquals(range.boundType(), again.boundType());
            assertEquals(range.upperEndpoint().value, again.upperEndpoint().value);
        }
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("[10, 2]"));
    }

    @Test
    public void genericEndpointsPreserveExtremeNumbersAndUnicode() {
        Type<Range<Box<BigDecimal>>> decimals = rangeType("BigDecimal");
        Range<Box<BigDecimal>> range = decimals.valueOf("[1E-1000, 123456789012345678901234567890.0000]");
        assertEquals(new BigDecimal("1E-1000"), range.lowerEndpoint().value);
        assertEquals(new BigDecimal("123456789012345678901234567890.0000"), range.upperEndpoint().value);
        assertEquals(range.upperEndpoint().value, decimals.valueOf(decimals.stringOf(range)).upperEndpoint().value);
        Type<Range<Box<Long>>> longs = rangeType("Long");
        Range<Box<Long>> longRange = longs.valueOf("[" + Long.MIN_VALUE + "," + Long.MAX_VALUE + "]");
        assertEquals(Long.valueOf(Long.MIN_VALUE), longRange.lowerEndpoint().value);
        assertEquals(Long.valueOf(Long.MAX_VALUE), longRange.upperEndpoint().value);
        Type<Range<Box<String>>> strings = rangeType("String");
        for (String text : new String[] { "", "\u00E9\uD83D\uDE00", "\uD800x\uDC00\u0000\uFFFF", "\"'[,]\\" }) {
            Range<Box<String>> original = Range.closed(Box.of(text), Box.of(text));
            Range<Box<String>> parsed = strings.valueOf(strings.stringOf(original));
            assertEquals(text, parsed.lowerEndpoint().value);
            assertEquals(text, parsed.upperEndpoint().value);
        }
    }

    @Test
    public void nestedGenericBeanEndpointsRetainElementTypes() {
        Type<Range<NestedEndpoint<BigDecimal>>> type = TypeFactory.getType("Range<" + NestedEndpoint.class.getName() + "<BigDecimal>>");
        NestedEndpoint<BigDecimal> endpoint = new NestedEndpoint<>();
        endpoint.setValues(List.of(new BigDecimal("1.2300"), new BigDecimal("1E-1000")));
        Range<NestedEndpoint<BigDecimal>> parsed = type.valueOf(type.stringOf(Range.closed(endpoint, endpoint)));
        assertEquals(endpoint.getValues(), parsed.lowerEndpoint().getValues());
        assertEquals(endpoint.getValues(), parsed.upperEndpoint().getValues());
    }

    @Test
    public void nullArityAndBoundaryValidationRemainIntact() {
        Type<Range<Box<BigDecimal>>> type = rangeType("BigDecimal");
        assertNull(type.stringOf(null));
        for (String text : new String[] { null, "", "  \t\r\n" }) {
            assertNull(type.valueOf(text));
        }
        for (String text : new String[] { "[]", "[1]", "[1,2,3]", "[null,2]", "[1,null]", "{1,2]", "[1,2}", "[1,,2]" }) {
            assertThrows(RuntimeException.class, () -> type.valueOf(text), text);
        }
        assertEquals(new BigDecimal("1.00"), type.valueOf("[1.00,1.00]").lowerEndpoint().value);
    }

    @Test
    public void rawPrimitiveAndNestedParserPathsRemainUsable() {
        for (String name : new String[] { "Range", "Range<int>", "Range<Integer>" }) {
            Type<Range<Integer>> type = TypeFactory.getType(name);
            Range<Integer> parsed = type.valueOf("[2,10]");
            assertEquals(Integer.valueOf(2), parsed.lowerEndpoint());
            assertEquals(Integer.valueOf(10), parsed.upperEndpoint());
            assertEquals("[2, 10]", type.stringOf(parsed));
            assertThrows(RuntimeException.class, () -> type.valueOf("[null,10]"));
        }
        Type<List<Range<Box<BigDecimal>>>> listType = TypeFactory.getType("List<Range<" + Box.class.getName() + "<BigDecimal>>>");
        List<Range<Box<BigDecimal>>> parsed = Utils.jsonParser.deserialize("[\"[2.000, 10.000]\"]", listType);
        assertEquals(new BigDecimal("10.000"), parsed.get(0).upperEndpoint().value);
    }

    private static <T> Type<Range<Box<T>>> rangeType(String elementName) {
        return TypeFactory.getType("Range<" + Box.class.getName() + "<" + elementName + ">>");
    }

    public static class Box<T> implements Comparable<Box<T>> {
        @JsonXmlValue
        public final T value;

        private Box(T value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static <U> Box<U> of(U value) {
            return new Box<>(value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(Box<T> other) {
            return ((Comparable<T>) value).compareTo(other.value);
        }
    }

    public static class NestedEndpoint<T> implements Comparable<NestedEndpoint<T>> {
        private List<T> values;

        public NestedEndpoint() {
        }

        public List<T> getValues() {
            return values;
        }

        public void setValues(List<T> values) {
            this.values = values;
        }

        @Override
        public int compareTo(NestedEndpoint<T> other) {
            return Integer.compare(values.size(), other.values.size());
        }
    }
}
