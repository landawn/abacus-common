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
    public void test_camelCase_convert_withUnderscores() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user_name"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("user_id"));
        assertEquals("firstName", NamingPolicy.CAMEL_CASE.convert("first_name"));
    }

    @Test
    public void test_camelCase_convert_withHyphens() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user-name"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("user-id"));
        assertEquals("firstName", NamingPolicy.CAMEL_CASE.convert("first-name"));
    }

    @Test
    public void test_camelCase_convert_withUpperCase() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("USER_NAME"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("USER_ID"));
    }

    @Test
    public void test_camelCase_convert_withSpaces() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user name"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("user id"));
    }

    @Test
    public void test_camelCase_convert_withMixedCase() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("UserName"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("UserId"));
    }

    @Test
    public void test_camelCase_convert_alreadyCamelCase() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("userName"));
        assertEquals("userId", NamingPolicy.CAMEL_CASE.convert("userId"));
    }

    @Test
    public void test_camelCase_convert_emptyString() {
        assertEquals("", NamingPolicy.CAMEL_CASE.convert(""));
    }

    @Test
    public void test_camelCase_convert_singleWord() {
        assertEquals("user", NamingPolicy.CAMEL_CASE.convert("user"));
        assertEquals("user", NamingPolicy.CAMEL_CASE.convert("USER"));
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
    public void test_upperCamelCase_convert_withCamelCase() {
        assertEquals("UserName", NamingPolicy.UPPER_CAMEL_CASE.convert("userName"));
        assertEquals("UserId", NamingPolicy.UPPER_CAMEL_CASE.convert("userId"));
    }

    @Test
    public void test_upperCamelCase_convert_alreadyUpperCamelCase() {
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
    public void test_snakeCase_convert_withCamelCase() {
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("userName"));
        assertEquals("user_id", NamingPolicy.SNAKE_CASE.convert("userId"));
        assertEquals("first_name", NamingPolicy.SNAKE_CASE.convert("firstName"));
    }

    @Test
    public void test_snakeCase_convert_withUpperCamelCase() {
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("UserName"));
        assertEquals("user_id", NamingPolicy.SNAKE_CASE.convert("UserId"));
        assertEquals("first_name", NamingPolicy.SNAKE_CASE.convert("FirstName"));
    }

    @Test
    public void test_snakeCase_convert_withHyphens() {
        assertEquals("user-name", NamingPolicy.SNAKE_CASE.convert("user-name"));
        assertEquals("user-id", NamingPolicy.SNAKE_CASE.convert("user-id"));
    }

    @Test
    public void test_snakeCase_convert_withSpaces() {
        assertEquals("user name", NamingPolicy.SNAKE_CASE.convert("user name"));
        assertEquals("user id", NamingPolicy.SNAKE_CASE.convert("user id"));
    }

    @Test
    public void test_snakeCase_convert_withUpperCase() {
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("USER_NAME"));
        assertEquals("user_id", NamingPolicy.SNAKE_CASE.convert("USER_ID"));
    }

    @Test
    public void test_snakeCase_convert_alreadySnakeCase() {
        assertEquals("user_name", NamingPolicy.SNAKE_CASE.convert("user_name"));
        assertEquals("user_id", NamingPolicy.SNAKE_CASE.convert("user_id"));
    }

    @Test
    public void test_snakeCase_convert_emptyString() {
        assertEquals("", NamingPolicy.SNAKE_CASE.convert(""));
    }

    @Test
    public void test_snakeCase_convert_singleWord() {
        assertEquals("user", NamingPolicy.SNAKE_CASE.convert("user"));
        assertEquals("user", NamingPolicy.SNAKE_CASE.convert("USER"));
    }

    @Test
    public void test_screamingSnakeCase_convert_withCamelCase() {
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("userName"));
        assertEquals("USER_ID", NamingPolicy.SCREAMING_SNAKE_CASE.convert("userId"));
        assertEquals("FIRST_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("firstName"));
    }

    @Test
    public void test_screamingSnakeCase_convert_withUpperCamelCase() {
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("UserName"));
        assertEquals("USER_ID", NamingPolicy.SCREAMING_SNAKE_CASE.convert("UserId"));
        assertEquals("FIRST_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("FirstName"));
    }

    @Test
    public void test_screamingSnakeCase_convert_withHyphens() {
        assertEquals("USER-NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user-name"));
        assertEquals("USER-ID", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user-id"));
    }

    @Test
    public void test_screamingSnakeCase_convert_withSpaces() {
        assertEquals("USER NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user name"));
        assertEquals("USER ID", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user id"));
    }

    @Test
    public void test_screamingSnakeCase_convert_withLowerCaseUnderscore() {
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user_name"));
        assertEquals("USER_ID", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user_id"));
    }

    @Test
    public void test_screamingSnakeCase_convert_alreadyUpperSnakeCase() {
        assertEquals("USER_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("USER_NAME"));
        assertEquals("USER_ID", NamingPolicy.SCREAMING_SNAKE_CASE.convert("USER_ID"));
    }

    @Test
    public void test_screamingSnakeCase_convert_emptyString() {
        assertEquals("", NamingPolicy.SCREAMING_SNAKE_CASE.convert(""));
    }

    @Test
    public void test_screamingSnakeCase_convert_singleWord() {
        assertEquals("USER", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user"));
        assertEquals("USER", NamingPolicy.SCREAMING_SNAKE_CASE.convert("USER"));
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
    public void test_camelCase_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.CAMEL_CASE.asFunction();
        assertNotNull(converter);
        assertEquals("userName", converter.apply("user_name"));
    }

    @Test
    public void test_upperCamelCase_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.UPPER_CAMEL_CASE.asFunction();
        assertNotNull(converter);
        assertEquals("UserName", converter.apply("user_name"));
    }

    @Test
    public void test_snakeCase_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.SNAKE_CASE.asFunction();
        assertNotNull(converter);
        assertEquals("user_name", converter.apply("userName"));
    }

    @Test
    public void test_screamingSnakeCase_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.SCREAMING_SNAKE_CASE.asFunction();
        assertNotNull(converter);
        assertEquals("USER_NAME", converter.apply("userName"));
    }

    @Test
    public void test_noChange_func_returnsFunction() {
        Function<String, String> converter = NamingPolicy.NO_CHANGE.asFunction();
        assertNotNull(converter);
        assertEquals("userName", converter.apply("userName"));
    }

    @Test
    public void test_func_returnsSameInstanceOnMultipleCalls() {
        Function<String, String> converter1 = NamingPolicy.CAMEL_CASE.asFunction();
        Function<String, String> converter2 = NamingPolicy.CAMEL_CASE.asFunction();
        assertSame(converter1, converter2);
    }

    @Test
    public void test_convert_withMultipleConsecutiveUnderscores() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user__name"));
        assertEquals("user__name", NamingPolicy.SNAKE_CASE.convert("user__name"));
    }

    @Test
    public void test_convert_withMultipleConsecutiveHyphens() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user--name"));
        assertEquals("user--name", NamingPolicy.SNAKE_CASE.convert("user--name"));
    }

    @Test
    public void test_convert_withLeadingUnderscore() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("_user_name"));
        assertEquals("_user_name", NamingPolicy.SNAKE_CASE.convert("_userName"));
    }

    @Test
    public void test_convert_withTrailingUnderscore() {
        assertEquals("userName", NamingPolicy.CAMEL_CASE.convert("user_name_"));
        assertEquals("user_name_", NamingPolicy.SNAKE_CASE.convert("userName_"));
    }

    @Test
    public void test_convert_withNumbers() {
        assertEquals("user1Name", NamingPolicy.CAMEL_CASE.convert("user1_name"));
        assertEquals("user1_name", NamingPolicy.SNAKE_CASE.convert("user1Name"));
        assertEquals("USER1_NAME", NamingPolicy.SCREAMING_SNAKE_CASE.convert("user1Name"));
    }

    @Test
    public void test_convert_withAcronyms() {
        assertEquals("httpUrl", NamingPolicy.CAMEL_CASE.convert("HTTP_URL"));
        assertEquals("http_url", NamingPolicy.SNAKE_CASE.convert("HTTPUrl"));
    }

    @Test
    public void test_convert_complexMixedFormat() {
        String input = "user-Name_ID";
        assertEquals("usernameid", NamingPolicy.CAMEL_CASE.convert(input).toLowerCase());
    }

    @Test
    public void test_enumValues_allPresent() {
        NamingPolicy[] values = NamingPolicy.values();
        assertEquals(6, values.length);

        assertNotNull(NamingPolicy.CAMEL_CASE);
        assertNotNull(NamingPolicy.UPPER_CAMEL_CASE);
        assertNotNull(NamingPolicy.SNAKE_CASE);
        assertNotNull(NamingPolicy.SCREAMING_SNAKE_CASE);
        assertNotNull(NamingPolicy.NO_CHANGE);
    }

    @Test
    public void test_valueOf_withValidName() {
        assertEquals(NamingPolicy.CAMEL_CASE, NamingPolicy.valueOf("CAMEL_CASE"));
        assertEquals(NamingPolicy.UPPER_CAMEL_CASE, NamingPolicy.valueOf("UPPER_CAMEL_CASE"));
        assertEquals(NamingPolicy.SNAKE_CASE, NamingPolicy.valueOf("SNAKE_CASE"));
        assertEquals(NamingPolicy.SCREAMING_SNAKE_CASE, NamingPolicy.valueOf("SCREAMING_SNAKE_CASE"));
        assertEquals(NamingPolicy.NO_CHANGE, NamingPolicy.valueOf("NO_CHANGE"));
    }
}
