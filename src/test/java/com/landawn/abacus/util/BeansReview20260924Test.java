package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-24 Beans review (ledger C-401..C-424).
 */
public class BeansReview20260924Test extends TestBase {

    // ---------------------------------------------------------------- fixtures

    public static class Address {
        private String city;

        public Address() {
        }

        public Address(final String city) {
            this.city = city;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class Person {
        private String firstName;
        private int age;
        private String password;
        private Address address;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(final Address address) {
            this.address = address;
        }
    }

    public static class UnicodeBean {
        private String naïve;
        private int count;

        public String getNaïve() {
            return naïve;
        }

        public void setNaïve(final String naïve) {
            this.naïve = naïve;
        }

        public int getCount() {
            return count;
        }

        public void setCount(final int count) {
            this.count = count;
        }
    }

    /** A setter-only property next to a paired one (C-406). */
    public static class SecretBean {
        private int age;
        private int secretValue;

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public void setSecret(final int secret) {
            this.secretValue = secret;
        }

        public int secretValue() {
            return secretValue;
        }
    }

    /** An overloaded setter whose parameter type differs from the property type (C-406). */
    public static class OverloadBean {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public void setCode(final int code) {
            this.code = "#" + code;
        }
    }

    /** Builder-based immutable bean (C-406: a builder setter is not a bean setter). */
    public static class BuiltBean {
        private final int age;
        private final String name;

        BuiltBean(final int age, final String name) {
            this.age = age;
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public String getName() {
            return name;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int age;
            private String name;

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public BuiltBean build() {
                return new BuiltBean(age, name);
            }
        }
    }

    /** Methods that are not property accessors, each for a different reason (C-411). */
    public static class NotAccessors {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public static String getStat() {
            return "s";
        }

        public String getWithArg(final int i) {
            return name + i;
        }

        public void getNothing() {
        }

        public String compute() {
            return name;
        }

        public void setTwo(final int a, final int b) {
        }

        public String setBad(final int a) {
            return "x";
        }
    }

    public static class RandNum {
        private Number num;
        private String name;

        public Number getNum() {
            return num;
        }

        public void setNum(final Number num) {
            this.num = num;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public abstract static class PAbs {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class PConcrete extends PAbs {
    }

    public static class RandAbsHolder {
        private String name;
        private PAbs abs;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public PAbs getAbs() {
            return abs;
        }

        public void setAbs(final PAbs abs) {
            this.abs = abs;
        }
    }

    private static Person person() {
        final Person p = new Person();
        p.setFirstName("Ada");
        p.setAge(36);
        p.setPassword("secret");
        p.setAddress(new Address("London"));
        return p;
    }

    private static IntFunction<Map<String, Object>> nullSupplier() {
        return n -> null;
    }

    private static void assertSupplierNull(final org.junit.jupiter.api.function.Executable executable) {
        final NullPointerException e = assertThrows(NullPointerException.class, executable);
        assertEquals("mapSupplier returned null", e.getMessage());
    }

    // ---------------------------------------------------------------- C-406

    @Test
    public void testC406_setterOnlyMethodConvertsToItsOwnParameterType() throws Exception {
        final SecretBean bean = new SecretBean();
        final Method setSecret = SecretBean.class.getMethod("setSecret", int.class);

        assertEquals(25, Beans.setPropValue(bean, setSecret, "25"));
        assertEquals(25, bean.secretValue());

        // the paired setter keeps working
        assertEquals(30, Beans.setPropValue(bean, SecretBean.class.getMethod("setAge", int.class), "30"));
        assertEquals(30, bean.getAge());

        // null -> the parameter type's default
        assertEquals(0, Beans.setPropValue(bean, setSecret, null));
        assertEquals(0, bean.secretValue());
    }

    @Test
    public void testC406_builderSetterConverts() throws Exception {
        final BuiltBean.Builder builder = BuiltBean.builder();

        assertEquals(25, Beans.setPropValue(builder, BuiltBean.Builder.class.getMethod("age", int.class), "25"));
        assertEquals("12", Beans.setPropValue(builder, BuiltBean.Builder.class.getMethod("name", String.class), 12));
        final BuiltBean built = builder.build();
        assertEquals(25, built.getAge());
        assertEquals("12", built.getName());
    }

    @Test
    public void testC406_overloadConvertsToTheOverloadsParameterType() throws Exception {
        final OverloadBean bean = new OverloadBean();

        Beans.setPropValue(bean, OverloadBean.class.getMethod("setCode", int.class), "7");
        assertEquals("#7", bean.getCode());

        // the paired String setter still converts to the property type
        Beans.setPropValue(bean, OverloadBean.class.getMethod("setCode", String.class), 8);
        assertEquals("8", bean.getCode());
    }

    @Test
    public void testC406_unconvertibleValueStillFailsAndNullBeanIsNpe() throws Exception {
        final Method setSecret = SecretBean.class.getMethod("setSecret", int.class);

        assertThrows(RuntimeException.class, () -> Beans.setPropValue(new SecretBean(), setSecret, "not-a-number"));
        assertThrows(NullPointerException.class, () -> Beans.setPropValue(null, setSecret, "1"));
    }

    // ---------------------------------------------------------------- C-407

    @Test
    public void testC407_nullMapStillValidatesSelection() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList("bogus"), Person.class));
        assertTrue(e.getMessage().contains("bogus"), e.getMessage());

        // the same selection is rejected for an empty map and by mapsToBeans - now all agree
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(new HashMap<>(), Arrays.asList("bogus"), Person.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(new ArrayList<Map<String, Object>>(), Arrays.asList("bogus"), Person.class));

        assertNull(Beans.mapToBean((Map<String, Object>) null, Arrays.asList("firstName", "FIRST_NAME", "address.city"), Person.class));
        assertNull(Beans.mapToBean((Map<String, Object>) null, Collections.<String> emptyList(), Person.class));
        assertNull(Beans.mapToBean((Map<String, Object>) null, (List<String>) null, Person.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList("naïveX"), UnicodeBean.class));
        assertNull(Beans.mapToBean((Map<String, Object>) null, Arrays.asList("NAÏVE"), UnicodeBean.class));
    }

    // ---------------------------------------------------------------- C-408

    @Test
    public void testC408_nullKeyIsSkippedInTolerantMode() {
        final Map<String, Object> map = new HashMap<>();
        map.put(null, "ignored");
        map.put("firstName", "Ada");
        map.put("unknown", 1);

        final Person p = Beans.mapToBean(map, true, Person.class);
        assertEquals("Ada", p.getFirstName());

        final Person p2 = Beans.mapToBean(map, Person.class);
        assertEquals("Ada", p2.getFirstName());

        final Map<String, Object> onlyNull = new HashMap<>();
        onlyNull.put(null, null);
        assertNotNull(Beans.mapToBean(onlyNull, true, Person.class));

        // strict mode still reports it
        final Map<String, Object> strict = new HashMap<>();
        strict.put(null, "x");
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(strict, false, Person.class));
    }

    // ---------------------------------------------------------------- C-411

    @Test
    public void testC411_rejectionMessageNamesTheFailedRule() throws Exception {
        assertRejected(NotAccessors.class.getMethod("getStat"), "static");
        assertRejected(NotAccessors.class.getMethod("getWithArg", int.class), "a getter must not take any parameter");
        assertRejected(NotAccessors.class.getMethod("getNothing"), "a getter must not return void");
        assertRejected(NotAccessors.class.getMethod("compute"), "must start with 'get/is/has' or 'set'");
        assertRejected(NotAccessors.class.getMethod("setTwo", int.class, int.class), "takes 2");
        assertRejected(NotAccessors.class.getMethod("setBad", int.class), "a setter must return void");
        assertRejected(Object.class.getMethod("toString"), "java.lang.Object");
        assertRejected(Object.class.getMethod("getClass"), "java.lang.Object");
    }

    private static void assertRejected(final Method method, final String expectedFragment) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("x", method));
        assertTrue(e.getMessage().contains(expectedFragment), e.getMessage());
        assertTrue(e.getMessage().contains(method.getName()), e.getMessage());
    }

