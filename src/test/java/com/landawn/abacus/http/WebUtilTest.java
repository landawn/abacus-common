package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class WebUtilTest extends TestBase {

    @Test
    public void testCurlConvertersRejectFileAndStdinDataArguments() {
        for (final String value : new String[] { "@", "@-", "@payload.txt", "@caf\u00e9\u4e2d.txt" }) {
            for (final String option : new String[] { "-d ", "-d", "--data ", "--data=", "--data-ascii ", "--data-ascii=" }) {
                final String command = "curl https://example.test/ --data-raw 'ordinary' " + option + "'" + value + "'";
                assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage().contains("File/stdin"));
                assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage().contains("File/stdin"));
            }
        }
    }

    @Test
    public void testCurlConvertersPreserveExactDataOptionValues() {
        final String[][] cases = { { "-d=foo", "=foo" }, { "-d=", "=" }, { "-d==x", "==x" }, { "-d=@file", "=@file" }, { "--data=", "" }, { "--data==x", "=x" },
                { "--data= ignored", "" }, { "--data-raw= ignored", "" }, { "--data-ascii= ignored", "" }, { "--data-raw=@-", "@-" },
                { "--data-raw '@caf\u00e9'", "@caf\u00e9" }, { "-d ''", "" }, { "-d 'caf\u00e9\u4e2d\ud83d\ude00'", "caf\u00e9\u4e2d\ud83d\ude00" },
                { "--data= -d '=x' --data-raw '@y'", "&=x&@y" } };
        for (final String[] entry : cases) {
            final String command = "curl https://example.test/ " + entry[0] + " -H 'X-End: kept'";
            final String escaped = com.landawn.abacus.util.EscapeUtil.escapeJava(entry[1]);
            final String legacy = WebUtil.curlToHttpRequestCode(command);
            assertTrue(legacy.contains("String requestBody = \"" + escaped + "\";"), legacy);
            final String okHttp = WebUtil.curlToOkHttpRequestCode(command);
            assertTrue(okHttp.contains("RequestBody.create(\"" + escaped + "\","), okHttp);
            assertTrue(legacy.contains("\"X-End\", \"kept\""));
            assertTrue(okHttp.contains("\"X-End\", \"kept\""));
        }
    }

    @Test
    public void testCurlConvertersRejectAttachedUnsupportedArguments() {
        for (final String option : new String[] { "-u", "-b", "-T", "-F" }) {
            for (final String value : new String[] { "", "user:password", "name=value", "caf\u00e9\u4e2d", "=", "@-" }) {
                for (final String command : new String[] { "curl " + option + value + " https://example.test/",
                        "curl -X POST https://example.test/ " + option + value }) {
                    assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage()
                            .contains("Unsupported curl option '" + option + "'"));
                    assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage()
                            .contains("Unsupported curl option '" + option + "'"));
                }
            }
        }
    }

    @Test
    public void testCurlConvertersKeepAttachedOptionStringsInsideArguments() {
        for (final String value : new String[] { "-uuser:password", "-bname=value", "-Tfile", "-Fname=value" }) {
            final String command = "curl -XPOST https://example.test/ -H'X-Value: " + value + "' -d'" + value + "'";
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertTrue(code.contains("\"X-Value\", \"" + value + "\""), code);
                assertTrue(code.contains(".post()"), code);
            }
        }
    }

    @Test
    public void testCurlConvertersRejectGetQueryRelocation() {
        for (final String flag : new String[] { "-G", "--get" }) {
            for (final String tail : new String[] { "", "-d 'id=7'", "-d ''", "-d 'name=caf\u00e9' -d 'id=7'", "-X GET -d 'id=7'", "-X POST -d 'id=7'",
                    "-I -d 'id=7'", flag + " -d 'id=7'" }) {
                for (final String command : new String[] { "curl " + flag + " https://example.test/ " + tail,
                        "curl https://example.test/ " + tail + " " + flag }) {
                    assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage()
                            .contains("Unsupported curl option '" + flag + "'"), command);
                    assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage()
                            .contains("Unsupported curl option '" + flag + "'"), command);
                }
            }
        }
    }

    @Test
    public void testCurlConvertersPreserveGetFlagArgumentLiteralsAndLowercaseGloboff() {
        for (final String value : new String[] { "-G", "--get" }) {
            final String command = "curl -g https://example.test/ -H 'X-Value: " + value + "' -d '" + value + "'";
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertTrue(code.contains("\"X-Value\", \"" + value + "\""), code);
                assertTrue(code.contains("\"" + value + "\""), code);
                assertTrue(code.contains(".post()"), code);
            }
        }
    }

    @Test
    public void testBuildCurlPreservesLiteralUrlWithoutCurlGlobbing() {
        for (final char quote : new char[] { '\'', '"' }) {
            for (final String url : new String[] { "https://example.test/items/[1-2]", "https://example.test/{one,two}?a[]=1",
                    "https://example.test/?a={x,y}&b=[3-4]", "https://example.test/%5B1-2%5D", "http://[::1]:8080/", "https://example.test/" }) {
                for (final HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.HEAD }) {
                    final String curl = WebUtil.buildCurl(method, url, null, null, null, quote);
                    assertTrue(curl.contains(quote + url + quote + " --globoff"), curl);
                    assertTrue(WebUtil.curlToHttpRequestCode(curl).contains("HttpRequest.url(\"" + url + "\")"), curl);
                }
            }
        }
    }

    @Test
    public void testGeneratedHttpRequestDefaultsDataToFormContentType() {
        for (final String data : new String[] { "a=b", "", "name=caf\u00e9" }) {
            final String code = WebUtil.curlToHttpRequestCode("curl https://example.test/ --data-raw '" + data + "'");
            assertTrue(code.contains(".header(\"Content-Type\", \"application/x-www-form-urlencoded\")"), code);
            assertTrue(code.contains(".body(requestBody)"), code);
            assertTrue(code.contains(".post();"), code);
        }
    }

    @Test
    public void testGeneratedHttpRequestPreservesExplicitContentTypeCasingAndValue() {
        final String code = WebUtil.curlToHttpRequestCode("curl https://example.test/ -H 'cOnTeNt-TyPe: application/json; charset=UTF-8' -d '{}'");
        assertTrue(code.contains(".header(\"cOnTeNt-TyPe\", \"application/json; charset=UTF-8\")"), code);
        assertFalse(code.contains("application/x-www-form-urlencoded"), code);
    }

    @Test
    public void testGeneratedHttpRequestDoesNotAddContentTypeWithoutAttachedBody() {
        for (final String curl : new String[] { "curl https://example.test/", "curl -I https://example.test/ -d 'ignored'" }) {
            final String code = WebUtil.curlToHttpRequestCode(curl);
            assertFalse(code.contains("application/x-www-form-urlencoded"), code);
            assertFalse(code.contains(".body(requestBody)"), code);
        }
    }

    @Test
    public void testBuildCurlTreatsFileAndStdinPrefixesAsLiteralBodies() {
        for (final char quote : new char[] { '\'', '"' }) {
            for (final String body : new String[] { "@", "@payload.txt", "@-" }) {
                final String curl = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", null, body, "text/plain", quote);
                assertTrue(curl.contains(" --data-raw " + quote + body + quote), curl);
                assertFalse(curl.contains(" -d "), curl);
                assertTrue(WebUtil.curlToHttpRequestCode(curl).contains("String requestBody = \"" + body + "\";"));
            }
        }
    }

    @Test
    public void testBuildCurlLiteralBodyRoundTripsQuotesNewlinesAndUnicode() {
        final String body = "@caf\u00e9\n\u4e2d\ud83d\ude00 'quoted' \"double\" \\ $value `command`";
        for (final char quote : new char[] { '\'', '"' }) {
            final String curl = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", Map.of(), body, "text/plain", quote);
            assertTrue(curl.contains(" --data-raw "), curl);
            assertFalse(curl.contains(" -d "), curl);
            assertTrue(WebUtil.curlToHttpRequestCode(curl).contains("String requestBody = \"" + com.landawn.abacus.util.EscapeUtil.escapeJava(body) + "\";"));
        }
    }

    @Test
    public void testBuildCurlNullAndEmptyBodiesDoNotEmitDataOptions() {
        for (final char quote : new char[] { '\'', '"' }) {
            for (final String body : new String[] { null, "" }) {
                final String curl = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", null, body, null, quote);
                assertFalse(curl.contains(" --data"), curl);
                assertFalse(curl.contains(" -d "), curl);
            }
        }
    }

    @Test
    public void testCurl2HttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequestWithPost() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
        assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2HttpRequestWithHeaders() {
        String curl = "curl -H \"Content-Type: application/json\" -H \"Authorization: Bearer token\" https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token\")"));
    }

    @Test
    public void testCurl2HttpRequestWithPut() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d '{\"name\":\"Jane\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".put();"));
    }

    @Test
    public void testCurl2HttpRequestWithDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2HttpRequestWithHead() {
        String curl = "curl -I https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".head();"));
    }

    @Test
    public void testCurl2HttpRequestWithDataInfersPost() {
        String curl = "curl https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestWithDataRaw() {
        String curl = "curl --data-raw '{\"key\":\"value\"}' https://api.example.com/data";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestWithInlineDataOption() {
        String curl = "curl https://api.example.com/users --data='{\"name\":\"John\"}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurlConvertersSupportAttachedShortAndEqualsLongOptions() {
        String curl = "curl -XPOST --header=Content-Type:application/json -HX-Trace:abc -dtest https://api.example.com/users";

        String httpCode = WebUtil.curlToHttpRequestCode(curl);
        assertTrue(httpCode.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(httpCode.contains(".header(\"X-Trace\", \"abc\")"));
        assertTrue(httpCode.contains("String requestBody = \"test\";"));
        assertTrue(httpCode.contains(".post();"));

        String okHttpCode = WebUtil.curlToOkHttpRequestCode(curl.replace("-XPOST", "--request=POST"));
        assertTrue(okHttpCode.contains(".header(\"X-Trace\", \"abc\")"));
        assertTrue(okHttpCode.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestWithLongForm() {
        String curl = "curl --request POST --header \"Content-Type: application/json\" https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".post("));
    }

    @Test
    public void testCurl2HttpRequestWithSpecialCharactersInBody() {
        String curl = "curl -d '{\"message\":\"Hello\\nWorld\"}' https://api.example.com/data";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
    }

    @Test
    public void testCurl2HttpRequest() {
        String curl = "curl -X POST https://api.example.com/users -H \"Content-Type: application/json\" -H \"Authorization: Bearer token123\" -d '{\"name\":\"John\",\"age\":30}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
        assertTrue(result.contains("String requestBody = \"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\";"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestGetWithHeaders() {
        String curl = "curl -X GET https://api.example.com/data -H \"Accept: application/json\"";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
        assertTrue(result.contains(".get();"));
    }

    @Test
    public void testCurl2HttpRequestDeleteWithBody() {
        String curl = "curl -X DELETE https://api.example.com/users/123 -d '{\"hard\":true}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertFalse(result.contains("Request body omitted for DELETE"));
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2HttpRequestOptionsWithBody() {
        String curl = "curl -X OPTIONS https://api.example.com/users -d '{\"probe\":true}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("String requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertFalse(result.contains("Request body omitted for OPTIONS"));
        assertTrue(result.contains(".execute(HttpMethod.OPTIONS);"));
    }

    @Test
    public void testCurl2HttpRequestOtherMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".execute(HttpMethod.PATCH);"));
    }

    @Test
    public void testCurl2HttpRequestWithSingleQuotes() {
        String curl = "curl -X POST 'https://api.example.com/users' -H 'Content-Type: application/json' -d '{\"name\":\"John\"}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
    }

    @Test
    public void testCurl2HttpRequestWithDoubleQuotes() {
        String curl = "curl -X POST \"https://api.example.com/users\" -H \"Content-Type: application/json\"";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users\")"));
    }

    @Test
    public void testCurl2HttpRequestWithLongHeader() {
        String curl = "curl -X GET https://api.example.com/data --header \"Authorization: Bearer token123\"";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
    }

    @Test
    public void testCurl2HttpRequestWithEscapedQuotes() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"message\":\"Hello \\\"World\\\"\"}'";

        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("requestBody"));
    }

    @Test
    public void testCurl2HttpRequestWithHeadLongFormOption() {
        String curl = "curl --head https://api.example.com/users";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".head();"));
    }

    @Test
    public void testCurl2HttpRequestHeadOptionOverridesImplicitPost() {
        String curl = "curl -I https://api.example.com/users -d '{}'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".head();"));
        assertFalse(result.contains(".body(requestBody)"));
        assertFalse(result.contains(".post();"));
        // The local is not declared at all when the body cannot be attached.
        assertFalse(result.contains("String requestBody ="));
    }

    @Test
    public void testCurl2HttpRequestWithMultipleDataOptions() {
        String curl = "curl https://api.example.com/users -d 'name=John' -d 'age=30'";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("String requestBody = \"name=John&age=30\";"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2HttpRequestEscapesUrl() {
        String curl = "curl \"https://api.example.com/users?q=\\\"quoted\\\"\"";
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("HttpRequest.url(\"https://api.example.com/users?q=\\\"quoted\\\"\")"));
    }

    @Test
    public void testCurl2HttpRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(null));
    }

    @Test
    public void testCurl2HttpRequestWithEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(""));
    }

    @Test
    public void testCurl2HttpRequestWithInvalidStartThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("wget https://example.com"));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("not a curl command"));
    }

    @Test
    public void testCurlCommandNameRequiresTokenBoundary() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curly https://example.com"));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode("curling https://example.com"));
    }

    @Test
    public void testCurl2HttpRequestWithoutUrlThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl -X POST -H \"Content-Type: application/json\""));
    }

    @Test
    public void testCurl2OkHttpRequestSimpleGet() {
        String curl = "curl https://api.example.com/users";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".get();"));
        assertTrue(result.contains("Response response = OkHttpRequest.url("));
        assertTrue(result.contains("try (response)"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPost() {
        String curl = "curl -X POST -H \"Content-Type: application/json\" https://api.example.com/users -d '{\"name\":\"John\"}'";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".post();"));
        assertTrue(result.contains("RequestBody requestBody"));
        assertTrue(result.contains("MediaType.parse(\"application/json\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithHeaders() {
        String curl = "curl -H \"Accept: application/json\" https://api.example.com/data";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithPut() {
        String curl = "curl -X PUT https://api.example.com/users/1 -d 'data'";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".put();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithDelete() {
        String curl = "curl -X DELETE https://api.example.com/users/1";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".delete();"));
    }

    @Test
    public void testCurl2OkHttpRequestWithBody() {
        String curl = "curl -H \"Content-Type: text/plain\" -d 'Hello World' https://api.example.com/echo";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains("MediaType.parse(\"text/plain\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithInlineDataOption() {
        String curl = "curl -H \"Content-Type: text/plain\" https://api.example.com/echo --data=Hello";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("RequestBody requestBody"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2OkHttpRequest() {
        String curl = "curl -X POST https://api.example.com/users -H \"Content-Type: application/json\" -H \"Authorization: Bearer token123\" -d '{\"name\":\"John\",\"age\":30}'";

        String result = WebUtil.curlToOkHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users\")"));
        assertTrue(result.contains(".header(\"Content-Type\", \"application/json\")"));
        assertTrue(result.contains(".header(\"Authorization\", \"Bearer token123\")"));
        assertTrue(result.contains(
                "RequestBody requestBody = RequestBody.create(\"{\\\"name\\\":\\\"John\\\",\\\"age\\\":30}\", MediaType.parse(\"application/json\"));"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2OkHttpRequestOtherMethod() {
        String curl = "curl -X PATCH https://api.example.com/users/123 -d '{\"name\":\"Jane\"}'";

        String result = WebUtil.curlToOkHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".execute(HttpMethod.PATCH);"));
    }

    @Test
    public void testCurl2OkHttpRequestWithoutContentType() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John\"}'";

        String result = WebUtil.curlToOkHttpRequestCode(curl);
        assertNotNull(result);
        // curl sends -d data as application/x-www-form-urlencoded unless a Content-Type header says
        // otherwise, so a missing header must not become a null MediaType.
        assertTrue(result.contains("RequestBody.create(\"{\\\"name\\\":\\\"John\\\"}\", MediaType.parse(\"application/x-www-form-urlencoded\"));"));
        assertFalse(result.contains("RequestBody.create(null,"));
    }

    @Test
    public void testCurl2OkHttpRequestWithHeadOption() {
        String curl = "curl -I https://api.example.com/users";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".head();"));
    }

    @Test
    public void testCurl2OkHttpRequestHeadOptionOverridesImplicitPost() {
        String curl = "curl -I https://api.example.com/users -d '{}'";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains(".head();"));
        assertFalse(result.contains(".body(requestBody)"));
        assertFalse(result.contains(".post();"));
        assertFalse(result.contains("RequestBody requestBody ="));
    }

    @Test
    public void testCurl2OkHttpRequestWithMultipleDataOptions() {
        String curl = "curl -H \"Content-Type: application/x-www-form-urlencoded\" https://api.example.com/users -d 'name=John' -d 'age=30'";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(
                result.contains("RequestBody requestBody = RequestBody.create(\"name=John&age=30\", MediaType.parse(\"application/x-www-form-urlencoded\"));"));
        assertTrue(result.contains(".body(requestBody)"));
        assertTrue(result.contains(".post();"));
    }

    @Test
    public void testCurl2OkHttpRequestEscapesUrlAndMediaType() {
        String curl = "curl \"https://api.example.com/users?q=\\\"quoted\\\"\" " + "-H \"Content-Type: multipart/form-data; boundary=\\\"abc\\\"\" -d test";
        String result = WebUtil.curlToOkHttpRequestCode(curl);

        assertNotNull(result);
        assertTrue(result.contains("OkHttpRequest.url(\"https://api.example.com/users?q=\\\"quoted\\\"\")"));
        assertTrue(result.contains("MediaType.parse(\"multipart/form-data; boundary=\\\"abc\\\"\")"));
    }

    @Test
    public void testCurl2OkHttpRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(null));
    }

    @Test
    public void testCurl2OkHttpRequestWithEmptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(""));
    }

    @Test
    public void testCurl2OkHttpRequestWithInvalidStartThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode("not a curl command"));
    }

    @Test
    public void testCurl2OkHttpRequestWithoutUrlThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode("curl -X POST -H \"Content-Type: application/json\""));
    }

    @Test
    public void testParseCurlWithDoubleSlashInPath() {
        String curl = "curl https://api.example.com/a//b";
        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains("https://api.example.com/a//b"));
    }

    @Test
    public void testParseCurlWithBackslash() {
        String curl = "curl https://api.example.com/data \\\n-H \"Accept: application/json\"";
        String result = WebUtil.curlToHttpRequestCode(curl);
        assertNotNull(result);
        assertTrue(result.contains(".header(\"Accept\", \"application/json\")"));
    }

    @Test
    public void testParseCurlPreservesEscapedWhitespace() {
        String result = WebUtil.curlToHttpRequestCode("curl https://api.example.com/data -d foo\\ bar");

        assertTrue(result.contains("String requestBody = \"foo bar\";"));
    }

    @Test
    public void testParseCurlCombinesAdjacentQuotedFragments() {
        String curl = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/data", null, "O'Brien", "text/plain", '\'');
        String result = WebUtil.curlToHttpRequestCode(curl);

        assertTrue(result.contains("String requestBody = \"O'Brien\";"));
    }

    @Test
    public void testCurlConvertersPreserveExplicitEmptyData() {
        String curl = "curl https://api.example.com/data -d ''";

        String httpCode = WebUtil.curlToHttpRequestCode(curl);
        assertTrue(httpCode.contains("String requestBody = \"\";"));
        assertTrue(httpCode.contains(".body(requestBody)"));

        String okHttpCode = WebUtil.curlToOkHttpRequestCode(curl);
        assertTrue(okHttpCode.contains("RequestBody requestBody = RequestBody.create(\"\", MediaType.parse(\"application/x-www-form-urlencoded\"));"));
        assertTrue(okHttpCode.contains(".body(requestBody)"));
    }

    @Test
    public void testCurlConvertersRejectInvalidOptionArguments() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl https://api.example.com -X"));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode("curl https://api.example.com -X UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl https://api.example.com -d"));
    }

    @Test
    public void testParseCurlWithUnmatchedQuote() {
        String curl = "curl -X POST https://api.example.com/users -d '{\"name\":\"John";
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(curl));
    }

    @Test
    public void testCreateOkHttpRequestForCurl() {
        AtomicReference<String> capturedCurl = new AtomicReference<>();
        OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", capturedCurl::set);
        assertNotNull(request);

        Consumer<String> logHandler = curl -> assertNotNull(curl);
        assertNotNull(WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", logHandler));
    }

    @Test
    public void testCreateOkHttpRequestForCurlWithQuoteChar() {
        Consumer<String> logHandler = curl -> {
            assertNotNull(curl);
            assertTrue(curl.contains("\""));
        };

        OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", '"', logHandler);
        assertNotNull(request);
    }

    @Test
    public void testCreateCurlLoggingOkHttpRequestRejectsNullLogHandler() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", null));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.createCurlLoggingOkHttpRequest("https://api.example.com", '"', null));
    }

    @Test
    public void testBuildCurlWithGetMethod() {
        String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/users", null, null, null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X GET"));
        assertTrue(result.contains("'https://api.example.com/users'"));
    }

    @Test
    public void testBuildCurlWithPostMethod() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        String result = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/users", headers, "{\"name\":\"John\"}", "application/json", '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X POST"));
        assertTrue(result.contains("-H 'Content-Type: application/json'"));
        assertTrue(result.contains("--data-raw '{\"name\":\"John\"}'"));
    }

    @Test
    public void testBuildCurlWithMultipleHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");
        headers.put("Accept", "application/json");

        String result = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/data", headers, "{}", "application/json", '\'');

        assertNotNull(result);
        assertTrue(result.contains("-H 'Content-Type: application/json'"));
        assertTrue(result.contains("-H 'Authorization: Bearer token123'"));
        assertTrue(result.contains("-H 'Accept: application/json'"));
    }

    @Test
    public void testBuildCurlRendersNullHeaderValueAsEmpty() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Empty", null);

        String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/data", headers, null, null, '\'');

        // "X-Empty: " is curl's spelling for *removing* the header; "X-Empty;" sends it empty.
        assertTrue(result.contains("-H 'X-Empty;'"), result);
        assertFalse(result.contains("X-Empty: "), result);
        assertFalse(result.contains("X-Empty: null"), result);
    }

    @Test
    public void testBuildCurlWithDoubleQuotes() {
        String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/users", null, null, null, '"');

        assertNotNull(result);
        assertTrue(result.contains("\"https://api.example.com/users\""));
    }

    @Test
    public void testBuildCurlEscapesUrlQuotes() {
        String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/o'hare?q='quoted'", null, null, null, '\'');

        assertNotNull(result);
        // POSIX single-quoted strings can't contain an escaped single quote; each ' must become '\''
        // (close quote, escaped quote, reopen quote). The old backslash-escaping produced a broken command.
        assertTrue(result.contains("'https://api.example.com/o'\\''hare?q='\\''quoted'\\'''"), result);
    }

    @Test
    public void testBuildCurlDoubleQuoteEscapesExpansionChars() {
        // Inside double quotes the shell expands $ and `; both (plus \ and ") must be backslash-escaped
        // so the generated command does not perform unintended expansion.
        String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/p?x=$HOME&y=`id`", null, null, null, '"');

        assertNotNull(result);
        assertTrue(result.contains("\"https://api.example.com/p?x=\\$HOME&y=\\`id\\`\""), result);
    }

    @Test
    public void testBuildCurlWithBodyNoHeaders() {
        String result = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/data", null, "test data", "text/plain", '\'');

        assertNotNull(result);
        assertTrue(result.contains("-H 'Content-Type: text/plain'"));
        assertTrue(result.contains("--data-raw 'test data'"));
    }

    @Test
    public void testBuildCurlWithBodyAndContentTypeHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml");

        String result = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/data", headers, "<root/>", "application/json", '\'');

        assertNotNull(result);
        // Should use the header value, not the bodyContentType
        assertTrue(result.contains("-H 'Content-Type: application/xml'"));
        // Should not add another Content-Type header
        int firstIndex = result.indexOf("Content-Type");
        int lastIndex = result.lastIndexOf("Content-Type");
        assertEquals(firstIndex, lastIndex); // Only one occurrence
    }

    @Test
    public void testBuildCurlWithEmptyBody() {
        String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/users", null, "", null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X GET"));
        assertTrue(result.contains("'https://api.example.com/users'"));
        assertTrue(!result.contains("-d"));
    }

    @Test
    public void testBuildCurlWithQuoteEscaping() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Custom-Header", "value with 'quotes'");

        String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/test", headers, null, null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("Custom-Header"));
    }

    @Test
    public void testBuildCurlEscapesHeaderName() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("X-Owner's-Token", "value");

        final String result = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/test", headers, null, null, '\'');

        assertTrue(result.contains("-H 'X-Owner'\\''s-Token: value'"), result);
    }

    @Test
    public void testBuildCurlWithPutMethod() {
        String result = WebUtil.buildCurl(HttpMethod.PUT, "https://api.example.com/users/1", null, "{\"name\":\"Updated\"}", "application/json", '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X PUT"));
        assertTrue(result.contains("--data-raw"));
    }

    @Test
    public void testBuildCurlWithDeleteMethod() {
        String result = WebUtil.buildCurl(HttpMethod.DELETE, "https://api.example.com/users/1", null, null, null, '\'');

        assertNotNull(result);
        assertTrue(result.contains("curl -X DELETE"));
    }

    @Test
    public void testBuildCurl() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");

        String curl = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/users", headers, "{\"name\":\"John\"}", "application/json", '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("curl -X POST 'https://api.example.com/users'"));
        assertTrue(curl.contains("-H 'Content-Type: application/json'"));
        assertTrue(curl.contains("-H 'Authorization: Bearer token123'"));
        assertTrue(curl.contains("--data-raw '{\"name\":\"John\"}'"));
    }

    @Test
    public void testBuildCurlWithoutBody() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", "*/*");

        String curl = WebUtil.buildCurl(HttpMethod.GET, "https://api.example.com/data", headers, null, null, '\'');

        assertNotNull(curl);
        assertFalse(curl.contains("-d"));
    }

    @Test
    public void testBuildCurlWithEmptyHeaders() {
        String curl = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/users", null, "{\"test\":true}", null, '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("curl -X POST"));
        assertTrue(curl.contains("--data-raw '{\"test\":true}'"));
    }

    @Test
    public void testBuildCurlWithBodyTypeButNoContentTypeHeader() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        String curl = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/users", headers, "{\"test\":true}", "application/json", '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("-H 'Content-Type: application/json'"));
    }

    @Test
    public void testBuildCurlWithSpecialCharacters() {
        Map<String, Object> headers = new HashMap<>();
        String body = "{\"message\":\"Hello 'World'\"}";

        String curl = WebUtil.buildCurl(HttpMethod.POST, "https://api.example.com/users", headers, body, null, '\'');

        assertNotNull(curl);
        assertTrue(curl.contains("--data-raw"));
        // The body should be properly escaped
    }

    @Test
    public void testBuildCurlWithNullHttpMethodThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.buildCurl(null, "https://api.example.com/users", null, null, null, '\''));
    }

    @Test
    public void testBuildCurlWithNullUrlThrows() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.buildCurl(HttpMethod.GET, null, null, null, null, '\''));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWhenNotSet() {
        HttpHeaders headers = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("application/json", headers);

        assertEquals("application/json", headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWhenAlreadySet() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setContentType("text/xml");

        WebUtil.setContentTypeByRequestBodyType("application/json", headers);

        // Should not change existing Content-Type
        assertEquals("text/xml", headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWithEmptyBodyType() {
        HttpHeaders headers = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("", headers);

        // Should not set Content-Type
        assertEquals(null, headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeWithNullBodyType() {
        HttpHeaders headers = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType(null, headers);

        // Should not set Content-Type
        assertEquals(null, headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testCurl2HttpRequestMethodCaseInsensitiveInTurkishLocale() {
        // Regression: methods like "options" / "patch" with lowercase 'i' must be uppercased
        // using a locale-independent mapping. With Locale.getDefault() set to Turkish, the
        // default toUpperCase() converts 'i' -> 'İ' (U+0130), which would prevent
        // HttpMethod.valueOf(...) and httpMethodMap.containsValue(...) from matching.
        final java.util.Locale prev = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(new java.util.Locale("tr", "TR"));

            final String curl = "curl -X options https://api.example.com/users";
            final String result = WebUtil.curlToHttpRequestCode(curl);

            assertNotNull(result);
            assertTrue(result.contains(".execute(HttpMethod.OPTIONS)"), "Expected OPTIONS handling regardless of default locale, but got:\n" + result);
        } finally {
            java.util.Locale.setDefault(prev);
        }
    }

    @Test
    public void testCurl2OkHttpRequestMethodCaseInsensitiveInTurkishLocale() {
        // Same regression for the OkHttp generator path.
        final java.util.Locale prev = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(new java.util.Locale("tr", "TR"));

            final String curl = "curl -X options https://api.example.com/users";
            final String result = WebUtil.curlToOkHttpRequestCode(curl);

            assertNotNull(result);
            assertTrue(result.contains(".execute(HttpMethod.OPTIONS)"), "Expected OPTIONS handling regardless of default locale, but got:\n" + result);
        } finally {
            java.util.Locale.setDefault(prev);
        }
    }

    // ==================== a08 F-1: PATCH / CONNECT cannot be issued by HttpRequest ====================

    @Test
    public void testCurl2HttpRequestPatchAndConnectCarryAnUnsupportedMethodComment() {
        for (final String command : new String[] { "curl -X PATCH https://api.example.test/v1/items/1 -d '{\"a\":1}'",
                "curl -X PATCH https://api.example.test/v1/items/1", "curl -X patch https://api.example.test/v1/items/1",
                "curl -XPATCH https://api.example.test/v1/items/1", "curl --request=patch https://api.example.test/v1/items/1 -d 'café中'" }) {
            final String code = WebUtil.curlToHttpRequestCode(command);
            assertTrue(code.startsWith("\n  // HttpRequest cannot issue PATCH"), code);
            assertTrue(code.contains("WebUtil.curlToOkHttpRequestCode"), code);
            assertTrue(code.contains("HttpMethod#PATCH"), code);
            assertTrue(code.contains(".execute(HttpMethod.PATCH);"), code);
            assertEquals(command.contains("-d '"), code.contains(".body(requestBody)"), code);
            assertEquals(command.contains("-d '"), code.contains("String requestBody = "), code);
            assertTrue(code.indexOf("// HttpRequest cannot issue PATCH") < code.indexOf("HttpRequest.url("), code);

            final String okHttp = WebUtil.curlToOkHttpRequestCode(command);
            assertFalse(okHttp.contains("cannot issue"), okHttp);
            assertTrue(okHttp.contains(".execute(HttpMethod.PATCH);"), okHttp);
            assertEquals(command.contains("-d '"), okHttp.contains(".body(requestBody)"), okHttp);
        }

        final String connect = WebUtil.curlToHttpRequestCode("curl -X CONNECT https://proxy.example.test:443/");
        assertTrue(connect.startsWith("\n  // HttpRequest cannot issue CONNECT"), connect);
        assertTrue(connect.contains(".execute(HttpMethod.CONNECT);"), connect);
        assertFalse(WebUtil.curlToOkHttpRequestCode("curl -X CONNECT https://proxy.example.test:443/").contains("cannot issue"));

        for (final String method : new String[] { "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE" }) {
            final String code = WebUtil.curlToHttpRequestCode("curl -X " + method + " https://api.example.test/ -d 'a=1'");
            assertFalse(code.contains("cannot issue"), code);
            assertFalse(WebUtil.curlToHttpRequestCode("curl -X " + method + " https://api.example.test/").contains("  //"), code);
        }
    }

    // ==================== a08 F-3: argument-taking options, --url, -A/-e, multiple URLs ====================

    @Test
    public void testCurlConvertersConsumeArgumentsOfIgnoredOptions() {
        final String target = "https://api.example.test/v1/items";
        final String[] shapes = { "-x http://proxy.corp:8080", "--proxy http://proxy.corp:8080", "--proxy=http://proxy.corp:8080", "-xhttp://proxy.corp:8080",
                "-x proxy.corp:8080", "-o out.txt", "--output out.txt", "-c jar.txt", "--cookie-jar jar.txt", "-D headers.txt", "--dump-header headers.txt",
                "--cacert ca.pem", "--cert client.pem", "--key client.key", "-m 5", "--max-time 5", "--connect-timeout 3", "--retry 2", "--retry-delay 1",
                "--retry-max-time 9", "-w '%{http_code}'", "--write-out '%{http_code}'", "--resolve host:443:1.2.3.4", "--socks4 localhost:1080",
                "--socks4a localhost:1080", "--socks5 localhost:1080", "--socks5-hostname localhost:1080", "--interface eth0", "-r 0-99", "--range 0-99",
                "--limit-rate 1k", "-U pu:pp", "--proxy-user pu:pp", "--ciphers ECDHE", "--proto =https", "--max-redirs 3", "-y 30", "--speed-time 30",
                "-Y 100", "--speed-limit 100", "--stderr err.txt", "--trace trace.txt", "--trace-ascii trace.txt", "-K curlrc", "--config curlrc",
                "-o 'café中.txt'", "-x https://proxy.corp:8080" };

        for (final String shape : shapes) {
            for (final String command : new String[] { "curl " + shape + " " + target, "curl " + target + " " + shape, "curl " + shape + " -X DELETE " + target,
                    "curl -X DELETE " + shape + " " + target }) {
                for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                    assertTrue(code.contains(".url(\"" + target + "\")"), command + " -> " + code);
                    assertTrue(code.contains(command.contains("-X DELETE") ? ".delete();" : ".get();"), command + " -> " + code);
                    assertFalse(code.contains("proxy.corp"), command + " -> " + code);
                    assertFalse(code.contains("requestBody"), command + " -> " + code);
                    assertFalse(code.contains(".header("), command + " -> " + code);
                }
            }
        }

        // The argument is consumed even when it looks like an option or a data value.
        final String code = WebUtil.curlToHttpRequestCode("curl -o -d " + target + " -m -X");
        assertTrue(code.contains(".url(\"" + target + "\")"), code);
        assertTrue(code.contains(".get();"), code);

        // A bare option at the end of the command has no argument to consume.
        for (final String option : new String[] { "-x", "--proxy", "-o", "-m", "--connect-timeout", "-K" }) {
            final String command = "curl " + target + " " + option;
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage().contains("Missing argument"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage().contains("Missing argument"));
        }

        // --data-urlencode is a data option, not an ignored one: still rejected.
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl --data-urlencode 'a=b' " + target));
        // Unknown flags without an argument stay ignored (documented contract).
        assertTrue(WebUtil.curlToHttpRequestCode("curl --frobnicate -L --compressed -k " + target).contains(".url(\"" + target + "\")"));
    }

    @Test
    public void testCurlConvertersMapUserAgentAndRefererToHeaders() {
        final String target = "https://api.example.test/v1";
        final String[][] cases = { { "-A 'MyAgent/1.0'", "User-Agent", "MyAgent/1.0" }, { "--user-agent 'MyAgent/1.0'", "User-Agent", "MyAgent/1.0" },
                { "--user-agent=MyAgent/1.0", "User-Agent", "MyAgent/1.0" }, { "-A'café中'", "User-Agent", "café中" },
                { "-AMyAgent/1.0", "User-Agent", "MyAgent/1.0" }, { "-e https://referer.example/page", "Referer", "https://referer.example/page" },
                { "--referer https://referer.example/page", "Referer", "https://referer.example/page" },
                { "--referer=https://referer.example/page", "Referer", "https://referer.example/page" },
                { "-ehttps://referer.example/page", "Referer", "https://referer.example/page" },
                { "-e 'https://referer.example/page;auto'", "Referer", "https://referer.example/page" },
                { "--referer='https://referer.example/page;auto'", "Referer", "https://referer.example/page" } };

        for (final String[] entry : cases) {
            final String escaped = com.landawn.abacus.util.EscapeUtil.escapeJava(entry[2]);

            for (final String command : new String[] { "curl " + entry[0] + " " + target, "curl " + target + " " + entry[0],
                    "curl " + entry[0] + " -X POST " + target + " -d 'a=1'" }) {
                for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                    assertTrue(code.contains(".header(\"" + entry[1] + "\", \"" + escaped + "\")"), command + " -> " + code);
                    assertTrue(code.contains(".url(\"" + target + "\")"), command + " -> " + code);
                    assertFalse(code.contains(".url(\"https://referer"), command + " -> " + code);
                    assertTrue(code.contains(command.contains("-X POST") ? ".post();" : ".get();"), command + " -> " + code);
                }
            }
        }

        // A bare ";auto" sends no Referer on the first request.
        for (final String code : new String[] { WebUtil.curlToHttpRequestCode("curl -e ';auto' " + target),
                WebUtil.curlToOkHttpRequestCode("curl --referer=';auto' " + target) }) {
            assertFalse(code.contains("Referer"), code);
            assertTrue(code.contains(".get();"), code);
        }

        // Together with an explicit header of the same name it is a repeated header, which is rejected.
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl -A a -H 'User-Agent: b' " + target));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode("curl -H 'referer: a' -e b " + target));

        for (final String option : new String[] { "-A", "--user-agent", "-e", "--referer" }) {
            final String command = "curl " + target + " " + option;
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage().contains("Missing argument"));
        }
    }

    @Test
    public void testCurlConvertersAcceptUrlOptionAndRejectMultipleUrls() {
        final String a = "https://a.example.test/";
        final String b = "https://b.example.test/";

        for (final String command : new String[] { "curl --url " + a, "curl --url=" + a, "curl -X POST --url " + a + " -d 'x=1'", "curl --url '" + a + "'",
                "curl -s --url " + a + " -H 'X-Y: y'" }) {
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertTrue(code.contains(".url(\"" + a + "\")"), command + " -> " + code);
            }
        }

        for (final String command : new String[] { "curl " + a + " " + b, "curl --url " + a + " " + b, "curl " + a + " --url=" + b,
                "curl --url " + a + " --url " + b, "curl --frobnicate " + a + " " + b, "curl -X POST " + a + " -d 'a=1' " + b,
                "curl " + a + " HTTP://B.EXAMPLE.TEST/" }) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage().contains("Multiple URLs"),
                    command);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage().contains("Multiple URLs"),
                    command);
        }

        assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl -s --url")).getMessage().contains("Missing URL"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl -s")).getMessage().contains("No URL found"));

        // URL-looking strings in argument position are values, not URLs.
        final String code = WebUtil.curlToHttpRequestCode("curl " + a + " -H 'Referer: " + b + "' -d '" + b + "'");
        assertTrue(code.contains(".url(\"" + a + "\")"), code);
        assertTrue(code.contains(".header(\"Referer\", \"" + b + "\")"), code);
        assertTrue(code.contains("String requestBody = \"" + b + "\";"), code);
    }

    // ==================== a08 F-4: clustered short options ====================

    @Test
    public void testCurlConvertersDecomposeShortOptionClusters() {
        final String target = "https://example.test/api";
        final String[][] cases = { { "-sX POST", ".post();" }, { "-sX DELETE", ".delete();" }, { "-LX DELETE", ".delete();" }, { "-sXPOST", ".post();" },
                { "-sSLX PUT", ".put();" }, { "-sI", ".head();" }, { "-Is", ".head();" }, { "-sSLf", ".get();" }, { "-fsSL", ".get();" }, { "-4sv", ".get();" },
                { "-#sS", ".get();" }, { "-sd 'a=1'", ".post();" }, { "-sSd'a=1'", ".post();" }, { "-sH 'X-Y: y'", ".get();" }, { "-ksH'X-Y: y'", ".get();" },
                { "-sX POST -d 'a=1'", ".post();" }, { "-sX post", ".post();" } };

        for (final String[] entry : cases) {
            for (final String command : new String[] { "curl " + entry[0] + " " + target, "curl " + target + " " + entry[0] }) {
                for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                    assertTrue(code.contains(entry[1]), command + " -> " + code);
                    assertTrue(code.contains(".url(\"" + target + "\")"), command + " -> " + code);
                    assertEquals(entry[0].contains("a=1"), code.contains("requestBody"), command + " -> " + code);
                    assertEquals(entry[0].contains("X-Y"), code.contains(".header(\"X-Y\", \"y\")"), command + " -> " + code);
                }
            }
        }

        // Unicode after the cluster.
        final String unicode = WebUtil.curlToHttpRequestCode("curl -sH 'X-Y: café中' " + target + " -sd 'café中'");
        assertTrue(unicode.contains(".header(\"X-Y\", \"" + com.landawn.abacus.util.EscapeUtil.escapeJava("café中") + "\")"), unicode);
        assertTrue(unicode.contains("String requestBody = \"" + com.landawn.abacus.util.EscapeUtil.escapeJava("café中") + "\";"), unicode);

        // Clustered forms of the rejected options are rejected with the same message as the plain forms.
        final String[][] rejected = { { "-Lu u:p", "-u" }, { "-ku u:p", "-u" }, { "-sLu u:p", "-u" }, { "-sb 'k=v'", "-b" }, { "-sT file.txt", "-T" },
                { "-sF 'a=b'", "-F" }, { "-sG", "-G" }, { "-sG -d 'id=7'", "-G" }, { "-LsG", "-G" } };

        for (final String[] entry : rejected) {
            for (final String command : new String[] { "curl " + entry[0] + " " + target, "curl " + target + " " + entry[0] }) {
                assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage()
                        .contains("Unsupported curl option '" + entry[1] + "'"), command);
                assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage()
                        .contains("Unsupported curl option '" + entry[1] + "'"), command);
            }
        }

        // An argument-taking first letter takes the rest of the token: nothing is peeled.
        assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl -XsPOST " + target)).getMessage()
                .contains("Unsupported HTTP method: SPOST"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode("curl -XsPOST " + target)).getMessage()
                .contains("Unsupported HTTP method: SPOST"));

        // Cluster-looking strings in argument position stay literal.
        for (final String value : new String[] { "-sX", "-Lu", "-sSLf", "-sd" }) {
            final String command = "curl " + target + " -d '" + value + "' -H 'X-V: " + value + "'";
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertTrue(code.contains("\"" + value + "\""), command + " -> " + code);
                assertTrue(code.contains(".header(\"X-V\", \"" + value + "\")"), command + " -> " + code);
                assertTrue(code.contains(".post();"), command + " -> " + code);
            }
        }

        // Clusters combine with consumed-argument options: -Lo takes the next token as the output file.
        final String consumed = WebUtil.curlToHttpRequestCode("curl -Lo out.txt " + target);
        assertTrue(consumed.contains(".url(\"" + target + "\")"), consumed);
        assertTrue(consumed.contains(".get();"), consumed);
    }

    // ==================== a08 F-5: special -H spellings ====================

    @Test
    public void testCurlConvertersHandleSpecialHeaderSpellings() {
        final String target = "https://example.test/";

        for (final String form : new String[] { "-H '@headers.txt'", "-H @headers.txt", "-H'@headers.txt'", "--header '@-'", "--header=@h.txt", "-H '@'",
                "-sH '@café.txt'" }) {
            final String command = "curl " + target + " " + form;
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage().contains("File/stdin header"),
                    command);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage().contains("File/stdin header"),
                    command);
        }

        for (final String form : new String[] { "-H 'X-Empty;'", "-H'X-Empty;'", "--header=X-Empty;", "-H 'X-Empty; '", "-H ' X-Empty ;'", "-sH 'X-Empty;'" }) {
            final String command = "curl " + target + " " + form;
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertTrue(code.contains(".header(\"X-Empty\", \"\")"), command + " -> " + code);
            }
        }

        // 'Name:' is curl's spelling for suppressing a header, not for sending it empty.
        for (final String code : new String[] { WebUtil.curlToHttpRequestCode("curl " + target + " -H 'Host:'"),
                WebUtil.curlToOkHttpRequestCode("curl " + target + " -H 'X-Gone: '") }) {
            assertFalse(code.contains(".header("), code);
            assertTrue(code.contains("suppress the header(s)"), code);
        }

        for (final String form : new String[] { "-H ' : v'", "-H ':v'", "-H ':'", "-H ';'", "-H ' ;'", "--header=': v'" }) {
            final String command = "curl " + target + " " + form;
            assertTrue(assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(command)).getMessage().contains("Header name is empty"),
                    command);
            assertTrue(
                    assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(command)).getMessage().contains("Header name is empty"),
                    command);
        }

        // No ':' and no trailing ';' is dropped, as curl does.
        for (final String form : new String[] { "-H ''", "-H 'X-Plain'", "-H 'café'" }) {
            final String command = "curl " + target + " " + form;
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertFalse(code.contains(".header("), command + " -> " + code);
                assertTrue(code.contains(".get();"), command + " -> " + code);
            }
        }

        // The first colon separates name and value; later colons belong to the value.
        for (final String code : new String[] { WebUtil.curlToHttpRequestCode("curl " + target + " -H 'X-Time: 12:30'"),
                WebUtil.curlToOkHttpRequestCode("curl " + target + " -H 'X-Time: 12:30;'") }) {
            assertTrue(code.contains(".header(\"X-Time\", \"12:30"), code);
        }

        // A literal '@' inside a value is not a file reference.
        assertTrue(WebUtil.curlToHttpRequestCode("curl " + target + " -H 'From: user@example.test'").contains(".header(\"From\", \"user@example.test\")"));
    }

    // ==================== G09 fixes 2026-09-08 ====================

    @Test
    public void g09_curlConverters_headerRemovalIsNotAnEmptyValue() {
        final String target = "https://example.test/";

        // "-H 'Name;'" sends the header empty; "-H 'Name:'" tells curl not to send it at all. The two
        // opposite instructions must not generate the same code.
        for (final String form : new String[] { "-H 'X-Gone:'", "-H 'X-Gone: '", "-H'X-Gone:'", "--header=X-Gone:", "-sH 'X-Gone:  '" }) {
            final String command = "curl " + target + " " + form;

            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertFalse(code.contains(".header(\"X-Gone\""), command + " -> " + code);
                assertTrue(code.contains("// cURL was told to suppress the header(s) X-Gone"), command + " -> " + code);
            }
        }

        for (final String code : new String[] { WebUtil.curlToHttpRequestCode("curl " + target + " -H 'X-Gone;'"),
                WebUtil.curlToOkHttpRequestCode("curl " + target + " -H 'X-Gone;'") }) {
            assertTrue(code.contains(".header(\"X-Gone\", \"\")"), code);
            assertFalse(code.contains("suppress the header(s)"), code);
        }
    }

    @Test
    public void g09_curlConverters_removalIsReportedOnceAndYieldsToAnExplicitValue() {
        final String target = "https://example.test/";

        // curl still sends a name that the same command also gives a value, whatever the order.
        for (final String command : new String[] { "curl " + target + " -H 'Accept: text/plain' -H 'Accept:'",
                "curl " + target + " -H 'accept:' -H 'Accept: text/plain'" }) {
            for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
                assertTrue(code.contains(".header(\"Accept\", \"text/plain\")"), command + " -> " + code);
                assertFalse(code.contains("suppress the header(s)"), command + " -> " + code);
            }
        }

        // A repeated suppression of the same name is listed once; distinct names are listed in order.
        final String code = WebUtil.curlToHttpRequestCode("curl " + target + " -H 'Accept:' -H 'accept: ' -H 'User-Agent:'");
        assertTrue(code.contains("// cURL was told to suppress the header(s) Accept, User-Agent"), code);
        assertFalse(code.contains(".header("), code);

        // A suppressed Content-Type does not become an empty Content-Type on an attached body.
        final String posted = WebUtil.curlToOkHttpRequestCode("curl " + target + " -d 'a=1' -H 'Content-Type:'");
        assertTrue(posted.contains("MediaType.parse(\"application/x-www-form-urlencoded\")"), posted);
        assertFalse(posted.contains(".header(\"Content-Type\", \"\")"), posted);
    }

    @Test
    public void g09_buildCurl_headWithABodyCannotUseMinusI() {
        // curl refuses "-I" together with a data option ("You can only select one HTTP request method").
        final String withBody = WebUtil.buildCurl(HttpMethod.HEAD, "http://h/u", null, "a=1", "text/plain", '\'');
        assertTrue(withBody.contains("curl -X HEAD 'http://h/u'"), withBody);
        assertFalse(withBody.contains("curl -I"), withBody);
        assertTrue(withBody.contains("--data-raw 'a=1'"), withBody);

        // Without a body, -I is still what curl needs so it does not wait for a response body.
        final String withoutBody = WebUtil.buildCurl(HttpMethod.HEAD, "http://h/u", null, null, null, '\'');
        assertTrue(withoutBody.contains("curl -I 'http://h/u'"), withoutBody);
        assertFalse(withoutBody.contains("-X HEAD"), withoutBody);

        final String emptyBody = WebUtil.buildCurl(HttpMethod.HEAD, "http://h/u", null, "", null, '\'');
        assertTrue(emptyBody.contains("curl -I 'http://h/u'"), emptyBody);
    }

    @Test
    public void g09_buildCurl_emptyHeaderValueRoundTripsThroughTheConverter() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("X-Empty", "");
        headers.put("X-Null", null);
        headers.put("X-Kept", "v");

        for (final char quote : new char[] { '\'', '"' }) {
            final String curl = WebUtil.buildCurl(HttpMethod.GET, "https://example.test/", headers, null, null, quote);

            assertTrue(curl.contains(" -H " + quote + "X-Empty;" + quote), curl);
            assertTrue(curl.contains(" -H " + quote + "X-Null;" + quote), curl);
            assertTrue(curl.contains(" -H " + quote + "X-Kept: v" + quote), curl);
            assertFalse(curl.contains("X-Empty: "), curl);

            // and the generated command parses back into the same empty-valued headers
            final String code = WebUtil.curlToHttpRequestCode(curl.trim());
            assertTrue(code.contains(".header(\"X-Empty\", \"\")"), code);
            assertTrue(code.contains(".header(\"X-Null\", \"\")"), code);
            assertTrue(code.contains(".header(\"X-Kept\", \"v\")"), code);
            assertFalse(code.contains("suppress the header(s)"), code);
        }
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-08 spillover S3 ITEM 4 (finding 46): buildCurl has the header NAME in hand, so it renders
    // the value with the field-aware HttpHeaders.valueOf(name, value). CurlInterceptor feeds it
    // Headers.toMultimap(), i.e. a List per name, so two Cookie lines used to be emitted as the
    // comma-joined "-H 'cookie: a=1, b=2'" - a curl command that sends a malformed cookie string.
    // ------------------------------------------------------------------------------------------

    @Test
    public void reviewFixes20260908_buildCurlJoinsACookieCollectionWithSemicolons() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("Cookie", Arrays.asList("a=1", "b=2"));
        headers.put("Accept-Encoding", Arrays.asList("gzip", "br"));

        for (final char quote : new char[] { '\'', '"' }) {
            final String curl = WebUtil.buildCurl(HttpMethod.GET, "https://example.test/", headers, null, null, quote);

            // RFC 6265 5.4: one Cookie line, cookie-pairs separated by "; "
            assertTrue(curl.contains(" -H " + quote + "Cookie: a=1; b=2" + quote), curl);
            // every other multiply-valued field keeps the comma-separated list grammar
            assertTrue(curl.contains(" -H " + quote + "Accept-Encoding: gzip, br" + quote), curl);
            assertFalse(curl.contains("Cookie: a=1, b=2"), curl);
        }

        // the name is matched case-insensitively, which is what CurlInterceptor needs:
        // okhttp3.Headers.toMultimap() lower-cases every name
        final String fromInterceptorShape = WebUtil.buildCurlByMethodName("GET", "https://example.test/",
                Map.of("cookie", List.of("a=1", "b=2")), null, null, '\'');
        assertTrue(fromInterceptorShape.contains(" -H 'cookie: a=1; b=2'"), fromInterceptorShape);

        // a single-element collection and a plain String value are unchanged
        assertTrue(WebUtil.buildCurl(HttpMethod.GET, "https://example.test/", Map.of("Cookie", List.of("only=1")), null, null, '\'')
                .contains(" -H 'Cookie: only=1'"));
        assertTrue(WebUtil.buildCurl(HttpMethod.GET, "https://example.test/", Map.of("Cookie", "a=1; b=2"), null, null, '\'')
                .contains(" -H 'Cookie: a=1; b=2'"));
    }

    // ==================== R01 self-review fixes 2026-09-08 ====================

    @Test
    public void r01_buildCurl_blankHeaderValueUsesTheSendEmptySpelling() {
        // curl skips the whitespace after the colon, so "-H 'Name:   '" is the same REMOVAL instruction as
        // "-H 'Name:'". Emitting it for a blank value undid the very fix that stopped emitting "Name: " for
        // an empty one, and WebUtil's own parser (which trims) then read the header back as a suppression.
        for (final String blank : new String[] { " ", "\t", "   ", "\t \t" }) {
            final Map<String, Object> headers = new HashMap<>();
            headers.put("X-Blank", blank);

            for (final char quote : new char[] { '\'', '"' }) {
                final String curl = WebUtil.buildCurl(HttpMethod.GET, "https://example.test/", headers, null, null, quote);

                assertTrue(curl.contains(" -H " + quote + "X-Blank;" + quote), curl);
                assertFalse(curl.contains("X-Blank: "), curl);

                // and it round-trips back to the empty-valued header rather than to a suppression note
                final String code = WebUtil.curlToHttpRequestCode(curl.trim());
                assertTrue(code.contains(".header(\"X-Blank\", \"\")"), code);
                assertFalse(code.contains("suppress the header(s)"), code);
            }
        }

        // a value that only looks blank is still sent as-is
        assertTrue(WebUtil.buildCurl(HttpMethod.GET, "https://example.test/", Map.of("X-Kept", " v "), null, null, '\'')
                .contains(" -H 'X-Kept:  v '"));
    }

    @Test
    public void r01_buildCurl_blankBodyContentTypeIsNotEmittedAsARemoval() {
        // The auto-added Content-Type went out as "-H 'Content-Type:   '" for a blank bodyContentType,
        // which tells curl to REMOVE the header - the opposite of adding one for the body.
        for (final String blank : new String[] { "", " ", "   ", "\t" }) {
            final String curl = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", null, "a=1", blank, '\'');

            assertFalse(curl.contains("Content-Type"), curl);
            assertTrue(curl.contains("--data-raw 'a=1'"), curl);
        }

        // a real content type is still added
        assertTrue(WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", null, "a=1", "application/json", '\'')
                .contains(" -H 'Content-Type: application/json'"));
    }

    @Test
    public void r01_curlConverters_suppressionNoteDoesNotContradictAGeneratedContentType() {
        final String target = "https://example.test/";
        final String command = "curl " + target + " -d 'a=1' -H 'Content-Type:'";

        // Both generators have to supply a Content-Type for an attached body, so a suppressed Content-Type
        // IS sent by the generated code itself; the note must not claim that no value is sent for it.
        for (final String code : new String[] { WebUtil.curlToHttpRequestCode(command), WebUtil.curlToOkHttpRequestCode(command) }) {
            assertTrue(code.contains("suppress the header(s) Content-Type"), code);
            assertTrue(code.contains("application/x-www-form-urlencoded"), code);
            assertFalse(code.contains("No value is sent for them here"), code);
        }

        // a header the generated code really does not send is still reported the same way
        final String gone = WebUtil.curlToHttpRequestCode("curl " + target + " -H 'X-Gone:'");
        assertTrue(gone.contains("suppress the header(s) X-Gone"), gone);
        assertFalse(gone.contains(".header("), gone);
    }

    @Test
    public void r01_buildCurl_contentTypeIsNeverEmittedTwice() {
        // The body's fallback Content-Type used to be gated on the VALUE of any existing Content-Type header
        // (Strings.isEmpty(HttpUtil.getContentType(headers))), which cannot tell an absent field from one
        // present with a null or empty value. A "Content-Type" entry mapped to null or "" therefore produced
        // BOTH "-H 'Content-Type;'" from the header loop AND "-H 'Content-Type: application/json'" - one
        // command carrying two contradictory Content-Type lines - while a blank "   " value produced neither
        // the caller's type nor the body's. The gate is now "did the loop emit a Content-Type line at all".
        final Object[] presentValues = { null, "", "   ", "\t", "text/xml" };

        for (final Object present : presentValues) {
            final Map<String, Object> headers = new HashMap<>();
            headers.put("X-Keep", "k");
            headers.put("Content-Type", present);

            final String curl = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", headers, "a=1", "application/json", '\'');

            assertEquals(1, countOccurrences(curl, "Content-Type"), curl);
            assertFalse(curl.contains("Content-Type: application/json"), curl);
            assertTrue(curl.contains("--data-raw " + APOS + "a=1" + APOS), curl);
        }

        // the name is matched case-insensitively, as everywhere else in this class
        final Map<String, Object> lowerCase = new HashMap<>();
        lowerCase.put("content-type", "");
        final String fromLowerCase = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", lowerCase, "a=1", "application/json", '\'');
        assertEquals(1, countOccurrences(fromLowerCase, "ontent-"), fromLowerCase);
        assertTrue(fromLowerCase.contains(" -H " + APOS + "content-type;" + APOS), fromLowerCase);

        // with no Content-Type entry at all the body's type is still supplied, exactly once
        final Map<String, Object> absent = new HashMap<>();
        absent.put("X-Keep", "k");
        final String supplied = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", absent, "a=1", "application/json", '\'');
        assertEquals(1, countOccurrences(supplied, "Content-Type"), supplied);
        assertTrue(supplied.contains(" -H " + APOS + "Content-Type: application/json" + APOS), supplied);
    }

    @Test
    public void r01_buildCurl_whitespaceOnlyBodyIsStillABody() {
        // "-I" and "--data-raw" are guarded by the same emptiness test on purpose: curl refuses a data option
        // together with -I. A whitespace-only body is a body (three bytes), so it must take -X HEAD and be
        // sent - switching either guard to isBlank alone would emit "curl -I .. --data-raw '   '", which curl
        // rejects outright, and switching both would silently drop the body.
        final String headWithBlankBody = WebUtil.buildCurl(HttpMethod.HEAD, "https://example.test/", null, "   ", null, '\'');
        assertTrue(headWithBlankBody.contains("curl -X HEAD "), headWithBlankBody);
        assertFalse(headWithBlankBody.contains("curl -I"), headWithBlankBody);
        assertTrue(headWithBlankBody.contains("--data-raw " + APOS + "   " + APOS), headWithBlankBody);

        final String headWithEmptyBody = WebUtil.buildCurl(HttpMethod.HEAD, "https://example.test/", null, "", null, '\'');
        assertTrue(headWithEmptyBody.contains("curl -I "), headWithEmptyBody);
        assertFalse(headWithEmptyBody.contains("--data-raw"), headWithEmptyBody);

        final String postWithBlankBody = WebUtil.buildCurl(HttpMethod.POST, "https://example.test/", null, "   ", "text/plain", '\'');
        assertTrue(postWithBlankBody.contains("--data-raw " + APOS + "   " + APOS), postWithBlankBody);
        assertTrue(postWithBlankBody.contains(" -H " + APOS + "Content-Type: text/plain" + APOS), postWithBlankBody);
    }

    /** The single-quote character the cURL builder uses, kept out of the string literals above. */
    private static final String APOS = "'";

    private static int countOccurrences(final String haystack, final String needle) {
        int count = 0;

        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }

        return count;
    }
}
