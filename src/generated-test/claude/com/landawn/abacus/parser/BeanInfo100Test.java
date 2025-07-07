package com.landawn.abacus.parser;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.util.NamingPolicy;

public class BeanInfo100Test extends TestBase {

    private ParserUtil.BeanInfo beanInfo;
    private TestBean testBean;

    @BeforeEach
    public void setup() {
        beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        testBean = new TestBean();
    }

    @Test
    public void testBeanInfoConstructor() {
        // Verify basic properties
        Assertions.assertEquals(TestBean.class, beanInfo.clazz);
        Assertions.assertEquals("TestBean", beanInfo.simpleClassName);
        Assertions.assertEquals("com.landawn.abacus.parser.BeanInfo100Test.TestBean", beanInfo.canonicalClassName);
        Assertions.assertNotNull(beanInfo.type);
        Assertions.assertNotNull(beanInfo.propNameList);
        Assertions.assertNotNull(beanInfo.propInfoList);
    }

    @Test
    public void testGetPropInfoByName() {
        // Test getting property info by name
        ParserUtil.PropInfo propInfo = beanInfo.getPropInfo("id");
        Assertions.assertNotNull(propInfo);
        Assertions.assertEquals("id", propInfo.name);
        Assertions.assertEquals(Long.class, propInfo.clazz);

        // Test with non-existent property
        ParserUtil.PropInfo nonExistent = beanInfo.getPropInfo("nonExistentProperty");
        Assertions.assertNull(nonExistent);

        // Test with aliased property
        ParserUtil.PropInfo aliasedProp = beanInfo.getPropInfo("alias1");
        Assertions.assertNotNull(aliasedProp);
        Assertions.assertEquals("aliasedField", aliasedProp.name);
    }

    @Test
    public void testGetPropInfoByPropInfo() {
        ParserUtil.BeanInfo otherBeanInfo = ParserUtil.getBeanInfo(OtherTestBean.class);
        ParserUtil.PropInfo otherPropInfo = otherBeanInfo.getPropInfo("sharedField");

        // Test getting prop info using prop info from another bean
        ParserUtil.PropInfo propInfo = beanInfo.getPropInfo(otherPropInfo);
        Assertions.assertNotNull(propInfo);
        Assertions.assertEquals("sharedField", propInfo.name);
    }

    @Test
    public void testGetPropValue() {
        testBean.setId(123L);
        testBean.setSimpleName("TestName");

        // Test getting simple property value
        Long id = beanInfo.getPropValue(testBean, "id");
        Assertions.assertEquals(123L, id);

        String name = beanInfo.getPropValue(testBean, "simpleName");
        Assertions.assertEquals("TestName", name);
    }

    @Test
    public void testGetPropValueNested() {
        testBean.setNestedBean(new NestedBean());
        testBean.getNestedBean().setNestedValue("NestedValue");

        // Test getting nested property value
        String nestedValue = beanInfo.getPropValue(testBean, "nestedBean.nestedValue");
        Assertions.assertEquals("NestedValue", nestedValue);

        // Test with null intermediate value
        testBean.setNestedBean(null);
        String nullNestedValue = beanInfo.getPropValue(testBean, "nestedBean.nestedValue");
        Assertions.assertNull(nullNestedValue);
    }

    @Test
    public void testSetPropValue() {
        // Test setting simple property value
        beanInfo.setPropValue(testBean, "id", 456L);
        Assertions.assertEquals(456L, testBean.getId());

        beanInfo.setPropValue(testBean, "simpleName", "UpdatedName");
        Assertions.assertEquals("UpdatedName", testBean.getSimpleName());
    }

