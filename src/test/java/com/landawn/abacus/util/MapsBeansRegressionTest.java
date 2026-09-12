package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the iterative {@code Maps}/{@code Beans} review of 2026-09-01 (2nd pass of the day).
 * Ledger: {@code scripts/cross_review/Maps_Beans_ledger_2026-09-01b.md}.
 *
 * <p><b>Cycle 1</b></p>
 * <ul>
 *   <li><b>C-001</b> - a getter/setter pair split across a class and its superclass was not a property at all.</li>
 *   <li><b>C-002</b> - {@code Beans}' per-class caches pinned the inspected class's {@code ClassLoader}.</li>
 *   <li><b>C-003/C-004</b> - a {@code null} merge result in {@code replaceKeys}/{@code zip} is not a durable removal.</li>
 *   <li><b>C-005</b> - a builder's two setter pools disagreed after the builder was introspected on its own.</li>
 *   <li><b>C-006</b> - the selection-based conversions invoked a getter once per accepted spelling.</li>
 *   <li><b>C-007</b> - flat vs deep selection of an all-{@code null} nested bean.</li>
 *   <li><b>C-008</b> - {@code removeIf*} decides every match before removing anything.</li>
 *   <li><b>C-013</b> - an empty-string map key <i>is</i> reachable as an index segment's prefix.</li>
 *   <li><b>C-014/C-016</b> - {@code unflatten(null)}; {@code getAsChar}'s error message.</li>
 * </ul>
 *
 * <p><b>Cycle 2</b></p>
 * <ul>
 *   <li><b>C-021</b> - a {@code Character} value is not a code point for the numeric accessors.</li>
 *   <li><b>C-022</b> - {@code getAsChar} is deliberately more lenient than {@code getAs(.., Character.class)}.</li>
 *   <li><b>C-023</b> - {@code registerNonBeanClass} must outrank {@code registerPropertyAccessor}.</li>
 *   <li><b>C-024</b> - the "not a bean class" failure must name the rule that rejected the class.</li>
 * </ul>
 *
 * <p><b>Cycle 3</b></p>
 * <ul>
 *   <li><b>C-025</b> - a {@code null} {@code Class}/{@code Type} argument is an {@link IllegalArgumentException},
 *       plus a class-wide guard that no public method answers a {@code null} argument with a bare
 *       {@link NullPointerException}.</li>
 * </ul>
 */
public class MapsBeansRegressionTest extends TestBase {

    // ---------------------------------------------------------------------------------------------------
    // B1 - getter and setter declared by different classes in one hierarchy
    // ---------------------------------------------------------------------------------------------------

    /** Read-only base; on its own it has no writable property. */
    public static class SplitBase {
        protected String name;

        public String getName() {
            return name;
        }
    }

    /** Adds the mutator the base lacks. {@code name} is a property of this class, not of {@link SplitBase}. */
    public static class SplitSub extends SplitBase {
        private int age;

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }

    /** Write-only base. */
    public static class SplitBase2 {
        protected String name;

        public void setName(final String name) {
            this.name = name;
        }
    }

    /** Adds the accessor the base lacks. */
    public static class SplitSub2 extends SplitBase2 {
        private int age;

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }

    /** The setter is package-private: only the declared-method fallback can see it. */
    public static class NonPublicSetterBean {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }

    @Test
    public void testB1_getterInSuperclassSetterInSubclassIsAProperty() {
        assertTrue(Beans.getPropNameList(SplitSub.class).contains("name"));
        assertTrue(Beans.getPropNameList(SplitSub.class).contains("age"));

        assertNotNull(Beans.getPropGetter(SplitSub.class, "name"));
        assertNotNull(Beans.getPropSetter(SplitSub.class, "name"));
        assertEquals("getName", Beans.getPropGetter(SplitSub.class, "name").getName());
        assertEquals("setName", Beans.getPropSetter(SplitSub.class, "name").getName());
    }

    @Test
    public void testB1_setterInSuperclassGetterInSubclassIsAProperty() {
        assertTrue(Beans.getPropNameList(SplitSub2.class).contains("name"));
        assertNotNull(Beans.getPropGetter(SplitSub2.class, "name"));
        assertNotNull(Beans.getPropSetter(SplitSub2.class, "name"));
    }

    @Test
    public void testB1_splitAccessorPropertyRoundTripsThroughTheConversionFamily() {
        final SplitSub bean = new SplitSub();
        bean.setName("hello");
        bean.setAge(7);

        final Map<String, Object> map = Beans.beanToMap(bean);
        assertEquals("hello", map.get("name"));
        assertEquals(7, map.get("age"));

        final SplitSub copy = Beans.copy(bean);
        assertEquals("hello", copy.getName());

        final SplitSub fromMap = Beans.mapToBean(CommonUtil.asMap("name", "hi", "age", 3), SplitSub.class);
        assertEquals("hi", fromMap.getName());
        assertEquals(3, fromMap.getAge());

        final SplitSub target = new SplitSub();
        Beans.mergeInto(bean, target);
        assertEquals("hello", target.getName());
    }

