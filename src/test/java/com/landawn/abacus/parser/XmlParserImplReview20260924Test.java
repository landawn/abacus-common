package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ImmutableBiMap;
import com.landawn.abacus.util.ImmutableCollection;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableNavigableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.ImmutableSortedMap;
import com.landawn.abacus.util.ImmutableSortedSet;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeReference;

/**
 * Ledger C-243: the XML parsers (XmlParserImpl and AbacusXmlParserImpl, every backend) must create {@code Immutable*}
 * and {@code EnumMap} targets the way the JSON parser does, instead of handing back a plain HashMap/ArrayList (a CCE at
 * the call site) or failing with "No default constructor found".
 */
public class XmlParserImplReview20260924Test extends TestBase {

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

    public static class ImmHolder {
        private ImmutableList<Sub> subs;
        private ImmutableSet<Sub> subSet;
        private ImmutableMap<String, Sub> subMap;
        private ImmutableMap<String, String> strMap;
        private ImmutableSortedMap<String, Integer> sortedMap;
        private EnumMap<TimeUnit, String> enumMap;
        private EnumMap<TimeUnit, Sub> enumSubMap;

        public ImmutableList<Sub> getSubs() {
            return subs;
        }

        public void setSubs(final ImmutableList<Sub> subs) {
            this.subs = subs;
        }

        public ImmutableSet<Sub> getSubSet() {
            return subSet;
        }

        public void setSubSet(final ImmutableSet<Sub> subSet) {
            this.subSet = subSet;
        }

        public ImmutableMap<String, Sub> getSubMap() {
            return subMap;
        }

        public void setSubMap(final ImmutableMap<String, Sub> subMap) {
            this.subMap = subMap;
        }

        public ImmutableMap<String, String> getStrMap() {
            return strMap;
        }

        public void setStrMap(final ImmutableMap<String, String> strMap) {
            this.strMap = strMap;
        }

        public ImmutableSortedMap<String, Integer> getSortedMap() {
            return sortedMap;
        }

        public void setSortedMap(final ImmutableSortedMap<String, Integer> sortedMap) {
            this.sortedMap = sortedMap;
        }

        public EnumMap<TimeUnit, String> getEnumMap() {
            return enumMap;
        }

        public void setEnumMap(final EnumMap<TimeUnit, String> enumMap) {
            this.enumMap = enumMap;
        }

        public EnumMap<TimeUnit, Sub> getEnumSubMap() {
            return enumSubMap;
        }

        public void setEnumSubMap(final EnumMap<TimeUnit, Sub> enumSubMap) {
            this.enumSubMap = enumSubMap;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof ImmHolder h && Objects.equals(subs, h.subs) && Objects.equals(subSet, h.subSet) && Objects.equals(subMap, h.subMap)
                    && Objects.equals(strMap, h.strMap) && Objects.equals(sortedMap, h.sortedMap) && Objects.equals(enumMap, h.enumMap)
                    && Objects.equals(enumSubMap, h.enumSubMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subs, subSet, subMap, strMap, sortedMap, enumMap, enumSubMap);
        }

