package com.example.funnelproxy.controller;

import com.example.funnelproxy.service.ProxyService;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Order(200) // Lower priority than admin controllers
public class ProxyController {
    
    private final ProxyService proxyService;
    
    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }
    
    @RequestMapping(value = "/**")
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
        String path = request.getPath().value();
        
        System.out.println("🔀 ProxyController handling: " + path);
        
        // Skip admin, health, and status paths - let other controllers handle them
        if (path.startsWith("/admin") || path.equals("/health") || path.equals("/status") || path.equals("/")) {
            System.out.println("⏭️ Skipping proxy for admin/health path: " + path);
            response.setStatusCode(org.springframework.http.HttpStatus.NOT_FOUND);
            return response.setComplete();
        }
        
        return proxyService.proxy(request, response);
    }
}