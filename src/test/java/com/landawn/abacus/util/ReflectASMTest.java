package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ReflectASMTest extends TestBase {

    public abstract static class AbstractConstructible {
        public AbstractConstructible() {
        }
    }

    @Test
    public void testNewInstanceOfAbstractClassPropagatesInstantiationError() {
        // Generated constructor access invokes new directly, so an abstract class fails with an Error.
        Assertions.assertThrows(InstantiationError.class, () -> ReflectASM.on(AbstractConstructible.class).newInstance());
    }

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

    public static class FinalFieldBean {
        // Not a compile-time constant, so a direct read cannot be inlined and hide a write.
        public final String text;

        FinalFieldBean(final String text) {
            this.text = text;
        }
    }

    public static class ShadowBase {
        public String x = "BASE";
    }

    public static class ShadowSub extends ShadowBase {
        private String x = "SUB";

        String ownX() {
            return x;
        }
    }

    public static class PublicShadowSub extends ShadowBase {
        public String x = "PUBSUB";
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

    @Test
    public void testConcurrentAccessorInitializationAndUse() {
        IntStream.range(0, 100).parallel().forEach(i -> {
            ReflectASM<TestPerson> reflect = ReflectASM.on(TestPerson.class).newInstance().set("name", "person-" + i).set("age", i);
            Assertions.assertEquals("person-" + i, (String) reflect.get("name"));
            Assertions.assertEquals(i, (int) reflect.<Integer> invoke("calculateBirthYear", i * 2));
        });
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
    public void testGetAfterSet() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        reflect.set("name", "TestName");
        String name = reflect.get("name");
        Assertions.assertEquals("TestName", name);
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
    public void testInvokeVoidMethod() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        Assertions.assertFalse(person.active);
        Object result = reflect.invoke("doSomething");
        Assertions.assertNull(result);
        Assertions.assertTrue(person.active);
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
    public void testCallReturnsSameInstance() {
        TestPerson person = new TestPerson();
        ReflectASM<TestPerson> reflect = ReflectASM.on(person);

        ReflectASM<TestPerson> returned = reflect.call("doSomething");
        Assertions.assertSame(reflect, returned);
    }

    /**
     * Pins the class-level "Limitations" note and the {@code @throws IllegalAccessError} tag on
     * {@link ReflectASM#set(String, Object)}: a {@code final} field can be READ through the generated
     * accessor but never assigned - the accessor's {@code putfield} is rejected when it is RESOLVED, not
     * when the accessor class links, which is why the very same accessor still serves the read below - and
     * the resulting {@link IllegalAccessError} is an {@code Error}, not a {@code RuntimeException}, so
     * {@code catch (RuntimeException)} around a {@code set} does not catch it.
     */
    @Test
    public void reviewFixes20260911_setOnAFinalFieldThrowsIllegalAccessErrorNotARuntimeException() throws Exception {
        final FinalFieldBean bean = new FinalFieldBean(new String("old"));

        final Throwable error = Assertions.assertThrows(IllegalAccessError.class, () -> ReflectASM.on(bean).set("text", "new"));
        Assertions.assertFalse(error instanceof RuntimeException, "an IllegalAccessError is an Error, so catch(RuntimeException) misses it");
        Assertions.assertEquals("old", FinalFieldBean.class.getDeclaredField("text").get(bean));

        // "Can read, but cannot assign, final fields" - the read half works.
        Assertions.assertEquals("old", (String) ReflectASM.on(bean).get("text"));
    }

    /**
     * Pins the warning on the by-name {@code get}/{@code set} pair: reflectasm's {@code FieldAccess} table omits
     * private fields, so a private subclass field that hides a non-private superclass field is invisible and the
     * name lookup silently resolves to the INHERITED field instead of throwing. {@link Reflection} resolves the
     * {@code Field} itself and is unaffected; the documented {@code RuntimeException} is still delivered for an
     * unshadowed private field.
     */
    @Test
    public void reviewFixes20260911_byNameAccessorsResolveToTheInheritedFieldWhenAPrivateFieldHidesIt() {
        final ShadowSub sub = new ShadowSub();

        // Reads the superclass field, and does NOT throw.
        Assertions.assertEquals("BASE", (String) ReflectASM.on(sub).get("x"));

        // Writes the superclass field and leaves the private subclass field untouched.
        ReflectASM.on(sub).set("x", "WRITTEN");
        Assertions.assertEquals("SUB", sub.ownX());
        Assertions.assertEquals("WRITTEN", ((ShadowBase) sub).x);

        // Control 1: the public entry point resolves the declared field and is correct.
        final ShadowSub sub2 = new ShadowSub();
        Assertions.assertEquals("SUB", Reflection.on(sub2).get("x"));
        Reflection.on(sub2).set("x", "VIA_REFLECTION");
        Assertions.assertEquals("VIA_REFLECTION", sub2.ownX());
        Assertions.assertEquals("BASE", ((ShadowBase) sub2).x);

        // Control 2: when both declarations are non-private the subclass field is picked correctly.
        final PublicShadowSub pub = new PublicShadowSub();
        Assertions.assertEquals("PUBSUB", (String) ReflectASM.on(pub).get("x"));
        ReflectASM.on(pub).set("x", "W2");
        Assertions.assertEquals("W2", pub.x);
        Assertions.assertEquals("BASE", ((ShadowBase) pub).x);

        // Control 3: an absent (or unshadowed private) field still raises the documented RuntimeException.
        Assertions.assertThrows(RuntimeException.class, () -> ReflectASM.on(sub).get("noSuchField"));
    }

    /**
     * Pins the "Internal adapter" paragraph of the class javadoc: {@code ReflectASM} is package-private, so the
     * examples in that javadoc compile only from inside {@code com.landawn.abacus.util}. {@link Reflection} is
     * the public entry point.
     */
    @Test
    public void reviewFixes20260911_reflectAsmIsPackagePrivateAndReflectionIsThePublicEntryPoint() {
        Assertions.assertFalse(java.lang.reflect.Modifier.isPublic(ReflectASM.class.getModifiers()));
        Assertions.assertTrue(java.lang.reflect.Modifier.isPublic(Reflection.class.getModifiers()));
    }

    public static class MissingDependency {
    }

    public static class BrokenSignatureHolder {
        public String ok() {
            return "ok";
        }

        // The only reason this class exists: a parameter type that can be made unresolvable, which makes
        // Class.getDeclaredMethods() - and therefore ReflectASM's accessor generation - raise a LinkageError.
        public void needsMissing(final MissingDependency dep) {
        }
    }

    /**
     * Defines one class from its own bytes and refuses to load exactly one other, so a method signature of the
     * defined class cannot be resolved. This is the one route into {@code canInvoke}'s catch that needs no
     * doctored ReflectASM jar.
     */
    private static final class OneClassHidingLoader extends ClassLoader {
        private final String definedName;

        private final String hiddenName;

        OneClassHidingLoader(final String definedName, final String hiddenName) {
            super(ReflectASMTest.class.getClassLoader());
            this.definedName = definedName;
            this.hiddenName = hiddenName;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (hiddenName.equals(name)) {
                throw new ClassNotFoundException(name + " is hidden on purpose");
            }

            if (!definedName.equals(name)) {
                return super.loadClass(name, resolve);
            }

            Class<?> cls = findLoadedClass(name);

            if (cls == null) {
                final String resource = name.replace('.', '/') + ".class";

                try (java.io.InputStream is = getParent().getResourceAsStream(resource)) {
                    if (is == null) {
                        throw new ClassNotFoundException(resource + " not found");
                    }

                    final byte[] bytes = is.readAllBytes();
                    cls = defineClass(name, bytes, 0, bytes.length);
                } catch (final java.io.IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }

            if (resolve) {
                resolveClass(cls);
            }

            return cls;
        }
    }

    /**
     * {@code canInvoke} must answer the reachability question rather than let a {@link LinkageError} escape.
     * Generating a {@code MethodAccess} walks {@code getDeclaredMethods()}, so a class with an unresolvable
     * parameter type - the in-process stand-in for a broken ReflectASM deployment, whose shaded
     * {@code com.esotericsoftware.asm} classes are missing - made it throw {@link NoClassDefFoundError},
     * which is not a {@code RuntimeException} and so escaped the probe's {@code catch}. Because
     * {@code Reflection.invoke}/{@code call} use {@code canInvoke} as their only gate, that Error reached the
     * caller instead of degrading to standard reflection.
     */
    @Test
    public void reviewFixes20260911_canInvokeReportsFalseInsteadOfLettingALinkageErrorEscape() throws Exception {
        final ClassLoader loader = new OneClassHidingLoader(BrokenSignatureHolder.class.getName(), MissingDependency.class.getName());
        final Class<?> holderCls = loader.loadClass(BrokenSignatureHolder.class.getName());
        final Object holder = holderCls.getDeclaredConstructor().newInstance();

        Assertions.assertNotSame(BrokenSignatureHolder.class, holderCls);

        // The premise: the declared-method table cannot be built at all.
        Assertions.assertThrows(NoClassDefFoundError.class, holderCls::getDeclaredMethods);

        // So ReflectASM cannot resolve anything on this class, and must say so.
        Assertions.assertFalse(ReflectASM.on(holder).canInvoke("ok"));
        Assertions.assertFalse(ReflectASM.on(holder).canInvoke("noSuchMethod"));
    }
}
