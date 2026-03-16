package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.entity.extendDirty.basic.AccountDevice;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.entity.TypeBean;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.types.WeekDay;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.JsonMappers;
import com.landawn.abacus.util.MutableChar;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.TypeReference;
import com.landawn.abacus.util.stream.Stream;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Tag("old-test")
public class JsonParserTest extends AbstractJsonParserTest {
    private static final String bigBeanStr = jsonParser.serialize(bigXBean);

    // Alias for test methods merged from JsonParserTest which used 'parser'
    private final JsonParser parser = jsonParser;
    private File tempFile;

    @Override
    protected Parser<?, ?> getParser() {
        return jsonParser;
    }

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

    @Builder
    @Data
    public static class Bean2817 {
        private LocalDateTime fieldA;
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

    public enum RoomIdentifier {
        @SerializedName("MARKER_NAME")
        @JsonXmlField(name = "MARKER_NAME")
        ROOM_NAME;
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
        protected String status;
        private String _status;

        public String get_status() {
            return _status;
        }

        public void set_status(final String _status) {
            this._status = _status;
        }
    }

    static class Son extends Parent {

        public String getStatus() {
            return status;
        }

        public void setStatus(final String status) {
            this.status = status;
        }
    }

    static final Gson gson = new Gson().newBuilder().create();

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
        assertNotNull(m2);
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

