package com.landawn.samples.abacus;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.landawn.abacus.Transaction.Status;
import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.JdbcUtil;
import com.landawn.abacus.util.SQLBuilder.NSC;
import com.landawn.abacus.util.SQLBuilder.SP;
import com.landawn.abacus.util.SQLTransaction;

public class TransactionTest extends Jdbc {

    @Test
    public void test_01() {
        SQLTransaction tranA = JdbcUtil.beginTransaction(dataSource);

        try {
            // SQLExecutor.beginTransaction shares the transaction started by static method JdbcUtil.beginTransaction
            SQLTransaction tranB = sqlExecutor.beginTransaction();

            assertTrue(tranA.connection() == tranB.connection());
            assertTrue(tranA == tranB);

            try {
                SP sp = NSC.selectFrom(User.class).where(CF.equal("id", 1)).pair();
                sqlExecutor.query(sp.sql, sp.parameters).println();
                tranB.commit();
            } finally {
                tranB.rollbackIfNotCommitted();
            }

            assertTrue(tranB.isActive());
            assertTrue(tranA.isActive());
        } finally {
            tranA.rollbackIfNotCommitted();
        }
    }

    @Test
    public void test_02() {
        SQLTransaction tranA = sqlExecutor.beginTransaction();

        try {
            // JdbcUtil.beginTransaction doesn't share the transaction started by static method SQLExecutor.beginTransaction
            SQLTransaction tranB = JdbcUtil.beginTransaction(dataSource);

            assertTrue(tranA.connection() != tranB.connection());
            assertTrue(tranA != tranB);

            try {
                SP sp = NSC.selectFrom(User.class).where(CF.equal("id", 1)).pair();
                sqlExecutor.query(sp.sql, sp.parameters).println();
                tranB.commit();
            } finally {
                tranB.rollbackIfNotCommitted();
            }

            assertTrue(tranB.status() == Status.COMMITTED);
            assertTrue(tranA.isActive());
        } finally {
            tranA.rollbackIfNotCommitted();
        }
    }

}
