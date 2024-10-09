package com.example.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.example.backend.security.RequestLoggingFilter;

@Configuration
public class FilterConfig {

    @Bean
    FilterRegistrationBean<RequestLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        registrationBean.setFilter(new RequestLoggingFilter());
        registrationBean.addUrlPatterns("/");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        return registrationBean;
    }
}