    /**
     * The base classes must be unaffected: widening the setter search to the whole hierarchy must not make a
     * getter-only (or setter-only) class writable when it is introspected in its own right.
     */
    @Test
    public void testB1_baseClassesKeepTheirOwnNarrowerModel() {
        assertFalse(Beans.getPropNameList(SplitBase.class).contains("name"));
        assertFalse(Beans.getPropNameList(SplitBase2.class).contains("name"));
    }

    /** Getter in the base, setter in the subclass spelled with different case - resolved via the declared probe. */
    public static class CaseInsensitiveSetterBase {
        protected String foo;

        public String getFoo() {
            return foo;
        }
    }

    public static class CaseInsensitiveSetterSub extends CaseInsensitiveSetterBase {
        private int ok;

        public void setfoo(final String foo) { // deliberately lower-case 'f'
            this.foo = foo;
        }

        public int getOk() {
            return ok;
        }

        public void setOk(final int ok) {
            this.ok = ok;
        }
    }

    /** A static "setter" would be invoked with the bean discarded. Pinning the pre-existing answer, not changing it. */
    public static class StaticSetterBean {
        private String foo;
        private int ok;

        public String getFoo() {
            return foo;
        }

        public static void setFoo(final String foo) { // NOSONAR - static on purpose
            // no-op
        }

        public int getOk() {
            return ok;
        }

        public void setOk(final int ok) {
            this.ok = ok;
        }
    }

    /** The widened search resolves a split pair whose names differ only by case, too. */
    @Test
    public void testB1_splitAccessorsAreMatchedCaseInsensitively() {
        assertTrue(Beans.getPropNameList(CaseInsensitiveSetterSub.class).contains("foo"));

        final Method setter = Beans.getPropSetter(CaseInsensitiveSetterSub.class, "foo");
        assertNotNull(setter);
        assertEquals("setfoo", setter.getName());
    }

    /**
     * Pins pre-existing behaviour that the B1 fix deliberately did <b>not</b> change: a {@code static} setter
     * declared in the getter's own class is still paired with it. The new hierarchy-wide sweep skips statics,
     * but the declared-method fallback does not, so the answer is the same before and after. Whether a static
     * setter should be a property at all is a separate API decision.
     */
    @Test
    public void testB1_staticSetterBehaviourIsUnchanged() {
        assertTrue(Beans.getPropNameList(StaticSetterBean.class).contains("foo"));

        final Method setter = Beans.getPropSetter(StaticSetterBean.class, "foo");
        assertNotNull(setter);
        assertTrue(java.lang.reflect.Modifier.isStatic(setter.getModifiers()));
    }

    /** The declared-method fallback must survive: {@code getMethods()} does not report a non-public setter. */
    @Test
    public void testB1_nonPublicSetterIsStillFound() {
        assertTrue(Beans.getPropNameList(NonPublicSetterBean.class).contains("name"));

        final Method setter = Beans.getPropSetter(NonPublicSetterBean.class, "name");
        assertNotNull(setter);
        assertEquals("setName", setter.getName());

        final NonPublicSetterBean bean = new NonPublicSetterBean();
        Beans.setPropValue(bean, "name", "v", false);
        assertEquals("v", bean.getName());
    }

    // ---------------------------------------------------------------------------------------------------
    // B2 - a per-class cache entry must not outlive the class it describes
    // ---------------------------------------------------------------------------------------------------

    /**
     * Loads a throwaway class in its own {@link URLClassLoader}, hands it to {@code touch}, then drops every
     * reference and reports whether the loader became phantom-reachable.
     *
     * <p>The class is defined from bytes rather than loaded from disk so the test carries no build-layout
     * assumptions. The loader has a {@code null} parent and delegates everything except the throwaway class to
     * the test's own loader, so only the throwaway class itself is loader-private.</p>
     */
    private static boolean loaderIsCollectedAfter(final java.util.function.Consumer<Class<?>> touch) throws Exception {
        final byte[] bytes = throwawayBeanClassBytes();

        @SuppressWarnings("resource")
        URLClassLoader loader = new URLClassLoader(new URL[0], null) {
            @Override
            protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                if (THROWAWAY_CLASS_NAME.equals(name)) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> c = findLoadedClass(name);

                        if (c == null) {
                            c = defineClass(name, bytes, 0, bytes.length);
                        }

                        if (resolve) {
                            resolveClass(c);
                        }

                        return c;
                    }
                }

