package com.wf.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SMSUtils {

    private static String accessKeyId;
    private static String accessKeySecret;
    private static String signName;
    private static String templateCode;

    private static final String ENDPOINT = "dysmsapi.aliyuncs.com";
    private static final String VERSION = "2017-05-25";
    private static final String ACTION = "SendSms";
    private static final String METHOD = "POST";
    private static final String SIGNATURE_METHOD = "HMAC-SHA1";
    private static final String SIGNATURE_VERSION = "1.0";

    private static OkHttpClient client;

    @Value("${aliyun.sms.accessKeyId}")
    public void setAccessKeyId(String accessKeyId) {
        SMSUtils.accessKeyId = accessKeyId;
    }

    @Value("${aliyun.sms.accessKeySecret}")
    public void setAccessKeySecret(String accessKeySecret) {
        SMSUtils.accessKeySecret = accessKeySecret;
    }

    @Value("${aliyun.sms.signName}")
    public void setSignName(String signName) {
        SMSUtils.signName = signName;
    }

    @Value("${aliyun.sms.templateCode}")
    public void setTemplateCode(String templateCode) {
        SMSUtils.templateCode = templateCode;
    }

    private static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    public static void sendCaptcha(String phone, String code) {
        Map<String, String> params = new HashMap<>();
        params.put("PhoneNumbers", phone);
        params.put("SignName", signName);
        params.put("TemplateCode", templateCode);
        params.put("TemplateParam", "{\"code\":\"" + code + "\"}");

        String response = sendRequest(params);
        log.info("短信验证码发送成功，phone: {}, code: {}, response: {}", phone, code, response);
    }

    public static void sendCustomMessage(String phone, String templateCode, String templateParam) {
        Map<String, String> params = new HashMap<>();
        params.put("PhoneNumbers", phone);
        params.put("SignName", signName);
        params.put("TemplateCode", templateCode);
        params.put("TemplateParam", templateParam);

        String response = sendRequest(params);
        log.info("自定义短信发送成功，phone: {}, response: {}", phone, response);
    }

    private static String sendRequest(Map<String, String> params) {
        try {
            String url = buildUrl(params);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded")))
                    .build();

            try (Response response = getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                return response.body().string();
            }
        } catch (Exception e) {
            log.error("短信发送失败", e);
            throw new RuntimeException("短信发送失败", e);
        }
    }

    private static String buildUrl(Map<String, String> params) throws Exception {
        Map<String, String> commonParams = new HashMap<>();
        commonParams.put("Action", ACTION);
        commonParams.put("Version", VERSION);
        commonParams.put("AccessKeyId", accessKeyId);
        commonParams.put("SignatureMethod", SIGNATURE_METHOD);
        commonParams.put("SignatureVersion", SIGNATURE_VERSION);
        commonParams.put("SignatureNonce", UUID.randomUUID().toString());
        commonParams.put("Timestamp", getTimestamp());
        commonParams.put("Format", "JSON");

        Map<String, String> allParams = new HashMap<>();
        allParams.putAll(commonParams);
        allParams.putAll(params);

        String signature = generateSignature(allParams);
        allParams.put("Signature", signature);

        StringBuilder url = new StringBuilder("https://");
        url.append(ENDPOINT).append("/?");
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            url.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        return url.substring(0, url.length() - 1);
    }

    private static String generateSignature(Map<String, String> params) throws Exception {
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        Collections.sort(sortedKeys);

        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append(METHOD).append("&");
        stringToSign.append(URLEncoder.encode("/", StandardCharsets.UTF_8)).append("&");

        StringBuilder canonicalizedQueryString = new StringBuilder();
        for (String key : sortedKeys) {
            String value = params.get(key);
            canonicalizedQueryString.append("&")
                    .append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }

        stringToSign.append(URLEncoder.encode(canonicalizedQueryString.substring(1), StandardCharsets.UTF_8));

        String key = accessKeySecret + "&";
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.toString().getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(signData);
    }

    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
