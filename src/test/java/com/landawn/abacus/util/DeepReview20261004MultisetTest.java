package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DeepReview20261004MultisetTest extends TestBase {

    @Test
    void identityBackedMultisetsCompareCountsByValue() {
        final Object key = new Object();
        final Multiset<Object> first = new Multiset<>(IdentityHashMap.class);
        final Multiset<Object> second = new Multiset<>(IdentityHashMap.class);
        first.add(key, 1000);
        first.add(null, 2);
        second.add(null, 2);
        second.add(key, 1000);

        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        final Multiset<Object> copied = new Multiset<>(IdentityHashMap.class);
        copied.addAll(first);
        assertEquals(first, copied);
        assertEquals(first.hashCode(), copied.hashCode());

        final int originalHash = first.hashCode();
        first.removeAllOccurrencesOf(key);
        first.add(key, 1000);
        assertEquals(originalHash, first.hashCode());

        second.add(key);
        assertFalse(first.equals(second));
        assertFalse(second.equals(first));
    }

    @Test
    void wrappedIdentityMapsAlsoCompareCountsByValue() {
        final Supplier<Map<Object, MutableInt>> supplier = () -> Collections.synchronizedMap(new IdentityHashMap<>());
        final Multiset<Object> first = new Multiset<>(supplier);
        final Multiset<Object> second = new Multiset<>(supplier);
        final Object key = new Object();
        first.add(key, 1000);
        second.add(key, 1000);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void differentKeyEquivalencesDoNotMakeEqualityAsymmetric() {
        final String firstKey = new String("key");
        final String secondKey = new String("key");
        final Multiset<String> identity = new Multiset<>(IdentityHashMap.class);
        final Multiset<String> ordinary = new Multiset<>();
        final Multiset<String> otherIdentity = new Multiset<>(IdentityHashMap.class);
        identity.add(firstKey, 3);
        ordinary.add(secondKey, 3);
        otherIdentity.add(secondKey, 3);

        assertFalse(identity.equals(otherIdentity));
        assertFalse(otherIdentity.equals(identity));
        assertFalse(identity.equals(ordinary));
        assertFalse(ordinary.equals(identity));

        ordinary.clear();
        ordinary.add(firstKey, 3);
        assertTrue(identity.equals(ordinary));
        assertTrue(ordinary.equals(identity));
        assertEquals(identity.hashCode(), ordinary.hashCode());
    }

    @Test
    void incompatibleSortedKeysCompareUnequalWithoutThrowing() {
        final Multiset<String> sorted = new Multiset<>(TreeMap.class);
        sorted.add("key", 3);
        final Multiset<Object> nullKey = new Multiset<>();
        nullKey.add(null, 3);
        final Multiset<Object> foreignKey = new Multiset<>();
        foreignKey.add(new Object(), 3);

        assertFalse(sorted.equals(nullKey));
        assertFalse(nullKey.equals(sorted));
        assertFalse(sorted.equals(foreignKey));
        assertFalse(foreignKey.equals(sorted));
    }
}
