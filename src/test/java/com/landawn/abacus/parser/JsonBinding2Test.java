package com.landawn.abacus.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
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

public class JsonBinding2Test extends AbstractParserTest {

    @Test
    @Tag("slow-test")
    public void testPerformance() {

        N.println("josn======================================================================");
        N.println(jsonParser.serialize(simpleBean, jsc));

        int threadNum = 6;
        int loopNumForSimpleBean = 300000;
        Map<String, Integer> methodLoopNumMap = new LinkedHashMap<>();
        methodLoopNumMap.put("executeByAbacusJSONWithSimpleBean", loopNumForSimpleBean);
        methodLoopNumMap.put("executeByJacksonWithSimpleBean", loopNumForSimpleBean);
        methodLoopNumMap.put("executeByFastJSON2WithSimpleBean", loopNumForSimpleBean);
        methodLoopNumMap.put("executeByAbacusJSONWithBigBean", loopNumForSimpleBean / 30);
        methodLoopNumMap.put("executeByJacksonWithBigBean", loopNumForSimpleBean / 30);
        methodLoopNumMap.put("executeByFastJSON2WithBigBean", loopNumForSimpleBean / 10);

        for (String methodName : methodLoopNumMap.keySet()) {
            Method method = ClassUtil.getDeclaredMethod(JsonBinding2Test.class, methodName);
            ClassUtil.setAccessible(method, true);
            Profiler.run(threadNum, methodLoopNumMap.get(methodName), 3, methodName, () -> {
                ClassUtil.invokeMethod(this, method);
            }).printResult();
        }
    }

    void executeByFastJSON2WithSimpleBean() {
        String str = com.alibaba.fastjson2.JSON.toJSONString(simpleBean);

        JSON.parseObject(str, XBean.class);

    }

    void executeByFastJSON2WithBigBean() {
        String str = com.alibaba.fastjson2.JSON.toJSONString(bigBean);

        JSON.parseObject(str, BigXBean.class);

    }

    void executeByJacksonWithSimpleBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();
        String str = objectMapper.writeValueAsString(simpleBean);

        objectMapper.readValue(str, XBean.class);

        recycle(objectMapper);
    }

    void executeByJacksonWithBigBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();
        String str = objectMapper.writeValueAsString(bigBean);

        objectMapper.readValue(str, BigXBean.class);

        recycle(objectMapper);

    }

    void executeByGSONWithSimpleBean() {
        Gson gson = getGson();
        String str = gson.toJson(simpleBean);
        gson.fromJson(str, XBean.class);
        recycle(gson);

    }

    void executeByGSONWithBigBean() {
        Gson gson = getGson();
        String st = gson.toJson(bigBean);
        gson.fromJson(st, BigXBean.class);
        recycle(gson);

    }

    void executeByKRYOWithSimpleBean() {
        kryoParser.deserialize(kryoParser.serialize(simpleBean), XBean.class);

    }

    void executeByKRYOWithBigBean() {
        String st = kryoParser.serialize(bigBean);
        kryoParser.deserialize(st, BigXBean.class);

    }

    void executeByAbacusJSONWithSimpleBean() {
        String str = jsonParser.serialize(simpleBean, jsc);

        jsonParser.deserialize(str, XBean.class);

    }

    void executeByAbacusJSONWithSimpleBeanMap() {
        String json = jsonParser.serialize(simpleBean, jsc);
        jsonParser.deserialize(json, Map.class);
    }

    void executeByAbacusJSONWithBigBean() {
        String str = jsonParser.serialize(bigBean, jsc);

        jsonParser.deserialize(str, BigXBean.class);

    }

    void executeByAbacusJSONWithBigBeanMap() {
        String json = jsonParser.serialize(bigBean, jsc);

        jsonParser.deserialize(json, Map.class);

    }

    void executeByJsonReaderWithSimpleBean() throws IOException {
        String json = jsonParser.serialize(simpleBean, jsc);

        final char[] cbuf = Objectory.createCharArrayBuffer();

        JsonReader jr = JsonStringReader.parse(json, cbuf);

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

    void executeByJsonReaderWithBigBean() throws IOException {
        String json = jsonParser.serialize(bigBean, jsc);

        final char[] cbuf = Objectory.createCharArrayBuffer();

        JsonReader jr = JsonStringReader.parse(json, cbuf);

        try {
            while (jr.nextToken() > -1) {
                if (jr.hasText()) {
                }
            }
        } finally {
            Objectory.recycle(cbuf);
        }
    }

    void executeByXMLWithSimpleBean() {
        xmlParser.deserialize(xmlParser.serialize(simpleBean), XBean.class);

    }

    void executeByXMLWithBigBean() {
        String st = xmlParser.serialize(bigBean);
        xmlParser.deserialize(st, BigXBean.class);

    }

    void executeByAbacusXMLWithSimpleBean() {
        abacusXmlParser.deserialize(abacusXmlParser.serialize(simpleBean), XBean.class);

    }

    void executeByAbacusXMLWithBigBean() {
        String st = abacusXmlParser.serialize(bigBean);
        abacusXmlParser.deserialize(st, BigXBean.class);

    }

    @Test
    public void test_withSimpleBean() {
        N.println(simpleBean);

        N.println(jsonParser.deserialize(jsonParser.serialize(simpleBean), XBean.class));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            jsonParser.deserialize(jsonParser.serialize(simpleBean), XBean.class);
        }
        long end = System.currentTimeMillis();
        N.println("Time taken: " + (end - start) + " ms");
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

        InputStream is = IOUtil.stringToInputStream(str);
        objectMapper.readValue(is, XBean.class);

        recycle(objectMapper);
    }

    @Test
    public void test_executeByJacksonWithBigBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();

        String str = objectMapper.writeValueAsString(bigBean);

        InputStream is = IOUtil.stringToInputStream(str);
        objectMapper.readValue(is, BigXBean.class);

        recycle(objectMapper);

    }

    @Test
    public void test_executeByAbacusJSONWithSimpleBean() {
        String str = jsonParser.serialize(simpleBean, jsc);

        InputStream is = IOUtil.stringToInputStream(str);

        N.println(jsonParser.deserialize(is, XBean.class));
    }

    @Test
    public void test_executeByAbacusJSONWithBigBean() {
        String str = jsonParser.serialize(bigBean, jsc);

        InputStream is = IOUtil.stringToInputStream(str);
        BigXBean bigXBean = jsonParser.deserialize(is, BigXBean.class);
        N.println(Beans.beanToMap(bigXBean));

    }
}
