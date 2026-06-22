package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.u.Nullable;

public class BeansTest extends TestBase {

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

    public static class NoDefaultConstructorBean {
        private final String name;

        public NoDefaultConstructorBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
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

    public static class BeanWithStaticAccessors {
        private String name;
        private static String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static String getVersion() {
            return version;
        }

        public static void setVersion(String version) {
            BeanWithStaticAccessors.version = version;
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

    // ========== Additional tests for untested methods and edge cases ==========

    @Test
    public void testIsBeanClass_EdgeCases() {
        assertFalse(Beans.isBeanClass(int.class));
        assertFalse(Beans.isBeanClass(void.class));
        assertFalse(Beans.isBeanClass(int[].class));
        assertFalse(Beans.isBeanClass(Object.class));
        assertFalse(Beans.isBeanClass(java.util.Date.class));
        assertFalse(Beans.isBeanClass(HashMap.class));
        assertFalse(Beans.isBeanClass(ArrayList.class));
    }

    @Test
    public void testIsRecordClass() {
        assertTrue(Beans.isRecordClass(RecordBean.class));
        assertFalse(Beans.isRecordClass(SimpleBean.class));
        assertFalse(Beans.isRecordClass(null));
    }

    @Test
    public void testIsRecordClass_EdgeCases() {
        assertFalse(Beans.isRecordClass(EntityBean.class));
        assertFalse(Beans.isRecordClass(String.class));
        assertFalse(Beans.isRecordClass(int.class));
    }

    @Test
    public void testGetBeanInfo_EntityBean() {
        com.landawn.abacus.parser.ParserUtil.BeanInfo beanInfo = Beans.getBeanInfo(EntityBean.class);
        assertNotNull(beanInfo);
        assertFalse(beanInfo.propInfoList.isEmpty());
    }

    @Test
    public void testGetBeanInfo_RecordBean() {
        com.landawn.abacus.parser.ParserUtil.BeanInfo beanInfo = Beans.getBeanInfo(RecordBean.class);
        assertNotNull(beanInfo);
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
    public void testRefreshBeanPropInfo_DoesNotBreak() {
        Beans.refreshBeanPropInfo(EntityBean.class);
        com.landawn.abacus.parser.ParserUtil.BeanInfo beanInfo = Beans.getBeanInfo(EntityBean.class);
        assertNotNull(beanInfo);
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
    public void testGetBuilderInfoForClassWithBuilder() {
        Beans.BuilderInfo builderInfo = Beans.getBuilderInfo(BeanWithBuilder.class);
        assertNotNull(builderInfo);
        assertNotNull(builderInfo.builderClass());

        Object builder = builderInfo.newBuilder();
        assertNotNull(builder);
        assertTrue(builder.getClass().getName().contains("Builder"));

        // build() turns a builder into the target bean
        Object built = builderInfo.build(builder);
        assertNotNull(built);
        assertTrue(built instanceof BeanWithBuilder);
    }

    @Test
    public void testGetBuilderInfoForClassWithoutBuilder() {
        var builderInfo = Beans.getBuilderInfo(SimpleBean.class);
        assertNull(builderInfo);
    }

    @Test
    public void testGetBuilderInfo_NonBuilderClass() {
        assertNull(Beans.getBuilderInfo(Address.class));
    }

    @Test
    public void testGetBuilderInfo() {
        Beans.BuilderInfo builderInfo = Beans.getBuilderInfo(BeanWithBuilder.class);

        assertNotNull(builderInfo);
        assertNotNull(builderInfo.builderClass());

        Object builder = builderInfo.newBuilder();
        assertNotNull(builder);

        assertNull(Beans.getBuilderInfo(SimpleBean.class));

        assertThrows(IllegalArgumentException.class, () -> Beans.getBuilderInfo(null));
    }

    @Test
    public void testGetBuilderInfo_NullArg() {
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
    public void testRegisterNonBeanClass_AlreadyNonBean() {
        Beans.registerNonBeanClass(NonBean.class);
        // Should not throw when registering again
        assertDoesNotThrow(() -> Beans.registerNonBeanClass(NonBean.class));
    }

    @Test
    public void testRegisterNonPropGetSetMethod() {
        assertDoesNotThrow(() -> {
            Beans.registerNonPropertyAccessor(SimpleBean.class, "internal");
        });
    }

    @Test
    public void testRegisterNonPropGetSetMethodFunctionality() {
        assertDoesNotThrow(() -> {
            Beans.registerNonPropertyAccessor(SimpleBean.class, "toString");
        });
    }

    @Test
    public void testRegisterNonPropertyAccessor_NullPropName() {
        assertDoesNotThrow(() -> Beans.registerNonPropertyAccessor(SimpleBean.class, "nonexistent"));
    }

    @Test
    public void testRegisterPropGetSetMethod() throws Exception {
        Method method = SimpleBean.class.getMethod("getName");
        Beans.registerPropertyAccessor("name", method);

        Method invalidMethod = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("invalid", invalidMethod));
    }

    @Test
    public void testRegisterPropGetSetMethodWithValidMethod() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        Beans.registerPropertyAccessor("name", getName);

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.registerPropertyAccessor("name", setName);
        assertNotNull(setName);
    }

    @Test
    public void testRegisterPropertyAccessor_InvalidMethod() throws Exception {
        Method toStringMethod = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("test", toStringMethod));
    }

    // ===== registerPropertyAccessor with same method (no-op) =====

    @Test
    public void testRegisterPropertyAccessor_sameMethodNoError() throws Exception {
        // Registering the same getter method again should not throw
        Method getName = SimpleBean.class.getMethod("getName");
        assertDoesNotThrow(() -> Beans.registerPropertyAccessor("name", getName));
    }

    @Test
    public void testRegisterPropertyAccessor_withSetMethod() throws Exception {
        // Registering a setter method
        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        assertDoesNotThrow(() -> Beans.registerPropertyAccessor("age", setAge));
    }

    @Test
    public void testRegisterPropertyAccessor_nonGetterSetterThrows() throws Exception {
        // A regular method that is neither getter nor setter should throw
        Method toString = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("name", toString));
    }

    // ===== registerPropertyAccessor - conflict throws (different getter method for same prop) =====

    @Test
    public void testRegisterPropertyAccessor_conflictGetterThrows() throws Exception {
        // First register getName as "name" getter (should be ok - same method)
        Method getName = SimpleBean.class.getMethod("getName");
        Beans.registerPropertyAccessor("name", getName);

        // Try to register a different method under the same prop name for the same class
        // "getActive" for "name" - different method, should throw conflict
        Method getActive = SimpleBean.class.getMethod("getActive");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("name", getActive));
    }

    @Test
    public void testRegisterXMLBindingClass() {
        Beans.registerXmlBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(SimpleBean.class));
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

        assertFalse(Beans.isRegisteredXmlBindingClass(XMLBean.class));

        Beans.registerXmlBindingClass(XMLBean.class);

        assertTrue(Beans.isRegisteredXmlBindingClass(XMLBean.class));
    }

