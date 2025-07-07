package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Reflection100Test extends TestBase {
    
    public static class TestClass {
        public String publicField = "publicValue";
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
        
        public int add(int a, int b) {
            return a + b;
        }
        
        public void voidMethod() {
            // do nothing
        }
        
        public String overloadedMethod(String value) {
            return value;
        }
        
        public String overloadedMethod(String value, int num) {
            return value + num;
        }
    }
    
    @Test
    public void testOnWithClassName() {
        Reflection<TestClass> reflection = Reflection.on("com.landawn.abacus.util.Reflection100Test$TestClass");
        Assertions.assertNotNull(reflection);
        TestClass instance = reflection._new().instance();
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
        TestClass instance = reflection._new().instance();
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
    public void testNewNoArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Reflection<TestClass> newReflection = reflection._new();
        Assertions.assertNotNull(newReflection);
        Assertions.assertNotNull(newReflection.instance());
        Assertions.assertEquals("publicValue", newReflection.instance().publicField);
    }
    
    @Test
    public void testNewWithArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Reflection<TestClass> newReflection = reflection._new("customValue");
        Assertions.assertNotNull(newReflection);
        Assertions.assertNotNull(newReflection.instance());
        Assertions.assertEquals("customValue", newReflection.instance().publicField);
    }
    
    @Test
    public void testNewWithMultipleArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Reflection<TestClass> newReflection = reflection._new("customValue", 100);
        Assertions.assertNotNull(newReflection);
        Assertions.assertNotNull(newReflection.instance());
        Assertions.assertEquals("customValue", newReflection.instance().publicField);
    }
    
    @Test
    public void testNewWithEmptyArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Object[] emptyArgs = new Object[0];
        Reflection<TestClass> newReflection = reflection._new(emptyArgs);
        Assertions.assertNotNull(newReflection);
        Assertions.assertNotNull(newReflection.instance());
    }
    
    @Test
    public void testNewWithNullArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Reflection<TestClass> newReflection = reflection._new((Object) null);
        Assertions.assertNotNull(newReflection);
        Assertions.assertNotNull(newReflection.instance());
    }
    
    @Test
    public void testNewWithInvalidArgs() {
        Reflection<TestClass> reflection = Reflection.on(TestClass.class);
        Assertions.assertThrows(RuntimeException.class, () -> {
            reflection._new(123, "invalid", true);
        });
    }
    
    @Test
    public void testInstance() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        Assertions.assertEquals(target, reflection.instance());
        
        Reflection<TestClass> classReflection = Reflection.on(TestClass.class);
        Assertions.assertNull(classReflection.instance());
    }
    
    @Test
    public void testGet() {
        TestClass target = new TestClass();
        target.publicField = "testValue";
        Reflection<TestClass> reflection = Reflection.on(target);
        
        String value = reflection.get("publicField");
        Assertions.assertEquals("testValue", value);
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
    public void testSet() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        
        reflection.set("publicField", "newValue");
        Assertions.assertEquals("newValue", target.publicField);
        
        // Test method chaining
        Reflection<TestClass> result = reflection.set("publicField", "chainedValue");
        Assertions.assertSame(reflection, result);
        Assertions.assertEquals("chainedValue", target.publicField);
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
    public void testInvoke() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        
        String result = reflection.invoke("concat", "Hello", " World");
        Assertions.assertEquals("Hello World", result);
        
        Integer sum = reflection.invoke("add", 5, 3);
        Assertions.assertEquals(8, sum);
    }
    
    @Test
    public void testInvokeNoArgs() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        
        String result = reflection.invoke("getPublicField");
        Assertions.assertEquals("publicValue", result);
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
    public void testCall() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        
        // Test method chaining
        Reflection<TestClass> result = reflection.call("setPublicField", "calledValue");
        Assertions.assertSame(reflection, result);
        Assertions.assertEquals("calledValue", target.publicField);
        
        // Test void method
        result = reflection.call("voidMethod");
        Assertions.assertSame(reflection, result);
    }
    
    @Test
    public void testCallNonExistentMethod() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        
        Assertions.assertThrows(RuntimeException.class, () -> {
            reflection.call("nonExistentMethod");
        });
    }
    
    // Test with primitive types
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
    
    @Test
    public void testWithPrimitiveTypes() {
        PrimitiveTestClass target = new PrimitiveTestClass();
        Reflection<PrimitiveTestClass> reflection = Reflection.on(target);
        
        // Test get/set with primitives
        Integer intValue = reflection.get("intValue");
        Assertions.assertEquals(10, intValue);
        
        reflection.set("intValue", 20);
        Assertions.assertEquals(20, target.intValue);
        
        Boolean boolValue = reflection.get("boolValue");
        Assertions.assertTrue(boolValue);
        
        reflection.set("boolValue", false);
        Assertions.assertFalse(target.boolValue);
        
        // Test invoke with primitives
        Integer result = reflection.invoke("multiply", 4, 5);
        Assertions.assertEquals(20, result);
        
        Boolean isPositive = reflection.invoke("isPositive", 10);
        Assertions.assertTrue(isPositive);
    }
    
    @Test
    public void testNewWithPrimitiveArgs() {
        Reflection<PrimitiveTestClass> reflection = Reflection.on(PrimitiveTestClass.class);
        Reflection<PrimitiveTestClass> newReflection = reflection._new(42);
        Assertions.assertNotNull(newReflection);
        Assertions.assertEquals(42, newReflection.instance().intValue);
    }
    
    // Test with null handling
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
    
    // Test caching behavior (internal implementation detail)
    @Test
    public void testFieldCaching() {
        TestClass target1 = new TestClass();
        TestClass target2 = new TestClass();
        
        Reflection<TestClass> reflection1 = Reflection.on(target1);
        Reflection<TestClass> reflection2 = Reflection.on(target2);
        
        // Access same field multiple times to test caching
        reflection1.get("publicField");
        reflection1.get("publicField");
        reflection2.get("publicField");
        
        // Should still work correctly
        reflection1.set("publicField", "value1");
        reflection2.set("publicField", "value2");
        
        Assertions.assertEquals("value1", target1.publicField);
        Assertions.assertEquals("value2", target2.publicField);
    }
    
    @Test
    public void testMethodCaching() {
        TestClass target = new TestClass();
        Reflection<TestClass> reflection = Reflection.on(target);
        
        // Invoke same method multiple times to test caching
        for (int i = 0; i < 3; i++) {
            String result = reflection.invoke("concat", "test", String.valueOf(i));
            Assertions.assertEquals("test" + i, result);
        }
    }
    
    // Test with inheritance
    public static class SubTestClass extends TestClass {
        public String subField = "subValue";
        
        public SubTestClass() {
            super();
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
    public void testWithInheritance() {
        SubTestClass target = new SubTestClass();
        Reflection<SubTestClass> reflection = Reflection.on(target);
        
        // Test accessing inherited field
        String publicField = reflection.get("publicField");
        Assertions.assertEquals("publicValue", publicField);
        
        // Test accessing subclass field
        String subField = reflection.get("subField");
        Assertions.assertEquals("subValue", subField);
        
        // Test invoking overridden method
        String result = reflection.invoke("getPublicField");
        Assertions.assertEquals("sub:publicValue", result);
        
        // Test invoking subclass method
        String subResult = reflection.invoke("getSubField");
        Assertions.assertEquals("subValue", subResult);
    }
}