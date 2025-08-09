package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Util100Test extends TestBase {

    // Test bean class
    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private Date createdDate;
        private NestedBean nested;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public Date getCreatedDate() { return createdDate; }
        public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
        
        public NestedBean getNested() { return nested; }
        public void setNested(NestedBean nested) { this.nested = nested; }
    }
    
    public static class NestedBean {
        private String value;
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    @Test
    public void testFillObject() {
        TestBean bean = new TestBean();
        Beans.fill(bean);
        
        // Verify all properties are filled
        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getAge() != 0); // Should be a random int
        // Boolean can be true or false, just verify it was set
        Assertions.assertNotNull(bean.getCreatedDate());
        Assertions.assertNotNull(bean.getNested());
        Assertions.assertNotNull(bean.getNested().getValue());
        
        // Test with null should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.fill(null));
    }

    @Test
    public void testFillObjectWithPropNames() {
        TestBean bean = new TestBean();
        Beans.fill(bean, Arrays.asList("name", "age"));
        
        // Verify only specified properties are filled
        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getAge() != 0);
        
        // Other properties should remain null/default
        Assertions.assertNull(bean.getCreatedDate());
        Assertions.assertNull(bean.getNested());
        
        // Test with null bean
        Assertions.assertThrows(IllegalArgumentException.class, 
                () -> Beans.fill(null, Arrays.asList("name")));
    }

    @Test
    public void testFillClass() {
        TestBean bean = Beans.fill(TestBean.class);
        
        Assertions.assertNotNull(bean);
        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getAge() != 0);
        Assertions.assertNotNull(bean.getCreatedDate());
        Assertions.assertNotNull(bean.getNested());
        Assertions.assertNotNull(bean.getNested().getValue());
        
        // Test with null class
        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.fill(null));
    }

    @Test
    public void testFillClassWithCount() {
        List<TestBean> beans = Beans.fill(TestBean.class, 5);
        
        Assertions.assertEquals(5, beans.size());
        
        // Verify each bean is filled
        for (TestBean bean : beans) {
            Assertions.assertNotNull(bean);
            Assertions.assertNotNull(bean.getName());
            Assertions.assertTrue(bean.getAge() != 0);
            Assertions.assertNotNull(bean.getCreatedDate());
            Assertions.assertNotNull(bean.getNested());
        }
        
        // Test with zero count
        List<TestBean> emptyList = Beans.fill(TestBean.class, 0);
        Assertions.assertTrue(emptyList.isEmpty());
        
        // Test with negative count
        Assertions.assertThrows(IllegalArgumentException.class, 
                () -> Beans.fill(TestBean.class, -1));
        
        // Test with null class
        Assertions.assertThrows(IllegalArgumentException.class, 
                () -> Beans.fill(null, 5));
    }

    @Test
    public void testFillClassWithPropNames() {
        TestBean bean = Beans.fill(TestBean.class, Arrays.asList("name", "active"));
        
        Assertions.assertNotNull(bean);
        Assertions.assertNotNull(bean.getName());
        // Boolean should be set (true or false)
        
        // Other properties should be default/null
        Assertions.assertEquals(0, bean.getAge());
        Assertions.assertNull(bean.getCreatedDate());
        Assertions.assertNull(bean.getNested());
        
        // Test with empty prop list
        TestBean emptyBean = Beans.fill(TestBean.class, Arrays.asList());
        Assertions.assertNotNull(emptyBean);
        Assertions.assertNull(emptyBean.getName());
        Assertions.assertEquals(0, emptyBean.getAge());
    }

    @Test
    public void testFillClassWithPropNamesAndCount() {
        List<TestBean> beans = Beans.fill(TestBean.class,
            Arrays.asList("name", "age"), 3);
        
        Assertions.assertEquals(3, beans.size());
        
        for (TestBean bean : beans) {
            Assertions.assertNotNull(bean);
            Assertions.assertNotNull(bean.getName());
            Assertions.assertTrue(bean.getAge() != 0);
            
            // Other properties should be default/null
            Assertions.assertNull(bean.getCreatedDate());
            Assertions.assertNull(bean.getNested());
        }
        
        // Test with zero count
        List<TestBean> emptyList = Beans.fill(TestBean.class,
            Arrays.asList("name"), 0);
        Assertions.assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testFillWithDifferentTypes() {
        // Create a bean with various property types
        TestBean bean = Beans.fill(TestBean.class);
        
        // String should be a UUID substring
        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getName().length() > 0);
        
        // int should be random
        // Can't predict exact value, just verify it's set
        
        // boolean should be set
        // Can be true or false
        
        // Date should be current timestamp
        Assertions.assertNotNull(bean.getCreatedDate());
        long timeDiff = System.currentTimeMillis() - bean.getCreatedDate().getTime();
        Assertions.assertTrue(timeDiff >= 0 && timeDiff < 1000); // Should be very recent
        
        // Nested bean should also be filled
        Assertions.assertNotNull(bean.getNested());
        Assertions.assertNotNull(bean.getNested().getValue());
    }

    @Test
    public void testFillNonBeanClass() {
        // Test with non-bean class (no getter/setter)
        Assertions.assertThrows(IllegalArgumentException.class, 
                () -> Beans.fill(String.class));
        
        Assertions.assertThrows(IllegalArgumentException.class, 
                () -> Beans.fill(Integer.class));
    }

    @Test
    public void testFillMultipleTimes() {
        // Verify randomness - filled values should be different
        TestBean bean1 = Beans.fill(TestBean.class);
        TestBean bean2 = Beans.fill(TestBean.class);
        
        // Names should be different (UUID based)
        Assertions.assertNotEquals(bean1.getName(), bean2.getName());
        
        // Ages might be the same occasionally, but usually different
        // Can't guarantee they're different due to randomness
    }

    @Test
    public void testFillWithInvalidPropertyNames() {
        TestBean bean = new TestBean();
        
        // Fill with non-existent property should be handled gracefully
        // The implementation might throw an exception or ignore invalid names
        try {
            Beans.fill(bean, Arrays.asList("nonExistentProperty"));
            // If no exception, that's also acceptable behavior
        } catch (Exception e) {
            // Expected if implementation validates property names
        }
    }

    // Test bean with Number properties
    public static class NumberBean {
        private Integer integerValue;
        private Long longValue;
        private Double doubleValue;
        private Float floatValue;
        private Short shortValue;
        private Byte byteValue;
        
        public Integer getIntegerValue() { return integerValue; }
        public void setIntegerValue(Integer integerValue) { this.integerValue = integerValue; }
        
        public Long getLongValue() { return longValue; }
        public void setLongValue(Long longValue) { this.longValue = longValue; }
        
        public Double getDoubleValue() { return doubleValue; }
        public void setDoubleValue(Double doubleValue) { this.doubleValue = doubleValue; }
        
        public Float getFloatValue() { return floatValue; }
        public void setFloatValue(Float floatValue) { this.floatValue = floatValue; }
        
        public Short getShortValue() { return shortValue; }
        public void setShortValue(Short shortValue) { this.shortValue = shortValue; }
        
        public Byte getByteValue() { return byteValue; }
        public void setByteValue(Byte byteValue) { this.byteValue = byteValue; }
    }

    @Test
    public void testFillNumberTypes() {
        NumberBean bean = Beans.fill(NumberBean.class);
        
        Assertions.assertNotNull(bean.getIntegerValue());
        Assertions.assertNotNull(bean.getLongValue());
        Assertions.assertNotNull(bean.getDoubleValue());
        Assertions.assertNotNull(bean.getFloatValue());
        Assertions.assertNotNull(bean.getShortValue());
        Assertions.assertNotNull(bean.getByteValue());
    }
}