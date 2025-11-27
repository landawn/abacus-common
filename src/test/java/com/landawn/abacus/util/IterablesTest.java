package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class IterablesTest extends AbstractTest {

    @Test
    public void test_reverse() {

        {
            final List<Integer> list = CommonUtil.asList(1, 2, 3, 4, 5);
            final List<Integer> reversed = Iterables.reverse(list);
            assertEquals(CommonUtil.asList(5, 4, 3, 2, 1), reversed);

            reversed.add(6);
            assertEquals(CommonUtil.asList(6, 1, 2, 3, 4, 5), list);

            list.remove(1);
            assertEquals(CommonUtil.asList(5, 4, 3, 2, 6), reversed);
        }

        {
            final List<Integer> list = CommonUtil.asList(1, 2, 3, 4, 5);
            final List<Integer> reversed = Lists.reverse(list);
            assertEquals(CommonUtil.asList(5, 4, 3, 2, 1), reversed);

            reversed.add(6);
            assertEquals(CommonUtil.asList(6, 1, 2, 3, 4, 5), list);

            list.remove(1);
            assertEquals(CommonUtil.asList(5, 4, 3, 2, 6), reversed);
        }

        {
            final List<Integer> list = CommonUtil.asList(1, 2, 3, 4, 5);
            final List<Integer> reversed = CommonUtil.reverseToList(list);
            assertEquals(CommonUtil.asList(5, 4, 3, 2, 1), reversed);
        }

        {
            final Collection<Integer> c = CommonUtil.asLinkedHashSet(1, 2, 3, 4, 5);
            CommonUtil.reverse(c);
            assertEquals(CommonUtil.asLinkedHashSet(5, 4, 3, 2, 1), c);
        }
    }

}
