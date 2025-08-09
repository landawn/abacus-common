package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.landawn.abacus.AbstractTest;

public class IterablesTest extends AbstractTest {

    @Test
    public void test_reverse() {

        {
            final List<Integer> list = N.asList(1, 2, 3, 4, 5);
            final List<Integer> reversed = Iterables.reverse(list);
            assertEquals(N.asList(5, 4, 3, 2, 1), reversed);

            reversed.add(6);
            assertEquals(N.asList(6, 1, 2, 3, 4, 5), list);

            list.remove(1);
            assertEquals(N.asList(5, 4, 3, 2, 6), reversed);
        }

        {
            final List<Integer> list = N.asList(1, 2, 3, 4, 5);
            final List<Integer> reversed = Lists.reverse(list);
            assertEquals(N.asList(5, 4, 3, 2, 1), reversed);

            reversed.add(6);
            assertEquals(N.asList(6, 1, 2, 3, 4, 5), list);

            list.remove(1);
            assertEquals(N.asList(5, 4, 3, 2, 6), reversed);
        }

        {
            final List<Integer> list = N.asList(1, 2, 3, 4, 5);
            final List<Integer> reversed = N.reverseToList(list);
            assertEquals(N.asList(5, 4, 3, 2, 1), reversed);
        }

        {
            final Collection<Integer> c = N.asLinkedHashSet(1, 2, 3, 4, 5);
            N.reverse(c);
            assertEquals(N.asLinkedHashSet(5, 4, 3, 2, 1), c);
        }
    }

}
