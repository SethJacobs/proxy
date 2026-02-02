package com.example.funnelproxy.config;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(-100) // Very high priority to log all requests
public class RequestLoggingFilter implements WebFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getURI().getQuery();
        String fullUrl = path + (query != null ? "?" + query : "");
        
        System.out.println("🔍 INCOMING REQUEST: " + method + " " + fullUrl);
        System.out.println("🔍 Headers: " + exchange.getRequest().getHeaders());
        
        return chain.filter(exchange)
                .doOnSuccess(unused -> {
                    int status = exchange.getResponse().getStatusCode() != null ? 
                        exchange.getResponse().getStatusCode().value() : 0;
                    System.out.println("✅ RESPONSE: " + status + " for " + method + " " + fullUrl);
                })
                .doOnError(error -> {
                    System.err.println("❌ ERROR: " + error.getMessage() + " for " + method + " " + fullUrl);
                    error.printStackTrace();
                });
    }
}