package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.Tuple.Tuple3;

@Tag("new-test")
public class Beans100Test extends TestBase {

    public static class SimpleBean {
        private String name;
        private int age;
        private Boolean active;

        public SimpleBean() {
        }

        public SimpleBean(String name, int age) {
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

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class NestedBean {
        private String id;
        private SimpleBean simpleBean;
        private Address address;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SimpleBean getSimpleBean() {
            return simpleBean;
        }

        public void setSimpleBean(SimpleBean simpleBean) {
            this.simpleBean = simpleBean;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        private String street;
        private String city;
        private String zipCode;

        public Address() {
        }

        public Address(String city) {
            this.city = city;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
    }

    @Entity
    public static class EntityBean {
        private Long id;
        private String value;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Record
    public static class RecordBean {
        private String recordId;

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }
    }

    public static class BeanWithDiffIgnore {
        private String name;
        @DiffIgnore
        private Date lastModified;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }
    }

    public static class BeanWithBuilder {
        private String value;
        private int number;

        private BeanWithBuilder() {
        }

        public String getValue() {
            return value;
        }

        public int getNumber() {
            return number;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String value;
            private int number;

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public Builder number(int number) {
                this.number = number;
                return this;
            }

            public BeanWithBuilder build() {
                BeanWithBuilder bean = new BeanWithBuilder();
                bean.value = this.value;
                bean.number = this.number;
                return bean;
            }
        }
    }

    public static class NonBean {
        private String field;
    }

    public static class BeanWithSnakeCase {
        private String firstName;
        private String lastName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    private SimpleBean simpleBean;
    private NestedBean nestedBean;
    private EntityBean entityBean;

    @BeforeEach
    public void setUp() {
        simpleBean = new SimpleBean("John", 25);
        simpleBean.setActive(true);

        nestedBean = new NestedBean();
        nestedBean.setId("123");
        nestedBean.setSimpleBean(simpleBean);

        Address address = new Address("New York");
        address.setStreet("5th Avenue");
        address.setZipCode("10001");
        nestedBean.setAddress(address);

        entityBean = new EntityBean();
        entityBean.setId(1L);
        entityBean.setValue("test");
    }

    @Test
    public void testIsBeanClass() {
        assertTrue(Beans.isBeanClass(SimpleBean.class));
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
        assertFalse(Beans.isRecordClass(SimpleBean.class));
        assertFalse(Beans.isRecordClass(null));
    }

    @Test
    public void testGetBeanInfo() {
        BeanInfo beanInfo = Beans.getBeanInfo(SimpleBean.class);
        assertNotNull(beanInfo);
        assertFalse(beanInfo.propInfoList.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Beans.getBeanInfo(NonBean.class));
    }

    @Test
    public void testRefreshBeanPropInfo() {
        Beans.refreshBeanPropInfo(SimpleBean.class);
    }

    @Test
    public void testGetBuilderInfo() {
        Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> builderInfo = Beans
                .getBuilderInfo(BeanWithBuilder.class);

        assertNotNull(builderInfo);
        assertNotNull(builderInfo._2);
        assertNotNull(builderInfo._3);

        Object builder = builderInfo._2.get();
        assertNotNull(builder);

        assertNull(Beans.getBuilderInfo(SimpleBean.class));

        assertThrows(IllegalArgumentException.class, () -> Beans.getBuilderInfo(null));
    }

    @Test
    public void testRegisterNonBeanClass() {
        class TestNonBean {
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }

        assertTrue(Beans.isBeanClass(TestNonBean.class));
        Beans.registerNonBeanClass(TestNonBean.class);
    }

    @Test
    public void testRegisterNonPropGetSetMethod() {
        Beans.registerNonPropertyAccessor(SimpleBean.class, "internal");
    }

    @Test
    public void testRegisterPropGetSetMethod() throws Exception {
        Method method = SimpleBean.class.getMethod("getName");
        Beans.registerPropertyAccessor("name", method);

        Method invalidMethod = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("invalid", invalidMethod));
    }

    @Test
    public void testRegisterXMLBindingClass() {
        Beans.registerXmlBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(SimpleBean.class));
    }

    @Test
    public void testGetPropNameByMethod() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        assertEquals("name", Beans.getPropNameByMethod(getName));

        Method getAge = SimpleBean.class.getMethod("getAge");
        assertEquals("age", Beans.getPropNameByMethod(getAge));

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        assertEquals("name", Beans.getPropNameByMethod(setName));
    }

