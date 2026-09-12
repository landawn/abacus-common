package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;

public class ParserNumberFormatTest extends TestBase {
    private final JsonParser parser = ParserFactory.createJsonParser();

    public static class Grouped {
        @JsonXmlField(numberFormat = "#,##0.00")
        private Double amount;

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }
    }

    public static class Affixes {
        @JsonXmlField(numberFormat = "0.00%")
        private Double percent;
        @JsonXmlField(numberFormat = "'USD '0.00")
        private Double currency;
        @JsonXmlField(numberFormat = "'\"\u540d\ud83d\ude00\\\n'0.00")
        private Double literal;

        public Double getPercent() {
            return percent;
        }

        public void setPercent(Double percent) {
            this.percent = percent;
        }

        public Double getCurrency() {
            return currency;
        }

        public void setCurrency(Double currency) {
            this.currency = currency;
        }

        public Double getLiteral() {
            return literal;
        }

        public void setLiteral(Double literal) {
            this.literal = literal;
        }
    }

    public static class NumericPatterns {
        @JsonXmlField(numberFormat = "0.00E0")
        private Double scientific;
        @JsonXmlField(numberFormat = "000")
        private Integer padded;

        public Double getScientific() {
            return scientific;
        }

        public void setScientific(Double scientific) {
            this.scientific = scientific;
        }

        public Integer getPadded() {
            return padded;
        }

        public void setPadded(Integer padded) {
            this.padded = padded;
        }
    }

    @Test
    public void groupingBoundariesProduceNumbersOrQuotedStringsAndRoundTrip() {
        for (double amount : new double[] { 0, 999.5, 1000, 1234.5, -1234.5, 1234567.89 }) {
            Grouped bean = new Grouped();
            bean.setAmount(amount);
            String json = parser.serialize(bean);
            Map<?, ?> values = parser.deserialize(json, Map.class);
            assertEquals(Math.abs(amount) >= 1000, values.get("amount") instanceof String, json);
            assertEquals(amount, parser.deserialize(json, Grouped.class).getAmount(), json);
            assertEquals(amount, parser.deserialize(new StringReader(json), Grouped.class).getAmount(), json);
            StringWriter writer = new StringWriter();
            parser.serialize(bean, null, writer);
            assertEquals(json, writer.toString());
        }
    }

    @Test
    public void percentCurrencyAndUnicodeLiteralsAreQuotedAndEscaped() {
        Affixes bean = new Affixes();
        bean.setPercent(0.125);
        bean.setCurrency(-1234.5);
        bean.setLiteral(2.5);
        String json = parser.serialize(bean);
        Map<?, ?> values = parser.deserialize(json, Map.class);
        assertEquals("12.50%", values.get("percent"));
        assertEquals("-USD 1234.50", values.get("currency"));
        assertEquals("\"\u540d\ud83d\ude00\\\n2.50", values.get("literal"));
        assertTrue(json.contains("\\n"), json);
        assertTrue(json.contains("\\\""), json);
        Affixes restored = parser.deserialize(json, Affixes.class);
        assertEquals(bean.getPercent(), restored.getPercent());
        assertEquals(bean.getCurrency(), restored.getCurrency());
        assertEquals(bean.getLiteral(), restored.getLiteral());
    }

    @Test
    public void scientificNotationRemainsNumericAndLeadingZeroesAreQuoted() {
        NumericPatterns bean = new NumericPatterns();
        bean.setScientific(-1250.0);
        bean.setPadded(1);
        String json = parser.serialize(bean);
        Map<?, ?> values = parser.deserialize(json, Map.class);
        assertTrue(values.get("scientific") instanceof Number, json);
        assertEquals("001", values.get("padded"));
        NumericPatterns restored = parser.deserialize(json, NumericPatterns.class);
        assertEquals(bean.getScientific(), restored.getScientific());
        assertEquals(bean.getPadded(), restored.getPadded());
    }

    @Test
    public void nullAndNonFiniteFormattedValuesRemainReadable() {
        Grouped bean = new Grouped();
        assertNull(parser.deserialize(parser.serialize(bean), Grouped.class).getAmount());
        for (double value : new double[] { Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -0.0 }) {
            bean.setAmount(value);
            String json = parser.serialize(bean);
            assertEquals(value, parser.deserialize(json, Grouped.class).getAmount(), json);
        }
    }

    @Test
    public void xmlKeepsFormattedTextAndJsonRespectsConfiguredQuotation() {
        Grouped bean = new Grouped();
        bean.setAmount(1234.5);
        for (XmlParser xmlParser : List.of(ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser())) {
            String xml = xmlParser.serialize(bean);
            assertTrue(xml.contains(">1,234.50<"), xml);
            assertEquals(bean.getAmount(), xmlParser.deserialize(xml, Grouped.class).getAmount());
        }
        String json = parser.serialize(bean, JsonSerConfig.create().setStringQuotation('\''));
        assertTrue(json.contains("'1,234.50'"), json);
        assertEquals(bean.getAmount(), parser.deserialize(json, Grouped.class).getAmount());
    }

    public static class Exact {
        @JsonXmlField(numberFormat = "#,##0.00")
        private java.math.BigDecimal price;
        @JsonXmlField(numberFormat = "#,##0.000")
        private java.math.BigDecimal tiny;
        @JsonXmlField(numberFormat = "#,##0")
        private java.math.BigInteger big;
        @JsonXmlField(numberFormat = "#,##0")
        private long id;
        @JsonXmlField(numberFormat = "#,##0")
        private Long boxed;
        @JsonXmlField(numberFormat = "0.00")
        private int i;
        @JsonXmlField(numberFormat = "0.00")
        private short sh;
        @JsonXmlField(numberFormat = "0.00")
        private byte bt;
        @JsonXmlField(numberFormat = "0.00")
        private float f;
        @JsonXmlField(numberFormat = "0.00")
        private Number n;
        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
        public java.math.BigDecimal getTiny() { return tiny; }
        public void setTiny(java.math.BigDecimal tiny) { this.tiny = tiny; }
        public java.math.BigInteger getBig() { return big; }
        public void setBig(java.math.BigInteger big) { this.big = big; }
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public Long getBoxed() { return boxed; }
        public void setBoxed(Long boxed) { this.boxed = boxed; }
        public int getI() { return i; }
        public void setI(int i) { this.i = i; }
        public short getSh() { return sh; }
        public void setSh(short sh) { this.sh = sh; }
        public byte getBt() { return bt; }
        public void setBt(byte bt) { this.bt = bt; }
        public float getF() { return f; }
        public void setF(float f) { this.f = f; }
        public Number getN() { return n; }
        public void setN(Number n) { this.n = n; }
    }

    @Test
    public void reviewFixes20260906_bigDecimalAndBigIntegerRoundTripExactlyOnJsonAndXml() {
        // 12345678901234567.89 has more digits than a double carries; the 30-digit BigInteger and 2^70 + 1 are
        // NOT powers of two (2^70 itself is exactly representable as a double and would hide the bug).
        for (Exact bean : new Exact[] { exact("12345678901234567.89", "123456789012345678901234567890", Long.MAX_VALUE),
                exact("-98765432109876543.21", "1180591620717411303425", Long.MIN_VALUE) }) {
            String json = parser.serialize(bean);
            assertTrue(json.contains("\"12,345,678,901,234,567.89\"") || json.contains("\"-98,765,432,109,876,543.21\""), json);
            assertExactEquals(bean, parser.deserialize(json, Exact.class), json);
            assertExactEquals(bean, parser.deserialize(new StringReader(json), Exact.class), json);

            for (XmlParser xmlParser : List.of(ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser())) {
                String xml = xmlParser.serialize(bean);
                assertExactEquals(bean, xmlParser.deserialize(xml, Exact.class), xml);
            }
        }
    }

    @Test
    public void reviewFixes20260906_bigDecimalScaleFollowsThePattern() {
        // 0.1 written under #,##0.00 is "0.10" on the wire; the faithful reading of that text is 0.10 (scale 2):
        // numerically equal to the input, not equals()-equal. Compare with compareTo, and pin the read-back scale.
        Exact bean = exact("0.1", "1", 1);
        String json = parser.serialize(bean);
        assertTrue(json.contains("\"price\": 0.10"), json);
        java.math.BigDecimal price = parser.deserialize(json, Exact.class).getPrice();
        assertEquals(0, bean.getPrice().compareTo(price), json);
        assertEquals(new java.math.BigDecimal("0.10"), price, json);
    }

    @Test
    public void reviewFixes20260906_readPropValueReturnsThePropertyType() {
        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(Exact.class);
        assertEquals(new java.math.BigDecimal("1234.50"), beanInfo.getPropInfo("price").readPropValue("1,234.50"));
        assertEquals(new java.math.BigInteger("1180591620717411303425"), beanInfo.getPropInfo("big").readPropValue("1,180,591,620,717,411,303,425"));
        assertEquals(Long.valueOf(Long.MAX_VALUE), beanInfo.getPropInfo("id").readPropValue("9,223,372,036,854,775,807"));
        assertEquals(Long.valueOf(5), beanInfo.getPropInfo("boxed").readPropValue("5"));
        assertEquals(Integer.valueOf(7), beanInfo.getPropInfo("i").readPropValue("7.00"));
        assertEquals(Short.valueOf((short) 9), beanInfo.getPropInfo("sh").readPropValue("9.00"));
        assertEquals(Byte.valueOf((byte) 3), beanInfo.getPropInfo("bt").readPropValue("3.00"));
        assertEquals(Float.valueOf(2.5f), beanInfo.getPropInfo("f").readPropValue("2.50"));
        assertEquals(Long.valueOf(1234), beanInfo.getPropInfo("n").readPropValue("1234.00"));
        assertNull(beanInfo.getPropInfo("price").readPropValue(null));
        assertNull(beanInfo.getPropInfo("i").readPropValue(null));

        // The Integer "000" pattern from the earlier review now yields an Integer directly, not a Long.
        Object padded = ParserUtil.getBeanInfo(NumericPatterns.class).getPropInfo("padded").readPropValue("001");
        assertEquals(Integer.class, padded.getClass());
        assertEquals(1, padded);

        // Floating targets stay on DecimalFormat's default parse mode, so the sign of -0.0 survives.
        Object negativeZero = ParserUtil.getBeanInfo(Grouped.class).getPropInfo("amount").readPropValue("-0.00");
        assertEquals(Double.class, negativeZero.getClass());
        assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits((Double) negativeZero));

        // Non-finite text cannot become a BigDecimal; empty text is still rejected by the format (pinned as-is).
        org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> beanInfo.getPropInfo("price").readPropValue("NaN"));
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> beanInfo.getPropInfo("price").readPropValue(""));
    }

    @Test
    public void reviewFixes20260906_formattedReadsNeverTakeTheSetPropValueRetryPath() {
        Exact bean = exact("12345678901234567.89", "123456789012345678901234567890", 42);
        String json = parser.serialize(bean);
        NumericPatterns patterns = new NumericPatterns();
        patterns.setPadded(1);
        patterns.setScientific(-1250.0);
        String patternsJson = parser.serialize(patterns);

        for (int k = 0; k < 200; k++) {
            assertExactEquals(bean, parser.deserialize(json, Exact.class), json);
            assertEquals(patterns.getPadded(), parser.deserialize(patternsJson, NumericPatterns.class).getPadded());
        }

        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(Exact.class);
        for (String prop : new String[] { "price", "tiny", "big", "id", "boxed", "i", "sh", "bt", "f", "n" }) {
            assertEquals(0, beanInfo.getPropInfo(prop).failureCountForSetProp, prop);
        }
        assertEquals(0, ParserUtil.getBeanInfo(NumericPatterns.class).getPropInfo("padded").failureCountForSetProp);
    }

    private static Exact exact(String price, String big, long id) {
        Exact bean = new Exact();
        bean.setPrice(new java.math.BigDecimal(price));
        bean.setTiny(new java.math.BigDecimal("-0.005"));
        bean.setBig(new java.math.BigInteger(big));
        bean.setId(id);
        bean.setBoxed(id);
        bean.setI(7);
        bean.setSh((short) 9);
        bean.setBt((byte) 3);
        bean.setF(2.5f);
        bean.setN(1234);
        return bean;
    }

    private static void assertExactEquals(Exact expected, Exact actual, String doc) {
        assertEquals(expected.getPrice(), actual.getPrice(), doc);
        assertEquals(expected.getTiny(), actual.getTiny(), doc);
        assertEquals(expected.getBig(), actual.getBig(), doc);
        assertEquals(expected.getId(), actual.getId(), doc);
        assertEquals(expected.getBoxed(), actual.getBoxed(), doc);
        assertEquals(expected.getI(), actual.getI(), doc);
        assertEquals(expected.getSh(), actual.getSh(), doc);
        assertEquals(expected.getBt(), actual.getBt(), doc);
        assertEquals(expected.getF(), actual.getF(), doc);
        // A Number-typed property keeps DecimalFormat's default parse result (Long for integral text).
        assertEquals(Long.valueOf(1234), actual.getN(), doc);
    }

    // G08-107: an incomplete numberFormat parse was reported as a bare java.lang.RuntimeException, so a caller
    // catching the parser's own failure type could not catch it alongside every other parse failure.
    @Test
    public void fixG08_F107_incompleteNumberFormatParseThrowsParsingException() {
        for (String text : new String[] { "12abc", "abc", "1,234.50x", "-" }) {
            com.landawn.abacus.exception.ParsingException e = org.junit.jupiter.api.Assertions.assertThrows(
                    com.landawn.abacus.exception.ParsingException.class, () -> parser.deserialize("{\"amount\":\"" + text + "\"}", Grouped.class), text);
            assertTrue(e.getMessage().startsWith("Failed to parse complete number value: " + text), e.getMessage());
        }

        // a complete parse is unaffected
        assertEquals(Double.valueOf(1234.5), parser.deserialize("{\"amount\":\"1,234.50\"}", Grouped.class).getAmount());
    }

}
