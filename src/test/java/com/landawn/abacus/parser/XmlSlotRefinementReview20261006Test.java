package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.AbstractXmlParser.SlotSchema;
import com.landawn.abacus.type.Type;

/**
 * Code review 2026-10-06 of the XML readers' slot refinement:
 * <ul>
 * <li>{@code compactJsonConfig} shares the XML configuration's validated per-key value types instead of re-validating
 * them for every compact value;</li>
 * <li>{@code refineSlot} applies the configuration's component and map types and restores the slot's declared array in
 * one step, where every reader used to make two or three separate calls;</li>
 * <li>container metadata narrows a component declared as an abstract class or interface with a type of its own
 * ({@code Long[]} on a {@code Number[]} slot reads {@code Long} elements), which the javadoc promised but no reader
 * produced: compact text read the JSON tokens as {@code Integer}, and child elements failed because {@code Number}
 * cannot read text.</li>
 * </ul>
 */
public class XmlSlotRefinementReview20261006Test extends TestBase {

    public static class NumberBean {
        private Number[] nums;
        private List<Number> lnums;
        private Map<String, Number> mnums;
        private Date[] dates;

        public Number[] getNums() {
            return nums;
        }

        public void setNums(final Number[] nums) {
            this.nums = nums;
        }

        public List<Number> getLnums() {
            return lnums;
        }

        public void setLnums(final List<Number> lnums) {
            this.lnums = lnums;
        }

        public Map<String, Number> getMnums() {
            return mnums;
        }

        public void setMnums(final Map<String, Number> mnums) {
            this.mnums = mnums;
        }

        public Date[] getDates() {
            return dates;
        }

        public void setDates(final Date[] dates) {
            this.dates = dates;
        }
    }

    enum Backend {
        XML_STAX(false, XmlParserType.StAX),
        XML_DOM(false, XmlParserType.DOM),
        ABACUS_STAX(true, XmlParserType.StAX),
        ABACUS_DOM(true, XmlParserType.DOM),
        ABACUS_SAX(true, XmlParserType.SAX);

        final boolean abacus;
        final XmlParserType parserType;

        Backend(final boolean abacus, final XmlParserType parserType) {
            this.abacus = abacus;
            this.parserType = parserType;
        }

        XmlParser parser() {
            final Set<Class<?>> allowed = Set.of(NumberBean.class);
            return abacus ? new AbacusXmlParserImpl(parserType, null, null, allowed) : new XmlParserImpl(parserType, null, null, allowed);
        }
    }

    static Stream<Backend> allBackends() {
        return Stream.of(Backend.values());
    }

    /** A bean document in the writer's form: the root names the bean class, which the DOM reader needs before it consults the children's type attributes. */
    static String bean(final String properties) {
        return "<numberBean type=\"com.landawn.abacus.parser.XmlSlotRefinementReview20261006Test.NumberBean\">" + properties + "</numberBean>";
    }

    @Test
    public void compactJsonConfigSharesTheValidatedValueTypes() {
        final XmlDeserConfig config = XmlDeserConfig.create()
                .setValueType("n", Long.class)
                .setElementType(Integer.class)
                .setMapKeyType(String.class)
                .setMapValueType(Double.class);
        final JsonDeserConfig jdc = AbstractXmlParser.compactJsonConfig(config);

        // the map validated by the XML configuration's setters is shared, neither re-validated nor copied
        assertSame(config.valueTypeMap, jdc.valueTypeMap);
        assertTrue(jdc.hasValueTypes());
        assertEquals(Type.of(Long.class), jdc.getValueType("n"));
        assertEquals(Type.of(Integer.class), jdc.getElementType());
        assertEquals(Type.of(String.class), jdc.getMapKeyType());
        assertEquals(Type.of(Double.class), jdc.getMapValueType());

        assertFalse(AbstractXmlParser.compactJsonConfig(XmlDeserConfig.create()).hasValueTypes());
        assertFalse(AbstractXmlParser.compactJsonConfig(null).hasValueTypes());

        // the bean form of the per-key types is shared the same way
        final XmlDeserConfig byBean = XmlDeserConfig.create().setValueTypesByBeanClass(NumberBean.class);
        assertSame(byBean.beanInfoForValueTypes, AbstractXmlParser.compactJsonConfig(byBean).beanInfoForValueTypes);
        assertTrue(AbstractXmlParser.compactJsonConfig(byBean).hasValueTypes());

        // and the shared types still reach compact text at the root, as before
        final XmlDeserConfig perKey = XmlDeserConfig.create().setValueType("n", Long.class);
        final List<?> list = ParserFactory.createXmlParser().deserialize("<list>[{\"n\":\"1\"}]</list>", perKey, List.class);
        assertEquals(List.of(Map.of("n", 1L)), list);
    }