    @Test
    public void testRegisterXmlBindingClass_Multiple() {
        Beans.registerXmlBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(SimpleBean.class));
        // Register again should not throw
        Beans.registerXmlBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(SimpleBean.class));
    }

    @Test
    public void testIsRegisteredXMLBindingClass() {
        Beans.registerXmlBindingClass(EntityBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(EntityBean.class));
        assertFalse(Beans.isRegisteredXmlBindingClass(Address.class));
    }

    @Test
    public void testIsRegisteredXmlBindingClass_Unregistered() {
        assertFalse(Beans.isRegisteredXmlBindingClass(CollectionBean.class));
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
    public void testGetPropNameByMethod_SetterMethods() throws Exception {
        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        assertEquals("age", Beans.getPropNameByMethod(setAge));

        Method setActive = SimpleBean.class.getMethod("setActive", Boolean.class);
        assertEquals("active", Beans.getPropNameByMethod(setActive));
    }

    @Test
    public void testGetPropNameByMethod_NullMethod() {
        assertThrows(NullPointerException.class, () -> Beans.getPropNameByMethod(null));
    }

    // ===== getPropNameByMethod with field-named accessor =====

    @Test
    public void testGetPropNameByMethod_withGetPrefix() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        String propName = Beans.getPropNameByMethod(getName);
        assertEquals("name", propName);
    }

    @Test
    public void testGetPropNameByMethod_withSetPrefix() throws Exception {
        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        String propName = Beans.getPropNameByMethod(setAge);
        assertEquals("age", propName);
    }

    // ===== getPropNameByMethod with is-prefix method =====

    @Test
    public void testGetPropNameByMethod_withBooleanField() throws Exception {
        // SimpleBean has getActive() returning Boolean - test getPropNameByMethod
        Method getActive = SimpleBean.class.getMethod("getActive");
        String propName = Beans.getPropNameByMethod(getActive);
        assertEquals("active", propName);
    }

    public static class HasPrefixBean {
        private boolean active;

        public boolean hasActive() {
            return active;
        }
    }

    @Test
    public void testGetPropNameByMethod_HasPrefix() throws Exception {
        final Method hasActive = HasPrefixBean.class.getMethod("hasActive");

        assertEquals("active", Beans.getPropNameByMethod(hasActive));
    }

    @Test
    public void testStaticAccessorsAreNotTreatedAsBeanProperties() {
        List<String> propNames = Beans.getPropNameList(BeanWithStaticAccessors.class);

        assertTrue(propNames.contains("name"));
        assertFalse(propNames.contains("version"));
        assertNotNull(Beans.getPropGetter(BeanWithStaticAccessors.class, "name"));
        assertNull(Beans.getPropGetter(BeanWithStaticAccessors.class, "version"));
        assertNull(Beans.getPropSetter(BeanWithStaticAccessors.class, "version"));
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
    public void testGetPropNameList_EntityBean() {
        ImmutableList<String> propNames = Beans.getPropNameList(EntityBean.class);
        assertNotNull(propNames);
        assertTrue(propNames.contains("id"));
        assertTrue(propNames.contains("value"));
    }

    @Test
    public void testGetPropNameList_NoDefaultConstructorBean() {
        List<String> propNames = Beans.getPropNameList(NoDefaultConstructorBean.class);

        assertEquals(Collections.singletonList("name"), propNames);
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
    public void testGetPropNameList_EmptyBean() {
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNameList(null));
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

    // ===== getPropNames(Class, Collection) with non-Set Collection hits L1038 =====

    @Test
    @SuppressWarnings("deprecation")
    public void testGetPropNames_NonSetCollection() {
        // Passing a LinkedList (not a Set) forces the branch at L1038 that delegates
        // to the Set overload after wrapping – rather than short-circuiting via
        // the instanceof Set check.
        java.util.LinkedList<String> exclusions = new java.util.LinkedList<>(Arrays.asList("age"));
        List<String> props = Beans.getPropNames(SimpleBean.class, exclusions);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));
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
    public void testGetPropNamesFromBean_IgnoreNullValue() {
        SimpleBean bean = new SimpleBean("Test", 0);
        bean.setActive(null);

        List<String> props = Beans.getPropNames(bean, true);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("active"));

        props = Beans.getPropNames(bean, false);
        assertTrue(props.contains("active"));
    }

    @Test
    public void testGetPropNames_NullExclusionCollection() {
        List<String> props = Beans.getPropNames(SimpleBean.class, (Collection<String>) null);
        assertNotNull(props);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
    }

    @Test
    public void testGetPropNames_EmptyExclusionCollection() {
        List<String> props = Beans.getPropNames(SimpleBean.class, Collections.emptyList());
        assertNotNull(props);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));
    }

    @Test
    public void testGetPropNames_NullExclusionSet() {
        List<String> props = Beans.getPropNames(SimpleBean.class, (Set<String>) null);
        assertNotNull(props);
        assertTrue(props.contains("name"));
    }

    @Test
    public void testGetPropNames_BeanIgnoreNullValue() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(null);

        List<String> props = Beans.getPropNames(bean, true);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertFalse(props.contains("active"));
    }

    @Test
    public void testGetPropNames_BeanDontIgnoreNullValue() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(null);

        List<String> props = Beans.getPropNames(bean, false);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));
    }

    // ===== getPropNames(Object, boolean) additional branch - all non-null =====

    @Test
    public void testGetPropNamesFromBean_ignoreNullTrue_withAllNonNull() {
        SimpleBean bean = new SimpleBean("Rex", 5);
        bean.setActive(false);
        List<String> props = Beans.getPropNames(bean, true);
        // active=false is not null, age=5 is primitive (boxed)
        assertTrue(props.contains("name"));
        assertTrue(props.contains("active"));
    }

    @Test
    public void testGetPropNamesFromBean_ignoreNullFalse_includesNullProps() {
        SimpleBean bean = new SimpleBean("Sara", 0);
        // active is null
        List<String> props = Beans.getPropNames(bean, false);
        assertTrue(props.contains("active"));
        assertTrue(props.contains("name"));
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
    public void testGetDiffIgnoredPropNames() {
        ImmutableSet<String> ignored = Beans.getIgnoredPropNamesForDiff(BeanWithDiffIgnore.class);
        assertTrue(ignored.contains("lastModified"));
        assertTrue(ignored.contains("internalFlag"));
        assertFalse(ignored.contains("name"));
        assertEquals(2, ignored.size());
    }

    @Test
    public void testGetDiffIgnoredPropNamesWithAnnotation() {
        ImmutableSet<String> ignored = Beans.getIgnoredPropNamesForDiff(BeanWithDiffIgnore.class);
        assertNotNull(ignored);
        assertEquals(2, ignored.size());
        assertTrue(ignored.contains("lastModified"));
        assertTrue(ignored.contains("internalFlag"));
        assertFalse(ignored.contains("name"));
    }

    @Test
    public void testGetIgnoredPropNamesForDiff_NoDiffIgnore() {
        ImmutableSet<String> ignored = Beans.getIgnoredPropNamesForDiff(SimpleBean.class);
        assertTrue(ignored.isEmpty());
    }

    // ===== isPropName - 128+ char limit throws =====

    @Test
    public void testGetPropGetter_propNameExceeding128CharsThrows() {
        // isPropName throws when input propName exceeds 128 chars
        String longName = "a".repeat(129);
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(SimpleBean.class, longName));
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
    public void testGetPropField_NonExistent() {
        assertNull(Beans.getPropField(SimpleBean.class, "nonExistent"));
    }

    // ===== getPropField =====

    @Test
    public void testGetPropField_existingProp() {
        java.lang.reflect.Field field = Beans.getPropField(SimpleBean.class, "name");
        assertNotNull(field);
        assertEquals("name", field.getName());
    }

    @Test
    public void testGetPropField_nonExistentProp() {
        java.lang.reflect.Field field = Beans.getPropField(SimpleBean.class, "nonExistent");
        assertNull(field);
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
    public void testGetPropGetterSetter_NonBeanClass_Throws() {
        // Consistent with getPropField: a non-bean class throws IllegalArgumentException (instead of returning null).
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(NonBean.class, "field"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropSetter(NonBean.class, "field"));

        // A bean class with a missing property still returns null (unchanged).
        assertNull(Beans.getPropGetter(SimpleBean.class, "nonExistent"));
        assertNull(Beans.getPropSetter(SimpleBean.class, "nonExistent"));
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
    public void testGetPropFieldsReturnsAllFields() {
        ImmutableMap<String, Field> fields = Beans.getPropFields(SimpleBean.class);
        assertNotNull(fields);
        assertTrue(fields.size() >= 3);
        assertNotNull(fields.get("name"));
        assertNotNull(fields.get("age"));
        assertNotNull(fields.get("active"));
    }

    @Test
    public void testGetPropFields_EntityBean() {
        ImmutableMap<String, java.lang.reflect.Field> fields = Beans.getPropFields(EntityBean.class);
        assertNotNull(fields);
        assertNotNull(fields.get("id"));
        assertNotNull(fields.get("value"));
    }

    // ===== getPropFields =====    // ===== getPropFields returns correct fields =====

    @Test
    public void testGetPropFields_includesExpectedFields() {
        ImmutableMap<String, java.lang.reflect.Field> fields = Beans.getPropFields(SimpleBean.class);
        assertNotNull(fields);
        assertTrue(fields.containsKey("name"));
        assertTrue(fields.containsKey("age"));
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
    public void testGetPropGetMethodWithCaseVariations() {
        Method method = Beans.getPropGetter(SimpleBean.class, "NAME");
        assertNotNull(method);
        assertEquals("getName", method.getName());

        method = Beans.getPropGetter(SimpleBean.class, "NaMe");
        assertNotNull(method);

        method = Beans.getPropGetter(SimpleBean.class, "name");
        assertNotNull(method);
    }

    @Test
    public void testGetPropGetter_EntityBean() {
        java.lang.reflect.Method method = Beans.getPropGetter(EntityBean.class, "id");
        assertNotNull(method);
        assertEquals("getId", method.getName());
    }

    @Test
    public void testGetPropGetter_NonExistentProp() {
        assertNull(Beans.getPropGetter(SimpleBean.class, "doesNotExist"));
    }

    @Test
    public void testGetPropGetter_classQualifiedName() {
        // Accessing property using simple name - basic getter lookup
        Method getter = Beans.getPropGetter(SimpleBean.class, "name");
        assertNotNull(getter);
        assertEquals("getName", getter.getName());
    }

    // ===== getPropGetter with is/has/set prefix (exercises isPropName branches) =====

    @Test
    public void testGetPropGetter_withIsPrefix() {
        // "isActive" as prop name triggers the isPropName "is" prefix branch
        // The getter may or may not be found depending on the bean definition
        // The main goal is to exercise the isPropName branch without exception
        assertDoesNotThrow(() -> Beans.getPropGetter(SimpleBean.class, "isActive"));
        // The "active" property getter should always be found
        Method activeGetter = Beans.getPropGetter(SimpleBean.class, "active");
        assertNotNull(activeGetter);
    }

    @Test
    public void testGetPropGetter_withSetPrefix() {
        // "setName" as prop name triggers the isPropName "set" prefix branch
        assertDoesNotThrow(() -> Beans.getPropGetter(SimpleBean.class, "setName"));
        assertNotNull(Beans.getPropGetter(SimpleBean.class, "name"));
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
    public void testGetPropGetMethodsReturnsAllGetters() {
        ImmutableMap<String, Method> methods = Beans.getPropGetters(SimpleBean.class);
        assertNotNull(methods);
        assertTrue(methods.size() >= 3);
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
        assertNotNull(methods.get("active"));
    }

    @Test
    public void testGetPropGetters_EntityBean() {
        ImmutableMap<String, java.lang.reflect.Method> methods = Beans.getPropGetters(EntityBean.class);
        assertNotNull(methods);
        assertNotNull(methods.get("id"));
        assertNotNull(methods.get("value"));
    }

    @Test
    public void testGetPropSetMethod() {
        Method method = Beans.getPropSetter(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("setName", method.getName());

        assertNull(Beans.getPropSetter(SimpleBean.class, "nonExistent"));
    }

    @Test
    public void testGetPropSetMethodWithCaseVariations() {
        Method method = Beans.getPropSetter(SimpleBean.class, "NAME");
        assertNotNull(method);
        assertEquals("setName", method.getName());

        method = Beans.getPropSetter(SimpleBean.class, "AgE");
        assertNotNull(method);

        method = Beans.getPropSetter(SimpleBean.class, "active");
        assertNotNull(method);
    }

    @Test
    public void testGetPropSetter_EntityBean() {
        java.lang.reflect.Method method = Beans.getPropSetter(EntityBean.class, "value");
        assertNotNull(method);
        assertEquals("setValue", method.getName());
    }

    @Test
    public void testGetPropSetter_NonExistentProp() {
        assertNull(Beans.getPropSetter(SimpleBean.class, "doesNotExist"));
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
    public void testGetPropSetMethodsReturnsAllSetters() {
        ImmutableMap<String, Method> methods = Beans.getPropSetters(SimpleBean.class);
        assertNotNull(methods);
        assertTrue(methods.size() >= 3);
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
        assertNotNull(methods.get("active"));
    }

    @Test
    public void testGetPropSetters_EntityBean() {
        ImmutableMap<String, java.lang.reflect.Method> methods = Beans.getPropSetters(EntityBean.class);
        assertNotNull(methods);
        assertNotNull(methods.get("id"));
        assertNotNull(methods.get("value"));
    }

    // ===== getPropSetters =====

    @Test
    public void testGetPropSetters_returnsAllSetters() {
        ImmutableMap<String, Method> setters = Beans.getPropSetters(SimpleBean.class);
        assertNotNull(setters);
        assertTrue(setters.containsKey("name"));
        assertTrue(setters.containsKey("age"));
    }

    // ===== getPropSetters returns correct setters =====

    @Test
    public void testGetPropSetters_includesExpectedMethods() {
        ImmutableMap<String, Method> setters = Beans.getPropSetters(SimpleBean.class);
        assertNotNull(setters);
        assertTrue(setters.containsKey("name"));
        assertTrue(setters.containsKey("age"));
        assertTrue(setters.containsKey("active"));
        assertEquals("setName", setters.get("name").getName());
    }

    @Test
    public void testGetPropValue_SimpleProperty() {
        assertEquals("John", Beans.getPropValue(simpleBean, "name"));
        assertEquals(25, (int) Beans.getPropValue(simpleBean, "age"));
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
    public void testGetPropValueWithNestedPropertyPath() {
        assertEquals("New York", Beans.getPropValue(nestedBean, "address.city"));
        assertEquals("5th Avenue", Beans.getPropValue(nestedBean, "address.street"));
        assertEquals("10001", Beans.getPropValue(nestedBean, "address.zipCode"));

        assertEquals("John", Beans.getPropValue(nestedBean, "simpleBean.name"));
        assertEquals(25, (Integer) Beans.getPropValue(nestedBean, "simpleBean.age"));
    }

    @Test
    public void testGetPropValueIfPresent() {
        // present, non-null
        Nullable<String> name = Beans.getPropValueIfPresent(simpleBean, "name");
        assertTrue(name.isPresent());
        assertEquals("John", name.get());

        // present but null (active is null on a fresh SimpleBean) -> Nullable.of(null), NOT empty
        SimpleBean bean = new SimpleBean("John", 25);
        Nullable<Boolean> active = Beans.getPropValueIfPresent(bean, "active");
        assertTrue(active.isPresent());
        assertNull(active.orElseNull());

        // absent (unmatched property) -> empty
        Nullable<Object> unknown = Beans.getPropValueIfPresent(bean, "unknown");
        assertTrue(unknown.isEmpty());

        // nested, reachable leaf (nestedBean.address.city == "New York")
        Nullable<String> city = Beans.getPropValueIfPresent(nestedBean, "address.city");
        assertTrue(city.isPresent());
        assertEquals("New York", city.get());

        // nested, leaf present but null
        NestedBean nb = new NestedBean();
        nb.setAddress(new Address("NYC")); // street/zipCode null
        Nullable<String> street = Beans.getPropValueIfPresent(nb, "address.street");
        assertTrue(street.isPresent());
        assertNull(street.orElseNull());

        // nested, intermediate null -> unreachable -> empty
        NestedBean nb2 = new NestedBean(); // address == null
        assertTrue(Beans.getPropValueIfPresent(nb2, "address.city").isEmpty());

        // nested, unknown path segment -> empty
        assertTrue(Beans.getPropValueIfPresent(nb2, "address.nope").isEmpty());
    }

    @Test
    public void testGetPropValue_NestedProperty() {
        assertEquals("New York", Beans.getPropValue(nestedBean, "address.city"));
        assertEquals("5th Avenue", Beans.getPropValue(nestedBean, "address.street"));
    }

    @Test
    public void testGetPropValue_IgnoreUnmatched() {
        assertNull(Beans.getPropValue(simpleBean, "nonExistent", true));
    }

    // ===== getPropValue with nested path =====

    @Test
    public void testGetPropValue_nestedPath_nullIntermediary() {
        NestedBean bean = new NestedBean();
        bean.setId("test");
        // simpleBean is null, accessing nested prop should return default value
        Object val = Beans.getPropValue(bean, "simpleBean.name", true);
        assertNull(val); // null intermediary returns default
    }

    @Test
    public void testGetPropValue_nestedPath_nonExistentIgnored() {
        SimpleBean bean = new SimpleBean("Nina", 25);
        Object val = Beans.getPropValue(bean, "nonexistent.field", true);
        assertNull(val);
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
    public void testGetPropValueWithMethod() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        Object value = Beans.getPropValue(simpleBean, getName);
        assertEquals("John", value);

        Method getAge = SimpleBean.class.getMethod("getAge");
        value = Beans.getPropValue(simpleBean, getAge);
        assertEquals(25, value);
    }

    @Test
    public void testGetPropValue_WithMethod() throws Exception {
        java.lang.reflect.Method getAge = SimpleBean.class.getMethod("getAge");
        int age = Beans.getPropValue(simpleBean, getAge);
        assertEquals(25, age);
    }

    @Test
    public void testGetPropValue_ThrowOnUnmatched() {
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(simpleBean, "nonExistent", false));
    }

    @Test
    public void testSetPropValue_StringPropName_WithTypeConversion() {
        SimpleBean bean = new SimpleBean();
        Beans.setPropValue(bean, "age", 999);
        assertEquals(999, bean.getAge());

        Beans.setPropValue(bean, "active", true);
        assertEquals(true, bean.getActive());
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

    @Test
    public void testSetPropValue_NestedProperty() {
        Beans.setPropValue(nestedBean, "address.city", "Boston");
        assertEquals("Boston", nestedBean.getAddress().getCity());
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
        Beans.setPropValueByGetter(bean, getItems, newList);

        assertEquals(2, bean.getItems().size());
        assertEquals("new1", bean.getItems().get(0));
        assertEquals("new2", bean.getItems().get(1));

        Beans.setPropValueByGetter(bean, getItems, null);
        assertEquals(2, bean.getItems().size());
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
        Beans.setPropValueByGetter(bean, getItems, newItems);

        assertEquals(3, bean.getItems().size());
        assertTrue(bean.getItems().contains("new1"));
        assertTrue(bean.getItems().contains("new2"));
        assertTrue(bean.getItems().contains("new3"));

        Beans.setPropValueByGetter(bean, getItems, Collections.emptyList());
        assertEquals(0, bean.getItems().size());
    }

    @Test
    public void testSetPropValue_WithMethod() throws Exception {
        SimpleBean bean = new SimpleBean();
        java.lang.reflect.Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        Beans.setPropValue(bean, setAge, 42);
        assertEquals(42, bean.getAge());
    }

    // ===== setPropValue(bean, method, value) with null =====

    @Test
    public void testSetPropValueByMethod_nullValue() throws Exception {
        SimpleBean bean = new SimpleBean("Alice", 30);
        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.setPropValue(bean, setName, null);
        assertNull(bean.getName());
    }

    @Test
    public void testSetPropValueByMethod_normalValue() throws Exception {
        SimpleBean bean = new SimpleBean("Alice", 30);
        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        Beans.setPropValue(bean, setAge, 42);
        assertEquals(42, bean.getAge());
    }

    // ===== setPropValue(Object, Method, Object) - null value for primitive type =====

    @Test
    public void testSetPropValue_byMethod_nullValueForPrimitive() throws Exception {
        SimpleBean bean = new SimpleBean("Kate", 30);
        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        // null value for primitive should set default (0)
        Beans.setPropValue(bean, setAge, null);
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testSetPropValue_byMethod_nonNullValue() throws Exception {
        SimpleBean bean = new SimpleBean("Luke", 20);
        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.setPropValue(bean, setName, "Updated");
        assertEquals("Updated", bean.getName());
    }

    @Test
    public void testSetPropValueByGetter_NullValue() throws Exception {
        CollectionBean bean = new CollectionBean();
        List<String> initial = new ArrayList<>();
        initial.add("item1");
        bean.setItems(initial);

        java.lang.reflect.Method getItems = CollectionBean.class.getMethod("getItems");
        Beans.setPropValueByGetter(bean, getItems, null);
        // Setting null through getter for collection should clear it (not set null)
        assertNotNull(bean.getItems());
    }

    // ===== setPropValueByGetter =====

    @Test
    public void testSetPropValueByGetter_collection() throws Exception {
        CollectionBean bean = new CollectionBean();
        bean.setItems(new ArrayList<>(java.util.Arrays.asList("a", "b")));
        Method getter = CollectionBean.class.getMethod("getItems");
        Beans.setPropValueByGetter(bean, getter, java.util.Arrays.asList("x", "y", "z"));
        assertEquals(3, bean.getItems().size());
        assertTrue(bean.getItems().contains("x"));
    }

    @Test
    public void testSetPropValueByGetter_nullValueIsNoOp() throws Exception {
        CollectionBean bean = new CollectionBean();
        bean.setItems(new ArrayList<>(java.util.Arrays.asList("a")));
        Method getter = CollectionBean.class.getMethod("getItems");
        Beans.setPropValueByGetter(bean, getter, null);
        assertEquals(1, bean.getItems().size());
    }

    // ===== setPropValueByGetter - throws for non-Collection non-Map return type =====

    @Test
    public void testSetPropValueByGetter_throwsForNonCollectionReturn() throws Exception {
        SimpleBean bean = new SimpleBean("Vera", 25);
        Method getter = SimpleBean.class.getMethod("getName");
        // getName() returns String, not Collection or Map - should throw
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValueByGetter(bean, getter, "newValue"));
    }

    @Test
    public void testFormalizePropName() {
        assertEquals("userName", Beans.normalizePropName("user_name"));
        assertEquals("firstName", Beans.normalizePropName("first_name"));
        assertEquals("clazz", Beans.normalizePropName("class"));
        assertEquals("id", Beans.normalizePropName("ID"));
        assertEquals("addressLine1", Beans.normalizePropName("address_line_1"));
    }

    @Test
    public void testNormalizePropName_ClassKeyword() {
        assertEquals("clazz", Beans.normalizePropName("class"));
        assertEquals("clazz", Beans.normalizePropName("CLASS"));
    }

    @Test
    public void testNormalizePropName_WithUnderscores() {
        assertEquals("myPropertyName", Beans.normalizePropName("my_property_name"));
        assertEquals("id", Beans.normalizePropName("id"));
    }

    @Test
    public void testFormalizePropNameWithVariousCases() {
        assertEquals("userName", Beans.normalizePropName("user_name"));
        assertEquals("firstName", Beans.normalizePropName("first_name"));

        assertEquals("clazz", Beans.normalizePropName("class"));

        assertEquals("id", Beans.normalizePropName("ID"));
        assertEquals("url", Beans.normalizePropName("URL"));

        assertEquals("userIdValue", Beans.normalizePropName("user_id_value"));
        assertEquals("addressLine1", Beans.normalizePropName("address_line_1"));

        assertEquals("simple", Beans.normalizePropName("simple"));
        assertEquals("alreadyCamel", Beans.normalizePropName("alreadyCamel"));
    }

    @Test
    public void testNormalizePropName_AlreadyCamelCase() {
        assertEquals("alreadyCamel", Beans.normalizePropName("alreadyCamel"));
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
    public void testToCamelCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_name", "John");
        map.put("first_name", "Jane");

        Maps.replaceKeysWithCamelCase(map);

        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("firstName"));
        assertFalse(map.containsKey("user_name"));
        assertEquals("John", map.get("userName"));
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
    public void testToCamelCaseMapTransformation() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_name", "John");
        map.put("first_name", "Jane");
        map.put("USER_ID", 123);
        map.put("address-line", "Main St");

        Maps.replaceKeysWithCamelCase(map);

        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("firstName"));
        assertTrue(map.containsKey("userId"));
        assertTrue(map.containsKey("addressLine"));

        assertFalse(map.containsKey("user_name"));
        assertFalse(map.containsKey("first_name"));
        assertFalse(map.containsKey("USER_ID"));
        assertFalse(map.containsKey("address-line"));

        Map<String, Object> emptyMap = new HashMap<>();
        Maps.replaceKeysWithCamelCase(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testToCamelCase_NullInput() {
        assertNull(Beans.toCamelCase((String) null));
    }

    @Test
    public void testToCamelCase_EmptyInput() {
        assertEquals("", Beans.toCamelCase(""));
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
    public void testToSnakeCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");

        Maps.replaceKeysWithSnakeCase(map);

        assertTrue(map.containsKey("user_name"));
        assertTrue(map.containsKey("first_name"));
        assertFalse(map.containsKey("userName"));
    }

    @Test
    public void testToSnakeCaseVariants() {
        assertEquals("user_name", Beans.toSnakeCase("userName"));
        assertEquals("first_name", Beans.toSnakeCase("firstName"));

        assertEquals("user_name", Beans.toSnakeCase("UserName"));

        assertEquals("user_id", Beans.toSnakeCase("userID"));
        assertEquals("url", Beans.toSnakeCase("URL"));

        assertEquals("simple", Beans.toSnakeCase("simple"));

        assertEquals("a", Beans.toSnakeCase("a"));
        assertEquals("a", Beans.toSnakeCase("A"));

        assertEquals("", Beans.toSnakeCase(""));
        assertNull(Beans.toSnakeCase((String) null));
    }

    @Test
    public void testToSnakeCaseMapTransformation() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");
        map.put("userId", 123);
        map.put("addressLine", "Main St");

        Maps.replaceKeysWithSnakeCase(map);

        assertTrue(map.containsKey("user_name"));
        assertTrue(map.containsKey("first_name"));
        assertTrue(map.containsKey("user_id"));
        assertTrue(map.containsKey("address_line"));

        assertFalse(map.containsKey("userName"));
        assertFalse(map.containsKey("firstName"));
    }

    @Test
    public void testToSnakeCase_SingleChar() {
        assertEquals("a", Beans.toSnakeCase("a"));
        assertEquals("a", Beans.toSnakeCase("A"));
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
    public void testToScreamingSnakeCaseMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");

        Maps.replaceKeysWithScreamingSnakeCase(map);

        assertTrue(map.containsKey("USER_NAME"));
        assertTrue(map.containsKey("FIRST_NAME"));
        assertFalse(map.containsKey("userName"));
    }

    @Test
    public void testToScreamingSnakeCaseVariants() {
        assertEquals("USER_NAME", Beans.toScreamingSnakeCase("userName"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("firstName"));

        assertEquals("USER_NAME", Beans.toScreamingSnakeCase("UserName"));

        assertEquals("USER_ID", Beans.toScreamingSnakeCase("userID"));
        assertEquals("URL", Beans.toScreamingSnakeCase("URL"));

        assertEquals("SIMPLE", Beans.toScreamingSnakeCase("SIMPLE"));

        assertEquals("A", Beans.toScreamingSnakeCase("a"));
        assertEquals("A", Beans.toScreamingSnakeCase("A"));

        assertEquals("", Beans.toScreamingSnakeCase(""));
        assertNull(Beans.toScreamingSnakeCase((String) null));
    }

    @Test
    public void testToScreamingSnakeCaseMapTransformation() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("firstName", "Jane");
        map.put("userId", 123);

        Maps.replaceKeysWithScreamingSnakeCase(map);

        assertTrue(map.containsKey("USER_NAME"));
        assertTrue(map.containsKey("FIRST_NAME"));
        assertTrue(map.containsKey("USER_ID"));

        assertFalse(map.containsKey("userName"));
    }

    @Test
    public void testToScreamingSnakeCase_SingleChar() {
        assertEquals("A", Beans.toScreamingSnakeCase("a"));
        assertEquals("A", Beans.toScreamingSnakeCase("A"));
    }

    @Test
    public void testReplaceKeysWithCamelCase_EmptyMap() {
        Map<String, Object> map = new HashMap<>();
        Maps.replaceKeysWithCamelCase(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeysWithCamelCase_NestedMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("user_name", "John");
        Map<String, Object> nested = new HashMap<>();
        nested.put("first_name", "John");
        map.put("inner_data", nested);

        Maps.replaceKeysWithCamelCase(map);
        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("innerData"));
    }

    @Test
    public void testReplaceKeysWithSnakeCase_EmptyMap() {
        Map<String, Object> map = new HashMap<>();
        Maps.replaceKeysWithSnakeCase(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeysWithSnakeCase_AlreadySnakeCase() {
        Map<String, Object> map = new HashMap<>();
        map.put("already_snake", "value");

        Maps.replaceKeysWithSnakeCase(map);
        assertTrue(map.containsKey("already_snake"));
        assertEquals("value", map.get("already_snake"));
    }

    @Test
    public void testReplaceKeysWithScreamingSnakeCase_EmptyMap() {
        Map<String, Object> map = new HashMap<>();
        Maps.replaceKeysWithScreamingSnakeCase(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testReplaceKeysWithScreamingSnakeCase_MixedKeys() {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", "John");
        map.put("already_snake", "value");

        Maps.replaceKeysWithScreamingSnakeCase(map);
        assertTrue(map.containsKey("USER_NAME"));
        assertTrue(map.containsKey("ALREADY_SNAKE"));
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

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals("Jane", beans.get(1).getName());

        beans = Beans.mapsToBeans(mapList, Arrays.asList("name"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
    }

    @Test
    public void testMapToBean_WithSelectProps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Test");
        map.put("age", 99);

        SimpleBean bean = Beans.mapToBean(map, Arrays.asList("name"), SimpleBean.class);
        assertEquals("Test", bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testMapToBean_MapList_Basic() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "Eve");
        map1.put("age", 22);
        mapList.add(map1);

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, SimpleBean.class);
        assertEquals(1, beans.size());
        assertEquals("Eve", beans.get(0).getName());
        assertEquals(22, beans.get(0).getAge());
    }

    // ===== mapToBean(Collection<Map>, Collection<String>, Class) =====

    @Test
    public void testMapToBean_mapList_withSelectPropNames() {
        Map<String, Object> m1 = new HashMap<>();
        m1.put("name", "Xena");
        m1.put("age", 28);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("name", "Yuri");
        m2.put("age", 32);

        List<Map<String, Object>> mapList = Arrays.asList(m1, m2);
        List<SimpleBean> beans = Beans.mapsToBeans(mapList, Arrays.asList("name"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("Xena", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge()); // age not selected
        assertEquals("Yuri", beans.get(1).getName());
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
    }

    @Test
    public void testMap2BeanWithOptions() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane");
        map.put("age", 30);
        map.put("active", null);

        SimpleBean bean = Beans.mapToBean(map, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(30, bean.getAge());
        assertNull(bean.getActive());

        bean = Beans.mapToBean(map, Arrays.asList("name", "age"), SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(30, bean.getAge());
    }

    @Test
    public void testMap2BeanListWithOptions() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", null);
        mapList.add(map1);

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, true, SimpleBean.class);
        assertEquals(1, beans.size());
        assertEquals("John", beans.get(0).getName());
    }

    @Test
    public void testMap2BeanWithIgnoreOptions() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane");
        map.put("age", null);
        map.put("unknownField", "value");

        SimpleBean bean = Beans.mapToBean(map, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertNull(bean.getActive());

        map.put("age", 25);
        bean = Beans.mapToBean(map, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(25, bean.getAge());
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

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, Arrays.asList("name", "age"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(25, beans.get(0).getAge());
        assertNull(beans.get(0).getActive());

        assertEquals("Jane", beans.get(1).getName());
        assertNull(beans.get(1).getActive());
    }

    @Test
    public void testNullHandlingInConversions() {
        SimpleBean bean = Beans.mapToBean((Map<String, Object>) null, SimpleBean.class);
        assertNull(bean);

        Map<String, Object> map = Beans.beanToMap((Object) null);
        assertTrue(map.isEmpty());

        map = Beans.deepBeanToMap((Object) null);
        assertTrue(map.isEmpty());

        map = Beans.beanToFlatMap((Object) null);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMap2BeanWithSelectPropsOnly() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Selected");
        map.put("age", 99);
        map.put("active", true);

        SimpleBean bean = Beans.mapToBean(map, Arrays.asList("name"), SimpleBean.class);
        assertEquals("Selected", bean.getName());
        assertEquals(0, bean.getAge());

        bean = Beans.mapToBean(map, Arrays.asList("name", "age"), SimpleBean.class);
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

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, true, SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
        assertNull(beans.get(1).getName());
        assertEquals(25, beans.get(1).getAge());
    }

    @Test
    public void testMapToBean_NullMap() {
        assertNull(Beans.mapToBean((Map<String, Object>) null, SimpleBean.class));
    }

    @Test
    public void testMapToBean_EmptyMap() {
        Map<String, Object> map = new HashMap<>();
        SimpleBean bean = Beans.mapToBean(map, SimpleBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testMapToBean_IgnoreNull_IgnoreUnmatched() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Test");
        map.put("age", null);
        map.put("unknownField", "value");

        SimpleBean bean = Beans.mapToBean(map, true, SimpleBean.class);
        assertEquals("Test", bean.getName());
    }

    @Test
    public void testMapToBean_MapList_SelectProps() {
        List<Map<String, Object>> mapList = new ArrayList<>();

        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "Alice");
        map1.put("age", 30);
        map1.put("active", true);
        mapList.add(map1);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Bob");
        map2.put("age", 40);
        map2.put("active", false);
        mapList.add(map2);

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, Arrays.asList("name"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("Alice", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
        assertNull(beans.get(0).getActive());
        assertEquals("Bob", beans.get(1).getName());
    }

    @Test
    public void testMapToBean_MapList_Empty() {
        List<SimpleBean> beans = Beans.mapsToBeans(Collections.emptyList(), SimpleBean.class);
        assertNotNull(beans);
        assertTrue(beans.isEmpty());
    }

    // ===== mapToBean with selectPropNames =====

    @Test
    public void testMapToBean_withSelectPropNames_basic() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Bob");
        map.put("age", 22);
        List<String> select = java.util.Arrays.asList("name");
        SimpleBean bean = Beans.mapToBean(map, select, SimpleBean.class);
        assertNotNull(bean);
        assertEquals("Bob", bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testMapToBean_SelectPropNames_NullMeansAllEmptyMeansNone() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Nora");
        map.put("age", 31);
        map.put("active", true);

        SimpleBean bean = Beans.mapToBean(map, (Collection<String>) null, SimpleBean.class);
        assertEquals("Nora", bean.getName());
        assertEquals(31, bean.getAge());
        assertEquals(true, bean.getActive());

        bean = Beans.mapToBean(map, Collections.emptyList(), SimpleBean.class);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testMapToBean_MapList_SelectPropNames_NullMeansAllEmptyMeansNone() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Owen");
        map.put("age", 42);
        map.put("active", false);

        List<Map<String, Object>> mapList = Collections.singletonList(map);

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, (Collection<String>) null, SimpleBean.class);
        assertEquals(1, beans.size());
        assertEquals("Owen", beans.get(0).getName());
        assertEquals(42, beans.get(0).getAge());
        assertEquals(false, beans.get(0).getActive());

        beans = Beans.mapsToBeans(mapList, Collections.emptyList(), SimpleBean.class);
        assertEquals(1, beans.size());
        assertNull(beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
        assertNull(beans.get(0).getActive());
    }

    @Test
    public void testMapToBean_withSelectPropNames_nullMap() {
        List<String> select = java.util.Arrays.asList("name");
        SimpleBean bean = Beans.mapToBean((Map<String, Object>) null, select, SimpleBean.class);
        assertNull(bean);
    }

    // ===== mapToBean(Map, Collection<String>, Class) with nested bean in map =====

    @Test
    public void testMapToBean_selectPropNames_withBeanValueAsMap() {
        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("city", "Boston");
        addressMap.put("street", "Elm St");

        Map<String, Object> map = new HashMap<>();
        map.put("id", "456");
        map.put("address", addressMap);

        List<String> select = Arrays.asList("id", "address");
        NestedBean bean = Beans.mapToBean(map, select, NestedBean.class);
        assertNotNull(bean);
        assertEquals("456", bean.getId());
        // address is a bean type, and the map value is a Map - it should be mapped
        assertNotNull(bean.getAddress());
        assertEquals("Boston", bean.getAddress().getCity());
    }

    @Test
    public void testMapToBean_selectPropNames_subsetOnly() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Mia");
        map.put("age", 27);

        // Only select "name", age should remain default
        SimpleBean bean = Beans.mapToBean(map, Arrays.asList("name"), SimpleBean.class);
        assertNotNull(bean);
        assertEquals("Mia", bean.getName());
        assertEquals(0, bean.getAge());
    }

    // ===== mapToBean(Map, Collection, Class) - propInfo null path =====

    @Test
    public void testMapToBean_selectPropNames_withUnrecognizedPropName() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Wendy");
        // "unknownProp" is in selectPropNames but not a valid bean property
        // propInfo will be null, and beanInfo.setPropValue is called with ignoreUnmatched=false
        // This tests the null propInfo branch
        List<String> select = Arrays.asList("name");
        SimpleBean bean = Beans.mapToBean(map, select, SimpleBean.class);
        assertNotNull(bean);
        assertEquals("Wendy", bean.getName());
    }

    // ===== flatMapToBean (inverse of beanToFlatMap) - is it already supported? =====
    // YES: mapToBean delegates unmatched (dotted) keys to BeanInfo.setPropValue, which resolves dotted
    // property names via getPropInfoChain, creating intermediate beans. So a beanToFlatMap -> mapToBean
    // round-trip reconstructs the nested bean; no dedicated flatMapToBean method is required.
    @Test
    public void testFlatMapToBean_roundTripViaMapToBean() {
        final Map<String, Object> flat = Beans.beanToFlatMap(nestedBean);

        // sanity: this really is a FLAT (dotted-key) map, not nested sub-maps
        assertEquals("New York", flat.get("address.city"));
        assertEquals("John", flat.get("simpleBean.name"));
        assertFalse(flat.get("address") instanceof Map);
        assertFalse(flat.get("simpleBean") instanceof Map);

        final NestedBean restored = Beans.mapToBean(flat, NestedBean.class);

        assertNotNull(restored);
        assertEquals("123", restored.getId());
        assertNotNull(restored.getSimpleBean());
        assertEquals("John", restored.getSimpleBean().getName());
        assertEquals(25, restored.getSimpleBean().getAge());
        assertTrue(restored.getSimpleBean().getActive());
        assertNotNull(restored.getAddress());
        assertEquals("New York", restored.getAddress().getCity());
        assertEquals("5th Avenue", restored.getAddress().getStreet());
        assertEquals("10001", restored.getAddress().getZipCode());
    }

    @Test
    public void testMapToBean_dottedKeys_buildsNestedBeans() {
        // A flat map with dotted keys assembled by hand (independent of beanToFlatMap) is reconstructed
        // into the nested bean graph by mapToBean - intermediate beans (address, simpleBean) are created.
        final Map<String, Object> flat = new LinkedHashMap<>();
        flat.put("id", "789");
        flat.put("address.city", "Boston");
        flat.put("address.zipCode", "02101");
        flat.put("simpleBean.name", "Ann");
        flat.put("simpleBean.age", 40);

        final NestedBean bean = Beans.mapToBean(flat, NestedBean.class);

        assertEquals("789", bean.getId());
        assertNotNull(bean.getAddress());
        assertEquals("Boston", bean.getAddress().getCity());
        assertEquals("02101", bean.getAddress().getZipCode());
        assertNotNull(bean.getSimpleBean());
        assertEquals("Ann", bean.getSimpleBean().getName());
        assertEquals(40, bean.getSimpleBean().getAge());
    }

    @Test
    public void testBean2MapWithOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, output);

        assertEquals("John", output.get("name"));
        assertEquals(25, output.get("age"));
        assertEquals(true, output.get("active"));

        output.clear();
        Beans.beanToMap(simpleBean, Arrays.asList("name"), output);
        assertEquals(1, output.size());
        assertTrue(output.containsKey("name"));
    }

    @Test
    public void testBean2MapWithMapSupplierOnly() {
        Map<String, Object> linkedMap = Beans.beanToMap(simpleBean, IntFunctions.ofLinkedHashMap());
        assertTrue(linkedMap instanceof LinkedHashMap);
        assertEquals("John", linkedMap.get("name"));

        TreeMap<String, Object> treeMap = Beans.beanToMap(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("John", treeMap.get("name"));
    }

    @Test
    public void testBean2MapWithSelectPropsOnly() {
        Map<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));

        map = Beans.beanToMap(simpleBean, Arrays.asList("name", "active"));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_SelectPropNames_NullMeansAllEmptyMeansNone() {
        Map<String, Object> map = Beans.beanToMap(simpleBean, (Collection<String>) null);
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertTrue(map.containsKey("active"));

        map = Beans.beanToMap(simpleBean, Collections.emptyList());
        assertTrue(map.isEmpty());

        map = Beans.beanToMap(simpleBean, Collections.emptyList(), IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertTrue(map.isEmpty());

        map = Beans.beanToMap(simpleBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.isEmpty());
    }

    // ===== 5.2 fluent conversion builder: Beans.mapBuilder(bean) =====

    @Test
    public void testToMapBuilder_basicSelectExclude() {
        // simpleBean: name="John", age=25, active=true. Default: all props, camelCase, shallow, LinkedHashMap.
        Map<String, Object> all = Beans.mapBuilder(simpleBean).toMap();
        assertTrue(all instanceof LinkedHashMap);
        assertEquals(3, all.size());
        assertEquals("John", all.get("name"));
        assertEquals(25, all.get("age"));
        assertEquals(true, all.get("active"));

        Map<String, Object> sel = Beans.mapBuilder(simpleBean).select("name", "age").toMap();
        assertEquals(2, sel.size());
        assertTrue(sel.containsKey("name"));
        assertTrue(sel.containsKey("age"));

        Map<String, Object> exc = Beans.mapBuilder(simpleBean).exclude("active").toMap();
        assertEquals(2, exc.size());
        assertFalse(exc.containsKey("active"));

        // a selected property that does not exist -> IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Beans.mapBuilder(simpleBean).select("nope").toMap());
    }

    @Test
    public void testToMapBuilder_filterAndSkipNulls() {
        SimpleBean bean = new SimpleBean("John", 25); // active defaults to null

        // filter: a plain inline lambda works (filter is a builder method, not an overload -> no ambiguity)
        Map<String, Object> strings = Beans.mapBuilder(bean).filter((name, value) -> value instanceof String).toMap();
        assertEquals(1, strings.size());
        assertEquals("John", strings.get("name"));

        // skipNulls drops the null "active"
        Map<String, Object> nonNull = Beans.mapBuilder(bean).skipNulls().toMap();
        assertEquals(2, nonNull.size());
        assertFalse(nonNull.containsKey("active"));

        // nulls are kept by default
        Map<String, Object> withNulls = Beans.mapBuilder(bean).toMap();
        assertEquals(3, withNulls.size());
        assertTrue(withNulls.containsKey("active"));
        assertNull(withNulls.get("active"));
    }

    @Test
    public void testToMapBuilder_namingSupplierInto() {
        NestedBean nb = new NestedBean();
        nb.setSimpleBean(new SimpleBean("x", 1));

        // naming applies to keys: simpleBean -> simple_bean
        Map<String, Object> snake = Beans.mapBuilder(nb).select("simpleBean").naming(NamingPolicy.SNAKE_CASE).toMap();
        assertTrue(snake.containsKey("simple_bean"));

        // custom supplier -> TreeMap
        Map<String, Object> tree = Beans.mapBuilder(simpleBean).toMap(size -> new java.util.TreeMap<>());
        assertTrue(tree instanceof java.util.TreeMap);
        assertEquals(3, tree.size());

        // into(existing) fills and returns the same map, preserving existing entries
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("id", 1);
        Map<String, Object> filled = Beans.mapBuilder(simpleBean).select("name").into(existing);
        assertTrue(existing == filled);
        assertEquals(1, existing.get("id"));
        assertEquals("John", existing.get("name"));
    }

    @Test
    public void testToMapBuilder_deepAndFlat() {
        NestedBean bean = new NestedBean();
        bean.setId("u1");
        bean.setAddress(new Address("NYC")); // city="NYC", street/zipCode null
        bean.setSimpleBean(null);
        bean.setTags(null);

        // deep: nested Address -> nested map (nested nulls omitted)
        Map<String, Object> deep = Beans.mapBuilder(bean).skipNulls().deep().toMap();
        assertEquals("u1", deep.get("id"));
        assertTrue(deep.get("address") instanceof Map);
        assertEquals("NYC", ((Map<?, ?>) deep.get("address")).get("city"));

        // flat: dotted keys
        Map<String, Object> flat = Beans.mapBuilder(bean).skipNulls().flat().toMap();
        assertEquals("u1", flat.get("id"));
        assertEquals("NYC", flat.get("address.city"));

        // deep()/flat() are mutually exclusive; calling both (either order) throws IllegalStateException.
        assertThrows(IllegalStateException.class, () -> Beans.mapBuilder(bean).flat().deep());
        assertThrows(IllegalStateException.class, () -> Beans.mapBuilder(bean).deep().flat());

        // calling the same mode more than once is harmless
        Map<String, Object> repeated = Beans.mapBuilder(bean).skipNulls().deep().deep().toMap();
        assertTrue(repeated.get("address") instanceof Map);
    }

    @Test
    public void testToMapBuilder_nullBean() {
        assertTrue(Beans.mapBuilder((Object) null).toMap().isEmpty());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("x", 1);
        assertTrue(Beans.mapBuilder((Object) null).into(out) == out);
        assertEquals(1, out.size());
    }

    @Test
    public void testBeanToMap_SelectPropNames_OutputEmptySelectDoesNotModifyOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("existing", "value");

        Beans.beanToMap(simpleBean, Collections.emptyList(), output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));

        Beans.beanToMap(simpleBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));
    }

    @Test
    public void testBean2MapWithOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, output);

        assertTrue(output.size() >= 3);
        assertEquals("John", output.get("name"));
        assertEquals(25, output.get("age"));
    }

    @Test
    public void testBeanToMap_SelectProps_Output() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, Arrays.asList("name"), output);
        assertEquals(1, output.size());
        assertEquals("John", output.get("name"));
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
    public void testBean2MapWithIgnoredAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());

        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
    }

    @Test
    public void testBean2MapWithMapSupplier() {
        TreeMap<String, Object> treeMap = Beans.beanToMap(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("John", treeMap.get("name"));
        assertEquals(25, treeMap.get("age"));

        Map<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), NamingPolicy.SNAKE_CASE, IntFunctions.ofLinkedHashMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithNamingPolicyAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), NamingPolicy.SCREAMING_SNAKE_CASE, output);

        assertTrue(output.containsKey("NAME") || output.containsKey("name"));
        assertTrue(output.containsKey("AGE") || output.containsKey("age"));
    }

    @Test
    public void testBean2MapWithIgnoredPropsAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());

        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullAndOutput() {
        simpleBean.setActive(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.beanToMap(simpleBean, true, output);
        assertFalse(output.containsKey("active"));
        assertTrue(output.containsKey("name"));

        output.clear();
        Set<String> ignored = new HashSet<>();
        ignored.add("age");
        Beans.beanToMap(simpleBean, true, ignored, output);
        assertFalse(output.containsKey("age"));
        assertFalse(output.containsKey("active"));
        assertTrue(output.containsKey("name"));
    }

    @Test
    public void testBean2MapWithIgnoredPropsNamingAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE, output);

        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
        assertTrue(output.containsKey("active"));
    }

    @Test
    public void testBeanFieldAccessWithoutGetterSetter() {
        CollectionBean bean = new CollectionBean();
        List<String> items = Arrays.asList("item1", "item2");
        bean.setItems(items);

        Map<String, Object> map = Beans.beanToMap(bean);
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

        Map<String, Object> lowerMap = Beans.beanToMap(bean, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(lowerMap.containsKey("first_name") || lowerMap.containsKey("firstName"));
        assertTrue(lowerMap.containsKey("last_name") || lowerMap.containsKey("lastName"));

        Map<String, Object> upperMap = Beans.beanToMap(bean, null, NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(upperMap.size() > 0);
    }

    @Test
    public void testBean2MapWithSelectPropsAndSupplier() {
        TreeMap<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), NamingPolicy.NO_CHANGE, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithSelectPropsAndNamingAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), NamingPolicy.SNAKE_CASE, output);

        assertTrue(output.size() >= 2);
        assertFalse(output.containsKey("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullOnly() {
        simpleBean.setActive(null);

        Map<String, Object> map = Beans.beanToMap(simpleBean, true);
        assertFalse(map.containsKey("active"));
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));

        map = Beans.beanToMap(simpleBean, false);
        assertTrue(map.containsKey("active"));
        assertNull(map.get("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullAndIgnoredProps() {
        simpleBean.setActive(null);
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.beanToMap(simpleBean, true, ignored);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithIgnoreNullIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        TreeMap<String, Object> map = Beans.beanToMap(simpleBean, false, ignored, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithAllParametersAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("active");

        Map<String, Object> map = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());

        assertFalse(map.containsKey("active"));
        assertTrue(map.size() >= 2);
    }

    @Test
    public void testBean2MapWithIgnoreNullAndOutputOnly() {
        simpleBean.setActive(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.beanToMap(simpleBean, true, output);
        assertFalse(output.containsKey("active"));
        assertTrue(output.containsKey("name"));
    }

    @Test
    public void testBean2MapWithIgnoreNullIgnoredPropsAndOutputOnly() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, false, ignored, output);

        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
        assertTrue(output.containsKey("active"));
    }

    @Test
    public void testBean2MapWithAllParametersAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, true, ignored, NamingPolicy.SNAKE_CASE, output);

        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
    }

    // --- Missing overload tests ---

    @Test
    public void testBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy_WithNull() {
        simpleBean.setActive(null);
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> map = Beans.beanToMap(simpleBean, true, ignored, NamingPolicy.SNAKE_CASE);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_SelectProps_NamingPolicy_MapSupplier() {
        TreeMap<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), NamingPolicy.NO_CHANGE, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
    }

    @Test
    public void testBean2MapWithSelectPropsNamingMapSupplierForAll() {
        Map<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name"), NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofLinkedHashMap());
        assertTrue(map.containsKey("NAME") || map.containsKey("name"));
        assertEquals(1, map.size());
    }

    @Test
    public void testBeanToMap_NullBean_ReturnsEmptyMap() {
        Map<String, Object> map = Beans.beanToMap((Object) null, true, null, NamingPolicy.NO_CHANGE);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testBeanToMap_IgnoreNull_IgnoredProps_MapSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("active");

        Map<String, Object> map = Beans.beanToMap(simpleBean, false, ignored, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy_MapSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        TreeMap<String, Object> map = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("name"));
    }

    @Test
    public void testBeanToMap_IgnoreNull_Output() {
        simpleBean.setActive(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.beanToMap(simpleBean, false, output);
        assertTrue(output.containsKey("active"));
        assertNull(output.get("active"));
    }

    @Test
    public void testBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy_Output() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
    }

    @Test
    public void testBeanToMap_NullBean() {
        Map<String, Object> result = Beans.beanToMap((Object) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBeanToMap_WithNamingPolicy_NoChange() {
        Map<String, Object> map = Beans.beanToMap(simpleBean, null, NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertTrue(map.containsKey("active"));
    }

    // ========== Additional missing tests ==========

    @Test
    public void testBeanToMap_SelectProps_MapSupplier_NoNamingPolicy() {
        TreeMap<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_SelectProps_MapSupplier_NullSelectProps() {
        Map<String, Object> map = Beans.beanToMap(simpleBean, (Collection<String>) null, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_NullBean_WithMapSupplier() {
        Map<String, Object> map = Beans.beanToMap((Object) null, IntFunctions.ofLinkedHashMap());
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    // ===== beanToMap(Object, boolean, Set, NamingPolicy, Map) =====

    @Test
    public void testBeanToMap_ignoreNull_namingPolicy_output() {
        SimpleBean bean = new SimpleBean("Alice", 30);
        bean.setActive(null);
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(bean, true, null, NamingPolicy.CAMEL_CASE, output);
        assertEquals("Alice", output.get("name"));
        assertEquals(30, output.get("age"));
        assertFalse(output.containsKey("active"));
    }

    @Test
    public void testBeanToMap_noIgnoreNull_namingPolicy_output() {
        SimpleBean bean = new SimpleBean("Bob", 25);
        bean.setActive(null);
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(bean, false, null, NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.containsKey("active"));
        assertNull(output.get("active"));
    }

    @Test
    public void testBeanToMap_withIgnoredPropNames_output() {
        SimpleBean bean = new SimpleBean("Carol", 40);
        bean.setActive(true);
        Set<String> ignored = new HashSet<>(Arrays.asList("age", "active"));
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(bean, false, ignored, NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));
        assertFalse(output.containsKey("active"));
    }

    @Test
    public void testBeanToMap_snakeCaseNaming_output() {
        SimpleBean bean = new SimpleBean("Dave", 35);
        bean.setActive(true);
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(bean, false, null, NamingPolicy.SNAKE_CASE, output);
        assertNotNull(output);
        assertTrue(output.containsKey("name") || output.containsKey("name"));
    }

    @Test
    public void testBeanToMap_nullBean_output() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(null, false, null, NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    // ===== beanToMap with namingPolicy variations =====

    @Test
    public void testBeanToMap_noChangeNaming_output() {
        SimpleBean bean = new SimpleBean("Oscar", 44);
        bean.setActive(true);
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(bean, false, null, NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("name"));
        assertEquals("Oscar", output.get("name"));
    }

    // ===== beanToMap(bean, selectProps, namingPolicy, output) =====

    @Test
    public void testBeanToMap_selectProps_snakeCase_output() {
        SimpleBean bean = new SimpleBean("Charlie", 35);
        Map<String, Object> output = new LinkedHashMap<>();
        List<String> select = java.util.Arrays.asList("name", "age");
        Beans.beanToMap(bean, select, NamingPolicy.SCREAMING_SNAKE_CASE, output);
        assertTrue(output.containsKey("NAME"));
        assertEquals("Charlie", output.get("NAME"));
    }

    @Test
    public void testBeanToMap_selectProps_output_nullBean() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(null, java.util.Arrays.asList("name"), NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    // ===== beanToMap(Object, Collection, NamingPolicy, Map) throws on invalid prop (L3134) =====

    @Test
    public void testBeanToMap_InvalidPropName_Throws() {
        Map<String, Object> output = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap(simpleBean, Arrays.asList("nonExistentProp"), NamingPolicy.NO_CHANGE, output));
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
    public void testDeepBean2MapWithOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, output);

        assertEquals("123", output.get("id"));
        assertTrue(output.get("simpleBean") instanceof Map);

        output.clear();
        Beans.deepBeanToMap(nestedBean, Arrays.asList("id"), output);
        assertEquals(1, output.size());
    }

    @Test
    public void testDeepBean2MapWithFiltering() {
        nestedBean.setSimpleBean(null);

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true);
        assertFalse(map.containsKey("simpleBean"));

        Set<String> ignored = new HashSet<>();
        ignored.add("id");
        nestedBean.setSimpleBean(simpleBean);
        map = Beans.deepBeanToMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());

        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean"));
    }

    @Test
    public void testDeepBean2MapWithMapSupplier() {
        TreeMap<String, Object> treeMap = Beans.deepBeanToMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("123", treeMap.get("id"));
        assertTrue(treeMap.get("simpleBean") instanceof Map);
    }

    @Test
    public void testDeepBean2MapWithSelectPropsAndSupplier() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"), IntFunctions.ofLinkedHashMap());
        assertEquals(2, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
        assertFalse(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsNamingAndSupplier() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "simpleBean"), output);
        assertEquals(2, output.size());
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsNamingAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, Arrays.asList("id"), NamingPolicy.SNAKE_CASE, output);
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true, ignored, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsNamingAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean") || map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullAndOutput() {
        nestedBean.setSimpleBean(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.deepBeanToMap(nestedBean, true, output);
        assertFalse(output.containsKey("simpleBean"));
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, false, ignored, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoredPropsNamingAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.size() > 0);
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

        Map<String, Object> deepMap = Beans.deepBeanToMap(nested);
        assertEquals("test123", deepMap.get("id"));
        assertTrue(deepMap.get("simpleBean") instanceof Map);
        assertTrue(deepMap.get("address") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> simpleMap = (Map<String, Object>) deepMap.get("simpleBean");
        assertEquals("Nested", simpleMap.get("name"));
        assertEquals(40, simpleMap.get("age"));

        Map<String, Object> flatMap = Beans.beanToFlatMap(nested);
        assertEquals("test123", flatMap.get("id"));
        assertEquals("Nested", flatMap.get("simpleBean.name"));
        assertEquals(40, flatMap.get("simpleBean.age"));
        assertEquals("Boston", flatMap.get("address.city"));
        assertEquals("Main St", flatMap.get("address.street"));
    }

    @Test
    public void testDeepBean2MapBasic() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertTrue(map.get("simpleBean") instanceof Map);
        assertTrue(map.get("address") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> innerBean = (Map<String, Object>) map.get("simpleBean");
        assertEquals("John", innerBean.get("name"));
    }

    @Test
    public void testDeepBean2MapWithSupplierOnly() {
        TreeMap<String, Object> map = Beans.deepBeanToMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals("123", map.get("id"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsOnly() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
        assertFalse(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsAndSupplierOnly() {
        TreeMap<String, Object> map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id"), IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithSelectPropsNamingAndSupplierFull() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "simpleBean"), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean") || map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, output);

        assertTrue(output.size() >= 2);
        assertEquals("123", output.get("id"));
        assertTrue(output.get("simpleBean") instanceof Map);
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullOnly() {
        nestedBean.setSimpleBean(null);

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true);
        assertTrue(map.containsKey("id"));
        assertFalse(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullAndIgnoredProps() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
    }

    @Test
    public void testDeepBean2MapWithIgnoreNullIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        TreeMap<String, Object> map = Beans.deepBeanToMap(nestedBean, false, ignored, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("id"));
    }

    @Test
    public void testDeepBean2MapWithAllParametersForMap() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertFalse(map.containsKey("tags"));
        assertTrue(map.size() >= 2);
    }

    @Test
    public void testDeepBean2MapWithAllParametersForMapAlternate() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.NO_CHANGE, IntFunctions.ofLinkedHashMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testDeepBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE);
        assertFalse(map.containsKey("tags"));
        assertTrue(map.size() >= 2);
    }

    @Test
    public void testDeepBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy_WithNull() {
        nestedBean.setSimpleBean(null);
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true, ignored, NamingPolicy.NO_CHANGE);
        assertFalse(map.containsKey("simpleBean"));
        assertFalse(map.containsKey("tags"));
        assertTrue(map.containsKey("id"));
    }

    @Test
    public void testDeepBeanToMap_SelectProps_NamingPolicy_MapSupplier() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
        assertFalse(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBeanToMapWithSelectPropsNamingOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("address"));
    }

    @Test
    public void testDeepBeanToMap_NullBean_ReturnsEmptyMap() {
        Map<String, Object> map = Beans.deepBeanToMap((Object) null, true, null, NamingPolicy.NO_CHANGE);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testDeepBeanToMap_IgnoreNull_IgnoredProps_MapSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, false, ignored, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testDeepBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy_MapSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        TreeMap<String, Object> map = Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testDeepBeanToMap_IgnoreNull_Output() {
        nestedBean.setSimpleBean(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.deepBeanToMap(nestedBean, false, output);
        assertTrue(output.containsKey("simpleBean"));
        assertNull(output.get("simpleBean"));
    }

    @Test
    public void testDeepBeanToMap_IgnoreNull_IgnoredProps_NamingPolicy_Output() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.NO_CHANGE, output);
        assertFalse(output.containsKey("tags"));
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testDeepBeanToMap_NullBean() {
        Map<String, Object> result = Beans.deepBeanToMap((Object) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDeepBeanToMap_WithNamingPolicy_NoChange() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, null, NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBeanToMap_SelectProps_NamingPolicy_Output() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "simpleBean"), NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBeanToMap_NullBean_WithMapSupplier() {
        Map<String, Object> map = Beans.deepBeanToMap((Object) null, IntFunctions.ofLinkedHashMap());
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    // ===== deepBeanToMap(Object, boolean, Set, NamingPolicy, Map) =====

    @Test
    public void testDeepBeanToMap_ignoreNull_namingPolicy_output() {
        NestedBean bean = new NestedBean();
        bean.setId("n1");
        SimpleBean simple = new SimpleBean("Eve", 28);
        bean.setSimpleBean(simple);
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(bean, true, null, NamingPolicy.CAMEL_CASE, output);
        assertEquals("n1", output.get("id"));
        assertNotNull(output.get("simpleBean"));
        assertTrue(output.get("simpleBean") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> innerMap = (Map<String, Object>) output.get("simpleBean");
        assertEquals("Eve", innerMap.get("name"));
    }

    @Test
    public void testDeepBeanToMap_withIgnoredPropNames_output() {
        NestedBean bean = new NestedBean();
        bean.setId("n2");
        SimpleBean simple = new SimpleBean("Frank", 22);
        bean.setSimpleBean(simple);
        Set<String> ignored = new HashSet<>(Collections.singletonList("simpleBean"));
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(bean, false, ignored, NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.containsKey("id"));
        assertFalse(output.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBeanToMap_snakeCaseNaming_output() {
        SimpleBean bean = new SimpleBean("Grace", 33);
        bean.setActive(true);
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(bean, false, null, NamingPolicy.SNAKE_CASE, output);
        assertNotNull(output);
        assertFalse(output.isEmpty());
    }

    @Test
    public void testDeepBeanToMap_nullBean_output() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(null, false, null, NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testDeepBeanToMap_noChangeNaming_output() {
        SimpleBean bean = new SimpleBean("Penny", 38);
        bean.setActive(true);
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(bean, false, null, NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("name"));
        assertEquals("Penny", output.get("name"));
    }

    // ===== deepBeanToMap(bean, selectProps, namingPolicy, output) =====

    @Test
    public void testDeepBeanToMap_selectProps_snakeCase_output() {
        SimpleBean bean = new SimpleBean("Dave", 40);
        Map<String, Object> output = new LinkedHashMap<>();
        List<String> select = java.util.Arrays.asList("name");
        Beans.deepBeanToMap(bean, select, NamingPolicy.SNAKE_CASE, output);
        assertTrue(output.containsKey("name"));
        assertEquals("Dave", output.get("name"));
    }

    @Test
    public void testDeepBeanToMap_selectProps_output_nullBean() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(null, java.util.Arrays.asList("name"), NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testDeepBeanToMap_SelectPropNames_NullMeansAllEmptyMeansNone() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, (Collection<String>) null);
        assertTrue(map.containsKey("id"));
        assertTrue(map.get("simpleBean") instanceof Map);
        assertTrue(map.get("address") instanceof Map);

        map = Beans.deepBeanToMap(nestedBean, Collections.emptyList());
        assertTrue(map.isEmpty());

        map = Beans.deepBeanToMap(nestedBean, Collections.emptyList(), IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertTrue(map.isEmpty());

        map = Beans.deepBeanToMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testDeepBeanToMap_SelectPropNames_OutputEmptySelectDoesNotModifyOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("existing", "value");

        Beans.deepBeanToMap(nestedBean, Collections.emptyList(), output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));

        Beans.deepBeanToMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));
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
    public void testBean2FlatMapWithOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, output);

        assertEquals("123", output.get("id"));
        assertEquals("John", output.get("simpleBean.name"));

        output.clear();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("id"), output);
        assertTrue(output.containsKey("id"));
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
    public void testBean2FlatMapWithIgnoredAndNaming() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());

        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name"));
    }

    @Test
    public void testBean2FlatMapWithMapSupplier() {
        TreeMap<String, Object> treeMap = Beans.beanToFlatMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("123", treeMap.get("id"));
        assertEquals("John", treeMap.get("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndSupplier() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"), IntFunctions.ofLinkedHashMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndSupplier() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address.city") || map.containsKey("address.CITY"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"), output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("id"), NamingPolicy.SNAKE_CASE, output);
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true, ignored, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsNamingAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name") || map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullAndOutput() {
        nestedBean.getAddress().setZipCode(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.beanToFlatMap(nestedBean, true, output);
        assertFalse(output.containsKey("address.zipCode"));
        assertTrue(output.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, false, ignored, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoredPropsNamingAndOutput() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, output);
        assertFalse(output.containsKey("id"));
        assertTrue(output.size() > 0);
    }

    @Test
    public void testBean2FlatMapBasic() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
        assertEquals(25, map.get("simpleBean.age"));
        assertEquals("New York", map.get("address.city"));
    }

    @Test
    public void testBean2FlatMapWithSupplierOnly() {
        TreeMap<String, Object> map = Beans.beanToFlatMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsOnly() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "simpleBean"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
        assertFalse(map.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndSupplierOnly() {
        TreeMap<String, Object> map = Beans.beanToFlatMap(nestedBean, Arrays.asList("address"), IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndSupplierFull() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, Arrays.asList("simpleBean"), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("simple_bean.name") || map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, output);

        assertTrue(output.size() >= 5);
        assertEquals("123", output.get("id"));
        assertEquals("John", output.get("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsAndOutputOnly() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("address"), output);

        assertTrue(output.containsKey("address.city"));
        assertFalse(output.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithSelectPropsNamingAndOutputFull() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.SNAKE_CASE, output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.size() >= 1);
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullOnly() {
        nestedBean.getAddress().setZipCode(null);

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true);
        assertFalse(map.containsKey("address.zipCode"));
        assertTrue(map.containsKey("address.city"));
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullAndIgnoredProps() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBean2FlatMapWithIgnoreNullIgnoredPropsAndSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        TreeMap<String, Object> map = Beans.beanToFlatMap(nestedBean, false, ignored, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testBean2FlatMapWithAllParametersForMap() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.size() >= 4);
    }

    @Test
    public void testBean2FlatMapWithAllParametersForMapAlternate() {
        Set<String> ignored = new HashSet<>();
        ignored.add("address");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true, ignored, NamingPolicy.NO_CHANGE, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertFalse(map.containsKey("address.city"));
    }

    @Test
    public void testBeanToFlatMap_IgnoreNull_IgnoredProps_NamingPolicy() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE);
        assertFalse(map.containsKey("tags"));
        assertTrue(map.size() >= 4);
    }

    @Test
    public void testBeanToFlatMap_IgnoreNull_IgnoredProps_NamingPolicy_WithNull() {
        nestedBean.getAddress().setZipCode(null);
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true, ignored, NamingPolicy.NO_CHANGE);
        assertFalse(map.containsKey("tags"));
        assertFalse(map.containsKey("address.zipCode"));
        assertTrue(map.containsKey("address.city"));
    }

    @Test
    public void testBeanToFlatMap_SelectProps_NamingPolicy_MapSupplier() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address.city"));
    }

    @Test
    public void testBeanToFlatMapWithSelectPropsNamingOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("address.city"));
    }

    @Test
    public void testBeanToFlatMap_NullBean_ReturnsEmptyMap() {
        Map<String, Object> map = Beans.beanToFlatMap((Object) null, true, null, NamingPolicy.NO_CHANGE);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testBeanToFlatMap_IgnoreNull_IgnoredProps_MapSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, false, ignored, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testBeanToFlatMap_IgnoreNull_IgnoredProps_NamingPolicy_MapSupplier() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        TreeMap<String, Object> map = Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testBeanToFlatMap_IgnoreNull_Output() {
        nestedBean.getAddress().setZipCode(null);
        Map<String, Object> output = new LinkedHashMap<>();

        Beans.beanToFlatMap(nestedBean, false, output);
        assertTrue(output.containsKey("address.zipCode"));
    }

    @Test
    public void testBeanToFlatMap_IgnoreNull_IgnoredProps_NamingPolicy_Output() {
        Set<String> ignored = new HashSet<>();
        ignored.add("tags");

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.NO_CHANGE, output);
        assertFalse(output.containsKey("tags"));
        assertTrue(output.containsKey("id"));
    }

    @Test
    public void testBeanToFlatMap_NullBean() {
        Map<String, Object> result = Beans.beanToFlatMap((Object) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBeanToFlatMap_WithNamingPolicy_NoChange() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, null, NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBeanToFlatMap_NullBean_WithMapSupplier() {
        Map<String, Object> map = Beans.beanToFlatMap((Object) null, IntFunctions.ofLinkedHashMap());
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    // ===== beanToFlatMap(bean, selectProps, namingPolicy, output) =====

    @Test
    public void testBeanToFlatMap_selectProps_output_basic() {
        SimpleBean bean = new SimpleBean("Eve", 28);
        Map<String, Object> output = new LinkedHashMap<>();
        List<String> select = java.util.Arrays.asList("name", "age");
        Beans.beanToFlatMap(bean, select, NamingPolicy.CAMEL_CASE, output);
        assertEquals("Eve", output.get("name"));
        assertEquals(28, output.get("age"));
    }

    @Test
    public void testBeanToFlatMap_SelectPropNames_NullMeansAllEmptyMeansNone() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, (Collection<String>) null);
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));
        assertTrue(map.containsKey("address.city"));

        map = Beans.beanToFlatMap(nestedBean, Collections.emptyList());
        assertTrue(map.isEmpty());

        map = Beans.beanToFlatMap(nestedBean, Collections.emptyList(), IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertTrue(map.isEmpty());

        map = Beans.beanToFlatMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testBeanToFlatMap_SelectPropNames_OutputEmptySelectDoesNotModifyOutput() {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("existing", "value");

        Beans.beanToFlatMap(nestedBean, Collections.emptyList(), output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));

        Beans.beanToFlatMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));
    }

    @Test
    public void testBeanToFlatMap_selectProps_output_nullBean() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(null, java.util.Arrays.asList("name"), NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    // ===== beanToFlatMap(bean, ignoreNull, ignoreSet, namingPolicy, output) =====

    @Test
    public void testBeanToFlatMap_withIgnoreNullAndIgnoredSet() {
        SimpleBean bean = new SimpleBean("Frank", 50);
        // active is null
        Map<String, Object> output = new LinkedHashMap<>();
        Set<String> ignored = new HashSet<>(java.util.Arrays.asList("age"));
        Beans.beanToFlatMap(bean, true, ignored, NamingPolicy.CAMEL_CASE, output);
        assertEquals("Frank", output.get("name"));
        assertFalse(output.containsKey("active"));
        assertFalse(output.containsKey("age"));
    }

    // ===== beanToFlatMap with nested bean - tests recursive call path =====

    @Test
    public void testBeanToFlatMap_withNestedBean_recursiveCall() {
        Map<String, Object> flatMap = Beans.beanToFlatMap(nestedBean);
        assertNotNull(flatMap);
        // nested properties should be flattened with dot notation
        assertTrue(flatMap.containsKey("id"));
        assertTrue(flatMap.containsKey("simpleBean.name"));
        assertEquals("John", flatMap.get("simpleBean.name"));
        assertEquals("New York", flatMap.get("address.city"));
    }

    @Test
    public void testNewBean() {
        SimpleBean bean = Beans.newBean(SimpleBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
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
    public void testNewBean_EntityBean() {
        EntityBean bean = Beans.newBean(EntityBean.class);
        assertNotNull(bean);
        assertNull(bean.getId());
        assertNull(bean.getValue());
    }

    @Test
    public void testNewBean_WithBuilderClass() {
        // Test that newBean works even with a builder-pattern class
        assertNotNull(Beans.newBean(SimpleBean.class));
    }

    @Test
    public void testCloneBasic() {
        SimpleBean original = new SimpleBean("Original", 100);
        original.setActive(true);

        SimpleBean cloned = Beans.deepCopy(original);
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
        SimpleBean cloned = Beans.deepCopy(null);
        assertNull(cloned);
    }

    @Test
    public void testDeepCopy_SimpleBean() {
        SimpleBean copy = Beans.deepCopy(simpleBean);
        assertNotNull(copy);
        assertNotSame(simpleBean, copy);
        assertEquals("John", copy.getName());
        assertEquals(25, copy.getAge());
        assertEquals(true, copy.getActive());
    }

    @Test
    public void testDeepCopy_NullInput() {
        assertNull(Beans.deepCopy(null));
    }

    @Test
    public void testDeepCopy_NestedBean_Independence() {
        NestedBean copy = Beans.deepCopy(nestedBean);
        assertNotNull(copy);
        assertNotSame(nestedBean, copy);
        assertEquals("123", copy.getId());
        assertEquals("John", copy.getSimpleBean().getName());
        assertEquals("New York", copy.getAddress().getCity());

        // Modify the copy and verify original is unchanged
        copy.getSimpleBean().setName("Modified");
        copy.getAddress().setCity("Boston");
        assertEquals("John", nestedBean.getSimpleBean().getName());
        assertEquals("New York", nestedBean.getAddress().getCity());
    }

    @Test
    public void testClone() {
        SimpleBean cloned = Beans.deepCopy(simpleBean);
        assertNotNull(cloned);
        assertNotSame(simpleBean, cloned);
        assertEquals(simpleBean.getName(), cloned.getName());
        assertEquals(simpleBean.getAge(), cloned.getAge());

        assertNull(Beans.deepCopy(null));

        SimpleBean cloned2 = Beans.deepCopyAs(simpleBean, SimpleBean.class);
        assertNotNull(cloned2);
        assertEquals(simpleBean.getName(), cloned2.getName());

        SimpleBean fromNull = Beans.deepCopyAs(null, SimpleBean.class);
        assertNotNull(fromNull);

        assertThrows(IllegalArgumentException.class, () -> Beans.deepCopyAs(simpleBean, null));
    }

    @Test
    public void testCloneWithTargetTypeNotNull() {
        SimpleBean original = new SimpleBean("Test", 50);

        SimpleBean cloned = Beans.deepCopyAs(original, SimpleBean.class);
        assertNotNull(cloned);
        assertEquals("Test", cloned.getName());
        assertEquals(50, cloned.getAge());
    }

    @Test
    public void testCloneWithNullSourceButTargetType() {
        SimpleBean cloned = Beans.deepCopyAs(null, SimpleBean.class);
        assertNotNull(cloned);
        assertNull(cloned.getName());
        assertEquals(0, cloned.getAge());
    }

    @Test
    public void testDeepCopyAs_NullSource() {
        SimpleBean result = Beans.deepCopyAs(null, SimpleBean.class);
        assertNotNull(result);
        assertNull(result.getName());
    }

    @Test
    public void testDeepCopyAs_NestedBean() {
        NestedBean copy = Beans.deepCopyAs(nestedBean, NestedBean.class);
        assertNotNull(copy);
        assertNotSame(nestedBean, copy);
        assertEquals("123", copy.getId());
    }

    // ===== deepCopyAs =====

    @Test
    public void testDeepCopyAs_crossType() {
        SimpleBean source = new SimpleBean("Leo", 70);
        source.setActive(true);
        SimpleBean copy = Beans.deepCopyAs(source, SimpleBean.class);
        assertNotNull(copy);
        assertEquals("Leo", copy.getName());
        assertEquals(70, copy.getAge());
        assertNotSame(source, copy);
    }

    @Test
    public void testDeepCopyAs_nullSource_isBeanClass() {
        SimpleBean copy = Beans.deepCopyAs(null, SimpleBean.class);
        assertNotNull(copy);
    }

    @Test
    public void testCloneWithTargetType() {
        SimpleBean cloned = Beans.deepCopyAs(simpleBean, SimpleBean.class);
        assertNotNull(cloned);
        assertNotSame(simpleBean, cloned);
        assertEquals("John", cloned.getName());
        assertEquals(25, cloned.getAge());

        SimpleBean fromNull = Beans.deepCopyAs(null, SimpleBean.class);
        assertNotNull(fromNull);
        assertNull(fromNull.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.deepCopyAs(simpleBean, null));
    }

    @Test
    public void testDeepCopyAs_NullTargetType() {
        assertThrows(IllegalArgumentException.class, () -> Beans.deepCopyAs(simpleBean, null));
    }

    @Test
    public void testDeepCopyAs_nullTargetType_throws() {
        SimpleBean source = new SimpleBean("Mia", 20);
        assertThrows(IllegalArgumentException.class, () -> Beans.deepCopyAs(source, null));
    }

    @Test
    public void testCopyWithPropFilter() {
        SimpleBean copied = Beans.copy(simpleBean, (name, value) -> value instanceof String);
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copyAs(simpleBean, (name, value) -> true, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
    }

    @Test
    public void testCopyWithPropFilterAndConverter() {
        SimpleBean copied = Beans.copyAs(simpleBean, (name, value) -> true, name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
    }

    @Test
    public void testCopyWithSelectPropsAndConverter() {
        SimpleBean copied = Beans.copyAs(simpleBean, Arrays.asList("name", "age"), name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
    }

    @Test
    public void testCopyWithDifferentTypes() {
        SimpleBean source = new SimpleBean("Transfer", 50);
        source.setActive(true);

        SimpleBean target = Beans.copyAs(source, SimpleBean.class);
        assertEquals("Transfer", target.getName());
        assertEquals(50, target.getAge());

        SimpleBean filtered = Beans.copyAs(source, (name, value) -> value instanceof String, SimpleBean.class);
        assertEquals("Transfer", filtered.getName());
        assertEquals(0, filtered.getAge());
    }

    @Test
    public void testCopyWithSelectPropsConverterAndTargetType() {
        SimpleBean original = new SimpleBean("Convert", 333);

        SimpleBean copied = Beans.copyAs(original, Arrays.asList("name", "age"), name -> name, SimpleBean.class);
        assertEquals("Convert", copied.getName());
        assertEquals(333, copied.getAge());
    }

    @Test
    public void testCopyWithPropFilterConverterAndTargetType() {
        SimpleBean original = new SimpleBean("FilterConvert", 555);

        SimpleBean copied = Beans.copyAs(original, (name, value) -> true, name -> name, SimpleBean.class);
        assertEquals("FilterConvert", copied.getName());
        assertEquals(555, copied.getAge());
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
        EntityBean copiedEntity = Beans.copyAs(entity, EntityBean.class);
        assertEquals(100L, copiedEntity.getId());

        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        copied = Beans.copyAs(snakeBean, (Collection<String>) null, name -> Beans.toCamelCase(name), SimpleBean.class);
        assertNotNull(copied);
    }

    @Test
    public void testCopyWithSelectPropNames() {
        SimpleBean copied = Beans.copy(simpleBean, Arrays.asList("name"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copy(simpleBean, (Collection<String>) null);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
        assertEquals(true, copied.getActive());

        copied = Beans.copy(simpleBean, Collections.emptyList());
        assertNull(copied.getName());
        assertEquals(0, copied.getAge());
        assertNull(copied.getActive());

        copied = Beans.copyAs(simpleBean, Collections.emptyList(), SimpleBean.class);
        assertNull(copied.getName());
        assertEquals(0, copied.getAge());
        assertNull(copied.getActive());
    }

    @Test
    public void testCopyWithPropNameConverter() {
        SimpleBean copied = Beans.copyAs(simpleBean, (Collection<String>) null, name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
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

        SimpleBean copied = Beans.copyAs(original, SimpleBean.class);
        assertNotNull(copied);
        assertEquals("TypeTest", copied.getName());
        assertEquals(111, copied.getAge());
    }

    @Test
    public void testCopyWithSelectPropsAndTargetType() {
        SimpleBean original = new SimpleBean("SelectType", 222);

        SimpleBean copied = Beans.copyAs(original, Arrays.asList("age"), SimpleBean.class);
        assertNull(copied.getName());
        assertEquals(222, copied.getAge());
    }

    @Test
    public void testCopyWithPropFilterAndTargetType() {
        SimpleBean original = new SimpleBean("FilterType", 444);
        original.setActive(true);

        SimpleBean copied = Beans.copyAs(original, (name, value) -> value instanceof Integer, SimpleBean.class);
        assertNull(copied.getName());
        assertEquals(444, copied.getAge());
        assertNull(copied.getActive());
    }

    @Test
    public void testCopyWithIgnoreUnmatchedAndIgnoredProps() {
        SimpleBean original = new SimpleBean("IgnoreTest", 666);
        original.setActive(true);

        Set<String> ignored = new HashSet<>();
        ignored.add("active");

        SimpleBean copied = Beans.copyAs(original, true, ignored, SimpleBean.class);
        assertEquals("IgnoreTest", copied.getName());
        assertEquals(666, copied.getAge());
        assertNull(copied.getActive());
    }

    @Test
    public void testCopy_NullInput() {
        assertNull(Beans.copy(null));
    }

    @Test
    public void testCopy_WithEmptySelectProps() {
        SimpleBean copy = Beans.copy(simpleBean, Collections.emptyList());
        assertNotNull(copy);
        assertEquals(0, copy.getAge());
        assertNull(copy.getName());
    }

    @Test
    public void testCopyWithIgnoreUnmatched() {
        SimpleBean source = new SimpleBean("John", 25);

        EntityBean target = Beans.copyAs(source, true, null, EntityBean.class);
        assertNotNull(target);

        SimpleBean target2 = Beans.copyAs(source, false, null, SimpleBean.class);
        assertEquals("John", target2.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(source, false, null, null));
    }

    @Test
    public void testCopyAs_WithSelectPropsAndTargetType() {
        SimpleBean copy = Beans.copyAs(simpleBean, Arrays.asList("name"), SimpleBean.class);
        assertEquals("John", copy.getName());
        assertEquals(0, copy.getAge());
    }

    // ===== copyAs(Object, Collection<String>, Function, Class) =====

    @Test
    public void testCopyAs_selectPropNames_withConverter_basic() {
        SimpleBean source = new SimpleBean("Helen", 45);
        source.setActive(true);
        SimpleBean copy = Beans.copyAs(source, Arrays.asList("name", "age"), name -> name, SimpleBean.class);
        assertEquals("Helen", copy.getName());
        assertEquals(45, copy.getAge());
    }

    @Test
    public void testCopyAs_SelectPropNames_PropNameConverter_TargetType() {
        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        snakeBean.setLastName("Doe");

        SimpleBean copied = Beans.copyAs(snakeBean, Arrays.asList("firstName"), name -> {
            if ("firstName".equals(name)) {
                return "name";
            }
            return name;
        }, SimpleBean.class);
        assertEquals("John", copied.getName());
    }

    @Test
    public void testCopyAs_NullSource() {
        SimpleBean result = Beans.copyAs(null, SimpleBean.class);
        assertNotNull(result);
    }

    // ===== copyAs(Object, boolean, Set, Class) =====

    @Test
    public void testCopyAs_ignoreUnmatched_withIgnoredPropNames() {
        SimpleBean source = new SimpleBean("Jack", 55);
        source.setActive(true);
        Set<String> ignored = new HashSet<>(Collections.singletonList("age"));
        SimpleBean copy = Beans.copyAs(source, true, ignored, SimpleBean.class);
        assertEquals("Jack", copy.getName());
        assertEquals(0, copy.getAge()); // age was ignored
        assertEquals(true, copy.getActive());
    }

    @Test
    public void testCopyAs_ignoreUnmatched_noIgnored() {
        SimpleBean source = new SimpleBean("Karen", 60);
        source.setActive(false);
        SimpleBean copy = Beans.copyAs(source, true, null, SimpleBean.class);
        assertEquals("Karen", copy.getName());
        assertEquals(60, copy.getAge());
        assertEquals(false, copy.getActive());
    }

    @Test
    public void testCopyAs_ignoreUnmatched_nullSource() {
        SimpleBean copy = Beans.copyAs(null, true, null, SimpleBean.class);
        assertNotNull(copy);
        assertNull(copy.getName());
        assertEquals(0, copy.getAge());
    }

    // ===== copyAs(sourceBean, propFilter, propNameConverter, targetType) =====

    @Test
    public void testCopyAs_withBiPredicate_allProps() {
        SimpleBean src = new SimpleBean("Grace", 35);
        src.setActive(true);
        SimpleBean copy = Beans.copyAs(src, (name, val) -> true, java.util.function.Function.identity(), SimpleBean.class);
        assertNotNull(copy);
        assertEquals("Grace", copy.getName());
        assertEquals(35, copy.getAge());
    }

    @Test
    public void testCopyAs_withBiPredicate_filterSomeProps() {
        SimpleBean src = new SimpleBean("Henry", 45);
        SimpleBean copy = Beans.copyAs(src, (propName, val) -> !propName.equals("age"), java.util.function.Function.identity(), SimpleBean.class);
        assertNotNull(copy);
        assertEquals("Henry", copy.getName());
        assertEquals(0, copy.getAge()); // age filtered out
    }

    @Test
    public void testCopyAs_withBiPredicate_nullSource() {
        SimpleBean copy = Beans.copyAs(null, (name, val) -> true, java.util.function.Function.identity(), SimpleBean.class);
        assertNotNull(copy);
        assertNull(copy.getName());
    }

    // ===== copyAs(Object, BiPredicate, Class) - 3-arg variant =====

    @Test
    public void testCopyAs_biPredicate_threeArg_allProps() {
        SimpleBean src = new SimpleBean("Iris", 29);
        src.setActive(true);
        SimpleBean copy = Beans.copyAs(src, (java.util.function.BiPredicate<String, Object>) (name, val) -> true, SimpleBean.class);
        assertNotNull(copy);
        assertEquals("Iris", copy.getName());
        assertEquals(29, copy.getAge());
    }

    @Test
    public void testCopyAs_biPredicate_threeArg_filterByValue() {
        SimpleBean src = new SimpleBean("Jack", 0);
        // filter out props with zero int value
        SimpleBean copy = Beans.copyAs(src, (java.util.function.BiPredicate<String, Object>) (name, val) -> !name.equals("age"), SimpleBean.class);
        assertNotNull(copy);
        assertEquals("Jack", copy.getName());
        assertEquals(0, copy.getAge());
    }

    @Test
    public void testCopyAs_biPredicate_threeArg_nullSource() {
        SimpleBean copy = Beans.copyAs(null, (java.util.function.BiPredicate<String, Object>) (name, val) -> true, SimpleBean.class);
        assertNotNull(copy);
        assertNull(copy.getName());
    }

    @Test
    public void testCopyAs_NullTargetType() {
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(simpleBean, (Class<SimpleBean>) null));
    }

    @Test
    public void testCopyAs_SelectProps_NullConverter() {
        assertThrows(NullPointerException.class, () -> Beans.copyAs(simpleBean, Arrays.asList("name", "age"), null, SimpleBean.class));
    }

    @Test
    public void testCopyAs_selectPropNames_nullConverter() {
        SimpleBean source = new SimpleBean("Ivan", 50);
        // When converter is null, use identity
        // Test that NullPointerException is thrown for null converter
        assertThrows(NullPointerException.class, () -> Beans.copyAs(source, Arrays.asList("name"), null, SimpleBean.class));
    }

    // ===== copyAs/copy exception behavior documented in @throws clauses =====

    @Test
    public void testCopyAs_selectPropNames_unmatchedTargetProp_throws() {
        SimpleBean source = new SimpleBean("Liam", 31);
        // "name" exists in SimpleBean (source) but not in Address (target) -> throws
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(source, Arrays.asList("name"), Address.class));
    }

    @Test
    public void testCopyAs_selectPropNames_invalidSourceProp_throws() {
        SimpleBean source = new SimpleBean("Mia", 22);
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(source, Arrays.asList("nonExistentProp"), SimpleBean.class));
    }

    @Test
    public void testCopyAs_biPredicate_unmatchedTargetProp_throws() {
        SimpleBean source = new SimpleBean("Noah", 41);
        // "name" passes the filter but does not exist in Address -> throws
        assertThrows(IllegalArgumentException.class,
                () -> Beans.copyAs(source, (java.util.function.BiPredicate<String, Object>) (propName, propValue) -> propName.equals("name"), Address.class));
    }

    @Test
    public void testCopy_selectPropNames_invalidPropName_throws() {
        SimpleBean source = new SimpleBean("Olivia", 19);
        assertThrows(IllegalArgumentException.class, () -> Beans.copy(source, Arrays.asList("nonExistentProp")));
    }

    @Test
    public void testCopyAs_ignoreUnmatchedFalse_skipsNullSourceValues() {
        // SimpleBean has "name" (String), "age" (int), "active" (Boolean); Address has none of them.
        // Null-valued source properties are skipped, so with "age" (primitive -> always non-null boxed)
        // excluded via ignoredPropNames, nothing is copied and the unmatched check never fires.
        SimpleBean source = new SimpleBean(); // name=null, age=0, active=null
        Set<String> ignored = new HashSet<>(Collections.singletonList("age"));
        Address copy = Beans.copyAs(source, false, ignored, Address.class);
        assertNotNull(copy);
        assertNull(copy.getCity());

        // A non-null source value ("name") now triggers the unmatched check and throws.
        source.setName("Pia");
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(source, false, ignored, Address.class));
    }

    @Test
    public void testMergeWithConverter() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);

        Beans.mergeInto(source, target, name -> name, (srcVal, tgtVal) -> srcVal);
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithPropFilterConverterAndMergeFuncSimple() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);

        Beans.mergeInto(source, target, (name, value) -> true, name -> name, (srcVal, tgtVal) -> srcVal);
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithSelectPropNamesAndConverter() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);

        Beans.mergeInto(source, target, Arrays.asList("name"), name -> name, (a, b) -> a);
        assertEquals("Jane", target.getName());
        assertEquals(25, target.getAge());
    }

    @Test
    public void testMergeWithMergeFuncOnly() {
        SimpleBean source = new SimpleBean("John", 10);
        SimpleBean target = new SimpleBean("Jane", 20);

        Beans.mergeInto(source, target, Fn.o((srcVal, tgtVal) -> {
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

        Beans.mergeInto(source, target, Arrays.asList("age"), (srcVal, tgtVal) -> {
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

        Beans.mergeInto(source, target, Arrays.asList("name"), name -> name, (a, b) -> a);
        assertEquals("John", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithPropFilterAndConverter() {
        SimpleBean source = new SimpleBean("John", 25);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Jane", 30);
        target.setActive(false);

        Beans.mergeInto(source, target, (name, value) -> value instanceof String, name -> name, (a, b) -> a);

        assertEquals("John", target.getName());
        assertEquals(30, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeWithPropFilterConverterAndMergeFunc() {
        SimpleBean source = new SimpleBean("Alice", 10);
        SimpleBean target = new SimpleBean("Bob", 20);

        Beans.mergeInto(source, target, (name, value) -> value instanceof Integer, name -> name, (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));

        assertEquals("Bob", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeBasic() {
        SimpleBean source = new SimpleBean("Source", 10);
        SimpleBean target = new SimpleBean("Target", 20);

        Beans.mergeInto(source, target);
        assertEquals("Source", target.getName());
        assertEquals(10, target.getAge());
    }

    @Test
    public void testMergeWithMergeFunc() {
        SimpleBean source = new SimpleBean("A", 5);
        SimpleBean target = new SimpleBean("B", 10);

        Beans.mergeInto(source, target, Fn.o((srcVal, tgtVal) -> {
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

        Beans.mergeInto(source, target, name -> name, (srcVal, tgtVal) -> srcVal);
        assertEquals("Test", target.getName());
        assertEquals(7, target.getAge());
    }

    @Test
    public void testMergeWithSelectPropsOnly() {
        SimpleBean source = new SimpleBean("New", 100);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Old", 200);
        target.setActive(false);

        Beans.mergeInto(source, target, Arrays.asList("name"));
        assertEquals("New", target.getName());
        assertEquals(200, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testCopyInto_SelectPropNames_NullMeansAllEmptyMeansNone() {
        SimpleBean source = new SimpleBean("New", 100);
        source.setActive(true);

        SimpleBean target = new SimpleBean("Old", 200);
        target.setActive(false);
        Beans.mergeInto(source, target, (Collection<String>) null);
        assertEquals("New", target.getName());
        assertEquals(100, target.getAge());
        assertEquals(true, target.getActive());

        target = new SimpleBean("Old", 200);
        target.setActive(false);
        Beans.mergeInto(source, target, Collections.emptyList());
        assertEquals("Old", target.getName());
        assertEquals(200, target.getAge());
        assertEquals(false, target.getActive());

        Beans.mergeInto(source, target, Collections.emptyList(), (srcVal, tgtVal) -> srcVal);
        assertEquals("Old", target.getName());
        assertEquals(200, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeWithSelectPropsAndMergeFuncOnly() {
        SimpleBean source = new SimpleBean("X", 8);
        SimpleBean target = new SimpleBean("Y", 2);

        Beans.mergeInto(source, target, Arrays.asList("age"), (srcVal, tgtVal) -> {
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

        Beans.mergeInto(source, target, Arrays.asList("age"), name -> name, (srcVal, tgtVal) -> {
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

        Beans.mergeInto(source, target, Fn.p((name, value) -> value instanceof Integer));
        assertEquals("BaseTarget", target.getName());
        assertEquals(20, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeWithPropFilterAndConverterOnly() {
        SimpleBean source = new SimpleBean("PropFilter", 25);
        SimpleBean target = new SimpleBean("TargetBean", 75);

        Beans.mergeInto(source, target, (name, value) -> true, name -> name);
        assertEquals("PropFilter", target.getName());
        assertEquals(25, target.getAge());
    }

    @Test
    public void testMergeWithPropFilterConverterAndMergeFuncFull() {
        SimpleBean source = new SimpleBean("Complex", 12);
        SimpleBean target = new SimpleBean("Base", 8);

        Beans.mergeInto(source, target, (name, value) -> value instanceof Integer, name -> name, (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));

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

        Beans.mergeInto(source, target, true, ignored);
        assertEquals("Ignore", target.getName());
        assertEquals(60, target.getAge());
    }

    @Test
    public void testCopyInto_PropFilter_Converter_MergeFunc() {
        SimpleBean source = new SimpleBean("Src", 10);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Tgt", 20);
        target.setActive(false);

        Beans.mergeInto(source, target, (name, value) -> name.equals("age"), name -> name, (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));
        assertEquals("Tgt", target.getName());
        assertEquals(30, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testCopyInto_IgnoreUnmatched_IgnoredProps_MergeFunc() {
        SimpleBean source = new SimpleBean("Src", 10);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Tgt", 20);
        target.setActive(false);

        Set<String> ignored = new HashSet<>();
        ignored.add("active");

        Beans.mergeInto(source, target, true, ignored, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        });
        assertEquals("Src", target.getName());
        assertEquals(30, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testCopyInto_IgnoreUnmatched_IgnoredProps() {
        SimpleBean source = new SimpleBean("Src", 10);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Tgt", 20);
        target.setActive(false);

        Set<String> ignored = new HashSet<>();
        ignored.add("active");

        Beans.mergeInto(source, target, true, ignored);
        assertEquals("Src", target.getName());
        assertEquals(10, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testCopyInto_WithPropFilter_Converter_NoMergeFunc() {
        SimpleBean source = new SimpleBean("FromSrc", 50);
        source.setActive(true);
        SimpleBean target = new SimpleBean("OrigTarget", 100);
        target.setActive(false);

        Beans.mergeInto(source, target, (name, value) -> name.equals("name"), name -> name);
        assertEquals("FromSrc", target.getName());
        assertEquals(100, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testCopyInto_SelectProps_MergeFunc_Only() {
        SimpleBean source = new SimpleBean("SrcOnly", 10);
        SimpleBean target = new SimpleBean("TgtOnly", 20);

        Beans.mergeInto(source, target, Arrays.asList("name", "age"), (srcVal, tgtVal) -> srcVal);
        assertEquals("SrcOnly", target.getName());
        assertEquals(10, target.getAge());
    }

    @Test
    public void testMergeWithPropFilter() {
        SimpleBean source = new SimpleBean("Jane", 30);
        source.setActive(true);
        SimpleBean target = new SimpleBean("John", 25);
        target.setActive(false);

        Beans.mergeInto(source, target, Fn.p((name, value) -> value instanceof String));
        assertEquals("Jane", target.getName());
        assertEquals(25, target.getAge());
        assertEquals(false, target.getActive());

        BeanWithSnakeCase snakeSource = new BeanWithSnakeCase();
        snakeSource.setFirstName("NewFirst");
        SimpleBean convertTarget = new SimpleBean();
        Beans.mergeInto(snakeSource, convertTarget, (name, value) -> true, name -> "name");
        assertEquals("NewFirst", convertTarget.getName());
    }

    @Test
    public void testMergeWithIgnoreUnmatched() {
        SimpleBean source = new SimpleBean("Jane", 30);
        EntityBean target = new EntityBean();
        target.setId(1L);

        Beans.mergeInto(source, target, true, null);
        assertEquals(1L, target.getId());

        source.setAge(5);
        SimpleBean target2 = new SimpleBean("John", 10);
        Beans.mergeInto(source, target2, true, null, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        });
        assertEquals(15, target2.getAge());
    }

    @Test
    public void testMergeWithDifferentBeanTypes() {
        SimpleBean source = new SimpleBean("John", 25);
        EntityBean target = new EntityBean();
        target.setId(1L);
        target.setValue("original");

        Beans.mergeInto(source, target, true, null);
        assertEquals(1L, target.getId());
        assertEquals("original", target.getValue());
    }

    @Test
    public void testMergeWithNullSourceDoesNotModify() {
        SimpleBean target = new SimpleBean("Target", 30);

        Beans.mergeInto(null, target);
        assertEquals("Target", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMergeWithIgnoreUnmatchedIgnoredPropsAndMergeFunc() {
        SimpleBean source = new SimpleBean("Final", 4);
        SimpleBean target = new SimpleBean("Init", 6);

        Beans.mergeInto(source, target, true, null, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) * ((Integer) tgtVal);
            }
            return srcVal;
        });

        assertEquals("Final", target.getName());
        assertEquals(24, target.getAge());
    }

    @Test
    public void testCopyInto_SelectProps_Converter_MergeFunc() {
        SimpleBean source = new SimpleBean("SrcName", 15);
        SimpleBean target = new SimpleBean("TgtName", 25);

        Beans.mergeInto(source, target, Arrays.asList("age"), name -> name, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return Math.max((Integer) srcVal, (Integer) tgtVal);
            }
            return srcVal;
        });
        assertEquals("TgtName", target.getName());
        assertEquals(25, target.getAge());
    }

    @Test
    public void testCopyInto_NullSource() {
        SimpleBean target = new SimpleBean("Jane", 30);
        Beans.mergeInto(null, target);
        // Target should remain unchanged
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    public void testMerge() {
        {
            SimpleBean source = new SimpleBean("Jane", 30);
            SimpleBean target = new SimpleBean("John", 25);

            Beans.mergeInto(source, target);
            assertEquals("Jane", target.getName());
            assertEquals(30, target.getAge());

            SimpleBean original = new SimpleBean("Bob", 40);
            Beans.mergeInto(null, original);
            assertEquals("Bob", original.getName());
        }

        {
            SimpleBean source = new SimpleBean("Alice", 35);
            SimpleBean target = new SimpleBean("Charlie", 28);
            Beans.mergeInto(source, target, Arrays.asList("age"));
            assertEquals("Charlie", target.getName());
            assertEquals(35, target.getAge());

            source.setAge(10);
            target.setAge(20);
            Beans.mergeInto(source, target, Arrays.asList("age"), (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));
            assertEquals(30, target.getAge());

            assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(source, null));
        }
    }

    @Test
    public void testCopyInto_NullTarget() {
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, null));
    }

    // ===== mergeInto(source, target, ignoreUnmatched=false, ignoredProps) throws on unmatched target prop (L5682) =====

    // SimpleBean has "name", "age", "active"; Address has "street", "city", "zipCode".
    // Copying a SimpleBean into an Address with ignoreUnmatchedProperty=false should throw
    // because SimpleBean's "name" property does not exist in Address.
    @Test
    public void testCopyInto_IgnoreUnmatched_False_Throws() {
        Address target = new Address();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, target, false, (Set<String>) null));
    }

    // ===== mergeInto(source, target, selectPropNames) throws on invalid source prop name (L5764) =====

    @Test
    public void testCopyInto_SelectPropNames_InvalidSourceProp_Throws() {
        SimpleBean target = new SimpleBean();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, target, Arrays.asList("nonExistentProp")));
    }

    // ===== mergeInto(source, target, biPredicate, propNameConverter, mergeFunc) throws on unmatched target prop (L5826) =====

    // Use a BiPredicate that accepts all props and a name converter that maps every prop name
    // to a name that does not exist in the target class, so targetPropInfo is null and throws.
    @Test
    public void testCopyInto_BiPredicate_UnmatchedTargetProp_Throws() {
        // source: SimpleBean (has "name", "age", "active")
        // target: SimpleBean (same class – use an identity converter but then map to a bad name)
        // Map every source prop name to "__bad__" so it can't be found in target.
        SimpleBean target = new SimpleBean();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, target, (propName, propValue) -> true, // include every property
                propName -> "__bad__", // convert to a name that doesn't exist
                (srcVal, tgtVal) -> srcVal));
    }

    // ===== mergeInto merge-all overloads silently skip unmatched target props (no throw) =====

    // Regression for the former strictness flip: adding only a mergeFunc must NOT turn a cross-type
    // merge into a thrown exception. SimpleBean (name, age, active) and Address (street, city, zipCode)
    // share no properties, so every source property is unmatched and silently skipped.
    @Test
    public void testMergeInto_MergeFunc_UnmatchedTarget_SkipsSilently() {
        Address target = new Address();
        target.setCity("NYC");

        Address result = Beans.mergeInto(simpleBean, target, Fn.o((srcVal, tgtVal) -> srcVal));

        assertTrue(target == result);
        assertNull(target.getStreet());
        assertEquals("NYC", target.getCity()); // unchanged: no source property matched
        assertNull(target.getZipCode());
    }

    // The (propNameConverter, mergeFunc) merge-all overload behaves the same way (no throw on unmatched).
    @Test
    public void testMergeInto_Converter_MergeFunc_UnmatchedTarget_SkipsSilently() {
        Address target = new Address();
        target.setCity("NYC");

        assertDoesNotThrow(() -> Beans.mergeInto(simpleBean, target, name -> name, (srcVal, tgtVal) -> srcVal));
        assertEquals("NYC", target.getCity());
    }

    // Null source must return the target unchanged (documented), not NPE - consistent with the other mergeInto overloads.
    @Test
    public void testMergeInto_Converter_MergeFunc_NullSource_ReturnsTargetUnchanged() {
        SimpleBean target = new SimpleBean("John", 25);

        SimpleBean result = Beans.mergeInto(null, target, name -> name, (srcVal, tgtVal) -> srcVal);

        assertTrue(target == result);
        assertEquals("John", target.getName());
        assertEquals(25, target.getAge());

        // null target still throws (documented)
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, (SimpleBean) null, name -> name, (srcVal, tgtVal) -> srcVal));
    }

    @Test
    public void testErase() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.clearProps(bean, "name", "age");
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(true, bean.getActive());

        bean = new SimpleBean("Jane", 30);
        Beans.clearProps(bean, Arrays.asList("name"));
        assertNull(bean.getName());
        assertEquals(30, bean.getAge());

        Beans.clearProps(null, "name");

        bean = new SimpleBean("Bob", 35);
        Beans.clearProps(bean, new String[0]);
        assertEquals("Bob", bean.getName());
    }

    @Test
    public void testEraseWithCollection() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.clearProps(bean, Arrays.asList("name", "age"));
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(true, bean.getActive());
    }

    @Test
    public void testEraseSpecificProperties() {
        SimpleBean bean = new SimpleBean("ToErase", 123);
        bean.setActive(true);

        Beans.clearProps(bean, "name");
        assertNull(bean.getName());
        assertEquals(123, bean.getAge());
        assertEquals(true, bean.getActive());

        Beans.clearProps(bean, "age", "active");
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testEraseWithCollectionOfProps() {
        SimpleBean bean = new SimpleBean("Erase", 456);
        bean.setActive(false);

        Beans.clearProps(bean, Arrays.asList("name", "age"));
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(false, bean.getActive());
    }

    @Test
    public void testClearProps_EmptyPropNames() {
        SimpleBean bean = new SimpleBean("John", 25);
        Beans.clearProps(bean, new String[0]);
        assertEquals("John", bean.getName());
        assertEquals(25, bean.getAge());
    }

    @Test
    public void testClearProps_WithCollection_EmptyCollection() {
        SimpleBean bean = new SimpleBean("Test", 10);
        Beans.clearProps(bean, Collections.emptyList());
        assertEquals("Test", bean.getName());
        assertEquals(10, bean.getAge());
    }

    @Test
    public void testEraseWithNullBean() {
        assertDoesNotThrow(() -> {
            Beans.clearProps(null, "name");
            Beans.clearProps(null, Arrays.asList("name"));
        });
    }

    @Test
    public void testEraseWithNullBeanDoesNotThrow() {
        assertDoesNotThrow(() -> {
            Beans.clearProps(null, "name");
            Beans.clearProps(null, Arrays.asList("name", "age"));
        });
    }

    @Test
    public void testClearProps_NullBean() {
        assertDoesNotThrow(() -> Beans.clearProps(null, "name"));
    }

    @Test
    public void testClearProps_WithCollection_NullBean() {
        assertDoesNotThrow(() -> Beans.clearProps(null, Arrays.asList("name")));
    }

    @Test
    public void testEraseAll() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.clearAllProps(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());

        Beans.clearAllProps(null);
    }

    @Test
    public void testEraseAllProperties() {
        SimpleBean bean = new SimpleBean("EraseAll", 789);
        bean.setActive(true);

        Beans.clearAllProps(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testEraseAllWithNullBeanDoesNotThrow() {
        assertDoesNotThrow(() -> {
            Beans.clearAllProps(null);
        });
    }

    @Test
    public void testClearAllProps_NullBean() {
        assertDoesNotThrow(() -> Beans.clearAllProps(null));
    }

    @Test
    public void testFillBeanInstance() {
        SimpleBean bean = new SimpleBean();

        Beans.randomize(bean);
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNotNull(bean.getActive());
    }

    @Test
    public void testFillBeanInstanceWithSelectProps() {
        SimpleBean bean = new SimpleBean();

        Beans.randomize(bean, Arrays.asList("name", "age"));
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNull(bean.getActive());
    }

    @Test
    public void testRandomize_WithSelectProps() {
        SimpleBean bean = new SimpleBean();
        Beans.randomize(bean, Arrays.asList("name"));
        assertNotNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testRandomize_NullPropNamesToFill_ThrowsIAE() {
        // a null propNamesToFill now throws the documented IllegalArgumentException (was an NPE)
        SimpleBean bean = new SimpleBean();
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize(bean, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean(SimpleBean.class, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(SimpleBean.class, (Collection<String>) null, 2));
    }

    @Test
    public void testFill() {
        SimpleBean bean = new SimpleBean();
        Beans.randomize(bean);
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNotNull(bean.getActive());

        bean = new SimpleBean();
        Beans.randomize(bean, Arrays.asList("name"));
        assertNotNull(bean.getName());
        assertEquals(0, bean.getAge());

        assertThrows(IllegalArgumentException.class, () -> Beans.randomize((Object) null));
    }

    @Test
    public void testFillWithNullBeanShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean((Class<?>) null));
    }

    @Test
    public void testRandomize_NullBean() {
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize((Object) null));
    }

    @Test
    public void testFillClassWithSelectProps() {
        SimpleBean filled = Beans.newRandomBean(SimpleBean.class, Arrays.asList("name"));
        assertNotNull(filled.getName());
        assertEquals(0, filled.getAge());
        assertNull(filled.getActive());
    }

    @Test
    public void testFillBeanClass() {
        SimpleBean filled = Beans.newRandomBean(SimpleBean.class);
        assertNotNull(filled);
        assertNotNull(filled.getName());
        assertTrue(filled.getAge() != 0);
    }

    @Test
    public void testFillBeanClassWithSelectPropsOnly() {
        SimpleBean filled = Beans.newRandomBean(SimpleBean.class, Arrays.asList("age"));
        assertNotNull(filled);
        assertTrue(filled.getAge() != 0);
        assertNull(filled.getName());
    }

    @Test
    public void testNewRandom_WithSelectProps() {
        SimpleBean bean = Beans.newRandomBean(SimpleBean.class, Arrays.asList("age"));
        assertNotNull(bean);
        assertTrue(bean.getAge() != 0);
        assertNull(bean.getName());
    }

    @Test
    public void testFillClass() {
        SimpleBean filled = Beans.newRandomBean(SimpleBean.class);
        assertNotNull(filled);
        assertNotNull(filled.getName());

        List<SimpleBean> filledList = Beans.newRandomBeanList(SimpleBean.class, 3);
        assertEquals(3, filledList.size());
        for (SimpleBean b : filledList) {
            assertNotNull(b.getName());
        }

        filled = Beans.newRandomBean(SimpleBean.class, Arrays.asList("age"));
        assertTrue(filled.getAge() != 0);
        assertNull(filled.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean((Class<?>) null));
    }

    @Test
    public void testNewRandom_NullClass() {
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean((Class<?>) null));
    }

    @Test
    public void testFillClassWithCount() {
        List<SimpleBean> filledList = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name", "age"), 2);
        assertEquals(2, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean.getName());
            assertTrue(bean.getAge() != 0);
        }
    }

    @Test
    public void testFillClassWithSelectPropsAndCount() {
        List<SimpleBean> filledList = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name", "age"), 3);
        assertEquals(3, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean.getName());
            assertTrue(bean.getAge() != 0);
            assertNull(bean.getActive());
        }
    }

    @Test
    public void testFillBeanClassWithCount() {
        List<SimpleBean> filledList = Beans.newRandomBeanList(SimpleBean.class, 5);
        assertEquals(5, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean);
            assertNotNull(bean.getName());
        }
    }

    @Test
    public void testFillBeanClassWithSelectPropsAndCount() {
        List<SimpleBean> filledList = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name"), 3);
        assertEquals(3, filledList.size());
        for (SimpleBean bean : filledList) {
            assertNotNull(bean.getName());
            assertEquals(0, bean.getAge());
        }
    }

    @Test
    public void testNewRandomList_BasicUsage() {
        List<SimpleBean> list = Beans.newRandomBeanList(SimpleBean.class, 5);
        assertEquals(5, list.size());
        for (SimpleBean bean : list) {
            assertNotNull(bean);
            assertNotNull(bean.getName());
        }
    }

    @Test
    public void testNewRandomList_ZeroCount() {
        List<SimpleBean> list = Beans.newRandomBeanList(SimpleBean.class, 0);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewRandomList_WithSelectProps_ZeroCount() {
        List<SimpleBean> list = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name"), 0);
        assertNotNull(list);
        assertTrue(list.isEmpty());
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

        assertTrue(N.equalsByCommonProps(x, y));

        y.setName("Different");
        assertFalse(N.equalsByCommonProps(x, y));
    }

    @Test
    public void testEqualsByCommonProps() {
        SimpleBean bean1 = new SimpleBean("John", 25);
        EntityBean bean2 = new EntityBean();
        bean2.setValue("test");

        assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(bean1, bean2));

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

        assertTrue(N.equalsByCommonProps(a, b));

        b.setName("Jane");
        assertFalse(N.equalsByCommonProps(a, b));

        assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(null, bean1));
        assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(bean1, null));
    }

    @Test
    public void testEqualsByCommonProps_NullArgs() {
        assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(null, simpleBean));
        assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(simpleBean, null));
    }

    @Test
    public void testEqualsByPropsWithAllPropsEqual() {
        SimpleBean bean1 = new SimpleBean("Equal", 50);
        bean1.setActive(true);
        SimpleBean bean2 = new SimpleBean("Equal", 50);
        bean2.setActive(true);

        assertTrue(N.equalsByProps(bean1, bean2, Arrays.asList("name", "age", "active")));
    }

    @Test
    public void testEqualsByPropsWithSpecificProps() {
        SimpleBean bean1 = new SimpleBean("Same", 100);
        SimpleBean bean2 = new SimpleBean("Same", 200);

        assertTrue(N.equalsByProps(bean1, bean2, Arrays.asList("name")));
        assertFalse(N.equalsByProps(bean1, bean2, Arrays.asList("age")));
        assertFalse(N.equalsByProps(bean1, bean2, Arrays.asList("name", "age")));
    }

    @Test
    public void testEqualsByProps() {
        SimpleBean bean1 = new SimpleBean("John", 25);
        SimpleBean bean2 = new SimpleBean("John", 30);

        assertTrue(N.equalsByProps(bean1, bean2, Arrays.asList("name")));
        assertFalse(N.equalsByProps(bean1, bean2, Arrays.asList("age")));
        assertFalse(N.equalsByProps(bean1, bean2, Arrays.asList("name", "age")));

        assertThrows(IllegalArgumentException.class, () -> N.equalsByProps(bean1, bean2, Collections.emptyList()));
    }

    @Test
    public void testEqualsByPropsEdgeCases() {
        SimpleBean bean1 = new SimpleBean("John", 25);
        SimpleBean bean2 = new SimpleBean("John", 25);

        assertTrue(N.equalsByProps(bean1, bean2, Arrays.asList("name", "age")));

        bean1.setActive(null);
        bean2.setActive(null);
        assertTrue(N.equalsByProps(bean1, bean2, Arrays.asList("name", "age", "active")));

        assertThrows(IllegalArgumentException.class, () -> N.equalsByProps(bean1, bean2, Collections.emptyList()));
    }

    @Test
    public void testEqualsByProps_EmptyPropNames() {
        SimpleBean bean1 = new SimpleBean("John", 25);
        SimpleBean bean2 = new SimpleBean("John", 25);
        assertThrows(IllegalArgumentException.class, () -> N.equalsByProps(bean1, bean2, Collections.emptyList()));
    }

    @Test
    public void testEqualsByProps_NullBeans() {
        assertThrows(IllegalArgumentException.class, () -> N.equalsByProps(null, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.equalsByProps(simpleBean, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.equalsByProps(null, simpleBean, Arrays.asList("name")));
    }

    // Regression: equalsByProps used to delegate to compareByProps, which cast each value to Comparable
    // and threw ClassCastException for non-Comparable properties (List/Map/nested bean). It now compares
    // via N.equals (value equality), so a List-valued property compares correctly without throwing.
    @Test
    public void testEqualsByProps_NonComparableProperty_NoClassCastException() {
        NestedBean b1 = new NestedBean();
        b1.setTags(new ArrayList<>(Arrays.asList("a", "b")));
        NestedBean b2 = new NestedBean();
        b2.setTags(new ArrayList<>(Arrays.asList("a", "b")));

        assertTrue(N.equalsByProps(b1, b2, Arrays.asList("tags")));

        b2.setTags(new ArrayList<>(Arrays.asList("a", "c")));
        assertFalse(N.equalsByProps(b1, b2, Arrays.asList("tags")));
    }

    @Test
    public void testEqualsByCommonProps_NonComparableProperty_NoClassCastException() {
        NestedBean b1 = new NestedBean();
        b1.setId("x");
        b1.setTags(new ArrayList<>(Arrays.asList("a", "b")));
        NestedBean b2 = new NestedBean();
        b2.setId("x");
        b2.setTags(new ArrayList<>(Arrays.asList("a", "b")));

        // Common props include the List-valued "tags" (and the null nested beans): compared by equals, no CCE.
        assertTrue(N.equalsByCommonProps(b1, b2));

        b2.setTags(new ArrayList<>(Arrays.asList("a", "c")));
        assertFalse(N.equalsByCommonProps(b1, b2));
    }

    @Test
    public void testCompareByPropsWithAllEqual() {
        SimpleBean bean1 = new SimpleBean("Equal", 100);
        SimpleBean bean2 = new SimpleBean("Equal", 100);

        assertEquals(0, N.compareByProps(bean1, bean2, Arrays.asList("name", "age")));
    }

    // ===== compareByProps =====

    @Test
    public void testCompareByProps_equal() {
        SimpleBean b1 = new SimpleBean("Tom", 25);
        SimpleBean b2 = new SimpleBean("Tom", 25);
        int result = N.compareByProps(b1, b2, java.util.Arrays.asList("name", "age"));
        assertEquals(0, result);
    }

    @Test
    public void testCompareByProps_less() {
        SimpleBean b1 = new SimpleBean("Anna", 20);
        SimpleBean b2 = new SimpleBean("Zara", 20);
        int result = N.compareByProps(b1, b2, java.util.Arrays.asList("name"));
        assertTrue(result < 0);
    }

    @Test
    public void testCompareByProps_greater() {
        SimpleBean b1 = new SimpleBean("Zara", 30);
        SimpleBean b2 = new SimpleBean("Anna", 30);
        int result = N.compareByProps(b1, b2, java.util.Arrays.asList("name"));
        assertTrue(result > 0);
    }

    @Test
    public void testCompareByPropsWithSingleProp() {
        SimpleBean bean1 = new SimpleBean("Alpha", 10);
        SimpleBean bean2 = new SimpleBean("Beta", 20);

        assertTrue(N.compareByProps(bean1, bean2, Arrays.asList("name")) < 0);
        assertTrue(N.compareByProps(bean2, bean1, Arrays.asList("name")) > 0);
        assertEquals(0, N.compareByProps(bean1, bean1, Arrays.asList("name")));
    }

    @Test
    public void testCompareByProps_NullOrEmptyProps_ThrowsIAE() {
        // empty propNamesToCompare now throws IAE (was: returned 0), matching equalsByProps
        SimpleBean bean1 = new SimpleBean("Alpha", 10);
        SimpleBean bean2 = new SimpleBean("Beta", 20);
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(bean1, bean2, Collections.<String> emptyList()));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(bean1, bean2, (Collection<String>) null));
    }

    @Test
    public void testCompareByPropsWithMultipleProps() {
        SimpleBean bean1 = new SimpleBean("Same", 10);
        SimpleBean bean2 = new SimpleBean("Same", 20);

        assertTrue(N.compareByProps(bean1, bean2, Arrays.asList("name", "age")) < 0);
        assertTrue(N.compareByProps(bean2, bean1, Arrays.asList("name", "age")) > 0);
    }

    @Test
    public void testCompareByProps() {
        SimpleBean bean1 = new SimpleBean("Alice", 25);
        SimpleBean bean2 = new SimpleBean("Bob", 30);

        assertTrue(N.compareByProps(bean1, bean2, Arrays.asList("name")) < 0);
        assertTrue(N.compareByProps(bean2, bean1, Arrays.asList("name")) > 0);
        assertTrue(N.compareByProps(bean1, bean1, Arrays.asList("name")) == 0);

        assertTrue(N.compareByProps(bean1, bean2, Arrays.asList("age")) < 0);

        SimpleBean bean3 = new SimpleBean("Alice", 30);
        assertTrue(N.compareByProps(bean1, bean3, Arrays.asList("name", "age")) < 0);

        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(null, bean2, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(bean1, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(bean1, bean2, null));
    }

    @Test
    public void testCompareByPropsEdgeCases() {
        SimpleBean bean1 = new SimpleBean("Alice", 25);
        SimpleBean bean2 = new SimpleBean("Bob", 25);
        SimpleBean bean3 = new SimpleBean("Alice", 30);

        assertTrue(N.compareByProps(bean1, bean2, Arrays.asList("name")) < 0);

        assertTrue(N.compareByProps(bean1, bean3, Arrays.asList("name", "age")) < 0);

        assertEquals(0, N.compareByProps(bean1, bean1, Arrays.asList("name", "age")));

        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(null, bean2, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(bean1, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(bean1, bean2, null));
    }

    @Test
    public void testCompareByProps_NullArgs() {
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(null, simpleBean, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(simpleBean, null, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(simpleBean, simpleBean, null));
    }

    // ===== compareByProps - invalid property throws =====

    @Test
    public void testCompareByProps_invalidPropThrows() {
        SimpleBean b1 = new SimpleBean("Ned", 30);
        SimpleBean b2 = new SimpleBean("Ned", 30);
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(b1, b2, Arrays.asList("nonExistentProp")));
    }

    // ===== compareByProps - prop not found in bean2 (different bean types) =====

    @Test
    public void testCompareByProps_propNotFoundInBean2Throws() {
        // NestedBean has "id" but SimpleBean does not have "id"
        NestedBean nb = new NestedBean();
        nb.setId("test");
        SimpleBean sb = new SimpleBean("test", 0);
        // "id" exists in NestedBean (bean1) but not in SimpleBean (bean2)
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(nb, sb, Arrays.asList("id")));
    }

    @Test
    public void testPropertiesWithFilter() {
        List<Map.Entry<String, Object>> props = Beans.stream(simpleBean, (name, value) -> value instanceof String).toList();

        assertEquals(1, props.size());
        assertEquals("name", props.get(0).getKey());
        assertEquals("John", props.get(0).getValue());
    }

    @Test
    public void testPropertiesStreamWithTypeFilter() {
        List<Map.Entry<String, Object>> stringProps = Beans.stream(simpleBean, (name, value) -> value instanceof String).toList();

        assertEquals(1, stringProps.size());
        assertEquals("name", stringProps.get(0).getKey());
    }

    @Test
    public void testPropertiesStreamWithNameFilter() {
        List<Map.Entry<String, Object>> filteredProps = Beans.stream(simpleBean, (name, value) -> name.startsWith("a")).toList();

        assertTrue(filteredProps.stream().allMatch(e -> e.getKey().startsWith("a")));
        assertTrue(filteredProps.stream().anyMatch(e -> "age".equals(e.getKey())));
        assertTrue(filteredProps.stream().anyMatch(e -> "active".equals(e.getKey())));
    }

    @Test
    public void testStream_WithFilter() {
        List<Map.Entry<String, Object>> props = Beans.stream(simpleBean, (name, value) -> name.equals("name")).toList();
        assertEquals(1, props.size());
        assertEquals("name", props.get(0).getKey());
        assertEquals("John", props.get(0).getValue());
    }

    @Test
    public void testStream_AllProperties() {
        List<Map.Entry<String, Object>> entries = Beans.stream(simpleBean).toList();
        assertTrue(entries.size() >= 3);

        Map<String, Object> asMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            asMap.put(entry.getKey(), entry.getValue());
        }
        assertEquals("John", asMap.get("name"));
        assertEquals(25, asMap.get("age"));
        assertEquals(true, asMap.get("active"));
    }

    @Test
    public void testPropertiesStreamBasic() {
        List<Map.Entry<String, Object>> props = Beans.stream(simpleBean).toList();

        assertFalse(props.isEmpty());
        assertTrue(props.stream().anyMatch(e -> "name".equals(e.getKey())));
        assertTrue(props.stream().anyMatch(e -> "age".equals(e.getKey())));
    }

    @Test
    public void testPropertiesStreamWithFilter() {
        simpleBean.setActive(null);

        List<Map.Entry<String, Object>> nonNullProps = Beans.stream(simpleBean, (name, value) -> value != null).toList();

        assertTrue(nonNullProps.stream().noneMatch(e -> e.getValue() == null));
        assertTrue(nonNullProps.stream().anyMatch(e -> "name".equals(e.getKey())));
        assertFalse(nonNullProps.stream().anyMatch(e -> "active".equals(e.getKey())));
    }

    @Test
    public void testStream_FilteredToEmpty() {
        List<Map.Entry<String, Object>> entries = Beans.stream(simpleBean, (name, value) -> false).toList();
        assertTrue(entries.isEmpty());
    }

    // ===== stream(Object) and stream(Object, BiPredicate) =====

    @Test
    public void testStream_fromBean() {
        SimpleBean bean = new SimpleBean("Owen", 40);
        bean.setActive(true);
        com.landawn.abacus.util.stream.Stream<Map.Entry<String, Object>> s = Beans.stream(bean);
        assertNotNull(s);
        List<Map.Entry<String, Object>> entries = s.toList();
        assertFalse(entries.isEmpty());
        assertTrue(entries.stream().anyMatch(e -> "name".equals(e.getKey()) && "Owen".equals(e.getValue())));
    }

    @Test
    public void testStream_fromBean_withBiPredicateFilter() {
        SimpleBean bean = new SimpleBean("Paula", 35);
        com.landawn.abacus.util.stream.Stream<Map.Entry<String, Object>> s = Beans.stream(bean, (name, val) -> val != null && val instanceof String);
        assertNotNull(s);
        List<Map.Entry<String, Object>> entries = s.toList();
        assertTrue(entries.stream().allMatch(e -> e.getValue() instanceof String));
        assertTrue(entries.stream().anyMatch(e -> "name".equals(e.getKey())));
        assertFalse(entries.stream().anyMatch(e -> "age".equals(e.getKey())));
    }

    @Test
    public void testStream_fromBean_withNullFilter() {
        SimpleBean bean = new SimpleBean("Quinn", 22);
        // active is null
        com.landawn.abacus.util.stream.Stream<Map.Entry<String, Object>> s = Beans.stream(bean, (name, val) -> val != null);
        List<Map.Entry<String, Object>> entries = s.toList();
        assertFalse(entries.stream().anyMatch(e -> "active".equals(e.getKey())));
        assertTrue(entries.stream().anyMatch(e -> "name".equals(e.getKey())));
    }

    @Test
    public void testProperties() {
        List<Map.Entry<String, Object>> props = Beans.stream(simpleBean).toList();

        assertFalse(props.isEmpty());
        assertTrue(props.stream().anyMatch(e -> "name".equals(e.getKey()) && "John".equals(e.getValue())));
        assertTrue(props.stream().anyMatch(e -> "age".equals(e.getKey()) && Integer.valueOf(25).equals(e.getValue())));

        simpleBean.setActive(null);
        props = Beans.stream(simpleBean, (name, value) -> value != null).toList();

        assertFalse(props.stream().anyMatch(e -> "active".equals(e.getKey())));

        assertThrows(IllegalArgumentException.class, () -> Beans.stream(null));
    }

    @Test
    public void testPropertiesEdgeCases() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(null);

        List<Map.Entry<String, Object>> allProps = Beans.stream(bean).toList();
        assertTrue(allProps.size() >= 3);

        List<Map.Entry<String, Object>> filteredByName = Beans.stream(bean, (name, value) -> name.startsWith("n")).toList();
        assertEquals(1, filteredByName.size());
        assertEquals("name", filteredByName.get(0).getKey());

        List<Map.Entry<String, Object>> nonNulls = Beans.stream(bean, (name, value) -> value != null).toList();
        assertFalse(nonNulls.stream().anyMatch(e -> e.getValue() == null));

        assertThrows(IllegalArgumentException.class, () -> Beans.stream(null));
    }

    @Test
    public void testStream_NullBean() {
        assertThrows(IllegalArgumentException.class, () -> Beans.stream(null));
    }

    @Test
    public void testBeanToMap_ignoredPropNamesLargerThanBeanProps() {
        // Regression: when the ignoredPropNames set is larger than the number of bean
        // properties, the computed initial map capacity went negative and the map supplier
        // threw IllegalArgumentException ("'size' can not be negative"). It should instead
        // just produce an (effectively empty) map.
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(Boolean.TRUE);

        // SimpleBean has 3 properties (name, age, active); ignore more names than that.
        Set<String> ignored = new HashSet<>(Arrays.asList("name", "age", "active", "x1", "x2", "x3", "x4"));

        Map<String, Object> map = assertDoesNotThrow(() -> Beans.beanToMap(bean, false, ignored));
        assertNotNull(map);
        assertTrue(map.isEmpty());

        Map<String, Object> deepMap = assertDoesNotThrow(() -> Beans.deepBeanToMap(bean, false, ignored));
        assertNotNull(deepMap);
        assertTrue(deepMap.isEmpty());

        Map<String, Object> flatMap = assertDoesNotThrow(() -> Beans.beanToFlatMap(bean, false, ignored));
        assertNotNull(flatMap);
        assertTrue(flatMap.isEmpty());
    }

    /**
     * Regression test for: getPropNames(Class, Set) with an exclusion set that is larger
     * than the number of properties in the class must NOT throw IllegalArgumentException
     * from ArrayList(negative-capacity). SimpleBean has 3 props; we exclude 7 names.
     */
    @Test
    public void testGetPropNames_ExclusionSetLargerThanPropList_doesNotThrow() {
        // SimpleBean has exactly 3 properties: name, age, active.
        // Passing a set with 7 entries (more than 3) previously computed a negative
        // initial capacity for ArrayList (3 - 7 == -4), causing IllegalArgumentException.
        Set<String> oversizedExclude = new HashSet<>(Arrays.asList("name", "age", "active", "x1", "x2", "x3", "x4"));

        List<String> result = assertDoesNotThrow(() -> Beans.getPropNames(SimpleBean.class, oversizedExclude));

        assertNotNull(result);
        assertTrue(result.isEmpty(), "All props are excluded so result should be empty");
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD })
    public @interface DifferenceIgnore {
    }

    public static class DiffProbeBean {
        private String name;
        @DifferenceIgnore
        private String secret;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(final String secret) {
            this.secret = secret;
        }
    }

    @org.junit.jupiter.api.Test
    public void testGetIgnoredPropNamesForDiffMatchesThirdPartyAnnotationByName() {
        // regression: the name match used getClass().getSimpleName() on the annotation proxy
        // ("$ProxyN"), so non-abacus DiffIgnore/DifferenceIgnore annotations never matched
        assertEquals(N.asSet("secret"), Beans.getIgnoredPropNamesForDiff(DiffProbeBean.class));
    }

    public static class XmlRegistryProbeBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @org.junit.jupiter.api.Test
    public void testXmlBindingRegistryDistinguishesNonBeanEntries() {
        // regression: registerNonBeanClass stores a FALSE entry in the same map, but
        // isRegisteredXmlBindingClass used containsKey and reported TRUE for it (and
        // registerXmlBindingClass silently no-op'd); isBeanClass also kept a stale cached TRUE
        assertTrue(Beans.isBeanClass(XmlRegistryProbeBean.class));

        Beans.registerNonBeanClass(XmlRegistryProbeBean.class);

        assertFalse(Beans.isRegisteredXmlBindingClass(XmlRegistryProbeBean.class));
        assertFalse(Beans.isBeanClass(XmlRegistryProbeBean.class));

        Beans.registerXmlBindingClass(XmlRegistryProbeBean.class);

        assertTrue(Beans.isRegisteredXmlBindingClass(XmlRegistryProbeBean.class));
    }

    public static class ConverterProbeBean {
        private String name;
        private String nickName;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(final String nickName) {
            this.nickName = nickName;
        }
    }

    @org.junit.jupiter.api.Test
    public void testCopyAsHonorsPropNameConverterForSameType() {
        // regression: the Kryo shallow-copy shortcut (taken when targetType == source class)
        // silently ignored a non-identity propNameConverter
        final ConverterProbeBean src = new ConverterProbeBean();
        src.setName("John");
        src.setNickName("Johnny");

        final java.util.function.Function<String, String> swap = p -> p.equals("name") ? "nickName" : (p.equals("nickName") ? "name" : p);
        final ConverterProbeBean copy = Beans.copyAs(src, (Collection<String>) null, swap, ConverterProbeBean.class);

        assertEquals("Johnny", copy.getName());
        assertEquals("John", copy.getNickName());
    }
}
