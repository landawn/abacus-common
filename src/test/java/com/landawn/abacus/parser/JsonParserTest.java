package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountDevice;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XmlSerializationConfig.XSC;
import com.landawn.abacus.parser.entity.GenericEntity;
import com.landawn.abacus.parser.entity.TypeBean;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.Clazz;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.EntityId;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.JsonMappers;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.MutableChar;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.TypeReference;
import com.landawn.abacus.util.UnitTestUtil;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Tag("old-test")
public class JsonParserTest extends AbstractJsonParserTest {

    @Data
    @AllArgsConstructor
    public static class Snapshot {
        LinkedMultiValueMap<String, InstanceMeta> registry;
        Map<String, Long> timestamps;
        Map<String, Long> versions;

        long version;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(of = { "schema", "host", "port", "context" })
    public static class InstanceMeta {
        private String schema;
        private String host;
        private Integer port;
        private String context;
        private Boolean status;
    }

    @Test
    public void test_2817() throws Exception {
        final LocalDateTime now = LocalDateTime.now();
        Bean2817.builder().fieldA(now).build();

        final Map<String, Object> map = new HashMap<>();
        map.put("fieldA", Timestamp.valueOf(now).getTime());
        final String json = N.toJson(map);

        N.println(json);

        final Bean2817 fromJson = N.fromJson(json, Bean2817.class);

        N.println(fromJson);

        final Map<String, BigDecimal> m = N.fromJson("{\"token\": 2.105465717176397390012604E+2043348}", Type.ofMap(String.class, BigDecimal.class));
        N.println(m);

        final Map<String, Double> m2 = N.fromJson("{\"token\": 2.105465717176397390012604E+2043348}", Type.ofMap(String.class, Double.class));
        N.println(m2);
    }

    @Builder
    @Data
    public static class Bean2817 {
        private LocalDateTime fieldA;
    }

    @Test
    public void test_2958() throws Exception {
        N.println(TypeFactory.getType(AtomicDouble.class));

        final UnsignedInteger unsignedInteger = UnsignedInteger.valueOf(String.valueOf(Integer.MIN_VALUE).replace("-", ""));
        final UnsignedLong unsignedLong = UnsignedLong.valueOf(String.valueOf(Long.MIN_VALUE).replace("-", ""));
        final BeanX bean = BeanX.builder()
                .count0(unsignedInteger)
                .count(unsignedLong)
                .countA(new AtomicInteger(10))
                .countB(new AtomicLong(Long.MAX_VALUE))
                .countC(new AtomicDouble(100))
                .countD(new MutableDouble(100))
                .fieldA(new MutableBoolean(true))
                .fieldB(MutableChar.of('a'))
                .build();

        final Map<String, Object> map = new HashMap<>();
        map.put("count", unsignedLong);

        System.out.println("fastjson2:  " + JSONObject.toJSONString(map));

        final ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("jackson:  " + objectMapper.writeValueAsString(map));

        final Gson gson = new GsonBuilder().create();
        System.out.println("gson:  " + gson.toJson(map));

        N.println(N.toJson(map));
        N.println(N.toJson(bean));
        assertEquals(
                "{\"count0\": 2147483648, \"count\": 9223372036854775808, \"countA\": 10, \"countB\": 9223372036854775807, \"countC\": 100.0, \"countD\": 100.0, \"fieldA\": true, \"fieldB\": \"a\"}",
                N.toJson(bean));
        N.fromJson(N.toJson(bean), BeanX.class);
        assertEquals(map, N.fromJson(N.toJson(map), Type.ofMap(String.class, UnsignedLong.class)));
    }

    @Builder
    @Data
    public static class BeanX {
        private UnsignedInteger count0;
        private UnsignedLong count;

        private AtomicInteger countA;
        private AtomicLong countB;
        private AtomicDouble countC;
        private MutableDouble countD;
        private MutableBoolean fieldA;
        private MutableChar fieldB;

    }

    @Test
    public void test_emptyBean() {
        final Object obj = new Object();
        final SerializationConfig serializationConfig = JsonMappers.createSerializationConfig().without(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        String json = JsonMappers.toJson(obj, serializationConfig);
        N.println(json);
        json = JsonMappers.toJson(N.asMap("key1", obj, "key2", "ddd"), serializationConfig);
        N.println(json);
        json = JsonMappers.toJson(N.asList("key1", obj, "key2", "ddd"), serializationConfig);
        N.println(json);

        try {
            json = N.toJson(obj);
            fail("Should throw ParseException");
        } catch (final ParseException e) {

        }

        final JsonSerializationConfig jsc = JSC.create().failOnEmptyBean(false);

        json = N.toJson(obj, jsc);
        N.println(json);
        json = N.toJson(N.asMap("key1", obj, "key2", "ddd"), jsc);
        N.println(json);
        json = N.toJson(N.asList("key1", obj, "key2", "ddd"), jsc);
        N.println(json);
    }

    @Test
    public void test_selfReference() {
        final List<Object> list = N.asList("a", "b");
        list.add(list);

        final Map<String, Object> map = N.asMap("a", "va", "b", 123);
        map.put("c", map);

        try {
            N.toJson(list);
            fail("Should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        try {
            N.toJson(map);
            fail("Should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        final JsonSerializationConfig jsc = JSC.create().supportCircularReference(true);

        String json = N.toJson(list, jsc);
        N.println(json);

        json = N.toJson(map, jsc);
        N.println(json);
    }

    @Test
    public void test_Map() {
        final Map<Object, Object> map = N.asMap(1, "a", "b", 2, 3, 3, "d", "d", 10, "x");
        final String json = N.toJson(map);
        N.println(json);

        final Map map2 = N.fromJson(json, Map.class);
        N.println(map2);

    }

    @Test
    public void test_PropHandler() {

        final Account account = Beans.newRandom(Account.class);
        N.println(account);

        account.setDevices(Beans.newRandomList(AccountDevice.class, 10));

        final String json = N.toJson(account);
        assertEquals(account, N.fromJson(json, Account.class));

        final Account account2 = N.fromJson(json, JDC.create().setPropHandler("devices", (c, e) -> {
            N.println(e);
        }), Account.class);

        assertEquals(0, account2.getDevices().size());

    }

    @Test
    public void test_MalformedJsonException() {

        final String str = "\\uXYZ";

        final String result = N.fromJson(str, String.class);

        N.println(result);

    }

    public enum RoomIdentifier {
        @SerializedName("MARKER_NAME")
        @JsonXmlField(name = "MARKER_NAME")
        ROOM_NAME;
    }

    @Test
    public void test_escape() {
        final Map<String, Object> map = N.asMap("a", "kdafs'ksfkd\"", "b", "\"", "c", '\'');

        N.println(N.toJson(map, true));

        N.println(JSON.toJSONString(map));
    }

    @Test
    public void test_Convert() {
        final RoomIdentifier id = RoomIdentifier.ROOM_NAME;

        final Map<RoomIdentifier, String> slots = new HashMap<>();
        slots.put(RoomIdentifier.ROOM_NAME, "ROOM_NAME_TEST");

        final String strSerialized = gson.toJson(slots);

        final java.lang.reflect.Type type = new TypeToken<Map<RoomIdentifier, String>>() {
        }.getType();
        final Map<RoomIdentifier, String> slotsDeserialized = gson.fromJson(strSerialized, type);

        System.out.println(gson.toJson(id));
        System.out.println(strSerialized);
        System.out.println(slotsDeserialized);

        N.println(Strings.repeat('=', 80));

        N.println(N.toJson(id));
        final String json = N.toJson(slots);
        N.println(json);
        assertEquals("{\"MARKER_NAME\": \"ROOM_NAME_TEST\"}", json);
        N.println(N.fromJson(json, new TypeReference<Map<RoomIdentifier, String>>() {
        }.type()));
        assertEquals(slots, N.fromJson(json, new TypeReference.TypeToken<Map<RoomIdentifier, String>>() {
        }.type()));
    }

    @Test
    public void test_bigValue() {
        final String bigValue = Strings.repeat(Strings.uuid() + '\\', 100);

        final TypeBean xbean = new TypeBean();

        xbean.setStringType(bigValue);
        xbean.setStringListType(N.asList(bigValue, bigValue, bigValue));
        xbean.setStringArrayType(N.asArray(bigValue, bigValue));

        {
            final String josn = jsonParser.serialize(xbean);
            final TypeBean xbeanFromJson = jsonParser.deserialize(josn, TypeBean.class);

            assertEquals(xbean.getStringType(), xbeanFromJson.getStringType());
            assertEquals(xbean, xbeanFromJson);
            assertEquals(xbean, jsonParser.deserialize(IOUtil.stringToReader(josn), TypeBean.class));
        }

        {
            final String xml = xmlParser.serialize(xbean);
            final TypeBean xbeanFromXml = xmlParser.deserialize(xml, TypeBean.class);

            assertEquals(xbean.getStringType(), xbeanFromXml.getStringType());
            assertEquals(xbean, xbeanFromXml);
            assertEquals(xbean, xmlParser.deserialize(IOUtil.stringToReader(xml), TypeBean.class));
        }

        {
            final String xml = xmlDOMParser.serialize(xbean);
            final TypeBean xbeanFromXml = xmlDOMParser.deserialize(xml, TypeBean.class);

            assertEquals(xbean.getStringType(), xbeanFromXml.getStringType());
            assertEquals(xbean, xbeanFromXml);
            assertEquals(xbean, xmlDOMParser.deserialize(IOUtil.stringToReader(xml), TypeBean.class));
        }

        {
            final String xml = abacusXmlParser.serialize(xbean);
            final TypeBean xbeanFromXml = abacusXmlParser.deserialize(xml, TypeBean.class);

            assertEquals(xbean.getStringType(), xbeanFromXml.getStringType());
            assertEquals(xbean, xbeanFromXml);
            assertEquals(xbean, abacusXmlParser.deserialize(IOUtil.stringToReader(xml), TypeBean.class));
        }

        {
            final String xml = abacusXMLStAXParser.serialize(xbean);
            final TypeBean xbeanFromXml = abacusXMLStAXParser.deserialize(xml, TypeBean.class);

            assertEquals(xbean.getStringType(), xbeanFromXml.getStringType());
            assertEquals(xbean, xbeanFromXml);
            assertEquals(xbean, abacusXMLStAXParser.deserialize(IOUtil.stringToReader(xml), TypeBean.class));
        }

        {
            final String xml = abacusXMLDOMParser.serialize(xbean);
            final TypeBean xbeanFromXml = abacusXMLDOMParser.deserialize(xml, TypeBean.class);

            assertEquals(xbean.getStringType(), xbeanFromXml.getStringType());
            assertEquals(xbean, xbeanFromXml);
            assertEquals(xbean, abacusXMLDOMParser.deserialize(IOUtil.stringToReader(xml), TypeBean.class));
        }
    }

    @Test
    public void test_put_get_0() {
        for (int i = 0; i < 1000; i++) {
            final Account account = createAccount(Account.class);
            final StringBuilder sb = Objectory.createStringBuilder();

            int tmp = Math.abs(rand.nextInt(1000));

            while (tmp-- > 0) {
                if (i % 3 == 0) {
                    sb.append(account.getGUI()).append('\\');
                } else {
                    sb.append(account.getGUI());
                }
            }

            account.setFirstName(sb.toString());

            if (Strings.isEmpty(account.getFirstName())) {
                account.setFirstName("xxx");
            }
            account.setLastUpdateTime(null);
            account.setCreatedTime(null);

            Objectory.recycle(sb);

            {
                final String josn = jsonParser.serialize(account);
                final Account accountFromJson = jsonParser.deserialize(josn, Account.class);

                assertEquals(account.getFirstName(), accountFromJson.getFirstName());
                assertEquals(account, accountFromJson);
                assertEquals(account, jsonParser.deserialize(IOUtil.stringToReader(josn), Account.class));
            }

            {
                final String xml = xmlParser.serialize(account);
                final Account accountFromXml = xmlParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), accountFromXml.getFirstName());
                assertEquals(account, accountFromXml);
                assertEquals(account, xmlParser.deserialize(IOUtil.stringToReader(xml), Account.class));
            }

            {
                final String xml = xmlDOMParser.serialize(account);
                final Account accountFromXml = xmlDOMParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), accountFromXml.getFirstName());
                assertEquals(account, accountFromXml);
                assertEquals(account, xmlDOMParser.deserialize(IOUtil.stringToReader(xml), Account.class));
            }

            {
                final String xml = abacusXmlParser.serialize(account);
                final Account accountFromXml = abacusXmlParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), accountFromXml.getFirstName());
                assertEquals(account, accountFromXml);
                assertEquals(account, abacusXmlParser.deserialize(IOUtil.stringToReader(xml), Account.class));
            }

