package com.example.funnelproxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {
    
    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK - Funnel Proxy is running");
    }
    
    @GetMapping("/status")
    public Mono<String> status() {
        return Mono.just("Application is healthy");
    }
}