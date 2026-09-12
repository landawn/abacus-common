package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.TypeReference;

public class TypeInterfaceTest extends TestBase {

    private Type<String> stringType;
    private Type<Integer> integerType;
    private Type<List<String>> listType;
    private Type<Map<String, Integer>> mapType;

    @BeforeEach
    public void setUp() {
        stringType = createType(String.class);
        integerType = createType(Integer.class);
        listType = createType(new TypeReference<List<String>>() {
        });
        mapType = createType(new TypeReference<Map<String, Integer>>() {
        });
    }

    @Test
    public void testOfFactories() {
        Type<String> ofClass = Type.of(String.class);
        assertEquals(String.class, ofClass.javaType());
        Type<List<String>> ofRef = Type.of(new TypeReference<List<String>>() {
        });
        assertNotNull(ofRef);
        assertNotNull(Type.of("String"));
        assertNotNull(Type.of("List<String>"));

        List<Type<Object>> allVarargs = Type.ofAll(String.class, Integer.class, Double.class);
        assertEquals(3, allVarargs.size());
        Collection<Class<? extends Number>> classes = Arrays.asList(Integer.class, Double.class, Float.class);
        assertEquals(3, Type.ofAll(classes).size());

        assertTrue(Type.ofList(String.class).isList());
        assertTrue(Type.ofLinkedList(String.class).isList());
        assertTrue(Type.ofListOfMap(String.class, Integer.class).isList());
        assertTrue(Type.ofListOfLinkedHashMap(String.class, Integer.class).isList());
        assertTrue(Type.ofSet(String.class).isSet());
        assertTrue(Type.ofSetOfMap(String.class, Integer.class).isSet());
        assertTrue(Type.ofSetOfLinkedHashMap(String.class, Integer.class).isSet());
        assertTrue(Type.ofLinkedHashSet(String.class).isSet());
        assertTrue(Type.ofSortedSet(String.class).isSet());
        assertTrue(Type.ofNavigableSet(String.class).isSet());
        assertTrue(Type.ofTreeSet(String.class).isSet());
        assertTrue(Type.ofQueue(String.class).isCollection());
        assertTrue(Type.ofDeque(String.class).isCollection());
        assertTrue(Type.ofArrayDeque(String.class).isCollection());
        assertTrue(Type.ofLinkedBlockingQueue(String.class).isCollection());
        assertTrue(Type.ofConcurrentLinkedQueue(String.class).isCollection());
        assertTrue(Type.ofPriorityQueue(String.class).isCollection());
        assertTrue(Type.ofPropsMap().isMap());
        assertTrue(Type.ofMap(String.class, Integer.class).isMap());
        assertTrue(Type.ofLinkedHashMap(String.class, Integer.class).isMap());
        assertTrue(Type.ofSortedMap(String.class, Integer.class).isMap());
        assertTrue(Type.ofNavigableMap(String.class, Integer.class).isMap());
        assertTrue(Type.ofTreeMap(String.class, Integer.class).isMap());
        assertTrue(Type.ofConcurrentMap(String.class, Integer.class).isMap());
        assertTrue(Type.ofConcurrentHashMap(String.class, Integer.class).isMap());
        assertNotNull(Type.ofMultiset(String.class));
        assertNotNull(Type.ofListMultimap(String.class, Integer.class));
        assertNotNull(Type.ofSetMultimap(String.class, Integer.class));
    }

    @Test
    public void testNamesAndJavaType() {
        assertTrue(stringType.name().length() > 0);
        assertTrue(stringType.declaringName().length() > 0);
        assertTrue(stringType.xmlName().length() > 0);
        assertEquals(String.class, stringType.javaType());
        assertEquals(Integer.class, integerType.javaType());
        assertEquals(String.class, listType.elementType().javaType());
        assertNull(stringType.elementType());
        assertEquals(2, mapType.parameterTypes().size());
        assertEquals(0, stringType.parameterTypes().size());
        assertEquals(Type.SerializationType.SERIALIZABLE, stringType.serializationType());
        assertNull(stringType.defaultValue());
        assertNull(integerType.defaultValue());
        assertTrue(stringType.isDefaultValue(null));
        assertFalse(stringType.isDefaultValue("test"));
        assertFalse(integerType.isDefaultValue(0));
        assertFalse(integerType.isDefaultValue(1));
    }

