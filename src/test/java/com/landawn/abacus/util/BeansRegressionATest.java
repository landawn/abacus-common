package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-30 {@link Beans} deep-review fixes. One nested section per ledger
 * finding; see scripts/cross_review/Beans_ledger_2026-08-30.md.
 */
public class BeansRegressionATest extends TestBase {

    // ------------------------------------------------------------------ C-001

    /** Its only property is excluded below, which must also drop the cached isBeanClass answer. */
    public static class C001OnlyOneProp {
        private String only;

        public String getOnly() {
            return only;
        }

        public void setOnly(final String only) {
            this.only = only;
        }
    }

    public static class C001TwoProps {
        private String kept;
        private String hidden;

        public String getKept() {
            return kept;
        }

        public void setKept(final String kept) {
            this.kept = kept;
        }

        public String getHidden() {
            return hidden;
        }

        public void setHidden(final String hidden) {
            this.hidden = hidden;
        }
    }

    @Test
    public void testC001_isBeanClassIsInvalidatedByRegisterNonPropertyAccessor() {
        // Warm both caches, then remove the only property. isBeanClass used to keep answering true.
        assertTrue(Beans.isBeanClass(C001OnlyOneProp.class));
        assertEquals(List.of("only"), Beans.getPropNameList(C001OnlyOneProp.class));

        Beans.registerNonPropertyAccessor(C001OnlyOneProp.class, "only");

        assertEquals(List.of(), Beans.getPropNameList(C001OnlyOneProp.class));
        assertFalse(Beans.isBeanClass(C001OnlyOneProp.class), "isBeanClass must not stay true for a class with no properties");
    }

    @Test
    public void testC001_cachedAccessorsAreDroppedForTheExcludedProperty() {
        assertNotNull(Beans.getPropGetter(C001TwoProps.class, "hidden"));
        assertNotNull(Beans.getPropSetter(C001TwoProps.class, "hidden"));

        Beans.registerNonPropertyAccessor(C001TwoProps.class, "hidden");

        assertEquals(List.of("kept"), Beans.getPropNameList(C001TwoProps.class));
        assertTrue(Beans.isBeanClass(C001TwoProps.class), "the class still has one property");
    }

    // ------------------------------------------------------------------ C-009 / C-010

