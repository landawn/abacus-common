package com.landawn.abacus.util;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.collect.Sets;
import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.PersonType;
import com.landawn.abacus.entity.PersonsType;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XmlDeserConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.types.JAXBean;
import com.landawn.abacus.types.WeekDay;
import com.landawn.abacus.util.Iterables.SetView;
import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.function.BooleanUnaryOperator;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Tag("2025")
public class NTest extends AbstractParserTest {

    private static final double DELTA = 0.000001d;
    private static final float DELTAf = 0.000001f;
    private static final IntPredicate IS_EVEN_INT = x -> x % 2 == 0;
    private static final IntPredicate IS_ODD_INT = x -> x % 2 != 0;
    private static final Predicate<Integer> IS_EVEN_INTEGER = x -> x % 2 == 0;
    private static final Predicate<String> STRING_NOT_EMPTY = s -> s != null && !s.isEmpty();
    private static final String TEST_JSON = "{\"name\":\"John\",\"age\":30}";
    private static final String TEST_XML = "<person><name>John</name><age>30</age></person>";
    private static final String TEST_JSON_ARRAY = "[1,2,3,4,5]";

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    @TempDir
    File tempDir;

    private boolean[] booleanArray;
    private char[] charArray;
    private byte[] byteArray;
    private short[] shortArray;
    private int[] intArray;
    private long[] longArray;
    private float[] floatArray;
    private double[] doubleArray;
    private String[] stringArray;
    private Integer[] integerArray;
    private List<String> stringList;
    private List<Integer> integerList;
    private Set<String> stringSet;
    private Map<String, Integer> stringIntMap;

    private boolean[] emptyBooleanArray = new boolean[0];
    private char[] emptyCharArray = new char[0];
    private int[] emptyIntArray = new int[0];
    private String[] emptyStringArray = new String[0];

    private boolean[] singleBooleanArray = new boolean[] { true };
    private int[] singleIntArray = new int[] { 42 };
    private String[] singleStringArray = new String[] { "single" };

    private int[] duplicateIntArray = new int[] { 1, 1, 1, 1, 1 };
    private String[] duplicateStringArray = new String[] { "dup", "dup", "dup" };

    private Integer[] nullContainingArray = new Integer[] { 1, null, 3, null, 5 };
    private List<String> nullContainingList = Arrays.asList("a", null, "c", null, "e");

    private int[] largeIntArray;
    private List<Integer> largeIntList;

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

    void execute() {
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

    private static Collection<String> splitToChars(String s) {
        List<String> result = new ArrayList<>(s.length());

        for (char ch : s.toCharArray()) {
            result.add(String.valueOf(ch));
        }

        return result;
    }

    @SafeVarargs
    private static <T> List<T> toMutableList(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    private static final boolean[] EMPTY_BOOLEAN_ARRAY_CONST = {};
    private static final char[] EMPTY_CHAR_ARRAY_CONST = {};
    private static final byte[] EMPTY_BYTE_ARRAY_CONST = {};
    private static final short[] EMPTY_SHORT_ARRAY_CONST = {};
    private static final int[] EMPTY_INT_ARRAY_CONST = {};
    private static final long[] EMPTY_LONG_ARRAY_CONST = {};
    private static final float[] EMPTY_FLOAT_ARRAY_CONST = {};
    private static final double[] EMPTY_DOUBLE_ARRAY_CONST = {};
    private static final Object[] EMPTY_OBJECT_ARRAY_CONST = {};

    private <T> List<T> iteratorToList(Iterator<T> iterator) {
        List<T> list = new ArrayList<>();

        if (iterator != null) {
            iterator.forEachRemaining(list::add);
        }

        return list;
    }

    private boolean contains(char[] array, char value) {
        for (char c : array) {
            if (c == value) {
                return true;
            }
        }

        return false;
    }

    private boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) {
                return true;
            }
        }

        return false;
    }

    void valueOf() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);
        abacusXmlParser.deserialize(abacusXmlParser.serialize(account), Account.class);

    }

    void executeBigBean() {
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
        private List<String> strList;
        private List intList;
        private List<Short> shortList;
        private XMLGregorianCalendar xmlGregorianCalendar;

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
        private byte[] bytes;
        private char[] chars;
        private String[] strings;
        private List typeList;
        private Set typeSet;

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
        private String name;
        private int value;
        private List<String> items;
        private Map<String, Integer> properties;

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

    private TestBean createSampleBean() {
        List<String> items = new ArrayList<>(Arrays.asList("item1", "item2"));
        Map<String, Integer> props = new LinkedHashMap<>();
        props.put("key1", 100);
        props.put("key2", 200);
        return new TestBean("testName", 123, items, props);
    }

    private String getExpectedJsonForSampleBean(boolean pretty) {
        if (pretty) {
            return String.join(IOUtil.LINE_SEPARATOR_UNIX,
                    "{\n" + "    \"name\": \"testName\",\n" + "    \"value\": 123,\n" + "    \"items\": [\"item1\", \"item2\"],\n" + "    \"properties\": {\n"
                            + "        \"key1\": 100,\n" + "        \"key2\": 200\n" + "    }\n" + "}");
        } else {
            return "{\"name\": \"testName\", \"value\": 123, \"items\": [\"item1\", \"item2\"], \"properties\": {\"key1\": 100, \"key2\": 200}}";
        }
    }

    public static class JaxbBean {
        private String string;
        private List<String> list;
        private Map<String, String> map;

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

    @Test
    public void test_misMatch() {

        assertEquals(Double.valueOf(2), CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3)));
        assertEquals(2D, CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3)));

        double d = CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3));
        N.println(d);

        final int len = 1;
        int[] a = Array.range(0, len);
        int[] b = Array.range(0, len);
        assertEquals(-1, Arrays.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, b));
        a[len - 1] = 0;
        b[len - 1] = 1;
        assertEquals(0, Arrays.mismatch(a, b));
        assertEquals(0, CommonUtil.mismatch(a, b));
        a = CommonUtil.EMPTY_INT_ARRAY;
        assertEquals(0, Arrays.mismatch(a, b));
        assertEquals(0, CommonUtil.mismatch(a, b));
        b = CommonUtil.EMPTY_INT_ARRAY;
        assertEquals(-1, Arrays.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, b));
        a = null;
        assertEquals(-1, CommonUtil.mismatch(a, b));
        b = null;
        assertEquals(-1, CommonUtil.mismatch(a, b));
    }

    @Test
    public void test_compare_perf() {
        final int len = 1000;
        final int[] a = Array.range(0, len);
        final int[] b = Array.range(0, len);
        a[len - 1] = 0;
        b[len - 1] = 1;

        assertEquals(-1, CommonUtil.compare(a, b));
        assertEquals(-1, Arrays.compare(a, b));

        Profiler.run(1, 1000, 3, "N.compare(...)", () -> assertEquals(-1, CommonUtil.compare(a, b))).printResult();
        Profiler.run(1, 1000, 3, "Arrays.compare(...)", () -> assertEquals(-1, Arrays.compare(a, b))).printResult();
    }

    @Test
    public void test_splitToCount() {

        final int[] a = Array.range(1, 8);

        {
            List<int[]> result = N.splitByChunkCount(7, 5, true, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1], [2], [3], [4, 5], [6, 7]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(7, 5, false, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1, 2], [3, 4], [5], [6], [7]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(3, 5, true, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1], [2], [3]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(3, 5, false, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1], [2], [3]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(6, 3, true, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1, 2], [3, 4], [5, 6]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(6, 3, false, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1, 2], [3, 4], [5, 6]]", CommonUtil.toString(result));
        }

        {
            Collection<Integer> c = CommonUtil.toLinkedHashSet(1, 2, 3, 4, 5, 6, 7);

            List<List<Integer>> result = N.splitByChunkCount(c, 5, true);
            assertEquals("[[1], [2], [3], [4, 5], [6, 7]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(c, 5, false);
            assertEquals("[[1, 2], [3, 4], [5], [6], [7]]", CommonUtil.toString(result));

            c = CommonUtil.toLinkedHashSet(1, 2, 3);

            result = N.splitByChunkCount(c, 5, true);
            assertEquals("[[1], [2], [3]]", CommonUtil.toString(result));

        }

    }

    @Test
    public void test_PermutationIterator() {
        PermutationIterator.of(CommonUtil.toList(1, 2, null, 3)).forEachRemaining(Fn.println());

        Iterables.powerSet(CommonUtil.toSet(1, 2, null, 3)).forEach(Fn.println());
    }

    @Test
    public void test_stream_persist_json() throws IOException {
        final List<Map<String, Object>> list = new ArrayList<>();
        list.add(CommonUtil.asMap("a", 1));

        final File file = new File("./a.json");

        Stream.of(list).persistToJson(new File("./a.json"));

        final String json = IOUtil.readAllToString(file);

        N.println(json);

        file.delete();
    }

    @Test
    public void test_asyncCall() {

        {

            final List<Callable<Integer>> commands = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                commands.add(() -> {
                    final int ret = Math.abs(CommonUtil.RAND.nextInt(10)) * 200;
                    N.sleepUninterruptibly(ret);

                    N.println(ret);

                    return ret;
                });
            }

            final List<Integer> result = N.asyncCall(commands).toList();

            N.println(result);

            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i + 1) >= result.get(i));
            }
        }

        {

            final List<Callable<Integer>> commands = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                commands.add(() -> {
                    final int ret = Math.abs(CommonUtil.RAND.nextInt(10)) * 200;
                    N.sleepUninterruptibly(ret);

                    if (ret == 1000) {
                        System.out.println("ERROR: **************: " + ret);
                        throw new RuntimeException("ERROR: **************: " + ret);
                    }

                    N.println(ret);

                    return ret;
                });
            }

            final List<Integer> result = N.asyncCall(commands).limit(2).toList();

            N.println(result);

            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i + 1) >= result.get(i));
            }
        }

    }

    @Test
    public void test_indexOf() {
        {
            final double[] a = { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY };
            assertTrue(N.contains(a, Double.POSITIVE_INFINITY));
            assertTrue(N.contains(a, Double.NEGATIVE_INFINITY));
            assertTrue(N.contains(a, Double.NaN));
        }

        {
            final float[] a = { Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY };
            assertTrue(N.contains(a, Float.POSITIVE_INFINITY));
            assertTrue(N.contains(a, Float.NEGATIVE_INFINITY));
            assertTrue(N.contains(a, Float.NaN));
        }
    }

    @Test
    public void test_frequencyMap() {
        final Map<String, Integer> map = N.frequencyMap(CommonUtil.toList("a", "b", "a", "c", "a", "D", "b"));

        N.println(map);
    }

    @Test
    public void test_applyToEach() {

        {
            final String[] a = CommonUtil.asArray("a ", "b", " c");
            N.replaceAll(a, Strings::trim);
            N.println(a);
        }

        {
            final List<String> c = CommonUtil.toList("a ", "b", " c");
            N.replaceAll(c, Strings::trim);
            N.println(c);
        }

        {
            final List<String> c = CommonUtil.toLinkedList("a ", "b", " c");
            N.replaceAll(c, Strings::trim);
            N.println(c);
        }
    }

    @Test
    public void test_isSorted() throws Exception {
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 3, 5)));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 3, 5, 5, 7, 9, 10)));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 3, 5)));
        assertFalse(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 7, 5)));
        assertFalse(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 2, 3, 7, 5)));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 2, 3, 7, 5), 1, 3));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 3, 7, 5), 0, 4));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 3, 7, 5), 2, 4));
        assertTrue(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 3, 7, 5), 3, 4));
        assertFalse(CommonUtil.isSorted(CommonUtil.toLinkedHashSet(1, 2, 3, 7, 5), 2, 5));

        assertFalse(CommonUtil.isSorted(new int[] { 1, 7, 5 }));
        assertFalse(CommonUtil.isSorted(new int[] { 1, 2, 2, 3, 7, 5 }));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 2, 3, 7, 5 }, 1, 3));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 0, 4));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 2, 4));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 3, 4));
        assertFalse(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 2, 5));
    }

    @Test
    public void test_range() throws Exception {
        N.println(Array.range(0, 10));
        N.println(Array.rangeClosed(0, 10));
        N.println(Array.range(0, 10, -1));
        N.println(Array.range(10, 0, -1));
        N.println(Array.rangeClosed(10, 0, -1));
        N.println(Array.range(10, 0, 1));
    }

    @Test
    public void test_frequency() throws Exception {
        assertEquals(1, N.frequency("aaa", "aa"));
        assertEquals(1, N.frequency("ababaab", "aa"));
        assertEquals(4, N.frequency("ababaab", "a"));

    }

    @Test
    public void test_cartesianProduct() throws Exception {
        Sets.cartesianProduct(CommonUtil.toList(CommonUtil.toSet("a", "b"), CommonUtil.toSet("a", "b", "c"))).forEach(Fn.println());

        N.println(Strings.repeat('=', 80));

        Iterables.cartesianProduct(CommonUtil.toList(CommonUtil.toSet("a", "b"), CommonUtil.toSet("a", "b", "c"))).forEach(Fn.println());
    }

    @Test
    public void test_SetView() throws Exception {
        final Set<?> set1 = CommonUtil.toSet("a", "c", "d");
        final Set<?> set2 = CommonUtil.toSet("b", "a", "c");
        N.println(set1);
        N.println(set2);

        N.println(Strings.repeat('=', 80));

        SetView<?> setView = Iterables.union(set1, set2);
        N.println(setView);

        setView = Iterables.union(set2, set1);
        N.println(setView);

        N.println(Strings.repeat('=', 80));

        setView = Iterables.difference(set1, set2);
        N.println(setView);

        setView = Iterables.difference(set2, set1);
        N.println(setView);

        N.println(Strings.repeat('=', 80));

        setView = Iterables.intersection(set1, set2);
        N.println(setView);

        setView = Iterables.intersection(set2, set1);
        N.println(setView);

        N.println(Strings.repeat('=', 80));

        setView = Iterables.symmetricDifference(set1, set2);
        N.println(setView);

        setView = Iterables.symmetricDifference(set2, set1);
        N.println(setView);
    }

    @Test
    public void test_subSet() throws Exception {
        final Set<?> a = CommonUtil.toSet("a", "c");
        final Set<?> b = CommonUtil.toSet("b", "a", "c");

        N.println(N.difference(a, b));
        N.println(SetUtils.difference(a, b));

        N.println(N.difference(b, a));
        N.println(SetUtils.difference(b, a));

        assertTrue(Range.just("a").contains("a"));

        final NavigableSet<String> c = CommonUtil.toNavigableSet("a", "c", "d", "b");

        assertEquals(CommonUtil.toNavigableSet("a"), Iterables.subSet(c, Range.just("a")));

        assertEquals(CommonUtil.toNavigableSet(), Iterables.subSet(c, Range.open("a", "a")));
        assertEquals(CommonUtil.toNavigableSet("a"), Iterables.subSet(c, Range.closed("a", "a")));
        assertEquals(CommonUtil.toNavigableSet(), Iterables.subSet(c, Range.openClosed("a", "a")));
        assertEquals(CommonUtil.toNavigableSet(), Iterables.subSet(c, Range.closedOpen("a", "a")));
        assertEquals(CommonUtil.toNavigableSet("a"), Iterables.subSet(c, Range.closedOpen("a", "b")));
        assertEquals(CommonUtil.toNavigableSet("a", "b"), Iterables.subSet(c, Range.closed("a", "b")));
        assertEquals(CommonUtil.toNavigableSet("a", "b"), Iterables.subSet(c, Range.closedOpen("a", "c")));

        N.println(Strings.repeat("=", 80));

        N.println(N.commonSet(a, b));
        N.println(Strings.repeat("=", 80));

    }

    @Test
    public void test_isSubCollection() throws Exception {
        Collection<?> a = CommonUtil.toList("a", "b", "c");
        final Collection<?> b = CommonUtil.toSet("b", "a", "c");

        assertTrue(N.isEqualCollection(a, b));
        assertTrue(N.isSubCollection(a, b));
        assertTrue(N.isSubCollection(b, a));
        assertFalse(N.isProperSubCollection(b, a));

        a = CommonUtil.toList("a", "b", "c", "a");
        assertFalse(N.isProperSubCollection(a, b));
        assertTrue(N.isProperSubCollection(b, a));
    }

    @Test
    public void test_clone_01() {
        Beans.deepCopy(u.Optional.of("a"));
        Beans.deepCopy(u.Nullable.of("a"));
    }

    @Test
    public void test_removeDuplicates() {
        List<String> c = CommonUtil.toList("a", "a");
        N.removeDuplicates(c);
        assertEquals(1, c.size());

        c = CommonUtil.toLinkedList("a", "a");
        N.removeDuplicates(c);
        assertEquals(1, c.size());
    }

    @Test
    public void test_firstNonEmpty() {
        final Optional<List<String>> result = CommonUtil.firstNonEmpty(CommonUtil.toList(), CommonUtil.toList("a"), CommonUtil.toList());
        N.println(result);
    }

    @Test
    public void test_replaceRange() {
        {
            final int[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            N.println(N.replaceRange(a, 1, 5, Array.of(3, 4, 5)));

        }

        N.println(Strings.repeat("=", 80));

        {
            final String str = "123456789";
            N.println(N.replaceRange(str, 7, 9, "00000"));

        }
    }

    @Test
    public void test_moveRanger() {
        {
            for (int i = 0; i < 7; i++) {
                final byte[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
                N.moveRange(a, 3, 6, i);
                N.println(a);
            }
        }

        N.println(Strings.repeat("=", 80));

        {
            final String str = "123456789";
            for (int i = 0; i < 7; i++) {
                N.println(i + ": " + N.moveRange(str, 3, 6, i));
            }
        }

        N.println(Strings.repeat("=", 80));

        {
            final String str = "123456789";
            for (int i = 0; i < 8; i++) {
                N.println(i + ": " + N.moveRange(str, 1, 3, i));
            }
        }
    }

    @Test
    public void test_repeat() {
        Array.repeat((String) null, 10);
    }

    @Test
    public void test_toString_2() {
        N.println(CommonUtil.toString((int[]) null));
        N.println(CommonUtil.toString((int[][]) null));
        N.println(CommonUtil.toString((int[][]) null));
        N.println(CommonUtil.toString(new int[0]));
        N.println(CommonUtil.toString(new int[0][]));
        N.println(CommonUtil.toString(new int[0][][]));
        N.println(CommonUtil.toString(new int[1]));
        N.println(CommonUtil.toString(new int[1][]));
        N.println(CommonUtil.toString(new int[1][][]));

    }

    @Test
    public void test_as() {
        List<String> list = CommonUtil.toList("a");
        N.println(list);

        list = CommonUtil.toList("a", "b");
        N.println(list);

        list = CommonUtil.toList(Array.of("a", "b", "c"));
        N.println(list);
    }

    @Test
    public void test_concat_01() {
        final String[] abc = N.concat(CommonUtil.asArray("a", "b"), CommonUtil.asArray("c"));
        N.println(abc);
        final List<String> ab123 = N.concat(CommonUtil.toList("a", "b"), CommonUtil.toList("1", "2", "3"));
        N.println(ab123);
        final List<String> ab = N.concat(CommonUtil.toList("a", "b"));
        N.println(ab);
        final List<String> abcd = N.concat(CommonUtil.toList(CommonUtil.toList("a", "b", "c", "d")));
        N.println(abcd);
    }

    @Test
    public void test_json_nullable() {
        final Map<String, Nullable<Integer>> map = CommonUtil.asMap("a", Nullable.of(12));
        final String json = N.toJson(map);
        N.println(json);
        final Map<String, Nullable<Integer>> map2 = N.fromJson(json, CommonUtil.<Map<String, Nullable<Integer>>> typeOf("Map<String, Nullable<Integer>>"));
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_json_optional() {
        final Map<String, Optional<Integer>> map = CommonUtil.asMap("a", Optional.of(12));
        final String json = N.toJson(map);
        N.println(json);
        final Map<String, Optional<Integer>> map2 = N.fromJson(json, CommonUtil.<Map<String, Optional<Integer>>> typeOf("Map<String, Optional<Integer>>"));
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_json_OptionalDouble() {
        final Map<String, OptionalDouble> map = CommonUtil.asMap("a", OptionalDouble.of(12));
        final String json = N.toJson(map);
        N.println(json);
        final Map<String, OptionalDouble> map2 = N.fromJson(json, CommonUtil.<Map<String, OptionalDouble>> typeOf("Map<String, OptionalDouble>"));
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_delete() {
        final int[] a = { 1, 2, 3, 4, 5 };
        final int[] b = N.removeAt(a, 0, 0, 1, 3);
        assertTrue(CommonUtil.equals(Array.of(3, 5), b));
    }

    @Test
    public void test_0003() {
        String str = "-a-";

        assertEquals("a", StrUtil.substringBetween(str, str.indexOf("-"), "-").orElse(null));
        assertEquals("a", StrUtil.substringBetween(str, "-", str.lastIndexOf("-")).orElse(null));

        str = "--";

        assertEquals("", StrUtil.substringBetween(str, str.indexOf("-"), "-").orElse(null));
        assertEquals("", StrUtil.substringBetween(str, "-", str.lastIndexOf("-")).orElse(null));
    }

    @Test
    public void test_002() {
        try {
            Try.call((Callable<Object>) () -> {
                throw new Exception();
            });
        } catch (final RuntimeException e) {

        }

        try {
            Try.run((Throwables.Runnable) () -> {
                throw new Exception();
            });
        } catch (final RuntimeException e) {

        }
    }

    @Test
    public void test_001() {
        final Account account = Beans.newRandom(Account.class);
        N.println(account);
    }

    @Test
    public void test_difference() {
        {
            final int[] a = { 0, 1, 2, 2, 3 };
            final int[] b = { 2, 5, 1 };
            final int[] c = N.removeAll(a, b);
            assertTrue(CommonUtil.equals(c, Array.of(0, 3)));
        }

        {
            final int[] a = { 0, 1, 2, 2, 3 };
            final int[] b = { 2, 5, 1 };
            int[] c = N.difference(a, b);
            assertTrue(CommonUtil.equals(c, Array.of(0, 2, 3)));

            c = N.intersection(a, b);
            assertTrue(CommonUtil.equals(c, Array.of(1, 2)));
        }

        {
            IntList a = IntList.of(0, 1, 2, 2, 3);
            IntList b = IntList.of(2, 5, 1);
            a.removeAll(b);
            assertTrue(CommonUtil.equals(a, IntList.of(0, 3)));

            a = IntList.of(0, 1, 2, 2, 3);
            b = IntList.of(2, 5, 1);
            a.retainAll(b);
            assertTrue(CommonUtil.equals(a, IntList.of(1, 2, 2)));
        }

        {
            final IntList a = IntList.of(0, 1, 2, 2, 3);
            final IntList b = IntList.of(2, 5, 1);
            IntList c = a.difference(b);
            assertTrue(CommonUtil.equals(c, IntList.of(0, 2, 3)));

            c = a.intersection(b);
            assertTrue(CommonUtil.equals(c, IntList.of(1, 2)));
        }

        {
            final IntList a = IntList.of(0, 1, 2, 2, 3);
            final IntList b = IntList.of(2, 5, 1);
            final IntList c = a.symmetricDifference(b);
            assertTrue(CommonUtil.equals(c, IntList.of(0, 2, 3, 5)));
        }
    }

    @Test
    public void test_copyOfRange() {
        {
            final int[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final int[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1, 3, 5)));
        }

        {
            final long[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final long[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1L, 3, 5)));
        }

        {
            final float[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final float[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1F, 3, 5)));
        }

        {
            final double[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final double[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1D, 3, 5)));
        }
    }

    @Test
    public void test_split() {
        {
            final int[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<int[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1, 2], [3, 4], [5]]", CommonUtil.stringOf(list));
        }

        {
            final long[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<long[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1, 2], [3, 4], [5]]", CommonUtil.stringOf(list));
        }

        {
            final float[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<float[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1.0, 2.0], [3.0, 4.0], [5.0]]", CommonUtil.stringOf(list));
        }

        {
            final double[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<double[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1.0, 2.0], [3.0, 4.0], [5.0]]", CommonUtil.stringOf(list));
        }
    }

    @Test
    public void test_pair() {
        final Pair<Integer, String> pair = Pair.of(1, "abc");
        N.println(pair);
        final Triple<Integer, Character, String> triple = Triple.of(1, 'c', "abc");
        N.println(triple);
    }

    //
    @Test
    public void test_copy_2() {
        final List<Integer> list1 = CommonUtil.toList(1, 2, 3);
        List<Integer> list2 = CommonUtil.toList(4, 5, 6);
        Iterables.copyInto(list1, list2);
        assertEquals(list1, list2);

        list2 = CommonUtil.toList(4, 5, 6);
        Iterables.copyRange(list1, 1, list2, 2, 1);
        assertEquals(CommonUtil.toList(4, 5, 2), list2);
    }

    @Test
    public void test_clone() {
        final int[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] b = a.clone();
        CommonUtil.reverse(b[0]);
        assertTrue(CommonUtil.equals(a[0], b[0]));

        b = CommonUtil.clone(a);
        CommonUtil.reverse(b[0]);
        assertFalse(CommonUtil.equals(a[0], b[0]));

        final String[] c = null;
        N.println(CommonUtil.clone(c));

        final String[][] d = null;
        N.println(CommonUtil.clone(d));

        final String[][][] e = null;
        N.println(CommonUtil.clone(e));

    }

    @Test
    public void test_kthLargest() {
        {
            int[] a = { 1 };
            CommonUtil.shuffle(a);
            assertEquals(1, N.kthLargest(a, 1));

            a = Array.of(1, 2);
            CommonUtil.shuffle(a);
            assertEquals(2, N.kthLargest(a, 1));
            assertEquals(1, N.kthLargest(a, 2));

            a = Array.of(1, 2, 3);
            CommonUtil.shuffle(a);
            assertEquals(3, N.kthLargest(a, 1));
            assertEquals(2, N.kthLargest(a, 2));
            assertEquals(1, N.kthLargest(a, 3));
        }
    }

    @Test
    public void test_lambda() {
        int[] a = Array.repeat(3, 10);
        N.println(a);

        CommonUtil.fill(a, 0);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        CommonUtil.reverse(a);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        CommonUtil.rotate(a, 2);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        CommonUtil.shuffle(a);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        N.println(N.sum(a));
        N.println(N.average(a));
        N.println(N.min(a));
        N.println(N.max(a));
        N.println(N.median(a));

    }

    @Test
    public void test_nCopies() {
        N.println(Array.repeat(true, 10));
        N.println(Array.repeat(false, 10));
        N.println(Array.repeat('1', 10));
        N.println(Array.repeat((byte) 1, 10));
        N.println(Array.repeat((short) 1, 10));
        N.println(Array.repeat(1, 10));
        N.println(Array.repeat(1L, 10));
        N.println(Array.repeat(1f, 10));
        N.println(Array.repeat(1d, 10));
        N.println(Array.repeat("1", 10));
    }

    @Test
    public void test_isNumber() {
        N.println(1.2345354);
        N.println(Double.valueOf("2e10"));
        N.println(Numbers.toDouble("2e10"));
        N.println(Numbers.createDouble("2e10"));
        N.println(Numbers.toFloat("2e3"));
        N.println(Numbers.createFloat("2e3"));
        N.println(Numbers.createNumber("2e10"));
    }

    @Test
    public void test_distinct() {
        final Account[] a = { createAccount(Account.class), createAccount(Account.class), createAccount(Account.class) };
        final List<Account> c = CommonUtil.toList(a);

        final List<Account> m = N.distinctBy(a, (Function<Account, String>) Account::getFirstName);

        N.println(m);

        final List<Account> m2 = N.distinctBy(c, (Function<Account, String>) Account::getFirstName);

        N.println(m2);
    }

    @Test
    public void test_asyncExecute() throws InterruptedException, ExecutionException {
        {
            final List<Throwables.Runnable<RuntimeException>> runnableList = CommonUtil
                    .asList((Throwables.Runnable<RuntimeException>) () -> N.println("Runnable"));

            N.asyncExecute(runnableList).get(0).get();
        }

        {
            final List<Throwables.Runnable<RuntimeException>> runnableList = CommonUtil.toList();
            runnableList.add(() -> N.println("Runnable"));

            N.asyncExecute(runnableList).get(0).get();
        }

        {
            final List<Callable<Void>> callableList = CommonUtil.toList((Callable<Void>) () -> {
                N.println("Callable");
                return null;
            });

            N.asyncExecute(callableList).get(0).get();
        }

        {
            final List<Callable<Void>> callableList = CommonUtil.toList();
            callableList.add(() -> {
                N.println("Callable");
                return null;
            });

            N.asyncExecute(callableList).get(0).get();
        }
    }

    @Test
    public void test_occurrences() {
        {
            final boolean[] a = { true, false, true };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final char[] a = { '1', '2', '1' };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final byte[] a = { 1, 2, 1 };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final short[] a = { 1, 2, 1 };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final int[] a = { 1, 2, 1 };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final long[] a = { 1, 2, 1 };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final float[] a = { 1, 2, 1 };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final double[] a = { 1, 2, 1 };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final String[] a = { "1", "2", "1" };
            assertEquals(2, N.frequency(a, a[0]));
            assertEquals(1, N.frequency(a, a[1]));
        }

        {
            final List<String> list = CommonUtil.toList("1", "2", "1");
            assertEquals(2, N.frequency(list, list.get(0)));
            assertEquals(1, N.frequency(list, list.get(1)));
        }
    }

    @Test
    public void test_rotate() {
        {
            final byte[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final byte[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final short[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final short[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final int[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final int[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final long[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final long[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final float[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final float[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final double[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final double[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final String[] a = { "1", "2", "3", "4", "5", "6" };
            CommonUtil.rotate(a, 2);
            final String[] expected = { "5", "6", "1", "2", "3", "4" };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final List<String> a = CommonUtil.toList("1", "2", "3", "4", "5", "6");
            CommonUtil.rotate(a, 2);
            final List<String> expected = CommonUtil.toList("5", "6", "1", "2", "3", "4");

            assertTrue(CommonUtil.equals(a, expected));
        }
    }

    @Test
    public void test_asImmutableMap() {
        final Map<String, String> m = ImmutableMap.of("123", "abc", "234", "ijk");
        N.println(m);

        for (final Map.Entry<String, String> entry : m.entrySet()) {
            N.println(entry.getKey() + ": " + entry.getValue());
        }

    }

    @Test
    public void test_asResultSet() {
        List<?> list = createAccountList(Account.class, 99);
        Dataset rs = CommonUtil.newDataset(list);
        rs.println();

        list = createAccountList(com.landawn.abacus.entity.pjo.basic.Account.class, 99);
        rs = CommonUtil.newDataset(list);
        rs.println();

        final List<?> list2 = createAccountPropsList(79);

        rs = CommonUtil.newDataset(list2);
        rs.println();

        list.addAll((List) list2);
        rs = CommonUtil.newDataset(list);
        rs.println();

        assertEquals(178, rs.size());
    }

    @Test
    public void test_asResultSet_2() {
        final List<Account> beanList = createAccountList(Account.class, 13);
        final Dataset rs1 = CommonUtil.newDataset(beanList);
        rs1.println();

        final List<Map<String, Object>> mapLsit = new ArrayList<>(beanList.size());
        for (final Account account : beanList) {
            mapLsit.add(Beans.beanToMap(account));
        }

        Dataset rs2 = CommonUtil.newDataset(rs1.columnNames(), mapLsit);
        rs2.println();
        assertEquals(rs1, rs2);

        rs2 = CommonUtil.newDataset(rs1.toList(Map.class));
        rs2.println();

        final Dataset rs3 = CommonUtil.newDataset(rs1.columnNames(), rs1.toList(Object[].class));
        rs3.println();

        final Dataset rs4 = CommonUtil.newDataset(rs1.columnNames(), rs1.toList(List.class));
        rs4.println();

    }

    @Test
    public void test_fprintln() {
        Object[] array = null;
        N.println(array);

        array = new Object[0];
        N.println(array);

        array = CommonUtil.asArray("123", "abc", "234", "ijk");
        N.println(array);

        List<?> list = null;
        N.println(list);

        list = new ArrayList<>();
        N.println(list);

        list = CommonUtil.toList("123", "abc", "234", "ijk");
        N.println(list);

        Map<?, ?> map = null;
        N.println(map);

        map = new HashMap<>();
        N.println(map);

        map = CommonUtil.asMap("123", "abc", "234", "ijk");
        N.println(map);
    }

    @Test
    public void test_stringOf() {

        assertEquals(Strings.EMPTY, "abc".substring(1, 1));

        {
            final Multiset<String> multiSet = CommonUtil.toMultiset("1", "2", "3", "2", "3", "3");
            N.println(multiSet);

            final String str = CommonUtil.stringOf(multiSet);

            final Multiset<String> multiSet2 = CommonUtil.valueOf(str, Multiset.class);

            N.println(multiSet2);

            final Multiset<Integer> multiSet3 = (Multiset<Integer>) CommonUtil.typeOf("Multiset<Integer>").valueOf(str);
            N.println(multiSet3);

            final Multiset<Object> multiSet4 = (Multiset<Object>) CommonUtil.typeOf("Multiset<Object>").valueOf(str);
            N.println(multiSet4);
        }

    }

    @Test
    public void test_lastIndexOf() {
        final String str = "aaa";
        N.println(str.lastIndexOf("a"));
        N.println(str.lastIndexOf("a", str.length()));
        N.println(str.lastIndexOf("a", str.length() - 1));
        N.println(str.lastIndexOf("a", str.length() - 2));
    }

    @Test
    public void test_wrap() {
        N.println(Array.box(1, 2, 3));
    }

    @Test
    public void test_getEnumMap() {
        final List<UnifiedStatus> statusList = CommonUtil.enumListOf(UnifiedStatus.class);
        N.println(statusList);

        final Set<UnifiedStatus> statusSet = CommonUtil.enumSetOf(UnifiedStatus.class);
        N.println(statusSet);

        final Map<UnifiedStatus, String> statusMap = CommonUtil.enumMapOf(UnifiedStatus.class);
        N.println(statusMap);
    }

    @Test
    public void test_parallel_sort() {
    }

    @Test
    public void test_sort_big_int_array() {
    }

    @Test
    public void test_sort_big_int_array_by_parallel_sort() {
    }

    @Test
    public void test_sort_big_long_array() {
    }

    @Test
    public void test_sort_big_long_array_by_parallel_sort() {
    }

    @Test
    public void test_sort_big_double_array() {
    }

    @Test
    public void test_sort_big_double_array_by_parallel_sort() {
    }

    @Test
    public void test_sort_big_string_array() {
    }

    @Test
    public void test_sort_big_string_array_by_parallel_sort() {
    }

    @Test
    public void test_stringSplit() {
        final String str = "123, daskf32rfasdf, sf3qijfdlsafj23i9jqfl;sdjfawdfs, akfj3iq2pflsefj32ijrlqawdfjiq2, i3q2jfals;dfj32i9qjfsdk, askdaf";

        Profiler.run(1, 1000, 1, () -> Splitter.withDefault().splitToArray(str)).printResult();
    }

    @Test
    public void test_getPackage() {
        N.println(ClassUtil.getClassName(int.class));
        N.println(ClassUtil.getSimpleClassName(int.class));
        N.println(ClassUtil.getCanonicalClassName(int.class));

        N.println(ClassUtil.getPackage(int.class));
        N.println(ClassUtil.getPackage(Integer.class));

        N.println(ClassUtil.getPackageName(int.class));
        N.println(ClassUtil.getPackageName(Integer.class));
    }

    @Test
    public void test_filter() {
        {
            final boolean[] a = { true, false, true };
            final boolean[] b = N.filter(a, (BooleanPredicate) value -> value);

            assertTrue(CommonUtil.equals(Array.of(true, true), b));
        }

        {
            final char[] a = { '1', '2', '3' };
            final char[] b = N.filter(a, (CharPredicate) value -> value > '1');

            assertTrue(CommonUtil.equals(Array.of('2', '3'), b));
        }

        {
            final byte[] a = { 1, 2, 3 };
            final byte[] b = N.filter(a, (BytePredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((byte) 2, (byte) 3), b));
        }

        {
            final short[] a = { 1, 2, 3 };
            final short[] b = N.filter(a, (ShortPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((short) 2, (short) 3), b));
        }

        {
            final int[] a = { 1, 2, 3 };
            final int[] b = N.filter(a, (IntPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of(2, 3), b));
        }

        {
            final long[] a = { 1, 2, 3 };
            final long[] b = N.filter(a, (LongPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((long) 2, (long) 3), b));
        }

        {
            final float[] a = { 1, 2, 3 };
            final float[] b = N.filter(a, (FloatPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((float) 2, (float) 3), b));
        }

        {
            final double[] a = { 1, 2, 3 };
            final double[] b = N.filter(a, (DoublePredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((double) 2, (double) 3), b));
        }

    }

    @Test
    public void test_count() {

        {
            assertEquals(N.count((String[]) null, Fn.isNull()), N.count((String[]) null, Fn.notNull()));
        }

        {
            final boolean[] a = { true, false, true };
            final int count = N.count(a, (BooleanPredicate) value -> value);

            assertEquals(2, count);
        }

        {
            final char[] a = { '1', '2', '3' };
            final int count = N.count(a, (CharPredicate) value -> value > '1');

            assertEquals(2, count);
        }

        {
            final byte[] a = { 1, 2, 3 };
            final int count = N.count(a, (BytePredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final short[] a = { 1, 2, 3 };
            final int count = N.count(a, (ShortPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final int[] a = { 1, 2, 3 };
            final int count = N.count(a, (IntPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final long[] a = { 1, 2, 3 };
            final int count = N.count(a, (LongPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final float[] a = { 1, 2, 3 };
            final int count = N.count(a, (FloatPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final double[] a = { 1, 2, 3 };
            final int count = N.count(a, (DoublePredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final List<String> list = CommonUtil.toList("a", "b", "c");

            final int count = N.count(list, (Predicate<String>) value -> value.equals("a") || value.equals("b"));

            assertEquals(2, count);
        }

        {
            final String[] array = { "a", "b", "c" };

            final int count = N.count(array, (Predicate<String>) value -> value.equals("a") || value.equals("b"));

            assertEquals(2, count);
        }

        {
            final Map<String, Integer> m = CommonUtil.asMap("a", 1, "b", 2, "c", 3);

            final int count = N.count(m.entrySet(), (Predicate<Entry<String, Integer>>) entry -> entry.getKey().equals("a") || entry.getKey().equals("b"));

            assertEquals(2, count);
        }
    }

    @Test
    public void test_split_2() {

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.toList("a", "b", "c"), N.split(str, 1)));
        }

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.toList("ab", "c"), N.split(str, 2)));
        }

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.toList("abc"), N.split(str, 3)));
        }

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.toList("abc"), N.split(str, 4)));
        }

        {
            final boolean[] a = { true, false, true };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of(true, false), Array.of(true)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(
                    CommonUtil.deepEquals(CommonUtil.toList(Array.of(true), Array.of(false), Array.of(true)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final char[] a = { '1', '2', '3' };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of('1', '2'), Array.of('3')).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of('1'), Array.of('2'), Array.of('3')).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final byte[] a = { 1, 2, 3 };
            assertTrue(
                    CommonUtil.deepEquals(CommonUtil.toList(Array.of((byte) 1, (byte) 2), Array.of((byte) 3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((byte) 1), Array.of((byte) 2), Array.of((byte) 3)).toArray(new Object[0]),
                    N.split(a, 1).toArray()));
        }

        {
            final short[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((short) 1, (short) 2), Array.of((short) 3)).toArray(new Object[0]),
                    N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((short) 1), Array.of((short) 2), Array.of((short) 3)).toArray(new Object[0]),
                    N.split(a, 1).toArray()));
        }

        {
            final int[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of(1, 2), Array.of(3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of(1), Array.of(2), Array.of(3)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final long[] a = { 1, 2, 3 };
            assertTrue(
                    CommonUtil.deepEquals(CommonUtil.toList(Array.of((long) 1, (long) 2), Array.of((long) 3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((long) 1), Array.of((long) 2), Array.of((long) 3)).toArray(new Object[0]),
                    N.split(a, 1).toArray()));
        }

        {
            final float[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((float) 1, (float) 2), Array.of((float) 3)).toArray(new Object[0]),
                    N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((float) 1), Array.of((float) 2), Array.of((float) 3)).toArray(new Object[0]),
                    N.split(a, 1).toArray()));
        }

        {
            final double[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((double) 1, (double) 2), Array.of((double) 3)).toArray(new Object[0]),
                    N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(Array.of((double) 1), Array.of((double) 2), Array.of((double) 3)).toArray(new Object[0]),
                    N.split(a, 1).toArray()));
        }

        {
            final String[] a = { "1", "2", "3" };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.asArray("1", "2"), CommonUtil.asArray("3")).toArray(new Object[0]),
                    N.split(a, 2).toArray()));
            assertTrue(
                    CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.asArray("1"), CommonUtil.asArray("2"), CommonUtil.asArray("3")).toArray(new Object[0]),
                            N.split(a, 1).toArray()));
        }

        {
            final String[] a = { "1", "2", "3" };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.asArray("1", "2"), CommonUtil.asArray("3")).toArray(new Object[0]),
                    N.split(a, 2).toArray()));
            assertTrue(
                    CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.asArray("1"), CommonUtil.asArray("2"), CommonUtil.asArray("3")).toArray(new Object[0]),
                            N.split(a, 1).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList((Object) CommonUtil.asArray("1", "2", "3")).toArray(new Object[0]), N.split(a, 3).toArray()));
        }

        {
            final String[] a = { "1", "2", "3" };
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.asArray("1", "2"), CommonUtil.asArray("3")).toArray(new Object[0]),
                    N.split(a, 2).toArray()));
            assertTrue(
                    CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.asArray("1"), CommonUtil.asArray("2"), CommonUtil.asArray("3")).toArray(new Object[0]),
                            N.split(a, 1).toArray()));
            final Object tmp = CommonUtil.asArray("1", "2", "3");
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(tmp).toArray(), N.split(a, 3).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(tmp).toArray(), N.split(a, 4).toArray()));
        }

        {
            final List<String> a = CommonUtil.toList("1", "2", "3");
            final List<List<String>> b = N.split(a, 2);
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.toList("1", "2"), CommonUtil.toList("3")).toArray(new Object[0]), b.toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.toList(CommonUtil.toList("1"), CommonUtil.toList("2"), CommonUtil.toList("3")).toArray(new Object[0]),
                    N.split(a, 1).toArray()));
        }

    }

    @Test
    public void test_isNumeric() {
        assertFalse(Strings.isAsciiNumber(null));
        assertFalse(Strings.isAsciiNumber(""));
        assertFalse(Strings.isAsciiNumber("  "));
        assertTrue(Strings.isAsciiNumber("123"));
        assertTrue(Strings.isAsciiNumber("-123"));
        assertTrue(Strings.isAsciiNumber("12.3"));
        assertTrue(Strings.isAsciiNumber("-12.3"));
        assertFalse(Strings.isAsciiNumber("12..3"));
        assertFalse(Strings.isAsciiNumber("12ae"));
        assertFalse(Strings.isAsciiNumber("123l"));
        assertFalse(Strings.isAsciiNumber("123f"));
    }

    @Test
    public void test_isBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank("  "));
        assertTrue(Strings.isBlank(" \n "));
        assertTrue(Strings.isBlank(" \r "));
        assertTrue(Strings.isBlank(" \r \n "));
        assertFalse(Strings.isBlank("12..3"));
        assertFalse(Strings.isBlank("12ae"));
        assertFalse(Strings.isBlank("123l"));
        assertFalse(Strings.isBlank("123f"));
    }

    @Test
    public void test_uuidPerformance() {
        final long startTime = System.currentTimeMillis();
        int k = 0;
        for (int i = 0; i < 1000000; i++) {
            Strings.uuid();
            k++;
        }

        N.println(k + " took: " + (System.currentTimeMillis() - startTime));
    }

    @Test
    public void test_propNameMethod() {
        final Account account = createAccount(Account.class);

        N.println(Beans.getPropValue(account, "firstName"));
        N.println(Beans.getPropValue(account, "firstname"));
        N.println(Beans.getPropValue(account, "FirstName"));
        N.println(Beans.getPropValue(account, "FIRSTNAME"));

        Beans.setPropValue(account, "lastName", "lastName1");
        N.println(Beans.getPropValue(account, "LASTNAME"));

        Beans.setPropValue(account, "lastname", "lastName2");
        N.println(Beans.getPropValue(account, "lastname"));

        Beans.setPropValue(account, "LastName", "lastName3");
        N.println(Beans.getPropValue(account, "LastName"));

        Beans.setPropValue(account, "LASTNAME", "lastName4");
        N.println(Beans.getPropValue(account, "LASTNAME"));
        N.println(Beans.getPropValue(account, "lastName"));
    }

    @Test
    public void test_checkNullOrEmpty() {
        List<String> list = CommonUtil.toList("a");
        list = CommonUtil.checkArgNotEmpty(list, "list");
        N.println(list);

        Set<String> set = CommonUtil.toSet("a");
        set = CommonUtil.checkArgNotEmpty(set, "set");
        N.println(set);

        Queue<String> queue = CommonUtil.toQueue("a");
        queue = CommonUtil.checkArgNotEmpty(queue, "queue");
        N.println(queue);
    }

    @Test
    public void test_array2List() {
        final boolean[] boa = { true, false, false, true, false };
        final List<Boolean> bol = CommonUtil.toList(boa);
        assertEquals(false, bol.get(2).booleanValue());

        final char[] ca = { '3', '2', '1', '4', '5' };
        final List<Character> cl = CommonUtil.toList(ca);
        assertEquals('1', cl.get(2).charValue());

        final byte[] ba = { 3, 2, 1, 4, 5 };
        final List<Byte> bl = CommonUtil.toList(ba);
        assertEquals(1, bl.get(2).intValue());

        final short[] sa = { 3, 2, 1, 4, 5 };
        final List<Short> sl = CommonUtil.toList(sa);
        assertEquals(1, sl.get(2).intValue());

        final int[] ia = { 3, 2, 1, 4, 5 };
        final List<Integer> il = CommonUtil.toList(ia);
        assertEquals(1, il.get(2).intValue());

        final long[] la = { 3, 2, 1, 4, 5 };
        final List<Long> ll = CommonUtil.toList(la);
        assertEquals(1, ll.get(2).intValue());

        final float[] fa = { 3, 2, 1, 4, 5 };
        final List<Float> fl = CommonUtil.toList(fa);
        assertEquals(1, fl.get(2).intValue());

        final double[] da = { 3, 2, 1, 4, 5 };
        final List<Double> dl = CommonUtil.toList(da);
        assertEquals(1, dl.get(2).intValue());

        final String[] stra = { "3", "2", "1", "4", "5" };
        final List<String> strl = CommonUtil.toList(stra);
        assertEquals("1", strl.get(2));
    }

    @Test
    public void test_max() {
        final char[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('5', ((Character) N.max(ca)).charValue());

        final byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(ba)).intValue());

        final short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(sa)).intValue());

        final int[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(ia)).intValue());

        final long[] la = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(la)).intValue());

        final float[] fa = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(fa)).intValue());

        final double[] da = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(da)).intValue());

        {
            final Iterable<Integer> c = CommonUtil.toList(3, 2, 1, 4, 5);
            assertEquals(5, N.max(c).intValue());
        }
        {
            final Iterable<Integer> c = CommonUtil.toList(3, 2, 1, 4, 5);
            assertEquals(1, N.min(c).intValue());
        }
    }

    @Test
    public void test_max_2() {
        final Character[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('5', N.max(ca).charValue());

        final Byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(5, N.max(ba).intValue());

        final Short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(5, N.max(sa).intValue());

        final Integer[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(5, N.max(ia).intValue());

        final Long[] la = { 3L, 2L, 1L, 4L, 5L };
        assertEquals(5, N.max(la).intValue());

        final Float[] fa = { 3f, 2f, 1f, 4f, 5f };
        assertEquals(5, N.max(fa).intValue());

        final Double[] da = { 3d, 2d, 1d, 4d, 5d };
        assertEquals(5, N.max(da).intValue());
    }

    @Test
    public void test_min() {
        final char[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('1', ((Character) N.min(ca)).charValue());

        final byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(ba)).intValue());

        final short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(sa)).intValue());

        final int[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(ia)).intValue());

        final long[] la = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(la)).intValue());

        final float[] fa = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(fa)).intValue());

        final double[] da = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(da)).intValue());
    }

    @Test
    public void test_min_2() {
        final Character[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('1', N.min(ca).charValue());

        final Byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(1, N.min(ba).intValue());

        final Short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(1, N.min(sa).intValue());

        final Integer[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(1, N.min(ia).intValue());

        final Long[] la = { 3L, 2L, 1L, 4L, 5L };
        assertEquals(1, N.min(la).intValue());

        final Float[] fa = { 3f, 2f, 1f, 4f, 5f };
        assertEquals(1, N.min(fa).intValue());

        final Double[] da = { 3d, 2d, 1d, 4d, 5d };
        assertEquals(1, N.min(da).intValue());
    }

    @Test
    public void test_array_performance() {
    }

    @Test
    public void test_arrayOf() {
        N.println(Array.of(false, true));
        N.println(Array.of('a', 'b'));
        N.println(Array.of((byte) 1, (byte) 2));
        N.println(Array.of((short) 1, (short) 2));
        N.println(Array.of(1, 2));
        N.println(Array.of(1L, 2L));
        N.println(Array.of(1f, 2f));
        N.println(Array.of(1d, 2d));
        N.println(CommonUtil.asArray(Dates.currentJUDate(), Dates.currentDate()));
        N.println(CommonUtil.asArray(Dates.currentCalendar(), Dates.currentCalendar()));

        final String a1 = "a";
        final String b1 = "b";
        final List<String> list = CommonUtil.toList(a1, b1);
        N.println(list);

        final List<Integer> list2 = CommonUtil.toList(1, 2, 3);
        N.println(list2);

        final int[] a = Array.of(1, 2, 3);
        N.println(a);

        final Class<?>[] classes = CommonUtil.asArray(String.class, Integer.class);
        N.println(classes);

        final Type<Object>[] types = CommonUtil.asArray(CommonUtil.typeOf(int.class), CommonUtil.typeOf(long.class));
        N.println(types);

        final Date[] dates = CommonUtil.asArray(Dates.currentDate(), Dates.currentDate());
        N.println(dates);

        final java.util.Date[] dateTimes = CommonUtil.asArray(Dates.currentDate(), Dates.currentTime());
        N.println(dateTimes);

        final UnifiedStatus[] status = CommonUtil.asArray(UnifiedStatus.ACTIVE, UnifiedStatus.CANCELED);
        N.println(status);

        N.println(ClassUtil.getCanonicalClassName(int.class));
    }

    @Test
    public void test_compare() {
        final int result = CommonUtil.compare("a", "bc", (Comparator<String>) String::compareTo);

        assertEquals(-1, result);
    }

    @Test
    public void test_requireNonNull() {
    }

    @Test
    public void test_Collections() {
        N.println(CommonUtil.asSingletonSet("abc"));
        N.println(CommonUtil.asSingletonList("abc"));
        N.println(CommonUtil.asSingletonMap("key", "value"));

        final List<String> list = CommonUtil.toList("a", "b", "c", "d");
        N.println(list);
        CommonUtil.reverse(list);
        N.println(list);
        N.replaceAll(list, "a", "newValue");
        N.println(list);
    }

    @Test
    public void test_format() {

        N.println(Dates.format(Dates.currentJUDate(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentJUDate(), Dates.LOCAL_DATE_TIME_FORMAT));
        N.println(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT));
        N.println(Dates.format(Dates.currentTime(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentTime(), Dates.LOCAL_DATE_TIME_FORMAT));
        N.println(Dates.format(Dates.currentTimestamp(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentTimestamp(), Dates.LOCAL_DATE_TIME_FORMAT));

        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate())));
        com.landawn.abacus.util.BufferedWriter writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate())));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        Dates.formatTo(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE, writer);
        N.println(Dates.parseTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
        Objectory.recycle(writer);

        writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar())));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));

        Dates.formatTo(Dates.currentCalendar(), null, null, writer);
        Dates.formatTo(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE, writer);
        N.println(Dates.parseTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
        Objectory.recycle(writer);

        writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar())));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        Dates.formatTo(Dates.currentXMLGregorianCalendar(), null, null, writer);
        Dates.formatTo(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE, writer);
        N.println(Dates.parseTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
        Objectory.recycle(writer);
    }

    @Test
    public void test_isPrimaryType() {

        assertTrue(Strings.isAsciiNumber("123"));
        assertTrue(Strings.isAsciiNumber("-123"));
        assertTrue(Strings.isAsciiNumber("90239.0329"));
        assertFalse(Strings.isAsciiNumber("90239.0329f"));
    }

    @Test
    public void test_encode_decode_2() {
        final String str = "ůůůůů";
        N.println(Strings.base64UrlEncode(str.getBytes()));

        N.println(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(str.getBytes()));

        final String string = "This string encoded will be longer that 76 characters and cause MIME base64 line folding";

        System.out.println("commons-codec JDK8Base64.encodeBase64\n" + Strings.base64Encode(string.getBytes()));

        assertEquals(string, Strings.base64DecodeToString(Strings.base64Encode(string.getBytes())));
    }

    @Test
    public void test_toString() {
        assertEquals("true", CommonUtil.toString(true));
        assertEquals("1", CommonUtil.toString('1'));
        assertEquals("1", CommonUtil.toString((byte) 1));
        assertEquals("1", CommonUtil.toString((short) 1));
        assertEquals("1", CommonUtil.toString(1));
        assertEquals("1", CommonUtil.toString(1L));
        assertEquals("1.0", CommonUtil.toString(1f));
        assertEquals("1.0", CommonUtil.toString(1d));
        assertEquals("[a, b]", CommonUtil.toString(new String[] { "a", "b" }));
        assertEquals("[a, b]", CommonUtil.deepToString(new String[] { "a", "b" }));

        assertEquals("[[false, true], [a, b], [1, 2], [1, 2], [1, 2], [1, 2], [1.0, 2.0], [1.0, 2.0], [a, bc]]",
                CommonUtil.deepToString(new Object[] { new boolean[] { false, true }, new char[] { 'a', 'b' }, new byte[] { 1, 2 }, new short[] { 1, 2 },
                        new int[] { 1, 2 }, new long[] { 1, 2 }, new float[] { 1, 2 }, new double[] { 1, 2 }, new String[] { "a", "bc" } }));

        final Object obj = new String[] { "a", "b" };
        assertEquals("[a, b]", CommonUtil.toString(obj));
        assertEquals("[a, b]", CommonUtil.deepToString(obj));

        assertEquals(Strings.NULL, CommonUtil.deepToString((Object) null));

        N.println(CommonUtil.deepToString(Dates.currentDate()));
    }

    @Test
    public void test_convert() {
        {
            CommonUtil.convert(12L, byte.class);
            CommonUtil.convert(12L, Byte.class);
            CommonUtil.convert(12L, short.class);
            CommonUtil.convert(12L, Short.class);
            CommonUtil.convert(12L, int.class);
            CommonUtil.convert(12L, Integer.class);
            CommonUtil.convert(12L, long.class);
            CommonUtil.convert(12L, Long.class);
            CommonUtil.convert(12L, float.class);
            CommonUtil.convert(12L, Float.class);
            CommonUtil.convert(12L, double.class);
            CommonUtil.convert(12L, Double.class);
        }

        {
            CommonUtil.convert(12f, byte.class);
            CommonUtil.convert(12f, Byte.class);
            CommonUtil.convert(12f, short.class);
            CommonUtil.convert(12f, Short.class);
            CommonUtil.convert(12f, int.class);
            CommonUtil.convert(12f, Integer.class);
            CommonUtil.convert(12f, long.class);
            CommonUtil.convert(12f, Long.class);
            CommonUtil.convert(12f, float.class);
            CommonUtil.convert(12f, Float.class);
            CommonUtil.convert(12f, double.class);
            CommonUtil.convert(12f, Double.class);
        }

        {
            CommonUtil.convert(12d, byte.class);
            CommonUtil.convert(12d, Byte.class);
            CommonUtil.convert(12d, short.class);
            CommonUtil.convert(12d, Short.class);
            CommonUtil.convert(12d, int.class);
            CommonUtil.convert(12d, Integer.class);
            CommonUtil.convert(12d, long.class);
            CommonUtil.convert(12d, Long.class);
            CommonUtil.convert(12d, float.class);
            CommonUtil.convert(12d, Float.class);
            CommonUtil.convert(12d, double.class);
            CommonUtil.convert(12d, Double.class);
        }

        {
            CommonUtil.convert((short) 12, byte.class);
            CommonUtil.convert((short) 12, Byte.class);
            CommonUtil.convert((short) 12, short.class);
            CommonUtil.convert((short) 12, Short.class);
            CommonUtil.convert((short) 12, int.class);
            CommonUtil.convert((short) 12, Integer.class);
            CommonUtil.convert((short) 12, long.class);
            CommonUtil.convert((short) 12, Long.class);
            CommonUtil.convert((short) 12, float.class);
            CommonUtil.convert((short) 12, Float.class);
            CommonUtil.convert((short) 12, double.class);
            CommonUtil.convert((short) 12, Double.class);
        }

        {
            CommonUtil.convert((byte) 12, byte.class);
            CommonUtil.convert((byte) 12, Byte.class);
            CommonUtil.convert((byte) 12, short.class);
            CommonUtil.convert((byte) 12, Short.class);
            CommonUtil.convert((byte) 12, int.class);
            CommonUtil.convert((byte) 12, Integer.class);
            CommonUtil.convert((byte) 12, long.class);
            CommonUtil.convert((byte) 12, Long.class);
            CommonUtil.convert((byte) 12, float.class);
            CommonUtil.convert((byte) 12, Float.class);
            CommonUtil.convert((byte) 12, double.class);
            CommonUtil.convert((byte) 12, Double.class);
        }

        {
            try {
                CommonUtil.convert(Integer.MAX_VALUE, byte.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Integer.MAX_VALUE, short.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Long.MIN_VALUE, int.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Float.MAX_VALUE, long.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(-Float.MAX_VALUE, long.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(-Double.MAX_VALUE, float.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Double.MAX_VALUE, float.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }
    }

    //     @Test
    //     public void test_asAndNew() {
    //         final List<String> linkedList = new LinkedList<>();
    //         linkedList.add("abc");
    //         N.println(linkedList);
    //
    //         N.println(CommonUtil.asProps("firstName", "1)1"));
    //
    //         N.println(new LinkedHashMap<>());
    //         N.println(new LinkedHashMap<>(10));
    //         N.println(new IdentityHashMap<>());
    //         N.println(new IdentityHashMap<>(10));
    //
    //         N.println(new ArrayDeque<>());
    //         N.println(new ArrayDeque<>(10));
    //         N.println(new TreeMap<>());
    //         N.println(new Multiset<>());
    //         N.println(new TreeSet<>());
    //         N.println(new ConcurrentLinkedQueue<>());
    //         N.println(CommonUtil.newListMultimap());
    //         N.println(CommonUtil.newLinkedListMultimap());
    //         N.println(CommonUtil.newSetMultimap());
    //         N.println(CommonUtil.newLinkedSetMultimap());
    //         N.println(CommonUtil.newLinkedSetMultimap());
    //         N.println(CommonUtil.newLinkedSetMultimap());
    //         N.println(CommonUtil.newLinkedSetMultimap());
    //         N.println(CommonUtil.newLinkedSetMultimap());
    //
    //         N.println(new MapEntity(AccountPNL.__, CommonUtil.asProps("firstName", "1)1")));
    //
    //         N.println(new MapEntity(AccountPNL.__));
    //
    //         N.println(Seid.of(AccountPNL.ID, 123));
    //         N.println(Seid.create(CommonUtil.asProps(AccountPNL.ID, 123)));
    //
    //         N.println(new LinkedHashMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
    //         N.println(new ConcurrentHashMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
    //         N.println(new IdentityHashMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
    //         N.println(new TreeMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
    //         N.println(new TreeMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
    //
    //         N.println(CommonUtil.toLinkedList("ab", "c"));
    //         N.println(new LinkedList<>(CommonUtil.toList("ab", "c")));
    //
    //         N.println(CommonUtil.toLinkedHashSet("ab", "c"));
    //
    //         N.println(CommonUtil.toSortedSet());
    //         N.println(CommonUtil.toSortedSet("ab", "c"));
    //         N.println(new TreeSet<>(CommonUtil.toList("ab", "c")));
    //         N.println(new TreeSet<>(CommonUtil.toSortedSet("ab", "c")));
    //
    //         N.println(CommonUtil.toQueue("ab", "c"));
    //         N.println(new ArrayDeque<>());
    //         N.println(new ArrayDeque<>(3));
    //         N.println(new ArrayDeque<>(CommonUtil.toList("ab", "c")));
    //
    //         N.println(CommonUtil.toArrayBlockingQueue("ab", "c"));
    //
    //         N.println(CommonUtil.toLinkedBlockingQueue("ab", "c"));
    //
    //         N.println(CommonUtil.toPriorityQueue("ab", "c"));
    //         N.println(CommonUtil.toConcurrentLinkedQueue("ab", "c"));
    //
    //         final Delayed d = new Delayed() {
    //             @Override
    //             public int compareTo(final Delayed o) {
    //                 return 0;
    //             }
    //
    //             @Override
    //             public long getDelay(final TimeUnit unit) {
    //                 return 0;
    //             }
    //         };
    //
    //         N.println(CommonUtil.toDelayQueue(d));
    //
    //         N.println(CommonUtil.toDeque("ab", "c"));
    //         N.println(new ArrayDeque<>(CommonUtil.toList("ab", "c")));
    //
    //         N.println(CommonUtil.toDeque("ab", "c"));
    //         N.println(new ArrayDeque<>(CommonUtil.toList("ab", "c")));
    //
    //         N.println(CommonUtil.toLinkedBlockingDeque("ab", "c"));
    //
    //         N.println(CommonUtil.toConcurrentLinkedDeque("ab", "c"));
    //
    //         assertEquals(true, Strings.parseBoolean("True"));
    //         assertEquals(1, Numbers.toByte("1"));
    //         assertEquals(1, Numbers.toShort("1"));
    //         assertEquals(1, Numbers.toInt("1"));
    //         assertEquals(0, Numbers.toInt(""));
    //         assertEquals(1, Numbers.toLong("1"));
    //         assertEquals(0, Numbers.toLong(""));
    //         assertEquals(1f, Numbers.toFloat("1"));
    //         assertEquals(1f, Numbers.toFloat("1f"));
    //         assertEquals(1f, Numbers.toFloat("1F"));
    //         assertEquals(0f, Numbers.toFloat(""));
    //         assertEquals(1d, Numbers.toDouble("1"));
    //         assertEquals(0d, Numbers.toDouble(""));
    //
    //         assertEquals(1, (int) CommonUtil.convert(1L, int.class));
    //
    //         N.println(Dates.createDate(Dates.currentJUDate()));
    //         N.println(Dates.createTime(Dates.currentJUDate()));
    //         N.println(Dates.createTimestamp(Dates.currentJUDate()));
    //
    //         N.println(Dates.createJUDate(Dates.currentCalendar()));
    //         N.println(Dates.createJUDate(Dates.currentJUDate()));
    //         N.println(Dates.createJUDate(System.currentTimeMillis()));
    //
    //         N.println(Dates.createCalendar(Dates.currentCalendar()));
    //         N.println(Dates.createCalendar(Dates.currentJUDate()));
    //         N.println(Dates.createCalendar(System.currentTimeMillis()));
    //
    //         N.println(Dates.createGregorianCalendar(Dates.currentCalendar()));
    //         N.println(Dates.createGregorianCalendar(Dates.currentJUDate()));
    //         N.println(Dates.createGregorianCalendar(System.currentTimeMillis()));
    //
    //         N.println(Dates.createXMLGregorianCalendar(Dates.currentCalendar()));
    //         N.println(Dates.createXMLGregorianCalendar(Dates.currentJUDate()));
    //         N.println(Dates.createXMLGregorianCalendar(System.currentTimeMillis()));
    //
    //         N.println(Dates.parseCalendar(Dates.format(Dates.currentJUDate())));
    //         N.println(Dates.parseGregorianCalendar(Dates.format(Dates.currentJUDate())));
    //         N.println(Dates.parseXMLGregorianCalendar(Dates.format(Dates.currentJUDate())));
    //     }

    //

    @Test
    public void test_hashCode() {
        N.println(CommonUtil.hashCode(false));
        N.println(CommonUtil.hashCode(true));
        N.println(CommonUtil.hashCode('a'));
        N.println(CommonUtil.hashCode((byte) 1));
        N.println(CommonUtil.hashCode((short) 1));
        N.println(CommonUtil.hashCode(1));
        N.println(CommonUtil.hashCode(1L));
        N.println(CommonUtil.hashCode(1f));
        N.println(CommonUtil.hashCode(1d));

        N.println(CommonUtil.hashCode(new boolean[] { true, false }));

        N.println(CommonUtil.hashCode(new char[] { 'a', 'b' }));

        N.println(CommonUtil.hashCode(new byte[] { (byte) 1, (byte) 1 }));

        N.println(CommonUtil.hashCode(new short[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new int[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new long[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new float[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new double[] { 1, 1 }));

        final String[][] a = { { "a", "b", "c" }, { "1", "2", "3" } };
        N.println(CommonUtil.hashCode(a));
        N.println(CommonUtil.deepHashCode(a));

        final Object b = new String[][] { { "a", "b", "c" }, { "1", "2", "3" } };

        N.println(CommonUtil.hashCode(b));
        N.println(CommonUtil.deepHashCode(b));

        assertEquals(CommonUtil.deepHashCode(a), CommonUtil.deepHashCode(b));

        assertEquals(CommonUtil.deepHashCode("abc"), CommonUtil.hashCode("abc"));
        assertEquals(0, CommonUtil.hashCode((Object) null));
        assertEquals(0, CommonUtil.deepHashCode((Object) null));
    }

    @Test
    public void test_equals() {
        assertFalse(CommonUtil.equals(true, false));

        assertFalse(CommonUtil.equals('a', 'b'));

        assertFalse(CommonUtil.equals((byte) 1, (byte) 2));

        assertFalse(CommonUtil.equals((short) 1, (short) 2));

        assertFalse(CommonUtil.equals(1, 2));

        assertFalse(CommonUtil.equals(1f, 2f));

        assertTrue(CommonUtil.equals(1f, 1));

        assertFalse(CommonUtil.equals(1d, 2d));

        assertTrue(CommonUtil.equals(1d, 1));

        assertFalse(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { false, true }));

        assertFalse(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'b' }));

        assertFalse(CommonUtil.equals(new byte[] { (byte) 1, (byte) 1 }, new byte[] { (byte) 1, (byte) 2 }));

        assertFalse(CommonUtil.equals(new short[] { 1, 1 }, new short[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new int[] { 1, 1 }, new int[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new long[] { 1, 1 }, new long[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new float[] { 1, 1 }, new float[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new double[] { 1, 1 }, new double[] { 1, 2 }));

        final String[][] a = { { "a", "b", "c" }, { "1", "2", "3" } };
        final String[][] b = { { "a", "b", "c" }, { "1", "2", "3" } };

        assertTrue(CommonUtil.equals(a, b));
        assertTrue(CommonUtil.deepEquals(a, b));

        final Object a1 = new String[][] { { "a", "b", "c" }, { "1", "2", "3" } };
        final Object b1 = new String[][] { { "a", "b", "c" }, { "1", "2", "3" } };

        assertTrue(CommonUtil.equals(a1, b1));
        assertTrue(CommonUtil.deepEquals(a1, b1));

        assertTrue(CommonUtil.deepEquals("a", "a"));

        assertFalse(CommonUtil.deepEquals("a", "b"));
    }

    @Test
    public void test_xmlToJson() {
        final Account account = new Account();
        account.setFirstName("firstName1");
        account.setLastName("lastName1");

        final String xml = abacusXmlParser.serialize(account);
        final String json = jsonParser.serialize(account);

        final String xml2 = N.jsonToXml(json, Account.class);
        N.println(xml2);

        final String json2 = N.xmlToJson(xml, Account.class);
        N.println(json2);
    }

    @Test
    public void test_xml2JSON_1() {
        final Account account = new Account();
        account.setFirstName("firstName1");
        account.setLastName("lastName1");

        final String xml = abacusXMLDOMParser.serialize(account);
        final String json = jsonParser.serialize(account);

        final String xml2 = N.jsonToXml(json, Account.class);
        N.println(xml2);

        final String json2 = N.xmlToJson(xml, Account.class);
        N.println(json2);

        final String xml3 = N.jsonToXml(json);
        N.println(xml3);

        final String json3 = N.xmlToJson(xml);
        N.println(json3);
    }

    @Test
    public void test_beanToMap_2() {
        final Map<String, Object> props = new HashMap<>();
        props.put("Account.firstName", "firstName1");
        props.put("Account.lastName", "lastName1");

        final Account account = Beans.mapToBean(props, Account.class);
        assertEquals("firstName1", account.getFirstName());
        assertEquals("lastName1", account.getLastName());
    }

    @Test
    public void test_copy() {
        final Account account1 = new Account();
        account1.setFirstName("firstName1");
        account1.setLastName("lastName1");

        final Account account2 = Beans.copy(account1);
        N.println(account2);
        assertEquals("firstName1", account2.getFirstName());

        final PersonType personType1 = new PersonType();
        personType1.setBirthday(Dates.currentDate());
        personType1.setFirstName("firstName");

        final PersonType personType2 = Beans.copy(personType1);
        N.println(personType2);
        assertEquals("firstName", personType2.getFirstName());

        final PersonsType personsType1 = new PersonsType();
        personsType1.getPerson().add(personType1);

        final PersonsType personsType2 = Beans.copy(personsType1);
        assertEquals("firstName", personsType2.getPerson().get(0).getFirstName());

        final JAXBean jaxBean1 = new JAXBean();
        jaxBean1.getCityList().add("a");
        jaxBean1.getCityList().add("b");

        final JAXBean jaxBean2 = Beans.copy(jaxBean1);
        assertEquals("b", jaxBean2.getCityList().get(1));
    }

    @Test
    public void test_erase() {
        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");

        N.println(account);
        Beans.clearAllProps(account);
        N.println(account);

        account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Beans.clearProps(account, "firstName");
        N.println(account);
        assertNull(account.getFirstName());

        account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Beans.clearProps(account, CommonUtil.toList("firstName", "lastName"));
        assertNull(account.getFirstName());
        assertNull(account.getLastName());

        final PersonType personType = new PersonType();
        personType.setBirthday(Dates.currentDate());
        Beans.clearAllProps(personType);
        assertNull(personType.getBirthday());
    }

    @Test
    public void test_merge() {
        final Account account1 = new Account();
        account1.setFirstName("firstName1");
        account1.setLastName("lastName1");

        final Account account2 = new Account();
        account2.setId(2);
        account2.setFirstName("firstName2");

        Beans.copyInto(account2, account1);
        N.println(account1);
        assertEquals("firstName2", account1.getFirstName());

        final PersonType personType1 = new PersonType();
        personType1.setBirthday(Dates.currentDate());

        final PersonType personType2 = new PersonType();
        personType2.setId(2);
        personType2.setFirstName("firstName");

        Beans.copyInto(personType2, personType1);
        N.println(personType1);
        assertEquals("firstName", personType1.getFirstName());

        final PersonsType personsType1 = new PersonsType();
        personsType1.getPerson().add(personType1);

        final PersonsType personsType2 = new PersonsType();
        personsType2.getPerson().add(personType2);
        Beans.copyInto(personsType2, personsType1);
        N.println(personsType1);

        assertEquals(1, personsType1.getPerson().size());

        final JAXBean jaxBean1 = new JAXBean();
        jaxBean1.getCityList().add("a");

        final JAXBean jaxBean2 = new JAXBean();
        jaxBean2.getCityList().add("b");

        Beans.copyInto(jaxBean1, jaxBean2);

        assertEquals("a", jaxBean2.getCityList().get(0));
    }

    @Test
    public void test_getPropField() {
        Field field = Beans.getPropField(Account.class, "firstName");
        N.println(field);

        field = Beans.getPropField(Account.class, "first_Name");
        N.println(field);

        field = Beans.getPropField(Account.class, "getfirstName");
        N.println(field);
    }

    @Test
    public void test_registerPropertyAccessor() {
        Beans.registerPropertyAccessor("a", ClassUtil.getDeclaredMethod(Account.class, "getFirstName"));

        Method method = Beans.getPropGetter(Account.class, "a");
        N.println(method);
        assertEquals(ClassUtil.getDeclaredMethod(Account.class, "getFirstName"), method);

        Beans.registerPropertyAccessor("b", ClassUtil.getDeclaredMethod(Account.class, "setFirstName", String.class));
        method = Beans.getPropSetter(Account.class, "b");
        N.println(method);
        assertEquals(ClassUtil.getDeclaredMethod(Account.class, "setFirstName", String.class), method);
    }

    @Test
    public void test_invokeConstructor() {
        final Account account = ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(Account.class));
        N.println(account);
    }

    @Test
    public void test_getClassName() {
        N.println(ClassUtil.getSimpleClassName(long.class));
        N.println(ClassUtil.getClassName(long.class));
        N.println(ClassUtil.getCanonicalClassName(long.class));

        N.println(ClassUtil.getSimpleClassName(long[].class));
        N.println(ClassUtil.getClassName(long[].class));
        N.println(ClassUtil.getCanonicalClassName(long[].class));

        N.println(ClassUtil.getSimpleClassName(Long.class));
        N.println(ClassUtil.getClassName(Long.class));
        N.println(ClassUtil.getCanonicalClassName(Long.class));

        N.println(ClassUtil.getSimpleClassName(Long[].class));
        N.println(ClassUtil.getClassName(Long[].class));
        N.println(ClassUtil.getCanonicalClassName(Long[].class));
    }

    @Test
    public void test_min_max() {
        N.println("==================================================");

        final List<String> coll2 = CommonUtil.toList("1", "2", "7", "0", "-1");
        N.println(N.min(coll2));
        N.println(N.max(coll2));

        N.println("==================================================");

        final Set<String> coll3 = CommonUtil.toSet("1", "2", "7", "0", "-1");
        N.println(N.min(coll3));
        N.println(N.max(coll3));

        N.println("==================================================");

        final int[] array = { 1, 2, 7, 0, -1 };
        N.println(N.sum(array));
        N.println(N.average(array));
        N.println(N.min(array));
        N.println(N.max(array));

        N.println("==================================================");

        final int[] array1 = { 1, 2, 7, 0, -1 };
        N.println(N.sum(array1));
        N.println(N.average(array1));
        N.println(N.min(array1));
        N.println(N.max(array1));

        N.println("==================================================");

        final String[] array2 = { "1", "2", "7", "0", "-1" };
        N.println(N.min(array2));
        N.println(N.max(array2));
    }

    @Test
    public void test_dateFormat_performance() {
        Profiler.run(this, "test_dateFormat", 6, 10000, 1).printResult();
    }

    @Test
    public void test_dateFormat() {
        final Timestamp date = Dates.currentTimestamp();
        final String st = Dates.format(date);

        Dates.parseTimestamp(st);
    }

    @Test
    public void test_dateFormat_2() throws ParseException {
        final DateFormat df = new SimpleDateFormat(Dates.ISO_8601_TIMESTAMP_FORMAT);
        final Timestamp date = Dates.currentTimestamp();
        final String st = df.format(date);

        df.parse(st);
    }

    @Test
    public void test_asArray() {
        final String[] a = CommonUtil.asArray("a", "b");
        N.println(a);

        final Object[] b = CommonUtil.asArray("a", 'c');
        N.println(b);

        final char[] c = Array.of('a', 'c');
        N.println(c);
    }

    @Test
    public void test_Bean2Map() {
        final Account account = createAccountWithContact(Account.class);

        Map<String, Object> m = Beans.beanToFlatMap(account);
        N.println(CommonUtil.stringOf(m));

        final XBean xBean = createBigXBean(1);
        m = Beans.beanToMap(xBean);
        N.println(CommonUtil.stringOf(m));

        m = Beans.beanToFlatMap(xBean);
        N.println(CommonUtil.stringOf(m));
    }

    @Test
    public void test_Bean2Map_2() {
        final com.landawn.abacus.entity.pjo.basic.Account account = createAccountWithContact(com.landawn.abacus.entity.pjo.basic.Account.class);

        Map<String, Object> m = Beans.beanToMap(account);
        N.println(CommonUtil.stringOf(m));

        m = Beans.deepBeanToMap(account);
        N.println(CommonUtil.stringOf(m));

        m = Beans.beanToFlatMap(account);
        N.println(CommonUtil.stringOf(m));
    }

    @Test
    public void testFormat_1() {
        N.println(Dates.parseDate("2014-01-01"));

        String st = Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT);
        N.println(st + " : " + Dates.format(Dates.parseDate(st)));

        st = Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        N.println(st + " : " + Dates.format(Dates.parseDate(st), Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("PST")));
    }

    @Test
    public void testFormat_2() {
        final String st = Dates.format(Dates.currentDate());
        N.println(st + " : " + Dates.format(Dates.parseDate(st)));

        for (int i = 0; i < 1000000; i++) {
            Dates.parseDate(st);
        }
    }

    @Test
    public void testFormat_3() {
        final String st = Dates.format(Dates.currentDate(), Dates.ISO_8601_TIMESTAMP_FORMAT);
        N.println(st + " : " + Dates.format(Dates.parseDate(st), Dates.ISO_8601_TIMESTAMP_FORMAT));

        for (int i = 0; i < 1000000; i++) {
            Dates.parseDate(st);
        }
    }

    @Test
    public void test_jaxb_1() throws Exception {
        Beans.registerXmlBindingClass(JaxbBean.class);

        final JaxbBean jb = new JaxbBean();
        jb.setString("string1");
        jb.getList().add("list_e_1");
        jb.getMap().put("map_key_1", "map_value_1");
        N.println(abacusXmlParser.serialize(jb));
        N.println(CommonUtil.stringOf(abacusXmlParser.deserialize(abacusXmlParser.serialize(jb), JaxbBean.class)));
    }

    @Test
    public void test_jaxb_2() throws Exception {
        Beans.registerXmlBindingClass(JaxbBean.class);

        final JaxbBean jb = new JaxbBean();
        jb.setString("string1");
        jb.getList().add("list_e_1");
        jb.getMap().put("map_key_1", "map_value_1");
        N.println(abacusXMLDOMParser.serialize(jb));
        N.println(CommonUtil.stringOf(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(jb), JaxbBean.class)));
    }

    @Test
    public void test_string2Array() {
        final byte[] array = Splitter.with(",").trim(true).splitToArray("1,2,3, 4", byte[].class);

        N.println(array);
    }

    @Test
    public void test_performance() {
        Profiler.run(this, "execute", 16, 100, 1).printResult();
    }

    @Test
    public void test_valueOf_1() throws Exception {
        final Bean_1 bean = new Bean_1();
        assertEquals(bean, abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class));

        bean.setStrList(CommonUtil.toList("abc", "123"));
        assertEquals(bean, abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class));

        bean.setShortList(CommonUtil.toList((short) 1, (short) 2, (short) 3));
        assertEquals(bean, abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class));

        bean.setIntList(CommonUtil.toList(1, 2, 3));
        assertFalse(bean.equals(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class)));

        final GregorianCalendar c = new GregorianCalendar();
        bean.setXMLGregorianCalendar(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        N.println(abacusXmlParser.serialize(bean));

        assertFalse(bean.equals(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class)));
    }

    @Test
    public void test_valueOf_2() throws Exception {
        final Bean_1 bean = new Bean_1();
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setStrList(CommonUtil.toList("abc", "123"));
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setShortList(CommonUtil.toList((short) 1, (short) 2, (short) 3));
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setIntList(CommonUtil.toList(1, 2, 3));
        assertFalse(bean.equals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class)));

        final GregorianCalendar c = new GregorianCalendar();
        bean.setXMLGregorianCalendar(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        N.println(abacusXMLDOMParser.serialize(bean));

        assertFalse(bean.equals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class)));
    }

    @Test
    public void testArrayName() {
        N.println(byte[].class.getSimpleName());
        assertTrue("byte[]".equals(byte[].class.getSimpleName()));

        N.println(int[].class.getSimpleName());
        assertTrue("int[]".equals(int[].class.getSimpleName()));
    }

    @Test
    public void testString2Array() {
        N.println(Splitter.withDefault().splitToArray("a, b, c", char[].class));
        N.println(Splitter.withDefault().splitToArray("1, 2, 3", byte[].class));
        N.println(Splitter.withDefault().splitToArray("1, 2, 3", int[].class));
    }

    @Test
    public void testStringOf() {
        N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray(CommonUtil.stringOf(Dates.currentDate()), Date[].class)));
        N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray("a, b, c", char[].class)));
        N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray("1, 2, 3", byte[].class)));
        N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray("1, 2, 3", int[].class)));
    }

    @Test
    public void testValueOf() {
        N.println(abacusXmlParser.deserialize("<array><e>" + CommonUtil.stringOf(Dates.currentDate()) + "</e></array>", Date[].class));
        N.println(abacusXmlParser.deserialize("<array>a, b, c</array>", char[].class));
        N.println(abacusXmlParser.deserialize("<array>1, 2, 3</array>", byte[].class));
        N.println(abacusXmlParser.deserialize("<array>1, 2, 3</array>", int[].class));
    }

    @Test
    public void testValueOf_1() {
        N.println(abacusXMLDOMParser.deserialize("<array><e>" + CommonUtil.stringOf(Dates.currentDate()) + "</e></array>", Date[].class));
        N.println(abacusXMLDOMParser.deserialize("<array>a, b, c</array>", char[].class));
        N.println(abacusXMLDOMParser.deserialize("<array>1, 2, 3</array>", byte[].class));
        N.println(abacusXMLDOMParser.deserialize("<array>1, 2, 3</array>", int[].class));
    }

    @Test
    public void testForClass() throws Exception {
        N.println(int.class.getSimpleName());
        N.println(int.class.getCanonicalName());

        assertEquals(int.class, ClassUtil.forName("int"));
        assertEquals(char.class, ClassUtil.forName("char"));
        assertEquals(boolean.class, ClassUtil.forName("boolean"));
        assertEquals(String.class, ClassUtil.forName("String"));
        assertEquals(String.class, ClassUtil.forName(String.class.getName()));
        assertEquals(Object.class, ClassUtil.forName("Object"));
        assertEquals(java.lang.Math.class, ClassUtil.forName("Math"));
        assertEquals(java.util.Date.class, ClassUtil.forName("java.util.Date"));

        assertEquals(int[].class, ClassUtil.forName("int[]"));
        assertEquals(char[].class, ClassUtil.forName("char[]"));
        assertEquals(boolean[].class, ClassUtil.forName("boolean[]"));
        assertEquals(String[].class, ClassUtil.forName("String[]"));
        assertEquals(String[].class, ClassUtil.forName(String[].class.getName()));
        assertEquals(Object[].class, ClassUtil.forName("Object[]"));
        assertEquals(java.util.Date[].class, ClassUtil.forName("java.util.Date[]"));

        assertEquals(int[][].class, ClassUtil.forName("int[][]"));
        assertEquals(char[][].class, ClassUtil.forName("char[][]"));
        assertEquals(boolean[][].class, ClassUtil.forName("boolean[][]"));
        assertEquals(String[][].class, ClassUtil.forName("String[][]"));
        assertEquals(String[][].class, ClassUtil.forName(String[][].class.getName()));
        assertEquals(Object[][].class, ClassUtil.forName("Object[][]"));
        assertEquals(java.util.Date[][].class, ClassUtil.forName("java.util.Date[][]"));

        N.println(int[][][].class.getName());
        N.println(int.class.getName());

        assertEquals(int[][][].class, ClassUtil.forName("int[][][]"));
        assertEquals(char[][][].class, ClassUtil.forName("char[][][]"));
        assertEquals(boolean[][][].class, ClassUtil.forName("boolean[][][]"));
        assertEquals(String[][][].class, ClassUtil.forName("String[][][]"));
        assertEquals(Object[][][].class, ClassUtil.forName("Object[][][]"));
        assertEquals(java.util.Date[][][].class, ClassUtil.forName("java.util.Date[][][]"));

        try {
            ClassUtil.forName("string");
            fail("should throw RuntimeException");
        } catch (final IllegalArgumentException e) {
        }

        try {
            ClassUtil.forName("object[]");
            fail("should throw RuntimeException");
        } catch (final IllegalArgumentException e) {
        }

        try {
            ClassUtil.forName("int[];[]");
            fail("should throw RuntimeException");
        } catch (final IllegalArgumentException e) {
        }
    }

    @Test
    public void testGetDefaultValueByType() {
        assertEquals(false, (boolean) CommonUtil.defaultValueOf(boolean.class));
        assertEquals(0, (char) CommonUtil.defaultValueOf(char.class));
        assertEquals(0, (byte) CommonUtil.defaultValueOf(byte.class));
        assertEquals(0, (short) CommonUtil.defaultValueOf(short.class));
        assertEquals(0, (int) CommonUtil.defaultValueOf(int.class));
        assertEquals(0L, (long) CommonUtil.defaultValueOf(long.class));
        assertEquals(0f, (float) CommonUtil.defaultValueOf(float.class));
        assertEquals(0d, (double) CommonUtil.defaultValueOf(double.class));
        assertEquals(null, CommonUtil.defaultValueOf(Boolean.class));
        assertEquals(null, CommonUtil.defaultValueOf(String.class));
        assertEquals(null, CommonUtil.defaultValueOf(Object.class));
    }

    @Test
    public void testIsNullorEmpty() {
        assertTrue(CommonUtil.isEmpty(new boolean[0]));
        assertTrue(CommonUtil.isEmpty(new char[0]));
        assertTrue(CommonUtil.isEmpty(new byte[0]));
        assertTrue(CommonUtil.isEmpty(new short[0]));
        assertTrue(CommonUtil.isEmpty(new int[0]));
        assertTrue(CommonUtil.isEmpty(new long[0]));
        assertTrue(CommonUtil.isEmpty(new float[0]));
        assertTrue(CommonUtil.isEmpty(new double[0]));

        assertTrue(CommonUtil.notEmpty(new boolean[1]));
        assertTrue(CommonUtil.notEmpty(new char[1]));
        assertTrue(CommonUtil.notEmpty(new byte[1]));
        assertTrue(CommonUtil.notEmpty(new short[1]));
        assertTrue(CommonUtil.notEmpty(new int[1]));
        assertTrue(CommonUtil.notEmpty(new long[1]));
        assertTrue(CommonUtil.notEmpty(new float[1]));
        assertTrue(CommonUtil.notEmpty(new double[1]));

        assertFalse(CommonUtil.isEmpty(new boolean[1]));
        assertFalse(CommonUtil.isEmpty(new char[1]));
        assertFalse(CommonUtil.isEmpty(new byte[1]));
        assertFalse(CommonUtil.isEmpty(new short[1]));
        assertFalse(CommonUtil.isEmpty(new int[1]));
        assertFalse(CommonUtil.isEmpty(new long[1]));
        assertFalse(CommonUtil.isEmpty(new float[1]));
        assertFalse(CommonUtil.isEmpty(new double[1]));

        assertTrue(CommonUtil.isEmpty(new ArrayList<>()));
        assertTrue(CommonUtil.isEmpty(new HashMap<>()));
        assertTrue(CommonUtil.isEmpty(new ArrayList<>(100)));
        assertTrue(Strings.isEmpty(""));

        final Map<String, Object> m = null;
        assertTrue(CommonUtil.isEmpty(m));
        assertTrue(CommonUtil.isEmpty(new HashMap<>()));

        final String st = null;
        assertTrue(Strings.isEmpty(st));

        final Account[] a = null;
        assertTrue(CommonUtil.isEmpty(a));
        assertTrue(CommonUtil.isEmpty(new String[] {}));

        assertFalse(Strings.isEmpty(" "));
        assertFalse(CommonUtil.isEmpty(new String[] { null }));
    }

    @Test
    public void testGetConstructor() {
        assertNotNull(ClassUtil.getDeclaredConstructor(Account.class));
        assertNull(ClassUtil.getDeclaredConstructor(Account.class, String.class));
        assertNotNull(ClassUtil.getDeclaredConstructor(TypeFactory.class));
    }

    @Test
    public void testGetMethod() {
        assertNotNull(ClassUtil.getDeclaredMethod(Account.class, "setFirstName", String.class));
        assertNotNull(ClassUtil.getDeclaredMethod(Account.class, "setFirstname", String.class));
        assertNull(ClassUtil.getDeclaredMethod(Account.class, "getFirstName", String.class));
        assertNull(ClassUtil.getDeclaredMethod(Account.class, "getFirstName1", String.class));

        assertNotNull(ClassUtil.getDeclaredMethod(CommonUtil.class, "typeOf", Class.class));
    }

    @Test
    public void testInvokeMethod() {
        final Account account = createAccount(Account.class);
        assertEquals(account.getFirstName(), ClassUtil.invokeMethod(account, ClassUtil.getDeclaredMethod(Account.class, "getFirstName")));
        ClassUtil.invokeMethod(account, ClassUtil.getDeclaredMethod(CommonUtil.class, "typeOf", Class.class), N.class);

        N.println(ClassUtil.invokeMethod(this, ClassUtil.getDeclaredMethod(NTest.class, "testGetMethod")));

        assertNull(ClassUtil.invokeMethod(this, ClassUtil.getDeclaredMethod(NTest.class, "testGetMethod")));
    }

    @Test
    public void testGetPropName() {
        assertEquals("gui", Beans.getPropNameByMethod(Beans.getPropGetter(Account.class, "GUI")));
        assertEquals("birthDate", Beans.getPropNameByMethod(Beans.getPropGetter(Account.class, "birthDate")));
    }

    @Test
    public void testGetGetterSetterMethodList() {
        assertEquals(Beans.getPropGetters(Account.class).size(), Beans.getPropSetters(Account.class).size());

        assertEquals(Beans.getPropGetters(XBean.class).size(), Beans.getPropSetters(XBean.class).size());
    }

    @Test
    public void testPropGetSetMethod() {
        final Account account = new Account();
        account.setFirstName("fn");
        account.setMiddleName("mn");
        account.setLastName("ln");

        println(Beans.getPropGetter(Account.class, "firstName"));
        println(Beans.getPropSetter(Account.class, "id"));
    }

    @Test
    public void testPropGetSetValue() {
        final Account account = new Account();

        Method getMethod = Beans.getPropGetter(Account.class, "firstName");
        Method setMethod = Beans.getPropSetter(Account.class, "firstName");
        Beans.setPropValue(account, setMethod, "fn");
        assertEquals("fn", Beans.getPropValue(account, getMethod));
        println(account);

        getMethod = Beans.getPropGetter(Account.class, AccountPNL.ID);
        setMethod = Beans.getPropSetter(Account.class, AccountPNL.ID);
        Beans.setPropValue(account, setMethod, -1);
        Beans.setPropValue(account, setMethod, Integer.valueOf(-2));
        assertEquals(Long.valueOf(-2), Beans.getPropValue(account, getMethod));
        println(account);

        getMethod = Beans.getPropGetter(Account.class, AccountPNL.BIRTH_DATE);
        setMethod = Beans.getPropSetter(Account.class, AccountPNL.BIRTH_DATE);

        final Date date = Dates.currentDate();
        Beans.setPropValue(account, setMethod, date);
        println(account);

        Beans.setPropValue(account, "firstName", "newfn");
        assertEquals("newfn", Beans.getPropValue(account, "firstName"));
        println(account);

        Beans.setPropValue(account, "id", null);

    }

    @Test
    public void testPropGetSetValue_2() {
        final Account account = new Account();
        String firstName = Beans.getPropValue(account, "firstName");
        N.println(firstName);

        firstName = "firstName";
        Beans.setPropValue(account, "firstName", firstName);

        assertEquals(firstName, Beans.getPropValue(account, "firstName"));
        firstName = Beans.getPropValue(account, "firstName");
        N.println(firstName);

        final long contactId = Beans.getPropValue(account, "contact.id");
        N.println(contactId);

        String email = Beans.getPropValue(account, "contact.email");
        N.println(email);

        assertNull(Beans.getPropValue(account, "contact"));

        Beans.setPropValue(account, "contact.email", "myemail@email.com");
        email = Beans.getPropValue(account, "contact.email");
        N.println(email);
    }

    @Test
    public void test_ParserUtil() {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        final Set<BeanInfo> set = CommonUtil.toSet(beanInfo);
        assertTrue(set.contains(beanInfo));
        N.println(beanInfo);

        final PropInfo propInfo = beanInfo.getPropInfo("firstName");
        final Set<PropInfo> set2 = CommonUtil.toSet(propInfo);
        set2.contains(propInfo);

        N.println(propInfo);
    }

    @Test
    public void testPropGetSetValue_by_ParserUtil() {
        final Account account = new Account();
        String firstName = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName");
        N.println(firstName);

        firstName = "firstName";
        ParserUtil.getBeanInfo(Account.class).setPropValue(account, "firstName", firstName);

        assertEquals(firstName, ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName"));
        firstName = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName");
        N.println(firstName);

        final long contactId = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.id");
        N.println(contactId);

        String email = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.email");
        N.println(email);

        assertNull(ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact"));

        Beans.setPropValue(account, "contact.email", "myemail@email.com");
        email = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.email");
        N.println(email);
    }

    @Test
    public void testMerge() {
        final Account account = new Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(100001);

        final Account account2 = new Account();
        account2.setFirstName("fn2");
        account2.setMiddleName("mn2");
        account2.setLastName("ln2");
        account2.setBirthDate(Dates.currentTimestamp());

        Beans.copyInto(account, account2);
        println(account);
        println(account2);

        Beans.clearAllProps(account);
        N.println(account);

    }

    @Test
    public void testMerge2() {
        final Account account = createAccount(Account.class);
        final Account account2 = createAccount(Account.class);

        Beans.copyInto(account, account2);
        println(account);
        println(account2);
    }

    @Test
    public void testCopyBean() {
        final Account account = new Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(1000);

        final Account copy = Beans.copy(account);
        println(copy);
        assertEquals(account, copy);
    }

    @Test
    public void testCopyBean2() {
        final com.landawn.abacus.entity.extendDirty.basic.Account account = new com.landawn.abacus.entity.extendDirty.basic.Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(100001);
        account.setBirthDate(Dates.currentTimestamp());

        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copy(account);
        println(copy);
        assertEquals(account, copy);
    }

    @Test
    public void testCopyBean3() {
        final Account account = new Account();
        account.setId(1000);
        account.setBirthDate(Dates.currentTimestamp());

        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copyAs(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);
        println(account);
        println(copy);
        println(CommonUtil.stringOf(account));
        println(CommonUtil.stringOf(copy));
        assertEquals(account.toString(), copy.toString());
    }

    @Test
    public void testCloneBean() {
        final Account account = new Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(1000);

        final Account copy = Beans.deepCopy(account);
        println(copy);
        assertEquals(account, copy);
    }

    @Test
    public void testCloneBean_2() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeString(
                "‰β,『�?★业€ > \\n sfd \\r ds \\' f d // \\\\  \\\\\\\\ /// /////// \\\\\\\\\\\\\\\\  \\\\\\\\\\\\\\\\n \\\\\\\\\\\\\\\\r  \\t sd \\\" fe stri‰β,『�?★业€ ng黎< > </ <//、\\n");

        final Map<Integer, Object> typeMap = CommonUtil.asMap(1, "2", 1, "2");
        xBean.setTypeMap(typeMap);

        final XBean xBean2 = Beans.deepCopy(xBean);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testCopy_1() {
        final Account account = createAccountWithContact(Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copyAs(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);
        println(copy);
    }

    @Test
    public void testCopy_2() {
        final com.landawn.abacus.entity.pjo.basic.Account account = createAccountWithContact(com.landawn.abacus.entity.pjo.basic.Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copyAs(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);
        println(copy);
    }

    @Test
    public void testMerge_1() {
        final Account account = createAccountWithContact(Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copyAs(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);

        Beans.copyInto(copy, account);
        println(account);
    }

    @Test
    public void testMerge_2() {
        final com.landawn.abacus.entity.pjo.basic.Account account = createAccountWithContact(com.landawn.abacus.entity.pjo.basic.Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copyAs(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);

        Beans.copyInto(copy, account);
        println(account);
    }

    @Test
    public void test_binarySearch_2() {
        final Random rand = new Random();

        for (int i = 0; i < 1000; i++) {
            final int len = i;
            final int[] a = new int[len];

            for (int j = 0; j < len; j++) {
                a[j] = rand.nextInt();
            }

            Arrays.sort(a);

            final List<Integer> listA = new ArrayList<>(CommonUtil.toList(a));
            final List<Integer> listB = new LinkedList<>(CommonUtil.toList(a));
            for (int k = 0; k < len; k++) {
                final int expected = CommonUtil.binarySearch(a, a[k]);
                assertEquals(expected, CommonUtil.binarySearch(listA, listA.get(k)));
                assertEquals(expected, CommonUtil.binarySearch(listB, listA.get(k)));
                assertEquals(a[k], listA.get(expected).intValue());
                assertEquals(a[k], listB.get(expected).intValue());
            }
        }
    }

    @Test
    public void test_binarySearch() {
        boolean[] copy0 = { false, false, true };
        assertEquals(0, CommonUtil.binarySearch(copy0, false));
        assertEquals(2, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true, true, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true, true, true, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, true };
        assertEquals(2, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false, true };
        assertEquals(3, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false, false, true };
        assertEquals(4, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false, false, false, true };
        assertEquals(5, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false };
        assertEquals(0, CommonUtil.binarySearch(copy0, false));
        assertEquals(-4, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { true, true, true };
        assertEquals(-1, CommonUtil.binarySearch(copy0, false));
        assertEquals(0, CommonUtil.binarySearch(copy0, true));

        final char[] copy1 = { '1', '2', '3' };
        assertEquals(1, CommonUtil.binarySearch(copy1, '2'));
        assertEquals(1, CommonUtil.binarySearch(copy1, 1, 2, '2'));

        final byte[] copy2 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy2, (byte) 2));
        assertEquals(1, CommonUtil.binarySearch(copy2, 1, 2, (byte) 2));

        final short[] copy3 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy3, (short) 2));
        assertEquals(1, CommonUtil.binarySearch(copy3, 1, 2, (short) 2));

        final int[] copy4 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy4, 2));
        assertEquals(1, CommonUtil.binarySearch(copy4, 1, 2, 2));

        final long[] copy5 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy5, 2));
        assertEquals(1, CommonUtil.binarySearch(copy5, 1, 2, 2));

        final float[] copy6 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy6, 2));
        assertEquals(1, CommonUtil.binarySearch(copy6, 1, 2, 2));

        final double[] copy7 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy7, 2));
        assertEquals(1, CommonUtil.binarySearch(copy7, 1, 2, 2));

        final Integer[] copy8 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy8, 2));
        assertEquals(1, CommonUtil.binarySearch(copy8, 2, Comparators.naturalOrder()));

        final Integer[] copy9 = { 1, 2, 3 };
        assertEquals(-1, CommonUtil.binarySearch(copy9, 0));
        assertEquals(-1, CommonUtil.binarySearch(copy9, 0, Comparators.naturalOrder()));
        assertEquals(-4, CommonUtil.binarySearch(copy9, 5));
        assertEquals(-4, CommonUtil.binarySearch(copy9, 5, Comparators.naturalOrder()));

        final List<Integer> list = CommonUtil.toList(1, 2, 3);
        assertEquals(1, CommonUtil.binarySearch(list, 2));
        assertEquals(1, CommonUtil.binarySearch(list, 2, Comparators.naturalOrder()));

        {
            final int[] a = {};
            assertEquals(-1, CommonUtil.binarySearch(a, 1));
            assertEquals(-1, Arrays.binarySearch(a, 1));
            assertEquals(Arrays.binarySearch(a, 1), CommonUtil.binarySearch(a, 1));
            assertEquals(-1, CommonUtil.binarySearch((int[]) null, 1));
            assertEquals(-1, CommonUtil.binarySearch((int[]) null, 0));
        }
    }

    @Test
    public void testCopyArray() {
        boolean[] copy = CommonUtil.copyOfRange(new boolean[] { true, true, false }, 0, 2);
        println(CommonUtil.toString(copy));

        char[] copy1 = CommonUtil.copyOfRange(new char[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy1));

        byte[] copy2 = CommonUtil.copyOfRange(new byte[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy2));

        short[] copy3 = CommonUtil.copyOfRange(new short[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy3));

        int[] copy4 = CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy4));

        long[] copy5 = CommonUtil.copyOfRange(new long[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy5));

        float[] copy6 = CommonUtil.copyOfRange(new float[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy6));

        double[] copy7 = CommonUtil.copyOfRange(new double[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy7));

        Object[] copy8 = CommonUtil.copyOfRange(new Object[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy8));

        copy8 = CommonUtil.copyOfRange(new Integer[] { 1, 2, 3 }, 0, 2, Object[].class);
        println(CommonUtil.toString(copy8));

        copy = CommonUtil.copyOf(new boolean[] { true, true, false }, 9);
        println(CommonUtil.toString(copy));

        copy1 = CommonUtil.copyOf(new char[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy1));

        copy2 = CommonUtil.copyOf(new byte[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy2));

        copy3 = CommonUtil.copyOf(new short[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy3));

        copy4 = CommonUtil.copyOf(new int[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy4));

        copy5 = CommonUtil.copyOf(new long[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy5));

        copy6 = CommonUtil.copyOf(new float[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy6));

        copy7 = CommonUtil.copyOf(new double[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy7));

        copy8 = CommonUtil.copyOf(new Object[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy8));

        copy8 = CommonUtil.copyOf(new String[] { "1", "2", "3" }, 9, Object[].class);
        println(CommonUtil.toString(copy8));
    }

    @Test
    public void testCombine() {
        final String[] a = { "a", "b", "c" };
        final String[] b = { "d", "e", "f" };
        final String[] c = { "a", "b", "c", "d", "e", "f" };

        println(CommonUtil.toString(N.concat(b, a)));
        assertTrue(CommonUtil.equals(c, N.concat(a, b)));

        final int[] i1 = { 1, 2, 3 };
        final int[] i2 = { 4, 5, 6 };

        N.concat(i2, i1);
    }

    @Test
    public void testBean2Map() {
        final Account account = createAccount(Account.class);
        println(Beans.beanToMap(account));
    }

    @Test
    public void testTimeUnit() {
        assertEquals(100, TimeUnit.MILLISECONDS.toMillis(100));
        assertEquals(100000, TimeUnit.SECONDS.toMillis(100));
        assertEquals(6000000, TimeUnit.MINUTES.toMillis(100));
        assertEquals(-100, TimeUnit.MILLISECONDS.toMillis(-100));
        assertEquals(-100000, TimeUnit.SECONDS.toMillis(-100));
        assertEquals(-6000000, TimeUnit.MINUTES.toMillis(-100));
    }

    @Test
    public void testAsDate() {
        final Calendar c = Calendar.getInstance();
        println(Dates.roll(Dates.createDate(c), 10, CalendarField.MONTH));

        println(Dates.roll(Dates.createDate(c), 10, TimeUnit.DAYS));
        println(Dates.roll(Dates.createDate(c), -10, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), -10000, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), 10000, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), 1000, CalendarField.WEEK_OF_YEAR));

        println(Dates.roll(Dates.createDate(c), 7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createDate(c), 1000, CalendarField.WEEK_OF_YEAR)), Dates.roll(Dates.createDate(c), 7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createTime(c), -1000, CalendarField.WEEK_OF_YEAR)), Dates.roll(Dates.createTime(c), -7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createTimestamp(c), -1000, CalendarField.WEEK_OF_YEAR)),
                Dates.roll(Dates.createTimestamp(c), -7000, CalendarField.DAY_OF_MONTH));

        println(Dates.createDate(Dates.roll(Dates.currentCalendar(), 10, TimeUnit.DAYS)));
        println(Dates.createDate(Dates.roll(Dates.currentCalendar(), 10, CalendarField.DAY_OF_MONTH)));
    }

    @Test
    public void testAsDateWithString() {
        final Type<Date> type = TypeFactory.getType(Date.class.getCanonicalName());
        final String st = type.stringOf(Dates.currentDate());
        N.println(st);
    }

    @Test
    public void testToString() {
        final Object arraysOfDouble = new double[][] { { 1, 1 }, { 1.2111, 2.111 } };
        println(CommonUtil.toString(arraysOfDouble));
        println(CommonUtil.deepToString(arraysOfDouble));
    }

    @Test
    public void testUUID() {
        N.println(Strings.uuidWithoutHyphens());
        N.println(Strings.uuidWithoutHyphens().length());
        N.println(Strings.uuid());
        N.println(Strings.uuid().length());

        println(UUID.randomUUID().toString());

        final String uuid = Strings.uuid();
        println(uuid);
        println(Strings.base64Encode(uuid.getBytes()));
    }

    @Test
    public void testAsString() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(CommonUtil.stringOf(account));

        final Bean bean = new Bean();
        println(CommonUtil.stringOf(bean));

        bean.setStrings(CommonUtil.asArray("a", "b"));
        println(CommonUtil.stringOf(bean));

        bean.setBytes(new byte[] { 1, 2 });
        println(CommonUtil.stringOf(bean));
    }

    @Test
    public void testAsXml() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(abacusXmlParser.serialize(account));
        println(abacusXmlParser.serialize(account));

        final Map<Class<?>, Set<String>> ignoredPropNames = CommonUtil.asMap(Account.class, CommonUtil.toSet("lastUpdateTime"));
        final XmlSerConfig config = new XmlSerConfig();
        config.setIgnoredPropNames(ignoredPropNames);
        println(abacusXmlParser.serialize(account, config));

        println(abacusXmlParser.deserialize(abacusXmlParser.serialize(account), Account.class));

        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(abacusXmlParser.deserialize(abacusXmlParser.serialize(account), Account.class)));
    }

    @Test
    public void testAsXml_1() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(abacusXMLDOMParser.serialize(account));
        println(abacusXMLDOMParser.serialize(account));

        final Map<Class<?>, Set<String>> ignoredPropNames = CommonUtil.asMap(Account.class, CommonUtil.toSet("lastUpdateTime"));
        final XmlSerConfig config = new XmlSerConfig();
        config.setIgnoredPropNames(ignoredPropNames);
        println(abacusXMLDOMParser.serialize(account, config));

        println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(account), Account.class));

        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(account), Account.class)));
    }

    @Test
    public void testValueOf_2() {
        valueOf();

        Profiler.run(this, "valueOf", 32, 100, 1).printResult();
    }

    @Test
    public void testSerialize() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String xml = abacusXmlParser.serialize(account);
        N.println(xml);

        Account xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        N.println(account);
        N.println(xmlBean);
        N.println(abacusXmlParser.serialize(account));
        N.println(abacusXmlParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(account));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXmlParser.serialize(account), abacusXmlParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));

        xml = abacusXmlParser.serialize(account);
        N.println(xml);

        xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        final XmlSerConfig config = new XmlSerConfig();
        config.setTagByPropertyName(false);
        xml = abacusXmlParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        config.setWriteTypeInfo(true);
        xml = abacusXmlParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);
    }

    @Test
    public void testSerialize_1() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String xml = abacusXMLDOMParser.serialize(account);
        N.println(xml);

        Account xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        N.println(account);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(account));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(account));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLDOMParser.serialize(account), abacusXMLDOMParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));

        xml = abacusXMLDOMParser.serialize(account);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        final XmlSerConfig config = new XmlSerConfig();
        config.setTagByPropertyName(false);
        xml = abacusXMLDOMParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        config.setWriteTypeInfo(true);
        xml = abacusXMLDOMParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);
    }

    @Test
    public void testSerialize1() {
        final Bean bean = new Bean();
        bean.setTypeList(CommonUtil.toList(
                "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★', '\n',
                '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                new String[] {
                        "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                        "\r", "\t", "\"", "\'" }));

        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        final String xml = abacusXmlParser.serialize(bean);
        println(xml);

        final Bean xmlBean = abacusXmlParser.deserialize(xml, Bean.class);
        N.println(abacusXmlParser.serialize(bean));
        N.println(abacusXmlParser.serialize(xmlBean));

        N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean.class));
        N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), Bean.class));
    }

    @Test
    public void testSerialize1_1() {
        final Bean bean = new Bean();
        bean.setTypeList(CommonUtil.toList(
                "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★', '\n',
                '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                new String[] {
                        "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                        "\r", "\t", "\"", "\'" }));

        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        final String xml = abacusXMLDOMParser.serialize(bean);
        println(xml);

        final Bean xmlBean = abacusXMLDOMParser.deserialize(xml, Bean.class);
        assertEquals(abacusXMLDOMParser.serialize(bean), abacusXMLDOMParser.serialize(xmlBean));
        N.println(abacusXMLDOMParser.serialize(bean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));

        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean.class));
        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), Bean.class));
    }

    @Test
    public void testSerialize2() {
        println(String.valueOf((char) 0));

        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
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

        final List typeList = new ArrayList();
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

        final String xml = abacusXmlParser.serialize(xBean, XmlSerConfig.create().setWriteTypeInfo(true));
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXmlParser.deserialize(xml, XBean.class);
        N.println(xmlBean.getTypeList().get(1).getClass());
        N.println(xBean);
        N.println(xmlBean);
        N.println(abacusXmlParser.serialize(xBean));
        N.println(abacusXmlParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(xBean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXmlParser.deserialize(abacusXmlParser.serialize(xBean), XBean.class),
                abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), XBean.class));

        assertEquals(abacusXmlParser.serialize(xBean), abacusXmlParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));

        N.println(abacusXmlParser.serialize(xBean));
        N.println(abacusXmlParser.serialize(xmlBean));

        N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(xBean), XBean.class));
        N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), XBean.class));
    }

    @Test
    public void testSerialize2_1() {
        println(String.valueOf((char) 0));

        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
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

        final List typeList = new ArrayList();
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

        final String xml = abacusXMLDOMParser.serialize(xBean, XmlSerConfig.create().setWriteTypeInfo(true));
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        N.println(xBean);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(xBean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(xBean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xBean), XBean.class),
                abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), XBean.class));

        assertEquals(abacusXMLDOMParser.serialize(xBean), abacusXMLDOMParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));

        N.println(abacusXMLDOMParser.serialize(xBean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));

        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xBean), XBean.class));
        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), XBean.class));
    }

    @Test
    public void testSerialize3() {
        final Account account = createAccount(Account.class);
        account.setId(100);

        String st = abacusXmlParser.serialize(account);
        println(st);

        println(abacusXmlParser.deserialize(st, Account.class));

        st = abacusXmlParser.serialize(account);
        println(st);

        println(abacusXmlParser.deserialize(st, Account.class));
        assertEquals(account, abacusXmlParser.deserialize(st, Account.class));
    }

    @Test
    public void testSerialize3_1() {
        final Account account = createAccount(Account.class);
        account.setId(100);

        String st = abacusXMLDOMParser.serialize(account);
        println(st);

        println(abacusXMLDOMParser.deserialize(st, Account.class));

        st = abacusXMLDOMParser.serialize(account);
        println(st);

        println(abacusXMLDOMParser.deserialize(st, Account.class));
        assertEquals(account, abacusXMLDOMParser.deserialize(st, Account.class));
    }

    @Test
    public void testGenericType() {
        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                CommonUtil.toList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(CommonUtil.toSet(1L, 2L));

        final XmlSerConfig config = new XmlSerConfig();
        config.setWriteTypeInfo(true);

        final String xml = abacusXmlParser.serialize(xBean, config);
        println(xml);

        assertEquals(xBean, abacusXmlParser.deserialize(xml, XBean.class));
    }

    @Test
    public void testGenericType_1() {
        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                CommonUtil.toList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(CommonUtil.toSet(1L, 2L));

        final XmlSerConfig config = new XmlSerConfig();
        config.setWriteTypeInfo(true);

        final String xml = abacusXMLDOMParser.serialize(xBean, config);
        println(xml);

        assertEquals(xBean, abacusXMLDOMParser.deserialize(xml, XBean.class));
    }

    @Test
    public void testPerformanceForBigBean() throws Exception {
    }

    @Test
    public void testSerialize4() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');

        xBean.setTypeString("");

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

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);

        final String xml = abacusXmlParser.serialize(xBean);
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXmlParser.deserialize(xml, XBean.class);
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize4_1() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');

        xBean.setTypeString("");

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

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);

        final String xml = abacusXMLDOMParser.serialize(xBean);
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize5() {
        final List<Account> accounts = createAccountWithContact(Account.class, 100);
        final String xml = abacusXmlParser.serialize(accounts);
        println(xml);

        final List<Account> xmlAccounts = abacusXmlParser.deserialize(xml,
                XmlDeserConfig.create().setElementType(Account.class).setIgnoreUnmatchedProperty(true).setIgnoredPropNames((Map<Class<?>, Set<String>>) null),
                List.class);

        N.println(CommonUtil.stringOf(accounts));
        N.println(CommonUtil.stringOf(xmlAccounts));

    }

    @Test
    public void testSerialize5_1() {
        final List<Account> accounts = createAccountWithContact(Account.class, 100);
        final String xml = abacusXMLDOMParser.serialize(accounts);
        println(xml);

        final List<Account> xmlAccounts = abacusXMLDOMParser.deserialize(xml,
                XmlDeserConfig.create().setElementType(Account.class).setIgnoreUnmatchedProperty(true).setIgnoredPropNames((Map<Class<?>, Set<String>>) null),
                List.class);

        N.println(CommonUtil.stringOf(accounts));
        N.println(CommonUtil.stringOf(xmlAccounts));

    }

    @Test
    public void testSerialize6() {
        final Account account = createAccountWithContact(Account.class);
        final String xml = abacusXmlParser.serialize(account);
        println(xml);

        final com.landawn.abacus.entity.extendDirty.basic.Account xmlBean = abacusXmlParser.deserialize(xml,
                com.landawn.abacus.entity.extendDirty.basic.Account.class);
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize6_1() {
        final Account account = createAccountWithContact(Account.class);
        final String xml = abacusXMLDOMParser.serialize(account);
        println(xml);

        final com.landawn.abacus.entity.extendDirty.basic.Account xmlBean = abacusXMLDOMParser.deserialize(xml,
                com.landawn.abacus.entity.extendDirty.basic.Account.class);
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize7() throws Exception {
        final InputStream is = new FileInputStream("./src/test/resources/XBean.xml");
        final XBean xmlBean = abacusXmlParser.deserialize(is, XBean.class);
        is.close();

        N.println(CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize7_() throws Exception {
        final InputStream is = new FileInputStream("./src/test/resources/XBean.xml");
        final XBean xmlBean = abacusXMLDOMParser.deserialize(is, XBean.class);
        is.close();

        N.println(CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testXBean() {
        final XBean bean = new XBean();
        final Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        final String xml = abacusXmlParser.serialize(bean);
        println(xml);

        final XBean xmlBean = abacusXmlParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(abacusXmlParser.serialize(bean));
        N.println(abacusXmlParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), XBean.class),
                abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), XBean.class));

    }

    @Test
    public void testXBean_1() {
        final XBean bean = new XBean();
        final Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        final String xml = abacusXMLDOMParser.serialize(bean);
        println(xml);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(bean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), XBean.class),
                abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), XBean.class));

    }

    @Test
    public void testMarshaller() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + "kd ");
        customer.setAge(29);
        customer.setChar('c');

        final String xml = XmlUtil.marshal(customer);
        println(xml);

        final Customer newCustomer = XmlUtil.unmarshal(Customer.class, xml);

        assertEquals(customer, newCustomer);
    }

    @Test
    public void testXMLEncoder() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + (char) 0 + "kd ");
        customer.setAge(29);
        customer.setChar((char) 1);

        final String xml = XmlUtil.xmlEncode(customer);
        println(xml);

        final Customer newCustomer = XmlUtil.xmlDecode(xml);

        assertEquals(customer, newCustomer);
    }

    @Test
    public void testJAXB() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + ((char) 0) + "kd ");
        customer.setAge(29);
        customer.setChar('c');

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);
            final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(customer, System.out);
        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMergeTwoArrays() {
        Integer[] a = { 1, 3, 5, 7 };
        Integer[] b = { 2, 4, 6, 8 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testMergeTwoArraysFirstNull() {
        Integer[] a = null;
        Integer[] b = { 1, 2, 3 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMergeTwoArraysSecondNull() {
        Integer[] a = { 1, 2, 3 };
        Integer[] b = null;

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMergeTwoArraysBothNull() {
        Integer[] a = null;
        Integer[] b = null;

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeTwoArraysBothEmpty() {
        Integer[] a = {};
        Integer[] b = {};

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeTwoIterables() {
        List<Integer> a = Arrays.asList(1, 3, 5);
        List<Integer> b = Arrays.asList(2, 4, 6);

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeTwoIterablesNullInputs() {
        List<Integer> a = null;
        List<Integer> b = Arrays.asList(1, 2, 3);

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertNotNull(result);
    }

    @Test
    public void testMergeCollectionOfIterables() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(lists, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testMergeCollectionOfIterablesEmpty() {
        List<List<Integer>> lists = new ArrayList<>();

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(lists, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeCollectionOfIterablesNull() {
        List<List<Integer>> lists = null;

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(lists, selector);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testMergeCollectionOfIterablesWithSupplier() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        Set<Integer> result = N.merge(lists, selector, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)), result);
    }

    @Test
    public void testZipTwoArrays() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("b", result.get(1).left());
        assertEquals(2, result.get(1).right());
        assertEquals("c", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testZipTwoArraysDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipTwoArraysFirstNull() {
        String[] a = null;
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysSecondNull() {
        String[] a = { "a", "b", "c" };
        Integer[] b = null;

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysBothNull() {
        String[] a = null;
        Integer[] b = null;

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysEmpty() {
        String[] a = {};
        Integer[] b = {};

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoIterables() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<Integer> b = Arrays.asList(1, 2, 3);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipTwoIterablesNull() {
        List<String> a = null;
        List<Integer> b = Arrays.asList(1, 2, 3);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoIterablesEmpty() {
        List<String> a = new ArrayList<>();
        List<Integer> b = new ArrayList<>();

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipThreeArrays() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true, false, true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testZipThreeArraysDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true, false };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(2, result.size());
    }

    @Test
    public void testZipThreeArraysOneNull() {
        String[] a = { "a", "b", "c" };
        Integer[] b = null;
        Boolean[] c = { true, false, true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipThreeIterables() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<Integer> b = Arrays.asList(1, 2, 3);
        List<Boolean> c = Arrays.asList(true, false, true);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testZipThreeIterablesOneNull() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = null;
        List<Boolean> c = Arrays.asList(true, false);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysWithDefaults() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3, 4, 5 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(5, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("default", result.get(3).left());
        assertEquals(4, result.get(3).right());
        assertEquals("default", result.get(4).left());
        assertEquals(5, result.get(4).right());
    }

    @Test
    public void testZipTwoArraysWithDefaultsFirstLonger() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(5, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("c", result.get(2).left());
        assertEquals(0, result.get(2).right());
    }

    @Test
    public void testZipTwoArraysWithDefaultsBothNull() {
        String[] a = null;
        Integer[] b = null;

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoArraysWithDefaultsFirstNull() {
        String[] a = null;
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(3, result.size());
        assertEquals("default", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipTwoIterablesWithDefaults() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = Arrays.asList(1, 2, 3, 4);

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(4, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("default", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testZipTwoIterablesWithDefaultsFirstNull() {
        List<String> a = null;
        List<Integer> b = Arrays.asList(1, 2, 3);

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(3, result.size());
        assertEquals("default", result.get(0).left());
        assertEquals(1, result.get(0).right());
    }

    @Test
    public void testZipThreeArraysWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
        assertEquals("default", result.get(2).left());
        assertEquals(3, result.get(2).middle());
        assertEquals(false, result.get(2).right());
    }

    @Test
    public void testZipThreeArraysWithDefaultsAllDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(5, result.size());
        assertEquals("d", result.get(3).left());
        assertEquals(0, result.get(3).middle());
        assertEquals(false, result.get(3).right());
    }

    @Test
    public void testZipThreeIterablesWithDefaults() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = Arrays.asList(1, 2, 3, 4);
        List<Boolean> c = Arrays.asList(true);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(4, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
        assertEquals("default", result.get(3).left());
        assertEquals(4, result.get(3).middle());
        assertEquals(false, result.get(3).right());
    }

    @Test
    public void testZipTwoArraysToArray() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        Pair<String, Integer>[] result = N.zip(a, b, Pair::of, Pair.class);

        assertEquals(3, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].right());
        assertEquals("b", result[1].left());
        assertEquals(2, result[1].right());
    }

    @Test
    public void testZipTwoArraysToArrayDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2 };

        Pair<String, Integer>[] result = N.zip(a, b, Pair::of, Pair.class);

        assertEquals(2, result.length);
    }

    @Test
    public void testZipTwoArraysToArrayFirstNull() {
        String[] a = null;
        Integer[] b = { 1, 2, 3 };

        Pair<String, Integer>[] result = N.zip(a, b, Pair::of, Pair.class);

        assertEquals(0, result.length);
    }

    @Test
    public void testZipTwoArraysToArrayWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3, 4 };

        Pair<String, Integer>[] result = N.zip(a, b, "default", 0, Pair::of, Pair.class);

        assertEquals(4, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].right());
        assertEquals("default", result[2].left());
        assertEquals(3, result[2].right());
    }

    @Test
    public void testZipThreeArraysToArray() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true, false, true };

        Triple<String, Integer, Boolean>[] result = N.zip(a, b, c, Triple::of, Triple.class);

        assertEquals(3, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].middle());
        assertEquals(true, result[0].right());
    }

    @Test
    public void testZipThreeArraysToArrayDifferentLengths() {
        String[] a = { "a", "b", "c", "d", "e" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        Triple<String, Integer, Boolean>[] result = N.zip(a, b, c, Triple::of, Triple.class);

        assertEquals(1, result.length);
    }

    @Test
    public void testZipThreeArraysToArrayWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        Boolean[] c = { true };

        Triple<String, Integer, Boolean>[] result = N.zip(a, b, c, "default", 0, false, Triple::of, Triple.class);

        assertEquals(3, result.length);
        assertEquals("a", result[0].left());
        assertEquals(1, result[0].middle());
        assertEquals(true, result[0].right());
        assertEquals("default", result[2].left());
        assertEquals(3, result[2].middle());
        assertEquals(false, result[2].right());
    }

    @Test
    public void testUnzipIterable() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzipIterableNull() {
        List<Pair<String, Integer>> pairs = null;

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipIterableEmpty() {
        List<Pair<String, Integer>> pairs = new ArrayList<>();

        Pair<List<String>, List<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipIterableWithSupplier() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3));

        Pair<Set<String>, Set<Integer>> result = N.unzip(pairs, (pair, output) -> {
            output.setLeft(pair.left());
            output.setRight(pair.right());
        }, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), result.left());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result.right());
    }

    @Test
    public void testUnzipIterableComplexObjects() {
        List<String> strings = Arrays.asList("a:1", "b:2", "c:3");

        Pair<List<String>, List<Integer>> result = N.unzip(strings, (str, output) -> {
            String[] parts = str.split(":");
            output.setLeft(parts[0]);
            output.setRight(Integer.parseInt(parts[1]));
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testUnzippIterable() {
        List<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false), Triple.of("c", 3, true));

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.middle());
        assertEquals(Arrays.asList(true, false, true), result.right());
    }

    @Test
    public void testUnzippIterableNull() {
        List<Triple<String, Integer, Boolean>> triples = null;

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzippIterableEmpty() {
        List<Triple<String, Integer, Boolean>> triples = new ArrayList<>();

        Triple<List<String>, List<Integer>, List<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.middle().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzippIterableWithSupplier() {
        List<Triple<String, Integer, Boolean>> triples = Arrays.asList(Triple.of("a", 1, true), Triple.of("b", 2, false));

        Triple<Set<String>, Set<Integer>, Set<Boolean>> result = N.unzip3(triples, (triple, output) -> {
            output.setLeft(triple.left());
            output.setMiddle(triple.middle());
            output.setRight(triple.right());
        }, HashSet::new);

        assertEquals(new HashSet<>(Arrays.asList("a", "b")), result.left());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.middle());
        assertEquals(new HashSet<>(Arrays.asList(true, false)), result.right());
    }

    @Test
    public void testZipWithFunctionReturningNull() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        BiFunction<String, Integer, Pair<String, Integer>> zipFunc = (s, i) -> i == 2 ? null : Pair.of(s, i);
        List<Pair<String, Integer>> result = N.zip(a, b, zipFunc);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals(null, result.get(1));
        assertEquals("c", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testMergeWithAlwaysTakeFirst() {
        Integer[] a = { 1, 2, 3 };
        Integer[] b = { 4, 5, 6 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> MergeResult.TAKE_FIRST;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithAlwaysTakeSecond() {
        Integer[] a = { 1, 2, 3 };
        Integer[] b = { 4, 5, 6 };

        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a, b, selector);

        assertEquals(Arrays.asList(4, 5, 6, 1, 2, 3), result);
    }

    @Test
    public void testZipTwoIterablesFirstLonger() {
        List<String> a = Arrays.asList("a", "b", "c", "d", "e");
        List<Integer> b = Arrays.asList(1, 2);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(2, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("b", result.get(1).left());
        assertEquals(2, result.get(1).right());
    }

    @Test
    public void testZipTwoIterablesSecondLonger() {
        List<String> a = Arrays.asList("a", "b");
        List<Integer> b = Arrays.asList(1, 2, 3, 4, 5);

        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);

        assertEquals(2, result.size());
    }

    @Test
    public void testZipThreeIterablesAllDifferentLengths() {
        List<String> a = Arrays.asList("a", "b", "c", "d", "e");
        List<Integer> b = Arrays.asList(1, 2, 3);
        List<Boolean> c = Arrays.asList(true);

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, Triple::of);

        assertEquals(1, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testUnzipWithNullElements() {
        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("a", 1), null, Pair.of("c", 3));

        assertThrows(NullPointerException.class, () -> {
            N.unzip(pairs, (pair, output) -> {
                output.setLeft(pair.left());
                output.setRight(pair.right());
            });
        });
    }

    @Test
    public void testZipTwoArraysWithDefaultsSameLength() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        List<Pair<String, Integer>> result = N.zip(a, b, "default", 0, Pair::of);

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).right());
        assertEquals("b", result.get(1).left());
        assertEquals(2, result.get(1).right());
        assertEquals("c", result.get(2).left());
        assertEquals(3, result.get(2).right());
    }

    @Test
    public void testZipThreeArraysWithDefaultsSameLength() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2 };
        Boolean[] c = { true, false };

        List<Triple<String, Integer, Boolean>> result = N.zip(a, b, c, "default", 0, false, Triple::of);

        assertEquals(2, result.size());
        assertEquals("a", result.get(0).left());
        assertEquals(1, result.get(0).middle());
        assertEquals(true, result.get(0).right());
    }

    @Test
    public void testOccurrencesOfBooleanArray() {
        boolean[] flags = { true, false, true, true, false };
        assertEquals(3, N.frequency(flags, true));
        assertEquals(2, N.frequency(flags, false));
    }

    @Test
    public void testOccurrencesOfBooleanArrayEmpty() {
        boolean[] empty = {};
        assertEquals(0, N.frequency(empty, true));
        assertEquals(0, N.frequency(empty, false));
    }

    @Test
    public void testOccurrencesOfBooleanArrayNull() {
        boolean[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, true));
        assertEquals(0, N.frequency(nullArray, false));
    }

    @Test
    public void testOccurrencesOfBooleanArrayAllSame() {
        boolean[] allTrue = { true, true, true };
        assertEquals(3, N.frequency(allTrue, true));
        assertEquals(0, N.frequency(allTrue, false));

        boolean[] allFalse = { false, false, false, false };
        assertEquals(0, N.frequency(allFalse, true));
        assertEquals(4, N.frequency(allFalse, false));
    }

    @Test
    public void testOccurrencesOfCharArray() {
        char[] letters = { 'a', 'b', 'c', 'a', 'b', 'a' };
        assertEquals(3, N.frequency(letters, 'a'));
        assertEquals(2, N.frequency(letters, 'b'));
        assertEquals(1, N.frequency(letters, 'c'));
        assertEquals(0, N.frequency(letters, 'z'));
    }

    @Test
    public void testOccurrencesOfCharArrayEmpty() {
        char[] empty = {};
        assertEquals(0, N.frequency(empty, 'x'));
    }

    @Test
    public void testOccurrencesOfCharArrayNull() {
        char[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, 'y'));
    }

    @Test
    public void testOccurrencesOfCharArraySingleElement() {
        char[] single = { 'a' };
        assertEquals(1, N.frequency(single, 'a'));
        assertEquals(0, N.frequency(single, 'b'));
    }

    @Test
    public void testOccurrencesOfByteArray() {
        byte[] data = { 1, 2, 3, 1, 2, 1 };
        assertEquals(3, N.frequency(data, (byte) 1));
        assertEquals(2, N.frequency(data, (byte) 2));
        assertEquals(1, N.frequency(data, (byte) 3));
        assertEquals(0, N.frequency(data, (byte) 0));
    }

    @Test
    public void testOccurrencesOfByteArrayEmpty() {
        byte[] empty = {};
        assertEquals(0, N.frequency(empty, (byte) 5));
    }

    @Test
    public void testOccurrencesOfByteArrayNull() {
        byte[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, (byte) 1));
    }

    @Test
    public void testOccurrencesOfByteArrayNegativeValues() {
        byte[] negatives = { -1, -2, -1, 0, -1 };
        assertEquals(3, N.frequency(negatives, (byte) -1));
        assertEquals(1, N.frequency(negatives, (byte) -2));
        assertEquals(1, N.frequency(negatives, (byte) 0));
    }

    @Test
    public void testOccurrencesOfShortArray() {
        short[] values = { 10, 20, 30, 10, 20, 10 };
        assertEquals(3, N.frequency(values, (short) 10));
        assertEquals(2, N.frequency(values, (short) 20));
        assertEquals(1, N.frequency(values, (short) 30));
        assertEquals(0, N.frequency(values, (short) 0));
    }

    @Test
    public void testOccurrencesOfShortArrayEmpty() {
        short[] empty = {};
        assertEquals(0, N.frequency(empty, (short) 5));
    }

    @Test
    public void testOccurrencesOfShortArrayNull() {
        short[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, (short) 10));
    }

    @Test
    public void testOccurrencesOfIntArray() {
        int[] numbers = { 1, 2, 3, 2, 4, 2, 5 };
        assertEquals(3, N.frequency(numbers, 2));
        assertEquals(1, N.frequency(numbers, 1));
        assertEquals(0, N.frequency(numbers, 10));
    }

    @Test
    public void testOccurrencesOfIntArrayEmpty() {
        int[] empty = {};
        assertEquals(0, N.frequency(empty, 1));
    }

    @Test
    public void testOccurrencesOfIntArrayNull() {
        int[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, 1));
    }

    @Test
    public void testOccurrencesOfIntArrayLargeNumbers() {
        int[] large = { 1000000, 2000000, 1000000, 3000000 };
        assertEquals(2, N.frequency(large, 1000000));
        assertEquals(1, N.frequency(large, 2000000));
    }

    @Test
    public void testOccurrencesOfLongArray() {
        long[] ids = { 1000L, 2000L, 3000L, 1000L, 2000L, 1000L };
        assertEquals(3, N.frequency(ids, 1000L));
        assertEquals(2, N.frequency(ids, 2000L));
        assertEquals(1, N.frequency(ids, 3000L));
        assertEquals(0, N.frequency(ids, 5000L));
    }

    @Test
    public void testOccurrencesOfLongArrayEmpty() {
        long[] empty = {};
        assertEquals(0, N.frequency(empty, 100L));
    }

    @Test
    public void testOccurrencesOfLongArrayNull() {
        long[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, 1000L));
    }

    @Test
    public void testOccurrencesOfFloatArray() {
        float[] prices = { 1.5f, 2.3f, 1.5f, 3.7f, 1.5f };
        assertEquals(3, N.frequency(prices, 1.5f));
        assertEquals(1, N.frequency(prices, 2.3f));
        assertEquals(0, N.frequency(prices, 9.9f));
    }

    @Test
    public void testOccurrencesOfFloatArrayWithNaN() {
        float[] withNaN = { 1.0f, Float.NaN, 2.0f, Float.NaN, 3.0f };
        assertEquals(2, N.frequency(withNaN, Float.NaN));
        assertEquals(1, N.frequency(withNaN, 1.0f));
    }

    @Test
    public void testOccurrencesOfFloatArrayEmpty() {
        float[] empty = {};
        assertEquals(0, N.frequency(empty, 1.0f));
    }

    @Test
    public void testOccurrencesOfFloatArrayNull() {
        float[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, 1.5f));
    }

    @Test
    public void testOccurrencesOfFloatArrayZero() {
        float[] zeros = { 0.0f, -0.0f, 0.0f };
        assertEquals(2, N.frequency(zeros, 0.0f));
        assertEquals(1, N.frequency(zeros, -0.0f));
    }

    @Test
    public void testOccurrencesOfDoubleArray() {
        double[] measurements = { 1.5, 2.3, 1.5, 3.7, 1.5 };
        assertEquals(3, N.frequency(measurements, 1.5));
        assertEquals(1, N.frequency(measurements, 2.3));
        assertEquals(0, N.frequency(measurements, 9.9));
    }

    @Test
    public void testOccurrencesOfDoubleArrayWithNaN() {
        double[] withNaN = { 1.0, Double.NaN, 2.0, Double.NaN };
        assertEquals(2, N.frequency(withNaN, Double.NaN));
        assertEquals(1, N.frequency(withNaN, 1.0));
    }

    @Test
    public void testOccurrencesOfDoubleArrayEmpty() {
        double[] empty = {};
        assertEquals(0, N.frequency(empty, 1.0));
    }

    @Test
    public void testOccurrencesOfDoubleArrayNull() {
        double[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, 1.5));
    }

    @Test
    public void testOccurrencesOfDoubleArrayInfinity() {
        double[] infinities = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
        assertEquals(2, N.frequency(infinities, Double.POSITIVE_INFINITY));
        assertEquals(1, N.frequency(infinities, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testOccurrencesOfObjectArray() {
        String[] words = { "hello", "world", "hello", "java", "hello" };
        assertEquals(3, N.frequency(words, "hello"));
        assertEquals(1, N.frequency(words, "world"));
        assertEquals(0, N.frequency(words, "missing"));
    }

    @Test
    public void testOccurrencesOfObjectArrayWithNulls() {
        String[] withNulls = { "a", null, "b", null, "c" };
        assertEquals(2, N.frequency(withNulls, null));
        assertEquals(1, N.frequency(withNulls, "a"));
    }

    @Test
    public void testOccurrencesOfObjectArrayEmpty() {
        Object[] empty = {};
        assertEquals(0, N.frequency(empty, "test"));
    }

    @Test
    public void testOccurrencesOfObjectArrayNull() {
        Object[] nullArray = null;
        assertEquals(0, N.frequency(nullArray, "test"));
    }

    @Test
    public void testOccurrencesOfObjectArrayDifferentTypes() {
        Object[] mixed = { 1, "hello", 1, 2.5, "hello", 1 };
        assertEquals(3, N.frequency(mixed, 1));
        assertEquals(2, N.frequency(mixed, "hello"));
        assertEquals(1, N.frequency(mixed, 2.5));
    }

    @Test
    public void testOccurrencesOfIterable() {
        List<String> words = Arrays.asList("hello", "world", "hello", "java");
        assertEquals(2, N.frequency(words, "hello"));
        assertEquals(1, N.frequency(words, "world"));
        assertEquals(0, N.frequency(words, "missing"));
    }

    @Test
    public void testOccurrencesOfIterableWithNulls() {
        List<String> withNulls = Arrays.asList("a", null, "b", null);
        assertEquals(2, N.frequency(withNulls, null));
        assertEquals(1, N.frequency(withNulls, "a"));
    }

    @Test
    public void testOccurrencesOfIterableEmpty() {
        List<String> empty = new ArrayList<>();
        assertEquals(0, N.frequency(empty, "test"));
    }

    @Test
    public void testOccurrencesOfIterableNull() {
        Iterable<String> nullIterable = null;
        assertEquals(0, N.frequency(nullIterable, "test"));
    }

    @Test
    public void testOccurrencesOfIterableSet() {
        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(1, N.frequency(numbers, 3));
        assertEquals(0, N.frequency(numbers, 10));
    }

    @Test
    public void testOccurrencesOfIterator() {
        List<String> words = Arrays.asList("hello", "world", "hello", "java");
        Iterator<String> iter = words.iterator();
        assertEquals(2, N.frequency(iter, "hello"));
    }

    @Test
    public void testOccurrencesOfIteratorNull() {
        Iterator<String> nullIter = null;
        assertEquals(0, N.frequency(nullIter, "test"));
    }

    @Test
    public void testOccurrencesOfIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        assertEquals(0, N.frequency(empty, "test"));
    }

    @Test
    public void testOccurrencesOfStringChar() {
        String text = "hello world";
        assertEquals(3, N.frequency(text, 'l'));
        assertEquals(2, N.frequency(text, 'o'));
        assertEquals(1, N.frequency(text, ' '));
        assertEquals(0, N.frequency(text, 'z'));
    }

    @Test
    public void testOccurrencesOfStringCharEmpty() {
        String empty = "";
        assertEquals(0, N.frequency(empty, 'a'));
    }

    @Test
    public void testOccurrencesOfStringCharNull() {
        String nullStr = null;
        assertEquals(0, N.frequency(nullStr, 'x'));
    }

    @Test
    public void testOccurrencesOfStringSubstring() {
        String text = "hello hello world hello";
        assertEquals(3, N.frequency(text, "hello"));
        assertEquals(1, N.frequency(text, "world"));
        assertEquals(0, N.frequency(text, "java"));
    }

    @Test
    public void testOccurrencesOfStringSubstringOverlapping() {
        String pattern = "ababab";
        assertEquals(3, N.frequency(pattern, "ab"));
    }

    @Test
    public void testOccurrencesOfStringSubstringEmpty() {
        String empty = "";
        assertEquals(0, N.frequency(empty, "test"));
    }

    @Test
    public void testOccurrencesOfStringSubstringNull() {
        String nullStr = null;
        assertEquals(0, N.frequency(nullStr, "test"));
    }

    @Test
    public void testOccurrencesMapArray() {
        String[] words = { "apple", "banana", "apple", "cherry", "banana", "apple" };
        Map<String, Integer> counts = N.frequencyMap(words);

        assertEquals(3, counts.size());
        assertEquals(3, counts.get("apple").intValue());
        assertEquals(2, counts.get("banana").intValue());
        assertEquals(1, counts.get("cherry").intValue());
    }

    @Test
    public void testOccurrencesMapArrayEmpty() {
        String[] empty = {};
        Map<String, Integer> counts = N.frequencyMap(empty);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapArrayNull() {
        String[] nullArray = null;
        Map<String, Integer> counts = N.frequencyMap(nullArray);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapArrayWithNulls() {
        String[] withNulls = { "a", null, "b", null, "a" };
        Map<String, Integer> counts = N.frequencyMap(withNulls);

        assertEquals(3, counts.size());
        assertEquals(2, counts.get("a").intValue());
        assertEquals(1, counts.get("b").intValue());
        assertEquals(2, counts.get(null).intValue());
    }

    @Test
    public void testOccurrencesMapArrayWithSupplier() {
        String[] words = { "c", "a", "b", "a", "c" };
        Map<String, Integer> sorted = N.frequencyMap(words, TreeMap::new);

        assertTrue(sorted instanceof TreeMap);
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));
    }

    @Test
    public void testOccurrencesMapArrayLinkedHashMap() {
        String[] words = { "c", "a", "b", "a", "c" };
        Map<String, Integer> ordered = N.frequencyMap(words, LinkedHashMap::new);

        assertTrue(ordered instanceof LinkedHashMap);
        assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(ordered.keySet()));
    }

    @Test
    public void testOccurrencesMapIterable() {
        List<String> words = Arrays.asList("apple", "banana", "apple", "cherry");
        Map<String, Integer> counts = N.frequencyMap(words);

        assertEquals(3, counts.size());
        assertEquals(2, counts.get("apple").intValue());
        assertEquals(1, counts.get("banana").intValue());
        assertEquals(1, counts.get("cherry").intValue());
    }

    @Test
    public void testOccurrencesMapIterableEmpty() {
        List<String> empty = new ArrayList<>();
        Map<String, Integer> counts = N.frequencyMap(empty);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIterableNull() {
        Iterable<String> nullIterable = null;
        Map<String, Integer> counts = N.frequencyMap(nullIterable);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIterableSet() {
        Set<Integer> uniqueNumbers = new HashSet<>(Arrays.asList(1, 2, 3));
        Map<Integer, Integer> setCounts = N.frequencyMap(uniqueNumbers);

        assertEquals(3, setCounts.size());
        assertEquals(1, setCounts.get(1).intValue());
        assertEquals(1, setCounts.get(2).intValue());
        assertEquals(1, setCounts.get(3).intValue());
    }

    @Test
    public void testOccurrencesMapIterableWithSupplier() {
        List<String> words = Arrays.asList("c", "a", "b", "a", "c");
        Map<String, Integer> sorted = N.frequencyMap(words, TreeMap::new);

        assertTrue(sorted instanceof TreeMap);
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));
    }

    @Test
    public void testOccurrencesMapIterator() {
        List<String> words = Arrays.asList("apple", "banana", "apple", "cherry");
        Iterator<String> iter = words.iterator();
        Map<String, Integer> counts = N.frequencyMap(iter);

        assertEquals(3, counts.size());
        assertEquals(2, counts.get("apple").intValue());
        assertEquals(1, counts.get("banana").intValue());
    }

    @Test
    public void testOccurrencesMapIteratorNull() {
        Iterator<String> nullIter = null;
        Map<String, Integer> counts = N.frequencyMap(nullIter);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        Map<String, Integer> counts = N.frequencyMap(empty);
        assertTrue(counts.isEmpty());
    }

    @Test
    public void testOccurrencesMapIteratorWithSupplier() {
        List<String> words = Arrays.asList("c", "a", "b", "a", "c");
        Iterator<String> iter = words.iterator();
        Map<String, Integer> sorted = N.frequencyMap(iter, TreeMap::new);

        assertTrue(sorted instanceof TreeMap);
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));
    }

    @Test
    public void testContainsBooleanArray() {
        boolean[] flags = { true, false, true };
        assertTrue(N.contains(flags, true));
        assertTrue(N.contains(flags, false));
    }

    @Test
    public void testContainsBooleanArrayEmpty() {
        boolean[] empty = {};
        assertTrue(!N.contains(empty, true));
    }

    @Test
    public void testContainsBooleanArrayNull() {
        boolean[] nullArray = null;
        assertTrue(!N.contains(nullArray, false));
    }

    @Test
    public void testContainsCharArray() {
        char[] letters = { 'a', 'b', 'c', 'd' };
        assertTrue(N.contains(letters, 'c'));
        assertTrue(!N.contains(letters, 'z'));
    }

    @Test
    public void testContainsCharArrayEmpty() {
        char[] empty = {};
        assertTrue(!N.contains(empty, 'x'));
    }

    @Test
    public void testContainsCharArrayNull() {
        char[] nullArray = null;
        assertTrue(!N.contains(nullArray, 'y'));
    }

    @Test
    public void testContainsByteArray() {
        byte[] data = { 1, 2, 3, 4, 5 };
        assertTrue(N.contains(data, (byte) 3));
        assertTrue(!N.contains(data, (byte) 10));
    }

    @Test
    public void testContainsByteArrayEmpty() {
        byte[] empty = {};
        assertTrue(!N.contains(empty, (byte) 1));
    }

    @Test
    public void testContainsByteArrayNull() {
        byte[] nullArray = null;
        assertTrue(!N.contains(nullArray, (byte) 2));
    }

    @Test
    public void testContainsShortArray() {
        short[] values = { 10, 20, 30, 40, 50 };
        assertTrue(N.contains(values, (short) 30));
        assertTrue(!N.contains(values, (short) 100));
    }

    @Test
    public void testContainsShortArrayEmpty() {
        short[] empty = {};
        assertTrue(!N.contains(empty, (short) 10));
    }

    @Test
    public void testContainsShortArrayNull() {
        short[] nullArray = null;
        assertTrue(!N.contains(nullArray, (short) 20));
    }

    @Test
    public void testContainsIntArray() {
        int[] numbers = { 1, 2, 3, 4, 5 };
        assertTrue(N.contains(numbers, 3));
        assertTrue(!N.contains(numbers, 7));
    }

    @Test
    public void testContainsIntArrayEmpty() {
        int[] empty = {};
        assertTrue(!N.contains(empty, 1));
    }

    @Test
    public void testContainsIntArrayNull() {
        int[] nullArray = null;
        assertTrue(!N.contains(nullArray, 1));
    }

    @Test
    public void testContainsLongArray() {
        long[] ids = { 1000L, 2000L, 3000L, 4000L };
        assertTrue(N.contains(ids, 1000L));
        assertTrue(!N.contains(ids, 5000L));
    }

    @Test
    public void testContainsLongArrayEmpty() {
        long[] empty = {};
        assertTrue(!N.contains(empty, 100L));
    }

    @Test
    public void testContainsLongArrayNull() {
        long[] nullArray = null;
        assertTrue(!N.contains(nullArray, 200L));
    }

    @Test
    public void testContainsFloatArray() {
        float[] values = { 1.5f, 2.3f, 3.7f, 4.1f };
        assertTrue(N.contains(values, 1.5f));
        assertTrue(!N.contains(values, 9.9f));
    }

    @Test
    public void testContainsFloatArrayWithNaN() {
        float[] withNaN = { 1.0f, Float.NaN, 2.0f };
        assertTrue(N.contains(withNaN, Float.NaN));
    }

    @Test
    public void testContainsFloatArrayEmpty() {
        float[] empty = {};
        assertTrue(!N.contains(empty, 1.0f));
    }

    @Test
    public void testContainsFloatArrayNull() {
        float[] nullArray = null;
        assertTrue(!N.contains(nullArray, 1.5f));
    }

    @Test
    public void testContainsDoubleArray() {
        double[] values = { 1.5, 2.3, 3.7, 4.1 };
        assertTrue(N.contains(values, 1.5));
        assertTrue(!N.contains(values, 9.9));
    }

    @Test
    public void testContainsDoubleArrayWithNaN() {
        double[] withNaN = { 1.0, Double.NaN, 2.0 };
        assertTrue(N.contains(withNaN, Double.NaN));
    }

    @Test
    public void testContainsDoubleArrayEmpty() {
        double[] empty = {};
        assertTrue(!N.contains(empty, 1.0));
    }

    @Test
    public void testContainsDoubleArrayNull() {
        double[] nullArray = null;
        assertTrue(!N.contains(nullArray, 1.5));
    }

    @Test
    public void testContainsObjectArray() {
        String[] words = { "hello", "world", null, "test" };
        assertTrue(N.contains(words, "hello"));
        assertTrue(N.contains(words, null));
        assertTrue(!N.contains(words, "missing"));
    }

    @Test
    public void testContainsObjectArrayIntegers() {
        Integer[] nums = { 1, 2, 3 };
        assertTrue(N.contains(nums, 2));
        assertTrue(!N.contains(nums, null));
    }

    @Test
    public void testContainsObjectArrayNull() {
        Object[] nullArray = null;
        assertTrue(!N.contains(nullArray, "any"));
    }

    @Test
    public void testContainsObjectArrayEmpty() {
        Object[] empty = {};
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsCollection() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        assertTrue(N.contains(words, "apple"));
        assertTrue(!N.contains(words, "grape"));
    }

    @Test
    public void testContainsCollectionSet() {
        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3));
        assertTrue(N.contains(numbers, 2));
        assertTrue(!N.contains(numbers, 5));
    }

    @Test
    public void testContainsCollectionNull() {
        Collection<String> nullCollection = null;
        assertTrue(!N.contains(nullCollection, "test"));
    }

    @Test
    public void testContainsCollectionEmpty() {
        Collection<String> empty = new ArrayList<>();
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsIterable() {
        List<String> words = Arrays.asList("apple", "banana", null, "cherry");
        assertTrue(N.contains((Iterable<String>) words, "apple"));
        assertTrue(N.contains((Iterable<String>) words, null));
        assertTrue(!N.contains((Iterable<String>) words, "grape"));
    }

    @Test
    public void testContainsIterableSet() {
        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3));
        assertTrue(N.contains((Iterable<Integer>) numbers, 2));
        assertTrue(!N.contains((Iterable<Integer>) numbers, 5));
    }

    @Test
    public void testContainsIterableNull() {
        Iterable<String> nullIterable = null;
        assertTrue(!N.contains(nullIterable, "test"));
    }

    @Test
    public void testContainsIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsIterator() {
        Iterator<String> iterator = Arrays.asList("a", "b", "c").iterator();
        assertTrue(N.contains(iterator, "b"));
    }

    @Test
    public void testContainsIteratorNotFound() {
        Iterator<String> iterator = Arrays.asList("a", "b", "c").iterator();
        assertTrue(!N.contains(iterator, "z"));
    }

    @Test
    public void testContainsIteratorNull() {
        Iterator<String> nullIter = null;
        assertTrue(!N.contains(nullIter, "test"));
    }

    @Test
    public void testContainsIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        assertTrue(!N.contains(empty, "test"));
    }

    @Test
    public void testContainsAllCollection() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue", "yellow"));
        List<String> primary = Arrays.asList("red", "blue", "yellow");
        assertTrue(N.containsAll(colors, primary));
    }

    @Test
    public void testContainsAllCollectionMissing() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue", "yellow"));
        List<String> missing = Arrays.asList("red", "purple");
        assertTrue(!N.containsAll(colors, missing));
    }

    @Test
    public void testContainsAllCollectionEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAll(colors, Collections.emptyList()));
    }

    @Test
    public void testContainsAllCollectionNull() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAll(colors, (Collection) null));
    }

    @Test
    public void testContainsAllCollectionNullCollection() {
        List<String> primary = Arrays.asList("red", "blue");
        assertTrue(!N.containsAll((Collection) null, primary));
    }

    @Test
    public void testContainsAllVarargs() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue", "yellow"));
        assertTrue(N.containsAll(colors, "red", "green", "blue"));
        assertTrue(!N.containsAll(colors, "red", "purple"));
    }

    @Test
    public void testContainsAllVarargsEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAll(colors));
    }

    @Test
    public void testContainsAllVarargsNull() {
        assertTrue(!N.containsAll(null, "red", "blue"));
    }

    @Test
    public void testContainsAllVarargsIntegers() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        assertTrue(N.containsAll(numbers, 2, 4, 5));
        assertTrue(!N.containsAll(numbers, 2, 10));
    }

    @Test
    public void testContainsAllIterable() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "banana"));
        List<String> fruits = Arrays.asList("apple", "banana", "cherry", "date");
        assertTrue(N.containsAll((Iterable<String>) fruits, searchFor));
    }

    @Test
    public void testContainsAllIterableMissing() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "grape"));
        List<String> fruits = Arrays.asList("apple", "banana", "cherry");
        assertTrue(!N.containsAll((Iterable<String>) fruits, searchFor));
    }

    @Test
    public void testContainsAllIterableEmpty() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "banana"));
        Iterable<String> empty = new ArrayList<>();
        assertTrue(!N.containsAll(empty, searchFor));
    }

    @Test
    public void testContainsAllIterableNull() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("apple", "banana"));
        assertTrue(!N.containsAll((Iterable<String>) null, searchFor));
    }

    @Test
    public void testContainsAllIterator() {
        List<String> words = Arrays.asList("hello", "world", "java", "stream");
        Set<String> searchFor = new HashSet<>(Arrays.asList("hello", "java"));
        assertTrue(N.containsAll(words.iterator(), searchFor));
    }

    @Test
    public void testContainsAllIteratorMissing() {
        List<String> words = Arrays.asList("hello", "world", "java");
        Set<String> searchFor = new HashSet<>(Arrays.asList("hello", "missing"));
        assertTrue(!N.containsAll(words.iterator(), searchFor));
    }

    @Test
    public void testContainsAllIteratorNull() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("hello", "java"));
        assertTrue(!N.containsAll((Iterator<String>) null, searchFor));
    }

    @Test
    public void testContainsAllIteratorEmpty() {
        Set<String> searchFor = new HashSet<>(Arrays.asList("test"));
        assertTrue(N.containsAll(Collections.emptyIterator(), Collections.emptySet()));
    }

    @Test
    public void testContainsAnyCollection() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        List<String> warm = Arrays.asList("red", "orange", "yellow");
        assertTrue(N.containsAny(colors, warm));
    }

    @Test
    public void testContainsAnyCollectionNoMatch() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        List<String> cool = Arrays.asList("purple", "cyan");
        assertTrue(!N.containsAny(colors, cool));
    }

    @Test
    public void testContainsAnyCollectionEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(!N.containsAny(colors, Collections.emptyList()));
    }

    @Test
    public void testContainsAnyCollectionNull() {
        List<String> warm = Arrays.asList("red", "orange");
        assertTrue(!N.containsAny(null, warm));
    }

    @Test
    public void testContainsAnyVarargs() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsAny(colors, "red", "orange", "yellow"));
        assertTrue(!N.containsAny(colors, "purple", "cyan"));
    }

    @Test
    public void testContainsAnyVarargsEmpty() {
        Set<String> colors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(!N.containsAny(colors));
    }

    @Test
    public void testContainsAnyVarargsIntegers() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        assertTrue(N.containsAny(numbers, 2, 6, 8));
        assertTrue(!N.containsAny(numbers, 10, 11, 12));
    }

    @Test
    public void testContainsAnyIterable() {
        List<String> fruits = Arrays.asList("apple", "banana", "cherry", "date");
        Set<String> common = new HashSet<>(Arrays.asList("apple", "grape"));
        assertTrue(N.containsAny((Iterable<String>) fruits, common));
    }

    @Test
    public void testContainsAnyIterableNoMatch() {
        List<String> fruits = Arrays.asList("apple", "banana", "cherry");
        Set<String> citrus = new HashSet<>(Arrays.asList("orange", "lemon", "lime"));
        assertTrue(!N.containsAny((Iterable<String>) fruits, citrus));
    }

    @Test
    public void testContainsAnyIterableNull() {
        Set<String> common = new HashSet<>(Arrays.asList("apple", "grape"));
        assertTrue(!N.containsAny((Iterable<String>) null, common));
    }

    @Test
    public void testContainsAnyIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        Set<String> common = new HashSet<>(Arrays.asList("apple"));
        assertTrue(!N.containsAny(empty, common));
    }

    @Test
    public void testContainsAnyIterator() {
        List<String> words = Arrays.asList("hello", "world", "java", "stream");
        Set<String> keywords = new HashSet<>(Arrays.asList("java", "python", "c++"));
        assertTrue(N.containsAny(words.iterator(), keywords));
    }

    @Test
    public void testContainsAnyIteratorNoMatch() {
        List<String> words = Arrays.asList("hello", "world");
        Set<String> keywords = new HashSet<>(Arrays.asList("java", "python"));
        assertTrue(!N.containsAny(words.iterator(), keywords));
    }

    @Test
    public void testContainsAnyIteratorNull() {
        Set<String> keywords = new HashSet<>(Arrays.asList("java"));
        assertTrue(!N.containsAny((Iterator<String>) null, keywords));
    }

    @Test
    public void testContainsNoneCollection() {
        Set<String> allowedColors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        List<String> invalidColors = Arrays.asList("yellow");
        assertTrue(N.containsNone(allowedColors, invalidColors));
    }

    @Test
    public void testContainsNoneCollectionHasMatch() {
        List<String> userColors = Arrays.asList("red", "yellow");
        List<String> invalidColors = Arrays.asList("yellow");
        assertTrue(!N.containsNone(userColors, invalidColors));
    }

    @Test
    public void testContainsNoneCollectionEmpty() {
        Set<String> allowedColors = new HashSet<>(Arrays.asList("red", "green", "blue"));
        assertTrue(N.containsNone(allowedColors, Collections.emptyList()));
    }

    @Test
    public void testContainsNoneCollectionNull() {
        List<String> invalidColors = Arrays.asList("yellow");
        assertTrue(N.containsNone(null, invalidColors));
    }

    @Test
    public void testContainsNoneVarargs() {
        Set<String> userInput = new HashSet<>(Arrays.asList("safe", "clean", "valid"));
        assertTrue(N.containsNone(userInput, "script", "eval", "exec"));
    }

    @Test
    public void testContainsNoneVarargsHasMatch() {
        List<String> mixed = Arrays.asList("good", "bad", "neutral");
        assertTrue(!N.containsNone(mixed, "bad", "evil"));
    }

    @Test
    public void testContainsNoneVarargsEmpty() {
        Set<String> userInput = new HashSet<>(Arrays.asList("safe", "clean"));
        assertTrue(N.containsNone(userInput));
    }

    @Test
    public void testContainsNoneVarargsNull() {
        assertTrue(N.containsNone(null, "test"));
    }

    @Test
    public void testContainsNoneIterable() {
        List<String> document = Arrays.asList("the", "quick", "brown", "fox");
        Set<String> stopWords = new HashSet<>(Arrays.asList("and", "or", "but", "if"));
        assertTrue(N.containsNone((Iterable<String>) document, stopWords));
    }

    @Test
    public void testContainsNoneIterableHasMatch() {
        List<String> document = Arrays.asList("the", "quick", "brown", "fox");
        Set<String> commonWords = new HashSet<>(Arrays.asList("the", "and", "or"));
        assertTrue(!N.containsNone((Iterable<String>) document, commonWords));
    }

    @Test
    public void testContainsNoneIterableNull() {
        Set<String> stopWords = new HashSet<>(Arrays.asList("and", "or"));
        assertTrue(N.containsNone((Iterable<String>) null, stopWords));
    }

    @Test
    public void testContainsNoneIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        Set<String> stopWords = new HashSet<>(Arrays.asList("and", "or"));
        assertTrue(N.containsNone(empty, stopWords));
    }

    @Test
    public void testContainsNoneIterator() {
        List<String> userInput = Arrays.asList("hello", "world", "test");
        Set<String> forbidden = new HashSet<>(Arrays.asList("admin", "root", "system"));
        assertTrue(N.containsNone(userInput.iterator(), forbidden));
    }

    @Test
    public void testContainsNoneIteratorHasMatch() {
        List<String> userInput = Arrays.asList("hello", "admin", "test");
        Set<String> forbidden = new HashSet<>(Arrays.asList("admin", "root"));
        assertTrue(!N.containsNone(userInput.iterator(), forbidden));
    }

    @Test
    public void testContainsNoneIteratorNull() {
        Set<String> forbidden = new HashSet<>(Arrays.asList("admin"));
        assertTrue(N.containsNone((Iterator<String>) null, forbidden));
    }

    @Test
    public void testFlattenBooleanArray() {
        boolean[][] array = { { true, false }, { true }, { false, false, true } };
        boolean[] result = N.flatten(array);
        assertArrayEquals(new boolean[] { true, false, true, false, false, true }, result);
    }

    @Test
    public void testFlattenBooleanArrayNull() {
        boolean[][] array = null;
        boolean[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenBooleanArrayEmpty() {
        boolean[][] array = {};
        boolean[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenBooleanArrayWithNulls() {
        boolean[][] array = { { true, false }, null, { true } };
        boolean[] result = N.flatten(array);
        assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    @Test
    public void testFlattenCharArray() {
        char[][] array = { { 'a', 'b' }, { 'c' }, { 'd', 'e', 'f' } };
        char[] result = N.flatten(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testFlattenCharArrayNull() {
        char[][] array = null;
        char[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenCharArrayEmpty() {
        char[][] array = {};
        char[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenByteArray() {
        byte[][] array = { { 1, 2 }, { 3 }, { 4, 5 } };
        byte[] result = N.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlattenByteArrayNull() {
        byte[][] array = null;
        byte[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenByteArrayWithNulls() {
        byte[][] array = { { 1, 2 }, null, { 3 } };
        byte[] result = N.flatten(array);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testFlattenShortArray() {
        short[][] array = { { 10, 20 }, { 30 }, { 40, 50 } };
        short[] result = N.flatten(array);
        assertArrayEquals(new short[] { 10, 20, 30, 40, 50 }, result);
    }

    @Test
    public void testFlattenShortArrayNull() {
        short[][] array = null;
        short[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenShortArrayEmpty() {
        short[][] array = {};
        short[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenIntArray() {
        int[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        int[] result = N.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testFlattenIntArrayNull() {
        int[][] array = null;
        int[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenIntArrayEmpty() {
        int[][] array = {};
        int[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenIntArrayWithEmptyArrays() {
        int[][] array = { { 1, 2 }, {}, { 3, 4 } };
        int[] result = N.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testFlattenLongArray() {
        long[][] array = { { 1000L, 2000L }, { 3000L }, { 4000L, 5000L } };
        long[] result = N.flatten(array);
        assertArrayEquals(new long[] { 1000L, 2000L, 3000L, 4000L, 5000L }, result);
    }

    @Test
    public void testFlattenLongArrayNull() {
        long[][] array = null;
        long[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenLongArrayEmpty() {
        long[][] array = {};
        long[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenFloatArray() {
        float[][] array = { { 1.5f, 2.5f }, { 3.5f }, { 4.5f, 5.5f } };
        float[] result = N.flatten(array);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f, 4.5f, 5.5f }, result, 0.001f);
    }

    @Test
    public void testFlattenFloatArrayNull() {
        float[][] array = null;
        float[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenFloatArrayEmpty() {
        float[][] array = {};
        float[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenDoubleArray() {
        double[][] array = { { 1.1, 2.2 }, { 3.3 }, { 4.4, 5.5 } };
        double[] result = N.flatten(array);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, result, 0.001);
    }

    @Test
    public void testFlattenDoubleArrayNull() {
        double[][] array = null;
        double[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenDoubleArrayEmpty() {
        double[][] array = {};
        double[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenObjectArray() {
        String[][] array = { { "a", "b" }, { "c" }, { "d", "e" } };
        String[] result = N.flatten(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, result);
    }

    @Test
    public void testFlattenObjectArrayNull() {
        String[][] array = null;
        String[] result = N.flatten(array);
        assertNull(result);
    }

    @Test
    public void testFlattenObjectArrayEmpty() {
        String[][] array = {};
        String[] result = N.flatten(array);
        assertEquals(0, result.length);
    }

    @Test
    public void testFlattenObjectArrayWithNulls() {
        String[][] array = { { "a", "b" }, null, { "c" } };
        String[] result = N.flatten(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testFlattenIterables() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e", "f"));
        List<String> result = N.flatten(lists);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testFlattenIterablesNull() {
        List<List<String>> lists = null;
        List<String> result = N.flatten(lists);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenIterablesEmpty() {
        List<List<String>> lists = new ArrayList<>();
        List<String> result = N.flatten(lists);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenIterablesWithNulls() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), null, Arrays.asList("c"));
        List<String> result = N.flatten(lists);
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testFlattenIterablesWithSupplier() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        Set<String> result = N.flatten(lists, (size) -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testFlattenIterator() {
        List<Iterator<String>> iters = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator());
        Iterator<String> result = N.flatten(iters.iterator());
        List<String> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), resultList);
    }

    @Test
    public void testFlattenIteratorNull() {
        Iterator<Iterator<String>> iters = null;
        Iterator<String> result = N.flatten(iters);
        assertTrue(!result.hasNext());
    }

    @Test
    public void testFlattenEachElement() {
        List<Object> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d", Arrays.asList("e", "f"));
        List<?> result = N.flattenEachElement(mixed);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testFlattenEachElementNull() {
        List<Object> mixed = null;
        List<?> result = N.flattenEachElement(mixed);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenEachElementEmpty() {
        List<Object> mixed = new ArrayList<>();
        List<?> result = N.flattenEachElement(mixed);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattenEachElementNestedIterables() {
        List<Object> nested = Arrays.asList(Arrays.asList("a", Arrays.asList("b", "c")), "d", Arrays.asList("e"));
        List<?> result = N.flattenEachElement(nested);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testFlattenEachElementWithSupplier() {
        List<Object> mixed = Arrays.asList("a", Arrays.asList("b", "c"), "d");
        Set<?> result = N.flattenEachElement(mixed, () -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testIntersectionBooleanArray() {
        boolean[] a = { true, false, false, true };
        boolean[] b = { false, false, true };
        boolean[] result = N.intersection(a, b);
        assertArrayEquals(new boolean[] { true, false, false }, result);
    }

    @Test
    public void testIntersectionBooleanArrayNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionBooleanArrayEmpty() {
        boolean[] a = {};
        boolean[] b = { true, false };
        boolean[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionCharArray() {
        char[] a = { 'a', 'b', 'b', 'c' };
        char[] b = { 'a', 'b', 'b', 'b', 'd' };
        char[] result = N.intersection(a, b);
        assertArrayEquals(new char[] { 'a', 'b', 'b' }, result);
    }

    @Test
    public void testIntersectionCharArrayNoCommon() {
        char[] a = { 'x', 'y' };
        char[] b = { 'z', 'w' };
        char[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionCharArrayNull() {
        char[] a = null;
        char[] b = { 'a', 'b' };
        char[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionByteArray() {
        byte[] a = { 1, 2, 2, 3, 4 };
        byte[] b = { 1, 2, 2, 2, 5, 6 };
        byte[] result = N.intersection(a, b);
        assertArrayEquals(new byte[] { 1, 2, 2 }, result);
    }

    @Test
    public void testIntersectionByteArrayNoCommon() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };
        byte[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1, 2 };
        byte[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionShortArray() {
        short[] a = { 1, 2, 2, 3, 4 };
        short[] b = { 1, 2, 2, 2, 5, 6 };
        short[] result = N.intersection(a, b);
        assertArrayEquals(new short[] { 1, 2, 2 }, result);
    }

    @Test
    public void testIntersectionShortArrayNull() {
        short[] a = null;
        short[] b = { 1, 2 };
        short[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionIntArray() {
        int[] a = { 1, 2, 2, 3, 4 };
        int[] b = { 1, 2, 2, 2, 5, 6 };
        int[] result = N.intersection(a, b);
        assertArrayEquals(new int[] { 1, 2, 2 }, result);
    }

    @Test
    public void testIntersectionIntArrayNoCommon() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionLongArray() {
        long[] a = { 1L, 2L, 2L, 3L, 4L };
        long[] b = { 1L, 2L, 2L, 2L, 5L, 6L };
        long[] result = N.intersection(a, b);
        assertArrayEquals(new long[] { 1L, 2L, 2L }, result);
    }

    @Test
    public void testIntersectionLongArrayNull() {
        long[] a = null;
        long[] b = { 1L, 2L };
        long[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionFloatArray() {
        float[] a = { 1.5f, 2.5f, 2.5f, 3.5f };
        float[] b = { 1.5f, 2.5f, 2.5f, 2.5f, 4.5f };
        float[] result = N.intersection(a, b);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 2.5f }, result, 0.001f);
    }

    @Test
    public void testIntersectionFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.5f, 2.5f };
        float[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionDoubleArray() {
        double[] a = { 1.1, 2.2, 2.2, 3.3 };
        double[] b = { 1.1, 2.2, 2.2, 2.2, 4.4 };
        double[] result = N.intersection(a, b);
        assertArrayEquals(new double[] { 1.1, 2.2, 2.2 }, result, 0.001);
    }

    @Test
    public void testIntersectionDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1, 2.2 };
        double[] result = N.intersection(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testIntersectionObjectArray() {
        String[] a = { "A", "B", "B", "C", "D" };
        String[] b = { "A", "B", "B", "B", "E", "F" };
        List<String> result = N.intersection(a, b);
        assertEquals(Arrays.asList("A", "B", "B"), result);
    }

    @Test
    public void testIntersectionObjectArrayNull() {
        String[] a = null;
        String[] b = { "A", "B" };
        List<String> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionObjectArrayEmpty() {
        String[] a = {};
        String[] b = { "A", "B" };
        List<String> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollection() {
        List<String> a = Arrays.asList("A", "B", "B", "C", "D");
        List<String> b = Arrays.asList("A", "B", "B", "B", "E", "F");
        List<String> result = N.intersection(a, b);
        assertEquals(Arrays.asList("A", "B", "B"), result);
    }

    @Test
    public void testIntersectionCollectionNoCommon() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(4, 5, 6);
        List<Integer> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollectionNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("A", "B");
        List<String> result = N.intersection(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollectionOfCollections() {
        List<String> a = Arrays.asList("A", "B", "B", "C");
        List<String> b = Arrays.asList("A", "B", "B", "B", "D");
        List<String> c = Arrays.asList("A", "B", "E");
        List<List<String>> collections = Arrays.asList(a, b, c);
        List<String> result = N.intersection(collections);
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testIntersectionCollectionOfCollectionsEmpty() {
        List<List<String>> collections = new ArrayList<>();
        List<String> result = N.intersection(collections);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionCollectionOfCollectionsSingle() {
        List<List<String>> collections = Arrays.asList(Arrays.asList("A", "B", "C"));
        List<String> result = N.intersection(collections);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testIntersectionCollectionOfCollectionsWithEmpty() {
        List<String> a = Arrays.asList("A", "B");
        List<String> b = new ArrayList<>();
        List<List<String>> collections = Arrays.asList(a, b);
        List<String> result = N.intersection(collections);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceBooleanArray() {
        boolean[] a = { true, true, false };
        boolean[] b = { true, false };
        boolean[] result = N.difference(a, b);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testDifferenceBooleanArrayNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceBooleanArraySecondNull() {
        boolean[] a = { true, false };
        boolean[] b = null;
        boolean[] result = N.difference(a, b);
        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testDifferenceCharArray() {
        char[] a = { 'a', 'b', 'b', 'c', 'd' };
        char[] b = { 'a', 'b', 'e' };
        char[] result = N.difference(a, b);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void testDifferenceCharArrayNull() {
        char[] a = null;
        char[] b = { 'a', 'b' };
        char[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceCharArraySecondNull() {
        char[] a = { 'a', 'b' };
        char[] b = null;
        char[] result = N.difference(a, b);
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testDifferenceByteArray() {
        byte[] a = { 1, 2, 2, 3, 4 };
        byte[] b = { 2, 5 };
        byte[] result = N.difference(a, b);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testDifferenceByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1, 2 };
        byte[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceShortArray() {
        short[] a = { 1, 2, 2, 3, 4 };
        short[] b = { 2, 5 };
        short[] result = N.difference(a, b);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testDifferenceShortArrayNull() {
        short[] a = null;
        short[] b = { 1, 2 };
        short[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceIntArray() {
        int[] a = { 1, 2, 2, 3, 4 };
        int[] b = { 2, 5 };
        int[] result = N.difference(a, b);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testDifferenceIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceIntArraySecondNull() {
        int[] a = { 1, 2, 3 };
        int[] b = null;
        int[] result = N.difference(a, b);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testDifferenceLongArray() {
        long[] a = { 1L, 2L, 2L, 3L, 4L };
        long[] b = { 2L, 5L };
        long[] result = N.difference(a, b);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testDifferenceLongArrayNull() {
        long[] a = null;
        long[] b = { 1L, 2L };
        long[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceFloatArray() {
        float[] a = { 1.5f, 2.5f, 2.5f, 3.5f };
        float[] b = { 2.5f, 4.5f };
        float[] result = N.difference(a, b);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f }, result, 0.001f);
    }

    @Test
    public void testDifferenceFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.5f, 2.5f };
        float[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceDoubleArray() {
        double[] a = { 1.1, 2.2, 2.2, 3.3 };
        double[] b = { 2.2, 4.4 };
        double[] result = N.difference(a, b);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, result, 0.001);
    }

    @Test
    public void testDifferenceDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1, 2.2 };
        double[] result = N.difference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testDifferenceObjectArray() {
        String[] a = { "A", "B", "B", "C", "D" };
        String[] b = { "A", "B", "E" };
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("B", "C", "D"), result);
    }

    @Test
    public void testDifferenceObjectArrayNull() {
        String[] a = null;
        String[] b = { "A", "B" };
        List<String> result = N.difference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceObjectArraySecondNull() {
        String[] a = { "A", "B", "C" };
        String[] b = null;
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testDifferenceCollection() {
        List<String> a = Arrays.asList("A", "B", "B", "C", "D");
        List<String> b = Arrays.asList("A", "B", "E");
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("B", "C", "D"), result);
    }

    @Test
    public void testDifferenceCollectionAllRemoved() {
        List<Integer> a = Arrays.asList(1, 2, 2, 3);
        List<Integer> b = Arrays.asList(2, 2, 2, 4);
        List<Integer> result = N.difference(a, b);
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testDifferenceCollectionNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("A", "B");
        List<String> result = N.difference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceCollectionSecondNull() {
        List<String> a = Arrays.asList("A", "B", "C");
        List<String> b = null;
        List<String> result = N.difference(a, b);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testSymmetricDifferenceBooleanArray() {
        boolean[] a = { true, true, false };
        boolean[] b = { true, false };
        boolean[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testSymmetricDifferenceBooleanArrayNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testSymmetricDifferenceBooleanArrayBothNull() {
        boolean[] a = null;
        boolean[] b = null;
        boolean[] result = N.symmetricDifference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testSymmetricDifferenceCharArray() {
        char[] a = { 'a', 'b', 'b', 'c' };
        char[] b = { 'b', 'd', 'a' };
        char[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void testSymmetricDifferenceCharArrayNull() {
        char[] a = null;
        char[] b = { 'a', 'b' };
        char[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testSymmetricDifferenceByteArray() {
        byte[] a = { 0, 1, 2, 2, 3 };
        byte[] b = { 2, 5, 1 };
        byte[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new byte[] { 0, 2, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifferenceByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1, 2 };
        byte[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testSymmetricDifferenceShortArray() {
        short[] a = { 1, 2, 2, 3, 4 };
        short[] b = { 2, 5, 1 };
        short[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new short[] { 2, 3, 4, 5 }, result);
    }

    @Test
    public void testSymmetricDifferenceShortArrayNull() {
        short[] a = null;
        short[] b = { 1, 2 };
        short[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testSymmetricDifferenceIntArray() {
        int[] a = { 0, 1, 2, 2, 3 };
        int[] b = { 2, 5, 1 };
        int[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new int[] { 0, 2, 3, 5 }, result);
    }

    @Test
    public void testSymmetricDifferenceIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testSymmetricDifferenceIntArrayBothNull() {
        int[] a = null;
        int[] b = null;
        int[] result = N.symmetricDifference(a, b);
        assertEquals(0, result.length);
    }

    @Test
    public void testSymmetricDifferenceLongArray() {
        long[] a = { 1L, 2L, 2L, 3L, 4L };
        long[] b = { 2L, 5L, 1L };
        long[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new long[] { 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testSymmetricDifferenceLongArrayNull() {
        long[] a = null;
        long[] b = { 1L, 2L };
        long[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    @Test
    public void testSymmetricDifferenceFloatArray() {
        float[] a = { 1.5f, 2.5f, 2.5f, 3.5f };
        float[] b = { 2.5f, 4.5f, 1.5f };
        float[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new float[] { 2.5f, 3.5f, 4.5f }, result, 0.001f);
    }

    @Test
    public void testSymmetricDifferenceFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.5f, 2.5f };
        float[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new float[] { 1.5f, 2.5f }, result, 0.001f);
    }

    @Test
    public void testSymmetricDifferenceDoubleArray() {
        double[] a = { 1.1, 2.2, 2.2, 3.3 };
        double[] b = { 2.2, 4.4, 1.1 };
        double[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new double[] { 2.2, 3.3, 4.4 }, result, 0.001);
    }

    @Test
    public void testSymmetricDifferenceDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1, 2.2 };
        double[] result = N.symmetricDifference(a, b);
        assertArrayEquals(new double[] { 1.1, 2.2 }, result, 0.001);
    }

    @Test
    public void testSymmetricDifferenceObjectArray() {
        String[] a = { "A", "B", "B", "C", "D" };
        String[] b = { "B", "E", "A" };
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("B", "C", "D", "E"), result);
    }

    @Test
    public void testSymmetricDifferenceObjectArrayNull() {
        String[] a = null;
        String[] b = { "A", "B" };
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testSymmetricDifferenceObjectArrayBothNull() {
        String[] a = null;
        String[] b = null;
        List<String> result = N.symmetricDifference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceCollection() {
        List<String> a = Arrays.asList("A", "B", "B", "C", "D");
        List<String> b = Arrays.asList("B", "E", "A");
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("B", "C", "D", "E"), result);
    }

    @Test
    public void testSymmetricDifferenceCollectionUnequal() {
        List<Integer> a = Arrays.asList(1, 2, 2);
        List<Integer> b = Arrays.asList(2, 2, 2);
        List<Integer> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testSymmetricDifferenceCollectionNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("A", "B");
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void testSymmetricDifferenceCollectionBothNull() {
        List<String> a = null;
        List<String> b = null;
        List<String> result = N.symmetricDifference(a, b);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceCollectionSecondNull() {
        List<String> a = Arrays.asList("A", "B", "C");
        List<String> b = null;
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testReplaceIfBooleanArray() {
        boolean[] a = { true, false, true, false, true };
        int count = N.replaceIf(a, x -> x, false);
        assertEquals(3, count);
        assertArrayEquals(new boolean[] { false, false, false, false, false }, a);
    }

    @Test
    public void testReplaceIfBooleanArrayNoPredicate() {
        boolean[] a = { true, false, true };
        int count = N.replaceIf(a, x -> false, true);
        assertEquals(0, count);
        assertArrayEquals(new boolean[] { true, false, true }, a);
    }

    @Test
    public void testReplaceIfBooleanArrayNull() {
        boolean[] a = null;
        int count = N.replaceIf(a, x -> true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfBooleanArrayEmpty() {
        boolean[] a = {};
        int count = N.replaceIf(a, x -> true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfCharArray() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        int count = N.replaceIf(a, x -> x < 'c', 'x');
        assertEquals(2, count);
        assertArrayEquals(new char[] { 'x', 'x', 'c', 'd', 'e' }, a);
    }

    @Test
    public void testReplaceIfCharArrayNull() {
        char[] a = null;
        int count = N.replaceIf(a, x -> true, 'x');
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfByteArray() {
        byte[] a = { 1, 2, 3, 4, 5 };
        int count = N.replaceIf(a, x -> x % 2 == 0, (byte) 0);
        assertEquals(2, count);
        assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, a);
    }

    @Test
    public void testReplaceIfByteArrayNull() {
        byte[] a = null;
        int count = N.replaceIf(a, x -> true, (byte) 0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfShortArray() {
        short[] a = { 10, 20, 30, 40, 50 };
        int count = N.replaceIf(a, x -> x > 25, (short) 99);
        assertEquals(3, count);
        assertArrayEquals(new short[] { 10, 20, 99, 99, 99 }, a);
    }

    @Test
    public void testReplaceIfShortArrayNull() {
        short[] a = null;
        int count = N.replaceIf(a, x -> true, (short) 0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfIntArray() {
        int[] a = { 1, 2, 3, 4, 5, 6, 7, 8 };
        int count = N.replaceIf(a, x -> x % 3 == 0, 0);
        assertEquals(2, count);
        assertArrayEquals(new int[] { 1, 2, 0, 4, 5, 0, 7, 8 }, a);
    }

    @Test
    public void testReplaceIfIntArrayNull() {
        int[] a = null;
        int count = N.replaceIf(a, x -> true, 0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfLongArray() {
        long[] a = { 100L, 200L, 300L, 400L };
        int count = N.replaceIf(a, x -> x >= 300L, 0L);
        assertEquals(2, count);
        assertArrayEquals(new long[] { 100L, 200L, 0L, 0L }, a);
    }

    @Test
    public void testReplaceIfLongArrayNull() {
        long[] a = null;
        int count = N.replaceIf(a, x -> true, 0L);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfFloatArray() {
        float[] a = { 1.5f, 2.5f, 3.5f, 4.5f };
        int count = N.replaceIf(a, x -> x > 3.0f, 0.0f);
        assertEquals(2, count);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 0.0f, 0.0f }, a);
    }

    @Test
    public void testReplaceIfFloatArrayNull() {
        float[] a = null;
        int count = N.replaceIf(a, x -> true, 0.0f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfDoubleArray() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        int count = N.replaceIf(a, x -> x < 3.0, 99.0);
        assertEquals(2, count);
        assertArrayEquals(new double[] { 99.0, 99.0, 3.0, 4.0, 5.0 }, a);
    }

    @Test
    public void testReplaceIfDoubleArrayNull() {
        double[] a = null;
        int count = N.replaceIf(a, x -> true, 0.0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfObjectArray() {
        String[] a = { "apple", "banana", "cherry", "date" };
        int count = N.replaceIf(a, x -> x.startsWith("b") || x.startsWith("c"), "REPLACED");
        assertEquals(2, count);
        assertArrayEquals(new String[] { "apple", "REPLACED", "REPLACED", "date" }, a);
    }

    @Test
    public void testReplaceIfObjectArrayNull() {
        String[] a = null;
        int count = N.replaceIf(a, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfObjectArrayEmpty() {
        String[] a = {};
        int count = N.replaceIf(a, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfList() {
        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "cherry", "date"));
        int count = N.replaceIf(list, x -> x.length() > 5, "LONG");
        assertEquals(2, count);
        assertEquals(Arrays.asList("apple", "LONG", "LONG", "date"), list);
    }

    @Test
    public void testReplaceIfListNull() {
        List<String> list = null;
        int count = N.replaceIf(list, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfListEmpty() {
        List<String> list = new ArrayList<>();
        int count = N.replaceIf(list, x -> true, "REPLACED");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllBooleanArray() {
        boolean[] a = { true, false, true, false, true };
        int count = N.replaceAll(a, true, false);
        assertEquals(3, count);
        assertArrayEquals(new boolean[] { false, false, false, false, false }, a);
    }

    @Test
    public void testReplaceAllBooleanArrayNoMatch() {
        boolean[] a = { true, true, true };
        int count = N.replaceAll(a, false, true);
        assertEquals(0, count);
        assertArrayEquals(new boolean[] { true, true, true }, a);
    }

    @Test
    public void testReplaceAllBooleanArrayNull() {
        boolean[] a = null;
        int count = N.replaceAll(a, true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllCharArray() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        int count = N.replaceAll(a, 'a', 'x');
        assertEquals(3, count);
        assertArrayEquals(new char[] { 'x', 'b', 'x', 'c', 'x' }, a);
    }

    @Test
    public void testReplaceAllCharArrayNull() {
        char[] a = null;
        int count = N.replaceAll(a, 'a', 'x');
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllByteArray() {
        byte[] a = { 1, 2, 1, 3, 1 };
        int count = N.replaceAll(a, (byte) 1, (byte) 9);
        assertEquals(3, count);
        assertArrayEquals(new byte[] { 9, 2, 9, 3, 9 }, a);
    }

    @Test
    public void testReplaceAllByteArrayNull() {
        byte[] a = null;
        int count = N.replaceAll(a, (byte) 1, (byte) 9);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllShortArray() {
        short[] a = { 10, 20, 10, 30, 10 };
        int count = N.replaceAll(a, (short) 10, (short) 99);
        assertEquals(3, count);
        assertArrayEquals(new short[] { 99, 20, 99, 30, 99 }, a);
    }

    @Test
    public void testReplaceAllShortArrayNull() {
        short[] a = null;
        int count = N.replaceAll(a, (short) 10, (short) 99);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllIntArray() {
        int[] a = { 1, 2, 3, 2, 4, 2, 5 };
        int count = N.replaceAll(a, 2, 99);
        assertEquals(3, count);
        assertArrayEquals(new int[] { 1, 99, 3, 99, 4, 99, 5 }, a);
    }

    @Test
    public void testReplaceAllIntArrayNull() {
        int[] a = null;
        int count = N.replaceAll(a, 2, 99);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllLongArray() {
        long[] a = { 100L, 200L, 100L, 300L };
        int count = N.replaceAll(a, 100L, 999L);
        assertEquals(2, count);
        assertArrayEquals(new long[] { 999L, 200L, 999L, 300L }, a);
    }

    @Test
    public void testReplaceAllLongArrayNull() {
        long[] a = null;
        int count = N.replaceAll(a, 100L, 999L);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllFloatArray() {
        float[] a = { 1.5f, 2.5f, 1.5f, 3.5f };
        int count = N.replaceAll(a, 1.5f, 9.9f);
        assertEquals(2, count);
        assertArrayEquals(new float[] { 9.9f, 2.5f, 9.9f, 3.5f }, a);
    }

    @Test
    public void testReplaceAllFloatArrayNull() {
        float[] a = null;
        int count = N.replaceAll(a, 1.5f, 9.9f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllDoubleArray() {
        double[] a = { 1.0, 2.0, 1.0, 3.0 };
        int count = N.replaceAll(a, 1.0, 99.0);
        assertEquals(2, count);
        assertArrayEquals(new double[] { 99.0, 2.0, 99.0, 3.0 }, a);
    }

    @Test
    public void testReplaceAllDoubleArrayNull() {
        double[] a = null;
        int count = N.replaceAll(a, 1.0, 99.0);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllObjectArray() {
        String[] a = { "apple", "banana", "apple", "cherry" };
        int count = N.replaceAll(a, "apple", "FRUIT");
        assertEquals(2, count);
        assertArrayEquals(new String[] { "FRUIT", "banana", "FRUIT", "cherry" }, a);
    }

    @Test
    public void testReplaceAllObjectArrayNull() {
        String[] a = null;
        int count = N.replaceAll(a, "apple", "FRUIT");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "apple", "cherry"));
        int count = N.replaceAll(list, "apple", "FRUIT");
        assertEquals(2, count);
        assertEquals(Arrays.asList("FRUIT", "banana", "FRUIT", "cherry"), list);
    }

    @Test
    public void testReplaceAllListNull() {
        List<String> list = null;
        int count = N.replaceAll(list, "apple", "FRUIT");
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAllBooleanArrayOperator() {
        boolean[] a = { true, false, true, false };
        N.replaceAll(a, x -> !x);
        assertArrayEquals(new boolean[] { false, true, false, true }, a);
    }

    @Test
    public void testReplaceAllBooleanArrayOperatorNull() {
        boolean[] a = null;
        N.replaceAll(a, x -> !x);
    }

    @Test
    public void testReplaceAllCharArrayOperator() {
        char[] a = { 'a', 'b', 'c' };
        N.replaceAll(a, x -> (char) (x + 1));
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, a);
    }

    @Test
    public void testReplaceAllCharArrayOperatorNull() {
        char[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllByteArrayOperator() {
        byte[] a = { 1, 2, 3 };
        N.replaceAll(a, x -> (byte) (x * 2));
        assertArrayEquals(new byte[] { 2, 4, 6 }, a);
    }

    @Test
    public void testReplaceAllByteArrayOperatorNull() {
        byte[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllShortArrayOperator() {
        short[] a = { 10, 20, 30 };
        N.replaceAll(a, x -> (short) (x + 5));
        assertArrayEquals(new short[] { 15, 25, 35 }, a);
    }

    @Test
    public void testReplaceAllShortArrayOperatorNull() {
        short[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllIntArrayOperator() {
        int[] a = { 1, 2, 3, 4 };
        N.replaceAll(a, x -> x * x);
        assertArrayEquals(new int[] { 1, 4, 9, 16 }, a);
    }

    @Test
    public void testReplaceAllIntArrayOperatorNull() {
        int[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllLongArrayOperator() {
        long[] a = { 100L, 200L, 300L };
        N.replaceAll(a, x -> x / 10);
        assertArrayEquals(new long[] { 10L, 20L, 30L }, a);
    }

    @Test
    public void testReplaceAllLongArrayOperatorNull() {
        long[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllFloatArrayOperator() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        N.replaceAll(a, x -> x * 2.0f);
        assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, a);
    }

    @Test
    public void testReplaceAllFloatArrayOperatorNull() {
        float[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllDoubleArrayOperator() {
        double[] a = { 1.0, 2.0, 3.0 };
        N.replaceAll(a, x -> x + 10.0);
        assertArrayEquals(new double[] { 11.0, 12.0, 13.0 }, a);
    }

    @Test
    public void testReplaceAllDoubleArrayOperatorNull() {
        double[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllObjectArrayOperator() {
        String[] a = { "hello", "world", "test" };
        N.replaceAll(a, String::toUpperCase);
        assertArrayEquals(new String[] { "HELLO", "WORLD", "TEST" }, a);
    }

    @Test
    public void testReplaceAllObjectArrayOperatorNull() {
        String[] a = null;
        N.replaceAll(a, x -> x);
    }

    @Test
    public void testReplaceAllListOperator() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        N.replaceAll(list, x -> x * 10);
        assertEquals(Arrays.asList(10, 20, 30, 40), list);
    }

    @Test
    public void testReplaceAllListOperatorNull() {
        List<String> list = null;
        N.replaceAll(list, x -> x);
    }

    @Test
    public void testSetAllBooleanArray() {
        boolean[] a = new boolean[5];
        N.setAll(a, i -> i % 2 == 0);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, a);
    }

    @Test
    public void testSetAllBooleanArrayNull() {
        boolean[] a = null;
        N.setAll(a, i -> true);
    }

    @Test
    public void testSetAllCharArray() {
        char[] a = new char[3];
        N.setAll(a, i -> (char) ('a' + i));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, a);
    }

    @Test
    public void testSetAllCharArrayNull() {
        char[] a = null;
        N.setAll(a, i -> 'x');
    }

    @Test
    public void testSetAllByteArray() {
        byte[] a = new byte[4];
        N.setAll(a, i -> (byte) (i * 10));
        assertArrayEquals(new byte[] { 0, 10, 20, 30 }, a);
    }

    @Test
    public void testSetAllByteArrayNull() {
        byte[] a = null;
        N.setAll(a, i -> (byte) i);
    }

    @Test
    public void testSetAllShortArray() {
        short[] a = new short[3];
        N.setAll(a, i -> (short) (i + 100));
        assertArrayEquals(new short[] { 100, 101, 102 }, a);
    }

    @Test
    public void testSetAllShortArrayNull() {
        short[] a = null;
        N.setAll(a, i -> (short) i);
    }

    @Test
    public void testSetAllIntArray() {
        int[] a = new int[5];
        N.setAll(a, i -> i * i);
        assertArrayEquals(new int[] { 0, 1, 4, 9, 16 }, a);
    }

    @Test
    public void testSetAllIntArrayNull() {
        int[] a = null;
        N.setAll(a, i -> i);
    }

    @Test
    public void testSetAllLongArray() {
        long[] a = new long[4];
        N.setAll(a, i -> (long) i * 1000);
        assertArrayEquals(new long[] { 0L, 1000L, 2000L, 3000L }, a);
    }

    @Test
    public void testSetAllLongArrayNull() {
        long[] a = null;
        N.setAll(a, i -> (long) i);
    }

    @Test
    public void testSetAllFloatArray() {
        float[] a = new float[3];
        N.setAll(a, i -> i * 1.5f);
        assertArrayEquals(new float[] { 0.0f, 1.5f, 3.0f }, a);
    }

    @Test
    public void testSetAllFloatArrayNull() {
        float[] a = null;
        N.setAll(a, i -> (float) i);
    }

    @Test
    public void testSetAllDoubleArray() {
        double[] a = new double[3];
        N.setAll(a, i -> i * 2.5);
        assertArrayEquals(new double[] { 0.0, 2.5, 5.0 }, a);
    }

    @Test
    public void testSetAllDoubleArrayNull() {
        double[] a = null;
        N.setAll(a, i -> (double) i);
    }

    @Test
    public void testSetAllObjectArray() {
        String[] a = new String[3];
        N.setAll(a, i -> "Item" + i);
        assertArrayEquals(new String[] { "Item0", "Item1", "Item2" }, a);
    }

    @Test
    public void testSetAllObjectArrayNull() {
        String[] a = null;
        N.setAll(a, i -> "Item" + i);
    }

    @Test
    public void testSetAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("", "", ""));
        N.setAll(list, i -> "Value" + i);
        assertEquals(Arrays.asList("Value0", "Value1", "Value2"), list);
    }

    @Test
    public void testSetAllListNull() {
        List<String> list = null;
        N.setAll(list, i -> "Value" + i);
    }

    @Test
    public void testCopyThenSetAllObjectArray() {
        String[] a = { "a", "b", "c" };
        String[] result = N.copyThenSetAll(a, i -> "Item" + i);
        assertArrayEquals(new String[] { "Item0", "Item1", "Item2" }, result);
        assertArrayEquals(new String[] { "a", "b", "c" }, a);
    }

    @Test
    public void testCopyThenSetAllObjectArrayNull() {
        String[] a = null;
        String[] result = N.copyThenSetAll(a, i -> "Item" + i);
        assertEquals(null, result);
    }

    @Test
    public void testCopyThenSetAllObjectArrayEmpty() {
        String[] a = {};
        String[] result = N.copyThenSetAll(a, i -> "Item" + i);
        assertEquals(0, result.length);
    }

    @Test
    public void testCopyThenReplaceAllObjectArray() {
        String[] a = { "hello", "world", "test" };
        String[] result = N.copyThenReplaceAll(a, String::toUpperCase);
        assertArrayEquals(new String[] { "HELLO", "WORLD", "TEST" }, result);
        assertArrayEquals(new String[] { "hello", "world", "test" }, a);
    }

    @Test
    public void testCopyThenReplaceAllObjectArrayNull() {
        String[] a = null;
        String[] result = N.copyThenReplaceAll(a, String::toUpperCase);
        assertEquals(null, result);
    }

    @Test
    public void testCopyThenReplaceAllObjectArrayEmpty() {
        String[] a = {};
        String[] result = N.copyThenReplaceAll(a, x -> x);
        assertEquals(0, result.length);
    }

    @Test
    public void testAddBooleanArray() {
        boolean[] a = { true, false };
        boolean[] result = N.add(a, true);
        assertArrayEquals(new boolean[] { true, false, true }, result);
        assertArrayEquals(new boolean[] { true, false }, a);
    }

    @Test
    public void testAddBooleanArrayNull() {
        boolean[] a = null;
        boolean[] result = N.add(a, true);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testAddBooleanArrayEmpty() {
        boolean[] a = {};
        boolean[] result = N.add(a, false);
        assertArrayEquals(new boolean[] { false }, result);
    }

    @Test
    public void testAddCharArray() {
        char[] a = { 'a', 'b' };
        char[] result = N.add(a, 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertArrayEquals(new char[] { 'a', 'b' }, a);
    }

    @Test
    public void testAddCharArrayNull() {
        char[] a = null;
        char[] result = N.add(a, 'x');
        assertArrayEquals(new char[] { 'x' }, result);
    }

    @Test
    public void testAddByteArray() {
        byte[] a = { 1, 2 };
        byte[] result = N.add(a, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
        assertArrayEquals(new byte[] { 1, 2 }, a);
    }

    @Test
    public void testAddByteArrayNull() {
        byte[] a = null;
        byte[] result = N.add(a, (byte) 5);
        assertArrayEquals(new byte[] { 5 }, result);
    }

    @Test
    public void testAddShortArray() {
        short[] a = { 10, 20 };
        short[] result = N.add(a, (short) 30);
        assertArrayEquals(new short[] { 10, 20, 30 }, result);
        assertArrayEquals(new short[] { 10, 20 }, a);
    }

    @Test
    public void testAddShortArrayNull() {
        short[] a = null;
        short[] result = N.add(a, (short) 50);
        assertArrayEquals(new short[] { 50 }, result);
    }

    @Test
    public void testAddIntArray() {
        int[] a = { 1, 2, 3 };
        int[] result = N.add(a, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new int[] { 1, 2, 3 }, a);
    }

    @Test
    public void testAddIntArrayNull() {
        int[] a = null;
        int[] result = N.add(a, 99);
        assertArrayEquals(new int[] { 99 }, result);
    }

    @Test
    public void testAddLongArray() {
        long[] a = { 100L, 200L };
        long[] result = N.add(a, 300L);
        assertArrayEquals(new long[] { 100L, 200L, 300L }, result);
        assertArrayEquals(new long[] { 100L, 200L }, a);
    }

    @Test
    public void testAddLongArrayNull() {
        long[] a = null;
        long[] result = N.add(a, 999L);
        assertArrayEquals(new long[] { 999L }, result);
    }

    @Test
    public void testAddFloatArray() {
        float[] a = { 1.5f, 2.5f };
        float[] result = N.add(a, 3.5f);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f }, result);
        assertArrayEquals(new float[] { 1.5f, 2.5f }, a);
    }

    @Test
    public void testAddFloatArrayNull() {
        float[] a = null;
        float[] result = N.add(a, 9.9f);
        assertArrayEquals(new float[] { 9.9f }, result);
    }

    @Test
    public void testAddDoubleArray() {
        double[] a = { 1.0, 2.0 };
        double[] result = N.add(a, 3.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result);
        assertArrayEquals(new double[] { 1.0, 2.0 }, a);
    }

    @Test
    public void testAddDoubleArrayNull() {
        double[] a = null;
        double[] result = N.add(a, 99.9);
        assertArrayEquals(new double[] { 99.9 }, result);
    }

    @Test
    public void testAddStringArray() {
        String[] a = { "hello", "world" };
        String[] result = N.add(a, "test");
        assertArrayEquals(new String[] { "hello", "world", "test" }, result);
        assertArrayEquals(new String[] { "hello", "world" }, a);
    }

    @Test
    public void testAddStringArrayNull() {
        String[] a = null;
        String[] result = N.add(a, "new");
        assertArrayEquals(new String[] { "new" }, result);
    }

    @Test
    public void testAddObjectArray() {
        Integer[] a = { 1, 2, 3 };
        Integer[] result = N.add(a, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, a);
    }

    @Test
    public void testAddAllBooleanArray() {
        boolean[] a = { true, false };
        boolean[] result = N.addAll(a, true, true);
        assertArrayEquals(new boolean[] { true, false, true, true }, result);
        assertArrayEquals(new boolean[] { true, false }, a);
    }

    @Test
    public void testAddAllBooleanArrayNull() {
        boolean[] a = null;
        boolean[] result = N.addAll(a, true, false);
        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testAddAllBooleanArrayEmpty() {
        boolean[] a = { true };
        boolean[] result = N.addAll(a);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testAddAllCharArray() {
        char[] a = { 'a', 'b' };
        char[] result = N.addAll(a, 'c', 'd');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
        assertArrayEquals(new char[] { 'a', 'b' }, a);
    }

    @Test
    public void testAddAllCharArrayNull() {
        char[] a = null;
        char[] result = N.addAll(a, 'x', 'y');
        assertArrayEquals(new char[] { 'x', 'y' }, result);
    }

    @Test
    public void testAddAllByteArray() {
        byte[] a = { 1, 2 };
        byte[] result = N.addAll(a, (byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new byte[] { 1, 2 }, a);
    }

    @Test
    public void testAddAllByteArrayNull() {
        byte[] a = null;
        byte[] result = N.addAll(a, (byte) 5, (byte) 6);
        assertArrayEquals(new byte[] { 5, 6 }, result);
    }

    @Test
    public void testAddAllShortArray() {
        short[] a = { 10, 20 };
        short[] result = N.addAll(a, (short) 30, (short) 40);
        assertArrayEquals(new short[] { 10, 20, 30, 40 }, result);
        assertArrayEquals(new short[] { 10, 20 }, a);
    }

    @Test
    public void testAddAllShortArrayNull() {
        short[] a = null;
        short[] result = N.addAll(a, (short) 50, (short) 60);
        assertArrayEquals(new short[] { 50, 60 }, result);
    }

    @Test
    public void testAddAllIntArray() {
        int[] a = { 1, 2 };
        int[] result = N.addAll(a, 3, 4, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
        assertArrayEquals(new int[] { 1, 2 }, a);
    }

    @Test
    public void testAddAllIntArrayNull() {
        int[] a = null;
        int[] result = N.addAll(a, 99, 100);
        assertArrayEquals(new int[] { 99, 100 }, result);
    }

    @Test
    public void testAddAllLongArray() {
        long[] a = { 100L, 200L };
        long[] result = N.addAll(a, 300L, 400L);
        assertArrayEquals(new long[] { 100L, 200L, 300L, 400L }, result);
        assertArrayEquals(new long[] { 100L, 200L }, a);
    }

    @Test
    public void testAddAllLongArrayNull() {
        long[] a = null;
        long[] result = N.addAll(a, 999L);
        assertArrayEquals(new long[] { 999L }, result);
    }

    @Test
    public void testAddAllFloatArray() {
        float[] a = { 1.5f, 2.5f };
        float[] result = N.addAll(a, 3.5f, 4.5f);
        assertArrayEquals(new float[] { 1.5f, 2.5f, 3.5f, 4.5f }, result);
        assertArrayEquals(new float[] { 1.5f, 2.5f }, a);
    }

    @Test
    public void testAddAllFloatArrayNull() {
        float[] a = null;
        float[] result = N.addAll(a, 9.9f);
        assertArrayEquals(new float[] { 9.9f }, result);
    }

    @Test
    public void testAddAllDoubleArray() {
        double[] a = { 1.0, 2.0 };
        double[] result = N.addAll(a, 3.0, 4.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result);
        assertArrayEquals(new double[] { 1.0, 2.0 }, a);
    }

    @Test
    public void testAddAllDoubleArrayNull() {
        double[] a = null;
        double[] result = N.addAll(a, 99.9);
        assertArrayEquals(new double[] { 99.9 }, result);
    }

    @Test
    public void testAddAllStringArray() {
        String[] a = { "hello", "world" };
        String[] result = N.addAll(a, "foo", "bar");
        assertArrayEquals(new String[] { "hello", "world", "foo", "bar" }, result);
        assertArrayEquals(new String[] { "hello", "world" }, a);
    }

    @Test
    public void testAddAllStringArrayNull() {
        String[] a = null;
        String[] result = N.addAll(a, "new");
        assertArrayEquals(new String[] { "new" }, result);
    }

    @Test
    public void testAddAllObjectArray() {
        Integer[] a = { 1, 2 };
        Integer[] result = N.addAll(a, 3, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, result);
        assertArrayEquals(new Integer[] { 1, 2 }, a);
    }

    @Test
    public void testAddAllCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        boolean changed = N.addAll(list, "c", "d");
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testAddAllCollectionNoElements() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        boolean changed = N.addAll(list);
        assertTrue(!changed);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testAddAllCollectionIterable() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterable<String> toAdd = Arrays.asList("c", "d");
        boolean changed = N.addAll(list, toAdd);
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testAddAllCollectionIterableEmpty() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterable<String> toAdd = new ArrayList<>();
        boolean changed = N.addAll(list, toAdd);
        assertTrue(!changed);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testAddAllCollectionIterator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterator<String> toAdd = Arrays.asList("c", "d").iterator();
        boolean changed = N.addAll(list, toAdd);
        assertTrue(changed);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testAddAllCollectionIteratorEmpty() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterator<String> toAdd = Collections.emptyIterator();
        boolean changed = N.addAll(list, toAdd);
        assertTrue(!changed);
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testSliceArray() {
        String[] words = { "a", "b", "c", "d", "e" };
        ImmutableList<String> slice = CommonUtil.slice(words, 1, 4);

        assertEquals(3, slice.size());
        assertEquals(Arrays.asList("b", "c", "d"), slice);
    }

    @Test
    public void testSliceArrayFullRange() {
        Integer[] numbers = { 1, 2, 3, 4, 5 };
        ImmutableList<Integer> slice = CommonUtil.slice(numbers, 0, 5);

        assertEquals(5, slice.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), slice);
    }

    @Test
    public void testSliceArrayEmpty() {
        String[] empty = {};
        ImmutableList<String> slice = CommonUtil.slice(empty, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceArrayNull() {
        String[] nullArray = null;
        ImmutableList<String> slice = CommonUtil.slice(nullArray, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceArraySingleElement() {
        Integer[] single = { 42 };
        ImmutableList<Integer> slice = CommonUtil.slice(single, 0, 1);

        assertEquals(1, slice.size());
        assertEquals(42, slice.get(0));
    }

    @Test
    public void testSliceArrayInvalidIndices() {
        String[] words = { "a", "b", "c" };
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, 2, 1));
    }

    @Test
    public void testSliceList() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        ImmutableList<String> slice = CommonUtil.slice(words, 1, 4);

        assertEquals(3, slice.size());
        assertEquals(Arrays.asList("b", "c", "d"), slice);
    }

    @Test
    public void testSliceListFullRange() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        ImmutableList<Integer> slice = CommonUtil.slice(numbers, 0, 5);

        assertEquals(5, slice.size());
    }

    @Test
    public void testSliceListEmpty() {
        List<String> empty = new ArrayList<>();
        ImmutableList<String> slice = CommonUtil.slice(empty, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceListNull() {
        List<String> nullList = null;
        ImmutableList<String> slice = CommonUtil.slice(nullList, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceListInvalidIndices() {
        List<String> words = Arrays.asList("a", "b", "c");
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, 2, 1));
    }

    @Test
    public void testSliceCollection() {
        Set<String> words = new HashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        ImmutableCollection<String> slice = CommonUtil.slice(words, 0, 3);

        assertEquals(3, slice.size());
    }

    @Test
    public void testSliceCollectionList() {
        Collection<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        ImmutableCollection<Integer> slice = CommonUtil.slice(numbers, 1, 4);

        assertEquals(3, slice.size());
    }

    @Test
    public void testSliceCollectionEmpty() {
        Collection<String> empty = new ArrayList<>();
        ImmutableCollection<String> slice = CommonUtil.slice(empty, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceCollectionNull() {
        Collection<String> nullColl = null;
        ImmutableCollection<String> slice = CommonUtil.slice(nullColl, 0, 0);

        assertTrue(slice.isEmpty());
    }

    @Test
    public void testSliceCollectionInvalidIndices() {
        Collection<String> words = Arrays.asList("a", "b", "c");
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(words, 0, 5));
    }

    @Test
    public void testSliceIterator() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        ObjIterator<String> slice = CommonUtil.slice(words.iterator(), 1, 4);

        List<String> result = new ArrayList<>();
        while (slice.hasNext()) {
            result.add(slice.next());
        }
        assertEquals(Arrays.asList("b", "c", "d"), result);
    }

    @Test
    public void testSliceIteratorFromStart() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        ObjIterator<Integer> slice = CommonUtil.slice(numbers.iterator(), 0, 3);

        List<Integer> result = new ArrayList<>();
        while (slice.hasNext()) {
            result.add(slice.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSliceIteratorEmpty() {
        List<String> words = Arrays.asList("a", "b", "c");
        ObjIterator<String> slice = CommonUtil.slice(words.iterator(), 2, 2);

        assertTrue(!slice.hasNext());
    }

    @Test
    public void testSliceIteratorNull() {
        Iterator<String> nullIter = null;
        ObjIterator<String> slice = CommonUtil.slice(nullIter, 0, 5);

        assertTrue(!slice.hasNext());
    }

    @Test
    public void testSliceIteratorInvalidIndices() {
        List<String> words = Arrays.asList("a", "b", "c");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.slice(words.iterator(), -1, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.slice(words.iterator(), 2, 1));
    }

    @Test
    public void testSplitBooleanArray() {
        boolean[] flags = { true, false, true, false, true, false, true };
        List<boolean[]> chunks = N.split(flags, 3);

        assertEquals(3, chunks.size());
        assertArrayEquals(new boolean[] { true, false, true }, chunks.get(0));
        assertArrayEquals(new boolean[] { false, true, false }, chunks.get(1));
        assertArrayEquals(new boolean[] { true }, chunks.get(2));
    }

    @Test
    public void testSplitBooleanArrayEvenSplit() {
        boolean[] flags = { true, false, true, false };
        List<boolean[]> chunks = N.split(flags, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0));
        assertArrayEquals(new boolean[] { true, false }, chunks.get(1));
    }

    @Test
    public void testSplitBooleanArrayEmpty() {
        boolean[] empty = {};
        List<boolean[]> chunks = N.split(empty, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitBooleanArrayNull() {
        boolean[] nullArray = null;
        List<boolean[]> chunks = N.split(nullArray, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitBooleanArrayLargeChunkSize() {
        boolean[] flags = { true, false };
        List<boolean[]> chunks = N.split(flags, 5);

        assertEquals(1, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0));
    }

    @Test
    public void testSplitBooleanArrayInvalidChunkSize() {
        boolean[] flags = { true, false, true };
        assertThrows(IllegalArgumentException.class, () -> N.split(flags, 0));
        assertThrows(IllegalArgumentException.class, () -> N.split(flags, -1));
    }

    @Test
    public void testSplitBooleanArrayWithRange() {
        boolean[] flags = { true, false, true, false, true, false, true, false };
        List<boolean[]> chunks = N.split(flags, 1, 7, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new boolean[] { false, true }, chunks.get(0));
        assertArrayEquals(new boolean[] { false, true }, chunks.get(1));
        assertArrayEquals(new boolean[] { false, true }, chunks.get(2));
    }

    @Test
    public void testSplitBooleanArrayWithRangePartial() {
        boolean[] flags = { true, true, false, false, true };
        List<boolean[]> chunks = N.split(flags, 1, 4, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0));
        assertArrayEquals(new boolean[] { false }, chunks.get(1));
    }

    @Test
    public void testSplitBooleanArrayWithRangeEmpty() {
        boolean[] flags = { true, false, true };
        List<boolean[]> chunks = N.split(flags, 1, 1, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCharArray() {
        char[] letters = { 'a', 'b', 'c', 'd', 'e' };
        List<char[]> chunks = N.split(letters, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new char[] { 'a', 'b' }, chunks.get(0));
        assertArrayEquals(new char[] { 'c', 'd' }, chunks.get(1));
        assertArrayEquals(new char[] { 'e' }, chunks.get(2));
    }

    @Test
    public void testSplitCharArrayEmpty() {
        char[] empty = {};
        List<char[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCharArrayNull() {
        char[] nullArray = null;
        List<char[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCharArrayWithRange() {
        char[] letters = { 'a', 'b', 'c', 'd', 'e', 'f' };
        List<char[]> chunks = N.split(letters, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new char[] { 'b', 'c' }, chunks.get(0));
        assertArrayEquals(new char[] { 'd', 'e' }, chunks.get(1));
    }

    @Test
    public void testSplitByteArray() {
        byte[] data = { 1, 2, 3, 4, 5, 6, 7 };
        List<byte[]> chunks = N.split(data, 3);

        assertEquals(3, chunks.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, chunks.get(0));
        assertArrayEquals(new byte[] { 4, 5, 6 }, chunks.get(1));
        assertArrayEquals(new byte[] { 7 }, chunks.get(2));
    }

    @Test
    public void testSplitByteArrayEmpty() {
        byte[] empty = {};
        List<byte[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByteArrayNull() {
        byte[] nullArray = null;
        List<byte[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByteArrayWithRange() {
        byte[] data = { 1, 2, 3, 4, 5, 6 };
        List<byte[]> chunks = N.split(data, 0, 5, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new byte[] { 1, 2 }, chunks.get(0));
        assertArrayEquals(new byte[] { 3, 4 }, chunks.get(1));
        assertArrayEquals(new byte[] { 5 }, chunks.get(2));
    }

    @Test
    public void testSplitShortArray() {
        short[] data = { 10, 20, 30, 40, 50 };
        List<short[]> chunks = N.split(data, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new short[] { 10, 20 }, chunks.get(0));
        assertArrayEquals(new short[] { 30, 40 }, chunks.get(1));
        assertArrayEquals(new short[] { 50 }, chunks.get(2));
    }

    @Test
    public void testSplitShortArrayEmpty() {
        short[] empty = {};
        List<short[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitShortArrayNull() {
        short[] nullArray = null;
        List<short[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitShortArrayWithRange() {
        short[] data = { 10, 20, 30, 40, 50, 60 };
        List<short[]> chunks = N.split(data, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new short[] { 20, 30 }, chunks.get(0));
        assertArrayEquals(new short[] { 40, 50 }, chunks.get(1));
    }

    @Test
    public void testSplitIntArray() {
        int[] numbers = { 1, 2, 3, 4, 5, 6, 7 };
        List<int[]> chunks = N.split(numbers, 3);

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0));
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1));
        assertArrayEquals(new int[] { 7 }, chunks.get(2));
    }

    @Test
    public void testSplitIntArrayEmpty() {
        int[] empty = {};
        List<int[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIntArrayNull() {
        int[] nullArray = null;
        List<int[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIntArrayWithRange() {
        int[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8 };
        List<int[]> chunks = N.split(numbers, 2, 7, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(0));
        assertArrayEquals(new int[] { 5, 6 }, chunks.get(1));
        assertArrayEquals(new int[] { 7 }, chunks.get(2));
    }

    @Test
    public void testSplitLongArray() {
        long[] numbers = { 1L, 2L, 3L, 4L, 5L };
        List<long[]> chunks = N.split(numbers, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new long[] { 1L, 2L }, chunks.get(0));
        assertArrayEquals(new long[] { 3L, 4L }, chunks.get(1));
        assertArrayEquals(new long[] { 5L }, chunks.get(2));
    }

    @Test
    public void testSplitLongArrayEmpty() {
        long[] empty = {};
        List<long[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitLongArrayNull() {
        long[] nullArray = null;
        List<long[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitLongArrayWithRange() {
        long[] numbers = { 1L, 2L, 3L, 4L, 5L, 6L };
        List<long[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new long[] { 2L, 3L }, chunks.get(0));
        assertArrayEquals(new long[] { 4L, 5L }, chunks.get(1));
    }

    @Test
    public void testSplitFloatArray() {
        float[] numbers = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<float[]> chunks = N.split(numbers, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new float[] { 1.0f, 2.0f }, chunks.get(0));
        assertArrayEquals(new float[] { 3.0f, 4.0f }, chunks.get(1));
        assertArrayEquals(new float[] { 5.0f }, chunks.get(2));
    }

    @Test
    public void testSplitFloatArrayEmpty() {
        float[] empty = {};
        List<float[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitFloatArrayNull() {
        float[] nullArray = null;
        List<float[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitFloatArrayWithRange() {
        float[] numbers = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f };
        List<float[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new float[] { 2.0f, 3.0f }, chunks.get(0));
        assertArrayEquals(new float[] { 4.0f, 5.0f }, chunks.get(1));
    }

    @Test
    public void testSplitDoubleArray() {
        double[] numbers = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<double[]> chunks = N.split(numbers, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new double[] { 1.0, 2.0 }, chunks.get(0));
        assertArrayEquals(new double[] { 3.0, 4.0 }, chunks.get(1));
        assertArrayEquals(new double[] { 5.0 }, chunks.get(2));
    }

    @Test
    public void testSplitDoubleArrayEmpty() {
        double[] empty = {};
        List<double[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitDoubleArrayNull() {
        double[] nullArray = null;
        List<double[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitDoubleArrayWithRange() {
        double[] numbers = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
        List<double[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new double[] { 2.0, 3.0 }, chunks.get(0));
        assertArrayEquals(new double[] { 4.0, 5.0 }, chunks.get(1));
    }

    @Test
    public void testSplitObjectArray() {
        String[] words = { "a", "b", "c", "d", "e" };
        List<String[]> chunks = N.split(words, 2);

        assertEquals(3, chunks.size());
        assertArrayEquals(new String[] { "a", "b" }, chunks.get(0));
        assertArrayEquals(new String[] { "c", "d" }, chunks.get(1));
        assertArrayEquals(new String[] { "e" }, chunks.get(2));
    }

    @Test
    public void testSplitObjectArrayEmpty() {
        String[] empty = {};
        List<String[]> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitObjectArrayNull() {
        String[] nullArray = null;
        List<String[]> chunks = N.split(nullArray, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitObjectArrayWithRange() {
        Integer[] numbers = { 1, 2, 3, 4, 5, 6 };
        List<Integer[]> chunks = N.split(numbers, 1, 5, 2);

        assertEquals(2, chunks.size());
        assertArrayEquals(new Integer[] { 2, 3 }, chunks.get(0));
        assertArrayEquals(new Integer[] { 4, 5 }, chunks.get(1));
    }

    @Test
    public void testSplitCollection() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> chunks = N.split(words, 2);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList("a", "b"), chunks.get(0));
        assertEquals(Arrays.asList("c", "d"), chunks.get(1));
        assertEquals(Arrays.asList("e"), chunks.get(2));
    }

    @Test
    public void testSplitCollectionEmpty() {
        List<String> empty = new ArrayList<>();
        List<List<String>> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCollectionNull() {
        Collection<String> nullColl = null;
        List<List<String>> chunks = N.split(nullColl, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitCollectionWithRange() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = N.split(numbers, 1, 6, 2);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(2, 3), chunks.get(0));
        assertEquals(Arrays.asList(4, 5), chunks.get(1));
        assertEquals(Arrays.asList(6), chunks.get(2));
    }

    @Test
    public void testSplitIterable() {
        Iterable<String> words = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> chunks = N.split(words, 2);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList("a", "b"), chunks.get(0));
        assertEquals(Arrays.asList("c", "d"), chunks.get(1));
        assertEquals(Arrays.asList("e"), chunks.get(2));
    }

    @Test
    public void testSplitIterableEmpty() {
        Iterable<String> empty = new ArrayList<>();
        List<List<String>> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIterableNull() {
        Iterable<String> nullIterable = null;
        List<List<String>> chunks = N.split(nullIterable, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitIterator() {
        List<String> words = Arrays.asList("a", "b", "c", "d", "e");
        ObjIterator<List<String>> chunks = N.split(words.iterator(), 2);

        List<List<String>> result = new ArrayList<>();
        while (chunks.hasNext()) {
            result.add(chunks.next());
        }

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d"), result.get(1));
        assertEquals(Arrays.asList("e"), result.get(2));
    }

    @Test
    public void testSplitIteratorEmpty() {
        Iterator<String> empty = Collections.emptyIterator();
        ObjIterator<List<String>> chunks = N.split(empty, 2);

        assertTrue(!chunks.hasNext());
    }

    @Test
    public void testSplitIteratorNull() {
        Iterator<String> nullIter = null;
        ObjIterator<List<String>> chunks = N.split(nullIter, 2);

        assertTrue(!chunks.hasNext());
    }

    @Test
    public void testSplitString() {
        String text = "abcdefg";
        List<String> chunks = N.split(text, 3);

        assertEquals(3, chunks.size());
        assertEquals("abc", chunks.get(0));
        assertEquals("def", chunks.get(1));
        assertEquals("g", chunks.get(2));
    }

    @Test
    public void testSplitStringEmpty() {
        String empty = "";
        List<String> chunks = N.split(empty, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitStringNull() {
        String nullStr = null;
        List<String> chunks = N.split(nullStr, 2);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitStringWithRange() {
        String text = "abcdefgh";
        List<String> chunks = N.split(text, 1, 7, 2);

        assertEquals(3, chunks.size());
        assertEquals("bc", chunks.get(0));
        assertEquals("de", chunks.get(1));
        assertEquals("fg", chunks.get(2));
    }

    @Test
    public void testSplitStringLargeChunkSize() {
        String text = "hello";
        List<String> chunks = N.split(text, 10);

        assertEquals(1, chunks.size());
        assertEquals("hello", chunks.get(0));
    }

    @Test
    public void testSplitByChunkCountWithFunction() {
        int[] a = { 1, 2, 3, 4, 5, 6, 7 };
        List<int[]> chunks = N.splitByChunkCount(7, 5, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));

        assertEquals(5, chunks.size());
        assertArrayEquals(new int[] { 1, 2 }, chunks.get(0));
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(1));
        assertArrayEquals(new int[] { 5 }, chunks.get(2));
        assertArrayEquals(new int[] { 6 }, chunks.get(3));
        assertArrayEquals(new int[] { 7 }, chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountWithFunctionSizeSmallerFirst() {
        int[] a = { 1, 2, 3, 4, 5, 6, 7 };
        List<int[]> chunks = N.splitByChunkCount(7, 5, true, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));

        assertEquals(5, chunks.size());
        assertArrayEquals(new int[] { 1 }, chunks.get(0));
        assertArrayEquals(new int[] { 2 }, chunks.get(1));
        assertArrayEquals(new int[] { 3 }, chunks.get(2));
        assertArrayEquals(new int[] { 4, 5 }, chunks.get(3));
        assertArrayEquals(new int[] { 6, 7 }, chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountWithFunctionExactDivision() {
        int[] a = { 1, 2, 3, 4, 5, 6 };
        List<int[]> chunks = N.splitByChunkCount(6, 3, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1, 2 }, chunks.get(0));
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(1));
        assertArrayEquals(new int[] { 5, 6 }, chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountWithFunctionSizeOne() {
        int[] a = { 1, 2, 3 };
        List<int[]> chunks = N.splitByChunkCount(3, 1, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));

        assertEquals(1, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0));
    }

    @Test
    public void testSplitByChunkCountWithFunctionMoreChunksThanSize() {
        int[] a = { 1, 2, 3 };
        List<int[]> chunks = N.splitByChunkCount(3, 5, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));

        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1 }, chunks.get(0));
        assertArrayEquals(new int[] { 2 }, chunks.get(1));
        assertArrayEquals(new int[] { 3 }, chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountWithFunctionInvalidArgs() {
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(-1, 5, (f, t) -> null));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(10, 0, (f, t) -> null));
    }

    @Test
    public void testSplitByChunkCountCollection() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 5);

        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5), chunks.get(2));
        assertEquals(Arrays.asList(6), chunks.get(3));
        assertEquals(Arrays.asList(7), chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountCollectionSizeSmallerFirst() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 5, true);

        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1), chunks.get(0));
        assertEquals(Arrays.asList(2), chunks.get(1));
        assertEquals(Arrays.asList(3), chunks.get(2));
        assertEquals(Arrays.asList(4, 5), chunks.get(3));
        assertEquals(Arrays.asList(6, 7), chunks.get(4));
    }

    @Test
    public void testSplitByChunkCountCollectionEmpty() {
        List<String> empty = new ArrayList<>();
        List<List<String>> chunks = N.splitByChunkCount(empty, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByChunkCountCollectionNull() {
        Collection<String> nullColl = null;
        List<List<String>> chunks = N.splitByChunkCount(nullColl, 3);

        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitByChunkCountCollectionExactDivision() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 3);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5, 6), chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountCollectionMoreChunksThanElements() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);
        List<List<Integer>> chunks = N.splitByChunkCount(numbers, 5);

        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1), chunks.get(0));
        assertEquals(Arrays.asList(2), chunks.get(1));
        assertEquals(Arrays.asList(3), chunks.get(2));
    }

    @Test
    public void testSplitByChunkCountCollectionInvalidChunkCount() {
        List<Integer> numbers = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(numbers, 0));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(numbers, -1));
    }

    @Test
    public void testConcatBooleanArrayTwoArrays() {
        boolean[] a = { true, false };
        boolean[] b = { true, true };
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false, true, true }, result);
    }

    @Test
    public void testConcatBooleanArrayFirstEmpty() {
        boolean[] a = {};
        boolean[] b = { true, false };
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArraySecondEmpty() {
        boolean[] a = { true, false };
        boolean[] b = {};
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArrayBothEmpty() {
        boolean[] a = {};
        boolean[] b = {};
        boolean[] result = N.concat(a, b);

        assertEquals(0, result.length);
    }

    @Test
    public void testConcatBooleanArrayFirstNull() {
        boolean[] a = null;
        boolean[] b = { true, false };
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArraySecondNull() {
        boolean[] a = { true, false };
        boolean[] b = null;
        boolean[] result = N.concat(a, b);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatBooleanArrayBothNull() {
        boolean[] a = null;
        boolean[] b = null;
        boolean[] result = N.concat(a, b);

        assertEquals(0, result.length);
    }

    @Test
    public void testConcatBooleanArrayVarargs() {
        boolean[] a = { true };
        boolean[] b = { false };
        boolean[] c = { true, false };
        boolean[] result = N.concat(a, b, c);

        assertArrayEquals(new boolean[] { true, false, true, false }, result);
    }

    @Test
    public void testConcatBooleanArrayVarargsEmpty() {
        boolean[][] empty = {};
        boolean[] result = N.concat(empty);

        assertEquals(0, result.length);
    }

    @Test
    public void testConcatBooleanArrayVarargsSingleArray() {
        boolean[] a = { true, false };
        boolean[] result = N.concat(a);

        assertArrayEquals(new boolean[] { true, false }, result);
    }

    @Test
    public void testConcatCharArrayTwoArrays() {
        char[] a = { 'a', 'b' };
        char[] b = { 'c', 'd' };
        char[] result = N.concat(a, b);

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testConcatCharArrayEmpty() {
        char[] a = {};
        char[] b = { 'x', 'y' };
        char[] result = N.concat(a, b);

        assertArrayEquals(new char[] { 'x', 'y' }, result);
    }

    @Test
    public void testConcatCharArrayNull() {
        char[] a = null;
        char[] b = { 'x' };
        char[] result = N.concat(a, b);

        assertArrayEquals(new char[] { 'x' }, result);
    }

    @Test
    public void testConcatCharArrayVarargs() {
        char[] a = { 'a', 'b' };
        char[] b = { 'c' };
        char[] c = { 'd', 'e' };
        char[] result = N.concat(a, b, c);

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testConcatByteArrayTwoArrays() {
        byte[] a = { 1, 2 };
        byte[] b = { 3, 4 };
        byte[] result = N.concat(a, b);

        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatByteArrayEmpty() {
        byte[] a = {};
        byte[] b = { 5, 6 };
        byte[] result = N.concat(a, b);

        assertArrayEquals(new byte[] { 5, 6 }, result);
    }

    @Test
    public void testConcatByteArrayNull() {
        byte[] a = null;
        byte[] b = { 1 };
        byte[] result = N.concat(a, b);

        assertArrayEquals(new byte[] { 1 }, result);
    }

    @Test
    public void testConcatByteArrayVarargs() {
        byte[] a = { 1 };
        byte[] b = { 2, 3 };
        byte[] c = { 4, 5, 6 };
        byte[] result = N.concat(a, b, c);

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatShortArrayTwoArrays() {
        short[] a = { 10, 20 };
        short[] b = { 30, 40 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 10, 20, 30, 40 }, result);
    }

    @Test
    public void testConcatShortArrayEmpty() {
        short[] a = {};
        short[] b = { 50 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 50 }, result);
    }

    @Test
    public void testConcatShortArrayNull() {
        short[] a = null;
        short[] b = { 10 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 10 }, result);
    }

    @Test
    public void testConcatShortArrayVarargs() {
        short[] a = { 10 };
        short[] b = { 20, 30 };
        short[] result = N.concat(a, b);

        assertArrayEquals(new short[] { 10, 20, 30 }, result);
    }

    @Test
    public void testConcatIntArrayTwoArrays() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5 };
        int[] result = N.concat(a, b);

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConcatIntArrayEmpty() {
        int[] a = {};
        int[] b = { 10, 20 };
        int[] result = N.concat(a, b);

        assertArrayEquals(new int[] { 10, 20 }, result);
    }

    @Test
    public void testConcatIntArrayNull() {
        int[] a = null;
        int[] b = { 1, 2 };
        int[] result = N.concat(a, b);

        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testConcatIntArrayVarargs() {
        int[] a = { 1, 2 };
        int[] b = { 3 };
        int[] c = { 4, 5, 6 };
        int[] result = N.concat(a, b, c);

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);
    }

    @Test
    public void testConcatIntArrayVarargsWithNulls() {
        int[] a = { 1 };
        int[] b = null;
        int[] c = { 2 };
        int[] result = N.concat(a, b, c);

        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testConcatLongArrayTwoArrays() {
        long[] a = { 1L, 2L };
        long[] b = { 3L, 4L };
        long[] result = N.concat(a, b);

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatLongArrayEmpty() {
        long[] a = {};
        long[] b = { 100L };
        long[] result = N.concat(a, b);

        assertArrayEquals(new long[] { 100L }, result);
    }

    @Test
    public void testConcatLongArrayNull() {
        long[] a = null;
        long[] b = { 5L };
        long[] result = N.concat(a, b);

        assertArrayEquals(new long[] { 5L }, result);
    }

    @Test
    public void testConcatLongArrayVarargs() {
        long[] a = { 1L };
        long[] b = { 2L, 3L };
        long[] c = { 4L };
        long[] result = N.concat(a, b, c);

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatFloatArrayTwoArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        float[] result = N.concat(a, b);

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
    }

    @Test
    public void testConcatFloatArrayEmpty() {
        float[] a = {};
        float[] b = { 5.5f };
        float[] result = N.concat(a, b);

        assertArrayEquals(new float[] { 5.5f }, result);
    }

    @Test
    public void testConcatFloatArrayNull() {
        float[] a = null;
        float[] b = { 1.1f };
        float[] result = N.concat(a, b);

        assertArrayEquals(new float[] { 1.1f }, result);
    }

    @Test
    public void testConcatFloatArrayVarargs() {
        float[] a = { 1.0f };
        float[] b = { 2.0f };
        float[] c = { 3.0f };
        float[] result = N.concat(a, b, c);

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

    @Test
    public void testConcatDoubleArrayTwoArrays() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0 };
        double[] result = N.concat(a, b);

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result);
    }

    @Test
    public void testConcatDoubleArrayEmpty() {
        double[] a = {};
        double[] b = { 5.5 };
        double[] result = N.concat(a, b);

        assertArrayEquals(new double[] { 5.5 }, result);
    }

    @Test
    public void testConcatDoubleArrayNull() {
        double[] a = null;
        double[] b = { 1.1 };
        double[] result = N.concat(a, b);

        assertArrayEquals(new double[] { 1.1 }, result);
    }

    @Test
    public void testConcatDoubleArrayVarargs() {
        double[] a = { 1.0 };
        double[] b = { 2.0, 3.0 };
        double[] c = { 4.0 };
        double[] result = N.concat(a, b, c);

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result);
    }

    @Test
    public void testConcatObjectArrayTwoArrays() {
        String[] a = { "a", "b" };
        String[] b = { "c", "d" };
        String[] result = N.concat(a, b);

        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);
    }

    @Test
    public void testConcatObjectArrayEmpty() {
        String[] a = {};
        String[] b = { "x", "y" };
        String[] result = N.concat(a, b);

        assertArrayEquals(new String[] { "x", "y" }, result);
    }

    @Test
    public void testConcatObjectArrayNull() {
        String[] a = null;
        String[] b = { "test" };
        String[] result = N.concat(a, b);

        assertArrayEquals(new String[] { "test" }, result);
    }

    @Test
    public void testConcatObjectArrayBothNull() {
        String[] a = null;
        String[] b = null;
        String[] result = N.concat(a, b);

        assertNull(result);
    }

    @Test
    public void testConcatObjectArrayVarargs() {
        Integer[] a = { 1, 2 };
        Integer[] b = { 3 };
        Integer[] c = { 4, 5 };
        Integer[] result = N.concat(a, b, c);

        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConcatObjectArrayVarargsWithNulls() {
        String[] a = { "a" };
        String[] b = null;
        String[] c = { "b" };
        String[] result = N.concat(a, b, c);

        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testConcatObjectArrayVarargsEmpty() {
        String[][] empty = {};
        String[] result = N.concat(empty);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testConcatIterableTwoIterables() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("c", "d");
        List<String> result = N.concat(a, b);

        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testConcatIterableEmpty() {
        List<String> a = new ArrayList<>();
        List<String> b = Arrays.asList("x", "y");
        List<String> result = N.concat(a, b);

        assertEquals(Arrays.asList("x", "y"), result);
    }

    @Test
    public void testConcatIterableNull() {
        List<String> a = null;
        List<String> b = Arrays.asList("test");
        List<String> result = N.concat(a, b);

        assertEquals(Arrays.asList("test"), result);
    }

    @Test
    public void testConcatIterableVarargs() {
        List<Integer> a = Arrays.asList(1, 2);
        List<Integer> b = Arrays.asList(3);
        List<Integer> c = Arrays.asList(4, 5);
        List<Integer> result = N.concat(a, b, c);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testConcatIterableVarargsEmpty() {
        @SuppressWarnings("unchecked")
        Iterable<String>[] empty = new Iterable[0];
        List<String> result = N.concat(empty);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterableCollection() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e"));
        List<String> result = N.concat(lists);

        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testConcatIterableCollectionEmpty() {
        List<List<String>> empty = new ArrayList<>();
        List<String> result = N.concat(empty);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterableCollectionNull() {
        Collection<List<String>> nullColl = null;
        List<String> result = N.concat(nullColl);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterableCollectionWithSupplier() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        Set<String> result = N.concat(lists, IntFunctions.ofSet());

        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testConcatIteratorTwoIterators() {
        Iterator<String> a = Arrays.asList("a", "b").iterator();
        Iterator<String> b = Arrays.asList("c", "d").iterator();
        ObjIterator<String> result = N.concat(a, b);

        List<String> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testConcatIteratorEmpty() {
        Iterator<String> a = Collections.emptyIterator();
        Iterator<String> b = Arrays.asList("x").iterator();
        ObjIterator<String> result = N.concat(a, b);

        assertTrue(result.hasNext());
        assertEquals("x", result.next());
    }

    @Test
    public void testConcatIteratorNull() {
        Iterator<String> a = null;
        Iterator<String> b = Arrays.asList("test").iterator();
        ObjIterator<String> result = N.concat(a, b);

        assertTrue(result.hasNext());
        assertEquals("test", result.next());
    }

    @Test
    public void testConcatIteratorVarargs() {
        Iterator<Integer> a = Arrays.asList(1, 2).iterator();
        Iterator<Integer> b = Arrays.asList(3).iterator();
        Iterator<Integer> c = Arrays.asList(4, 5).iterator();
        ObjIterator<Integer> result = N.concat(a, b, c);

        List<Integer> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void testConcatIteratorVarargsEmpty() {
        @SuppressWarnings("unchecked")
        Iterator<String>[] empty = new Iterator[0];
        ObjIterator<String> result = N.concat(empty);

        assertTrue(!result.hasNext());
    }

    @Test
    public void testConcatIteratorVarargsWithNulls() {
        Iterator<String> a = Arrays.asList("a").iterator();
        Iterator<String> b = null;
        Iterator<String> c = Arrays.asList("b").iterator();
        ObjIterator<String> result = N.concat(a, b, c);

        List<String> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void toJson_object() {
        TestBean bean = createSampleBean();
        String json = N.toJson(bean);
        N.println(json);
        assertEquals(getExpectedJsonForSampleBean(false), json);
    }

    @Test
    public void toJson_object_prettyFormat() {
        TestBean bean = createSampleBean();
        String json = N.toJson(bean, true);
        assertTrue(json.contains("\"name\": \"testName\""));
        assertTrue(json.contains("\n"));
    }

    @Test
    public void toJson_object_withConfig() {
        TestBean bean = createSampleBean();
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);
        String json = N.toJson(bean, config);
        assertEquals(getExpectedJsonForSampleBean(true), json);
    }

    @Test
    public void toJson_object_toFile(@TempDir Path tempDir) throws IOException {
        TestBean bean = createSampleBean();
        File outputFile = tempDir.resolve("output.json").toFile();
        N.toJson(bean, outputFile);
        assertTrue(outputFile.exists());
        String fileContent = new String(Files.readAllBytes(outputFile.toPath()));
        assertEquals(getExpectedJsonForSampleBean(false), fileContent);
    }

    @Test
    public void toJson_object_withConfig_toFile(@TempDir Path tempDir) throws IOException {
        TestBean bean = createSampleBean();
        File outputFile = tempDir.resolve("output_pretty.json").toFile();
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);
        N.toJson(bean, config, outputFile);
        assertTrue(outputFile.exists());
        String fileContent = new String(Files.readAllBytes(outputFile.toPath()));
        assertEquals(getExpectedJsonForSampleBean(true), fileContent);
    }

    @Test
    public void toJson_object_toOutputStream() throws IOException {
        TestBean bean = createSampleBean();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toJson(bean, baos);
        assertEquals(getExpectedJsonForSampleBean(false), baos.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    public void toJson_object_withConfig_toOutputStream() throws IOException {
        TestBean bean = createSampleBean();
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toJson(bean, config, baos);
        assertEquals(getExpectedJsonForSampleBean(true), baos.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    public void toJson_object_toWriter() throws IOException {
        TestBean bean = createSampleBean();
        StringWriter writer = new StringWriter();
        N.toJson(bean, writer);
        assertEquals(getExpectedJsonForSampleBean(false), writer.toString());
    }

    @Test
    public void toJson_object_withConfig_toWriter() throws IOException {
        TestBean bean = createSampleBean();
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);
        StringWriter writer = new StringWriter();
        N.toJson(bean, config, writer);
        assertEquals(getExpectedJsonForSampleBean(true), writer.toString());
    }

    @Test
    public void fromJson_string_toClass() {
        String json = getExpectedJsonForSampleBean(false);
        TestBean bean = N.fromJson(json, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_toType() {
        String json = getExpectedJsonForSampleBean(false);
        Type<TestBean> type = new TypeReference<TestBean>() {
        }.type();
        TestBean bean = N.fromJson(json, type);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withDefault_toClass() {
        String json = null;
        TestBean defaultBean = new TestBean("default", 0, null, null);
        TestBean bean = N.fromJson(json, defaultBean, TestBean.class);
        assertEquals(defaultBean, bean);

        String validJson = getExpectedJsonForSampleBean(false);
        bean = N.fromJson(validJson, defaultBean, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withDefault_toType() {
        String json = null;
        Type<TestBean> type = new TypeReference<TestBean>() {
        }.type();
        TestBean defaultBean = new TestBean("default", 0, null, null);
        TestBean bean = N.fromJson(json, defaultBean, type);
        assertEquals(defaultBean, bean);

        String validJson = getExpectedJsonForSampleBean(false);
        bean = N.fromJson(validJson, defaultBean, type);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withConfig_toClass() {
        String json = getExpectedJsonForSampleBean(false);
        JsonDeserConfig config = JsonDeserConfig.create();
        TestBean bean = N.fromJson(json, config, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withConfig_toType() {
        String json = getExpectedJsonForSampleBean(false);
        JsonDeserConfig config = JsonDeserConfig.create();
        Type<TestBean> type = new TypeReference<TestBean>() {
        }.type();
        TestBean bean = N.fromJson(json, config, type);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_file_toClass(@TempDir Path tempDir) throws IOException {
        String jsonContent = getExpectedJsonForSampleBean(false);
        File inputFile = tempDir.resolve("input.json").toFile();
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(jsonContent);
        }
        TestBean bean = N.fromJson(inputFile, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_inputStream_toClass() throws IOException {
        String jsonContent = getExpectedJsonForSampleBean(false);
        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        TestBean bean = N.fromJson(inputStream, TestBean.class);
        assertEquals(createSampleBean(), bean);
        inputStream.close();
    }

    @Test
    public void fromJson_reader_toClass() throws IOException {
        String jsonContent = getExpectedJsonForSampleBean(false);
        Reader reader = new StringReader(jsonContent);
        TestBean bean = N.fromJson(reader, TestBean.class);
        assertEquals(createSampleBean(), bean);
        reader.close();
    }

    @Test
    public void fromJson_substring_toClass() {
        String prefix = "###";
        String suffix = "@@@";
        String actualJson = getExpectedJsonForSampleBean(false);
        String jsonWithPadding = prefix + actualJson + suffix;
        TestBean bean = N.fromJson(jsonWithPadding, prefix.length(), jsonWithPadding.length() - suffix.length(), TestBean.class);
        assertEquals(createSampleBean(), bean);

        assertThrows(IndexOutOfBoundsException.class, () -> N.fromJson("{}", 0, 10, TestBean.class));
    }

    @Test
    public void streamJson_string_toClass() {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "," + getExpectedJsonForSampleBean(false) + "]";
        Stream<TestBean> stream = N.streamJson(jsonArray, Type.of(TestBean.class));
        List<TestBean> list = stream.toList();
        assertEquals(2, list.size());
        assertEquals(createSampleBean(), list.get(0));
        assertEquals(createSampleBean(), list.get(1));
    }

    @Test
    public void streamJson_file_toClass(@TempDir Path tempDir) throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        File inputFile = tempDir.resolve("input_array.json").toFile();
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(jsonArray);
        }
        Stream<TestBean> stream = N.streamJson(inputFile, Type.of(TestBean.class));
        assertEquals(createSampleBean(), stream.first().orElse(null));
    }

    @Test
    public void streamJson_inputStream_toClass_autoClose() throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        final AtomicBoolean closed = new AtomicBoolean(false);
        InputStream inputStream = new ByteArrayInputStream(jsonArray.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                super.close();
                closed.set(true);
            }
        };

        try (Stream<TestBean> stream = N.streamJson(inputStream, true, Type.of(TestBean.class))) {
            assertEquals(createSampleBean(), stream.first().orElse(null));
        }
        assertTrue(closed.get(), "InputStream should be closed when stream is closed with autoClose=true");
    }

    @Test
    public void streamJson_reader_toClass_autoClose() throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        final AtomicBoolean closed = new AtomicBoolean(false);
        Reader reader = new StringReader(jsonArray) {
            @Override
            public void close() {
                super.close();
                closed.set(true);
            }
        };

        try (Stream<TestBean> stream = N.streamJson(reader, true, Type.of(TestBean.class))) {
            assertEquals(createSampleBean(), stream.first().orElse(null));
        }
        assertTrue(closed.get(), "Reader should be closed when stream is closed with autoClose=true");
    }

    @Test
    public void formatJson_string() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String prettyJson = N.formatJson(uglyJson);
        assertTrue(prettyJson.contains("\n"));
        assertEquals(N.fromJson(uglyJson, Object.class), N.fromJson(prettyJson, Object.class));
    }

    @Test
    public void formatJson_string_withClass() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String prettyJson = N.formatJson(uglyJson, TestBean.class);
        assertEquals(getExpectedJsonForSampleBean(true), prettyJson);
    }

    @Test
    public void toXml_object() {
        TestBean bean = createSampleBean();
        String xml = N.toXml(bean);
        assertTrue(xml.contains("<testBean>"));
        assertTrue(xml.contains("<name>testName</name>"));
        assertTrue(xml.contains("<value>123</value>"));
        assertTrue(xml.contains("<items>[&quot;item1&quot;, &quot;item2&quot;]</items>"));
        assertTrue(xml.contains("<key1>100</key1>") || xml.contains("<key>key1</key><value>100</value>"));
    }

    @Test
    public void toXml_object_prettyFormat() {
        TestBean bean = createSampleBean();
        String xml = N.toXml(bean, true);
        assertTrue(xml.contains("<testBean>"));
        assertTrue(xml.contains("<name>testName</name>"));
    }

    @Test
    public void toXml_object_withConfig_toWriter() throws IOException {
        TestBean bean = createSampleBean();
        XmlSerConfig config = XmlSerConfig.create().setPrettyFormat(true);
        StringWriter writer = new StringWriter();
        N.toXml(bean, config, writer);
        String xml = writer.toString();
        assertTrue(xml.contains("<testBean>"));
        assertTrue(xml.contains("<name>testName</name>"));
    }

    @Test
    public void fromXml_string_toClass() {
        String xml = "<NTest_TestBean><name>testName</name><value>123</value><items><String>item1</String><String>item2</String></items><properties><entry><key>key1</key><Object>100</Object></entry><entry><key>key2</key><Object>200</Object></entry></properties></NTest_TestBean>";
        String generatedXml = N.toXml(createSampleBean());

        TestBean bean = N.fromXml(generatedXml, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void xmlToJson_string() {
        String xml = N.toXml(createSampleBean());
        String json = N.xmlToJson(xml);

        Map<String, Object> map = N.fromJson(json, Map.class);
        assertNotNull(map);
        assertEquals("testName", map.get("name"));
    }

    @Test
    public void jsonToXml_string() {
        String json = N.toJson(createSampleBean());
        String xml = N.jsonToXml(json);

        MapEntity mapEntity = N.fromXml(xml, MapEntity.class);
        assertNotNull(mapEntity);
        assertEquals("testName", mapEntity.get("name"));
        assertEquals("123", mapEntity.get("value"));
    }

    @Test
    public void forEach_intRange_runnable() {
        AtomicInteger count = new AtomicInteger(0);
        N.forEach(0, 5, count::incrementAndGet);
        assertEquals(5, count.get());

        count.set(0);
        N.forEach(5, 0, count::incrementAndGet);
        assertEquals(0, count.get());
    }

    @Test
    public void forEach_intRange_withStep_runnable() {
        AtomicInteger count = new AtomicInteger(0);
        N.forEach(0, 5, 2, count::incrementAndGet);
        assertEquals(3, count.get());

        count.set(0);
        N.forEach(5, 0, -1, count::incrementAndGet);
        assertEquals(5, count.get());

        assertThrows(IllegalArgumentException.class, () -> N.forEach(0, 5, 0, count::incrementAndGet));
    }

    @Test
    public void forEach_intRange_intConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(0, 5, sum::addAndGet);
        assertEquals(10, sum.get());
    }

    @Test
    public void forEach_intRange_withStep_intConsumer() {
        List<Integer> result = new ArrayList<>();
        N.forEach(0, 6, 2, result::add);
        assertEquals(Arrays.asList(0, 2, 4), result);

        result.clear();
        N.forEach(5, -1, -2, result::add);
        assertEquals(Arrays.asList(5, 3, 1), result);
    }

    @Test
    public void forEach_intRange_withObject_intObjConsumer() {
        StringBuilder sb = new StringBuilder();
        String prefix = "val:";
        N.forEach(0, 3, prefix, (i, p) -> sb.append(p).append(i).append(" "));
        assertEquals("val:0 val:1 val:2 ", sb.toString());
    }

    @Test
    public void forEach_array_consumer() {
        String[] array = { "a", "b", "c" };
        List<String> result = new ArrayList<>();
        N.forEach(array, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        N.forEach((String[]) null, result::add);
        assertTrue(result.size() == 3);

        N.forEach(new String[0], result::add);
        assertTrue(result.size() == 3);
    }

    @Test
    public void forEach_array_fromIndex_toIndex_consumer() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEach(array, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        result.clear();
        N.forEach(array, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);

        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(array, 0, 10, result::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(array, -1, 2, result::add));
    }

    @Test
    public void forEach_iterable_consumer() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        N.forEach((List<String>) null, result::add);
        assertTrue(result.size() == 3);
    }

    @Test
    public void forEach_iterator_consumer() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list.iterator(), result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        N.forEach((Iterator<String>) null, result::add);
        assertTrue(result.size() == 3);
    }

    @Test
    public void forEach_collection_fromIndex_toIndex_consumer() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();
        N.forEach(list, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        Collection<String> collection = new java.util.LinkedList<>(list);
        result.clear();
        N.forEach(collection, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        result.clear();
        N.forEach(list, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);

        result.clear();
        N.forEach(collection, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);

        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(list, 0, 10, result::add));
    }

    @Test
    public void forEach_map_entryConsumer() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(map, (Throwables.Consumer<Map.Entry<String, Integer>, RuntimeException>) entry -> sum.addAndGet(entry.getValue()));
        assertEquals(3, sum.get());
    }

    @Test
    public void forEach_map_biConsumer() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> resultMap = new HashMap<>();
        N.forEach(map, (Throwables.BiConsumer<String, Integer, RuntimeException>) resultMap::put);
        assertEquals(map, resultMap);
    }

    @Test
    public void forEach_iterable_consumer_threaded() throws InterruptedException {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger sum = new AtomicInteger(0);
        int numThreads = 2;

        N.forEach(list, (Throwables.Consumer<Integer, RuntimeException>) e -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            sum.addAndGet(e);
        }, numThreads, executorService);

        assertEquals(55, sum.get());
    }

    @Test
    public void forEach_iterator_consumer_threaded_withException() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        AtomicInteger processedCount = new AtomicInteger(0);
        Throwables.Consumer<Integer, Exception> consumerWithException = val -> {
            processedCount.incrementAndGet();
            if (val == 3) {
                throw new IOException("Test exception");
            }
        };

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> N.forEach(list.iterator(), consumerWithException, 2, executorService));
        assertTrue(thrown.getCause() instanceof IOException
                || (thrown.getCause() instanceof ExecutionException && thrown.getCause().getCause() instanceof IOException));
        assertTrue(processedCount.get() >= 1 && processedCount.get() <= list.size());
    }

    @Test
    public void forEach_array_flatMap_biConsumer() throws Exception {
        String[] array = { "a", "b" };
        List<Tuple.Tuple2<String, Integer>> result = new ArrayList<>();
        N.forEach(array, s -> s.equals("a") ? Arrays.asList(1, 2) : Arrays.asList(3, 4), (s, i) -> result.add(Tuple.of(s, i)));

        List<Tuple.Tuple2<String, Integer>> expected = Arrays.asList(Tuple.of("a", 1), Tuple.of("a", 2), Tuple.of("b", 3), Tuple.of("b", 4));
        assertEquals(expected, result);
    }

    @Test
    public void forEach_arrays_biConsumer_shortCircuit() throws Exception {
        String[] a = { "one", "two", "three" };
        Integer[] b = { 1, 2 };
        List<String> result = new ArrayList<>();
        N.forEach(a, b, (s, i) -> result.add(s + ":" + i));
        assertEquals(Arrays.asList("one:1", "two:2"), result);
    }

    @Test
    public void forEach_arrays_biConsumer_withDefaults() throws Exception {
        String[] a = { "one", "two" };
        Integer[] b = { 1, 2, 3 };
        String defaultA = "defaultA";
        Integer defaultB = -1;
        List<String> result = new ArrayList<>();
        N.forEach(a, b, defaultA, defaultB, (s, i) -> result.add(s + ":" + i));

        assertEquals(Arrays.asList("one:1", "two:2", "defaultA:3"), result);
    }

    @Test
    public void forEachNonNull_array_consumer() throws Exception {
        String[] array = { "a", null, "c", null, "e" };
        List<String> result = new ArrayList<>();
        N.forEachNonNull(array, result::add);
        assertEquals(Arrays.asList("a", "c", "e"), result);
    }

    @Test
    public void forEachIndexed_array_intObjConsumer() throws Exception {
        String[] array = { "x", "y", "z" };
        List<String> result = new ArrayList<>();
        N.forEachIndexed(array, (idx, val) -> result.add(idx + ":" + val));
        assertEquals(Arrays.asList("0:x", "1:y", "2:z"), result);
    }

    @Test
    public void forEachIndexed_collection_fromIndex_toIndex_intObjConsumer() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();
        N.forEachIndexed(list, 1, 4, (idx, val) -> result.add(idx + ":" + val));
        assertEquals(Arrays.asList("1:b", "2:c", "3:d"), result);

        result.clear();
        N.forEachIndexed(list, 3, 1, (idx, val) -> result.add(idx + ":" + val));
        assertEquals(Arrays.asList("3:d", "2:c"), result);
    }

    @Test
    public void forEachIndexed_iterable_consumer_threaded() throws InterruptedException {
        List<String> data = Arrays.asList("a", "b", "c", "d", "e", "f");
        Map<Integer, String> resultMap = Collections.synchronizedMap(new HashMap<>());
        int numThreads = 3;

        N.forEachIndexed(data, (Throwables.IntObjConsumer<String, InterruptedException>) (idx, val) -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            resultMap.put(idx, val);
        }, numThreads, executorService);

        assertEquals(data.size(), resultMap.size());
        for (int i = 0; i < data.size(); i++) {
            assertEquals(data.get(i), resultMap.get(i));
        }
    }

    @Test
    public void forEachPair_array_biConsumer() throws Exception {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEachPair(array, (e1, e2) -> result.add(e1 + (e2 == null ? "_null" : e2)));
        assertEquals(Arrays.asList("ab", "bc", "cd", "de"), result);

        result.clear();
        N.forEachPair(new String[] { "a" }, (e1, e2) -> result.add(e1 + (e2 == null ? "_null" : e2)));
        assertEquals(Arrays.asList("a_null"), result);

        result.clear();
        N.forEachPair(array, 2, (e1, e2) -> result.add(e1 + (e2 == null ? "_null" : e2)));
        assertEquals(Arrays.asList("ab", "cd", "e_null"), result);
    }

    @Test
    public void forEachTriple_iterable_triConsumer() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();
        N.forEachTriple(list, (e1, e2, e3) -> result.add(e1 + (e2 == null ? "_null" : e2) + (e3 == null ? "_null" : e3)));
        assertEquals(Arrays.asList("abc", "bcd", "cde"), result);

        result.clear();
        N.forEachTriple(list, 2, (e1, e2, e3) -> result.add(e1 + (e2 == null ? "_null" : e2) + (e3 == null ? "_null" : e3)));
        assertEquals(Arrays.asList("abc", "cde"), result);
    }

    @Test
    public void execute_runnable_withRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Throwables.Runnable<IOException> flakyRunnable = () -> {
            attempts.incrementAndGet();
            if (attempts.get() < 3) {
                throw new IOException("Temporary failure");
            }
        };

        N.runWithRetry(flakyRunnable, 3, 10, e -> e instanceof IOException);
        assertEquals(3, attempts.get());

        attempts.set(0);
        Throwables.Runnable<IOException> failingRunnable = () -> {
            attempts.incrementAndGet();
            throw new IOException("Persistent failure");
        };
        assertThrows(RuntimeException.class, () -> N.runWithRetry(failingRunnable, 2, 10, e -> e instanceof IOException));
        assertEquals(3, attempts.get());
    }

    @Test
    public void execute_callable_withRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> flakyCallable = () -> {
            attempts.incrementAndGet();
            if (attempts.get() < 2) {
                throw new ExecutionException("Temp fail", new IOException());
            }
            return "Success";
        };

        String result = N.callWithRetry(flakyCallable, 3, 10, (res, e) -> e != null && e.getCause() instanceof IOException);
        assertEquals("Success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void asyncExecute_runnable() throws ExecutionException, InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<Void> future = N.asyncExecute(() -> {
            Thread.sleep(50);
            executed.set(true);
        });
        future.get();
        assertTrue(executed.get());
    }

    @Test
    public void asyncExecute_callable_withExecutor() throws ExecutionException, InterruptedException {
        Callable<String> task = () -> {
            Thread.sleep(50);
            return "done";
        };
        ContinuableFuture<String> future = N.asyncExecute(task, executorService);
        assertEquals("done", future.get());
    }

    @Test
    public void asyncExecute_runnable_withDelay() throws ExecutionException, InterruptedException {
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong execTime = new AtomicLong(0);
        long delay = 100;

        ContinuableFuture<Void> future = N.asyncExecute(() -> {
            execTime.set(System.currentTimeMillis());
        }, delay);

        startTime.set(System.currentTimeMillis());
        future.get();
        assertTrue(execTime.get() - startTime.get() >= (delay - 20), "Execution was not delayed enough. Diff: " + (execTime.get() - startTime.get()));
    }

    @Test
    public void asyncExecute_listOfRunnables() throws ExecutionException, InterruptedException {
        List<Throwables.Runnable<Exception>> tasks = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        tasks.add(counter::incrementAndGet);
        tasks.add(counter::incrementAndGet);

        List<ContinuableFuture<Void>> futures = N.asyncExecute(tasks, executorService);
        for (ContinuableFuture<Void> f : futures) {
            f.get();
        }
        assertEquals(2, counter.get());
    }

    @Test
    public void asyncRun_collectionOfRunnables() {
        List<Throwables.Runnable<Exception>> tasks = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        tasks.add(() -> {
            Thread.sleep(50);
            count.incrementAndGet();
            latch.countDown();
        });
        tasks.add(() -> {
            Thread.sleep(20);
            count.incrementAndGet();
            latch.countDown();
        });

        ObjIterator<Void> iter = N.asyncRun(tasks, executorService);
        while (iter.hasNext())
            iter.next();

        try {
            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        assertEquals(2, count.get());
    }

    @Test
    public void asyncCall_collectionOfCallables() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        tasks.add(() -> {
            Thread.sleep(50);
            latch.countDown();
            return 1;
        });
        tasks.add(() -> {
            Thread.sleep(20);
            latch.countDown();
            return 2;
        });

        ObjIterator<Integer> iter = N.asyncCall(tasks, executorService);
        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        try {
            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        assertEquals(2, results.size());
        assertTrue(results.contains(1));
        assertTrue(results.contains(2));
    }

    @Test
    public void runInParallel_twoRunnables() {
        AtomicInteger count = new AtomicInteger(0);
        Throwables.Runnable<Exception> r1 = () -> {
            Thread.sleep(50);
            count.incrementAndGet();
        };
        Throwables.Runnable<Exception> r2 = () -> {
            Thread.sleep(50);
            count.incrementAndGet();
        };
        N.runInParallel(r1, r2);
        assertEquals(2, count.get());
    }

    @Test
    public void callInParallel_twoCallables() {
        Callable<String> c1 = () -> {
            Thread.sleep(50);
            return "first";
        };
        Callable<Integer> c2 = () -> {
            Thread.sleep(50);
            return 123;
        };
        Tuple.Tuple2<String, Integer> result = N.callInParallel(c1, c2);
        assertEquals("first", result._1);
        assertEquals(Integer.valueOf(123), result._2);
    }

    @Test
    public void runInParallel_collectionOfRunnables() {
        AtomicInteger count = new AtomicInteger(0);
        List<Throwables.Runnable<Exception>> tasks = Arrays.asList(() -> {
            Thread.sleep(30);
            count.incrementAndGet();
        }, () -> {
            Thread.sleep(30);
            count.incrementAndGet();
        }, () -> {
            Thread.sleep(30);
            count.incrementAndGet();
        });
        N.runInParallel(tasks, executorService);
        assertEquals(3, count.get());
    }

    @Test
    public void callInParallel_collectionOfCallables() {
        List<Callable<Integer>> tasks = Arrays.asList(() -> {
            Thread.sleep(30);
            return 1;
        }, () -> {
            Thread.sleep(30);
            return 2;
        }, () -> {
            Thread.sleep(30);
            return 3;
        });
        List<Integer> results = N.callInParallel(tasks, executorService);
        assertEquals(Arrays.asList(1, 2, 3), results);
        assertTrue(results.containsAll(Arrays.asList(1, 2, 3)) && results.size() == 3);
    }

    @Test
    public void runInParallel_exceptionHandling() {
        Throwables.Runnable<Exception> r1 = () -> {
            throw new IOException("Task 1 failed");
        };
        AtomicBoolean r2Executed = new AtomicBoolean(false);
        Throwables.Runnable<Exception> r2 = () -> {
            Thread.sleep(200);
            r2Executed.set(true);
        };

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> N.runInParallel(r1, r2));
        assertTrue(thrown.getCause() instanceof IOException);
    }

    @Test
    public void runByBatch_array() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7 };
        List<List<Integer>> batches = new ArrayList<>();
        N.runByBatch(array, 3, (Throwables.Consumer<List<Integer>, RuntimeException>) batches::add);

        assertEquals(3, batches.size());
        assertEquals(Arrays.asList(1, 2, 3), batches.get(0));
        assertEquals(Arrays.asList(4, 5, 6), batches.get(1));
        assertEquals(Arrays.asList(7), batches.get(2));
    }

    @Test
    public void runByBatch_iterable_withElementConsumerAndBatchAction() {
        List<Integer> data = Arrays.asList(10, 20, 30, 40, 50);
        AtomicInteger elementSumInBatch = new AtomicInteger(0);
        List<Integer> batchSums = new ArrayList<>();

        N.runByBatch(data, 2, (Throwables.IntObjConsumer<Integer, RuntimeException>) (idx, val) -> {
            elementSumInBatch.addAndGet(val);
        }, (Throwables.Runnable<RuntimeException>) () -> {
            batchSums.add(elementSumInBatch.get());
            elementSumInBatch.set(0);
        });

        assertEquals(3, batchSums.size());
        assertEquals(Arrays.asList(30, 70, 50), batchSums);
    }

    @Test
    public void callByBatch_iterator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> batchSums = N.callByBatch(list.iterator(), 2,
                (Throwables.Function<List<Integer>, Integer, RuntimeException>) batch -> batch.stream().mapToInt(Integer::intValue).sum());

        assertEquals(3, batchSums.size());
        assertEquals(Arrays.asList(3, 7, 5), batchSums);
    }

    @Test
    public void runUninterruptibly_runnable() {
        AtomicBoolean executed = new AtomicBoolean(false);
        N.runUninterruptibly(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void callUninterruptibly_callable() {
        String result = N.callUninterruptibly(() -> "done");
        assertEquals("done", result);
    }

    @Test
    public void tryOrEmptyIfExceptionOccurred_callable() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> "success");
        assertTrue(result.isPresent());
        assertEquals("success", result.get());

        result = N.tryOrEmptyIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void tryOrDefaultIfExceptionOccurred_callable_withDefaultValue() {
        String result = N.tryOrDefaultIfExceptionOccurred(() -> "success", "default");
        assertEquals("success", result);

        result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        }, "default");
        assertEquals("default", result);
    }

    @Test
    public void tryOrDefaultIfExceptionOccurred_callable_withDefaultSupplier() {
        Supplier<String> defaultSupplier = () -> "defaultSupplier";
        String result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> "success", defaultSupplier);
        assertEquals("success", result);

        result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        }, (java.util.function.Supplier<String>) defaultSupplier);
        assertEquals("defaultSupplier", result);
    }

    @Test
    public void ifOrEmpty_boolean_supplier() throws Exception {
        Nullable<String> result = N.ifOrEmpty(true, () -> "supplied");
        assertTrue(result.isPresent());
        assertEquals("supplied", result.get());

        result = N.ifOrEmpty(false, () -> "supplied");
        assertFalse(result.isPresent());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void ifOrElse_deprecated() {
        AtomicBoolean trueAction = new AtomicBoolean(false);
        AtomicBoolean falseAction = new AtomicBoolean(false);
        N.ifOrElse(true, () -> trueAction.set(true), () -> falseAction.set(true));
        assertTrue(trueAction.get());
        assertFalse(falseAction.get());

        trueAction.set(false);
        falseAction.set(false);
        N.ifOrElse(false, () -> trueAction.set(true), () -> falseAction.set(true));
        assertFalse(trueAction.get());
        assertTrue(falseAction.get());
    }

    @Test
    public void ifNotNull_consumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        N.ifNotNull("test", ref::set);
        assertEquals("test", ref.get());

        ref.set(null);
        N.ifNotNull(null, (String s) -> ref.set("should not happen"));
        assertNull(ref.get());
    }

    @Test
    public void ifNotEmpty_charSequence_consumer() throws Exception {
        AtomicReference<CharSequence> ref = new AtomicReference<>();
        N.ifNotEmpty("test", ref::set);
        assertEquals("test", ref.get().toString());

        ref.set(null);
        N.ifNotEmpty("", (CharSequence cs) -> ref.set("fail"));
        assertNull(ref.get());

        N.ifNotEmpty((String) null, (CharSequence cs) -> ref.set("fail"));
        assertNull(ref.get());
    }

    @Test
    public void sleep_millis() {
        long start = System.nanoTime();
        N.sleep(10);
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(duration >= 10, "Sleep duration was less than expected.");
    }

    @Test
    public void sleepUninterruptibly_millis() {
        long start = System.nanoTime();
        N.sleepUninterruptibly(10);
        long end = System.nanoTime();
        assertTrue((end - start) >= TimeUnit.MILLISECONDS.toNanos(10) - TimeUnit.MILLISECONDS.toNanos(5));
    }

    @Test
    public void lazyInit_abacusSupplier() {
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Supplier<String> lazySupplier = N.lazyInit(() -> {
            supplierCallCount.incrementAndGet();
            return "lazyValue";
        });

        assertEquals(0, supplierCallCount.get(), "Supplier should not be called before get()");
        assertEquals("lazyValue", lazySupplier.get());
        assertEquals(1, supplierCallCount.get(), "Supplier should be called once on first get()");
        assertEquals("lazyValue", lazySupplier.get());
        assertEquals(1, supplierCallCount.get(), "Supplier should not be called again on subsequent gets()");
    }

    @Test
    public void lazyInitialize_throwablesSupplier() throws Exception {
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Throwables.Supplier<String, IOException> lazySupplier = N.lazyInitialize(() -> {
            supplierCallCount.incrementAndGet();
            if (supplierCallCount.get() == 1)
                return "firstCall";
            throw new IOException("Simulated failure on subsequent init (should not happen)");
        });

        assertEquals(0, supplierCallCount.get());
        assertEquals("firstCall", lazySupplier.get());
        assertEquals(1, supplierCallCount.get());
        assertEquals("firstCall", lazySupplier.get());
        assertEquals(1, supplierCallCount.get());
    }

    @Test
    public void toRuntimeException_exception() {
        Exception checkedEx = new IOException("checked");
        RuntimeException runtimeEx = N.toRuntimeException(checkedEx);
        assertNotNull(runtimeEx);
        assertEquals(checkedEx, runtimeEx.getCause());

        RuntimeException originalRuntimeEx = new IllegalArgumentException("original_runtime");
        assertSame(originalRuntimeEx, N.toRuntimeException(originalRuntimeEx));
    }

    @Test
    public void toRuntimeException_throwable() {
        Throwable error = new OutOfMemoryError("error");
        assertThrows(OutOfMemoryError.class, () -> ExceptionUtil.toRuntimeException(error, true, true));

        Throwable checkedEx = new ClassNotFoundException("checked_throwable");
        RuntimeException runtimeEx = N.toRuntimeException(checkedEx);
        assertNotNull(runtimeEx);
        assertEquals(checkedEx, runtimeEx.getCause());
    }

    @Test
    public void println_object() {
        N.println("Test string");
        N.println(123);
        N.println(createSampleBean());
        N.println(Arrays.asList("a", "b"));
        N.println(new String[] { "c", "d" });
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        N.println(map);
        N.println((Object) null);
    }

    @Test
    public void fprintln_format() {
        N.fprintln("Hello, %s! You are %d.", "World", 30);
    }

    @Test
    public void testFilterBooleanArray() {
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.filter((boolean[]) null, b -> b));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.filter(new boolean[0], b -> b));
        assertArrayEquals(new boolean[] { true, true }, N.filter(new boolean[] { true, false, true }, b -> b));
        assertArrayEquals(new boolean[] { false }, N.filter(new boolean[] { true, false, true }, b -> !b));
    }

    @Test
    public void testFilterBooleanArrayWithRange() {
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.filter((boolean[]) null, 0, 0, b -> b));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.filter(new boolean[0], 0, 0, b -> b));

        boolean[] arr = { true, false, true, true, false };
        assertArrayEquals(new boolean[] { true, true }, N.filter(arr, 0, 3, b -> b));
        assertArrayEquals(new boolean[] { true, true }, N.filter(arr, 2, 4, b -> b));
        assertArrayEquals(new boolean[] { false }, N.filter(arr, 3, 5, b -> !b));

        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.filter(arr, 1, 1, b -> b));

        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, -1, 2, b -> b));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 0, 6, b -> b));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 3, 1, b -> b));
    }

    @Test
    public void testFilterCharArray() {
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, N.filter((char[]) null, c -> c == 'a'));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, N.filter(new char[0], c -> c == 'a'));
        assertArrayEquals(new char[] { 'a', 'a' }, N.filter(new char[] { 'a', 'b', 'a' }, c -> c == 'a'));
        assertArrayEquals(new char[] { 'b' }, N.filter(new char[] { 'a', 'b', 'a' }, c -> c == 'b'));
    }

    @Test
    public void testFilterCharArrayWithRange() {
        char[] arr = { 'x', 'y', 'x', 'z', 'x' };
        assertArrayEquals(new char[] { 'x' }, N.filter(arr, 0, 2, c -> c == 'x'));
        assertArrayEquals(new char[] { 'z', 'x' }, N.filter(arr, 3, 5, c -> c == 'z' || c == 'x'));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, N.filter(arr, 1, 1, c -> c == 'y'));
    }

    @Test
    public void testFilterByteArray() {
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, N.filter((byte[]) null, b -> b > 0));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, N.filter(new byte[0], b -> b > 0));
        assertArrayEquals(new byte[] { 1, 3 }, N.filter(new byte[] { 1, -2, 3 }, b -> b > 0));
    }

    @Test
    public void testFilterByteArrayWithRange() {
        byte[] arr = { 10, 20, -5, 30, -15 };
        assertArrayEquals(new byte[] { 10, 20 }, N.filter(arr, 0, 2, b -> b > 0));
        assertArrayEquals(new byte[] { 30 }, N.filter(arr, 2, 4, b -> b > 0));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, N.filter(arr, 1, 1, b -> b > 0));
    }

    @Test
    public void testFilterShortArray() {
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, N.filter((short[]) null, s -> s > 0));
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, N.filter(new short[0], s -> s > 0));
        assertArrayEquals(new short[] { 100, 300 }, N.filter(new short[] { 100, -200, 300 }, s -> s > 0));
    }

    @Test
    public void testFilterShortArrayWithRange() {
        short[] arr = { 1, 2, 0, 3, -1 };
        assertArrayEquals(new short[] { 1, 2 }, N.filter(arr, 0, 2, s -> s > 0));
        assertArrayEquals(new short[] { 3 }, N.filter(arr, 2, 4, s -> s > 0));
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, N.filter(arr, 1, 1, s -> s > 0));
    }

    @Test
    public void testFilterIntArray() {
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.filter((int[]) null, IS_EVEN_INT));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.filter(new int[0], IS_EVEN_INT));
        assertArrayEquals(new int[] { 2, 4 }, N.filter(new int[] { 1, 2, 3, 4, 5 }, IS_EVEN_INT));
        assertArrayEquals(new int[] { 1, 3, 5 }, N.filter(new int[] { 1, 2, 3, 4, 5 }, IS_ODD_INT));
    }

    @Test
    public void testFilterIntArrayWithRange() {
        int[] arr = { 1, 2, 3, 4, 5, 6 };
        assertArrayEquals(new int[] { 2, 4 }, N.filter(arr, 0, 4, IS_EVEN_INT));
        assertArrayEquals(new int[] { 5 }, N.filter(arr, 3, 5, IS_ODD_INT));
        assertArrayEquals(new int[] { 6 }, N.filter(arr, 5, 6, IS_EVEN_INT));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.filter(arr, 1, 1, IS_EVEN_INT));

        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, -1, 2, IS_EVEN_INT));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 0, 7, IS_EVEN_INT));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 3, 1, IS_EVEN_INT));
    }

    @Test
    public void testFilterLongArray() {
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, N.filter((long[]) null, l -> l > 0));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, N.filter(new long[0], l -> l > 0));
        assertArrayEquals(new long[] { 1L, 3L }, N.filter(new long[] { 1L, -2L, 3L }, l -> l > 0));
    }

    @Test
    public void testFilterLongArrayWithRange() {
        long[] arr = { 10L, 20L, -5L, 30L, -15L };
        assertArrayEquals(new long[] { 10L, 20L }, N.filter(arr, 0, 2, l -> l > 0));
        assertArrayEquals(new long[] { 30L }, N.filter(arr, 2, 4, l -> l > 0));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, N.filter(arr, 1, 1, l -> l > 0));
    }

    @Test
    public void testFilterFloatArray() {
        assertArrayEquals(CommonUtil.EMPTY_FLOAT_ARRAY, N.filter((float[]) null, f -> f > 0));
        assertArrayEquals(CommonUtil.EMPTY_FLOAT_ARRAY, N.filter(new float[0], f -> f > 0));
        assertArrayEquals(new float[] { 1.0f, 3.0f }, N.filter(new float[] { 1.0f, -2.0f, 3.0f }, f -> f > 0), 0.001f);
    }

    @Test
    public void testFilterFloatArrayWithRange() {
        float[] arr = { 1.f, 2.f, 0.f, 3.f, -1.f };
        assertArrayEquals(new float[] { 1.f, 2.f }, N.filter(arr, 0, 2, f -> f > 0));
        assertArrayEquals(new float[] { 3.f }, N.filter(arr, 2, 4, f -> f > 0));
        assertArrayEquals(CommonUtil.EMPTY_FLOAT_ARRAY, N.filter(arr, 1, 1, f -> f > 0));
    }

    @Test
    public void testFilterDoubleArray() {
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.filter((double[]) null, d -> d > 0));
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.filter(new double[0], d -> d > 0));
        assertArrayEquals(new double[] { 1.0, 3.0 }, N.filter(new double[] { 1.0, -2.0, 3.0 }, d -> d > 0), 0.001);
    }

    @Test
    public void testFilterDoubleArrayWithRange() {
        double[] arr = { 1., 2., 0., 3., -1. };
        assertArrayEquals(new double[] { 1., 2. }, N.filter(arr, 0, 2, d -> d > 0));
        assertArrayEquals(new double[] { 3. }, N.filter(arr, 2, 4, d -> d > 0));
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.filter(arr, 1, 1, d -> d > 0));
    }

    @Test
    public void testFilterGenericArray() {
        String[] arr = { "apple", "banana", "avocado", "grape" };
        assertEquals(List.of("apple", "avocado"), N.filter(arr, s -> s.startsWith("a")));
        assertEquals(Collections.emptyList(), N.filter((String[]) null, STRING_NOT_EMPTY));
        assertEquals(Collections.emptyList(), N.filter(new String[0], STRING_NOT_EMPTY));
    }

    @Test
    public void testFilterGenericArrayWithSupplier() {
        String[] arr = { "one", "two", "three", "four" };
        Set<String> result = N.filter(arr, s -> s.length() == 3, size -> new HashSet<>());
        assertEquals(Set.of("one", "two"), result);

        List<String> listResult = N.filter(arr, s -> s.length() > 3, IntFunctions.ofList());
        assertEquals(List.of("three", "four"), listResult);

        assertTrue(N.filter((String[]) null, STRING_NOT_EMPTY, IntFunctions.ofList()).isEmpty());
        assertTrue(N.filter(new String[0], STRING_NOT_EMPTY, IntFunctions.ofSet()).isEmpty());

    }

    @Test
    public void testFilterGenericArrayWithRange() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        assertEquals(List.of(2, 4), N.filter(arr, 0, 4, IS_EVEN_INTEGER));
        assertEquals(List.of(5), N.filter(arr, 3, 5, x -> x % 2 != 0));
        assertTrue(N.filter(arr, 1, 1, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testFilterGenericArrayWithRangeAndSupplier() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        Set<Integer> resultSet = N.filter(arr, 0, 4, IS_EVEN_INTEGER, size -> new HashSet<>());
        assertEquals(Set.of(2, 4), resultSet);

        List<Integer> resultList = N.filter(arr, 3, 5, x -> x % 2 != 0, IntFunctions.ofList());
        assertEquals(List.of(5), resultList);

        assertTrue(N.filter(arr, 1, 1, IS_EVEN_INTEGER, IntFunctions.ofList()).isEmpty());
    }

    @Test
    public void testFilterCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "aa", "bb", "aaa");
        assertEquals(List.of("aa", "aaa"), N.filter(list, 1, 5, s -> s.contains("a") && s.length() > 1));
        assertTrue(N.filter(list, 1, 1, STRING_NOT_EMPTY).isEmpty());
        assertEquals(List.of("b", "bb"), N.filter(list, 1, 4, s -> s.startsWith("b")));
    }

    @Test
    public void testFilterIterable() {
        Iterable<String> iter = Arrays.asList("apple", "banana", "", "grape");
        assertEquals(List.of("apple", "banana", "grape"), N.filter(iter, STRING_NOT_EMPTY));
        assertTrue(N.filter((Iterable<String>) null, STRING_NOT_EMPTY).isEmpty());
        assertTrue(N.filter(Collections.emptyList(), STRING_NOT_EMPTY).isEmpty());
    }

    @Test
    public void testFilterIterableWithSupplier() {
        Iterable<String> iter = Arrays.asList("apple", "banana", "apricot", "grape");
        Set<String> resultSet = N.filter(iter, s -> s.startsWith("a"), HashSet::new);
        assertEquals(Set.of("apple", "apricot"), resultSet);
    }

    @Test
    public void testFilterIterator() {
        Iterator<String> iter = Arrays.asList("one", "two", "", "three").iterator();
        assertEquals(List.of("one", "two", "three"), N.filter(iter, STRING_NOT_EMPTY));
        assertTrue(N.filter((Iterator<String>) null, STRING_NOT_EMPTY).isEmpty());
        assertTrue(N.filter(Collections.emptyIterator(), STRING_NOT_EMPTY).isEmpty());
    }

    @Test
    public void testFilterIteratorWithSupplier() {
        Iterator<String> iter = Arrays.asList("one", "two", "three", "onetwo").iterator();
        Set<String> resultSet = N.filter(iter, s -> s.length() == 3, HashSet::new);
        assertEquals(Set.of("one", "two"), resultSet);
    }

    @Test
    public void testMapToBooleanArray() {
        String[] arr = { "true", "false", "TRUE" };
        assertArrayEquals(new boolean[] { true, false, true }, N.mapToBoolean(arr, Boolean::parseBoolean));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean((String[]) null, Boolean::parseBoolean));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(new String[0], Boolean::parseBoolean));
    }

    @Test
    public void testMapToBooleanArrayWithRange() {
        String[] arr = { "true", "false", "TRUE", "yes" };
        assertArrayEquals(new boolean[] { false, true }, N.mapToBoolean(arr, 1, 3, Boolean::parseBoolean));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(arr, 1, 1, Boolean::parseBoolean));
    }

    @Test
    public void testMapToBooleanCollection() {
        List<String> list = Arrays.asList("true", "false", "TRUE");
        assertArrayEquals(new boolean[] { true, false, true }, N.mapToBoolean(list, Boolean::parseBoolean));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean((Collection<String>) null, Boolean::parseBoolean));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(Collections.emptyList(), Boolean::parseBoolean));
    }

    @Test
    public void testMapToBooleanCollectionWithRange() {
        List<String> list = Arrays.asList("true", "false", "TRUE", "no");
        assertArrayEquals(new boolean[] { false, true }, N.mapToBoolean(list, 1, 3, Boolean::parseBoolean));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(list, 0, 0, Boolean::parseBoolean));

        Collection<String> nonRaList = new LinkedList<>(list);
        assertArrayEquals(new boolean[] { false, true }, N.mapToBoolean(nonRaList, 1, 3, Boolean::parseBoolean));
    }

    @Test
    public void testMapToCharArray() {
        String[] arr = { "apple", "cat" };
        assertArrayEquals(new char[] { 'a', 'c' }, N.mapToChar(arr, s -> s.charAt(0)));
    }

    @Test
    public void testMapToCharArrayWithRange() {
        String[] arr = { "apple", "banana", "cat" };
        assertArrayEquals(new char[] { 'b', 'c' }, N.mapToChar(arr, 1, 3, s -> s.charAt(0)));
    }

    @Test
    public void testMapToCharCollection() {
        List<String> list = Arrays.asList("hello", "world");
        assertArrayEquals(new char[] { 'h', 'w' }, N.mapToChar(list, s -> s.charAt(0)));
    }

    @Test
    public void testMapToCharCollectionWithRange() {
        List<String> list = Arrays.asList("one", "two", "three");
        assertArrayEquals(new char[] { 't', 't' }, N.mapToChar(list, 1, 3, s -> s.charAt(0)));
    }

    @Test
    public void testMapToByteArray() {
        Integer[] arr = { 10, 20, -10 };
        assertArrayEquals(new byte[] { 10, 20, -10 }, N.mapToByte(arr, Integer::byteValue));
    }

    @Test
    public void testMapToByteArrayWithRange() {
        Integer[] arr = { 10, 20, 30, 40 };
        assertArrayEquals(new byte[] { 20, 30 }, N.mapToByte(arr, 1, 3, Integer::byteValue));
    }

    @Test
    public void testMapToByteCollection() {
        List<Integer> list = Arrays.asList(5, 15, 25);
        assertArrayEquals(new byte[] { 5, 15, 25 }, N.mapToByte(list, Integer::byteValue));
    }

    @Test
    public void testMapToByteCollectionWithRange() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertArrayEquals(new byte[] { 2, 3, 4 }, N.mapToByte(list, 1, 4, Integer::byteValue));
    }

    @Test
    public void testMapToShortArray() {
        Integer[] arr = { 100, 200, 300 };
        assertArrayEquals(new short[] { 100, 200, 300 }, N.mapToShort(arr, Integer::shortValue));
    }

    @Test
    public void testMapToShortArrayWithRange() {
        Integer[] arr = { 100, 200, 300, 400 };
        assertArrayEquals(new short[] { 200, 300 }, N.mapToShort(arr, 1, 3, Integer::shortValue));
    }

    @Test
    public void testMapToShortCollection() {
        List<Integer> list = Arrays.asList(10, 20, 30);
        assertArrayEquals(new short[] { 10, 20, 30 }, N.mapToShort(list, Integer::shortValue));
    }

    @Test
    public void testMapToShortCollectionWithRange() {
        List<Integer> list = Arrays.asList(10, 20, 30, 40, 50);
        assertArrayEquals(new short[] { 20, 30, 40 }, N.mapToShort(list, 1, 4, Integer::shortValue));
    }

    @Test
    public void testMapToIntArray() {
        String[] arr = { "1", "22", "333" };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.mapToInt(arr, String::length));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt((String[]) null, String::length));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt(new String[0], String::length));
    }

    @Test
    public void testMapToIntArrayWithRange() {
        String[] arr = { "a", "bb", "ccc", "dddd" };
        assertArrayEquals(new int[] { 2, 3 }, N.mapToInt(arr, 1, 3, String::length));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt(arr, 2, 2, String::length));
    }

    @Test
    public void testMapToIntCollection() {
        List<String> list = Arrays.asList("one", "two", "three");
        assertArrayEquals(new int[] { 3, 3, 5 }, N.mapToInt(list, String::length));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt((Collection<String>) null, String::length));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt(Collections.emptyList(), String::length));
    }

    @Test
    public void testMapToIntCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        assertArrayEquals(new int[] { 6, 6 }, N.mapToInt(list, 1, 3, String::length));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt(list, 0, 0, String::length));

        Collection<String> nonRaList = new LinkedList<>(list);
        assertArrayEquals(new int[] { 6, 6 }, N.mapToInt(nonRaList, 1, 3, String::length));
    }

    @Test
    public void testMapToIntFromLongArray() {
        long[] arr = { 1L, 10000000000L, 3L };
        assertArrayEquals(new int[] { 1, (int) 10000000000L, 3 }, N.mapToInt(arr, l -> (int) l));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt((long[]) null, l -> (int) l));
    }

    @Test
    public void testMapToIntFromDoubleArray() {
        double[] arr = { 1.1, 2.9, 3.5 };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.mapToInt(arr, d -> (int) d));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt((double[]) null, d -> (int) d));
    }

    @Test
    public void testMapToLongArray() {
        String[] arr = { "1", "22", "333" };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(arr, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongArrayWithRange() {
        String[] arr = { "a", "bb", "ccc", "dddd" };
        assertArrayEquals(new long[] { 2L, 3L }, N.mapToLong(arr, 1, 3, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongCollection() {
        List<String> list = Arrays.asList("one", "two", "three");
        assertArrayEquals(new long[] { 3L, 3L, 5L }, N.mapToLong(list, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        assertArrayEquals(new long[] { 6L }, N.mapToLong(list, 1, 2, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongFromIntArray() {
        int[] arr = { 1, 2, 3 };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(arr, i -> (long) i));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, N.mapToLong((int[]) null, i -> (long) i));
    }

    @Test
    public void testMapToLongFromDoubleArray() {
        double[] arr = { 1.1, 2.9, 3.5 };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(arr, d -> (long) d));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, N.mapToLong((double[]) null, d -> (long) d));
    }

    @Test
    public void testMapToFloatArray() {
        String[] arr = { "1.1", "2.2", "3.3" };
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, N.mapToFloat(arr, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToFloatArrayWithRange() {
        String[] arr = { "1.0", "2.5", "3.0", "4.5" };
        assertArrayEquals(new float[] { 2.5f, 3.0f }, N.mapToFloat(arr, 1, 3, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToFloatCollection() {
        List<String> list = Arrays.asList("0.5", "1.5");
        assertArrayEquals(new float[] { 0.5f, 1.5f }, N.mapToFloat(list, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToFloatCollectionWithRange() {
        List<String> list = Arrays.asList("1.0", "2.5", "3.0", "4.5");
        assertArrayEquals(new float[] { 2.5f, 3.0f }, N.mapToFloat(list, 1, 3, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToDoubleArray() {
        String[] arr = { "1.1", "2.2", "3.3" };
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, N.mapToDouble(arr, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleArrayWithRange() {
        String[] arr = { "1.0", "2.5", "3.0", "4.5" };
        assertArrayEquals(new double[] { 2.5, 3.0 }, N.mapToDouble(arr, 1, 3, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleCollection() {
        List<String> list = Arrays.asList("0.5", "1.5");
        assertArrayEquals(new double[] { 0.5, 1.5 }, N.mapToDouble(list, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleCollectionWithRange() {
        List<String> list = Arrays.asList("1.0", "2.5", "3.0", "4.5");
        assertArrayEquals(new double[] { 2.5, 3.0 }, N.mapToDouble(list, 1, 3, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleFromIntArray() {
        int[] arr = { 1, 2, 3 };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.mapToDouble(arr, i -> (double) i), 0.001);
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.mapToDouble((int[]) null, i -> (double) i));
    }

    @Test
    public void testMapToDoubleFromLongArray() {
        long[] arr = { 1L, 2L, 3L };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.mapToDouble(arr, l -> (double) l), 0.001);
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.mapToDouble((long[]) null, l -> (double) l));
    }

    @Test
    public void testMapArray() {
        String[] arr = { "a", "bb", "ccc" };
        List<Integer> expected = Arrays.asList(1, 2, 3);
        assertEquals(expected, N.map(arr, String::length));
        assertTrue(N.map((String[]) null, String::length).isEmpty());
        assertTrue(N.map(new String[0], String::length).isEmpty());
    }

    @Test
    public void testMapArrayWithSupplier() {
        String[] arr = { "a", "bb", "a" };
        Set<Integer> expected = Set.of(1, 2);
        assertEquals(expected, N.map(arr, String::length, size -> new HashSet<>()));
        assertTrue(N.map((String[]) null, String::length, IntFunctions.ofList()).isEmpty());
    }

    @Test
    public void testMapArrayWithRange() {
        String[] arr = { "one", "two", "three", "four" };
        List<Integer> expected = Arrays.asList(3, 5);
        assertEquals(expected, N.map(arr, 1, 3, String::length));
        assertTrue(N.map(arr, 1, 1, String::length).isEmpty());
    }

    @Test
    public void testMapArrayWithRangeAndSupplier() {
        String[] arr = { "one", "two", "three", "two" };
        Set<Integer> expected = Set.of(3, 5);
        assertEquals(expected, N.map(arr, 1, 4, String::length, HashSet::new));
    }

    @Test
    public void testMapCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        assertEquals(List.of("BANANA", "CHERRY"), N.map(list, 1, 3, String::toUpperCase));
        assertTrue(N.map(list, 0, 0, String::toUpperCase).isEmpty());
    }

    @Test
    public void testMapCollectionWithRangeAndSupplier() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana");
        Set<String> expected = Set.of("BANANA", "CHERRY");
        assertEquals(expected, N.map(list, 1, 4, String::toUpperCase, HashSet::new));
    }

    @Test
    public void testMapIterable() {
        Iterable<Integer> iter = Arrays.asList(1, 2, 3);
        assertEquals(Arrays.asList("1", "2", "3"), N.map(iter, String::valueOf));
        assertTrue(N.map((Iterable<Integer>) null, String::valueOf).isEmpty());
    }

    @Test
    public void testMapIterableWithSupplier() {
        Iterable<Integer> iter = Arrays.asList(1, 2, 1, 3);
        assertEquals(Set.of("1", "2", "3"), N.map(iter, String::valueOf, HashSet::new));
    }

    @Test
    public void testMapIterator() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
        assertEquals(Arrays.asList("1", "2", "3"), N.map(iter, String::valueOf));
        assertTrue(N.map((Iterator<Integer>) null, String::valueOf).isEmpty());
    }

    @Test
    public void testMapIteratorWithSupplier() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 1, 3).iterator();
        assertEquals(Set.of("1", "2", "3"), N.map(iter, String::valueOf, HashSet::new));
    }

    @Test
    public void testFlatMapArray() {
        String[] arr = { "ab", "c" };
        List<String> expected = Arrays.asList("a", "b", "c");
        assertEquals(expected, N.flatMap(arr, NTest::splitToChars));
        assertTrue(N.flatMap((String[]) null, NTest::splitToChars).isEmpty());
    }

    @Test
    public void testFlatMapArrayWithSupplier() {
        String[] arr = { "ab", "ca" };
        Set<String> expected = Set.of("a", "b", "c");
        assertEquals(expected, N.flatMap(arr, NTest::splitToChars, IntFunctions.ofSet()));
    }

    @Test
    public void testFlatMapArrayWithRange() {
        String[] arr = { "hi", "world", "test" };
        List<String> expected = List.of('w', 'o', 'r', 'l', 'd').stream().map(String::valueOf).collect(Collectors.toList());
        assertEquals(expected, N.flatMap(arr, 1, 2, NTest::splitToChars));
    }

    @Test
    public void testFlatMapArrayWithRangeAndSupplier() {
        String[] arr = { "hi", "bob", "test", "bib" };
        Set<String> expected = Set.of("b", "o");
        assertEquals(expected, N.flatMap(arr, 1, 2, NTest::splitToChars, HashSet::new));
    }

    @Test
    public void testFlatMapCollectionWithRange() {
        List<String> list = Arrays.asList("key", "lock", "door");
        List<String> expected = List.of('l', 'o', 'c', 'k', 'd', 'o', 'o', 'r').stream().map(String::valueOf).collect(Collectors.toList());
        assertEquals(expected, N.flatMap(list, 1, 3, NTest::splitToChars));
    }

    @Test
    public void testFlatMapCollectionWithRangeAndSupplier() {
        List<String> list = Arrays.asList("key", "lock", "lol");
        Set<String> expected = Set.of("l", "o", "c", "k");
        assertEquals(expected, N.flatMap(list, 1, 3, NTest::splitToChars, HashSet::new));
    }

    @Test
    public void testFlatMapIterable() {
        Iterable<String> iter = Arrays.asList("one", "two");
        assertEquals(List.of("o", "n", "e", "t", "w", "o"), N.flatMap(iter, NTest::splitToChars));
    }

    @Test
    public void testFlatMapIterableWithSupplier() {
        Iterable<String> iter = Arrays.asList("one", "too");
        assertEquals(Set.of("o", "n", "e", "t"), N.flatMap(iter, NTest::splitToChars, IntFunctions.ofSet()));
    }

    @Test
    public void testFlatMapIterator() {
        Iterator<String> iter = Arrays.asList("hi", "bye").iterator();
        assertEquals(List.of("h", "i", "b", "y", "e"), N.flatMap(iter, NTest::splitToChars));
    }

    @Test
    public void testFlatMapIteratorWithSupplier() {
        Iterator<String> iter = Arrays.asList("hi", "bib").iterator();
        assertEquals(Set.of("h", "i", "b"), N.flatMap(iter, NTest::splitToChars, IntFunctions.ofSet()));
    }

    @Test
    public void testFlatMapArrayTwoLevels() {
        String[] arr = { "ab-cd", "ef" };
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", "f");
        assertEquals(expected, N.flatMap(arr, splitByDash, NTest::splitToChars));
    }

    @Test
    public void testFlatMapArrayTwoLevelsWithSupplier() {
        String[] arr = { "ab-ca", "ef-fa" };
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        Set<String> expected = Set.of("a", "b", "c", "e", "f");
        assertEquals(expected, N.flatMap(arr, splitByDash, NTest::splitToChars, HashSet::new));
    }

    @Test
    public void testFlatMapIterableTwoLevels() {
        List<String> list = Arrays.asList("uv-wx", "yz");
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        List<String> expected = Arrays.asList("u", "v", "w", "x", "y", "z");
        assertEquals(expected, N.flatMap(list, splitByDash, NTest::splitToChars));
    }

    @Test
    public void testFlatMapIterableTwoLevelsWithSupplier() {
        List<String> list = Arrays.asList("uv-wa", "yz-za");
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        Set<String> expected = Set.of("u", "v", "w", "a", "y", "z");
        assertEquals(expected, N.flatMap(list, splitByDash, NTest::splitToChars, HashSet::new));
    }

    @Test
    public void testFlatMapIteratorTwoLevels() {
        Iterator<String> iter = Arrays.asList("12-34", "56").iterator();
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        List<String> expected = Arrays.asList("1", "2", "3", "4", "5", "6");
        assertEquals(expected, N.flatMap(iter, splitByDash, NTest::splitToChars));
    }

    @Test
    public void testFlatMapIteratorTwoLevelsWithSupplier() {
        Iterator<String> iter = Arrays.asList("12-31", "56-65").iterator();
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        Set<String> expected = Set.of("1", "2", "3", "5", "6");
        assertEquals(expected, N.flatMap(iter, splitByDash, NTest::splitToChars, HashSet::new));
    }

    @Test
    public void testTakeWhileArray() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(2, 4), N.takeWhile(arr, IS_EVEN_INTEGER));
        assertTrue(N.takeWhile((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertTrue(N.takeWhile(new Integer[0], IS_EVEN_INTEGER).isEmpty());
        assertEquals(List.of(1, 3), N.takeWhile(new Integer[] { 1, 3, 2, 5 }, x -> x % 2 != 0));
        assertTrue(N.takeWhile(new Integer[] { 1, 2, 3 }, x -> x > 10).isEmpty());
    }

    @Test
    public void testTakeWhileIterable() {
        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertEquals(List.of(2, 4), N.takeWhile(list, IS_EVEN_INTEGER));
        assertTrue(N.takeWhile((Iterable<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testTakeWhileIterator() {
        Iterator<Integer> iter = Arrays.asList(2, 4, 5, 6, 8).iterator();
        assertEquals(List.of(2, 4), N.takeWhile(iter, IS_EVEN_INTEGER));
        assertTrue(N.takeWhile((Iterator<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testTakeWhileInclusiveArray() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(arr, IS_EVEN_INTEGER));
        assertEquals(List.of(1, 3, 2), N.takeWhileInclusive(new Integer[] { 1, 3, 2, 5 }, x -> x % 2 != 0));
        assertEquals(List.of(10), N.takeWhileInclusive(new Integer[] { 10, 1, 2 }, x -> x < 5));
        assertTrue(N.takeWhileInclusive((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testTakeWhileInclusiveIterable() {
        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(list, IS_EVEN_INTEGER));
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        Iterator<Integer> iter = Arrays.asList(2, 4, 5, 6, 8).iterator();
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(iter, IS_EVEN_INTEGER));
    }

    @Test
    public void testDropWhileArray() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(5, 6, 8), N.dropWhile(arr, IS_EVEN_INTEGER));
        assertTrue(N.dropWhile((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertEquals(List.of(2, 5), N.dropWhile(new Integer[] { 1, 3, 2, 5 }, x -> x % 2 != 0));
        assertEquals(List.of(1, 2, 3), N.dropWhile(new Integer[] { 1, 2, 3 }, x -> x > 10));
    }

    @Test
    public void testDropWhileIterable() {
        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertEquals(List.of(5, 6, 8), N.dropWhile(list, IS_EVEN_INTEGER));
        assertTrue(N.dropWhile((Iterable<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testDropWhileIterator() {
        Iterator<Integer> iter = Arrays.asList(2, 4, 5, 6, 8).iterator();
        assertEquals(List.of(5, 6, 8), N.dropWhile(iter, IS_EVEN_INTEGER));
        assertTrue(N.dropWhile((Iterator<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testSkipUntilArray() {
        Integer[] arr = { 1, 3, 4, 5, 6 };
        assertEquals(List.of(4, 5, 6), N.skipUntil(arr, IS_EVEN_INTEGER));
        assertTrue(N.skipUntil((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertEquals(List.of(2, 5), N.skipUntil(new Integer[] { 1, 3, 2, 5 }, IS_EVEN_INTEGER));
        assertTrue(N.skipUntil(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testSkipUntilIterable() {
        List<Integer> list = Arrays.asList(1, 3, 4, 5, 6);
        assertEquals(List.of(4, 5, 6), N.skipUntil(list, IS_EVEN_INTEGER));
    }

    @Test
    public void testSkipUntilIterator() {
        Iterator<Integer> iter = Arrays.asList(1, 3, 4, 5, 6).iterator();
        assertEquals(List.of(4, 5, 6), N.skipUntil(iter, IS_EVEN_INTEGER));
    }

    @Test
    public void testMapAndFilterIterable() {
        List<String> input = Arrays.asList("1", "2a", "3", "4b", "5");
        Function<String, Integer> mapper = s -> s.length();
        Predicate<Integer> filter = i -> i == 1;
        List<Integer> result = N.mapAndFilter(input, mapper, filter);
        assertEquals(Arrays.asList(1, 1, 1), result);

        assertTrue(N.mapAndFilter(null, mapper, filter).isEmpty());
        assertTrue(N.mapAndFilter(Collections.emptyList(), mapper, filter).isEmpty());
    }

    @Test
    public void testMapAndFilterIterableWithSupplier() {
        List<String> input = Arrays.asList("1", "2a", "3", "2a", "5");
        Function<String, Integer> mapper = s -> s.length();
        Predicate<Integer> filter = i -> i == 2;
        Set<Integer> result = N.mapAndFilter(input, mapper, filter, size -> new HashSet<>());
        assertEquals(Set.of(2), result);
    }

    @Test
    public void testFilterAndMapIterable() {
        List<String> input = Arrays.asList("apple", "banana", "kiwi", "avocado");
        Predicate<String> filter = s -> s.startsWith("a");
        Function<String, Integer> mapper = String::length;
        List<Integer> result = N.filterAndMap(input, filter, mapper);
        assertEquals(Arrays.asList(5, 7), result);

        assertTrue(N.filterAndMap(null, filter, mapper).isEmpty());
    }

    @Test
    public void testFilterAndMapIterableWithSupplier() {
        List<String> input = Arrays.asList("apple", "apricot", "banana", "apricot");
        Predicate<String> filter = s -> s.length() > 5;
        Function<String, String> mapper = String::toUpperCase;
        Set<String> result = N.filterAndMap(input, filter, mapper, HashSet::new);
        assertEquals(Set.of("APRICOT", "BANANA"), result);
    }

    @Test
    public void testFlatMapAndFilterIterable() {
        List<String> input = Arrays.asList("a b", "c", "d e f");
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" "));
        Predicate<String> filter = s -> s.length() == 1;
        List<String> result = N.flatMapAndFilter(input, mapper, filter);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);

        assertTrue(N.flatMapAndFilter(null, mapper, filter).isEmpty());
    }

    @Test
    public void testFlatMapAndFilterIterableWithSupplier() {
        List<String> input = Arrays.asList("a b a", "c c", "d e f d");
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" "));
        Predicate<String> filter = s -> s.length() == 1;
        Set<String> result = N.flatMapAndFilter(input, mapper, filter, HashSet::new);
        assertEquals(Set.of("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testFilterAndFlatMapIterable() {
        List<String> input = Arrays.asList("one two", "three", "four five six", "seven");
        Predicate<String> filter = s -> s.contains(" ");
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" "));
        List<String> result = N.filterAndFlatMap(input, filter, mapper);
        assertEquals(Arrays.asList("one", "two", "four", "five", "six"), result);

        assertTrue(N.filterAndFlatMap(null, filter, mapper).isEmpty());
    }

    @Test
    public void testFilterAndFlatMapIterableWithSupplier() {
        List<String> input = Arrays.asList("one two one", "three", "four five six four", "seven");
        Predicate<String> filter = s -> s.contains(" ");
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" "));
        Set<String> result = N.filterAndFlatMap(input, filter, mapper, HashSet::new);
        assertEquals(Set.of("one", "two", "four", "five", "six"), result);
    }

    @Test
    public void testDistinctIntArray() {
        assertArrayEquals(new int[] { 1, 2, 3 }, N.distinct(new int[] { 1, 2, 2, 3, 1, 3 }));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.distinct((int[]) null));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.distinct(new int[0]));
        assertArrayEquals(new int[] { 1 }, N.distinct(new int[] { 1, 1, 1 }));
    }

    @Test
    public void testDistinctIntArrayWithRange() {
        int[] arr = { 1, 2, 1, 3, 2, 4, 1 };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.distinct(arr, 0, 4));
        assertArrayEquals(new int[] { 2, 4, 1 }, N.distinct(arr, 4, 7));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.distinct(arr, 2, 2));
    }

    @Test
    public void testDistinctGenericArray() {
        String[] arr = { "a", "b", "a", "c", "b" };
        assertEquals(List.of("a", "b", "c"), N.distinct(arr));
        assertTrue(N.distinct((String[]) null).isEmpty());
    }

    @Test
    public void testDistinctGenericArrayWithRange() {
        String[] arr = { "a", "b", "a", "c", "b", "d", "a" };
        assertEquals(List.of("a", "b", "c"), N.distinct(arr, 0, 4));
        assertEquals(List.of("b", "d", "a"), N.distinct(arr, 4, 7));
    }

    @Test
    public void testDistinctCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "d", "a");
        assertEquals(List.of("a", "b", "c"), N.distinct(list, 0, 4));
    }

    @Test
    public void testDistinctIterable() {
        Iterable<String> iter = Arrays.asList("x", "y", "x", "z", "y");
        assertEquals(List.of("x", "y", "z"), N.distinct(iter));
        assertTrue(N.distinct((Iterable<String>) null).isEmpty());

        Set<String> set = Set.of("1", "2", "3");
        List<String> distinctFromSet = N.distinct(set);
        assertEquals(3, distinctFromSet.size());
        assertTrue(distinctFromSet.containsAll(Set.of("1", "2", "3")));
    }

    @Test
    public void testDistinctIterator() {
        Iterator<String> iter = Arrays.asList("x", "y", "x", "z", "y").iterator();
        assertEquals(List.of("x", "y", "z"), N.distinct(iter));
        assertTrue(N.distinct((Iterator<String>) null).isEmpty());
    }

    @Test
    public void testDistinctByArray() {
        String[] arr = { "apple", "apricot", "banana", "blueberry" };
        List<String> result = N.distinctBy(arr, s -> s.charAt(0));
        assertEquals(List.of("apple", "banana"), result);
        assertTrue(N.distinctBy((String[]) null, s -> s.charAt(0)).isEmpty());
    }

    @Test
    public void testDistinctByArrayWithRange() {
        String[] arr = { "apple", "apricot", "banana", "avocado", "blueberry" };
        List<String> result = N.distinctBy(arr, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("apricot", "banana"), result);
    }

    @Test
    public void testDistinctByArrayWithSupplier() {
        String[] arr = { "apple", "apricot", "banana", "apricot" };
        Set<String> result = N.distinctBy(arr, s -> s.charAt(0), HashSet::new);
        assertEquals(Set.of("apple", "banana"), result);
    }

    @Test
    public void testDistinctByCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "blueberry");
        List<String> result = N.distinctBy(list, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("apricot", "banana"), result);
    }

    @Test
    public void testDistinctByIterable() {
        List<String> list = Arrays.asList("cat", "cow", "dog", "deer");
        List<String> result = N.distinctBy(list, s -> s.charAt(0));
        assertEquals(List.of("cat", "dog"), result);
    }

    @Test
    public void testDistinctByIterableWithSupplier() {
        List<String> list = Arrays.asList("cat", "cow", "dog", "deer", "dove");
        Set<String> result = N.distinctBy(list, s -> s.charAt(0), HashSet::new);
        assertEquals(Set.of("cat", "dog"), result);
    }

    @Test
    public void testDistinctByIterator() {
        Iterator<String> iter = Arrays.asList("cat", "cow", "dog", "deer").iterator();
        List<String> result = N.distinctBy(iter, s -> s.charAt(0));
        assertEquals(List.of("cat", "dog"), result);
    }

    @Test
    public void testDistinctByIteratorWithSupplier() {
        Iterator<String> iter = Arrays.asList("cat", "cow", "dog", "deer", "dove").iterator();
        Set<String> result = N.distinctBy(iter, s -> s.charAt(0), HashSet::new);
        assertEquals(Set.of("cat", "dog"), result);
    }

    @Test
    public void testAllMatchArray() {
        assertTrue(N.allMatch(new Integer[] { 2, 4, 6 }, IS_EVEN_INTEGER));
        assertFalse(N.allMatch(new Integer[] { 2, 3, 6 }, IS_EVEN_INTEGER));
        assertTrue(N.allMatch((Integer[]) null, IS_EVEN_INTEGER));
        assertTrue(N.allMatch(new Integer[0], IS_EVEN_INTEGER));
    }

    @Test
    public void testAllMatchIterable() {
        assertTrue(N.allMatch(List.of(2, 4, 6), IS_EVEN_INTEGER));
        assertFalse(N.allMatch(List.of(2, 3, 6), IS_EVEN_INTEGER));
    }

    @Test
    public void testAllMatchIterator() {
        assertTrue(N.allMatch(List.of(2, 4, 6).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.allMatch(List.of(2, 3, 6).iterator(), IS_EVEN_INTEGER));
    }

    @Test
    public void testAnyMatchArray() {
        assertTrue(N.anyMatch(new Integer[] { 1, 3, 4 }, IS_EVEN_INTEGER));
        assertFalse(N.anyMatch(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER));
        assertFalse(N.anyMatch((Integer[]) null, IS_EVEN_INTEGER));
    }

    @Test
    public void testAnyMatchIterable() {
        assertTrue(N.anyMatch(List.of(1, 3, 4), IS_EVEN_INTEGER));
        assertFalse(N.anyMatch(List.of(1, 3, 5), IS_EVEN_INTEGER));
    }

    @Test
    public void testAnyMatchIterator() {
        assertTrue(N.anyMatch(List.of(1, 3, 4).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.anyMatch(List.of(1, 3, 5).iterator(), IS_EVEN_INTEGER));
    }

    @Test
    public void testNoneMatchArray() {
        assertTrue(N.noneMatch(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER));
        assertFalse(N.noneMatch(new Integer[] { 1, 2, 5 }, IS_EVEN_INTEGER));
        assertTrue(N.noneMatch((Integer[]) null, IS_EVEN_INTEGER));
    }

    @Test
    public void testNoneMatchIterable() {
        assertTrue(N.noneMatch(List.of(1, 3, 5), IS_EVEN_INTEGER));
        assertFalse(N.noneMatch(List.of(1, 2, 5), IS_EVEN_INTEGER));
    }

    @Test
    public void testNoneMatchIterator() {
        assertTrue(N.noneMatch(List.of(1, 3, 5).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.noneMatch(List.of(1, 2, 5).iterator(), IS_EVEN_INTEGER));
    }

    @Test
    public void testNMatchArray() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        assertTrue(N.isMatchCountBetween(arr, 3, 3, IS_EVEN_INTEGER));
        assertTrue(N.isMatchCountBetween(arr, 2, 4, IS_EVEN_INTEGER));
        assertFalse(N.isMatchCountBetween(arr, 4, 5, IS_EVEN_INTEGER));
        assertTrue(N.isMatchCountBetween(arr, 0, 0, x -> x > 10));
        assertFalse(N.isMatchCountBetween(arr, 1, 1, x -> x > 10));

        assertTrue(N.isMatchCountBetween((Integer[]) null, 0, 0, IS_EVEN_INTEGER));
        assertFalse(N.isMatchCountBetween((Integer[]) null, 1, 1, IS_EVEN_INTEGER));

        assertThrows(IllegalArgumentException.class, () -> N.isMatchCountBetween(arr, -1, 2, IS_EVEN_INTEGER));
        assertThrows(IllegalArgumentException.class, () -> N.isMatchCountBetween(arr, 3, 1, IS_EVEN_INTEGER));
    }

    @Test
    public void testNMatchIterable() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6);
        assertTrue(N.isMatchCountBetween(list, 3, 3, IS_EVEN_INTEGER));
        assertTrue(N.isMatchCountBetween(list, 1, 2, x -> x > 5));
        assertTrue(N.isMatchCountBetween(list, 1, 1, x -> x > 5));
    }

    @Test
    public void testNMatchIterator() {
        Iterator<Integer> iter = List.of(1, 2, 3, 4, 5, 6).iterator();
        assertTrue(N.isMatchCountBetween(iter, 3, 3, IS_EVEN_INTEGER));
    }

    @Test
    public void testAllTrue() {
        assertTrue(N.allTrue(new boolean[] { true, true, true }));
        assertFalse(N.allTrue(new boolean[] { true, false, true }));
        assertTrue(N.allTrue(null));
        assertTrue(N.allTrue(new boolean[0]));
    }

    @Test
    public void testAllFalse() {
        assertTrue(N.allFalse(new boolean[] { false, false, false }));
        assertFalse(N.allFalse(new boolean[] { false, true, false }));
        assertTrue(N.allFalse(null));
    }

    @Test
    public void testAnyTrue() {
        assertTrue(N.anyTrue(new boolean[] { false, true, false }));
        assertFalse(N.anyTrue(new boolean[] { false, false, false }));
        assertFalse(N.anyTrue(null));
    }

    @Test
    public void testAnyFalse() {
        assertTrue(N.anyFalse(new boolean[] { true, false, true }));
        assertFalse(N.anyFalse(new boolean[] { true, true, true }));
        assertFalse(N.anyFalse(null));
    }

    @Test
    public void testCountBooleanArray() {
        assertEquals(2, N.count(new boolean[] { true, false, true, false }, b -> b));
        assertEquals(0, N.count((boolean[]) null, b -> b));
    }

    @Test
    public void testCountBooleanArrayWithRange() {
        assertEquals(1, N.count(new boolean[] { true, false, true, false }, 1, 3, b -> b));
    }

    @Test
    public void testCountIntArray() {
        assertEquals(3, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, IS_EVEN_INT));
        assertEquals(0, N.count((int[]) null, IS_EVEN_INT));
        assertEquals(0, N.count(new int[0], IS_EVEN_INT));
    }

    @Test
    public void testCountIntArrayWithRange() {
        assertEquals(2, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, 0, 4, IS_EVEN_INT));
        assertEquals(0, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, 0, 1, IS_EVEN_INT));
    }

    @Test
    public void testCountGenericArray() {
        String[] arr = { "a", "b", "", "d", "" };
        assertEquals(3, N.count(arr, STRING_NOT_EMPTY));
        assertEquals(0, N.count((String[]) null, STRING_NOT_EMPTY));
    }

    @Test
    public void testCountGenericArrayWithRange() {
        String[] arr = { "a", "b", "", "d", "" };
        assertEquals(1, N.count(arr, 1, 3, STRING_NOT_EMPTY));
    }

    @Test
    public void testCountCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "", "grape");
        assertEquals(2, N.count(list, 0, 3, STRING_NOT_EMPTY));
    }

    @Test
    public void testCountIterable() {
        Iterable<String> iter = Arrays.asList("a", "", "c", "");
        assertEquals(2, N.count(iter, STRING_NOT_EMPTY));
        assertEquals(0, N.count((Iterable<String>) null, STRING_NOT_EMPTY));
    }

    @Test
    public void testCountIterator() {
        assertEquals(3, N.count(Arrays.asList(1, 2, 3).iterator()));
        assertEquals(0, N.count((Iterator<Integer>) null));
        assertEquals(0, N.count(Collections.emptyIterator()));
    }

    @Test
    public void testCountIteratorWithPredicate() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(2, N.count(iter, IS_EVEN_INTEGER));
        assertEquals(0, N.count((Iterator<Integer>) null, IS_EVEN_INTEGER));
    }

    @Test
    public void testMergeArrays() {
        Integer[] a1 = { 1, 3, 5 };
        Integer[] a2 = { 2, 4, 6 };
        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a1, a2, selector);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);

        assertTrue(N.merge((Integer[]) null, (Integer[]) null, selector).isEmpty());
        assertEquals(List.of(1, 2), N.merge(new Integer[] { 1, 2 }, null, selector));
        assertEquals(List.of(1, 2), N.merge(null, new Integer[] { 1, 2 }, selector));

        Integer[] a3 = { 1, 2, 7 };
        Integer[] a4 = { 3, 4, 5, 6 };
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), N.merge(a3, a4, selector));
    }

    @Test
    public void testMergeIterables() {
        List<Integer> l1 = List.of(1, 3, 5);
        List<Integer> l2 = List.of(2, 4, 6);
        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(l1, l2, selector);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipArrays() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2)), result);
        assertTrue(N.zip(null, b, Pair::of).isEmpty());
        assertTrue(N.zip(a, null, Pair::of).isEmpty());
    }

    @Test
    public void testZipIterables() {
        List<String> l1 = List.of("x", "y", "z");
        List<Integer> l2 = List.of(10, 20);
        List<String> result = N.zip(l1, l2, (s, i) -> s + i);
        assertEquals(List.of("x10", "y20"), result);
    }

    @Test
    public void testZipArraysThree() {
        Character[] c = { 'P', 'Q', 'R' };
        String[] s = { "One", "Two" };
        Integer[] i = { 100, 200, 300 };
        List<Triple<Character, String, Integer>> result = N.zip(c, s, i, Triple::of);
        assertEquals(List.of(Triple.of('P', "One", 100), Triple.of('Q', "Two", 200)), result);
    }

    @Test
    public void testZipIterablesThree() {
        List<Character> l1 = List.of('A', 'B');
        List<String> l2 = List.of("Apple", "Ball", "Cat");
        List<Double> l3 = List.of(1.0, 2.0, 3.0, 4.0);
        List<String> result = N.zip(l1, l2, l3, (c, s, d) -> c + ":" + s + ":" + d);
        assertEquals(List.of("A:Apple:1.0", "B:Ball:2.0"), result);
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        List<Pair<String, Integer>> result = N.zip(a, b, "defaultA", 0, Pair::of);
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("defaultA", 3)), result);

        List<Pair<String, Integer>> result2 = N.zip(b, a, 0, "defaultA", (i, s) -> Pair.of(s, i));
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("defaultA", 3)), result2);
    }

    @Test
    public void testZipIterablesWithDefaults() {
        List<String> l1 = List.of("x");
        List<Integer> l2 = List.of(10, 20);
        List<String> result = N.zip(l1, l2, "def", 0, (s, i) -> s + i);
        assertEquals(List.of("x10", "def20"), result);
    }

    @Test
    public void testZipArraysThreeWithDefaults() {
        Character[] c = { 'P' };
        String[] s = { "One", "Two" };
        Integer[] i = { 100, 200, 300 };
        List<String> result = N.zip(c, s, i, 'Z', "DefS", 0, (ch, str, in) -> "" + ch + str + in);
        assertEquals(List.of("POne100", "ZTwo200", "ZDefS300"), result);
    }

    @Test
    public void testZipIterablesThreeWithDefaults() {
        List<Character> l1 = List.of('A', 'B');
        List<String> l2 = List.of("Apple");
        List<Double> l3 = List.of(1.0, 2.0, 3.0);

        List<String> result = N.zip(l1, l2, l3, 'X', "YYY", 0.0, (ch, str, d) -> "" + ch + str + d);
        assertEquals(List.of("AApple1.0", "BYYY2.0", "XYYY3.0"), result);
    }

    @Test
    public void testZipArraysToTargetArray() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        String[] result = N.zip(a, b, (s, i) -> s + i, String.class);
        assertArrayEquals(new String[] { "a1", "b2" }, result);
    }

    @Test
    public void testZipArraysWithDefaultsToTargetArray() {
        String[] a = { "a" };
        Integer[] b = { 1, 2 };
        String[] result = N.zip(a, b, "defA", 0, (s, i) -> s + i, String.class);
        assertArrayEquals(new String[] { "a1", "defA2" }, result);
    }

    @Test
    public void testZipArraysThreeToTargetArray() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        Character[] c = { 'X', 'Y' };
        String[] result = N.zip(a, b, c, (s, i, ch) -> s + i + ch, String.class);
        assertArrayEquals(new String[] { "a1X", "b2Y" }, result);
    }

    @Test
    public void testZipArraysThreeWithDefaultsToTargetArray() {
        String[] a = { "a" };
        Integer[] b = { 1, 2 };
        Character[] c = { 'X', 'Y', 'Z' };
        String[] result = N.zip(a, b, c, "defA", 0, 'D', (s, i, ch) -> s + i + ch, String.class);
        assertArrayEquals(new String[] { "a1X", "defA2Y", "defA0Z" }, result);
    }

    @Test
    public void testGroupByArray() {
        String[] arr = { "apple", "apricot", "banana", "blueberry", "avocado" };
        Map<Character, List<String>> result = N.groupBy(arr, s -> s.charAt(0));
        assertEquals(List.of("apple", "apricot", "avocado"), result.get('a'));
        assertEquals(List.of("banana", "blueberry"), result.get('b'));
        assertTrue(N.groupBy((String[]) null, s -> s.charAt(0)).isEmpty());
    }

    @Test
    public void testGroupByArrayWithMapSupplier() {
        String[] arr = { "apple", "apricot", "banana" };
        TreeMap<Character, List<String>> result = N.groupBy(arr, s -> s.charAt(0), TreeMap::new);
        assertEquals(List.of("apple", "apricot"), result.get('a'));
        assertEquals(List.of("banana"), result.get('b'));
        assertEquals(Arrays.asList('a', 'b'), new ArrayList<>(result.keySet()));
    }

    @Test
    public void testGroupByArrayWithRange() {
        String[] arr = { "one", "two", "three", "four", "five" };
        Map<Character, List<String>> result = N.groupBy(arr, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("two", "three"), result.get('t'));
        assertEquals(List.of("four"), result.get('f'));
        assertNull(result.get('o'));
    }

    @Test
    public void testGroupByArrayWithRangeAndMapSupplier() {
        String[] arr = { "one", "two", "three", "four", "five" };
        TreeMap<Character, List<String>> result = N.groupBy(arr, 1, 4, s -> s.charAt(0), TreeMap::new);
        assertEquals(List.of("four"), result.get('f'));
        assertEquals(List.of("two", "three"), result.get('t'));
        assertEquals(Arrays.asList('f', 't'), new ArrayList<>(result.keySet()));
    }

    @Test
    public void testGroupByCollectionWithRange() {
        List<String> list = Arrays.asList("one", "two", "three", "four", "five");
        Map<Character, List<String>> result = N.groupBy(list, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("two", "three"), result.get('t'));
        assertEquals(List.of("four"), result.get('f'));
    }

    @Test
    public void testGroupByIterable() {
        Iterable<String> iter = Arrays.asList("apple", "apricot", "banana");
        Map<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0));
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIterableWithMapSupplier() {
        Iterable<String> iter = Arrays.asList("apple", "apricot", "banana");
        TreeMap<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), Suppliers.ofTreeMap());
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIterator() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        Map<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0));
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        TreeMap<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), Suppliers.ofTreeMap());
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIterableWithValueExtractor() {
        List<String> list = Arrays.asList("apple:red", "banana:yellow", "apricot:orange");
        Function<String, Character> keyExtractor = s -> s.charAt(0);
        Function<String, String> valueExtractor = s -> s.split(":")[1];
        Map<Character, List<String>> result = N.groupBy(list, keyExtractor, valueExtractor);
        assertEquals(List.of("red", "orange"), result.get('a'));
        assertEquals(List.of("yellow"), result.get('b'));
    }

    @Test
    public void testGroupByIterableWithValueExtractorAndMapSupplier() {
        List<String> list = Arrays.asList("apple:red", "banana:yellow", "apricot:orange");
        TreeMap<Character, List<String>> result = N.groupBy(list, s -> s.charAt(0), s -> s.split(":")[1], TreeMap::new);
        assertEquals(List.of("red", "orange"), result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithValueExtractor() {
        Iterator<String> iter = Arrays.asList("apple:red", "banana:yellow", "apricot:orange").iterator();
        Map<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), s -> s.split(":")[1]);
        assertEquals(List.of("red", "orange"), result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithValueExtractorAndMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple:red", "banana:yellow", "apricot:orange").iterator();
        TreeMap<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), s -> s.split(":")[1], TreeMap::new);
        assertEquals(List.of("red", "orange"), result.get('a'));
    }

    @Test
    public void testGroupByIterableWithCollector() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "blueberry");
        Collector<String, ?, String> valueCollector = Collectors.mapping(it -> String.valueOf(it.length()), Collectors.joining(","));
        Map<Character, String> result = N.groupBy(list, s -> s.charAt(0), valueCollector);
        assertEquals("5,7", result.get('a'));
        assertEquals("6,9", result.get('b'));
    }

    @Test
    public void testGroupByIterableWithCollectorAndMapSupplier() {
        List<String> list = Arrays.asList("apple", "apricot", "banana");
        Collector<String, ?, Long> countingCollector = Collectors.counting();
        TreeMap<Character, Long> result = N.groupBy(list, s -> s.charAt(0), countingCollector, TreeMap::new);
        assertEquals(2L, result.get('a'));
        assertEquals(1L, result.get('b'));
    }

    @Test
    public void testGroupByIteratorWithCollector() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        Collector<String, ?, Long> countingCollector = Collectors.counting();
        Map<Character, Long> result = N.groupBy(iter, s -> s.charAt(0), countingCollector);
        assertEquals(2L, result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithCollectorAndMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        Collector<String, ?, Long> countingCollector = Collectors.counting();
        TreeMap<Character, Long> result = N.groupBy(iter, s -> s.charAt(0), countingCollector, TreeMap::new);
        assertEquals(2L, result.get('a'));
    }

    @Test
    public void testCountByIterable() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "apricot");
        Map<Character, Integer> result = N.countBy(list, s -> s.charAt(0));
        assertEquals(3, result.get('a'));
        assertEquals(1, result.get('b'));
        assertTrue(N.countBy((List<String>) null, s -> s).isEmpty());
    }

    @Test
    public void testCountByIterableWithMapSupplier() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "apricot");
        TreeMap<Character, Integer> result = N.countBy(list, s -> s.charAt(0), TreeMap::new);
        assertEquals(3, result.get('a'));
    }

    @Test
    public void testCountByIterator() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana", "apricot").iterator();
        Map<Character, Integer> result = N.countBy(iter, s -> s.charAt(0));
        assertEquals(3, result.get('a'));
    }

    @Test
    public void testCountByIteratorWithMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana", "apricot").iterator();
        TreeMap<Character, Integer> result = N.countBy(iter, s -> s.charAt(0), TreeMap::new);
        assertEquals(3, result.get('a'));
    }

    @Test
    public void testIterateArray() {
        String[] arr = { "a", "b" };
        Iterator<String> iter = N.iterate(arr);
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(N.iterate((String[]) null).hasNext());
        assertFalse(N.iterate(new String[0]).hasNext());
    }

    @Test
    public void testIterateArrayWithRange() {
        String[] arr = { "a", "b", "c", "d" };
        Iterator<String> iter = N.iterate(arr, 1, 3);
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(N.iterate(arr, 1, 1).hasNext());
    }

    @Test
    public void testIterateMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Iterator<Map.Entry<String, Integer>> iter = N.iterate(map);
        int count = 0;
        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            assertTrue(map.containsKey(entry.getKey()));
            assertEquals(map.get(entry.getKey()), entry.getValue());
            count++;
        }
        assertEquals(2, count);
        assertFalse(N.iterate((Map<String, Integer>) null).hasNext());
    }

    @Test
    public void testIterateIterable() {
        List<String> list = List.of("x", "y");
        Iterator<String> iter = N.iterate(list);
        assertEquals("x", iter.next());
        assertEquals("y", iter.next());
        assertFalse(iter.hasNext());
        assertFalse(N.iterate((Iterable<String>) null).hasNext());
    }

    @Test
    public void testIterateEach() {
        List<String> l1 = List.of("a", "b");
        List<String> l2 = List.of("c");
        List<String> l3 = Collections.emptyList();
        Collection<Iterable<String>> iterables = CommonUtil.toList(l1, l2, l3, null);

        List<Iterator<String>> resultIterators = N.iterateEach(iterables);
        assertEquals(4, resultIterators.size());

        Iterator<String> iter1 = resultIterators.get(0);
        assertEquals("a", iter1.next());
        assertEquals("b", iter1.next());
        assertFalse(iter1.hasNext());

        Iterator<String> iter2 = resultIterators.get(1);
        assertEquals("c", iter2.next());
        assertFalse(iter2.hasNext());

        Iterator<String> iter3 = resultIterators.get(2);
        assertFalse(iter3.hasNext());

        Iterator<String> iter4 = resultIterators.get(3);
        assertFalse(iter4.hasNext());

        assertTrue(N.iterateEach(null).isEmpty());
    }

    @Test
    public void testIterateAll() {
        List<String> l1 = List.of("a", "b");
        List<String> l2 = List.of("c");
        List<String> l3 = Collections.emptyList();
        Collection<Iterable<String>> iterables = CommonUtil.toList(l1, null, l2, l3);

        Iterator<String> combinedIter = N.iterateAll(iterables);
        assertEquals("a", combinedIter.next());
        assertEquals("b", combinedIter.next());
        assertEquals("c", combinedIter.next());
        assertFalse(combinedIter.hasNext());

        assertFalse(N.iterateAll(null).hasNext());
        assertFalse(N.iterateAll(Collections.emptyList()).hasNext());
    }

    @Test
    public void testDisjointArrays() {
        assertTrue(N.disjoint(new String[] { "a", "b" }, new String[] { "c", "d" }));
        assertFalse(N.disjoint(new String[] { "a", "b" }, new String[] { "b", "c" }));
        assertTrue(N.disjoint(null, new String[] { "a" }));
        assertTrue(N.disjoint(new String[] { "a" }, null));
        assertTrue(N.disjoint(new String[0], new String[] { "a" }));
    }

    @Test
    public void testDisjointCollections() {
        assertTrue(N.disjoint(List.of("a", "b"), Set.of("c", "d")));
        assertFalse(N.disjoint(Set.of("a", "b"), List.of("b", "c")));
        assertTrue(N.disjoint(null, List.of("a")));
        assertTrue(N.disjoint(Collections.emptySet(), List.of("a")));

        assertTrue(N.disjoint(new HashSet<>(List.of("a", "b")), new ArrayList<>(List.of("c", "d"))));
        assertFalse(N.disjoint(new ArrayList<>(List.of("c", "d", "a")), new HashSet<>(List.of("a", "b"))));
    }

    @Test
    public void testDeleteRangeBooleanArray() {
        assertArrayEquals(new boolean[] {}, N.removeRange((boolean[]) null, 0, 0));
        assertArrayEquals(new boolean[] {}, N.removeRange(new boolean[] {}, 0, 0));
        assertArrayEquals(new boolean[] { true, false, true }, N.removeRange(new boolean[] { true, false, true }, 1, 1));
        assertArrayEquals(new boolean[] { false, true }, N.removeRange(new boolean[] { true, false, true }, 0, 1));
        assertArrayEquals(new boolean[] { true, false }, N.removeRange(new boolean[] { true, false, true }, 2, 3));
        assertArrayEquals(new boolean[] { true, true }, N.removeRange(new boolean[] { true, false, true }, 1, 2));
        assertArrayEquals(new boolean[] {}, N.removeRange(new boolean[] { true, false, true }, 0, 3));

        final boolean[] original = { true, false, true, true, false };
        boolean[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new boolean[] { true, true, false }, result);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, original, "Original array should not be modified.");

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new boolean[] { true }, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new boolean[] { true }, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new boolean[] { true }, 1, 0));
    }

    @Test
    public void testDeleteRangeCharArray() {
        assertArrayEquals(new char[] {}, N.removeRange((char[]) null, 0, 0));
        assertArrayEquals(new char[] {}, N.removeRange(new char[] {}, 0, 0));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeRange(new char[] { 'a', 'b', 'c' }, 1, 1));
        assertArrayEquals(new char[] { 'b', 'c' }, N.removeRange(new char[] { 'a', 'b', 'c' }, 0, 1));
        assertArrayEquals(new char[] { 'a' }, N.removeRange(new char[] { 'a', 'b', 'c' }, 1, 3));
        final char[] original = { 'a', 'b', 'c', 'd', 'e' };
        char[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'e' }, result);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, original, "Original array should not be modified.");

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new char[] { 'a' }, -1, 0));
    }

    @Test
    public void testDeleteRangeByteArray() {
        assertArrayEquals(new byte[] {}, N.removeRange((byte[]) null, 0, 0));
        assertArrayEquals(new byte[] {}, N.removeRange(new byte[] {}, 0, 0));
        assertArrayEquals(new byte[] { 1, 2, 3 }, N.removeRange(new byte[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new byte[] { 2, 3 }, N.removeRange(new byte[] { 1, 2, 3 }, 0, 1));
        final byte[] original = { 1, 2, 3, 4, 5 };
        byte[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new byte[] { 1, 4, 5 }, result);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, original);
    }

    @Test
    public void testDeleteRangeShortArray() {
        assertArrayEquals(new short[] {}, N.removeRange((short[]) null, 0, 0));
        assertArrayEquals(new short[] {}, N.removeRange(new short[] {}, 0, 0));
        assertArrayEquals(new short[] { 1, 2, 3 }, N.removeRange(new short[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new short[] { 2, 3 }, N.removeRange(new short[] { 1, 2, 3 }, 0, 1));
        final short[] original = { 1, 2, 3, 4, 5 };
        short[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new short[] { 1, 4, 5 }, result);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, original);
    }

    @Test
    public void testDeleteRangeIntArray() {
        assertArrayEquals(new int[] {}, N.removeRange((int[]) null, 0, 0));
        assertArrayEquals(new int[] {}, N.removeRange(new int[] {}, 0, 0));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeRange(new int[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new int[] { 2, 3 }, N.removeRange(new int[] { 1, 2, 3 }, 0, 1));
        final int[] original = { 1, 2, 3, 4, 5 };
        int[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new int[] { 1, 4, 5 }, result);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, original);
    }

    @Test
    public void testDeleteRangeLongArray() {
        assertArrayEquals(new long[] {}, N.removeRange((long[]) null, 0, 0));
        assertArrayEquals(new long[] {}, N.removeRange(new long[] {}, 0, 0));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.removeRange(new long[] { 1L, 2L, 3L }, 1, 1));
        assertArrayEquals(new long[] { 2L, 3L }, N.removeRange(new long[] { 1L, 2L, 3L }, 0, 1));
        final long[] original = { 1L, 2L, 3L, 4L, 5L };
        long[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new long[] { 1L, 4L, 5L }, result);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, original);
    }

    @Test
    public void testDeleteRangeFloatArray() {
        assertArrayEquals(new float[] {}, N.removeRange((float[]) null, 0, 0), DELTAf);
        assertArrayEquals(new float[] {}, N.removeRange(new float[] {}, 0, 0), DELTAf);
        assertArrayEquals(new float[] { 1f, 2f, 3f }, N.removeRange(new float[] { 1f, 2f, 3f }, 1, 1), DELTAf);
        assertArrayEquals(new float[] { 2f, 3f }, N.removeRange(new float[] { 1f, 2f, 3f }, 0, 1), DELTAf);
        final float[] original = { 1f, 2f, 3f, 4f, 5f };
        float[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new float[] { 1f, 4f, 5f }, result, DELTAf);
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, original, DELTAf);
    }

    @Test
    public void testDeleteRangeDoubleArray() {
        assertArrayEquals(new double[] {}, N.removeRange((double[]) null, 0, 0), DELTA);
        assertArrayEquals(new double[] {}, N.removeRange(new double[] {}, 0, 0), DELTA);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.removeRange(new double[] { 1.0, 2.0, 3.0 }, 1, 1), DELTA);
        assertArrayEquals(new double[] { 2.0, 3.0 }, N.removeRange(new double[] { 1.0, 2.0, 3.0 }, 0, 1), DELTA);
        final double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0 }, result, DELTA);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, original, DELTA);
    }

    @Test
    public void testDeleteRangeStringArray() {
        assertArrayEquals(new String[] {}, N.removeRange((String[]) null, 0, 0));
        assertArrayEquals(new String[] {}, N.removeRange(new String[] {}, 0, 0));
        assertArrayEquals(new String[] { "a", "b", "c" }, N.removeRange(new String[] { "a", "b", "c" }, 1, 1));
        assertArrayEquals(new String[] { "b", "c" }, N.removeRange(new String[] { "a", "b", "c" }, 0, 1));
        final String[] original = { "a", "b", "c", "d", "e" };
        String[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new String[] { "a", "d", "e" }, result);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, original);
    }

    @Test
    public void testDeleteRangeGenericArray() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange((Integer[]) null, 0, 1));
        assertArrayEquals(new Integer[] {}, N.removeRange(new Integer[] {}, 0, 0));
        Integer[] originalArr = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, N.removeRange(originalArr, 1, 1));

        assertArrayEquals(new Integer[] { 2, 3 }, N.removeRange(new Integer[] { 1, 2, 3 }, 0, 1));

        assertArrayEquals(new Integer[] { 4, 5 }, N.removeRange(new Integer[] { 1, 2, 3, 4, 5 }, 0, 3));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, N.removeRange(new Integer[] { 1, 2, 3, 4, 5 }, 3, 5));
        assertArrayEquals(new Integer[] { 1, 5 }, N.removeRange(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertArrayEquals(new Integer[] {}, N.removeRange(new Integer[] { 1, 2, 3 }, 0, 3));

        Integer[] original = { 10, 20, 30, 40, 50 };
        Integer[] result = N.removeRange(original, 1, 3);
        assertArrayEquals(new Integer[] { 10, 40, 50 }, result);
        assertArrayEquals(new Integer[] { 10, 20, 30, 40, 50 }, original, "Original array should not be modified.");

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new Integer[] { 1 }, -1, 0));
    }

    @Test
    public void testDeleteRangeList() {
        List<String> list = toMutableList("a", "b", "c", "d");
        assertTrue(N.removeRange(list, 1, 3));
        assertEquals(toMutableList("a", "d"), list);

        List<String> list2 = toMutableList("a", "b", "c");
        assertFalse(N.removeRange(list2, 1, 1));
        assertEquals(toMutableList("a", "b", "c"), list2);

        List<String> list3 = toMutableList("a", "b", "c");
        assertTrue(N.removeRange(list3, 0, 3));
        assertTrue(list3.isEmpty());

        List<String> list4 = toMutableList("a", "b", "c");
        assertTrue(N.removeRange(list4, 0, 1));
        assertEquals(toMutableList("b", "c"), list4);

        List<String> list5 = toMutableList("a", "b", "c");
        assertTrue(N.removeRange(list5, 2, 3));
        assertEquals(toMutableList("a", "b"), list5);

        List<String> emptyList = new ArrayList<>();
        assertFalse(N.removeRange(emptyList, 0, 0));
        assertTrue(emptyList.isEmpty());

        List<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(N.removeRange(linkedList, 1, 3));
        assertEquals(Arrays.asList(1, 4, 5), linkedList);

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(toMutableList("a"), -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(toMutableList("a"), 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(toMutableList("a"), 1, 0));
        N.removeRange((List<String>) null, 0, 0);

    }

    @Test
    public void testDeleteRangeString() {
        assertEquals("ac", N.removeRange("abc", 1, 2));
        assertEquals("abc", N.removeRange("abc", 1, 1));
        assertEquals("", N.removeRange("abc", 0, 3));
        assertEquals("c", N.removeRange("abc", 0, 2));
        assertEquals("ab", N.removeRange("abc", 2, 3));
        assertEquals("", N.removeRange("", 0, 0));
        assertEquals("", N.removeRange((String) null, 0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange("a", -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange("a", 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange("a", 1, 2));
    }

    @Test
    public void testReplaceRangeBooleanArray() {
        assertArrayEquals(new boolean[] { true, false }, N.replaceRange(new boolean[] {}, 0, 0, new boolean[] { true, false }));
        assertArrayEquals(new boolean[] {}, N.replaceRange(new boolean[] { true }, 0, 1, new boolean[] {}));
        assertArrayEquals(new boolean[] { true, true, false, false, false },
                N.replaceRange(new boolean[] { false, false, false, false }, 0, 1, new boolean[] { true, true }));
        assertArrayEquals(new boolean[] { true, false, false }, N.replaceRange(new boolean[] { true, true, true, false }, 1, 3, new boolean[] { false }));

        boolean[] original = { true, false, true, false };
        boolean[] replacement = { true, true };
        boolean[] result = N.replaceRange(original, 1, 3, replacement);
        assertArrayEquals(new boolean[] { true, true, true, false }, result);
        assertArrayEquals(new boolean[] { true, false, true, false }, original, "Original array should not be modified.");

        assertArrayEquals(new boolean[] { true, false }, N.replaceRange(null, 0, 0, new boolean[] { true, false }));
        assertArrayEquals(new boolean[] {}, N.replaceRange(new boolean[] { true, true }, 0, 2, null));

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(new boolean[] { true }, -1, 0, new boolean[] {}));
    }

    @Test
    public void testReplaceRangeCharArray() {
        assertArrayEquals(new char[] { 'x', 'y' }, N.replaceRange(new char[] {}, 0, 0, new char[] { 'x', 'y' }));
        assertArrayEquals(new char[] {}, N.replaceRange(new char[] { 'a' }, 0, 1, new char[] {}));
        char[] original = { 'a', 'b', 'c', 'd' };
        char[] replacement = { 'X', 'Y' };
        char[] result = N.replaceRange(original, 1, 3, replacement);
        assertArrayEquals(new char[] { 'a', 'X', 'Y', 'd' }, result);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, original);
    }

    @Test
    public void testReplaceRangeGenericArray() {
        Integer[] original = { 1, 2, 3, 4 };
        Integer[] replacement = { 8, 9 };
        Integer[] result = N.replaceRange(original, 1, 3, replacement);
        assertArrayEquals(new Integer[] { 1, 8, 9, 4 }, result);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, original);

        assertArrayEquals(new Integer[] { 8, 9 }, N.replaceRange(new Integer[] {}, 0, 0, new Integer[] { 8, 9 }));
        assertArrayEquals(new Integer[] { 1, 4 }, N.replaceRange(original, 1, 3, new Integer[] {}));
        Integer[] rep = { 10, 20 };
        assertArrayEquals(rep, N.replaceRange((Integer[]) null, 0, 0, rep));
        assertArrayEquals(new Integer[] { 1, 4 }, N.replaceRange(original, 1, 3, null));

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(new Integer[] { 1 }, 1, 0, new Integer[] {}));
    }

    @Test
    public void testReplaceRangeList() {
        List<String> list = toMutableList("a", "b", "c", "d");
        List<String> replacement = toMutableList("X", "Y");
        assertTrue(N.replaceRange(list, 1, 3, replacement));
        assertEquals(toMutableList("a", "X", "Y", "d"), list);

        List<String> list2 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list2, 0, 1, toMutableList("Z")));
        assertEquals(toMutableList("Z", "b"), list2);

        List<String> list3 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list3, 0, 2, toMutableList("W")));
        assertEquals(toMutableList("W"), list3);

        List<String> list4 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list4, 1, 1, toMutableList("MID")));
        assertEquals(toMutableList("a", "MID", "b"), list4);

        List<String> list5 = toMutableList("a", "b");
        assertTrue(N.replaceRange(list5, 0, 0, Arrays.asList("S", "T")));
        assertEquals(Arrays.asList("S", "T", "a", "b"), list5);

        List<String> list6 = toMutableList("a", "b");
        boolean changed = N.replaceRange(list6, 1, 1, Collections.emptyList());
        assertFalse(changed);
        assertEquals(Arrays.asList("a", "b"), list6);

        assertTrue(N.replaceRange(list6, 0, 2, Collections.emptyList()));
        assertTrue(list6.isEmpty());

        List<String> listNullRep = toMutableList("a", "b");
        assertTrue(N.replaceRange(listNullRep, 0, 1, null));
        assertEquals(toMutableList("b"), listNullRep);

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange(toMutableList("a"), -1, 0, replacement));
        assertThrows(IllegalArgumentException.class, () -> N.replaceRange((List<String>) null, 0, 0, replacement));
    }

    @Test
    public void testReplaceRangeString() {
        assertEquals("aXYd", N.replaceRange("abcd", 1, 3, "XY"));
        assertEquals("XYabcd", N.replaceRange("abcd", 0, 0, "XY"));
        assertEquals("abXYcd", N.replaceRange("abcd", 2, 2, "XY"));
        assertEquals("abcdXY", N.replaceRange("abcd", 4, 4, "XY"));
        assertEquals("XY", N.replaceRange("abcd", 0, 4, "XY"));
        assertEquals("ad", N.replaceRange("abcd", 1, 3, ""));
        assertEquals("XY", N.replaceRange("", 0, 0, "XY"));
        assertEquals("XY", N.replaceRange(null, 0, 0, "XY"));
        assertEquals("ac", N.replaceRange("abc", 1, 2, null));

        assertThrows(IndexOutOfBoundsException.class, () -> N.replaceRange("a", -1, 0, "b"));
    }

    @Test
    public void testMoveRangeBooleanArray() {
        boolean[] arr = { true, false, true, false, true };
        N.moveRange(arr, 1, 3, 0);
        assertArrayEquals(new boolean[] { false, true, true, false, true }, arr);

        boolean[] arr2 = { true, false, true, false, true };
        N.moveRange(arr2, 0, 2, 3);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, arr2);

        boolean[] arr3 = { true, false, true };
        N.moveRange(arr3, 0, 1, 1);
        assertArrayEquals(new boolean[] { false, true, true }, arr3);

        boolean[] arr4 = { true, false, true, false, true };
        N.moveRange(arr4, 0, 0, 0);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);
        N.moveRange(arr4, 1, 1, 0);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);

        N.moveRange(arr4, 1, 2, 1);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr4);

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true }, -1, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true }, 0, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(new boolean[] { true, false }, 0, 1, 2));
    }

    @Test
    public void testMoveRangeGenericArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 0);
        assertArrayEquals(new Integer[] { 2, 3, 1, 4, 5 }, arr);

        Integer[] arr2 = { 1, 2, 3, 4, 5 };
        N.moveRange(arr2, 0, 2, 3);
        assertArrayEquals(new Integer[] { 3, 4, 5, 1, 2 }, arr2);
    }

    @Test
    public void testMoveRangeList() {
        List<String> list = toMutableList("a", "b", "c", "d", "e");
        assertTrue(N.moveRange(list, 1, 3, 0));
        assertEquals(toMutableList("b", "c", "a", "d", "e"), list);

        List<String> list2 = toMutableList("a", "b", "c", "d", "e");
        assertTrue(N.moveRange(list2, 0, 2, 3));
        assertEquals(toMutableList("c", "d", "e", "a", "b"), list2);

        List<String> list3 = toMutableList("a", "b", "c");
        assertFalse(N.moveRange(list3, 0, 0, 0));
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertFalse(N.moveRange(list3, 1, 1, 0));
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertFalse(N.moveRange(list3, 0, 1, 0));
        assertEquals(toMutableList("a", "b", "c"), list3);

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange(toMutableList("a"), -1, 0, 0));
        N.moveRange((List<String>) null, 0, 0, 0);
    }

    @Test
    public void testMoveRangeString() {
        assertEquals("bcade", N.moveRange("abcde", 1, 3, 0));
        assertEquals("cdeab", N.moveRange("abcde", 0, 2, 3));
        assertEquals("abc", N.moveRange("abc", 0, 0, 0));
        assertEquals("abc", N.moveRange("abc", 1, 1, 0));
        assertEquals("abc", N.moveRange("abc", 0, 1, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> N.moveRange("a", -1, 0, 0));
    }

    @Test
    public void testSkipRangeGenericArray() {
        assertNull(N.skipRange((Integer[]) null, 0, 0));
        Integer[] emptyArr = {};
        assertArrayEquals(emptyArr, N.skipRange(emptyArr, 0, 0));

        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, N.skipRange(arr, 2, 2));
        assertArrayEquals(arr, N.skipRange(arr, 2, 2));

        assertArrayEquals(new Integer[] { 3, 4, 5 }, N.skipRange(arr, 0, 2));
        assertArrayEquals(new Integer[] { 1, 2 }, N.skipRange(arr, 2, 5));
        assertArrayEquals(new Integer[] { 1, 5 }, N.skipRange(arr, 1, 4));
        assertArrayEquals(new Integer[] {}, N.skipRange(arr, 0, 5));

        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(arr, 3, 1));
    }

    @Test
    public void testSkipRangeCollection() {
        Collection<Integer> coll = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.skipRange(coll, 2, 2));
        assertEquals(Arrays.asList(3, 4, 5), N.skipRange(coll, 0, 2));
        assertEquals(Arrays.asList(1, 2), N.skipRange(coll, 2, 5));
        assertEquals(Arrays.asList(1, 5), N.skipRange(coll, 1, 4));
        assertEquals(Collections.emptyList(), N.skipRange(coll, 0, 5));

        Collection<Integer> emptyColl = Collections.emptyList();
        assertEquals(Collections.emptyList(), N.skipRange(emptyColl, 0, 0));

        assertThrows(IndexOutOfBoundsException.class, () -> N.skipRange(coll, -1, 2));

        Set<Integer> resultSet = N.skipRange(coll, 1, 3, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList(1, 4, 5)), resultSet);

        Collection<Integer> nonList = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        List<Integer> skippedNonList = N.skipRange(nonList, 1, 3);
        assertEquals(nonList.size() - 2, skippedNonList.size());
    }

    @Test
    public void testHasDuplicatesBooleanArray() {
        assertFalse(N.containsDuplicates((boolean[]) null));
        assertFalse(N.containsDuplicates(new boolean[] {}));
        assertFalse(N.containsDuplicates(new boolean[] { true }));
        assertTrue(N.containsDuplicates(new boolean[] { true, true }));
        assertFalse(N.containsDuplicates(new boolean[] { true, false }));
        assertTrue(N.containsDuplicates(new boolean[] { true, false, true }));
        assertTrue(N.containsDuplicates(new boolean[] { false, false, false }));
    }

    @Test
    public void testHasDuplicatesCharArray() {
        assertFalse(N.containsDuplicates((char[]) null));
        assertFalse(N.containsDuplicates(new char[] {}));
        assertFalse(N.containsDuplicates(new char[] { 'a' }));
        assertTrue(N.containsDuplicates(new char[] { 'a', 'a' }));
        assertFalse(N.containsDuplicates(new char[] { 'a', 'b' }));
        assertTrue(N.containsDuplicates(new char[] { 'a', 'b', 'a' }));
        assertFalse(N.containsDuplicates(new char[] { 'a', 'b', 'c' }));

        assertTrue(N.containsDuplicates(new char[] { 'a', 'a', 'b' }, true));
        assertFalse(N.containsDuplicates(new char[] { 'a', 'b', 'c' }, true));
        assertTrue(N.containsDuplicates(new char[] { 'c', 'a', 'b', 'a' }, false));
        assertFalse(N.containsDuplicates(new char[] { 'd', 'c', 'b', 'a' }, false));
    }

    @Test
    public void testHasDuplicatesFloatArray() {
        assertFalse(N.containsDuplicates((float[]) null));
        assertFalse(N.containsDuplicates(new float[] {}));
        assertFalse(N.containsDuplicates(new float[] { 1.0f }));
        assertTrue(N.containsDuplicates(new float[] { 1.0f, 1.0f }));
        assertFalse(N.containsDuplicates(new float[] { 1.0f, 2.0f }));
        assertTrue(N.containsDuplicates(new float[] { 1.0f, 2.0f, 1.0f }));
        assertTrue(N.containsDuplicates(new float[] { Float.NaN, Float.NaN }));

        assertTrue(N.containsDuplicates(new float[] { 1.0f, 1.0f, 2.0f }, true));
        assertFalse(N.containsDuplicates(new float[] { 1.0f, 2.0f, 3.0f }, true));
    }

    @Test
    public void testHasDuplicatesDoubleArray() {
        assertFalse(N.containsDuplicates((double[]) null));
        assertFalse(N.containsDuplicates(new double[] {}));
        assertFalse(N.containsDuplicates(new double[] { 1.0 }));
        assertTrue(N.containsDuplicates(new double[] { 1.0, 1.0 }));
        assertFalse(N.containsDuplicates(new double[] { 1.0, 2.0 }));
        assertTrue(N.containsDuplicates(new double[] { 1.0, 2.0, 1.0 }));
        assertTrue(N.containsDuplicates(new double[] { Double.NaN, Double.NaN }));

        assertTrue(N.containsDuplicates(new double[] { 1.0, 1.0, 2.0 }, true));
        assertFalse(N.containsDuplicates(new double[] { 1.0, 2.0, 3.0 }, true));
    }

    @Test
    public void testHasDuplicatesGenericArray() {
        assertFalse(N.containsDuplicates((Integer[]) null));
        assertFalse(N.containsDuplicates(new Integer[] {}));
        assertFalse(N.containsDuplicates(new Integer[] { 1 }));
        assertTrue(N.containsDuplicates(new Integer[] { 1, 1 }));
        assertFalse(N.containsDuplicates(new Integer[] { 1, 2 }));
        assertTrue(N.containsDuplicates(new Integer[] { 1, 2, 1 }));
        assertFalse(N.containsDuplicates(new Integer[] { 1, 2, 3 }));
        assertTrue(N.containsDuplicates(new Integer[] { null, null }));
        assertFalse(N.containsDuplicates(new Integer[] { 1, null }));

        assertTrue(N.containsDuplicates(new String[] { "a", "a", "b" }, true));
        assertFalse(N.containsDuplicates(new String[] { "a", "b", "c" }, true));
    }

    @Test
    public void testHasDuplicatesCollection() {
        assertFalse(N.containsDuplicates((Collection<?>) null));
        assertFalse(N.containsDuplicates(Collections.emptyList()));
        assertFalse(N.containsDuplicates(Collections.singletonList(1)));
        assertTrue(N.containsDuplicates(Arrays.asList(1, 1)));
        assertFalse(N.containsDuplicates(Arrays.asList(1, 2)));
        assertTrue(N.containsDuplicates(Arrays.asList(1, 2, 1)));
        assertTrue(N.containsDuplicates(Arrays.asList(null, null)));

        List<Integer> sortedListWithDup = Arrays.asList(1, 2, 2, 3);
        assertTrue(N.containsDuplicates(sortedListWithDup, true));
        List<Integer> sortedListNoDup = Arrays.asList(1, 2, 3, 4);
        assertFalse(N.containsDuplicates(sortedListNoDup, true));

        Set<Integer> setWithNoDup = new HashSet<>(Arrays.asList(1, 2, 3));
        assertFalse(N.containsDuplicates(setWithNoDup, false));
        assertFalse(N.containsDuplicates(setWithNoDup, true));
    }

    @Test
    public void testRetainAll() {
        Collection<Integer> main = toMutableList(1, 2, 3, 4, 5);
        Collection<Integer> keep = Arrays.asList(3, 5, 6);
        assertTrue(N.retainAll(main, keep));
        assertEquals(toMutableList(3, 5), main);

        Collection<String> main2 = toMutableList("a", "b", "c");
        Collection<String> keep2 = Arrays.asList("x", "y");
        assertTrue(N.retainAll(main2, keep2));
        assertTrue(main2.isEmpty());

        Collection<Integer> main3 = toMutableList(1, 2, 3);
        Collection<Integer> keep3 = Arrays.asList(1, 2, 3, 4);
        assertFalse(N.retainAll(main3, keep3));
        assertEquals(toMutableList(1, 2, 3), main3);

        Collection<Integer> main4 = toMutableList(1, 2, 3);
        assertTrue(N.retainAll(main4, Collections.emptyList()));
        assertTrue(main4.isEmpty());

        Collection<Integer> emptyMain = new ArrayList<>();
        assertFalse(N.retainAll(emptyMain, Arrays.asList(1, 2)));
        assertTrue(emptyMain.isEmpty());

        HashSet<Integer> mainHashSet = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        List<Integer> keepList = Arrays.asList(1, 5, 10, 11);
        assertTrue(N.retainAll(mainHashSet, keepList));
        assertEquals(new HashSet<>(Arrays.asList(1, 5, 10)), mainHashSet);

        N.retainAll(null, keep);
        Collection<Integer> mainForNullKeep = toMutableList(1, 2, 3);
        assertTrue(N.retainAll(mainForNullKeep, null));
        assertTrue(mainForNullKeep.isEmpty());
    }

    @Test
    public void testSumCharArray() {
        assertEquals(0, N.sum((char[]) null));
        assertEquals(0, N.sum(new char[] {}));
        assertEquals('a' + 'b' + 'c', N.sum('a', 'b', 'c'));
        assertEquals('b' + 'c', N.sum(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals(0, N.sum(new char[] { 'a', 'b' }, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.sum(new char[] { 'a' }, 0, 2));
    }

    @Test
    public void testSumIntArray() {
        assertEquals(0, N.sum((int[]) null));
        assertEquals(0, N.sum(new int[] {}));
        assertEquals(6, N.sum(1, 2, 3));
        assertEquals(5, N.sum(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(0, N.sum(new int[] { 1, 2 }, 1, 1));

        assertEquals(Integer.MAX_VALUE, N.sum(new int[] { Integer.MAX_VALUE }));
        assertThrows(ArithmeticException.class, () -> N.sum(new int[] { Integer.MAX_VALUE, 1 }));
    }

    @Test
    public void testSumToLongIntArray() {
        assertEquals(0L, N.sumToLong((int[]) null));
        assertEquals(0L, N.sumToLong(new int[] {}));
        assertEquals(6L, N.sumToLong(1, 2, 3));
        assertEquals((long) Integer.MAX_VALUE + 1, N.sumToLong(new int[] { Integer.MAX_VALUE, 1 }));

        assertEquals(Integer.MAX_VALUE, N.sumToLong(new int[] { Integer.MAX_VALUE }));
        assertEquals(3L, N.sumToLong(new int[] { 1, 2 }));
    }

    @Test
    public void testSumLongArray() {
        assertEquals(0L, N.sum((long[]) null));
        assertEquals(0L, N.sum(new long[] {}));
        assertEquals(6L, N.sum(1L, 2L, 3L));
        assertEquals(Long.MAX_VALUE, N.sum(Long.MAX_VALUE - 1L, 1L));
    }

    @Test
    public void testSumFloatArray() {
        assertEquals(0f, N.sum((float[]) null), DELTA);
        assertEquals(0f, N.sum(new float[] {}), DELTA);
        assertEquals(6.0f, N.sum(1.0f, 2.0f, 3.0f), DELTA);
        assertEquals(0.3f, N.sum(0.1f, 0.2f), DELTA);
    }

    @Test
    public void testSumToDoubleFloatArray() {
        assertEquals(0.0, N.sumToDouble((float[]) null), DELTA);
        assertEquals(0.0, N.sumToDouble(new float[] {}), DELTA);
        assertEquals(6.0, N.sumToDouble(1.0f, 2.0f, 3.0f), DELTA);
        assertEquals(0.3, N.sumToDouble(0.1f, 0.2f), DELTA);
    }

    @Test
    public void testSumDoubleArray() {
        assertEquals(0.0, N.sum((double[]) null), DELTA);
        assertEquals(0.0, N.sum(new double[] {}), DELTA);
        assertEquals(6.0, N.sum(1.0, 2.0, 3.0), DELTA);
        assertEquals(0.3, N.sum(0.1, 0.2), DELTA);
    }

    @Test
    public void testAverageCharArray() {
        assertEquals(0.0, N.average((char[]) null), DELTA);
        assertEquals(0.0, N.average(new char[] {}), DELTA);
        assertEquals(('a' + 'b' + 'c') / 3.0, N.average('a', 'b', 'c'), DELTA);
        assertEquals(('b' + 'c') / 2.0, N.average(new char[] { 'a', 'b', 'c', 'd' }, 1, 3), DELTA);
    }

    @Test
    public void testAverageIntArray() {
        assertEquals(0.0, N.average((int[]) null), DELTA);
        assertEquals(0.0, N.average(new int[] {}), DELTA);
        assertEquals(2.0, N.average(1, 2, 3), DELTA);
        assertEquals(2.5, N.average(new int[] { 1, 2, 3, 4 }, 1, 3), DELTA);
        assertEquals((Integer.MAX_VALUE + Integer.MIN_VALUE) / 2.0, N.average(Integer.MAX_VALUE, Integer.MIN_VALUE), DELTA);
    }

    @Test
    public void testSumIntGenericArray() {
        assertEquals(0, N.sumInt((Integer[]) null));
        assertEquals(0, N.sumInt(new Integer[] {}));
        assertEquals(6, N.sumInt(new Integer[] { 1, 2, 3 }));
        assertEquals(5, N.sumInt(new Integer[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(9, N.sumInt(new String[] { "1", "3", "5" }, s -> Integer.parseInt(s)));
        assertEquals(0, N.sumInt(new Integer[] { 1, 2, 3 }, 1, 1, Fn.numToInt()));

        assertThrows(ArithmeticException.class, () -> N.sumInt(new Integer[] { Integer.MAX_VALUE, 1 }));
    }

    @Test
    public void testSumIntGenericCollection() {
        assertEquals(6, N.sumInt(Arrays.asList(1, 2, 3)));
        assertEquals(5, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(9, N.sumInt(Arrays.asList("1", "3", "5"), s -> Integer.parseInt(s)));
        assertEquals(0, N.sumInt(Arrays.asList(1, 2, 3), 1, 1, Fn.numToInt()));
        assertThrows(ArithmeticException.class, () -> N.sumInt(Arrays.asList(Integer.MAX_VALUE, 1)));
    }

    @Test
    public void testSumIntToLongGenericIterable() {
        assertEquals(0L, N.sumIntToLong((Iterable<Integer>) null));
        assertEquals(0L, N.sumIntToLong(Collections.<Integer> emptyList()));
        assertEquals(6L, N.sumIntToLong(Arrays.asList(1, 2, 3)));
        assertEquals(Integer.MAX_VALUE + 1L, N.sumIntToLong(Arrays.asList(Integer.MAX_VALUE, 1)));
        assertEquals(9L, N.sumIntToLong(Arrays.asList("1", "3", "5"), s -> Integer.parseInt(s)));
    }

    @Test
    public void testSumLongGenericArray() {
        assertEquals(0L, N.sumLong((Long[]) null));
        assertEquals(6L, N.sumLong(new Long[] { 1L, 2L, 3L }));
        assertEquals(Long.MAX_VALUE, N.sumLong(new Long[] { Long.MAX_VALUE - 1L, 1L }));
        assertEquals(9L, N.sumLong(new String[] { "1", "3", "5" }, s -> Long.parseLong(s)));
    }

    @Test
    public void testSumDoubleGenericIterable() {
        assertEquals(0.0, N.sumDouble((Iterable<Double>) null), DELTA);
        assertEquals(6.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0)), DELTA);
        assertEquals(0.3, N.sumDouble(Arrays.asList(0.1, 0.2)), DELTA);
        assertEquals(0.6, N.sumDouble(Arrays.asList("0.1", "0.2", "0.3"), s -> Double.parseDouble(s)), DELTA);
    }

    @Test
    public void testSumBigIntegerIterable() {
        assertEquals(BigInteger.ZERO, N.sumBigInteger(null));
        assertEquals(BigInteger.ZERO, N.sumBigInteger(Collections.<BigInteger> emptyList()));
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(Arrays.asList("1", "2", "3"), s -> new BigInteger(s)));
        assertEquals(BigInteger.valueOf(5), N.sumBigInteger(Arrays.asList(BigInteger.ONE, null, BigInteger.valueOf(4)), Function.identity()));
    }

    @Test
    public void testSumBigDecimalIterable() {
        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(null));
        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(Collections.<BigDecimal> emptyList()));
        assertEquals(new BigDecimal("6.3"), N.sumBigDecimal(Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.0"))));
        assertEquals(new BigDecimal("6.3"), N.sumBigDecimal(Arrays.asList("1.1", "2.2", "3.0"), s -> new BigDecimal(s)));
    }

    @Test
    public void testAverageIntGenericArray() {
        assertEquals(0.0, N.averageInt((Integer[]) null), DELTA);
        assertEquals(0.0, N.averageInt(new Integer[] {}), DELTA);
        assertEquals(2.0, N.averageInt(new Integer[] { 1, 2, 3 }), DELTA);
        assertEquals(2.5, N.averageInt(new Integer[] { 1, 2, 3, 4 }, 1, 3), DELTA);
        assertEquals(3.0, N.averageInt(new String[] { "1", "3", "5" }, s -> Integer.parseInt(s)), DELTA);
    }

    @Test
    public void testAverageDoubleGenericIterable() {
        assertEquals(0.0, N.averageDouble((Iterable<Double>) null), DELTA);
        assertEquals(0.0, N.averageDouble(Collections.<Double> emptyList()), DELTA);
        assertEquals(2.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0)), DELTA);
        assertEquals(0.15, N.averageDouble(Arrays.asList(0.1, 0.2)), DELTA);
        assertEquals(0.2, N.averageDouble(Arrays.asList("0.1", "0.2", "0.3"), s -> Double.parseDouble(s)), DELTA);
    }

    @Test
    public void testAverageBigIntegerIterable() {
        assertEquals(BigDecimal.ZERO, N.averageBigInteger(null));
        assertEquals(BigDecimal.ZERO, N.averageBigInteger(Collections.emptyList()));
        assertEquals(new BigDecimal("2"), N.averageBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
    }

    @Test
    public void testAverageBigDecimalIterable() {
        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(null));
        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(Collections.emptyList()));
        assertEquals(new BigDecimal("2.1"), N.averageBigDecimal(Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.0"))));
    }

    @Test
    public void testMinMaxPrimitives() {
        assertEquals(1, N.min(1, 2));
        assertEquals('a', N.min('a', 'b', 'c'));
        assertEquals(1.0f, N.min(3.0f, 1.0f, 2.0f), DELTA);
        assertEquals(-1, N.min(new int[] { 3, 1, 4, 1, 5, 9, -1, 2, 6 }));
        assertThrows(IllegalArgumentException.class, () -> N.min(new int[] {}));

        assertEquals(2, N.max(1, 2));
        assertEquals('c', N.max('a', 'b', 'c'));
        assertEquals(3.0, N.max(new double[] { 3.0, 1.0, Double.NaN, 2.0 }), DELTA);
        assertEquals(1.0, N.max(new double[] { Double.NaN, 1.0 }), DELTA);
        assertEquals(9, N.max(new int[] { 3, 1, 4, 1, 5, 9, -1, 2, 6 }));
        assertThrows(IllegalArgumentException.class, () -> N.max(new int[] {}));

        assertEquals(Float.NaN, N.min(Float.NaN, 1.0f), DELTA);
        assertEquals(Float.NaN, N.min(1.0f, Float.NaN), DELTA);
        assertEquals(Float.NaN, N.min(Float.NaN, Float.NaN), DELTA);
        assertEquals(1.0f, N.min(1.0f, 2.0f), DELTA);

        assertEquals(1.0f, N.min(new float[] { 3.0f, 1.0f, Float.NaN, 2.0f }), DELTA);
        assertEquals(1.0f, N.min(new float[] { Float.NaN, 1.0f, 2.0f }), DELTA);
    }

    @Test
    public void testMinMaxComparables() {
        assertEquals("a", N.min("a", "b"));
        assertEquals("apple", N.min("banana", "apple", "cherry"));
        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 3, 1, 4, null, 1, 5 }, (a, b) -> {
            if (a == null)
                return 1;
            if (b == null)
                return -1;
            return a.compareTo(b);
        }));

        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 3, 1, null, 5 }));
        assertEquals(Integer.valueOf(5), N.max(new Integer[] { 3, 1, null, 5 }));

        List<String> strList = Arrays.asList("zebra", "apple", "Banana");
        assertEquals("apple", N.min(strList, String.CASE_INSENSITIVE_ORDER));
        assertEquals("zebra", N.max(strList, String.CASE_INSENSITIVE_ORDER));

        Iterator<Integer> iter = Arrays.asList(5, 2, 8, 2, 5).iterator();
        assertEquals(Integer.valueOf(2), N.min(iter));

        Iterator<Integer> iter2 = Arrays.asList(5, 2, 8, 2, 5).iterator();
        assertEquals(Integer.valueOf(8), N.max(iter2));

        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.emptyList()));
    }

    @Test
    public void testMinMaxBy() {
        String[] strs = { "apple", "Banana", "KIWI" };
        assertEquals("KIWI", N.minBy(strs, String::length));
        assertEquals("Banana", N.maxBy(strs, String::length));

        List<Pair<String, Integer>> pairs = Arrays.asList(Pair.of("A", 3), Pair.of("B", 1), Pair.of("C", 2));
        assertEquals(Pair.of("B", 1), N.minBy(pairs, it -> it.right()));
    }

    @Test
    public void testMinMaxAll() {
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 3, 1, 4, 1, 5 }));
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 3, 1, 4, 1, 5 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(5), N.maxAll(new Integer[] { 3, 1, 4, 1, 5 }));
        assertEquals(Arrays.asList(5, 5), N.maxAll(new Integer[] { 3, 5, 1, 4, 1, 5 }, Comparator.naturalOrder()));
        assertEquals(Collections.emptyList(), N.minAll(new Integer[] {}));
    }

    @Test
    public void testMinMaxOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(4), N.minOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, String::length, 100));
        assertEquals(Integer.valueOf(4), N.minOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, s -> s.equals("kiwi") ? 4 : s.length(), 100));
        assertEquals(Integer.valueOf(100), N.minOrDefaultIfEmpty(new String[] {}, String::length, 100));

        assertEquals(-9, N.minIntOrDefaultIfEmpty(new String[] { "apple", "kiwi" }, s -> s.charAt(0) - 'j', 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(new String[] {}, s -> s.charAt(0), 99));

        assertEquals(4L, N.minLongOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, String::length, 100L));
        assertEquals(0.33333333, N.minDoubleOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> 1.0 / x, 10.0), DELTA);

        assertEquals(Integer.valueOf(5), N.maxOrDefaultIfEmpty(new String[] { "apple", "kiwi", "plum" }, String::length, -1));
        assertEquals(-1, N.maxOrDefaultIfEmpty(new String[] {}, String::length, -1));
    }

    @Test
    public void testMinMaxPair() {
        Pair<Integer, Integer> p1 = N.minMax(new Integer[] { 3, 1, 4, 5, 2 });
        assertEquals(Pair.of(1, 5), p1);

        Pair<String, String> p2 = N.minMax(Arrays.asList("b", "c", "a"), String::compareTo);
        assertEquals(Pair.of("a", "c"), p2);

        assertThrows(IllegalArgumentException.class, () -> N.minMax(new Integer[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.emptyList()));
    }

    @Test
    public void testMedianPrimitives() {
        assertEquals(2, N.median(1, 3, 2));
        assertEquals('b', N.median('c', 'a', 'b'));
        assertEquals(2.0f, N.median(1.0f, 3.0f, 2.0f), DELTA);

        assertEquals(3, N.median(new int[] { 5, 1, 4, 2, 3 }));
        assertEquals(2, N.median(new int[] { 1, 2, 3, 4 }));

        assertThrows(IllegalArgumentException.class, () -> N.median(new int[] {}));
    }

    @Test
    public void testMedianGeneric() {
        assertEquals("banana", N.median(new String[] { "apple", "cherry", "banana" }));
        assertEquals(Integer.valueOf(3), N.median(Arrays.asList(5, 1, 4, 2, 3)));

        List<String> strList = Arrays.asList("zebra", "apple", "Banana");
        assertEquals("Banana", N.median(strList, String.CASE_INSENSITIVE_ORDER));

        assertThrows(IllegalArgumentException.class, () -> N.median(Collections.emptyList()));
    }

    @Test
    public void testKthLargestPrimitives() {
        assertEquals(4, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 2));
        assertEquals(1, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 5));
        assertEquals(5, N.kthLargest(new int[] { 5, 1, 4, 2, 3 }, 1));

        assertEquals(3.0f, N.kthLargest(new float[] { 1f, 5f, 2f, 4f, 3f }, 3), DELTA);

        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] {}, 1));
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2 }, 3));
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2 }, 0));
    }

    @Test
    public void testKthLargestGeneric() {
        assertEquals("cherry", N.kthLargest(new String[] { "apple", "cherry", "banana" }, 1, String.CASE_INSENSITIVE_ORDER));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(5, 1, 4, 2, 3, null), 2));

        List<Integer> listWithNulls = Arrays.asList(null, 1, 5, null, 3);
        assertEquals(Integer.valueOf(5), N.kthLargest(listWithNulls, 1));
        assertEquals(Integer.valueOf(1), N.kthLargest(listWithNulls, 3));
        assertNull(N.kthLargest(listWithNulls, 4));

        Comparator<Integer> nullsLargest = Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<Integer> forKthLargestNullsLargest = Comparator.nullsFirst(Comparator.reverseOrder());

        assertEquals(Integer.valueOf(3), N.kthLargest(listWithNulls, 2, forKthLargestNullsLargest));
        Comparator<Integer> cmpNullMax = Comparator.nullsLast(Comparator.naturalOrder());
        List<Integer> numbers = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        assertEquals(Integer.valueOf(6), N.kthLargest(numbers, 2));
    }

    @Test
    public void testTopPrimitives() {
        assertArrayEquals(new int[] { 5, 9, 6 }, N.top(new int[] { 3, 1, 5, 9, 2, 6 }, 3));
        List<Integer> top3 = CommonUtil.toList(N.top(new int[] { 3, 1, 5, 9, 2, 6 }, 3));
        assertTrue(top3.containsAll(Arrays.asList(5, 9, 6)) && top3.size() == 3);

        assertArrayEquals(new int[] {}, N.top(new int[] { 1, 2, 3 }, 0));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.top(new int[] { 1, 2, 3 }, 5));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.top(new int[] { 1, 2, 3 }, 3));
    }

    @Test
    public void testTopGeneric() {
        List<String> top2 = N.top(new String[] { "apple", "banana", "cherry", "date" }, 2);
        assertTrue(top2.containsAll(Arrays.asList("date", "cherry")) && top2.size() == 2);

        List<String> top2Sorted = N.top(new String[] { "apple", "banana", "cherry", "date" }, 2, Comparator.naturalOrder());
        assertTrue(top2Sorted.containsAll(Arrays.asList("date", "cherry")) && top2Sorted.size() == 2);

        List<Integer> numbers = Arrays.asList(1, 5, 2, 8, 2, 5);
        List<Integer> top3 = N.top(numbers, 3);
        Collections.sort(top3, Comparator.reverseOrder());
        assertEquals(Arrays.asList(8, 5, 5), top3);

        Integer[] arrKeepOrder = { 1, 5, 2, 8, 2, 6 };
        List<Integer> top3KeepOrder = N.top(arrKeepOrder, 3, true);
        assertEquals(Arrays.asList(5, 8, 6), top3KeepOrder);

        List<Integer> topAllKeepOrder = N.top(arrKeepOrder, 10, true);
        assertEquals(Arrays.asList(1, 5, 2, 8, 2, 6), topAllKeepOrder);

        List<Integer> top0KeepOrder = N.top(arrKeepOrder, 0, true);
        assertTrue(top0KeepOrder.isEmpty());
    }

    @Test
    public void testPercentilesIntArray() {
        int[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Integer> p = N.percentilesOfSorted(sorted);
        assertEquals(Integer.valueOf(6), p.get(Percentage._50));
        assertEquals(Integer.valueOf(10), p.get(Percentage._90));
        assertEquals(Integer.valueOf(1), p.get(Percentage._1));
        assertEquals(Integer.valueOf(10), p.get(Percentage._99));

        int[] single = { 5 };
        Map<Percentage, Integer> pSingle = N.percentilesOfSorted(single);
        assertEquals(Integer.valueOf(5), pSingle.get(Percentage._50));

        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new int[] {}));
    }

    @Test
    public void testPercentilesGenericList() {
        List<String> sorted = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        Map<Percentage, String> p = N.percentilesOfSorted(sorted);
        assertEquals("f", p.get(Percentage._50));
        assertEquals("j", p.get(Percentage._90));

        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.<String> emptyList()));

    }

    @Test
    public void testReplaceIfGenericArray() {
        String[] arr = { "apple", "banana", "apple", "cherry" };
        Predicate<String> predicate = s -> "apple".equals(s);
        int replacements = N.replaceIf(arr, predicate, "orange");
        assertArrayEquals(new String[] { "orange", "banana", "orange", "cherry" }, arr);
        assertEquals(2, replacements);

        Integer[] intArr = { 1, null, 1, 3 };
        Predicate<Integer> nullPredicate = Objects::isNull;
        replacements = N.replaceIf(intArr, nullPredicate, 0);
        assertArrayEquals(new Integer[] { 1, 0, 1, 3 }, intArr);
        assertEquals(1, replacements);

        assertEquals(0, N.replaceIf((String[]) null, predicate, "x"));
        assertEquals(0, N.replaceIf(new String[0], predicate, "x"));
    }

    @Test
    public void testReplaceAllGenericArray() {
        String[] arr = { "apple", "banana", "apple", "apple", null };
        int replacements = N.replaceAll(arr, "apple", "orange");
        assertArrayEquals(new String[] { "orange", "banana", "orange", "orange", null }, arr);
        assertEquals(3, replacements);

        replacements = N.replaceAll(arr, null, "grape");
        assertArrayEquals(new String[] { "orange", "banana", "orange", "orange", "grape" }, arr);
        assertEquals(1, replacements);

        assertEquals(0, N.replaceAll((String[]) null, "a", "x"));
        assertEquals(0, N.replaceAll(new String[0], "a", "x"));
    }

    @Test
    public void testReplaceAllBooleanArrayWithOperator() {
        boolean[] arr = { true, false, true };
        BooleanUnaryOperator operator = b -> !b;
        N.replaceAll(arr, operator);
        assertArrayEquals(new boolean[] { false, true, false }, arr);

        N.replaceAll((boolean[]) null, operator);
        N.replaceAll(new boolean[0], operator);
    }

    @Test
    public void testReplaceAllCharArrayWithOperator() {
        char[] arr = { 'a', 'b', 'c' };
        CharUnaryOperator operator = c -> (char) (c + 1);
        N.replaceAll(arr, operator);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, arr);

        N.replaceAll((char[]) null, operator);
        N.replaceAll(new char[0], operator);
    }

    @Test
    public void testReplaceAllByteArrayWithOperator() {
        byte[] arr = { 1, 2, 3 };
        ByteUnaryOperator operator = b -> (byte) (b * 2);
        N.replaceAll(arr, operator);
        assertArrayEquals(new byte[] { 2, 4, 6 }, arr);

        N.replaceAll((byte[]) null, operator);
        N.replaceAll(new byte[0], operator);
    }

    @Test
    public void testReplaceAllShortArrayWithOperator() {
        short[] arr = { 10, 20, 30 };
        ShortUnaryOperator operator = s -> (short) (s / 2);
        N.replaceAll(arr, operator);
        assertArrayEquals(new short[] { 5, 10, 15 }, arr);

        N.replaceAll((short[]) null, operator);
        N.replaceAll(new short[0], operator);
    }

    @Test
    public void testReplaceAllIntArrayWithOperator() {
        int[] arr = { 1, 2, 3 };
        IntUnaryOperator operator = i -> i * i;
        N.replaceAll(arr, operator);
        assertArrayEquals(new int[] { 1, 4, 9 }, arr);

        N.replaceAll((int[]) null, operator);
        N.replaceAll(new int[0], operator);
    }

    @Test
    public void testReplaceAllLongArrayWithOperator() {
        long[] arr = { 10L, 20L, 30L };
        LongUnaryOperator operator = l -> l - 5L;
        N.replaceAll(arr, operator);
        assertArrayEquals(new long[] { 5L, 15L, 25L }, arr);

        N.replaceAll((long[]) null, operator);
        N.replaceAll(new long[0], operator);
    }

    @Test
    public void testReplaceAllFloatArrayWithOperator() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        FloatUnaryOperator operator = f -> f * 1.5f;
        N.replaceAll(arr, operator);
        assertFloatArrayEquals(new float[] { 1.5f, 3.0f, 4.5f }, arr, 0.001f);

        N.replaceAll((float[]) null, operator);
        N.replaceAll(new float[0], operator);
    }

    @Test
    public void testReplaceAllDoubleArrayWithOperator() {
        double[] arr = { 1.0, 2.0, 3.0 };
        DoubleUnaryOperator operator = d -> d / 2.0;
        N.replaceAll(arr, operator);
        assertDoubleArrayEquals(new double[] { 0.5, 1.0, 1.5 }, arr, 0.001);

        N.replaceAll((double[]) null, operator);
        N.replaceAll(new double[0], operator);
    }

    @Test
    public void testReplaceAllGenericArrayWithOperator() {
        String[] arr = { "a", "b", "c" };
        UnaryOperator<String> operator = s -> s.toUpperCase();
        N.replaceAll(arr, operator);
        assertArrayEquals(new String[] { "A", "B", "C" }, arr);

        N.replaceAll((String[]) null, operator);
        N.replaceAll(new String[0], operator);
    }

    @Test
    public void testReplaceAllListWithOperator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        UnaryOperator<String> operator = s -> s.toUpperCase();
        N.replaceAll(list, operator);
        assertEquals(Arrays.asList("A", "B", "C"), list);

        List<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3));
        UnaryOperator<Integer> intOp = i -> i * 2;
        N.replaceAll(linkedList, intOp);
        assertEquals(Arrays.asList(2, 4, 6), linkedList);

        N.replaceAll((List<String>) null, operator);
        N.replaceAll(new ArrayList<String>(), operator);
    }

    @Test
    public void testUpdateAllArray() throws IOException {
        String[] arr = { "a", "b" };
        Throwables.UnaryOperator<String, IOException> operator = s -> {
            if ("b".equals(s))
                throw new IOException("Test Exception");
            return s.toUpperCase();
        };

        assertThrows(IOException.class, () -> N.updateAll(arr, operator));
        assertEquals("A", arr[0]);
        assertEquals("b", arr[1]);

        String[] arr2 = { "c", "d" };
        Throwables.UnaryOperator<String, IOException> noExceptionOp = String::toUpperCase;
        N.updateAll(arr2, noExceptionOp);
        assertArrayEquals(new String[] { "C", "D" }, arr2);

        N.updateAll((String[]) null, noExceptionOp);
        N.updateAll(new String[0], noExceptionOp);
    }

    @Test
    public void testUpdateAllList() throws IOException {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Throwables.UnaryOperator<String, IOException> operator = s -> {
            if ("b".equals(s))
                throw new IOException("Test Exception on List");
            return s.toUpperCase();
        };

        assertThrows(IOException.class, () -> N.updateAll(list, operator));
        assertEquals("A", list.get(0));
        assertEquals("b", list.get(1));

        List<String> list2 = new LinkedList<>(Arrays.asList("c", "d"));
        Throwables.UnaryOperator<String, IOException> noExceptionOp = String::toUpperCase;
        N.updateAll(list2, noExceptionOp);
        assertEquals(Arrays.asList("C", "D"), list2);

        N.updateAll((List<String>) null, noExceptionOp);
        N.updateAll(new ArrayList<String>(), noExceptionOp);
    }

    @Test
    public void testUpdateAllUsingReplaceAllInstead() {
        assertThrows(UnsupportedOperationException.class, N::updateAllUsingReplaceAllInstead);
    }

    @Test
    public void testUpdateIfUsingReplaceIfInstead() {
        assertThrows(UnsupportedOperationException.class, N::updateIfUsingReplaceIfInstead);
    }

    @Test
    public void testSetAllGenericArray() {
        String[] arr = new String[3];
        IntFunction<String> generator = i -> "Val" + i;
        N.setAll(arr, generator);
        assertArrayEquals(new String[] { "Val0", "Val1", "Val2" }, arr);

        N.setAll((String[]) null, generator);
        N.setAll(new String[0], generator);
    }

    @Test
    public void testSetAllGenericArrayWithConverter() throws IOException {
        String[] arr = { "a", "b", "c" };
        Throwables.IntObjFunction<String, String, IOException> converter = (idx, val) -> {
            if ("b".equals(val))
                throw new IOException("Converter Exception");
            return val.toUpperCase() + idx;
        };
        assertThrows(IOException.class, () -> N.setAll(arr, converter));
        assertEquals("A0", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);

        String[] arr2 = { "x", "y" };
        Throwables.IntObjFunction<String, String, IOException> noExConverter = (idx, val) -> val.toUpperCase() + idx;
        N.setAll(arr2, noExConverter);
        assertArrayEquals(new String[] { "X0", "Y1" }, arr2);

        N.setAll((String[]) null, noExConverter);
        N.setAll(new String[0], noExConverter);
    }

    @Test
    public void testSetAllListWithConverter() throws IOException {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Throwables.IntObjFunction<String, String, IOException> converter = (idx, val) -> {
            if ("b".equals(val) && idx == 1)
                throw new IOException("Converter Exception on List");
            return val.toUpperCase() + idx;
        };
        assertThrows(IOException.class, () -> N.setAll(list, converter));
        assertEquals("A0", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> list2 = new LinkedList<>(Arrays.asList("x", "y"));
        Throwables.IntObjFunction<String, String, IOException> noExConverter = (idx, val) -> val.toUpperCase() + idx;
        N.setAll(list2, noExConverter);
        assertEquals(Arrays.asList("X0", "Y1"), list2);

        N.setAll((List<String>) null, noExConverter);
        N.setAll(new ArrayList<String>(), noExConverter);
    }

    @Test
    public void testCopyThenSetAllArrayWithGenerator() {
        String[] original = { "a", "b", "c" };
        String[] originalCopy = Arrays.copyOf(original, original.length);
        IntFunction<String> generator = i -> "New" + i;

        String[] result = N.copyThenSetAll(original, generator);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "New0", "New1", "New2" }, result);

        assertNull(N.copyThenSetAll((String[]) null, generator));
        assertArrayEquals(new String[0], N.copyThenSetAll(new String[0], generator));
    }

    @Test
    public void testCopyThenSetAllArrayWithConverter() throws IOException {
        String[] original = { "a", "b", "c" };
        String[] originalCopy = Arrays.copyOf(original, original.length);
        Throwables.IntObjFunction<String, String, IOException> converter = (idx, val) -> val.toUpperCase() + idx;

        String[] result = N.copyThenSetAll(original, converter);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "A0", "B1", "C2" }, result);

        Throwables.IntObjFunction<String, String, IOException> exConverter = (idx, val) -> {
            if (idx == 1)
                throw new IOException("Test");
            return val;
        };
        assertThrows(IOException.class, () -> N.copyThenSetAll(original, exConverter));

        assertNull(N.copyThenSetAll((String[]) null, converter));
        assertArrayEquals(new String[0], N.copyThenSetAll(new String[0], converter));
    }

    @Test
    public void testCopyThenReplaceAllArray() {
        String[] original = { "a", "b", "a" };
        String[] originalCopy = Arrays.copyOf(original, original.length);
        UnaryOperator<String> operator = s -> "a".equals(s) ? "x" : s;

        String[] result = N.copyThenReplaceAll(original, operator);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "x", "b", "x" }, result);

        assertNull(N.copyThenReplaceAll(null, operator));
        assertArrayEquals(new String[0], N.copyThenReplaceAll(new String[0], operator));
    }

    @Test
    public void testCopyThenUpdateAllArray() throws IOException {
        String[] original = { "a", "b", "c" };
        String[] originalCopy = Arrays.copyOf(original, original.length);
        Throwables.UnaryOperator<String, IOException> operator = s -> s.toUpperCase();

        String[] result = N.copyThenUpdateAll(original, operator);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "A", "B", "C" }, result);

        Throwables.UnaryOperator<String, IOException> exOperator = s -> {
            if ("b".equals(s))
                throw new IOException("Test");
            return s;
        };
        assertThrows(IOException.class, () -> N.copyThenUpdateAll(original, exOperator));

        assertNull(N.copyThenUpdateAll(null, operator));
        assertArrayEquals(new String[0], N.copyThenUpdateAll(new String[0], operator));
    }

    @Test
    public void testAddBoolean() {
        assertArrayEquals(new boolean[] { true }, N.add(new boolean[0], true));
        assertArrayEquals(new boolean[] { false, true }, N.add(new boolean[] { false }, true));
        assertArrayEquals(new boolean[] { true }, N.add((boolean[]) null, true));
    }

    @Test
    public void testAddChar() {
        assertArrayEquals(new char[] { 'a' }, N.add(new char[0], 'a'));
        assertArrayEquals(new char[] { 'x', 'a' }, N.add(new char[] { 'x' }, 'a'));
        assertArrayEquals(new char[] { 'a' }, N.add((char[]) null, 'a'));
    }

    @Test
    public void testAddByte() {
        assertArrayEquals(new byte[] { 1 }, N.add(new byte[0], (byte) 1));
        assertArrayEquals(new byte[] { 5, 1 }, N.add(new byte[] { 5 }, (byte) 1));
        assertArrayEquals(new byte[] { 1 }, N.add((byte[]) null, (byte) 1));
    }

    @Test
    public void testAddShort() {
        assertArrayEquals(new short[] { 10 }, N.add(new short[0], (short) 10));
        assertArrayEquals(new short[] { 50, 10 }, N.add(new short[] { 50 }, (short) 10));
        assertArrayEquals(new short[] { 10 }, N.add((short[]) null, (short) 10));
    }

    @Test
    public void testAddInt() {
        assertArrayEquals(new int[] { 100 }, N.add(new int[0], 100));
        assertArrayEquals(new int[] { 500, 100 }, N.add(new int[] { 500 }, 100));
        assertArrayEquals(new int[] { 100 }, N.add((int[]) null, 100));
    }

    @Test
    public void testAddLong() {
        assertArrayEquals(new long[] { 1000L }, N.add(new long[0], 1000L));
        assertArrayEquals(new long[] { 5000L, 1000L }, N.add(new long[] { 5000L }, 1000L));
        assertArrayEquals(new long[] { 1000L }, N.add((long[]) null, 1000L));
    }

    @Test
    public void testAddFloat() {
        assertFloatArrayEquals(new float[] { 1.5f }, N.add(new float[0], 1.5f), 0.0f);
        assertFloatArrayEquals(new float[] { 5.5f, 1.5f }, N.add(new float[] { 5.5f }, 1.5f), 0.0f);
        assertFloatArrayEquals(new float[] { 1.5f }, N.add((float[]) null, 1.5f), 0.0f);
    }

    @Test
    public void testAddDouble() {
        assertDoubleArrayEquals(new double[] { 1.5 }, N.add(new double[0], 1.5), 0.0);
        assertDoubleArrayEquals(new double[] { 5.5, 1.5 }, N.add(new double[] { 5.5 }, 1.5), 0.0);
        assertDoubleArrayEquals(new double[] { 1.5 }, N.add((double[]) null, 1.5), 0.0);
    }

    @Test
    public void testAddGenericArray() {
        assertArrayEquals(new Integer[] { 1 }, N.add(new Integer[0], 1));
        assertArrayEquals(new Integer[] { 5, 1 }, N.add(new Integer[] { 5 }, 1));
        assertThrows(IllegalArgumentException.class, () -> N.add((Integer[]) null, 1));
    }

    @Test
    public void testAddAllGenericArray() {
        assertArrayEquals(new Integer[] { 1, 2 }, N.addAll(new Integer[0], 1, 2));
        assertArrayEquals(new Integer[] { 0, 1, 2 }, N.addAll(new Integer[] { 0 }, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Integer[]) null, 1, 2));

        Integer[] arr = { 1 };
        Integer[] result = N.addAll(arr, (Integer[]) null);
        assertArrayEquals(new Integer[] { 1 }, result);

        Integer[] arr2 = {};
        Integer[] result2 = N.addAll(arr2, (Integer[]) null);
        assertArrayEquals(new Integer[] {}, result2);
    }

    @Test
    public void testAddAllToCollectionVarArgs() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        assertTrue(N.addAll(coll, "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c"), coll);
        assertFalse(N.addAll(coll));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, "a"));
    }

    @Test
    public void testAddAllToCollectionIterable() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        assertTrue(N.addAll(coll, Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "b", "c"), coll);
        assertFalse(N.addAll(coll, (Iterable<String>) null));
        assertFalse(N.addAll(coll, new ArrayList<String>()));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, Arrays.asList("a")));
    }

    @Test
    public void testAddAllToCollectionIterator() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        assertTrue(N.addAll(coll, Arrays.asList("b", "c").iterator()));
        assertEquals(Arrays.asList("a", "b", "c"), coll);
        assertFalse(N.addAll(coll, (Iterator<String>) null));
        assertFalse(N.addAll(coll, new ArrayList<String>().iterator()));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, Arrays.asList("a").iterator()));
    }

    @Test
    public void testInsertBoolean() {
        assertArrayEquals(new boolean[] { true }, N.insert(new boolean[0], 0, true));
        assertArrayEquals(new boolean[] { true, false }, N.insert(new boolean[] { false }, 0, true));
        assertArrayEquals(new boolean[] { false, true }, N.insert(new boolean[] { false }, 1, true));
        assertArrayEquals(new boolean[] { false, true, true }, N.insert(new boolean[] { false, true }, 1, true));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new boolean[] { true }, 2, false));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new boolean[] { true }, -1, false));
    }

    @Test
    public void testInsertInt() {
        assertArrayEquals(new int[] { 5, 1, 2 }, N.insert(new int[] { 1, 2 }, 0, 5));
        assertArrayEquals(new int[] { 1, 5, 2 }, N.insert(new int[] { 1, 2 }, 1, 5));
        assertArrayEquals(new int[] { 1, 2, 5 }, N.insert(new int[] { 1, 2 }, 2, 5));
    }

    @Test
    public void testInsertGenericArray() {
        assertArrayEquals(new Integer[] { 5, 1, 2 }, N.insert(new Integer[] { 1, 2 }, 0, 5));
        assertArrayEquals(new Integer[] { 1, 5, 2 }, N.insert(new Integer[] { 1, 2 }, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> N.insert((Integer[]) null, 0, 1));
    }

    @Test
    public void testInsertString() {
        assertEquals("ab", N.insert("b", 0, "a"));
        assertEquals("ba", N.insert("b", 1, "a"));
        assertEquals("hello world", N.insert("helloworld", 5, " "));
        assertEquals("test", N.insert("test", 0, ""));
        assertEquals("test", N.insert("test", 4, null));
        assertEquals("abc", N.insert("", 0, "abc"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert("abc", 4, "d"));
    }

    @Test
    public void testInsertAllBooleanArray() {
        assertArrayEquals(new boolean[] { true, false, true, false }, N.insertAll(new boolean[] { true, false }, 0, true, false));
        assertArrayEquals(new boolean[] { true, true, false, false }, N.insertAll(new boolean[] { true, false }, 1, true, false));
        assertArrayEquals(new boolean[] { true, false, true, false }, N.insertAll(new boolean[] { true, false }, 2, true, false));
        assertArrayEquals(new boolean[] { true, false }, N.insertAll(new boolean[0], 0, true, false));
    }

    @Test
    public void testInsertAllIntArray() {
        assertArrayEquals(new int[] { 5, 6, 1, 2, 3 }, N.insertAll(new int[] { 1, 2, 3 }, 0, 5, 6));
        assertArrayEquals(new int[] { 1, 5, 6, 2, 3 }, N.insertAll(new int[] { 1, 2, 3 }, 1, 5, 6));
    }

    @Test
    public void testInsertAllGenericArray() {
        assertArrayEquals(new Integer[] { 5, 6, 1, 2, 3 }, N.insertAll(new Integer[] { 1, 2, 3 }, 0, 5, 6));
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((Integer[]) null, 0, 1, 2));
    }

    @Test
    public void testInsertAllListVarArgs() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "d"));
        assertTrue(N.insertAll(list, 1, "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        assertFalse(N.insertAll(list, 1));
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((List<String>) null, 0, "a"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll(list, 10, "x"));
    }

    @Test
    public void testInsertAllListCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "d"));
        assertTrue(N.insertAll(list, 1, Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        assertFalse(N.insertAll(list, 1, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((List<String>) null, 0, Arrays.asList("a")));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll(list, 10, Arrays.asList("x")));
    }

    @Test
    public void testDeleteByIndexBoolean() {
        assertArrayEquals(new boolean[] { false }, N.removeAt(new boolean[] { true, false }, 0));
        assertArrayEquals(new boolean[] { true }, N.removeAt(new boolean[] { true, false }, 1));
        assertArrayEquals(new boolean[0], N.removeAt(new boolean[] { true }, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new boolean[] { true }, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new boolean[0], 0));
    }

    @Test
    public void testDeleteByIndexInt() {
        assertArrayEquals(new int[] { 2, 3 }, N.removeAt(new int[] { 1, 2, 3 }, 0));
        assertArrayEquals(new int[] { 1, 3 }, N.removeAt(new int[] { 1, 2, 3 }, 1));
        assertArrayEquals(new int[] { 1, 2 }, N.removeAt(new int[] { 1, 2, 3 }, 2));
    }

    @Test
    public void testDeleteByIndexGeneric() {
        assertArrayEquals(new String[] { "b", "c" }, N.removeAt(new String[] { "a", "b", "c" }, 0));
        String[] emptyArr = {};
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(emptyArr, 0));
        String[] nullArr = null;
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(nullArr, 0));

    }

    @Test
    public void testDeleteAllByIndicesBoolean() {
        assertArrayEquals(new boolean[] { true, true }, N.removeAt(new boolean[] { true, false, true }, 1));
        assertArrayEquals(new boolean[] { false }, N.removeAt(new boolean[] { true, false, true }, 0, 2));
        assertArrayEquals(new boolean[] { true, false, true }, N.removeAt(new boolean[] { true, false, true }));
        assertArrayEquals(new boolean[0], N.removeAt(new boolean[] { true, false, true }, 0, 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new boolean[0], 0, 1, 2));
        assertArrayEquals(new boolean[0], N.removeAt(new boolean[0]));

        boolean[] arrToClone = { true, false };
        boolean[] cloned = N.removeAt(arrToClone);
        assertNotSame(arrToClone, cloned);
        assertArrayEquals(arrToClone, cloned);

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new boolean[] { true }, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new boolean[] { true }, -1));
    }

    @Test
    public void testDeleteAllByIndicesInt() {
        assertArrayEquals(new int[] { 1, 4 }, N.removeAt(new int[] { 1, 2, 3, 4 }, 1, 2));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.removeAt(new int[] { 1, 2, 3, 4 }));
        assertArrayEquals(new int[] { 4 }, N.removeAt(new int[] { 1, 2, 3, 4 }, 0, 1, 2));
        assertArrayEquals(new int[] {}, N.removeAt(new int[] { 1, 2, 3, 4 }, 0, 1, 2, 3));
    }

    @Test
    public void testDeleteAllByIndicesGeneric() {
        assertArrayEquals(new String[] { "a", "d" }, N.removeAt(new String[] { "a", "b", "c", "d" }, 1, 2));
        String[] arrNull = null;
        assertThrows(NullPointerException.class, () -> N.removeAt(arrNull, 1, 2));
    }

    @Test
    public void testDeleteAllByIndicesList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeAt(list, 1, 3));
        assertEquals(Arrays.asList("a", "c", "e"), list);

        List<String> list2 = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeAt(list2, 0, 4));
        assertEquals(Arrays.asList("b", "c", "d"), list2);

        assertFalse(N.removeAt(list, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> N.removeAt((List<String>) null, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(list, 10));

        List<Integer> intList = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        N.removeAt(intList, 0, 0, 1);
        assertEquals(Arrays.asList(3, 4, 5), intList);

    }

    @Test
    public void testRemoveBoolean() {
        assertArrayEquals(new boolean[] { false }, N.remove(new boolean[] { true, false }, true));
        assertArrayEquals(new boolean[] { true, true }, N.remove(new boolean[] { true, false, true }, false));
        assertArrayEquals(new boolean[] { true }, N.remove(new boolean[] { true }, false));
        assertArrayEquals(new boolean[0], N.remove(new boolean[0], true));
        assertArrayEquals(new boolean[0], N.remove(null, true));
    }

    @Test
    public void testRemoveInt() {
        assertArrayEquals(new int[] { 2, 3, 1 }, N.remove(new int[] { 1, 2, 3, 1 }, 1));
        assertArrayEquals(new int[] { 1, 3 }, N.remove(new int[] { 1, 2, 3 }, 2));
    }

    @Test
    public void testRemoveGenericArray() {
        assertArrayEquals(new String[] { "b", "c", "a" }, N.remove(new String[] { "a", "b", "c", "a" }, "a"));
        assertArrayEquals(new String[] { "a", "b" }, N.remove(new String[] { "a", "b" }, "c"));
        String[] arrNull = null;
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.remove(arrNull, "a"));
        String[] emptyArr = {};
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.remove(emptyArr, "a"));
    }

    @Test
    public void testRemoveFromCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        assertTrue(N.remove(coll, "a"));
        assertEquals(Arrays.asList("b", "a", "c"), coll);
        assertFalse(N.remove(coll, "d"));
        assertFalse(N.remove(new ArrayList<String>(), "a"));
        assertFalse(N.remove((Collection<String>) null, "a"));
    }

    @Test
    public void testRemoveAllBooleanArray() {
        assertArrayEquals(new boolean[] { false }, N.removeAll(new boolean[] { true, false, true }, true));
        assertArrayEquals(new boolean[0], N.removeAll(new boolean[] { true, false, true }, true, false));
        assertArrayEquals(new boolean[] { true, false }, N.removeAll(new boolean[] { true, false }, new boolean[0]));
        assertArrayEquals(new boolean[0], N.removeAll(new boolean[0], true));
        assertArrayEquals(new boolean[0], N.removeAll((boolean[]) null, true));
    }

    @Test
    public void testRemoveAllIntArray() {
        assertArrayEquals(new int[] { 3 }, N.removeAll(new int[] { 1, 2, 1, 3, 2 }, 1, 2));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeAll(new int[] { 1, 2, 3 }, 4, 5));
    }

    @Test
    public void testRemoveAllGenericArray() {
        assertArrayEquals(new String[] { "c" }, N.removeAll(new String[] { "a", "b", "a", "c", "b" }, "a", "b"));
        String[] arr = { "a", "b" };
        String[] result = N.removeAll(arr, "x", "y");
        assertNotSame(arr, result);
        assertArrayEquals(arr, result);

        String[] arrNull = null;
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAll(arrNull, "a"));
        String[] emptyArr = {};
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAll(emptyArr, "a"));
    }

    @Test
    public void testRemoveAllFromCollectionVarArgs() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(coll, "a", "b"));
        assertEquals(Arrays.asList("c", "d"), coll);
        assertFalse(N.removeAll(coll, "x", "y"));
        assertFalse(N.removeAll(new ArrayList<String>(), "a"));
        assertFalse(N.removeAll(coll));
    }

    @Test
    public void testRemoveAllFromCollectionIterable() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(coll, Arrays.asList("a", "b")));
        assertEquals(Arrays.asList("c", "d"), coll);
        assertFalse(N.removeAll(coll, Arrays.asList("x", "y")));
        assertFalse(N.removeAll(coll, (Iterable<String>) null));

        Collection<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(set, Arrays.asList("a", "x")));
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), set);
    }

    @Test
    public void testRemoveAllFromCollectionIterator() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(coll, Arrays.asList("a", "b").iterator()));
        assertEquals(Arrays.asList("c", "d"), coll);
        assertFalse(N.removeAll(coll, Arrays.asList("x", "y").iterator()));
        assertFalse(N.removeAll(coll, (Iterator<String>) null));

        Collection<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(set, Arrays.asList("a", "x").iterator()));
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), set);
    }

    @Test
    public void testRemoveAllOccurrencesBoolean() {
        assertArrayEquals(new boolean[] { false }, N.removeAllOccurrences(new boolean[] { true, false, true }, true));
        assertArrayEquals(new boolean[] { true, true }, N.removeAllOccurrences(new boolean[] { true, false, true }, false));
        assertArrayEquals(new boolean[0], N.removeAllOccurrences(new boolean[0], true));
        assertArrayEquals(new boolean[0], N.removeAllOccurrences(null, true));
    }

    @Test
    public void testRemoveAllOccurrencesInt() {
        assertArrayEquals(new int[] { 2, 3 }, N.removeAllOccurrences(new int[] { 1, 2, 1, 3, 1 }, 1));
    }

    @Test
    public void testRemoveAllOccurrencesGenericArray() {
        assertArrayEquals(new String[] { "b", "c" }, N.removeAllOccurrences(new String[] { "a", "b", "a", "c", "a" }, "a"));
        String[] arrNull = null;
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAllOccurrences(arrNull, "a"));
    }

    @Test
    public void testRemoveAllOccurrencesFromCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "a"));
        assertTrue(N.removeAllOccurrences(coll, "a"));
        assertEquals(Arrays.asList("b", "c"), coll);
        assertFalse(N.removeAllOccurrences(coll, "d"));
        assertFalse(N.removeAllOccurrences(new ArrayList<String>(), "a"));
    }

    @Test
    public void testRemoveDuplicatesBooleanArrayDeprecated() {
        assertArrayEquals(new boolean[] { true, false }, N.removeDuplicates(new boolean[] { true, false, true }));
        assertArrayEquals(new boolean[] { true }, N.removeDuplicates(new boolean[] { true, true, true }));
        assertArrayEquals(new boolean[0], N.removeDuplicates(new boolean[0]));
        assertArrayEquals(new boolean[0], N.removeDuplicates((boolean[]) null));
    }

    @Test
    public void testRemoveDuplicatesCharArray() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'b', 'a', 'c', 'b' }));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'a', 'b', 'c', 'c' }, true));
        assertArrayEquals(new char[] { 'a' }, N.removeDuplicates(new char[] { 'a', 'a', 'a' }));
        assertArrayEquals(new char[0], N.removeDuplicates(new char[0]));
        assertArrayEquals(new char[0], N.removeDuplicates((char[]) null));
    }

    @Test
    public void testRemoveDuplicatesCharArrayIsSorted() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'b', 'a', 'c', 'b' }, false));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'a', 'b', 'c', 'c' }, true));
    }

    @Test
    public void testRemoveDuplicatesCharArrayRangeIsSorted() {
        assertArrayEquals(new char[] { 'b', 'a' }, N.removeDuplicates(new char[] { 'x', 'b', 'b', 'a', 'y' }, 1, 4, false));
        assertArrayEquals(new char[] { 'a', 'b' }, N.removeDuplicates(new char[] { 'x', 'a', 'a', 'b', 'y' }, 1, 4, true));
        assertArrayEquals(new char[0], N.removeDuplicates(new char[] { 'a', 'b' }, 1, 1, true));
        assertArrayEquals(new char[] { 'b' }, N.removeDuplicates(new char[] { 'a', 'b', 'c' }, 1, 2, false));
    }

    @Test
    public void testRemoveDuplicatesIntArray() {
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeDuplicates(new int[] { 1, 2, 1, 3, 2 }));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeDuplicates(new int[] { 1, 1, 2, 3, 3 }, true));
    }

    @Test
    public void testRemoveDuplicatesStringArray() {
        assertArrayEquals(new String[] { "a", "b", "c" }, N.removeDuplicates(new String[] { "a", "b", "a", "c", "b" }));
        assertArrayEquals(new String[] { "a", "b", "c" }, N.removeDuplicates(new String[] { "a", "a", "b", "c", "c" }, true));
        String[] arrNull = null;
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeDuplicates(arrNull));
    }

    @Test
    public void testRemoveDuplicatesGenericArray() {
        Integer[] in = { 1, 2, 1, 3, null, 2, null };
        Integer[] expected = { 1, 2, 3, null };
        assertArrayEquals(expected, N.removeDuplicates(in));

        Integer[] inSorted = { 1, 1, 2, 2, 3, null, null };
        Integer[] expectedSorted = { 1, 2, 3, null };
        assertArrayEquals(expectedSorted, N.removeDuplicates(inSorted, true));

        Integer[] arrNull = null;
        assertSame(arrNull, N.removeDuplicates(arrNull));
    }

    @Test
    public void testRemoveDuplicatesCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b"));
        assertTrue(N.removeDuplicates(coll));
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), new HashSet<>(coll));
        assertEquals(3, coll.size());

        Collection<String> noDups = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertFalse(N.removeDuplicates(noDups));
        assertEquals(Arrays.asList("a", "b", "c"), noDups);

        Collection<String> emptyColl = new ArrayList<>();
        assertFalse(N.removeDuplicates(emptyColl));

        Collection<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertFalse(N.removeDuplicates(set));

        Collection<Integer> sortedList = new LinkedList<>(Arrays.asList(1, 1, 2, 3, 3, 3, 4));
        assertTrue(N.removeDuplicates(sortedList, true));
        assertEquals(Arrays.asList(1, 2, 3, 4), sortedList);

        Collection<Integer> unsortedList = new LinkedList<>(Arrays.asList(3, 1, 2, 1, 3));
        assertTrue(N.removeDuplicates(unsortedList, false));
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(unsortedList));
        assertEquals(3, unsortedList.size());

    }

    @Test
    public void testOccurrencesOf_booleanArray() {
        assertEquals(0, N.frequency((boolean[]) null, true));
        assertEquals(0, N.frequency(EMPTY_BOOLEAN_ARRAY_CONST, true));
        assertEquals(2, N.frequency(new boolean[] { true, false, true }, true));
        assertEquals(1, N.frequency(new boolean[] { true, false, true }, false));
        assertEquals(0, N.frequency(new boolean[] { false, false }, true));
        assertEquals(3, N.frequency(new boolean[] { true, true, true }, true));
    }

    @Test
    public void testOccurrencesOf_charArray() {
        assertEquals(0, N.frequency((char[]) null, 'a'));
        assertEquals(0, N.frequency(EMPTY_CHAR_ARRAY_CONST, 'a'));
        assertEquals(2, N.frequency(new char[] { 'a', 'b', 'a' }, 'a'));
        assertEquals(1, N.frequency(new char[] { 'a', 'b', 'c' }, 'b'));
        assertEquals(0, N.frequency(new char[] { 'b', 'c' }, 'a'));
    }

    @Test
    public void testOccurrencesOf_byteArray() {
        assertEquals(0, N.frequency((byte[]) null, (byte) 1));
        assertEquals(0, N.frequency(EMPTY_BYTE_ARRAY_CONST, (byte) 1));
        assertEquals(2, N.frequency(new byte[] { 1, 2, 1 }, (byte) 1));
        assertEquals(1, N.frequency(new byte[] { 1, 2, 3 }, (byte) 2));
        assertEquals(0, N.frequency(new byte[] { 2, 3 }, (byte) 1));
    }

    @Test
    public void testOccurrencesOf_shortArray() {
        assertEquals(0, N.frequency((short[]) null, (short) 1));
        assertEquals(0, N.frequency(EMPTY_SHORT_ARRAY_CONST, (short) 1));
        assertEquals(2, N.frequency(new short[] { 1, 2, 1 }, (short) 1));
        assertEquals(1, N.frequency(new short[] { 1, 2, 3 }, (short) 2));
        assertEquals(0, N.frequency(new short[] { 2, 3 }, (short) 1));
    }

    @Test
    public void testOccurrencesOf_intArray() {
        assertEquals(0, N.frequency((int[]) null, 1));
        assertEquals(0, N.frequency(EMPTY_INT_ARRAY_CONST, 1));
        assertEquals(2, N.frequency(new int[] { 1, 2, 1 }, 1));
        assertEquals(1, N.frequency(new int[] { 1, 2, 3 }, 2));
        assertEquals(0, N.frequency(new int[] { 2, 3 }, 1));
    }

    @Test
    public void testOccurrencesOf_longArray() {
        assertEquals(0, N.frequency((long[]) null, 1L));
        assertEquals(0, N.frequency(EMPTY_LONG_ARRAY_CONST, 1L));
        assertEquals(2, N.frequency(new long[] { 1L, 2L, 1L }, 1L));
        assertEquals(1, N.frequency(new long[] { 1L, 2L, 3L }, 2L));
        assertEquals(0, N.frequency(new long[] { 2L, 3L }, 1L));
    }

    @Test
    public void testOccurrencesOf_floatArray() {
        assertEquals(0, N.frequency((float[]) null, 1.0f));
        assertEquals(0, N.frequency(EMPTY_FLOAT_ARRAY_CONST, 1.0f));
        assertEquals(2, N.frequency(new float[] { 1.0f, 2.0f, 1.0f }, 1.0f));
        assertEquals(1, N.frequency(new float[] { 1.0f, 2.0f, 3.0f }, 2.0f));
        assertEquals(0, N.frequency(new float[] { 2.0f, 3.0f }, 1.0f));
        assertEquals(1, N.frequency(new float[] { Float.NaN, 1.0f }, Float.NaN));
    }

    @Test
    public void testOccurrencesOf_doubleArray() {
        assertEquals(0, N.frequency((double[]) null, 1.0));
        assertEquals(0, N.frequency(EMPTY_DOUBLE_ARRAY_CONST, 1.0));
        assertEquals(2, N.frequency(new double[] { 1.0, 2.0, 1.0 }, 1.0));
        assertEquals(1, N.frequency(new double[] { 1.0, 2.0, 3.0 }, 2.0));
        assertEquals(0, N.frequency(new double[] { 2.0, 3.0 }, 1.0));
        assertEquals(1, N.frequency(new double[] { Double.NaN, 1.0 }, Double.NaN));
    }

    @Test
    public void testOccurrencesOf_objectArray() {
        assertEquals(0, N.frequency((Object[]) null, "a"));
        assertEquals(0, N.frequency(EMPTY_OBJECT_ARRAY_CONST, "a"));
        assertEquals(2, N.frequency(new Object[] { "a", "b", "a" }, "a"));
        assertEquals(1, N.frequency(new Object[] { "a", "b", "c" }, "b"));
        assertEquals(0, N.frequency(new Object[] { "b", "c" }, "a"));
        assertEquals(2, N.frequency(new Object[] { "a", null, "a", null }, null));
        assertEquals(1, N.frequency(new Object[] { null }, null));
        assertEquals(0, N.frequency(new Object[] { "a" }, null));
    }

    @Test
    public void testOccurrencesOf_iterable() {
        assertEquals(0, N.frequency((Iterable<?>) null, "a"));
        assertEquals(0, N.frequency(Collections.emptyList(), "a"));
        assertEquals(2, N.frequency(Arrays.asList("a", "b", "a"), "a"));
        assertEquals(1, N.frequency(Arrays.asList("a", "b", "c"), "b"));
        assertEquals(0, N.frequency(Arrays.asList("b", "c"), "a"));
        assertEquals(2, N.frequency(Arrays.asList("a", null, "a", null), null));
        assertEquals(1, N.frequency(Collections.singletonList(null), null));
    }

    @Test
    public void testOccurrencesOf_iterator() {
        assertEquals(0, N.frequency((Iterator<?>) null, "a"));
        assertEquals(0, N.frequency(Collections.emptyIterator(), "a"));
        assertEquals(2, N.frequency(Arrays.asList("a", "b", "a").iterator(), "a"));
        assertEquals(1, N.frequency(Arrays.asList("a", "b", "c").iterator(), "b"));
        assertEquals(0, N.frequency(Arrays.asList("b", "c").iterator(), "a"));
        assertEquals(2, N.frequency(Arrays.asList("a", null, "a", null).iterator(), null));

    }

    @Test
    public void testOccurrencesOf_stringChar() {
        assertEquals(0, N.frequency((String) null, 'a'));
        assertEquals(0, N.frequency("", 'a'));
        assertEquals(2, N.frequency("aba", 'a'));
        assertEquals(1, N.frequency("abc", 'b'));
        assertEquals(0, N.frequency("bc", 'a'));
    }

    @Test
    public void testOccurrencesOf_stringString() {
        assertEquals(0, N.frequency((String) null, "a"));
        assertEquals(0, N.frequency("", "a"));
        assertEquals(2, N.frequency("ababa", "ab"));
        assertEquals(1, N.frequency("abcab", "bca"));
        assertEquals(0, N.frequency("def", "abc"));
        assertEquals(0, N.frequency("abc", ""));
        assertEquals(0, N.frequency("abc", ""));
        assertEquals(0, N.frequency("", ""));
    }

    @Test
    public void testOccurrencesMap_array() {
        Map<String, Integer> expected = new HashMap<>();
        assertMapEquals(expected, N.frequencyMap((String[]) null));
        assertMapEquals(expected, N.frequencyMap(new String[] {}));

        expected.put("a", 2);
        expected.put("b", 1);
        assertMapEquals(expected, N.frequencyMap(new String[] { "a", "b", "a" }));

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        assertMapEquals(expected, N.frequencyMap(new String[] { null, "a", null }));
    }

    @Test
    public void testOccurrencesMap_arrayWithSupplier() {
        Supplier<Map<String, Integer>> supplier = LinkedHashMap::new;

        Map<String, Integer> expected = new LinkedHashMap<>();
        assertMapEquals(expected, N.frequencyMap((String[]) null, supplier));
        assertMapEquals(expected, N.frequencyMap(new String[] {}, supplier));

        expected.put("a", 2);
        expected.put("b", 1);
        Map<String, Integer> actual = N.frequencyMap(new String[] { "a", "b", "a" }, supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof LinkedHashMap);

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        actual = N.frequencyMap(new String[] { null, "a", null }, supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof LinkedHashMap);
    }

    @Test
    public void testOccurrencesMap_iterable() {
        Map<String, Integer> expected = new HashMap<>();
        assertMapEquals(expected, N.frequencyMap((Iterable<String>) null));
        assertMapEquals(expected, N.frequencyMap(Collections.<String> emptyList()));

        expected.put("a", 2);
        expected.put("b", 1);
        assertMapEquals(expected, N.frequencyMap(Arrays.asList("a", "b", "a")));

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        assertMapEquals(expected, N.frequencyMap(Arrays.asList(null, "a", null)));
    }

    @Test
    public void testOccurrencesMap_iterableWithSupplier() {
        Supplier<Map<String, Integer>> supplier = TreeMap::new;

        Map<String, Integer> expected = new TreeMap<>();
        assertMapEquals(expected, N.frequencyMap((Iterable<String>) null, supplier));
        assertMapEquals(expected, N.frequencyMap(Collections.<String> emptyList(), supplier));

        expected.put("a", 2);
        expected.put("b", 1);
        Map<String, Integer> actual = N.frequencyMap(Arrays.asList("a", "b", "a"), supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof TreeMap);

        expected.clear();
        Supplier<Map<String, Integer>> hashMapSupplier = HashMap::new;
        Map<String, Integer> expectedNull = new HashMap<>();
        expectedNull.put(null, 2);
        expectedNull.put("a", 1);
        Map<String, Integer> actualNull = N.frequencyMap(Arrays.asList(null, "a", null), hashMapSupplier);
        assertMapEquals(expectedNull, actualNull);
        assertTrue(actualNull instanceof HashMap);
    }

    @Test
    public void testOccurrencesMap_iterator() {
        Map<String, Integer> expected = new HashMap<>();
        assertMapEquals(expected, N.frequencyMap((Iterator<String>) null));
        assertMapEquals(expected, N.frequencyMap(Collections.<String> emptyIterator()));

        expected.put("a", 2);
        expected.put("b", 1);
        assertMapEquals(expected, N.frequencyMap(Arrays.asList("a", "b", "a").iterator()));

        expected.clear();
        expected.put(null, 2);
        expected.put("a", 1);
        assertMapEquals(expected, N.frequencyMap(Arrays.asList(null, "a", null).iterator()));
    }

    @Test
    public void testOccurrencesMap_iteratorWithSupplier() {
        Supplier<Map<String, Integer>> supplier = ConcurrentHashMap::new;

        Map<String, Integer> expected = new ConcurrentHashMap<>();
        assertMapEquals(expected, N.frequencyMap((Iterator<String>) null, supplier));
        assertMapEquals(expected, N.frequencyMap(Collections.<String> emptyIterator(), supplier));

        expected.put("a", 2);
        expected.put("b", 1);
        Map<String, Integer> actual = N.frequencyMap(Arrays.asList("a", "b", "a").iterator(), supplier);
        assertMapEquals(expected, actual);
        assertTrue(actual instanceof ConcurrentHashMap);
    }

    @Test
    public void testContains_booleanArray() {
        assertFalse(N.contains((boolean[]) null, true));
        assertFalse(N.contains(EMPTY_BOOLEAN_ARRAY_CONST, true));
        assertTrue(N.contains(new boolean[] { true, false }, true));
        assertFalse(N.contains(new boolean[] { false, false }, true));
    }

    @Test
    public void testContains_charArray() {
        assertFalse(N.contains((char[]) null, 'a'));
        assertFalse(N.contains(EMPTY_CHAR_ARRAY_CONST, 'a'));
        assertTrue(N.contains(new char[] { 'a', 'b' }, 'a'));
        assertFalse(N.contains(new char[] { 'b', 'c' }, 'a'));
    }

    @Test
    public void testContains_byteArray() {
        assertFalse(N.contains((byte[]) null, (byte) 1));
        assertFalse(N.contains(EMPTY_BYTE_ARRAY_CONST, (byte) 1));
        assertTrue(N.contains(new byte[] { 1, 2 }, (byte) 1));
        assertFalse(N.contains(new byte[] { 2, 3 }, (byte) 1));
    }

    @Test
    public void testContains_shortArray() {
        assertFalse(N.contains((short[]) null, (short) 1));
        assertFalse(N.contains(EMPTY_SHORT_ARRAY_CONST, (short) 1));
        assertTrue(N.contains(new short[] { 1, 2 }, (short) 1));
        assertFalse(N.contains(new short[] { 2, 3 }, (short) 1));
    }

    @Test
    public void testContains_intArray() {
        assertFalse(N.contains((int[]) null, 1));
        assertFalse(N.contains(EMPTY_INT_ARRAY_CONST, 1));
        assertTrue(N.contains(new int[] { 1, 2 }, 1));
        assertFalse(N.contains(new int[] { 2, 3 }, 1));
    }

    @Test
    public void testContains_longArray() {
        assertFalse(N.contains((long[]) null, 1L));
        assertFalse(N.contains(EMPTY_LONG_ARRAY_CONST, 1L));
        assertTrue(N.contains(new long[] { 1L, 2L }, 1L));
        assertFalse(N.contains(new long[] { 2L, 3L }, 1L));
    }

    @Test
    public void testContains_floatArray() {
        assertFalse(N.contains((float[]) null, 1.0f));
        assertFalse(N.contains(EMPTY_FLOAT_ARRAY_CONST, 1.0f));
        assertTrue(N.contains(new float[] { 1.0f, 2.0f }, 1.0f));
        assertFalse(N.contains(new float[] { 2.0f, 3.0f }, 1.0f));
        assertTrue(N.contains(new float[] { Float.NaN, 1.0f }, Float.NaN));
    }

    @Test
    public void testContains_doubleArray() {
        assertFalse(N.contains((double[]) null, 1.0));
        assertFalse(N.contains(EMPTY_DOUBLE_ARRAY_CONST, 1.0));
        assertTrue(N.contains(new double[] { 1.0, 2.0 }, 1.0));
        assertFalse(N.contains(new double[] { 2.0, 3.0 }, 1.0));
        assertTrue(N.contains(new double[] { Double.NaN, 1.0 }, Double.NaN));
    }

    @Test
    public void testContains_objectArray() {
        assertFalse(N.contains((Object[]) null, "a"));
        assertFalse(N.contains(EMPTY_OBJECT_ARRAY_CONST, "a"));
        assertTrue(N.contains(new Object[] { "a", "b" }, "a"));
        assertFalse(N.contains(new Object[] { "b", "c" }, "a"));
        assertTrue(N.contains(new Object[] { "a", null }, null));
        assertFalse(N.contains(new Object[] { "a" }, null));
    }

    @Test
    public void testContains_collection() {
        assertFalse(N.contains((Collection<?>) null, "a"));
        assertFalse(N.contains(Collections.emptyList(), "a"));
        assertTrue(N.contains(Arrays.asList("a", "b"), "a"));
        assertFalse(N.contains(Arrays.asList("b", "c"), "a"));
        assertTrue(N.contains(Arrays.asList("a", null), null));
        assertFalse(N.contains(Collections.singletonList("a"), null));
    }

    @Test
    public void testContains_iterable() {
        Iterable<String> nullIterable = null;
        assertFalse(N.contains(nullIterable, "a"));
        assertFalse(N.contains(Collections.<String> emptyIterator(), "a"));
        assertTrue(N.contains(Arrays.asList("a", "b").iterator(), "a"));
        assertFalse(N.contains(Arrays.asList("b", "c").iterator(), "a"));
        assertTrue(N.contains(Arrays.asList("a", null).iterator(), null));
        assertFalse(N.contains(Collections.singleton("a").iterator(), null));
    }

    @Test
    public void testContains_iterator() {
        assertFalse(N.contains((Iterator<?>) null, "a"));
        assertFalse(N.contains(Collections.emptyIterator(), "a"));
        assertTrue(N.contains(Arrays.asList("a", "b").iterator(), "a"));
        assertFalse(N.contains(Arrays.asList("b", "c").iterator(), "a"));
        assertTrue(N.contains(Arrays.asList("a", null).iterator(), null));
        assertFalse(N.contains(Collections.singleton("a").iterator(), null));
    }

    @Test
    public void testContainsAll_collectionCollection() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAll(main, Arrays.asList("a", "b")));
        assertTrue(N.containsAll(main, Collections.emptyList()));
        assertTrue(N.containsAll(main, (Collection<?>) null));
        assertFalse(N.containsAll(main, Arrays.asList("a", "d")));
        assertFalse(N.containsAll(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(N.containsAll((Iterator<String>) null, Arrays.asList("a")));
        assertTrue(N.containsAll(Arrays.asList("a", "a", "b"), Arrays.asList("a", "a")));
        assertTrue(N.containsAll(Arrays.asList("a", "b"), Arrays.asList("a", "a")));
    }

    @Test
    public void testContainsAll_collectionVarargs() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAll(main, "a", "b"));
        assertTrue(N.containsAll(main));
        assertTrue(N.containsAll(main, (Object[]) null));
        assertFalse(N.containsAll(main, "a", "d"));
        assertFalse(N.containsAll(Collections.emptyList(), "a"));
        assertFalse(N.containsAll(null, "a"));
        assertTrue(N.containsAll(Arrays.asList("a", "a", "b"), "a", "a"));
        assertTrue(N.containsAll(Arrays.asList("a", "b"), "a", "a"));
    }

    @Test
    public void testContainsAll_iterableCollection() {
        Iterable<String> mainIter = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAll(mainIter, Arrays.asList("a", "b")));
        assertTrue(N.containsAll(mainIter, Collections.emptyList()));
        assertTrue(N.containsAll(mainIter, (Collection<?>) null));
        assertFalse(N.containsAll(mainIter, Arrays.asList("a", "d")));

        Iterable<String> emptyIter = Collections::emptyIterator;
        assertFalse(N.containsAll(emptyIter, Arrays.asList("a")));

        Iterable<String> nullIter = null;
        assertFalse(N.containsAll(nullIter, Arrays.asList("a")));

        Iterable<String> mainWithDupes = Arrays.asList("a", "a", "b", "c");
        assertTrue(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a", "b"))));
        assertTrue(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a"))));
        assertTrue(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a", "c", "b"))));
        assertFalse(N.containsAll(mainWithDupes, new HashSet<>(Arrays.asList("a", "d"))));
    }

    @Test
    public void testContainsAll_iteratorCollection() {
        assertTrue(N.containsAll(Arrays.asList("a", "b", "c").iterator(), Arrays.asList("a", "b")));
        assertTrue(N.containsAll(Arrays.asList("a", "b", "c").iterator(), Collections.emptyList()));
        assertTrue(N.containsAll(Arrays.asList("a", "b", "c").iterator(), (Collection<?>) null));

        assertFalse(N.containsAll(Arrays.asList("a", "b", "c").iterator(), Arrays.asList("a", "d")));
        assertFalse(N.containsAll(Collections.emptyIterator(), Arrays.asList("a")));
        assertFalse(N.containsAll((Iterator<String>) null, Arrays.asList("a")));

        Iterator<String> mainWithDupesIter = Arrays.asList("a", "a", "b", "c").iterator();
        assertTrue(N.containsAll(mainWithDupesIter, new HashSet<>(Arrays.asList("a", "b"))));

        mainWithDupesIter = Arrays.asList("a", "a", "b", "c").iterator();
        assertTrue(N.containsAll(mainWithDupesIter, new HashSet<>(Arrays.asList("a", "c", "b"))));

        mainWithDupesIter = Arrays.asList("a", "a", "b", "c").iterator();
        assertFalse(N.containsAll(mainWithDupesIter, new HashSet<>(Arrays.asList("a", "d"))));
    }

    @Test
    public void testContainsAny_collectionCollection() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(main, Arrays.asList("c", "d")));
        assertFalse(N.containsAny(main, Arrays.asList("d", "e")));
        assertFalse(N.containsAny(main, Collections.emptyList()));
        assertFalse(N.containsAny(main, (Collection<?>) null));
        assertFalse(N.containsAny(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(N.containsAny(null, Arrays.asList("a")));
    }

    @Test
    public void testContainsAny_collectionVarargs() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(main, "c", "d"));
        assertFalse(N.containsAny(main, "d", "e"));
        assertFalse(N.containsAny(main));
        assertFalse(N.containsAny(main, (Object[]) null));
        assertFalse(N.containsAny(Collections.emptyList(), "a"));
        assertFalse(N.containsAny(null, "a"));
    }

    @Test
    public void testContainsAny_iterableSet() {
        Iterable<String> mainIter = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(mainIter, new HashSet<>(Arrays.asList("c", "d"))));
        assertFalse(N.containsAny(mainIter, new HashSet<>(Arrays.asList("d", "e"))));
        assertFalse(N.containsAny(mainIter, Collections.emptySet()));
        assertFalse(N.containsAny(mainIter, (Set<?>) null));

        Iterable<String> emptyIter = Collections::emptyIterator;
        assertFalse(N.containsAny(emptyIter, new HashSet<>(Arrays.asList("a"))));

        Iterable<String> nullIter = null;
        assertFalse(N.containsAny(nullIter, new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testContainsAny_iteratorSet() {
        assertTrue(N.containsAny(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("c", "d"))));
        assertFalse(N.containsAny(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("d", "e"))));
        assertFalse(N.containsAny(Arrays.asList("a", "b", "c").iterator(), Collections.emptySet()));
        assertFalse(N.containsAny(Arrays.asList("a", "b", "c").iterator(), (Set<?>) null));
        assertFalse(N.containsAny(Collections.emptyIterator(), new HashSet<>(Arrays.asList("a"))));
        assertFalse(N.containsAny((Iterator<String>) null, new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testContainsNone_collectionCollection() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertFalse(N.containsNone(main, Arrays.asList("c", "d")));
        assertTrue(N.containsNone(main, Arrays.asList("d", "e")));
        assertTrue(N.containsNone(main, Collections.emptyList()));
        assertTrue(N.containsNone(main, (Collection<?>) null));
        assertTrue(N.containsNone(Collections.emptyList(), Arrays.asList("a")));
        assertTrue(N.containsNone(null, Arrays.asList("a")));
    }

    @Test
    public void testContainsNone_collectionVarargs() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertFalse(N.containsNone(main, "c", "d"));
        assertTrue(N.containsNone(main, "d", "e"));
        assertTrue(N.containsNone(main));
        assertTrue(N.containsNone(main, (Object[]) null));
        assertTrue(N.containsNone(Collections.emptyList(), "a"));
        assertTrue(N.containsNone(null, "a"));
    }

    @Test
    public void testContainsNone_iterableSet() {
        Iterable<String> mainIter = Arrays.asList("a", "b", "c");
        assertFalse(N.containsNone(mainIter, new HashSet<>(Arrays.asList("c", "d"))));
        assertTrue(N.containsNone(mainIter, new HashSet<>(Arrays.asList("d", "e"))));
        assertTrue(N.containsNone(mainIter, Collections.emptySet()));
        assertTrue(N.containsNone(mainIter, (Set<?>) null));

        Iterable<String> emptyIter = Collections::emptyIterator;
        assertTrue(N.containsNone(emptyIter, new HashSet<>(Arrays.asList("a"))));

        Iterable<String> nullIter = null;
        assertTrue(N.containsNone(nullIter, new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testContainsNone_iteratorSet() {
        assertFalse(N.containsNone(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("c", "d"))));
        assertTrue(N.containsNone(Arrays.asList("a", "b", "c").iterator(), new HashSet<>(Arrays.asList("d", "e"))));
        assertTrue(N.containsNone(Arrays.asList("a", "b", "c").iterator(), Collections.emptySet()));
        assertTrue(N.containsNone(Arrays.asList("a", "b", "c").iterator(), (Set<?>) null));
        assertTrue(N.containsNone(Collections.emptyIterator(), new HashSet<>(Arrays.asList("a"))));
        assertTrue(N.containsNone((Iterator<String>) null, new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testSlice_array() {
        String[] arr = { "a", "b", "c", "d" };
        assertIterableEquals(Arrays.asList("b", "c"), CommonUtil.slice(arr, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), CommonUtil.slice(arr, 0, 4));
        assertTrue(CommonUtil.slice(arr, 1, 1).isEmpty());
        assertTrue(CommonUtil.slice(new String[] {}, 0, 0).isEmpty());
        assertTrue(CommonUtil.slice((String[]) null, 0, 0).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(arr, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(arr, 3, 1));
    }

    @Test
    public void testSlice_list() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertIterableEquals(Arrays.asList("b", "c"), CommonUtil.slice(list, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), CommonUtil.slice(list, 0, 4));
        assertTrue(CommonUtil.slice(list, 1, 1).isEmpty());
        assertTrue(CommonUtil.slice(Collections.<String> emptyList(), 0, 0).isEmpty());
        assertTrue(CommonUtil.slice((List<String>) null, 0, 0).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(list, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(list, 3, 1));
    }

    @Test
    public void testSlice_collection() {
        Collection<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        assertIterableEquals(Arrays.asList("b", "c"), CommonUtil.slice(coll, 1, 3));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), CommonUtil.slice(coll, 0, 4));
        assertTrue(CommonUtil.slice(coll, 1, 1).isEmpty());
        assertTrue(CommonUtil.slice(Collections.<String> emptySet(), 0, 0).isEmpty());
        assertTrue(CommonUtil.slice((Collection<String>) null, 0, 0).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(coll, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(coll, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.slice(coll, 3, 1));

        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertTrue(CommonUtil.slice((Collection<String>) list, 1, 3) instanceof ImmutableList);
        assertIterableEquals(Arrays.asList("b", "c"), CommonUtil.slice((Collection<String>) list, 1, 3));
    }

    @Test
    public void testSlice_iterator() {
        assertIterableEquals(Arrays.asList("b", "c"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b", "c", "d").iterator(), 1, 3)));
        assertIterableEquals(Arrays.asList("a", "b", "c", "d"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b", "c", "d").iterator(), 0, 4)));
        assertTrue(iteratorToList(CommonUtil.slice(Arrays.asList("a", "b", "c", "d").iterator(), 1, 1)).isEmpty());
        assertTrue(iteratorToList(CommonUtil.slice(Collections.<String> emptyIterator(), 0, 0)).isEmpty());
        assertTrue(iteratorToList(CommonUtil.slice((Iterator<String>) null, 0, 0)).isEmpty());

        assertIterableEquals(Collections.emptyList(), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b").iterator(), 0, 0)));
        assertIterableEquals(Arrays.asList("a"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b").iterator(), 0, 1)));
        assertIterableEquals(Arrays.asList("b"), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b").iterator(), 1, 2)));
        assertIterableEquals(Collections.emptyList(), iteratorToList(CommonUtil.slice(Arrays.asList("a", "b").iterator(), 2, 2)));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.slice(Arrays.asList("a").iterator(), -1, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.slice(Arrays.asList("a").iterator(), 1, 0));
    }

    @Test
    public void testSplit_booleanArray_chunkSize() {
        boolean[] arr = { true, false, true, false, true };
        List<boolean[]> expected = Arrays.asList(new boolean[] { true, false }, new boolean[] { true, false }, new boolean[] { true });
        assertListOfBooleanArraysEquals(expected, N.split(arr, 2));

        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split((boolean[]) null, 2));
        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split(EMPTY_BOOLEAN_ARRAY_CONST, 2));

        List<boolean[]> singleChunks = Arrays.asList(new boolean[] { true }, new boolean[] { false }, new boolean[] { true }, new boolean[] { false },
                new boolean[] { true });
        assertListOfBooleanArraysEquals(singleChunks, N.split(arr, 1));

        List<boolean[]> largeChunk = Collections.singletonList(arr.clone());
        assertListOfBooleanArraysEquals(largeChunk, N.split(arr, 5));
        assertListOfBooleanArraysEquals(largeChunk, N.split(arr, 10));

        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 0));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, -1));
    }

    @Test
    public void testSplit_booleanArray_fromIndex_toIndex_chunkSize() {
        boolean[] arr = { true, false, true, false, true, false };
        List<boolean[]> expected = Arrays.asList(new boolean[] { false, true }, new boolean[] { false, true });
        assertListOfBooleanArraysEquals(expected, N.split(arr, 1, 5, 2));

        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split(arr, 1, 1, 2));
        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split((boolean[]) null, 0, 0, 1));
        assertListOfBooleanArraysEquals(Collections.emptyList(), N.split(EMPTY_BOOLEAN_ARRAY_CONST, 0, 0, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.split(arr, -1, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.split(arr, 0, 7, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.split(arr, 4, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_charArray_chunkSize() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        List<char[]> expected = Arrays.asList(new char[] { 'a', 'b' }, new char[] { 'c', 'd' }, new char[] { 'e' });
        assertListOfCharArraysEquals(expected, N.split(arr, 2));
        assertListOfCharArraysEquals(Collections.emptyList(), N.split((char[]) null, 2));
    }

    @Test
    public void testSplit_charArray_fromIndex_toIndex_chunkSize() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e', 'f' };
        List<char[]> expected = Arrays.asList(new char[] { 'b', 'c' }, new char[] { 'd', 'e' });
        assertListOfCharArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_byteArray_chunkSize() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        List<byte[]> expected = Arrays.asList(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5 });
        assertListOfByteArraysEquals(expected, N.split(arr, 2));
        assertListOfByteArraysEquals(Collections.emptyList(), N.split((byte[]) null, 2));
    }

    @Test
    public void testSplit_byteArray_fromIndex_toIndex_chunkSize() {
        byte[] arr = { 1, 2, 3, 4, 5, 6 };
        List<byte[]> expected = Arrays.asList(new byte[] { 2, 3 }, new byte[] { 4, 5 });
        assertListOfByteArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_shortArray_chunkSize() {
        short[] arr = { 1, 2, 3, 4, 5 };
        List<short[]> expected = Arrays.asList(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 });
        assertListOfShortArraysEquals(expected, N.split(arr, 2));
        assertListOfShortArraysEquals(Collections.emptyList(), N.split((short[]) null, 2));
    }

    @Test
    public void testSplit_shortArray_fromIndex_toIndex_chunkSize() {
        short[] arr = { 1, 2, 3, 4, 5, 6 };
        List<short[]> expected = Arrays.asList(new short[] { 2, 3 }, new short[] { 4, 5 });
        assertListOfShortArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_intArray_chunkSize() {
        int[] arr = { 1, 2, 3, 4, 5 };
        List<int[]> expected = Arrays.asList(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5 });
        assertListOfIntArraysEquals(expected, N.split(arr, 2));
        assertListOfIntArraysEquals(Collections.emptyList(), N.split((int[]) null, 2));
    }

    @Test
    public void testSplit_intArray_fromIndex_toIndex_chunkSize() {
        int[] arr = { 1, 2, 3, 4, 5, 6 };
        List<int[]> expected = Arrays.asList(new int[] { 2, 3 }, new int[] { 4, 5 });
        assertListOfIntArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_longArray_chunkSize() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        List<long[]> expected = Arrays.asList(new long[] { 1L, 2L }, new long[] { 3L, 4L }, new long[] { 5L });
        assertListOfLongArraysEquals(expected, N.split(arr, 2));
        assertListOfLongArraysEquals(Collections.emptyList(), N.split((long[]) null, 2));
    }

    @Test
    public void testSplit_longArray_fromIndex_toIndex_chunkSize() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L, 6L };
        List<long[]> expected = Arrays.asList(new long[] { 2L, 3L }, new long[] { 4L, 5L });
        assertListOfLongArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_floatArray_chunkSize() {
        float[] arr = { 1f, 2f, 3f, 4f, 5f };
        List<float[]> expected = Arrays.asList(new float[] { 1f, 2f }, new float[] { 3f, 4f }, new float[] { 5f });
        assertListOfFloatArraysEquals(expected, N.split(arr, 2));
        assertListOfFloatArraysEquals(Collections.emptyList(), N.split((float[]) null, 2));
    }

    @Test
    public void testSplit_floatArray_fromIndex_toIndex_chunkSize() {
        float[] arr = { 1f, 2f, 3f, 4f, 5f, 6f };
        List<float[]> expected = Arrays.asList(new float[] { 2f, 3f }, new float[] { 4f, 5f });
        assertListOfFloatArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_doubleArray_chunkSize() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<double[]> expected = Arrays.asList(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0 }, new double[] { 5.0 });
        assertListOfDoubleArraysEquals(expected, N.split(arr, 2));
        assertListOfDoubleArraysEquals(Collections.emptyList(), N.split((double[]) null, 2));
    }

    @Test
    public void testSplit_doubleArray_fromIndex_toIndex_chunkSize() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 };
        List<double[]> expected = Arrays.asList(new double[] { 2.0, 3.0 }, new double[] { 4.0, 5.0 });
        assertListOfDoubleArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_objectArray_chunkSize() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String[]> expected = Arrays.asList(new String[] { "a", "b" }, new String[] { "c", "d" }, new String[] { "e" });
        assertListOfArraysEquals(expected, N.split(arr, 2));
        assertListOfArraysEquals(Collections.emptyList(), N.split((String[]) null, 2));
    }

    @Test
    public void testSplit_objectArray_fromIndex_toIndex_chunkSize() {
        String[] arr = { "a", "b", "c", "d", "e", "f" };
        List<String[]> expected = Arrays.asList(new String[] { "b", "c" }, new String[] { "d", "e" });
        assertListOfArraysEquals(expected, N.split(arr, 1, 5, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(arr, 1, 5, 0));
    }

    @Test
    public void testSplit_collection_chunkSize() {
        Collection<String> coll = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> expected = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Collections.singletonList("e"));
        List<List<String>> actual = N.split(coll, 2);
        assertListOfListsEquals(expected, actual);

        assertTrue(N.split((Collection<String>) null, 2).isEmpty());
        assertTrue(N.split(Collections.<String> emptyList(), 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(coll, 0));
    }

    @Test
    public void testSplit_collection_fromIndex_toIndex_chunkSize() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<List<String>> expected = Arrays.asList(list.subList(1, 3), list.subList(3, 5));
        List<List<String>> actual = N.split(list, 1, 5, 2);
        assertListOfListsEquals(expected, actual);

        Collection<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
        List<List<String>> expectedNonList = Arrays.asList(Arrays.asList("b", "c"), Arrays.asList("d", "e"));
        List<List<String>> actualNonList = N.split(coll, 1, 5, 2);
        assertListOfListsEquals(expectedNonList, actualNonList);

        assertTrue(N.split(list, 1, 1, 2).isEmpty());
        assertTrue(N.split((Collection<String>) null, 0, 0, 1).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> N.split(list, -1, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(list, 1, 5, 0));
    }

    @Test
    public void testSplit_iterable_chunkSize() {
        Iterable<String> iter = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> expected = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Collections.singletonList("e"));
        assertListOfListsEquals(expected, N.split(iter, 2));

        assertTrue(N.split((Iterable<String>) null, 2).isEmpty());
        assertTrue(N.split(Collections::emptyIterator, 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(iter, 0));
    }

    @Test
    public void testSplit_iterator_chunkSize() {
        ObjIterator<List<String>> objIter = N.split(Arrays.asList("a", "b", "c", "d", "e").iterator(), 2);
        List<List<String>> actual = iteratorToList(objIter);
        List<List<String>> expected = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"), Collections.singletonList("e"));
        assertListOfListsEquals(expected, actual);

        assertTrue(iteratorToList(N.split((Iterator<String>) null, 2)).isEmpty());
        assertTrue(iteratorToList(N.split(Collections.<String> emptyIterator(), 2)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(Arrays.asList("a").iterator(), 0));
    }

    @Test
    public void testSplitIterator_NextAfterExhaustion() {
        ObjIterator<List<String>> chunks = N.split(Arrays.asList("a").iterator(), 1);

        assertTrue(chunks.hasNext());
        assertEquals(Collections.singletonList("a"), chunks.next());
        assertFalse(chunks.hasNext());
        org.junit.jupiter.api.Assertions.assertThrows(NoSuchElementException.class, chunks::next);
    }

    @Test
    public void testSplit_charSequence_chunkSize() {
        CharSequence str = "abcde";
        List<String> expected = Arrays.asList("ab", "cd", "e");
        assertEquals(expected, N.split(str, 2));

        assertTrue(N.split((CharSequence) null, 2).isEmpty());
        assertTrue(N.split("", 2).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(str, 0));
    }

    @Test
    public void testSplit_charSequence_fromIndex_toIndex_chunkSize() {
        CharSequence str = "abcdef";
        List<String> expected = Arrays.asList("bc", "de");
        assertEquals(expected, N.split(str, 1, 5, 2));

        assertTrue(N.split(str, 1, 1, 2).isEmpty());
        assertTrue(N.split((CharSequence) null, 0, 0, 1).isEmpty());
        assertTrue(N.split("", 0, 0, 1).isEmpty());

        assertThrows(IndexOutOfBoundsException.class, () -> N.split(str, -1, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> N.split(str, 1, 5, 0));
    }

    @Test
    public void testSplitByChunkCount_totalSizeFunc() {
        IntBiFunction<int[]> copyRangeFunc = (from, to) -> Arrays.copyOfRange(new int[] { 1, 2, 3, 4, 5, 6, 7 }, from, to);

        List<int[]> result1 = N.splitByChunkCount(7, 5, copyRangeFunc);
        List<int[]> expected1 = Arrays.asList(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5 }, new int[] { 6 }, new int[] { 7 });
        assertListOfIntArraysEquals(expected1, result1);

        List<int[]> result2 = N.splitByChunkCount(7, 5, true, copyRangeFunc);
        List<int[]> expected2 = Arrays.asList(new int[] { 1 }, new int[] { 2 }, new int[] { 3 }, new int[] { 4, 5 }, new int[] { 6, 7 });
        assertListOfIntArraysEquals(expected2, result2);

        assertEquals(0, N.splitByChunkCount(0, 5, copyRangeFunc).size());

        List<int[]> result3 = N.splitByChunkCount(3, 5, copyRangeFunc);
        List<int[]> expected3 = Arrays.asList(new int[] { 1 }, new int[] { 2 }, new int[] { 3 });
        assertListOfIntArraysEquals(expected3, result3);

        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(-1, 5, copyRangeFunc));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(5, 0, copyRangeFunc));
    }

    @Test
    public void testSplitByChunkCount_collection() {
        List<Integer> coll = Arrays.asList(1, 2, 3, 4, 5, 6, 7);

        List<List<Integer>> result1 = N.splitByChunkCount(coll, 5);
        List<List<Integer>> expected1 = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5), Arrays.asList(6), Arrays.asList(7));
        assertListOfListsEquals(expected1, result1);

        List<List<Integer>> result2 = N.splitByChunkCount(coll, 5, true);
        List<List<Integer>> expected2 = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3), Arrays.asList(4, 5), Arrays.asList(6, 7));
        assertListOfListsEquals(expected2, result2);

        assertTrue(N.splitByChunkCount((Collection<Integer>) null, 5).isEmpty());
        assertTrue(N.splitByChunkCount(Collections.<Integer> emptyList(), 5).isEmpty());

        List<Integer> smallColl = Arrays.asList(1, 2, 3);
        List<List<Integer>> result3 = N.splitByChunkCount(smallColl, 5);
        List<List<Integer>> expected3 = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3));
        assertListOfListsEquals(expected3, result3);

        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(coll, 0));

        Collection<Integer> setColl = new LinkedHashSet<>(coll);
        List<List<Integer>> resultSet = N.splitByChunkCount(setColl, 5);
        assertListOfListsEquals(expected1, resultSet);
    }

    @Test
    public void testConcat_booleanArrays() {
        assertArrayEquals(new boolean[] { true, false, true, true }, N.concat(new boolean[] { true, false }, new boolean[] { true, true }));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }, null));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }, EMPTY_BOOLEAN_ARRAY_CONST));
        assertArrayEquals(new boolean[] { true, true }, N.concat(null, new boolean[] { true, true }));
        assertArrayEquals(new boolean[] { true, true }, N.concat(EMPTY_BOOLEAN_ARRAY_CONST, new boolean[] { true, true }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat((boolean[]) null, (boolean[]) null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat(EMPTY_BOOLEAN_ARRAY_CONST, EMPTY_BOOLEAN_ARRAY_CONST));
    }

    @Test
    public void testConcat_booleanArraysVarargs() {
        assertArrayEquals(new boolean[] { true, false, true, true, false },
                N.concat(new boolean[] { true, false }, new boolean[] { true, true }, new boolean[] { false }));
        assertArrayEquals(new boolean[] { true, false }, N.concat(new boolean[] { true, false }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat((boolean[][]) null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat(new boolean[0][0]));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.concat(new boolean[][] { null, null }));
        assertArrayEquals(new boolean[] { true }, N.concat(null, new boolean[] { true }, null, EMPTY_BOOLEAN_ARRAY_CONST));
    }

    @Test
    public void testConcat_charArrays() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, N.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd' }));
        assertArrayEquals(new char[] { 'a', 'b' }, N.concat(new char[] { 'a', 'b' }, null));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.concat((char[]) null, (char[]) null));
    }

    @Test
    public void testConcat_charArraysVarargs() {
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, N.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd' }, new char[] { 'e' }));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.concat((char[][]) null));
    }

    @Test
    public void testConcat_byteArrays() {
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, N.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 }));
        assertArrayEquals(new byte[] { 1, 2 }, N.concat(new byte[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_BYTE_ARRAY_CONST, N.concat((byte[]) null, (byte[]) null));
    }

    @Test
    public void testConcat_byteArraysVarargs() {
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, N.concat(new byte[] { 1, 2 }, new byte[] { 3, 4 }, new byte[] { 5 }));
        assertArrayEquals(EMPTY_BYTE_ARRAY_CONST, N.concat((byte[][]) null));
    }

    @Test
    public void testConcat_shortArrays() {
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, N.concat(new short[] { 1, 2 }, new short[] { 3, 4 }));
        assertArrayEquals(new short[] { 1, 2 }, N.concat(new short[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_SHORT_ARRAY_CONST, N.concat((short[]) null, (short[]) null));
    }

    @Test
    public void testConcat_shortArraysVarargs() {
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, N.concat(new short[] { 1, 2 }, new short[] { 3, 4 }, new short[] { 5 }));
        assertArrayEquals(EMPTY_SHORT_ARRAY_CONST, N.concat((short[][]) null));
    }

    @Test
    public void testConcat_intArrays() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.concat(new int[] { 1, 2 }, new int[] { 3, 4 }));
        assertArrayEquals(new int[] { 1, 2 }, N.concat(new int[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.concat((int[]) null, (int[]) null));
    }

    @Test
    public void testConcat_intArraysVarargs() {
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.concat(new int[] { 1, 2 }, new int[] { 3, 4 }, new int[] { 5 }));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.concat((int[][]) null));
    }

    @Test
    public void testConcat_longArrays() {
        assertArrayEquals(new long[] { 1, 2, 3, 4 }, N.concat(new long[] { 1, 2 }, new long[] { 3, 4 }));
        assertArrayEquals(new long[] { 1, 2 }, N.concat(new long[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_LONG_ARRAY_CONST, N.concat((long[]) null, (long[]) null));
    }

    @Test
    public void testConcat_longArraysVarargs() {
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, N.concat(new long[] { 1, 2 }, new long[] { 3, 4 }, new long[] { 5 }));
        assertArrayEquals(EMPTY_LONG_ARRAY_CONST, N.concat((long[][]) null));
    }

    @Test
    public void testConcat_floatArrays() {
        assertArrayEquals(new float[] { 1, 2, 3, 4 }, N.concat(new float[] { 1, 2 }, new float[] { 3, 4 }));
        assertArrayEquals(new float[] { 1, 2 }, N.concat(new float[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_FLOAT_ARRAY_CONST, N.concat((float[]) null, (float[]) null));
    }

    @Test
    public void testConcat_floatArraysVarargs() {
        assertArrayEquals(new float[] { 1, 2, 3, 4, 5 }, N.concat(new float[] { 1, 2 }, new float[] { 3, 4 }, new float[] { 5 }));
        assertArrayEquals(EMPTY_FLOAT_ARRAY_CONST, N.concat((float[][]) null));
    }

    @Test
    public void testConcat_doubleArrays() {
        assertArrayEquals(new double[] { 1, 2, 3, 4 }, N.concat(new double[] { 1, 2 }, new double[] { 3, 4 }));
        assertArrayEquals(new double[] { 1, 2 }, N.concat(new double[] { 1, 2 }, null));
        assertArrayEquals(EMPTY_DOUBLE_ARRAY_CONST, N.concat((double[]) null, (double[]) null));
    }

    @Test
    public void testConcat_doubleArraysVarargs() {
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5 }, N.concat(new double[] { 1, 2 }, new double[] { 3, 4 }, new double[] { 5 }));
        assertArrayEquals(EMPTY_DOUBLE_ARRAY_CONST, N.concat((double[][]) null));
    }

    @Test
    public void testConcat_objectArrays() {
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, N.concat(new String[] { "a", "b" }, new String[] { "c", "d" }));
        assertArrayEquals(new String[] { "a", "b" }, N.concat(new String[] { "a", "b" }, null));
        assertArrayEquals(new String[] { "c", "d" }, N.concat(null, new String[] { "c", "d" }));
        assertNull(N.concat((String[]) null, (String[]) null));
        assertArrayEquals(EMPTY_OBJECT_ARRAY_CONST, N.concat(new String[] {}, new String[] {}));
    }

    @Test
    public void testConcat_objectArraysVarargs() {
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.concat(new String[] { "a", "b" }, new String[] { "c", "d" }, new String[] { "e" }));
        assertNull(N.concat((String[][]) null));
        assertArrayEquals(new String[0], N.concat(new String[0][0]));
        assertArrayEquals(new String[] { "a" }, N.concat(new String[] { "a" }));
        assertArrayEquals(new String[] { "a" }, N.concat(null, new String[] { "a" }, null));
        assertArrayEquals(new String[0], N.concat(new String[][] { null, null }));
        String[][] emptyVarargs = new String[0][0];
        assertEquals(0, N.concat(emptyVarargs).length);
        assertTrue(N.concat(emptyVarargs) instanceof String[]);
    }

    @Test
    public void testConcat_iterables() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        List<String> expected = Arrays.asList("a", "b", "c", "d");
        assertEquals(expected, N.concat(iterA, iterB));

        assertEquals(Arrays.asList("a", "b"), N.concat(iterA, null));
        assertEquals(Arrays.asList("c", "d"), N.concat(null, iterB));
        assertEquals(Collections.emptyList(), N.concat((Iterable<String>) null, (Iterable<String>) null));
        assertEquals(Collections.emptyList(), N.concat(Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcat_iterablesVarargs() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        Iterable<String> iterC = Arrays.asList("e");
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, N.concat(iterA, iterB, iterC));

        assertEquals(Arrays.asList("a", "b"), N.concat(iterA));
        assertEquals(Collections.emptyList(), N.concat((Iterable<String>[]) null));
        assertEquals(Collections.emptyList(), N.concat(new Iterable[0]));
        assertEquals(Arrays.asList("a", "b"), N.concat(null, iterA, null, Collections.emptyList()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcat_collectionOfIterables() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        Collection<Iterable<String>> collOfIters = Arrays.asList(iterA, null, iterB, Collections.emptyList());
        List<String> expected = Arrays.asList("a", "b", "c", "d");
        assertEquals(expected, N.concat(collOfIters));

        assertTrue(N.concat((Collection<Iterable<String>>) null).isEmpty());
        assertTrue(N.concat(Collections.<Iterable<String>> emptyList()).isEmpty());
        assertEquals(Arrays.asList("a", "b"), N.concat(Collections.singletonList(iterA)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcat_collectionOfIterablesWithSupplier() {
        Iterable<String> iterA = Arrays.asList("a", "b");
        Iterable<String> iterB = Arrays.asList("c", "d");
        Collection<Iterable<String>> collOfIters = Arrays.asList(iterA, iterB);

        IntFunction<LinkedList<String>> supplier = IntFunctions.ofLinkedList();
        LinkedList<String> result = N.concat(collOfIters, supplier);

        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
        assertTrue(result instanceof LinkedList);

        assertTrue(N.concat((Collection<Iterable<String>>) null, supplier).isEmpty());
    }

    @Test
    public void testConcat_iterators() {
        Iterator<String> iterA = Arrays.asList("a", "b").iterator();
        Iterator<String> iterB = Arrays.asList("c", "d").iterator();
        List<String> expected = Arrays.asList("a", "b", "c", "d");
        assertEquals(expected, iteratorToList(N.concat(iterA, iterB)));

        assertEquals(Arrays.asList("a", "b"), iteratorToList(N.concat(Arrays.asList("a", "b").iterator(), null)));
        assertEquals(Arrays.asList("c", "d"), iteratorToList(N.concat(null, Arrays.asList("c", "d").iterator())));
        assertEquals(Collections.emptyList(), iteratorToList(N.concat((Iterator<String>) null, (Iterator<String>) null)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConcat_iteratorsVarargs() {
        Iterator<String> iterA = Arrays.asList("a", "b").iterator();
        Iterator<String> iterB = Arrays.asList("c", "d").iterator();
        Iterator<String> iterC = Arrays.asList("e").iterator();
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, iteratorToList(N.concat(iterA, iterB, iterC)));

        assertEquals(Arrays.asList("a", "b"), iteratorToList(N.concat(Arrays.asList("a", "b").iterator())));
        assertTrue(iteratorToList(N.concat((Iterator<String>[]) null)).isEmpty());
        assertTrue(iteratorToList(N.concat(new Iterator[0])).isEmpty());
    }

    @Test
    public void testFlatten_boolean2DArray() {
        boolean[][] arr = { { true, false }, { true }, {}, null, { false, false } };
        assertArrayEquals(new boolean[] { true, false, true, false, false }, N.flatten(arr));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten((boolean[][]) null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten(new boolean[][] {}));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten(new boolean[][] { null, null }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.flatten(new boolean[][] { {}, {} }));
    }

    @Test
    public void testFlatten_char2DArray() {
        char[][] arr = { { 'a', 'b' }, { 'c' }, {}, null, { 'd', 'e' } };
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, N.flatten(arr));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.flatten((char[][]) null));
    }

    @Test
    public void testFlatten_byte2DArray() {
        byte[][] arr = { { 1, 2 }, { 3 }, {}, null, { 4, 5 } };
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, N.flatten(arr));
        assertArrayEquals(EMPTY_BYTE_ARRAY_CONST, N.flatten((byte[][]) null));
    }

    @Test
    public void testFlatten_short2DArray() {
        short[][] arr = { { 1, 2 }, { 3 }, {}, null, { 4, 5 } };
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, N.flatten(arr));
        assertArrayEquals(EMPTY_SHORT_ARRAY_CONST, N.flatten((short[][]) null));
    }

    @Test
    public void testFlatten_int2DArray() {
        int[][] arr = { { 1, 2 }, { 3 }, {}, null, { 4, 5 } };
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.flatten(arr));
        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.flatten((int[][]) null));
    }

    @Test
    public void testFlatten_long2DArray() {
        long[][] arr = { { 1L, 2L }, { 3L }, {}, null, { 4L, 5L } };
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, N.flatten(arr));
        assertArrayEquals(EMPTY_LONG_ARRAY_CONST, N.flatten((long[][]) null));
    }

    @Test
    public void testFlatten_float2DArray() {
        float[][] arr = { { 1f, 2f }, { 3f }, {}, null, { 4f, 5f } };
        assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, N.flatten(arr));
        assertArrayEquals(EMPTY_FLOAT_ARRAY_CONST, N.flatten((float[][]) null));
    }

    @Test
    public void testFlatten_double2DArray() {
        double[][] arr = { { 1.0, 2.0 }, { 3.0 }, {}, null, { 4.0, 5.0 } };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, N.flatten(arr));
        assertArrayEquals(EMPTY_DOUBLE_ARRAY_CONST, N.flatten((double[][]) null));
    }

    @Test
    public void testFlatten_object2DArray() {
        String[][] arr = { { "a", "b" }, { "c" }, {}, null, { "d", "e" } };
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.flatten(arr));
        assertNull(N.flatten((String[][]) null));
        assertArrayEquals(new String[0], N.flatten(new String[][] {}));
    }

    @Test
    public void testFlatten_object2DArrayWithComponentType() {
        String[][] arr = { { "a", "b" }, { "c" }, {}, null, { "d", "e" } };
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, N.flatten(arr, String.class));
        assertArrayEquals(new String[0], N.flatten((String[][]) null, String.class));
        assertArrayEquals(new String[0], N.flatten(new String[][] {}, String.class));
    }

    @Test
    public void testFlatten_iterableOfIterables() {
        Iterable<Iterable<String>> iterOfIters = Arrays.asList(Arrays.asList("a", "b"), Collections.singletonList("c"), Collections.emptyList(), null,
                Arrays.asList("d", "e"));
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, N.flatten(iterOfIters));

        assertTrue(N.flatten((Iterable<Iterable<String>>) null).isEmpty());
        assertTrue(N.flatten(Collections.<Iterable<String>> emptyList()).isEmpty());
        assertTrue(N.flatten(Arrays.asList(null, null)).isEmpty());
    }

    @Test
    public void testFlatten_iterableOfIterablesWithSupplier() {
        Iterable<Iterable<String>> iterOfIters = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"));
        IntFunction<Set<String>> supplier = LinkedHashSet::new;

        Set<String> result = N.flatten(iterOfIters, supplier);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), result);
        assertTrue(result instanceof LinkedHashSet);

        assertTrue(N.flatten((Iterable<Iterable<String>>) null, supplier).isEmpty());
    }

    @Test
    public void testFlatten_iteratorOfIterators() {
        List<Iterator<String>> listOfIters = Arrays.asList(Arrays.asList("a", "b").iterator(), Collections.singletonList("c").iterator(),
                Collections.<String> emptyIterator(), null, Arrays.asList("d", "e").iterator());
        Iterator<Iterator<String>> iterOfIters = listOfIters.iterator();
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(expected, iteratorToList(N.flatten(iterOfIters)));

        assertTrue(iteratorToList(N.flatten((Iterator<Iterator<String>>) null)).isEmpty());
        assertTrue(iteratorToList(N.flatten(Collections.<Iterator<String>> emptyIterator())).isEmpty());
    }

    @Test
    public void testIntersection_booleanArrays() {
        assertArrayEquals(new boolean[] { true, false, true }, N.intersection(new boolean[] { true, false, true }, new boolean[] { true, true, false, false }));
        assertArrayEquals(new boolean[] { true, false }, N.intersection(new boolean[] { true, false, true }, new boolean[] { true, false }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(new boolean[] { true }, new boolean[] { false }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(null, new boolean[] { true }));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(new boolean[] { true }, null));
        assertArrayEquals(EMPTY_BOOLEAN_ARRAY_CONST, N.intersection(EMPTY_BOOLEAN_ARRAY_CONST, new boolean[] { true }));
    }

    @Test
    public void testIntersection_charArrays() {
        assertArrayEquals(new char[] { 'a', 'b', 'a' }, N.intersection(new char[] { 'a', 'b', 'a', 'c' }, new char[] { 'b', 'a', 'd', 'a' }));
        assertArrayEquals(EMPTY_CHAR_ARRAY_CONST, N.intersection(new char[] { 'a' }, null));
    }

    @Test
    public void testIntersection_intArrays() {

        int[] a1 = { 0, 1, 2, 2, 3 };
        int[] b1 = { 2, 5, 1 };
        int[] expected1 = { 1, 2 };
        int[] actual1 = N.intersection(a1, b1);
        Arrays.sort(actual1);
        assertArrayEquals(expected1, actual1);

        int[] a2 = { 1, 2, 2, 3, 3, 3 };
        int[] b2 = { 2, 3, 3, 4, 4, 4 };
        int[] expected2 = { 2, 3, 3 };
        int[] actual2 = N.intersection(a2, b2);
        Arrays.sort(actual2);
        assertArrayEquals(expected2, actual2);

        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.intersection(new int[] { 1 }, null));
    }

    @Test
    public void testIntersection_objectArrays() {
        String[] a = { "a", "b", "a", "c" };
        Object[] b = { "b", "a", "d", "a" };
        List<String> expected = Arrays.asList("a", "a", "b");
        List<String> actual = N.intersection(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.intersection((String[]) null, b).isEmpty());
        assertTrue(N.intersection(a, (Object[]) null).isEmpty());
        assertTrue(N.intersection(new String[0], b).isEmpty());
    }

    @Test
    public void testIntersection_collections() {
        Collection<String> a = Arrays.asList("a", "b", "a", "c");
        Collection<?> b = Arrays.asList("b", "a", "d", "a");
        List<String> expected = Arrays.asList("a", "a", "b");
        List<String> actual = N.intersection(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.intersection((Collection<String>) null, b).isEmpty());
        assertTrue(N.intersection(a, (Collection<?>) null).isEmpty());
    }

    @Test
    public void testIntersection_collectionOfCollections() {
        Collection<String> c1 = Arrays.asList("a", "b", "c", "c");
        Collection<String> c2 = Arrays.asList("b", "c", "d", "c");
        Collection<String> c3 = Arrays.asList("c", "a", "b", "c");

        List<String> expected = Arrays.asList("b", "c", "c");

        List<List<String>> listOfColls = Arrays.asList(new ArrayList<>(c1), new ArrayList<>(c2), new ArrayList<>(c3));
        List<String> actual = N.intersection(listOfColls);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.intersection((Collection<Collection<String>>) null).isEmpty());
        assertTrue(N.intersection(Collections.<Collection<String>> emptyList()).isEmpty());
        assertEquals(Arrays.asList("a", "b"), N.intersection(Collections.singletonList(Arrays.asList("a", "b"))));
        assertTrue(N.intersection(Arrays.asList(c1, Collections.emptyList())).isEmpty());
    }

    @Test
    public void testDifference_intArrays() {
        int[] a1 = { 0, 1, 2, 2, 3 };
        int[] b1 = { 2, 5, 1 };
        int[] expected1 = { 0, 2, 3 };
        int[] actual1 = N.difference(a1, b1);
        Arrays.sort(actual1);
        assertArrayEquals(expected1, actual1);

        assertArrayEquals(EMPTY_INT_ARRAY_CONST, N.difference(null, b1));
        assertArrayEquals(a1, N.difference(a1, null));
        assertArrayEquals(a1, N.difference(a1, EMPTY_INT_ARRAY_CONST));
    }

    @Test
    public void testDifference_objectArrays() {
        String[] a = { "a", "b", "a", "c" };
        Object[] b = { "b", "a", "d" };
        List<String> expected = Arrays.asList("a", "c");
        List<String> actual = N.difference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.difference((String[]) null, b).isEmpty());
        assertEquals(Arrays.asList(a), N.difference(a, (Object[]) null));
    }

    @Test
    public void testDifference_collections() {
        Collection<String> a = Arrays.asList("a", "b", "a", "c");
        Collection<?> b = Arrays.asList("b", "a", "d");
        List<String> expected = Arrays.asList("a", "c");
        List<String> actual = N.difference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertTrue(N.difference((Collection<String>) null, b).isEmpty());
        assertEquals(new ArrayList<>(a), N.difference(a, (Collection<?>) null));
    }

    @Test
    public void testSymmetricDifference_intArrays() {
        int[] a1 = { 0, 1, 2, 2, 3 };
        int[] b1 = { 2, 5, 1 };
        int[] expected1 = { 0, 2, 3, 5 };
        int[] actual1 = N.symmetricDifference(a1, b1);
        Arrays.sort(actual1);
        assertArrayEquals(expected1, actual1);

        assertArrayEquals(b1, N.symmetricDifference(null, b1));
        assertArrayEquals(a1, N.symmetricDifference(a1, null));
    }

    @Test
    public void testSymmetricDifference_objectArrays() {
        String[] a = { "a", "b", "a", "c" };
        String[] b = { "b", "a", "d" };
        List<String> expected = Arrays.asList("a", "c", "d");
        List<String> actual = N.symmetricDifference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertEquals(Arrays.asList(b), N.symmetricDifference(null, b));
        assertEquals(Arrays.asList(a), N.symmetricDifference(a, null));
    }

    @Test
    public void testSymmetricDifference_collections() {
        Collection<String> a = Arrays.asList("a", "b", "a", "c");
        Collection<String> b = Arrays.asList("b", "a", "d");
        List<String> expected = Arrays.asList("a", "c", "d");
        List<String> actual = N.symmetricDifference(a, b);
        Collections.sort(actual);
        Collections.sort(expected);
        assertEquals(expected, actual);

        assertEquals(new ArrayList<>(b), N.symmetricDifference(null, b));
        assertEquals(new ArrayList<>(a), N.symmetricDifference(a, null));
    }

    @Test
    public void testCommonSet_twoCollections() {
        Collection<String> a = Arrays.asList("a", "b", "c", "c");
        Collection<?> b = Arrays.asList("b", "c", "d", "c", "e");
        Set<String> expected = new HashSet<>(Arrays.asList("b", "c"));
        assertEquals(expected, N.commonSet(a, b));

        assertTrue(N.commonSet(null, b).isEmpty());
        assertTrue(N.commonSet(a, null).isEmpty());
        assertTrue(N.commonSet(a, Collections.emptyList()).isEmpty());
    }

    @Test
    public void testCommonSet_collectionOfCollections() {
        Collection<String> c1 = Arrays.asList("a", "b", "c", "c");
        Collection<String> c2 = Arrays.asList("b", "c", "d", "c");
        Collection<String> c3 = Arrays.asList("c", "a", "b", "c", "b");
        Set<String> expected = new HashSet<>(Arrays.asList("b", "c"));

        List<Collection<String>> listOfColls = Arrays.asList(c1, c2, c3);
        assertEquals(expected, N.commonSet(listOfColls));

        assertTrue(N.commonSet((Collection<Collection<String>>) null).isEmpty());
        assertEquals(new HashSet<>(c1), N.commonSet(Collections.singletonList(c1)));
        assertTrue(N.commonSet(Arrays.asList(c1, Collections.emptyList())).isEmpty());

        Collection<String> lc1 = new LinkedHashSet<>(Arrays.asList("z", "y", "x"));
        Collection<String> lc2 = new LinkedHashSet<>(Arrays.asList("y", "x", "w"));
        Set<String> expectedLinked = new LinkedHashSet<>(Arrays.asList("y", "x"));
        List<Collection<String>> listOfLinked = Arrays.asList(lc1, lc2);
        Set<String> actualLinked = N.commonSet(listOfLinked);
        assertEquals(expectedLinked, actualLinked);
        assertTrue(actualLinked instanceof LinkedHashSet);
    }

    @Test
    public void testExclude() {
        Collection<String> coll = Arrays.asList("a", "b", "a", "c");
        assertEquals(Arrays.asList("b", "c"), N.exclude(coll, "a"));
        assertEquals(Arrays.asList("a", "b", "a", "c"), N.exclude(coll, "d"));
        assertEquals(Arrays.asList("a", "a", "c"), N.exclude(coll, "b"));
        assertEquals(Arrays.asList("a", "b", "a", "c"), N.exclude(coll, null));

        Collection<String> collWithNull = Arrays.asList("a", null, "b", null);
        assertEquals(Arrays.asList("a", "b"), N.exclude(collWithNull, null));

        assertTrue(N.exclude(null, "a").isEmpty());
        assertTrue(N.exclude(Collections.emptyList(), "a").isEmpty());
    }

    @Test
    public void testExcludeToSet() {
        Collection<String> coll = Arrays.asList("a", "b", "a", "c");
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), N.excludeToSet(coll, "a"));
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), N.excludeToSet(coll, "d"));

        Collection<String> linkedColl = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        Set<String> resultLinked = N.excludeToSet(linkedColl, "a");
        assertEquals(new LinkedHashSet<>(Arrays.asList("c", "b")), resultLinked);
        assertTrue(resultLinked instanceof LinkedHashSet);

        assertTrue(N.excludeToSet(null, "a").isEmpty());
    }

    @Test
    public void testExcludeAll() {
        Collection<String> main = Arrays.asList("a", "b", "c", "a", "d");
        Collection<?> toExclude = Arrays.asList("a", "c", "e");
        assertEquals(Arrays.asList("b", "d"), N.excludeAll(main, toExclude));

        assertEquals(new ArrayList<>(main), N.excludeAll(main, null));
        assertEquals(new ArrayList<>(main), N.excludeAll(main, Collections.emptyList()));
        assertTrue(N.excludeAll(null, toExclude).isEmpty());

        assertEquals(Arrays.asList("b", "c", "d"), N.excludeAll(main, Collections.singletonList("a")));
    }

    @Test
    public void testExcludeAllToSet() {
        Collection<String> main = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        Collection<?> toExclude = Arrays.asList("a", "c", "e");
        assertEquals(new LinkedHashSet<>(Arrays.asList("b", "d")), N.excludeAllToSet(main, toExclude));

        assertEquals(new HashSet<>(main), N.excludeAllToSet(main, null));
        assertTrue(N.excludeAllToSet(null, toExclude).isEmpty());
    }

    @Test
    public void testIsSubCollection() {
        Collection<String> collA = Arrays.asList("a", "b", "c");
        Collection<String> collB = Arrays.asList("a", "b", "c", "d");
        Collection<String> collC = Arrays.asList("a", "b");
        Collection<String> collD = Arrays.asList("a", "x");
        Collection<String> collE = Arrays.asList("a", "a", "b");
        Collection<String> collF = Arrays.asList("a", "b", "a");

        assertTrue(N.isSubCollection(collC, collA));
        assertTrue(N.isSubCollection(collA, collB));
        assertTrue(N.isSubCollection(collA, collA));
        assertTrue(N.isSubCollection(Collections.emptyList(), collA));

        assertFalse(N.isSubCollection(collA, collC));
        assertFalse(N.isSubCollection(collD, collA));
        assertFalse(N.isSubCollection(collA, Collections.emptyList()));

        assertTrue(N.isSubCollection(collE, collF));
        assertTrue(N.isSubCollection(Arrays.asList("a", "b"), collE));
        assertFalse(N.isSubCollection(collE, Arrays.asList("a", "b")));

        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(null, collA));
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(collA, null));
    }

    @Test
    public void testIsProperSubCollection() {
        Collection<String> collA = Arrays.asList("a", "b", "c");
        Collection<String> collB = Arrays.asList("a", "b", "c", "d");
        Collection<String> collC = Arrays.asList("a", "b");

        assertTrue(N.isProperSubCollection(collC, collA));
        assertTrue(N.isProperSubCollection(collA, collB));
        assertFalse(N.isProperSubCollection(collA, collA));
        assertTrue(N.isProperSubCollection(Collections.emptyList(), collA));

        assertFalse(N.isProperSubCollection(collA, collC));
        assertFalse(N.isProperSubCollection(collA, Collections.emptyList()));

        Collection<String> collE = Arrays.asList("a", "b");
        Collection<String> collF = Arrays.asList("a", "a", "b");
        assertTrue(N.isProperSubCollection(collE, collF));
        assertFalse(N.isProperSubCollection(collF, collE));

        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(null, collA));
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(collA, null));
    }

    @Test
    public void testIsEqualCollection() {
        Collection<String> collA1 = Arrays.asList("a", "b", "a");
        Collection<String> collA2 = Arrays.asList("a", "a", "b");
        Collection<String> collB = Arrays.asList("a", "b");
        Collection<String> collC = Arrays.asList("a", "b", "c");

        assertTrue(N.isEqualCollection(collA1, collA2));
        assertTrue(N.isEqualCollection(null, null));
        assertTrue(N.isEqualCollection(Collections.emptyList(), Collections.emptySet()));

        assertFalse(N.isEqualCollection(collA1, collB));
        assertFalse(N.isEqualCollection(collA1, collC));
        assertFalse(N.isEqualCollection(collA1, null));
        assertFalse(N.isEqualCollection(null, collA1));

        Collection<Integer> list1 = Arrays.asList(1, 2, 2, 3);
        Collection<Integer> list2 = new LinkedList<>(Arrays.asList(3, 2, 1, 2));
        assertTrue(N.isEqualCollection(list1, list2));
    }

    @Test
    public void testComplexMergeWithMultipleIterables() {
        List<List<Integer>> iterables = new ArrayList<>();
        iterables.add(Arrays.asList(1, 5, 9));
        iterables.add(Arrays.asList(2, 6, 10));
        iterables.add(Arrays.asList(3, 7, 11));
        iterables.add(Arrays.asList(4, 8, 12));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        List<Integer> result = N.merge(iterables, selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), result);
    }

    @Test
    public void testMergeWithNullElements() {
        List<List<Integer>> iterables = new ArrayList<>();
        iterables.add(null);
        iterables.add(Arrays.asList(1, 2, 3));
        iterables.add(null);
        iterables.add(Arrays.asList(4, 5, 6));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;

        List<Integer> result = N.merge(iterables, selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testGroupByWithComplexCollector() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 35, "Sales"),
                new Person("David", 25, "Sales"), new Person("Eve", 30, "Engineering"));

        Map<String, Double> avgAgeByDept = N.groupBy(people, Person::getDepartment, Collectors.averagingInt(Person::getAge));

        assertEquals(28.33, avgAgeByDept.get("Engineering"), 0.01);
        assertEquals(30.0, avgAgeByDept.get("Sales"), 0.01);
    }

    @Test
    public void testNestedGroupBy() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 25, "Sales"),
                new Person("David", 30, "Sales"));

        Map<String, List<Person>> byDept = N.groupBy(people, Person::getDepartment);

        Map<String, Map<Integer, List<Person>>> result = new HashMap<>();
        for (Map.Entry<String, List<Person>> entry : byDept.entrySet()) {
            result.put(entry.getKey(), N.groupBy(entry.getValue(), Person::getAge));
        }

        assertEquals(2, result.get("Engineering").size());
        assertEquals(1, result.get("Engineering").get(25).size());
        assertEquals("Alice", result.get("Engineering").get(25).get(0).getName());
    }

    @Test
    public void testZipWithDifferentTypesAndTransformations() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        String[] departments = { "Engineering", "Sales", "Marketing" };

        TriFunction<String, Integer, String, Person> zipper = (name, age, dept) -> new Person(name, age, dept);

        List<Person> people = N.zip(names, ages, departments, zipper);
        assertEquals(3, people.size());
        assertEquals("Alice", people.get(0).getName());
        assertEquals(30, people.get(1).getAge());
        assertEquals("Marketing", people.get(2).getDepartment());
    }

    @Test
    public void testUnzipComplexObjects() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Sales"), new Person("Charlie", 35, "Marketing"));

        BiConsumer<Person, Pair<String, Integer>> unzipper = (person, pair) -> pair.set(person.getName(), person.getAge());

        Pair<List<String>, List<Integer>> result = N.unzip(people, unzipper);
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), result.left());
        assertEquals(Arrays.asList(25, 30, 35), result.right());
    }

    @Test
    public void testFilterPerformanceWithLargeData() {
        int size = 100000;
        Integer[] data = new Integer[size];
        for (int i = 0; i < size; i++) {
            data[i] = i;
        }

        long startTime = System.currentTimeMillis();
        List<Integer> result = N.filter(data, i -> i % 100 == 0);
        long endTime = System.currentTimeMillis();

        assertEquals(1000, result.size());
        assertTrue((endTime - startTime) < 100, "Filter operation took too long");
    }

    @Test
    public void testChainedOperations() {
        String[] data = { "apple", "banana", "apricot", "berry", "cherry", "date" };

        List<String> filtered = N.filter(data, s -> s.length() > 5);
        List<Character> mapped = N.map(filtered, s -> s.charAt(0));
        List<Character> distinct = N.distinct(mapped);

        assertEquals(3, distinct.size());
        assertTrue(distinct.contains('b'));
        assertTrue(distinct.contains('a'));
        assertTrue(distinct.contains('c'));
    }

    @Test
    public void testComplexPredicateCombinations() {
        Integer[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        Predicate<Integer> isEven = i -> i % 2 == 0;
        Predicate<Integer> isGreaterThan5 = i -> i > 5;
        Predicate<Integer> combined = i -> isEven.test(i) && isGreaterThan5.test(i);

        List<Integer> result = N.filter(numbers, combined);
        assertEquals(Arrays.asList(6, 8, 10), result);
    }

    @Test
    public void testUnicodeAndSpecialCharacters() {
        String[] unicodeStrings = { "café", "naïve", "résumé", "🎉", "😀" };

        List<String> filtered = N.filter(unicodeStrings, s -> s.contains("é"));
        assertEquals(2, filtered.size());

        List<Integer> lengths = N.map(unicodeStrings, String::length);
        assertEquals(Arrays.asList(4, 5, 6, 2, 2), lengths);
    }

    @Test
    public void testConcurrentModificationScenarios() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(ConcurrentModificationException.class, () -> N.filter(list, i -> {
            if (i == 3) {
                list.add(6);
            }
            return i % 2 == 0;
        }));

    }

    @Test
    public void testMemoryEfficientOperations() {
        int[] largeArray = new int[1000000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }

        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        int count = N.count(largeArray, i -> i % 1000 == 0);
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        assertEquals(1000, count);
        assertTrue((endMemory - startMemory) < 1000000, "Memory usage increased significantly");
    }

    @Test
    public void testBoundaryValues() {
        int maxSize = 1000;

        boolean[] allTrue = new boolean[maxSize];
        Arrays.fill(allTrue, true);
        assertTrue(N.allTrue(allTrue));
        assertFalse(N.anyFalse(allTrue));

        int[] numbers = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new int[0], N.filter(numbers, 3, 3, i -> true));
        assertEquals(0, N.count(numbers, 5, 5, i -> true));
    }

    @Test
    public void testCustomPredicatesAndFunctions() {
        String[] words = { "hello", "world", "java", "programming" };

        Predicate<String> complexPredicate = new Predicate<>() {
            @Override
            public boolean test(String s) {
                return s.length() > 4 && s.contains("o") && !s.startsWith("p");
            }
        };

        List<String> result = N.filter(words, complexPredicate);
        assertEquals(Arrays.asList("hello", "world"), result);

        Function<String, String> statefulMapper = new Function<>() {
            private int counter = 0;

            @Override
            public String apply(String s) {
                return s + "_" + (counter++);
            }
        };

        List<String> mapped = N.map(words, statefulMapper);
        assertEquals("hello_0", mapped.get(0));
        assertEquals("programming_3", mapped.get(3));
    }

    @Test
    public void testNullElementHandling() {
        String[] withNulls = { "a", null, "b", null, "c" };

        List<String> nonNulls = N.filter(withNulls, Objects::nonNull);
        assertEquals(Arrays.asList("a", "b", "c"), nonNulls);

        List<Integer> lengths = N.map(withNulls, s -> s == null ? -1 : s.length());
        assertEquals(Arrays.asList(1, -1, 1, -1, 1), lengths);

        String[] duplicatesWithNulls = { "a", null, "a", null, "b" };
        List<String> distinct = N.distinct(duplicatesWithNulls);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testExtremeRanges() {
        int[] array = new int[100];
        for (int i = 0; i < 100; i++) {
            array[i] = i;
        }

        int[] filtered = N.filter(array, 0, 100, i -> i % 10 == 0);
        assertEquals(10, filtered.length);

        filtered = N.filter(array, 50, 51, i -> true);
        assertArrayEquals(new int[] { 50 }, filtered);

        filtered = N.filter(array, 99, 100, i -> true);
        assertArrayEquals(new int[] { 99 }, filtered);
    }

    @Test
    public void testWithDifferentCollectionTypes() {
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c"));
        List<String> upperLinked = N.map(linkedList, 0, 3, String::toUpperCase);
        assertEquals(Arrays.asList("A", "B", "C"), upperLinked);

        TreeSet<Integer> treeSet = new TreeSet<>(Arrays.asList(3, 1, 4, 1, 5, 9));
        List<Integer> filtered = N.filter(treeSet, i -> i > 3);
        assertEquals(Arrays.asList(4, 5, 9), filtered);

        ArrayDeque<String> deque = new ArrayDeque<>(Arrays.asList("first", "second", "third"));
        List<Integer> lengths = N.map(deque, String::length);
        assertEquals(Arrays.asList(5, 6, 5), lengths);
    }

    @Test
    public void testWithCustomCollections() {
        CustomIterable<Integer> custom = new CustomIterable<>(Arrays.asList(1, 2, 3, 4, 5));

        List<Integer> filtered = N.filter(custom, i -> i % 2 == 0);
        assertEquals(Arrays.asList(2, 4), filtered);

        List<String> mapped = N.map(custom, i -> "num" + i);
        assertEquals(5, mapped.size());
    }

    @Test
    public void testCollectorIntegration() {
        List<String> words = Arrays.asList("apple", "banana", "apricot", "blueberry", "cherry");

        Map<Character, String> joined = N.groupBy(words, s -> s.charAt(0), Collectors.joining(", "));

        assertEquals("apple, apricot", joined.get('a'));
        assertEquals("banana, blueberry", joined.get('b'));
        assertEquals("cherry", joined.get('c'));

        Map<Integer, Long> countByLength = N.groupBy(words, String::length, Collectors.counting());

        assertEquals(1L, (long) countByLength.get(5));
        assertEquals(2L, (long) countByLength.get(6));
    }

    @Test
    public void testPrimitiveArrayConversions() {
        long[] longs = { 1L, 2L, 3L, 4L, 5L };
        int[] ints = N.mapToInt(longs, l -> (int) (l * 2));
        assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, ints);

        int[] intArray = { 1, 2, 3 };
        long[] longArray = N.mapToLong(intArray, i -> i * 1000000L);
        assertArrayEquals(new long[] { 1000000L, 2000000L, 3000000L }, longArray);

        int[] scores = { 85, 90, 78, 92, 88 };
        double[] percentages = N.mapToDouble(scores, score -> score / 100.0);
        assertArrayEquals(new double[] { 0.85, 0.90, 0.78, 0.92, 0.88 }, percentages, 0.001);
    }

    @Test
    public void testPrimitivePredicates() {
        char[] chars = { 'a', 'B', 'c', 'D', 'e' };
        char[] uppercase = N.filter(chars, Character::isUpperCase);
        assertArrayEquals(new char[] { 'B', 'D' }, uppercase);

        byte[] bytes = { -128, -1, 0, 1, 127 };
        byte[] positive = N.filter(bytes, b -> b > 0);
        assertArrayEquals(new byte[] { 1, 127 }, positive);

        float[] floats = { 1.5f, 2.0f, 2.5f, 3.0f, 3.5f };
        float[] integers = N.filter(floats, f -> f == (int) f);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, integers, 0.001f);
    }

    @Test
    public void testPrimitiveDistinct() {
        char[] chars = { 'a', 'b', 'a', 'c', 'b', 'c' };
        char[] distinctChars = N.distinct(chars);
        Arrays.sort(distinctChars);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, distinctChars);

        double[] doubles = { 1.1, 2.2, 1.1, 3.3, 2.2, 3.3 };
        double[] distinctDoubles = N.distinct(doubles);
        assertEquals(3, distinctDoubles.length);

        double[] special = { Double.NaN, 1.0, Double.POSITIVE_INFINITY, Double.NaN, 1.0 };
        double[] distinctSpecial = N.distinct(special);
        assertEquals(3, distinctSpecial.length);
    }

    @Test
    public void testSumInt_Array() {
        assertEquals(6, N.sumInt(new Integer[] { 1, 2, 3 }));
        assertEquals(5, N.sumInt(new Integer[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(0, N.sumInt(new Integer[] {}));
        assertEquals(6, N.sumInt(new Integer[] { 1, 2, 3 }, x -> x));
        assertEquals(12, N.sumInt(new Integer[] { 1, 2, 3 }, x -> x * 2));
    }

    @Test
    public void testSumInt_Collection() {
        assertEquals(5, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(10, N.sumInt(Arrays.asList(1, 2, 3, 4), 1, 3, x -> x * 2));
        assertEquals(10, N.sumInt(CommonUtil.toLinkedHashSet(1, 2, 3, 4), 1, 3, x -> x * 2));
    }

    @Test
    public void testSumInt_Iterable() {
        assertEquals(6, N.sumInt(Arrays.asList(1, 2, 3)));
        assertEquals(12, N.sumInt(Arrays.asList(1, 2, 3), x -> x * 2));
        assertEquals(0, N.sumInt(Collections.emptyList()));
    }

    @Test
    public void testSumIntToLong_Iterable() {
        assertEquals(6L, N.sumIntToLong(Arrays.asList(1, 2, 3)));
        assertEquals(12L, N.sumIntToLong(Arrays.asList(1, 2, 3), x -> x * 2));
        assertEquals(0L, N.sumIntToLong(Collections.emptyList()));
    }

    @Test
    public void testSumLong_Array() {
        assertEquals(6L, N.sumLong(new Long[] { 1L, 2L, 3L }));
        assertEquals(5L, N.sumLong(new Long[] { 1L, 2L, 3L, 4L }, 1, 3));
        assertEquals(0L, N.sumLong(new Long[] {}));
        assertEquals(6L, N.sumLong(new Long[] { 1L, 2L, 3L }, x -> x));
        assertEquals(12L, N.sumLong(new Long[] { 1L, 2L, 3L }, x -> x * 2));
    }

    @Test
    public void testSumLong_Collection() {
        assertEquals(5L, N.sumLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3));
        assertEquals(10L, N.sumLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3, x -> x * 2));
        assertEquals(10L, N.sumLong(CommonUtil.toLinkedHashSet(1L, 2L, 3L, 4L), 1, 3, x -> x * 2));
    }

    @Test
    public void testSumLong_Iterable() {
        assertEquals(6L, N.sumLong(Arrays.asList(1L, 2L, 3L)));
        assertEquals(12L, N.sumLong(Arrays.asList(1L, 2L, 3L), x -> x * 2));
        assertEquals(0L, N.sumLong(Collections.emptyList()));
    }

    @Test
    public void testSumDouble_Array() {
        assertEquals(6.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0 }), 0.001);
        assertEquals(5.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals(0.0, N.sumDouble(new Double[] {}), 0.001);
        assertEquals(6.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x), 0.001);
        assertEquals(12.0, N.sumDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_Collection() {
        assertEquals(5.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3), 0.001);
        assertEquals(10.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3, x -> x * 2), 0.001);
        assertEquals(10.0, N.sumDouble(CommonUtil.toLinkedHashSet(1.0, 2.0, 3.0, 4.0), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_Iterable() {
        assertEquals(6.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0)), 0.001);
        assertEquals(12.0, N.sumDouble(Arrays.asList(1.0, 2.0, 3.0), x -> x * 2), 0.001);
        assertEquals(0.0, N.sumDouble(Collections.emptyList()), 0.001);
    }

    @Test
    public void testSumBigInteger() {
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigInteger.valueOf(12), N.sumBigInteger(Arrays.asList(1, 2, 3), x -> BigInteger.valueOf(x * 2)));
        assertEquals(BigInteger.ZERO, N.sumBigInteger(Collections.emptyList()));
    }

    @Test
    public void testSumBigDecimal() {
        assertEquals(BigDecimal.valueOf(6), N.sumBigDecimal(Arrays.asList(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        assertEquals(BigDecimal.valueOf(12), N.sumBigDecimal(Arrays.asList(1, 2, 3), x -> BigDecimal.valueOf(x * 2)));
        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(Collections.emptyList()));
    }

    @Test
    public void testAverageInt_Array() {
        assertEquals(2.0, N.averageInt(new Integer[] { 1, 2, 3 }), 0.001);
        assertEquals(2.5, N.averageInt(new Integer[] { 1, 2, 3, 4 }, 1, 3), 0.001);
        assertEquals(0.0, N.averageInt(new Integer[] {}), 0.001);
        assertEquals(2.0, N.averageInt(new Integer[] { 1, 2, 3 }, x -> x), 0.001);
        assertEquals(4.0, N.averageInt(new Integer[] { 1, 2, 3 }, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_Collection() {
        assertEquals(2.5, N.averageInt(Arrays.asList(1, 2, 3, 4), 1, 3), 0.001);
        assertEquals(5.0, N.averageInt(Arrays.asList(1, 2, 3, 4), 1, 3, x -> x * 2), 0.001);
        assertEquals(5.0, N.averageInt(CommonUtil.toLinkedHashSet(1, 2, 3, 4), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_Iterable() {
        assertEquals(2.0, N.averageInt(Arrays.asList(1, 2, 3)), 0.001);
        assertEquals(4.0, N.averageInt(Arrays.asList(1, 2, 3), x -> x * 2), 0.001);
        assertEquals(0.0, N.averageInt(Collections.emptyList()), 0.001);
    }

    @Test
    public void testAverageLong_Array() {
        assertEquals(2.0, N.averageLong(new Long[] { 1L, 2L, 3L }), 0.001);
        assertEquals(2.5, N.averageLong(new Long[] { 1L, 2L, 3L, 4L }, 1, 3), 0.001);
        assertEquals(0.0, N.averageLong(new Long[] {}), 0.001);
        assertEquals(2.0, N.averageLong(new Long[] { 1L, 2L, 3L }, x -> x), 0.001);
        assertEquals(4.0, N.averageLong(new Long[] { 1L, 2L, 3L }, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_Collection() {
        assertEquals(2.5, N.averageLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3), 0.001);
        assertEquals(5.0, N.averageLong(Arrays.asList(1L, 2L, 3L, 4L), 1, 3, x -> x * 2), 0.001);
        assertEquals(5.0, N.averageLong(CommonUtil.toLinkedHashSet(1L, 2L, 3L, 4L), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_Iterable() {
        assertEquals(2.0, N.averageLong(Arrays.asList(1L, 2L, 3L)), 0.001);
        assertEquals(4.0, N.averageLong(Arrays.asList(1L, 2L, 3L), x -> x * 2), 0.001);
        assertEquals(0.0, N.averageLong(Collections.emptyList()), 0.001);
    }

    @Test
    public void testAverageDouble_Array() {
        assertEquals(2.0, N.averageDouble(new Double[] { 1.0, 2.0, 3.0 }), 0.001);
        assertEquals(2.5, N.averageDouble(new Double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals(0.0, N.averageDouble(new Double[] {}), 0.001);
        assertEquals(2.0, N.averageDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x), 0.001);
        assertEquals(4.0, N.averageDouble(new Double[] { 1.0, 2.0, 3.0 }, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_Collection() {
        assertEquals(2.5, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3), 0.001);
        assertEquals(5.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0, 4.0), 1, 3, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_Iterable() {
        assertEquals(2.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0)), 0.001);
        assertEquals(4.0, N.averageDouble(Arrays.asList(1.0, 2.0, 3.0), x -> x * 2), 0.001);
        assertEquals(0.0, N.averageDouble(Collections.emptyList()), 0.001);
    }

    @Test
    public void testAverageBigInteger() {
        assertEquals(BigDecimal.valueOf(2), N.averageBigInteger(Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3))));
        assertEquals(BigDecimal.valueOf(4), N.averageBigInteger(Arrays.asList(1, 2, 3), x -> BigInteger.valueOf(x * 2)));
        assertEquals(BigDecimal.ZERO, N.averageBigInteger(Collections.emptyList()));
    }

    @Test
    public void testAverageBigDecimal() {
        assertEquals(BigDecimal.valueOf(2), N.averageBigDecimal(Arrays.asList(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3))));
        assertEquals(BigDecimal.valueOf(4), N.averageBigDecimal(Arrays.asList(1, 2, 3), x -> BigDecimal.valueOf(x * 2)));
        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(Collections.emptyList()));
    }

    @Test
    public void testMinTwoValues() {
        assertEquals('a', N.min('a', 'b'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2));
        assertEquals((short) 1, N.min((short) 1, (short) 2));
        assertEquals(1, N.min(1, 2));
        assertEquals(1L, N.min(1L, 2L));
        assertEquals(1.0f, N.min(1.0f, 2.0f), 0.001f);
        assertEquals(1.0, N.min(1.0, 2.0), 0.001);
        assertEquals(1, N.min(1, 2));
        assertEquals(Integer.valueOf(1), N.min(1, 2, Comparator.naturalOrder()));
        assertEquals("a", N.min("a", "b"));
        assertEquals("a", N.min("a", "b", Comparator.naturalOrder()));
    }

    @Test
    public void testMinThreeValues() {
        assertEquals('a', N.min('a', 'b', 'c'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3));
        assertEquals(1, N.min(1, 2, 3));
        assertEquals(1L, N.min(1L, 2L, 3L));
        assertEquals(1.0f, N.min(1.0f, 2.0f, 3.0f), 0.001f);
        assertEquals(1.0, N.min(1.0, 2.0, 3.0), 0.001);
        assertEquals(1, N.min(1, 2, 3));
        assertEquals(Integer.valueOf(1), N.min(1, 2, 3, Comparator.naturalOrder()));
        assertEquals("a", N.min("a", "b", "c"));
        assertEquals("a", N.min("a", "b", "c", Comparator.naturalOrder()));
    }

    @Test
    public void testMinArray() {
        assertEquals('a', N.min('a', 'b', 'c', 'd'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3, (short) 4));
        assertEquals(1, N.min(1, 2, 3, 4));
        assertEquals(1L, N.min(1L, 2L, 3L, 4L));
        assertEquals(1.0f, N.min(1.0f, 2.0f, 3.0f, 4.0f), 0.001f);
        assertEquals(1.0, N.min(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals("a", N.min(new String[] { "a", "b", "c", "d" }));
        assertEquals("a", N.min(new String[] { "a", "b", "c", "d" }, Comparator.naturalOrder()));

        assertEquals('b', N.min(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals((byte) 2, N.min(new byte[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals((short) 2, N.min(new short[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(2, N.min(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(2L, N.min(new long[] { 1L, 2L, 3L, 4L }, 1, 3));
        assertEquals(2.0f, N.min(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1, 3), 0.001f);
        assertEquals(2.0, N.min(new double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals("b", N.min(new String[] { "a", "b", "c", "d" }, 1, 3));
        assertEquals("b", N.min(new String[] { "a", "b", "c", "d" }, 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMinArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.min(new int[] {}));
    }

    @Test
    public void testMinArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.min((int[]) null));
    }

    @Test
    public void testMinCollection() {
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(2), N.min(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), 0, 4, Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(2), N.min(Arrays.asList(1, 2, 3, 4), 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMinIterable() {
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));

        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(null, 1, 2, 3)));
        assertNull(N.min(Arrays.asList(null, null, null)));
    }

    @Test
    public void testMinIterator() {
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4).iterator()));
        assertEquals(Integer.valueOf(1), N.min(Arrays.asList(1, 2, 3, 4).iterator(), Comparator.naturalOrder()));
    }

    @Test
    public void testMinIteratorEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<Integer> emptyList().iterator()));
    }

    @Test
    public void testMinBy() {
        Person[] people = { new Person("John", 25), new Person("Jane", 22), new Person("Bob", 30) };
        assertEquals("Jane", N.minBy(people, p -> p.age).name);

        List<Person> peopleList = Arrays.asList(people);
        assertEquals("Jane", N.minBy(peopleList, p -> p.age).name);

        assertEquals("Jane", N.minBy(peopleList.iterator(), p -> p.age).name);
    }

    @Test
    public void testMinAll() {
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 1, 2, 3, 1, 4 }));
        assertEquals(Arrays.asList(1, 1), N.minAll(new Integer[] { 1, 2, 3, 1, 4 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4)));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4), Comparator.naturalOrder()));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4).iterator()));
        assertEquals(Arrays.asList(1, 1), N.minAll(Arrays.asList(1, 2, 3, 1, 4).iterator(), Comparator.naturalOrder()));

        assertEquals(Collections.emptyList(), N.minAll(new Integer[] {}));
        assertEquals(Collections.emptyList(), N.minAll(Collections.emptyList()));
    }

    @Test
    public void testMinOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(1), N.minOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(Integer.valueOf(1), N.minOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(Integer.valueOf(1), N.minOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.minOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMinIntOrDefaultIfEmpty() {
        assertEquals(1, N.minIntOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(1, N.minIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(1, N.minIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(99, N.minIntOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMinLongOrDefaultIfEmpty() {
        assertEquals(1L, N.minLongOrDefaultIfEmpty(new Long[] { 1L, 2L, 3L }, x -> x, 99L));
        assertEquals(99L, N.minLongOrDefaultIfEmpty(new Long[] {}, x -> x, 99L));
        assertEquals(1L, N.minLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L), x -> x, 99L));
        assertEquals(99L, N.minLongOrDefaultIfEmpty(Collections.<Long> emptyList(), x -> x, 99L));
        assertEquals(1L, N.minLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L).iterator(), x -> x, 99L));
        assertEquals(99L, N.minLongOrDefaultIfEmpty(Collections.<Long> emptyIterator(), x -> x, 99L));
    }

    @Test
    public void testMinDoubleOrDefaultIfEmpty() {
        assertEquals(1.0, N.minDoubleOrDefaultIfEmpty(new Double[] { 1.0, 2.0, 3.0 }, x -> x, 99.0), 0.001);
        assertEquals(99.0, N.minDoubleOrDefaultIfEmpty(new Double[] {}, x -> x, 99.0), 0.001);
        assertEquals(1.0, N.minDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.minDoubleOrDefaultIfEmpty(Collections.<Double> emptyList(), x -> x, 99.0), 0.001);
        assertEquals(1.0, N.minDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0).iterator(), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.minDoubleOrDefaultIfEmpty(Collections.<Double> emptyIterator(), x -> x, 99.0), 0.001);
    }

    @Test
    public void testMinMax() {
        Pair<Integer, Integer> result = N.minMax(new Integer[] { 1, 2, 3, 4, 5 });
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(new Integer[] { 1, 2, 3, 4, 5 }, Comparator.naturalOrder());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5), Comparator.naturalOrder());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5).iterator());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(Arrays.asList(1, 2, 3, 4, 5).iterator(), Comparator.naturalOrder());
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(5), result.right());

        result = N.minMax(new Integer[] { 1 });
        assertEquals(Integer.valueOf(1), result.left());
        assertEquals(Integer.valueOf(1), result.right());
    }

    @Test
    public void testMaxTwoValues() {
        assertEquals('b', N.max('a', 'b'));
        assertEquals((byte) 2, N.max((byte) 1, (byte) 2));
        assertEquals((short) 2, N.max((short) 1, (short) 2));
        assertEquals(2, N.max(1, 2));
        assertEquals(2L, N.max(1L, 2L));
        assertEquals(2.0f, N.max(1.0f, 2.0f), 0.001f);
        assertEquals(2.0, N.max(1.0, 2.0), 0.001);
        assertEquals(2, N.max(1, 2));
        assertEquals(Integer.valueOf(2), N.max(1, 2, Comparator.naturalOrder()));
        assertEquals("b", N.max("a", "b"));
        assertEquals("b", N.max("a", "b", Comparator.naturalOrder()));
    }

    @Test
    public void testMaxThreeValues() {
        assertEquals('c', N.max('a', 'b', 'c'));
        assertEquals((byte) 3, N.max((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 3, N.max((short) 1, (short) 2, (short) 3));
        assertEquals(3, N.max(1, 2, 3));
        assertEquals(3L, N.max(1L, 2L, 3L));
        assertEquals(3.0f, N.max(1.0f, 2.0f, 3.0f), 0.001f);
        assertEquals(3.0, N.max(1.0, 2.0, 3.0), 0.001);
        assertEquals(3, N.max(1, 2, 3));
        assertEquals(Integer.valueOf(3), N.max(1, 2, 3, Comparator.naturalOrder()));
        assertEquals("c", N.max("a", "b", "c"));
        assertEquals("c", N.max("a", "b", "c", Comparator.naturalOrder()));
    }

    @Test
    public void testMaxArray() {
        assertEquals('d', N.max('a', 'b', 'c', 'd'));
        assertEquals((byte) 4, N.max((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        assertEquals((short) 4, N.max((short) 1, (short) 2, (short) 3, (short) 4));
        assertEquals(4, N.max(1, 2, 3, 4));
        assertEquals(4L, N.max(1L, 2L, 3L, 4L));
        assertEquals(4.0f, N.max(1.0f, 2.0f, 3.0f, 4.0f), 0.001f);
        assertEquals(4.0, N.max(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals("d", N.max(new String[] { "a", "b", "c", "d" }));
        assertEquals("d", N.max(new String[] { "a", "b", "c", "d" }, Comparator.naturalOrder()));

        assertEquals('c', N.max(new char[] { 'a', 'b', 'c', 'd' }, 1, 3));
        assertEquals((byte) 3, N.max(new byte[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals((short) 3, N.max(new short[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(3, N.max(new int[] { 1, 2, 3, 4 }, 1, 3));
        assertEquals(3L, N.max(new long[] { 1L, 2L, 3L, 4L }, 1, 3));
        assertEquals(3.0f, N.max(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1, 3), 0.001f);
        assertEquals(3.0, N.max(new double[] { 1.0, 2.0, 3.0, 4.0 }, 1, 3), 0.001);
        assertEquals("c", N.max(new String[] { "a", "b", "c", "d" }, 1, 3));
        assertEquals("c", N.max(new String[] { "a", "b", "c", "d" }, 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMaxArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.max(new int[] {}));
    }

    @Test
    public void testMaxArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.max((int[]) null));
    }

    @Test
    public void testMaxCollection() {
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, 4), 1, 3));
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), 0, 4, Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, 4), 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMaxIterable() {
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));

        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(1, 2, 3, null)));
        assertNull(N.max(Arrays.asList(null, null, null)));
    }

    @Test
    public void testMaxIterator() {
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4).iterator()));
        assertEquals(Integer.valueOf(4), N.max(Arrays.asList(1, 2, 3, 4).iterator(), Comparator.naturalOrder()));
    }

    @Test
    public void testMaxIteratorEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<Integer> emptyList().iterator()));
    }

    @Test
    public void testMaxBy() {
        Person[] people = { new Person("John", 25), new Person("Jane", 22), new Person("Bob", 30) };
        assertEquals("Bob", N.maxBy(people, p -> p.age).name);

        List<Person> peopleList = Arrays.asList(people);
        assertEquals("Bob", N.maxBy(peopleList, p -> p.age).name);

        assertEquals("Bob", N.maxBy(peopleList.iterator(), p -> p.age).name);
    }

    @Test
    public void testMaxAll() {
        assertEquals(Arrays.asList(4, 4), N.maxAll(new Integer[] { 1, 2, 4, 3, 4 }));
        assertEquals(Arrays.asList(4, 4), N.maxAll(new Integer[] { 1, 2, 4, 3, 4 }, Comparator.naturalOrder()));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4)));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4), Comparator.naturalOrder()));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4).iterator()));
        assertEquals(Arrays.asList(4, 4), N.maxAll(Arrays.asList(1, 2, 4, 3, 4).iterator(), Comparator.naturalOrder()));

        assertEquals(Collections.emptyList(), N.maxAll(new Integer[] {}));
        assertEquals(Collections.emptyList(), N.maxAll(Collections.emptyList()));
    }

    @Test
    public void testMaxOrDefaultIfEmpty() {
        assertEquals(Integer.valueOf(3), N.maxOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(Integer.valueOf(3), N.maxOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(Integer.valueOf(3), N.maxOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(Integer.valueOf(99), N.maxOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMaxIntOrDefaultIfEmpty() {
        assertEquals(3, N.maxIntOrDefaultIfEmpty(new Integer[] { 1, 2, 3 }, x -> x, 99));
        assertEquals(99, N.maxIntOrDefaultIfEmpty(new Integer[] {}, x -> x, 99));
        assertEquals(3, N.maxIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3), x -> x, 99));
        assertEquals(99, N.maxIntOrDefaultIfEmpty(Collections.<Integer> emptyList(), x -> x, 99));
        assertEquals(3, N.maxIntOrDefaultIfEmpty(Arrays.asList(1, 2, 3).iterator(), x -> x, 99));
        assertEquals(99, N.maxIntOrDefaultIfEmpty(Collections.<Integer> emptyIterator(), x -> x, 99));
    }

    @Test
    public void testMaxLongOrDefaultIfEmpty() {
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(new Long[] { 1L, 2L, 3L }, x -> x, 99L));
        assertEquals(99L, N.maxLongOrDefaultIfEmpty(new Long[] {}, x -> x, 99L));
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L), x -> x, 99L));
        assertEquals(99L, N.maxLongOrDefaultIfEmpty(Collections.<Long> emptyList(), x -> x, 99L));
        assertEquals(3L, N.maxLongOrDefaultIfEmpty(Arrays.asList(1L, 2L, 3L).iterator(), x -> x, 99L));
        assertEquals(99L, N.maxLongOrDefaultIfEmpty(Collections.<Long> emptyIterator(), x -> x, 99L));
    }

    @Test
    public void testMaxDoubleOrDefaultIfEmpty() {
        assertEquals(3.0, N.maxDoubleOrDefaultIfEmpty(new Double[] { 1.0, 2.0, 3.0 }, x -> x, 99.0), 0.001);
        assertEquals(99.0, N.maxDoubleOrDefaultIfEmpty(new Double[] {}, x -> x, 99.0), 0.001);
        assertEquals(3.0, N.maxDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.maxDoubleOrDefaultIfEmpty(Collections.<Double> emptyList(), x -> x, 99.0), 0.001);
        assertEquals(3.0, N.maxDoubleOrDefaultIfEmpty(Arrays.asList(1.0, 2.0, 3.0).iterator(), x -> x, 99.0), 0.001);
        assertEquals(99.0, N.maxDoubleOrDefaultIfEmpty(Collections.<Double> emptyIterator(), x -> x, 99.0), 0.001);
    }

    @Test
    public void testMedianThreeValues() {
        assertEquals('b', N.median('a', 'b', 'c'));
        assertEquals('b', N.median('c', 'b', 'a'));
        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
        assertEquals(2, N.median(1, 2, 3));
        assertEquals(2L, N.median(1L, 2L, 3L));
        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f), 0.001f);
        assertEquals(2.0, N.median(1.0, 2.0, 3.0), 0.001);
        assertEquals(2, N.median(1, 2, 3));
        assertEquals((Integer) 2, N.median(1, 2, 3, Comparator.naturalOrder()));
        assertEquals("b", N.median("a", "b", "c"));
        assertEquals("b", N.median("a", "b", "c", Comparator.naturalOrder()));
    }

    @Test
    public void testMedianArray() {
        assertEquals('b', N.median('a', 'b', 'c', 'd'));
        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3, (short) 4));
        assertEquals(2, N.median(1, 2, 3, 4));
        assertEquals(2L, N.median(1L, 2L, 3L, 4L));
        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f, 4.0f), 0.001f);
        assertEquals(2.0, N.median(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }));
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }, Comparator.naturalOrder()));

        assertEquals(1, N.median(1));

        assertEquals(1, N.median(1, 2));

        assertEquals('b', N.median(new char[] { 'a', 'b', 'c', 'd' }, 0, 3));
        assertEquals((byte) 2, N.median(new byte[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals((short) 2, N.median(new short[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals(2, N.median(new int[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals(2L, N.median(new long[] { 1L, 2L, 3L, 4L }, 0, 3));
        assertEquals(2.0f, N.median(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 0, 3), 0.001f);
        assertEquals(2.0, N.median(new double[] { 1.0, 2.0, 3.0, 4.0 }, 0, 3), 0.001);
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }, 0, 3));
        assertEquals("b", N.median(new String[] { "a", "b", "c", "d" }, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMedianArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.median(new int[] {}));
    }

    @Test
    public void testMedianArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.median((int[]) null));
    }

    @Test
    public void testMedianCollection() {
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(2), N.median(Arrays.asList(1, 2, 3, 4), 0, 4, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargest() {
        assertEquals('d', N.kthLargest(new char[] { 'a', 'b', 'c', 'd', 'e' }, 2));
        assertEquals((byte) 4, N.kthLargest(new byte[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals((short) 4, N.kthLargest(new short[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals(4, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 2));
        assertEquals(4L, N.kthLargest(new long[] { 1L, 2L, 3L, 4L, 5L }, 2));
        assertEquals(4.0f, N.kthLargest(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2), 0.001f);
        assertEquals(4.0, N.kthLargest(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2), 0.001);
        assertEquals("d", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 2));
        assertEquals("d", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 2, Comparator.naturalOrder()));

        assertEquals(5, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 1));

        assertEquals(1, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 5));

        assertEquals('c', N.kthLargest(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4, 2));
        assertEquals((byte) 3, N.kthLargest(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals((short) 3, N.kthLargest(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals(3, N.kthLargest(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertEquals(3L, N.kthLargest(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2));
        assertEquals(3.0f, N.kthLargest(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2), 0.001f);
        assertEquals(3.0, N.kthLargest(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2), 0.001);
        assertEquals("c", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2));
        assertEquals("c", N.kthLargest(new String[] { "a", "b", "c", "d", "e" }, 1, 4, 2, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargestArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] {}, 1));
    }

    @Test
    public void testKthLargestInvalidK() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new int[] { 1, 2, 3 }, 4));
    }

    @Test
    public void testKthLargestCollection() {
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 2));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 2));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 2, Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(4), N.kthLargest(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 2, Comparator.naturalOrder()));
    }

    @Test
    public void testTopShort() {
        assertArrayEquals(new short[] { 4, 5 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new short[] { 2, 1 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new short[] { 3, 4 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertArrayEquals(new short[] { 3, 2 }, N.top(new short[] { 1, 2, 3, 4, 5 }, 1, 4, 2, Comparator.reverseOrder()));

        assertArrayEquals(new short[] { 1, 2, 3 }, N.top(new short[] { 1, 2, 3 }, 5));

        assertArrayEquals(new short[] {}, N.top(new short[] { 1, 2, 3 }, 0));

        assertArrayEquals(new short[] {}, N.top((short[]) null, 2));
    }

    @Test
    public void testTopInt() {
        assertArrayEquals(new int[] { 4, 5 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 2));
        assertArrayEquals(new int[] { 2, 1 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new int[] { 3, 4 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2));
        assertArrayEquals(new int[] { 3, 2 }, N.top(new int[] { 1, 2, 3, 4, 5 }, 1, 4, 2, Comparator.reverseOrder()));
    }

    @Test
    public void testTopLong() {
        assertArrayEquals(new long[] { 4L, 5L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 2));
        assertArrayEquals(new long[] { 2L, 1L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 2, Comparator.reverseOrder()));
        assertArrayEquals(new long[] { 3L, 4L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2));
        assertArrayEquals(new long[] { 3L, 2L }, N.top(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4, 2, Comparator.reverseOrder()));
    }

    @Test
    public void testTopFloat() {
        assertArrayEquals(new float[] { 4.0f, 5.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2), 0.001f);
        assertArrayEquals(new float[] { 2.0f, 1.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 2, Comparator.reverseOrder()), 0.001f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2), 0.001f);
        assertArrayEquals(new float[] { 3.0f, 2.0f }, N.top(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 1, 4, 2, Comparator.reverseOrder()), 0.001f);
    }

    @Test
    public void testTopDouble() {
        assertArrayEquals(new double[] { 4.0, 5.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2), 0.001);
        assertArrayEquals(new double[] { 2.0, 1.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 2, Comparator.reverseOrder()), 0.001);
        assertArrayEquals(new double[] { 3.0, 4.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2), 0.001);
        assertArrayEquals(new double[] { 3.0, 2.0 }, N.top(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4, 2, Comparator.reverseOrder()), 0.001);
    }

    @Test
    public void testTopCollection() {
        assertEquals(Arrays.asList(4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 2));
        assertEquals(Arrays.asList(2, 1), N.top(Arrays.asList(1, 2, 3, 4, 5), 2, Comparator.reverseOrder()));
        assertEquals(Arrays.asList(3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 2));
        assertEquals(Arrays.asList(3, 2), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 2, Comparator.reverseOrder()));

        assertEquals(Arrays.asList(3, 4, 5), N.top(Arrays.asList(1, 2, 3, null, 4, 5), 3));
        assertEquals(Arrays.asList(3, 4, 5), N.top(CommonUtil.toLinkedHashSet(1, 2, 3, null, 4, 5), 3));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 6, Comparator.reverseOrder()));
        assertEquals(Arrays.asList(2, 3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 6, Comparator.reverseOrder()));

        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 2, Comparator.naturalOrder(), true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, true));
        assertEquals(Arrays.asList(5, 4), N.top(Arrays.asList(1, 5, 3, 4, 2), 1, 4, 2, Comparator.naturalOrder(), true));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), N.top(Arrays.asList(1, 2, 3, 4, 5), 0, 5, 6, Comparator.reverseOrder(), true));
        assertEquals(Arrays.asList(2, 3, 4), N.top(Arrays.asList(1, 2, 3, 4, 5), 1, 4, 6, Comparator.reverseOrder(), true));
    }

    @Test
    public void testTopNegativeN() {
        assertThrows(IllegalArgumentException.class, () -> N.top(new int[] { 1, 2, 3 }, -1));
    }

    @Test
    public void testPercentilesChar() {
        char[] sorted = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        Map<Percentage, Character> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
        assertTrue(percentiles.containsKey(Percentage._0_1));
        assertTrue(percentiles.containsKey(Percentage._1));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._99));
    }

    @Test
    public void testPercentilesByte() {
        byte[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Byte> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesShort() {
        short[] sorted = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Map<Percentage, Short> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesInt() {
        int[] sorted = new int[100];
        for (int i = 0; i < 100; i++) {
            sorted[i] = i + 1;
        }
        Map<Percentage, Integer> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
        assertEquals(Integer.valueOf(2), percentiles.get(Percentage._1));
        assertEquals(Integer.valueOf(51), percentiles.get(Percentage._50));
        assertEquals(Integer.valueOf(100), percentiles.get(Percentage._99));
    }

    @Test
    public void testPercentilesLong() {
        long[] sorted = { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L };
        Map<Percentage, Long> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesFloat() {
        float[] sorted = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f };
        Map<Percentage, Float> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesDouble() {
        double[] sorted = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 };
        Map<Percentage, Double> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesGeneric() {
        String[] sorted = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };
        Map<Percentage, String> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesList() {
        List<Integer> sorted = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Map<Percentage, Integer> percentiles = N.percentilesOfSorted(sorted);
        assertNotNull(percentiles);
        assertEquals(Percentage.values().length, percentiles.size());
    }

    @Test
    public void testPercentilesEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new int[] {}));
    }

    @Test
    public void testPercentilesNullArray() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted((int[]) null));
    }

    @Test
    public void testPercentilesEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.emptyList()));
    }

    @Test
    public void testLen() {
        assertEquals(5, CommonUtil.len(new int[] { 1, 2, 3, 4, 5 }));
        assertEquals(0, CommonUtil.len(new int[] {}));
        assertEquals(0, CommonUtil.len((int[]) null));

        assertEquals(3, CommonUtil.len(new String[] { "a", "b", "c" }));
        assertEquals(0, CommonUtil.len(new String[] {}));
        assertEquals(0, CommonUtil.len((String[]) null));
    }

    @Test
    public void testSize() {
        assertEquals(5, CommonUtil.size(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(0, CommonUtil.size(Collections.emptyList()));
        assertEquals(0, CommonUtil.size((Collection<?>) null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, CommonUtil.size(map));
        assertEquals(0, CommonUtil.size(Collections.emptyMap()));
        assertEquals(0, CommonUtil.size((Map<?, ?>) null));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(CommonUtil.isEmpty(new int[] {}));
        assertTrue(CommonUtil.isEmpty((int[]) null));
        assertFalse(CommonUtil.isEmpty(new int[] { 1 }));

        assertTrue(CommonUtil.isEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.isEmpty((Collection<?>) null));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1)));

        assertTrue(CommonUtil.isEmpty(Collections.emptyMap()));
        assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(CommonUtil.isEmpty(map));

        assertTrue(CommonUtil.isEmpty(""));
        assertTrue(CommonUtil.isEmpty((String) null));
        assertFalse(CommonUtil.isEmpty("a"));
    }

    @Test
    public void testNotEmpty() {
        assertFalse(CommonUtil.notEmpty(new int[] {}));
        assertFalse(CommonUtil.notEmpty((int[]) null));
        assertTrue(CommonUtil.notEmpty(new int[] { 1 }));

        assertFalse(CommonUtil.notEmpty(Collections.emptyList()));
        assertFalse(CommonUtil.notEmpty((Collection<?>) null));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1)));

        assertFalse(CommonUtil.notEmpty(Collections.emptyMap()));
        assertFalse(CommonUtil.notEmpty((Map<?, ?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertTrue(CommonUtil.notEmpty(map));

        assertFalse(CommonUtil.notEmpty(""));
        assertFalse(CommonUtil.notEmpty((String) null));
        assertTrue(CommonUtil.notEmpty("a"));
    }

    @Test
    public void testCopyOfRange() {
        assertArrayEquals(new int[] { 2, 3, 4 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertArrayEquals(new long[] { 2L, 3L, 4L }, CommonUtil.copyOfRange(new long[] { 1L, 2L, 3L, 4L, 5L }, 1, 4));
        assertArrayEquals(new double[] { 2.0, 3.0, 4.0 }, CommonUtil.copyOfRange(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 4), 0.001);
        assertArrayEquals(new String[] { "b", "c", "d" }, CommonUtil.copyOfRange(new String[] { "a", "b", "c", "d", "e" }, 1, 4));

        assertArrayEquals(new int[] {}, CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 1, 1));
        assertArrayEquals(new int[] { 1, 2, 3 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 0, 3));
    }

    @Test
    public void testClone() {
        int[] intArr = { 1, 2, 3 };
        int[] intClone = CommonUtil.clone(intArr);
        assertArrayEquals(intArr, intClone);
        assertNotSame(intArr, intClone);

        assertNull(CommonUtil.clone((int[]) null));

        String[] strArr = { "a", "b", "c" };
        String[] strClone = CommonUtil.clone(strArr);
        assertArrayEquals(strArr, strClone);
        assertNotSame(strArr, strClone);
    }

    @Test
    public void testEquals() {
        assertTrue(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
        assertTrue(CommonUtil.equals((int[]) null, (int[]) null));
        assertFalse(CommonUtil.equals(new int[] { 1 }, null));
        assertFalse(CommonUtil.equals(null, new int[] { 1 }));

        assertTrue(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "a", "c" }));

        assertTrue(CommonUtil.equals(new String[] { null, "a" }, new String[] { null, "a" }));
        assertFalse(CommonUtil.equals(new String[] { null, "a" }, new String[] { "a", null }));

        assertTrue(CommonUtil.equals("abc", "abc"));
        assertFalse(CommonUtil.equals("abc", "def"));
        assertTrue(CommonUtil.equals((String) null, (String) null));
        assertFalse(CommonUtil.equals("abc", null));
        assertFalse(CommonUtil.equals(null, "abc"));

        assertTrue(CommonUtil.equals(1.0, 1.0));
        assertFalse(CommonUtil.equals(1.0, 1.1));
        assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
        assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));
    }

    @Test
    public void testCompare() {
        assertEquals(0, CommonUtil.compare(1, 1));
        assertTrue(CommonUtil.compare(1, 2) < 0);
        assertTrue(CommonUtil.compare(2, 1) > 0);

        assertEquals(0, CommonUtil.compare(1.0, 1.0));
        assertTrue(CommonUtil.compare(1.0, 2.0) < 0);
        assertTrue(CommonUtil.compare(2.0, 1.0) > 0);

        assertEquals(0, CommonUtil.compare("a", "a"));
        assertTrue(CommonUtil.compare("a", "b") < 0);
        assertTrue(CommonUtil.compare("b", "a") > 0);

        assertEquals(0, CommonUtil.compare((String) null, (String) null));
        assertTrue(CommonUtil.compare(null, "a") < 0);
        assertTrue(CommonUtil.compare("a", null) > 0);
    }

    @Test
    public void testHashCode() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

        assertEquals(CommonUtil.hashCode(arr1), CommonUtil.hashCode(arr2));
        assertNotEquals(CommonUtil.hashCode(arr1), CommonUtil.hashCode(arr3));

        assertEquals(CommonUtil.hashCode("abc"), CommonUtil.hashCode("abc"));
        assertNotEquals(CommonUtil.hashCode("abc"), CommonUtil.hashCode("def"));

        assertEquals(0, CommonUtil.hashCode((String) null));
    }

    @Test
    public void testNewArray() {
        String[] strArray = CommonUtil.newArray(String.class, 5);
        assertNotNull(strArray);
        assertEquals(5, strArray.length);

        Integer[] intArray = CommonUtil.newArray(Integer.class, 3);
        assertNotNull(intArray);
        assertEquals(3, intArray.length);

        Object[] emptyArray = CommonUtil.newArray(Object.class, 0);
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToList() {
        List<Integer> list = CommonUtil.toList(new Integer[] { 1, 2, 3 });
        assertEquals(Arrays.asList(1, 2, 3), list);

        list = CommonUtil.toList(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), list);

        list = CommonUtil.toList(new Integer[] {});
        assertEquals(Collections.emptyList(), list);

        list = CommonUtil.toList((Integer[]) null);
        assertEquals(Collections.emptyList(), list);
    }

    @Test
    public void testToSet() {
        Set<Integer> set = CommonUtil.toSet(new Integer[] { 1, 2, 3, 2, 1 });
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);

        set = CommonUtil.toSet(new Integer[] {});
        assertEquals(Collections.emptySet(), set);

        set = CommonUtil.toSet((Integer[]) null);
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void testNewHashSet() {
        Set<String> set = CommonUtil.newHashSet(10);
        assertNotNull(set);
        assertTrue(set instanceof HashSet);
        assertEquals(0, set.size());

        set = CommonUtil.newHashSet(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testNewLinkedHashMap() {
        Map<String, Integer> map = CommonUtil.newLinkedHashMap(10);
        assertNotNull(map);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(0, map.size());
    }

    @Test
    public void testCheckArgNotNull() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "arg"));
    }

    @Test
    public void testCheckArgNotNullValid() {
        CommonUtil.checkArgNotNull("valid", "arg");
    }

    @Test
    public void testCheckArgNotEmptyNull() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String) null, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyEmpty() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "arg"));
    }

    @Test
    public void testCheckArgNotEmptyArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[] {}, "arg"));
    }

    @Test
    public void testCheckArgNotEmptyValid() {
        CommonUtil.checkArgNotEmpty("valid", "arg");
        CommonUtil.checkArgNotEmpty(new int[] { 1 }, "arg");
    }

    @Test
    public void testCheckArgNotNegative() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "arg"));
    }

    @Test
    public void testCheckArgNotNegativeValid() {
        CommonUtil.checkArgNotNegative(0, "arg");
        CommonUtil.checkArgNotNegative(1, "arg");
    }

    @Test
    public void testCheckArgument() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Invalid argument"));
    }

    @Test
    public void testCheckArgumentValid() {
        CommonUtil.checkArgument(true, "Valid argument");
    }

    @Test
    public void testCheckFromToIndexInvalidFrom() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 2, 5));
    }

    @Test
    public void testCheckFromToIndexInvalidTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 6, 5));
    }

    @Test
    public void testCheckFromToIndexInvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(3, 2, 5));
    }

    @Test
    public void testCheckFromToIndexValid() {
        CommonUtil.checkFromToIndex(0, 5, 5);
        CommonUtil.checkFromToIndex(1, 3, 5);
        CommonUtil.checkFromToIndex(2, 2, 5);
    }

    @Test
    public void testNaNHandling() {
        assertEquals(Float.NaN, N.min(1.0f, Float.NaN), 0.001f);
        assertEquals(Float.NaN, N.max(1.0f, Float.NaN), 0.001f);
        assertEquals(Double.NaN, N.min(1.0, Double.NaN), 0.001);
        assertEquals(Double.NaN, N.max(1.0, Double.NaN), 0.001);

        assertEquals(1.0f, N.min(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
        assertEquals(3.0f, N.max(new float[] { 1.0f, Float.NaN, 3.0f }), 0.001f);
    }

    @Test
    public void testComparatorEdgeCases() {
        Comparator<Integer> reverseComp = Comparator.reverseOrder();
        assertEquals(Integer.valueOf(3), N.min(new Integer[] { 1, 2, 3 }, reverseComp));
        assertEquals(Integer.valueOf(1), N.max(new Integer[] { 1, 2, 3 }, reverseComp));

        assertEquals(Integer.valueOf(1), N.min(new Integer[] { 1, 2, 3 }, null));
        assertEquals(Integer.valueOf(3), N.max(new Integer[] { 1, 2, 3 }, null));
    }

    @Test
    public void testLargeArrayPerformance() {
        int[] largeArray = new int[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i;
        }

        assertEquals(9999, N.kthLargest(largeArray, 1));
        assertEquals(5000, N.kthLargest(largeArray, 5000));
        assertEquals(0, N.kthLargest(largeArray, 10000));

        int[] top100 = N.top(largeArray, 100);
        assertEquals(100, top100.length);
        assertEquals(9900, top100[0]);
    }

    @Test
    public void testThreadSafety() {
        final int[] array = { 1, 2, 3, 4, 5 };
        final int iterations = 1000;
        final int threadCount = 10;

        Thread[] threads = new Thread[threadCount];
        final boolean[] errors = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        assertEquals(1, N.min(array));
                        assertEquals(5, N.max(array));
                        assertEquals(3, N.median(array));
                        assertEquals(15, N.sum(array));
                        assertEquals(3.0, N.average(array), 0.001);
                    }
                } catch (Exception e) {
                    errors[threadIndex] = true;
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Thread interrupted");
            }
        }

        for (boolean error : errors) {
            assertFalse(error, "Thread safety issue detected");
        }
    }

    @Test
    public void testFilterCollectionWithRangeAndSupplier() {
        List<String> list = Arrays.asList("a", "b", "aa", "bb", "aaa");
        Set<String> resultSet = N.filter(list, 1, 5, s -> s.contains("a") && s.length() > 1, HashSet::new);
        assertEquals(Set.of("aa", "aaa"), resultSet);

        resultSet = N.filter(CommonUtil.newLinkedHashSet(list), 1, 5, s -> s.contains("a") && s.length() > 1, HashSet::new);
        assertEquals(Set.of("aa", "aaa"), resultSet);
        assertTrue(N.filter(list, 1, 1, STRING_NOT_EMPTY, ArrayList::new).isEmpty());
    }

    @Test
    public void testMapToCollection() {
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new char[] { 'h', 'w' }, N.mapToChar(list, s -> s.charAt(0)));

            assertArrayEquals(new char[] { 'h', 'w' }, N.mapToChar(CommonUtil.newLinkedHashSet(list), s -> s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new byte[] { 104, 119 }, N.mapToByte(list, s -> (byte) s.charAt(0)));
            assertArrayEquals(new byte[] { 104, 119 }, N.mapToByte(CommonUtil.newLinkedHashSet(list), s -> (byte) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new short[] { 104, 119 }, N.mapToShort(list, s -> (short) s.charAt(0)));
            assertArrayEquals(new short[] { 104, 119 }, N.mapToShort(CommonUtil.newLinkedHashSet(list), s -> (short) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new int[] { 104, 119 }, N.mapToInt(list, s -> (int) s.charAt(0)));
            assertArrayEquals(new int[] { 104, 119 }, N.mapToInt(CommonUtil.newLinkedHashSet(list), s -> (int) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new long[] { 104L, 119L }, N.mapToLong(list, s -> (long) s.charAt(0)));
            assertArrayEquals(new long[] { 104L, 119L }, N.mapToLong(CommonUtil.newLinkedHashSet(list), s -> (long) s.charAt(0)));
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new float[] { 104.0f, 119.0f }, N.mapToFloat(list, s -> (float) s.charAt(0)), 0.001f);
            assertArrayEquals(new float[] { 104.0f, 119.0f }, N.mapToFloat(CommonUtil.newLinkedHashSet(list), s -> (float) s.charAt(0)), 0.001f);
        }
        {
            List<String> list = Arrays.asList("hello", "world");
            assertArrayEquals(new double[] { 104.0, 119.0 }, N.mapToDouble(list, s -> (double) s.charAt(0)), 0.001);
            assertArrayEquals(new double[] { 104.0, 119.0 }, N.mapToDouble(CommonUtil.newLinkedHashSet(list), s -> (double) s.charAt(0)), 0.001);
        }
    }

    @Test
    public void testZipIterablesWithDefaults3() {

        {
            Iterable<String> a = CommonUtil.toList("x", "y", "z");
            Iterable<String> b = CommonUtil.toList("a", "b");
            Iterable<String> c = CommonUtil.toList("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = CommonUtil.toList("a", "b");
            Iterable<String> c = CommonUtil.toList("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = createIterable("a", "b");
            Iterable<String> c = CommonUtil.toList("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = createIterable("a", "b");
            Iterable<String> c = createIterable("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = createIterable("x", "y", "z");
            Iterable<String> b = CommonUtil.toList("a", "b");
            Iterable<String> c = createIterable("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

        {
            Iterable<String> a = CommonUtil.toList("x", "y", "z");
            Iterable<String> b = CommonUtil.toList("a", "b");
            Iterable<String> c = createIterable("1");
            List<String> result = N.zip(a, b, c, "X", "A", "0", (s, i, j) -> s + i + j);
            assertEquals(List.of("xa1", "yb0", "zA0"), result);
        }

    }

    @Test
    public void testGroupByCollectionWithRangeAndMapSupplier() {
        {
            List<String> list = Arrays.asList("one", "two", "three", "four", "five");
            TreeMap<Character, List<String>> result = N.groupBy(list, 1, 4, s -> s.charAt(0), TreeMap::new);
            assertEquals(List.of("four"), result.get('f'));
            assertEquals(List.of("two", "three"), result.get('t'));
        }
        {
            List<String> list = CommonUtil.toLinkedList("one", "two", "three", "four", "five");
            TreeMap<Character, List<String>> result = N.groupBy(list, 1, 4, s -> s.charAt(0), TreeMap::new);
            assertEquals(List.of("four"), result.get('f'));
            assertEquals(List.of("two", "three"), result.get('t'));
        }
    }

    @Test
    public void forEach_iterables_triConsumer_shortCircuit() throws Exception {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<Integer> l2 = Arrays.asList(1, 2, 3, 4);
        List<Boolean> l3 = Arrays.asList(true, false);
        List<String> result = new ArrayList<>();
        N.forEach(l1, l2, l3, (s, i, bool) -> result.add(s + i + bool));

        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void test_forEach_01() throws Exception {
        {
            String[] a = CommonUtil.asArray("a", "b", "c");
            String[] b = CommonUtil.asArray("1", "2", "3", "4");
            List<String> result = new ArrayList<>();
            N.forEach(a, b, "X", "0", (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), result);
        }
        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<String> result = new ArrayList<>();
            N.forEach(a, b, "X", "0", (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), result);
        }
        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<String> result = new ArrayList<>();
            N.forEach(a.iterator(), b.iterator(), "X", "0", (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("a1", "b2", "c3", "X4"), result);
        }

        {
            String[] a = CommonUtil.asArray("a", "b", "c");
            String[] b = CommonUtil.asArray("1", "2", "3", "4");
            Boolean[] c = CommonUtil.asArray(true, false);
            List<String> result = new ArrayList<>();
            N.forEach(a, b, c, "X", "0", false, (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), result);
        }

        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<Boolean> c = CommonUtil.toList(true, false);
            List<String> result = new ArrayList<>();
            N.forEach(a, b, c, "X", "0", false, (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), result);
        }

        {
            List<String> a = Arrays.asList("a", "b", "c");
            List<String> b = Arrays.asList("1", "2", "3", "4");
            List<Boolean> c = CommonUtil.toList(true, false);
            List<String> result = new ArrayList<>();
            N.forEach(a.iterator(), b.iterator(), c.iterator(), "X", "0", false, (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("a1true", "b2false", "c3false", "X4false"), result);
        }
    }

    @Test
    public void test_forEachNonNull() throws Exception {
        {
            String[] a = CommonUtil.asArray("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> result.add(e));

            assertEquals(Arrays.asList("a", "b", "c"), result);
        }
        {
            String[] a = CommonUtil.asArray("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.toList(e, e), (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("aa", "aa", "bb", "bb", "cc", "cc"), result);
        }
        {
            String[] a = CommonUtil.asArray("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.toList(e, e), e -> CommonUtil.toList(e), (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("aaa", "aaa", "bbb", "bbb", "ccc", "ccc"), result);
        }
        {
            List<String> a = CommonUtil.toList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> result.add(e));

            assertEquals(Arrays.asList("a", "b", "c"), result);
        }
        {
            List<String> a = CommonUtil.toList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.toList(e, e), (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("aa", "aa", "bb", "bb", "cc", "cc"), result);
        }
        {
            List<String> a = CommonUtil.toList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a, e -> CommonUtil.toList(e, e), e -> CommonUtil.toList(e), (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("aaa", "aaa", "bbb", "bbb", "ccc", "ccc"), result);
        }
        {
            List<String> a = CommonUtil.toList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a.iterator(), e -> result.add(e));

            assertEquals(Arrays.asList("a", "b", "c"), result);
        }
        {
            List<String> a = CommonUtil.toList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a.iterator(), e -> CommonUtil.toList(e, e), (s, i) -> result.add(s + i));

            assertEquals(Arrays.asList("aa", "aa", "bb", "bb", "cc", "cc"), result);
        }
        {
            List<String> a = CommonUtil.toList("a", null, "b", null, "c");
            List<String> result = new ArrayList<>();
            N.forEachNonNull(a.iterator(), e -> CommonUtil.toList(e, e), e -> CommonUtil.toList(e), (s, i, j) -> result.add(s + i + j));

            assertEquals(Arrays.asList("aaa", "aaa", "bbb", "bbb", "ccc", "ccc"), result);
        }
    }

    @Test
    public void testToJson() {
        TestPerson person = new TestPerson("John", 30);
        String json = N.toJson(person);
        assertNotNull(json);
        assertTrue(json.contains("\"name\": \"John\""));
        assertTrue(json.contains("\"age\": 30"));
    }

    @Test
    public void testToJsonWithPrettyFormat() {
        TestPerson person = new TestPerson("John", 30);
        String json = N.toJson(person, true);
        assertNotNull(json);
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"name\": \"John\""));
    }

    @Test
    public void testToJsonWithConfig() {
        TestPerson person = new TestPerson("John", 30);
        JsonSerConfig config = JsonSerConfig.create();
        String json = N.toJson(person, config);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        File outputFile = new File(tempDir, "test.json");
        N.toJson(person, outputFile);
        assertTrue(outputFile.exists());
        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("John"));

        IOUtil.deleteIfExists(outputFile);
    }

    @Test
    public void testToJsonToOutputStream() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toJson(person, baos);
        String json = baos.toString();
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToWriter() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        StringWriter writer = new StringWriter();
        N.toJson(person, writer);
        String json = writer.toString();
        assertTrue(json.contains("John"));
    }

    @Test
    public void testFromJsonString() {
        TestPerson person = N.fromJson(TEST_JSON, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void testFromJsonStringWithType() {
        Type<List<Integer>> type = new TypeReference<List<Integer>>() {
        }.type();
        List<Integer> list = N.fromJson("[1,2,3]", type);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testFromJsonStringWithDefault() {
        TestPerson defaultPerson = new TestPerson("Default", 0);
        TestPerson person = N.fromJson(null, defaultPerson, TestPerson.class);
        assertEquals(defaultPerson, person);
    }

    @Test
    public void testFromJsonFile() throws IOException {
        File jsonFile = new File(tempDir, "test.json");
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(TEST_JSON);
        }
        TestPerson person = N.fromJson(jsonFile, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());

        IOUtil.deleteIfExists(jsonFile);
    }

    @Test
    public void testFromJsonInputStream() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_JSON.getBytes(StandardCharsets.UTF_8));
        TestPerson person = N.fromJson(bais, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonReader() throws IOException {
        StringReader reader = new StringReader(TEST_JSON);
        TestPerson person = N.fromJson(reader, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonSubstring() {
        String json = "prefix" + TEST_JSON + "suffix";
        TestPerson person = N.fromJson(json, 6, 6 + TEST_JSON.length(), TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
    }

    @Test
    public void testStreamJson() {
        assertThrows(IllegalArgumentException.class, () -> N.streamJson(TEST_JSON_ARRAY, Type.of(Integer.class)));
    }

    @Test
    public void testFormatJson() {
        String compactJson = "{\"a\":1,\"b\":2}";
        String formatted = N.formatJson(compactJson);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    public void testToXml() {
        TestPerson person = new TestPerson("John", 30);
        String xml = N.toXml(person);
        assertNotNull(xml);
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<age>30</age>"));
    }

    @Test
    public void testToXmlWithPrettyFormat() {
        TestPerson person = new TestPerson("John", 30);
        String xml = N.toXml(person, true);
        assertNotNull(xml);
        assertTrue(xml.contains("\n"));
    }

    @Test
    public void testFromXmlString() {
        TestPerson person = N.fromXml(TEST_XML, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void testFormatXml() {
        String compactXml = "<root><a>1</a><b>2</b></root>";
        String formatted = N.formatXml(compactXml);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    public void testXml2Json() {
        String json = N.xmlToJson(TEST_XML);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void testJson2Xml() {
        String xml = N.jsonToXml(TEST_JSON);
        assertNotNull(xml);
        assertTrue(xml.contains("John"));
    }

    @Test
    public void testForEachIntRange() {
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(1, 5, sum::addAndGet);
        assertEquals(10, sum.get());
    }

    @Test
    public void testForEachIntRangeWithStep() {
        List<Integer> values = new ArrayList<>();
        N.forEach(0, 10, 2, values::add);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), values);
    }

    @Test
    public void testForEachArray() {
        String[] array = { "a", "b", "c" };
        List<String> result = new ArrayList<>();
        N.forEach(array, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testForEachArrayWithRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEach(array, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);
    }

    @Test
    public void testForEachIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list, result::add);
        assertEquals(list, result);
    }

    @Test
    public void testForEachIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list.iterator(), result::add);
        assertEquals(list, result);
    }

    @Test
    public void testForEachMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(map, (k, v) -> sum.addAndGet(v));
        assertEquals(3, sum.get());
    }

    @Test
    public void testForEachParallel() throws Exception {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ConcurrentLinkedQueue<Integer> result = new ConcurrentLinkedQueue<>();
        N.forEach(list, result::add, 2);
        assertEquals(5, result.size());
        assertTrue(result.containsAll(list));
    }

    @Test
    public void testForEachWithFlatMapper() {
        {
            String[] array = { "ab", "cd", "ef" };
            List<Character> result = new ArrayList<>();
            N.forEach(array, s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
            assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);
        }
        {
            List<String> list = CommonUtil.toList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list, s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
            assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);
        }
        {
            List<String> list = CommonUtil.toList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list.iterator(), s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
            assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);
        }
    }

    @Test
    public void testForEachWithFlatMapper3() {
        {
            String[] array = { "ab", "cd", "ef" };
            List<Character> result = new ArrayList<>();
            N.forEach(array, s -> Arrays.asList(s.charAt(0), s.charAt(1)), e -> CommonUtil.toList(e, e), (s, c, x) -> result.add(c));
            assertEquals(Arrays.asList('a', 'a', 'b', 'b', 'c', 'c', 'd', 'd', 'e', 'e', 'f', 'f'), result);
        }
        {
            List<String> list = CommonUtil.toList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list, s -> Arrays.asList(s.charAt(0), s.charAt(1)), e -> CommonUtil.toList(e, e), (s, c, x) -> result.add(c));
            assertEquals(Arrays.asList('a', 'a', 'b', 'b', 'c', 'c', 'd', 'd', 'e', 'e', 'f', 'f'), result);
        }
        {
            List<String> list = CommonUtil.toList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list.iterator(), s -> Arrays.asList(s.charAt(0), s.charAt(1)), e -> CommonUtil.toList(e, e), (s, c, x) -> result.add(c));
            assertEquals(Arrays.asList('a', 'a', 'b', 'b', 'c', 'c', 'd', 'd', 'e', 'e', 'f', 'f'), result);
        }
    }

    @Test
    public void testForEachPair() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<String> result = new ArrayList<>();
        N.forEachPair(array, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4", "4-5"), result);
    }

    @Test
    public void testForEachTriple() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<String> result = new ArrayList<>();
        N.forEachTriple(array, (a, b, c) -> result.add(a + "-" + (b != null ? b : "null") + "-" + (c != null ? c : "null")));
        assertEquals(Arrays.asList("1-2-3", "2-3-4", "3-4-5"), result);
    }

    @Test
    public void testForEachIndexed() {
        String[] array = { "a", "b", "c" };
        Map<Integer, String> result = new HashMap<>();
        N.forEachIndexed(array, result::put);
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testForEachNonNull() {
        String[] array = { "a", null, "b", null, "c" };
        List<String> result = new ArrayList<>();
        N.forEachNonNull(array, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testExecute() {
        AtomicInteger counter = new AtomicInteger(0);
        N.runWithRetry(counter::incrementAndGet, 2, 10, e -> e instanceof RuntimeException);
        assertEquals(1, counter.get());
    }

    @Test
    public void testExecuteCallable() {
        Callable<String> callable = () -> "success";
        String result = N.callWithRetry(callable, 2, 10, (r, e) -> e != null);
        assertEquals("success", result);
    }

    @Test
    public void testAsyncExecute() throws Exception {
        ContinuableFuture<Void> future = N.asyncExecute(() -> Thread.sleep(10));
        assertNotNull(future);
        future.get();
    }

    @Test
    public void testAsyncExecuteCallable() throws Exception {
        ContinuableFuture<String> future = N.asyncExecute(() -> "result");
        assertEquals("result", future.get());
    }

    @Test
    public void testAsyncExecuteWithDelay() throws Exception {
        long start = System.currentTimeMillis();
        ContinuableFuture<Void> future = N.asyncExecute(() -> {
        }, 50);
        future.get();
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testAsyncExecuteList() throws Exception {
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(() -> Thread.sleep(10), () -> Thread.sleep(10), () -> Thread.sleep(10));
        List<ContinuableFuture<Void>> futures = N.asyncExecute(commands);
        assertEquals(3, futures.size());
        for (ContinuableFuture<Void> future : futures) {
            future.get();
        }
    }

    @Test
    public void testAsynRun() {
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(() -> {
        }, () -> {
        }, () -> {
        });
        ObjIterator<Void> iter = N.asyncRun(commands);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testAsynCall() {
        List<Callable<Integer>> commands = Arrays.asList(() -> 1, () -> 2, () -> 3);
        ObjIterator<Integer> iter = N.asyncCall(commands);
        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }
        assertEquals(3, results.size());
        assertTrue(results.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testRunInParallel() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        N.runInParallel(counter1::incrementAndGet, counter2::incrementAndGet);
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    public void testCallInParallel() {
        Tuple2<String, Integer> result = N.callInParallel(() -> "hello", () -> 42);
        assertEquals("hello", result._1);
        assertEquals(42, result._2);
    }

    @Test
    public void testRunByBatch() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<Integer> sums = new ArrayList<>();
        N.runByBatch(array, 3, batch -> sums.add(batch.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(Arrays.asList(6, 15, 24, 10), sums);
    }

    @Test
    public void testCallByBatch() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> results = N.callByBatch(list, 3, batch -> batch.size());
        assertEquals(Arrays.asList(3, 3, 3, 1), results);
    }

    @Test
    public void testRunUninterruptibly() {
        AtomicInteger counter = new AtomicInteger(0);
        N.runUninterruptibly(() -> {
            counter.incrementAndGet();
            Thread.sleep(10);
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void testCallUninterruptibly() throws Exception {
        String result = N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return "success";
        });
        assertEquals("success", result);
    }

    @Test
    public void testTryOrEmptyIfExceptionOccurred() {
        Nullable<String> result1 = N.tryOrEmptyIfExceptionOccurred(() -> "success");
        assertTrue(result1.isPresent());
        assertEquals("success", result1.get());

        Nullable<String> result2 = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new RuntimeException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred() {
        String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "success", "default");
        assertEquals("success", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
            throw new RuntimeException();
        }, "default");
        assertEquals("default", result2);
    }

    @Test
    public void testIfOrEmpty() {
        Nullable<String> result1 = N.ifOrEmpty(true, () -> "value");
        assertTrue(result1.isPresent());
        assertEquals("value", result1.get());

        Nullable<String> result2 = N.ifOrEmpty(false, () -> "value");
        assertFalse(result2.isPresent());
    }

    @Test
    public void testIfOrElse() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);

        N.ifOrElse(true, counter1::incrementAndGet, counter2::incrementAndGet);
        assertEquals(1, counter1.get());
        assertEquals(0, counter2.get());

        N.ifOrElse(false, counter1::incrementAndGet, counter2::incrementAndGet);
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    public void testIfNotNull() {
        AtomicInteger counter = new AtomicInteger(0);
        N.ifNotNull("value", v -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotNull(null, v -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testIfNotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        N.ifNotEmpty("hello", s -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotEmpty("", s -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotEmpty(Arrays.asList(1, 2, 3), c -> counter.incrementAndGet());
        assertEquals(2, counter.get());

        N.ifNotEmpty(Collections.emptyList(), c -> counter.incrementAndGet());
        assertEquals(2, counter.get());
    }

    @Test
    public void testSleep() {
        long start = System.currentTimeMillis();
        N.sleep(50);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testSleepWithTimeUnit() {
        long start = System.currentTimeMillis();
        N.sleep(50, TimeUnit.MILLISECONDS);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testSleepUninterruptibly() {
        long start = System.currentTimeMillis();
        N.sleepUninterruptibly(50);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testLazyInit() {
        AtomicInteger counter = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            counter.incrementAndGet();
            return "value";
        });

        assertEquals(0, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testToRuntimeException() {
        Exception checkedException = new Exception("test");
        RuntimeException runtimeException = N.toRuntimeException(checkedException);
        assertNotNull(runtimeException);
        assertEquals(checkedException, runtimeException.getCause());

        RuntimeException existingRuntimeException = new RuntimeException("test");
        RuntimeException result = N.toRuntimeException(existingRuntimeException);
        assertSame(existingRuntimeException, result);
    }

    @Test
    public void testPrintln() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            String result = N.println("test");
            assertEquals("test", result);
            assertTrue(baos.toString().contains("test"));

            baos.reset();
            List<String> list = Arrays.asList("a", "b", "c");
            List<String> listResult = N.println(list);
            assertSame(list, listResult);
            assertTrue(baos.toString().contains("[a, b, c]"));

            baos.reset();
            String[] array = { "x", "y", "z" };
            String[] arrayResult = N.println(array);
            assertSame(array, arrayResult);
            assertTrue(baos.toString().contains("[x, y, z]"));

            baos.reset();
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            Map<String, Integer> mapResult = N.println(map);
            assertSame(map, mapResult);
            String mapOutput = baos.toString();
            assertTrue(mapOutput.contains("{") && mapOutput.contains("}"));
            assertTrue(mapOutput.contains("a=1") || mapOutput.contains("b=2"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testFprintln() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.fprintln("Hello %s, you are %d years old", "John", 30);
            String output = baos.toString();
            assertTrue(output.contains("Hello John, you are 30 years old"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testForEachWithNullArray() {
        String[] nullArray = null;
        List<String> result = new ArrayList<>();
        N.forEach(nullArray, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithEmptyArray() {
        String[] emptyArray = new String[0];
        List<String> result = new ArrayList<>();
        N.forEach(emptyArray, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithNullIterable() {
        Iterable<String> nullIterable = null;
        List<String> result = new ArrayList<>();
        N.forEach(nullIterable, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithNullIterator() {
        Iterator<String> nullIterator = null;
        List<String> result = new ArrayList<>();
        N.forEach(nullIterator, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithEmptyMap() {
        Map<String, Integer> emptyMap = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        N.forEach(emptyMap, (k, v) -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testForEachRangeWithInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.forEach(0, 10, 0, i -> {
            });
        });
    }

    @Test
    public void testForEachRangeBackwards() {
        List<Integer> result = new ArrayList<>();
        N.forEach(10, 0, -2, result::add);
        assertEquals(Arrays.asList(10, 8, 6, 4, 2), result);
    }

    @Test
    public void testFromJsonWithInvalidJson() {
        assertThrows(Exception.class, () -> {
            N.fromJson("invalid json", TestPerson.class);
        });
    }

    @Test
    public void testFromXmlWithInvalidXml() {
        assertThrows(Exception.class, () -> {
            N.fromXml("invalid xml", TestPerson.class);
        });
    }

    @Test
    public void testRunByBatchWithInvalidBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.runByBatch(new Integer[] { 1, 2, 3 }, 0, batch -> {
            });
        });

        assertThrows(IllegalArgumentException.class, () -> {
            N.runByBatch(new Integer[] { 1, 2, 3 }, -1, batch -> {
            });
        });
    }

    @Test
    public void testAsyncExecuteWithNullExecutor() {
        assertThrows(NullPointerException.class, () -> {
            N.asyncExecute(() -> {
            }, null);
        });
    }

    @Test
    public void testSleepWithNullTimeUnit() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.sleep(100, null);
        });
    }

    @Test
    public void testForEachIndexedParallel() throws Exception {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ConcurrentHashMap<Integer, Integer> result = new ConcurrentHashMap<>();
        N.forEachIndexed(list, result::put, 3);

        assertEquals(10, result.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1, result.get(i));
        }
    }

    @Test
    public void testForEachWithTwoArrays() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "1", "2", "3" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, (a, b) -> result.add(a + b));
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testForEachWithThreeArrays() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "1", "2", "3" };
        String[] array3 = { "x", "y", "z" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, array3, (a, b, c) -> result.add(a + b + c));
        assertEquals(Arrays.asList("a1x", "b2y", "c3z"), result);
    }

    @Test
    public void testForEachWithArraysOfDifferentLengths() {
        String[] array1 = { "a", "b", "c", "d" };
        String[] array2 = { "1", "2" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, (a, b) -> result.add(a + b));
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testForEachWithDefaultValues() {
        String[] array1 = { "a", "b" };
        String[] array2 = { "1", "2", "3", "4" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, "X", "Y", (a, b) -> result.add(a + b));
        assertEquals(Arrays.asList("a1", "b2", "X3", "X4"), result);
    }

    @Test
    public void testCallInParallelWithCollection() {
        List<Callable<Integer>> tasks = Arrays.asList(() -> 1, () -> 2, () -> 3, () -> 4, () -> 5);
        List<Integer> results = N.callInParallel(tasks);
        assertEquals(5, results.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
    }

    @Test
    public void testRunInParallelWithCollection() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Throwables.Runnable<Exception>> tasks = Arrays.asList(counter::incrementAndGet, counter::incrementAndGet, counter::incrementAndGet);
        N.runInParallel(tasks);
        assertEquals(3, counter.get());
    }

    @Test
    public void testForEachPairWithStep() {
        Integer[] array = { 1, 2, 3, 4, 5, 6 };
        List<String> result = new ArrayList<>();
        N.forEachPair(array, 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("1-2", "3-4", "5-6"), result);
    }

    @Test
    public void testForEachTripleWithStep() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        List<String> result = new ArrayList<>();
        N.forEachTriple(array, 3, (a, b, c) -> result.add(a + "-" + (b != null ? b : "null") + "-" + (c != null ? c : "null")));
        assertEquals(Arrays.asList("1-2-3", "4-5-6", "7-8-9"), result);
    }

    @Test
    public void testAsyncExecuteWithRetry() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);
        ContinuableFuture<String> future = N.asyncExecute(() -> {
            attemptCount.incrementAndGet();
            if (attemptCount.get() < 3) {
                throw new RuntimeException("Retry");
            }
            return "success";
        }, 3, 10, (result, exception) -> exception != null);

        String result = future.get();
        assertEquals("success", result);
        assertEquals(3, attemptCount.get());
    }

    @Test
    public void testLazyInitialize() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(() -> {
            counter.incrementAndGet();
            return "value";
        });

        assertEquals(0, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testForEachCollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();

        N.forEach(list, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        result.clear();
        N.forEach(list, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);
    }

    @Test
    public void testRunByBatchWithElementConsumer() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<Integer> processedElements = new ArrayList<>();
        AtomicInteger batchCount = new AtomicInteger(0);

        N.runByBatch(array, 2, (idx, element) -> processedElements.add(element), batchCount::incrementAndGet);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processedElements);
        assertEquals(3, batchCount.get());
    }

    @Test
    public void testCallByBatchWithElementConsumer() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> processedElements = new ArrayList<>();

        List<String> results = N.callByBatch(list, 2, (idx, element) -> processedElements.add(element), () -> "batch-" + processedElements.size());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processedElements);
        assertEquals(Arrays.asList("batch-2", "batch-4", "batch-5"), results);
    }

    @Test
    public void testRunUninterruptiblyWithTimeout() {
        AtomicInteger counter = new AtomicInteger(0);
        N.runUninterruptibly(remainingMillis -> {
            counter.incrementAndGet();
            Thread.sleep(10);
        }, 100);
        assertEquals(1, counter.get());
    }

    @Test
    public void testCallUninterruptiblyWithTimeout() {
        String result = N.callUninterruptibly(remainingMillis -> {
            Thread.sleep(10);
            return "success-" + remainingMillis;
        }, 100);
        assertTrue(result.startsWith("success-"));
    }

    @Test
    public void testCallInParallelMultiple() {
        for (int i = 0; i < 100; i++) {
            Tuple3<String, Integer, Boolean> result3 = N.callInParallel(() -> "hello", () -> 42, () -> true);
            assertEquals("hello", result3._1);
            assertEquals(42, result3._2);
            assertEquals(true, result3._3);

            Tuple4<String, Integer, Boolean, Double> result4 = N.callInParallel(() -> "hello", () -> 42, () -> true, () -> 3.14);
            assertEquals("hello", result4._1);
            assertEquals(42, result4._2);
            assertEquals(true, result4._3);
            assertEquals(3.14, result4._4);

            Tuple5<String, Integer, Boolean, Double, Character> result5 = N.callInParallel(() -> "hello", () -> 42, () -> true, () -> 3.14, () -> 'x');
            assertEquals("hello", result5._1);
            assertEquals(42, result5._2);
            assertEquals(true, result5._3);
            assertEquals(3.14, result5._4);
            assertEquals('x', result5._5);
        }
    }

    @Test
    public void testRunInParallelMultiple() {
        AtomicInteger c1 = new AtomicInteger(0);
        AtomicInteger c2 = new AtomicInteger(0);
        AtomicInteger c3 = new AtomicInteger(0);
        AtomicInteger c4 = new AtomicInteger(0);
        AtomicInteger c5 = new AtomicInteger(0);

        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
        assertEquals(1, c3.get());

        c1.set(0);
        c2.set(0);
        c3.set(0);

        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
        assertEquals(1, c3.get());
        assertEquals(1, c4.get());

        c1.set(0);
        c2.set(0);
        c3.set(0);
        c4.set(0);

        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet, c5::incrementAndGet);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
        assertEquals(1, c3.get());
        assertEquals(1, c4.get());
        assertEquals(1, c5.get());
    }

    @Test
    public void testRunInParallelMultiple_01() {
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet));

            assertEquals(0, c1.get());
            assertEquals(1, c2.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet));

            assertEquals(0, c1.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet));

            assertEquals(0, c1.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);
            AtomicInteger c5 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet, c5::incrementAndGet));

            assertEquals(0, c1.get());
        }
    }

    @Test
    public void testRunInParallelMultiple_02() {
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet));

            assertEquals(0, c1.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet));

            assertEquals(0, c1.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet));

            assertEquals(0, c1.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);
            AtomicInteger c5 = new AtomicInteger(0);

            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet, c5::incrementAndGet));

            assertEquals(0, c1.get());
        }
    }

    @Test
    public void testTryOrDefaultWithSupplier() {
        {
            String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "success", Fn.s(() -> "default"));
            assertEquals("success", result1);

            String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new RuntimeException();
            }, Fn.s(() -> "default"));
            assertEquals("default", result2);
        }
        {

            AtomicInteger supplierCalls = new AtomicInteger(0);

            String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "Success", Fn.s(() -> {
                supplierCalls.incrementAndGet();
                return "Supplied Default";
            }));
            assertEquals("Success", result1);
            assertEquals(0, supplierCalls.get());

            String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new RuntimeException();
            }, Fn.s(() -> {
                supplierCalls.incrementAndGet();
                return "Supplied Default";
            }));
            assertEquals("Supplied Default", result2);
            assertEquals(1, supplierCalls.get());

        }
    }

    @Test
    public void testTryOrDefaultWithInitAndFunction() {
        String result1 = N.tryOrDefaultIfExceptionOccurred("input", s -> s.toUpperCase(), "default");
        assertEquals("INPUT", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred("input", s -> {
            throw new RuntimeException();
        }, "default");
        assertEquals("default", result2);
    }

    @Test
    public void testTryOrEmptyWithInitAndFunction() {
        Nullable<String> result1 = N.tryOrEmptyIfExceptionOccurred("input", s -> s.toUpperCase());
        assertTrue(result1.isPresent());
        assertEquals("INPUT", result1.get());

        Nullable<String> result2 = N.tryOrEmptyIfExceptionOccurred("input", s -> {
            throw new RuntimeException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testForEachNonNullWithFlatMapperTriple() {
        String[] array = { "ab", null, "cd", "ef" };
        List<String> result = new ArrayList<>();

        N.forEachNonNull(array, s -> s != null ? Arrays.asList(s.charAt(0), s.charAt(1)) : null,
                c -> c != null ? Arrays.asList(c.toString().toUpperCase(), c.toString().toLowerCase()) : null,
                (original, ch, str) -> result.add(original + ":" + ch + ":" + str));

        assertEquals(12, result.size());
        assertTrue(result.contains("ab:a:A"));
        assertTrue(result.contains("ab:a:a"));
    }

    @Test
    public void testToJsonWithFileAndConfig() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        File outputFile = new File(tempDir, "test_config.json");
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);
        N.toJson(person, config, outputFile);

        assertTrue(outputFile.exists());
        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("\n"));
        assertTrue(content.contains("John"));

        IOUtil.deleteIfExists(outputFile);
    }

    @Test
    public void testToJsonWithOutputStreamAndConfig() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);
        N.toJson(person, config, baos);

        String json = baos.toString();
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonWithWriterAndConfig() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        StringWriter writer = new StringWriter();
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);
        N.toJson(person, config, writer);

        String json = writer.toString();
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("John"));
    }

    @Test
    public void testFormatJsonWithTransferType() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        String json = N.toJson(map);

        String formatted = N.formatJson(json, Map.class);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("John"));
    }

    @Test
    public void testFormatXmlWithTransferType() {
        TestPerson person = new TestPerson("John", 30);
        String xml = N.toXml(person);

        String formatted = N.formatXml(xml, TestPerson.class);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("John"));
    }

    @Test
    public void testForEachWithStepLargerThanRange() {
        List<Integer> result = new ArrayList<>();
        N.forEach(0, 5, 10, result::add);
        assertEquals(Arrays.asList(0), result);
    }

    @Test
    public void testForEachWithNegativeStep() {
        List<Integer> result = new ArrayList<>();
        N.forEach(10, 0, -1, result::add);
        assertEquals(Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), result);
    }

    @Test
    public void testForEachWithEqualStartAndEnd() {
        List<Integer> result = new ArrayList<>();
        N.forEach(5, 5, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachArrayReversed() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEach(array, 4, -1, result::add);
        assertEquals(Arrays.asList("e", "d", "c", "b", "a"), result);
    }

    @Test
    public void testForEachIterableWithLinkedList() {
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> result = new ArrayList<>();
        N.forEach(linkedList, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);
    }

    @Test
    public void testForEachMapWithBiConsumerEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<String> result = new ArrayList<>();
        N.forEach(map, entry -> result.add(entry.getKey() + "=" + entry.getValue()));
        assertEquals(Arrays.asList("a=1", "b=2", "c=3"), result);
    }

    @Test
    public void testForEachParallelWithException() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertThrows(RuntimeException.class, () -> {
            N.forEach(list, i -> {
                if (i == 3) {
                    throw new RuntimeException("Test exception");
                }
            }, 2);
        });
    }

    @Test
    public void testForEachWithFlatMapperNullHandling() {
        String[] array = { "ab", null, "cd" };
        List<Character> result = new ArrayList<>();
        N.forEach(array, s -> s != null ? Arrays.asList(s.charAt(0), s.charAt(1)) : null, (s, c) -> result.add(c));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testForEachTripleNested() {
        List<String> level1 = Arrays.asList("A", "B");
        List<String> result = new ArrayList<>();
        N.forEach(level1, s1 -> Arrays.asList(s1 + "1", s1 + "2"), s2 -> Arrays.asList(s2 + "a", s2 + "b"),
                (original, mid, end) -> result.add(original + "-" + mid + "-" + end));
        assertEquals(8, result.size());
        assertTrue(result.contains("A-A1-A1a"));
        assertTrue(result.contains("B-B2-B2b"));
    }

    @Test
    public void testForEachPairWithEmptyCollection() {
        List<String> empty = Collections.emptyList();
        List<String> result = new ArrayList<>();
        N.forEachPair(empty, (a, b) -> result.add(a + "-" + b));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachPairWithSingleElement() {
        List<String> single = Arrays.asList("only");
        List<String> result = new ArrayList<>();
        N.forEachPair(single, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("only-null"), result);
    }

    @Test
    public void testForEachPair_1() {
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "b-c", "c-d", "d-e"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c, 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "c-d", "e-null"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c, 3, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "d-e"), result);
        }

        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c.iterator(), (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "b-c", "c-d", "d-e"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c.iterator(), 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "c-d", "e-null"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c.iterator(), 3, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "d-e"), result);
        }
    }

    @Test
    public void testForEachTrip_1() {
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "b-c-d", "c-d-e"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 2, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "c-d-e"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 3, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "d-e-null"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 4, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "e-null-null"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 5, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 6, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }

        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "b-c-d", "c-d-e"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 2, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "c-d-e"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 3, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "d-e-null"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 4, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "e-null-null"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 5, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }
        {
            List<String> c = CommonUtil.toList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 6, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }

    }

    @Test
    public void testForEachTripleWithLargeStep() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<String> result = new ArrayList<>();
        N.forEachTriple(array, 5, (a, b, c) -> result.add(a + "-" + (b != null ? b : "null") + "-" + (c != null ? c : "null")));
        assertEquals(Arrays.asList("1-2-3", "6-7-8"), result);
    }

    @Test
    public void testForEachIndexedWithNegativeRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Map<Integer, String> result = new HashMap<>();
        N.forEachIndexed(list, 3, 1, result::put);
        assertEquals("d", result.get(3));
        assertEquals("c", result.get(2));
        assertEquals(2, result.size());
    }

    @Test
    public void testForEachIndexe_01() {
        {
            List<String> list = Arrays.asList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 3, 1, result::put);
            assertEquals("d", result.get(3));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 1, 3, result::put);
            assertEquals("b", result.get(1));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 3, 1, result::put);
            assertEquals("d", result.get(3));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = CommonUtil.toLinkedList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 3, 1, result::put);
            assertEquals("d", result.get(3));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = CommonUtil.toLinkedList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list.iterator(), result::put);
            assertEquals("a", result.get(0));
            assertEquals("b", result.get(1));
            assertEquals("c", result.get(2));
            assertEquals("d", result.get(3));
            assertEquals("e", result.get(4));
            assertEquals(5, result.size());
        }
        {
            Collection<String> list = CommonUtil.toLinkedList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list.iterator(), result::put);
            assertEquals("a", result.get(0));
            assertEquals("b", result.get(1));
            assertEquals("c", result.get(2));
            assertEquals("d", result.get(3));
            assertEquals("e", result.get(4));
            assertEquals(5, result.size());

            Map<Integer, String> result2 = new HashMap<>();
            N.forEachIndexed(result, (i, e) -> result2.put(e.getKey() + 1, e.getValue()));
            assertEquals("a", result2.get(1));
            assertEquals("b", result2.get(2));
            assertEquals("c", result2.get(3));
            assertEquals("d", result2.get(4));
            assertEquals("e", result2.get(5));
            assertEquals(5, result2.size());

            Map<Integer, String> result3 = new HashMap<>();
            N.forEachIndexed(result, (i, k, v) -> result3.put(k + 1, v));
            assertEquals("a", result3.get(1));
            assertEquals("b", result3.get(2));
            assertEquals("c", result3.get(3));
            assertEquals("d", result3.get(4));
            assertEquals("e", result3.get(5));
            assertEquals(5, result3.size());
        }
    }

    @Test
    public void testForEachIndexedParallelWithCustomExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newFixedThreadPool(2);
        try {
            List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
            ConcurrentHashMap<Integer, Integer> result = new ConcurrentHashMap<>();
            N.forEachIndexed(list, result::put, 2, customExecutor);

            assertEquals(5, result.size());
            for (int i = 0; i < 5; i++) {
                assertEquals(i + 1, result.get(i));
            }
        } finally {
            customExecutor.shutdown();
        }
    }

    @Test
    public void testForEachNonNullWithAllNulls() {
        String[] array = { null, null, null };
        List<String> result = new ArrayList<>();
        N.forEachNonNull(array, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithMixedIterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        List<String> result = new ArrayList<>();
        N.forEach(list1, set2, (i, s) -> result.add(i + s));
        assertEquals(Arrays.asList("1a", "2b", "3c"), result);
    }

    @Test
    public void testAsyncExecuteWithCancellation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ContinuableFuture<Void> future = N.asyncExecute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertFalse(future.isDone());
        future.cancel(true);
        assertTrue(future.isCancelled());
        latch.countDown();
    }

    @Test
    public void testAsyncExecuteChaining() throws Exception {
        ContinuableFuture<String> future = N.asyncExecute(() -> "Hello");

        assertEquals("Hello World!", future.getThenApply(s -> s + " World!"));
    }

    @Test
    public void testAsyncExecuteWithExceptionHandling() throws Exception {
        ContinuableFuture<String> future = N.asyncExecute(() -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    public void testAsyncExecuteMultipleWithDifferentDelays() throws Exception {
        long start = System.currentTimeMillis();
        List<ContinuableFuture<Integer>> futures = Arrays.asList(N.asyncExecute(() -> 1, 100), N.asyncExecute(() -> 2, 50), N.asyncExecute(() -> 3, 150));

        List<Integer> results = new ArrayList<>();
        for (ContinuableFuture<Integer> future : futures) {
            results.add(future.get());
        }

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 150);
        assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    public void testAsyncExecuteListWithEmptyList() {
        List<Throwables.Runnable<Exception>> emptyCommands = Collections.emptyList();
        List<ContinuableFuture<Void>> futures = N.asyncExecute(emptyCommands);
        assertTrue(futures.isEmpty());
    }

    @Test
    public void testAsyncExecuteCollectionWithMixedResults() throws Exception {
        Collection<Callable<Object>> commands = Arrays.asList(() -> "string", () -> 42, () -> true, () -> null);

        List<ContinuableFuture<Object>> futures = N.asyncExecute(commands);
        List<Object> results = new ArrayList<>();
        for (ContinuableFuture<Object> future : futures) {
            results.add(future.get());
        }

        assertEquals(4, results.size());
        assertEquals("string", results.get(0));
        assertEquals(42, results.get(1));
        assertEquals(true, results.get(2));
        assertNull(results.get(3));
    }

    @Test
    public void testAsyncExecute_01() throws Exception {
        Collection<Callable<Object>> commands = Arrays.asList(() -> "string", () -> 42, () -> true, () -> null);

        List<ContinuableFuture<Object>> futures = N.asyncExecute(commands, Executors.newFixedThreadPool(6));
        List<Object> results = new ArrayList<>();
        for (ContinuableFuture<Object> future : futures) {
            results.add(future.get());
        }

        assertEquals(4, results.size());
        assertEquals("string", results.get(0));
        assertEquals(42, results.get(1));
        assertEquals(true, results.get(2));
        assertNull(results.get(3));
    }

    @Test
    public void testAsynRunWithExceptionInOneTask() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(counter::incrementAndGet, () -> {
            throw new RuntimeException("Test exception");
        }, counter::incrementAndGet);

        ObjIterator<Void> iter = N.asyncRun(commands);

        assertTrue(iter.hasNext());
        iter.next();

        assertThrows(RuntimeException.class, () -> {
            while (iter.hasNext()) {
                iter.next();
            }
        });
    }

    @Test
    public void testAsynCallWithDifferentExecutionTimes() {
        List<Callable<String>> commands = Arrays.asList(() -> {
            Thread.sleep(100);
            return "slow";
        }, () -> {
            Thread.sleep(10);
            return "fast";
        }, () -> {
            Thread.sleep(50);
            return "medium";
        });

        ObjIterator<String> iter = N.asyncCall(commands);
        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(3, results.size());
        assertTrue(results.contains("slow"));
        assertTrue(results.contains("fast"));
        assertTrue(results.contains("medium"));
    }

    @Test
    public void testRunInParallelWithExceptionInFirstTask() {
        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            N.runInParallel(() -> {
                throw new RuntimeException("First task failed");
            }, counter::incrementAndGet);
        });
    }

    @Test
    public void testRunInParallelWithExceptionInSecondTask() {
        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            N.runInParallel(counter::incrementAndGet, () -> {
                Thread.sleep(50);
                throw new RuntimeException("Second task failed");
            });
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void testRunInParallelWithEmptyCollection() {
        List<Throwables.Runnable<Exception>> emptyTasks = Collections.emptyList();
        N.runInParallel(emptyTasks);
    }

    @Test
    public void testRunInParallelWithCustomExecutor() {
        ExecutorService customExecutor = Executors.newFixedThreadPool(2);
        try {
            AtomicInteger counter = new AtomicInteger(0);
            List<Throwables.Runnable<Exception>> tasks = Arrays.asList(counter::incrementAndGet, counter::incrementAndGet, counter::incrementAndGet);

            N.runInParallel(tasks, customExecutor);
            assertEquals(3, counter.get());
        } finally {
            customExecutor.shutdown();
        }
    }

    @Test
    public void testCallInParallelWithNullResults() {
        Tuple3<String, Integer, Object> result = N.callInParallel(() -> "text", () -> 42, () -> null);

        assertEquals("text", result._1);
        assertEquals(42, result._2);
        assertNull(result._3);
    }

    @Test
    public void testRunByBatchWithExactMultiple() {
        Integer[] array = { 1, 2, 3, 4, 5, 6 };
        List<Integer> batchSizes = new ArrayList<>();
        N.runByBatch(array, 3, batch -> batchSizes.add(batch.size()));
        assertEquals(Arrays.asList(3, 3), batchSizes);
    }

    @Test
    public void testRunByBatchWithIterator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<Integer> sums = new ArrayList<>();
        N.runByBatch(list.iterator(), 2, batch -> sums.add(batch.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(Arrays.asList(3, 7, 11, 7), sums);
    }

    @Test
    public void testCallByBatchWithException() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertThrows(RuntimeException.class, () -> {
            N.callByBatch(list, 2, batch -> {
                if (batch.contains(3)) {
                    throw new RuntimeException("Batch contains 3");
                }
                return batch.size();
            });
        });
    }

    @Test
    public void testRunByBatchWithElementConsumerException() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        assertThrows(RuntimeException.class, () -> {
            N.runByBatch(array, 2, (idx, element) -> {
                if (element == 3) {
                    throw new RuntimeException("Element is 3");
                }
            }, () -> {
            });
        });
    }

    @Test
    public void testRunUninterruptiblyWithInterruption() {
        Thread currentThread = Thread.currentThread();
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> currentThread.interrupt(), 50, TimeUnit.MILLISECONDS);

        try {
            N.runUninterruptibly(() -> {
                Thread.sleep(100);
                wasInterrupted.set(Thread.currentThread().isInterrupted());
            });

            assertTrue(Thread.interrupted());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void testCallUninterruptiblyWithTimeUnitAndInterruption() {
        Thread currentThread = Thread.currentThread();

        MutableBoolean interrupted = MutableBoolean.of(false);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
        scheduler.schedule(() -> {
            currentThread.interrupt();
            interrupted.setTrue();
        }, 50, TimeUnit.MILLISECONDS);

        try {
            String result = N.callUninterruptibly((remainingNanos, unit) -> {
                unit.sleep(100);
                return "completed";
            }, 200, TimeUnit.MILLISECONDS);

            assertEquals("completed", result);
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void testForEachWithVeryLargeStep() {
        List<Integer> result = new ArrayList<>();
        N.forEach(0, 100, 1000, result::add);
        assertEquals(Arrays.asList(0), result);
    }

    @Test
    public void testForEachIndexedWithEmptyMap() {
        Map<String, Integer> emptyMap = Collections.emptyMap();
        AtomicInteger counter = new AtomicInteger(0);
        N.forEachIndexed(emptyMap, (idx, entry) -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testAsyncExecuteWithRetryAllFailures() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        assertThrows(ExecutionException.class, () -> {
            ContinuableFuture<Void> future = N.asyncExecute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 3, 10, e -> true);

            future.get();
        });

        assertEquals(4, attemptCount.get());
    }

    @Test
    public void testRunInParallelWithDifferentExecutionTimes() {
        long start = System.currentTimeMillis();
        AtomicInteger fast = new AtomicInteger(0);
        AtomicInteger slow = new AtomicInteger(0);

        N.runInParallel(() -> {
            Thread.sleep(10);
            fast.incrementAndGet();
        }, () -> {
            Thread.sleep(100);
            slow.incrementAndGet();
        });

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 100);
        assertEquals(1, fast.get());
        assertEquals(1, slow.get());
    }

    @Test
    public void testCallByBatchWithSingleElementBatch() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> results = N.callByBatch(list, 1, batch -> batch.get(0));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
    }

    @Test
    public void testForEachWithDescendingIterator() {
        {
            List<String> list = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
            List<String> result = new ArrayList<>();
            N.forEach(list, 4, 1, result::add);
            assertEquals(Arrays.asList("e", "d", "c"), result);
        }
        {
            Collection<String> list = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEach(list, 4, 1, result::add);
            assertEquals(Arrays.asList("e", "d", "c"), result);
        }
    }

    @Test
    public void testSleepWithZeroTimeout() {
        long start = System.currentTimeMillis();
        N.sleep(0);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 10);
    }

    @Test
    public void testSleepUninterruptiblyWithZeroTimeout() {
        long start = System.currentTimeMillis();
        N.sleepUninterruptibly(0);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 10);
    }

    @Test
    public void testToRuntimeExceptionWithError() {
        Error error = new Error("Test error");
        RuntimeException result = N.toRuntimeException(error);
        assertNotNull(result);
        assertEquals(error, result.getCause());
    }

    @Test
    public void testIfNotEmptyWithNull() {
        AtomicInteger counter = new AtomicInteger(0);

        N.ifNotEmpty((CharSequence) null, s -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        N.ifNotEmpty((Collection) null, c -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        N.ifNotEmpty((Map) null, m -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testForEachWithObjectAndParameter() {
        String[] array = { "a", "b", "c" };
        StringBuilder sb = new StringBuilder();
        N.forEach(0, array.length, sb, (i, builder) -> builder.append(array[i]));
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testAsyncExecuteListWithDifferentExecutors() throws Exception {
        ExecutorService executor1 = Executors.newSingleThreadExecutor();
        ExecutorService executor2 = Executors.newSingleThreadExecutor();

        try {
            List<Throwables.Runnable<Exception>> commands = Arrays.asList(() -> Thread.sleep(10), () -> Thread.sleep(10));

            List<ContinuableFuture<Void>> futures1 = N.asyncExecute(commands, executor1);
            List<ContinuableFuture<Void>> futures2 = N.asyncExecute(commands, executor2);

            for (ContinuableFuture<Void> future : futures1) {
                future.get();
            }
            for (ContinuableFuture<Void> future : futures2) {
                future.get();
            }
        } finally {
            executor1.shutdown();
            executor2.shutdown();
        }
    }

    @Test
    @Disabled("Performance assertions are nondeterministic across environments")
    public void testForEachParallelPerformance() throws Exception {
        int size = 1000;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }

        // initializing first
        N.forEach(List.of(1), Fn.println(), 10);

        AtomicInteger sum = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        N.forEach(list, i -> {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // N.println(System.currentTimeMillis() + "::" + Thread.currentThread().getName() + "::" + i);
            sum.addAndGet(i);
        }, 10);

        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < size);
        assertEquals(size * (size - 1) / 2, sum.get());
    }

    @Test
    public void testAsynCallOrderPreservation() {
        int size = 100;
        List<Callable<Integer>> commands = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final int value = i;
            commands.add(() -> {
                Thread.sleep(ThreadLocalRandom.current().nextInt(10));
                return value;
            });
        }

        ObjIterator<Integer> iter = N.asyncCall(commands);
        Set<Integer> results = new HashSet<>();

        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(size, results.size());
        for (int i = 0; i < size; i++) {
            assertTrue(results.contains(i));
        }
    }

    @Test
    public void testRunInParallelCancellationPropagation() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicBoolean task2Started = new AtomicBoolean(false);
        AtomicBoolean task2Completed = new AtomicBoolean(false);

        Thread executionThread = new Thread(() -> {
            try {
                N.runInParallel(() -> {
                    latch1.await();
                }, () -> {
                    task2Started.set(true);
                    latch2.await();
                    task2Completed.set(true);
                });
            } catch (Exception e) {
            }
        });

        executionThread.start();
        Thread.sleep(50);
        assertTrue(task2Started.get());

        executionThread.interrupt();
        latch1.countDown();
        latch2.countDown();

        executionThread.join(1000);
        assertFalse(executionThread.isAlive());
    }

    @Test
    public void testForEachWithConcurrentModification() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(ConcurrentModificationException.class, () -> {
            N.forEach(list, i -> {
                if (i == 3) {
                    list.add(6);
                }
            });
        });
    }

    @Test
    public void testAsyncExecuteMemoryLeak() throws Exception {
        List<ContinuableFuture<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            final int value = i;
            futures.add(N.asyncExecute(() -> value));
        }

        for (ContinuableFuture<Integer> future : futures) {
            future.get();
        }

        System.gc();
        Thread.sleep(100);

        for (ContinuableFuture<Integer> future : futures) {
            assertTrue(future.isDone());
        }
    }

    @Test
    public void testNestedForEachOperations() {
        List<String> outer = Arrays.asList("A", "B");
        List<Integer> inner = Arrays.asList(1, 2, 3);
        List<String> results = new ArrayList<>();

        N.forEach(outer, outerItem -> {
            N.forEach(inner, innerItem -> {
                N.forEach(0, innerItem, i -> {
                    results.add(outerItem + innerItem + i);
                });
            });
        });

        assertEquals(12, results.size());
    }

    @Test
    public void testComplexBatchProcessing() {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        List<String> results = N.callByBatch(data, 10, batch -> {
            int sum = batch.stream().mapToInt(Integer::intValue).sum();
            int min = batch.stream().min(Integer::compareTo).orElse(0);
            int max = batch.stream().max(Integer::compareTo).orElse(0);
            return String.format("Batch[%d-%d]: sum=%d", min, max, sum);
        });

        assertEquals(10, results.size());
        assertTrue(results.get(0).contains("Batch[1-10]: sum=55"));
        assertTrue(results.get(9).contains("Batch[91-100]: sum=955"));
    }

    @Test
    public void testMixedSynchronousAsynchronousExecution() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> syncResults = new ArrayList<>();

        ContinuableFuture<Void> async1 = N.asyncExecute(() -> {
            Thread.sleep(50);
            counter.addAndGet(10);
        });

        N.forEach(Arrays.asList(1, 2, 3), syncResults::add);

        ContinuableFuture<Integer> async2 = N.asyncExecute(() -> {
            Thread.sleep(25);
            return counter.addAndGet(20);
        });

        N.forEach(Arrays.asList(4, 5, 6), syncResults::add);

        async1.get();
        int async2Result = async2.get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), syncResults);
        assertEquals(30, counter.get());
        assertEquals(20, async2Result);
    }

    @Test
    public void testHighConcurrencyForEach() throws Exception {
        int threadCount = 50;
        int itemsPerThread = 100;
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    List<Integer> items = new ArrayList<>();
                    for (int i = 0; i < itemsPerThread; i++) {
                        items.add(threadId * itemsPerThread + i);
                    }
                    N.forEach(items, results::add, 5);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        assertEquals(threadCount * itemsPerThread, results.size());
        Set<Integer> uniqueResults = new HashSet<>(results);
        assertEquals(threadCount * itemsPerThread, uniqueResults.size());
    }

    @Test
    public void testLargeCollectionProcessing() {
        int size = 10000;
        List<Integer> largeList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            largeList.add(i);
        }

        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(largeList, 1000, 9000, sum::addAndGet);

        int expectedSum = 0;
        for (int i = 1000; i < 9000; i++) {
            expectedSum += i;
        }
        assertEquals(expectedSum, sum.get());
    }

    @Test
    public void testJsonSerializationOfComplexObjects() {
        Map<String, Object> complex = new HashMap<>();
        complex.put("string", "value");
        complex.put("number", 42);
        complex.put("array", Arrays.asList(1, 2, 3));
        complex.put("nested", Collections.singletonMap("key", "value"));
        complex.put("null", null);

        String json = N.toJson(complex);
        Map<String, Object> deserialized = N.fromJson(json, Map.class);

        assertEquals("value", deserialized.get("string"));
        assertEquals(42, ((Number) deserialized.get("number")).intValue());
        assertEquals(Arrays.asList(1, 2, 3), deserialized.get("array"));
        assertEquals("value", ((Map) deserialized.get("nested")).get("key"));
        assertNull(deserialized.get("null"));
    }

    @Test
    public void testExecuteWithRetryRecovery() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = N.callWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Fail " + attempt);
            }
            return "Success on attempt " + attempt;
        }, 5, 10, (r, e) -> e != null);

        assertEquals("Success on attempt 3", result);
        assertEquals(3, attempts.get());
    }

    @Test
    public void testForEachWithPartialFailure() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> processed = new ArrayList<>();

        try {
            N.forEach(items, i -> {
                processed.add(i);
                if (i == 3) {
                    throw new RuntimeException("Failed at 3");
                }
            });
        } catch (RuntimeException e) {
            assertEquals("Failed at 3", e.getMessage());
        }

        assertEquals(Arrays.asList(1, 2, 3), processed);
    }

    @Test
    public void testForEachWithRandomAccess() {
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        List<String> arrayListResult = new ArrayList<>();
        List<String> linkedListResult = new ArrayList<>();

        N.forEach(arrayList, 1, 4, arrayListResult::add);
        N.forEach(linkedList, 1, 4, linkedListResult::add);

        assertEquals(arrayListResult, linkedListResult);
        assertEquals(Arrays.asList("b", "c", "d"), arrayListResult);
    }

    @Test
    public void testPrintlnWithSpecialCharacters() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.println("Line1\nLine2\tTab\r\nLine3");
            String output = baos.toString();
            assertTrue(output.contains("Line1"));
            assertTrue(output.contains("Line2"));
            assertTrue(output.contains("Tab"));
            assertTrue(output.contains("Line3"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testLazyInitializationThreadSafety() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "initialized";
        });

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<String> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    results.add(lazy.get());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        assertEquals(1, counter.get());
        assertEquals(threadCount, results.size());
        for (String result : results) {
            assertEquals("initialized", result);
        }
    }

    @Test
    public void testComplexFlatMapping() {
        List<String> departments = Arrays.asList("Engineering", "Sales");
        Map<String, List<String>> employees = new HashMap<>();
        employees.put("Engineering", Arrays.asList("Alice", "Bob"));
        employees.put("Sales", Arrays.asList("Charlie", "David", "Eve"));

        List<String> result = new ArrayList<>();
        N.forEach(departments, dept -> employees.get(dept), emp -> Arrays.asList(emp.toLowerCase(), emp.toUpperCase()),
                (dept, emp, formatted) -> result.add(dept + ":" + emp + ":" + formatted));

        assertEquals(10, result.size());
        assertTrue(result.contains("Engineering:Alice:alice"));
        assertTrue(result.contains("Sales:Eve:EVE"));
    }

    @Test
    public void testRunByBatchWithNullElements() {
        String[] array = { "a", null, "b", null, "c" };
        List<List<String>> batches = new ArrayList<>();
        N.runByBatch(array, 2, batches::add);

        assertEquals(3, batches.size());
        assertEquals(Arrays.asList("a", null), batches.get(0));
        assertEquals(Arrays.asList("b", null), batches.get(1));
        assertEquals(Arrays.asList("c"), batches.get(2));
    }

    @Test
    public void testRunByBatchWithSingleElement() {
        Integer[] array = { 42 };
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(array, 5, batch -> {
            assertEquals(1, batch.size());
            assertEquals(42, batch.get(0));
            batchCount.incrementAndGet();
        });
        assertEquals(1, batchCount.get());
    }

    @Test
    public void testRunByBatchWithEmptyIterable() {
        List<String> emptyList = Collections.emptyList();
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(emptyList, 10, batch -> batchCount.incrementAndGet());
        assertEquals(0, batchCount.get());
    }

    @Test
    public void testRunByBatchWithNullIterator() {
        Iterator<String> nullIterator = null;
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(nullIterator, 10, batch -> batchCount.incrementAndGet());
        assertEquals(0, batchCount.get());
    }

    @Test
    public void testRunByBatchWithElementConsumerTracking() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f", "g");
        List<String> processedItems = new ArrayList<>();
        List<Integer> processedIndices = new ArrayList<>();
        AtomicInteger batchExecutions = new AtomicInteger(0);

        N.runByBatch(items, 3, (idx, item) -> {
            processedIndices.add(idx);
            processedItems.add(item);
        }, batchExecutions::incrementAndGet);

        assertEquals(items, processedItems);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6), processedIndices);
        assertEquals(3, batchExecutions.get());
    }

    @Test
    public void testRunByBatchExceptionPropagation() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);

        assertThrows(RuntimeException.class, () -> {
            N.runByBatch(items, 2, batch -> {
                if (batch.contains(3)) {
                    throw new RuntimeException("Found 3!");
                }
            });
        });
    }

    @Test
    public void testRunByBatchWithLargeBatchSize() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(array, 100, batch -> {
            assertEquals(5, batch.size());
            batchCount.incrementAndGet();
        });
        assertEquals(1, batchCount.get());
    }

    @Test
    public void testCallByBatchWithTransformation() {
        List<String> items = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        List<String> results = N.callByBatch(items, 2, batch -> batch.stream().map(String::toUpperCase).collect(Collectors.joining("-")));

        assertEquals(Arrays.asList("APPLE-BANANA", "CHERRY-DATE", "ELDERBERRY"), results);
    }

    @Test
    public void testCallByBatchWithEmptyArray() {
        String[] emptyArray = new String[0];
        List<Integer> results = N.callByBatch(emptyArray, 5, List::size);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testCallByBatchWithNullReturn() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4);
        List<String> results = N.callByBatch(items, 2, batch -> batch.contains(3) ? null : batch.toString());

        assertEquals(2, results.size());
        assertEquals("[1, 2]", results.get(0));
        assertNull(results.get(1));
    }

    @Test
    public void testCallByBatchWithElementConsumerState() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6);
        AtomicInteger sum = new AtomicInteger(0);

        List<Integer> results = N.callByBatch(items, 2, (idx, item) -> sum.addAndGet(item), () -> {
            int currentSum = sum.get();
            sum.set(0);
            return currentSum;
        });

        assertEquals(Arrays.asList(3, 7, 11), results);
    }

    @Test
    public void testCallByBatchIteratorWithComplexProcessing() {
        Iterator<String> iter = Arrays.asList("a", "bb", "ccc", "dddd", "eeeee").iterator();
        List<Integer> results = N.callByBatch(iter, 2, batch -> batch.stream().mapToInt(String::length).sum());

        assertEquals(Arrays.asList(3, 7, 5), results);
    }

    @Test
    public void testRunUninterruptiblyWithImmediateSuccess() {
        AtomicBoolean executed = new AtomicBoolean(false);
        N.runUninterruptibly(() -> {
            executed.set(true);
            Thread.sleep(10);
        });
        assertTrue(executed.get());
    }

    @Test
    public void testRunUninterruptiblyWithMultipleInterruptions() {
        AtomicInteger attempts = new AtomicInteger(0);
        Thread currentThread = Thread.currentThread();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> currentThread.interrupt(), 20, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> currentThread.interrupt(), 40, TimeUnit.MILLISECONDS);

        try {
            N.runUninterruptibly(() -> {
                attempts.incrementAndGet();
                Thread.sleep(100);
            });

            assertEquals(3, attempts.get());
            assertTrue(Thread.interrupted());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void testRunUninterruptiblyWithTimeoutExpiration() {
        AtomicBoolean completed = new AtomicBoolean(false);
        long start = System.currentTimeMillis();

        N.runUninterruptibly(remainingMillis -> {
            try {
                if (remainingMillis > 0) {
                    Thread.sleep(remainingMillis);
                    completed.set(true);
                }
            } catch (InterruptedException e) {
                throw e;
            }
        }, 100);

        long duration = System.currentTimeMillis() - start;
        assertTrue(completed.get());
        assertTrue(duration >= 100);
    }

    @Test
    public void testRunUninterruptiblyWithTimeUnit() {
        AtomicInteger executionCount = new AtomicInteger(0);

        N.runUninterruptibly((remaining, unit) -> {
            executionCount.incrementAndGet();
            assertTrue(remaining > 0);
            assertEquals(TimeUnit.NANOSECONDS, unit);
            unit.sleep(10);
        }, 50, TimeUnit.MILLISECONDS);

        assertEquals(1, executionCount.get());
    }

    @Test
    public void testCallUninterruptiblyWithResult() {
        String result = N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return "Success";
        });
        assertEquals("Success", result);
    }

    @Test
    public void testCallUninterruptiblyWithNullResult() {
        String result = N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return null;
        });
        assertNull(result);
    }

    @Test
    public void testCallUninterruptiblyWithComplexComputation() {
        List<Integer> result = N.callUninterruptibly(() -> {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Thread.sleep(10);
                list.add(i);
            }
            return list;
        });

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testCallUninterruptiblyWithRemainingTime() {
        List<Long> remainingTimes = new ArrayList<>();

        String result = N.callUninterruptibly(remainingMillis -> {
            remainingTimes.add(remainingMillis);
            Thread.sleep(30);
            return "Completed with " + remainingMillis + "ms remaining";
        }, 100);

        assertNotNull(result);
        assertTrue(remainingTimes.get(0) <= 100);
        assertTrue(remainingTimes.get(0) > 0);
    }

    @Test
    public void testCallUninterruptiblyWithTimeUnitConversion() {
        AtomicReference<TimeUnit> receivedUnit = new AtomicReference<>();
        AtomicLong receivedTime = new AtomicLong();

        Integer result = N.callUninterruptibly((time, unit) -> {
            receivedUnit.set(unit);
            receivedTime.set(time);
            unit.sleep(1);
            return 42;
        }, 100, TimeUnit.MILLISECONDS);

        assertEquals(42, result);
        assertEquals(TimeUnit.NANOSECONDS, receivedUnit.get());
        assertTrue(receivedTime.get() > 0);
    }

    @Test
    public void testTryOrEmptyWithSuccessfulExecution() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> "Success");
        assertTrue(result.isPresent());
        assertEquals("Success", result.get());
    }

    @Test
    public void testTryOrEmptyWithCheckedException() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new IOException("IO Error");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testTryOrEmptyWithRuntimeException() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new IllegalArgumentException("Invalid argument");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testTryOrEmptyWithNullResult() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void testTryOrEmptyWithFunction() {
        Nullable<Integer> result1 = N.tryOrEmptyIfExceptionOccurred("123", Integer::parseInt);
        assertTrue(result1.isPresent());
        assertEquals(123, result1.get());

        Nullable<Integer> result2 = N.tryOrEmptyIfExceptionOccurred("abc", Integer::parseInt);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrEmptyWithComplexOperation() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);

        Nullable<Integer> result1 = N.tryOrEmptyIfExceptionOccurred(map, m -> m.get("key"));
        assertTrue(result1.isPresent());
        assertEquals(42, result1.get());

        Nullable<Integer> result2 = N.tryOrEmptyIfExceptionOccurred(map, m -> {
            throw new UnsupportedOperationException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrDefaultWithVariousScenarios() {
        String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "Success", "Default");
        assertEquals("Success", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
            throw new RuntimeException();
        }, "Default");
        assertEquals("Default", result2);

        String result3 = N.tryOrDefaultIfExceptionOccurred(() -> null, "Default");
        assertNull(result3);
    }

    @Test
    public void testTryOrDefaultWithFunctionAndInit() {
        List<String> list = Arrays.asList("a", "b", "c");

        Integer result1 = N.tryOrDefaultIfExceptionOccurred(list, List::size, -1);
        assertEquals(3, result1);

        Integer result2 = N.tryOrDefaultIfExceptionOccurred(list, l -> {
            throw new IndexOutOfBoundsException();
        }, -1);
        assertEquals(-1, result2);
    }

    @Test
    public void testIfNotEmptyWithCharSequence() {
        StringBuilder executed = new StringBuilder();

        N.ifNotEmpty("Hello", s -> executed.append("String: ").append(s));
        assertEquals("String: Hello", executed.toString());

        executed.setLength(0);
        N.ifNotEmpty("", s -> executed.append("Should not execute"));
        assertEquals("", executed.toString());

        N.ifNotEmpty((CharSequence) null, s -> executed.append("Should not execute"));
        assertEquals("", executed.toString());
    }

    @Test
    public void testIfNotEmptyWithStringBuilder() {
        AtomicInteger counter = new AtomicInteger(0);

        StringBuilder sb1 = new StringBuilder("content");
        N.ifNotEmpty(sb1, s -> {
            counter.incrementAndGet();
            s.append(" modified");
        });
        assertEquals(1, counter.get());
        assertEquals("content modified", sb1.toString());

        StringBuilder sb2 = new StringBuilder();
        N.ifNotEmpty(sb2, s -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testIfNotEmptyWithCollections() {
        List<String> executed = new ArrayList<>();

        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b"));
        N.ifNotEmpty(arrayList, list -> executed.add("ArrayList: " + list.size()));

        LinkedList<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3));
        N.ifNotEmpty(linkedList, list -> executed.add("LinkedList: " + list.size()));

        HashSet<String> hashSet = new HashSet<>(Arrays.asList("x", "y"));
        N.ifNotEmpty(hashSet, set -> executed.add("HashSet: " + set.size()));

        Vector<String> emptyVector = new Vector<>();
        N.ifNotEmpty(emptyVector, v -> executed.add("Should not execute"));

        assertEquals(3, executed.size());
        assertTrue(executed.contains("ArrayList: 2"));
        assertTrue(executed.contains("LinkedList: 3"));
        assertTrue(executed.contains("HashSet: 2"));
    }

    @Test
    public void testIfNotEmptyWithMaps() {
        List<String> executed = new ArrayList<>();

        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        N.ifNotEmpty(hashMap, map -> executed.add("HashMap: " + map.size()));

        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("x", "X");
        N.ifNotEmpty(treeMap, map -> executed.add("TreeMap: " + map.size()));

        LinkedHashMap<String, String> emptyMap = new LinkedHashMap<>();
        N.ifNotEmpty(emptyMap, map -> executed.add("Should not execute"));

        assertEquals(2, executed.size());
        assertTrue(executed.contains("HashMap: 2"));
        assertTrue(executed.contains("TreeMap: 1"));
    }

    @Test
    public void testIfNotEmptyWithModification() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        N.ifNotEmpty(list, l -> {
            l.add(4);
            l.remove(Integer.valueOf(1));
        });
        assertEquals(Arrays.asList(2, 3, 4), list);

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        N.ifNotEmpty(map, m -> {
            m.put("b", 2);
            m.remove("a");
        });
        assertEquals(1, map.size());
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testIfOrEmptyBasicCases() {
        Nullable<String> result1 = N.ifOrEmpty(true, () -> "Present");
        assertTrue(result1.isPresent());
        assertEquals("Present", result1.get());

        Nullable<String> result2 = N.ifOrEmpty(false, () -> "Not Present");
        assertFalse(result2.isPresent());
    }

    @Test
    public void testIfOrEmptyWithNullSupplier() {
        Nullable<String> result = N.ifOrEmpty(true, () -> null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void testIfOrEmptyWithException() {
        assertThrows(RuntimeException.class, () -> {
            N.ifOrEmpty(true, () -> {
                throw new RuntimeException("Supplier failed");
            });
        });

        Nullable<String> result = N.ifOrEmpty(false, () -> {
            throw new RuntimeException("Should not execute");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testIfOrEmptyWithComplexConditions() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 85);
        scores.put("Bob", 92);

        Nullable<String> topScorer = N.ifOrEmpty(scores.values().stream().anyMatch(s -> s > 90),
                () -> scores.entrySet().stream().filter(e -> e.getValue() > 90).map(Map.Entry::getKey).findFirst().orElse(null));

        assertTrue(topScorer.isPresent());
        assertEquals("Bob", topScorer.get());
    }

    @Test
    public void testIfNotNullBasicCases() {
        AtomicInteger counter = new AtomicInteger(0);

        N.ifNotNull("value", v -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotNull(null, v -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotNull(42, v -> counter.addAndGet(v));
        assertEquals(43, counter.get());
    }

    @Test
    public void testIfNotNullWithDifferentTypes() {
        List<Object> processed = new ArrayList<>();

        N.ifNotNull("String", processed::add);
        N.ifNotNull(123, processed::add);
        N.ifNotNull(true, processed::add);
        N.ifNotNull(new TestPerson("John", 30), processed::add);
        N.ifNotNull(Arrays.asList(1, 2, 3), processed::add);
        N.ifNotNull(null, processed::add);

        assertEquals(5, processed.size());
        assertFalse(processed.contains(null));
    }

    @Test
    public void testIfNotNullWithModification() {
        TestPerson person = new TestPerson("John", 30);
        N.ifNotNull(person, p -> {
            p.setName("Jane");
            p.setAge(31);
        });
        assertEquals("Jane", person.getName());
        assertEquals(31, person.getAge());
    }

    @Test
    public void testCombiningBatchAndParallelProcessing() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        List<ContinuableFuture<Integer>> futures = N.callByBatch(data, 10, batch -> N.asyncExecute(() -> batch.stream().mapToInt(Integer::intValue).sum()));

        int totalSum = 0;
        for (ContinuableFuture<Integer> future : futures) {
            totalSum += future.get();
        }

        assertEquals(5050, totalSum);
    }

    @Test
    public void testNestedTryOperations() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            String intermediate = N.tryOrDefaultIfExceptionOccurred(() -> {
                if (Math.random() < 0.5) {
                    throw new RuntimeException("Random failure");
                }
                return "Success";
            }, "Fallback");

            return N.tryOrDefaultIfExceptionOccurred(intermediate, s -> s.toUpperCase(), "ERROR");
        });

        assertTrue(result.isPresent());
        assertTrue("SUCCESS".equals(result.get()) || "FALLBACK".equals(result.get()));
    }

    @Test
    public void testConditionalBatchProcessing() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger evenBatches = new AtomicInteger(0);
        AtomicInteger oddBatches = new AtomicInteger(0);

        N.forEach(numbers, n -> {
            N.ifOrEmpty(n % 2 == 0, () -> "even").ifPresent(type -> {
                List<Integer> batch = Arrays.asList(n);
                N.runByBatch(batch, 1, b -> evenBatches.incrementAndGet());
            });

            N.ifOrEmpty(n % 2 != 0, () -> "odd").ifPresent(type -> {
                List<Integer> batch = Arrays.asList(n);
                N.runByBatch(batch, 1, b -> oddBatches.incrementAndGet());
            });
        });

        assertEquals(5, evenBatches.get());
        assertEquals(5, oddBatches.get());
    }

    @Test
    public void testUninterruptibleBatchProcessing() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> results = new ArrayList<>();

        N.runUninterruptibly(() -> {
            N.callByBatch(data, 3, batch -> {
                Thread.sleep(10);
                return batch.stream().mapToInt(Integer::intValue).sum();
            }).forEach(results::add);
        });

        assertEquals(4, results.size());
        assertEquals(55, results.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void testRunByBatchWithNegativeBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.runByBatch(Arrays.asList(1, 2, 3), -1, batch -> {
            });
        });
    }

    @Test
    public void testCallByBatchWithZeroBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.callByBatch(Arrays.asList(1, 2, 3), 0, batch -> batch.size());
        });
    }

    @Test
    public void testRunUninterruptiblyWithNullCommand() {
        assertThrows(NullPointerException.class, () -> {
            N.runUninterruptibly(null);
        });
    }

    @Test
    public void testCallUninterruptiblyWithNullCommand() {
        assertThrows(NullPointerException.class, () -> {
            N.callUninterruptibly(null);
        });
    }

    @Test
    public void testIfNotEmptyWithCustomCollectionTypes() {
        Queue<String> queue = new LinkedList<>(Arrays.asList("a", "b", "c"));
        AtomicInteger queueProcessed = new AtomicInteger(0);
        N.ifNotEmpty(queue, q -> queueProcessed.set(q.size()));
        assertEquals(3, queueProcessed.get());

        Deque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 4));
        AtomicInteger dequeProcessed = new AtomicInteger(0);
        N.ifNotEmpty(deque, d -> dequeProcessed.set(d.size()));
        assertEquals(4, dequeProcessed.get());
    }

    @Test
    public void testBatchProcessingWithStateManagement() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f");
        Map<Integer, List<String>> batchMap = new HashMap<>();
        AtomicInteger batchNumber = new AtomicInteger(0);

        N.runByBatch(items, 2, (idx, item) -> {
            int currentBatch = batchNumber.get();
            batchMap.computeIfAbsent(currentBatch, k -> new ArrayList<>()).add(item);
        }, () -> batchNumber.incrementAndGet());

        assertEquals(3, batchMap.size());
        assertEquals(Arrays.asList("a", "b"), batchMap.get(0));
        assertEquals(Arrays.asList("c", "d"), batchMap.get(1));
        assertEquals(Arrays.asList("e", "f"), batchMap.get(2));
    }

    @Test
    public void testPrintlnWithNullValue() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Object result = N.println(null);
            assertNull(result);
            assertTrue(baos.toString().contains("null"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintlnWithPrimitiveArrays() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            int[] intArray = { 1, 2, 3 };
            int[] result = N.println(intArray);
            assertSame(intArray, result);

            baos.reset();
            Object[] objArray = { 1, "two", 3.0 };
            Object[] objResult = N.println(objArray);
            assertSame(objArray, objResult);
            assertTrue(baos.toString().contains("[1, two, 3.0]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintlnWithNestedCollections() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            List<List<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
            List<List<String>> result = N.println(nested);
            assertSame(nested, result);
            assertTrue(baos.toString().contains("[[a, b], [c, d]]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testFprintlnWithVariousFormats() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.fprintln("String: %s, Integer: %d, Float: %.2f", "test", 42, 3.14159);
            String output = baos.toString();
            assertTrue(output.contains("String: test, Integer: 42, Float: 3.14"));

            baos.reset();
            N.fprintln("No arguments");
            assertEquals("No arguments" + IOUtil.LINE_SEPARATOR, baos.toString());

            baos.reset();
            N.fprintln("%d%% complete", 75);
            assertTrue(baos.toString().contains("75% complete"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testExecuteWithNoRetries() {
        AtomicInteger attempts = new AtomicInteger(0);
        N.runWithRetry(() -> attempts.incrementAndGet(), 0, 0, e -> false);
        assertEquals(1, attempts.get());
    }

    @Test
    public void testExecuteWithConditionalRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        assertThrows(RuntimeException.class, () -> N.runWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            RuntimeException e = new RuntimeException("Attempt " + attempt);
            exceptions.add(e);
            throw e;
        }, 3, 10, e -> e.getMessage().contains("Attempt 1") || e.getMessage().contains("Attempt 2")));

        assertEquals(3, attempts.get());
        assertEquals(3, exceptions.size());
    }

    @Test
    public void testExecuteCallableWithBiPredicate() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = N.callWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                return "wrong";
            } else {
                return "correct";
            }
        }, 3, 10, (r, e) -> "wrong".equals(r));

        assertEquals("correct", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testLazyInitWithException() {
        AtomicInteger attempts = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Initialization failed");
        });

        assertThrows(RuntimeException.class, lazy::get);
        assertEquals(1, attempts.get());

        assertThrows(RuntimeException.class, lazy::get);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testLazyInitializeWithCheckedException() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Throwables.Supplier<String, IOException> lazy = N.lazyInitialize(() -> {
            attempts.incrementAndGet();
            if (attempts.get() == 1) {
                throw new IOException("First attempt fails");
            }
            return "Success";
        });

        assertThrows(IOException.class, lazy::get);
        assertEquals(1, attempts.get());

        assertEquals("Success", lazy.get());
        assertEquals(2, attempts.get());
    }

    @Test
    public void testRunByBatchThreadSafety() throws Exception {
        int threadCount = 10;
        int itemsPerThread = 100;
        ConcurrentLinkedQueue<Integer> allProcessed = new ConcurrentLinkedQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    List<Integer> items = new ArrayList<>();
                    for (int i = 0; i < itemsPerThread; i++) {
                        items.add(threadId * itemsPerThread + i);
                    }

                    N.runByBatch(items, 10, batch -> {
                        Thread.sleep(1);
                        allProcessed.addAll(batch);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        assertEquals(threadCount * itemsPerThread, allProcessed.size());
        Set<Integer> unique = new HashSet<>(allProcessed);
        assertEquals(threadCount * itemsPerThread, unique.size());
    }

    @Test
    public void testComplexIfNotEmptyChaining() {
        Map<String, List<Integer>> data = new HashMap<>();
        data.put("numbers", Arrays.asList(1, 2, 3, 4, 5));

        AtomicInteger result = new AtomicInteger(0);

        N.ifNotEmpty(data, map -> {
            N.ifNotEmpty(map.get("numbers"), list -> {
                N.ifNotEmpty(list.stream().filter(n -> n % 2 == 0).collect(Collectors.toList()), evens -> {
                    result.set(evens.stream().mapToInt(Integer::intValue).sum());
                });
            });
        });

        assertEquals(6, result.get());
    }

    @Test
    public void testRunUninterruptiblyEdgeCases() {
        AtomicBoolean executed = new AtomicBoolean(false);
        N.runUninterruptibly(() -> executed.set(true));
        assertTrue(executed.get());

        executed.set(false);
        N.runUninterruptibly(millis -> executed.set(true), 0);
        assertTrue(executed.get());

        executed.set(false);
        N.runUninterruptibly(millis -> executed.set(true), -100);
        assertTrue(executed.get());
    }

    @Test
    public void testCallUninterruptiblyEdgeCases() {
        String result1 = N.callUninterruptibly(() -> "immediate");
        assertEquals("immediate", result1);

        String result2 = N.callUninterruptibly(millis -> "zero timeout", 0);
        assertEquals("zero timeout", result2);

        String result3 = N.callUninterruptibly(millis -> "negative timeout", -100);
        assertEquals("negative timeout", result3);
    }

    @Test
    public void testBatchProcessingWithEmptyBatches() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        AtomicInteger batchCount = new AtomicInteger(0);

        N.runByBatch(items.iterator(), 10, batch -> {
            assertFalse(batch.isEmpty());
            batchCount.incrementAndGet();
        });

        assertEquals(1, batchCount.get());
    }

    @Test
    public void testTryOrDefaultWithExceptionInDefault() {
        assertThrows(RuntimeException.class, () -> {
            N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new IOException("Primary failed");
            }, Fn.s(() -> {
                throw new RuntimeException("Default also failed");
            }));
        });
    }

    @Test
    public void testComplexParallelBatchProcessing() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            data.add(i);
        }

        ConcurrentHashMap<Integer, Integer> results = new ConcurrentHashMap<>();

        List<Callable<Void>> tasks = N.callByBatch(data, 5, batch -> {
            return (Callable<Void>) () -> {
                int sum = batch.stream().mapToInt(Integer::intValue).sum();
                results.put(batch.get(0), sum);
                return null;
            };
        });

        N.callInParallel(tasks);

        assertEquals(10, results.size());
        int totalSum = results.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(1225, totalSum);
    }

    @Test
    public void testIfOrElseDeprecated() {
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        N.ifOrElse(true, trueCount::incrementAndGet, falseCount::incrementAndGet);
        assertEquals(1, trueCount.get());
        assertEquals(0, falseCount.get());

        N.ifOrElse(false, trueCount::incrementAndGet, falseCount::incrementAndGet);
        assertEquals(1, trueCount.get());
        assertEquals(1, falseCount.get());

        N.ifOrElse(true, null, falseCount::incrementAndGet);
        assertEquals(1, falseCount.get());

        N.ifOrElse(false, trueCount::incrementAndGet, null);
        assertEquals(1, trueCount.get());
    }

    @Test
    public void testSleepInterruption() {
        Thread currentThread = Thread.currentThread();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> currentThread.interrupt(), 50, TimeUnit.MILLISECONDS);

        try {
            assertThrows(RuntimeException.class, () -> N.sleep(200));
            assertTrue(Thread.interrupted());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void testAsyncExecuteWithNullCommands() {
        List<Throwables.Runnable<Exception>> nullList = null;
        assertTrue(N.asyncExecute(nullList).isEmpty());

        Collection<Callable<String>> nullCollection = null;
        assertTrue(N.asyncExecute(nullCollection).isEmpty());
    }

    @Test
    public void testForEachIntegrationScenarios() {
        Map<String, List<Integer>> departmentScores = new HashMap<>();
        departmentScores.put("Sales", Arrays.asList(85, 92, 78));
        departmentScores.put("Engineering", Arrays.asList(95, 88, 91, 87));
        departmentScores.put("HR", Arrays.asList(82, 79));

        Map<String, Double> averages = new HashMap<>();

        N.forEach(departmentScores, (dept, scores) -> {
            N.ifNotEmpty(scores, scoreList -> {
                MutableDouble sum = MutableDouble.of(0.0);
                AtomicInteger count = new AtomicInteger(0);

                N.forEachIndexed(scoreList, (idx, score) -> {
                    N.ifNotNull(score, s -> count.incrementAndGet());
                });

                N.forEach(scoreList, score -> {
                    N.ifNotNull(score, s -> sum.add(s));
                });

                if (count.get() > 0) {
                    averages.put(dept, sum.value() / count.get());
                }
            });
        });

        assertEquals(3, averages.size());
        assertTrue(averages.get("Sales") > 80);
        assertTrue(averages.get("Engineering") > 85);
        assertTrue(averages.get("HR") > 75);
    }

    @Test
    public void testMemoryEfficientBatchProcessing() {
        int totalItems = 1000;
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger maxBatchesInMemory = new AtomicInteger(0);
        List<WeakReference<List<Integer>>> batchRefs = new ArrayList<>();

        Iterator<Integer> iter = new Iterator<>() {
            int current = 0;

            @Override
            public boolean hasNext() {
                return current < totalItems;
            }

            @Override
            public Integer next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return current++;
            }
        };

        N.runByBatch(iter, 50, batch -> {
            processedCount.addAndGet(batch.size());
            batchRefs.add(new WeakReference<>(batch));

            if (batchRefs.size() % 5 == 0) {
                System.gc();

                long activeRefs = batchRefs.stream().filter(ref -> ref.get() != null).count();
                maxBatchesInMemory.set(Math.max(maxBatchesInMemory.get(), (int) activeRefs));
            }
        });

        assertEquals(totalItems, processedCount.get());
        assertTrue(maxBatchesInMemory.get() < 10);
    }

    @Test
    public void testFilterObjectArray() {
        List<String> result = N.filter(stringArray, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four", "five"), result);

        result = N.filter(stringArray, s -> s.startsWith("t"));
        assertEquals(Arrays.asList("two", "three"), result);
    }

    @Test
    public void testFilterObjectArrayWithSupplier() {
        Set<String> result = N.filter(stringArray, s -> s.length() > 3, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("three", "four", "five")), result);
    }

    @Test
    public void testFilterObjectArrayWithRange() {
        List<String> result = N.filter(stringArray, 1, 4, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four"), result);
    }

    @Test
    public void testFilterCollection() {
        List<String> result = N.filter(stringList, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four", "five"), result);
    }

    @Test
    public void testMapToBoolean() {
        boolean[] result = N.mapToBoolean(stringArray, s -> s.length() > 3);
        assertArrayEquals(new boolean[] { false, false, true, true, true }, result);
    }

    @Test
    public void testMapToBooleanWithRange() {
        boolean[] result = N.mapToBoolean(stringArray, 1, 4, s -> s.length() > 3);
        assertArrayEquals(new boolean[] { false, true, true }, result);
    }

    @Test
    public void testMapToBooleanFromCollection() {
        boolean[] result = N.mapToBoolean(stringList, s -> s.startsWith("t"));
        assertArrayEquals(new boolean[] { false, true, true, false, false }, result);
    }

    @Test
    public void testMapToChar() {
        char[] result = N.mapToChar(stringArray, s -> s.charAt(0));
        assertArrayEquals(new char[] { 'o', 't', 't', 'f', 'f' }, result);
    }

    @Test
    public void testMapToCharWithRange() {
        char[] result = N.mapToChar(stringArray, 1, 3, s -> s.charAt(0));
        assertArrayEquals(new char[] { 't', 't' }, result);
    }

    @Test
    public void testMapToByte() {
        byte[] result = N.mapToByte(integerArray, i -> i.byteValue());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToByteWithRange() {
        byte[] result = N.mapToByte(integerArray, 2, 5, i -> i.byteValue());
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
    }

    @Test
    public void testMapToShort() {
        short[] result = N.mapToShort(integerArray, i -> i.shortValue());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToShortWithRange() {
        short[] result = N.mapToShort(integerArray, 1, 4, i -> i.shortValue());
        assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    public void testMapToInt() {
        int[] result = N.mapToInt(stringArray, String::length);
        assertArrayEquals(new int[] { 3, 3, 5, 4, 4 }, result);
    }

    @Test
    public void testMapToIntWithRange() {
        int[] result = N.mapToInt(stringArray, 0, 3, String::length);
        assertArrayEquals(new int[] { 3, 3, 5 }, result);
    }

    @Test
    public void testMapToLong() {
        long[] result = N.mapToLong(integerArray, i -> i.longValue());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testMapToLongWithRange() {
        long[] result = N.mapToLong(integerArray, 2, 5, i -> i.longValue());
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testMapToFloat() {
        float[] result = N.mapToFloat(integerArray, i -> i.floatValue());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result, 0.001f);
    }

    @Test
    public void testMapToFloatWithRange() {
        float[] result = N.mapToFloat(integerArray, 1, 4, i -> i.floatValue());
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testMapToDouble() {
        double[] result = N.mapToDouble(integerArray, i -> i.doubleValue());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testMapToDoubleWithRange() {
        double[] result = N.mapToDouble(integerArray, 0, 3, i -> i.doubleValue());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testMap() {
        List<Integer> result = N.map(stringArray, String::length);
        assertEquals(Arrays.asList(3, 3, 5, 4, 4), result);
    }

    @Test
    public void testMapWithSupplier() {
        Set<Integer> result = N.map(stringArray, String::length, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void testMapWithRange() {
        List<Integer> result = N.map(stringArray, 1, 4, String::length);
        assertEquals(Arrays.asList(3, 5, 4), result);
    }

    @Test
    public void testMapCollection() {
        List<Integer> result = N.map(stringList, 0, 3, String::length);
        assertEquals(Arrays.asList(3, 3, 5), result);
    }

    @Test
    public void testFlatMap() {
        List<Character> result = N.flatMap(stringArray, s -> Arrays.asList(s.charAt(0), s.charAt(1)));
        assertEquals(Arrays.asList('o', 'n', 't', 'w', 't', 'h', 'f', 'o', 'f', 'i'), result);
    }

    @Test
    public void testFlatMapWithSupplier() {
        Set<Character> result = N.flatMap(new String[] { "ab", "cd", "ef" }, s -> Arrays.asList(s.charAt(0), s.charAt(1)), IntFunctions.ofSet());
        assertEquals(new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f')), result);
    }

    @Test
    public void testFlatMapWithRange() {
        List<Character> result = N.flatMap(stringArray, 0, 2, s -> Arrays.asList(s.charAt(0), s.charAt(1)));
        assertEquals(Arrays.asList('o', 'n', 't', 'w'), result);
    }

    @Test
    public void testFlatMapCollection() {
        List<Character> result = N.flatMap(Arrays.asList("ab", "cd"), 1, 2, s -> Arrays.asList(s.charAt(0), s.charAt(1)));
        assertEquals(Arrays.asList('c', 'd'), result);
    }

    @Test
    public void testFlatMapTwice() {
        List<Integer> result = N.flatMap(new String[] { "1,2", "3,4" }, s -> Arrays.asList(s.split(",")),
                str -> Arrays.asList(Integer.parseInt(String.valueOf(str))), IntFunctions.ofList());
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testTakeWhile() {
        List<Integer> result = N.takeWhile(integerArray, i -> i < 4);
        assertEquals(Arrays.asList(1, 2, 3), result);

        result = N.takeWhile(integerArray, i -> i < 1);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testTakeWhileInclusive() {
        List<Integer> result = N.takeWhileInclusive(integerArray, i -> i < 3);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDropWhile() {
        List<Integer> result = N.dropWhile(integerArray, i -> i < 3);
        assertEquals(Arrays.asList(3, 4, 5), result);

        result = N.dropWhile(integerArray, i -> i < 10);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testSkipUntil() {
        List<Integer> result = N.skipUntil(integerArray, i -> i > 3);
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testMapAndFilter() {
        List<Integer> result = N.mapAndFilter(stringList, String::length, len -> len > 3);
        assertEquals(Arrays.asList(5, 4, 4), result);
    }

    @Test
    public void testMapAndFilterWithSupplier() {
        Set<Integer> result = N.mapAndFilter(stringList, String::length, len -> len > 3, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(5, 4)), result);
    }

    @Test
    public void testFilterAndMap() {
        List<Integer> result = N.filterAndMap(stringList, s -> s.length() > 3, String::length);
        assertEquals(Arrays.asList(5, 4, 4), result);
    }

    @Test
    public void testFilterAndMapWithSupplier() {
        Set<String> result = N.filterAndMap(integerList, i -> i % 2 == 0, i -> "even:" + i, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("even:2", "even:4")), result);
    }

    @Test
    public void testFlatMapAndFilter() {
        List<Character> result = N.flatMapAndFilter(Arrays.asList("abc", "de", "fghi"), s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList()),
                c -> c > 'c');
        assertEquals(Arrays.asList('d', 'e', 'f', 'g', 'h', 'i'), result);
    }

    @Test
    public void testFilterAndFlatMap() {
        List<Character> result = N.filterAndFlatMap(Arrays.asList("abc", "de", "fghi"), s -> s.length() > 2,
                s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList()));
        assertEquals(Arrays.asList('a', 'b', 'c', 'f', 'g', 'h', 'i'), result);
    }

    @Test
    public void testDistinctCharArray() {
        char[] input = { 'a', 'b', 'a', 'c', 'b', 'd' };
        char[] result = N.distinct(input);
        assertEquals(4, result.length);
        assertTrue(contains(result, 'a'));
        assertTrue(contains(result, 'b'));
        assertTrue(contains(result, 'c'));
        assertTrue(contains(result, 'd'));
    }

    @Test
    public void testDistinctObjectArray() {
        String[] input = { "one", "two", "one", "three", "two" };
        List<String> result = N.distinct(input);
        assertEquals(3, result.size());
        assertTrue(result.contains("one"));
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));
    }

    @Test
    public void testDistinctCollection() {
        List<Integer> input = Arrays.asList(1, 2, 3, 2, 4, 3, 5);
        List<Integer> result = N.distinct(input, 1, 6);
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctBy() {
        List<String> result = N.distinctBy(stringArray, String::length);
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctByWithSupplier() {
        Set<String> result = N.distinctBy(stringArray, String::length, HashSet::new);
        assertEquals(3, result.size());
    }

    @Test
    public void testAllMatch() {
        assertTrue(N.allMatch(new Integer[] { 2, 4, 6 }, i -> i % 2 == 0));
        assertFalse(N.allMatch(integerArray, i -> i % 2 == 0));
        assertTrue(N.allMatch(new Integer[0], i -> false));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(N.anyMatch(integerArray, i -> i > 4));
        assertFalse(N.anyMatch(integerArray, i -> i > 10));
        assertFalse(N.anyMatch(new Integer[0], i -> true));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(N.noneMatch(integerArray, i -> i > 10));
        assertFalse(N.noneMatch(integerArray, i -> i > 3));
        assertTrue(N.noneMatch(new Integer[0], i -> true));
    }

    @Test
    public void testNMatch() {
        assertTrue(N.isMatchCountBetween(integerArray, 2, 3, i -> i % 2 == 0));
        assertFalse(N.isMatchCountBetween(integerArray, 3, 4, i -> i % 2 == 0));
        assertTrue(N.isMatchCountBetween(new Integer[0], 0, 0, i -> true));
    }

    @Test
    public void testCountCharArray() {
        assertEquals(3, N.count(charArray, c -> c > 'b'));
        assertEquals(2, N.count(charArray, c -> c <= 'b'));
    }

    @Test
    public void testCountObjectArray() {
        assertEquals(3, N.count(stringArray, s -> s.length() > 3));
        assertEquals(2, N.count(stringArray, s -> s.startsWith("t")));
    }

    @Test
    public void testCountCollection() {
        assertEquals(3, N.count(stringList, 0, 5, s -> s.length() > 3));
        assertEquals(2, N.count(stringList, 1, 4, s -> s.length() > 3));
        assertEquals(2, N.count(CommonUtil.newLinkedList(stringList), 1, 4, s -> s.length() > 3));
    }

    @Test
    public void testMergeMultipleIterables() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));
        List<Integer> result = N.merge(lists, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipWithDefaults() {
        Integer[] a1 = { 1, 2, 3 };
        String[] a2 = { "a", "b" };
        List<Pair<Integer, String>> result = N.zip(a1, a2, 0, "z", Fn.pair());
        assertEquals(3, result.size());
        assertEquals(Pair.of(3, "z"), result.get(2));
    }

    @Test
    public void testZipToArray() {
        Integer[] a1 = { 1, 2, 3 };
        Integer[] a2 = { 10, 20, 30 };
        Integer[] result = N.zip(a1, a2, (x, y) -> x + y, Integer.class);
        assertArrayEquals(new Integer[] { 11, 22, 33 }, result);
    }

    @Test
    public void testUnzip() {
        List<Pair<Integer, String>> pairs = Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"));
        Pair<List<Integer>, List<String>> result = N.unzip(pairs, (p, out) -> {
            out.setLeft(p.left());
            out.setRight(p.right());
        });
        assertEquals(Arrays.asList(1, 2, 3), result.left());
        assertEquals(Arrays.asList("a", "b", "c"), result.right());
    }

    @Test
    public void testUnzipp() {
        List<Triple<Integer, String, Double>> triples = Arrays.asList(Triple.of(1, "a", 1.0), Triple.of(2, "b", 2.0));
        Triple<List<Integer>, List<String>, List<Double>> result = N.unzip3(triples, (t, out) -> {
            out.setLeft(t.left());
            out.setMiddle(t.middle());
            out.setRight(t.right());
        });
        assertEquals(Arrays.asList(1, 2), result.left());
        assertEquals(Arrays.asList("a", "b"), result.middle());
        assertEquals(Arrays.asList(1.0, 2.0), result.right());
    }

    @Test
    public void testGroupByWithValueExtractor() {
        Map<Integer, List<Character>> result = N.groupBy(stringList, String::length, s -> s.charAt(0));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList('o', 't'), result.get(3));
        assertEquals(Arrays.asList('t'), result.get(5));
        assertEquals(Arrays.asList('f', 'f'), result.get(4));
    }

    @Test
    public void testGroupByWithCollector() {
        Map<Integer, Long> result = N.groupBy(stringList, String::length, Collectors.counting());
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(2), result.get(3));
        assertEquals(Long.valueOf(1), result.get(5));
        assertEquals(Long.valueOf(2), result.get(4));
    }

    @Test
    public void testCountBy() {
        Map<Integer, Integer> result = N.countBy(stringList, String::length);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(2), result.get(3));
        assertEquals(Integer.valueOf(1), result.get(5));
        assertEquals(Integer.valueOf(2), result.get(4));
    }

    @Test
    public void testDisjointEmptyCollections() {
        assertTrue(N.disjoint(Collections.emptyList(), Collections.emptyList()));
        assertTrue(N.disjoint(integerList, Collections.emptyList()));
        assertTrue(N.disjoint(Collections.emptyList(), integerList));
    }

    @Test
    public void testNullHandling() {
        assertArrayEquals(new boolean[0], N.filter((boolean[]) null, b -> b));
        assertEquals(Collections.emptyList(), N.filter((String[]) null, s -> true));

        assertArrayEquals(new boolean[0], N.mapToBoolean((String[]) null, s -> true));
        assertEquals(Collections.emptyList(), N.map((String[]) null, s -> s));

        assertEquals(0, N.count((int[]) null, i -> true));
        assertEquals(0, N.count((Iterator<?>) null));

        assertArrayEquals(new boolean[0], N.distinct((boolean[]) null));
        assertEquals(Collections.emptyList(), N.distinct((String[]) null));
    }

    @Test
    public void testEmptyCollectionHandling() {
        String[] emptyArray = new String[0];
        assertEquals(Collections.emptyList(), N.filter(emptyArray, s -> true));
        assertEquals(Collections.emptyList(), N.map(emptyArray, s -> s));
        assertEquals(Collections.emptyList(), N.distinct(emptyArray));

        List<String> emptyList = Collections.emptyList();
        assertEquals(Collections.emptyList(), N.filter(emptyList, s -> true));
        assertEquals(Collections.emptyList(), N.map(emptyList, s -> s));
        assertEquals(Collections.emptyList(), N.distinct(emptyList));
    }

    @Test
    public void testInvalidRangeFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, -1, 3, i -> true));
    }

    @Test
    public void testInvalidRangeToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, 0, 10, i -> true));
    }

    @Test
    public void testInvalidRangeFromGreaterThanTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, 3, 2, i -> true));
    }

    @Test
    public void testNMatchNegativeAtLeast() {
        assertThrows(IllegalArgumentException.class, () -> N.isMatchCountBetween(integerArray, -1, 2, i -> true));
    }

    @Test
    public void testNMatchNegativeAtMost() {
        assertThrows(IllegalArgumentException.class, () -> N.isMatchCountBetween(integerArray, 1, -1, i -> true));
    }

    @Test
    public void testNMatchAtLeastGreaterThanAtMost() {
        assertThrows(IllegalArgumentException.class, () -> N.isMatchCountBetween(integerArray, 3, 2, i -> true));
    }

    @Test
    public void testFilterWithEmptyArrays() {
        assertArrayEquals(new boolean[0], N.filter(emptyBooleanArray, b -> b));
        assertArrayEquals(new char[0], N.filter(emptyCharArray, c -> true));
        assertArrayEquals(new int[0], N.filter(emptyIntArray, i -> true));
        assertEquals(Collections.emptyList(), N.filter(emptyStringArray, s -> true));
    }

    @Test
    public void testFilterWithSingleElement() {
        assertArrayEquals(new boolean[] { true }, N.filter(singleBooleanArray, b -> b));
        assertArrayEquals(new boolean[0], N.filter(singleBooleanArray, b -> !b));
        assertArrayEquals(new int[] { 42 }, N.filter(singleIntArray, i -> i > 0));
        assertEquals(Arrays.asList("single"), N.filter(singleStringArray, s -> s != null));
    }

    @Test
    public void testFilterAllElementsMatch() {
        int[] allEven = { 2, 4, 6, 8, 10 };
        assertArrayEquals(allEven, N.filter(allEven, i -> i % 2 == 0));
    }

    @Test
    public void testFilterNoElementsMatch() {
        int[] allOdd = { 1, 3, 5, 7, 9 };
        assertArrayEquals(new int[0], N.filter(allOdd, i -> i % 2 == 0));
    }

    @Test
    public void testFilterWithNullElements() {
        List<Integer> result = N.filter(nullContainingArray, i -> i != null);
        assertEquals(Arrays.asList(1, 3, 5), result);

        result = N.filter(nullContainingArray, i -> i == null);
        assertEquals(Arrays.asList(null, null), result);
    }

    @Test
    public void testFilterLargeDataset() {
        int[] result = N.filter(largeIntArray, i -> i % 1000 == 0);
        assertEquals(10, result.length);
        assertEquals(0, result[0]);
        assertEquals(9000, result[9]);
    }

    @Test
    public void testFilterRangeEdgeCases() {
        int[] arr = { 1, 2, 3, 4, 5 };

        assertArrayEquals(new int[0], N.filter(arr, 2, 2, i -> true));

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.filter(arr, 0, 5, i -> true));

        assertArrayEquals(new int[] { 3 }, N.filter(arr, 2, 3, i -> true));
    }

    @Test
    public void testMapWithComplexTransformations() {
        String[] words = { "hello", "world", "java", "test" };

        List<Character> firstChars = N.map(words, s -> Character.toUpperCase(s.charAt(0)));
        assertEquals(Arrays.asList('H', 'W', 'J', 'T'), firstChars);

        List<String> stats = N.map(words, s -> s + ":" + s.length());
        assertEquals(Arrays.asList("hello:5", "world:5", "java:4", "test:4"), stats);
    }

    @Test
    public void testMapToIntWithComplexFunctions() {
        String[] sentences = { "Hello world", "Java programming", "Unit testing" };

        int[] spaceCounts = N.mapToInt(sentences, s -> (int) s.chars().filter(c -> c == ' ').count());
        assertArrayEquals(new int[] { 1, 1, 1 }, spaceCounts);

        int[] hashCodes = N.mapToInt(sentences, String::hashCode);
        assertEquals(3, hashCodes.length);
    }

    @Test
    public void testMapPrimitiveConversions() {
        int[] ints = { 1, 2, 3 };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(ints, i -> (long) i));
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.mapToDouble(ints, i -> (double) i), 0.001);

        long[] longs = { 100L, 200L, 300L };
        assertArrayEquals(new int[] { 100, 200, 300 }, N.mapToInt(longs, l -> (int) l));
        assertArrayEquals(new double[] { 100.0, 200.0, 300.0 }, N.mapToDouble(longs, l -> (double) l), 0.001);

        double[] doubles = { 1.5, 2.5, 3.5 };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.mapToInt(doubles, d -> (int) d));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(doubles, d -> (long) d));
    }

    @Test
    public void testMapWithNullHandling() {
        List<String> result = N.map(nullContainingArray, i -> i == null ? "NULL" : i.toString());
        assertEquals(Arrays.asList("1", "NULL", "3", "NULL", "5"), result);
    }

    @Test
    public void testFlatMapEmptyCollections() {
        String[] arr = { "", "a", "", "bc" };
        List<Character> result = N.flatMap(arr, s -> s.isEmpty() ? Collections.emptyList() : Arrays.asList(s.charAt(0)));
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testFlatMapNestedStructures() {
        List<List<List<Integer>>> nested = Arrays.asList(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)),
                Arrays.asList(Arrays.asList(5, 6), Arrays.asList(7, 8)));

        List<Integer> result = N.flatMap(nested, Fn.identity(), Fn.identity());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testFlatMapWithNullCollections() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), null, Arrays.asList("c", "d"), null);

        List<String> result = N.flatMap(lists, list -> list == null ? Collections.emptyList() : list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testDistinctAllDuplicates() {
        assertArrayEquals(new int[] { 1 }, N.distinct(duplicateIntArray));
        assertEquals(Arrays.asList("dup"), N.distinct(duplicateStringArray));
    }

    @Test
    public void testDistinctAlreadyDistinct() {
        int[] distinct = { 1, 2, 3, 4, 5 };
        assertArrayEquals(distinct, N.distinct(distinct));
    }

    @Test
    public void testDistinctWithNulls() {
        Integer[] withNulls = { 1, null, 2, null, 3, null, 1, 2, 3 };
        List<Integer> result = N.distinct(withNulls);
        assertEquals(4, result.size());
        assertTrue(result.contains(null));
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testDistinctByComplexKeys() {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }

        Person[] people = { new Person("John", 25), new Person("Jane", 25), new Person("Bob", 30), new Person("Alice", 25) };

        List<Person> byAge = N.distinctBy(people, p -> p.age);
        assertEquals(2, byAge.size());

        List<Person> byNameLength = N.distinctBy(people, p -> p.name.length());
        assertEquals(3, byNameLength.size());
    }

    @Test
    public void testMatchWithEmptyPredicates() {
        assertTrue(N.allMatch(new Integer[] { 1, 2, 3 }, i -> true));
        assertTrue(N.anyMatch(new Integer[] { 1, 2, 3 }, i -> true));
        assertFalse(N.noneMatch(new Integer[] { 1, 2, 3 }, i -> true));

        assertFalse(N.allMatch(new Integer[] { 1, 2, 3 }, i -> false));
        assertFalse(N.anyMatch(new Integer[] { 1, 2, 3 }, i -> false));
        assertTrue(N.noneMatch(new Integer[] { 1, 2, 3 }, i -> false));
    }

    @Test
    public void testNMatchEdgeCases() {
        Integer[] arr = { 1, 2, 3, 4, 5 };

        assertTrue(N.isMatchCountBetween(arr, 2, 2, i -> i % 2 == 0));

        assertTrue(N.isMatchCountBetween(arr, 0, 5, i -> false));

        assertFalse(N.isMatchCountBetween(arr, 10, 10, i -> true));
    }

    @Test
    public void testCountAllPrimitiveTypes() {
        assertEquals(2, N.count(new boolean[] { true, false, true }, b -> b));
        assertEquals(3, N.count(new char[] { 'a', 'b', 'c' }, c -> c >= 'a'));
        assertEquals(2, N.count(new byte[] { 1, 2, 3 }, b -> b % 2 == 1));
        assertEquals(3, N.count(new short[] { 10, 20, 30 }, s -> s >= 10));
        assertEquals(2, N.count(new float[] { 1.5f, 2.5f, 3.5f }, f -> f < 3.0f));
        assertEquals(1, N.count(new double[] { 1.1, 2.2, 3.3 }, d -> d > 3.0));
    }

    @Test
    public void testCountWithComplexPredicates() {
        String[] words = { "hello", "world", "java", "programming", "test" };

        assertEquals(0, N.count(words, s -> s.equals(new StringBuilder(s).reverse().toString())));

        assertEquals(2, N.count(words, s -> s.contains("a")));

        assertEquals(2, N.count(words, s -> s.length() % 2 == 0));
    }

    @Test
    public void testMergeWithEmptyArrays() {
        Integer[] empty = new Integer[0];
        Integer[] nonEmpty = { 1, 2, 3 };

        List<Integer> result1 = N.merge(empty, nonEmpty, Fn.f((a, b) -> MergeResult.TAKE_FIRST));
        assertEquals(Arrays.asList(1, 2, 3), result1);

        List<Integer> result2 = N.merge(nonEmpty, empty, Fn.f((a, b) -> MergeResult.TAKE_FIRST));
        assertEquals(Arrays.asList(1, 2, 3), result2);

        List<Integer> result3 = N.merge(empty, empty, Fn.f((a, b) -> MergeResult.TAKE_FIRST));
        assertEquals(Collections.emptyList(), result3);
    }

    @Test
    public void testMergeComplexDecisions() {
        String[] arr1 = { "apple", "cherry", "elderberry" };
        String[] arr2 = { "banana", "date", "fig" };

        List<String> result = N.merge(arr1, arr2, Fn.f((a, b) -> a.compareTo(b) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));
        assertEquals(Arrays.asList("apple", "banana", "cherry", "date", "elderberry", "fig"), result);
    }

    @Test
    public void testMergeMultipleCollections() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 5, 9), Arrays.asList(2, 6, 10), Arrays.asList(3, 7, 11), Arrays.asList(4, 8, 12));

        List<Integer> result = N.merge(lists, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), result);
    }

    @Test
    public void testZipDifferentLengths() {
        Integer[] short1 = { 1, 2 };
        String[] long1 = { "a", "b", "c", "d" };

        List<Pair<Integer, String>> result = N.zip(short1, long1, Fn.pair());
        assertEquals(2, result.size());
        assertEquals(Pair.of(1, "a"), result.get(0));
        assertEquals(Pair.of(2, "b"), result.get(1));
    }

    @Test
    public void testZipWithTransformation() {
        Double[] prices = { 10.5, 20.0, 15.75 };
        Integer[] quantities = { 2, 3, 1 };

        List<Double> totals = N.zip(prices, quantities, (p, q) -> p * q);
        assertEquals(Arrays.asList(21.0, 60.0, 15.75), totals);
    }

    @Test
    public void testZipThreeWithDefaults() {
        Integer[] a1 = { 1, 2 };
        String[] a2 = { "a" };
        Double[] a3 = { 1.0, 2.0, 3.0 };

        List<String> result = N.zip(a1, a2, a3, 0, "z", 0.0, (i, s, d) -> i + s + d);
        assertEquals(Arrays.asList("1a1.0", "2z2.0", "0z3.0"), result);
    }

    @Test
    public void testUnzipEmptyCollection() {
        List<Pair<String, Integer>> empty = Collections.emptyList();
        Pair<List<String>, List<Integer>> result = N.unzip(empty, (p, out) -> {
            out.setLeft(p.left());
            out.setRight(p.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipWithTransformation() {
        List<String> items = Arrays.asList("a:1", "b:2", "c:3");
        Pair<List<String>, List<Integer>> result = N.unzip(items, (item, out) -> {
            String[] parts = item.split(":");
            out.setLeft(parts[0]);
            out.setRight(Integer.parseInt(parts[1]));
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testGroupByEmptyCollection() {
        Map<String, List<String>> result = N.groupBy(Collections.<String> emptyList(), s -> s);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupByAllSameGroup() {
        String[] allSame = { "aa", "bb", "cc", "dd" };
        Map<Integer, List<String>> result = N.groupBy(allSame, String::length);
        assertEquals(1, result.size());
        assertEquals(4, result.get(2).size());
    }

    @Test
    public void testGroupByWithNullKeys() {
        String[] words = { "one", null, "two", null, "three" };
        Map<Integer, List<String>> result = N.groupBy(words, s -> s == null ? null : s.length());

        assertTrue(result.containsKey(null));
        assertEquals(Arrays.asList(null, null), result.get(null));
        assertEquals(Arrays.asList("one", "two"), result.get(3));
        assertEquals(Arrays.asList("three"), result.get(5));
    }

    @Test
    public void testCountByWithTransformations() {
        String[] words = { "Hello", "World", "JAVA", "test", "CODE" };

        Map<Boolean, Integer> byCase = N.countBy(Arrays.asList(words), s -> s.equals(s.toUpperCase()));
        assertEquals(Integer.valueOf(2), byCase.get(true));
        assertEquals(Integer.valueOf(3), byCase.get(false));
    }

    @Test
    public void testIterateWithModification() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> iter = N.iterate(list);

        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());

        list.add("d");

        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void testIterateAllEmpty() {
        List<List<String>> empty = Arrays.asList(Collections.<String> emptyList(), Collections.<String> emptyList());

        ObjIterator<String> iter = N.iterateAll(empty);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterateAllMixed() {
        List<List<Integer>> mixed = Arrays.asList(Collections.<Integer> emptyList(), Arrays.asList(1, 2), Collections.<Integer> emptyList(),
                Arrays.asList(3, 4, 5), Collections.<Integer> emptyList());

        ObjIterator<Integer> iter = N.iterateAll(mixed);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDisjointWithDuplicates() {
        Integer[] a1 = { 1, 1, 2, 2, 3, 3 };
        Integer[] a2 = { 4, 4, 5, 5, 6, 6 };
        assertTrue(N.disjoint(a1, a2));

        Integer[] a3 = { 3, 3, 4, 4, 5, 5 };
        assertFalse(N.disjoint(a1, a3));
    }

    @Test
    public void testDisjointWithNulls() {
        Object[] a1 = { 1, null, 3 };
        Object[] a2 = { 2, null, 4 };
        assertFalse(N.disjoint(a1, a2));

        Object[] a3 = { 2, 4, 6 };
        assertTrue(N.disjoint(a1, a3));
    }

    @Test
    public void testDisjointPerformance() {
        Set<Integer> bigSet = new HashSet<>();
        List<Integer> smallList = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            bigSet.add(i);
        }

        for (int i = 20000; i < 20010; i++) {
            smallList.add(i);
        }

        assertTrue(N.disjoint(bigSet, smallList));
        assertTrue(N.disjoint(smallList, bigSet));
    }

    @Test
    public void testLargeDatasetOperations() {
        int[] filtered = N.filter(largeIntArray, i -> i % 100 == 0);
        assertEquals(100, filtered.length);

        boolean[] mapped = N.mapToBoolean(largeIntList, i -> i % 2 == 0);
        assertEquals(10000, mapped.length);

        int count = N.count(largeIntArray, i -> i < 5000);
        assertEquals(5000, count);

        int[] manyDupes = new int[10000];
        Arrays.fill(manyDupes, 0, 5000, 1);
        Arrays.fill(manyDupes, 5000, 10000, 2);
        int[] distinct = N.distinct(manyDupes);
        assertEquals(2, distinct.length);
    }

    @Test
    public void testComplexPredicates() {
        class Product {
            String name;
            double price;
            String category;
            boolean inStock;

            Product(String name, double price, String category, boolean inStock) {
                this.name = name;
                this.price = price;
                this.category = category;
                this.inStock = inStock;
            }
        }

        Product[] products = { new Product("Laptop", 999.99, "Electronics", true), new Product("Mouse", 29.99, "Electronics", true),
                new Product("Desk", 299.99, "Furniture", false), new Product("Chair", 199.99, "Furniture", true),
                new Product("Monitor", 399.99, "Electronics", false) };

        List<Product> result = N.filter(products, p -> p.inStock && p.price < 500 && "Electronics".equals(p.category));
        assertEquals(1, result.size());
        assertEquals("Mouse", result.get(0).name);

        Map<String, List<Product>> byCategory = N.groupBy(products, p -> p.category);
        assertEquals(2, byCategory.size());
        assertEquals(3, byCategory.get("Electronics").size());
        assertEquals(2, byCategory.get("Furniture").size());

        Map<Boolean, Integer> stockCount = N.countBy(Arrays.asList(products), p -> p.inStock);
        assertEquals(Integer.valueOf(3), stockCount.get(true));
        assertEquals(Integer.valueOf(2), stockCount.get(false));
    }

    @Test
    public void testStatelessOperations() {
        int[] arr = { 1, 2, 3, 4, 5 };

        int[] result1 = N.filter(arr, i -> i > 2);
        int[] result2 = N.filter(arr, i -> i > 2);
        assertArrayEquals(result1, result2);

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, arr);
    }

    @Test
    public void testSpecialNumberCases() {
        float[] floats = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f };
        float[] filtered = N.filter(floats, f -> !Float.isNaN(f) && Float.isFinite(f));
        assertArrayEquals(new float[] { 0.0f, -0.0f }, filtered, 0.001f);

        double[] doubles = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -0.0 };
        int count = N.count(doubles, d -> Double.isInfinite(d));
        assertEquals(2, count);
    }

    @Test
    public void testFunctionalComposition() {
        String[] words = { "hello", "world", "java", "programming" };

        List<String> filtered = N.filter(words, s -> s.length() > 4);
        List<Integer> lengths = N.map(filtered.toArray(new String[0]), String::length);
        assertEquals(Arrays.asList(5, 5, 11), lengths);

        int[] wordLengths = N.mapToInt(words, String::length);
        int[] longLengths = N.filter(wordLengths, len -> len > 4);
        assertArrayEquals(new int[] { 5, 5, 11 }, longLengths);
    }

    @Test
    public void testGroupByWithCustomCollectors() {
        String[] words = { "one", "two", "three", "four", "five", "six" };

        Map<Integer, String> joined = N.groupBy(Arrays.asList(words), String::length, Collectors.joining("-"));

        assertEquals("one-two-six", joined.get(3));
        assertEquals("four-five", joined.get(4));
        assertEquals("three", joined.get(5));

        Map<Integer, java.util.Optional<String>> firstByLength = N.groupBy(Arrays.asList(words), String::length, Collectors.reducing((a, b) -> a));

        assertEquals("one", firstByLength.get(3).get());
        assertEquals("four", firstByLength.get(4).get());
        assertEquals("three", firstByLength.get(5).get());
    }

    @Test
    public void testIteratorExhaustion() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Iterator<Integer> iter = list.iterator();

        assertEquals(Integer.valueOf(1), iter.next());

        List<Integer> result = N.filter(iter, i -> i > 1);
        assertEquals(Arrays.asList(2, 3), result);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testComplexMapOperations() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("evens", Arrays.asList(2, 4, 6));
        map.put("odds", Arrays.asList(1, 3, 5));
        map.put("mixed", Arrays.asList(1, 2, 3));

        Iterator<Map.Entry<String, List<Integer>>> iter = N.iterate(map);
        int entryCount = 0;
        while (iter.hasNext()) {
            iter.next();
            entryCount++;
        }
        assertEquals(3, entryCount);
    }

    @Test
    public void testDeleteRange_boolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.removeRange(arr, 1, 3);
        assertArrayEquals(new boolean[] { true, false, true }, result);

        assertEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.removeRange((boolean[]) null, 0, 0));

        boolean[] original = { true, false };
        boolean[] noDeletion = N.removeRange(original, 0, 0);
        assertArrayEquals(original, noDeletion);
    }

    @Test
    public void testDeleteRange_char() {
        char[] result = N.removeRange(charArray, 1, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'e' }, result);

        assertEquals(CommonUtil.EMPTY_CHAR_ARRAY, N.removeRange((char[]) null, 0, 0));
        assertArrayEquals(charArray.clone(), N.removeRange(charArray, 0, 0));
    }

    @Test
    public void testDeleteRange_byte() {
        byte[] result = N.removeRange(byteArray, 1, 3);
        assertArrayEquals(new byte[] { 1, 4, 5 }, result);

        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, N.removeRange((byte[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_short() {
        short[] result = N.removeRange(shortArray, 1, 3);
        assertArrayEquals(new short[] { 1, 4, 5 }, result);

        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, N.removeRange((short[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_int() {
        int[] result = N.removeRange(intArray, 1, 3);
        assertArrayEquals(new int[] { 1, 4, 5 }, result);

        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.removeRange((int[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_long() {
        long[] result = N.removeRange(longArray, 1, 3);
        assertArrayEquals(new long[] { 1L, 4L, 5L }, result);

        assertEquals(CommonUtil.EMPTY_LONG_ARRAY, N.removeRange((long[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_float() {
        float[] result = N.removeRange(floatArray, 1, 3);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f }, result);

        assertEquals(CommonUtil.EMPTY_FLOAT_ARRAY, N.removeRange((float[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_double() {
        double[] result = N.removeRange(doubleArray, 1, 3);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0 }, result);

        assertEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.removeRange((double[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_String() {
        String[] result = N.removeRange(stringArray, 1, 3);
        assertArrayEquals(new String[] { "one", "four", "five" }, result);

        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.removeRange((String[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_generic() {
        Integer[] result = N.removeRange(integerArray, 1, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5 }, result);
    }

    @Test
    public void testDeleteRange_List() {
        List<Integer> list = new ArrayList<>(integerList);
        assertTrue(N.removeRange(list, 1, 3));
        assertEquals(Arrays.asList(1, 4, 5), list);

        List<Integer> emptyList = new ArrayList<>();
        assertFalse(N.removeRange(emptyList, 0, 0));
    }

    @Test
    public void testDeleteRange_String_method() {
        String result = N.removeRange("hello world", 5, 6);
        assertEquals("helloworld", result);
    }

    @Test
    public void testReplaceRange_boolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] replacement = { false, false };
        boolean[] result = N.replaceRange(arr, 1, 3, replacement);
        assertArrayEquals(new boolean[] { true, false, false, false, true }, result);

        boolean[] emptyReplacement = {};
        result = N.replaceRange(arr, 1, 3, emptyReplacement);
        assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    @Test
    public void testReplaceRange_char() {
        char[] replacement = { 'x', 'y' };
        char[] result = N.replaceRange(charArray, 1, 3, replacement);
        assertArrayEquals(new char[] { 'a', 'x', 'y', 'd', 'e' }, result);
    }

    @Test
    public void testReplaceRange_byte() {
        byte[] replacement = { 10, 20 };
        byte[] result = N.replaceRange(byteArray, 1, 3, replacement);
        assertArrayEquals(new byte[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_short() {
        short[] replacement = { 10, 20 };
        short[] result = N.replaceRange(shortArray, 1, 3, replacement);
        assertArrayEquals(new short[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_int() {
        int[] replacement = { 10, 20 };
        int[] result = N.replaceRange(intArray, 1, 3, replacement);
        assertArrayEquals(new int[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_long() {
        long[] replacement = { 10L, 20L };
        long[] result = N.replaceRange(longArray, 1, 3, replacement);
        assertArrayEquals(new long[] { 1L, 10L, 20L, 4L, 5L }, result);
    }

    @Test
    public void testReplaceRange_float() {
        float[] replacement = { 10.0f, 20.0f };
        float[] result = N.replaceRange(floatArray, 1, 3, replacement);
        assertArrayEquals(new float[] { 1.0f, 10.0f, 20.0f, 4.0f, 5.0f }, result);
    }

    @Test
    public void testReplaceRange_double() {
        double[] replacement = { 10.0, 20.0 };
        double[] result = N.replaceRange(doubleArray, 1, 3, replacement);
        assertArrayEquals(new double[] { 1.0, 10.0, 20.0, 4.0, 5.0 }, result);
    }

    @Test
    public void testReplaceRange_String() {
        String[] replacement = { "ten", "twenty" };
        String[] result = N.replaceRange(stringArray, 1, 3, replacement);
        assertArrayEquals(new String[] { "one", "ten", "twenty", "four", "five" }, result);
    }

    @Test
    public void testReplaceRange_generic() {
        Integer[] replacement = { 10, 20 };
        Integer[] result = N.replaceRange(integerArray, 1, 3, replacement);
        assertArrayEquals(new Integer[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_List() {
        List<Integer> list = new ArrayList<>(integerList);
        List<Integer> replacement = Arrays.asList(10, 20);
        assertTrue(N.replaceRange(list, 1, 3, replacement));
        assertEquals(Arrays.asList(1, 10, 20, 4, 5), list);

        assertFalse(N.replaceRange(list, 0, 0, Collections.emptyList()));
    }

    @Test
    public void testReplaceRange_String_method() {
        String result = N.replaceRange("hello world", 5, 6, " my ");
        assertEquals("hello my world", result);
    }

    @Test
    public void testMoveRange_boolean() {
        boolean[] arr = { true, false, true, false, true };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr);
    }

    @Test
    public void testMoveRange_char() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'e', 'b', 'c' }, arr);

        char[] arr2 = { 'a', 'b', 'c' };
        N.moveRange(arr2, 1, 1, 0);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, arr2);

        N.moveRange(arr2, 1, 2, 1);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, arr2);
    }

    @Test
    public void testMoveRange_byte() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new byte[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_short() {
        short[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_int() {
        int[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new int[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_long() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new long[] { 1L, 4L, 5L, 2L, 3L }, arr);
    }

    @Test
    public void testMoveRange_float() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f, 2.0f, 3.0f }, arr);
    }

    @Test
    public void testMoveRange_double() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0, 2.0, 3.0 }, arr);
    }

    @Test
    public void testMoveRange_generic() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_List() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(N.moveRange(list, 1, 3, 3));
        assertEquals(Arrays.asList(1, 4, 5, 2, 3), list);

        assertFalse(N.moveRange(list, 1, 1, 0));
    }

    @Test
    public void testMoveRange_String_method() {
        String result = N.moveRange("hello", 1, 3, 3);
        assertEquals("hloel", result);
    }

    @Test
    public void testSkipRange_generic() {
        Integer[] result = N.skipRange(integerArray, 1, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5 }, result);

        assertNull(N.skipRange((Integer[]) null, 0, 0));

        assertArrayEquals(integerArray.clone(), N.skipRange(integerArray, 0, 0));
    }

    @Test
    public void testSkipRange_Collection() {
        List<Integer> result = N.skipRange(integerList, 1, 3);
        assertEquals(Arrays.asList(1, 4, 5), result);

        Set<Integer> resultSet = N.skipRange(integerList, 1, 3, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList(1, 4, 5)), resultSet);
    }

    @Test
    public void testHasDuplicates_boolean() {
        boolean[] noDups = { true, false };
        boolean[] hasDups = { true, true, false };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new boolean[] {}));
    }

    @Test
    public void testHasDuplicates_char() {
        char[] noDups = { 'a', 'b', 'c' };
        char[] hasDups = { 'a', 'b', 'a' };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new char[] {}));

        char[] sortedDups = { 'a', 'a', 'b' };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_byte() {
        byte[] noDups = { 1, 2, 3 };
        byte[] hasDups = { 1, 2, 1 };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new byte[] {}));

        byte[] sortedDups = { 1, 1, 2 };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_short() {
        short[] noDups = { 1, 2, 3 };
        short[] hasDups = { 1, 2, 1 };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new short[] {}));

        short[] sortedDups = { 1, 1, 2 };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_int() {
        int[] noDups = { 1, 2, 3 };
        int[] hasDups = { 1, 2, 1 };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new int[] {}));

        int[] sortedDups = { 1, 1, 2 };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_long() {
        long[] noDups = { 1L, 2L, 3L };
        long[] hasDups = { 1L, 2L, 1L };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new long[] {}));

        long[] sortedDups = { 1L, 1L, 2L };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_float() {
        float[] noDups = { 1.0f, 2.0f, 3.0f };
        float[] hasDups = { 1.0f, 2.0f, 1.0f };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new float[] {}));

        float[] sortedDups = { 1.0f, 1.0f, 2.0f };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_double() {
        double[] noDups = { 1.0, 2.0, 3.0 };
        double[] hasDups = { 1.0, 2.0, 1.0 };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new double[] {}));

        double[] sortedDups = { 1.0, 1.0, 2.0 };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_generic() {
        String[] noDups = { "a", "b", "c" };
        String[] hasDups = { "a", "b", "a" };

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(new String[] {}));

        String[] sortedDups = { "a", "a", "b" };
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_Collection() {
        List<String> noDups = Arrays.asList("a", "b", "c");
        List<String> hasDups = Arrays.asList("a", "b", "a");

        assertFalse(N.containsDuplicates(noDups));
        assertTrue(N.containsDuplicates(hasDups));
        assertFalse(N.containsDuplicates(Collections.emptyList()));

        List<String> sortedDups = Arrays.asList("a", "a", "b");
        assertTrue(N.containsDuplicates(sortedDups, true));
    }

    @Test
    public void testSum_char() {
        assertEquals(294, N.sum('a', 'b', 'c'));
        assertEquals(0, N.sum(new char[] {}));

        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(N.sum('b', 'c', 'd'), N.sum(chars, 1, 4));
    }

    @Test
    public void testSum_byte() {
        assertEquals(15, N.sum(byteArray));
        assertEquals(0, N.sum(new byte[] {}));
        assertEquals(9, N.sum(byteArray, 1, 4));
    }

    @Test
    public void testSum_short() {
        assertEquals(15, N.sum(shortArray));
        assertEquals(0, N.sum(new short[] {}));
        assertEquals(9, N.sum(shortArray, 1, 4));
    }

    @Test
    public void testSum_int() {
        assertEquals(15, N.sum(intArray));
        assertEquals(0, N.sum(new int[] {}));
        assertEquals(9, N.sum(intArray, 1, 4));
    }

    @Test
    public void testSumToLong_int() {
        assertEquals(15L, N.sumToLong(intArray));
        assertEquals(0L, N.sumToLong(new int[] {}));
        assertEquals(9L, N.sumToLong(intArray, 1, 4));
    }

    @Test
    public void testSum_long() {
        assertEquals(15L, N.sum(longArray));
        assertEquals(0L, N.sum(new long[] {}));
        assertEquals(9L, N.sum(longArray, 1, 4));
    }

    @Test
    public void testSum_float() {
        assertEquals(15.0f, N.sum(floatArray), 0.001);
        assertEquals(0f, N.sum(new float[] {}), 0.001);
        assertEquals(9.0f, N.sum(floatArray, 1, 4), 0.001);
    }

    @Test
    public void testSumToDouble_float() {
        assertEquals(15.0, N.sumToDouble(floatArray), 0.001);
        assertEquals(0.0, N.sumToDouble(new float[] {}), 0.001);
        assertEquals(9.0, N.sumToDouble(floatArray, 1, 4), 0.001);
    }

    @Test
    public void testSum_double() {
        assertEquals(15.0, N.sum(doubleArray), 0.001);
        assertEquals(0.0, N.sum(new double[] {}), 0.001);
        assertEquals(9.0, N.sum(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_char() {
        char[] chars = { 'a', 'b', 'c' };
        assertEquals(98.0, N.average(chars), 0.001);
        assertEquals(0.0, N.average(new char[] {}), 0.001);
        assertEquals(98.5, N.average(chars, 1, 3), 0.001);
    }

    @Test
    public void testAverage_byte() {
        assertEquals(3.0, N.average(byteArray), 0.001);
        assertEquals(0.0, N.average(new byte[] {}), 0.001);
        assertEquals(3.0, N.average(byteArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_short() {
        assertEquals(3.0, N.average(shortArray), 0.001);
        assertEquals(0.0, N.average(new short[] {}), 0.001);
        assertEquals(3.0, N.average(shortArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_int() {
        assertEquals(3.0, N.average(intArray), 0.001);
        assertEquals(0.0, N.average(new int[] {}), 0.001);
        assertEquals(3.0, N.average(intArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_long() {
        assertEquals(3.0, N.average(longArray), 0.001);
        assertEquals(0.0, N.average(new long[] {}), 0.001);
        assertEquals(3.0, N.average(longArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_float() {
        assertEquals(3.0, N.average(floatArray), 0.001);
        assertEquals(0.0, N.average(new float[] {}), 0.001);
        assertEquals(3.0, N.average(floatArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_double() {
        assertEquals(3.0, N.average(doubleArray), 0.001);
        assertEquals(0.0, N.average(new double[] {}), 0.001);
        assertEquals(3.0, N.average(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testSumInt_array() {
        assertEquals(15, N.sumInt(integerArray));
        assertEquals(9, N.sumInt(integerArray, 1, 4));
        assertEquals(30, N.sumInt(integerArray, x -> x * 2));
        assertEquals(18, N.sumInt(integerArray, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumInt_collection() {
        assertEquals(9, N.sumInt(integerList, 1, 4));
        assertEquals(18, N.sumInt(integerList, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumInt_iterable() {
        assertEquals(15, N.sumInt(integerList));
        assertEquals(30, N.sumInt(integerList, x -> x * 2));
    }

    @Test
    public void testSumIntToLong_iterable() {
        assertEquals(15L, N.sumIntToLong(integerList));
        assertEquals(30L, N.sumIntToLong(integerList, x -> x * 2));
    }

    @Test
    public void testSumLong_array() {
        Long[] longObjArray = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(15L, N.sumLong(longObjArray));
        assertEquals(9L, N.sumLong(longObjArray, 1, 4));
        assertEquals(30L, N.sumLong(longObjArray, x -> x * 2));
        assertEquals(18L, N.sumLong(longObjArray, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumLong_collection() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(9L, N.sumLong(longList, 1, 4));
        assertEquals(18L, N.sumLong(longList, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumLong_iterable() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(15L, N.sumLong(longList));
        assertEquals(30L, N.sumLong(longList, x -> x * 2));
    }

    @Test
    public void testSumDouble_array() {
        Double[] doubleObjArray = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertEquals(15.0, N.sumDouble(doubleObjArray), 0.001);
        assertEquals(9.0, N.sumDouble(doubleObjArray, 1, 4), 0.001);
        assertEquals(30.0, N.sumDouble(doubleObjArray, x -> x * 2), 0.001);
        assertEquals(18.0, N.sumDouble(doubleObjArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_collection() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(9.0, N.sumDouble(doubleList, 1, 4), 0.001);
        assertEquals(18.0, N.sumDouble(doubleList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_iterable() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(15.0, N.sumDouble(doubleList), 0.001);
        assertEquals(30.0, N.sumDouble(doubleList, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_array() {
        assertEquals(3.0, N.averageInt(integerArray), 0.001);
        assertEquals(3.0, N.averageInt(integerArray, 1, 4), 0.001);
        assertEquals(6.0, N.averageInt(integerArray, x -> x * 2), 0.001);
        assertEquals(6.0, N.averageInt(integerArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_collection() {
        assertEquals(3.0, N.averageInt(integerList, 1, 4), 0.001);
        assertEquals(6.0, N.averageInt(integerList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_iterable() {
        assertEquals(3.0, N.averageInt(integerList), 0.001);
        assertEquals(6.0, N.averageInt(integerList, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_array() {
        Long[] longObjArray = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(3.0, N.averageLong(longObjArray), 0.001);
        assertEquals(3.0, N.averageLong(longObjArray, 1, 4), 0.001);
        assertEquals(6.0, N.averageLong(longObjArray, x -> x * 2), 0.001);
        assertEquals(6.0, N.averageLong(longObjArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_collection() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(3.0, N.averageLong(longList, 1, 4), 0.001);
        assertEquals(6.0, N.averageLong(longList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_iterable() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(3.0, N.averageLong(longList), 0.001);
        assertEquals(6.0, N.averageLong(longList, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_array() {
        Double[] doubleObjArray = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertEquals(3.0, N.averageDouble(doubleObjArray), 0.001);
        assertEquals(3.0, N.averageDouble(doubleObjArray, 1, 4), 0.001);
        assertEquals(6.0, N.averageDouble(doubleObjArray, x -> x * 2), 0.001);
        assertEquals(6.0, N.averageDouble(doubleObjArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_collection() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(3.0, N.averageDouble(doubleList, 1, 4), 0.001);
        assertEquals(6.0, N.averageDouble(doubleList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_iterable() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(3.0, N.averageDouble(doubleList), 0.001);
        assertEquals(6.0, N.averageDouble(doubleList, x -> x * 2), 0.001);
    }

    @Test
    public void testMin_primitives() {
        assertEquals('a', N.min('a', 'b'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2));
        assertEquals((short) 1, N.min((short) 1, (short) 2));
        assertEquals(1, N.min(1, 2));
        assertEquals(1L, N.min(1L, 2L));
        assertEquals(1.0f, N.min(1.0f, 2.0f), 0.001);
        assertEquals(1.0, N.min(1.0, 2.0), 0.001);
    }

    @Test
    public void testMin_three_primitives() {
        assertEquals('a', N.min('a', 'b', 'c'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3));
        assertEquals(1, N.min(1, 2, 3));
        assertEquals(1L, N.min(1L, 2L, 3L));
        assertEquals(1.0f, N.min(1.0f, 2.0f, 3.0f), 0.001);
        assertEquals(1.0, N.min(1.0, 2.0, 3.0), 0.001);
    }

    @Test
    public void testMin_comparable() {
        assertEquals("a", N.min("a", "b"));
        assertEquals("a", N.min("a", "b", "c"));

        assertEquals("a", N.min(null, "a"));
        assertEquals("a", N.min("a", null));
    }

    @Test
    public void testMin_withComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        assertEquals("b", N.min("a", "b", reverseComparator));
        assertEquals("c", N.min("a", "b", "c", reverseComparator));
    }

    @Test
    public void testMin_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.min(new char[] {}));

        assertEquals('a', N.min(charArray));
        assertEquals('b', N.min(charArray, 1, 4));

        assertEquals((byte) 1, N.min(byteArray));
        assertEquals((byte) 2, N.min(byteArray, 1, 4));

        assertEquals((short) 1, N.min(shortArray));
        assertEquals((short) 2, N.min(shortArray, 1, 4));

        assertEquals(1, N.min(intArray));
        assertEquals(2, N.min(intArray, 1, 4));

        assertEquals(1L, N.min(longArray));
        assertEquals(2L, N.min(longArray, 1, 4));

        assertEquals(1.0f, N.min(floatArray), 0.001);
        assertEquals(2.0f, N.min(floatArray, 1, 4), 0.001);

        assertEquals(1.0, N.min(doubleArray), 0.001);
        assertEquals(2.0, N.min(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testMin_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.min(new String[] {}));

        assertEquals("five", N.min(stringArray));
        assertEquals("four", N.min(stringArray, 1, 4));
        assertEquals("five", N.min(stringArray, Comparator.naturalOrder()));
        assertEquals("two", N.min(stringArray, Comparator.reverseOrder()));
    }

    @Test
    public void testMin_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.emptyList(), 0, 0));

        assertEquals("five", N.min(stringList, 0, 5));
        assertEquals("four", N.min(stringList, 1, 4));
        assertEquals("five", N.min(stringList, 0, 5, Comparator.naturalOrder()));
    }

    @Test
    public void testMin_iterable() {
        assertEquals("five", N.min(stringList));
        assertEquals("five", N.min(stringList, Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<String> emptyList()));
    }

    @Test
    public void testMin_iterator() {
        assertEquals("five", N.min(stringList.iterator()));
        assertEquals("five", N.min(stringList.iterator(), Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<String> emptyList().iterator()));
    }

    @Test
    public void testMinMax_array() {
        assertThrows(IllegalArgumentException.class, () -> N.minMax(new String[] {}));

        String[] singleElement = { "a" };
        assertEquals(Pair.of("a", "a"), N.minMax(singleElement));

        assertEquals(Pair.of("five", "two"), N.minMax(stringArray));
        assertEquals(Pair.of("five", "two"), N.minMax(stringArray, Comparator.naturalOrder()));
    }

    @Test
    public void testMinMax_iterable() {
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.emptyList()));

        assertEquals(Pair.of("five", "two"), N.minMax(stringList));
        assertEquals(Pair.of("five", "two"), N.minMax(stringList, Comparator.naturalOrder()));
    }

    @Test
    public void testMinMax_iterator() {
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.<String> emptyList().iterator()));

        assertEquals(Pair.of("five", "two"), N.minMax(stringList.iterator()));
        assertEquals(Pair.of("five", "two"), N.minMax(stringList.iterator(), Comparator.naturalOrder()));
    }

    @Test
    public void testMax_primitives() {
        assertEquals('b', N.max('a', 'b'));
        assertEquals((byte) 2, N.max((byte) 1, (byte) 2));
        assertEquals((short) 2, N.max((short) 1, (short) 2));
        assertEquals(2, N.max(1, 2));
        assertEquals(2L, N.max(1L, 2L));
        assertEquals(2.0f, N.max(1.0f, 2.0f), 0.001);
        assertEquals(2.0, N.max(1.0, 2.0), 0.001);
    }

    @Test
    public void testMax_three_primitives() {
        assertEquals('c', N.max('a', 'b', 'c'));
        assertEquals((byte) 3, N.max((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 3, N.max((short) 1, (short) 2, (short) 3));
        assertEquals(3, N.max(1, 2, 3));
        assertEquals(3L, N.max(1L, 2L, 3L));
        assertEquals(3.0f, N.max(1.0f, 2.0f, 3.0f), 0.001);
        assertEquals(3.0, N.max(1.0, 2.0, 3.0), 0.001);
    }

    @Test
    public void testMax_comparable() {
        assertEquals("b", N.max("a", "b"));
        assertEquals("c", N.max("a", "b", "c"));

        assertEquals("a", N.max(null, "a"));
        assertEquals("a", N.max("a", null));
    }

    @Test
    public void testMax_withComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        assertEquals("a", N.max("a", "b", reverseComparator));
        assertEquals("a", N.max("a", "b", "c", reverseComparator));
    }

    @Test
    public void testMax_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.max(new char[] {}));

        assertEquals('e', N.max(charArray));
        assertEquals('d', N.max(charArray, 1, 4));

        assertEquals((byte) 5, N.max(byteArray));
        assertEquals((byte) 4, N.max(byteArray, 1, 4));

        assertEquals((short) 5, N.max(shortArray));
        assertEquals((short) 4, N.max(shortArray, 1, 4));

        assertEquals(5, N.max(intArray));
        assertEquals(4, N.max(intArray, 1, 4));

        assertEquals(5L, N.max(longArray));
        assertEquals(4L, N.max(longArray, 1, 4));

        assertEquals(5.0f, N.max(floatArray), 0.001);
        assertEquals(4.0f, N.max(floatArray, 1, 4), 0.001);

        assertEquals(5.0, N.max(doubleArray), 0.001);
        assertEquals(4.0, N.max(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testMax_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.max(new String[] {}));

        assertEquals("two", N.max(stringArray));
        assertEquals("two", N.max(stringArray, 1, 4));
        assertEquals("two", N.max(stringArray, Comparator.naturalOrder()));
        assertEquals("five", N.max(stringArray, Comparator.reverseOrder()));
    }

    @Test
    public void testMax_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.emptyList(), 0, 0));

        assertEquals("two", N.max(stringList, 0, 5));
        assertEquals("two", N.max(stringList, 1, 4));
        assertEquals("two", N.max(stringList, 0, 5, Comparator.naturalOrder()));
    }

    @Test
    public void testMax_iterable() {
        assertEquals("two", N.max(stringList));
        assertEquals("two", N.max(stringList, Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<String> emptyList()));
    }

    @Test
    public void testMax_iterator() {
        assertEquals("two", N.max(stringList.iterator()));
        assertEquals("two", N.max(stringList.iterator(), Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<String> emptyList().iterator()));
    }

    @Test
    public void testMedian_three_primitives() {
        assertEquals('b', N.median('a', 'b', 'c'));
        assertEquals('b', N.median('c', 'b', 'a'));

        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
        assertEquals((byte) 2, N.median((byte) 3, (byte) 2, (byte) 1));

        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
        assertEquals((short) 2, N.median((short) 3, (short) 2, (short) 1));

        assertEquals(2, N.median(1, 2, 3));
        assertEquals(2, N.median(3, 2, 1));

        assertEquals(2L, N.median(1L, 2L, 3L));
        assertEquals(2L, N.median(3L, 2L, 1L));

        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f), 0.001);
        assertEquals(2.0f, N.median(3.0f, 2.0f, 1.0f), 0.001);

        assertEquals(2.0, N.median(1.0, 2.0, 3.0), 0.001);
        assertEquals(2.0, N.median(3.0, 2.0, 1.0), 0.001);
    }

    @Test
    public void testMedian_three_comparable() {
        assertEquals("b", N.median("a", "b", "c"));
        assertEquals("b", N.median("c", "b", "a"));

        assertEquals("bee", N.median("ant", "bee", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("bee", "ant", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("bee", "tiger", "ant", Comparator.comparing(String::length)));
        assertEquals("bee", N.median("tiger", "bee", "ant", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("be", "ant", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("be", "tiger", "ant", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("tiger", "be", "ant", Comparator.comparing(String::length)));
    }

    @Test
    public void testMedian_three_withComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        assertEquals("b", N.median("a", "b", "c", reverseComparator));
        assertEquals("b", N.median("c", "b", "a", reverseComparator));
    }

    @Test
    public void testMedian_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.median(new char[] {}));

        assertEquals('a', N.median(new char[] { 'a' }));
        assertEquals('a', N.median(new char[] { 'a', 'b' }));
        assertEquals('b', N.median(new char[] { 'a', 'b', 'c' }));
        assertEquals('c', N.median(charArray));
        assertEquals('c', N.median(charArray, 1, 4));

        assertEquals((byte) 3, N.median(byteArray));
        assertEquals((byte) 3, N.median(byteArray, 1, 4));

        assertEquals((short) 3, N.median(shortArray));
        assertEquals((short) 3, N.median(shortArray, 1, 4));

        assertEquals(3, N.median(intArray));
        assertEquals(3, N.median(intArray, 1, 4));

        assertEquals(3L, N.median(longArray));
        assertEquals(3L, N.median(longArray, 1, 4));

        assertEquals(3.0f, N.median(floatArray), 0.001);
        assertEquals(3.0f, N.median(floatArray, 1, 4), 0.001);

        assertEquals(3.0, N.median(doubleArray), 0.001);
        assertEquals(3.0, N.median(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testMedian_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.median(new String[] {}));

        String[] sorted = { "a", "b", "c", "d", "e" };
        assertEquals("c", N.median(sorted));
        assertEquals("c", N.median(sorted, 1, 4));
        assertEquals("c", N.median(sorted, Comparator.naturalOrder()));
        assertEquals("c", N.median(sorted, 0, 5, Comparator.naturalOrder()));

        assertEquals("bee", N.median(Array.of("ant", "bee", "tiger"), Comparator.comparing(String::length)));
    }

    @Test
    public void testMedian_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.median(Collections.emptyList()));

        List<String> sorted = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals("c", N.median(sorted));
        assertEquals("c", N.median(sorted, 1, 4));
        assertEquals("c", N.median(sorted, Comparator.naturalOrder()));
        assertEquals("c", N.median(sorted, 0, 5, Comparator.naturalOrder()));
        assertEquals("bee", N.median(CommonUtil.toList("ant", "bee", "tiger"), Comparator.comparing(String::length)));
    }

    @Test
    public void testKthLargest_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new char[] {}, 1));

        assertEquals('e', N.kthLargest(charArray, 1));
        assertEquals('a', N.kthLargest(charArray, 5));
        assertEquals('c', N.kthLargest(charArray, 3));
        assertEquals('d', N.kthLargest(charArray, 1, 4, 1));

        assertEquals((byte) 5, N.kthLargest(byteArray, 1));
        assertEquals((byte) 1, N.kthLargest(byteArray, 5));
        assertEquals((byte) 3, N.kthLargest(byteArray, 3));

        assertEquals((short) 5, N.kthLargest(shortArray, 1));
        assertEquals((short) 1, N.kthLargest(shortArray, 5));
        assertEquals((short) 3, N.kthLargest(shortArray, 3));

        assertEquals(5, N.kthLargest(intArray, 1));
        assertEquals(1, N.kthLargest(intArray, 5));
        assertEquals(3, N.kthLargest(intArray, 3));

        assertEquals(5L, N.kthLargest(longArray, 1));
        assertEquals(1L, N.kthLargest(longArray, 5));
        assertEquals(3L, N.kthLargest(longArray, 3));

        assertEquals(5.0f, N.kthLargest(floatArray, 1), 0.001);
        assertEquals(1.0f, N.kthLargest(floatArray, 5), 0.001);
        assertEquals(3.0f, N.kthLargest(floatArray, 3), 0.001);

        assertEquals(5.0, N.kthLargest(doubleArray, 1), 0.001);
        assertEquals(1.0, N.kthLargest(doubleArray, 5), 0.001);
        assertEquals(3.0, N.kthLargest(doubleArray, 3), 0.001);
    }

    @Test
    public void testKthLargest_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new String[] {}, 1));

        String[] arr = { "a", "b", "c", "d", "e" };
        assertEquals("e", N.kthLargest(arr, 1));
        assertEquals("a", N.kthLargest(arr, 5));
        assertEquals("c", N.kthLargest(arr, 3));
        assertEquals("d", N.kthLargest(arr, 1, 4, 1));
        assertEquals("e", N.kthLargest(arr, 1, Comparator.naturalOrder()));
        assertEquals("c", N.kthLargest(arr, 0, 5, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargest_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(Collections.emptyList(), 1));

        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals("e", N.kthLargest(list, 1));
        assertEquals("a", N.kthLargest(list, 5));
        assertEquals("c", N.kthLargest(list, 3));
        assertEquals("d", N.kthLargest(list, 1, 4, 1));
        assertEquals("e", N.kthLargest(list, 1, Comparator.naturalOrder()));
        assertEquals("c", N.kthLargest(list, 0, 5, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testTop_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.top(shortArray, -1));

        assertArrayEquals(new short[] {}, N.top(new short[] {}, 3));
        assertArrayEquals(new short[] {}, N.top(shortArray, 0));
        assertArrayEquals(shortArray.clone(), N.top(shortArray, 10));
        assertArrayEquals(new short[] { 3, 4, 5 }, N.top(shortArray, 3));
        assertArrayEquals(new short[] { 3, 4 }, N.top(shortArray, 1, 4, 2));
        assertArrayEquals(new short[] { 3, 4, 5 }, N.top(shortArray, 3, Comparator.naturalOrder()));
        assertArrayEquals(new short[] { 3, 4 }, N.top(shortArray, 1, 4, 2, Comparator.naturalOrder()));

        assertArrayEquals(new int[] { 3, 4, 5 }, N.top(intArray, 3));
        assertArrayEquals(new long[] { 3, 4, 5 }, N.top(longArray, 3));
        assertArrayEquals(new float[] { 3, 4, 5 }, N.top(floatArray, 3));
        assertArrayEquals(new double[] { 3, 4, 5 }, N.top(doubleArray, 3));
    }

    @Test
    public void testTop_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.top(stringArray, -1));

        List<String> result = N.top(stringArray, 3);
        assertEquals(3, result.size());
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));

        result = N.top(stringArray, 1, 4, 2);
        assertEquals(2, result.size());

        result = N.top(stringArray, 3, Comparator.naturalOrder());
        assertEquals(3, result.size());

        String[] arr = { "d", "b", "e", "a", "c" };
        result = N.top(arr, 3, true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(arr, 3, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(arr, 1, 4, 2, true);
        assertEquals(Arrays.asList("b", "e"), result);

        result = N.top(arr, 1, 4, 2, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("b", "e"), result);
    }

    @Test
    public void testTop_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.top(stringList, -1));

        List<String> result = N.top(stringList, 3);
        assertEquals(3, result.size());
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));

        result = N.top(stringList, 1, 4, 2);
        assertEquals(2, result.size());

        result = N.top(stringList, 3, Comparator.naturalOrder());
        assertEquals(3, result.size());

        List<String> list = Arrays.asList("d", "b", "e", "a", "c");
        result = N.top(list, 3, true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(list, 3, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(list, 1, 4, 2, true);
        assertEquals(Arrays.asList("b", "e"), result);

        result = N.top(list, 1, 4, 2, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("b", "e"), result);
    }

    @Test
    public void testPercentiles_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new char[] {}));

        char[] sortedChars = { 'a', 'b', 'c', 'd', 'e' };
        Map<Percentage, Character> charPercentiles = N.percentilesOfSorted(sortedChars);
        assertNotNull(charPercentiles);
        assertEquals(Percentage.values().length, charPercentiles.size());

        byte[] sortedBytes = { 1, 2, 3, 4, 5 };
        Map<Percentage, Byte> bytePercentiles = N.percentilesOfSorted(sortedBytes);
        assertNotNull(bytePercentiles);
        assertEquals(Percentage.values().length, bytePercentiles.size());

        short[] sortedShorts = { 1, 2, 3, 4, 5 };
        Map<Percentage, Short> shortPercentiles = N.percentilesOfSorted(sortedShorts);
        assertNotNull(shortPercentiles);
        assertEquals(Percentage.values().length, shortPercentiles.size());

        int[] sortedInts = new int[100];
        for (int i = 0; i < 100; i++) {
            sortedInts[i] = i + 1;
        }
        Map<Percentage, Integer> intPercentiles = N.percentilesOfSorted(sortedInts);
        assertNotNull(intPercentiles);
        assertEquals(2, intPercentiles.get(Percentage._1).intValue());
        assertEquals(51, intPercentiles.get(Percentage._50).intValue());
        assertEquals(100, intPercentiles.get(Percentage._99).intValue());

        long[] sortedLongs = { 1L, 2L, 3L, 4L, 5L };
        Map<Percentage, Long> longPercentiles = N.percentilesOfSorted(sortedLongs);
        assertNotNull(longPercentiles);
        assertEquals(Percentage.values().length, longPercentiles.size());

        float[] sortedFloats = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Map<Percentage, Float> floatPercentiles = N.percentilesOfSorted(sortedFloats);
        assertNotNull(floatPercentiles);
        assertEquals(Percentage.values().length, floatPercentiles.size());

        double[] sortedDoubles = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Map<Percentage, Double> doublePercentiles = N.percentilesOfSorted(sortedDoubles);
        assertNotNull(doublePercentiles);
        assertEquals(Percentage.values().length, doublePercentiles.size());
    }

    @Test
    public void testPercentiles_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new String[] {}));

        String[] sortedStrings = { "a", "b", "c", "d", "e" };
        Map<Percentage, String> stringPercentiles = N.percentilesOfSorted(sortedStrings);
        assertNotNull(stringPercentiles);
        assertEquals(Percentage.values().length, stringPercentiles.size());
    }

    @Test
    public void testPercentiles_list() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.emptyList()));

        List<String> sortedList = Arrays.asList("a", "b", "c", "d", "e");
        Map<Percentage, String> listPercentiles = N.percentilesOfSorted(sortedList);
        assertNotNull(listPercentiles);
        assertEquals(Percentage.values().length, listPercentiles.size());
    }

    @Test
    public void testReplaceAllObjectArrayWithOperator() {
        String[] arr = { "a", "b", "c", "d" };
        N.replaceAll(arr, String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C", "D" }, arr);
    }

    @Test
    public void testAddString() {
        String[] arr = { "a", "b" };
        String[] result = N.add(arr, "c");
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testAddObject() {
        Integer[] arr = { 1, 2 };
        Integer[] result = N.add(arr, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAddAllBoolean() {
        boolean[] arr = { true, false };
        boolean[] result = N.addAll(arr, true, false);
        assertArrayEquals(new boolean[] { true, false, true, false }, result);

        result = N.addAll(new boolean[0], true, false);
        assertArrayEquals(new boolean[] { true, false }, result);

        result = N.addAll(arr, new boolean[0]);
        assertArrayEquals(arr, result);
    }

    @Test
    public void testAddAllChar() {
        char[] arr = { 'a', 'b' };
        char[] result = N.addAll(arr, 'c', 'd');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testAddAllByte() {
        byte[] arr = { 1, 2 };
        byte[] result = N.addAll(arr, (byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAddAllShort() {
        short[] arr = { 1, 2 };
        short[] result = N.addAll(arr, (short) 3, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAddAllInt() {
        int[] arr = { 1, 2 };
        int[] result = N.addAll(arr, 3, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAddAllLong() {
        long[] arr = { 1L, 2L };
        long[] result = N.addAll(arr, 3L, 4L);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testAddAllFloat() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = N.addAll(arr, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testAddAllDouble() {
        double[] arr = { 1.0, 2.0 };
        double[] result = N.addAll(arr, 3.0, 4.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testAddAllString() {
        String[] arr = { "a", "b" };
        String[] result = N.addAll(arr, "c", "d");
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);
    }

    @Test
    public void testAddAllObject() {
        Integer[] arr = { 1, 2 };
        Integer[] result = N.addAll(arr, 3, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testInsertChar() {
        char[] arr = { 'a', 'b', 'c' };
        char[] result = N.insert(arr, 1, 'x');
        assertArrayEquals(new char[] { 'a', 'x', 'b', 'c' }, result);
    }

    @Test
    public void testInsertByte() {
        byte[] arr = { 1, 2, 3 };
        byte[] result = N.insert(arr, 1, (byte) 9);
        assertArrayEquals(new byte[] { 1, 9, 2, 3 }, result);
    }

    @Test
    public void testInsertShort() {
        short[] arr = { 1, 2, 3 };
        short[] result = N.insert(arr, 1, (short) 9);
        assertArrayEquals(new short[] { 1, 9, 2, 3 }, result);
    }

    @Test
    public void testInsertLong() {
        long[] arr = { 1L, 2L, 3L };
        long[] result = N.insert(arr, 1, 9L);
        assertArrayEquals(new long[] { 1L, 9L, 2L, 3L }, result);
    }

    @Test
    public void testInsertFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        float[] result = N.insert(arr, 1, 9.0f);
        assertArrayEquals(new float[] { 1.0f, 9.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testInsertDouble() {
        double[] arr = { 1.0, 2.0, 3.0 };
        double[] result = N.insert(arr, 1, 9.0);
        assertArrayEquals(new double[] { 1.0, 9.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testInsertObject() {
        Integer[] arr = { 1, 2, 3 };
        Integer[] result = N.insert(arr, 1, 9);
        assertArrayEquals(new Integer[] { 1, 9, 2, 3 }, result);
    }

    @Test
    public void testInsertInvalidIndex() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(arr, 4, 9));
    }

    @Test
    public void testInsertAllBoolean() {
        boolean[] arr = { true, false };
        boolean[] result = N.insertAll(arr, 1, true, false);
        assertArrayEquals(new boolean[] { true, true, false, false }, result);
    }

    @Test
    public void testInsertAllChar() {
        char[] arr = { 'a', 'b' };
        char[] result = N.insertAll(arr, 1, 'x', 'y');
        assertArrayEquals(new char[] { 'a', 'x', 'y', 'b' }, result);
    }

    @Test
    public void testInsertAllByte() {
        byte[] arr = { 1, 2 };
        byte[] result = N.insertAll(arr, 1, (byte) 8, (byte) 9);
        assertArrayEquals(new byte[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllShort() {
        short[] arr = { 1, 2 };
        short[] result = N.insertAll(arr, 1, (short) 8, (short) 9);
        assertArrayEquals(new short[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllInt() {
        int[] arr = { 1, 2 };
        int[] result = N.insertAll(arr, 1, 8, 9);
        assertArrayEquals(new int[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllLong() {
        long[] arr = { 1L, 2L };
        long[] result = N.insertAll(arr, 1, 8L, 9L);
        assertArrayEquals(new long[] { 1L, 8L, 9L, 2L }, result);
    }

    @Test
    public void testInsertAllFloat() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = N.insertAll(arr, 1, 8.0f, 9.0f);
        assertArrayEquals(new float[] { 1.0f, 8.0f, 9.0f, 2.0f }, result, 0.001f);
    }

    @Test
    public void testInsertAllDouble() {
        double[] arr = { 1.0, 2.0 };
        double[] result = N.insertAll(arr, 1, 8.0, 9.0);
        assertArrayEquals(new double[] { 1.0, 8.0, 9.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testInsertAllString() {
        String[] arr = { "a", "b" };
        String[] result = N.insertAll(arr, 1, "x", "y");
        assertArrayEquals(new String[] { "a", "x", "y", "b" }, result);
    }

    @Test
    public void testInsertAllObject() {
        Integer[] arr = { 1, 2 };
        Integer[] result = N.insertAll(arr, 1, 8, 9);
        assertArrayEquals(new Integer[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.insertAll(list, 1, "x", "y");
        assertTrue(result);
        assertEquals(Arrays.asList("a", "x", "y", "b", "c"), list);
    }

    @Test
    public void testDeleteByIndexChar() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        char[] result = N.removeAt(arr, 1);
        assertArrayEquals(new char[] { 'a', 'c', 'd' }, result);
    }

    @Test
    public void testDeleteByIndexByte() {
        byte[] arr = { 1, 2, 3, 4 };
        byte[] result = N.removeAt(arr, 1);
        assertArrayEquals(new byte[] { 1, 3, 4 }, result);
    }

    @Test
    public void testDeleteByIndexShort() {
        short[] arr = { 1, 2, 3, 4 };
        short[] result = N.removeAt(arr, 1);
        assertArrayEquals(new short[] { 1, 3, 4 }, result);
    }

    @Test
    public void testDeleteByIndexLong() {
        long[] arr = { 1L, 2L, 3L, 4L };
        long[] result = N.removeAt(arr, 1);
        assertArrayEquals(new long[] { 1L, 3L, 4L }, result);
    }

    @Test
    public void testDeleteByIndexFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] result = N.removeAt(arr, 1);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testDeleteByIndexDouble() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        double[] result = N.removeAt(arr, 1);
        assertArrayEquals(new double[] { 1.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testDeleteByIndexObject() {
        Integer[] arr = { 1, 2, 3, 4 };
        Integer[] result = N.removeAt(arr, 1);
        assertArrayEquals(new Integer[] { 1, 3, 4 }, result);
    }

    @Test
    public void testDeleteByIndexInvalidIndex() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(arr, 3));
    }

    @Test
    public void testDeleteAllByIndicesChar() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        char[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, result);
    }

    @Test
    public void testDeleteAllByIndicesByte() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        byte[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new byte[] { 1, 3, 5 }, result);
    }

    @Test
    public void testDeleteAllByIndicesShort() {
        short[] arr = { 1, 2, 3, 4, 5 };
        short[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    public void testDeleteAllByIndicesLong() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        long[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new long[] { 1L, 3L, 5L }, result);
    }

    @Test
    public void testDeleteAllByIndicesFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, result, 0.001f);
    }

    @Test
    public void testDeleteAllByIndicesDouble() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testDeleteAllByIndicesString() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new String[] { "a", "c", "e" }, result);
    }

    @Test
    public void testDeleteAllByIndicesObject() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Integer[] result = N.removeAt(arr, 1, 3);
        assertArrayEquals(new Integer[] { 1, 3, 5 }, result);
    }

    @Test
    public void testRemoveChar() {
        char[] arr = { 'a', 'b', 'a', 'c' };
        char[] result = N.remove(arr, 'a');
        assertArrayEquals(new char[] { 'b', 'a', 'c' }, result);
    }

    @Test
    public void testRemoveByte() {
        byte[] arr = { 1, 2, 1, 3 };
        byte[] result = N.remove(arr, (byte) 1);
        assertArrayEquals(new byte[] { 2, 1, 3 }, result);
    }

    @Test
    public void testRemoveShort() {
        short[] arr = { 1, 2, 1, 3 };
        short[] result = N.remove(arr, (short) 1);
        assertArrayEquals(new short[] { 2, 1, 3 }, result);
    }

    @Test
    public void testRemoveLong() {
        long[] arr = { 1L, 2L, 1L, 3L };
        long[] result = N.remove(arr, 1L);
        assertArrayEquals(new long[] { 2L, 1L, 3L }, result);
    }

    @Test
    public void testRemoveFloat() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f };
        float[] result = N.remove(arr, 1.0f);
        assertArrayEquals(new float[] { 2.0f, 1.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveDouble() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0 };
        double[] result = N.remove(arr, 1.0);
        assertArrayEquals(new double[] { 2.0, 1.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRemoveString() {
        String[] arr = { "a", "b", "a", "c" };
        String[] result = N.remove(arr, "a");
        assertArrayEquals(new String[] { "b", "a", "c" }, result);
    }

    @Test
    public void testRemoveObject() {
        Integer[] arr = { 1, 2, 1, 3 };
        Integer[] result = N.remove(arr, 1);
        assertArrayEquals(new Integer[] { 2, 1, 3 }, result);
    }

    @Test
    public void testRemoveCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        boolean result = N.remove(list, "a");
        assertTrue(result);
        assertEquals(Arrays.asList("b", "a", "c"), list);

        result = N.remove(list, "x");
        assertFalse(result);
    }

    @Test
    public void testRemoveAllBoolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.removeAll(arr, true, false);
        assertEquals(0, result.length);

        result = N.removeAll(arr, true);
        assertArrayEquals(new boolean[] { false, false }, result);

        result = N.removeAll(arr);
        assertArrayEquals(arr, result);
    }

    @Test
    public void testRemoveAllChar() {
        char[] arr = { 'a', 'b', 'c', 'd', 'a' };
        char[] result = N.removeAll(arr, 'a', 'c');
        assertArrayEquals(new char[] { 'b', 'd' }, result);
    }

    @Test
    public void testRemoveAllByte() {
        byte[] arr = { 1, 2, 3, 4, 1 };
        byte[] result = N.removeAll(arr, (byte) 1, (byte) 3);
        assertArrayEquals(new byte[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllShort() {
        short[] arr = { 1, 2, 3, 4, 1 };
        short[] result = N.removeAll(arr, (short) 1, (short) 3);
        assertArrayEquals(new short[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllInt() {
        int[] arr = { 1, 2, 3, 4, 1 };
        int[] result = N.removeAll(arr, 1, 3);
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllLong() {
        long[] arr = { 1L, 2L, 3L, 4L, 1L };
        long[] result = N.removeAll(arr, 1L, 3L);
        assertArrayEquals(new long[] { 2L, 4L }, result);
    }

    @Test
    public void testRemoveAllFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 1.0f };
        float[] result = N.removeAll(arr, 1.0f, 3.0f);
        assertArrayEquals(new float[] { 2.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveAllDouble() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 1.0 };
        double[] result = N.removeAll(arr, 1.0, 3.0);
        assertArrayEquals(new double[] { 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testRemoveAllString() {
        String[] arr = { "a", "b", "c", "d", "a" };
        String[] result = N.removeAll(arr, "a", "c");
        assertArrayEquals(new String[] { "b", "d" }, result);
    }

    @Test
    public void testRemoveAllObject() {
        Integer[] arr = { 1, 2, 3, 4, 1 };
        Integer[] result = N.removeAll(arr, 1, 3);
        assertArrayEquals(new Integer[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "a"));
        boolean result = N.removeAll(list, "a", "c");
        assertTrue(result);
        assertEquals(Arrays.asList("b", "d"), list);
    }

    @Test
    public void testRemoveAllCollectionIterable() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "a"));
        List<String> toRemove = Arrays.asList("a", "c");
        boolean result = N.removeAll(list, toRemove);
        assertTrue(result);
        assertEquals(Arrays.asList("b", "d"), list);
    }

    @Test
    public void testRemoveAllCollectionIterator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "a"));
        List<String> toRemove = Arrays.asList("a", "c");
        boolean result = N.removeAll(list, toRemove.iterator());
        assertTrue(result);
        assertEquals(Arrays.asList("b", "d"), list);
    }

    @Test
    public void testRemoveAllOccurrencesChar() {
        char[] arr = { 'a', 'b', 'a', 'c', 'a' };
        char[] result = N.removeAllOccurrences(arr, 'a');
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testRemoveAllOccurrencesByte() {
        byte[] arr = { 1, 2, 1, 3, 1 };
        byte[] result = N.removeAllOccurrences(arr, (byte) 1);
        assertArrayEquals(new byte[] { 2, 3 }, result);
    }

    @Test
    public void testRemoveAllOccurrencesShort() {
        short[] arr = { 1, 2, 1, 3, 1 };
        short[] result = N.removeAllOccurrences(arr, (short) 1);
        assertArrayEquals(new short[] { 2, 3 }, result);
    }

    @Test
    public void testRemoveAllOccurrencesLong() {
        long[] arr = { 1L, 2L, 1L, 3L, 1L };
        long[] result = N.removeAllOccurrences(arr, 1L);
        assertArrayEquals(new long[] { 2L, 3L }, result);
    }

    @Test
    public void testRemoveAllOccurrencesFloat() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        float[] result = N.removeAllOccurrences(arr, 1.0f);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveAllOccurrencesDouble() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        double[] result = N.removeAllOccurrences(arr, 1.0);
        assertArrayEquals(new double[] { 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRemoveAllOccurrencesString() {
        String[] arr = { "a", "b", "a", "c", "a" };
        String[] result = N.removeAllOccurrences(arr, "a");
        assertArrayEquals(new String[] { "b", "c" }, result);

        String[] arrWithNull = { "a", null, "a", null, "b" };
        result = N.removeAllOccurrences(arrWithNull, null);
        assertArrayEquals(new String[] { "a", "a", "b" }, result);
    }

    @Test
    public void testRemoveAllOccurrencesObject() {
        Integer[] arr = { 1, 2, 1, 3, 1 };
        Integer[] result = N.removeAllOccurrences(arr, 1);
        assertArrayEquals(new Integer[] { 2, 3 }, result);
    }

    @Test
    public void testRemoveAllOccurrencesCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "a"));
        boolean result = N.removeAllOccurrences(list, "a");
        assertTrue(result);
        assertEquals(Arrays.asList("b", "c"), list);
    }

    @Test
    public void testRemoveDuplicatesBoolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.removeDuplicates(arr);
        assertArrayEquals(new boolean[] { true, false }, result);

        result = N.removeDuplicates(new boolean[] { true, false });
        assertArrayEquals(new boolean[] { true, false }, result);

        result = N.removeDuplicates(new boolean[] { true, true, true });
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testRemoveDuplicatesChar() {
        char[] arr = { 'a', 'b', 'a', 'c', 'b' };
        char[] result = N.removeDuplicates(arr);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        char[] sortedArr = { 'a', 'a', 'b', 'b', 'c' };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRemoveDuplicatesByte() {
        byte[] arr = { 1, 2, 1, 3, 2 };
        byte[] result = N.removeDuplicates(arr);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        byte[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveDuplicatesShort() {
        short[] arr = { 1, 2, 1, 3, 2 };
        short[] result = N.removeDuplicates(arr);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);

        short[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveDuplicatesInt() {
        int[] arr = { 1, 2, 1, 3, 2 };
        int[] result = N.removeDuplicates(arr);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);

        int[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveDuplicatesLong() {
        long[] arr = { 1L, 2L, 1L, 3L, 2L };
        long[] result = N.removeDuplicates(arr);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);

        long[] sortedArr = { 1L, 1L, 2L, 2L, 3L };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testRemoveDuplicatesFloat() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f, 2.0f };
        float[] result = N.removeDuplicates(arr);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);

        float[] sortedArr = { 1.0f, 1.0f, 2.0f, 2.0f, 3.0f };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveDuplicatesDouble() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0, 2.0 };
        double[] result = N.removeDuplicates(arr);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);

        double[] sortedArr = { 1.0, 1.0, 2.0, 2.0, 3.0 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRemoveDuplicatesString() {
        String[] arr = { "a", "b", "a", "c", "b" };
        String[] result = N.removeDuplicates(arr);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);

        String[] sortedArr = { "a", "a", "b", "b", "c" };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testRemoveDuplicatesObject() {
        Integer[] arr = { 1, 2, 1, 3, 2 };
        Integer[] result = N.removeDuplicates(arr);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);

        Integer[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void testCopyThenSetAll() {
        Integer[] arr = { 1, 2, 3, 4 };
        Integer[] result = N.copyThenSetAll(arr, i -> i * 10);
        assertArrayEquals(new Integer[] { 0, 10, 20, 30 }, result);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, arr);

        assertNull(N.copyThenSetAll((Integer[]) null, i -> i));

        Integer[] empty = new Integer[0];
        result = N.copyThenSetAll(empty, i -> i);
        assertEquals(0, result.length);
    }

    @Test
    public void testCopyThenReplaceAll() {
        Integer[] arr = { 1, 2, 3, 4 };
        Integer[] result = N.copyThenReplaceAll(arr, val -> val * 2);
        assertArrayEquals(new Integer[] { 2, 4, 6, 8 }, result);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, arr);

        assertNull(N.copyThenReplaceAll((Integer[]) null, val -> val));

        Integer[] empty = new Integer[0];
        result = N.copyThenReplaceAll(empty, val -> val);
        assertEquals(0, result.length);
    }

    @Test
    public void testSetAllArrayWithIntObjFunction() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        N.setAll(arr, (i, val) -> val + i);
        assertArrayEquals(new String[] { "a0", "b1", "c2", "d3" }, arr);
    }

    @Test
    public void testSetAllListWithIntObjFunction() throws Exception {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.setAll(list, (i, val) -> val + i);
        assertEquals(Arrays.asList("a0", "b1", "c2", "d3"), list);
    }

    @Test
    public void testCopyThenSetAllWithIntObjFunction() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        String[] result = N.copyThenSetAll(arr, (i, val) -> val + i);
        assertArrayEquals(new String[] { "a0", "b1", "c2", "d3" }, result);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, arr);
    }

    @Test
    public void testCopyThenUpdateAll() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        String[] result = N.copyThenUpdateAll(arr, String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C", "D" }, result);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, arr);
    }

    @Test
    public void testRemoveDuplicatesCharRange() {
        char[] arr = { 'a', 'b', 'a', 'b', 'c' };
        char[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new char[] { 'b', 'a' }, result);

        char[] sortedArr = { 'x', 'a', 'a', 'b', 'y' };
        result = N.removeDuplicates(sortedArr, 1, 4, true);
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testRemoveDuplicatesByteRange() {
        byte[] arr = { 5, 1, 2, 1, 5 };
        byte[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testRemoveDuplicatesShortRange() {
        short[] arr = { 5, 1, 2, 1, 5 };
        short[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testRemoveDuplicatesIntRange() {
        int[] arr = { 5, 1, 2, 1, 5 };
        int[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testRemoveDuplicatesLongRange() {
        long[] arr = { 5L, 1L, 2L, 1L, 5L };
        long[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    @Test
    public void testRemoveDuplicatesFloatRange() {
        float[] arr = { 5.0f, 1.0f, 2.0f, 1.0f, 5.0f };
        float[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveDuplicatesDoubleRange() {
        double[] arr = { 5.0, 1.0, 2.0, 1.0, 5.0 };
        double[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new double[] { 1.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testRemoveDuplicatesStringRange() {
        String[] arr = { "x", "a", "b", "a", "y" };
        String[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testRemoveDuplicatesObjectRange() {
        Integer[] arr = { 5, 1, 2, 1, 5 };
        Integer[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new Integer[] { 1, 2 }, result);
    }

    @Test
    public void testEmptyArrayOperations() {
        assertEquals(0, N.replaceIf(new int[0], val -> true, 1));

        assertEquals(0, N.replaceAll(new int[0], 1, 2));

        assertArrayEquals(new int[] { 1 }, N.add(new int[0], 1));

        assertArrayEquals(new int[0], N.remove(new int[0], 1));

        assertArrayEquals(new int[0], N.removeAll(new int[0], 1, 2));

        assertArrayEquals(new int[0], N.removeDuplicates(new int[0]));
    }

    @Test
    public void testNullArrayOperations() {
        assertEquals(0, N.replaceIf((int[]) null, val -> true, 1));

        assertEquals(0, N.replaceAll((int[]) null, 1, 2));

        assertArrayEquals(new int[] { 1 }, N.add((int[]) null, 1));

        assertArrayEquals(new int[0], N.remove((int[]) null, 1));

        assertArrayEquals(new int[0], N.removeAll((int[]) null, 1, 2));

        assertArrayEquals(new int[0], N.removeDuplicates((int[]) null));
    }

    @Test
    public void testLargeArrayOperations() {
        int[] largeArray = new int[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i % 100;
        }

        int count = N.replaceIf(largeArray, val -> val < 50, -1);
        assertEquals(500, count);

        int[] unique = N.removeDuplicates(largeArray);
        assertEquals(51, unique.length);
    }

    @Test
    public void testCollectionWithNullElements() {
        List<String> list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));

        int count = N.replaceAll(list, null, "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("a", "x", "b", "x", "c"), list);

        list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));
        boolean removed = N.remove(list, null);
        assertTrue(removed);
        assertEquals(Arrays.asList("a", "b", null, "c"), list);

        list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));
        removed = N.removeAllOccurrences(list, null);
        assertTrue(removed);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testSpecialFloatingPointValues() {
        float[] floatArr = { 1.0f, Float.NaN, 2.0f, Float.NaN };
        int count = N.replaceAll(floatArr, Float.NaN, 0.0f);
        assertEquals(2, count);

        double[] doubleArr = { 1.0, Double.NaN, 2.0, Double.NaN };
        count = N.replaceAll(doubleArr, Double.NaN, 0.0);
        assertEquals(2, count);

        floatArr = new float[] { 1.0f, Float.POSITIVE_INFINITY, 2.0f, Float.NEGATIVE_INFINITY };
        count = N.replaceAll(floatArr, Float.POSITIVE_INFINITY, 999.0f);
        assertEquals(1, count);
        assertArrayEquals(new float[] { 1.0f, 999.0f, 2.0f, Float.NEGATIVE_INFINITY }, floatArr, 0.001f);

        doubleArr = new double[] { 1.0, Double.POSITIVE_INFINITY, 2.0, Double.NEGATIVE_INFINITY };
        count = N.replaceAll(doubleArr, Double.POSITIVE_INFINITY, 999.0);
        assertEquals(1, count);
        assertArrayEquals(new double[] { 1.0, 999.0, 2.0, Double.NEGATIVE_INFINITY }, doubleArr, 0.001);
    }

    @Test
    public void testLinkedListOperations() {
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        boolean result = N.removeAt(linkedList, 1, 3);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "c", "e"), linkedList);

        linkedList = new LinkedList<>(Arrays.asList("a", "b", "a", "c"));
        int count = N.replaceAll(linkedList, "a", "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("x", "b", "x", "c"), linkedList);
    }

    @Test
    public void testBoundaryIndices() {
        int[] arr = { 1, 2, 3, 4, 5 };

        int[] result = N.insert(arr, 0, 0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, result);

        result = N.insert(arr, 5, 6);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

        result = N.removeAt(arr, 0);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, result);

        result = N.removeAt(arr, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);

        result = N.removeDuplicates(arr, 2, 3, false);
        assertArrayEquals(new int[] { 3 }, result);
    }

    @Test
    public void testPerformanceConsiderations() {
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            arrayList.add(i);
        }
        N.replaceAll(arrayList, val -> val * 2);

        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            linkedList.add(i);
        }
        N.replaceAll(linkedList, val -> val * 2);

        assertEquals(arrayList, linkedList);
    }

    @Test
    public void testOccurrencesOfString() {
        assertEquals(0, N.frequency((String) null, 'a'));
        assertEquals(0, N.frequency("", 'a'));
        assertEquals(3, N.frequency("banana", 'a'));
        assertEquals(2, N.frequency("banana", 'n'));
        assertEquals(0, N.frequency("banana", 'x'));

        assertEquals(0, N.frequency((String) null, "a"));
        assertEquals(0, N.frequency("", "a"));
        assertEquals(2, N.frequency("aba aba", "aba"));
        assertEquals(1, N.frequency("banana", "ana"));
    }

    @Test
    public void testContainsAllCollectionCollection() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "c");
        assertTrue(N.containsAll(collection, valuesToFind));

        List<String> notAllPresent = Arrays.asList("a", "e");
        assertFalse(N.containsAll(collection, notAllPresent));

        assertTrue(N.containsAll(collection, new ArrayList<>()));
        assertFalse(N.containsAll(new ArrayList<>(), valuesToFind));
        assertFalse(N.containsAll((List<String>) null, valuesToFind));
        assertTrue(N.containsAll(collection, (Collection<?>) null));
    }

    @Test
    public void testContainsAllCollectionArray() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsAll(collection, "a", "c"));
        assertFalse(N.containsAll(collection, "a", "e"));
        assertTrue(N.containsAll(collection, new String[] {}));
        assertFalse(N.containsAll(new ArrayList<>(), "a"));
    }

    @Test
    public void testContainsAllIterableCollection() {
        List<String> iterable = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "c");
        assertTrue(N.containsAll((Iterable<?>) iterable, valuesToFind));

        List<String> notAllPresent = Arrays.asList("a", "e");
        assertFalse(N.containsAll((Iterable<?>) iterable, notAllPresent));
    }

    @Test
    public void testContainsAllIteratorCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "c");
        assertTrue(N.containsAll(list.iterator(), valuesToFind));

        List<String> list2 = Arrays.asList("a", "b", "c", "d");
        List<String> notAllPresent = Arrays.asList("a", "e");
        assertFalse(N.containsAll(list2.iterator(), notAllPresent));
    }

    @Test
    public void testContainsAnyCollectionCollection() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        List<String> valuesToFind = Arrays.asList("a", "e");
        assertTrue(N.containsAny(collection, valuesToFind));

        List<String> nonePresent = Arrays.asList("x", "y");
        assertFalse(N.containsAny(collection, nonePresent));

        assertFalse(N.containsAny(collection, new ArrayList<>()));
        assertFalse(N.containsAny(new ArrayList<>(), valuesToFind));
    }

    @Test
    public void testContainsAnyCollectionArray() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsAny(collection, "a", "e"));
        assertFalse(N.containsAny(collection, "x", "y"));
        assertFalse(N.containsAny(collection, new String[] {}));
    }

    @Test
    public void testContainsAnyIterableSet() {
        List<String> iterable = Arrays.asList("a", "b", "c", "d");
        Set<String> valuesToFind = new HashSet<>(Arrays.asList("a", "e"));
        assertTrue(N.containsAny((Iterable<?>) iterable, valuesToFind));

        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertFalse(N.containsAny((Iterable<?>) iterable, nonePresent));
    }

    @Test
    public void testContainsAnyIteratorSet() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Set<String> valuesToFind = new HashSet<>(Arrays.asList("a", "e"));
        assertTrue(N.containsAny(list.iterator(), valuesToFind));

        List<String> list2 = Arrays.asList("a", "b", "c", "d");
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertFalse(N.containsAny(list2.iterator(), nonePresent));
    }

    @Test
    public void testContainsNoneCollectionCollection() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        List<String> nonePresent = Arrays.asList("x", "y");
        assertTrue(N.containsNone(collection, nonePresent));

        List<String> somePresent = Arrays.asList("a", "x");
        assertFalse(N.containsNone(collection, somePresent));

        assertTrue(N.containsNone(collection, new ArrayList<>()));
        assertTrue(N.containsNone(new ArrayList<>(), nonePresent));
    }

    @Test
    public void testContainsNoneCollectionArray() {
        List<String> collection = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsNone(collection, "x", "y"));
        assertFalse(N.containsNone(collection, "a", "x"));
        assertTrue(N.containsNone(collection, new String[] {}));
    }

    @Test
    public void testContainsNoneIterableSet() {
        List<String> iterable = Arrays.asList("a", "b", "c", "d");
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertTrue(N.containsNone((Iterable<?>) iterable, nonePresent));

        Set<String> somePresent = new HashSet<>(Arrays.asList("a", "x"));
        assertFalse(N.containsNone((Iterable<?>) iterable, somePresent));
    }

    @Test
    public void testContainsNoneIteratorSet() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertTrue(N.containsNone(list.iterator(), nonePresent));

        List<String> list2 = Arrays.asList("a", "b", "c", "d");
        Set<String> somePresent = new HashSet<>(Arrays.asList("a", "x"));
        assertFalse(N.containsNone(list2.iterator(), somePresent));
    }

    @Test
    public void testSliceIteratorInvalidFromIndex() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.slice(Arrays.asList("a", "b").iterator(), -1, 2));
    }

    @Test
    public void testSplitCharSequence() {
        String str = "abcde";
        List<String> chunks = N.split(str, 2);
        assertEquals(3, chunks.size());
        assertEquals("ab", chunks.get(0));
        assertEquals("cd", chunks.get(1));
        assertEquals("e", chunks.get(2));

        List<String> empty = N.split("", 2);
        assertTrue(empty.isEmpty());

        List<String> nullSplit = N.split((CharSequence) null, 2);
        assertTrue(nullSplit.isEmpty());
    }

    @Test
    public void testSplitCharSequenceWithRange() {
        String str = "abcdef";
        List<String> chunks = N.split(str, 1, 5, 2);
        assertEquals(2, chunks.size());
        assertEquals("bc", chunks.get(0));
        assertEquals("de", chunks.get(1));
    }

    @Test
    public void testSplitByChunkCount() {
        IntBiFunction<int[]> func = (fromIndex, toIndex) -> {
            int[] result = new int[toIndex - fromIndex];
            for (int i = 0; i < result.length; i++) {
                result[i] = fromIndex + i;
            }
            return result;
        };

        List<int[]> chunks = N.splitByChunkCount(10, 3, func);
        assertEquals(3, chunks.size());
        assertEquals(4, chunks.get(0).length);
        assertEquals(3, chunks.get(1).length);
        assertEquals(3, chunks.get(2).length);
    }

    @Test
    public void testSplitByChunkCountSizeSmallerFirst() {
        IntBiFunction<int[]> func = (fromIndex, toIndex) -> {
            int[] result = new int[toIndex - fromIndex];
            for (int i = 0; i < result.length; i++) {
                result[i] = fromIndex + i;
            }
            return result;
        };

        List<int[]> chunks = N.splitByChunkCount(10, 3, true, func);
        assertEquals(3, chunks.size());
        assertEquals(3, chunks.get(0).length);
        assertEquals(3, chunks.get(1).length);
        assertEquals(4, chunks.get(2).length);
    }

    @Test
    public void testConcatBooleanArrays() {
        boolean[] a = { true, false };
        boolean[] b = { false, true };
        boolean[] result = N.concat(a, b);
        assertArrayEquals(new boolean[] { true, false, false, true }, result);

        boolean[] empty = {};
        assertArrayEquals(a.clone(), N.concat(a, empty));
        assertArrayEquals(b.clone(), N.concat(empty, b));
        assertArrayEquals(empty, N.concat(empty, empty));
        assertArrayEquals(b.clone(), N.concat(null, b));
        assertArrayEquals(a.clone(), N.concat(a, null));
    }

    @Test
    public void testConcatMultipleBooleanArrays() {
        boolean[] a = { true };
        boolean[] b = { false };
        boolean[] c = { true, false };
        boolean[] result = N.concat(a, b, c);
        assertArrayEquals(new boolean[] { true, false, true, false }, result);

        assertArrayEquals(new boolean[] {}, N.concat(new boolean[0]));
        assertArrayEquals(a.clone(), N.concat(a));
        assertArrayEquals(new boolean[] {}, N.concat((boolean[][]) null));
    }

    @Test
    public void testConcatMultipleCharArrays() {
        char[] a = { 'a' };
        char[] b = { 'b' };
        char[] c = { 'c', 'd' };
        char[] result = N.concat(a, b, c);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testConcatMultipleByteArrays() {
        byte[] a = { 1 };
        byte[] b = { 2 };
        byte[] c = { 3, 4 };
        byte[] result = N.concat(a, b, c);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatShortArrays() {
        short[] a = { 1, 2 };
        short[] b = { 3, 4 };
        short[] result = N.concat(a, b);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatMultipleShortArrays() {
        short[] a = { 1 };
        short[] b = { 2 };
        short[] c = { 3, 4 };
        short[] result = N.concat(a, b, c);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatIntArrays() {
        int[] a = { 1, 2 };
        int[] b = { 3, 4 };
        int[] result = N.concat(a, b);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatMultipleIntArrays() {
        int[] a = { 1 };
        int[] b = { 2 };
        int[] c = { 3, 4 };
        int[] result = N.concat(a, b, c);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testConcatMultipleLongArrays() {
        long[] a = { 1L };
        long[] b = { 2L };
        long[] c = { 3L, 4L };
        long[] result = N.concat(a, b, c);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testConcatFloatArrays() {
        float[] a = { 1.0f, 2.0f };
        float[] b = { 3.0f, 4.0f };
        float[] result = N.concat(a, b);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testConcatMultipleFloatArrays() {
        float[] a = { 1.0f };
        float[] b = { 2.0f };
        float[] c = { 3.0f, 4.0f };
        float[] result = N.concat(a, b, c);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testConcatDoubleArrays() {
        double[] a = { 1.0, 2.0 };
        double[] b = { 3.0, 4.0 };
        double[] result = N.concat(a, b);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testConcatMultipleDoubleArrays() {
        double[] a = { 1.0 };
        double[] b = { 2.0 };
        double[] c = { 3.0, 4.0 };
        double[] result = N.concat(a, b, c);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testConcatObjectArrays() {
        String[] a = { "a", "b" };
        String[] b = { "c", "d" };
        String[] result = N.concat(a, b);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);

        String[] empty = {};
        assertArrayEquals(a.clone(), N.concat(a, empty));
        assertArrayEquals(b.clone(), N.concat(empty, b));
        assertArrayEquals(empty, N.concat(empty, empty));
    }

    @Test
    public void testConcatMultipleObjectArrays() {
        String[] a = { "a" };
        String[] b = { "b" };
        String[] c = { "c", "d" };
        String[] result = N.concat(a, b, c);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);

        assertNull(N.concat((String[][]) null));
        assertArrayEquals(new String[] {}, N.concat(new String[0][]));
        assertArrayEquals(a.clone(), N.concat(a));
    }

    @Test
    public void testConcatIterables() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("c", "d");
        List<String> result = N.concat(a, b);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        List<String> empty = new ArrayList<>();
        assertEquals(a, N.concat(a, empty));
        assertEquals(b, N.concat(empty, b));
        assertEquals(empty, N.concat(empty, empty));
    }

    @Test
    public void testConcatMultipleIterables() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        List<String> result = N.concat(a, b, c);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        assertEquals(new ArrayList<>(), N.concat(new Iterable[0]));
    }

    @Test
    public void testConcatCollectionOfIterables() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        List<Iterable<String>> iterables = Arrays.asList(a, b, c);
        List<String> result = N.concat(iterables);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testConcatCollectionOfIterablesWithSupplier() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        List<Iterable<String>> iterables = Arrays.asList(a, b, c);
        Set<String> result = N.concat(iterables, IntFunctions.ofSet());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d")), result);
    }

    @Test
    public void testConcatIterators() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("c", "d");
        ObjIterator<String> result = N.concat(a.iterator(), b.iterator());

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }

    @Test
    public void testConcatMultipleIterators() {
        List<String> a = Arrays.asList("a");
        List<String> b = Arrays.asList("b");
        List<String> c = Arrays.asList("c", "d");
        ObjIterator<String> result = N.concat(a.iterator(), b.iterator(), c.iterator());

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d"), collected);
    }

    @Test
    public void testFlattenBoolean2DArray() {
        boolean[][] array = { { true, false }, { false }, { true, true } };
        boolean[] result = N.flatten(array);
        assertArrayEquals(new boolean[] { true, false, false, true, true }, result);

        assertArrayEquals(new boolean[] {}, N.flatten(new boolean[0][]));
        assertArrayEquals(new boolean[] {}, N.flatten((boolean[][]) null));

        boolean[][] withNull = { { true }, null, { false } };
        assertArrayEquals(new boolean[] { true, false }, N.flatten(withNull));
    }

    @Test
    public void testFlattenChar2DArray() {
        char[][] array = { { 'a', 'b' }, { 'c' }, { 'd', 'e' } };
        char[] result = N.flatten(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testFlattenShort2DArray() {
        short[][] array = { { 1, 2 }, { 3 }, { 4, 5 } };
        short[] result = N.flatten(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlattenInt2DArray() {
        int[][] array = { { 1, 2 }, { 3 }, { 4, 5 } };
        int[] result = N.flatten(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testFlattenLong2DArray() {
        long[][] array = { { 1L, 2L }, { 3L }, { 4L, 5L } };
        long[] result = N.flatten(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testFlattenFloat2DArray() {
        float[][] array = { { 1.0f, 2.0f }, { 3.0f }, { 4.0f, 5.0f } };
        float[] result = N.flatten(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result, 0.001f);
    }

    @Test
    public void testFlattenDouble2DArray() {
        double[][] array = { { 1.0, 2.0 }, { 3.0 }, { 4.0, 5.0 } };
        double[] result = N.flatten(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testFlattenObject2DArray() {
        String[][] array = { { "a", "b" }, { "c" }, { "d", "e" } };
        String[] result = N.flatten(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, result);

        assertNull(N.flatten((String[][]) null));

        String[][] withNull = { { "a" }, null, { "b" } };
        assertArrayEquals(new String[] { "a", "b" }, N.flatten(withNull));
    }

    @Test
    public void testFlattenObject2DArrayWithClass() {
        String[][] array = { { "a", "b" }, { "c" }, { "d", "e" } };
        String[] result = N.flatten(array, String.class);
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, result);

        assertArrayEquals(new String[] {}, N.flatten((String[][]) null, String.class));
    }

    @Test
    public void testFlattenIterableOfIterables() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e"));
        List<String> result = N.flatten(lists);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);

        List<List<String>> empty = new ArrayList<>();
        assertEquals(new ArrayList<>(), N.flatten(empty));

        assertEquals(new ArrayList<>(), N.flatten((Iterable<Iterable<String>>) null));
    }

    @Test
    public void testFlattenIterableOfIterablesWithSupplier() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c"), Arrays.asList("d", "e"));
        Set<String> result = N.flatten(lists, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d", "e")), result);
    }

    @Test
    public void testFlattenIteratorOfIterators() {
        List<Iterator<String>> iterators = Arrays.asList(Arrays.asList("a", "b").iterator(), Arrays.asList("c").iterator(), Arrays.asList("d", "e").iterator());
        ObjIterator<String> result = N.flatten(iterators.iterator());

        List<String> collected = new ArrayList<>();
        while (result.hasNext()) {
            collected.add(result.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), collected);

        ObjIterator<String> empty = N.flatten((Iterator<Iterator<String>>) null);
        assertFalse(empty.hasNext());
    }

    @Test
    public void testIntersectionBooleanArrays() {
        boolean[] a = { true, false, true, true };
        boolean[] b = { false, true, true };
        boolean[] result = N.intersection(a, b);
        assertArrayEquals(new boolean[] { true, false, true }, result);

        assertArrayEquals(new boolean[] {}, N.intersection(new boolean[] {}, b));
        assertArrayEquals(new boolean[] {}, N.intersection(a, new boolean[] {}));
        assertArrayEquals(new boolean[] {}, N.intersection((boolean[]) null, b));
    }

    @Test
    public void testIntersectionCharArrays() {
        char[] a = { 'a', 'b', 'c', 'a' };
        char[] b = { 'b', 'd', 'a' };
        char[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 'a'));
        assertTrue(N.contains(result, 'b'));
    }

    @Test
    public void testIntersectionByteArrays() {
        byte[] a = { 1, 2, 3, 1 };
        byte[] b = { 2, 4, 1 };
        byte[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (byte) 1));
        assertTrue(N.contains(result, (byte) 2));
    }

    @Test
    public void testIntersectionShortArrays() {
        short[] a = { 1, 2, 3, 1 };
        short[] b = { 2, 4, 1 };
        short[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (short) 1));
        assertTrue(N.contains(result, (short) 2));
    }

    @Test
    public void testIntersectionIntArrays() {
        int[] a = { 0, 1, 2, 2, 3 };
        int[] b = { 2, 5, 1 };
        int[] result = N.intersection(a, b);
        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testIntersectionLongArrays() {
        long[] a = { 1L, 2L, 3L, 1L };
        long[] b = { 2L, 4L, 1L };
        long[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1L));
        assertTrue(N.contains(result, 2L));
    }

    @Test
    public void testIntersectionFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f, 1.0f };
        float[] b = { 2.0f, 4.0f, 1.0f };
        float[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0f));
        assertTrue(N.contains(result, 2.0f));
    }

    @Test
    public void testIntersectionDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0, 1.0 };
        double[] b = { 2.0, 4.0, 1.0 };
        double[] result = N.intersection(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0));
        assertTrue(N.contains(result, 2.0));
    }

    @Test
    public void testIntersectionObjectArrays() {
        String[] a = { "a", "b", "c", "a" };
        String[] b = { "b", "d", "a" };
        List<String> result = N.intersection(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));

        assertEquals(new ArrayList<>(), N.intersection(new String[] {}, b));
        assertEquals(new ArrayList<>(), N.intersection(a, new String[] {}));
    }

    @Test
    public void testIntersectionCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("b", "d", "a");
        List<String> result = N.intersection(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));

        assertEquals(new ArrayList<>(), N.intersection(new ArrayList<>(), b));
        assertEquals(new ArrayList<>(), N.intersection(a, new ArrayList<>()));
    }

    @Test
    public void testIntersectionMultipleCollections() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("b", "c", "d");
        List<String> c = Arrays.asList("c", "d", "e");
        List<Collection<String>> collections = Arrays.asList(a, b, c);
        List<String> result = N.intersection(collections);
        assertEquals(1, result.size());
        assertTrue(result.contains("c"));

        assertEquals(new ArrayList<>(), N.intersection(new ArrayList<Collection<String>>()));

        List<Collection<String>> withEmpty = Arrays.asList(a, new ArrayList<>(), c);
        assertEquals(new ArrayList<>(), N.intersection(withEmpty));
    }

    @Test
    public void testDifferenceBooleanArrays() {
        boolean[] a = { true, false, true, true };
        boolean[] b = { false, true };
        boolean[] result = N.difference(a, b);
        assertArrayEquals(new boolean[] { true, true }, result);

        assertArrayEquals(new boolean[] {}, N.difference(new boolean[] {}, b));
        assertArrayEquals(a.clone(), N.difference(a, new boolean[] {}));
        assertArrayEquals(new boolean[] {}, N.difference((boolean[]) null, b));
        assertArrayEquals(a.clone(), N.difference(a, null));
    }

    @Test
    public void testDifferenceCharArrays() {
        char[] a = { 'a', 'b', 'c', 'a' };
        char[] b = { 'b', 'd' };
        char[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.frequency(result, 'a'));
        assertEquals(1, N.frequency(result, 'c'));
    }

    @Test
    public void testDifferenceByteArrays() {
        byte[] a = { 1, 2, 3, 1 };
        byte[] b = { 2, 4 };
        byte[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.frequency(result, (byte) 1));
        assertEquals(1, N.frequency(result, (byte) 3));
    }

    @Test
    public void testDifferenceShortArrays() {
        short[] a = { 1, 2, 3, 1 };
        short[] b = { 2, 4 };
        short[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.frequency(result, (short) 1));
        assertEquals(1, N.frequency(result, (short) 3));
    }

    @Test
    public void testDifferenceIntArrays() {
        int[] a = { 0, 1, 2, 2, 3 };
        int[] b = { 2, 5, 1 };
        int[] result = N.difference(a, b);
        assertArrayEquals(new int[] { 0, 2, 3 }, result);
    }

    @Test
    public void testDifferenceLongArrays() {
        long[] a = { 1L, 2L, 3L, 1L };
        long[] b = { 2L, 4L };
        long[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.frequency(result, 1L));
        assertEquals(1, N.frequency(result, 3L));
    }

    @Test
    public void testDifferenceFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f, 1.0f };
        float[] b = { 2.0f, 4.0f };
        float[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.frequency(result, 1.0f));
        assertEquals(1, N.frequency(result, 3.0f));
    }

    @Test
    public void testDifferenceDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0, 1.0 };
        double[] b = { 2.0, 4.0 };
        double[] result = N.difference(a, b);
        assertEquals(3, result.length);
        assertEquals(2, N.frequency(result, 1.0));
        assertEquals(1, N.frequency(result, 3.0));
    }

    @Test
    public void testDifferenceObjectArrays() {
        String[] a = { "a", "b", "c", "a" };
        String[] b = { "b", "d" };
        List<String> result = N.difference(a, b);
        assertEquals(3, result.size());
        assertEquals(2, N.frequency(result, "a"));
        assertEquals(1, N.frequency(result, "c"));

        assertEquals(new ArrayList<>(), N.difference(new String[] {}, b));
        assertEquals(Arrays.asList(a), N.difference(a, new String[] {}));
    }

    @Test
    public void testDifferenceCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("b", "d");
        List<String> result = N.difference(a, b);
        assertEquals(3, result.size());
        assertEquals(2, N.frequency(result, "a"));
        assertEquals(1, N.frequency(result, "c"));

        assertEquals(new ArrayList<>(), N.difference(new ArrayList<>(), b));
        assertEquals(new ArrayList<>(a), N.difference(a, new ArrayList<>()));
    }

    @Test
    public void testSymmetricDifferenceBooleanArrays() {
        boolean[] a = { true, false, true };
        boolean[] b = { false, false, true };
        boolean[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertEquals(1, N.frequency(result, true));
        assertEquals(1, N.frequency(result, false));

        assertArrayEquals(b.clone(), N.symmetricDifference(new boolean[] {}, b));
        assertArrayEquals(a.clone(), N.symmetricDifference(a, new boolean[] {}));
        assertArrayEquals(b.clone(), N.symmetricDifference(null, b));
        assertArrayEquals(a.clone(), N.symmetricDifference(a, null));
    }

    @Test
    public void testSymmetricDifferenceCharArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'b', 'c', 'd' };
        char[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 'a'));
        assertTrue(N.contains(result, 'd'));
    }

    @Test
    public void testSymmetricDifferenceByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 2, 3, 4 };
        byte[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (byte) 1));
        assertTrue(N.contains(result, (byte) 4));
    }

    @Test
    public void testSymmetricDifferenceShortArrays() {
        short[] a = { 1, 2, 3 };
        short[] b = { 2, 3, 4 };
        short[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, (short) 1));
        assertTrue(N.contains(result, (short) 4));
    }

    @Test
    public void testSymmetricDifferenceLongArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 2L, 3L, 4L };
        long[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1L));
        assertTrue(N.contains(result, 4L));
    }

    @Test
    public void testSymmetricDifferenceFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 2.0f, 3.0f, 4.0f };
        float[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0f));
        assertTrue(N.contains(result, 4.0f));
    }

    @Test
    public void testSymmetricDifferenceDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 2.0, 3.0, 4.0 };
        double[] result = N.symmetricDifference(a, b);
        assertEquals(2, result.length);
        assertTrue(N.contains(result, 1.0));
        assertTrue(N.contains(result, 4.0));
    }

    @Test
    public void testSymmetricDifferenceObjectArrays() {
        String[] a = { "a", "b", "c" };
        String[] b = { "b", "c", "d" };
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("d"));

        assertEquals(Arrays.asList(b), N.symmetricDifference(new String[] {}, b));
        assertEquals(Arrays.asList(a), N.symmetricDifference(a, new String[] {}));
    }

    @Test
    public void testSymmetricDifferenceCollections() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("b", "c", "d");
        List<String> result = N.symmetricDifference(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("d"));

        assertEquals(new ArrayList<>(b), N.symmetricDifference(new ArrayList<>(), b));
        assertEquals(new ArrayList<>(a), N.symmetricDifference(a, new ArrayList<>()));
    }

    @Test
    public void testCommonSet() {
        List<String> a = Arrays.asList("a", "b", "c", "a");
        List<String> b = Arrays.asList("b", "c", "d", "c");
        Set<String> result = N.commonSet(a, b);
        assertEquals(2, result.size());
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));

        assertEquals(new HashSet<>(), N.commonSet(new ArrayList<>(), b));
        assertEquals(new HashSet<>(), N.commonSet(a, new ArrayList<>()));
        assertEquals(new HashSet<>(), N.commonSet(null, b));
        assertEquals(new HashSet<>(), N.commonSet(a, null));
    }

    @Test
    public void testCommonSetMultipleCollections() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("b", "c", "d");
        List<String> c = Arrays.asList("c", "d", "e");
        List<Collection<String>> collections = Arrays.asList(a, b, c);
        Set<String> result = N.commonSet(collections);
        assertEquals(1, result.size());
        assertTrue(result.contains("c"));

        assertEquals(new HashSet<>(), N.commonSet(new ArrayList<Collection<String>>()));

        List<Collection<String>> singleCollection = Arrays.asList(a);
        assertEquals(new HashSet<>(a), N.commonSet(singleCollection));

        List<Collection<String>> withEmpty = Arrays.asList(a, new ArrayList<>(), c);
        assertEquals(new HashSet<>(), N.commonSet(withEmpty));
    }

    @Test
    public void testIsSubCollectionNullSubColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(null, Arrays.asList("a")));
    }

    @Test
    public void testIsSubCollectionNullColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isSubCollection(Arrays.asList("a"), null));
    }

    @Test
    public void testIsProperSubCollectionNullSubColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(null, Arrays.asList("a")));
    }

    @Test
    public void testIsProperSubCollectionNullColl() {
        assertThrows(IllegalArgumentException.class, () -> N.isProperSubCollection(Arrays.asList("a"), null));
    }

    // ==================== removeAt tests ====================

    @Test
    public void testRemoveAt() {
        // boolean array
        boolean[] bools = { true, false, true };
        boolean[] resultBools = N.removeAt(bools, 1);
        assertArrayEquals(new boolean[] { true, true }, resultBools);

        // char array
        char[] chars = { 'a', 'b', 'c' };
        char[] resultChars = N.removeAt(chars, 0);
        assertArrayEquals(new char[] { 'b', 'c' }, resultChars);

        // byte array
        byte[] bytes = { 1, 2, 3 };
        byte[] resultBytes = N.removeAt(bytes, 2);
        assertArrayEquals(new byte[] { 1, 2 }, resultBytes);

        // short array
        short[] shorts = { 10, 20, 30 };
        short[] resultShorts = N.removeAt(shorts, 1);
        assertArrayEquals(new short[] { 10, 30 }, resultShorts);

        // int array
        int[] ints = { 1, 2, 3, 4, 5 };
        int[] resultInts = N.removeAt(ints, 2);
        assertArrayEquals(new int[] { 1, 2, 4, 5 }, resultInts);

        // long array
        long[] longs = { 100L, 200L, 300L };
        long[] resultLongs = N.removeAt(longs, 0);
        assertArrayEquals(new long[] { 200L, 300L }, resultLongs);

        // float array
        float[] floats = { 1.0f, 2.0f, 3.0f };
        float[] resultFloats = N.removeAt(floats, 1);
        assertArrayEquals(new float[] { 1.0f, 3.0f }, resultFloats, DELTAf);

        // double array
        double[] doubles = { 1.0, 2.0, 3.0 };
        double[] resultDoubles = N.removeAt(doubles, 2);
        assertArrayEquals(new double[] { 1.0, 2.0 }, resultDoubles, DELTA);

        // generic array
        String[] strings = { "a", "b", "c" };
        String[] resultStrings = N.removeAt(strings, 1);
        assertArrayEquals(new String[] { "a", "c" }, resultStrings);
    }

    @Test
    public void testRemoveAt_SingleElement() {
        int[] single = { 42 };
        int[] result = N.removeAt(single, 0);
        assertEquals(0, result.length);
    }

    @Test
    public void testRemoveAt_IndexOutOfBounds() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(arr, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(arr, -1));
    }

    @Test
    public void testRemoveAt_MultipleIndices() {
        int[] arr = { 1, 2, 3, 4, 5 };
        int[] result = N.removeAt(arr, 0, 2, 4);
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAt_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        boolean modified = N.removeAt(list, 1, 3);
        assertTrue(modified);
        assertEquals(Arrays.asList("a", "c"), list);
    }

    @Test
    public void testRemoveAt_ListSingleIndex() {
        List<Integer> list = new ArrayList<>(Arrays.asList(10, 20, 30));
        boolean modified = N.removeAt(list, 1);
        assertTrue(modified);
        assertEquals(Arrays.asList(10, 30), list);
    }

    @Test
    public void testRemoveAt_ListNoIndices() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        boolean modified = N.removeAt(list, new int[0]);
        assertFalse(modified);
        assertEquals(2, list.size());
    }

    // ==================== removeRange tests ====================

    @Test
    public void testRemoveRange() {
        // boolean array
        boolean[] bools = { true, false, true, false, true };
        boolean[] resultBools = N.removeRange(bools, 1, 3);
        assertArrayEquals(new boolean[] { true, false, true }, resultBools);

        // char array
        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        char[] resultChars = N.removeRange(chars, 1, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'e' }, resultChars);

        // int array
        int[] ints = { 1, 2, 3, 4, 5 };
        int[] resultInts = N.removeRange(ints, 0, 2);
        assertArrayEquals(new int[] { 3, 4, 5 }, resultInts);

        // long array
        long[] longs = { 10L, 20L, 30L, 40L };
        long[] resultLongs = N.removeRange(longs, 2, 4);
        assertArrayEquals(new long[] { 10L, 20L }, resultLongs);

        // double array
        double[] doubles = { 1.0, 2.0, 3.0 };
        double[] resultDoubles = N.removeRange(doubles, 0, 3);
        assertEquals(0, resultDoubles.length);

        // generic array
        String[] strings = { "a", "b", "c", "d" };
        String[] resultStrings = N.removeRange(strings, 1, 3);
        assertArrayEquals(new String[] { "a", "d" }, resultStrings);
    }

    @Test
    public void testRemoveRange_SameFromTo() {
        int[] arr = { 1, 2, 3 };
        int[] result = N.removeRange(arr, 1, 1);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveRange_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        boolean modified = N.removeRange(list, 1, 3);
        assertTrue(modified);
        assertEquals(Arrays.asList("a", "d", "e"), list);
    }

    @Test
    public void testRemoveRange_ListNoChange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        boolean modified = N.removeRange(list, 1, 1);
        assertFalse(modified);
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveRange_String() {
        String result = N.removeRange("abcdef", 1, 4);
        assertEquals("aef", result);
    }

    @Test
    public void testRemoveRange_StringFull() {
        String result = N.removeRange("abc", 0, 3);
        assertEquals("", result);
    }

    @Test
    public void testRemoveRange_IndexOutOfBounds() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(arr, 0, 5));
    }

    // ==================== containsDuplicates tests ====================

    @Test
    public void testContainsDuplicates() {
        // boolean array
        assertFalse(N.containsDuplicates(new boolean[] { true }));
        assertFalse(N.containsDuplicates(new boolean[] { true, false }));
        assertTrue(N.containsDuplicates(new boolean[] { true, false, true }));

        // char array
        assertFalse(N.containsDuplicates(new char[] { 'a', 'b', 'c' }));
        assertTrue(N.containsDuplicates(new char[] { 'a', 'b', 'a' }));

        // int array
        assertFalse(N.containsDuplicates(new int[] { 1, 2, 3 }));
        assertTrue(N.containsDuplicates(new int[] { 1, 2, 1 }));

        // long array
        assertFalse(N.containsDuplicates(new long[] { 1L, 2L, 3L }));
        assertTrue(N.containsDuplicates(new long[] { 1L, 2L, 1L }));

        // double array
        assertFalse(N.containsDuplicates(new double[] { 1.0, 2.0, 3.0 }));
        assertTrue(N.containsDuplicates(new double[] { 1.0, 2.0, 1.0 }));

        // generic array
        assertFalse(N.containsDuplicates(new String[] { "a", "b", "c" }));
        assertTrue(N.containsDuplicates(new String[] { "a", "b", "a" }));

        // Collection
        assertFalse(N.containsDuplicates(Arrays.asList("x", "y", "z")));
        assertTrue(N.containsDuplicates(Arrays.asList("x", "y", "x")));
    }

    @Test
    public void testContainsDuplicates_Empty() {
        assertFalse(N.containsDuplicates(new int[0]));
        assertFalse(N.containsDuplicates(new String[0]));
        assertFalse(N.containsDuplicates(new ArrayList<>()));
    }

    @Test
    public void testContainsDuplicates_NullArray() {
        assertFalse(N.containsDuplicates((int[]) null));
        assertFalse(N.containsDuplicates((String[]) null));
        assertFalse(N.containsDuplicates((Collection<?>) null));
    }

    @Test
    public void testContainsDuplicates_Sorted() {
        assertTrue(N.containsDuplicates(new int[] { 1, 1, 2, 3 }, true));
        assertFalse(N.containsDuplicates(new int[] { 1, 2, 3 }, true));
        assertTrue(N.containsDuplicates(new char[] { 'a', 'a', 'b' }, true));
        assertFalse(N.containsDuplicates(new char[] { 'a', 'b', 'c' }, true));
    }

    // ==================== count tests ====================

    @Test
    public void testCount() {
        // boolean count
        boolean[] flags = { true, false, true, true, false };
        assertEquals(3, N.count(flags, b -> b));
        assertEquals(2, N.count(flags, b -> !b));

        // int count
        int[] ints = { 1, 2, 3, 4, 5, 6 };
        assertEquals(3, N.count(ints, (IntPredicate) x -> x % 2 == 0));

        // long count
        long[] longs = { 10L, 20L, 30L, 40L };
        assertEquals(2, N.count(longs, (LongPredicate) x -> x > 25L));

        // double count
        double[] doubles = { 1.5, 2.5, 3.5 };
        assertEquals(2, N.count(doubles, (DoublePredicate) x -> x > 2.0));

        // char count
        char[] chars = { 'a', 'B', 'c', 'D' };
        assertEquals(2, N.count(chars, (CharPredicate) Character::isUpperCase));

        // generic array count
        String[] strings = { "a", "bb", "ccc", "dd" };
        assertEquals(3, N.count(strings, (Predicate<String>) s -> s.length() > 1));

        // iterable count
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(2, N.count((Iterable<Integer>) list, (Predicate<Integer>) x -> x > 3));

        // iterator count (no predicate)
        assertEquals(5, N.count(list.iterator()));
    }

    @Test
    public void testCount_WithRange() {
        int[] arr = { 1, 2, 3, 4, 5, 6 };
        assertEquals(1, N.count(arr, 0, 3, (IntPredicate) x -> x % 2 == 0));
        assertEquals(2, N.count(arr, 3, 6, (IntPredicate) x -> x % 2 == 0));
    }

    @Test
    public void testCount_Empty() {
        assertEquals(0, N.count(new int[0], (IntPredicate) x -> true));
        assertEquals(0, N.count((int[]) null, (IntPredicate) x -> true));
        assertEquals(0, N.count(Collections.emptyList().iterator()));
    }

    @Test
    public void testCount_IteratorWithPredicate() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        assertEquals(2, N.count(list.iterator(), (Predicate<String>) s -> s.length() > 5));
    }

    // ==================== isMatchCountBetween tests ====================

    @Test
    public void testIsMatchCountBetween() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        // 4 even numbers
        assertTrue(N.isMatchCountBetween(arr, 2, 5, (Predicate<Integer>) x -> x % 2 == 0));
        assertTrue(N.isMatchCountBetween(arr, 4, 4, (Predicate<Integer>) x -> x % 2 == 0));
        assertFalse(N.isMatchCountBetween(arr, 5, 5, (Predicate<Integer>) x -> x % 2 == 0));
        assertFalse(N.isMatchCountBetween(arr, 0, 2, (Predicate<Integer>) x -> x % 2 == 0));
    }

    @Test
    public void testIsMatchCountBetween_Iterable() {
        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertTrue(N.isMatchCountBetween(list, 3, 5, (Predicate<Integer>) x -> x % 2 == 0));
        assertFalse(N.isMatchCountBetween(list, 5, 10, (Predicate<Integer>) x -> x % 2 == 0));
    }

    @Test
    public void testIsMatchCountBetween_Iterator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertTrue(N.isMatchCountBetween(list.iterator(), 2, 3, (Predicate<Integer>) x -> x % 2 == 0));
    }

    @Test
    public void testIsMatchCountBetween_EmptyArray() {
        Integer[] arr = {};
        assertTrue(N.isMatchCountBetween(arr, 0, 5, (Predicate<Integer>) x -> true));
        assertFalse(N.isMatchCountBetween(arr, 1, 5, (Predicate<Integer>) x -> true));
    }

    @Test
    public void testIsMatchCountBetween_InvalidArgs() {
        Integer[] arr = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> N.isMatchCountBetween(arr, -1, 5, (Predicate<Integer>) x -> true));
        assertThrows(IllegalArgumentException.class, () -> N.isMatchCountBetween(arr, 5, 2, (Predicate<Integer>) x -> true));
    }

    // ==================== callWithRetry / runWithRetry tests ====================

    @Test
    public void testRunWithRetry() {
        AtomicInteger counter = new AtomicInteger(0);
        // Succeeds on 3rd attempt
        N.runWithRetry(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("fail attempt " + counter.get());
            }
        }, 3, 10, (java.util.function.Predicate<? super Exception>) e -> e instanceof RuntimeException);

        assertEquals(3, counter.get());
    }

    @Test
    public void testRunWithRetry_AllFail() {
        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            N.runWithRetry(() -> {
                counter.incrementAndGet();
                throw new RuntimeException("always fail");
            }, 2, 10, (java.util.function.Predicate<? super Exception>) e -> e instanceof RuntimeException);
        });
        assertEquals(3, counter.get()); // initial + 2 retries
    }

    @Test
    public void testCallWithRetry() {
        AtomicInteger counter = new AtomicInteger(0);
        String result = N.callWithRetry(() -> {
            if (counter.incrementAndGet() < 3) {
                throw new RuntimeException("fail");
            }
            return "success";
        }, 3, 10, (r, e) -> e instanceof RuntimeException);

        assertEquals("success", result);
        assertEquals(3, counter.get());
    }

    @Test
    public void testCallWithRetry_ImmediateSuccess() {
        String result = N.callWithRetry(() -> "done", 3, 10, (r, e) -> false);
        assertEquals("done", result);
    }

    private static class Person {
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

    private static class CustomIterable<T> implements Iterable<T> {
        private final List<T> data;

        CustomIterable(List<T> data) {
            this.data = data;
        }

        @Override
        public Iterator<T> iterator() {
            return data.iterator();
        }
    }

    @XmlRootElement(name = "person")
    private static class TestPerson {
        private String name;
        private int age;

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

    // Cover package-private range-specific duplicate detection for char arrays.
    @org.junit.jupiter.api.Test
    public void testContainsDuplicates_CharRange_Unsorted() {
        final char[] values = { 'x', 'a', 'b', 'a', 'z' };

        assertTrue(N.containsDuplicates(values, 1, 4, false));
        assertFalse(N.containsDuplicates(values, 0, 3, false));
    }

    @org.junit.jupiter.api.Test
    public void testContainsDuplicates_CharRange_Sorted() {
        final char[] values = { 'a', 'b', 'b', 'c', 'd' };

        assertTrue(N.containsDuplicates(values, 0, 4, true));
        assertFalse(N.containsDuplicates(values, 3, 5, true));
    }

    @org.junit.jupiter.api.Test
    public void testZip_WithDefaultValuesForUnevenIterables() {
        final Iterable<String> left = new CustomIterable<>(Arrays.asList("a", "b", "c"));
        final Iterable<Integer> right = new CustomIterable<>(Arrays.asList(1));

        final List<Pair<String, Integer>> result = N.zip(left, right, "x", 9, Pair::of);

        assertEquals(3, result.size());
        assertEquals(Pair.of("a", 1), result.get(0));
        assertEquals(Pair.of("b", 9), result.get(1));
        assertEquals(Pair.of("c", 9), result.get(2));
    }

    @org.junit.jupiter.api.Test
    public void testZip_WithDefaultValuesForThreeUnevenIterables() {
        final Iterable<String> first = new CustomIterable<>(Arrays.asList("a", "b"));
        final Iterable<Integer> second = new CustomIterable<>(Arrays.asList(1));
        final Iterable<Boolean> third = new CustomIterable<>(Arrays.asList(true, false, true));

        final List<Triple<String, Integer, Boolean>> result = N.zip(first, second, third, "x", 9, false, Triple::of);

        assertEquals(3, result.size());
        assertEquals(Triple.of("a", 1, true), result.get(0));
        assertEquals(Triple.of("b", 9, false), result.get(1));
        assertEquals(Triple.of("x", 9, true), result.get(2));
    }

    @org.junit.jupiter.api.Test
    public void testInsert_StringArray_InsertAtBounds() {
        assertArrayEquals(new String[] { "z", "a", "b" }, N.insert(new String[] { "a", "b" }, 0, "z"));
        assertArrayEquals(new String[] { "a", "b", "z" }, N.insert(new String[] { "a", "b" }, 2, "z"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new String[] { "a", "b" }, 3, "z"));
    }

}
