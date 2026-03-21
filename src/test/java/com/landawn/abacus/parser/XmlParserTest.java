package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.entity.GenericEntity;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

public class XmlParserTest extends AbstractXmlParserTest {

    @Override
    protected Parser<?, ?> getParser() {
        return xmlParser;
    }

    @Test
    public void test_prettyFormat_2() {
        final Account account = createAccount(Account.class);

        final GenericEntity genericBean = new GenericEntity();
        genericBean.setBooleanList(N.toList(true, false));
        genericBean.setCharList(N.toList('a', 'b', '黎'));
        genericBean.setIntList(N.toList(1, 2, 3));
        genericBean.setStringList(N.toList("abc", "123"));
        genericBean.setAccountList(N.toList(account));
        final Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        final XmlSerConfig xsc = XmlSerConfig.create().setPrettyFormat(true);
        final String str = xmlParser.serialize(genericBean, xsc);

        N.println(str);

        final GenericEntity genericBean2 = xmlParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
    }

    @Test
    public void test_GenericEntity() {
        final Account account = createAccount(Account.class);

        final GenericEntity genericBean = new GenericEntity();
        genericBean.setAccountList(N.toList(account));
        final Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        final String str = xmlParser.serialize(genericBean);

        N.println(str);

        final GenericEntity genericBean2 = xmlParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
    }

    @Test
    public void test_transient() {
        final TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");

        String str = xmlParser.serialize(bean);

        N.println(str);

        assertTrue(str.indexOf("abc") == -1);

        final XmlSerConfig config = XmlSerConfig.create().setSkipTransientField(false);
        str = xmlParser.serialize(bean, config);

        N.println(str);

        assertTrue(str.indexOf("abc") >= 0);

        assertTrue(bean.equals(xmlParser.deserialize(str, TransientBean.class)));
    }

    @Test
    public void testSerialize_map() {
        Map<String, Object> m = N.asMap("firstName", "fn", "lastName", "ln", "birthday", "2003-08-08");
        String str = jsonParser.serialize(m);
        N.println(str);

        Map<String, Object> m2 = jsonParser.deserialize(str, Map.class);

        N.println(m2);
        assertEquals(m, m2);
    }

    @Test
    public void test_null_1() {
        final String nullElement = null;
        final String str = xmlParser.serialize(nullElement);
        assertNull(xmlParser.deserialize(str, String.class));
    }

