package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;

/**
 * Regression tests for the 2026-08-31 {@link Beans} review fixes. One nested section per finding; see
 * scripts/cross_review/Beans_ledger_2026-08-31.md.
 */
public class BeansRegressionBTest extends TestBase {

    // ================================================================== shared fixtures

    public static class Inner {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class Outer {
        private String name;
        private Inner inner;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Inner getInner() {
            return inner;
        }

        public void setInner(final Inner inner) {
            this.inner = inner;
        }
    }

    /** Fields declared in a deliberately non-alphabetical order, so a sorted map is observable. */
    public static class Zebra {
        private String zip;
        private String city;

        public String getZip() {
            return zip;
        }

        public void setZip(final String zip) {
            this.zip = zip;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class ZebraHolder {
        private String name;
        private Zebra address;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Zebra getAddress() {
            return address;
        }

        public void setAddress(final Zebra address) {
            this.address = address;
        }
    }

    // ================================================================== B1: the 128-character name limit

    private static String longName(final int len) {
        return Strings.repeat("a", len);
    }

    @Test
    public void testB1_overLongPropNameIsNotFoundRatherThanAnError() throws Exception {
        withRestoredNamePools(() -> {
            final String tooLong = longName(129);

            // "not found", not an error: an over-long name is simply not a property of the class.
            assertNull(Beans.getPropGetter(Outer.class, tooLong));
            assertNull(Beans.getPropSetter(Outer.class, tooLong));
            assertNull(Beans.getPropField(Outer.class, tooLong));

            // The 128-character boundary itself is unchanged.
            assertNull(Beans.getPropGetter(Outer.class, longName(128)));

            // ... and a real property is still found.
            assertNotNull(Beans.getPropGetter(Outer.class, "name"));
        });
    }

    @Test
    public void testB1_ignoreUnmatchedPropertyHonoursAnOverLongName() throws Exception {
        withRestoredNamePools(() -> {
            final String tooLong = longName(200);
            final Outer bean = new Outer();
            bean.setName("N");

            // The whole point of ignoreUnmatchedProperty=true is that an unknown name is ignored.
            assertNull(Beans.getPropValue(bean, tooLong, true));
            assertTrue(Beans.getPropValueIfPresent(bean, tooLong).isEmpty());

            // ... and with the flag off it is still a plain "no such property" failure.
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(bean, tooLong, false));
        });
    }

    @Test
    public void testB1_mapToBeanIgnoresAnOverLongKey() throws Exception {
        withRestoredNamePools(() -> {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put(longName(300), "junk");
            map.put("name", "Ada");

            final Outer bean = Beans.mapToBean(map, true, Outer.class);
            assertEquals("Ada", bean.getName());

            assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, false, Outer.class));
        });
    }

    // ================================================================== B2: mergeInto alias matching + single read

    public static class B2Source {
        @JsonXmlField(aliases = { "nick" })
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class B2Target {
        private String nick;

        public String getNick() {
            return nick;
        }

        public void setNick(final String nick) {
            this.nick = nick;
        }
    }

    @Test
    public void testB2_converterOverloadMatchesAliasesLikeItsSiblings() {
        final B2Source source = new B2Source();
        source.setName("Ada");

        final B2Target viaConverter = new B2Target();
        Beans.mergeInto(source, viaConverter, Fn.identity(), Fn.selectFirst());

        final B2Target viaSelection = new B2Target();
        Beans.mergeInto(source, viaSelection, (Collection<String>) null, Fn.identity(), Fn.selectFirst());

        // All three routes resolve the target through the source PropInfo, so all three see the alias.
        assertEquals("Ada", viaConverter.getNick());
        assertEquals("Ada", viaSelection.getNick());
        assertEquals("Ada", Beans.copyAs(source, B2Target.class).getNick());
    }

    public static final AtomicInteger B2_READS = new AtomicInteger();

    public static class B2Counting {
        private String a = "A";
        private String b = "B";
        private String c = "C";

