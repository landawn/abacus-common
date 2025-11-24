package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.parser.XMLDeserializationConfig.XDC;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.parser.entity.GenericEntity;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

@Tag("old-test")
public class XMLParserTest extends AbstractXMLParserTest {
    @Override
    protected Parser<?, ?> getParser() {
        return xmlParser;
    }

    @Test
    public void test_prettyFormat_2() {
        final Account account = createAccount(Account.class);

        final GenericEntity genericBean = new GenericEntity();
        genericBean.setBooleanList(N.asList(true, false));
        genericBean.setCharList(N.asList('a', 'b', '黎'));
        genericBean.setIntList(N.asList(1, 2, 3));
        genericBean.setStringList(N.asList("abc", "123"));
        genericBean.setAccountList(N.asList(account));
        final Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        final XMLSerializationConfig xsc = XSC.create().prettyFormat(true);
        final String str = xmlParser.serialize(genericBean, xsc);

        N.println(str);

        final GenericEntity genericBean2 = xmlParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
    }

    @Test
    public void test_prettyFormat_3() {
        final Account account = createAccount(Account.class);

        final GenericEntity genericBean = new GenericEntity();
        genericBean.setBooleanList(N.asList(true, false));
        genericBean.setCharList(N.asList('a', 'b', '黎'));
        genericBean.setIntList(N.asList(1, 2, 3));
        genericBean.setStringList(N.asList("abc", "123"));
        genericBean.setAccountList(N.asList(account));
        final Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        final XMLSerializationConfig xsc = XSC.create().prettyFormat(true);
        final Map<String, Object> props = Beans.bean2Map(genericBean);
        final String str = xmlDOMParser.serialize(props, xsc);

        N.println(str);

        final GenericEntity genericBean2 = xmlDOMParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
        final Map<String, Object> props2 = xmlDOMParser.deserialize(str, Map.class);
        N.println(props);
        N.println(props2);

    }

    @Test
    public void test_GenericEntity() {
        final Account account = createAccount(Account.class);

        final GenericEntity genericBean = new GenericEntity();
        genericBean.setAccountList(N.asList(account));
        final Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        final String str = xmlParser.serialize(genericBean);

        N.println(str);

        final GenericEntity genericBean2 = xmlParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
    }

    @Test
    public void test_node_by_name() throws SAXException, IOException {
        final Account account = createAccount(Account.class);

        final String str = xmlParser.serialize(account);
        final InputStream is = IOUtil.string2InputStream(str);

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
        final Document doc = docBuilder.parse(IOUtil.string2InputStream(str));
        account2 = xmlParser.deserialize(doc.getDocumentElement(), null, nodeClasses);

        N.println(account2);

        assertEquals(account, account2);
    }

    @Test
    public void test_BufferedWriter() {
        final Writer writer = new StringWriter();
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(writer);
        final Account account = createAccount(Account.class);
        xmlParser.serialize(account, bw);

        N.println(writer.toString());

        Objectory.recycle(bw);
    }

    @Test
    public void test_BufferedWriter_2() {
        final Writer writer = new StringWriter();
        final BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(writer);
        final Account account = null;
        xmlParser.serialize(account, bw);

        assertEquals(Strings.EMPTY, writer.toString());

        Objectory.recycle(bw);
    }

    @Test
    public void test_null() {
        final String nullElement = null;

        final String[] array = N.asArray(nullElement);
        String str = xmlParser.serialize(array);
        N.println(str);

        final String[] array2 = xmlParser.deserialize(str, String[].class);
        assertTrue(N.equals(array, array2));

        final List<String> list = N.asList(nullElement);
        str = xmlParser.serialize(list);
        N.println(str);

        final List<String> list2 = xmlParser.deserialize(str, List.class);
        N.println(list2);

        final Map<String, Object> map = N.asMap(nullElement, nullElement);
        final XMLSerializationConfig jsc = XSC.create().setExclusion(Exclusion.NONE);
        str = xmlParser.serialize(map, jsc);
        N.println(str);

        final Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);
        N.println(map2);

        xmlParser.deserialize(str, Object.class);

        try {
            xmlParser.deserialize(str, xmlParser.getClass());
            fail("Should throw RuntimeException");
        } catch (final ParseException e) {

        }

        try {
            xmlParser.serialize(xmlParser);
            fail("Should throw RuntimeException");
        } catch (final ParseException e) {

        }
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

        final Object[] array2 = xmlParser.deserialize(str, XDC.of(Account.class), Object[].class);
        assertTrue(N.equals(array, array2));

        final List<?> list = N.asList(account, nullElement);
        str = xmlParser.serialize(list);
        N.println(str);

        final List<String> list2 = xmlParser.deserialize(str, XDC.of(Account.class), List.class);
        assertTrue(N.equals(list, list2));
        N.println(list2);

        final Map<String, Object> map = N.asMap(nullElement, account);
        final XMLSerializationConfig xsc = XSC.create().setExclusion(Exclusion.NONE);
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

