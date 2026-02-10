package com.wf.service.impl;

import com.wf.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String NUMERIC_CAPTCHA_PREFIX = "captcha:numeric:";
    private static final String IMAGE_CAPTCHA_PREFIX = "captcha:image:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int NUMERIC_LENGTH = 4;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 40;

    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();

    public CaptchaServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String generateNumericCaptcha(String key) {
        String code = generateRandomNumeric(NUMERIC_LENGTH);
        String redisKey = NUMERIC_CAPTCHA_PREFIX + key;
        redisTemplate.opsForValue().set(redisKey, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("生成数字验证码，key: {}, code: {}", key, code);
        return code;
    }

    @Override
    public boolean verifyNumericCaptcha(String key, String code) {
        String redisKey = NUMERIC_CAPTCHA_PREFIX + key;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            log.warn("验证码已过期或不存在，key: {}", key);
            return false;
        }
        boolean isValid = storedCode.equals(code);
        if (isValid) {
            redisTemplate.delete(redisKey);
            log.info("数字验证码验证成功，key: {}", key);
        } else {
            log.warn("数字验证码验证失败，key: {}, expected: {}, actual: {}", key, storedCode, code);
        }
        return isValid;
    }

    @Override
    public String generateImageCaptcha(String key) {
        String code = generateRandomNumeric(NUMERIC_LENGTH);
        String redisKey = IMAGE_CAPTCHA_PREFIX + key;
        redisTemplate.opsForValue().set(redisKey, code, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);
        
        BufferedImage image = createCaptchaImage(code);
        String base64Image = imageToBase64(image);
        log.info("生成图形验证码，key: {}, code: {}", key, code);
        return base64Image;
    }

    @Override
    public boolean verifyImageCaptcha(String key, String code) {
        String redisKey = IMAGE_CAPTCHA_PREFIX + key;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode == null) {
            log.warn("图形验证码已过期或不存在，key: {}", key);
            return false;
        }
        boolean isValid = storedCode.equalsIgnoreCase(code);
        if (isValid) {
            redisTemplate.delete(redisKey);
            log.info("图形验证码验证成功，key: {}", key);
        } else {
            log.warn("图形验证码验证失败，key: {}, expected: {}, actual: {}", key, storedCode, code);
        }
        return isValid;
    }

    private String generateRandomNumeric(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private BufferedImage createCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        g2d.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
        
        int x = 20;
        for (char c : code.toCharArray()) {
            g2d.drawString(String.valueOf(c), x, 30);
            x += 25;
        }
        
        for (int i = 0; i < 5; i++) {
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g2d.drawLine(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT),
                        random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT));
        }
        
        for (int i = 0; i < 30; i++) {
            g2d.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g2d.fillOval(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT), 2, 2);
        }
        
        g2d.dispose();
        return image;
    }

    private String imageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            log.error("图片转Base64失败", e);
            throw new RuntimeException("生成图形验证码失败", e);
        }
    }
}
