package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.util.u.Nullable;

/**
 * Regression tests for the 2026-09-01 {@link Beans} review fixes. One nested section per finding; see
 * {@code scripts/cross_review/Beans_ledger_2026-09-01.md}.
 */
public class BeansRegressionCTest extends TestBase {

    // ============================================================ B1 - discovery must not run user code
    //                                                                   destructively, nor under the lock

    /** Its {@code <clinit>} throws, so reading its constant raises an Error rather than an Exception. */
    public static class ThrowingInitializer {
        public static final String MARKER = boom();

        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        static String boom() {
            throw new IllegalStateException("deliberate <clinit> failure");
        }
    }

    /**
     * Reading a class's {@code public static final String} constants runs its static initializer. When that
     * initializer throws, {@link Field#get} raises {@link ExceptionInInitializerError} - an {@link Error}, so
     * the {@code catch (Exception)} that used to guard it did not apply, and the Error escaped straight out of
     * {@link Beans#isBeanClass(Class)}, a method documented to answer {@code true}/{@code false}. Worse, every
     * later touch of the now-erroneous class raised {@link NoClassDefFoundError}, so one metadata query broke
     * the class permanently.
     */
    @Test
    public void testB1_isBeanClass_survivesAThrowingStaticInitializer() {
        // The class still has a getter/setter pair, so it is a bean; only the constants are unreadable.
        assertTrue(Beans.isBeanClass(ThrowingInitializer.class));
        // ... and the failure is not sticky.
        assertTrue(Beans.isBeanClass(ThrowingInitializer.class));
        assertEquals(List.of("name"), new ArrayList<>(Beans.getPropNameList(ThrowingInitializer.class)));
        assertNotNull(Beans.getPropGetter(ThrowingInitializer.class, "name"));
        assertNotNull(Beans.getPropSetter(ThrowingInitializer.class, "name"));
        assertNotNull(Beans.getPropField(ThrowingInitializer.class, "name"));
        assertEquals(Set.of("name"), Beans.getPropGetters(ThrowingInitializer.class).keySet());

        // Actually *instantiating* the class still fails, and must: the JVM marks a class whose initializer
        // threw as erroneous for good. Only the metadata queries are required to stay total.
        assertThrows(NoClassDefFoundError.class, () -> Beans.newBean(ThrowingInitializer.class));
    }

    /** Its {@code <clinit>} blocks until the test releases it. */
    public static class BlockingInitializer {
        static final CountDownLatch STARTED = new CountDownLatch(1);
        static final CountDownLatch RELEASE = new CountDownLatch(1);

