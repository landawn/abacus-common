package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Verification of the rule that XML type metadata never constructs a JDK container class outside the public API
 * ({@link AbstractParser#isNonPublicJdkContainerClass(Class)}).
 */
public class AbstractParserVerify20261027G141Test extends TestBase {

    // ---- deep review 2026-10-27 verify G141 begin ----

    public static class G141ViewBean {
        private Object obj;
        private Collection<Object> coll;
        private Set<Object> set;
        private List<Object> items;
        private String tail;

        public Object getObj() {
            return obj;
        }

        public void setObj(final Object obj) {
            this.obj = obj;
        }

        public Collection<Object> getColl() {
            return coll;
        }

        public void setColl(final Collection<Object> coll) {
            this.coll = coll;
        }

        public Set<Object> getSet() {
            return set;
        }

        public void setSet(final Set<Object> set) {
            this.set = set;
        }

        public List<Object> getItems() {
            return items;
        }

        public void setItems(final List<Object> items) {
            this.items = items;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(final String tail) {
            this.tail = tail;
        }
    }

    private static XmlParser[] defaultParsers() {
        return new XmlParser[] { new XmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.StAX),
                new AbacusXmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.SAX) };
    }

    private static void assertMutableCopy(final Collection<?> expected, final Object actual, final String label) {
        assertNotNull(actual, label);
        assertTrue(Modifier.isPublic(actual.getClass().getModifiers()), () -> actual.getClass().getName() + " read from " + label);
        @SuppressWarnings("unchecked")
        final Collection<Object> collection = assertInstanceOf(Collection.class, actual, label);
        final List<Object> sortedExpected = new ArrayList<>(expected);
        final List<Object> sortedActual = new ArrayList<>(collection);
        sortedExpected.sort(null);
        sortedActual.sort(null);
        assertEquals(sortedExpected, sortedActual, label);
        collection.add("marker");
        collection.remove("marker");
    }

    // Map views (HashMap.keySet()/values(), IdentityHashMap.keySet()) are recorded by the writer with their runtime class, which the
    // default parsers accept. With --add-opens java.base/java.util (as this suite runs) r108 built a fresh view of an empty map
    // from that metadata and failed with UnsupportedOperationException at the first element, at the root, as a list item, as a
    // map value and as a bean property, on all five backends; now every slot reads a mutable public copy.
    @Test
    @SuppressWarnings("unchecked")
    public void testMapViewMetadataFromDefaultParsersTakesTheMutableFallback() {
        final Map<String, Integer> hashMap = new HashMap<>(Map.of("a", 1, "b", 2));
        final Map<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put("a", 1);
        final List<Collection<?>> views = List.of(hashMap.keySet(), hashMap.values(), identityMap.keySet());

        for (final XmlParser parser : defaultParsers()) {
            for (final Collection<?> view : views) {
                final XmlSerConfig withTypes = XmlSerConfig.create().setWriteTypeInfo(true);
                final String name = parser.getClass().getSimpleName() + " " + view.getClass().getName();

                String xml = parser.serialize(view, withTypes);
                assertTrue(xml.contains(view.getClass().getCanonicalName()), xml);
                assertMutableCopy(view, parser.deserialize(xml, null, view instanceof Set ? Set.class : Collection.class), name + " root: " + xml);
                assertMutableCopy(view, parser.deserialize(xml, null, Object.class), name + " root as Object: " + xml);

                final List<Object> list = new ArrayList<>();
                list.add(view);
                list.add("end");
                xml = parser.serialize(list, withTypes);
                final List<Object> items = parser.deserialize(xml, null, Type.of("List<Object>"));
                assertMutableCopy(view, items.get(0), name + " item: " + xml);
                assertEquals("end", items.get(1), xml);

                final Map<String, Object> map = new LinkedHashMap<>();
                map.put("k", view);
                map.put("tail", "end");
                xml = parser.serialize(map, withTypes);
                final Map<String, Object> values = parser.deserialize(xml, null, Type.of("LinkedHashMap<String, Object>"));
                assertMutableCopy(view, values.get("k"), name + " map value: " + xml);
                assertEquals("end", values.get("tail"), xml);

                final G141ViewBean bean = new G141ViewBean();
                bean.setObj(view);
                bean.setColl((Collection<Object>) view);
                if (view instanceof Set) {
                    bean.setSet((Set<Object>) view);
                }
                bean.setItems(new ArrayList<>(List.of(view)));
                bean.setTail("end");
                xml = parser.serialize(bean, withTypes);
                final G141ViewBean back = parser.deserialize(xml, null, G141ViewBean.class);
                assertMutableCopy(view, back.getObj(), name + " Object property: " + xml);
                assertMutableCopy(view, back.getColl(), name + " Collection property: " + xml);
                if (view instanceof Set) {
                    assertMutableCopy(view, back.getSet(), name + " Set property: " + xml);
                }
                assertMutableCopy(view, back.getItems().get(0), name + " List<Object> element: " + xml);
                assertEquals("end", back.getTail(), xml);
            }
        }
    }

    private static class G141PrivateList extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
    }

    // The rule's boundaries: anonymous JDK classes (AbstractMap's key/value views) are outside the public API; arrays,
    // interfaces, abstract classes, proxies and application classes of the unnamed module (public, private or anonymous)
    // are not JDK implementation classes. getConcreteClass keeps a constructible application class whatever its visibility.
    @Test
    public void testNonPublicJdkContainerRuleBoundaries() {
        final Map<Object, Object> abstractMap = new java.util.AbstractMap<>() {
            @Override
            public Set<Map.Entry<Object, Object>> entrySet() {
                return java.util.Collections.emptySet();
            }
        };
        assertTrue(AbstractParser.isNonPublicJdkContainerClass(abstractMap.keySet().getClass()));
        assertTrue(AbstractParser.isNonPublicJdkContainerClass(abstractMap.values().getClass()));
        assertSame(Set.class, AbstractParser.getConcreteClass(abstractMap.keySet().getClass(), Set.class));

        final Class<?> proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { List.class }, (p, m, a) -> null)
                .getClass();
        final Class<?> anonymous = new ArrayList<Object>() {
            private static final long serialVersionUID = 1L;
        }.getClass();
        for (final Class<?> cls : List.of(Object[].class, ArrayList[].class, java.util.Collections.emptyList().getClass().arrayType(), List.class,
                java.util.SortedMap.class, java.util.AbstractList.class, java.util.AbstractMap.class, proxy, anonymous, G141PrivateList.class,
                com.landawn.abacus.util.BiMap.class)) {
            assertFalse(AbstractParser.isNonPublicJdkContainerClass(cls), cls.getName());
        }

        assertSame(G141PrivateList.class, AbstractParser.getConcreteClass(G141PrivateList.class, List.class));
        assertSame(G141PrivateList.class, AbstractParser.newPropInstance(List.class, G141PrivateList.class).getClass());
    }

    // ---- deep review 2026-10-27 verify G141 end ----
}