    // ---------------------------------------------------------------- C-412 / C-418

    @Test
    public void testC412_beanToMapSupplierReturningNull() {
        final Person p = person();

        assertSupplierNull(() -> Beans.beanToMap(p, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToMap(null, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToMap(p, Arrays.asList("firstName"), nullSupplier()));
        assertSupplierNull(() -> Beans.beanToMap(p, Arrays.asList("firstName"), NamingPolicy.SNAKE_CASE, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToMap(null, Arrays.asList("firstName"), NamingPolicy.SNAKE_CASE, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToMap(p, false, (Set<String>) null, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToMap(null, true, (Set<String>) null, NamingPolicy.CAMEL_CASE, nullSupplier()));
    }

    @Test
    public void testC418_deepAndFlatSupplierReturningNull() {
        final Person p = person();

        assertSupplierNull(() -> Beans.deepBeanToMap(p, nullSupplier()));
        assertSupplierNull(() -> Beans.deepBeanToMap(null, nullSupplier()));
        assertSupplierNull(() -> Beans.deepBeanToMap(p, false, (Set<String>) null, nullSupplier()));
        assertSupplierNull(() -> Beans.deepBeanToMap(p, Arrays.asList("address"), nullSupplier()));
        assertSupplierNull(() -> Beans.beanToFlatMap(p, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToFlatMap(null, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToFlatMap(p, false, (Set<String>) null, nullSupplier()));
        assertSupplierNull(() -> Beans.beanToFlatMap(p, Arrays.asList("address"), nullSupplier()));
        assertSupplierNull(() -> Beans.mapBuilder(p).toMap(nullSupplier()));
        assertSupplierNull(() -> Beans.mapBuilder(null).toMap(nullSupplier()));
        assertSupplierNull(() -> Beans.mapBuilder(p).deep().toMap(nullSupplier()));
    }

    @Test
    public void testC418_nestedSupplierReturningNull() {
        final Person p = person();

        final IntFunction<Map<String, Object>> topOnly = new IntFunction<>() {
            private int calls = 0;

            @Override
            public Map<String, Object> apply(final int value) {
                return calls++ == 0 ? new LinkedHashMap<>() : null;
            }
        };
        assertSupplierNull(() -> Beans.deepBeanToMap(p, topOnly));

        final IntFunction<Map<String, Object>> topOnly2 = new IntFunction<>() {
            private int calls = 0;

            @Override
            public Map<String, Object> apply(final int value) {
                return calls++ == 0 ? new LinkedHashMap<>() : null;
            }
        };
        assertSupplierNull(() -> Beans.deepBeanToMap(p, Arrays.asList("firstName", "address"), topOnly2));

        // a well-behaved supplier is used at every level, unchanged
        final Map<String, Object> m = Beans.deepBeanToMap(p, n -> new TreeMap<>());
        assertTrue(m instanceof TreeMap);
        assertTrue(m.get("address") instanceof TreeMap);
        assertEquals("London", ((Map<?, ?>) m.get("address")).get("city"));
    }

    @Test
    public void testC418_nullBeanWithWorkingSupplierStillReturnsEmptyMap() {
        final Map<String, Object> m = Beans.beanToMap(null, n -> new TreeMap<>());
        assertTrue(m.isEmpty());
        assertTrue(Beans.mapBuilder(null).toMap(n -> new TreeMap<>()).isEmpty());
    }

    // ---------------------------------------------------------------- C-414

    @Test
    public void testC414_nonBeanMessagesNameTheReason() {
        assertNotABean(() -> Beans.getBeanInfo(String.class), "CharSequence");
        assertNotABean(() -> Beans.getPropNames("abc", n -> true), "CharSequence");
        assertNotABean(() -> Beans.getPropNames("abc", (n, v) -> true), "CharSequence");
        assertNotABean(() -> Beans.getPropNames("abc", false), "CharSequence");
        assertNotABean(() -> Beans.getPropNames("abc", true), "CharSequence");
        assertNotABean(() -> Beans.getPropValue("abc", "length", false), "CharSequence");
        assertNotABean(() -> Beans.getPropValue("abc", "length", true), "CharSequence");
        assertNotABean(() -> Beans.getPropValue(Integer.valueOf(1), "x"), "Number");
        assertNotABean(() -> Beans.getPropValueIfPresent(new ArrayList<>(), "x"), "Collection");
        assertNotABean(() -> Beans.getPropValueIfPresent(new HashMap<>(), "x"), "Map implementations");
    }

    @Test
    public void testC414_parameterizedNonBeanTypeNamesTheReason() throws Exception {
        final java.lang.reflect.Type listOfString = TypeCarrier.class.getDeclaredField("list").getGenericType();
        assertNotABean(() -> Beans.getBeanInfo(listOfString), "Collection");

        // a genuine bean is unaffected
        assertNotNull(Beans.getBeanInfo(Person.class));
        assertEquals(Arrays.asList("firstName"), Beans.getPropNames(person(), n -> n.startsWith("first")));
        assertEquals("Ada", Beans.getPropValue(person(), "firstName", false));
    }

    static class TypeCarrier {
        List<String> list;
    }

    private static void assertNotABean(final org.junit.jupiter.api.function.Executable executable, final String reason) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertTrue(e.getMessage().startsWith("Not a bean class: "), e.getMessage());
        assertTrue(e.getMessage().contains(reason), e.getMessage());
    }

    // ---------------------------------------------------------------- C-419

    @Test
    public void testC419_numberTypedPropertyGetsDefault() {
        final RandNum r = new RandNum();
        Beans.randomize(r);
        assertNull(r.getNum());
        assertNotNull(r.getName());

        final RandNum r2 = new RandNum();
        Beans.randomize(r2, Arrays.asList("num", "name"));
        assertNull(r2.getNum());
        assertNotNull(r2.getName());

        final RandNum r3 = Beans.newRandomBean(RandNum.class);
        assertNull(r3.getNum());
        assertNotNull(r3.getName());
        assertEquals(3, Beans.newRandomBeanList(RandNum.class, 3).size());
    }

    @Test
    public void testC419_abstractBeanTypedPropertyGetsDefault() {
        final RandAbsHolder h = new RandAbsHolder();
        Beans.randomize(h);
        assertNull(h.getAbs());
        assertNotNull(h.getName());

        final RandAbsHolder h2 = Beans.newRandomBean(RandAbsHolder.class);
        assertNull(h2.getAbs());
        assertNotNull(h2.getName());
    }

    // ---------------------------------------------------------------- C-420

    @Test
    public void testC420_copyToAbstractBeanIsIae() {
        final PConcrete src = new PConcrete();
        src.setName("x");

        assertAbstract(() -> Beans.copyAs(null, PAbs.class));
        assertAbstract(() -> Beans.copyAs(src, PAbs.class));
        assertAbstract(() -> Beans.copyAs(src, Arrays.asList("name"), PAbs.class));
        assertAbstract(() -> Beans.copyAs(src, (n, v) -> true, PAbs.class));
        assertAbstract(() -> Beans.copyAs(src, true, (Set<String>) null, PAbs.class));
        assertAbstract(() -> Beans.deepCopyAs(null, PAbs.class));

        // the concrete subclass still copies
        assertEquals("x", Beans.copyAs(src, PConcrete.class).getName());
        assertEquals("x", Beans.copyAs(src, true, (Set<String>) null, PConcrete.class).getName());
    }

    private static void assertAbstract(final org.junit.jupiter.api.function.Executable executable) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertTrue(e.getMessage().contains("abstract"), e.getMessage());
        assertTrue(e.getMessage().contains("PAbs"), e.getMessage());
    }

    // ---------------------------------------------------------------- C-421

    @Test
    public void testC421_beanToMapIgnoredNamesResolveSpellings() {
        final Person p = person();

        for (final String spelling : Arrays.asList("first_name", "FirstName", "FIRSTNAME", "firstName", " firstName ", "FIRST_NAME")) {
            final Map<String, Object> m = Beans.beanToMap(p, false, Collections.singleton(spelling));
            assertFalse(m.containsKey("firstName"), spelling + " -> " + m);
            assertTrue(m.containsKey("age"));
        }

        // an unknown name excludes nothing; a null element is tolerated
        final Set<String> withNull = new HashSet<>(Arrays.asList(null, "bogus"));
        assertEquals(Beans.beanToMap(p, false), Beans.beanToMap(p, false, withNull));

        // the key naming policy is applied after the exclusion
        final Map<String, Object> snake = Beans.beanToMap(p, false, Collections.singleton("FirstName"), NamingPolicy.SNAKE_CASE);
        assertFalse(snake.containsKey("first_name"), snake.toString());
        assertTrue(snake.containsKey("password"));

        // a Unicode property name
        final UnicodeBean u = new UnicodeBean();
        u.setNaïve("ü");
        final Map<String, Object> um = Beans.beanToMap(u, false, Collections.singleton("NAÏVE"));
        assertFalse(um.containsKey("naïve"), um.toString());
        assertTrue(um.containsKey("count"));
    }

    @Test
    public void testC421_deepAndFlatIgnoredNamesResolveSpellings() {
        final Person p = person();

        final Map<String, Object> deep = Beans.deepBeanToMap(p, false, Collections.singleton("PASSWORD"));
        assertFalse(deep.containsKey("password"), deep.toString());
        assertTrue(deep.containsKey("address"));

        final Map<String, Object> deep2 = Beans.deepBeanToMap(p, false, Collections.singleton("ADDRESS"), n -> new TreeMap<>());
        assertFalse(deep2.containsKey("address"), deep2.toString());

        final Map<String, Object> flat = Beans.beanToFlatMap(p, false, Collections.singleton("Password"));
        assertFalse(flat.containsKey("password"), flat.toString());
        assertEquals("London", flat.get("address.city"));

        // top-level only: a nested property of the same name is still emitted
        final Map<String, Object> flat2 = Beans.beanToFlatMap(p, false, Collections.singleton("City"));
        assertEquals("London", flat2.get("address.city"));
    }

    @Test
    public void testC421_copyAndMergeIgnoredNamesResolveSpellings() {
        final Person p = person();

        final Person copy = Beans.copyAs(p, true, Collections.singleton("Password"), Person.class);
        assertNull(copy.getPassword());
        assertEquals("Ada", copy.getFirstName());

        final Person target = new Person();
        Beans.mergeInto(p, target, true, Collections.singleton("pass_word"));
        assertNull(target.getPassword());
        assertEquals("Ada", target.getFirstName());

        final Person target2 = new Person();
        Beans.mergeInto(p, target2, true, Collections.singleton("PASSWORD"), (a, b) -> a);
        assertNull(target2.getPassword());
        assertEquals(36, target2.getAge());

        // builder exclusion keeps resolving spellings (the hoisted helper)
        final Map<String, Object> built = Beans.mapBuilder(p).exclude("first_name", "PASSWORD").toMap();
        assertFalse(built.containsKey("firstName"));
        assertFalse(built.containsKey("password"));
        assertTrue(built.containsKey("age"));
    }

    // ---------------------------------------------------------------- C-422

    @Test
    public void testC422_builderSelectCopiesTheCollection() {
        final Person p = person();
        final List<String> sel = new ArrayList<>(Arrays.asList("firstName"));

        final Beans.BeanMapBuilder builder = Beans.mapBuilder(p).select(sel);
        sel.add("age");
        sel.set(0, "password");

        final Map<String, Object> m = builder.toMap();
        assertEquals(Collections.singletonList("firstName"), new ArrayList<>(m.keySet()));

        // null clears the selection
        assertEquals(4, Beans.mapBuilder(p).select((List<String>) null).toMap().size());
    }

    // ================================================================ V2 items (verifier-confirmed)

    // ---------------------------------------------------------------- C-401 fixtures

    /** children declared BEFORE hasChildren: the order in which first-match used to go wrong. */
    public static class KidsNode {
        private List<String> children;
        private boolean hasChildren;

        public List<String> getChildren() {
            return children;
        }

        public void setChildren(final List<String> children) {
            this.children = children;
        }

        public boolean getHasChildren() {
            return hasChildren;
        }

        public void setHasChildren(final boolean hasChildren) {
            this.hasChildren = hasChildren;
        }
    }

    /** hasChildren declared first. */
    public static class KidsNodeRev {
        private boolean hasChildren;
        private List<String> children;

        public boolean getHasChildren() {
            return hasChildren;
        }

        public void setHasChildren(final boolean hasChildren) {
            this.hasChildren = hasChildren;
        }

        public List<String> getChildren() {
            return children;
        }

        public void setChildren(final List<String> children) {
            this.children = children;
        }
    }

    /** Same shape as KidsNodeRev, used only by the concurrency smoke test. */
    public static class KidsConc {
        private List<String> children;
        private boolean hasChildren;

        public List<String> getChildren() {
            return children;
        }

        public void setChildren(final List<String> children) {
            this.children = children;
        }

        public boolean getHasChildren() {
            return hasChildren;
        }

        public void setHasChildren(final boolean hasChildren) {
            this.hasChildren = hasChildren;
        }
    }

    /** Public fields only: BeanInfo.getPropInfo reaches its own alias scan. */
    public static class KidsFieldNode {
        public List<String> children;
        public boolean hasChildren;
    }

    /** Getter-backed children next to a field-only hasChildren. */
    public static class KidsMixedNode {
        private List<String> children;
        public boolean hasChildren;

        public List<String> getChildren() {
            return children;
        }

        public void setChildren(final List<String> children) {
            this.children = children;
        }
    }

    public static class FlagBean {
        private String active;
        private boolean isActive;

        public String getActive() {
            return active;
        }

        public void setActive(final String active) {
            this.active = active;
        }

        public boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(final boolean isActive) {
            this.isActive = isActive;
        }
    }

    private static final List<String> HAS_CHILDREN_SPELLINGS = Arrays.asList("haschildren", "HASCHILDREN", "hasCHILDREN", "has_children", "HAS_CHILDREN",
            "hasChildren", "  haschildren  ", "gethaschildren");

    private static final List<String> CHILDREN_SPELLINGS = Arrays.asList("children", "CHILDREN", "getchildren", "getChildren", " children ");

    // ---------------------------------------------------------------- C-401

    @Test
    public void testC401_accessorLookupsPickTheBestMatchInBothDeclarationOrders() {
        for (final Class<?> cls : Arrays.<Class<?>> asList(KidsNode.class, KidsNodeRev.class)) {
            for (final String s : HAS_CHILDREN_SPELLINGS) {
                assertEquals("getHasChildren", Beans.getPropGetter(cls, s).getName(), cls.getSimpleName() + ": " + s);
                assertEquals("setHasChildren", Beans.getPropSetter(cls, s).getName(), cls.getSimpleName() + ": " + s);
                assertEquals("hasChildren", Beans.getPropField(cls, s).getName(), cls.getSimpleName() + ": " + s);
                assertEquals("hasChildren", Beans.getBeanInfo(cls).getPropInfo(s).name, cls.getSimpleName() + ": " + s);
            }

            for (final String s : CHILDREN_SPELLINGS) {
                assertEquals("getChildren", Beans.getPropGetter(cls, s).getName(), cls.getSimpleName() + ": " + s);
                assertEquals("setChildren", Beans.getPropSetter(cls, s).getName(), cls.getSimpleName() + ": " + s);
                assertEquals("children", Beans.getPropField(cls, s).getName(), cls.getSimpleName() + ": " + s);
                assertEquals("children", Beans.getBeanInfo(cls).getPropInfo(s).name, cls.getSimpleName() + ": " + s);
            }
        }

        assertEquals("getIsActive", Beans.getPropGetter(FlagBean.class, "isactive").getName());
        assertEquals("isActive", Beans.getBeanInfo(FlagBean.class).getPropInfo("ISACTIVE").name);
        assertEquals("getActive", Beans.getPropGetter(FlagBean.class, "getactive").getName());
    }

    @Test
    public void testC401_fieldOnlyAndMixedBeansResolveTheBestMatch() {
        for (final String s : HAS_CHILDREN_SPELLINGS) {
            assertEquals("hasChildren", Beans.getPropField(KidsFieldNode.class, s).getName(), s);
            assertEquals("hasChildren", Beans.getBeanInfo(KidsFieldNode.class).getPropInfo(s).name, s);
            assertEquals("hasChildren", Beans.getBeanInfo(KidsMixedNode.class).getPropInfo(s).name, s);
        }

        for (final String s : CHILDREN_SPELLINGS) {
            assertEquals("children", Beans.getBeanInfo(KidsFieldNode.class).getPropInfo(s).name, s);
            assertEquals("children", Beans.getBeanInfo(KidsMixedNode.class).getPropInfo(s).name, s);
        }

        final Map<String, Object> m = new HashMap<>();
        m.put("haschildren", true);
        final KidsMixedNode mixed = Beans.mapToBean(m, KidsMixedNode.class);
        assertTrue(mixed.hasChildren);
        assertNull(mixed.getChildren());
    }

    @Test
    public void testC401_valueAndMappingPathsWriteTheRightProperty() {
        for (final Class<? extends Object> cls : Arrays.<Class<? extends Object>> asList(KidsNode.class, KidsNodeRev.class)) {
            final Map<String, Object> m = new HashMap<>();
            m.put("haschildren", true);
            final Object bean = Beans.mapToBean(m, cls);
            assertEquals(Boolean.TRUE, Beans.getPropValue(bean, "hasChildren"), cls.getSimpleName());
            assertNull(Beans.getPropValue(bean, "children"), cls.getSimpleName());

            final Object strict = Beans.mapToBean(m, false, cls);
            assertEquals(Boolean.TRUE, Beans.getPropValue(strict, "hasChildren"));

            final Object fromJson = N.fromJson("{\"haschildren\":true,\"children\":[\"x\"]}", cls);
            assertEquals(Boolean.TRUE, Beans.getPropValue(fromJson, "hasChildren"));
            assertEquals(Arrays.asList("x"), Beans.getPropValue(fromJson, "children"));

            assertEquals(Boolean.TRUE, Beans.getPropValue(bean, "haschildren"));
            assertEquals(Boolean.TRUE, Beans.getPropValue(bean, "HAS_CHILDREN", false));
            Beans.setPropValue(bean, "haschildren", false);
            assertEquals(Boolean.FALSE, Beans.getPropValue(bean, "hasChildren"));
            assertNull(Beans.getPropValue(bean, "children"));
        }
    }

    @Test
    public void testC401_exhaustiveCaseVariantsOfHasChildren() {
        final String base = "haschildren";

        for (int mask = 0; mask < (1 << base.length()); mask++) {
            final StringBuilder sb = new StringBuilder(base.length());

            for (int i = 0; i < base.length(); i++) {
                final char c = base.charAt(i);
                sb.append((mask & (1 << i)) != 0 ? Character.toUpperCase(c) : c);
            }

            final String v = sb.toString();
            assertEquals("getHasChildren", Beans.getPropGetter(KidsNode.class, v).getName(), v);
            assertEquals("setHasChildren", Beans.getPropSetter(KidsNode.class, v).getName(), v);
            assertEquals("hasChildren", Beans.getBeanInfo(KidsNode.class).getPropInfo(v).name, v);
            assertEquals("hasChildren", Beans.getPropField(KidsFieldNode.class, v).getName(), v);
            assertEquals("hasChildren", Beans.getBeanInfo(KidsFieldNode.class).getPropInfo(v).name, v);
        }
    }

    @Test
    public void testC401_lengthCapAppliesOnceAfterTrimming() {
        final String name128 = "has" + Strings.repeat('_', 117) + "children";
        final String name129 = "has" + Strings.repeat('_', 118) + "children";
        assertEquals(128, name128.length());
        assertEquals(129, name129.length());

        assertEquals("getHasChildren", Beans.getPropGetter(KidsNode.class, name128).getName());
        assertEquals("hasChildren", Beans.getBeanInfo(KidsNode.class).getPropInfo("  " + name128 + "  ").name);
        // over the cap the alias scan does not match; the underscore spelling still reaches the property through
        // the separate normalizePropName fallback (unchanged behaviour)
        assertEquals("getHasChildren", Beans.getPropGetter(KidsNode.class, name129).getName());

        final String long129 = Strings.repeat('x', 129);
        assertNull(Beans.getPropGetter(KidsNode.class, long129));
        assertNull(Beans.getPropField(KidsNode.class, long129));
        assertNull(Beans.getBeanInfo(KidsNode.class).getPropInfo(long129));
        assertNull(Beans.getBeanInfo(KidsFieldNode.class).getPropInfo(long129));
        assertNull(Beans.getPropValue(new KidsNode(), long129, true));
    }

    @Test
    public void testC401_beansAndBeanInfoAgreeOnASpellingTable() {
        final List<String> table = new ArrayList<>(HAS_CHILDREN_SPELLINGS);
        table.addAll(CHILDREN_SPELLINGS);
        table.addAll(Arrays.asList("KidsFieldNode.children", "kidsfieldnode.HASCHILDREN", "sethaschildren", "ischildren", "has__children__", "bogus", "has"));

        for (final String s : table) {
            final java.lang.reflect.Field f = Beans.getPropField(KidsFieldNode.class, s);
            final com.landawn.abacus.parser.ParserUtil.PropInfo p = Beans.getBeanInfo(KidsFieldNode.class).getPropInfo(s);
            assertEquals(f == null ? null : f.getName(), p == null ? null : p.name, s);
        }

        final List<String> table2 = new ArrayList<>(table);
        table2.replaceAll(s -> s.replace("KidsFieldNode", "KidsNode").replace("kidsfieldnode", "kidsnode"));

        for (final String s : table2) {
            final Method g = Beans.getPropGetter(KidsNode.class, s);
            final com.landawn.abacus.parser.ParserUtil.PropInfo p = Beans.getBeanInfo(KidsNode.class).getPropInfo(s);
            assertEquals(g == null ? null : Beans.getPropNameByMethod(g), p == null ? null : p.name, s);
        }
    }

    // ---------------------------------------------------------------- C-402 fixtures

    /** A bean with a property named like its own class. */
    public static class Chain {
        private String name;
        private Chain chain;

        public Chain() {
        }

        public Chain(final String name, final Chain chain) {
            this.name = name;
            this.chain = chain;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Chain getChain() {
            return chain;
        }

        public void setChain(final Chain chain) {
            this.chain = chain;
        }
    }

    public static class Customer {
        private String name;
        private String email;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }
    }

    /** A bean-typed property named like the class, of another type. */
    public static class Purchase {
        private String name;
        private String zip;
        private Customer purchase;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(final String zip) {
            this.zip = zip;
        }

        public Customer getPurchase() {
            return purchase;
        }

        public void setPurchase(final Customer purchase) {
            this.purchase = purchase;
        }
    }

    /** A String-typed property named like the class: the qualified alias must keep working. */
    public static class Wide {
        private String wide;
        private String name;

        public String getWide() {
            return wide;
        }

        public void setWide(final String wide) {
            this.wide = wide;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class Account {
        private long id;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // ---------------------------------------------------------------- C-402

    @Test
    public void testC402_selfNamedBeanPropertyMakesTheDottedNameANestedPath() {
        final Chain child = new Chain("child", new Chain("parent", null));

        for (final String path : Arrays.asList("chain.name", "Chain.name", "CHAIN.NAME")) {
            assertEquals("parent", Beans.getPropValue(child, path), path);
            assertEquals("parent", Beans.<String> getPropValueIfPresent(child, path).orElseThrow(), path);
            assertNull(Beans.getPropGetter(Chain.class, path), path);
            assertNull(Beans.getBeanInfo(Chain.class).getPropInfo(path), path);
        }

        // an unreachable nested path
        assertNull(Beans.getPropValue(new Chain("x", null), "chain.name"));
        assertFalse(Beans.getPropValueIfPresent(new Chain("x", null), "chain.name").isPresent());

        // mapToBean writes the nested level, tolerant and strict
        final Map<String, Object> m = new HashMap<>();
        m.put("chain.name", "P");
        final Chain c = Beans.mapToBean(m, Chain.class);
        assertNull(c.getName());
        assertEquals("P", c.getChain().getName());
        final Chain c2 = Beans.mapToBean(m, false, Chain.class);
        assertNull(c2.getName());
        assertEquals("P", c2.getChain().getName());
    }

    @Test
    public void testC402_flatMapRoundTripIsExact() {
        final Chain child = new Chain("child", new Chain("parent", null));
        final Map<String, Object> flat = Beans.beanToFlatMap(child);
        assertEquals("child", flat.get("name"));
        assertEquals("parent", flat.get("chain.name"));

        final Chain back = Beans.mapToBean(flat, Chain.class);
        assertEquals("child", back.getName());
        assertEquals("parent", back.getChain().getName());
        assertEquals(flat, Beans.beanToFlatMap(back));
    }

    @Test
    public void testC402_otherTypedSelfNamedPropertyAndTailFallback() {
        final Purchase p = new Purchase();
        p.setName("outer");
        p.setZip("Z1");
        final Customer cu = new Customer();
        cu.setName("inner");
        cu.setEmail("a@b.c");
        p.setPurchase(cu);

        assertEquals("inner", Beans.getPropValue(p, "purchase.name"));
        assertEquals("a@b.c", Beans.getPropValue(p, "purchase.email"));
        // the tail does not resolve on Customer: the class-qualified alias still names the outer zip
        assertEquals("Z1", Beans.getPropValue(p, "purchase.zip"));
        assertEquals("getZip", Beans.getPropGetter(Purchase.class, "Purchase.zip").getName());
    }

    @Test
    public void testC402_nonBeanQualifierAndPlainQualifiedAliasStillResolve() {
        final Wide w = new Wide();
        w.setWide("W");
        w.setName("N");
        assertEquals("N", Beans.getPropValue(w, "Wide.name"));
        assertEquals("N", Beans.getPropValue(w, "wide.name"));
        assertEquals("getName", Beans.getPropGetter(Wide.class, "wide.name").getName());
        assertEquals("name", Beans.getBeanInfo(Wide.class).getPropInfo("WIDE.NAME").name);

        final Account a = new Account();
        a.setId(7);
        a.setName("acc");
        assertEquals(7L, (Long) Beans.getPropValue(a, "Account.id"));
        assertEquals("acc", Beans.getPropValue(a, "account.NAME"));
        final Map<String, Object> m = new HashMap<>();
        m.put("Account.id", 9);
        assertEquals(9L, Beans.mapToBean(m, Account.class).getId());
    }

    // ---------------------------------------------------------------- C-403

    @Test
    public void testC403_nonPublicBuilderShapesAreUsable() throws Exception {
        for (final Class<?> cls : Arrays.<Class<?>> asList(com.landawn.abacus.testfixture.beansreview.BuilderFixtures.pkgPersonClass(),
                com.landawn.abacus.testfixture.beansreview.BuilderFixtures.pubPersonClass(),
                com.landawn.abacus.testfixture.beansreview.BuilderFixtures.lombokPersonClass())) {
            final String label = cls.getName();
            assertEquals(new HashSet<>(Arrays.asList("name", "age")), new HashSet<>(Beans.getPropNameList(cls)), label);

            final Beans.BuilderInfo info = Beans.getBuilderInfo(cls);
            assertNotNull(info, label);

            final Object builder = info.newBuilder();
            assertNotNull(builder, label);
            Beans.setPropValue(builder, Beans.getPropSetter(info.builderClass(), "name"), "Ada");
            Beans.setPropValue(builder, Beans.getPropSetter(info.builderClass(), "age"), "36");
            final Object built = info.build(builder);
            assertEquals("Ada", Beans.getPropValue(built, "name"), label);
            assertEquals(36, (Integer) Beans.getPropValue(built, "age"), label);

            final Map<String, Object> m = new HashMap<>();
            m.put("name", "Bob");
            m.put("age", 7);
            final Object mapped = Beans.mapToBean(m, cls);
            assertEquals("Bob", Beans.getPropValue(mapped, "name"), label);
            assertEquals(7, (Integer) Beans.getPropValue(mapped, "age"), label);

            final Object parsed = N.fromJson("{\"name\":\"Cy\",\"age\":3}", cls);
            assertEquals("Cy", Beans.getPropValue(parsed, "name"), label);

            final Object copied = Beans.copy(mapped);
            assertEquals("Bob", Beans.getPropValue(copied, "name"), label);

            final Object copiedAs = Beans.copyAs(mapped, cls);
            assertEquals(7, (Integer) Beans.getPropValue(copiedAs, "age"), label);
        }
    }

    // ---------------------------------------------------------------- C-404 fixtures

    public static class XBase {
        private long id;
        private final List<String> items = new ArrayList<>();

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public List<String> getItems() {
            return items;
        }
    }

    public static class XSub extends XBase {
        private String extra;

        public String getExtra() {
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }
    }

    public static class XSub2 extends XSub {
    }

    /** Introspected BEFORE the base is registered. */
    public static class XSubEarly extends XBase {
    }

    /** Declares its own getter-only collection. */
    public static class XSubOwn extends XBase {
        private final List<String> tags = new ArrayList<>();

        public List<String> getTags() {
            return tags;
        }
    }

    /** Explicitly registered, but cannot be constructed: its own (demoted) entry wins over the base's. */
    public static class XSubBad extends XBase {
        public XSubBad() {
            throw new IllegalStateException("no");
        }
    }

    public interface XIface {
        List<String> getNotes();
    }

    public static class XImpl implements XIface {
        private long id;
        private final List<String> notes = new ArrayList<>();

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        @Override
        public List<String> getNotes() {
            return notes;
        }
    }

    // ---------------------------------------------------------------- C-404

    @Test
    public void testC404_xmlBindingRegistrationGovernsSubclasses() {
        // introspected before the registration: the applyRegistration sweep must pick it up
        assertFalse(Beans.getPropNameList(XSubEarly.class).contains("items"));
        assertFalse(Beans.isRegisteredXmlBindingClass(XSubEarly.class));

        Beans.registerXmlBindingClass(XBase.class);
        Beans.registerXmlBindingClass(XSubBad.class);
        Beans.registerXmlBindingClass(XIface.class);

        assertTrue(Beans.getPropNameList(XBase.class).contains("items"));

        for (final Class<?> cls : Arrays.<Class<?>> asList(XSub.class, XSub2.class, XSubEarly.class, XSubOwn.class)) {
            assertTrue(Beans.isRegisteredXmlBindingClass(cls), cls.getSimpleName());
            assertTrue(Beans.getPropNameList(cls).contains("items"), cls.getSimpleName() + " " + Beans.getPropNameList(cls));
        }

        final Map<String, Object> m = new HashMap<>();
        m.put("id", 1);
        m.put("items", Arrays.asList("a", "b"));
        m.put("extra", "e");

        assertEquals(Arrays.asList("a", "b"), Beans.mapToBean(m, XSub.class).getItems());
        assertEquals(Arrays.asList("a", "b"), Beans.mapToBean(m, false, XSub2.class).getItems());
        assertEquals(Arrays.asList("a", "b"), N.fromJson(N.toJson(Beans.mapToBean(m, XSub.class)), XSub.class).getItems());

        // a getter-only collection declared by the subclass itself is discovered AND writable
        final Map<String, Object> own = new HashMap<>();
        own.put("tags", Arrays.asList("t"));
        own.put("items", Arrays.asList("i"));
        final XSubOwn o = Beans.mapToBean(own, false, XSubOwn.class);
        assertEquals(Arrays.asList("t"), o.getTags());
        assertEquals(Arrays.asList("i"), o.getItems());

        // the nearest explicit entry decides: XSubBad's registration is demoted when it cannot be constructed
        assertFalse(Beans.getPropNameList(XSubBad.class).contains("items"));
        assertFalse(Beans.isRegisteredXmlBindingClass(XSubBad.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(XBase.class));

        // interfaces are not consulted
        assertFalse(Beans.isRegisteredXmlBindingClass(XImpl.class));
        assertFalse(Beans.getPropNameList(XImpl.class).contains("notes"));

        assertFalse(Beans.isRegisteredXmlBindingClass(null));
    }

    // ---------------------------------------------------------------- C-405 fixtures

    @com.landawn.abacus.annotation.Entity
    public static class EBase {
        private long id;
        private String code = "C";

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public String getComputed() {
            return "x" + id;
        }
    }

    public static class ESub extends EBase {
        private String extra;

        public String getExtra() {
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }

        public String getSubComputed() {
            return "sub";
        }
    }

    public static class PBase {
        private long id;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getDerived() {
            return "d";
        }
    }

    @com.landawn.abacus.annotation.Entity
    public static class ESub2 extends PBase {
        private String extra;

        public String getExtra() {
            return extra;
        }

        public void setExtra(final String extra) {
            this.extra = extra;
        }

        public String getOwn() {
            return "o";
        }
    }

    // ---------------------------------------------------------------- C-405

    @Test
    public void testC405_entityBaseGetterOnlyPropertiesSurviveInAPlainSubclass() {
        final List<String> baseProps = Beans.getPropNameList(EBase.class);
        assertTrue(baseProps.containsAll(Arrays.asList("id", "code", "computed")), baseProps.toString());

        final List<String> subProps = Beans.getPropNameList(ESub.class);
        assertTrue(subProps.containsAll(Arrays.asList("id", "code", "computed", "extra")), subProps.toString());
        assertFalse(subProps.contains("subComputed"), subProps.toString());

        final ESub s = new ESub();
        s.setId(5);
        s.setExtra("e");
        final Map<String, Object> m = Beans.beanToMap(s);
        assertEquals("C", m.get("code"));
        assertEquals("x5", m.get("computed"));
        assertTrue(N.toJson(s).contains("\"computed\""));

        // with C-417 the new read-only property does not break the write side
        Beans.clearAllProps(s);
        assertEquals(0, s.getId());
        assertNull(s.getExtra());
        final ESub back = Beans.mapToBean(Beans.beanToMap(s), ESub.class);
        assertNotNull(back);
        assertNotNull(Beans.newRandomBean(ESub.class));

        // plain base + @Entity subclass: unchanged (the whole hierarchy is an entity)
        final List<String> sub2Props = Beans.getPropNameList(ESub2.class);
        assertTrue(sub2Props.containsAll(Arrays.asList("id", "derived", "extra", "own")), sub2Props.toString());
        assertFalse(Beans.getPropNameList(PBase.class).contains("derived"));
    }

    // ---------------------------------------------------------------- C-409

    public static class RegTarget {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Test
    public void testC409_concurrentTolerantLookupsMatchSingleThreadedAnswers() throws Exception {
        final List<String> known = new ArrayList<>(HAS_CHILDREN_SPELLINGS);
        known.addAll(CHILDREN_SPELLINGS);

        final Map<String, String> expectedGetter = new HashMap<>();
        final Map<String, String> expectedSetter = new HashMap<>();
        final Map<String, String> expectedField = new HashMap<>();

        for (final String s : known) {
            expectedGetter.put(s, Beans.getPropGetter(KidsNodeRev.class, s).getName());
            expectedSetter.put(s, Beans.getPropSetter(KidsNodeRev.class, s).getName());
            expectedField.put(s, Beans.getPropField(KidsNodeRev.class, s).getName());
        }

        final Method getValue = RegTarget.class.getMethod("getValue");
        final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(9);
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        final List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

        try {
            for (int t = 0; t < 8; t++) {
                final int tid = t;
                futures.add(pool.submit(() -> {
                    try {
                        start.await();

                        for (int i = 0; i < 1500; i++) {
                            // U14-01 (2026-09-25): a small ROTATING set of unknown spellings (16 in all: 8 threads x 2),
                            // not 12,000 distinct ones. Every tolerant miss is memoized under the caller's spelling in
                            // Beans' per-class pools, and used to be interned into NameUtil's bounded process-global
                            // pool as well (U14-02), so the distinct names filled that pool (POOL_SIZE = 4000 under
                            // -Xmx1200m) and NameUtilTest failed later in the same JVM. The contended miss path is
                            // still exercised - concurrently, by the same and by different spellings, each with the
                            // underscore that sends it through the normalized retry - which is what this smoke test
                            // is about.
                            final String unknown = "zz" + tid + "_" + (i % 2);
                            assertNull(Beans.getPropGetter(KidsConc.class, unknown));
                            assertNull(Beans.getPropSetter(KidsConc.class, unknown));
                            assertNull(Beans.getPropField(KidsConc.class, unknown));

                            final String s = known.get(i % known.size());
                            assertEquals(expectedGetter.get(s), Beans.getPropGetter(KidsConc.class, s).getName(), s);
                            assertEquals(expectedSetter.get(s), Beans.getPropSetter(KidsConc.class, s).getName(), s);
                            assertEquals(expectedField.get(s), Beans.getPropField(KidsConc.class, s).getName(), s);
                        }
                    } catch (final Throwable e) {
                        failure.compareAndSet(null, e);
                    }
                }));
            }

            futures.add(pool.submit(() -> {
                try {
                    start.await();

                    for (int i = 0; i < 200; i++) {
                        Beans.registerPropertyAccessor("alias", getValue);
                        Beans.getPropGetter(RegTarget.class, "ALIAS");
                    }
                } catch (final Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }));

            start.countDown();

            for (final java.util.concurrent.Future<?> f : futures) {
                f.get(120, java.util.concurrent.TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }

        for (final String s : known) {
            assertEquals(expectedGetter.get(s), Beans.getPropGetter(KidsConc.class, s).getName(), s);
        }

        assertEquals("getValue", Beans.getPropGetter(RegTarget.class, "alias").getName());
    }

    // ---------------------------------------------------------------- C-416 fixtures

    public static class CopyAddr {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    /** Kryo cannot instantiate it (no no-arg constructor). */
    public static class NoNoArg {
        private final String v;

        public NoNoArg(final String v) {
            this.v = v;
        }

        public String getV() {
            return v;
        }
    }

    public static class CopyHolder {
        private Object payload;
        private List<CopyAddr> addrs;

        public Object getPayload() {
            return payload;
        }

        public void setPayload(final Object payload) {
            this.payload = payload;
        }

        public List<CopyAddr> getAddrs() {
            return addrs;
        }

        public void setAddrs(final List<CopyAddr> addrs) {
            this.addrs = addrs;
        }
    }

    public static class CycleNode {
        private Object payload;
        private CycleNode next;

        public Object getPayload() {
            return payload;
        }

        public void setPayload(final Object payload) {
            this.payload = payload;
        }

        public CycleNode getNext() {
            return next;
        }

        public void setNext(final CycleNode next) {
            this.next = next;
        }
    }

    private static CopyHolder sharedHolder() {
        final CopyAddr a = new CopyAddr();
        a.setCity("X");
        final CopyHolder h = new CopyHolder();
        h.setAddrs(new ArrayList<>(Arrays.asList(a, a)));
        return h;
    }

    // ---------------------------------------------------------------- C-416

    @Test
    public void testC416_oneFailingDeepCopyDoesNotDowngradeTheClass() {
        final CopyHolder good = sharedHolder();
        final CopyHolder c1 = Beans.deepCopy(good);
        assertTrue(c1.getAddrs().get(0) == c1.getAddrs().get(1), "Kryo keeps shared identity");

        final CopyHolder bad = sharedHolder();
        bad.setPayload(new NoNoArg("y"));
        // the failing instance itself still copies (through the XML round trip)
        final CopyHolder badCopy = Beans.deepCopy(bad);
        assertNotNull(badCopy);
        assertEquals("X", badCopy.getAddrs().get(0).getCity());

        final CopyHolder c2 = Beans.deepCopy(good);
        assertTrue(c2.getAddrs().get(0) == c2.getAddrs().get(1), "a later good copy must still keep shared identity");

        // and the bad one copies again, every time
        assertNotNull(Beans.deepCopy(bad));
    }

    @Test
    public void testC416_cycleStillCopiesAfterAFailingCopy() {
        final CycleNode cyc = new CycleNode();
        cyc.setNext(cyc);
        final CycleNode c1 = Beans.deepCopy(cyc);
        assertTrue(c1.getNext() == c1);

        final CycleNode bad = new CycleNode();
        bad.setPayload(new NoNoArg("y"));

        try {
            Beans.deepCopy(bad);
        } catch (final RuntimeException e) {
            // whether the XML fallback manages this payload is not the point here
        }

        final CycleNode c2 = Beans.deepCopy(cyc);
        assertTrue(c2.getNext() == c2, "a cyclic copy that worked must keep working");
    }

    @Test
    public void testC416_arrayListIsNotDowngradedByOneBadElement() {
        try {
            Beans.deepCopy(new ArrayList<Object>(Arrays.asList(new NoNoArg("y"))));
        } catch (final RuntimeException e) {
            // see above
        }

        final CopyAddr a = new CopyAddr();
        a.setCity("Y");
        final ArrayList<CopyAddr> list = new ArrayList<>(Arrays.asList(a, a));
        final ArrayList<CopyAddr> copy = Beans.deepCopy(list);
        assertTrue(copy.get(0) == copy.get(1), "ArrayList copies keep shared identity");
        assertEquals("Y", copy.get(0).getCity());
    }

    // ---------------------------------------------------------------- C-417 fixtures

    @com.landawn.abacus.annotation.Entity
    public static class Ent {
        private String first;
        private String last;

        public Ent() {
        }

        public Ent(final String first, final String last) {
            this.first = first;
            this.last = last;
        }

        public String getFirst() {
            return first;
        }

        public void setFirst(final String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(final String last) {
            this.last = last;
        }

        public String getFullName() {
            return first + " " + last;
        }
    }

    /** A plain bean whose fullName IS writable. */
    public static class PlainFull {
        private String first;
        private String last;
        private String fullName;

        public String getFirst() {
            return first;
        }

        public void setFirst(final String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(final String last) {
            this.last = last;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(final String fullName) {
            this.fullName = fullName;
        }
    }

    public static class EntHolder {
        private Ent ent;
        private String x;

        public Ent getEnt() {
            return ent;
        }

        public void setEnt(final Ent ent) {
            this.ent = ent;
        }

        public String getX() {
            return x;
        }

        public void setX(final String x) {
            this.x = x;
        }
    }

    public static class SerOnly {
        @com.landawn.abacus.annotation.JsonXmlField(direction = com.landawn.abacus.annotation.JsonXmlField.Direction.SERIALIZE_ONLY)
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(final String token) {
            this.token = token;
        }
    }

    public record RecE(String a, int b) {
    }

    private static void assertEnt(final Ent e, final String first, final String last) {
        assertEquals(first, e.getFirst());
        assertEquals(last, e.getLast());
    }

    // ---------------------------------------------------------------- C-417

    @Test
    public void testC417_allPropertyWritersSkipReadOnlyProperties() {
        assertTrue(Beans.getPropNameList(Ent.class).contains("fullName"));

        final Ent e = new Ent("A", "B");
        Beans.clearAllProps(e);
        assertEnt(e, null, null);

        Beans.randomize(e);
        assertNotNull(e.getFirst());
        assertNotNull(e.getLast());

        final Ent r = Beans.newRandomBean(Ent.class);
        assertNotNull(r.getFirst());
        assertEquals(2, Beans.newRandomBeanList(Ent.class, 2).size());

        final EntHolder h = Beans.newRandomBean(EntHolder.class);
        assertNotNull(h.getEnt());
        assertNotNull(h.getEnt().getFirst());
    }

    @Test
    public void testC417_explicitReadOnlySelectionIsRejectedBeforeAnyWrite() {
        final Ent e = new Ent("A", "B");

        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(e, "first", "fullName"));
        assertEnt(e, "A", "B");
        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(e, Arrays.asList("last", "FULL_NAME")));
        assertEnt(e, "A", "B");

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Beans.randomize(e, Arrays.asList("first", "fullName")));
        assertTrue(ex.getMessage().contains("read-only"), ex.getMessage());
        assertEnt(e, "A", "B");

        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean(Ent.class, Arrays.asList("fullName")));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(Ent.class, Arrays.asList("fullName"), 1));

        final Ent target = new Ent("T", "U");
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(e, target, Arrays.asList("first", "fullName")));
        assertEnt(target, "T", "U");
        assertThrows(IllegalArgumentException.class, () -> Beans.copy(e, Arrays.asList("first", "fullName")));

        final Map<String, Object> m = Beans.beanToMap(e);
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(m, Arrays.asList("first", "fullName"), Ent.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList("fullName"), Ent.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(Arrays.asList(m), Arrays.asList("fullName"), Ent.class));
    }

    @Test
    public void testC417_mappingAndMergingTreatReadOnlyAsUnmatched() {
        final Ent e = new Ent("A", "B");

        // the round trip the immutable-bean message recommends
        final Map<String, Object> m = Beans.beanToMap(e);
        assertEquals("A B", m.get("fullName"));
        assertEnt(Beans.mapToBean(m, Ent.class), "A", "B");
        assertEnt(Beans.mapToBean(m, true, Ent.class), "A", "B");
        assertEquals(1, Beans.mapsToBeans(Arrays.asList(m), Ent.class).size());
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(m, false, Ent.class));

        final Ent t1 = new Ent("T", "U");
        Beans.mergeInto(e, t1);
        assertEnt(t1, "A", "B");

        final Ent t2 = new Ent("T", "U");
        Beans.mergeInto(e, t2, true, (Set<String>) null);
        assertEnt(t2, "A", "B");

        final Ent t3 = new Ent("T", "U");
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(e, t3, false, (Set<String>) null));
        assertEnt(t3, "T", "U");

        final PlainFull pf = new PlainFull();
        pf.setFirst("P");
        pf.setLast("Q");
        pf.setFullName("ignored");
        final Ent t4 = new Ent();
        Beans.mergeInto(pf, t4);
        assertEnt(t4, "P", "Q");
        assertEnt(Beans.copyAs(pf, Ent.class), "P", "Q");

        // the propFilter family skips a read-only target
        assertEnt(Beans.copyAs(e, (n, v) -> true, Ent.class), "A", "B");
        assertEnt(Beans.copy(e, (n, v) -> v != null), "A", "B");
        final Ent t5 = new Ent();
        Beans.mergeIntoIf(e, t5, (n, v) -> true);
        assertEnt(t5, "A", "B");

        assertEnt(Beans.copyAs(e, true, (Set<String>) null, Ent.class), "A", "B");
    }

    @Test
    public void testC417_writablePropertiesThatLookSimilarAreStillWritten() {
        // a field-backed SERIALIZE_ONLY property is not read-only
        final Map<String, Object> m = new HashMap<>();
        m.put("token", "t");
        assertEquals("t", Beans.mapToBean(m, SerOnly.class).getToken());
        final SerOnly s = new SerOnly();
        s.setToken("x");
        Beans.clearAllProps(s);
        assertNull(s.getToken());

        // record components are never read-only
        final Map<String, Object> rm = new HashMap<>();
        rm.put("a", "x");
        rm.put("b", 2);
        assertEquals(new RecE("x", 2), Beans.mapToBean(rm, RecE.class));
        assertNotNull(Beans.newRandomBean(RecE.class));
    }
}
