package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.landawn.abacus.util.*;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.landawn.abacus.parser.entity.EntityA;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

public class NewParserTest {

    @Data
    @Accessors(fluent = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Title {
        private String id;

    }

    @Test
    public void test_02() {
        Title tile = new Title("123");
        N.println(Beans.getPropNameList(Title.class));
        String json = N.toJson(tile);
        N.println(json);

        assertEquals(tile, N.fromJson(json, Title.class));

        String xml = N.toXml(tile);
        N.println(xml);
        assertEquals(tile, N.fromXml(xml, Title.class));
    }

    @Test
    public void test_01() {
        EntityA beanA = new EntityA("abc", 123, Color.GREEN, N.asMap(Dates.currentTimestamp(), 0.1f));
        String json = N.toJson(beanA);
        N.println(json);
        assertEquals(beanA, N.fromJson(json, EntityA.class));

        json = JSON.toJSONString(beanA);
        N.println(json);
    }
}
