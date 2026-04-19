package com.wf.interceptor;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

@Slf4j
@Component
public class RequestInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取完整请求信息
        Map<String, Object> requestInfo = getRequestInfoMap(request);
        
        // 存储到 SaHolder 上下文（如果上下文已初始化）
        try {
            SaHolder.getStorage().set("requestInfo", requestInfo);
            SaHolder.getStorage().set("clientIp", requestInfo.get("clientIp"));
            SaHolder.getStorage().set("requestUri", requestInfo.get("requestUri"));
            SaHolder.getStorage().set("method", requestInfo.get("method"));
        } catch (Exception e) {
            // Sa-Token 上下文未初始化，忽略存储
            log.debug("Sa-Token context not initialized, skip storage");
        }
        
        return true;
    }

    /**
     * 构建请求信息 Map
     */
    private Map<String, Object> getRequestInfoMap(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>(16);

        // 基础信息
        info.put("method", request.getMethod());
        info.put("requestUri", request.getRequestURI());
        info.put("queryString", request.getQueryString());
        info.put("protocol", request.getProtocol());
        info.put("scheme", request.getScheme());
        info.put("secure", request.isSecure());

        // IP 信息
        info.put("clientIp", getRealIp(request));
        info.put("remoteAddr", request.getRemoteAddr());
        info.put("remoteHost", request.getRemoteHost());
        info.put("remotePort", request.getRemotePort());

        // 服务器信息
        info.put("serverName", request.getServerName());
        info.put("serverPort", request.getServerPort());
        info.put("contextPath", request.getContextPath());
        info.put("servletPath", request.getServletPath());

        // 请求头
        info.put("headers", getAllHeaders(request));

        // 请求参数
        info.put("parameters", request.getParameterMap());

        // Cookie
        info.put("cookies", getAllCookies(request));

        // Session
        info.put("sessionId", getSessionId(request));

        // Sa-Token 信息（如果上下文已初始化）
        try {
            info.put("tokenValue", StpUtil.getTokenValue());
            info.put("isLogin", StpUtil.isLogin());
            if (StpUtil.isLogin()) {
                try {
                    info.put("loginId", StpUtil.getLoginId());
                } catch (Exception e) {
                    // 忽略
                }
            }
        } catch (Exception e) {
            // Sa-Token 上下文未初始化
            info.put("tokenValue", null);
            info.put("isLogin", false);
        }

        // 时间戳
        info.put("requestTime", System.currentTimeMillis());

        return info;
    }

    /**
     * 获取真实 IP（处理代理/CDN）
     */
    private String getRealIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 获取所有请求头
     */
    private Map<String, String> getAllHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    /**
     * 获取所有 Cookie
     */
    private List<Map<String, String>> getAllCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Collections.emptyList();
        }

        List<Map<String, String>> cookieList = new ArrayList<>();
        for (Cookie cookie : cookies) {
            Map<String, String> cookieMap = new HashMap<>(4);
            cookieMap.put("name", cookie.getName());
            cookieMap.put("value", cookie.getValue());
            cookieMap.put("domain", cookie.getDomain());
            cookieMap.put("path", cookie.getPath());
            cookieList.add(cookieMap);
        }
        return cookieList;
    }

    /**
     * 获取 Session ID
     */
    private String getSessionId(HttpServletRequest request) {
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        return session != null ? session.getId() : null;
    }


    public int getOrder(){
        return -90;
    }
}