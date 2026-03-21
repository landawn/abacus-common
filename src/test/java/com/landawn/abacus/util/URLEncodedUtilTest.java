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
import java.util.function.Supplier;

import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.NamingPolicy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;

import lombok.Data;

public class URLEncodedUtilTest extends AbstractTest {

    @Data
    public static class User {
        private String name;
        private int age;
        private String email;

    }

    @Data
    public static class Product {
        private String productName;
        private double price;
    }

    @Data
    public static class TestBean {
        private String name;
        private int age;
        private boolean active;
        private String[] tags;
    }

    @Test
    public void test_encode_object_stringWithoutEquals() {
        String result = URLEncodedUtil.encode("test");
        assertEquals("test", result);
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
    public void test_encode_url_object_withExistingQueryAndFragment() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", 2);

        String result = URLEncodedUtil.encode("http://search.example.com?q=java#section", params);
        assertEquals("http://search.example.com?q=java&page=2#section", result);
    }

    @Test
    public void test_encode_url_object_charset() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "中文");

        String result = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8);
        assertEquals("http://example.com?name=%E4%B8%AD%E6%96%87", result);
    }

    @Test
    public void test_encode_objectArray_evenLength() {
        Object[] params = new Object[] { "key1", "value1", "key2", "value2" };
        String result = URLEncodedUtil.encode(params);
        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2=value2"));
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
    public void testEncodeWithUrlAndCharset() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "中文");

        String result = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8);
        Assertions.assertEquals("http://example.com?name=%E4%B8%AD%E6%96%87", result);
    }

    @Test
    public void test_format() {
        Account account = createAccount(Account.class);
        AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String query = URLEncodedUtil.encode(account);
        N.println(query);

        Account account2 = URLEncodedUtil.decode(query, Account.class);
        N.println(CommonUtil.stringOf(account2));

        query = URLEncodedUtil.encode(Beans.beanToMap(account));
        N.println(query);

        account2 = URLEncodedUtil.decode(query, Account.class);
        N.println(CommonUtil.stringOf(account2));
        assertNotNull(account2);
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
    public void test_encode_url_object_charset_namingPolicy_null() {
        String result = URLEncodedUtil.encode("http://example.com", null, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE);
        assertEquals("http://example.com", result);
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
    public void test_encode_bean_nullProperties() {
        User user = new User();
        String result = URLEncodedUtil.encode(user);
        assertNotNull(result);
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
    public void testEncodeWithNamingPolicy() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        String result = URLEncodedUtil.encode(bean, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE);
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
    public void testEncodeWithUrlCharsetAndNamingPolicy() {
        TestBean bean = new TestBean();
        bean.setName("John");

        String result = URLEncodedUtil.encode("http://example.com", bean, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE);
        Assertions.assertTrue(result.startsWith("http://example.com?"));
        Assertions.assertTrue(result.contains("name=John"));
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

    @Test
    public void testEncodeUrlWithParameters_WithNamingPolicy() {
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("name", "Alice");
        params.put("age", 25);
        String result = URLEncodedUtil.encode("http://example.com", params, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        Assertions.assertTrue(result.startsWith("http://example.com?"));
        Assertions.assertTrue(result.contains("name=Alice"));
        Assertions.assertTrue(result.contains("age=25"));
    }

    @Test
    public void testEncodeUrlWithParameters_NullParams() {
        String url = "http://example.com";
        String result = URLEncodedUtil.encode(url, null, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        Assertions.assertEquals(url, result);
    }

    @Test
    public void testEncodeUrlWithParameters_EmptyMap() {
        String url = "http://example.com";
        Map<String, Object> params = new HashMap<>();
        String result = URLEncodedUtil.encode(url, params, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        Assertions.assertEquals(url, result);
    }

    @Test
    public void testEncodeUrlWithFragment() {
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("key", "val");
        String result = URLEncodedUtil.encode("http://example.com/page#section", params, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        Assertions.assertTrue(result.contains("key=val"));
        Assertions.assertTrue(result.endsWith("#section"));
    }

    @Test
    public void testEncodeUrlWithExistingQueryString() {
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("b", "2");
        String result = URLEncodedUtil.encode("http://example.com?a=1", params, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        Assertions.assertTrue(result.contains("a=1"));
        Assertions.assertTrue(result.contains("b=2"));
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
    public void test_encode_object_charset_namingPolicy_appendable_null() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(null, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void test_encode_object_charset_namingPolicy_appendable_emptyMap() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(new HashMap<>(), StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void test_encode_object_charset_namingPolicy_appendable_arrayOddLength() {
        Object[] params = new Object[] { "name", "John", "age" };
        StringBuilder sb = new StringBuilder();
        assertThrows(IllegalArgumentException.class, () -> {
            URLEncodedUtil.encode(params, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE, sb);
        });
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

        URLEncodedUtil.encode(bean, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE, sb);
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
    public void testEncodeObjectWithNamingPolicyToAppendable() throws IOException {
        StringBuilder sb = new StringBuilder();
        User user = new User();
        user.setName("Bob");
        user.setAge(30);
        URLEncodedUtil.encode(user, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE, sb);
        String result = sb.toString();
        Assertions.assertTrue(result.contains("name=Bob"));
        Assertions.assertTrue(result.contains("age=30"));
    }

    @Test
    public void testEncUserInfo() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encUserInfo("user:pass@host", StandardCharsets.UTF_8, sb);
        String encoded = sb.toString();
        Assertions.assertNotNull(encoded);
        Assertions.assertFalse(encoded.contains("@"));
    }

    @Test
    public void testEncUserInfo_SpecialChars() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encUserInfo("hello world", StandardCharsets.UTF_8, sb);
        Assertions.assertFalse(sb.toString().contains(" "));
    }

    @Test
    public void testEncUric() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encUric("path?query=val&other=1", StandardCharsets.UTF_8, sb);
        String encoded = sb.toString();
        Assertions.assertNotNull(encoded);
    }

    @Test
    public void testEncPath() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encPath("/path/to/resource", StandardCharsets.UTF_8, sb);
        String encoded = sb.toString();
        Assertions.assertNotNull(encoded);
        Assertions.assertTrue(encoded.contains("path"));
    }

    @Test
    public void test_decodeToBean() {
        Account account = createAccount(Account.class);
        Map<String, Object> props = Beans.beanToMap(account);
        Map<String, String[]> parameters = new HashMap<>();

        for (String propName : props.keySet()) {
            parameters.put(propName, CommonUtil.asArray(CommonUtil.stringOf(props.get(propName))));
        }

        Account account2 = URLEncodedUtil.convertToBean(parameters, Account.class);

        assertEquals(account, account2);
    }

    @Test
    public void test_decode_string() {
        Map<String, String> result = URLEncodedUtil.decode("name=John+Doe&age=30");
        assertEquals("John Doe", result.get("name"));
        assertEquals("30", result.get("age"));
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
    public void test_decode_string_charset_class_encoding() {
        Product product = URLEncodedUtil.decode("productName=%E4%B8%AD%E6%96%87&price=100", StandardCharsets.UTF_8, Product.class);
        assertEquals("中文", product.getProductName());
        assertEquals(100.0, product.getPrice(), 0.001);
    }

    @Test
    public void test_decode_specialCharacters() {
        Map<String, String> result = URLEncodedUtil.decode("key=%21%40%23%24%25%5E%26*");
        assertEquals("!@#$%^&*", result.get("key"));
    }

    @Test
    public void test_decode_whitespace() {
        Map<String, String> result = URLEncodedUtil.decode("  name  =  value  ");
        assertEquals("value", result.get("name"));
    }

    @Test
    public void test_decode_urlWithFragment() {
        Map<String, String> result = URLEncodedUtil.decode("name=John&age=30");
        assertEquals(2, result.size());
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
    public void testDecodeToMap_WithCharset() {
        Map<String, String> result = URLEncodedUtil.decode("key=hello+world", StandardCharsets.UTF_8, java.util.LinkedHashMap.class);
        Assertions.assertEquals("hello world", result.get("key"));
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
    public void test_decodeToBean_multipleValues() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "Bob", "Bobby" });
        params.put("age", new String[] { "30" });

        User user = URLEncodedUtil.convertToBean(params, User.class);
        assertNotNull(user);
        assertEquals("Bob, Bobby", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    public void test_decodeToBean_emptyValue() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "" });
        params.put("age", new String[] { "35" });

        User user = URLEncodedUtil.convertToBean(params, User.class);
        assertNotNull(user);
        assertNull(user.getName());
        assertEquals(35, user.getAge());
    }

    @Test
    public void test_decodeToBean_nullMap() {
        User user = URLEncodedUtil.convertToBean(null, User.class);
        assertNotNull(user);
    }

    @Test
    public void test_decodeToBean_emptyMap() {
        User user = URLEncodedUtil.convertToBean(new HashMap<>(), User.class);
        assertNotNull(user);
    }

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
    public void testDecodeToBean_WithCharset() {
        User user = URLEncodedUtil.decode("name=Alice&age=30&email=a%40b.com", StandardCharsets.UTF_8, User.class);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("Alice", user.getName());
        Assertions.assertEquals(30, user.getAge());
        Assertions.assertEquals("a@b.com", user.getEmail());
    }

    @Test
    public void testDecodeToBean_EmptyQuery() {
        User user = URLEncodedUtil.decode("", StandardCharsets.UTF_8, User.class);
        Assertions.assertNotNull(user);
    }

    @Test
    public void test_decodeToMultimap_string_charset_withEncoding() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("name=%E4%B8%AD%E6%96%87&name=test", StandardCharsets.UTF_8);
        assertEquals(2, result.get("name").size());
        assertTrue(result.get("name").contains("中文"));
        assertTrue(result.get("name").contains("test"));
    }

    @Test
    public void testDecodeToMultimapWithSpecificCharset() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("name=%E4%B8%AD%E6%96%87&name=test", StandardCharsets.UTF_8);

        Assertions.assertEquals(2, result.get("name").size());
        Assertions.assertEquals("中文", result.get("name").get(0));
        Assertions.assertEquals("test", result.get("name").get(1));
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
    public void test_decodeToMultimap_singleValue() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("name=John");
        assertEquals(1, result.get("name").size());
        assertEquals("John", result.get("name").get(0));
    }

    @Test
    public void testDecodeToMultimapWithDefaultCharset() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("");
        Assertions.assertTrue(result.isEmpty());

        result = URLEncodedUtil.decodeToMultimap(null);
        Assertions.assertTrue(result.isEmpty());

        result = URLEncodedUtil.decodeToMultimap("key=value");
        Assertions.assertEquals(1, result.totalValueCount());
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
    public void testDecodeToMultimap_WithDuplicateKeys() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("tag=java&tag=url", StandardCharsets.UTF_8);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.get("tag").size());
        Assertions.assertTrue(result.get("tag").contains("java"));
        Assertions.assertTrue(result.get("tag").contains("url"));
    }

    @Test
    public void testDecodeToMultimap_Empty() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("", StandardCharsets.UTF_8);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testDecodeToMultimap_NullQuery() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap(null, StandardCharsets.UTF_8);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testParameters2Bean() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("name", new String[] { "John" });
        parameters.put("age", new String[] { "30" });
        parameters.put("active", new String[] { "true" });

        TestBean bean = URLEncodedUtil.convertToBean(parameters, TestBean.class);

        Assertions.assertEquals("John", bean.getName());
        Assertions.assertEquals(30, bean.getAge());
        Assertions.assertTrue(bean.isActive());

        parameters.put("tags", new String[] { "reading", "swimming", "coding" });
        bean = URLEncodedUtil.convertToBean(parameters, TestBean.class);
        Assertions.assertNotNull(bean);

        bean = URLEncodedUtil.convertToBean(new HashMap<>(), TestBean.class);
        Assertions.assertNotNull(bean);

        bean = URLEncodedUtil.convertToBean(null, TestBean.class);
        Assertions.assertNotNull(bean);

        parameters.clear();
        parameters.put("name", new String[] { "" });
        bean = URLEncodedUtil.convertToBean(parameters, TestBean.class);
        Assertions.assertNull(bean.getName());

        parameters.clear();
        parameters.put("tags", new String[] { "tag1", "tag2", "tag3" });
        bean = URLEncodedUtil.convertToBean(parameters, TestBean.class);
        Assertions.assertArrayEquals(new String[] { "tag1", "tag2", "tag3" }, bean.getTags());
    }

    @Test
    public void testConvertToBean_WithParameters() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "Charlie" });
        params.put("age", new String[] { "25" });
        User user = URLEncodedUtil.convertToBean(params, User.class);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("Charlie", user.getName());
        Assertions.assertEquals(25, user.getAge());
    }

    @Test
    public void testConvertToBean_EmptyParams() {
        User user = URLEncodedUtil.convertToBean(null, User.class);
        Assertions.assertNotNull(user);
    }

    @Test
    public void testConvertToBean_EmptyStringValues() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "" });
        User user = URLEncodedUtil.convertToBean(params, User.class);
        Assertions.assertNotNull(user);
        Assertions.assertNull(user.getName()); // empty string -> default value
    }

    @Test
    public void testConstants() {
        Assertions.assertEquals('&', URLEncodedUtil.QP_SEP_A);
        Assertions.assertEquals(';', URLEncodedUtil.QP_SEP_S);
        Assertions.assertEquals("=", URLEncodedUtil.NAME_VALUE_SEPARATOR);
    }

}
