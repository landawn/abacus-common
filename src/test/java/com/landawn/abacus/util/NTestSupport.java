package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.Predicate;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.entity.extendDirty.basic.AccountContact;
import testfixtures.entity.extendDirty.basic.ExtendDirtyBasicPNL;
import testfixtures.types.WeekDay;

public abstract class NTestSupport extends AbstractParserTest {

    protected static final double DELTA = 0.000001d;
    protected static final float DELTAf = 0.000001f;
    protected static final IntPredicate IS_EVEN_INT = x -> x % 2 == 0;
    protected static final IntPredicate IS_ODD_INT = x -> x % 2 != 0;
    protected static final Predicate<Integer> IS_EVEN_INTEGER = x -> x % 2 == 0;
    protected static final Predicate<String> STRING_NOT_EMPTY = s -> s != null && !s.isEmpty();
    protected static final String TEST_JSON = "{\"name\":\"John\",\"age\":30}";
    protected static final String TEST_XML = "<person><name>John</name><age>30</age></person>";
    protected static final String TEST_JSON_ARRAY = "[1,2,3,4,5]";

    protected ExecutorService executorService;
    protected ScheduledExecutorService scheduledExecutorService;

    @TempDir
    File tempDir;

    protected boolean[] booleanArray;
    protected char[] charArray;
    protected byte[] byteArray;
    protected short[] shortArray;
    protected int[] intArray;
    protected long[] longArray;
    protected float[] floatArray;
    protected double[] doubleArray;
    protected String[] stringArray;
    protected Integer[] integerArray;
    protected List<String> stringList;
    protected List<Integer> integerList;
    protected Set<String> stringSet;
    protected Map<String, Integer> stringIntMap;

    protected boolean[] emptyBooleanArray = new boolean[0];
    protected char[] emptyCharArray = new char[0];
    protected int[] emptyIntArray = new int[0];
    protected String[] emptyStringArray = new String[0];

    protected boolean[] singleBooleanArray = new boolean[] { true };
    protected int[] singleIntArray = new int[] { 42 };
    protected String[] singleStringArray = new String[] { "single" };

    protected int[] duplicateIntArray = new int[] { 1, 1, 1, 1, 1 };
    protected String[] duplicateStringArray = new String[] { "dup", "dup", "dup" };

    protected Integer[] nullContainingArray = new Integer[] { 1, null, 3, null, 5 };
    protected List<String> nullContainingList = Arrays.asList("a", null, "c", null, "e");

    protected int[] largeIntArray;
    protected List<Integer> largeIntList;

    double add_1() {
        final int[] a = new int[1000];

        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }

        double d = 0;

        for (final int element : a) {
            d += element;
        }

