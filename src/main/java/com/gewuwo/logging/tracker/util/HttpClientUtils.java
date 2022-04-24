package com.gewuwo.logging.tracker.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HttpClientUtils
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/5 10:39 上午
 */
public class HttpClientUtils {


    public static String post(String url, String data) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        //设置请求和传输超时时间
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build();
        httpPost.setConfig(requestConfig);
        httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
        httpPost.setEntity(new StringEntity(data,"UTF-8"));
        String httpEntityContent;
        HttpResponse response = httpClient.execute(httpPost);
        httpEntityContent = getHttpEntityContent(response);
        httpPost.abort();
        return httpEntityContent;
    }


    /**
     * 封装HTTP POST方法
     *
     * @param url      url
     * @param paramMap param
     * @return httpEntityContent
     * @throws ClientProtocolException cc
     * @throws IOException             exception
     */
    public static String post(String url, Map<String, String> paramMap) throws ClientProtocolException, IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = getHttpPost(url, paramMap);
        HttpResponse response = httpClient.execute(httpPost);
        String httpEntityContent = getHttpEntityContent(response);
        httpPost.abort();

        return httpEntityContent;

    }

    private static HttpPost getHttpPost(String url, Map<String, String> paramMap) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(url);
        //设置请求和传输超时时间
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build();
        httpPost.setConfig(requestConfig);
        List<NameValuePair> formParams = setHttpParams(paramMap);
        UrlEncodedFormEntity param = new UrlEncodedFormEntity(formParams, "UTF-8");
        httpPost.setEntity(param);
        return httpPost;
    }

    /**
     * 获得响应HTTP实体内容
     *
     * @param response response
     * @return ""
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    private static String getHttpEntityContent(HttpResponse response) throws IOException, UnsupportedEncodingException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\n");
                line = br.readLine();
            }
            return sb.toString();
        }
        return "";
    }

    /**
     * 设置请求参数
     *
     * @param paramMap
     * @return n
     */
    private static List<NameValuePair> setHttpParams(Map<String, String> paramMap) {
        List<NameValuePair> formparams = new ArrayList<>();
        Set<Map.Entry<String, String>> set = paramMap.entrySet();
        for (Map.Entry<String, String> entry : set) {
            formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return formparams;
    }
}