        str = xmlParser.serialize(account, XSC.of(Exclusion.DEFAULT, null));
        N.println(str);

        str = xmlParser.serialize(Beans.bean2Map(account), XSC.of(Exclusion.DEFAULT, null));
        N.println(str);

        final Map<String, Object> map = Beans.bean2Map(account);
        map.put("lastName", null);
        map.put("account", account);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.asSet("id"));

        XMLSerializationConfig xsc = XSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = xmlParser.serialize(map, xsc);
        N.println(str);

        final Map<String, Object> map2 = xmlParser.deserialize(str, XDC.of(Account.class), Map.class);
        N.println(map2);

        str = xmlParser.serialize(N.asList(map), xsc);
        N.println(str);

        final XMLDeserializationConfig xdc = XDC.of(Map.class);
        final List<?> list = xmlParser.deserialize(str, xdc, List.class);
        N.println(list);

        final Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.asList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));

        xsc = XSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = xmlParser.serialize(map3, xsc);
        N.println(str);

        N.println(xmlParser.deserialize(str, XDC.of(Account.class), Map.class));

        xsc = XSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = xmlParser.serialize(map3, xsc);
        N.println(str);

        final XBean xBean = createXBean();
        str = xmlParser.serialize(xBean, xsc);
        N.println(str);

        N.println(xmlParser.deserialize(str, XBean.class));
    }

    @Test
    public void test_config() {
        final XMLSerializationConfig xsc1 = XSC.create();
        final XMLSerializationConfig xsc2 = XSC.create();

        N.println(xsc1);

        assertTrue(N.asSet(xsc1).contains(xsc2));

        final XMLDeserializationConfig xdc1 = XDC.of(String.class, String.class, true, null);
        final XMLDeserializationConfig xdc2 = XDC.of(String.class, String.class, true, null);

        N.println(xdc1);

        assertTrue(N.asSet(xdc1).contains(xdc2));
    }

    @Test
    public void test_transient() {
        final TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");

        String str = xmlParser.serialize(bean);

        N.println(str);

        assertTrue(str.indexOf("abc") == -1);

        final XMLSerializationConfig config = XSC.create().skipTransientField(false);
        str = xmlParser.serialize(bean, config);

        N.println(str);

        assertTrue(str.indexOf("abc") >= 0);

        assertTrue(bean.equals(xmlParser.deserialize(str, TransientBean.class)));
    }

    @Test
    public void test_stax() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        final String xml = xmlParser.serialize(account, XSC.of(true, true));
        N.println(xml);
        N.println(account);

        final Account account2 = xmlParser.deserialize(xml, Account.class);
        N.println(account2);

        final Object account3 = xmlParser.deserialize(xml, Map.class);
        N.println(account3);
    }

    @Test
    @Tag("slow-test")
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
        Profiler.run(threadNum, loopNum, repeat, () -> abacusXMLParser.deserialize(xml, Account.class)).printResult();

        N.println("########################################### Abacus DOM #################################");
        Profiler.run(threadNum, loopNum, repeat, () -> abacusXMLDOMParser.deserialize(xml, Account.class)).printResult();
    }

    @Test
    public void testSerialize_List() throws Exception {
        final List<String> list = N.asList("abc", "123");
        final String xml = xmlParser.serialize(list);

        N.println(xml);

        final List<?> list2 = xmlParser.deserialize(xml, List.class);
        N.println(list2);

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
    }

    @Test
    public void testSerialize_1() throws Exception {
        final XBean xBean = createXBean();

        final XMLSerializationConfig sc = XSC.create().setExclusion(Exclusion.NONE);
        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize_1_1() throws Exception {
        final XBean xBean = createXBean();

        final XMLSerializationConfig sc = XSC.create().setExclusion(Exclusion.NONE);
        sc.setPropNamingPolicy(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize_2() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        final XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.writeTypeInfo(true);

        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize_3() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        final XMLSerializationConfig sc = XSC.create();
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
    }

    @Test
    public void testSerialize_4() throws Exception {
        final XBean xBean = createXBean();

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        final XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.setExclusion(Exclusion.NONE);

        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final Document doc = XmlUtil.createDOMParser().parse(IOUtil.string2InputStream(str));
        final XBean xBean2 = xmlParser.deserialize(doc.getDocumentElement(), XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize_5() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("charArray", Array.of('a', 'b', 'c'));
        map.put("array", N.asArray("abc", "123"));
        map.put("intList", N.asList(1, 2, 3));
        map.put("charList", N.asList('a', 'b', 'c'));
        map.put("list", N.asList("abc", "123"));

        final String str = xmlParser.serialize(map);

        N.println(str);

        Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XDC.of(String.class, String[].class), Map.class);

        N.println(map2);
    }

    @Test
    public void testSerialize_6() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("charArray", Array.of('a', 'b', 'c'));
        map.put("array", new Object[] { "abc", "123" });
        map.put("intList", N.asList(1, 2, 3));
        map.put("charList", N.asList('a', 'b', 'c'));
        map.put("list", N.asList("abc", "123"));

        final String str = xmlParser.serialize(map);

        N.println(str);

        Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XDC.of(String.class, String[].class), Map.class);

        N.println(map2);
    }

    @Test
    public void testSerialize_7() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("charArray", Array.of('a', 'b', 'c'));
        map.put("array", new Object[] { createAccount(Account.class), createAccount(Account.class) });
        map.put("intList", N.asList(1, 2, 3));
        map.put("charList", N.asList('a', 'b', 'c'));
        map.put("list", N.asList(createAccount(Account.class), createAccount(Account.class)));

        String str = xmlParser.serialize(map);

        N.println(str);

        Map<String, Object> map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XDC.of(Account.class), Map.class);

        N.println(map2);

        str = xmlParser.serialize(map2);

        N.println(str);

        map2 = xmlParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = xmlParser.deserialize(str, XDC.of(Account.class), Map.class);

        N.println(map2);
    }

    @Test
    public void testSerialize_8() throws Exception {
        final XBean xBean = createXBean();

        final XMLSerializationConfig sc = XSC.create().tagByPropertyName(false);
        final String str = xmlParser.serialize(xBean, sc);

        N.println(str);

        final XBean xBean2 = xmlParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize__10() throws Exception {
        final Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);

        XMLSerializationConfig xsc = XSC.of(false, false);
        xsc.setExclusion(Exclusion.NONE);
        String xml = xmlParser.serialize(account, xsc);
        N.println(xml);

        XMLDeserializationConfig xdc = XDC.create();
        Account account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());

        xsc = XSC.of(false, true);
        xsc.setExclusion(Exclusion.NONE);
        xml = xmlParser.serialize(account, xsc);
        N.println(xml);

        xdc = XDC.create();
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

        XMLSerializationConfig xsc = XSC.of(true, false);

        xsc.setExclusion(Exclusion.NONE);
        String xml = xmlParser.serialize(N.asMap(account), xsc);
        N.println(xml);

        XMLDeserializationConfig xdc = XDC.create();
        Account account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account2);

        xml = xmlParser.serialize(N.asList(account), xsc);
        N.println(xml);

        xdc = XDC.of(Account.class);
        final List<Account> accountList = xmlParser.deserialize(xml, xdc, List.class);
        N.println(accountList);

        xml = xmlParser.serialize(N.asArray(account), xsc);
        N.println(xml);

        xdc = XDC.of(Account.class);
        final Object[] accountArray = xmlParser.deserialize(xml, xdc, Object[].class);
        N.println(accountArray);

        xsc = XSC.of(false, true);
        xsc.setExclusion(Exclusion.NONE);
        xml = xmlParser.serialize(N.asMap(account), xsc);
        N.println(xml);

        xdc = XDC.create();
        account2 = xmlParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);
    }

    @Test
    public void testSerialize_ignorePropName() throws Exception {
        final Account account = createAccountWithContact(Account.class);

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("firstName", "contact"));
        final XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        final String xml = xmlParser.serialize(account);
        N.println(xml);

        final XMLDeserializationConfig xdc = XDC.create();
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

        final Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.asSet("firstName", "contact"));
        final XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        final String xml = xmlParser.serialize(Beans.deepBean2Map(account));
        N.println(xml);

        final XMLDeserializationConfig xdc = XDC.create();
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

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("firstName", "contact"));
        final XMLDeserializationConfig dc = XDC.create();
        dc.setIgnoredPropNames(ignoredPropNames);
        Account account2 = xmlParser.deserialize(xml, dc, Account.class);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());

        try {
            xmlParser.deserialize(xml, XDC.of(false, null), Account.class);
            fail("Should throw RuntimeException");
        } catch (final ParseException e) {

        }

        ignoredPropNames = N.asMap(Account.class, N.asSet("gui_1", "contact_1"));
        account2 = xmlParser.deserialize(xml, XDC.of(false, ignoredPropNames), Account.class);

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
            final String xml = xmlParser.serialize(N.asList("abc", "123"));

            N.println(xml);

            final List<?> a = xmlParser.deserialize(xml, List.class);

            N.println(N.stringOf(a));
        }

    }

    @Test
    public void testSerialize_list() throws Exception {
        final List<Account> accounts = N.asList(createAccount(Account.class), createAccount(Account.class));

        final String xml = xmlParser.serialize(accounts);

        N.println(xml);

        final XMLDeserializationConfig xdc = XDC.create().setElementType(Account.class);
        final List<Account> accounts2 = xmlParser.deserialize(xml, xdc, List.class);
        N.println(accounts2);

        assertTrue(N.equals(accounts, accounts2));

    }

    @Test
    public void testSerialize_MapEntity() throws Exception {
        final MapEntity account = new MapEntity("MapEntity", Beans.bean2Map(createAccount(Account.class)));

        final String xml = xmlParser.serialize(account);

        N.println(xml);

        final MapEntity accounts2 = xmlParser.deserialize(xml, MapEntity.class);
        N.println(accounts2);

    }
}
