/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.BigXBean;
import com.landawn.abacus.entity.XBean;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Profiler;

public class JSONBinding2Test extends AbstractParserTest {

    @Test
    @Tag("slow-test")
    public void testPerformance() {

        N.println("josn======================================================================");
        N.println(jsonParser.serialize(simpleBean, jsc));

        int threadNum = 6;
        int loopNumForSimpleBean = 300000;
        Map<String, Integer> methodLoopNumMap = new LinkedHashMap<>();
        //        methodLoopNumMap.put("executeByJSONReaderWithSimpleBean", loopNum);
        //        methodLoopNumMap.put("executeByAbacusJSONWithSimpleBeanMap", loopNum);
        // performance: 0.016 ~ 0.017
        methodLoopNumMap.put("executeByAbacusJSONWithSimpleBean", loopNumForSimpleBean);
        // performance: 0.012 ~ 0.013
        methodLoopNumMap.put("executeByJacksonWithSimpleBean", loopNumForSimpleBean);
        // performance: 0.01 ~ 0.011
        methodLoopNumMap.put("executeByFastJSONWithSimpleBean", loopNumForSimpleBean);
        //        methodLoopNumMap.put("executeByFastJSON2WithSimpleBean", loopNum);
        //        //
        // methodLoopNumMap.put("executeByGSONWithSimpleBean", loopNumForSimpleBean);
        //        //        methodLoopNumMap.put("executeByKRYOWithSimpleBean", loopNumForSimpleBean);
        //        //        methodLoopNumMap.put("executeByXMLWithSimpleBean", loopNumForSimpleBean);
        //        //        methodLoopNumMap.put("executeByAbacusXMLWithSimpleBean", loopNumForSimpleBean);
        //
        //        // TODO remove javaee*.jar before running test with Boon.
        //        //        methodLoopNumMap.put("executeByBoonWithSimpleBean", loopNumForSimpleBean);
        //        loopNum = 30000;
        //
        //        //
        //        //        methodLoopNumMap.put("executeByJSONReaderWithBigBean", loopNumForSimpleBean / 10);
        //        //        methodLoopNumMap.put("executeByAbacusJSONWithBigBeanMap", loopNumForSimpleBean / 10);
        // performance: 1.6 ~ 1.7
        methodLoopNumMap.put("executeByAbacusJSONWithBigBean", loopNumForSimpleBean / 30);
        // performance: 1.2 ~ 1.3
        methodLoopNumMap.put("executeByJacksonWithBigBean", loopNumForSimpleBean / 30);
        // performance: 1.0 ~ 1.1
        methodLoopNumMap.put("executeByFastJSONWithBigBean", loopNumForSimpleBean / 30);
        //        methodLoopNumMap.put("executeByFastJSON2WithBigBean", loopNumForSimpleBean / 10);

        //
        // methodLoopNumMap.put("executeByGSONWithBigBean", loopNumForSimpleBean / 10);
        //        methodLoopNumMap.put("executeByKRYOWithBigBean", loopNumForSimpleBean / 10);
        //        methodLoopNumMap.put("executeByXMLWithBigBean", loopNumForSimpleBean / 10);
        //        methodLoopNumMap.put("executeByAbacusXMLWithBigBean", loopNumForSimpleBean / 10);

        //         methodLoopNumMap.put("executeByBoonWithBigBean", loopNumForSimpleBean / 10);
        for (String methodName : methodLoopNumMap.keySet()) {
            Method method = ClassUtil.getDeclaredMethod(JSONBinding2Test.class, methodName);
            ClassUtil.setAccessible(method, true);
            Profiler.run(threadNum, methodLoopNumMap.get(methodName), 3, methodName, () -> {
                ClassUtil.invokeMethod(this, method);
            }).printResult();
        }
    }
    //
    //    void executeByBoonWithSimpleBean() throws Exception {
    //        Boon.fromJson(Boon.toJson(simpleBean), XBean.class);
    //    }
    //
    //    void executeByBoonWithBigBean() throws Exception {
    //        String st = Boon.toJson(bigBean);
    //        Boon.fromJson(st, BigXBean.class);
    //
    //        // N.println(st);
    //    }

    void executeByJacksonWithSimpleBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();
        String str = objectMapper.writeValueAsString(simpleBean);

        objectMapper.readValue(str, XBean.class);

        // InputStream is = IOUtil.string2InputStream(str);
        // objectMapper.readValue(is, XBean.class);

