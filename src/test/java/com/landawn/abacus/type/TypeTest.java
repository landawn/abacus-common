package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.types.MyConstant;
import com.landawn.abacus.util.AccountStatus;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.BufferedJSONWriter;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Color;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.DayOfWeek;
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
import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.ServiceStatus;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Status;
import com.landawn.abacus.util.StringWriter;

import lombok.Builder;
import lombok.Data;

@SuppressWarnings("rawtypes")
@Tag("old-test")
public class TypeTest extends AbstractTest {

    @Test
    public void test_GuavaTypes() throws Exception {
        {
            com.google.common.collect.Multiset<Character> guavaMultiset = com.google.common.collect.HashMultiset.create(N.asList('a', 'b', 'c', 'a'));
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
            N.println(data);

            String json = N.toJson(data, true);
            N.println(json);
            DataTypeA data2 = N.fromJson(json, DataTypeA.class);
            assertEquals(data, data2);

            BeanInfo beanInfo = ParserUtil.getBeanInfo(DataTypeA.class);
            for (PropInfo propInfo : beanInfo.propInfoList) {
                N.println(propInfo.type.name());
                N.println(propInfo.type.declaringName());
            }
        }
    }

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

    @Test
    public void test_001() {
        for (int i = 0; i < 3; i++) {
            assertEquals("Map<List<com.landawn.abacus.util.stream.Stream>, String>",
                    TypeFactory.getType("Map<List<com.landawn.abacus.util.stream.Stream>, String>").name());
            assertEquals("Map<List<com.landawn.abacus.util.stream.Stream<String>>, String>",
                    TypeFactory.getType("Map<List<com.landawn.abacus.util.stream.Stream<String>>, String>").declaringName());
        }
    }

    @Test
    public void testURI() {
        String uriString = N.stringOf(URI.create("http://www.google.com"));
        N.println(uriString);

        URI uri = N.valueOf(uriString, URI.class);

        assertEquals(URI.create("http://www.google.com"), uri);
    }

    @Test
    public void test_type2() {
        TypeFactory.getType("List<com.landawn.abacus.types.WeekDay(false)>");
    }

    @Test
    public void test_char() {
        {
            char[] a = { 'a', 'b', 'c' };
            N.println(Arrays.toString(a));
        }

        {
            List<?> list = N.asList('a', 'b', 'c');
            N.println(list.toString());
        }
    }

    @Test
    public void test_Multimap() throws Exception {
        {

            Type<Multimap<String, Date, List<Date>>> type = N.typeOf("Multimap<String, Date, List<Date>>");

            assertEquals("Multimap<String, Date, List<Date>>", type.name());
            assertEquals("Multimap<String, Date, List<Date>>", type.declaringName());
            N.println(type.clazz());

            assertEquals(Multimap.class, type.clazz());
            assertEquals(String.class, type.getParameterTypes()[0].clazz());
            assertEquals(Date.class, type.getParameterTypes()[1].clazz());
            assertEquals(List.class, type.getParameterTypes()[2].clazz());
            assertEquals(Date.class, type.getParameterTypes()[2].getElementType().clazz());

            assertTrue(type.isSerializable());
        }

        {
            Type<Multimap<String, Date, List<Date>>> type = N.typeOf("Multimap<String, List<Date>>");

            assertEquals("Multimap<String, List<Date>>", type.name());
            assertEquals("Multimap<String, List<Date>>", type.declaringName());
            N.println(type.clazz());

            assertEquals(Multimap.class, type.clazz());
            assertEquals(String.class, type.getParameterTypes()[0].clazz());
            assertEquals(List.class, type.getParameterTypes()[1].clazz());
            assertEquals(Date.class, type.getParameterTypes()[1].getElementType().clazz());

            assertTrue(type.isSerializable());
        }
    }

    @Test
    public void test_Multiset() throws Exception {
        Type<Multiset<Date>> type = N.typeOf("Multiset<Date>");

        N.println(type.clazz());

        assertEquals(Multiset.class, type.clazz());
        assertEquals(Date.class, type.getElementType().clazz());
        assertEquals(Date.class, type.getParameterTypes()[0].clazz());

        assertTrue(type.isSerializable());
    }

