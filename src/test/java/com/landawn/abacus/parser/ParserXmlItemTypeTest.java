package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;

public class ParserXmlItemTypeTest extends TestBase {
    private final XmlParser parser = ParserFactory.createAbacusXmlParser();

    private List<Object> values() {
        return new ArrayList<>(Arrays.asList(1, 2L, true, new BigDecimal("1.2300"), " \u96ea\ud83d\ude00 ", "", null, -0.0d));
    }

    @Test
    public void mixedListsMatchAcrossBackendsSourcesAndTaggingModes() {
        for (boolean names : List.of(true, false)) {
            for (boolean pretty : List.of(true, false)) {
                String xml = parser.serialize(values(), XmlSerConfig.create().setWriteTypeInfo(true).setTagByPropertyName(names).setPrettyFormat(pretty));
                for (XmlParserType mode : XmlParserType.values()) {
                    XmlParser reader = new AbacusXmlParserImpl(mode);
                    assertEquals(values(), reader.deserialize(xml, List.class), mode + ": " + xml);
                    assertEquals(values(), reader.deserialize(new StringReader(xml), List.class));
                    assertEquals(values(), reader.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), List.class));
                }
            }
        }
    }

    @Test
    public void objectAndPrimitiveArraysPreserveTypesAndNullPositions() {
        Object[] input = values().toArray();
        String xml = parser.serialize(input, XmlSerConfig.create().setWriteTypeInfo(true));
        for (XmlParserType mode : XmlParserType.values()) {
            assertArrayEquals(input, new AbacusXmlParserImpl(mode).deserialize(xml, Object[].class));
        }
        assertArrayEquals(new int[] { 1, -2 }, parser.deserialize("<array type=\"int[]\"><e type=\"Integer\">1</e><e>-2</e></array>", int[].class));
        assertArrayEquals(new Object[] { null, 7L },
                parser.deserialize("<array type=\"Object[]\"><e isNull=\"true\"/><e type=\"Long\">7</e></array>", Object[].class));
    }

    @Test
    public void explicitAndConfiguredTypesKeepPrecedence() {
        String xml = "<list><e type=\"Integer\">1</e><e type=\"Long\">2</e></list>";
        assertEquals(List.of("1", "2"), parser.deserialize(xml, Type.of("List<String>")));
        assertEquals(List.of("1", "2"), parser.deserialize(xml.replace("<list>", "<list type=\"ArrayList&lt;Object&gt;\">"), Type.of("List<String>")));
        assertEquals(List.of("1", "2"), parser.deserialize(xml, XmlDeserConfig.create().setElementType(String.class), List.class));
        List<Number> numbers = parser.deserialize(xml, Type.of("List<Number>"));
        assertEquals(Integer.class, numbers.get(0).getClass());
        assertEquals(Long.class, numbers.get(1).getClass());
    }

    @Test
    public void declaredNestedGenericsSurviveConcreteMetadata() {
        String xml = "<list><e type=\"ArrayList\"><list><e>1.2300</e><e isNull=\"true\"/><e>2.00</e></list></e></list>";
        List<List<BigDecimal>> result = parser.deserialize(xml, Type.of("List<List<BigDecimal>>"));
        assertEquals(ArrayList.class, result.get(0).getClass());
        assertEquals(Arrays.asList(new BigDecimal("1.2300"), null, new BigDecimal("2.00")), result.get(0));
        String sameClass = xml.replace("ArrayList", "List");
        assertEquals(result, parser.deserialize(sameClass, Type.of("List<List<BigDecimal>>")));
        String attributedGeneric = xml.replace("ArrayList", "ArrayList&lt;BigDecimal&gt;");
        assertEquals(result, parser.deserialize(attributedGeneric, List.class));
        String textArray = "<list><e type=\"ArrayList\">[1.2300,2.00]</e></list>";
        List<List<BigDecimal>> scalar = parser.deserialize(textArray, Type.of("List<List<BigDecimal>>"));
        assertEquals(List.of(new BigDecimal("1.2300"), new BigDecimal("2.00")), scalar.get(0));
    }

    @Test
    public void absentBlankTypesAndEmptyValuesRetainFallback() {
        assertEquals(Arrays.asList("1", " ", "", null), parser.deserialize("<list><e>1</e><e type=\" \" > </e><e/><e isNull=\"true\"/></list>", List.class));
        assertEquals(List.of(), parser.deserialize("<list/>", List.class));
        assertArrayEquals(new Object[0], parser.deserialize("<array/>", Object[].class));
        assertEquals(List.of("\u96ea\ud83d\ude00"), parser.deserialize("<list><e type=\"String\">\u96ea\ud83d\ude00</e></list>", List.class));
    }

    @Test
    public void rejectedItemMetadataUsesExistingValidation() {
        String xml = "<list><e type=\"missing.example.UnregisteredValue\">1</e></list>";
        for (XmlParserType mode : XmlParserType.values()) {
            XmlParser reader = new AbacusXmlParserImpl(mode);
            ParsingException error = assertThrows(ParsingException.class, () -> reader.deserialize(xml, List.class));
            assertTrue(error.getMessage().contains("XML type attribute is not allowed"));
            assertThrows(ParsingException.class, () -> reader.deserialize(xml, Type.of("List<String>")));
        }
    }

    @Test
    public void itemMetadataDoesNotRequireAnAncestorTypeAttribute() {
        String items = "<e type=\"Integer\">1</e><e isNull=\"true\"/><e type=\"String\"> \u96ea </e>";
        for (XmlParserType mode : XmlParserType.values()) {
            XmlParser reader = new AbacusXmlParserImpl(mode);
            List<Object> expected = Arrays.asList(1, null, " \u96ea ");
            assertEquals(expected, reader.deserialize("<list>" + items + "</list>", List.class));
            assertArrayEquals(expected.toArray(), reader.deserialize("<array>" + items + "</array>", Object[].class));
            Holder holder = reader.deserialize("<Holder><items><list>" + items + "</list></items></Holder>", Holder.class);
            assertEquals(expected, holder.items, mode.toString());
            String generic = "<list><e type=\"ArrayList\"><list><e>1.2300</e></list></e></list>";
            List<List<BigDecimal>> result = reader.deserialize(generic, Type.of("List<List<BigDecimal>>"));
            assertEquals(new BigDecimal("1.2300"), result.get(0).get(0), mode.toString());
        }
    }

    @Test
    public void mapValueOverridesAndNestedBeanItemsRemainIntact() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("amount", 12);
        input.put("values", values());
        String xml = parser.serialize(input, XmlSerConfig.create().setWriteTypeInfo(true));
        XmlDeserConfig config = XmlDeserConfig.create().setValueType("amount", String.class);
        Map<String, Object> result = parser.deserialize(xml, config, Map.class);
        assertEquals("12", result.get("amount"));
        assertEquals(values(), result.get("values"));
        Item bean = new Item();
        bean.text = " \u96ea ";
        String beans = parser.serialize(new ArrayList<>(Arrays.asList(bean, null)), XmlSerConfig.create().setWriteTypeInfo(true));
        List<?> items = parser.deserialize(beans, List.class);
        assertEquals(bean.text, ((Item) items.get(0)).text);
        assertNull(items.get(1));
    }

    public static class Item {
        public String text;
    }

    public static class Holder {
        public List<Object> items;
    }
}
