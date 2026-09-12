package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the second {@code Maps} + {@code Beans} review of 2026-08-31.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li><b>B1</b> - the selected {@code beanToMap}/{@code deepBeanToMap}/{@code beanToFlatMap} paths, and
 *       {@code BeanMapBuilder}, now key off the bean's canonical property name rather than echoing the
 *       caller's spelling.</li>
 *   <li><b>B2</b> - {@code Maps.putIfAbsent(Map, K, Supplier)} is the supplier overload of
 *       {@code putIfAbsent}. A bare lambda is an ambiguous method call against the value overload;
 *       pass a typed {@code Supplier}, including when it is initialized with a method reference. On a {@code Map<K, Object>} even a
 *       typed {@code Supplier<Object>} stays ambiguous because it is itself a valid value.</li>
 *   <li><b>B3</b> - {@code Beans.mergeIntoIf} replaces the {@code mergeInto} {@code BiPredicate} overloads,
 *       which made {@code mergeInto(src, tgt, (a, b) -> a)} an ambiguous method call.</li>
 *   <li><b>B4/B5/D1/D2/D3/J2</b> - the documented-behaviour and validation items from the same review.</li>
 * </ul>
 *
 * <p>Note that this class compiling <em>is</em> the regression test for B3: the {@code mergeIntoIf}
 * calls below are written in the natural lambda form that previously failed to compile. B2 uses a
 * typed {@code Supplier} because {@code putIfAbsent(map, key, () -> v)} is ambiguous against the
 * value overload.</p>
 */
