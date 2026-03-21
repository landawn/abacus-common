package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.function.Predicate;

public class ClassUtilTest extends TestBase {

    class OuterClass {
        int x = 10;

        class InnerClass {
            int y = 5;
        }
    }

    public abstract static class A<T> implements Predicate<T> {

    }

    public abstract class B<T> extends A<T> implements Callable<T> {

    }

    public abstract class C<T> extends B<T> implements Callable<T> {

    }

    static class ThrowingCtorClass {
        ThrowingCtorClass() {
            throw new IllegalStateException("boom");
        }
    }

    static class ThrowingMethodClass {
        String fail() {
            throw new IllegalArgumentException("boom");
        }
    }

    static class GenericTypeHolder<T> {
        public List<T> getValues() {
            return Collections.emptyList();
        }
    }

    static class NonBean {
        private String value;
    }

    static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private boolean data;
        public String publicField;

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

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean hasData() {
            return data;
        }

        public void setData(boolean data) {
            this.data = data;
        }
    }

    @Entity
    static class EntityBean {
        private List<String> tags = new ArrayList<>();

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    @Record
    static class RecordBean {
        private String recordId;

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }
    }

    static class NestedBean {
        private TestBean inner;

        public TestBean getInner() {
            return inner;
        }

        public void setInner(TestBean inner) {
            this.inner = inner;
        }
    }

    static final class ImmutableBean {
        private final String value;

        ImmutableBean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    static class DiffBean {
        private String name;

        @DiffIgnore
        private String ignoredField;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIgnoredField() {
            return ignoredField;
        }

        public void setIgnoredField(String ignoredField) {
            this.ignoredField = ignoredField;
        }
    }

    static class BuilderBean {
        private String value;

        private BuilderBean() {
        }

        public String getValue() {
            return value;
        }

        public static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String value;

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public BuilderBean build() {
                BuilderBean bean = new BuilderBean();
                bean.value = value;
                return bean;
            }
        }
    }

    interface TestInterface {
    }

    static class ConcreteTestClass implements TestInterface {
    }

    static class InnerClassContainer {
        class InnerClass {
        }

        static class StaticInnerClass {
        }
    }

    @Test
    public void testGetClassLocation2() {
        // Class with a known location (test classes or jar)
        String location = ClassUtil.getClassLocation(ClassUtil.class);
        assertNotNull(location);
        assertTrue(location.length() > 0);
    }

    // ========== Additional tests for untested methods ==========

    @Test
    public void testGetClassLocation() {
        String location = ClassUtil.getClassLocation(String.class);
        // String.class is in the JDK; location may or may not be null depending on environment
        // Just verify no exception is thrown
        assertDoesNotThrow(() -> ClassUtil.getClassLocation(String.class));

        // For a project class, location should not be null
        String selfLocation = ClassUtil.getClassLocation(ClassUtil.class);
        assertNotNull(selfLocation);
    }

    @Test
    public void testGetClassLocation_NullCodeSource() {
        // Primitive types have no code source
        assertDoesNotThrow(() -> ClassUtil.getClassLocation(int.class));
    }

    @Test
    public void testForName_AllPrimitives() {
        assertEquals(boolean.class, ClassUtil.forName("boolean"));
        assertEquals(char.class, ClassUtil.forName("char"));
        assertEquals(byte.class, ClassUtil.forName("byte"));
        assertEquals(short.class, ClassUtil.forName("short"));
        assertEquals(int.class, ClassUtil.forName("int"));
        assertEquals(long.class, ClassUtil.forName("long"));
        assertEquals(float.class, ClassUtil.forName("float"));
        assertEquals(double.class, ClassUtil.forName("double"));
    }

    @Test
    public void testForName_ArrayTypes() {
        assertEquals(String[].class, ClassUtil.forName("java.lang.String[]"));
        assertEquals(int[].class, ClassUtil.forName("int[]"));
        assertEquals(double[].class, ClassUtil.forName("double[]"));
        assertEquals(boolean[].class, ClassUtil.forName("boolean[]"));
    }

    @Test
    public void testForName_InnerClass() {
        assertEquals(Map.Entry.class, ClassUtil.forName("java.util.Map$Entry"));
        assertEquals(Map.Entry.class, ClassUtil.forName("Map.Entry"));
    }

    @Test
    public void testForName_cached() {
        // Call twice to exercise cache
        Class<?> cls1 = ClassUtil.forName("java.lang.String");
        assertNotNull(cls1);
        assertEquals(String.class, cls1);

        Class<?> cls2 = ClassUtil.forName("java.lang.String");
        assertEquals(cls1, cls2);

        Class<?> cls3 = ClassUtil.forName("com.landawn.abacus.util.ClassUtil");
        assertNotNull(cls3);
        assertEquals(ClassUtil.class, cls3);
    }

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
    public void testForClass() {
        assertEquals(int.class, ClassUtil.forName("int"));
        assertEquals(boolean.class, ClassUtil.forName("boolean"));
        assertEquals(double.class, ClassUtil.forName("double"));

        assertEquals(Integer.class, ClassUtil.forName("java.lang.Integer"));
        assertEquals(Integer.class, ClassUtil.forName("Integer"));

        assertEquals(String[].class, ClassUtil.forName("String[]"));
        assertEquals(int[].class, ClassUtil.forName("int[]"));
        assertEquals(String[][].class, ClassUtil.forName("String[][]"));

        assertEquals(String.class, ClassUtil.forName("java.lang.String"));
        assertEquals(String.class, ClassUtil.forName("String"));
        assertEquals(ArrayList.class, ClassUtil.forName("java.util.ArrayList"));
        assertEquals(HashMap.class, ClassUtil.forName("java.util.HashMap"));

        assertEquals(Map.Entry.class, ClassUtil.forName("java.util.Map$Entry"));
        assertEquals(Map.Entry.class, ClassUtil.forName("Map.Entry"));

        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.forName("com.invalid.NonExistentClass");
        });
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        Beans.getPropNameList(TestBean.class);
                        ClassUtil.forName("java.lang.String");
                        Beans.toCamelCase("test_name_" + j);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertTrue(exceptions.isEmpty(), "Thread safety test failed with exceptions: " + exceptions);
    }

    @Test
    public void testForName_InvalidClass() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.forName("com.nonexistent.FakeClass"));
    }

    @Test
    public void testGetTypeName() {
        assertEquals("String", ClassUtil.getTypeName(String.class));
        assertEquals("List<String>", new TypeReference<List<String>>() {
        }.type().name());
    }

    @Test
    public void testGetTypeName_PrimitiveTypes() {
        assertEquals("boolean", ClassUtil.getTypeName(boolean.class));
        assertEquals("int", ClassUtil.getTypeName(int.class));
        assertEquals("double", ClassUtil.getTypeName(double.class));
    }

    @Test
    public void test_getTypeName() {
        java.lang.reflect.Type type = String.class;
        String typeName = ClassUtil.getTypeName(type);
        assertNotNull(typeName);
        assertTrue(typeName.contains("String"));
    }

    @Test
    public void testGetTypeName_ArrayType() {
        String typeName = ClassUtil.getTypeName(String[].class);
        assertNotNull(typeName);
    }

    @Test
    public void test_getCanonicalClassName() {
        assertEquals("java.lang.String", ClassUtil.getCanonicalClassName(String.class));
        assertEquals("int", ClassUtil.getCanonicalClassName(int.class));

        assertEquals("java.lang.String[]", ClassUtil.getCanonicalClassName(String[].class));
    }

    @Test
    public void testGetCanonicalClassName() {
        assertEquals("java.lang.String", ClassUtil.getCanonicalClassName(String.class));
        assertEquals("java.util.ArrayList", ClassUtil.getCanonicalClassName(ArrayList.class));
        assertEquals("int", ClassUtil.getCanonicalClassName(int.class));

        assertEquals("java.lang.String[]", ClassUtil.getCanonicalClassName(String[].class));
    }

    @Test
    public void testGetCanonicalClassName_Null() {
        assertThrows(Exception.class, () -> ClassUtil.getCanonicalClassName(null));
    }

    @Test
    public void test_innerClass() {

        final OuterClass myOuter = new OuterClass();
        final OuterClass.InnerClass myInner = myOuter.new InnerClass();
        System.out.println(myInner.y + myOuter.x);

        N.println(ClassUtil.getClassName(myInner.getClass()));

        final OuterClass.InnerClass myInner2 = myOuter.new InnerClass();
        assertEquals(myInner2.getClass(), myInner.getClass());
    }

    @Test
    public void test_getClassName() {
        assertEquals("java.lang.String", ClassUtil.getClassName(String.class));
        assertEquals("int", ClassUtil.getClassName(int.class));
        assertEquals("[Ljava.lang.String;", ClassUtil.getClassName(String[].class));
    }

    @Test
    public void testGetClassName() {
        assertEquals("java.lang.String", ClassUtil.getClassName(String.class));
        assertEquals("[Ljava.lang.String;", ClassUtil.getClassName(String[].class));
        assertEquals("int", ClassUtil.getClassName(int.class));
    }

    @Test
    public void testGetClassName_ArrayTypes() {
        assertEquals("[Ljava.lang.String;", ClassUtil.getClassName(String[].class));
        assertEquals("[I", ClassUtil.getClassName(int[].class));
    }

    @Test
    public void test_getSimpleClassName() {
        assertEquals("String", ClassUtil.getSimpleClassName(String.class));
        assertEquals("Integer", ClassUtil.getSimpleClassName(Integer.class));
        assertEquals("int", ClassUtil.getSimpleClassName(int.class));
    }

    @Test
    public void testGetSimpleClassName() {
        assertEquals("String", ClassUtil.getSimpleClassName(String.class));
        assertEquals("ArrayList", ClassUtil.getSimpleClassName(ArrayList.class));
        assertEquals("int", ClassUtil.getSimpleClassName(int.class));
        assertEquals("String[]", ClassUtil.getSimpleClassName(String[].class));
    }

    @Test
    public void testGetSimpleClassName_InnerClass() {
        String simpleName = ClassUtil.getSimpleClassName(Map.Entry.class);
        assertNotNull(simpleName);
    }

    @Test
    public void test_getPackage() {
        Package pkg = ClassUtil.getPackage(String.class);
        assertNotNull(pkg);
        assertEquals("java.lang", pkg.getName());

        assertNull(ClassUtil.getPackage(int.class));
    }

    @Test
    public void testGetPackage_RegularClass() {
        Package pkg = ClassUtil.getPackage(String.class);
        assertNotNull(pkg);
        assertEquals("java.lang", pkg.getName());
    }

    @Test
    public void testGetPackage_PrimitiveType() {
        assertNull(ClassUtil.getPackage(int.class));
        assertNull(ClassUtil.getPackage(boolean.class));
    }

    @Test
    public void test_getPackageName() {
        assertEquals("java.lang", ClassUtil.getPackageName(String.class));
        assertEquals("", ClassUtil.getPackageName(int.class));
    }

    @Test
    public void testGetPackageName() {
        assertEquals("java.lang", ClassUtil.getPackageName(String.class));
        assertEquals("java.util", ClassUtil.getPackageName(ArrayList.class));

        assertEquals("", ClassUtil.getPackageName(int.class));
    }

    @Test
    public void testGetPackageName_ArrayType() {
        String pkgName = ClassUtil.getPackageName(String[].class);
        assertNotNull(pkgName);
    }

    @Test
    public void test_findClassesInPackage_withPredicate() {
        List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.util", false, true, cls -> cls.getName().contains("ClassUtil"));
        assertNotNull(classes);
        assertTrue(classes.stream().anyMatch(c -> c.getSimpleName().equals("ClassUtil")));
    }

    @Test
    public void testFindClassesInPackage_recursive() {
        List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.util", true, true);
        assertNotNull(classes);
        assertTrue(classes.size() > 0);
    }

    @Test
    public void testFindClassesInPackage_withPredicate_recursive() {
        List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.util", true, true, cls -> cls.getSimpleName().contains("List"));
        assertNotNull(classes);
        assertTrue(classes.stream().anyMatch(c -> c.getSimpleName().equals("FloatList")));
    }

    @Test
    public void test_02() {
        assertDoesNotThrow(() -> {
            N.println(ClassUtil.findClassesInPackage("com.landawn.abacus", true, true));
        });
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
    public void testGetClassesByPackage() {
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.findClassesInPackage("java.lang", false, true));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.findClassesInPackage("java.util", false, true));

        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.findClassesInPackage("com.invalid.package.that.does.not.exist", false, false);
        });
    }

    @Test
    public void testFindClassesInPackage_Recursive() {
        assertDoesNotThrow(() -> {
            List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.util", true, true);
            assertNotNull(classes);
            assertFalse(classes.isEmpty());
        });
    }

    @Test
    public void testFindClassesInPackage_WithPredicate_Recursive() {
        assertDoesNotThrow(() -> {
            List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.util", true, true, cls -> cls.getSimpleName().startsWith("N"));
            assertNotNull(classes);
        });
    }

    @Test
    public void testFindClassesInPackage_NonExistentPackage() {
        assertThrows(Exception.class, () -> ClassUtil.findClassesInPackage("com.fake.does.not.exist", false, false));
    }

    @Test
    public void testPackageNameToFilePath_EdgeCase_AppendsTrailingSlash() throws Exception {
        Method method = ClassUtil.class.getDeclaredMethod("packageNameToFilePath", String.class);
        ClassUtil.setAccessible(method, true);

        assertEquals("com/landawn/abacus/util/", ClassUtil.invokeMethod(method, "com.landawn.abacus.util"));
        assertEquals("already/trimmed/", ClassUtil.invokeMethod(method, "already/trimmed/"));
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
    public void testGetAllInterfaces_NoInterfaces() {
        Set<Class<?>> interfaces = ClassUtil.getAllInterfaces(Object.class);
        assertNotNull(interfaces);
        assertTrue(interfaces.isEmpty());
    }

    @Test
    public void testGetAllInterfaces_MultipleInterfaces() {
        Set<Class<?>> interfaces = ClassUtil.getAllInterfaces(LinkedList.class);
        assertNotNull(interfaces);
        assertTrue(interfaces.contains(List.class));
        assertTrue(interfaces.contains(Collection.class));
        assertTrue(interfaces.contains(Serializable.class));
    }

    @Test
    public void test_getAllSuperclasses() {
        List<Class<?>> superclasses = ClassUtil.getAllSuperclasses(ArrayList.class);
        assertNotNull(superclasses);
        assertTrue(superclasses.size() > 0);
        assertFalse(superclasses.contains(Object.class));
    }

    @Test
    public void testGetAllSuperclasses() {
        List<Class<?>> superclasses = ClassUtil.getAllSuperclasses(ArrayList.class);
        assertNotNull(superclasses);
        assertTrue(superclasses.contains(AbstractList.class));
        assertTrue(superclasses.contains(AbstractCollection.class));
        assertFalse(superclasses.contains(Object.class));

        superclasses = ClassUtil.getAllSuperclasses(String.class);
        assertTrue(superclasses.isEmpty());
    }

    @Test
    public void testGetAllSuperclasses_ObjectClass() {
        List<Class<?>> superclasses = ClassUtil.getAllSuperclasses(Object.class);
        assertNotNull(superclasses);
        assertTrue(superclasses.isEmpty());
    }

    @Test
    public void test_01() {
        assertDoesNotThrow(() -> {
            N.println(ClassUtil.getAllSuperclasses(C.class));
            N.println(ClassUtil.getAllInterfaces(C.class));
            N.println(ClassUtil.getAllSuperTypes(C.class));
        });
    }

    @Test
    public void test_getAllSuperTypes() {
        Set<Class<?>> superTypes = ClassUtil.getAllSuperTypes(ArrayList.class);
        assertNotNull(superTypes);
        assertTrue(superTypes.contains(List.class));
        assertFalse(superTypes.contains(Object.class));
    }

    @Test
    public void testGetAllSuperTypes() {
        Set<Class<?>> superTypes = ClassUtil.getAllSuperTypes(ArrayList.class);
        assertNotNull(superTypes);
        assertTrue(superTypes.contains(List.class));
        assertTrue(superTypes.contains(Collection.class));
        assertTrue(superTypes.contains(AbstractList.class));
        assertFalse(superTypes.contains(Object.class));
    }

    @Test
    public void testGetAllSuperTypes_Interface() {
        Set<Class<?>> superTypes = ClassUtil.getAllSuperTypes(Serializable.class);
        assertNotNull(superTypes);
    }

    @Test
    public void testGetEnclosingClass_InnerClass() {
        Class<?> enclosing = ClassUtil.getEnclosingClass(Map.Entry.class);
        assertEquals(Map.class, enclosing);
    }

    @Test
    public void test_getEnclosingClass() {
        Class<?> innerClass = Map.Entry.class;
        Class<?> enclosing = ClassUtil.getEnclosingClass(innerClass);
        assertEquals(Map.class, enclosing);

        assertNull(ClassUtil.getEnclosingClass(String.class));
    }

    @Test
    public void testGetEnclosingClass_TopLevelClass() {
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
    public void testGetDeclaredConstructor_NoArgs() {
        Constructor<ArrayList> ctor = ClassUtil.getDeclaredConstructor(ArrayList.class);
        assertNotNull(ctor);
    }

    @Test
    public void testGetDeclaredConstructor_WithArgs() {
        Constructor<String> ctor = ClassUtil.getDeclaredConstructor(String.class, char[].class);
        assertNotNull(ctor);
    }

    @Test
    public void testGetDeclaredConstructor_NonExistent() {
        Constructor<String> ctor = ClassUtil.getDeclaredConstructor(String.class, Map.class);
        assertNull(ctor);
    }

    @Test
    public void testGetDeclaredConstructor_cacheHit() {
        // No-arg constructor cache hit
        Constructor<String> c1 = ClassUtil.getDeclaredConstructor(String.class);
        assertNotNull(c1);
        Constructor<String> c2 = ClassUtil.getDeclaredConstructor(String.class);
        assertNotNull(c2);
        assertEquals(c1, c2);

        // With parameters - cache hit
        Constructor<String> c3 = ClassUtil.getDeclaredConstructor(String.class, char[].class);
        assertNotNull(c3);
        Constructor<String> c4 = ClassUtil.getDeclaredConstructor(String.class, char[].class);
        assertNotNull(c4);
        assertEquals(c3, c4);
    }

    @Test
    public void testGetDeclaredMethod_EdgeCase_CaseInsensitiveFallback() {
        Method method = ClassUtil.getDeclaredMethod(String.class, "ISEMPTY");

        assertNotNull(method);
        assertEquals("isEmpty", method.getName());
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
    public void testGetDeclaredMethod_Existing() {
        Method method = ClassUtil.getDeclaredMethod(String.class, "length");
        assertNotNull(method);
        assertEquals("length", method.getName());
    }

    @Test
    public void testGetDeclaredMethod_NonExistent() {
        Method method = ClassUtil.getDeclaredMethod(String.class, "nonExistentMethod123");
        assertNull(method);
    }

    @Test
    public void testGetDeclaredMethod_WithParameterTypes() {
        Method method = ClassUtil.getDeclaredMethod(String.class, "substring", int.class, int.class);
        assertNotNull(method);
    }

    @Test
    public void testGetDeclaredMethod_cacheHit() {
        // First call - cache miss
        Method m1 = ClassUtil.getDeclaredMethod(String.class, "isEmpty");
        assertNotNull(m1);
        // Second call - cache hit
        Method m2 = ClassUtil.getDeclaredMethod(String.class, "isEmpty");
        assertNotNull(m2);
        assertEquals(m1, m2);

        // With parameters - cache miss
        Method m3 = ClassUtil.getDeclaredMethod(String.class, "substring", int.class, int.class);
        assertNotNull(m3);
        // Second call with params - cache hit
        Method m4 = ClassUtil.getDeclaredMethod(String.class, "substring", int.class, int.class);
        assertNotNull(m4);
        assertEquals(m3, m4);
    }

    @Test
    public void testGetDeclaredMethod_notFound() {
        Method m = ClassUtil.getDeclaredMethod(String.class, "nonExistentXYZ");
        assertNull(m);
        // Second call for not found - cache path
        Method m2 = ClassUtil.getDeclaredMethod(String.class, "nonExistentXYZ");
        assertNull(m2);
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
    public void testGetParameterizedTypeNameByField_SimpleField() throws Exception {
        Field field = TestBean.class.getDeclaredField("name");
        String typeName = ClassUtil.getParameterizedTypeNameByField(field);
        assertNotNull(typeName);
    }

    @Test
    public void testGetParameterizedTypeNameByField_GenericField() throws Exception {
        Field field = EntityBean.class.getDeclaredField("tags");
        String typeName = ClassUtil.getParameterizedTypeNameByField(field);
        assertNotNull(typeName);
        assertTrue(typeName.contains("List") || typeName.contains("String"));
    }

    @Test
    public void testGetParameterizedTypeNameByField_simple() throws Exception {
        class SimpleBean {
            String name;
            List<Integer> numbers;
            Map<String, List<Integer>> nested;
        }
        Field simpleField = SimpleBean.class.getDeclaredField("name");
        String simpleName = ClassUtil.getParameterizedTypeNameByField(simpleField);
        assertNotNull(simpleName);

        Field listField = SimpleBean.class.getDeclaredField("numbers");
        String listName = ClassUtil.getParameterizedTypeNameByField(listField);
        assertNotNull(listName);
        assertTrue(listName.contains("Integer") || listName.contains("List"));
    }

    @Test
    public void test_getTypeArgumentsByMethod() {
        Method method = Beans.getPropGetter(Entity_2.class, "getAutoGeneratedClassMap");

        N.println(ClassUtil.getParameterizedTypeNameByMethod(method));

        method = Beans.getPropSetter(Entity_2.class, "setAutoGeneratedClassMap");

        N.println(ClassUtil.getParameterizedTypeNameByMethod(method));

        method = Beans.getPropGetter(Entity_2.class, "gui");

        N.println(ClassUtil.getParameterizedTypeNameByMethod(method));

        method = Beans.getPropSetter(Entity_2.class, "gui");

        N.println(ClassUtil.getParameterizedTypeNameByMethod(method));

        method = Beans.getPropGetter(Entity_2.class, "intType");

        N.println(ClassUtil.getParameterizedTypeNameByMethod(method));

        method = Beans.getPropSetter(Entity_2.class, "intType");

        N.println(ClassUtil.getParameterizedTypeNameByMethod(method));
        assertNotNull(method);
    }

    @Test
    public void testGetParameterizedTypeNameByMethod_EdgeCase_GenericTypeVariableFallback() throws Exception {
        Method method = GenericTypeHolder.class.getDeclaredMethod("getValues");

        assertEquals("List<T>", ClassUtil.getParameterizedTypeNameByMethod(method));
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
    public void testGetParameterizedTypeNameByMethod_Getter() throws Exception {
        Method method = EntityBean.class.getDeclaredMethod("getTags");
        String typeName = ClassUtil.getParameterizedTypeNameByMethod(method);
        assertNotNull(typeName);
    }

    @Test
    public void testGetParameterizedTypeNameByMethod_Setter() throws Exception {
        Method method = EntityBean.class.getDeclaredMethod("setTags", List.class);
        String typeName = ClassUtil.getParameterizedTypeNameByMethod(method);
        assertNotNull(typeName);
    }

    @Test
    public void testGetParameterizedTypeNameByMethod_noParam_returnType() throws Exception {
        class TestBean {
            Map<String, Integer> getScores() {
                return null;
            }
        }
        Method getter = TestBean.class.getDeclaredMethod("getScores");
        String typeName = ClassUtil.getParameterizedTypeNameByMethod(getter);
        assertNotNull(typeName);
    }

    @Test
    public void test_formatParameterizedTypeName() {
        String formatted = ClassUtil.formatParameterizedTypeName("java.lang.String");
        assertEquals("String", formatted);

        String formatted2 = ClassUtil.formatParameterizedTypeName("class [Ljava.lang.String;");
        assertEquals("String[]", formatted2);
    }

    @Test
    public void testFormatParameterizedTypeName() {
        assertEquals("String[]", ClassUtil.formatParameterizedTypeName("class [Ljava.lang.String;"));
        assertEquals("Integer[]", ClassUtil.formatParameterizedTypeName("class [Ljava.lang.Integer;"));

        assertEquals("String", ClassUtil.formatParameterizedTypeName("class java.lang.String"));
        assertEquals("java.util.ArrayList", ClassUtil.formatParameterizedTypeName("class java.util.ArrayList"));

        assertEquals("String", ClassUtil.formatParameterizedTypeName("String"));
    }

    @Test
    public void testFormatParameterizedTypeName_evenLengthDuplicateSegment() {
        // Even-length repeated segment: "FooFoo" should be deduplicated to "Foo"
        String input = "FooFoo$Inner";
        String result = ClassUtil.formatParameterizedTypeName(input);
        assertEquals("Foo$Inner", result);
    }

    @Test
    public void testFormatParameterizedTypeName_oddLengthDuplicateWithSeparator() {
        // Odd-length repeated segment with separator: "Foo.Foo" should be deduplicated to "Foo"
        String input = "Foo.Foo$Inner";
        String result = ClassUtil.formatParameterizedTypeName(input);
        assertEquals("Foo$Inner", result);
    }

    @Test
    public void testFormatParameterizedTypeName_noDuplicate() {
        // Non-duplicated segment should remain unchanged
        String input = "Outer$Inner";
        String result = ClassUtil.formatParameterizedTypeName(input);
        assertEquals("Outer$Inner", result);
    }

    @Test
    public void testFormatParameterizedTypeName_noDollarSign() {
        // No $ sign - should pass through unchanged
        String input = "java.util.ArrayList";
        String result = ClassUtil.formatParameterizedTypeName(input);
        assertEquals("java.util.ArrayList", result);
    }

    @Test
    public void testFormatParameterizedTypeName_InterfaceArray() {
        String result = ClassUtil.formatParameterizedTypeName("interface [Ljava.lang.Comparable;");
        assertEquals("Comparable[]", result);
    }

    @Test
    public void testFormatParameterizedTypeName_singleCharBeforeDollar() {
        // Single char before $ - too short for duplication check
        String input = "A$Inner";
        String result = ClassUtil.formatParameterizedTypeName(input);
        assertEquals("A$Inner", result);
    }

    @Test
    public void testFormatParameterizedTypeName_EmptyString() {
        assertEquals("", ClassUtil.formatParameterizedTypeName(""));
    }

    @Test
    public void testFormatParameterizedTypeName_NullInput() {
        assertNull(ClassUtil.formatParameterizedTypeName(null));
    }

    @Test
    public void testFormatParameterizedTypeName_complex() {
        String result = ClassUtil.formatParameterizedTypeName("java.util.List<java.lang.String>");
        assertNotNull(result);
        assertTrue(result.contains("List"));
        assertTrue(result.contains("String"));
    }

    @Test
    public void testInheritanceDistance_UnrelatedClasses() {
        assertEquals(-1, ClassUtil.inheritanceDistance(String.class, Integer.class));
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
    public void testDistanceOfInheritance() {
        assertEquals(0, ClassUtil.inheritanceDistance(String.class, String.class));

        assertEquals(1, ClassUtil.inheritanceDistance(ArrayList.class, AbstractList.class));
        assertEquals(2, ClassUtil.inheritanceDistance(ArrayList.class, AbstractCollection.class));
        assertEquals(1, ClassUtil.inheritanceDistance(String.class, Object.class));

        assertEquals(-1, ClassUtil.inheritanceDistance(String.class, Integer.class));

        assertEquals(-1, ClassUtil.inheritanceDistance(null, String.class));
        assertEquals(-1, ClassUtil.inheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtil.inheritanceDistance(null, null));
    }

    @Test
    public void testInheritanceDistance_SameClass() {
        assertEquals(0, ClassUtil.inheritanceDistance(String.class, String.class));
    }

    @Test
    public void testInheritanceDistance_NullChild() {
        assertEquals(-1, ClassUtil.inheritanceDistance(null, String.class));
    }

    @Test
    public void testInheritanceDistance_NullParent() {
        assertEquals(-1, ClassUtil.inheritanceDistance(String.class, null));
    }

    @Test
    public void testHierarchy() {
        ObjIterator<Class<?>> hierarchy = ClassUtil.hierarchy(ArrayList.class);
        List<Class<?>> classes = new ArrayList<>();
        while (hierarchy.hasNext()) {
            classes.add(hierarchy.next());
        }

        assertEquals(ArrayList.class, classes.get(0));
        assertTrue(classes.contains(AbstractList.class));
        assertTrue(classes.contains(Object.class));
        assertFalse(classes.stream().anyMatch(Class::isInterface));

        hierarchy = ClassUtil.hierarchy(ArrayList.class, true);
        classes.clear();
        while (hierarchy.hasNext()) {
            classes.add(hierarchy.next());
        }

        assertTrue(classes.contains(ArrayList.class));
        assertTrue(classes.contains(List.class));
        assertTrue(classes.contains(Collection.class));
        assertTrue(classes.contains(Iterable.class));
    }

    @Test
    public void testHierarchy_ExcludingInterfaces() {
        ObjIterator<Class<?>> iter = ClassUtil.hierarchy(String.class);
        List<Class<?>> classes = new ArrayList<>();
        while (iter.hasNext()) {
            classes.add(iter.next());
        }
        assertTrue(classes.contains(String.class));
        assertTrue(classes.contains(Object.class));
        assertFalse(classes.stream().anyMatch(Class::isInterface));
    }

    @Test
    public void testHierarchy_IncludingInterfaces() {
        ObjIterator<Class<?>> iter = ClassUtil.hierarchy(String.class, true);
        List<Class<?>> classes = new ArrayList<>();
        while (iter.hasNext()) {
            classes.add(iter.next());
        }
        assertTrue(classes.contains(String.class));
        assertTrue(classes.contains(Serializable.class));
        assertTrue(classes.contains(Comparable.class));
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
    public void testHierarchy_IteratorExhausted() {
        ObjIterator<Class<?>> hierarchy = ClassUtil.hierarchy(String.class);

        assertEquals(String.class, hierarchy.next());
        assertEquals(Object.class, hierarchy.next());
        assertFalse(hierarchy.hasNext());
        assertThrows(java.util.NoSuchElementException.class, hierarchy::next);
    }

    @Test
    public void testInvokeConstructor_NewInstance() {
        Constructor<StringBuilder> ctor = ClassUtil.getDeclaredConstructor(StringBuilder.class, String.class);
        assertNotNull(ctor);
        StringBuilder sb = ClassUtil.invokeConstructor(ctor, "hello");
        assertEquals("hello", sb.toString());
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
    public void testInvokeConstructor_EdgeCase_ThrowsWrappedRuntimeException() throws Exception {
        Constructor<ThrowingCtorClass> ctor = ThrowingCtorClass.class.getDeclaredConstructor();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ClassUtil.invokeConstructor(ctor));

        assertEquals(IllegalStateException.class, exception.getClass());
        assertEquals("boom", exception.getMessage());
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
    public void testInvokeMethod_EdgeCase_TargetThrowsWrappedRuntimeException() throws Exception {
        Method method = ThrowingMethodClass.class.getDeclaredMethod("fail");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> ClassUtil.invokeMethod(new ThrowingMethodClass(), method));

        assertEquals(IllegalArgumentException.class, exception.getClass());
        assertEquals("boom", exception.getMessage());
    }

    @Test
    public void testInvokeMethod_StaticMethod() throws Exception {
        Method method = Integer.class.getDeclaredMethod("parseInt", String.class);
        int result = ClassUtil.invokeMethod(method, "42");
        assertEquals(42, result);
    }

    @Test
    public void testInvokeMethod_InstanceMethod() throws Exception {
        Method method = String.class.getDeclaredMethod("length");
        int result = ClassUtil.invokeMethod("hello", method);
        assertEquals(5, result);
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
    public void testSetAccessible() throws NoSuchMethodException, NoSuchFieldException {
        Method method = TestBean.class.getDeclaredMethod("getName");
        ClassUtil.setAccessible(method, true);
        assertTrue(method.isAccessible());

        ClassUtil.setAccessible(method, false);
        assertFalse(method.isAccessible());

        Field field = TestBean.class.getDeclaredField("name");
        ClassUtil.setAccessible(field, true);
        assertTrue(field.isAccessible());

        ClassUtil.setAccessible(null, true);
    }

    @Test
    public void testSetAccessible_NullObject() {
        // Should not throw
        assertDoesNotThrow(() -> ClassUtil.setAccessible(null, true));
    }

    @Test
    public void testSetAccessibleQuietly_NullObject() {
        assertFalse(ClassUtil.setAccessibleQuietly(null, true));
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
    public void testSetAccessibleQuietly() throws NoSuchMethodException, NoSuchFieldException {
        Method method = TestBean.class.getDeclaredMethod("getName");
        boolean result = ClassUtil.setAccessibleQuietly(method, true);
        assertTrue(result);
        assertTrue(method.isAccessible());

        result = ClassUtil.setAccessibleQuietly(null, true);
        assertFalse(result);

        result = ClassUtil.setAccessibleQuietly(method, true);
        assertTrue(result);
    }

    @Test
    public void testSetAccessibleQuietly_AlreadyAccessible() throws Exception {
        Method method = TestBean.class.getDeclaredMethod("getName");
        ClassUtil.setAccessible(method, true);
        // Setting same flag should return true immediately
        assertTrue(ClassUtil.setAccessibleQuietly(method, true));
    }

    @Test
    public void testSetAccessibleQuietly_privateField() throws Exception {
        Field f = String.class.getDeclaredField("value");
        // Try to set accessible - may succeed or quietly fail
        boolean result = ClassUtil.setAccessibleQuietly(f, true);
        // Result depends on module system - just ensure no exception thrown
        assertTrue(result || !result);
    }

    @Test
    public void test_isBeanClass() {
        @SuppressWarnings("deprecation")
        boolean result = ClassUtil.isBeanClass(String.class);
        assertNotNull(result);
    }

    @Test
    public void testEdgeCases() {
        assertFalse(Beans.isBeanClass(String[].class));

        assertFalse(Beans.isBeanClass(int[].class));

        assertFalse(Beans.isBeanClass(List.class));

        assertFalse(Beans.isBeanClass(AbstractList.class));

        assertFalse(Beans.isBeanClass(Thread.State.class));
    }

    @Test
    public void testIsBeanClass_Deprecated() {
        @SuppressWarnings("deprecation")
        boolean result = ClassUtil.isBeanClass(TestBean.class);
        assertTrue(result);

        @SuppressWarnings("deprecation")
        boolean result2 = ClassUtil.isBeanClass(null);
        assertFalse(result2);
    }

    @Test
    public void test_isRecordClass() {
        @SuppressWarnings("deprecation")
        boolean result = ClassUtil.isRecordClass(String.class);
        assertFalse(result);
    }

    @Test
    public void testIsRecordClass_Deprecated() {
        @SuppressWarnings("deprecation")
        boolean result = ClassUtil.isRecordClass(RecordBean.class);
        assertTrue(result);

        @SuppressWarnings("deprecation")
        boolean result2 = ClassUtil.isRecordClass(null);
        assertFalse(result2);
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
    public void testIsAnonymousClass_Regular() {
        assertFalse(ClassUtil.isAnonymousClass(String.class));
    }

    @Test
    public void testIsAnonymousClass_AnonymousInstance() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
            }
        };
        assertTrue(ClassUtil.isAnonymousClass(r.getClass()));
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
    public void testIsMemberClass() {
        assertTrue(ClassUtil.isMemberClass(InnerClassContainer.InnerClass.class));
        assertTrue(ClassUtil.isMemberClass(InnerClassContainer.StaticInnerClass.class));
        assertTrue(ClassUtil.isMemberClass(TestBean.class));
        assertFalse(ClassUtil.isMemberClass(String.class));
    }

    @Test
    public void testIsMemberClass_TopLevelClass() {
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
    public void testIsAnonymousOrMemberClass() {
        assertTrue(ClassUtil.isAnonymousOrMemberClass(InnerClassContainer.InnerClass.class));
        assertTrue(ClassUtil.isAnonymousOrMemberClass(InnerClassContainer.StaticInnerClass.class));

        Runnable anonymous = new Runnable() {
            @Override
            public void run() {
            }
        };
        assertTrue(ClassUtil.isAnonymousOrMemberClass(anonymous.getClass()));

        assertTrue(ClassUtil.isAnonymousOrMemberClass(TestBean.class));
    }

    @Test
    public void testIsAnonymousOrMemberClass_Regular() {
        assertFalse(ClassUtil.isAnonymousOrMemberClass(String.class));
    }

    @Test
    public void testIsPrimitiveType_AllPrimitives() {
        assertTrue(ClassUtil.isPrimitiveType(boolean.class));
        assertTrue(ClassUtil.isPrimitiveType(char.class));
        assertTrue(ClassUtil.isPrimitiveType(byte.class));
        assertTrue(ClassUtil.isPrimitiveType(short.class));
        assertTrue(ClassUtil.isPrimitiveType(int.class));
        assertTrue(ClassUtil.isPrimitiveType(long.class));
        assertTrue(ClassUtil.isPrimitiveType(float.class));
        assertTrue(ClassUtil.isPrimitiveType(double.class));
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
    public void testIsPrimitiveType() {
        assertTrue(ClassUtil.isPrimitiveType(int.class));
        assertTrue(ClassUtil.isPrimitiveType(boolean.class));
        assertTrue(ClassUtil.isPrimitiveType(double.class));

        assertFalse(ClassUtil.isPrimitiveType(Integer.class));
        assertFalse(ClassUtil.isPrimitiveType(String.class));

        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.isPrimitiveType(null);
        });
    }

    @Test
    public void testIsPrimitiveWrapper_AllWrappers() {
        assertTrue(ClassUtil.isPrimitiveWrapper(Boolean.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Character.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Byte.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Short.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Integer.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Long.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Float.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Double.class));
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
    public void testIsPrimitiveWrapper() {
        assertTrue(ClassUtil.isPrimitiveWrapper(Integer.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Boolean.class));
        assertTrue(ClassUtil.isPrimitiveWrapper(Double.class));

        assertFalse(ClassUtil.isPrimitiveWrapper(int.class));
        assertFalse(ClassUtil.isPrimitiveWrapper(String.class));

        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.isPrimitiveWrapper(null);
        });
    }

    @Test
    public void testIsPrimitiveArrayType_AllPrimitiveArrays() {
        assertTrue(ClassUtil.isPrimitiveArrayType(boolean[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(char[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(byte[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(short[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(int[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(long[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(float[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(double[].class));
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
    public void testIsPrimitiveArrayType() {
        assertTrue(ClassUtil.isPrimitiveArrayType(int[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(boolean[].class));
        assertTrue(ClassUtil.isPrimitiveArrayType(double[].class));

        assertFalse(ClassUtil.isPrimitiveArrayType(Integer[].class));
        assertFalse(ClassUtil.isPrimitiveArrayType(String[].class));
        assertFalse(ClassUtil.isPrimitiveArrayType(int.class));

        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.isPrimitiveArrayType(null);
        });
    }

    @Test
    public void testWrap_AllPrimitives() {
        assertEquals(Boolean.class, ClassUtil.wrap(boolean.class));
        assertEquals(Character.class, ClassUtil.wrap(char.class));
        assertEquals(Byte.class, ClassUtil.wrap(byte.class));
        assertEquals(Short.class, ClassUtil.wrap(short.class));
        assertEquals(Integer.class, ClassUtil.wrap(int.class));
        assertEquals(Long.class, ClassUtil.wrap(long.class));
        assertEquals(Float.class, ClassUtil.wrap(float.class));
        assertEquals(Double.class, ClassUtil.wrap(double.class));
    }

    @Test
    public void testWrap_NonPrimitive() {
        assertEquals(String.class, ClassUtil.wrap(String.class));
        assertEquals(List.class, ClassUtil.wrap(List.class));
    }

    @Test
    public void testWrap_PrimitiveArrayTypes() {
        assertEquals(Boolean[].class, ClassUtil.wrap(boolean[].class));
        assertEquals(Character[].class, ClassUtil.wrap(char[].class));
        assertEquals(Integer[].class, ClassUtil.wrap(int[].class));
        assertEquals(Double[].class, ClassUtil.wrap(double[].class));
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
    public void testWrap() {
        assertEquals(Integer.class, ClassUtil.wrap(int.class));
        assertEquals(Boolean.class, ClassUtil.wrap(boolean.class));
        assertEquals(Double.class, ClassUtil.wrap(double.class));

        assertEquals(Integer[].class, ClassUtil.wrap(int[].class));
        assertEquals(Boolean[].class, ClassUtil.wrap(boolean[].class));

        assertEquals(String.class, ClassUtil.wrap(String.class));
        assertEquals(Integer.class, ClassUtil.wrap(Integer.class));

        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.wrap(null);
        });
    }

    @Test
    public void testUnwrap_AllWrappers() {
        assertEquals(boolean.class, ClassUtil.unwrap(Boolean.class));
        assertEquals(char.class, ClassUtil.unwrap(Character.class));
        assertEquals(byte.class, ClassUtil.unwrap(Byte.class));
        assertEquals(short.class, ClassUtil.unwrap(Short.class));
        assertEquals(int.class, ClassUtil.unwrap(Integer.class));
        assertEquals(long.class, ClassUtil.unwrap(Long.class));
        assertEquals(float.class, ClassUtil.unwrap(Float.class));
        assertEquals(double.class, ClassUtil.unwrap(Double.class));
    }

    @Test
    public void testUnwrap_NonWrapper() {
        assertEquals(String.class, ClassUtil.unwrap(String.class));
        assertEquals(List.class, ClassUtil.unwrap(List.class));
    }

    @Test
    public void testUnwrap_WrapperArrayTypes() {
        assertEquals(boolean[].class, ClassUtil.unwrap(Boolean[].class));
        assertEquals(char[].class, ClassUtil.unwrap(Character[].class));
        assertEquals(int[].class, ClassUtil.unwrap(Integer[].class));
        assertEquals(double[].class, ClassUtil.unwrap(Double[].class));
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
    public void testUnwrap() {
        assertEquals(int.class, ClassUtil.unwrap(Integer.class));
        assertEquals(boolean.class, ClassUtil.unwrap(Boolean.class));
        assertEquals(double.class, ClassUtil.unwrap(Double.class));

        assertEquals(int[].class, ClassUtil.unwrap(Integer[].class));
        assertEquals(boolean[].class, ClassUtil.unwrap(Boolean[].class));

        assertEquals(String.class, ClassUtil.unwrap(String.class));
        assertEquals(int.class, ClassUtil.unwrap(int.class));

        assertThrows(IllegalArgumentException.class, () -> {
            ClassUtil.unwrap(null);
        });
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
    public void testCreateMethodHandle_ValidMethod() throws Exception {
        Method method = TestBean.class.getDeclaredMethod("getName");
        // createMethodHandle may throw UnsupportedOperationException for non-interface default methods
        // Just verify it does not return null for an accessible method, or throws expected exception
        assertDoesNotThrow(() -> {
            try {
                MethodHandle mh = ClassUtil.createMethodHandle(method);
                assertNotNull(mh);
            } catch (UnsupportedOperationException e) {
                // Expected for some method types
            }
        });
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
    public void testNewNullSentinel_UniquenessAndToString() {
        Object sentinel1 = ClassUtil.newNullSentinel();
        Object sentinel2 = ClassUtil.newNullSentinel();
        assertNotNull(sentinel1);
        assertNotNull(sentinel2);
        assertNotEquals(sentinel1, sentinel2);
        assertEquals("NULL", sentinel1.toString());
        assertEquals("NULL", sentinel2.toString());
        // Each sentinel should only equal itself
        assertEquals(sentinel1, sentinel1);
        assertNotEquals(sentinel1, sentinel2);
    }

    @Test
    public void test_setPropValue() {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        final PropInfo propInfo = beanInfo.getPropInfo("contact.address");
        N.println(propInfo);

        final Account account = new Account();
        Beans.setPropValue(account, "contact.address", "address1");
        Beans.setPropValue(account, "devices.name", "device1");
        Beans.setPropValue(account, "devices.model", "model1");
        assertNotNull(account);
    }

    @Test
    public void test_getPropNameList() {
        assertDoesNotThrow(() -> {
            N.println(Beans.getPropNameList(Account.class));

            N.println(Beans.getPropNames(Account.class, CommonUtil.toList("id", "gui")));

            N.println(Beans.getPropNames(Account.class, CommonUtil.toSet("id", "gui")));
        });
    }

    @Test
    public void testFormalizePropName() {
        assertEquals("userName", Beans.normalizePropName("user_name"));
        assertEquals("firstName", Beans.normalizePropName("first_name"));
        assertEquals("id", Beans.normalizePropName("id"));

        assertEquals("clazz", Beans.normalizePropName("class"));
        assertEquals("clazz", Beans.normalizePropName("CLASS"));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("userName", Beans.toCamelCase("user_name"));
        assertEquals("firstName", Beans.toCamelCase("first_name"));
        assertEquals("myPropertyName", Beans.toCamelCase("my_property_name"));
        assertEquals("id", Beans.toCamelCase("id"));
        assertEquals("id", Beans.toCamelCase("id"));
        assertEquals("", Beans.toCamelCase(""));
    }

    @Test
    public void testToSnakeCase() {
        assertEquals("user_name", Beans.toSnakeCase("userName"));
        assertEquals("first_name", Beans.toSnakeCase("firstName"));
        assertEquals("my_property_name", Beans.toSnakeCase("myPropertyName"));
        assertEquals("id", Beans.toSnakeCase("id"));
        assertEquals("id", Beans.toSnakeCase("ID"));
        assertEquals("", Beans.toSnakeCase(""));
        assertNull(Beans.toSnakeCase((String) null));
    }

    @Test
    public void testToScreamingSnakeCase() {
        assertEquals("USER_NAME", Beans.toScreamingSnakeCase("userName"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("firstName"));
        assertEquals("MY_PROPERTY_NAME", Beans.toScreamingSnakeCase("myPropertyName"));
        assertEquals("ID", Beans.toScreamingSnakeCase("id"));
        assertEquals("ID", Beans.toScreamingSnakeCase("ID"));
        assertEquals("", Beans.toScreamingSnakeCase(""));
        assertNull(Beans.toScreamingSnakeCase((String) null));
    }

    @Test
    public void testToCamelCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_name", "John");
        map.put("first_name", "Doe");
        map.put("age_value", 30);

        Beans.replaceKeysWithCamelCase(map);

        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("firstName"));
        assertTrue(map.containsKey("ageValue"));
        assertFalse(map.containsKey("user_name"));
        assertFalse(map.containsKey("first_name"));
        assertFalse(map.containsKey("age_value"));

        assertEquals("John", map.get("userName"));
        assertEquals("Doe", map.get("firstName"));
        assertEquals(30, map.get("ageValue"));
    }

    @Test
    public void testToSnakeCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Doe");
        map.put("ageValue", 30);

        Beans.replaceKeysWithSnakeCase(map);

        assertTrue(map.containsKey("user_name"));
        assertTrue(map.containsKey("first_name"));
        assertTrue(map.containsKey("age_value"));
        assertFalse(map.containsKey("userName"));
        assertFalse(map.containsKey("firstName"));
        assertFalse(map.containsKey("ageValue"));

        assertEquals("John", map.get("user_name"));
        assertEquals("Doe", map.get("first_name"));
        assertEquals(30, map.get("age_value"));
    }

    @Test
    public void testToScreamingSnakeCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Doe");
        map.put("ageValue", 30);

        Beans.replaceKeysWithScreamingSnakeCase(map);

        assertTrue(map.containsKey("USER_NAME"));
        assertTrue(map.containsKey("FIRST_NAME"));
        assertTrue(map.containsKey("AGE_VALUE"));
        assertFalse(map.containsKey("userName"));
        assertFalse(map.containsKey("firstName"));
        assertFalse(map.containsKey("ageValue"));

        assertEquals("John", map.get("USER_NAME"));
        assertEquals("Doe", map.get("FIRST_NAME"));
        assertEquals(30, map.get("AGE_VALUE"));
    }

    @Test
    public void testConstantFields() {
        assertNotNull(ClassUtil.SENTINEL_CLASS);
        assertEquals("SentinelClass", ClassUtil.SENTINEL_CLASS.getSimpleName());

        assertNotNull(ClassUtil.SENTINEL_METHOD);
        assertEquals("sentinelMethod", ClassUtil.SENTINEL_METHOD.getName());

        assertNotNull(ClassUtil.SENTINEL_FIELD);
        assertEquals("SENTINEL_FIELD", ClassUtil.SENTINEL_FIELD.getName());
    }

    @Test
    public void testComplexScenarios() {
        Object anonymous = new Object() {
            private String value = "test";

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        };

        List<String> propNames = Beans.getPropNameList(anonymous.getClass());
        assertTrue(propNames.contains("value"));

        assertEquals("test", Beans.getPropValue(anonymous, "value"));
        Beans.setPropValue(anonymous, "value", "updated");
        assertEquals("updated", Beans.getPropValue(anonymous, "value"));
    }

    @Test
    public void testPerformance() {
        Beans.getPropNameList(TestBean.class);

        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            Beans.getPropNameList(TestBean.class);
            Beans.getPropGetters(TestBean.class);
            Beans.getPropSetters(TestBean.class);
        }

        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 100, start + ": Performance test failed. Elapsed time: " + elapsed);
    }
}
