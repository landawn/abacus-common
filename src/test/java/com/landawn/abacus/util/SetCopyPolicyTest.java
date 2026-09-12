package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SetCopyPolicyTest {
    private enum Flag {
        A, B
    }

    private record Key(int rank) {
    }

    @Test
    void perEntryOrderingAndMembershipSurviveIndependentCopy() {
        Comparator<Key> reverse = Comparator.comparingInt(Key::rank).reversed();
        TreeSet<Key> sorted = new TreeSet<>(reverse);
        sorted.addAll(List.of(new Key(1), new Key(2)));
        Map<String, Set<Key>> source = new LinkedHashMap<>();
        source.put("sorted", sorted);
        SetMultimap<String, Key> original = SetMultimap.wrap(source);
        source.put("empty", new TreeSet<>(reverse));
        SetMultimap<String, Key> copy = original.copy();
        assertSame(reverse, ((SortedSet<Key>) copy.get("sorted")).comparator());
        assertEquals(new ArrayList<>(sorted), new ArrayList<>(copy.get("sorted")));
        assertNotSame(sorted, copy.get("sorted"));
        assertTrue(copy.get("empty").isEmpty());
        copy.get("sorted").clear();
        assertEquals(2, sorted.size());

        String first = new String("same");
        String second = new String("same");
        IdentityHashSet<String> identity = new IdentityHashSet<>();
        identity.add(first);
        identity.add(second);
        LinkedHashSet<String> insertion = new LinkedHashSet<>(List.of("z", "a"));
        Map<String, Set<String>> mixed = new LinkedHashMap<>();
        mixed.put("identity", identity);
        mixed.put("insertion", insertion);
        mixed.put("hash", new HashSet<>(List.of("x")));
        AtomicInteger factoryCalls = new AtomicInteger();
        SetMultimap<String, String> policies = SetMultimap.wrap(mixed, () -> {
            factoryCalls.incrementAndGet();
            return new HashSet<>();
        }).copy();
        assertEquals(0, factoryCalls.get(), "known policies require no supplier probe");
        assertEquals(2, policies.get("identity").size());
        assertTrue(policies.get("identity").contains(first));
        assertFalse(policies.get("identity").contains(new String("same")));
        assertEquals(List.of("z", "a"), new ArrayList<>(policies.get("insertion")));
        EnumSet<Flag> enums = EnumSet.of(Flag.B);
        SetMultimap<String, Flag> enumCopy = SetMultimap.wrap(Map.<String, Set<Flag>> of("enum", enums)).copy();
        assertInstanceOf(EnumSet.class, enumCopy.get("enum"));
        enumCopy.get("enum").add(Flag.A);
        assertEquals(EnumSet.of(Flag.B), enums);
    }

    @Test
    void unknownFactoriesRejectAliasingAndDetectedElementLoss() {
        Set<String> unknown = Collections.unmodifiableSet(new LinkedHashSet<>(List.of("A", "a")));
        Map<String, Set<String>> source = new LinkedHashMap<>();
        source.put("first", unknown);
        Set<String> later = new HashSet<>();
        SetMultimap<String, String> aliased = SetMultimap.wrap(source, () -> later);
        SetMultimap<String, String> lossy = SetMultimap.wrap(source, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        SetMultimap<String, String> occupied = SetMultimap.wrap(source, () -> new HashSet<>(List.of("occupied")));
        SetMultimap<String, String> nullFactory = SetMultimap.wrap(source, () -> null);
        SetMultimap<String, String> independent = SetMultimap.wrap(source, LinkedHashSet::new);
        // Empty or null value sets can be inserted through the externally owned backing map after wrap.
        source.put("later", later);
        assertThrows(IllegalArgumentException.class, aliased::copy);
        assertTrue(later.isEmpty(), "later input must not be filled by a factory alias");
        assertThrows(IllegalArgumentException.class, lossy::copy);
        assertThrows(IllegalArgumentException.class, occupied::copy);
        assertThrows(IllegalArgumentException.class, nullFactory::copy);
        SetMultimap<String, String> valid = independent.copy();
        assertEquals(unknown, valid.get("first"));
        source.put("null", null);
        assertThrows(IllegalArgumentException.class, independent::copy);

        // A reused empty output must be rejected even though no elements expose the alias yet.
        Map<String, Set<String>> empties = new LinkedHashMap<>();
        empties.put("first", unknown);
        Set<String> reused = new HashSet<>();
        SetMultimap<String, String> reusedOutput = SetMultimap.wrap(empties, () -> reused);
        empties.put("first", Collections.emptySet());
        empties.put("second", Collections.emptySet());
        assertThrows(IllegalArgumentException.class, reusedOutput::copy);
        assertTrue(reused.isEmpty());

        Map<String, Set<String>> sharedMap = new LinkedHashMap<>();
        SetMultimap<String, String> aliasedMap = new SetMultimap<>(() -> sharedMap, HashSet::new);
        assertThrows(IllegalArgumentException.class, aliasedMap::copy);
        assertTrue(sharedMap.isEmpty());
    }
}
