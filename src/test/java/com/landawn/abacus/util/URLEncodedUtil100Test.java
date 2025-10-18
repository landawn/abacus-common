package com.landawn.abacus.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class URLEncodedUtil100Test extends TestBase {

    @Test
    public void testDecodeWithDefaultCharset() {
        Map<String, String> result = URLEncodedUtil.decode("");
        Assertions.assertTrue(result.isEmpty());

        result = URLEncodedUtil.decode(null);
        Assertions.assertTrue(result.isEmpty());

        result = URLEncodedUtil.decode("key=value");
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("value", result.get("key"));

        result = URLEncodedUtil.decode("key1=value1&key2=value2");
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("value1", result.get("key1"));
        Assertions.assertEquals("value2", result.get("key2"));

        result = URLEncodedUtil.decode("key1=value1;key2=value2");
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("value1", result.get("key1"));
        Assertions.assertEquals("value2", result.get("key2"));

        result = URLEncodedUtil.decode("key1&key2=value2");
        Assertions.assertEquals(2, result.size());
        Assertions.assertNull(result.get("key1"));
        Assertions.assertEquals("value2", result.get("key2"));

        result = URLEncodedUtil.decode("key=hello+world");
        Assertions.assertEquals("hello world", result.get("key"));

        result = URLEncodedUtil.decode("key=hello%20world");
        Assertions.assertEquals("hello world", result.get("key"));

        result = URLEncodedUtil.decode("key=%21%40%23%24%25");
        Assertions.assertEquals("!@#$%", result.get("key"));
    }

    @Test
    public void testDecodeWithSpecificCharset() {
        Charset utf8 = StandardCharsets.UTF_8;

        Map<String, String> result = URLEncodedUtil.decode("key=value", utf8);
        Assertions.assertEquals("value", result.get("key"));

        result = URLEncodedUtil.decode("name=%E4%B8%AD%E6%96%87", utf8);
        Assertions.assertEquals("中文", result.get("name"));

        Charset iso = StandardCharsets.ISO_8859_1;
        result = URLEncodedUtil.decode("key=caf%E9", iso);
        Assertions.assertEquals("café", result.get("key"));
    }

    @Test
    public void testDecodeWithMapSupplier() {
        Supplier<HashMap<String, String>> hashMapSupplier = HashMap::new;

        HashMap<String, String> result = URLEncodedUtil.decode("key1=value1&key2=value2", StandardCharsets.UTF_8, hashMapSupplier);

        Assertions.assertTrue(result instanceof HashMap);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("value1", result.get("key1"));
        Assertions.assertEquals("value2", result.get("key2"));

        Supplier<LinkedHashMap<String, String>> linkedMapSupplier = LinkedHashMap::new;
        LinkedHashMap<String, String> linkedResult = URLEncodedUtil.decode("a=1&b=2&c=3", StandardCharsets.UTF_8, linkedMapSupplier);

        Assertions.assertTrue(linkedResult instanceof LinkedHashMap);
        Assertions.assertEquals(3, linkedResult.size());
    }

    @Test
    public void testDecodeToMultimapWithDefaultCharset() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("");
        Assertions.assertTrue(result.isEmpty());

        result = URLEncodedUtil.decodeToMultimap(null);
        Assertions.assertTrue(result.isEmpty());

        result = URLEncodedUtil.decodeToMultimap("key=value");
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("value", result.get("key").get(0));

        result = URLEncodedUtil.decodeToMultimap("key=value1&key=value2&key=value3");
        Assertions.assertEquals(3, result.get("key").size());
        Assertions.assertEquals("value1", result.get("key").get(0));
        Assertions.assertEquals("value2", result.get("key").get(1));
        Assertions.assertEquals("value3", result.get("key").get(2));

        result = URLEncodedUtil.decodeToMultimap("a=1&b=2&a=3");
        Assertions.assertEquals(2, result.get("a").size());
        Assertions.assertEquals(1, result.get("b").size());
    }

    @Test
    public void testDecodeToMultimapWithSpecificCharset() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("name=%E4%B8%AD%E6%96%87&name=test", StandardCharsets.UTF_8);

        Assertions.assertEquals(2, result.get("name").size());
        Assertions.assertEquals("中文", result.get("name").get(0));
        Assertions.assertEquals("test", result.get("name").get(1));
    }

    @Test
    public void testDecodeToBeanWithDefaultCharset() {
        TestBean bean = URLEncodedUtil.decode("name=John&age=30&active=true", TestBean.class);

        Assertions.assertEquals("John", bean.getName());
        Assertions.assertEquals(30, bean.getAge());
        Assertions.assertTrue(bean.isActive());

        bean = URLEncodedUtil.decode("", TestBean.class);
        Assertions.assertNotNull(bean);

        bean = URLEncodedUtil.decode("name=Jane&age=", TestBean.class);
        Assertions.assertEquals("Jane", bean.getName());
        Assertions.assertEquals(0, bean.getAge());
    }

    @Test
    public void testDecodeToBeanWithSpecificCharset() {
        TestBean bean = URLEncodedUtil.decode("name=%E4%B8%AD%E6%96%87&age=25", StandardCharsets.UTF_8, TestBean.class);

        Assertions.assertEquals("中文", bean.getName());
        Assertions.assertEquals(25, bean.getAge());
    }

    @Test
    public void testDecodeToMap() {
        Map<String, String> map = URLEncodedUtil.decode("key1=value1&key2=value2", StandardCharsets.UTF_8, Map.class);

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("value1", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testParameters2Bean() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("name", new String[] { "John" });
        parameters.put("age", new String[] { "30" });
        parameters.put("active", new String[] { "true" });

        TestBean bean = URLEncodedUtil.parameters2Bean(parameters, TestBean.class);

        Assertions.assertEquals("John", bean.getName());
        Assertions.assertEquals(30, bean.getAge());
        Assertions.assertTrue(bean.isActive());

        parameters.put("tags", new String[] { "reading", "swimming", "coding" });
        bean = URLEncodedUtil.parameters2Bean(parameters, TestBean.class);
        Assertions.assertNotNull(bean);

        bean = URLEncodedUtil.parameters2Bean(new HashMap<>(), TestBean.class);
        Assertions.assertNotNull(bean);

        bean = URLEncodedUtil.parameters2Bean(null, TestBean.class);
        Assertions.assertNotNull(bean);

        parameters.clear();
        parameters.put("name", new String[] { "" });
        bean = URLEncodedUtil.parameters2Bean(parameters, TestBean.class);
        Assertions.assertNull(bean.getName());

        parameters.clear();
        parameters.put("tags", new String[] { "tag1", "tag2", "tag3" });
        bean = URLEncodedUtil.parameters2Bean(parameters, TestBean.class);
        Assertions.assertArrayEquals(new String[] { "tag1", "tag2", "tag3" }, bean.getTags());
    }

    @Test
    public void testEncodeWithDefaultCharset() {
        String result = URLEncodedUtil.encode(null);
        Assertions.assertEquals("", result);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("key1", "value1");
        params.put("key2", "value2");
        result = URLEncodedUtil.encode(params);
        Assertions.assertEquals("key1=value1&key2=value2", result);

        params.clear();
        params.put("name", "hello world");
        result = URLEncodedUtil.encode(params);
        Assertions.assertEquals("name=hello+world", result);

        params.clear();
        params.put("special", "!@#$%");
        result = URLEncodedUtil.encode(params);
        Assertions.assertEquals("special=%21%40%23%24%25", result);

        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);
        result = URLEncodedUtil.encode(bean);
        Assertions.assertTrue(result.contains("name=John"));
        Assertions.assertTrue(result.contains("age=30"));
        Assertions.assertTrue(result.contains("active=true"));

        Object[] arrayParams = new Object[] { "key1", "value1", "key2", "value2" };
        result = URLEncodedUtil.encode(arrayParams);
        Assertions.assertEquals("key1=value1&key2=value2", result);

        result = URLEncodedUtil.encode("key1=value1&key2=value2");
        Assertions.assertEquals("key1=value1&key2=value2", result);

        result = URLEncodedUtil.encode("simpletext");
        Assertions.assertEquals("simpletext", result);

        result = URLEncodedUtil.encode(12345);
        Assertions.assertEquals("12345", result);
    }

    @Test
    public void testEncodeWithSpecificCharset() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "中文");

        String result = URLEncodedUtil.encode(params, StandardCharsets.UTF_8);
        Assertions.assertEquals("name=%E4%B8%AD%E6%96%87", result);

        params.clear();
        params.put("name", "café");
        result = URLEncodedUtil.encode(params, StandardCharsets.ISO_8859_1);
        Assertions.assertTrue(result.startsWith("name="));
    }

    @Test
    public void testEncodeWithNamingPolicy() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        String result = URLEncodedUtil.encode(bean, StandardCharsets.UTF_8, NamingPolicy.LOWER_CAMEL_CASE);
        Assertions.assertTrue(result.contains("name=John"));

        result = URLEncodedUtil.encode(bean, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        Assertions.assertTrue(result.contains("name=John"));
    }

    @Test
    public void testEncodeWithUrl() {
        String result = URLEncodedUtil.encode("http://example.com", (Object) null);
        Assertions.assertEquals("http://example.com", result);

        result = URLEncodedUtil.encode("http://example.com", new HashMap<>());
        Assertions.assertEquals("http://example.com", result);

        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");
        result = URLEncodedUtil.encode("http://example.com", params);
        Assertions.assertEquals("http://example.com?key=value", result);

        params.put("key2", "value2");
        result = URLEncodedUtil.encode("http://example.com", params);
        Assertions.assertTrue(result.startsWith("http://example.com?"));
        Assertions.assertTrue(result.contains("key=value"));
        Assertions.assertTrue(result.contains("key2=value2"));
    }

    @Test
    public void testEncodeWithUrlAndCharset() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "中文");

        String result = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8);
        Assertions.assertEquals("http://example.com?name=%E4%B8%AD%E6%96%87", result);
    }

    @Test
    public void testEncodeWithUrlCharsetAndNamingPolicy() {
        TestBean bean = new TestBean();
        bean.setName("John");

        String result = URLEncodedUtil.encode("http://example.com", bean, StandardCharsets.UTF_8, NamingPolicy.LOWER_CAMEL_CASE);
        Assertions.assertTrue(result.startsWith("http://example.com?"));
        Assertions.assertTrue(result.contains("name=John"));
    }

    @Test
    public void testEncodeToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");

        URLEncodedUtil.encode(params, sb);
        Assertions.assertEquals("key=value", sb.toString());

        sb = new StringBuilder();
        URLEncodedUtil.encode((Object) null, sb);
        Assertions.assertEquals("", sb.toString());
    }

    @Test
    public void testEncodeToAppendableWithCharset() throws IOException {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("name", "中文");

        URLEncodedUtil.encode(params, StandardCharsets.UTF_8, sb);
        Assertions.assertEquals("name=%E4%B8%AD%E6%96%87", sb.toString());
    }

    @Test
    public void testEncodeToAppendableWithCharsetAndNamingPolicy() throws IOException {
        StringBuilder sb = new StringBuilder();
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        URLEncodedUtil.encode(bean, StandardCharsets.UTF_8, NamingPolicy.LOWER_CAMEL_CASE, sb);
        String result = sb.toString();
        Assertions.assertTrue(result.contains("name=John"));
        Assertions.assertTrue(result.contains("age=30"));

        sb = new StringBuilder();
        URLEncodedUtil.encode(bean, StandardCharsets.UTF_8, null, sb);
        result = sb.toString();
        Assertions.assertTrue(result.contains("name=John"));

        StringBuilder sb2 = new StringBuilder();
        Object[] oddArray = new Object[] { "key1", "value1", "key2" };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            URLEncodedUtil.encode(oddArray, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE, sb2);
        });
    }

    @Test
    public void testSpecialCases() {
        Map<String, Object> params = new HashMap<>();
        params.put("key1", null);
        params.put("key2", "value2");
        String result = URLEncodedUtil.encode(params);
        Assertions.assertTrue(result.contains("key1=null"));
        Assertions.assertTrue(result.contains("key2=value2"));

        Map<String, String> decoded = URLEncodedUtil.decode("key=%2");
        Assertions.assertEquals("%2", decoded.get("key"));

        decoded = URLEncodedUtil.decode("key=%ZZ");
        Assertions.assertEquals("%ZZ", decoded.get("key"));

        decoded = URLEncodedUtil.decode("a=1&b=2;c=3&d=4");
        Assertions.assertEquals(4, decoded.size());

        decoded = URLEncodedUtil.decode("  key  =  value  ");
        Assertions.assertEquals("value", decoded.get("key"));
    }

    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private String[] tags;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String[] getTags() {
            return tags;
        }

        public void setTags(String[] tags) {
            this.tags = tags;
        }
    }
}
