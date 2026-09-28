package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeReference;

/**
 * U32-01 / U32-05 (2026-09-25, ledger L12/C-243 follow-up): with {@code XmlSerConfig.setWriteTypeInfo(true)} the
 * writers record the RAW runtime type of a map value / entry key/value / list element
 * ({@code type="java.util.EnumMap&lt;Object, Object&gt;"}, {@code type="ImmutableList&lt;Object&gt;"}). The StAX
 * readers of both parsers, the SAX reader and the Xml-DOM ARRAY/COLLECTION element branches took that attribute in
 * place of the DECLARED parameterized type, so a declared {@code Map<String, EnumMap<TimeUnit, String>>} failed with
 * "EnumMap requires an enum key type ... (resolved key type: Object)" and a nested {@code ImmutableList<Sub>} value
 * came back as a list of maps. These tests pin the fixed behaviour on all five backends, with and without type info,
 * for EnumMap / Immutable* in map VALUE and list ELEMENT position, Immutable-in-Immutable and EnumMap-of-Immutable
 * shapes, both as bean properties and as root {@code Type} targets, and check that the attribute still wins when it
 * adds information the declared type lacks.
 */
public class XmlParserImplReview20260925Test extends TestBase {

    public static class Sub {
        private String name;

        public Sub() {
        }

