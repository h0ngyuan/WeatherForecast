package com.wf.factory;

import com.wf.strategy.LoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class LoginStrategyFactory {

    private final Map<String, LoginStrategy> strategyMap;

    @Autowired
    public LoginStrategyFactory(Map<String, LoginStrategy> strategyMap) {
        this.strategyMap = strategyMap;
    }

    public LoginStrategy getStrategy(String type) {
        LoginStrategy strategy = strategyMap.get(type + "Login");
        if (strategy == null) {
            log.warn("不支持的登录类型: {}", type);
            throw new RuntimeException("不支持的登录类型: " + type);
        }
        return strategy;
    }
}
