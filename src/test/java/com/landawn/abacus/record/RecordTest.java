package com.landawn.abacus.record;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.N;

public class RecordTest extends TestBase {

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

        Dataset ds = N.newDataset(N.toList(recordA, record2));
        ds.println();

        ds.toList(RecordB.class).forEach(Fn.println());

        final RecordB converted = N.convert(record2, RecordB.class);
        N.println(converted);
        assertEquals(new RecordB(1, "RecordA"), converted);

        assertEquals(recordA, Beans.mapToBean(Beans.beanToMap(record2), RecordA.class));
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

        Dataset ds = N.newDataset(N.toList(recordA, record2));
        ds.println();

        ds.toList(RecordA.class).forEach(Fn.println());

        final RecordA converted = N.convert(record2, RecordA.class);
        N.println(converted);
        assertEquals(new RecordA(2, "RecordB"), converted);

        assertEquals(recordA, Beans.mapToBean(Beans.beanToMap(record2), RecordB.class));

    }

    @Test
    public void test_03() {
        Element e = new Element(100, "abc", N.toList(1L), N.asMap("a", BigInteger.ZERO), N.asMap("b", 2d), N.toList("Speaker"));
        Map<String, Object> map = Beans.beanToMap(e);

        N.println(map);

        Element e2 = Beans.mapToBean(map, Element.class);

        assertEquals(e, e2);

        assertEquals(e, N.fromJson(N.toJson(e), Element.class));

        String xml = N.toXml(e);
        N.println(xml);
        assertEquals(e, N.fromXml(xml, Element.class));
    }
}
