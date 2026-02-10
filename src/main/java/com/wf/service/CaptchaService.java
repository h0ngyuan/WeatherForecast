package com.wf.service;

public interface CaptchaService {

    String generateNumericCaptcha(String key);

    boolean verifyNumericCaptcha(String key, String code);

    String generateImageCaptcha(String key);

    boolean verifyImageCaptcha(String key, String code);
}
