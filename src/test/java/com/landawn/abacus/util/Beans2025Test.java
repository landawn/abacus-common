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
import java.util.LinkedHashMap;
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

@Tag("2025")
public class Beans2025Test extends TestBase {

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
        private List<String> tags;

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

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
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
        @DiffIgnore
        private String internalFlag;

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

        public String getInternalFlag() {
            return internalFlag;
        }

        public void setInternalFlag(String internalFlag) {
            this.internalFlag = internalFlag;
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
        private String userID;

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

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }
    }

    public static class CollectionBean {
        private List<String> items;

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
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

        BeanInfo beanInfo = Beans.getBeanInfo(SimpleBean.class);
        assertNotNull(beanInfo);
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
        Beans.registerNonPropGetSetMethod(SimpleBean.class, "internal");
    }

    @Test
    public void testRegisterPropGetSetMethod() throws Exception {
        Method method = SimpleBean.class.getMethod("getName");
        Beans.registerPropGetSetMethod("name", method);

        Method invalidMethod = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropGetSetMethod("invalid", invalidMethod));
    }

    @Test
    public void testRegisterXMLBindingClass() {
        Beans.registerXMLBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXMLBindingClass(SimpleBean.class));
    }

    @Test
    public void testIsRegisteredXMLBindingClass() {
        Beans.registerXMLBindingClass(EntityBean.class);
        assertTrue(Beans.isRegisteredXMLBindingClass(EntityBean.class));
        assertFalse(Beans.isRegisteredXMLBindingClass(Address.class));
    }

    @Test
    public void testGetPropNameByMethod() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        assertEquals("name", Beans.getPropNameByMethod(getName));

        Method getAge = SimpleBean.class.getMethod("getAge");
        assertEquals("age", Beans.getPropNameByMethod(getAge));

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        assertEquals("name", Beans.getPropNameByMethod(setName));

        Method getActive = SimpleBean.class.getMethod("getActive");
        assertEquals("active", Beans.getPropNameByMethod(getActive));
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
    public void testGetPropNamesWithSetExclusion() {
        Set<String> exclusions = new HashSet<>();
        exclusions.add("age");
        exclusions.add("active");

        List<String> props = Beans.getPropNames(SimpleBean.class, exclusions);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));
        assertFalse(props.contains("active"));
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
        assertTrue(ignored.contains("internalFlag"));
        assertFalse(ignored.contains("name"));
        assertEquals(2, ignored.size());
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
        Method method = Beans.getPropGetMethod(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("getName", method.getName());

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

        CollectionBean bean = new CollectionBean();
        bean.setItems(list);

        Method getItems = CollectionBean.class.getMethod("getItems");

        List<String> newList = Arrays.asList("new1", "new2");
        Beans.setPropValueByGet(bean, getItems, newList);

        assertEquals(2, bean.getItems().size());
        assertEquals("new1", bean.getItems().get(0));
        assertEquals("new2", bean.getItems().get(1));

        Beans.setPropValueByGet(bean, getItems, null);
        assertEquals(2, bean.getItems().size());
    }

    @Test
    public void testFormalizePropName() {
        assertEquals("userName", Beans.formalizePropName("user_name"));
        assertEquals("firstName", Beans.formalizePropName("first_name"));
        assertEquals("clazz", Beans.formalizePropName("class"));
        assertEquals("id", Beans.formalizePropName("ID"));
        assertEquals("addressLine1", Beans.formalizePropName("address_line_1"));
    }

    @Test
    public void testToCamelCase() {
        assertEquals("userName", Beans.toCamelCase("user_name"));
        assertEquals("firstName", Beans.toCamelCase("FIRST_NAME"));
        assertEquals("addressLine1", Beans.toCamelCase("address-line-1"));
        assertEquals("id", Beans.toCamelCase("id"));
        assertEquals("userId", Beans.toCamelCase("user_id"));
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

        assertNull(Beans.map2Bean((Map<String, Object>) null, SimpleBean.class));
    }

    @Test
    public void testMap2BeanWithOptions() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane");
        map.put("age", 30);
        map.put("active", null);

        SimpleBean bean = Beans.map2Bean(map, true, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(30, bean.getAge());
        assertNull(bean.getActive());

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

        beans = Beans.map2Bean(mapList, Arrays.asList("name"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
    }

    @Test
    public void testMap2BeanListWithOptions() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", null);
        mapList.add(map1);

        List<SimpleBean> beans = Beans.map2Bean(mapList, true, true, SimpleBean.class);
        assertEquals(1, beans.size());
        assertEquals("John", beans.get(0).getName());
    }

    @Test
    public void testBean2Map() {
        Map<String, Object> map = Beans.bean2Map(simpleBean);
        assertEquals("John", map.get("name"));
        assertEquals(25, map.get("age"));
        assertEquals(true, map.get("active"));

        TreeMap<String, Object> treeMap = Beans.bean2Map(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);

        map = Beans.bean2Map(simpleBean, Arrays.asList("name", "age"));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("active"));

        map = Beans.bean2Map(simpleBean, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
    }

    @Test
    public void testBean2MapWithOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2Map(simpleBean, output);

        assertEquals("John", output.get("name"));
        assertEquals(25, output.get("age"));
        assertEquals(true, output.get("active"));

        output.clear();
        Beans.bean2Map(simpleBean, Arrays.asList("name"), output);
        assertEquals(1, output.size());
        assertTrue(output.containsKey("name"));
    }

    @Test
    public void testBean2MapWithFiltering() {
        simpleBean.setActive(null);

        Map<String, Object> map = Beans.bean2Map(simpleBean, true);
        assertFalse(map.containsKey("active"));

        map = Beans.bean2Map(simpleBean, false);
        assertTrue(map.containsKey("active"));
        assertNull(map.get("active"));

        Set<String> ignored = new HashSet<>();
        ignored.add("age");
        map = Beans.bean2Map(simpleBean, true, ignored);
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
        assertTrue(map.containsKey("name"));
    }

    @Test
    public void testBean2MapWithIgnoredAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.bean2Map(simpleBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());

        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
    }

    @Test
    public void testDeepBean2Map() {
        Map<String, Object> map = Beans.deepBean2Map(nestedBean);
        assertEquals("123", map.get("id"));
        assertTrue(map.get("simpleBean") instanceof Map);
        assertTrue(map.get("address") instanceof Map);

        Map<String, Object> simpleBeanMap = (Map<String, Object>) map.get("simpleBean");
        assertEquals("John", simpleBeanMap.get("name"));

        map = Beans.deepBean2Map(nestedBean, Arrays.asList("id", "address"));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("simpleBean"));

        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        snakeBean.setLastName("Doe");

        map = Beans.deepBean2Map(snakeBean, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("first_name"));
        assertTrue(map.containsKey("last_name"));
    }

    @Test
    public void testDeepBean2MapWithOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBean2Map(nestedBean, output);

        assertEquals("123", output.get("id"));
        assertTrue(output.get("simpleBean") instanceof Map);

        output.clear();
        Beans.deepBean2Map(nestedBean, Arrays.asList("id"), output);
        assertEquals(1, output.size());
    }

    @Test
    public void testDeepBean2MapWithFiltering() {
        nestedBean.setSimpleBean(null);

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, true);
        assertFalse(map.containsKey("simpleBean"));

        Set<String> ignored = new HashSet<>();
        ignored.add("id");
        nestedBean.setSimpleBean(simpleBean);
        map = Beans.deepBean2Map(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());

        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean"));
    }

    @Test
    public void testBean2FlatMap() {
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
        assertEquals(25, map.get("simpleBean.age"));
        assertEquals("New York", map.get("address.city"));
        assertEquals("5th Avenue", map.get("address.street"));

        map = Beans.bean2FlatMap(nestedBean, Arrays.asList("id", "address"));
        assertEquals("123", map.get("id"));
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));

        map = Beans.bean2FlatMap(nestedBean, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name"));
    }

    @Test
    public void testBean2FlatMapWithOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, output);

        assertEquals("123", output.get("id"));
        assertEquals("John", output.get("simpleBean.name"));

        output.clear();
        Beans.bean2FlatMap(nestedBean, Arrays.asList("id"), output);
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testBean2FlatMapWithFiltering() {
        nestedBean.getAddress().setZipCode(null);

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, true);
        assertFalse(map.containsKey("address.zipCode"));

        Set<String> ignored = new HashSet<>();
        ignored.add("id");
        map = Beans.bean2FlatMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());

        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name"));
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
    public void testCopyWithPropFilter() {
        SimpleBean copied = Beans.copy(simpleBean, (name, value) -> value instanceof String);
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copy(simpleBean, (name, value) -> true, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
    }

    @Test
    public void testCopyWithPropFilterAndConverter() {
        SimpleBean copied = Beans.copy(simpleBean, (name, value) -> true, name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
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
    public void testMergeWithConverter() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);

        Beans.merge(source, target, name -> name, (srcVal, tgtVal) -> srcVal);
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());
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
    public void testMergeWithPropFilterConverterAndMergeFuncSimple() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);

        Beans.merge(source, target, (name, value) -> true, name -> name, (srcVal, tgtVal) -> srcVal);
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());
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
    public void testMergeWithSelectPropNamesAndConverter() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);

        Beans.merge(source, target, Arrays.asList("name"), name -> name, (a, b) -> a);
        assertEquals("Jane", target.getName());
        assertEquals(25, target.getAge());
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

        assertThrows(IllegalArgumentException.class, () -> Beans.fill((Object) null));
    }

    @Test
    public void testFillClass() {
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

        assertThrows(IllegalArgumentException.class, () -> Beans.fill((Class<?>) null));
    }

    @Test
    public void testFillClassWithCount() {
        List<SimpleBean> filledList = Beans.fill(SimpleBean.class, Arrays.asList("name", "age"), 2);
        assertEquals(2, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean.getName());
            assertTrue(bean.getAge() != 0);
        }
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
        List<Map.Entry<String, Object>> props = Beans.properties(simpleBean).toList();

        assertFalse(props.isEmpty());
        assertTrue(props.stream().anyMatch(e -> "name".equals(e.getKey()) && "John".equals(e.getValue())));
        assertTrue(props.stream().anyMatch(e -> "age".equals(e.getKey()) && Integer.valueOf(25).equals(e.getValue())));

        simpleBean.setActive(null);
        props = Beans.properties(simpleBean, (name, value) -> value != null).toList();

        assertFalse(props.stream().anyMatch(e -> "active".equals(e.getKey())));

        assertThrows(IllegalArgumentException.class, () -> Beans.properties(null));
    }

    @Test
    public void testPropertiesWithFilter() {
        List<Map.Entry<String, Object>> props = Beans.properties(simpleBean, (name, value) -> value instanceof String).toList();

        assertEquals(1, props.size());
        assertEquals("name", props.get(0).getKey());
        assertEquals("John", props.get(0).getValue());
    }

    @Test
    public void testMap2BeanWithIgnoreOptions() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane");
        map.put("age", null);
        map.put("unknownField", "value");

        SimpleBean bean = Beans.map2Bean(map, false, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertNull(bean.getActive());

        map.put("age", 25);
        bean = Beans.map2Bean(map, true, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(25, bean.getAge());
    }

    @Test
    public void testBean2MapWithMapSupplier() {
        TreeMap<String, Object> treeMap = Beans.bean2Map(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("John", treeMap.get("name"));
        assertEquals(25, treeMap.get("age"));

        Map<String, Object> map = Beans.bean2Map(simpleBean, Arrays.asList("name", "age"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE,
                IntFunctions.ofLinkedHashMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithNamingPolicyAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2Map(simpleBean, Arrays.asList("name", "age"), NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, output);

        assertTrue(output.containsKey("NAME") || output.containsKey("name"));
        assertTrue(output.containsKey("AGE") || output.containsKey("age"));
    }

    @Test
    public void testBean2MapWithIgnoredPropsAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.bean2Map(simpleBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());

        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullAndOutput() {
        simpleBean.setActive(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.bean2Map(simpleBean, true, output);
        assertFalse(output.containsKey("active"));
        assertTrue(output.containsKey("name"));

        output.clear();
        Set<String> ignored = new HashSet<>();
        ignored.add("age");
        Beans.bean2Map(simpleBean, true, ignored, output);
        assertFalse(output.containsKey("age"));
        assertFalse(output.containsKey("active"));
        assertTrue(output.containsKey("name"));
    }

    @Test
    public void testBean2MapWithIgnoredPropsNamingAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2Map(simpleBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);

        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
        assertTrue(output.containsKey("active"));
    }

    @Test
    public void testDeepBean2MapWithMapSupplier() {
        TreeMap<String, Object> treeMap = Beans.deepBean2Map(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("123", treeMap.get("id"));
        assertTrue(treeMap.get("simpleBean") instanceof Map);
    }

    @Test
    public void testDeepBean2MapWithSelectPropsAndSupplier() {
        Map<String, Object> map = Beans.deepBean2Map(nestedBean, Arrays.asList("id", "address"), IntFunctions.ofLinkedHashMap());
        assertEquals(2, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
        assertFalse(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsNamingAndSupplier() {
        Map<String, Object> map = Beans.deepBean2Map(nestedBean, Arrays.asList("id", "address"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBean2Map(nestedBean, Arrays.asList("id", "simpleBean"), output);
        assertEquals(2, output.size());
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsNamingAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBean2Map(nestedBean, Arrays.asList("id"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, true, ignored, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsNamingAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean") || map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullAndOutput() {
        nestedBean.setSimpleBean(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.deepBean2Map(nestedBean, true, output);
        assertFalse(output.containsKey("simpleBean"));
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBean2Map(nestedBean, false, ignored, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsNamingAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBean2Map(nestedBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.size() > 0);
    }

    @Test
    public void testBean2FlatMapWithMapSupplier() {
        TreeMap<String, Object> treeMap = Beans.bean2FlatMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("123", treeMap.get("id"));
        assertEquals("John", treeMap.get("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndSupplier() {
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, Arrays.asList("id", "address"), IntFunctions.ofLinkedHashMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndSupplier() {
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address.city") || map.containsKey("address.CITY"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, Arrays.asList("id", "address"), output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, Arrays.asList("id"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, true, ignored, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsNamingAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name") || map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullAndOutput() {
        nestedBean.getAddress().setZipCode(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.bean2FlatMap(nestedBean, true, output);
        assertFalse(output.containsKey("address.zipCode"));
        assertTrue(output.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, false, ignored, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsNamingAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.size() > 0);
    }

    @Test
    public void testCloneWithTargetType() {
        SimpleBean cloned = Beans.clone(simpleBean, SimpleBean.class);
        assertNotNull(cloned);
        assertNotSame(simpleBean, cloned);
        assertEquals("John", cloned.getName());
        assertEquals(25, cloned.getAge());

        SimpleBean fromNull = Beans.clone(null, SimpleBean.class);
        assertNotNull(fromNull);
        assertNull(fromNull.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.clone(simpleBean, null));
    }

    @Test
    public void testCopyWithSelectPropNames() {
        SimpleBean copied = Beans.copy(simpleBean, Arrays.asList("name"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copy(simpleBean, Collections.emptyList());
        assertEquals(0, copied.getAge());
    }

    @Test
    public void testCopyWithPropNameConverter() {
        SimpleBean copied = Beans.copy(simpleBean, (Collection<String>) null, name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
    }

    @Test
    public void testCopyWithSelectPropsAndConverter() {
        SimpleBean copied = Beans.copy(simpleBean, Arrays.asList("name", "age"), name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
    }

    @Test
    public void testMergeWithNullSource() {
        SimpleBean target = new SimpleBean("Jane", 30);
        Beans.merge(null, target);
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithMergeFuncOnly() {
        SimpleBean source = new SimpleBean("John", 10);
        SimpleBean target = new SimpleBean("Jane", 20);

        Beans.merge(source, target, Fn.o((srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        }));

        assertEquals("John", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithSelectPropsAndMergeFunc() {
        SimpleBean source = new SimpleBean("Alice", 5);
        SimpleBean target = new SimpleBean("Bob", 10);

        Beans.merge(source, target, Arrays.asList("age"), (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        });

        assertEquals("Bob", target.getName());
        assertEquals(15, target.getAge());
    }

    @Test
    public void testMergeWithSelectPropsConverterAndMergeFunc() {
        SimpleBean source = new SimpleBean("John", 25);
        SimpleBean target = new SimpleBean("Jane", 30);

        Beans.merge(source, target, Arrays.asList("name"), name -> name, (a, b) -> a);
        assertEquals("John", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithPropFilterAndConverter() {
        SimpleBean source = new SimpleBean("John", 25);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Jane", 30);
        target.setActive(false);

        Beans.merge(source, target, (name, value) -> value instanceof String, name -> name, (a, b) -> a);

        assertEquals("John", target.getName());
        assertEquals(30, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeWithPropFilterConverterAndMergeFunc() {
        SimpleBean source = new SimpleBean("Alice", 10);
        SimpleBean target = new SimpleBean("Bob", 20);

        Beans.merge(source, target, (name, value) -> value instanceof Integer, name -> name, (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));

        assertEquals("Bob", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testEraseWithCollection() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.erase(bean, Arrays.asList("name", "age"));
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(true, bean.getActive());
    }

    @Test
    public void testEraseWithNullBean() {
        Beans.erase(null, "name");
        Beans.erase(null, Arrays.asList("name"));
    }

    @Test
    public void testFillWithSelectProps() {
        SimpleBean bean = new SimpleBean();
        Beans.fill(bean, Arrays.asList("name", "age"));
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNull(bean.getActive());
    }

    @Test
    public void testFillClassWithSelectProps() {
        SimpleBean filled = Beans.fill(SimpleBean.class, Arrays.asList("name"));
        assertNotNull(filled.getName());
        assertEquals(0, filled.getAge());
        assertNull(filled.getActive());
    }

    @Test
    public void testFillClassWithSelectPropsAndCount() {
        List<SimpleBean> filledList = Beans.fill(SimpleBean.class, Arrays.asList("name", "age"), 3);
        assertEquals(3, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean.getName());
            assertTrue(bean.getAge() != 0);
            assertNull(bean.getActive());
        }
    }

    @Test
    public void testEqualsByPropsEdgeCases() {
        SimpleBean bean1 = new SimpleBean("John", 25);
        SimpleBean bean2 = new SimpleBean("John", 25);

        assertTrue(Beans.equalsByProps(bean1, bean2, Arrays.asList("name", "age")));

        bean1.setActive(null);
        bean2.setActive(null);
        assertTrue(Beans.equalsByProps(bean1, bean2, Arrays.asList("name", "age", "active")));

        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByProps(bean1, bean2, Collections.emptyList()));
    }

    @Test
    public void testCompareByPropsEdgeCases() {
        SimpleBean bean1 = new SimpleBean("Alice", 25);
        SimpleBean bean2 = new SimpleBean("Bob", 25);
        SimpleBean bean3 = new SimpleBean("Alice", 30);

        assertTrue(Beans.compareByProps(bean1, bean2, Arrays.asList("name")) < 0);

        assertTrue(Beans.compareByProps(bean1, bean3, Arrays.asList("name", "age")) < 0);

        assertEquals(0, Beans.compareByProps(bean1, bean1, Arrays.asList("name", "age")));

        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(null, bean2, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(bean1, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> Beans.compareByProps(bean1, bean2, null));
    }

    @Test
    public void testPropertiesEdgeCases() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(null);

        List<Map.Entry<String, Object>> allProps = Beans.properties(bean).toList();
        assertTrue(allProps.size() >= 3);

        List<Map.Entry<String, Object>> filteredByName = Beans.properties(bean, (name, value) -> name.startsWith("n")).toList();
        assertEquals(1, filteredByName.size());
        assertEquals("name", filteredByName.get(0).getKey());

        List<Map.Entry<String, Object>> nonNulls = Beans.properties(bean, (name, value) -> value != null).toList();
        assertFalse(nonNulls.stream().anyMatch(e -> e.getValue() == null));

        assertThrows(IllegalArgumentException.class, () -> Beans.properties(null));
    }

    @Test
    public void testMap2BeanListWithSelectPropsAndOptions() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", 25);
        map1.put("active", true);
        mapList.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Jane");
        map2.put("age", null);
        map2.put("active", false);
        mapList.add(map2);

        List<SimpleBean> beans = Beans.map2Bean(mapList, Arrays.asList("name", "age"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(25, beans.get(0).getAge());
        assertNull(beans.get(0).getActive());

        assertEquals("Jane", beans.get(1).getName());
        assertNull(beans.get(1).getActive());
    }

    @Test
    public void testNullHandlingInConversions() {
        SimpleBean bean = Beans.map2Bean((Map<String, Object>) null, SimpleBean.class);
        assertNull(bean);

        Map<String, Object> map = Beans.bean2Map((Object) null);
        assertTrue(map.isEmpty());

        map = Beans.deepBean2Map((Object) null);
        assertTrue(map.isEmpty());

        map = Beans.bean2FlatMap((Object) null);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testComplexNestedBeanConversions() {
        NestedBean nested = new NestedBean();
        nested.setId("test123");

        SimpleBean simple = new SimpleBean("Nested", 40);
        simple.setActive(true);
        nested.setSimpleBean(simple);

        Address addr = new Address("Boston");
        addr.setStreet("Main St");
        addr.setZipCode("02101");
        nested.setAddress(addr);

        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");
        nested.setTags(tags);

        Map<String, Object> deepMap = Beans.deepBean2Map(nested);
        assertEquals("test123", deepMap.get("id"));
        assertTrue(deepMap.get("simpleBean") instanceof Map);
        assertTrue(deepMap.get("address") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> simpleMap = (Map<String, Object>) deepMap.get("simpleBean");
        assertEquals("Nested", simpleMap.get("name"));
        assertEquals(40, simpleMap.get("age"));

        Map<String, Object> flatMap = Beans.bean2FlatMap(nested);
        assertEquals("test123", flatMap.get("id"));
        assertEquals("Nested", flatMap.get("simpleBean.name"));
        assertEquals(40, flatMap.get("simpleBean.age"));
        assertEquals("Boston", flatMap.get("address.city"));
        assertEquals("Main St", flatMap.get("address.street"));
    }

    @Test
    public void testBeanFieldAccessWithoutGetterSetter() {
        CollectionBean bean = new CollectionBean();
        List<String> items = Arrays.asList("item1", "item2");
        bean.setItems(items);

        Map<String, Object> map = Beans.bean2Map(bean);
        assertNotNull(map.get("items"));

        @SuppressWarnings("unchecked")
        List<String> mappedItems = (List<String>) map.get("items");
        assertEquals(2, mappedItems.size());
    }

    @Test
    public void testNamingPolicyTransformations() {
        BeanWithSnakeCase bean = new BeanWithSnakeCase();
        bean.setFirstName("John");
        bean.setLastName("Doe");
        bean.setUserID("user123");

        Map<String, Object> lowerMap = Beans.bean2Map(bean, null, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(lowerMap.containsKey("first_name") || lowerMap.containsKey("firstName"));
        assertTrue(lowerMap.containsKey("last_name") || lowerMap.containsKey("lastName"));

        Map<String, Object> upperMap = Beans.bean2Map(bean, null, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(upperMap.size() > 0);
    }

    @Test
    public void testCopyWithDifferentTypes() {
        SimpleBean source = new SimpleBean("Transfer", 50);
        source.setActive(true);

        SimpleBean target = Beans.copy(source, SimpleBean.class);
        assertEquals("Transfer", target.getName());
        assertEquals(50, target.getAge());

        SimpleBean filtered = Beans.copy(source, (name, value) -> value instanceof String, SimpleBean.class);
        assertEquals("Transfer", filtered.getName());
        assertEquals(0, filtered.getAge());
    }

    @Test
    public void testMergeWithDifferentBeanTypes() {
        SimpleBean source = new SimpleBean("John", 25);
        EntityBean target = new EntityBean();
        target.setId(1L);
        target.setValue("original");

        Beans.merge(source, target, true, null);
        assertEquals(1L, target.getId());
        assertEquals("original", target.getValue());
    }

    @Test
    public void testFillWithNullBeanShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> Beans.fill((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.fill((Class<?>) null));
    }

    @Test
    public void testGetPropValueWithNestedNullProperty() {
        NestedBean bean = new NestedBean();
        bean.setId("123");
        bean.setSimpleBean(null);

        Object value = Beans.getPropValue(bean, "simpleBean.name", true);
        assertNull(value);
    }

    @Test
    public void testGetPropNamesWithSetOfExclusionsVariants() {
        Set<String> treeSet = new java.util.TreeSet<>();
        treeSet.add("age");
        List<String> props = Beans.getPropNames(SimpleBean.class, treeSet);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));

        props = Beans.getPropNames(SimpleBean.class, (Set<String>) null);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
    }

    @Test
    public void testGetPropNamesWithPredicateFilter() {
        List<String> props = Beans.getPropNames(simpleBean, name -> name.startsWith("a"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));
        assertFalse(props.contains("name"));

        props = Beans.getPropNames(simpleBean, name -> false);
        assertTrue(props.isEmpty());

        props = Beans.getPropNames(simpleBean, name -> true);
        assertTrue(props.size() >= 3);
    }

    @Test
    public void testGetPropNamesWithBiPredicateFilter() {
        List<String> stringProps = Beans.getPropNames(simpleBean, (name, value) -> value instanceof String);
        assertTrue(stringProps.contains("name"));
        assertFalse(stringProps.contains("age"));

        simpleBean.setActive(null);
        List<String> nonNullProps = Beans.getPropNames(simpleBean, (name, value) -> value != null);
        assertFalse(nonNullProps.contains("active"));
        assertTrue(nonNullProps.contains("name"));

        List<String> complexFilter = Beans.getPropNames(simpleBean, (name, value) -> name.length() > 3 && value != null);
        assertTrue(complexFilter.contains("name"));
        assertFalse(complexFilter.contains("age"));
    }

    @Test
    public void testGetPropFieldWithCaseSensitivity() {
        Field field = Beans.getPropField(SimpleBean.class, "name");
        assertNotNull(field);
        assertEquals("name", field.getName());

        field = Beans.getPropField(SimpleBean.class, "NAME");
        assertNotNull(field);

        field = Beans.getPropField(SimpleBean.class, "NaMe");
        assertNotNull(field);

        assertNull(Beans.getPropField(SimpleBean.class, "nonExistent"));
    }

    @Test
    public void testGetPropGetMethodWithCaseVariations() {
        Method method = Beans.getPropGetMethod(SimpleBean.class, "NAME");
        assertNotNull(method);
        assertEquals("getName", method.getName());

        method = Beans.getPropGetMethod(SimpleBean.class, "NaMe");
        assertNotNull(method);

        method = Beans.getPropGetMethod(SimpleBean.class, "name");
        assertNotNull(method);
    }

    @Test
    public void testGetPropSetMethodWithCaseVariations() {
        Method method = Beans.getPropSetMethod(SimpleBean.class, "NAME");
        assertNotNull(method);
        assertEquals("setName", method.getName());

        method = Beans.getPropSetMethod(SimpleBean.class, "AgE");
        assertNotNull(method);

        method = Beans.getPropSetMethod(SimpleBean.class, "active");
        assertNotNull(method);
    }

    @Test
    public void testSetPropValueWithMethodOverload() throws Exception {
        SimpleBean bean = new SimpleBean();

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.setPropValue(bean, setName, "TestName");
        assertEquals("TestName", bean.getName());

        Beans.setPropValue(bean, setName, null);
        assertNull(bean.getName());

        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        Beans.setPropValue(bean, setAge, 42);
        assertEquals(42, bean.getAge());
    }

    @Test
    public void testSetPropValueWithIgnoreUnmatchedProperty() {
        SimpleBean bean = new SimpleBean();

        boolean result = Beans.setPropValue(bean, "name", "Test", false);
        assertTrue(result);
        assertEquals("Test", bean.getName());

        result = Beans.setPropValue(bean, "nonExistent", "value", true);
        assertFalse(result);

        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(bean, "nonExistent", "value", false));
    }

    @Test
    public void testSetPropValueByGetWithCollections() throws Exception {
        CollectionBean bean = new CollectionBean();
        List<String> initial = new ArrayList<>();
        initial.add("item1");
        bean.setItems(initial);

        Method getItems = CollectionBean.class.getMethod("getItems");

        List<String> newItems = Arrays.asList("new1", "new2", "new3");
        Beans.setPropValueByGet(bean, getItems, newItems);

        assertEquals(3, bean.getItems().size());
        assertTrue(bean.getItems().contains("new1"));
        assertTrue(bean.getItems().contains("new2"));
        assertTrue(bean.getItems().contains("new3"));

        Beans.setPropValueByGet(bean, getItems, Collections.emptyList());
        assertEquals(0, bean.getItems().size());
    }

    @Test
    public void testFormalizePropNameWithVariousCases() {
        assertEquals("userName", Beans.formalizePropName("user_name"));
        assertEquals("firstName", Beans.formalizePropName("first_name"));

        assertEquals("clazz", Beans.formalizePropName("class"));

        assertEquals("id", Beans.formalizePropName("ID"));
        assertEquals("url", Beans.formalizePropName("URL"));

        assertEquals("userIdValue", Beans.formalizePropName("user_id_value"));
        assertEquals("addressLine1", Beans.formalizePropName("address_line_1"));

        assertEquals("simple", Beans.formalizePropName("simple"));
        assertEquals("alreadyCamel", Beans.formalizePropName("alreadyCamel"));
    }

    @Test
    public void testToCamelCaseWithVariousFormats() {
        assertEquals("userName", Beans.toCamelCase("user_name"));
        assertEquals("firstName", Beans.toCamelCase("first_name"));

        assertEquals("addressLine", Beans.toCamelCase("address-line"));

        assertEquals("userId", Beans.toCamelCase("USER_ID"));

        assertEquals("name", Beans.toCamelCase("name"));
        assertEquals("name", Beans.toCamelCase("NAME"));

        assertEquals("address1", Beans.toCamelCase("address_1"));
        assertEquals("line2Value", Beans.toCamelCase("line_2_value"));

        assertEquals("", Beans.toCamelCase(""));
        assertNull(Beans.toCamelCase((String) null));
    }

    @Test
    public void testToLowerCaseWithUnderscoreVariants() {
        assertEquals("user_name", Beans.toLowerCaseWithUnderscore("userName"));
        assertEquals("first_name", Beans.toLowerCaseWithUnderscore("firstName"));

        assertEquals("user_name", Beans.toLowerCaseWithUnderscore("UserName"));

        assertEquals("user_id", Beans.toLowerCaseWithUnderscore("userID"));
        assertEquals("url", Beans.toLowerCaseWithUnderscore("URL"));

        assertEquals("simple", Beans.toLowerCaseWithUnderscore("simple"));

        assertEquals("a", Beans.toLowerCaseWithUnderscore("a"));
        assertEquals("a", Beans.toLowerCaseWithUnderscore("A"));

        assertEquals("", Beans.toLowerCaseWithUnderscore(""));
        assertNull(Beans.toLowerCaseWithUnderscore((String) null));
    }

    @Test
    public void testToUpperCaseWithUnderscoreVariants() {
        assertEquals("USER_NAME", Beans.toUpperCaseWithUnderscore("userName"));
        assertEquals("FIRST_NAME", Beans.toUpperCaseWithUnderscore("firstName"));

        assertEquals("USER_NAME", Beans.toUpperCaseWithUnderscore("UserName"));

        assertEquals("USER_ID", Beans.toUpperCaseWithUnderscore("userID"));
        assertEquals("URL", Beans.toUpperCaseWithUnderscore("URL"));

        assertEquals("SIMPLE", Beans.toUpperCaseWithUnderscore("SIMPLE"));

        assertEquals("A", Beans.toUpperCaseWithUnderscore("a"));
        assertEquals("A", Beans.toUpperCaseWithUnderscore("A"));

        assertEquals("", Beans.toUpperCaseWithUnderscore(""));
        assertNull(Beans.toUpperCaseWithUnderscore((String) null));
    }

    @Test
    public void testToCamelCaseMapTransformation() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_name", "John");
        map.put("first_name", "Jane");
        map.put("USER_ID", 123);
        map.put("address-line", "Main St");

        Beans.toCamelCase(map);

        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("firstName"));
        assertTrue(map.containsKey("userId"));
        assertTrue(map.containsKey("addressLine"));

        assertFalse(map.containsKey("user_name"));
        assertFalse(map.containsKey("first_name"));
        assertFalse(map.containsKey("USER_ID"));
        assertFalse(map.containsKey("address-line"));

        Map<String, Object> emptyMap = new HashMap<>();
        Beans.toCamelCase(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testToLowerCaseWithUnderscoreMapTransformation() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");
        map.put("userId", 123);
        map.put("addressLine", "Main St");

        Beans.toLowerCaseWithUnderscore(map);

        assertTrue(map.containsKey("user_name"));
        assertTrue(map.containsKey("first_name"));
        assertTrue(map.containsKey("user_id"));
        assertTrue(map.containsKey("address_line"));

        assertFalse(map.containsKey("userName"));
        assertFalse(map.containsKey("firstName"));
    }

    @Test
    public void testToUpperCaseWithUnderscoreMapTransformation() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");
        map.put("userId", 123);

        Beans.toUpperCaseWithUnderscore(map);

        assertTrue(map.containsKey("USER_NAME"));
        assertTrue(map.containsKey("FIRST_NAME"));
        assertTrue(map.containsKey("USER_ID"));

        assertFalse(map.containsKey("userName"));
    }

    @Test
    public void testMap2BeanWithSelectPropsOnly() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Selected");
        map.put("age", 99);
        map.put("active", true);

        SimpleBean bean = Beans.map2Bean(map, Arrays.asList("name"), SimpleBean.class);
        assertEquals("Selected", bean.getName());
        assertEquals(0, bean.getAge());

        bean = Beans.map2Bean(map, Arrays.asList("name", "age"), SimpleBean.class);
        assertEquals("Selected", bean.getName());
        assertEquals(99, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testMap2BeanListWithIgnoreOptions() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", null);
        mapList.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", null);
        map2.put("age", 25);
        mapList.add(map2);

        List<SimpleBean> beans = Beans.map2Bean(mapList, true, true, SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
        assertNull(beans.get(1).getName());
        assertEquals(25, beans.get(1).getAge());
    }

    @Test
    public void testBean2MapWithMapSupplierOnly() {
        Map<String, Object> linkedMap = Beans.bean2Map(simpleBean, IntFunctions.ofLinkedHashMap());
        assertTrue(linkedMap instanceof LinkedHashMap);
        assertEquals("John", linkedMap.get("name"));

        TreeMap<String, Object> treeMap = Beans.bean2Map(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("John", treeMap.get("name"));
    }

    @Test
    public void testBean2MapWithSelectPropsOnly() {
        Map<String, Object> map = Beans.bean2Map(simpleBean, Arrays.asList("name"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));

        map = Beans.bean2Map(simpleBean, Arrays.asList("name", "active"));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithSelectPropsAndSupplier() {
        TreeMap<String, Object> map = Beans.bean2Map(simpleBean, Arrays.asList("name", "age"), NamingPolicy.NO_CHANGE, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2Map(simpleBean, output);

        assertTrue(output.size() >= 3);
        assertEquals("John", output.get("name"));
        assertEquals(25, output.get("age"));
    }

    @Test
    public void testBean2MapWithSelectPropsAndNamingAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2Map(simpleBean, Arrays.asList("name", "age"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);

        assertTrue(output.size() >= 2);
        assertFalse(output.containsKey("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullOnly() {
        simpleBean.setActive(null);

        Map<String, Object> map = Beans.bean2Map(simpleBean, true);
        assertFalse(map.containsKey("active"));
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));

        map = Beans.bean2Map(simpleBean, false);
        assertTrue(map.containsKey("active"));
        assertNull(map.get("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullAndIgnoredProps() {
        simpleBean.setActive(null);
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.bean2Map(simpleBean, true, ignored);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        TreeMap<String, Object> map = Beans.bean2Map(simpleBean, false, ignored, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithAllParametersAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("active");

        Map<String, Object> map = Beans.bean2Map(simpleBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());

        assertFalse(map.containsKey("active"));
        assertTrue(map.size() >= 2);
    }

    @Test
    public void testBean2MapWithIgnoreNullAndOutputOnly() {
        simpleBean.setActive(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.bean2Map(simpleBean, true, output);
        assertFalse(output.containsKey("active"));
        assertTrue(output.containsKey("name"));
    }

    @Test
    public void testBean2MapWithIgnoreNullIgnoredPropsAndOutputOnly() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2Map(simpleBean, false, ignored, output);

        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
        assertTrue(output.containsKey("active"));
    }

    @Test
    public void testBean2MapWithAllParametersAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2Map(simpleBean, true, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);

        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
    }

    @Test
    public void testDeepBean2MapBasic() {
        Map<String, Object> map = Beans.deepBean2Map(nestedBean);
        assertEquals("123", map.get("id"));
        assertTrue(map.get("simpleBean") instanceof Map);
        assertTrue(map.get("address") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> innerBean = (Map<String, Object>) map.get("simpleBean");
        assertEquals("John", innerBean.get("name"));
    }

    @Test
    public void testDeepBean2MapWithSupplierOnly() {
        TreeMap<String, Object> map = Beans.deepBean2Map(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals("123", map.get("id"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsOnly() {
        Map<String, Object> map = Beans.deepBean2Map(nestedBean, Arrays.asList("id", "address"));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
        assertFalse(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsAndSupplierOnly() {
        TreeMap<String, Object> map = Beans.deepBean2Map(nestedBean, Arrays.asList("id"), IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsNamingAndSupplierFull() {
        Map<String, Object> map = Beans.deepBean2Map(nestedBean, Arrays.asList("id", "simpleBean"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE,
                IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean") || map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBean2Map(nestedBean, output);

        assertTrue(output.size() >= 2);
        assertEquals("123", output.get("id"));
        assertTrue(output.get("simpleBean") instanceof Map);
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullOnly() {
        nestedBean.setSimpleBean(null);

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, true);
        assertTrue(map.containsKey("id"));
        assertFalse(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullAndIgnoredProps() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        TreeMap<String, Object> map = Beans.deepBean2Map(nestedBean, false, ignored, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithAllParametersForMap() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, true, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertFalse(map.containsKey("tags"));
        assertTrue(map.size() >= 2);
    }

    @Test
    public void testDeepBean2MapWithAllParametersForMapAlternate() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBean2Map(nestedBean, false, ignored, NamingPolicy.NO_CHANGE, IntFunctions.ofLinkedHashMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testBean2FlatMapBasic() {
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
        assertEquals(25, map.get("simpleBean.age"));
        assertEquals("New York", map.get("address.city"));
    }

    @Test
    public void testBean2FlatMapWithSupplierOnly() {
        TreeMap<String, Object> map = Beans.bean2FlatMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsOnly() {
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, Arrays.asList("id", "simpleBean"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
        assertFalse(map.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndSupplierOnly() {
        TreeMap<String, Object> map = Beans.bean2FlatMap(nestedBean, Arrays.asList("address"), IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndSupplierFull() {
        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, Arrays.asList("simpleBean"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertTrue(map.containsKey("simple_bean.name") || map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, output);

        assertTrue(output.size() >= 5);
        assertEquals("123", output.get("id"));
        assertEquals("John", output.get("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, Arrays.asList("address"), output);

        assertTrue(output.containsKey("address.city"));
        assertFalse(output.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndOutputFull() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.bean2FlatMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.size() >= 1);
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullOnly() {
        nestedBean.getAddress().setZipCode(null);

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, true);
        assertFalse(map.containsKey("address.zipCode"));
        assertTrue(map.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullAndIgnoredProps() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        TreeMap<String, Object> map = Beans.bean2FlatMap(nestedBean, false, ignored, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testBean2FlatMapWithAllParametersForMap() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, false, ignored, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.size() >= 4);
    }

    @Test
    public void testBean2FlatMapWithAllParametersForMapAlternate() {
        Set<String> ignored = new HashSet<>();
        ignored.add("address");

        Map<String, Object> map = Beans.bean2FlatMap(nestedBean, true, ignored, NamingPolicy.NO_CHANGE, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertFalse(map.containsKey("address.city"));
    }

    @Test
    public void testNewBeanCreation() {
        SimpleBean bean = Beans.newBean(SimpleBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());

        NestedBean nested = Beans.newBean(NestedBean.class);
        assertNotNull(nested);
        assertNull(nested.getId());

        EntityBean entity = Beans.newBean(EntityBean.class);
        assertNotNull(entity);
        assertNull(entity.getId());
    }

    @Test
    public void testCloneBasic() {
        SimpleBean original = new SimpleBean("Original", 100);
        original.setActive(true);

        SimpleBean cloned = Beans.clone(original);
        assertNotNull(cloned);
        assertNotSame(original, cloned);
        assertEquals("Original", cloned.getName());
        assertEquals(100, cloned.getAge());
        assertEquals(true, cloned.getActive());

        cloned.setName("Modified");
        assertEquals("Original", original.getName());
    }

    @Test
    public void testCloneWithNullObject() {
        SimpleBean cloned = Beans.clone(null);
        assertNull(cloned);
    }

    @Test
    public void testCloneWithTargetTypeNotNull() {
        SimpleBean original = new SimpleBean("Test", 50);

        SimpleBean cloned = Beans.clone(original, SimpleBean.class);
        assertNotNull(cloned);
        assertEquals("Test", cloned.getName());
        assertEquals(50, cloned.getAge());
    }

    @Test
    public void testCloneWithNullSourceButTargetType() {
        SimpleBean cloned = Beans.clone(null, SimpleBean.class);
        assertNotNull(cloned);
        assertNull(cloned.getName());
        assertEquals(0, cloned.getAge());
    }

    @Test
    public void testCopyBasic() {
        SimpleBean original = new SimpleBean("Copy", 75);
        original.setActive(false);

        SimpleBean copied = Beans.copy(original);
        assertNotNull(copied);
        assertNotSame(original, copied);
        assertEquals("Copy", copied.getName());
        assertEquals(75, copied.getAge());
        assertEquals(false, copied.getActive());
    }

    @Test
    public void testCopyWithNullSource() {
        SimpleBean copied = Beans.copy((SimpleBean) null);
        assertNull(copied);
    }

    @Test
    public void testCopyWithSelectPropNamesOnly() {
        SimpleBean original = new SimpleBean("Selected", 88);
        original.setActive(true);

        SimpleBean copied = Beans.copy(original, Arrays.asList("name"));
        assertEquals("Selected", copied.getName());
        assertEquals(0, copied.getAge());
        assertNull(copied.getActive());
    }

    @Test
    public void testCopyWithPropFilterOnly() {
        SimpleBean original = new SimpleBean("Filtered", 99);
        original.setActive(true);

        SimpleBean copied = Beans.copy(original, (name, value) -> value instanceof String);
        assertEquals("Filtered", copied.getName());
        assertEquals(0, copied.getAge());
        assertNull(copied.getActive());
    }

    @Test
    public void testCopyWithTargetTypeOnly() {
        SimpleBean original = new SimpleBean("TypeTest", 111);

        SimpleBean copied = Beans.copy(original, SimpleBean.class);
        assertNotNull(copied);
        assertEquals("TypeTest", copied.getName());
        assertEquals(111, copied.getAge());
    }

    @Test
    public void testCopyWithSelectPropsAndTargetType() {
        SimpleBean original = new SimpleBean("SelectType", 222);

        SimpleBean copied = Beans.copy(original, Arrays.asList("age"), SimpleBean.class);
        assertNull(copied.getName());
        assertEquals(222, copied.getAge());
    }

    @Test
    public void testCopyWithSelectPropsConverterAndTargetType() {
        SimpleBean original = new SimpleBean("Convert", 333);

        SimpleBean copied = Beans.copy(original, Arrays.asList("name", "age"), name -> name, SimpleBean.class);
        assertEquals("Convert", copied.getName());
        assertEquals(333, copied.getAge());
    }

    @Test
    public void testCopyWithPropFilterAndTargetType() {
        SimpleBean original = new SimpleBean("FilterType", 444);
        original.setActive(true);

        SimpleBean copied = Beans.copy(original, (name, value) -> value instanceof Integer, SimpleBean.class);
        assertNull(copied.getName());
        assertEquals(444, copied.getAge());
        assertNull(copied.getActive());
    }

    @Test
    public void testCopyWithPropFilterConverterAndTargetType() {
        SimpleBean original = new SimpleBean("FilterConvert", 555);

        SimpleBean copied = Beans.copy(original, (name, value) -> true, name -> name, SimpleBean.class);
        assertEquals("FilterConvert", copied.getName());
        assertEquals(555, copied.getAge());
    }

    @Test
    public void testCopyWithIgnoreUnmatchedAndIgnoredProps() {
        SimpleBean original = new SimpleBean("IgnoreTest", 666);
        original.setActive(true);

        Set<String> ignored = new HashSet<>();
        ignored.add("active");

        SimpleBean copied = Beans.copy(original, true, ignored, SimpleBean.class);
        assertEquals("IgnoreTest", copied.getName());
        assertEquals(666, copied.getAge());
        assertNull(copied.getActive());
    }

    @Test
    public void testMergeBasic() {
        SimpleBean source = new SimpleBean("Source", 10);
        SimpleBean target = new SimpleBean("Target", 20);

        Beans.merge(source, target);
        assertEquals("Source", target.getName());
        assertEquals(10, target.getAge());
    }

    @Test
    public void testMergeWithNullSourceDoesNotModify() {
        SimpleBean target = new SimpleBean("Target", 30);

        Beans.merge(null, target);
        assertEquals("Target", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithMergeFunc() {
        SimpleBean source = new SimpleBean("A", 5);
        SimpleBean target = new SimpleBean("B", 10);

        Beans.merge(source, target, Fn.o((srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        }));

        assertEquals("A", target.getName());
        assertEquals(15, target.getAge());
    }

    @Test
    public void testMergeWithConverterAndMergeFunc() {
        SimpleBean source = new SimpleBean("Test", 7);
        SimpleBean target = new SimpleBean("Original", 3);

        Beans.merge(source, target, name -> name, (srcVal, tgtVal) -> srcVal);
        assertEquals("Test", target.getName());
        assertEquals(7, target.getAge());
    }

    @Test
    public void testMergeWithSelectPropsOnly() {
        SimpleBean source = new SimpleBean("New", 100);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Old", 200);
        target.setActive(false);

        Beans.merge(source, target, Arrays.asList("name"));
        assertEquals("New", target.getName());
        assertEquals(200, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeWithSelectPropsAndMergeFuncOnly() {
        SimpleBean source = new SimpleBean("X", 8);
        SimpleBean target = new SimpleBean("Y", 2);

        Beans.merge(source, target, Arrays.asList("age"), (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) * ((Integer) tgtVal);
            }
            return srcVal;
        });

        assertEquals("Y", target.getName());
        assertEquals(16, target.getAge());
    }

    @Test
    public void testMergeWithSelectPropsConverterAndMergeFuncFull() {
        SimpleBean source = new SimpleBean("Merge", 15);
        SimpleBean target = new SimpleBean("Base", 5);

        Beans.merge(source, target, Arrays.asList("age"), name -> name, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) - ((Integer) tgtVal);
            }
            return srcVal;
        });

        assertEquals("Base", target.getName());
        assertEquals(10, target.getAge());
    }

    @Test
    public void testMergeWithPropFilterOnly() {
        SimpleBean source = new SimpleBean("FilteredMerge", 20);
        source.setActive(true);
        SimpleBean target = new SimpleBean("BaseTarget", 40);
        target.setActive(false);

        Beans.merge(source, target, Fn.p((name, value) -> value instanceof Integer));
        assertEquals("BaseTarget", target.getName());
        assertEquals(20, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeWithPropFilterAndConverterOnly() {
        SimpleBean source = new SimpleBean("PropFilter", 25);
        SimpleBean target = new SimpleBean("TargetBean", 75);

        Beans.merge(source, target, (name, value) -> true, name -> name);
        assertEquals("PropFilter", target.getName());
        assertEquals(25, target.getAge());
    }

    @Test
    public void testMergeWithPropFilterConverterAndMergeFuncFull() {
        SimpleBean source = new SimpleBean("Complex", 12);
        SimpleBean target = new SimpleBean("Base", 8);

        Beans.merge(source, target, (name, value) -> value instanceof Integer, name -> name, (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));

        assertEquals("Base", target.getName());
        assertEquals(20, target.getAge());
    }

    @Test
    public void testMergeWithIgnoreUnmatchedAndIgnoredPropsOnly() {
        SimpleBean source = new SimpleBean("Ignore", 90);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Target", 60);

        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Beans.merge(source, target, true, ignored);
        assertEquals("Ignore", target.getName());
        assertEquals(60, target.getAge());
    }

    @Test
    public void testMergeWithIgnoreUnmatchedIgnoredPropsAndMergeFunc() {
        SimpleBean source = new SimpleBean("Final", 4);
        SimpleBean target = new SimpleBean("Init", 6);

        Beans.merge(source, target, true, null, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) * ((Integer) tgtVal);
            }
            return srcVal;
        });

        assertEquals("Final", target.getName());
        assertEquals(24, target.getAge());
    }

    @Test
    public void testEraseSpecificProperties() {
        SimpleBean bean = new SimpleBean("ToErase", 123);
        bean.setActive(true);

        Beans.erase(bean, "name");
        assertNull(bean.getName());
        assertEquals(123, bean.getAge());
        assertEquals(true, bean.getActive());

        Beans.erase(bean, "age", "active");
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testEraseWithCollectionOfProps() {
        SimpleBean bean = new SimpleBean("Erase", 456);
        bean.setActive(false);

        Beans.erase(bean, Arrays.asList("name", "age"));
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(false, bean.getActive());
    }

    @Test
    public void testEraseWithNullBeanDoesNotThrow() {
        Beans.erase(null, "name");
        Beans.erase(null, Arrays.asList("name", "age"));
    }

    @Test
    public void testEraseAllProperties() {
        SimpleBean bean = new SimpleBean("EraseAll", 789);
        bean.setActive(true);

        Beans.eraseAll(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testEraseAllWithNullBeanDoesNotThrow() {
        Beans.eraseAll(null);
    }

    @Test
    public void testFillBeanInstance() {
        SimpleBean bean = new SimpleBean();

        Beans.fill(bean);
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNotNull(bean.getActive());
    }

    @Test
    public void testFillBeanInstanceWithSelectProps() {
        SimpleBean bean = new SimpleBean();

        Beans.fill(bean, Arrays.asList("name", "age"));
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNull(bean.getActive());
    }

    @Test
    public void testFillBeanClass() {
        SimpleBean filled = Beans.fill(SimpleBean.class);
        assertNotNull(filled);
        assertNotNull(filled.getName());
        assertTrue(filled.getAge() != 0);
    }

    @Test
    public void testFillBeanClassWithCount() {
        List<SimpleBean> filledList = Beans.fill(SimpleBean.class, 5);
        assertEquals(5, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean);
            assertNotNull(bean.getName());
        }
    }

    @Test
    public void testFillBeanClassWithSelectPropsOnly() {
        SimpleBean filled = Beans.fill(SimpleBean.class, Arrays.asList("age"));
        assertNotNull(filled);
        assertTrue(filled.getAge() != 0);
        assertNull(filled.getName());
    }

    @Test
    public void testFillBeanClassWithSelectPropsAndCount() {
        List<SimpleBean> filledList = Beans.fill(SimpleBean.class, Arrays.asList("name"), 3);
        assertEquals(3, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean.getName());
            assertEquals(0, bean.getAge());
        }
    }

    @Test
    public void testEqualsByCommonPropsWithCommonProperties() {
        class BeanX {
            private String name;
            private int value;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public int getValue() {
                return value;
            }

            public void setValue(int value) {
                this.value = value;
            }
        }

        class BeanY {
            private String name;
            private String extra;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getExtra() {
                return extra;
            }

            public void setExtra(String extra) {
                this.extra = extra;
            }
        }

        BeanX x = new BeanX();
        x.setName("Common");
        BeanY y = new BeanY();
        y.setName("Common");

        assertTrue(Beans.equalsByCommonProps(x, y));

        y.setName("Different");
        assertFalse(Beans.equalsByCommonProps(x, y));
    }

    @Test
    public void testEqualsByPropsWithSpecificProps() {
        SimpleBean bean1 = new SimpleBean("Same", 100);
        SimpleBean bean2 = new SimpleBean("Same", 200);

        assertTrue(Beans.equalsByProps(bean1, bean2, Arrays.asList("name")));
        assertFalse(Beans.equalsByProps(bean1, bean2, Arrays.asList("age")));
        assertFalse(Beans.equalsByProps(bean1, bean2, Arrays.asList("name", "age")));
    }

    @Test
    public void testEqualsByPropsWithAllPropsEqual() {
        SimpleBean bean1 = new SimpleBean("Equal", 50);
        bean1.setActive(true);
        SimpleBean bean2 = new SimpleBean("Equal", 50);
        bean2.setActive(true);

        assertTrue(Beans.equalsByProps(bean1, bean2, Arrays.asList("name", "age", "active")));
    }

    @Test
    public void testCompareByPropsWithSingleProp() {
        SimpleBean bean1 = new SimpleBean("Alpha", 10);
        SimpleBean bean2 = new SimpleBean("Beta", 20);

        assertTrue(Beans.compareByProps(bean1, bean2, Arrays.asList("name")) < 0);
        assertTrue(Beans.compareByProps(bean2, bean1, Arrays.asList("name")) > 0);
        assertEquals(0, Beans.compareByProps(bean1, bean1, Arrays.asList("name")));
    }

    @Test
    public void testCompareByPropsWithMultipleProps() {
        SimpleBean bean1 = new SimpleBean("Same", 10);
        SimpleBean bean2 = new SimpleBean("Same", 20);

        assertTrue(Beans.compareByProps(bean1, bean2, Arrays.asList("name", "age")) < 0);
        assertTrue(Beans.compareByProps(bean2, bean1, Arrays.asList("name", "age")) > 0);
    }

    @Test
    public void testCompareByPropsWithAllEqual() {
        SimpleBean bean1 = new SimpleBean("Equal", 100);
        SimpleBean bean2 = new SimpleBean("Equal", 100);

        assertEquals(0, Beans.compareByProps(bean1, bean2, Arrays.asList("name", "age")));
    }

    @Test
    public void testPropertiesStreamBasic() {
        List<Map.Entry<String, Object>> props = Beans.properties(simpleBean).toList();

        assertFalse(props.isEmpty());
        assertTrue(props.stream().anyMatch(e -> "name".equals(e.getKey())));
        assertTrue(props.stream().anyMatch(e -> "age".equals(e.getKey())));
    }

    @Test
    public void testPropertiesStreamWithFilter() {
        simpleBean.setActive(null);

        List<Map.Entry<String, Object>> nonNullProps = Beans.properties(simpleBean, (name, value) -> value != null).toList();

        assertTrue(nonNullProps.stream().noneMatch(e -> e.getValue() == null));
        assertTrue(nonNullProps.stream().anyMatch(e -> "name".equals(e.getKey())));
        assertFalse(nonNullProps.stream().anyMatch(e -> "active".equals(e.getKey())));
    }

    @Test
    public void testPropertiesStreamWithTypeFilter() {
        List<Map.Entry<String, Object>> stringProps = Beans.properties(simpleBean, (name, value) -> value instanceof String).toList();

        assertEquals(1, stringProps.size());
        assertEquals("name", stringProps.get(0).getKey());
    }

    @Test
    public void testPropertiesStreamWithNameFilter() {
        List<Map.Entry<String, Object>> filteredProps = Beans.properties(simpleBean, (name, value) -> name.startsWith("a")).toList();

        assertTrue(filteredProps.stream().allMatch(e -> e.getKey().startsWith("a")));
        assertTrue(filteredProps.stream().anyMatch(e -> "age".equals(e.getKey())));
        assertTrue(filteredProps.stream().anyMatch(e -> "active".equals(e.getKey())));
    }

    @Test
    public void testRegisterNonBeanClassFunctionality() {
        class CustomNonBean {
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }

        assertTrue(Beans.isBeanClass(CustomNonBean.class));

        Beans.registerNonBeanClass(CustomNonBean.class);

    }

    @Test
    public void testRegisterNonPropGetSetMethodFunctionality() {
        Beans.registerNonPropGetSetMethod(SimpleBean.class, "toString");

    }

    @Test
    public void testRegisterPropGetSetMethodWithValidMethod() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        Beans.registerPropGetSetMethod("name", getName);

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.registerPropGetSetMethod("name", setName);

    }

    @Test
    public void testRegisterXMLBindingClassFunctionality() {
        class XMLBean {
            private String data;

            public String getData() {
                return data;
            }

            public void setData(String data) {
                this.data = data;
            }
        }

        assertFalse(Beans.isRegisteredXMLBindingClass(XMLBean.class));

        Beans.registerXMLBindingClass(XMLBean.class);

        assertTrue(Beans.isRegisteredXMLBindingClass(XMLBean.class));
    }

    @Test
    public void testGetPropNameByMethodWithVariousMethods() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        assertEquals("name", Beans.getPropNameByMethod(getName));

        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        assertEquals("age", Beans.getPropNameByMethod(setAge));

        Method getActive = SimpleBean.class.getMethod("getActive");
        assertEquals("active", Beans.getPropNameByMethod(getActive));

        Method setActive = SimpleBean.class.getMethod("setActive", Boolean.class);
        assertEquals("active", Beans.getPropNameByMethod(setActive));
    }

    @Test
    public void testGetPropNameListCompleteness() {
        ImmutableList<String> propNames = Beans.getPropNameList(SimpleBean.class);
        assertNotNull(propNames);
        assertTrue(propNames.size() >= 3);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("age"));
        assertTrue(propNames.contains("active"));
    }

    @Test
    public void testGetDiffIgnoredPropNamesWithAnnotation() {
        ImmutableSet<String> ignored = Beans.getDiffIgnoredPropNames(BeanWithDiffIgnore.class);
        assertNotNull(ignored);
        assertEquals(2, ignored.size());
        assertTrue(ignored.contains("lastModified"));
        assertTrue(ignored.contains("internalFlag"));
        assertFalse(ignored.contains("name"));
    }

    @Test
    public void testGetPropFieldsReturnsAllFields() {
        ImmutableMap<String, Field> fields = Beans.getPropFields(SimpleBean.class);
        assertNotNull(fields);
        assertTrue(fields.size() >= 3);
        assertNotNull(fields.get("name"));
        assertNotNull(fields.get("age"));
        assertNotNull(fields.get("active"));
    }

    @Test
    public void testGetPropGetMethodsReturnsAllGetters() {
        ImmutableMap<String, Method> methods = Beans.getPropGetMethods(SimpleBean.class);
        assertNotNull(methods);
        assertTrue(methods.size() >= 3);
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
        assertNotNull(methods.get("active"));
    }

    @Test
    public void testGetPropSetMethodsReturnsAllSetters() {
        ImmutableMap<String, Method> methods = Beans.getPropSetMethods(SimpleBean.class);
        assertNotNull(methods);
        assertTrue(methods.size() >= 3);
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
        assertNotNull(methods.get("active"));
    }

    @Test
    public void testGetBuilderInfoForClassWithBuilder() {
        var builderInfo = Beans.getBuilderInfo(BeanWithBuilder.class);
        assertNotNull(builderInfo);
        assertNotNull(builderInfo._1);
        assertNotNull(builderInfo._2);
        assertNotNull(builderInfo._3);

        Object builder = builderInfo._2.get();
        assertNotNull(builder);
        assertTrue(builder.getClass().getName().contains("Builder"));
    }

    @Test
    public void testGetBuilderInfoForClassWithoutBuilder() {
        var builderInfo = Beans.getBuilderInfo(SimpleBean.class);
        assertNull(builderInfo);
    }

    @Test
    public void testRefreshBeanPropInfoDoesNotThrow() {
        Beans.refreshBeanPropInfo(SimpleBean.class);
        Beans.refreshBeanPropInfo(NestedBean.class);
        Beans.refreshBeanPropInfo(EntityBean.class);

        var beanInfo = Beans.getBeanInfo(SimpleBean.class);
        assertNotNull(beanInfo);
        assertFalse(beanInfo.propInfoList.isEmpty());
    }

    @Test
    public void testGetPropValueWithNestedPropertyPath() {
        assertEquals("New York", Beans.getPropValue(nestedBean, "address.city"));
        assertEquals("5th Avenue", Beans.getPropValue(nestedBean, "address.street"));
        assertEquals("10001", Beans.getPropValue(nestedBean, "address.zipCode"));

        assertEquals("John", Beans.getPropValue(nestedBean, "simpleBean.name"));
        assertEquals(25, (Integer) Beans.getPropValue(nestedBean, "simpleBean.age"));
    }

    @Test
    public void testGetPropValueWithMethod() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        Object value = Beans.getPropValue(simpleBean, getName);
        assertEquals("John", value);

        Method getAge = SimpleBean.class.getMethod("getAge");
        value = Beans.getPropValue(simpleBean, getAge);
        assertEquals(25, value);
    }

    @Test
    public void testSetPropValueWithNestedPropertyHandling() {
        SimpleBean bean = new SimpleBean();
        Beans.setPropValue(bean, "name", "NewName");
        assertEquals("NewName", bean.getName());

        Beans.setPropValue(bean, "age", 150);
        assertEquals(150, bean.getAge());

        Beans.setPropValue(bean, "active", Boolean.FALSE);
        assertEquals(Boolean.FALSE, bean.getActive());
    }
}
