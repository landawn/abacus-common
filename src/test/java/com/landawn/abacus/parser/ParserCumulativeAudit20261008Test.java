package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.ImmutableList;

@Tag("unit")
class ParserCumulativeAudit20261008Test extends TestBase {
    @Test
    void compactXmlKeepsAlreadyTypedCopyOnWriteAndImmutableListValues() {
        final List<XmlParser> parsers = List.of(new XmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM),
                new AbacusXmlParserImpl(XmlParserType.StAX), new AbacusXmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.SAX));
        for (final Class<?> listClass : List.of(CopyOnWriteArrayList.class, ImmutableList.class)) {
            final Type<Map<String, List<Object>>> type = Type.of("Map<String," + listClass.getCanonicalName() + "<Object>>");
            for (final XmlParser parser : parsers) {
                final Map<String, List<Object>> result = parser.deserialize("<map>{\"k\":[1000,null,2000]}</map>",
                        XmlDeserConfig.create().setElementType(Long.class), type);
                final List<Object> values = result.get("k");
                assertInstanceOf(listClass, values);
                assertEquals(java.util.Arrays.asList(1000L, null, 2000L), values);
                assertInstanceOf(Long.class, values.getFirst());
            }
        }
    }

    @Test
    void primitiveComponentsKeepTheirBoxedValuesInCopyOnWriteAndImmutableLists() {
        final List<ConfiguredElements> cases = List.of(new ConfiguredElements(long.class, "[1000,null,2000]", List.of(1000L, 0L, 2000L)),
                new ConfiguredElements(int.class, "[1000,null,2000]", List.of(1000, 0, 2000)),
                new ConfiguredElements(short.class, "[1000,null,2000]", List.of((short) 1000, (short) 0, (short) 2000)),
                new ConfiguredElements(byte.class, "[10,null,20]", List.of((byte) 10, (byte) 0, (byte) 20)),
                new ConfiguredElements(float.class, "[-0.0,null,2000.25]", List.of(-0.0f, 0.0f, 2000.25f)),
                new ConfiguredElements(double.class, "[-0.0,null,2000.25]", List.of(-0.0d, 0.0d, 2000.25d)),
                new ConfiguredElements(char.class, "[\"\u0100\",null,\"\u0200\"]", List.of('\u0100', '\0', '\u0200')),
                new ConfiguredElements(boolean.class, "[true,null,false]", List.of(true, false, false)));
        for (final XmlParser parser : List.of(new XmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM),
                new AbacusXmlParserImpl(XmlParserType.StAX), new AbacusXmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.SAX))) {
            for (final Class<?> listClass : List.of(CopyOnWriteArrayList.class, ImmutableList.class)) {
                final Type<Map<String, List<Object>>> type = Type.of("Map<String," + listClass.getCanonicalName() + "<Object>>");
                for (final ConfiguredElements testCase : cases) {
                    final Map<String, List<Object>> result = parser.deserialize("<map>{\"k\":" + testCase.json() + "}</map>",
                            XmlDeserConfig.create().setElementType(testCase.component()), type);
                    final List<Object> values = result.get("k");
                    assertInstanceOf(listClass, values);
                    assertEquals(testCase.values(), values, testCase.component().getName());
                    if (testCase.component() == float.class) {
                        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits((Float) values.getFirst()));
                    } else if (testCase.component() == double.class) {
                        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits((Double) values.getFirst()));
                    }
                }
            }
        }
    }

    private record ConfiguredElements(Class<?> component, String json, List<?> values) {
    }

    @Test
    void ordinaryJsonResolvesEachConfiguredMapValueOnlyOnce() {
        final CountingConfig config = new CountingConfig();
        config.setValueType("items", Type.of("List<Integer>"));
        final Map<String, Object> result = new JsonParserImpl().deserialize("{\"items\":[1,2]}", config, Type.of("Map<String,Object>"));
        assertEquals(List.of(1, 2), result.get("items"));
        assertEquals(1, config.valueTypeCalls.get(), "the XML-only array hook must not query an ordinary JSON key again");
    }

    @Test
    void ordinaryJsonDoesNotReadUnusedGlobalTypesForDeclaredNestedMapArguments() {
        final CountingConfig config = new CountingConfig();
        final Map<String, Map<String, Integer>> result = new JsonParserImpl().deserialize("{\"nested\":{\"x\":1}}", config,
                Type.of("Map<String,Map<String,Integer>>"));
        assertEquals(Map.of("nested", Map.of("x", 1)), result);
        assertEquals(0, config.mapKeyTypeCalls.get());
        assertEquals(0, config.mapValueTypeCalls.get());
    }

    @Test
    void reentrantXmlReadsUseTheirOwnElementConfigurationAndRestoreTheEnclosingRead() {
        final AtomicReference<XmlParser> activeParser = new AtomicReference<>();
        TypeFactory.registerType(ReentrantKey.class, key -> "key", text -> {
            final XmlParser parser = activeParser.get();
            final Type<Map<String, Object[]>> innerType = Type.of("Map<String,Object[]>");
            final Map<String, Object[]> inner = parser.deserialize("<map>{\"inner\":[1.2300]}</map>", innerType);
            assertThrows(ParsingException.class, () -> parser.deserialize("<map>{\"inner\":[1,}</map>", innerType));
            return new ReentrantKey(inner.get("inner")[0]);
        });
        final Type<Map<ReentrantKey, Object[]>> outerType = Type.of("Map<" + ReentrantKey.class.getCanonicalName() + ",Object[]>");
        for (final XmlParser parser : List.of(new XmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM),
                new AbacusXmlParserImpl(XmlParserType.StAX), new AbacusXmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.SAX))) {
            activeParser.set(parser);
            final Map<ReentrantKey, Object[]> outer = parser.deserialize("<map>{\"key\":[2]}</map>", XmlDeserConfig.create().setElementType(Long.class),
                    outerType);
            assertEquals(1.23d, outer.keySet().iterator().next().nestedValue());
            assertEquals(2L, outer.values().iterator().next()[0], "the outer component must survive both the successful and failed nested reads");
            final Map<String, Object[]> after = parser.deserialize("<map>{\"after\":[3.25]}</map>", Type.of("Map<String,Object[]>"));
            assertEquals(3.25d, after.get("after")[0], "completed reads must release their component state");
        }
    }

    public record ReentrantKey(Object nestedValue) {
    }

    @Test
    void everyCompactContainerEntryStartsItsOwnReentrantComponentScope() {
        final AtomicReference<XmlParser> activeParser = new AtomicReference<>();
        final AtomicReference<NestedRead> activeRead = new AtomicReference<>();
        TypeFactory.registerType(ReentrantContainerKey.class, key -> "key", text -> {
            final XmlParser parser = activeParser.get();
            final NestedRead read = activeRead.get();
            final XmlDeserConfig config = XmlDeserConfig.create().setValueType("items", Type.of("List<Object>"));
            final Object nested = parser.deserialize(read.xml(), config, read.type());
            final List<Number> numbers = new ArrayList<>();
            collectNumbers(nested, numbers);
            assertEquals(read.numberCount(), numbers.size(), read.label());
            for (final Number number : numbers) {
                assertEquals(1.23d, number, read.label() + " must keep its own unconfigured JSON number type");
            }

            // Fail through both open entry paths, then verify an explicitly configured nested read as well.
            assertThrows(ParsingException.class, () -> parser.deserialize("<array>[{\"items\":[1,}]</array>", config, Object[].class));
            assertThrows(ParsingException.class, () -> parser.deserialize("<list>[{\"items\":[1,}]</list>", config, Type.of("List<Object>")));
            final Object[][] strings = parser.deserialize("<array>[[1.2300]]</array>", XmlDeserConfig.create().setElementType(String.class), Object[][].class);
            assertEquals("1.2300", strings[0][0]);
            return new ReentrantContainerKey(text);
        });

        final Type<Map<ReentrantContainerKey, Object[]>> outerType = Type.of("Map<" + ReentrantContainerKey.class.getCanonicalName() + ",Object[]>");
        final String arrayJson = "[{\"items\":[1.2300]}]";
        for (final XmlParser parser : List.of(new XmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM),
                new AbacusXmlParserImpl(XmlParserType.StAX), new AbacusXmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.SAX))) {
            activeParser.set(parser);
            final String wrapped = parser instanceof AbacusXmlParserImpl ? "<map><entry><key>wrapped</key><value>" + arrayJson + "</value></entry></map>"
                    : "<map><wrapped>" + arrayJson + "</wrapped></map>";
            final String bean = "<holder><array>" + arrayJson + "</array><list>" + arrayJson + "</list><map>{\"inner\":[1.2300]}</map><optional>" + arrayJson
                    + "</optional></holder>";
            final List<NestedRead> reads = List.of(new NestedRead("open array", "<array>" + arrayJson + "</array>", Type.of(Object[].class), 1),
                    new NestedRead("open list", "<list>" + arrayJson + "</list>", Type.of("List<Object>"), 1),
                    new NestedRead("open set", "<set>" + arrayJson + "</set>", Type.of("Set<Object>"), 1),
                    new NestedRead("map", "<map>{\"inner\":[1.2300]}</map>", Type.of("Map<String,Object[]>"), 1),
                    new NestedRead("refined reference array", "<array type=\"java.util.HashMap[]\">" + arrayJson + "</array>", Type.of(Map[].class), 1),
                    new NestedRead("optional array map value", wrapped, Type.of("Map<String,java.util.Optional<Object[]>>"), 1),
                    new NestedRead("bean slots", bean, Type.of(ReentrantContainersBean.class), 4),
                    new NestedRead("blank array", "<array> </array>", Type.of(Object[].class), 0),
                    new NestedRead("blank list", "<list> </list>", Type.of("List<Object>"), 0));
            for (final NestedRead read : reads) {
                activeRead.set(read);
                final Map<ReentrantContainerKey, Object[]> outer = parser.deserialize("<map>{\"key\":[2000]}</map>",
                        XmlDeserConfig.create().setElementType(Long.class), outerType);
                assertEquals(2000L, outer.values().iterator().next()[0], read.label() + " must restore the enclosing component after success and failures");
            }
        }
    }

    public record ReentrantContainerKey(String value) {
    }

    private record NestedRead(String label, String xml, Type<?> type, int numberCount) {
    }

    public static class ReentrantContainersBean {
        public Object[] array;
        public List<Object> list;
        public Map<String, Object[]> map;
        public Optional<Object[]> optional;
    }

    private static void collectNumbers(final Object value, final List<Number> numbers) {
        if (value instanceof Number number) {
            numbers.add(number);
        } else if (value instanceof Object[] array) {
            for (final Object element : array) {
                collectNumbers(element, numbers);
            }
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(element -> collectNumbers(element, numbers));
        } else if (value instanceof Map<?, ?> map) {
            map.values().forEach(element -> collectNumbers(element, numbers));
        } else if (value instanceof Optional<?> optional) {
            optional.ifPresent(element -> collectNumbers(element, numbers));
        } else if (value instanceof ReentrantContainersBean bean) {
            collectNumbers(bean.array, numbers);
            collectNumbers(bean.list, numbers);
            collectNumbers(bean.map, numbers);
            collectNumbers(bean.optional, numbers);
        }
    }

    private static final class CountingConfig extends JsonDeserConfig {
        final AtomicInteger valueTypeCalls = new AtomicInteger();
        final AtomicInteger mapKeyTypeCalls = new AtomicInteger();
        final AtomicInteger mapValueTypeCalls = new AtomicInteger();

        @Override
        public <T> Type<T> getValueType(final String key, final Type<T> defaultType) {
            valueTypeCalls.incrementAndGet();
            assertNotNull(defaultType, "the parser has already resolved this map value with its declared default");
            return super.getValueType(key, defaultType);
        }

        @Override
        public <T> Type<T> getMapKeyType() {
            mapKeyTypeCalls.incrementAndGet();
            return super.getMapKeyType();
        }

        @Override
        public <T> Type<T> getMapValueType() {
            mapValueTypeCalls.incrementAndGet();
            return super.getMapValueType();
        }
    }
}
