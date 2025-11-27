package com.landawn.abacus.parser;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.base.Strings;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.FastJson;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeReference;

import lombok.Data;

@Tag("2025")
public class JSONParserImpl2025Test extends TestBase {

    @Test
    public void test_ParameterizedBean() throws Exception {

        ParameterizedBean<Integer, String, Double, Byte, Short> bean = new ParameterizedBean<>();
        bean.setA(100);
        bean.setB(new String[] { "one", "two", "three" });
        bean.setCList(N.asList(11.11, 22.22, 33.33));
        bean.setMapList(N.asList(N.asMap((byte) 1, (short) 10), N.asMap((byte) 2, (short) 20)));
        bean.setName("ParameterizedBean");
        bean.setBb(new String[][] { { "aa", "bb" }, { "cc", "dd" } });
        bean.setMap2(N.asMap(N.asSingletonList(new Byte[] { 1, 2 }), N.asSingletonList(new String[][] { { "v11", "v12" }, { "v21", "v22" } })));
        bean.setMap3(N.asMap(N.asSingletonList(new String[] { "s1", "s2" }), new String[][] { { "m31", "m32" }, { "m41", "m42" } }));
        bean.setMap4(N.asMap(N.asSingletonList(new String[][] { { "x1", "x2" }, { "y1", "y2" } }), (byte) 100));

        String json = N.toJson(bean);

        N.println(Strings.repeat("=", 80));

        N.println(json);

        //        final ObjectMapper mapper = new ObjectMapper();
        //
        //        ParameterizedBean<Integer, String, Double, Byte, Short> ret = mapper.readValue(json,
        //                mapper.getTypeFactory().constructParametricType(ParameterizedBean.class, Integer.class, String.class, Double.class, Byte.class, Short.class));
        //
        //        N.println(Strings.repeat("=", 80));
        //        System.out.println(ret);
        //        System.out.println(ret.getClass());
        //        System.out.println(ret.getA().getClass());
        //        System.out.println(ret.getB().getClass());
        //        System.out.println(ret.getCList().get(0).getClass());
        //        System.out.println(ret.getMapList().get(0).entrySet().iterator().next().getKey().getClass());
        //        System.out.println(ret.getMapList().get(0).entrySet().iterator().next().getValue().getClass());

        ParameterizedBean<Integer, String, Double, Byte, Short> ret = FastJson.fromJson(json,
                new TypeReference<ParameterizedBean<Integer, String, Double, Byte, Short>>() {
                }.javaType());

        N.println(Strings.repeat("=", 80));
        assertEquals(json, N.toJson(ret));
        assertEquals(ParameterizedBean.class, ret.getClass());
        assertEquals(Integer.class, ret.getA().getClass());
        assertEquals(String[].class, ret.getB().getClass());
        assertEquals(Double.class, ret.getCList().get(0).getClass());
        assertEquals(Byte.class, ret.getMapList().get(0).entrySet().iterator().next().getKey().getClass());
        assertEquals(Short.class, ret.getMapList().get(0).entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getBb().getClass());

        assertEquals(Byte[].class, ret.getMap2().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap2().entrySet().iterator().next().getValue().get(0).getClass());

        assertEquals(String[].class, ret.getMap3().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap3().entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getMap4().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(Byte.class, ret.getMap4().entrySet().iterator().next().getValue().getClass());

        Type<ParameterizedBean<Integer, String, Double, Byte, Short>> type = Type
                .of(new TypeReference<ParameterizedBean<Integer, String, Double, Byte, Short>>() {
                }.javaType());

        ret = N.fromJson(json, type);

        N.println(Strings.repeat("=", 80));
        assertEquals(json, N.toJson(ret));
        assertEquals(ParameterizedBean.class, ret.getClass());
        assertEquals(Integer.class, ret.getA().getClass());
        assertEquals(String[].class, ret.getB().getClass());
        assertEquals(Double.class, ret.getCList().get(0).getClass());
        assertEquals(Byte.class, ret.getMapList().get(0).entrySet().iterator().next().getKey().getClass());
        assertEquals(Short.class, ret.getMapList().get(0).entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getBb().getClass());

        assertEquals(Byte[].class, ret.getMap2().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap2().entrySet().iterator().next().getValue().get(0).getClass());

        assertEquals(String[].class, ret.getMap3().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap3().entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getMap4().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(Byte.class, ret.getMap4().entrySet().iterator().next().getValue().getClass());

        N.println(type.name());
        N.println(type.javaType());
    }

    @Data
    public static class ParameterizedBean<A, B, C, K, V> {
        private A a;
        private B[] b;
        private List<C> cList;
        private List<Map<K, V>> mapList;
        private String name;
        private B[][] bb;
        private Map<List<K[]>, List<B[][]>> map2;
        private Map<List<String[]>, B[][]> map3;
        private Map<List<B[][]>, Byte> map4;
    }

    @Test
    public void test_constructor_default() {
        JSONParser parser = new JSONParserImpl();
        assertNotNull(parser);
    }

    @Test
    public void test_constructor_withConfig() {
        JSONSerializationConfig jsc = new JSONSerializationConfig();
        JSONDeserializationConfig jdc = new JSONDeserializationConfig();
        JSONParser parser = new JSONParserImpl(jsc, jdc);
        assertNotNull(parser);
    }

    @Test
    public void test_serialize_string() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize("test");
        assertNotNull(json);
    }

    @Test
    public void test_deserialize_string() {
        JSONParser parser = new JSONParserImpl();
        String result = parser.deserialize("\"test\"", String.class);
        assertEquals("\"test\"", result);
    }

    @Test
    public void test_serialize_integer() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize(123);
        assertEquals("123", json);
    }

    @Test
    public void test_deserialize_integer() {
        JSONParser parser = new JSONParserImpl();
        Integer result = parser.deserialize("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    public void test_serialize_boolean() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize(true);
        assertEquals("true", json);
    }

    @Test
    public void test_deserialize_boolean() {
        JSONParser parser = new JSONParserImpl();
        Boolean result = parser.deserialize("true", Boolean.class);
        assertEquals(true, result);
    }

    @Test
    public void test_serialize_null() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize(null);
        assertNull(json);
    }
}
