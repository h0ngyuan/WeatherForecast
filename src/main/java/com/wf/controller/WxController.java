package com.wf.controller;
import cn.dev33.satoken.stp.StpUtil;
import com.wf.object.entity.UserInfoEntity;
import com.wf.object.query.BindContactQuery;
import com.wf.object.query.CaptchaQuery;
import com.wf.object.query.LoginQuery;
import com.wf.object.query.NotifySettingQuery;
import com.wf.object.query.SendCaptchaQuery;
import com.wf.service.CaptchaService;
import com.wf.service.ContactService;
import com.wf.service.WxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.tool.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "微信登录", description = "用户登录、登出、验证码等接口")
@RestController
@RequestMapping("/wx")
public class WxController {

    @Autowired
    private WxService wxService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private ContactService contactService;


    @Operation(summary = "用户登录", description = "支持微信、手机号、邮箱登录。微信登录只需openId；手机号/邮箱登录需要验证码，首次使用会自动注册")
    @PostMapping("/login")
    public R<String> wxLogin(@RequestBody() LoginQuery query) {
        return R.data(wxService.login(query));
    }

    @Operation(summary = "用户登出", description = "退出当前登录状态")
    @PostMapping("/logout")
    public R<String> logout() {
        wxService.logout();
        return R.success("登出成功");
    }

    @Operation(summary = "获取用户信息", description = "获取当前登录用户的完整信息（不包含密码）")
    @GetMapping("/getUserInfo")
    public R<UserInfoEntity> getUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserInfoEntity userInfo = wxService.getUserInfo(userId);
        return R.data(userInfo);
    }

    @Operation(summary = "生成数字验证码", description = "生成4位数字验证码，返回key和code")
    @GetMapping("/generateNumericCaptcha")
    public R<Map<String, String>> generateNumericCaptcha() {
        String key = UUID.randomUUID().toString();
        String code = captchaService.generateNumericCaptcha(key);
        return R.data(Map.of("key", key, "code", code));
    }

    @Operation(summary = "验证数字验证码", description = "验证数字验证码是否正确")
    @PostMapping("/verifyNumericCaptcha")
    public R<Boolean> verifyNumericCaptcha(@RequestBody CaptchaQuery query) {
        boolean result = captchaService.verifyNumericCaptcha(query.getKey(), query.getCode());
        return R.data(result);
    }

    @Operation(summary = "生成图形验证码", description = "生成图形验证码，返回key和base64图片")
    @GetMapping("/generateImageCaptcha")
    public R<Map<String, String>> generateImageCaptcha() {
        String key = UUID.randomUUID().toString();
        String base64Image = captchaService.generateImageCaptcha(key);
        return R.data(Map.of("key", key, "image", base64Image));
    }

    @Operation(summary = "验证图形验证码", description = "验证图形验证码是否正确")
    @PostMapping("/verifyImageCaptcha")
    public R<Boolean> verifyImageCaptcha(@RequestBody CaptchaQuery query) {
        boolean result = captchaService.verifyImageCaptcha(query.getKey(), query.getCode());
        return R.data(result);
    }

    @Operation(summary = "检查手机号是否绑定", description = "检查当前用户是否已绑定手机号（暂时禁用）")
    @GetMapping("/checkPhoneBound")
    public R<Boolean> checkPhoneBound() {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = contactService.checkPhoneBound(userId);
        return R.data(result);
    }

    @Operation(summary = "检查邮箱是否绑定", description = "检查当前用户是否已绑定邮箱")
    @GetMapping("/checkEmailBound")
    public R<Boolean> checkEmailBound() {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = contactService.checkEmailBound(userId);
        return R.data(result);
    }

    @Operation(summary = "绑定手机号", description = "为当前用户绑定手机号（暂时禁用）")
    @PostMapping("/bindPhone")
    public R<Boolean> bindPhone(@RequestBody BindContactQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = contactService.bindPhone(userId, query.getPhone(), query.getCode());
        return R.data(result);
    }

    @Operation(summary = "绑定邮箱", description = "为当前用户绑定邮箱")
    @PostMapping("/bindEmail")
    public R<Boolean> bindEmail(@RequestBody BindContactQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = contactService.bindEmail(userId, query.getEmail(), query.getCode());
        return R.data(result);
    }

    @Operation(summary = "发送手机号验证码", description = "向指定手机号发送验证码（暂时禁用，请使用邮箱登录）")
    @PostMapping("/sendPhoneCaptcha")
    public R<Void> sendPhoneCaptcha(@RequestBody SendCaptchaQuery query) {
        contactService.sendPhoneCaptcha(query.getPhone());
        return R.success();
    }

    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送验证码")
    @PostMapping("/sendEmailCaptcha")
    public R<Void> sendEmailCaptcha(@RequestBody SendCaptchaQuery query) {
        contactService.sendEmailCaptcha(query.getEmail());
        return R.success();
    }

    @Operation(summary = "获取通知设置", description = "获取用户的联系方式绑定状态和通知权限设置")
    @GetMapping("/getNotifySettings")
    public R<UserInfoEntity> getNotifySettings() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserInfoEntity settings = contactService.getNotifySettings(userId);
        return R.data(settings);
    }

    @Operation(summary = "更新通知设置", description = "批量更新用户的通知权限设置，支持同时更新多个权限")
    @PostMapping("/updateNotifySettings")
    public R<Void> updateNotifySettings(@RequestBody NotifySettingQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();
        contactService.updateNotifySettings(userId, query);
        return R.success();
    }

    @Operation(summary = "测试接口", description = "测试接口是否可用")
    @GetMapping("/hi")
    public R hi(){
        return R.success("hi");
    }
}
