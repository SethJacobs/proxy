package com.example.funnelproxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {
    
    @GetMapping("/health")
    public Mono<String> health() {
        System.out.println("🏥 Health endpoint called");
        return Mono.just("OK - Funnel Proxy is running");
    }
    
    @GetMapping("/status")
    public Mono<String> status() {
        System.out.println("📊 Status endpoint called");
        return Mono.just("Application is healthy");
    }
    
    @GetMapping("/")
    public Mono<String> root() {
        System.out.println("🏠 Root endpoint called");
        return Mono.just("Funnel Proxy is running. Visit /admin for configuration.");
    }
}