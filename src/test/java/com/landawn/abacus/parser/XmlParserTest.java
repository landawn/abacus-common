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

import javax.xml.parsers.DocumentBuilder;

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
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.XmlUtil;

import testfixtures.entity.extendDirty.basic.Account;

public class XmlParserTest extends AbstractXmlParserTest {

    private static final String[] TAGS = { "map", "list", "key", "value", "entry", "e", "unknown" };

    @Override
    protected Parser<?, ?> getParser() {
        return xmlParser;
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

        String pretty = xmlParser.serialize(genericBean, XmlSerConfig.create().setPrettyFormat(true));
        assertEquals(genericBean, xmlParser.deserialize(pretty, GenericEntity.class));
        assertTrue(pretty.contains("<"));

        GenericEntity slim = new GenericEntity();
        slim.setAccountList(N.toList(account));
        slim.setAccountMap(N.asMap(account.getFirstName(), account));
        assertEquals(slim, xmlParser.deserialize(xmlParser.serialize(slim), GenericEntity.class));
    }

    @Test
    public void testTransient() {
        TransientBean bean = new TransientBean();
        bean.setTransientField("abc");
        bean.setNontransientField("123");
        assertTrue(xmlParser.serialize(bean).indexOf("abc") < 0);
        String withTransient = xmlParser.serialize(bean, XmlSerConfig.create().setSkipTransientField(false));
        assertTrue(withTransient.indexOf("abc") >= 0);
        assertEquals(bean, xmlParser.deserialize(withTransient, TransientBean.class));
    }

