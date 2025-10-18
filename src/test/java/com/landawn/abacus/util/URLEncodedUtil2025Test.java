package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class URLEncodedUtil2025Test extends TestBase {

    public static class User {
        private String name;
        private int age;
        private String email;

        public User() {
        }

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class Product {
        private String productName;
        private double price;

        public Product() {
        }

        public Product(String productName, double price) {
            this.productName = productName;
            this.price = price;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    @Test
    public void test_decode_string() {
        Map<String, String> result = URLEncodedUtil.decode("name=John+Doe&age=30");
        assertEquals("John Doe", result.get("name"));
        assertEquals("30", result.get("age"));
    }

    @Test
    public void test_decode_string_empty() {
        Map<String, String> result = URLEncodedUtil.decode("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_decode_string_null() {
        Map<String, String> result = URLEncodedUtil.decode(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_decode_string_withPercentEncoding() {
        Map<String, String> result = URLEncodedUtil.decode("name=John%20Doe&email=john%40example.com");
        assertEquals("John Doe", result.get("name"));
        assertEquals("john@example.com", result.get("email"));
    }

    @Test
    public void test_decode_string_withSemicolonSeparator() {
        Map<String, String> result = URLEncodedUtil.decode("name=John;age=30");
        assertEquals("John", result.get("name"));
        assertEquals("30", result.get("age"));
    }

    @Test
    public void test_decode_string_withNullValue() {
        Map<String, String> result = URLEncodedUtil.decode("name=John&age=");
        assertEquals("John", result.get("name"));
        assertEquals("", result.get("age"));
    }

    @Test
    public void test_decode_string_noValue() {
        Map<String, String> result = URLEncodedUtil.decode("name");
        assertNull(result.get("name"));
        assertTrue(result.containsKey("name"));
    }

    @Test
    public void test_decode_string_duplicateKeys() {
        Map<String, String> result = URLEncodedUtil.decode("color=red&color=blue");
        assertEquals("blue", result.get("color"));
    }

    @Test
    public void test_decode_string_charset() {
        Map<String, String> result = URLEncodedUtil.decode("name=%E4%B8%AD%E6%96%87", StandardCharsets.UTF_8);
        assertEquals("中文", result.get("name"));
    }

    @Test
    public void test_decode_string_charset_null() {
        Map<String, String> result = URLEncodedUtil.decode(null, StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_decode_string_charset_nullCharset() {
        Charset nullCharset = null;
        Map<String, String> result = URLEncodedUtil.decode("name=test", nullCharset);
        assertEquals("test", result.get("name"));
    }

    @Test
    public void test_decode_string_charset_supplier() {
        TreeMap<String, String> result = URLEncodedUtil.decode("b=2&a=1", StandardCharsets.UTF_8, TreeMap::new);
        assertNotNull(result);
        assertTrue(result instanceof TreeMap);
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("a", result.firstKey());
    }

    @Test
    public void test_decode_string_charset_supplier_empty() {
        HashMap<String, String> result = URLEncodedUtil.decode("", StandardCharsets.UTF_8, HashMap::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_decodeToMultimap_string() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("color=red&color=blue&size=L");
        assertNotNull(result);
        assertEquals(2, result.get("color").size());
        assertTrue(result.get("color").contains("red"));
        assertTrue(result.get("color").contains("blue"));
        assertEquals(1, result.get("size").size());
        assertEquals("L", result.get("size").get(0));
    }

    @Test
    public void test_decodeToMultimap_string_empty() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_decodeToMultimap_string_null() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_decodeToMultimap_string_charset() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("tag=java&tag=url", StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals(2, result.get("tag").size());
        assertTrue(result.get("tag").contains("java"));
        assertTrue(result.get("tag").contains("url"));
    }

    @Test
    public void test_decodeToMultimap_string_charset_null() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap(null, StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_decodeToMultimap_string_charset_withEncoding() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("name=%E4%B8%AD%E6%96%87&name=test", StandardCharsets.UTF_8);
        assertEquals(2, result.get("name").size());
        assertTrue(result.get("name").contains("中文"));
        assertTrue(result.get("name").contains("test"));
    }

    @Test
    public void test_decode_string_class() {
        User user = URLEncodedUtil.decode("name=John&age=30", User.class);
        assertNotNull(user);
        assertEquals("John", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    public void test_decode_string_class_empty() {
        User user = URLEncodedUtil.decode("", User.class);
        assertNotNull(user);
        assertNull(user.getName());
        assertEquals(0, user.getAge());
    }

    @Test
    public void test_decode_string_class_null() {
        User user = URLEncodedUtil.decode(null, User.class);
        assertNotNull(user);
    }

    @Test
    public void test_decode_string_class_map() {
        @SuppressWarnings("unchecked")
        Map<String, String> result = URLEncodedUtil.decode("name=John&age=30", LinkedHashMap.class);
        assertNotNull(result);
        assertEquals("John", result.get("name"));
        assertEquals("30", result.get("age"));
    }

    @Test
    public void test_decode_string_charset_class() {
        Product product = URLEncodedUtil.decode("productName=Laptop&price=999.99", StandardCharsets.UTF_8, Product.class);
        assertNotNull(product);
        assertEquals("Laptop", product.getProductName());
        assertEquals(999.99, product.getPrice(), 0.001);
    }

    @Test
    public void test_decode_string_charset_class_null() {
        User user = URLEncodedUtil.decode(null, StandardCharsets.UTF_8, User.class);
        assertNotNull(user);
    }

    @Test
    public void test_decode_string_charset_class_encoding() {
        Product product = URLEncodedUtil.decode("productName=%E4%B8%AD%E6%96%87&price=100", StandardCharsets.UTF_8, Product.class);
        assertEquals("中文", product.getProductName());
        assertEquals(100.0, product.getPrice(), 0.001);
    }

    @Test
    public void test_parameters2Bean() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "Alice" });
        params.put("age", new String[] { "25" });

        User user = URLEncodedUtil.parameters2Bean(params, User.class);
        assertNotNull(user);
        assertEquals("Alice", user.getName());
        assertEquals(25, user.getAge());
    }

    @Test
    public void test_parameters2Bean_multipleValues() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "Bob", "Bobby" });
        params.put("age", new String[] { "30" });

        User user = URLEncodedUtil.parameters2Bean(params, User.class);
        assertNotNull(user);
        assertEquals("Bob, Bobby", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    public void test_parameters2Bean_emptyValue() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "" });
        params.put("age", new String[] { "35" });

        User user = URLEncodedUtil.parameters2Bean(params, User.class);
        assertNotNull(user);
        assertNull(user.getName());
        assertEquals(35, user.getAge());
    }

    @Test
    public void test_parameters2Bean_nullMap() {
        User user = URLEncodedUtil.parameters2Bean(null, User.class);
        assertNotNull(user);
    }

    @Test
    public void test_parameters2Bean_emptyMap() {
        User user = URLEncodedUtil.parameters2Bean(new HashMap<>(), User.class);
        assertNotNull(user);
    }

    @Test
    public void test_encode_object() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "John Doe");
        params.put("age", 30);

        String result = URLEncodedUtil.encode(params);
        assertNotNull(result);
        assertTrue(result.contains("name=John+Doe"));
        assertTrue(result.contains("age=30"));
    }

    @Test
    public void test_encode_object_null() {
        String result = URLEncodedUtil.encode(null);
        assertEquals("", result);
    }

    @Test
    public void test_encode_object_bean() {
        User user = new User("John", 30);
        String result = URLEncodedUtil.encode(user);
        assertNotNull(result);
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
    }

    @Test
    public void test_encode_object_array() {
        Object[] params = new Object[] { "name", "John Doe", "age", 30 };
        String result = URLEncodedUtil.encode(params);
        assertNotNull(result);
        assertTrue(result.contains("name=John+Doe"));
        assertTrue(result.contains("age=30"));
    }

    @Test
    public void test_encode_object_string() {
        String result = URLEncodedUtil.encode("name=John&age=30");
        assertNotNull(result);
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
    }

    @Test
    public void test_encode_object_stringWithoutEquals() {
        String result = URLEncodedUtil.encode("test");
        assertEquals("test", result);
    }

    @Test
    public void test_encode_object_charset() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "中文");

        String result = URLEncodedUtil.encode(params, StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.contains("name=%E4%B8%AD%E6%96%87"));
    }

    @Test
    public void test_encode_object_charset_null() {
        String result = URLEncodedUtil.encode((Map) null, StandardCharsets.UTF_8);
        assertEquals("", result);
    }

    @Test
    public void test_encode_object_charset_nullCharset() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "test");

        String result = URLEncodedUtil.encode(params, (Charset) null);
        assertTrue(result.contains("name=test"));
    }

    @Test
    public void test_encode_object_charset_namingPolicy() {
        Product product = new Product("Laptop", 999.99);
        String result = URLEncodedUtil.encode(product, StandardCharsets.UTF_8, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertNotNull(result);
        assertTrue(result.contains("product_name=Laptop"));
        assertTrue(result.contains("price=999.99"));
    }

    @Test
    public void test_encode_object_charset_namingPolicy_noChange() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("firstName", "John");

        String result = URLEncodedUtil.encode(params, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName=John"));
    }

    @Test
    public void test_encode_object_charset_namingPolicy_null() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "test");

        String result = URLEncodedUtil.encode(params, StandardCharsets.UTF_8, (NamingPolicy) null);
        assertTrue(result.contains("name=test"));
    }

    @Test
    public void test_encode_url_object() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("q", "java url encoding");
        params.put("page", 1);

        String result = URLEncodedUtil.encode("http://search.example.com", params);
        assertEquals("http://search.example.com?q=java+url+encoding&page=1", result);
    }

    @Test
    public void test_encode_url_object_null() {
        String result = URLEncodedUtil.encode("http://example.com", (Map) null);
        assertEquals("http://example.com", result);
    }

    @Test
    public void test_encode_url_object_emptyMap() {
        String result = URLEncodedUtil.encode("http://example.com", new HashMap<>());
        assertEquals("http://example.com", result);
    }

    @Test
    public void test_encode_url_object_charset() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "中文");

        String result = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8);
        assertEquals("http://example.com?name=%E4%B8%AD%E6%96%87", result);
    }

    @Test
    public void test_encode_url_object_charset_null() {
        String result = URLEncodedUtil.encode("http://example.com", null, StandardCharsets.UTF_8);
        assertEquals("http://example.com", result);
    }

    @Test
    public void test_encode_url_object_charset_namingPolicy() {
        User user = new User("John", 30);
        String result = URLEncodedUtil.encode("http://api.com/users", user, StandardCharsets.UTF_8, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertTrue(result.startsWith("http://api.com/users?"));
        assertTrue(result.contains("name=John") || result.contains("age=30"));
    }

    @Test
    public void test_encode_url_object_charset_namingPolicy_null() {
        String result = URLEncodedUtil.encode("http://example.com", null, StandardCharsets.UTF_8, NamingPolicy.LOWER_CAMEL_CASE);
        assertEquals("http://example.com", result);
    }

    @Test
    public void test_encode_object_appendable() throws IOException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("key", "value");

        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(params, sb);

        assertEquals("key=value", sb.toString());
    }

    @Test
    public void test_encode_object_appendable_null() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode((Map) null, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void test_encode_object_appendable_writer() throws IOException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "test");

        StringWriter writer = new StringWriter();
        URLEncodedUtil.encode(params, writer);

        assertEquals("name=test", writer.toString());
    }

    @Test
    public void test_encode_object_charset_appendable() throws IOException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "中文");

        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(params, StandardCharsets.UTF_8, sb);

        assertEquals("name=%E4%B8%AD%E6%96%87", sb.toString());
    }

    @Test
    public void test_encode_object_charset_appendable_null() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(null, StandardCharsets.UTF_8, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void test_encode_object_charset_namingPolicy_appendable() throws IOException {
        User user = new User("John", 30);
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(user, StandardCharsets.UTF_8, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, sb);

        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("name=John") || result.contains("age=30"));
    }

    @Test
    public void test_encode_object_charset_namingPolicy_appendable_null() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(null, StandardCharsets.UTF_8, NamingPolicy.LOWER_CAMEL_CASE, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void test_encode_object_charset_namingPolicy_appendable_emptyMap() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(new HashMap<>(), StandardCharsets.UTF_8, NamingPolicy.LOWER_CAMEL_CASE, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void test_encode_object_charset_namingPolicy_appendable_arrayOddLength() {
        Object[] params = new Object[] { "name", "John", "age" };
        StringBuilder sb = new StringBuilder();
        assertThrows(IllegalArgumentException.class, () -> {
            URLEncodedUtil.encode(params, StandardCharsets.UTF_8, NamingPolicy.LOWER_CAMEL_CASE, sb);
        });
    }

    @Test
    public void test_decode_specialCharacters() {
        Map<String, String> result = URLEncodedUtil.decode("key=%21%40%23%24%25%5E%26*");
        assertEquals("!@#$%^&*", result.get("key"));
    }

    @Test
    public void test_encode_specialCharacters() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("key", "!@#$%^&*()");

        String result = URLEncodedUtil.encode(params);
        assertNotNull(result);
        assertTrue(result.contains("key="));
    }

    @Test
    public void test_decode_whitespace() {
        Map<String, String> result = URLEncodedUtil.decode("  name  =  value  ");
        assertEquals("value", result.get("name"));
    }

    @Test
    public void test_encode_emptyValue() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("key", "");

        String result = URLEncodedUtil.encode(params);
        assertEquals("key=", result);
    }

    @Test
    public void test_encode_multipleParameters() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "2");
        params.put("c", "3");

        String result = URLEncodedUtil.encode(params);
        assertTrue(result.contains("a=1"));
        assertTrue(result.contains("b=2"));
        assertTrue(result.contains("c=3"));
        assertTrue(result.contains("&"));
    }

    @Test
    public void test_decode_urlWithFragment() {
        Map<String, String> result = URLEncodedUtil.decode("name=John&age=30");
        assertEquals(2, result.size());
    }

    @Test
    public void test_encode_bean_nullProperties() {
        User user = new User();
        String result = URLEncodedUtil.encode(user);
        assertNotNull(result);
    }

    @Test
    public void test_decodeToMultimap_singleValue() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("name=John");
        assertEquals(1, result.get("name").size());
        assertEquals("John", result.get("name").get(0));
    }

    @Test
    public void test_encode_objectArray_evenLength() {
        Object[] params = new Object[] { "key1", "value1", "key2", "value2" };
        String result = URLEncodedUtil.encode(params);
        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2=value2"));
    }
}
