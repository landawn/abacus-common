package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.http.HARUtil;
import com.landawn.abacus.http.HttpRequest;
import com.landawn.abacus.http.OkHttpRequest;
import com.landawn.abacus.http.WebUtil;

import okhttp3.MediaType;
import okhttp3.RequestBody;

class WebUtilTest {

    // https://reqbin.com/req/c-d2nzjn3z/curl-post-body
    @Test
    public void test_curl2HttpRequeset() throws IOException {

        String resp0 = HttpRequest.url("https://www.google.com/").get(String.class);

        N.println(resp0.substring(0, 1000));

        String curl = """
                curl -X POST https://reqbin.com/echo/post/json
                   -H "Content-Type: application/json;charset=utf-8"
                   -d '{"productId": "abd\\\'123", "quantity": 100}'
                                                    """;

        String httpRequestCode = WebUtil.curl2HttpRequest(curl);
        N.println(httpRequestCode);

        httpRequestCode = WebUtil.curl2OkHttpRequest(curl);
        N.println(httpRequestCode);

        {
            String requestBody = "{\"productId\": \"abd\\'123\", \"quantity\": 100}";

            HttpRequest.url("https://reqbin.com/echo/post/json").header("Content-Type", "application/json").body(requestBody).post(String.class);
        }

        {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), "{\"productId\": \"abd\\'123\", \"quantity\": 100}");

            OkHttpRequest.url("https://reqbin.com/echo/post/json").header("Content-Type", "application/json").body(requestBody).post();
        }
    }

    @Test
    public void test_httpRequest2Curl() throws IOException {

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), "{\"productId\": \"abc'\\\\\"\", \"quantity\": 100}");
        WebUtil.createOkHttpRequestForCurl("https://reqbin.com/echo/post/json", System.out::println)
                .header("Content-Type", "application/json;charset = utf-8")
                .body(requestBody)
                .post();

        WebUtil.createOkHttpRequestForCurl("https://reqbin.com/echo/post/json", '"', System.out::println)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .post();

    }

    @Test
    public void test_sendRequestByHAR() {
        HARUtil.logRequestCurlForHARRequest(true, '"');
        File file = new File("./src/test/resources/sjpermits.org.har");
        final String targetUrl = "https://sjpermits.org/permits/ir/detail_5.asp";

        String resp = HARUtil.sendRequestByHAR(file, Fn.startsWith(targetUrl));

        String prefix = "<input type=\"hidden\" name=\"hRequestDate\" value=\"";
        String result = Strings.substringBetween(resp, prefix, "\">");
        N.println(result);

        resp = HARUtil.sendRequestByHAR(file, Fn.startsWith(targetUrl));

        result = Strings.substringBetween(resp, prefix, "\">");
        N.println(result);

        HARUtil.streamMultiRequestsByHAR(file, Fn.startsWith(targetUrl))
                .map(it -> Strings.substringBetween(it._2.body(String.class), prefix, "\">"))
                .forEach(Fn.println());
    }

}
