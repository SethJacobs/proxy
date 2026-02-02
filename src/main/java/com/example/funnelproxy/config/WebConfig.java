package com.example.funnelproxy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.config.ViewResolverRegistry;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {
    
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        // Ensure Thymeleaf view resolver has higher priority than the proxy controller
        registry.order(Ordered.HIGHEST_PRECEDENCE);
    }
}