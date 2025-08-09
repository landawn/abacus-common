package com.landawn.abacus.util;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ReflectASM100Test extends TestBase {

    public static class TestPerson {
        public String name;
        public int age;
        public boolean active;

        public TestPerson() {
            // Default constructor required for ReflectASM
        }

        public TestPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setDetails(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getInfo() {
            return name + " is " + age + " years old";
        }

        public int calculateBirthYear(int currentYear) {
            return currentYear - age;
        }

        public void doSomething() {
            this.active = true;
        }
    }

    @Test
    public void testOnWithClassName() {
        ReflectASM<TestPerson> reflect = ReflectASM.on("com.landawn.abacus.util.ReflectASM100Test$TestPerson");
        Assertions.assertNotNull(reflect);

        // Test creating instance
        reflect._new();
    }

    @Test
    public void testOnWithClass() {
        ReflectASM<TestPerson> reflect = ReflectASM.on(TestPerson.class);
        Assertions.assertNotNull(reflect);

        reflect._new();
    }

    @Test
    public void testOnWithTarget() {
        TestPerson person = new TestPerson("John", 30);
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);
        Assertions.assertNotNull(reflect);

        // Verify we can access fields
        String name = reflect.get("name");
        Assertions.assertEquals("John", name);
    }

    @Test
    public void testNew() {
        ReflectASM<TestPerson> reflect = ReflectASM.on(TestPerson.class);
        ReflectASM<TestPerson> newReflect = reflect._new();

        newReflect._new();
    }

    @Test
    public void testGet() {
        TestPerson person = new TestPerson("Alice", 25);
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        String name = reflect.get("name");
        Integer age = reflect.get("age");
        Boolean active = reflect.get("active");

        Assertions.assertEquals("Alice", name);
        Assertions.assertEquals(25, age);
        Assertions.assertFalse(active);
    }

    @Test
    public void testSet() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        reflect.set("name", "Bob").set("age", 35).set("active", true);

        Assertions.assertEquals("Bob", person.name);
        Assertions.assertEquals(35, person.age);
        Assertions.assertTrue(person.active);
    }

    @Test
    public void testSetChaining() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> result = ReflectASM.on(person).set("name", "Charlie").set("age", 40);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Charlie", person.name);
        Assertions.assertEquals(40, person.age);
    }

    @Test
    public void testInvokeNoArgs() {
        TestPerson person = new TestPerson("David", 28);
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        String name = reflect.invoke("getName");
        Assertions.assertEquals("David", name);

        String info = reflect.invoke("getInfo");
        Assertions.assertEquals("David is 28 years old", info);
    }

    @Test
    public void testInvokeWithArgs() {
        TestPerson person = new TestPerson("Eve", 30);
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        Integer birthYear = reflect.invoke("calculateBirthYear", 2024);
        Assertions.assertEquals(1994, birthYear);

        // Test method with multiple args
        reflect.invoke("setDetails", "Frank", 45);
        Assertions.assertEquals("Frank", person.name);
        Assertions.assertEquals(45, person.age);
    }

    @Test
    public void testCallNoReturn() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        Assertions.assertFalse(person.active);
        reflect.call("doSomething");
        Assertions.assertTrue(person.active);
    }

    @Test
    public void testCallWithArgs() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> result = ReflectASM.on(person).call("setDetails", "Grace", 50);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Grace", person.name);
        Assertions.assertEquals(50, person.age);
    }

    @Test
    public void testCallChaining() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> result = ReflectASM.on(person).set("name", "Henry").set("age", 60).call("doSomething");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Henry", person.name);
        Assertions.assertEquals(60, person.age);
        Assertions.assertTrue(person.active);
    }

    @Test
    public void testComplexScenario() {
        // Test creating instance and chaining operations
        TestPerson person = new TestPerson();
        ReflectASM.on(person).set("name", "Ivy").set("age", 22).call("doSomething");

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Ivy", person.name);
        Assertions.assertEquals(22, person.age);
        Assertions.assertTrue(person.active);

        // Test getting values after creation
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);
        String info = reflect.invoke("getInfo");
        Assertions.assertEquals("Ivy is 22 years old", info);
    }

    @Test
    public void testWithList() {
        // Test that ReflectASM works with standard Java classes
        ReflectASM<ArrayList> listReflect = ReflectASM.on(ArrayList.class);
        listReflect._new();
    }
}