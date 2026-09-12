package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Profiler;

import testfixtures.entity.BigXBean;
import testfixtures.entity.XBean;

public class JsonBindingTest extends AbstractParserTest {

    @Test
    public void testPerformance() {
        assertDoesNotThrow(() -> {
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
                Method method = ClassUtil.getDeclaredMethod(JsonBindingTest.class, methodName);
                ClassUtil.setAccessible(method, true);
                Profiler.run(threadNum, methodLoopNumMap.get(methodName), 3, methodName, () -> {
                    ClassUtil.invokeMethod(this, method);
                }).printResult();
            }
        });
    }

    public void executeByFastJSON2WithSimpleBean() {
        String str = com.alibaba.fastjson2.JSON.toJSONString(simpleBean);

        JSON.parseObject(str, XBean.class);

    }

    public void executeByFastJSON2WithBigBean() {
        String str = com.alibaba.fastjson2.JSON.toJSONString(bigBean);

        JSON.parseObject(str, BigXBean.class);

    }

    public void executeByJacksonWithSimpleBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();
        String str = objectMapper.writeValueAsString(simpleBean);

        objectMapper.readValue(str, XBean.class);

        recycle(objectMapper);
    }

    public void executeByJacksonWithBigBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();
        String str = objectMapper.writeValueAsString(bigBean);

        objectMapper.readValue(str, BigXBean.class);

        recycle(objectMapper);

    }

    public void executeByGSONWithSimpleBean() {
        Gson gson = getGson();
        String str = gson.toJson(simpleBean);
        gson.fromJson(str, XBean.class);
        recycle(gson);

    }

    public void executeByGSONWithBigBean() {
        Gson gson = getGson();
        String st = gson.toJson(bigBean);
        gson.fromJson(st, BigXBean.class);
        recycle(gson);

    }

    public void executeByKRYOWithSimpleBean() {
        kryoParser.deserialize(kryoParser.serialize(simpleBean), XBean.class);

    }

    public void executeByKRYOWithBigBean() {
        String st = kryoParser.serialize(bigBean);
        kryoParser.deserialize(st, BigXBean.class);

    }

    public void executeByAbacusJSONWithSimpleBean() {
        String str = jsonParser.serialize(simpleBean, jsc);

        jsonParser.deserialize(str, XBean.class);

    }

    public void executeByAbacusJSONWithSimpleBeanMap() {
        String json = jsonParser.serialize(simpleBean, jsc);
        jsonParser.deserialize(json, Map.class);
    }

    public void executeByAbacusJSONWithBigBean() {
        String str = jsonParser.serialize(bigBean, jsc);

        jsonParser.deserialize(str, BigXBean.class);

    }

    public void executeByAbacusJSONWithBigBeanMap() {
        String json = jsonParser.serialize(bigBean, jsc);

        jsonParser.deserialize(json, Map.class);

    }

    public void executeByJsonReaderWithSimpleBean() throws IOException {
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

    public void executeByJsonReaderWithBigBean() throws IOException {
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

    public void executeByXMLWithSimpleBean() {
        xmlParser.deserialize(xmlParser.serialize(simpleBean), XBean.class);

    }

    public void executeByXMLWithBigBean() {
        String st = xmlParser.serialize(bigBean);
        xmlParser.deserialize(st, BigXBean.class);

    }

    public void executeByAbacusXMLWithSimpleBean() {
        abacusXmlParser.deserialize(abacusXmlParser.serialize(simpleBean), XBean.class);

    }

    public void executeByAbacusXMLWithBigBean() {
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
        assertNotNull(end);
    }

    @Test
    public void test_withBigBean() {
        assertDoesNotThrow(() -> {
            N.println(N.toString(bigBean.getXBeanList()));

            N.println(xmlParser.serialize(bigBean));

            N.println(N.toString(xmlParser.deserialize(xmlParser.serialize(bigBean), BigXBean.class).getXBeanList()));
        });
    }

    @Test
    public void test_executeByJacksonWithSimpleBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();

        String str = objectMapper.writeValueAsString(simpleBean);

        InputStream is = IOUtil.stringToInputStream(str);
        objectMapper.readValue(is, XBean.class);

        recycle(objectMapper);
        assertNotNull(is);
    }

    @Test
    public void test_executeByJacksonWithBigBean() throws Exception {
        ObjectMapper objectMapper = getObjectMapper();

        String str = objectMapper.writeValueAsString(bigBean);

        InputStream is = IOUtil.stringToInputStream(str);
        objectMapper.readValue(is, BigXBean.class);

        recycle(objectMapper);
        assertNotNull(is);
    }

    @Test
    public void test_executeByAbacusJSONWithSimpleBean() {
        String str = jsonParser.serialize(simpleBean, jsc);

        InputStream is = IOUtil.stringToInputStream(str);

        N.println(jsonParser.deserialize(is, XBean.class));
        assertNotNull(is);
    }

    @Test
    public void test_executeByAbacusJSONWithBigBean() {
        String str = jsonParser.serialize(bigBean, jsc);

        InputStream is = IOUtil.stringToInputStream(str);
        BigXBean bigXBean = jsonParser.deserialize(is, BigXBean.class);
        N.println(Beans.beanToMap(bigXBean));
        assertNotNull(bigXBean);
    }
}
