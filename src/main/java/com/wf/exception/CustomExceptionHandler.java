package com.wf.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Slf4j
@Component
public class CustomExceptionHandler implements HandlerExceptionResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex instanceof NotLoginException) {
            NotLoginException nle = (NotLoginException) ex;

            // 打印堆栈，以供调试
            log.warn("Sa-Token 登录校验失败: type={"+nle.getType()+"}, message={"+nle.getMessage()+"}");

            // 判断场景值，定制化异常信息
            String message = "";
            if(nle.getType().equals(NotLoginException.NOT_TOKEN)) {
                message = "未提供token";
            }
            else if(nle.getType().equals(NotLoginException.INVALID_TOKEN)) {
                message = "token无效";
            }
            else if(nle.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
                message = "token已过期";
            }
            else if(nle.getType().equals(NotLoginException.BE_REPLACED)) {
                message = "token已被顶下线";
            }
            else if(nle.getType().equals(NotLoginException.KICK_OUT)) {
                message = "token已被踢下线";
            }
            else {
                message = "当前会话未登录";
            }

            // 直接写入响应对象
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try {
                R result = R.fail(message);
                response.getWriter().write(objectMapper.writeValueAsString(result));
            } catch (IOException e) {
                log.error("写入响应失败", e);
            }
            return new ModelAndView();
        }
        return null;
    }
}
