package com.wf;

import cn.dev33.satoken.annotation.SaCheckLogin;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.wf.mapper")
@SpringBootApplication
public class WeatherForecastApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherForecastApplication.class, args);
    }

}
