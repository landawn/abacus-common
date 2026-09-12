package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.alibaba.fastjson2.JSONException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-02 review of {@link JsonMappers}, {@link XmlMappers}, {@link FastJson} and
 * {@link JsonUtil}. Each test names the finding it locks down.
 */
public class JsonXmlMappersTest extends TestBase {

    @TempDir
    File tempDir;

    public static class Bean {
        private String name;
        private int age;

        public Bean() {
        }

        public Bean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }

    /** An ObjectMapper subclass that does not override copy() - the shape that used to break wrap(). */
    public static class CustomJsonMapper extends ObjectMapper {
        private static final long serialVersionUID = 1L;
    }

    /** An XmlMapper subclass that does not override copy(). */
    public static class CustomXmlMapper extends XmlMapper {
        private static final long serialVersionUID = 1L;
    }

    private static Map<?, ?> pool(final Class<?> owner, final String field) throws Exception {
        final Field f = owner.getDeclaredField(field);
        f.setAccessible(true);
        return (Map<?, ?>) f.get(null);
    }

    // ------------------------------------------------------------------------------------------------
    // B2 / J4 - wrap() must accept mapper subclasses (ObjectMapper.copy() rejects them)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testWrap_AcceptsObjectMapperSubclassThatDoesNotOverrideCopy() {
        final JsonMappers.One one = JsonMappers.wrap(new CustomJsonMapper());
        Assertions.assertEquals("{\"name\":\"a\",\"age\":1}", one.toJson(new Bean("a", 1)));
        Assertions.assertTrue(one.toJson(new Bean("a", 1), true).contains("\n"));
    }

    @Test
    public void testWrap_AcceptsAnonymousObjectMapperSubclass() {
        final JsonMappers.One one = JsonMappers.wrap(new ObjectMapper() {
            private static final long serialVersionUID = 1L;
        });
        Assertions.assertEquals("{\"name\":\"a\",\"age\":1}", one.toJson(new Bean("a", 1)));
    }

    @Test
    public void testWrap_AcceptsXmlMapperSubclassThatDoesNotOverrideCopy() {
        final XmlMappers.One one = XmlMappers.wrap(new CustomXmlMapper());
        Assertions.assertEquals("<Bean><name>a</name><age>1</age></Bean>", one.toXml(new Bean("a", 1)));
        Assertions.assertTrue(one.toXml(new Bean("a", 1), true).contains("\n"));
    }

    @Test
    public void testToJson_PrettyOutputUnchangedByObjectWriterSwitch() throws Exception {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "John");
        m.put("age", 30);
        // the ObjectWriter-based pretty path must produce exactly what the copied INDENT_OUTPUT mapper produced
        Assertions.assertEquals(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(m).replace("\r\n", "\n"),
                JsonMappers.toJson(m, true).replace("\r\n", "\n"));
        Assertions.assertEquals(JsonMappers.toJson(m, true), JsonMappers.wrap(new ObjectMapper()).toJson(m, true));
        Assertions.assertEquals(XmlMappers.toXml(m, true), XmlMappers.wrap(new XmlMapper()).toXml(m, true));
        Assertions.assertTrue(JsonMappers.toJson(m, true).contains("\n"));
        Assertions.assertTrue(XmlMappers.toXml(m, true).contains("\n"));
    }

    /**
     * The pretty path used to be a mapper with {@link SerializationFeature#INDENT_OUTPUT} enabled, so a custom
     * serializer could observe the feature through {@link SerializerProvider#isEnabled(SerializationFeature)}.
     * Replacing it with an {@code ObjectWriter} must not silently drop that: {@code writerWithDefaultPrettyPrinter()}
     * installs a pretty printer <i>without</i> enabling the feature, which is why {@code writer(INDENT_OUTPUT)}
     * is used instead.
     */
    @Test
    public void testToJson_PrettyPathEnablesIndentOutputForCustomSerializers() {
        final SimpleModule probe = new SimpleModule();
        probe.addSerializer(Bean.class, new StdSerializer<>(Bean.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void serialize(final Bean v, final JsonGenerator g, final SerializerProvider p) throws IOException {
                g.writeStartObject();
                g.writeStringField("indent", String.valueOf(p.isEnabled(SerializationFeature.INDENT_OUTPUT)));
                g.writeEndObject();
            }
        });
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(probe);
        final JsonMappers.One one = JsonMappers.wrap(om);

        Assertions.assertTrue(one.toJson(new Bean("a", 1), true).contains("\"indent\" : \"true\""),
                "the pretty writer must enable INDENT_OUTPUT, not merely install a pretty printer");
        Assertions.assertTrue(one.toJson(new Bean("a", 1), false).contains("\"indent\":\"false\""));
        // the class-owned static pretty path must behave the same way
        Assertions.assertTrue(JsonMappers.toJson(new Bean("a", 1), SerializationFeature.INDENT_OUTPUT).contains("\n"));
    }

    /** A custom pretty printer installed on the wrapped mapper must still be the one used. */
    @Test
    public void testToJson_PrettyPathHonoursCallerSuppliedDefaultPrettyPrinter() {
        final ObjectMapper om = new ObjectMapper();
        final DefaultPrettyPrinter dpp = new DefaultPrettyPrinter();
        dpp.indentObjectsWith(new DefaultPrettyPrinter.FixedSpaceIndenter());
        om.setDefaultPrettyPrinter(dpp);
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        Assertions.assertEquals("{ \"a\" : 1, \"b\" : 2 }", JsonMappers.wrap(om).toJson(m, true));
    }

    // ------------------------------------------------------------------------------------------------
    // J3 - prettyFormat==false cannot switch OFF a mapper that already indents
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testToJson_PrettyFormatFalseDoesNotForceCompactOnIndentingMapper() {
        final ObjectMapper indenting = new ObjectMapper();
        indenting.enable(SerializationFeature.INDENT_OUTPUT);
        final JsonMappers.One one = JsonMappers.wrap(indenting);
        Assertions.assertTrue(one.toJson(new Bean("a", 1), false).contains("\n"), "documented: false serializes with the mapper as configured");
        Assertions.assertTrue(one.toJson(new Bean("a", 1), true).contains("\n"));

        final XmlMapper indentingXml = new XmlMapper();
        indentingXml.enable(SerializationFeature.INDENT_OUTPUT);
        Assertions.assertTrue(XmlMappers.wrap(indentingXml).toXml(new Bean("a", 1), false).contains("\n"));
    }

    @Test
    public void testToJson_ClassOwnedDefaultsAreCompactWhenPrettyFormatFalse() {
        Assertions.assertFalse(JsonMappers.toJson(new Bean("a", 1), false).contains("\n"));
        Assertions.assertFalse(XmlMappers.toXml(new Bean("a", 1), false).contains("\n"));
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - the feature overloads must not go through the identity-keyed mapper cache
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testToJson_SerializationFeatureOverloadDoesNotGrowMapperPool() throws Exception {
        final Map<?, ?> p = pool(JsonMappers.class, "serializationMapperPool");
        final int before;
        synchronized (p) {
            before = p.size();
        }
        for (int i = 0; i < 25; i++) {
            JsonMappers.toJson(new Bean("a", 1), SerializationFeature.INDENT_OUTPUT);
        }
        synchronized (p) {
            Assertions.assertEquals(before, p.size(), "feature overloads must use an ObjectWriter, not the config cache");
        }
    }

    @Test
    public void testFromJson_DeserializationFeatureOverloadDoesNotGrowMapperPool() throws Exception {
        final Map<?, ?> p = pool(JsonMappers.class, "deserializationMapperPool");
        final int before;
        synchronized (p) {
            before = p.size();
        }
        // ACCEPT_SINGLE_VALUE_AS_ARRAY is OFF by default, so config.with(..) really does produce a new
        // config instance. A feature that is already ON returns the same instance and would hit the cache
        // even without the fix, which would make this test prove nothing.
        for (int i = 0; i < 25; i++) {
            JsonMappers.fromJson("{\"name\":\"a\",\"age\":1}", Bean.class, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            JsonMappers.fromJson("[{\"name\":\"a\",\"age\":1}]", new TypeReference<List<Bean>>() {
            }, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        }
        synchronized (p) {
            Assertions.assertEquals(before, p.size());
        }
    }

    @Test
    public void testToXml_FeatureOverloadsDoNotGrowMapperPools() throws Exception {
        final Map<?, ?> sp = pool(XmlMappers.class, "serializationMapperPool");
        final Map<?, ?> dp = pool(XmlMappers.class, "deserializationMapperPool");
        final int sBefore;
        final int dBefore;
        synchronized (sp) {
            sBefore = sp.size();
        }
        synchronized (dp) {
            dBefore = dp.size();
        }
        for (int i = 0; i < 25; i++) {
            XmlMappers.toXml(new Bean("a", 1), SerializationFeature.INDENT_OUTPUT);
            XmlMappers.fromXml("<Bean><name>a</name><age>1</age></Bean>", Bean.class, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
            XmlMappers.fromXml("<Bean><name>a</name><age>1</age></Bean>", new TypeReference<Bean>() {
            }, DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        }
        synchronized (sp) {
            Assertions.assertEquals(sBefore, sp.size());
        }
        synchronized (dp) {
            Assertions.assertEquals(dBefore, dp.size());
        }
    }

    @Test
    public void testToJson_FeatureOverloadsProduceConfiguredBehaviour() {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "John");
        Assertions.assertTrue(JsonMappers.toJson(m, SerializationFeature.INDENT_OUTPUT).contains("\n"));
        Assertions.assertTrue(XmlMappers.toXml(m, SerializationFeature.INDENT_OUTPUT).contains("\n"));
        Assertions.assertEquals(JsonMappers.toJson(m, true), JsonMappers.toJson(m, SerializationFeature.INDENT_OUTPUT));
        Assertions.assertEquals(XmlMappers.toXml(m, true), XmlMappers.toXml(m, SerializationFeature.INDENT_OUTPUT));

        // a feature that actually changes deserialization behaviour still takes effect
        assertThrows(RuntimeException.class, () -> JsonMappers.fromJson("{\"nope\":1}", Bean.class, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        final Bean ok = JsonMappers.fromJson("{\"name\":\"a\"}", Bean.class, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Assertions.assertEquals("a", ok.getName());

        assertThrows(RuntimeException.class,
                () -> XmlMappers.fromXml("<Bean><nope>1</nope></Bean>", Bean.class, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    public void testFromJson_FeatureOverloadsAcceptTypeReferenceTargets() {
        final List<Bean> beans = JsonMappers.fromJson("[{\"name\":\"a\",\"age\":1}]", new TypeReference<List<Bean>>() {
        }, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Assertions.assertEquals(1, beans.size());
        Assertions.assertEquals("a", beans.get(0).getName());

        final Bean b = XmlMappers.fromXml("<Bean><name>a</name><age>1</age></Bean>", new TypeReference<Bean>() {
        }, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Assertions.assertEquals("a", b.getName());
    }

    @Test
    public void testToJson_MultipleFeaturesAreAllApplied() {
        final String json = JsonMappers.toJson(new Bean("a", 1), SerializationFeature.INDENT_OUTPUT, SerializationFeature.WRAP_ROOT_VALUE);
        Assertions.assertTrue(json.contains("\n"));
        Assertions.assertTrue(json.contains("Bean"));
    }

    @Test
    public void testToJson_ConfigOverloadsCacheReusedConfigInstance() throws Exception {
        final Map<?, ?> p = pool(JsonMappers.class, "serializationMapperPool");
        synchronized (p) {
            p.clear();
        }
        final SerializationConfig held = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
        for (int i = 0; i < 10; i++) {
            JsonMappers.toJson(new Bean("a", 1), held);
        }
        synchronized (p) {
            Assertions.assertEquals(1, p.size(), "a reused config instance must hit the cache");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 - XmlMappers must validate its arguments the way JsonMappers does
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testFromXml_ByteRangeIsValidatedLikeJsonMappers() {
        final byte[] xml = "<Bean><name>a</name><age>1</age></Bean>".getBytes(StandardCharsets.UTF_8);
        final byte[] json = "{\"name\":\"a\"}".getBytes(StandardCharsets.UTF_8);

        assertThrows(IndexOutOfBoundsException.class, () -> XmlMappers.fromXml(xml, 0, 999, Bean.class));
        assertThrows(IndexOutOfBoundsException.class, () -> JsonMappers.fromJson(json, 0, 999, Bean.class));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlMappers.fromXml(xml, -1, 2, Bean.class));
        assertThrows(IndexOutOfBoundsException.class, () -> JsonMappers.fromJson(json, -1, 2, Bean.class));
        // N.checkFromIndexSize's documented contract: negative size is an IAE, a bad position is an IOOBE
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.fromXml(xml, 0, -1, Bean.class));
        assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(json, 0, -1, Bean.class));
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.fromXml((byte[]) null, 0, 1, Bean.class));

        final TypeReference<Bean> tr = new TypeReference<>() {
        };
        assertThrows(IndexOutOfBoundsException.class, () -> XmlMappers.fromXml(xml, 0, 999, tr));
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.fromXml(xml, 0, -1, tr));

        final XmlMappers.One one = XmlMappers.wrap(new XmlMapper());
        assertThrows(IndexOutOfBoundsException.class, () -> one.fromXml(xml, 0, 999, Bean.class));
        assertThrows(IndexOutOfBoundsException.class, () -> one.fromXml(xml, 0, 999, tr));
    }

    @Test
    public void testFromXml_ByteRangeWorksForValidRanges() {
        final byte[] buf = "##<Bean><name>a</name><age>1</age></Bean>".getBytes(StandardCharsets.UTF_8);
        Assertions.assertEquals("a", XmlMappers.fromXml(buf, 2, buf.length - 2, Bean.class).getName());
        Assertions.assertEquals("a", XmlMappers.wrap(new XmlMapper()).fromXml(buf, 2, buf.length - 2, Bean.class).getName());
        final TypeReference<Bean> tr = new TypeReference<>() {
        };
        Assertions.assertEquals("a", XmlMappers.<Bean> fromXml(buf, 2, buf.length - 2, tr).getName());
    }

    @Test
    public void testToXml_NullFeatureArgumentsRejectedWithIae() {
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.toXml(new Bean("a", 1), (SerializationFeature) null));
        assertThrows(IllegalArgumentException.class, () -> JsonMappers.toJson(new Bean("a", 1), (SerializationFeature) null));
        assertThrows(IllegalArgumentException.class,
                () -> XmlMappers.toXml(new Bean("a", 1), SerializationFeature.INDENT_OUTPUT, (SerializationFeature[]) null));

        assertThrows(IllegalArgumentException.class, () -> XmlMappers.fromXml("<Bean/>", Bean.class, (DeserializationFeature) null));
        assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson("{}", Bean.class, (DeserializationFeature) null));
        assertThrows(IllegalArgumentException.class,
                () -> XmlMappers.fromXml("<Bean/>", (Class<Bean>) null, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson("{}", (Class<Bean>) null, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        final TypeReference<Bean> tr = new TypeReference<>() {
        };
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.fromXml("<Bean/>", tr, (DeserializationFeature) null));
        assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson("{}", tr, (DeserializationFeature) null));
    }

    @Test
    public void testWrap_NullGivesNamedArgumentException() {
        final IllegalArgumentException x = assertThrows(IllegalArgumentException.class, () -> XmlMappers.wrap(null));
        final IllegalArgumentException j = assertThrows(IllegalArgumentException.class, () -> JsonMappers.wrap(null));
        Assertions.assertEquals("'xmlMapper' cannot be null", x.getMessage());
        Assertions.assertEquals("'jsonMapper' cannot be null", j.getMessage());
    }

    // ------------------------------------------------------------------------------------------------
    // URL family - reimplemented over URL.openStream(); no deprecated Jackson API, same results
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testFromJson_UrlReadsWorkForClassAndTypeReference() throws Exception {
        final File jsonFile = new File(tempDir, "b.json");
        Files.write(jsonFile.toPath(), "{\"name\":\"a\",\"age\":1}".getBytes(StandardCharsets.UTF_8));
        final URL jsonUrl = jsonFile.toURI().toURL();
        final TypeReference<Bean> jtr = new TypeReference<>() {
        };

        Assertions.assertEquals("a", JsonMappers.fromJson(jsonUrl, Bean.class).getName());
        Assertions.assertEquals("a", JsonMappers.fromJson(jsonUrl, Bean.class, JsonMappers.createDeserializationConfig()).getName());
        Assertions.assertEquals("a", JsonMappers.<Bean> fromJson(jsonUrl, jtr).getName());
        Assertions.assertEquals("a", JsonMappers.<Bean> fromJson(jsonUrl, jtr, JsonMappers.createDeserializationConfig()).getName());
        Assertions.assertEquals("a", JsonMappers.wrap(new ObjectMapper()).fromJson(jsonUrl, Bean.class).getName());
        Assertions.assertEquals("a", JsonMappers.wrap(new ObjectMapper()).<Bean> fromJson(jsonUrl, jtr).getName());

        final File xmlFile = new File(tempDir, "b.xml");
        Files.write(xmlFile.toPath(), "<Bean><name>a</name><age>1</age></Bean>".getBytes(StandardCharsets.UTF_8));
        final URL xmlUrl = xmlFile.toURI().toURL();
        final TypeReference<Bean> xtr = new TypeReference<>() {
        };

        Assertions.assertEquals("a", XmlMappers.fromXml(xmlUrl, Bean.class).getName());
        Assertions.assertEquals("a", XmlMappers.fromXml(xmlUrl, Bean.class, XmlMappers.createDeserializationConfig()).getName());
        Assertions.assertEquals("a", XmlMappers.<Bean> fromXml(xmlUrl, xtr).getName());
        Assertions.assertEquals("a", XmlMappers.<Bean> fromXml(xmlUrl, xtr, XmlMappers.createDeserializationConfig()).getName());
        Assertions.assertEquals("a", XmlMappers.wrap(new XmlMapper()).fromXml(xmlUrl, Bean.class).getName());
        Assertions.assertEquals("a", XmlMappers.wrap(new XmlMapper()).<Bean> fromXml(xmlUrl, xtr).getName());
    }

    @Test
    public void testFromJson_UrlNullArgumentsRejected() {
        assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson((URL) null, Bean.class));
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.fromXml((URL) null, Bean.class));
        assertThrows(IllegalArgumentException.class, () -> JsonMappers.wrap(new ObjectMapper()).fromJson((URL) null, Bean.class));
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.wrap(new XmlMapper()).fromXml((URL) null, Bean.class));
    }

    @Test
    public void testFromJson_MissingUrlSurfacesAsRuntimeException() throws Exception {
        final URL missing = new File(tempDir, "does-not-exist.json").toURI().toURL();
        assertThrows(RuntimeException.class, () -> JsonMappers.fromJson(missing, Bean.class));
    }

    // ------------------------------------------------------------------------------------------------
    // XML hardening must survive the pooling and pretty-printing changes
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testFromXml_HardeningSurvivesEveryDeserializationEntryPoint() {
        final String hostile = "<!DOCTYPE Bean [<!ENTITY xxe SYSTEM \"file:///definitely-not-readable\">]><Bean><name>&xxe;</name></Bean>";
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml(hostile, Bean.class));
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml(hostile, Bean.class, XmlMappers.createDeserializationConfig()));
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml(hostile, Bean.class, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    public void testToXml_PrettyPathNoLongerOwnsASecondMapper() {
        assertThrows(NoSuchFieldException.class, () -> XmlMappers.class.getDeclaredField("defaultXmlMapperForPretty"));
        assertThrows(NoSuchFieldException.class, () -> JsonMappers.class.getDeclaredField("defaultJsonMapperForPretty"));
    }

    // ------------------------------------------------------------------------------------------------
    // B7 - unwrap(JSONArray) returns a checked List<Object> instead of an unchecked <T> sink
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testUnwrap_JsonArrayReturnsListOfObject() {
        final List<Object> list = JsonUtil.unwrap(new JSONArray("[\"text\",123,true,null]"));
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals("text", list.get(0));
        Assertions.assertEquals(123, list.get(1));
        Assertions.assertEquals(Boolean.TRUE, list.get(2));
        Assertions.assertNull(list.get(3));
        // the previous <T> sink allowed List<String> here and threw ClassCastException on first read;
        // the element type now has to be requested explicitly through the checked API
        Assertions.assertEquals(Arrays.asList("a", "b"), JsonUtil.toList(new JSONArray("[\"a\",\"b\"]"), String.class));
    }

    @Test
    public void testUnwrap_JsonArrayIsNoLongerUncheckedGenericSink() throws Exception {
        // The defect was purely at compile time - <T> was inferred from the assignment target and never
        // checked - so it can only be pinned down through the declared signature.
        Assertions.assertEquals("java.util.List<java.lang.Object>", JsonUtil.class.getMethod("unwrap", JSONArray.class).getGenericReturnType().getTypeName());
    }

    @Test
    public void testToList_GivesGenuinelyTypedList() {
        final List<String> typed = JsonUtil.toList(new JSONArray("[\"a\",\"b\"]"), String.class);
        for (final String s : typed) {
            Assertions.assertNotNull(s);
        }
        final List<Bean> beans = JsonUtil.toList(new JSONArray("[{\"name\":\"a\",\"age\":1}]"), Bean.class);
        Assertions.assertEquals("a", beans.get(0).getName());
    }

    // ------------------------------------------------------------------------------------------------
    // J6 / B6 - JsonUtil.wrap contracts
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testWrap_RejectsNonBeanArguments() {
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap("abc"));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap(42));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((Object) new int[] { 1, 2 }));
        // the documented trap: a statically-Object collection binds to wrap(Object), not wrap(Collection)
        final Object staticallyObject = new ArrayList<>(List.of(1, 2));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap(staticallyObject));
        // ... and the cast the javadoc prescribes fixes it
        Assertions.assertEquals(2, JsonUtil.wrap((Collection<?>) staticallyObject).length());
    }

    @Test
    public void testWrap_NullPolicyIsAsDocumented() {
        Assertions.assertEquals(0, JsonUtil.wrap((Map<String, Object>) null).length());
        Assertions.assertEquals(0, JsonUtil.wrap((Object) null).length());
        Assertions.assertEquals(0, JsonUtil.wrap((Collection<?>) null).length());

        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((boolean[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((char[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((short[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((long[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((float[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((double[]) null));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((Object[]) null));
    }

    @Test
    public void testUnwrap_KeyOrderIsUndefinedButOrderedMapCanBeRequested() {
        final JSONObject jo = new JSONObject("{\"z\":1,\"y\":2,\"x\":3}");
        Assertions.assertEquals(3, JsonUtil.unwrap(jo).size());
        final Map<String, Object> sorted = JsonUtil.unwrap(jo, TreeMap.class);
        Assertions.assertEquals(Arrays.asList("x", "y", "z"), new ArrayList<>(sorted.keySet()));
    }

    // ------------------------------------------------------------------------------------------------
    // B5 / D10 - documented FastJson vs JsonMappers conventions
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testToJson_EmptyInputConventionsAreAsDocumented() {
        Assertions.assertNull(FastJson.fromJson("", Bean.class));
        Assertions.assertNull(FastJson.fromJson(new byte[0], Bean.class));
        Assertions.assertNull(FastJson.fromJson("{}".getBytes(StandardCharsets.UTF_8), 0, 0, Bean.class));
        Assertions.assertNull(FastJson.fromJson(new StringReader(""), Bean.class));
        Assertions.assertNull(FastJson.fromJson("null", Bean.class));
        // whitespace-only is not empty
        assertThrows(JSONException.class, () -> FastJson.fromJson("   ", Bean.class));
        // JsonMappers deliberately does not share the convention - and blank input behaves like empty there
        assertThrows(RuntimeException.class, () -> JsonMappers.fromJson("", Bean.class));
        assertThrows(RuntimeException.class, () -> JsonMappers.fromJson("   ", Bean.class));
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml("", Bean.class));
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml("   ", Bean.class));
    }

    /** FastJson silently ignores unknown properties; the Jackson-backed classes reject them. */
    @Test
    public void testFromJson_UnknownPropertyConventionsDivergeAsDocumented() {
        Assertions.assertNotNull(FastJson.fromJson("{\"unknown\":1}", Bean.class));
        assertThrows(RuntimeException.class, () -> JsonMappers.fromJson("{\"unknown\":1}", Bean.class));
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml("<Bean><unknown>1</unknown></Bean>", Bean.class));
    }

    @Test
    public void testToJson_FileOutputParentDirectoryConventions() {
        final File nested = new File(new File(tempDir, "created-by-fastjson"), "out.json");
        Assertions.assertFalse(nested.getParentFile().exists());
        FastJson.toJson(new Bean("a", 1), nested);
        Assertions.assertTrue(nested.exists());

        final File nested2 = new File(new File(tempDir, "not-created-by-jsonmappers"), "out.json");
        assertThrows(RuntimeException.class, () -> JsonMappers.toJson(new Bean("a", 1), nested2));
        Assertions.assertFalse(nested2.exists());
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 2 - C-022 / C-023 / C-024: charset, byte-input and failed-write conventions
    // ------------------------------------------------------------------------------------------------

    /** A non-BMP character (surrogate pair), a combining mark, an RTL letter and a CJK ideograph. */
    private static final String UNICODE = "A😀B́CאD中E";

    /**
     * C-022: Jackson's UTF-8 generator escapes non-BMP characters as {@code &#92;uXXXX} surrogate pairs, while
     * the String/Writer path emits raw UTF-8 - the same object yields different bytes across overloads of one
     * method.
     */
    @Test
    public void testToJson_ByteOutputEscapesNonBmpWhileCharOutputDoesNot() throws Exception {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", UNICODE);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JsonMappers.toJson(m, bos);
        final String viaBytes = bos.toString(StandardCharsets.UTF_8);
        final String viaString = JsonMappers.toJson(m);

        Assertions.assertTrue(viaBytes.contains("\\uD83D\\uDE00"), "byte path should escape the astral char: " + viaBytes);
        Assertions.assertFalse(viaBytes.contains("😀"));
        Assertions.assertTrue(viaString.contains("😀"), "String path should emit it raw");
        Assertions.assertNotEquals(viaString, viaBytes, "the documented divergence between the two paths");

        // BMP non-ASCII is raw on both paths
        Assertions.assertTrue(viaBytes.contains("中"));
        Assertions.assertTrue(viaString.contains("中"));

        // ... and both still round-trip to the identical value
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(viaBytes, Map.class).get("k"));
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(viaString, Map.class).get("k"));
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(bos.toByteArray(), Map.class).get("k"));

        // FastJson and XmlMappers emit raw UTF-8 on the byte path
        final ByteArrayOutputStream fj = new ByteArrayOutputStream();
        FastJson.toJson(m, fj);
        Assertions.assertTrue(fj.toString(StandardCharsets.UTF_8).contains("😀"));
        final ByteArrayOutputStream xm = new ByteArrayOutputStream();
        XmlMappers.toXml(m, xm);
        Assertions.assertTrue(xm.toString(StandardCharsets.UTF_8).contains("😀"));
    }

    /** Unicode survives every read/write path in all four classes. */
    @Test
    public void testToJson_UnicodeRoundTripsThroughEveryPath() throws Exception {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", UNICODE);
        final File jf = new File(tempDir, "u.json");
        JsonMappers.toJson(m, jf);
        final File xf = new File(tempDir, "u.xml");
        XmlMappers.toXml(m, xf);
        final ByteArrayOutputStream jb = new ByteArrayOutputStream();
        JsonMappers.toJson(m, jb);

        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(jf, Map.class).get("k"));
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(jf.toURI().toURL(), Map.class).get("k"));
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(new ByteArrayInputStream(jb.toByteArray()), Map.class).get("k"));
        Assertions.assertEquals(UNICODE, XmlMappers.fromXml(xf, Map.class).get("k"));
        Assertions.assertEquals(UNICODE, XmlMappers.fromXml(xf.toURI().toURL(), Map.class).get("k"));
        Assertions.assertEquals(UNICODE, FastJson.fromJson(FastJson.toJson(m), Map.class).get("k"));
        Assertions.assertEquals(UNICODE, FastJson.fromJson(FastJson.toJson(m).getBytes(StandardCharsets.UTF_8), Map.class).get("k"));
        Assertions.assertEquals(UNICODE, JsonUtil.unwrap(JsonUtil.wrap(m)).get("k"));
    }

    /** C-023: FastJson's byte[] overloads assume UTF-8 with no BOM; JsonMappers auto-detects the encoding. */
    @Test
    public void testFromJson_FastJsonByteInputRejectsBomAndUtf16() {
        final String json = "{\"k\":\"" + UNICODE + "\"}";
        final byte[] utf8 = json.getBytes(StandardCharsets.UTF_8);
        final byte[] utf16 = json.getBytes(StandardCharsets.UTF_16);
        final byte[] bom = new byte[utf8.length + 3];
        bom[0] = (byte) 0xEF;
        bom[1] = (byte) 0xBB;
        bom[2] = (byte) 0xBF;
        System.arraycopy(utf8, 0, bom, 3, utf8.length);

        // plain UTF-8 works everywhere
        Assertions.assertEquals(UNICODE, FastJson.fromJson(utf8, Map.class).get("k"));
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(utf8, Map.class).get("k"));

        // a BOM or UTF-16 is the documented divergence
        assertThrows(JSONException.class, () -> FastJson.fromJson(bom, Map.class));
        assertThrows(JSONException.class, () -> FastJson.fromJson(utf16, Map.class));
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(bom, Map.class).get("k"));
        Assertions.assertEquals(UNICODE, JsonMappers.fromJson(utf16, Map.class).get("k"));
    }

    /**
     * C-029: the two JSON facades disagree on the Java type a decimal binds to when the target is
     * {@code Map}/{@code Object} — a real hazard for code that casts, and for FastJson a lost signed zero.
     */
    @Test
    public void testFromJson_DecimalNumberTypesDivergeBetweenFacades() {
        final String json = "{\"i\":1,\"big\":12345678901234,\"d\":1.5,\"neg\":-0.0,\"huge\":123456789012345678901234567890}";
        final Map<?, ?> jm = JsonMappers.fromJson(json, Map.class);
        final Map<?, ?> fj = FastJson.fromJson(json, Map.class);

        // integers agree across both facades
        for (final Map<?, ?> m : new Map<?, ?>[] { jm, fj }) {
            Assertions.assertEquals(Integer.class, m.get("i").getClass());
            Assertions.assertEquals(Long.class, m.get("big").getClass());
            Assertions.assertEquals(java.math.BigInteger.class, m.get("huge").getClass());
        }

        // decimals do not
        Assertions.assertEquals(Double.class, jm.get("d").getClass());
        Assertions.assertEquals(java.math.BigDecimal.class, fj.get("d").getClass());

        // ... and FastJson loses the sign of negative zero, while JsonMappers keeps it
        Assertions.assertEquals(Double.class, jm.get("neg").getClass());
        Assertions.assertTrue(Double.doubleToRawLongBits((Double) jm.get("neg")) != 0L, "JsonMappers keeps -0.0");
        Assertions.assertEquals(0, ((java.math.BigDecimal) fj.get("neg")).signum(), "FastJson yields BigDecimal 0.0");
    }

    /** Deeply nested input is rejected cleanly by every backend rather than overflowing the stack. */
    @Test
    public void testFromJson_DeeplyNestedInputFailsCleanly() {
        final StringBuilder sb = new StringBuilder();
        final int depth = 50_000;
        sb.append("[".repeat(depth)).append("1").append("]".repeat(depth));
        final String deep = sb.toString();
        assertThrows(RuntimeException.class, () -> JsonMappers.fromJson(deep, List.class));
        assertThrows(RuntimeException.class, () -> FastJson.fromJson(deep, List.class));
        assertThrows(RuntimeException.class, () -> new JSONArray(deep));
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 3 - C-026 / C-030: non-finite floating point
    // ------------------------------------------------------------------------------------------------

    public static class PrimNum {
        private double d;
        private float f;

        public double getD() {
            return d;
        }

        public void setD(final double v) {
            d = v;
        }

        public float getF() {
            return f;
        }

        public void setF(final float v) {
            f = v;
        }
    }

    public static class BoxedNum {
        private Double d;

        public Double getD() {
            return d;
        }

        public void setD(final Double v) {
            d = v;
        }
    }

    /** C-026: FastJson writes non-finite values as JSON null and loses them, silently. */
    @Test
    public void testToJson_FastJsonSilentlyLosesNonFiniteValues() {
        final PrimNum p = new PrimNum();
        p.setD(Double.NaN);
        p.setF(Float.POSITIVE_INFINITY);
        Assertions.assertEquals("{\"d\":null,\"f\":null}", FastJson.toJson(p));

        // primitive target -> 0.0
        final PrimNum back = FastJson.fromJson(FastJson.toJson(p), PrimNum.class);
        Assertions.assertEquals(0.0d, back.getD());
        Assertions.assertEquals(0.0f, back.getF());
        Assertions.assertFalse(Double.isNaN(back.getD()), "the NaN is gone, with no error");

        // boxed / Map / List / bare targets -> null, not 0.0
        final BoxedNum b = new BoxedNum();
        b.setD(Double.NaN);
        Assertions.assertNull(FastJson.fromJson(FastJson.toJson(b), BoxedNum.class).getD());
        Assertions.assertNull(FastJson.fromJson(FastJson.toJson(Collections.singletonMap("d", Double.NaN)), Map.class).get("d"));
        Assertions.assertNull(FastJson.<List<Object>> fromJson(FastJson.toJson(Arrays.asList(Double.NaN)), List.class).get(0));

        // NaN, +Inf and -Inf all collapse to the same token, so they cannot be told apart afterwards
        Assertions.assertEquals(FastJson.toJson(Collections.singletonMap("x", Double.NaN)),
                FastJson.toJson(Collections.singletonMap("x", Double.POSITIVE_INFINITY)));
    }

    /** C-026: the documented lossless escape hatch, and that it matches Jackson byte for byte. */
    @Test
    public void testToJson_WriteFloatSpecialAsStringMakesFastJsonLossless() {
        final BoxedNum b = new BoxedNum();
        b.setD(Double.NaN);
        final String withFeature = FastJson.toJson(b, com.alibaba.fastjson2.JSONWriter.Feature.WriteFloatSpecialAsString);
        Assertions.assertEquals("{\"d\":\"NaN\"}", withFeature);
        Assertions.assertEquals(JsonMappers.toJson(b), withFeature, "identical to what Jackson writes by default");
        Assertions.assertTrue(Double.isNaN(FastJson.fromJson(withFeature, BoxedNum.class).getD()));
        // ... and each facade reads the other's output
        Assertions.assertTrue(Double.isNaN(FastJson.fromJson(JsonMappers.toJson(b), BoxedNum.class).getD()));
        Assertions.assertTrue(Double.isNaN(JsonMappers.fromJson(withFeature, BoxedNum.class).getD()));
    }

    /** C-026: JsonMappers/XmlMappers keep the value for a TYPED target but hand back a String for an untyped one. */
    @Test
    public void testFromJson_JacksonRoundTripTypedTargetsYieldStringForUntyped() {
        final PrimNum p = new PrimNum();
        p.setD(Double.NaN);
        p.setF(Float.NEGATIVE_INFINITY);

        Assertions.assertEquals("{\"d\":\"NaN\",\"f\":\"-Infinity\"}", JsonMappers.toJson(p));
        final PrimNum back = JsonMappers.fromJson(JsonMappers.toJson(p), PrimNum.class);
        Assertions.assertTrue(Double.isNaN(back.getD()));
        Assertions.assertEquals(Float.NEGATIVE_INFINITY, back.getF());

        // untyped target: the value comes back as a String, not a Double
        Assertions.assertEquals(String.class,
                JsonMappers.fromJson(JsonMappers.toJson(Collections.singletonMap("d", Double.NaN)), Map.class).get("d").getClass());

        // XmlMappers writes bare text and round-trips a typed target
        Assertions.assertTrue(XmlMappers.toXml(p).contains("<d>NaN</d>"));
        Assertions.assertTrue(Double.isNaN(XmlMappers.fromXml(XmlMappers.toXml(p), PrimNum.class).getD()));

        // JsonUtil rejects them outright, on every wrap overload
        assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(Collections.singletonMap("d", (Object) Double.NaN)));
        assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(new double[] { Double.NaN }));
        assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(new Object[] { Double.POSITIVE_INFINITY }));
    }

    /**
     * C-030: FastJson's documented "parse failures throw JSONException" contract does not hold for a
     * top-level quoted non-numeric string bound to a floating-point type — an upstream FastJSON2 defect
     * that surfaces an {@link ArrayIndexOutOfBoundsException}. Reachable because JsonMappers writes exactly
     * that token for a bare non-finite Double.
     */
    @Test
    public void testFromJson_TopLevelQuotedScalarToDoubleThrowsAioobe() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FastJson.fromJson("\"Infinity\"", Double.class));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FastJson.fromJson("\"Infinity\"", Float.class));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FastJson.fromJson("\"I\"", Double.class));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FastJson.fromJson("\"AAAAAAAAAA\"", Double.class));

        // the reachable cross-facade path the javadoc warns about
        final String bare = JsonMappers.toJson(Double.POSITIVE_INFINITY);
        Assertions.assertEquals("\"Infinity\"", bare);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FastJson.fromJson(bare, Double.class));

        // scope of the defect: "NaN" is special-cased, BigDecimal behaves, nested positions are fine
        Assertions.assertTrue(Double.isNaN(FastJson.fromJson("\"NaN\"", Double.class)));
        assertThrows(JSONException.class, () -> FastJson.fromJson("\"Infinity\"", java.math.BigDecimal.class));
        Assertions.assertEquals("Infinity", FastJson.fromJson("{\"d\":\"Infinity\"}", Map.class).get("d"));
        Assertions.assertEquals("Infinity", FastJson.<List<Object>> fromJson("[\"Infinity\"]", List.class).get(0));
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 4 - C-032 / C-033: empty beans and circular references
    // ------------------------------------------------------------------------------------------------

    /** A bean exposing no properties at all. */
    public static class NoProps {
    }

    /** C-032: an empty bean is rejected by the Jackson-backed classes but written as {} by FastJson. */
    @Test
    public void testToJson_EmptyBeanRejectedByJacksonBackedClassesOnly() {
        assertThrows(RuntimeException.class, () -> JsonMappers.toJson(new NoProps()));
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(new NoProps()));
        Assertions.assertEquals("{}", FastJson.toJson(new NoProps()));
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap(new NoProps()));

        // the documented remedy: only the config overload can turn the feature off
        Assertions.assertEquals("{}",
                JsonMappers.toJson(new NoProps(), JsonMappers.createSerializationConfig().without(SerializationFeature.FAIL_ON_EMPTY_BEANS)));
        Assertions.assertEquals("<NoProps/>",
                XmlMappers.toXml(new NoProps(), XmlMappers.createSerializationConfig().without(SerializationFeature.FAIL_ON_EMPTY_BEANS)));
    }

    public static class CycleNode {
        private String name = "n";
        private CycleNode child;
        private List<CycleNode> kids;

        public String getName() {
            return name;
        }

        public void setName(final String s) {
            name = s;
        }

        public CycleNode getChild() {
            return child;
        }

        public void setChild(final CycleNode n) {
            child = n;
        }

        public List<CycleNode> getKids() {
            return kids;
        }

        public void setKids(final List<CycleNode> k) {
            kids = k;
        }
    }

    /**
     * C-033: {@code JsonUtil}'s circular-reference doc used to say only "a recursion error or an org.json
     * nesting-depth error". There are three distinct outcomes, and two of them are {@link StackOverflowError}
     * — an {@link Error}, which {@code catch (Exception)} will not stop.
     */
    @Test
    public void testToJson_CyclicGraphsFailInThreeDocumentedWays() {
        // (1) a pure bean cycle is DETECTED and reported cleanly
        final CycleNode self = new CycleNode();
        self.setChild(self);
        final IllegalArgumentException direct = assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap(self));
        Assertions.assertTrue(direct.getMessage().contains("Cyclic bean reference"), direct.getMessage());

        final CycleNode x = new CycleNode();
        final CycleNode y = new CycleNode();
        x.setChild(y);
        y.setChild(x);
        assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap(x));

        // (2) a self-containing Collection/Map hits org.json's own depth guard
        final List<Object> selfList = new ArrayList<>();
        selfList.add(selfList);
        Assertions.assertTrue(assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(selfList)).getMessage().contains("recursion depth"));
        final Map<String, Object> selfMap = new LinkedHashMap<>();
        selfMap.put("me", selfMap);
        Assertions.assertTrue(assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(selfMap)).getMessage().contains("recursion depth"));

        // (3) a cycle running THROUGH a collection/array back to a bean is NOT detected -> StackOverflowError
        final CycleNode viaList = new CycleNode();
        viaList.setKids(new ArrayList<>());
        viaList.getKids().add(viaList);
        assertThrows(StackOverflowError.class, () -> JsonUtil.wrap(viaList));
        final Object[] selfArr = new Object[1];
        selfArr[0] = selfArr;
        assertThrows(StackOverflowError.class, () -> JsonUtil.wrap(selfArr));
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 4 - C-034 / C-036: XML element names are not validated; prefixes are dropped on read
    // ------------------------------------------------------------------------------------------------

    private static boolean isWellFormed(final String xml) {
        try {
            final javax.xml.parsers.DocumentBuilder b = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            b.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void error(final org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
                    throw e;
                }

                @Override
                public void fatalError(final org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
                    throw e;
                }
            });
            b.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private static String xmlOf(final String key) {
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, "V");
        return XmlMappers.toXml(m);
    }

    /** Invalid XML element names are rejected before serialization can report success. */
    @Test
    public void testToXml_InvalidElementNamesAreRejected() {
        for (final String key : new String[] { "1abc", "a b", "a<b", "" }) {
            assertThrows(RuntimeException.class, () -> xmlOf(key), "key [" + key + "]");
        }
        // File output obeys the same validation policy; writing need not be atomic.
        final File f = new File(tempDir, "bad.xml");
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("1abc", "V");
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(m, f));
    }

    /** Invalid local names, including embedded namespace separators, cannot silently change the data. */
    @Test
    public void testToXml_InvalidLocalNamesCannotSilentlyChangeData() {
        for (final String key : new String[] { "a>b", "x ", "x\t", "xml:lang", "a:b" }) {
            assertThrows(RuntimeException.class, () -> xmlOf(key), "key [" + key + "]");
        }
    }

    /** C-034: the documented-safe shapes must stay safe, and the JSON facades must stay unaffected. */
    @Test
    public void testToXml_ValidNamesRoundTripAndJsonFacadesAreUnrestricted() {
        for (final String key : new String[] { "good", "_a", "a-b", "a.b", "café", "日本語", "xmlFoo", "xmlns" }) {
            final String xml = xmlOf(key);
            Assertions.assertTrue(isWellFormed(xml), "expected well-formed for key [" + key + "]: " + xml);
            Assertions.assertEquals(Collections.singletonMap(key, "V"), XmlMappers.fromXml(xml, Map.class), "key [" + key + "]");
        }
        // JSON has no such restriction
        for (final String key : new String[] { "1abc", "a b", "a<b", "a>b", "x ", "" }) {
            final Map<String, Object> m = new LinkedHashMap<>();
            m.put(key, "V");
            Assertions.assertEquals(m, JsonMappers.fromJson(JsonMappers.toJson(m), Map.class), "key [" + key + "]");
        }
    }

    /** C-034: the documented escape hatch really does round-trip every hostile key. */
    @Test
    public void testToXml_Base64NameProcessorRoundTripsCorruptNames() {
        final XmlMappers.One safe = XmlMappers
                .wrap(XmlMapper.builder().xmlNameProcessor(com.fasterxml.jackson.dataformat.xml.XmlNameProcessors.newBase64Processor()).build());
        for (final String key : new String[] { "1abc", "a b", "a<b", "a>b", "x " }) {
            final Map<String, Object> m = new LinkedHashMap<>();
            m.put(key, "V");
            final String xml = safe.toXml(m);
            Assertions.assertTrue(isWellFormed(xml), "key [" + key + "]: " + xml);
            Assertions.assertEquals(m, safe.fromXml(xml, Map.class), "key [" + key + "]");
        }
    }

    /** C-036: reading into an untyped Map discards namespace prefixes, merging distinct elements. */
    @Test
    public void testFromXml_NamespacePrefixesAreDiscardedOnRead() {
        final Map<?, ?> prefixed = XmlMappers.fromXml("<r xmlns:p='urn:p' xmlns:q='urn:q'><p:x>1</p:x><q:x>2</q:x></r>", Map.class);
        final Map<?, ?> plain = XmlMappers.fromXml("<r><x>1</x><x>2</x></r>", Map.class);
        Assertions.assertEquals(plain, prefixed, "two structurally different documents read back identically");
        Assertions.assertEquals(Collections.singleton("x"), prefixed.keySet());
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 5 - C-037: the XML root element name is the runtime implementation class
    // ------------------------------------------------------------------------------------------------

    @com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement(localName = "config")
    public static class RootAnnotated {
        private String a = "v";

        public String getA() {
            return a;
        }

        public void setA(final String s) {
            a = s;
        }
    }

    /** C-037: the same logical Map serializes to a different document per implementation class. */
    @Test
    public void testToXml_RootElementNameLeaksMapImplementationClass() {
        Assertions.assertEquals("<HashMap><a>1</a></HashMap>", XmlMappers.toXml(new HashMap<>(Map.of("a", 1))));
        Assertions.assertEquals("<LinkedHashMap><a>1</a></LinkedHashMap>", XmlMappers.toXml(new LinkedHashMap<>(Map.of("a", 1))));
        Assertions.assertEquals("<TreeMap><a>1</a></TreeMap>", XmlMappers.toXml(new TreeMap<>(Map.of("a", 1))));
        Assertions.assertEquals("<SingletonMap><a>1</a></SingletonMap>", XmlMappers.toXml(Collections.singletonMap("a", 1)));
        // JDK-internal names leak into the wire format
        Assertions.assertEquals("<Map1><a>1</a></Map1>", XmlMappers.toXml(Map.of("a", 1)));
        Assertions.assertTrue(XmlMappers.toXml(List.of(1, 2)).startsWith("<List12>"));
        Assertions.assertEquals("<EmptyMap/>", XmlMappers.toXml(Collections.emptyMap()));

        // reading is tolerant of any root name, which is why this only bites external consumers
        for (final String xml : new String[] { "<HashMap><a>1</a></HashMap>", "<Map1><a>1</a></Map1>", "<whatever><a>1</a></whatever>" }) {
            Assertions.assertEquals(Collections.singletonMap("a", "1"), XmlMappers.fromXml(xml, Map.class), xml);
        }

        // a bean root is stable, and the annotation overrides it
        Assertions.assertEquals("<RootAnnotated><a>v</a></RootAnnotated>".replace("RootAnnotated", "config"), XmlMappers.toXml(new RootAnnotated()));

        // JSON has no equivalent leak
        Assertions.assertEquals(JsonMappers.toJson(new HashMap<>(Map.of("a", 1))), JsonMappers.toJson(Collections.singletonMap("a", 1)));
    }

    /** C-037: the documented remedy pins the root name regardless of implementation. */
    @Test
    public void testToXml_WithRootNamePinsTheRootAcrossImplementations() throws Exception {
        final XmlMapper m = new XmlMapper();
        final String fromHash = m.writer().withRootName("root").writeValueAsString(new HashMap<>(Map.of("a", 1)));
        final String fromLinked = m.writer().withRootName("root").writeValueAsString(new LinkedHashMap<>(Map.of("a", 1)));
        Assertions.assertEquals("<root><a>1</a></root>", fromHash);
        Assertions.assertEquals(fromHash, fromLinked);
    }

    /** The One wrapper must be behaviourally identical to the static methods for an equivalent mapper. */
    @Test
    public void testWrap_OneWrapperMatchesStaticMethodsForEquivalentMapper() {
        final XmlMappers.One xone = XmlMappers.wrap(new XmlMapper());
        final JsonMappers.One jone = JsonMappers.wrap(new ObjectMapper());
        for (final Object v : new Object[] { new Bean("a", 1), new HashMap<>(Map.of("a", 1)), List.of(1, 2), "s", 42 }) {
            Assertions.assertEquals(XmlMappers.toXml(v), xone.toXml(v), "XML differs for " + v.getClass().getSimpleName());
            Assertions.assertEquals(JsonMappers.toJson(v), jone.toJson(v), "JSON differs for " + v.getClass().getSimpleName());
        }
    }

    /** A bean whose second getter throws, to force a failure part-way through serialization. */
    public static class Exploding {
        public String getA() {
            return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        }

        public String getB() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * C-024: the File overloads truncate the target before serializing, so a failure destroys whatever was
     * there. For XML what survives is <i>parseable but semantically empty</i>, which is the dangerous case.
     */
    @Test
    public void testToJson_FailedFileWriteDestroysPreviousContent() throws Exception {
        final File j = new File(tempDir, "prev.json");
        Files.write(j.toPath(), "{\"good\":true}".getBytes(StandardCharsets.UTF_8));
        assertThrows(RuntimeException.class, () -> JsonMappers.toJson(new Exploding(), j));
        Assertions.assertNotEquals("{\"good\":true}", new String(Files.readAllBytes(j.toPath()), StandardCharsets.UTF_8),
                "documented: the previous content is gone");

        final File f = new File(tempDir, "prev2.json");
        Files.write(f.toPath(), "{\"good\":true}".getBytes(StandardCharsets.UTF_8));
        assertThrows(RuntimeException.class, () -> FastJson.toJson(new Exploding(), f));
        Assertions.assertEquals(0, f.length(), "FastJson leaves the target empty");

        final File x = new File(tempDir, "prev.xml");
        Files.write(x.toPath(), "<good/>".getBytes(StandardCharsets.UTF_8));
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(new Exploding(), x));
        final String left = new String(Files.readAllBytes(x.toPath()), StandardCharsets.UTF_8);
        Assertions.assertNotEquals("<good/>", left);
        // The trap the javadoc warns about: the root element gets closed on the way out, so whatever was
        // flushed before the failure is left behind as WELL-FORMED xml that parses cleanly back into a
        // silently incomplete object. How much was flushed depends on generator buffering, so assert the
        // property that matters rather than an exact byte sequence.
        Assertions.assertTrue(left.startsWith("<Exploding>") && left.endsWith("</Exploding>"), "left behind: " + left);
        final Map<?, ?> reparsed = XmlMappers.fromXml(left, Map.class);
        Assertions.assertNotNull(reparsed, "the wreckage still parses - that is the danger");
        Assertions.assertFalse(reparsed.containsKey("b"), "the failed property is simply absent, with no error");
    }

    /**
     * C-027: an unwritable {@code File} target surfaces as an {@link IllegalArgumentException} from FastJson
     * (its {@code IOUtil} open-check names the problem) but as an {@code UncheckedIOException} from the two
     * Jackson-backed classes. Both are unchecked and carry the {@code FileNotFoundException} as the cause.
     */
    @Test
    public void testToJson_UnwritableFileTargetThrowsDifferentTypes() {
        // a directory can never be opened as a file for writing
        final IllegalArgumentException fj = assertThrows(IllegalArgumentException.class, () -> FastJson.toJson(new Bean("a", 1), tempDir));
        Assertions.assertTrue(fj.getMessage().contains("is a directory"), fj.getMessage());
        Assertions.assertInstanceOf(java.io.FileNotFoundException.class, fj.getCause());

        final RuntimeException jm = assertThrows(RuntimeException.class, () -> JsonMappers.toJson(new Bean("a", 1), tempDir));
        Assertions.assertInstanceOf(com.landawn.abacus.exception.UncheckedIOException.class, jm);
        final RuntimeException xm = assertThrows(RuntimeException.class, () -> XmlMappers.toXml(new Bean("a", 1), tempDir));
        Assertions.assertInstanceOf(com.landawn.abacus.exception.UncheckedIOException.class, xm);
    }

    /** A caller-supplied stream can already hold a partial document when the exception is thrown. */
    @Test
    public void testToJson_FailedStreamWriteLeavesPartialDocument() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertThrows(RuntimeException.class, () -> JsonMappers.toJson(new Exploding(), bos));
        Assertions.assertFalse(bos.toString(StandardCharsets.UTF_8).isEmpty(), "documented: partial output is possible");
    }

    // ------------------------------------------------------------------------------------------------
    // J7 - the XML DataInput read family always fails; the javadoc now says so
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testFromXml_DataInputAlwaysThrowsUnsupportedOperationException() throws Exception {
        final byte[] xml = "<Bean><name>a</name><age>1</age></Bean>".getBytes(StandardCharsets.UTF_8);
        final TypeReference<Bean> tr = new TypeReference<>() {
        };
        final DeserializationConfig cfg = XmlMappers.createDeserializationConfig();
        // DataInputStream is both a DataInput and an InputStream, so the target type must be stated explicitly
        // to pick the DataInput overloads.
        final DataInput di1 = new DataInputStream(new ByteArrayInputStream(xml));
        final DataInput di2 = new DataInputStream(new ByteArrayInputStream(xml));
        final DataInput di3 = new DataInputStream(new ByteArrayInputStream(xml));
        final DataInput di4 = new DataInputStream(new ByteArrayInputStream(xml));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(di1, Bean.class));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(di2, Bean.class, cfg));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(di3, tr));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(di4, tr, cfg));
        // ... while DataOutput writes do work
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput dout = new DataOutputStream(bos);
        XmlMappers.toXml(new Bean("a", 1), dout);
        Assertions.assertTrue(bos.toString(StandardCharsets.UTF_8).contains("<name>a</name>"));
    }

    // ------------------------------------------------------------------------------------------------
    // D6 - the wrapper's output format follows the supplied mapper, not the class name
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testWrap_JsonMappersWithXmlMapperEmitsXml() {
        Assertions.assertEquals("<Bean><name>a</name><age>1</age></Bean>", JsonMappers.wrap(new XmlMapper()).toJson(new Bean("a", 1)));
    }

    // ------------------------------------------------------------------------------------------------
    // General round-trip sanity across every changed write path
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testRoundTripsAcrossWritePaths() throws Exception {
        final Bean b = new Bean("a", 1);

        final StringWriter sw = new StringWriter();
        JsonMappers.toJson(b, sw);
        Assertions.assertEquals("a", JsonMappers.fromJson(sw.toString(), Bean.class).getName());

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JsonMappers.toJson(b, bos);
        Assertions.assertEquals("a", JsonMappers.fromJson(bos.toByteArray(), Bean.class).getName());

        final File f = new File(tempDir, "rt.json");
        JsonMappers.toJson(b, f);
        Assertions.assertEquals("a", JsonMappers.fromJson(f, Bean.class).getName());

        final StringWriter xsw = new StringWriter();
        XmlMappers.toXml(b, xsw);
        Assertions.assertEquals("a", XmlMappers.fromXml(xsw.toString(), Bean.class).getName());

        Assertions.assertEquals("a", XmlMappers.fromXml(XmlMappers.toXml(b, true), Bean.class).getName());
        Assertions.assertEquals("a", JsonMappers.fromJson(JsonMappers.toJson(b, true), Bean.class).getName());
        Assertions.assertEquals("a", FastJson.fromJson(FastJson.toJson(b, true), Bean.class).getName());
    }
}
