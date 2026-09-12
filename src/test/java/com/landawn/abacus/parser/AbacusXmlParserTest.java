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
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.types.WeekDay;

public class AbacusXmlParserTest extends AbstractXmlParserTest {

    @Override
    protected Parser<?, ?> getParser() {
        return abacusXmlParser;
    }

    @Test
    public void testRejectedTypeAttributeDoesNotFallBackToNodeName() {
        assertRejectedTypeAttributeDoesNotFallBackToNodeName(abacusXmlParser);
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

        String pretty = abacusXmlParser.serialize(genericBean, XmlSerConfig.create().setPrettyFormat(true));
        assertEquals(genericBean, abacusXmlParser.deserialize(pretty, GenericEntity.class));

        Map<String, Object> props2 = abacusXmlParser.deserialize(
                abacusXmlParser.serialize(Beans.beanToMap(genericBean), XmlSerConfig.create().setPrettyFormat(true)),
                XmlDeserConfig.create().setValueType("account", Account.class), Map.class);
        assertTrue(props2.containsKey("stringList") || props2.containsKey("booleanList"));

        GenericEntity slim = new GenericEntity();
        slim.setAccountList(N.toList(account));
        slim.setAccountMap(N.asMap(account.getFirstName(), account));
        assertEquals(slim, abacusXmlParser.deserialize(abacusXmlParser.serialize(slim), GenericEntity.class));
    }

    @Test
    public void testPrettyFormat() {
        Account account = createAccountWithContact(Account.class);
        account.setId(100);
        XmlSerConfig config = XmlSerConfig.create().setPrettyFormat(true).setIndentation("    ");

        String accountXml = abacusXmlParser.serialize(account, config);
        assertTrue(accountXml.contains("\n"));
        assertEquals(100, abacusXmlParser.deserialize(accountXml, Account.class).getId());
        assertEquals(2, abacusXmlParser.deserialize(abacusXmlParser.serialize(N.asArray(account, account), config), Account[].class).length);
        assertEquals(2,
                abacusXmlParser
                        .deserialize(abacusXmlParser.serialize(N.toList(account, account), config), XmlDeserConfig.create().setElementType(Account.class),
                                List.class)
                        .size());
        assertTrue(abacusXmlParser.serialize(Beans.deepBeanToMap(account), config).contains("id")
                || abacusXmlParser.serialize(Beans.deepBeanToMap(account), config).contains("firstName"));

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
        XBean roundTrip = abacusXmlParser.deserialize(abacusXmlParser.serialize(xBean, config), XBean.class);
        assertEquals(WeekDay.THURSDAY, roundTrip.getWeekDay());
        assertEquals('黎', roundTrip.getTypeChar());
        assertEquals(xBean.getTypeString(), roundTrip.getTypeString());
        assertTrue(N.equals(N.asArray("abc", "123"), abacusXmlParser.deserialize(abacusXmlParser.serialize(N.asArray("abc", "123"), config), String[].class)));
        assertEquals(N.toList("abc", "123"), abacusXmlParser.deserialize(abacusXmlParser.serialize(N.toList("abc", "123"), config), List.class));
    }

    @Test
    public void testNodeByName() throws SAXException, IOException {
        Account account = createAccount(Account.class);
        String str = abacusXmlParser.serialize(account);
        Map<String, Type<?>> nodeClasses = Map.of("account", Type.of(Account.class));

        InputStream is = IOUtil.stringToInputStream(str);
        assertEquals(account, abacusXmlParser.deserialize(is, null, nodeClasses));
        IOUtil.close(is);
        Reader reader = new StringReader(str);
        assertEquals(account, abacusXmlParser.deserialize(reader, null, nodeClasses));
        IOUtil.close(reader);
        Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        assertEquals(account, abacusXmlParser.deserialize(doc.getDocumentElement(), null, nodeClasses));
    }

