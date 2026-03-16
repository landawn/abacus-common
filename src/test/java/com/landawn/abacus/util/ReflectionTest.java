package com.landawn.abacus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.IntStream;

@Tag("2025")
public class ReflectionTest extends TestBase {

    public static class X {

        public long m_01() {
            return m_11();
        }

        public long m_02() {
            return m_12();
        }

        public static long m_11() {
            long sum = IntStream.range(0, 10).sum();
            assertEquals(45, sum);
            return sum;
        }

        public static long m_12() {
            long sum = IntStream.range(0, 10000).sum();
            assertEquals(49995000, sum);
            return sum;
        }
    }

    public static class TestClass {
        public String publicField = "default";
        private String privateField = "privateValue";
        private int intField = 42;

        public TestClass() {
        }

        public TestClass(String value) {
            this.publicField = value;
        }

        public TestClass(String value, int intValue) {
            this.publicField = value;
            this.intField = intValue;
        }

        public String getPublicField() {
            return publicField;
        }

        public void setPublicField(String value) {
            this.publicField = value;
        }

        public String concat(String a, String b) {
            return a + b;
        }

        public int calculate(int a, int b) {
            return a + b;
        }

        public void voidMethod() {
        }

        public String overloadedMethod(String value) {
            return value;
        }

        public String overloadedMethod(String value, int num) {
            return value + num;
        }
    }

    public static class PrimitiveTestClass {
        public int intValue = 10;
        public boolean boolValue = true;
        public double doubleValue = 3.14;

        public PrimitiveTestClass() {
        }

        public PrimitiveTestClass(int value) {
            this.intValue = value;
        }

        public int multiply(int a, int b) {
            return a * b;
        }

        public boolean isPositive(int value) {
            return value > 0;
        }
    }

    public static class SubTestClass extends TestClass {
        public String subField = "subValue";

        public SubTestClass() {
            super("publicValue");
        }

        @Override
        public String getPublicField() {
            return "sub:" + super.getPublicField();
        }

        public String getSubField() {
            return subField;
        }
    }

    @Test
    public void test_01() {
        assertDoesNotThrow(() -> {
            N.println(Reflection.on(Pair.class).newInstance("left", 2).invoke("left"));
            N.println(Reflection.on(Pair.class).newInstance("left", 2).call("setRight", "right").invoke("right"));
            N.println(Reflection.on(Pair.class).newInstance("left", 2).call("setRight", new Object[] { null }).invoke("right"));
            N.println(Reflection.on(Pair.class).newInstance("left", 2).call("set", null, "right").invoke("right"));

            N.println(X.m_11());
            N.println(X.m_12());
        });
    }

    @Test
    public void test_perf() {
        assertDoesNotThrow(() -> {
            Profiler.run(3, 10000, 3, "m_01 by refelct", () -> Reflection.on(X.class).newInstance().invoke("m_01")).printResult();

            Profiler.run(3, 10000, 3, "m_01 direct call", () -> new X().m_01()).printResult();

            Profiler.run(3, 10000, 3, "m_02 by refelct", () -> Reflection.on(X.class).newInstance().invoke("m_02")).printResult();

            Profiler.run(3, 10000, 3, "m_02 direct call", () -> new X().m_02()).printResult();

            Profiler.run(3, 10000, 3, "m_11 by refelct", () -> Reflection.on(X.class).newInstance().invoke("m_11")).printResult();

            Profiler.run(3, 10000, 3, "m_11 direct call", () -> X.m_11()).printResult();

            Profiler.run(3, 10000, 3, "m_12 by refelct", () -> Reflection.on(X.class).newInstance().invoke("m_12")).printResult();

            Profiler.run(3, 10000, 3, "m_12 direct call", () -> X.m_12()).printResult();
        });
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
        Reflection<TestClass> ref = Reflection.on(TestClass.class).newInstance();
        TestClass instance = ref.instance();

        Assertions.assertNotNull(instance);
        Assertions.assertEquals("default", instance.publicField);
    }

