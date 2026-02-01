package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Util100Test extends TestBase {

    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private Date createdDate;
        private NestedBean nested;

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

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }

        public NestedBean getNested() {
            return nested;
        }

        public void setNested(NestedBean nested) {
            this.nested = nested;
        }
    }

    public static class NestedBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    public void testFillObject() {
        TestBean bean = new TestBean();
        Beans.randomize(bean);

        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getAge() != 0);
        Assertions.assertNotNull(bean.getCreatedDate());
        Assertions.assertNotNull(bean.getNested());
        Assertions.assertNotNull(bean.getNested().getValue());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.newRandom(null));
    }

    @Test
    public void testFillObjectWithPropNames() {
        TestBean bean = new TestBean();
        Beans.randomize(bean, Arrays.asList("name", "age"));

        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getAge() != 0);

        Assertions.assertNull(bean.getCreatedDate());
        Assertions.assertNull(bean.getNested());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.newRandom(null, Arrays.asList("name")));
    }

    @Test
    public void testFillClass() {
        TestBean bean = Beans.newRandom(TestBean.class);

        Assertions.assertNotNull(bean);
        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getAge() != 0);
        Assertions.assertNotNull(bean.getCreatedDate());
        Assertions.assertNotNull(bean.getNested());
        Assertions.assertNotNull(bean.getNested().getValue());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.newRandom(null));
    }

    @Test
    public void testFillClassWithCount() {
        List<TestBean> beans = Beans.newRandomList(TestBean.class, 5);

        Assertions.assertEquals(5, beans.size());

        for (TestBean bean : beans) {
            Assertions.assertNotNull(bean);
            Assertions.assertNotNull(bean.getName());
            Assertions.assertTrue(bean.getAge() != 0);
            Assertions.assertNotNull(bean.getCreatedDate());
            Assertions.assertNotNull(bean.getNested());
        }

        List<TestBean> emptyList = Beans.newRandomList(TestBean.class, 0);
        Assertions.assertTrue(emptyList.isEmpty());

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.newRandomList(TestBean.class, -1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.newRandomList(null, 5));
    }

    @Test
    public void testFillClassWithPropNames() {
        TestBean bean = Beans.newRandom(TestBean.class, Arrays.asList("name", "active"));

        Assertions.assertNotNull(bean);
        Assertions.assertNotNull(bean.getName());

        Assertions.assertEquals(0, bean.getAge());
        Assertions.assertNull(bean.getCreatedDate());
        Assertions.assertNull(bean.getNested());

        TestBean emptyBean = Beans.newRandom(TestBean.class, Arrays.asList());
        Assertions.assertNotNull(emptyBean);
        Assertions.assertNull(emptyBean.getName());
        Assertions.assertEquals(0, emptyBean.getAge());
    }

    @Test
    public void testFillClassWithPropNamesAndCount() {
        List<TestBean> beans = Beans.newRandomList(TestBean.class, Arrays.asList("name", "age"), 3);

        Assertions.assertEquals(3, beans.size());

        for (TestBean bean : beans) {
            Assertions.assertNotNull(bean);
            Assertions.assertNotNull(bean.getName());
            Assertions.assertTrue(bean.getAge() != 0);

            Assertions.assertNull(bean.getCreatedDate());
            Assertions.assertNull(bean.getNested());
        }

        List<TestBean> emptyList = Beans.newRandomList(TestBean.class, Arrays.asList("name"), 0);
        Assertions.assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testFillWithDifferentTypes() {
        TestBean bean = Beans.newRandom(TestBean.class);

        Assertions.assertNotNull(bean.getName());
        Assertions.assertTrue(bean.getName().length() > 0);

        Assertions.assertNotNull(bean.getCreatedDate());
        long timeDiff = System.currentTimeMillis() - bean.getCreatedDate().getTime();
        Assertions.assertTrue(timeDiff >= 0 && timeDiff < 1000);

        Assertions.assertNotNull(bean.getNested());
        Assertions.assertNotNull(bean.getNested().getValue());
    }

    @Test
    public void testFillNonBeanClass() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.newRandom(String.class));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.newRandom(Integer.class));
    }

    @Test
    public void testFillMultipleTimes() {
        TestBean bean1 = Beans.newRandom(TestBean.class);
        TestBean bean2 = Beans.newRandom(TestBean.class);

        Assertions.assertNotEquals(bean1.getName(), bean2.getName());

    }

    @Test
    public void testFillWithInvalidPropertyNames() {
        TestBean bean = new TestBean();

        try {
            Beans.randomize(bean, Arrays.asList("nonExistentProperty"));
        } catch (Exception e) {
        }
    }

    public static class NumberBean {
        private Integer integerValue;
        private Long longValue;
        private Double doubleValue;
        private Float floatValue;
        private Short shortValue;
        private Byte byteValue;

        public Integer getIntegerValue() {
            return integerValue;
        }

        public void setIntegerValue(Integer integerValue) {
            this.integerValue = integerValue;
        }

        public Long getLongValue() {
            return longValue;
        }

        public void setLongValue(Long longValue) {
            this.longValue = longValue;
        }

        public Double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(Double doubleValue) {
            this.doubleValue = doubleValue;
        }

        public Float getFloatValue() {
            return floatValue;
        }

        public void setFloatValue(Float floatValue) {
            this.floatValue = floatValue;
        }

        public Short getShortValue() {
            return shortValue;
        }

        public void setShortValue(Short shortValue) {
            this.shortValue = shortValue;
        }

        public Byte getByteValue() {
            return byteValue;
        }

        public void setByteValue(Byte byteValue) {
            this.byteValue = byteValue;
        }
    }

    @Test
    public void testFillNumberTypes() {
        NumberBean bean = Beans.newRandom(NumberBean.class);

        Assertions.assertNotNull(bean.getIntegerValue());
        Assertions.assertNotNull(bean.getLongValue());
        Assertions.assertNotNull(bean.getDoubleValue());
        Assertions.assertNotNull(bean.getFloatValue());
        Assertions.assertNotNull(bean.getShortValue());
        Assertions.assertNotNull(bean.getByteValue());
    }
}
