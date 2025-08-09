package com.landawn.abacus.parser;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;

class JsonMappersTest {

    @Test
    public void test_01() {
        final List<XBean> bigXBean = Beans.fill(XBean.class, 10000);
        File outputFile = new File("./src/test/resources/test.json");
        N.toJson(bigXBean, outputFile);

        String json = N.toJson(N.asMap("key1", 123));
        Map map = N.fromJson(json, Map.class);
        N.println(map);

        map = N.fromJson(json, JDC.create().setMapValueType(Type.of("List<Integer>>")), Map.class);
        N.println(map);

        IOUtil.deleteIfExists(outputFile);
    }

}