        public String getA() {
            B2_READS.incrementAndGet();
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public String getB() {
            B2_READS.incrementAndGet();
            return b;
        }

        public void setB(final String b) {
            this.b = b;
        }

        public String getC() {
            B2_READS.incrementAndGet();
            return c;
        }

        public void setC(final String c) {
            this.c = c;
        }
    }

    public static class B2Plain {
        private String a;
        private String b;
        private String c;

        public String getA() {
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(final String b) {
            this.b = b;
        }

        public String getC() {
            return c;
        }

        public void setC(final String c) {
            this.c = c;
        }
    }

    @Test
    public void testB2b_converterOverloadReadsEachSourceGetterOnce() {
        final B2Counting source = new B2Counting();

        B2_READS.set(0);
        Beans.mergeInto(source, new B2Plain(), Fn.identity(), Fn.selectFirst());
        assertEquals(3, B2_READS.get(), "each of the 3 source getters must be invoked exactly once");

        // Same result as the equivalent selection-based call, which already read each getter once.
        B2_READS.set(0);
        Beans.mergeInto(source, new B2Plain(), (Collection<String>) null, Fn.identity(), Fn.selectFirst());
        assertEquals(3, B2_READS.get());
    }

    @Test
    public void testB2_converterOverloadStillSkipsUnmatchedProperties() {
        final B2Counting source = new B2Counting();
        final Zebra target = new Zebra(); // shares no property name with B2Counting

        // Silently skipped, as documented - not an exception.
        Beans.mergeInto(source, target, Fn.identity(), Fn.selectFirst());
        assertNull(target.getCity());
        assertNull(target.getZip());
    }

    // ================================================================== B3: clearProps is all-or-nothing

    @Test
    public void testB3_clearPropsRejectsABogusDottedNameBeforeWriting() {
        final Outer bean = new Outer();
        bean.setName("KEEP");
        final Inner inner = new Inner();
        inner.setCity("NYC");
        bean.setInner(inner);

        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(bean, "name", "bogus.path"));

        assertEquals("KEEP", bean.getName(), "an unresolvable dotted name must not leave the bean half-cleared");
        assertEquals("NYC", inner.getCity());
    }

    @Test
    public void testB3_clearPropsCollectionOverloadIsAlsoAllOrNothing() {
        final Outer bean = new Outer();
        bean.setName("KEEP");

        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(bean, Arrays.asList("name", "bogus.path")));
        assertEquals("KEEP", bean.getName());
    }

    @Test
    public void testB3_clearPropsStillAcceptsAResolvableDottedName() {
        final Outer bean = new Outer();
        bean.setName("N");
        final Inner inner = new Inner();
        inner.setCity("NYC");
        bean.setInner(inner);

        Beans.clearProps(bean, "inner.city");
        assertNull(inner.getCity());
        assertEquals("N", bean.getName());

        // A missing intermediate is instantiated by the write, exactly as it is for a dotted mapToBean key.
        final Outer noInner = new Outer();
        noInner.setName("N");
        Beans.clearProps(noInner, "inner.city");
        assertNotNull(noInner.getInner());
        assertNull(noInner.getInner().getCity());
    }

    // ================================================================== B4/B5: caller-keyed caches are bounded

    private static int maxCachedNames() throws Exception {
        final Field f = Beans.class.getDeclaredField("MAX_CACHED_NAMES");
        f.setAccessible(true);
        return f.getInt(null);
    }

    @SuppressWarnings("unchecked")
    /**
     * Reads one class's entry out of a private {@code Beans} pool. The pools are {@code Beans.ClassCache}
     * (backed by a {@code ClassValue}, so a cache entry cannot outlive the class it describes) rather than a
     * {@code Map<Class<?>, ?>}, so the entry has to be fetched through the cache's own {@code get}.
     */
    private static Object perClassEntry(final String fieldName, final Class<?> cls) throws Exception {
        final Field f = Beans.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        final Object cache = f.get(null);
        final java.lang.reflect.Method get = cache.getClass().getDeclaredMethod("get", Class.class);
        get.setAccessible(true);
        return get.invoke(cache, cls);
    }

