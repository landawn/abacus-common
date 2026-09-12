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
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

import testfixtures.entity.extendDirty.basic.Account;

public class XmlDOMParserTest extends AbstractXmlParserTest {

    private static final String[] TAGS = { "map", "list", "key", "value", "entry", "e", "unknown" };

    @Override
    protected Parser<?, ?> getParser() {
        return xmlDOMParser;
    }

    private static String wrap(String tag, String innerTag) {
        return "<" + tag + "><id>1002759403</id><gui>8354b425f53d4c1893b848a35191bd89</gui>"
                + "<emailAddress>a267c0eb96d84088968ec4885110ddab@earth.com</emailAddress>"
                + "<firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName>"
                + "<birthDate>1414001208304</birthDate><lastUpdateTime>1414001208304</lastUpdateTime>" + "<createdTime>1414001208305</createdTime><contact><"
                + innerTag + "><id>2801</id><accountId>1002759403</accountId><address>ca, US</address>"
                + "<city>sunnyvale</city><state>CA</state><country>U.S.</country></" + innerTag + "></contact></" + tag + ">";
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

        String pretty = xmlDOMParser.serialize(genericBean, XmlSerConfig.create().setPrettyFormat(true));
        assertEquals(genericBean, xmlDOMParser.deserialize(pretty, GenericEntity.class));
        assertTrue(pretty.contains("<"));

        String propsXml = xmlDOMParser.serialize(Beans.beanToMap(genericBean), XmlSerConfig.create().setPrettyFormat(true));
        assertEquals(genericBean, xmlDOMParser.deserialize(propsXml, GenericEntity.class));
        Map<String, Object> props2 = xmlDOMParser.deserialize(propsXml, Map.class);
        assertTrue(propsXml.contains("\n"));
        assertTrue(props2.containsKey("stringList") || props2.containsKey("booleanList"));

        GenericEntity slim = new GenericEntity();
        slim.setAccountList(N.toList(account));
        slim.setAccountMap(N.asMap(account.getFirstName(), account));
        assertEquals(slim, xmlDOMParser.deserialize(xmlDOMParser.serialize(slim), GenericEntity.class));
    }

    @Test
    public void testConfigEquals() {
        assertEquals(XmlSerConfig.create(), XmlSerConfig.create());
        XmlDeserConfig xdc1 = XmlDeserConfig.create()
                .setMapKeyType(String.class)
                .setMapValueType(String.class)
                .setIgnoreUnmatchedProperty(true)
                .setIgnoredPropNames((Map<Class<?>, Set<String>>) null);
        XmlDeserConfig xdc2 = XmlDeserConfig.create()
                .setMapKeyType(String.class)
                .setMapValueType(String.class)
                .setIgnoreUnmatchedProperty(true)
                .setIgnoredPropNames((Map<Class<?>, Set<String>>) null);
        assertEquals(xdc1, xdc2);
        assertTrue(N.toSet(xdc1).contains(xdc2));
    }

    @Test
    public void testTransient() {
        TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");
        assertTrue(xmlDOMParser.serialize(bean).indexOf("abc") < 0);
        String withTransient = xmlDOMParser.serialize(bean, XmlSerConfig.create().setSkipTransientField(false));
        assertTrue(withTransient.indexOf("abc") >= 0);
        assertEquals(bean, xmlDOMParser.deserialize(withTransient, TransientBean.class));
    }

