package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;

public class NFlatTest extends NTestSupport {

    @Test
    public void testFlatMap() {
        assertEquals(List.of("a", "b", "c"), N.flatMap(new String[] { "ab", "c" }, NTestSupport::splitToChars));
        assertTrue(N.flatMap((String[]) null, NTestSupport::splitToChars).isEmpty());
        assertEquals(Set.of("a", "b", "c"), N.flatMap(new String[] { "ab", "ca" }, NTestSupport::splitToChars, IntFunctions.ofSet()));
        assertEquals(List.of("w", "o", "r", "l", "d"), N.flatMap(new String[] { "hi", "world", "test" }, 1, 2, NTestSupport::splitToChars));
        assertEquals(Set.of("b", "o"), N.flatMap(new String[] { "hi", "bob", "test", "bib" }, 1, 2, NTestSupport::splitToChars, HashSet::new));

        assertEquals(List.of("l", "o", "c", "k", "d", "o", "o", "r"), N.flatMap(Arrays.asList("key", "lock", "door"), 1, 3, NTestSupport::splitToChars));
        assertEquals(Set.of("l", "o", "c", "k"), N.flatMap(Arrays.asList("key", "lock", "lol"), 1, 3, NTestSupport::splitToChars, HashSet::new));
        assertEquals(List.of("o", "n", "e", "t", "w", "o"), N.flatMap((Iterable<String>) Arrays.asList("one", "two"), NTestSupport::splitToChars));
        assertEquals(Set.of("o", "n", "e", "t"), N.flatMap((Iterable<String>) Arrays.asList("one", "too"), NTestSupport::splitToChars, IntFunctions.ofSet()));
        assertEquals(List.of("h", "i", "b", "y", "e"), N.flatMap(Arrays.asList("hi", "bye").iterator(), NTestSupport::splitToChars));
        assertEquals(Set.of("h", "i", "b"), N.flatMap(Arrays.asList("hi", "bib").iterator(), NTestSupport::splitToChars, IntFunctions.ofSet()));

        assertEquals(Arrays.asList('o', 'n', 't', 'w', 't', 'h', 'f', 'o', 'f', 'i'), N.flatMap(stringArray, s -> Arrays.asList(s.charAt(0), s.charAt(1))));
        assertEquals(Arrays.asList('o', 'n', 't', 'w'), N.flatMap(stringArray, 0, 2, s -> Arrays.asList(s.charAt(0), s.charAt(1))));
        assertEquals(Arrays.asList('c', 'd'), N.flatMap(Arrays.asList("ab", "cd"), 1, 2, s -> Arrays.asList(s.charAt(0), s.charAt(1))));
        assertEquals(0, N.flatMap(Arrays.asList("a", "b", "c"), 1, 1, s -> Arrays.asList(s.split("")), IntFunctions.ofList()).size());

        List<Character> linked = N.flatMap(new LinkedList<>(Arrays.asList("ab", "cd", "ef")), 0, 2, s -> {
            List<Character> chars = new ArrayList<>();
            for (char ch : s.toCharArray()) {
                chars.add(ch);
            }
            return chars;
        }, IntFunctions.ofList());
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), linked);

        assertEquals(Arrays.asList('a', 'b'),
                N.flatMap(new String[] { "", "a", "", "bc" }, s -> s.isEmpty() ? Collections.emptyList() : Arrays.asList(s.charAt(0))));
        assertEquals(Arrays.asList("a", "b", "c", "d"),
                N.flatMap(Arrays.asList(Arrays.asList("a", "b"), null, Arrays.asList("c", "d"), null), list -> list == null ? Collections.emptyList() : list));
    }

    @Test
    public void testFlatMapTwoLevels() {
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), N.flatMap(new String[] { "ab-cd", "ef" }, splitByDash, NTestSupport::splitToChars));
        assertEquals(Set.of("a", "b", "c", "e", "f"), N.flatMap(new String[] { "ab-ca", "ef-fa" }, splitByDash, NTestSupport::splitToChars, HashSet::new));
        assertEquals(Arrays.asList("u", "v", "w", "x", "y", "z"), N.flatMap(Arrays.asList("uv-wx", "yz"), splitByDash, NTestSupport::splitToChars));
        assertEquals(Arrays.asList("1", "2", "3", "4", "5", "6"), N.flatMap(Arrays.asList("12-34", "56").iterator(), splitByDash, NTestSupport::splitToChars));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8),
                N.flatMap(Arrays.asList(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)), Arrays.asList(Arrays.asList(5, 6), Arrays.asList(7, 8))),
                        Fn.identity(), Fn.identity()));
    }

    @Test
    public void testFlatMapAndFilter() {
        assertEquals(Arrays.asList('d', 'e', 'f', 'g', 'h', 'i'),
                N.flatMapAndFilter(Arrays.asList("abc", "de", "fghi"), s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList()), c -> c > 'c'));
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" "));
        Predicate<String> filter = s -> s.length() == 1;
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), N.flatMapAndFilter(Arrays.asList("a b", "c", "d e f"), mapper, filter));
        assertEquals(Set.of("a", "b", "c", "d", "e", "f"), N.flatMapAndFilter(Arrays.asList("a b a", "c c", "d e f d"), mapper, filter, HashSet::new));
        assertTrue(N.flatMapAndFilter(null, mapper, filter).isEmpty());
    }
}
