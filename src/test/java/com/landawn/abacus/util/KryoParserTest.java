/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.types.WeekDay;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

public class KryoParserTest extends AbstractParserTest {

    @Test
    public void testSerialize_map() {

        String str = kryoParser.serialize(CommonUtil.asMap("abc", "123"));

        Object obj = kryoParser.deserialize(str, HashMap.class);

        N.println(obj);
    }

    @Test
    public void testSerialize() {
        Account account = createAccount(Account.class);
        account.setId(100);

        AccountContact contact = createAccountContact(AccountContact.class);
        contact.setId(1000);
        account.setContact(contact);

        println(kryoParser.serialize(contact));
        println(kryoParser.serialize(account));

        println(kryoParser.deserialize(kryoParser.serialize(account), Account.class));

        println(kryoParser.serialize(CommonUtil.asArray(account)));

        println(kryoParser.serialize(CommonUtil.asMap("a", 12, 'c', "ddd")));

        println(kryoParser.serialize(account));

        Object obj = kryoParser.deserialize(kryoParser.serialize(new Object[] { account }), Account[].class);
        println(obj);
    }

    @Test
    public void testSerialize1() {
        Bean bean = new Bean();
        bean.setTypeList(CommonUtil.asList(
                "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★', '\n',
                '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                new String[] {
                        "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                        "\r", "\t", "\"", "\'" }));

        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        String jsonStr = kryoParser.serialize(bean);
        println(jsonStr);