    @Test
    public void test_null_2() {
        final Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        final String nullElement = null;

        final Object[] array = { account, nullElement };
        String str = xmlParser.serialize(array);
        N.println(str);

        final Object[] array2 = xmlParser.deserialize(str, XmlDeserConfig.create().setElementType(Account.class), Object[].class);
        assertTrue(N.equals(array, array2));

        final List<?> list = N.toList(account, nullElement);
        str = xmlParser.serialize(list);
        N.println(str);

        final List<String> list2 = xmlParser.deserialize(str, XmlDeserConfig.create().setElementType(Account.class), List.class);
        assertTrue(N.equals(list, list2));
        N.println(list2);

        final Map<String, Object> map = N.asMap(nullElement, account);
        final XmlSerConfig xsc = XmlSerConfig.create().setExclusion(Exclusion.NONE);
        str = xmlParser.serialize(map, xsc);
        N.println(str);

        final Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);
        N.println(map2);
    }

    @Test
    public void test_null_3() {
        final Account account = new Account();

        String str = xmlParser.serialize(account);

        account.setId(0);
        account.setFirstName("firstName");
        account.setLastName(null);

        str = xmlParser.serialize(account, XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames((Map<Class<?>, Set<String>>) null));
        N.println(str);

        str = xmlParser.serialize(Beans.beanToMap(account),
                XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames((Map<Class<?>, Set<String>>) null));
        N.println(str);

        final Map<String, Object> map = Beans.beanToMap(account);
        map.put("lastName", null);
        map.put("account", account);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.toSet("id"));

        XmlSerConfig xsc = XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignoredPropNames).setPrettyFormat(true);
        str = xmlParser.serialize(map, xsc);
        N.println(str);

        final Map<String, Object> map2 = xmlParser.deserialize(str, XmlDeserConfig.create().setElementType(Account.class), Map.class);
        N.println(map2);

        str = xmlParser.serialize(N.toList(map), xsc);
        N.println(str);

        final XmlDeserConfig xdc = XmlDeserConfig.create().setElementType(Map.class);
        final List<?> list = xmlParser.deserialize(str, xdc, List.class);
        N.println(list);

        final Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.toList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));

        xsc = XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignoredPropNames).setPrettyFormat(true);
        str = xmlParser.serialize(map3, xsc);
        N.println(str);

        N.println(xmlParser.deserialize(str, XmlDeserConfig.create().setElementType(Account.class), Map.class));

        xsc = XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignoredPropNames).setPrettyFormat(true);
        str = xmlParser.serialize(map3, xsc);
        N.println(str);

        final XBean xBean = createXBean();
        str = xmlParser.serialize(xBean, xsc);
        N.println(str);

        N.println(xmlParser.deserialize(str, XBean.class));
        assertNotNull(str);
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

        str = "{id:100,gui:\"5197aaf659794f1784fd45570ada3d62\",unknownProperty1:null, emailAddress:\"13f6a5129c274c758a6eddf40fd825c6@earth.com\",firstName:\"firstName\",middleName:\"MN\",lastName:\"lastName\",birthDate:1399943675943,lastUpdateTime:1399943675943,createdTime:1399943675943,unknownProperty2:1}";
        println(jsonParser.deserialize(str, Account.class));
        println(jsonParser.deserialize(str, Map.class));
        assertNotNull(str);
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
    public void test_node_by_name() throws SAXException, IOException {
        final Account account = createAccount(Account.class);

        final String str = xmlParser.serialize(account);
        final InputStream is = IOUtil.stringToInputStream(str);

        final Map<String, Type<?>> nodeClasses = N.asMap("account", Type.of(Account.class));

        Account account2 = xmlParser.deserialize(is, null, nodeClasses);

        IOUtil.close(is);

        N.println(account2);

        assertEquals(account, account2);

        final Reader reader = new StringReader(str);
        account2 = xmlParser.deserialize(reader, null, nodeClasses);

        IOUtil.close(reader);

        N.println(account2);

        assertEquals(account, account2);

        final DocumentBuilder docBuilder = XmlUtil.createDOMParser();
        final Document doc = docBuilder.parse(IOUtil.stringToInputStream(str));
        account2 = xmlParser.deserialize(doc.getDocumentElement(), null, nodeClasses);

        N.println(account2);

        assertEquals(account, account2);
    }

    @Test
    public void test_null() {
        final String nullElement = null;

        final String[] array = N.asArray(nullElement);
        String str = xmlParser.serialize(array);
        N.println(str);

        final String[] array2 = xmlParser.deserialize(str, String[].class);
        assertTrue(N.equals(array, array2));

        final List<String> list = N.toList(nullElement);
        str = xmlParser.serialize(list);
        N.println(str);

        final List<String> list2 = xmlParser.deserialize(str, List.class);
        N.println(list2);

        final Map<String, Object> map = N.asMap(nullElement, nullElement);
        final XmlSerConfig jsc = XmlSerConfig.create().setExclusion(Exclusion.NONE);
        str = xmlParser.serialize(map, jsc);
        N.println(str);

        final Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);
        N.println(map2);

        xmlParser.deserialize(str, Object.class);

        try {
            xmlParser.deserialize(str, xmlParser.getClass());
            fail("Should throw RuntimeException");
        } catch (final ParsingException e) {

        }

        try {
            xmlParser.serialize(xmlParser);
            fail("Should throw RuntimeException");
        } catch (final ParsingException e) {

        }
    }

    @Test
    public void test_stax() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        final String xml = xmlParser.serialize(account, XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(true));
        N.println(xml);
        N.println(account);

        final Account account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        final Object account3 = xmlParser.deserialize(xml, Map.class);
        N.println(account3);
        assertNotNull(account3);
    }

    @Test
    public void test_perf() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        final String xml = xmlParser.serialize(account);
        N.println(xml);
        N.println(account);

        final int threadNum = 6;
        final int loopNum = 1;
        final int repeat = 1;

        N.println("########################################### DOM #################################");
        Profiler.run(threadNum, loopNum, repeat, () -> xmlDOMParser.deserialize(xml, Account.class)).printResult();

        N.println("########################################### StAX #################################");
        Profiler.run(threadNum, loopNum, repeat, () -> xmlParser.deserialize(xml, Account.class)).printResult();

        N.println("########################################### Abacus SAX #################################");
        Profiler.run(threadNum, loopNum, repeat, () -> abacusXmlParser.deserialize(xml, Account.class)).printResult();

        N.println("########################################### Abacus DOM #################################");
        Profiler.run(threadNum, loopNum, repeat, () -> abacusXMLDOMParser.deserialize(xml, Account.class)).printResult();
        assertNotNull(repeat);
    }

    @Test
    public void testSerialize_List() throws Exception {
        final List<String> list = N.toList("abc", "123");
        final String xml = xmlParser.serialize(list);

        N.println(xml);

        final List<?> list2 = xmlParser.deserialize(xml, List.class);
        N.println(list2);
        assertNotNull(list2);
    }

    @Test
    public void testSerialize_tagName() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        String xml = xmlParser.serialize(account);
        N.println(xml);
        N.println(account);

        xml = "<map><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><accountContact><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact></map>";
        Account account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        xml = "<map><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><map><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></map></contact></map>";
        account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        xml = "<list><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><list><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></list></contact></list>";
        account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        xml = "<key><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><key><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></key></contact></key>";
        account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        xml = "<value><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><value><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></value></contact></value>";
        account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        xml = "<entry><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><entry><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></entry></contact></entry>";
        account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        xml = "<e><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><e><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></e></contact></e>";
        account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        xml = "<unknown><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><unknown><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></unknown></contact></unknown>";
        account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);
        assertNotNull(account2);
    }

    @Test
    public void testSerialize_tagName2() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        String xml = xmlParser.serialize(account);
        N.println(xml);
        N.println(account);

        xml = "<map><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><accountContact><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact></map>";
        Map<String, String> account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<map><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><map><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></map></contact></map>";
        account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<list><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><list><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></list></contact></list>";
        account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<key><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><key><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></key></contact></key>";
        account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<value><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><value><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></value></contact></value>";
        account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<entry><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><entry><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></entry></contact></entry>";
        account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<e><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><e><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></e></contact></e>";
        account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<unknown><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><unknown><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></unknown></contact></unknown>";
        account2 = xmlParser.deserialize(xml, Map.class);
        N.println(account2);
        assertNotNull(account2);
    }

    @Test
    public void testSerialize_1() throws Exception {
        final XBean xBean = createXBean();

        final XmlSerConfig sc = XmlSerConfig.create().setExclusion(Exclusion.NONE);
        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertNotNull(xBean2);
    }

    @Test
    public void testSerialize_1_1() throws Exception {
        final XBean xBean = createXBean();

        final XmlSerConfig sc = XmlSerConfig.create().setExclusion(Exclusion.NONE);
        sc.setPropNamingPolicy(NamingPolicy.SNAKE_CASE);
        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertNotNull(xBean2);
    }

    @Test
    public void testSerialize_2() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.toSet("typeBoolean", "typeShort", "typeLong"));
        final XmlSerConfig sc = XmlSerConfig.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.setWriteTypeInfo(true);

        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertNotNull(xBean2);
    }

    @Test
    public void testSerialize_3() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.toSet("typeBoolean", "typeShort", "typeLong"));
        final XmlSerConfig sc = XmlSerConfig.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.setExclusion(Exclusion.NONE);

        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertNotNull(xBean2);
    }

    @Test
    public void testSerialize_4() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.toSet("typeBoolean", "typeShort", "typeLong"));
        final XmlSerConfig sc = XmlSerConfig.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.setExclusion(Exclusion.NONE);

        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        final XBean xBean2 = xmlParser.deserialize(doc.getDocumentElement(), XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertNotNull(xBean2);
    }

    @Test
    public void testSerialize_5() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("charArray", Array.of('a', 'b', 'c'));
        map.put("array", N.asArray("abc", "123"));
        map.put("intList", N.toList(1, 2, 3));
        map.put("charList", N.toList('a', 'b', 'c'));
        map.put("list", N.toList("abc", "123"));

        final String str = xmlParser.serialize(map);

        N.println(str);

        Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XmlDeserConfig.create().setMapKeyType(String.class).setMapValueType(String[].class), Map.class);

        N.println(map2);
        assertNotNull(map2);
    }

    @Test
    public void testSerialize_6() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("charArray", Array.of('a', 'b', 'c'));
        map.put("array", new Object[] { "abc", "123" });
        map.put("intList", N.toList(1, 2, 3));
        map.put("charList", N.toList('a', 'b', 'c'));
        map.put("list", N.toList("abc", "123"));

        final String str = xmlParser.serialize(map);

        N.println(str);

        Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XmlDeserConfig.create().setMapKeyType(String.class).setMapValueType(String[].class), Map.class);

        N.println(map2);
        assertNotNull(map2);
    }

    @Test
    public void testSerialize_7() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("charArray", Array.of('a', 'b', 'c'));
        map.put("array", new Object[] { createAccount(Account.class), createAccount(Account.class) });
        map.put("intList", N.toList(1, 2, 3));
        map.put("charList", N.toList('a', 'b', 'c'));
        map.put("list", N.toList(createAccount(Account.class), createAccount(Account.class)));

        String str = xmlParser.serialize(map);

        N.println(str);

        Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XmlDeserConfig.create().setElementType(Account.class), Map.class);

        N.println(map2);

        str = xmlParser.serialize(map2);

        N.println(str);

        map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XmlDeserConfig.create().setElementType(Account.class), Map.class);

        N.println(map2);
        assertNotNull(map2);
    }

    @Test
    public void testSerialize_8() throws Exception {
        final XBean xBean = createXBean();

        final XmlSerConfig sc = XmlSerConfig.create().setTagByPropertyName(false);
        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertNotNull(xBean2);
    }

    @Test
    public void testSerialize__10() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);

        XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(false);
        xsc.setExclusion(Exclusion.NONE);
        String xml = xmlParser.serialize(account, xsc);
        N.println(xml);

        XmlDeserConfig xdc = XmlDeserConfig.create();
        Account account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());

        xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(true);
        xsc.setExclusion(Exclusion.NONE);
        xml = xmlParser.serialize(account, xsc);
        N.println(xml);

        xdc = XmlDeserConfig.create();
        account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());
    }

    @Test
    public void testSerialize__11() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);
        N.println(account);

        XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false);

        xsc.setExclusion(Exclusion.NONE);
        String xml = xmlParser.serialize(Beans.deepBeanToMap(account), xsc);
        N.println(xml);

        XmlDeserConfig xdc = XmlDeserConfig.create();
        Account account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account2);

        xml = xmlParser.serialize(N.toList(account), xsc);
        N.println(xml);

        xdc = XmlDeserConfig.create().setElementType(Account.class);
        final List<Account> accountList = xmlParser.deserialize(xml, xdc, List.class);
        N.println(accountList);

        xml = xmlParser.serialize(N.asArray(account), xsc);
        N.println(xml);

        xdc = XmlDeserConfig.create().setElementType(Account.class);
        final Object[] accountArray = xmlParser.deserialize(xml, xdc, Object[].class);
        N.println(accountArray);

        xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(true);
        xsc.setExclusion(Exclusion.NONE);
        xml = xmlParser.serialize(Beans.deepBeanToMap(account), xsc);
        N.println(xml);

        xdc = XmlDeserConfig.create();
        account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);
        assertNotNull(account2);
    }

    @Test
    public void testSerialize_ignorePropName() throws Exception {
        final Account account = createAccountWithContact(Account.class);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.toSet("firstName", "contact"));
        final XmlSerConfig sc = XmlSerConfig.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        final String xml = xmlParser.serialize(account);
        N.println(xml);

        final XmlDeserConfig xdc = XmlDeserConfig.create();
        xdc.setIgnoredPropNames(ignoredPropNames);
        final Account account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());
    }

    @Test
    public void testSerialize_ignorePropName_2() throws Exception {
        final Account account = createAccountWithContact(Account.class);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.toSet("firstName", "contact"));
        final XmlSerConfig sc = XmlSerConfig.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        final String xml = xmlParser.serialize(Beans.deepBeanToMap(account));
        N.println(xml);

        final XmlDeserConfig xdc = XmlDeserConfig.create();
        xdc.setIgnoredPropNames(ignoredPropNames);
        final Map<String, Object> account2 = xmlParser.deserialize(xml, xdc, Map.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.get("firstName"));
        assertNull(account2.get("contact"));
    }

    @Test
    public void testSerialize_unknowPropNames() throws Exception {
        final String xml = "<account><gui_1>9b1b4964298a4868a4ab95ccf6a5f987</gui_1><emailAddress>48c6a440fa114de28fad1bf04fa66090@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1413839551838</birthDate><lastUpdateTime>1413839551838</lastUpdateTime><createdTime>1413839551838</createdTime><contact_1><accountContact><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact_1></account>";
        N.println(xml);

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.toSet("firstName", "contact"));
        final XmlDeserConfig dc = XmlDeserConfig.create();
        dc.setIgnoredPropNames(ignoredPropNames);
        Account account2 = xmlParser.deserialize(xml, dc, Account.class);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());

        try {
            xmlParser.deserialize(xml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames((Map<Class<?>, Set<String>>) null),
                    Account.class);
            fail("Should throw RuntimeException");
        } catch (final ParsingException e) {

        }

        ignoredPropNames = N.asMap(Account.class, N.toSet("gui_1", "contact_1"));
        account2 = xmlParser.deserialize(xml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames(ignoredPropNames), Account.class);

        assertNotNull(account2.getFirstName());
        assertNull(account2.getContact());
    }

    @Test
    public void testSerialize_array() throws Exception {
        final Account[] accounts = N.asArray(createAccount(Account.class), createAccount(Account.class));

        final String xml = xmlParser.serialize(accounts);

        N.println(xml);

        final Account[] accounts2 = xmlParser.deserialize(xml, Account[].class);
        N.println(accounts2);

        assertTrue(N.equals(accounts, accounts2));

    }

    @Test
    public void testSerialize_simple_type() throws Exception {
        {
            final String xml = xmlParser.serialize(new Object[] {});

            N.println(xml);

            final String[] a = xmlParser.deserialize(xml, String[].class);

            N.println(N.stringOf(a));
        }

        {
            final String xml = xmlParser.serialize(N.asArray("abc", "123"));

            N.println(xml);

            final String[] a = xmlParser.deserialize(xml, String[].class);

            N.println(N.stringOf(a));
        }

        {
            final String xml = xmlParser.serialize(new ArrayList<>());

            N.println(xml);

            final List<?> a = xmlParser.deserialize(xml, List.class);

            N.println(N.stringOf(a));
        }

        {
            final String xml = xmlParser.serialize(N.toList("abc", "123"));

            N.println(xml);

            final List<?> a = xmlParser.deserialize(xml, List.class);

            N.println(N.stringOf(a));
        }
    }

    @Test
    public void testSerialize_list() throws Exception {
        final List<Account> accounts = N.toList(createAccount(Account.class), createAccount(Account.class));

        final String xml = xmlParser.serialize(accounts);

        N.println(xml);

        final XmlDeserConfig xdc = XmlDeserConfig.create().setElementType(Account.class);
        final List<Account> accounts2 = xmlParser.deserialize(xml, xdc, List.class);
        N.println(accounts2);

        assertTrue(N.equals(accounts, accounts2));

    }

    @Test
    public void testSerialize_MapEntity() throws Exception {
        final MapEntity account = new MapEntity("MapEntity", Beans.beanToMap(createAccount(Account.class)));

        final String xml = xmlParser.serialize(account);

        N.println(xml);

        final MapEntity accounts2 = xmlParser.deserialize(xml, MapEntity.class);
        N.println(accounts2);
        assertNotNull(accounts2);
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
    public void test_BufferedWriter() {
        final Writer writer = new StringWriter();
        final BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(writer);
        final Account account = createAccount(Account.class);
        xmlParser.serialize(account, bw);

        N.println(writer.toString());

        Objectory.recycle(bw);
        assertNotNull(account);
    }

    @Test
    public void test_BufferedWriter_2() {
        final Writer writer = new StringWriter();
        final BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(writer);
        final Account account = null;
        xmlParser.serialize(account, bw);

        assertEquals(Strings.EMPTY, writer.toString());

        Objectory.recycle(bw);
    }
}