public class MapsBeansTest extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------------------------------------

    public static class Addr {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class User {
        private String firstName;
        private int age;
        private Addr address;

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

        public Addr getAddress() {
            return address;
        }

        public void setAddress(final Addr address) {
            this.address = address;
        }
    }

    private static User newUser() {
        final User u = new User();
        u.setFirstName("John");
        u.setAge(30);
        final Addr a = new Addr();
        a.setCity("NY");
        u.setAddress(a);
        return u;
    }

    /** A Map whose capacity constructor rejects large sizes but accepts small ones - see the D2 tests. */
    public static class SizePickyMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;

        static final List<Integer> ATTEMPTS = new ArrayList<>();

        public SizePickyMap(final int size) {
            ATTEMPTS.add(size);

            if (size > 3) {
                throw new IllegalArgumentException("refusing size " + size);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - selected bean-to-map conversions key off the canonical property name
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B1CanonicalKeys {

        @Test
        public void beanToMap_aliasSelection_emitsCanonicalKey() {
            final User u = newUser();

            // Every one of these spellings resolves to the "firstName" property, and all must key the
            // result the same way the unselected overload does.
            for (final String alias : new String[] { "firstName", "FIRSTNAME", "first_name", "getFirstName", "FirstName" }) {
                assertEquals(Map.of("firstName", "John"), Beans.beanToMap(u, CommonUtil.asList(alias)), "alias: " + alias);
            }
        }

        @Test
        public void beanToMap_aliasSelection_matchesUnselected_underEveryNamingPolicy() {
            final User u = newUser();

            for (final NamingPolicy np : new NamingPolicy[] { NamingPolicy.CAMEL_CASE, NamingPolicy.SNAKE_CASE, NamingPolicy.SCREAMING_SNAKE_CASE,
                    NamingPolicy.KEBAB_CASE, NamingPolicy.NO_CHANGE }) {

                final Map<String, Object> unselected = Beans.beanToMap(u, (Collection<String>) null, np, IntFunctions.<String, Object> ofLinkedHashMap());

                for (final String alias : new String[] { "firstName", "FIRSTNAME", "getFirstName" }) {
                    final Map<String, Object> selected = Beans.beanToMap(u, CommonUtil.asList(alias), np, IntFunctions.<String, Object> ofLinkedHashMap());

                    assertEquals(CommonUtil.asSet(unselected.keySet().iterator().next()), selected.keySet(), "policy " + np + ", alias " + alias);
                }
            }
        }

        @Test
        public void beanToMap_aliasSelection_wasEmittingTheCallersSpelling() {
            final User u = newUser();

            // The exact regressions: before the fix these produced {get_first_name=John} and {firstname=John}.
            assertEquals(Map.of("first_name", "John"),
                    Beans.beanToMap(u, CommonUtil.asList("getFirstName"), NamingPolicy.SNAKE_CASE, IntFunctions.<String, Object> ofLinkedHashMap()));
            assertEquals(Map.of("first_name", "John"),
                    Beans.beanToMap(u, CommonUtil.asList("FIRSTNAME"), NamingPolicy.SNAKE_CASE, IntFunctions.<String, Object> ofLinkedHashMap()));
            assertEquals(Map.of("firstName", "John", "age", 30), Beans.beanToMap(u, CommonUtil.asList("first_name", "AGE")));
        }

        @Test
        public void beanToMap_intoOutputMap_alsoCanonical() {
            final User u = newUser();
            final Map<String, Object> output = new LinkedHashMap<>();

            Beans.beanToMap(u, CommonUtil.asList("getFirstName"), NamingPolicy.SNAKE_CASE, output);

            assertEquals(Map.of("first_name", "John"), output);
        }

        @Test
        public void beanToMap_duplicateAliasesOfOneProperty_collapseToOneEntry() {
            final User u = newUser();

            assertEquals(Map.of("firstName", "John"), Beans.beanToMap(u, CommonUtil.asList("firstName", "first_name", "getFirstName")));
        }

        @Test
        public void deepBeanToMap_aliasSelection_emitsCanonicalKey() {
            final User u = newUser();

            assertEquals(Map.of("address", Map.of("city", "NY")), Beans.deepBeanToMap(u, CommonUtil.asList("ADDRESS")));
        }

        @Test
        public void beanToFlatMap_aliasSelection_prefixIsCanonical() {
            final User u = newUser();

            // Before the fix this produced {ADDRESS.city=NY} - the parent segment from the caller and the
            // nested segment from the nested bean, inside a single key.
            assertEquals(Map.of("address.city", "NY"), Beans.beanToFlatMap(u, CommonUtil.asList("ADDRESS")));
        }

        @Test
        public void unmatchedSelectionStillThrows_andNamesTheCallersSpelling() {
            final User u = newUser();

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap(u, CommonUtil.asList("bogusProp")));

            assertTrue(e.getMessage().contains("bogusProp"), e.getMessage());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - BeanMapBuilder: canonical keys, canonical exclusions, canonical filter names
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B1BeanMapBuilder {

        @Test
        public void select_emitsCanonicalKey() {
            assertEquals(Map.of("firstName", "John"), Beans.mapBuilder(newUser()).select("first_name").toMap());
        }

        @Test
        public void exclude_worksWithEitherSpelling_inBothPaths() {
            // Before the fix exclude() meant opposite things in the two paths: with no select(),
            // exclude("first_name") did nothing and exclude("firstName") worked; after select("first_name")
            // it was exactly reversed.
            for (final String alias : new String[] { "firstName", "first_name", "getFirstName" }) {
                final Map<String, Object> allProps = Beans.mapBuilder(newUser()).exclude(alias).toMap();
                assertFalse(allProps.containsKey("firstName"), "all-props path, exclude alias: " + alias);
                assertTrue(allProps.containsKey("age"), "all-props path must keep other properties");

                final Map<String, Object> selected = Beans.mapBuilder(newUser()).select("first_name", "age").exclude(alias).toMap();
                assertEquals(Map.of("age", 30), selected, "selected path, exclude alias: " + alias);
            }
        }

        @Test
        public void exclude_unmatchedName_excludesNothing() {
            assertEquals(3, Beans.mapBuilder(newUser()).exclude("noSuchProperty").toMap().size());
        }

        @Test
        public void filter_receivesCanonicalNames_inBothPaths() {
            final List<String> seenAll = new ArrayList<>();
            Beans.mapBuilder(newUser()).filter((n, v) -> {
                seenAll.add(n);
                return true;
            }).toMap();
            assertEquals(CommonUtil.asList("firstName", "age", "address"), seenAll);

            // Before the fix this saw the caller's spellings, so a predicate written against property names
            // silently stopped matching once a select(..) was added.
            final List<String> seenSelected = new ArrayList<>();
            Beans.mapBuilder(newUser()).select("first_name", "AGE").filter((n, v) -> {
                seenSelected.add(n);
                return true;
            }).toMap();
            assertEquals(CommonUtil.asList("firstName", "age"), seenSelected);
        }

        @Test
        public void filter_byCanonicalName_selectsTheSamePropertyInBothPaths() {
            final Map<String, Object> allProps = Beans.mapBuilder(newUser()).filter((n, v) -> "firstName".equals(n)).toMap();
            final Map<String, Object> selected = Beans.mapBuilder(newUser()).select("first_name", "AGE").filter((n, v) -> "firstName".equals(n)).toMap();

            assertEquals(Map.of("firstName", "John"), allProps);
            assertEquals(allProps, selected);
        }

        @Test
        public void duplicateAliases_readTheGetterOnce() {
            final AtomicInteger filterCalls = new AtomicInteger();

            final Map<String, Object> result = Beans.mapBuilder(newUser()).select("firstName", "first_name", "getFirstName").filter((n, v) -> {
                filterCalls.incrementAndGet();
                return true;
            }).toMap();

            assertEquals(Map.of("firstName", "John"), result);
            assertEquals(1, filterCalls.get(), "three spellings of one property must be considered once");
        }

        @Test
        public void deepAndFlatShapes_alsoUseCanonicalKeys() {
            assertEquals(Map.of("address", Map.of("city", "NY")), Beans.mapBuilder(newUser()).select("ADDRESS").deep().toMap());
            assertEquals(Map.of("address.city", "NY"), Beans.mapBuilder(newUser()).select("ADDRESS").flat().toMap());

            // ... and on the value-capturing path too (skipNulls forces the getters to be read up front).
            assertEquals(Map.of("address", Map.of("city", "NY")), Beans.mapBuilder(newUser()).select("ADDRESS").skipNulls().deep().toMap());
            assertEquals(Map.of("address.city", "NY"), Beans.mapBuilder(newUser()).select("ADDRESS").skipNulls().flat().toMap());
        }

        @Test
        public void intoOutputMap_usesCanonicalKeys() {
            final Map<String, Object> output = new LinkedHashMap<>();

            Beans.mapBuilder(newUser()).select("first_name").into(output);

            assertEquals(Map.of("firstName", "John"), output);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B2 - Maps.putIfAbsent(Map, K, Supplier)
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B2PutIfAbsentSupplier {

        @Test
        public void acceptsAnExplicitSupplier() {
            final Map<String, String> map = new HashMap<>();
            final Supplier<String> supplier = () -> "v";

            assertNull(Maps.putIfAbsent(map, "k", supplier));
            assertEquals(Map.of("k", "v"), map);
        }

        @Test
        public void acceptsATypedMethodReference() {
            final Map<String, List<String>> map = new HashMap<>();
            final Supplier<List<String>> supplier = ArrayList::new;

            assertNull(Maps.putIfAbsent(map, "k", supplier));
            assertEquals(List.of(), map.get("k"));
        }

        @Test
        public void returnsPreviousValue_andDoesNotInvokeSupplierWhenPresent() {
            final Map<String, String> map = new HashMap<>();
            map.put("present", "old");
            final AtomicInteger calls = new AtomicInteger();
            final Supplier<String> creating = () -> {
                calls.incrementAndGet();
                return "new";
            };
            final Supplier<String> created = () -> {
                calls.incrementAndGet();
                return "created";
            };

            assertEquals("old", Maps.putIfAbsent(map, "present", creating));
            assertEquals(0, calls.get());
            assertEquals("old", map.get("present"));

            assertNull(Maps.putIfAbsent(map, "absent", created));
            assertEquals(1, calls.get());
            assertEquals("created", map.get("absent"));
        }

        @Test
        public void treatsANullValueAsAbsent() {
            final Map<String, String> map = new HashMap<>();
            map.put("k", null);
            final Supplier<String> supplier = () -> "v";

            assertNull(Maps.putIfAbsent(map, "k", supplier));
            assertEquals("v", map.get("k"));
        }

        @Test
        public void rejectsNullMapAndNullSupplier() {
            final Supplier<String> supplier = () -> "v";
            assertThrows(IllegalArgumentException.class, () -> Maps.putIfAbsent((Map<String, String>) null, "k", supplier));
            assertThrows(IllegalArgumentException.class, () -> Maps.putIfAbsent(new HashMap<String, String>(), "k", (Supplier<String>) null));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B4 - a null value / null supplier result is stored, matching Map.putIfAbsent
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B4NullIsStored {

        @Test
        public void valueOverload_storesNull_likeTheJdk() {
            final Map<String, String> ours = new HashMap<>();
            final Map<String, String> jdk = new HashMap<>();

            assertNull(Maps.putIfAbsent(ours, "k", (String) null));
            jdk.putIfAbsent("k", null);

            assertEquals(jdk, ours);
            assertTrue(ours.containsKey("k"));
            assertNull(ours.get("k"));
        }

        @Test
        public void supplierOverload_storesANullResult() {
            final Map<String, String> map = new HashMap<>();

            assertNull(Maps.putIfAbsent(map, "k", (Supplier<String>) () -> null));

            assertTrue(map.containsKey("k"));
            assertNull(map.get("k"));
        }

        @Test
        public void aStoredNullIsStillAbsent_soASecondCallSuppliesAgain() {
            final Map<String, String> map = new HashMap<>();
            final AtomicInteger calls = new AtomicInteger();

            final Supplier<String> first = () -> {
                calls.incrementAndGet();
                return null;
            };
            final Supplier<String> second = () -> {
                calls.incrementAndGet();
                return "second";
            };
            Maps.putIfAbsent(map, "k", first);
            Maps.putIfAbsent(map, "k", second);

            assertEquals(2, calls.get(), "a null mapping counts as absent, so the supplier runs again");
            assertEquals("second", map.get("k"));
        }

        @Test
        public void getOrPutIfAbsent_isTheStrictSibling() {
            // Contrast documented on putIfAbsent(Map, K, Supplier): the get-or-create family guarantees a non-null
            // result and therefore rejects a null-producing supplier.
            assertThrows(IllegalArgumentException.class, () -> Maps.getOrPutIfAbsent(new HashMap<String, String>(), "k", () -> null));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B5 - getByPath index-segment grammar
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B5IndexGrammar {

        private Map<String, Object> pathMap() {
            final Map<String, Object> inner = new HashMap<>();
            inner.put("k", Arrays.asList(Arrays.asList(7, 8)));

            final Map<String, Object> m = new HashMap<>();
            m.put("a", Arrays.asList(10, 20));
            m.put("a]", 1);
            m.put("a[b]", 2);
            m.put("m", inner);
            return m;
        }

        @Test
        public void wellFormedIndexesStillResolve() {
            final Map<String, Object> m = pathMap();

            assertEquals(10, (Integer) Maps.getByPath(m, "a[0]"));
            assertEquals(20, (Integer) Maps.getByPath(m, "a[1]"));
            assertEquals(8, (Integer) Maps.getByPath(m, "m.k[0][1]"));
        }

        @Test
        public void malformedIndexSuffixesAreRejected() {
            final Map<String, Object> m = pathMap();

            // "a[0]junk]" used to resolve to 10, and "a[+1]"/"a[0x1]" used to resolve to 20, because only
            // the extracted substrings were validated and Numbers.toInt is a general numeric parser.
            for (final String path : new String[] { "a[0]junk]", "a[+1]", "a[0x1]", "a[-1]", "a[]", "a[ ]", "a[1a]", "a[a]", "a[0", "a[99999999999]",
                    "m.k[0][1]x]" }) {
                assertNull(Maps.getByPath(m, path), "should be unresolvable: " + path);
            }
        }

        @Test
        public void aDotInsideBracketsIsStillAPathSeparator() {
            // Not an index-grammar case: the path is split on '.' first, so "a[1.0]" is the two segments
            // "a[1" and "0]" and never reaches the index parser. Pinned so the grammar tests above are not
            // misread as covering it.
            assertNull(Maps.getByPath(pathMap(), "a[1.0]"));
        }

        @Test
        public void malformedIndexIsUnresolvable_notMerelyNullValued() {
            final Map<String, Object> m = pathMap();

            assertFalse(Maps.getByPathIfExists(m, "a[0]junk]").isPresent());
            assertFalse(Maps.getByPathAsInt(m, "a[0x1]").isPresent());
            assertFalse(Maps.getByPathAs(m, "a[+1]", Integer.class).isPresent());
            assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(m, "a[]", -1));

            // ... while a well-formed one still resolves through the same entry points.
            assertTrue(Maps.getByPathIfExists(m, "a[0]").isPresent());
            assertEquals(10, Maps.getByPathAsInt(m, "a[0]").orElse(-1));
        }

        @Test
        public void aKeyEndingInABracketIsStillAnOrdinaryKey() {
            // Regression guard for the earlier "a]" fix: a segment is index syntax only with BOTH brackets.
            assertEquals(1, (Integer) Maps.getByPath(pathMap(), "a]"));
        }

        @Test
        public void aKeyContainingBracketsIsNotAddressable() {
            // Documented reserved-syntax limitation, pinned so it is a decision rather than an accident.
            assertNull(Maps.getByPath(pathMap(), "a[b]"));
        }

        @Test
        public void outOfBoundsIndexIsUnresolvable() {
            assertNull(Maps.getByPath(pathMap(), "a[2]"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D1 / J2 - documented conversion rules for the non-numeric accessors
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class ConversionRules {

        @Test
        public void emptyTextIsAbsentForNumericAccessorsOnly() {
            final Map<String, Object> m = new HashMap<>();
            m.put("e", "");

            assertFalse(Maps.getAsByte(m, "e").isPresent());
            assertFalse(Maps.getAsShort(m, "e").isPresent());
            assertFalse(Maps.getAsInt(m, "e").isPresent());
            assertFalse(Maps.getAsLong(m, "e").isPresent());
            assertFalse(Maps.getAsFloat(m, "e").isPresent());
            assertFalse(Maps.getAsDouble(m, "e").isPresent());
            assertFalse(Maps.getAs(m, "e", Integer.class).isPresent());

            // The non-numeric accessors deliberately treat "" as a value that is present.
            assertEquals('\0', Maps.getAsChar(m, "e").orElse('x'));
            assertFalse(Maps.getAsBoolean(m, "e").orElse(true));
            assertEquals("", Maps.getAsString(m, "e").orElse("fallback"));
            assertEquals("", Maps.getAs(m, "e", String.class).orElse("fallback"));
        }

        @Test
        public void booleanTrueSpellings() {
            final Map<String, Object> m = new HashMap<>();
            m.put("true", "true");
            m.put("TRUE", "TRUE");
            m.put("Y", "Y");
            m.put("y", "y");
            m.put("one", "1");

            for (final String key : new String[] { "true", "TRUE", "Y", "y", "one" }) {
                assertTrue(Maps.getAsBoolean(m, key).orElse(false), "should be true: " + m.get(key));
            }

            // Spellings a reader may expect to be true but are not - the reason the javadoc now enumerates.
            final Map<String, Object> f = new HashMap<>();
            f.put("T", "T");
            f.put("yes", "yes");
            f.put("on", "on");
            f.put("junk", "junk");

            for (final String key : new String[] { "T", "yes", "on", "junk" }) {
                assertFalse(Maps.getAsBoolean(f, key).orElse(true), "should be false: " + f.get(key));
            }
        }

        @Test
        public void getAsWithBooleanClassCannotDistinguishPresentButNotABoolean() {
            // Pins the corrected javadoc: getAs(map, key, Boolean.class) performs the very same conversion,
            // so it is not the escape hatch the old note claimed it was.
            final Map<String, Object> m = new HashMap<>();
            m.put("junk", "junk");

            assertEquals(Maps.getAsBoolean(m, "junk").orElse(true), Maps.getAs(m, "junk", Boolean.class).orElse(true));
            assertFalse(Maps.getAs(m, "junk", Boolean.class).orElse(true));

            // The raw value is how you actually tell the two apart.
            assertEquals("junk", Maps.getIfExists(m, "junk").orElse(null));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D2 / D3 - documented behaviour of the target-map probe and of putAllIf's return
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class DocumentedBehaviour {

        @Test
        public void sizeSensitiveMapClassKeepsTypePreservationAtWorkableSizes() {
            // The size-1 retry in newTargetMap must not be turned into an unconditional blacklist: a class
            // that merely rejected one size has to stay type-preserved at the sizes it does accept.
            final SizePickyMap<String, Integer> big = new SizePickyMap<>(0);
            big.put("a", 1);
            big.put("b", 2);
            big.put("c", 3);
            big.put("d", 4);
            big.put("e", 5);

            assertEquals(HashMap.class, Maps.filter(big, (k, v) -> true).getClass(), "a rejected size falls back to HashMap");

            final SizePickyMap<String, Integer> small = new SizePickyMap<>(0);
            small.put("a", 1);

            assertEquals(SizePickyMap.class, Maps.filter(small, (k, v) -> true).getClass(), "an accepted size stays type-preserved");
        }

        @Test
        public void putAllIfReportsThatAnEntryWasPut_notThatContentsChanged() {
            final Map<String, Integer> target = new HashMap<>();
            target.put("a", 1);
            final Map<String, Integer> source = new HashMap<>();
            source.put("a", 1);

            assertTrue(Maps.putAllIf(target, source, k -> true), "an equal mapping was still put");
            assertEquals(Map.of("a", 1), target);

            assertFalse(Maps.putAllIf(target, source, k -> false), "nothing passed the filter");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J3 / J5 - claims the javadoc now makes about sizing hints and null tolerance
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class DocumentedSizingAndNullTolerance {

        @Test
        public void capacityHintsAreOverEstimatesWhenInputsCollapseOntoOneKey() {
            // The "Result Sizing" note now calls these hints rather than exact sizes.
            assertEquals(2, Maps.zip(CommonUtil.asList("a", "a", "b"), CommonUtil.asList(1, 2, 3)).size(), "duplicate zip keys collapse");
            assertEquals(1, Maps.invert(CommonUtil.asMap("a", 1, "b", 1)).size(), "duplicate values collapse on invert");

            // A non-Collection Iterable contributes a hint of 0 but still zips every element.
            final Iterable<String> keys = () -> CommonUtil.asList("x", "y", "z").iterator();
            assertEquals(3, Maps.zip(keys, CommonUtil.asList(1, 2, 3)).size());
        }

        @Test
        public void getOrEmptyAccessorsToleratANullMap() {
            assertTrue(Maps.getOrEmptyListIfAbsent((Map<String, List<String>>) null, "k").isEmpty());
            assertTrue(Maps.getOrEmptySetIfAbsent((Map<String, Set<String>>) null, "k").isEmpty());
            assertTrue(Maps.getOrEmptyMapIfAbsent((Map<String, Map<String, String>>) null, "k").isEmpty());

            // ... and the empty result is immutable, as documented.
            assertThrows(UnsupportedOperationException.class, () -> Maps.getOrEmptyListIfAbsent((Map<String, List<String>>) null, "k").add("x"));
        }

        @Test
        public void flatInvertNullAndEmptyCollectionBehaviour() {
            assertTrue(Maps.flatInvert((Map<String, List<String>>) null).isEmpty());

            final Map<String, List<String>> m = new LinkedHashMap<>();
            m.put("empty", new ArrayList<>());
            m.put("nullColl", null);
            m.put("a", CommonUtil.asList("x"));

            final Map<String, List<String>> inverted = Maps.flatInvert(m);

            assertEquals(Map.of("x", CommonUtil.asList("a")), inverted, "a null or empty collection contributes nothing");
        }

        @Test
        public void flatInvertNullElementBecomesANullKey() {
            final Map<String, List<String>> m = new LinkedHashMap<>();
            m.put("a", Arrays.asList("x", null));

            final Map<String, List<String>> inverted = Maps.flatInvert(m);

            assertEquals(2, inverted.size());
            assertTrue(inverted.containsKey(null), "a null element is not skipped - it becomes a null key");
            assertEquals(CommonUtil.asList("a"), inverted.get(null));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 - Beans.mergeIntoIf
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B3MergeIntoIf {

        @Test
        public void mergeIntoAcceptsABareBinaryOperatorLambda() {
            // Previously "reference to mergeInto is ambiguous": a BiPredicate and a BinaryOperator are both
            // two-argument functional interfaces, so neither three-arg overload was more specific.
            final User source = newUser();
            final User target = new User();
            target.setFirstName("Jane");
            target.setAge(25);

            Beans.mergeInto(source, target, (sourceVal, targetVal) -> sourceVal);

            assertEquals("John", target.getFirstName());
            assertEquals(30, target.getAge());
        }

        @Test
        public void mergeIntoIfAcceptsABareBiPredicateLambda() {
            final User source = newUser();
            final User target = new User();
            target.setFirstName("Jane");
            target.setAge(25);

            Beans.mergeIntoIf(source, target, (propName, propValue) -> "firstName".equals(propName));

            assertEquals("John", target.getFirstName());
            assertEquals(25, target.getAge(), "age was not selected by the filter");
        }

        @Test
        public void mergeIntoIfWithMergeFunc() {
            final User source = newUser();
            final User target = new User();
            target.setAge(12);

            Beans.mergeIntoIf(source, target, (n, v) -> "age".equals(n), (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));

            assertEquals(42, target.getAge());
        }

        @Test
        public void mergeIntoIfWithPropNameConverter() {
            final User source = newUser();
            final User target = new User();

            Beans.mergeIntoIf(source, target, (n, v) -> "firstName".equals(n), propName -> propName);

            assertEquals("John", target.getFirstName());
        }

        @Test
        public void mergeIntoIfKeepsTheSelectionStrictness() {
            // A filter is a selection: a property that passes it must exist on the target.
            final User source = newUser();

            assertThrows(IllegalArgumentException.class, () -> Beans.mergeIntoIf(source, new Addr(), (n, v) -> true));
        }

        @Test
        public void mergeIntoIfValidatesArguments() {
            final User source = newUser();

            assertThrows(IllegalArgumentException.class, () -> Beans.mergeIntoIf(source, null, (n, v) -> true));
            assertThrows(IllegalArgumentException.class, () -> Beans.mergeIntoIf(source, new User(), (java.util.function.BiPredicate<String, Object>) null));
        }

        @Test
        public void mergeIntoIfWithNullSourceIsANoOp() {
            final User target = newUser();

            assertEquals(target, Beans.mergeIntoIf(null, target, (n, v) -> true));
            assertEquals("John", target.getFirstName());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Cross-check: the canonical-key change did not disturb the unselected conversions
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class UnselectedPathsUnchanged {

        @Test
        public void unselectedConversionsAreUntouched() {
            final User u = newUser();

            assertEquals(CommonUtil.asList("firstName", "age", "address"), new ArrayList<>(Beans.beanToMap(u).keySet()));
            assertEquals(Map.of("firstName", "John", "age", 30, "address.city", "NY"), Beans.beanToFlatMap(u));
            assertEquals(Map.of("firstName", "John", "age", 30, "address", Map.of("city", "NY")), Beans.deepBeanToMap(u));
        }

        @Test
        public void beanToMapStillOmitsNullPropertiesByDefault() {
            final User u = new User();
            u.setFirstName("John");

            final Map<String, Object> m = Beans.beanToMap(u);

            assertEquals(Map.of("firstName", "John", "age", 0), m);
            assertFalse(m.containsKey("address"));
        }

        @Test
        public void selectedConversionsStillIncludeNullValuedProperties() {
            final User u = new User();
            u.setFirstName("John");

            final Map<String, Object> m = Beans.beanToMap(u, CommonUtil.asList("firstName", "address"));

            assertTrue(m.containsKey("address"));
            assertNull(m.get("address"));
        }

        @Test
        public void mapBuilderRoundTripsThroughMapToBean() {
            final User u = newUser();

            final Map<String, Object> m = Beans.mapBuilder(u).select("first_name", "AGE").toMap();
            final User back = Beans.mapToBean(m, User.class);

            assertEquals("John", back.getFirstName());
            assertEquals(30, back.getAge());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Guard: the excluded-name set is resolved per bean class, not cached across classes
    // ------------------------------------------------------------------------------------------------

    @Test
    public void excludeIsResolvedAgainstTheBeanBeingConverted() {
        // "city" is a property of Addr but not of User: excluding it must affect only the Addr conversion.
        final Addr addr = new Addr();
        addr.setCity("NY");

        assertEquals(Map.of(), Beans.mapBuilder(addr).exclude("CITY").toMap());
        assertEquals(3, Beans.mapBuilder(newUser()).exclude("CITY").toMap().size());
    }

    @Test
    public void selectionSetIsNotSharedBetweenBuilders() {
        final Set<String> shared = new LinkedHashSet<>(CommonUtil.asList("first_name"));

        assertEquals(Map.of("firstName", "John"), Beans.mapBuilder(newUser()).select(shared).toMap());
        assertEquals(Map.of("firstName", "John"), Beans.mapBuilder(newUser()).select(shared).toMap());
        assertEquals(new LinkedHashSet<>(CommonUtil.asList("first_name")), shared, "the caller's collection must not be mutated");
    }
}
