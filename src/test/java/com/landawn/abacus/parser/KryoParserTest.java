package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.parser.KryoSerConfig;
import com.landawn.abacus.util.N;

public class KryoParserTest extends AbstractParserTest {
    static {
        kryoParser.register(Account.class, 101);
    }

    @Override
    protected Parser<?, ?> getParser() {
        return kryoParser;
    }

    @Test
    public void test_config() {
        KryoSerConfig ksc1 = KryoSerConfig.create();
        KryoSerConfig ksc2 = KryoSerConfig.create();

        N.println(ksc1);

        assertTrue(N.toSet(ksc1).contains(ksc2));
    }

    @Test
    public void test_encode() {
        Account account = createAccount(Account.class);
        byte[] bytes = kryoParser.encode(account);

        Account account2 = kryoParser.decode(bytes);
        assertEquals(account, account2);
    }

    @Test
    public void test_shallowCopy_and_deepCopy() {
        Account account = createAccountWithContact(Account.class);
        Account account2 = kryoParser.shallowCopy(account);
        assertTrue(account.getContact().equals(account2.getContact()));
        assertTrue(account.getContact() == account2.getContact());

        account2 = kryoParser.deepCopy(account);
        assertTrue(account.getContact().equals(account2.getContact()));
        assertFalse(account.getContact() == account2.getContact());
    }
}
