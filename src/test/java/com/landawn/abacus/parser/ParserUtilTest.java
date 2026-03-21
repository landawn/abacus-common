package com.landawn.abacus.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.JsonNameTag;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.ParserUtil.XmlNameTag;
import com.landawn.abacus.parser.entity.ImmutableBuilderEntity;
import com.landawn.abacus.parser.entity.ImmutableBuilderEntity2;
import com.landawn.abacus.parser.entity.ImmutableBuilderEntity3;
import com.landawn.abacus.parser.entity.ImmutableEntity4;
import com.landawn.abacus.parser.entity.ImmutableEntity5;
import com.landawn.abacus.parser.entity.ImmutableEntity6;
import com.landawn.abacus.parser.entity.RecordB;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;
import lombok.experimental.Accessors;

public class ParserUtilTest extends AbstractTest {

    @Value
    public static class ImmutableEntity7 {
        private int id;
        private String firstName;
        private String lastName;
    }

    @Value
    @Accessors(fluent = true)
    public static class ImmutableEntity8 {
        int id;
        String firstName;
        String lastName;
    }

    @Value
    public static class ImmutableEntity9 {
        int id;
        String firstName;
        String lastName;

        public static class ImmutableEntity9Builder {
            private int id;
            private String firstName;
            private String lastName;

            public static ImmutableEntity9Builder createBuilder() {
                return new ImmutableEntity9Builder();
            }

            public ImmutableEntity9Builder id(final int id) {
                this.id = id;

                return this;
            }

            public ImmutableEntity9Builder firstName(final String firstName) {
                this.firstName = firstName;

                return this;
            }

            public ImmutableEntity9Builder lastName(final String lastName) {
                this.lastName = lastName;

                return this;
            }

            public ImmutableEntity9 create() {
                return new ImmutableEntity9(id, firstName, lastName);
            }
        }
    }

    public static record RecordA(int id, String firstName, String lastName) {

    }

    @Table("test_bean")
    public static class TestBean {
        @Id
        private long id;

        @Column("name_column")
        private String name;

