package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;

import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.entity.extendDirty.basic.AclUser;

public class NamingPolicyTest extends AbstractParserTest {

    @Test
    public void testCamelCaseConvert() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user_name"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("user_id"));
        assertEquals("firstName", NamingPolicy.CAMEL_CASE.convert("first_name"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user-name"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("user-id"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("USER_NAME"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user name"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("UserName"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("userName"));
        assertEquals("", NamingPolicy.CAMEL_CASE.convert(""));
        assertEquals("user", NamingPolicy.CAMEL_CASE.convert("user"));
        assertEquals("user", NamingPolicy.CAMEL_CASE.convert("USER"));
    }

    @Test
    public void testUpperCamelCaseConvert() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("user_name"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("user_id"));
        assertEquals("FirstName", NamingPolicy.UPPER_CAMEL_CASE.convert("first_name"));
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("user-name"));
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("USER_NAME"));
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("user name"));
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("userName"));
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("UserName"));
        assertEquals("", NamingPolicy.UPPER_CAMEL_CASE.convert(""));
        assertEquals("User", NamingPolicy.UPPER_CAMEL_CASE.convert("user"));
        assertEquals("User", NamingPolicy.UPPER_CAMEL_CASE.convert("USER"));
    }

    @Test
    public void testSnakeCaseConvert() {
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("userName"));
        assertEquals("user_id", NamingPolicy.SNAKE_CASE.convert("userId"));
        assertEquals("first_name", NamingPolicy.SNAKE_CASE.convert("firstName"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("UserName"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("user-name"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("user name"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("USER_NAME"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("user_name"));
        assertEquals("", NamingPolicy.SNAKE_CASE.convert(""));
        assertEquals("user", NamingPolicy.SNAKE_CASE.convert("user"));
        assertEquals("user", NamingPolicy.SNAKE_CASE.convert("USER"));
    }

    @Test
    public void testScreamingSnakeCaseConvert() {
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("userName"));
        assertEquals("USER_ID", NamingPolicy.SCREAMING_SNAKE_CASE.convert("userId"));
        assertEquals("FIRST_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("firstName"));
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("UserName"));
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user-name"));
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user name"));
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user_name"));
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("USER_NAME"));
        assertEquals("", NamingPolicy.SCREAMING_SNAKE_CASE.convert(""));
        assertEquals("USER", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user"));
        assertEquals("USER", NamingPolicy.SCREAMING_SNAKE_CASE.convert("USER"));
    }

    @Test
    public void testKebabCaseConvert() {
        assertEquals("user-name", NamingPolicy.KEBAB_CASE.convert("userName"));
        assertEquals("user-id", NamingPolicy.KEBAB_CASE.convert("userId"));
        assertEquals("first-name", NamingPolicy.KEBAB_CASE.convert("firstName"));
        assertEquals("user-name", NamingPolicy.KEBAB_CASE.convert("UserName"));
        assertEquals("user-name", NamingPolicy.KEBAB_CASE.convert("user_name"));
        assertEquals("user-name", NamingPolicy.KEBAB_CASE.convert("user-name"));
        assertEquals("", NamingPolicy.KEBAB_CASE.convert(""));
        assertEquals("user", NamingPolicy.KEBAB_CASE.convert("user"));
        assertEquals("user", NamingPolicy.KEBAB_CASE.convert("USER"));
    }

    @Test
    public void testNoChangeConvert() {
        assertEquals("userName", NamingPolicy.NO_CHANGE.convert("userName"));
        assertEquals("user_name", NamingPolicy.NO_CHANGE.convert("user_name"));
        assertEquals("USER_NAME", NamingPolicy.NO_CHANGE.convert("USER_NAME"));
        assertEquals("user-name", NamingPolicy.NO_CHANGE.convert("user-name"));
        assertEquals("UserName", NamingPolicy.NO_CHANGE.convert("UserName"));
        assertEquals("", NamingPolicy.NO_CHANGE.convert(""));
        assertEquals("user@name#123", NamingPolicy.NO_CHANGE.convert("user@name#123"));
        assertEquals("any-String_123", NamingPolicy.NO_CHANGE.convert("any-String_123"));
    }

    @Test
    public void testConvert_EdgeCase() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user__name"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("user__name"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user--name"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("user--name"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("_user_name"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("_userName"));
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user_name_"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("userName_"));
        assertEquals("user1Name", NamingPolicy.CAMEL_CASE.convert("user1_name"));
        assertEquals("user1_name", NamingPolicy.SNAKE_CASE.convert("user1Name"));
        assertEquals("USER1_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user1Name"));
        assertEquals("httpUrl", NamingPolicy.CAMEL_CASE.convert("HTTP_URL"));
        assertEquals("http_url", NamingPolicy.SNAKE_CASE.convert("HTTPUrl"));
        assertEquals("usernameid", NamingPolicy.CAMEL_CASE.convert("user-Name_ID").toLowerCase());
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("-user-name-"));
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("-user-name-"));

        for (NamingPolicy p : NamingPolicy.values()) {
            assertNull(p.convert(null), "policy=" + p);
            assertNotNull(p.convert("___"));
            assertNotNull(p.convert("---"));
        }
        assertEquals("a", NamingPolicy.CAMEL_CASE.convert("a"));
        assertEquals("a", NamingPolicy.CAMEL_CASE.convert("A"));
        assertEquals("A", NamingPolicy.UPPER_CAMEL_CASE.convert("a"));
        assertEquals("A", NamingPolicy.UPPER_CAMEL_CASE.convert("A"));
        assertEquals("a", NamingPolicy.SNAKE_CASE.convert("A"));
        assertEquals("A", NamingPolicy.SCREAMING_SNAKE_CASE.convert("a"));
        assertEquals("a", NamingPolicy.KEBAB_CASE.convert("A"));
        assertEquals("A", NamingPolicy.NO_CHANGE.convert("A"));
        assertNotNull(NamingPolicy.CAMEL_CASE.convert("café_au_lait"));
        assertNotNull(NamingPolicy.SNAKE_CASE.convert("caféAuLait"));
        assertNotNull(NamingPolicy.KEBAB_CASE.convert("caféAuLait"));
    }

    @Test
    public void testAsFunction() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.asFunction().apply("user_name"));
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.asFunction().apply("user_name"));
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.asFunction().apply("userName"));
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.asFunction().apply("userName"));
        assertEquals("user-name", NamingPolicy.KEBAB_CASE.asFunction().apply("userName"));
        assertEquals("userName", NamingPolicy.NO_CHANGE.asFunction().apply("userName"));
        Function<String, String> converter = NamingPolicy.CAMEL_CASE.asFunction();
        assertNotNull(converter);
        assertSame(converter, NamingPolicy.CAMEL_CASE.asFunction());
    }

    @Test
    public void testBeanMapRoundTrip() {
        Account account = createAccountWithContact(Account.class);
        for (NamingPolicy policy : new NamingPolicy[] { NamingPolicy.CAMEL_CASE, NamingPolicy.SNAKE_CASE, NamingPolicy.SCREAMING_SNAKE_CASE }) {
            Map<String, Object> props = Beans.beanToMap(account, true, null, policy);
            assertNotNull(props);
            assertFalse(props.isEmpty());
            assertNotNull(Beans.mapToBean(props, Account.class));

            props = Beans.deepBeanToMap(account, true, null, policy);
            assertNotNull(props);
            assertFalse(props.isEmpty());
            assertNotNull(Beans.mapToBean(props, Account.class));

            props = Beans.beanToFlatMap(account, true, null, policy);
            assertNotNull(props);
            assertFalse(props.isEmpty());
            assertNotNull(Beans.mapToBean(props, Account.class));
        }

        AclUser user = createAclUserWithAclGroup(AclUser.class);
        for (NamingPolicy policy : new NamingPolicy[] { NamingPolicy.CAMEL_CASE, NamingPolicy.SNAKE_CASE, NamingPolicy.SCREAMING_SNAKE_CASE }) {
            Map<String, Object> props = Beans.beanToMap(user, true, null, policy);
            assertNotNull(props);
            assertFalse(props.isEmpty());
            assertNotNull(Beans.mapToBean(props, AclUser.class));

            props = Beans.deepBeanToMap(user, true, null, policy);
            assertNotNull(props);
            assertFalse(props.isEmpty());
            assertNotNull(Beans.mapToBean(props, AclUser.class));

            props = Beans.beanToFlatMap(user, true, null, policy);
            assertNotNull(props);
            assertFalse(props.isEmpty());
            assertNotNull(Beans.mapToBean(props, AclUser.class));
        }
    }

    @Test
    public void testValuesAndValueOf() {
        NamingPolicy[] values = NamingPolicy.values();
        assertEquals(6, values.length);
        assertEquals(NamingPolicy.CAMEL_CASE, NamingPolicy.valueOf("CAMEL_CASE"));
        assertEquals(NamingPolicy.UPPER_CAMEL_CASE, NamingPolicy.valueOf("UPPER_CAMEL_CASE"));
        assertEquals(NamingPolicy.SNAKE_CASE, NamingPolicy.valueOf("SNAKE_CASE"));
        assertEquals(NamingPolicy.SCREAMING_SNAKE_CASE, NamingPolicy.valueOf("SCREAMING_SNAKE_CASE"));
        assertEquals(NamingPolicy.KEBAB_CASE, NamingPolicy.valueOf("KEBAB_CASE"));
        assertEquals(NamingPolicy.NO_CHANGE, NamingPolicy.valueOf("NO_CHANGE"));
    }

    /**
     * G54-004: the camel-case policies are not idempotent - re-applying one to its own output changes
     * it, because the separator that created the word boundary is gone. The other four are idempotent.
     */
    @Test
    public void testCamelCasePoliciesAreNotIdempotent() {
        assertEquals("AB", NamingPolicy.UPPER_CAMEL_CASE.convert("a__b"));
        assertEquals("Ab", NamingPolicy.UPPER_CAMEL_CASE.convert("AB"));
        assertEquals("a9ABb", NamingPolicy.CAMEL_CASE.convert("a9_ABb"));
        assertEquals("a9aBb", NamingPolicy.CAMEL_CASE.convert("a9ABb"));

        final String[] samples = { "a__b", "a9_ABb", "_first__name_", "userName", "XMLParser", "-a-", " -hello- ", "" };
        final NamingPolicy[] idempotent = { NamingPolicy.SNAKE_CASE, NamingPolicy.SCREAMING_SNAKE_CASE, NamingPolicy.KEBAB_CASE,
                NamingPolicy.NO_CHANGE };

        for (final String s : samples) {
            for (final NamingPolicy p : idempotent) {
                final String once = p.convert(s);
                assertEquals(once, p.convert(once), p + " must be idempotent for [" + s + "]");
            }
        }
    }
}
