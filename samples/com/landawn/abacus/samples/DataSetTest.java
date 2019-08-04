package com.landawn.abacus.samples;

import java.util.Random;

import org.junit.Test;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.samples.entity.Account;
import com.landawn.abacus.samples.entity.Contact;
import com.landawn.abacus.util.N;

public class DataSetTest {
    static final Random RAND = new Random();

    @Test
    public void test_join() {
        Account account = Account.builder().id(1001).firstName("Tom").build();
        Contact contact = Contact.builder().id(2001).accountId(1001).address("1 Rd").build();

        DataSet ds1 = N.newDataSet(N.asList("id", "firstName"), N.asList(account));
        ds1.println();

        DataSet ds2 = N.newDataSet(N.asList("id", "accountId", "address"), N.asList(contact));
        ds2.println();
        ds2.renameColumn("id", "contactId");

        DataSet ds3 = ds1.innerJoin(ds2, N.asMap("id", "accountId"));
        ds3.println();
    }

}