            {
                final String xml = abacusXMLSAXParser.serialize(account);
                final Account accountFromXml = abacusXMLSAXParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), accountFromXml.getFirstName());
                assertEquals(account, accountFromXml);
                assertEquals(account, abacusXMLSAXParser.deserialize(IOUtil.stringToReader(xml), Account.class));
            }

            {
                final String xml = abacusXMLStAXParser.serialize(account);
                final Account accountFromXml = abacusXMLStAXParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), accountFromXml.getFirstName());
                assertEquals(account, accountFromXml);
                assertEquals(account, abacusXMLStAXParser.deserialize(IOUtil.stringToReader(xml), Account.class));
            }

            {
                final String xml = abacusXMLDOMParser.serialize(account);
                final Account accountFromXml = abacusXMLDOMParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), accountFromXml.getFirstName());
                assertEquals(account, accountFromXml);
                assertEquals(account, abacusXMLDOMParser.deserialize(IOUtil.stringToReader(xml), Account.class));
            }

            {
                final String xml = kryoParser.serialize(account);
                final Account accountFromXml = kryoParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), accountFromXml.getFirstName());
                assertEquals(account, accountFromXml);
                assertEquals(account, kryoParser.deserialize(IOUtil.stringToReader(xml), Account.class));
            }

        }
    }

    @Test
    public void test_ignoreProp() {
        final Account account = createAccount(Account.class);

        final String json = N.toJson(account, JSC.create().setIgnoredPropNames(N.asSet("firstName")));

        N.println(json);

        final Account account2 = N.fromJson(json, JDC.create().setIgnoredPropNames(N.asSet("lastName")), Account.class);

        N.println(account2);
    }

    public enum LongEnum {
        ONE(1), TWO(2);

        private final long order;

        LongEnum(final int order) {
            this.order = order;
        }

        @JsonXmlValue
        public long order() {
            return order;
        }

        @JsonXmlCreator
        public static LongEnum from(final long num) {
            return num == 1 ? ONE : TWO;
        }
    }

    public static class SingleValueObject {

        @JsonXmlValue
        private final String value;

        SingleValueObject(final String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @JsonXmlCreator
        public static SingleValueObject from(final String value) {
            return new SingleValueObject(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }
            final SingleValueObject other = (SingleValueObject) obj;
            if (!Objects.equals(value, other.value)) {
                return false;
            }
            return true;
        }
    }

    @Test
    public void test_jsonValue() {
        {
            final LongEnum[] a = { LongEnum.TWO, LongEnum.ONE };
            final String json = N.toJson(a);
            N.println(json);

            final LongEnum[] b = N.fromJson(json, LongEnum[].class);

            assertTrue(N.equals(a, b));
        }

        {
            final SingleValueObject[] a = { SingleValueObject.from("abc"), SingleValueObject.from("123") };
            final String json = N.toJson(a);
            N.println(json);

            final SingleValueObject[] b = N.fromJson(json, SingleValueObject[].class);

            assertTrue(N.equals(a, b));
        }
    }

    @Test
    public void test_2951() {
        final String data = "{\"field1\": null, \"field2\": null, \"field3\": \"1\", \"field4\": null}";

        final Model model = JSON.parseObject(data, Model.class);

        assertEquals(model.field1, 0);
        assertEquals(model.field2, 0F);
        assertEquals(model.field3, "1");
        assertNull(model.field4);
        assertEquals(model.field5, 10000);
    }

    public static class Model {
        public final int field1;
        public final float field2;
        public final String field3;
        public final List<String> field4;
        public int field5 = 0;

        public Model(final int field1, final float field2, final String field3, final List<String> field4) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
            this.field4 = field4;
            field5 = 10000;
        }
    }

    public static class TestBean {
        public int is_subscribe = 0;
        public int subscribe = 0;
        public int isHave = 0;
    }

    @Test
    public void test_3083() {
        final String s = "{'is_subscribe':1,'subscribe':3,'isHave':5}";
        final TestBean b = JSON.parseObject(s, TestBean.class);
        println(b.is_subscribe + "--" + b.subscribe + "--" + b.isHave);

        N.println(N.stringOf(N.fromJson(s, TestBean.class)));
    }

    @Test
    public void test_3174() {
        final TestDemo demo = new TestDemo();
        demo.setName("测试");
        demo.setNum(1);
        demo.setIsFlag(true);
        final List<TestDemo.Demo> demoList = new ArrayList<>();
        demoList.add(new TestDemo.Demo("张三", "15566667777"));
        demoList.add(new TestDemo.Demo("李四", "15577778888"));
        demoList.add(new TestDemo.Demo("王五", "15588889999"));
        demo.setDemoList(demoList);
        final Map<String, List<TestDemo.Demo>> map = new HashMap<>();
        map.put("11", demoList);
        demo.setMap(map);

        N.println(JSON.toJSONString(demo));
        N.println(N.toJson(demo));
    }

    @Data
    public static class TestDemo {

        private String name;

        private Integer num;

        private Boolean isFlag;

        private List<TestDemo.Demo> demoList;

        private Map<String, List<TestDemo.Demo>> map;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Demo {
            private String user;
            private String phone;
        }
    }

    static class Parent {
        private String _status;

        public String get_status() {
            return _status;
        }

        public void set_status(final String _status) {
            this._status = _status;
        }
    }

    static class Son extends Parent {

        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(final String status) {
            this.status = status;
        }
    }

    static final Gson gson = new Gson().newBuilder().create();

    @Test
    public void test_stream() throws IOException {
        final List<Account> accountList = createAccountList(Account.class, 10);
        accountList.add(accountList.size() / 2, null);
        accountList.add(0, null);

        String json = jsonParser.serialize(accountList);

        N.println(json);

        final String str = jsonParser.stream(json, Type.of(Account.class)).skip(2).filter(it -> it.getId() >= 0).map(Account::getId).limit(3).join(", ");

        N.println(str);

        assertEquals("0, 0, 0", str);

        assertEquals(accountList.size(), jsonParser.stream(json, Type.of(Account.class)).count());
        assertEquals(accountList, jsonParser.stream(json, Type.of(Account.class)).toList());

        json = "[ \n ]";

        assertEquals(0, jsonParser.stream(json, Type.of(Account.class)).count());
    }

    @SuperBuilder
    @Data
    @NoArgsConstructor
    public static class BeanF {
        private long id;
        private String gui;
        @JsonXmlField(isJsonRawValue = true)
        private String json;
        @JsonRawValue
        @JsonXmlField(isJsonRawValue = true)
        private List<Map<String, Date>> json2;
    }

    @Test
    public void test_jsonXmlConfig() throws JsonProcessingException {
        java.sql.Date currentDate = Dates.currentDate();

        String gui = "271e92c2-a3f1-40ef-af81-18e1bbcef490";

        BeanF bean = BeanF.builder()
                .id(1001)
                .gui(gui)
                .json(N.toJson(N.asMap("key1", 2, "key2", Dates.currentCalendar(), "key3", Dates.currentDate()),
                        JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)))
                .json2(N.asList(N.asMap("aa", currentDate)))
                .build();

        String json = N.toJson(bean);

        N.println(json);

        N.println(objMapper.writeValueAsString(bean));

        BeanF bean2 = N.fromJson(json, BeanF.class);

        N.println(bean);
        N.println(bean2);

        assertNotSame(bean, bean2);

        bean2.setGui(bean.getGui());
        N.println(bean2);

        assertEquals(bean, bean2);

        bean = BeanF.builder()
                .id(1001)
                .gui(gui)
                .json(N.toJson(N.asMap("key1", 2)).replace(": 2}", ": 2  }  ").replace("[{\"", " [  {   \"").replace(", ", ",    "))
                .json2(N.asList(N.asMap("aa", currentDate)))
                .build();

        json = N.toJson(bean);

        N.println(json);

        final String expectedValue = "{\"id\": 1001, \"gui\": \"271e92c2-a3f1-40ef-af81-18e1bbcef490\", \"json\": \"{\\\"key1\\\": 2  }  \", \"json2\": \"[{\\\"aa\\\": "
                + currentDate.getTime() + "}]\"}";
        N.println(expectedValue);

        assertEquals(expectedValue, json);

        bean2 = N.fromJson(json, BeanF.class);

        N.println(bean);
        N.println(bean2);

        assertNotSame(bean, bean2);

    }

    @SuperBuilder
    @Data
    @Accessors(fluent = true)
    static class BeanC {
        private int id;
    }

    @SuperBuilder
    @Data
    @Accessors(fluent = true)
    static class BeanD extends BeanC {
        private String name;
    }

    @Data
    static class Pojo {
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS")
        private Timestamp timestamp;
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS")
        private LocalDateTime localDateTime;
    }

    @Test
    public void test_writeBigDecimalAsPlain() {
        final BigDecimal[] a = { BigDecimal.valueOf(0.0000000005), BigDecimal.valueOf(55000000000000000.0000000005) };

        String json = N.toJson(a);
        N.println(json);
        assertEquals("[5.0E-10, 5.5E+16]", json);

        json = N.toJson(a, JSC.create().writeBigDecimalAsPlain(true));
        N.println(N.fromJson(json, BigDecimal[].class));
        N.println(json);
        assertEquals("[0.00000000050, 55000000000000000]", json);
    }

    @Test
    public void test_2894() {

        final String json = "{\"timestamp\": \"2019-09-19 08:49:52.350000000\", " + "\"local_date_time\": \"2019-09-19 08:49:52.350000000\"}";

        N.println(N.fromJson(json, Pojo.class));
    }

    @Test
    public void test_2925() {
        final ZonedDateTime now = ZonedDateTime.now();
        final Map<String, Object> map = N.asLinkedHashMap("ZonedDateTime", now);

        ZonedDateTime.parse(now.toString());

        final String json = N.toJson(map, JSC.create().setDateTimeFormat(null));
        N.println(json);

        N.println(N.fromJson(json, new TypeReference<Map<String, ZonedDateTime>>() {
        }.type()));

        final String json2 = "{\"ZonedDateTime\":\"2019-12-05T22:28:22.867Z\"}\r\n" + "";

        N.println(N.fromJson(json2, new TypeReference<Map<String, ZonedDateTime>>() {
        }.type()));

        N.println(N.toJson(map, JSC.create().setStringQuotation('\"').prettyFormat(true)));
    }

    @Test
    public void test_2901() {
        final Map<String, Object> map = N.asLinkedHashMap("abc", TimeUnit.HOURS);

        N.println(TimeUnit.HOURS.getDeclaringClass());
        N.println(TimeUnit.HOURS.getClass().isEnum());
        N.println(Enum.class.isAssignableFrom(TimeUnit.HOURS.getClass()));
        N.println(TimeUnit.HOURS.getDeclaringClass().isEnum());

        N.println(JSON.toJSONString(map));
        N.println(N.toJson(map));

        final Type<Map<String, TimeUnit>> type = new TypeReference<Map<String, TimeUnit>>() {
        }.type();
        final Map<String, TimeUnit> map2 = N.fromJson(N.toJson(map), type);

        assertEquals(map, map2);

        N.println(N.toJson(map, JSC.create().setStringQuotation('\"').prettyFormat(true)));
    }

    @Test
    public void test_01() {
        final BeanD bean = BeanD.builder().id(123).name("abc").build();
        N.println(N.toJson(bean));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    static class PublicBean {
        public ObjectId _id;
        public long id;
        @JsonXmlField(name = "fullName")
        public String name;
        private String lastName;
        @JsonXmlField(dateFormat = "dd/MM/yyyy")
        private Date birthDate;

        public String getLastName() {
            return lastName;
        }

        public void setLastName(final String lastName) {
            this.lastName = lastName;
        }

        public Date getBirthDate() {
            return birthDate;
        }

        public void setBirthDate(final Date birthDate) {
            this.birthDate = birthDate;
        }
    }

    @Test
    public void test_publicBean() {
        final ObjectId _id = new ObjectId("5dbf26f6970f81156e673659");
        final Date birthDate = Dates.parseJUDate("1979-01-01");
        final PublicBean bean = new PublicBean();
        bean._id = _id;
        bean.id = 123;
        bean.name = "fullName";
        bean.setLastName("lastName");
        bean.setBirthDate(birthDate);
        N.println(bean);

        for (final NamingPolicy np : NamingPolicy.values()) {
            N.println(np);
            final String json = N.toJson(bean, JSC.create().setPropNamingPolicy(np));
            N.println(json);

            assertEquals(bean, N.fromJson(json, PublicBean.class));
        }

        for (final NamingPolicy np : NamingPolicy.values()) {
            final String xml = N.toXml(bean, XSC.create().setPropNamingPolicy(np));
            N.println(xml);

            assertEquals(bean, N.fromXml(xml, PublicBean.class));
        }

        final String json = N.toJson(bean);
        N.println(json);
        assertEquals(
                "{\"_id\": \"5dbf26f6970f81156e673659\", \"id\": 123, \"fullName\": \"fullName\", \"lastName\": \"lastName\", \"birthDate\": \"01/01/1979\"}",
                json);

        Object bean2 = N.fromJson(json, PublicBean.class);
        assertEquals(bean, bean2);

        final String json2 = "{\"_id\": \"5dbf26f6970f81156e673659\", \"id\": 123, \"name\": \"fullName\", \"lastName\": \"lastName\", \"birthDate\": \"01/01/1979\"}";
        bean2 = N.fromJson(json2, PublicBean.class);
        assertEquals(bean, bean2);

        final String xml = N.toXml(bean);
        N.println(xml);
        assertEquals(
                "<publicBean><_id>5dbf26f6970f81156e673659</_id><id>123</id><fullName>fullName</fullName><lastName>lastName</lastName><birthDate>01/01/1979</birthDate></publicBean>",
                xml);

        bean2 = N.fromXml(xml, PublicBean.class);
        assertEquals(bean, bean2);

        final String xml2 = "<publicBean><_id>5dbf26f6970f81156e673659</_id><id>123</id><name>fullName</name><lastName>lastName</lastName><birthDate>01/01/1979</birthDate></publicBean>";

        bean2 = N.fromXml(xml2, PublicBean.class);
        assertEquals(bean, bean2);

        N.println(Beans.beanToMap(bean2));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class BeanA1 {
        private ObjectId _id;
        private long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class BeanA2 {
        private ObjectId _id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class BeanA3 {
        private long id;
        private String name;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    static class BeanA4 {
        private ObjectId _id;
        private String name;

        public ObjectId getId() {
            return _id;
        }

        public void setId(final ObjectId _id) {
            this._id = _id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    static class BeanA5 {
        private ObjectId _id;
        private long id;
        private String name;

        public ObjectId getId() {
            return _id;
        }

        public void setId(final ObjectId _id) {
            this._id = _id;
        }

        public long get_id() {
            return id;
        }

        public void set_id(final long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public void test_propName() {
        final ObjectId _id = new ObjectId("5dbf26f6970f81156e673659");
        final BeanA1 bean1 = BeanA1.builder().id(123)._id(_id).name("test").build();
        N.println(bean1);

        String json = N.toJson(bean1);
        N.println(json);
        assertEquals("{\"_id\": \"5dbf26f6970f81156e673659\", \"id\": 123, \"name\": \"test\"}", json);

        Object bean = N.fromJson(json, BeanA1.class);
        assertEquals(bean1, bean);

        final BeanA2 bean2 = BeanA2.builder()._id(_id).name("test").build();
        N.println(bean2);

        json = N.toJson(bean2);
        N.println(json);
        assertEquals("{\"_id\": \"5dbf26f6970f81156e673659\", \"name\": \"test\"}", json);

        bean = N.fromJson(json, BeanA2.class);
        assertEquals(bean2, bean);

        final BeanA3 bean3 = BeanA3.builder().id(123).name("test").build();
        N.println(bean3);

        json = N.toJson(bean3);
        N.println(json);
        assertEquals("{\"id\": 123, \"name\": \"test\"}", json);

        bean = N.fromJson(json, BeanA3.class);
        assertEquals(bean3, bean);

        final BeanA4 bean4 = new BeanA4();
        bean4.setId(_id);
        bean4.setName("test");
        N.println(bean4);

        json = N.toJson(bean4);
        N.println(json);
        assertEquals("{\"_id\": \"5dbf26f6970f81156e673659\", \"name\": \"test\"}", json);

        bean = N.fromJson(json, BeanA4.class);
        assertEquals(bean4, bean);

        final BeanA5 bean5 = new BeanA5();
        bean5.setId(_id);
        bean5.set_id(123);
        bean5.setName("test");
        N.println(bean5);

        json = N.toJson(bean5);
        N.println(json);
        assertEquals("{\"_id\": \"5dbf26f6970f81156e673659\", \"name\": \"test\", \"id\": 123}", json);

        bean = N.fromJson(json, BeanA5.class);
        assertTrue(N.equals(bean5, bean));
    }

    @Test
    public void test_single() {
        N.println(N.toJson("abc"));
        N.println(N.toJson(123));
        N.println(N.toJson(Dates.currentCalendar()));

        N.println(N.fromJson(N.toJson("abc"), String.class));
        N.println(N.fromJson(N.toJson(123), int.class));
        N.println(N.fromJson(N.toJson(Dates.currentCalendar()), Calendar.class));
    }

    @Test
    public void test_2716() {
        final String json = "{\"roleName\": \"ullamcodolo3reest\",\"state\": 1.1,\"roleDetail\": \"ut ex\"}";

        final Role role2 = JSON.parseObject(json, Role.class);
        N.println(role2);

        N.fromJson(json, Role.class);

        role2.set_returnGoodsDOS("kkkdddkjie");

        N.println(Beans.getPropNameList(Role.class));

        N.println(N.toJson(role2, JSC.create().setPropNamingPolicy(NamingPolicy.CAMEL_CASE)));
    }

    @Data
    static class Role {
        private String roleDetail;
        private String roleName;
        private int state;
        private String _returnGoodsDOS = "returnGoodsDOS";
    }

    @Test
    public void test_null_to_empty() {
        final String[] strs = { "abc", null, "", "null" };
        String json = N.toJson(strs);
        N.println(json);

        String[] strs2 = N.fromJson(json, JDC.create().readNullToEmpty(true), String[].class);
        N.println(strs2);

        strs2 = N.fromJson(json, JDC.create().readNullToEmpty(true).ignoreNullOrEmpty(true), String[].class);
        N.println(strs2);

        strs2 = N.fromJson(json, JDC.create().ignoreNullOrEmpty(true), String[].class);
        N.println(strs2);

        final Map<String, String> map = N.asMap("abc", "abc", null, "nullKey", "nullValue", null);

        json = N.toJson(map);
        N.println(json);

        Map<String, String> map2 = N.fromJson(json, Clazz.ofMap(String.class, String.class));
        N.println(map2);

        map2 = N.fromJson(json, JDC.create().readNullToEmpty(true), Type.ofMap(String.class, String.class));
        N.println(map2);

        map2 = N.fromJson(json, JDC.create().ignoreNullOrEmpty(true), Type.ofMap(String.class, String.class));
        N.println(map2);
    }

    @Test
    public void test_config_copy() {
        final JsonDeserializationConfig config = JDC.create()
                .ignoreNullOrEmpty(true)
                .setElementType(String.class)
                .setElementType(Type.of("List<String>"))
                .setIgnoredPropNames(N.asSet("abc", "123"));
        final JsonDeserializationConfig copy = config.copy();
        assertEquals(config, copy);
    }

    @Data
    static class B<T> {
        private List<T> list;
        private List<T>[] arrayList;
        private List<int[]> intArrayList;
        private Map<List<List<String>[]>, List<List<String[]>[]>> map;
    }

    @Data
    static class GsonDouble {

        private double negInf = Double.NEGATIVE_INFINITY;
        private double posInf = Double.POSITIVE_INFINITY;
        private double notANumber = Double.NaN;
        private double number = 2.0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestGson {
        private String name;
        private StringBuilder value;
        private Date date;
        @JsonXmlField(dateFormat = "yyy/mm/dd hh:mm:ss")
        private Time time;

    }

    @Test
    public void test_97() {

        final int cnt = 3;
        final List<TestGson> list = new ArrayList<>(cnt);

        for (int x = 0; x < cnt; x++) {
            list.add(new TestGson("name" + x, new StringBuilder("value" + x), new Date(), Dates.currentTime()));
        }

        String json = gson.toJson(list);

        final java.lang.reflect.Type collectionType = new TypeToken<ArrayList<TestGson>>() {
        }.getType();

        List<TestGson> list2 = gson.fromJson(json, collectionType);

        assertEquals(cnt, list2.size());

        json = N.toJson(list, JSC.create().setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME));
        N.println(json);

        list2 = N.fromJson(json, List.class);

        assertEquals(cnt, list2.size());
    }

    @Test
    public void test_96() {
        final int cnt = 10_000;
        final List<TestGson> list = new ArrayList<>(cnt);

        for (int x = 0; x < cnt; x++) {
            list.add(new TestGson("name" + x, new StringBuilder("value" + x), new Date(), Dates.currentTime()));
        }

        String json = gson.toJson(list);

        final java.lang.reflect.Type collectionType = new TypeToken<ArrayList<TestGson>>() {
        }.getType();

        List<TestGson> list2 = gson.fromJson(json, collectionType);

        assertEquals(cnt, list2.size());

        json = N.toJson(list);

        list2 = N.fromJson(json, List.class);

        assertEquals(cnt, list2.size());
    }

    @Test
    public void test_81() {

        String json = N.toJson(new GsonDouble());
        N.println(json);

        N.println(N.fromJson(json, GsonDouble.class));

        final Gson gson = new Gson().newBuilder().serializeSpecialFloatingPointValues().create();

        json = gson.toJson(new GsonDouble());
        System.out.println(json);
        final GsonDouble gsonDouble = gson.fromJson(json, GsonDouble.class);

        N.println(gsonDouble);
    }

    @Test
    public void test_35() {
        final BigDecimal x = new BigDecimal("1.00");
        String json = gson.toJson(x);
        BigDecimal y = gson.fromJson(json, BigDecimal.class);
        assertEquals(x, y);

        json = N.toJson(x);
        y = N.fromJson(json, BigDecimal.class);
        assertEquals(x, y);

        N.println(gson.fromJson("1.234567899E8", Double.class));
        N.println(gson.fromJson("1.234567899E8", BigDecimal.class));

        N.println(N.fromJson("1.234567899E8", Double.class));
        N.println(N.fromJson("1.234567899E8", BigDecimal.class));

        final B<String> b = new B<>();
        b.setList(N.asList("a", "b"));
        b.setArrayList(N.asArray(N.asList("c", "d")));
        json = gson.toJson(b);
        N.println(json);

        N.println(N.toJson(b));

        final B<String> b2 = gson.fromJson(json, B.class);
        N.println(b2);
        N.println(N.fromJson(json, B.class));

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(B.class);

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            N.println(propInfo.type.name());
        }

        N.println(beanInfo.type);
    }

    @Test
    public void test_236() {
        final String json = "{\"name\": 、\" 你好\", \"code\": \"aa\", \"remark\": \"aa\"}";

        N.println(N.fromJson(json, LinkedHashMap.class));
    }

    enum WeekDays {
        @JsonXmlField(name = "Monday")
        MONDAY, @JsonXmlField(name = "Tuesday")
        TUESDAY, @JsonXmlField(name = "Wednesday")
        WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    @Test
    public void test_enum() {
        final WeekDays[] weekdays = { WeekDays.MONDAY, WeekDays.TUESDAY, WeekDays.FRIDAY, WeekDays.SUNDAY };

        final String json = N.toJson(weekdays);
        N.println(json);

        assertTrue("[\"Monday\", \"Tuesday\", \"FRIDAY\", \"SUNDAY\"]".equals(json));

        final WeekDays[] weekdays2 = N.fromJson(json, WeekDays[].class);
        N.println(weekdays2);

        assertTrue(N.equals(weekdays, weekdays2));
    }

    @Test
    public void test_1168() {
        final String json = "{ \"test2\":{}}";
        final TestModel testModel = new Gson().fromJson(json, TestModel.class);
        N.println("test: " + testModel.a + " " + testModel.test2.b);

        N.println(N.fromJson(json, TestModel.class));
    }

    @Data
    static class TestModel {

        public boolean a = true;
        public Test2 test2 = new Test2();

        @Data
        static class Test2 {
            public boolean b = true;
        }
    }

    @Data
    static class User {
        String name;
        List<String> interest;
    }

    @Test
    public void test_1164() {
        String json = """
                {\r
                  "user":[\r
                    {\r
                      "name": "\u2028 a",\r
                      "interest": [\r
                        "game",\r
                        "music"\r
                      ]\r
                    },\r
                    {\r
                      "name": "b",\r
                      "interest": ""\r
                    }\r
                  ]\r
                }""";

        Map<String, List<User>> user = N.fromJson(json, new TypeReference<Map<String, List<User>>>() {
        }.type());
        N.println(user);

        user = N.fromJson(json, new TypeReference.TypeToken<Map<String, List<User>>>() {
        }.type());
        N.println(user);

        try {
            user = gson.fromJson(json, new TypeToken<Map<String, List<User>>>() {
            }.getType());
            N.println(user);
            fail("Should throw JsonSyntaxException");
        } catch (final JsonSyntaxException e) {
        }

        json = """
                {\r
                  "user":[\r
                    {\r
                      "name": "a",\r
                      "interest": [\r
                        "game",\r
                        "music"\r
                      ]\r
                    },\r
                    {\r
                      "name": "b",\r
                      "interest": null\
                    }\r
                  ]\r
                }""";

        user = N.fromJson(json, new TypeReference<Map<String, List<User>>>() {
        }.type());
        N.println(user);

        user = N.fromJson(json, new TypeReference.TypeToken<Map<String, List<User>>>() {
        }.type());
        N.println(user);

        user = gson.fromJson(json, new TypeToken<Map<String, List<User>>>() {
        }.getType());
        N.println(user);
    }

    @Test
    public void test_ObjectId() {
        final MongoDoc doc = MongoDoc.builder().id(ObjectId.get()).name("abc").build();
        final String json = N.toJson(doc);
        N.println(json);
        MongoDoc doc2 = N.fromJson(json, MongoDoc.class);

        assertEquals(doc, doc2);

        final String json2 = json.replace("_id", "id");
        doc2 = N.fromJson(json2, MongoDoc.class);

        assertEquals(doc, doc2);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class MongoDoc {
        @JsonXmlField(name = "_id")
        private ObjectId id;
        private String name;
    }

    @Test
    public void test_read_string() {
        final String str = "a,b";

        assertTrue(N.equals(N.asArray("a", "b"), N.fromJson(str, String[].class)));
    }

    @Test
    public void test_empty_array_element() {
        String json = "[]";
        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, N.fromJson(json, String[].class)));
        json = "[,]";
        assertTrue(N.equals(N.asArray("", ""), N.fromJson(json, String[].class)));
        json = "[12,]";
        assertTrue(N.equals(N.asArray("12", ""), N.fromJson(json, String[].class)));
        json = "[,12]";
        assertTrue(N.equals(N.asArray("", "12"), N.fromJson(json, String[].class)));

        json = "";
        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, N.fromJson(json, String[].class)));
        json = ",";
        assertTrue(N.equals(N.asArray("", ""), N.fromJson(json, String[].class)));
        json = "12,";
        assertTrue(N.equals(N.asArray("12", ""), N.fromJson(json, String[].class)));
        json = ",12";
        assertTrue(N.equals(N.asArray("", "12"), N.fromJson(json, String[].class)));
    }

    @Test
    public void test_444() {
        N.println(Beans.getPropNameList(PersonVO.class));
        final String json = "{id:\"aa\",_id:\"bbb\"}";
        final PersonVO p = N.fromJson(json, PersonVO.class);
        System.out.println(p);

        assertEquals(new PersonVO("aa", "bbb"), p);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonVO {
        private String id;
        private String _id;
    }

    @Test
    public void test_333() {
        final Map<Object, Object> map = N.asMap(Integer.MIN_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MIN_VALUE, -Double.MIN_VALUE, "abc : \" , : }[[932]93', ", "abc : \" , : }[[932]93', ");
        final String json = N.toJson(map, JSC.create().quoteMapKey(false));
        N.println(json);
        final Map<Object, Object> map2 = N.fromJson(json, Map.class);

        N.println(map2.entrySet().iterator().next().getKey().getClass());
        N.println(map2.entrySet().iterator().next().getValue().getClass());

        assertEquals(map, map2);
        final String xml = abacusXmlParser.serialize(map, XSC.create().tagByPropertyName(true));
        N.println(xml);
    }

    @Test
    public void test_133() {
        final String json = "{60000000000149:610,60000000000171:900}";
        final Map<Object, Object> map = N.fromJson(json, Map.class);

        Stream.ofKeys(map).forEach(Fn.println());
    }

    @Test
    public void test_001() {
        final String json = "{2:\"1\",3:\"2\",4:\"wenshao\",32:\"0\",49:\"1\"}";

        N.println(N.fromJson(json, Map.class));
    }

    @Override
    protected Parser<?, ?> getParser() {
        return jsonParser;
    }

    @Test
    public void test_parse_string() {
        final Map<String, Integer> m = N.asMap("a", 1, "b", 2);
        String json = jsonParser.serialize(m);
        N.println(json);
        json = "abc===" + json;
        N.println(json);

        N.println(jsonParser.deserialize(json, 6, json.length(), Map.class));
    }

    @Test
    public void test_BeanInfo_setPropValue() {
        final Account account = createAccountWithContact(Account.class);
        final Map<String, Object> props = Beans.beanToFlatMap(account);
        N.println(props);

        final Account account2 = new Account();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(account2.getClass());
        for (final Map.Entry<String, Object> entry : props.entrySet()) {
            beanInfo.setPropValue(account2, entry.getKey(), entry.getValue());
        }

        assertEquals(account.getContact(), account2.getContact());
    }

    @Test
    public void test_bigAccountList() {
        final List<Account> accountList = createAccountList(Account.class, 1000);

        final File file = new File("./src/test/resources/tmp.json");
        jsonParser.serialize(accountList, file);

        final JsonDeserializationConfig jdc = JDC.create().setElementType(Account.class);
        final List<Account> accountList2 = jsonParser.deserialize(file, jdc, List.class);

        N.println(accountList2.size());
        N.println(accountList2.get(0));

        file.delete();
    }

    @Test
    public void test_bigString() {
        final char[] cbuf = Objectory.createCharArrayBuffer();
        final StringBuilder sb = Objectory.createStringBuilder();
        String bigStr = "";
        try {
            while (sb.length() < cbuf.length - 128) {
                sb.append(Strings.uuid());
                sb.append(WD._QUOTATION_D);
                sb.append(WD._QUOTATION_S);
            }

            bigStr = sb.toString();
        } finally {
            Objectory.recycle(cbuf);
            Objectory.recycle(sb);
        }

        final String[] array = N.asArray(bigStr, bigStr);

        String json = jsonParser.serialize(array);

        String[] array2 = jsonParser.deserialize(json, String[].class);
        assertTrue(N.equals(array, array2));

        array2 = jsonParser.deserialize(IOUtil.stringToReader(json), String[].class);
        assertTrue(N.equals(array, array2));

        final JsonSerializationConfig jsc = JSC.create().setStringQuotation(WD._QUOTATION_D);
        json = jsonParser.serialize(array, jsc);

        array2 = jsonParser.deserialize(json, String[].class);
        assertTrue(N.equals(array, array2));

        array2 = jsonParser.deserialize(IOUtil.stringToReader(json), String[].class);
        assertTrue(N.equals(array, array2));
    }

    @Test
    public void test_serializable() {
        String str = jsonParser.serialize("abc");
        N.println(str);
        assertEquals("abc", str);

        str = jsonParser.serialize(1);
        N.println(str);
        assertEquals("1", str);

        str = jsonParser.serialize(true);
        N.println(str);
        assertEquals("true", str);

        str = jsonParser.serialize('a');
        N.println(str);
        assertEquals("a", str);

        final File file = new File("./src/test/resources/tmp.json");

        jsonParser.serialize("abc", file);
        N.println(str);
        assertEquals("abc", IOUtil.readAllToString(file));

        jsonParser.serialize(1, file);
        N.println(str);
        assertEquals("1", IOUtil.readAllToString(file));

        jsonParser.serialize(true, file);
        N.println(str);
        assertEquals("true", IOUtil.readAllToString(file));

        jsonParser.serialize('a', file);
        N.println(str);
        assertEquals("a", IOUtil.readAllToString(file));

        file.delete();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        jsonParser.serialize("abc", os);
        N.println(str);
        assertEquals("abc", new String(os.toByteArray()));

        os = new ByteArrayOutputStream();
        jsonParser.serialize(1, os);
        N.println(str);
        assertEquals("1", new String(os.toByteArray()));

        os = new ByteArrayOutputStream();
        jsonParser.serialize(true, os);
        N.println(str);
        assertEquals("true", new String(os.toByteArray()));

        os = new ByteArrayOutputStream();
        jsonParser.serialize('a', os);
        N.println(str);
        assertEquals("a", new String(os.toByteArray()));

        StringWriter writer = new StringWriter();
        jsonParser.serialize("abc", writer);
        N.println(str);
        assertEquals("abc", writer.toString());

        writer = new StringWriter();
        jsonParser.serialize(1, writer);
        N.println(str);
        assertEquals("1", writer.toString());

        writer = new StringWriter();
        jsonParser.serialize(true, writer);
        N.println(str);
        assertEquals("true", writer.toString());

        writer = new StringWriter();
        jsonParser.serialize('a', writer);
        N.println(str);
        assertEquals("a", writer.toString());

    }

    @Test
    public void test_prettyFormat_2() {
        final Account account = createAccount(Account.class);

        final GenericEntity genericBean = new GenericEntity();
        genericBean.setBooleanList(N.asList(true, false));
        genericBean.setCharList(N.asList('a', 'b', '好'));
        genericBean.setIntList(N.asList(1, 2, 3));
        genericBean.setStringList(N.asList("abc", "123"));
        genericBean.setAccountList(N.asList(account));
        final Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        final JsonSerializationConfig jsc = JSC.create().prettyFormat(true);
        final String str = jsonParser.serialize(genericBean, jsc);

        N.println(str);

        final GenericEntity genericBean2 = jsonParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
    }

    @Test
    public void test_null() {
        final String nullElement = null;

        final String[] array = N.asArray(nullElement);
        String str = jsonParser.serialize(array);
        N.println(str);

        final String[] array2 = jsonParser.deserialize(str, String[].class);
        assertTrue(N.equals(array, array2));

        final List<String> list = N.asList(nullElement);
        str = jsonParser.serialize(list);
        N.println(str);

        final List<String> list2 = jsonParser.deserialize(str, List.class);
        assertTrue(N.equals(list, list2));

        final Map<String, Object> map = N.asMap(nullElement, nullElement);
        final JsonSerializationConfig jsc = JSC.create().setExclusion(Exclusion.NONE);
        str = jsonParser.serialize(map, jsc);
        N.println(str);

        final Map<String, Object> map2 = jsonParser.deserialize(str, Map.class);
        N.println(map2);

        jsonParser.deserialize(str, Object.class);

        jsonParser.readString(str, Object.class);
    }

    @Test
    public void test_null_1() {
        final String nullElement = null;
        final String str = jsonParser.serialize(nullElement);
        assertNull(jsonParser.deserialize(str, String.class));

        assertEquals(0, jsonParser.readString(nullElement, int.class).intValue());

        final List<String> list = new ArrayList<>();
        jsonParser.readString(nullElement, list);
        assertEquals(0, list.size());

        final Map<String, Object> map = new HashMap<>();
        jsonParser.readString(nullElement, map);
        assertEquals(0, map.size());
    }

    @Test
    public void test_null_2() {
        final Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        final String nullElement = null;

        final Object[] array = { account, nullElement };
        String str = jsonParser.serialize(array);
        N.println(str);

        final Object[] array2 = jsonParser.deserialize(str, JDC.create().setElementType(Account.class), Object[].class);
        assertTrue(N.equals(array, array2));

        final List<?> list = N.asList(account, nullElement);
        str = jsonParser.serialize(list);
        N.println(str);

        final List<String> list2 = jsonParser.deserialize(str, JDC.create().setElementType(Account.class), List.class);
        assertTrue(N.equals(list, list2));
        N.println(list2);

        final Map<String, Object> map = N.asMap(nullElement, account);
        final JsonSerializationConfig jsc = JSC.create().setExclusion(Exclusion.NONE);
        str = jsonParser.serialize(map, jsc);
        N.println(str);

        final Map<String, Object> map2 = jsonParser.deserialize(str, Map.class);
        N.println(map2);
    }

    @Test
    public void test_null_3() {
        final Account account = new Account();

        String str = jsonParser.serialize(account);

        account.setId(0);
        account.setFirstName("firstName");
        account.setLastName(null);

        str = jsonParser.serialize(account, JSC.of(Exclusion.DEFAULT, null));
        N.println(str);

        str = jsonParser.serialize(Beans.beanToMap(account), JSC.of(Exclusion.DEFAULT, null));
        N.println(str);

        final Map<String, Object> map = Beans.beanToMap(account);
        map.put("lastName", null);
        map.put("account", account);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.asSet("id"));

        JsonSerializationConfig jsc = JSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = jsonParser.serialize(map, jsc);
        N.println(str);

        final Map<String, Object> map2 = jsonParser.deserialize(str, JDC.create().setElementType(Account.class), Map.class);
        N.println(map2);

        str = jsonParser.serialize(N.asList(map), jsc);
        N.println(str);

        final JsonDeserializationConfig xdc = JDC.create().setElementType(Map.class);
        final List<?> list = jsonParser.deserialize(str, xdc, List.class);
        N.println(list);

        final Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.asList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));

        jsc = JSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = jsonParser.serialize(map3, jsc);
        N.println(str);

        N.println(jsonParser.deserialize(str, JDC.create().setElementType(Account.class), Map.class));

        jsc = JSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = jsonParser.serialize(map3, jsc);
        N.println(str);

        final XBean xBean = createXBean();
        str = jsonParser.serialize(xBean, jsc);
        N.println(str);

        N.println(jsonParser.deserialize(str, XBean.class));
    }

    @Test
    public void test_config() {
        final JsonSerializationConfig jsc1 = JSC.create();
        final JsonSerializationConfig jsc2 = JSC.create();

        N.println(jsc1);

        assertTrue(N.asSet(jsc1).contains(jsc2));

        final JsonDeserializationConfig jdc1 = JDC.create();
        final JsonDeserializationConfig jdc2 = JDC.create();

        N.println(jdc1);

        assertTrue(N.asSet(jdc1).contains(jdc2));
    }

    @Test
    public void test_config_2() {
        final JsonSerializationConfig jsc1 = new JsonSerializationConfig();
        final JsonSerializationConfig jsc2 = new JsonSerializationConfig();

        N.println(jsc1);

        assertTrue(N.asSet(jsc1).contains(jsc2));

        final XmlDeserializationConfig jdc1 = new XmlDeserializationConfig();
        final XmlDeserializationConfig jdc2 = new XmlDeserializationConfig();

        N.println(jdc1);

        assertTrue(N.asSet(jdc1).contains(jdc2));
    }

    @Test
    public void test_transient() {
        final TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");

        String str = jsonParser.serialize(bean);

        N.println(str);

        assertTrue(str.indexOf("abc") == -1);

        final JsonSerializationConfig config = JSC.create().skipTransientField(false);
        str = jsonParser.serialize(bean, config);

        N.println(str);

        assertTrue(str.indexOf("abc") >= 0);

        assertTrue(bean.equals(jsonParser.deserialize(str, TransientBean.class)));
    }

    @Test
    public void test_wrapRootValue() {
        final Account account = createAccount(Account.class);

        JsonSerializationConfig config = JSC.create().wrapRootValue(true);
        String str = jsonParser.serialize(account, config);
        N.println("========================================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        config = JSC.create().quotePropName(true).wrapRootValue(true).prettyFormat(true);
        str = jsonParser.serialize(account, config);
        N.println("========================================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");
    }

    @Test
    public void test_encloseRootValue() {
        final Account account = createAccount(Account.class);

        final JsonSerializationConfig config = JSC.create().bracketRootValue(false).setStringQuotation(WD.CHAR_ZERO).setCharQuotation(WD.CHAR_ZERO);
        String str = jsonParser.serialize(account, config);
        N.println(str);

        str = jsonParser.serialize(N.asList("abc", "123"), config);
        N.println(str);
        assertTrue(N.equals("abc, 123", str));

        str = jsonParser.serialize(N.asArray("abc", "123"), config);
        N.println(str);
        assertTrue(N.equals("abc, 123", str));

        str = jsonParser.serialize(Array.of(false, true), config);
        N.println(str);
        assertTrue(N.equals("false, true", str));

        str = jsonParser.serialize(Array.of(1, 2, 3), config);
        N.println(str);
        assertTrue(N.equals("1, 2, 3", str));

        str = jsonParser.serialize(Array.of('a', 'b', '1', '2', '你'), config);
        N.println(str);
        assertTrue(N.equals("a, b, 1, 2, 你", str));

        str = jsonParser.serialize(new Object[] { Array.of('a', 'b'), Array.of('1', '2') }, config);
        N.println(str);
        assertTrue(N.equals("[a, b], [1, 2]", str));
    }

    @Test
    public void test_typeBean() {
        TypeBean typeBean = UnitTestUtil.createBean(TypeBean.class);
        final String json = jsonParser.serialize(Beans.beanToFlatMap(typeBean));
        N.println(json);

        typeBean = jsonParser.deserialize(json, TypeBean.class);

        String xml = jsonParser.serialize(typeBean);
        N.println(xml);

        typeBean = new TypeBean();
        typeBean.setStringType("aa\n\t");

        xml = jsonParser.serialize(typeBean);
        N.println(xml);

        final TypeBean typeBean2 = jsonParser.deserialize(xml, TypeBean.class);
        assertEquals(typeBean.getStringType(), typeBean2.getStringType());

    }

    @Test
    public void test_writeString() {
        String st = "\"\'feajwiefle'''''afjeia";
        final String result = jsonParser.serialize(st);
        N.println(result);

        st = "\"\'feajwiefle'''''afjeia";

        final BufferedWriter writer = Objectory.createBufferedWriter();
        jsonParser.serialize(st, writer);
        assertEquals(result, writer.toString());
        Objectory.recycle(writer);
    }

    @Test
    public void test_readString() {
        String st = "aksfjei, null, null  ,  ,  \"null\",  askejiow,askjei, ";
        List<String> c = jsonParser.readString(st, List.class);

        N.println(c);

        for (final String e : c) {
            N.println(e);
        }

        assertNull(c.get(1));
        assertTrue(c.get(3).length() == 0);
        assertTrue(NULL_STRING.equals(c.get(4)));

        st = "aks\\\"fjei, null, null  ,  ,  askejiow,askjei, ";
        c = jsonParser.readString(st, List.class);

        for (final String e : c) {
            N.println(e);
        }

        st = "aks\\\"fjei, null, null  ,  ,  ask\\nejiow,askjei, aa\\u4231, dakslf\\nasfe\\tadfe\\bad s\\rae fe aer \\fsfwe";
        c = jsonParser.readString(st, List.class);

        for (final String e : c) {
            N.println(e);
        }
    }

    @Test
    public void test_readString_2() {
        String str = jsonParser.serialize("abc");
        N.println(str);
        assertTrue(N.equals("abc", jsonParser.readString(str, String.class)));

        final Date date = Dates.currentDate();
        str = jsonParser.serialize(date);
        N.println(str);
        assertTrue(N.equals(N.stringOf(date), N.stringOf(jsonParser.readString(str, Date.class))));

        str = jsonParser.serialize(Array.of(1, 2, 3));
        N.println(str);

        assertTrue(N.equals(Array.of(1, 2, 3), jsonParser.readString(str, int[].class)));
        assertTrue(N.equals(Array.of(1, 2, 3), jsonParser.deserialize(str, int[].class)));

        str = jsonParser.serialize(Array.of('a', 'b', 'c'));
        N.println(str);

        assertTrue(N.equals(Array.of('a', 'b', 'c'), jsonParser.readString(str, char[].class)));
        assertTrue(N.equals(Array.of('a', 'b', 'c'), jsonParser.deserialize(str, char[].class)));

        str = jsonParser.serialize(N.asArray("a", "b", "c"));
        N.println(str);

        assertTrue(N.equals(N.asArray("a", "b", "c"), jsonParser.readString(str, String[].class)));
        assertTrue(N.equals(N.asArray("a", "b", "c"), jsonParser.deserialize(str, String[].class)));

        assertTrue(N.equals(N.asList('a', 'b', 'c'), jsonParser.readString(str, JDC.create().setElementType(char.class), List.class)));
        assertTrue(N.equals(N.asList('a', 'b', 'c'), jsonParser.deserialize(str, JDC.create().setElementType(char.class), List.class)));

        str = jsonParser.serialize(N.asArray("a", "b", "c"));
        N.println(str);

        assertTrue(N.equals(N.asList("a", "b", "c"), jsonParser.readString(str, List.class)));
        assertTrue(N.equals(N.asList("a", "b", "c"), jsonParser.deserialize(str, List.class)));
        final List<String> list = new ArrayList<>();
        jsonParser.readString(str, list);

        assertTrue(N.equals(N.asList("a", "b", "c"), list));

        final Map<String, Object> map = N.asMap("abc", 123);

        str = jsonParser.serialize(map);

        map.clear();
        jsonParser.readString(str, map);

        N.println(map);
    }

    @Test
    public void test_readString_3() {
        final EntityId entityId = Seid.of(AccountPNL.ID, 123);

        final JsonSerializationConfig jsc = JSC.create().bracketRootValue(false);
        String str = jsonParser.serialize(entityId, jsc);
        N.println(str);

        final EntityId entityId2 = jsonParser.readString(str, EntityId.class);
        assertEquals(entityId, entityId2);

        final String[] array = N.asArray("abc", "123");
        str = jsonParser.serialize(array, jsc);
        N.println(str);

        final String[] array2 = jsonParser.readString(str, String[].class);
        assertTrue(N.equals(array, array2));

        final List<String> list = N.asList("abc", "123");
        str = jsonParser.serialize(list, jsc);
        N.println(str);

        final List<String> list2 = jsonParser.readString(str, JDC.create().setElementType(String.class), List.class);
        assertTrue(N.equals(list, list2));

        final Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        account.setGUI(Strings.uuid());

        str = jsonParser.serialize(account, jsc);
        N.println(str);

        final Account account2 = jsonParser.readString(str, Account.class);
        assertTrue(N.equals(account, account2));

        final Map<String, Object> props = Beans.beanToMap(account);
        str = jsonParser.serialize(props, jsc);
        N.println(str);

        jsonParser.readString(str, Map.class);

        final Dataset dataset = N.newDataset(N.asList(account));
        str = jsonParser.serialize(dataset, jsc);
        N.println(str);

        jsonParser.readString(str, Dataset.class);
    }

    @Test
    public void test_readString_4() {
        final EntityId entityId = Seid.of(AccountPNL.ID, 123);

        final JsonSerializationConfig jsc = JSC.create().bracketRootValue(true);
        String str = jsonParser.serialize(entityId, jsc);
        N.println(str);

        final EntityId entityId2 = jsonParser.readString(str, EntityId.class);
        assertEquals(entityId, entityId2);

        final String[] array = N.asArray("abc", "123");
        str = jsonParser.serialize(array, jsc);
        N.println(str);

        final String[] array2 = jsonParser.readString(str, String[].class);
        assertTrue(N.equals(array, array2));

        final List<String> list = N.asList("abc", "123");
        str = jsonParser.serialize(list, jsc);
        N.println(str);

        final List<String> list2 = jsonParser.readString(str, JDC.create().setElementType(String.class), List.class);
        assertTrue(N.equals(list, list2));

        final Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        account.setGUI(Strings.uuid());

        str = jsonParser.serialize(account, jsc);
        N.println(str);

        final Account account2 = jsonParser.readString(str, Account.class);
        assertTrue(N.equals(account, account2));

        final Map<String, Object> props = Beans.beanToMap(account);
        str = jsonParser.serialize(props, jsc);
        N.println(str);

        jsonParser.readString(str, Map.class);

        final Dataset dataset = N.newDataset(N.asList(account));
        str = jsonParser.serialize(dataset, jsc);
        N.println(str);

        jsonParser.readString(str, Dataset.class);
    }

    @Test
    public void test_readString_5() {
        final EntityId entityId = Seid.of(AccountPNL.ID, 123);

        final JsonSerializationConfig jsc = JSC.create().bracketRootValue(false).quoteMapKey(false).quotePropName(false).setStringQuotation(WD.CHAR_ZERO);
        String str = jsonParser.serialize(entityId, jsc);
        N.println(str);

        final EntityId entityId2 = jsonParser.readString(str, EntityId.class);
        assertEquals(entityId, entityId2);

        final String[] array = N.asArray("abc", "123");
        str = jsonParser.serialize(array, jsc);
        N.println(str);

        final String[] array2 = jsonParser.readString(str, String[].class);
        assertTrue(N.equals(array, array2));

        final List<String> list = N.asList("abc", "123");
        str = jsonParser.serialize(list, jsc);
        N.println(str);

        final List<String> list2 = jsonParser.readString(str, JDC.create().setElementType(String.class), List.class);
        assertTrue(N.equals(list, list2));

        final Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        account.setGUI(Strings.uuid());

        str = jsonParser.serialize(account, jsc);
        N.println(str);

        final Account account2 = jsonParser.readString(str, Account.class);
        assertTrue(N.equals(account, account2));

        final Map<String, Object> props = Beans.beanToMap(account);
        str = jsonParser.serialize(props, jsc);
        N.println(str);

        jsonParser.readString(str, Map.class);

        final Dataset dataset = N.newDataset(N.asList(account));
        str = jsonParser.serialize(dataset, jsc);
        N.println(str);

        jsonParser.readString(str, Dataset.class);
    }

    @Test
    public void testSerialize_mapEntity() {
        final MapEntity mapEntity = new MapEntity(AccountPNL.__);
        mapEntity.set(AccountPNL.ID, 123);
        mapEntity.set(AccountPNL.FIRST_NAME, "firstName");

        String json = jsonParser.serialize(mapEntity);
        N.println(json);

        MapEntity mapEntity2 = jsonParser.deserialize(json, MapEntity.class);
        N.println(mapEntity2);

        assertEquals(mapEntity, mapEntity2);

        final JsonSerializationConfig jsc = JSC.create().prettyFormat(true);
        json = jsonParser.serialize(mapEntity, jsc);
        N.println(json);

        mapEntity2 = jsonParser.deserialize(json, MapEntity.class);
        N.println(mapEntity2);

        assertEquals(mapEntity, mapEntity2);
    }

    @Test
    public void testSerialize_entityId() {
        Seid entityId = Seid.of(AccountPNL.ID, 123);

        String json = jsonParser.serialize(entityId);
        N.println(json);
        entityId = Seid.of(AccountPNL.__).set("id", 123);

        json = jsonParser.serialize(entityId);
        N.println(json);

        EntityId entityId2 = jsonParser.deserialize(json, EntityId.class);
        N.println(entityId2);

        assertEquals(entityId, entityId2);

        entityId2 = jsonParser.deserialize(json.substring(1, json.length() - 1), EntityId.class);
        N.println(entityId2);

        assertEquals(entityId, entityId2);

        String json3 = N.stringOf(entityId);
        N.println(json3);
        assertEquals(json, json3);

        EntityId entityId3 = N.valueOf(json3, EntityId.class);
        assertEquals(entityId, entityId3);

        entityId.clear();
        json = jsonParser.serialize(entityId);
        N.println(json);

        entityId2 = jsonParser.deserialize(json, EntityId.class);
        N.println(entityId2);
        assertEquals(entityId, entityId2);

        json3 = N.stringOf(entityId);
        N.println(json3);
        assertEquals(json, json3);

        entityId3 = N.valueOf(json3, EntityId.class);
        assertEquals(entityId, entityId3);
    }

    void execute(final Dataset rs) {
        final String json = jsonParser.serialize(rs);
        final Dataset rs2 = jsonParser.deserialize(json, Dataset.class);
        assertEquals(rs.size(), rs2.size());
    }

    @Test
    public void testSerialize_1() throws Exception {
        final XBean xBean = createXBean();

        final JsonSerializationConfig sc = JSC.create().setExclusion(Exclusion.NONE);
        final String str = jsonParser.serialize(xBean, sc);

        N.println(str);

        XBean xBean2 = jsonParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);

        xBean2 = jsonParser.deserialize(str.substring(1, str.length() - 1), XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_1_1() throws Exception {
        final XBean xBean = createXBean();

        final JsonSerializationConfig sc = JSC.create().setExclusion(Exclusion.NONE);
        sc.setPropNamingPolicy(NamingPolicy.SNAKE_CASE);
        final String str = jsonParser.serialize(xBean, sc);

        N.println(str);

        XBean xBean2 = jsonParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);

        xBean2 = jsonParser.deserialize(str.substring(1, str.length() - 1), XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_1_file() throws Exception {
        final XBean xBean = createXBean();

        final JsonSerializationConfig sc = JSC.create().setExclusion(Exclusion.NONE);
        final String str = jsonParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = jsonParser.deserialize(new File("./src/test/resources/test.json"), XBean.class);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize_2() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        final JsonSerializationConfig sc = JSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        final String str = jsonParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = jsonParser.deserialize(str, XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_3() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        final JsonSerializationConfig sc = JSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.setExclusion(Exclusion.NONE);

        final String str = jsonParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = jsonParser.deserialize(str, XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_ignorePropName() throws Exception {
        final Account account = createAccountWithContact(Account.class);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("firstName", "contact"));
        final JsonSerializationConfig sc = JSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        final String json = jsonParser.serialize(account);
        N.println(json);

        final JsonDeserializationConfig ds = JDC.create();
        ds.setIgnoredPropNames(ignoredPropNames);
        final Account account2 = jsonParser.deserialize(json, ds, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());
    }

    @Test
    public void testSerialize_ignorePropName_1() throws Exception {
        final Account account = createAccount(Account.class);

        final Map<String, Object> props = Beans.beanToMap(account);
        props.put("unknownProperty", "unknownProperty");
        final String json = jsonParser.serialize(props);
        N.println(json);

        Account account2 = jsonParser.deserialize(json, Account.class);
        N.println(account2);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("unknownProperty", "contact"));
        JsonDeserializationConfig ds = JDC.create().ignoreUnmatchedProperty(false).setIgnoredPropNames(ignoredPropNames);
        account2 = jsonParser.deserialize(json, Account.class);
        N.println(account2);

        assertNull(account2.getContact());

        ds = JDC.create().ignoreUnmatchedProperty(false);

        try {
            jsonParser.deserialize(json, ds, Account.class);
            fail("Should throw RuntimeException");
        } catch (final ParseException e) {

        }

    }

    @Test
    public void testSerialize_ignorePropName_1_1() throws Exception {
        final Account account = createAccount(Account.class);

        final Map<String, Object> props = Beans.beanToMap(account);
        props.put("unknownProperty", "unknownProperty");
        final String json = jsonParser.serialize(props, JSC.of(true, true));
        N.println(json);

        Account account2 = jsonParser.deserialize(json, Account.class);
        N.println(account2);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("unknownProperty", "contact"));
        JsonDeserializationConfig ds = JDC.create().ignoreUnmatchedProperty(false).setIgnoredPropNames(ignoredPropNames);
        account2 = jsonParser.deserialize(json, Account.class);
        N.println(account2);

        assertNull(account2.getContact());

        ds = JDC.create().ignoreUnmatchedProperty(false);

        try {
            jsonParser.deserialize(json, ds, Account.class);
            fail("Should throw RuntimeException");
        } catch (final ParseException e) {

        }

    }

    @Test
    public void testSerialize_ignorePropName_2() throws Exception {
        final Account account = createAccountWithContact(Account.class);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.asSet("firstName", "contact"));
        final JsonSerializationConfig sc = JSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        final String json = jsonParser.serialize(account);
        N.println(json);

        final JsonDeserializationConfig ds = JDC.create();
        ds.setIgnoredPropNames(ignoredPropNames);
        final Map<String, Object> account2 = jsonParser.deserialize(json, ds, Map.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.get("firstName"));
        assertNull(account2.get("contact"));
    }

    @Test
    public void testSerialize_unknowPropNames() throws Exception {
        final String json = "{gui_1:\"a59341120b70449c9ef003af96c61b79\", emailAddress:\"f89fd851ce774443b244468cc056762b@earth.com\", firstName:\"firstName\", middleName:\"MN\", lastName:\"lastName\", birthDate:1413839121008, lastUpdateTime:1413839121008, createdTime:1413839121008, contact_1:{address:\"ca, US\", city:\"sunnyvale\", state:\"CA\", country:\"U.S.\"}}";
        N.println(json);

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("firstName", "contact"));
        final JsonDeserializationConfig ds = JDC.create();
        ds.setIgnoredPropNames(ignoredPropNames);
        Account account2 = jsonParser.deserialize(json, ds, Account.class);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());

        try {
            jsonParser.deserialize(json, JDC.create().ignoreUnmatchedProperty(false), Account.class);
            fail("Should throw RuntimeException");
        } catch (final ParseException e) {

        }

        ignoredPropNames = N.asMap(Account.class, N.asSet("gui_1", "contact_1"));
        account2 = jsonParser.deserialize(json, JDC.create().ignoreUnmatchedProperty(false).setIgnoredPropNames(ignoredPropNames), Account.class);

        assertNotNull(account2.getFirstName());
        assertNull(account2.getContact());
    }

}
