package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;

import lombok.Data;
import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.entity.extendDirty.basic.AccountContact;
import testfixtures.types.WeekDay;

public class KryoParserRoundTripTest extends AbstractParserTest {

    @Override
    protected Parser<?, ?> getParser() {
        return kryoParser;
    }

    private <T> T roundTrip(final Object original, final Class<T> type) {
        return kryoParser.deserialize(kryoParser.serialize(original), type);
    }

    @Override
    @Test
    public void testSerialize_nullObject() throws Exception {
        String str = parser.serialize(null);
        assertNotNull(str);
        assertEquals(parser.serialize(null), str);
    }

    @Override
    @Test
    public void testDeserialize_readerWithClass() throws Exception {
        Account account = createAccount(Account.class);
        String encoded = parser.serialize(account);
        Account restored = parser.deserialize(new java.io.StringReader(encoded), Account.class);
        assertEquals(account.getFirstName(), restored.getFirstName());
    }

    @Test
    public void testSerializeAccountAndCollections() {
        Map<String, String> original = N.asMap("abc", "123");
        assertEquals(original, roundTrip(original, HashMap.class));

        Account account = createAccount(Account.class);
        account.setId(100);
        AccountContact contact = createAccountContact(AccountContact.class);
        contact.setId(1000);
        account.setContact(contact);

        assertEquals(account, roundTrip(account, Account.class));
        assertEquals(contact, roundTrip(contact, AccountContact.class));
        assertEquals(account, roundTrip(new Object[] { account }, Account[].class)[0]);

        Map<?, ?> map = roundTrip(N.asMap("a", 12, 'c', "ddd"), HashMap.class);
        assertEquals(12, map.get("a"));
        assertEquals("ddd", map.get('c'));

        List<Account> accounts = createAccountWithContact(Account.class, 100);
        List<Account> restored = roundTrip(accounts, ArrayList.class);
        assertEquals(accounts.size(), restored.size());
        assertEquals(accounts.get(0).getFirstName(), restored.get(0).getFirstName());
    }

    @Test
    public void testSerializeBeanSpecialChars() {
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

        Bean restored = roundTrip(bean, Bean.class);
        assertArrayEquals(bean.getBytes(), restored.getBytes());
        assertArrayEquals(bean.getChars(), restored.getChars());
        assertArrayEquals(bean.getStrings(), restored.getStrings());
        assertEquals(bean.getTypeList().size(), restored.getTypeList().size());
        assertEquals(kryoParser.serialize(bean), kryoParser.serialize(restored));
    }

    @Test
    public void testSerializeXBean() {
        Account account = createAccount(Account.class);
        account.setContact(createAccountContact(AccountContact.class));

        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2(5L);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);
        xBean.setTypeString(">string黎< > </ <//、");
        xBean.setWeekDay(WeekDay.THURSDAY);
        List typeList = new ArrayList();
        typeList.add(account.getFirstName());
        typeList.add(account);
        typeList.add(account.getContact());
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        XBean restored = roundTrip(xBean, XBean.class);
        assertEquals(roundTrip(xBean, XBean.class), roundTrip(restored, XBean.class));
        assertEquals(xBean.getTypeString(), restored.getTypeString());
        assertEquals(WeekDay.THURSDAY, restored.getWeekDay());

        xBean.setTypeChar('<');
        xBean.setTypeGenericList(
                N.toList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(N.toSet(1L, 2L));
        restored = roundTrip(xBean, XBean.class);
        assertEquals(xBean.getTypeGenericSet(), restored.getTypeGenericSet());
        assertEquals(xBean.getTypeGenericList(), restored.getTypeGenericList());

        xBean.setTypeString("");
        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());
        List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);
        Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);
        restored = roundTrip(xBean, XBean.class);
        assertEquals("", restored.getTypeString());
        assertEquals(3, restored.getTypeGenericList().size());
        assertEquals("", restored.getTypeGenericMap4().get("aaabbbccc"));

        XBean withNullSet = new XBean();
        Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        withNullSet.setTypeSet(typeSet);
        withNullSet.setWeekDay(WeekDay.FRIDAY);
        withNullSet.setTypeChar('0');
        assertEquals(withNullSet, roundTrip(withNullSet, XBean.class));

        XBean original = createBigXBean(10);
        restored = roundTrip(original, XBean.class);
        assertNotNull(restored);
        assertEquals(original.getTypeString(), restored.getTypeString());
        assertEquals(original.getWeekDay(), restored.getWeekDay());
        assertEquals(original.getTypeList().size(), restored.getTypeList().size());
    }

    public static XBean createBigXBean(int size) {
        Account account = createAccount(Account.class);
        account.setContact(createAccountContact(AccountContact.class));

        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2(5L);
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
        typeList.add(null);
        typeList.add(new HashMap<>());
        xBean.setTypeList(typeList);

        Map<String, Account> typeGenericMap = new HashMap<>();
        typeGenericMap.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap.put("null", null);
        xBean.setTypeGenericMap(typeGenericMap);

        Map<String, Object> typeGenericMap2 = new TreeMap<>();
        typeGenericMap2.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap2.put("null", null);
        xBean.setTypeGenericMap2(typeGenericMap2);

        Map typeMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            typeMap.put(createAccount(Account.class), createAccount(Account.class));
        }
        typeMap.put("null", null);
        xBean.setTypeMap(typeMap);
        return xBean;
    }

    @Data
    public static class Bean {
        private byte[] bytes;
        private char[] chars;
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
        private Map<Object, Object> typeGenericMap4;
        private Map typeMap;
    }
}