    private static int perClassPoolSize(final String fieldName, final Class<?> cls) throws Exception {
        final Map<String, ?> m = (Map<String, ?>) perClassEntry(fieldName, cls);
        return m == null ? 0 : m.size();
    }

    /** A dedicated class per test, so one test's cache pressure cannot be observed by another. */
    public static class B4Bean {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    @Test
    public void testB4_nestedPathCacheStopsGrowing() throws Exception {
        withRestoredNamePools(() -> {
            final int cap = maxCachedNames();
            final B4Bean bean = new B4Bean();

            for (int i = 0; i < cap + 500; i++) {
                assertNull(Beans.getPropValue(bean, "b4NoSuchProp" + i, true));
            }

            assertTrue(perClassPoolSize("beanInlinePropGetMethodPool", B4Bean.class) <= cap,
                    "beanInlinePropGetMethodPool grew past the cap: " + perClassPoolSize("beanInlinePropGetMethodPool", B4Bean.class));

            // Results stay correct whether or not the cache accepted the entry.
            bean.setCity("NYC");
            assertEquals("NYC", Beans.getPropValue(bean, "city"));
            assertNull(Beans.getPropValue(bean, "b4NoSuchProp0", true));
            assertNull(Beans.getPropValue(bean, "b4NeverSeenBefore", true));
        });
    }

    public static class B5Bean {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    /**
     * Underscores are stripped by {@code isPropName}, so every one of these <em>resolves</em> to {@code city}.
     * Before the fix each distinct spelling was a permanent cache entry.
     */
    private static List<String> underscoreAliasesOfCity(final int howMany) {
        final List<String> aliases = new ArrayList<>(howMany);
        final char[] base = "city".toCharArray();

        for (int i = 0; aliases.size() < howMany; i++) {
            final StringBuilder sb = new StringBuilder();
            int n = i;

            for (int gap = 0; gap <= 4; gap++) {
                final int count = n % 8;
                n /= 8;
                sb.append(Strings.repeat("_", count));

                if (gap < 4) {
                    sb.append(base[gap]);
                }
            }

            aliases.add(sb.toString());
        }

        return aliases;
    }

    @Test
    public void testB5_resolvedAliasCacheStopsGrowing() throws Exception {
        withRestoredNamePools(() -> {
            final int cap = maxCachedNames();
            final List<String> aliases = underscoreAliasesOfCity(cap + 500);

            for (final String alias : aliases) {
                // Every alias really does resolve - this is the "hit" side of the cache, not the miss side.
                assertNotNull(Beans.getPropGetter(B5Bean.class, alias), alias);
            }

            assertTrue(perClassPoolSize("beanPropGetMethodPool", B5Bean.class) <= cap,
                    "beanPropGetMethodPool grew past the cap: " + perClassPoolSize("beanPropGetMethodPool", B5Bean.class));

            // The canonical entry is never crowded out, and lookups past the cap still resolve.
            assertEquals("getCity", Beans.getPropGetter(B5Bean.class, "city").getName());
            assertEquals("getCity", Beans.getPropGetter(B5Bean.class, aliases.get(aliases.size() - 1)).getName());
            assertEquals("getCity", Beans.getPropGetter(B5Bean.class, "c_i_t_y").getName());
            assertNull(Beans.getPropGetter(B5Bean.class, "notAProperty"));
        });
    }

    // ================================================================== B6: null Class is rejected uniformly

    @Test
    public void testB6_nullClassIsIllegalArgumentAcrossTheIntrospectionFamily() {
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNameList(null));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetters(null));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropSetter(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropSetters(null));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropField(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropFields(null));
        assertThrows(IllegalArgumentException.class, () -> Beans.getIgnoredPropNamesForDiff(null));
        assertThrows(IllegalArgumentException.class, () -> Beans.registerNonBeanClass(null));
        assertThrows(IllegalArgumentException.class, () -> Beans.registerXmlBindingClass(null));