    public static class C009Bean {
        private String name = "preset";
        private int age = 7;

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
    public void testC009_newRandomBeanListValidatesEvenWhenCountIsZero() {
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(C009Bean.class, (Collection<String>) null, 0));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(C009Bean.class, Arrays.asList("bogus"), 0));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(C009Bean.class, Arrays.asList("name", "bogus"), 0));
    }

    @Test
    public void testC009_newRandomBeanListStillWorksForValidInput() {
        assertEquals(List.of(), Beans.newRandomBeanList(C009Bean.class, Arrays.asList("name"), 0));
        assertEquals(List.of(), Beans.newRandomBeanList(C009Bean.class, new ArrayList<>(), 0));

        final List<C009Bean> beans = Beans.newRandomBeanList(C009Bean.class, Arrays.asList("name"), 3);
        assertEquals(3, beans.size());

        for (final C009Bean bean : beans) {
            assertFalse("preset".equals(bean.getName()), "name should have been randomized");
            assertEquals(7, bean.getAge(), "age was not selected and must keep its initializer");
        }
    }

    @Test
    public void testC010_clearPropsIsAllOrNothing() {
        final C009Bean bean = new C009Bean();

        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(bean, "name", "missing"));
        assertEquals("preset", bean.getName(), "name must not be cleared when a later name is rejected");
        assertEquals(7, bean.getAge());

        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(bean, Arrays.asList("name", "missing")));
        assertEquals("preset", bean.getName());
    }

    @Test
    public void testC010_clearPropsStillClearsValidNames() {
        final C009Bean bean = new C009Bean();
        Beans.clearProps(bean, "name");
        assertEquals(null, bean.getName());
        assertEquals(7, bean.getAge());

        final C009Bean bean2 = new C009Bean();
        Beans.clearProps(bean2, Arrays.asList("name", "age"));
        assertEquals(null, bean2.getName());
        assertEquals(0, bean2.getAge());
    }

    @Test
    public void testC010_clearPropsEdgeCases() {
        final C009Bean bean = new C009Bean();
        Beans.clearProps(bean, new String[0]);
        Beans.clearProps(bean, List.of());
        Beans.clearProps(null, "name");
        assertEquals("preset", bean.getName());
    }

    @Test
    public void testC010_randomizeIsAllOrNothing() {
        final C009Bean bean = new C009Bean();

        assertThrows(IllegalArgumentException.class, () -> Beans.randomize(bean, Arrays.asList("name", "bogus")));
        assertEquals("preset", bean.getName(), "name must not be randomized when a later name is rejected");
        assertEquals(7, bean.getAge());

        Beans.randomize(bean, Arrays.asList("name"));
        assertFalse("preset".equals(bean.getName()));
        assertEquals(7, bean.getAge());
    }

    // ------------------------------------------------------------------ C-017

    @Test
    public void testC017_normalizePropNameRemapsOnlyClass() {
        assertEquals("clazz", Beans.normalizePropName("class"));
        assertEquals("userName", Beans.normalizePropName("user_name"));

        // Documented as the only remapping: every other keyword comes back unchanged and still illegal.
        assertEquals("int", Beans.normalizePropName("int"));
        assertEquals("enum", Beans.normalizePropName("enum"));
        assertEquals("null", Beans.normalizePropName("null"));

        assertEquals(null, Beans.normalizePropName(null));
        assertEquals("", Beans.normalizePropName(""));
    }

    // ------------------------------------------------------------------ C-010 (part 2): mergeInto

    public static class C010Src {
        private String name = "srcName";
        private int age = 30;
        private String extra = "onlyInSource";

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

        public String getExtra() {
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }
    }

    public static class C010Tgt {
        private String name = "tgtName";
        private int age = 1;

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

    private static void assertUntouched(final C010Tgt target) {
        assertEquals("tgtName", target.getName(), "target must not be half-merged");
        assertEquals(1, target.getAge(), "target must not be half-merged");
    }

    @Test
    public void testC010_mergeIntoSelectedIsAllOrNothing() {
        // "extra" exists on the source but not the target, and "name" would already have been written.
        final C010Tgt byTargetMiss = new C010Tgt();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(new C010Src(), byTargetMiss, Arrays.asList("name", "extra")));
        assertUntouched(byTargetMiss);

        final C010Tgt bySourceMiss = new C010Tgt();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(new C010Src(), bySourceMiss, Arrays.asList("name", "bogus")));
        assertUntouched(bySourceMiss);
    }

    @Test
    public void testC010_mergeIntoFilteredIsAllOrNothing() {
        final C010Tgt target = new C010Tgt();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeIntoIf(new C010Src(), target, Fn.<String, Object> p((n, v) -> true)));
        assertUntouched(target);
    }

    @Test
    public void testC010_mergeIntoStrictUnmatchedIsAllOrNothing() {
        final C010Tgt target = new C010Tgt();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(new C010Src(), target, false, null));
        assertUntouched(target);
    }

    @Test
    public void testC010_mergeIntoStillMergesNormally() {
        final C010Tgt selected = new C010Tgt();
        Beans.mergeInto(new C010Src(), selected, Arrays.asList("name", "age"));
        assertEquals("srcName", selected.getName());
        assertEquals(30, selected.getAge());

        final C010Tgt lenient = new C010Tgt();
        Beans.mergeInto(new C010Src(), lenient, true, null);
        assertEquals("srcName", lenient.getName());
        assertEquals(30, lenient.getAge());

        final C010Tgt filtered = new C010Tgt();
        Beans.mergeIntoIf(new C010Src(), filtered, Fn.<String, Object> p((n, v) -> "name".equals(n)));
        assertEquals("srcName", filtered.getName());
        assertEquals(1, filtered.getAge(), "age was filtered out");

        final C010Tgt ignored = new C010Tgt();
        Beans.mergeInto(new C010Src(), ignored, true, CommonUtil.asSet("extra"), Fn.<Object> o((s, t) -> s));
        assertEquals("srcName", ignored.getName());
        assertEquals(30, ignored.getAge());

        final C010Tgt empty = new C010Tgt();
        Beans.mergeInto(new C010Src(), empty, List.of());
        assertUntouched(empty);
    }

    @Test
    public void testC010_mergeFuncStillSeesTheLiveTargetValue() {
        // Deferring the writes must not change what the merge function is handed.
        final C010Tgt target = new C010Tgt();
        Beans.mergeInto(new C010Src(), target, Arrays.asList("age"), Fn.identity(),
                Fn.<Object> o((s, t) -> (s instanceof Integer si && t instanceof Integer ti) ? si + ti : s));
        assertEquals(31, target.getAge(), "30 (source) + 1 (target)");
    }

    // ------------------------------------------------------------------ C-011

    public static class C011Dto {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public void testC011_nameConversionPoolsStopGrowing() throws Exception {
        final int cap = maxCachedNames();

        withRestoredNamePools(() -> {
            for (int i = 0; i < cap * 2; i++) {
                final String s = "c011_probe_" + i;
                Beans.toCamelCase(s);
                Beans.toSnakeCase(s);
                Beans.toScreamingSnakeCase(s);
                Beans.normalizePropName(s);
            }

            for (final String pool : new String[] { "camelCasePropNamePool", "snakeCasePropNamePool", "screamingSnakeCasePropNamePool",
                    "formalizedPropNamePool" }) {
                assertTrue(poolSize(pool) <= cap, pool + " grew past the cap: " + poolSize(pool));
            }

            // Conversion results must be identical whether or not the pool accepted the entry.
            assertEquals("userName", Beans.toCamelCase("user_name"));
            assertEquals("user_name", Beans.toSnakeCase("userName"));
            assertEquals("USER_NAME", Beans.toScreamingSnakeCase("userName"));
            assertEquals("clazz", Beans.normalizePropName("class"));
            assertEquals("zzzNeverCachedYet", Beans.toCamelCase("zzz_never_cached_yet"));
            assertEquals(null, Beans.toCamelCase(null));
            assertEquals("", Beans.toCamelCase(""));
        });
    }

    /**
     * Runs {@code body}, then removes everything it added to the shared static name pools.
     *
     * <p>These pools outlive the test, and both C-011 tests deliberately overflow them. Beans' own pools are
     * pure memoization so leaving them full would only be untidy &mdash; but {@code Beans.toCamelCase} (which
     * {@code normalizePropName}, and therefore every failed {@code getPropGetter} lookup, goes through) also
     * interns into {@code NameUtil}'s separately capped pool. Filling that one makes {@code NameUtilTest}'s
     * {@code isCachedName} / {@code assertSame} assertions fail later in the suite.</p>
     */
    private static void withRestoredNamePools(final Throwables.Runnable<Exception> body) throws Exception {
        final java.util.List<java.util.Map<String, ?>> shared = java.util.List.of(namePool(Beans.class, "camelCasePropNamePool"),
                namePool(Beans.class, "snakeCasePropNamePool"), namePool(Beans.class, "screamingSnakeCasePropNamePool"),
                namePool(Beans.class, "formalizedPropNamePool"), namePool(NameUtil.class, "cachedNamePool"));
        final java.util.List<java.util.Set<String>> before = new ArrayList<>();

        for (final java.util.Map<String, ?> pool : shared) {
            before.add(new java.util.HashSet<>(pool.keySet()));
        }

        try {
            body.run();
        } finally {
            for (int i = 0; i < shared.size(); i++) {
                shared.get(i).keySet().retainAll(before.get(i));
            }
        }
    }

    /** The live backing map of a private static name pool, so a test can undo what it added to it. */
    @SuppressWarnings("unchecked")
    private static java.util.Map<String, ?> namePool(final Class<?> owner, final String fieldName) throws Exception {
        final java.lang.reflect.Field f = owner.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (java.util.Map<String, ?>) f.get(null);
    }

    @Test
    public void testC011_negativeLookupCachesStopGrowing() throws Exception {
        final int cap = maxCachedNames();

        assertNotNull(Beans.getPropGetter(C011Dto.class, "name"));

        // A failed lookup runs the name through normalizePropName, so this overflows the shared pools too.
        withRestoredNamePools(() -> {
            for (int i = 0; i < cap * 2; i++) {
                Beans.getPropGetter(C011Dto.class, "c011missing" + i);
                Beans.getPropSetter(C011Dto.class, "c011missing" + i);
                Beans.getPropField(C011Dto.class, "c011missing" + i);
            }

            assertTrue(perClassPoolSize("beanPropGetMethodPool", C011Dto.class) <= cap);
            assertTrue(perClassPoolSize("beanPropSetMethodPool", C011Dto.class) <= cap);
            assertTrue(perClassPoolSize("beanPropFieldPool", C011Dto.class) <= cap);

            // Positive entries are never refused, and misses still resolve to null past the cap.
            assertNotNull(Beans.getPropGetter(C011Dto.class, "name"));
            assertNotNull(Beans.getPropSetter(C011Dto.class, "name"));
            assertEquals(null, Beans.getPropGetter(C011Dto.class, "c011neverAsked"));
            assertEquals(List.of("name"), Beans.getPropNameList(C011Dto.class));

            final C011Dto dto = new C011Dto();
            Beans.setPropValue(dto, "name", "Ada");
            assertEquals("Ada", Beans.getPropValue(dto, "name"));
        });
    }

    // ------------------------------------------------------------------ C-003

    public static class C003Jaxb {
        private final List<String> tags = new ArrayList<>();
        private final java.util.Map<String, String> attrs = new java.util.LinkedHashMap<>();

        public List<String> getTags() {
            return tags;
        }

        public java.util.Map<String, String> getAttrs() {
            return attrs;
        }

        public String getName() {
            return "not-a-collection";
        }
    }

    @Test
    public void testC003_wrongTypedValueDoesNotWipeTheTarget() throws Exception {
        final java.lang.reflect.Method getTags = C003Jaxb.class.getMethod("getTags");
        final C003Jaxb bean = new C003Jaxb();
        bean.getTags().addAll(Arrays.asList("a", "b", "c"));

        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValueByGetter(bean, getTags, "not-a-collection"));
        assertEquals(Arrays.asList("a", "b", "c"), bean.getTags(), "the target must survive a rejected value");

        final java.lang.reflect.Method getAttrs = C003Jaxb.class.getMethod("getAttrs");
        final C003Jaxb mapBean = new C003Jaxb();
        mapBean.getAttrs().put("k", "v");

        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValueByGetter(mapBean, getAttrs, Arrays.asList("z")));
        assertEquals(java.util.Map.of("k", "v"), mapBean.getAttrs());
    }

    @Test
    public void testC003_viewOverTheSameBackingCollectionIsNotWiped() throws Exception {
        final java.lang.reflect.Method getTags = C003Jaxb.class.getMethod("getTags");

        final C003Jaxb unmodifiable = new C003Jaxb();
        unmodifiable.getTags().addAll(Arrays.asList("x", "y"));
        Beans.setPropValueByGetter(unmodifiable, getTags, java.util.Collections.unmodifiableList(unmodifiable.getTags()));
        assertEquals(Arrays.asList("x", "y"), unmodifiable.getTags());

        final C003Jaxb sublist = new C003Jaxb();
        sublist.getTags().addAll(Arrays.asList("p", "q", "r"));
        Beans.setPropValueByGetter(sublist, getTags, sublist.getTags().subList(0, 2));
        assertEquals(Arrays.asList("p", "q"), sublist.getTags());
    }

    @Test
    public void testC003_normalReplacementStillWorks() throws Exception {
        final java.lang.reflect.Method getTags = C003Jaxb.class.getMethod("getTags");
        final java.lang.reflect.Method getAttrs = C003Jaxb.class.getMethod("getAttrs");
        final java.lang.reflect.Method getName = C003Jaxb.class.getMethod("getName");
        final C003Jaxb bean = new C003Jaxb();
        bean.getTags().add("old");

        Beans.setPropValueByGetter(bean, getTags, Arrays.asList("new1", "new2"));
        assertEquals(Arrays.asList("new1", "new2"), bean.getTags());

        Beans.setPropValueByGetter(bean, getTags, null);
        assertEquals(Arrays.asList("new1", "new2"), bean.getTags(), "a null value is a no-op");

        Beans.setPropValueByGetter(bean, getTags, List.of());
        assertEquals(List.of(), bean.getTags());

        Beans.setPropValueByGetter(bean, getTags, bean.getTags());
        assertEquals(List.of(), bean.getTags(), "the identical instance is a no-op");

        Beans.setPropValueByGetter(bean, getAttrs, java.util.Map.of("k", "v"));
        assertEquals(java.util.Map.of("k", "v"), bean.getAttrs());

        // A getter returning neither a Collection nor a Map is still rejected.
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValueByGetter(bean, getName, "x"));
    }

    // ------------------------------------------------------------------ C-004

    public static class C004WithInit {
        private int count = 42;
        private String label = "preset";

        public int getCount() {
            return count;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }
    }

    @Test
    public void testC004_absentSelectedKeyKeepsTheInitializer() {
        final java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("label", "given");

        final C004WithInit bean = Beans.mapToBean(map, Arrays.asList("label", "count"), C004WithInit.class);
        assertEquals("given", bean.getLabel());
        assertEquals(42, bean.getCount(), "an absent key must not clobber the field initializer");

        // The sibling overload has always behaved this way; the two now agree.
        assertEquals(42, Beans.mapToBean(map, C004WithInit.class).getCount());

        assertEquals(42, Beans.mapToBean(new java.util.LinkedHashMap<>(), Arrays.asList("count"), C004WithInit.class).getCount());
        assertEquals(42, Beans.mapToBean(map, List.of(), C004WithInit.class).getCount());
    }

    @Test
    public void testC004_explicitNullStillClearsAndPresentValuesStillWrite() {
        final java.util.Map<String, Object> withNull = new java.util.LinkedHashMap<>();
        withNull.put("count", null);
        withNull.put("label", null);
        final C004WithInit cleared = Beans.mapToBean(withNull, Arrays.asList("count", "label"), C004WithInit.class);
        assertEquals(0, cleared.getCount(), "an explicit null is still the way to clear a property");
        assertEquals(null, cleared.getLabel());

        final C004WithInit written = Beans.mapToBean(java.util.Map.of("count", 5), Arrays.asList("count"), C004WithInit.class);
        assertEquals(5, written.getCount());
    }

    @Test
    public void testC004_bogusSelectedNameIsStillRejected() {
        // Skipping absent keys must not skip the validation that used to happen inside the write.
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(new java.util.LinkedHashMap<>(), Arrays.asList("bogus"), C004WithInit.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(java.util.Map.of("label", "x"), Arrays.asList("bogus"), C004WithInit.class));
        assertEquals(null, Beans.mapToBean((java.util.Map<String, Object>) null, Arrays.asList("count"), C004WithInit.class));
    }

    // ------------------------------------------------------------------ C-007

    /** Value equality matters here: the shallow shape stores this bean itself, and the captured and
     *  delegated builder paths are compared across two separate bean instances. */
    public static class C007Addr {
        private String city = "NYC";

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof C007Addr other && CommonUtil.equals(city, other.city);
        }

        @Override
        public int hashCode() {
            return CommonUtil.hashCode(city);
        }

        @Override
        public String toString() {
            return "C007Addr[" + city + "]";
        }
    }

    /** Counts getter invocations so the builder's double-read is directly observable. */
    public static class C007Counting {
        static int reads = 0;

        private String a = "A";
        private String b;
        private C007Addr addr = new C007Addr();

        public String getA() {
            reads++;
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public String getB() {
            reads++;
            return b;
        }

        public void setB(final String b) {
            this.b = b;
        }

        public C007Addr getAddr() {
            reads++;
            return addr;
        }

        public void setAddr(final C007Addr addr) {
            this.addr = addr;
        }
    }

    /** Returns a non-null value on the first read and null afterwards. */
    public static class C007Flipper {
        static int flips = 0;

        private String a = "A";

        public String getA() {
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public String getFlip() {
            return flips++ == 0 ? "first" : null;
        }

        public void setFlip(final String flip) {
        }
    }

    @Test
    public void testC007_eachGetterIsReadOnce() {
        for (final String shape : new String[] { "shallow", "deep", "flat" }) {
            for (final String mode : new String[] { "plain", "skipNulls", "filter" }) {
                C007Counting.reads = 0;

                Beans.BeanMapBuilder builder = Beans.mapBuilder(new C007Counting());

                if ("deep".equals(shape)) {
                    builder = builder.deep();
                } else if ("flat".equals(shape)) {
                    builder = builder.flat();
                }

                if ("skipNulls".equals(mode)) {
                    builder = builder.skipNulls();
                } else if ("filter".equals(mode)) {
                    builder = builder.filter((n, v) -> true);
                }

                builder.toMap();

                assertEquals(3, C007Counting.reads, shape + "/" + mode + ": each of the 3 getters must run exactly once");
            }
        }
    }

    @Test
    public void testC007_skipNullsUsesTheValueItActuallyStores() {
        C007Flipper.flips = 0;
        final java.util.Map<String, Object> map = Beans.mapBuilder(new C007Flipper()).skipNulls().toMap();

        assertEquals("A", map.get("a"));
        assertEquals("first", map.get("flip"), "the value tested by skipNulls must be the value stored");
        assertFalse(map.containsValue(null), "skipNulls must not admit a null");
    }

    @Test
    public void testC007_capturedAndDelegatedPathsAgree() {
        // skipNulls/filter take the captured path; without them the builder delegates. Both must match.
        for (final NamingPolicy policy : new NamingPolicy[] { NamingPolicy.CAMEL_CASE, NamingPolicy.NO_CHANGE, NamingPolicy.SNAKE_CASE,
                NamingPolicy.UPPER_CAMEL_CASE, NamingPolicy.KEBAB_CASE, NamingPolicy.SCREAMING_SNAKE_CASE }) {
            for (final String shape : new String[] { "shallow", "deep", "flat" }) {
                assertEquals(build(shape, policy, false), build(shape, policy, true), shape + "/" + policy);
            }
        }
    }

    private static java.util.Map<String, Object> build(final String shape, final NamingPolicy policy, final boolean captured) {
        Beans.BeanMapBuilder builder = Beans.mapBuilder(new C007Counting()).naming(policy);

        if ("deep".equals(shape)) {
            builder = builder.deep();
        } else if ("flat".equals(shape)) {
            builder = builder.flat();
        }

        // A filter that accepts everything forces the captured path without changing the expected content.
        return captured ? builder.filter((n, v) -> true).toMap() : builder.toMap();
    }

    @Test
    public void testC007_selectionKeepsTheSelectionOrder_andAliasesCollapse() {
        assertEquals(List.of("a", "addr"), List.copyOf(Beans.mapBuilder(new C007Counting()).skipNulls().toMap().keySet()));
        assertEquals(List.of("addr", "a"), List.copyOf(Beans.mapBuilder(new C007Counting()).select("addr", "a").skipNulls().toMap().keySet()));
        assertEquals(List.of("a"), List.copyOf(Beans.mapBuilder(new C007Counting()).select("a", "A").skipNulls().toMap().keySet()));
        assertEquals(List.of("a"), List.copyOf(Beans.mapBuilder(new C007Counting()).exclude("addr", "b").skipNulls().toMap().keySet()));
        assertEquals(java.util.Map.of(), Beans.mapBuilder(null).skipNulls().toMap());
        assertThrows(IllegalArgumentException.class, () -> Beans.mapBuilder(new C007Counting()).select("bogus").skipNulls().toMap());
    }

    private static int maxCachedNames() throws Exception {
        final java.lang.reflect.Field f = Beans.class.getDeclaredField("MAX_CACHED_NAMES");
        f.setAccessible(true);
        return f.getInt(null);
    }

    @SuppressWarnings("unchecked")
    private static int poolSize(final String fieldName) throws Exception {
        final java.lang.reflect.Field f = Beans.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return ((java.util.Map<String, ?>) f.get(null)).size();
    }

    @SuppressWarnings("unchecked")
    private static int perClassPoolSize(final String fieldName, final Class<?> cls) throws Exception {
        final java.lang.reflect.Field f = Beans.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        // The pools are Beans.ClassCache (backed by a ClassValue, so a cache entry cannot outlive the class
        // it describes) rather than a Map<Class<?>, ?>, so the entry comes through the cache's own get.
        final Object cache = f.get(null);
        final java.lang.reflect.Method get = cache.getClass().getDeclaredMethod("get", Class.class);
        get.setAccessible(true);
        final java.util.Map<String, ?> inner = (java.util.Map<String, ?>) get.invoke(cache, cls);
        return inner == null ? 0 : inner.size();
    }
}
