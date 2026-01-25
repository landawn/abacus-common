package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ClassUtil2025Test extends TestBase {

    @Test
    public void test_forName() {
        assertEquals(int.class, ClassUtil.forName("int"));
        assertEquals(boolean.class, ClassUtil.forName("boolean"));
        assertEquals(char.class, ClassUtil.forName("char"));
        assertEquals(byte.class, ClassUtil.forName("byte"));
        assertEquals(short.class, ClassUtil.forName("short"));
        assertEquals(long.class, ClassUtil.forName("long"));
        assertEquals(float.class, ClassUtil.forName("float"));
        assertEquals(double.class, ClassUtil.forName("double"));

        assertEquals(Integer.class, ClassUtil.forName("Integer"));
        assertEquals(String.class, ClassUtil.forName("String"));
        assertEquals(String.class, ClassUtil.forName("java.lang.String"));

        assertEquals(int[].class, ClassUtil.forName("int[]"));
        assertEquals(String[].class, ClassUtil.forName("String[]"));
        assertEquals(int[][].class, ClassUtil.forName("int[][]"));

        assertEquals(Map.Entry.class, ClassUtil.forName("java.util.Map.Entry"));

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.forName("invalid.class.Name"));
    }

    @Test
    public void test_getTypeName() {
        java.lang.reflect.Type type = String.class;
        String typeName = ClassUtil.getTypeName(type);
        assertNotNull(typeName);
        assertTrue(typeName.contains("String"));
    }

    @Test
    public void test_getCanonicalClassName() {
        assertEquals("java.lang.String", ClassUtil.getCanonicalClassName(String.class));
        assertEquals("int", ClassUtil.getCanonicalClassName(int.class));

        assertEquals("java.lang.String[]", ClassUtil.getCanonicalClassName(String[].class));
    }

    @Test
    public void test_getClassName() {
        assertEquals("java.lang.String", ClassUtil.getClassName(String.class));
        assertEquals("int", ClassUtil.getClassName(int.class));
        assertEquals("[Ljava.lang.String;", ClassUtil.getClassName(String[].class));
    }

    @Test
    public void test_getSimpleClassName() {
        assertEquals("String", ClassUtil.getSimpleClassName(String.class));
        assertEquals("Integer", ClassUtil.getSimpleClassName(Integer.class));
        assertEquals("int", ClassUtil.getSimpleClassName(int.class));
    }

    @Test
    public void test_getPackage() {
        Package pkg = ClassUtil.getPackage(String.class);
        assertNotNull(pkg);
        assertEquals("java.lang", pkg.getName());

        assertNull(ClassUtil.getPackage(int.class));
    }

    @Test
    public void test_getPackageName() {
        assertEquals("java.lang", ClassUtil.getPackageName(String.class));
        assertEquals("", ClassUtil.getPackageName(int.class));
    }

    @Test
    public void test_findClassesInPackage() {
        List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.util", false, true);
        assertNotNull(classes);
        assertTrue(classes.size() > 0);
        assertTrue(classes.stream().anyMatch(c -> c.getSimpleName().equals("ClassUtil")));

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.findClassesInPackage("invalid.package.name", false, false));
    }

    @Test
    public void test_findClassesInPackage_withPredicate() {
        List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.util", false, true, cls -> cls.getName().contains("ClassUtil"));
        assertNotNull(classes);
        assertTrue(classes.stream().anyMatch(c -> c.getSimpleName().equals("ClassUtil")));
    }

    @Test
    public void test_getAllInterfaces() {
        Set<Class<?>> interfaces = ClassUtil.getAllInterfaces(ArrayList.class);
        assertNotNull(interfaces);
        assertTrue(interfaces.contains(List.class));
        assertTrue(interfaces.contains(Collection.class));
        assertTrue(interfaces.contains(Iterable.class));
    }

    @Test
    public void test_getAllSuperclasses() {
        List<Class<?>> superclasses = ClassUtil.getAllSuperclasses(ArrayList.class);
        assertNotNull(superclasses);
        assertTrue(superclasses.size() > 0);
        assertFalse(superclasses.contains(Object.class));
    }

    @Test
    public void test_getAllSuperTypes() {
        Set<Class<?>> superTypes = ClassUtil.getAllSuperTypes(ArrayList.class);
        assertNotNull(superTypes);
        assertTrue(superTypes.contains(List.class));
        assertFalse(superTypes.contains(Object.class));
    }

    @Test
    public void test_getEnclosingClass() {
        Class<?> innerClass = Map.Entry.class;
        Class<?> enclosing = ClassUtil.getEnclosingClass(innerClass);
        assertEquals(Map.class, enclosing);

        assertNull(ClassUtil.getEnclosingClass(String.class));
    }

    @Test
    public void test_getDeclaredConstructor() {
        Constructor<String> ctor = ClassUtil.getDeclaredConstructor(String.class);
        assertNotNull(ctor);

        Constructor<String> ctor2 = ClassUtil.getDeclaredConstructor(String.class, char[].class);
        assertNotNull(ctor2);

        Constructor<String> ctor3 = ClassUtil.getDeclaredConstructor(String.class, int.class, int.class, int.class);
        assertNull(ctor3);
    }

    @Test
    public void test_getDeclaredMethod() {
        Method method = ClassUtil.getDeclaredMethod(String.class, "isEmpty");
        assertNotNull(method);

        Method method2 = ClassUtil.getDeclaredMethod(String.class, "substring", int.class, int.class);
        assertNotNull(method2);

        Method method3 = ClassUtil.getDeclaredMethod(String.class, "nonExistentMethod");
        assertNull(method3);
    }

    @Test
    public void test_getParameterizedTypeNameByField() throws Exception {
        class TestClass {
            List<String> names;
        }

        Field field = TestClass.class.getDeclaredField("names");
        String typeName = ClassUtil.getParameterizedTypeNameByField(field);
        assertNotNull(typeName);
    }

    @Test
    public void test_getParameterizedTypeNameByMethod() throws Exception {
        class TestClass {
            void setNames(List<String> names) {
            }

            List<Integer> getNumbers() {
                return null;
            }
        }

        Method setter = TestClass.class.getDeclaredMethod("setNames", List.class);
        String typeName = ClassUtil.getParameterizedTypeNameByMethod(setter);
        assertNotNull(typeName);

        Method getter = TestClass.class.getDeclaredMethod("getNumbers");
        String typeName2 = ClassUtil.getParameterizedTypeNameByMethod(getter);
        assertNotNull(typeName2);
    }

    @Test
    public void test_formatParameterizedTypeName() {
        String formatted = ClassUtil.formatParameterizedTypeName("java.lang.String");
        assertEquals("String", formatted);

        String formatted2 = ClassUtil.formatParameterizedTypeName("class [Ljava.lang.String;");
        assertEquals("String[]", formatted2);
    }

    @Test
    public void test_inheritanceDistance() {
        assertEquals(0, ClassUtil.inheritanceDistance(String.class, String.class));

        assertEquals(1, ClassUtil.inheritanceDistance(String.class, Object.class));

        int distance = ClassUtil.inheritanceDistance(ArrayList.class, Object.class);
        assertTrue(distance > 0);

        assertEquals(-1, ClassUtil.inheritanceDistance(String.class, Integer.class));

        assertEquals(-1, ClassUtil.inheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtil.inheritanceDistance(String.class, null));
    }

    @Test
    public void test_hierarchy() {
        ObjIterator<Class<?>> iter = ClassUtil.hierarchy(ArrayList.class);
        assertNotNull(iter);
        assertTrue(iter.hasNext());

        Class<?> first = iter.next();
        assertEquals(ArrayList.class, first);

        while (iter.hasNext()) {
            assertNotNull(iter.next());
        }
    }

    @Test
    public void test_hierarchy_withInterfaces() {
        ObjIterator<Class<?>> iter = ClassUtil.hierarchy(ArrayList.class, true);
        assertNotNull(iter);
        assertTrue(iter.hasNext());

        boolean foundInterface = false;
        while (iter.hasNext()) {
            Class<?> cls = iter.next();
            if (cls.isInterface()) {
                foundInterface = true;
                break;
            }
        }
        assertTrue(foundInterface);
    }

    @Test
    public void test_invokeConstructor() throws Exception {
        Constructor<String> ctor = String.class.getDeclaredConstructor(char[].class);
        String result = ClassUtil.invokeConstructor(ctor, new char[] { 'a', 'b', 'c' });
        assertEquals("abc", result);

        Constructor<File> fileCtor = File.class.getDeclaredConstructor(String.class);
        File file = ClassUtil.invokeConstructor(fileCtor, "/test/path");
        assertNotNull(file);
    }

    @Test
    public void test_invokeMethod_static() throws Exception {
        Method method = Integer.class.getDeclaredMethod("parseInt", String.class);
        int result = ClassUtil.invokeMethod(method, "123");
        assertEquals(123, result);
    }

    @Test
    public void test_invokeMethod_instance() throws Exception {
        Method method = String.class.getDeclaredMethod("substring", int.class, int.class);
        String result = ClassUtil.invokeMethod("Hello World", method, 0, 5);
        assertEquals("Hello", result);
    }

    @Test
    public void test_setAccessible() throws Exception {
        Field field = String.class.getDeclaredField("value");
        assertFalse(field.isAccessible());

        ClassUtil.setAccessible(field, true);
        assertTrue(field.isAccessible());

        ClassUtil.setAccessible(field, false);
        assertFalse(field.isAccessible());

        ClassUtil.setAccessible(null, true);
    }

    @Test
    public void test_setAccessibleQuietly() throws Exception {
        Field field = String.class.getDeclaredField("value");
        boolean result = ClassUtil.setAccessibleQuietly(field, true);
        assertTrue(result);
        assertTrue(field.isAccessible());

        assertFalse(ClassUtil.setAccessibleQuietly(null, true));

        assertTrue(ClassUtil.setAccessibleQuietly(field, true));
    }

    @Test
    public void test_isAnonymousClass() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
            }
        };
        assertTrue(ClassUtil.isAnonymousClass(r.getClass()));
        assertFalse(ClassUtil.isAnonymousClass(String.class));
    }

    @Test
    public void test_isMemberClass() {
        class Outer {
            class Inner {
            }
        }

        assertTrue(ClassUtil.isMemberClass(Outer.Inner.class));
        assertFalse(ClassUtil.isMemberClass(String.class));
    }

    @Test
    public void test_isAnonymousOrMemberClass() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
            }
        };
        assertTrue(ClassUtil.isAnonymousOrMemberClass(r.getClass()));
        assertFalse(ClassUtil.isAnonymousOrMemberClass(String.class));
    }

    @Test
    public void test_isPrimitiveType() {
        assertTrue(ClassUtil.isPrimitiveType(int.class));
        assertTrue(ClassUtil.isPrimitiveType(boolean.class));
        assertTrue(ClassUtil.isPrimitiveType(char.class));
        assertTrue(ClassUtil.isPrimitiveType(byte.class));
        assertTrue(ClassUtil.isPrimitiveType(short.class));
        assertTrue(ClassUtil.isPrimitiveType(long.class));
        assertTrue(ClassUtil.isPrimitiveType(float.class));
        assertTrue(ClassUtil.isPrimitiveType(double.class));

        assertFalse(ClassUtil.isPrimitiveType(Integer.class));
        assertFalse(ClassUtil.isPrimitiveType(String.class));

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.isPrimitiveType(null));
    }

    @Test
    public void test_isPrimitiveWrapper() {
        assertTrue(ClassUtil.isPrimitiveWrapper(Integer.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Boolean.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Character.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Byte.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Short.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Long.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Float.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Double.class));

        assertFalse(ClassUtil.isPrimitiveWrapper(int.class));
        assertFalse(ClassUtil.isPrimitiveWrapper(String.class));

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.isPrimitiveWrapper(null));
    }

    @Test
    public void test_isPrimitiveArrayType() {
        assertTrue(ClassUtil.isPrimitiveArrayType(int[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(boolean[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(char[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(byte[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(short[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(long[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(float[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(double[].class));

        assertFalse(ClassUtil.isPrimitiveArrayType(Integer[].class));
        assertFalse(ClassUtil.isPrimitiveArrayType(String[].class));
        assertFalse(ClassUtil.isPrimitiveArrayType(int.class));

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.isPrimitiveArrayType(null));
    }

    @Test
    public void test_wrap() {
        assertEquals(Integer.class, ClassUtil.wrap(int.class));
        assertEquals(Boolean.class, ClassUtil.wrap(boolean.class));
        assertEquals(Character.class, ClassUtil.wrap(char.class));
        assertEquals(Byte.class, ClassUtil.wrap(byte.class));
        assertEquals(Short.class, ClassUtil.wrap(short.class));
        assertEquals(Long.class, ClassUtil.wrap(long.class));
        assertEquals(Float.class, ClassUtil.wrap(float.class));
        assertEquals(Double.class, ClassUtil.wrap(double.class));

        assertEquals(Integer.class, ClassUtil.wrap(Integer.class));

        assertEquals(String.class, ClassUtil.wrap(String.class));

        assertEquals(Integer[].class, ClassUtil.wrap(int[].class));

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.wrap(null));
    }

    @Test
    public void test_unwrap() {
        assertEquals(int.class, ClassUtil.unwrap(Integer.class));
        assertEquals(boolean.class, ClassUtil.unwrap(Boolean.class));
        assertEquals(char.class, ClassUtil.unwrap(Character.class));
        assertEquals(byte.class, ClassUtil.unwrap(Byte.class));
        assertEquals(short.class, ClassUtil.unwrap(Short.class));
        assertEquals(long.class, ClassUtil.unwrap(Long.class));
        assertEquals(float.class, ClassUtil.unwrap(Float.class));
        assertEquals(double.class, ClassUtil.unwrap(Double.class));

        assertEquals(int.class, ClassUtil.unwrap(int.class));

        assertEquals(String.class, ClassUtil.unwrap(String.class));

        assertEquals(int[].class, ClassUtil.unwrap(Integer[].class));

        assertThrows(IllegalArgumentException.class, () -> ClassUtil.unwrap(null));
    }

    @Test
    public void test_createMethodHandle() throws Exception {
        Method method = String.class.getDeclaredMethod("isEmpty");
        assertNotNull(method);

        try {
            var handle = ClassUtil.createMethodHandle(method);
            assertNotNull(handle);
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void test_newNullSentinel() {
        Object nullMask = ClassUtil.newNullSentinel();
        assertNotNull(nullMask);

        Object nullMask2 = ClassUtil.newNullSentinel();
        assertNotEquals(nullMask, nullMask2);

        assertEquals("NULL", nullMask.toString());
    }

    @Test
    public void test_isBeanClass() {
        @SuppressWarnings("deprecation")
        boolean result = ClassUtil.isBeanClass(String.class);
        assertNotNull(result);
    }

    @Test
    public void test_isRecordClass() {
        @SuppressWarnings("deprecation")
        boolean result = ClassUtil.isRecordClass(String.class);
        assertFalse(result);
    }
}
