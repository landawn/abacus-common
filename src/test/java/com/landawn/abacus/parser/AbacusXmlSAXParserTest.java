package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.entity.GenericEntity;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.record.Element;
import testfixtures.types.WeekDay;

public class AbacusXmlSAXParserTest extends AbstractXmlParserTest {

    @Override
    protected Parser<?, ?> getParser() {
        return abacusXMLSAXParser;
    }

    @Test
    public void testDeserialize_ObjectArray_withoutElementType_returnsObjectArrayLikeStAX() {
        String xml = "<array><e type=\"Integer\">1</e><e type=\"String\">two</e></array>";
        Object[] viaStAX = abacusXMLStAXParser.deserialize(xml, Object[].class);
        Object[] result = abacusXMLSAXParser.deserialize(xml, Object[].class);
        assertEquals(Object[].class, result.getClass());
        assertEquals(2, result.length);
        assertEquals(Object[].class, viaStAX.getClass());
        assertTrue(N.deepEquals(viaStAX, result));
    }

    @Test
    public void testElementRoundTripAcrossParsers() {
        Element e = new Element(100, "abc", N.toList(1L), N.asMap("a", BigInteger.ZERO), N.asMap("b", 2d), N.toList("Speaker"));
        assertEquals(e, Beans.mapToBean(Beans.beanToMap(e), Element.class));
        assertEquals(e, jsonParser.deserialize(jsonParser.serialize(e), Element.class));
        assertEquals(e, xmlParser.deserialize(xmlParser.serialize(e), Element.class));
        assertEquals(e, xmlDOMParser.deserialize(xmlDOMParser.serialize(e), Element.class));
        assertEquals(e, abacusXmlParser.deserialize(abacusXmlParser.serialize(e), Element.class));
        assertEquals(e, abacusXMLStAXParser.deserialize(abacusXMLStAXParser.serialize(e), Element.class));
        assertEquals(e, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(e), Element.class));
        assertEquals(e, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(e), Element.class));
    }

    @Test
    public void testNestedArray() {
        String xml = "<array><e><array><e>a</e><e>b</e></array></e><e><array><e>c</e></array></e></array>";
        String[][] result = abacusXMLSAXParser.deserialize(xml, String[][].class);
        assertEquals(2, result.length);
        assertTrue(N.equals(new String[] { "a", "b" }, result[0]));
        assertTrue(N.equals(new String[] { "c" }, result[1]));
        assertTrue(N.deepEquals(result, abacusXMLStAXParser.deserialize(xml, String[][].class)));
        assertTrue(N.deepEquals(result, abacusXMLDOMParser.deserialize(xml, String[][].class)));
    }

    @Test
    public void testPrettyFormatAndGenericEntity() {
        Account account = createAccount(Account.class);
        GenericEntity genericBean = new GenericEntity();
        genericBean.setBooleanList(N.toList(true, false));
        genericBean.setCharList(N.toList('a', 'b', '黎'));
        genericBean.setIntList(N.toList(1, 2, 3));
        genericBean.setStringList(N.toList("abc", "123"));
        genericBean.setAccountList(N.toList(account));
        genericBean.setAccountMap(N.asMap(account.getFirstName(), account));

        String pretty = abacusXMLSAXParser.serialize(genericBean, XmlSerConfig.create().setPrettyFormat(true));
        assertEquals(genericBean, abacusXMLSAXParser.deserialize(pretty, GenericEntity.class));

        Map<String, Object> props2 = abacusXMLSAXParser.deserialize(
                abacusXMLSAXParser.serialize(Beans.beanToMap(genericBean), XmlSerConfig.create().setPrettyFormat(true)),
                XmlDeserConfig.create().setValueType("account", Account.class), Map.class);
        assertTrue(props2.containsKey("stringList") || props2.containsKey("booleanList"));

        GenericEntity slim = new GenericEntity();
        slim.setAccountList(N.toList(account));
        slim.setAccountMap(N.asMap(account.getFirstName(), account));
        assertEquals(slim, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(slim), GenericEntity.class));
    }

    @Test
    public void testPrettyFormat() {
        Account account = createAccountWithContact(Account.class);
        account.setId(100);
        XmlSerConfig config = XmlSerConfig.create().setPrettyFormat(true).setIndentation("    ");
        String accountXml = abacusXMLSAXParser.serialize(account, config);
        assertTrue(accountXml.contains("\n"));
        assertEquals(100, abacusXMLSAXParser.deserialize(accountXml, Account.class).getId());
        assertEquals(2, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(N.asArray(account, account), config), Account[].class).length);
        assertEquals(2,
                abacusXMLSAXParser
                        .deserialize(abacusXMLSAXParser.serialize(N.toList(account, account), config), XmlDeserConfig.create().setElementType(Account.class),
                                List.class)
                        .size());

        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2(5L);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);
        xBean.setTypeString(">string黎< > </ <//、");
        xBean.setWeekDay(WeekDay.THURSDAY);
        XBean roundTrip = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(xBean, config), XBean.class);
        assertEquals(WeekDay.THURSDAY, roundTrip.getWeekDay());
        assertEquals('黎', roundTrip.getTypeChar());
        assertEquals(xBean.getTypeString(), roundTrip.getTypeString());
        assertTrue(N.equals(N.asArray("abc", "123"),
                abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(N.asArray("abc", "123"), config), String[].class)));
        assertEquals(N.toList("abc", "123"), abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(N.toList("abc", "123"), config), List.class));
    }

    @Test
    public void testNodeByName() throws SAXException, IOException {
        Account account = createAccount(Account.class);
        String str = abacusXMLSAXParser.serialize(account);
        Map<String, Type<?>> nodeClasses = Map.of("account", Type.of(Account.class));
        InputStream is = IOUtil.stringToInputStream(str);
        assertEquals(account, abacusXMLSAXParser.deserialize(is, null, nodeClasses));
        IOUtil.close(is);
        Reader reader = new StringReader(str);
        assertEquals(account, abacusXMLSAXParser.deserialize(reader, null, nodeClasses));
        IOUtil.close(reader);
        Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        assertEquals(account, abacusXMLSAXParser.deserialize(doc.getDocumentElement(), null, nodeClasses));
    }

    @Test
    public void testBufferedWriter() {
        Writer writer = new StringWriter();
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(writer);
        Account account = createAccount(Account.class);
        abacusXMLSAXParser.serialize(account, bw);
        Objectory.recycle(bw);
        String written = writer.toString();
        assertTrue(written.contains(account.getFirstName()));
        assertEquals(account, abacusXMLSAXParser.deserialize(written, Account.class));

        Writer empty = new StringWriter();
        BufferedXmlWriter emptyBw = Objectory.createBufferedXmlWriter(empty);
        abacusXMLSAXParser.serialize((Account) null, emptyBw);
        assertEquals(Strings.EMPTY, empty.toString());
        Objectory.recycle(emptyBw);
    }

    @Test
    public void testNull() {
        assertNull(abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize((String) null), String.class));
        String[] array = N.asArray((String) null);
        assertTrue(N.equals(array, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(array), String[].class)));
        List<String> list = N.toList((String) null);
        assertEquals(list, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(list), List.class));

        String str = abacusXMLSAXParser.serialize(N.asMap((String) null, (Object) null), XmlSerConfig.create().setExclusion(Exclusion.NONE));
        Map<String, Object> map2 = abacusXMLSAXParser.deserialize(str, Map.class);
        map2 = (Map<String, Object>) abacusXMLSAXParser.deserialize(str, Object.class);
        map2 = (Map<String, Object>) abacusXMLSAXParser.deserialize(str, abacusXMLSAXParser.getClass());
        assertThrows(ParsingException.class, () -> abacusXMLSAXParser.serialize(abacusXMLSAXParser));
        assertNotNull(map2);

        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Account[] mixed = { account, null };
        assertTrue(N.equals(mixed,
                abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(mixed), XmlDeserConfig.create().setElementType(Account.class), Account[].class)));
        List<?> mixedList = N.toList(account, (String) null);
        assertTrue(N.equals(mixedList,
                abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(mixedList), XmlDeserConfig.create().setElementType(Account.class), List.class)));
        XmlDeserConfig xdc = XmlDeserConfig.create().setElementType(Account.class);
        xdc.setValueType("account", Account.class);
        assertNotNull(abacusXMLSAXParser.deserialize(
                abacusXMLSAXParser.serialize(N.asMap((String) null, account), XmlSerConfig.create().setExclusion(Exclusion.NONE)), xdc, Map.class));

        account.setId(0);
        account.setLastName(null);
        Map<String, Object> map = Beans.beanToMap(account);
        map.put("lastName", null);
        map.put("account", account);
        Map<Class<?>, Set<String>> ignored = N.asMap(Map.class, N.toSet("id"));
        XmlSerConfig xsc = XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignored).setPrettyFormat(true);
        xdc = XmlDeserConfig.create().setElementType(Account.class);
        xdc.setValueType("account", Account.class);
        assertNotNull(abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(map, xsc), xdc, Map.class));
        Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.toList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));
        xdc = XmlDeserConfig.create();
        xdc.setValueType("account", Account.class);
        assertNotNull(abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(map3, xsc), xdc, Map.class));
        String xBeanXml = abacusXMLSAXParser.serialize(createXBean(), xsc);
        assertNotNull(abacusXMLSAXParser.deserialize(xBeanXml, XBean.class));
        assertTrue(xBeanXml.contains("firstName"));
    }

    @Test
    public void testTransient() {
        TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");
        assertTrue(abacusXMLSAXParser.serialize(bean).indexOf("abc") < 0);
        String withTransient = abacusXMLSAXParser.serialize(bean, XmlSerConfig.create().setSkipTransientField(false));
        assertTrue(withTransient.indexOf("abc") >= 0);
        assertEquals(bean, abacusXMLSAXParser.deserialize(withTransient, TransientBean.class));
    }

    @Test
    public void testSerializeTagName() {
        String xml = "<unknown><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui>"
                + "<emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress>"
                + "<firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName>"
                + "<birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime>"
                + "<contact><unknown><id>2801</id><accountId>1002759403</accountId><address>ca, US</address>"
                + "<city>sunnyvale</city><state>CA</state><country>U.S.</country></unknown></contact></unknown>";
        Account account2 = abacusXMLSAXParser.deserialize(xml, Account.class);
        assertEquals("firstName", account2.getFirstName());
        assertEquals(1002759403L, account2.getId());
    }

    @Test
    public void testSerializeXBean() throws Exception {
        XBean xBean = createXBean();
        assertEquals(xBean,
                abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(xBean, XmlSerConfig.create().setExclusion(Exclusion.NONE)), XBean.class));
        assertEquals(xBean,
                abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(xBean, XmlSerConfig.create().setTagByPropertyName(false)), XBean.class));

        Map<Class<?>, Set<String>> ignored = N.asMap(XBean.class, N.toSet("typeBoolean", "typeShort", "typeLong"));
        XBean ignoredCopy = createXBean();
        XBean restored = abacusXMLSAXParser
                .deserialize(abacusXMLSAXParser.serialize(ignoredCopy, XmlSerConfig.create().setIgnoredPropNames(ignored).setWriteTypeInfo(true)), XBean.class);
        ignoredCopy.setTypeBoolean(false);
        ignoredCopy.setTypeShort((short) 0);
        ignoredCopy.setTypeLong(0);
        assertEquals(ignoredCopy, restored);

        XBean none = createXBean();
        XBean noneRestored = abacusXMLSAXParser
                .deserialize(abacusXMLSAXParser.serialize(none, XmlSerConfig.create().setIgnoredPropNames(ignored).setExclusion(Exclusion.NONE)), XBean.class);
        none.setTypeBoolean(false);
        none.setTypeShort((short) 0);
        none.setTypeLong(0);
        assertEquals(none, noneRestored);

        XBean source = createXBean();
        String str = abacusXMLSAXParser.serialize(source, XmlSerConfig.create().setIgnoredPropNames(ignored).setExclusion(Exclusion.NONE));
        Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        XBean fromNode = abacusXMLSAXParser.deserialize(doc.getDocumentElement(), XBean.class);
        assertEquals(source.getFirstName(), fromNode.getFirstName());
        assertFalse(fromNode.getTypeBoolean());
        assertEquals((short) 0, fromNode.getTypeShort());
        assertEquals(0L, fromNode.getTypeLong());

        Map<String, Object> map = new HashMap<>();
        map.put("array", N.asArray("abc", "123"));
        map.put("list", N.toList("abc", "123"));
        Map<String, Object> restoredMap = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(map),
                XmlDeserConfig.create().setMapKeyType(String.class).setMapValueType(String[].class), Map.class);
        assertTrue(restoredMap.containsKey("list") || restoredMap.containsKey("array"));
    }

    @Test
    public void testSerializeNullFirstName() {
        Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);
        XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        Account restored = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(account, xsc), XmlDeserConfig.create(), Account.class);
        assertNull(restored.getFirstName());
        assertNotNull(restored.getContact());

        xsc = XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        XmlDeserConfig xdc = XmlDeserConfig.create();
        xdc.setValueType("accountContact", account.getContact().getClass());
        Map<String, Object> asMap = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(Beans.deepBeanToMap(account), xsc), xdc, Map.class);
        List<Account> accountList = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(N.toList(account), xsc),
                XmlDeserConfig.create().setElementType(Account.class), List.class);
        Object[] accountArray = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(N.asArray(account), xsc),
                XmlDeserConfig.create().setElementType(Account.class), Object[].class);
        assertNotNull(asMap);
        assertEquals(1, accountList.size());
        assertEquals(1, accountArray.length);
        assertNull(accountList.get(0).getFirstName());
    }

    @Test
    public void testSerializeCollectionKeys() {
        Map<Object, Object> map = new HashMap<>();
        map.put(N.toList("abc"), N.toList(123));
        Map<Object, Object> restored = abacusXMLSAXParser
                .deserialize(abacusXMLSAXParser.serialize(map, XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false)), Map.class);
        map = new HashMap<>();
        map.put(N.asArray("abc"), Array.of(123));
        String str = abacusXMLSAXParser.serialize(map, XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false));
        restored = abacusXMLSAXParser.deserialize(str, Map.class);
        assertFalse(restored.isEmpty());
        assertTrue(str.contains("abc"));
    }

    @Test
    public void testIgnorePropNames() {
        Account account = createAccountWithContact(Account.class);
        Map<Class<?>, Set<String>> ignored = N.asMap(Account.class, N.toSet("firstName", "contact"));
        Account ignoredAccount = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(account), XmlDeserConfig.create().setIgnoredPropNames(ignored),
                Account.class);
        assertNull(ignoredAccount.getFirstName());
        assertNull(ignoredAccount.getContact());

        Map<Class<?>, Set<String>> mapIgnored = N.asMap(Map.class, N.toSet("firstName", "contact"));
        Map<String, Object> ignoredMap = abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(Beans.deepBeanToMap(account)),
                XmlDeserConfig.create().setIgnoredPropNames(mapIgnored), Map.class);
        assertNull(ignoredMap.get("firstName"));
        assertNull(ignoredMap.get("contact"));

        String xml = "<account><gui_1>9b1b4964298a4868a4ab95ccf6a5f987</gui_1><emailAddress>48c6a440fa114de28fad1bf04fa66090@earth.com</emailAddress>"
                + "<firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName>"
                + "<birthDate>1413839551838</birthDate><lastUpdateTime>1413839551838</lastUpdateTime><createdTime>1413839551838</createdTime>"
                + "<contact_1><accountContact><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact_1></account>";
        assertNull(abacusXMLSAXParser.deserialize(xml, XmlDeserConfig.create().setIgnoredPropNames(ignored), Account.class).getFirstName());
        assertThrows(ParsingException.class,
                () -> abacusXMLSAXParser.deserialize(xml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false), Account.class));
        Account allowed = abacusXMLSAXParser.deserialize(xml,
                XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames(N.asMap(Account.class, N.toSet("gui_1", "contact_1"))),
                Account.class);
        assertNotNull(allowed.getFirstName());
        assertNull(allowed.getContact());
    }

    @Test
    public void testSerializeCollections() {
        Account[] accounts = N.asArray(createAccount(Account.class), createAccount(Account.class));
        assertTrue(N.equals(accounts, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(accounts), Account[].class)));
        assertEquals(0, abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(new Object[] {}), String[].class).length);
        assertTrue(N.equals(N.asArray("abc", "123"), abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(N.asArray("abc", "123")), String[].class)));
        assertTrue(abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(new ArrayList<>()), List.class).isEmpty());
        assertEquals(N.toList("abc", "123"), abacusXMLSAXParser.deserialize(abacusXMLSAXParser.serialize(N.toList("abc", "123")), List.class));
    }
}