    @Test
    public void test_valueOf() throws Exception {
        List<Type<Object>> types = N.asList(byte.class, short.class, int.class, long.class)
                .stream()
                .map(cls -> TypeFactory.getType(cls))
                .toList();

        Map<Class<?>, Object[]> rangValues = N.asMap(byte.class, N.asArray(Byte.MIN_VALUE, Byte.MAX_VALUE), short.class,
                N.asArray(Short.MIN_VALUE, Short.MAX_VALUE), int.class, N.asArray(-1000000, 1000000), long.class, N.asArray(-1000000, 1000000));

        for (Type<Object> type : types) {
            int minValue = ((Number) rangValues.get(type.clazz())[0]).intValue();
            int maxValue = ((Number) rangValues.get(type.clazz())[1]).intValue();
            N.println(minValue + " : " + maxValue);

            for (int i = minValue; i <= maxValue; i++) {
                assertEquals(i, ((Number) type.valueOf(String.valueOf(i))).intValue());
            }
        }
    }

    @Test
    public void test_XMLGregorianCalendar() throws IOException {
        final Type<Object> type = N.typeOf(XMLGregorianCalendar.class);
        Dates.currentXMLGregorianCalendar();

        BufferedJSONWriter writer = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer, Dates.currentXMLGregorianCalendar(), JSC.of(DateTimeFormat.LONG));

        N.println(writer.toString());

