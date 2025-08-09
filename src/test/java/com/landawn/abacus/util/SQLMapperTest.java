/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class SQLMapperTest extends AbstractTest {

    @Test
    public void test() throws IOException {
        //        Timestamp now = new Timestamp(System.currentTimeMillis());
        //        long id = (Long) sqlExecutor.insert(MySqlDef.insertAccount, "fn", "ln", UUID.randomUUID().toString(), now, now);
        //        Account account = sqlExecutor.get(Account.class, SELECT_ACCOUNT_BY_ID, id).get();
        //
        //        Map<String, String> attrs = new HashMap<>();
        //        attrs.put(SQLMapper.BATCH_SIZE, "200");
        //        attrs.put(SQLMapper.RESULT_SET_TYPE, "SCROLL_INSENSITIVE");
        //
        //        SQLMapper sqlMapper = new SQLMapper();
        //        sqlMapper.add("findByName", "select * from Account where firstName='" + account.getFirstName() + "'", attrs);
        //        attrs.clear();
        //        sqlMapper.add("findById", "select * from Account where id=" + account.getId(), attrs);
        //
        //        File file = new File("sqlMapper.xml");
        //
        //        if (file.exists()) {
        //            file.delete();
        //        }
        //
        //        sqlMapper.saveTo(file);
        //
        //        SQLMapper sqlMapper2 = SQLMapper.fromFile(file.getPath());
        //        assertEquals(sqlMapper, sqlMapper2);
        //
        //        file.delete();
    }
}
