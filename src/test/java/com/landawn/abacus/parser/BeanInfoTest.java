package com.landawn.abacus.parser;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.Table;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

public class BeanInfoTest extends TestBase {

    private ParserUtil.BeanInfo beanInfo;
    private TestBean testBean;

    @BeforeEach
    public void setup() {
        beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        testBean = new TestBean();
    }

    @Test
    public void testBeanInfoConstructor() {
        Assertions.assertEquals(TestBean.class, beanInfo.clazz);
        Assertions.assertEquals("TestBean", beanInfo.simpleClassName);
        Assertions.assertEquals(TestBean.class.getCanonicalName(), beanInfo.canonicalClassName);
        Assertions.assertNotNull(beanInfo.type);
        Assertions.assertNotNull(beanInfo.propNameList);
        Assertions.assertNotNull(beanInfo.propInfoList);
    }

    @Test
    public void testGetPropInfoByName() {
        ParserUtil.PropInfo propInfo = beanInfo.getPropInfo("id");
        Assertions.assertNotNull(propInfo);
        Assertions.assertEquals("id", propInfo.name);
        Assertions.assertEquals(Long.class, propInfo.clazz);

        ParserUtil.PropInfo nonExistent = beanInfo.getPropInfo("nonExistentProperty");
        Assertions.assertNull(nonExistent);

        ParserUtil.PropInfo aliasedProp = beanInfo.getPropInfo("alias1");
        Assertions.assertNotNull(aliasedProp);
        Assertions.assertEquals("aliasedField", aliasedProp.name);
    }

    @Test
    public void testGetPropInfoByPropInfo() {
        ParserUtil.BeanInfo otherBeanInfo = ParserUtil.getBeanInfo(OtherTestBean.class);
        ParserUtil.PropInfo otherPropInfo = otherBeanInfo.getPropInfo("sharedField");

        ParserUtil.PropInfo propInfo = beanInfo.getPropInfo(otherPropInfo);
        Assertions.assertNotNull(propInfo);
        Assertions.assertEquals("sharedField", propInfo.name);
    }

    @Test
    public void testGetPropValue() {
        testBean.setId(123L);
        testBean.setSimpleName("TestName");

        Long id = beanInfo.getPropValue(testBean, "id");
        Assertions.assertEquals(123L, id);

        String name = beanInfo.getPropValue(testBean, "simpleName");
        Assertions.assertEquals("TestName", name);
    }

    @Test
    public void testGetPropValueNested() {
        testBean.setNestedBean(new NestedBean());
        testBean.getNestedBean().setNestedValue("NestedValue");

        String nestedValue = beanInfo.getPropValue(testBean, "nestedBean.nestedValue");
        Assertions.assertEquals("NestedValue", nestedValue);

        testBean.setNestedBean(null);
        String nullNestedValue = beanInfo.getPropValue(testBean, "nestedBean.nestedValue");
        Assertions.assertNull(nullNestedValue);
    }

    @Test
    public void testSetPropValue() {
        beanInfo.setPropValue(testBean, "id", 456L);
        Assertions.assertEquals(456L, testBean.getId());

        beanInfo.setPropValue(testBean, "simpleName", "UpdatedName");
        Assertions.assertEquals("UpdatedName", testBean.getSimpleName());
    }