        Bean xmlBean = kryoParser.deserialize(jsonStr, Bean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(kryoParser.serialize(bean));
        N.println(kryoParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        // assertEquals(bean, xmlBean);
        // assertEquals(N.deserialize(Bean.class, N.serialize(bean)),
        // N.deserialize(Bean.class, N.serialize(xmlBean)));
        assertEquals(kryoParser.serialize(bean), kryoParser.serialize(xmlBean));
        // assertEquals(N.stringOf(bean), N.stringOf(xmlBean));
        N.println(kryoParser.serialize(bean));
        N.println(kryoParser.serialize(xmlBean));

        N.println(kryoParser.deserialize(kryoParser.serialize(bean), Bean.class));
        N.println(kryoParser.deserialize(kryoParser.serialize(xmlBean), Bean.class));
    }

    @Test
    public void testSerialize2() {
        println(String.valueOf((char) 0));

        Account account = createAccount(Account.class);
        AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        N.println(kryoParser.serialize(account));

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

        String jsonStr = kryoParser.serialize(xBean);
        println(jsonStr);

        String st = CommonUtil.stringOf(xBean);
        println(st);

        XBean xmlBean = kryoParser.deserialize(jsonStr, XBean.class);
        N.println(xBean);
        N.println(xmlBean);
        N.println(kryoParser.serialize(xBean));
        N.println(kryoParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(xBean));
        N.println(CommonUtil.stringOf(xmlBean));
        // assertEquals(N.stringOf(xBean), N.stringOf(xmlBean));
        assertEquals(kryoParser.deserialize(kryoParser.serialize(xBean), XBean.class), kryoParser.deserialize(kryoParser.serialize(xmlBean), XBean.class));

        // assertEquals(KRYO.serialize(xBean), KRYO.serialize(xmlBean));
        // assertEquals(N.stringOf(xBean), N.stringOf(xmlBean));
        N.println(kryoParser.serialize(xBean));
        N.println(kryoParser.serialize(xmlBean));

        N.println(kryoParser.deserialize(kryoParser.serialize(xBean), XBean.class));
        N.println(kryoParser.deserialize(kryoParser.serialize(xmlBean), XBean.class));
    }

    @Test
    public void testGenericType() {
        XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                CommonUtil.asList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(CommonUtil.asSet(1L, 2L));

        String jsonStr = kryoParser.serialize(xBean);
        println(jsonStr);
        println(kryoParser.serialize(kryoParser.deserialize(jsonStr, XBean.class)));

        // assertEquals(xBean, FasterJSON.deserialize(XBean.class, jsonStr));
    }


    @Test
    @Tag("slow-test")
    public void testPerformanceForBigBean() throws Exception {
        kryoParser.deserialize(kryoParser.serialize(createBigXBean(1)), XBean.class);
        File file = new File("./src/test/resources/bigBean.txt");
        IOUtil.write(kryoParser.serialize(createBigXBean(10000)), file);

        Profiler.run(this, "executeBigBean", 1, 3, 1).printResult();

        file.delete();
    }

    void executeBigBean() {
        kryoParser.deserialize(kryoParser.serialize(createBigXBean(10000)), XBean.class);
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

        Set<Long> typeGenericSet = CommonUtil.asSortedSet();
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
        typeGenericMap2.put("bookList", CommonUtil.asList(createAccount(Account.class)));
        xBean.setTypeGenericMap2(typeGenericMap2);

        Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put("aaabbbccc", "");
        typeGenericMap4.put("bookList", CommonUtil.asList(createAccount(Account.class)));
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
        typeMap.put("bookList", CommonUtil.asList(createAccount(Account.class)));
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

        String xml = kryoParser.serialize(xBean);
        println(xml);

        String st = CommonUtil.stringOf(xBean);
        println(st);

        kryoParser.deserialize(xml, XBean.class);

        //        XBean xmlBean = FasterJSON.deserialize(XBean.class, xml);
        //
        //        assertEquals(N.stringOf(xBean), N.stringOf(xmlBean));
    }

    @Test
    public void testSerialize5() {
        List<Account> accounts = createAccountWithContact(Account.class, 100);
        String xml = kryoParser.serialize(accounts);
        println(xml);

        List<Account> xmlAccounts = kryoParser.deserialize(xml, ArrayList.class);

        N.println(CommonUtil.stringOf(accounts));
        N.println(CommonUtil.stringOf(xmlAccounts));

        //        assertEquals(38080, N.stringOf(accounts).length());
        //        assertEquals(39380, N.stringOf(xmlAccounts).length());
    }

    //    public void testSerialize6() {
    //        Account account = createAccountWithContact(Account.class);
    //        String xml = kryoParser.serialize(account);
    //        println(xml);
    //
    //        kryoParser.deserialize(com.landawn.abacus.entity.implDirty.basic.Account.class, xml);
    //
    //        //        com.landawn.abacus.entity.implDirty.basic.Account xmlBean = FasterJSON.deserialize(com.landawn.abacus.entity.implDirty.basic.Account.class,
    //        //                xml);
    //        //         assertEquals(N.stringOf(account), N.stringOf(xmlBean));
    //    }

    @Test
    public void testXBean() {
        XBean bean = new XBean();
        Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        String xml = kryoParser.serialize(bean);
        println(xml);

        XBean xmlBean = kryoParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(kryoParser.serialize(bean));
        N.println(kryoParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(kryoParser.deserialize(kryoParser.serialize(bean), XBean.class), kryoParser.deserialize(kryoParser.serialize(xmlBean), XBean.class));

        // assertEquals(N.serialize(bean), N.serialize(xmlBean));
        // assertEquals(N.asString(bean), N.asString(xmlBean));
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

            if (!Objects.equals(intList, other.intList) || !Objects.equals(shortList, other.shortList) || !Objects.equals(strList, other.strList) || !Objects.equals(xmlGregorianCalendar, other.xmlGregorianCalendar)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "Bean_1 [strList=" + strList + ", intList=" + intList + ", shortList=" + shortList + ", xmlGregorianCalendar=" + xmlGregorianCalendar + "]";
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
            return "Customer [name=" + name + ", ch=" + ch + ", age=" + age + ", id=" + id + "]";
        }
    }

    public static class Bean {
        private byte[] bytes;
        private char[] chars;
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

            if (!Arrays.equals(bytes, other.bytes) || !Arrays.equals(chars, other.chars) || !Arrays.equals(strings, other.strings) || !Objects.equals(typeList, other.typeList)) {
                return false;
            }

            if (!Objects.equals(typeSet, other.typeSet)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "Bean [bytes=" + Arrays.toString(bytes) + ", chars=" + Arrays.toString(chars) + ", strings=" + Arrays.toString(strings) + ", typeList="
                    + typeList + ", typeSet=" + typeSet + "]";
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

            if ((typeBoolean != other.typeBoolean) || !Objects.equals(typeBoolean2, other.typeBoolean2) || (typeByte != other.typeByte) || !Objects.equals(typeCalendar, other.typeCalendar)) {
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
            return "Bean [typeBoolean=" + typeBoolean + ", typeBoolean2=" + typeBoolean2 + ", typeChar=" + typeChar + ", typeChar2=" + typeChar2 + ", typeByte="
                    + typeByte + ", typeShort=" + typeShort + ", typeInt=" + typeInt + ", typeLong=" + typeLong + ", typeLong2=" + typeLong2 + ", typeFloat="
                    + typeFloat + ", typeDouble=" + typeDouble + ", typeString=" + typeString + ", typeCalendar=" + typeCalendar + ", typeDate=" + typeDate
                    + ", typeSqlDate=" + typeSqlDate + ", typeSqlTime=" + typeSqlTime + ", typeSqlTimestamp=" + typeSqlTimestamp + ", typeGenericList="
                    + typeGenericList + ", typeGenericSet=" + typeGenericSet + ", typeList=" + typeList + ", typeSet=" + typeSet + ", typeGenericMap="
                    + typeGenericMap + ", typeGenericMap2=" + typeGenericMap2 + ", typeGenericMap3=" + typeGenericMap3 + ", typeGenericMap4=" + typeGenericMap4
                    + ", typeMap=" + typeMap + "]";
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
            return "JaxBBean [string=" + string + ", list=" + list + ", map=" + map + "]";
        }
    }
}
