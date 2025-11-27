package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.gson.reflect.TypeToken;
import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.AbstractType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("old-test")
public class TypeTest extends AbstractTest {

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nums {
        @com.landawn.abacus.annotation.Type(clazz = MyBigIntegerType.class)
        private BigInteger a;
        private BigDecimal b;
        private int c;
        private BigInteger d;
    }

    public static class MyBigIntegerType extends AbstractType<BigInteger> {

        protected MyBigIntegerType() {
            super("MyBigInteger");
        }

        @Override
        public Class<BigInteger> clazz() {
            return BigInteger.class;
        }

        @Override
        public String stringOf(BigInteger x) {
            return String.valueOf(666);
        }

        @Override
        public BigInteger valueOf(String str) {
            return BigInteger.valueOf(Long.parseLong("666"));
        }
    }

    @Test
    public void test_nums() {
        Nums nums = Nums.builder().a(BigInteger.valueOf(111)).b(BigDecimal.valueOf(222)).c(333).d(BigInteger.valueOf(444)).build();
        String json = N.toJson(nums);
        N.println(json);

        assertEquals(Nums.builder().a(BigInteger.valueOf(666)).b(BigDecimal.valueOf(222)).c(333).d(BigInteger.valueOf(444)).build(),
                N.fromJson(json, Nums.class));
    }

    @Test
    public void test_01() {
        java.lang.reflect.Type type = TypeToken.getParameterized(HashMap.class, Integer.class, String.class).getType();

        Type<Map<Integer, String>> type22 = Type.of(type);

        N.println(type22.declaringName());

        N.println(Type.of((java.lang.reflect.Type) Map.class).declaringName());

        N.println(Type.of((java.lang.reflect.Type) TypeTest.class).declaringName());

    }

    @Test
    public void test_MapEntry() {
        AbstractMap.SimpleEntry<String, Integer> entry = new AbstractMap.SimpleEntry<>("abc", 123);
        Type<Map.Entry<String, Integer>> type = CommonUtil.typeOf("Map.Entry<String, Integer>");

        String str = type.stringOf(entry);
        N.println(str);

        Map.Entry<String, Integer> entry2 = type.valueOf(str);
        N.println(entry2);
    }

    @Test
    public void testUnit() {
        Type type = TypeFactory.getType(char.class.getCanonicalName());
        N.println(type.valueOf(String.valueOf((char) 0)));
        N.println("ok");
    }

    @Test
    public void testTimestampType() {
        Type type = TypeFactory.getType(Timestamp.class.getCanonicalName());
        Timestamp t = Dates.currentTimestamp();
        String st = type.stringOf(t);
        N.println(st);
        t.setTime(t.getTime());
        assertEquals(t, type.valueOf(st));
    }

    @Test
    public void testCalendarType() {
        Type type = TypeFactory.getType(Calendar.class.getCanonicalName());
        Calendar c = Dates.currentCalendar();
        String st = type.stringOf(c);
        N.println(st);
    }
}