        // Unchanged: these two are documented as null-tolerant.
        assertFalse(Beans.isBeanClass(null));
        assertFalse(Beans.isRegisteredXmlBindingClass(null));
    }

    // ================================================================== B7: randomize matches newRandomBean

    public static class B7Node {
        private String name;
        private B7Node next;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public B7Node getNext() {
            return next;
        }

        public void setNext(final B7Node next) {
            this.next = next;
        }
    }

    @Test
    public void testB7_randomizeBreaksSelfReferenceAtTheSameDepthAsNewRandomBean() {
        final B7Node randomized = new B7Node();
        Beans.randomize(randomized);

        assertNotNull(randomized.getName());
        assertNull(randomized.getNext(), "randomize must seed the visited set with the bean's own class");

        assertNull(Beans.newRandomBean(B7Node.class).getNext());
    }

    @Test
    public void testB7_randomizeStillClearsTheThreadLocalAfterwards() throws Exception {
        Beans.randomize(new B7Node());

        final Field f = Beans.class.getDeclaredField("NEW_RANDOM_VISITED");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        final ThreadLocal<Set<Class<?>>> visited = (ThreadLocal<Set<Class<?>>>) f.get(null);

        assertTrue(visited.get().isEmpty());
        visited.remove();
    }

    // ================================================================== D2: the supplier builds every level

    @Test
    public void testD2_mapSupplierIsUsedForNestedBeansToo() {
        final ZebraHolder holder = new ZebraHolder();
        holder.setName("N");
        final Zebra address = new Zebra();
        address.setZip("10001");
        address.setCity("NYC");
        holder.setAddress(address);

        final TreeMap<String, Object> deep = Beans.deepBeanToMap(holder, IntFunctions.<String, Object> ofTreeMap());

        assertEquals(Arrays.asList("address", "name"), new ArrayList<>(deep.keySet()));

        final Object nested = deep.get("address");
        assertTrue(nested instanceof TreeMap, "nested map should come from the supplier, was " + nested.getClass());
        assertEquals(Arrays.asList("city", "zip"), new ArrayList<>(((Map<String, Object>) nested).keySet()));
    }

    @Test
    public void testD2_supplierAlsoReachesTheSelectionAndExclusionPaths() {
        final ZebraHolder holder = new ZebraHolder();
        final Zebra address = new Zebra();
        address.setZip("10001");
        address.setCity("NYC");
        holder.setAddress(address);

        final Map<String, Object> selected = Beans.deepBeanToMap(holder, Arrays.asList("address"), NamingPolicy.CAMEL_CASE,
                IntFunctions.<String, Object> ofTreeMap());
        assertTrue(selected.get("address") instanceof TreeMap);

        final Map<String, Object> excluded = Beans.deepBeanToMap(holder, true, CommonUtil.asSet("name"), NamingPolicy.CAMEL_CASE,
                IntFunctions.<String, Object> ofTreeMap());
        assertTrue(excluded.get("address") instanceof TreeMap);

        final Map<String, Object> viaBuilder = Beans.mapBuilder(holder).deep().toMap(IntFunctions.<String, Object> ofTreeMap());
        assertTrue(viaBuilder.get("address") instanceof TreeMap);
    }

    @Test
    public void testD2_inPlaceOverloadsStillProduceLinkedHashMapNestedMaps() {
        final ZebraHolder holder = new ZebraHolder();
        final Zebra address = new Zebra();
        address.setZip("10001");
        holder.setAddress(address);

        final Map<String, Object> output = new TreeMap<>();
        Beans.deepBeanToMap(holder, true, output);

        // No supplier was given, so the nested map falls back to the documented default.
        assertTrue(output.get("address") instanceof LinkedHashMap);
    }

    // ================================================================== D3: the builder's null policy is uniform

