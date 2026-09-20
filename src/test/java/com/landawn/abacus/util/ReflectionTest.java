package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.IntStream;

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

        public String typed(int value) {
            return "int:" + value;
        }

        public String typed(String value) {
            return "str:" + value;
        }

        private String privateConcat(String a, String b) {
            return a + b;
        }

        private void privateSetField(String value) {
            this.publicField = value;
        }
    }

    public static class NullOverloadTarget {
        private Object value;

        public NullOverloadTarget() {
        }

        public NullOverloadTarget(final int value) {
            this.value = value;
        }

        public NullOverloadTarget(final String value) {
            this.value = value;
        }

        public String set(final int value) {
            this.value = value;
            return "int";
        }

        public String set(final String value) {
            this.value = value;
            return "string";
        }
    }

    public static class SpecificConstructorTarget {
        private final String selected;

        public SpecificConstructorTarget(final Object value) {
            selected = "object";
        }

        public SpecificConstructorTarget(final CharSequence value) {
            selected = "charSequence";
        }
    }

    public static class SpecificMethodParent {
        public String select(final CharSequence value) {
            return "charSequence";
        }
    }

    public static class SpecificMethodTarget extends SpecificMethodParent {
        public String select(final Object value) {
            return "object";
        }
    }

    public static class AmbiguousConstructorTarget {
        public AmbiguousConstructorTarget(final CharSequence value) {
        }

        public AmbiguousConstructorTarget(final Serializable value) {
        }
    }

    public static class AmbiguousMethodTarget {
        public String select(final CharSequence value) {
            return "charSequence";
        }

        public String select(final Serializable value) {
            return "serializable";
        }
    }

    public interface Greeter {
        default String greet(String name) {
            return "Hello " + name;
        }
    }

    public static class GreeterImpl implements Greeter {
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

    public static class PrimitiveWideningTarget {
        private final long constructedValue;

        public PrimitiveWideningTarget(final long value) {
            constructedValue = value;
        }

        public long constructedValue() {
            return constructedValue;
        }

        public String select(final int value) {
            return "int:" + value;
        }

        public String select(final long value) {
            return "long:" + value;
        }

        public long longOnly(final long value) {
            return value;
        }
    }

    public static class InvocationPhaseTarget {
        public String select(final Number value) {
            return "number";
        }

        public String select(final long value) {
            return "long";
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

    public static class ShadowedFieldParent {
        public String value = "parent";
        public long number;
        public double decimal;
    }

    public static class ShadowedFieldChild extends ShadowedFieldParent {
        private String value = "child";
    }

    public static class StaticShadowedFieldChild extends ShadowedFieldParent {
        public static String value = "static child";
    }

    public static class FinalFieldHolder {
        // Deliberately NOT compile-time constants: a "final String f = \"old\"" initializer would be inlined
        // at every read site and hide whether the write actually landed.
        public final String text;
        public final int number;

        FinalFieldHolder(final String text, final int number) {
            this.text = text;
            this.number = number;
        }
    }

    public static class VarargsTarget {
        public VarargsTarget() {
        }

        public VarargsTarget(final String... parts) {
        }

        public String va(final String... s) {
            return "va n=" + s.length;
        }

        public String plain(final String a, final String b) {
            return "plain";
        }
    }

    public static class NoStaticMembers {
        public String instanceField = "instance";

        public String instanceMethod() {
            return "instanceMethod";
        }

        public static String staticMethod() {
            return "staticMethod";
        }
    }

    @Test
    public void testOn() {
        assertThrows(IllegalArgumentException.class, () -> Reflection.on((Class<Object>) null));
        // All three factories diagnose a null argument the same way, with the argument name.
        assertThrows(IllegalArgumentException.class, () -> Reflection.on((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Reflection.on((String) null));
        assertThrows(IllegalArgumentException.class, () -> Reflection.on(""));
        assertTrue(Reflection.clsFieldPool instanceof ClassValue);
        assertTrue(Reflection.clsConstructorPool instanceof ClassValue);
        assertTrue(Reflection.clsMethodPool instanceof ClassValue);

        TestClass instance = new TestClass("direct");
        assertSame(instance, new Reflection<>(TestClass.class, instance).instance());
        assertSame(instance, Reflection.on(instance).instance());
        assertNotNull(Reflection.on(TestClass.class));
        assertNull(Reflection.on(TestClass.class).instance());
        assertNotNull(Reflection.on("com.landawn.abacus.util.ReflectionTest$TestClass").newInstance().instance());
        assertThrows(RuntimeException.class, () -> Reflection.on("non.existent.Class"));
    }

    @Test
    public void testNewInstance() {
        assertEquals("default", Reflection.on(TestClass.class).newInstance().instance().publicField);
        assertEquals("custom", Reflection.on(TestClass.class).newInstance("custom").instance().publicField);
        assertEquals("custom", Reflection.on(TestClass.class).newInstance("custom", 100).instance().publicField);
        assertNotNull(Reflection.on(TestClass.class).newInstance(new Object[0]).instance());
        assertNotNull(Reflection.on(TestClass.class).newInstance((Object) null).instance());
        assertNull(Reflection.on(NullOverloadTarget.class).newInstance((Object) null).instance().value);
        assertEquals("charSequence", Reflection.on(SpecificConstructorTarget.class).newInstance(new StringBuilder("value")).instance().selected);

        RuntimeException ambiguous = assertThrows(RuntimeException.class, () -> Reflection.on(AmbiguousConstructorTarget.class).newInstance("value"));
        assertTrue(ambiguous.getMessage().contains("Ambiguous constructor"));
        assertThrows(RuntimeException.class, () -> Reflection.on(TestClass.class).newInstance(123, "invalid", true));

        TestClass fromClassName = Reflection.<TestClass> on("com.landawn.abacus.util.ReflectionTest$TestClass").newInstance("fromClassName").instance();
        assertEquals("fromClassName", fromClassName.publicField);
        Reflection<TestClass> ref = Reflection.on(TestClass.class);
        assertEquals("first", ref.newInstance("first").instance().publicField);
        assertEquals("second", ref.newInstance("second").instance().publicField);
        assertEquals(42, Reflection.on(PrimitiveTestClass.class).newInstance(42).instance().intValue);
    }

    @Test
    public void testGetAndSet() {
        TestClass target1 = new TestClass();
        TestClass target2 = new TestClass();
        Reflection.on(target1).get("publicField");
        Reflection.on(target1).set("publicField", "value1");
        Reflection.on(target2).set("publicField", "value2");
        assertEquals("value1", target1.publicField);
        assertEquals("value2", target2.publicField);

        TestClass instance = new TestClass("test value");
        Reflection<TestClass> ref = Reflection.on(instance);
        assertEquals("test value", ref.get("publicField"));
        ref.set("publicField", "v1").set("publicField", "v2");
        assertEquals("v2", instance.publicField);
        ref.set("publicField", null);
        assertNull(instance.publicField);
        assertThrows(RuntimeException.class, () -> ref.get("nonExistentField"));
        assertThrows(RuntimeException.class, () -> ref.set("nonExistentField", "value"));

        SubTestClass sub = new SubTestClass();
        assertEquals("publicValue", Reflection.on(sub).get("publicField"));
        assertEquals("subValue", Reflection.on(sub).get("subField"));
        assertEquals("privateValue", Reflection.on(sub).get("privateField"));
        Reflection.on(sub).set("privateField", "updated");
        assertEquals("updated", Reflection.on(sub).get("privateField"));

        PrimitiveTestClass primitive = new PrimitiveTestClass();
        Reflection<PrimitiveTestClass> pref = Reflection.on(primitive);
        Integer intValue = pref.get("intValue");
        assertEquals(10, intValue);
        pref.set("intValue", 20);
        assertEquals(20, primitive.intValue);
        Boolean boolValue = pref.get("boolValue");
        assertTrue(boolValue);
        pref.set("boolValue", false);
        assertFalse(primitive.boolValue);
        Double doubleValue = pref.get("doubleValue");
        assertEquals(3.14, doubleValue, 0.001);
        pref.set("doubleValue", 2.718);
        Double updated = pref.get("doubleValue");
        assertEquals(2.718, updated, 0.001);
    }

    @Test
    public void testGetAndSet_EdgeCase() {
        ShadowedFieldChild child = new ShadowedFieldChild();
        Reflection<ShadowedFieldChild> reflection = Reflection.on(child);
        assertEquals("child", reflection.get("value"));
        assertSame(reflection, reflection.set("value", "updated"));
        assertEquals("updated", child.value);
        assertEquals("parent", ((ShadowedFieldParent) child).value);

        ShadowedFieldParent parent = new ShadowedFieldParent();
        Reflection<ShadowedFieldParent> pref = Reflection.on(parent);
        pref.set("number", Integer.valueOf(7));
        pref.set("decimal", Float.valueOf(1.5f));
        assertEquals(7L, parent.number);
        assertEquals(1.5d, parent.decimal);
        pref.set("number", Character.valueOf('A'));
        assertEquals(65L, parent.number);
        assertThrows(RuntimeException.class, () -> pref.set("number", Double.valueOf(1.5d)));
        assertEquals(65L, parent.number);
        assertThrows(RuntimeException.class, () -> pref.set("number", null));
        assertEquals(65L, parent.number);
        pref.set("value", null);
        assertNull(parent.value);

        String original = StaticShadowedFieldChild.value;
        try {
            StaticShadowedFieldChild target = new StaticShadowedFieldChild();
            Reflection<StaticShadowedFieldChild> sref = Reflection.on(target);
            assertEquals(original, sref.get("value"));
            sref.set("value", "updated static");
            assertEquals("updated static", StaticShadowedFieldChild.value);
            assertEquals("parent", ((ShadowedFieldParent) target).value);
            assertEquals("updated static", Reflection.on(StaticShadowedFieldChild.class).get("value"));
        } finally {
            StaticShadowedFieldChild.value = original;
        }
    }

    @Test
    public void testInvoke() {
        TestClass target = new TestClass();
        Reflection<TestClass> ref = Reflection.on(target);
        Integer calculated = ref.invoke("calculate", 5, 3);
        assertEquals(8, calculated);
        assertEquals("default", ref.invoke("getPublicField"));
        assertEquals("test", ref.invoke("overloadedMethod", "test"));
        assertEquals("test123", ref.invoke("overloadedMethod", "test", 123));
        assertEquals("str:hello", ref.invoke("typed", "hello"));
        assertEquals("int:5", ref.invoke("typed", 5));
        assertEquals("Hello World", ref.invoke("concat", "Hello", " World"));
        assertNull(ref.invoke("voidMethod"));
        assertEquals("null World", ref.invoke("concat", null, " World"));
        assertEquals("null World", ref.invoke("concat", (Object) null, " World"));
        assertThrows(RuntimeException.class, () -> ref.invoke("nonExistentMethod"));
        for (int i = 0; i < 3; i++) {
            assertEquals("test" + i, ref.invoke("concat", "test", String.valueOf(i)));
            Integer looped = ref.invoke("calculate", i, 1);
            assertEquals(i + 1, looped);
        }

        assertEquals("charSequence", Reflection.on(new SpecificMethodTarget()).invoke("select", new StringBuilder("value")));
        RuntimeException ambiguous = assertThrows(RuntimeException.class, () -> Reflection.on(new AmbiguousMethodTarget()).invoke("select", "value"));
        assertTrue(ambiguous.getMessage().contains("Ambiguous method"));
        Integer inherited = Reflection.on(new SubTestClass()).invoke("calculate", 4, 6);
        assertEquals(10, inherited);
        assertEquals("sub:publicValue", Reflection.on(new SubTestClass()).invoke("getPublicField"));
        assertEquals("subValue", Reflection.on(new SubTestClass()).invoke("getSubField"));

        NullOverloadTarget nullTarget = new NullOverloadTarget();
        assertEquals("string", Reflection.on(nullTarget).invoke("set", (Object) null));
        assertNull(nullTarget.value);

        PrimitiveTestClass primitive = new PrimitiveTestClass();
        Integer product = Reflection.on(primitive).invoke("multiply", 4, 5);
        assertEquals(20, product);
        Boolean positive = Reflection.on(primitive).invoke("isPositive", 10);
        assertTrue(positive);
        Boolean negative = Reflection.on(primitive).invoke("isPositive", -5);
        assertFalse(negative);
    }

    @Test
    public void testInvoke_EdgeCase() {
        PrimitiveWideningTarget target = Reflection.on(PrimitiveWideningTarget.class).newInstance(42).instance();
        assertEquals(42L, target.constructedValue());
        Long widened = Reflection.on(target).invoke("longOnly", 7);
        assertEquals(7L, widened.longValue());
        assertEquals("int:7", Reflection.on(new PrimitiveWideningTarget(0)).invoke("select", 7));
        assertEquals("number", Reflection.on(new InvocationPhaseTarget()).invoke("select", 7));

        TestClass privateTarget = new TestClass();
        assertEquals("ab", Reflection.on(privateTarget).invoke("privateConcat", "a", "b"));
        assertEquals("xy", Reflection.on(new SubTestClass()).invoke("privateConcat", "x", "y"));
        Integer hash = Reflection.on(privateTarget).invoke("hashCode");
        assertEquals(privateTarget.hashCode(), hash);
        assertEquals(privateTarget.toString(), Reflection.on(privateTarget).invoke("toString"));
        assertEquals("Hello World", Reflection.on(new GreeterImpl()).invoke("greet", "World"));
        for (int i = 0; i < 3; i++) {
            assertEquals("a" + i, Reflection.on(privateTarget).invoke("privateConcat", "a", String.valueOf(i)));
        }
    }

    @Test
    public void testCall() {
        TestClass instance = new TestClass();
        Reflection<TestClass> ref = Reflection.on(instance);
        assertSame(ref, ref.call("voidMethod"));
        assertSame(ref, ref.call("setPublicField", "new value"));
        assertEquals("new value", instance.publicField);
        assertSame(ref, ref.call("setPublicField", "chained").call("voidMethod"));
        assertEquals("chained", instance.publicField);
        ref.call("setPublicField", "a").call("setPublicField", "b").call("setPublicField", "c");
        assertEquals("c", instance.publicField);
        assertThrows(RuntimeException.class, () -> ref.call("nonExistentMethod"));

        TestClass privateTarget = new TestClass();
        Reflection<TestClass> pref = Reflection.on(privateTarget);
        assertSame(pref, pref.call("privateSetField", "viaPrivate"));
        assertEquals("viaPrivate", privateTarget.publicField);
    }

    @Test
    public void testPairAndXViaReflection() {
        assertEquals("left", Reflection.on(Pair.class).newInstance("left", 2).invoke("left"));
        assertEquals("right", Reflection.on(Pair.class).newInstance("left", 2).call("setRight", "right").invoke("right"));
        assertNull(Reflection.on(Pair.class).newInstance("left", 2).call("setRight", new Object[] { null }).invoke("right"));
        assertEquals("right", Reflection.on(Pair.class).newInstance("left", 2).call("set", null, "right").invoke("right"));
        assertEquals(45L, X.m_11());
        assertEquals(49995000L, X.m_12());
        Long m01 = Reflection.on(X.class).newInstance().invoke("m_01");
        Long m02 = Reflection.on(X.class).newInstance().invoke("m_02");
        Long m11 = Reflection.on(X.class).newInstance().invoke("m_11");
        Long m12 = Reflection.on(X.class).newInstance().invoke("m_12");
        assertEquals(45L, m01);
        assertEquals(49995000L, m02);
        assertEquals(45L, m11);
        assertEquals(49995000L, m12);
        assertDoesNotThrow(() -> new X().m_01());
        assertDoesNotThrow(() -> new X().m_02());
    }

    /**
     * A {@code final} instance field is written through standard reflection, whether or not the optional
     * reflectasm jar happens to be on the classpath. ReflectASM's generated accessor cannot assign a final
     * field - its {@code putfield} is rejected with {@link IllegalAccessError} when that {@code putfield} is
     * resolved - and an {@code Error} escaped both the inner ReflectASM fallback and the outer {@code catch}
     * list, so merely having the optional dependency present broke a write the fallback performs correctly.
     */
    @Test
    public void reviewFixes20260911_setWritesAFinalInstanceFieldEvenWhenReflectAsmIsOnTheClasspath() throws Exception {
        final FinalFieldHolder holder = new FinalFieldHolder(new String("old"), 1);

        final Reflection<FinalFieldHolder> reflection = Reflection.on(holder);
        assertSame(reflection, reflection.set("text", "new"));
        assertSame(reflection, reflection.set("number", 7));

        // Read back reflectively: a final field read can otherwise be folded away.
        assertEquals("new", FinalFieldHolder.class.getDeclaredField("text").get(holder));
        assertEquals(7, FinalFieldHolder.class.getDeclaredField("number").get(holder));
        assertEquals("new", reflection.get("text"));
        assertEquals(7, (Integer) reflection.get("number"));
    }

    /**
     * Pins the varargs contract documented on {@code invoke}/{@code call}/{@code newInstance}: a variable-arity
     * member is matched on its DECLARED arity only, exactly as plain {@code java.lang.reflect} does. Spreading
     * the elements - or omitting them - never matches, so the trailing array must be passed explicitly and cast.
     */
    @Test
    public void reviewFixes20260911_varargsMembersAreMatchedOnTheirDeclaredArityOnly() {
        final VarargsTarget target = new VarargsTarget();
        final Reflection<VarargsTarget> reflection = Reflection.on(target);

        assertThrows(RuntimeException.class, () -> reflection.invoke("va", "a", "b"));
        assertThrows(RuntimeException.class, () -> reflection.invoke("va", "a"));
        assertThrows(RuntimeException.class, () -> reflection.invoke("va"));
        assertThrows(RuntimeException.class, () -> reflection.call("va", "a", "b"));

        // The documented form: the trailing array, cast so it is not spread.
        assertEquals("va n=2", reflection.invoke("va", (Object) new String[] { "a", "b" }));
        assertEquals("va n=0", reflection.invoke("va", (Object) new String[0]));

        // Control: a fixed-arity method is matched by spread arguments as usual.
        assertEquals("plain", reflection.invoke("plain", "a", "b"));

        assertThrows(RuntimeException.class, () -> Reflection.on(VarargsTarget.class).newInstance("a", "b"));
        assertNotNull(Reflection.on(VarargsTarget.class).newInstance((Object) new String[] { "a", "b" }).instance());
    }

    /**
     * Pins the {@code @throws} note on {@code get}/{@code set}/{@code invoke}: a {@code Reflection} created from
     * a {@code Class} has no target instance, so a non-static member fails with a {@link NullPointerException}
     * raised by the JVM rather than a wrapped reflective exception. Static members still work.
     */
    @Test
    public void reviewFixes20260911_nonStaticMembersOnAClassReflectionFailWithNullPointerException() {
        final Reflection<NoStaticMembers> reflection = Reflection.on(NoStaticMembers.class);

        assertNull(reflection.instance());
        assertThrows(NullPointerException.class, () -> reflection.get("instanceField"));
        assertThrows(NullPointerException.class, () -> reflection.set("instanceField", "x"));
        assertThrows(NullPointerException.class, () -> reflection.invoke("instanceMethod"));

        // Control: a static method needs no instance.
        assertEquals("staticMethod", reflection.invoke("staticMethod"));
    }

    public static class MemoHolder {
        // Not a compile-time constant, so a read is never folded away.
        public final String text;

        public String plain = "plain";

        MemoHolder(final String text) {
            this.text = text;
        }
    }

    /**
     * A field whose ReflectASM accessor fails to link is remembered, so the fast path is skipped for it from
     * then on. The JVM re-raises the accessor's resolution failure on every execution and {@link ClassValue}
     * caches nothing for a {@code computeValue} that threw, so the uncached fallback cost ~6.2us per access -
     * about 200x the fast path and ~400x the same read with the optional jar merely absent - forever, with no
     * log line to explain it. Only a linkage failure is memoized: ReflectASM rejecting a field outright (a
     * static or unshadowed private field) happens before any access and is cheap to repeat.
     */
    @Test
    public void reviewFixes20260911_aFieldWhoseAsmAccessorFailsToLinkIsMemoizedInsteadOfRetriedForever() throws Exception {
        final MemoHolder holder = new MemoHolder(new String("old"));
        final Reflection<MemoHolder> reflection = Reflection.on(holder);

        // Assigning a final field always defeats the generated accessor, so this takes the fallback.
        assertSame(reflection, reflection.set("text", "new"));
        assertEquals("new", MemoHolder.class.getDeclaredField("text").get(holder));

        // ... and the linkage failure is now recorded, so later accesses never re-enter ReflectASM for it.
        // Guarded by isReflectASMAvailable so the test means the same thing without the optional jar.
        assertEquals(Reflection.isReflectASMAvailable, Reflection.asmUnreachableFields.get(MemoHolder.class).contains("text"));

        // The cached path must still produce exactly the same answers.
        reflection.set("text", "newer");
        assertEquals("newer", MemoHolder.class.getDeclaredField("text").get(holder));
        assertEquals("newer", reflection.get("text"));

        // A field whose accessor links is never recorded, so the fast path keeps its cost.
        reflection.set("plain", "written");
        assertEquals("written", reflection.get("plain"));
        assertFalse(Reflection.asmUnreachableFields.get(MemoHolder.class).contains("plain"));
    }

    public static class VarargsOnlyTarget {
        public VarargsOnlyTarget(final String... parts) {
        }
    }

    public static class StaticFieldHolder {
        public static String staticField = "initial";

        // Not a compile-time constant, so a read is never folded away.
        public static final String STATIC_FINAL = String.valueOf("constant");
    }

    /**
     * Fills the two branches the varargs and {@code NullPointerException} javadoc paragraphs claim but their
     * own tests left uncovered, and pins the {@code final}-field sentence on {@code set}.
     * <ul>
     *   <li>"neither does omitting them" for a CONSTRUCTOR needs a class whose only constructor is varargs -
     *       {@code VarargsTarget} declares an explicit no-arg one, so {@code newInstance()} resolved that.</li>
     *   <li>"the named FIELD is not static" implies a static field works through a {@code Class} reflection;
     *       the existing control only covers a static METHOD.</li>
     *   <li>{@code set} writes a {@code final} instance field but not a {@code static final} one.</li>
     * </ul>
     * Green on both sides of the change by design - all three claims already held; only their coverage was
     * missing.
     */
    @Test
    public void reviewFixes20260911_varargsOnlyConstructorsAndStaticFieldAccessOnAClassReflection() throws Exception {
        // No constructor matches, including the omitted-argument form.
        assertThrows(RuntimeException.class, () -> Reflection.on(VarargsOnlyTarget.class).newInstance());
        assertThrows(RuntimeException.class, () -> Reflection.on(VarargsOnlyTarget.class).newInstance("a"));
        assertThrows(RuntimeException.class, () -> Reflection.on(VarargsOnlyTarget.class).newInstance("a", "b"));

        // ... and the documented form still works.
        assertNotNull(Reflection.on(VarargsOnlyTarget.class).newInstance((Object) new String[] { "a", "b" }).instance());

        final Reflection<StaticFieldHolder> reflection = Reflection.on(StaticFieldHolder.class);

        try {
            assertNull(reflection.instance());
            assertEquals("initial", reflection.get("staticField"));
            assertSame(reflection, reflection.set("staticField", "updated"));
            assertEquals("updated", reflection.get("staticField"));
            assertEquals("updated", StaticFieldHolder.class.getDeclaredField("staticField").get(null));

            // A static final field is writable by neither ReflectASM nor standard reflection.
            assertThrows(RuntimeException.class, () -> reflection.set("STATIC_FINAL", "x"));
            assertEquals("constant", reflection.get("STATIC_FINAL"));
        } finally {
            StaticFieldHolder.staticField = "initial";
        }
    }

    /**
     * A ReflectASM class that cannot be loaded must disable the optional fast path, not kill {@code Reflection}.
     * {@code ClassUtil.forName} catches only {@code ClassNotFoundException}, so a {@link LinkageError} from the
     * availability probe escaped its {@code catch (Exception)} and left {@code Reflection.<clinit>} erroneous:
     * every later touch of the class - including {@code newInstance()}, which uses no ReflectASM at all - then
     * failed with {@code NoClassDefFoundError: Could not initialize class}. Module opens are fixed at JVM start
     * and a class-loading verdict is cached for the life of the JVM, so this has to be observed in a child JVM;
     * the corrupt stub shadows the first name the probe resolves, with or without the real jar present.
     */
    @Test
    public void reviewFixes20260911_anUnloadableReflectAsmClassDisablesTheFastPathInsteadOfKillingReflection() throws Exception {
        final java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("abacus-reflectasm-probe");

        try {
            final java.nio.file.Path pkg = dir.resolve("com/esotericsoftware/reflectasm");
            java.nio.file.Files.createDirectories(pkg);

            // Deliberately not a class file: loading it raises ClassFormatError, a LinkageError.
            java.nio.file.Files.write(pkg.resolve("ConstructorAccess.class"), "not a class file".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            final String classpath = dir + java.io.File.pathSeparator + codeSourceOf(Reflection.class) + java.io.File.pathSeparator
                    + codeSourceOf(ReflectionTest.class) + java.io.File.pathSeparator + System.getProperty("java.class.path");

            final Process process = new ProcessBuilder(java.nio.file.Paths.get(System.getProperty("java.home"), "bin", "java").toString(), "-cp", classpath,
                    ReflectAsmProbe.class.getName()).redirectErrorStream(true).start();

            final String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(process.waitFor(2, java.util.concurrent.TimeUnit.MINUTES), "the probe JVM must finish");

            assertTrue(output.contains(ReflectAsmProbe.EXPECTED), "the probe JVM must report " + ReflectAsmProbe.EXPECTED + " but said:\n" + output);
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static String codeSourceOf(final Class<?> cls) throws Exception {
        return new java.io.File(cls.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
    }

    /**
     * Child-JVM entry point for
     * {@link #reviewFixes20260911_anUnloadableReflectAsmClassDisablesTheFastPathInsteadOfKillingReflection()}.
     * Deliberately a nested class with no JUnit involvement, so the child loads nothing but {@code Reflection}
     * and its own fixture.
     */
    public static final class ReflectAsmProbe {
        static final String EXPECTED = "PROBE available=false get=default newInstance=ok";

        public static void main(final String[] args) {
            final String got = Reflection.on(new TestClass()).get("publicField");
            final Object created = Reflection.on(TestClass.class).newInstance().instance();

            System.out.println(
                    "PROBE available=" + Reflection.isReflectASMAvailable + " get=" + got + " newInstance=" + (created instanceof TestClass ? "ok" : "FAILED"));
        }
    }
}