        @Override
        public String toString() {
            return "ImmHolder[" + subs + ", " + subSet + ", " + subMap + ", " + strMap + ", " + sortedMap + ", " + enumMap + ", " + enumSubMap + "]";
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

    private static final TimeUnit SECONDS = TimeUnit.SECONDS;

    private static Map<String, String> unicodeMap() {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("b", "2");
        m.put("a", "1");
        m.put("ключ", "値é");
        return m;
    }

    private static List<Sub> subs() {
        return List.of(new Sub("a"), new Sub("b"), new Sub("漢字"));
    }

    private static void assertUnmodifiable(final Map<?, ?> m) {
        assertThrows(UnsupportedOperationException.class, () -> ((Map<Object, Object>) m).put("z", "z"));
    }

    @Test
    public void testC243_immutableMapClassTargets() {
        for (final P p : parsers()) {
            final String xml = p.parser.serialize(unicodeMap());

            final Object im = p.parser.deserialize(xml, ImmutableMap.class);
            assertInstanceOf(ImmutableMap.class, im, p.name);
            assertEquals(unicodeMap(), im, p.name);
            assertUnmodifiable((Map<?, ?>) im);

            final Object sm = p.parser.deserialize(xml, ImmutableSortedMap.class);
            assertInstanceOf(ImmutableSortedMap.class, sm, p.name);
            assertEquals(new TreeMap<>(unicodeMap()), sm, p.name);
            assertEquals("a", ((ImmutableSortedMap<?, ?>) sm).firstKey(), p.name);

            final Object nm = p.parser.deserialize(xml, ImmutableNavigableMap.class);
            assertInstanceOf(ImmutableNavigableMap.class, nm, p.name);
            assertEquals(new TreeMap<>(unicodeMap()), nm, p.name);

            final Object bm = p.parser.deserialize(xml, ImmutableBiMap.class);
            assertInstanceOf(ImmutableBiMap.class, bm, p.name);
            assertEquals(unicodeMap(), bm, p.name);
        }
    }

    @Test
    public void testC243_immutableMapTypeTargets() {
        for (final P p : parsers()) {
            final String xml = p.parser.serialize(Map.of("a", 1, "b", 22));

            final ImmutableMap<String, Integer> im = p.parser.deserialize(xml, new TypeReference<ImmutableMap<String, Integer>>() {
            }.type());
            assertInstanceOf(ImmutableMap.class, im, p.name);
            assertEquals(Map.of("a", 1, "b", 22), im, p.name);

            final ImmutableSortedMap<String, Integer> sm = p.parser.deserialize(xml, new TypeReference<ImmutableSortedMap<String, Integer>>() {
            }.type());
            assertInstanceOf(ImmutableSortedMap.class, sm, p.name);
            assertEquals(Map.of("a", 1, "b", 22), sm, p.name);
            assertEquals("b", sm.lastKey(), p.name);

            final ImmutableMap<String, Sub> beanValues = p.parser.deserialize(p.parser.serialize(Map.of("k", new Sub("v"))),
                    new TypeReference<ImmutableMap<String, Sub>>() {
                    }.type());
            assertInstanceOf(ImmutableMap.class, beanValues, p.name);
            assertEquals(Map.of("k", new Sub("v")), beanValues, p.name);
        }
    }

    @Test
    public void testC243_immutableCollectionTypeTargets() {
        for (final P p : parsers()) {
            final String xml = p.parser.serialize(subs());

            final ImmutableList<Sub> il = p.parser.deserialize(xml, new TypeReference<ImmutableList<Sub>>() {
            }.type());
            assertInstanceOf(ImmutableList.class, il, p.name);
            assertEquals(subs(), il, p.name);
            assertThrows(UnsupportedOperationException.class, () -> il.add(new Sub("x")), p.name);

            final ImmutableSet<Sub> is = p.parser.deserialize(xml, new TypeReference<ImmutableSet<Sub>>() {
            }.type());
            assertInstanceOf(ImmutableSet.class, is, p.name);
            assertEquals(N.asSet(subs().toArray()), is, p.name);

            final ImmutableCollection<Sub> ic = p.parser.deserialize(xml, new TypeReference<ImmutableCollection<Sub>>() {
            }.type());
            assertInstanceOf(ImmutableList.class, ic, p.name);
            assertEquals(subs(), new ArrayList<>(ic), p.name);
        }
    }

    @Test
    public void testC243_immutableCollectionClassTargets() {
        for (final P p : parsers()) {
            // element form (not the compact JSON text form, which both parsers already hand to the JSON parser)
            final String xml = "<list><e>b</e><e>a</e><e>é</e></list>";

            for (final Class<?> cls : List.of(ImmutableList.class, ImmutableSet.class, ImmutableCollection.class)) {
                final Object result = p.parser.deserialize(xml, cls);
                assertInstanceOf(cls, result, p.name + " " + cls.getSimpleName());
                assertEquals(N.asSet("a", "b", "é"), new java.util.HashSet<>((java.util.Collection<?>) result), p.name + " " + cls.getSimpleName());
            }

            final Object sorted = p.parser.deserialize(xml, ImmutableSortedSet.class);
            assertInstanceOf(ImmutableSortedSet.class, sorted, p.name);
            assertEquals(List.of("a", "b", "é"), new ArrayList<>((ImmutableSortedSet<?>) sorted), p.name);
        }
    }

    @Test
    public void testC243_rawEnumMapTargetThrowsParsingException() {
        for (final P p : parsers()) {
            final String xml = p.parser.serialize(Map.of("SECONDS", "s"));

            final ParsingException e = assertThrows(ParsingException.class, () -> p.parser.deserialize(xml, EnumMap.class), p.name);
            assertTrue(e.getMessage().startsWith("EnumMap requires an enum key type"), p.name + ": " + e.getMessage());
            assertTrue(e.getMessage().contains("XmlDeserConfig.setMapKeyType"), p.name + ": " + e.getMessage());
        }
    }

    @Test
    public void testC243_typedEnumMapTarget() {
        for (final P p : parsers()) {
            final String xml = p.parser.serialize(Map.of("SECONDS", "s"));

            final Map<TimeUnit, String> byType = p.parser.deserialize(xml, Type.of("java.util.EnumMap<java.util.concurrent.TimeUnit, String>"));
            assertInstanceOf(EnumMap.class, byType, p.name);
            assertEquals(Map.of(SECONDS, "s"), byType, p.name);

            final Map<TimeUnit, String> byRef = p.parser.deserialize(xml, new TypeReference<EnumMap<TimeUnit, String>>() {
            }.type());
            assertInstanceOf(EnumMap.class, byRef, p.name);
            assertEquals(Map.of(SECONDS, "s"), byRef, p.name);

            // raw EnumMap.class with the key type declared through the config
            final Map<?, ?> byConfig = p.parser.deserialize(xml, XmlDeserConfig.create().setMapKeyType(TimeUnit.class), EnumMap.class);
            assertInstanceOf(EnumMap.class, byConfig, p.name);
            assertEquals(Map.of(SECONDS, "s"), byConfig, p.name);
        }
    }

    @Test
    public void testC243_beanWithImmutableAndEnumMapPropertiesRoundTrips() {
        final ImmHolder bean = new ImmHolder();
        bean.setSubs(ImmutableList.of(new Sub("a"), new Sub("b")));
        bean.setSubSet(ImmutableSet.of(new Sub("c")));
        bean.setSubMap(ImmutableMap.of("k", new Sub("d")));
        bean.setStrMap(ImmutableMap.of("x", "1", "ключ", "値"));
        bean.setSortedMap(ImmutableSortedMap.of("m", 1, "n", 2));
        final EnumMap<TimeUnit, String> em = new EnumMap<>(TimeUnit.class);
        em.put(TimeUnit.SECONDS, "s");
        em.put(TimeUnit.DAYS, "d");
        bean.setEnumMap(em);
        final EnumMap<TimeUnit, Sub> esm = new EnumMap<>(TimeUnit.class);
        esm.put(TimeUnit.HOURS, new Sub("e"));
        bean.setEnumSubMap(esm);

        for (final P p : parsers()) {
            for (final boolean typeInfo : new boolean[] { false, true }) {
                // ImmutableSortedMap is not on the XML type-attribute allowlist (AbstractXmlParser), so with type info
                // the property is left out: its type attribute would be rejected before the reader is reached.
                bean.setSortedMap(typeInfo ? null : ImmutableSortedMap.of("m", 1, "n", 2));

                final String msg = p.name + " typeInfo=" + typeInfo;
                final String xml = p.parser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                final ImmHolder copy = p.parser.deserialize(xml, ImmHolder.class);

                assertEquals(bean, copy, msg);
                assertInstanceOf(ImmutableList.class, copy.getSubs(), msg);
                assertInstanceOf(ImmutableSet.class, copy.getSubSet(), msg);
                assertInstanceOf(ImmutableMap.class, copy.getSubMap(), msg);

                if (!typeInfo) {
                    assertInstanceOf(ImmutableSortedMap.class, copy.getSortedMap(), msg);
                }

                assertInstanceOf(EnumMap.class, copy.getEnumMap(), msg);
                assertInstanceOf(EnumMap.class, copy.getEnumSubMap(), msg);
            }
        }

        // the entry point named in the finding
        bean.setSortedMap(ImmutableSortedMap.of("m", 1, "n", 2));
        final ImmHolder viaN = N.fromXml(N.toXml(bean), ImmHolder.class);
        assertEquals(bean, viaN);
    }

    @Test
    public void testC243_nFromXmlImmutableTargets() {
        final ImmutableMap<?, ?> m = N.fromXml("<map><a>1</a></map>", ImmutableMap.class);
        assertEquals(Map.of("a", "1"), m);

        final Object c = N.fromXml("<list>[\"x\", \"y\"]</list>", ImmutableCollection.class);
        assertInstanceOf(ImmutableList.class, c);
        assertEquals(List.of("x", "y"), c);
    }

    @Test
    public void testC243_emptyContainerIntoImmutableTarget() {
        for (final P p : parsers()) {
            final String emptyMap = p.parser.serialize(new HashMap<>());
            final String emptyList = p.parser.serialize(new ArrayList<>());

            for (final String xml : List.of(emptyMap, "<map></map>", "<map/>")) {
                final Object m = p.parser.deserialize(xml, ImmutableMap.class);
                assertInstanceOf(ImmutableMap.class, m, p.name + " " + xml);
                assertTrue(((Map<?, ?>) m).isEmpty(), p.name + " " + xml);
            }

            for (final String xml : List.of(emptyList, "<list></list>", "<list/>")) {
                final Object l = p.parser.deserialize(xml, new TypeReference<ImmutableList<Sub>>() {
                }.type());
                assertInstanceOf(ImmutableList.class, l, p.name + " " + xml);
                assertTrue(((List<?>) l).isEmpty(), p.name + " " + xml);
            }
        }
    }

    @Test
    public void testC243_mutableTargetsUnchanged() {
        for (final P p : parsers()) {
            final String mapXml = p.parser.serialize(unicodeMap());
            final String listXml = p.parser.serialize(subs());

            assertSame(HashMap.class, p.parser.deserialize(mapXml, HashMap.class).getClass(), p.name);
            assertSame(LinkedHashMap.class, p.parser.deserialize(mapXml, LinkedHashMap.class).getClass(), p.name);
            assertSame(TreeMap.class, p.parser.deserialize(mapXml, TreeMap.class).getClass(), p.name);
            assertEquals(unicodeMap(), p.parser.deserialize(mapXml, LinkedHashMap.class), p.name);

            final List<Sub> al = p.parser.deserialize(listXml, new TypeReference<ArrayList<Sub>>() {
            }.type());
            assertSame(ArrayList.class, al.getClass(), p.name);
            assertEquals(subs(), al, p.name);

            final List<Sub> ll = p.parser.deserialize(listXml, new TypeReference<LinkedList<Sub>>() {
            }.type());
            assertSame(LinkedList.class, ll.getClass(), p.name);
            assertEquals(subs(), ll, p.name);

            // the stand-in list stays mutable for a mutable target
            al.add(new Sub("z"));
            assertEquals(4, al.size(), p.name);
        }
    }

    @Test
    public void testC243_jsonEnumMapMessageUnchanged() {
        final ParsingException e = assertThrows(ParsingException.class, () -> N.fromJson("{\"SECONDS\":\"s\"}", EnumMap.class));
        assertEquals("EnumMap requires an enum key type; declare it via the property type, Type.of(\"java.util.EnumMap<K, V>\") "
                + "or JsonDeserConfig.setMapKeyType (resolved key type: Object)", e.getMessage());

        final Map<TimeUnit, String> ok = N.fromJson("{\"SECONDS\":\"s\"}", Type.of("java.util.EnumMap<java.util.concurrent.TimeUnit, String>"));
        assertInstanceOf(EnumMap.class, ok);
        assertEquals(Map.of(SECONDS, "s"), ok);
    }
}
