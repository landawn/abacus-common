package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.http.HARUtil;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.u;

/** Regression tests for the follow-up XML policy and HAR validation findings. */
@org.junit.jupiter.api.Tag("unit")
public class ReviewFollowupParserTest extends TestBase {
    private static List<AbstractXmlParser> parsers() {
        return parsers(Set.of(ScalarBean.class));
    }

    private static List<AbstractXmlParser> parsers(final Set<Class<?>> approved) {
        final List<AbstractXmlParser> parsers = new ArrayList<>();
        for (final XmlParserType backend : XmlParserType.values()) {
            parsers.add(new AbacusXmlParserImpl(backend, null, null, approved));
            if (backend != XmlParserType.SAX) {
                parsers.add(new XmlParserImpl(backend, null, null, approved));
            }
        }
        return parsers;
    }

    @Test
    public void ordinaryXmlParsersReadTheirOwnApplicationTypesAndCallerTargets() {
        final ScalarBean bean = new ScalarBean();
        bean.setValue(7);
        final List<AbstractXmlParser> defaults = List.of(new AbacusXmlParserImpl(XmlParserType.SAX), new AbacusXmlParserImpl(XmlParserType.DOM),
                new AbacusXmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM), new XmlParserImpl(XmlParserType.StAX));
        for (final AbstractXmlParser parser : defaults) {
            // The caller's target class is sufficient; a preliminary serialize/register call is unnecessary.
            final String direct = "<scalarBean type='" + ScalarBean.class.getCanonicalName() + "'><value>7</value></scalarBean>";
            assertEquals(7, parser.deserialize(direct, ScalarBean.class).getValue());
            final String xml = parser.serialize(bean, XmlSerConfig.create().setWriteTypeInfo(true));
            assertEquals(7, parser.deserialize(xml, ScalarBean.class).getValue());
            final String items = parser.serialize(new ArrayList<>(List.of(bean)), XmlSerConfig.create().setWriteTypeInfo(true));
            final List<?> back = parser.deserialize(items, Type.of("List<" + ScalarBean.class.getCanonicalName() + ">"));
            assertEquals(7, ((ScalarBean) back.get(0)).getValue());
            assertSame(ScalarBean[].class, parser.resolveTypeAttribute(ScalarBean.class.getCanonicalName() + "[]").javaType());
            assertSame(ScalarBean.class, parser.resolveTypeAttribute("List<" + ScalarBean.class.getCanonicalName() + ">").elementType().javaType());
            assertNull(parser.resolveTypeAttribute("unregistered.application.ClassMustNotBeLoaded"));
            assertNull(parser.resolveTypeAttribute("List<unregistered.application.ClassMustNotBeLoaded>"));
            // A registration alias is never permission, including on the compatibility factories.
            final String alias = "review.application.AliasNotAClassName";
            if (com.landawn.abacus.type.TypeFactory.getTypeIfPresent(alias) == null) {
                com.landawn.abacus.type.TypeFactory.registerType(alias, AliasOnly.class, value -> "alias", value -> new AliasOnly());
            }
            assertNull(parser.resolveTypeAttribute(alias));
            assertThrows(ParsingException.class, () -> parser.deserialize(direct.replace(ScalarBean.class.getCanonicalName(), alias), ScalarBean.class));
        }
        // A different target type forces Beans' XML fallback even on machines where Kryo is available.
        final ScalarCopy copy = com.landawn.abacus.util.Beans.deepCopyAs(bean, ScalarCopy.class);
        assertEquals(7, copy.getValue());
    }

    public static class ScalarCopy {
        private Integer value;

        public Integer getValue() {
            return value;
        }

        public void setValue(final Integer value) {
            this.value = value;
        }
    }

    public static class AliasOnly {
    }

    @Test
    public void explicitApprovalUsesTheResolvedTypeOfAnEnumConstantBody() {
        final Class<?> constantBody = ApprovedEnum.VALUE.getClass();
        assertNotEquals(ApprovedEnum.class, constantBody);
        for (final AbstractXmlParser parser : parsers(Set.of(constantBody))) {
            // Type.of normalizes a constant-specific subclass to its declaring enum. Class approval must
            // retain that resolved class while the name policy remains limited to the supplied class names.
            assertSame(ApprovedEnum.class, parser.resolveTypeAttribute(constantBody.getName()).javaType());
            assertNull(parser.resolveTypeAttribute(ApprovedEnum.class.getCanonicalName()));
        }
    }

    public enum ApprovedEnum {
        VALUE {
        }
    }

    @Test
    public void emptyObjectSerializationHonorsTheFlagAtRootAndNestedPositions() throws Exception {
        for (final AbstractXmlParser parser : parsers()) {
            for (final boolean names : new boolean[] { false, true }) {
                for (final boolean pretty : new boolean[] { false, true }) {
                    final XmlSerConfig config = XmlSerConfig.create()
                            .setFailOnEmptyBean(false)
                            .setWriteTypeInfo(true)
                            .setTagByPropertyName(names)
                            .setPrettyFormat(pretty);
                    final EmptyClass empty = new EmptyClass();
                    final RawObjectHolder holder = new RawObjectHolder();
                    holder.setValue(empty);
                    for (final Object value : List.of(empty, holder, new LinkedHashMap<>(Map.of("value", empty)), new ArrayList<>(List.of(empty)),
                            new Object[] { empty })) {
                        final String xml = parser.serialize(value, config);
                        final org.w3c.dom.Document doc = com.landawn.abacus.util.XmlUtil.createDOMParser(false, false)
                                .parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
                        assertNotNull(doc.getDocumentElement());
                        assertTrue(xml.contains(EmptyClass.class.getCanonicalName()), xml);
                        assertThrows(ParsingException.class, () -> parser.serialize(value));
                    }
                }
            }
        }
    }

    public static class EmptyClass {
    }

    public static class RawObjectHolder {
        private Object value;

        public Object getValue() {
            return value;
        }

        public void setValue(final Object value) {
            this.value = value;
        }
    }

    @Test
    public void harNullOptionalNodesAreAbsentButRequiredNodesRemainValidated() {
        for (final String har : List.of("{\"log\":null}", "{\"log\":{\"entries\":null}}")) {
            assertTrue(HARUtil.findRequestEntry(har, url -> true).isEmpty());
        }
        final Map<String, Object> request = new LinkedHashMap<>();
        request.put("headers", null);
        request.put("postData", null);
        assertTrue(HARUtil.getHeadersByRequestEntry(request).headerNames().isEmpty());
        assertEquals(Tuple.of(null, null), HARUtil.getBodyAndMimeTypeByRequestEntry(request));
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("params", null);
        body.put("text", null);
        body.put("mimeType", null);
        request.put("postData", body);
        assertDoesNotThrow(() -> HARUtil.getBodyAndMimeTypeByRequestEntry(request));
        final String valid = "{\"log\":{\"entries\":[{\"request\":{\"url\":\"https://example.test/\",\"method\":\"GET\",\"headers\":null,\"postData\":null}}]}}";
        assertTrue(HARUtil.findRequestEntry(valid, url -> true).isPresent());
        assertTrue(HARUtil.findRequestEntry(valid.replace("\"GET\"", "null"), url -> true).isPresent());
        for (final String bad : List.of(valid.replace("\"https://example.test/\"", "null"), valid.replace("\"headers\":null", "\"headers\":[null]"),
                valid.replace("\"postData\":null", "\"postData\":{\"params\":[null]}"))) {
            assertThrows(IllegalArgumentException.class, () -> HARUtil.findRequestEntry(bad, url -> false));
        }
    }

    @Test
    public void jdkCollectionFactoryValuesRoundTripWithTypeInformation() {
        final List<Object> sources = List.of(List.of(), List.of("a"), List.of("a", "b", "c"), Set.of(), Set.of("a"), Set.of("a", "b", "c"), Map.of(),
                Map.of("a", "x"), Map.of("a", "x", "b", "y"), Arrays.asList("a", "b"), Collections.nCopies(2, "a"), Collections.emptyList(),
                Collections.emptySet(), Collections.emptyMap(), Collections.emptyNavigableSet(), Collections.emptyNavigableMap(),
                Collections.singletonList("a"), Collections.singleton("a"), Collections.singletonMap("a", "x"),
                Collections.unmodifiableList(new ArrayList<>(List.of("a"))), Collections.unmodifiableList(new LinkedList<>(List.of("a"))),
                Collections.unmodifiableSet(Set.of("a")), Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of("a"))),
                Collections.unmodifiableMap(Map.of("a", "x")), Collections.unmodifiableNavigableMap(new TreeMap<>(Map.of("a", "x"))),
                Collections.synchronizedList(new ArrayList<>(List.of("a"))), Collections.synchronizedMap(new LinkedHashMap<>(Map.of("a", "x"))),
                Collections.checkedList(new ArrayList<>(List.of("a")), String.class),
                Collections.checkedMap(new LinkedHashMap<>(Map.of("a", "x")), String.class, String.class));
        for (final AbstractXmlParser parser : parsers()) {
            for (final Object source : sources) {
                for (final boolean pretty : new boolean[] { false, true }) {
                    final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true).setPrettyFormat(pretty));
                    final Class<?> target = source instanceof Map ? Map.class : source instanceof Set ? Set.class : List.class;
                    assertEquals(source, parser.deserialize(xml, target), xml);
                }
            }
            // Exact trusted class names do not approve lookalikes or unknown generic components.
            assertNull(parser.resolveTypeAttribute("java.util.Collections.NotAnApprovedWrapper"));
            assertNull(parser.resolveTypeAttribute(List.of().getClass().getCanonicalName() + "<unapproved.Payload>"));
        }
    }

    @Test
    public void nestedJdkFactoryValuesKeepTheirExistingDomAndSaxSupport() {
        // StAX cannot instantiate an undeclared nested JDK wrapper even before the type-approval change.
        // DOM and SAX already support this case, so type approval must preserve that behavior.
        for (final AbstractXmlParser parser : List.of(new AbacusXmlParserImpl(XmlParserType.SAX, null, null, Set.of()),
                new AbacusXmlParserImpl(XmlParserType.DOM, null, null, Set.of()), new XmlParserImpl(XmlParserType.DOM, null, null, Set.of()))) {
            final Map<String, Object> source = new LinkedHashMap<>(Map.of("items", List.of(Map.of("key", "value"))));
            final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
            assertEquals(source, parser.deserialize(xml, Map.class), xml);
        }
    }

    @Test
    public void primitiveListsAndMutableScalarsRoundTripWithTypeInformation() {
        final List<Object> values = List.of(com.landawn.abacus.util.BooleanList.of(true, false), com.landawn.abacus.util.CharList.of('a', 'b'),
                com.landawn.abacus.util.ByteList.of((byte) 1), com.landawn.abacus.util.ShortList.of((short) 2), com.landawn.abacus.util.IntList.of(1, 2),
                com.landawn.abacus.util.LongList.of(3L), com.landawn.abacus.util.FloatList.of(1.5F), com.landawn.abacus.util.DoubleList.of(2.5),
                com.landawn.abacus.util.MutableBoolean.of(true), com.landawn.abacus.util.MutableChar.of('c'), com.landawn.abacus.util.MutableByte.of((byte) 1),
                com.landawn.abacus.util.MutableShort.of((short) 2), com.landawn.abacus.util.MutableInt.of(3), com.landawn.abacus.util.MutableLong.of(4),
                com.landawn.abacus.util.MutableFloat.of(1.5F), com.landawn.abacus.util.MutableDouble.of(2.5));
        for (final AbstractXmlParser parser : parsers()) {
            for (final Object value : values) {
                final Class<?> cls = value.getClass();
                for (final String name : List.of(cls.getSimpleName(), cls.getCanonicalName())) {
                    assertSame(cls, parser.resolveTypeAttribute(name).javaType(), name);
                    assertSame(Array.newInstance(cls, 0).getClass(), parser.resolveTypeAttribute(name + "[]").javaType(), name);
                    assertSame(cls, parser.resolveTypeAttribute("List<" + name + ">").parameterTypes().get(0).javaType(), name);
                }
                final Map<String, Object> source = new LinkedHashMap<>(Map.of("value", value));
                final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
                assertEquals(source, parser.deserialize(xml, Map.class), xml);
            }
            assertNull(parser.resolveTypeAttribute("unapproved.IntList"));
        }
    }

    @Test
    public void emptyEnumTokensRoundTripWithoutTurningNullMarkersIntoValues() {
        for (final AbstractXmlParser parser : parsers(Set.of(EnumBean.class, EmptyToken.class, EmptyValueOnly.class))) {
            for (final XmlSerConfig config : List.of(XmlSerConfig.create(), XmlSerConfig.create().setWriteTypeInfo(true),
                    XmlSerConfig.create().setTagByPropertyName(false).setPrettyFormat(true).setWriteTypeInfo(true))) {
                config.setExclusion(Exclusion.NONE);
                for (final EmptyToken token : new EmptyToken[] { EmptyToken.EMPTY, null, EmptyToken.OTHER }) {
                    final EnumBean source = new EnumBean();
                    source.setToken(token);
                    source.setValueOnly(EmptyValueOnly.EMPTY);
                    final String xml = parser.serialize(source, config);
                    final EnumBean back = parser.deserialize(xml, EnumBean.class);
                    assertSame(token, back.getToken(), xml);
                    assertSame(EmptyValueOnly.EMPTY, back.getValueOnly(), xml);
                }
            }
            for (final String element : List.of("<token/>", "<token></token>")) {
                assertSame(EmptyToken.EMPTY, parser.deserialize("<enumBean>" + element + "</enumBean>", EnumBean.class).getToken());
            }
            final EnumBean nulls = parser.deserialize("<enumBean><token isNull='true'/><valueOnly isNull='true'/></enumBean>", EnumBean.class);
            assertNull(nulls.getToken());
            assertNull(nulls.getValueOnly());
            assertNull(parser.deserialize("<enumBean><token isNull='true'>invalid</token></enumBean>", EnumBean.class).getToken());
        }
    }

    @Test
    public void emptyEnumMapValuesUseTheirCodecAndKeepNullValues() {
        final Map<String, EmptyToken> source = new LinkedHashMap<>();
        source.put("empty", EmptyToken.EMPTY);
        source.put("absent", null);
        source.put("other", EmptyToken.OTHER);
        final XmlDeserConfig config = XmlDeserConfig.create().setMapValueType(EmptyToken.class);
        for (final AbstractXmlParser parser : parsers(Set.of(EmptyToken.class))) {
            final String xml = parser.serialize(source, XmlSerConfig.create().setExclusion(Exclusion.NONE));
            assertEquals(source, parser.deserialize(xml, config, Map.class), xml);
            if (parser instanceof XmlParserImpl) {
                final MapEntity entity = parser.deserialize(xml, config, MapEntity.class);
                assertSame(EmptyToken.EMPTY, entity.get("empty"));
                assertNull(entity.get("absent"));
                assertSame(EmptyToken.OTHER, entity.get("other"));
            }
        }
        // The StAX contract for an empty String property has not changed.
        assertNull(new XmlParserImpl(XmlParserType.StAX).deserialize("<stringBean><value/></stringBean>", StringBean.class).getValue());
    }

    @Test
    public void enumNullResultsAreNotReparsedAsEmptyTokens() {
        for (final AbstractXmlParser parser : parsers(Set.of(EnumBean.class, EmptyToken.class, EmptyValueOnly.class))) {
            for (final String rootType : List.of("", " type='" + EnumBean.class.getCanonicalName() + "'")) {
                for (final String token : List.of("null", "absent", "<![CDATA[null]]>", "nu<!--split-->ll")) {
                    final String xml = "<enumBean" + rootType + "><token>" + token + "</token><valueOnly>null</valueOnly></enumBean>";
                    final EnumBean bean = parser.deserialize(xml, EnumBean.class);
                    assertNull(bean.getToken(), xml);
                    assertNull(bean.getValueOnly(), xml);
                }
                // Actual empty input still reaches the codec, including after a null-valued property.
                final EnumBean bean = parser.deserialize("<enumBean" + rootType + "><token>null</token><valueOnly/><token/></enumBean>", EnumBean.class);
                assertSame(EmptyToken.EMPTY, bean.getToken());
                assertSame(EmptyValueOnly.EMPTY, bean.getValueOnly());
            }
        }
    }

    public enum EmptyToken {
        EMPTY, OTHER;

        @com.fasterxml.jackson.annotation.JsonValue
        public String token() {
            return this == EMPTY ? "" : "other";
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static EmptyToken from(final String value) {
            if (value.isEmpty())
                return EMPTY;
            if (value.equals("other"))
                return OTHER;
            if (value.equals("absent"))
                return null; // A nonempty token may legitimately decode to null.
            throw new IllegalArgumentException(value);
        }
    }

    public enum EmptyValueOnly {
        EMPTY;

        @com.fasterxml.jackson.annotation.JsonValue
        public String token() {
            return "";
        }
    }

    public static class EnumBean {
        private EmptyToken token;
        private EmptyValueOnly valueOnly;

        public EmptyToken getToken() {
            return token;
        }

        public void setToken(final EmptyToken token) {
            this.token = token;
        }

        public EmptyValueOnly getValueOnly() {
            return valueOnly;
        }

        public void setValueOnly(final EmptyValueOnly valueOnly) {
            this.valueOnly = valueOnly;
        }
    }

    public static class StringBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Test
    public void builtInAliasesAndTheirArrayAndGenericFormsRemainApproved() {
        final Map<String, Class<?>> aliases = Map.of("Duration", Duration.class, "Optional", u.Optional.class, "OptionalInt", u.OptionalInt.class,
                "OptionalLong", u.OptionalLong.class, "OptionalDouble", u.OptionalDouble.class);
        for (final AbstractXmlParser parser : parsers()) {
            for (final Map.Entry<String, Class<?>> alias : aliases.entrySet()) {
                for (final String name : List.of(alias.getKey(), alias.getValue().getName(), alias.getValue().getCanonicalName())) {
                    assertSame(alias.getValue(), parser.resolveTypeAttribute(name).javaType(), name);
                    assertSame(Array.newInstance(alias.getValue(), 0).getClass(), parser.resolveTypeAttribute(name + "[]").javaType(), name);
                    assertSame(alias.getValue(), parser.resolveTypeAttribute("List<" + name + ">").parameterTypes().get(0).javaType(), name);
                }
            }
            assertSame(String.class, parser.resolveTypeAttribute("Optional<String>").parameterTypes().get(0).javaType());
            assertNull(parser.resolveTypeAttribute("List<unapproved.Payload>"));
        }
    }

    @Test
    public void durationWithTypeInformationRoundTripsThroughEveryBackend() {
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("duration", Duration.ofMillis(7));
        for (final AbstractXmlParser parser : parsers()) {
            final String xml = parser.serialize(source, XmlSerConfig.create().setWriteTypeInfo(true));
            assertTrue(xml.contains("Duration"), xml);
            assertEquals(source, parser.deserialize(xml, Map.class), xml);
        }
    }

    @Test
    public void scalarAndConfiguredBeanPropertiesAlwaysValidateTypeAttributes() {
        for (final AbstractXmlParser parser : parsers()) {
            for (final XmlDeserConfig config : List.of(XmlDeserConfig.create(), XmlDeserConfig.create().setValueType("value", Integer.class))) {
                for (final String rootType : List.of("", " type='" + ScalarBean.class.getCanonicalName() + "'")) {
                    for (final String bad : List.of("unapproved.Payload", "List&lt;unapproved.Payload&gt;")) {
                        final String prefix = "<scalarBean" + rootType + "><value type='" + bad + "'";
                        assertTrue(assertThrows(ParsingException.class, () -> parser.deserialize(prefix + ">7</value></scalarBean>", config, ScalarBean.class))
                                .getMessage()
                                .contains("not allowed"));
                        assertThrows(ParsingException.class, () -> parser.deserialize(prefix + " isNull='true'/></scalarBean>", config, ScalarBean.class));
                    }
                    for (final String attribute : List.of("", " type='   '", " type='Integer'", " type='String'")) {
                        final String xml = "<scalarBean" + rootType + "><value" + attribute + ">7</value></scalarBean>";
                        assertEquals(7, parser.deserialize(xml, config, ScalarBean.class).getValue(), xml);
                    }
                }
            }
        }
    }

    @Test
    public void configuredMapValuesCannotBypassTypeApproval() {
        final XmlDeserConfig config = XmlDeserConfig.create().setValueType("value", Integer.class);
        for (final AbstractXmlParser parser : parsers()) {
            final String xml = parser instanceof AbacusXmlParserImpl ? "<map><entry><key>value</key><value type='unapproved.Payload'>7</value></entry></map>"
                    : "<map><value type='unapproved.Payload'>7</value></map>";
            assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, Map.class));
        }
    }

    @Test
    public void nullCollectionItemsStillValidateTypeAttributes() {
        for (final AbstractXmlParser parser : parsers()) {
            final String item = "<e type='unapproved.Payload' isNull='true'/>";
            assertTrue(assertThrows(ParsingException.class, () -> parser.deserialize("<list>" + item + "</list>", List.class)).getMessage()
                    .contains("not allowed"));
            if (parser instanceof XmlParserImpl) {
                final XmlDeserConfig config = XmlDeserConfig.create().setValueType("items", List.class);
                assertTrue(
                        assertThrows(ParsingException.class, () -> parser.deserialize("<map><items>" + item + "</items></map>", config, Map.class)).getMessage()
                                .contains("not allowed"));
            }
        }
    }

    @Test
    public void embeddedJsonRejectsPresentNullNullableAtEveryNestingLevel() {
        final u.Nullable<Object> presentNull = u.Nullable.of((Object) null);
        final NullableBean bean = new NullableBean();
        bean.setValues(List.of(presentNull));
        final RawBean rawBean = new RawBean();
        rawBean.setPayload(Tuple.of(bean));
        final List<Object> values = Arrays.asList(List.of(presentNull), new u.Nullable<?>[] { presentNull }, List.of(List.of(presentNull)),
                Tuple.of("v", presentNull), Pair.of("v", presentNull), u.Optional.of(List.of(presentNull)), java.util.Optional.of(List.of(presentNull)),
                Holder.of(List.of(presentNull)), Map.of("payload", List.of(presentNull)), bean, Tuple.of(bean), Map.of("payload", Tuple.of(bean)), rawBean);
        for (final AbstractXmlParser parser : parsers()) {
            for (final boolean circular : new boolean[] { false, true }) {
                for (final boolean emptyBean : new boolean[] { false, true }) {
                    final XmlSerConfig config = XmlSerConfig.create().setCircularReferenceSupported(circular).setFailOnEmptyBean(emptyBean);
                    for (final Object value : values) {
                        assertThrows(ParsingException.class, () -> parser.serialize(value, config), parser.getClass().getSimpleName() + ": " + value);
                    }
                    assertDoesNotThrow(() -> parser.serialize(List.of(u.Nullable.empty(), u.Nullable.of("v")), config));
                }
            }
        }
        // The stricter XML policy must not leak into ordinary JSON serialization.
        assertEquals("[null]", ParserFactory.createJsonParser().serialize(List.of(presentNull)));
    }

    @Test
    public void embeddedJsonStillPreservesEscapedNulCharacters() {
        for (final XmlParserType backend : List.of(XmlParserType.StAX, XmlParserType.DOM)) {
            final XmlParser parser = new XmlParserImpl(backend);
            final String xml = parser.serialize(List.of(Character.valueOf((char) 0)));
            assertTrue(xml.contains("\\u0000"), xml);
            assertEquals(List.of(Character.valueOf((char) 0)), parser.deserialize(xml, null, Type.of("List<Character>")));
        }
    }

    @Test
    public void nullablePolicyIsCopiedAndParticipatesInConfigEquality() {
        final JsonSerConfig defaults = JsonSerConfig.create();
        final JsonSerConfig strict = new ParserUtil.XmlEmbeddedJsonConfig();
        assertFalse(defaults instanceof ParserUtil.XmlEmbeddedJsonConfig);
        assertTrue(strict.copy() instanceof ParserUtil.XmlEmbeddedJsonConfig);
        assertNotEquals(defaults, strict);
        assertNotEquals(strict, defaults);
        assertEquals(strict, strict.copy());
        assertEquals(strict.hashCode(), strict.copy().hashCode());
        assertFalse(strict.toString().contains("failOnPresentNullNullable"));
        assertThrows(ParsingException.class, () -> ParserFactory.createJsonParser().serialize(List.of(u.Nullable.of(null)), strict));
        assertThrows(ParsingException.class, () -> ParserFactory.createJsonParser().serialize(List.of(u.Nullable.of(null)), strict.copy()));
        assertEquals("[null]", ParserFactory.createJsonParser().serialize(List.of(u.Nullable.empty()), strict));
        assertEquals(defaults, JsonSerConfig.create());
    }

    @Test
    public void harOriginalBodyValidatesParamsWithoutConvertingOrEncodingThem() {
        final CountingNumber number = new CountingNumber();
        final Map<String, Object> postData = new LinkedHashMap<>();
        postData.put("text", "original body");
        postData.put("mimeType", "text/plain");
        postData.put("params", List.of(Map.of("name", "n", "value", number)));
        final Map<String, Object> request = Map.of("postData", postData);
        assertEquals(Tuple.of("original body", "text/plain"), HARUtil.getBodyAndMimeTypeByRequestEntry(request));
        assertEquals(0, number.stringCalls);
        postData.remove("text");
        assertEquals("n=7", HARUtil.getBodyAndMimeTypeByRequestEntry(request)._1);
        assertEquals(1, number.stringCalls);

        postData.put("text", "original body");
        postData.put("params", List.of(Map.of("value", "missing name")));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> HARUtil.getBodyAndMimeTypeByRequestEntry(request)).getMessage()
                .contains("request.postData.params[0].name"));
    }

    public static class ScalarBean {
        private Integer value;

        public Integer getValue() {
            return value;
        }

        public void setValue(final Integer value) {
            this.value = value;
        }
    }

    @Test
    public void abacusNestedValueTypesAreApprovedEvenWithoutAncestorTypeInformation() {
        for (final XmlParserType backend : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(backend, null, null, Set.of(NestedBean.class, ScalarBean.class));
            for (final String rootType : List.of("", " type='" + NestedBean.class.getCanonicalName() + "'")) {
                for (final XmlDeserConfig config : List.of(XmlDeserConfig.create(), XmlDeserConfig.create().setValueType("child", ScalarBean.class))) {
                    for (final String attribute : List.of("", " type='   '", " type='" + ScalarBean.class.getCanonicalName() + "'")) {
                        final String xml = "<nestedBean" + rootType + "><child><scalarBean" + attribute + "><value>7</value></scalarBean></child></nestedBean>";
                        assertEquals(7, parser.deserialize(xml, config, NestedBean.class).getChild().getValue(), backend + " " + xml);
                    }
                    for (final String denied : List.of("unapproved.Payload", "List&lt;unapproved.Payload&gt;")) {
                        final String xml = "<nestedBean" + rootType + "><child><scalarBean type='" + denied
                                + "'><value>7</value></scalarBean></child></nestedBean>";
                        assertTrue(assertThrows(ParsingException.class, () -> parser.deserialize(xml, config, NestedBean.class)).getMessage()
                                .contains("not allowed"), backend + " " + xml);
                        // Deliberately ignored properties remain opaque, including their nested type metadata.
                        assertNull(parser.deserialize(xml, XmlDeserConfig.create().setIgnoredPropNames(NestedBean.class, Set.of("child")), NestedBean.class)
                                .getChild());
                    }
                }
            }
        }
    }

    @Test
    public void abacusNullRootsValidateTypeInformationBeforeReturningNull() {
        for (final XmlParserType backend : XmlParserType.values()) {
            final XmlParser parser = new AbacusXmlParserImpl(backend, null, null, Set.of(ScalarBean.class));
            final Map<String, Class<?>> roots = Map.of("scalarBean", ScalarBean.class, "list", List.class, "map", Map.class);
            for (final Map.Entry<String, Class<?>> root : roots.entrySet()) {
                for (final String denied : List.of("unapproved.Payload", "List&lt;unapproved.Payload&gt;")) {
                    final String xml = "<" + root.getKey() + " type='" + denied + "' isNull='true'/>";
                    assertTrue(assertThrows(ParsingException.class, () -> parser.deserialize(xml, root.getValue())).getMessage().contains("not allowed"),
                            backend + " " + xml);
                }
                for (final String attribute : List.of("", " type=' '", " type='" + root.getValue().getCanonicalName() + "'")) {
                    final String xml = "<" + root.getKey() + attribute + " isNull='true'/>";
                    assertNull(parser.deserialize(xml, root.getValue()), backend + " " + xml);
                }
            }
        }
    }

    public static class NestedBean {
        private ScalarBean child;

        public ScalarBean getChild() {
            return child;
        }

        public void setChild(final ScalarBean child) {
            this.child = child;
        }
    }

    public static class NullableBean {
        private List<u.Nullable<Object>> values;

        public List<u.Nullable<Object>> getValues() {
            return values;
        }

        public void setValues(final List<u.Nullable<Object>> values) {
            this.values = values;
        }
    }

    public static class RawBean {
        @com.landawn.abacus.annotation.JsonXmlField(isJsonRawValue = true)
        private Object payload;

        public Object getPayload() {
            return payload;
        }

        public void setPayload(final Object payload) {
            this.payload = payload;
        }
    }

    private static final class CountingNumber extends Number {
        private int stringCalls;

        @Override
        public int intValue() {
            return 7;
        }

        @Override
        public long longValue() {
            return 7L;
        }

        @Override
        public float floatValue() {
            return 7F;
        }

        @Override
        public double doubleValue() {
            return 7D;
        }

        @Override
        public String toString() {
            stringCalls++;
            return "7";
        }
    }
}
