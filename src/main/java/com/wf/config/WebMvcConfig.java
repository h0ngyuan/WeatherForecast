package com.wf.config;

import com.wf.interceptor.RequestInfoInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RequestInfoInterceptor requestInfoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestInfoInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/static/**",
                    "/public/**",
                    "/favicon.ico"
                );
    }
}