package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.util.N;

public class TypeFactoryTest extends TestBase {

    // ---- NEW TESTS targeting uncovered lines ----

    // Covers L550: typeName is null/empty so falls back to getClassName(cls)
    @Test
    public void testGetType_ReflectTypeForClass_FallsBackToClassName() {
        java.lang.reflect.Type t = Integer.class;
        Type<?> type = TypeFactory.getType(t);
        assertNotNull(type);
        assertEquals(Integer.class, type.javaType());
    }

    @Test
    public void testGetTypeWithClass() {
        Type<String> type = TypeFactory.getType(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.javaType());
    }

    @Test
    public void testGetTypeWithJavaLangReflectType() {
        java.lang.reflect.Type javaType = String.class;
        Type<String> type = TypeFactory.getType(javaType);
        assertNotNull(type);
    }

    @Test
    public void testGetTypeWithString() {
        Type<String> type = TypeFactory.getType("String");
        assertNotNull(type);
    }

    @Test
    public void testGetSheetTypeIsCached() {
        Type<?> type1 = TypeFactory.getType("Sheet<String, Integer, Long>");
        Type<?> type2 = TypeFactory.getType("Sheet<String, Integer, Long>");

        assertNotNull(type1);
        assertSame(type1, type2);
    }

    @Test
    public void testGetType_JsonAndXmlDefaultMap() {
        Type<?> jsonType = TypeFactory.getType("JSON");
        Type<?> xmlType = TypeFactory.getType("XML");

        assertNotNull(jsonType);
        assertNotNull(xmlType);
        assertTrue(jsonType.declaringName().startsWith("JSON"));
        assertTrue(xmlType.declaringName().startsWith("XML"));
    }

    @Test
    public void testGetType_IndexedAndTimedAliases() {
        Type<?> indexedType = TypeFactory.getType("Indexed");
        Type<?> timedType = TypeFactory.getType("Timed");

        assertNotNull(indexedType);
        assertNotNull(timedType);
        assertEquals("Indexed", indexedType.javaType().getSimpleName());
        assertEquals("Timed", timedType.javaType().getSimpleName());
    }

    // Covers java.sql.Date, Time, Timestamp, and java.util.Date branches
    @Test
    public void testGetType_SqlDateTypes() {
        assertNotNull(TypeFactory.getType(java.sql.Date.class));
        assertNotNull(TypeFactory.getType(java.sql.Time.class));
        assertNotNull(TypeFactory.getType(java.sql.Timestamp.class));
        assertNotNull(TypeFactory.getType(java.util.Date.class));
    }

    // Covers Calendar and XMLGregorianCalendar branches
    @Test
    public void testGetType_CalendarAndXmlGregorianCalendar() {
        assertNotNull(TypeFactory.getType(java.util.Calendar.class));
        assertNotNull(TypeFactory.getType(javax.xml.datatype.XMLGregorianCalendar.class));
    }

    // Covers Reader and InputStream branches
    @Test
    public void testGetType_ReaderAndInputStream() {
        assertNotNull(TypeFactory.getType(java.io.Reader.class));
        assertNotNull(TypeFactory.getType(java.io.InputStream.class));
    }

    // Covers ByteBuffer branch
    @Test
    public void testGetType_ByteBuffer() {
        assertNotNull(TypeFactory.getType(java.nio.ByteBuffer.class));
    }

    // Covers Enum branch
    @Test
    public void testGetType_Enum() {
        assertNotNull(TypeFactory.getType(java.time.DayOfWeek.class));
    }

    // Covers java.util.Optional branch
    @Test
    public void testGetType_JdkOptional() {
        assertNotNull(TypeFactory.getType(java.util.Optional.class));
        assertNotNull(TypeFactory.getType("Optional<String>"));
    }

