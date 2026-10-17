package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u;

/**
 * Regression coverage for the six XML review findings: array runtime types, independent map-value schemas,
 * bean declarations before metadata refinement, configured open slots, bean-array text, and immutable map keys.
 * Also covers the four follow-up array findings: String element configuration, structured configured components,
 * conflicting array metadata, and preservation of declared primitive-array text conversions.
 * Further regressions cover escaped boolean tokens, primitive numeric JSON tokens and null defaults, and Unicode
 * whitespace around String-array text, including root arrays and text split across XML character events.
 * The 2026-10-05 evening findings are covered at the end: Unicode padding around reference-array and collection
 * text, named value overrides inside list-valued (Spring {@code MultiValueMap}) maps in compact and structured
 * form, declared scalar values under list-valued metadata, nested open array components under element
 * configuration, compact map text inside list elements, and primitive-list text read through the array codecs.
 * The two open issues found afterwards are covered last: a primitive array adopted from metadata ignores element
 * configuration, and a scalar per-key override on a declared list value names its element type in both forms. The last
 * two methods cover the remaining backend differences: a standard-XML map value needs its {@code <map>} wrapper, and
 * specialized map items named by list or array metadata survive child elements. Finally, untyped children of an open
 * {@code Object} slot in standard XML are read by their element names on both standard readers, and Abacus metadata on
 * a {@code <list>} or {@code <array>} element alone types the items. The next two methods cover typed items of open lists
 * keeping their types on the standard readers, and untyped beans in open slots reading as maps on the Abacus readers. The
 * next three cover the typed wrapper child of an Object property, empty elements, and untyped beans inside arrays and
 * nested values. The next two cover per-key value types in root compact text and an Object override on a slot that
 * carries map metadata. The next two cover per-key value types in the compact text of array values and the global map
 * value type inside compact text. The last methods cover the reviewer's follow-up items: declared scalar types in
 * compact map entries, root arrays under metadata, a present null under a scalar override, a global scalar value
 * type against specialized metadata, compact root maps, configured components of nested open elements, and blank root
 * arrays. The third review's items follow: a container component applied once, per-key overrides keeping the declared
 * container shape and the array overrides on every backend, root nested open arrays, requested container arguments at
 * the root, a present null under any non-List override, and the elements of an abstract reference array value under metadata.
 * The fourth review's items end the class: declared container items keep the configuration inside, the standard readers' item
 * type for nested array slots, and the global map value type in nested maps of compact text.
 * Assertions describe the required behavior, so unresolved production defects remain visible as test failures.
 */
