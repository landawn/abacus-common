package com.landawn.abacus.type;

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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.TypeReference;

@Tag("new-test")
public class TypeInterface100Test extends TestBase {

    private Type<String> stringType;
    private Type<Integer> integerType;
    private Type<List<String>> listType;
    private Type<Map<String, Integer>> mapType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        stringType = createType(String.class);
        integerType = createType(Integer.class);
        listType = createType(new TypeReference<List<String>>() {
        });
        mapType = createType(new TypeReference<Map<String, Integer>>() {
        });

        characterWriter = createCharacterWriter();
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    @DisplayName("Test Type.of(java.lang.reflect.Type)")
    public void testOfReflectType() {
        Type<String> type = Type.of(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.clazz());
    }

    @Test
    @DisplayName("Test Type.of(TypeReference)")
    public void testOfTypeReference() {
        TypeReference<List<String>> typeRef = new TypeReference<>() {
        };
        Type<List<String>> type = Type.of(typeRef);
        assertNotNull(type);
    }

    @Test
    @DisplayName("Test Type.of(Class)")
    public void testOfClass() {
        Type<String> type = Type.of(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.clazz());
    }

    @Test
    @DisplayName("Test Type.of(String typeName)")
    public void testOfTypeName() {
        Type<String> type = Type.of("String");
        assertNotNull(type);

        Type<List> listType = Type.of("List<String>");
        assertNotNull(listType);
    }

    @Test
    @DisplayName("Test Type.ofAll(Class...)")
    public void testOfAllVarargs() {
        List<Type<Object>> types = Type.ofAll(String.class, Integer.class, Double.class);
        assertNotNull(types);
        assertEquals(3, types.size());
    }

    @Test
    @DisplayName("Test Type.ofAll(Collection<Class>)")
    public void testOfAllCollection() {
        Collection<Class<? extends Number>> classes = Arrays.asList(Integer.class, Double.class, Float.class);
        List<Type<Number>> types = Type.ofAll(classes);
        assertNotNull(types);
        assertEquals(3, types.size());
    }

    @Test
    @DisplayName("Test Type.ofList(Class)")
    public void testOfList() {
        Type<List<String>> type = Type.ofList(String.class);
        assertNotNull(type);
        assertTrue(type.isList());
    }

    @Test
    @DisplayName("Test Type.ofLinkedList(Class)")
    public void testOfLinkedList() {
        Type<LinkedList<String>> type = Type.ofLinkedList(String.class);
        assertNotNull(type);
        assertTrue(type.isList());
    }

