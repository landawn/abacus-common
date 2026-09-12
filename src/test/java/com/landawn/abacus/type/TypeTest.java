package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.AccountStatus;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Color;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Gender;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MediaType;
import com.landawn.abacus.util.Month;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.ServiceStatus;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.TypeReference;
import com.landawn.abacus.util.UnifiedStatus;

import lombok.Builder;
import lombok.Data;
import testfixtures.types.MyConstant;

@SuppressWarnings("rawtypes")
public class TypeTest extends TestBase {

    @Data
    @Builder
    public static class DataTypeA {
        private Multiset<Character> multiset;
        private Multimap<Character, String, List<String>> listMultimap;
        private SetMultimap<Character, String> setMultimap;
        private com.google.common.collect.Multiset<Character> guavaMultiset;
        private com.google.common.collect.Multimap<Character, Integer> guavaListMultimap;
        private com.google.common.collect.SetMultimap<String, Long> guavaSetMultimap;
    }

    @Deprecated
    static <T> T[] asArray(final T... a) {
        return a;
    }

    @BeforeEach
    public void setUp() {
        TypeFactory.getType(String.class);
        TypeFactory.getType("JSON<Map>");
        TypeFactory.getType("XML<Map>");
        TypeFactory.getType("JSON<List>");
        TypeFactory.getType("XML<List>");
        TypeFactory.getType(MyConstant.class);
        TypeFactory.getType(UnifiedStatus.class);
        TypeFactory.getType("Status(true)");
    }