    @Test
    public void testSetPropValueWithIgnoreUnmatched() {
        boolean result = beanInfo.setPropValue(testBean, "id", 789L, false);
        Assertions.assertTrue(result);
        Assertions.assertEquals(789L, testBean.getId());

        boolean nonExistentResult = beanInfo.setPropValue(testBean, "nonExistent", "value", true);
        Assertions.assertFalse(nonExistentResult);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            beanInfo.setPropValue(testBean, "nonExistent", "value", false);
        });
    }

    @Test
    public void testSetPropValueByPropInfo() {
        ParserUtil.BeanInfo otherBeanInfo = ParserUtil.getBeanInfo(OtherTestBean.class);
        ParserUtil.PropInfo otherPropInfo = otherBeanInfo.getPropInfo("sharedField");

        beanInfo.setPropValue(testBean, otherPropInfo, "SharedValue");
        Assertions.assertEquals("SharedValue", testBean.getSharedField());
    }

    @Test
    public void testGetPropInfoQueue() {
        List<ParserUtil.PropInfo> queue = beanInfo.getPropInfoChain("nestedBean.nestedValue");
        Assertions.assertEquals(2, queue.size());
        Assertions.assertEquals("nestedBean", queue.get(0).name);
        Assertions.assertEquals("nestedValue", queue.get(1).name);

        List<ParserUtil.PropInfo> simpleQueue = beanInfo.getPropInfoChain("id");
        Assertions.assertEquals(0, simpleQueue.size());
    }

    @Test
    public void testReadPropInfo() {
        char[] buffer = "id".toCharArray();
        ParserUtil.PropInfo propInfo = beanInfo.readPropInfo(buffer, 0, buffer.length);
        Assertions.assertNotNull(propInfo);
        Assertions.assertEquals("id", propInfo.name);

        char[] nonExistentBuffer = "xyz".toCharArray();
        ParserUtil.PropInfo nonExistentProp = beanInfo.readPropInfo(nonExistentBuffer, 0, nonExistentBuffer.length);
        Assertions.assertNull(nonExistentProp);
    }

    @Test
    public void testSetPropValueByPropInfo_WithAliasFallback() {
        AliasOnlyBean target = new AliasOnlyBean();
        ParserUtil.PropInfo sourcePropInfo = beanInfo.getPropInfo("aliasedField");

        boolean result = ParserUtil.getBeanInfo(AliasOnlyBean.class).setPropValue(target, sourcePropInfo, "aliasValue", false);

        Assertions.assertTrue(result);
        Assertions.assertEquals("aliasValue", target.getAlias1());
    }

    @Test
    public void testSetPropValueByPropInfo_WithAliasFallbackIgnored() {
        UnmatchedBean target = new UnmatchedBean();
        ParserUtil.PropInfo sourcePropInfo = beanInfo.getPropInfo("aliasedField");

        boolean result = ParserUtil.getBeanInfo(UnmatchedBean.class).setPropValue(target, sourcePropInfo, "aliasValue", true);

        Assertions.assertFalse(result);
    }

    @Test
    public void testIsAnnotationPresent() {
        boolean hasJsonXmlConfig = beanInfo.isAnnotationPresent(JsonXmlConfig.class);
        Assertions.assertTrue(hasJsonXmlConfig);

        boolean hasTable = beanInfo.isAnnotationPresent(Table.class);
        Assertions.assertTrue(hasTable);

        boolean hasEntity = beanInfo.isAnnotationPresent(Entity.class);
        Assertions.assertFalse(hasEntity);
    }

    @Test
    public void testGetAnnotation() {
        JsonXmlConfig jsonXmlConfig = beanInfo.getAnnotation(JsonXmlConfig.class);
        Assertions.assertNotNull(jsonXmlConfig);
        Assertions.assertEquals(NamingPolicy.CAMEL_CASE, jsonXmlConfig.namingPolicy());

        Table table = beanInfo.getAnnotation(Table.class);
        Assertions.assertNotNull(table);
        Assertions.assertEquals("test_bean", table.value());

        Entity entity = beanInfo.getAnnotation(Entity.class);
        Assertions.assertNull(entity);
    }

    @Test
    public void testNewInstance() {
        TestBean newInstance = beanInfo.newInstance();
        Assertions.assertNotNull(newInstance);
        Assertions.assertNull(newInstance.getId());

        TestBean instanceWithArgs = beanInfo.newInstance(123L, "Name", null, null, null);
        Assertions.assertNotNull(instanceWithArgs);
    }

    @Test
    public void testCreateAndFinishBeanResult() {
        Object result = beanInfo.createBeanResult();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof TestBean);

        TestBean finished = beanInfo.finishBeanResult(result);
        Assertions.assertNotNull(finished);
        Assertions.assertSame(result, finished);
    }

    @Test
    public void testHashCodeAndEquals() {
        ParserUtil.BeanInfo anotherBeanInfo = ParserUtil.getBeanInfo(TestBean.class);

        Assertions.assertEquals(beanInfo.hashCode(), anotherBeanInfo.hashCode());

        Assertions.assertEquals(beanInfo, anotherBeanInfo);
        Assertions.assertEquals(beanInfo, beanInfo);
        Assertions.assertNotEquals(beanInfo, null);
        Assertions.assertNotEquals(beanInfo, new Object());

        ParserUtil.BeanInfo differentBeanInfo = ParserUtil.getBeanInfo(OtherTestBean.class);
        Assertions.assertNotEquals(beanInfo, differentBeanInfo);
    }

    @Test
    public void testToString() {
        String toString = beanInfo.toString();
        Assertions.assertEquals(TestBean.class.getCanonicalName(), toString);
    }

    @JsonXmlConfig(namingPolicy = NamingPolicy.CAMEL_CASE)
    @Table("test_bean")
    public static class TestBean {
        @Id
        private Long id;
        private String simpleName;
        @JsonXmlField(aliases = { "alias1", "alias2" })
        private String aliasedField;
        private String sharedField;
        private NestedBean nestedBean;

        public TestBean() {
        }

        public TestBean(Long id, String simpleName, String aliasedField, String sharedField, NestedBean nestedBean) {
            this.id = id;
            this.simpleName = simpleName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public void setSimpleName(String simpleName) {
            this.simpleName = simpleName;
        }

        public String getAliasedField() {
            return aliasedField;
        }

        public void setAliasedField(String aliasedField) {
            this.aliasedField = aliasedField;
        }

        public String getSharedField() {
            return sharedField;
        }

        public void setSharedField(String sharedField) {
            this.sharedField = sharedField;
        }

        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }
    }

    public static class NestedBean {
        private String nestedValue;

        public String getNestedValue() {
            return nestedValue;
        }

        public void setNestedValue(String nestedValue) {
            this.nestedValue = nestedValue;
        }
    }

    public static class OtherTestBean {
        private String sharedField;

        public String getSharedField() {
            return sharedField;
        }

        public void setSharedField(String sharedField) {
            this.sharedField = sharedField;
        }
    }

    public static class AliasOnlyBean {
        private String alias1;

        public String getAlias1() {
            return alias1;
        }

        public void setAlias1(String alias1) {
            this.alias1 = alias1;
        }
    }

    public static class UnmatchedBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // --- review fixes 2026-09-06 (P3-01): lookup-time caches are bounded ---

    public static class CacheBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class CacheBean2 extends CacheBean {
    }

    private static int cacheSize(ParserUtil.BeanInfo beanInfo, String fieldName) throws Exception {
        java.lang.reflect.Field field = ParserUtil.BeanInfo.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object cache = field.get(beanInfo);
        return cache instanceof java.util.Map ? ((java.util.Map<?, ?>) cache).size() : ((java.util.Collection<?>) cache).size();
    }

    @Test
    public void reviewFixes20260906_lookupCachesStayBoundedUnderFreshUnknownKeys() throws Exception {
        ParserUtil.BeanInfo cacheInfo = ParserUtil.getBeanInfo(CacheBean.class);
        final int initialBindings = cacheSize(cacheInfo, "propInfoMap");

        // junk_<i> differs from its normalizePropName spelling, so the recursive lookup runs too (two misses per key before the fix).
        for (int i = 0; i < 10_000; i++) {
            Assertions.assertEquals("a", N.fromJson("{\"name\": \"a\", \"junk_" + i + "\": 1}", CacheBean.class).getName());
        }

        for (int i = 0; i < 10_000; i++) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("name", "a");
            map.put("Junk_" + i, 1);
            Assertions.assertEquals("a", Beans.mapToBean(map, true, CacheBean.class).getName());
        }

        // Before the fix 20_000 distinct keys produced >= 20_000 propInfoMap entries and 10_000 chain entries.
        Assertions.assertTrue(cacheSize(cacheInfo, "propInfoMap") < 10_000, "propInfoMap=" + cacheSize(cacheInfo, "propInfoMap"));
        Assertions.assertTrue(cacheSize(cacheInfo, "propInfoQueueMap") < 10_000, "propInfoQueueMap=" + cacheSize(cacheInfo, "propInfoQueueMap"));
        Assertions.assertTrue(cacheSize(cacheInfo, "propInfoMap") >= initialBindings);

        java.lang.reflect.Field capField = ParserUtil.class.getDeclaredField("MAX_CACHED_PROP_NAMES");
        capField.setAccessible(true);
        final int cap = (Integer) capField.get(null);
        Assertions.assertTrue(cap > 0 && cap < 20_000, "cap=" + cap);
        Assertions.assertTrue(cacheSize(cacheInfo, "propInfoMap") <= cap, "propInfoMap=" + cacheSize(cacheInfo, "propInfoMap"));
        Assertions.assertTrue(cacheSize(cacheInfo, "missedPropNames") <= cap, "missedPropNames=" + cacheSize(cacheInfo, "missedPropNames"));
        Assertions.assertTrue(cacheSize(cacheInfo, "propInfoQueueMap") <= cap, "propInfoQueueMap=" + cacheSize(cacheInfo, "propInfoQueueMap"));

        // Known property still resolves once the caches are saturated: exact, fuzzy and char-buffer lookups.
        Assertions.assertEquals("name", cacheInfo.getPropInfo("name").name);
        Assertions.assertEquals("name", cacheInfo.getPropInfo("getName").name);
        Assertions.assertEquals("name", cacheInfo.getPropInfo("na_me").name);
        Assertions.assertEquals("name", cacheInfo.getPropInfo("NAME").name);
        Assertions.assertEquals("name", cacheInfo.readPropInfo("name".toCharArray(), 0, 4).name);
        Assertions.assertNull(cacheInfo.getPropInfo("junk_1"));
        Assertions.assertNull(cacheInfo.getPropInfo("junk_999999"));

        // Flat names are never chains and are not memoized; a real nested path still resolves.
        final int queueBefore = cacheSize(cacheInfo, "propInfoQueueMap");
        Assertions.assertTrue(cacheInfo.getPropInfoChain("flat_" + System.nanoTime()).isEmpty());
        Assertions.assertEquals(queueBefore, cacheSize(cacheInfo, "propInfoQueueMap"));
        Assertions.assertEquals(2, beanInfo.getPropInfoChain("nestedBean.nestedValue").size());

        // Strict mode still reports an unknown property.
        Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class,
                () -> N.fromJson("{\"zzz\": 1}", JsonDeserConfig.create().setIgnoreUnmatchedProperty(false), CacheBean.class));
    }

    @Test
    public void reviewFixes20260906_repeatedUnknownKeyIsServedFromTheNegativeCache() throws Exception {
        ParserUtil.BeanInfo cacheInfo = ParserUtil.getBeanInfo(CacheBean2.class);
        final int bindingsBefore = cacheSize(cacheInfo, "propInfoMap");

        Assertions.assertNull(cacheInfo.getPropInfo("nope_key"));
        Assertions.assertNull(cacheInfo.getPropInfo("nope_key"));
        Assertions.assertNull(cacheInfo.getPropInfo("nope_key"));

        // A miss never becomes a propInfoMap entry (it used to add Optional.empty markers there)...
        Assertions.assertEquals(bindingsBefore, cacheSize(cacheInfo, "propInfoMap"));
        // ...it lands in the separate negative cache: one marker (plus the normalized spelling the recursive lookup tried), never more.
        final int misses = cacheSize(cacheInfo, "missedPropNames");
        Assertions.assertTrue(misses >= 1 && misses <= 2, "missedPropNames=" + misses);
        Assertions.assertNull(cacheInfo.getPropInfo("nope_key"));
        Assertions.assertEquals(misses, cacheSize(cacheInfo, "missedPropNames"));

        // A cached miss does not poison a later fuzzy hit (the 2026-06-11 regression stays covered).
        Assertions.assertNull(cacheInfo.getPropInfo("GETNAME"));
        Assertions.assertEquals("name", cacheInfo.getPropInfo("getName").name);
    }

    // --- review fixes 2026-09-06 (P3-04): readPropInfo agrees with getPropInfo when two properties share a tag ---

    public static class TagCollision {
        private String userName;
        private String user_name;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUser_name() {
            return user_name;
        }

        public void setUser_name(String user_name) {
            this.user_name = user_name;
        }
    }

    public static class TagCollisionReversed {
        private String user_name;
        private String userName;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUser_name() {
            return user_name;
        }

        public void setUser_name(String user_name) {
            this.user_name = user_name;
        }
    }

    public static class TagCollisionColumn {
        @Column("USER_NAME")
        private String userName;
        private String user_name;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUser_name() {
            return user_name;
        }

        public void setUser_name(String user_name) {
            this.user_name = user_name;
        }
    }

    public static class NoTagCollision {
        private String userName;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Object> asMap(Object bean) {
        return N.fromJson(N.toJson(bean), java.util.Map.class);
    }

    @Test
    public void reviewFixes20260906_readPropInfoAgreesWithGetPropInfoOnSharedNameTags() {
        for (Class<?> cls : new Class<?>[] { TagCollision.class, TagCollisionReversed.class, TagCollisionColumn.class, NoTagCollision.class }) {
            ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(cls);

            for (String key : new String[] { "USER_NAME", "user_name", "userName", "UserName", "USERNAME" }) {
                Assertions.assertSame(info.getPropInfo(key), info.readPropInfo(key.toCharArray(), 0, key.length()), cls.getSimpleName() + " / " + key);
            }

            // Offset into a larger buffer (the parser's usual call shape).
            char[] padded = "xxUSER_NAMEyy".toCharArray();
            Assertions.assertSame(info.getPropInfo("USER_NAME"), info.readPropInfo(padded, 2, 11), cls.getSimpleName());
        }

        // The explicit binding (@Column / declaration order) is the tie-breaker, on every entry point.
        Assertions.assertEquals("userName", ParserUtil.getBeanInfo(TagCollision.class).readPropInfo("USER_NAME".toCharArray(), 0, 9).name);
        Assertions.assertEquals("userName", ParserUtil.getBeanInfo(TagCollisionColumn.class).readPropInfo("USER_NAME".toCharArray(), 0, 9).name);
        Assertions.assertEquals("user_name", ParserUtil.getBeanInfo(TagCollisionReversed.class).readPropInfo("USER_NAME".toCharArray(), 0, 9).name);

        String doc = "{\"USER_NAME\": \"a\", \"user_name\": \"b\"}";
        java.util.Map<String, Object> source = new java.util.LinkedHashMap<>();
        source.put("USER_NAME", "a");
        source.put("user_name", "b");
        String xml = "<bean><USER_NAME>a</USER_NAME><user_name>b</user_name></bean>";

        for (Class<?> cls : new Class<?>[] { TagCollision.class, TagCollisionReversed.class, TagCollisionColumn.class }) {
            java.util.Map<String, Object> fromJson = asMap(N.fromJson(doc, cls));
            Assertions.assertEquals(fromJson, asMap(Beans.mapToBean(source, true, cls)), cls.getSimpleName());
            Assertions.assertEquals(fromJson, asMap(N.fromXml(xml, cls)), cls.getSimpleName());
            Assertions.assertEquals(fromJson, asMap(ParserFactory.createAbacusXmlParser().deserialize(xml, cls)), cls.getSimpleName());
        }

        // Both values survive when the explicit binding routes USER_NAME to userName (JSON used to drop "a").
        Assertions.assertEquals(java.util.Map.of("userName", "a", "user_name", "b"), asMap(N.fromJson(doc, TagCollision.class)));
        Assertions.assertEquals(java.util.Map.of("userName", "a", "user_name", "b"), asMap(N.fromJson(doc, TagCollisionColumn.class)));

        // A bean without a collision still takes the tag shortcut.
        ParserUtil.BeanInfo plain = ParserUtil.getBeanInfo(NoTagCollision.class);
        Assertions.assertEquals("userName", plain.readPropInfo("user_name".toCharArray(), 0, 9).name);
        Assertions.assertEquals("userName", plain.readPropInfo("USER_NAME".toCharArray(), 0, 9).name);
        Assertions.assertNull(plain.readPropInfo("nobody".toCharArray(), 0, 6));
    }

    // --- review fixes 2026-09-06 (P3-05): alias resolution does not depend on field declaration order ---

    public static class AliasVsTagA {
        private String firstName;
        @JsonXmlField(aliases = { "first_name" })
        private String surname;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }
    }

    public static class AliasVsTagB {
        @JsonXmlField(aliases = { "first_name" })
        private String surname;
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }
    }

    public static class AliasVsNameA {
        @JsonXmlField(aliases = { "surname" })
        private String x;
        private String surname;

        public String getX() {
            return x;
        }

        public void setX(String x) {
            this.x = x;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }
    }

    public static class AliasVsNameB {
        private String surname;
        @JsonXmlField(aliases = { "surname" })
        private String x;

        public String getX() {
            return x;
        }

        public void setX(String x) {
            this.x = x;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }
    }

    public static class AliasVsColumnA {
        @Column("NICK")
        private String nick;
        @JsonXmlField(aliases = { "NICK" })
        private String other;

        public String getNick() {
            return nick;
        }

        public void setNick(String nick) {
            this.nick = nick;
        }

        public String getOther() {
            return other;
        }

        public void setOther(String other) {
            this.other = other;
        }
    }

    public static class AliasVsColumnB {
        @JsonXmlField(aliases = { "NICK" })
        private String other;
        @Column("NICK")
        private String nick;

        public String getNick() {
            return nick;
        }

        public void setNick(String nick) {
            this.nick = nick;
        }

        public String getOther() {
            return other;
        }

        public void setOther(String other) {
            this.other = other;
        }
    }

    public static class SelfAlias {
        @JsonXmlField(aliases = { "first_name", "FIRST_NAME" })
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
    }

    @Test
    public void reviewFixes20260906_aliasResolutionIsDeclarationOrderIndependent() {
        // Explicit alias beats another property's derived naming-policy spelling, in BOTH orders (order A used to throw).
        for (Class<?> cls : new Class<?>[] { AliasVsTagA.class, AliasVsTagB.class }) {
            ParserUtil.BeanInfo info = Assertions.assertDoesNotThrow(() -> ParserUtil.getBeanInfo(cls), cls.getSimpleName());
            Assertions.assertEquals("surname", info.getPropInfo("first_name").name, cls.getSimpleName());
            Assertions.assertSame(info.getPropInfo("first_name"), info.readPropInfo("first_name".toCharArray(), 0, 10), cls.getSimpleName());
            // firstName keeps its own name and its other derived spellings.
            Assertions.assertEquals("firstName", info.getPropInfo("firstName").name, cls.getSimpleName());
            Assertions.assertEquals("firstName", info.getPropInfo("FIRST_NAME").name, cls.getSimpleName());

            java.util.Map<String, Object> values = asMap(N.fromJson("{\"first_name\": \"X\", \"firstName\": \"F\"}", cls));
            Assertions.assertEquals(java.util.Map.of("surname", "X", "firstName", "F"), values, cls.getSimpleName());
        }

        // An alias restating another property's own name or @Column name is an error in BOTH orders
        // (one order used to be silently accepted with the alias dead or the column shadowed).
        for (Class<?> cls : new Class<?>[] { AliasVsNameA.class, AliasVsNameB.class, AliasVsColumnA.class, AliasVsColumnB.class }) {
            IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> ParserUtil.getBeanInfo(cls), cls.getSimpleName());
            Assertions.assertTrue(e.getMessage().contains("Cannot set alias"), e.getMessage());
            Assertions.assertTrue(e.getMessage().contains("already the name, column name or alias of property"), e.getMessage());
        }

        // An alias restating the property's OWN spellings is still accepted.
        ParserUtil.BeanInfo self = ParserUtil.getBeanInfo(SelfAlias.class);
        Assertions.assertEquals("firstName", self.getPropInfo("first_name").name);
        Assertions.assertEquals("firstName", self.getPropInfo("FIRST_NAME").name);
    }

    // ---- perf review 2026-09-26 G009 begin ----

    public static class SameLengthNames {
        private String email;
        private String phone;
        private String alpha;
        private int id;
        private String address;
        private String userName;
        @JsonXmlField(name = "Aa")
        private String x1;
        @JsonXmlField(name = "BB")
        private String x2;
        @Column("ADDR_LINE")
        private String line;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAlpha() {
            return alpha;
        }

        public void setAlpha(String alpha) {
            this.alpha = alpha;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getX1() {
            return x1;
        }

        public void setX1(String x1) {
            this.x1 = x1;
        }

        public String getX2() {
            return x2;
        }

        public void setX2(String x2) {
            this.x2 = x2;
        }

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }
    }

    // G009-01: readPropInfo finds same-length names ("email"/"phone"/"alpha", and "Aa"/"BB", whose hash codes collide)
    // without the boxed hash map; every key, known or not, must resolve exactly as before.
    @Test
    public void testReadPropInfo_sameLengthNamesAndHashCollisions() {
        ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(SameLengthNames.class);
        Assertions.assertEquals("Aa".hashCode(), "BB".hashCode());

        String[][] expected = { { "email", "email" }, { "phone", "phone" }, { "alpha", "alpha" }, { "id", "id" }, { "address", "address" },
                { "userName", "userName" }, { "user_name", "userName" }, { "USER_NAME", "userName" }, { "Email", "email" }, { "EMAIL", "email" },
                { "Aa", "x1" }, { "BB", "x2" }, { "x1", "x1" }, { "x2", "x2" }, { "line", "line" }, { "ADDR_LINE", "line" }, { "addr_line", "line" },
                { "emaiL", null }, { "zz", null }, { "AB", null }, { "C#", null }, { "a", null }, { "averyveryverylongunknownkey", null },
                { "phonE", null }, { "address1", null } };

        for (String[] pair : expected) {
            String key = pair[0];
            ParserUtil.PropInfo direct = info.readPropInfo(key.toCharArray(), 0, key.length());
            char[] padded = ("#" + key + "##").toCharArray();
            ParserUtil.PropInfo offset = info.readPropInfo(padded, 1, 1 + key.length());

            if (pair[1] == null) {
                Assertions.assertNull(direct, key);
                Assertions.assertNull(offset, key);
            } else {
                Assertions.assertEquals(pair[1], direct.name, key);
                Assertions.assertSame(direct, offset, key);
                Assertions.assertSame(info.getPropInfo(pair[1]), direct, key);
            }
        }

        // Once getPropInfo has memoized its (tolerant) answers, readPropInfo returns exactly those for the same text.
        for (String[] pair : expected) {
            String key = pair[0];
            ParserUtil.PropInfo resolved = info.getPropInfo(key);
            Assertions.assertSame(resolved, info.readPropInfo(key.toCharArray(), 0, key.length()), key);
            Assertions.assertSame(resolved, info.readPropInfo(("##" + key + "#").toCharArray(), 2, 2 + key.length()), key);
        }

        Assertions.assertEquals("email", info.readPropInfo("emaiL".toCharArray(), 0, 5).name);
        Assertions.assertNull(info.readPropInfo("zz".toCharArray(), 0, 2));

        Assertions.assertNull(info.readPropInfo("email".toCharArray(), 2, 2));

        SameLengthNames bean = N.fromJson("{\"Aa\": \"1\", \"BB\": \"2\", \"email\": \"e\", \"phone\": \"p\", \"alpha\": \"a\", \"ADDR_LINE\": \"l\", \"id\": 7}",
                SameLengthNames.class);
        Assertions.assertEquals("1", bean.getX1());
        Assertions.assertEquals("2", bean.getX2());
        Assertions.assertEquals("e", bean.getEmail());
        Assertions.assertEquals("p", bean.getPhone());
        Assertions.assertEquals("a", bean.getAlpha());
        Assertions.assertEquals("l", bean.getLine());
        Assertions.assertEquals(7, bean.getId());
    }

    // ---- perf review 2026-09-26 G009 end ----

    public static class WideBean {
        public int field000, field001, field002, field003, field004, field005, field006, field007, field008, field009, field010, field011, field012, field013, field014, field015;
        public int field016, field017, field018, field019, field020, field021, field022, field023, field024, field025, field026, field027, field028, field029, field030, field031;
        public int field032, field033, field034, field035, field036, field037, field038, field039, field040, field041, field042, field043, field044, field045, field046, field047;
        public int field048, field049, field050, field051, field052, field053, field054, field055, field056, field057, field058, field059, field060, field061, field062, field063;
        public int field064, field065, field066, field067, field068, field069, field070, field071, field072, field073, field074, field075, field076, field077, field078, field079;
        public int field080, field081, field082, field083, field084, field085, field086, field087, field088, field089, field090, field091, field092, field093, field094, field095;
        public int field096, field097, field098, field099, field100, field101, field102, field103, field104, field105, field106, field107, field108, field109, field110, field111;
        public int field112, field113, field114, field115, field116, field117, field118, field119, field120, field121, field122, field123, field124, field125, field126, field127;
    }

    @Test
    public void testReadPropInfo_manyEqualLengthNames() throws Exception {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(WideBean.class);
        Assertions.assertEquals(128, info.propInfoList.size());
        final StringBuilder json = new StringBuilder("{");
        for (int i = 127; i >= 0; i--) {
            final String name = String.format(java.util.Locale.ROOT, "field%03d", i);
            final char[] key = ("##" + name + "##").toCharArray();
            Assertions.assertSame(info.getPropInfo(name), info.readPropInfo(key, 2, key.length - 2));
            if (i < 127) {
                json.append(',');
            }
            json.append('"').append(name).append("\":").append(i);
        }
        json.append('}');
        final WideBean bean = N.fromJson(json.toString(), WideBean.class);
        for (int i = 0; i < 128; i++) {
            Assertions.assertEquals(i, WideBean.class.getField(String.format(java.util.Locale.ROOT, "field%03d", i)).getInt(bean));
        }
        Assertions.assertNull(info.readPropInfo("field128".toCharArray(), 0, 8));
        Assertions.assertNull(info.readPropInfo("field".toCharArray(), 0, 5));
    }
}