        public Sub(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof Sub s && Objects.equals(name, s.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        @Override
        public String toString() {
            return "Sub(" + name + ")";
        }
    }

    /**
     * Every property puts an EnumMap or an Immutable* container in a map VALUE or list ELEMENT position, where the
     * writer records the raw runtime type when type info is on.
     */
    public static class Nested {
        private Map<String, EnumMap<TimeUnit, String>> byName;
        private List<EnumMap<TimeUnit, String>> enumMaps;
        private Map<String, ImmutableList<Sub>> immVals;
        private ImmutableMap<String, ImmutableList<Sub>> immOfImm;
        private EnumMap<TimeUnit, ImmutableList<Sub>> enumOfImm;
        private Map<String, ImmutableSet<Sub>> immSetVals;
        private Map<String, List<Sub>> mutableVals;

        public Map<String, EnumMap<TimeUnit, String>> getByName() {
            return byName;
        }

        public void setByName(final Map<String, EnumMap<TimeUnit, String>> byName) {
            this.byName = byName;
        }

        public List<EnumMap<TimeUnit, String>> getEnumMaps() {
            return enumMaps;
        }

        public void setEnumMaps(final List<EnumMap<TimeUnit, String>> enumMaps) {
            this.enumMaps = enumMaps;
        }

        public Map<String, ImmutableList<Sub>> getImmVals() {
            return immVals;
        }

        public void setImmVals(final Map<String, ImmutableList<Sub>> immVals) {
            this.immVals = immVals;
        }

        public ImmutableMap<String, ImmutableList<Sub>> getImmOfImm() {
            return immOfImm;
        }

        public void setImmOfImm(final ImmutableMap<String, ImmutableList<Sub>> immOfImm) {
            this.immOfImm = immOfImm;
        }

        public EnumMap<TimeUnit, ImmutableList<Sub>> getEnumOfImm() {
            return enumOfImm;
        }

        public void setEnumOfImm(final EnumMap<TimeUnit, ImmutableList<Sub>> enumOfImm) {
            this.enumOfImm = enumOfImm;
        }

        public Map<String, ImmutableSet<Sub>> getImmSetVals() {
            return immSetVals;
        }

        public void setImmSetVals(final Map<String, ImmutableSet<Sub>> immSetVals) {
            this.immSetVals = immSetVals;
        }

        public Map<String, List<Sub>> getMutableVals() {
            return mutableVals;
        }

        public void setMutableVals(final Map<String, List<Sub>> mutableVals) {
            this.mutableVals = mutableVals;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof Nested n && Objects.equals(byName, n.byName) && Objects.equals(enumMaps, n.enumMaps)
                    && Objects.equals(immVals, n.immVals) && Objects.equals(immOfImm, n.immOfImm) && Objects.equals(enumOfImm, n.enumOfImm)
                    && Objects.equals(immSetVals, n.immSetVals) && Objects.equals(mutableVals, n.mutableVals);
        }

        @Override
        public int hashCode() {
            return Objects.hash(byName, enumMaps, immVals, immOfImm, enumOfImm, immSetVals, mutableVals);
        }

        @Override
        public String toString() {
            return "Nested[" + byName + ", " + enumMaps + ", " + immVals + ", " + immOfImm + ", " + enumOfImm + ", " + immSetVals + ", " + mutableVals
                    + "]";
        }
    }

    private record P(String name, XmlParser parser) {
    }

    private static List<P> parsers() {
        final List<P> list = new ArrayList<>();
        list.add(new P("Xml-StAX", new XmlParserImpl(XmlParserType.StAX)));
        list.add(new P("Xml-DOM", new XmlParserImpl(XmlParserType.DOM)));
        list.add(new P("AbacusXml-StAX", new AbacusXmlParserImpl(XmlParserType.StAX)));
        list.add(new P("AbacusXml-DOM", new AbacusXmlParserImpl(XmlParserType.DOM)));
        list.add(new P("AbacusXml-SAX", new AbacusXmlParserImpl(XmlParserType.SAX)));
        return list;
    }

    private static final boolean[] TYPE_INFO = { false, true };

    /** The raw runtime type attribute the writers record for an EnumMap value/element: no key enum in it. */
    private static final String RAW_ENUM_MAP_ATTR = "java.util.EnumMap&lt;Object, Object&gt;";

    /** The raw runtime type attribute the writers record for an ImmutableList value/element: no element type in it. */
    private static final String RAW_IMMUTABLE_LIST_ATTR = "ImmutableList&lt;Object&gt;";

    private static EnumMap<TimeUnit, String> enumMap(final String seconds, final String days) {
        final EnumMap<TimeUnit, String> em = new EnumMap<>(TimeUnit.class);
        em.put(TimeUnit.SECONDS, seconds);
        em.put(TimeUnit.DAYS, days);
        return em;
    }

    private static Nested nested() {
        final Nested bean = new Nested();

        final Map<String, EnumMap<TimeUnit, String>> byName = new LinkedHashMap<>();
        byName.put("k", enumMap("s", "d"));
        byName.put("empty", new EnumMap<>(TimeUnit.class));
        byName.put("nil", null);
        bean.setByName(byName);

        bean.setEnumMaps(new ArrayList<>(List.of(enumMap("s1", "d1"), enumMap("s2", "d2"))));

        final Map<String, ImmutableList<Sub>> immVals = new LinkedHashMap<>();
        immVals.put("k", ImmutableList.of(new Sub("a"), new Sub("\u00E9")));
        immVals.put("empty", ImmutableList.empty());
        bean.setImmVals(immVals);

        bean.setImmOfImm(ImmutableMap.of("k", ImmutableList.of(new Sub("z"))));

        final EnumMap<TimeUnit, ImmutableList<Sub>> enumOfImm = new EnumMap<>(TimeUnit.class);
        enumOfImm.put(TimeUnit.HOURS, ImmutableList.of(new Sub("h")));
        bean.setEnumOfImm(enumOfImm);

        final Map<String, ImmutableSet<Sub>> immSetVals = new LinkedHashMap<>();
        immSetVals.put("k", ImmutableSet.of(new Sub("c")));
        bean.setImmSetVals(immSetVals);

        final Map<String, List<Sub>> mutableVals = new LinkedHashMap<>();
        mutableVals.put("k", ImmutableList.of(new Sub("m")));
        bean.setMutableVals(mutableVals);

        return bean;
    }

    private static void assertEnumMap(final Object o, final Map<TimeUnit, String> expected, final String msg) {
        assertInstanceOf(EnumMap.class, o, msg);
        assertEquals(expected, o, msg);

        for (final Object k : ((Map<?, ?>) o).keySet()) {
            assertInstanceOf(TimeUnit.class, k, msg);
        }
    }

    private static void assertImmutableListOfSub(final Object o, final List<Sub> expected, final String msg) {
        assertInstanceOf(ImmutableList.class, o, msg);
        assertEquals(expected, o, msg);

        for (final Object e : (List<?>) o) {
            assertInstanceOf(Sub.class, e, msg);
        }
    }

    private static void assertNested(final Nested bean, final Nested copy, final String msg) {
        assertEquals(bean, copy, msg);

        // map VALUE = EnumMap: filled, empty and null values
        assertEnumMap(copy.getByName().get("k"), enumMap("s", "d"), msg);
        assertEnumMap(copy.getByName().get("empty"), Map.of(), msg);
        assertTrue(copy.getByName().containsKey("nil"), msg);
        assertNull(copy.getByName().get("nil"), msg);

        // list ELEMENT = EnumMap
        assertEquals(2, copy.getEnumMaps().size(), msg);
        assertEnumMap(copy.getEnumMaps().get(0), enumMap("s1", "d1"), msg);
        assertEnumMap(copy.getEnumMaps().get(1), enumMap("s2", "d2"), msg);

        // map VALUE = ImmutableList<Sub>: the element type must survive (Xml-StAX read the elements as maps)
        assertImmutableListOfSub(copy.getImmVals().get("k"), List.of(new Sub("a"), new Sub("\u00E9")), msg);
        assertImmutableListOfSub(copy.getImmVals().get("empty"), List.of(), msg);

        // Immutable in Immutable
        assertInstanceOf(ImmutableMap.class, copy.getImmOfImm(), msg);
        assertImmutableListOfSub(copy.getImmOfImm().get("k"), List.of(new Sub("z")), msg);

        // EnumMap whose VALUE is an ImmutableList<Sub> (the entry KEY carries type="...TimeUnit(NAME)")
        assertInstanceOf(EnumMap.class, copy.getEnumOfImm(), msg);
        assertImmutableListOfSub(copy.getEnumOfImm().get(TimeUnit.HOURS), List.of(new Sub("h")), msg);

        // map VALUE = ImmutableSet<Sub>
        assertInstanceOf(ImmutableSet.class, copy.getImmSetVals().get("k"), msg);
        assertInstanceOf(Sub.class, copy.getImmSetVals().get("k").iterator().next(), msg);

        // declared mutable value type holding an Immutable value at runtime: the declared element type still wins
        assertEquals(List.of(new Sub("m")), copy.getMutableVals().get("k"), msg);
        assertInstanceOf(Sub.class, copy.getMutableVals().get("k").get(0), msg);
    }

    @Test
    public void testU32_01_beanWithNestedEnumMapAndImmutableValuesRoundTripsOnAllBackends() {
        final Nested bean = nested();

        for (final P p : parsers()) {
            for (final boolean typeInfo : TYPE_INFO) {
                final String msg = p.name + " typeInfo=" + typeInfo;
                final String xml = p.parser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(typeInfo));

                // the formerly failing input: the value/element attributes are the RAW runtime types
                assertEquals(typeInfo, xml.contains(RAW_ENUM_MAP_ATTR), msg + ": " + xml);
                assertEquals(typeInfo, xml.contains(RAW_IMMUTABLE_LIST_ATTR), msg + ": " + xml);

                assertNested(bean, p.parser.deserialize(xml, Nested.class), msg);
            }
        }
    }