    @Test
    public void test_emptyBean() {
        final Object obj = new Object();
        final SerializationConfig serializationConfig = JsonMappers.createSerializationConfig().without(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        String json = JsonMappers.toJson(obj, serializationConfig);
        N.println(json);
        json = JsonMappers.toJson(N.asMap("key1", obj, "key2", "ddd"), serializationConfig);
        N.println(json);
        json = JsonMappers.toJson(N.toList("key1", obj, "key2", "ddd"), serializationConfig);
        N.println(json);

        try {
            json = N.toJson(obj);
            fail("Should throw ParseException");
        } catch (final ParsingException e) {

        }

        final JsonSerConfig jsc = JsonSerConfig.create().setFailOnEmptyBean(false);

        json = N.toJson(obj, jsc);
        N.println(json);
        json = N.toJson(N.asMap("key1", obj, "key2", "ddd"), jsc);
        N.println(json);
        json = N.toJson(N.toList("key1", obj, "key2", "ddd"), jsc);
        N.println(json);
    }

    @Test
    public void test_selfReference() {
        final List<Object> list = N.toList("a", "b");
        list.add(list);

        final Map<String, Object> map = N.toMap("a", "va", "b", 123);
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

        final JsonSerConfig jsc = JsonSerConfig.create().setSupportCircularReference(true);

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
        assertNotNull(map2);
    }

    @Test
    public void test_PropHandler() {

        final Account account = Beans.newRandom(Account.class);
        N.println(account);

        account.setDevices(Beans.newRandomList(AccountDevice.class, 10));

        final String json = N.toJson(account);
        assertEquals(account, N.fromJson(json, Account.class));

        final Account account2 = N.fromJson(json, JsonDeserConfig.create().setPropHandler("devices", (c, e) -> {
            N.println(e);
        }), Account.class);

        assertEquals(0, account2.getDevices().size());

    }

    @Test
    public void test_MalformedJsonException() {

        final String str = "\\uXYZ";

        final String result = N.fromJson(str, String.class);

        N.println(result);
        assertNotNull(result);
    }

    @Test
    public void test_escape() {
        final Map<String, Object> map = N.asMap("a", "kdafs'ksfkd\"", "b", "\"", "c", '\'');

        N.println(N.toJson(map, true));

        N.println(JSON.toJSONString(map));
        assertNotNull(map);
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
        xbean.setStringListType(N.toList(bigValue, bigValue, bigValue));
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

        final String json = N.toJson(account, JsonSerConfig.create().setIgnoredPropNames(N.toSet("firstName")));

        N.println(json);

        final Account account2 = N.fromJson(json, JsonDeserConfig.create().setIgnoredPropNames(N.toSet("lastName")), Account.class);

        N.println(account2);
        assertNotNull(account2);
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

    @Test
    public void test_3083() {
        final String s = "{'is_subscribe':1,'subscribe':3,'isHave':5}";
        final TestBean b = JSON.parseObject(s, TestBean.class);
        println(b.is_subscribe + "--" + b.subscribe + "--" + b.isHave);

        N.println(N.stringOf(N.fromJson(s, TestBean.class)));
        assertNotNull(b);
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
        assertNotNull(map);
    }

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

    @Test
    public void test_jsonXmlConfig() throws JsonProcessingException {
        java.sql.Date currentDate = Dates.currentDate();

        String gui = "271e92c2-a3f1-40ef-af81-18e1bbcef490";

        BeanF bean = BeanF.builder()
                .id(1001)
                .gui(gui)
                .json(N.toJson(N.asMap("key1", 2, "key2", Dates.currentCalendar(), "key3", Dates.currentDate()),
                        JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)))
                .json2(N.toList(N.asMap("aa", currentDate)))
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
    }

    @Test
    public void test_parse_withClass() {
        String json = "\"hello\"";
        String result = parser.parse(json, String.class);
        Assertions.assertEquals("\"hello\"", result);

        String numJson = "42";
        Integer numResult = parser.parse(numJson, Integer.class);
        Assertions.assertEquals(42, numResult);

        String mapJson = "{\"name\":\"John\",\"age\":30}";
        Map<?, ?> mapResult = parser.parse(mapJson, Map.class);
        Assertions.assertEquals("John", mapResult.get("name"));
        Assertions.assertEquals(30, mapResult.get("age"));

        String listJson = "[1,2,3]";
        List<?> listResult = parser.parse(listJson, List.class);
        Assertions.assertEquals(3, listResult.size());
        Assertions.assertEquals(1, listResult.get(0));

        String nullResult = parser.parse(null, String.class);
        Assertions.assertNull(nullResult);
    }

    @Test
    public void test_parse_withConfigAndClass() {
        String json = "{\"name\":\"Jane\",\"age\":25}";
        JsonDeserConfig config = new JsonDeserConfig();

        Map<?, ?> result = parser.parse(json, config, Map.class);
        Assertions.assertEquals("Jane", result.get("name"));
        Assertions.assertEquals(25, result.get("age"));

        Map<?, ?> result2 = parser.parse(json, null, Map.class);
        Assertions.assertEquals("Jane", result2.get("name"));

        String boolJson = "true";
        Boolean boolResult = parser.parse(boolJson, config, Boolean.class);
        Assertions.assertEquals(true, boolResult);
    }

    @Test
    public void test_parse_toArray() {
        String json = "[1,2,3,4,5]";
        Integer[] array = new Integer[5];
        parser.parse(json, array);

        Assertions.assertEquals(1, array[0]);
        Assertions.assertEquals(2, array[1]);
        Assertions.assertEquals(3, array[2]);
        Assertions.assertEquals(4, array[3]);
        Assertions.assertEquals(5, array[4]);

        String strJson = "[\"a\",\"b\",\"c\"]";
        String[] strArray = new String[3];
        parser.parse(strJson, strArray);
        Assertions.assertEquals("a", strArray[0]);
        Assertions.assertEquals("b", strArray[1]);
        Assertions.assertEquals("c", strArray[2]);
    }

    @Test
    public void test_parse_toArrayWithConfig() {
        String json = "[10,20,30]";
        Integer[] array = new Integer[3];
        JsonDeserConfig config = new JsonDeserConfig();

        parser.parse(json, config, array);
        Assertions.assertEquals(10, array[0]);
        Assertions.assertEquals(20, array[1]);
        Assertions.assertEquals(30, array[2]);

        Integer[] array2 = new Integer[3];
        parser.parse(json, null, array2);
        Assertions.assertEquals(10, array2[0]);
        Assertions.assertEquals(20, array2[1]);
        Assertions.assertEquals(30, array2[2]);
    }

    @Test
    public void test_parse_toCollection() {
        String json = "[1,2,3,4]";
        List<Integer> list = new ArrayList<>();
        parser.parse(json, list);

        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
        Assertions.assertEquals(4, list.get(3));

        List<Integer> list2 = new ArrayList<>();
        list2.add(999);
        parser.parse(json, list2);
        Assertions.assertEquals(5, list2.size());
        Assertions.assertEquals(999, list2.get(0));
    }

    @Test
    public void test_parse_toCollectionWithConfig() {
        String json = "[\"x\",\"y\",\"z\"]";
        List<String> list = new ArrayList<>();
        JsonDeserConfig config = new JsonDeserConfig();

        parser.parse(json, config, list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("x", list.get(0));
        Assertions.assertEquals("y", list.get(1));
        Assertions.assertEquals("z", list.get(2));

        List<String> list2 = new ArrayList<>();
        parser.parse(json, null, list2);
        Assertions.assertEquals(3, list2.size());
    }

    @Test
    public void test_parse_toMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        Map<String, String> map = new HashMap<>();
        parser.parse(json, map);

        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("value1", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
        Assertions.assertEquals("value3", map.get("key3"));

        Map<String, String> map2 = new HashMap<>();
        map2.put("old", "data");
        parser.parse(json, map2);
        Assertions.assertEquals(4, map2.size());
        Assertions.assertEquals("data", map2.get("old"));
    }

    @Test
    public void test_parse_toMapWithConfig() {
        String json = "{\"a\":1,\"b\":2,\"c\":3}";
        Map<String, Integer> map = new LinkedHashMap<>();
        JsonDeserConfig config = new JsonDeserConfig();

        parser.parse(json, config, map);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
        Assertions.assertEquals(3, map.get("c"));

        Map<String, Integer> map2 = new HashMap<>();
        parser.parse(json, null, map2);
        Assertions.assertEquals(3, map2.size());
    }

    @Test
    public void test_deserialize_substring() {
        String json = "prefix{\"name\":\"Bob\",\"age\":35}suffix";
        int fromIndex = 6;
        int toIndex = json.length() - 6;

        Map<?, ?> result = parser.deserialize(json, fromIndex, toIndex, Map.class);
        Assertions.assertEquals("Bob", result.get("name"));
        Assertions.assertEquals(35, result.get("age"));

        String arrayJson = "xxx[1,2,3,4,5]yyy";
        List<?> listResult = parser.deserialize(arrayJson, 3, 14, List.class);
        Assertions.assertEquals(5, listResult.size());
        Assertions.assertEquals(1, listResult.get(0));
        Assertions.assertEquals(5, listResult.get(4));
    }

    @Test
    public void test_deserialize_substringWithConfig() {
        String json = "start{\"x\":100,\"y\":200}end";
        int fromIndex = 5;
        int toIndex = 23;
        JsonDeserConfig config = new JsonDeserConfig();

        Map<?, ?> result = parser.deserialize(json, fromIndex, toIndex, config, Map.class);
        Assertions.assertEquals(100, result.get("x"));
        Assertions.assertEquals(200, result.get("y"));

        Map<?, ?> result2 = parser.deserialize(json, fromIndex, toIndex, null, Map.class);
        Assertions.assertEquals(100, result2.get("x"));
    }

    @Test
    public void test_stream_fromString() {
        String json = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"},{\"id\":3,\"name\":\"Charlie\"}]";

        try (Stream<Map> stream = parser.stream(json, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0).get("id"));
            Assertions.assertEquals("Alice", list.get(0).get("name"));
            Assertions.assertEquals(2, list.get(1).get("id"));
            Assertions.assertEquals("Bob", list.get(1).get("name"));
            Assertions.assertEquals(3, list.get(2).get("id"));
            Assertions.assertEquals("Charlie", list.get(2).get("name"));
        }

        try (Stream<Map> stream = parser.stream(json, Type.of(Map.class))) {
            long count = stream.filter(m -> (Integer) m.get("id") > 1).count();
            Assertions.assertEquals(2, count);
        }
    }

    @Test
    public void test_stream_fromStringWithConfig() {
        String json = "[{\"value\":10},{\"value\":20},{\"value\":30}]";
        JsonDeserConfig config = new JsonDeserConfig();

        try (Stream<Map> stream = parser.stream(json, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(10, list.get(0).get("value"));
            Assertions.assertEquals(20, list.get(1).get("value"));
            Assertions.assertEquals(30, list.get(2).get("value"));
        }

        try (Stream<Map> stream = parser.stream(json, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
        }
    }

    @Test
    public void test_stream_fromFile() throws Exception {
        String json = "[{\"num\":1},{\"num\":2},{\"num\":3}]";
        tempFile = File.createTempFile("test", ".json");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(json);
        }

        try (Stream<Map> stream = parser.stream(tempFile, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0).get("num"));
            Assertions.assertEquals(2, list.get(1).get("num"));
            Assertions.assertEquals(3, list.get(2).get("num"));
        }
    }

    @Test
    public void test_stream_fromFileWithConfig() throws Exception {
        String json = "[{\"data\":\"A\"},{\"data\":\"B\"}]";
        tempFile = File.createTempFile("test", ".json");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(json);
        }

        JsonDeserConfig config = new JsonDeserConfig();
        try (Stream<Map> stream = parser.stream(tempFile, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals("A", list.get(0).get("data"));
            Assertions.assertEquals("B", list.get(1).get("data"));
        }

        try (Stream<Map> stream = parser.stream(tempFile, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
        }
    }

    @Test
    public void test_stream_fromInputStream() {
        String json = "[{\"id\":100},{\"id\":200}]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        try (Stream<Map> stream = parser.stream(inputStream, true, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(100, list.get(0).get("id"));
            Assertions.assertEquals(200, list.get(1).get("id"));
        }

        InputStream inputStream2 = new ByteArrayInputStream(json.getBytes());
        try (Stream<Map> stream = parser.stream(inputStream2, false, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
        }
    }

    @Test
    public void test_stream_fromInputStreamWithConfig() {
        String json = "[{\"val\":\"X\"},{\"val\":\"Y\"},{\"val\":\"Z\"}]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());
        JsonDeserConfig config = new JsonDeserConfig();

        try (Stream<Map> stream = parser.stream(inputStream, true, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals("X", list.get(0).get("val"));
            Assertions.assertEquals("Y", list.get(1).get("val"));
            Assertions.assertEquals("Z", list.get(2).get("val"));
        }

        InputStream inputStream2 = new ByteArrayInputStream(json.getBytes());
        try (Stream<Map> stream = parser.stream(inputStream2, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
        }
    }

    @Test
    public void test_stream_fromReader() {
        String json = "[{\"code\":1001},{\"code\":1002}]";
        Reader reader = new StringReader(json);

        try (Stream<Map> stream = parser.stream(reader, true, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(1001, list.get(0).get("code"));
            Assertions.assertEquals(1002, list.get(1).get("code"));
        }

        Reader reader2 = new StringReader(json);
        try (Stream<Map> stream = parser.stream(reader2, false, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
        }
    }

    @Test
    public void test_stream_fromReaderWithConfig() {
        String json = "[{\"item\":\"apple\"},{\"item\":\"banana\"},{\"item\":\"cherry\"}]";
        Reader reader = new StringReader(json);
        JsonDeserConfig config = new JsonDeserConfig();

        try (Stream<Map> stream = parser.stream(reader, true, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals("apple", list.get(0).get("item"));
            Assertions.assertEquals("banana", list.get(1).get("item"));
            Assertions.assertEquals("cherry", list.get(2).get("item"));
        }

        Reader reader2 = new StringReader(json);
        try (Stream<Map> stream = parser.stream(reader2, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
        }
    }

    @Test
    public void test_edgeCases_emptyArray() {
        String json = "[]";
        List<Integer> list = new ArrayList<>();
        parser.parse(json, list);
        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void test_edgeCases_emptyMap() {
        String json = "{}";
        Map<String, Object> map = new HashMap<>();
        parser.parse(json, map);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void test_edgeCases_nestedStructures() {
        String json = "{\"outer\":{\"inner\":{\"value\":42}}}";
        Map<?, ?> result = parser.parse(json, Map.class);
        Map<?, ?> outer = (Map<?, ?>) result.get("outer");
        Map<?, ?> inner = (Map<?, ?>) outer.get("inner");
        Assertions.assertEquals(42, inner.get("value"));
    }

    @Test
    public void test_edgeCases_arrayOfArrays() {
        String json = "[[1,2],[3,4],[5,6]]";
        List<?> result = parser.parse(json, List.class);
        Assertions.assertEquals(3, result.size());
        List<?> first = (List<?>) result.get(0);
        Assertions.assertEquals(2, first.size());
        Assertions.assertEquals(1, first.get(0));
        Assertions.assertEquals(2, first.get(1));
    }

    @Test
    public void testparseWithClass() {
        String json = "\"test\"";
        String result = parser.parse(json, String.class);
        Assertions.assertEquals("\"test\"", result);

        String nullResult = parser.parse(null, String.class);
        Assertions.assertNull(nullResult);
    }

    @Test
    public void testparseWithConfigAndClass() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Map<String, Object> result = parser.parse(json, null, Map.class);
        Assertions.assertEquals("John", result.get("name"));
        Assertions.assertEquals(30, result.get("age"));
    }

    @Test
    public void testparseToArray() {
        String json = "[1,2,3]";
        Integer[] array = new Integer[3];
        parser.parse(json, array);
        Assertions.assertEquals(1, array[0]);
        Assertions.assertEquals(2, array[1]);
        Assertions.assertEquals(3, array[2]);
    }

    @Test
    public void testparseToArrayWithConfig() {
        String json = "[\"a\",\"b\",\"c\"]";
        String[] array = new String[3];
        parser.parse(json, null, array);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    @Test
    public void testparseToCollection() {
        String json = "[1,2,3]";
        List<Integer> list = new ArrayList<>();
        parser.parse(json, list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
    }

    @Test
    public void testparseToCollectionWithConfig() {
        String json = "[\"x\",\"y\",\"z\"]";
        List<String> list = new ArrayList<>();
        parser.parse(json, null, list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("x", list.get(0));
        Assertions.assertEquals("y", list.get(1));
        Assertions.assertEquals("z", list.get(2));
    }

    @Test
    public void testparseToMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Map<String, String> map = new HashMap<>();
        parser.parse(json, map);
        Assertions.assertEquals("value1", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testparseToMapWithConfig() {
        String json = "{\"a\":1,\"b\":2}";
        Map<String, Integer> map = new HashMap<>();
        parser.parse(json, null, map);
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
    }

    @Test
    public void testDeserializeSubstring() {
        String json = "prefix{\"value\":123}suffix";
        Map<String, Object> result = parser.deserialize(json, 6, 19, Map.class);
        Assertions.assertEquals(123, result.get("value"));
    }

    @Test
    public void testDeserializeSubstringWithConfig() {
        String json = "xxx[1,2,3]yyy";
        List<Integer> result = parser.deserialize(json, 3, 10, null, List.class);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertEquals(2, result.get(1));
        Assertions.assertEquals(3, result.get(2));
    }

    //
    //    @Test
    //    public void testDeserializeRejectsTrailingTokensAfterList() {
    //        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[1,2] true", List.class));
    //    }

    //
    //
    //
    //
    @Test
    public void testStreamFromString() {
        String json = "[{\"id\":1},{\"id\":2},{\"id\":3}]";
        try (Stream<Map> stream = parser.stream(json, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0).get("id"));
            Assertions.assertEquals(2, list.get(1).get("id"));
            Assertions.assertEquals(3, list.get(2).get("id"));
        }
    }

    @Test
    @Disabled("This test is disabled due to the element type is not supported type: array/collection/map/bean.")
    public void testStreamFromStringWithConfig() {
        String json = "[\"a\",\"b\",\"c\"]";
        try (Stream<String> stream = parser.stream(json, null, Type.of(String.class))) {
            List<String> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals("a", list.get(0));
            Assertions.assertEquals("b", list.get(1));
            Assertions.assertEquals("c", list.get(2));
        }
    }

    @Test
    @Disabled("This test is disabled due to the element type is not supported type: array/collection/map/bean.")
    public void testStreamFromInputStream() {
        String json = "[1,2,3]";
        InputStream is = new ByteArrayInputStream(json.getBytes());
        try (Stream<Integer> stream = parser.stream(is, true, Type.of(Integer.class))) {
            List<Integer> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0));
            Assertions.assertEquals(2, list.get(1));
            Assertions.assertEquals(3, list.get(2));
        }
    }

    @Test
    public void testStreamFromInputStreamWithConfig() {
        String json = "[{\"x\":1},{\"x\":2}]";
        InputStream is = new ByteArrayInputStream(json.getBytes());
        try (Stream<Map> stream = parser.stream(is, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(1, list.get(0).get("x"));
            Assertions.assertEquals(2, list.get(1).get("x"));
        }
    }

    @Test
    @Disabled("This test is disabled due to the element type is not supported type: array/collection/map/bean.")
    public void testStreamFromReader() {
        String json = "[\"hello\",\"world\"]";
        Reader reader = new StringReader(json);
        try (Stream<String> stream = parser.stream(reader, true, Type.of(String.class))) {
            List<String> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals("hello", list.get(0));
            Assertions.assertEquals("world", list.get(1));
        }
    }

    @Test
    public void testStreamFromReaderWithConfig() {
        String json = "[{\"n\":100},{\"n\":200}]";
        Reader reader = new StringReader(json);
        try (Stream<Map> stream = parser.stream(reader, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(100, list.get(0).get("n"));
            Assertions.assertEquals(200, list.get(1).get("n"));
        }
    }

    @Test
    public void test_writeLongAsString() throws Exception {
        assertDoesNotThrow(() -> {
            Map<String, Object> map = N.asMap("key1", Long.valueOf(123), "Long.MAX_VALUE", Long.MAX_VALUE, "Integer.MAX_VALUE", Integer.MAX_VALUE,
                    "Integer.MIN_VALUE", Integer.MIN_VALUE);
            N.println(N.toJson(map));

            N.println(N.toJson(map, JsonSerConfig.create().setWriteLongAsString(true)));
        });
    }

    @Test
    public void test_double() throws Exception {
        String bigNum = Strings.repeat("6", 309) + "." + Strings.repeat("8", 10);

        {
            Map map = N.asMap("key", new BigDecimal(bigNum));
            String json = N.toJson(map);

            N.println(json);

            Map map01 = N.fromJson(json, Map.class);
            N.println(map01);

            assertEquals(map, map01);
        }
    }

    @Test
    public void test_02() {
        Object obj = N.toList(N.asMap("key", "value", "key2", "value2", "num1", 1, "num2", "92390asdflkj"));
        String json = N.toJson(obj);
        N.println(json);

        N.println(N.fromJson(json, Object.class));

        N.println(N.formatJson(json));

        json = "[{\"key2\":\"value2\", \"num2\":92390aef, \"num1\":1, \"key\":\"value\"}]";

        N.println(N.fromJson(json, Object.class));

        N.println(N.formatJson(json));
        assertNotNull(json);
    }

    @Test
    public void test_01() {
        Object obj = N.toList(N.asMap("key", "value", "key2", "value2"));
        String json = N.toJson(obj);
        N.println(json);

        N.println(N.fromJson(json, Object.class));

        N.println(N.formatJson(json));
        assertNotNull(json);
    }

    @Test
    public void test_prettyFormat() {
        Account account = createAccountWithContact(Account.class);
        account.setId(100);

        JsonSerConfig config = JsonSerConfig.create().setQuotePropName(true).setQuoteMapKey(true).setPrettyFormat(true).setIndentation("    ");

        String str = jsonParser.serialize(account, config);
        N.println("============account=====================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(N.asArray(account, account), config);
        N.println("============Array.of(account, account)=================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(N.toList(account, account), config);
        N.println("============N.toList(account, account)===================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(Beans.deepBeanToMap(account), config);
        N.println("============(N.deepBeanToMap(account)==================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(new Object[] { Beans.deepBeanToMap(account), account }, config);
        N.println("============Array.of(N.deepBeanToMap(account), account)===============================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(N.toList(Beans.deepBeanToMap(account), account), config);
        N.println("============N.toList(N.deepBeanToMap(account), account)================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(new Object[] { new Object[] { account, account }, N.toList(account, account) }, config);
        N.println("============Array.of(Array.of(account, account), N.toList(account, account))==========================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(N.toList(N.asArray(account, account), N.toList(account, account)), config);
        N.println("============N.toList(Array.of(account, account), N.toList(account, account))===========================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(Seid.of(AccountPNL.ID, "abc123"), config);
        N.println("============Seid.valueOf(Account.ID, \"abc123\")========================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(Seid.of(AccountPNL.ID, "abc123", AccountPNL.LAST_NAME, "lastName"), config);
        N.println("============Seid.valueOf(Account.ID, \"abc123\", Account.LAST_NAME, \"lastName\")=======================================");
        N.println(str);
        N.println("========================================================================================================================");

        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string黎< > </ <//、");

        List<Object> typeList = new ArrayList<>();
        typeList.add(account.getFirstName());
        typeList.add(account);
        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        xBean.setWeekDay(WeekDay.THURSDAY);

        str = jsonParser.serialize(xBean, config);

        N.println("============xBean=======================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");
        assertNotNull(str);
    }

    @Test
    @Tag("slow-test")
    public void test_readerPerformance() {
        assertDoesNotThrow(() -> {
            Profiler.run(6, 100, 1, this::test_reader).printResult();
        });
    }

    @Test
    public void test_parser_1() throws Exception {
        Account account = createAccount(Account.class);
        File file = new File("./src/test/resources/json.txt");

        if (file.exists()) {
            IOUtil.deleteRecursivelyIfExists(file);
        }

        OutputStream os = new FileOutputStream(file);
        jsonParser.serialize(account, os);
        IOUtil.close(os);

        String str = IOUtil.readAllToString(file);
        N.println(str);

        Account account2 = jsonParser.deserialize(file, Account.class);

        assertEquals(account, account2);

        file.delete();

        Writer writer = new FileWriter(file);
        jsonParser.serialize(account, writer);
        IOUtil.close(writer);

        str = IOUtil.readAllToString(file);
        N.println(str);

        account2 = jsonParser.deserialize(file, Account.class);

        assertEquals(account, account2);

        IOUtil.deleteIfExists(file);
    }

    @Test
    public void test_parser_2() throws Exception {
        Account account = createAccount(Account.class);
        File file = new File("./src/test/resources/json.txt");

        if (file.exists()) {
            IOUtil.deleteRecursivelyIfExists(file);
        }

        Writer writer = new FileWriter(file);
        jsonParser.serialize(account, writer);
        IOUtil.close(writer);

        String str = IOUtil.readAllToString(file);
        N.println(str);

        Account account2 = jsonParser.deserialize(file, Account.class);

        assertEquals(account, account2);
        IOUtil.deleteIfExists(file);
    }

    @Test
    public void test_reader() throws Exception {
        final char[] cbuf = Objectory.createCharArrayBuffer();

        final BufferedReader reader = Objectory.createBufferedReader(bigBeanStr);

        JsonReader jr = JsonStringReader.parse(bigBeanStr, cbuf);

        try {
            while (jr.nextToken() > -1) {
                if (jr.hasText()) {
                    jr.getText();
                }
            }
        } finally {
            Objectory.recycle(reader);
            Objectory.recycle(cbuf);
        }
        assertNotNull(jr);
    }

    @Test
    public void testSerialize_map() {
        Map<String, Object> m = N.asMap("firstName", "fn", "lastName", "ln", "birthday", "2003-08-08");
        String str = jsonParser.serialize(m, JsonSerConfig.create().setQuotePropName(false).setQuoteMapKey(false));
        N.println(str);

        Map<String, Object> m2 = jsonParser.deserialize(str, Map.class);

        N.println(m2);
        assertEquals(m, m2);
    }

    @Test
    public void testSerialize_0() {
        XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeString(
                "‰β,『�?★业€ > \\n sfd \\r ds \\' f d // \\\\  \\\\\\\\ /// /////// \\\\\\\\\\\\\\\\  \\\\\\\\\\\\\\\\n \\\\\\\\\\\\\\\\r  \\t sd \\\" fe stri‰β,『�?★业€ ng黎< > </ <//、\\n");

        String st = jsonParser.serialize(xBean);
        println(st);

        XBean xBean2 = jsonParser.deserialize(st, XBean.class);
        println(xBean2);
        assertEquals(xBean, xBean2);

        Bean bean = new Bean();
        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });
        bean.setChars2(new Character[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        String jsonStr = jsonParser.serialize(bean);
        println(jsonStr);

        Bean xmlBean = jsonParser.deserialize(jsonStr, Bean.class);
        assertEquals(bean, xmlBean);
    }

    @Test
    public void testSerialize() {
        Account account = createAccount(Account.class);
        account.setId(100);

        String str = jsonParser.serialize(account);
        println(str);

        println(jsonParser.deserialize(str, Account.class));

        str = "{\r\n" + "    \"id\": 100,\r\n" + "    \"gui\": \"d670ced631a14cf2820296263e2364f0\",\r\n"
                + "    \"emailAddress\": \"1e7cd28a386d47058a593d0dae386394@earth.com\",\r\n" + "    \"firstName\": \"firstName\",\r\n"
                + "    \"middleName\": \"MN\",\r\n" + "    \"lastName\": \"lastName\",\r\n" + "    \"birthDate\": 1394842092851,\r\n"
                + "    \"lastUpdateTime\": 1394842092851,\r\n" + "    \"createdTime\": 1394842092851\\r\\n" + "}";

        println(jsonParser.deserialize(str, Account.class));

        str = "{id:100,gui:\"5197aaf659794f1784fd45570ada3d62\",\"unknownProperty1\":null, emailAddress:\"13f6a5129c274c758a6eddf40fd825c6@earth.com\",firstName:\"firstName\",middleName:\"MN\",lastName:\"lastName\",birthDate:1399943675943,lastUpdateTime:1399943675943,createdTime:1399943675943,\"unknownProperty2\":1}";
        println(jsonParser.deserialize(str, Account.class));
        println(jsonParser.deserialize(str, Map.class));
        assertNotNull(str);
    }

    @Test
    public void testSerialize0() {
        Account account = createAccount(Account.class);
        account.setId(100);

        String st = jsonParser.serialize(account);
        println(st);

        println(jsonParser.deserialize(st, Account.class));

        st = jsonParser.serialize(account);
        println(st);

        println(jsonParser.deserialize(st, Account.class));
        assertNotNull(st);
    }

    @Test
    public void testSerialize1() {
        Bean bean = new Bean();
        bean.setTypeList(N.toList(
                "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★', '\n',
                '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                new String[] {
                        "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                        "\r", "\t", "\"", "\'" }));

        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        String jsonStr = jsonParser.serialize(bean);
        println(jsonStr);

        Bean xmlBean = jsonParser.deserialize(jsonStr, Bean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(jsonParser.serialize(bean));
        N.println(jsonParser.serialize(xmlBean));
        N.println(N.stringOf(bean));
        N.println(N.stringOf(xmlBean));
        N.println(jsonParser.serialize(bean));
        N.println(jsonParser.serialize(xmlBean));

        N.println(jsonParser.deserialize(jsonParser.serialize(bean), Bean.class));
        N.println(jsonParser.deserialize(jsonParser.serialize(xmlBean), Bean.class));
        assertNotNull(xmlBean);
    }

    @Test
    public void testSerialize2() {
        println(String.valueOf((char) 0));

        Account account = createAccount(Account.class);
        AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        N.println(jsonParser.serialize(account));

        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string黎< > </ <//、");

        List typeList = new ArrayList();
        typeList.add(account.getFirstName());
        typeList.add(account);
        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        xBean.setWeekDay(WeekDay.THURSDAY);

        String jsonStr = jsonParser.serialize(xBean);
        println(jsonStr);

        String st = N.stringOf(xBean);
        println(st);

        XBean xmlBean = jsonParser.deserialize(jsonStr, XBean.class);
        N.println(xBean);
        N.println(xmlBean);
        N.println(jsonParser.serialize(xBean));
        N.println(jsonParser.serialize(xmlBean));
        N.println(N.stringOf(xBean));
        N.println(N.stringOf(xmlBean));
        assertEquals(jsonParser.deserialize(jsonParser.serialize(xBean), XBean.class), jsonParser.deserialize(jsonParser.serialize(xmlBean), XBean.class));

        N.println(jsonParser.serialize(xBean));
        N.println(jsonParser.serialize(xmlBean));

        N.println(jsonParser.deserialize(jsonParser.serialize(xBean), XBean.class));
        N.println(jsonParser.deserialize(jsonParser.serialize(xmlBean), XBean.class));

        N.println(jsonParser.deserialize(jsonParser.serialize(xBean), Map.class));
        N.println(jsonParser.deserialize(jsonParser.serialize(xmlBean), Map.class));
    }

    @Test
    public void testGenericType() {
        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                N.toList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(N.toSet(1L, 2L));

        String jsonStr = jsonParser.serialize(xBean);
        println(jsonStr);
        println(jsonParser.serialize(jsonParser.deserialize(jsonStr, XBean.class)));
        assertNotNull(jsonStr);
    }

    @Test
    public void testSerialize4() {
        XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');

        xBean.setTypeString("");

        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());

        List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);

        String xml = jsonParser.serialize(xBean);
        println(xml);

        String st = N.stringOf(xBean);
        println(st);

        jsonParser.deserialize(xml, XBean.class);
        assertNotNull(st);
    }

    @Test
    public void testSerialize5() {
        List<Account> accounts = createAccountWithContact(Account.class, 100);
        String xml = jsonParser.serialize(accounts);
        println(xml);

        List<Account> xmlAccounts = jsonParser.deserialize(xml, List.class);

        N.println(N.stringOf(accounts));
        N.println(N.stringOf(xmlAccounts));
        assertNotNull(xmlAccounts);
    }

    @Test
    public void testSerialize6() {
        Account account = createAccountWithContact(Account.class);
        String xml = jsonParser.serialize(account);
        println(xml);

        jsonParser.deserialize(xml, com.landawn.abacus.entity.extendDirty.basic.Account.class);
        assertNotNull(xml);
    }

    @Test
    public void testXBean() {
        XBean bean = new XBean();
        Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        String xml = jsonParser.serialize(bean);
        println(xml);

        XBean xmlBean = jsonParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(jsonParser.serialize(bean));
        N.println(jsonParser.serialize(xmlBean));
        N.println(N.stringOf(bean));
        N.println(N.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(jsonParser.deserialize(jsonParser.serialize(bean), XBean.class), jsonParser.deserialize(jsonParser.serialize(xmlBean), XBean.class));

    }

    public static XBean createBigXBean(int size) {
        Account account = createAccount(Account.class);
        AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string< > </ <//");
        xBean.setWeekDay(WeekDay.FRIDAY);

        xBean.setTypeCalendar(Dates.currentCalendar());

        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());

        List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        Set<Long> typeGenericSet = N.toSortedSet();
        typeGenericSet.add(1332333L);
        typeGenericSet.add(Long.MAX_VALUE);
        typeGenericSet.add(Long.MIN_VALUE);
        xBean.setTypeGenericSet(typeGenericSet);

        List typeList = new ArrayList();
        typeList.add(account.getFirstName());

        for (int i = 0; i < size; i++) {
            typeList.add(account);
        }

        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        Set typeSet = new HashSet<>();
        typeSet.add(new HashMap<>());
        typeSet.add(new ArrayList<>());
        typeSet.add(new HashMap<>());
        typeSet.add(new HashMap<>());
        typeSet.add(null);
        typeSet.add(null);
        typeSet.add(account);
        typeSet.add(account.getLastName());
        typeSet.add(account);
        typeSet.add(account.getContact());
        typeSet.add(account);
        typeSet.add(null);
        xBean.setTypeSet(typeSet);

        Map<String, Account> typeGenericMap = new HashMap<>();
        typeGenericMap.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap.put(account.getLastName(), account);
        typeGenericMap.put("123", createAccount(Account.class));
        typeGenericMap.put("null", null);
        xBean.setTypeGenericMap(typeGenericMap);

        Map<String, Object> typeGenericMap2 = new TreeMap<>();
        typeGenericMap2.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap2.put(account.getLastName(), createAccount(Account.class));
        typeGenericMap2.put("null", null);
        typeGenericMap2.put("bookList", N.toList(createAccount(Account.class)));
        xBean.setTypeGenericMap2(typeGenericMap2);

        Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put("aaabbbccc", "");
        typeGenericMap4.put("bookList", N.toList(createAccount(Account.class)));
        typeGenericMap4.put("edse", " ");
        typeGenericMap4.put(new HashMap<>(), " ");
        typeGenericMap4.put(new ArrayList<>(), new HashSet<>());
        typeGenericMap4.put(typeGenericMap2, typeGenericMap);
        xBean.setTypeGenericMap4(typeGenericMap4);

        Map typeMap = new HashMap<>();

        for (int i = 0; i < size; i++) {
            typeMap.put(createAccount(Account.class), createAccount(Account.class));
        }

        typeMap.put("null", null);
        typeMap.put("bookList", N.toList(createAccount(Account.class)));
        typeMap.put(" ", " ");
        typeMap.put(new HashMap<>(), " ");
        typeMap.put(new ArrayList<>(), new HashSet<>());
        typeMap.put("007", null);
        typeMap.put(new HashMap<>(), null);
        typeMap.put(new ArrayList<>(), new HashSet<>());
        xBean.setTypeMap(typeMap);

        return xBean;
    }

    @Data
    public static class Bean_1 {
        private List<String> strList;
        private List intList;
        private List<Short> shortList;
        private XMLGregorianCalendar xmlGregorianCalendar;
    }

    @Data
    @XmlRootElement
    public static class Customer {
        String name;
        char ch;
        int age;
        int id;

    }

    @Data
    public static class Bean {
        private byte[] bytes;
        private char[] chars;
        private Character[] chars2;
        private String[] strings;
        private List typeList;
        private Set typeSet;

    }

    @Data
    public static class XBean {
        private boolean typeBoolean;
        private Boolean typeBoolean2;
        private char typeChar;
        private Character typeChar2;
        private byte typeByte;
        private short typeShort;
        private int typeInt;
        private long typeLong;
        private Long typeLong2;
        private float typeFloat;
        private double typeDouble;
        private String typeString;
        private Calendar typeCalendar;
        private java.util.Date typeDate;
        private Date typeSqlDate;
        private Time typeSqlTime;
        private Timestamp typeSqlTimestamp;
        private WeekDay weekDay;
        private List<Date> typeGenericList;
        private Set<Long> typeGenericSet;
        private List typeList;
        private Set typeSet;
        private Map<String, Account> typeGenericMap;
        private Map<String, Object> typeGenericMap2;
        private Map<Object, String> typeGenericMap3;
        private Map<Object, Object> typeGenericMap4;
        private Map typeMap;

    }

    @Data
    public static class JaxbBean {
        private String string;
        private List<String> list;
        private Map<String, String> map;

    }

}
