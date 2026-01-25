package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.types.WeekDay;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Strings;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.experimental.Accessors;

public class JsonParser2Test extends AbstractParserTest {
    private static final String bigBeanStr = jsonParser.serialize(bigBean);

    @Test
    public void test_2309() {
        final SpecificResource resource = new SpecificResource();

        resource.id = "/resources/specific/123";
        resource.data = "resource payload";

        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(resource));

        N.println(N.toJson(resource));

        assertEquals("{\"id\": \"/resources/specific/123\", \"data\": \"resource payload\"}", N.toJson(resource));
    }

    public abstract static class BaseResource {
        public String id;
    }

    public static final class SpecificResource extends BaseResource {
        public String data;
    }

    @Test
    public void test_writeLongAsString() throws Exception {
        Map<String, Object> map = N.asMap("key1", Long.valueOf(123), "Long.MAX_VALUE", Long.MAX_VALUE, "Integer.MAX_VALUE", Integer.MAX_VALUE,
                "Integer.MIN_VALUE", Integer.MIN_VALUE);
        N.println(N.toJson(map));

        N.println(N.toJson(map, JSC.create().writeLongAsString(true)));
    }

    @Data
    @Accessors(chain = true)
    static class BeanMap {
        private String key;
        private Object value;
        private String type;
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
    public void test_000() throws Exception {
        final BigInteger bigInteger = new BigInteger(Strings.repeat('8', 100_000));

        {
            Map map = N.asMap("key", Long.MAX_VALUE);
            String json = N.toJson(map);

            N.println(json);

            Map map01 = N.fromJson(json, Map.class);

            assertEquals(map, map01);
        }
        {
            Map map = N.asMap("key", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
            String json = N.toJson(map);

            N.println(json);

            Map map01 = N.fromJson(json, Map.class);

            assertEquals(map, map01);
        }

        {
            Map map00 = N.asMap("bigInteger", bigInteger);

            String json00 = N.toJson(map00);

            Map map01 = N.fromJson(json00, Map.class);

            assertEquals(map00, map01);

            Map map02 = N.fromJson(new StringReader(json00), Map.class);

            assertEquals(map00, map02);
        }

        {

            Map map = N.asLinkedHashMap("null", null, "bigInteger", bigInteger, "false", false, "true", true, "minInt", Integer.MIN_VALUE, "maxInt",
                    Integer.MAX_VALUE, "intZero", 0, "minLong", Long.MIN_VALUE, "maxLong", Long.MAX_VALUE, "minLongMinusOne",
                    BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), "maxLongPlusOne", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),
                    "minLongPlusOne", Long.MIN_VALUE + 1, "floatMin", Float.MIN_VALUE, "floatMax", Float.MAX_VALUE, "floatMin-2", -Float.MAX_VALUE, "float-NaN",
                    Float.NaN, "float-NEGATIVE_INFINITY", Float.NEGATIVE_INFINITY, "float-POSITIVE_INFINITY", Float.POSITIVE_INFINITY, "float-0.0", 0.0f,
                    "float-0", 0f, "float-0.000123", 0.000123f, "float--0.000123", -0.000123f, "float-0.12309294093821094201", 0.12309294093821094201f,
                    "float--0.12309294093821094201", -0.12309294093821094201f, "float-232.123092940931094201", 232.123092940931094201f, "doubleMin",
                    Double.MIN_VALUE, "doubleMax", Double.MAX_VALUE, "doubleMin-2", -Double.MAX_VALUE, "double-NaN", Double.NaN, "double-NEGATIVE_INFINITY",
                    Double.NEGATIVE_INFINITY, "double-POSITIVE_INFINITY", Double.POSITIVE_INFINITY, "double-0.0", 0.0d, "double-0.000123", 0.000123d,
                    "double--0.000123", -0.000123d, "double-0.12309294093821094201", 0.12309294093821094201d, "double--0.12309294093821094201",
                    -0.12309294093821094201d, "double-232.123092940931094201", 232.123092940931094201d);

            String json = N.toJson(map);
            N.println(json);
            N.println(N.toJson(map, true));

            Map<Object, Object> map2 = N.fromJson(json, LinkedHashMap.class);

            N.println(N.toJson(map2, true));

            {
                List<Object> floatKeys = map2.entrySet().stream().filter(it -> it.getKey().toString().startsWith("float")).map(Entry::getKey).toList();

                for (Object key : floatKeys) {
                    if (map2.get(key) instanceof Number) {
                        map2.put(key, ((Number) map2.get(key)).floatValue());
                    } else {
                        map2.put(key, Numbers.toFloat(((String) map2.get(key))));
                    }
                }
            }

            {
                List<Object> floatKeys = map2.entrySet().stream().filter(it -> it.getKey().toString().startsWith("double")).map(Entry::getKey).toList();

                for (Object key : floatKeys) {
                    if (map2.get(key) instanceof Number) {
                    } else {
                        map2.put(key, Numbers.toDouble(((String) map2.get(key))));
                    }
                }
            }

            N.println(N.toJson(map2, true));

            assertEquals(map, map2);
        }

        {

            Map map = N.asLinkedHashMap("null", null, "false", false, "true", true, "minInt", Integer.MIN_VALUE, "maxInt", Integer.MAX_VALUE, "intZero", 0,
                    "minLong", Long.MIN_VALUE, "maxLong", Long.MAX_VALUE, "minLongMinusOne", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE),
                    "maxLongPlusOne", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), "minLongPlusOne", Long.MIN_VALUE + 1, "floatMin", Float.MIN_VALUE,
                    "floatMax", Float.MAX_VALUE, "floatMin-2", -Float.MAX_VALUE, "float-NaN", Float.NaN, "float-NEGATIVE_INFINITY", Float.NEGATIVE_INFINITY,
                    "float-POSITIVE_INFINITY", Float.POSITIVE_INFINITY, "float-0.0", 0.0f, "float-0", 0f, "float-0.000123", 0.000123f, "float--0.000123",
                    -0.000123f, "float-0.12309294093821094201", 0.12309294093821094201f, "float--0.12309294093821094201", -0.12309294093821094201f,
                    "float-232.123092940931094201", 232.123092940931094201f, "doubleMin", Double.MIN_VALUE, "doubleMax", Double.MAX_VALUE, "doubleMin-2",
                    -Double.MAX_VALUE, "double-NaN", Double.NaN, "double-NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY, "double-POSITIVE_INFINITY",
                    Double.POSITIVE_INFINITY, "double-0.0", 0.0d, "double-0.000123", 0.000123d, "double--0.000123", -0.000123d, "double-0.12309294093821094201",
                    0.12309294093821094201d, "double--0.12309294093821094201", -0.12309294093821094201d, "double-232.123092940931094201",
                    232.123092940931094201d);

            String json = N.toJson(map);
            N.println(json);
            N.println(N.toJson(map, true));

            Map<Object, Object> map2 = N.fromJson(new StringReader(json), LinkedHashMap.class);

            N.println(N.toJson(map2, true));

            {
                List<Object> floatKeys = map2.entrySet().stream().filter(it -> it.getKey().toString().startsWith("float")).map(Entry::getKey).toList();

                for (Object key : floatKeys) {
                    if (map2.get(key) instanceof Number) {
                        map2.put(key, ((Number) map2.get(key)).floatValue());
                    } else {
                        map2.put(key, Numbers.toFloat(((String) map2.get(key))));
                    }
                }
            }

            {
                List<Object> floatKeys = map2.entrySet().stream().filter(it -> it.getKey().toString().startsWith("double")).map(Entry::getKey).toList();

                for (Object key : floatKeys) {
                    if (map2.get(key) instanceof Number) {
                    } else {
                        map2.put(key, Numbers.toDouble(((String) map2.get(key))));
                    }
                }
            }

            N.println(N.toJson(map2, true));

            assertEquals(map, map2);
        }
    }

    @Test
    public void test_03() {
        List<BeanMap> beanMapList = new ArrayList<>();
        beanMapList.add(new BeanMap().setKey("key1").setValue("val1").setType("String"));
        beanMapList.add(new BeanMap().setKey("key2").setValue(N.asList(beanMapList.get(0), 1, "aaaaaa")).setType("Object"));
        beanMapList.add(new BeanMap().setKey("key3").setValue(Dates.currentCalendar()).setType("Calendar"));
        beanMapList.add(new BeanMap().setKey("key4").setValue(N.asMap("mapKey1", Dates.currentDate())).setType("Calendar"));

        String json = N.toJson(beanMapList);

        N.println(json);

        List<BeanMap> beanMapList2 = N.fromJson(json, Type.ofList(BeanMap.class));
        N.println(beanMapList2);
    }

    @Test
    public void test_02() {
        Object obj = N.asList(N.asMap("key", "value", "key2", "value2", "num1", 1, "num2", "92390asdflkj"));
        String json = N.toJson(obj);
        N.println(json);

        N.println(N.fromJson(json, Object.class));

        N.println(N.formatJson(json));

        json = "[{\"key2\":\"value2\", \"num2\":92390aef, \"num1\":1, \"key\":\"value\"}]";

        N.println(N.fromJson(json, Object.class));

        N.println(N.formatJson(json));
    }

    @Test
    public void test_01() {
        Object obj = N.asList(N.asMap("key", "value", "key2", "value2"));
        String json = N.toJson(obj);
        N.println(json);

        N.println(N.fromJson(json, Object.class));

        N.println(N.formatJson(json));
    }

    @Test
    public void test_prettyFormat() {
        Account account = createAccountWithContact(Account.class);
        account.setId(100);

        JsonSerializationConfig config = JSC.create().quotePropName(true).quoteMapKey(true).prettyFormat(true).setIndentation("    ");

        String str = jsonParser.serialize(account, config);
        N.println("============account=====================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(N.asArray(account, account), config);
        N.println("============Array.of(account, account)=================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(N.asList(account, account), config);
        N.println("============N.asList(account, account)===================================================================================");
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

        str = jsonParser.serialize(N.asList(Beans.deepBeanToMap(account), account), config);
        N.println("============N.asList(N.deepBeanToMap(account), account)================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(new Object[] { new Object[] { account, account }, N.asList(account, account) }, config);
        N.println("============Array.of(Array.of(account, account), N.asList(account, account))==========================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = jsonParser.serialize(N.asList(N.asArray(account, account), N.asList(account, account)), config);
        N.println("============N.asList(Array.of(account, account), N.asList(account, account))===========================================");
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
    }

    @Test
    @Tag("slow-test")
    public void test_readerPerformance() {
        Profiler.run(6, 100, 1, this::test_reader).printResult();
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

    }

    @Test
    public void testSerialize_map() {
        Map<String, Object> m = N.asMap("firstName", "fn", "lastName", "ln", "birthday", "2003-08-08");
        String str = jsonParser.serialize(m, JSC.of(false, false));
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
    }

    @Test
    public void testSerialize1() {
        Bean bean = new Bean();
        bean.setTypeList(N.asList(
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
                N.asList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(N.asSet(1L, 2L));

        String jsonStr = jsonParser.serialize(xBean);
        println(jsonStr);
        println(jsonParser.serialize(jsonParser.deserialize(jsonStr, XBean.class)));

    }

    @Test
    @Tag("slow-test")
    public void testPerformanceForBigBean() throws Exception {
        jsonParser.deserialize(jsonParser.serialize(createBigXBean(1)), XBean.class);
        File file = new File("./src/test/resources/bigBean.txt");
        IOUtil.write(jsonParser.serialize(createBigXBean(10000)), file);

        Profiler.run(1, 3, 1, this::executeBigBean).printResult();

        file.delete();
    }

    void executeBigBean() {
        jsonParser.deserialize(jsonParser.serialize(createBigXBean(10000)), XBean.class);
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

        Set<Long> typeGenericSet = N.asSortedSet();
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
        typeGenericMap2.put("bookList", N.asList(createAccount(Account.class)));
        xBean.setTypeGenericMap2(typeGenericMap2);

        Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put("aaabbbccc", "");
        typeGenericMap4.put("bookList", N.asList(createAccount(Account.class)));
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
        typeMap.put("bookList", N.asList(createAccount(Account.class)));
        typeMap.put(" ", " ");
        typeMap.put(new HashMap<>(), " ");
        typeMap.put(new ArrayList<>(), new HashSet<>());
        typeMap.put("007", null);
        typeMap.put(new HashMap<>(), null);
        typeMap.put(new ArrayList<>(), new HashSet<>());
        xBean.setTypeMap(typeMap);

        return xBean;
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

    }

    @Test
    public void testSerialize5() {
        List<Account> accounts = createAccountWithContact(Account.class, 100);
        String xml = jsonParser.serialize(accounts);
        println(xml);

        List<Account> xmlAccounts = jsonParser.deserialize(xml, List.class);

        N.println(N.stringOf(accounts));
        N.println(N.stringOf(xmlAccounts));

    }

    @Test
    public void testSerialize6() {
        Account account = createAccountWithContact(Account.class);
        String xml = jsonParser.serialize(account);
        println(xml);

        jsonParser.deserialize(xml, com.landawn.abacus.entity.extendDirty.basic.Account.class);

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

    public static class Bean_1 {
        private List<String> strList;
        private List intList;
        private List<Short> shortList;
        private XMLGregorianCalendar xmlGregorianCalendar;

        public List<String> getStrList() {
            return strList;
        }

        public void setStrList(List<String> strList) {
            this.strList = strList;
        }

        public List getIntList() {
            return intList;
        }

        public void setIntList(List intList) {
            this.intList = intList;
        }

        public List<Short> getShortList() {
            return shortList;
        }

        public void setShortList(List<Short> shortList) {
            this.shortList = shortList;
        }

        public XMLGregorianCalendar getXmlGregorianCalendar() {
            return xmlGregorianCalendar;
        }

        public void setXmlGregorianCalendar(XMLGregorianCalendar xmlGregorianCalendar) {
            this.xmlGregorianCalendar = xmlGregorianCalendar;
        }

        @Override
        public int hashCode() {
            return Objects.hash(intList, shortList, strList, xmlGregorianCalendar);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            Bean_1 other = (Bean_1) obj;

            if (!Objects.equals(intList, other.intList) || !Objects.equals(shortList, other.shortList) || !Objects.equals(strList, other.strList)
                    || !Objects.equals(xmlGregorianCalendar, other.xmlGregorianCalendar)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "{strList=" + strList + ", intList=" + intList + ", shortList=" + shortList + ", xmlGregorianCalendar=" + xmlGregorianCalendar + "}";
        }
    }

    @XmlRootElement
    public static class Customer {
        String name;
        char ch;
        int age;
        int id;

        public String getName() {
            return name;
        }

        @XmlElement
        public void setChar(char ch) {
            this.ch = ch;
        }

        public char getChar() {
            return ch;
        }

        @XmlElement
        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        @XmlElement
        public void setAge(int age) {
            this.age = age;
        }

        public int getId() {
            return id;
        }

        @XmlAttribute
        public void setId(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, ch, id, name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            Customer other = (Customer) obj;

            if ((age != other.age) || (ch != other.ch) || (id != other.id) || !Objects.equals(name, other.name)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "{name=" + name + ", ch=" + ch + ", age=" + age + ", id=" + id + "}";
        }
    }

    public static class Bean {
        private byte[] bytes;
        private char[] chars;
        private Character[] chars2;
        private String[] strings;
        private List typeList;
        private Set typeSet;

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public char[] getChars() {
            return chars;
        }

        public void setChars(char[] chars) {
            this.chars = chars;
        }

        public Character[] getChars2() {
            return chars2;
        }

        public void setChars2(Character[] chars2) {
            this.chars2 = chars2;
        }

        public String[] getStrings() {
            return strings;
        }

        public void setStrings(String[] strings) {
            this.strings = strings;
        }

        public List getTypeList() {
            return typeList;
        }

        public void setTypeList(List typeList) {
            this.typeList = typeList;
        }

        public Set getTypeSet() {
            return typeSet;
        }

        public void setTypeSet(Set typeSet) {
            this.typeSet = typeSet;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(bytes), Arrays.hashCode(chars), Arrays.hashCode(strings), typeList, typeSet);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            Bean other = (Bean) obj;

            if (!Arrays.equals(bytes, other.bytes) || !Arrays.equals(chars, other.chars) || !Arrays.equals(strings, other.strings)
                    || !Objects.equals(typeList, other.typeList)) {
                return false;
            }

            if (!Objects.equals(typeSet, other.typeSet)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "{bytes=" + Arrays.toString(bytes) + ", chars=" + Arrays.toString(chars) + ", strings=" + Arrays.toString(strings) + ", typeList=" + typeList
                    + ", typeSet=" + typeSet + "}";
        }
    }

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

        public boolean getTypeBoolean() {
            return typeBoolean;
        }

        public void setTypeBoolean(boolean typeBoolean) {
            this.typeBoolean = typeBoolean;
        }

        public Boolean getTypeBoolean2() {
            return typeBoolean2;
        }

        public void setTypeBoolean2(Boolean typeBoolean2) {
            this.typeBoolean2 = typeBoolean2;
        }

        public char getTypeChar() {
            return typeChar;
        }

        public void setTypeChar(char typeChar) {
            this.typeChar = typeChar;
        }

        public Character getTypeChar2() {
            return typeChar2;
        }

        public void setTypeChar2(Character typeChar2) {
            this.typeChar2 = typeChar2;
        }

        public byte getTypeByte() {
            return typeByte;
        }

        public void setTypeByte(byte typeByte) {
            this.typeByte = typeByte;
        }

        public short getTypeShort() {
            return typeShort;
        }

        public void setTypeShort(short typeShort) {
            this.typeShort = typeShort;
        }

        public int getTypeInt() {
            return typeInt;
        }

        public void setTypeInt(int typeInt) {
            this.typeInt = typeInt;
        }

        public long getTypeLong() {
            return typeLong;
        }

        public void setTypeLong(long typeLong) {
            this.typeLong = typeLong;
        }

        public Long getTypeLong2() {
            return typeLong2;
        }

        public void setTypeLong2(Long typeLong2) {
            this.typeLong2 = typeLong2;
        }

        public float getTypeFloat() {
            return typeFloat;
        }

        public void setTypeFloat(float typeFloat) {
            this.typeFloat = typeFloat;
        }

        public double getTypeDouble() {
            return typeDouble;
        }

        public void setTypeDouble(double typeDouble) {
            this.typeDouble = typeDouble;
        }

        public String getTypeString() {
            return typeString;
        }

        public void setTypeString(String typeString) {
            this.typeString = typeString;
        }

        public Calendar getTypeCalendar() {
            return typeCalendar;
        }

        public void setTypeCalendar(Calendar typeCalendar) {
            this.typeCalendar = typeCalendar;
        }

        public java.util.Date getTypeDate() {
            return typeDate;
        }

        public void setTypeDate(java.util.Date typeDate) {
            this.typeDate = typeDate;
        }

        public Date getTypeSqlDate() {
            return typeSqlDate;
        }

        public void setTypeSqlDate(Date typeSqlDate) {
            this.typeSqlDate = typeSqlDate;
        }

        public Time getTypeSqlTime() {
            return typeSqlTime;
        }

        public void setTypeSqlTime(Time typeSqlTime) {
            this.typeSqlTime = typeSqlTime;
        }

        public Timestamp getTypeSqlTimestamp() {
            return typeSqlTimestamp;
        }

        public void setTypeSqlTimestamp(Timestamp typeSqlTimestamp) {
            this.typeSqlTimestamp = typeSqlTimestamp;
        }

        public WeekDay getWeekDay() {
            return weekDay;
        }

        public void setWeekDay(WeekDay weekDay) {
            this.weekDay = weekDay;
        }

        public List<Date> getTypeGenericList() {
            return typeGenericList;
        }

        public void setTypeGenericList(List<Date> typeGenericList) {
            this.typeGenericList = typeGenericList;
        }

        public Set<Long> getTypeGenericSet() {
            return typeGenericSet;
        }

        public void setTypeGenericSet(Set<Long> typeGenericSet) {
            this.typeGenericSet = typeGenericSet;
        }

        public List getTypeList() {
            return typeList;
        }

        public void setTypeList(List typeList) {
            this.typeList = typeList;
        }

        public Set getTypeSet() {
            return typeSet;
        }

        public void setTypeSet(Set typeSet) {
            this.typeSet = typeSet;
        }

        public Map<String, Account> getTypeGenericMap() {
            return typeGenericMap;
        }

        public void setTypeGenericMap(Map<String, Account> typeGenericMap) {
            this.typeGenericMap = typeGenericMap;
        }

        public Map getTypeMap() {
            return typeMap;
        }

        public void setTypeMap(Map typeMap) {
            this.typeMap = typeMap;
        }

        public Map<String, Object> getTypeGenericMap2() {
            return typeGenericMap2;
        }

        public void setTypeGenericMap2(Map<String, Object> typeGenericMap2) {
            this.typeGenericMap2 = typeGenericMap2;
        }

        public Map<Object, String> getTypeGenericMap3() {
            return typeGenericMap3;
        }

        public void setTypeGenericMap3(Map<Object, String> typeGenericMap3) {
            this.typeGenericMap3 = typeGenericMap3;
        }

        public Map<Object, Object> getTypeGenericMap4() {
            return typeGenericMap4;
        }

        public void setTypeGenericMap4(Map<Object, Object> typeGenericMap4) {
            this.typeGenericMap4 = typeGenericMap4;
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeBoolean, typeBoolean2, typeByte, typeCalendar, typeChar, typeChar2, typeDate, typeDouble, typeFloat, typeGenericList,
                    typeGenericMap, typeGenericMap2, typeGenericMap3, typeGenericMap4, typeGenericSet, typeInt, typeList, typeLong, typeLong2, typeMap, typeSet,
                    typeShort, typeSqlDate, typeSqlTime, typeSqlTimestamp, typeString);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            XBean other = (XBean) obj;

            if ((typeBoolean != other.typeBoolean) || !Objects.equals(typeBoolean2, other.typeBoolean2) || (typeByte != other.typeByte)
                    || !Objects.equals(typeCalendar, other.typeCalendar)) {
                return false;
            }

            if (typeChar != other.typeChar) {
                return false;
            }

            if (!Objects.equals(typeChar2, other.typeChar2)) {
                return false;
            }

            if (!Objects.equals(typeDate, other.typeDate)) {
                return false;
            }

            if (Double.doubleToLongBits(typeDouble) != Double.doubleToLongBits(other.typeDouble)) {
                return false;
            }

            if (Float.floatToIntBits(typeFloat) != Float.floatToIntBits(other.typeFloat)) {
                return false;
            }

            if (!Objects.equals(typeGenericList, other.typeGenericList)) {
                return false;
            }

            if (!Objects.equals(typeGenericMap, other.typeGenericMap)) {
                return false;
            }

            if (!Objects.equals(typeGenericMap2, other.typeGenericMap2)) {
                return false;
            }

            if (!Objects.equals(typeGenericMap3, other.typeGenericMap3)) {
                return false;
            }

            if (!Objects.equals(typeGenericMap4, other.typeGenericMap4)) {
                return false;
            }

            if (!Objects.equals(typeGenericSet, other.typeGenericSet)) {
                return false;
            }

            if (typeInt != other.typeInt) {
                return false;
            }

            if (!Objects.equals(typeList, other.typeList)) {
                return false;
            }

            if (typeLong != other.typeLong) {
                return false;
            }

            if (!Objects.equals(typeLong2, other.typeLong2)) {
                return false;
            }

            if (!Objects.equals(typeMap, other.typeMap)) {
                return false;
            }

            if (!Objects.equals(typeSet, other.typeSet)) {
                return false;
            }

            if (typeShort != other.typeShort) {
                return false;
            }

            if (!Objects.equals(typeSqlDate, other.typeSqlDate)) {
                return false;
            }

            if (!Objects.equals(typeSqlTime, other.typeSqlTime)) {
                return false;
            }

            if (!Objects.equals(typeSqlTimestamp, other.typeSqlTimestamp)) {
                return false;
            }

            if (!Objects.equals(typeString, other.typeString)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "{typeBoolean=" + typeBoolean + ", typeBoolean2=" + typeBoolean2 + ", typeChar=" + typeChar + ", typeChar2=" + typeChar2 + ", typeByte="
                    + typeByte + ", typeShort=" + typeShort + ", typeInt=" + typeInt + ", typeLong=" + typeLong + ", typeLong2=" + typeLong2 + ", typeFloat="
                    + typeFloat + ", typeDouble=" + typeDouble + ", typeString=" + typeString + ", typeCalendar=" + typeCalendar + ", typeDate=" + typeDate
                    + ", typeSqlDate=" + typeSqlDate + ", typeSqlTime=" + typeSqlTime + ", typeSqlTimestamp=" + typeSqlTimestamp + ", typeGenericList="
                    + typeGenericList + ", typeGenericSet=" + typeGenericSet + ", typeList=" + typeList + ", typeSet=" + typeSet + ", typeGenericMap="
                    + typeGenericMap + ", typeGenericMap2=" + typeGenericMap2 + ", typeGenericMap3=" + typeGenericMap3 + ", typeGenericMap4=" + typeGenericMap4
                    + ", typeMap=" + typeMap + "}";
        }
    }

    public static class JaxbBean {
        private String string;
        private List<String> list;
        private Map<String, String> map;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public List<String> getList() {
            if (list == null) {
                list = new ArrayList<>();
            }

            return list;
        }

        public Map<String, String> getMap() {
            if (map == null) {
                map = new HashMap<>();
            }

            return map;
        }

        @Override
        public int hashCode() {
            return Objects.hash(list, map, string);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            JaxbBean other = (JaxbBean) obj;

            if (!Objects.equals(list, other.list) || !Objects.equals(map, other.map) || !Objects.equals(string, other.string)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "{string=" + string + ", list=" + list + ", map=" + map + "}";
        }
    }
}
