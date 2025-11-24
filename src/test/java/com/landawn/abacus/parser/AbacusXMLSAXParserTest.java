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
import java.math.BigInteger;
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
import com.landawn.abacus.record.Element;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.types.WeekDay;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BufferedXMLWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

@Tag("old-test")
public class AbacusXMLSAXParserTest extends AbstractXMLParserTest {
    @Override
    protected Parser<?, ?> getParser() {
        return abacusXMLSAXParser;
    }

    @Test
    public void test_03() {
        Element e = new Element(100, "abc", N.asList(1L), N.asMap("a", BigInteger.ZERO), N.asMap("b", 2d), N.asList("Speaker"));
        Map<String, Object> map = Beans.bean2Map(e);

        N.println(map);

        Element e2 = Beans.map2Bean(map, Element.class);

        assertEquals(e, e2);

        assertEquals(e, jsonParser.deserialize(jsonParser.serialize(e), Element.class));

        assertEquals(e, xmlParser.deserialize(xmlParser.serialize(e), Element.class));
        assertEquals(e, xmlDOMParser.deserialize(xmlDOMParser.serialize(e), Element.class));

        assertEquals(e, abacusXMLParser.deserialize(abacusXMLParser.serialize(e), Element.class));
        assertEquals(e, abacusXMLStAXParser.deserialize(abacusXMLStAXParser.serialize(e), Element.class));
        assertEquals(e, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(e), Element.class));
        assertEquals(e, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(e), Element.class));

    }

    @Test
    public void test_prettyFormat_2() {
        Account account = createAccount(Account.class);

        GenericEntity genericBean = new GenericEntity();
        genericBean.setBooleanList(N.asList(true, false));
        genericBean.setCharList(N.asList('a', 'b', '黎'));
        genericBean.setIntList(N.asList(1, 2, 3));
        genericBean.setStringList(N.asList("abc", "123"));
        genericBean.setAccountList(N.asList(account));
        Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        XMLSerializationConfig xsc = XSC.create().prettyFormat(true);
        String str = abacusXMLSAXParser.serialize(genericBean, xsc);

        N.println(str);

        GenericEntity genericBean2 = abacusXMLSAXParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
    }

    @Test
    public void test_prettyFormat_3() {
        Account account = createAccount(Account.class);

        GenericEntity genericBean = new GenericEntity();
        genericBean.setBooleanList(N.asList(true, false));
        genericBean.setCharList(N.asList('a', 'b', '黎'));
        genericBean.setIntList(N.asList(1, 2, 3));
        genericBean.setStringList(N.asList("abc", "123"));
        genericBean.setAccountList(N.asList(account));
        Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        XMLSerializationConfig xsc = XSC.create().prettyFormat(true);
        Map<String, Object> props = Beans.bean2Map(genericBean);
        String str = abacusXMLSAXParser.serialize(props, xsc);

        N.println(str);

        XMLDeserializationConfig xdc = XDC.create().setValueType("account", Account.class);
        Map<String, Object> props2 = abacusXMLSAXParser.deserialize(str, xdc, Map.class);
        N.println(props);
        N.println(props2);

    }

    @Test
    public void test_GenericEntity() {
        Account account = createAccount(Account.class);

        GenericEntity genericBean = new GenericEntity();
        genericBean.setAccountList(N.asList(account));
        Map<String, Account> map = N.asMap(account.getFirstName(), account);
        genericBean.setAccountMap(map);

        String str = abacusXMLSAXParser.serialize(genericBean);

        N.println(str);

        GenericEntity genericBean2 = abacusXMLSAXParser.deserialize(str, GenericEntity.class);
        N.println(genericBean2);

        assertEquals(genericBean, genericBean2);
    }

    @Test
    public void test_node_by_name() throws SAXException, IOException {
        Account account = createAccount(Account.class);

        String str = abacusXMLSAXParser.serialize(account);
        InputStream is = IOUtil.string2InputStream(str);

        Map<String, Type<?>> nodeClasses = Map.of("account", Type.of(Account.class));

        Account account2 = abacusXMLSAXParser.deserialize(is, null, nodeClasses);

        IOUtil.close(is);

        N.println(account2);

        assertEquals(account, account2);

        Reader reader = new StringReader(str);
        account2 = abacusXMLSAXParser.deserialize(reader, null, nodeClasses);

        IOUtil.close(reader);

        N.println(account2);

        assertEquals(account, account2);

        DocumentBuilder docBuilder = XmlUtil.createDOMParser();
        Document doc = docBuilder.parse(IOUtil.string2InputStream(str));
        account2 = abacusXMLSAXParser.deserialize(doc.getDocumentElement(), null, nodeClasses);

        N.println(account2);

        assertEquals(account, account2);
    }

    @Test
    public void test_BufferedWriter() {
        Writer writer = new StringWriter();
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(writer);
        Account account = createAccount(Account.class);
        abacusXMLSAXParser.serialize(account, bw);

        N.println(writer.toString());

        Objectory.recycle(bw);
    }

    @Test
    public void test_BufferedWriter_2() {
        Writer writer = new StringWriter();
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(writer);
        Account account = null;
        abacusXMLSAXParser.serialize(account, bw);

        assertEquals(Strings.EMPTY, writer.toString());

        Objectory.recycle(bw);
    }

    @Test
    public void test_null() {
        String nullElement = null;

        String[] array = N.asArray(nullElement);
        String str = abacusXMLSAXParser.serialize(array);
        N.println(str);

        String[] array2 = abacusXMLSAXParser.deserialize(str, String[].class);
        assertTrue(N.equals(array, array2));

        List<String> list = N.asList(nullElement);
        str = abacusXMLSAXParser.serialize(list);
        N.println(str);

        List<String> list2 = abacusXMLSAXParser.deserialize(str, List.class);
        N.println(list2);

        Map<String, Object> map = N.asMap(nullElement, nullElement);
        XMLSerializationConfig jsc = XSC.create().setExclusion(Exclusion.NONE);
        str = abacusXMLSAXParser.serialize(map, jsc);
        N.println(str);

        Map<String, Object> map2 = abacusXMLSAXParser.deserialize(str, Map.class);
        N.println(map2);

        map2 = (Map<String, Object>) abacusXMLSAXParser.deserialize(str, Object.class);
        N.println(map2);

        map2 = (Map<String, Object>) abacusXMLSAXParser.deserialize(str, abacusXMLSAXParser.getClass());
        N.println(map2);

        try {
            abacusXMLSAXParser.serialize(abacusXMLSAXParser);
            fail("Should throw RuntimeException");
        } catch (ParseException e) {

        }
    }

    @Test
    public void test_null_1() {
        String nullElement = null;
        String str = abacusXMLSAXParser.serialize(nullElement);
        assertNull(abacusXMLSAXParser.deserialize(str, String.class));
    }

    @Test
    public void test_null_2() {
        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        String nullElement = null;

        Account[] array = { account, null };
        String str = abacusXMLSAXParser.serialize(array);
        N.println(str);

        Object[] array2 = abacusXMLSAXParser.deserialize(str, XDC.of(Account.class), Account[].class);
        assertTrue(N.equals(array, array2));

        List<?> list = N.asList(account, nullElement);
        str = abacusXMLSAXParser.serialize(list);
        N.println(str);

        List<String> list2 = abacusXMLSAXParser.deserialize(str, XDC.of(Account.class), List.class);
        assertTrue(N.equals(list, list2));
        N.println(list2);

        Map<String, Object> map = N.asMap(nullElement, account);
        XMLSerializationConfig xsc = XSC.create().setExclusion(Exclusion.NONE);
        str = abacusXMLSAXParser.serialize(map, xsc);
        N.println(str);

        XMLDeserializationConfig xdc = XDC.of(Account.class);
        xdc.setValueType("account", Account.class);
        Map<String, Object> map2 = abacusXMLSAXParser.deserialize(str, xdc, Map.class);
        N.println(map2);
    }

    @Test
    public void test_null_3() {
        Account account = new Account();

        String str = abacusXMLSAXParser.serialize(account);

        account.setId(0);
        account.setFirstName("firstName");
        account.setLastName(null);

        str = abacusXMLSAXParser.serialize(account, XSC.of(Exclusion.DEFAULT, null));
        N.println(str);

        str = abacusXMLSAXParser.serialize(Beans.bean2Map(account), XSC.of(Exclusion.DEFAULT, null));
        N.println(str);

        Map<String, Object> map = Beans.bean2Map(account);
        map.put("lastName", null);
        map.put("account", account);

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.asSet("id"));

        XMLSerializationConfig xsc = XSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = abacusXMLSAXParser.serialize(map, xsc);
        N.println(str);

        XMLDeserializationConfig xdc = XDC.of(Account.class);
        xdc.setValueType("account", Account.class);
        Map<String, Object> map2 = abacusXMLSAXParser.deserialize(str, xdc, Map.class);
        N.println(map2);

        str = abacusXMLSAXParser.serialize(N.asList(map), xsc);
        N.println(str);

        xdc = XDC.of(Map.class);
        xdc.setValueType("account", Account.class);
        List<?> list = abacusXMLSAXParser.deserialize(str, xdc, List.class);
        N.println(list);

        Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.asList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));

        xsc = XSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = abacusXMLSAXParser.serialize(map3, xsc);
        N.println(str);

        xdc = XDC.create();
        xdc.setValueType("account", Account.class);
        N.println(abacusXMLSAXParser.deserialize(str, xdc, Map.class));

        xsc = XSC.of(Exclusion.DEFAULT, ignoredPropNames).prettyFormat(true);
        str = abacusXMLSAXParser.serialize(map3, xsc);
        N.println(str);

        XBean xBean = createXBean();
        str = abacusXMLSAXParser.serialize(xBean, xsc);
        N.println(str);

        N.println(abacusXMLSAXParser.deserialize(str, XBean.class));
    }

    @Test
    public void test_config() {
        XMLSerializationConfig xsc1 = XSC.create();
        XMLSerializationConfig xsc2 = XSC.create();

        N.println(xsc1);

        assertTrue(N.asSet(xsc1).contains(xsc2));

        XMLDeserializationConfig xdc1 = XDC.of(String.class, String.class, true, null);
        XMLDeserializationConfig xdc2 = XDC.of(String.class, String.class, true, null);

        N.println(xdc1);

        assertTrue(N.asSet(xdc1).contains(xdc2));
    }

    @Test
    public void test_transient() {
        TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");

        String str = abacusXMLSAXParser.serialize(bean);

        N.println(str);

        assertTrue(str.indexOf("abc") == -1);

        XMLSerializationConfig config = XSC.create().skipTransientField(false);
        str = abacusXMLSAXParser.serialize(bean, config);

        N.println(str);

        assertTrue(str.indexOf("abc") >= 0);

        assertTrue(bean.equals(abacusXMLSAXParser.deserialize(str, TransientBean.class)));
    }

    @Test
    public void test_prettyFormat_1() {
        Account account = createAccountWithContact(Account.class);
        account.setId(100);

        XMLSerializationConfig config = XSC.create().prettyFormat(true).setIndentation("    ");

        String str = abacusXMLSAXParser.serialize(account, config);
        N.println("============account=====================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(N.asArray(account, account), config);
        N.println("============Array.of(account, account)=================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(N.asList(account, account), config);
        N.println("============N.asList(account, account)===================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(Beans.deepBean2Map(account), config);
        N.println("============(N.deepBean2Map(account)==================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(new Object[] { Beans.deepBean2Map(account), account }, config);
        N.println("============Array.of(N.deepBean2Map(account), account)===============================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(N.asList(Beans.deepBean2Map(account), account), config);
        N.println("============N.asList(N.deepBean2Map(account), account)================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(new Object[] { N.asArray(account, account), N.asList(account, account) }, config);
        N.println("============Array.of(Array.of(account, account), N.asList(account, account))==========================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(N.asList(N.asArray(account, account), N.asList(account, account)), config);
        N.println("============N.asList(Array.of(account, account), N.asList(account, account))===========================================");
        N.println(str);
        N.println("========================================================================================================================");

        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string黎< > </ <//、");

        xBean.setWeekDay(WeekDay.THURSDAY);

        str = abacusXMLSAXParser.serialize(xBean, config);

        N.println("============xBean=======================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(N.asArray("abc", "123"), config);

        N.println("========================================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");

        str = abacusXMLSAXParser.serialize(N.asList("abc", "123"), config);

        N.println("========================================================================================================================");
        N.println(str);
        N.println("========================================================================================================================");
    }

    @Test
    public void testSerialize_tagName() throws Exception {
        Account account = createAccountWithContact(Account.class);
        String xml = abacusXMLSAXParser.serialize(account);
        N.println(xml);
        N.println(account);

        xml = "<unknown><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><unknown><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></unknown></contact></unknown>";
        Account account2 = abacusXMLSAXParser.deserialize(xml, Account.class);
        N.println(account2);
    }

    public void estSerialize_tagName2() throws Exception {
        Account account = createAccountWithContact(Account.class);
        String xml = abacusXMLSAXParser.serialize(account);
        N.println(xml);
        N.println(account);

        xml = "<map><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><accountContact><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact></map>";
        Map<String, String> account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<map><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><map><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></map></contact></map>";
        account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<list><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><list><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></list></contact></list>";
        account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<key><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><key><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></key></contact></key>";
        account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<value><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><value><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></value></contact></value>";
        account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<entry><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><entry><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></entry></contact></entry>";
        account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<e><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><e><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></e></contact></e>";
        account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);

        xml = "<unknown><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui><emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime><contact><unknown><id>2801</id><accountId>1002759403</accountId><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></unknown></contact></unknown>";
        account2 = abacusXMLSAXParser.deserialize(xml, Map.class);
        N.println(account2);
    }

    @Test
    public void testSerialize_array() throws Exception {
        Account[] accounts = N.asArray(createAccount(Account.class), createAccount(Account.class));

        String xml = abacusXMLSAXParser.serialize(accounts);

        N.println(xml);

        Account[] accounts2 = abacusXMLSAXParser.deserialize(xml, Account[].class);
        N.println(accounts2);

        assertTrue(N.equals(accounts, accounts2));
    }

    @Test
    public void testSerialize_simple_type() throws Exception {
        {
            String xml = abacusXMLSAXParser.serialize(new Object[] {});

            N.println(xml);

            String[] a = abacusXMLSAXParser.deserialize(xml, String[].class);

            N.println(N.stringOf(a));
        }

        {
            String xml = abacusXMLSAXParser.serialize(N.asArray("abc", "123"));

            N.println(xml);

            String[] a = abacusXMLSAXParser.deserialize(xml, String[].class);

            N.println(N.stringOf(a));
        }

        {
            String xml = abacusXMLSAXParser.serialize(new ArrayList<>());

            N.println(xml);

            List<?> a = abacusXMLSAXParser.deserialize(xml, List.class);

            N.println(N.stringOf(a));
        }

        {
            String xml = abacusXMLSAXParser.serialize(N.asList("abc", "123"));

            N.println(xml);

            List<?> a = abacusXMLSAXParser.deserialize(xml, List.class);

            N.println(N.stringOf(a));
        }

    }

    @Test
    public void testSerialize_1() throws Exception {
        XBean xBean = createXBean();

        XMLSerializationConfig sc = XSC.create().setExclusion(Exclusion.NONE);
        String str = abacusXMLSAXParser.serialize(xBean, sc);

        N.println(str);

        XBean xBean2 = abacusXMLSAXParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_2() throws Exception {
        XBean xBean = createXBean();

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.writeTypeInfo(true);

        String str = abacusXMLSAXParser.serialize(xBean, sc);

        N.println(str);

        XBean xBean2 = abacusXMLSAXParser.deserialize(str, XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_3() throws Exception {
        XBean xBean = createXBean();

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.setExclusion(Exclusion.NONE);

        String str = abacusXMLSAXParser.serialize(xBean, sc);

        N.println(str);

        XBean xBean2 = abacusXMLSAXParser.deserialize(str, XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_4() throws Exception {
        XBean xBean = createXBean();

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(XBean.class, N.asSet("typeBoolean", "typeShort", "typeLong"));
        XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);
        sc.setExclusion(Exclusion.NONE);

        String str = abacusXMLSAXParser.serialize(xBean, sc);

        N.println(str);

        Document doc = XmlUtil.createDOMParser().parse(IOUtil.string2InputStream(str));
        XBean xBean2 = abacusXMLSAXParser.deserialize(doc.getDocumentElement(), XBean.class);

        xBean.setTypeBoolean(false);
        xBean.setTypeShort((short) 0);
        xBean.setTypeLong(0);

        N.println(xBean);
        N.println(xBean2);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testSerialize_5() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("array", N.asArray("abc", "123"));
        map.put("list", N.asList("abc", "123"));

        String str = abacusXMLSAXParser.serialize(map);

        N.println(str);

        Map<String, Object> map2 = abacusXMLSAXParser.deserialize(str, Map.class);

        N.println(map2);

        map2 = abacusXMLSAXParser.deserialize(str, XDC.of(String.class, String[].class), Map.class);

        N.println(map2);
    }

    @Test
    public void testSerialize_8() throws Exception {
        XBean xBean = createXBean();

        XMLSerializationConfig sc = XSC.create().tagByPropertyName(false);
        String str = abacusXMLSAXParser.serialize(xBean, sc);

        N.println(str);

        XBean xBean2 = abacusXMLSAXParser.deserialize(str, XBean.class);

        N.println(xBean);
        N.println(xBean2);
    }

    @Test
    public void testSerialize__10() throws Exception {
        Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);

        XMLSerializationConfig xsc = XSC.of(false, false);
        xsc.setExclusion(Exclusion.NONE);
        String xml = abacusXMLSAXParser.serialize(account, xsc);
        N.println(xml);

        XMLDeserializationConfig xdc = XDC.create();
        Account account2 = abacusXMLSAXParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNotNull(account2.getContact());

        xsc = XSC.of(false, true);
        xsc.setExclusion(Exclusion.NONE);
        xml = abacusXMLSAXParser.serialize(account, xsc);
        N.println(xml);

        xdc = XDC.create();
        account2 = abacusXMLSAXParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNotNull(account2.getContact());
    }

    @Test
    public void testSerialize__11() throws Exception {
        Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);
        N.println(account);

        XMLSerializationConfig xsc = XSC.of(true, false);

        xsc.setExclusion(Exclusion.NONE);
        String xml = abacusXMLSAXParser.serialize(N.asMap(account), xsc);
        N.println(xml);

        XMLDeserializationConfig xdc = XDC.create();
        xdc.setValueType("accountContact", account.getContact().getClass());
        Map<String, Object> account2 = abacusXMLSAXParser.deserialize(xml, xdc, Map.class);

        N.println(account2);

        xml = abacusXMLSAXParser.serialize(N.asList(account), xsc);
        N.println(xml);

        xdc = XDC.of(Account.class);
        List<Account> accountList = abacusXMLSAXParser.deserialize(xml, xdc, List.class);
        N.println(accountList);

        xml = abacusXMLSAXParser.serialize(N.asArray(account), xsc);
        N.println(xml);

        xdc = XDC.of(Account.class);
        Object[] accountArray = abacusXMLSAXParser.deserialize(xml, xdc, Object[].class);
        N.println(accountArray);

        xsc = XSC.of(false, true);
        xsc.setExclusion(Exclusion.NONE);
        xml = abacusXMLSAXParser.serialize(N.asMap(account), xsc);
        N.println(xml);

        xdc = XDC.create();
        xdc.setValueType("accountContact", account.getContact().getClass());
        account2 = abacusXMLSAXParser.deserialize(xml, xdc, Map.class);

        N.println(account);
        N.println(account2);
    }

    @Test
    public void testSerialize__12() throws Exception {
        Map<Object, Object> map = new HashMap<>();

        map.put(N.asList("abc"), N.asList(123));

        String str = abacusXMLSAXParser.serialize(map);
        N.println(str);

        Map<Object, Object> map2 = abacusXMLSAXParser.deserialize(str, Map.class);
        N.println(map2);

        str = abacusXMLSAXParser.serialize(map, XSC.of(true, false));
        N.println(str);

        map2 = abacusXMLSAXParser.deserialize(str, Map.class);
        N.println(map2);

        map = new HashMap<>();
        map.put(N.asArray("abc"), Array.of(123));

        str = abacusXMLSAXParser.serialize(map, XSC.of(true, false));
        N.println(str);

        map2 = abacusXMLSAXParser.deserialize(str, Map.class);
        N.println(map2);

    }

    @Test
    public void testSerialize_ignorePropName() throws Exception {
        Account account = createAccountWithContact(Account.class);

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("firstName", "contact"));
        XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        String xml = abacusXMLSAXParser.serialize(account);
        N.println(xml);

        XMLDeserializationConfig xdc = XDC.create();
        xdc.setIgnoredPropNames(ignoredPropNames);
        Account account2 = abacusXMLSAXParser.deserialize(xml, xdc, Account.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());
    }

    @Test
    public void testSerialize_ignorePropName_2() throws Exception {
        Account account = createAccountWithContact(Account.class);

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Map.class, N.asSet("firstName", "contact"));
        XMLSerializationConfig sc = XSC.create();
        sc.setIgnoredPropNames(ignoredPropNames);

        String xml = abacusXMLSAXParser.serialize(Beans.deepBean2Map(account));
        N.println(xml);

        XMLDeserializationConfig xdc = XDC.create();
        xdc.setIgnoredPropNames(ignoredPropNames);
        Map<String, Object> account2 = abacusXMLSAXParser.deserialize(xml, xdc, Map.class);

        N.println(account);
        N.println(account2);

        assertNull(account2.get("firstName"));
        assertNull(account2.get("contact"));
    }

    @Test
    public void testSerialize_unknowPropNames() throws Exception {
        String xml = "<account><gui_1>9b1b4964298a4868a4ab95ccf6a5f987</gui_1><emailAddress>48c6a440fa114de28fad1bf04fa66090@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1413839551838</birthDate><lastUpdateTime>1413839551838</lastUpdateTime><createdTime>1413839551838</createdTime><contact_1><accountContact><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact_1></account>";
        N.println(xml);

        Map<Class<?>, Set<String>> ignoredPropNames = N.asMap(Account.class, N.asSet("firstName", "contact"));
        XMLDeserializationConfig dc = XDC.create();
        dc.setIgnoredPropNames(ignoredPropNames);
        Account account2 = abacusXMLSAXParser.deserialize(xml, dc, Account.class);
        N.println(account2);

        assertNull(account2.getFirstName());
        assertNull(account2.getContact());

        try {
            abacusXMLSAXParser.deserialize(xml, XDC.of(false, null), Account.class);
            fail("Should throw RuntimeException");
        } catch (ParseException e) {

        }

        ignoredPropNames = N.asMap(Account.class, N.asSet("gui_1", "contact_1"));
        account2 = abacusXMLSAXParser.deserialize(xml, XDC.of(false, ignoredPropNames), Account.class);

        assertNotNull(account2.getFirstName());
        assertNull(account2.getContact());
    }
}
