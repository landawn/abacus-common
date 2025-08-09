/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;

public class XMLTest extends AbstractParserTest {

    @Test
    public void test_serialize() {
        Account account = createAccount(Account.class);
        AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        StringWriter stWriter = new StringWriter();

        abacusXMLParser.serialize(account, stWriter);

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = stackTrace.length - 1; i >= 0; i--) {
            N.println(stackTrace[i].getClassName());
        }

        N.println(stWriter.toString());
    }

    @Test
    public void testSerialize() {
        Account account = createAccount(Account.class);
        account.setId(100);

        String str = abacusXMLParser.serialize(account);
        println(str);

        println(abacusXMLParser.deserialize(str, Account.class));

        // test unknown property:
        str = "<account><id>100</id><gui>737a45812bad4d5bafbb62428cefe66c</gui><unknownProperty1><list><e>11</e></list></unknownProperty1><emailAddress>fc3283c92282499ea2ba515e840e305a@earth.com</emailAddress><firstName>firstName</firstName><middleName>MN</middleName><lastName>lastName</lastName><birthDate>1399944204713</birthDate><lastUpdateTime>1399944204713</lastUpdateTime><createdTime>1399944204713</createdTime><unknownProperty2>null</unknownProperty2></account>";
        println(abacusXMLParser.deserialize(str, Account.class));

        //        assertEquals(N.stringOf(account),
        //            N.stringOf(FasterJSON.deserialize(Account.class, FasterJSON.serialize(account))));
    }
}
