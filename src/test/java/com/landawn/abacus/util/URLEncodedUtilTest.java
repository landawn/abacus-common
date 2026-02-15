package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;

@Tag("old-test")
public class URLEncodedUtilTest extends AbstractTest {

    @Test
    public void test_format() {
        Account account = createAccount(Account.class);
        AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String query = URLEncodedUtil.encode(account);
        N.println(query);

        Account account2 = URLEncodedUtil.decode(query, Account.class);
        N.println(CommonUtil.stringOf(account2));

        query = URLEncodedUtil.encode(Beans.beanToMap(account));
        N.println(query);

        account2 = URLEncodedUtil.decode(query, Account.class);
        N.println(CommonUtil.stringOf(account2));
    }

    @Test
    public void test_decodeToBean() {
        Account account = createAccount(Account.class);
        Map<String, Object> props = Beans.beanToMap(account);
        Map<String, String[]> parameters = new HashMap<>();

        for (String propName : props.keySet()) {
            parameters.put(propName, CommonUtil.asArray(CommonUtil.stringOf(props.get(propName))));
        }

        Account account2 = URLEncodedUtil.convertToBean(parameters, Account.class);

        assertEquals(account, account2);
    }
}