    @Test
    public void refineSlotRefinesAndRestoresTheDeclaredArrayTogether() {
        final XmlDeserConfig elementInteger = XmlDeserConfig.create().setElementType(Integer.class);
        final Type<?> objectArray = Type.of(Object[].class);
        final Type<?> longArray = Type.of(Long[].class);

        // an open array slot under element configuration keeps its declared runtime while the configured component
        // types its elements
        SlotSchema slot = AbstractXmlParser.refineSlot(objectArray, longArray, elementInteger);
        assertEquals(longArray, slot.refined());
        assertEquals(objectArray, slot.destination());
        assertEquals(Type.of(Integer.class), AbstractXmlParser.arrayComponentOverride(objectArray, slot.refined(), elementInteger.getElementType()));
        assertEquals(slot, AbstractXmlParser.refineSlot(objectArray, longArray, elementInteger.getElementType()));

        // without configuration the metadata is the only component information and decides the runtime class too
        slot = AbstractXmlParser.refineSlot(objectArray, longArray, XmlDeserConfig.create());
        assertEquals(longArray, slot.refined());
        assertEquals(longArray, slot.destination());
        assertEquals(slot, AbstractXmlParser.refineSlot(objectArray, longArray, (XmlDeserConfig) null));

        // a declared reference array keeps its class while the refined component types the elements
        slot = AbstractXmlParser.refineSlot(Type.of(Number[].class), longArray, (XmlDeserConfig) null);
        assertEquals(longArray, slot.refined());
        assertEquals(Type.of(Number[].class), slot.destination());
        assertEquals(Type.of(Long.class), AbstractXmlParser.arrayComponentOverride(Type.of(Number[].class), slot.refined(), null));

        // a collection slot: the configured component replaces an open element, and the destination is the schema itself
        slot = AbstractXmlParser.refineSlot(Type.of("List<Object>"), Type.of("ArrayList<Long>"), elementInteger);
        assertEquals("ArrayList<Integer>", slot.refined().name());
        assertSame(slot.refined(), slot.destination());

        // the global map types fill a declared map slot's open value through the configuration overload only
        final XmlDeserConfig mapValueLong = XmlDeserConfig.create().setMapValueType(Long.class);
        final Type<?> openMap = Type.of("Map<String, Object>");
        assertEquals(Type.of("Map<String, Long>"), AbstractXmlParser.refineSlot(openMap, openMap, mapValueLong).refined());
        assertEquals(openMap, AbstractXmlParser.refineSlot(openMap, openMap, mapValueLong.getElementType()).refined());

        // a missing schema or declaration passes through
        assertNull(AbstractXmlParser.refineSlot(objectArray, null, elementInteger).refined());
        assertNull(AbstractXmlParser.refineSlot(objectArray, null, elementInteger).destination());
        slot = AbstractXmlParser.refineSlot(null, longArray, elementInteger);
        assertEquals(longArray, slot.refined());
        assertEquals(longArray, slot.destination());
    }