    @Test
    @DisplayName("Test Type.ofListOfMap(Class, Class)")
    public void testOfListOfMap() {
        Type<List<Map<String, Integer>>> type = Type.ofListOfMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isList());
    }

    @Test
    @DisplayName("Test Type.ofListOfLinkedHashMap(Class, Class)")
    public void testOfListOfLinkedHashMap() {
        Type<List<Map<String, Integer>>> type = Type.ofListOfLinkedHashMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isList());
    }

    @Test
    @DisplayName("Test Type.ofSet(Class)")
    public void testOfSet() {
        Type<Set<String>> type = Type.ofSet(String.class);
        assertNotNull(type);
        assertTrue(type.isSet());
    }

    @Test
    @DisplayName("Test Type.ofSetOfMap(Class, Class)")
    public void testOfSetOfMap() {
        Type<Set<Map<String, Integer>>> type = Type.ofSetOfMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isSet());
    }

    @Test
    @DisplayName("Test Type.ofSetOfLinkedHashMap(Class, Class)")
    public void testOfSetOfLinkedHashMap() {
        Type<Set<Map<String, Integer>>> type = Type.ofSetOfLinkedHashMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isSet());
    }

    @Test
    @DisplayName("Test Type.ofLinkedHashSet(Class)")
    public void testOfLinkedHashSet() {
        Type<LinkedHashSet<String>> type = Type.ofLinkedHashSet(String.class);
        assertNotNull(type);
        assertTrue(type.isSet());
    }

    @Test
    @DisplayName("Test Type.ofSortedSet(Class)")
    public void testOfSortedSet() {
        Type<SortedSet<String>> type = Type.ofSortedSet(String.class);
        assertNotNull(type);
        assertTrue(type.isSet());
    }

    @Test
    @DisplayName("Test Type.ofNavigableSet(Class)")
    public void testOfNavigableSet() {
        Type<NavigableSet<String>> type = Type.ofNavigableSet(String.class);
        assertNotNull(type);
        assertTrue(type.isSet());
    }

    @Test
    @DisplayName("Test Type.ofTreeSet(Class)")
    public void testOfTreeSet() {
        Type<TreeSet<String>> type = Type.ofTreeSet(String.class);
        assertNotNull(type);
        assertTrue(type.isSet());
    }

    @Test
    @DisplayName("Test Type.ofQueue(Class)")
    public void testOfQueue() {
        Type<Queue<String>> type = Type.ofQueue(String.class);
        assertNotNull(type);
        assertTrue(type.isCollection());
    }

    @Test
    @DisplayName("Test Type.ofDeque(Class)")
    public void testOfDeque() {
        Type<Deque<String>> type = Type.ofDeque(String.class);
        assertNotNull(type);
        assertTrue(type.isCollection());
    }

    @Test
    @DisplayName("Test Type.ofArrayDeque(Class)")
    public void testOfArrayDeque() {
        Type<ArrayDeque<String>> type = Type.ofArrayDeque(String.class);
        assertNotNull(type);
        assertTrue(type.isCollection());
    }

    @Test
    @DisplayName("Test Type.ofLinkedBlockingQueue(Class)")
    public void testOfLinkedBlockingQueue() {
        Type<LinkedBlockingQueue<String>> type = Type.ofLinkedBlockingQueue(String.class);
        assertNotNull(type);
        assertTrue(type.isCollection());
    }

    @Test
    @DisplayName("Test Type.ofConcurrentLinkedQueue(Class)")
    public void testOfConcurrentLinkedQueue() {
        Type<ConcurrentLinkedQueue<String>> type = Type.ofConcurrentLinkedQueue(String.class);
        assertNotNull(type);
        assertTrue(type.isCollection());
    }

    @Test
    @DisplayName("Test Type.ofPriorityQueue(Class)")
    public void testOfPriorityQueue() {
        Type<PriorityQueue<String>> type = Type.ofPriorityQueue(String.class);
        assertNotNull(type);
        assertTrue(type.isCollection());
    }

    @Test
    @DisplayName("Test Type.ofPropsMap()")
    public void testOfPropsMap() {
        Type<Map<String, Object>> type = Type.ofPropsMap();
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofMap(Class, Class)")
    public void testOfMap() {
        Type<Map<String, Integer>> type = Type.ofMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofLinkedHashMap(Class, Class)")
    public void testOfLinkedHashMap() {
        Type<LinkedHashMap<String, Integer>> type = Type.ofLinkedHashMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofSortedMap(Class, Class)")
    public void testOfSortedMap() {
        Type<SortedMap<String, Integer>> type = Type.ofSortedMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofNavigableMap(Class, Class)")
    public void testOfNavigableMap() {
        Type<NavigableMap<String, Integer>> type = Type.ofNavigableMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofTreeMap(Class, Class)")
    public void testOfTreeMap() {
        Type<TreeMap<String, Integer>> type = Type.ofTreeMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofConcurrentMap(Class, Class)")
    public void testOfConcurrentMap() {
        Type<ConcurrentMap<String, Integer>> type = Type.ofConcurrentMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofConcurrentHashMap(Class, Class)")
    public void testOfConcurrentHashMap() {
        Type<ConcurrentHashMap<String, Integer>> type = Type.ofConcurrentHashMap(String.class, Integer.class);
        assertNotNull(type);
        assertTrue(type.isMap());
    }

    @Test
    @DisplayName("Test Type.ofMultiset(Class)")
    public void testOfMultiset() {
        Type<Multiset<String>> type = Type.ofMultiset(String.class);
        assertNotNull(type);
    }

    @Test
    @DisplayName("Test Type.ofListMultimap(Class, Class)")
    public void testOfListMultimap() {
        Type<ListMultimap<String, Integer>> type = Type.ofListMultimap(String.class, Integer.class);
        assertNotNull(type);
    }

    @Test
    @DisplayName("Test Type.ofSetMultimap(Class, Class)")
    public void testOfSetMultimap() {
        Type<SetMultimap<String, Integer>> type = Type.ofSetMultimap(String.class, Integer.class);
        assertNotNull(type);
    }

    @Test
    @DisplayName("Test name()")
    public void testName() {
        assertNotNull(stringType.name());
        assertTrue(stringType.name().length() > 0);
    }

    @Test
    @DisplayName("Test declaringName()")
    public void testDeclaringName() {
        assertNotNull(stringType.declaringName());
        assertTrue(stringType.declaringName().length() > 0);
    }

    @Test
    @DisplayName("Test xmlName()")
    public void testXmlName() {
        assertNotNull(stringType.xmlName());
        assertTrue(stringType.xmlName().length() > 0);
    }

    @Test
    @DisplayName("Test clazz()")
    public void testClazz() {
        assertEquals(String.class, stringType.clazz());
        assertEquals(Integer.class, integerType.clazz());
    }

    @Test
    @DisplayName("Test isPrimitiveType()")
    public void testIsPrimitiveType() {
        Type<Integer> intType = createType(int.class);
        assertTrue(intType.isPrimitiveType());
        assertFalse(stringType.isPrimitiveType());
    }

    @Test
    @DisplayName("Test isPrimitiveWrapper()")
    public void testIsPrimitiveWrapper() {
        assertTrue(integerType.isPrimitiveWrapper());
        assertFalse(stringType.isPrimitiveWrapper());
    }

    @Test
    @DisplayName("Test isPrimitiveList()")
    public void testIsPrimitiveList() {
        assertFalse(stringType.isPrimitiveList());
        assertFalse(listType.isPrimitiveList());
    }

    @Test
    @DisplayName("Test isBoolean()")
    public void testIsBoolean() {
        Type<Boolean> booleanType = createType(Boolean.class);
        assertTrue(booleanType.isBoolean());
        assertFalse(stringType.isBoolean());
    }

    @Test
    @DisplayName("Test isNumber()")
    public void testIsNumber() {
        assertTrue(integerType.isNumber());
        assertFalse(stringType.isNumber());
    }

    @Test
    @DisplayName("Test isString()")
    public void testIsString() {
        assertTrue(stringType.isString());
        assertFalse(integerType.isString());
    }

    @Test
    @DisplayName("Test isCharSequence()")
    public void testIsCharSequence() {
        assertTrue(stringType.isCharSequence());
        assertFalse(integerType.isCharSequence());
    }

    @Test
    @DisplayName("Test isDate()")
    public void testIsDate() {
        Type<Date> dateType = createType(Date.class);
        assertTrue(dateType.isDate());
        assertFalse(stringType.isDate());
    }

    @Test
    @DisplayName("Test isCalendar()")
    public void testIsCalendar() {
        Type<Calendar> calendarType = createType(Calendar.class);
        assertTrue(calendarType.isCalendar());
        assertFalse(stringType.isCalendar());
    }

    @Test
    @DisplayName("Test isJodaDateTime()")
    public void testIsJodaDateTime() {
        assertFalse(stringType.isJodaDateTime());
    }

    @Test
    @DisplayName("Test isPrimitiveArray()")
    public void testIsPrimitiveArray() {
        Type<int[]> intArrayType = createType(int[].class);
        assertTrue(intArrayType.isPrimitiveArray());
        assertFalse(stringType.isPrimitiveArray());
    }

    @Test
    @DisplayName("Test isPrimitiveByteArray()")
    public void testIsPrimitiveByteArray() {
        Type<byte[]> byteArrayType = createType(byte[].class);
        assertTrue(byteArrayType.isPrimitiveByteArray());
        assertFalse(stringType.isPrimitiveByteArray());
    }

    @Test
    @DisplayName("Test isObjectArray()")
    public void testIsObjectArray() {
        Type<String[]> stringArrayType = createType(String[].class);
        assertTrue(stringArrayType.isObjectArray());
        assertFalse(stringType.isObjectArray());
    }

    @Test
    @DisplayName("Test isArray()")
    public void testIsArray() {
        Type<String[]> stringArrayType = createType(String[].class);
        Type<int[]> intArrayType = createType(int[].class);
        assertTrue(stringArrayType.isArray());
        assertTrue(intArrayType.isArray());
        assertFalse(stringType.isArray());
    }

    @Test
    @DisplayName("Test isList()")
    public void testIsList() {
        assertTrue(listType.isList());
        assertFalse(stringType.isList());
    }

    @Test
    @DisplayName("Test isSet()")
    public void testIsSet() {
        Type<Set<String>> setType = createType(new TypeReference<Set<String>>() {
        });
        assertTrue(setType.isSet());
        assertFalse(stringType.isSet());
    }

    @Test
    @DisplayName("Test isCollection()")
    public void testIsCollection() {
        assertTrue(listType.isCollection());
        assertFalse(stringType.isCollection());
    }

    @Test
    @DisplayName("Test isMap()")
    public void testIsMap() {
        assertTrue(mapType.isMap());
        assertFalse(stringType.isMap());
    }

    @Test
    @DisplayName("Test isBean()")
    public void testIsBean() {
        assertFalse(stringType.isBean());
    }

    @Test
    @DisplayName("Test isMapEntity()")
    public void testIsMapEntity() {
        assertFalse(stringType.isMapEntity());
    }

    @Test
    @DisplayName("Test isEntityId()")
    public void testIsEntityId() {
        assertFalse(stringType.isEntityId());
    }

    @Test
    @DisplayName("Test isDataset()")
    public void testIsDataset() {
        assertFalse(stringType.isDataset());
    }

    @Test
    @DisplayName("Test isInputStream()")
    public void testIsInputStream() {
        assertFalse(stringType.isInputStream());
    }

    @Test
    @DisplayName("Test isReader()")
    public void testIsReader() {
        assertFalse(stringType.isReader());
    }

    @Test
    @DisplayName("Test isByteBuffer()")
    public void testIsByteBuffer() {
        assertFalse(stringType.isByteBuffer());
    }

    @Test
    @DisplayName("Test isGenericType()")
    public void testIsGenericType() {
        assertTrue(listType.isGenericType());
        assertFalse(stringType.isGenericType());
    }

    @Test
    @DisplayName("Test isImmutable()")
    public void testIsImmutable() {
        assertTrue(stringType.isImmutable());
        assertFalse(listType.isImmutable());
    }

    @Test
    @DisplayName("Test isComparable()")
    public void testIsComparable() {
        assertTrue(stringType.isComparable());
        assertTrue(integerType.isComparable());
    }

    @Test
    @DisplayName("Test isSerializable()")
    public void testIsSerializable() {
        assertTrue(stringType.isSerializable());
    }

    @Test
    @DisplayName("Test isObjectType()")
    public void testIsObjectType() {
        Type<Object> objectType = createType(Object.class);
        assertTrue(objectType.isObjectType());
        assertFalse(stringType.isObjectType());
    }

    @Test
    @DisplayName("Test isOptionalOrNullable()")
    public void testIsOptionalOrNullable() {
        Type<Optional<String>> optionalType = createType(new TypeReference<Optional<String>>() {
        });
        assertTrue(optionalType.isOptionalOrNullable());
        assertFalse(stringType.isOptionalOrNullable());
    }

    @Test
    @DisplayName("Test isNonQuotableCsvType()")
    public void testIsNonQuotableCsvType() {
        assertTrue(integerType.isNonQuotableCsvType());
        assertFalse(stringType.isNonQuotableCsvType());
    }

    @Test
    @DisplayName("Test getSerializationType()")
    public void testGetSerializationType() {
        assertNotNull(stringType.getSerializationType());
        assertEquals(Type.SerializationType.SERIALIZABLE, stringType.getSerializationType());
    }

    @Test
    @DisplayName("Test getElementType()")
    public void testGetElementType() {
        Type<?> elementType = listType.getElementType();
        assertNotNull(elementType);
        assertEquals(String.class, elementType.clazz());

        assertNull(stringType.getElementType());
    }

    @Test
    @DisplayName("Test getParameterTypes()")
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = mapType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);

        Type<?>[] stringParamTypes = stringType.getParameterTypes();
        assertNotNull(stringParamTypes);
        assertEquals(0, stringParamTypes.length);
    }

    @Test
    @DisplayName("Test defaultValue()")
    public void testDefaultValue() {
        assertNull(stringType.defaultValue());
        assertNull(integerType.defaultValue());
    }

    @Test
    @DisplayName("Test isDefaultValue()")
    public void testIsDefaultValue() {
        assertTrue(stringType.isDefaultValue(null));
        assertFalse(stringType.isDefaultValue("test"));

        assertFalse(integerType.isDefaultValue(0));
        assertFalse(integerType.isDefaultValue(1));
    }

    @Test
    @DisplayName("Test compare()")
    public void testCompare() {
        assertEquals(0, stringType.compare("a", "a"));
        assertTrue(stringType.compare("a", "b") < 0);
        assertTrue(stringType.compare("b", "a") > 0);
        assertTrue(stringType.compare(null, "a") < 0);
        assertTrue(stringType.compare("a", null) > 0);
        assertEquals(0, stringType.compare(null, null));
    }

    @Test
    @DisplayName("Test stringOf()")
    public void testStringOf() {
        assertEquals("test", stringType.stringOf("test"));
        assertEquals(null, stringType.stringOf(null));
        assertEquals("123", integerType.stringOf(123));
    }

    @Test
    @DisplayName("Test valueOf(String)")
    public void testValueOfString() {
        assertEquals("test", stringType.valueOf("test"));
        assertNull(stringType.valueOf((String) null));
        assertEquals(123, integerType.valueOf("123"));
    }

    @Test
    @DisplayName("Test valueOf(Object)")
    public void testValueOfObject() {
        assertEquals("test", stringType.valueOf((Object) "test"));
        assertNull(stringType.valueOf((Object) null));
        assertEquals(123, integerType.valueOf(123));
    }

    @Test
    @DisplayName("Test valueOf(char[], int, int)")
    public void testValueOfCharArray() {
        char[] chars = "test".toCharArray();
        assertEquals("test", stringType.valueOf(chars, 0, 4));
        assertEquals("es", stringType.valueOf(chars, 1, 2));
        assertNull(stringType.valueOf(null, 0, 0));
    }

    @Test
    @DisplayName("Test get(ResultSet, int)")
    public void testGetResultSetInt() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        assertEquals("test", stringType.get(rs, 1));
        verify(rs).getString(1);
    }

    @Test
    @DisplayName("Test get(ResultSet, String)")
    public void testGetResultSetString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        assertEquals("test", stringType.get(rs, "column"));
        verify(rs).getString("column");
    }

    @Test
    @DisplayName("Test set(PreparedStatement, int, T)")
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        stringType.set(stmt, 1, "test");
        verify(stmt).setString(1, "test");

        stringType.set(stmt, 2, null);
        verify(stmt).setString(2, null);
    }

    @Test
    @DisplayName("Test set(CallableStatement, String, T)")
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        stringType.set(stmt, "param", "test");
        verify(stmt).setString("param", "test");

        stringType.set(stmt, "param2", null);
        verify(stmt).setString("param2", null);
    }

    @Test
    @DisplayName("Test set(PreparedStatement, int, T, int)")
    public void testSetPreparedStatementWithSqlType() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        stringType.set(stmt, 1, "test", java.sql.Types.VARCHAR);
        verify(stmt).setString(1, "test");
    }

    @Test
    @DisplayName("Test set(CallableStatement, String, T, int)")
    public void testSetCallableStatementWithSqlType() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        stringType.set(stmt, "param", "test", java.sql.Types.VARCHAR);
        verify(stmt).setString("param", "test");
    }

    @Test
    @DisplayName("Test appendTo()")
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringType.appendTo(sb, "test");
        assertEquals("test", sb.toString());

        sb = new StringBuilder();
        stringType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    @DisplayName("Test writeCharacter()")
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = null;

        stringType.writeCharacter(writer, "test", config);

        stringType.writeCharacter(writer, null, config);

        config = mock(JsonXmlSerializationConfig.class);
        when(config.getStringQuotation()).thenReturn('"');
        stringType.writeCharacter(writer, "test", config);
    }

    @Test
    @DisplayName("Test collectionToArray()")
    public void testCollection2Array() {
        Type<String[]> arrayType = createType(String[].class);
        Collection<String> collection = Arrays.asList("a", "b", "c");
        String[] array = arrayType.collectionToArray(collection);
        assertNotNull(array);
        assertEquals(3, array.length);

        assertThrows(UnsupportedOperationException.class, () -> {
            stringType.collectionToArray(collection);
        });
    }

    @Test
    @DisplayName("Test arrayToCollection(T, Class)")
    public void testArray2CollectionWithClass() {
        Type<String[]> arrayType = createType(String[].class);
        String[] array = { "a", "b", "c" };
        Collection<String> collection = arrayType.arrayToCollection(array, ArrayList.class);
        assertNotNull(collection);
        assertEquals(3, collection.size());

        assertThrows(UnsupportedOperationException.class, () -> {
            stringType.arrayToCollection("test", ArrayList.class);
        });
    }

    @Test
    @DisplayName("Test arrayToCollection(T, Collection)")
    public void testArray2CollectionWithOutput() {
        Type<String[]> arrayType = createType(String[].class);
        String[] array = { "a", "b", "c" };
        List<String> output = new ArrayList<>();
        arrayType.arrayToCollection(array, output);
        assertEquals(3, output.size());

        assertThrows(UnsupportedOperationException.class, () -> {
            List<String> out = new ArrayList<>();
            stringType.arrayToCollection("test", out);
        });
    }

    @Test
    @DisplayName("Test hashCode(T)")
    public void testHashCodeValue() {
        assertEquals("test".hashCode(), stringType.hashCode("test"));
        assertEquals(0, stringType.hashCode(null));
        assertEquals(Integer.valueOf(123).hashCode(), integerType.hashCode(123));
    }

    @Test
    @DisplayName("Test deepHashCode(T)")
    public void testDeepHashCode() {
        assertEquals("test".hashCode(), stringType.deepHashCode("test"));
        assertEquals(0, stringType.deepHashCode(null));

        Type<int[]> arrayType = createType(int[].class);
        int[] array = { 1, 2, 3 };
        assertTrue(arrayType.deepHashCode(array) != 0);
    }

    @Test
    @DisplayName("Test equals(T, T)")
    public void testEqualsValues() {
        assertTrue(stringType.equals("test", "test"));
        assertFalse(stringType.equals("test1", "test2"));
        assertTrue(stringType.equals(null, null));
        assertFalse(stringType.equals("test", null));
        assertFalse(stringType.equals(null, "test"));
    }

    @Test
    @DisplayName("Test deepEquals(T, T)")
    public void testDeepEquals() {
        assertTrue(stringType.deepEquals("test", "test"));
        assertFalse(stringType.deepEquals("test1", "test2"));

        Type<int[]> arrayType = createType(int[].class);
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };
        int[] array3 = { 1, 2, 4 };
        assertTrue(arrayType.deepEquals(array1, array2));
        assertFalse(arrayType.deepEquals(array1, array3));
    }

    @Test
    @DisplayName("Test toString(T)")
    public void testToStringValue() {
        assertEquals("test", stringType.toString("test"));
        assertEquals("null", stringType.toString(null));
        assertEquals("123", integerType.toString(123));
    }

    @Test
    @DisplayName("Test deepToString(T)")
    public void testDeepToString() {
        assertEquals("test", stringType.deepToString("test"));
        assertEquals("null", stringType.deepToString(null));

        Type<int[]> arrayType = createType(int[].class);
        int[] array = { 1, 2, 3 };
        String str = arrayType.deepToString(array);
        assertNotNull(str);
        assertTrue(str.contains("1") && str.contains("2") && str.contains("3"));
    }
}
