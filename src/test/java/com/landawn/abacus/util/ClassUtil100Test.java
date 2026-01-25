package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.util.Tuple.Tuple3;

@Tag("new-test")
public class ClassUtil100Test extends TestBase {

    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private String _hidden;
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
            return name != null;
        }

        public String get_hidden() {
            return _hidden;
        }

        public void set_hidden(String hidden) {
            this._hidden = hidden;
        }
    }

    @Entity
    public static class EntityBean {
        private String id;
        private List<String> tags;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getTags() {
            if (tags == null) {
                tags = new ArrayList<>();
            }
            return tags;
        }
    }

    public static class ImmutableBean {
        private final String value;

        public ImmutableBean(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class BuilderBean {
        private String name;
        private int value;

        private BuilderBean() {
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private int value;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder value(int value) {
                this.value = value;
                return this;
            }

            public BuilderBean build() {
                BuilderBean bean = new BuilderBean();
                bean.name = this.name;
                bean.value = this.value;
                return bean;
            }
        }
    }

    public static class DiffBean {
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

    @Record
    public static class RecordBean {
        private final String name;
        private final int value;

        public RecordBean(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    public static class NestedBean {
        private TestBean inner;

        public TestBean getInner() {
            return inner;
        }

        public void setInner(TestBean inner) {
            this.inner = inner;
        }
    }

    public static class NonBean {
    }

    public interface TestInterface {
        public void doSomething();
    }

    public static abstract class AbstractTestClass implements TestInterface {
        public abstract void doMore();
    }

    public static class ConcreteTestClass extends AbstractTestClass {
        @Override
        public void doSomething() {
        }

        @Override
        public void doMore() {
        }
    }

    public static class InnerClassContainer {
        public class InnerClass {
        }

        public static class StaticInnerClass {
        }
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testRegisterNonBeanClass() {
        Beans.registerNonBeanClass(NonBean.class);

        List<String> propNames = Beans.getPropNameList(NonBean.class);
        assertTrue(propNames.isEmpty() || propNames.size() == 0);
    }

    @Test
    public void testRegisterNonPropGetSetMethod() {
        Beans.registerNonPropertyAccessor(TestBean.class, "name");

    }

    @Test
    public void testRegisterPropGetSetMethod() throws NoSuchMethodException {
        Method getMethod = TestBean.class.getMethod("getName");
        Beans.registerPropertyAccessor("customName", getMethod);

        Method setMethod = TestBean.class.getMethod("setName", String.class);
        Beans.registerPropertyAccessor("customName", setMethod);

        Method invalidMethod = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> {
            Beans.registerPropertyAccessor("invalid", invalidMethod);
        });
    }

    @Test
    public void testRegisterXMLBindingClass() {
        Beans.registerXmlBindingClass(EntityBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(EntityBean.class));

        assertFalse(Beans.isRegisteredXmlBindingClass(TestBean.class));
    }

    @Test
    public void testCreateMethodHandle() throws NoSuchMethodException {
        Method method = TestBean.class.getMethod("getName");
        MethodHandle handle = ClassUtil.createMethodHandle(method);
        assertNotNull(handle);
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
    public void testFormalizePropName() {
        assertEquals("userName", Beans.formalizePropName("user_name"));
        assertEquals("firstName", Beans.formalizePropName("first_name"));
        assertEquals("id", Beans.formalizePropName("id"));

        assertEquals("clazz", Beans.formalizePropName("class"));
        assertEquals("clazz", Beans.formalizePropName("CLASS"));
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
    public void testGetCanonicalClassName() {
        assertEquals("java.lang.String", ClassUtil.getCanonicalClassName(String.class));
        assertEquals("java.util.ArrayList", ClassUtil.getCanonicalClassName(ArrayList.class));
        assertEquals("int", ClassUtil.getCanonicalClassName(int.class));

        assertEquals("java.lang.String[]", ClassUtil.getCanonicalClassName(String[].class));
    }

    @Test
    public void testGetClassName() {
        assertEquals("java.lang.String", ClassUtil.getClassName(String.class));
        assertEquals("[Ljava.lang.String;", ClassUtil.getClassName(String[].class));
        assertEquals("int", ClassUtil.getClassName(int.class));
    }

    @Test
    public void testGetSimpleClassName() {
        assertEquals("String", ClassUtil.getSimpleClassName(String.class));
        assertEquals("ArrayList", ClassUtil.getSimpleClassName(ArrayList.class));
        assertEquals("int", ClassUtil.getSimpleClassName(int.class));
        assertEquals("String[]", ClassUtil.getSimpleClassName(String[].class));
    }

    @Test
    public void testGetPackage() {
        Package pkg = ClassUtil.getPackage(String.class);
        assertNotNull(pkg);
        assertEquals("java.lang", pkg.getName());

        assertNull(ClassUtil.getPackage(int.class));
    }

    @Test
    public void testGetPackageName() {
        assertEquals("java.lang", ClassUtil.getPackageName(String.class));
        assertEquals("java.util", ClassUtil.getPackageName(ArrayList.class));

        assertEquals("", ClassUtil.getPackageName(int.class));
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
    public void testGetAllInterfaces() {
        Set<Class<?>> interfaces = ClassUtil.getAllInterfaces(ArrayList.class);
        assertNotNull(interfaces);
        assertTrue(interfaces.contains(List.class));
        assertTrue(interfaces.contains(Collection.class));
        assertTrue(interfaces.contains(Iterable.class));

        interfaces = ClassUtil.getAllInterfaces(ConcreteTestClass.class);
        assertTrue(interfaces.contains(TestInterface.class));
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
    public void testGetAllSuperTypes() {
        Set<Class<?>> superTypes = ClassUtil.getAllSuperTypes(ArrayList.class);
        assertNotNull(superTypes);
        assertTrue(superTypes.contains(List.class));
        assertTrue(superTypes.contains(Collection.class));
        assertTrue(superTypes.contains(AbstractList.class));
        assertFalse(superTypes.contains(Object.class));
    }

    @Test
    public void testGetEnclosingClass() {
        assertEquals(InnerClassContainer.class, ClassUtil.getEnclosingClass(InnerClassContainer.InnerClass.class));
        assertEquals(InnerClassContainer.class, ClassUtil.getEnclosingClass(InnerClassContainer.StaticInnerClass.class));

        assertNull(ClassUtil.getEnclosingClass(String.class));
    }

    @Test
    public void testGetDeclaredConstructor() {
        Constructor<TestBean> noArgConstructor = ClassUtil.getDeclaredConstructor(TestBean.class);
        assertNotNull(noArgConstructor);

        Constructor<ImmutableBean> paramConstructor = ClassUtil.getDeclaredConstructor(ImmutableBean.class, String.class);
        assertNotNull(paramConstructor);

        Constructor<ImmutableBean> nonExistent = ClassUtil.getDeclaredConstructor(ImmutableBean.class, Integer.class);
        assertNull(nonExistent);
    }

    @Test
    public void testGetDeclaredMethod() {
        Method method = ClassUtil.getDeclaredMethod(TestBean.class, "getName");
        assertNotNull(method);
        assertEquals("getName", method.getName());

        method = ClassUtil.getDeclaredMethod(TestBean.class, "setName", String.class);
        assertNotNull(method);
        assertEquals("setName", method.getName());

        method = ClassUtil.getDeclaredMethod(TestBean.class, "nonExistent");
        assertNull(method);
    }

    @Test
    public void testGetParameterizedTypeNameByField() throws NoSuchFieldException {
        Field field = TestBean.class.getDeclaredField("name");
        String typeName = ClassUtil.getParameterizedTypeNameByField(field);
        assertEquals("String", typeName);

        field = EntityBean.class.getDeclaredField("tags");
        typeName = ClassUtil.getParameterizedTypeNameByField(field);
        assertTrue(typeName.contains("List"));
    }

    @Test
    public void testGetParameterizedTypeNameByMethod() throws NoSuchMethodException {
        Method method = EntityBean.class.getMethod("getTags");
        String typeName = ClassUtil.getParameterizedTypeNameByMethod(method);
        assertTrue(typeName.contains("List"));

        method = TestBean.class.getMethod("setName", String.class);
        typeName = ClassUtil.getParameterizedTypeNameByMethod(method);
        assertEquals("String", typeName);
    }

    @Test
    public void testGetPropNameByMethod() throws NoSuchMethodException {
        Method method = TestBean.class.getMethod("getName");
        assertEquals("name", Beans.getPropNameByMethod(method));

        method = TestBean.class.getMethod("setAge", int.class);
        assertEquals("age", Beans.getPropNameByMethod(method));

        method = TestBean.class.getMethod("isActive");
        assertEquals("active", Beans.getPropNameByMethod(method));

        method = TestBean.class.getMethod("hasData");
        assertEquals("data", Beans.getPropNameByMethod(method));
    }

    @Test
    public void testGetPropNameList() {
        ImmutableList<String> propNames = Beans.getPropNameList(TestBean.class);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("age"));
        assertTrue(propNames.contains("active"));
        assertTrue(propNames.contains("publicField"));

        propNames = Beans.getPropNameList(String.class);
        assertNotNull(propNames);
    }

    @Test
    public void testGetPropNamesWithExclusion() {
        Set<String> toExclude = new HashSet<>();
        toExclude.add("age");
        toExclude.add("active");

        List<String> propNames = Beans.getPropNames(TestBean.class, toExclude);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertFalse(propNames.contains("age"));
        assertFalse(propNames.contains("active"));

        propNames = Beans.getPropNames(TestBean.class, new HashSet<>());
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("age"));
    }

    @Test
    public void testGetPropNamesWithPredicate() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setAge(25);

        List<String> propNames = Beans.getPropNames(bean, name -> name.startsWith("a"));
        assertNotNull(propNames);
        assertTrue(propNames.contains("age"));
        assertTrue(propNames.contains("active"));
        assertFalse(propNames.contains("name"));
    }

    @Test
    public void testGetPropNamesWithBiPredicate() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setAge(25);
        bean.setActive(true);

        List<String> propNames = Beans.getPropNames(bean, (name, value) -> value != null && !value.equals(false));
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("age"));
        assertTrue(propNames.contains("active"));
    }

    @Test
    public void testGetPropField() {
        Field field = Beans.getPropField(TestBean.class, "name");
        assertNotNull(field);
        assertEquals("name", field.getName());

        field = Beans.getPropField(TestBean.class, "age");
        assertNotNull(field);
        assertEquals("age", field.getName());

        field = Beans.getPropField(TestBean.class, "nonExistent");
        assertNull(field);

        field = Beans.getPropField(TestBean.class, "NAME");
        assertNotNull(field);
    }

    @Test
    public void testGetPropFields() {
        ImmutableMap<String, Field> fields = Beans.getPropFields(TestBean.class);
        assertNotNull(fields);
        assertTrue(fields.containsKey("name"));
        assertTrue(fields.containsKey("age"));
        assertTrue(fields.containsKey("active"));
    }

    @Test
    public void testGetPropGetMethod() {
        Method method = Beans.getPropGetter(TestBean.class, "name");
        assertNotNull(method);
        assertEquals("getName", method.getName());

        method = Beans.getPropGetter(TestBean.class, "active");
        assertNotNull(method);
        assertEquals("isActive", method.getName());

        method = Beans.getPropGetter(TestBean.class, "nonExistent");
        assertNull(method);
    }

    @Test
    public void testGetPropGetMethods() {
        ImmutableMap<String, Method> methods = Beans.getPropGetters(TestBean.class);
        assertNotNull(methods);
        assertTrue(methods.containsKey("name"));
        assertTrue(methods.containsKey("age"));
        assertTrue(methods.containsKey("active"));
        assertFalse(methods.containsKey("data"));
    }

    @Test
    public void testGetPropSetMethod() {
        Method method = Beans.getPropSetter(TestBean.class, "name");
        assertNotNull(method);
        assertEquals("setName", method.getName());

        method = Beans.getPropSetter(TestBean.class, "age");
        assertNotNull(method);
        assertEquals("setAge", method.getName());

        method = Beans.getPropSetter(TestBean.class, "nonExistent");
        assertNull(method);
    }

    @Test
    public void testGetPropSetMethods() {
        ImmutableMap<String, Method> methods = Beans.getPropSetters(TestBean.class);
        assertNotNull(methods);
        assertTrue(methods.containsKey("name"));
        assertTrue(methods.containsKey("age"));
        assertTrue(methods.containsKey("active"));
    }

    @Test
    public void testGetPropValueByMethod() throws NoSuchMethodException {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        Method method = TestBean.class.getMethod("getName");
        String name = Beans.getPropValue(bean, method);
        assertEquals("John", name);

        method = TestBean.class.getMethod("getAge");
        int age = Beans.getPropValue(bean, method);
        assertEquals(30, age);
    }

    @Test
    public void testGetPropValueByName() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        assertEquals("John", Beans.getPropValue(bean, "name"));
        assertEquals(30, (Integer) Beans.getPropValue(bean, "age"));
        assertEquals(true, Beans.getPropValue(bean, "active"));

        NestedBean nested = new NestedBean();
        nested.setInner(bean);
        assertEquals("John", Beans.getPropValue(nested, "inner.name"));
        assertEquals(30, (Integer) Beans.getPropValue(nested, "inner.age"));

        NestedBean emptyNested = new NestedBean();
        Object result = Beans.getPropValue(emptyNested, "inner.name", true);
        assertNull(result);

        assertThrows(IllegalArgumentException.class, () -> {
            Beans.getPropValue(bean, "nonExistent");
        });

        assertNull(Beans.getPropValue(bean, "nonExistent", true));
    }

    @Test
    public void testSetPropValueByMethod() throws NoSuchMethodException {
        TestBean bean = new TestBean();
        Method method = TestBean.class.getMethod("setName", String.class);

        Object result = Beans.setPropValue(bean, method, "John");
        assertEquals("John", result);
        assertEquals("John", bean.getName());

        result = Beans.setPropValue(bean, method, null);
        assertNull(result);
        assertNull(bean.getName());

        method = TestBean.class.getMethod("setAge", int.class);
        result = Beans.setPropValue(bean, method, "25");
        assertEquals(25, bean.getAge());
    }

    @Test
    public void testSetPropValueByName() {
        TestBean bean = new TestBean();

        Beans.setPropValue(bean, "name", "John");
        assertEquals("John", bean.getName());

        Beans.setPropValue(bean, "age", 30);
        assertEquals(30, bean.getAge());

        Beans.setPropValue(bean, "active", true);
        assertTrue(bean.isActive());

        NestedBean nested = new NestedBean();
        TestBean inner = new TestBean();
        nested.setInner(inner);

        boolean result = Beans.setPropValue(nested, "inner.name", "Jane", false);
        assertTrue(result);
        assertEquals("Jane", nested.getInner().getName());

        assertThrows(IllegalArgumentException.class, () -> {
            Beans.setPropValue(bean, "nonExistent", "value");
        });

        result = Beans.setPropValue(bean, "nonExistent", "value", true);
        assertFalse(result);
    }

    @Test
    public void testSetPropValueByGet() throws NoSuchMethodException {
        EntityBean bean = new EntityBean();
        Method method = EntityBean.class.getMethod("getTags");

        List<String> newTags = Arrays.asList("tag1", "tag2");
        Beans.setPropValueByGetter(bean, method, newTags);

        assertEquals(2, bean.getTags().size());
        assertTrue(bean.getTags().contains("tag1"));
        assertTrue(bean.getTags().contains("tag2"));

        Beans.setPropValueByGetter(bean, method, null);
        assertEquals(2, bean.getTags().size());

        Method invalidMethod = TestBean.class.getMethod("getName");
        TestBean testBean = new TestBean();
        assertThrows(IllegalArgumentException.class, () -> {
            Beans.setPropValueByGetter(testBean, invalidMethod, "value");
        });
    }

    @Test
    public void testGetDiffIgnoredPropNames() {
        ImmutableSet<String> ignoredProps = Beans.getDiffIgnoredPropNames(DiffBean.class);
        assertNotNull(ignoredProps);
        assertTrue(ignoredProps.contains("ignoredField"));
        assertFalse(ignoredProps.contains("name"));
    }

    @Test
    public void testGetBuilderInfo() {
        Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> builderInfo = Beans
                .getBuilderInfo(BuilderBean.class);

        assertNotNull(builderInfo);
        assertEquals(BuilderBean.Builder.class, builderInfo._1);
        assertNotNull(builderInfo._2);
        assertNotNull(builderInfo._3);

        Object builder = builderInfo._2.get();
        assertNotNull(builder);
        assertTrue(builder instanceof BuilderBean.Builder);

        builderInfo = Beans.getBuilderInfo(TestBean.class);
        assertNull(builderInfo);
    }

    @Test
    public void testGetTypeName() {
        assertEquals("String", ClassUtil.getTypeName(String.class));
        assertEquals("List<String>", new TypeReference<List<String>>() {
        }.type().name());
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
    public void testInvokeConstructor() throws Exception {
        Constructor<TestBean> constructor = TestBean.class.getDeclaredConstructor();
        TestBean bean = ClassUtil.invokeConstructor(constructor);
        assertNotNull(bean);

        Constructor<ImmutableBean> paramConstructor = ImmutableBean.class.getDeclaredConstructor(String.class);
        ImmutableBean immutable = ClassUtil.invokeConstructor(paramConstructor, "test");
        assertNotNull(immutable);
        assertEquals("test", immutable.getValue());
    }

    @Test
    public void testInvokeMethod() throws NoSuchMethodException {
        TestBean bean = new TestBean();
        bean.setName("John");

        Method method = TestBean.class.getMethod("getName");
        String name = ClassUtil.invokeMethod(bean, method);
        assertEquals("John", name);

        Method staticMethod = Integer.class.getMethod("valueOf", String.class);
        Integer value = ClassUtil.invokeMethod(staticMethod, "123");
        assertEquals(123, value);

        method = TestBean.class.getMethod("setAge", int.class);
        ClassUtil.invokeMethod(bean, method, 25);
        assertEquals(25, bean.getAge());
    }

    @Test
    public void testIsBeanClass() {
        assertTrue(Beans.isBeanClass(TestBean.class));
        assertTrue(Beans.isBeanClass(EntityBean.class));
        assertTrue(Beans.isBeanClass(RecordBean.class));

        assertFalse(Beans.isBeanClass(String.class));
        assertFalse(Beans.isBeanClass(Integer.class));
        assertFalse(Beans.isBeanClass(Map.Entry.class));
        assertFalse(Beans.isBeanClass(null));
    }

    @Test
    public void testIsRecordClass() {
        assertTrue(Beans.isRecordClass(RecordBean.class));
        assertFalse(Beans.isRecordClass(TestBean.class));
        assertFalse(Beans.isRecordClass(String.class));
        assertFalse(Beans.isRecordClass(null));
    }

    @Test
    public void testIsAnonymousClass() {
        Runnable anonymous = new Runnable() {
            @Override
            public void run() {
            }
        };

        assertTrue(ClassUtil.isAnonymousClass(anonymous.getClass()));
        assertFalse(ClassUtil.isAnonymousClass(TestBean.class));
        assertFalse(ClassUtil.isAnonymousClass(String.class));
    }

    @Test
    public void testIsMemberClass() {
        assertTrue(ClassUtil.isMemberClass(InnerClassContainer.InnerClass.class));
        assertTrue(ClassUtil.isMemberClass(InnerClassContainer.StaticInnerClass.class));
        assertTrue(ClassUtil.isMemberClass(TestBean.class));
        assertFalse(ClassUtil.isMemberClass(String.class));
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
    public void test_newNullSentinel() {
        Object nullMask1 = ClassUtil.newNullSentinel();
        Object nullMask2 = ClassUtil.newNullSentinel();

        assertNotNull(nullMask1);
        assertNotNull(nullMask2);
        assertNotEquals(nullMask1, nullMask2);
        assertEquals("NULL", nullMask1.toString());
        assertEquals(nullMask1, nullMask1);
        assertNotEquals(nullMask1, "NULL");
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

        Beans.toCamelCaseKeys(map);

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

        Beans.toSnakeCaseKeys(map);

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

        Beans.toScreamingSnakeCaseKeys(map);

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
    public void testEdgeCases() {
        assertFalse(Beans.isBeanClass(String[].class));

        assertFalse(Beans.isBeanClass(int[].class));

        assertFalse(Beans.isBeanClass(List.class));

        assertFalse(Beans.isBeanClass(AbstractList.class));

        assertFalse(Beans.isBeanClass(Thread.State.class));
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
}
