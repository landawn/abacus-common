package com.landawn.abacus.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;

@Tag("new-test")
public class ParserUtil100Test extends TestBase {

    @Table("test_bean")
    public static class TestBean {
        @Id
        private Long id;

        @Column("name_column")
        private String name;

        @Transient
        private String transientField;

        @JsonXmlField(name = "customAge")
        private int age;

        private Date createdDate;

        @ReadOnlyId
        private String readOnlyId;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTransientField() {
            return transientField;
        }

        public void setTransientField(String transientField) {
            this.transientField = transientField;
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

        public String getReadOnlyId() {
            return readOnlyId;
        }

        public void setReadOnlyId(String readOnlyId) {
            this.readOnlyId = readOnlyId;
        }
    }

    public static class EmptyBean {
    }

    public static class SimpleBean {
        private String property;

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    @Test
    public void testGetBeanInfo() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        assertNotNull(beanInfo);
        assertEquals(TestBean.class, beanInfo.clazz);
        assertEquals("TestBean", beanInfo.simpleClassName);
        assertTrue(beanInfo.propNameList.size() > 0);
        assertTrue(beanInfo.idPropNameList.contains("id"));
        assertTrue(beanInfo.readOnlyIdPropNameList.contains("readOnlyId"));
        assertTrue(beanInfo.tableName.isPresent());
        assertEquals("test_bean", beanInfo.tableName.get());
    }

    @Test
    public void testGetBeanInfoThrowsExceptionForNonBeanClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserUtil.getBeanInfo(String.class);
        });
    }

    @Test
    public void testRefreshBeanPropInfo() {
        ParserUtil.refreshBeanPropInfo(TestBean.class);
    }

    @Test
    public void testBeanInfoGetPropInfo() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        PropInfo propInfo = beanInfo.getPropInfo("name");
        assertNotNull(propInfo);
        assertEquals("name", propInfo.name);

        PropInfo idPropInfo = beanInfo.getPropInfo("id");
        assertNotNull(idPropInfo);
        assertTrue(idPropInfo.isMarkedToId);

        PropInfo agePropInfo = beanInfo.getPropInfo("age");
        assertNotNull(agePropInfo);
        assertArrayEquals("customAge".toCharArray(), agePropInfo.jsonNameTags[0].name);

        PropInfo notExist = beanInfo.getPropInfo("notExist");
        assertNull(notExist);
    }

    @Test
    public void testBeanInfoGetPropInfoByPropInfo() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(SimpleBean.class);

        PropInfo propInfo1 = beanInfo1.getPropInfo("name");
        PropInfo propInfo2 = beanInfo2.getPropInfo(propInfo1);

        assertNull(propInfo2);
    }

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
    public void testBeanInfoSetPropValue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();

        beanInfo.setPropValue(bean, "name", "Jane");
        assertEquals("Jane", bean.getName());

        beanInfo.setPropValue(bean, "age", 25);
        assertEquals(25, bean.getAge());
    }

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
    public void testBeanInfoSetPropValueByPropInfo() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        TestBean bean = new TestBean();
        PropInfo propInfo = beanInfo.getPropInfo("name");

        beanInfo.setPropValue(bean, propInfo, "TestName");
        assertEquals("TestName", bean.getName());
    }

    @Test
    public void testBeanInfoGetPropInfoQueue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        var queue = beanInfo.getPropInfoQueue("name");
        assertEquals(0, queue.size());

        var emptyQueue = beanInfo.getPropInfoQueue("nested.property");
        assertTrue(emptyQueue.isEmpty());
    }

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
    public void testBeanInfoIsAnnotationPresent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        assertTrue(beanInfo.isAnnotationPresent(Table.class));
        assertFalse(beanInfo.isAnnotationPresent(Deprecated.class));
    }

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
    public void testBeanInfoNewInstance() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);

        TestBean bean = beanInfo.newInstance();
        assertNotNull(bean);

        TestBean beanWithArgs = beanInfo.newInstance();
        assertNotNull(beanWithArgs);
    }

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
    public void testBeanInfoHashCodeAndEquals() {
        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);
        BeanInfo beanInfo3 = ParserUtil.getBeanInfo(SimpleBean.class);

        assertEquals(beanInfo1.hashCode(), beanInfo2.hashCode());
        assertNotEquals(beanInfo1.hashCode(), beanInfo3.hashCode());

        assertTrue(beanInfo1.equals(beanInfo1));
        assertTrue(beanInfo1.equals(beanInfo2));
        assertFalse(beanInfo1.equals(beanInfo3));
        assertFalse(beanInfo1.equals(null));
        assertFalse(beanInfo1.equals("string"));
    }

    @Test
    public void testBeanInfoToString() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        String str = beanInfo.toString();
        assertNotNull(str);
        assertTrue(str.contains("TestBean"));
    }

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
    public void testPropInfoSetPropValue() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        TestBean bean = new TestBean();
        propInfo.setPropValue(bean, "NewValue");

        assertEquals("NewValue", bean.getName());
    }

    @Test
    public void testPropInfoReadPropValue() throws Exception {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("age");

        Object value = propInfo.readPropValue("25");
        assertEquals(25, value);
    }

    @Test
    public void testPropInfoIsAnnotationPresent() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        assertTrue(propInfo.isAnnotationPresent(Column.class));
        assertFalse(propInfo.isAnnotationPresent(Id.class));
    }

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
    public void testPropInfoToString() {
        BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        PropInfo propInfo = beanInfo.getPropInfo("name");

        String str = propInfo.toString();
        assertNotNull(str);
        assertEquals("name", str);
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
}
