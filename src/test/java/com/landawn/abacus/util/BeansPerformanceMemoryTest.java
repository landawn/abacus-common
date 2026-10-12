package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Beans. */
@Tag("slow-test")
public class BeansPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G003 end ----
    // ---- perf review 2026-10-08 G004 begin ----
    @Nested
    class G004 {
        /**
         * An entity, so the computed {@code getFullName()} is a read-only property.
         */
        @com.landawn.abacus.annotation.Entity
        public static class G004Person {
            private String firstName;
            private String lastName = "initial";
            private int age;
            private G004Address address;

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(final String firstName) {
                this.firstName = firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(final String lastName) {
                this.lastName = lastName;
            }

            public int getAge() {
                return age;
            }

            public void setAge(final int age) {
                this.age = age;
            }

            public G004Address getAddress() {
                return address;
            }

            public void setAddress(final G004Address address) {
                this.address = address;
            }

            public String getFullName() {
                return firstName + " " + lastName;
            }
        }

        /**
         * An entity with a computed, read-only {@code label}.
         */
        @com.landawn.abacus.annotation.Entity
        public static class G004Address {
            private String city;
            private String zip;

            public String getCity() {
                return city;
            }

            public void setCity(final String city) {
                this.city = city;
            }

            public String getZip() {
                return zip;
            }

            public void setZip(final String zip) {
                this.zip = zip;
            }

            public String getLabel() {
                return city + "-" + zip;
            }
        }

        /**
         * Records its construction and every setter call, so the tests can see where validation interleaves with writes.
         */
        // Accessor-based on purpose: this fixture observes getter/setter calls, and properties are read and written
        // through their fields unless @AccessFieldByMethod says otherwise (in both the reflective and the ASM mode).
        @com.landawn.abacus.annotation.AccessFieldByMethod
        public static class G004Recorder {
            static final List<String> LOG = new ArrayList<>();

            private String a;
            private String b;

            public G004Recorder() {
                LOG.add("new");
            }

            public String getA() {
                return a;
            }

            public void setA(final String a) {
                LOG.add("a=" + a);
                this.a = a;
            }

            public String getB() {
                return b;
            }

            public void setB(final String b) {
                LOG.add("b=" + b);
                this.b = b;
            }
        }

        private String notABeanMessage(final Class<?> cls) {
            return assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(cls)).getMessage();
        }

        private void assertNotABean(final Class<?> cls, final org.junit.jupiter.api.function.Executable executable) {
            assertEquals(notABeanMessage(cls), assertThrows(IllegalArgumentException.class, executable).getMessage(), cls.getName());
        }

        private String readOnlyMessage(final String propName, final Class<?> cls) {
            return "No setter method found with property name: " + propName + " in class: " + ClassUtil.getCanonicalClassName(cls)
                    + " (the property is read-only: no permitted field write, setter, or supported getter-based write)";
        }

        private String noSetterMessage(final String propName, final Class<?> cls) {
            return "No setter method found with property name: " + propName + " in class: " + cls.getCanonicalName();
        }

        private G004Person person() {
            final G004Person p = new G004Person();
            p.setFirstName("Ann");
            p.setAge(7);
            final G004Address a = new G004Address();
            a.setCity("NYC");
            p.setAddress(a);
            return p;
        }

        // G004-01: getPropValue/getPropValueIfPresent/getPropNames(bean, ..) validate the class once (inside ParserUtil.getBeanInfo)
        // and still throw the reason-bearing exception for every kind of non-bean, before any callback runs.
        @Test
        public void testGetPropValue_nonBeanClassesKeepTheReasonBearingException() {
            assertEquals("Not a bean class: java.lang.String - CharSequence implementations are never treated as beans", notABeanMessage(String.class));
            assertEquals("Not a bean class: java.lang.IllegalStateException - Throwable subclasses are never treated as beans",
                    notABeanMessage(IllegalStateException.class));

            final AtomicInteger callbacks = new AtomicInteger();

            for (final Object nonBean : Arrays.asList("abc", 5, new ArrayList<>(Arrays.asList(1)), new HashMap<>(), new Object(), new java.util.Date(0),
                    new IllegalStateException("x"))) {
                final Class<?> cls = nonBean.getClass();

                assertFalse(Beans.isBeanClass(cls), cls.getName());
                assertNotABean(cls, () -> Beans.getPropValue(nonBean, "length"));
                assertNotABean(cls, () -> Beans.getPropValue(nonBean, "length", true));
                assertNotABean(cls, () -> Beans.getPropValue(nonBean, "message", false));
                assertNotABean(cls, () -> Beans.getPropValueIfPresent(nonBean, "x"));
                assertNotABean(cls, () -> Beans.getPropNames(nonBean, true));
                assertNotABean(cls, () -> Beans.getPropNames(nonBean, false));
                assertNotABean(cls, () -> Beans.getPropNames(nonBean, (java.util.function.Predicate<String>) n -> callbacks.incrementAndGet() > 0));
                assertNotABean(cls,
                        () -> Beans.getPropNames(nonBean, (java.util.function.BiPredicate<String, Object>) (n, v) -> callbacks.incrementAndGet() > 0));
            }

            assertEquals(0, callbacks.get());

            // argument validation still comes first, with the same messages as for a bean
            final G004Person bean = person();
            final String nullFilter = assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames(bean, (java.util.function.Predicate<String>) null))
                    .getMessage();
            assertEquals(nullFilter,
                    assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames("abc", (java.util.function.Predicate<String>) null)).getMessage());
            final String nullBiFilter = assertThrows(IllegalArgumentException.class,
                    () -> Beans.getPropNames(bean, (java.util.function.BiPredicate<String, Object>) null)).getMessage();
            assertEquals(nullBiFilter,
                    assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames("abc", (java.util.function.BiPredicate<String, Object>) null))
                            .getMessage());
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(null, "firstName"));
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValueIfPresent(null, "firstName"));
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames((Object) null, true));
        }

        // G004-01: the bean paths of the same methods are unchanged (values, aliases, nested paths, misses, callback order).
        @Test
        public void testGetPropValue_beanPathsAndCallbacksUnchanged() {
            final G004Person p = person();

            assertEquals("Ann", Beans.<String> getPropValue(p, "firstName"));
            assertEquals("Ann", Beans.<String> getPropValue(p, "FIRST_NAME"));
            assertEquals("Ann", Beans.<String> getPropValue(p, "first_name", false));
            assertEquals(Integer.valueOf(7), Beans.<Integer> getPropValue(p, "age"));
            assertEquals("Ann initial", Beans.<String> getPropValue(p, "fullName"));
            assertEquals("NYC", Beans.<String> getPropValue(p, "address.city"));
            assertEquals("NYC-null", Beans.<String> getPropValue(p, "address.label"));
            assertNull(Beans.getPropValue(p, "address.zip"));
            assertNull(Beans.getPropValue(p, "nope", true));
            assertEquals("No property method found with property name: nope in class " + ClassUtil.getCanonicalClassName(G004Person.class),
                    assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(p, "nope")).getMessage());
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(p, "address.nope", false));
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(p, (String) null));

            assertEquals(u.Nullable.of("Ann"), Beans.getPropValueIfPresent(p, "firstName"));
            assertEquals(u.Nullable.of("NYC"), Beans.getPropValueIfPresent(p, "address.city"));
            assertEquals(u.Nullable.of((Object) null), Beans.getPropValueIfPresent(p, "address.zip"));
            assertFalse(Beans.getPropValueIfPresent(p, "nope").isPresent());

            final G004Person noAddress = new G004Person();
            assertNull(Beans.getPropValue(noAddress, "address.city"));
            assertFalse(Beans.getPropValueIfPresent(noAddress, "address.city").isPresent());

            assertEquals(Arrays.asList("firstName", "lastName", "age", "address", "fullName"), Beans.getPropNames(p, false));
            assertEquals(Arrays.asList("lastName", "age", "fullName"), Beans.getPropNames(noAddress, true));

            final List<String> seen = new ArrayList<>();
            assertEquals(Arrays.asList("firstName", "lastName", "fullName"), Beans.getPropNames(p, (java.util.function.Predicate<String>) n -> {
                seen.add(n);
                return n.endsWith("Name");
            }));
            assertEquals(Arrays.asList("firstName", "lastName", "age", "address", "fullName"), seen);

            seen.clear();
            assertEquals(Arrays.asList("age"), Beans.getPropNames(p, (java.util.function.BiPredicate<String, Object>) (n, v) -> {
                seen.add(n + "=" + (v instanceof G004Address ? "address" : v));
                return v instanceof Integer;
            }));
            assertEquals(Arrays.asList("firstName=Ann", "lastName=initial", "age=7", "address=address", "fullName=Ann initial"), seen);
        }

        // G004-02: mapToBean/mapsToBeans check the bean class once per call (ParserUtil.getBeanInfo does it for a non-null map);
        // a non-bean class is still rejected with the same exception for null/non-null maps and null/empty/bogus selections.
        @Test
        public void testMapToBean_beanClassCheckedOnceWithSameException() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("firstName", "Bob");

            for (final Class<?> cls : Arrays.asList(String.class, Integer.class, ArrayList.class, HashMap.class, Object.class, java.util.Date.class,
                    IllegalStateException.class)) {
                @SuppressWarnings("unchecked")
                final Class<Object> target = (Class<Object>) cls;

                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, target));
                assertNotABean(cls, () -> Beans.mapToBean(map, target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, false, target));
                assertNotABean(cls, () -> Beans.mapToBean(map, false, target));
                assertNotABean(cls, () -> Beans.mapToBean(new HashMap<>(), true, target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, (Collection<String>) null, target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, Collections.<String> emptyList(), target));
                assertNotABean(cls, () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList("bogus"), target));
                assertNotABean(cls, () -> Beans.mapToBean(map, (Collection<String>) null, target));
                assertNotABean(cls, () -> Beans.mapToBean(map, Collections.<String> emptyList(), target));
                assertNotABean(cls, () -> Beans.mapToBean(map, Arrays.asList("bogus"), target));
                assertNotABean(cls, () -> Beans.mapsToBeans(new ArrayList<>(), target));
                assertNotABean(cls, () -> Beans.mapsToBeans(Arrays.asList(map, null), true, target));
                assertNotABean(cls, () -> Beans.mapsToBeans(Arrays.asList(null, map), Arrays.asList("bogus"), target));
            }

            final String nullClass = assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean((Map<String, Object>) null, (Class<Object>) null))
                    .getMessage();
            assertEquals(nullClass, assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, (Class<Object>) null)).getMessage());
            assertEquals(nullClass, assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, false, (Class<Object>) null)).getMessage());
            assertEquals(nullClass,
                    assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, (Collection<String>) null, (Class<Object>) null)).getMessage());
            assertEquals(nullClass,
                    assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("bogus"), (Class<Object>) null)).getMessage());

            // the bean class itself: null map -> null, otherwise a populated new bean
            assertNull(Beans.mapToBean((Map<String, Object>) null, G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, false, G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, (Collection<String>) null, G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, Collections.<String> emptyList(), G004Person.class));
            assertNull(Beans.mapToBean((Map<String, Object>) null, Arrays.asList("firstName", "address.city"), G004Person.class));
            assertEquals(noSetterMessage("bogus", G004Person.class), assertThrows(IllegalArgumentException.class,
                    () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList("firstName", "bogus"), G004Person.class)).getMessage());

            final Map<String, Object> full = new LinkedHashMap<>();
            full.put("FIRST_NAME", "Bob");
            full.put("age", "41");
            full.put("address", CommonUtil.<String, Object> asMap("city", "LA", "bogus", 1));
            full.put("unknown", 1);
            full.put("fullName", "ignored");

            for (final G004Person p : Arrays.asList(Beans.mapToBean(full, G004Person.class), Beans.mapToBean(full, true, G004Person.class),
                    Beans.mapToBean(full, (Collection<String>) null, G004Person.class), Beans.mapsToBeans(Arrays.asList(full), G004Person.class).get(0))) {
                assertEquals("Bob", p.getFirstName());
                assertEquals("initial", p.getLastName());
                assertEquals(41, p.getAge());
                assertEquals("LA", p.getAddress().getCity());
            }

            assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(full, false, G004Person.class));
            assertEquals(Arrays.asList(null, null), Beans.mapsToBeans(Arrays.asList(null, null), G004Person.class));
            assertEquals(0, Beans.mapsToBeans(null, G004Person.class).size());
        }

        // G004-03: mapToBean(Map, Collection, Class) resolves each selected name once; the checks, their messages and their
        // interleaving with the constructor and setter calls are unchanged.
        @Test
        public void testMapToBeanSelected_validationAndWritesInterleaveAsBefore() {
            Beans.mapToBean(CommonUtil.<String, Object> asMap("a", "warm"), G004Recorder.class);

            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("a", "1");
            map.put("b", "2");

            G004Recorder.LOG.clear();
            G004Recorder r = Beans.mapToBean(map, Arrays.asList("b", "A", "a", new String("b")), G004Recorder.class);
            assertEquals("1", r.getA());
            assertEquals("2", r.getB());
            // "A" resolves to property a, but the map has no "A" key, so it is skipped; the second "b" is written again
            assertEquals(Arrays.asList("new", "b=2", "a=1", "b=2"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            assertEquals(noSetterMessage("bogus", G004Recorder.class),
                    assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("a", "bogus", "b"), G004Recorder.class))
                            .getMessage());
            assertEquals(Arrays.asList("new", "a=1"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("b", null, "a"), G004Recorder.class));
            assertEquals(Arrays.asList("new", "b=2"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            r = Beans.mapToBean(map, Collections.<String> emptyList(), G004Recorder.class);
            assertNull(r.getA());
            assertEquals(Arrays.asList("new"), G004Recorder.LOG);

            G004Recorder.LOG.clear();
            final List<G004Recorder> list = Beans.mapsToBeans(Arrays.asList(map, null, CommonUtil.<String, Object> asMap("a", "x")), Arrays.asList("a"),
                    G004Recorder.class);
            assertEquals(3, list.size());
            assertEquals("1", list.get(0).getA());
            assertNull(list.get(1));
            assertEquals("x", list.get(2).getA());
            assertEquals(Arrays.asList("new", "a=1", "new", "a=x"), G004Recorder.LOG);
        }

        // G004-03: read-only, unknown, aliased and dot-separated selected names keep their outcomes and messages.
        @Test
        public void testMapToBeanSelected_readOnlyUnknownAndDottedNames() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("firstName", "Cy");
            map.put("FIRST_NAME", "Dee");
            map.put("lastName", null);
            map.put("age", 3);
            map.put("address.city", "SF");
            map.put("fullName", "x");

            G004Person p = Beans.mapToBean(map, Arrays.asList("firstName", "age"), G004Person.class);
            assertEquals("Cy", p.getFirstName());
            assertEquals("initial", p.getLastName());
            assertEquals(3, p.getAge());
            assertNull(p.getAddress());

            p = Beans.mapToBean(map, Arrays.asList("FIRST_NAME", "lastName", "address.city"), G004Person.class);
            assertEquals("Dee", p.getFirstName());
            assertNull(p.getLastName());
            assertEquals("SF", p.getAddress().getCity());

            p = Beans.mapToBean(map, Arrays.asList("address.zip", "first_name"), G004Person.class);
            assertNull(p.getFirstName());
            assertNull(p.getAddress());

            for (final String readOnly : Arrays.asList("fullName", "FULL_NAME", "full_name", "address.label")) {
                assertEquals(readOnlyMessage(readOnly, G004Person.class),
                        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList("firstName", readOnly), G004Person.class))
                                .getMessage(),
                        readOnly);
                assertEquals(readOnlyMessage(readOnly, G004Person.class),
                        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(Arrays.asList(map), Arrays.asList(readOnly), G004Person.class))
                                .getMessage(),
                        readOnly);
                assertEquals(readOnlyMessage(readOnly, G004Person.class), assertThrows(IllegalArgumentException.class,
                        () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList(readOnly), G004Person.class)).getMessage(), readOnly);
            }

            for (final String unknown : Arrays.asList("nope", "address.nope", "nope.city")) {
                assertEquals(noSetterMessage(unknown, G004Person.class),
                        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, Arrays.asList(unknown), G004Person.class)).getMessage(),
                        unknown);
                assertEquals(noSetterMessage(unknown, G004Person.class), assertThrows(IllegalArgumentException.class,
                        () -> Beans.mapToBean((Map<String, Object>) null, Arrays.asList(unknown), G004Person.class)).getMessage(), unknown);
            }
        }
    }

    // ---- perf review 2026-10-08 G004 end ----
    // ---- perf review 2026-10-08 G005 begin ----
    @Nested
    class G005 {
        private static final java.util.regex.Pattern UUID_FRAGMENT = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]");
        private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{3}@email\\.com");
        private static final String NOT_A_BEAN_STRING = "Not a bean class: java.lang.String - CharSequence implementations are never treated as beans";
        private static final String NOT_A_BEAN_INTEGER = "Not a bean class: java.lang.Integer - Number implementations are never treated as beans";
        private static final String LONG_PROP = "longPropertyName" + "Abcdefghij".repeat(12); // 136 characters: past the 128-character cache key limit

        public static class G005Address {
            private String city;
            private String zip;
            private int number;

            public String getCity() {
                return city;
            }

            public void setCity(final String city) {
                this.city = city;
            }

            public String getZip() {
                return zip;
            }

            public void setZip(final String zip) {
                this.zip = zip;
            }

            public int getNumber() {
                return number;
            }

            public void setNumber(final int number) {
                this.number = number;
            }
        }

        public static class G005Person {
            private String name;
            private String email;
            private String mail;
            private String workEMail;
            private int age;
            private G005Address address;

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

            public String getMail() {
                return mail;
            }

            public void setMail(final String mail) {
                this.mail = mail;
            }

            public String getWorkEMail() {
                return workEMail;
            }

            public void setWorkEMail(final String workEMail) {
                this.workEMail = workEMail;
            }

            public int getAge() {
                return age;
            }

            public void setAge(final int age) {
                this.age = age;
            }

            public G005Address getAddress() {
                return address;
            }

            public void setAddress(final G005Address address) {
                this.address = address;
            }
        }

        public static class G005Node {
            private String label;
            private G005Node parent;

            public String getLabel() {
                return label;
            }

            public void setLabel(final String label) {
                this.label = label;
            }

            public G005Node getParent() {
                return parent;
            }

            public void setParent(final G005Node parent) {
                this.parent = parent;
            }
        }

        public static class G005Names {
            private String userID;
            private String htmlURLValue;
            private int value2Go;
            private String x;
            private G005Address homeAddress;
            private String longPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij;

            public String getUserID() {
                return userID;
            }

            public void setUserID(final String userID) {
                this.userID = userID;
            }

            public String getHtmlURLValue() {
                return htmlURLValue;
            }

            public void setHtmlURLValue(final String htmlURLValue) {
                this.htmlURLValue = htmlURLValue;
            }

            public int getValue2Go() {
                return value2Go;
            }

            public void setValue2Go(final int value2Go) {
                this.value2Go = value2Go;
            }

            public String getX() {
                return x;
            }

            public void setX(final String x) {
                this.x = x;
            }

            public G005Address getHomeAddress() {
                return homeAddress;
            }

            public void setHomeAddress(final G005Address homeAddress) {
                this.homeAddress = homeAddress;
            }

            public String getLongPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij() {
                return longPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij;
            }

            public void setLongPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij(
                    final String value) {
                this.longPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij = value;
            }
        }

        private G005Person person(final String name, final String email, final String mail, final int age, final G005Address address) {
            final G005Person person = new G005Person();
            person.setName(name);
            person.setEmail(email);
            person.setMail(mail);
            person.setAge(age);
            person.setAddress(address);
            return person;
        }

        private G005Address address(final String city, final String zip, final int number) {
            final G005Address address = new G005Address();
            address.setCity(city);
            address.setZip(zip);
            address.setNumber(number);
            return address;
        }

        private void assertUuidFragment(final String value) {
            assertNotNull(value);
            assertTrue(UUID_FRAGMENT.matcher(value).matches(), value);
        }

        private void assertEmail(final String value) {
            assertNotNull(value);
            assertTrue(EMAIL.matcher(value).matches(), value);
        }

        // G005-01: a random String is still the 16-character prefix of a version-4 UUID (12 characters + "@email.com" for a
        // property whose name contains "email", ignoring case), at the top level and in a nested bean, and values differ.
        @Test
        public void testNewRandomBean_stringPropertiesAreUuidFragments() {
            final Set<String> names = new HashSet<>();

            for (int i = 0; i < 300; i++) {
                final G005Person person = Beans.newRandomBean(G005Person.class);

                assertUuidFragment(person.getName());
                assertUuidFragment(person.getMail()); // "mail" does not contain "email"
                assertEmail(person.getEmail());
                assertEmail(person.getWorkEMail()); // "workEMail" contains "email" ignoring case
                assertNotNull(person.getAddress());
                assertUuidFragment(person.getAddress().getCity());
                assertUuidFragment(person.getAddress().getZip());
                assertTrue(names.add(person.getName()), person.getName());
            }

            final G005Node node = Beans.newRandomBean(G005Node.class);
            assertUuidFragment(node.getLabel());
            assertNull(node.getParent()); // the reference cycle stops the recursion
        }

        // G005-01: the same shapes through randomize(..) and newRandomBeanList(..); unselected properties are untouched.
        @Test
        public void testRandomize_andNewRandomBeanList_stringShapes() {
            final G005Person person = person(null, null, "keep", 5, null);
            Beans.randomize(person, Arrays.asList("name", "email"));
            assertUuidFragment(person.getName());
            assertEmail(person.getEmail());
            assertEquals("keep", person.getMail());
            assertNull(person.getWorkEMail());
            assertEquals(5, person.getAge());
            assertNull(person.getAddress());

            Beans.randomize(person);
            assertUuidFragment(person.getName());
            assertUuidFragment(person.getMail());
            assertEmail(person.getEmail());
            assertEmail(person.getWorkEMail());
            assertUuidFragment(person.getAddress().getCity());

            final List<G005Person> list = Beans.newRandomBeanList(G005Person.class, 20);
            final Set<String> names = new HashSet<>();
            assertEquals(20, list.size());

            for (final G005Person e : list) {
                assertUuidFragment(e.getName());
                assertEmail(e.getEmail());
                assertUuidFragment(e.getAddress().getZip());
                assertTrue(names.add(e.getName()), e.getName());
            }

            final List<G005Person> partial = Beans.newRandomBeanList(G005Person.class, Arrays.asList("workEMail"), 3);
            assertEquals(3, partial.size());

            for (final G005Person e : partial) {
                assertEmail(e.getWorkEMail());
                assertNull(e.getName());
                assertNull(e.getAddress());
            }

            assertTrue(Beans.newRandomBeanList(G005Person.class, 0).isEmpty());
        }

        // G005-01: callers on several threads each get well-formed values (every value comes from the calling thread's own
        // ThreadLocalRandom).
        @Test
        public void testNewRandomBean_concurrentCallers() throws Exception {
            final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);

            try {
                final List<java.util.concurrent.Future<Set<String>>> futures = new ArrayList<>();

                for (int t = 0; t < 4; t++) {
                    futures.add(executor.submit(() -> {
                        final Set<String> names = new HashSet<>();

                        for (int i = 0; i < 200; i++) {
                            final G005Person person = Beans.newRandomBean(G005Person.class);
                            assertUuidFragment(person.getName());
                            assertEmail(person.getEmail());
                            names.add(person.getName());
                        }

                        return names;
                    }));
                }

                final Set<String> all = new HashSet<>();

                for (final java.util.concurrent.Future<Set<String>> future : futures) {
                    all.addAll(future.get(60, java.util.concurrent.TimeUnit.SECONDS));
                }

                assertEquals(800, all.size());
            } finally {
                executor.shutdownNow();
            }
        }

        // G005-02: the null-selection (default) paths of beanToMap / deepBeanToMap / beanToFlatMap and N.convert(bean, Map)
        // produce the same maps, keys and order; a null bean leaves the output untouched.
        @Test
        public void testBeanToMap_nullSelectionPathsKeepResults() {
            final G005Address address = address("c", null, 4);
            final G005Person person = person("n", null, "m", 3, address);

            final Map<String, Object> shallow = Beans.beanToMap(person);
            assertEquals(Arrays.asList("name", "mail", "age", "address"), new ArrayList<>(shallow.keySet()));
            assertEquals("n", shallow.get("name"));
            assertEquals("m", shallow.get("mail"));
            assertEquals(3, shallow.get("age"));
            assertSame(address, shallow.get("address"));
            assertEquals(shallow, CommonUtil.convert(person, Map.class));

            final Map<String, Object> output = new LinkedHashMap<>();
            output.put("k", 1);
            Beans.beanToMap(person, output);
            assertEquals(Arrays.asList("k", "name", "mail", "age", "address"), new ArrayList<>(output.keySet()));

            final Map<String, Object> snake = new LinkedHashMap<>();
            Beans.beanToMap(person, (Collection<String>) null, NamingPolicy.SCREAMING_SNAKE_CASE, snake);
            assertEquals(Arrays.asList("NAME", "MAIL", "AGE", "ADDRESS"), new ArrayList<>(snake.keySet()));

            final Map<String, Object> deep = Beans.deepBeanToMap(person);
            assertEquals(Arrays.asList("name", "mail", "age", "address"), new ArrayList<>(deep.keySet()));
            final Map<String, Object> expectedAddress = new LinkedHashMap<>();
            expectedAddress.put("city", "c");
            expectedAddress.put("number", 4);
            assertEquals(expectedAddress, deep.get("address"));
            assertEquals(Arrays.asList("city", "number"), new ArrayList<>(((Map<?, ?>) deep.get("address")).keySet()));

            final TreeMap<String, Object> deepTree = Beans.deepBeanToMap(person, IntFunctions.<String, Object> ofTreeMap());
            assertTrue(deepTree.get("address") instanceof TreeMap, String.valueOf(deepTree.get("address")));

            final Map<String, Object> flat = Beans.beanToFlatMap(person);
            assertEquals(Arrays.asList("name", "mail", "age", "address.city", "address.number"), new ArrayList<>(flat.keySet()));
            assertEquals("c", flat.get("address.city"));

            final Map<String, Object> flatSnake = Beans.beanToFlatMap(person, (Collection<String>) null, NamingPolicy.SCREAMING_SNAKE_CASE,
                    IntFunctions.<String, Object> ofLinkedHashMap());
            assertEquals(Arrays.asList("NAME", "MAIL", "AGE", "ADDRESS.CITY", "ADDRESS.NUMBER"), new ArrayList<>(flatSnake.keySet()));

            final Map<String, Object> deepOutput = new LinkedHashMap<>();
            deepOutput.put("k", 1);
            Beans.deepBeanToMap(person, deepOutput);
            Beans.beanToFlatMap(person, deepOutput);
            assertEquals(Arrays.asList("k", "name", "mail", "age", "address", "address.city", "address.number"), new ArrayList<>(deepOutput.keySet()));

            // the selection paths are unchanged: a selected null property is kept, two spellings of one property give one entry
            assertEquals(Arrays.asList("name", "email"), new ArrayList<>(Beans.beanToMap(person, Arrays.asList("name", "email", "NAME")).keySet()));

            final Map<String, Object> untouched = new LinkedHashMap<>();
            untouched.put("k", 1);
            Beans.beanToMap(null, untouched);
            Beans.deepBeanToMap(null, untouched);
            Beans.beanToFlatMap(null, untouched);
            assertEquals(Collections.singletonMap("k", 1), untouched);
            assertTrue(Beans.beanToMap((Object) null).isEmpty());
            assertTrue(Beans.deepBeanToMap((Object) null).isEmpty());
            assertTrue(Beans.beanToFlatMap((Object) null).isEmpty());
        }

        // G005-02: a non-bean is rejected on the default paths with the same IllegalArgumentException as before, before
        // anything is written, and the deep/flat cycle guard is clean afterwards.
        @Test
        public void testBeanToMap_nullSelectionPathsRejectNonBeanBeforeWriting() {
            final Map<String, Object> output = new LinkedHashMap<>();
            output.put("k", 1);

            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap("abc")).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap("abc", output)).getMessage());
            assertEquals(NOT_A_BEAN_STRING,
                    assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap("abc", (Collection<String>) null, NamingPolicy.SNAKE_CASE, output))
                            .getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap("abc")).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap("abc", output)).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap("abc")).getMessage());
            assertEquals(NOT_A_BEAN_STRING, assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap("abc", output)).getMessage());
            assertEquals(NOT_A_BEAN_INTEGER, assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap(5, output)).getMessage());
            assertEquals(NOT_A_BEAN_INTEGER, assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(5, output)).getMessage());
            assertEquals(NOT_A_BEAN_INTEGER, assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(5, output)).getMessage());
            // the selection paths reject it the same way
            assertEquals(NOT_A_BEAN_STRING,
                    assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap("abc", Arrays.asList("name"), output)).getMessage());
            assertEquals(NOT_A_BEAN_STRING,
                    assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap("abc", Arrays.asList("name"), output)).getMessage());
            assertEquals(Collections.singletonMap("k", 1), output);

            // the cycle guard was left clean by those failures
            final G005Node child = new G005Node();
            final G005Node parent = new G005Node();
            child.setLabel("c");
            child.setParent(parent);
            parent.setLabel("p");
            assertEquals(Arrays.asList("label", "parent.label"), new ArrayList<>(Beans.beanToFlatMap(child).keySet()));
            assertEquals("p", ((Map<?, ?>) Beans.deepBeanToMap(child).get("parent")).get("label"));

            parent.setParent(child);
            final String deepMessage = assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(child)).getMessage();
            final String flatMessage = assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(child)).getMessage();
            assertTrue(deepMessage.startsWith("Cyclic bean reference cannot be converted to a map: "), deepMessage);
            assertTrue(flatMessage.startsWith("Cyclic bean reference cannot be flattened: "), flatMessage);

            parent.setParent(null);
            assertEquals(2, Beans.deepBeanToMap(child).size());
        }

        private String key(final NamingPolicy policy, final String propName) {
            return policy == NamingPolicy.CAMEL_CASE || policy == NamingPolicy.NO_CHANGE ? propName : policy.convert(propName);
        }

        private List<String> keys(final Map<String, Object> map) {
            return new ArrayList<>(map.keySet());
        }

        // G005-04: under every NamingPolicy each key is exactly policy.convert(propertyName) (the name itself for
        // CAMEL_CASE / NO_CHANGE) on every conversion path - top level, selected, nested, flattened, builder - both
        // before and after the converted names are cached, and for a property name too long to be cached.
        @Test
        public void testBeanToMap_keyNamingPoliciesConvertEveryKeyLikeThePolicy() {
            final G005Names bean = new G005Names();
            bean.setUserID("u");
            bean.setHtmlURLValue("h");
            bean.setValue2Go(2);
            bean.setX("x");
            bean.setHomeAddress(address("c", "z", 3));
            bean.setLongPropertyNameAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghijAbcdefghij(
                    "l");

            final List<String> propNames = Beans.getPropNameList(G005Names.class);
            assertEquals(Arrays.asList("userID", "htmlURLValue", "value2Go", "x", "homeAddress", LONG_PROP), propNames);
            final List<String> addressPropNames = Arrays.asList("city", "zip", "number");

            for (int round = 0; round < 2; round++) {
                for (final NamingPolicy policy : NamingPolicy.values()) {
                    final List<String> top = new ArrayList<>();
                    final List<String> flat = new ArrayList<>();

                    for (final String propName : propNames) {
                        top.add(key(policy, propName));

                        if ("homeAddress".equals(propName)) {
                            for (final String nested : addressPropNames) {
                                flat.add(key(policy, propName) + "." + key(policy, nested));
                            }
                        } else {
                            flat.add(key(policy, propName));
                        }
                    }

                    final List<String> nestedKeys = new ArrayList<>();

                    for (final String nested : addressPropNames) {
                        nestedKeys.add(key(policy, nested));
                    }

                    final String message = policy + " round " + round;
                    final String addressKey = key(policy, "homeAddress");

                    assertEquals(top, keys(Beans.beanToMap(bean, false, null, policy)), message);
                    assertEquals(top, keys(Beans.beanToMap(bean, propNames, policy, IntFunctions.<String, Object> ofLinkedHashMap())), message);

                    final Map<String, Object> deep = Beans.deepBeanToMap(bean, false, null, policy);
                    assertEquals(top, keys(deep), message);
                    assertEquals(nestedKeys, new ArrayList<>(((Map<?, ?>) deep.get(addressKey)).keySet()), message);

                    final Map<String, Object> deepSelected = Beans.deepBeanToMap(bean, propNames, policy, IntFunctions.<String, Object> ofLinkedHashMap());
                    assertEquals(top, keys(deepSelected), message);
                    assertEquals(nestedKeys, new ArrayList<>(((Map<?, ?>) deepSelected.get(addressKey)).keySet()), message);

                    assertEquals(flat, keys(Beans.beanToFlatMap(bean, false, null, policy)), message);
                    assertEquals(flat, keys(Beans.beanToFlatMap(bean, propNames, policy, IntFunctions.<String, Object> ofLinkedHashMap())), message);

                    assertEquals(top, keys(Beans.mapBuilder(bean).naming(policy).toMap()), message);
                    assertEquals(top, keys(Beans.mapBuilder(bean).naming(policy).skipNulls().toMap()), message);
                    final Map<String, Object> builderDeep = Beans.mapBuilder(bean).naming(policy).skipNulls().deep().toMap();
                    assertEquals(top, keys(builderDeep), message);
                    assertEquals(nestedKeys, new ArrayList<>(((Map<?, ?>) builderDeep.get(addressKey)).keySet()), message);
                    assertEquals(flat, keys(Beans.mapBuilder(bean).naming(policy).flat().toMap()), message);
                    assertEquals(flat, keys(Beans.mapBuilder(bean).naming(policy).skipNulls().flat().toMap()), message);

                    assertEquals("u", Beans.beanToMap(bean, false, null, policy).get(key(policy, "userID")), message);
                    assertEquals("l", Beans.beanToFlatMap(bean, false, null, policy).get(key(policy, LONG_PROP)), message);
                }
            }

            // spot checks of the converted forms themselves
            assertEquals(Arrays.asList("user_id", "html_url_value", "value2_go", "x", "home_address", Strings.toSnakeCase(LONG_PROP)),
                    keys(Beans.beanToMap(bean, false, null, NamingPolicy.SNAKE_CASE)));
            assertEquals("HOME_ADDRESS.CITY", keys(Beans.beanToFlatMap(bean, false, null, NamingPolicy.SCREAMING_SNAKE_CASE)).get(4));
        }
    }
}
