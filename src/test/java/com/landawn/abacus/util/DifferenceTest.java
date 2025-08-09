package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;

public class DifferenceTest extends AbstractTest {

    @Test
    public void test_001() {
        Account a = new Account();
        a.setLastUpdateTime(Dates.currentTimestampRolled(1, TimeUnit.DAYS));
        a.setCreatedTime(Dates.currentTimestampRolled(1, TimeUnit.DAYS));
        Account b = new Account();
        b.setLastUpdateTime(Dates.currentTimestamp());
        b.setCreatedTime(Dates.currentTimestamp());
        var diff = BeanDifference.of(a, b);

        println(diff);

        assertFalse(diff.withDifferentValues().containsKey("lastUpdateTime"));

        diff = BeanDifference.of(a, b, N.asList("lastUpdateTime", "createdTime"));
        assertTrue(diff.withDifferentValues().containsKey("lastUpdateTime"));
    }

    @Test
    public void test_002() {
        List<Account> listA = Beans.fill(Account.class, 10);
        List<Account> listB = Beans.fill(Account.class, 10);

        listA.get(0).setGUI(listB.get(3).getGUI());
        listA.get(5).setGUI(listB.get(7).getGUI());
        listA.get(7).setGUI(listB.get(1).getGUI());

        listA.set(4, Beans.copy(listB.get(2)));
        listA.set(6, Beans.copy(listB.get(8)));

        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(listA, listB, Account::getGUI);

        println(diff);

        println(diff.withDifferentValues());

        for (int i = 0; i < listA.size(); i++) {
            listA.set(i, Beans.copy(listB.get(i)));
        }

        diff = BeanDifference.of(listA, listB, Account::getGUI);

        assertTrue(diff.areEqual());
    }

}
