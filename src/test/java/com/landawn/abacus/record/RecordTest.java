package com.landawn.abacus.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.N;

public class RecordTest {

    @Test
    public void test_01() {
        RecordA recordA = new RecordA(1, "RecordA");
        N.println(RecordA.class.getAnnotations());
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordA.class);
        N.println(beanInfo.propInfoList);
        String json = N.toJson(recordA);
        N.println(json);

        RecordA record2 = N.fromJson(json, RecordA.class);
        N.println(record2);

        assertEquals(recordA, record2);

        Dataset ds = N.newDataset(N.asList(recordA, record2));
        ds.println();

        ds.toList(RecordB.class).forEach(Fn.println());

        N.println(N.convert(record2, RecordB.class));

        assertEquals(recordA, Beans.map2Bean(Beans.bean2Map(record2), RecordA.class));
    }

    @Test
    public void test_02() {
        RecordB recordA = new RecordB(2, "RecordB");
        N.println(RecordB.class.getAnnotations());
        BeanInfo beanInfo = ParserUtil.getBeanInfo(RecordB.class);
        N.println(beanInfo.propInfoList);
        String json = N.toJson(recordA);
        N.println(json);

        RecordB record2 = N.fromJson(json, RecordB.class);
        N.println(record2);

        assertEquals(recordA, record2);

        Dataset ds = N.newDataset(N.asList(recordA, record2));
        ds.println();

        ds.toList(RecordA.class).forEach(Fn.println());

        N.println(N.convert(record2, RecordA.class));

        assertEquals(recordA, Beans.map2Bean(Beans.bean2Map(record2), RecordB.class));

    }

    @Test
    public void test_03() {
        Element e = new Element(100, "abc", N.asList(1L), N.asMap("a", BigInteger.ZERO), N.asMap("b", 2d), N.asList("Speaker"));
        Map<String, Object> map = Beans.bean2Map(e);

        N.println(map);

        Element e2 = Beans.map2Bean(map, Element.class);

        assertEquals(e, e2);

        assertEquals(e, N.fromJson(N.toJson(e), Element.class));

        String xml = N.toXml(e);
        N.println(xml);
        assertEquals(e, N.fromXml(xml, Element.class));
    }
}
