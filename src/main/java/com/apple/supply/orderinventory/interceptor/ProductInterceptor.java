package com.apple.supply.orderinventory.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class ProductInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        String module = request.getHeader("X-Module");

        System.out.println("Received X-Module: " + module);

        if ("products".equalsIgnoreCase(module)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"message\":\"Invalid or missing X-Module header. Expected products.\"}"
        );

        return false;
    }
}
