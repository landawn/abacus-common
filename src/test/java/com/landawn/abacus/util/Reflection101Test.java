package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Reflection101Test extends TestBase {

    public static class TestClass {
        public String publicField = "default";
        private int privateField = 42;

        public TestClass() {
        }

        public TestClass(String value) {
            this.publicField = value;
        }

        public TestClass(String value, int number) {
            this.publicField = value;
            this.privateField = number;
        }

        public String getPublicField() {
            return publicField;
        }

        public void setPublicField(String value) {
            this.publicField = value;
        }

        public int calculate(int a, int b) {
            return a + b;
        }

        public void voidMethod() {
        }
    }

    @Test
    public void testOnClass() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class);
        Assertions.assertNotNull(ref);
    }

    @Test
    public void testOnInstance() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);
        Assertions.assertNotNull(ref);
        Assertions.assertSame(instance, ref.instance());
    }

    @Test
    public void testNewNoArgs() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class)._new();
        TestClass instance = ref.instance();

        Assertions.assertNotNull(instance);
        Assertions.assertEquals("default", instance.publicField);
    }

    @Test
    public void testNewWithArgs() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class)._new("custom");
        TestClass instance = ref.instance();

        Assertions.assertNotNull(instance);
        Assertions.assertEquals("custom", instance.publicField);
    }

    @Test
    public void testNewWithMultipleArgs() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class)._new("custom", 100);
        TestClass instance = ref.instance();

        Assertions.assertNotNull(instance);
        Assertions.assertEquals("custom", instance.publicField);
    }

    @Test
    public void testInstance() {
        TestClass testObj = new TestClass();
        Reflection<TestClass> ref = Reflection.on(testObj);

        Assertions.assertSame(testObj, ref.instance());
    }

    @Test
    public void testGet() {
        TestClass instance = new TestClass("test value");
        Reflection<TestClass> ref = Reflection.on(instance);

        String value = ref.get("publicField");
        Assertions.assertEquals("test value", value);
    }

    @Test
    public void testSet() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);

        ref.set("publicField", "new value");
        Assertions.assertEquals("new value", instance.publicField);
    }

    @Test
    public void testSetChaining() {
        TestClass instance = new TestClass();
        Reflection<TestClass> result = Reflection.on(instance).set("publicField", "value1").set("publicField", "value2");

        Assertions.assertEquals("value2", instance.publicField);
    }

    @Test
    public void testInvoke() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);

        Integer result = ref.invoke("calculate", 5, 3);
        Assertions.assertEquals(8, result);
    }

    @Test
    public void testInvokeNoArgs() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);

        String result = ref.invoke("getPublicField");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testCall() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);

        Reflection<TestClass> result = ref.call("voidMethod");
        Assertions.assertSame(ref, result);
    }

    @Test
    public void testCallWithArgs() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);

        Reflection<TestClass> result = ref.call("setPublicField", "new value");
        Assertions.assertSame(ref, result);
        Assertions.assertEquals("new value", instance.publicField);
    }
}