    @Test
    public void testD3_builderKeepsNestedNullsByDefault() {
        final Outer bean = new Outer();
        bean.setInner(new Inner()); // name == null, inner.city == null

        assertEquals("{name=null, inner={city=null}}", Beans.mapBuilder(bean).deep().toMap().toString());
        assertEquals("{name=null, inner.city=null}", Beans.mapBuilder(bean).flat().toMap().toString());
    }

    @Test
    public void testD3_builderSkipNullsAppliesAtEveryLevel() {
        final Outer bean = new Outer();
        final Inner inner = new Inner();
        bean.setInner(inner);

        assertEquals("{inner={}}", Beans.mapBuilder(bean).deep().skipNulls().toMap().toString());
        assertEquals("{}", Beans.mapBuilder(bean).flat().skipNulls().toMap().toString());

        inner.setCity("NYC");
        assertEquals("{inner={city=NYC}}", Beans.mapBuilder(bean).deep().skipNulls().toMap().toString());
        assertEquals("{inner.city=NYC}", Beans.mapBuilder(bean).flat().skipNulls().toMap().toString());
    }

    @Test
    public void testD3_selectionOverloadsStillDropNestedNulls() {
        final Outer bean = new Outer();
        bean.setInner(new Inner());

        // Unchanged and documented: the selectPropNames overloads always omit nested nulls.
        assertEquals("{inner={}}", Beans.deepBeanToMap(bean, Arrays.asList("inner")).toString());
        assertEquals("{}", Beans.beanToFlatMap(bean, Arrays.asList("inner")).toString());
    }

    @Test
    public void testD3_builderWithAFilterAlsoKeepsNestedNulls() {
        final Outer bean = new Outer();
        bean.setInner(new Inner());

        // A filter forces the read-once path, which must apply the same null policy as the delegating path.
        assertEquals("{inner={city=null}}", Beans.mapBuilder(bean).deep().filter((n, v) -> !"name".equals(n)).toMap().toString());
        assertEquals("{inner.city=null}", Beans.mapBuilder(bean).flat().filter((n, v) -> !"name".equals(n)).toMap().toString());
    }

    // ================================================================== D4: varargs null handling

    @Test
    public void testD4_varargsSelectAndExcludeAcceptNull() {
        final Outer bean = new Outer();
        bean.setName("N");

        assertEquals("{name=N, inner=null}", Beans.mapBuilder(bean).select((String[]) null).toMap().toString());
        assertEquals("{name=N, inner=null}", Beans.mapBuilder(bean).exclude((String[]) null).toMap().toString());
        assertEquals("{name=N}", Beans.mapBuilder(bean).exclude("inner").toMap().toString());
    }

    // ================================================================== D5: selected merges stay all-or-nothing

    @Test
    public void testD5_selectedMergeRejectsATargetMissingNameWithoutWriting() {
        final Outer source = new Outer();
        source.setName("NEW");
        final Zebra target = new Zebra();
        target.setCity("OLD");

        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(source, target, Arrays.asList("name")));
        assertEquals("OLD", target.getCity());
    }

    // ================================================================== D6: builder metadata is invalidated too

    public static class D6Bean {
        private final String kept;
        private final String dropped;

        private D6Bean(final String kept, final String dropped) {
            this.kept = kept;
            this.dropped = dropped;
        }

        public String getKept() {
            return kept;
        }

        public String getDropped() {
            return dropped;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String kept;
            private String dropped;

            public Builder kept(final String kept) {
                this.kept = kept;
                return this;
            }

            public Builder dropped(final String dropped) {
                this.dropped = dropped;
                return this;
            }

            public D6Bean build() {
                return new D6Bean(kept, dropped);
            }
        }
    }

