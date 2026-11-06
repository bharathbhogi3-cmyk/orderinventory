package com.apple.supply.orderinventory.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class OrderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        String module = request.getHeader("X-Module");

        if ("orders".equalsIgnoreCase(module)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"message\":\"Invalid or missing X-Module header. Expected orders.\"}"
        );

        return false;
    }
}
