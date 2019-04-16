package com.gerry.yitao.yitaosellerservice;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication(scanBasePackages = "com.gerry.yitao")
@EnableDubbo
public class YitaoSellerWebApiApplication {


    public static void main(String[] args) {
        SpringApplication.run(YitaoSellerWebApiApplication.class);

    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 配置允许跨域的域名
        config.addAllowedOrigin("http://api.yitao.com");
        config.addAllowedOrigin("http://manage.yitao.com");
        // 允许发送cookie
        config.setAllowCredentials(true);
        // 允许跨域接口
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");

        // 允许头信息
        config.addAllowedHeader("*");

        // 添加映射路径
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);

        return new CorsFilter(configSource);
    }

}