    // Covers Multiset branch
    @Test
    public void testGetType_Multiset() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.Multiset.class));
        assertNotNull(TypeFactory.getType("Multiset<String>"));
    }

    // Covers ListMultimap branch
    @Test
    public void testGetType_ListMultimap() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.ListMultimap.class));
        assertNotNull(TypeFactory.getType("ListMultimap<String, Integer>"));
    }

    // Covers PasswordType branch
    @Test
    public void testGetType_Password() {
        assertNotNull(TypeFactory.getType("Password"));
    }

    // Covers getType(String, Class, Type) - Collection/List/Set branches
    @Test
    public void testGetType_CollectionWithTypeParam() {
        Type<?> listType = TypeFactory.getType("List<String>");
        assertNotNull(listType);
        assertTrue(listType.isList());

        Type<?> setType = TypeFactory.getType("Set<Integer>");
        assertNotNull(setType);
        assertTrue(setType.isSet());

        Type<?> collType = TypeFactory.getType("Collection<Long>");
        assertNotNull(collType);
        assertTrue(collType.isCollection());
    }

    @Test
    public void testGetType_MapWithTypeParams() {
        Type<?> mapType = TypeFactory.getType("Map<String, Integer>");
        assertNotNull(mapType);
        assertTrue(mapType.isMap());

        Type<?> treeMapType = TypeFactory.getType("TreeMap<String, Integer>");
        assertNotNull(treeMapType);
        assertTrue(treeMapType.isMap());
    }

    @Test
    public void testGetType_ImmutableCollections() {
        assertNotNull(TypeFactory.getType("ImmutableList<String>"));
        assertNotNull(TypeFactory.getType("ImmutableSet<String>"));
        assertNotNull(TypeFactory.getType("ImmutableMap<String, Integer>"));
    }

    @Test
    public void testGetType_PairAndTripleAndTuple() {
        assertNotNull(TypeFactory.getType("Pair<String, Integer>"));
        assertNotNull(TypeFactory.getType("Triple<String, Integer, Long>"));
        assertNotNull(TypeFactory.getType("Tuple1<String>"));
        assertNotNull(TypeFactory.getType("Tuple2<String, Integer>"));
        assertNotNull(TypeFactory.getType("Tuple3<String, Integer, Long>"));
    }

    @Test
    public void testGetType_SetMultimapAndMultimap() {
        assertNotNull(TypeFactory.getType("SetMultimap<String, Integer>"));
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.SetMultimap.class));
    }

    @Test
    public void testGetType_RangeType() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.Range.class));
        assertNotNull(TypeFactory.getType("Range<Integer>"));
    }

    @Test
    public void testGetType_OptionalAndNullable() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.u.Optional.class));
        assertNotNull(TypeFactory.getType("Optional<String>"));
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.u.Nullable.class));
        assertNotNull(TypeFactory.getType("Nullable<String>"));
    }

    @Test
    public void testGetType_NullTypeNameFallsBackToClass() {
        // When typeName is empty, it falls back to class-based resolution
        Type<String> type = TypeFactory.getType(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.javaType());
    }

    @Test
    public void testGetType_UnknownClassNameReturnsObjectType() {
        // When the class name is not recognized, should return an ObjectType
        Type<?> type = TypeFactory.getType("NonExistentType_XYZ_ABC_123");
        assertNotNull(type);
    }

    @Test
    public void testGetType_TypeType() {
        Type<?> typeType = TypeFactory.getType("Type<String>");
        assertNotNull(typeType);
    }

    @Test
    public void testGetType_JSONWithTypeParam() {
        Type<?> jsonType = TypeFactory.getType("JSON<List>");
        assertNotNull(jsonType);
    }

    @Test
    public void testGetType_XMLWithTypeParam() {
        Type<?> xmlType = TypeFactory.getType("XML<List>");
        assertNotNull(xmlType);
    }

    @Test
    public void testGetType_ClazzWithTypeParam() {
        Type<?> clazzType = TypeFactory.getType("Clazz<String>");
        assertNotNull(clazzType);
    }

    @Test
    public void testGetType_DatasetAndEntityId() {
        assertNotNull(TypeFactory.getType(com.landawn.abacus.util.Dataset.class));
    }

    @Test
    public void testGetType_PrimitiveTypes() {
        assertNotNull(TypeFactory.getType(int.class));
        assertNotNull(TypeFactory.getType(long.class));
        assertNotNull(TypeFactory.getType(double.class));
        assertNotNull(TypeFactory.getType(boolean.class));
        assertNotNull(TypeFactory.getType(byte.class));
        assertNotNull(TypeFactory.getType(char.class));
        assertNotNull(TypeFactory.getType(float.class));
        assertNotNull(TypeFactory.getType(short.class));
    }

    @Test
    public void testGetType_ArrayTypes() {
        assertNotNull(TypeFactory.getType(int[].class));
        assertNotNull(TypeFactory.getType(String[].class));
        assertNotNull(TypeFactory.getType(Object[].class));
    }

    // Covers L623-L624: Password with no parameters returns cached type
    @Test
    public void testGetType_PasswordWithNoParam_ReturnsCachedType() {
        Type<?> t1 = TypeFactory.getType("Password");
        Type<?> t2 = TypeFactory.getType("Password");
        assertNotNull(t1);
        assertSame(t1, t2);
    }

    // Covers L632-L636: java.sql.Date/Time/Timestamp subclass branches
    @Test
    public void testGetType_SqlDateSubclasses() {
        // These are the actual sql types which should map to DateType/TimeType/TimestampType
        Type<?> dateType = TypeFactory.getType(java.sql.Date.class);
        Type<?> timeType = TypeFactory.getType(java.sql.Time.class);
        Type<?> tsType = TypeFactory.getType(java.sql.Timestamp.class);
        assertNotNull(dateType);
        assertNotNull(timeType);
        assertNotNull(tsType);
        assertEquals("Date", dateType.name());
        assertEquals("Time", timeType.name());
        assertEquals("Timestamp", tsType.name());
    }

    // Covers L637: java.util.Date branch (not sql subtype)
    @Test
    public void testGetType_JavaUtilDateBranch() {
        Type<?> type = TypeFactory.getType(java.util.Date.class);
        assertNotNull(type);
        assertEquals("JUDate", type.name());
    }

    // Covers L643: XMLGregorianCalendar branch
    @Test
    public void testGetType_XMLGregorianCalendarBranch() {
        Type<?> type = TypeFactory.getType(javax.xml.datatype.XMLGregorianCalendar.class);
        assertNotNull(type);
    }

    // Covers L649: ByteBuffer branch via string name
    @Test
    public void testGetType_ByteBufferByName() {
        Type<?> type = TypeFactory.getType("java.nio.ByteBuffer");
        assertNotNull(type);
        assertEquals(java.nio.ByteBuffer.class, type.javaType());
    }

    // Covers L783-L784: HBaseColumn with no type parameters returns default
    @Test
    public void testGetType_HBaseColumnNoTypeParam() {
        Type<?> type = TypeFactory.getType(com.landawn.abacus.util.HBaseColumn.class);
        assertNotNull(type);
    }

    // Covers L896-L897: Pair with no type params
    @Test
    public void testGetType_PairNoTypeParam() {
        Type<?> type = TypeFactory.getType("Pair");
        assertNotNull(type);
    }

    // Covers L910-L911: Triple with no type params
    @Test
    public void testGetType_TripleNoTypeParam() {
        Type<?> type = TypeFactory.getType("Triple");
        assertNotNull(type);
    }

    // Covers L924-L925: Tuple1 with no type params
    @Test
    public void testGetType_Tuple1NoTypeParam() {
        Type<?> type = TypeFactory.getType("Tuple1");
        assertNotNull(type);
    }

    // Covers L968: Tuple4 with 4 type params
    @Test
    public void testGetType_Tuple4WithTypeParams() {
        Type<?> type = TypeFactory.getType("Tuple4<String, Integer, Long, Double>");
        assertNotNull(type);
    }

    // Covers L982: Tuple5 with 5 type params
    @Test
    public void testGetType_Tuple5WithTypeParams() {
        Type<?> type = TypeFactory.getType("Tuple5<String, Integer, Long, Double, Boolean>");
        assertNotNull(type);
    }

    // Covers L996: Tuple6 with 6 type params
    @Test
    public void testGetType_Tuple6WithTypeParams() {
        Type<?> type = TypeFactory.getType("Tuple6<String, Integer, Long, Double, Boolean, Byte>");
        assertNotNull(type);
    }

    // Covers L1011: Tuple7 with 7 type params
    @Test
    public void testGetType_Tuple7WithTypeParams() {
        Type<?> type = TypeFactory.getType("Tuple7<String, Integer, Long, Double, Boolean, Byte, Short>");
        assertNotNull(type);
    }

    // Covers L1027: Tuple8 with 8 type params
    @Test
    public void testGetType_Tuple8WithTypeParams() {
        Type<?> type = TypeFactory.getType("Tuple8<String, Integer, Long, Double, Boolean, Byte, Short, Float>");
        assertNotNull(type);
    }

    // Covers L1043: Tuple9 with 9 type params
    @Test
    public void testGetType_Tuple9WithTypeParams() {
        Type<?> type = TypeFactory.getType("Tuple9<String, Integer, Long, Double, Boolean, Byte, Short, Float, Character>");
        assertNotNull(type);
    }

    // Covers L1075-L1079, L1083-L1084: ImmutableMapEntry
    @Test
    public void testGetType_ImmutableMapEntryNoTypeParam() {
        Type<?> type = TypeFactory.getType("Map.ImmutableEntry");
        assertNotNull(type);
    }

    // Covers L1089-L1093, L1097-L1098: Map.Entry
    @Test
    public void testGetType_MapEntryNoTypeParam() {
        Type<?> type = TypeFactory.getType("Map.Entry");
        assertNotNull(type);
    }

    // Covers L1108-L1110: Blob and Clob branches
    @Test
    public void testGetType_BlobAndClob() {
        assertNotNull(TypeFactory.getType(java.sql.Blob.class));
        assertNotNull(TypeFactory.getType(java.sql.Clob.class));
    }

    // Covers Multimap no-param and with type params
    @Test
    public void testGetType_MultimapNoParam() {
        Type<?> type = TypeFactory.getType(com.landawn.abacus.util.Multimap.class);
        assertNotNull(type);
    }

    // Covers Sheet with 3 type params
    @Test
    public void testGetType_SheetWithThreeTypeParams() {
        Type<?> type = TypeFactory.getType("Sheet<String, Integer, Long>");
        assertNotNull(type);
    }

    // Covers getType(Class) returns not null for Number subclass
    @Test
    public void testGetType_NumberSubclass() {
        assertNotNull(TypeFactory.getType(java.math.BigDecimal.class));
        assertNotNull(TypeFactory.getType(java.math.BigInteger.class));
    }

    // Covers object array via string name
    @Test
    public void testGetType_ObjectArrayViaStringName() {
        Type<?> type = TypeFactory.getType("String[]");
        assertNotNull(type);
        assertTrue(type.isArray());
    }

    @Test
    public void testGetTypeWithClassNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.getType((Class<?>) null);
        });
    }

    @Test
    public void testGetTypeWithParameterizedType() throws NoSuchFieldException {
        class TypeHolder {
            List<String> list;
        }
        java.lang.reflect.Type paramType = TypeHolder.class.getDeclaredField("list").getGenericType();
        Type<?> type = TypeFactory.getType(paramType);
        assertNotNull(type);
    }

    @Test
    public void testGetTypeWithStringNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.getType((String) null);
        });
    }

    @Test
    public void testGetType_ClazzRejectsMultipleTypeParameters() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Clazz<String, Integer>"));
    }

    @Test
    public void testGetType_ClazzRejectsZeroTypeParameters() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Clazz"));
    }

    // Covers L616-L619: Password type with >0 type parameters should throw
    @Test
    public void testGetType_PasswordWithTypeParam_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Password<String>"));
    }

    // Covers L619: Password with >1 parameters should throw
    @Test
    public void testGetType_PasswordWithTwoParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Password(SHA256, MD5)"));
    }

    // Covers L656: Enum with >1 parameters should throw
    @Test
    public void testGetType_EnumWithTooManyParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("java.time.DayOfWeek(true, false)"));
    }

    // Covers L660-L663: JdkOptional with >1 type parameters should throw
    @Test
    public void testGetType_JdkOptionalWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("java.util.Optional<String, Integer>"));
    }

    // Covers L669-L672: com.landawn Optional with >1 type parameters should throw
    @Test
    public void testGetType_OptionalWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("com.landawn.abacus.util.u.Optional<String, Integer>"));
    }

    // Covers L678-L681: Nullable with >1 type parameters should throw
    @Test
    public void testGetType_NullableWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("com.landawn.abacus.util.u.Nullable<String, Integer>"));
    }

    // Covers L687-L691: Multiset with >1 type parameters / with extra params
    @Test
    public void testGetType_MultisetWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Multiset<String, Integer>"));
    }

    // Covers L702-L706: ListMultimap with wrong type param count
    @Test
    public void testGetType_ListMultimapWithOneTypeParam_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ListMultimap<String>"));
    }

    // Covers L716-L720: SetMultimap with wrong type param count
    @Test
    public void testGetType_SetMultimapWithOneTypeParam_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("SetMultimap<String>"));
    }

    // Covers L748-L751: Range with >1 type parameters
    @Test
    public void testGetType_RangeWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Range<String, Integer>"));
    }

    // Covers L776-L779: HBaseColumn with >1 type parameters
    @Test
    public void testGetType_HBaseColumnWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("com.landawn.abacus.util.HBaseColumn<String, Integer>"));
    }

    // Covers L789-L793: ImmutableList with >1 type parameters
    @Test
    public void testGetType_ImmutableListWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableList<String, Integer>"));
    }

    // Covers L803-L807: ImmutableSet with >1 type parameters
    @Test
    public void testGetType_ImmutableSetWithTwoTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableSet<String, Integer>"));
    }

    // Covers L862-L866: ImmutableMap wrong type param count
    @Test
    public void testGetType_ImmutableMapWithOneTypeParam_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableMap<String>"));
    }

    // Covers L889-L892: Map wrong type param count
    @Test
    public void testGetType_MapWithOneTypeParam_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Map<String>"));
    }

    // Covers L902-L906: Triple wrong type param count
    @Test
    public void testGetType_TripleWrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Triple<String, Integer>"));
    }

    // Covers L916-L920: Tuple1 wrong type param count
    @Test
    public void testGetType_Tuple1WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple1<String, Integer>"));
    }

    // Covers L930-L934: Tuple2 wrong type param count
    @Test
    public void testGetType_Tuple2WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple2<String>"));
    }

    // Covers L944-L948: Tuple3 wrong type param count
    @Test
    public void testGetType_Tuple3WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple3<String, Integer>"));
    }

    // Covers L958-L962: Tuple4 wrong type param count
    @Test
    public void testGetType_Tuple4WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple4<String, Integer, Long>"));
    }

    // Covers L972-L976: Tuple5 wrong type param count
    @Test
    public void testGetType_Tuple5WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple5<String, Integer, Long, Double>"));
    }

    // Covers L986-L990: Tuple6 wrong type param count
    @Test
    public void testGetType_Tuple6WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple6<String, Integer, Long, Double, Boolean>"));
    }

    // Covers L1000-L1004: Tuple7 wrong type param count
    @Test
    public void testGetType_Tuple7WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple7<String, Integer, Long, Double, Boolean, Byte>"));
    }

    // Covers L1016-L1020: Tuple8 wrong type param count
    @Test
    public void testGetType_Tuple8WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple8<String, Integer, Long, Double, Boolean, Byte, Short>"));
    }

    // Covers L1032-L1036: Tuple9 wrong type param count
    @Test
    public void testGetType_Tuple9WrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Tuple9<String, Integer, Long, Double, Boolean, Byte, Short, Float>"));
    }

    // Covers L1048-L1052: Indexed wrong type param count
    @Test
    public void testGetType_IndexedWrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Indexed<String, Integer>"));
    }

    // Covers L1062-L1065: Timed wrong type param count
    @Test
    public void testGetType_TimedWrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Timed<String, Integer>"));
    }

    // Covers L1075-L1079: ImmutableMapEntry wrong type param count
    @Test
    public void testGetType_ImmutableMapEntryWrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Map.ImmutableEntry<String>"));
    }

    // Covers L1089-L1093: Map.Entry wrong type param count
    @Test
    public void testGetType_MapEntryWrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Map.Entry<String>"));
    }

    // Covers L566: Clazz with extra parameters (not type params)
    @Test
    public void testGetType_ClazzWithExtraParameters_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Clazz<String>(extra)"));
    }

    // Covers L574-L577: JSON with extra parameters
    @Test
    public void testGetType_JSONWithExtraParameters_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("JSON<String>(extra)"));
    }

    // Covers L587-L590: XML with extra parameters
    @Test
    public void testGetType_XMLWithExtraParameters_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("XML<String>(extra)"));
    }

    // Covers L574: JSON with too many type parameters
    @Test
    public void testGetType_JSONWithTooManyTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("JSON<String, Integer>"));
    }

    // Covers L587: XML with too many type parameters
    @Test
    public void testGetType_XMLWithTooManyTypeParams_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("XML<String, Integer>"));
    }

    // Covers L732-L736: Multimap wrong type param count
    @Test
    public void testGetType_MultimapWrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("com.landawn.abacus.util.Multimap<String>"));
    }

    // Covers L765-L766: Sheet wrong type param count
    @Test
    public void testGetType_SheetWrongTypeParamCount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Sheet<String, Integer>"));
    }

    @Test
    public void testGetType_ClazzRejectsParameters() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Clazz<String>(foo)"));
    }

    @Test
    public void testRegisterTypeWithClassAndBiFunctions() {
        class CustomClass {
            String value = "test";
        }

        BiFunction<CustomClass, JsonParser, String> toStringFunc = (obj, parser) -> obj.value;
        BiFunction<String, JsonParser, CustomClass> fromStringFunc = (str, parser) -> {
            CustomClass obj = new CustomClass();
            obj.value = str;
            return obj;
        };

        TypeFactory.registerType(CustomClass.class, toStringFunc, fromStringFunc);
    }

    @Test
    public void testRegisterTypeWithClassAndFunctions() {
        class CustomClass2 {
            String value = "test";
        }

        Function<CustomClass2, String> toStringFunc = obj -> obj.value;
        Function<String, CustomClass2> fromStringFunc = str -> {
            CustomClass2 obj = new CustomClass2();
            obj.value = str;
            return obj;
        };

        TypeFactory.registerType(CustomClass2.class, toStringFunc, fromStringFunc);
    }

    @Test
    public void testRegisterTypeWithStringAndClassAndBiFunctions() {
        class CustomClass4 {
            String value = "test";
        }

        BiFunction<CustomClass4, JsonParser, String> toStringFunc = (obj, parser) -> obj.value;
        BiFunction<String, JsonParser, CustomClass4> fromStringFunc = (str, parser) -> {
            CustomClass4 obj = new CustomClass4();
            obj.value = str;
            return obj;
        };

        TypeFactory.registerType("CustomType4", CustomClass4.class, toStringFunc, fromStringFunc);
    }

    @Test
    public void testRegisterTypeWithStringAndClassAndFunctions() {
        class CustomClass5 {
            String value = "test";
        }

        Function<CustomClass5, String> toStringFunc = obj -> obj.value;
        Function<String, CustomClass5> fromStringFunc = str -> {
            CustomClass5 obj = new CustomClass5();
            obj.value = str;
            return obj;
        };

        TypeFactory.registerType("CustomType5", CustomClass5.class, toStringFunc, fromStringFunc);
    }

    // Covers L1383/L1388: registerType(Class, BiFunction, BiFunction) lambda body execution
    @Test
    public void testRegisterType_BiFunctionLambdaBodies() {
        class BiFuncClass {
            String val;

            BiFuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType(BiFuncClass.class, (obj, parser) -> obj.val, (str, parser) -> new BiFuncClass(str));
        Type<BiFuncClass> type = TypeFactory.getType(BiFuncClass.class);
        assertNotNull(type);
        BiFuncClass obj = new BiFuncClass("hello");
        assertEquals("hello", type.stringOf(obj));
        assertEquals("hello", type.valueOf("hello").val);
    }

    // Covers L1431/L1436: registerType(Class, Function, Function) lambda body execution
    @Test
    public void testRegisterType_FunctionLambdaBodies() {
        class FuncClass {
            String val;

            FuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType(FuncClass.class, obj -> obj.val, str -> new FuncClass(str));
        Type<FuncClass> type = TypeFactory.getType(FuncClass.class);
        assertNotNull(type);
        FuncClass obj = new FuncClass("world");
        assertEquals("world", type.stringOf(obj));
        assertEquals("world", type.valueOf("world").val);
    }

    // Covers L1523/L1528: registerType(String, Class, BiFunction, BiFunction) lambda body execution
    @Test
    public void testRegisterType_NamedBiFunctionLambdaBodies() {
        class NamedBiFuncClass {
            String val;

            NamedBiFuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType("NamedBiFuncClass_TypeTest", NamedBiFuncClass.class,
                (BiFunction<NamedBiFuncClass, JsonParser, String>) (obj, parser) -> obj.val,
                (BiFunction<String, JsonParser, NamedBiFuncClass>) (str, parser) -> new NamedBiFuncClass(str));
        Type<NamedBiFuncClass> type = TypeFactory.getType("NamedBiFuncClass_TypeTest");
        assertNotNull(type);
        NamedBiFuncClass obj = new NamedBiFuncClass("test");
        assertEquals("test", type.stringOf(obj));
        assertEquals("test", type.valueOf("test").val);
    }

    // Covers L1582/L1587: registerType(String, Class, Function, Function) lambda body execution
    @Test
    public void testRegisterType_NamedFunctionLambdaBodies() {
        class NamedFuncClass {
            String val;

            NamedFuncClass(String v) {
                this.val = v;
            }
        }
        TypeFactory.registerType("NamedFuncClass_TypeTest", NamedFuncClass.class, (Function<NamedFuncClass, String>) obj -> obj.val,
                (Function<String, NamedFuncClass>) str -> new NamedFuncClass(str));
        Type<NamedFuncClass> type = TypeFactory.getType("NamedFuncClass_TypeTest");
        assertNotNull(type);
        NamedFuncClass obj = new NamedFuncClass("test2");
        assertEquals("test2", type.stringOf(obj));
        assertEquals("test2", type.valueOf("test2").val);
    }

    @Test
    public void testRegisterTypeWithClassAndType() {
        class CustomClass3 {
        }
        Type<CustomClass3> customType = new AbstractType<>("CustomClass3") {
            @Override
            public String name() {
                return "CustomClass3";
            }

            @Override
            public String declaringName() {
                return "CustomClass3";
            }

            @Override
            public String xmlName() {
                return "CustomClass3";
            }

            @Override
            public Class<CustomClass3> javaType() {
                return CustomClass3.class;
            }

            @Override
            public boolean isPrimitive() {
                return false;
            }

            @Override
            public boolean isPrimitiveWrapper() {
                return false;
            }

            @Override
            public boolean isPrimitiveList() {
                return false;
            }

            @Override
            public boolean isBoolean() {
                return false;
            }

            @Override
            public boolean isNumber() {
                return false;
            }

            @Override
            public boolean isString() {
                return false;
            }

            @Override
            public boolean isCharSequence() {
                return false;
            }

            @Override
            public boolean isDate() {
                return false;
            }

            @Override
            public boolean isCalendar() {
                return false;
            }

            @Override
            public boolean isJodaDateTime() {
                return false;
            }

            @Override
            public boolean isPrimitiveArray() {
                return false;
            }

            @Override
            public boolean isPrimitiveByteArray() {
                return false;
            }

            @Override
            public boolean isObjectArray() {
                return false;
            }

            @Override
            public boolean isArray() {
                return false;
            }

            @Override
            public boolean isList() {
                return false;
            }

            @Override
            public boolean isSet() {
                return false;
            }

            @Override
            public boolean isCollection() {
                return false;
            }

            @Override
            public boolean isMap() {
                return false;
            }

            @Override
            public boolean isBean() {
                return false;
            }

            @Override
            public boolean isMapEntity() {
                return false;
            }

            @Override
            public boolean isEntityId() {
                return false;
            }

            @Override
            public boolean isDataset() {
                return false;
            }

            @Override
            public boolean isInputStream() {
                return false;
            }

            @Override
            public boolean isReader() {
                return false;
            }

            @Override
            public boolean isByteBuffer() {
                return false;
            }

            @Override
            public boolean isParameterizedType() {
                return false;
            }

            @Override
            public boolean isImmutable() {
                return true;
            }

            @Override
            public boolean isComparable() {
                return false;
            }

            @Override
            public boolean isSerializable() {
                return true;
            }

            @Override
            public boolean isObject() {
                return false;
            }

            @Override
            public boolean isOptionalOrNullable() {
                return false;
            }

            @Override
            public SerializationType serializationType() {
                return SerializationType.ENTITY;
            }

            @Override
            public Type<?> elementType() {
                return null;
            }

            @Override
            public Type<?>[] parameterTypes() {
                return new Type[0];
            }

            @Override
            public CustomClass3 defaultValue() {
                return null;
            }

            @Override
            public boolean isDefaultValue(CustomClass3 value) {
                return value == null;
            }

            @Override
            public int compare(CustomClass3 x, CustomClass3 y) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String stringOf(CustomClass3 x) {
                return x == null ? null : x.toString();
            }

            @Override
            public CustomClass3 valueOf(String str) {
                return null;
            }

            @Override
            public CustomClass3 valueOf(Object obj) {
                return (CustomClass3) obj;
            }

            @Override
            public CustomClass3 valueOf(char[] cbuf, int offset, int len) {
                return null;
            }

            @Override
            public CustomClass3 get(java.sql.ResultSet rs, int columnIndex) {
                return null;
            }

            @Override
            public CustomClass3 get(java.sql.ResultSet rs, String columnLabel) {
                return null;
            }

            @Override
            public void set(java.sql.PreparedStatement stmt, int columnIndex, CustomClass3 x) {
            }

            @Override
            public void set(java.sql.CallableStatement stmt, String parameterName, CustomClass3 x) {
            }

            @Override
            public void set(java.sql.PreparedStatement stmt, int columnIndex, CustomClass3 x, int sqlTypeOrLength) {
            }

            @Override
            public void set(java.sql.CallableStatement stmt, String parameterName, CustomClass3 x, int sqlTypeOrLength) {
            }

            @Override
            public void appendTo(Appendable appendable, CustomClass3 x) {
            }

            @Override
            public void writeCharacter(com.landawn.abacus.util.CharacterWriter writer, CustomClass3 x, com.landawn.abacus.parser.JsonXmlSerConfig<?> config) {
            }

            @Override
            public CustomClass3 collectionToArray(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <E> Collection<E> arrayToCollection(CustomClass3 x, Class<?> collClass) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <E> void arrayToCollection(CustomClass3 x, Collection<E> output) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int hashCode(CustomClass3 x) {
                return x == null ? 0 : x.hashCode();
            }

            @Override
            public int deepHashCode(CustomClass3 x) {
                return hashCode(x);
            }

            @Override
            public boolean equals(CustomClass3 x, CustomClass3 y) {
                return x == y || (x != null && x.equals(y));
            }

            @Override
            public boolean deepEquals(CustomClass3 x, CustomClass3 y) {
                return equals(x, y);
            }

            @Override
            public String toString(CustomClass3 x) {
                return stringOf(x);
            }

            @Override
            public String deepToString(CustomClass3 x) {
                return toString(x);
            }
        };

        TypeFactory.registerType(CustomClass3.class, customType);
    }

    @Test
    public void testRegisterType() {
        Type<?> customType = new AbstractType<>("UniqueTypeName456") {
            @Override
            public String name() {
                return "UniqueTypeName456";
            }

            @Override
            public String declaringName() {
                return "UniqueTypeName456";
            }

            @Override
            public String xmlName() {
                return "UniqueTypeName456";
            }

            @Override
            public Class<Object> javaType() {
                return Object.class;
            }

            @Override
            public boolean isPrimitive() {
                return false;
            }

            @Override
            public boolean isPrimitiveWrapper() {
                return false;
            }

            @Override
            public boolean isPrimitiveList() {
                return false;
            }

            @Override
            public boolean isBoolean() {
                return false;
            }

            @Override
            public boolean isNumber() {
                return false;
            }

            @Override
            public boolean isString() {
                return false;
            }

            @Override
            public boolean isCharSequence() {
                return false;
            }

            @Override
            public boolean isDate() {
                return false;
            }

            @Override
            public boolean isCalendar() {
                return false;
            }

            @Override
            public boolean isJodaDateTime() {
                return false;
            }

            @Override
            public boolean isPrimitiveArray() {
                return false;
            }

            @Override
            public boolean isPrimitiveByteArray() {
                return false;
            }

            @Override
            public boolean isObjectArray() {
                return false;
            }

            @Override
            public boolean isArray() {
                return false;
            }

            @Override
            public boolean isList() {
                return false;
            }

            @Override
            public boolean isSet() {
                return false;
            }

            @Override
            public boolean isCollection() {
                return false;
            }

            @Override
            public boolean isMap() {
                return false;
            }

            @Override
            public boolean isBean() {
                return false;
            }

            @Override
            public boolean isMapEntity() {
                return false;
            }

            @Override
            public boolean isEntityId() {
                return false;
            }

            @Override
            public boolean isDataset() {
                return false;
            }

            @Override
            public boolean isInputStream() {
                return false;
            }

            @Override
            public boolean isReader() {
                return false;
            }

            @Override
            public boolean isByteBuffer() {
                return false;
            }

            @Override
            public boolean isParameterizedType() {
                return false;
            }

            @Override
            public boolean isImmutable() {
                return false;
            }

            @Override
            public boolean isComparable() {
                return false;
            }

            @Override
            public boolean isSerializable() {
                return true;
            }

            @Override
            public boolean isObject() {
                return true;
            }

            @Override
            public boolean isOptionalOrNullable() {
                return false;
            }

            @Override
            public SerializationType serializationType() {
                return SerializationType.UNKNOWN;
            }

            @Override
            public Type<?> elementType() {
                return null;
            }

            @Override
            public Type<?>[] parameterTypes() {
                return new Type[0];
            }

            @Override
            public Object defaultValue() {
                return null;
            }

            @Override
            public boolean isDefaultValue(Object value) {
                return value == null;
            }

            @Override
            public int compare(Object x, Object y) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String stringOf(Object x) {
                return x == null ? null : x.toString();
            }

            @Override
            public Object valueOf(String str) {
                return str;
            }

            @Override
            public Object valueOf(Object obj) {
                return obj;
            }

            @Override
            public Object valueOf(char[] cbuf, int offset, int len) {
                N.checkFromIndexSize(offset, N.len(cbuf), len);

                if (N.isEmpty(cbuf) || len == 0) {
                    return Strings.EMPTY;
                }

                return new String(cbuf, offset, len);
            }

            @Override
            public Object get(java.sql.ResultSet rs, int columnIndex) {
                return null;
            }

            @Override
            public Object get(java.sql.ResultSet rs, String columnLabel) {
                return null;
            }

            @Override
            public void set(java.sql.PreparedStatement stmt, int columnIndex, Object x) {
            }

            @Override
            public void set(java.sql.CallableStatement stmt, String parameterName, Object x) {
            }

            @Override
            public void set(java.sql.PreparedStatement stmt, int columnIndex, Object x, int sqlTypeOrLength) {
            }

            @Override
            public void set(java.sql.CallableStatement stmt, String parameterName, Object x, int sqlTypeOrLength) {
            }

            @Override
            public void appendTo(Appendable appendable, Object x) {
            }

            @Override
            public void writeCharacter(com.landawn.abacus.util.CharacterWriter writer, Object x, com.landawn.abacus.parser.JsonXmlSerConfig<?> config) {
            }

            @Override
            public Object collectionToArray(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <E> Collection<E> arrayToCollection(Object x, Class<?> collClass) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <E> void arrayToCollection(Object x, Collection<E> output) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int hashCode(Object x) {
                return x == null ? 0 : x.hashCode();
            }

            @Override
            public int deepHashCode(Object x) {
                return hashCode(x);
            }

            @Override
            public boolean equals(Object x, Object y) {
                return x == y || (x != null && x.equals(y));
            }

            @Override
            public boolean deepEquals(Object x, Object y) {
                return equals(x, y);
            }

            @Override
            public String toString(Object x) {
                return stringOf(x);
            }

            @Override
            public String deepToString(Object x) {
                return toString(x);
            }
        };

        TypeFactory.registerType(customType);
    }

    // Covers L1472: registerType(Class, Type) throws when class already registered
    @Test
    public void testRegisterTypeWithClass_ThrowsIfAlreadyRegistered() {
        // String.class already has a registered type
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.registerType(String.class, TypeFactory.getType(String.class));
        });
    }

    // Covers L1627: registerType(String, Type) throws when name already registered
    @Test
    public void testRegisterTypeWithName_ThrowsIfAlreadyRegistered() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.registerType("String", TypeFactory.getType(String.class));
        });
    }
}