        writer = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer, Dates.currentXMLGregorianCalendar(), JSC.of(DateTimeFormat.ISO_8601_DATE_TIME));

        N.println(writer.toString());

        writer = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer, Dates.currentXMLGregorianCalendar(), JSC.of(DateTimeFormat.ISO_8601_TIMESTAMP));

        N.println(writer.toString());

    }

    @Test
    public void test_status() throws IOException {
        for (Status status : Status.values()) {
            assertEquals(status, Status.valueOf(status.name()));
        }

        for (AccountStatus status : AccountStatus.values()) {
            assertEquals(status, AccountStatus.valueOf(status.intValue()));
        }

        for (ServiceStatus status : ServiceStatus.values()) {
            assertEquals(status, ServiceStatus.valueOf(status.intValue()));
        }

        for (Gender gender : Gender.values()) {
            assertEquals(gender, Gender.valueOf(gender.intValue()));
        }

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            assertEquals(dayOfWeek, DayOfWeek.valueOf(dayOfWeek.intValue()));
        }

        for (Month month : Month.values()) {
            assertEquals(month, Month.valueOf(month.intValue()));
        }

        for (Color color : Color.values()) {
            assertEquals(color, Color.valueOf(color.intValue()));
        }

        for (MediaType mediaType : MediaType.values()) {
            assertEquals(mediaType, MediaType.valueOf(mediaType.intValue()));
        }
    }

    @Test
    public void test_clazz() throws IOException {
        Type<Object> type = N.typeOf("clazz<int>");

        N.println(type.clazz());

        assertEquals(int.class, type.clazz());
    }

    @Test
    public void test_primaryArray() throws IOException {
        {
            Type<Object> type = N.typeOf(boolean[].class);
            boolean[] a = Array.of(false, true, false);
            String str = "[false, true, false]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(char[].class);
            char[] a = Array.of('a', 'b', 'c');
            String str = "[a, b, c]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(byte[].class);
            byte[] a = Array.of((byte) 1, (byte) 2, (byte) 3);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(short[].class);
            short[] a = Array.of((short) 1, (short) 2, (short) 3);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(int[].class);
            int[] a = Array.of(1, 2, 3);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(long[].class);
            long[] a = Array.of(1L, 2L, 3L);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(float[].class);
            float[] a = Array.of(1f, 2f, 3f);
            String str = "[1.0, 2.0, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(double[].class);
            double[] a = Array.of(1d, 2d, 3d);
            String str = "[1.0, 2.0, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertTrue(type.isPrimitiveArray());
            assertFalse(type.isObjectArray());
        }
    }

    @Test
    public void test_primaryArray_2() throws IOException {
        {
            Type<Object> type = N.typeOf(Boolean[].class);
            Boolean[] a = asArray(false, true, null, false);
            String str = "[false, true, null, false]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(Character[].class);
            Character[] a = asArray('a', 'b', null, 'c');
            String str = "[a, b, null, c]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(Byte[].class);
            Byte[] a = asArray((byte) 1, (byte) 2, null, (byte) 3);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(Short[].class);
            Short[] a = asArray((short) 1, (short) 2, null, (short) 3);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(Integer[].class);
            Integer[] a = asArray(1, 2, null, 3);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(Long[].class);
            Long[] a = asArray(1L, 2L, null, 3L);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(Float[].class);
            Float[] a = asArray(1f, 2f, null, 3f);
            String str = "[1.0, 2.0, null, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }

        {
            Type<Object> type = N.typeOf(Double[].class);
            Double[] a = asArray(1d, 2d, null, 3d);
            String str = "[1.0, 2.0, null, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, a);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, a, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(a, type.valueOf(type.stringOf(a))));

            assertEquals(N.typeOf(a.getClass().getComponentType()), type.getElementType());
            assertTrue(type.isArray());
            assertFalse(type.isPrimitiveArray());
            assertTrue(type.isObjectArray());
        }
    }

    @Test
    public void test_primaryArray_3() throws IOException {
        {
            Type<Object> type = N.typeOf("List<Boolean>");
            List<?> list = N.asList(false, true, null, false);
            String str = "[false, true, null, false]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Boolean.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());
        }

        {
            Type<Object> type = N.typeOf("List<Character>");
            List<?> list = N.asList('a', 'b', null, 'c');
            Writer writer = new StringWriter();
            type.appendTo(writer, list);

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Character.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());

        }

        {
            Type<Object> type = N.typeOf("List<Byte>");
            List<?> list = N.asList((byte) 1, (byte) 2, null, (byte) 3);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Byte.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());
        }

        {
            Type<Object> type = N.typeOf("List<Short>");
            List<?> list = N.asList((short) 1, (short) 2, null, (short) 3);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Short.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());
        }

        {
            Type<Object> type = N.typeOf("List<Integer>");
            List<?> list = N.asList(1, 2, null, 3);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Integer.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());
        }

        {
            Type<Object> type = N.typeOf("List<Long>");
            List<?> list = N.asList(1L, 2L, null, 3L);
            String str = "[1, 2, null, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Long.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());
        }

        {
            Type<Object> type = N.typeOf("List<Float>");
            List<?> list = N.asList(1f, 2f, null, 3f);
            String str = "[1.0, 2.0, null, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Float.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());
        }

        {
            Type<Object> type = N.typeOf("List<Double>");
            List<?> list = N.asList(1d, 2d, null, 3d);
            String str = "[1.0, 2.0, null, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(Double.class), type.getElementType());
            assertTrue(type.isList());
            assertFalse(type.isSet());
            assertTrue(type.isCollection());
        }
    }

    @Test
    public void test_primaryArray_4() throws IOException {
        {
            Type<Object> type = N.typeOf(BooleanList.class);
            BooleanList list = BooleanList.of(false, true, false);
            String str = "[false, true, false]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(boolean.class), type.getElementType());
            assertTrue(type.isPrimitiveList());
        }

        {
            Type<Object> type = N.typeOf(CharList.class);
            CharList list = CharList.of('a', 'b', 'c');
            String str = "[a, b, c]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(char.class), type.getElementType());
            assertTrue(type.isPrimitiveList());

        }

        {
            Type<Object> type = N.typeOf(ByteList.class);
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(byte.class), type.getElementType());
            assertTrue(type.isPrimitiveList());
        }

        {
            Type<Object> type = N.typeOf(ShortList.class);
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(short.class), type.getElementType());
            assertTrue(type.isPrimitiveList());
        }

        {
            Type<Object> type = N.typeOf(IntList.class);
            IntList list = IntList.of(1, 2, 3);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(int.class), type.getElementType());
            assertTrue(type.isPrimitiveList());

        }

        {
            Type<Object> type = N.typeOf(LongList.class);
            LongList list = LongList.of(1, 2, 3);
            String str = "[1, 2, 3]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(long.class), type.getElementType());
            assertTrue(type.isPrimitiveList());

        }

        {
            Type<Object> type = N.typeOf(FloatList.class);
            FloatList list = FloatList.of(1, 2, 3);
            String str = "[1.0, 2.0, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(float.class), type.getElementType());
            assertTrue(type.isPrimitiveList());
        }

        {
            Type<Object> type = N.typeOf(DoubleList.class);
            DoubleList list = DoubleList.of(1, 2, 3);
            String str = "[1.0, 2.0, 3.0]";
            Writer writer = new StringWriter();
            type.appendTo(writer, list);
            assertEquals(str, writer.toString());

            BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
            type.writeCharacter(bw, list, JSC.create());
            assertEquals(str, bw.toString());

            assertTrue(N.equals(list, type.valueOf(type.stringOf(list))));

            assertEquals(N.typeOf(double.class), type.getElementType());
            assertTrue(type.isPrimitiveList());
        }
    }

    @Test
    public void test_AbstractType() throws IOException {
        Type<InputStream> type = N.typeOf(InputStream.class);

        assertFalse(type.isArray());
        assertFalse(type.isPrimitiveWrapper());
        assertFalse(type.isPrimitiveList());
        assertFalse(type.isString());
        assertFalse(type.isDate());
        assertFalse(type.isCalendar());
        assertFalse(type.isPrimitiveArray());
        assertFalse(type.isObjectArray());
        assertFalse(type.isBean());
        assertFalse(type.isReader());
        assertTrue(type.isInputStream());
    }

    @Test
    public void test_ClobAsciiStreamType() throws IOException {
        Type<InputStream> type = N.typeOf(ClobAsciiStreamType.CLOB_ASCII_STREAM);

        Writer writer = new StringWriter();
        type.appendTo(writer, IOUtil.string2InputStream("[abc, 123, 213]"));

        N.println(writer.toString());

        BufferedJSONWriter writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2InputStream("[abc, 123, 213]"), null);

        N.println(writer1.toString());

        writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2InputStream("[abc, 123, 213]"), JSC.create().setStringQuotation('\''));

        N.println(writer1.toString());
    }

    @Test
    public void test_AsciiStreamType() throws IOException {
        Type<InputStream> type = N.typeOf(AsciiStreamType.ASCII_STREAM);

        Writer writer = new StringWriter();
        type.appendTo(writer, IOUtil.string2InputStream("[abc, 123, 213]"));

        N.println(writer.toString());

        BufferedJSONWriter writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2InputStream("[abc, 123, 213]"), null);

        N.println(writer1.toString());

        writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2InputStream("[abc, 123, 213]"), JSC.create().setStringQuotation('\''));

        N.println(writer1.toString());
    }

    @Test
    public void test_inputStream() throws IOException {
        Type<InputStream> type = N.typeOf(InputStream.class);

        Writer writer = new StringWriter();
        type.appendTo(writer, IOUtil.string2InputStream("[abc, 123, 213]"));

        N.println(writer.toString());

        BufferedJSONWriter writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2InputStream("[abc, 123, 213]"), null);

        N.println(writer1.toString());

        writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2InputStream("[abc, 123, 213]"), JSC.create().setStringQuotation('\''));

        N.println(writer1.toString());
    }

    @Test
    public void test_reader() throws IOException {
        Type<Reader> type = N.typeOf(Reader.class);

        Writer writer = new StringWriter();
        type.appendTo(writer, IOUtil.string2Reader("[abc, 123, 213]"));

        N.println(writer.toString());

        BufferedJSONWriter writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2Reader("[abc, 123, 213]"), null);

        N.println(writer1.toString());

        writer1 = Objectory.createBufferedJSONWriter();
        type.writeCharacter(writer1, IOUtil.string2Reader("[abc, 123, 213]"), JSC.create().setStringQuotation('\''));

        N.println(writer1.toString());
    }

    @Test
    public void test_ObjectArray() throws IOException {
        Type<Object[]> type = N.typeOf(Object[].class);

        Writer writer = new StringWriter();
        type.appendTo(writer, N.asArray("abc", "123", "213"));

        N.println(writer.toString());

        N.println(type.array2Collection(N.asArray("abc", "123", "213"), List.class));
        N.println(type.array2Collection(N.asArray("abc", "123", "213"), Set.class));
        N.println(type.array2Collection(N.asArray("abc", "123", "213"), Queue.class));
    }

    @Test
    public void test_collection() throws IOException {
        Type<Object> type = N.typeOf(Queue.class);

        Writer writer = new StringWriter();
        type.appendTo(writer, N.asQueue("abc", "123", "213"));

        N.println(writer.toString());

        type = N.typeOf("Collection<String>");

        assertEquals(Collection.class, type.clazz());
    }

    @Test
    public void test_getType() {
        N.println(N.typeOf("Type"));
        N.println(N.typeOf("Type<?>"));
        N.println(N.typeOf("Type<Object>"));
        N.println(N.typeOf("Type<String>"));
        N.println(N.typeOf("Type<Integer>"));
        N.println(N.typeOf("Type<int>"));
        N.println(N.typeOf("Type<unknown>"));
    }

    @Test
    public void test_int2String() {

        long startTime = System.currentTimeMillis();
        Type type = N.typeOf(int.class);
        for (int k = 0; k < 1000; k++) {
            for (int i = -1000; i < 10000; i++) {
                assertEquals(i, ((Integer) type.valueOf(type.stringOf(i))).intValue());
            }
        }

        N.println("Took: " + (System.currentTimeMillis() - startTime));
    }

    @Test
    public void test_int2String2() {
        final Type type = N.typeOf(int.class);
        Profiler.run(1, 100, 1, () -> {
            for (int i = -1000; i < 10000; i++) {
                assertEquals(i, ((Integer) type.valueOf(type.stringOf(i))).intValue());
            }
        }).printResult();
    }

    //    @Test
    //    public void test_byte() {
    //        List<Type<Number>> types = TypeFactory.getType(byte.class, short.class, int.class, long.class, float.class, double.class);
    //        String[] strs = { "2", "-1", "2l", "-1l", "2f", "-1f", "2d", "-1d" };
    //
    //        for (Type type : types) {
    //            for (String str : strs) {
    //                N.println(type.valueOf(str));
    //            }
    //        }
    //    }
    //
    //    @Test
    //    public void test_byte_2() {
    //        List<Type<Number>> types = TypeFactory.getType(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class);
    //        String[] strs = { "2", "-1", "2l", "-1l", "2f", "-1f", "2d", "-1d" };
    //
    //        for (Type type : types) {
    //            for (String str : strs) {
    //                N.println(type.valueOf(str));
    //            }
    //        }
    //    }

    @Test
    public void test_TypeFactory() {
        // N.println(TypeFactory.getType(Long.class, long.class, int.class, Timestamp.class));

        //    List<Class<?>> classes = N.asList(Long.class, long.class, int.class, Timestamp.class); 
        //    N.println(TypeFactory.getType(classes));

        N.println(TypeFactory.getType("List<String>").name());
        N.println(TypeFactory.getType("ArrayList<String>").name());
        N.println(TypeFactory.getType("Set<String>").name());
        N.println(TypeFactory.getType("HashSet<String>").name());
        N.println(TypeFactory.getType("LinkedHashSet<String>").name());
        N.println(TypeFactory.getType("Map<String, Object>").name());
        N.println(TypeFactory.getType("HashMap<String, Object>").name());
        N.println(TypeFactory.getType("LinkedHashMap<String, Object>").name());

        try {
            TypeFactory.getType("LinkedHashSet<String, Integer>").name();
            fail("Should throw RuntimeException");
        } catch (IllegalArgumentException e) {
        }

        try {
            TypeFactory.getType("LinkedHashSet<String>(||)").name();
            fail("Should throw RuntimeException");
        } catch (IllegalArgumentException e) {
        }

        try {
            TypeFactory.getType("LinkedHashMap<String>").name();
            fail("Should throw RuntimeException");
        } catch (IllegalArgumentException e) {
        }

        try {
            TypeFactory.getType("LinkedHashMap<String, Object>(||)").name();
            fail("Should throw RuntimeException");
        } catch (IllegalArgumentException e) {
        }
    }

    @BeforeEach
    public void setUp() {
        TypeFactory.getType(String.class);
        TypeFactory.getType("JSON<Map>");
        TypeFactory.getType("XML<Map>");

        TypeFactory.getType("JSON<List>");
        TypeFactory.getType("XML<List>");

        TypeFactory.getType(MyConstant.class);
        TypeFactory.getType(Status.class);
        TypeFactory.getType(Status.class);
        TypeFactory.getType("Status(true)");
    }

    @Deprecated
    static <T> T[] asArray(final T... a) {
        return a;
    }
}