    @Test
    public void testSetPropValueWithIgnoreUnmatched() {
        // Test setting existing property
        boolean result = beanInfo.setPropValue(testBean, "id", 789L, false);
        Assertions.assertTrue(result);
        Assertions.assertEquals(789L, testBean.getId());

        // Test setting non-existent property with ignore
        boolean nonExistentResult = beanInfo.setPropValue(testBean, "nonExistent", "value", true);
        Assertions.assertFalse(nonExistentResult);

        // Test setting non-existent property without ignore
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            beanInfo.setPropValue(testBean, "nonExistent", "value", false);
        });
    }

    @Test
    public void testSetPropValueByPropInfo() {
        ParserUtil.BeanInfo otherBeanInfo = ParserUtil.getBeanInfo(OtherTestBean.class);
        ParserUtil.PropInfo otherPropInfo = otherBeanInfo.getPropInfo("sharedField");

        // Test setting value using prop info from another bean
        beanInfo.setPropValue(testBean, otherPropInfo, "SharedValue");
        Assertions.assertEquals("SharedValue", testBean.getSharedField());
    }

    @Test
    public void testGetPropInfoQueue() {
        // Test getting prop info queue for nested property
        List<ParserUtil.PropInfo> queue = beanInfo.getPropInfoQueue("nestedBean.nestedValue");
        Assertions.assertEquals(2, queue.size());
        Assertions.assertEquals("nestedBean", queue.get(0).name);
        Assertions.assertEquals("nestedValue", queue.get(1).name);

        // Test with simple property
        List<ParserUtil.PropInfo> simpleQueue = beanInfo.getPropInfoQueue("id");
        Assertions.assertEquals(0, simpleQueue.size());
    }

    @Test
    public void testReadPropInfo() {
        // Test reading prop info from character buffer
        char[] buffer = "id".toCharArray();
        ParserUtil.PropInfo propInfo = beanInfo.readPropInfo(buffer, 0, buffer.length);
        Assertions.assertNotNull(propInfo);
        Assertions.assertEquals("id", propInfo.name);

        // Test with non-existent property
        char[] nonExistentBuffer = "xyz".toCharArray();
        ParserUtil.PropInfo nonExistentProp = beanInfo.readPropInfo(nonExistentBuffer, 0, nonExistentBuffer.length);
        Assertions.assertNull(nonExistentProp);
    }

    @Test
    public void testIsAnnotationPresent() {
        // Test class-level annotations
        boolean hasJsonXmlConfig = beanInfo.isAnnotationPresent(JsonXmlConfig.class);
        Assertions.assertTrue(hasJsonXmlConfig);

        boolean hasTable = beanInfo.isAnnotationPresent(Table.class);
        Assertions.assertTrue(hasTable);

        boolean hasEntity = beanInfo.isAnnotationPresent(Entity.class);
        Assertions.assertFalse(hasEntity);
    }

    @Test
    public void testGetAnnotation() {
        // Test getting class-level annotation
        JsonXmlConfig jsonXmlConfig = beanInfo.getAnnotation(JsonXmlConfig.class);
        Assertions.assertNotNull(jsonXmlConfig);
        Assertions.assertEquals(NamingPolicy.LOWER_CAMEL_CASE, jsonXmlConfig.namingPolicy());

        Table table = beanInfo.getAnnotation(Table.class);
        Assertions.assertNotNull(table);
        Assertions.assertEquals("test_bean", table.value());

        Entity entity = beanInfo.getAnnotation(Entity.class);
        Assertions.assertNull(entity);
    }

    @Test
    public void testNewInstance() {
        // Test creating new instance with no-args constructor
        TestBean newInstance = beanInfo.newInstance();
        Assertions.assertNotNull(newInstance);
        Assertions.assertNull(newInstance.getId());

        // Test creating new instance with args
        TestBean instanceWithArgs = beanInfo.newInstance(123L, "Name", null, null, null);
        Assertions.assertNotNull(instanceWithArgs);
    }

    @Test
    public void testCreateAndFinishBeanResult() {
        // Test creating bean result for mutable bean
        Object result = beanInfo.createBeanResult();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof TestBean);

        // Test finishing bean result
        TestBean finished = beanInfo.finishBeanResult(result);
        Assertions.assertNotNull(finished);
        Assertions.assertSame(result, finished);
    }

    @Test
    public void testHashCodeAndEquals() {
        ParserUtil.BeanInfo anotherBeanInfo = ParserUtil.getBeanInfo(TestBean.class);

        // Test hashCode
        Assertions.assertEquals(beanInfo.hashCode(), anotherBeanInfo.hashCode());

        // Test equals
        Assertions.assertEquals(beanInfo, anotherBeanInfo);
        Assertions.assertEquals(beanInfo, beanInfo);
        Assertions.assertNotEquals(beanInfo, null);
        Assertions.assertNotEquals(beanInfo, new Object());

        // Test with different class
        ParserUtil.BeanInfo differentBeanInfo = ParserUtil.getBeanInfo(OtherTestBean.class);
        Assertions.assertNotEquals(beanInfo, differentBeanInfo);
    }

    @Test
    public void testToString() {
        String toString = beanInfo.toString();
        Assertions.assertEquals("com.landawn.abacus.parser.BeanInfo100Test.TestBean", toString);
    }

    // Test classes
    @JsonXmlConfig(namingPolicy = NamingPolicy.LOWER_CAMEL_CASE)
    @Table("test_bean")
    public static class TestBean {
        @Id
        private Long id;
        private String simpleName;
        @JsonXmlField(alias = { "alias1", "alias2" })
        private String aliasedField;
        private String sharedField;
        private NestedBean nestedBean;

        // Constructor for testing
        public TestBean() {
        }

        public TestBean(Long id, String simpleName, String aliasedField, String sharedField, NestedBean nestedBean) {
            this.id = id;
            this.simpleName = simpleName;
        }

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public void setSimpleName(String simpleName) {
            this.simpleName = simpleName;
        }

        public String getAliasedField() {
            return aliasedField;
        }

        public void setAliasedField(String aliasedField) {
            this.aliasedField = aliasedField;
        }

        public String getSharedField() {
            return sharedField;
        }

        public void setSharedField(String sharedField) {
            this.sharedField = sharedField;
        }

        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }
    }

    public static class NestedBean {
        private String nestedValue;

        public String getNestedValue() {
            return nestedValue;
        }

        public void setNestedValue(String nestedValue) {
            this.nestedValue = nestedValue;
        }
    }

    public static class OtherTestBean {
        private String sharedField;

        public String getSharedField() {
            return sharedField;
        }

        public void setSharedField(String sharedField) {
            this.sharedField = sharedField;
        }
    }
}
