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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.Tuple.Tuple3;

public class Beans100Test extends TestBase {

    // Test classes
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
        // No getters/setters
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

    // Test data
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
        // Just ensure it doesn't throw
        Beans.refreshBeanPropInfo(SimpleBean.class);
    }

    @Test
    public void testGetBuilderInfo() {
        Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> builderInfo = Beans
                .getBuilderInfo(BeanWithBuilder.class);

        assertNotNull(builderInfo);
        assertNotNull(builderInfo._2);
        assertNotNull(builderInfo._3);

        // Test builder creation
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
        // Note: The cache might still return true, but the registration is done
    }

    @Test
    public void testRegisterNonPropGetSetMethod() {
        Beans.registerNonPropGetSetMethod(SimpleBean.class, "internal");
    }

    @Test
    public void testRegisterPropGetSetMethod() throws Exception {
        Method method = SimpleBean.class.getMethod("getName");
        Beans.registerPropGetSetMethod("name", method);

        // Test invalid method
        Method invalidMethod = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropGetSetMethod("invalid", invalidMethod));
    }

    @Test
    public void testRegisterXMLBindingClass() {
        Beans.registerXMLBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXMLBindingClass(SimpleBean.class));
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

        // Test with collection overload
        props = Beans.getPropNames(SimpleBean.class, Arrays.asList("age", "active"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));
        assertFalse(props.contains("active"));

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames(null, Collections.emptySet()));
    }

    @Test
    public void testGetPropNamesFromBean() {
        // Test with ignoreNullValue
        simpleBean.setActive(null);
        List<String> props = Beans.getPropNames(simpleBean, true);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertFalse(props.contains("active"));

        props = Beans.getPropNames(simpleBean, false);
        assertTrue(props.contains("active"));

        // Test with predicate filter
        props = Beans.getPropNames(simpleBean, name -> name.startsWith("a"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));
        assertFalse(props.contains("name"));

        // Test with BiPredicate filter
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

        // Test case-insensitive
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
        Method method = Beans.getPropGetMethod(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("getName", method.getName());

        // Test case variations
        method = Beans.getPropGetMethod(SimpleBean.class, "NAME");
        assertNotNull(method);

        assertNull(Beans.getPropGetMethod(SimpleBean.class, "nonExistent"));
    }

    @Test
    public void testGetPropGetMethods() {
        ImmutableMap<String, Method> methods = Beans.getPropGetMethods(SimpleBean.class);
        assertNotNull(methods);
        assertFalse(methods.isEmpty());
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
    }

    @Test
    public void testGetPropSetMethod() {
        Method method = Beans.getPropSetMethod(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("setName", method.getName());

        assertNull(Beans.getPropSetMethod(SimpleBean.class, "nonExistent"));
    }

    @Test
    public void testGetPropSetMethods() {
        ImmutableMap<String, Method> methods = Beans.getPropSetMethods(SimpleBean.class);
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

        // Test with method
        Method getName = SimpleBean.class.getMethod("getName");
        assertEquals("John", Beans.getPropValue(simpleBean, getName));

        // Test nested property
        assertEquals("New York", Beans.getPropValue(nestedBean, "address.city"));

        // Test with ignoreUnmatchedProperty
        assertNull(Beans.getPropValue(simpleBean, "nonExistent", true));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(simpleBean, "nonExistent", false));
    }

    @Test
    public void testSetPropValue() throws Exception {
        Beans.setPropValue(simpleBean, "name", "Jane");
        assertEquals("Jane", simpleBean.getName());

        Beans.setPropValue(simpleBean, "age", 30);
        assertEquals(30, simpleBean.getAge());

        // Test with method
        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.setPropValue(simpleBean, setName, "Bob");
        assertEquals("Bob", simpleBean.getName());

        // Test with null value
        Beans.setPropValue(simpleBean, "active", null);
        assertNull(simpleBean.getActive());

        // Test with ignoreUnmatchedProperty
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
        Beans.setPropValueByGet(bean, getItems, newList);

        assertEquals(2, bean.getItems().size());
        assertEquals("new1", bean.getItems().get(0));
        assertEquals("new2", bean.getItems().get(1));

        // Test with null
        Beans.setPropValueByGet(bean, getItems, null);
        assertEquals(2, bean.getItems().size()); // No change
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
    public void testToLowerCaseWithUnderscore() {
        assertEquals("user_name", Beans.toLowerCaseWithUnderscore("userName"));
        assertEquals("first_name", Beans.toLowerCaseWithUnderscore("FirstName"));
        assertEquals("user_id", Beans.toLowerCaseWithUnderscore("userID"));
        assertEquals("", Beans.toLowerCaseWithUnderscore(""));
        assertNull(Beans.toLowerCaseWithUnderscore((String) null));
    }

    @Test
    public void testToUpperCaseWithUnderscore() {
        assertEquals("USER_NAME", Beans.toUpperCaseWithUnderscore("userName"));
        assertEquals("FIRST_NAME", Beans.toUpperCaseWithUnderscore("firstName"));
        assertEquals("USER_ID", Beans.toUpperCaseWithUnderscore("userID"));
        assertEquals("", Beans.toUpperCaseWithUnderscore(""));
        assertNull(Beans.toUpperCaseWithUnderscore((String) null));
    }

    @Test
    public void testToCamelCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_name", "John");
        map.put("first_name", "Jane");

        Beans.toCamelCase(map);

        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("firstName"));
        assertFalse(map.containsKey("user_name"));
        assertEquals("John", map.get("userName"));
    }

    @Test
    public void testToLowerCaseWithUnderscoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");

        Beans.toLowerCaseWithUnderscore(map);

        assertTrue(map.containsKey("user_name"));
        assertTrue(map.containsKey("first_name"));
        assertFalse(map.containsKey("userName"));
    }

    @Test
    public void testToUpperCaseWithUnderscoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");

        Beans.toUpperCaseWithUnderscore(map);

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

        SimpleBean bean = Beans.map2Bean(map, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(30, bean.getAge());
        assertEquals(false, bean.getActive());

        // Test with null map
        assertNull(Beans.map2Bean((Map<String, Object>) null, SimpleBean.class));

        // Test with ignoreNullProperty
        map.put("active", null);
        bean = Beans.map2Bean(map, true, true, SimpleBean.class);
        assertNotNull(bean);

        // Test with selectPropNames
        bean = Beans.map2Bean(map, Arrays.asList("name", "age"), SimpleBean.class);
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

        List<SimpleBean> beans = Beans.map2Bean(mapList, SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals("Jane", beans.get(1).getName());

        // Test with selectPropNames
        beans = Beans.map2Bean(mapList, Arrays.asList("name"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge()); // Not selected
    }

    @Test
    public void testBean2Map() {
        Map<String, Object> map = Beans.bean2Map(simpleBean);
        assertEquals("John", map.get("name"));
        assertEquals(25, map.get("age"));
        assertEquals(true, map.get("active"));

        // Test with custom map supplier
        TreeMap<String, Object> treeMap = Beans.bean2Map(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);

        // Test with selectPropNames
        map = Beans.bean2Map(simpleBean, Arrays.asList("name", "age"));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("active"));

        // Test with naming policy
        map = Beans.bean2Map(simpleBean, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
    }

    @Test
    public void testBean2MapWithFiltering() {
        simpleBean.setActive(null);

        // Test ignoreNullProperty
        Map<String, Object> map = Beans.bean2Map(simpleBean, true);
        assertFalse(map.containsKey("active"));

        map = Beans.bean2Map(simpleBean, false);
        assertTrue(map.containsKey("active"));
        assertNull(map.get("active"));

        // Test with ignoredPropNames
        Set<String> ignored = new HashSet<>();
        ignored.add("age");
        map = Beans.bean2Map(simpleBean, true, ignored);
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active")); // null
        assertTrue(map.containsKey("name"));
    }

    @Test
    public void testDeepBean2Map() {
        Map<String, Object> map = Beans.deepBean2Map(nestedBean);
        assertEquals("123", map.get("id"));
        assertTrue(map.get("simpleBean") instanceof Map);
        assertTrue(map.get("address") instanceof Map);

        Map<String, Object> simpleBeanMap = (Map<String, Object>) map.get("simpleBean");
        assertEquals("John", simpleBeanMap.get("name"));

        // Test with selectPropNames
        map = Beans.deepBean2Map(nestedBean, Arrays.asList("id", "address"));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("simpleBean"));

        // Test with naming policy
        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        snakeBean.setLastName("Doe");

        map = Beans.deepBean2Map(snakeBean, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("first_name"));
        assertTrue(map.containsKey("last_name"));
    }

    @Test
    public void testDeepBean2MapWithFiltering() {
        nestedBean.setSimpleBean(null);

        // Test ignoreNullProperty
        Map<String, Object> map = Beans.deepBean2Map(nestedBean, true);
        assertFalse(map.containsKey("simpleBean"));

        // Test with ignoredPropNames
        Set<String> ignored = new HashSet<>();
        ignored.add("id");
        map = Beans.deepBean2Map(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testBean2FlatMap() {
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
        assertEquals(25, map.get("simpleBean.age"));
        assertEquals("New York", map.get("address.city"));
        assertEquals("5th Avenue", map.get("address.street"));

        // Test with selectPropNames
        map = Beans.bean2FlatMap(nestedBean, Arrays.asList("id", "address"));
        assertEquals("123", map.get("id"));
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));

        // Test with naming policy
        map = Beans.bean2FlatMap(nestedBean, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name"));
    }

    @Test
    public void testBean2FlatMapWithFiltering() {
        nestedBean.getAddress().setZipCode(null);

        // Test ignoreNullProperty
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, true);
        assertFalse(map.containsKey("address.zipCode"));

        // Test with ignoredPropNames
        Set<String> ignored = new HashSet<>();
        ignored.add("id");
        map = Beans.bean2FlatMap(nestedBean, true, ignored);
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

        // Test with null
        assertNull(Beans.clone(null));

        // Test with target type
        SimpleBean cloned2 = Beans.clone(simpleBean, SimpleBean.class);
        assertNotNull(cloned2);
        assertEquals(simpleBean.getName(), cloned2.getName());

        // Test cloning null with target type
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

        // Test with null
        assertNull(Beans.copy(null));

        // Test with selectPropNames
        copied = Beans.copy(simpleBean, Arrays.asList("name"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge()); // Not selected

        // Test with propFilter
        copied = Beans.copy(simpleBean, (name, value) -> !name.equals("age"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge()); // Filtered out

        // Test with target type
        EntityBean entity = new EntityBean();
        entity.setId(100L);
        EntityBean copiedEntity = Beans.copy(entity, EntityBean.class);
        assertEquals(100L, copiedEntity.getId());

        // Test with propNameConverter
        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        copied = Beans.copy(snakeBean, (Collection<String>) null, name -> Beans.toCamelCase(name), SimpleBean.class);
        assertNotNull(copied);
    }

    @Test
    public void testCopyWithIgnoreUnmatched() {
        SimpleBean source = new SimpleBean("John", 25);

        // Test with ignoreUnmatchedProperty = true
        EntityBean target = Beans.copy(source, true, null, EntityBean.class);
        assertNotNull(target);

        // Test with ignoreUnmatchedProperty = false (should work if no unmatched)
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

            // Test with null source
            SimpleBean original = new SimpleBean("Bob", 40);
            Beans.merge(null, original);
            assertEquals("Bob", original.getName());
        }

        {

            // Test with selectPropNames
            SimpleBean source = new SimpleBean("Alice", 35);
            SimpleBean target = new SimpleBean("Charlie", 28);
            Beans.merge(source, target, Arrays.asList("age"));
            assertEquals("Charlie", target.getName()); // Not selected
            assertEquals(35, target.getAge());

            // Test with mergeFunc
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

        // Merge only string properties
        Beans.merge(source, target, Fn.p((name, value) -> value instanceof String));
        assertEquals("Jane", target.getName());
        assertEquals(25, target.getAge()); // Not merged
        assertEquals(false, target.getActive()); // Not merged

        // Test with propNameConverter
        BeanWithSnakeCase snakeSource = new BeanWithSnakeCase();
        snakeSource.setFirstName("NewFirst");
        SimpleBean convertTarget = new SimpleBean();
        Beans.merge(snakeSource, convertTarget, (name, value) -> true, name -> "name"); // Map all to "name"
        assertEquals("NewFirst", convertTarget.getName());
    }

    @Test
    public void testMergeWithIgnoreUnmatched() {
        SimpleBean source = new SimpleBean("Jane", 30);
        EntityBean target = new EntityBean();
        target.setId(1L);

        // Test with ignoreUnmatchedProperty = true
        Beans.merge(source, target, true, null);
        assertEquals(1L, target.getId()); // Unchanged

        // Test with custom merge function
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
        assertEquals(true, bean.getActive()); // Not erased

        // Test with collection
        bean = new SimpleBean("Jane", 30);
        Beans.erase(bean, Arrays.asList("name"));
        assertNull(bean.getName());
        assertEquals(30, bean.getAge());

        // Test with null bean
        Beans.erase(null, "name"); // Should not throw

        // Test with empty propNames
        bean = new SimpleBean("Bob", 35);
        Beans.erase(bean, new String[0]);
        assertEquals("Bob", bean.getName()); // Unchanged
    }

    @Test
    public void testEraseAll() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.eraseAll(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());

        // Test with null
        Beans.eraseAll(null); // Should not throw
    }

    @Test
    public void testFill() {
        SimpleBean bean = new SimpleBean();
        Beans.fill(bean);
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNotNull(bean.getActive());

        // Test with selectPropNames
        bean = new SimpleBean();
        Beans.fill(bean, Arrays.asList("name"));
        assertNotNull(bean.getName());
        assertEquals(0, bean.getAge()); // Not filled

        // Test creating filled instance
        SimpleBean filled = Beans.fill(SimpleBean.class);
        assertNotNull(filled);
        assertNotNull(filled.getName());

        // Test creating multiple filled instances
        List<SimpleBean> filledList = Beans.fill(SimpleBean.class, 3);
        assertEquals(3, filledList.size());
        for (SimpleBean b : filledList) {
            assertNotNull(b.getName());
        }

        // Test with selectPropNames for class
        filled = Beans.fill(SimpleBean.class, Arrays.asList("age"));
        assertTrue(filled.getAge() != 0);
        assertNull(filled.getName()); // Not filled

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

        // No common properties except from Object class
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(bean1, bean2));

        // Test with beans having common properties
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

        // Test multiple properties
        SimpleBean bean3 = new SimpleBean("Alice", 30);
        assertTrue(Beans.compareByProps(bean1, bean3, Arrays.asList("name", "age")) < 0);

        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(null, bean2, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(bean1, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(bean1, bean2, null));
    }

    @Test
    public void testProperties() {
        List<Map.Entry<String, Object>> props = Beans.properties(simpleBean).toList();

        assertFalse(props.isEmpty());
        assertTrue(props.stream().anyMatch(e -> "name".equals(e.getKey()) && "John".equals(e.getValue())));
        assertTrue(props.stream().anyMatch(e -> "age".equals(e.getKey()) && Integer.valueOf(25).equals(e.getValue())));

        // Test with filter
        simpleBean.setActive(null);
        props = Beans.properties(simpleBean, (name, value) -> value != null).toList();

        assertFalse(props.stream().anyMatch(e -> "active".equals(e.getKey())));

        assertThrows(IllegalArgumentException.class, () -> Beans.properties(null));
    }
}