    @Test
    public void testTypeFactoryNames() {
        for (int i = 0; i < 3; i++) {
            assertEquals("Map<List<com.landawn.abacus.util.stream.Stream>, String>",
                    TypeFactory.getType("Map<List<com.landawn.abacus.util.stream.Stream>, String>").name());
            assertEquals("Map<List<com.landawn.abacus.util.stream.Stream<String>>, String>",
                    TypeFactory.getType("Map<List<com.landawn.abacus.util.stream.Stream<String>>, String>").declaringName());
        }
        assertEquals("List<String>", TypeFactory.getType("List<String>").name());
        assertEquals("ArrayList<String>", TypeFactory.getType("ArrayList<String>").name());
        assertEquals("Set<String>", TypeFactory.getType("Set<String>").name());
        assertEquals("HashSet<String>", TypeFactory.getType("HashSet<String>").name());
        assertEquals("LinkedHashSet<String>", TypeFactory.getType("LinkedHashSet<String>").name());
        assertEquals("Map<String, Object>", TypeFactory.getType("Map<String, Object>").name());
        assertEquals("HashMap<String, Object>", TypeFactory.getType("HashMap<String, Object>").name());
        assertEquals("LinkedHashMap<String, Object>", TypeFactory.getType("LinkedHashMap<String, Object>").name());
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("LinkedHashSet<String, Integer>"));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("LinkedHashSet<String>(||)"));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("LinkedHashMap<String>"));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("LinkedHashMap<String, Object>(||)"));

        assertEquals("Type", N.typeOf("Type").name());
        assertEquals("Type<?>", N.typeOf("Type<?>").name());
        assertEquals("Type<Object>", N.typeOf("Type<Object>").name());
        assertEquals("Type<String>", N.typeOf("Type<String>").name());
        assertEquals("Type<Integer>", N.typeOf("Type<Integer>").name());
        assertEquals("Type<int>", N.typeOf("Type<int>").name());
        assertEquals("Type<unknown>", N.typeOf("Type<unknown>").name());
        assertEquals(Type.class, N.typeOf("Type").javaType());
        assertEquals(String.class, N.typeOf("Type<String>").parameterTypes().get(0).javaType());

        Type<?> weekDayList = TypeFactory.getType("List<testfixtures.types.WeekDay(NAME)>");
        assertEquals(List.class, weekDayList.javaType());
        assertNotNull(weekDayList.elementType());
    }

    @Test
    public void testGuavaAndMultimapTypes() throws Exception {
        com.google.common.collect.Multiset<Character> guavaMultiset = com.google.common.collect.HashMultiset.create(N.toList('a', 'b', 'c', 'a'));
        com.google.common.collect.Multimap<Character, Integer> guavaListMultimap = ArrayListMultimap.create();
        guavaListMultimap.put('a', 1);
        guavaListMultimap.put('b', 2);
        guavaListMultimap.put('a', 1);
        com.google.common.collect.SetMultimap<String, Long> guavaSetMultimap = HashMultimap.create();
        guavaSetMultimap.put("a", 1L);
        guavaSetMultimap.put("b", 2L);
        guavaSetMultimap.put("a", 1L);
        Multimap<Character, String, List<String>> listMultimap = N.newListMultimap();
        listMultimap.put('a', "x");
        listMultimap.put('b', "y");
        listMultimap.put('a', "x");
        SetMultimap<Character, String> setMultimap = N.newSetMultimap();
        setMultimap.put('a', "x");
        setMultimap.put('b', "y");
        setMultimap.put('a', "x");

        DataTypeA data = DataTypeA.builder()
                .multiset(Multiset.of('a', 'b', 'c', 'a'))
                .listMultimap(listMultimap)
                .setMultimap(setMultimap)
                .guavaMultiset(guavaMultiset)
                .guavaListMultimap(guavaListMultimap)
                .guavaSetMultimap(guavaSetMultimap)
                .build();
        String json = N.toJson(data, true);
        assertEquals(data, N.fromJson(json, DataTypeA.class));
        assertTrue(json.contains("multiset"));
        assertTrue(json.contains("guavaMultiset"));

        BeanInfo beanInfo = ParserUtil.getBeanInfo(DataTypeA.class);
        assertEquals("Multiset<Character>", beanInfo.getPropInfo("multiset").type.name());
        assertEquals("Multimap<Character, String, List<String>>", beanInfo.getPropInfo("listMultimap").type.name());
        assertEquals("SetMultimap<Character, String>", beanInfo.getPropInfo("setMultimap").type.name());
        assertEquals("com.google.common.collect.Multiset<Character>", beanInfo.getPropInfo("guavaMultiset").type.name());
        assertTrue(beanInfo.getPropInfo("guavaListMultimap").type.name().contains("Multimap"));
        assertTrue(beanInfo.getPropInfo("guavaSetMultimap").type.name().contains("Multimap"));
        for (PropInfo propInfo : beanInfo.propInfoList) {
            assertNotNull(propInfo.type.name());
            assertNotNull(propInfo.type.declaringName());
        }

        Type<?> threeParam = N.typeOf("Multimap<String, Date, List<Date>>");
        assertEquals("Multimap<String, Date, List<Date>>", threeParam.name());
        assertEquals("Multimap<String, Date, List<Date>>", threeParam.declaringName());
        assertEquals(Multimap.class, threeParam.javaType());
        assertEquals(String.class, threeParam.parameterTypes().get(0).javaType());
        assertEquals(Date.class, threeParam.parameterTypes().get(1).javaType());
        assertEquals(List.class, threeParam.parameterTypes().get(2).javaType());
        assertEquals(Date.class, threeParam.parameterTypes().get(2).elementType().javaType());
        assertTrue(threeParam.isSerializable());

        Type<?> twoParam = N.typeOf("Multimap<String, List<Date>>");
        assertEquals("Multimap<String, List<Date>>", twoParam.name());
        assertEquals(Multimap.class, twoParam.javaType());
        assertEquals(String.class, twoParam.parameterTypes().get(0).javaType());
        assertEquals(List.class, twoParam.parameterTypes().get(1).javaType());
        assertEquals(Date.class, twoParam.parameterTypes().get(1).elementType().javaType());
        assertTrue(twoParam.isSerializable());

        Type<?> multiset = N.typeOf("Multiset<Date>");
        assertEquals(Multiset.class, multiset.javaType());
        assertEquals(Date.class, multiset.elementType().javaType());
        assertEquals(Date.class, multiset.parameterTypes().get(0).javaType());
        assertTrue(multiset.isSerializable());
    }

    @Test
    public void testClazzUriAndFlags() throws IOException {
        Type<?> clazzType = N.typeOf("clazz<int>");
        assertEquals(Class.class, clazzType.javaType());
        assertEquals(int.class, ((ClazzType) (Type) clazzType).parameterClass());

        Type<InputStream> streamType = N.typeOf(InputStream.class);
        assertFalse(streamType.isArray());
        assertFalse(streamType.isPrimitiveWrapper());
        assertFalse(streamType.isPrimitiveList());
        assertFalse(streamType.isString());
        assertFalse(streamType.isDate());
        assertFalse(streamType.isCalendar());
        assertFalse(streamType.isPrimitiveArray());
        assertFalse(streamType.isObjectArray());
        assertFalse(streamType.isBean());
        assertFalse(streamType.isReader());
        assertTrue(streamType.isInputStream());

        String uriString = N.stringOf(URI.create("http://www.google.com"));
        assertEquals("http://www.google.com", uriString);
        assertEquals(URI.create("http://www.google.com"), N.valueOf(uriString, URI.class));
    }

    @Test
    public void testNumericValueOf() {
        Type type = N.typeOf(int.class);
        for (int i = -1000; i < 10000; i++) {
            assertEquals(i, ((Integer) type.valueOf(type.stringOf(i))).intValue());
        }

        List<Type<Object>> types = N.toList(byte.class, short.class, int.class, long.class).stream().map(TypeFactory::getType).toList();
        Map<Class<?>, Object[]> rangeValues = N.asMap(byte.class, N.asArray(Byte.MIN_VALUE, Byte.MAX_VALUE), short.class,
                N.asArray(Short.MIN_VALUE, Short.MAX_VALUE), int.class, N.asArray(-1000000, 1000000), long.class, N.asArray(-1000000, 1000000));
        for (Type<Object> numericType : types) {
            int minValue = ((Number) rangeValues.get(numericType.javaType())[0]).intValue();
            int maxValue = ((Number) rangeValues.get(numericType.javaType())[1]).intValue();
            assertTrue(minValue < maxValue);
            for (int i = minValue; i <= maxValue; i++) {
                assertEquals(i, ((Number) numericType.valueOf(String.valueOf(i))).intValue());
            }
        }
    }

    @Test
    public void testStatusEnums() {
        for (UnifiedStatus unifiedStatus : UnifiedStatus.values()) {
            assertEquals(unifiedStatus, UnifiedStatus.valueOf(unifiedStatus.name()));
        }
        for (AccountStatus status : AccountStatus.values()) {
            assertEquals(status, AccountStatus.fromCode(status.code()));
        }
        for (ServiceStatus status : ServiceStatus.values()) {
            assertEquals(status, ServiceStatus.fromCode(status.code()));
        }
        for (Gender gender : Gender.values()) {
            assertEquals(gender, Gender.of(gender.intValue()));
        }
        for (Month month : Month.values()) {
            assertEquals(month, Month.of(month.intValue()));
        }
        for (Color color : Color.values()) {
            assertEquals(color, Color.of(color.intValue()));
        }
        for (MediaType mediaType : MediaType.values()) {
            assertEquals(mediaType, MediaType.of(mediaType.intValue()));
        }
    }

    @Test
    public void testPrimitiveAndWrapperArrays() throws IOException {
        assertPrimitiveArray(N.typeOf(boolean[].class), Array.of(false, true, false), "[false, true, false]", true);
        assertPrimitiveArray(N.typeOf(char[].class), Array.of('a', 'b', 'c'), "[a, b, c]", false);
        assertPrimitiveArray(N.typeOf(byte[].class), Array.of((byte) 1, (byte) 2, (byte) 3), "[1, 2, 3]", true);
        assertPrimitiveArray(N.typeOf(short[].class), Array.of((short) 1, (short) 2, (short) 3), "[1, 2, 3]", true);
        assertPrimitiveArray(N.typeOf(int[].class), Array.of(1, 2, 3), "[1, 2, 3]", true);
        assertPrimitiveArray(N.typeOf(long[].class), Array.of(1L, 2L, 3L), "[1, 2, 3]", true);
        assertPrimitiveArray(N.typeOf(float[].class), Array.of(1f, 2f, 3f), "[1.0, 2.0, 3.0]", true);
        assertPrimitiveArray(N.typeOf(double[].class), Array.of(1d, 2d, 3d), "[1.0, 2.0, 3.0]", true);

        assertObjectArray(N.typeOf(Boolean[].class), asArray(false, true, null, false), "[false, true, null, false]", true);
        assertObjectArray(N.typeOf(Character[].class), asArray('a', 'b', null, 'c'), "[a, b, null, c]", false);
        assertObjectArray(N.typeOf(Byte[].class), asArray((byte) 1, (byte) 2, null, (byte) 3), "[1, 2, null, 3]", true);
        assertObjectArray(N.typeOf(Short[].class), asArray((short) 1, (short) 2, null, (short) 3), "[1, 2, null, 3]", true);
        assertObjectArray(N.typeOf(Integer[].class), asArray(1, 2, null, 3), "[1, 2, null, 3]", true);
        assertObjectArray(N.typeOf(Long[].class), asArray(1L, 2L, null, 3L), "[1, 2, null, 3]", true);
        assertObjectArray(N.typeOf(Float[].class), asArray(1f, 2f, null, 3f), "[1.0, 2.0, null, 3.0]", true);
        assertObjectArray(N.typeOf(Double[].class), asArray(1d, 2d, null, 3d), "[1.0, 2.0, null, 3.0]", true);

        Type<char[]> charType = N.typeOf(char[].class);
        assertEquals("['a', 'b', 'c']", charType.stringOf(new char[] { 'a', 'b', 'c' }));
        assertEquals("[a, b, c]", N.toList('a', 'b', 'c').toString());
    }

    @Test
    public void testCollectionAndPrimitiveListTypes() throws IOException {
        assertListType(new TypeReference<List<Boolean>>() {
        }.type(), N.toList(false, true, null, false), "[false, true, null, false]", Boolean.class, true);
        assertListType(new TypeReference<List<Character>>() {
        }.type(), N.toList('a', 'b', null, 'c'), null, Character.class, false);
        assertListType(new TypeReference<List<Byte>>() {
        }.type(), N.toList((byte) 1, (byte) 2, null, (byte) 3), "[1, 2, null, 3]", Byte.class, true);
        assertListType(new TypeReference<List<Short>>() {
        }.type(), N.toList((short) 1, (short) 2, null, (short) 3), "[1, 2, null, 3]", Short.class, true);
        assertListType(new TypeReference<List<Integer>>() {
        }.type(), N.toList(1, 2, null, 3), "[1, 2, null, 3]", Integer.class, true);
        assertListType(new TypeReference<List<Long>>() {
        }.type(), N.toList(1L, 2L, null, 3L), "[1, 2, null, 3]", Long.class, true);
        assertListType(new TypeReference<List<Float>>() {
        }.type(), N.toList(1f, 2f, null, 3f), "[1.0, 2.0, null, 3.0]", Float.class, true);
        assertListType(new TypeReference<List<Double>>() {
        }.type(), N.toList(1d, 2d, null, 3d), "[1.0, 2.0, null, 3.0]", Double.class, true);

        assertPrimitiveList(N.typeOf(BooleanList.class), BooleanList.of(false, true, false), "[false, true, false]", boolean.class, true);
        assertPrimitiveList(N.typeOf(CharList.class), CharList.of('a', 'b', 'c'), "[a, b, c]", char.class, false);
        assertPrimitiveList(N.typeOf(ByteList.class), ByteList.of((byte) 1, (byte) 2, (byte) 3), "[1, 2, 3]", byte.class, true);
        assertPrimitiveList(N.typeOf(ShortList.class), ShortList.of((short) 1, (short) 2, (short) 3), "[1, 2, 3]", short.class, true);
        assertPrimitiveList(N.typeOf(IntList.class), IntList.of(1, 2, 3), "[1, 2, 3]", int.class, true);
        assertPrimitiveList(N.typeOf(LongList.class), LongList.of(1, 2, 3), "[1, 2, 3]", long.class, true);
        assertPrimitiveList(N.typeOf(FloatList.class), FloatList.of(1, 2, 3), "[1.0, 2.0, 3.0]", float.class, true);
        assertPrimitiveList(N.typeOf(DoubleList.class), DoubleList.of(1, 2, 3), "[1.0, 2.0, 3.0]", double.class, true);

        Type<Object[]> objectArrayType = N.typeOf(Object[].class);
        Writer writer = new StringWriter();
        objectArrayType.appendTo(writer, N.asArray("abc", "123", "213"));
        assertEquals("[abc, 123, 213]", writer.toString());
        String listStr = N.stringOf(objectArrayType.arrayToCollection(N.asArray("abc", "123", "213"), List.class));
        String setStr = N.stringOf(objectArrayType.arrayToCollection(N.asArray("abc", "123", "213"), Set.class));
        String queueStr = N.stringOf(objectArrayType.arrayToCollection(N.asArray("abc", "123", "213"), Queue.class));
        assertTrue(listStr.contains("abc") && listStr.contains("123") && listStr.contains("213"));
        assertTrue(setStr.contains("abc") && setStr.contains("123") && setStr.contains("213"));
        assertTrue(queueStr.contains("abc") && queueStr.contains("123") && queueStr.contains("213"));

        Type<Queue> queueType = N.typeOf(Queue.class);
        Writer queueWriter = new StringWriter();
        queueType.appendTo(queueWriter, N.toQueue("abc", "123", "213"));
        assertEquals("[abc, 123, 213]", queueWriter.toString());
        Type<?> collectionType = N.typeOf("Collection<String>");
        assertEquals(Collection.class, collectionType.javaType());
        assertEquals(String.class, collectionType.elementType().javaType());
    }

    @Test
    public void testStreamReaderAndXmlCalendar() throws IOException {
        assertStreamContent(ClobAsciiStreamType.class.cast(N.typeOf(ClobAsciiStreamType.CLOB_ASCII_STREAM)));
        assertStreamContent(AsciiStreamType.class.cast(N.typeOf(AsciiStreamType.ASCII_STREAM)));
        assertStreamContent(N.typeOf(InputStream.class));

        Type<Reader> readerType = N.typeOf(Reader.class);
        Writer writer = new StringWriter();
        readerType.appendTo(writer, IOUtil.stringToReader("[abc, 123, 213]"));
        assertEquals("[abc, 123, 213]", writer.toString());
        BufferedJsonWriter jsonWriter = Objectory.createBufferedJsonWriter();
        try {
            readerType.serializeTo(jsonWriter, IOUtil.stringToReader("[abc, 123, 213]"), null);
            assertEquals("[abc, 123, 213]", jsonWriter.toString());
        } finally {
            Objectory.recycle(jsonWriter);
        }
        jsonWriter = Objectory.createBufferedJsonWriter();
        try {
            readerType.serializeTo(jsonWriter, IOUtil.stringToReader("[abc, 123, 213]"), JsonSerConfig.create().setStringQuotation('\''));
            assertEquals("'[abc, 123, 213]'", jsonWriter.toString());
        } finally {
            Objectory.recycle(jsonWriter);
        }

        Type<XMLGregorianCalendar> calType = N.typeOf(XMLGregorianCalendar.class);
        XMLGregorianCalendar cal = Dates.currentXMLGregorianCalendar();
        BufferedJsonWriter calWriter = Objectory.createBufferedJsonWriter();
        try {
            calType.serializeTo(calWriter, cal, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.LONG));
            assertEquals(String.valueOf(cal.toGregorianCalendar().getTimeInMillis()), calWriter.toString());
        } finally {
            Objectory.recycle(calWriter);
        }
        calWriter = Objectory.createBufferedJsonWriter();
        try {
            calType.serializeTo(calWriter, cal, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME));
            assertTrue(calWriter.toString().contains("T"));
        } finally {
            Objectory.recycle(calWriter);
        }
        calWriter = Objectory.createBufferedJsonWriter();
        try {
            calType.serializeTo(calWriter, cal, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP));
            assertTrue(calWriter.toString().contains("T"));
        } finally {
            Objectory.recycle(calWriter);
        }
    }

    private static <T> void assertPrimitiveArray(Type<T> type, T value, String expected, boolean assertJson) throws IOException {
        Writer writer = new StringWriter();
        type.appendTo(writer, value);
        assertEquals(expected, writer.toString());
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(bw, value, JsonSerConfig.create());
            if (assertJson) {
                assertEquals(expected, bw.toString());
            }
        } finally {
            Objectory.recycle(bw);
        }
        assertTrue(type.equals(value, type.valueOf(type.stringOf(value))));
        assertEquals(N.typeOf(value.getClass().getComponentType()), type.elementType());
        assertTrue(type.isArray());
        assertTrue(type.isPrimitiveArray());
        assertFalse(type.isObjectArray());
    }

    private static <T> void assertObjectArray(Type<T> type, T value, String expected, boolean assertJson) throws IOException {
        Writer writer = new StringWriter();
        type.appendTo(writer, value);
        assertEquals(expected, writer.toString());
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(bw, value, JsonSerConfig.create());
            if (assertJson) {
                assertEquals(expected, bw.toString());
            }
        } finally {
            Objectory.recycle(bw);
        }
        assertTrue(type.equals(value, type.valueOf(type.stringOf(value))));
        assertEquals(N.typeOf(value.getClass().getComponentType()), type.elementType());
        assertTrue(type.isArray());
        assertFalse(type.isPrimitiveArray());
        assertTrue(type.isObjectArray());
    }

    private static <T> void assertListType(Type<T> type, T value, String expected, Class<?> elementClass, boolean assertJson) throws IOException {
        Writer writer = new StringWriter();
        type.appendTo(writer, value);
        if (expected != null) {
            assertEquals(expected, writer.toString());
        }
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(bw, value, JsonSerConfig.create());
            if (assertJson && expected != null) {
                assertEquals(expected, bw.toString());
            }
        } finally {
            Objectory.recycle(bw);
        }
        assertTrue(N.equals(value, type.valueOf(type.stringOf(value))));
        assertEquals(N.typeOf(elementClass), type.elementType());
        assertTrue(type.isList());
        assertFalse(type.isSet());
        assertTrue(type.isCollection());
    }

    private static <T> void assertPrimitiveList(Type<T> type, T value, String expected, Class<?> elementClass, boolean assertJson) throws IOException {
        Writer writer = new StringWriter();
        type.appendTo(writer, value);
        assertEquals(expected, writer.toString());
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(bw, value, JsonSerConfig.create());
            if (assertJson) {
                assertEquals(expected, bw.toString());
            }
        } finally {
            Objectory.recycle(bw);
        }
        assertTrue(N.equals(value, type.valueOf(type.stringOf(value))));
        assertEquals(N.typeOf(elementClass), type.elementType());
        assertTrue(type.isPrimitiveList());
    }

    private static void assertStreamContent(Type<InputStream> type) throws IOException {
        Writer writer = new StringWriter();
        type.appendTo(writer, IOUtil.stringToInputStream("[abc, 123, 213]"));
        assertEquals("[abc, 123, 213]", writer.toString());
        BufferedJsonWriter writer1 = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(writer1, IOUtil.stringToInputStream("[abc, 123, 213]"), null);
            assertEquals("[abc, 123, 213]", writer1.toString());
        } finally {
            Objectory.recycle(writer1);
        }
        writer1 = Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(writer1, IOUtil.stringToInputStream("[abc, 123, 213]"), JsonSerConfig.create().setStringQuotation('\''));
            assertEquals("'[abc, 123, 213]'", writer1.toString());
        } finally {
            Objectory.recycle(writer1);
        }
    }

    @Test
    public void reviewFixes20260906_nameAndDeclaringNameFollowThePackagePrefixRule() {
        // T1-06: java.lang / java.util / java.time / com.landawn.abacus prefixes are omitted, other packages
        // keep their canonical name; declaringName() only swaps a concrete collection/map for its interface.
        assertEquals("List<String>", Type.ofList(String.class).name());
        assertEquals("List<String>", Type.ofList(String.class).declaringName());
        assertEquals("List<String>", Type.of("java.util.List<java.lang.String>").name());
        assertEquals("ArrayList<String>", Type.of("ArrayList<String>").name());
        assertEquals("List<String>", Type.of("ArrayList<String>").declaringName());
        assertEquals("HashMap<String, Integer>", Type.of("HashMap<String, Integer>").name());
        assertEquals("Map<String, Integer>", Type.of("HashMap<String, Integer>").declaringName());
        assertEquals("Map<String, java.io.File>", Type.ofMap(String.class, java.io.File.class).name());
        assertEquals("Map<String, java.io.File>", Type.ofMap(String.class, java.io.File.class).declaringName());
        assertEquals("LocalDate", Type.of(java.time.LocalDate.class).name());
        // different spellings are cached separately but are equal
        assertTrue(Type.of("List<String>").equals(Type.of("java.util.List<java.lang.String>")));
        assertEquals(Type.of("List<String>").hashCode(), Type.of("java.util.List<java.lang.String>").hashCode());
    }
}