@Tag("unit")
public class CodexXmlParserTest extends TestBase {
    private static final String NUMBERS = "[\"1\",\"2\"]";
    private static final String NUMBER_ELEMENTS = "<e>1</e><e>2</e>";

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
            final Set<Class<?>> allowed = Set.of(ObjectBean.class, ListBean.class, ArrayBean.class, LongArrayBean.class, StringArrayBean.class,
                    CharArrayBean.class, BooleanArrayBean.class, ArrayItem.class, ByteArrayBean.class, ShortArrayBean.class, IntArrayBean.class,
                    PrimitiveLongArrayBean.class, FloatArrayBean.class, DoubleArrayBean.class, LinkedMultiValueMap.class, Person.class);
            return abacus ? new AbacusXmlParserImpl(parserType, null, null, allowed) : new XmlParserImpl(parserType, null, null, allowed);
        }
    }

    enum Form {
        TEXT, CONTAINER_TEXT, CHILDREN
    }

    enum Wrapper {
        NONE("values", null),
        JDK("jdk", "java.util.Optional"),
        OPTIONAL("optional", "com.landawn.abacus.util.u.Optional"),
        NULLABLE("nullable", "com.landawn.abacus.util.u.Nullable");

        final String property;
        final String typeName;

        Wrapper(final String property, final String typeName) {
            this.property = property;
            this.typeName = typeName;
        }

        String schema(final String payload) {
            return this == NONE ? payload : typeName + "<" + payload + ">";
        }

        Object unwrap(final Object value) {
            return switch (this) {
                case NONE -> value;
                case JDK -> assertInstanceOf(Optional.class, value).orElseThrow();
                case OPTIONAL -> assertInstanceOf(u.Optional.class, value).get();
                case NULLABLE -> assertInstanceOf(u.Nullable.class, value).get();
            };
        }

        Object wrap(final Object value) {
            return switch (this) {
                case NONE -> value;
                case JDK -> Optional.of(value);
                case OPTIONAL -> u.Optional.of(value);
                case NULLABLE -> u.Nullable.of(value);
            };
        }
    }

    enum ArrayContent {
        NUMBERS, EMPTY, WITH_NULL
    }

    enum KeyKind {
        LIST, SET, MAP
    }

    private static Stream<Form> forms(final Backend backend) {
        return backend.abacus ? Stream.of(Form.values()) : Stream.of(Form.TEXT, Form.CHILDREN);
    }

    private static String attribute(final String metadata) {
        return metadata.isEmpty() ? "" : " type=\"" + metadata.replace("<", "&lt;").replace(">", "&gt;") + "\"";
    }

    private static String element(final String name, final String metadata, final String body) {
        return "<" + name + attribute(metadata) + ">" + body + "</" + name + ">";
    }

    private static String sequenceBody(final Backend backend, final Form form, final String container, final String text, final String children) {
        return switch (form) {
            case TEXT -> text;
            case CONTAINER_TEXT -> element(container, "", text);
            case CHILDREN -> backend.abacus ? element(container, "", children) : children;
        };
    }

    private static String mapValueDocument(final Backend backend, final String key, final String metadata, final String body) {
        return backend.abacus ? "<map><entry><key>" + key + "</key>" + element("value", metadata, body) + "</entry></map>"
                : "<map>" + element(key, metadata, body) + "</map>";
    }

    private static String beanDocument(final Class<?> beanClass, final String property, final String metadata, final String body) {
        // Root metadata enables the ordinary metadata-aware bean path on every backend.
        return element("bean", beanClass.getName(), element(property, metadata, body));
    }

    private static String listMetadata(final boolean immutable) {
        return (immutable ? List.of(1, 2).getClass().getName() : "ArrayList") + "<Integer>";
    }

    private static Object beanProperty(final Object bean, final Wrapper wrapper) throws ReflectiveOperationException {
        return bean.getClass().getField(wrapper.property).get(bean);
    }

    static Stream<Arguments> arrayCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final Backend backend : Backend.values()) {
            for (final Form form : forms(backend).toList()) {
                for (final Wrapper wrapper : Wrapper.values()) {
                    for (final Class<?> component : List.of(Long.class, int.class)) {
                        for (final ArrayContent content : ArrayContent.values()) {
                            // An empty standard property element denotes its default value, not an empty array.
                            // Use explicit [] there; an Abacus <array> child can represent an empty array separately.
                            if (content == ArrayContent.EMPTY && form == Form.CHILDREN && !backend.abacus) {
                                continue;
                            }
                            // Primitive null conversion has a separate policy; here null tests the reference component.
                            if (content != ArrayContent.WITH_NULL || component == Long.class) {
                                cases.add(Arguments.of(backend, form, wrapper, component, content));
                            }
                        }
                    }
                }
            }
        }
        return cases.stream();
    }

    private static String arrayBody(final Backend backend, final Form form, final Class<?> component, final ArrayContent content) {
        final String text = switch (content) {
            // Keep primitive tests focused on boxing and array identity rather than quoted-number codec differences.
            case NUMBERS -> component.isPrimitive() ? "[1,2]" : NUMBERS;
            case EMPTY -> "[]";
            case WITH_NULL -> "[\"1\",null,\"2\"]";
        };
        final String children = switch (content) {
            case NUMBERS -> NUMBER_ELEMENTS;
            case EMPTY -> "";
            case WITH_NULL -> "<e>1</e><e isNull=\"true\"/><e>2</e>";
        };
        return sequenceBody(backend, form, "array", text, children);
    }

    private static Object[] expectedArray(final Class<?> component, final ArrayContent content) {
        if (content == ArrayContent.EMPTY) {
            return new Object[0];
        }
        if (content == ArrayContent.WITH_NULL) {
            return new Object[] { 1L, null, 2L };
        }
        return component == Long.class ? new Object[] { 1L, 2L } : new Object[] { 1, 2 };
    }

    private static void assertOpenArray(final Object actual, final Object[] expected, final String xml) {
        assertNotNull(actual, xml);
        // Assignability alone accepts Long[] and misses the ArrayStoreException regression.
        assertAll(xml, () -> assertEquals(Object[].class, actual.getClass(), "Configuration must not change the reified array class"),
                () -> assertArrayEquals(expected, assertInstanceOf(Object[].class, actual)), () -> {
                    final Object[] array = assertInstanceOf(Object[].class, actual);
                    if (array.length > 0) {
                        assertDoesNotThrow(() -> array[0] = "replacement", "An Object[] must accept a value of another class");
                    }
                });
    }

    // Finding 1: configuration determines the values, including boxed primitive values, without narrowing Object[].
    @ParameterizedTest(name = "{0}, {1}, {2}, component={3}, {4}")
    @MethodSource("arrayCases")
    void mapObjectArraysKeepTheirDeclaredRuntimeType(final Backend backend, final Form form, final Wrapper wrapper, final Class<?> component,
            final ArrayContent content) {
        final String xml = mapValueDocument(backend, "k", "", arrayBody(backend, form, component, content));
        final Map<?, ?> actual = backend.parser()
                .deserialize(xml, XmlDeserConfig.create().setElementType(component), Type.of("Map<String," + wrapper.schema("Object[]") + ">"));
        assertOpenArray(wrapper.unwrap(actual.get("k")), expectedArray(component, content), xml);
    }

    // Findings 1 and 5: bean properties must take the same path for compact text and structured arrays.
    @ParameterizedTest(name = "{0}, {1}, {2}, component={3}, {4}")
    @MethodSource("arrayCases")
    void beanObjectArraysUseConfigurationWithoutChangingTheirRuntimeType(final Backend backend, final Form form, final Wrapper wrapper,
            final Class<?> component, final ArrayContent content) throws ReflectiveOperationException {
        final String xml = beanDocument(ArrayBean.class, wrapper.property, "", arrayBody(backend, form, component, content));
        final ArrayBean actual = backend.parser().deserialize(xml, XmlDeserConfig.create().setElementType(component), ArrayBean.class);
        assertOpenArray(wrapper.unwrap(beanProperty(actual, wrapper)), expectedArray(component, content), xml);
    }

    static Stream<Arguments> declaredArrayCases() {
        return Stream.of(Backend.values()).flatMap(backend -> forms(backend).map(form -> Arguments.of(backend, form)));
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("declaredArrayCases")
    void declaredLongArraysKeepTheirComponentDespiteConflictingConfiguration(final Backend backend, final Form form) {
        final String xml = mapValueDocument(backend, "k", "", arrayBody(backend, form, Long.class, ArrayContent.NUMBERS));
        final Map<?, ?> actual = backend.parser().deserialize(xml, XmlDeserConfig.create().setElementType(Integer.class), Type.of("Map<String,Long[]>"));
        assertEquals(Long[].class, actual.get("k").getClass(), xml);
        assertArrayEquals(new Long[] { 1L, 2L }, (Long[]) actual.get("k"), xml);
    }

    static Stream<Arguments> mapSchemaCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final Backend backend : Backend.values()) {
            for (final Form form : List.of(Form.TEXT, Form.CHILDREN)) {
                for (final Wrapper wrapper : Wrapper.values()) {
                    for (final boolean immutable : new boolean[] { false, true }) {
                        for (final boolean stringValue : new boolean[] { false, true }) {
                            cases.add(Arguments.of(backend, form, wrapper, immutable, stringValue));
                        }
                    }
                }
            }
        }
        return cases.stream();
    }

    // Finding 2: setElementType is independent of map value types, whether supplied by metadata or declarations.
    @ParameterizedTest(name = "{0}, {1}, {2}, immutable={3}, strings={4}")
    @MethodSource("mapSchemaCases")
    void elementConfigurationDoesNotOverwriteAdoptedMapValueTypes(final Backend backend, final Form form, final Wrapper wrapper, final boolean immutable,
            final boolean stringValue) {
        final String valueType = stringValue ? "String" : "Integer";
        final String metadata = (immutable ? Map.of("x", 1).getClass().getName() : "LinkedHashMap") + "<String," + valueType + ">";
        final String scalar = stringValue ? "hello" : "1";
        final String text = "{\"x\":\"" + scalar + "\"}";
        final String entries = backend.abacus ? "<entry><key>x</key><value>" + scalar + "</value></entry>" : "<x>" + scalar + "</x>";
        final String body = form == Form.TEXT ? text : element("map", "", entries);
        final String xml = mapValueDocument(backend, "k", metadata, body);
        final XmlParser parser = backend.parser();
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final Map<?, ?> expected = stringValue ? Map.of("x", "hello") : Map.of("x", 1);

        assertAll(xml, () -> {
            final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String," + wrapper.schema("Object") + ">"));
            assertEquals(expected, wrapper.unwrap(actual.get("k")));
        }, () -> {
            final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String," + wrapper.schema("Map<String," + valueType + ">") + ">"));
            assertEquals(expected, wrapper.unwrap(actual.get("k")));
        });
    }

    static Stream<Arguments> beanMetadataCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final Backend backend : Backend.values()) {
            for (final Form form : forms(backend).toList()) {
                for (final Wrapper wrapper : Wrapper.values()) {
                    for (final boolean immutable : new boolean[] { false, true }) {
                        for (final boolean listProperty : new boolean[] { false, true }) {
                            cases.add(Arguments.of(backend, form, wrapper, immutable, listProperty));
                        }
                    }
                }
            }
        }
        return cases.stream();
    }

    // Finding 3: metadata must not turn an originally open bean slot into a declared Integer constraint.
    @ParameterizedTest(name = "{0}, {1}, {2}, immutable={3}, listProperty={4}")
    @MethodSource("beanMetadataCases")
    void beanSlotsRememberTheirDeclarationBeforeMetadataRefinement(final Backend backend, final Form form, final Wrapper wrapper, final boolean immutable,
            final boolean listProperty) throws ReflectiveOperationException {
        final Class<?> beanClass = listProperty ? ListBean.class : ObjectBean.class;
        final String body = sequenceBody(backend, form, "list", NUMBERS, NUMBER_ELEMENTS);
        final String xml = beanDocument(beanClass, wrapper.property, listMetadata(immutable), body);
        final Object actual = backend.parser().deserialize(xml, XmlDeserConfig.create().setElementType(Long.class), beanClass);
        final List<?> values = assertInstanceOf(List.class, wrapper.unwrap(beanProperty(actual, wrapper)), xml);
        assertEquals(List.of(1L, 2L), values, xml);
        assertEquals(Long.class, values.getFirst().getClass(), xml);
    }

    static Stream<Arguments> configuredSlotCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final Backend backend : Backend.values()) {
            for (final Form form : forms(backend).toList()) {
                for (final Wrapper wrapper : Wrapper.values()) {
                    for (final String metadata : List.of("", listMetadata(false), listMetadata(true))) {
                        cases.add(Arguments.of(backend, form, wrapper, metadata));
                    }
                }
            }
        }
        return cases.stream();
    }

    // Finding 4: named configuration is itself a declaration; its Object component remains open to element configuration.
    @ParameterizedTest(name = "{0}, {1}, {2}, metadata={3}")
    @MethodSource("configuredSlotCases")
    void configuredOpenMapAndBeanSlotsHonorElementConfiguration(final Backend backend, final Form form, final Wrapper wrapper, final String metadata) {
        final String body = sequenceBody(backend, form, "list", NUMBERS, NUMBER_ELEMENTS);
        final String mapXml = mapValueDocument(backend, wrapper.property, metadata, body);
        final String beanXml = beanDocument(ListBean.class, wrapper.property, metadata, body);
        final XmlParser parser = backend.parser();
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class).setValueType(wrapper.property, wrapper.schema("List<Object>"));

        assertAll(backend + " " + form + " " + metadata, () -> {
            final Map<?, ?> actual = parser.deserialize(mapXml, config, Type.of("Map<String,Object>"));
            assertEquals(List.of(1L, 2L), wrapper.unwrap(actual.get(wrapper.property)), mapXml);
        }, () -> {
            final ListBean actual = parser.deserialize(beanXml, config, ListBean.class);
            assertEquals(List.of(1L, 2L), wrapper.unwrap(beanProperty(actual, wrapper)), beanXml);
        });
    }

    @ParameterizedTest(name = "{0}, {1}, {2}, metadata={3}")
    @MethodSource("configuredSlotCases")
    void configuredExplicitComponentsStillBeatGlobalConfiguration(final Backend backend, final Form form, final Wrapper wrapper, final String metadata) {
        final String xml = mapValueDocument(backend, "k", metadata, sequenceBody(backend, form, "list", NUMBERS, NUMBER_ELEMENTS));
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class).setValueType("k", wrapper.schema("List<Long>"));
        final Map<?, ?> actual = backend.parser().deserialize(xml, config, Type.of("Map<String,Object>"));
        assertEquals(List.of(1L, 2L), wrapper.unwrap(actual.get("k")), xml);
    }

    static Stream<Arguments> mapEntityCases() {
        return Stream.of(Backend.XML_STAX, Backend.XML_DOM)
                .flatMap(backend -> forms(backend)
                        .flatMap(form -> Stream.of("", listMetadata(false), listMetadata(true)).map(metadata -> Arguments.of(backend, form, metadata))));
    }

    @ParameterizedTest(name = "{0}, {1}, metadata={2}")
    @MethodSource("mapEntityCases")
    void configuredOpenMapEntityValuesHonorElementConfiguration(final Backend backend, final Form form, final String metadata) {
        final String xml = mapValueDocument(backend, "k", metadata, sequenceBody(backend, form, "list", NUMBERS, NUMBER_ELEMENTS));
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class).setValueType("k", "List<Object>");
        final MapEntity actual = backend.parser().deserialize(xml, config, MapEntity.class);
        assertEquals(List.of(1L, 2L), actual.get("k"), xml);
    }

    static Stream<Arguments> immutableKeyCases() {
        return Stream.of(Backend.ABACUS_STAX, Backend.ABACUS_DOM, Backend.ABACUS_SAX)
                .flatMap(backend -> forms(backend).flatMap(form -> Stream.of(Wrapper.values())
                        .flatMap(wrapper -> Stream.of(KeyKind.values())
                                // Map JSON text belongs directly in the key wrapper; a <map> child requires entry elements.
                                .filter(kind -> kind != KeyKind.MAP || form != Form.CONTAINER_TEXT)
                                .map(kind -> Arguments.of(backend, form, wrapper, kind)))));
    }

    // Finding 6: an Object key must adopt approved container metadata before the String fallback is considered.
    @ParameterizedTest(name = "{0}, {1}, {2}, {3}")
    @MethodSource("immutableKeyCases")
    void immutableContainerMapKeysRetainTheirShapeSchemaAndMutableFallback(final Backend backend, final Form form, final Wrapper wrapper, final KeyKind kind) {
        final Object expectedKey = switch (kind) {
            case LIST -> List.of(1, 2);
            case SET -> Set.of(1, 2);
            case MAP -> Map.of("x", 1);
        };
        final String metadata = expectedKey.getClass().getName() + (kind == KeyKind.MAP ? "<String,Integer>" : "<Integer>");
        final String container = kind == KeyKind.MAP ? "map" : kind == KeyKind.SET ? "set" : "list";
        final String text = kind == KeyKind.MAP ? "{\"x\":\"1\"}" : NUMBERS;
        final String children = kind == KeyKind.MAP ? "<entry><key>x</key><value>1</value></entry>" : NUMBER_ELEMENTS;
        final String body = sequenceBody(backend, form, container, text, children);
        final String xml = "<map><entry>" + element("key", metadata, body) + "<value>kept</value></entry>"
                + "<entry><key>tail</key><value>end</value></entry></map>";
        final Map<?, ?> actual = backend.parser().deserialize(xml, Type.of("Map<" + wrapper.schema("Object") + ",String>"));
        assertEquals(2, actual.size(), xml);
        assertEquals("kept", actual.get(wrapper.wrap(expectedKey)), xml);
        assertEquals("end", actual.get(wrapper.wrap("tail")), xml);
        final Object parsedKey = actual.keySet().stream().map(wrapper::unwrap).filter(expectedKey::equals).findFirst().orElseThrow();
        // Remove keys before testing mutability so changing their hash codes cannot corrupt the containing map.
        actual.clear();
        assertMutableContainer(parsedKey, kind, xml);
    }

    @SuppressWarnings("unchecked")
    private static void assertMutableContainer(final Object value, final KeyKind kind, final String xml) {
        if (kind == KeyKind.MAP) {
            final Map<Object, Object> map = assertInstanceOf(Map.class, value, xml);
            assertDoesNotThrow(() -> map.put("extra", null), xml);
            map.remove("extra");
        } else {
            final Collection<Object> collection = kind == KeyKind.SET ? assertInstanceOf(Set.class, value, xml) : assertInstanceOf(List.class, value, xml);
            final Object marker = new Object();
            assertDoesNotThrow(() -> collection.add(marker), xml);
            collection.remove(marker);
        }
    }

    enum ArraySlot {
        MAP, BEAN, MAP_ENTITY
    }

    /**
     * The same array declaration at each property-reading entry point. MapEntity supplies its declaration through
     * named configuration; beans and ordinary maps supply it through their Java field or generic value type.
     */
    record ArrayLocation(Backend backend, Form form, Wrapper wrapper, ArraySlot slot) {
        String document(final Class<?> beanClass, final String metadata, final String body) {
            return slot == ArraySlot.BEAN ? beanDocument(beanClass, wrapper.property, metadata, body)
                    : mapValueDocument(backend, wrapper.property, metadata, body);
        }

        Object read(final String xml, final String payloadSchema, final Class<?> beanClass, final XmlDeserConfig config) {
            return assertDoesNotThrow(() -> {
                final XmlParser parser = backend.parser();
                final Object value = switch (slot) {
                    case MAP -> ((Map<?, ?>) parser.deserialize(xml, config, Type.of("Map<String," + wrapper.schema(payloadSchema) + ">")))
                            .get(wrapper.property);
                    case BEAN -> beanProperty(parser.deserialize(xml, config, beanClass), wrapper);
                    case MAP_ENTITY -> parser.deserialize(xml, config.setValueType(wrapper.property, wrapper.schema(payloadSchema)), MapEntity.class)
                            .get(wrapper.property);
                };
                return wrapper.unwrap(value);
            }, xml);
        }

        @Override
        public String toString() {
            return backend + ", " + form + ", " + wrapper + ", " + slot;
        }
    }

    private static Stream<ArrayLocation> arrayLocations() {
        return Stream.of(Backend.values())
                .flatMap(backend -> forms(backend).flatMap(form -> Stream.of(Wrapper.values())
                        .flatMap(wrapper -> (backend.abacus ? Stream.of(ArraySlot.MAP, ArraySlot.BEAN) : Stream.of(ArraySlot.values()))
                                .map(slot -> new ArrayLocation(backend, form, wrapper, slot)))));
    }

    enum StringArrayContent {
        MIXED(
                "[1,-2,1.5,true,false,null,\"\",\"kept\"]",
                "<e>1</e><e>-2</e><e>1.5</e><e>true</e><e>false</e><e isNull=\"true\"/><e/><e>kept</e>",
                new Object[] { "1", "-2", "1.5", "true", "false", null, "", "kept" }),
        QUOTED("[\"1\",\"true\",\"a & <b>\",\"\"]", "<e>1</e><e>true</e><e>a &amp; &lt;b&gt;</e><e/>", new Object[] { "1", "true", "a & <b>", "" }),
        NULLS("[null,null]", "<e isNull=\"true\"/><e isNull=\"true\"/>", new Object[] { null, null }),
        EMPTY("[]", "", new Object[0]);

        final String text;
        final String children;
        final Object[] expected;

        StringArrayContent(final String text, final String children, final Object[] expected) {
            this.text = text;
            this.children = children;
            this.expected = expected;
        }
    }

    private static String xmlText(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static Stream<Arguments> stringArrayCases() {
        return arrayLocations().flatMap(location -> Stream.of(StringArrayContent.values())
                // An empty standard property means null; explicit JSON [] tests an empty array there.
                .filter(content -> content != StringArrayContent.EMPTY || location.backend.abacus || location.form != Form.CHILDREN)
                .map(content -> Arguments.of(location, content)));
    }

    /**
     * Follow-up 1: numeric/boolean JSON tokens must be converted to Strings when the Object[] component is open.
     * Quoted strings, XML escaping, null positions, empty strings and empty arrays are controls for the same path.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("stringArrayCases")
    void configuredStringArrayElementsApplyToTextAndChildren(final ArrayLocation location, final StringArrayContent content) {
        final String body = sequenceBody(location.backend, location.form, "array", xmlText(content.text), content.children);
        final String xml = location.document(ArrayBean.class, "", body);
        final Object actual = location.read(xml, "Object[]", ArrayBean.class, XmlDeserConfig.create().setElementType(String.class));
        assertOpenArray(actual, content.expected, xml);
    }

    /**
     * Follow-up 1 control: a DECLARED String[] keeps String elements with and without configuration, through
     * the same text/children decoders the open slot uses (a Long configuration must not replace them).
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("stringArrayCases")
    void declaredStringArraysKeepStringElementsRegardlessOfConfiguration(final ArrayLocation location, final StringArrayContent content) {
        final String body = sequenceBody(location.backend, location.form, "array", xmlText(content.text), content.children);
        final String xml = location.document(StringArrayBean.class, "", body);
        assertArrayEquals(content.expected,
                assertInstanceOf(String[].class, location.read(xml, "String[]", StringArrayBean.class, XmlDeserConfig.create()), xml), xml);
        assertArrayEquals(content.expected, assertInstanceOf(String[].class,
                location.read(xml, "String[]", StringArrayBean.class, XmlDeserConfig.create().setElementType(Long.class)), xml), xml);
    }

    enum StructuredArrayElement {
        LIST, MAP, ARRAY, BEAN;

        String schema() {
            return switch (this) {
                case LIST -> "List<Long>";
                case MAP -> "Map<String,List<Long>>";
                case ARRAY -> "Long[]";
                case BEAN -> ArrayItem.class.getName();
            };
        }

        String text() {
            return switch (this) {
                case LIST, ARRAY -> "[[1,2],null,[3,4]]";
                case MAP -> "[{\"x\":[1,2]},null,{\"x\":[3,4]}]";
                case BEAN -> "[{\"id\":1,\"name\":\"first\"},null,{\"id\":2,\"name\":\"last\"}]";
            };
        }

        String child(final Backend backend, final boolean last) {
            final String numbers = last ? "<e>3</e><e>4</e>" : NUMBER_ELEMENTS;
            return switch (this) {
                case LIST -> element("list", "", numbers);
                case ARRAY -> element("array", "", numbers);
                case MAP -> element("map", "",
                        backend.abacus ? "<entry><key>x</key><value><list>" + numbers + "</list></value></entry>" : element("x", "", numbers));
                case BEAN -> element("arrayItem", "", last ? "<id>2</id><name>last</name>" : "<id>1</id><name>first</name>");
            };
        }

        Object[] expected() {
            return switch (this) {
                case LIST -> new Object[] { List.of(1L, 2L), null, List.of(3L, 4L) };
                case MAP -> new Object[] { Map.of("x", List.of(1L, 2L)), null, Map.of("x", List.of(3L, 4L)) };
                case ARRAY -> new Object[] { new Long[] { 1L, 2L }, null, new Long[] { 3L, 4L } };
                case BEAN -> new Object[] { new ArrayItem(1, "first"), null, new ArrayItem(2, "last") };
            };
        }
    }

    static Stream<Arguments> structuredArrayCases() {
        return arrayLocations().flatMap(location -> Stream.of(StructuredArrayElement.values()).map(element -> Arguments.of(location, element)));
    }

    /**
     * Follow-up 2: configuration selects structured elements too. Repeated nested e elements must stay a list,
     * every generic Long component must survive, and a null item must not consume the following structured item.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("structuredArrayCases")
    void configuredStructuredArrayElementsKeepTheirShapeAndSchema(final ArrayLocation location, final StructuredArrayElement component) {
        final String first = component.child(location.backend, false);
        final String last = component.child(location.backend, true);
        // Abacus array items have their own <e> wrapper around a structured payload; standard XML uses direct children.
        final String children = (location.backend.abacus ? element("e", "", first) : first) + "<e isNull=\"true\"/>"
                + (location.backend.abacus ? element("e", "", last) : last);
        final String body = sequenceBody(location.backend, location.form, "array", component.text(), children);
        final String xml = location.document(ArrayBean.class, "", body);
        final Object actual = location.read(xml, "Object[]", ArrayBean.class, XmlDeserConfig.create().setElementType(component.schema()));
        assertOpenArray(actual, component.expected(), xml);
    }

    static Stream<Arguments> arrayMetadataCases() {
        return arrayLocations().flatMap(location -> Stream.of("Object[]", "Integer[]")
                .flatMap(metadata -> Stream.of(Long.class, int.class)
                        .flatMap(component -> Stream.of(ArrayContent.values())
                                .filter(content -> content != ArrayContent.WITH_NULL || component == Long.class)
                                .filter(content -> content != ArrayContent.EMPTY || location.backend.abacus || location.form != Form.CHILDREN)
                                .map(content -> Arguments.of(location, metadata, component, content)))));
    }

    /**
     * Follow-up 3: metadata must not close a declared Object[] slot or leave an Integer[] destination for Long
     * elements. Matching Object[] metadata is the control; primitive configuration also checks boxed array values.
     */
    @ParameterizedTest(name = "{0}, metadata={1}, component={2}, {3}")
    @MethodSource("arrayMetadataCases")
    void openArrayMetadataCannotConflictWithConfiguredElements(final ArrayLocation location, final String metadata, final Class<?> component,
            final ArrayContent content) {
        final String xml = location.document(ArrayBean.class, metadata, arrayBody(location.backend, location.form, component, content));
        final Object actual = location.read(xml, "Object[]", ArrayBean.class, XmlDeserConfig.create().setElementType(component));
        assertOpenArray(actual, expectedArray(component, content), xml);
    }

    static Stream<ArrayLocation> declaredArraySlotCases() {
        return arrayLocations();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("declaredArraySlotCases")
    void declaredArrayComponentsStillWinOverConfigurationAndIncompatibleMetadata(final ArrayLocation location) {
        final String body = arrayBody(location.backend, location.form, Long.class, ArrayContent.WITH_NULL);
        final String xml = location.document(LongArrayBean.class, "Integer[]", body);
        final Object actual = location.read(xml, "Long[]", LongArrayBean.class, XmlDeserConfig.create().setElementType(String.class));
        assertEquals(Long[].class, actual.getClass(), xml);
        assertArrayEquals(new Long[] { 1L, null, 2L }, assertInstanceOf(Long[].class, actual, xml), xml);
    }

    static Stream<Arguments> openArrayMetadataNoConfigCases() {
        // Text form only: a nested <array> wrapper under a standard-XML property element is a separate,
        // unsupported shape (the reader treats it as an element), independent of metadata precedence.
        return arrayLocations().filter(location -> location.form == Form.TEXT)
                .flatMap(location -> Stream.of("Object[]", "Integer[]").map(metadata -> Arguments.of(location, metadata)));
    }

    /**
     * Follow-up 3 contrast: WITHOUT element configuration, array metadata on an open {@code Object[]} slot is the
     * only component information, so it decides both the runtime class and the elements ({@code Integer[]} of
     * Integers). Matching {@code Object[]} metadata is the control: the open slot keeps its runtime and falls back
     * to String elements. It is element configuration that makes metadata yield
     * ({@link #openArrayMetadataCannotConflictWithConfiguredElements}), and a DECLARED concrete array beats both
     * ({@link #declaredArrayComponentsStillWinOverConfigurationAndIncompatibleMetadata}).
     */
    @ParameterizedTest(name = "{0}, metadata={1}")
    @MethodSource("openArrayMetadataNoConfigCases")
    void openArrayMetadataDecidesWhenNoElementConfiguration(final ArrayLocation location, final String metadata) {
        final String xml = location.document(ArrayBean.class, metadata, "[\"1\",\"2\"]");
        final Object actual = location.read(xml, "Object[]", ArrayBean.class, XmlDeserConfig.create());
        if (metadata.equals("Object[]")) {
            assertOpenArray(actual, new Object[] { "1", "2" }, xml);
        } else {
            assertArrayEquals(new Integer[] { 1, 2 }, assertInstanceOf(Integer[].class, actual, xml), xml);
        }
    }

    enum PrimitiveArrayText {
        CHAR_DIGITS("[1,2,9]", new char[] { '1', '2', '9' }),
        CHAR_MIXED("[a,'1',65]", new char[] { 'a', '1', 'A' }),
        CHAR_ESCAPES("[\"\\n\",\"\\u0041\",\"\\\\\",\"<\",\"&\",\"\\\"\"]", new char[] { '\n', 'A', '\\', '<', '&', '"' }),
        CHAR_QUOTED("[\"1\",\"2\"]", new char[] { '1', '2' }),
        CHAR_EMPTY("[]", new char[0]),
        // A number is read by value (1 -> true; 0 and -1 -> false; any other number is rejected), the library-wide rule.
        BOOLEAN_NUMBERS("[0,1,-1]", new boolean[] { false, true, false }),
        BOOLEAN_LITERALS("[true,false,true]", new boolean[] { true, false, true }),
        // A quoted JSON string token is read as its content ("true" -> true, "1" -> true), as the char codec
        // already did; the pre-fix codec kept the quotes and read every quoted token as false.
        BOOLEAN_QUOTED("[\"true\",\"false\",\"1\",\"0\"]", new boolean[] { true, false, true, false }),
        BOOLEAN_EMPTY("[]", new boolean[0]);

        final String text;
        final Object expected;

        PrimitiveArrayText(final String text, final Object expected) {
            this.text = text;
            this.expected = expected;
        }
    }

    static Stream<Arguments> primitiveArrayTextCases() {
        // Exercise the property text decoder used by the reported primitive-array regressions.
        return arrayLocations().filter(location -> location.form == Form.TEXT)
                .flatMap(location -> Stream.of(PrimitiveArrayText.values())
                        .flatMap(content -> Stream.of(false, true).map(configured -> Arguments.of(location, content, configured))));
    }

    /**
     * Follow-up 4: a declared primitive array retains its text codec. In particular, single digits are characters,
     * and only 1 (not every nonzero number) means true. Expectations are literals, independent of either decoder.
     */
    @ParameterizedTest(name = "{0}, {1}, configured={2}")
    @MethodSource("primitiveArrayTextCases")
    void declaredPrimitiveArrayTextKeepsExistingConversions(final ArrayLocation location, final PrimitiveArrayText content, final boolean configured) {
        final boolean chars = content.expected instanceof char[];
        final Class<?> beanClass = chars ? CharArrayBean.class : BooleanArrayBean.class;
        final String xml = location.document(beanClass, "", xmlText(content.text));
        final XmlDeserConfig config = XmlDeserConfig.create();
        if (configured) {
            config.setElementType(Long.class);
        }
        final Object actual = location.read(xml, chars ? "char[]" : "boolean[]", beanClass, config);
        if (chars) {
            assertArrayEquals((char[]) content.expected, assertInstanceOf(char[].class, actual, xml), xml);
        } else {
            assertArrayEquals((boolean[]) content.expected, assertInstanceOf(boolean[].class, actual, xml), xml);
        }
    }

    enum JsonTextEncoding {
        TEXT, CDATA, SPLIT_CDATA, CHARACTER_REFERENCES;

        String encode(final String text) {
            return switch (this) {
                case TEXT -> xmlText(text);
                case CDATA -> "<![CDATA[" + text + "]]>";
                case SPLIT_CDATA -> {
                    // Split an escape after its backslash and first letter when possible. The XML reader must
                    // reassemble the JSON token before decoding it; no individual fragment is valid JSON.
                    final int escape = text.indexOf('\\');
                    final int split = escape >= 0 ? escape + 2 : text.length() / 2;
                    yield xmlText(text.substring(0, split)) + "<![CDATA[" + text.substring(split) + "]]>";
                }
                case CHARACTER_REFERENCES -> xmlText(text).replace("\u2003", "&#x2003;").replace("\u2002", "&#x2002;").replace("\u3000", "&#x3000;");
            };
        }
    }

    private static Stream<ArrayLocation> arrayTextLocations() {
        return arrayLocations().filter(location -> location.form != Form.CHILDREN);
    }

    private static String arrayTextBody(final ArrayLocation location, final String json, final JsonTextEncoding encoding) {
        return sequenceBody(location.backend, location.form, "array", encoding.encode(json), "");
    }

    enum BooleanJsonText {
        QUOTED_CONTROL("[\"true\",\"false\",\"1\",\"0\"]", new boolean[] { true, false, true, false }, JsonTextEncoding.TEXT),
        UNICODE(
                "[\"\\u0074rue\",\"f\\u0061lse\",\"\\u0031\",\"\\u0030\",\"\\u0059\",\"\\u0079\"]",
                new boolean[] { true, false, true, false, true, true },
                JsonTextEncoding.TEXT),
        ESCAPED_PADDING("[\"\\ttrue\\n\",\"\\r1\\t\",\"false\\n\",\"\\t0\\r\"]", new boolean[] { true, true, false, false }, JsonTextEncoding.TEXT),
        // These contain a literal backslash after one JSON decoding pass, not the words true or 1, so they are not
        // boolean text and are rejected (expected == null), as any other unrecognized text is (code review 2026-10-05).
        DOUBLE_ESCAPE("[\"\\\\u0074rue\",\"\\\\u0031\"]", null, JsonTextEncoding.TEXT),
        UNICODE_CDATA("[\"\\u0074rue\",null,\"\\u0031\"]", new boolean[] { true, false, true }, JsonTextEncoding.CDATA),
        UNICODE_SPLIT("[\"\\u0074rue\",\"\\u0031\",null,\"false\"]", new boolean[] { true, true, false, false }, JsonTextEncoding.SPLIT_CDATA),
        NULL_AND_EMPTY_TOKENS("[null,\"\",false]", new boolean[] { false, false, false }, JsonTextEncoding.TEXT),
        EMPTY("[]", new boolean[0], JsonTextEncoding.TEXT);

        final String text;
        final boolean[] expected;
        final JsonTextEncoding encoding;

        BooleanJsonText(final String text, final boolean[] expected, final JsonTextEncoding encoding) {
            this.text = text;
            this.expected = expected;
            this.encoding = encoding;
        }
    }

    static Stream<Arguments> escapedBooleanPropertyCases() {
        return arrayTextLocations().flatMap(location -> Stream.of(BooleanJsonText.values()).map(content -> Arguments.of(location, content)));
    }

    /**
     * New finding 1: a quoted boolean token must be unescaped exactly once before its boolean conversion.
     * Nulls keep their positions as false, including between escaped values and across CDATA event boundaries.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("escapedBooleanPropertyCases")
    void booleanArrayPropertiesDecodeJsonEscapesExactlyOnce(final ArrayLocation location, final BooleanJsonText content) {
        final String xml = location.document(BooleanArrayBean.class, "", arrayTextBody(location, content.text, content.encoding));
        if (content.expected == null) {
            // read() wraps the parse in assertDoesNotThrow, so the rejection arrives as the cause of its failure.
            final Throwable failure = assertThrows(org.opentest4j.AssertionFailedError.class,
                    () -> location.read(xml, "boolean[]", BooleanArrayBean.class, XmlDeserConfig.create()), xml);
            assertInstanceOf(IllegalArgumentException.class, failure.getCause(), xml);
            return;
        }
        final Object actual = location.read(xml, "boolean[]", BooleanArrayBean.class, XmlDeserConfig.create());
        assertArrayEquals(content.expected, assertInstanceOf(boolean[].class, actual, xml), xml);
    }

    static Stream<Arguments> escapedBooleanRootCases() {
        // Standard XML does not support primitive-array roots; the three Abacus backends do.
        return Stream.of(Backend.values())
                .filter(backend -> backend.abacus)
                .flatMap(backend -> Stream.of(BooleanJsonText.values()).map(content -> Arguments.of(backend, content)));
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("escapedBooleanRootCases")
    void booleanArrayRootsDecodeJsonEscapesExactlyOnce(final Backend backend, final BooleanJsonText content) {
        final String xml = element("array", "", content.encoding.encode(content.text));
        if (content.expected == null) {
            assertThrows(IllegalArgumentException.class, () -> backend.parser().deserialize(xml, boolean[].class), xml);
            return;
        }
        final boolean[] actual = assertDoesNotThrow(() -> backend.parser().deserialize(xml, boolean[].class), xml);
        assertArrayEquals(content.expected, actual, xml);
    }

    enum NumericArrayKind {
        BYTE("byte[]", ByteArrayBean.class, new byte[] { 1, -2 }, new byte[] { 0, 1, 0, -2, 0 }, new byte[0]),
        SHORT("short[]", ShortArrayBean.class, new short[] { 1, -2 }, new short[] { 0, 1, 0, -2, 0 }, new short[0]),
        INT("int[]", IntArrayBean.class, new int[] { 1, -2 }, new int[] { 0, 1, 0, -2, 0 }, new int[0]),
        LONG("long[]", PrimitiveLongArrayBean.class, new long[] { 1, -2 }, new long[] { 0, 1, 0, -2, 0 }, new long[0]),
        FLOAT("float[]", FloatArrayBean.class, new float[] { 1, -2 }, new float[] { 0, 1, 0, -2, 0 }, new float[0]),
        DOUBLE("double[]", DoubleArrayBean.class, new double[] { 1, -2 }, new double[] { 0, 1, 0, -2, 0 }, new double[0]);

        final String schema;
        final Class<?> beanClass;
        final Object numbers;
        final Object nullDefaults;
        final Object empty;

        NumericArrayKind(final String schema, final Class<?> beanClass, final Object numbers, final Object nullDefaults, final Object empty) {
            this.schema = schema;
            this.beanClass = beanClass;
            this.numbers = numbers;
            this.nullDefaults = nullDefaults;
            this.empty = empty;
        }

        Object expected(final NumericJsonText content) {
            return switch (content) {
                case RAW, QUOTED, ESCAPED_QUOTED, QUOTED_SPLIT -> numbers;
                case NULLS, QUOTED_NULLS_CDATA -> nullDefaults;
                case EMPTY -> empty;
            };
        }
    }

    enum NumericJsonText {
        RAW("[1,-2]", JsonTextEncoding.TEXT),
        QUOTED("[\"1\",\"-2\"]", JsonTextEncoding.TEXT),
        ESCAPED_QUOTED("[\"\\u0031\",\"\\u002d2\"]", JsonTextEncoding.TEXT),
        QUOTED_SPLIT("[\"1\",\"-2\"]", JsonTextEncoding.SPLIT_CDATA),
        NULLS("[null,1,null,-2,null]", JsonTextEncoding.TEXT),
        QUOTED_NULLS_CDATA("[null,\"1\",null,\"-2\",null]", JsonTextEncoding.CDATA),
        EMPTY("[]", JsonTextEncoding.TEXT);

        final String text;
        final JsonTextEncoding encoding;

        NumericJsonText(final String text, final JsonTextEncoding encoding) {
            this.text = text;
            this.encoding = encoding;
        }
    }

    private static void assertNumericArray(final Object expected, final Object actual, final String xml) {
        // Typed assertions catch both the values and the primitive runtime class, including empty arrays.
        switch (expected) {
            case byte[] values -> assertArrayEquals(values, assertInstanceOf(byte[].class, actual, xml), xml);
            case short[] values -> assertArrayEquals(values, assertInstanceOf(short[].class, actual, xml), xml);
            case int[] values -> assertArrayEquals(values, assertInstanceOf(int[].class, actual, xml), xml);
            case long[] values -> assertArrayEquals(values, assertInstanceOf(long[].class, actual, xml), xml);
            case float[] values -> assertArrayEquals(values, assertInstanceOf(float[].class, actual, xml), xml);
            case double[] values -> assertArrayEquals(values, assertInstanceOf(double[].class, actual, xml), xml);
            default -> throw new AssertionError("Unexpected numeric array fixture: " + expected.getClass());
        }
    }

    static Stream<Arguments> numericJsonPropertyCases() {
        return arrayTextLocations().flatMap(location -> Stream.of(NumericArrayKind.values())
                .flatMap(kind -> Stream.of(NumericJsonText.values()).map(content -> Arguments.of(location, kind, content))));
    }

    /**
     * New finding 2: compact primitive-array JSON accepts quoted numbers and maps null to the primitive default.
     * Leading, interior and trailing nulls must remain array positions, and literal numeric tokens are controls.
     */
    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("numericJsonPropertyCases")
    void primitiveNumericArrayPropertiesKeepJsonTokenConversions(final ArrayLocation location, final NumericArrayKind kind, final NumericJsonText content) {
        final String xml = location.document(kind.beanClass, "", arrayTextBody(location, content.text, content.encoding));
        final Object actual = location.read(xml, kind.schema, kind.beanClass, XmlDeserConfig.create());
        assertNumericArray(kind.expected(content), actual, xml);
    }

    static Stream<Arguments> numericJsonRootCases() {
        return Stream.of(Backend.values())
                .filter(backend -> backend.abacus)
                .flatMap(backend -> Stream.of(NumericArrayKind.values())
                        .flatMap(kind -> Stream.of(NumericJsonText.values()).map(content -> Arguments.of(backend, kind, content))));
    }

    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("numericJsonRootCases")
    void primitiveNumericArrayRootsKeepJsonTokenConversions(final Backend backend, final NumericArrayKind kind, final NumericJsonText content) {
        final String xml = element("array", "", content.encoding.encode(content.text));
        final Object actual = assertDoesNotThrow(() -> backend.parser().deserialize(xml, Type.of(kind.schema)), xml);
        assertNumericArray(kind.expected(content), actual, xml);
    }

    static Stream<ArrayLocation> characterNullPropertyCases() {
        return arrayTextLocations();
    }

    /**
     * Null-to-default conversion also applies to char[]. Quoted letters avoid changing the separately tested
     * single-digit character policy; an escaped letter after a null ensures JSON decoding continues correctly.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("characterNullPropertyCases")
    void characterArrayPropertiesRetainNullDefaults(final ArrayLocation location) {
        final String json = "[null,\"a\",null,\"\\u0062\",null]";
        final String xml = location.document(CharArrayBean.class, "", arrayTextBody(location, json, JsonTextEncoding.TEXT));
        final Object actual = location.read(xml, "char[]", CharArrayBean.class, XmlDeserConfig.create());
        assertArrayEquals(new char[] { '\0', 'a', '\0', 'b', '\0' }, assertInstanceOf(char[].class, actual, xml), xml);
    }

    static Stream<Backend> primitiveArrayRootBackends() {
        return Stream.of(Backend.values()).filter(backend -> backend.abacus);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("primitiveArrayRootBackends")
    void characterArrayRootsRetainNullDefaults(final Backend backend) {
        final String xml = "<array>[null,\"a\",null,\"\\u0062\",null]</array>";
        final char[] actual = assertDoesNotThrow(() -> backend.parser().deserialize(xml, char[].class), xml);
        assertArrayEquals(new char[] { '\0', 'a', '\0', 'b', '\0' }, actual, xml);
    }

    enum StringArrayPadding {
        ASCII_CONTROL(" \t", "\n ", JsonTextEncoding.TEXT, false),
        LEADING_EM_SPACE("\u2003", "", JsonTextEncoding.TEXT, false),
        TRAILING_EM_SPACE("", "\u2003", JsonTextEncoding.TEXT, false),
        BOTH_EM_SPACE("\u2003", "\u2003", JsonTextEncoding.TEXT, false),
        MIXED_UNICODE("\u3000", "\u2002", JsonTextEncoding.TEXT, false),
        XML_REFERENCES("\u2003", "\u2003", JsonTextEncoding.CHARACTER_REFERENCES, false),
        CDATA("\u2003", "\u2003", JsonTextEncoding.CDATA, false),
        SPLIT_CDATA("\u2003", "\u2003", JsonTextEncoding.SPLIT_CDATA, false),
        EMPTY("\u2003", "\u2003", JsonTextEncoding.TEXT, true);

        final String leading;
        final String trailing;
        final JsonTextEncoding encoding;
        final boolean empty;

        StringArrayPadding(final String leading, final String trailing, final JsonTextEncoding encoding, final boolean empty) {
            this.leading = leading;
            this.trailing = trailing;
            this.encoding = encoding;
            this.empty = empty;
        }

        String text() {
            // The padding outside the JSON may be stripped; the same characters inside a String must survive.
            return leading + (empty ? "[]" : "[\"\u2003kept\u2003\",null,\"\",\"a & <b>\"]") + trailing;
        }

        String[] expected() {
            return empty ? new String[0] : new String[] { "\u2003kept\u2003", null, "", "a & <b>" };
        }
    }

    static Stream<Arguments> paddedStringPropertyCases() {
        return arrayTextLocations().flatMap(location -> Stream.of(StringArrayPadding.values()).map(padding -> Arguments.of(location, padding)));
    }

    /**
     * New finding 3: declared String[] text retains its Unicode-whitespace tolerance, without trimming values.
     * Literal XML text, character references and CDATA must all deliver the same array text to the decoder.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("paddedStringPropertyCases")
    void stringArrayPropertiesAcceptUnicodePaddingWithoutTrimmingValues(final ArrayLocation location, final StringArrayPadding padding) {
        final String xml = location.document(StringArrayBean.class, "", arrayTextBody(location, padding.text(), padding.encoding));
        final Object actual = location.read(xml, "String[]", StringArrayBean.class, XmlDeserConfig.create());
        assertArrayEquals(padding.expected(), assertInstanceOf(String[].class, actual, xml), xml);
    }

    static Stream<Arguments> paddedStringRootCases() {
        return Stream.of(Backend.values()).flatMap(backend -> Stream.of(StringArrayPadding.values()).map(padding -> Arguments.of(backend, padding)));
    }

    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("paddedStringRootCases")
    void stringArrayRootsKeepUnicodePaddingTolerance(final Backend backend, final StringArrayPadding padding) {
        final String xml = element("array", "", padding.encoding.encode(padding.text()));
        final String[] actual = assertDoesNotThrow(() -> backend.parser().deserialize(xml, String[].class), xml);
        assertArrayEquals(padding.expected(), actual, xml);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // 2026-10-05 evening findings

    private static final String MULTI_VALUE_MAP = "org.springframework.util.LinkedMultiValueMap";

    static Stream<Backend> allBackends() {
        return Stream.of(Backend.values());
    }

    private static Object mapValue(final Backend backend, final String xml, final XmlDeserConfig config, final String valueSchema) {
        final Map<?, ?> map = assertDoesNotThrow(() -> (Map<?, ?>) backend.parser().deserialize(xml, config, Type.of("Map<String," + valueSchema + ">")), xml);
        return map.get("k");
    }

    private static void assertOrdinaryMapWithString(final Object value, final String xml) {
        final Map<?, ?> map = assertInstanceOf(Map.class, value, xml);
        assertFalse(map instanceof MultiValueMap, "A scalar override cannot live in a list-valued map: " + xml);
        assertEquals(Map.of("a", "1"), map, xml);
    }

    static Stream<Arguments> paddedReferenceContainerCases() {
        return allBackends().flatMap(backend -> Stream.of("Long[]", "Object[]", "List<Long>")
                .flatMap(declaration -> Stream.of(true, false).map(root -> Arguments.of(backend, declaration, root))));
    }

    /**
     * Remaining finding 1: Unicode boundary padding around compact JSON text is tolerated for reference arrays and
     * collections, at the root and as a map value, on every backend, as it already was for String[] and primitive arrays.
     */
    @ParameterizedTest(name = "{0}, {1}, root={2}")
    @MethodSource("paddedReferenceContainerCases")
    void referenceContainerTextAcceptsUnicodePaddingOnEveryBackend(final Backend backend, final String declaration, final boolean root) {
        final String text = "\u2003[1,2]\u2003";
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final String container = declaration.startsWith("List") ? "list" : "array";
        final Object actual = root
                ? assertDoesNotThrow(() -> backend.parser().deserialize(element(container, "", text), config, Type.of(declaration)), declaration)
                : mapValue(backend, mapValueDocument(backend, "k", "", text), config, declaration);
        assertNotNull(actual, declaration);
        if (declaration.startsWith("List")) {
            assertEquals(List.of(1L, 2L), actual, declaration);
        } else {
            assertEquals(Type.of(declaration).javaType(), actual.getClass(), declaration);
            assertArrayEquals(new Object[] { 1L, 2L }, (Object[]) actual, declaration);
        }
    }

    static Stream<Arguments> scalarDeclarationCases() {
        return allBackends().flatMap(backend -> Stream.of("Map<String,String>", "Map<String,Object>", "Map")
                .flatMap(declaration -> Stream.of("none", "global", "named")
                        .filter(configuration -> !(configuration.equals("none") && !declaration.equals("Map<String,String>")))
                        .map(configuration -> Arguments.of(backend, declaration, configuration))));
    }

    /**
     * Remaining finding 6: list-valued map metadata (Spring's MultiValueMap) on a root map whose declared or configured
     * values are scalars keeps ordinary map construction instead of failing inside the implementation.
     */
    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("scalarDeclarationCases")
    void specializedMapMetadataKeepsDeclaredScalarValues(final Backend backend, final String declaration, final String configuration) {
        final XmlDeserConfig config = switch (configuration) {
            case "global" -> XmlDeserConfig.create().setMapValueType(String.class);
            case "named" -> XmlDeserConfig.create().setValueType("k", String.class);
            default -> XmlDeserConfig.create();
        };
        final String xml = "<map" + attribute(MULTI_VALUE_MAP) + ">" + (backend.abacus ? "<entry><key>k</key><value>1</value></entry>" : "<k>1</k>") + "</map>";
        final Map<?, ?> actual = assertDoesNotThrow(() -> (Map<?, ?>) backend.parser().deserialize(xml, config, Type.of(declaration)), xml);
        assertFalse(actual instanceof MultiValueMap, xml);
        assertEquals(Map.of("k", "1"), actual, xml);
    }

    enum NestedMapShape {
        RAW_MAP("Map", MULTI_VALUE_MAP),
        PARAMETERIZED_MAP("Map<String,Object>", MULTI_VALUE_MAP + "<String,Integer>"),
        STRING_MAP("Map<String,String>", MULTI_VALUE_MAP),
        LIST_OF_MAPS("List<Map<String,Object>>", "ArrayList<" + MULTI_VALUE_MAP + "<String,Integer>>"),
        ARRAY_OF_MAPS("Map<String,Object>[]", MULTI_VALUE_MAP + "[]");

        final String declaration;
        final String metadata;

        NestedMapShape(final String declaration, final String metadata) {
            this.declaration = declaration;
            this.metadata = metadata;
        }

        String compactBody() {
            return this == LIST_OF_MAPS || this == ARRAY_OF_MAPS ? "[{\"a\":\"1\"}]" : "{\"a\":\"1\"}";
        }

        String childBody(final Backend backend) {
            // The shapes the serializers write: Abacus wraps a nested map in <map> and every item in <e>; standard XML
            // writes a nested map as a <map> element and the items of a list or array as consecutive <map> elements.
            final String map = backend.abacus ? "<map><entry><key>a</key><value>1</value></entry></map>" : "<map><a>1</a></map>";
            return switch (this) {
                case LIST_OF_MAPS -> backend.abacus ? element("list", "", element("e", "", map)) : map;
                case ARRAY_OF_MAPS -> backend.abacus ? element("array", "", element("e", "", map)) : map;
                default -> map;
            };
        }

        Map<?, ?> containedMap(final Object value) {
            return switch (this) {
                case LIST_OF_MAPS -> (Map<?, ?>) assertInstanceOf(List.class, value).get(0);
                case ARRAY_OF_MAPS -> (Map<?, ?>) assertInstanceOf(Object[].class, value)[0];
                default -> (Map<?, ?>) value;
            };
        }
    }

    static Stream<Arguments> nestedOverrideCases() {
        return allBackends().flatMap(backend -> Stream.of(NestedMapShape.values())
                .flatMap(shape -> Stream.of(Form.TEXT, Form.CHILDREN).map(form -> Arguments.of(backend, shape, form))));
    }

    /**
     * Remaining findings 2 and 3: a named String override applies inside compact text at any nesting level, exactly as
     * to child elements, and a list-valued implementation yields to it on every backend and in every representation.
     */
    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("nestedOverrideCases")
    void namedStringOverridesReachSpecializedMapsInEveryRepresentation(final Backend backend, final NestedMapShape shape, final Form form) {
        final String body = form == Form.TEXT ? shape.compactBody() : shape.childBody(backend);
        final String xml = mapValueDocument(backend, "k", shape.metadata, body);
        final Object value = mapValue(backend, xml, XmlDeserConfig.create().setValueType("a", String.class), shape.declaration);
        assertOrdinaryMapWithString(shape.containedMap(value), xml);
    }

    static Stream<Arguments> namedObjectCases() {
        return allBackends().flatMap(backend -> Stream.of(Form.TEXT, Form.CHILDREN).map(form -> Arguments.of(backend, form)));
    }

    /**
     * Remaining finding 4: an explicit Object override is applied literally, so the value is read as open scalar text
     * and cannot live in a list-valued map either; compact and structured forms agree on every backend.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("namedObjectCases")
    void namedObjectOverridesReadTheSameInCompactAndStructuredForms(final Backend backend, final Form form) {
        final NestedMapShape shape = NestedMapShape.PARAMETERIZED_MAP;
        final String xml = mapValueDocument(backend, "k", shape.metadata, form == Form.TEXT ? shape.compactBody() : shape.childBody(backend));
        final Object value = mapValue(backend, xml, XmlDeserConfig.create().setValueType("a", Object.class), shape.declaration);
        assertOrdinaryMapWithString(value, xml);
    }

    /**
     * Controls for findings 2 to 4: configuration for an absent key changes nothing, a later present key's scalar
     * override preserves the entries read before it, and a compatible {@code List<V>} declaration keeps both the
     * implementation and its element type.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void specializedMapsKeepTheirImplementationUnlessAPresentKeyRequiresOtherwise(final Backend backend) {
        final String metadata = MULTI_VALUE_MAP + "<String,Integer>";
        // an open Object value adopts the implementation's List<V> shape: the scalar text becomes a one-element list
        final String root = "<map" + attribute(MULTI_VALUE_MAP) + ">" + (backend.abacus ? "<entry><key>k</key><value>1</value></entry>" : "<k>1</k>")
                + "</map>";
        final Object adopted = assertDoesNotThrow(() -> backend.parser().deserialize(root, XmlDeserConfig.create(), Type.of("Map<String,Object>")), root);
        assertInstanceOf(LinkedMultiValueMap.class, adopted, root);
        assertEquals(Map.of("k", List.of(1)), adopted, root);

        String xml = mapValueDocument(backend, "k", metadata, "{\"a\":\"1\"}");
        Object value = mapValue(backend, xml, XmlDeserConfig.create().setValueType("absent", String.class), "Map<String,Object>");
        assertInstanceOf(LinkedMultiValueMap.class, value, xml);
        assertEquals(Map.of("a", List.of(1)), value, xml);

        xml = mapValueDocument(backend, "k", metadata, "{\"a\":[1],\"b\":\"x\"}");
        value = mapValue(backend, xml, XmlDeserConfig.create().setValueType("b", String.class), "Map<String,Object>");
        final Map<?, ?> map = assertInstanceOf(Map.class, value, xml);
        assertFalse(map instanceof MultiValueMap, xml);
        assertEquals(List.of(1), map.get("a"), xml);
        assertEquals("x", map.get("b"), xml);
        assertEquals(List.of("a", "b"), new ArrayList<>(map.keySet()), xml);

        xml = mapValueDocument(backend, "k", MULTI_VALUE_MAP, "{\"a\":[\"1\"]}");
        value = mapValue(backend, xml, XmlDeserConfig.create(), "Map<String,List<Long>>");
        assertInstanceOf(LinkedMultiValueMap.class, value, xml);
        assertEquals(Map.of("a", List.of(1L)), value, xml);
    }

    enum NestedOpenArray {
        MATRIX("Object[][]", "Long[][]", "[[1,2]]"),
        LIST_OF_ARRAYS("List<Object[]>", "ArrayList<Long[]>", "[[1,2]]"),
        MAP_OF_ARRAYS("Map<String,Object[]>", "LinkedHashMap<String,Long[]>", "{\"x\":[1,2]}"),
        OPTIONAL_MATRIX("java.util.Optional<Object[][]>", "Long[][]", "[[1,2]]");

        final String declaration;
        final String metadata;
        final String text;

        NestedOpenArray(final String declaration, final String metadata, final String text) {
            this.declaration = declaration;
            this.metadata = metadata;
            this.text = text;
        }

        Object innerArray(final Object value) {
            return switch (this) {
                case MATRIX -> assertInstanceOf(Object[][].class, value)[0];
                case LIST_OF_ARRAYS -> assertInstanceOf(List.class, value).get(0);
                case MAP_OF_ARRAYS -> assertInstanceOf(Map.class, value).get("x");
                case OPTIONAL_MATRIX -> assertInstanceOf(Object[][].class, assertInstanceOf(Optional.class, value).orElseThrow())[0];
            };
        }
    }

    static Stream<Arguments> nestedOpenArrayCases() {
        return allBackends().flatMap(backend -> Stream.of(NestedOpenArray.values()).map(shape -> Arguments.of(backend, shape)));
    }

    /**
     * Remaining finding 5: with element configuration present, metadata supplies the container implementation but does
     * not narrow a nested open array component, which reads the configured elements, on every backend.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("nestedOpenArrayCases")
    void nestedOpenArrayComponentsHonorElementConfigurationInCompactText(final Backend backend, final NestedOpenArray shape) {
        final String xml = mapValueDocument(backend, "k", shape.metadata, shape.text);
        final Object value = mapValue(backend, xml, XmlDeserConfig.create().setElementType(String.class), shape.declaration);
        final Object inner = shape.innerArray(value);
        assertEquals(Object[].class, inner.getClass(), "Metadata must not narrow the declared open component: " + xml);
        assertArrayEquals(new Object[] { "1", "2" }, (Object[]) inner, xml);
    }

    /**
     * Remaining finding 5 contrast: without element configuration, metadata on a nested open array slot decides the
     * runtime class and the elements, as it does for a one-dimensional slot.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void nestedArrayMetadataDecidesWithoutElementConfiguration(final Backend backend) {
        final String xml = mapValueDocument(backend, "k", "Long[][]", "[[1,2]]");
        final Object value = mapValue(backend, xml, XmlDeserConfig.create(), "Object[][]");
        assertEquals(Long[][].class, value.getClass(), xml);
        assertArrayEquals(new Long[] { 1L, 2L }, ((Long[][]) value)[0], xml);
    }

    /**
     * Remaining finding 7: a map written as compact JSON text inside a list element is read by every backend; the
     * standard StAX reader used to return an empty map silently.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void compactMapTextInsideListElementsIsReadOnEveryBackend(final Backend backend) {
        final String xml = "<list><e>{\"a\":[\"1\",\"2\"]}</e></list>";
        final List<?> actual = assertDoesNotThrow(() -> backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("List<Map<String,List<Integer>>>")),
                xml);
        assertEquals(List.of(Map.of("a", List.of(1, 2))), actual, xml);

        final String nested = mapValueDocument(backend, "k", "", "{\"a\":[\"1\",\"2\"]}");
        assertEquals(Map.of("a", List.of(1, 2)), mapValue(backend, nested, XmlDeserConfig.create(), "Map<String,List<Integer>>"), nested);
    }

    /**
     * New findings 1 and 2 of the evening review: primitive-list text goes through the array codecs, which read quoted
     * content decoded once and a bare null as the element default, as the JSON parser's direct path does.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void primitiveListTextReadsQuotedNumbersAndNullDefaults(final Backend backend) {
        final String ints = mapValueDocument(backend, "k", "", "[\"1\",null,2]");
        assertEquals(IntList.of(1, 0, 2), mapValue(backend, ints, XmlDeserConfig.create(), "IntList"), ints);
        final String chars = mapValueDocument(backend, "k", "", "[\"1\",null,\"65\"]");
        assertEquals(CharList.of('1', '\0', 'A'), mapValue(backend, chars, XmlDeserConfig.create(), "CharList"), chars);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Open issues found after the evening review

    enum AdoptedPrimitiveArray {
        CHARS("char[]", "[1,1]", "<e>1</e><e>1</e>", new char[] { '1', '1' }),
        INTS("int[]", "[1,2]", "<e>1</e><e>2</e>", new int[] { 1, 2 }),
        BOOLEANS("boolean[]", "[1,0]", "<e>1</e><e>0</e>", new boolean[] { true, false });

        final String metadata;
        final String text;
        final String children;
        final Object expected;

        AdoptedPrimitiveArray(final String metadata, final String text, final String children, final Object expected) {
            this.metadata = metadata;
            this.text = text;
            this.children = children;
            this.expected = expected;
        }

        String body(final Backend backend, final Form form) {
            return form == Form.TEXT ? text : backend.abacus ? element("array", "", children) : children;
        }
    }

    private static String describeArray(final Object array) {
        if (array instanceof char[] chars) {
            final StringBuilder codes = new StringBuilder("char codes");
            for (final char c : chars) {
                codes.append(' ').append((int) c);
            }
            return codes.toString();
        }
        return java.util.Arrays.deepToString(new Object[] { array });
    }

    static Stream<Arguments> adoptedPrimitiveArrayCases() {
        return allBackends().flatMap(backend -> Stream.of(AdoptedPrimitiveArray.values())
                .flatMap(kind -> Stream.of(Form.TEXT, Form.CHILDREN)
                        .flatMap(form -> Stream.of("none", "Integer", "String").map(configuration -> Arguments.of(backend, kind, form, configuration)))));
    }

    /**
     * Open issue 1: a primitive array that an open Object slot adopts from metadata is as reified as a declared one, so
     * element configuration never types its elements. The StAX readers used to hand a configured Integer to a char[],
     * reading the digit 1 as the character with code 1 from text and failing on child elements.
     */
    @ParameterizedTest(name = "{0}, {1}, {2}, config={3}")
    @MethodSource("adoptedPrimitiveArrayCases")
    void adoptedPrimitiveArraysIgnoreElementConfiguration(final Backend backend, final AdoptedPrimitiveArray kind, final Form form,
            final String configuration) {
        final XmlDeserConfig config = switch (configuration) {
            case "Integer" -> XmlDeserConfig.create().setElementType(Integer.class);
            case "String" -> XmlDeserConfig.create().setElementType(String.class);
            default -> XmlDeserConfig.create();
        };
        final String xml = mapValueDocument(backend, "k", kind.metadata, kind.body(backend, form));
        final Object actual = mapValue(backend, xml, config, "Object");
        assertNotNull(actual, xml);
        assertEquals(kind.expected.getClass(), actual.getClass(), xml);
        assertTrue(Objects.deepEquals(kind.expected, actual), xml + " -> " + describeArray(actual));
    }

    static Stream<Arguments> scalarOverrideOnListCases() {
        return allBackends().flatMap(backend -> Stream.of(Form.TEXT, Form.CHILDREN).map(form -> Arguments.of(backend, form)));
    }

    /**
     * Open issue 2: a scalar per-key override of a declared list value names the element type, in compact text as for
     * child elements, on every backend. The text form used to read the whole JSON array as one String. The second
     * document is the reviewer's variant: a numeric key of a list-valued map, whose List of Strings keeps the
     * implementation (Abacus SAX used to ignore the override there).
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("scalarOverrideOnListCases")
    void scalarValueOverridesNameTheElementTypeOfDeclaredListValues(final Backend backend, final Form form) {
        final String body = form == Form.TEXT ? "[1,2]" : sequenceBody(backend, Form.CHILDREN, "list", null, "<e>1</e><e>2</e>");
        final String xml = mapValueDocument(backend, "k", "", body);
        assertEquals(List.of("1", "2"), mapValue(backend, xml, XmlDeserConfig.create().setValueType("k", String.class), "List<Integer>"), xml);

        final String numericKey = form == Form.TEXT ? "{\"1\":[1,2]}"
                : backend.abacus ? "<map><entry><key>1</key><value><list><e>1</e><e>2</e></list></value></entry></map>"
                        : "<map><entry name=\"1\"><e>1</e><e>2</e></entry></map>";
        final String nested = mapValueDocument(backend, "k", MULTI_VALUE_MAP + "<Integer,Integer>", numericKey);
        final Object value = mapValue(backend, nested, XmlDeserConfig.create().setValueType("1", String.class), "Map<Integer,List<Integer>>");
        assertInstanceOf(LinkedMultiValueMap.class, value, nested);
        assertEquals(Map.of(1, List.of("1", "2")), value, nested);
    }

    /**
     * Open issue 2 contrast: the rule needs a declared list value. A declared scalar or open value keeps the literal
     * reading of its text, and a container override still names the whole value.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void scalarValueOverridesKeepTheirLiteralMeaningElsewhere(final Backend backend) {
        final String xml = mapValueDocument(backend, "k", "", "[1,2]");
        assertEquals("[1,2]", mapValue(backend, xml, XmlDeserConfig.create().setValueType("k", String.class), "Object"), xml);
        assertEquals("[1,2]", mapValue(backend, xml, XmlDeserConfig.create().setValueType("k", String.class), "String"), xml);
        assertEquals(List.of(1L, 2L), mapValue(backend, xml, XmlDeserConfig.create().setValueType("k", "List<Long>"), "List<Integer>"), xml);
    }

    static Stream<Arguments> nestedArrayItemCases() {
        return Stream.of(Backend.XML_STAX, Backend.XML_DOM)
                .flatMap(backend -> Stream.of("<array><e>1</e><e>2</e></array>", "<array>[1,2]</array>").map(item -> Arguments.of(backend, item)));
    }

    /**
     * Leftover shape of the evening review: a nested {@code <array>} item of a list value under standard XML, with the
     * array metadata on the list rather than on the item. With element configuration present, metadata supplies the
     * list implementation but may not narrow the declared open {@code Object[]} item, whose elements take the
     * configured type; the DOM reader used to read the item as {@code Long[]}. Without configuration the metadata
     * decides, as for any open slot. The serializer's own shape, with the type on the item, is the control.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("nestedArrayItemCases")
    void nestedArrayItemsOfStandardListValuesHonorElementConfiguration(final Backend backend, final String item) {
        final String xml = mapValueDocument(backend, "k", "ArrayList<Long[]>", item);
        final List<?> configured = assertInstanceOf(List.class, mapValue(backend, xml, XmlDeserConfig.create().setElementType(String.class), "List<Object[]>"),
                xml);
        assertEquals(1, configured.size(), xml);
        assertEquals(Object[].class, configured.get(0).getClass(), "Metadata must not narrow the declared open item: " + xml);
        assertArrayEquals(new Object[] { "1", "2" }, (Object[]) configured.get(0), xml);

        final List<?> unconfigured = assertInstanceOf(List.class, mapValue(backend, xml, XmlDeserConfig.create(), "List<Object[]>"), xml);
        assertEquals(Long[].class, unconfigured.get(0).getClass(), "Without configuration the metadata decides: " + xml);
        assertArrayEquals(new Long[] { 1L, 2L }, (Long[]) unconfigured.get(0), xml);

        final String canonical = mapValueDocument(backend, "k", "ArrayList<Object>", "<array type=\"Object[]\">[1, 2]</array>");
        final List<?> written = assertInstanceOf(List.class,
                mapValue(backend, canonical, XmlDeserConfig.create().setElementType(String.class), "List<Object[]>"), canonical);
        assertArrayEquals(new Object[] { "1", "2" }, (Object[]) written.get(0), canonical);
    }

    static Stream<Arguments> unwrappedMapValueCases() {
        return Stream.of(Backend.XML_STAX, Backend.XML_DOM)
                .flatMap(backend -> Stream.of("<x><e>1</e><e>2</e></x>", "<x>[1,2]</x>", "<x>[1,2]</x><y>[3]</y>").map(body -> Arguments.of(backend, body)));
    }

    /**
     * Remaining difference 1: standard XML writes a nested map value inside a {@code <map>} element. A value written
     * without that wrapper is rejected instead of being misread: the readers used to take the first child as the
     * wrapper and lose the keys, and the StAX reader returned an empty map for two children. The serializer's own
     * shape is the control.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("unwrappedMapValueCases")
    void standardMapValuesRequireTheMapWrapper(final Backend backend, final String body) {
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(String.class);
        final String xml = mapValueDocument(backend, "k", "LinkedHashMap<String,Long[]>", body);
        final ParsingException failure = assertThrows(ParsingException.class,
                () -> backend.parser().deserialize(xml, config, Type.of("Map<String,Map<String,Object[]>>")), xml);
        assertTrue(failure.getMessage().contains("must be a <map> element") || failure.getMessage().contains("Only one child element is supported"),
                failure.getMessage());

        final String canonical = mapValueDocument(backend, "k", "LinkedHashMap<String,Long[]>", "<map><x>[1,2]</x></map>");
        final Map<?, ?> value = assertInstanceOf(Map.class, mapValue(backend, canonical, config, "Map<String,Object[]>"), canonical);
        assertArrayEquals(new Object[] { "1", "2" }, (Object[]) value.get("x"), canonical);
    }

    static Stream<Arguments> specializedItemCases() {
        return allBackends().flatMap(backend -> Stream.of(Form.TEXT, Form.CHILDREN)
                .flatMap(form -> Stream.of("List<Map<String,Object>>", "Map<String,Object>[]").map(declaration -> Arguments.of(backend, form, declaration))));
    }

    /**
     * Remaining difference 2: list and array metadata naming a specialized map item applies to child elements as it
     * does to compact text. Without an override the items are list-valued maps on every backend (the Abacus readers
     * used to construct the declared Map and read "1" as a scalar), and a scalar override falls back to the same
     * ordinary {@link LinkedHashMap} on every backend.
     */
    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("specializedItemCases")
    void specializedMapItemsSurviveChildElements(final Backend backend, final Form form, final String declaration) {
        final boolean list = declaration.startsWith("List");
        final String metadata = list ? "ArrayList<" + MULTI_VALUE_MAP + "<String,Integer>>" : MULTI_VALUE_MAP + "[]";
        final String text = "[{\"a\":1}]";
        final Object element = 1;
        final String map = backend.abacus ? "<map><entry><key>a</key><value>1</value></entry></map>" : "<map><a>1</a></map>";
        final String body = form == Form.TEXT ? text : backend.abacus ? element(list ? "list" : "array", "", element("e", "", map)) : map;
        final String xml = mapValueDocument(backend, "k", metadata, body);

        final Object plain = mapValue(backend, xml, XmlDeserConfig.create(), declaration);
        final Map<?, ?> item = (Map<?, ?>) (list ? assertInstanceOf(List.class, plain, xml).get(0) : assertInstanceOf(Object[].class, plain, xml)[0]);
        assertInstanceOf(LinkedMultiValueMap.class, item, xml);
        assertEquals(Map.of("a", List.of(element)), item, xml);

        final Object overridden = mapValue(backend, xml, XmlDeserConfig.create().setValueType("a", String.class), declaration);
        final Map<?, ?> fallback = (Map<?, ?>) (list ? assertInstanceOf(List.class, overridden, xml).get(0)
                : assertInstanceOf(Object[].class, overridden, xml)[0]);
        assertEquals(LinkedHashMap.class, fallback.getClass(), "The ordinary fallback map is the same on every backend: " + xml);
        assertEquals(Map.of("a", "1"), fallback, xml);
    }

    static Stream<Arguments> nestedOpenArrayChildCases() {
        return Stream.of(Backend.ABACUS_STAX, Backend.ABACUS_DOM, Backend.ABACUS_SAX)
                .flatMap(backend -> Stream.of(NestedOpenArray.MATRIX, NestedOpenArray.LIST_OF_ARRAYS).map(shape -> Arguments.of(backend, shape)));
    }

    /**
     * Remaining finding 5, child-element form on the Abacus backends: a nested open array item keeps its declared,
     * open form under element configuration, so its elements take the configured type, and without configuration the
     * metadata decides the item class. The compact form is covered above.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("nestedOpenArrayChildCases")
    void nestedOpenArrayComponentsHonorElementConfigurationInChildElements(final Backend backend, final NestedOpenArray shape) {
        final String inner = element("array", "", "<e>1</e><e>2</e>");
        final String body = shape == NestedOpenArray.MATRIX ? element("array", "", element("e", "", inner)) : element("list", "", element("e", "", inner));
        final String xml = mapValueDocument(backend, "k", shape.metadata, body);

        final Object configured = mapValue(backend, xml, XmlDeserConfig.create().setElementType(String.class), shape.declaration);
        final Object configuredInner = shape.innerArray(configured);
        assertEquals(Object[].class, configuredInner.getClass(), "Metadata must not narrow the declared open item: " + xml);
        assertArrayEquals(new Object[] { "1", "2" }, (Object[]) configuredInner, xml);

        final Object plain = mapValue(backend, xml, XmlDeserConfig.create(), shape.declaration);
        final Object plainInner = shape == NestedOpenArray.MATRIX ? assertInstanceOf(Long[][].class, plain, xml)[0]
                : assertInstanceOf(List.class, plain, xml).get(0);
        assertEquals(Long[].class, plainInner.getClass(), "Without configuration the metadata decides: " + xml);
        assertArrayEquals(new Long[] { 1L, 2L }, (Long[]) plainInner, xml);
    }

    static Stream<Arguments> untypedChildCases() {
        final Map<String, Object> xy = new LinkedHashMap<>();
        xy.put("x", "1");
        xy.put("y", "s");
        final List<Arguments> cases = new ArrayList<>();
        for (final Backend backend : List.of(Backend.XML_STAX, Backend.XML_DOM)) {
            // A single child is the value (a <map> the nested map the writer wrote that way, an <e> its text); several
            // children, or a nested container child, are the items of a list, read by name: <map> a map, <list> a list,
            // <set> a set, <array> an array, <e> a scalar.
            cases.add(Arguments.of(backend, "one map", "<map><x>1</x><y>s</y></map>", xy));
            cases.add(Arguments.of(backend, "two maps", "<map><x>1</x></map><map><x>2</x></map>", List.of(Map.of("x", "1"), Map.of("x", "2"))));
            cases.add(Arguments.of(backend, "one list of elements", "<list><e>1</e><e>2</e></list>", single(List.of("1", "2"))));
            // Compact JSON text keeps its JSON typing (1 is an Integer, "1" a String), as everywhere else; element text is a String.
            cases.add(Arguments.of(backend, "one list of text", "<list>[1,2]</list>", single(List.of(1, 2))));
            cases.add(Arguments.of(backend, "two lists", "<list>[1]</list><list>[\"2\"]</list>", List.of(List.of(1), List.of("2"))));
            cases.add(Arguments.of(backend, "one array of elements", "<array><e>1</e><e>2</e></array>", single(new Object[] { "1", "2" })));
            cases.add(Arguments.of(backend, "one array of text", "<array>[1,2]</array>", single(new Object[] { 1, 2 })));
            cases.add(Arguments.of(backend, "scalar elements", "<e>1</e><e>2</e>", List.of("1", "2")));
            cases.add(Arguments.of(backend, "one scalar element", "<e>1</e>", "1"));
            cases.add(Arguments.of(backend, "mixed items", "<e>1</e><map><x>1</x></map>", List.of("1", Map.of("x", "1"))));
            cases.add(Arguments.of(backend, "list of maps", "<list><map><x>1</x></map></list>", single(single(Map.of("x", "1")))));
            cases.add(Arguments.of(backend, "one set of elements", "<set><e>1</e><e>2</e></set>", single(Set.of("1", "2"))));
        }
        return cases.stream();
    }

    private static List<Object> single(final Object item) {
        // List.of(array) would spread the array over the list.
        final List<Object> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    /**
     * Standard XML without type information: an open {@code Object} slot holding element children is read the way the
     * writer produced them, on the StAX and DOM readers alike, with or without an Optional/Nullable around the slot. The
     * DOM reader used to wrap a single nested map in a one-element list, the StAX reader used to keep only the first of
     * several children, and both used to read a nested {@code <list>} or {@code <array>} as a map of its {@code <e>}
     * elements.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("untypedChildCases")
    void untypedChildrenOfOpenObjectSlotsAreReadByName(final Backend backend, final String label, final String body, final Object expected) {
        final String mapXml = mapValueDocument(backend, "k", "", body);
        assertDeepEquals(expected, mapValue(backend, mapXml, XmlDeserConfig.create(), "Object"), mapXml);

        final String beanXml = beanDocument(ObjectBean.class, "values", "", body);
        final ObjectBean bean = backend.parser().deserialize(beanXml, XmlDeserConfig.create(), Type.of(ObjectBean.class));
        assertDeepEquals(expected, bean.values, beanXml);

        final Object wrapped = mapValue(backend, mapXml, XmlDeserConfig.create(), "java.util.Optional<Object>");
        assertDeepEquals(expected, assertInstanceOf(Optional.class, wrapped, mapXml).orElseThrow(), mapXml);
    }

    /**
     * The standard writer's own untyped output for nested containers in open slots reads back on both readers.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("standardBackends")
    void untypedWriterOutputOfOpenSlotsRoundTrips(final Backend backend) {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", "1");
        final Map<String, Object> original = new LinkedHashMap<>();
        original.put("nested", inner);
        original.put("maps", List.of(inner, inner));
        original.put("lists", List.of(List.of("1", "2"), List.of("3")));
        original.put("mixed", List.of("1", inner));

        final String xml = backend.parser().serialize(original, XmlSerConfig.create().setWriteTypeInfo(false));
        final Map<?, ?> read = assertInstanceOf(Map.class, backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("Map<String,Object>")), xml);
        assertDeepEquals(original, read, xml);
    }

    static Stream<Arguments> standardBackends() {
        return Stream.of(Arguments.of(Backend.XML_STAX), Arguments.of(Backend.XML_DOM));
    }

    private static void assertDeepEquals(final Object expected, final Object actual, final String xml) {
        if (expected instanceof Object[] expectedArray) {
            assertNotNull(actual, xml);
            assertEquals(Object[].class, actual.getClass(), "Expected an Object[]: " + xml);
            final Object[] actualArray = (Object[]) actual;
            assertEquals(expectedArray.length, actualArray.length, xml);
            for (int i = 0; i < expectedArray.length; i++) {
                assertDeepEquals(expectedArray[i], actualArray[i], xml);
            }
        } else if (expected instanceof List<?> expectedList) {
            final List<?> actualList = assertInstanceOf(List.class, actual, xml);
            assertEquals(expectedList.size(), actualList.size(), "Expected " + expectedList + " but was " + actualList + ": " + xml);
            for (int i = 0; i < expectedList.size(); i++) {
                assertDeepEquals(expectedList.get(i), actualList.get(i), xml);
            }
        } else if (expected instanceof Set<?> expectedSet) {
            assertEquals(expectedSet, assertInstanceOf(Set.class, actual, xml), xml);
        } else if (expected instanceof Map<?, ?> expectedMap) {
            final Map<?, ?> actualMap = assertInstanceOf(Map.class, actual, xml);
            assertEquals(expectedMap.keySet(), actualMap.keySet(), xml);
            for (final Map.Entry<?, ?> entry : expectedMap.entrySet()) {
                assertDeepEquals(entry.getValue(), actualMap.get(entry.getKey()), xml);
            }
        } else {
            assertEquals(expected, actual, xml);
        }
    }

    static Stream<Arguments> containerMetadataCases() {
        return Stream.of(Backend.ABACUS_STAX, Backend.ABACUS_DOM, Backend.ABACUS_SAX)
                .flatMap(backend -> Stream.of("List<Map<String,Object>>", "Map<String,Object>[]").map(declaration -> Arguments.of(backend, declaration)));
    }

    /**
     * Abacus XML: metadata carried by the {@code <list>} or {@code <array>} element alone, without the same type on
     * its {@code <value>} wrapper, types the items as wrapper metadata does. The StAX and DOM readers used to drop it
     * for a declared array and construct the declared {@code Map} items; the array itself stays the declared class.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("containerMetadataCases")
    void metadataOnTheContainerElementAloneTypesTheItems(final Backend backend, final String declaration) {
        final boolean list = declaration.startsWith("List");
        final String metadata = list ? "ArrayList<" + MULTI_VALUE_MAP + "<String,Integer>>" : MULTI_VALUE_MAP + "[]";
        final Object element = 1;
        final String item = element("e", "", "<map><entry><key>a</key><value>1</value></entry></map>");
        final String xml = mapValueDocument(backend, "k", "", element(list ? "list" : "array", metadata, item));

        final Object plain = mapValue(backend, xml, XmlDeserConfig.create(), declaration);
        final Map<?, ?> readItem;
        if (list) {
            readItem = (Map<?, ?>) assertInstanceOf(List.class, plain, xml).get(0);
        } else {
            assertNotNull(plain, xml);
            assertEquals(Map[].class, plain.getClass(), "Metadata never narrows the declared array class: " + xml);
            readItem = (Map<?, ?>) ((Object[]) plain)[0];
        }
        assertInstanceOf(LinkedMultiValueMap.class, readItem, xml);
        assertEquals(Map.of("a", List.of(element)), readItem, xml);

        final Object overridden = mapValue(backend, xml, XmlDeserConfig.create().setValueType("a", String.class), declaration);
        final Map<?, ?> fallback = (Map<?, ?>) (list ? assertInstanceOf(List.class, overridden, xml).get(0) : ((Object[]) overridden)[0]);
        assertEquals(LinkedHashMap.class, fallback.getClass(), "The ordinary fallback map is the same on every backend: " + xml);
        assertEquals(Map.of("a", "1"), fallback, xml);
    }

    private static Person person() {
        final Person person = new Person();
        person.name = "n";
        person.age = 3;
        return person;
    }

    private static Map<String, Object> personMap(final Object age) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "n");
        map.put("age", age);
        return map;
    }

    private static void assertPerson(final Object value, final String xml) {
        final Person person = assertInstanceOf(Person.class, value, xml);
        assertEquals("n", person.name, xml);
        assertEquals(3, person.age, xml);
    }

    /**
     * Remaining issue 1: with type information, a bean item and a nested list item of an open list keep the types their
     * attributes name on every backend, in a map value and at the root of a list or array document. The standard
     * readers used to read every structured item of an open list as a map: the bean as a map of its properties, the
     * nested list as an empty map.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void typedItemsOfOpenListsKeepTheirTypes(final Backend backend) {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", "1");
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("beans", List.of(person(), person()));
        source.put("lists", List.of(List.of("1"), List.of("2")));
        source.put("mixed", List.of("1", person(), inner));
        final XmlSerConfig typed = XmlSerConfig.create().setWriteTypeInfo(true);
        final String xml = backend.parser().serialize(source, typed);
        final Map<?, ?> read = assertInstanceOf(Map.class, backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("Map<String,Object>")), xml);

        final List<?> beans = assertInstanceOf(List.class, read.get("beans"), xml);
        assertEquals(2, beans.size(), xml);
        beans.forEach(bean -> assertPerson(bean, xml));
        assertDeepEquals(List.of(List.of("1"), List.of("2")), read.get("lists"), xml);
        final List<?> mixed = assertInstanceOf(List.class, read.get("mixed"), xml);
        assertEquals(3, mixed.size(), xml);
        assertEquals("1", mixed.get(0), xml);
        assertPerson(mixed.get(1), xml);
        assertDeepEquals(inner, mixed.get(2), xml);

        final List<Object> rootItems = List.of(person(), inner, List.of("1"), "s");
        final String listXml = backend.parser().serialize(rootItems, typed);
        final List<?> readList = assertInstanceOf(List.class, backend.parser().deserialize(listXml, XmlDeserConfig.create(), Type.of("List<Object>")), listXml);
        assertEquals(4, readList.size(), listXml);
        assertPerson(readList.get(0), listXml);
        assertDeepEquals(List.of(inner, List.of("1"), "s"), readList.subList(1, 4), listXml);

        final String arrayXml = backend.parser().serialize(rootItems.toArray(), typed);
        final Object[] readArray = assertInstanceOf(Object[].class, backend.parser().deserialize(arrayXml, XmlDeserConfig.create(), Type.of("Object[]")),
                arrayXml);
        assertEquals(4, readArray.length, arrayXml);
        assertPerson(readArray[0], arrayXml);
        assertDeepEquals(List.of(inner, List.of("1"), "s"), List.of(readArray[1], readArray[2], readArray[3]), arrayXml);
    }

    /**
     * Remaining issue 2: without type information a bean in an open Object slot is read as a map of its properties on
     * every backend, alone or as an item. The Abacus readers used to fail with "No bean class found" on their own
     * writer's output. A declared bean slot still constructs the bean, and a bean root of another element name still
     * reads the requested bean.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void untypedBeansInOpenSlotsReadAsMaps(final Backend backend) {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", "1");
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("bean", person());
        source.put("items", List.of(person(), inner, List.of("1"), "s"));
        final XmlSerConfig untyped = XmlSerConfig.create().setWriteTypeInfo(false);
        final String xml = backend.parser().serialize(source, untyped);
        final Map<?, ?> read = assertInstanceOf(Map.class, backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("Map<String,Object>")), xml);
        assertDeepEquals(personMap("3"), read.get("bean"), xml);
        assertDeepEquals(List.of(personMap("3"), inner, List.of("1"), "s"), read.get("items"), xml);

        final String rootXml = backend.parser().serialize(List.of(person(), inner), untyped);
        assertDeepEquals(List.of(personMap("3"), inner), backend.parser().deserialize(rootXml, XmlDeserConfig.create(), Type.of("List<Object>")), rootXml);

        final Map<String, Object> beanOnly = new LinkedHashMap<>();
        beanOnly.put("bean", person());
        final String beanXml = backend.parser().serialize(beanOnly, untyped);
        final Map<?, ?> declared = assertInstanceOf(Map.class,
                backend.parser().deserialize(beanXml, XmlDeserConfig.create(), Type.of("Map<String," + Person.class.getName() + ">")), beanXml);
        assertPerson(declared.get("bean"), beanXml);

        // A bean root of another element name still reads the requested bean, on every backend.
        final String strangerXml = "<stranger><name>n</name><age>3</age></stranger>";
        assertPerson(backend.parser().deserialize(strangerXml, XmlDeserConfig.create(), Type.of(Person.class)), strangerXml);
    }

    private static void assertSameValue(final Object expected, final Object actual, final String xml) {
        if (expected instanceof Person) {
            assertPerson(actual, xml);
        } else {
            assertDeepEquals(expected, actual, xml);
        }
    }

    /**
     * Remaining issue 1: with type information the standard writer keeps a bean property's declared type on the property
     * and records the value's class on a wrapper child ({@code <values type="Object"><person type="...">}); that child is
     * the value, read with its own type, on every backend. The standard readers used to read a bean child as a map
     * and a list child as a list inside a list.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void typedWrapperChildOfAnObjectPropertyIsTheValue(final Backend backend) throws ReflectiveOperationException {
        final Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("x", "1");
        final List<Object> values = new ArrayList<>();
        values.add(person());
        values.add(new ArrayList<>(List.of("1", 2)));
        values.add(inner);
        values.add(new Object[] { "1", 2 });

        for (final Object value : values) {
            // The Optional properties stay empty: the writers put an Optional<Object> property's value as compact text,
            // which the readers hand back as a String (a writer limitation outside this test).
            final ObjectBean bean = new ObjectBean();
            bean.values = value;
            final String xml = backend.parser().serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));
            final ObjectBean read = backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of(ObjectBean.class));
            assertSameValue(value, read.values, xml);
        }
    }

    /**
     * Remaining issue 2: an empty element is the empty string both writers emit for one, on every backend; a null is
     * written with its marker and stays null. The standard StAX reader used to read an empty element as the type's
     * default, null for a String.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void emptyElementsReadAsEmptyStrings(final Backend backend) {
        for (final boolean typeInfo : new boolean[] { true, false }) {
            final XmlSerConfig config = XmlSerConfig.create().setWriteTypeInfo(typeInfo);
            final Person empty = person();
            empty.name = "";
            final String beanXml = backend.parser().serialize(empty, config);
            final Person readBean = backend.parser().deserialize(beanXml, XmlDeserConfig.create(), Type.of(Person.class));
            assertEquals("", readBean.name, beanXml);
            assertEquals(3, readBean.age, beanXml);

            final Map<String, Object> source = new LinkedHashMap<>();
            source.put("s", "");
            source.put("blank", " ");
            final String mapXml = backend.parser().serialize(source, config);
            final Map<?, ?> readMap = assertInstanceOf(Map.class, backend.parser().deserialize(mapXml, XmlDeserConfig.create(), Type.of("Map<String,Object>")),
                    mapXml);
            assertEquals("", readMap.get("s"), mapXml);
            assertEquals(" ", readMap.get("blank"), mapXml);

            final Person nothing = person();
            nothing.name = null;
            final String nullXml = backend.parser().serialize(nothing, config);
            assertNull(backend.parser().deserialize(nullXml, XmlDeserConfig.create(), Type.of(Person.class)).name, nullXml);
        }
    }

    /**
     * Remaining issue 3: without type information a bean-shaped element is read as a map of its properties inside an
     * untyped {@code <array>} and inside a map value nested in a typed list as well. The Abacus readers used to fail with
     * "No bean class found" in both places (the array assumed String items; the SAX handler judged the slot by the
     * enclosing list's element type).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void untypedBeansInsideArraysAndNestedValuesReadAsMaps(final Backend backend) {
        final XmlSerConfig untyped = XmlSerConfig.create().setWriteTypeInfo(false);
        final Map<String, Object> innerMap = new LinkedHashMap<>();
        innerMap.put("k", person());
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("items", List.of(innerMap));
        final String xml = backend.parser().serialize(source, untyped);
        final Map<?, ?> read = assertInstanceOf(Map.class,
                backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("Map<String,List<Map<String,Object>>>")), xml);
        assertDeepEquals(List.of(Map.of("k", personMap("3"))), read.get("items"), xml);

        if (backend.abacus) {
            // The standard writer has no wrapper for a one-element array, so the array shapes are Abacus only.
            final Map<String, Object> arrays = new LinkedHashMap<>();
            arrays.put("people", new Person[] { person() });
            arrays.put("mixed", new Object[] { person(), "s" });
            final String arrayXml = backend.parser().serialize(arrays, untyped);
            final Map<?, ?> readArrays = assertInstanceOf(Map.class,
                    backend.parser().deserialize(arrayXml, XmlDeserConfig.create(), Type.of("Map<String,Object>")), arrayXml);
            final Object[] people = assertInstanceOf(Object[].class, readArrays.get("people"), arrayXml);
            assertEquals(1, people.length, arrayXml);
            assertDeepEquals(personMap("3"), people[0], arrayXml);
            final Object[] mixed = assertInstanceOf(Object[].class, readArrays.get("mixed"), arrayXml);
            assertEquals(2, mixed.length, arrayXml);
            assertDeepEquals(personMap("3"), mixed[0], arrayXml);
            assertEquals("s", mixed[1], arrayXml);
        }
    }

    /**
     * Reviewer follow-up 1: per-key value types reach the compact text of a root array or list, as they reach it in a
     * map value and in JSON. The root readers used to hand the JSON reader a configuration holding the element type
     * only.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void perKeyValueTypesReachCompactTextOfRootArraysAndLists(final Backend backend) {
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", Long.class);
        final String arrayXml = "<array>[{\"a\":\"1\"}]</array>";
        final Object[] array = assertInstanceOf(Object[].class, backend.parser().deserialize(arrayXml, config, Type.of("Object[]")), arrayXml);
        assertEquals(1L, assertInstanceOf(Map.class, array[0], arrayXml).get("a"), arrayXml);

        final String listXml = "<list>[{\"a\":\"1\"}]</list>";
        final List<?> list = assertInstanceOf(List.class, backend.parser().deserialize(listXml, config, Type.of("List<Object>")), listXml);
        assertEquals(1L, assertInstanceOf(Map.class, list.get(0), listXml).get("a"), listXml);

        // The same text as a map value, the control.
        final String valueXml = mapValueDocument(backend, "k", "", "[{\"a\":\"1\"}]");
        final List<?> value = assertInstanceOf(List.class, mapValue(backend, valueXml, config, "List<Object>"), valueXml);
        assertEquals(1L, assertInstanceOf(Map.class, value.get(0), valueXml).get("a"), valueXml);
    }

    /**
     * Reviewer follow-up 2: an explicit {@code Object} override on a value that carries map metadata adopts that
     * metadata, as any open slot does, in compact text and in child elements; without metadata the text is read
     * literally. The standard DOM reader used to skip the metadata for an overridden slot.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void objectOverrideOnAMetadataSlotAdoptsTheMetadata(final Backend backend) {
        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("k", Object.class);
        final String metadata = MULTI_VALUE_MAP + "<String,Integer>";

        final String compactXml = mapValueDocument(backend, "k", metadata, "{\"x\":[1],\"y\":[2]}");
        final Map<?, ?> compact = assertInstanceOf(LinkedMultiValueMap.class, mapValue(backend, compactXml, override, "Object"), compactXml);
        assertEquals(Map.of("x", List.of(1), "y", List.of(2)), compact, compactXml);

        final String children = backend.abacus ? "<map><entry><key>x</key><value>1</value></entry></map>" : "<map><x>1</x></map>";
        final String childXml = mapValueDocument(backend, "k", metadata, children);
        final Map<?, ?> structured = assertInstanceOf(LinkedMultiValueMap.class, mapValue(backend, childXml, override, "Object"), childXml);
        assertEquals(Map.of("x", List.of(1)), structured, childXml);

        final String plainXml = mapValueDocument(backend, "k", "", "{\"x\":[1]}");
        assertEquals("{\"x\":[1]}", mapValue(backend, plainXml, override, "Object"), plainXml);
    }

    /**
     * Reviewer follow-up 3: per-key value types reach the compact text of an array value with an open component, as
     * they reach a list value, a root array and JSON. The shared array-text reader used to hand an open component to
     * the array type's own codec, which never sees the configuration.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void perKeyValueTypesReachCompactTextOfArrayValues(final Backend backend) {
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", Long.class);
        final String text = "[{\"a\":\"1\"}]";

        final String declaredXml = mapValueDocument(backend, "k", "", text);
        final Object[] declared = assertInstanceOf(Object[].class, mapValue(backend, declaredXml, config, "Object[]"), declaredXml);
        assertEquals(1L, assertInstanceOf(Map.class, declared[0], declaredXml).get("a"), declaredXml);

        final String typedXml = mapValueDocument(backend, "k", "Object[]", text);
        final Object[] typed = assertInstanceOf(Object[].class, mapValue(backend, typedXml, config, "Object"), typedXml);
        assertEquals(1L, assertInstanceOf(Map.class, typed[0], typedXml).get("a"), typedXml);

        final String beanXml = "<arrayBean><values>" + text + "</values></arrayBean>";
        final ArrayBean bean = backend.parser().deserialize(beanXml, config, Type.of(ArrayBean.class));
        assertEquals(1L, assertInstanceOf(Map.class, assertInstanceOf(Object[].class, bean.values, beanXml)[0], beanXml).get("a"), beanXml);

        // Unquoted tokens keep their JSON types either way.
        final String tokensXml = mapValueDocument(backend, "k", "", "[1, true, \"s\"]");
        assertDeepEquals(new Object[] { 1, true, "s" }, mapValue(backend, tokensXml, config, "Object[]"), tokensXml);
    }

    /**
     * Reviewer follow-up 1: the global map value type fills the open values of a map inside compact text as it fills
     * them inside child elements, at any nesting level: the compact JSON reader of the XML parsers applies it to a map
     * in an open slot, which the ordinary JSON reader leaves alone.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void globalMapValueTypeReachesMapsInOpenSlotsOfCompactText(final Backend backend) {
        final XmlDeserConfig longValues = XmlDeserConfig.create().setMapValueType(Long.class);
        final Map<String, Object> expected = Map.of("x", 1L);

        final String listXml = "<list>[{\"x\":1}]</list>";
        final List<?> list = assertInstanceOf(List.class, backend.parser().deserialize(listXml, longValues, Type.of("List<Object>")), listXml);
        assertEquals(expected, list.get(0), listXml);

        final String arrayXml = "<array>[{\"x\":1}]</array>";
        final Object[] array = assertInstanceOf(Object[].class, backend.parser().deserialize(arrayXml, longValues, Type.of("Object[]")), arrayXml);
        assertEquals(expected, array[0], arrayXml);

        // A list property of a bean: below the root, where the global type cannot replace the declared argument.
        final String beanXml = "<listBean><values>[{\"x\":1}]</values></listBean>";
        final ListBean bean = backend.parser().deserialize(beanXml, longValues, Type.of(ListBean.class));
        assertEquals(expected, assertInstanceOf(List.class, bean.values, beanXml).get(0), beanXml);

        // The child-element form, the control, and the ordinary JSON reader, which keeps its own rule.
        final String children = backend.abacus ? "<list><e><map><entry><key>x</key><value>1</value></entry></map></e></list>"
                : "<list><map><x>1</x></map></list>";
        final List<?> structured = assertInstanceOf(List.class, backend.parser().deserialize(children, longValues, Type.of("List<Object>")), children);
        assertEquals(expected, structured.get(0), children);
        assertEquals(Map.of("x", 1), ((List<?>) N.fromJson("[{\"x\":1}]", JsonDeserConfig.create().setMapValueType(Long.class), List.class)).get(0));
    }

    /**
     * Reviewer item 1: a compact map entry keeps its declared scalar type when it meets a JSON array, as in JSON: a
     * String value reads the text literally, an Integer value rejects it, and an explicit String override on an open
     * value reads the text literally too. Only a scalar override on a declared list or array value names the element
     * type. The compact reader used to turn every scalar value type that met an array into a list.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void compactScalarValuesKeepTheirDeclaredTypes(final Backend backend) {
        final String text = "{\"a\":[1,2]}";
        final Object literal = ((Map<?, ?>) N.fromJson(text, Type.of("Map<String,String>"))).get("a");
        assertInstanceOf(String.class, literal);

        final String xml = mapValueDocument(backend, "k", "", text);
        final Map<?, ?> strings = assertInstanceOf(Map.class, mapValue(backend, xml, XmlDeserConfig.create(), "Map<String,String>"), xml);
        assertEquals(literal, strings.get("a"), xml);

        // The mapValue helper asserts that nothing is thrown, so the rejection is checked on the parser directly.
        assertThrows(RuntimeException.class, () -> backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("Map<String,Map<String,Integer>>")), xml);

        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("a", String.class);
        final Map<?, ?> open = assertInstanceOf(Map.class, mapValue(backend, xml, override, "Map<String,Object>"), xml);
        assertEquals(literal, open.get("a"), xml);

        // The intended rule: a scalar override on a declared list value names its element type.
        final Map<?, ?> lists = assertInstanceOf(Map.class, mapValue(backend, xml, override, "Map<String,List<Integer>>"), xml);
        assertEquals(List.of("1", "2"), lists.get("a"), xml);
    }

    static Stream<Arguments> rootArrayMetadataCases() {
        return allBackends().flatMap(backend -> Stream.of(Form.TEXT, Form.CHILDREN).map(form -> Arguments.of(backend, form)));
    }

    /**
     * Reviewer item 2: a root array keeps the requested array class under metadata, as a property does; the metadata
     * refines only the items, and a per-key override then falls back to an ordinary map without an array store
     * failure. Three readers used to construct the metadata's narrower array (and threw once the override reached the
     * entries), one ignored the metadata.
     */
    @ParameterizedTest(name = "{0}, {1}")
    @MethodSource("rootArrayMetadataCases")
    void rootArraysKeepTheRequestedClassUnderMetadata(final Backend backend, final Form form) {
        final String item = backend.abacus ? "<e><map><entry><key>a</key><value>1</value></entry></map></e>" : "<map><a>1</a></map>";
        final String xml = element("array", MULTI_VALUE_MAP + "[]", form == Form.TEXT ? "[{\"a\":1}]" : item);

        final Object[] plain = assertInstanceOf(Object[].class, backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("Map<String,Object>[]")),
                xml);
        assertEquals(Map[].class, plain.getClass(), "Metadata never narrows the requested array class: " + xml);
        assertEquals(1, plain.length, xml);
        assertInstanceOf(LinkedMultiValueMap.class, plain[0], xml);
        assertEquals(Map.of("a", List.of(1)), plain[0], xml);

        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("a", String.class);
        final Object[] overridden = assertInstanceOf(Object[].class, backend.parser().deserialize(xml, override, Type.of("Map<String,Object>[]")), xml);
        assertEquals(Map[].class, overridden.getClass(), xml);
        assertEquals(LinkedHashMap.class, overridden[0].getClass(), "The ordinary fallback map is the same on every backend: " + xml);
        assertEquals(Map.of("a", "1"), overridden[0], xml);
    }

    /**
     * Reviewer item 7: a present key whose override is a scalar type moves a list-valued map to ordinary construction
     * even when its value is null, in compact text and in child elements; without an override a null value keeps the
     * implementation. The guard used to look at non-null values only.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void presentNullWithScalarOverrideMovesToOrdinaryMap(final Backend backend) {
        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("a", String.class);
        final Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("a", null);

        final String compactXml = mapValueDocument(backend, "k", MULTI_VALUE_MAP, "{\"a\":null}");
        final Map<?, ?> compact = assertInstanceOf(Map.class, mapValue(backend, compactXml, override, "Object"), compactXml);
        assertEquals(LinkedHashMap.class, compact.getClass(), compactXml);
        assertEquals(expected, compact, compactXml);

        final String children = backend.abacus ? "<map><entry><key>a</key><value isNull=\"true\"/></entry></map>" : "<map><a isNull=\"true\"/></map>";
        final String childXml = mapValueDocument(backend, "k", MULTI_VALUE_MAP, children);
        final Map<?, ?> structured = assertInstanceOf(Map.class, mapValue(backend, childXml, override, "Object"), childXml);
        assertEquals(LinkedHashMap.class, structured.getClass(), childXml);
        assertEquals(expected, structured, childXml);

        final Map<?, ?> plain = assertInstanceOf(LinkedMultiValueMap.class, mapValue(backend, compactXml, XmlDeserConfig.create(), "Object"), compactXml);
        assertEquals(expected, plain, compactXml);
    }

    /**
     * Reviewer item 6: a global scalar map value type keeps ordinary map construction when a slot declared with open
     * values carries list-valued map metadata, in compact text as in child elements; the metadata's list shape used to
     * be decided before the global type was consulted. Without the global type the metadata still applies.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void globalScalarValueTypeBeatsSpecializedMetadata(final Backend backend) {
        final String metadata = MULTI_VALUE_MAP + "<String,Integer>";
        final XmlDeserConfig strings = XmlDeserConfig.create().setMapValueType(String.class).setValueType("k", Type.of("Map<String,Object>"));
        final XmlDeserConfig declared = XmlDeserConfig.create().setValueType("k", Type.of("Map<String,Object>"));
        final String children = backend.abacus ? "<map><entry><key>a</key><value>1</value></entry></map>" : "<map><a>1</a></map>";

        for (final String body : List.of("{\"a\":\"1\"}", children)) {
            final String xml = mapValueDocument(backend, "k", metadata, body);
            final Map<?, ?> ordinary = assertInstanceOf(Map.class, mapValue(backend, xml, strings, "Map<String,Object>"), xml);
            assertFalse(ordinary instanceof MultiValueMap, xml);
            assertEquals(Map.of("a", "1"), ordinary, xml);

            final Map<?, ?> specialized = assertInstanceOf(LinkedMultiValueMap.class, mapValue(backend, xml, declared, "Map<String,Object>"), xml);
            assertEquals(Map.of("a", List.of(1)), specialized, xml);
        }
    }

    /**
     * Reviewer items 8 and 9: a root map written as compact JSON text is read by every backend, with ASCII or Unicode
     * whitespace around the text, and with the root's key and value types: an explicit global type ahead of the
     * requested arguments, exactly as the root's child elements are read. Four backends used to return an empty map
     * for the compact form, and the fifth read it with the requested arguments instead of the configured ones.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void compactRootMapsReadWithTheRootTypesOnEveryBackend(final Backend backend) {
        for (final String xml : List.of("<map>{\"a\":1}</map>", "<map> {\"a\":1} </map>", "<map>&#x2003;{\"a\":1}&#x2003;</map>",
                "<map><![CDATA[{\"a\":1}]]></map>")) {
            final Map<?, ?> map = backend.parser().deserialize(xml, XmlDeserConfig.create(), Type.of("Map<String,Integer>"));
            assertEquals(Map.of("a", 1), map, xml);
        }
        assertEquals(Map.of(), backend.parser().deserialize("<map> </map>", XmlDeserConfig.create(), Type.of("Map<String,Integer>")));

        final XmlDeserConfig rootTypes = XmlDeserConfig.create().setMapKeyType(Long.class).setMapValueType(String.class);
        final String children = backend.abacus ? "<map><entry><key>1</key><value>1</value></entry></map>" : "<map><e name=\"1\">1</e></map>";
        final Map<?, ?> structured = backend.parser().deserialize(children, rootTypes, Type.of("Map<Integer,Integer>"));
        assertEquals(Map.of(1L, "1"), structured, children);

        final String compact = "<map>{\"1\":1}</map>";
        assertEquals(structured, backend.parser().deserialize(compact, rootTypes, Type.of("Map<Integer,Integer>")), compact);

        // Below the root the declared arguments keep their precedence, in compact text as in child elements (a list
        // root, so that the global map types do not apply to the root itself).
        final String nested = "<list><e>{\"1\":1}</e></list>";
        final List<?> items = backend.parser().deserialize(nested, rootTypes, Type.of("List<Map<Integer,Integer>>"));
        assertEquals(List.of(Map.of(1, 1)), items, nested);
    }

    /**
     * Reviewer items 4 and 5: the configured component types the innermost open elements of nested containers in
     * compact text at parse time, as it types nested child elements: a parameterized component keeps its own
     * arguments two levels down, and a String component keeps every token's text, under array metadata as without
     * it. The elements used to be converted after the JSON reader had typed them (a {@code List<Integer>} stayed, and
     * {@code 1.2300} became {@code "1.23"}). JSON itself types the root container only.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void configuredComponentsReachNestedOpenElementsAtParseTime(final Backend backend) {
        final XmlDeserConfig longLists = XmlDeserConfig.create().setElementType(Type.of("List<Long>"));
        final String nested = mapValueDocument(backend, "k", "", "[[[1,2]]]");
        final Object[] arrays = assertInstanceOf(Object[].class, mapValue(backend, nested, longLists, "Object[][]"), nested);
        assertEquals(Object[][].class, arrays.getClass(), nested);
        assertEquals(List.of(1L, 2L), assertInstanceOf(Object[].class, arrays[0], nested)[0], nested);

        final List<?> lists = assertInstanceOf(List.class, mapValue(backend, nested, longLists, "List<Object[]>"), nested);
        assertEquals(List.of(1L, 2L), assertInstanceOf(Object[].class, lists.get(0), nested)[0], nested);

        final Object[][] json = N.fromJson("[[[1,2]]]", JsonDeserConfig.create().setElementType(Type.of("List<Long>")), Object[][].class);
        assertEquals(List.of(1, 2), json[0][0], "JSON itself types the root container only");

        final XmlDeserConfig strings = XmlDeserConfig.create().setElementType(String.class);
        for (final String metadata : List.of("", "Long[][]")) {
            final String xml = mapValueDocument(backend, "k", metadata, "[[1.2300,1e3]]");
            final Object[] value = assertInstanceOf(Object[].class, mapValue(backend, xml, strings, "Object[][]"), xml);
            assertEquals(Object[][].class, value.getClass(), "Configuration keeps the declared array under metadata: " + xml);
            assertArrayEquals(new Object[] { "1.2300", "1e3" }, assertInstanceOf(Object[].class, value[0], xml), xml);
        }
        final String flat = mapValueDocument(backend, "k", "", "[1.2300,1e3]");
        assertArrayEquals(new Object[] { "1.2300", "1e3" }, assertInstanceOf(Object[].class, mapValue(backend, flat, strings, "Object[]"), flat), flat);
    }

    /**
     * A blank root array element is an empty array of the requested class on every backend, with or without element
     * configuration. The standard DOM reader used to return {@code null} without configuration.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void blankRootArraysAreEmptyArraysOnEveryBackend(final Backend backend) {
        for (final String xml : List.of("<array></array>", "<array> </array>", "<array>\n</array>")) {
            for (final XmlDeserConfig config : List.of(XmlDeserConfig.create(), XmlDeserConfig.create().setElementType(Long.class))) {
                for (final String arrayType : List.of("Object[]", "Long[]", "String[]")) {
                    final Object[] array = backend.parser().deserialize(xml, config, Type.of(arrayType));
                    assertNotNull(array, arrayType + " " + xml);
                    assertEquals(Type.of(arrayType).javaType(), array.getClass(), arrayType + " " + xml);
                    assertEquals(0, array.length, arrayType + " " + xml);
                }
            }
        }
    }

    /**
     * Third review, item 1: a configured container component ({@code List<Object>}, {@code Object[]}) types the open
     * elements of the slot once, at the slot's own open level; it is never applied again to its own elements. The r53
     * compact reader re-applied the component inside the component ({@code [[1,2],[3]]} became {@code [[[1],[2]],[[3]]]}),
     * and the child-element readers typed the elements of a configured list a second time.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void configuredContainerComponentsApplyOnceAtTheirOpenLevel(final Backend backend) {
        final XmlDeserConfig listOfObject = XmlDeserConfig.create().setElementType(Type.of("List<Object>"));
        final XmlDeserConfig objectArray = XmlDeserConfig.create().setElementType(Type.of("Object[]"));
        final List<Object> expected = List.of(List.of(1, 2), List.of(3));

        final String arrayXml = "<array>[[1,2],[3]]</array>";
        assertEquals(expected, Arrays.asList(backend.parser().deserialize(arrayXml, listOfObject, Type.of("Object[]"))), arrayXml);
        final String listXml = "<list>[[1,2],[3]]</list>";
        assertEquals(new HashSet<>(expected), backend.parser().deserialize(listXml, listOfObject, Type.of("Set<Object>")), listXml);
        assertEquals(expected, backend.parser().deserialize(listXml, listOfObject, Type.of("List<List<Object>>")), listXml);

        final Object[] arrays = backend.parser().deserialize(arrayXml, objectArray, Type.of("Object[]"));
        assertEquals(2, arrays.length, arrayXml);
        assertArrayEquals(new Object[] { 1, 2 }, assertInstanceOf(Object[].class, arrays[0], arrayXml), arrayXml);
        assertArrayEquals(new Object[] { 3 }, assertInstanceOf(Object[].class, arrays[1], arrayXml), arrayXml);

        final String arrayBean = "<arrayBean><values>[[1,2],[3]]</values></arrayBean>";
        assertEquals(expected, Arrays.asList(backend.parser().deserialize(arrayBean, listOfObject, Type.of(ArrayBean.class)).values), arrayBean);
        final String listBean = "<listBean><values>[[1,2],[3]]</values></listBean>";
        assertEquals(expected, backend.parser().deserialize(listBean, listOfObject, Type.of(ListBean.class)).values, listBean);

        final String valueXml = mapValueDocument(backend, "k", "", "[[1,2],[3]]");
        assertEquals(expected, Arrays.asList(assertInstanceOf(Object[].class, mapValue(backend, valueXml, listOfObject, "Object[]"), valueXml)), valueXml);

        final String matrixXml = mapValueDocument(backend, "k", "", "[[[1,2]]]");
        final Object[][] lists = assertInstanceOf(Object[][].class, mapValue(backend, matrixXml, listOfObject, "Object[][]"), matrixXml);
        assertEquals(List.of(1, 2), lists[0][0], matrixXml);
        final Object[][] nested = assertInstanceOf(Object[][].class, mapValue(backend, matrixXml, objectArray, "Object[][]"), matrixXml);
        assertArrayEquals(new Object[] { 1, 2 }, assertInstanceOf(Object[].class, nested[0][0], matrixXml), matrixXml);

        // Child elements: a configured list item keeps its own, open elements.
        final String children = backend.abacus
                ? element("array", "", element("e", "", element("list", "", "<e>1</e><e>2</e>")) + element("e", "", element("list", "", "<e>3</e>")))
                : "<list>[1,2]</list><list>[3]</list>";
        final String childXml = mapValueDocument(backend, "k", "", children);
        final Object[] items = assertInstanceOf(Object[].class, mapValue(backend, childXml, listOfObject, "Object[]"), childXml);
        assertEquals(backend.abacus ? List.of(List.of("1", "2"), List.of("3")) : expected, Arrays.asList(items), childXml);
    }

    /**
     * Third review, item 2: a scalar per-key override on a declared array or {@code Set} value keeps the declared
     * container shape in compact text, as it does for child elements: a {@code String[]} for an {@code Integer[]}
     * value, a {@code Set<String>} without duplicates for a {@code Set<Integer>} value. The compact reader used to
     * build an {@code ArrayList} for both.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void scalarValueOverridesKeepTheDeclaredContainerShapeInCompactText(final Backend backend) {
        final XmlDeserConfig override = XmlDeserConfig.create().setValueType("a", String.class);

        final String compact = mapValueDocument(backend, "k", "", "{\"a\":[1,2]}");
        final Map<?, ?> arrays = assertInstanceOf(Map.class, mapValue(backend, compact, override, "Map<String,Integer[]>"), compact);
        assertArrayEquals(new String[] { "1", "2" }, assertInstanceOf(String[].class, arrays.get("a"), compact), compact);

        final String children = mapValueDocument(backend, "k", "",
                backend.abacus ? "<map><entry><key>a</key><value><array><e>1</e><e>2</e></array></value></entry></map>" : "<map><a><e>1</e><e>2</e></a></map>");
        final Map<?, ?> structured = assertInstanceOf(Map.class, mapValue(backend, children, override, "Map<String,Integer[]>"), children);
        assertArrayEquals(new String[] { "1", "2" }, assertInstanceOf(String[].class, structured.get("a"), children), children);

        final String sets = mapValueDocument(backend, "k", "", "{\"a\":[1,1,2]}");
        final Map<?, ?> setValues = assertInstanceOf(Map.class, mapValue(backend, sets, override, "Map<String,Set<Integer>>"), sets);
        assertEquals(Set.of("1", "2"), assertInstanceOf(Set.class, setValues.get("a"), sets), sets);

        final Map<?, ?> lists = assertInstanceOf(Map.class, mapValue(backend, compact, override, "Map<String,List<Integer>>"), compact);
        assertEquals(List.of("1", "2"), lists.get("a"), compact);

        final String root = backend.abacus ? "<map><entry><key>a</key><value>[1,2]</value></entry></map>" : "<map><a>[1,2]</a></map>";
        final Map<?, ?> rootMap = backend.parser().deserialize(root, override, Type.of("Map<String,Integer[]>"));
        assertArrayEquals(new String[] { "1", "2" }, assertInstanceOf(String[].class, rootMap.get("a"), root), root);
    }

    /**
     * Third review, item 3: a per-key array type names the array a map entry is read with, for child elements and
     * compact text on every backend. The Abacus DOM and SAX readers used to keep the map's declared {@code Integer[]}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void namedArrayOverridesApplyOnEveryBackend(final Backend backend) {
        final String children = backend.abacus ? "<map><entry><key>a</key><value><array><e>1</e><e>2</e></array></value></entry></map>"
                : "<map><a><e>1</e><e>2</e></a></map>";
        final Map<?, ?> strings = backend.parser()
                .deserialize(children, XmlDeserConfig.create().setValueType("a", Type.of("String[]")), Type.of("Map<String,Integer[]>"));
        assertArrayEquals(new String[] { "1", "2" }, assertInstanceOf(String[].class, strings.get("a"), children), children);
        final Map<?, ?> longs = backend.parser()
                .deserialize(children, XmlDeserConfig.create().setValueType("a", Type.of("Long[]")), Type.of("Map<String,Integer[]>"));
        assertArrayEquals(new Long[] { 1L, 2L }, assertInstanceOf(Long[].class, longs.get("a"), children), children);

        final String compact = backend.abacus ? "<map><entry><key>a</key><value>[1,2]</value></entry></map>" : "<map><a>[1,2]</a></map>";
        final Map<?, ?> compactStrings = backend.parser()
                .deserialize(compact, XmlDeserConfig.create().setValueType("a", Type.of("String[]")), Type.of("Map<String,Integer[]>"));
        assertArrayEquals(new String[] { "1", "2" }, assertInstanceOf(String[].class, compactStrings.get("a"), compact), compact);
    }

    /**
     * Third review, item 4 and the root contract: a root array or list whose requested element is itself a container
     * keeps that element on every backend, and the configured component types the innermost open elements, in compact
     * text and in child elements. The Abacus root array text used to ignore the component, and every backend used to
     * replace the requested {@code Object[]} element of a root list with the configured {@code Long}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void configuredComponentsReachRootNestedOpenArraysOnEveryBackend(final Backend backend) {
        final XmlDeserConfig longs = XmlDeserConfig.create().setElementType(Long.class);

        final String arrayXml = "<array>[[1,2],[3]]</array>";
        final Object[][] matrix = backend.parser().deserialize(arrayXml, longs, Type.of("Object[][]"));
        assertArrayEquals(new Object[] { 1L, 2L }, matrix[0], arrayXml);
        assertArrayEquals(new Object[] { 3L }, matrix[1], arrayXml);

        final String listXml = "<list>[[1,2],[3]]</list>";
        final List<?> list = backend.parser().deserialize(listXml, longs, Type.of("List<Object[]>"));
        assertEquals(2, list.size(), listXml);
        assertArrayEquals(new Object[] { 1L, 2L }, assertInstanceOf(Object[].class, list.get(0), listXml), listXml);
        assertArrayEquals(new Object[] { 3L }, assertInstanceOf(Object[].class, list.get(1), listXml), listXml);

        final String children = backend.abacus ? element("array", "", element("e", "", element("array", "", "<e>1</e><e>2</e>")))
                : element("array", "", element("array", "", "<e>1</e><e>2</e>"));
        final Object[][] structured = backend.parser().deserialize(children, longs, Type.of("Object[][]"));
        assertArrayEquals(new Object[] { 1L, 2L }, structured[0], children);

        // A requested element that is not open keeps its own arguments.
        final String integers = "<list>[[1,2]]</list>";
        assertEquals(List.of(List.of(1, 2)), backend.parser().deserialize(integers, longs, Type.of("List<List<Integer>>")), integers);
    }

    /**
     * Third review, root contract: at the root an explicit global map value type replaces a requested scalar or open
     * argument (the tested contract), but never a requested container argument, which keeps its shape while the global
     * type applies inside it as it does below the root. A requested {@code Object[]} value used to be replaced by the
     * global type: the array text came back as a String or failed to convert.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void globalMapTypesKeepARequestedContainerArgumentAtTheRoot(final Backend backend) {
        final String arrays = mapValueDocument(backend, "k", "", "[{\"a\":[1,2]}]");
        final Map<?, ?> strings = backend.parser().deserialize(arrays, XmlDeserConfig.create().setMapValueType(String.class), Type.of("Map<String,Object[]>"));
        final Object[] value = assertInstanceOf(Object[].class, strings.get("k"), arrays);
        assertEquals(1, value.length, arrays);
        assertEquals(Map.of("a", "[1, 2]"), value[0], "The global String reaches the open values of the map inside: " + arrays);

        final String numbers = mapValueDocument(backend, "k", "", "[{\"a\":1}]");
        final Map<?, ?> longs = backend.parser().deserialize(numbers, XmlDeserConfig.create().setMapValueType(Long.class), Type.of("Map<String,Object[]>"));
        assertEquals(Map.of("a", 1L), assertInstanceOf(Object[].class, longs.get("k"), numbers)[0], numbers);

        final String lists = mapValueDocument(backend, "k", "", "[1,2]");
        final Map<?, ?> integers = backend.parser()
                .deserialize(lists, XmlDeserConfig.create().setMapValueType(Long.class), Type.of("Map<String,List<Integer>>"));
        assertEquals(List.of(1, 2), integers.get("k"), "A requested List<Integer> value keeps its element: " + lists);

        final String scalar = mapValueDocument(backend, "k", "", "1");
        assertEquals(Map.of("k", 1L), backend.parser().deserialize(scalar, XmlDeserConfig.create().setMapValueType(Long.class), Type.of("Map<String,Integer>")),
                "A requested scalar argument still yields to the global type at the root: " + scalar);
    }

    /**
     * Third review, item 5: a present key whose override cannot hold a {@code List} (a scalar, an explicit Object, an
     * array, a Set) moves a list-valued map to ordinary construction even when its value is null; a {@code List}
     * override keeps the implementation. The guard used to react to a scalar override only.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void presentNullWithAnyNonListOverrideMovesToOrdinaryMap(final Backend backend) {
        final Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("a", null);
        final String compact = mapValueDocument(backend, "k", MULTI_VALUE_MAP, "{\"a\":null}");
        for (final Type<?> override : List.of(Type.of(Object.class), Type.of("String[]"), Type.of("Set<String>"))) {
            final XmlDeserConfig config = XmlDeserConfig.create().setValueType("a", override);
            final Map<?, ?> ordinary = assertInstanceOf(Map.class, mapValue(backend, compact, config, "Object"), compact);
            assertEquals(LinkedHashMap.class, ordinary.getClass(), override.name() + ": " + compact);
            assertEquals(expected, ordinary, compact);
        }
        final Map<?, ?> kept = assertInstanceOf(LinkedMultiValueMap.class,
                mapValue(backend, compact, XmlDeserConfig.create().setValueType("a", Type.of("List<String>")), "Object"), compact);
        assertEquals(expected, kept, compact);

        final String children = mapValueDocument(backend, "k", MULTI_VALUE_MAP,
                backend.abacus ? "<map><entry><key>a</key><value isNull=\"true\"/></entry></map>" : "<map><a isNull=\"true\"/></map>");
        final Map<?, ?> structured = assertInstanceOf(Map.class, mapValue(backend, children, XmlDeserConfig.create().setValueType("a", Object.class), "Object"),
                children);
        assertEquals(LinkedHashMap.class, structured.getClass(), children);
        assertEquals(expected, structured, children);
    }

    /**
     * Found by the differential of the third round: a declared {@code Number[]} value whose metadata names {@code Long[]}
     * holds Long elements on every backend, for compact text and child elements alike. The compact form used to keep
     * JSON-typed Integers, and the child-element form failed on three readers (no text conversion for Number).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void metadataRefinesTheElementsOfAnAbstractReferenceArrayValue(final Backend backend) {
        final String compact = mapValueDocument(backend, "k", "Long[]", "[1,2]");
        final Number[] numbers = assertInstanceOf(Number[].class, mapValue(backend, compact, XmlDeserConfig.create(), "Number[]"), compact);
        assertArrayEquals(new Number[] { 1L, 2L }, numbers, compact);

        final String children = mapValueDocument(backend, "k", "Long[]", backend.abacus ? element("array", "", "<e>1</e><e>2</e>") : "<e>1</e><e>2</e>");
        final Number[] structured = assertInstanceOf(Number[].class, mapValue(backend, children, XmlDeserConfig.create(), "Number[]"), children);
        assertArrayEquals(new Number[] { 1L, 2L }, structured, children);
    }

    /**
     * Fourth review, item 1: a structured item that is the DECLARED container element of its slot ({@code Object[]} in
     * {@code Object[][]} or {@code List<Object[]>}) keeps the element configuration inside it, so the configured
     * component types the open elements nested in the item, for compact item text and for child elements. The r53
     * readers typed the item's elements twice on the standard readers, and the round-8 suppression then cleared the
     * configuration one level too early on every reader. An item the configuration itself typed is still read without it.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void declaredContainerItemsKeepTheConfiguredComponentInside(final Backend backend) {
        final XmlDeserConfig arrays = XmlDeserConfig.create().setElementType(Type.of("Object[]"));
        final String item = backend.abacus ? element("e", "", element("array", "", "[[1,2]]")) : element("array", "", "[[1,2]]");

        final String matrix = element("array", "", item);
        final Object[][] root = backend.parser().deserialize(matrix, arrays, Type.of("Object[][]"));
        assertEquals(1, root.length, matrix);
        assertEquals(1, root[0].length, matrix);
        assertArrayEquals(new Object[] { 1, 2 }, assertInstanceOf(Object[].class, root[0][0], matrix), matrix);

        final String value = mapValueDocument(backend, "k", "", backend.abacus ? element("array", "", item) : item);
        final Object[][] held = assertInstanceOf(Object[][].class, mapValue(backend, value, arrays, "Object[][]"), value);
        assertArrayEquals(new Object[] { 1, 2 }, assertInstanceOf(Object[].class, held[0][0], value), value);

        final String list = element("list", "", item);
        final List<?> items = backend.parser().deserialize(list, arrays, Type.of("List<Object[]>"));
        assertEquals(1, items.size(), list);
        assertArrayEquals(new Object[] { 1, 2 }, assertInstanceOf(Object[].class, assertInstanceOf(Object[].class, items.get(0), list)[0], list), list);

        final String heldList = mapValueDocument(backend, "k", "", backend.abacus ? element("list", "", item) : item);
        final List<?> heldItems = assertInstanceOf(List.class, mapValue(backend, heldList, arrays, "List<Object[]>"), heldList);
        assertArrayEquals(new Object[] { 1, 2 }, assertInstanceOf(Object[].class, assertInstanceOf(Object[].class, heldItems.get(0), heldList)[0], heldList),
                heldList);

        // Control: an item the configuration typed (the open element of a root Object[]) keeps its own elements open.
        final String configured = element("array", "",
                backend.abacus ? element("e", "", element("array", "", "<e>1</e><e>2</e>")) : element("array", "", "<e>1</e><e>2</e>"));
        final Object[] open = backend.parser().deserialize(configured, arrays, Type.of("Object[]"));
        assertArrayEquals(new Object[] { "1", "2" }, assertInstanceOf(Object[].class, open[0], configured), configured);
    }

    /**
     * Fourth review, item 2: the standard readers read the structured items of a nested array slot
     * ({@code Map<String,Object[][]>}, {@code Map<String,List<Object[]>>}) with the slot's declared element, and the
     * configured component applies inside each item, as the Abacus readers do. The standard readers used to take the
     * configured component ({@code List<Object>}) as the item type and failed to store it in the {@code Object[][]}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void standardReadersKeepTheDeclaredItemOfANestedArraySlot(final Backend backend) {
        final XmlDeserConfig lists = XmlDeserConfig.create().setElementType(Type.of("List<Object>"));
        final String item = backend.abacus ? element("e", "", element("array", "", "[[1,2]]")) : element("array", "", "[[1,2]]");

        final String matrix = mapValueDocument(backend, "k", "", backend.abacus ? element("array", "", item) : item);
        final Object[][] held = assertInstanceOf(Object[][].class, mapValue(backend, matrix, lists, "Object[][]"), matrix);
        assertEquals(List.of(1, 2), held[0][0], matrix);

        final String list = mapValueDocument(backend, "k", "", backend.abacus ? element("list", "", item) : item);
        final List<?> items = assertInstanceOf(List.class, mapValue(backend, list, lists, "List<Object[]>"), list);
        assertEquals(List.of(1, 2), assertInstanceOf(Object[].class, items.get(0), list)[0], list);

        final XmlDeserConfig longs = XmlDeserConfig.create().setElementType(Long.class);
        final String scalars = mapValueDocument(backend, "k", "",
                backend.abacus ? element("array", "", element("e", "", element("array", "", "[1,2]"))) : element("array", "", "[1,2]"));
        final Object[][] typed = assertInstanceOf(Object[][].class, mapValue(backend, scalars, longs, "Object[][]"), scalars);
        assertArrayEquals(new Object[] { 1L, 2L }, typed[0], scalars);
    }

    /**
     * Fourth review, item 3: the global map value type reaches the open values of a map nested in compact text at the
     * root and under a per-key type, as it reaches them in child elements. The compact reader used to replace the
     * global type with the nested map's own {@code Object} argument when it entered the nested map.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("allBackends")
    void globalMapValueTypeReachesNestedMapsOfCompactText(final Backend backend) {
        final XmlDeserConfig longs = XmlDeserConfig.create().setMapValueType(Long.class);
        final Map<String, Map<String, Object>> expected = Map.of("k", Map.of("a", 1L));

        final String compact = "<map>{\"k\":{\"a\":1}}</map>";
        assertEquals(expected, backend.parser().deserialize(compact, longs, Type.of("Map<String,Map<String,Object>>")), compact);

        final String children = mapValueDocument(backend, "k", "",
                backend.abacus ? "<map><entry><key>a</key><value>1</value></entry></map>" : "<map><a>1</a></map>");
        assertEquals(expected, backend.parser().deserialize(children, longs, Type.of("Map<String,Map<String,Object>>")), children);

        final String value = mapValueDocument(backend, "k", "", "{\"a\":1}");
        assertEquals(expected, backend.parser().deserialize(value, longs, Type.of("Map<String,Map<String,Object>>")), value);

        final XmlDeserConfig pinned = XmlDeserConfig.create().setMapValueType(Long.class).setValueType("k", Type.of("Map<String,Object>"));
        assertEquals(expected, backend.parser().deserialize(compact, pinned, Type.of("Map<String,Map<String,Object>>")), compact);
    }

    public static class Person {
        public String name;
        public int age;
    }

    public static class ObjectBean {
        public Object values;
        public Optional<Object> jdk;
        public u.Optional<Object> optional;
        public u.Nullable<Object> nullable;
    }

    public static class ListBean {
        public List<Object> values;
        public Optional<List<Object>> jdk;
        public u.Optional<List<Object>> optional;
        public u.Nullable<List<Object>> nullable;
    }

    public static class ArrayBean {
        public Object[] values;
        public Optional<Object[]> jdk;
        public u.Optional<Object[]> optional;
        public u.Nullable<Object[]> nullable;
    }

    public static class LongArrayBean {
        public Long[] values;
        public Optional<Long[]> jdk;
        public u.Optional<Long[]> optional;
        public u.Nullable<Long[]> nullable;
    }

    public static class StringArrayBean {
        public String[] values;
        public Optional<String[]> jdk;
        public u.Optional<String[]> optional;
        public u.Nullable<String[]> nullable;
    }

    public static class CharArrayBean {
        public char[] values;
        public Optional<char[]> jdk;
        public u.Optional<char[]> optional;
        public u.Nullable<char[]> nullable;
    }

    public static class BooleanArrayBean {
        public boolean[] values;
        public Optional<boolean[]> jdk;
        public u.Optional<boolean[]> optional;
        public u.Nullable<boolean[]> nullable;
    }

    public static class ByteArrayBean {
        public byte[] values;
        public Optional<byte[]> jdk;
        public u.Optional<byte[]> optional;
        public u.Nullable<byte[]> nullable;
    }

    public static class ShortArrayBean {
        public short[] values;
        public Optional<short[]> jdk;
        public u.Optional<short[]> optional;
        public u.Nullable<short[]> nullable;
    }

    public static class IntArrayBean {
        public int[] values;
        public Optional<int[]> jdk;
        public u.Optional<int[]> optional;
        public u.Nullable<int[]> nullable;
    }

    public static class PrimitiveLongArrayBean {
        public long[] values;
        public Optional<long[]> jdk;
        public u.Optional<long[]> optional;
        public u.Nullable<long[]> nullable;
    }

    public static class FloatArrayBean {
        public float[] values;
        public Optional<float[]> jdk;
        public u.Optional<float[]> optional;
        public u.Nullable<float[]> nullable;
    }

    public static class DoubleArrayBean {
        public double[] values;
        public Optional<double[]> jdk;
        public u.Optional<double[]> optional;
        public u.Nullable<double[]> nullable;
    }

    public static class ArrayItem {
        public long id;
        public String name;

        public ArrayItem() {
        }

        ArrayItem(final long id, final String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof ArrayItem item && id == item.id && Objects.equals(name, item.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }
}