    @Test
    public void testTypeFlags() {
        assertTrue(createType(int.class).isPrimitive());
        assertFalse(stringType.isPrimitive());
        assertTrue(integerType.isPrimitiveWrapper());
        assertFalse(stringType.isPrimitiveWrapper());
        assertFalse(stringType.isPrimitiveList());
        assertFalse(listType.isPrimitiveList());
        assertTrue(createType(Boolean.class).isBoolean());
        assertFalse(stringType.isBoolean());
        assertTrue(integerType.isNumber());
        assertFalse(stringType.isNumber());
        assertTrue(stringType.isString());
        assertFalse(integerType.isString());
        assertTrue(stringType.isCharSequence());
        assertFalse(integerType.isCharSequence());
        assertTrue(createType(Date.class).isDate());
        assertFalse(stringType.isDate());
        assertTrue(createType(Calendar.class).isCalendar());
        assertFalse(stringType.isCalendar());
        assertFalse(stringType.isJodaDateTime());
        assertTrue(createType(java.time.LocalDateTime.class).isTemporal());
        assertFalse(stringType.isTemporal());
        assertTrue(createType(int[].class).isPrimitiveArray());
        assertFalse(stringType.isPrimitiveArray());
        assertTrue(createType(byte[].class).isPrimitiveByteArray());
        assertFalse(stringType.isPrimitiveByteArray());
        assertTrue(createType(String[].class).isObjectArray());
        assertFalse(stringType.isObjectArray());
        assertTrue(createType(String[].class).isArray());
        assertTrue(createType(int[].class).isArray());
        assertFalse(stringType.isArray());
        assertTrue(listType.isList());
        assertFalse(stringType.isList());
        Type<java.util.Set<String>> setType = createType(new TypeReference<java.util.Set<String>>() {
        });
        assertTrue(setType.isSet());
        assertFalse(stringType.isSet());
        assertTrue(listType.isCollection());
        assertFalse(stringType.isCollection());
        assertTrue(mapType.isMap());
        assertFalse(stringType.isMap());
        assertFalse(stringType.isBean());
        assertFalse(stringType.isMapEntity());
        assertFalse(stringType.isEntityId());
        assertFalse(stringType.isDataset());
        assertFalse(stringType.isInputStream());
        assertFalse(stringType.isReader());
        assertFalse(stringType.isByteBuffer());
        assertTrue(listType.isParameterizedType());
        assertFalse(stringType.isParameterizedType());
        assertTrue(stringType.isImmutable());
        assertFalse(listType.isImmutable());
        assertTrue(stringType.isComparable());
        assertTrue(integerType.isComparable());
        assertTrue(stringType.isSerializable());
        assertTrue(createType(Integer.class).isSerializable());
        assertTrue(createType(Boolean.class).isSerializable());
        assertTrue(createType(java.math.BigDecimal.class).isSerializable());
        assertFalse(createType("Map<String, Integer>").isSerializable());
        assertFalse(createType(com.landawn.abacus.util.Dataset.class).isSerializable());
        assertTrue(createType(Object.class).isObject());
        assertFalse(stringType.isObject());
        Type<Optional<String>> optionalType = createType(new TypeReference<Optional<String>>() {
        });
        assertTrue(optionalType.isOptionalOrNullable());
        assertFalse(stringType.isOptionalOrNullable());
        assertFalse(integerType.isCsvQuoteRequired());
        assertTrue(stringType.isCsvQuoteRequired());
    }

