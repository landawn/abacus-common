package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class TypeFactory100Test extends TestBase {

    @Test
    public void testGetTypeWithClass() {
        Type<String> type = TypeFactory.getType(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.clazz());
    }

    @Test
    public void testGetTypeWithClassNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.getType((Class<?>) null);
        });
    }

    @Test
    public void testGetTypeWithJavaLangReflectType() {
        java.lang.reflect.Type javaType = String.class;
        Type<String> type = TypeFactory.getType(javaType);
        assertNotNull(type);
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
    public void testGetTypeWithString() {
        Type<String> type = TypeFactory.getType("String");
        assertNotNull(type);
    }

    @Test
    public void testGetTypeWithStringNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            TypeFactory.getType((String) null);
        });
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
            public Class<CustomClass3> clazz() {
                return CustomClass3.class;
            }

            @Override
            public boolean isPrimitiveType() {
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
            public boolean isGenericType() {
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
            public boolean isObjectType() {
                return false;
            }

            @Override
            public boolean isOptionalOrNullable() {
                return false;
            }

            @Override
            public SerializationType getSerializationType() {
                return SerializationType.ENTITY;
            }

            @Override
            public Type<?> getElementType() {
                return null;
            }

            @Override
            public Type<?>[] getParameterTypes() {
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
            public void writeCharacter(com.landawn.abacus.util.CharacterWriter writer, CustomClass3 x,
                    com.landawn.abacus.parser.JsonXmlSerializationConfig<?> config) {
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
            public Class<Object> clazz() {
                return Object.class;
            }

            @Override
            public boolean isPrimitiveType() {
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
            public boolean isGenericType() {
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
            public boolean isObjectType() {
                return true;
            }

            @Override
            public boolean isOptionalOrNullable() {
                return false;
            }

            @Override
            public SerializationType getSerializationType() {
                return SerializationType.UNKNOWN;
            }

            @Override
            public Type<?> getElementType() {
                return null;
            }

            @Override
            public Type<?>[] getParameterTypes() {
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
            public void writeCharacter(com.landawn.abacus.util.CharacterWriter writer, Object x,
                    com.landawn.abacus.parser.JsonXmlSerializationConfig<?> config) {
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
}