        return d;
    }

    double add_2() {
        final int[] a = new int[1000];

        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }

        return N.sum(a);
    }

    public void execute() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        abacusXmlParser.deserialize(abacusXmlParser.serialize(account), Account.class);

    }

    protected String getDomainName() {
        return ExtendDirtyBasicPNL._DN;
    }

    @BeforeEach
    public void setUp() {
        executorService = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        booleanArray = new boolean[] { true, false, true, false, true };
        charArray = new char[] { 'a', 'b', 'c', 'd', 'e' };
        byteArray = new byte[] { 1, 2, 3, 4, 5 };
        shortArray = new short[] { 1, 2, 3, 4, 5 };
        intArray = new int[] { 1, 2, 3, 4, 5 };
        longArray = new long[] { 1L, 2L, 3L, 4L, 5L };
        floatArray = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        doubleArray = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        stringArray = new String[] { "one", "two", "three", "four", "five" };
        integerArray = new Integer[] { 1, 2, 3, 4, 5 };
        stringList = Arrays.asList("one", "two", "three", "four", "five");
        integerList = Arrays.asList(1, 2, 3, 4, 5);
        stringSet = new HashSet<>(stringList);
        stringIntMap = new HashMap<>();
        stringIntMap.put("one", 1);
        stringIntMap.put("two", 2);
        stringIntMap.put("three", 3);
        largeIntArray = new int[10000];
        largeIntList = new ArrayList<>(10000);

        for (int i = 0; i < 10000; i++) {
            largeIntArray[i] = i;
            largeIntList.add(i);
        }
    }

    @AfterEach
    public void tearDown() {
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Executor service did not terminate in time.");
            }

            if (!scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Scheduled executor service did not terminate in time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected static Collection<String> splitToChars(String s) {
        List<String> result = new ArrayList<>(s.length());

        for (char ch : s.toCharArray()) {
            result.add(String.valueOf(ch));
        }

        return result;
    }

    @SafeVarargs
    protected static <T> List<T> toMutableList(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    protected static final boolean[] EMPTY_BOOLEAN_ARRAY_CONST = {};
    protected static final char[] EMPTY_CHAR_ARRAY_CONST = {};
    protected static final byte[] EMPTY_BYTE_ARRAY_CONST = {};
    protected static final short[] EMPTY_SHORT_ARRAY_CONST = {};
    protected static final int[] EMPTY_INT_ARRAY_CONST = {};
    protected static final long[] EMPTY_LONG_ARRAY_CONST = {};
    protected static final float[] EMPTY_FLOAT_ARRAY_CONST = {};
    protected static final double[] EMPTY_DOUBLE_ARRAY_CONST = {};
    protected static final Object[] EMPTY_OBJECT_ARRAY_CONST = {};

    protected <T> List<T> iteratorToList(Iterator<T> iterator) {
        List<T> list = new ArrayList<>();

        if (iterator != null) {
            iterator.forEachRemaining(list::add);
        }

        return list;
    }

    protected boolean contains(char[] array, char value) {
        for (char c : array) {
            if (c == value) {
                return true;
            }
        }

        return false;
    }

    protected boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) {
                return true;
            }
        }

        return false;
    }

    public void valueOf() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);
        abacusXmlParser.deserialize(abacusXmlParser.serialize(account), Account.class);

    }

    public void executeBigBean() {
        abacusXmlParser.deserialize(abacusXmlParser.serialize(createBigXBean(10000)), XBean.class);
    }

    public static XBean createBigXBean(final int size) {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
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

        final List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        final Set<Long> typeGenericSet = CommonUtil.toSortedSet();
        typeGenericSet.add(1332333L);
        typeGenericSet.add(Long.MAX_VALUE);
        typeGenericSet.add(Long.MIN_VALUE);
        xBean.setTypeGenericSet(typeGenericSet);

        final List typeList = new ArrayList();
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

        final Set typeSet = new HashSet<>();
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

        final Map<String, Account> typeGenericMap = new HashMap<>();
        typeGenericMap.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap.put(account.getLastName(), account);
        typeGenericMap.put(null, createAccount(Account.class));
        typeGenericMap.put("null", null);
        xBean.setTypeGenericMap(typeGenericMap);

        final Map<String, Object> typeGenericMap2 = new TreeMap<>();
        typeGenericMap2.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap2.put(account.getLastName(), createAccount(Account.class));
        typeGenericMap2.put("null", null);
        typeGenericMap2.put("bookList", CommonUtil.toList(createAccount(Account.class)));
        xBean.setTypeGenericMap2(typeGenericMap2);

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put("aaabbbccc", "");
        typeGenericMap4.put("bookList", CommonUtil.toList(createAccount(Account.class)));
        typeGenericMap4.put(" ", " ");
        typeGenericMap4.put(new HashMap<>(), " ");
        typeGenericMap4.put(new ArrayList<>(), new HashSet<>());
        typeGenericMap4.put(typeGenericMap2, typeGenericMap);
        xBean.setTypeGenericMap4(typeGenericMap4);

        final Map typeMap = new HashMap<>();

        for (int i = 0; i < size; i++) {
            typeMap.put(createAccount(Account.class), createAccount(Account.class));
        }

        typeMap.put("null", null);
        typeMap.put("bookList", CommonUtil.toList(createAccount(Account.class)));
        typeMap.put(" ", " ");
        typeMap.put(new HashMap<>(), " ");
        typeMap.put(new ArrayList<>(), new HashSet<>());
        typeMap.put(null, null);
        typeMap.put(new HashMap<>(), null);
        typeMap.put(new ArrayList<>(), new HashSet<>());
        xBean.setTypeMap(typeMap);

        return xBean;
    }

    public static class Bean_1 {
        protected List<String> strList;
        protected List intList;
        protected List<Short> shortList;
        protected XMLGregorianCalendar xmlGregorianCalendar;

        public List<String> getStrList() {
            return strList;
        }

        public void setStrList(final List<String> strList) {
            this.strList = strList;
        }

        public List getIntList() {
            return intList;
        }

        public void setIntList(final List intList) {
            this.intList = intList;
        }

        public List<Short> getShortList() {
            return shortList;
        }

        public void setShortList(final List<Short> shortList) {
            this.shortList = shortList;
        }

        public XMLGregorianCalendar getXMLGregorianCalendar() {
            return xmlGregorianCalendar;
        }

        public void setXMLGregorianCalendar(final XMLGregorianCalendar xmlGregorianCalendar) {
            this.xmlGregorianCalendar = xmlGregorianCalendar;
        }

        @Override
        public int hashCode() {
            return Objects.hash(intList, shortList, strList, xmlGregorianCalendar);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final Bean_1 other = (Bean_1) obj;

            if (!Objects.equals(intList, other.intList) || !Objects.equals(shortList, other.shortList) || !Objects.equals(strList, other.strList)
                    || !Objects.equals(xmlGregorianCalendar, other.xmlGregorianCalendar)) {
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
        public void setChar(final char ch) {
            this.ch = ch;
        }

        public char getChar() {
            return ch;
        }

        @XmlElement
        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        @XmlElement
        public void setAge(final int age) {
            this.age = age;
        }

        public int getId() {
            return id;
        }

        @XmlAttribute
        public void setId(final int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, ch, id, name);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final Customer other = (Customer) obj;

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
        protected byte[] bytes;
        protected char[] chars;
        protected String[] strings;
        protected List typeList;
        protected Set typeSet;

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(final byte[] bytes) {
            this.bytes = bytes;
        }

        public char[] getChars() {
            return chars;
        }

        public void setChars(final char[] chars) {
            this.chars = chars;
        }

        public String[] getStrings() {
            return strings;
        }

        public void setStrings(final String[] strings) {
            this.strings = strings;
        }

        public List getTypeList() {
            return typeList;
        }

        public void setTypeList(final List typeList) {
            this.typeList = typeList;
        }

        public Set getTypeSet() {
            return typeSet;
        }

        public void setTypeSet(final Set typeSet) {
            this.typeSet = typeSet;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(bytes), Arrays.hashCode(chars), Arrays.hashCode(strings), typeList, typeSet);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final Bean other = (Bean) obj;

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
            return "Bean [bytes=" + Arrays.toString(bytes) + ", chars=" + Arrays.toString(chars) + ", strings=" + Arrays.toString(strings) + ", typeList="
                    + typeList + ", typeSet=" + typeSet + "]";
        }
    }

    public static class XBean {
        protected boolean typeBoolean;
        protected Boolean typeBoolean2;
        protected char typeChar;
        protected Character typeChar2;
        protected byte typeByte;
        protected short typeShort;
        protected int typeInt;
        protected long typeLong;
        protected Long typeLong2;
        protected float typeFloat;
        protected double typeDouble;
        protected String typeString;
        protected Calendar typeCalendar;
        protected java.util.Date typeDate;
        protected Date typeSqlDate;
        protected Time typeSqlTime;
        protected Timestamp typeSqlTimestamp;
        protected WeekDay weekDay;
        protected List<Date> typeGenericList;
        protected Set<Long> typeGenericSet;
        protected List typeList;
        protected Set typeSet;
        protected Map<String, Account> typeGenericMap;
        protected Map<String, Object> typeGenericMap2;
        protected Map<Object, String> typeGenericMap3;
        protected Map<Object, Object> typeGenericMap4;
        protected Map typeMap;

        public boolean getTypeBoolean() {
            return typeBoolean;
        }

        public void setTypeBoolean(final boolean typeBoolean) {
            this.typeBoolean = typeBoolean;
        }

        public Boolean getTypeBoolean2() {
            return typeBoolean2;
        }

        public void setTypeBoolean2(final Boolean typeBoolean2) {
            this.typeBoolean2 = typeBoolean2;
        }

        public char getTypeChar() {
            return typeChar;
        }

        public void setTypeChar(final char typeChar) {
            this.typeChar = typeChar;
        }

        public Character getTypeChar2() {
            return typeChar2;
        }

        public void setTypeChar2(final Character typeChar2) {
            this.typeChar2 = typeChar2;
        }

        public byte getTypeByte() {
            return typeByte;
        }

        public void setTypeByte(final byte typeByte) {
            this.typeByte = typeByte;
        }

        public short getTypeShort() {
            return typeShort;
        }

        public void setTypeShort(final short typeShort) {
            this.typeShort = typeShort;
        }

        public int getTypeInt() {
            return typeInt;
        }

        public void setTypeInt(final int typeInt) {
            this.typeInt = typeInt;
        }

        public long getTypeLong() {
            return typeLong;
        }

        public void setTypeLong(final long typeLong) {
            this.typeLong = typeLong;
        }

        public Long getTypeLong2() {
            return typeLong2;
        }

        public void setTypeLong2(final Long typeLong2) {
            this.typeLong2 = typeLong2;
        }

        public float getTypeFloat() {
            return typeFloat;
        }

        public void setTypeFloat(final float typeFloat) {
            this.typeFloat = typeFloat;
        }

        public double getTypeDouble() {
            return typeDouble;
        }

        public void setTypeDouble(final double typeDouble) {
            this.typeDouble = typeDouble;
        }

        public String getTypeString() {
            return typeString;
        }

        public void setTypeString(final String typeString) {
            this.typeString = typeString;
        }

        public Calendar getTypeCalendar() {
            return typeCalendar;
        }

        public void setTypeCalendar(final Calendar typeCalendar) {
            this.typeCalendar = typeCalendar;
        }

        public java.util.Date getTypeDate() {
            return typeDate;
        }

        public void setTypeDate(final java.util.Date typeDate) {
            this.typeDate = typeDate;
        }

        public Date getTypeSqlDate() {
            return typeSqlDate;
        }

        public void setTypeSqlDate(final Date typeSqlDate) {
            this.typeSqlDate = typeSqlDate;
        }

        public Time getTypeSqlTime() {
            return typeSqlTime;
        }

        public void setTypeSqlTime(final Time typeSqlTime) {
            this.typeSqlTime = typeSqlTime;
        }

        public Timestamp getTypeSqlTimestamp() {
            return typeSqlTimestamp;
        }

        public void setTypeSqlTimestamp(final Timestamp typeSqlTimestamp) {
            this.typeSqlTimestamp = typeSqlTimestamp;
        }

        public WeekDay getWeekDay() {
            return weekDay;
        }

        public void setWeekDay(final WeekDay weekDay) {
            this.weekDay = weekDay;
        }

        public List<Date> getTypeGenericList() {
            return typeGenericList;
        }

        public void setTypeGenericList(final List<Date> typeGenericList) {
            this.typeGenericList = typeGenericList;
        }

        public Set<Long> getTypeGenericSet() {
            return typeGenericSet;
        }

        public void setTypeGenericSet(final Set<Long> typeGenericSet) {
            this.typeGenericSet = typeGenericSet;
        }

        public List getTypeList() {
            return typeList;
        }

        public void setTypeList(final List typeList) {
            this.typeList = typeList;
        }

        public Set getTypeSet() {
            return typeSet;
        }

        public void setTypeSet(final Set typeSet) {
            this.typeSet = typeSet;
        }

        public Map<String, Account> getTypeGenericMap() {
            return typeGenericMap;
        }

        public void setTypeGenericMap(final Map<String, Account> typeGenericMap) {
            this.typeGenericMap = typeGenericMap;
        }

        public Map getTypeMap() {
            return typeMap;
        }

        public void setTypeMap(final Map typeMap) {
            this.typeMap = typeMap;
        }

        public Map<String, Object> getTypeGenericMap2() {
            return typeGenericMap2;
        }

        public void setTypeGenericMap2(final Map<String, Object> typeGenericMap2) {
            this.typeGenericMap2 = typeGenericMap2;
        }

        public Map<Object, String> getTypeGenericMap3() {
            return typeGenericMap3;
        }

        public void setTypeGenericMap3(final Map<Object, String> typeGenericMap3) {
            this.typeGenericMap3 = typeGenericMap3;
        }

        public Map<Object, Object> getTypeGenericMap4() {
            return typeGenericMap4;
        }

        public void setTypeGenericMap4(final Map<Object, Object> typeGenericMap4) {
            this.typeGenericMap4 = typeGenericMap4;
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeBoolean, typeBoolean2, typeByte, typeCalendar, typeChar, typeChar2, typeDate, typeDouble, typeFloat, typeGenericList,
                    typeGenericMap, typeGenericMap2, typeGenericMap3, typeGenericMap4, typeGenericSet, typeInt, typeList, typeLong, typeLong2, typeMap, typeSet,
                    typeShort, typeSqlDate, typeSqlTime, typeSqlTimestamp, typeString);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final XBean other = (XBean) obj;

            if ((typeBoolean != other.typeBoolean) || !Objects.equals(typeBoolean2, other.typeBoolean2) || (typeByte != other.typeByte)
                    || !Objects.equals(typeCalendar, other.typeCalendar)) {
                return false;
            }

            if ((typeChar != other.typeChar) || !Objects.equals(typeChar2, other.typeChar2) || !Objects.equals(typeDate, other.typeDate)
                    || (Double.doubleToLongBits(typeDouble) != Double.doubleToLongBits(other.typeDouble))) {
                return false;
            }

            if ((Float.floatToIntBits(typeFloat) != Float.floatToIntBits(other.typeFloat)) || !Objects.equals(typeGenericList, other.typeGenericList)
                    || !Objects.equals(typeGenericMap, other.typeGenericMap) || !Objects.equals(typeGenericMap2, other.typeGenericMap2)) {
                return false;
            }

            if (!Objects.equals(typeGenericMap3, other.typeGenericMap3) || !Objects.equals(typeGenericMap4, other.typeGenericMap4)
                    || !Objects.equals(typeGenericSet, other.typeGenericSet) || (typeInt != other.typeInt)) {
                return false;
            }

            if (!Objects.equals(typeList, other.typeList) || (typeLong != other.typeLong) || !Objects.equals(typeLong2, other.typeLong2)
                    || !Objects.equals(typeMap, other.typeMap)) {
                return false;
            }

            if (!Objects.equals(typeSet, other.typeSet) || (typeShort != other.typeShort) || !Objects.equals(typeSqlDate, other.typeSqlDate)
                    || !Objects.equals(typeSqlTime, other.typeSqlTime)) {
                return false;
            }

            if (!Objects.equals(typeSqlTimestamp, other.typeSqlTimestamp) || !Objects.equals(typeString, other.typeString)) {
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

    public static class TestBean {
        protected String name;
        protected int value;
        protected List<String> items;
        protected Map<String, Integer> properties;

        public TestBean() {
        }

        public TestBean(String name, int value, List<String> items, Map<String, Integer> properties) {
            this.name = name;
            this.value = value;
            this.items = items;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }

        public Map<String, Integer> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Integer> properties) {
            this.properties = properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestBean testBean = (TestBean) o;
            return value == testBean.value && Objects.equals(name, testBean.name) && Objects.equals(items, testBean.items)
                    && Objects.equals(properties, testBean.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, items, properties);
        }
    }

    protected TestBean createSampleBean() {
        List<String> items = new ArrayList<>(Arrays.asList("item1", "item2"));
        Map<String, Integer> props = new LinkedHashMap<>();
        props.put("key1", 100);
        props.put("key2", 200);
        return new TestBean("testName", 123, items, props);
    }

    protected String getExpectedJsonForSampleBean(boolean pretty) {
        if (pretty) {
            return String.join(IOUtil.LINE_SEPARATOR_UNIX,
                    "{\n" + "    \"name\": \"testName\",\n" + "    \"value\": 123,\n" + "    \"items\": [\"item1\", \"item2\"],\n" + "    \"properties\": {\n"
                            + "        \"key1\": 100,\n" + "        \"key2\": 200\n" + "    }\n" + "}");
        } else {
            return "{\"name\": \"testName\", \"value\": 123, \"items\": [\"item1\", \"item2\"], \"properties\": {\"key1\": 100, \"key2\": 200}}";
        }
    }

    public static class JaxbBean {
        protected String string;
        protected List<String> list;
        protected Map<String, String> map;

        public String getString() {
            return string;
        }

        public void setString(final String string) {
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
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final JaxbBean other = (JaxbBean) obj;

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

    protected static class Person {
        String name;
        int age;
        String department;

        Person(String name, int age) {
            this(name, age, null);
        }

        Person(String name, int age, String department) {
            this.name = name;
            this.age = age;
            this.department = department;
        }

        String getName() {
            return name;
        }

        int getAge() {
            return age;
        }

        String getDepartment() {
            return department;
        }
    }

    protected static class CustomIterable<T> implements Iterable<T> {
        protected final List<T> data;

        CustomIterable(List<T> data) {
            this.data = data;
        }

        @Override
        public Iterator<T> iterator() {
            return data.iterator();
        }
    }

    @XmlRootElement(name = "person")
    protected static class TestPerson {
        protected String name;
        protected int age;

        public TestPerson() {
        }

        TestPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof TestPerson other)) {
                return false;
            }

            return age == other.age && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    // ---- concat(char[]...) varargs version taking multiple arrays - empty-inner-array path ----

    // commonSet(Collection) - test the multi-collection path with 3 collections

    // replaceAll(List, oldVal, newVal) - verify it returns count

    // setAll(List, IntFunction)

    // ---- setAll(List, IntFunction) - triggers the ListIterator path for non-RandomAccess ----

    // ---- setAll(List, Throwables.IntObjFunction) - triggers the ListIterator path ----

    // removeAt(char[], int...) with multiple indices

    // ---- removeAt(short[], int...) with multiple indices ----

    // ---- removeAt(long[], int...) with multiple indices ----

    // ---- removeAt(float[], int...) with multiple indices ----

    // ---- removeAt(double[], int...) with multiple indices ----

    // ---- removeDuplicates with fromIndex==toIndex (returns EMPTY) and single-element range ----

    // replaceRange(boolean[], fromIndex, toIndex, boolean[]) - edge cases

    // replaceRange(char[], fromIndex, toIndex, char[]) - edge cases

    // Cover package-protected range-specific duplicate detection for char arrays.

    // containsDuplicates(char[], fromIndex, toIndex, isSorted) - additional edge cases

    // sumInt(Collection, fromIndex, toIndex, func) - with non-list collection

    // sumLong(Collection, fromIndex, toIndex, func) - with non-list collection

    // sumDouble(Collection, fromIndex, toIndex, func) - with non-list collection

    // averageInt(Collection, fromIndex, toIndex, func) - with non-list collection

    // median(long, long, long) and median(double, double, double)

    // top(int[], fromIndex, toIndex, n, Comparator) rejects a null comparator.

    // filter(Collection, fromIndex, toIndex, Predicate, IntFunction) - with supplier

    // ---- flatMap(Collection, int, int, Function, IntFunction) with range ----

    // distinct(Iterable) using non-Collection Iterable (exercises the else branch)

    // ---- distinctBy(Collection, int, int, Function) with range ----

    // count(primitive[], int, int, predicate) range variants

    // ---- merge(Collection, BiFunction, IntFunction) with 3+ iterables ----

    // merge(Collection, BiFunction, IntFunction) with single-element and null-iterable edge cases

    // zip(Iterable, Iterable, defaults, BiFunction) using non-Collection Iterable

    // zip(Iterable, Iterable, Iterable, defaults, TriFunction) using non-Collection Iterables

    // ---- zip(Iterable, Iterable, defaultA, defaultB, BiFunction) ----

    // ---- zip(Iterable, Iterable, Iterable, defaultA, defaultB, defaultC, TriFunction) ----

    // zip(Iterable, Iterable, Iterable, TriFunction) - three iterables, no defaults

    // groupBy(Collection, fromIndex, toIndex, Function, Supplier) - with non-list collection

    protected static void assertWaitingWorkerDoesNotPullAfterCoordinatorInterruption(final boolean indexed) throws Exception {
        final AtomicInteger nextCalls = new AtomicInteger();
        final AtomicInteger consumerCalls = new AtomicInteger();
        final Iterator<Integer> iterator = new Iterator<>() {
            protected boolean consumed;

            @Override
            public boolean hasNext() {
                return !consumed;
            }

            @Override
            public Integer next() {
                consumed = true;
                nextCalls.incrementAndGet();
                return 1;
            }
        };
        final AtomicReference<Thread> workerRef = new AtomicReference<>();
        final ExecutorService executor = Executors.newFixedThreadPool(1, command -> {
            final Thread worker = new Thread(command, "NTest-cancellation-race-worker");
            workerRef.set(worker);
            return worker;
        });
        final AtomicReference<Throwable> coordinatorFailure = new AtomicReference<>();
        final Thread coordinator = new Thread(() -> {
            try {
                if (indexed) {
                    N.forEachIndexedInParallel(iterator, (index, value) -> consumerCalls.incrementAndGet(), 1, executor);
                } else {
                    N.forEachInParallel(iterator, value -> consumerCalls.incrementAndGet(), 1, executor);
                }
            } catch (final Throwable e) {
                coordinatorFailure.set(e);
            }
        }, "NTest-cancellation-race-coordinator");

        try {
            synchronized (iterator) {
                coordinator.start();
                assertTrue(awaitWorkerState(workerRef, Thread.State.BLOCKED, 5, TimeUnit.SECONDS),
                        "Worker must pass the cancellation check and contend for the iterator monitor");

                coordinator.interrupt();
                coordinator.join(TimeUnit.SECONDS.toMillis(5));
                assertFalse(coordinator.isAlive());
                assertNotNull(findCause(coordinatorFailure.get(), InterruptedException.class));
            }
        } finally {
            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertEquals(0, nextCalls.get(), "A cancelled worker waiting for the iterator monitor must not pull another element");
        assertEquals(0, consumerCalls.get());
    }

    protected static void assertSameExceptionInstanceDoesNotEscapeWorkerAggregation(final boolean indexed) throws Exception {
        final AtomicReference<Throwable> uncaught = new AtomicReference<>();
        final AtomicInteger workerNumber = new AtomicInteger();
        final ExecutorService executor = Executors.newFixedThreadPool(2, command -> {
            final Thread worker = new Thread(command, "NTest-shared-failure-worker-" + workerNumber.incrementAndGet());
            worker.setUncaughtExceptionHandler((thread, failure) -> uncaught.compareAndSet(null, failure));
            return worker;
        });
        final CountDownLatch actionsStarted = new CountDownLatch(2);
        final RuntimeException sharedFailure = new RuntimeException("shared failure");

        try {
            final RuntimeException reported = assertThrows(RuntimeException.class, () -> {
                if (indexed) {
                    N.forEachIndexedInParallel(Arrays.asList(1, 2).iterator(), (index, value) -> {
                        actionsStarted.countDown();
                        assertTrue(actionsStarted.await(5, TimeUnit.SECONDS));
                        throw sharedFailure;
                    }, 2, executor);
                } else {
                    N.forEachInParallel(Arrays.asList(1, 2).iterator(), value -> {
                        actionsStarted.countDown();
                        assertTrue(actionsStarted.await(5, TimeUnit.SECONDS));
                        throw sharedFailure;
                    }, 2, executor);
                }
            });

            assertSame(sharedFailure, reported);
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        assertTrue(uncaught.get() == null, "Error aggregation must not attempt Throwable self-suppression: " + uncaught.get());
    }

    protected static void assertCoordinatorInterruptionStopsAcceptedWorkers(final boolean indexed) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch workersStarted = new CountDownLatch(2);
        final CountDownLatch releaseWorkers = new CountDownLatch(1);
        final AtomicInteger processed = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final AtomicBoolean interruptRestored = new AtomicBoolean();
        final List<Integer> values = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            values.add(i);
        }

        final Thread coordinator = new Thread(() -> {
            try {
                if (indexed) {
                    N.forEachIndexedInParallel(values.iterator(), (index, value) -> {
                        processed.incrementAndGet();
                        workersStarted.countDown();
                        releaseWorkers.await();
                    }, 2, executor);
                } else {
                    N.forEachInParallel(values.iterator(), value -> {
                        processed.incrementAndGet();
                        workersStarted.countDown();
                        releaseWorkers.await();
                    }, 2, executor);
                }
            } catch (final Throwable e) {
                failure.set(e);
            } finally {
                interruptRestored.set(Thread.currentThread().isInterrupted());
            }
        }, "NTest-interrupted-parallel-coordinator");

        try {
            coordinator.start();
            assertTrue(workersStarted.await(5, TimeUnit.SECONDS), "Both workers must enter the consumer");

            coordinator.interrupt();
            coordinator.join(5000);

            assertFalse(coordinator.isAlive(), "Interrupted coordinator must return promptly");
            assertNotNull(failure.get());
            assertTrue(failure.get().getCause() instanceof InterruptedException);
            assertTrue(interruptRestored.get());
        } finally {
            releaseWorkers.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            if (coordinator.isAlive()) {
                coordinator.interrupt();
                coordinator.join(5000);
            }
        }

        assertEquals(2, processed.get(), "Accepted workers must stop after their in-flight actions complete");
    }

    protected static Throwable awaitAggregatedWorkerFailure(final Throwable firstFailure, final Throwable secondFailure, final long timeout,
            final TimeUnit unit) throws InterruptedException {
        final long deadline = System.nanoTime() + unit.toNanos(timeout);

        while (System.nanoTime() - deadline < 0) {
            if (Arrays.asList(firstFailure.getSuppressed()).contains(secondFailure)) {
                return firstFailure;
            } else if (Arrays.asList(secondFailure.getSuppressed()).contains(firstFailure)) {
                return secondFailure;
            }

            Thread.sleep(1);
        }

        return null;
    }

    protected static boolean awaitWorkerState(final AtomicReference<Thread> workerRef, final Thread.State expectedState, final long timeout,
            final TimeUnit unit) {
        final long deadline = System.nanoTime() + unit.toNanos(timeout);

        while (System.nanoTime() - deadline < 0) {
            final Thread worker = workerRef.get();

            if (worker != null && worker.getState() == expectedState) {
                return true;
            }

            Thread.yield();
        }

        return false;
    }

    protected static Throwable findCause(final Throwable failure, final Class<? extends Throwable> causeType) {
        Throwable current = failure;

        while (current != null && !causeType.isInstance(current)) {
            current = current.getCause();
        }

        return current;
    }

    // forEach(Iterable, flatMapper, BiConsumer) - iterable variant

    // forEach(Iterator, flatMapper, BiConsumer) - iterator variant

    // forEach(Object[], flatMapper, flatMapper2, triAction) - array with three levels

    // forEach(Iterable, flatMapper, flatMapper2, triAction)

    // forEach(Iterator, flatMapper, flatMapper2, triAction)

    // forEachNonNull(Iterator, flatMapper, BiConsumer)

    // forEachNonNull(T[], flatMapper, flatMapper2, triAction) - array with null elements

    // forEachNonNull(Iterable, flatMapper, flatMapper2, triAction)

    // forEachNonNull(Iterator, flatMapper, flatMapper2, triAction)

    // forEachNonNull(Object[], flatMapper, BiConsumer) - array variant

    // forEachNonNull(Iterable, flatMapper, BiConsumer) - iterable variant

    // forEachIndexed(Object[], int, int, IntObjConsumer) - range variant

    // forEachPair(Iterator, increment, BiConsumer)

    // runInParallel with 5 tasks - additional edge case with exceptions

    // ---- runInParallel(Collection, Executor) with custom executor ----

    // callInParallel with 4 tasks - additional edge case

    // callInParallel with 5 tasks - additional edge case

    // callInParallel(Collection, Executor)

    // ---- callInParallel(Collection, Executor) with custom executor ----

    // runByBatch(Iterator, int, IntObjConsumer, Runnable)

    //

    //

    // Regression test for Percentage.intValue() truncation bug.
    // Before the fix, (int)(0.35 * 1_000_000) truncated to 349_999 instead of
    // 350_000, so step-based range queries silently dropped values like _35 and _45.

    // -------------- bug-fix regression tests --------------

    // -------------- coverage tests for sleep/sum/average behavior --------------

    // ============================================================
    // Coverage-targeted tests appended 2026-05-29 (uncovered overloads/branches)
    // ============================================================

    // --- concat(X[], X[]) one-empty-side clone branches ---

    // --- removeAt(X[], int...) variadic ---

    // --- removeAll(X[], X...) with multiple values ---

    // --- removeAll(Collection, Iterable) HashSet & Iterator branches ---

    // --- insertAll empty-array / empty-elements clone branches ---

    // --- difference / symmetricDifference array clone branches (empty b) ---

    // --- min/max range + iterable + NULL-comparator null short-circuit ---

    // --- median(a, b, c, cmp) branches & median(X[], from, to) ---

    // --- top(long[], from, to, n) ---

    // --- zip(a, b, valueForNoneA, valueForNoneB, fn, Class) ---

    // --- merge(Collection<Iterable>, nextSelector, supplier) ---

    // --- concat(Collection<Iterable>, supplier) with non-Collection iterable ---

    // --- forEach(a, b[, c], action) empty short-circuit guards ---

    // --- callByBatch / runByBatch overloads ---

    // --- excludeAllToSet plain-HashSet-source result branch ---

    // --- replaceAll(List, oldVal, newVal) & updateAll(List, op) ListIterator branch (non-RandomAccess, size>=11) ---

    // --- regression tests for 2026-06-10 deep-review fixes ---

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
