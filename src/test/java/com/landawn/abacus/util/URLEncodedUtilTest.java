package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

import lombok.Data;
import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.entity.extendDirty.basic.AccountContact;

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

    @Data
    public static class NullableBean {
        private String value;
    }

    @Test
    public void testEncode_stringWithoutEquals() {
        String result = URLEncodedUtil.encode("test");
        assertEquals("test", result);
    }

    @Test
    public void testEncodeUrl_withExistingQueryAndFragment() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", 2);

        String result = URLEncodedUtil.encode("http://search.example.com?q=java#section", params);
        assertEquals("http://search.example.com?q=java&page=2#section", result);
    }

    @Test
    public void testEncodeUrlDoesNotAppendSeparatorForBeanWithNoEncodedProperties() {
        final NullableBean bean = new NullableBean();

        assertEquals("https://example.test/path#fragment", URLEncodedUtil.encode("https://example.test/path#fragment", bean));
        assertEquals("https://example.test/path?a=1#fragment", URLEncodedUtil.encode("https://example.test/path?a=1#fragment", bean));
        assertEquals("https://example.test/path?", URLEncodedUtil.encode("https://example.test/path?", bean));
        assertEquals("https://example.test/path?a=1&", URLEncodedUtil.encode("https://example.test/path?a=1&", bean));
    }

    @Test
    public void testEncode_rejectsInvalidParameterNames() {
        final Map<Object, Object> nonStringKey = new LinkedHashMap<>();
        nonStringKey.put(1, "value");
        assertThrows(ClassCastException.class, () -> URLEncodedUtil.encode(nonStringKey));
        assertThrows(ClassCastException.class, () -> URLEncodedUtil.encode(new Object[] { 1, "value" }));

        final Map<Object, Object> nullKey = new LinkedHashMap<>();
        nullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(nullKey));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(new Object[] { null, "value" }));
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
    public void testFormat() {
        Account account = createAccount(Account.class);
        AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String query = URLEncodedUtil.encode(account);
        assertFalse(query.isEmpty());

        Account account2 = URLEncodedUtil.decode(query, Account.class);
        assertNotNull(account2);

        query = URLEncodedUtil.encode(Beans.beanToMap(account));
        assertFalse(query.isEmpty());

        account2 = URLEncodedUtil.decode(query, Account.class);
        assertNotNull(account2);
    }

    @Test
    public void testEncodeObject_nullWithCharset() {
        String result = URLEncodedUtil.encode((Map) null, StandardCharsets.UTF_8);
        assertEquals("", result);
    }

    @Test
    public void testEncodeObject_nullCharset() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "test");

        String result = URLEncodedUtil.encode(params, (Charset) null);
        assertTrue(result.contains("name=test"));
    }

    @Test
    public void testEncodeObject_namingPolicyNoChange() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("firstName", "John");

        String result = URLEncodedUtil.encode(params, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE);
        assertTrue(result.contains("firstName=John"));
    }

    @Test
    public void testEncodeObject_nullNamingPolicy() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "test");

        String result = URLEncodedUtil.encode(params, StandardCharsets.UTF_8, (NamingPolicy) null);
        assertTrue(result.contains("name=test"));
    }

    @Test
    public void testEncodeUrl_nullParamsWithNamingPolicy() {
        String result = URLEncodedUtil.encode("http://example.com", null, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE);
        assertEquals("http://example.com", result);
    }

    @Test
    public void testEncode_emptyValue() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("key", "");

        String result = URLEncodedUtil.encode(params);
        assertEquals("key=", result);
    }

    @Test
    public void testEncodeBean_nullProperties() {
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
        params.put("first_name", "Ada");
        result = URLEncodedUtil.encode(params);
        Assertions.assertEquals("first_name=Ada", result);
        Assertions.assertEquals("first_name=Ada", URLEncodedUtil.encode(params, StandardCharsets.UTF_8));

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

        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("q", "java url encoding");
        ordered.put("page", 1);
        Assertions.assertEquals("http://search.example.com?q=java+url+encoding&page=1", URLEncodedUtil.encode("http://search.example.com", ordered));

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
        Assertions.assertFalse(result.contains("key1="));
        Assertions.assertTrue(result.contains("key1"));
        Assertions.assertTrue(result.contains("key2=value2"));

        // A null value round-trips as a valueless token, not the literal string "null".
        final Map<String, String> decodedParams = URLEncodedUtil.decode(result);
        Assertions.assertTrue(decodedParams.containsKey("key1"));
        Assertions.assertNull(decodedParams.get("key1"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("key=%2"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("key=%ZZ"));
        Assertions.assertEquals("%2", URLEncodedUtil.decodeLenient("key=%2").get("key"));
        Assertions.assertEquals("%ZZ", URLEncodedUtil.decodeLenient("key=%ZZ").get("key"));

        Map<String, String> decoded = URLEncodedUtil.decode("a=1&b=2;c=3&d=4");
        Assertions.assertEquals(4, decoded.size());

        decoded = URLEncodedUtil.decode("  key  =  value  ");
        Assertions.assertEquals("  value  ", decoded.get("  key  "));

        final ListMultimap<String, String> decodedMultimap = URLEncodedUtil.decodeToMultimap("  key  =  value  &  key  = second ");
        Assertions.assertEquals(Arrays.asList("  value  ", " second "), decodedMultimap.get("  key  "));

        final NullableBean decodedBean = URLEncodedUtil.decode("value=  text  ", NullableBean.class);
        Assertions.assertEquals("  text  ", decodedBean.getValue());
    }

    @Test
    public void testEncodeDecodeRoundTrip_MultiByteCharsets() {
        // Every byte of a non-safe character must be percent-escaped so decode(encode(v, cs), cs)
        // round-trips for multi-byte and stateful charsets (UTF-16, ISO-2022-JP, ...).
        for (final String charsetName : new String[] { "UTF-16", "ISO-2022-JP", "UTF-8" }) {
            final Charset charset = Charset.forName(charsetName);
            final Map<String, Object> params = new LinkedHashMap<>();
            params.put("k", "abc");
            params.put("text", "a b&c");

            if (charsetName.equals("ISO-2022-JP")) {
                params.put("stateful", "日本a語");
            }

            final String encoded = URLEncodedUtil.encode(params, charset);
            final Map<String, String> decoded = URLEncodedUtil.decode(encoded, charset);

            Assertions.assertEquals("abc", decoded.get("k"), charsetName);
            Assertions.assertEquals("a b&c", decoded.get("text"), charsetName);

            if (charsetName.equals("ISO-2022-JP")) {
                Assertions.assertEquals("日本a語", decoded.get("stateful"), charsetName);
            }
        }

        // UTF-16 specifically used to split a character's bytes across the literal/escape boundary.
        final Map<String, Object> params = new LinkedHashMap<>();
        params.put("k", "abc");
        Assertions.assertEquals("abc", URLEncodedUtil.decode(URLEncodedUtil.encode(params, StandardCharsets.UTF_16), StandardCharsets.UTF_16).get("k"));
    }

    @Test
    public void testEncodeRejectsLossyCharsetConversion() {
        final Map<String, Object> params = new LinkedHashMap<>();
        params.put("value", "é");

        final IllegalArgumentException unmappable = assertThrows(IllegalArgumentException.class,
                () -> URLEncodedUtil.encode(params, StandardCharsets.US_ASCII));
        assertTrue(unmappable.getMessage().contains("US-ASCII"));

        params.put("value", "\uD800");
        final IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(params, StandardCharsets.UTF_8));
        assertTrue(malformed.getMessage().contains("UTF-8"));
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
    public void testEncodeToAppendable_writer() throws IOException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "test");

        StringWriter writer = new StringWriter();
        URLEncodedUtil.encode(params, writer);

        assertEquals("name=test", writer.toString());
    }

    @Test
    public void testEncodeToAppendableWithCharset_null() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(null, StandardCharsets.UTF_8, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void testEncodeToAppendableWithCharsetAndNamingPolicy_null() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(null, StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void testEncodeToAppendableWithCharsetAndNamingPolicy_emptyMap() throws IOException {
        StringBuilder sb = new StringBuilder();
        URLEncodedUtil.encode(new HashMap<>(), StandardCharsets.UTF_8, NamingPolicy.CAMEL_CASE, sb);
        assertEquals("", sb.toString());
    }

    @Test
    public void testRequiredEncodingTargetsAreValidatedForEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(null, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE, null));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode((String) null, null, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE));
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
    public void testComponentEncodersRejectNullCharset() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encUserInfo("", null, new StringBuilder()));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encUric("safe", null, new StringBuilder()));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encPath(null, null, new StringBuilder()));
    }

    @Test
    public void testConvertToBean_account() {
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
    public void testDecode_duplicateKeysKeepLast() {
        Map<String, String> result = URLEncodedUtil.decode("color=red&color=blue");
        assertEquals("blue", result.get("color"));
    }

    @Test
    public void testDecodeToProduct_withCharset() {
        Product product = URLEncodedUtil.decode("productName=%E4%B8%AD%E6%96%87&price=100", StandardCharsets.UTF_8, Product.class);
        assertEquals("中文", product.getProductName());
        assertEquals(100.0, product.getPrice(), 0.001);

        product = URLEncodedUtil.decode("productName=Laptop&price=999.99", StandardCharsets.UTF_8, Product.class);
        assertEquals("Laptop", product.getProductName());
        assertEquals(999.99, product.getPrice(), 0.001);
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
    public void testDecodeMapSupplierValidationIsEager() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode(null, StandardCharsets.UTF_8, (Supplier<Map<String, String>>) null));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode(null, StandardCharsets.UTF_8, () -> null));
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
    public void testDecode_emptyValue() {
        Map<String, String> result = URLEncodedUtil.decode("name=John&age=");
        assertEquals("John", result.get("name"));
        assertEquals("", result.get("age"));
    }

    @Test
    public void testDecode_nullWithCharset() {
        Map<String, String> result = URLEncodedUtil.decode(null, StandardCharsets.UTF_8);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDecode_nullCharset() {
        Charset nullCharset = null;
        Map<String, String> result = URLEncodedUtil.decode("name=test", nullCharset);
        assertEquals("test", result.get("name"));
    }

    @Test
    public void testDecodeWithMapSupplier_treeMap() {
        TreeMap<String, String> result = URLEncodedUtil.decode("b=2&a=1", StandardCharsets.UTF_8, TreeMap::new);
        assertNotNull(result);
        assertTrue(result instanceof TreeMap);
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("a", result.firstKey());
    }

    @Test
    public void testDecodeWithMapSupplier_empty() {
        HashMap<String, String> result = URLEncodedUtil.decode("", StandardCharsets.UTF_8, HashMap::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDecodeToUser_nullQuery() {
        User user = URLEncodedUtil.decode(null, User.class);
        assertNotNull(user);
    }

    @Test
    public void testDecodeToClass_nullTargetType() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("name=John", (Class<User>) null));
    }

    @Test
    public void testDecodeToMap_withoutCharset() {
        @SuppressWarnings("unchecked")
        Map<String, String> result = URLEncodedUtil.decode("name=John&age=30", LinkedHashMap.class);
        assertNotNull(result);
        assertEquals("John", result.get("name"));
        assertEquals("30", result.get("age"));
    }

    @Test
    public void testDecodeToUser_nullQueryWithCharset() {
        User user = URLEncodedUtil.decode(null, StandardCharsets.UTF_8, User.class);
        assertNotNull(user);
    }

    @Test
    public void testConvertToBean_multipleValues() {
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[] { "Bob", "Bobby" });
        params.put("age", new String[] { "30" });

        User user = URLEncodedUtil.convertToBean(params, User.class);
        assertNotNull(user);
        assertEquals("Bob, Bobby", user.getName());
        assertEquals(30, user.getAge());
    }

    @Test
    public void testConvertToBean_nullTargetType() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.convertToBean(new HashMap<>(), null));
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

        result = URLEncodedUtil.decode("name=John+Doe&age=30");
        Assertions.assertEquals("John Doe", result.get("name"));
        Assertions.assertEquals("30", result.get("age"));

        result = URLEncodedUtil.decode("name=John%20Doe&email=john%40example.com");
        Assertions.assertEquals("John Doe", result.get("name"));
        Assertions.assertEquals("john@example.com", result.get("email"));

        result = URLEncodedUtil.decode("key=%21%40%23%24%25%5E%26*");
        Assertions.assertEquals("!@#$%^&*", result.get("key"));

        result = URLEncodedUtil.decode("a=+x+&b=%20x%20&c= x ");
        Assertions.assertEquals(" x ", result.get("a"));
        Assertions.assertEquals(" x ", result.get("b"));
        Assertions.assertEquals(" x ", result.get("c"));
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
    public void testDecodeToMultimapWithSpecificCharset() {
        ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimap("name=%E4%B8%AD%E6%96%87&name=test", StandardCharsets.UTF_8);

        Assertions.assertEquals(2, result.get("name").size());
        Assertions.assertEquals("中文", result.get("name").get(0));
        Assertions.assertEquals("test", result.get("name").get(1));
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

        result = URLEncodedUtil.decodeToMultimap("color=red&color=blue&size=L");
        Assertions.assertEquals(2, result.get("color").size());
        Assertions.assertTrue(result.get("color").contains("red"));
        Assertions.assertTrue(result.get("color").contains("blue"));
        Assertions.assertEquals(1, result.get("size").size());
        Assertions.assertEquals("L", result.get("size").get(0));
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

    @Test
    public void testDecodeWithSurrogatePairAfterAsciiPrefix() {
        // Regression: in urlDecode the surrogate-pair peek used cb.charAt(cb.position())
        // which is RELATIVE on CharBuffer (= absolute index 2*position) and so missed the
        // low surrogate when the high surrogate was not at position 0. The non-ASCII chars
        // were then encoded as two lone surrogates, producing replacement bytes after
        // round-tripping through the charset.
        // Use 'q' as the parameter name so the value (with the emoji) is decoded literally.
        String emoji = new String(Character.toChars(0x1F600)); // grinning face: surrogate pair
        String value = "ab" + emoji + "cd";
        Map<String, String> decoded = URLEncodedUtil.decode("q=" + value);
        assertEquals(value, decoded.get("q"));
    }

    @Test
    public void testDecodeWithCharsetWhoseEncoderCanEmitMoreThanFourBytesPerCharacter() {
        Charset charset = Charset.forName("ISO-2022-JP");

        // The previous content.length() * 4 byte buffer overflows once several literal
        // characters are each encoded with ISO-2022-JP's shift sequences.
        assertEquals("あいうえお", URLEncodedUtil.decode("q=あいうえお", charset).get("q"));
    }

    @Test
    public void testDecodeRejectsMalformedPercentEscapeAndLenientModePreservesIt() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("q=%雪x"));
        assertEquals("%雪x", URLEncodedUtil.decodeLenient("q=%雪x").get("q"));
    }

    @Test
    public void testDecodeUsesAsciiHexAndReportsMalformedEncodedBytes() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("q=%１２"));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("q=%١٢"));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("q=%C3"));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("q=%ZZ"));
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("q=%A"));

        assertEquals("%１２", URLEncodedUtil.decodeLenient("q=%１２").get("q"));
        assertEquals("%١٢", URLEncodedUtil.decodeLenient("q=%١٢").get("q"));
        assertEquals("�", URLEncodedUtil.decodeLenient("q=%C3").get("q"));
        assertEquals("%ZZ", URLEncodedUtil.decodeLenient("q=%ZZ").get("q"));
        assertEquals("%A", URLEncodedUtil.decodeLenient("q=%A").get("q"));
        assertEquals("ÿ", URLEncodedUtil.decode("q=%FF", StandardCharsets.ISO_8859_1).get("q"));
    }

    @Test
    public void testDecodeToMultimapStrictAndLenientPolicies() {
        assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decodeToMultimap("q=%C3&q=ok"));

        final ListMultimap<String, String> result = URLEncodedUtil.decodeToMultimapLenient("q=%C3&q=ok");
        assertEquals(List.of("�", "ok"), result.get("q"));
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testDecodeSkipsEmptyTokensBetweenSeparators() {
        // regression: consecutive separators produced an empty token that was decoded into a
        // bogus empty-string parameter name
        Map<String, String> decoded = URLEncodedUtil.decode("a=1&&b=2");

        assertEquals(2, decoded.size());
        assertEquals("1", decoded.get("a"));
        assertEquals("2", decoded.get("b"));
        org.junit.jupiter.api.Assertions.assertFalse(decoded.containsKey(""));

        // Only truly empty tokens are skipped. Whitespace is significant query data and remains a valueless name.
        decoded = URLEncodedUtil.decode("&a=1;; &;b=2&");
        assertEquals(3, decoded.size());
        assertEquals("1", decoded.get("a"));
        assertTrue(decoded.containsKey(" "));
        assertNull(decoded.get(" "));
        assertEquals("2", decoded.get("b"));

        final ListMultimap<String, String> decodedMultimap = URLEncodedUtil.decodeToMultimap("&a=1;; &;b=2&");
        assertEquals(Arrays.asList((String) null), decodedMultimap.get(" "));
        assertEquals(3, decodedMultimap.totalValueCount());
    }

    @Test
    public void testEncodeUrlWithPreEncodedQueryStringAppendsVerbatim() {
        // regression: a pre-built query string (documented as already URL-encoded) was split via
        // a map-based splitter (silently dropping duplicate names: a=1&a=2 -> a=2) and re-percent-
        // encoded (q=a%20b -> q=a%2520b)
        assertEquals("http://h/search?a=1&a=2", URLEncodedUtil.encode("http://h/search", "a=1&a=2"));
        assertEquals("http://h/s?q=a%20b", URLEncodedUtil.encode("http://h/s", "q=a%20b"));
        assertEquals("http://h/s?q=a+b", URLEncodedUtil.encode("http://h/s", "q=a+b"));
        assertEquals("http://h/s?x=1&q=2", URLEncodedUtil.encode("http://h/s?x=1", "q=2")); // existing query joined with &
    }

    @Test
    public void testEncodeStandaloneParameterStringAppendsVerbatim() {
        // A CharSequence containing '=' is a pre-built, already-encoded query string and is appended
        // verbatim - the same rule encode(String url, Object) applies. It used to be re-split into
        // name/value pairs and re-encoded here only, so the two overloads disagreed: encode("q=a%20b")
        // produced "q=a%2520b" while encode(url, "q=a%20b") produced "...?q=a%20b".
        assertEquals("q=a%20b", URLEncodedUtil.encode("q=a%20b"));
        assertEquals("a=1&a=2", URLEncodedUtil.encode("a=1&a=2"));
        assertEquals(" a = 1 & a = 2 & q = hello world ", URLEncodedUtil.encode(" a = 1 & a = 2 & q = hello world "));

        // The two overloads must agree on the same input.
        assertEquals("http://h/s?" + URLEncodedUtil.encode("q=a%20b"), URLEncodedUtil.encode("http://h/s", "q=a%20b"));

        // A CharSequence WITHOUT '=' is not a query string: it is encoded as a single form field.
        assertEquals("hello+world", URLEncodedUtil.encode("hello world"));

        final StringBuilder output = new StringBuilder();
        URLEncodedUtil.encode("a=1&a=2", StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE, output);
        assertEquals("a=1&a=2", output.toString());
    }

    @Test
    public void testDecodeRejectsMapTargetTypesThatCannotBeInstantiated() {
        // Suppliers.ofMap silently substitutes a HashMap/TreeMap for these four, and the unchecked cast in
        // decode erases, so the caller used to get a bare ClassCastException thrown in its own frame.
        for (final Class<?> targetType : new Class<?>[] { ImmutableMap.class, ImmutableSortedMap.class, ImmutableNavigableMap.class,
                java.util.EnumMap.class }) {
            final String expected = "Cannot decode into " + targetType.getName() + ": no mutable instance of that type can be created";

            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("a=1&b=2", targetType)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("", targetType)).getMessage());
            assertEquals(expected,
                    assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decode("a=1", StandardCharsets.UTF_8, targetType)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decodeLenient("a=1", targetType)).getMessage());
            assertEquals(expected,
                    assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.decodeLenient("a=1", StandardCharsets.UTF_8, targetType)).getMessage());
        }

        // Map targets whose substitution still satisfies the request are untouched.
        assertEquals(LinkedHashMap.class, URLEncodedUtil.decode("a=1", Map.class).getClass());
        assertEquals(LinkedHashMap.class, URLEncodedUtil.decode("a=1", java.util.AbstractMap.class).getClass());
        assertEquals(LinkedHashMap.class, URLEncodedUtil.decode("a=1", LinkedHashMap.class).getClass());
        assertEquals(HashMap.class, URLEncodedUtil.decode("a=1", HashMap.class).getClass());
        assertEquals(TreeMap.class, URLEncodedUtil.decode("a=1", TreeMap.class).getClass());
        assertEquals(TreeMap.class, URLEncodedUtil.decode("a=1", java.util.SortedMap.class).getClass());
        assertEquals(TreeMap.class, URLEncodedUtil.decode("a=1", java.util.NavigableMap.class).getClass());
        assertEquals(java.util.concurrent.ConcurrentHashMap.class, URLEncodedUtil.decode("a=1", java.util.concurrent.ConcurrentMap.class).getClass());
        assertEquals(BiMap.class, URLEncodedUtil.decode("a=1", BiMap.class).getClass());
        assertEquals("1", URLEncodedUtil.decode("a=1", BiMap.class).get("a"));
    }

    @Test
    public void testDecodeValuelessTokenIntoNullHostileMapNamesTheCulprit() {
        // A token without '=' is documented to be stored with a null value; a null-hostile Map rejects that
        // with a message-less NPE, although the class contract promises descriptive NPE messages.
        assertEquals(
                "The Map created for this call (java.util.concurrent.ConcurrentHashMap) does not permit null values, but the query contains"
                        + " the valueless token \"flag\". Use a null-tolerant Map or decodeToMultimap(..).",
                assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("flag", java.util.concurrent.ConcurrentMap.class)).getMessage());

        assertEquals(
                "The Map created for this call (java.util.Hashtable) does not permit null values, but the query contains"
                        + " the valueless token \"debug\". Use a null-tolerant Map or decodeToMultimap(..).",
                assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("debug", java.util.Hashtable.class)).getMessage());

        assertEquals(
                "The Map created for this call (java.util.concurrent.ConcurrentSkipListMap) does not permit null values, but the query contains"
                        + " the valueless token \"verbose\". Use a null-tolerant Map or decodeToMultimap(..).",
                assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("a=1&verbose&b=2", java.util.concurrent.ConcurrentNavigableMap.class))
                        .getMessage());

        // the Supplier entry points and both lenient families reach the same guard
        assertNotNull(
                assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("flag", StandardCharsets.UTF_8, java.util.Hashtable::new)).getMessage());
        assertNotNull(assertThrows(NullPointerException.class,
                () -> URLEncodedUtil.decodeLenient("flag", StandardCharsets.UTF_8, java.util.concurrent.ConcurrentHashMap::new)).getMessage());
        assertNotNull(assertThrows(NullPointerException.class, () -> URLEncodedUtil.decodeLenient("flag", java.util.Hashtable.class)).getMessage());
        assertNotNull(assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("flag", java.util.Properties.class)).getMessage());

        // The token is interpolated from the query, so it is bounded and its control characters escaped: a
        // crafted token must not be able to forge a line in a log that records this message, and a huge one
        // must not allocate a message as large as the query.
        final String crafted = assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("%0A%0DFAKE%20LOG%20LINE", java.util.Hashtable.class))
                .getMessage();
        assertFalse(crafted.contains("\n"), crafted);
        assertFalse(crafted.contains("\r"), crafted);
        assertTrue(crafted.contains("\\u000A\\u000DFAKE LOG LINE"), crafted);

        final String huge = assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("x".repeat(200_000), java.util.Hashtable.class)).getMessage();
        assertTrue(huge.length() < 300, "message length " + huge.length() + " grows with the token");
        assertTrue(huge.contains("\"" + "x".repeat(61) + "...\""), huge);

        // a well-formed surrogate pair is printable and passes through whole
        assertTrue(assertThrows(NullPointerException.class, () -> URLEncodedUtil.decode("%F0%9F%98%80ok", java.util.Hashtable.class)).getMessage()
                .contains(Character.toString(0x1F600) + "ok"));

        // the documented null is unchanged wherever the Map tolerates it
        final Map<String, String> tolerant = URLEncodedUtil.decode("flag", TreeMap.class);
        assertTrue(tolerant.containsKey("flag"));
        assertNull(tolerant.get("flag"));
        assertEquals("1", URLEncodedUtil.decode("flag=1", java.util.Hashtable.class).get("flag"));
    }

    @Test
    public void testEncodeStringifiesAnyOtherParameterShape() {
        // documented catch-all: a value that is not a Map, bean, Object[] or CharSequence is converted with
        // N.stringOf(..) into ONE valueless form field instead of being split into name/value pairs
        assertEquals("%5B1%2C+2%5D", URLEncodedUtil.encode((Object) new int[] { 1, 2 }));
        assertEquals("a=1", URLEncodedUtil.encode((Object) new String[] { "a", "1" }));
        assertEquals("%5B%22a%22%2C+%22b%22%5D", URLEncodedUtil.encode((Object) List.of("a", "b")));
        assertEquals("42", URLEncodedUtil.encode((Object) 42));
        assertEquals("http://x/p?%5B1%2C+2%5D", URLEncodedUtil.encode("http://x/p", (Object) new int[] { 1, 2 }));

        final Map<String, String> roundTripped = URLEncodedUtil.decode(URLEncodedUtil.encode((Object) new int[] { 1, 2 }));
        assertTrue(roundTripped.containsKey("[1, 2]"));
        assertNull(roundTripped.get("[1, 2]"));

        // ... so an EMPTY Collection or empty primitive array is not one of the @return empty cases: it still
        // encodes, as the text "[]". Only an empty Map/CharSequence/Object[] (and null) leave the URL untouched.
        assertEquals("http://x/p?%5B%5D", URLEncodedUtil.encode("http://x/p", (Object) new java.util.ArrayList<>()));
        assertEquals("http://x/p?%5B%5D", URLEncodedUtil.encode("http://x/p", (Object) new int[0]));
        assertEquals("http://x/p?%5B%5D", URLEncodedUtil.encode("http://x/p", new java.util.ArrayList<>(), StandardCharsets.UTF_8));
        assertEquals("http://x/p?%5B%5D", URLEncodedUtil.encode("http://x/p", new int[0], StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE));
        assertEquals("http://x/p", URLEncodedUtil.encode("http://x/p", (Object) new Object[0]));
        assertEquals("http://x/p", URLEncodedUtil.encode("http://x/p", (Object) ""));
        assertEquals("http://x/p", URLEncodedUtil.encode("http://x/p", (Object) new LinkedHashMap<>()));
        assertEquals("http://x/p", URLEncodedUtil.encode("http://x/p", (Object) null));
    }

    @Test
    public void testEncodeRejectsEmptyNameWithNullValueOnUtf8Overloads() {
        final Map<String, Object> emptyNameNullValue = new LinkedHashMap<>();
        emptyNameNullValue.put("", null);
        final String message = "An empty parameter name requires a non-null value";

        assertEquals(message, assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(emptyNameNullValue)).getMessage());
        assertEquals(message, assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode("http://x", emptyNameNullValue)).getMessage());
        assertEquals(message, assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(emptyNameNullValue, new StringBuilder())).getMessage());
        assertEquals(message, assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode((Object) new Object[] { "", null })).getMessage());

        // the @throws sentence now stands on every encode overload, so all nine are covered here
        assertEquals(message,
                assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(emptyNameNullValue, StandardCharsets.UTF_8)).getMessage());
        assertEquals(message,
                assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(emptyNameNullValue, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE))
                        .getMessage());
        assertEquals(message,
                assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode("http://x", emptyNameNullValue, StandardCharsets.UTF_8)).getMessage());
        assertEquals(message, assertThrows(IllegalArgumentException.class,
                () -> URLEncodedUtil.encode("http://x", emptyNameNullValue, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE)).getMessage());
        assertEquals(message,
                assertThrows(IllegalArgumentException.class, () -> URLEncodedUtil.encode(emptyNameNullValue, StandardCharsets.UTF_8, new StringBuilder()))
                        .getMessage());
        assertEquals(message, assertThrows(IllegalArgumentException.class,
                () -> URLEncodedUtil.encode(emptyNameNullValue, StandardCharsets.UTF_8, NamingPolicy.NO_CHANGE, new StringBuilder())).getMessage());
    }

    @Test
    public void testConvertToBeanJoinsNullElementOfMultiValueArrayAsText() {
        final Map<String, String[]> parameters = new HashMap<>();

        parameters.put("name", new String[] { null, "x" });
        assertEquals("null, x", URLEncodedUtil.convertToBean(parameters, User.class).getName());

        // a lone null element counts as empty, so the property falls back to its default value
        parameters.put("name", new String[] { null });
        assertNull(URLEncodedUtil.convertToBean(parameters, User.class).getName());

        // the consequence of the documented join on a TYPED property: "null, 5" is not a number, and the
        // conversion failure reaches the caller as a raw NumberFormatException
        final Map<String, String[]> typed = new HashMap<>();
        typed.put("age", new String[] { null, "5" });
        assertThrows(NumberFormatException.class, () -> URLEncodedUtil.convertToBean(typed, User.class));

        // ... while a lone null on the same typed property still falls back to the type default
        typed.put("age", new String[] { null });
        assertEquals(0, URLEncodedUtil.convertToBean(typed, User.class).getAge());
    }
}
