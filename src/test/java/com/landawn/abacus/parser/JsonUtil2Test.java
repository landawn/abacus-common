package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.pjo.basic.Account;
import com.landawn.abacus.entity.pjo.basic.AccountDevice;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.JsonUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@Tag("old-test")
public class JsonUtil2Test extends AbstractTest {

    @Test
    public void test_01() {
        Map<String, Object> map = N.asMap("abc", 123);
        Map<String, Object> map2 = JsonUtil.unwrap(JsonUtil.wrap(map));
        assertEquals(map, map2);

        map2 = JsonUtil.unwrap(JsonUtil.wrap(map), Map.class);
        assertEquals(map, map2);

        Account account = new Account();
        account.setId(123);
        account.setFirstName("firstName");
        Account account2 = JsonUtil.unwrap(JsonUtil.wrap(account), Account.class);
        assertEquals(account, account2);

        {
            boolean[] array = Array.of(false, true, false);

            boolean[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), boolean[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            char[] array = Array.of('a', 'b', 'c');

            char[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), char[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            byte[] array = Array.of((byte) 1, (byte) 2, (byte) 3);

            byte[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), byte[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            short[] array = Array.of((short) 1, (short) 2, (short) 3);

            short[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), short[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            int[] array = Array.of(1, 2, 3);

            int[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), int[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            long[] array = Array.of(1L, 2L, 3L);

            long[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), long[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            float[] array = Array.of(1f, 2f, 3f);

            float[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), float[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            double[] array = Array.of(1d, 2d, 3d);

            double[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), double[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            String[] array = N.asArray("a", "b", "c");

            String[] array2 = JsonUtil.unwrap(JsonUtil.wrap(array), String[].class);

            assertTrue(N.equals(array, array2));
        }

        {
            List<String> list = N.asList("a", "b", "c");

            List<String> list2 = JsonUtil.unwrap(JsonUtil.wrap(list));

            assertTrue(N.equals(list, list2));
        }
    }

    @Test
    public void test_02() {
        Map<String, Object> map = N.asMap("abc", 123);

        Account account = new Account();
        account.setId(123);
        account.setFirstName("firstName");

        map.put("account", account);

        Map<String, Object> map2 = JsonUtil.unwrap(JsonUtil.wrap(map));

        N.println(map);
        N.println(map2);
    }

    @Test
    public void test_03() {
        Account account = new Account();
        account.setId(123);
        account.setFirstName("firstName");
        List<AccountDevice> devices = new ArrayList<>();
        AccountDevice device = new AccountDevice();
        device.setName(Strings.uuid());
        devices.add(device);
        account.setDevices(devices);

        JSONObject jsonObject = JsonUtil.wrap(account);
        N.println(jsonObject);

        Account account2 = JsonUtil.unwrap(jsonObject, Account.class);
        N.println(account2);

        assertEquals(account.getDevices().get(0), account2.getDevices().get(0));
    }
}