        @JsonXmlField(name = "customAge")
        private int age;
        private Date createdDate;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
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

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }
    }

    public static class EmptyClass {
    }

    @JsonXmlConfig(ignoredFields = { "internal.*" }, dateFormat = "yyyy/MM/dd")
    public static class ConfigAnnotatedBean {
        private Date createdAt;
        private String internalCode;
    }

    public static class JacksonAnnotatedBean {
        @JsonProperty("first_name")
        @JsonAlias({ "firstName", "fname" })
        private String firstName;
    }

    @Table("test_bean2")
    public static class TestBean2 {
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

    // =====================================================================
    // isJsonXmlSerializable(Field, JsonXmlConfig)
    // =====================================================================

    @Test
    public void testIsJsonXmlSerializable_nullField() {
        assertTrue(ParserUtil.isJsonXmlSerializable(null, null));
    }

    @Test
    public void testIsJsonXmlSerializable_staticField() throws Exception {
        // ParserUtil itself has static fields, e.g., logger
        Field staticField = ParserUtil.class.getDeclaredField("logger");
        assertTrue(Modifier.isStatic(staticField.getModifiers()));
        assertFalse(ParserUtil.isJsonXmlSerializable(staticField, null));
    }

    @Test
    public void testIsJsonXmlSerializable_normalField() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        assertTrue(ParserUtil.isJsonXmlSerializable(nameField, null));
    }

    @Test
    public void testIsJsonXmlSerializable_normalFieldNoConfig() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        assertTrue(ParserUtil.isJsonXmlSerializable(ageField, null));
    }

    @Test
    public void testIsJsonXmlSerializable_ignoredFieldsRegex() throws Exception {
        Field field = ConfigAnnotatedBean.class.getDeclaredField("internalCode");
        JsonXmlConfig config = ConfigAnnotatedBean.class.getAnnotation(JsonXmlConfig.class);
        assertFalse(ParserUtil.isJsonXmlSerializable(field, config));
    }

    // =====================================================================
    // getDateFormat(Field, JsonXmlConfig)
    // =====================================================================

    @Test
    public void testGetDateFormat_nullField() {
        assertNull(ParserUtil.getDateFormat(null, null));
    }

    @Test
    public void testGetDateFormat_fieldWithoutAnnotation() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        assertNull(ParserUtil.getDateFormat(nameField, null));
    }

    @Test
    public void testGetDateFormat_fieldWithNoDateFormat() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        // age has @JsonXmlField(name="customAge") but no dateFormat
        assertNull(ParserUtil.getDateFormat(ageField, null));
    }

    @Test
    public void testGetDateFormat_fromJsonXmlConfig() throws Exception {
        Field field = ConfigAnnotatedBean.class.getDeclaredField("createdAt");
        JsonXmlConfig config = ConfigAnnotatedBean.class.getAnnotation(JsonXmlConfig.class);
        assertEquals("yyyy/MM/dd", ParserUtil.getDateFormat(field, config));
    }

    // =====================================================================
    // getTimeZone(Field, JsonXmlConfig)
    // =====================================================================

    @Test
    public void testGetTimeZone_nullField() {
        assertNull(ParserUtil.getTimeZone(null, null));
    }

    @Test
    public void testGetTimeZone_fieldWithoutAnnotation() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        assertNull(ParserUtil.getTimeZone(nameField, null));
    }

    @Test
    public void testGetTimeZone_fieldWithNoTimeZone() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        assertNull(ParserUtil.getTimeZone(ageField, null));
    }

    // =====================================================================
    // getNumberFormat(Field, JsonXmlConfig)
    // =====================================================================

    @Test
    public void testGetNumberFormat_nullField() {
        assertNull(ParserUtil.getNumberFormat(null, null));
    }

    @Test
    public void testGetNumberFormat_fieldWithoutAnnotation() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        assertNull(ParserUtil.getNumberFormat(nameField, null));
    }

    @Test
    public void testGetNumberFormat_fieldWithNoNumberFormat() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        assertNull(ParserUtil.getNumberFormat(ageField, null));
    }

    // =====================================================================
    // getEnumerated(Field, JsonXmlConfig)
    // =====================================================================

    @Test
    public void testGetEnumerated_nullField() {
        assertEquals(com.landawn.abacus.util.EnumType.NAME, ParserUtil.getEnumerated(null, null));
    }

    @Test
    public void testGetEnumerated_fieldWithoutAnnotation() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        assertEquals(com.landawn.abacus.util.EnumType.NAME, ParserUtil.getEnumerated(nameField, null));
    }

    @Test
    public void testGetEnumerated_fieldWithJsonXmlFieldAnnotation() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        // Has @JsonXmlField but no custom enumerated setting, defaults to NAME
        assertNotNull(ParserUtil.getEnumerated(ageField, null));
    }

    // =====================================================================
    // isJsonRawValue(Field)
    // =====================================================================

    @Test
    public void testIsJsonRawValue_nullField() {
        assertFalse(ParserUtil.isJsonRawValue(null));
    }

    @Test
    public void testIsJsonRawValue_normalField() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        assertFalse(ParserUtil.isJsonRawValue(nameField));
    }

    @Test
    public void testIsJsonRawValue_annotatedFieldNotRaw() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        // Has @JsonXmlField but isJsonRawValue defaults to false
        assertFalse(ParserUtil.isJsonRawValue(ageField));
    }

    // =====================================================================
    // getJsonNameTags(String)
    // =====================================================================

    @Test
    public void testGetJsonNameTags_simpleName() {
        JsonNameTag[] tags = ParserUtil.getJsonNameTags("firstName");
        assertNotNull(tags);
        assertEquals(NamingPolicy.values().length, tags.length);
        // CAMEL_CASE index should return "firstName"
        assertArrayEquals("firstName".toCharArray(), tags[NamingPolicy.CAMEL_CASE.ordinal()].name);
    }

    @Test
    public void testGetJsonNameTags_singleWord() {
        JsonNameTag[] tags = ParserUtil.getJsonNameTags("name");
        assertNotNull(tags);
        // All naming policies for a single lowercase word should produce "name" or "Name" etc.
        assertArrayEquals("name".toCharArray(), tags[NamingPolicy.CAMEL_CASE.ordinal()].name);
    }

    @Test
    public void testGetJsonNameTags_namingPolicies() {
        JsonNameTag[] tags = ParserUtil.getJsonNameTags("firstName");
        // UPPER_CAMEL_CASE should produce "FirstName"
        assertArrayEquals("FirstName".toCharArray(), tags[NamingPolicy.UPPER_CAMEL_CASE.ordinal()].name);
        // SNAKE_CASE should produce "first_name"
        assertArrayEquals("first_name".toCharArray(), tags[NamingPolicy.SNAKE_CASE.ordinal()].name);
    }

    @Test
    public void testGetJsonNameTags_withNullField() {
        JsonNameTag[] tags = ParserUtil.getJsonNameTags("myProp", null);
        assertNotNull(tags);
        assertEquals(NamingPolicy.values().length, tags.length);
        assertArrayEquals("myProp".toCharArray(), tags[NamingPolicy.CAMEL_CASE.ordinal()].name);
    }

    // =====================================================================
    // getJsonNameTags(String, Field)
    // =====================================================================

    @Test
    public void testGetJsonNameTags_withField_noAnnotation() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        JsonNameTag[] tags = ParserUtil.getJsonNameTags("name", nameField);
        assertNotNull(tags);
        assertEquals(NamingPolicy.values().length, tags.length);
        assertArrayEquals("name".toCharArray(), tags[NamingPolicy.CAMEL_CASE.ordinal()].name);
    }

    @Test
    public void testGetJsonNameTags_withField_customName() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        // @JsonXmlField(name = "customAge")
        JsonNameTag[] tags = ParserUtil.getJsonNameTags("age", ageField);
        assertNotNull(tags);
        // All naming policies should use the custom name
        for (JsonNameTag tag : tags) {
            assertArrayEquals("customAge".toCharArray(), tag.name);
        }
    }

    @Test
    public void testGetJsonNameTags_withJsonPropertyAnnotation() throws Exception {
        Field field = JacksonAnnotatedBean.class.getDeclaredField("firstName");
        JsonNameTag[] tags = ParserUtil.getJsonNameTags("firstName", field);

        assertEquals(NamingPolicy.values().length, tags.length);

        for (JsonNameTag tag : tags) {
            assertEquals("first_name", new String(tag.name));
        }
    }

    // =====================================================================
    // getXmlNameTags(String, String, boolean)
    // =====================================================================

    @Test
    public void testGetXmlNameTags_simpleName() {
        XmlNameTag[] tags = ParserUtil.getXmlNameTags("firstName", "string", false);
        assertNotNull(tags);
        assertEquals(NamingPolicy.values().length, tags.length);
    }

    @Test
    public void testGetXmlNameTags_withBean() {
        XmlNameTag[] tags = ParserUtil.getXmlNameTags("myBean", "MyBean", true);
        assertNotNull(tags);
        assertEquals(NamingPolicy.values().length, tags.length);
    }

    @Test
    public void testGetXmlNameTags_withNullField() {
        XmlNameTag[] tags = ParserUtil.getXmlNameTags("myProp", null, "int", false);
        assertNotNull(tags);
        assertEquals(NamingPolicy.values().length, tags.length);
    }

    // =====================================================================
    // getXmlNameTags(String, Field, String, boolean)
    // =====================================================================

    @Test
    public void testGetXmlNameTags_withField() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        XmlNameTag[] tags = ParserUtil.getXmlNameTags("name", nameField, "string", false);
        assertNotNull(tags);
        assertEquals(NamingPolicy.values().length, tags.length);
    }

    @Test
    public void testGetXmlNameTags_withCustomNameField() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        XmlNameTag[] tags = ParserUtil.getXmlNameTags("age", ageField, "int", false);
        assertNotNull(tags);
        // All naming policies should use the custom name "customAge"
        for (XmlNameTag tag : tags) {
            assertNotNull(tag);
        }
    }

    @Test
    public void testGetXmlNameTags_withJsonPropertyAnnotation() throws Exception {
        Field field = JacksonAnnotatedBean.class.getDeclaredField("firstName");
        XmlNameTag[] tags = ParserUtil.getXmlNameTags("firstName", field, "string", false);

        assertEquals(NamingPolicy.values().length, tags.length);

        for (XmlNameTag tag : tags) {
            assertEquals("first_name", new String(tag.name));
        }
    }

    // =====================================================================
    // getAliases(Field)
    // =====================================================================

    @Test
    public void testGetAliases_nullField() {
        String[] aliases = ParserUtil.getAliases(null);
        assertNull(aliases);
    }

    @Test
    public void testGetAliases_fieldWithoutAliases() throws Exception {
        Field nameField = TestBean.class.getDeclaredField("name");
        String[] aliases = ParserUtil.getAliases(nameField);
        // name field does not have alias annotations
        assertTrue(aliases == null || aliases.length == 0);
    }

    @Test
    public void testGetAliases_fieldWithJsonXmlFieldNoAliases() throws Exception {
        Field ageField = TestBean.class.getDeclaredField("age");
        String[] aliases = ParserUtil.getAliases(ageField);
        // @JsonXmlField(name="customAge") but no aliases
        assertTrue(aliases == null || aliases.length == 0);
    }

    @Test
    public void testGetAliases_withJsonAliasAnnotation() throws Exception {
        Field field = JacksonAnnotatedBean.class.getDeclaredField("firstName");
        String[] aliases = ParserUtil.getAliases(field);
        Assertions.assertArrayEquals(new String[] { "fname" }, aliases);
    }

    @Test
    public void testHashCode_charArray_differentStrings() {
        int hash1 = ParserUtil.hashCode("abc".toCharArray());
        int hash2 = ParserUtil.hashCode("xyz".toCharArray());
        assertNotEquals(hash1, hash2);
    }

    // =====================================================================
    // hashCode(char[], int, int)
    // =====================================================================

    @Test
    public void testHashCode_charArrayRange() {
        char[] chars = "hello world".toCharArray();
        int hash = ParserUtil.hashCode(chars, 0, 5);
        assertEquals(ParserUtil.hashCode("hello".toCharArray(), 0, 5), hash);
    }

    @Test
    public void testHashCode_charArrayRange_fullArray() {
        char[] chars = "test".toCharArray();
        int hashFull = ParserUtil.hashCode(chars, 0, chars.length);
        assertNotEquals(0, hashFull);
    }

    @Test
    public void testHashCode_charArrayRange_partialArray() {
        char[] chars = "abcdef".toCharArray();
        int hash1 = ParserUtil.hashCode(chars, 1, 4); // "bcd"
        int hash2 = ParserUtil.hashCode("bcd".toCharArray(), 0, 3);
        assertEquals(hash1, hash2);
    }

    // =====================================================================
    // hashCode(char[])
    // =====================================================================

    @Test
    public void testHashCode_charArray() {
        char[] a = "hello".toCharArray();
        int hash = ParserUtil.hashCode(a);
        // Same input produces same hash
        assertEquals(hash, ParserUtil.hashCode("hello".toCharArray()));
    }

    @Test
    public void testHashCode_charArray_emptyArray() {
        int hash = ParserUtil.hashCode(new char[0]);
        assertEquals(1, hash); // hash starts with 1, no iterations
    }

    @Test
    public void testHashCode_charArray_singleChar() {
        int hash = ParserUtil.hashCode("a".toCharArray());
        assertEquals(31 + 'a', hash);
    }

    @Test
    public void testHashCode_charArrayRange_emptyRange() {
        char[] chars = "test".toCharArray();
        int hash = ParserUtil.hashCode(chars, 2, 2);
        // Empty range
        assertNotNull(hash);
    }

    @Test
    public void testGetBeanInfo_tableName() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertTrue(beanInfo.tableName.isPresent());
        assertEquals("test_bean", beanInfo.tableName.get());
    }

    @Test
    public void testGetBeanInfo_tableNameNotPresent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        assertFalse(beanInfo.tableName.isPresent());
    }

    @Test
    public void testGetBeanInfo_isImmutable() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertFalse(beanInfo.isImmutable);
    }

    @Test
    public void testGetBeanInfo_isImmutable_record() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        assertTrue(beanInfo.isImmutable);
    }

    @Test
    public void testGetBeanInfo_isMarkedAsBean() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        // TestBean is not annotated with @Entity
        assertFalse(beanInfo.isMarkedAsBean);
    }

    @Test
    public void testGetBeanInfo_propInfoListSize() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        // TestBean has 4 properties: id, name, age, createdDate
        assertEquals(4, beanInfo.propInfoList.size());
        assertEquals(4, beanInfo.propNameList.size());
    }

    @Test
    public void testGetBeanInfo_propNameListContents() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertTrue(beanInfo.propNameList.contains("id"));
        assertTrue(beanInfo.propNameList.contains("name"));
        assertTrue(beanInfo.propNameList.contains("age"));
        assertTrue(beanInfo.propNameList.contains("createdDate"));
    }

    // =====================================================================
    // BeanInfo.getPropValue(Object, String)
    // =====================================================================

    @Test
    public void testBeanInfoGetPropValue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        String name = beanInfo.getPropValue(bean, "name");
        assertEquals("John", name);

        int age = beanInfo.getPropValue(bean, "age");
        assertEquals(30, age);
    }

    @Test
    public void testBeanInfoGetPropValue_defaultPrimitive() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        long id = beanInfo.getPropValue(bean, "id");
        assertEquals(0L, id);
    }

    // =====================================================================
    // BeanInfo.setPropValue(Object, String, Object)
    // =====================================================================

    @Test
    public void testBeanInfoSetPropValue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();

        beanInfo.setPropValue(bean, "name", "Jane");
        assertEquals("Jane", bean.getName());

        beanInfo.setPropValue(bean, "age", 25);
        assertEquals(25, bean.getAge());
    }

    @Test
    public void testBeanInfoSetPropValue_allFields() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        Date now = new Date();

        beanInfo.setPropValue(bean, "id", 42L);
        beanInfo.setPropValue(bean, "name", "Test");
        beanInfo.setPropValue(bean, "age", 99);
        beanInfo.setPropValue(bean, "createdDate", now);

        assertEquals(42L, bean.getId());
        assertEquals("Test", bean.getName());
        assertEquals(99, bean.getAge());
        assertEquals(now, bean.getCreatedDate());
    }

    @Test
    public void testBeanInfoSetPropValue_matchedReturnsTrue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();

        boolean result = beanInfo.setPropValue(bean, "name", "TestValue", true);
        assertTrue(result);
        assertEquals("TestValue", bean.getName());
    }

    @Test
    public void testBeanInfoSetPropValue_ignoreTrue_matchedReturnsTrue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();

        boolean result = beanInfo.setPropValue(bean, "age", 50, true);
        assertTrue(result);
        assertEquals(50, bean.getAge());
    }

    @Test
    public void testBeanInfoSetPropValue_ignoreFalse_matchedReturnsTrue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();

        boolean result = beanInfo.setPropValue(bean, "age", 55, false);
        assertTrue(result);
        assertEquals(55, bean.getAge());
    }

    // =====================================================================
    // BeanInfo.setPropValue(Object, PropInfo, Object)
    // =====================================================================

    @Test
    public void testBeanInfoSetPropValueByPropInfo() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        PropInfo propInfo = beanInfo.getPropInfo("name");

        beanInfo.setPropValue(bean, propInfo, "TestName");
        assertEquals("TestName", bean.getName());
    }

    @Test
    public void testBeanInfoSetPropValueByPropInfo_id() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        PropInfo propInfo = beanInfo.getPropInfo("id");

        beanInfo.setPropValue(bean, propInfo, 100L);
        assertEquals(100L, bean.getId());
    }

    // =====================================================================
    // BeanInfo.setPropValue(Object, PropInfo, Object, boolean)
    // =====================================================================

    @Test
    public void testBeanInfoSetPropValueByPropInfo_withIgnoreUnmatchedProperty() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);
        TestBean2 bean2 = new TestBean2();

        PropInfo namePropFromBean1 = beanInfo1.getPropInfo("name");
        boolean result = beanInfo2.setPropValue(bean2, namePropFromBean1, "CrossBeanValue", true);
        assertTrue(result);
        assertEquals("CrossBeanValue", bean2.getName());
    }

    @Test
    public void testBeanInfoSetPropValueByPropInfo_unmatchedIgnored() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);
        TestBean2 bean2 = new TestBean2();

        PropInfo agePropFromBean1 = beanInfo1.getPropInfo("age");
        boolean result = beanInfo2.setPropValue(bean2, agePropFromBean1, 99, true);
        // "age" doesn't exist in TestBean2, so should return false
        assertFalse(result);
    }

    // =====================================================================
    // BeanInfo.isAnnotationPresent
    // =====================================================================

    @Test
    public void testBeanInfoIsAnnotationPresent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        assertTrue(beanInfo.isAnnotationPresent(Table.class));
        assertFalse(beanInfo.isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void testBeanInfoIsAnnotationPresent_noAnnotations() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        assertFalse(beanInfo.isAnnotationPresent(Table.class));
    }

    // =====================================================================
    // BeanInfo.hashCode
    // =====================================================================

    @Test
    public void testBeanInfoHashCode() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);

        assertEquals(beanInfo1.hashCode(), beanInfo2.hashCode());
    }

    @Test
    public void testBeanInfoHashCode_differentClasses() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);

        assertNotEquals(beanInfo1.hashCode(), beanInfo2.hashCode());
    }

    @Test
    public void testBeanInfoHashCode_consistent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        int hash1 = beanInfo.hashCode();
        int hash2 = beanInfo.hashCode();
        assertEquals(hash1, hash2);
    }

    // =====================================================================
    // BeanInfo.equals
    // =====================================================================

    @Test
    public void testBeanInfoEquals() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);

        assertTrue(beanInfo1.equals(beanInfo2));
        assertTrue(beanInfo1.equals(beanInfo1));
    }

    @Test
    public void testBeanInfoEquals_differentClasses() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);

        assertFalse(beanInfo1.equals(beanInfo2));
    }

    @Test
    public void testBeanInfoEquals_symmetry() {
        BeanInfo a = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo b = ParserUtil.getBeanInfo(TestBean.class);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
    }

    @Test
    public void testBeanInfoToString_matchesCanonicalName() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertEquals(beanInfo.canonicalClassName, beanInfo.toString());
    }

    // =====================================================================
    // PropInfo.getPropValue(Object)
    // =====================================================================

    @Test
    public void testPropInfoGetPropValue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        TestBean bean = new TestBean();
        bean.setName("TestValue");

        String value = propInfo.getPropValue(bean);
        assertEquals("TestValue", value);
    }

    @Test
    public void testPropInfoGetPropValue_primitiveDefault() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");

        TestBean bean = new TestBean();

        int value = propInfo.getPropValue(bean);
        assertEquals(0, value);
    }

    @Test
    public void testPropInfoGetPropValue_longField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        TestBean bean = new TestBean();
        bean.setId(12345L);

        long value = propInfo.getPropValue(bean);
        assertEquals(12345L, value);
    }

    @Test
    public void testPropInfoGetPropValue_dateField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("createdDate");

        TestBean bean = new TestBean();
        Date now = new Date();
        bean.setCreatedDate(now);

        Date value = propInfo.getPropValue(bean);
        assertEquals(now, value);
    }

    // =====================================================================
    // PropInfo.setPropValue(Object, Object)
    // =====================================================================

    @Test
    public void testPropInfoSetPropValue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        TestBean bean = new TestBean();
        propInfo.setPropValue(bean, "NewValue");

        assertEquals("NewValue", bean.getName());
    }

    @Test
    public void testPropInfoSetPropValue_withTypeConversion() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");

        TestBean bean = new TestBean();
        propInfo.setPropValue(bean, 42);

        assertEquals(42, bean.getAge());
    }

    @Test
    public void testPropInfoSetPropValue_dateField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("createdDate");

        TestBean bean = new TestBean();
        Date now = new Date();
        propInfo.setPropValue(bean, now);

        assertEquals(now, bean.getCreatedDate());
    }

    @Test
    public void testPropInfoSetPropValue_stringConversion() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        TestBean bean = new TestBean();
        propInfo.setPropValue(bean, "999");

        assertEquals(999L, bean.getId());
    }

    // =====================================================================
    // PropInfo.isAnnotationPresent
    // =====================================================================

    @Test
    public void testPropInfoIsAnnotationPresent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertTrue(propInfo.isAnnotationPresent(Column.class));
        assertFalse(propInfo.isAnnotationPresent(Id.class));
    }

    @Test
    public void testPropInfoIsAnnotationPresent_id() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");
        assertTrue(propInfo.isAnnotationPresent(Id.class));
    }

    @Test
    public void testPropInfoIsAnnotationPresent_jsonXmlField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");
        assertTrue(propInfo.isAnnotationPresent(JsonXmlField.class));
    }

    @Test
    public void testPropInfoIsAnnotationPresent_noAnnotation() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("createdDate");
        assertFalse(propInfo.isAnnotationPresent(Column.class));
        assertFalse(propInfo.isAnnotationPresent(Id.class));
    }

    @Test
    public void testPropInfoHashCode_consistent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");
        int h1 = propInfo.hashCode();
        int h2 = propInfo.hashCode();
        assertEquals(h1, h2);
    }

    @Test
    public void testPropInfoToString_allProps() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertEquals("id", beanInfo.getPropInfo("id").toString());
        assertEquals("name", beanInfo.getPropInfo("name").toString());
        assertEquals("age", beanInfo.getPropInfo("age").toString());
        assertEquals("createdDate", beanInfo.getPropInfo("createdDate").toString());
    }

    @Test
    public void testPropInfoPublicFields_columnName() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertTrue(propInfo.columnName.isPresent());
        assertEquals("name_column", propInfo.columnName.get());
    }

    @Test
    public void testPropInfoPublicFields_columnNameAbsent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");
        assertFalse(propInfo.columnName.isPresent());
    }

    @Test
    public void testPropInfoPublicFields_isTransient() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertFalse(propInfo.isTransient);
    }

    @Test
    public void testPropInfoPublicFields_isMarkedAsColumn() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertTrue(propInfo.isMarkedAsColumn);
    }

    @Test
    public void testPropInfoPublicFields_isMarkedAsColumn_false() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");
        assertFalse(propInfo.isMarkedAsColumn);
    }

    @Test
    public void testPropInfoPublicFields_isSubEntity() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertFalse(propInfo.isSubEntity);
    }

    @Test
    public void testPropInfoPublicFields_isMarkedAsReadOnlyId() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        assertFalse(propInfo.isMarkedAsReadOnlyId);
    }

    @Test
    public void testPropInfoPublicFields_declaringClass() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");
        assertEquals(TestBean.class, propInfo.declaringClass);
    }

    @Test
    public void testPropInfoPublicFields_clazz() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        PropInfo nameProp = beanInfo.getPropInfo("name");
        assertEquals(String.class, nameProp.clazz);

        PropInfo ageProp = beanInfo.getPropInfo("age");
        assertEquals(int.class, ageProp.clazz);

        PropInfo idProp = beanInfo.getPropInfo("id");
        assertEquals(long.class, idProp.clazz);
    }

    // =====================================================================
    // Existing integration/feature tests
    // =====================================================================

    @Test
    public void test_setPropValue() {
        final Account account = Beans.newRandom(Account.class);
        N.println(account);
        Beans.setPropValue(account, Beans.getPropSetter(Account.class, "id"), 1);
        assertEquals(1, account.getId());

        Beans.setPropValue(account, Beans.getPropSetter(Account.class, "id"), "101");
        assertEquals(101, account.getId());

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        beanInfo.getPropInfo("id").setPropValue(account, 102);
        assertEquals(102, account.getId());
        beanInfo.getPropInfo("id").setPropValue(account, "103");
        assertEquals(103, account.getId());

        beanInfo.getPropInfo("lastName").setPropValue(account, 201);
        assertEquals("201", account.getLastName());
        beanInfo.getPropInfo("lastName").setPropValue(account, "202");
        assertEquals("202", account.getLastName());
    }

    // =====================================================================
    // getBeanInfo(java.lang.reflect.Type)
    // =====================================================================

    @Test
    public void testGetBeanInfo() {
        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        Assertions.assertNotNull(beanInfo);
        Assertions.assertEquals(TestBean.class, beanInfo.clazz);
        Assertions.assertEquals("TestBean", beanInfo.simpleClassName);

        ParserUtil.BeanInfo cachedBeanInfo = ParserUtil.getBeanInfo(TestBean.class);
        Assertions.assertSame(beanInfo, cachedBeanInfo);
    }

    @Test
    public void testGetBeanInfo_withJavaLangReflectType() {
        java.lang.reflect.Type type = TestBean.class;
        BeanInfo beanInfo = ParserUtil.getBeanInfo(type);
        assertNotNull(beanInfo);
        assertEquals(TestBean.class, beanInfo.clazz);
    }

    @Test
    public void testGetBeanInfo_cachedInstance() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);
        assertSame(beanInfo1, beanInfo2);
    }

    @Test
    public void testGetBeanInfo_publicFields() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.propInfoList);
        assertFalse(beanInfo.propInfoList.isEmpty());
        assertNotNull(beanInfo.propNameList);
        assertFalse(beanInfo.propNameList.isEmpty());
    }

    @Test
    public void testGetBeanInfo_canonicalClassName() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.canonicalClassName);
        assertTrue(beanInfo.canonicalClassName.contains("TestBean"));
    }

    @Test
    public void testGetBeanInfo_javaType() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.javaType);
        assertEquals(TestBean.class, beanInfo.javaType);
    }

    @Test
    public void testGetBeanInfo_typeField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.type);
    }

    @Test
    public void testGetBeanInfo_idPropNameList() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.idPropNameList);
        assertTrue(beanInfo.idPropNameList.contains("id"));
    }

    @Test
    public void testGetBeanInfo_idPropInfoList() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.idPropInfoList);
        assertFalse(beanInfo.idPropInfoList.isEmpty());
    }

    @Test
    public void testGetBeanInfo_annotations() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.annotations);
        assertTrue(beanInfo.annotations.containsKey(Table.class));
    }

    @Test
    public void testGetBeanInfo_readOnlyIdPropNameList() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.readOnlyIdPropNameList);
    }

    @Test
    public void testGetBeanInfo_readOnlyIdPropInfoList() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.readOnlyIdPropInfoList);
    }

    @Test
    public void testGetBeanInfo_subEntityPropNameList() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.subEntityPropNameList);
    }

    @Test
    public void testGetBeanInfo_subEntityPropInfoList() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.subEntityPropInfoList);
    }

    // =====================================================================
    // getBeanInfo(Class)
    // =====================================================================

    @Test
    public void testGetBeanInfo_classVersion() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo);
        assertEquals(TestBean.class, beanInfo.clazz);
    }

    @Test
    public void testGetBeanInfo_classVersion_TestBean2() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean2.class);
        assertNotNull(beanInfo);
        assertEquals(TestBean2.class, beanInfo.clazz);
        assertEquals("TestBean2", beanInfo.simpleClassName);
    }

    @Test
    public void testGetBeanInfo_recordType() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        assertNotNull(beanInfo);
        assertEquals(RecordA.class, beanInfo.clazz);
        assertTrue(beanInfo.isImmutable);
    }

    @Test
    public void testGetBeanInfo_immutableEntity() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(ImmutableEntity7.class);
        assertNotNull(beanInfo);
        assertTrue(beanInfo.isImmutable);
    }

    // =====================================================================
    // getBeanInfo(Type, boolean) - internal/deprecated
    // =====================================================================

    @Test
    public void testGetBeanInfo_withAsmFlag() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class, false);
        assertNotNull(beanInfo);
        assertEquals(TestBean.class, beanInfo.clazz);
    }

    @Test
    public void testGetBeanInfo_withAsmFlagTrue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class, true);
        assertNotNull(beanInfo);
        assertEquals(TestBean.class, beanInfo.clazz);
    }

    // =====================================================================
    // BeanInfo.getPropInfo(String)
    // =====================================================================

    @Test
    public void testBeanInfoGetPropInfo() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        PropInfo propInfo = beanInfo.getPropInfo("name");
        assertNotNull(propInfo);
        assertEquals("name", propInfo.name);

        PropInfo idPropInfo = beanInfo.getPropInfo("id");
        assertNotNull(idPropInfo);
        assertTrue(idPropInfo.isMarkedAsId);

        PropInfo agePropInfo = beanInfo.getPropInfo("age");
        assertNotNull(agePropInfo);
        assertArrayEquals("customAge".toCharArray(), agePropInfo.jsonNameTags[0].name);

        PropInfo notExist = beanInfo.getPropInfo("notExist");
        assertNull(notExist);
    }

    @Test
    public void testBeanInfoGetPropInfo_byColumnName() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name_column");
        assertNotNull(propInfo);
        assertEquals("name", propInfo.name);
    }

    @Test
    public void testBeanInfoGetPropInfo_caseInsensitiveColumnName() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfoUpper = beanInfo.getPropInfo("NAME_COLUMN");
        assertNotNull(propInfoUpper);
        assertEquals("name", propInfoUpper.name);
    }

    @Test
    public void testBeanInfoGetPropInfo_repeatedLookupCached() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo p1 = beanInfo.getPropInfo("name");
        PropInfo p2 = beanInfo.getPropInfo("name");
        assertSame(p1, p2);
    }

    @Test
    public void testBeanInfoGetPropInfo_allProperties() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        assertNotNull(beanInfo.getPropInfo("id"));
        assertNotNull(beanInfo.getPropInfo("name"));
        assertNotNull(beanInfo.getPropInfo("age"));
        assertNotNull(beanInfo.getPropInfo("createdDate"));
    }

    // =====================================================================
    // BeanInfo.getPropInfo(PropInfo)
    // =====================================================================

    @Test
    public void testBeanInfoGetPropInfoFromOtherBean() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);

        PropInfo namePropFromBean1 = beanInfo1.getPropInfo("name");
        PropInfo matchedProp = beanInfo2.getPropInfo(namePropFromBean1);
        assertNotNull(matchedProp);
        assertEquals("name", matchedProp.name);
    }

    @Test
    public void testBeanInfoGetPropInfoFromOtherBean_noMatch() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);

        PropInfo agePropFromBean1 = beanInfo1.getPropInfo("age");
        PropInfo matchedProp = beanInfo2.getPropInfo(agePropFromBean1);
        // "age" does not exist in TestBean2
        assertNull(matchedProp);
    }

    @Test
    public void testBeanInfoGetPropInfoFromOtherBean_sameBean() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");
        PropInfo matched = beanInfo.getPropInfo(propInfo);
        assertNotNull(matched);
        assertEquals("name", matched.name);
    }

    @Test
    public void testBeanInfoGetPropValue_nullObjectField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        Date date = beanInfo.getPropValue(bean, "createdDate");
        assertNull(date);
    }

    // =====================================================================
    // BeanInfo.getPropInfoChain(String)
    // =====================================================================

    @Test
    public void testBeanInfoGetPropInfoQueue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        var queue = beanInfo.getPropInfoChain("name");
        assertEquals(0, queue.size());

        var emptyQueue = beanInfo.getPropInfoChain("nested.property");
        assertTrue(emptyQueue.isEmpty());
    }

    @Test
    public void testBeanInfoGetPropInfoChain_singleProp() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        // Single property path returns empty list (no chain needed, it's a direct prop)
        List<PropInfo> chain = beanInfo.getPropInfoChain("id");
        assertTrue(chain.isEmpty());
    }

    @Test
    public void testBeanInfoGetPropInfoChain_cached() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        List<PropInfo> chain1 = beanInfo.getPropInfoChain("name");
        List<PropInfo> chain2 = beanInfo.getPropInfoChain("name");
        assertSame(chain1, chain2);
    }

    // =====================================================================
    // BeanInfo.readPropInfo(char[], int, int)
    // =====================================================================

    @Test
    public void testBeanInfoReadPropInfo() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        char[] nameChars = "name".toCharArray();
        PropInfo propInfo = beanInfo.readPropInfo(nameChars, 0, nameChars.length);
        assertNotNull(propInfo);
        assertEquals("name", propInfo.name);

        char[] notExistChars = "notExist".toCharArray();
        PropInfo nullPropInfo = beanInfo.readPropInfo(notExistChars, 0, notExistChars.length);
        assertNull(nullPropInfo);
    }

    @Test
    public void testBeanInfoReadPropInfo_partialArray() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        // Read "name" from a larger buffer
        char[] buffer = "prefix_name_suffix".toCharArray();
        PropInfo propInfo = beanInfo.readPropInfo(buffer, 7, 11);
        assertNotNull(propInfo);
        assertEquals("name", propInfo.name);
    }

    @Test
    public void testBeanInfoReadPropInfo_emptyRange() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.readPropInfo("test".toCharArray(), 0, 0);
        assertNull(propInfo);
    }

    @Test
    public void testBeanInfoReadPropInfo_idField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        char[] idChars = "id".toCharArray();
        PropInfo propInfo = beanInfo.readPropInfo(idChars, 0, idChars.length);
        assertNotNull(propInfo);
        assertEquals("id", propInfo.name);
    }

    @Test
    public void testBeanInfoReadPropInfo_ageField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        char[] ageChars = "age".toCharArray();
        PropInfo propInfo = beanInfo.readPropInfo(ageChars, 0, ageChars.length);
        assertNotNull(propInfo);
        assertEquals("age", propInfo.name);
    }

    // =====================================================================
    // BeanInfo.getAnnotation
    // =====================================================================

    @Test
    public void testBeanInfoGetAnnotation() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        Table tableAnnotation = beanInfo.getAnnotation(Table.class);
        assertNotNull(tableAnnotation);
        assertEquals("test_bean", tableAnnotation.value());

        Deprecated deprecatedAnnotation = beanInfo.getAnnotation(Deprecated.class);
        assertNull(deprecatedAnnotation);
    }

    @Test
    public void testBeanInfoGetAnnotation_noAnnotation() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        Table tableAnnotation = beanInfo.getAnnotation(Table.class);
        assertNull(tableAnnotation);
    }

    // =====================================================================
    // BeanInfo.newInstance
    // =====================================================================

    @Test
    public void testBeanInfoNewInstance() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        TestBean bean = beanInfo.newInstance();
        assertNotNull(bean);

        TestBean beanWithArgs = beanInfo.newInstance();
        assertNotNull(beanWithArgs);
    }

    @Test
    public void testBeanInfoNewInstance_defaultValues() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = beanInfo.newInstance();
        assertEquals(0L, bean.getId());
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getCreatedDate());
    }

    @Test
    public void testBeanInfoNewInstance_withArgs() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        RecordA record = beanInfo.newInstance(42, "John", "Doe");
        assertNotNull(record);
        assertEquals(42, record.id());
        assertEquals("John", record.firstName());
        assertEquals("Doe", record.lastName());
    }

    @Test
    public void testBeanInfoNewInstance_withEmptyArgs() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = beanInfo.newInstance(new Object[0]);
        assertNotNull(bean);
    }

    // =====================================================================
    // BeanInfo.createBeanResult / finishBeanResult
    // =====================================================================

    @Test
    public void testBeanInfoCreateAndFinishBeanResult() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        Object result = beanInfo.createBeanResult();
        assertNotNull(result);

        TestBean bean = beanInfo.finishBeanResult(result);
        assertNotNull(bean);

        TestBean nullBean = beanInfo.finishBeanResult(null);
        assertNull(nullBean);
    }

    @Test
    public void testBeanInfoCreateBeanResult_mutableBean() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        Object result = beanInfo.createBeanResult();
        assertNotNull(result);
        assertTrue(result instanceof TestBean);
    }

    @Test
    public void testBeanInfoCreateBeanResult_immutableRecord() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        Object result = beanInfo.createBeanResult();
        assertNotNull(result);
        // For immutable beans without builder, returns an Object array
        assertTrue(result instanceof Object[]);
    }

    @Test
    public void testBeanInfoFinishBeanResult_immutableRecord() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        Object result = beanInfo.createBeanResult();
        // Set values in the args array
        Object[] args = (Object[]) result;
        args[0] = 1;
        args[1] = "First";
        args[2] = "Last";
        RecordA record = beanInfo.finishBeanResult(result);
        assertNotNull(record);
        assertEquals(1, record.id());
        assertEquals("First", record.firstName());
        assertEquals("Last", record.lastName());
    }

    @Test
    public void testBeanInfoEquals_withNonBeanInfo() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        assertFalse(beanInfo.equals(null));
        assertFalse(beanInfo.equals("string"));
    }

    // =====================================================================
    // BeanInfo.toString
    // =====================================================================

    @Test
    public void testBeanInfoToString() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        String str = beanInfo.toString();
        assertNotNull(str);
        assertTrue(str.contains("TestBean"));
    }

    @Test
    public void testPropInfoGetPropValue_nullValue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        TestBean bean = new TestBean();

        String value = propInfo.getPropValue(bean);
        assertNull(value);
    }

    @Test
    public void testPropInfoSetPropValue_nullForObjectType() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        TestBean bean = new TestBean();
        bean.setName("Existing");
        propInfo.setPropValue(bean, null);

        assertNull(bean.getName());
    }

    // =====================================================================
    // PropInfo.getAnnotation
    // =====================================================================

    @Test
    public void testPropInfoGetAnnotation() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        Column columnAnnotation = propInfo.getAnnotation(Column.class);
        assertNotNull(columnAnnotation);
        assertEquals("name_column", columnAnnotation.value());

        Id idAnnotation = propInfo.getAnnotation(Id.class);
        assertNull(idAnnotation);
    }

    @Test
    public void testPropInfoGetAnnotation_idField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");
        Id idAnnotation = propInfo.getAnnotation(Id.class);
        assertNotNull(idAnnotation);
    }

    @Test
    public void testPropInfoGetAnnotation_jsonXmlField() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");
        JsonXmlField jsonXmlField = propInfo.getAnnotation(JsonXmlField.class);
        assertNotNull(jsonXmlField);
        assertEquals("customAge", jsonXmlField.name());
    }

    // =====================================================================
    // PropInfo.hashCode / equals / toString
    // =====================================================================

    @Test
    public void testPropInfoHashCodeAndEquals() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo1 = beanInfo.getPropInfo("name");
        PropInfo propInfo2 = beanInfo.getPropInfo("name");
        PropInfo propInfo3 = beanInfo.getPropInfo("age");

        assertEquals(propInfo1.hashCode(), propInfo2.hashCode());
        assertNotEquals(propInfo1.hashCode(), propInfo3.hashCode());

        assertTrue(propInfo1.equals(propInfo1));
        assertTrue(propInfo1.equals(propInfo2));
        assertFalse(propInfo1.equals(propInfo3));
        assertFalse(propInfo1.equals(null));
        assertFalse(propInfo1.equals("string"));
    }

    @Test
    public void testPropInfoEquals_crossBeanSameName() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);
        PropInfo p1 = beanInfo1.getPropInfo("name");
        PropInfo p2 = beanInfo2.getPropInfo("name");
        // Same name but different fields
        assertFalse(p1.equals(p2));
    }

    @Test
    public void testPropInfoToString() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        String str = propInfo.toString();
        assertNotNull(str);
        assertEquals("name", str);
    }

    // =====================================================================
    // PropInfo public fields
    // =====================================================================

    @Test
    public void testPropInfoPublicFields() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertNotNull(propInfo.declaringClass);
        assertEquals("name", propInfo.name);
        assertNotNull(propInfo.type);
        assertNotNull(propInfo.jsonXmlType);
        assertNotNull(propInfo.dbType);
        assertNotNull(propInfo.clazz);
        assertNotNull(propInfo.annotations);
    }

    @Test
    public void testPropInfoPublicFields_id() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        assertTrue(propInfo.isMarkedAsId);
        assertNotNull(propInfo.field);
    }

    @Test
    public void testPropInfoPublicFields_jsonXmlExpose() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertNotNull(propInfo.jsonXmlExpose);
    }

    @Test
    public void testPropInfoPublicFields_getMethod() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertNotNull(propInfo.getMethod);
        assertEquals("getName", propInfo.getMethod.getName());
    }

    @Test
    public void testPropInfoPublicFields_setMethod() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertNotNull(propInfo.setMethod);
        assertEquals("setName", propInfo.setMethod.getName());
    }

    @Test
    public void testPropInfoPublicFields_aliases() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertNotNull(propInfo.aliases);
    }

    @Test
    public void testPropInfoPublicFields_tablePrefix() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertNotNull(propInfo.tablePrefix);
    }

    @Test
    public void testPropInfoPublicFields_field() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");
        assertNotNull(propInfo.field);
        assertEquals("name", propInfo.field.getName());
    }

    @Test
    public void testPropInfoPublicFields_getAndSetMethodConsistency() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        for (PropInfo propInfo : beanInfo.propInfoList) {
            // Each property should have a getter
            assertNotNull(propInfo.getMethod, "Getter should exist for " + propInfo.name);
        }
    }

    @Test
    public void test_setGetNullBeanProperty() {
        final Account account = Beans.newRandom(Account.class);
        N.println(account);
        account.setContact(null);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);

        N.println(beanInfo.getPropValue(account, "contact.email"));
        assertEquals((String) null, beanInfo.getPropValue(account, "contact.email"));
        N.println(beanInfo.getPropValue(account, "contact.status"));
        assertEquals(Integer.valueOf(0), beanInfo.getPropValue(account, "contact.status"));

        N.println(account.getContact());
        beanInfo.setPropValue(account, "contact.email", "test@email.com");
        N.println(account.getContact());
        assertEquals("test@email.com", account.getContact().getEmail());

        beanInfo.setPropValue(account, "contact.status", "2");
        N.println(account.getContact());
        assertEquals(2, account.getContact().getStatus());
    }

    @Test
    public void test_get_set_prop_value() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class, false);
        TestBean bean = new TestBean();

        beanInfo.setPropValue(bean, "name", "Alice");
        beanInfo.setPropValue(bean, "age", 28);
        beanInfo.setPropValue(bean, "createdDate", new Date());

        String name = (String) beanInfo.getPropValue(bean, "name");
        int age = (int) beanInfo.getPropValue(bean, "age");
        Date createdDate = (Date) beanInfo.getPropValue(bean, "createdDate");

        assertEquals("Alice", name);
        assertEquals(28, age);
        assertNotNull(createdDate);
    }

    @Test
    public void testGetBeanInfoInvalidClass() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParserUtil.getBeanInfo(EmptyClass.class);
        });
    }

    @Test
    public void testGetBeanInfoThrowsExceptionForNonBeanClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserUtil.getBeanInfo(String.class);
        });
    }

    @Test
    public void testBeanInfoGetPropValue_nonExistentProp() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();

        assertThrows(RuntimeException.class, () -> {
            beanInfo.getPropValue(bean, "nonExistentProperty");
        });
    }

    @Test
    public void testBeanInfoSetPropValue_nonExistent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        assertThrows(IllegalArgumentException.class, () -> {
            beanInfo.setPropValue(bean, "nonExistent", "value");
        });
    }

    // =====================================================================
    // BeanInfo.setPropValue(Object, String, Object, boolean)
    // =====================================================================

    @Test
    public void testBeanInfoSetPropValueWithIgnoreUnmatchedProperty() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();

        boolean result = beanInfo.setPropValue(bean, "nonExistent", "value", true);
        assertFalse(result);

        assertThrows(IllegalArgumentException.class, () -> {
            beanInfo.setPropValue(bean, "nonExistent", "value", false);
        });
    }

    @Test
    public void testBeanInfoSetPropValueByPropInfo_unmatchedThrows() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);

        PropInfo agePropFromBean1 = beanInfo1.getPropInfo("age");
        assertThrows(RuntimeException.class, () -> {
            beanInfo2.setPropValue(new TestBean2(), agePropFromBean1, 99, false);
        });
    }

    @Test
    public void testBeanInfoGetPropInfoChain_invalidNested() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        List<PropInfo> chain = beanInfo.getPropInfoChain("nonExistent.property");
        assertTrue(chain.isEmpty());
    }

    // =====================================================================
    // PropInfo.readPropValue(String)
    // =====================================================================

    @Test
    public void testPropInfoReadPropValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");

        Object value = propInfo.readPropValue("25");
        assertEquals(25, value);
    }

    @Test
    public void testPropInfoReadPropValue_stringType() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        Object value = propInfo.readPropValue("hello");
        assertEquals("hello", value);
    }

    @Test
    public void testPropInfoReadPropValue_longType() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        Object value = propInfo.readPropValue("12345");
        assertEquals(12345L, value);
    }

    @Test
    public void testPropInfoReadPropValue_zeroValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");

        Object value = propInfo.readPropValue("0");
        assertEquals(0, value);
    }

    @Test
    public void testPropInfoReadPropValue_negativeValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");

        Object value = propInfo.readPropValue("-5");
        assertEquals(-5, value);
    }

    @Test
    public void testPropInfoReadPropValue_emptyStringForString() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        Object value = propInfo.readPropValue("");
        assertEquals("", value);
    }

    // =====================================================================
    // PropInfo.writePropValue(CharacterWriter, Object, JsonXmlSerConfig)
    // =====================================================================

    @Test
    public void testPropInfoWritePropValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        com.landawn.abacus.util.CharacterWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        JsonXmlSerConfig<?> config = JsonSerConfig.create();
        propInfo.writePropValue(writer, "hello", config);
        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("hello"));
        com.landawn.abacus.util.Objectory.recycle(writer);
    }

    @Test
    public void testPropInfoWritePropValue_nullValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        com.landawn.abacus.util.CharacterWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        JsonXmlSerConfig<?> config = JsonSerConfig.create();
        propInfo.writePropValue(writer, null, config);
        String output = writer.toString();
        assertNotNull(output);
        assertEquals("null", output);
        com.landawn.abacus.util.Objectory.recycle(writer);
    }

    @Test
    public void testPropInfoWritePropValue_intValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");

        com.landawn.abacus.util.CharacterWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        JsonXmlSerConfig<?> config = JsonSerConfig.create();
        propInfo.writePropValue(writer, 42, config);
        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("42"));
        com.landawn.abacus.util.Objectory.recycle(writer);
    }

    @Test
    public void testPropInfoWritePropValue_longValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("id");

        com.landawn.abacus.util.CharacterWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        JsonXmlSerConfig<?> config = JsonSerConfig.create();
        propInfo.writePropValue(writer, 99999L, config);
        String output = writer.toString();
        assertNotNull(output);
        assertTrue(output.contains("99999"));
        com.landawn.abacus.util.Objectory.recycle(writer);
    }

    @Test
    public void testRefreshBeanPropInfo_preservesProperties() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        int propCount = beanInfo1.propInfoList.size();
        ParserUtil.refreshBeanPropInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);
        assertEquals(propCount, beanInfo2.propInfoList.size());
    }

    // =====================================================================
    // refreshBeanPropInfo
    // =====================================================================

    @Test
    public void testRefreshBeanPropInfo() {
        ParserUtil.BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);

        ParserUtil.refreshBeanPropInfo(TestBean.class);

        ParserUtil.BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);

        Assertions.assertNotSame(beanInfo1, beanInfo2);
    }

    @Test
    public void testRefreshBeanPropInfo_recreatesInfo() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean2.class);
        ParserUtil.refreshBeanPropInfo(TestBean2.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean2.class);
        assertNotSame(beanInfo1, beanInfo2);
        assertEquals(beanInfo1.clazz, beanInfo2.clazz);
    }

    @Test
    public void testRefreshBeanPropInfo_multipleRefreshes() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        ParserUtil.refreshBeanPropInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);
        ParserUtil.refreshBeanPropInfo(TestBean.class);
        BeanInfo beanInfo3 = ParserUtil.getBeanInfo(TestBean.class);
        assertNotSame(beanInfo1, beanInfo2);
        assertNotSame(beanInfo2, beanInfo3);
        assertNotSame(beanInfo1, beanInfo3);
    }

    @Test
    public void test_underscorePropName() {
        assertEquals("a", Beans.toSnakeCase("A"));
        assertEquals("a", Beans.toSnakeCase("a"));
        assertEquals("name", Beans.toSnakeCase("name"));
        assertEquals("name", Beans.toSnakeCase("Name"));
        assertEquals("name", Beans.toSnakeCase("NAME"));
        assertEquals("first_name", Beans.toSnakeCase("firstName"));
        assertEquals("first_name", Beans.toSnakeCase("FIRSTName"));
        assertEquals("gui", Beans.toSnakeCase("GUI"));
    }

    @Test
    public void test_screamingSnakeCase() {
        assertEquals("A", Beans.toScreamingSnakeCase("A"));
        assertEquals("A", Beans.toScreamingSnakeCase("a"));
        assertEquals("NAME", Beans.toScreamingSnakeCase("name"));
        assertEquals("NAME", Beans.toScreamingSnakeCase("Name"));
        assertEquals("NAME", Beans.toScreamingSnakeCase("NAME"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("firstName"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("FIRSTName"));
        assertEquals("GUI", Beans.toScreamingSnakeCase("GUI"));
    }

    @Test
    public void test_toScreamingSnakeCase() {
        final Account account = createAccount(Account.class);

        Map<String, Object> props = Beans.beanToMap(account);

        Beans.replaceKeysWithSnakeCase(props);

        N.println(props);

        props = Beans.beanToMap(account);

        Beans.replaceKeysWithScreamingSnakeCase(props);

        N.println(props);
        assertNotNull(props);
    }

    @Test
    public void test_ImmutableBuilderEntity() {
        {
            final ImmutableBuilderEntity bean = ImmutableBuilderEntity.builder().id(111).firstName("fn").lastName("ln").build();

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableBuilderEntity bean2 = N.fromJson(json, ImmutableBuilderEntity.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableBuilderEntity2 bean = ImmutableBuilderEntity2.builder().id(111).firstName("fn").lastName("ln").build();

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableBuilderEntity2 bean2 = N.fromJson(json, ImmutableBuilderEntity2.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableBuilderEntity3 bean = ImmutableBuilderEntity3.builder().id(111).firstName("fn").lastName("ln").build();

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableBuilderEntity3 bean2 = N.fromJson(json, ImmutableBuilderEntity3.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity4 bean = new ImmutableEntity4().id(111).firstName("fn").lastName("ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity4 bean2 = N.fromJson(json, ImmutableEntity4.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity5 bean = new ImmutableEntity5(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity5 bean2 = N.fromJson(json, ImmutableEntity5.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity6 bean = new ImmutableEntity6(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity6 bean2 = N.fromJson(json, ImmutableEntity6.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity7 bean = new ImmutableEntity7(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity7 bean2 = N.fromJson(json, ImmutableEntity7.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity8 bean = new ImmutableEntity8(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity8 bean2 = N.fromJson(json, ImmutableEntity8.class);

            assertEquals(bean, bean2);
        }

        {
            final ImmutableEntity9 bean = new ImmutableEntity9(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final ImmutableEntity9 bean2 = N.fromJson(json, ImmutableEntity9.class);

            assertEquals(bean, bean2);
        }

        {
            final RecordA bean = new RecordA(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final RecordA bean2 = N.fromJson(json, RecordA.class);

            assertEquals(bean, bean2);
        }

        {
            final RecordB bean = new RecordB(111, "fn", "ln");

            N.println(bean);
            final String json = N.toJson(bean);

            N.println(json);
            final RecordB bean2 = N.fromJson(json, RecordB.class);

            assertEquals(bean, bean2);
        }

    }

    // TODO: Remaining ParserUtil.ASMPropInfo gaps are ReflectASM-specific generated access paths that are not meaningfully isolated beyond public ParserUtil/PropInfo coverage.

}
