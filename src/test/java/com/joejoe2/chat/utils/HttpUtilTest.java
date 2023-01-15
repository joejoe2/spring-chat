package com.joejoe2.chat.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpUtilTest {

    @Test
    void extractAccessToken() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.doReturn("Bearer token").when(request).getHeader(HttpHeaders.AUTHORIZATION);
        assertEquals("token", HttpUtil.extractAccessToken(request));
        Mockito.doReturn("token").when(request).getParameter("access_token");
        assertEquals("token", HttpUtil.extractAccessToken(request));
    }

    @Test
    void splitQuery() {
        Map<String, String> params = new HashMap<>();
        params.put("k1", "1");
        params.put("k2", "str");
        params.put("k3", "45799962");

        String query = "arr=[1, 2, 3]";
        for (Map.Entry<String, String> param : params.entrySet()) {
            query += "&" + param.getKey() + "=" + param.getValue();
        }
        params.put("arr", "[1, 2, 3]");
        // test invalid query
        query += "&";
        query += "&invalid";

        assertEquals(params, HttpUtil.splitQuery(query));
    }
}