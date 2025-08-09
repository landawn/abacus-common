/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.util.N;

public abstract class AbstractJSONParserTest extends AbstractParserTest {

    @Test
    public void test_array() {
        Account[] accounts = N.asArray(createAccount(Account.class));

        String str = parser.serialize(accounts);

        N.println(str);

        Account[] accounts2 = parser.deserialize(str, Account[].class);

        assertTrue(N.equals(accounts, accounts2));

        String[][] twoArray = { { "abc", "efg" }, { "123", "345" } };

        str = parser.serialize(twoArray);

        String[][] twoArray2 = parser.deserialize(str, String[][].class);

        assertTrue(N.deepEquals(twoArray, twoArray2));

        twoArray = new String[][] { null, { "123", "345" } };

        str = parser.serialize(twoArray);

        twoArray2 = parser.deserialize(str, String[][].class);

        assertTrue(N.deepEquals(twoArray, twoArray2));

        if (parser instanceof JSONParserImpl) {
            str = "[, [\"123\", \"345\"]]";

            twoArray2 = parser.deserialize(str, String[][].class);

            assertTrue(N.deepEquals(new String[][] { null, { "123", "345" } }, twoArray2));
        }
    }
}
