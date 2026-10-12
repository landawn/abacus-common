package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@SuppressWarnings("serial")
public class AnonymousContainerParserTest extends TestBase {
    private static int outerCalls;
    private static int initializerCalls;

    public static class Owner {
        public Owner() {
            outerCalls++;
        }

        public List<String> list() {
            return new ArrayList<>() {
                {
                    initializerCalls++;
                    add("x");
                }
            };
        }

        public Map<String, Integer> map() {
            return new HashMap<>() {
                {
                    initializerCalls++;
                    put("x", 1);
                }
            };
        }

        public static List<String> staticList() {
            return new ArrayList<>() {
                {
                    initializerCalls++;
                    add("x");
                }
            };
        }

        public static Map<String, Integer> staticMap() {
            return new HashMap<>() {
                {
                    initializerCalls++;
                    put("x", 1);
                }
            };
        }
    }

    public static class Holder {
        public Object value;
    }

    @Test
    public void existingAnonymousContainersCanStillBeSerializedAndPopulated() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final Owner owner = new Owner();
        final List<String> list = owner.list();
        final Map<String, Integer> map = owner.map();
        final int outerBefore = outerCalls;
        final int initializerBefore = initializerCalls;
        assertEquals("[\"x\"]", parser.serialize(list));
        assertEquals(Map.of("x", 1), parser.deserialize(parser.serialize(map), Map.class));
        parser.parseInto("[\"y\"]", list);
        parser.parseInto("{\"y\":2}", map);
        assertEquals(List.of("x", "y"), list);
        assertEquals(Map.of("x", 1, "y", 2), map);
        assertEquals(outerBefore, outerCalls);
        assertEquals(initializerBefore, initializerCalls);
    }

    // Parse both nonempty and empty documents: even an empty destination must not silently contain initializer data.
    @Test
    public void jsonRejectsRootAndNestedTargetsFromEverySource(@TempDir final Path directory) throws Exception {
        final JsonParser parser = ParserFactory.createJsonParser();
        final Owner owner = new Owner();
        // Static-context targets must fail without constructing Owner, just like instance-context targets.
        for (final Object original : new Object[] { owner.list(), owner.map(), Owner.staticList(), Owner.staticMap() }) {
            final Class<?> target = original.getClass();
            final int outerBefore = outerCalls;
            final int initializerBefore = initializerCalls;
            final String[] inputs = original instanceof Map ? new String[] { "{}", "{\"y\":2}" } : new String[] { "[]", "[\"x\"]" };
            for (final String json : inputs) {
                final Path file = directory.resolve("anonymous.json");
                Files.writeString(file, json);
                assertThrows(IllegalArgumentException.class, () -> parser.parse(json, null, target));
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(json, target));
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new StringReader(json), target));
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), target));
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(file.toFile(), target));
                final JsonDeserConfig config = JsonDeserConfig.create().setValueType("value", target);
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize("{\"value\":" + json + "}", config, Holder.class));
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize("{\"value\":" + json + "}", config, Map.class));
                assertThrows(IllegalArgumentException.class, () -> parser.stream("[" + json + "]", Type.of(target)).toList());
            }
            assertEquals(outerBefore, outerCalls);
            assertEquals(initializerBefore, initializerCalls);
        }
    }

    @Test
    public void xmlRejectsAnonymousContainersAcrossBackends(@TempDir final Path directory) throws Exception {
        final Owner owner = new Owner();
        final List<String> list = owner.list();
        final Map<String, Integer> map = owner.map();
        final List<String> staticList = Owner.staticList();
        final Map<String, Integer> staticMap = Owner.staticMap();
        for (final XmlParserType backend : XmlParserType.values()) {
            final List<XmlParser> parsers = new ArrayList<>();
            parsers.add(new AbacusXmlParserImpl(backend, null, null, Set.of(Holder.class)));
            if (backend != XmlParserType.SAX) {
                parsers.add(new XmlParserImpl(backend, null, null, Set.of(Holder.class)));
            }
            for (final XmlParser parser : parsers) {
                for (final Object original : new Object[] { list, map, staticList, staticMap }) {
                    final int outerBefore = outerCalls;
                    final int initializerBefore = initializerCalls;
                    // Serialize an ordinary container so the input does not name or require approval for the anonymous type.
                    final Object plain = original instanceof Map ? new HashMap<>(map) : new ArrayList<>(list);
                    final String xml = parser.serialize(plain);
                    final Class<?> target = original.getClass();
                    final Path file = directory.resolve("anonymous.xml");
                    Files.writeString(file, xml);
                    assertThrows(IllegalArgumentException.class, () -> parser.deserialize(xml, target), backend.name());
                    assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new StringReader(xml), target), backend.name());
                    assertThrows(IllegalArgumentException.class,
                            () -> parser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), target), backend.name());
                    assertThrows(IllegalArgumentException.class, () -> parser.deserialize(file.toFile(), target), backend.name());
                    if (backend == XmlParserType.DOM) {
                        final org.w3c.dom.Node node = com.landawn.abacus.util.XmlUtil.createDOMParser()
                                .parse(new org.xml.sax.InputSource(new StringReader(xml)))
                                .getDocumentElement();
                        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(node, target));
                    }
                    final Holder holder = new Holder();
                    holder.value = plain;
                    final String nestedXml = parser.serialize(holder);
                    final XmlDeserConfig config = XmlDeserConfig.create().setValueType("value", target);
                    assertThrows(IllegalArgumentException.class, () -> parser.deserialize(nestedXml, config, Holder.class), backend.name());
                    assertEquals(outerBefore, outerCalls);
                    assertEquals(initializerBefore, initializerCalls);
                }
            }
        }
    }

    private static List<XmlParser> xmlParsers(final XmlParserType backend) {
        final List<XmlParser> parsers = new ArrayList<>();
        parsers.add(new AbacusXmlParserImpl(backend, null, null, Set.of(Holder.class)));
        if (backend != XmlParserType.SAX) {
            parsers.add(new XmlParserImpl(backend, null, null, Set.of(Holder.class)));
        }
        return parsers;
    }

    private static boolean readerFactoriesRegistered;
    private static int anonymousMapFactoryCalls;

    // Registrations are global and permanent, so they are made once per JVM; the fixtures are used by no other test.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static synchronized void registerReaderFactoriesOnce() {
        if (!readerFactoriesRegistered) {
            assertTrue(com.landawn.abacus.util.Suppliers.registerForMap((Class) testfixtures.SupplierReviewFixtures.prefilledAnonymousMap().getClass(),
                    (java.util.function.Supplier) () -> {
                        anonymousMapFactoryCalls++;
                        return testfixtures.SupplierReviewFixtures.prefilledAnonymousMap();
                    }));
            assertTrue(com.landawn.abacus.util.Suppliers.registerForCollection((Class) testfixtures.SupplierReviewFixtures.prefilledAnonymousList().getClass(),
                    (java.util.function.Supplier) testfixtures.SupplierReviewFixtures::prefilledAnonymousList));
            assertTrue(com.landawn.abacus.util.Suppliers.registerForMap((Class) testfixtures.SupplierReviewFixtures.RegisteredNamedMap.class,
                    (java.util.function.Supplier) () -> new testfixtures.SupplierReviewFixtures.RegisteredNamedMap(0)));
            assertTrue(com.landawn.abacus.util.Suppliers.registerForMap((Class) testfixtures.SupplierReviewFixtures.RegisteredSortedMap.class,
                    (java.util.function.Supplier) () -> new testfixtures.SupplierReviewFixtures.RegisteredSortedMap(String.CASE_INSENSITIVE_ORDER)));
            assertTrue(com.landawn.abacus.util.Suppliers.registerForCollection((Class) testfixtures.SupplierReviewFixtures.RegisteredNamedList.class,
                    (java.util.function.Supplier) () -> new testfixtures.SupplierReviewFixtures.RegisteredNamedList(0)));
            readerFactoriesRegistered = true;
        }
    }

    // StAX created a target with N.newMap/N.newCollection, which use Suppliers registrations: a registered supplier
    // returning a pre-filled anonymous container made StAX read {x=y} as {x=y, initializer=7}, while DOM, SAX and JSON
    // rejected the class. Every reader now rejects an anonymous container before consulting any factory.
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void anonymousTargetsAreRejectedEvenWithRegisteredFactories() {
        registerReaderFactoriesOnce();
        final Class mapType = testfixtures.SupplierReviewFixtures.prefilledAnonymousMap().getClass();
        final Class listType = testfixtures.SupplierReviewFixtures.prefilledAnonymousList().getClass();
        for (final XmlParserType backend : XmlParserType.values()) {
            for (final XmlParser parser : xmlParsers(backend)) {
                final String name = backend + " " + parser.getClass().getSimpleName();
                final String mapXml = parser.serialize(new HashMap<>(Map.of("x", "y")));
                final String listXml = parser.serialize(new ArrayList<>(List.of("x")));
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(mapXml, mapType), name);
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(listXml, listType), name);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> com.landawn.abacus.util.N.fromJson("{\"x\":\"y\"}", mapType));
        assertThrows(IllegalArgumentException.class, () -> com.landawn.abacus.util.N.fromJson("[\"x\"]", listType));
    }

    // Map.class uses the configured implementation, which must reject an anonymous class before its registered supplier
    // runs: previously even {} acquired the supplier's initializer entry.
    @Test
    public void configuredAnonymousMapsAreRejectedFromEveryJsonSource(@TempDir final Path directory) throws Exception {
        registerReaderFactoriesOnce();
        final JsonParser parser = ParserFactory.createJsonParser();
        final Class<? extends Map> target = testfixtures.SupplierReviewFixtures.prefilledAnonymousMap().getClass();
        final JsonDeserConfig config = JsonDeserConfig.create().setMapInstanceType(target);
        final int callsBefore = anonymousMapFactoryCalls;
        for (final String json : new String[] { "{}", "{\"x\":\"y\"}" }) {
            final Path file = directory.resolve("configured-map.json");
            Files.writeString(file, json);
            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> parser.deserialize(json, config, Map.class));
            assertTrue(failure.getMessage().contains(target.getName()), failure.getMessage());
            assertThrows(IllegalArgumentException.class, () -> parser.parse(json, config, Map.class));
            assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new StringReader(json), config, Map.class));
            assertThrows(IllegalArgumentException.class,
                    () -> parser.deserialize(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), config, Map.class));
            assertThrows(IllegalArgumentException.class, () -> parser.deserialize(file.toFile(), config, Map.class));
        }
        assertEquals(callsBefore, anonymousMapFactoryCalls);
    }

    @Test
    public void configuredAnonymousMapsAreRejectedForNestedJsonTargets() {
        registerReaderFactoriesOnce();
        final JsonParser parser = ParserFactory.createJsonParser();
        final JsonDeserConfig config = JsonDeserConfig.create().setMapInstanceType(testfixtures.SupplierReviewFixtures.prefilledAnonymousMap().getClass());
        final int callsBefore = anonymousMapFactoryCalls;
        for (final String json : new String[] { "{}", "{\"x\":\"y\"}" }) {
            // A concrete root bypasses the setting, so these failures must come from the nested Map target.
            assertThrows(IllegalArgumentException.class,
                    () -> parser.deserialize("{\"value\":" + json + "}", config.copy().setMapValueType(Map.class), HashMap.class));
            assertThrows(IllegalArgumentException.class,
                    () -> parser.deserialize("{\"value\":" + json + "}", config.copy().setValueType("value", Map.class), Holder.class));
            assertThrows(IllegalArgumentException.class, () -> parser.deserialize("[" + json + "]", config.copy().setElementType(Map.class), ArrayList.class));
        }
        assertEquals(callsBefore, anonymousMapFactoryCalls);
    }

    @Test
    public void configuredNamedMapFactoriesAndExistingJsonOutputsRemainSupported() {
        registerReaderFactoriesOnce();
        final JsonParser parser = ParserFactory.createJsonParser();
        final JsonDeserConfig config = JsonDeserConfig.create().setMapInstanceType(testfixtures.SupplierReviewFixtures.RegisteredNamedMap.class);
        final Map<?, ?> named = parser.deserialize("{\"x\":\"y\"}", config, Map.class);
        assertEquals(testfixtures.SupplierReviewFixtures.RegisteredNamedMap.class, named.getClass());
        assertEquals(Map.of("x", "y"), named);
        final Map<?, ?> nested = parser.deserialize("{\"value\":{\"x\":\"y\"}}", config.copy().setMapValueType(Map.class), HashMap.class);
        assertEquals(testfixtures.SupplierReviewFixtures.RegisteredNamedMap.class, nested.get("value").getClass());
        assertEquals(Map.of("x", "y"), nested.get("value"));

        config.setMapInstanceType(testfixtures.SupplierReviewFixtures.RegisteredSortedMap.class);
        final java.util.SortedMap<?, ?> sorted = (java.util.SortedMap<?, ?>) parser.deserialize("{\"x\":\"y\"}", config, Map.class);
        assertSame(String.CASE_INSENSITIVE_ORDER, sorted.comparator());
        assertEquals("y", sorted.get("X"));
        config.setMapInstanceType(java.util.SortedMap.class);
        assertInstanceOf(java.util.TreeMap.class, parser.deserialize("{}", config, Map.class));

        config.setMapInstanceType(testfixtures.SupplierReviewFixtures.prefilledAnonymousMap().getClass());
        final int callsBefore = anonymousMapFactoryCalls;
        assertEquals(Map.of("x", "y"), parser.deserialize("{\"x\":\"y\"}", config, HashMap.class));
        final Map<String, Object> existing = testfixtures.SupplierReviewFixtures.prefilledAnonymousMap();
        parser.parseInto("{\"x\":\"y\"}", config, existing);
        assertEquals(Map.of("initializer", "7", "x", "y"), existing);
        assertEquals(callsBefore, anonymousMapFactoryCalls);
    }

    // The anonymous-class rejection must not take a named class away from its registered factory on the paths that use
    // factories (StAX): one with only a constructor argument cannot be created otherwise, and a sorted map whose factory
    // supplies a comparator would silently be read with natural ordering.
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void registeredFactoriesForNamedTargetsKeepWorkingOnStax() {
        registerReaderFactoriesOnce();
        final Class namedType = testfixtures.SupplierReviewFixtures.RegisteredNamedMap.class;
        final Class sortedType = testfixtures.SupplierReviewFixtures.RegisteredSortedMap.class;
        for (final XmlParser parser : xmlParsers(XmlParserType.StAX)) {
            final String name = parser.getClass().getSimpleName();
            final String mapXml = parser.serialize(new HashMap<>(Map.of("x", "y")));
            final Map<?, ?> named = (Map<?, ?>) parser.deserialize(mapXml, namedType);
            assertEquals(namedType, named.getClass(), name);
            assertEquals(Map.of("x", "y"), named, name);
            final java.util.TreeMap<String, Object> sorted = (java.util.TreeMap<String, Object>) parser.deserialize(mapXml, sortedType);
            assertEquals(sortedType, sorted.getClass(), name);
            assertSame(String.CASE_INSENSITIVE_ORDER, sorted.comparator(), name);
            assertEquals("y", sorted.get("X"), name);
        }
        // Through the default XML parser (StAX) as well.
        assertSame(String.CASE_INSENSITIVE_ORDER, ((java.util.TreeMap<String, Object>) com.landawn.abacus.util.N
                .fromXml(xmlParsers(XmlParserType.StAX).get(0).serialize(new HashMap<>(Map.of("x", "y"))), sortedType)).comparator());
    }

    public static class ListHolder {
        private testfixtures.SupplierReviewFixtures.RegisteredNamedList names;

        public testfixtures.SupplierReviewFixtures.RegisteredNamedList getNames() {
            return names;
        }

        public void setNames(final testfixtures.SupplierReviewFixtures.RegisteredNamedList names) {
            this.names = names;
        }
    }

    // AbacusXmlParserImpl's StAX reader creates a collection-typed bean property through N.newCollection, so a named property
    // class that can only be created by its registered factory still reads there. (Its DOM/SAX readers and JSON create
    // properties through N.newInstance and never used registrations for a concrete class; XmlParserImpl's default text form
    // does too, while its child-element form is covered by xmlParserImplElementFormCollectionsUseFactoriesForNamedTypesOnly.)
    @Test
    public void registeredFactoriesForNamedPropertyCollectionsKeepWorking() {
        registerReaderFactoriesOnce();
        final ListHolder holder = new ListHolder();
        holder.setNames(new testfixtures.SupplierReviewFixtures.RegisteredNamedList(0));
        holder.getNames().add("x");
        final Set<Class<?>> approved = Set.of(Holder.class, ListHolder.class, testfixtures.SupplierReviewFixtures.RegisteredNamedList.class);
        final XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX, null, null, approved);
        final ListHolder read = parser.deserialize(parser.serialize(holder), ListHolder.class);
        assertEquals(testfixtures.SupplierReviewFixtures.RegisteredNamedList.class, read.getNames().getClass());
        assertEquals(List.of("x"), read.getNames());
    }

    // XmlParserImpl reads a collection property written as child elements (<names><e>x</e></names>, not its default JSON
    // text) through newReaderCollection on both of its backends: a named class keeps its registered factory there, and an
    // anonymous value type is rejected before its registered (pre-filling) factory is consulted.
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void xmlParserImplElementFormCollectionsUseFactoriesForNamedTypesOnly() {
        registerReaderFactoriesOnce();
        final Set<Class<?>> approved = Set.of(Holder.class, ListHolder.class, testfixtures.SupplierReviewFixtures.RegisteredNamedList.class);
        final Class anonymousListType = testfixtures.SupplierReviewFixtures.prefilledAnonymousList().getClass();
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("value", anonymousListType);
        for (final XmlParserType backend : new XmlParserType[] { XmlParserType.StAX, XmlParserType.DOM }) {
            final XmlParserImpl parser = new XmlParserImpl(backend, null, null, approved);
            final ListHolder read = parser.deserialize("<listHolder><names><e>x</e></names></listHolder>", ListHolder.class);
            assertEquals(testfixtures.SupplierReviewFixtures.RegisteredNamedList.class, read.getNames().getClass(), backend.name());
            assertEquals(List.of("x"), read.getNames(), backend.name());
            assertThrows(IllegalArgumentException.class, () -> parser.deserialize("<holder><value><e>x</e></value></holder>", config, Holder.class),
                    backend.name());
        }
    }

    public static class DayMap extends java.util.EnumMap<java.time.DayOfWeek, Integer> {
        private static final long serialVersionUID = 1L;

        public DayMap() {
            super(java.time.DayOfWeek.class);
        }
    }

    private static java.util.EnumMap<java.time.DayOfWeek, Integer> staticAnonymousEnumMap() {
        return new java.util.EnumMap<>(java.time.DayOfWeek.class) {
            private static final long serialVersionUID = 1L;
        };
    }

    // The EnumMap branch of every reader created a plain EnumMap for any EnumMap subclass: JSON and XmlParserImpl returned
    // a map that is not an instance of the requested class, and an anonymous subclass escaped the anonymous-map rejection.
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void enumMapSubclassTargetsAreCreatedAsThemselves() {
        final Class anonymousType = staticAnonymousEnumMap().getClass();
        final JsonParser json = ParserFactory.createJsonParser();
        final JsonDeserConfig jsonConfig = JsonDeserConfig.create().setMapKeyType(java.time.DayOfWeek.class).setMapValueType(Integer.class);
        final String jsonText = "{\"MONDAY\":1}";
        assertThrows(IllegalArgumentException.class, () -> json.deserialize(jsonText, jsonConfig, anonymousType));
        final DayMap jsonDayMap = json.deserialize(jsonText, jsonConfig, DayMap.class);
        assertEquals(DayMap.class, jsonDayMap.getClass());
        assertEquals(1, jsonDayMap.get(java.time.DayOfWeek.MONDAY));
        assertEquals(java.util.EnumMap.class, json.deserialize(jsonText, jsonConfig, java.util.EnumMap.class).getClass());

        final XmlDeserConfig xmlConfig = XmlDeserConfig.create().setMapKeyType(java.time.DayOfWeek.class).setMapValueType(Integer.class);
        for (final XmlParserType backend : XmlParserType.values()) {
            for (final XmlParser parser : xmlParsers(backend)) {
                final String name = backend + " " + parser.getClass().getSimpleName();
                final String xml = parser.serialize(new java.util.EnumMap<>(Map.of(java.time.DayOfWeek.MONDAY, 1)));
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(xml, xmlConfig, anonymousType), name);
                final DayMap dayMap = parser.deserialize(xml, xmlConfig, DayMap.class);
                assertEquals(DayMap.class, dayMap.getClass(), name);
                assertEquals(1, dayMap.get(java.time.DayOfWeek.MONDAY), name);
            }
        }
    }

    // A configured key enum that differs from the one an EnumMap subclass declares failed in EnumMap.put with a bare
    // ClassCastException; it is now reported as a ParsingException naming both enums.
    @Test
    public void enumMapSubclassRejectsAConfiguredKeyOfAnotherEnum() {
        final JsonDeserConfig jsonConfig = JsonDeserConfig.create().setMapKeyType(java.time.Month.class).setMapValueType(Integer.class);
        final com.landawn.abacus.exception.ParsingException jsonFailure = assertThrows(com.landawn.abacus.exception.ParsingException.class,
                () -> ParserFactory.createJsonParser().deserialize("{\"JANUARY\":1}", jsonConfig, DayMap.class));
        assertTrue(jsonFailure.getMessage().contains("java.time.DayOfWeek"), jsonFailure.getMessage());
        assertTrue(jsonFailure.getMessage().contains("Month"), jsonFailure.getMessage());

        final XmlDeserConfig xmlConfig = XmlDeserConfig.create().setMapKeyType(java.time.Month.class).setMapValueType(Integer.class);
        for (final XmlParserType backend : XmlParserType.values()) {
            for (final XmlParser parser : xmlParsers(backend)) {
                final String xml = parser.serialize(new java.util.EnumMap<>(Map.of(java.time.Month.JANUARY, 1)));
                assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> parser.deserialize(xml, xmlConfig, DayMap.class),
                        backend + " " + parser.getClass().getSimpleName());
            }
        }
    }

    // The EnumMap branch is also taken for nested values: an anonymous EnumMap value type is rejected there too.
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void nestedAnonymousEnumMapValuesAreRejected() {
        final Class anonymousType = staticAnonymousEnumMap().getClass();
        final JsonDeserConfig jsonConfig = JsonDeserConfig.create().setValueType("value", anonymousType);
        assertThrows(IllegalArgumentException.class,
                () -> ParserFactory.createJsonParser().deserialize("{\"value\":{\"MONDAY\":1}}", jsonConfig, Holder.class));

        final Holder holder = new Holder();
        holder.value = new java.util.EnumMap<>(Map.of(java.time.DayOfWeek.MONDAY, 1));
        final XmlDeserConfig xmlConfig = XmlDeserConfig.create().setValueType("value", anonymousType);
        for (final XmlParserType backend : XmlParserType.values()) {
            for (final XmlParser parser : xmlParsers(backend)) {
                final String xml = parser.serialize(holder);
                assertThrows(IllegalArgumentException.class, () -> parser.deserialize(xml, xmlConfig, Holder.class),
                        backend + " " + parser.getClass().getSimpleName());
            }
        }
    }
}