                return MapsBeansRegressionTest.class.getClassLoader().loadClass(name);
            }
        };

        Class<?> cls = loader.loadClass(THROWAWAY_CLASS_NAME);
        touch.accept(cls);

        final ReferenceQueue<Object> queue = new ReferenceQueue<>();
        final PhantomReference<Object> ref = new PhantomReference<>(loader, queue);

        loader.close();
        loader = null; // NOSONAR - dropping the last strong reference is the point of the test
        cls = null; // NOSONAR

        try {
            for (int i = 0; i < 60; i++) {
                System.gc();
                Thread.sleep(25);

                if (queue.poll() != null) {
                    return true;
                }
            }

            return false;
        } finally {
            Reference.reachabilityFence(ref);
        }
    }

    private static final String THROWAWAY_CLASS_NAME = "com.landawn.abacus.util.gen.ThrowawayBean";

    /**
     * The class file for {@code public class ThrowawayBean { public String name; }} - a bare public field is
     * enough to make it a bean, and no accessors keeps the bytes hand-writable.
     */
    private static byte[] throwawayBeanClassBytes() {
        // Constant pool: 1 Class(this) 2 Utf8(name of this) 3 Class(Object) 4 Utf8(java/lang/Object)
        // 5 Utf8("name") 6 Utf8("Ljava/lang/String;") 7 Utf8("<init>") 8 Utf8("()V") 9 Utf8("Code")
        // 10 NameAndType(<init>:()V) 11 Methodref(Object.<init>)
        final String internalName = THROWAWAY_CLASS_NAME.replace('.', '/');
        final java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();

        try (java.io.DataOutputStream out = new java.io.DataOutputStream(bout)) {
            out.writeInt(0xCAFEBABE);
            out.writeShort(0); // minor
            out.writeShort(52); // major - Java 8, readable by every supported JDK
            out.writeShort(12); // constant_pool_count = entries + 1

            out.writeByte(7); // 1: CONSTANT_Class
            out.writeShort(2);
            out.writeByte(1); // 2: CONSTANT_Utf8
            out.writeUTF(internalName);
            out.writeByte(7); // 3: CONSTANT_Class
            out.writeShort(4);
            out.writeByte(1); // 4
            out.writeUTF("java/lang/Object");
            out.writeByte(1); // 5
            out.writeUTF("name");
            out.writeByte(1); // 6
            out.writeUTF("Ljava/lang/String;");
            out.writeByte(1); // 7
            out.writeUTF("<init>");
            out.writeByte(1); // 8
            out.writeUTF("()V");
            out.writeByte(1); // 9
            out.writeUTF("Code");
            out.writeByte(12); // 10: CONSTANT_NameAndType
            out.writeShort(7);
            out.writeShort(8);
            out.writeByte(10); // 11: CONSTANT_Methodref
            out.writeShort(3);
            out.writeShort(10);

            out.writeShort(0x0021); // ACC_PUBLIC | ACC_SUPER
            out.writeShort(1); // this_class
            out.writeShort(3); // super_class
            out.writeShort(0); // interfaces_count

            out.writeShort(1); // fields_count
            out.writeShort(0x0001); // ACC_PUBLIC
            out.writeShort(5); // name_index
            out.writeShort(6); // descriptor_index
            out.writeShort(0); // attributes_count

            out.writeShort(1); // methods_count
            out.writeShort(0x0001); // ACC_PUBLIC
            out.writeShort(7); // <init>
            out.writeShort(8); // ()V
            out.writeShort(1); // attributes_count
            out.writeShort(9); // "Code"
            out.writeInt(17); // attribute_length
            out.writeShort(1); // max_stack
            out.writeShort(1); // max_locals
            out.writeInt(5); // code_length
            out.writeByte(0x2A); // aload_0
            out.writeByte(0xB7); // invokespecial
            out.writeShort(11); // Object.<init>
            out.writeByte(0xB1); // return
            out.writeShort(0); // exception_table_length
            out.writeShort(0); // attributes_count

            out.writeShort(0); // class attributes_count
        } catch (final java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }

        return bout.toByteArray();
    }

    /** Sanity check: without any {@code Beans} call the loader is collectable, so the harness itself is sound. */
    @Test
    public void testB2_baselineLoaderIsCollectable() throws Exception {
        assertTrue(loaderIsCollectedAfter(cls -> {
        }), "the probe harness itself retains the loader; the leak assertions below would be meaningless");
    }

    /**
     * {@code isRecordClass} and {@code getBuilderInfo} answer entirely out of {@code Beans}' own per-class
     * caches, so they are the end-to-end check that those caches no longer pin the class.
     *
     * <p>The heavier entry points ({@code isBeanClass}, {@code getPropNameList}, ...) additionally populate the
     * {@code Class}-keyed pools in {@code ClassUtil} and {@code ParserUtil}, which still retain; see the class
     * documentation of {@link Beans}.</p>
     */
    @Test
    public void testB2_perClassCachesDoNotPinTheClassLoader() throws Exception {
        assertTrue(loaderIsCollectedAfter(Beans::isRecordClass), "recordClassPool pinned the ClassLoader");
        assertTrue(loaderIsCollectedAfter(Beans::getBuilderInfo), "builderMap pinned the ClassLoader");
    }

    // ---------------------------------------------------------------------------------------------------
    // B3 / J9 - a null merge result removes the current mapping but is not a durable "drop this key"
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testB3_replaceKeysNullMergeIsNotFinalWhenALaterEntryCollides() {
        final Map<String, Integer> two = new LinkedHashMap<>();
        two.put("a1", 1);
        two.put("a2", 2);
        Maps.replaceKeys(two, k -> "a", (existing, incoming) -> null);
        assertTrue(two.isEmpty(), "the last collision resolved to null, so the key is gone");

        final Map<String, Integer> three = new LinkedHashMap<>();
        three.put("a1", 1);
        three.put("a2", 2);
        three.put("a3", 3);
        Maps.replaceKeys(three, k -> "a", (existing, incoming) -> existing == 1 ? null : existing + incoming);

        // The (1, 2) merge removed "a"; the third entry then found it absent and was stored directly, without
        // the merge function being consulted. This is Map.merge's stepwise behaviour, and is now documented.
        assertEquals(1, three.size());
        assertEquals(3, three.get("a"));
    }

    @Test
    public void testB3_zipNullMergeIsNotFinalWhenALaterElementCollides() {
        assertTrue(Maps.zip(Arrays.asList("a", "a"), Arrays.asList(1, 2), (v1, v2) -> null, IntFunctions.ofMap()).isEmpty());

        final Map<String, Integer> back = Maps.zip(Arrays.asList("a", "a", "a"), Arrays.asList(1, 2, 3), (v1, v2) -> null, IntFunctions.ofMap());
        assertEquals(1, back.size());
        assertEquals(3, back.get("a"));
    }

    // ---------------------------------------------------------------------------------------------------
    // D1 - one getter invocation per selected property, not per accepted spelling
    // ---------------------------------------------------------------------------------------------------

    public static class CountingBean {
        static int firstNameReads = 0;

        private String firstName;
        private CountingAddress address;

        public String getFirstName() {
            firstNameReads++;
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public CountingAddress getAddress() {
            return address;
        }

        public void setAddress(final CountingAddress address) {
            this.address = address;
        }
    }

    public static class CountingAddress {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    private static CountingBean newCountingBean() {
        final CountingBean bean = new CountingBean();
        bean.setFirstName("John");
        bean.setAddress(new CountingAddress());
        CountingBean.firstNameReads = 0;
        return bean;
    }

    @Test
    public void testD1_shallowSelectionReadsEachGetterOnce() {
        final CountingBean bean = newCountingBean();
        final Map<String, Object> map = Beans.beanToMap(bean, Arrays.asList("firstName", "first_name", "FirstName"));

        assertEquals(1, map.size());
        assertEquals("John", map.get("firstName"));
        assertEquals(1, CountingBean.firstNameReads, "three spellings of one property must read the getter once");
    }

    @Test
    public void testD1_deepAndFlatSelectionReadEachGetterOnce() {
        final CountingBean deepBean = newCountingBean();
        final Map<String, Object> deep = Beans.deepBeanToMap(deepBean, Arrays.asList("firstName", "first_name"));
        assertEquals(1, deep.size());
        assertEquals(1, CountingBean.firstNameReads);

        final CountingBean flatBean = newCountingBean();
        final Map<String, Object> flat = Beans.beanToFlatMap(flatBean, Arrays.asList("firstName", "first_name"));
        assertEquals(1, flat.size());
        assertEquals(1, CountingBean.firstNameReads);
    }

    /** De-duplication must not change which entries are produced, only how often the getters run. */
    @Test
    public void testD1_deduplicationPreservesSelectionOrderAndContent() {
        final CountingBean bean = newCountingBean();
        final Map<String, Object> map = Beans.beanToMap(bean, Arrays.asList("address", "firstName", "first_name"));

        assertEquals(Arrays.asList("address", "firstName"), new ArrayList<>(map.keySet()));
    }

    // ---------------------------------------------------------------------------------------------------
    // D2 - the documented flat/deep difference for an all-null nested bean
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testD2_flatSelectionDropsAnAllNullNestedBeanWhileDeepKeepsAnEmptyMap() {
        final CountingBean bean = newCountingBean(); // address is non-null, address.city is null

        assertTrue(Beans.beanToFlatMap(bean, Arrays.asList("address")).isEmpty());
        assertEquals(CommonUtil.asMap("address", new LinkedHashMap<>()), Beans.deepBeanToMap(bean, Arrays.asList("address")));

        // The builder carries the caller's null policy into the nested level, so the property always appears.
        assertEquals(CommonUtil.asMap("address.city", null), Beans.mapBuilder(bean).flat().select("address").toMap());
    }

    // ---------------------------------------------------------------------------------------------------
    // D3 - removeIf* decides every match before removing anything
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testD3_removeIfLeavesTheMapUntouchedWhenTheFilterThrows() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        assertThrows(IllegalStateException.class, () -> Maps.removeIf(map, (k, v) -> {
            if ("c".equals(k)) {
                throw new IllegalStateException("boom");
            }
            return true;
        }));

        assertEquals(3, map.size(), "a throwing filter must not leave the map half-emptied");
    }

    @Test
    public void testD3_removeIfOnAnUnmodifiableMapOnlyThrowsWhenSomethingMatches() {
        final Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>(CommonUtil.asMap("a", 1)));

        assertFalse(Maps.removeIf(unmodifiable, (k, v) -> false));
        assertFalse(Maps.removeIfKey(unmodifiable, k -> false));
        assertFalse(Maps.removeIfValue(unmodifiable, v -> false));

        assertThrows(UnsupportedOperationException.class, () -> Maps.removeIf(unmodifiable, (k, v) -> true));
    }

    // ---------------------------------------------------------------------------------------------------
    // D5 - a builder's declared and lookup setter pools must agree
    // ---------------------------------------------------------------------------------------------------

    public static class BuiltBean {
        private final String alpha;

        private BuiltBean(final String alpha) {
            this.alpha = alpha;
        }

        public String getAlpha() {
            return alpha;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String alpha;

            public Builder alpha(final String alpha) {
                this.alpha = alpha;
                return this;
            }

            public String getAlpha() {
                return alpha;
            }

            public BuiltBean build() {
                return new BuiltBean(alpha);
            }
        }
    }

    @Test
    public void testD5_builderSetterPoolsStayConsistentAfterTheBuilderIsIntrospected() {
        // Introspecting the bean publishes its builder's setters under the bean's property names.
        Beans.getPropNameList(BuiltBean.class);
        assertTrue(Beans.getPropSetters(BuiltBean.Builder.class).containsKey("alpha"));
        assertNotNull(Beans.getPropSetter(BuiltBean.Builder.class, "alpha"));

        // Introspecting the builder in its own right used to overwrite one pool and merge into the other,
        // leaving getPropSetters(builder) empty while getPropSetter(builder, "alpha") still resolved.
        Beans.getPropNameList(BuiltBean.Builder.class);

        assertTrue(Beans.getPropSetters(BuiltBean.Builder.class).containsKey("alpha"), "the declared pool lost the builder setter");
        assertNotNull(Beans.getPropSetter(BuiltBean.Builder.class, "alpha"), "the lookup pool lost the builder setter");
        assertSame(Beans.getPropSetters(BuiltBean.Builder.class).get("alpha"), Beans.getPropSetter(BuiltBean.Builder.class, "alpha"));
    }

    @Test
    public void testD5_builderBasedBeanStillRoundTrips() {
        final BuiltBean bean = Beans.mapToBean(CommonUtil.asMap("alpha", "v"), BuiltBean.class);
        assertEquals("v", bean.getAlpha());
    }

    /** A second builder-based bean, introspected builder-first, to pin the opposite publication order. */
    public static class BuiltBean2 {
        private final String beta;

        private BuiltBean2(final String beta) {
            this.beta = beta;
        }

        public String getBeta() {
            return beta;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String beta;

            public Builder beta(final String beta) {
                this.beta = beta;
                return this;
            }

            public String getBeta() {
                return beta;
            }

            public BuiltBean2 build() {
                return new BuiltBean2(beta);
            }
        }
    }

    /** The builder's setter model must not depend on whether the bean or the builder is introspected first. */
    @Test
    public void testD5_builderSetterPoolsAreIndependentOfIntrospectionOrder() {
        Beans.getPropNameList(BuiltBean2.Builder.class); // builder first, this time
        Beans.getPropNameList(BuiltBean2.class);

        assertTrue(Beans.getPropSetters(BuiltBean2.Builder.class).containsKey("beta"));
        assertNotNull(Beans.getPropSetter(BuiltBean2.Builder.class, "beta"));
        assertEquals("v", Beans.mapToBean(CommonUtil.asMap("beta", "v"), BuiltBean2.class).getBeta());
    }

    // ---------------------------------------------------------------------------------------------------
    // B2 - the ClassCache must behave like the ConcurrentCacheMap it replaced, including under concurrency
    // ---------------------------------------------------------------------------------------------------

    public static class ConcurrentScanBean {
        private String name;
        private int size;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }
    }

    /**
     * Many threads racing to introspect the same class must all end up with one consistent model. This is what
     * would break if {@code ClassValue} handed a different {@code Slot} to each racing caller, since a
     * publication into a discarded slot would be lost.
     */
    @Test
    public void testB2_concurrentIntrospectionConvergesOnOneModel() throws Exception {
        final int threads = 16;
        final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(threads);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        final java.util.Set<List<String>> models = Collections.synchronizedSet(new java.util.HashSet<>());

        for (int i = 0; i < threads; i++) {
            final Thread t = new Thread(() -> {
                try {
                    start.await();

                    for (int j = 0; j < 200; j++) {
                        models.add(new ArrayList<>(Beans.getPropNameList(ConcurrentScanBean.class)));
                        assertNotNull(Beans.getPropGetter(ConcurrentScanBean.class, "name"));
                        assertNotNull(Beans.getPropSetter(ConcurrentScanBean.class, "size"));
                        assertNotNull(Beans.getPropFields(ConcurrentScanBean.class).get("name"));
                    }
                } catch (final Throwable e) {
                    errors.add(e);
                } finally {
                    done.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        start.countDown();
        assertTrue(done.await(60, java.util.concurrent.TimeUnit.SECONDS), "concurrent introspection did not finish");
        assertTrue(errors.isEmpty(), () -> "concurrent introspection failed: " + errors);
        assertEquals(1, models.size(), "racing threads observed different property models: " + models);
        assertEquals(Arrays.asList("name", "size"), models.iterator().next());
    }

    public static class RegistrationBase {
        private String hidden;

        public String getHidden() {
            return hidden;
        }

        public void setHidden(final String hidden) {
            this.hidden = hidden;
        }
    }

    public static class RegistrationSub extends RegistrationBase {
        private String own;

        public String getOwn() {
            return own;
        }

        public void setOwn(final String own) {
            this.own = own;
        }
    }

    /**
     * A registration against a base type must still refresh the subtypes that were introspected before it. That
     * sweep used to enumerate {@code beanDeclaredPropGetMethodPool.keySet()}; a {@code ClassValue} cannot be
     * enumerated, so it now runs off a weak index, and this is the test that the index is actually maintained.
     */
    @Test
    public void testB2_registrationStillRefreshesAlreadyIntrospectedSubtypes() {
        assertTrue(Beans.getPropNameList(RegistrationBase.class).contains("hidden"));
        assertTrue(Beans.getPropNameList(RegistrationSub.class).contains("hidden"));

        Beans.registerNonPropertyAccessor(RegistrationBase.class, "hidden");

        assertFalse(Beans.getPropNameList(RegistrationBase.class).contains("hidden"));
        assertFalse(Beans.getPropNameList(RegistrationSub.class).contains("hidden"), "the already-introspected subtype was not refreshed");
        assertTrue(Beans.getPropNameList(RegistrationSub.class).contains("own"));
    }

    // ---------------------------------------------------------------------------------------------------
    // Javadoc-accuracy regressions
    // ---------------------------------------------------------------------------------------------------

    /** J3: an index segment's prefix may be empty, so {@code "[0]"} reaches the collection stored under "". */
    @Test
    public void testJ3_emptyKeyIsReachableAsAnIndexSegmentPrefix() {
        final Map<String, Object> map = new HashMap<>();
        map.put("", Arrays.asList("x", "y"));

        assertEquals("x", Maps.getByPath(map, "[0]"));
        assertEquals("y", Maps.getByPath(map, "[1]"));

        // ...but "" still cannot name an intermediate map: a leading empty segment is dropped, not matched.
        final Map<String, Object> nested = new HashMap<>();
        nested.put("", CommonUtil.asMap("a", 1));
        nested.put("a", 99);
        assertEquals(99, (Integer) Maps.getByPath(nested, ".a"));
    }

    /** J4: every {@code unflatten} overload tolerates a {@code null} map. */
    @Test
    public void testJ4_unflattenAcceptsANullMap() {
        assertTrue(Maps.unflatten(null).isEmpty());
        assertTrue(Maps.unflatten(null, IntFunctions.ofMap()).isEmpty());
        assertTrue(Maps.unflatten(null, ".", IntFunctions.ofLinkedHashMap()).isEmpty());
    }

    /** J6: a char accessor must not report a failure in terms of {@code Long}. */
    @Test
    public void testJ6_getAsCharErrorMessageNamesTheAccessorsOwnContract() {
        final Map<String, Object> map = new HashMap<>();
        map.put("flag", Boolean.TRUE);

        final NumberFormatException e = assertThrows(NumberFormatException.class, () -> Maps.getAsChar(map, "flag"));

        assertTrue(e.getMessage().contains("neither a single character nor a numeric UTF-16 code unit"), e.getMessage());
        assertFalse(e.getMessage().contains("Long"), "the message must not name the internal parse type: " + e.getMessage());
        assertNotNull(e.getCause(), "the parser's own failure is kept as the cause");
    }

    /** The single-character and numeric paths must keep working unchanged. */
    @Test
    public void testJ6_getAsCharStillParsesItsSupportedForms() {
        final Map<String, Object> map = new HashMap<>();
        map.put("one", "A");
        map.put("code", "65");
        map.put("hex", "0x41");
        map.put("num", 65);

        assertEquals('A', Maps.getAsChar(map, "one").get());
        assertEquals('A', Maps.getAsChar(map, "code").get());
        assertEquals('A', Maps.getAsChar(map, "hex").get());
        assertEquals('A', Maps.getAsChar(map, "num").get());

        // An out-of-range code unit is still an IllegalArgumentException, not the new NumberFormatException.
        map.put("neg", "-1");
        final IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "neg"));
        assertFalse(iae instanceof NumberFormatException);
    }

    // ---------------------------------------------------------------------------------------------------
    // Cycle 2 — C-023: registerNonBeanClass must outrank registerPropertyAccessor
    // ---------------------------------------------------------------------------------------------------

    public static class MoneyLike {
        private long cents;

        public long getCents() {
            return cents;
        }

        public void setCents(final long cents) {
            this.cents = cents;
        }

        /** Getter-only, so it is a property only once explicitly registered. */
        public long getAmount() {
            return cents;
        }
    }

    public static class PriceLike extends MoneyLike {
        private String currency;

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(final String currency) {
            this.currency = currency;
        }
    }

    /**
     * {@code registerNonBeanClass} documents itself as "inherited and absolute", with a worked example
     * asserting {@code getPropNameList} returns {@code []}. The registration overlay used to run regardless,
     * putting a registered accessor back after the scan had excluded it - so the class ended up with
     * {@code isBeanClass == false} and a non-empty property list at the same time, and its subclasses
     * inherited the phantom property.
     *
     * <p>Registrations are global and permanent, so this test uses classes no other test touches.</p>
     */
    @Test
    public void testC023_registerNonBeanClassOutranksARegisteredPropertyAccessor() throws Exception {
        assertTrue(Beans.isBeanClass(MoneyLike.class));
        assertEquals(Arrays.asList("cents"), new ArrayList<>(Beans.getPropNameList(MoneyLike.class)));
        assertEquals(Arrays.asList("cents", "currency"), new ArrayList<>(Beans.getPropNameList(PriceLike.class)));

        Beans.registerPropertyAccessor("amount", MoneyLike.class.getMethod("getAmount"));
        assertTrue(Beans.getPropNameList(MoneyLike.class).contains("amount"), "the registered accessor should be a property first");

        Beans.registerNonBeanClass(MoneyLike.class);

        assertFalse(Beans.isBeanClass(MoneyLike.class));
        assertTrue(Beans.getPropNameList(MoneyLike.class).isEmpty(), "a non-bean class must expose no properties, registered or not");

        // The subclass keeps only what it declares itself - it must not inherit the base's registered accessor.
        assertEquals(Arrays.asList("currency"), new ArrayList<>(Beans.getPropNameList(PriceLike.class)));

        // isBeanClass and the property list must agree, in both directions.
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(MoneyLike.class, "amount"));
    }

    // ---------------------------------------------------------------------------------------------------
    // Cycle 2 — C-024: the "not a bean class" failure must name the rule that rejected the class
    // ---------------------------------------------------------------------------------------------------

    @Test
    public void testC024_getIgnoredPropNamesForDiffNamesTheActualReason() {
        assertEquals("'cls' cannot be null", assertThrows(IllegalArgumentException.class, () -> Beans.getIgnoredPropNamesForDiff(null)).getMessage());

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Beans.getIgnoredPropNamesForDiff(String.class)).getMessage().contains("CharSequence"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Beans.getIgnoredPropNamesForDiff(Integer.class)).getMessage().contains("Number"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Beans.getIgnoredPropNamesForDiff(ArrayList.class)).getMessage().contains("Collection"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Beans.getIgnoredPropNamesForDiff(HashMap.class)).getMessage().contains("Map"));

        // A real bean still works, and the result is still cached.
        assertNotNull(Beans.getIgnoredPropNamesForDiff(SplitSub.class));
        assertSame(Beans.getIgnoredPropNamesForDiff(SplitSub.class), Beans.getIgnoredPropNamesForDiff(SplitSub.class));
    }

    // ---------------------------------------------------------------------------------------------------
    // Cycle 2 — C-021 / C-022: the documented (dis)agreement between the accessor families
    // ---------------------------------------------------------------------------------------------------

    /** A {@code Character} goes through its string form in the numeric accessors, and only `int` differs in getAs. */
    @Test
    public void testC021_characterValueIsNotACodePointForTheNumericAccessors() {
        final Map<String, Object> map = new HashMap<>();
        map.put("c", 'A');

        assertThrows(NumberFormatException.class, () -> Maps.getAsInt(map, "c"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsByte(map, "c"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsShort(map, "c"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsLong(map, "c"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsFloat(map, "c"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsDouble(map, "c"));

        // getAs uses N.convert, which special-cases Character -> Integer only. Pinned so the asymmetry is
        // noticed if N.convert ever changes.
        assertEquals(65, Maps.getAs(map, "c", Integer.class).orElseThrow());
        assertThrows(NumberFormatException.class, () -> Maps.getAs(map, "c", Long.class));

        // Where the two families are documented to agree, they do.
        final Map<String, Object> empty = new HashMap<>();
        empty.put("c", "");
        assertFalse(Maps.getAsInt(empty, "c").isPresent());
        assertFalse(Maps.getAs(empty, "c", Integer.class).isPresent());
    }

    /** {@code getAsChar} accepts spellings {@code getAs(.., Character.class)} rejects, and vice versa for "". */
    @Test
    public void testC022_getAsCharIsMoreLenientThanTheTypedAccessor() {
        final Map<String, Object> map = new HashMap<>();
        map.put("hex", "0x41");
        map.put("frac", 65.9d);
        map.put("empty", "");

        assertEquals('A', Maps.getAsChar(map, "hex").orElseThrow());
        assertThrows(NumberFormatException.class, () -> Maps.getAs(map, "hex", Character.class));

        assertEquals('A', Maps.getAsChar(map, "frac").orElseThrow());
        assertThrows(NumberFormatException.class, () -> Maps.getAs(map, "frac", Character.class));

        assertEquals('\0', Maps.getAsChar(map, "empty").orElseThrow());
        assertFalse(Maps.getAs(map, "empty", Character.class).isPresent());
    }

    /** Surrogates: a supplementary code point cannot be a {@code char}, and must fail rather than truncate. */
    @Test
    public void testC022_getAsCharRejectsSupplementaryCodePoints() {
        final Map<String, Object> map = new HashMap<>();
        map.put("emoji", "😀"); // U+1F600, two UTF-16 units
        map.put("cp", 0x1F600);

        assertThrows(NumberFormatException.class, () -> Maps.getAsChar(map, "emoji"));
        assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map, "cp"));

        // A single BMP character still works, including a non-ASCII one.
        map.put("umlaut", "ü");
        assertEquals('ü', Maps.getAsChar(map, "umlaut").orElseThrow());
    }

    // ---------------------------------------------------------------------------------------------------
    // Cycle 3 — C-025: a null Class/Type argument is an IllegalArgumentException, like every sibling
    // ---------------------------------------------------------------------------------------------------

    /**
     * These seven entry points used to let a {@code null} class reach reflection and surface as a raw
     * {@code NullPointerException} ("Cannot invoke \"java.lang.Class.getCanonicalName()\" because \"cls\" is
     * null"), even though their own javadoc documents {@link IllegalArgumentException} and every sibling in
     * the class - and all of {@code Maps} - rejects a null argument that way.
     */
    @Test
    public void testC025_aNullClassArgumentIsRejectedWithIllegalArgumentException() {
        final Map<String, Object> map = CommonUtil.asMap("name", "v");
        final List<Map<String, Object>> maps = Arrays.asList(map);

        assertEquals("'targetType' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, (Class<SplitSub>) null)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, true, (Class<SplitSub>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("name"), (Class<SplitSub>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(maps, (Class<SplitSub>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(maps, true, (Class<SplitSub>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(maps, Arrays.asList("name"), (Class<SplitSub>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newBean(null));
        assertEquals("'beanType' cannot be null", assertThrows(IllegalArgumentException.class, () -> Beans.getBeanInfo(null)).getMessage());

        // The null check must not disturb the valid path, including the "selectPropNames == null" delegation.
        assertEquals("v", Beans.mapToBean(map, SplitSub.class).getName());
        assertEquals("v", Beans.mapToBean(map, (Collection<String>) null, SplitSub.class).getName());
        assertEquals(1, Beans.mapsToBeans(maps, SplitSub.class).size());
        assertNotNull(Beans.newBean(SplitSub.class));
        assertNotNull(Beans.getBeanInfo(SplitSub.class));
    }

    /**
     * A guard against regressing the class-wide convention: no public static method of {@code Maps} or
     * {@code Beans} may answer a {@code null} argument with a bare {@code NullPointerException}, except the
     * accessors that documentedly dereference the bean itself.
     */
    @Test
    public void testC025_nullArgumentsNeverProduceAnUndocumentedNpe() throws Exception {
        // Methods whose javadoc states that a null bean/method is a NullPointerException.
        final java.util.Set<String> documentedNpe = CommonUtil.asSet("getPropNameByMethod", "getPropNames", "getPropValue", "getPropValueIfPresent",
                "setPropValue", "setPropValueByGetter");

        final List<String> offenders = new ArrayList<>();

        for (final Class<?> cls : Arrays.asList(Maps.class, Beans.class)) {
            for (final java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(m.getModifiers()) || !java.lang.reflect.Modifier.isStatic(m.getModifiers()) || m.isSynthetic()
                        || documentedNpe.contains(m.getName())) {
                    continue;
                }

                final Object[] args = new Object[m.getParameterCount()];

                for (int i = 0; i < args.length; i++) {
                    args[i] = defaultArg(m.getParameterTypes()[i]);
                }

                try {
                    m.invoke(null, args);
                } catch (final java.lang.reflect.InvocationTargetException e) {
                    if (e.getCause() instanceof NullPointerException) {
                        offenders.add(cls.getSimpleName() + "." + m.getName() + "/" + m.getParameterCount() + " -> " + e.getCause());
                    }
                }
            }
        }

        assertTrue(offenders.isEmpty(), () -> "null arguments produced an undocumented NullPointerException: " + offenders);
    }

    private static Object defaultArg(final Class<?> t) {
        if (!t.isPrimitive()) {
            return null;
        }
        if (t == boolean.class) {
            return false;
        }
        if (t == char.class) {
            return 'a';
        }
        if (t == byte.class) {
            return (byte) 0;
        }
        if (t == short.class) {
            return (short) 0;
        }
        if (t == int.class) {
            return 0;
        }
        if (t == long.class) {
            return 0L;
        }
        if (t == float.class) {
            return 0f;
        }
        return 0d;
    }

    /** O3 was a scoping cleanup; this pins the multi-index path it touched. */
    @Test
    public void testGetByPathStillWalksChainedIndexSegments() {
        final Map<String, Object> map = new HashMap<>();
        map.put("grid", Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d")));

        assertEquals("a", Maps.getByPath(map, "grid[0][0]"));
        assertEquals("d", Maps.getByPath(map, "grid[1][1]"));
        assertNull(Maps.getByPath(map, "grid[2][0]"));
        assertNull(Maps.getByPath(map, "grid[0]junk]"));

        final List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(CommonUtil.asMap("city", "NYC"));
        map.put("rows", rows);
        assertEquals("NYC", Maps.getByPath(map, "rows[0].city"));
    }
}