    /**
     * {@code loadPropGetSetMethodList(D6Bean.class)} writes the builder class's setter pools as a side effect,
     * so those entries are derived from {@code D6Bean}'s introspection and must be dropped when it is
     * invalidated. This is cache hygiene rather than a behaviour change: the builder branch derives its map
     * from the builder's own methods, so re-deriving it produces the same content today. The point is that
     * {@code invalidateBeanMetadata} no longer leaves behind an entry it wrote.
     */
    @Test
    public void testD6_registrationInvalidatesTheBuilderClassPoolsToo() throws Exception {
        assertEquals(Arrays.asList("kept", "dropped"), new ArrayList<>(Beans.getPropNameList(D6Bean.class)));
        assertTrue(Beans.getPropSetters(D6Bean.Builder.class).containsKey("dropped"));
        assertNotNull(perClassEntry("beanDeclaredPropSetMethodPool", D6Bean.Builder.class));

        Beans.registerNonPropertyAccessor(D6Bean.class, "dropped");

        // Checked before anything re-introspects D6Bean, which would legitimately rewrite these entries.
        assertNull(perClassEntry("beanDeclaredPropSetMethodPool", D6Bean.Builder.class),
                "the builder pool entry written while introspecting D6Bean must be dropped with it");
        assertNull(perClassEntry("beanPropSetMethodPool", D6Bean.Builder.class));

        assertEquals(Arrays.asList("kept"), new ArrayList<>(Beans.getPropNameList(D6Bean.class)));

        // Re-deriving the builder on its own still works.
        assertTrue(Beans.getPropSetters(D6Bean.Builder.class).containsKey("kept"));
    }

    // ================================================================== J9: the "not a bean class" message

    @Test
    public void testJ9_notABeanClassMessageNamesTheActualReason() {
        assertTrue(reasonFor(String.class).contains("CharSequence"), reasonFor(String.class));
        assertTrue(reasonFor(Integer.class).contains("Number"), reasonFor(Integer.class));
        assertTrue(reasonFor(ArrayList.class).contains("Collection"), reasonFor(ArrayList.class));
        assertTrue(reasonFor(LinkedHashMap.class).contains("Map"), reasonFor(LinkedHashMap.class));

        // A plain class with no accessors still gets the original explanation.
        final String noProps = reasonFor(NoProps.class);
        assertTrue(noProps.contains("no property getter/setter method or public field"), noProps);
    }

    public static class NoProps {
        // deliberately empty
    }