        recycle(objectMapper);
    }

    void executeByJacksonWithBigBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();
        String str = objectMapper.writeValueAsString(bigBean);

        objectMapper.readValue(str, BigXBean.class);

        // InputStream is = IOUtil.string2InputStream(str);
        // objectMapper.readValue(is, BigXBean.class);

        recycle(objectMapper);

        // N.println(st);
    }

    void executeByGSONWithSimpleBean() {
        Gson gson = getGson();
        String str = gson.toJson(simpleBean);
        gson.fromJson(str, XBean.class);
        recycle(gson);

        // N.println(N.stringOf(gson.fromJson(st, XBean.class)));
    }

    void executeByGSONWithBigBean() {
        Gson gson = getGson();
        String st = gson.toJson(bigBean);
        gson.fromJson(st, BigXBean.class);
        recycle(gson);

        // N.println(N.stringOf(gson.fromJson(st, BigXBean.class)));
    }

    void executeByKRYOWithSimpleBean() {
        kryoParser.deserialize(kryoParser.serialize(simpleBean), XBean.class);

        // N.println(N.stringOf(N.deserialize(XBean.class, N.serialize(simpleBean))));
    }

    void executeByKRYOWithBigBean() {
        String st = kryoParser.serialize(bigBean);
        kryoParser.deserialize(st, BigXBean.class);

        // N.println(st);

        // N.println(N.stringOf(N.deserialize(BigXBean.class, N.serialize(bigBean))));
    }

    void executeByAbacusJSONWithSimpleBean() {
        String str = jsonParser.serialize(simpleBean, jsc);

        jsonParser.deserialize(str, XBean.class);

        // InputStream is = IOUtil.string2InputStream(str);
        // jsonParser.deserialize(XBean.class, is);
    }

    void executeByAbacusJSONWithSimpleBeanMap() {
        String json = jsonParser.serialize(simpleBean, jsc);
        jsonParser.deserialize(json, Map.class);
    }

    void executeByAbacusJSONWithBigBean() {
        String str = jsonParser.serialize(bigBean, jsc);

        jsonParser.deserialize(str, BigXBean.class);

        //        InputStream is = IOUtil.string2InputStream(str);
        //        jsonParser.deserialize(BigXBean.class, is);

        //        BufferedReader br = IOUtil.createBufferedReader(new StringReader(json));
        //        jsonParser.deserialize(BigXBean.class, br);
        //        IOUtil.recycle(br);
    }

    void executeByAbacusJSONWithBigBeanMap() {
        String json = jsonParser.serialize(bigBean, jsc);

        jsonParser.deserialize(json, Map.class);

        //        BufferedReader br = IOUtil.createBufferedReader(new StringReader(json));
        //        jsonParser.deserialize(BigXBean.class, br);
        //        IOUtil.recycle(br);
    }

    void executeByJSONReaderWithSimpleBean() throws IOException {
        String json = jsonParser.serialize(simpleBean, jsc);

        final char[] cbuf = Objectory.createCharArrayBuffer();

        JSONReader jr = JSONStringReader.parse(json, cbuf);

        try {
            while (jr.nextToken() > -1) {
                if (jr.hasText()) {
                    jr.getText();
                }
            }
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    void executeByJSONReaderWithBigBean() throws IOException {
        String json = jsonParser.serialize(bigBean, jsc);

        final char[] cbuf = Objectory.createCharArrayBuffer();

        JSONReader jr = JSONStringReader.parse(json, cbuf);

        try {
            while (jr.nextToken() > -1) {
                if (jr.hasText()) {
                    //                    jr.getText();
                }
            }
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    void executeByXMLWithSimpleBean() {
        xmlParser.deserialize(xmlParser.serialize(simpleBean), XBean.class);

        // N.println(N.stringOf(N.deserialize(XBean.class, N.serialize(simpleBean))));
    }

    void executeByXMLWithBigBean() {
        String st = xmlParser.serialize(bigBean);
        xmlParser.deserialize(st, BigXBean.class);

        // N.println(st);

        // N.println(N.stringOf(N.deserialize(BigXBean.class, N.serialize(bigBean))));
    }

    void executeByAbacusXMLWithSimpleBean() {
        abacusXMLParser.deserialize(abacusXMLParser.serialize(simpleBean), XBean.class);

        // N.println(N.stringOf(N.deserialize(XBean.class, N.serialize(simpleBean))));
    }

    void executeByAbacusXMLWithBigBean() {
        String st = abacusXMLParser.serialize(bigBean);
        abacusXMLParser.deserialize(st, BigXBean.class);

        // N.println(st);

        // N.println(N.stringOf(N.deserialize(BigXBean.class, N.serialize(bigBean))));
    }

    @Test
    public void test_withSimpleBean() {
        N.println(simpleBean);

        N.println(xmlParser.deserialize(xmlParser.serialize(simpleBean), XBean.class));
    }

    @Test
    public void test_withBigBean() {
        N.println(N.toString(bigBean.getXBeanList()));

        N.println(xmlParser.serialize(bigBean));

        N.println(N.toString(xmlParser.deserialize(xmlParser.serialize(bigBean), BigXBean.class).getXBeanList()));

    }

    @Test
    public void test_executeByJacksonWithSimpleBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();

        String str = objectMapper.writeValueAsString(simpleBean);

        //        objectMapper.readValue(str, XBean.class);

        InputStream is = IOUtil.string2InputStream(str);
        objectMapper.readValue(is, XBean.class);

        recycle(objectMapper);
    }

    @Test
    public void test_executeByJacksonWithBigBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();

        String str = objectMapper.writeValueAsString(bigBean);

        //        objectMapper.readValue(str, BigXBean.class);

        InputStream is = IOUtil.string2InputStream(str);
        objectMapper.readValue(is, BigXBean.class);

        recycle(objectMapper);

        // N.println(st);
    }

    @Test
    public void test_executeByAbacusJSONWithSimpleBean() {
        String str = jsonParser.serialize(simpleBean, jsc);

        //        jsonParser.deserialize(XBean.class, str);

        InputStream is = IOUtil.string2InputStream(str);

        N.println(jsonParser.deserialize(is, XBean.class));
    }

    @Test
    public void test_executeByAbacusJSONWithBigBean() {
        String str = jsonParser.serialize(bigBean, jsc);

        //        jsonParser.deserialize(BigXBean.class, str);

        InputStream is = IOUtil.string2InputStream(str);
        BigXBean bigXBean = jsonParser.deserialize(is, BigXBean.class);
        N.println(Beans.bean2Map(bigXBean));

        //        BufferedReader br = IOUtil.createBufferedReader(new StringReader(json));
        //        jsonParser.deserialize(BigXBean.class, br);
        //        IOUtil.recycle(br);
    }
}
