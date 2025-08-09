package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
public class JacksonUtilTest extends AbstractTest {

    @Test
    public void test_pretty_format() {

        Account account = Beans.fill(Account.class);
        String json = JsonMappers.toJson(account);
        N.println(json);

        json = JsonMappers.toJson(account, true);
        N.println(json);

        Account account2 = JsonMappers.fromJson(json, Account.class);

        assertEquals(account, account2);
    }

    @Test
    public void test_pretty_format_xml() {

        Account account = Beans.fill(Account.class);
        String json = XmlMappers.toXml(account);
        N.println(json);

        json = XmlMappers.toXml(account, true);
        N.println(json);

        Account account2 = XmlMappers.fromXml(json, Account.class);

        assertEquals(account, account2);
    }

}