    @Test
    public void testU32_01_nFromXmlWithTypeInfo() {
        final Nested bean = nested();

        for (final boolean typeInfo : TYPE_INFO) {
            final String xml = N.toXml(bean, XmlSerConfig.create().setWriteTypeInfo(typeInfo));

            assertNested(bean, N.fromXml(xml, Nested.class), "N.fromXml typeInfo=" + typeInfo);
        }
    }

    @Test
    public void testU32_01_rootMapWithEnumMapValues() {
        final LinkedHashMap<String, EnumMap<TimeUnit, String>> byName = new LinkedHashMap<>();
        byName.put("k", enumMap("s", "d"));
        byName.put("empty", new EnumMap<>(TimeUnit.class));

        // Declared as the runtime class: the root element keeps its declared type on every backend, so what is
        // exercised is exactly the map VALUE site.
        final Type<LinkedHashMap<String, EnumMap<TimeUnit, String>>> exactType = new TypeReference<LinkedHashMap<String, EnumMap<TimeUnit, String>>>() {
        }.type();
        final Type<Map<String, EnumMap<TimeUnit, String>>> mapType = new TypeReference<Map<String, EnumMap<TimeUnit, String>>>() {
        }.type();

        for (final P p : parsers()) {
            for (final boolean typeInfo : TYPE_INFO) {
                final String msg = p.name + " typeInfo=" + typeInfo;
                final String xml = p.parser.serialize(byName, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                assertEquals(typeInfo, xml.contains(RAW_ENUM_MAP_ATTR), msg + ": " + xml);

                final Map<String, EnumMap<TimeUnit, String>> exact = p.parser.deserialize(xml, exactType);
                assertInstanceOf(LinkedHashMap.class, exact, msg);
                assertEquals(byName, exact, msg);
                assertEnumMap(exact.get("k"), enumMap("s", "d"), msg);
                assertEnumMap(exact.get("empty"), Map.of(), msg);

                // A root Map<..> target whose runtime class (LinkedHashMap) differs from the declared class is re-typed
                // to the raw runtime class by the Xml-DOM, AbacusXml-StAX (maps, unlike collections), AbacusXml-DOM and
                // AbacusXml-SAX readers before the value type is looked at - a pre-existing ROOT-level rule that U32-01
                // does not touch. Xml-StAX keeps the declared root type (chooseDeclaredType); without type info there is
                // no attribute to re-type from.
                if (!typeInfo || p.name.equals("Xml-StAX")) {
                    final Map<String, EnumMap<TimeUnit, String>> viaMap = p.parser.deserialize(xml, mapType);
                    assertEquals(byName, viaMap, msg);
                    assertEnumMap(viaMap.get("k"), enumMap("s", "d"), msg);
                    assertEnumMap(viaMap.get("empty"), Map.of(), msg);
                }
            }
        }
    }

    @Test
    public void testU32_01_rootImmutableMapOfImmutableList() {
        final ImmutableMap<String, ImmutableList<Sub>> immOfImm = ImmutableMap.of("k", ImmutableList.of(new Sub("z"), new Sub("\u6F22\u5B57")), "empty",
                ImmutableList.empty());
        final Type<ImmutableMap<String, ImmutableList<Sub>>> type = new TypeReference<ImmutableMap<String, ImmutableList<Sub>>>() {
        }.type();

        for (final P p : parsers()) {
            for (final boolean typeInfo : TYPE_INFO) {
                final String msg = p.name + " typeInfo=" + typeInfo;
                final String xml = p.parser.serialize(immOfImm, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                assertEquals(typeInfo, xml.contains(RAW_IMMUTABLE_LIST_ATTR), msg + ": " + xml);

                final ImmutableMap<String, ImmutableList<Sub>> copy = p.parser.deserialize(xml, type);
                assertInstanceOf(ImmutableMap.class, copy, msg);
                assertEquals(immOfImm, copy, msg);
                assertImmutableListOfSub(copy.get("k"), List.of(new Sub("z"), new Sub("\u6F22\u5B57")), msg);
                assertImmutableListOfSub(copy.get("empty"), List.of(), msg);
            }
        }
    }

    @Test
    public void testU32_01_rootListWithEnumMapAndImmutableListElements() {
        // list ELEMENT position; declared as the runtime class (ArrayList) so that no root-level re-typing interferes.
        // Xml-DOM used to throw "EnumMap requires an enum key type" for the EnumMap elements and read the Sub elements of
        // the ImmutableList elements as maps with type info on (DOM ARRAY/COLLECTION branches).
        final ArrayList<EnumMap<TimeUnit, String>> enumMaps = new ArrayList<>(List.of(enumMap("s1", "d1"), enumMap("s2", "d2")));
        final ArrayList<ImmutableList<Sub>> immElems = new ArrayList<>(List.of(ImmutableList.of(new Sub("b"), new Sub("\u043A\u043B\u044E\u0447")), ImmutableList.empty()));

        final Type<ArrayList<EnumMap<TimeUnit, String>>> enumMapsType = new TypeReference<ArrayList<EnumMap<TimeUnit, String>>>() {
        }.type();
        final Type<ArrayList<ImmutableList<Sub>>> immElemsType = new TypeReference<ArrayList<ImmutableList<Sub>>>() {
        }.type();

        for (final P p : parsers()) {
            for (final boolean typeInfo : TYPE_INFO) {
                final String msg = p.name + " typeInfo=" + typeInfo;

                final String enumXml = p.parser.serialize(enumMaps, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                assertEquals(typeInfo, enumXml.contains(RAW_ENUM_MAP_ATTR), msg + ": " + enumXml);
                final List<EnumMap<TimeUnit, String>> enumCopy = p.parser.deserialize(enumXml, enumMapsType);
                assertEquals(enumMaps, enumCopy, msg);
                assertEnumMap(enumCopy.get(0), enumMap("s1", "d1"), msg);
                assertEnumMap(enumCopy.get(1), enumMap("s2", "d2"), msg);

                final String immXml = p.parser.serialize(immElems, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                assertEquals(typeInfo, immXml.contains(RAW_IMMUTABLE_LIST_ATTR), msg + ": " + immXml);
                final List<ImmutableList<Sub>> immCopy = p.parser.deserialize(immXml, immElemsType);
                assertEquals(immElems, immCopy, msg);
                assertImmutableListOfSub(immCopy.get(0), List.of(new Sub("b"), new Sub("\u043A\u043B\u044E\u0447")), msg);
                assertImmutableListOfSub(immCopy.get(1), List.of(), msg);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testU32_01_rootArrayWithEnumMapAndImmutableListElements() {
        // array ELEMENT position: the Xml-StAX and Xml-DOM ARRAY branches built the element type raw from the concrete
        // class, so with type info the EnumMap elements threw "EnumMap requires an enum key type" and the Sub elements
        // of the ImmutableList elements were read as maps.
        final EnumMap<TimeUnit, String>[] enumMaps = new EnumMap[] { enumMap("s1", "d1"), enumMap("s2", "d2") };
        final ImmutableList<Sub>[] immElems = new ImmutableList[] { ImmutableList.of(new Sub("b"), new Sub("\u043A\u043B\u044E\u0447")), ImmutableList.empty() };

        final Type<Object> enumMapsType = Type.of("java.util.EnumMap<java.util.concurrent.TimeUnit, String>[]");
        final Type<Object> immElemsType = Type.of("ImmutableList<" + Sub.class.getCanonicalName() + ">[]");

        for (final P p : parsers()) {
            for (final boolean typeInfo : TYPE_INFO) {
                final String msg = p.name + " typeInfo=" + typeInfo;

                final String enumXml = p.parser.serialize(enumMaps, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                assertEquals(typeInfo, enumXml.contains(RAW_ENUM_MAP_ATTR), msg + ": " + enumXml);
                final Object[] enumCopy = (Object[]) p.parser.deserialize(enumXml, enumMapsType);
                assertInstanceOf(EnumMap[].class, enumCopy, msg);
                assertEquals(2, enumCopy.length, msg);
                assertEnumMap(enumCopy[0], enumMap("s1", "d1"), msg);
                assertEnumMap(enumCopy[1], enumMap("s2", "d2"), msg);

                final String immXml = p.parser.serialize(immElems, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                assertEquals(typeInfo, immXml.contains(RAW_IMMUTABLE_LIST_ATTR), msg + ": " + immXml);
                final Object[] immCopy = (Object[]) p.parser.deserialize(immXml, immElemsType);
                assertInstanceOf(ImmutableList[].class, immCopy, msg);
                assertEquals(2, immCopy.length, msg);
                assertImmutableListOfSub(immCopy[0], List.of(new Sub("b"), new Sub("\u043A\u043B\u044E\u0447")), msg);
                assertImmutableListOfSub(immCopy[1], List.of(), msg);
            }
        }
    }

    @Test
    public void testU32_01_attributeStillWinsWhenTheDeclaredTypeAddsNothing() {
        // The fix only keeps the declared type arguments; when the declared value type carries none (Map<Object, Object>),
        // a value written as a different, instantiable class is still read as that class on every backend.
        final Map<String, Map<Object, Object>> byName = new LinkedHashMap<>();
        byName.put("k", new TreeMap<>(Map.of("b", 2, "a", 1)));

        final Type<LinkedHashMap<String, Map<Object, Object>>> type = new TypeReference<LinkedHashMap<String, Map<Object, Object>>>() {
        }.type();

        for (final P p : parsers()) {
            final String msg = p.name;
            final String xml = p.parser.serialize(byName, XmlSerConfig.create().setWriteTypeInfo(true));
            assertTrue(xml.contains("TreeMap&lt;Object, Object&gt;"), msg + ": " + xml);

            final Map<String, Map<Object, Object>> copy = p.parser.deserialize(xml, type);
            assertInstanceOf(TreeMap.class, copy.get("k"), msg);
            assertEquals(Map.of("a", 1, "b", 2), copy.get("k"), msg);
            assertInstanceOf(Integer.class, copy.get("k").get("a"), msg);
        }
    }
}
