package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class NamingPolicy2025Test extends TestBase {

    @Test
    public void test_lowerCamelCase_convert_withUnderscores() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("user_name"));
        assertEquals("userId", NamingPolicy.LOWER_CAMEL_CASE.convert("user_id"));
        assertEquals("firstName", NamingPolicy.LOWER_CAMEL_CASE.convert("first_name"));
    }

    @Test
    public void test_lowerCamelCase_convert_withHyphens() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("user-name"));
        assertEquals("userId", NamingPolicy.LOWER_CAMEL_CASE.convert("user-id"));
        assertEquals("firstName", NamingPolicy.LOWER_CAMEL_CASE.convert("first-name"));
    }

    @Test
    public void test_lowerCamelCase_convert_withUpperCase() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("USER_NAME"));
        assertEquals("userId", NamingPolicy.LOWER_CAMEL_CASE.convert("USER_ID"));
    }

    @Test
    public void test_lowerCamelCase_convert_withSpaces() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("user name"));
        assertEquals("userId", NamingPolicy.LOWER_CAMEL_CASE.convert("user id"));
    }

    @Test
    public void test_lowerCamelCase_convert_withMixedCase() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("UserName"));
        assertEquals("userId", NamingPolicy.LOWER_CAMEL_CASE.convert("UserId"));
    }

    @Test
    public void test_lowerCamelCase_convert_alreadyCamelCase() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("userName"));
        assertEquals("userId", NamingPolicy.LOWER_CAMEL_CASE.convert("userId"));
    }

    @Test
    public void test_lowerCamelCase_convert_emptyString() {
        assertEquals("", NamingPolicy.LOWER_CAMEL_CASE.convert(""));
    }

    @Test
    public void test_lowerCamelCase_convert_singleWord() {
        assertEquals("user", NamingPolicy.LOWER_CAMEL_CASE.convert("user"));
        assertEquals("user", NamingPolicy.LOWER_CAMEL_CASE.convert("USER"));
    }

    @Test
    public void test_upperCamelCase_convert_withUnderscores() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("user_name"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("user_id"));
        assertEquals("FirstName", NamingPolicy.UPPER_CAMEL_CASE.convert("first_name"));
    }

    @Test
    public void test_upperCamelCase_convert_withHyphens() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("user-name"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("user-id"));
        assertEquals("FirstName", NamingPolicy.UPPER_CAMEL_CASE.convert("first-name"));
    }

    @Test
    public void test_upperCamelCase_convert_withUpperCase() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("USER_NAME"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("USER_ID"));
    }

    @Test
    public void test_upperCamelCase_convert_withSpaces() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("user name"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("user id"));
    }

    @Test
    public void test_upperCamelCase_convert_withLowerCamelCase() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("userName"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("userId"));
    }

    @Test
    public void test_upperCamelCase_convert_alreadyPascalCase() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("UserName"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("UserId"));
    }

    @Test
    public void test_upperCamelCase_convert_emptyString() {
        assertEquals("", NamingPolicy.UPPER_CAMEL_CASE.convert(""));
    }

    @Test
    public void test_upperCamelCase_convert_singleWord() {
        assertEquals("User", NamingPolicy.UPPER_CAMEL_CASE.convert("user"));
        assertEquals("USER", NamingPolicy.UPPER_CAMEL_CASE.convert("USER"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_withCamelCase() {
        assertEquals("user_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("userName"));
        assertEquals("user_id", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("userId"));
        assertEquals("first_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("firstName"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_withPascalCase() {
        assertEquals("user_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("UserName"));
        assertEquals("user_id", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("UserId"));
        assertEquals("first_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("FirstName"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_withHyphens() {
        assertEquals("user-name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user-name"));
        assertEquals("user-id", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user-id"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_withSpaces() {
        assertEquals("user name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user name"));
        assertEquals("user id", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user id"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_withUpperCase() {
        assertEquals("user_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("USER_NAME"));
        assertEquals("user_id", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("USER_ID"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_alreadySnakeCase() {
        assertEquals("user_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user_name"));
        assertEquals("user_id", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user_id"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_emptyString() {
        assertEquals("", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert(""));
    }

    @Test
    public void test_lowerCaseWithUnderscore_convert_singleWord() {
        assertEquals("user", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user"));
        assertEquals("user", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("USER"));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_withCamelCase() {
        assertEquals("USER_NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("userName"));
        assertEquals("USER_ID", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("userId"));
        assertEquals("FIRST_NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("firstName"));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_withPascalCase() {
        assertEquals("USER_NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("UserName"));
        assertEquals("USER_ID", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("UserId"));
        assertEquals("FIRST_NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("FirstName"));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_withHyphens() {
        assertEquals("USER-NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user-name"));
        assertEquals("USER-ID", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user-id"));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_withSpaces() {
        assertEquals("USER NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user name"));
        assertEquals("USER ID", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user id"));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_withLowerCaseUnderscore() {
        assertEquals("USER_NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user_name"));
        assertEquals("USER_ID", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user_id"));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_alreadyUpperSnakeCase() {
        assertEquals("USER_NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("USER_NAME"));
        assertEquals("USER_ID", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("USER_ID"));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_emptyString() {
        assertEquals("", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert(""));
    }

    @Test
    public void test_upperCaseWithUnderscore_convert_singleWord() {
        assertEquals("USER", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user"));
        assertEquals("USER", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("USER"));
    }

    @Test
    public void test_noChange_convert_withVariousInputs() {
        assertEquals("userName", NamingPolicy.NO_CHANGE.convert("userName"));
        assertEquals("user_name", NamingPolicy.NO_CHANGE.convert("user_name"));
        assertEquals("USER_NAME", NamingPolicy.NO_CHANGE.convert("USER_NAME"));
        assertEquals("user-name", NamingPolicy.NO_CHANGE.convert("user-name"));
        assertEquals("UserName", NamingPolicy.NO_CHANGE.convert("UserName"));
    }

    @Test
    public void test_noChange_convert_emptyString() {
        assertEquals("", NamingPolicy.NO_CHANGE.convert(""));
    }

    @Test
    public void test_noChange_convert_withSpecialCharacters() {
        assertEquals("user@name#123", NamingPolicy.NO_CHANGE.convert("user@name#123"));
        assertEquals("any-String_123", NamingPolicy.NO_CHANGE.convert("any-String_123"));
    }

    @Test
    public void test_lowerCamelCase_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.LOWER_CAMEL_CASE.func();
        assertNotNull(converter);
        assertEquals("userName", converter.apply("user_name"));
    }

    @Test
    public void test_upperCamelCase_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.UPPER_CAMEL_CASE.func();
        assertNotNull(converter);
        assertEquals("UserName", converter.apply("user_name"));
    }

    @Test
    public void test_lowerCaseWithUnderscore_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.func();
        assertNotNull(converter);
        assertEquals("user_name", converter.apply("userName"));
    }

    @Test
    public void test_upperCaseWithUnderscore_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.func();
        assertNotNull(converter);
        assertEquals("USER_NAME", converter.apply("userName"));
    }

    @Test
    public void test_noChange_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.NO_CHANGE.func();
        assertNotNull(converter);
        assertEquals("userName", converter.apply("userName"));
    }

    @Test
    public void test_func_returnsSameInstanceOnMultipleCalls() {
        Function<String, String> converter1 = NamingPolicy.LOWER_CAMEL_CASE.func();
        Function<String, String> converter2 = NamingPolicy.LOWER_CAMEL_CASE.func();
        assertSame(converter1, converter2);
    }

    @Test
    public void test_convert_withMultipleConsecutiveUnderscores() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("user__name"));
        assertEquals("user__name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user__name"));
    }

    @Test
    public void test_convert_withMultipleConsecutiveHyphens() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("user--name"));
        assertEquals("user--name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user--name"));
    }

    @Test
    public void test_convert_withLeadingUnderscore() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("_user_name"));
        assertEquals("_user_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("_userName"));
    }

    @Test
    public void test_convert_withTrailingUnderscore() {
        assertEquals("userName", NamingPolicy.LOWER_CAMEL_CASE.convert("user_name_"));
        assertEquals("user_name_", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("userName_"));
    }

    @Test
    public void test_convert_withNumbers() {
        assertEquals("user1Name", NamingPolicy.LOWER_CAMEL_CASE.convert("user1_name"));
        assertEquals("user1_name", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("user1Name"));
        assertEquals("USER1_NAME", NamingPolicy.UPPER_CASE_WITH_UNDERSCORE.convert("user1Name"));
    }

    @Test
    public void test_convert_withAcronyms() {
        assertEquals("httpUrl", NamingPolicy.LOWER_CAMEL_CASE.convert("HTTP_URL"));
        assertEquals("http_url", NamingPolicy.LOWER_CASE_WITH_UNDERSCORE.convert("HTTPUrl"));
    }

    @Test
    public void test_convert_complexMixedFormat() {
        String input = "user-Name_ID";
        assertEquals("usernameid", NamingPolicy.LOWER_CAMEL_CASE.convert(input).toLowerCase());
    }

    @Test
    public void test_enumValues_allPresent() {
        NamingPolicy[] values = NamingPolicy.values();
        assertEquals(5, values.length);

        assertNotNull(NamingPolicy.LOWER_CAMEL_CASE);
        assertNotNull(NamingPolicy.UPPER_CAMEL_CASE);
        assertNotNull(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertNotNull(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE);
        assertNotNull(NamingPolicy.NO_CHANGE);
    }

    @Test
    public void test_valueOf_withValidName() {
        assertEquals(NamingPolicy.LOWER_CAMEL_CASE, NamingPolicy.valueOf("LOWER_CAMEL_CASE"));
        assertEquals(NamingPolicy.UPPER_CAMEL_CASE, NamingPolicy.valueOf("UPPER_CAMEL_CASE"));
        assertEquals(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, NamingPolicy.valueOf("LOWER_CASE_WITH_UNDERSCORE"));
        assertEquals(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, NamingPolicy.valueOf("UPPER_CASE_WITH_UNDERSCORE"));
        assertEquals(NamingPolicy.NO_CHANGE, NamingPolicy.valueOf("NO_CHANGE"));
    }
}