    @Test
    public void testNull() {
        assertNull(xmlParser.deserialize(xmlParser.serialize((String) null), String.class));

        String[] array = N.asArray((String) null);
        assertTrue(N.equals(array, xmlParser.deserialize(xmlParser.serialize(array), String[].class)));
        List<String> list = N.toList((String) null);
        assertEquals(list, xmlParser.deserialize(xmlParser.serialize(list), List.class));

        Map<String, Object> nullMap = N.asMap((String) null, (Object) null);
        String str = xmlParser.serialize(nullMap, XmlSerConfig.create().setExclusion(Exclusion.NONE));
        assertNotNull(xmlParser.deserialize(str, Map.class));
        xmlParser.deserialize(str, Object.class);
        assertThrows(ParsingException.class, () -> xmlParser.deserialize(str, xmlParser.getClass()));
        assertThrows(ParsingException.class, () -> xmlParser.serialize(xmlParser));

        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Object[] mixed = { account, null };
        assertTrue(N.equals(mixed, xmlParser.deserialize(xmlParser.serialize(mixed), XmlDeserConfig.create().setElementType(Account.class), Object[].class)));
        List<?> mixedList = N.toList(account, (String) null);
        assertTrue(
                N.equals(mixedList, xmlParser.deserialize(xmlParser.serialize(mixedList), XmlDeserConfig.create().setElementType(Account.class), List.class)));
        assertNotNull(
                xmlParser.deserialize(xmlParser.serialize(N.asMap((String) null, account), XmlSerConfig.create().setExclusion(Exclusion.NONE)), Map.class));

        account.setId(0);
        account.setLastName(null);
        Map<String, Object> map = Beans.beanToMap(account);
        map.put("lastName", null);
        map.put("account", account);
        Map<Class<?>, Set<String>> ignored = N.asMap(Map.class, N.toSet("id"));
        XmlSerConfig xsc = XmlSerConfig.create().setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignored).setPrettyFormat(true);
        Map<String, Object> map2 = xmlParser.deserialize(xmlParser.serialize(map, xsc), XmlDeserConfig.create().setElementType(Account.class), Map.class);
        List<?> nested = xmlParser.deserialize(xmlParser.serialize(N.toList(map), xsc), XmlDeserConfig.create().setElementType(Map.class), List.class);
        Map<String, Object> map3 = new HashMap<>();
        map3.put("accountList", N.toList(account, null, account));
        map3.put("accountArray", N.asArray(account, null, account));
        assertNotNull(xmlParser.deserialize(xmlParser.serialize(map3, xsc), XmlDeserConfig.create().setElementType(Account.class), Map.class));
        XBean xBean = createXBean();
        String xBeanXml = xmlParser.serialize(xBean, xsc);
        assertNotNull(xmlParser.deserialize(xBeanXml, XBean.class));
        assertTrue(xBeanXml.contains("firstName"));
        assertNotNull(map2);
        assertNotNull(nested);
    }

    @Test
    public void testNodeByName() throws SAXException, IOException {
        Account account = createAccount(Account.class);
        String str = xmlParser.serialize(account);
        Map<String, Type<?>> nodeClasses = N.asMap("account", Type.of(Account.class));

        InputStream is = IOUtil.stringToInputStream(str);
        assertEquals(account, xmlParser.deserialize(is, null, nodeClasses));
        IOUtil.close(is);

        Reader reader = new StringReader(str);
        assertEquals(account, xmlParser.deserialize(reader, null, nodeClasses));
        IOUtil.close(reader);

        DocumentBuilder docBuilder = XmlUtil.createDOMParser();
        Document doc = docBuilder.parse(IOUtil.stringToInputStream(str));
        assertEquals(account, xmlParser.deserialize(doc.getDocumentElement(), null, nodeClasses));
    }

    @Test
    public void testStaxTypeInfo() {
        Account account = createAccountWithContact(Account.class);
        String xml = xmlParser.serialize(account, XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(true));
        Account account2 = xmlParser.deserialize(xml, Account.class);
        Object asMap = xmlParser.deserialize(xml, Map.class);
        assertEquals(account.getFirstName(), account2.getFirstName());
        assertEquals(account.getLastName(), account2.getLastName());
        assertTrue(asMap instanceof Map);
    }

    @Test
    public void testCrossParserRoundTrip() {
        Account account = createAccountWithContact(Account.class);
        String xml = xmlParser.serialize(account);
        assertEquals(account.getFirstName(), xmlDOMParser.deserialize(xml, Account.class).getFirstName());
        assertEquals(account.getFirstName(), xmlParser.deserialize(xml, Account.class).getFirstName());
        assertEquals(account.getFirstName(), abacusXmlParser.deserialize(xml, Account.class).getFirstName());
        assertEquals(account.getFirstName(), abacusXMLDOMParser.deserialize(xml, Account.class).getFirstName());
    }

    @Test
    public void testSerializeTagName() {
        Account last = null;
        Map<String, String> lastMap = null;
        for (String tag : TAGS) {
            last = xmlParser.deserialize(wrap(tag, tag.equals("map") ? "accountContact" : tag), Account.class);
            lastMap = xmlParser.deserialize(wrap(tag, tag.equals("map") ? "accountContact" : tag), Map.class);
        }
        last = xmlParser.deserialize(wrap("map", "map"), Account.class);
        lastMap = xmlParser.deserialize(wrap("map", "map"), Map.class);
        assertEquals("firstName", last.getFirstName());
        assertEquals("lastName", last.getLastName());
        assertEquals("MN", last.getMiddleName());
        assertEquals(1002759403L, last.getId());
        assertEquals("firstName", String.valueOf(lastMap.get("firstName")));
        assertTrue(String.valueOf(lastMap.get("id")).contains("1002759403"));
    }

    @Test
    public void testSerializeXBean() throws Exception {
        XBean xBean = createXBean();
        String xml = xmlParser.serialize(xBean, XmlSerConfig.create().setExclusion(Exclusion.NONE));
        assertTrue(xml.contains(xBean.getFirstName()));
        assertEquals(xBean.getFirstName(), xmlParser.deserialize(xml, XBean.class).getFirstName());
        String snake = xmlParser.serialize(xBean, XmlSerConfig.create().setExclusion(Exclusion.NONE).setPropNamingPolicy(NamingPolicy.SNAKE_CASE));
        assertTrue(snake.contains("first_name") || snake.contains(xBean.getFirstName()));
        String untagged = xmlParser.serialize(xBean, XmlSerConfig.create().setTagByPropertyName(false));
        assertTrue(untagged.contains(xBean.getFirstName()));

        Map<Class<?>, Set<String>> ignored = N.asMap(XBean.class, N.toSet("typeBoolean", "typeShort", "typeLong"));
        XBean ignoredCopy = createXBean();
        XBean restored = xmlParser.deserialize(xmlParser.serialize(ignoredCopy, XmlSerConfig.create().setIgnoredPropNames(ignored).setWriteTypeInfo(true)),
                XBean.class);
        assertEquals(ignoredCopy.getFirstName(), restored.getFirstName());
        assertFalse(restored.getTypeBoolean());
        assertEquals((short) 0, restored.getTypeShort());
        assertEquals(0L, restored.getTypeLong());

        String str = xmlParser.serialize(createXBean(), XmlSerConfig.create().setIgnoredPropNames(ignored).setExclusion(Exclusion.NONE));
        Document doc = XmlUtil.createDOMParser().parse(IOUtil.stringToInputStream(str));
        XBean fromNode = xmlParser.deserialize(doc.getDocumentElement(), XBean.class);
        assertNotNull(fromNode);
    }

    @Test
    public void testSerializeMapCollections() {
        Map<String, Object> map = new HashMap<>();
        map.put("intArray", Array.of(1, 2, 3));
        map.put("charArray", Array.of('a', 'b', 'c'));
        map.put("array", N.asArray("abc", "123"));
        map.put("intList", N.toList(1, 2, 3));
        map.put("charList", N.toList('a', 'b', 'c'));
        map.put("list", N.toList("abc", "123"));
        Map<String, Object> restored = xmlParser.deserialize(xmlParser.serialize(map),
                XmlDeserConfig.create().setMapKeyType(String.class).setMapValueType(String[].class), Map.class);
        assertTrue(restored.containsKey("list") || restored.containsKey("array") || restored.containsKey("intList"));

        Map<String, Object> objectArrayMap = new HashMap<>(map);
        objectArrayMap.put("array", new Object[] { "abc", "123" });
        restored = xmlParser.deserialize(xmlParser.serialize(objectArrayMap),
                XmlDeserConfig.create().setMapKeyType(String.class).setMapValueType(String[].class), Map.class);
        assertTrue(restored.containsKey("list") || restored.containsKey("array") || restored.containsKey("intList"));

        Map<String, Object> accountMap = new HashMap<>();
        accountMap.put("intArray", Array.of(1, 2, 3));
        accountMap.put("array", new Object[] { createAccount(Account.class), createAccount(Account.class) });
        accountMap.put("list", N.toList(createAccount(Account.class), createAccount(Account.class)));
        String xml = xmlParser.serialize(accountMap);
        Map<String, Object> withElementType = xmlParser.deserialize(xml, XmlDeserConfig.create().setElementType(Account.class), Map.class);
        withElementType = xmlParser.deserialize(xmlParser.serialize(withElementType), XmlDeserConfig.create().setElementType(Account.class), Map.class);
        assertTrue(withElementType.containsKey("list") || withElementType.containsKey("array") || withElementType.containsKey("intArray"));
    }

    @Test
    public void testSerializeNullFirstName() {
        Account account = createAccountWithContact(Account.class);
        account.setFirstName(null);

        // review fix 2026-09-06 (P5-02): the StAX reader resolves <property name="..."> elements, so a
        // tagByPropertyName=false document no longer deserializes to an all-default bean. Before the fix every
        // property was silently dropped and the assertions below passed vacuously (assertNull on everything).
        XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        Account restored = xmlParser.deserialize(xmlParser.serialize(account, xsc), XmlDeserConfig.create(), Account.class);
        assertNull(restored.getFirstName());
        assertEquals(account.getLastName(), restored.getLastName());
        assertEquals(account.getMiddleName(), restored.getMiddleName());
        assertEquals(account.getEmailAddress(), restored.getEmailAddress());
        assertNotNull(restored.getContact());
        assertEquals(account.getContact().getCity(), restored.getContact().getCity());
        assertEquals(account.getContact().getCountry(), restored.getContact().getCountry());

        xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(true).setExclusion(Exclusion.NONE);
        restored = xmlParser.deserialize(xmlParser.serialize(account, xsc), XmlDeserConfig.create(), Account.class);
        assertNull(restored.getFirstName());
        assertEquals(account.getLastName(), restored.getLastName());
        assertNotNull(restored.getContact());
        assertEquals(account.getContact().getCity(), restored.getContact().getCity());

        xsc = XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(false).setExclusion(Exclusion.NONE);
        restored = xmlParser.deserialize(xmlParser.serialize(Beans.deepBeanToMap(account), xsc), XmlDeserConfig.create(), Account.class);
        List<Account> accountList = xmlParser.deserialize(xmlParser.serialize(N.toList(account), xsc), XmlDeserConfig.create().setElementType(Account.class),
                List.class);
        Object[] accountArray = xmlParser.deserialize(xmlParser.serialize(N.asArray(account), xsc), XmlDeserConfig.create().setElementType(Account.class),
                Object[].class);
        xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(true).setExclusion(Exclusion.NONE);
        restored = xmlParser.deserialize(xmlParser.serialize(Beans.deepBeanToMap(account), xsc), XmlDeserConfig.create(), Account.class);
        assertNotNull(restored);
        assertEquals(1, accountList.size());
        assertEquals(1, accountArray.length);
        assertNull(accountList.get(0).getFirstName());
    }

    @Test
    public void testIgnorePropNames() {
        Account account = createAccountWithContact(Account.class);
        Map<Class<?>, Set<String>> ignored = N.asMap(Account.class, N.toSet("firstName", "contact"));
        Account ignoredAccount = xmlParser.deserialize(xmlParser.serialize(account), XmlDeserConfig.create().setIgnoredPropNames(ignored), Account.class);
        assertNull(ignoredAccount.getFirstName());
        assertNull(ignoredAccount.getContact());

        Map<Class<?>, Set<String>> mapIgnored = N.asMap(Map.class, N.toSet("firstName", "contact"));
        Map<String, Object> ignoredMap = xmlParser.deserialize(xmlParser.serialize(Beans.deepBeanToMap(account)),
                XmlDeserConfig.create().setIgnoredPropNames(mapIgnored), Map.class);
        assertNull(ignoredMap.get("firstName"));
        assertNull(ignoredMap.get("contact"));

        String xml = "<account><gui_1>9b1b4964298a4868a4ab95ccf6a5f987</gui_1><emailAddress>48c6a440fa114de28fad1bf04fa66090@earth.com</emailAddress>"
                + "<firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName>"
                + "<birthDate>1413839551838</birthDate><lastUpdateTime>1413839551838</lastUpdateTime><createdTime>1413839551838</createdTime>"
                + "<contact_1><accountContact><address>ca, US</address><city>sunnyvale</city><state>CA</state><country>U.S.</country></accountContact></contact_1></account>";
        Account unknown = xmlParser.deserialize(xml, XmlDeserConfig.create().setIgnoredPropNames(ignored), Account.class);
        assertNull(unknown.getFirstName());
        assertNull(unknown.getContact());
        assertThrows(ParsingException.class, () -> xmlParser.deserialize(xml,
                XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames((Map<Class<?>, Set<String>>) null), Account.class));
        Account allowed = xmlParser.deserialize(xml,
                XmlDeserConfig.create().setIgnoreUnmatchedProperty(false).setIgnoredPropNames(N.asMap(Account.class, N.toSet("gui_1", "contact_1"))),
                Account.class);
        assertNotNull(allowed.getFirstName());
        assertNull(allowed.getContact());
    }

    @Test
    public void testSerializeCollections() {
        assertEquals(N.toList("abc", "123"), xmlParser.deserialize(xmlParser.serialize(N.toList("abc", "123")), List.class));
        Account[] accounts = N.asArray(createAccount(Account.class), createAccount(Account.class));
        assertTrue(N.equals(accounts, xmlParser.deserialize(xmlParser.serialize(accounts), Account[].class)));
        assertEquals(0, xmlParser.deserialize(xmlParser.serialize(new Object[] {}), String[].class).length);
        assertTrue(N.equals(N.asArray("abc", "123"), xmlParser.deserialize(xmlParser.serialize(N.asArray("abc", "123")), String[].class)));
        assertTrue(xmlParser.deserialize(xmlParser.serialize(new ArrayList<>()), List.class).isEmpty());
        List<Account> accountList = N.toList(createAccount(Account.class), createAccount(Account.class));
        assertTrue(N.equals(accountList,
                xmlParser.deserialize(xmlParser.serialize(accountList), XmlDeserConfig.create().setElementType(Account.class), List.class)));

        MapEntity entity = new MapEntity("MapEntity", Beans.beanToMap(createAccount(Account.class)));
        MapEntity restored = xmlParser.deserialize(xmlParser.serialize(entity), MapEntity.class);
        assertEquals(entity.entityName(), restored.entityName());
        assertEquals(String.valueOf((Object) entity.get("firstName")), String.valueOf((Object) restored.get("firstName")));
    }

    /**
     * P5-02: the default {@code ParserFactory.createXmlParser()} (StAX) silently returned an all-default bean
     * for {@code tagByPropertyName=false} output, because it ignored the {@code name} attribute.
     */
    @Test
    public void reviewFixes20260906_tagByPropertyNameFalseRoundTrip() {
        final Account account = createAccountWithContact(Account.class);

        for (boolean typeInfo : new boolean[] { false, true }) {
            for (boolean pretty : new boolean[] { false, true }) {
                final XmlSerConfig xsc = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(typeInfo).setPrettyFormat(pretty);
                final String xml = xmlParser.serialize(account, xsc);
                assertTrue(xml.contains("<property name=\"firstName\""), xml);

                final Account restored = xmlParser.deserialize(xml, Account.class);
                assertEquals(account.getFirstName(), restored.getFirstName(), xml);
                assertEquals(account.getLastName(), restored.getLastName(), xml);
                assertEquals(account.getEmailAddress(), restored.getEmailAddress(), xml);
                assertNotNull(restored.getContact(), xml);
                assertEquals(account.getContact().getCity(), restored.getContact().getCity(), xml);

                // The same document read as an untyped value keeps the real property names.
                final MapEntity entity = xmlParser.deserialize(xml, MapEntity.class);
                assertEquals(account.getFirstName(), String.valueOf((Object) entity.get("firstName")), xml);
                assertFalse(entity.containsKey("property"), entity.keySet().toString());
            }
        }
    }

    /**
     * P5-08: a map key becomes an element name, so a key that is not a valid XML name used to produce a
     * document that this very parser could not read.
     */
    @Test
    public void reviewFixes20260906_mapKeyMustBeValidXmlName() {
        final Map<Integer, String> intKeyed = new HashMap<>();
        intKeyed.put(1, "a");

        final ParsingException exception = assertThrows(ParsingException.class, () -> xmlParser.serialize(intKeyed));
        assertEquals("Map key '1' is not a valid XML element name", exception.getMessage());

        assertThrows(ParsingException.class, () -> xmlParser.serialize(N.asMap("a b", "v")));

        final Map<String, String> valid = new HashMap<>();
        valid.put("firstName", "a");
        assertEquals(valid, xmlParser.deserialize(xmlParser.serialize(valid), Map.class));
    }

    /**
     * P5-12: a String or file source is one whole document; a second root element or trailing text is reported
     * instead of being silently ignored (the DOM parser type has always rejected it).
     */
    @Test
    public void reviewFixes20260906_contentAfterRootElementIsRejected() {
        final String valid = "<account><firstName>fn</firstName></account>";

        assertThrows(ParsingException.class, () -> xmlParser.deserialize(valid + "<account/>", Account.class));
        assertThrows(ParsingException.class, () -> xmlParser.deserialize(valid + "junk", Account.class));
        assertEquals("fn", xmlParser.deserialize(valid + "  <!-- c --> <?pi x?>", Account.class).getFirstName());

        // A caller-supplied reader is not VALIDATED past the root element, so the first document is still
        // returned. It is not left intact either: the StAX reader buffers ahead, so a small source is read
        // to EOF and a large one is left mid-token -- a second document cannot be read from it.
        assertEquals("fn", xmlParser.deserialize(new StringReader(valid + "<account/>"), Account.class).getFirstName());
    }

    @Test
    public void testBufferedWriter() {
        Writer writer = new StringWriter();
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(writer);
        Account account = createAccount(Account.class);
        xmlParser.serialize(account, bw);
        Objectory.recycle(bw);
        String written = writer.toString();
        assertTrue(written.contains(account.getFirstName()));
        assertEquals(account, xmlParser.deserialize(written, Account.class));

        Writer empty = new StringWriter();
        BufferedXmlWriter emptyBw = Objectory.createBufferedXmlWriter(empty);
        xmlParser.serialize((Account) null, emptyBw);
        assertEquals(Strings.EMPTY, empty.toString());
        Objectory.recycle(emptyBw);
    }
}
