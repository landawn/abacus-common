package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ReflectASMTest extends TestBase {

    public static class TestPerson {
        public String name;
        public int age;
        public boolean active;

        public TestPerson() {
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
        ReflectASM<TestPerson> reflect = ReflectASM.on("com.landawn.abacus.util.ReflectASMTest$TestPerson");
        Assertions.assertNotNull(reflect);

        reflect.newInstance();
    }

    @Test
    public void testOnWithClass() {
        ReflectASM<TestPerson> reflect = ReflectASM.on(TestPerson.class);
        Assertions.assertNotNull(reflect);

        reflect.newInstance();
    }

    @Test
    public void testOnWithTarget() {
        TestPerson person = new TestPerson("John", 30);
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);
        Assertions.assertNotNull(reflect);

        String name = reflect.get("name");
        Assertions.assertEquals("John", name);
    }

    @Test
    public void testNew() {
        ReflectASM<TestPerson> reflect = ReflectASM.on(TestPerson.class);
        ReflectASM<TestPerson> newReflect = reflect.newInstance();

        newReflect.newInstance();
        assertNotNull(newReflect);
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
        TestPerson person = new TestPerson();
        ReflectASM.on(person).set("name", "Ivy").set("age", 22).call("doSomething");

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Ivy", person.name);
        Assertions.assertEquals(22, person.age);
        Assertions.assertTrue(person.active);

        ReflectASM<TestPerson> reflect = ReflectASM.on(person);
        String info = reflect.invoke("getInfo");
        Assertions.assertEquals("Ivy is 22 years old", info);
    }

    @Test
    public void testWithList() {
        ReflectASM<ArrayList> listReflect = ReflectASM.on(ArrayList.class);
        listReflect.newInstance();
        assertNotNull(listReflect);
    }

    @Test
    public void testNewInstance() {
        ReflectASM<TestPerson> reflect = ReflectASM.on(TestPerson.class);
        ReflectASM<TestPerson> newReflect = reflect.newInstance();

        Assertions.assertNotNull(newReflect);
        Assertions.assertNotNull(newReflect.instance());
        Assertions.assertNull(newReflect.instance().name);
        Assertions.assertEquals(0, newReflect.instance().age);
    }

    @Test
    public void testInstance() {
        ReflectASM<TestPerson> reflectFromClass = ReflectASM.on(TestPerson.class);
        Assertions.assertNull(reflectFromClass.instance());

        TestPerson person = new TestPerson("Test", 20);
        ReflectASM<TestPerson> reflectFromInstance = ReflectASM.on(person);
        Assertions.assertSame(person, reflectFromInstance.instance());

        ReflectASM<TestPerson> reflectNew = ReflectASM.on(TestPerson.class).newInstance();
        Assertions.assertNotNull(reflectNew.instance());
    }

    @Test
    public void testOnWithInvalidClassName() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            ReflectASM.on("com.nonexistent.FakeClass");
        });
    }

    @Test
    public void testOnWithNullClass() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ReflectASM.on((Class<?>) null);
        });
    }

    @Test
    public void testOnWithNullInstance() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ReflectASM.on((Object) null);
        });
    }

    @Test
    public void testNewInstanceAndSetFields() {
        ReflectASM<TestPerson> reflect = ReflectASM.on(TestPerson.class).newInstance();
        reflect.set("name", "NewPerson").set("age", 99).set("active", true);

        Assertions.assertEquals("NewPerson", reflect.instance().name);
        Assertions.assertEquals(99, reflect.instance().age);
        Assertions.assertTrue(reflect.instance().active);
    }

    @Test
    public void testInvokeVoidMethod() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        Assertions.assertFalse(person.active);
        Object result = reflect.invoke("doSomething");
        Assertions.assertNull(result);
        Assertions.assertTrue(person.active);
    }

    @Test
    public void testCallReturnsSameInstance() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        ReflectASM<TestPerson> returned = reflect.call("doSomething");
        Assertions.assertSame(reflect, returned);
    }

    @Test
    public void testGetAfterSet() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        reflect.set("name", "TestName");
        String name = reflect.get("name");
        Assertions.assertEquals("TestName", name);
    }

    @Test
    public void testFieldAccessCaching() {
        TestPerson p1 = new TestPerson("A", 1);
        TestPerson p2 = new TestPerson("B", 2);

        ReflectASM<TestPerson> r1 = ReflectASM.on(p1);
        ReflectASM<TestPerson> r2 = ReflectASM.on(p2);

        Assertions.assertEquals("A", (String) r1.get("name"));
        Assertions.assertEquals("B", (String) r2.get("name"));

        r1.set("name", "A2");
        r2.set("name", "B2");

        Assertions.assertEquals("A2", p1.name);
        Assertions.assertEquals("B2", p2.name);
    }
}
