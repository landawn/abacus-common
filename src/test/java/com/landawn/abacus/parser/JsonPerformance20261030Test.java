package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.AbstractType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Sheet;

@Tag("unit")
class JsonPerformance20261030Test extends TestBase {
    public static final class Bean {
        public int keep;
    }

    private static final class CountingList extends ArrayList<Object> {
        private int reads;

        CountingList(final Collection<?> values) {
            super(values);
        }

        @Override
        public Object get(final int index) {
            reads++;
            return super.get(index);
        }
    }

    private static final class CountingSheetParser extends JsonParserImpl {
        private CountingList keys;

        @SuppressWarnings("unchecked")
        @Override
        protected <T> T readCollection(final JsonReader reader, final JsonDeserConfig config, final Type<?> propType,
                final BiConsumer<? super Collection<Object>, ?> handler, final boolean first, final Class<? extends T> targetClass,
                final Type<? extends T> targetType, final Collection<Object> output) throws IOException {
            final T result = super.readCollection(reader, config, propType, handler, first, targetClass, targetType, output);
            if (config.getElementType() != null && config.getElementType().javaType().equals(Integer.class)) {
                keys = new CountingList((Collection<?>) result);
                return (T) keys;
            }
            return result;
        }
    }

    @Test
    void wideIntegerSheetUsesLinearColumnKeyLookups() {
        final int count = 400;
        final StringBuilder keys = new StringBuilder();
        final StringBuilder columns = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                keys.append(',');
                columns.append(',');
            }
            keys.append(i);
            columns.append('"').append(count - i - 1).append("\":[").append(count - i - 1).append(']');
        }
        final String json = "{rowKeySet:['r'],columnKeySet:[" + keys + "],columns:{" + columns + "}}";
        for (final boolean readerInput : new boolean[] { false, true }) {
            final CountingSheetParser parser = new CountingSheetParser();
            final Type<Sheet<String, Integer, Object>> type = Type.of("Sheet<String,Integer,Object>");
            final Sheet<String, Integer, Object> sheet = readerInput ? parser.deserialize(new StringReader(json), type) : parser.deserialize(json, type);
            assertEquals(count, sheet.columnCount());
            assertTrue(parser.keys.reads <= count * 2, "Column matching must not scan the key list once per column: " + parser.keys.reads);
            for (int i = 0; i < count; i++) {
                assertEquals(i, sheet.get("r", i));
            }
        }
    }

    @Test
    void numericSheetKeysKeepEqualityAndRepeatedColumnRules() {
        final JsonParser parser = new JsonParserImpl();
        for (final String keyType : List.of("Byte", "Short", "Integer", "Long", "Float", "Double")) {
            final Type<Sheet<String, Object, Integer>> type = Type.of("Sheet<String," + keyType + ",Integer>");
            final Sheet<String, Object, Integer> sheet = parser.deserialize("{rowKeySet:['r'],columnKeySet:[1,2],columns:{'2':[20],'1':[10],'1':[11]}}", type);
            assertEquals(11, sheet.get("r", Type.of(keyType).valueOf("1")));
            assertEquals(20, sheet.get("r", Type.of(keyType).valueOf("2")));
            assertThrows(RuntimeException.class, () -> parser.deserialize("{rowKeySet:['r'],columnKeySet:[1],columns:{'2':[20]}}", type));
        }
        final Sheet<String, Double, Integer> doubles = parser
                .deserialize("{rowKeySet:['r'],columnKeySet:[-0.0,0.0,NaN],columns:{'NaN':[3],'0.0':[2],'-0.0':[1]}}", Type.of("Sheet<String,Double,Integer>"));
        assertEquals(1, doubles.get("r", -0.0));
        assertEquals(2, doubles.get("r", 0.0));
        assertEquals(3, doubles.get("r", Double.NaN));

        final Sheet<String, BigDecimal, Integer> decimals = parser.deserialize("{rowKeySet:['r'],columnKeySet:[1.0,1.00],columns:{'1.00':[20],'1.0':[10]}}",
                Type.of("Sheet<String,BigDecimal,Integer>"));
        assertEquals(10, decimals.get("r", new BigDecimal("1.0")));
        assertEquals(20, decimals.get("r", new BigDecimal("1.00")));

        final Sheet<String, int[], Integer> arrays = parser.deserialize("{rowKeySet:['r'],columnKeySet:[[1],[1]],columns:{'[1]':[10],'[1]':[20]}}",
                Type.of("Sheet<String,int[],Integer>"));
        final List<int[]> arrayKeys = new ArrayList<>(arrays.columnKeySet());
        assertNotSame(arrayKeys.get(0), arrayKeys.get(1));
        assertEquals(10, arrays.get("r", arrayKeys.get(0)));
        assertEquals(20, arrays.get("r", arrayKeys.get(1)));
    }

    @Test
    void discardedQuotedValuesDoNotAllocateTheirStringPayloads() {
        final var bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        final var allocations = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(allocations.isThreadAllocatedMemorySupported());
        allocations.setThreadAllocatedMemoryEnabled(true);
        final JsonParser parser = new JsonParserImpl();
        final int count = 5000;
        final String value = '"' + "x".repeat(128) + '"';
        for (final String subtree : List.of("[" + (value + ',').repeat(count - 1) + value + "]",
                "{" + ("\"a\":" + value + ',').repeat(count - 1) + "\"a\":" + value + "}")) {
            final String json = "{\"discard\":" + subtree + ",\"keep\":9}";
            for (int i = 0; i < 8; i++) {
                assertEquals(9, parser.deserialize(new StringReader(json), Bean.class).keep);
            }
            final long start = allocations.getThreadAllocatedBytes(Thread.currentThread().threadId());
            final Bean result = parser.deserialize(new StringReader(json), Bean.class);
            final long allocated = allocations.getThreadAllocatedBytes(Thread.currentThread().threadId()) - start;
            assertEquals(9, result.keep);
            // Allows key Strings for discarded maps and routine parsing overhead, but excludes 5000 value Strings.
            assertTrue(allocated < count * 80L + 100_000, "Discarded payload allocated " + allocated + " bytes");
        }
    }

    @Test
    void discardedQuotedValuesPreserveSyntaxAndTypedFailures() {
        final JsonParser parser = new JsonParserImpl();
        final JsonParser reference = new JsonParserImpl() {
        };
        final String slash = "\\";
        for (final String subtree : List.of("[\"ok\",\"\",\"null\"]", "{\"a\":\"ok\",\"a\":\"\"}", "[\"" + slash + "u0041\"]", "[\"" + slash + "u00XZ\"]",
                "[\"" + slash + "u00\"]", "[\"unterminated]", "[\"a\" \"b\"]", "[\"a\"}", "[1.5,null,123456789012345678901234567890]")) {
            final String json = "{\"discard\":" + subtree + ",\"keep\":9}";
            for (final boolean readerInput : new boolean[] { false, true }) {
                assertEquals(outcome(reference, json, readerInput), outcome(parser, json, readerInput), subtree);
            }
            assertEquals(validationOutcome(reference, subtree), validationOutcome(parser, subtree), subtree);
        }
        final JsonParser typedArray = new JsonParserImpl(null, JsonDeserConfig.create().setElementType(Integer.class));
        assertThrows(RuntimeException.class, () -> typedArray.deserialize("{discard:['not-an-int']}", Bean.class));
        final JsonParser typedMap = new JsonParserImpl(null, JsonDeserConfig.create().setValueType("number", Integer.class));
        assertThrows(RuntimeException.class, () -> typedMap.deserialize("{discard:{number:'not-an-int'}}", Bean.class));
        assertEquals(9, new JsonParserImpl(null, JsonDeserConfig.create().setIgnoreNullOrEmpty(true)).deserialize("{discard:['', 'null', null, 'text'],keep:9}",
                Bean.class).keep);
    }

    @Test
    void parserReaderAndTypeConversionHooksRemainObservable() throws IOException {
        final AtomicInteger parserCalls = new AtomicInteger();
        final JsonParser parser = new JsonParserImpl() {
            @Override
            protected Object readValue(final JsonReader reader, final boolean nullToEmpty, final Type<?> type) {
                if (type.isObject() && (reader.lastToken() == JsonReader.START_SINGLE_QUOTE || reader.lastToken() == JsonReader.START_DOUBLE_QUOTE)) {
                    parserCalls.incrementAndGet();
                }
                return super.readValue(reader, nullToEmpty, type);
            }
        };
        assertEquals(9, parser.deserialize("{discard:['a','','null'],keep:9}", Bean.class).keep);
        assertEquals(3, parserCalls.get());

        final String json = "{discard:['a','','null'],keep:9}";
        final AtomicInteger readerCalls = new AtomicInteger();
        final JsonReader reader = new JsonStringReader(json, new char[64]) {
            @Override
            public <T> T readValue(final Type<? extends T> type) {
                if (type.isObject()) {
                    readerCalls.incrementAndGet();
                }
                return super.readValue(type);
            }
        };
        final Bean result = new JsonParserImpl().parse(json, reader, new JsonDeserConfig(), Type.of(Bean.class), null);
        assertEquals(9, result.keep);
        assertEquals(3, readerCalls.get());

        final AtomicInteger conversions = new AtomicInteger();
        final Type<Object> custom = new AbstractType<>("JsonPerformance20261030Object") {
            @Override
            public Class<Object> javaType() {
                return Object.class;
            }

            @Override
            public boolean isObject() {
                return true;
            }

            @Override
            public String stringOf(final Object value) {
                return String.valueOf(value);
            }

            @Override
            public Object valueOf(final String text) {
                conversions.incrementAndGet();
                return text;
            }
        };
        new JsonParserImpl(null, JsonDeserConfig.create().setValueType("value", custom)).deserialize("{discard:{value:'text'}}", Bean.class);
        assertEquals(1, conversions.get());
    }

    public static final class CapturingMap extends HashMap<Object, Object> {
        static final List<Object> insertedValues = new ArrayList<>();

        @Override
        public Object put(final Object key, final Object value) {
            insertedValues.add(value);
            return super.put(key, value);
        }
    }

    @Test
    void discardedValuesStillReachHandlersAndCustomMapFactories() {
        final AtomicReference<Collection<Object>> captured = new AtomicReference<>();
        final JsonDeserConfig handled = JsonDeserConfig.create().setPropHandler("discard", (Collection<Object> values, Object value) -> {
            values.add(value);
            captured.set(values);
        });
        new JsonParserImpl().deserialize("{discard:['a','','null']}", handled, Bean.class);
        assertEquals(List.of("a", "", "null"), new ArrayList<>(captured.get()));

        CapturingMap.insertedValues.clear();
        final AtomicInteger factoryCalls = new AtomicInteger();
        final JsonDeserConfig customConfig = new JsonDeserConfig() {
            @Override
            public Class<? extends Map> getMapInstanceType() {
                factoryCalls.incrementAndGet();
                return CapturingMap.class;
            }
        };
        new JsonParserImpl(null, customConfig).deserialize("{discard:{a:'text',b:''}}", Bean.class);
        assertEquals(List.of("text", ""), CapturingMap.insertedValues);
        assertEquals(1, factoryCalls.get());

        CapturingMap.insertedValues.clear();
        new JsonParserImpl(null, JsonDeserConfig.create().setMapInstanceType(CapturingMap.class)).deserialize("{discard:{a:'text',b:''}}", Bean.class);
        assertEquals(List.of("text", ""), CapturingMap.insertedValues);
    }

    private static Object outcome(final JsonParser parser, final String json, final boolean readerInput) {
        try {
            return (readerInput ? parser.deserialize(new StringReader(json), Bean.class) : parser.deserialize(json, Bean.class)).keep;
        } catch (final RuntimeException e) {
            return e.getClass();
        }
    }

    private static Object validationOutcome(final JsonParser parser, final String json) {
        try {
            parser.validate(json);
            return "valid";
        } catch (final RuntimeException e) {
            return e.getClass();
        }
    }
}
