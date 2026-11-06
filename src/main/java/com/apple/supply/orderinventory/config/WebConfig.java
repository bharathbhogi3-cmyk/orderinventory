package com.apple.supply.orderinventory.config;

import com.apple.supply.orderinventory.interceptor.OrderInterceptor;
import com.apple.supply.orderinventory.interceptor.ProductInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final OrderInterceptor orderInterceptor;
    private final ProductInterceptor productInterceptor;

    public WebConfig(
            OrderInterceptor orderInterceptor,
            ProductInterceptor productInterceptor) {

        this.orderInterceptor = orderInterceptor;
        this.productInterceptor = productInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(orderInterceptor)
                .addPathPatterns("/orders/**");

        registry.addInterceptor(productInterceptor)
                .addPathPatterns("/products/**");
    }
}
