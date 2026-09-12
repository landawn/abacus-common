package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Difference.BeanDifference;

public class DifferenceBeanTest extends DifferenceTestSupport {

    @Test
    public void testOf() {
        Account a = new Account();
        a.setFirstName("John");
        a.setLastName("Doe");
        a.setStatus(1);
        Account b = new Account();
        b.setFirstName("John");
        b.setLastName("Smith");
        b.setStatus(2);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b);
        assertEquals("John", diff.common().get("firstName"));
        assertEquals(Pair.of("Doe", "Smith"), diff.differentValues().get("lastName"));
        assertTrue(diff.differentValues().containsKey("status"));
        assertFalse(diff.areEqual());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> selected = BeanDifference.of(a, b,
                Arrays.asList("firstName", "lastName"));
        assertEquals(1, selected.differentValues().size());
        assertFalse(selected.differentValues().containsKey("status"));

        Account equal = new Account();
        equal.setFirstName("John");
        equal.setLastName("Doe");
        equal.setStatus(1);
        assertTrue(BeanDifference.of(a, equal).areEqual());
        assertTrue(BeanDifference.of((Account) null, (Account) null).areEqual());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> leftOnly = BeanDifference.of(a, null);
        assertFalse(leftOnly.onlyOnLeft().isEmpty());
        assertTrue(leftOnly.onlyOnRight().isEmpty());
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> rightOnly = BeanDifference.of(null, a);
        assertTrue(rightOnly.onlyOnLeft().isEmpty());
        assertFalse(rightOnly.onlyOnRight().isEmpty());
    }

    @Test
    public void testOf_DiffIgnore() {
        Account a = new Account();
        a.setFirstName("John");
        a.setLastUpdateTime(Dates.currentTimestamp());
        Account b = new Account();
        b.setFirstName("John");
        b.setLastUpdateTime(Dates.currentTimestampPlus(1, TimeUnit.DAYS));
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(a, b);
        assertFalse(diff.common().containsKey("lastUpdateTime"));
        assertFalse(diff.differentValues().containsKey("lastUpdateTime"));

        DiffIncludedBean included = new DiffIncludedBean();
        included.setValue("left");
        DiffIgnoredBean ignored = new DiffIgnoredBean();
        ignored.setValue("right");
        assertFalse(BeanDifference.of(included, ignored).differentValues().containsKey("value"));
        assertFalse(BeanDifference.of(ignored, included).differentValues().containsKey("value"));
    }

    @Test
    public void testOfCollections() {
        Account a1 = new Account();
        a1.setGUI("1");
        a1.setFirstName("John");
        a1.setLastName("Doe");
        a1.setStatus(1);
        Account b1 = new Account();
        b1.setGUI("1");
        b1.setFirstName("John");
        b1.setLastName("Smith");
        b1.setStatus(2);
        BeanDifference<List<Account>, List<Account>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> coll = BeanDifference
                .of(Arrays.asList(a1), Arrays.asList(b1), Arrays.asList("firstName", "lastName"), Account::getGUI);
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> inner = coll.differentValues().get("1");
        assertEquals("John", inner.common().get("firstName"));
        assertEquals(Pair.of("Doe", "Smith"), inner.differentValues().get("lastName"));
        assertFalse(inner.differentValues().containsKey("status"));

        assertTrue(BeanDifference.of(new ArrayList<Account>(), new ArrayList<Account>(), Account::getGUI).areEqual());
        Account only = new Account();
        only.setGUI("1");
        only.setFirstName("John");
        assertEquals(1, BeanDifference.of(Arrays.asList(only), new ArrayList<Account>(), Account::getGUI).onlyOnLeft().size());
        assertEquals(1, BeanDifference.of(new ArrayList<Account>(), Arrays.asList(only), Account::getGUI).onlyOnRight().size());
    }

    @Test
    public void testOf_Predicates() {
        Account a = new Account();
        a.setFirstName("JOHN");
        a.setLastName("DOE");
        Account b = new Account();
        b.setFirstName("john");
        b.setLastName("doe");
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> caseInsensitive = BeanDifference.ofByValues(a, b,
                (java.util.function.BiPredicate<Object, Object>) (v1,
                        v2) -> v1 instanceof String && v2 instanceof String ? ((String) v1).equalsIgnoreCase((String) v2) : CommonUtil.equals(v1, v2));
        assertTrue(caseInsensitive.common().containsKey("firstName"));
        assertTrue(caseInsensitive.differentValues().isEmpty());

        Account c = new Account();
        c.setFirstName("john");
        c.setLastName("Smith");
        c.setStatus(1);
        a.setStatus(1);
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> tri = BeanDifference.ofByProps(a, c,
                (com.landawn.abacus.util.function.TriPredicate<String, Object, Object>) (propName, v1, v2) -> {
                    if ("firstName".equals(propName) && v1 instanceof String && v2 instanceof String) {
                        return ((String) v1).equalsIgnoreCase((String) v2);
                    }
                    return CommonUtil.equals(v1, v2);
                });
        assertTrue(tri.common().containsKey("firstName"));
        assertTrue(tri.differentValues().containsKey("lastName"));

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> selected = BeanDifference.ofByProps(a, c,
                Arrays.asList("firstName", "lastName"), (com.landawn.abacus.util.function.TriPredicate<String, Object, Object>) (propName, v1, v2) -> {
                    if ("firstName".equals(propName) && v1 instanceof String && v2 instanceof String) {
                        return ((String) v1).equalsIgnoreCase((String) v2);
                    }
                    return CommonUtil.equals(v1, v2);
                });
        assertTrue(selected.common().containsKey("firstName"));
        assertFalse(selected.differentValues().containsKey("status"));
    }

    @Test
    public void testOf_EdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of("string", "another"));
        assertThrows(IllegalArgumentException.class,
                () -> BeanDifference.of(Arrays.asList("not a bean"), Arrays.asList("also not a bean"), Function.identity()));
        assertThrows(IllegalArgumentException.class,
                () -> BeanDifference.of(Arrays.asList(new Account(), "not a bean"), Collections.emptyList(), Function.identity()));
        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(Arrays.asList(null, "not a bean"), Collections.emptyList(), Function.identity()));
        Account a = new Account();
        assertThrows(IllegalArgumentException.class, () -> BeanDifference.ofByValues(a, a, (java.util.function.BiPredicate<Object, Object>) null));
        assertThrows(IllegalArgumentException.class,
                () -> BeanDifference.ofByProps(a, a, (com.landawn.abacus.util.function.TriPredicate<String, Object, Object>) null));
    }

    @Test
    public void testEqualsHashCodeToString() {
        Account a = new Account();
        a.setFirstName("John");
        Account b = new Account();
        b.setFirstName("Jane");
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> d1 = BeanDifference.of(a, b);
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> d2 = BeanDifference.of(a, b);
        assertEquals(d1, d1);
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertNotEquals(d1, null);
        assertNotEquals(d1, "not a difference");
        String str = d1.toString();
        assertTrue(str.contains("areEqual="));
        assertTrue(str.contains("differentValues="));
    }

    @Test
    public void testArrayProperties() {
        ArrayPropBean bean1 = new ArrayPropBean();
        bean1.setName("x");
        bean1.setData(new byte[] { 1, 2, 3 });
        bean1.setMatrix(new int[][] { { 1, 2 }, { 3 } });
        ArrayPropBean bean2 = new ArrayPropBean();
        bean2.setName("x");
        bean2.setData(new byte[] { 1, 2, 3 });
        bean2.setMatrix(new int[][] { { 1, 2 }, { 3 } });
        assertTrue(BeanDifference.of(bean1, bean2).areEqual());

        ArrayPropBean different = new ArrayPropBean();
        different.setName("x");
        different.setData(new byte[] { 1, 2, 4 });
        assertTrue(BeanDifference.of(bean1, different).differentValues().containsKey("data"));

        ArrayPropBean otherName = new ArrayPropBean();
        otherName.setName("y");
        otherName.setData(new byte[] { 1, 2, 3 });
        assertTrue(BeanDifference.of(bean1, otherName, Arrays.asList("data")).areEqual());
    }

    @Test
    public void testBeanVersusNull() {
        NullPropBean bean = new NullPropBean("John", null, 5);
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> left = BeanDifference.of(bean, (Object) null);
        assertEquals(CommonUtil.asMap("name", (Object) "John", "email", null, "score", 5), left.onlyOnLeft());
        assertFalse(left.areEqual());
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> right = BeanDifference.of((Object) null, bean);
        assertEquals(CommonUtil.asMap("name", (Object) "John", "email", null, "score", 5), right.onlyOnRight());
    }
}
