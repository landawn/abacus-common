package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjectTypeTest extends TestBase {

    private ObjectType<Object> objectType;
    private Type<String> stringObjectType;
    private Type<Integer> integerObjectType;

    @BeforeEach
    public void setUp() {
        objectType = (ObjectType<Object>) createType(Object.class);
        stringObjectType = createType(String.class);
        integerObjectType = createType(Integer.class);
    }

    @Test
    public void testDefaultConstructor() {
        ObjectType<Object> defaultType = (ObjectType<Object>) createType("Object");
        assertNotNull(defaultType);
        assertEquals(Object.class, defaultType.javaType());
    }

    @Test
    public void testConstructorWithClass() {
        assertEquals(String.class, stringObjectType.javaType());
        assertEquals(Integer.class, integerObjectType.javaType());
    }

    @Test
    public void testConstructorWithTypeNameAndClass() {
        ObjectType<String> customType = (ObjectType<String>) createType("CustomString");
        assertNotNull(customType);
        assertEquals("CustomString", customType.name());
    }

    @Test
    public void testClazz() {
        assertEquals(Object.class, objectType.javaType());
        assertEquals(String.class, stringObjectType.javaType());
        assertEquals(Integer.class, integerObjectType.javaType());
    }

    @Test
    public void testName() {
        assertNotNull(objectType.name());
        assertNotNull(stringObjectType.name());
        assertNotNull(integerObjectType.name());
    }

    @Test
    public void testIsGenericType() {
        assertFalse(objectType.isParameterizedType());
        assertFalse(stringObjectType.isParameterizedType());
        assertFalse(integerObjectType.isParameterizedType());
    }

    @Test
    public void testIsPrimitive() {
        assertFalse(objectType.isPrimitive());
        assertFalse(stringObjectType.isPrimitive());
        assertFalse(integerObjectType.isPrimitive());
    }

    @Test
    public void testIsObject() {
        assertTrue(objectType.isObject());
        assertFalse(stringObjectType.isObject());
        assertFalse(integerObjectType.isObject());
    }

    @Test
    public void testStringOf() {
        assertEquals("test", stringObjectType.stringOf("test"));
        assertEquals("123", integerObjectType.stringOf(123));
        assertNull(objectType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        assertEquals("test", stringObjectType.valueOf("test"));
        assertEquals(123, integerObjectType.valueOf("123"));
        assertNull(objectType.valueOf(null));
    }

    @Test
    public void testIsSerializable() {
        assertTrue(stringObjectType.isSerializable());
        assertTrue(integerObjectType.isSerializable());
    }

    @Test
    public void testEquals() {
        String str1 = "test";
        String str2 = "test";
        String str3 = "different";

        assertTrue(stringObjectType.equals(str1, str2));
        assertFalse(stringObjectType.equals(str1, str3));
        assertTrue(stringObjectType.equals(null, null));
        assertFalse(stringObjectType.equals(str1, null));
        assertFalse(stringObjectType.equals(null, str1));
    }

    @Test
    public void testHashCode() {
        String str = "test";
        Integer num = 123;

        assertEquals(str.hashCode(), stringObjectType.hashCode(str));
        assertEquals(num.hashCode(), integerObjectType.hashCode(num));
        assertEquals(0, objectType.hashCode(null));
    }

    @Test
    public void testToString() {
        assertEquals("test", stringObjectType.toString("test"));
        assertEquals("123", integerObjectType.toString(123));
        assertEquals("null", objectType.toString(null));
    }

    // ---- review fixes 2026-09-06, T2-01: object-mode serializeTo dispatches on the runtime type ----

    public static class HolderBean {
        private com.landawn.abacus.util.Holder<Object> h;
        private com.landawn.abacus.util.Tuple.Tuple2<Object, Object> t;
        private com.landawn.abacus.util.u.Optional<Object> o;

        public com.landawn.abacus.util.Holder<Object> getH() {
            return h;
        }

        public void setH(final com.landawn.abacus.util.Holder<Object> h) {
            this.h = h;
        }

        public com.landawn.abacus.util.Tuple.Tuple2<Object, Object> getT() {
            return t;
        }

        public void setT(final com.landawn.abacus.util.Tuple.Tuple2<Object, Object> t) {
            this.t = t;
        }

        public com.landawn.abacus.util.u.Optional<Object> getO() {
            return o;
        }

        public void setO(final com.landawn.abacus.util.u.Optional<Object> o) {
            this.o = o;
        }
    }

    private static String serializeJson(final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config) throws java.io.IOException {
        final java.io.StringWriter output = new java.io.StringWriter();
        final com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter(output);

        try {
            Type.<Object> of(Object.class).serializeTo(writer, value, config);
            writer.flush();
            return output.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }

    private static String serializeXml(final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config) throws java.io.IOException {
        final java.io.StringWriter output = new java.io.StringWriter();
        final com.landawn.abacus.util.BufferedXmlWriter writer = com.landawn.abacus.util.Objectory.createBufferedXmlWriter(output);

        try {
            Type.<Object> of(Object.class).serializeTo(writer, value, config);
            writer.flush();
            return output.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }

    @Test
    public void reviewFixes20260906_serializeToScalarsKeepTheirJsonShape() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig config = com.landawn.abacus.parser.JsonSerConfig.create();

        // Before the fix every one of these was written as a quoted JSON string ("5", "true", ...).
        assertEquals("5", serializeJson(5, config));
        assertEquals("true", serializeJson(true, config));
        assertEquals("2.5", serializeJson(2.5, config));
        assertEquals("1.10", serializeJson(new java.math.BigDecimal("1.10"), config));
        assertEquals("[1, 2]", serializeJson(new int[] { 1, 2 }, config));
        // A String is still a quoted, escaped JSON string.
        assertEquals("\"a\\\"b\"", serializeJson("a\"b", config));
        // config == null: no quotation, as before.
        assertEquals("5", serializeJson(5, null));
        assertEquals("s", serializeJson("s", null));
    }

    @Test
    public void reviewFixes20260906_serializeToHonoursTheConfigOfTheRuntimeType() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig config = com.landawn.abacus.parser.JsonSerConfig.create()
                .setDateTimeFormat(com.landawn.abacus.util.DateTimeFormat.LONG);

        // The Date slot honours the LONG format exactly like a Date-typed slot; before the fix it wrote the ISO text quoted.
        assertEquals("0", serializeJson(new java.util.Date(0), config));
    }

    @Test
    public void reviewFixes20260906_serializeToWritesLiteralNullForEmptyWrappers() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig config = com.landawn.abacus.parser.JsonSerConfig.create();

        // stringOf() of these is null; before the fix the STRING "null" (quoted) was written.
        assertEquals("null", serializeJson(com.landawn.abacus.util.Holder.of(null), config));
        assertEquals("null", serializeJson(com.landawn.abacus.util.u.Optional.empty(), config));
        assertEquals("null", serializeJson(null, config));
    }

    @Test
    public void reviewFixes20260906_serializeToPlainObjectTypeStaysQuotedToString() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig config = com.landawn.abacus.parser.JsonSerConfig.create();
        final Object plain = new Object();

        // An object-mode runtime type takes the toString() branch (the recursion guard), quoted and escaped.
        assertEquals("\"" + plain + "\"", serializeJson(plain, config));
        // A value-mode ObjectType (auto-detected wrapper) is also an ObjectType and keeps the toString() path, like stringOf().
        assertEquals("\"true\"", serializeJson(new org.apache.commons.lang3.mutable.MutableBoolean(true), config));
        assertEquals("\"fr_FR\"", serializeJson(java.util.Locale.FRANCE, config));
    }

    @Test
    public void reviewFixes20260906_serializeToNonSerializableRuntimeTypeIsStructuralJsonOnlyUnderJsonConfig() throws java.io.IOException {
        final java.util.Map<String, Integer> map = com.landawn.abacus.util.N.asMap("a", 1);

        // JsonSerConfig: the JSON parser writes the structural form.
        assertEquals("{\"a\": 1}", serializeJson(map, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("[5, \"x\"]", serializeJson(com.landawn.abacus.util.N.asList(5, "x"), com.landawn.abacus.parser.JsonSerConfig.create()));
        // XmlSerConfig: the text form, escaped for the XML writer.
        assertEquals("{&quot;a&quot;: 1}", serializeXml(map, com.landawn.abacus.parser.XmlSerConfig.create()));
    }

    @Test
    public void reviewFixes20260906_objectSlotsRoundTripWithTheirRuntimeTypes() {
        final HolderBean bean = new HolderBean();
        bean.setH(com.landawn.abacus.util.Holder.of(5));
        bean.setT(com.landawn.abacus.util.Tuple.of(5, true));
        bean.setO(com.landawn.abacus.util.u.Optional.of(6));

        final String json = com.landawn.abacus.util.N.toJson(bean);

        // Before the fix: {"h": "5", "t": ["5", "true"], "o": "6"} and everything came back as String.
        assertEquals("{\"h\": 5, \"t\": [5, true], \"o\": 6}", json);

        final HolderBean back = com.landawn.abacus.util.N.fromJson(json, HolderBean.class);

        assertEquals(Integer.valueOf(5), back.getH().value());
        assertEquals(Integer.valueOf(5), back.getT()._1);
        assertEquals(Boolean.TRUE, back.getT()._2);
        assertEquals(Integer.valueOf(6), back.getO().get());
    }

    @Test
    public void reviewFixes20260906_mapPayloadInObjectSlotIsStructuralJsonAndTextInXml() {
        final HolderBean bean = new HolderBean();
        bean.setH(com.landawn.abacus.util.Holder.of(com.landawn.abacus.util.N.asMap("k", 9)));

        // Before the fix: {"h": "{\"k\": 9}"} (an escaped string).
        assertEquals("{\"h\": {\"k\": 9}}", com.landawn.abacus.util.N.toJson(bean));
        assertEquals("<holderBean><h>{&quot;k&quot;: 9}</h></holderBean>", com.landawn.abacus.util.N.toXml(bean));

        final HolderBean nullHolder = new HolderBean();
        nullHolder.setH(com.landawn.abacus.util.Holder.of(null));
        assertEquals("{\"h\": null}", com.landawn.abacus.util.N.toJson(nullHolder));
    }

    @Test
    public void reviewFixes20260907_jdbcLocatorKeepsItsOwnUnsupportedOperationException() throws Exception {
        final java.sql.Blob blob = new javax.sql.rowset.serial.SerialBlob(new byte[] { 1, 2 });

        // R12: a handler with no serialization category (UNKNOWN - the JDBC locators) has no JSON shape, so it keeps
        // its own descriptive exception, as AbstractTupleType.serializeSlot does and as this branch did before T2-01;
        // routing it through the JSON parser produced "Unsupported class: ...SerialBlob" instead.
        final UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> serializeJson(blob, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertTrue(e.getMessage().contains("Blob"), e.getMessage());
    }

    // FINDING R05-4 (2026-09-08): ObjectArrayType, CollectionType and AbstractTupleType all switch prettyFormat off
    // for the embedded JSON write because they are not told the caller's indentation; SingleValueType/ObjectType
    // still passed the caller's config through and emitted a block that restarts at the left margin.
    @Test
    public void reviewFixes20260908_embeddedJsonIsAlwaysWrittenCompactly() throws java.io.IOException {
        final com.landawn.abacus.parser.JsonSerConfig pretty = com.landawn.abacus.parser.JsonSerConfig.create().setPrettyFormat(true);
        final com.landawn.abacus.parser.JsonSerConfig compact = com.landawn.abacus.parser.JsonSerConfig.create();
        final Object map = com.landawn.abacus.util.N.asMap("k", 1, "s", "v");

        assertEquals(serializeJson(map, compact), serializeJson(map, pretty));
        assertEquals("{\"k\": 1, \"s\": \"v\"}", serializeJson(map, pretty));

        // The caller's own config is not mutated by the compaction.
        assertTrue(pretty.isPrettyFormat());

        // ... and the sibling handlers that were fixed first still agree.
        assertEquals("[{\"k\": 1, \"s\": \"v\"}]", serialize(createType(Object[].class), new Object[] { map }, pretty));
        assertEquals("[{\"k\": 1, \"s\": \"v\"}]", serialize(createType("List<Object>"), com.landawn.abacus.util.N.asList(map), pretty));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String serialize(final Type type, final Object value, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config)
            throws java.io.IOException {
        final java.io.StringWriter output = new java.io.StringWriter();
        final com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter(output);

        try {
            type.serializeTo(writer, value, config);
            writer.flush();
            return output.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }
}