    @Test
    public void testBufferedWriter() {
        Writer writer = new StringWriter();
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(writer);
        Account account = createAccount(Account.class);
        abacusXmlParser.serialize(account, bw);
        Objectory.recycle(bw);
        String written = writer.toString();
        assertTrue(written.contains(account.getFirstName()));
        assertEquals(account, abacusXmlParser.deserialize(written, Account.class));

        Writer empty = new StringWriter();
        BufferedXmlWriter emptyBw = Objectory.createBufferedXmlWriter(empty);
        abacusXmlParser.serialize((Account) null, emptyBw);
        assertEquals(Strings.EMPTY, empty.toString());
        Objectory.recycle(emptyBw);
    }

    @Test
    public void testNull() {
        assertNull(abacusXmlParser.deserialize(abacusXmlParser.serialize((String) null), String.class));
        String[] array = N.asArray((String) null);
        assertTrue(N.equals(array, abacusXmlParser.deserialize(abacusXmlParser.serialize(array), String[].class)));
        List<String> list = N.toList((String) null);
        assertEquals(list, abacusXmlParser.deserialize(abacusXmlParser.serialize(list), List.class));

        String str = abacusXmlParser.serialize(N.asMap((String) null, (Object) null), XmlSerConfig.create().setExclusion(Exclusion.NONE));
        Map<String, Object> map2 = abacusXmlParser.deserialize(str, Map.class);
        map2 = (Map<String, Object>) abacusXmlParser.deserialize(str, Object.class);
        map2 = (Map<String, Object>) abacusXmlParser.deserialize(str, abacusXmlParser.getClass());
        assertThrows(ParsingException.class, () -> abacusXmlParser.serialize(abacusXmlParser));
        assertNotNull(map2);

        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Object[] mixed = { account, null };
        assertTrue(N.equals(mixed,
                abacusXmlParser.deserialize(abacusXmlParser.serialize(mixed), XmlDeserConfig.create().setElementType(Account.class), Object[].class)));
        List<?> mixedList = N.toList(account, (String) null);
        assertTrue(N.equals(mixedList,
                abacusXmlParser.deserialize(abacusXmlParser.serialize(mixedList), XmlDeserConfig.create().setElementType(Account.class), List.class)));
        XmlDeserConfig xdc = XmlDeserConfig.create().setElementType(Account.class);
        xdc.setValueType("account", Account.class);
        assertNotNull(abacusXmlParser
                .deserialize(abacusXmlParser.serialize(N.asMap((String) null, account), XmlSerConfig.create().setExclusion(Exclusion.NONE)), xdc, Map.class));

        account.setId(0);
        account.setLastName(null);
        Map<String, Object> map = Beans.beanToMap(account);
        map.put("lastName", null);
        map.put("account", account);
        Map<Class<?>, Set<String>> ignored = N.asMap(Map.class, N.toSet("id"));
        XmlSerConfig xsc = XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignored).setPrettyFormat(true);
        xdc = XmlDeserConfig.create().setElementType(Account.class);
        xdc.setValueType("account", Account.class);
        assertNotNull(abacusXmlParser.deserialize(abacusXmlParser.serialize(map, xsc), xdc, Map.class));
        Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.toList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));
        xdc = XmlDeserConfig.create();
        xdc.setValueType("account", Account.class);
        assertNotNull(abacusXmlParser.deserialize(abacusXmlParser.serialize(map3, xsc), xdc, Map.class));
        String xBeanXml = abacusXmlParser.serialize(createXBean(), xsc);
        assertNotNull(abacusXmlParser.deserialize(xBeanXml, XBean.class));
        assertTrue(xBeanXml.contains("firstName"));
    }

    @Test
    public void testTransient() {
        TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");
        assertTrue(abacusXmlParser.serialize(bean).indexOf("abc") < 0);
        String withTransient = abacusXmlParser.serialize(bean, XmlSerConfig.create().setSkipTransientField(false));
        assertTrue(withTransient.indexOf("abc") >= 0);
        assertEquals(bean, abacusXmlParser.deserialize(withTransient, TransientBean.class));
    }

    @Test
    public void testSerializeTagName() {
        String xml = "<unknown><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui>"
                + "<emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress>"
                + "<firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName>"
                + "<birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime><createdTime>1414001208305</createdTime>"
                + "<contact><unknown><id>2801</id><accountId>1002759403</accountId><address>ca, US</address>"
                + "<city>sunnyvale</city><state>CA</state><country>U.S.</country></unknown></contact></unknown>";
        Account account2 = abacusXmlParser.deserialize(xml, Account.class);
        assertEquals("firstName", account2.getFirstName());
        assertEquals("lastName", account2.getLastName());
        assertEquals("MN", account2.getMiddleName());
        assertEquals(1002759403L, account2.getId());
    }

    @Test
    public void testSerializeXBean() throws Exception {
        XBean xBean = createXBean();
        assertEquals(xBean, abacusXmlParser.deserialize(abacusXmlParser.serialize(xBean, XmlSerConfig.create().setExclusion(Exclusion.NONE)), XBean.class));
        assertEquals(xBean,
                abacusXmlParser.deserialize(
                        abacusXmlParser.serialize(xBean, XmlSerConfig.create().setExclusion(Exclusion.NONE).setPropNamingPolicy(NamingPolicy.SNAKE_CASE)),
                        XBean.class));
        assertEquals(xBean, abacusXmlParser.deserialize(abacusXmlParser.serialize(xBean, XmlSerConfig.create().setTagByPropertyName(false)), XBean.class));

        Map<Class<?>, Set<String>> ignored = N.asMap(XBean.class, N.toSet("typeBoolean", "typeShort", "typeLong"));
        XBean ignoredCopy = createXBean();
        XBean restored = abacusXmlParser
                .deserialize(abacusXmlParser.serialize(ignoredCopy, XmlSerConfig.create().setIgnoredPropNames(ignored).setWriteTypeInfo(true)), XBean.class);
        ignoredCopy.setTypeBoolean(false);
        ignoredCopy.setTypeShort((short) 0);
        ignoredCopy.setTypeLong(0);
        assertEquals(ignoredCopy, restored);

        XBean none = createXBean();
        XBean noneRestored = abacusXmlParser
                .deserialize(abacusXmlParser.serialize(none, XmlSerConfig.create().setIgnoredPropNames(ignored).setExclusion(Exclusion.NONE)), XBean.class);
        none.setTypeBoolean(false);
        none.setTypeShort((short) 0);
        none.setTypeLong(0);
        assertEquals(none, noneRestored);

        XBean source = createXBean();
        String str = abacusXmlParser.serialize(source, XmlSerConfig.create().setIgnoredPropNames(ignored).setExclusion(Exclusion.NONE));
        Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        XBean fromNode = abacusXmlParser.deserialize(doc.getDocumentElement(), XBean.class);
        assertEquals(source.getFirstName(), fromNode.getFirstName());
        assertFalse(fromNode.getTypeBoolean());
        assertEquals((short) 0, fromNode.getTypeShort());
        assertEquals(0L, fromNode.getTypeLong());

        Map<String, Object> map = new HashMap<>();
        map.put("array", N.asArray("abc", "123"));
        map.put("list", N.toList("abc", "123"));
        Map<String, Object> restoredMap = abacusXmlParser.deserialize(abacusXmlParser.serialize(map),
                XmlDeserConfig.create().setMapKeyType(String.class).setMapValueType(String[].class), Map.class);
        assertTrue(restoredMap.containsKey("list") || restoredMap.containsKey("array"));
    }

    @Test
    public void testSerializeNullFirstName() {
        Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);

        XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        Account restored = abacusXmlParser.deserialize(abacusXmlParser.serialize(account, xsc), XmlDeserConfig.create(), Account.class);
        assertNull(restored.getFirstName());
        assertNotNull(restored.getContact());

        xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(true).setExclusion(Exclusion.NONE);
        restored = abacusXmlParser.deserialize(abacusXmlParser.serialize(account, xsc), XmlDeserConfig.create(), Account.class);
        assertNull(restored.getFirstName());
        assertNotNull(restored.getContact());

        xsc = XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        XmlDeserConfig xdc = XmlDeserConfig.create();
        xdc.setValueType("accountContact", account.getContact().getClass());
        Map<String, Object> asMap = abacusXmlParser.deserialize(abacusXmlParser.serialize(Beans.deepBeanToMap(account), xsc), xdc, Map.class);
        List<Account> accountList = abacusXmlParser.deserialize(abacusXmlParser.serialize(N.toList(account), xsc),
                XmlDeserConfig.create().setElementType(Account.class), List.class);
        Object[] accountArray = abacusXmlParser.deserialize(abacusXmlParser.serialize(N.asArray(account), xsc),
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
        Map<Object, Object> restored = abacusXmlParser.deserialize(abacusXmlParser.serialize(map), Map.class);
        restored = abacusXmlParser.deserialize(abacusXmlParser.serialize(map, XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false)),
                Map.class);
        map = new HashMap<>();
        map.put(N.asArray("abc"), Array.of(123));
        String str = abacusXmlParser.serialize(map, XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false));
        restored = abacusXmlParser.deserialize(str, Map.class);
        assertFalse(restored.isEmpty());
        assertTrue(str.contains("abc"));
    }

    @Test
    public void testIgnorePropNames() {
        Account account = createAccountWithContact(Account.class);
        Map<Class<?>, Set<String>> ignored = N.asMap(Account.class, N.toSet("firstName", "contact"));
        Account ignoredAccount = abacusXmlParser.deserialize(abacusXmlParser.serialize(account), XmlDeserConfig.create().setIgnoredPropNames(ignored),
                Account.class);
        assertNull(ignoredAccount.getFirstName());
        assertNull(ignoredAccount.getContact());

        Map<Class<?>, Set<String>> mapIgnored = N.asMap(Map.class, N.toSet("firstName", "contact"));
        Map<String, Object> ignoredMap = abacusXmlParser.deserialize(abacusXmlParser.serialize(Beans.deepBeanToMap(account)),
                XmlDeserConfig.create().setIgnoredPropNames(mapIgnored), Map.class);
        assertNull(ignoredMap.get("firstName"));
        assertNull(ignoredMap.get("contact"));

        String xml = "<account><gui_1>9b1b4964298a4868a4ab95ccf6a5f987</gui_1><emailAddress>48c6a440fa114de28fad1bf04fa66090@earth.com</emailAddress>"
                + "<firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName>"
                + "<birthDate>1413839551838</birthDate><lastUpdateTime>1413839551838</lastUpdateTime><createdTime>1413839551838</createdTime>"
                + "<contact_1><accountContact><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact_1></account>";
        assertNull(abacusXmlParser.deserialize(xml, XmlDeserConfig.create().setIgnoredPropNames(ignored), Account.class).getFirstName());
        assertThrows(ParsingException.class, () -> abacusXmlParser.deserialize(xml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false), Account.class));
        Account allowed = abacusXmlParser.deserialize(xml,
                XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames(N.asMap(Account.class, N.toSet("gui_1", "contact_1"))),
                Account.class);
        assertNotNull(allowed.getFirstName());
        assertNull(allowed.getContact());
    }

    @Test
    public void testSerializeCollections() {
        Account[] accounts = N.asArray(createAccount(Account.class), createAccount(Account.class));
        assertTrue(N.equals(accounts, abacusXmlParser.deserialize(abacusXmlParser.serialize(accounts), Account[].class)));
        assertEquals(0, abacusXmlParser.deserialize(abacusXmlParser.serialize(new Object[] {}), String[].class).length);
        assertTrue(N.equals(N.asArray("abc", "123"), abacusXmlParser.deserialize(abacusXmlParser.serialize(N.asArray("abc", "123")), String[].class)));
        assertTrue(abacusXmlParser.deserialize(abacusXmlParser.serialize(new ArrayList<>()), List.class).isEmpty());
        assertEquals(N.toList("abc", "123"), abacusXmlParser.deserialize(abacusXmlParser.serialize(N.toList("abc", "123")), List.class));
    }
}