        public static final String MARKER = block();

        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        static String block() {
            STARTED.countDown();

            try {
                RELEASE.await(30, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return "marker";
        }
    }

    public static class UnrelatedToBlocking {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    /**
     * Discovery used to hold one process-wide monitor for the whole scan, application code included. A single
     * bean whose static initializer (or constructor, or JAXB getter) waited on another thread therefore froze
     * introspection of every other class. The scan now runs outside that monitor.
     */
    @Test
    public void testB1_introspectingABlockingClassDoesNotStallUnrelatedClasses() throws Exception {
        final Thread blocked = new Thread(() -> Beans.isBeanClass(BlockingInitializer.class), "beans-blocking-clinit");
        blocked.setDaemon(true);

        try {
            blocked.start();
            assertTrue(BlockingInitializer.STARTED.await(15, TimeUnit.SECONDS), "the blocking <clinit> never started");

            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final Thread other = new Thread(() -> {
                try {
                    assertTrue(Beans.isBeanClass(UnrelatedToBlocking.class));
                    assertEquals(List.of("value"), new ArrayList<>(Beans.getPropNameList(UnrelatedToBlocking.class)));
                } catch (final Throwable t) {
                    failure.set(t);
                }
            }, "beans-unrelated");
            other.setDaemon(true);
            other.start();
            other.join(TimeUnit.SECONDS.toMillis(15));

            assertFalse(other.isAlive(), "introspecting an unrelated class blocked behind another class's <clinit>");
            assertNull(failure.get(), () -> String.valueOf(failure.get()));
        } finally {
            BlockingInitializer.RELEASE.countDown();
            blocked.join(TimeUnit.SECONDS.toMillis(15));
        }
    }

    // ============================================================ B2 - a non-bean registration is inherited

    public static class RegBase {
        private String baseProp;

        public String getBaseProp() {
            return baseProp;
        }

        public void setBaseProp(final String baseProp) {
            this.baseProp = baseProp;
        }
    }

    /** Introspected BEFORE its base is registered, to prove the registration invalidates cached subtypes. */
    public static class RegSubWarmedFirst extends RegBase {
        private String ownProp;

        public String getOwnProp() {
            return ownProp;
        }

        public void setOwnProp(final String ownProp) {
            this.ownProp = ownProp;
        }
    }

    /** Introspected AFTER, to prove the outcome does not depend on introspection order. */
    public static class RegSubWarmedLater extends RegBase {
        private String ownProp;

        public String getOwnProp() {
            return ownProp;
        }

        public void setOwnProp(final String ownProp) {
            this.ownProp = ownProp;
        }
    }

    /**
     * The hierarchy walk skipped a registered level, which covered that level's declared <i>fields</i> - but the
     * accessor scan reads {@code clazz.getMethods()}, which also returns inherited public methods, so the
     * subclass level re-discovered everything the registered superclass declared. The registration therefore
     * only reordered a subclass's properties instead of removing them.
     */
    @Test
    public void testB2_registeringABaseClassRemovesItsPropertiesFromSubclasses() {
        assertEquals(List.of("baseProp", "ownProp"), new ArrayList<>(Beans.getPropNameList(RegSubWarmedFirst.class)));

        Beans.registerNonBeanClass(RegBase.class);

        // Already introspected before the registration ...
        assertEquals(List.of("ownProp"), new ArrayList<>(Beans.getPropNameList(RegSubWarmedFirst.class)));
        // ... and introspected for the first time after it: same answer either way.
        assertEquals(List.of("ownProp"), new ArrayList<>(Beans.getPropNameList(RegSubWarmedLater.class)));

        // Not just the name list: every derived view agrees.
        assertEquals(Set.of("ownProp"), Beans.getPropGetters(RegSubWarmedFirst.class).keySet());
        assertEquals(Set.of("ownProp"), Beans.getPropSetters(RegSubWarmedFirst.class).keySet());
        assertEquals(Set.of("ownProp"), Beans.getPropFields(RegSubWarmedFirst.class).keySet());

        final RegSubWarmedFirst bean = new RegSubWarmedFirst();
        bean.setBaseProp("b");
        bean.setOwnProp("o");
        assertEquals(Map.of("ownProp", "o"), Beans.beanToMap(bean));
    }

    public static class ExtendsJavaUtilDate extends java.util.Date {
        private static final long serialVersionUID = 1L;

        private String label;

        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }
    }

    /**
     * {@code java.util.Date} is one of this class's own built-in non-bean registrations, yet a bean extending it
     * used to expose {@code year}, {@code time}, {@code seconds}, {@code month}, {@code hours}, {@code minutes}
     * and {@code date} as read/write properties - so {@code clearAllProps} zeroed the timestamp and
     * {@code randomize} scrambled it.
     */
    @Test
    public void testB2_builtInNonBeanRegistrationsApplyToUserSubclasses() {
        assertEquals(List.of("label"), new ArrayList<>(Beans.getPropNameList(ExtendsJavaUtilDate.class)));

        final ExtendsJavaUtilDate bean = new ExtendsJavaUtilDate();
        bean.setTime(123_456L);
        bean.setLabel("x");

        assertEquals(Map.of("label", "x"), Beans.beanToMap(bean));

        Beans.clearAllProps(bean);
        assertEquals(123_456L, bean.getTime(), "clearAllProps must not touch the inherited non-bean state");
    }

    // ============================================================ B3 - the registry outranks @Entity/record

    @Entity
    public static class RegisteredAnnotatedEntity {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public record RegisteredRecord(String x, int y) {
    }

    /**
     * {@code isBeanClass} consulted the {@code @Entity}/record rules before (and independently of) the explicit
     * registry, so a registered annotated entity answered {@code true} while {@code getPropNameList} answered
     * {@code []} - a pair no caller can act on, and the very invariant the registration APIs exist to keep.
     */
    @Test
    public void testB3_registerNonBeanClass_overridesEntityAnnotationAndRecords() {
        assertTrue(Beans.isBeanClass(RegisteredAnnotatedEntity.class));
        assertTrue(Beans.isBeanClass(RegisteredRecord.class));

        Beans.registerNonBeanClass(RegisteredAnnotatedEntity.class);
        Beans.registerNonBeanClass(RegisteredRecord.class);

        for (final Class<?> cls : List.of(RegisteredAnnotatedEntity.class, RegisteredRecord.class)) {
            assertFalse(Beans.isBeanClass(cls), cls + " is registered as a non-bean class");
            assertTrue(Beans.getPropNameList(cls).isEmpty(), cls + " must expose no properties");

            // isBeanClass and the getProp* family must agree: "not a bean" is an error, not a silent null.
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(cls, "value"));
            assertTrue(e.getMessage().contains("registered as a non-bean class"), e.getMessage());
        }
    }

    // ============================================================ B4 - getPropField resolves the same
    //                                                                   spellings as its siblings

    public static class FieldOnlyProp {
        public String user_name; //NOSONAR - deliberately a bare public field
    }

    public static class GetterBackedProp {
        private String user_name; //NOSONAR - matches FieldOnlyProp's spelling on purpose

        public String getUser_name() {
            return user_name;
        }

        public void setUser_name(final String v) {
            user_name = v;
        }
    }

    /**
     * The alias scan walked the <i>getter</i> map and then looked the matched key up in the field map, so a
     * property backed solely by a public field could only ever be found by its exact spelling. It also stopped
     * at the first getter whose name matched even when that name had no field, hiding any later match.
     */
    @Test
    public void testB4_getPropField_resolvesAliasesForAFieldOnlyProperty() {
        assertEquals(List.of("user_name"), new ArrayList<>(Beans.getPropNameList(FieldOnlyProp.class)));

        // Whatever spelling resolves for a getter-backed "user_name" must resolve for a field-only one too.
        for (final String spelling : List.of("user_name", "USER_NAME", "User_Name", "username", "getUser_name")) {
            final boolean getterBacked = Beans.getPropField(GetterBackedProp.class, spelling) != null;
            final boolean fieldOnly = Beans.getPropField(FieldOnlyProp.class, spelling) != null;

            assertEquals(getterBacked, fieldOnly, "spelling '" + spelling + "' resolved differently for a field-only property");
        }

        assertEquals("user_name", Beans.getPropField(FieldOnlyProp.class, "USER_NAME").getName());
        assertNull(Beans.getPropField(FieldOnlyProp.class, "noSuchProperty"));
    }

    // ============================================================ B5 - WITHDRAWN: a propFilter is a
    //                                                                   selection, and that is intended

    public static class WideSource {
        private String name;
        private Integer age;
        private String secret;

        public WideSource() {
        }

        public WideSource(final String name, final Integer age, final String secret) {
            this.name = name;
            this.age = age;
            this.secret = secret;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(final Integer age) {
            this.age = age;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(final String secret) {
            this.secret = secret;
        }
    }

    public static class NarrowTarget {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(final Integer age) {
            this.age = age;
        }
    }

    /**
     * The review flagged the {@code propFilter} overloads for throwing where their unfiltered siblings skip.
     * That turned out to be a deliberate, test-locked contract rather than a defect: a filter is read as a
     * <i>selection</i>, so a property that passes it must exist on both sides, exactly like an explicit
     * {@code selectPropNames} collection. Pinned here alongside the lenient siblings, because the pair is
     * easy to "fix" into a regression - the javadoc of all four filter overloads now says so.
     */
    @Test
    public void testB5_aPropFilterIsASelection_soUnmatchedTargetPropertiesAreAnError() {
        final WideSource src = new WideSource("John", 25, "s3cret");

        // Lenient: no filter, unmatched "secret" silently skipped. This is what makes DTO narrowing work.
        assertEquals(Map.of("name", "John", "age", 25), Beans.beanToMap(Beans.copyAs(src, NarrowTarget.class)));
        assertEquals(Map.of("name", "John", "age", 25), Beans.beanToMap(Beans.mergeInto(src, new NarrowTarget())));
        assertEquals(Map.of("name", "John", "age", 25), Beans.beanToMap(Beans.mergeInto(src, new NarrowTarget(), true, (Set<String>) null)));
        assertEquals(Map.of("name", "John", "age", 25), Beans.beanToMap(Beans.mergeInto(src, new NarrowTarget(), Fn.o((a, b) -> a))));

        // Strict: "secret" passes the filter but the target has no such property.
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(src, (n, v) -> true, NarrowTarget.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(src, (n, v) -> true, Fn.identity(), NarrowTarget.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeIntoIf(src, new NarrowTarget(), Fn.p((n, v) -> true)));
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeIntoIf(src, new NarrowTarget(), Fn.p((n, v) -> true), Fn.o((a, b) -> a)));

        // Excluding it in the filter is the documented way to narrow.
        final NarrowTarget narrowed = Beans.copyAs(src, (n, v) -> !"secret".equals(n), NarrowTarget.class);
        assertEquals(Map.of("name", "John", "age", 25), Beans.beanToMap(narrowed));

        // ... and the strict path is all-or-nothing: nothing is written before the rejection.
        final NarrowTarget untouched = new NarrowTarget();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeIntoIf(src, untouched, Fn.p((n, v) -> true)));
        assertNull(untouched.getName());
        assertNull(untouched.getAge());

        // An EXPLICITLY selected name likewise has to exist on both sides.
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(src, new NarrowTarget(), List.of("secret")));
    }

    // ============================================================ D1/J1 - map suppliers must be passable
    //                                                                     as an ordinary lambda / method ref

    public static class SupplierBean {
        private String name;
        private Integer age;
        private FieldOnlyProp ignored;

        public SupplierBean(final String name, final Integer age) {
            this.name = name;
            this.age = age;
        }

        public SupplierBean() {
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(final Integer age) {
            this.age = age;
        }

        public FieldOnlyProp getIgnored() {
            return ignored;
        }

        public void setIgnored(final FieldOnlyProp ignored) {
            this.ignored = ignored;
        }
    }

    /**
     * Every {@code (..., IntFunction<? extends M> mapSupplier)} overload collided with a
     * {@code (..., M output)} twin: {@code M} is the method's own type parameter, so an implicitly typed lambda
     * or an inexact method reference stayed potentially compatible with both and the call was ambiguous. The
     * output overloads no longer declare a type variable they never used, so these all resolve.
     *
     * <p>This test compiling <i>is</i> the assertion; the values are checked as a bonus.</p>
     */
    @Test
    public void testD1_mapSupplierOverloadsAcceptALambdaOrMethodReference() {
        final SupplierBean bean = new SupplierBean("John", 25);
        final Set<String> none = Set.of();
        final List<String> both = List.of("name", "age");

        assertEquals("{age=25, name=John}", Beans.beanToMap(bean, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToMap(bean, both, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToMap(bean, both, NamingPolicy.CAMEL_CASE, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToMap(bean, true, none, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToMap(bean, true, none, NamingPolicy.CAMEL_CASE, size -> new TreeMap<>()).toString());

        assertEquals("{age=25, name=John}", Beans.deepBeanToMap(bean, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.deepBeanToMap(bean, both, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.deepBeanToMap(bean, both, NamingPolicy.CAMEL_CASE, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.deepBeanToMap(bean, true, none, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.deepBeanToMap(bean, true, none, NamingPolicy.CAMEL_CASE, size -> new TreeMap<>()).toString());

        assertEquals("{age=25, name=John}", Beans.beanToFlatMap(bean, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToFlatMap(bean, both, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToFlatMap(bean, both, NamingPolicy.CAMEL_CASE, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToFlatMap(bean, true, none, size -> new TreeMap<>()).toString());
        assertEquals("{age=25, name=John}", Beans.beanToFlatMap(bean, true, none, NamingPolicy.CAMEL_CASE, size -> new TreeMap<>()).toString());

        // An inexact method reference works too - this is the shape users reach for first.
        final Map<String, Object> viaMethodRef = Beans.beanToMap(bean, HashMap::new);
        assertEquals(Map.of("name", "John", "age", 25), viaMethodRef);
        assertEquals(Map.of("name", "John", "age", 25), Beans.deepBeanToMap(bean, LinkedHashMap::new));
        assertEquals(Map.of("name", "John", "age", 25), Beans.beanToFlatMap(bean, LinkedHashMap::new));
    }

    /** The in-place overloads still accept any {@code Map<String, Object>} and still preserve existing entries. */
    @Test
    public void testD1_inPlaceOverloadsStillAcceptEveryMapImplementation() {
        final SupplierBean bean = new SupplierBean("John", 25);

        final TreeMap<String, Object> tree = new TreeMap<>();
        tree.put("id", 1);
        Beans.beanToMap(bean, tree);
        assertEquals("{age=25, id=1, name=John}", tree.toString());

        final LinkedHashMap<String, Object> linked = new LinkedHashMap<>();
        Beans.deepBeanToMap(bean, linked);
        assertEquals("{name=John, age=25}", linked.toString());

        final Map<String, Object> plain = new HashMap<>();
        Beans.beanToFlatMap(bean, plain);
        assertEquals(Map.of("name", "John", "age", 25), plain);

        final HashMap<String, Object> withPolicy = new HashMap<>();
        Beans.beanToMap(bean, true, Set.of("age"), NamingPolicy.SCREAMING_SNAKE_CASE, withPolicy);
        assertEquals(Map.of("NAME", "John"), withPolicy);
    }

    // ============================================================ D3 - one shared dotted-path resolver

    public static class Leaf {
        private String city;
        private int zip;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }

        public int getZip() {
            return zip;
        }

        public void setZip(final int zip) {
            this.zip = zip;
        }
    }

    public static class Root {
        private String name;
        private Leaf leaf;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Leaf getLeaf() {
            return leaf;
        }

        public void setLeaf(final Leaf leaf) {
            this.leaf = leaf;
        }
    }

    /**
     * {@code getPropValue(bean, name, ignoreUnmatched)} and {@code getPropValueIfPresent} carried a verbatim
     * copy of the path resolver each. They now share one, so the set of paths that resolve cannot drift; their
     * documented difference is only in how they report an unreachable leaf.
     */
    @Test
    public void testD3_dottedPathResolutionIsSharedAndUnchanged() {
        final Root withLeaf = new Root();
        withLeaf.setName("n");
        final Leaf leaf = new Leaf();
        leaf.setCity("NYC");
        leaf.setZip(10_001);
        withLeaf.setLeaf(leaf);

        assertEquals("NYC", Beans.getPropValue(withLeaf, "leaf.city"));
        assertEquals(10_001, (int) Beans.getPropValue(withLeaf, "leaf.zip"));
        assertEquals(Nullable.of("NYC"), Beans.getPropValueIfPresent(withLeaf, "leaf.city"));

        final Root empty = new Root();
        // Unreachable path: the leaf type's default for getPropValue ...
        assertNull(Beans.getPropValue(empty, "leaf.city"));
        assertEquals(0, (int) Beans.getPropValue(empty, "leaf.zip"));
        // ... and "absent" for the Nullable-returning sibling.
        assertTrue(Beans.getPropValueIfPresent(empty, "leaf.city").isEmpty());
        assertTrue(Beans.getPropValueIfPresent(empty, "leaf.zip").isEmpty());

        // A present-but-null leaf is reported as present.
        leaf.setCity(null);
        assertEquals(Nullable.of((String) null), Beans.getPropValueIfPresent(withLeaf, "leaf.city"));

        // Both agree that these do not resolve.
        assertTrue(Beans.getPropValueIfPresent(withLeaf, "leaf.nope").isEmpty());
        assertTrue(Beans.getPropValueIfPresent(withLeaf, "name.length").isEmpty());
        assertNull(Beans.getPropValue(withLeaf, "leaf.nope", true));
        assertNull(Beans.getPropValue(withLeaf, "name.length", true));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(withLeaf, "leaf.nope", false));
    }

    // ============================================================ D4 - one definition of the key rule

    /**
     * The "CAMEL_CASE and NO_CHANGE emit property names verbatim" rule was written out inline at four sites.
     * All four now call one predicate, so the shallow, deep and flat conversions cannot disagree.
     */
    @Test
    public void testD4_namingPolicyIsAppliedIdenticallyByEveryConversionShape() {
        final Root root = new Root();
        root.setName("n");
        final Leaf leaf = new Leaf();
        leaf.setCity("NYC");
        root.setLeaf(leaf);

        for (final NamingPolicy verbatim : List.of(NamingPolicy.CAMEL_CASE, NamingPolicy.NO_CHANGE)) {
            assertTrue(Beans.beanToMap(root, false, null, verbatim).containsKey("name"), String.valueOf(verbatim));
            assertTrue(Beans.deepBeanToMap(root, false, null, verbatim).containsKey("leaf"), String.valueOf(verbatim));
            assertTrue(Beans.beanToFlatMap(root, false, null, verbatim).containsKey("leaf.city"), String.valueOf(verbatim));
        }

        assertTrue(Beans.beanToMap(root, false, null, NamingPolicy.SCREAMING_SNAKE_CASE).containsKey("NAME"));
        assertTrue(Beans.deepBeanToMap(root, false, null, NamingPolicy.SCREAMING_SNAKE_CASE).containsKey("LEAF"));
        assertTrue(Beans.beanToFlatMap(root, false, null, NamingPolicy.SCREAMING_SNAKE_CASE).containsKey("LEAF.CITY"));

        // A null policy means CAMEL_CASE everywhere. (The cast is required because a bare `null` cannot pick
        // between the NamingPolicy overload and the output-map one - true before this review's changes too.)
        assertTrue(Beans.beanToMap(root, false, null, (NamingPolicy) null).containsKey("name"));
        assertTrue(Beans.deepBeanToMap(root, false, null, (NamingPolicy) null).containsKey("leaf"));
        assertTrue(Beans.beanToFlatMap(root, false, null, (NamingPolicy) null).containsKey("leaf.city"));
    }

    // ============================================================ D5 - writer and reader share one gate

    /**
     * {@code deepBeanToMap} decided "this property is a nested bean" from {@code jsonXmlType} while
     * {@code mapToBean} decided the same thing from {@code type}. The two now ask one predicate, so the
     * round trip the class documents holds by construction.
     */
    @Test
    public void testD5_deepBeanToMapAndMapToBeanAgreeOnWhatIsANestedBean() {
        final Root root = new Root();
        root.setName("n");
        final Leaf leaf = new Leaf();
        leaf.setCity("NYC");
        leaf.setZip(10_001);
        root.setLeaf(leaf);

        final Map<String, Object> deep = Beans.deepBeanToMap(root);
        assertTrue(deep.get("leaf") instanceof Map, "a nested bean must be written as a nested map");

        final Root back = Beans.mapToBean(deep, Root.class);
        assertEquals("n", back.getName());
        assertEquals("NYC", back.getLeaf().getCity());
        assertEquals(10_001, back.getLeaf().getZip());

        // The flat shape round-trips through the dotted-key form too.
        final Root fromFlat = Beans.mapToBean(Beans.beanToFlatMap(root), Root.class);
        assertEquals("NYC", fromFlat.getLeaf().getCity());
    }

    // ============================================================ D6 - a builder's build()/setters must be
    //                                                                   instance methods

    public static class StaticBuildBean {
        private String name;

        public String getName() {
            return name;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            /** Static, so invoking it on a populated builder would silently discard that builder. */
            public static StaticBuildBean build() {
                return new StaticBuildBean();
            }
        }
    }

    public static class GoodBuilderBean {
        private final String name;

        GoodBuilderBean(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;

            /** A static factory, not a property setter. */
            public static Builder of(final String name) {
                return new Builder().name(name);
            }

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public GoodBuilderBean build() {
                return new GoodBuilderBean(name);
            }
        }
    }

    @Test
    public void testD6_builderDetectionRequiresInstanceBuildAndSetterMethods() {
        assertNull(Beans.getBuilderInfo(StaticBuildBean.class), "a static build() cannot build a populated builder");

        final Beans.BuilderInfo info = Beans.getBuilderInfo(GoodBuilderBean.class);
        assertNotNull(info);
        assertSame(GoodBuilderBean.Builder.class, info.builderClass());

        // The builder's setter map is derived while introspecting the BEAN, so introspect that first.
        assertEquals(List.of("name"), new ArrayList<>(Beans.getPropNameList(GoodBuilderBean.class)));

        // The static factory `of(String)` must not be mistaken for a property setter.
        assertFalse(Beans.getPropSetters(GoodBuilderBean.Builder.class).containsKey("of"));
        assertTrue(Beans.getPropSetters(GoodBuilderBean.Builder.class).containsKey("name"));

        // And the builder path still works end to end.
        final GoodBuilderBean built = Beans.mapToBean(Map.of("name", "John"), GoodBuilderBean.class);
        assertEquals("John", built.getName());
    }

    // ============================================================ O1 - the length cap measures the name

    /** The 128-character cap was applied to the untrimmed input, so padding alone could defeat a lookup. */
    @Test
    public void testO1_propertyNameLengthCapIgnoresSurroundingWhitespace() {
        final String padded = Strings.repeat(" ", 200) + "value" + Strings.repeat(" ", 200);

        assertNotNull(Beans.getPropGetter(UnrelatedToBlocking.class, padded));
        assertNotNull(Beans.getPropSetter(UnrelatedToBlocking.class, padded));
        assertNotNull(Beans.getPropField(UnrelatedToBlocking.class, padded));

        // A genuinely over-long name is still "not found" rather than an error, in every tolerant mode.
        final String tooLong = Strings.repeat("x", 129);
        assertNull(Beans.getPropGetter(UnrelatedToBlocking.class, tooLong));

        final UnrelatedToBlocking bean = Beans.newBean(UnrelatedToBlocking.class);
        assertNull(Beans.getPropValue(bean, tooLong, true));
        assertNotNull(Beans.mapToBean(Map.of(tooLong, "v"), true, UnrelatedToBlocking.class));
    }

    // ============================================================ J2 - the documented property model

    public static class MixedModel {
        public String tag; //NOSONAR - a bare public field is a property on its own
        private String name;
        private String secret;
        private String label;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        /** Setter with no getter: not a property. */
        public void setSecret(final String secret) {
            this.secret = secret;
        }

        /** Getter with no setter on an ordinary mutable class: not a property. */
        public String getLabel() {
            return label;
        }

        /** No field and no setter: not a property. */
        public String getComputed() {
            return name + label;
        }
    }

    /**
     * {@code getPropNameList}'s javadoc claimed "all properties that have getter and/or setter methods", which
     * was wrong in both directions. This pins the model the rewritten javadoc now describes.
     */
    @Test
    public void testJ2_propertyModelMatchesTheDocumentedRule() {
        assertEquals(List.of("tag", "name"), new ArrayList<>(Beans.getPropNameList(MixedModel.class)));

        // A class with nothing but public fields is still a bean.
        assertTrue(Beans.isBeanClass(FieldOnlyProp.class));
        assertEquals(List.of("user_name"), new ArrayList<>(Beans.getPropNameList(FieldOnlyProp.class)));

        // Superclass-first ordering, as documented.
        assertEquals(List.of("baseProp", "ownProp"), new ArrayList<>(Beans.getPropNameList(OrderSub.class)));
    }

    public static class OrderBase {
        private String baseProp;

        public String getBaseProp() {
            return baseProp;
        }

        public void setBaseProp(final String v) {
            baseProp = v;
        }
    }

    public static class OrderSub extends OrderBase {
        private String ownProp;

        public String getOwnProp() {
            return ownProp;
        }

        public void setOwnProp(final String v) {
            ownProp = v;
        }
    }

    // ============================================================ J4 - exclude is top-level only

    /** {@code filter} and {@code ignoredPropNames} both documented this; {@code exclude} did not. */
    @Test
    public void testJ4_builderExcludeAppliesToTopLevelPropertiesOnly() {
        final Root root = new Root();
        root.setName("n");
        final Leaf leaf = new Leaf();
        leaf.setCity("NYC");
        root.setLeaf(leaf);

        assertEquals(Map.of("leaf", Map.of("city", "NYC", "zip", 0)), Beans.mapBuilder(root).deep().exclude("name").toMap());

        // "city" is a nested property, so excluding it at the top level changes nothing.
        final Map<String, Object> deep = Beans.mapBuilder(root).deep().exclude("city").toMap();
        assertEquals("NYC", ((Map<?, ?>) deep.get("leaf")).get("city"));

        final Map<String, Object> flat = Beans.mapBuilder(root).flat().exclude("city").toMap();
        assertEquals("NYC", flat.get("leaf.city"));

        // Matching the documented behaviour of the exclusion-based overloads.
        assertEquals("NYC", ((Map<?, ?>) Beans.deepBeanToMap(root, false, Set.of("city")).get("leaf")).get("city"));
    }

    // ============================================================ J6 - randomize's e-mail heuristic

    public static class RandomizeMe {
        private String emailAddress;
        private String plain;

        public String getEmailAddress() {
            return emailAddress;
        }

        public void setEmailAddress(final String v) {
            emailAddress = v;
        }

        public String getPlain() {
            return plain;
        }

        public void setPlain(final String v) {
            plain = v;
        }
    }

    /** Documented on {@code randomize(Object)}: a name containing "email" gets an address-shaped value. */
    @Test
    public void testJ6_randomizeUsesAnAddressShapedValueForEmailNamedProperties() {
        final RandomizeMe bean = Beans.newRandomBean(RandomizeMe.class);

        assertTrue(bean.getEmailAddress().endsWith("@email.com"), bean.getEmailAddress());
        assertEquals(12, bean.getEmailAddress().indexOf('@'));

        assertEquals(16, bean.getPlain().length());
        assertFalse(bean.getPlain().contains("@"));
    }

    // ============================================================ registration bookkeeping

    /**
     * Every registration API now routes through one path that invalidates the class <i>and</i> its already
     * introspected subtypes. The two that skipped that sweep are covered above; this pins that the two that
     * always had it still do, and that a registration is visible to a scan that is already in flight.
     */
    @Test
    public void testRegistrationsInvalidateEveryDerivedPool() throws Exception {
        assertEquals(List.of("baseProp", "ownProp"), new ArrayList<>(Beans.getPropNameList(SweepSub.class)));

        Beans.registerNonPropertyAccessor(SweepBase.class, "baseProp");

        assertEquals(List.of("ownProp"), new ArrayList<>(Beans.getPropNameList(SweepSub.class)));
        assertFalse(Beans.getPropGetters(SweepSub.class).containsKey("baseProp"));

        // registerPropertyAccessor also sweeps, and validates its argument before touching anything.
        final Method notAnAccessor = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("x", notAnAccessor));

        // The metadata epoch advances on every registration, which is what lets an in-flight scan notice.
        final AtomicLong epoch = new AtomicLong(readEpoch());
        Beans.registerNonPropertyAccessor(SweepBase.class, "anotherName");
        assertTrue(readEpoch() > epoch.get(), "a registration must advance the metadata epoch");
    }

    private static long readEpoch() throws Exception {
        final java.lang.reflect.Field f = Beans.class.getDeclaredField("beanMetadataEpoch");
        f.setAccessible(true); //NOSONAR
        return ((java.util.concurrent.atomic.AtomicLong) f.get(null)).get();
    }

    public static class SweepBase {
        private String baseProp;

        public String getBaseProp() {
            return baseProp;
        }

        public void setBaseProp(final String v) {
            baseProp = v;
        }
    }

    public static class SweepSub extends SweepBase {
        private String ownProp;

        public String getOwnProp() {
            return ownProp;
        }

        public void setOwnProp(final String v) {
            ownProp = v;
        }
    }

    // ============================================================ nothing else regressed

    /** A broad smoke test over the surfaces the structural changes above touch. */
    @Test
    public void testConversionSurfaceStillBehaves() {
        final Root root = new Root();
        root.setName("n");
        final Leaf leaf = new Leaf();
        leaf.setCity("NYC");
        leaf.setZip(7);
        root.setLeaf(leaf);

        assertEquals("{name=n, leaf=" + leaf + "}", Beans.beanToMap(root).toString());
        assertEquals("{name=n, leaf={city=NYC, zip=7}}", Beans.deepBeanToMap(root).toString());
        assertEquals("{name=n, leaf.city=NYC, leaf.zip=7}", Beans.beanToFlatMap(root).toString());
        assertEquals("{name=n, leaf={city=NYC, zip=7}}", Beans.mapBuilder(root).deep().skipNulls().toMap().toString());
        assertEquals("{leaf.city=NYC, leaf.zip=7}", Beans.mapBuilder(root).flat().exclude("name").toMap().toString());

        assertEquals(Arrays.asList("name", "leaf"), Beans.getPropNames(root, false));
        assertEquals(2, Beans.stream(root).count());

        final Root copy = Beans.copy(root);
        assertEquals("NYC", copy.getLeaf().getCity());

        Beans.clearProps(copy, "name");
        assertNull(copy.getName());
        assertNotNull(copy.getLeaf());

        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(copy, "name", "nope"));
    }
}