    @Test
    public void testNull() {
        assertNull(xmlDOMParser.deserialize(xmlDOMParser.serialize((String) null), String.class));

        String[] array = N.asArray((String) null);
        assertTrue(N.equals(array, xmlDOMParser.deserialize(xmlDOMParser.serialize(array), String[].class)));
        List<String> list = N.toList((String) null);
        assertEquals(list, xmlDOMParser.deserialize(xmlDOMParser.serialize(list), List.class));

        Map<String, Object> nullMap = N.asMap((String) null, (Object) null);
        String str = xmlDOMParser.serialize(nullMap, XmlSerConfig.create().setExclusion(Exclusion.NONE));
        assertNotNull(xmlDOMParser.deserialize(str, Map.class));
        xmlDOMParser.deserialize(str, Object.class);
        assertThrows(ParsingException.class, () -> xmlDOMParser.deserialize(str, xmlDOMParser.getClass()));
        assertThrows(ParsingException.class, () -> xmlDOMParser.serialize(xmlDOMParser));

        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Object[] mixed = { account, null };
        assertTrue(N.equals(mixed,
                xmlDOMParser.deserialize(xmlDOMParser.serialize(mixed), XmlDeserConfig.create().setElementType(Account.class), Object[].class)));
        List<?> mixedList = N.toList(account, (String) null);
        assertTrue(N.equals(mixedList,
                xmlDOMParser.deserialize(xmlDOMParser.serialize(mixedList), XmlDeserConfig.create().setElementType(Account.class), List.class)));
        assertNotNull(xmlDOMParser.deserialize(xmlDOMParser.serialize(N.asMap((String) null, account), XmlSerConfig.create().setExclusion(Exclusion.NONE)),
                Map.class));

        account.setId(0);
        account.setLastName(null);
        Map<String, Object> map = Beans.beanToMap(account);
        map.put("lastName", null);
        map.put("account", account);
        Map<Class<?>, Set<String>> ignored = N.asMap(Map.class, N.toSet("id"));
        XmlSerConfig xsc = XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignored).setPrettyFormat(true);
        assertNotNull(xmlDOMParser.deserialize(xmlDOMParser.serialize(map, xsc), XmlDeserConfig.create().setElementType(Account.class), Map.class));
        assertNotNull(xmlDOMParser.deserialize(xmlDOMParser.serialize(N.toList(map), xsc), XmlDeserConfig.create().setElementType(Map.class), List.class));
        Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.toList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));
        assertNotNull(xmlDOMParser.deserialize(xmlDOMParser.serialize(map3, xsc), XmlDeserConfig.create().setElementType(Account.class), Map.class));
        String xBeanXml = xmlDOMParser.serialize(createXBean(), xsc);
        assertNotNull(xmlDOMParser.deserialize(xBeanXml, XBean.class));
        assertTrue(xBeanXml.contains("firstName"));
    }

    @Test
    public void testNodeByName() throws SAXException, IOException {
        Account account = createAccount(Account.class);
        String str = xmlDOMParser.serialize(account);
        Map<String, Type<?>> nodeClasses = N.asMap("account", Type.of(Account.class));

        InputStream is = IOUtil.stringToInputStream(str);
        assertEquals(account, xmlDOMParser.deserialize(is, null, nodeClasses));
        IOUtil.close(is);
        Reader reader = new StringReader(str);
        assertEquals(account, xmlDOMParser.deserialize(reader, null, nodeClasses));
        IOUtil.close(reader);
        Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        assertEquals(account, xmlDOMParser.deserialize(doc.getDocumentElement(), null, nodeClasses));
    }

    @Test
    public void testTypeInfo() {
        Account account = createAccountWithContact(Account.class);
        String xml = xmlDOMParser.serialize(account, XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(true));
        Account account2 = xmlDOMParser.deserialize(xml, Account.class);
        Object asMap = xmlDOMParser.deserialize(xml, Map.class);
        assertEquals(account.getFirstName(), account2.getFirstName());
        assertEquals(account.getLastName(), account2.getLastName());
        assertTrue(asMap instanceof Map);
    }

    @Test
    public void testSerializeTagName() {
        Account last = null;
        Map<String, String> lastMap = null;
        for (String tag : TAGS) {
            String inner = tag.equals("map") ? "accountContact" : tag;
            last = xmlDOMParser.deserialize(wrap(tag, inner), Account.class);
            lastMap = xmlDOMParser.deserialize(wrap(tag, inner), Map.class);
        }
        last = xmlDOMParser.deserialize(wrap("map", "map"), Account.class);
        lastMap = xmlDOMParser.deserialize(wrap("map", "map"), Map.class);
        assertEquals("firstName", last.getFirstName());
        assertEquals(1002759403L, last.getId());
        assertEquals("firstName", String.valueOf(lastMap.get("firstName")));
        assertTrue(String.valueOf(lastMap.get("id")).contains("1002759403"));
    }

    @Test
    public void testSerializeXBean() throws Exception {
        XBean xBean = createXBean();
        String xml = xmlDOMParser.serialize(xBean, XmlSerConfig.create().setExclusion(Exclusion.NONE));
        assertTrue(xml.contains(xBean.getFirstName()));
        assertEquals(xBean.getFirstName(), xmlDOMParser.deserialize(xml, XBean.class).getFirstName());
        String untagged = xmlDOMParser.serialize(xBean, XmlSerConfig.create().setTagByPropertyName(false));
        assertTrue(untagged.contains(xBean.getFirstName()));

        Map<Class<?>, Set<String>> ignored = N.asMap(XBean.class, N.toSet("typeBoolean", "typeShort", "typeLong"));
        XBean ignoredCopy = createXBean();
        XBean restored = xmlDOMParser
                .deserialize(xmlDOMParser.serialize(ignoredCopy, XmlSerConfig.create().setIgnoredPropNames(ignored).setWriteTypeInfo(true)), XBean.class);
        assertEquals(ignoredCopy.getFirstName(), restored.getFirstName());
        assertFalse(restored.getTypeBoolean());
        assertEquals((short) 0, restored.getTypeShort());
        assertEquals(0L, restored.getTypeLong());

        String str = xmlDOMParser.serialize(createXBean(), XmlSerConfig.create().setIgnoredPropNames(ignored).setExclusion(Exclusion.NONE));
        Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        assertNotNull(xmlDOMParser.deserialize(doc.getDocumentElement(), XBean.class));
    }

    @Test
    public void testSerializeMapCollections() {
        Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("array", N.asArray("abc", "123"));
        map.put("list", N.toList("abc", "123"));
        Map<String, Object> restored = xmlDOMParser.deserialize(xmlDOMParser.serialize(map),
                XmlDeserConfig.create().setMapKeyType(String.class).setMapValueType(String[].class), Map.class);
        assertTrue(restored.containsKey("list") || restored.containsKey("array") || restored.containsKey("intArray"));

        Map<String, Object> accountMap = new HashMap<>();
        accountMap.put("array", new Object[] { createAccount(Account.class), createAccount(Account.class) });
        accountMap.put("list", N.toList(createAccount(Account.class), createAccount(Account.class)));
        Map<String, Object> withElementType = xmlDOMParser.deserialize(xmlDOMParser.serialize(accountMap),
                XmlDeserConfig.create().setElementType(Account.class), Map.class);
        withElementType = xmlDOMParser.deserialize(xmlDOMParser.serialize(withElementType), XmlDeserConfig.create().setElementType(Account.class), Map.class);
        assertTrue(withElementType.containsKey("list") || withElementType.containsKey("array"));
    }

    @Test
    public void testSerializeNullFirstName() {
        Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);

        XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        Account restored = xmlDOMParser.deserialize(xmlDOMParser.serialize(account, xsc), XmlDeserConfig.create(), Account.class);
        assertNull(restored.getFirstName());
        assertNotNull(restored.getContact());

        xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(true).setExclusion(Exclusion.NONE);
        restored = xmlDOMParser.deserialize(xmlDOMParser.serialize(account, xsc), XmlDeserConfig.create(), Account.class);
        assertNull(restored.getFirstName());
        assertNotNull(restored.getContact());

        xsc = XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        restored = xmlDOMParser.deserialize(xmlDOMParser.serialize(Beans.deepBeanToMap(account), xsc), XmlDeserConfig.create(), Account.class);
        List<Account> accountList = xmlDOMParser.deserialize(xmlDOMParser.serialize(N.toList(account), xsc),
                XmlDeserConfig.create().setElementType(Account.class), List.class);
        Object[] accountArray = xmlDOMParser.deserialize(xmlDOMParser.serialize(N.asArray(account), xsc), XmlDeserConfig.create().setElementType(Account.class),
                Object[].class);
        assertNotNull(restored);
        assertEquals(1, accountList.size());
        assertEquals(1, accountArray.length);
        assertNull(accountList.get(0).getFirstName());
    }

    @Test
    public void testIgnorePropNames() {
        Account account = createAccountWithContact(Account.class);
        Map<Class<?>, Set<String>> ignored = N.asMap(Account.class, N.toSet("firstName", "contact"));
        Account ignoredAccount = xmlDOMParser.deserialize(xmlDOMParser.serialize(account), XmlDeserConfig.create().setIgnoredPropNames(ignored), Account.class);
        assertNull(ignoredAccount.getFirstName());
        assertNull(ignoredAccount.getContact());

        Map<Class<?>, Set<String>> mapIgnored = N.asMap(Map.class, N.toSet("firstName", "contact"));
        Map<String, Object> ignoredMap = xmlDOMParser.deserialize(xmlDOMParser.serialize(Beans.deepBeanToMap(account)),
                XmlDeserConfig.create().setIgnoredPropNames(mapIgnored), Map.class);
        assertNull(ignoredMap.get("firstName"));
        assertNull(ignoredMap.get("contact"));

        String xml = "<account><gui_1>9b1b4964298a4868a4ab95ccf6a5f987</gui_1><emailAddress>48c6a440fa114de28fad1bf04fa66090@earth.com</emailAddress>"
                + "<firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName>"
                + "<birthDate>1413839551838</birthDate><lastUpdateTime>1413839551838</lastUpdateTime><createdTime>1413839551838</createdTime>"
                + "<contact_1><accountContact><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact_1></account>";
        assertNull(xmlDOMParser.deserialize(xml, XmlDeserConfig.create().setIgnoredPropNames(ignored), Account.class).getFirstName());
        assertThrows(ParsingException.class, () -> xmlDOMParser.deserialize(xml,
                XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames((Map<Class<?>, Set<String>>) null), Account.class));
        Account allowed = xmlDOMParser.deserialize(xml,
                XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames(N.asMap(Account.class, N.toSet("gui_1", "contact_1"))),
                Account.class);
        assertNotNull(allowed.getFirstName());
        assertNull(allowed.getContact());
    }

    @Test
    public void testSerializeCollections() {
        assertEquals(N.toList("abc", "123"), xmlDOMParser.deserialize(xmlDOMParser.serialize(N.toList("abc", "123")), List.class));
        Account[] accounts = N.asArray(createAccount(Account.class), createAccount(Account.class));
        assertTrue(N.equals(accounts, xmlDOMParser.deserialize(xmlDOMParser.serialize(accounts), Account[].class)));
        assertEquals(0, xmlDOMParser.deserialize(xmlDOMParser.serialize(new Object[] {}), String[].class).length);
        assertTrue(N.equals(N.asArray("abc", "123"), xmlDOMParser.deserialize(xmlDOMParser.serialize(N.asArray("abc", "123")), String[].class)));
        assertTrue(xmlDOMParser.deserialize(xmlDOMParser.serialize(new ArrayList<>()), List.class).isEmpty());
        List<Account> accountList = N.toList(createAccount(Account.class), createAccount(Account.class));
        assertTrue(N.equals(accountList,
                xmlDOMParser.deserialize(xmlDOMParser.serialize(accountList), XmlDeserConfig.create().setElementType(Account.class), List.class)));

        MapEntity entity = new MapEntity("MapEntity", Beans.beanToMap(createAccount(Account.class)));
        MapEntity restored = xmlDOMParser.deserialize(xmlDOMParser.serialize(entity), MapEntity.class);
        assertEquals(entity.entityName(), restored.entityName());
        assertEquals(String.valueOf((Object) entity.get("firstName")), String.valueOf((Object) restored.get("firstName")));
    }

    /**
     * P5-03: a bean written with {@code tagByPropertyName=false} inherits that flag into its Map property, and
     * the DOM reader then demanded a {@code name} attribute on every map entry -- but {@code writeMap} names the
     * entries after their keys, whatever the flag says ("Missing 'name' attribute on XML element: k").
     */
    @Test
    public void reviewFixes20260906_tagByPropertyNameFalseWithMapProperty() {
        // Fixture from XmlParserImplTest (same package): a bean with map, nested-bean-list and String-list props.
        final XmlParserImplTest.OuterBean original = new XmlParserImplTest.OuterBean();
        original.setName("n");
        original.setMap(N.asMap("k", 1));
        original.setStrs(N.toList("a", "b"));
        original.setTail("t");

        for (boolean typeInfo : new boolean[] { false, true }) {
            for (boolean pretty : new boolean[] { false, true }) {
                final XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(typeInfo).setPrettyFormat(pretty);
                final String xml = xmlDOMParser.serialize(original, xsc);
                assertTrue(xml.contains("<property name=\"map\""), xml);
                assertFalse(xml.contains("<k name="), xml);

                final XmlParserImplTest.OuterBean restored = xmlDOMParser.deserialize(xml, XmlParserImplTest.OuterBean.class);
                assertEquals("n", restored.getName(), xml);
                assertEquals("t", restored.getTail(), xml);
                assertEquals(1, restored.getMap().get("k"), xml);
                assertEquals(N.toList("a", "b"), restored.getStrs(), xml);
            }
        }

        // A root map written with the same flag is unchanged, and a <property> element inside a BEAN still needs
        // its name attribute (pinned by XmlParserImplTest.testReadByDOMParser).
        final Map<String, String> rootMap = N.asMap("k", "v");
        assertEquals(rootMap, xmlDOMParser.deserialize(xmlDOMParser.serialize(rootMap, XmlSerConfig.create().setTagByPropertyName(false)), Map.class));
    }

    /**
     * P5-04: with {@code writeTypeInfo=true} the DOM backend read a {@code List<Bean>} property back as a list
     * of HashMaps -- turning the type-information feature ON lost type information.
     */
    @Test
    public void reviewFixes20260906_writeTypeInfoKeepsListElementType() {
        final Account account = createAccount(Account.class);
        final GenericEntity original = new GenericEntity();
        original.setAccountList(N.toList(account));
        original.setStringList(N.toList("abc", "123"));

        for (boolean pretty : new boolean[] { false, true }) {
            final String xml = xmlDOMParser.serialize(original, XmlSerConfig.create().setWriteTypeInfo(true).setPrettyFormat(pretty));
            assertTrue(xml.contains("accountList type="), xml);

            final GenericEntity restored = xmlDOMParser.deserialize(xml, GenericEntity.class);
            assertEquals(1, restored.getAccountList().size(), xml);
            assertEquals(Account.class, restored.getAccountList().get(0).getClass(), xml);
            assertEquals(account.getFirstName(), restored.getAccountList().get(0).getFirstName(), xml);
            assertEquals(N.toList("abc", "123"), restored.getStringList(), xml);
        }
    }

    /**
     * P5-13: both writers emit {@code type="MapEntity"} with {@code writeTypeInfo=true}; the DOM backend used to
     * reject that as an unsafe type attribute and could not read the parser's own output.
     */
    @Test
    public void reviewFixes20260906_mapEntityTypeAttributeIsAccepted() {
        final MapEntity entity = new MapEntity("MapEntity", Beans.beanToMap(createAccount(Account.class)));
        final String xml = xmlDOMParser.serialize(entity, XmlSerConfig.create().setWriteTypeInfo(true));
        assertTrue(xml.contains("type=\"MapEntity\""), xml);

        final MapEntity restored = xmlDOMParser.deserialize(xml, MapEntity.class);
        assertEquals(entity.entityName(), restored.entityName());
        assertEquals(String.valueOf((Object) entity.get("firstName")), String.valueOf((Object) restored.get("firstName")));

        // A type attribute that is not on the allowlist is still rejected.
        assertThrows(ParsingException.class,
                () -> xmlDOMParser.deserialize("<Account type=\"com.example.NotRegistered\"><firstName>x</firstName></Account>", MapEntity.class));
    }

    @Test
    public void testBufferedWriter() {
        Writer writer = new StringWriter();
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(writer);
        Account account = createAccount(Account.class);
        xmlDOMParser.serialize(account, bw);
        Objectory.recycle(bw);
        String written = writer.toString();
        assertTrue(written.contains(account.getFirstName()));
        assertEquals(account, xmlDOMParser.deserialize(written, Account.class));

        Writer empty = new StringWriter();
        BufferedXmlWriter emptyBw = Objectory.createBufferedXmlWriter(empty);
        xmlDOMParser.serialize((Account) null, emptyBw);
        assertEquals(Strings.EMPTY, empty.toString());
        Objectory.recycle(emptyBw);
    }
}