    @Test
    public void containerMetadataNarrowsAnAbstractDeclaredComponent() {
        // the schema refinement behind every reader
        assertEquals(Type.of(Long[].class), AbstractXmlParser.refineDeclaredSchema(Type.of(Number[].class), Type.of(Long[].class)));
        assertEquals(Type.of(Long.class), AbstractXmlParser.refineDeclaredSchema(Type.of("List<Number>"), Type.of("ArrayList<Long>")).elementType());
        assertEquals(Type.of(Long.class),
                AbstractXmlParser.refineDeclaredSchema(Type.of("Map<String, Number>"), Type.of("HashMap<String, Long>")).parameterTypes().get(1));
        // a concrete declared component reads its own text and keeps its type
        assertEquals(Type.of(Date[].class), AbstractXmlParser.refineDeclaredSchema(Type.of(Date[].class), Type.of(Timestamp[].class)));
        // an open component still takes the metadata, and metadata that supplies nothing changes nothing
        assertEquals(Type.of(Long[].class), AbstractXmlParser.refineDeclaredSchema(Type.of(Object[].class), Type.of(Long[].class)));
        assertEquals(Type.of(Number[].class), AbstractXmlParser.refineDeclaredSchema(Type.of(Number[].class), Type.of(Object[].class)));
        assertEquals(Type.of(Number[].class), AbstractXmlParser.refineDeclaredSchema(Type.of(Number[].class), Type.of(Number[].class)));
        // the slot itself (not a component) is refined by chooseDeclaredType, not here
        assertEquals(Type.of(Number.class), AbstractXmlParser.refineDeclaredSchema(Type.of(Number.class), Type.of(Long.class)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void arrayMetadataOnADeclaredNumberArrayReadsItsElementsAsTheMetadataComponent(final Backend backend) {
        final XmlParser parser = backend.parser();
        final Number[] expected = { 1L, 2L };

        // compact text, in a property and at the root
        NumberBean bean = parser.deserialize(bean("<nums type=\"Long[]\">[1,2]</nums>"), null, NumberBean.class);
        assertSame(Number[].class, bean.getNums().getClass());
        assertArrayEquals(expected, bean.getNums());
        Number[] root = parser.deserialize("<array type=\"Long[]\">[1,2]</array>", null, Number[].class);
        assertSame(Number[].class, root.getClass());
        assertArrayEquals(expected, root);

        // child elements, which used to fail with "No public static method or constructor found in java.lang.Number"
        final String children = backend.abacus ? bean("<nums type=\"Long[]\"><array><e>1</e><e>2</e></array></nums>")
                : bean("<nums type=\"Long[]\"><e>1</e><e>2</e></nums>");
        bean = parser.deserialize(children, null, NumberBean.class);
        assertSame(Number[].class, bean.getNums().getClass());
        assertArrayEquals(expected, bean.getNums());
        root = parser.deserialize("<array type=\"Long[]\"><e>1</e><e>2</e></array>", null, Number[].class);
        assertSame(Number[].class, root.getClass());
        assertArrayEquals(expected, root);

        // metadata naming the declared type itself keeps the JSON tokens' own types
        bean = parser.deserialize(bean("<nums type=\"Number[]\">[1, 2.5]</nums>"), null, NumberBean.class);
        assertArrayEquals(new Number[] { 1, 2.5d }, bean.getNums());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void containerMetadataOnDeclaredNumberCollectionsReadsTheirElementsAsTheMetadataComponent(final Backend backend) {
        final XmlParser parser = backend.parser();

        NumberBean bean = parser.deserialize(bean("<lnums type=\"ArrayList&lt;Long&gt;\">[1,2]</lnums>"), null, NumberBean.class);
        assertEquals(List.of(1L, 2L), bean.getLnums());
        final String listChildren = backend.abacus ? bean("<lnums type=\"ArrayList&lt;Long&gt;\"><list><e>1</e><e>2</e></list></lnums>")
                : bean("<lnums type=\"ArrayList&lt;Long&gt;\"><e>1</e><e>2</e></lnums>");
        bean = parser.deserialize(listChildren, null, NumberBean.class);
        assertEquals(List.of(1L, 2L), bean.getLnums());
        final List<Number> rootList = parser.deserialize("<list type=\"ArrayList&lt;Long&gt;\">[1,2]</list>", null, Type.of("List<Number>"));
        assertEquals(List.of(1L, 2L), rootList);

        bean = parser.deserialize(bean("<mnums type=\"HashMap&lt;String, Long&gt;\">{\"a\":1}</mnums>"), null, NumberBean.class);
        assertEquals(Map.of("a", 1L), bean.getMnums());
        final String mapChildren = backend.abacus
                ? bean("<mnums type=\"HashMap&lt;String, Long&gt;\"><map><entry><key>a</key><value>1</value></entry></map></mnums>")
                : bean("<mnums type=\"HashMap&lt;String, Long&gt;\"><map><a>1</a></map></mnums>");
        bean = parser.deserialize(mapChildren, null, NumberBean.class);
        assertEquals(Map.of("a", 1L), bean.getMnums());
    }
}