    @Test
    public void testStringValueCompareAndHash() {
        assertEquals(0, stringType.compare("a", "a"));
        assertTrue(stringType.compare("a", "b") < 0);
        assertTrue(stringType.compare("b", "a") > 0);
        assertTrue(stringType.compare(null, "a") < 0);
        assertTrue(stringType.compare("a", null) > 0);
        assertEquals(0, stringType.compare(null, null));
        assertEquals("test", stringType.stringOf("test"));
        assertNull(stringType.stringOf(null));
        assertEquals("123", integerType.stringOf(123));
        assertEquals("test", stringType.valueOf("test"));
        assertNull(stringType.valueOf((String) null));
        assertEquals(123, integerType.valueOf("123"));
        assertEquals("test", stringType.valueOf((Object) "test"));
        assertNull(stringType.valueOf((Object) null));
        assertEquals(123, integerType.valueOf(123));
        char[] chars = "test".toCharArray();
        assertEquals("test", stringType.valueOf(chars, 0, 4));
        assertEquals("es", stringType.valueOf(chars, 1, 2));
        assertNull(stringType.valueOf(null, 0, 0));
        assertEquals("test".hashCode(), stringType.hashCode("test"));
        assertEquals(0, stringType.hashCode(null));
        assertEquals(Integer.valueOf(123).hashCode(), integerType.hashCode(123));
        assertEquals("test".hashCode(), stringType.deepHashCode("test"));
        assertEquals(0, stringType.deepHashCode(null));
        Type<int[]> arrayType = createType(int[].class);
        assertTrue(arrayType.deepHashCode(new int[] { 1, 2, 3 }) != 0);
        assertTrue(stringType.equals("test", "test"));
        assertFalse(stringType.equals("test1", "test2"));
        assertTrue(stringType.equals(null, null));
        assertFalse(stringType.equals("test", null));
        assertFalse(stringType.equals(null, "test"));
        assertTrue(stringType.deepEquals("test", "test"));
        assertFalse(stringType.deepEquals("test1", "test2"));
        assertTrue(arrayType.deepEquals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertFalse(arrayType.deepEquals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }));
        assertEquals("test", stringType.toString("test"));
        assertEquals("null", stringType.toString(null));
        assertEquals("123", integerType.toString(123));
        assertEquals("test", stringType.deepToString("test"));
        assertEquals("null", stringType.deepToString(null));
        String arrayStr = arrayType.deepToString(new int[] { 1, 2, 3 });
        assertTrue(arrayStr.contains("1") && arrayStr.contains("2") && arrayStr.contains("3"));
    }

    @Test
    public void testJdbcGetSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");
        when(rs.getString("column")).thenReturn("test");
        assertEquals("test", stringType.get(rs, 1));
        assertEquals("test", stringType.get(rs, "column"));
        verify(rs).getString(1);
        verify(rs).getString("column");

        PreparedStatement stmt = mock(PreparedStatement.class);
        stringType.set(stmt, 1, "test");
        stringType.set(stmt, 2, null);
        stringType.set(stmt, 3, "test", java.sql.Types.VARCHAR);
        verify(stmt).setString(1, "test");
        verify(stmt).setString(2, null);
        verify(stmt).setString(3, "test");

        CallableStatement callable = mock(CallableStatement.class);
        stringType.set(callable, "param", "test");
        stringType.set(callable, "param2", null);
        stringType.set(callable, "param3", "test", java.sql.Types.VARCHAR);
        verify(callable).setString("param", "test");
        verify(callable).setString("param2", null);
        verify(callable).setString("param3", "test");
    }

    @Test
    public void testAppendSerializeAndCollectionConversion() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringType.appendTo(sb, "test");
        assertEquals("test", sb.toString());
        sb = new StringBuilder();
        stringType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        assertDoesNotThrow(() -> {
            CharacterWriter writer = createCharacterWriter();
            stringType.serializeTo(writer, "test", null);
            stringType.serializeTo(writer, null, null);
            JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
            when(config.getStringQuotation()).thenReturn('"');
            stringType.serializeTo(writer, "test", config);
        });

        Type<String[]> arrayType = createType(String[].class);
        Collection<String> collection = Arrays.asList("a", "b", "c");
        String[] array = arrayType.collectionToArray(collection);
        assertEquals(3, array.length);
        assertThrows(UnsupportedOperationException.class, () -> stringType.collectionToArray(collection));
        Collection<String> fromArray = arrayType.arrayToCollection(new String[] { "a", "b", "c" }, ArrayList.class);
        assertEquals(3, fromArray.size());
        assertThrows(UnsupportedOperationException.class, () -> stringType.arrayToCollection("test", ArrayList.class));
        List<String> output = new ArrayList<>();
        arrayType.arrayToCollection(new String[] { "a", "b", "c" }, output);
        assertEquals(3, output.size());
        assertThrows(UnsupportedOperationException.class, () -> stringType.arrayToCollection("test", new ArrayList<>()));
    }

    @Test
    public void reviewFixes20260906_valueOfNullObjectYieldsThePrimitiveDefault() {
        // T1-08: valueOf(Object null) / valueOf(char[] null) delegate to valueOf((String) null)
        assertEquals(0, Type.of(int.class).valueOf((Object) null));
        assertEquals(0L, Type.of(long.class).valueOf((Object) null));
        assertEquals(false, Type.of(boolean.class).valueOf((Object) null));
        assertEquals(0, Type.of(int.class).valueOf((char[]) null, 0, 0));
        assertNull(Type.of(Integer.class).valueOf((Object) null));
        assertNull(Type.of(Integer.class).valueOf((char[]) null, 0, 0));
        assertNull(Type.of(String.class).valueOf((Object) null));
        assertNull(Type.of(String.class).valueOf((char[]) null, 0, 0));
    }

    @Test
    public void reviewFixes20260906_ofXxxHelpersRejectNullClassesWithIllegalArgumentException() {
        // T1-10: consistent with Type.of((Class) null)
        assertThrows(IllegalArgumentException.class, () -> Type.ofList(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofLinkedList(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofSet(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofLinkedHashSet(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofSortedSet(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofNavigableSet(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofTreeSet(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofQueue(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofDeque(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofArrayDeque(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofLinkedBlockingQueue(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofConcurrentLinkedQueue(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofPriorityQueue(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofMultiset(null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofListOfMap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofListOfMap(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofListOfLinkedHashMap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofSetOfMap(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofSetOfLinkedHashMap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofMap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofMap(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofLinkedHashMap(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofSortedMap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofNavigableMap(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofTreeMap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofConcurrentMap(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofConcurrentHashMap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofListMultimap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofListMultimap(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> Type.ofSetMultimap(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Type.ofSetMultimap(String.class, null));
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Type.ofList(null));
        assertTrue(error.getMessage().contains("eleClass"), error.getMessage());
        // the happy path is unchanged
        assertEquals(java.util.List.of("a"), Type.ofList(String.class).valueOf("[\"a\"]"));
        assertEquals(java.util.Map.of("k", 1), Type.ofMap(String.class, Integer.class).valueOf("{\"k\": 1}"));
    }

    @Test
    public void reviewFixes20260906_compareContractNullsFirstNaNAndNegativeZeroAndEqualsInconsistency() {
        // R-T06: the documented contract of Type.compare
        final Type<Double> doubleType = Type.of(Double.class);
        assertEquals(0, doubleType.compare(null, null));
        assertTrue(doubleType.compare(null, 1.0) < 0);
        assertTrue(doubleType.compare(1.0, null) > 0);
        assertEquals(0, doubleType.compare(Double.NaN, Double.NaN));
        assertTrue(doubleType.compare(Double.NaN, Double.POSITIVE_INFINITY) > 0);
        assertTrue(doubleType.compare(-0.0, 0.0) < 0);
        assertEquals(0, Type.of(double.class).compare(Double.NaN, Double.NaN));
        assertTrue(Type.of(double.class).compare(-0.0, 0.0) < 0);
        assertTrue(Type.of(Float.class).compare(-0.0f, 0.0f) < 0);
        assertEquals(0, Type.of(Float.class).compare(Float.NaN, Float.NaN));

        final Type<java.math.BigDecimal> bigDecimalType = Type.of(java.math.BigDecimal.class);
        assertEquals(0, bigDecimalType.compare(new java.math.BigDecimal("2.0"), new java.math.BigDecimal("2.00")));
        assertFalse(bigDecimalType.equals(new java.math.BigDecimal("2.0"), new java.math.BigDecimal("2.00")));

        final Type<StringBuilder> stringBuilderType = Type.of(StringBuilder.class);
        assertTrue(stringBuilderType.isComparable());
        assertEquals(0, stringBuilderType.compare(new StringBuilder("a"), new StringBuilder("a")));
        assertFalse(stringBuilderType.equals(new StringBuilder("a"), new StringBuilder("a")));
        final Type<StringBuffer> stringBufferType = Type.of(StringBuffer.class);
        assertEquals(0, stringBufferType.compare(new StringBuffer("a"), new StringBuffer("a")));
        assertFalse(stringBufferType.equals(new StringBuffer("a"), new StringBuffer("a")));

        assertThrows(UnsupportedOperationException.class, () -> Type.of(Object.class).compare(new Object(), new Object()));
    }
}