    @Test
    public void testNewWithArgs() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class).newInstance("custom");
        TestClass instance = ref.instance();

        Assertions.assertNotNull(instance);
        Assertions.assertEquals("custom", instance.publicField);
    }

    @Test
    public void testNewWithMultipleArgs() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class).newInstance("custom", 100);
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

    @Test
    public void testOnWithClassName() {
        Reflection<TestClass> reflection = Reflection.on("com.landawn.abacus.util.ReflectionTest$TestClass");
        Assertions.assertNotNull(reflection);
        TestClass instance = reflection.newInstance().instance();
        Assertions.assertNotNull(instance);
    }

    @Test
    public void testOnWithClassNameInvalid() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Reflection.on("non.existent.Class");
        });
    }

    @Test
    public void testOnWithClass() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Assertions.assertNotNull(reflection);
        TestClass instance = reflection.newInstance().instance();
        Assertions.assertNotNull(instance);
    }

    @Test
    public void testOnWithTarget() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        Assertions.assertNotNull(reflection);
        Assertions.assertEquals(target, reflection.instance());
    }

    @Test
    public void testNewWithEmptyArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Object[] emptyArgs = new Object[0];
        Reflection<TestClass> newReflection = reflection.newInstance(emptyArgs);
        Assertions.assertNotNull(newReflection);
        Assertions.assertNotNull(newReflection.instance());
    }

    @Test
    public void testNewWithNullArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Reflection<TestClass> newReflection = reflection.newInstance((Object) null);
        Assertions.assertNotNull(newReflection);
        Assertions.assertNotNull(newReflection.instance());
    }

    @Test
    public void testNewWithInvalidArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Assertions.assertThrows(RuntimeException.class, () -> {
            reflection.newInstance(123, "invalid", true);
        });
    }

    @Test
    public void testGetNonExistentField() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        Assertions.assertThrows(RuntimeException.class, () -> {
            reflection.get("nonExistentField");
        });
    }

    @Test
    public void testSetNonExistentField() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        Assertions.assertThrows(RuntimeException.class, () -> {
            reflection.set("nonExistentField", "value");
        });
    }

    @Test
    public void testInvokeVoidMethod() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        Object result = reflection.invoke("voidMethod");
        Assertions.assertNull(result);
    }

    @Test
    public void testInvokeOverloadedMethod() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        String result1 = reflection.invoke("overloadedMethod", "test");
        Assertions.assertEquals("test", result1);

        String result2 = reflection.invoke("overloadedMethod", "test", 123);
        Assertions.assertEquals("test123", result2);
    }

    @Test
    public void testInvokeNonExistentMethod() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        Assertions.assertThrows(RuntimeException.class, () -> {
            reflection.invoke("nonExistentMethod");
        });
    }

    @Test
    public void testCallNonExistentMethod() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        Assertions.assertThrows(RuntimeException.class, () -> {
            reflection.call("nonExistentMethod");
        });
    }

    @Test
    public void testWithPrimitiveTypes() {
        PrimitiveTestClass target = new PrimitiveTestClass();
        Reflection<PrimitiveTestClass> reflection = Reflection.on(target);

        Integer intValue = reflection.get("intValue");
        Assertions.assertEquals(10, intValue);

        reflection.set("intValue", 20);
        Assertions.assertEquals(20, target.intValue);

        Boolean boolValue = reflection.get("boolValue");
        Assertions.assertTrue(boolValue);

        reflection.set("boolValue", false);
        Assertions.assertFalse(target.boolValue);

        Integer result = reflection.invoke("multiply", 4, 5);
        Assertions.assertEquals(20, result);

        Boolean isPositive = reflection.invoke("isPositive", 10);
        Assertions.assertTrue(isPositive);
    }

    @Test
    public void testNewWithPrimitiveArgs() {
        Reflection<PrimitiveTestClass> reflection = Reflection.on(PrimitiveTestClass.class);
        Reflection<PrimitiveTestClass> newReflection = reflection.newInstance(42);
        Assertions.assertNotNull(newReflection);
        Assertions.assertEquals(42, newReflection.instance().intValue);
    }

    @Test
    public void testSetNullValue() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        reflection.set("publicField", null);
        Assertions.assertNull(target.publicField);
    }

    @Test
    public void testInvokeWithNullArgs() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        String result = reflection.invoke("concat", null, " World");
        Assertions.assertEquals("null World", result);
    }

    @Test
    public void testFieldCaching() {
        TestClass target1 = new TestClass();
        TestClass target2 = new TestClass();

        Reflection<TestClass> reflection1 = Reflection.on(target1);
        Reflection<TestClass> reflection2 = Reflection.on(target2);

        reflection1.get("publicField");
        reflection1.get("publicField");
        reflection2.get("publicField");

        reflection1.set("publicField", "value1");
        reflection2.set("publicField", "value2");

        Assertions.assertEquals("value1", target1.publicField);
        Assertions.assertEquals("value2", target2.publicField);
    }

    @Test
    public void testMethodCaching() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);

        for (int i = 0; i < 3; i++) {
            String result = reflection.invoke("concat", "test", String.valueOf(i));
            Assertions.assertEquals("test" + i, result);
        }
    }

    @Test
    public void testWithInheritance() {
        SubTestClass target = new SubTestClass();
        Reflection<SubTestClass> reflection = Reflection.on(target);

        String publicField = reflection.get("publicField");
        Assertions.assertEquals("publicValue", publicField);

        String subField = reflection.get("subField");
        Assertions.assertEquals("subValue", subField);

        String result = reflection.invoke("getPublicField");
        Assertions.assertEquals("sub:publicValue", result);

        String subResult = reflection.invoke("getSubField");
        Assertions.assertEquals("subValue", subResult);
    }

    @Test
    public void testNewInstance() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class);
        Reflection<TestClass> newRef = ref.newInstance();
        Assertions.assertNotNull(newRef);
        Assertions.assertNotNull(newRef.instance());
        Assertions.assertEquals("default", newRef.instance().publicField);

        Reflection<TestClass> newRef2 = ref.newInstance("hello", 99);
        Assertions.assertNotNull(newRef2.instance());
        Assertions.assertEquals("hello", newRef2.instance().publicField);
    }

    @Test
    public void testInstance_NullWhenCreatedFromClass() {
        Reflection<TestClass> ref = Reflection.on(TestClass.class);
        Assertions.assertNull(ref.instance());
    }

    @Test
    public void testCallChaining() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);

        Reflection<TestClass> result = ref.call("setPublicField", "chained").call("voidMethod");
        Assertions.assertSame(ref, result);
        Assertions.assertEquals("chained", instance.publicField);
    }

    @Test
    public void testInvokeConcatMethod() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);

        String result = ref.invoke("concat", "Hello", " World");
        Assertions.assertEquals("Hello World", result);
    }

    @Test
    public void testNewInstanceFromClassNameWithArgs() {
        Reflection<TestClass> ref = Reflection.on("com.landawn.abacus.util.ReflectionTest$TestClass");
        TestClass instance = ref.newInstance("fromClassName").instance();
        Assertions.assertNotNull(instance);
        Assertions.assertEquals("fromClassName", instance.publicField);
    }

    @Test
    public void testDoubleGetField() {
        PrimitiveTestClass target = new PrimitiveTestClass();
        Reflection<PrimitiveTestClass> ref = Reflection.on(target);

        double value = ref.get("doubleValue");
        Assertions.assertEquals(3.14, value, 0.001);
    }

    @Test
    public void testSetDoubleField() {
        PrimitiveTestClass target = new PrimitiveTestClass();
        Reflection<PrimitiveTestClass> ref = Reflection.on(target);

        ref.set("doubleValue", 2.718);
        double value = ref.get("doubleValue");
        Assertions.assertEquals(2.718, value, 0.001);
    }

    @Test
    public void testIsPositiveMethod() {
        PrimitiveTestClass target = new PrimitiveTestClass();
        Reflection<PrimitiveTestClass> ref = Reflection.on(target);

        Boolean negative = ref.invoke("isPositive", -5);
        Assertions.assertFalse(negative);
    }

}