    private static String reasonFor(final Class<?> cls) {
        return assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(cls, "nope")).getMessage();
    }

    // ================================================================== helpers

    /**
     * Runs {@code body}, then removes everything it added to the shared static name pools.
     *
     * <p>The cache-pressure tests above push thousands of distinct names through lookups that intern through
     * {@link NameUtil#getCachedName(String)}, whose pool is capped and shared with the rest of the suite.
     * Leaving it full makes {@code NameUtilTest}'s {@code isCachedName}/{@code assertSame} assertions fail
     * later in the same JVM.</p>
     */
    private static void withRestoredNamePools(final Throwables.Runnable<Exception> body) throws Exception {
        final List<Map<String, ?>> shared = List.of(namePool(Beans.class, "camelCasePropNamePool"), namePool(Beans.class, "snakeCasePropNamePool"),
                namePool(Beans.class, "screamingSnakeCasePropNamePool"), namePool(Beans.class, "formalizedPropNamePool"),
                namePool(NameUtil.class, "cachedNamePool"));
        final List<Set<String>> before = new ArrayList<>();

        for (final Map<String, ?> pool : shared) {
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

    @SuppressWarnings("unchecked")
    private static Map<String, ?> namePool(final Class<?> owner, final String fieldName) throws Exception {
        final Field f = owner.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (Map<String, ?>) f.get(null);
    }

    @Test
    public void testHelpers_aliasGeneratorReallyProducesResolvableNames() {
        final List<String> aliases = underscoreAliasesOfCity(20);
        assertEquals(20, aliases.size());
        assertEquals(20, new java.util.HashSet<>(aliases).size(), "aliases must be distinct");

        for (final String alias : aliases) {
            assertEquals("city", alias.replace("_", ""));
            assertTrue(alias.length() <= 128);
        }

        assertSame(aliases.get(0), aliases.get(0));
    }

    // ================================================================== D7: a builder that omits a property

    public static class D7Bean {
        private final String value;
        private final String extra;

        private D7Bean(final String value, final String extra) {
            this.value = value;
            this.extra = extra;
        }

        public String getValue() {
            return value;
        }

        public String getExtra() {
            return extra;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String value;

            public Builder value(final String value) {
                this.value = value;
                return this;
            }

            public D7Bean build() {
                return new D7Bean(value, null);
            }
        }
    }

    /** Mutable source for the {@code copyAs} shape: it carries a value for the property D7Bean's builder omits. */
    public static class D7Plain {
        private String value;
        private String extra;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }
    }

    /**
     * An immutable builder-based bean may legitimately have a property its builder does not expose: a
     * field-backed getter is promoted to a property without requiring the builder to be able to set it.
     * {@code ParserUtil.BeanInfo} then drives the builder through {@code Beans.getPropSetter} for <i>every</i>
     * property of the bean, so that lookup must answer {@code null} for the omitted one instead of rejecting the
     * builder as "not a bean class".
     *
     * <p>With {@code private final} fields there is no self-heal: {@code ParserUtil} cannot fall back to field
     * access, so each of these calls failed on the first and on every later attempt.</p>
     *
     * <p>Writing the omitted property is the {@code ParserUtil}-side half of the same defect: with no builder
     * setter for it, {@code ParserUtil.PropInfo.setPropValue} used to fall back to setting the bean's own (final)
     * field on the builder instance, so {@code fromJson} of a document that carries {@code "extra"} - and
     * {@code copyAs}, which goes through the same writer - failed with {@code IllegalArgumentException: Can not set
     * final java.lang.String field ..D7Bean.extra to ..D7Bean$Builder}. Such a property is now skipped, the same
     * way an unmatched source value is, so both shapes below produce the bean the builder can actually build.</p>
     */
    @Test
    public void testD7_builderBasedBeanWithAPropertyItsBuilderOmits() {
        final D7Bean bean = D7Bean.builder().value("v").build();

        assertEquals("{value=v}", Beans.beanToMap(bean).toString());
        assertEquals("{value=v, extra=null}", Beans.beanToMap(bean, false).toString());
        assertEquals("{\"value\": \"v\"}", N.toJson(bean));
        assertEquals("v", N.fromJson("{\"value\": \"v\"}", D7Bean.class).getValue());
        assertNull(N.fromJson("{\"value\": \"v\"}", D7Bean.class).getExtra());

        // Stable, not just "works on the second attempt": the first failure used to degrade the cached BeanInfo.
        assertEquals("{\"value\": \"v\"}", N.toJson(bean));
        assertEquals("{value=v}", Beans.beanToMap(bean).toString());

        // A document that CARRIES the omitted property: the value has nowhere to go, so it is dropped rather than
        // written into the builder, and the properties the builder does expose still arrive.
        final D7Bean withExtra = N.fromJson("{\"value\": \"v\", \"extra\": \"e\"}", D7Bean.class);
        assertEquals("v", withExtra.getValue());
        assertNull(withExtra.getExtra());

        final D7Bean fromMap = Beans.mapToBean(Beans.beanToMap(bean, false), D7Bean.class);
        assertEquals("v", fromMap.getValue());
        assertNull(fromMap.getExtra());

        // copyAs goes through the same writer, whether or not the source actually has a value for it.
        final D7Plain plain = new D7Plain();
        plain.setValue("v");
        plain.setExtra("e");
        assertEquals("v", Beans.copyAs(plain, D7Bean.class).getValue());
        assertNull(Beans.copyAs(plain, D7Bean.class).getExtra());

        plain.setExtra(null);
        assertEquals("v", Beans.copyAs(plain, D7Bean.class).getValue());
        assertNull(Beans.copyAs(plain, D7Bean.class).getExtra());

        // Copying the other way is unaffected: the bean's own value for the omitted property is still readable.
        final D7Plain back = Beans.copyAs(bean, D7Plain.class);
        assertEquals("v", back.getValue());
        assertNull(back.getExtra());
    }
}
