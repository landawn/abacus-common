package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Difference. */
@Tag("slow-test")
public class DifferencePerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G023 end ----
    // ---- perf review 2026-10-08 G024 begin ----
    @Nested
    class G024 {

        public static class Person {
            private long id;
            private String name;
            private Integer age;
            private String email;
            private String note;

            public Person() {
            }

            Person(final long id, final String name, final Integer age, final String email, final String note) {
                this.id = id;
                this.name = name;
                this.age = age;
                this.email = email;
                this.note = note;
            }

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

            public Integer getAge() {
                return age;
            }

            public void setAge(final Integer age) {
                this.age = age;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(final String email) {
                this.email = email;
            }

            public String getNote() {
                return note;
            }

            public void setNote(final String note) {
                this.note = note;
            }
        }

        public static class ExactAfterFuzzy {
            public String id = "1";
            public String userName = "fuzzy";
            public String username = "exact";
        }

        public static class IdAndLowerName {
            public String id = "1";
            public String username = "exact";
            public String extra = "right-only";
            @com.landawn.abacus.annotation.DiffIgnore
            public String secret = "ignored";
        }

        public static class LowerName {
            public String username;

            public LowerName() {
            }

            LowerName(final String username) {
                this.username = username;
            }
        }

        public static class ActiveIgnoredActive {
            public String userName = "same";
            @com.landawn.abacus.annotation.DiffIgnore
            public String username = "ignored";
            public String user_name = "same";
        }

        public static class IgnoredThenActive {
            @com.landawn.abacus.annotation.DiffIgnore
            public String userName = "ignored";
            public String user_name = "same";
        }

        public static class ActiveThenIgnored {
            public String user_name = "same";
            @com.landawn.abacus.annotation.DiffIgnore
            public String userName = "ignored";
        }

        // G024-01/G024-02: same-class beans (no fuzzy match): results, null/null handling and predicate calls unchanged.
        @Test
        public void testBeanDifferenceOf_sameClassBeans() {
            final Person left = new Person(1, "John", 30, null, "n1");
            final Person right = new Person(1, "John", 31, null, "n2");

            final var diff = Difference.BeanDifference.of(left, right);

            assertEquals(Arrays.asList("id", "name"), new ArrayList<>(diff.common().keySet()));
            assertEquals(1L, diff.common().get("id"));
            assertEquals("John", diff.common().get("name"));
            assertEquals(Arrays.asList("age", "note"), new ArrayList<>(diff.differentValues().keySet()));
            assertEquals(Pair.of(30, 31), diff.differentValues().get("age"));
            assertEquals(Pair.of("n1", "n2"), diff.differentValues().get("note"));
            assertTrue(diff.onlyOnLeft().isEmpty());
            assertTrue(diff.onlyOnRight().isEmpty());
            assertFalse(diff.areEqual());
            assertEquals(diff, Difference.BeanDifference.of(left, right));

            final List<String> calls = new ArrayList<>();
            final var byProps = Difference.BeanDifference.ofByProps(left, right, (name, v1, v2) -> {
                calls.add(name + "=" + v1 + "/" + v2);
                return !"note".equals(name);
            });

            assertEquals(Arrays.asList("id=1/1", "name=John/John", "age=30/31", "email=null/null", "note=n1/n2"), calls);
            assertEquals(Arrays.asList("id", "name", "age"), new ArrayList<>(byProps.common().keySet()));
            assertEquals(Collections.singletonList("note"), new ArrayList<>(byProps.differentValues().keySet()));
            assertTrue(byProps.onlyOnLeft().isEmpty());
            assertTrue(byProps.onlyOnRight().isEmpty());

            final Person same = new Person(7, null, null, null, null);
            final var equal = Difference.BeanDifference.of(same, new Person(7, null, null, null, null));
            assertTrue(equal.areEqual());
            assertEquals(Collections.singletonMap("id", 7L), equal.common());
        }

        // G024-01: the lazily built exact-name set still reserves an exact name declared after the first fuzzy match.
        @Test
        public void testBeanDifferenceOf_exactNameDeclaredAfterFuzzyMatch() {
            final var diff = Difference.BeanDifference.of(new ExactAfterFuzzy(), new IdAndLowerName());

            assertEquals(Arrays.asList("id", "username"), new ArrayList<>(diff.common().keySet()));
            assertEquals("exact", diff.common().get("username"));
            assertEquals(Collections.singletonMap("userName", "fuzzy"), diff.onlyOnLeft());
            assertEquals(Collections.singletonMap("extra", "right-only"), diff.onlyOnRight());
            assertTrue(diff.differentValues().isEmpty());

            final var reverse = Difference.BeanDifference.of(new IdAndLowerName(), new ExactAfterFuzzy());
            assertEquals(Arrays.asList("id", "username"), new ArrayList<>(reverse.common().keySet()));
            assertEquals(Collections.singletonMap("extra", "right-only"), reverse.onlyOnLeft());
            assertEquals(Collections.singletonMap("userName", "fuzzy"), reverse.onlyOnRight());
        }

        // G024-02: an ignored alias between two active fuzzy matches of one right property still triggers the IAE.
        @Test
        public void testBeanDifferenceOf_activeIgnoredActiveAliasesRejected() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> Difference.BeanDifference.of(new ActiveIgnoredActive(), new LowerName("same")));
            assertEquals("Multiple properties of " + ActiveIgnoredActive.class.getName() + " resolve to property 'username' of " + LowerName.class.getName(),
                    e.getMessage());

            assertThrows(IllegalArgumentException.class,
                    () -> Difference.BeanDifference.ofByProps(new ActiveIgnoredActive(), new LowerName(null), (name, v1, v2) -> true));
        }

        // G024-02: an ignored alias before or after the single active match leaves that match intact.
        @Test
        public void testBeanDifferenceOf_ignoredAliasAroundActiveMatch() {
            final var ignoredFirst = Difference.BeanDifference.of(new IgnoredThenActive(), new LowerName("same"));
            assertTrue(ignoredFirst.areEqual());
            assertEquals(Collections.singletonMap("user_name", "same"), ignoredFirst.common());

            final var ignoredLast = Difference.BeanDifference.of(new ActiveThenIgnored(), new LowerName("other"));
            assertFalse(ignoredLast.areEqual());
            assertTrue(ignoredLast.common().isEmpty());
            assertEquals(Collections.singletonMap("user_name", Pair.of("same", "other")), ignoredLast.differentValues());
            assertTrue(ignoredLast.onlyOnLeft().isEmpty());
            assertTrue(ignoredLast.onlyOnRight().isEmpty());
        }

        // G024-01/G024-02: the collection overload runs the all-properties path once per matched pair.
        @Test
        public void testBeanDifferenceOfCollections_allProperties() {
            final List<Person> left = Arrays.asList(new Person(1, "a", 1, null, "x"), new Person(2, "b", 2, "e", "x"), new Person(3, "c", 3, null, "x"));
            final List<Person> right = Arrays.asList(new Person(4, "d", 4, null, "x"), new Person(2, "b", 2, "e", "x"), new Person(1, "a", 9, null, "x"));

            final var diff = Difference.BeanDifference.of(left, right, Person::getId);

            assertEquals(1, diff.common().size());
            assertSame(left.get(1), diff.common().get(0));
            assertEquals(1, diff.onlyOnLeft().size());
            assertSame(left.get(2), diff.onlyOnLeft().get(0));
            assertEquals(1, diff.onlyOnRight().size());
            assertSame(right.get(0), diff.onlyOnRight().get(0));
            assertEquals(Collections.singletonList(1L), new ArrayList<>(diff.differentValues().keySet()));
            assertEquals(Difference.BeanDifference.of(left.get(0), right.get(2)), diff.differentValues().get(1L));
            assertEquals(Collections.singletonMap("age", Pair.of(1, 9)), diff.differentValues().get(1L).differentValues());
        }

        private List<Map<String, Object>> rows(final Object... idAndValues) {
            final List<Map<String, Object>> rows = new ArrayList<>();

            for (int i = 0; i < idAndValues.length; i += 4) {
                final Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", idAndValues[i]);
                row.put("a", idAndValues[i + 1]);
                row.put("b", idAndValues[i + 2]);
                row.put("c", idAndValues[i + 3]);
                rows.add(row);
            }

            return rows;
        }

        // G024-03: a List selection (copied once) compares every matched pair exactly like MapDifference.of(Map, Map, Collection).
        @Test
        public void testMapDifferenceOfCollections_listSelection() {
            final List<Map<String, Object>> left = rows(1, "a1", "b1", "c1", 2, "a2", "b2", "c2", 3, "a3", "b3", "c3", 5, null, "b5", "c5");
            final List<Map<String, Object>> right = rows(3, "a3", "B3", "c3", 1, "a1", "b1", "C1", 4, "a4", "b4", "c4", 2, "A2", "b2", "c2", 5, null, "b5",
                    "C5");
            final List<String> selection = new ArrayList<>(Arrays.asList("a", "b", "a", "zzz", null));
            final List<String> selectionCopy = new ArrayList<>(selection);

            final var diff = Difference.MapDifference.of(left, right, selection, m -> m.get("id"));

            assertEquals(selectionCopy, selection);
            assertEquals(2, diff.common().size());
            assertSame(left.get(0), diff.common().get(0));
            assertSame(left.get(3), diff.common().get(1));
            assertTrue(diff.onlyOnLeft().isEmpty());
            assertEquals(1, diff.onlyOnRight().size());
            assertSame(right.get(2), diff.onlyOnRight().get(0));
            assertEquals(Arrays.asList(2, 3), new ArrayList<>(diff.differentValues().keySet()));
            assertEquals(Difference.MapDifference.of(left.get(1), right.get(3), selection), diff.differentValues().get(2));
            assertEquals(Difference.MapDifference.of(left.get(2), right.get(0), selection), diff.differentValues().get(3));
            assertEquals(Collections.singletonMap("a", Pair.of("a2", "A2")), diff.differentValues().get(2).differentValues());
            assertEquals(Collections.singletonMap("b", Pair.of("b3", "B3")), diff.differentValues().get(3).differentValues());

            final var twoExtractors = Difference.MapDifference.of(left, right, selection, m -> m.get("id"), m -> m.get("id"));
            assertEquals(diff, twoExtractors);

            // null and empty selections still compare every key
            final var all = Difference.MapDifference.of(left, right, m -> m.get("id"));
            assertEquals(all, Difference.MapDifference.of(left, right, (Collection<String>) null, m -> m.get("id")));
            assertEquals(all, Difference.MapDifference.of(left, right, new ArrayList<String>(), m -> m.get("id")));
            assertEquals(Arrays.asList(1, 2, 3, 5), new ArrayList<>(all.differentValues().keySet()));
            assertTrue(all.common().isEmpty());
        }

        // G024-03: a Set selection is used as given for every pair; no selection is read when no pair matches.
        @Test
        public void testMapDifferenceOfCollections_setSelectionAndNoMatches() {
            final List<Map<String, Object>> left = rows(1, "x", "y", "z", 2, "x", "y", "z");
            final List<Map<String, Object>> right = rows(1, "X", "y", "z", 2, "x", "y", "Z");
            final Set<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitive.add("A");

            final var viaSet = Difference.MapDifference.of(left, right, caseInsensitive, m -> m.get("id"));
            assertEquals(Collections.singletonList(1), new ArrayList<>(viaSet.differentValues().keySet()));
            assertEquals(Collections.singletonMap("a", Pair.of("x", "X")), viaSet.differentValues().get(1).differentValues());
            assertEquals(1, viaSet.common().size());
            assertSame(left.get(1), viaSet.common().get(0));

            final var viaList = Difference.MapDifference.of(left, right, Arrays.asList("A"), m -> m.get("id"));
            assertTrue(viaList.differentValues().isEmpty());
            assertEquals(2, viaList.common().size());

            final AtomicInteger reads = new AtomicInteger();
            final List<String> keys = Arrays.asList("a", "b");
            final Collection<String> countingSelection = new java.util.AbstractCollection<>() {
                @Override
                public Iterator<String> iterator() {
                    reads.incrementAndGet();
                    return keys.iterator();
                }

                @Override
                public int size() {
                    reads.incrementAndGet();
                    return keys.size();
                }
            };

            final var noMatches = Difference.MapDifference.of(left, rows(8, "x", "y", "z", 9, "x", "y", "z"), countingSelection, m -> m.get("id"));
            assertEquals(0, reads.get());
            assertEquals(2, noMatches.onlyOnLeft().size());
            assertEquals(2, noMatches.onlyOnRight().size());

            final var matched = Difference.MapDifference.of(left, right, countingSelection, m -> m.get("id"));
            assertTrue(reads.get() > 0);
            assertEquals(Difference.MapDifference.of(left, right, keys, m -> m.get("id")), matched);
        }
    }
}