    @Test
    public void testGetPropNameList() {
        ImmutableList<String> propNames = Beans.getPropNameList(SimpleBean.class);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("age"));
        assertTrue(propNames.contains("active"));

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNameList(null));
    }

    @Test
    public void testGetPropNamesWithExclusion() {
        List<String> props = Beans.getPropNames(SimpleBean.class, Collections.singleton("age"));
        assertTrue(props.contains("name"));
        assertTrue(props.contains("active"));
        assertFalse(props.contains("age"));

        props = Beans.getPropNames(SimpleBean.class, Arrays.asList("age", "active"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));
        assertFalse(props.contains("active"));

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames(null, Collections.emptySet()));
    }

    @Test
    public void testGetPropNamesFromBean() {
        simpleBean.setActive(null);
        List<String> props = Beans.getPropNames(simpleBean, true);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertFalse(props.contains("active"));

        props = Beans.getPropNames(simpleBean, false);
        assertTrue(props.contains("active"));

        props = Beans.getPropNames(simpleBean, name -> name.startsWith("a"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));
        assertFalse(props.contains("name"));

        props = Beans.getPropNames(simpleBean, (name, value) -> value instanceof String);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));
    }

    @Test
    public void testGetDiffIgnoredPropNames() {
        ImmutableSet<String> ignored = Beans.getDiffIgnoredPropNames(BeanWithDiffIgnore.class);
        assertTrue(ignored.contains("lastModified"));
        assertFalse(ignored.contains("name"));
    }

    @Test
    public void testGetPropField() {
        Field field = Beans.getPropField(SimpleBean.class, "name");
        assertNotNull(field);
        assertEquals("name", field.getName());

        field = Beans.getPropField(SimpleBean.class, "NAME");
        assertNotNull(field);

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropField(NonBean.class, "field"));
    }

    @Test
    public void testGetPropFields() {
        ImmutableMap<String, Field> fields = Beans.getPropFields(SimpleBean.class);
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        assertNotNull(fields.get("name"));
        assertNotNull(fields.get("age"));
    }

    @Test
    public void testGetPropGetMethod() {
        Method method = Beans.getPropGetter(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("getName", method.getName());

        method = Beans.getPropGetter(SimpleBean.class, "NAME");
        assertNotNull(method);

        assertNull(Beans.getPropGetter(SimpleBean.class, "nonExistent"));
    }

    @Test
    public void testGetPropGetMethods() {
        ImmutableMap<String, Method> methods = Beans.getPropGetters(SimpleBean.class);
        assertNotNull(methods);
        assertFalse(methods.isEmpty());
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
    }

    @Test
    public void testGetPropSetMethod() {
        Method method = Beans.getPropSetter(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("setName", method.getName());

        assertNull(Beans.getPropSetter(SimpleBean.class, "nonExistent"));
    }

    @Test
    public void testGetPropSetMethods() {
        ImmutableMap<String, Method> methods = Beans.getPropSetters(SimpleBean.class);
        assertNotNull(methods);
        assertFalse(methods.isEmpty());
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
    }

    @Test
    public void testGetPropValue() throws Exception {
        assertEquals("John", Beans.getPropValue(simpleBean, "name"));
        assertEquals(25, (Integer) Beans.getPropValue(simpleBean, "age"));
        assertEquals(true, Beans.getPropValue(simpleBean, "active"));

        Method getName = SimpleBean.class.getMethod("getName");
        assertEquals("John", Beans.getPropValue(simpleBean, getName));

        assertEquals("New York", Beans.getPropValue(nestedBean, "address.city"));

        assertNull(Beans.getPropValue(simpleBean, "nonExistent", true));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(simpleBean, "nonExistent", false));
    }

    @Test
    public void testSetPropValue() throws Exception {
        Beans.setPropValue(simpleBean, "name", "Jane");
        assertEquals("Jane", simpleBean.getName());

        Beans.setPropValue(simpleBean, "age", 30);
        assertEquals(30, simpleBean.getAge());

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.setPropValue(simpleBean, setName, "Bob");
        assertEquals("Bob", simpleBean.getName());

        Beans.setPropValue(simpleBean, "active", null);
        assertNull(simpleBean.getActive());

        boolean result = Beans.setPropValue(simpleBean, "nonExistent", "value", true);
        assertFalse(result);

        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(simpleBean, "nonExistent", "value", false));
    }

    @Test
    public void testSetPropValueByGet() throws Exception {
        List<String> list = new ArrayList<>();
        list.add("old");

        class BeanWithList {
            private List<String> items = list;

            public List<String> getItems() {
                return items;
            }
        }

        BeanWithList bean = new BeanWithList();
        Method getItems = BeanWithList.class.getMethod("getItems");

        List<String> newList = Arrays.asList("new1", "new2");
        Beans.setPropValueByGetter(bean, getItems, newList);

        assertEquals(2, bean.getItems().size());
        assertEquals("new1", bean.getItems().get(0));
        assertEquals("new2", bean.getItems().get(1));

        Beans.setPropValueByGetter(bean, getItems, null);
        assertEquals(2, bean.getItems().size());
    }

    @Test
    public void testFormalizePropName() {
        assertEquals("userName", Beans.formalizePropName("user_name"));
        assertEquals("firstName", Beans.formalizePropName("first_name"));
        assertEquals("clazz", Beans.formalizePropName("class"));
        assertEquals("id", Beans.formalizePropName("ID"));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("userName", Beans.toCamelCase("user_name"));
        assertEquals("firstName", Beans.toCamelCase("FIRST_NAME"));
        assertEquals("addressLine1", Beans.toCamelCase("address-line-1"));
    }

    @Test
    public void testToSnakeCase() {
        assertEquals("user_name", Beans.toSnakeCase("userName"));
        assertEquals("first_name", Beans.toSnakeCase("FirstName"));
        assertEquals("user_id", Beans.toSnakeCase("userID"));
        assertEquals("", Beans.toSnakeCase(""));
        assertNull(Beans.toSnakeCase((String) null));
    }

    @Test
    public void testToScreamingSnakeCase() {
        assertEquals("USER_NAME", Beans.toScreamingSnakeCase("userName"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("firstName"));
        assertEquals("USER_ID", Beans.toScreamingSnakeCase("userID"));
        assertEquals("", Beans.toScreamingSnakeCase(""));
        assertNull(Beans.toScreamingSnakeCase((String) null));
    }

    @Test
    public void testToCamelCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_name", "John");
        map.put("first_name", "Jane");

        Beans.toCamelCaseKeys(map);

        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("firstName"));
        assertFalse(map.containsKey("user_name"));
        assertEquals("John", map.get("userName"));
    }

    @Test
    public void testToSnakeCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");

        Beans.toSnakeCaseKeys(map);

        assertTrue(map.containsKey("user_name"));
        assertTrue(map.containsKey("first_name"));
        assertFalse(map.containsKey("userName"));
    }

    @Test
    public void testToScreamingSnakeCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");

        Beans.toScreamingSnakeCaseKeys(map);

        assertTrue(map.containsKey("USER_NAME"));
        assertTrue(map.containsKey("FIRST_NAME"));
        assertFalse(map.containsKey("userName"));
    }

    @Test
    public void testMap2Bean() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane");
        map.put("age", 30);
        map.put("active", false);

        SimpleBean bean = Beans.mapToBean(map, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(30, bean.getAge());
        assertEquals(false, bean.getActive());

        assertNull(Beans.mapToBean((Map<String, Object>) null, SimpleBean.class));

        map.put("active", null);
        bean = Beans.mapToBean(map, true, true, SimpleBean.class);
        assertNotNull(bean);

        bean = Beans.mapToBean(map, Arrays.asList("name", "age"), SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(30, bean.getAge());
    }

    @Test
    public void testMap2BeanList() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", 25);
        mapList.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Jane");
        map2.put("age", 30);
        mapList.add(map2);

        List<SimpleBean> beans = Beans.mapToBean(mapList, SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals("Jane", beans.get(1).getName());

        beans = Beans.mapToBean(mapList, Arrays.asList("name"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
    }

    @Test
    public void testBean2Map() {
        Map<String, Object> map = Beans.beanToMap(simpleBean);
        assertEquals("John", map.get("name"));
        assertEquals(25, map.get("age"));
        assertEquals(true, map.get("active"));

        TreeMap<String, Object> treeMap = Beans.beanToMap(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);

        map = Beans.beanToMap(simpleBean, Arrays.asList("name", "age"));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("active"));

        map = Beans.beanToMap(simpleBean, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
    }

    @Test
    public void testBean2MapWithFiltering() {
        simpleBean.setActive(null);

        Map<String, Object> map = Beans.beanToMap(simpleBean, true);
        assertFalse(map.containsKey("active"));

        map = Beans.beanToMap(simpleBean, false);
        assertTrue(map.containsKey("active"));
        assertNull(map.get("active"));

        Set<String> ignored = new HashSet<>();
        ignored.add("age");
        map = Beans.beanToMap(simpleBean, true, ignored);
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
        assertTrue(map.containsKey("name"));
    }

    @Test
    public void testDeepBean2Map() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertTrue(map.get("simpleBean") instanceof Map);
        assertTrue(map.get("address") instanceof Map);

        Map<String, Object> simpleBeanMap = (Map<String, Object>) map.get("simpleBean");
        assertEquals("John", simpleBeanMap.get("name"));

        map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("simpleBean"));

        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        snakeBean.setLastName("Doe");

        map = Beans.deepBeanToMap(snakeBean, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("first_name"));
        assertTrue(map.containsKey("last_name"));
    }

    @Test
    public void testDeepBean2MapWithFiltering() {
        nestedBean.setSimpleBean(null);

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true);
        assertFalse(map.containsKey("simpleBean"));

        Set<String> ignored = new HashSet<>();
        ignored.add("id");
        map = Beans.deepBeanToMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testBean2FlatMap() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
        assertEquals(25, map.get("simpleBean.age"));
        assertEquals("New York", map.get("address.city"));
        assertEquals("5th Avenue", map.get("address.street"));

        map = Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"));
        assertEquals("123", map.get("id"));
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));

        map = Beans.beanToFlatMap(nestedBean, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name"));
    }

    @Test
    public void testBean2FlatMapWithFiltering() {
        nestedBean.getAddress().setZipCode(null);

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true);
        assertFalse(map.containsKey("address.zipCode"));

        Set<String> ignored = new HashSet<>();
        ignored.add("id");
        map = Beans.beanToFlatMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testNewBean() {
        SimpleBean bean = Beans.newBean(SimpleBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testClone() {
        SimpleBean cloned = Beans.clone(simpleBean);
        assertNotNull(cloned);
        assertNotSame(simpleBean, cloned);
        assertEquals(simpleBean.getName(), cloned.getName());
        assertEquals(simpleBean.getAge(), cloned.getAge());

        assertNull(Beans.clone(null));

        SimpleBean cloned2 = Beans.clone(simpleBean, SimpleBean.class);
        assertNotNull(cloned2);
        assertEquals(simpleBean.getName(), cloned2.getName());

        SimpleBean fromNull = Beans.clone(null, SimpleBean.class);
        assertNotNull(fromNull);

        assertThrows(IllegalArgumentException.class, () -> Beans.clone(simpleBean, null));
    }

    @Test
    public void testCopy() {
        SimpleBean copied = Beans.copy(simpleBean);
        assertNotNull(copied);
        assertNotSame(simpleBean, copied);
        assertEquals(simpleBean.getName(), copied.getName());
        assertEquals(simpleBean.getAge(), copied.getAge());

        assertNull(Beans.copy(null));

        copied = Beans.copy(simpleBean, Arrays.asList("name"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copy(simpleBean, (name, value) -> !name.equals("age"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        EntityBean entity = new EntityBean();
        entity.setId(100L);
        EntityBean copiedEntity = Beans.copy(entity, EntityBean.class);
        assertEquals(100L, copiedEntity.getId());

        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        copied = Beans.copy(snakeBean, (Collection<String>) null, name -> Beans.toCamelCase(name), SimpleBean.class);
        assertNotNull(copied);
    }

    @Test
    public void testCopyWithIgnoreUnmatched() {
        SimpleBean source = new SimpleBean("John", 25);

        EntityBean target = Beans.copy(source, true, null, EntityBean.class);
        assertNotNull(target);

        SimpleBean target2 = Beans.copy(source, false, null, SimpleBean.class);
        assertEquals("John", target2.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.copy(source, false, null, null));
    }

    @Test
    public void testMerge() {
        {
            SimpleBean source = new SimpleBean("Jane", 30);
            SimpleBean target = new SimpleBean("John", 25);

            Beans.merge(source, target);
            assertEquals("Jane", target.getName());
            assertEquals(30, target.getAge());

            SimpleBean original = new SimpleBean("Bob", 40);
            Beans.merge(null, original);
            assertEquals("Bob", original.getName());
        }

        {

            SimpleBean source = new SimpleBean("Alice", 35);
            SimpleBean target = new SimpleBean("Charlie", 28);
            Beans.merge(source, target, Arrays.asList("age"));
            assertEquals("Charlie", target.getName());
            assertEquals(35, target.getAge());

            source.setAge(10);
            target.setAge(20);
            Beans.merge(source, target, Arrays.asList("age"), (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));
            assertEquals(30, target.getAge());

            assertThrows(IllegalArgumentException.class, () -> Beans.merge(source, null));
        }
    }

    @Test
    public void testMergeWithPropFilter() {
        SimpleBean source = new SimpleBean("Jane", 30);
        source.setActive(true);
        SimpleBean target = new SimpleBean("John", 25);
        target.setActive(false);

        Beans.merge(source, target, Fn.p((name, value) -> value instanceof String));
        assertEquals("Jane", target.getName());
        assertEquals(25, target.getAge());
        assertEquals(false, target.getActive());

        BeanWithSnakeCase snakeSource = new BeanWithSnakeCase();
        snakeSource.setFirstName("NewFirst");
        SimpleBean convertTarget = new SimpleBean();
        Beans.merge(snakeSource, convertTarget, (name, value) -> true, name -> "name");
        assertEquals("NewFirst", convertTarget.getName());
    }

    @Test
    public void testMergeWithIgnoreUnmatched() {
        SimpleBean source = new SimpleBean("Jane", 30);
        EntityBean target = new EntityBean();
        target.setId(1L);

        Beans.merge(source, target, true, null);
        assertEquals(1L, target.getId());

        source.setAge(5);
        SimpleBean target2 = new SimpleBean("John", 10);
        Beans.merge(source, target2, true, null, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        });
        assertEquals(15, target2.getAge());
    }

    @Test
    public void testErase() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.erase(bean, "name", "age");
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(true, bean.getActive());

        bean = new SimpleBean("Jane", 30);
        Beans.erase(bean, Arrays.asList("name"));
        assertNull(bean.getName());
        assertEquals(30, bean.getAge());

        Beans.erase(null, "name");

        bean = new SimpleBean("Bob", 35);
        Beans.erase(bean, new String[0]);
        assertEquals("Bob", bean.getName());
    }

    @Test
    public void testEraseAll() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.eraseAll(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());

        Beans.eraseAll(null);
    }

    @Test
    public void testFill() {
        SimpleBean bean = new SimpleBean();
        Beans.fill(bean);
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNotNull(bean.getActive());

        bean = new SimpleBean();
        Beans.fill(bean, Arrays.asList("name"));
        assertNotNull(bean.getName());
        assertEquals(0, bean.getAge());

        SimpleBean filled = Beans.fill(SimpleBean.class);
        assertNotNull(filled);
        assertNotNull(filled.getName());

        List<SimpleBean> filledList = Beans.fill(SimpleBean.class, 3);
        assertEquals(3, filledList.size());
        for (SimpleBean b : filledList) {
            assertNotNull(b.getName());
        }

        filled = Beans.fill(SimpleBean.class, Arrays.asList("age"));
        assertTrue(filled.getAge() != 0);
        assertNull(filled.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.fill((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.fill((Class<?>) null));
    }

    @Test
    public void testEqualsByProps() {
        SimpleBean bean1 = new SimpleBean("John", 25);
        SimpleBean bean2 = new SimpleBean("John", 30);

        assertTrue(Beans.equalsByProps(bean1, bean2, Arrays.asList("name")));
        assertFalse(Beans.equalsByProps(bean1, bean2, Arrays.asList("age")));
        assertFalse(Beans.equalsByProps(bean1, bean2, Arrays.asList("name", "age")));

        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByProps(bean1, bean2, Collections.emptyList()));
    }

    @Test
    public void testEqualsByCommonProps() {
        SimpleBean bean1 = new SimpleBean("John", 25);
        EntityBean bean2 = new EntityBean();
        bean2.setValue("test");

        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(bean1, bean2));

        class BeanA {
            private String name;
            private int age;

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
        }

        class BeanB {
            private String name;
            private String email;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }
        }

        BeanA a = new BeanA();
        a.setName("John");
        BeanB b = new BeanB();
        b.setName("John");

        assertTrue(Beans.equalsByCommonProps(a, b));

        b.setName("Jane");
        assertFalse(Beans.equalsByCommonProps(a, b));

        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(null, bean1));
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(bean1, null));
    }

    @Test
    public void testCompareByProps() {
        SimpleBean bean1 = new SimpleBean("Alice", 25);
        SimpleBean bean2 = new SimpleBean("Bob", 30);

        assertTrue(Beans.compareByProps(bean1, bean2, Arrays.asList("name")) < 0);
        assertTrue(Beans.compareByProps(bean2, bean1, Arrays.asList("name")) > 0);
        assertTrue(Beans.compareByProps(bean1, bean1, Arrays.asList("name")) == 0);

        assertTrue(Beans.compareByProps(bean1, bean2, Arrays.asList("age")) < 0);

        SimpleBean bean3 = new SimpleBean("Alice", 30);
        assertTrue(Beans.compareByProps(bean1, bean3, Arrays.asList("name", "age")) < 0);

        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(null, bean2, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(bean1, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(bean1, bean2, null));
    }

    @Test
    public void testProperties() {
        List<Map.Entry<String, Object>> props = Beans.propertyEntries(simpleBean).toList();

        assertFalse(props.isEmpty());
        assertTrue(props.stream().anyMatch(e -> "name".equals(e.getKey()) && "John".equals(e.getValue())));
        assertTrue(props.stream().anyMatch(e -> "age".equals(e.getKey()) && Integer.valueOf(25).equals(e.getValue())));

        simpleBean.setActive(null);
        props = Beans.propertyEntries(simpleBean, (name, value) -> value != null).toList();

        assertFalse(props.stream().anyMatch(e -> "active".equals(e.getKey())));

        assertThrows(IllegalArgumentException.class, () -> Beans.propertyEntries(null));
    }
}
